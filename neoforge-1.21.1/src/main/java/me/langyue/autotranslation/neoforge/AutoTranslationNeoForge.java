package me.langyue.autotranslation.neoforge;

import me.langyue.autotranslation.client1201.ClientConfig;
import me.langyue.autotranslation.core.LegacyConfigBackup;
import me.shedaniel.autoconfig.AutoConfig;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/** Client-only native NeoForge entry point. */
@Mod(value = "autotranslation", dist = Dist.CLIENT)
public final class AutoTranslationNeoForge implements AutoCloseable {
    private static final KeyMapping SCREEN_TRANSLATE = new KeyMapping("key.autotranslation.screen_translate",
            InputConstants.Type.KEYSYM, -1, "category.autotranslation");
    private final ClientConfig config;
    private volatile NeoForgeClientService service;

    public AutoTranslationNeoForge(ModContainer container, IEventBus modBus) {
        try { LegacyConfigBackup.createOnce(Minecraft.getInstance().gameDirectory.toPath()); }
        catch (java.io.IOException failure) { System.err.println("[AutoTranslation] Could not back up the legacy config before loading it."); }
        config = ClientConfig.register(Minecraft.getInstance().gameDirectory.toPath());
        container.registerExtensionPoint(IConfigScreenFactory.class,
                (IConfigScreenFactory) (ignored, parent) -> AutoConfig.getConfigScreen(ClientConfig.class, parent).get());
        modBus.addListener((FMLClientSetupEvent event) -> event.enqueueWork(this::service));
        modBus.addListener((RegisterClientReloadListenersEvent event) ->
                event.registerReloadListener((ResourceManagerReloadListener) resources -> service().onResourceReload(resources)));
        modBus.addListener((RegisterKeyMappingsEvent event) -> event.register(SCREEN_TRANSLATE));
        NeoForge.EVENT_BUS.addListener((RegisterClientCommandsEvent event) ->
                service().registerCommands(event));
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> {
            while (SCREEN_TRANSLATE.consumeClick()) DisplayTranslationRuntime.toggleCurrentScreen();
        });
        Runtime.getRuntime().addShutdownHook(new Thread(this::close, "AutoTranslation-shutdown"));
    }

    private synchronized NeoForgeClientService service() {
        if (service == null) service = new NeoForgeClientService(config);
        return service;
    }

    @Override public void close() {
        NeoForgeClientService current = service;
        if (current != null) current.close();
    }
}
