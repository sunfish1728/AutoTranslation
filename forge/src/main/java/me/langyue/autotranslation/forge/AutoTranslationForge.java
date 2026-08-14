package me.langyue.autotranslation.forge;

import me.langyue.autotranslation.AutoTranslation;
import me.langyue.autotranslation.ScreenTranslationHelper;
import me.langyue.autotranslation.command.AutoTranslationCommands;
import net.minecraftforge.client.gui.ModListScreen;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;

@Mod(AutoTranslation.MOD_ID)
public class AutoTranslationForge {
    public AutoTranslationForge() {
        AutoTranslation.bootstrap(FMLPaths.GAMEDIR.get());
        AutoTranslation.init();
        ScreenTranslationHelper.addScreenBlacklist(ModListScreen.class);
        var modBus = net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener((RegisterKeyMappingsEvent event) -> event.register(AutoTranslation.SCREEN_TRANSLATE_KEYMAPPING));
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener((RegisterClientCommandsEvent event) -> AutoTranslationCommands.register(event.getDispatcher()));
    }
}
