package me.langyue.autotranslation.forge1201.mixin.compat;

import me.langyue.autotranslation.client1201.ClientTranslationRuntime;
import me.langyue.autotranslation.client1201.ScreenTranslationState;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.LiteralContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Returns a safe display-only copy; Patchouli's stored name is never changed. */
@Pseudo
@Mixin(targets = "vazkii.patchouli.client.book.BookEntry")
abstract class PatchouliBookEntryMixin {
    @Inject(method = "getName", at = @At("RETURN"), cancellable = true, require = 0)
    private void autotranslation$displayCopy(CallbackInfoReturnable<MutableComponent> callback) {
        MutableComponent original = callback.getReturnValue();
        if (!safeLiteral(original)) return;
        String source = original.getString();
        String screen = ScreenTranslationState.screenId(Minecraft.getInstance().screen);
        String translated = ClientTranslationRuntime.screenDisplayCopy(screen, source);
        if (!source.equals(translated)) callback.setReturnValue(Component.literal(translated).withStyle(original.getStyle()));
    }
    private static boolean safeLiteral(Component component) {
        if (component == null || !(component.getContents() instanceof LiteralContents) || !component.getSiblings().isEmpty()) return false;
        Style style = component.getStyle();
        return style.getClickEvent() == null && style.getHoverEvent() == null && style.getInsertion() == null;
    }
}
