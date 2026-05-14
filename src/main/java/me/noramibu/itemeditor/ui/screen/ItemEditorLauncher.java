package me.noramibu.itemeditor.ui.screen;

import me.noramibu.itemeditor.editor.ItemEditorSession;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

public final class ItemEditorLauncher {

    private ItemEditorLauncher() {
    }

    public static void open(Minecraft client) {
        if (client.player == null || client.level == null) {
            return;
        }

        ItemStack held = client.player.getInventory().getSelectedItem();
        if (held.isEmpty()) {
            client.setScreen(new ItemEntryScreen(client));
            return;
        }

        client.setScreen(new ItemEditorScreen(new ItemEditorSession(client, held)));
    }
}
