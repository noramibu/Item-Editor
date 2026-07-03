package me.noramibu.itemeditor.util;

import net.minecraft.network.chat.Component;

public final class ItemEditorText {

    private static final String ROOT = "itemeditor";
    private static final String CHAT_PREFIX = "[Item Editor] ";

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

    public static String prefixedMessage(String message) {
        return message == null || message.startsWith(CHAT_PREFIX) ? message : CHAT_PREFIX + message;
    }
}
