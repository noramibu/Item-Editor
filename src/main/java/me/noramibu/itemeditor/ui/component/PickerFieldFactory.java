package me.noramibu.itemeditor.ui.component;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Sizing;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import me.noramibu.itemeditor.ui.panel.specialdata.SpecialDataPanelContext;
import me.noramibu.itemeditor.util.ItemEditorText;
import net.minecraft.network.chat.Component;

public final class PickerFieldFactory {
    private static final int MIN_TEXT_INPUT_WIDTH = 160;

    private PickerFieldFactory() {}

    public static <T> FlowLayout dropdownField(
            SpecialDataPanelContext context,
            Component label,
            Component helpText,
            Component buttonText,
            int buttonWidth,
            List<T> values,
            Function<T, String> labelMapper,
            Consumer<T> onSelected) {
        int effectiveButtonWidth = boundedButtonWidth(context, buttonWidth);
        return UiFactory.pickerField(
                label,
                helpText,
                buttonText,
                effectiveButtonWidth,
                button -> context.openDropdown(button, values, labelMapper, onSelected));
    }

    public static FlowLayout searchableField(
            SpecialDataPanelContext context,
            Component label,
            Component helpText,
            Component buttonText,
            int buttonWidth,
            String pickerTitle,
            String pickerBody,
            List<String> values,
            Function<String, String> labelMapper,
            Consumer<String> onSelected) {
        int effectiveButtonWidth = boundedButtonWidth(context, buttonWidth);
        return UiFactory.pickerField(
                label,
                helpText,
                buttonText,
                effectiveButtonWidth,
                button -> context.openSearchablePicker(pickerTitle, pickerBody, values, labelMapper, onSelected));
    }

    public static FlowLayout searchableTextField(
            SpecialDataPanelContext context,
            Component label,
            String value,
            Consumer<String> setter,
            int pickButtonWidth,
            String pickerTitle,
            String pickerBody,
            List<String> values,
            Function<String, String> labelMapper,
            Consumer<String> onSelected,
            ButtonComponent... extraActions) {
        int availableWidth =
                Math.max(1, context.panelWidthHint() - UiFactory.scaleProfile().padding() * 4);
        int effectiveButtonWidth = Math.clamp(pickButtonWidth, 1, availableWidth);
        int rowGap = Math.max(1, UiFactory.scaleProfile().tightSpacing());
        ButtonComponent pick = UiFactory.pickerButton(
                ItemEditorText.tr("common.pick"),
                pickButtonWidth,
                button -> context.openSearchablePicker(pickerTitle, pickerBody, values, labelMapper, onSelected));
        pick.horizontalSizing(Sizing.fixed(effectiveButtonWidth));
        List<ButtonComponent> actions = Stream.concat(Stream.of(pick), Stream.of(extraActions))
                .filter(Objects::nonNull)
                .toList();
        int actionWidth = actions.stream()
                .mapToInt(action -> action.horizontalSizing().get().value)
                .max()
                .orElse(effectiveButtonWidth);
        actions.forEach(action -> action.horizontalSizing(Sizing.fixed(actionWidth)));
        int actionsWidth = actionWidth * actions.size() + rowGap * (actions.size() - 1);
        boolean stacked = availableWidth < MIN_TEXT_INPUT_WIDTH + actionsWidth + rowGap;
        FlowLayout row = stacked ? UiFactory.column() : UiFactory.row();
        row.child(UiFactory.textBox(value, context.bindText(setter))
                .horizontalSizing(stacked ? Sizing.fill(100) : Sizing.expand(100)));
        if (stacked) {
            actions.forEach(action -> action.horizontalSizing(Sizing.fill(100 / actions.size())));
            row.child(UiFactory.actionButtonRow(false, actions.toArray(ButtonComponent[]::new))
                    .horizontalSizing(Sizing.fill(100)));
        } else {
            actions.forEach(row::child);
        }
        return UiFactory.field(label, Component.empty(), row);
    }

    public static Component selectedOrFallback(String value, Component fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return Component.literal(value);
    }

    private static int boundedButtonWidth(SpecialDataPanelContext context, int requestedButtonWidth) {
        int panelWidth = context.panelWidthHint();
        if (panelWidth < requestedButtonWidth + UiFactory.scaleProfile().padding() * 2) {
            return -1;
        }
        return Math.min(panelWidth, requestedButtonWidth);
    }
}
