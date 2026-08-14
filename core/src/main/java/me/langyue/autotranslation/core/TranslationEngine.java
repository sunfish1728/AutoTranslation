package me.langyue.autotranslation.core;

import me.langyue.autotranslation.translate.ITranslator;
import me.langyue.autotranslation.translate.TranslatorManager;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.LinkedHashMap;
import java.util.List;

/** Coordinates immutable cache reads, background translation, validation, and persistence. */
public final class TranslationEngine implements AutoCloseable {
    private final TranslationQueue queue;
    private final TranslationStore store;
    private volatile TranslationFilter filter;
    private volatile boolean appendOriginal;

    public TranslationEngine(TranslationQueue queue, TranslationStore store, TranslationFilter filter, boolean appendOriginal) {
        this.queue = Objects.requireNonNull(queue, "queue");
        this.store = Objects.requireNonNull(store, "store");
        this.filter = Objects.requireNonNull(filter, "filter");
        this.appendOriginal = appendOriginal;
    }

    public void updateRules(TranslationFilter filter, boolean appendOriginal) {
        this.filter = Objects.requireNonNull(filter, "filter");
        this.appendOriginal = appendOriginal;
    }

    /**
     * Returns an already cached display value, otherwise schedules work and
     * returns null. The callback is always dispatched through the queue's
     * client executor and receives a new display string, never a mutable game object.
     */
    public String translate(String namespace, String key, String source, Consumer<String> onReady) {
        String cached = store.get(key);
        if (cached != null) return append(cached, source);
        if (!filter.shouldTranslate(store.language(), key, source, false)) return null;
        queue.trySubmit(qualifiedKey(namespace, key), ignored -> translateAndPersist(namespace, key, source), translated -> {
            if (onReady != null) onReady.accept(append(translated, source));
        });
        return null;
    }

    public Map<String, String> cacheSnapshot() { return store.snapshot(); }

    /**
     * Translates resource values in validated batches. Keys never leave the
     * process; stable batch IDs detect drops, reordering, or duplication.
     * Returns false if bounded admission rejects any batch, allowing a later
     * resource reload to explicitly retrigger the missing work.
     */
    public boolean translateBatch(String namespace, Map<String, String> sourceValues, Runnable onReady) {
        Objects.requireNonNull(namespace, "namespace");
        Objects.requireNonNull(sourceValues, "sourceValues");
        ITranslator translator = TranslatorManager.getTranslator();
        if (translator == null || !translator.ready() || sourceValues.isEmpty()) return false;
        Map<String, String> uncached = new LinkedHashMap<>();
        sourceValues.forEach((key, value) -> {
            if (store.get(key) == null && filter.shouldTranslate(store.language(), key, value, false)) uncached.put(key, value);
        });
        if (uncached.isEmpty()) return true;
        List<TranslationBatch> batches = TranslationBatch.partition(uncached, translator.maxLength());
        // One bounded queue slot represents the entire resource batch. This
        // prevents a large pack (thousands of missing keys) from overflowing
        // the queue merely because it must be partitioned into many HTTP
        // requests. Each request still respects the translator's maxLength.
        String workKey = "batch\0" + namespace + "\0" + uncached.hashCode();
        return queue.trySubmit(workKey, ignored -> translateBatchesAndPersist(namespace, translator, batches), ignored -> {
            if (onReady != null) onReady.run();
        });
    }

    private String translateAndPersist(String namespace, String key, String source) {
        ITranslator translator = TranslatorManager.getTranslator();
        if (translator == null || !translator.ready()) return null;
        try {
            FormatProtector.ProtectedText protectedText = FormatProtector.protect(source);
            String response = translator.translate(protectedText.text(), store.language(), "en");
            if (response == null || response.isBlank()) return null;
            String restored = protectedText.restore(response);
            store.merge(namespace == null ? TranslationStore.NO_KEY_NAMESPACE : namespace, Map.of(key, restored));
            return restored;
        } catch (IOException | IllegalArgumentException failure) {
            return null;
        }
    }

    private String translateBatchesAndPersist(String namespace, ITranslator translator, List<TranslationBatch> batches) {
        for (TranslationBatch batch : batches) {
            try {
                String response = translator.translate(batch.requestText(), store.language(), "en");
                if (response == null || response.isBlank()) return null;
                store.merge(namespace, batch.decode(response));
            } catch (IOException | IllegalArgumentException failure) {
                return null;
            }
        }
        return "ok";
    }

    private static String qualifiedKey(String namespace, String key) {
        return Objects.requireNonNullElse(namespace, TranslationStore.NO_KEY_NAMESPACE) + '\0' + key;
    }

    private String append(String translation, String original) {
        if (!appendOriginal || original == null) return translation;
        String[] translatedLines = translation.split("\\R", -1);
        String[] originalLines = original.split("\\R", -1);
        if (translatedLines.length != originalLines.length) return translation + " §7* (" + escapePercent(original) + ")";
        StringBuilder display = new StringBuilder();
        for (int index = 0; index < translatedLines.length; index++) {
            if (index > 0) display.append('\n');
            display.append(translatedLines[index]).append(" §7* (").append(escapePercent(originalLines[index])).append(')');
        }
        return display.toString();
    }

    private static String escapePercent(String value) { return value.replace("%", "%%"); }

    @Override public void close() { queue.close(); }
}
