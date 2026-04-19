package me.noramibu.itemeditor.util;

import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.ItemStack;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public final class RawValidationAsyncService {

    private static final ExecutorService PARSE_EXECUTOR =
            AsyncDispatchUtil.newSingleThreadExecutor("itemeditor-raw-validate-parse");
    private static final ExecutorService HEAVY_EXECUTOR =
            AsyncDispatchUtil.newSingleThreadExecutor("itemeditor-raw-validate-heavy");

    private final AtomicLong requestVersion = new AtomicLong();

    public void requestTwoPhase(
            String rawText,
            ItemStack originalStack,
            RegistryAccess registryAccess,
            long parseIdleDelayMs,
            long heavyIdleDelayMs,
            Consumer<ParsePhaseResult> onParseResult,
            Consumer<Result> onHeavyResult
    ) {
        String safeText = rawText == null ? "" : rawText;
        ItemStack safeOriginal = originalStack == null ? ItemStack.EMPTY : originalStack.copy();
        long parseIdleDelay = Math.max(0L, parseIdleDelayMs);
        long idleDelay = Math.max(0L, heavyIdleDelayMs);
        Consumer<ParsePhaseResult> parseConsumer = AsyncDispatchUtil.nullSafeConsumer(onParseResult);
        Consumer<Result> heavyConsumer = AsyncDispatchUtil.nullSafeConsumer(onHeavyResult);

        long requestId = this.requestVersion.incrementAndGet();

        CompletableFuture<ParsePhaseResult> parseFuture = CompletableFuture.supplyAsync(
                () -> this.computeParseWithIdle(requestId, safeText, registryAccess, parseIdleDelay),
                PARSE_EXECUTOR
        );
        AsyncDispatchUtil.deliverIfLatest(parseFuture, this.requestVersion, requestId, result -> {
            if (result != null) {
                parseConsumer.accept(result);
            }
        });

        CompletableFuture<Result> heavyFuture = CompletableFuture.supplyAsync(
                () -> this.computeHeavyWithIdle(requestId, safeOriginal, registryAccess, idleDelay, parseFuture),
                HEAVY_EXECUTOR
        );
        AsyncDispatchUtil.deliverIfLatest(heavyFuture, this.requestVersion, requestId, result -> {
            if (result != null) {
                heavyConsumer.accept(result);
            }
        });
    }

    public void cancelPending() {
        this.requestVersion.incrementAndGet();
    }

    private ParsePhaseResult computeParseWithIdle(
            long requestId,
            String rawText,
            RegistryAccess registryAccess,
            long idleDelayMs
    ) {
        if (requestId != this.requestVersion.get()) {
            return null;
        }
        if (idleDelayMs > 0L) {
            long remaining = idleDelayMs;
            while (remaining > 0L) {
                if (requestId != this.requestVersion.get()) {
                    return null;
                }
                long sleep = Math.min(remaining, 40L);
                try {
                    Thread.sleep(sleep);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    return null;
                }
                remaining -= sleep;
            }
        }
        if (requestId != this.requestVersion.get()) {
            return null;
        }

        RawItemDataUtil.ParseResult parsed = RawItemDataUtil.parse(rawText, registryAccess);
        if (!parsed.success()) {
            return new ParsePhaseResult(false, parsed.error(), parsed.line(), parsed.column(), null);
        }
        return new ParsePhaseResult(true, null, -1, -1, parsed.stack() == null ? null : parsed.stack().copy());
    }

    private Result computeHeavyWithIdle(
            long requestId,
            ItemStack originalStack,
            RegistryAccess registryAccess,
            long idleDelayMs,
            CompletableFuture<ParsePhaseResult> parseFuture
    ) {
        if (requestId != this.requestVersion.get()) {
            return null;
        }
        if (idleDelayMs > 0L) {
            long remaining = idleDelayMs;
            while (remaining > 0L) {
                if (requestId != this.requestVersion.get()) {
                    return null;
                }
                long sleep = Math.min(remaining, 50L);
                try {
                    Thread.sleep(sleep);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    return null;
                }
                remaining -= sleep;
            }
        }
        if (requestId != this.requestVersion.get()) {
            return null;
        }
        ParsePhaseResult parsePhase = parseFuture.join();
        if (parsePhase == null) {
            return null;
        }
        if (!parsePhase.success()) {
            return new Result(
                    false,
                    parsePhase.parseError(),
                    parsePhase.line(),
                    parsePhase.column(),
                    null,
                    0
            );
        }

        ItemStack parsedStack = parsePhase.parsedStack();
        if (parsedStack == null) {
            return new Result(
                    false,
                    ItemEditorText.str("raw.unknown_error"),
                    -1,
                    -1,
                    null,
                    0
            );
        }

        var validationMessages = RawItemDataUtil.validatePreviewStack(parsedStack, registryAccess);
        for (var message : validationMessages) {
            if (message.severity() == me.noramibu.itemeditor.editor.ValidationMessage.Severity.ERROR) {
                return new Result(
                        false,
                        message.message(),
                        -1,
                        -1,
                        null,
                    0
                );
            }
        }

        ItemComponentDiffUtil.Result diff = ItemComponentDiffUtil.diff(originalStack, parsedStack, registryAccess);
        return new Result(
                true,
                null,
                -1,
                -1,
                diff.error(),
            diff.error() == null ? diff.entries().size() : 0
        );
    }

    public record ParsePhaseResult(
            boolean success,
            String parseError,
            int line,
            int column,
            ItemStack parsedStack
    ) {
        public boolean hasPosition() {
            return this.line > 0 && this.column > 0;
        }
    }

    public record Result(
            boolean success,
            String parseError,
            int line,
            int column,
            String diffError,
            int diffEntries
    ) {
        public boolean hasPosition() {
            return this.line > 0 && this.column > 0;
        }
    }
}
