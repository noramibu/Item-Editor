package me.noramibu.itemeditor.ui.component;

import io.wispforest.owo.ui.component.BoxComponent;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.ColorPickerComponent;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.component.TextBoxComponent;
import io.wispforest.owo.ui.component.UIComponents;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Color;
import io.wispforest.owo.ui.core.Sizing;
import me.noramibu.itemeditor.util.ItemEditorText;
import me.noramibu.itemeditor.util.ValidationUtil;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

final class ColorPickerUiUtil {

    static final int INPUT_BORDER_CLIP_PADDING = 2;

    private ColorPickerUiUtil() {
    }

    static Runnable createSyncRunnable(
            AtomicBoolean syncing,
            IntSupplier rgbSupplier,
            ColorPickerComponent picker,
            BoxComponent swatch,
            LabelComponent swatchLabel,
            TextBoxComponent hexInput,
            Runnable postSync
    ) {
        return () -> {
            syncing.set(true);
            int rgb = rgbSupplier.getAsInt() & 0xFFFFFF;
            picker.selectedColor(Color.ofRgb(rgb));
            swatch.color(Color.ofRgb(rgb));
            swatchLabel.text(Component.literal(ValidationUtil.toHex(rgb)).withColor(rgb));
            hexInput.text(ValidationUtil.toHex(rgb));
            postSync.run();
            syncing.set(false);
        };
    }

    static void applyPickerSizing(
            ColorPickerComponent picker,
            int pickerWidth,
            int pickerHeight
    ) {
        picker.sizing(Sizing.fixed(pickerWidth), Sizing.fixed(pickerHeight));
    }

    static Swatch createSwatch(int rgb, int swatchSize) {
        BoxComponent swatch = UIComponents.box(UiFactory.fixed(swatchSize), UiFactory.fixed(swatchSize))
                .fill(true)
                .color(Color.ofRgb(rgb));
        LabelComponent swatchLabel = UiFactory.title(ValidationUtil.toHex(rgb)).shadow(false);
        return new Swatch(swatch, swatchLabel);
    }

    static FlowLayout compactInputField(Component label, TextBoxComponent input, int width) {
        return compactInputFieldPixels(label, input, UiFactory.scaledPixels(width));
    }

    static FlowLayout compactInputFieldPixels(Component label, TextBoxComponent input, int width) {
        int inputWidth = Math.max(1, width);
        int fieldWidth = compactInputFieldOuterWidth(inputWidth);
        FlowLayout field = UiFactory.column();
        field.gap(Math.max(1, UiFactory.scaleProfile().tightSpacing() - 2));
        field.horizontalSizing(Sizing.fixed(fieldWidth));
        field.child(UiFactory.muted(label, Math.max(UiFactory.scaledPixels(36), fieldWidth)));
        input.horizontalSizing(Sizing.fixed(inputWidth));
        field.child(input);
        return field;
    }

    static int compactInputFieldOuterWidth(int inputWidth) {
        return Math.max(1, inputWidth) + UiFactory.scaledPixels(INPUT_BORDER_CLIP_PADDING);
    }

    static void bindHexInput(
            TextBoxComponent hexInput,
            AtomicBoolean syncing,
            LabelComponent errorLabel,
            IntConsumer setSelectedRgb
    ) {
        hexInput.onChanged().subscribe(value -> {
            if (syncing.get()) return;
            Integer parsed = ValidationUtil.tryParseHexColor(value);
            if (parsed == null) {
                errorLabel.text(ItemEditorText.tr("dialog.unified_color_picker.hex_error"));
                return;
            }
            setSelectedRgb.accept(parsed);
        });
    }

    static Runnable rgbPostSync(
            IntSupplier rgbSupplier,
            TextBoxComponent redInput,
            TextBoxComponent greenInput,
            TextBoxComponent blueInput,
            LabelComponent errorLabel
    ) {
        return () -> {
            int rgb = rgbSupplier.getAsInt();
            redInput.text(Integer.toString((rgb >> 16) & 0xFF));
            greenInput.text(Integer.toString((rgb >> 8) & 0xFF));
            blueInput.text(Integer.toString(rgb & 0xFF));
            errorLabel.text(Component.empty());
        };
    }

