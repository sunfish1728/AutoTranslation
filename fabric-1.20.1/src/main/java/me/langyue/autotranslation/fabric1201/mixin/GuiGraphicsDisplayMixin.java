package me.langyue.autotranslation.fabric1201.mixin;

import me.langyue.autotranslation.client1201.ClientTranslationRuntime;
import me.langyue.autotranslation.client1201.ScreenTranslationState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.LiteralContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Screen-only display copies. The source String and Component remain untouched. */
@Mixin(GuiGraphics.class)
abstract class GuiGraphicsDisplayMixin {
    @ModifyVariable(method = "drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)I", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private String autotranslation$copyString(String source) { return ClientTranslationRuntime.screenDisplayCopy(screenName(), source); }

    @ModifyVariable(method = "drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)I", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private Component autotranslation$copyComponent(Component source) {
        // Do not flatten translated, composite, clickable, or hoverable Components.
        if (!(source.getContents() instanceof LiteralContents) || !source.getSiblings().isEmpty()
                || source.getStyle().getClickEvent() != null || source.getStyle().getHoverEvent() != null
                || source.getStyle().getInsertion() != null) return source;
        String display = ClientTranslationRuntime.screenDisplayCopy(screenName(), source.getString());
        return display.equals(source.getString()) ? source : Component.literal(display).withStyle(source.getStyle());
    }

    private static String screenName() { return ScreenTranslationState.screenId(Minecraft.getInstance().screen); }
}
