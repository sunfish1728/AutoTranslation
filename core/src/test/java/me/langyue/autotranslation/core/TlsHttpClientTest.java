package me.langyue.autotranslation.core;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.SocketPolicy;
import okhttp3.tls.HandshakeCertificates;
import okhttp3.tls.HeldCertificate;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TlsHttpClientTest {
    @Test void overrideIpStillUsesOriginalHostForSniAndCertificate() throws Exception {
        HeldCertificate certificate = certificateFor("translate.test");
        try (MockWebServer server = server(certificate)) {
            HandshakeCertificates clientTrust = new HandshakeCertificates.Builder()
                    .addTrustedCertificate(certificate.certificate()).build();
            try (TlsHttpClient client = clientFor(server, "translate.test", clientTrust)) {
                TlsHttpClient.Response response = client.get(endpoint(server, "translate.test"), Duration.ofSeconds(3));
                assertEquals("ok", response.bodyUtf8());
                assertEquals("translate.test", response.connectionKey().domain());
                assertEquals("127.0.0.1", response.connectionKey().resolvedIp());
            }
        }
    }

    @Test void defaultJvmTrustRejectsSelfSignedCertificate() throws Exception {
        HeldCertificate certificate = certificateFor("translate.test");
        try (MockWebServer server = server(certificate);
             TlsHttpClient client = new TlsHttpClient(TlsHttpClient.fixedAddress("translate.test", "127.0.0.1"), Duration.ofSeconds(2), 1024)) {
            assertThrows(Exception.class, () -> client.get(endpoint(server, "translate.test"), Duration.ofSeconds(3)));
        }
    }

    @Test void endpointIdentificationRejectsWrongHostname() throws Exception {
        HeldCertificate certificate = certificateFor("wrong.test");
        try (MockWebServer server = server(certificate)) {
            HandshakeCertificates clientTrust = new HandshakeCertificates.Builder()
                    .addTrustedCertificate(certificate.certificate()).build();
            try (TlsHttpClient client = clientFor(server, "translate.test", clientTrust)) {
                assertThrows(Exception.class, () -> client.get(endpoint(server, "translate.test"), Duration.ofSeconds(3)));
            }
        }
    }

    @Test void certificateValidityIsEnforced() throws Exception {
        HeldCertificate authority = new HeldCertificate.Builder().commonName("AutoTranslation test CA").certificateAuthority(1).build();
        HeldCertificate expired = new HeldCertificate.Builder()
                .commonName("translate.test")
                .addSubjectAlternativeName("translate.test")
                .signedBy(authority)
                .validityInterval(Instant.parse("2020-01-01T00:00:00Z").toEpochMilli(), Instant.parse("2020-01-02T00:00:00Z").toEpochMilli())
                .build();
        try (MockWebServer server = server(expired, authority)) {
            HandshakeCertificates clientTrust = new HandshakeCertificates.Builder()
                    .addTrustedCertificate(authority.certificate()).build();
            try (TlsHttpClient client = clientFor(server, "translate.test", clientTrust)) {
                assertThrows(Exception.class, () -> client.get(endpoint(server, "translate.test"), Duration.ofSeconds(3)));
            }
        }
    }

    @Test void closeCancelsAnInFlightRead() throws Exception {
        HeldCertificate certificate = certificateFor("translate.test");
        HandshakeCertificates serverCertificates = new HandshakeCertificates.Builder().heldCertificate(certificate).build();
        try (MockWebServer server = new MockWebServer()) {
            server.useHttps(serverCertificates.sslSocketFactory(), false);
            server.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE));
            server.start(java.net.InetAddress.getByName("127.0.0.1"), 0);
            HandshakeCertificates clientTrust = new HandshakeCertificates.Builder()
                    .addTrustedCertificate(certificate.certificate()).build();
            TlsHttpClient client = clientFor(server, "translate.test", clientTrust);
            CompletableFuture<TlsHttpClient.Response> request = CompletableFuture.supplyAsync(() -> {
                try {
                    return client.get(endpoint(server, "translate.test"), Duration.ofSeconds(30));
                } catch (Exception failure) {
                    throw new RuntimeException(failure);
                }
            });
            assertNotNull(server.takeRequest(2, TimeUnit.SECONDS));
            client.close();
            assertThrows(Exception.class, () -> request.get(2, TimeUnit.SECONDS));
        }
    }

    private static HeldCertificate certificateFor(String hostname) {
        return new HeldCertificate.Builder().commonName(hostname).addSubjectAlternativeName(hostname).build();
    }

    private static MockWebServer server(HeldCertificate certificate) throws Exception {
        return server(certificate, null);
    }

    private static MockWebServer server(HeldCertificate certificate, HeldCertificate authority) throws Exception {
        HandshakeCertificates.Builder serverBuilder = new HandshakeCertificates.Builder();
        if (authority == null) serverBuilder.heldCertificate(certificate);
        else serverBuilder.heldCertificate(certificate, authority.certificate());
        HandshakeCertificates serverCertificates = serverBuilder.build();
        MockWebServer server = new MockWebServer();
        server.useHttps(serverCertificates.sslSocketFactory(), false);
        server.enqueue(new MockResponse().setResponseCode(200).setHeader("Content-Type", "text/plain; charset=utf-8").setBody("ok"));
        server.start(java.net.InetAddress.getByName("127.0.0.1"), 0);
        return server;
    }

    private static TlsHttpClient clientFor(MockWebServer server, String hostname, HandshakeCertificates trust) {
        return new TlsHttpClient(TlsHttpClient.fixedAddress(hostname, "127.0.0.1"), trust.sslSocketFactory(), Duration.ofSeconds(2), 1024);
    }

    private static URI endpoint(MockWebServer server, String hostname) {
        return URI.create("https://" + hostname + ":" + server.getPort() + "/translate");
    }
}
