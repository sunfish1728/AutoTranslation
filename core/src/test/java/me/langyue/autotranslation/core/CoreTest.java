package me.langyue.autotranslation.core;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipFile;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class CoreTest {
    @Test void formatsRoundTrip() {
        var protectedText = FormatProtector.protect("%1$s %1$.2f %08d §aok\\n%% {} {0}\nnext");
        assertEquals("%1$s %1$.2f %08d §aok\\n%% {} {0}\nnext", protectedText.restore(protectedText.text()));
        assertThrows(IllegalArgumentException.class, () -> protectedText.restore(protectedText.text().replace("[[AT0]]", "")));
        assertThrows(IllegalArgumentException.class, () -> protectedText.restore(protectedText.text() + "[[AT0]]"));
    }
    @Test void queueIsBoundedAndCanBeRetriggeredAfterFailure() throws Exception {
        CountDownLatch release = new CountDownLatch(1);
        try (var queue = new TranslationQueue(Runnable::run, 1, 1, Duration.ZERO)) {
            assertTrue(queue.trySubmit("one", value -> { try { release.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } return null; }, value -> fail()));
            assertFalse(queue.trySubmit("two", value -> "two", value -> fail()));
            release.countDown();
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(1);
            while (queue.pendingCount() != 0 && System.nanoTime() < deadline) Thread.onSpinWait();
            assertEquals(0, queue.pendingCount());
            assertTrue(queue.trySubmit("one", value -> null, value -> fail()));
        }
    }
    @Test void queueRetriesAndDeduplicates() throws Exception {
        AtomicInteger calls = new AtomicInteger(); CountDownLatch done = new CountDownLatch(1);
        try (var queue = new TranslationQueue(Runnable::run, 3, Duration.ofMillis(5))) {
            queue.submit("x", value -> calls.incrementAndGet() == 3 ? "ok" : null, value -> done.countDown());
            queue.submit("x", value -> "wrong", value -> fail());
            assertTrue(done.await(1, TimeUnit.SECONDS)); assertEquals(3, calls.get());
        }
    }
    @Test void atomicWriteKeepsBackup() throws Exception {
        var directory = Files.createTempDirectory("at-core"); var file = directory.resolve("cache.json");
        AtomicFileStore.writeUtf8(file, "one"); AtomicFileStore.writeUtf8(file, "two");
        assertEquals("two", Files.readString(file)); assertEquals("one", Files.readString(file.resolveSibling("cache.json.bak")));
    }
    @Test void rejectsPlainHttp() { assertThrows(IllegalArgumentException.class, () -> new SecureHttp().get(java.net.URI.create("http://example.test"), Duration.ofSeconds(1))); }
    @Test void translationBatchValidatesIdsAndTokens() {
        Map<String, String> source = new LinkedHashMap<>();
        source.put("key.one", "Hello %s");
        source.put("key.two", "Line one\nLine two");
        TranslationBatch batch = TranslationBatch.partition(source, 1024).get(0);
        assertEquals(source, batch.decode(batch.requestText()));
        assertThrows(IllegalArgumentException.class, () -> batch.decode(batch.requestText().replace("__AT_ITEM_000000__", "changed")));
        assertThrows(IllegalArgumentException.class, () -> batch.decode(batch.requestText().replace("[[AT0]]", "")));
    }
    @Test void writesDeterministicSafeResourcePack() throws Exception {
        Path directory = Files.createTempDirectory("at-pack");
        Path meta = directory.resolve("pack.mcmeta");
        Path lang = directory.resolve("zh_tw.json");
        Files.writeString(meta, "{}");
        Files.writeString(lang, "{\\\"key\\\":\\\"value\\\"}");
        Path target = directory.resolve("pack.zip");
        ResourcePackWriter.write(target, Map.of("pack.mcmeta", meta, "assets/example/lang/zh_tw.json", lang));
        try (ZipFile zip = new ZipFile(target.toFile())) {
            assertNotNull(zip.getEntry("pack.mcmeta"));
            assertNotNull(zip.getEntry("assets/example/lang/zh_tw.json"));
        }
        assertThrows(IllegalArgumentException.class, () -> ResourcePackWriter.write(target, Map.of("../escape", lang)));
    }
    @Test void filterHonoursLanguageNamespaceAndBlacklist() {
        TranslationFilter filter = new TranslationFilter("([A-Z]?[a-z]{2,}\\s*)+", "[\\u0800-\\u9fa5]+", Set.of("Minecraft"), Set.of("minecraft", "^fabric-.*"));
        assertFalse(filter.shouldTranslate("en_us", "key", "An English sentence", false));
        assertTrue(filter.shouldTranslate("zh_tw", "key", "An English sentence", false));
        assertFalse(filter.shouldTranslate("zh_tw", "key", "已翻譯 English", false));
        assertFalse(filter.shouldTranslate("zh_tw", "key", "Minecraft", false));
        assertTrue(filter.excludesNamespace("fabric-api"));
        assertFalse(filter.excludesNamespace("examplemod"));
    }
    @Test void fixedDnsIsHostScopedAndRejectsNonIpInput() throws Exception {
        TlsHttpClient.Resolver resolver = TlsHttpClient.fixedAddress("translate.example", "127.0.0.1");
        assertEquals("127.0.0.1", resolver.resolve("translate.example").getHostAddress());
        assertThrows(java.io.IOException.class, () -> resolver.resolve("other.example"));
        assertThrows(IllegalArgumentException.class, () -> TlsHttpClient.fixedAddress("translate.example", "localhost"));
        assertThrows(IllegalArgumentException.class, () -> TlsHttpClient.fixedAddress("127.0.0.1", "127.0.0.1"));
    }
    @Test void translationStorePublishesImmutableSnapshotsAndKeepsLegacyLayout() throws Exception {
        Path game = Files.createTempDirectory("at-store");
        TranslationStore store = new TranslationStore(game, "zh_tw");
        store.merge("example", Map.of("example.key", "翻譯"));
        assertEquals("翻譯", store.get("example.key"));
        assertTrue(Files.isRegularFile(game.resolve("AutoTranslation/example/zh_tw.json")));
        assertThrows(UnsupportedOperationException.class, () -> store.snapshot().put("bad", "value"));
        TranslationStore reloaded = new TranslationStore(game, "zh_tw");
        assertEquals("翻譯", reloaded.load("example").get("example.key"));
        assertThrows(IllegalArgumentException.class, () -> store.merge("../bad", Map.of()));
    }
    @Test void legacyConfigBackupIsOneTimeAndNonDestructive() throws Exception {
        Path game = Files.createTempDirectory("at-config");
        Path config = game.resolve("config/autotranslation.json5");
        Files.createDirectories(config.getParent());
        Files.writeString(config, "legacy");
        LegacyConfigBackup.createOnce(game);
        Path backup = config.resolveSibling("autotranslation.json5.pre-1.3.0.bak");
        assertEquals("legacy", Files.readString(backup));
        Files.writeString(config, "new");
        LegacyConfigBackup.createOnce(game);
        assertEquals("legacy", Files.readString(backup));
    }
    @Test void screenWhitelistPersistsAndBlacklistsEditorsByClassName() throws Exception {
        Path game = Files.createTempDirectory("at-screen");
        class ExampleScreen { }
        ExampleScreen screen = new ExampleScreen();
        try (ScreenTranslationState state = new ScreenTranslationState(game)) {
            assertTrue(state.toggle(screen));
            assertTrue(state.shouldTranslate(screen));
        }
        try (ScreenTranslationState restored = new ScreenTranslationState(game)) {
            assertTrue(restored.shouldTranslate(screen));
        }
    }
}
