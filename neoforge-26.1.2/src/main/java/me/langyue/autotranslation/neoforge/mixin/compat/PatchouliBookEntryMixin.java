package me.langyue.autotranslation.neoforge.mixin.compat;

import me.langyue.autotranslation.neoforge.DisplayTranslationRuntime;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.PlainTextContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Optional title bridge; Patchouli-owned state is never mutated. */
@Pseudo
@Mixin(targets = "vazkii.patchouli.client.book.BookEntry")
abstract class PatchouliBookEntryMixin {
    @Inject(method = "getName", at = @At("RETURN"), cancellable = true, require = 0)
    private void autotranslation$displayCopy(CallbackInfoReturnable<MutableComponent> callback) {
        MutableComponent original = callback.getReturnValue();
        if (!safeLiteral(original)) return;
        String source = original.getString();
        String translated = DisplayTranslationRuntime.displayCopy(source);
        if (!source.equals(translated)) callback.setReturnValue(Component.literal(translated).withStyle(original.getStyle()));
    }

    private static boolean safeLiteral(Component component) {
        if (component == null || !(component.getContents() instanceof PlainTextContents.LiteralContents)
                || !component.getSiblings().isEmpty()) return false;
        Style style = component.getStyle();
        return style.getClickEvent() == null && style.getHoverEvent() == null && style.getInsertion() == null;
    }
}
