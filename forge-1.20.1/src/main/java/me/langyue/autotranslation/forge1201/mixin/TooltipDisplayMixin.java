package me.langyue.autotranslation.forge1201.mixin;

import java.util.List;

import me.langyue.autotranslation.client1201.ClientTranslationRuntime;
import me.langyue.autotranslation.client1201.ScreenTranslationState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.LiteralContents;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Generates a separate tooltip result without mutating the final source component. */
@Mixin(Tooltip.class)
abstract class TooltipDisplayMixin {
    @Shadow @Final private Component message;

    @Inject(method = "toCharSequence", at = @At("HEAD"), cancellable = true)
    private void autotranslation$tooltipCopy(Minecraft minecraft, CallbackInfoReturnable<List<FormattedCharSequence>> result) {
        String screen = ScreenTranslationState.screenId(minecraft.screen);
        if (!(message.getContents() instanceof LiteralContents) || !message.getSiblings().isEmpty()
                || message.getStyle().getClickEvent() != null || message.getStyle().getHoverEvent() != null
                || message.getStyle().getInsertion() != null) return;
        String display = ClientTranslationRuntime.screenDisplayCopy(screen, message.getString());
        if (!display.equals(message.getString())) result.setReturnValue(Tooltip.splitTooltip(minecraft, Component.literal(display).withStyle(message.getStyle())));
    }
}
