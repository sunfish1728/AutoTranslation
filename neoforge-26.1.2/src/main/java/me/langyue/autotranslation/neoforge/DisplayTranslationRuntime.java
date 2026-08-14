package me.langyue.autotranslation.neoforge;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;
import me.langyue.autotranslation.client1201.ClientConfig;
import me.langyue.autotranslation.core.ScreenTranslationState;
import net.minecraft.client.Minecraft;

/** Immutable render-facing state; source Components are never retained or mutated. */
public final class DisplayTranslationRuntime {
    private static final AtomicReference<UnaryOperator<String>> TRANSLATOR =
            new AtomicReference<>(UnaryOperator.identity());
    private static final AtomicReference<ScreenTranslationState> SCREENS = new AtomicReference<>();
    private static final AtomicReference<ClientConfig> CONFIG = new AtomicReference<>();

    private DisplayTranslationRuntime() { }

    static void install(UnaryOperator<String> translator, ScreenTranslationState screens, ClientConfig config) {
        TRANSLATOR.set(translator);
        SCREENS.set(screens);
        CONFIG.set(config);
    }

    public static String displayCopy(String source) {
        ScreenTranslationState screens = SCREENS.get();
        Minecraft minecraft = Minecraft.getInstance();
        if (source == null || screens == null || minecraft == null || !screens.shouldTranslate(minecraft.screen)) return source;
        return TRANSLATOR.get().apply(source);
    }

    static boolean toggleCurrentScreen() {
        ScreenTranslationState screens = SCREENS.get();
        return screens != null && screens.toggle(Minecraft.getInstance().screen);
    }

    public static boolean iconVisible(Object screen) {
        ClientConfig config = CONFIG.get();
        ScreenTranslationState screens = SCREENS.get();
        return config != null && screens != null && screens.allowed(screen)
                && (config.icon.alwaysDisplay || screens.shouldTranslate(screen));
    }

    public static boolean screenEnabled(Object screen) {
        ScreenTranslationState screens = SCREENS.get();
        return screens != null && screens.shouldTranslate(screen);
    }

    public static ClientConfig config() { return CONFIG.get(); }

    static void clear() {
        TRANSLATOR.set(UnaryOperator.identity());
        SCREENS.set(null);
        CONFIG.set(null);
    }
}
