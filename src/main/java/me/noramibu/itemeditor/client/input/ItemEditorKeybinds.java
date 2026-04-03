package me.noramibu.itemeditor.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import me.noramibu.itemeditor.ItemEditorClient;
import me.noramibu.itemeditor.ui.screen.ItemEditorLauncher;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public final class ItemEditorKeybinds {

    private static final String CATEGORY = "key.categories." + ItemEditorClient.MOD_ID + ".controls";

    private static final KeyMapping OPEN_EDITOR = KeyBindingHelper.registerKeyBinding(
            new KeyMapping(
                    "key.itemeditor.open_editor",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_I,
                    CATEGORY
            )
    );

    private ItemEditorKeybinds() {
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (OPEN_EDITOR.consumeClick()) {
                ItemEditorLauncher.open(client);
            }
        });
    }
}
