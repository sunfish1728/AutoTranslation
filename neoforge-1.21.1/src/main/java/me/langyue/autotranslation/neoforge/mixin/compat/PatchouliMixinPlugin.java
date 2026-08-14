package me.langyue.autotranslation.neoforge.mixin.compat;

import java.util.List;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/** Prevents optional Patchouli classes from being resolved unless Patchouli is installed. */
public final class PatchouliMixinPlugin implements IMixinConfigPlugin {
    private static final String BOOK_ENTRY = "vazkii.patchouli.client.book.BookEntry";
    @Override public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return !mixinClassName.endsWith("PatchouliBookEntryMixin") || present(BOOK_ENTRY);
    }
    private static boolean present(String name) {
        try {
            Class.forName(name, false, PatchouliMixinPlugin.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError ignored) {
            return false;
        }
    }
    @Override public void onLoad(String mixinPackage) { }
    @Override public String getRefMapperConfig() { return null; }
    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) { }
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) { }
    @Override public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) { }
}
