package me.noramibu.itemeditor.editor;

import me.noramibu.itemeditor.service.ItemApplyService;
import me.noramibu.itemeditor.storage.model.SavedIndexEntryUtil;
import me.noramibu.itemeditor.storage.model.SavedIndexItemEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Function;

public sealed interface ItemEditorSessionOrigin permits ItemEditorSessionOrigin.Transient, ItemEditorSessionOrigin.Storage, ItemEditorSessionOrigin.External {

    ItemEditorSessionOrigin.Transient TRANSIENT = new ItemEditorSessionOrigin.Transient();

    record Transient() implements ItemEditorSessionOrigin {
    }

    record Storage(SavedIndexItemEntry entry, ItemStack originalSavedStack) implements ItemEditorSessionOrigin {
        public Storage {
            entry = SavedIndexEntryUtil.copy(entry);
            originalSavedStack = originalSavedStack == null ? ItemStack.EMPTY : originalSavedStack.copy();
        }
    }

    record External(
            @Nullable Screen returnScreen,
            Function<ItemStack, ItemApplyService.ApplyResult> saveHandler,
            int verificationSlot
    ) implements ItemEditorSessionOrigin {
        public External {
            Objects.requireNonNull(saveHandler, "saveHandler");
        }
    }
}
