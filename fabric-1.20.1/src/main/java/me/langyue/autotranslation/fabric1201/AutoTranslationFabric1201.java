package me.langyue.autotranslation.fabric1201;

import me.langyue.autotranslation.client1201.ClientConfig;
import me.langyue.autotranslation.client1201.ClientTranslationRuntime;
import me.langyue.autotranslation.client1201.ScreenTranslationState;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.SharedConstants;
import net.minecraft.server.packs.PackType;

import java.util.concurrent.atomic.AtomicReference;

/** Native Fabric client entry; core integration follows after port validation. */
public final class AutoTranslationFabric1201 implements ClientModInitializer {
    private static final KeyMapping TOGGLE_SCREEN = new KeyMapping("key.autotranslation.screen_translate",
            InputConstants.Type.KEYSYM, -1, "category.autotranslation");
    @Override public void onInitializeClient() {
        FabricPlatform platform = new FabricPlatform();
        ClientConfig config = ClientConfig.register(platform.gameDirectory());
        AtomicReference<ClientTranslationService> service = new AtomicReference<>();
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            ClientTranslationService started = new ClientTranslationService(platform, config, client.options.languageCode,
                    SharedConstants.getCurrentVersion().getPackVersion(PackType.CLIENT_RESOURCES));
            service.set(started);
            FabricPlatform.registerShutdown(started);
        });
        KeyBindingHelper.registerKeyBinding(TOGGLE_SCREEN);
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (TOGGLE_SCREEN.consumeClick() && client.screen != null) {
                ClientTranslationRuntime.toggleScreen(ScreenTranslationState.screenId(client.screen));
            }
        });
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, access) -> {
            ClientTranslationService started = service.get();
            if (started != null) new FabricClientCommands(started.resources(), started.commandState()).register(dispatcher);
        });
    }
}
