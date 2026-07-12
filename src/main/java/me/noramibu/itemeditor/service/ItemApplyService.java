package me.noramibu.itemeditor.service;

import me.noramibu.itemeditor.util.ItemEditorText;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public final class ItemApplyService {

    public ApplyResult apply(Minecraft minecraft, ItemStack stack) {
        return minecraft.player == null
                ? ApplyResult.failure(ItemEditorText.str("apply.no_player"))
                : applyToSlot(minecraft, minecraft.player.getInventory().getSelectedSlot(), stack, null);
    }

    public static ApplyResult applyToSlot(
            Minecraft minecraft,
            int slot,
            ItemStack stack,
            @Nullable ItemStack expected
    ) {
        if (minecraft.player == null) {
            return ApplyResult.failure(ItemEditorText.str("apply.no_player"));
        }
        if (slot < 0 || slot >= Inventory.INVENTORY_SIZE) {
            return ApplyResult.failure(ItemEditorText.str("apply.verify.error"));
        }

        Inventory inventory = minecraft.player.getInventory();
        ItemStack previous = inventory.getItem(slot).copy();
        if (expected != null && !ItemStack.matches(previous, expected)) {
            return ApplyResult.failure(ItemEditorText.str("apply.verify.error"));
        }
        ItemStack copy = stack.copy();

        var singleplayerServer = minecraft.getSingleplayerServer();
        if (singleplayerServer != null) {
            RegistryAccess clientRegistryAccess = minecraft.level == null ? RegistryAccess.EMPTY : minecraft.level.registryAccess();
            Optional<ItemStack> serverStack = rebindForRegistryTransfer(copy, clientRegistryAccess, singleplayerServer.registryAccess());
            if (serverStack.isEmpty()) {
                return ApplyResult.failure(ItemEditorText.str("preview.validation.component_failed", "Failed to rebind item to singleplayer server registry"));
            }

            inventory.setItem(slot, copy.copy());
            singleplayerServer.execute(() -> {
                ServerPlayer serverPlayer = singleplayerServer.getPlayerList().getPlayer(minecraft.player.getUUID());
                if (serverPlayer == null) return;

                serverPlayer.getInventory().setItem(slot, serverStack.get().copy());
                serverPlayer.inventoryMenu.broadcastChanges();
                serverPlayer.containerMenu.broadcastChanges();
            });
            return ApplyResult.success(ItemEditorText.str("apply.singleplayer_success"));
        }

        inventory.setItem(slot, copy.copy());
        if (ClientInventorySyncService.syncSlot(minecraft, slot, copy)) {
            return ApplyResult.success(ItemEditorText.str("apply.creative_success"));
        }
        inventory.setItem(slot, previous);

        return ApplyResult.failure(ItemEditorText.str("apply.multiplayer_preview_only"));
    }

    private static Optional<ItemStack> rebindForRegistryTransfer(
            ItemStack stack,
            RegistryAccess sourceRegistryAccess,
            RegistryAccess targetRegistryAccess
    ) {
        return ItemStack.CODEC.encodeStart(
                        sourceRegistryAccess.createSerializationContext(NbtOps.INSTANCE),
                        stack
                )
                .flatMap(encoded -> ItemStack.CODEC.parse(
                        targetRegistryAccess.createSerializationContext(NbtOps.INSTANCE),
                        encoded
                ))
                .result()
                .map(ItemStack::copy);
    }

    public record ApplyResult(boolean success, String message) {
        public static ApplyResult success(String message) {
            return new ApplyResult(true, message);
        }

        public static ApplyResult failure(String message) {
            return new ApplyResult(false, message);
        }
    }
}
