package me.langyue.autotranslation.fabric1201;

import me.langyue.autotranslation.client1201.ClientConfig;
import me.langyue.autotranslation.client1201.ClientCommandState;
import me.langyue.autotranslation.client1201.ClientResourceStore;
import me.langyue.autotranslation.client1201.ClientTranslationRuntime;

/** Client-owned queue. Rendering code receives only immutable translated Strings. */
final class ClientTranslationService implements AutoCloseable {
    private final ClientTranslationRuntime runtime;
    private final ClientCommandState commandState = ClientCommandState.managed();
    ClientTranslationService(FabricPlatform platform, ClientConfig config, String language, int packFormat) {
        runtime = ClientTranslationRuntime.start(platform, config, language, packFormat);
    }
    ClientResourceStore resources() { return runtime.resources(); }
    ClientCommandState commandState() { return commandState; }
    @Override public void close() { commandState.close(); ClientTranslationRuntime.closeActive(); }
}
