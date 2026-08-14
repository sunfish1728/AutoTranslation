package me.langyue.autotranslation.core;

import javax.net.ssl.SNIHostName;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.IDN;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Small HTTPS/1.1 transport with injectable DNS resolution. The socket may
 * connect to an override IP, while Host, SNI, and certificate verification
 * always use the URI hostname. Connections are not pooled, so a resolver
 * change can never reuse a route created for an older IP.
 */
public final class TlsHttpClient implements AutoCloseable {
    @FunctionalInterface
    public interface Resolver { InetAddress resolve(String hostname) throws IOException; }

    public record ConnectionKey(String scheme, String domain, int port, String resolvedIp) { }
    public record Response(int status, String contentType, byte[] body, ConnectionKey connectionKey) {
        public String bodyUtf8() { return new String(body, StandardCharsets.UTF_8); }
    }

    private final Resolver resolver;
    private final SSLSocketFactory sslSocketFactory;
    private final Duration connectTimeout;
    private final int maximumBodyBytes;
    private final Set<Socket> activeSockets = ConcurrentHashMap.newKeySet();
    private volatile boolean closed;

    public TlsHttpClient(Resolver resolver, Duration connectTimeout, int maximumBodyBytes) {
        this(resolver, (SSLSocketFactory) SSLSocketFactory.getDefault(), connectTimeout, maximumBodyBytes);
    }

    TlsHttpClient(Resolver resolver, SSLSocketFactory sslSocketFactory, Duration connectTimeout, int maximumBodyBytes) {
        this.resolver = Objects.requireNonNull(resolver, "resolver");
        this.sslSocketFactory = Objects.requireNonNull(sslSocketFactory, "sslSocketFactory");
        if (connectTimeout == null || connectTimeout.isNegative() || connectTimeout.isZero()) throw new IllegalArgumentException("connectTimeout must be positive");
        if (maximumBodyBytes < 1) throw new IllegalArgumentException("maximumBodyBytes must be positive");
        this.connectTimeout = connectTimeout;
        this.maximumBodyBytes = maximumBodyBytes;
    }

    public static Resolver systemResolver() { return InetAddress::getByName; }

    public static Resolver fixedAddress(String expectedDomain, String configuredIp) {
        String domain = normalizeDomain(expectedDomain);
        InetAddress address = parseIpLiteral(configuredIp);
        return hostname -> {
            if (!domain.equals(normalizeDomain(hostname))) throw new IOException("DNS override used for an unexpected hostname");
            return address;
        };
    }

    public Response get(URI endpoint, Duration totalDeadline) throws IOException {
        if (closed) throw new IOException("HTTP client is closed");
        validateEndpoint(endpoint);
        if (totalDeadline == null || totalDeadline.isNegative() || totalDeadline.isZero()) throw new IllegalArgumentException("totalDeadline must be positive");
        long deadline = System.nanoTime() + totalDeadline.toNanos();
        String domain = normalizeDomain(endpoint.getHost());
        int port = endpoint.getPort() == -1 ? 443 : endpoint.getPort();
        InetAddress resolved = resolver.resolve(domain);
        ConnectionKey key = new ConnectionKey("https", domain, port, resolved.getHostAddress());

        Socket plain = new Socket();
        activeSockets.add(plain);
        try (plain) {
            if (closed) throw new IOException("HTTP client is closed");
            plain.connect(new InetSocketAddress(resolved, port), boundedMillis(connectTimeout, deadline));
            plain.setSoTimeout(remainingMillis(deadline));
            try (SSLSocket tls = (SSLSocket) sslSocketFactory.createSocket(plain, domain, port, true)) {
                activeSockets.add(tls);
                try {
                    if (closed) throw new IOException("HTTP client is closed");
                    SSLParameters parameters = tls.getSSLParameters();
                    parameters.setEndpointIdentificationAlgorithm("HTTPS");
                    parameters.setServerNames(List.of(new SNIHostName(domain)));
                    tls.setSSLParameters(parameters);
                    tls.setSoTimeout(remainingMillis(deadline));
                    tls.startHandshake();
                    writeRequest(tls.getOutputStream(), endpoint, domain, port);
                    return readResponse(tls, tls.getInputStream(), key, deadline);
                } finally {
                    activeSockets.remove(tls);
                }
            }
        } finally {
            activeSockets.remove(plain);
        }
    }

