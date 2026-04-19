package me.noramibu.itemeditor.storage;

import me.noramibu.itemeditor.storage.model.SavedIndexItemEntry;
import net.minecraft.nbt.CompoundTag;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class SavedChunkCodec {

    private SavedChunkCodec() {
    }

    static SavedChunkData fromTag(CompoundTag root, String fallbackChunkId) {
        String chunkId = root.getStringOr("chunkId", fallbackChunkId);
        Map<Integer, SavedChunkEntry> entries = new HashMap<>();
        for (int slot = 0; slot < StorageConstants.CHUNK_SIZE; slot++) {
            CompoundTag entryTag = root.getCompoundOrEmpty(slotKey(slot));
            String id = entryTag.getStringOr("id", "");
            if (id.isBlank()) {
                continue;
            }
            entries.put(slot, new SavedChunkEntry(
                    id,
                    entryTag.getLongOr("savedAt", 0L),
                    entryTag.getLongOr("updatedAt", 0L),
                    entryTag.getCompoundOrEmpty("item")
            ));
        }
        return new SavedChunkData(chunkId, entries);
    }

    static CompoundTag toTag(SavedChunkData chunk) {
        CompoundTag root = new CompoundTag();
        root.putInt("schemaVersion", StorageConstants.SAVED_CHUNK_SCHEMA_VERSION);
        root.putString("chunkId", chunk.chunkId());
        for (int slot = 0; slot < StorageConstants.CHUNK_SIZE; slot++) {
            SavedChunkEntry entry = chunk.entries().get(slot);
            if (entry == null) {
                continue;
            }
            CompoundTag entryTag = new CompoundTag();
            entryTag.putString("id", entry.id());
            entryTag.putLong("savedAt", entry.savedAt());
            entryTag.putLong("updatedAt", entry.updatedAt());
            entryTag.put("item", entry.itemTag().copy());
            root.put(slotKey(slot), entryTag);
        }
        return root;
    }

    static DecodeKey decodeKey(CompoundTag itemTag) {
        if (itemTag == null) {
            return new DecodeKey(0, "");
        }
        int hash = itemTag.hashCode();
        String id = itemTag.getStringOr("id", "");
        int count = itemTag.getIntOr("count", 1);
        int componentsHash = itemTag.getCompoundOrEmpty("components").hashCode();
        return new DecodeKey(hash, id + "|" + count + "|" + componentsHash);
    }

    static long entriesSignature(List<SavedIndexItemEntry> entries) {
        long signature = 1125899906842597L;
        if (entries == null) {
            return signature;
        }
        for (SavedIndexItemEntry entry : entries) {
            if (entry == null) {
                continue;
            }
            signature = (signature * 31L) + (entry.id == null ? 0L : entry.id.hashCode());
            signature = (signature * 31L) + entry.updatedAt;
            signature = (signature * 31L) + entry.slotInPage;
        }
        return signature;
    }

    private static String slotKey(int slot) {
        return "slot_" + slot;
    }

    record DecodeKey(
            int tagHash,
            String fingerprint
    ) {
    }

    record SavedChunkData(
            String chunkId,
            Map<Integer, SavedChunkEntry> entries
    ) {
    }

    record SavedChunkEntry(
            String id,
            long savedAt,
            long updatedAt,
            CompoundTag itemTag
    ) {
    }
}
