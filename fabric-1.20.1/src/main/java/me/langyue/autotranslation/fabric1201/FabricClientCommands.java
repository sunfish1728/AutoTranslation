package me.langyue.autotranslation.fabric1201;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import me.langyue.autotranslation.client1201.ClientCommandState;
import me.langyue.autotranslation.client1201.ClientResourceStore;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.*;

final class FabricClientCommands {
    private final ClientResourceStore store; private final ClientCommandState state;
    FabricClientCommands(ClientResourceStore store, ClientCommandState state) { this.store = store; this.state = state; }
    void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        dispatcher.register(literal("auto_translation")
          .then(literal("reload").executes(c -> reload(c.getSource(), null)).then(argument("namespace", StringArgumentType.word()).executes(c -> reload(c.getSource(), StringArgumentType.getString(c, "namespace")))))
          .then(literal("confirm").executes(c -> { if (!state.confirm()) { c.getSource().sendError(Component.translatable("commands.autotranslation.command.unconfirmed.empty")); return 0; } return 1; }))
          .then(literal("pack_resource").then(literal("full").executes(c -> schedule(c.getSource(), false))).then(literal("increment").executes(c -> schedule(c.getSource(), true)))));
    }
    private int reload(FabricClientCommandSource source, String namespace) { try { store.reload(namespace); source.sendFeedback(Component.translatable("commands.autotranslation.command.reloaded")); return 1; } catch (Exception e) { source.sendError(Component.translatable("commands.autotranslation.command.error.reload")); return 0; } }
    private int schedule(FabricClientCommandSource source, boolean increment) { try { if (increment && !store.hasIncrement()) return 0; if (!state.schedule(() -> pack(source, increment))) return 0; source.sendFeedback(Component.translatable("commands.autotranslation.command.pack_resource.unconfirmed")); return 1; } catch (Exception e) { return 0; } }
    private void pack(FabricClientCommandSource source, boolean increment) { try { store.pack(increment); source.sendFeedback(Component.translatable("commands.autotranslation.command.pack_resource.packed")); } catch (Exception e) { source.sendError(Component.translatable("commands.autotranslation.command.error.pack_resource")); } }
}
