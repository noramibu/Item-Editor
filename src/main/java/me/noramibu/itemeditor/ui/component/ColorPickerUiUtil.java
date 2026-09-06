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
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import me.noramibu.itemeditor.util.ItemEditorText;
import me.noramibu.itemeditor.util.ValidationUtil;
import net.minecraft.network.chat.Component;

final class ColorPickerUiUtil {

    static final int INPUT_BORDER_CLIP_PADDING = 2;

    private ColorPickerUiUtil() {}

    static Runnable createSyncRunnable(
            AtomicBoolean syncing,
            IntSupplier rgbSupplier,
            ColorPickerComponent picker,
            BoxComponent swatch,
            LabelComponent swatchLabel,
            TextBoxComponent hexInput,
            Runnable postSync) {
        return createSyncRunnable(syncing, rgbSupplier, picker, swatch, swatchLabel, hexInput, false, postSync);
    }

    static Runnable createSyncRunnable(
            AtomicBoolean syncing,
            IntSupplier colorSupplier,
            ColorPickerComponent picker,
            BoxComponent swatch,
            LabelComponent swatchLabel,
            TextBoxComponent hexInput,
            boolean argb,
            Runnable postSync) {
        return () -> {
            syncing.set(true);
            int color = colorSupplier.getAsInt();
            int rgb = color & 0xFFFFFF;
            picker.selectedColor(argb ? Color.ofArgb(color) : Color.ofRgb(rgb));
            swatch.color(argb ? Color.ofArgb(color) : Color.ofRgb(rgb));
            swatchLabel.text(Component.literal(argb ? ValidationUtil.toArgbHex(color) : ValidationUtil.toHex(rgb))
                    .withColor(rgb));
            hexInput.text(argb ? ValidationUtil.toArgbHex(color) : ValidationUtil.toHex(rgb));
            postSync.run();
            syncing.set(false);
        };
    }

    static Swatch createSwatch(int rgb, int swatchSize) {
        return createSwatch(rgb, swatchSize, false);
    }

    static Swatch createSwatch(int color, int swatchSize, boolean argb) {
        int rgb = color & 0xFFFFFF;
        BoxComponent swatch = UIComponents.box(UiFactory.fixed(swatchSize), UiFactory.fixed(swatchSize))
                .fill(true)
                .color(argb ? Color.ofArgb(color) : Color.ofRgb(rgb));
        LabelComponent swatchLabel = UiFactory.title(argb ? ValidationUtil.toArgbHex(color) : ValidationUtil.toHex(rgb))
                .shadow(false);
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
            TextBoxComponent hexInput, AtomicBoolean syncing, LabelComponent errorLabel, IntConsumer setSelectedRgb) {
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
            LabelComponent errorLabel) {
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
            IntConsumer setSelectedRgb) {
        bindPickerAndChannelInputs(
                picker, syncing, hexInput, null, redInput, greenInput, blueInput, errorLabel, false, setSelectedRgb);
    }

    static void bindPickerAndArgbInputs(
            ColorPickerComponent picker,
            AtomicBoolean syncing,
            TextBoxComponent hexInput,
            TextBoxComponent alphaInput,
            TextBoxComponent redInput,
            TextBoxComponent greenInput,
            TextBoxComponent blueInput,
            LabelComponent errorLabel,
            IntConsumer setSelectedArgb) {
        bindPickerAndChannelInputs(
                picker,
                syncing,
                hexInput,
                alphaInput,
                redInput,
                greenInput,
                blueInput,
                errorLabel,
                true,
                setSelectedArgb);
    }

