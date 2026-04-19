package me.noramibu.itemeditor.service;

import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.editor.ValidationMessage;
import me.noramibu.itemeditor.util.ItemEditorText;
import me.noramibu.itemeditor.util.RegistryUtil;
import me.noramibu.itemeditor.util.ValidationUtil;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.List;
import java.util.Objects;

final class EnchantmentsPreviewApplier extends AbstractPreviewApplierSupport implements ItemPreviewApplier {

    @Override
    public void apply(ItemPreviewApplyContext context) {
        if (this.sameEnchantmentDrafts(context.state().enchantments, context.baselineState().enchantments)
                && this.sameEnchantmentDrafts(context.state().storedEnchantments, context.baselineState().storedEnchantments)
                && context.state().unsafeEnchantments == context.baselineState().unsafeEnchantments) {
            this.restoreOriginalComponent(context.originalStack(), context.previewStack(), DataComponents.ENCHANTMENTS);
            this.restoreOriginalComponent(context.originalStack(), context.previewStack(), DataComponents.STORED_ENCHANTMENTS);
            return;
        }

        Registry<Enchantment> enchantmentRegistry = context.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);

        ItemEnchantments enchantments = this.buildEnchantments(
                context.previewStack(),
                context.state().enchantments,
                context.state().unsafeEnchantments,
                enchantmentRegistry,
                context.messages()
        );
        ItemEnchantments storedEnchantments = this.buildEnchantments(
                context.previewStack(),
                context.state().storedEnchantments,
                true,
                enchantmentRegistry,
                context.messages()
        );

        if (enchantments.isEmpty()) {
            this.clearToPrototype(context.previewStack(), DataComponents.ENCHANTMENTS);
        } else {
            context.previewStack().set(DataComponents.ENCHANTMENTS, enchantments);
        }

        if (storedEnchantments.isEmpty()) {
            this.clearToPrototype(context.previewStack(), DataComponents.STORED_ENCHANTMENTS);
        } else {
            context.previewStack().set(DataComponents.STORED_ENCHANTMENTS, storedEnchantments);
        }
    }

    private ItemEnchantments buildEnchantments(
            net.minecraft.world.item.ItemStack preview,
            List<ItemEditorState.EnchantmentDraft> drafts,
            boolean unsafeAllowed,
            Registry<Enchantment> enchantmentRegistry,
            List<ValidationMessage> messages
    ) {
        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);

        for (ItemEditorState.EnchantmentDraft draft : drafts) {
            if (draft.enchantmentId.isBlank()) continue;

            Holder<Enchantment> enchantment = RegistryUtil.resolveHolder(enchantmentRegistry, draft.enchantmentId);
            if (enchantment == null) {
                messages.add(ValidationMessage.error(ItemEditorText.str(
                        "validation.registry_missing",
                        ItemEditorText.str("enchantments.entry.enchantment"),
                        draft.enchantmentId
                )));
                continue;
            }

            int maxLevel = unsafeAllowed ? Integer.MAX_VALUE : Enchantment.MAX_LEVEL;
            Integer level = ValidationUtil.parseInt(draft.level, ItemEditorText.str("enchantments.entry.level"), 1, maxLevel, messages);
            if (level == null) continue;

            if (!unsafeAllowed) {
                if (level < enchantment.value().getMinLevel() || level > enchantment.value().getMaxLevel()) {
                    messages.add(ValidationMessage.error(ItemEditorText.str("preview.validation.enchantment_level", draft.enchantmentId, enchantment.value().getMinLevel(), enchantment.value().getMaxLevel())));
                    continue;
                }
                if (!enchantment.value().canEnchant(preview)) {
                    messages.add(ValidationMessage.warning(ItemEditorText.str("preview.validation.enchantment_applicable", draft.enchantmentId)));
                }
            }

            mutable.set(enchantment, level);
        }

        return mutable.toImmutable();
    }

    private boolean sameEnchantmentDrafts(List<ItemEditorState.EnchantmentDraft> current, List<ItemEditorState.EnchantmentDraft> baseline) {
        return this.sameList(current, baseline, (left, right) -> Objects.equals(left.enchantmentId, right.enchantmentId)
                && Objects.equals(left.level, right.level));
    }
}
