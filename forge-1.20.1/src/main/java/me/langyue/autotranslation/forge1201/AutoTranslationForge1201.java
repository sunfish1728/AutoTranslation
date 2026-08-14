package me.langyue.autotranslation.forge1201;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

/** Client-only metadata and display test prevent this UI mod being required by servers. */
@Mod("autotranslation")
public final class AutoTranslationForge1201 {
    public AutoTranslationForge1201() {
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> AutoTranslationForgeClient::initialize);
    }
}
