package me.noramibu.itemeditor.service;

import me.noramibu.itemeditor.editor.ValidationMessage;
import me.noramibu.itemeditor.util.ItemEditorText;
import me.noramibu.itemeditor.util.ValidationUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.MapItemColor;
import net.minecraft.world.item.component.MapPostProcessing;

import java.util.Objects;

final class MapSpecialDataApplier extends AbstractPreviewApplierSupport implements SpecialDataApplier {

    @Override
    public void apply(SpecialDataApplyContext context) {
        if (Objects.equals(context.special().mapColor, context.baselineSpecial().mapColor)
                && Objects.equals(context.special().mapPostProcessing, context.baselineSpecial().mapPostProcessing)) {
            this.restoreOriginalComponent(context.originalStack(), context.previewStack(), DataComponents.MAP_COLOR);
            this.restoreOriginalComponent(context.originalStack(), context.previewStack(), DataComponents.MAP_POST_PROCESSING);
            return;
        }

        if (context.special().mapColor.isBlank()) {
            this.clearToPrototype(context.previewStack(), DataComponents.MAP_COLOR);
        } else {
            Integer mapColor = ValidationUtil.parseColor(context.special().mapColor, ItemEditorText.str("special.misc.map.color"), context.messages());
            if (mapColor != null) {
                context.previewStack().set(DataComponents.MAP_COLOR, new MapItemColor(mapColor));
            }
        }

        if (context.special().mapPostProcessing.isBlank()) {
            this.clearToPrototype(context.previewStack(), DataComponents.MAP_POST_PROCESSING);
        } else {
            try {
                context.previewStack().set(DataComponents.MAP_POST_PROCESSING, MapPostProcessing.valueOf(context.special().mapPostProcessing));
            } catch (IllegalArgumentException exception) {
                context.messages().add(ValidationMessage.error(ItemEditorText.str(
                        "validation.registry_missing",
                        ItemEditorText.str("special.misc.map.post"),
                        context.special().mapPostProcessing
                )));
            }
        }
    }
}
