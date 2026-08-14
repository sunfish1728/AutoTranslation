package me.langyue.autotranslation.neoforge;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import me.langyue.autotranslation.core.ResourcePackWriter;
import me.langyue.autotranslation.core.TranslationStore;

/** Writes the mandatory post-format-64 min/max metadata without changing older loader roots. */
final class NeoForgeResourcePack {
    private final TranslationStore store;
    private final Path root;
    private final int packFormat;

    NeoForgeResourcePack(TranslationStore store, int packFormat) {
        this.store = store;
        this.root = store.root();
        this.packFormat = packFormat;
    }

    Path pack(boolean increment, Set<String> incrementNamespaces) throws IOException {
        List<String> namespaces = namespaces(increment, incrementNamespaces);
        if (namespaces.isEmpty()) throw new NoSuchFileException("No translated language files");
        Files.createDirectories(root);
        String language = store.language();
        Path output = root.resolve("AutoTranslation." + language + (increment ? ".Increment." : ".Full.")
                + Instant.now().toEpochMilli() + ".zip");
        Map<String, Path> entries = new LinkedHashMap<>();
        for (String namespace : namespaces) {
            entries.put("assets/" + namespace + "/lang/" + language + ".json", store.languageFile(namespace));
        }
        Path metadata = Files.createTempFile("autotranslation-pack", ".mcmeta");
        try {
            Files.writeString(metadata, "{\"pack\":{\"min_format\":[" + packFormat
                    + ",0],\"max_format\":[" + packFormat + ",0],\"description\":\"AutoTranslation\"}}");
            entries.put("pack.mcmeta", metadata);
            ResourcePackWriter.write(output, entries);
        } finally {
            Files.deleteIfExists(metadata);
        }
        return output;
    }

    private List<String> namespaces(boolean increment, Set<String> incrementNamespaces) throws IOException {
        List<String> result = new ArrayList<>();
        if (!Files.isDirectory(root)) return result;
        Set<String> requested = increment ? new TreeSet<>(incrementNamespaces) : Set.of();
        try (var entries = Files.list(root)) {
            entries.filter(Files::isDirectory).forEach(path -> {
                String namespace = path.getFileName().toString();
                if (!namespace.equals(TranslationStore.NO_KEY_NAMESPACE)
                        && (!increment || requested.contains(namespace))
                        && Files.isRegularFile(path.resolve(store.language() + ".json"))) {
                    result.add(namespace);
                }
            });
        }
        result.sort(String::compareTo);
        return result;
    }
}
