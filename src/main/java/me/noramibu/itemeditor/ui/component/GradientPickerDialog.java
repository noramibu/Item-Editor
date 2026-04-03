package me.noramibu.itemeditor.ui.component;

import io.wispforest.owo.ui.component.BoxComponent;
import io.wispforest.owo.ui.component.ColorPickerComponent;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.component.TextBoxComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Color;
import io.wispforest.owo.ui.core.Sizing;
import me.noramibu.itemeditor.util.ColorInterpolationUtil;
import me.noramibu.itemeditor.util.ItemEditorText;
import me.noramibu.itemeditor.util.ValidationUtil;
import net.minecraft.network.chat.Component;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

public final class GradientPickerDialog {

    private static final int DIALOG_WIDTH = 520;

    private GradientPickerDialog() {
    }

    public static FlowLayout create(
            String title,
            int initialStartRgb,
            int initialEndRgb,
            BiConsumer<Integer, Integer> onApply,
            Runnable onCancel
    ) {
        FlowLayout overlay = DialogUiUtil.overlay();
        int dialogWidth = DialogUiUtil.dialogWidth(DIALOG_WIDTH);
        boolean compactLayout = dialogWidth < 420;
        int columnWidth = compactLayout ? dialogWidth - 24 : Math.max(140, (dialogWidth - 22) / 2);

        FlowLayout dialog = UiFactory.centeredCard(dialogWidth).gap(8);
        dialog.child(UiFactory.title(title));

        AtomicInteger startRgb = new AtomicInteger(initialStartRgb & 0xFFFFFF);
        AtomicInteger endRgb = new AtomicInteger(initialEndRgb & 0xFFFFFF);
        LabelComponent errorLabel = UiFactory.message("", 0xFF8A8A);

        PickerColumn startColumn = createPickerColumn(ItemEditorText.str("dialog.gradient_picker.start"), startRgb, errorLabel, columnWidth);
        PickerColumn endColumn = createPickerColumn(ItemEditorText.str("dialog.gradient_picker.end"), endRgb, errorLabel, columnWidth);

        FlowLayout pickerRow = compactLayout ? UiFactory.column() : UiFactory.row();
        pickerRow.child(startColumn.layout());
        pickerRow.child(endColumn.layout());
        dialog.child(pickerRow);

        FlowLayout preview = UiFactory.subCard();
        preview.child(UiFactory.title(ItemEditorText.tr("screen.preview")).shadow(false));
        preview.child(buildGradientPreview(startRgb.get(), endRgb.get()));
        dialog.child(preview);

        Runnable refreshPreview = () -> {
            preview.clearChildren();
            preview.child(UiFactory.title(ItemEditorText.tr("screen.preview")).shadow(false));
            preview.child(buildGradientPreview(startRgb.get(), endRgb.get()));
        };

        startColumn.onChanged().accept(refreshPreview);
        endColumn.onChanged().accept(refreshPreview);

        dialog.child(errorLabel);

        FlowLayout buttonRow = DialogUiUtil.rightAlignedButtonRow();
        buttonRow.child(UiFactory.button(ItemEditorText.tr("common.cancel"), button -> onCancel.run()).horizontalSizing(Sizing.content()));
        buttonRow.child(UiFactory.button(ItemEditorText.tr("dialog.gradient_picker.apply"), button -> onApply.accept(startRgb.get(), endRgb.get())).horizontalSizing(Sizing.content()));
        dialog.child(buttonRow);

        overlay.child(dialog);
        return overlay;
    }

    private static PickerColumn createPickerColumn(String title, AtomicInteger colorRef, LabelComponent errorLabel, int columnWidth) {
        FlowLayout column = UiFactory.subCard();
        column.horizontalSizing(Sizing.fixed(columnWidth));
        column.child(UiFactory.title(title).shadow(false));

        AtomicBoolean syncing = new AtomicBoolean(false);

        ColorPickerComponent picker = new ColorPickerComponent()
                .selectedColor(Color.ofRgb(colorRef.get()))
                .showAlpha(false)
                .selectorWidth(14)
                .selectorPadding(6);
        picker.sizing(Sizing.fixed(Math.max(120, columnWidth - 24)), Sizing.fixed(110));

        BoxComponent swatch = Components.box(Sizing.fixed(24), Sizing.fixed(24))
                .fill(true)
                .color(Color.ofRgb(colorRef.get()));
        LabelComponent swatchLabel = UiFactory.title(ValidationUtil.toHex(colorRef.get())).shadow(false);

        TextBoxComponent hexInput = UiFactory.textBox(ValidationUtil.toHex(colorRef.get()), value -> {});
        hexInput.horizontalSizing(Sizing.fixed(90));

        Runnable syncInputs = ColorPickerUiUtil.createSyncRunnable(
                syncing,
                colorRef::get,
                picker,
                swatch,
                swatchLabel,
                hexInput,
                () -> errorLabel.text(Component.empty())
        );

        AtomicReference<Runnable> changeListener = new AtomicReference<>(() -> {});

        IntConsumer setSelectedRgb = rgb -> {
            colorRef.set(rgb & 0xFFFFFF);
            syncInputs.run();
            changeListener.get().run();
        };

        picker.onChanged().subscribe(color -> {
            if (syncing.get()) return;
            setSelectedRgb.accept(color.rgb());
        });

        ColorPickerUiUtil.bindHexInput(hexInput, syncing, errorLabel, setSelectedRgb);

        FlowLayout sampleRow = UiFactory.row();
        sampleRow.child(swatch);
        sampleRow.child(swatchLabel);

        column.child(picker);
        column.child(UiFactory.field(ItemEditorText.tr("common.hex"), Component.empty(), hexInput));
        column.child(sampleRow);
        return new PickerColumn(column, changeListener::set);
    }

    private static FlowLayout buildGradientPreview(int startRgb, int endRgb) {
        FlowLayout row = UiFactory.row();
        String text = ItemEditorText.str("toolbar.gradient");
        int length = text.length();

        for (int index = 0; index < length; index++) {
            float progress = length == 1 ? 0f : (float) index / (length - 1);
            int color = ColorInterpolationUtil.interpolateRgb(startRgb, endRgb, progress);
            row.child(Components.label(Component.literal(Character.toString(text.charAt(index))).withColor(color)));
        }

        return row;
    }

    private record PickerColumn(FlowLayout layout, Consumer<Runnable> onChanged) {
    }
}
