package me.noramibu.itemeditor.storage;

import net.minecraft.client.Minecraft;

public final class StorageServices {

    private static StorageDataFoundation foundation;
    private static SavedItemStorageService savedItems;

    private StorageServices() {
    }

    public static synchronized void initialize(Minecraft minecraft) {
        if (foundation != null) {
            return;
        }
        foundation = StorageDataFoundation.create(minecraft);
        foundation.initialize();
        savedItems = new SavedItemStorageService(foundation);
    }

    public static synchronized StorageDataFoundation foundation() {
        if (foundation == null) {
            initialize(Minecraft.getInstance());
        }
        return foundation;
    }

    public static synchronized SavedItemStorageService savedItems() {
        if (savedItems == null) {
            initialize(Minecraft.getInstance());
        }
        return savedItems;
    }
}
