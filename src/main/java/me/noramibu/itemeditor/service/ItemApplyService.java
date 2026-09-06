package me.noramibu.itemeditor.service;

import me.noramibu.itemeditor.util.ItemEditorText;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class ItemApplyService {

    public ApplyResult apply(Minecraft minecraft, ItemStack stack) {
        return minecraft.player == null
                ? ApplyResult.failure(ItemEditorText.str("apply.no_player"))
                : applyToSlot(minecraft, minecraft.player.getInventory().getSelectedSlot(), stack, null);
    }

    public static ApplyResult applyToSlot(
            Minecraft minecraft, int slot, ItemStack stack, @Nullable ItemStack expected) {
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

        inventory.setItem(slot, copy.copy());
        if (ClientInventorySyncService.syncSlot(minecraft, slot, copy)) {
            return ApplyResult.success(ItemEditorText.str(
                    minecraft.getSingleplayerServer() == null
                            ? "apply.creative_success"
                            : "apply.singleplayer_success"));
        }
        inventory.setItem(slot, previous);

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
