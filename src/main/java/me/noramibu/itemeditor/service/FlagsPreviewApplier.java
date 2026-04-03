package me.noramibu.itemeditor.service;

import me.noramibu.itemeditor.editor.ValidationMessage;
import me.noramibu.itemeditor.util.ItemEditorText;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;

final class FlagsPreviewApplier extends AbstractPreviewApplierSupport implements ItemPreviewApplier {

    @Override
    public void apply(ItemPreviewApplyContext context) {
        if (context.state().hideTooltip == context.baselineState().hideTooltip
                && context.state().hiddenTooltipComponents.equals(context.baselineState().hiddenTooltipComponents)) {
            this.restoreOriginalComponent(context.originalStack(), context.previewStack(), DataComponents.HIDE_TOOLTIP);
            this.restoreOriginalComponent(context.originalStack(), context.previewStack(), DataComponents.HIDE_ADDITIONAL_TOOLTIP);
            return;
        }

        if (context.state().hideTooltip) {
            context.previewStack().set(DataComponents.HIDE_TOOLTIP, net.minecraft.util.Unit.INSTANCE);
        } else {
            this.clearToPrototype(context.previewStack(), DataComponents.HIDE_TOOLTIP);
        }

        if (context.state().hiddenTooltipComponents.isEmpty()) {
            this.clearToPrototype(context.previewStack(), DataComponents.HIDE_ADDITIONAL_TOOLTIP);
        } else {
            for (String hiddenId : context.state().hiddenTooltipComponents) {
                ResourceLocation identifier = ResourceLocation.tryParse(hiddenId);
                if (identifier == null) {
                    context.messages().add(ValidationMessage.error(ItemEditorText.str("preview.validation.hidden_component_id", hiddenId)));
                    continue;
                }

                if (net.minecraft.core.registries.BuiltInRegistries.DATA_COMPONENT_TYPE.getOptional(identifier).isEmpty()) {
                    context.messages().add(ValidationMessage.error(ItemEditorText.str("preview.validation.hidden_component_unknown", hiddenId)));
                }
            }
            context.previewStack().set(DataComponents.HIDE_ADDITIONAL_TOOLTIP, net.minecraft.util.Unit.INSTANCE);
        }
    }
}
