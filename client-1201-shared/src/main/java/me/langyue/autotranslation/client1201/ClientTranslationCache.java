package me.langyue.autotranslation.client1201;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import me.langyue.autotranslation.core.AtomicFileStore;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Persistent client-only cache. Writes are atomic and retain the previous file as .bak. */
public final class ClientTranslationCache {
    private static final Gson GSON = new Gson();
    private static final Type MAP = new TypeToken<Map<String, String>>() { }.getType();
    private final Path file;
    private final Map<String, String> values = new ConcurrentHashMap<>();
    public ClientTranslationCache(Path gameDirectory, String language) {
        file = gameDirectory.resolve("AutoTranslation").resolve("_at_store").resolve(language + ".json"); load();
    }
    public String get(String key) { return values.get(key); }
    public void put(String key, String value) { if (key != null && value != null) values.put(key, value); }
    public boolean contains(String key) { return values.containsKey(key); }
    public void save() throws IOException { AtomicFileStore.writeUtf8(file, GSON.toJson(values)); }
    private void load() {
        if (!Files.isRegularFile(file)) return;
        try { Map<String,String> disk = GSON.fromJson(Files.readString(file), MAP); if (disk != null) values.putAll(disk); } catch (Exception ignored) { }
    }
}
