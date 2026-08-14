package me.langyue.autotranslation.fabric1201;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.autoconfig.AutoConfig;
import me.langyue.autotranslation.client1201.ClientConfig;

/** Optional metadata entrypoint; Fabric Loader ignores it when ModMenu is absent. */
public final class ModMenuIntegration implements ModMenuApi {
    @Override public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> AutoConfig.getConfigScreen(ClientConfig.class, parent).get();
    }
}
