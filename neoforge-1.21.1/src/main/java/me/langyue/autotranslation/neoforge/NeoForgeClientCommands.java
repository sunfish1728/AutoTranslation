package me.langyue.autotranslation.neoforge;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import me.langyue.autotranslation.client1201.ClientCommandState;
import me.langyue.autotranslation.client1201.ClientResourceStore;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/** Native client commands; the connected server never receives these commands. */
final class NeoForgeClientCommands implements AutoCloseable {
    private final ClientResourceStore store;
    private final ClientCommandState state;

    NeoForgeClientCommands(ClientResourceStore store) {
        this.store = store;
        ScheduledThreadPoolExecutor timer = new ScheduledThreadPoolExecutor(1, runnable -> {
            Thread thread = new Thread(runnable, "AutoTranslation-confirm");
            thread.setDaemon(true);
            return thread;
        });
        timer.setRemoveOnCancelPolicy(true);
        state = new ClientCommandState(timer);
    }

    void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(literal("auto_translation")
                .then(literal("reload")
                        .executes(context -> reload(context.getSource(), null))
                        .then(argument("namespace", StringArgumentType.word())
                                .executes(context -> reload(context.getSource(), StringArgumentType.getString(context, "namespace")))))
                .then(literal("confirm").executes(context -> confirm(context.getSource())))
                .then(literal("pack_resource")
                        .executes(context -> schedule(context.getSource(), false))
                        .then(literal("full").executes(context -> schedule(context.getSource(), false)))
                        .then(literal("increment").executes(context -> schedule(context.getSource(), true)))));
    }

    private int reload(CommandSourceStack source, String namespace) {
        try {
            store.reload(namespace);
            source.sendSuccess(() -> Component.translatable("commands.autotranslation.command.reloaded"), false);
            return 1;
        } catch (Exception failure) {
            source.sendFailure(Component.translatable("commands.autotranslation.command.error.reload"));
            return 0;
        }
    }

    private int schedule(CommandSourceStack source, boolean increment) {
        try {
            if (increment && !store.hasIncrement()) return 0;
            if (!state.schedule(() -> Minecraft.getInstance().execute(() -> pack(source, increment)))) return 0;
            source.sendSuccess(() -> Component.translatable("commands.autotranslation.command.pack_resource.unconfirmed"), false);
            return 1;
        } catch (Exception failure) {
            source.sendFailure(Component.translatable("commands.autotranslation.command.error.pack_resource"));
            return 0;
        }
    }

    private int confirm(CommandSourceStack source) {
        if (!state.confirm()) {
            source.sendFailure(Component.translatable("commands.autotranslation.command.error.confirm"));
            return 0;
        }
        return 1;
    }

    private void pack(CommandSourceStack source, boolean increment) {
        try {
            store.pack(increment);
            source.sendSuccess(() -> Component.translatable("commands.autotranslation.command.pack_resource.packed"), false);
        } catch (Exception failure) {
            source.sendFailure(Component.translatable("commands.autotranslation.command.error.pack_resource"));
        }
    }

    @Override public void close() { state.close(); }
}
