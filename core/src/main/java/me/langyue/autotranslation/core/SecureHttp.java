package me.langyue.autotranslation.core;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;

/** JVM-default PKIX/hostname verification; accepts absolute HTTPS endpoints only. */
public final class SecureHttp {
    private static final int DEFAULT_MAX_BODY_BYTES = 2 * 1024 * 1024;
    private final HttpClient client;
    private final int maxBodyBytes;

    public SecureHttp() { this(Duration.ofSeconds(5), DEFAULT_MAX_BODY_BYTES); }

    public SecureHttp(Duration connectTimeout, int maxBodyBytes) {
        if (connectTimeout == null || connectTimeout.isNegative() || connectTimeout.isZero()) {
            throw new IllegalArgumentException("connectTimeout must be positive");
        }
        if (maxBodyBytes < 1) throw new IllegalArgumentException("maxBodyBytes must be positive");
        this.client = HttpClient.newBuilder().connectTimeout(connectTimeout).followRedirects(HttpClient.Redirect.NEVER).build();
        this.maxBodyBytes = maxBodyBytes;
    }

    public String get(URI endpoint, Duration deadline) throws Exception {
        validateEndpoint(endpoint);
        if (deadline == null || deadline.isNegative() || deadline.isZero()) throw new IllegalArgumentException("deadline must be positive");
        HttpRequest request = HttpRequest.newBuilder(endpoint).timeout(deadline).GET().build();
        HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        try (InputStream body = response.body()) {
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new HttpStatusException(response.statusCode());
            }
            String contentType = response.headers().firstValue("content-type").orElse("").toLowerCase(Locale.ROOT);
            if (!contentType.isEmpty() && !contentType.startsWith("application/json") && !contentType.startsWith("text/")) {
                throw new IllegalStateException("Unexpected response content type");
            }
            byte[] bytes = body.readNBytes(maxBodyBytes + 1);
            if (bytes.length > maxBodyBytes) throw new IllegalStateException("Response body exceeds configured limit");
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }

    public void probe(URI endpoint, Duration deadline) throws Exception {
        get(endpoint, deadline);
    }

    private static void validateEndpoint(URI endpoint) {
        if (endpoint == null || !"https".equalsIgnoreCase(endpoint.getScheme()) || endpoint.getHost() == null
                || endpoint.getUserInfo() != null || endpoint.isOpaque()) {
            throw new IllegalArgumentException("Absolute HTTPS endpoint without user-info required");
        }
    }

    public static final class HttpStatusException extends Exception {
        private final int statusCode;
        public HttpStatusException(int statusCode) {
            super("Unexpected HTTP status " + statusCode);
            this.statusCode = statusCode;
        }
        public int statusCode() { return statusCode; }
    }
}
