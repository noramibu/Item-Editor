package me.noramibu.itemeditor.api;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import me.noramibu.itemeditor.editor.ItemEditorSession;
import me.noramibu.itemeditor.editor.ItemEditorSessionOrigin;
import me.noramibu.itemeditor.service.ItemApplyService;
import me.noramibu.itemeditor.ui.screen.ItemEditorScreen;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** Client-side entry points for opening Item Editor from another mod. */
@Environment(EnvType.CLIENT)
public final class ItemEditorApi {

    private ItemEditorApi() {}

    /**
     * Opens a copy of {@code stack} and returns the edited copy to {@code onSave}.
     * The callback runs on the Minecraft client thread and is not called when editing is cancelled.
     * The caller is responsible for saving and synchronizing the returned item.
     *
     * @return whether the request was accepted
     */
    public static boolean open(ItemStack stack, Consumer<ItemStack> onSave) {
        if (stack == null || stack.isEmpty() || onSave == null) {
            return false;
        }
        ItemStack copy = stack.copy();
        return onClientThread(() -> openNow(
                copy,
                edited -> {
                    onSave.accept(edited.copy());
                    return ItemApplyService.ApplyResult.success("");
                },
                -1));
    }

    /**
     * Opens an item from the player's main inventory and saves it back to the same slot.
     * Saving is rejected if the slot changed while the editor was open.
     *
     * @param inventorySlot player inventory index from {@code 0} to {@link Inventory#INVENTORY_SIZE}{@code - 1}
     * @return whether the request was accepted
     */
    public static boolean openPlayerInventorySlot(int inventorySlot) {
        return onClientThread(() -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null || inventorySlot < 0 || inventorySlot >= Inventory.INVENTORY_SIZE) {
                return false;
            }
            ItemStack original =
                    minecraft.player.getInventory().getItem(inventorySlot).copy();
            if (original.isEmpty()) {
                return false;
            }
            return openNow(
                    original,
                    edited -> ItemApplyService.applyToSlot(minecraft, inventorySlot, edited, original),
                    inventorySlot);
        });
    }

    private static boolean onClientThread(BooleanSupplier action) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.isSameThread()) {
            return action.getAsBoolean();
        }
        minecraft.execute(action::getAsBoolean);
        return true;
    }

    private static boolean openNow(
            ItemStack stack, Function<ItemStack, ItemApplyService.ApplyResult> saveHandler, int verificationSlot) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.gui.screen() instanceof ItemEditorScreen) {
            return false;
        }
        ItemEditorSessionOrigin.External origin =
                new ItemEditorSessionOrigin.External(minecraft.gui.screen(), saveHandler, verificationSlot);
        minecraft.setScreenAndShow(new ItemEditorScreen(new ItemEditorSession(minecraft, stack, origin)));
        return true;
    }
}
