package me.langyue.autotranslation.neoforge.mixin;

import me.langyue.autotranslation.neoforge.DisplayTranslationRuntime;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.PlainTextContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Replaces only display arguments; caller-owned text and Components remain unchanged. */
@Mixin(GuiGraphicsExtractor.class)
abstract class GuiGraphicsDisplayMixin {
    @ModifyVariable(
            method = "text(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)V",
            at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private String autotranslation$displayCopy(String source) {
        return DisplayTranslationRuntime.displayCopy(source);
    }

    @ModifyVariable(
            method = "text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)V",
            at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private Component autotranslation$componentDisplayCopy(Component source) {
        return safeDisplayCopy(source);
    }

    @ModifyVariable(
            method = "setTooltipForNextFrame(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IILnet/minecraft/resources/Identifier;)V",
            at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private Component autotranslation$tooltipDisplayCopy(Component source) {
        return safeDisplayCopy(source);
    }

    private static Component safeDisplayCopy(Component source) {
        if (source == null || !(source.getContents() instanceof PlainTextContents.LiteralContents)
                || !source.getSiblings().isEmpty() || source.getStyle().getClickEvent() != null
                || source.getStyle().getHoverEvent() != null || source.getStyle().getInsertion() != null) return source;
        String original = source.getString();
        String translated = DisplayTranslationRuntime.displayCopy(original);
        return original.equals(translated) ? source : Component.literal(translated).withStyle(source.getStyle());
    }
}
