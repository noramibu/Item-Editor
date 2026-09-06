package me.noramibu.itemeditor;

import me.noramibu.itemeditor.client.command.StorageCommands;
import me.noramibu.itemeditor.client.input.ItemEditorKeybinds;
import me.noramibu.itemeditor.service.PostApplyVerificationService;
import me.noramibu.itemeditor.service.UsageReporter;
import me.noramibu.itemeditor.storage.StorageServices;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;

public final class ItemEditorClient implements ClientModInitializer {

    public static final String MOD_ID = "itemeditor";

    @Override
    public void onInitializeClient() {
        StorageServices.initialize(Minecraft.getInstance());
        ItemEditorKeybinds.register();
        StorageCommands.register();
        PostApplyVerificationService.initialize();
        UsageReporter.initialize();
        ClientPlayConnectionEvents.DISCONNECT.register(
                (listener, client) -> StorageServices.savedItems().invalidateDecodedItemCaches());
        ClientPlayConnectionEvents.JOIN.register(
                (listener, sender, client) -> StorageServices.savedItems().invalidateDecodedItemCaches());
    }
}
