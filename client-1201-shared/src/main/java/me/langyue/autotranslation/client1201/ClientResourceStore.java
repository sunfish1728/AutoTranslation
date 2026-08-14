package me.langyue.autotranslation.client1201;

import java.io.IOException;
import java.nio.file.*;
import me.langyue.autotranslation.core.ResourcePackWriter;
import me.langyue.autotranslation.core.TranslationStore;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

/** Client-owned persisted translations and resource-pack writer; no server state is touched. */
public final class ClientResourceStore {
    private final TranslationStore store;
    private final Path root;
    private final String language;
    private final int packFormat;
    public ClientResourceStore(Path gameDirectory, String language, int packFormat) {
        this(new TranslationStore(gameDirectory, language), packFormat);
    }

    ClientResourceStore(TranslationStore store, int packFormat) {
        this.store = store;
        root = store.root();
        language = store.language();
        this.packFormat = packFormat;
    }
    public void reload(String namespace) throws IOException {
        if (namespace == null) {
            for (String candidate : namespaces()) reload(candidate);
            DisplayTranslations.replace(store.snapshot());
            return;
        }
        if (!namespace.matches("[a-z0-9_.-]+")) throw new IllegalArgumentException("Invalid namespace");
        store.load(namespace);
        DisplayTranslations.replace(store.snapshot());
    }
    TranslationStore translationStore() { return store; }
    public Map<String, String> snapshot() { return store.snapshot(); }
    public boolean hasIncrement() throws IOException { return !namespaces().isEmpty(); }
    public Path pack(boolean increment) throws IOException {
        List<String> namespaces = namespaces();
        if (namespaces.isEmpty()) throw new NoSuchFileException("No translated language files");
        Files.createDirectories(root);
        String name = "AutoTranslation." + language + (increment ? ".Increment" : ".Full") + "." + Instant.now().toEpochMilli() + ".zip";
        Path output = root.resolve(name);
        Map<String,Path> entries = new LinkedHashMap<>();
        for (String namespace : namespaces) entries.put("assets/" + namespace + "/lang/" + language + ".json", store.languageFile(namespace));
        Path metadata = Files.createTempFile("autotranslation-pack", ".mcmeta");
        try {
            Files.writeString(metadata, "{\"pack\":{\"pack_format\":" + packFormat + ",\"description\":\"AutoTranslation\"}}");
            entries.put("pack.mcmeta", metadata); ResourcePackWriter.write(output, entries);
        } finally { Files.deleteIfExists(metadata); }
        return output;
    }
    private List<String> namespaces() throws IOException {
        List<String> result = new ArrayList<>();
        if (!Files.isDirectory(root)) return result;
        try (var entries = Files.list(root)) {
            entries.filter(Files::isDirectory).forEach(path -> { if (Files.isRegularFile(path.resolve(language + ".json"))) result.add(path.getFileName().toString()); });
        }
        return result;
    }
}
