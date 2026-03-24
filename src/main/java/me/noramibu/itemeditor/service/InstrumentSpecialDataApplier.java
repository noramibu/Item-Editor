package me.noramibu.itemeditor.service;

import me.noramibu.itemeditor.editor.ValidationMessage;
import me.noramibu.itemeditor.util.IdFieldNormalizer;
import me.noramibu.itemeditor.util.ItemEditorText;
import me.noramibu.itemeditor.util.RegistryUtil;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Instrument;
import net.minecraft.world.item.component.InstrumentComponent;

import java.util.Objects;

final class InstrumentSpecialDataApplier extends AbstractPreviewApplierSupport implements SpecialDataApplier {

    @Override
    public void apply(SpecialDataApplyContext context) {
        if (Objects.equals(context.special().instrumentId, context.baselineSpecial().instrumentId)) {
            this.restoreOriginalComponent(context.originalStack(), context.previewStack(), DataComponents.INSTRUMENT);
            return;
        }

        if (context.special().instrumentId.isBlank()) {
            this.clearToPrototype(context.previewStack(), DataComponents.INSTRUMENT);
            return;
        }

        Identifier instrumentId = IdFieldNormalizer.parse(context.special().instrumentId);
        if (instrumentId == null) {
            context.messages().add(ValidationMessage.error(ItemEditorText.str("preview.validation.instrument_id")));
            return;
        }

        Holder<Instrument> instrument = RegistryUtil.resolveHolder(
                context.registryAccess().lookupOrThrow(Registries.INSTRUMENT),
                instrumentId.toString()
        );
        if (instrument == null) {
            context.messages().add(ValidationMessage.error(ItemEditorText.str("preview.validation.instrument_id")));
            return;
        }

        context.previewStack().set(DataComponents.INSTRUMENT, new InstrumentComponent(instrument));
    }
}
