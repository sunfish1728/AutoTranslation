package me.langyue.autotranslation.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/** Persistent screen whitelist and optional-mod-safe wrapper inspection. */
public final class ScreenTranslationState implements AutoCloseable {
    private static final List<String> BLACKLIST_PREFIXES = List.of(
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

    private final Path file;
    private final boolean ignoreOriginalScreens;
    private final AtomicReference<Set<String>> whitelist = new AtomicReference<>(Set.of());

    public ScreenTranslationState(Path gameDirectory) {
        this(gameDirectory, true);
    }

    public ScreenTranslationState(Path gameDirectory, boolean ignoreOriginalScreens) {
        file = gameDirectory.toAbsolutePath().resolve("AutoTranslation").resolve("screen.whitelist");
        this.ignoreOriginalScreens = ignoreOriginalScreens;
        reload();
    }

    public boolean toggle(Object screen) {
        if (screen == null) return false;
        String name = resolveScreenClassName(screen);
        if (isBlacklisted(name)) return false;
        synchronized (this) {
            Set<String> changed = new LinkedHashSet<>(whitelist.get());
            boolean enabled = changed.add(name);
            if (!enabled) changed.remove(name);
            whitelist.set(Set.copyOf(changed));
            save();
            return enabled;
        }
    }

    public boolean shouldTranslate(Object screen) {
        if (screen == null) return false;
        String name = resolveScreenClassName(screen);
        return !isBlacklisted(name) && whitelist.get().contains(name);
    }

    public boolean allowed(Object screen) { return screen != null && !isBlacklisted(resolveScreenClassName(screen)); }

    public void reload() {
        if (!Files.isRegularFile(file)) return;
        try {
            Set<String> loaded = new LinkedHashSet<>();
            for (String line : Files.readAllLines(file)) if (!line.isBlank()) loaded.add(line.trim());
            whitelist.set(Set.copyOf(loaded));
        } catch (IOException ignored) { }
    }

    public static String resolveScreenClassName(Object screen) {
        String className = screen.getClass().getName();
        if (className.startsWith("dev.ftb.mods.ftblibrary.ui.ScreenWrapper")) {
            try {
                Object inner = screen.getClass().getMethod("getGui").invoke(screen);
                if (inner != null) className = inner.getClass().getName();
            } catch (ReflectiveOperationException | RuntimeException ignored) { }
        }
        if (className.startsWith("vazkii.patchouli.client.book.gui.")) return "vazkii.patchouli.client.book.gui.*";
        return className;
    }

    private boolean isBlacklisted(String className) {
        return BLACKLIST_PREFIXES.stream().anyMatch(className::startsWith)
                || (ignoreOriginalScreens && (className.startsWith("net.minecraft.client.gui.screens.TitleScreen")
                || className.startsWith("com.mojang.realmsclient.RealmsMainScreen")));
    }

    private void save() {
        try { AtomicFileStore.writeUtf8(file, String.join(System.lineSeparator(), whitelist.get())); }
        catch (IOException ignored) { }
    }

    @Override public void close() { save(); }
}
