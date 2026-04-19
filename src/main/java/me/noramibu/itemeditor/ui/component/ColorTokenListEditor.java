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
    private static final double COMPACT_LAYOUT_SCALE_THRESHOLD = 1.35d;
    private static final int INPUT_FIELD_WIDTH = 220;
    private static final int PICK_BUTTON_WIDTH = 92;
    private static final int REMOVE_BUTTON_WIDTH = 84;
    private static final int HANDLE_BUTTON_WIDTH = 58;
    private static final int MOVE_BUTTON_WIDTH = 56;
    private static final int CHIP_BUTTON_WIDTH = 94;
    private static final int REMOVE_CHIP_BUTTON_WIDTH = 84;
    private static final int MIN_BUTTON_WIDTH = 42;
    private static final int MAX_BUTTON_WIDTH = 180;

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
        boolean compactLayout = UiFactory.scaleProfile().scale() >= COMPACT_LAYOUT_SCALE_THRESHOLD;
        FlowLayout field = UiFactory.field(label, helpText, UiFactory.column());
        FlowLayout content = (FlowLayout) field.children().getLast();
        content.clearChildren();

        String currentRaw = currentValueSupplier.get();
        FlowLayout inputRow = compactLayout ? UiFactory.column() : UiFactory.row();
        inputRow.child(UiFactory.textBox(currentRaw, value -> mutateRefresh.accept(() -> setter.accept(value)))
                    .horizontalSizing(compactLayout ? Sizing.fill(100) : UiFactory.fixed(INPUT_FIELD_WIDTH)));

        int selectedColor = firstColorOrDefault(currentRaw, fallbackColor);
        ButtonComponent pickButton = UiFactory.button(
                Component.literal(ItemEditorText.str("common.pick")).withColor(selectedColor), UiFactory.ButtonTextPreset.STANDARD, 
                button -> openColorPicker.accept(
                        firstColorOrDefault(currentValueSupplier.get(), fallbackColor),
                        color -> mutateRefresh.accept(() -> setter.accept(appendColor(currentValueSupplier.get(), ValidationUtil.toHex(color))))
                )
        );
        pickButton.horizontalSizing(compactLayout ? Sizing.fill(100) : resolveBoundedButtonSizing(PICK_BUTTON_WIDTH));
        inputRow.child(pickButton);

        ButtonComponent removeButton = UiFactory.button(
                ItemEditorText.tr("common.remove"), UiFactory.ButtonTextPreset.STANDARD, 
                button -> mutateRefresh.accept(() -> setter.accept(removeLastColor(currentValueSupplier.get())))
        );
        removeButton.horizontalSizing(compactLayout ? Sizing.fill(100) : resolveBoundedButtonSizing(REMOVE_BUTTON_WIDTH));
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

                FlowLayout chipRow = compactLayout ? UiFactory.column() : UiFactory.row();
                ButtonComponent handle = UiFactory.button(ItemEditorText.tr("special.firework.color_handle"), UiFactory.ButtonTextPreset.STANDARD,  button -> {});
                handle.horizontalSizing(compactLayout ? Sizing.fill(100) : resolveBoundedButtonSizing(HANDLE_BUTTON_WIDTH));
                handle.active(false);
                chipRow.child(handle);

                ButtonComponent moveLeft = UiFactory.button(ItemEditorText.tr("special.firework.color_move_left"), UiFactory.ButtonTextPreset.STANDARD,  button ->
                        mutateRefresh.accept(() -> setter.accept(moveColorToken(currentValueSupplier.get(), currentIndex, currentIndex - 1)))
                );
                moveLeft.horizontalSizing(compactLayout ? Sizing.fill(100) : resolveBoundedButtonSizing(MOVE_BUTTON_WIDTH));
                moveLeft.active(currentIndex > 0);
                chipRow.child(moveLeft);

                ButtonComponent moveRight = UiFactory.button(ItemEditorText.tr("special.firework.color_move_right"), UiFactory.ButtonTextPreset.STANDARD,  button ->
                        mutateRefresh.accept(() -> setter.accept(moveColorToken(currentValueSupplier.get(), currentIndex, currentIndex + 1)))
                );
                moveRight.horizontalSizing(compactLayout ? Sizing.fill(100) : resolveBoundedButtonSizing(MOVE_BUTTON_WIDTH));
                moveRight.active(currentIndex < colorTokens.size() - 1);
                chipRow.child(moveRight);

                ButtonComponent chip = UiFactory.button(
                        Component.literal(ValidationUtil.toHex(displayColor)).withColor(displayColor), UiFactory.ButtonTextPreset.STANDARD, 
                        button -> openColorPicker.accept(
                                displayColor,
                                color -> mutateRefresh.accept(() -> setter.accept(replaceColorAt(currentValueSupplier.get(), currentIndex, ValidationUtil.toHex(color))))
                        )
                );
                chip.horizontalSizing(compactLayout ? Sizing.fill(100) : resolveBoundedButtonSizing(CHIP_BUTTON_WIDTH));
                chip.tooltip(List.of(chipTooltip.apply(currentIndex)));
                chipRow.child(chip);

                ButtonComponent removeChip = UiFactory.button(ItemEditorText.tr("common.remove"), UiFactory.ButtonTextPreset.STANDARD,  button ->
                        mutateRefresh.accept(() -> setter.accept(removeColorAt(currentValueSupplier.get(), currentIndex)))
                );
                removeChip.horizontalSizing(compactLayout ? Sizing.fill(100) : resolveBoundedButtonSizing(REMOVE_CHIP_BUTTON_WIDTH));
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

    private static Sizing resolveBoundedButtonSizing(int baseWidth) {
        int scaled = UiFactory.scaledPixels(baseWidth);
        return Sizing.fixed(Math.max(MIN_BUTTON_WIDTH, Math.min(MAX_BUTTON_WIDTH, scaled)));
    }
}
