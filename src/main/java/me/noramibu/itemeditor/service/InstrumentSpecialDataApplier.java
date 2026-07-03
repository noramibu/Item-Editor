package me.noramibu.itemeditor.service;

import me.noramibu.itemeditor.editor.ValidationMessage;
import me.noramibu.itemeditor.util.IdFieldNormalizer;
import me.noramibu.itemeditor.util.InstrumentDetails;
import me.noramibu.itemeditor.util.ItemEditorText;
import me.noramibu.itemeditor.util.RegistryUtil;
import me.noramibu.itemeditor.util.TextComponentUtil;
import me.noramibu.itemeditor.util.ValidationUtil;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Instrument;
import net.minecraft.world.item.component.InstrumentComponent;

import java.util.Objects;

final class InstrumentSpecialDataApplier extends AbstractPreviewApplierSupport implements SpecialDataApplier {

    @Override
    public void apply(SpecialDataApplyContext context) {
        InstrumentDetails details = InstrumentDetails.fromSpecial(context.special());
        if (this.matchesBaseline(context, details)) {
            this.restoreOriginalComponent(context.originalStack(), context.previewStack(), DataComponents.INSTRUMENT);
            return;
        }

        if (context.special().instrumentId.isBlank() && details.isBlank()) {
            this.clearToPrototype(context.previewStack(), DataComponents.INSTRUMENT);
            return;
        }

        Holder<Instrument> registryInstrument = this.resolveRegistryInstrument(context);
        if (registryInstrument != null
                && (details.isBlank() || details.equals(InstrumentDetails.fromInstrument(registryInstrument.value())))) {
            context.previewStack().set(DataComponents.INSTRUMENT, new InstrumentComponent(registryInstrument));
            return;
        }

        Holder<SoundEvent> soundEvent = this.resolveSoundEvent(context, details.soundEventId());
        if (soundEvent == null) {
            context.messages().add(ValidationMessage.error(ItemEditorText.str(
                    "validation.registry_missing",
                    ItemEditorText.str("special.misc.instrument.sound_event"),
                    details.soundEventId()
            )));
            return;
        }

        Float useDuration = ValidationUtil.parseFloat(
                details.useDuration(),
                ItemEditorText.str("special.misc.instrument.use_duration"),
                context.messages()
        );
        Float range = ValidationUtil.parseFloat(
                details.range(),
                ItemEditorText.str("special.misc.instrument.range"),
                context.messages()
        );
        if (useDuration == null || range == null) {
            return;
        }

        Instrument instrument = new Instrument(
                soundEvent,
                useDuration,
                range,
                TextComponentUtil.parseMarkup(details.description())
        );
        context.previewStack().set(DataComponents.INSTRUMENT, new InstrumentComponent(Holder.direct(instrument)));
    }

    private Holder<SoundEvent> resolveSoundEvent(SpecialDataApplyContext context, String soundEventId) {
        Holder<SoundEvent> registrySound = RegistryUtil.resolveHolder(
                context.registryAccess().lookupOrThrow(Registries.SOUND_EVENT),
                soundEventId
        );
        if (registrySound != null) {
            return registrySound;
        }

        Identifier id = IdFieldNormalizer.parse(soundEventId);
        return id == null ? null : Holder.direct(SoundEvent.createVariableRangeEvent(id));
    }

    private boolean matchesBaseline(SpecialDataApplyContext context, InstrumentDetails details) {
        return Objects.equals(context.special().instrumentId, context.baselineSpecial().instrumentId)
                && Objects.equals(details, InstrumentDetails.fromSpecial(context.baselineSpecial()));
    }

    private Holder<Instrument> resolveRegistryInstrument(SpecialDataApplyContext context) {
        if (context.special().instrumentId.isBlank()) {
            return null;
        }

        Identifier instrumentId = IdFieldNormalizer.parse(context.special().instrumentId);
        if (instrumentId == null) {
            return null;
        }

        Registry<Instrument> registry = context.registryAccess().lookupOrThrow(Registries.INSTRUMENT);
        return RegistryUtil.resolveHolder(registry, instrumentId.toString());
    }
}
