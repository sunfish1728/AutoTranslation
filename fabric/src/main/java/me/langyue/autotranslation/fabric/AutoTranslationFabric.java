package me.langyue.autotranslation.fabric;

import me.langyue.autotranslation.AutoTranslation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;

public class AutoTranslationFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        if (FabricLoader.getInstance().getEnvironmentType() != EnvType.CLIENT) return;
        AutoTranslation.bootstrap(FabricLoader.getInstance().getGameDir());
        AutoTranslation.init();
        KeyBindingHelper.registerKeyBinding(AutoTranslation.SCREEN_TRANSLATE_KEYMAPPING);
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> FabricAutoTranslationCommands.register(dispatcher));
    }
}
