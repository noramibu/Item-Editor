package me.noramibu.itemeditor;

import dev.faststats.Metrics;
import dev.faststats.fabric.FabricContext;
import me.noramibu.itemeditor.client.command.StorageCommands;
import me.noramibu.itemeditor.client.input.ItemEditorKeybinds;
import me.noramibu.itemeditor.service.PostApplyVerificationService;
import me.noramibu.itemeditor.storage.StorageServices;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;

public final class ItemEditorClient implements ClientModInitializer {

    public static final String MOD_ID = "itemeditor";

    @Override
    public void onInitializeClient() {
        new FabricContext.Factory(MOD_ID, "a5479221f69d76f653f372d49aef2b24")
                .metrics(Metrics.Factory::create)
                .create();
        StorageServices.initialize(Minecraft.getInstance());
        ItemEditorKeybinds.register();
        StorageCommands.register();
        PostApplyVerificationService.initialize();
    }
}
