package me.noramibu.itemeditor.ui.screen;

import me.noramibu.itemeditor.editor.ItemEditorSession;
import me.noramibu.itemeditor.util.ItemEditorText;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

public final class ItemEditorLauncher {

    private ItemEditorLauncher() {
    }

    public static void open(Minecraft client) {
        if (client.player == null || client.level == null) {
            return;
        }

        ItemStack held = client.player.getInventory().getSelected();
        if (held.isEmpty()) {
            client.player.displayClientMessage(ItemEditorText.tr("launcher.hold_item").copy().withStyle(ChatFormatting.RED), true);
            return;
        }

        client.setScreen(new ItemEditorScreen(new ItemEditorSession(client, held)));
    }
}
