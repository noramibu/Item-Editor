package me.noramibu.itemeditor.compat.rei;

import java.util.List;
import me.noramibu.itemeditor.ui.screen.StorageScreen;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.config.ConfigObject;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.screen.ExclusionZones;
import net.minecraft.client.gui.navigation.ScreenRectangle;

public final class ItemEditorReiClientPlugin implements REIClientPlugin {

    public static boolean preferRightController(boolean fallback) {
        ConfigObject config = ConfigObject.getInstance();
        return config.isOverlayVisible() ? config.isLeftHandSidePanel() : fallback;
    }

    @Override
    public void registerExclusionZones(ExclusionZones zones) {
        zones.register(StorageScreen.class, screen -> {
            ScreenRectangle bounds = screen.controllerBounds();
            return List.of(new Rectangle(bounds.left(), bounds.top(), bounds.width(), bounds.height()));
        });
    }
}
