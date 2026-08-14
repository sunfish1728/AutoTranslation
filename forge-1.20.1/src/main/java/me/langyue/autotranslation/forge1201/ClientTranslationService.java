package me.langyue.autotranslation.forge1201;

import me.langyue.autotranslation.client1201.ClientConfig;
import me.langyue.autotranslation.client1201.ClientCommandState;
import me.langyue.autotranslation.client1201.ClientResourceStore;
import me.langyue.autotranslation.client1201.ClientTranslationRuntime;

final class ClientTranslationService implements AutoCloseable {
    private final ClientTranslationRuntime runtime;
    private final ClientCommandState commandState = ClientCommandState.managed();
    ClientTranslationService(ForgePlatform platform, ClientConfig config, String language, int packFormat) {
        runtime = ClientTranslationRuntime.start(platform, config, language, packFormat);
    }
    ClientResourceStore resources() { return runtime.resources(); }
    ClientCommandState commandState() { return commandState; }
    @Override public void close() { commandState.close(); ClientTranslationRuntime.closeActive(); }
}
