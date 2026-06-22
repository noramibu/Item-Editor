package me.noramibu.itemeditor.service;

import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ClientInventorySyncService {

    private ClientInventorySyncService() {
    }

    public static Map<Integer, ItemStack> snapshot(Minecraft minecraft) {
        Map<Integer, ItemStack> snapshot = new HashMap<>();
        if (minecraft == null || minecraft.player == null) {
            return snapshot;
        }
        Inventory inventory = minecraft.player.getInventory();
        for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
            snapshot.put(slot, inventory.getItem(slot).copy());
        }
        return snapshot;
    }

    public static int syncChangedSlots(Minecraft minecraft, Map<Integer, ItemStack> before) {
        if (minecraft == null || minecraft.player == null || before == null || before.isEmpty()) {
            return 0;
        }
        int synced = 0;
        Inventory inventory = minecraft.player.getInventory();
        for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
            ItemStack oldStack = before.getOrDefault(slot, ItemStack.EMPTY);
            ItemStack newStack = inventory.getItem(slot).copy();
            if (!ItemStack.matches(oldStack, newStack) && syncSlot(minecraft, slot, newStack)) {
                synced++;
            }
        }
        return synced;
    }

    public static boolean putInFreeSlot(Minecraft minecraft, ItemStack stack) {
        if (minecraft == null || minecraft.player == null || stack == null || stack.isEmpty()) {
            return false;
        }
        Inventory inventory = minecraft.player.getInventory();
        int slot = inventory.getFreeSlot();
        if (slot < 0 || slot >= Inventory.INVENTORY_SIZE) {
            return false;
        }
        ItemStack copy = stack.copy();
        inventory.setItem(slot, copy.copy());
        if (syncSlot(minecraft, slot, copy)) {
            return true;
        }
        inventory.setItem(slot, ItemStack.EMPTY);
        return false;
    }

    public static boolean syncSlot(Minecraft minecraft, int slot, ItemStack stack) {
        if (minecraft == null || minecraft.player == null || slot < 0 || slot >= Inventory.INVENTORY_SIZE) {
            return false;
        }
        ItemStack copy = stack == null ? ItemStack.EMPTY : stack.copy();
        var singleplayerServer = minecraft.getSingleplayerServer();
        if (singleplayerServer != null) {
            UUID playerId = minecraft.player.getUUID();
            singleplayerServer.execute(() -> {
                ServerPlayer serverPlayer = singleplayerServer.getPlayerList().getPlayer(playerId);
                if (serverPlayer == null) {
                    return;
                }
                serverPlayer.getInventory().setItem(slot, copy.copy());
                serverPlayer.inventoryMenu.broadcastChanges();
                serverPlayer.containerMenu.broadcastChanges();
            });
            return true;
        }
        if (minecraft.gameMode != null && minecraft.player.hasInfiniteMaterials()) {
            minecraft.gameMode.handleCreativeModeItemAdd(
                    copy,
                    slot < Inventory.getSelectionSize() ? Inventory.INVENTORY_SIZE + slot : slot
            );
            return true;
        }
        return false;
    }
}
