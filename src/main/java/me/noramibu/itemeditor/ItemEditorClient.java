package me.noramibu.itemeditor;

import me.noramibu.itemeditor.client.input.ItemEditorKeybinds;
import me.noramibu.itemeditor.service.PostApplyVerificationService;
import net.fabricmc.api.ClientModInitializer;

@SuppressWarnings("unused")
public final class ItemEditorClient implements ClientModInitializer {

    public static final String MOD_ID = "itemeditor";

    @Override
    public void onInitializeClient() {
        ItemEditorKeybinds.register();
        PostApplyVerificationService.initialize();
    }
}
