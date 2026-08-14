package me.langyue.autotranslation.translate.google;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import me.langyue.autotranslation.core.FormatProtector;
import me.langyue.autotranslation.core.TlsHttpClient;
import me.langyue.autotranslation.translate.ITranslator;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/** Google web translator using verified TLS probes instead of ICMP address selection. */
public final class GoogleTranslator implements ITranslator, AutoCloseable {
    private static final Duration REQUEST_DEADLINE = Duration.ofSeconds(15);
    private final String domain;
    private final List<String> candidateAddresses;
    private final List<TlsHttpClient> clients = new ArrayList<>();
    private volatile TlsHttpClient client;
    private volatile boolean ready;

    public GoogleTranslator(String domain, Collection<String> candidateAddresses) {
        this.domain = Objects.requireNonNull(domain, "domain").trim();
        this.candidateAddresses = new ArrayList<>(Objects.requireNonNull(candidateAddresses, "candidateAddresses"));
    }

    @Override public synchronized void init() {
        close();
        for (String address : candidateAddresses) {
            try {
                clients.add(new TlsHttpClient(TlsHttpClient.fixedAddress(domain, address), Duration.ofSeconds(4), 2 * 1024 * 1024));
            } catch (IllegalArgumentException invalidAddress) {
                // Legacy configs are user-editable; one malformed override must not disable system DNS.
            }
        }
        clients.add(new TlsHttpClient(TlsHttpClient.systemResolver(), Duration.ofSeconds(5), 2 * 1024 * 1024));
        ready = true;
    }

    @Override public boolean ready() { return ready; }

    @Override public int maxLength() { return 5000; }

    @Override public String translate(String text, String targetLanguage, String sourceLanguage) {
        if (!ready || text == null) return null;
        try {
            FormatProtector.ProtectedText protectedText = FormatProtector.protect(text);
            String query = "client=gtx&dt=t&sl=" + encode(sourceLanguage) + "&tl=" + encode(targetLanguage)
                    + "&q=" + encode(protectedText.text());
            URI endpoint = URI.create("https://" + domain + "/translate_a/single?" + query);
            String translated = decodeGoogleResponse(request(endpoint));
            return protectedText.restore(translated);
        } catch (Exception failure) {
            return null;
        }
    }

    private String request(URI endpoint) throws Exception {
        long deadline = System.nanoTime() + REQUEST_DEADLINE.toNanos();
        TlsHttpClient active = client;
        if (active != null) {
            try {
                return active.get(endpoint, remainingOrThrow(deadline)).bodyUtf8();
            } catch (Exception routeFailure) {
                synchronized (this) { if (client == active) client = null; }
            }
        }
        URI probe = URI.create("https://" + domain + "/generate_204");
        Exception lastFailure = null;
        for (TlsHttpClient candidate : clients) {
            if (candidate == active) continue;
            Duration remaining = remaining(deadline);
            if (remaining.isZero()) break;
            try {
                candidate.probe(probe, min(remaining, Duration.ofSeconds(3)));
                remaining = remaining(deadline);
                if (remaining.isZero()) break;
                String response = candidate.get(endpoint, remaining).bodyUtf8();
                client = candidate;
                return response;
            } catch (Exception rejected) {
                lastFailure = rejected;
            }
        }
        if (lastFailure != null) throw lastFailure;
        throw new IllegalStateException("No HTTPS routes are configured");
    }

    private static Duration remaining(long deadline) {
        long nanos = deadline - System.nanoTime();
        return nanos <= 0 ? Duration.ZERO : Duration.ofNanos(nanos);
    }

    private static Duration remainingOrThrow(long deadline) {
        Duration remaining = remaining(deadline);
        if (remaining.isZero()) throw new IllegalStateException("Google translation request deadline exceeded");
        return remaining;
    }

    private static Duration min(Duration first, Duration second) { return first.compareTo(second) <= 0 ? first : second; }

    static String decodeGoogleResponse(String json) {
        JsonElement root = JsonParser.parseString(json);
        if (!root.isJsonArray()) throw new IllegalArgumentException("Unexpected Google response");
        JsonArray outer = root.getAsJsonArray();
        if (outer.isEmpty() || !outer.get(0).isJsonArray()) throw new IllegalArgumentException("Unexpected Google response");
        StringBuilder translated = new StringBuilder();
        for (JsonElement segmentElement : outer.get(0).getAsJsonArray()) {
            if (!segmentElement.isJsonArray()) continue;
            JsonArray segment = segmentElement.getAsJsonArray();
            if (!segment.isEmpty() && !segment.get(0).isJsonNull()) translated.append(segment.get(0).getAsString());
        }
        if (translated.isEmpty()) throw new IllegalArgumentException("Google response contained no translation");
        return translated.toString();
    }

    private static String encode(String value) { return URLEncoder.encode(Objects.requireNonNullElse(value, "auto"), StandardCharsets.UTF_8); }

    @Override public synchronized void close() {
        ready = false;
        client = null;
        clients.forEach(TlsHttpClient::close);
        clients.clear();
    }
}
