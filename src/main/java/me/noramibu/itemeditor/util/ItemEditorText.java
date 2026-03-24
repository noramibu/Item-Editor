package me.noramibu.itemeditor.util;

import net.minecraft.network.chat.Component;

public final class ItemEditorText {

    private static final String ROOT = "itemeditor";

    private ItemEditorText() {
    }

    public static Component tr(String path, Object... args) {
        return Component.translatable(key(path), args);
    }

    public static String str(String path, Object... args) {
        return tr(path, args).getString();
    }

    public static String key(String path) {
        return ROOT + "." + path;
    }
}
