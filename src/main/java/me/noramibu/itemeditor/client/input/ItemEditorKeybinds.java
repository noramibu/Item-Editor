package me.noramibu.itemeditor.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import me.noramibu.itemeditor.ItemEditorClient;
import me.noramibu.itemeditor.storage.StorageSortMode;
import me.noramibu.itemeditor.ui.screen.ItemEditorLauncher;
import me.noramibu.itemeditor.ui.screen.StoragePagesScreen;
import me.noramibu.itemeditor.ui.screen.StorageScreen;
import me.noramibu.itemeditor.ui.screen.StorageScreenMode;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public final class ItemEditorKeybinds {

    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(ItemEditorClient.MOD_ID, "controls")
    );

    private static final KeyMapping OPEN_EDITOR = KeyMappingHelper.registerKeyMapping(
            new KeyMapping(
                    "key.itemeditor.open_editor",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_I,
                    CATEGORY
            )
    );
    private static final KeyMapping OPEN_STORAGE = KeyMappingHelper.registerKeyMapping(
            new KeyMapping(
                    "key.itemeditor.open_storage",
                    InputConstants.Type.KEYSYM,
                    InputConstants.UNKNOWN.getValue(),
                    CATEGORY
            )
    );
    private static final KeyMapping OPEN_STORAGE_PAGES = KeyMappingHelper.registerKeyMapping(
            new KeyMapping(
                    "key.itemeditor.open_storage_pages",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_U,
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
            while (OPEN_STORAGE.consumeClick()) {
                if (client.player != null && client.level != null) {
                    client.setScreenAndShow(new StorageScreen(1, "", StorageSortMode.REGULAR));
                }
            }
            while (OPEN_STORAGE_PAGES.consumeClick()) {
                if (client.player != null && client.level != null) {
                    client.setScreenAndShow(new StoragePagesScreen(
                            client,
                            1,
                            "",
                            StorageSortMode.REGULAR,
                            StorageScreenMode.MANAGE,
                            null
                    ));
                }
            }
        });
    }
}
