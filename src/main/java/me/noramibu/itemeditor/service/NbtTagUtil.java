package me.noramibu.itemeditor.service;

import me.noramibu.itemeditor.util.TextComponentUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.ComponentSerialization;

final class NbtTagUtil {

    private NbtTagUtil() {
    }

    static void setBooleanKey(CompoundTag tag, String key, boolean value) {
        if (value) {
            tag.putBoolean(key, true);
        } else {
            tag.remove(key);
        }
    }

    static void setTextComponentKey(CompoundTag tag, String key, String rawText) {
        if (rawText == null || rawText.isBlank()) {
            tag.remove(key);
            return;
        }
        tag.store(
                key,
                ComponentSerialization.CODEC,
                TextComponentUtil.parseMarkup(rawText)
        );
    }
}