    static void bindPickerAndRgbInputs(
            ColorPickerComponent picker,
            AtomicBoolean syncing,
            TextBoxComponent hexInput,
            TextBoxComponent redInput,
            TextBoxComponent greenInput,
            TextBoxComponent blueInput,
            LabelComponent errorLabel,
            IntConsumer setSelectedRgb
    ) {
        picker.onChanged().subscribe(color -> {
            if (syncing.get()) return;
            setSelectedRgb.accept(color.rgb());
        });

        bindHexInput(hexInput, syncing, errorLabel, setSelectedRgb);
        redInput.onChanged().subscribe(value -> updateFromRgb(syncing, errorLabel, redInput, greenInput, blueInput, setSelectedRgb));
        greenInput.onChanged().subscribe(value -> updateFromRgb(syncing, errorLabel, redInput, greenInput, blueInput, setSelectedRgb));
        blueInput.onChanged().subscribe(value -> updateFromRgb(syncing, errorLabel, redInput, greenInput, blueInput, setSelectedRgb));
    }

    private static void updateFromRgb(
            AtomicBoolean syncing,
            LabelComponent errorLabel,
            TextBoxComponent redInput,
            TextBoxComponent greenInput,
            TextBoxComponent blueInput,
            IntConsumer setSelectedRgb
    ) {
        if (syncing.get()) return;
        Integer red = ValidationUtil.tryParseByteChannel(redInput.getValue());
        Integer green = ValidationUtil.tryParseByteChannel(greenInput.getValue());
        Integer blue = ValidationUtil.tryParseByteChannel(blueInput.getValue());
        if (red == null || green == null || blue == null) {
            errorLabel.text(ItemEditorText.tr("dialog.unified_color_picker.rgb_error"));
            return;
        }
        setSelectedRgb.accept((red << 16) | (green << 8) | blue);
    }

    static FlowLayout savedPresetRow(
            Component applyLabel,
            Component applyHint,
            Runnable onApply,
            Runnable onEdit,
            Component editHint,
            boolean canMoveUp,
            Runnable onMoveUp,
            boolean canMoveDown,
            Runnable onMoveDown,
            Runnable onRemove,
            int applyButtonWidth,
            int actionButtonWidth,
            Component removeHint
    ) {
        FlowLayout row = UiFactory.row();
        row.horizontalSizing(Sizing.fill(100));

        ButtonComponent applyButton = UiFactory.button(
                applyLabel,
                UiFactory.ButtonTextPreset.COMPACT,
                button -> onApply.run()
        );
        if (applyButtonWidth > 0) {
            ButtonFitUtil.applyFittedFixedLabel(applyButton, applyLabel, applyButtonWidth, 24, 8);
        } else {
            applyButton.horizontalSizing(Sizing.expand(100));
        }
        if (applyHint != null && !applyHint.getString().isBlank()) {
            applyButton.tooltip(List.of(applyHint));
        }
        row.child(applyButton);

        if (onEdit != null) {
            row.child(savedPresetAction("E", editHint, true, onEdit, actionButtonWidth));
        }
        row.child(savedPresetAction("^", ItemEditorText.tr("common.up"), canMoveUp, onMoveUp, actionButtonWidth));
        row.child(savedPresetAction("v", ItemEditorText.tr("common.down"), canMoveDown, onMoveDown, actionButtonWidth));
        row.child(savedPresetAction("x", removeHint, true, onRemove, actionButtonWidth));
        return row;
    }

    private static ButtonComponent savedPresetAction(
            String label,
            Component hint,
            boolean active,
            Runnable action,
            int width
    ) {
        ButtonComponent button = UiFactory.button(Component.literal(label), UiFactory.ButtonTextPreset.TINY, ignored -> action.run());
        button.active(active);
        button.tooltip(List.of(hint));
        button.horizontalSizing(UiFactory.fixed(width));
        return button;
    }

    record Swatch(BoxComponent swatch, LabelComponent label) {
    }
}
