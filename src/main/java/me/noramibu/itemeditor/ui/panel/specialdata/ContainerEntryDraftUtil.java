package me.noramibu.itemeditor.ui.panel.specialdata;

import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.util.IdFieldNormalizer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

final class ContainerEntryDraftUtil {

    private ContainerEntryDraftUtil() {
    }

    static Item resolveItem(String rawItemId) {
        ResourceLocation id = IdFieldNormalizer.parse(rawItemId);
        if (id == null) {
            return null;
        }
        return BuiltInRegistries.ITEM.getOptional(id).orElse(null);
    }

    static void syncTemplateStack(ItemEditorState.ContainerEntryDraft draft, Item item, int count) {
        if (item == null || item == Items.AIR) {
            return;
        }
        if (draft.templateStack == null || draft.templateStack.isEmpty() || !draft.templateStack.is(item)) {
            draft.templateStack = new ItemStack(item, count);
            return;
        }
        draft.templateStack = draft.templateStack.copyWithCount(count);
    }
}
