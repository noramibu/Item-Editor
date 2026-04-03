package me.noramibu.itemeditor.util;

import com.mojang.serialization.Codec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;

import java.util.Set;

public final class NbtCompatUtil {

    private NbtCompatUtil() {
    }

    public static String getStringOr(CompoundTag tag, String key, String fallback) {
        return tag.contains(key, Tag.TAG_STRING) ? tag.getString(key) : fallback;
    }

    public static int getIntOr(CompoundTag tag, String key, int fallback) {
        return tag.contains(key, Tag.TAG_ANY_NUMERIC) ? tag.getInt(key) : fallback;
    }

    public static float getFloatOr(CompoundTag tag, String key, float fallback) {
        return tag.contains(key, Tag.TAG_ANY_NUMERIC) ? tag.getFloat(key) : fallback;
    }

    public static boolean getBooleanOr(CompoundTag tag, String key, boolean fallback) {
        return tag.contains(key, Tag.TAG_BYTE) ? tag.getBoolean(key) : fallback;
    }

    public static CompoundTag getCompoundOrEmpty(CompoundTag tag, String key) {
        return tag.contains(key, Tag.TAG_COMPOUND) ? tag.getCompound(key) : new CompoundTag();
    }

    public static ListTag getListOrEmpty(CompoundTag tag, String key) {
        return tag.contains(key, Tag.TAG_LIST) ? tag.getList(key, Tag.TAG_COMPOUND) : new ListTag();
    }

    public static CompoundTag getCompoundOrEmpty(ListTag list, int index) {
        return index >= 0 && index < list.size() ? list.getCompound(index) : new CompoundTag();
    }

    public static Set<String> keys(CompoundTag tag) {
        return tag.getAllKeys();
    }

    public static <T> T readOrDefault(CompoundTag tag, String key, Codec<T> codec, T fallback) {
        if (!tag.contains(key)) {
            return fallback;
        }
        return codec.parse(NbtOps.INSTANCE, tag.get(key)).result().orElse(fallback);
    }

    public static <T> void store(CompoundTag tag, String key, Codec<T> codec, T value) {
        codec.encodeStart(NbtOps.INSTANCE, value).result().ifPresent(encoded -> tag.put(key, encoded));
    }
}
