package me.langyue.autotranslation.client1201;

import me.langyue.autotranslation.core.TranslationFilter;
import me.langyue.autotranslation.core.TranslationQueue;
import me.langyue.autotranslation.translate.ITranslator;
import java.util.function.Consumer;

/** Returns only String display copies. It never mutates a Minecraft Component or tooltip instance. */
public final class ClientTranslator implements AutoCloseable {
    private final ClientTranslationCache cache;
    private final TranslationQueue queue;
    private final ITranslator translator;
    private final String language; private final TranslationFilter filter;
    public ClientTranslator(ClientTranslationCache cache, TranslationQueue queue, ITranslator translator, String language, TranslationFilter filter) { this.cache=cache; this.queue=queue; this.translator=translator; this.language=language; this.filter=filter; }
    public String translate(String key, String original, Consumer<String> onClientResult) {
        String cached = cache.get(key); if (cached != null) return cached;
        if (!filter.shouldTranslate(language, key, original, false)) return original;
        queue.trySubmit(key, ignored -> translator.ready() ? translator.translate(original, language, "en") : null, translated -> {
            cache.put(key, translated);
            if (onClientResult != null) onClientResult.accept(translated);
        });
        return original;
    }
    @Override public void close() { try { cache.save(); } catch (Exception ignored) { } queue.close(); if (translator instanceof AutoCloseable closeable) try { closeable.close(); } catch (Exception ignored) { } }
}
