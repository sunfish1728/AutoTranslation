package me.langyue.autotranslation.core;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

/** Deduplicated queue with bounded exponential retry. Results are marshalled by caller supplied client executor. */
public final class TranslationQueue implements AutoCloseable {
    private final ScheduledExecutorService worker;
    private final Executor clientExecutor;
    private final Map<String, TaskState> pending = new ConcurrentHashMap<>();
    private final Object admissionLock = new Object();
    private final int capacity;
    private final int maxAttempts;
    private final Duration baseBackoff;
    private volatile boolean closed;

    public TranslationQueue(Executor clientExecutor, int maxAttempts, Duration baseBackoff) {
        this(clientExecutor, 1024, maxAttempts, baseBackoff);
    }

    public TranslationQueue(Executor clientExecutor, int capacity, int maxAttempts, Duration baseBackoff) {
        this.clientExecutor = Objects.requireNonNull(clientExecutor, "clientExecutor");
        if (capacity < 1) throw new IllegalArgumentException("capacity must be positive");
        if (baseBackoff == null || baseBackoff.isNegative()) throw new IllegalArgumentException("baseBackoff must not be negative");
        this.capacity = capacity;
        this.maxAttempts = Math.max(1, maxAttempts);
        this.baseBackoff = baseBackoff;
        this.worker = new ScheduledThreadPoolExecutor(1, runnable -> {
            Thread thread = new Thread(runnable, "AutoTranslation-queue");
            thread.setDaemon(true);
            return thread;
        });
    }

    public void submit(String key, Function<String, String> translator, Consumer<String> callback) {
        trySubmit(key, translator, callback);
    }

    /** Returns false when the queue is closed, full, or the key is already in flight. */
    public boolean trySubmit(String key, Function<String, String> translator, Consumer<String> callback) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(translator, "translator");
        Objects.requireNonNull(callback, "callback");
        TaskState state = new TaskState(translator, callback);
        synchronized (admissionLock) {
            if (closed || pending.size() >= capacity || pending.containsKey(key)) return false;
            pending.put(key, state);
        }
        scheduleAttempt(key, state, Duration.ZERO);
        return true;
    }

    public int pendingCount() { return pending.size(); }

    private void scheduleAttempt(String key, TaskState state, Duration delay) {
        try {
            worker.schedule(() -> attempt(key, state), delay.toMillis(), TimeUnit.MILLISECONDS);
        } catch (RejectedExecutionException ignored) {
            pending.remove(key, state);
        }
    }

    private void attempt(String key, TaskState state) {
        if (closed || pending.get(key) != state) return;
        int attempt = ++state.attempts;
            String result = null;
            try { result = state.translator.apply(key); } catch (RuntimeException ignored) { }
            if (result != null) {
                String completed = result;
                if (pending.remove(key, state) && !closed) {
                    try {
                        clientExecutor.execute(() -> state.callback.accept(completed));
                    } catch (RejectedExecutionException ignored) { }
                }
            } else if (attempt < maxAttempts && !closed) {
                long exponential = saturatedShift(baseBackoff.toMillis(), attempt - 1);
                long jitterBound = Math.max(1L, exponential / 4L + 1L);
                long delay = Math.min(TimeUnit.MINUTES.toMillis(5), exponential + ThreadLocalRandom.current().nextLong(jitterBound));
                scheduleAttempt(key, state, Duration.ofMillis(delay));
            } else {
                pending.remove(key, state);
            }
    }

    private static long saturatedShift(long value, int shift) {
        if (value == 0) return 0;
        if (shift >= 62 || value > (Long.MAX_VALUE >> shift)) return Long.MAX_VALUE;
        return value << shift;
    }

    @Override public void close() {
        synchronized (admissionLock) {
            closed = true;
            pending.clear();
        }
        worker.shutdownNow();
        try {
            worker.awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    private static final class TaskState {
        private final Function<String, String> translator;
        private final Consumer<String> callback;
        private int attempts;

        private TaskState(Function<String, String> translator, Consumer<String> callback) {
            this.translator = translator;
            this.callback = callback;
        }
    }
}
