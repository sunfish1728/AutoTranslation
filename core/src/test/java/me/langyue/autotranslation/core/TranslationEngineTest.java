package me.langyue.autotranslation.core;

import me.langyue.autotranslation.translate.ITranslator;
import me.langyue.autotranslation.translate.TranslatorManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class TranslationEngineTest {
    @AfterEach void resetRegistry() throws Exception {
        var method = TranslatorManager.class.getDeclaredMethod("clearForTests");
        method.setAccessible(true);
        method.invoke(null);
    }

    @Test void persistsValidatedResultBeforeClientCallback() throws Exception {
        TranslatorManager.registerTranslator("Google", () -> new ITranslator() {
            @Override public void init() { }
            @Override public int maxLength() { return 1000; }
            @Override public String translate(String text, String tl, String sl) { return text.replace("Hello", "您好"); }
        });
        TranslatorManager.setTranslator("Google");
        TranslationStore store = new TranslationStore(Files.createTempDirectory("at-engine"), "zh_tw");
        TranslationFilter filter = new TranslationFilter("([A-Z]?[a-z]{2,}\\s*)+", "[\\u0800-\\u9fa5]+", Set.of(), Set.of());
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<String> display = new AtomicReference<>();
        try (TranslationEngine engine = new TranslationEngine(new TranslationQueue(Runnable::run, 8, 1, Duration.ZERO), store, filter, true)) {
            assertNull(engine.translate("example", "example.key", "Hello world %s", value -> { display.set(value); done.countDown(); }));
            assertTrue(done.await(1, TimeUnit.SECONDS));
            assertTrue(display.get().contains("您好 world %s"));
            assertEquals("您好 world %s", store.get("example.key"));
            assertNotNull(engine.translate("example", "example.key", "Hello world %s", null));
        }
    }

    @Test void batchesMoreThanQueueCapacityWithoutDroppingKeys() throws Exception {
        AtomicReference<Integer> largestRequest = new AtomicReference<>(0);
        TranslatorManager.registerTranslator("Google", () -> new ITranslator() {
            @Override public void init() { }
            @Override public int maxLength() { return 1024; }
            @Override public String translate(String text, String tl, String sl) {
                largestRequest.accumulateAndGet(text.length(), Math::max);
                return text;
            }
        });
        TranslatorManager.setTranslator("Google");
        TranslationStore store = new TranslationStore(Files.createTempDirectory("at-engine-large"), "zh_tw");
        TranslationFilter filter = new TranslationFilter(".*[A-Za-z].*", "[\\u0800-\\u9fa5]+", Set.of(), Set.of());
        Map<String, String> source = new LinkedHashMap<>();
        for (int index = 0; index < 2500; index++) {
            source.put("example.key." + index, "English sentence number " + index);
        }
        CountDownLatch done = new CountDownLatch(1);
        try (TranslationEngine engine = new TranslationEngine(new TranslationQueue(Runnable::run, 1, 1, Duration.ZERO), store, filter, false)) {
            assertTrue(engine.translateBatch("example", source, done::countDown));
            assertTrue(done.await(15, TimeUnit.SECONDS));
            assertEquals(2500, store.snapshot().size());
            assertTrue(largestRequest.get() <= 1024);
        }
    }
}
