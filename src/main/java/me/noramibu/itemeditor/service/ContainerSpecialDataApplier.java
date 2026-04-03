package me.noramibu.itemeditor.service;

import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.editor.ValidationMessage;
import me.noramibu.itemeditor.util.IdFieldNormalizer;
import me.noramibu.itemeditor.util.ItemEditorText;
import me.noramibu.itemeditor.util.ValidationUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

final class ContainerSpecialDataApplier extends AbstractPreviewApplierSupport implements SpecialDataApplier {

    @Override
    public void apply(SpecialDataApplyContext context) {
        if (this.sameContainerEntries(context.special().containerEntries, context.baselineSpecial().containerEntries)) {
            this.restoreOriginalComponent(context.originalStack(), context.previewStack(), DataComponents.CONTAINER);
            return;
        }

        if (context.special().containerEntries.isEmpty()) {
            this.clearToPrototype(context.previewStack(), DataComponents.CONTAINER);
            return;
        }

        Map<Integer, ItemStack> slotStacks = new HashMap<>();
        int maxSlot = -1;

        for (ItemEditorState.ContainerEntryDraft draft : context.special().containerEntries) {
            if (draft.itemId.isBlank()) {
                continue;
            }

            Integer slot = ValidationUtil.parseInt(draft.slot, ItemEditorText.str("special.container.slot_index"), 0, 255, context.messages());
            if (slot == null) {
                continue;
            }

            ResourceLocation itemId = IdFieldNormalizer.parse(draft.itemId);
            if (itemId == null) {
                context.messages().add(ValidationMessage.error(ItemEditorText.str("preview.validation.container_item_id")));
                continue;
            }

            Item item = BuiltInRegistries.ITEM.getOptional(itemId).orElse(null);
            if (item == null || item == Items.AIR) {
                context.messages().add(ValidationMessage.error(ItemEditorText.str(
                        "validation.registry_missing",
                        ItemEditorText.str("special.container.item"),
                        itemId
                )));
                continue;
            }

            int maxStackSize = Math.max(1, new ItemStack(item).getMaxStackSize());
            Integer count = ValidationUtil.parseInt(draft.count, ItemEditorText.str("special.container.count"), 1, maxStackSize, context.messages());
            if (count == null) {
                continue;
            }

            ItemStack entryStack = this.buildContainerEntryStack(draft, item, count);
            if (!entryStack.isEmpty()) {
                slotStacks.put(slot, entryStack);
                maxSlot = Math.max(maxSlot, slot);
            }
        }

        if (slotStacks.isEmpty() || maxSlot < 0) {
            this.clearToPrototype(context.previewStack(), DataComponents.CONTAINER);
            return;
        }

        List<ItemStack> slots = new ArrayList<>(maxSlot + 1);
        for (int slot = 0; slot <= maxSlot; slot++) {
            slots.add(ItemStack.EMPTY);
        }
        slotStacks.forEach(slots::set);

        ItemContainerContents contents = ItemContainerContents.fromItems(slots);
        if (contents.equals(ItemContainerContents.EMPTY)) {
            this.clearToPrototype(context.previewStack(), DataComponents.CONTAINER);
        } else {
            context.previewStack().set(DataComponents.CONTAINER, contents);
        }
    }

    private ItemStack buildContainerEntryStack(ItemEditorState.ContainerEntryDraft draft, Item item, int count) {
        if (draft.templateStack != null && !draft.templateStack.isEmpty() && draft.templateStack.is(item)) {
            return draft.templateStack.copyWithCount(count);
        }
        return new ItemStack(item, count);
    }

    private boolean sameContainerEntries(List<ItemEditorState.ContainerEntryDraft> current, List<ItemEditorState.ContainerEntryDraft> baseline) {
        return this.sameList(current, baseline, (left, right) -> Objects.equals(left.slot, right.slot)
                && Objects.equals(left.itemId, right.itemId)
                && Objects.equals(left.count, right.count));
    }
}
