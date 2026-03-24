package me.noramibu.itemeditor.ui.component;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Sizing;
import me.noramibu.itemeditor.util.ItemEditorText;
import me.noramibu.itemeditor.util.ValidationUtil;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

public final class ColorTokenListEditor {

    private ColorTokenListEditor() {
    }

    public static FlowLayout buildField(
            Component label,
            Component helpText,
            Supplier<String> currentValueSupplier,
            Consumer<String> setter,
            int fallbackColor,
            Consumer<Runnable> mutateRefresh,
            BiConsumer<Integer, IntConsumer> openColorPicker,
            Function<Integer, Component> chipTooltip
    ) {
        FlowLayout field = UiFactory.field(label, helpText, UiFactory.column());
        FlowLayout content = (FlowLayout) field.children().getLast();
        content.clearChildren();

        String currentRaw = currentValueSupplier.get();
        FlowLayout inputRow = UiFactory.row();
        inputRow.child(UiFactory.textBox(currentRaw, value -> mutateRefresh.accept(() -> setter.accept(value)))
                .horizontalSizing(Sizing.fixed(220)));

        int selectedColor = firstColorOrDefault(currentRaw, fallbackColor);
        ButtonComponent pickButton = UiFactory.button(
                Component.literal(ItemEditorText.str("common.pick")).withColor(selectedColor),
                button -> openColorPicker.accept(
                        firstColorOrDefault(currentValueSupplier.get(), fallbackColor),
                        color -> mutateRefresh.accept(() -> setter.accept(appendColor(currentValueSupplier.get(), ValidationUtil.toHex(color))))
                )
        );
        pickButton.horizontalSizing(Sizing.content());
        inputRow.child(pickButton);

        ButtonComponent removeButton = UiFactory.button(
                ItemEditorText.tr("common.remove"),
                button -> mutateRefresh.accept(() -> setter.accept(removeLastColor(currentValueSupplier.get())))
        );
        removeButton.horizontalSizing(Sizing.content());
        inputRow.child(removeButton);

        content.child(inputRow);

        List<String> colorTokens = splitColorTokens(currentRaw);
        if (!colorTokens.isEmpty()) {
            FlowLayout chips = UiFactory.column();
            chips.gap(2);
            for (int index = 0; index < colorTokens.size(); index++) {
                int currentIndex = index;
                String token = colorTokens.get(index);
                Integer parsed = ValidationUtil.tryParseHexColor(token);
                int displayColor = parsed == null ? fallbackColor : parsed;

                FlowLayout chipRow = UiFactory.row();
                ButtonComponent handle = UiFactory.button(ItemEditorText.tr("special.firework.color_handle"), button -> {});
                handle.horizontalSizing(Sizing.content());
                handle.active(false);
                chipRow.child(handle);

                ButtonComponent moveLeft = UiFactory.button(ItemEditorText.tr("special.firework.color_move_left"), button ->
                        mutateRefresh.accept(() -> setter.accept(moveColorToken(currentValueSupplier.get(), currentIndex, currentIndex - 1)))
                );
                moveLeft.horizontalSizing(Sizing.content());
                moveLeft.active(currentIndex > 0);
                chipRow.child(moveLeft);

                ButtonComponent moveRight = UiFactory.button(ItemEditorText.tr("special.firework.color_move_right"), button ->
                        mutateRefresh.accept(() -> setter.accept(moveColorToken(currentValueSupplier.get(), currentIndex, currentIndex + 1)))
                );
                moveRight.horizontalSizing(Sizing.content());
                moveRight.active(currentIndex < colorTokens.size() - 1);
                chipRow.child(moveRight);

                ButtonComponent chip = UiFactory.button(
                        Component.literal(ValidationUtil.toHex(displayColor)).withColor(displayColor),
                        button -> openColorPicker.accept(
                                displayColor,
                                color -> mutateRefresh.accept(() -> setter.accept(replaceColorAt(currentValueSupplier.get(), currentIndex, ValidationUtil.toHex(color))))
                        )
                );
                chip.horizontalSizing(Sizing.content());
                chip.tooltip(List.of(chipTooltip.apply(currentIndex)));
                chipRow.child(chip);

                ButtonComponent removeChip = UiFactory.button(ItemEditorText.tr("common.remove"), button ->
                        mutateRefresh.accept(() -> setter.accept(removeColorAt(currentValueSupplier.get(), currentIndex)))
                );
                removeChip.horizontalSizing(Sizing.content());
                chipRow.child(removeChip);
                chips.child(chipRow);
            }
            content.child(chips);
        }

        return field;
    }

    public static int firstColorOrDefault(String raw, int fallbackColor) {
        List<Integer> colors = parseColors(raw);
        return colors.isEmpty() ? fallbackColor : colors.getFirst();
    }

    public static List<Integer> parseColors(String raw) {
        List<Integer> colors = new ArrayList<>();
        for (String token : splitColorTokens(raw)) {
            Integer parsed = ValidationUtil.tryParseHexColor(token);
            if (parsed != null) {
                colors.add(parsed);
            }
        }
        return colors;
    }

    public static String appendColor(String raw, String color) {
        List<String> tokens = splitColorTokens(raw);
        tokens.add(color);
        return String.join(", ", tokens);
    }

    public static String removeLastColor(String raw) {
        List<String> tokens = splitColorTokens(raw);
        if (!tokens.isEmpty()) {
            tokens.removeLast();
        }
        return String.join(", ", tokens);
    }

    public static String removeColorAt(String raw, int index) {
        List<String> tokens = splitColorTokens(raw);
        if (index >= 0 && index < tokens.size()) {
            tokens.remove(index);
        }
        return String.join(", ", tokens);
    }

    public static String replaceColorAt(String raw, int index, String replacement) {
        List<String> tokens = splitColorTokens(raw);
        if (index >= 0 && index < tokens.size()) {
            tokens.set(index, replacement);
        }
        return String.join(", ", tokens);
    }

    public static String moveColorToken(String raw, int fromIndex, int toIndex) {
        List<String> tokens = splitColorTokens(raw);
        if (fromIndex < 0 || fromIndex >= tokens.size() || toIndex < 0 || toIndex >= tokens.size()) {
            return String.join(", ", tokens);
        }
        String moved = tokens.remove(fromIndex);
        tokens.add(toIndex, moved);
        return String.join(", ", tokens);
    }

    public static List<String> splitColorTokens(String raw) {
        List<String> tokens = new ArrayList<>();
        if (raw == null || raw.isBlank()) {
            return tokens;
        }

        for (String part : raw.split(",")) {
            String token = part.trim();
            if (!token.isEmpty()) {
                tokens.add(token);
            }
        }
        return tokens;
    }
}
