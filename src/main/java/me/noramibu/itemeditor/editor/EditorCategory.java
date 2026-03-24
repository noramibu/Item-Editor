package me.noramibu.itemeditor.editor;

import me.noramibu.itemeditor.util.ItemEditorText;
import net.minecraft.network.chat.Component;

public enum EditorCategory {
    GENERAL("category.general"),
    DISPLAY("category.display"),
    ATTRIBUTES("category.attributes"),
    ENCHANTMENTS("category.enchantments"),
    FLAGS("category.flags"),
    BOOK("category.book"),
    SPECIAL_DATA("category.special_data");

    private final Component title;
    private final Component description;

    EditorCategory(String key) {
        this.title = ItemEditorText.tr(key + ".title");
        this.description = Component.empty();
    }

    public Component title() {
        return this.title;
    }

    public Component description() {
        return this.description;
    }
}
