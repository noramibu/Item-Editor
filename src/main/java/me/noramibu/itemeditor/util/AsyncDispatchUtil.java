package me.noramibu.itemeditor.util;

import net.minecraft.client.Minecraft;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class AsyncDispatchUtil {

    private AsyncDispatchUtil() {
    }

    public static ExecutorService newSingleThreadExecutor(String threadName) {
        return Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, threadName);
            thread.setDaemon(true);
            return thread;
        });
    }

    public static <T> Consumer<T> nullSafeConsumer(Consumer<T> consumer) {
        return consumer == null ? ignored -> {} : consumer;
    }

    public static <T> void deliverIfLatest(
            CompletableFuture<T> future,
            AtomicLong requestVersion,
            long requestId,
            Consumer<T> onResult
    ) {
        future.thenAccept(result -> {
            if (requestId != requestVersion.get()) {
                return;
            }
            Minecraft minecraft = Minecraft.getInstance();
            minecraft.execute(() -> {
                if (requestId != requestVersion.get()) {
                    return;
                }
                onResult.accept(result);
            });
        });
    }

    public static <T> void requestLatest(
            AtomicLong requestVersion,
            Executor executor,
            Supplier<T> supplier,
            Consumer<T> onResult
    ) {
        long requestId = requestVersion.incrementAndGet();
        CompletableFuture<T> future = CompletableFuture.supplyAsync(supplier, executor);
        deliverIfLatest(future, requestVersion, requestId, onResult);
    }
}
