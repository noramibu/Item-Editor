package me.noramibu.itemeditor.util;

import com.mojang.serialization.DataResult;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.SnbtPrinterTagVisitor;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;

public final class ItemComponentDiffUtil {

    private ItemComponentDiffUtil() {
    }

    public static Result diff(ItemStack original, ItemStack preview, RegistryAccess registryAccess) {
        Snapshot originalSnapshot = snapshot(original, registryAccess);
        if (originalSnapshot.error() != null) {
            return new Result(List.of(), originalSnapshot.error());
        }

        Snapshot previewSnapshot = snapshot(preview, registryAccess);
        if (previewSnapshot.error() != null) {
            return new Result(List.of(), previewSnapshot.error());
        }

        List<Entry> entries = new ArrayList<>();

        if (!Objects.equals(originalSnapshot.itemId(), previewSnapshot.itemId())) {
            entries.add(new Entry(
                    EntryType.CHANGED,
                    "id",
                    originalSnapshot.itemId(),
                    previewSnapshot.itemId()
            ));
        }

        if (originalSnapshot.count() != previewSnapshot.count()) {
            entries.add(new Entry(
                    EntryType.CHANGED,
                    "count",
                    Integer.toString(originalSnapshot.count()),
                    Integer.toString(previewSnapshot.count())
            ));
        }

        TreeSet<String> keys = new TreeSet<>();
        keys.addAll(originalSnapshot.components().keySet());
        keys.addAll(previewSnapshot.components().keySet());

        for (String key : keys) {
            String originalValue = originalSnapshot.components().get(key);
            String previewValue = previewSnapshot.components().get(key);
            if (Objects.equals(originalValue, previewValue)) {
                continue;
            }

            if (originalValue == null) {
                entries.add(new Entry(EntryType.ADDED, key, "", previewValue));
            } else if (previewValue == null) {
                entries.add(new Entry(EntryType.REMOVED, key, originalValue, ""));
            } else {
                entries.add(new Entry(EntryType.CHANGED, key, originalValue, previewValue));
            }
        }

        return new Result(List.copyOf(entries), null);
    }

    private static Snapshot snapshot(ItemStack stack, RegistryAccess registryAccess) {
        DataResult<Tag> encoded = ItemStack.CODEC.encodeStart(
                registryAccess.createSerializationContext(NbtOps.INSTANCE),
                stack
        );

        Tag root = encoded.result().orElse(null);
        if (!(root instanceof CompoundTag compound)) {
            String error = encoded.error().map(DataResult.Error::message).orElse(ItemEditorText.str("raw.unknown_error"));
            return new Snapshot("", 0, Map.of(), ItemEditorText.str("raw.serialize_failed", error));
        }

        String itemId = compound.getStringOr("id", "");
        int count = compound.getIntOr("count", 0);
        CompoundTag components = compound.getCompoundOrEmpty("components");

        Map<String, String> componentMap = new LinkedHashMap<>();
        for (String key : new TreeSet<>(components.keySet())) {
            Tag value = components.get(key);
            if (value != null) {
                componentMap.put(key, new SnbtPrinterTagVisitor().visit(value));
            }
        }

        return new Snapshot(itemId, count, componentMap, null);
    }

    public enum EntryType {
        ADDED,
        REMOVED,
        CHANGED
    }

    public record Entry(EntryType type, String key, String originalValue, String previewValue) {
    }

    public record Result(List<Entry> entries, String error) {
    }

    private record Snapshot(String itemId, int count, Map<String, String> components, String error) {
    }
}
