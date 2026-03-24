package me.noramibu.itemeditor.service;

import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.editor.ValidationMessage;
import me.noramibu.itemeditor.util.IdFieldNormalizer;
import me.noramibu.itemeditor.util.ItemEditorText;
import me.noramibu.itemeditor.util.ValidationUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BundleContents;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

final class BundleSpecialDataApplier extends AbstractPreviewApplierSupport implements SpecialDataApplier {

    @Override
    public void apply(SpecialDataApplyContext context) {
        if (this.sameBundleEntries(context.special().bundleEntries, context.baselineSpecial().bundleEntries)) {
            this.restoreOriginalComponent(context.originalStack(), context.previewStack(), DataComponents.BUNDLE_CONTENTS);
            return;
        }

        if (context.special().bundleEntries.isEmpty()) {
            this.clearToPrototype(context.previewStack(), DataComponents.BUNDLE_CONTENTS);
            return;
        }

        List<ItemStack> bundleStacks = new ArrayList<>();
        for (int index = 0; index < context.special().bundleEntries.size(); index++) {
            ItemEditorState.ContainerEntryDraft draft = context.special().bundleEntries.get(index);
            ItemStack entryStack = this.buildEntryStack(draft, index, context.messages());
            if (entryStack.isEmpty()) {
                continue;
            }
            if (!BundleContents.canItemBeInBundle(entryStack)) {
                context.messages().add(ValidationMessage.error(ItemEditorText.str("preview.validation.bundle_item_not_allowed", draft.itemId)));
                continue;
            }
            bundleStacks.add(entryStack);
        }

        if (bundleStacks.isEmpty()) {
            this.clearToPrototype(context.previewStack(), DataComponents.BUNDLE_CONTENTS);
            return;
        }

        BundleContents contents;
        try {
            BundleContents.Mutable mutable = new BundleContents.Mutable(new BundleContents(bundleStacks));
            int selected = Math.clamp(context.special().selectedBundleIndex, 0, bundleStacks.size() - 1);
            if (selected >= 0) {
                mutable.toggleSelectedItem(selected);
            }
            contents = mutable.toImmutable();
        } catch (RuntimeException exception) {
            context.messages().add(ValidationMessage.error(ItemEditorText.str("preview.validation.bundle_build_failed", exception.getMessage())));
            return;
        }

        context.previewStack().set(DataComponents.BUNDLE_CONTENTS, contents);
    }

    private ItemStack buildEntryStack(
            ItemEditorState.ContainerEntryDraft draft,
            int index,
            List<ValidationMessage> messages
    ) {
        if (draft.itemId.isBlank()) {
            return ItemStack.EMPTY;
        }

        Identifier itemId = IdFieldNormalizer.parse(draft.itemId);
        if (itemId == null) {
            messages.add(ValidationMessage.error(ItemEditorText.str("preview.validation.bundle_item_id", index + 1)));
            return ItemStack.EMPTY;
        }

        Item item = BuiltInRegistries.ITEM.getOptional(itemId).orElse(null);
        if (item == null || item == Items.AIR) {
            messages.add(ValidationMessage.error(ItemEditorText.str(
                    "validation.registry_missing",
                    ItemEditorText.str("special.container.item"),
                    itemId
            )));
            return ItemStack.EMPTY;
        }

        int maxStackSize = Math.max(1, new ItemStack(item).getMaxStackSize());
        Integer count = ValidationUtil.parseInt(draft.count, ItemEditorText.str("special.container.count"), 1, maxStackSize, messages);
        if (count == null) {
            return ItemStack.EMPTY;
        }

        if (draft.templateStack != null && !draft.templateStack.isEmpty() && draft.templateStack.is(item)) {
            return draft.templateStack.copyWithCount(count);
        }
        return new ItemStack(item, count);
    }

    private boolean sameBundleEntries(List<ItemEditorState.ContainerEntryDraft> current, List<ItemEditorState.ContainerEntryDraft> baseline) {
        return this.sameList(current, baseline, (left, right) -> Objects.equals(left.itemId, right.itemId)
                && Objects.equals(left.count, right.count));
    }
}
