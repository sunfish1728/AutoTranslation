package me.langyue.autotranslation.client1201;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import me.langyue.autotranslation.core.AtomicFileStore;

/** Persistent, opt-in screen whitelist. It stores class names, never Screen objects. */
public final class ScreenTranslationState {
    private static final List<String> ALWAYS_BLOCKED = List.of(
            "net.minecraft.client.gui.screens.ChatScreen",
            "net.minecraft.client.gui.screens.inventory.BookEditScreen",
            "net.minecraft.client.gui.screens.inventory.SignEditScreen",
            "net.minecraft.client.gui.screens.inventory.HangingSignEditScreen",
            "net.minecraft.client.gui.screens.inventory.CommandBlockEditScreen",
            "net.minecraft.client.gui.screens.inventory.StructureBlockEditScreen",
            "net.minecraft.client.gui.screens.inventory.MinecartCommandBlockEditScreen",
            "net.minecraft.client.gui.screens.inventory.JigsawBlockEditScreen",
            "dev.ftb.mods.ftblibrary.config.ui.",
            "dev.ftb.mods.ftbquests.client.gui.SelectQuestObjectScreen",
            "dev.ftb.mods.ftbquests.client.gui.MultilineTextEditorScreen",
            "dev.ftb.mods.ftbquests.client.gui.RewardTablesScreen",
            "dev.ftb.mods.ftbquests.client.gui.quests.QuestScreen",
            "me.shedaniel.clothconfig2.gui.ClothConfigScreen");
    private static final List<String> ORIGINAL_SCREENS = List.of(
            "net.minecraft.client.gui.screens.TitleScreen", "com.mojang.realmsclient.RealmsMainScreen");
    private final Path file;
    private final boolean ignoreOriginal;
    private final Set<String> enabled = new LinkedHashSet<>();

    ScreenTranslationState(Path gameDirectory, boolean ignoreOriginal) {
        file = gameDirectory.resolve("AutoTranslation").resolve("screen.whitelist");
        this.ignoreOriginal = ignoreOriginal;
        try { if (Files.isRegularFile(file)) enabled.addAll(Files.readAllLines(file)); } catch (IOException ignored) { }
    }

    public synchronized boolean toggle(String screenClass) {
        if (blocked(screenClass)) return false;
        if (!enabled.remove(screenClass)) enabled.add(screenClass);
        save();
        return enabled.contains(screenClass);
    }

    public synchronized boolean enabled(String screenClass) { return screenClass != null && enabled.contains(screenClass) && !blocked(screenClass); }
    public boolean allowed(String screenClass) { return screenClass != null && !blocked(screenClass); }

    /** Resolves optional FTB wrappers without loading any FTB class at link time. */
    public static String screenId(Object screen) {
        if (screen == null) return "";
        Object resolved = screen;
        String name = resolved.getClass().getName();
        if (name.startsWith("dev.ftb.mods.ftblibrary.ui.ScreenWrapper")) {
            try {
                Object inner = resolved.getClass().getMethod("getGui").invoke(resolved);
                if (inner != null) { resolved = inner; name = inner.getClass().getName(); }
            } catch (ReflectiveOperationException | RuntimeException ignored) { }
        }
        return name.startsWith("vazkii.patchouli.client.book.gui.") ? "vazkii.patchouli.client.book.gui.*" : name;
    }

    private boolean blocked(String screenClass) {
        if (screenClass == null) return true;
        if (ALWAYS_BLOCKED.stream().anyMatch(screenClass::startsWith)) return true;
        return ignoreOriginal && ORIGINAL_SCREENS.stream().anyMatch(screenClass::startsWith);
    }

    private void save() {
        try { AtomicFileStore.writeUtf8(file, String.join("\n", enabled) + (enabled.isEmpty() ? "" : "\n")); }
        catch (IOException ignored) { }
    }
}
