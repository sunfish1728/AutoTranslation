package me.langyue.autotranslation.neoforge;

import me.langyue.autotranslation.client1201.ClientConfig;
import me.langyue.autotranslation.core.LegacyConfigBackup;
import me.shedaniel.autoconfig.AutoConfigClient;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.lifecycle.ClientStoppingEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

import java.nio.file.Path;

/** Native NeoForge 26.1.2 client entry point for the core translation service. */
@Mod(value = AutoTranslationNeoForge.MOD_ID, dist = Dist.CLIENT)
public final class AutoTranslationNeoForge implements AutoCloseable {
    public static final String MOD_ID = "autotranslation";
    private static final KeyMapping SCREEN_TRANSLATE_KEY = new KeyMapping(
            "key.autotranslation.screen_translate", InputConstants.Type.KEYSYM, -1,
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "general")));
    private final NeoForgeClientService service;

    public AutoTranslationNeoForge(ModContainer container, IEventBus modBus) {
        Path gameDirectory = FMLPaths.GAMEDIR.get();
        try {
            LegacyConfigBackup.createOnce(gameDirectory);
        } catch (java.io.IOException backupFailure) {
            throw new IllegalStateException("Cannot back up config/autotranslation.json5 before migration", backupFailure);
        }
        ClientConfig config = ClientConfig.register(gameDirectory);
        container.registerExtensionPoint(IConfigScreenFactory.class,
                (IConfigScreenFactory) (ignored, parent) -> AutoConfigClient.getConfigScreen(ClientConfig.class, parent).get());
        service = new NeoForgeClientService(config, gameDirectory);
        modBus.addListener(AddClientReloadListenersEvent.class, event -> event.addListener(
                Identifier.fromNamespaceAndPath(MOD_ID, "language_discovery"),
                (ResourceManagerReloadListener) service::onResourceReload));
        modBus.addListener(RegisterKeyMappingsEvent.class, event -> event.register(SCREEN_TRANSLATE_KEY));
        NeoForge.EVENT_BUS.addListener(RegisterClientCommandsEvent.class,
                event -> NeoForgeClientCommands.register(event.getDispatcher(), service));
        NeoForge.EVENT_BUS.addListener(ClientStoppingEvent.class, ignored -> close());
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, ignored -> service.onClientTick(SCREEN_TRANSLATE_KEY));
    }

    @Override public void close() { service.close(); }
}
