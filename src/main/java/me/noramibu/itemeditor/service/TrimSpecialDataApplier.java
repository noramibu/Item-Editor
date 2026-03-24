package me.noramibu.itemeditor.service;

import me.noramibu.itemeditor.editor.ValidationMessage;
import me.noramibu.itemeditor.util.ItemEditorText;
import me.noramibu.itemeditor.util.RegistryUtil;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import net.minecraft.world.item.equipment.trim.TrimMaterial;
import net.minecraft.world.item.equipment.trim.TrimPattern;

import java.util.Objects;

final class TrimSpecialDataApplier extends AbstractPreviewApplierSupport implements SpecialDataApplier {

    @Override
    public void apply(SpecialDataApplyContext context) {
        if (Objects.equals(context.special().trimMaterialId, context.baselineSpecial().trimMaterialId)
                && Objects.equals(context.special().trimPatternId, context.baselineSpecial().trimPatternId)) {
            this.restoreOriginalComponent(context.originalStack(), context.previewStack(), DataComponents.TRIM);
            return;
        }

        if (context.special().trimMaterialId.isBlank() || context.special().trimPatternId.isBlank()) {
            this.clearToPrototype(context.previewStack(), DataComponents.TRIM);
            return;
        }

        var materialRegistry = context.registryAccess().lookupOrThrow(Registries.TRIM_MATERIAL);
        var patternRegistry = context.registryAccess().lookupOrThrow(Registries.TRIM_PATTERN);
        Holder<TrimMaterial> material = RegistryUtil.resolveHolder(materialRegistry, context.special().trimMaterialId);
        Holder<TrimPattern> pattern = RegistryUtil.resolveHolder(patternRegistry, context.special().trimPatternId);
        if (material == null || pattern == null) {
            context.messages().add(ValidationMessage.error(ItemEditorText.str("preview.validation.trim")));
            return;
        }

        context.previewStack().set(DataComponents.TRIM, new ArmorTrim(material, pattern));
    }
}
