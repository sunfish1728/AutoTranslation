package me.langyue.autotranslation.forge1201.mixin;

import me.langyue.autotranslation.client1201.ClientTranslationRuntime;
import net.minecraft.client.resources.language.ClientLanguage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientLanguage.class)
abstract class LanguageDisplayMixin {
 @Inject(method="getOrDefault", at=@At("RETURN"), cancellable=true)
 private void autotranslation$displayCopy(String key, String fallback, CallbackInfoReturnable<String> result) {
  result.setReturnValue(ClientTranslationRuntime.displayCopy(key, result.getReturnValue()));
 }
}