    private static void bindPickerAndChannelInputs(
            ColorPickerComponent picker,
            AtomicBoolean syncing,
            TextBoxComponent hexInput,
            TextBoxComponent alphaInput,
            TextBoxComponent redInput,
            TextBoxComponent greenInput,
            TextBoxComponent blueInput,
            LabelComponent errorLabel,
            boolean argb,
            IntConsumer setSelectedColor) {
        picker.onChanged().subscribe(color -> {
            if (syncing.get()) return;
            setSelectedColor.accept(argb ? color.argb() : color.rgb());
        });

        if (argb) {
            hexInput.onChanged().subscribe(value -> {
                if (syncing.get()) return;
                Integer parsed = ValidationUtil.tryParseArgbColor(value);
                if (parsed == null) {
                    errorLabel.text(ItemEditorText.tr("dialog.unified_color_picker.argb_error"));
                    return;
                }
                setSelectedColor.accept(parsed);
            });
        } else {
            bindHexInput(hexInput, syncing, errorLabel, setSelectedColor);
        }
        redInput.onChanged()
                .subscribe(value -> updateFromChannels(
                        syncing, errorLabel, alphaInput, redInput, greenInput, blueInput, argb, setSelectedColor));
        greenInput
                .onChanged()
                .subscribe(value -> updateFromChannels(
                        syncing, errorLabel, alphaInput, redInput, greenInput, blueInput, argb, setSelectedColor));
        blueInput
                .onChanged()
                .subscribe(value -> updateFromChannels(
                        syncing, errorLabel, alphaInput, redInput, greenInput, blueInput, argb, setSelectedColor));
        if (argb) {
            alphaInput
                    .onChanged()
                    .subscribe(value -> updateFromChannels(
                            syncing, errorLabel, alphaInput, redInput, greenInput, blueInput, true, setSelectedColor));
        }
    }

    private static void updateFromChannels(
            AtomicBoolean syncing,
            LabelComponent errorLabel,
            TextBoxComponent alphaInput,
            TextBoxComponent redInput,
            TextBoxComponent greenInput,
            TextBoxComponent blueInput,
            boolean argb,
            IntConsumer setSelectedColor) {
        if (syncing.get()) return;
        Integer alpha = argb ? ValidationUtil.tryParseByteChannel(alphaInput.getValue()) : Integer.valueOf(255);
        Integer red = ValidationUtil.tryParseByteChannel(redInput.getValue());
        Integer green = ValidationUtil.tryParseByteChannel(greenInput.getValue());
        Integer blue = ValidationUtil.tryParseByteChannel(blueInput.getValue());
        if (alpha == null || red == null || green == null || blue == null) {
            errorLabel.text(ItemEditorText.tr(
                    argb ? "dialog.unified_color_picker.argb_error" : "dialog.unified_color_picker.rgb_error"));
            return;
        }
        setSelectedColor.accept(
                argb ? (alpha << 24) | (red << 16) | (green << 8) | blue : (red << 16) | (green << 8) | blue);
    }

    static Runnable argbPostSync(
            IntSupplier argbSupplier,
            TextBoxComponent alphaInput,
            TextBoxComponent redInput,
            TextBoxComponent greenInput,
            TextBoxComponent blueInput,
            LabelComponent errorLabel) {
        return () -> {
            int argb = argbSupplier.getAsInt();
            alphaInput.text(Integer.toString((argb >>> 24) & 0xFF));
            redInput.text(Integer.toString((argb >> 16) & 0xFF));
            greenInput.text(Integer.toString((argb >> 8) & 0xFF));
            blueInput.text(Integer.toString(argb & 0xFF));
            errorLabel.text(Component.empty());
        };
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
            Component removeHint) {
        FlowLayout row = UiFactory.row();
        row.horizontalSizing(Sizing.fill(100));

        ButtonComponent applyButton =
                UiFactory.button(applyLabel, UiFactory.ButtonTextPreset.COMPACT, button -> onApply.run());
        if (applyButtonWidth > 0) {
            UiFactory.applyFixedButtonLabel(applyButton, applyLabel, applyButtonWidth);
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
            String label, Component hint, boolean active, Runnable action, int width) {
        ButtonComponent button =
                UiFactory.button(Component.literal(label), UiFactory.ButtonTextPreset.TINY, ignored -> action.run());
        button.active(active);
        button.tooltip(List.of(hint));
        button.horizontalSizing(UiFactory.fixed(width));
        return button;
    }

    record Swatch(BoxComponent swatch, LabelComponent label) {}
}
