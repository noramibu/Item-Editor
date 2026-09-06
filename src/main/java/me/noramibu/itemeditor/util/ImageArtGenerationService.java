package me.noramibu.itemeditor.util;

import java.util.concurrent.CancellationException;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public final class ImageArtGenerationService implements AutoCloseable {
    private static final ScheduledThreadPoolExecutor WORKER = new ScheduledThreadPoolExecutor(1, runnable -> {
        Thread thread = new Thread(runnable, "itemeditor-image-art");
        thread.setDaemon(true);
        return thread;
    });

    static {
        WORKER.setRemoveOnCancelPolicy(true);
    }

    private final AtomicLong version = new AtomicLong();
    private final Executor delivery;
    private ScheduledFuture<?> pending;
    private boolean closed;

    public ImageArtGenerationService(Executor delivery) {
        this.delivery = delivery;
    }

    public void cancel() {
        version.incrementAndGet();
        if (pending != null) pending.cancel(true);
        pending = null;
    }

    public void request(
            LoreImageArtUtil.Generator generator,
            int width,
            LoreImageArtUtil.Options options,
            boolean textDisplay,
            Consumer<Result> onResult,
            Consumer<RuntimeException> onError) {
        cancel();
        if (closed) return;
        long request = version.get();
        pending = WORKER.schedule(
                () -> {
                    try {
                        var art = generator.generate(width, options);
                        if (request != version.get()) return;
                        int length = art.serializedLength();
                        if (request != version.get()) return;
                        long nbtUsage = textDisplay ? art.textDisplayNbtUsage() : 0;
                        delivery.execute(() -> {
                            if (request == version.get()) onResult.accept(new Result(art, length, nbtUsage));
                        });
                    } catch (CancellationException ignored) {
                    } catch (RuntimeException failure) {
                        delivery.execute(() -> {
                            if (request == version.get()) onError.accept(failure);
                        });
                    }
                },
                200,
                TimeUnit.MILLISECONDS);
    }

    @Override
    public void close() {
        closed = true;
        cancel();
    }

    public record Result(LoreImageArtUtil.Result art, int serializedLength, long nbtUsage) {}
}
