package me.noramibu.itemeditor.ui.panel.specialdata;

import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Sizing;
import me.noramibu.itemeditor.ui.component.PickerFieldFactory;
import me.noramibu.itemeditor.ui.component.UiFactory;
import me.noramibu.itemeditor.util.ItemEditorText;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

final class EffectFieldLayoutUtil {

    private EffectFieldLayoutUtil() {
    }

    static FlowLayout buildEffectFields(
            SpecialDataPanelContext context,
            boolean compactLayout,
            List<String> effectIds,
            String effectId,
            Consumer<String> setEffectId,
            String duration,
            Consumer<String> setDuration,
            String amplifier,
            Consumer<String> setAmplifier,
            int pickerWidth,
            int numericWidth
    ) {
        FlowLayout fields = compactLayout ? UiFactory.column() : UiFactory.row();
        fields.child(PickerFieldFactory.searchableField(
                context,
                ItemEditorText.tr("special.potion.effect_id"),
                Component.empty(),
                PickerFieldFactory.selectedOrFallback(effectId, ItemEditorText.tr("special.potion.select_effect")),
                compactLayout ? -1 : pickerWidth,
                ItemEditorText.str("special.potion.effect_id"),
                "",
                effectIds,
                Function.identity(),
                setEffectId
        ));
        fields.child(UiFactory.field(
                ItemEditorText.tr("special.potion.duration"),
                Component.empty(),
                UiFactory.textBox(duration, setDuration).horizontalSizing(
                        compactLayout ? Sizing.fill(100) : UiFactory.fixed(numericWidth)
                )
        ));
        if (amplifier != null && setAmplifier != null) {
            fields.child(UiFactory.field(
                    ItemEditorText.tr("special.potion.amplifier"),
                    Component.empty(),
                    UiFactory.textBox(amplifier, setAmplifier).horizontalSizing(
                            compactLayout ? Sizing.fill(100) : UiFactory.fixed(numericWidth)
                    )
            ));
        }
        return fields;
    }
}
