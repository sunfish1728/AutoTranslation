package me.langyue.autotranslation;

import com.mojang.blaze3d.platform.InputConstants;
import me.langyue.autotranslation.config.Config;
import me.langyue.autotranslation.resource.ResourceManager;
import me.langyue.autotranslation.translate.TranslatorManager;
import me.langyue.autotranslation.translate.TranslateThreadPool;
import me.langyue.autotranslation.translate.google.HttpClientUtil;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.locale.Language;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public class AutoTranslation {
    public static final Logger LOGGER = LoggerFactory.getLogger("AutoTranslation");
    public static final String MOD_ID = "autotranslation";

    /**
     * Client loaders set this before initialisation.  Keeping the core free of
     * Architectury avoids a loader bridge being shipped at runtime.
     */
    public static Path ROOT;
    public static Config CONFIG = null;
    private static boolean shutdownHookInstalled;

    public static final KeyMapping SCREEN_TRANSLATE_KEYMAPPING = new KeyMapping(
            "key.autotranslation.screen_translate", // The translation key of the name shown in the Controls screen
            InputConstants.Type.KEYSYM, // This key mapping is for Keyboards by default
            -1, // The default keycode
            "category.autotranslation" // The category translation key used to categorize in the Controls screen
    );

    public static synchronized void bootstrap(Path gameDirectory) {
        if (ROOT == null) {
            ROOT = gameDirectory.resolve("AutoTranslation");
        }
    }

    public static void init() {
        if (ROOT == null) {
            throw new IllegalStateException("AutoTranslation client platform was not bootstrapped");
        }
        Config.init();
        TranslatorManager.init();
        TranslatorHelper.init();
        ResourceManager.init();
        ScreenTranslationHelper.init();
        if (!shutdownHookInstalled) {
            shutdownHookInstalled = true;
            Runtime.getRuntime().addShutdownHook(new Thread(AutoTranslation::stop, "AutoTranslation-shutdown"));
        }
    }

    public static void stop() {
        ScreenTranslationHelper.close();
        ResourceManager.save();
        ResourceManager.close();
        TranslateThreadPool.close();
        me.langyue.autotranslation.translate.google.Google.close();
        HttpClientUtil.closeConnectionPool();
    }

    public static String getLanguage() {
        try {
            return Minecraft.getInstance().options.languageCode;
        } catch (Throwable e) {
            return Language.DEFAULT;
        }
    }

    public static void debug(String var1, Object... var2) {
        if (CONFIG.debug) {
            LOGGER.info(var1, var2);
        }
    }
}
