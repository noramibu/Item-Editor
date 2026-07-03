package me.noramibu.itemeditor.util;

import me.noramibu.itemeditor.editor.ItemEditorState;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Instrument;

public record InstrumentDetails(
        String description,
        String soundEventId,
        String useDuration,
        String range
) {
    public static InstrumentDetails fromInstrument(Instrument instrument) {
        return new InstrumentDetails(
                TextComponentUtil.toMarkup(instrument.description()),
                soundEventId(instrument.soundEvent()),
                ValidationUtil.trimTrailingZeros(instrument.useDuration()),
                ValidationUtil.trimTrailingZeros(instrument.range())
        );
    }

    public static InstrumentDetails fromSpecial(ItemEditorState.SpecialData special) {
        return new InstrumentDetails(
                special.instrumentDescription,
                special.instrumentSoundEventId,
                special.instrumentUseDuration,
                special.instrumentRange
        );
    }

    public void applyTo(ItemEditorState.SpecialData special) {
        special.instrumentDescription = this.description;
        special.instrumentSoundEventId = this.soundEventId;
        special.instrumentUseDuration = this.useDuration;
        special.instrumentRange = this.range;
    }

    public boolean isBlank() {
        return this.description.isBlank()
                && this.soundEventId.isBlank()
                && this.useDuration.isBlank()
                && this.range.isBlank();
    }

    private static String soundEventId(Holder<SoundEvent> soundEvent) {
        return soundEvent.unwrapKey()
                .map(key -> key.identifier().toString())
                .orElseGet(() -> soundEvent.value().location().toString());
    }
}
