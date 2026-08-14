package me.langyue.autotranslation.client1201;

import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/** Loader-neutral confirmation state; each adapter exposes the same command semantics. */
public final class ClientCommandState implements AutoCloseable {
    private final ScheduledExecutorService timer;
    private Runnable pending;
    public ClientCommandState(ScheduledExecutorService timer) { this.timer = Objects.requireNonNull(timer); }
    /** A daemon timer owned by the client translation lifecycle, never by an ephemeral command registration. */
    public static ClientCommandState managed() {
        return new ClientCommandState(Executors.newSingleThreadScheduledExecutor(task -> {
            Thread thread = new Thread(task, "AutoTranslation-command-confirm");
            thread.setDaemon(true);
            return thread;
        }));
    }
    public synchronized boolean schedule(Runnable action) {
        if (pending != null) return false;
        pending = action;
        timer.schedule(() -> { synchronized (ClientCommandState.this) { pending = null; } }, 30, TimeUnit.SECONDS);
        return true;
    }
    public synchronized boolean confirm() {
        Runnable action = pending; pending = null;
        if (action == null) return false;
        action.run(); return true;
    }
    @Override public void close() { synchronized (this) { pending = null; } timer.shutdownNow(); }
}
