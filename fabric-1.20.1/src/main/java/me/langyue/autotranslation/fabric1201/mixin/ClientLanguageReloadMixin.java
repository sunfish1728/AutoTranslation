package me.langyue.autotranslation.fabric1201.mixin;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.langyue.autotranslation.client1201.ClientTranslationRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.ClientLanguage;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Reloads only the persisted immutable translation snapshot after language data is rebuilt. */
@Mixin(ClientLanguage.class)
abstract class ClientLanguageReloadMixin {
    @Inject(method = "loadFrom", at = @At("RETURN"))
    private static void autotranslation$reloadSaved(ResourceManager manager, List<String> languages, boolean rtl,
                                                    CallbackInfoReturnable<ClientLanguage> result) {
        ClientTranslationRuntime.onLanguageReload(Minecraft.getInstance().options.languageCode);
        String targetLanguage = Minecraft.getInstance().options.languageCode;
        if ("en_us".equalsIgnoreCase(targetLanguage)) return;
        for (String namespace : manager.getNamespaces()) {
            Map<String, String> english = read(manager, namespace, "en_us");
            Map<String, String> target = read(manager, namespace, targetLanguage);
            ClientTranslationRuntime.discoverBatch(namespace, english, target);
        }
    }

    private static Map<String, String> read(ResourceManager manager, String namespace, String language) {
        Map<String, String> entries = new LinkedHashMap<>();
        try {
            ResourceLocation location = new ResourceLocation(namespace, "lang/" + language + ".json");
            for (Resource resource : manager.getResourceStack(location)) {
                try (InputStreamReader reader = new InputStreamReader(resource.open(), StandardCharsets.UTF_8)) {
                    JsonElement document = JsonParser.parseReader(reader);
                    if (!document.isJsonObject()) continue;
                    for (Map.Entry<String, JsonElement> entry : document.getAsJsonObject().entrySet()) {
                        if (entry.getValue().isJsonPrimitive() && entry.getValue().getAsJsonPrimitive().isString()) entries.put(entry.getKey(), entry.getValue().getAsString());
                    }
                }
            }
        } catch (Exception ignored) { }
        return entries;
    }
}
