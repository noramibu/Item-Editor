package me.noramibu.itemeditor.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public final class ClientInventorySyncService {

    private ClientInventorySyncService() {}

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

    public static boolean dropStack(Minecraft minecraft, ItemStack stack) {
        if (minecraft == null || minecraft.player == null || stack == null || stack.isEmpty()) {
            return false;
        }
        ItemStack copy = stack.copy();
        var singleplayerServer = minecraft.getSingleplayerServer();
        if (singleplayerServer != null) {
            UUID playerId = minecraft.player.getUUID();
            singleplayerServer.execute(() -> {
                ServerPlayer serverPlayer = singleplayerServer.getPlayerList().getPlayer(playerId);
                if (serverPlayer != null) {
                    serverPlayer.drop(copy, false, true);
                }
            });
            return true;
        }
        var connection = minecraft.getConnection();
        if (minecraft.gameMode == null
                || connection == null
                || !minecraft.player.hasInfiniteMaterials()
                || !connection.isFeatureEnabled(copy.getItem().requiredFeatures())) {
            return false;
        }
        connection.send(new ServerboundSetCreativeModeSlotPacket(-1, copy));
        minecraft.player.getDropSpamThrottler().increment();
        return true;
    }

    public static boolean syncSlot(Minecraft minecraft, int slot, ItemStack stack) {
        if (minecraft == null || minecraft.player == null || slot < 0 || slot >= Inventory.INVENTORY_SIZE) {
            return false;
        }
        ItemStack copy = stack == null ? ItemStack.EMPTY : stack.copy();
        var singleplayerServer = minecraft.getSingleplayerServer();
        if (singleplayerServer != null) {
            RegistryAccess clientRegistryAccess =
                    minecraft.level == null ? RegistryAccess.EMPTY : minecraft.level.registryAccess();
            Optional<ItemStack> serverStack =
                    rebindForRegistryTransfer(copy, clientRegistryAccess, singleplayerServer.registryAccess());
            if (serverStack.isEmpty()) {
                return false;
            }
            UUID playerId = minecraft.player.getUUID();
            singleplayerServer.execute(() -> {
                ServerPlayer serverPlayer = singleplayerServer.getPlayerList().getPlayer(playerId);
                if (serverPlayer == null) {
                    return;
                }
                serverPlayer.getInventory().setItem(slot, serverStack.get().copy());
                serverPlayer.inventoryMenu.broadcastChanges();
                serverPlayer.containerMenu.broadcastChanges();
            });
            return true;
        }
        if (minecraft.gameMode != null && minecraft.player.hasInfiniteMaterials()) {
            minecraft.gameMode.handleCreativeModeItemAdd(
                    copy, slot < Inventory.getSelectionSize() ? Inventory.INVENTORY_SIZE + slot : slot);
            return true;
        }
        return false;
    }

    private static Optional<ItemStack> rebindForRegistryTransfer(
            ItemStack stack, RegistryAccess sourceRegistryAccess, RegistryAccess targetRegistryAccess) {
        if (stack.isEmpty()) {
            return Optional.of(ItemStack.EMPTY);
        }
        return ItemStack.CODEC
                .encodeStart(sourceRegistryAccess.createSerializationContext(NbtOps.INSTANCE), stack)
                .flatMap(encoded -> ItemStack.CODEC.parse(
                        targetRegistryAccess.createSerializationContext(NbtOps.INSTANCE), encoded))
                .result()
                .map(ItemStack::copy);
    }
}
