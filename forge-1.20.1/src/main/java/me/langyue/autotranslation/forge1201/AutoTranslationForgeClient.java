package me.langyue.autotranslation.forge1201;

import me.langyue.autotranslation.client1201.ClientConfig;
import me.langyue.autotranslation.client1201.ClientTranslationRuntime;
import me.langyue.autotranslation.client1201.ScreenTranslationState;
import me.shedaniel.autoconfig.AutoConfig;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.SharedConstants;
import net.minecraft.server.packs.PackType;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/** Client-only bootstrap intentionally isolated from the @Mod class. */
final class AutoTranslationForgeClient {
    private static final KeyMapping TOGGLE_SCREEN = new KeyMapping("key.autotranslation.screen_translate",
            InputConstants.Type.KEYSYM, -1, "category.autotranslation");
    private final ClientTranslationService service;

    private AutoTranslationForgeClient() {
        ForgePlatform platform = new ForgePlatform();
        ClientConfig config = ClientConfig.register(platform.gameDirectory());
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((minecraft, parent) -> AutoConfig.getConfigScreen(ClientConfig.class, parent).get()));
        service = new ClientTranslationService(platform, config, net.minecraft.client.Minecraft.getInstance().options.languageCode,
                SharedConstants.getCurrentVersion().getPackVersion(PackType.CLIENT_RESOURCES));
        FMLJavaModLoadingContext.get().getModEventBus().addListener((RegisterKeyMappingsEvent event) -> event.register(TOGGLE_SCREEN));
        MinecraftForge.EVENT_BUS.register(this);
        Runtime.getRuntime().addShutdownHook(new Thread(service::close, "AutoTranslation-Forge-shutdown"));
    }

    static void initialize() { new AutoTranslationForgeClient(); }

    @SubscribeEvent public void onCommands(RegisterClientCommandsEvent event) {
        new ForgeClientCommands(service.resources(), service.commandState()).register(event.getDispatcher());
    }

    @SubscribeEvent public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft client = Minecraft.getInstance();
        while (TOGGLE_SCREEN.consumeClick() && client.screen != null) {
            ClientTranslationRuntime.toggleScreen(ScreenTranslationState.screenId(client.screen));
        }
    }
}
