package me.noramibu.itemeditor.editor;

import me.noramibu.itemeditor.storage.model.SavedIndexEntryUtil;
import me.noramibu.itemeditor.storage.model.SavedIndexItemEntry;
import net.minecraft.world.item.ItemStack;

public sealed interface ItemEditorSessionOrigin permits ItemEditorSessionOrigin.Transient, ItemEditorSessionOrigin.Storage {

    ItemEditorSessionOrigin.Transient TRANSIENT = new ItemEditorSessionOrigin.Transient();

    record Transient() implements ItemEditorSessionOrigin {
    }

    record Storage(SavedIndexItemEntry entry, ItemStack originalSavedStack) implements ItemEditorSessionOrigin {
        public Storage {
            entry = SavedIndexEntryUtil.copy(entry);
            originalSavedStack = originalSavedStack == null ? ItemStack.EMPTY : originalSavedStack.copy();
        }
    }
}