    public void probe(URI endpoint, Duration totalDeadline) throws IOException { get(endpoint, totalDeadline); }

    private void writeRequest(OutputStream output, URI endpoint, String domain, int port) throws IOException {
        String path = endpoint.getRawPath();
        if (path == null || path.isEmpty()) path = "/";
        if (endpoint.getRawQuery() != null) path += "?" + endpoint.getRawQuery();
        String host = port == 443 ? domain : domain + ":" + port;
        String request = "GET " + path + " HTTP/1.1\r\nHost: " + host
                + "\r\nUser-Agent: AutoTranslation/1.3.0\r\nAccept: application/json,text/plain;q=0.9\r\nAccept-Encoding: identity\r\nConnection: close\r\n\r\n";
        output.write(request.getBytes(StandardCharsets.US_ASCII));
        output.flush();
    }

    private Response readResponse(SSLSocket socket, InputStream input, ConnectionKey key, long deadline) throws IOException {
        String statusLine = readAsciiLine(socket, input, deadline);
        String[] statusParts = statusLine.split(" ", 3);
        if (statusParts.length < 2 || !statusParts[0].startsWith("HTTP/")) throw new IOException("Malformed HTTP response");
        int status;
        try { status = Integer.parseInt(statusParts[1]); } catch (NumberFormatException malformed) { throw new IOException("Malformed HTTP status", malformed); }
        Map<String, String> headers = new LinkedHashMap<>();
        int headerBytes = statusLine.length();
        for (;;) {
            String line = readAsciiLine(socket, input, deadline);
            headerBytes += line.length();
            if (headerBytes > 32 * 1024) throw new IOException("HTTP headers exceed limit");
            if (line.isEmpty()) break;
            int separator = line.indexOf(':');
            if (separator <= 0) throw new IOException("Malformed HTTP header");
            headers.putIfAbsent(line.substring(0, separator).trim().toLowerCase(Locale.ROOT), line.substring(separator + 1).trim());
        }
        if (status < 200 || status >= 300) throw new IOException("Unexpected HTTP status " + status);
        String contentType = headers.getOrDefault("content-type", "").toLowerCase(Locale.ROOT);
        if (!contentType.isEmpty() && !contentType.startsWith("application/json") && !contentType.startsWith("text/")) {
            throw new IOException("Unexpected response content type");
        }
        byte[] body;
        if ("chunked".equalsIgnoreCase(headers.get("transfer-encoding"))) {
            body = readChunked(socket, input, deadline);
        } else if (headers.containsKey("content-length")) {
            long length;
            try { length = Long.parseLong(headers.get("content-length")); } catch (NumberFormatException malformed) { throw new IOException("Malformed Content-Length", malformed); }
            if (length < 0 || length > maximumBodyBytes) throw new IOException("Response body exceeds configured limit");
            body = readExactly(socket, input, (int) length, deadline);
        } else {
            body = readUntilEof(socket, input, deadline);
        }
        return new Response(status, contentType, body, key);
    }

