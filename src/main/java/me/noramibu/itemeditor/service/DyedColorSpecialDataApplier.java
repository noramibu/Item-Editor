package me.noramibu.itemeditor.service;

import me.noramibu.itemeditor.util.ItemEditorText;
import me.noramibu.itemeditor.util.ValidationUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.DyedItemColor;

import java.util.Objects;

final class DyedColorSpecialDataApplier extends AbstractPreviewApplierSupport implements SpecialDataApplier {

    @Override
    public void apply(SpecialDataApplyContext context) {
        if (Objects.equals(context.special().dyedColor, context.baselineSpecial().dyedColor)) {
            this.restoreOriginalComponent(context.originalStack(), context.previewStack(), DataComponents.DYED_COLOR);
            return;
        }

        if (context.special().dyedColor.isBlank()) {
            this.clearToPrototype(context.previewStack(), DataComponents.DYED_COLOR);
            return;
        }

        Integer dyedColor = ValidationUtil.parseColor(context.special().dyedColor, ItemEditorText.str("special.misc.dyed.title"), context.messages());
        if (dyedColor != null) {
            context.previewStack().set(DataComponents.DYED_COLOR, new DyedItemColor(dyedColor, true));
        }
    }
}
