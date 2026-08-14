package me.langyue.autotranslation.neoforge.mixin;

import me.langyue.autotranslation.client1201.DisplayTranslations;
import net.minecraft.client.resources.language.ClientLanguage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Substitutes only the returned display String from an immutable cache. */
@Mixin(ClientLanguage.class)
abstract class LanguageDisplayMixin {
    @Inject(method = "getOrDefault(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;", at = @At("RETURN"), cancellable = true)
    private void autotranslation$displayCopy(String key, String fallback, CallbackInfoReturnable<String> result) {
        result.setReturnValue(DisplayTranslations.displayCopy(key, result.getReturnValue()));
    }
}
