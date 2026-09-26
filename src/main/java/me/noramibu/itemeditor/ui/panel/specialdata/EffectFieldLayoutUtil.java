package me.noramibu.itemeditor.ui.panel.specialdata;

import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Sizing;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import me.noramibu.itemeditor.ui.component.PickerFieldFactory;
import me.noramibu.itemeditor.ui.component.UiFactory;
import me.noramibu.itemeditor.util.ItemEditorText;
import net.minecraft.network.chat.Component;

final class EffectFieldLayoutUtil {

    private EffectFieldLayoutUtil() {}

    static FlowLayout buildEffectFields(
            SpecialDataPanelContext context,
            List<String> effectIds,
            String effectId,
            Consumer<String> setEffectId,
            String duration,
            Consumer<String> setDuration,
            String amplifier,
            Consumer<String> setAmplifier) {
        FlowLayout fields = UiFactory.column();
        fields.child(PickerFieldFactory.searchableField(
                context,
                Field.EFFECT_ID.text(),
                Component.empty(),
                PickerFieldFactory.selectedOrFallback(effectId, ItemEditorText.tr("special.potion.select_effect")),
                -1,
                Field.EFFECT_ID.text().getString(),
                "",
                effectIds,
                Function.identity(),
                setEffectId));
        fields.child(numberField(Field.DURATION.text(), duration, setDuration));
        if (amplifier != null && setAmplifier != null) {
            fields.child(numberField(Field.AMPLIFIER.text(), amplifier, setAmplifier));
        }
        return fields;
    }

    static FlowLayout numberField(Component label, String value, Consumer<String> setter) {
        return UiFactory.field(
                label, Component.empty(), UiFactory.textBox(value, setter).horizontalSizing(Sizing.fill(100)));
    }

    enum Field implements SpecialDataSearch.Field {
        EFFECT_ID("special.potion.effect_id"),
        DURATION("special.potion.duration"),
        AMPLIFIER("special.potion.amplifier");

        private final String key;

        Field(String key) {
            this.key = key;
        }

        public String key() {
            return key;
        }
    }
}
