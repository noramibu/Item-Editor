package me.noramibu.itemeditor.storage;

import net.minecraft.SharedConstants;

public final class StorageMetadataUtil {

    private StorageMetadataUtil() {
    }

    public static boolean isGeneratedDefaultName(String name, int index) {
        return name == null || name.isBlank() || ("Page " + (Math.max(0, index) + 1)).equals(name);
    }

    public static String currentMinecraftVersion() {
        try {
            return SharedConstants.getCurrentVersion().id();
        } catch (RuntimeException ignored) {
            return "";
        }
    }

    public static int currentDataVersion() {
        try {
            return SharedConstants.getCurrentVersion().dataVersion().version();
        } catch (RuntimeException ignored) {
            return 0;
        }
    }
}
