package me.langyue.autotranslation.client1201;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.time.Duration;
import java.util.Objects;

import me.langyue.autotranslation.core.Platform;
import me.langyue.autotranslation.core.TranslationEngine;
import me.langyue.autotranslation.core.TranslationFilter;
import me.langyue.autotranslation.core.TranslationQueue;
import me.langyue.autotranslation.core.TranslationStore;
import me.langyue.autotranslation.translate.TranslatorManager;
import me.langyue.autotranslation.translate.google.GoogleTranslator;

/**
 * Loader-neutral client lifecycle.  Its public rendering method accepts and
 * returns Strings only; Minecraft Components, tooltips, and packets never
 * cross this boundary.
 */
public final class ClientTranslationRuntime implements AutoCloseable {
    private static final String FALLBACK_NAMESPACE = "autotranslation";
    private static volatile ClientTranslationRuntime active;

    private final Platform platform;
    private final ClientConfig config;
    private final int packFormat;
    private final String language;
    private final ClientResourceStore resources;
    private final TranslationEngine engine;
    private final ScreenTranslationState screens;

    private ClientTranslationRuntime(Platform platform, ClientConfig config, String language, int packFormat) {
        this.platform = Objects.requireNonNull(platform, "platform");
        this.config = Objects.requireNonNull(config, "config");
        this.language = Objects.requireNonNull(language, "language");
        this.packFormat = packFormat;
        TranslationStore store = new TranslationStore(platform.gameDirectory(), language);
        this.resources = new ClientResourceStore(store, packFormat);
        this.screens = new ScreenTranslationState(platform.gameDirectory(), config.ignoreOriginalScreen);
        TranslationFilter filter = new TranslationFilter(config.enFeature, config.yourLanguageFeature,
                config.wordBlacklist, config.excludedNamespace);
        TranslatorManager.registerTranslator(TranslatorManager.DEFAULT_TRANSLATOR,
                () -> new GoogleTranslator(config.google.domain, config.google.dns));
        TranslatorManager.setTranslator(config.translator);
        this.engine = new TranslationEngine(new TranslationQueue(platform::runOnClient, 1024, 3,
                Duration.ofMillis(250)), store, filter, config.appendOriginal);
        loadPersisted();
    }

    public static synchronized ClientTranslationRuntime start(Platform platform, ClientConfig config,
                                                               String language, int packFormat) {
        closeActive();
        active = new ClientTranslationRuntime(platform, config, language, packFormat);
        return active;
    }

    /** Called only from a client-language reload hook. */
    public static synchronized void onLanguageReload(String language) {
        ClientTranslationRuntime current = active;
        if (current == null) return;
        if (!current.language.equals(language)) {
            start(current.platform, current.config, language, current.packFormat);
        } else {
            current.loadPersisted();
        }
    }

    public static String displayCopy(String key, String original) {
        ClientTranslationRuntime current = active;
        if (current == null || key == null || original == null) return original;
        String translated = current.engine.translate(FALLBACK_NAMESPACE, key, original,
                ignored -> DisplayTranslations.replace(current.engine.cacheSnapshot()));
        return translated == null ? original : translated;
    }

    /** Opt-in literal screen text, gated by a persisted per-screen whitelist. */
    public static String screenDisplayCopy(String screenClass, String original) {
        ClientTranslationRuntime current = active;
        if (current == null || original == null || !current.screens.enabled(screenClass)) return original;
        String translated = current.engine.translate(FALLBACK_NAMESPACE, original, original,
                ignored -> DisplayTranslations.replace(current.engine.cacheSnapshot()));
        return translated == null ? original : translated;
    }

    public static boolean toggleScreen(String screenClass) {
        ClientTranslationRuntime current = active;
        return current != null && current.screens.toggle(screenClass);
    }

    public static boolean screenEnabled(String screenClass) {
        ClientTranslationRuntime current = active;
        return current != null && current.screens.enabled(screenClass);
    }

    public static boolean screenAllowed(String screenClass) {
        ClientTranslationRuntime current = active;
        return current != null && current.screens.allowed(screenClass);
    }

    public static ClientConfig screenConfig() {
        ClientTranslationRuntime current = active;
        return current == null ? null : current.config;
    }

    public ClientResourceStore resources() { return resources; }

    /**
     * Receives a resource-language entry from the native reload hook.  The
     * hook only reads pack JSON; rendering remains an immutable String copy.
     */
    public static void discover(String namespace, String key, String english, String targetValue) {
        ClientTranslationRuntime current = active;
        if (current == null || current.config.mode == ClientConfig.FilterMode.RESOURCE && targetValue != null) return;
        if (namespace == null || current.engine == null) return;
        TranslationFilter filter = new TranslationFilter(current.config.enFeature, current.config.yourLanguageFeature,
                current.config.wordBlacklist, current.config.excludedNamespace);
        if (filter.excludesNamespace(namespace)) return;
        // Correction mode only replaces a missing translation or one still equal to English.
        if (current.config.mode == ClientConfig.FilterMode.CORRECTION && targetValue != null && !english.equals(targetValue)) return;
        current.engine.translate(namespace, key, english,
                ignored -> DisplayTranslations.replace(current.engine.cacheSnapshot()));
    }

    public static void discoverBatch(String namespace, java.util.Map<String, String> english,
                                     java.util.Map<String, String> target) {
        ClientTranslationRuntime current = active;
        if (current == null || namespace == null) return;
        TranslationFilter filter = new TranslationFilter(current.config.enFeature, current.config.yourLanguageFeature,
                current.config.wordBlacklist, current.config.excludedNamespace);
        if (filter.excludesNamespace(namespace)) return;
        java.util.Map<String, String> missing = new java.util.LinkedHashMap<>();
        english.forEach((key, value) -> {
            String targetValue = target.get(key);
            boolean absent = targetValue == null;
            boolean correction = current.config.mode == ClientConfig.FilterMode.CORRECTION && value.equals(targetValue);
            if ((absent || correction) && filter.shouldTranslate(current.language, key, value,
                    current.engine.cacheSnapshot().containsKey(key))) missing.put(key, value);
        });
        current.engine.translateBatch(namespace, missing,
                () -> DisplayTranslations.replace(current.engine.cacheSnapshot()));
    }

    private void loadPersisted() {
        try {
            resources.reload(null);
        } catch (NoSuchFileException ignored) {
            DisplayTranslations.replace(engine.cacheSnapshot());
        } catch (IOException ignored) {
            // A damaged optional cache must not abort Minecraft's resource reload.
            DisplayTranslations.replace(engine.cacheSnapshot());
        }
    }

    public static synchronized void closeActive() {
        ClientTranslationRuntime current = active;
        active = null;
        if (current != null) current.close();
    }

    @Override public void close() {
        engine.close();
        TranslatorManager.closeAll();
        DisplayTranslations.replace(java.util.Map.of());
    }
}
