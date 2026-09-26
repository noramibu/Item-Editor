package me.noramibu.itemeditor.ui.component;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Sizing;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import me.noramibu.itemeditor.ui.panel.specialdata.SpecialDataPanelContext;
import me.noramibu.itemeditor.util.IdFieldNormalizer;
import me.noramibu.itemeditor.util.ItemEditorText;
import net.minecraft.network.chat.Component;

public final class PickerFieldFactory {
    private static final int MIN_TEXT_INPUT_WIDTH = 160;

    private PickerFieldFactory() {}

    public static <E extends Enum<E>> FlowLayout enumField(
            SpecialDataPanelContext context, Component label, String current, E[] values, Consumer<String> setter) {
        ButtonComponent selector = UiFactory.button(
                selectedOrFallback(current, ItemEditorText.tr("special.advanced.select")),
                UiFactory.ButtonTextPreset.STANDARD,
                anchor -> context.openClearableDropdown(
                        anchor,
                        ItemEditorText.tr("common.none"),
                        () -> context.mutateRefresh(() -> setter.accept("")),
                        List.of(values),
                        Enum::name,
                        value -> context.mutateRefresh(() -> setter.accept(value.name()))));
        return CompactFieldLayout.selectorRow(label, selector);
    }

    public static <T> FlowLayout dropdownField(
            SpecialDataPanelContext context,
            Component label,
            Component helpText,
            Component buttonText,
            int buttonWidth,
            List<T> values,
            Function<T, String> labelMapper,
            Consumer<T> onSelected) {
        ButtonComponent selector = UiFactory.button(
                buttonText,
                UiFactory.ButtonTextPreset.STANDARD,
                button -> context.openDropdown(button, values, labelMapper, onSelected));
        FlowLayout field = CompactFieldLayout.selectorRow(label, selector, false, buttonWidth);
        if (!helpText.getString().isBlank()) field.child(UiFactory.muted(helpText));
        return field;
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
        return UiFactory.field(
                label,
                helpText,
                UiFactory.pickerButton(
                        buttonText,
                        effectiveButtonWidth,
                        button -> context.openSearchablePicker(
                                pickerTitle, pickerBody, values, labelMapper, onSelected)));
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
        ButtonComponent pick = UiFactory.button(
                ItemEditorText.tr("common.pick"),
                UiFactory.ButtonTextPreset.STANDARD,
                button -> context.openSearchablePicker(pickerTitle, pickerBody, values, labelMapper, onSelected));
        List<ButtonComponent> actions = Stream.concat(Stream.of(pick), Stream.of(extraActions))
                .filter(Objects::nonNull)
                .toList();
        return textFieldWithActions(
                context, label, value, setter, pickButtonWidth, actions.toArray(ButtonComponent[]::new));
    }

    public static FlowLayout textFieldWithActions(
            SpecialDataPanelContext context,
            Component label,
            String value,
            Consumer<String> setter,
            int buttonWidth,
            ButtonComponent... suppliedActions) {
        int availableWidth =
                Math.max(1, context.panelWidthHint() - UiFactory.scaleProfile().padding() * 4);
        int effectiveButtonWidth = Math.clamp(buttonWidth, 1, availableWidth);
        int rowGap = Math.max(1, UiFactory.scaleProfile().tightSpacing());
        List<ButtonComponent> actions =
                Arrays.stream(suppliedActions).filter(Objects::nonNull).toList();
        int actionWidth = Math.max(
                effectiveButtonWidth,
                actions.stream()
                        .mapToInt(action -> action.horizontalSizing().get().value)
                        .max()
                        .orElse(effectiveButtonWidth));
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

    public static ButtonComponent clearableIdButton(
            SpecialDataPanelContext context, String current, List<String> entries, Consumer<String> setter) {
        return clearableValueButton(
                context,
                current,
                entries,
                Function.identity(),
                Function.identity(),
                IdFieldNormalizer::normalize,
                setter,
                UiFactory.ButtonTextPreset.STANDARD);
    }

    public static <T> ButtonComponent clearableValueButton(
            SpecialDataPanelContext context,
            String current,
            List<T> entries,
            Function<T, String> value,
            Function<T, String> label,
            UnaryOperator<String> normalize,
            Consumer<String> setter,
            UiFactory.ButtonTextPreset preset) {
        Component selected = current.isBlank()
                ? ItemEditorText.tr("common.unset")
                : Component.literal(entries.stream()
                        .filter(entry -> value.apply(entry).equals(current))
                        .map(label)
                        .findFirst()
                        .orElse(current));
        return UiFactory.button(
                selected.copy().withColor(current.isBlank() ? 0xAAAAAA : 0x55FFFF),
                preset,
                anchor -> context.openClearableDropdown(
                        anchor,
                        ItemEditorText.tr("common.unset"),
                        () -> context.mutateRefresh(() -> setter.accept("")),
                        entries,
                        label,
                        entry -> context.mutateRefresh(() -> setter.accept(normalize.apply(value.apply(entry))))));
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
