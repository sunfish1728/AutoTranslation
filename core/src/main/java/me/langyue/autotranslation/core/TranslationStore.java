package me.langyue.autotranslation.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/** Persistent namespace/language JSON files with an immutable render-thread snapshot. */
public final class TranslationStore {
    public static final String NO_KEY_NAMESPACE = "_at_store";
    public static final String REFERENCE_FILE = "_ref.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path root;
    private final String language;
    private final Map<String, Map<String, String>> namespaces = new ConcurrentHashMap<>();
    private final AtomicReference<Map<String, String>> renderSnapshot = new AtomicReference<>(Map.of());

    public TranslationStore(Path gameDirectory, String language) {
        this.root = Objects.requireNonNull(gameDirectory, "gameDirectory").resolve("AutoTranslation");
        this.language = validateLanguage(language);
    }

    public Path root() { return root; }
    public String language() { return language; }
    public Map<String, String> snapshot() { return renderSnapshot.get(); }
    public String get(String key) { return renderSnapshot.get().get(key); }

    public synchronized Map<String, String> load(String namespace) throws IOException {
        String validated = validateNamespace(namespace);
        Path file = languageFile(validated);
        if (!Files.isRegularFile(file)) throw new NoSuchFileException(file.toString());
        Map<String, String> loaded = readStringMap(file);
        namespaces.put(validated, Collections.unmodifiableMap(loaded));
        publishSnapshot();
        return namespaces.get(validated);
    }

    public synchronized void unload(String namespace) {
        namespaces.remove(validateNamespace(namespace));
        publishSnapshot();
    }

    public synchronized void merge(String namespace, Map<String, String> translations) throws IOException {
        String validated = validateNamespace(namespace);
        Path file = languageFile(validated);
        Map<String, String> merged = Files.isRegularFile(file) ? readStringMap(file) : new LinkedHashMap<>();
        for (Map.Entry<String, String> translation : translations.entrySet()) {
            if (translation.getKey() != null && translation.getValue() != null) merged.put(translation.getKey(), translation.getValue());
        }
        AtomicFileStore.writeUtf8(file, GSON.toJson(merged));
        namespaces.put(validated, Collections.unmodifiableMap(new LinkedHashMap<>(merged)));
        publishSnapshot();
    }

    public synchronized void writeReference(String namespace, Map<String, String> sourceValues) throws IOException {
        Path file = root.resolve(validateNamespace(namespace)).resolve(REFERENCE_FILE);
        AtomicFileStore.writeUtf8(file, GSON.toJson(sourceValues));
    }

    public Path languageFile(String namespace) { return root.resolve(validateNamespace(namespace)).resolve(language + ".json"); }

    private void publishSnapshot() {
        Map<String, String> flattened = new LinkedHashMap<>();
        new java.util.TreeMap<>(namespaces).values().forEach(flattened::putAll);
        renderSnapshot.set(Collections.unmodifiableMap(flattened));
    }

    private static Map<String, String> readStringMap(Path file) throws IOException {
        JsonElement parsed;
        try { parsed = JsonParser.parseString(Files.readString(file)); }
        catch (RuntimeException malformed) { throw new IOException("Invalid translation JSON: " + file, malformed); }
        if (!parsed.isJsonObject()) throw new IOException("Translation JSON must be an object: " + file);
        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : parsed.getAsJsonObject().entrySet()) {
            if (!entry.getValue().isJsonPrimitive() || !entry.getValue().getAsJsonPrimitive().isString()) {
                throw new IOException("Translation JSON values must be strings: " + file);
            }
            result.put(entry.getKey(), entry.getValue().getAsString());
        }
        return result;
    }

    private static String validateNamespace(String namespace) {
        if (namespace == null || !namespace.matches("[a-z0-9_.-]+") || namespace.equals(".") || namespace.equals("..")) {
            throw new IllegalArgumentException("Invalid namespace");
        }
        return namespace;
    }

    private static String validateLanguage(String language) {
        if (language == null || !language.matches("[a-z0-9_-]+")) throw new IllegalArgumentException("Invalid language code");
        return language;
    }
}
