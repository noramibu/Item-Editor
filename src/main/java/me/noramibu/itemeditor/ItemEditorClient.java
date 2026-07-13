package me.noramibu.itemeditor;

import dev.faststats.Metrics;
import dev.faststats.fabric.FabricContext;
import me.noramibu.itemeditor.client.command.StorageCommands;
import me.noramibu.itemeditor.client.input.ItemEditorKeybinds;
import me.noramibu.itemeditor.service.PostApplyVerificationService;
import me.noramibu.itemeditor.storage.StorageServices;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;
import org.slf4j.LoggerFactory;

public final class ItemEditorClient implements ClientModInitializer {

    public static final String MOD_ID = "itemeditor";

    @Override
    public void onInitializeClient() {
        try {
            new FabricContext.Factory(MOD_ID, "a5479221f69d76f653f372d49aef2b24")
                    .metrics(Metrics.Factory::create)
                    .create();
        } catch (UnsupportedClassVersionError error) {
            LoggerFactory.getLogger(MOD_ID).warn(
                    "[Item Editor] FastStats disabled because its compatibility layer requires a newer Java runtime"
            );
        }
        StorageServices.initialize(Minecraft.getInstance());
        ItemEditorKeybinds.register();
        StorageCommands.register();
        PostApplyVerificationService.initialize();
    }
}
