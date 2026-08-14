package me.langyue.autotranslation.neoforge.mixin;

import me.langyue.autotranslation.neoforge.DisplayTranslationRuntime;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.Font;
import java.util.List;
import net.minecraft.network.chat.contents.PlainTextContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Produces a translated String argument while leaving the caller's source object untouched. */
@Mixin(GuiGraphics.class)
abstract class GuiGraphicsDisplayMixin {
    @ModifyVariable(
            method = "drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)I",
            at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private String autotranslation$displayCopy(String source) {
        return DisplayTranslationRuntime.displayCopy(source);
    }

    @ModifyVariable(
            method = "drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)I",
            at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private Component autotranslation$componentDisplayCopy(Component source) {
        if (!(source.getContents() instanceof PlainTextContents.LiteralContents) || !source.getSiblings().isEmpty()
                || source.getStyle().getClickEvent() != null || source.getStyle().getHoverEvent() != null
                || source.getStyle().getInsertion() != null) return source;
        String translated = DisplayTranslationRuntime.displayCopy(source.getString());
        return translated.equals(source.getString()) ? source : Component.literal(translated).withStyle(source.getStyle());
    }

    @ModifyVariable(
            method = "renderTooltip(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;II)V",
            at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private Component autotranslation$tooltipDisplayCopy(Component source) {
        if (!(source.getContents() instanceof PlainTextContents.LiteralContents) || !source.getSiblings().isEmpty()
                || source.getStyle().getClickEvent() != null || source.getStyle().getHoverEvent() != null
                || source.getStyle().getInsertion() != null) return source;
        String translated = DisplayTranslationRuntime.displayCopy(source.getString());
        return translated.equals(source.getString()) ? source : Component.literal(translated).withStyle(source.getStyle());
    }
}
