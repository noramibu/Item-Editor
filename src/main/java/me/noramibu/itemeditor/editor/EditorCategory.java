package me.noramibu.itemeditor.editor;

import me.noramibu.itemeditor.util.ItemEditorText;
import net.minecraft.network.chat.Component;

public enum EditorCategory {
    GENERAL("category.general"),
    COMPONENTS("category.components"),
    DISPLAY("category.display"),
    ATTRIBUTES("category.attributes"),
    ENCHANTMENTS("category.enchantments"),
    COMBAT("category.combat"),
    FLAGS("category.flags"),
    BOOK("category.book"),
    RAW_EDITOR("category.raw_editor"),
    SPECIAL_DATA("category.special_data");

    private final Component title;

    EditorCategory(String key) {
        this.title = ItemEditorText.tr(key + ".title");
    }

    public Component title() {
        return this.title;
    }
}
