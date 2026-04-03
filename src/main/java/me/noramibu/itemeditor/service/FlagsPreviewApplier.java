package me.noramibu.itemeditor.service;

import me.noramibu.itemeditor.editor.ValidationMessage;
import me.noramibu.itemeditor.util.ItemEditorText;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.LinkedHashSet;

final class FlagsPreviewApplier extends AbstractPreviewApplierSupport implements ItemPreviewApplier {

    @Override
    public void apply(ItemPreviewApplyContext context) {
        if (context.state().hideTooltip == context.baselineState().hideTooltip
                && context.state().hiddenTooltipComponents.equals(context.baselineState().hiddenTooltipComponents)) {
            this.restoreOriginalComponent(context.originalStack(), context.previewStack(), DataComponents.TOOLTIP_DISPLAY);
            return;
        }

        TooltipDisplay tooltipDisplay = new TooltipDisplay(context.state().hideTooltip, new LinkedHashSet<>());
        for (String hiddenId : context.state().hiddenTooltipComponents) {
            ResourceLocation identifier = ResourceLocation.tryParse(hiddenId);
            if (identifier == null) {
                context.messages().add(ValidationMessage.error(ItemEditorText.str("preview.validation.hidden_component_id", hiddenId)));
                continue;
            }

            var componentType = net.minecraft.core.registries.BuiltInRegistries.DATA_COMPONENT_TYPE.getOptional(identifier).orElse(null);
            if (componentType == null) {
                context.messages().add(ValidationMessage.error(ItemEditorText.str("preview.validation.hidden_component_unknown", hiddenId)));
                continue;
            }
            tooltipDisplay = tooltipDisplay.withHidden(componentType, true);
        }

        if (TooltipDisplay.DEFAULT.equals(tooltipDisplay)) {
            this.clearToPrototype(context.previewStack(), DataComponents.TOOLTIP_DISPLAY);
        } else {
            context.previewStack().set(DataComponents.TOOLTIP_DISPLAY, tooltipDisplay);
        }
    }
}
