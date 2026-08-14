package me.langyue.autotranslation.translate;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Thread-safe registry for the public translator API. */
public class TranslatorManager {
    public static final String DEFAULT_TRANSLATOR = "Google";

    private static final Map<String, Supplier<ITranslator>> FACTORIES = new LinkedHashMap<>();
    private static final Map<String, ITranslator> INSTANCES = new ConcurrentHashMap<>();
    private static volatile String selectedName = DEFAULT_TRANSLATOR;
    private static volatile String requestedName = DEFAULT_TRANSLATOR;
    private static volatile Consumer<String> warningSink = message -> { };

    public TranslatorManager() { }

    /** Legacy 1.2.x entry point retained for binary compatibility. */
    public static synchronized void init() { setTranslator(selectedName); }

    public static synchronized void registerTranslator(String name, Supplier<ITranslator> getInstance) {
        String normalized = normalizeName(name);
        Objects.requireNonNull(getInstance, "getInstance");
        FACTORIES.put(normalized, getInstance);
        ITranslator replaced = INSTANCES.remove(normalized);
        if (replaced instanceof AutoCloseable closeable) {
            try { closeable.close(); } catch (Exception ignored) { }
        }
        if (normalized.equals(requestedName)) {
            selectedName = normalized;
            instantiate(normalized);
        }
    }

    public static void setWarningSink(Consumer<String> sink) {
        warningSink = Objects.requireNonNull(sink, "sink");
    }

    /** Selects a translator, falling back to Google for blank or unknown values. */
    public static synchronized void setTranslator(String requestedName) {
        String requested = requestedName == null ? "" : requestedName.trim();
        TranslatorManager.requestedName = requested;
        String resolved = FACTORIES.containsKey(requested) ? requested : DEFAULT_TRANSLATOR;
        if (!resolved.equals(requested)) {
            warningSink.accept("Unknown translator '" + requested + "'; falling back to " + DEFAULT_TRANSLATOR);
        }
        selectedName = resolved;
        instantiate(resolved);
    }

    public static ITranslator getTranslator() {
        ITranslator selected = getTranslator(selectedName);
        if (selected != null) return selected;
        return getTranslator(DEFAULT_TRANSLATOR);
    }

    public static synchronized ITranslator getTranslator(String name) {
        if (name == null || name.isBlank()) return null;
        return instantiate(name.trim());
    }

    public static String selectedName() { return selectedName; }

    /** Releases HTTP clients registered by the client lifecycle before exit. */
    public static synchronized void closeAll() {
        INSTANCES.values().forEach(instance -> {
            if (instance instanceof AutoCloseable closeable) {
                try { closeable.close(); } catch (Exception ignored) { }
            }
        });
        INSTANCES.clear();
    }

    static synchronized void clearForTests() {
        FACTORIES.clear();
        INSTANCES.clear();
        selectedName = DEFAULT_TRANSLATOR;
        requestedName = DEFAULT_TRANSLATOR;
        warningSink = message -> { };
    }

    private static ITranslator instantiate(String name) {
        ITranslator existing = INSTANCES.get(name);
        if (existing != null) return existing;
        Supplier<ITranslator> factory = FACTORIES.get(name);
        if (factory == null) return null;
        ITranslator created;
        try {
            created = factory.get();
            if (created == null) {
                warningSink.accept("Translator factory returned no instance: " + name);
                return null;
            }
            created.init();
        } catch (RuntimeException failure) {
            warningSink.accept("Translator failed to initialize: " + name);
            return null;
        }
        INSTANCES.put(name, created);
        return created;
    }

    private static String normalizeName(String name) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("translator name must not be blank");
        return name.trim();
    }
}
