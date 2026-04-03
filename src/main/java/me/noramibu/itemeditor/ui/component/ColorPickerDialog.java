package me.noramibu.itemeditor.ui.component;

import io.wispforest.owo.ui.component.BoxComponent;
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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntConsumer;

public final class ColorPickerDialog {

    private static final int DIALOG_WIDTH = 430;

    private ColorPickerDialog() {
    }

    public static FlowLayout create(String title, int initialRgb, IntConsumer onApply, Runnable onCancel) {
        FlowLayout overlay = DialogUiUtil.overlay();
        int dialogWidth = DialogUiUtil.dialogWidth(DIALOG_WIDTH);
        int pickerWidth = Math.max(150, dialogWidth - 48);

        FlowLayout dialog = UiFactory.centeredCard(dialogWidth).gap(8);
        dialog.child(UiFactory.title(title));

        AtomicInteger selectedRgb = new AtomicInteger(initialRgb & 0xFFFFFF);
        AtomicBoolean syncing = new AtomicBoolean(false);

        ColorPickerComponent picker = new ColorPickerComponent()
                .selectedColor(Color.ofRgb(selectedRgb.get()))
                .showAlpha(false)
                .selectorWidth(18)
                .selectorPadding(8);
        picker.sizing(Sizing.fixed(pickerWidth), Sizing.fixed(145));

        BoxComponent swatch = UIComponents.box(Sizing.fixed(36), Sizing.fixed(36))
                .fill(true)
                .color(Color.ofRgb(selectedRgb.get()));
        LabelComponent swatchLabel = UiFactory.title(ValidationUtil.toHex(selectedRgb.get())).shadow(false);
        LabelComponent errorLabel = UiFactory.message("", 0xFF8A8A);

        FlowLayout sampleRow = UiFactory.row();
        sampleRow.child(swatch);
        sampleRow.child(swatchLabel);

        TextBoxComponent hexInput = UiFactory.textBox(ValidationUtil.toHex(selectedRgb.get()), value -> {});
        hexInput.horizontalSizing(Sizing.fixed(90));
        TextBoxComponent redInput = UiFactory.textBox(Integer.toString((selectedRgb.get() >> 16) & 0xFF), value -> {});
        redInput.horizontalSizing(Sizing.fixed(48));
        TextBoxComponent greenInput = UiFactory.textBox(Integer.toString((selectedRgb.get() >> 8) & 0xFF), value -> {});
        greenInput.horizontalSizing(Sizing.fixed(48));
        TextBoxComponent blueInput = UiFactory.textBox(Integer.toString(selectedRgb.get() & 0xFF), value -> {});
        blueInput.horizontalSizing(Sizing.fixed(48));

        Runnable syncInputs = ColorPickerUiUtil.createSyncRunnable(
                syncing,
                selectedRgb::get,
                picker,
                swatch,
                swatchLabel,
                hexInput,
                () -> {
                    int rgb = selectedRgb.get();
                    redInput.text(Integer.toString((rgb >> 16) & 0xFF));
                    greenInput.text(Integer.toString((rgb >> 8) & 0xFF));
                    blueInput.text(Integer.toString(rgb & 0xFF));
                    errorLabel.text(Component.empty());
                }
        );

        IntConsumer setSelectedRgb = rgb -> {
            selectedRgb.set(rgb & 0xFFFFFF);
            syncInputs.run();
        };

        picker.onChanged().subscribe(color -> {
            if (syncing.get()) return;
            setSelectedRgb.accept(color.rgb());
        });

        ColorPickerUiUtil.bindHexInput(hexInput, syncing, errorLabel, setSelectedRgb);

        redInput.onChanged().subscribe(value -> updateFromRgb(syncing, errorLabel, redInput, greenInput, blueInput, setSelectedRgb));
        greenInput.onChanged().subscribe(value -> updateFromRgb(syncing, errorLabel, redInput, greenInput, blueInput, setSelectedRgb));
        blueInput.onChanged().subscribe(value -> updateFromRgb(syncing, errorLabel, redInput, greenInput, blueInput, setSelectedRgb));

        FlowLayout hexRow = UiFactory.row();
        hexRow.child(UiFactory.field(ItemEditorText.tr("common.hex"), Component.empty(), hexInput));
        hexRow.child(sampleRow);

        FlowLayout rgbRow = UiFactory.row();
        rgbRow.child(UiFactory.field(ItemEditorText.tr("common.rgb.red"), Component.empty(), redInput));
        rgbRow.child(UiFactory.field(ItemEditorText.tr("common.rgb.green"), Component.empty(), greenInput));
        rgbRow.child(UiFactory.field(ItemEditorText.tr("common.rgb.blue"), Component.empty(), blueInput));

        dialog.child(picker);
        dialog.child(hexRow);
        dialog.child(rgbRow);
        dialog.child(errorLabel);

        FlowLayout buttonRow = DialogUiUtil.rightAlignedButtonRow();
        buttonRow.child(UiFactory.button(ItemEditorText.tr("common.cancel"), button -> onCancel.run()).horizontalSizing(Sizing.content()));
        buttonRow.child(UiFactory.button(ItemEditorText.tr("dialog.color_picker.apply"), button -> onApply.accept(selectedRgb.get())).horizontalSizing(Sizing.content()));
        dialog.child(buttonRow);

        overlay.child(dialog);
        return overlay;
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
            errorLabel.text(ItemEditorText.tr("dialog.color_picker.rgb_error"));
            return;
        }

        setSelectedRgb.accept((red << 16) | (green << 8) | blue);
    }
}
