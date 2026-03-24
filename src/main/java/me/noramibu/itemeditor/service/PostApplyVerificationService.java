package me.noramibu.itemeditor.service;

import me.noramibu.itemeditor.util.ItemComponentDiffUtil;
import me.noramibu.itemeditor.util.ItemEditorText;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

public final class PostApplyVerificationService {

    private static final int DEFAULT_VERIFY_DELAY_TICKS = 12;
    private static final List<PendingVerification> PENDING = new ArrayList<>();
    private static boolean initialized;

    private PostApplyVerificationService() {
    }

    public static void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
        ClientTickEvents.END_CLIENT_TICK.register(PostApplyVerificationService::tick);
    }

    public static void schedule(Minecraft minecraft, ItemStack expectedStack, int slot, Consumer<VerificationResult> callback) {
        if (minecraft == null || slot < 0) {
            callback.accept(new VerificationResult(
                    false,
                    ItemEditorText.str("apply.verify.error"),
                    List.of()
            ));
            return;
        }

        PENDING.add(new PendingVerification(expectedStack.copy(), slot, callback));
    }

    private static void tick(Minecraft client) {
        if (PENDING.isEmpty()) {
            return;
        }

        Iterator<PendingVerification> iterator = PENDING.iterator();
        while (iterator.hasNext()) {
            PendingVerification pending = iterator.next();
            pending.ticksRemaining -= 1;
            if (pending.ticksRemaining > 0) {
                continue;
            }

            VerificationResult result = verify(client, pending);
            pending.callback.accept(result);
            iterator.remove();
        }
    }

    private static VerificationResult verify(Minecraft client, PendingVerification pending) {
        if (client.player == null) {
            return new VerificationResult(false, ItemEditorText.str("apply.verify.error"), List.of());
        }

        ItemStack current = client.player.getInventory().getItem(pending.slot).copy();
        if (ItemStack.isSameItemSameComponents(current, pending.expectedStack)
                && current.getCount() == pending.expectedStack.getCount()) {
            return new VerificationResult(true, "", List.of());
        }

        RegistryAccess registryAccess = client.level != null ? client.level.registryAccess() : RegistryAccess.EMPTY;
        ItemComponentDiffUtil.Result diff = ItemComponentDiffUtil.diff(pending.expectedStack, current, registryAccess);
        if (diff.error() != null) {
            return new VerificationResult(false, ItemEditorText.str("apply.verify.error"), List.of());
        }

        return new VerificationResult(
                false,
                ItemEditorText.str("apply.verify.warning", diff.entries().size()),
                diff.entries()
        );
    }

    public record VerificationResult(boolean matchesExpected, String message, List<ItemComponentDiffUtil.Entry> entries) {
    }

    private static final class PendingVerification {
        private final ItemStack expectedStack;
        private final int slot;
        private int ticksRemaining = DEFAULT_VERIFY_DELAY_TICKS;
        private final Consumer<VerificationResult> callback;

        private PendingVerification(ItemStack expectedStack, int slot, Consumer<VerificationResult> callback) {
            this.expectedStack = expectedStack;
            this.slot = slot;
            this.callback = callback;
        }
    }
}
