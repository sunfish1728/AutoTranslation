package me.langyue.autotranslation.neoforge;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import me.langyue.autotranslation.client1201.ClientConfig;
import me.langyue.autotranslation.core.TranslationFilter;
import me.langyue.autotranslation.core.TranslationStore;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/** Reads language stacks at the reload boundary without replacing vanilla language loading. */
final class LanguageDiscovery {
    private LanguageDiscovery() { }

    static Map<String, Map<String, String>> discover(ResourceManager resources, String targetLanguage, ClientConfig config,
                                                       TranslationFilter filter, TranslationStore store) {
        if ("en_us".equalsIgnoreCase(targetLanguage)) return Map.of();
        Map<String, Map<String, String>> discovered = new LinkedHashMap<>();
        for (String namespace : resources.getNamespaces()) {
            if (filter.excludesNamespace(namespace)) continue;
            try {
                Map<String, String> english = load(resources, namespace, "en_us");
                Map<String, String> target = load(resources, namespace, targetLanguage);
                Map<String, String> references = new LinkedHashMap<>();
                for (Map.Entry<String, String> entry : english.entrySet()) {
                    boolean absent = !target.containsKey(entry.getKey());
                    boolean correction = config.mode == ClientConfig.FilterMode.CORRECTION
                            && entry.getValue().equals(target.get(entry.getKey()));
                    if ((absent || correction) && filter.shouldTranslate(targetLanguage, entry.getKey(), entry.getValue(), store.get(entry.getKey()) != null)) {
                        references.put(entry.getKey(), entry.getValue());
                    }
                }
                if (!references.isEmpty()) {
                    store.writeReference(namespace, references);
                    try { store.load(namespace); } catch (java.io.IOException missingFile) { /* translation output is created asynchronously */ }
                    discovered.put(namespace, Map.copyOf(references));
                }
            } catch (Exception malformedPack) {
                System.err.println("[AutoTranslation] Skipped malformed language resources for namespace " + namespace);
            }
        }
        return Map.copyOf(discovered);
    }

    private static Map<String, String> load(ResourceManager resources, String namespace, String language) throws Exception {
        Identifier location = Identifier.fromNamespaceAndPath(namespace, "lang/" + language + ".json");
        Map<String, String> values = new LinkedHashMap<>();
        for (Resource resource : resources.getResourceStack(location)) {
            try (var reader = new InputStreamReader(resource.open(), StandardCharsets.UTF_8)) {
                JsonElement parsed = JsonParser.parseReader(reader);
                if (!parsed.isJsonObject()) throw new IllegalArgumentException("Language resource must be an object");
                parsed.getAsJsonObject().entrySet().forEach(entry -> {
                    if (entry.getValue().isJsonPrimitive() && entry.getValue().getAsJsonPrimitive().isString()) {
                        values.put(entry.getKey(), entry.getValue().getAsString());
                    }
                });
            }
        }
        return values;
    }
}