    private byte[] readChunked(SSLSocket socket, InputStream input, long deadline) throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        for (;;) {
            String sizeLine = readAsciiLine(socket, input, deadline);
            int extension = sizeLine.indexOf(';');
            if (extension >= 0) sizeLine = sizeLine.substring(0, extension);
            int length;
            try { length = Integer.parseInt(sizeLine.trim(), 16); } catch (NumberFormatException malformed) { throw new IOException("Malformed chunk length", malformed); }
            if (length == 0) {
                while (!readAsciiLine(socket, input, deadline).isEmpty()) { }
                break;
            }
            if (length < 0 || result.size() + (long) length > maximumBodyBytes) throw new IOException("Response body exceeds configured limit");
            result.write(readExactly(socket, input, length, deadline));
            if (!readAsciiLine(socket, input, deadline).isEmpty()) throw new IOException("Malformed chunk terminator");
        }
        return result.toByteArray();
    }

    private byte[] readUntilEof(SSLSocket socket, InputStream input, long deadline) throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        for (;;) {
            socket.setSoTimeout(remainingMillis(deadline));
            int read = input.read(buffer);
            if (read < 0) return result.toByteArray();
            if (result.size() + (long) read > maximumBodyBytes) throw new IOException("Response body exceeds configured limit");
            result.write(buffer, 0, read);
        }
    }

    private static byte[] readExactly(SSLSocket socket, InputStream input, int length, long deadline) throws IOException {
        byte[] result = new byte[length];
        int offset = 0;
        while (offset < length) {
            socket.setSoTimeout(remainingMillis(deadline));
            int read = input.read(result, offset, length - offset);
            if (read < 0) throw new EOFException("Unexpected end of HTTP response");
            offset += read;
        }
        return result;
    }

    private static String readAsciiLine(SSLSocket socket, InputStream input, long deadline) throws IOException {
        ByteArrayOutputStream line = new ByteArrayOutputStream();
        boolean carriageReturn = false;
        while (line.size() <= 8192) {
            socket.setSoTimeout(remainingMillis(deadline));
            int value = input.read();
            if (value < 0) throw new EOFException("Unexpected end of HTTP headers");
            if (carriageReturn) {
                if (value == '\n') return line.toString(StandardCharsets.US_ASCII);
                line.write('\r');
                carriageReturn = false;
            }
            if (value == '\r') carriageReturn = true; else line.write(value);
        }
        throw new IOException("HTTP line exceeds limit");
    }

    private static int boundedMillis(Duration configured, long deadline) throws IOException {
        return (int) Math.min(Math.max(1L, configured.toMillis()), remainingMillis(deadline));
    }

    private static int remainingMillis(long deadline) throws IOException {
        long remaining = deadline - System.nanoTime();
        if (remaining <= 0) throw new IOException("HTTP request deadline exceeded");
        return (int) Math.min(Integer.MAX_VALUE, Math.max(1L, Duration.ofNanos(remaining).toMillis()));
    }

    private static void ensureBeforeDeadline(long deadline) throws IOException {
        if (System.nanoTime() >= deadline) throw new IOException("HTTP request deadline exceeded");
    }

    private static void validateEndpoint(URI endpoint) {
        if (endpoint == null || !"https".equalsIgnoreCase(endpoint.getScheme()) || endpoint.getHost() == null
                || endpoint.getUserInfo() != null || endpoint.getFragment() != null || endpoint.isOpaque()) {
            throw new IllegalArgumentException("Absolute HTTPS endpoint without user-info or fragment required");
        }
        normalizeDomain(endpoint.getHost());
    }

    private static String normalizeDomain(String rawDomain) {
        if (rawDomain == null || rawDomain.isBlank()) throw new IllegalArgumentException("domain must not be blank");
        String domain = IDN.toASCII(rawDomain.trim(), IDN.USE_STD3_ASCII_RULES).toLowerCase(Locale.ROOT);
        if (domain.length() > 253 || domain.startsWith(".") || domain.endsWith(".") || domain.contains("..")
                || domain.matches("[0-9.]+") || domain.contains(":")) {
            throw new IllegalArgumentException("A DNS hostname, not an IP literal, is required");
        }
        return domain;
    }

    private static InetAddress parseIpLiteral(String rawIp) {
        if (rawIp == null || rawIp.isBlank() || rawIp.contains("%") || !rawIp.matches("[0-9A-Fa-f:.]+")) {
            throw new IllegalArgumentException("A literal IPv4 or IPv6 address is required");
        }
        try {
            InetAddress parsed = InetAddress.getByName(rawIp);
            if (!rawIp.contains(":")) {
                String[] octets = rawIp.split("\\.", -1);
                if (octets.length != 4) throw new IllegalArgumentException("A canonical IPv4 address is required");
                for (String octet : octets) {
                    int value = Integer.parseInt(octet);
                    if (value < 0 || value > 255 || (!octet.equals("0") && octet.startsWith("0"))) throw new IllegalArgumentException("A canonical IPv4 address is required");
                }
            }
            return parsed;
        } catch (IOException | NumberFormatException failure) {
            throw new IllegalArgumentException("Invalid IP address", failure);
        }
    }

    @Override public void close() {
        closed = true;
        for (Socket socket : activeSockets) {
            try { socket.close(); } catch (IOException ignored) { }
        }
        activeSockets.clear();
    }
}
