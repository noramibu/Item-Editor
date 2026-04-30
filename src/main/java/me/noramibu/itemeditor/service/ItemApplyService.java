package me.noramibu.itemeditor.service;

import me.noramibu.itemeditor.util.ItemEditorText;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class ItemApplyService {

    public ApplyResult apply(Minecraft minecraft, ItemStack stack) {
        if (minecraft.player == null) {
            return ApplyResult.failure(ItemEditorText.str("apply.no_player"));
        }

        int selectedSlot = minecraft.player.getInventory().getSelectedSlot();
        ItemStack copy = stack.copy();

        var singleplayerServer = minecraft.getSingleplayerServer();
        if (singleplayerServer != null) {
            minecraft.player.getInventory().setItem(selectedSlot, copy.copy());
            singleplayerServer.execute(() -> {
                ServerPlayer serverPlayer = singleplayerServer.getPlayerList().getPlayer(minecraft.player.getUUID());
                if (serverPlayer == null) return;

                serverPlayer.getInventory().setItem(selectedSlot, copy.copy());
                serverPlayer.inventoryMenu.broadcastChanges();
                serverPlayer.containerMenu.broadcastChanges();
            });
            return ApplyResult.success(ItemEditorText.str("apply.singleplayer_success"));
        }

        if (minecraft.player.hasInfiniteMaterials() && minecraft.gameMode != null) {
            minecraft.player.getInventory().setItem(selectedSlot, copy.copy());
            minecraft.gameMode.handleCreativeModeItemAdd(copy, 36 + selectedSlot);
            return ApplyResult.success(ItemEditorText.str("apply.creative_success"));
        }

        return ApplyResult.failure(ItemEditorText.str("apply.multiplayer_preview_only"));
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
