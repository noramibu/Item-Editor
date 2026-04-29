package me.noramibu.itemeditor.storage.model;

import java.util.ArrayList;

public final class SavedIndexEntryUtil {

    private SavedIndexEntryUtil() {
    }

    public static SavedIndexItemEntry copy(SavedIndexItemEntry source) {
        SavedIndexItemEntry copy = new SavedIndexItemEntry();
        copy.id = source.id;
        copy.chunkId = source.chunkId;
        copy.slotInChunk = source.slotInChunk;
        copy.page = source.page;
        copy.slotInPage = source.slotInPage;
        copy.savedAt = source.savedAt;
        copy.updatedAt = source.updatedAt;
        copy.itemRegistryKey = source.itemRegistryKey;
        copy.stackCount = Math.max(1, source.stackCount);
        copy.nbtBytes = Math.max(0, source.nbtBytes);
        copy.customNamePlain = source.customNamePlain;
        copy.lorePlain = source.lorePlain == null ? new ArrayList<>() : new ArrayList<>(source.lorePlain);
        return copy;
    }
}
