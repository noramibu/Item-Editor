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
import me.noramibu.itemeditor.util.ColorInterpolationUtil;
import me.noramibu.itemeditor.util.ItemEditorText;
import me.noramibu.itemeditor.util.TextColorPresets;
import me.noramibu.itemeditor.util.ValidationUtil;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

public final class GradientPickerDialog {

    private static final int DIALOG_WIDTH = 520;
    private static final int DIALOG_GAP = 8;
    private static final int CONTENT_GAP = 8;
    private static final int COMPACT_LAYOUT_WIDTH_THRESHOLD = 420;
    private static final int COMPACT_BUTTON_WIDTH_THRESHOLD = 360;
    private static final int COMPACT_BUTTON_ROWS = 2;
    private static final int BUTTON_RESERVE_COMPACT_EXTRA = 22;
    private static final int BUTTON_RESERVE_NORMAL_EXTRA = 16;
    private static final int HEADER_RESERVE = 54;
    private static final int CONTENT_MIN_HEIGHT_COMPACT = 170;
    private static final int CONTENT_MIN_HEIGHT_NORMAL = 150;
    private static final int CONTENT_MAX_HEIGHT = 280;
    private static final int DIALOG_MIN_HEIGHT_COMPACT = 250;
    private static final int DIALOG_MIN_HEIGHT_NORMAL = 230;
    private static final int PICKER_HEIGHT_FLOOR = 84;
    private static final int PICKER_HEIGHT_MAX_COMPACT = 110;
    private static final int PICKER_HEIGHT_MAX_NORMAL = 124;
    private static final int PICKER_HEIGHT_RESERVE_COMPACT = 92;
    private static final int PICKER_HEIGHT_RESERVE_NORMAL = 86;
    private static final int COMPACT_COLUMN_MIN_WIDTH = 170;
    private static final int COMPACT_COLUMN_RESERVE = 40;
    private static final int NORMAL_COLUMN_MIN_WIDTH = 132;
    private static final int NORMAL_COLUMN_RESERVE = 36;
    private static final int NORMAL_COLUMN_DIVISOR = 2;
    private static final int PICKER_WIDTH_MIN = 120;
    private static final int PICKER_WIDTH_RESERVE = 24;
    private static final int PICKER_SELECTOR_WIDTH = 14;
    private static final int PICKER_SELECTOR_PADDING = 6;
    private static final int HEX_INPUT_WIDTH = 90;
    private static final int RGB_INPUT_WIDTH = 48;
    private static final int SWATCH_SIZE = 24;
    private static final int FOOTER_BUTTON_MIN_WIDTH = 72;
    private static final int FOOTER_BUTTON_MAX_WIDTH = 150;
    private static final int FOOTER_BUTTON_DIVISOR = 4;
    private static final int FOOTER_BUTTON_TEXT_RESERVE = 10;
    private static final int FOOTER_BUTTON_TEXT_MIN_WIDTH = 24;
    private static final int SAVE_ACTION_BUTTON_MIN_WIDTH = 112;
    private static final int SAVED_REMOVE_BUTTON_WIDTH = 58;

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
        boolean compactButtons = DialogUiUtil.compactButtons(dialogWidth, COMPACT_BUTTON_WIDTH_THRESHOLD);
        boolean compactLayout = dialogWidth < COMPACT_LAYOUT_WIDTH_THRESHOLD || compactButtons;
        int buttonRowReserve = DialogUiUtil.buttonRowReserve(
                compactButtons,
                COMPACT_BUTTON_ROWS,
                BUTTON_RESERVE_COMPACT_EXTRA,
                BUTTON_RESERVE_NORMAL_EXTRA
        );
        int headerReserve = UiFactory.scaledPixels(HEADER_RESERVE);
        int contentHeight = DialogUiUtil.availableDialogContentHeight(
                headerReserve + buttonRowReserve,
                compactLayout ? CONTENT_MIN_HEIGHT_COMPACT : CONTENT_MIN_HEIGHT_NORMAL
        );
        contentHeight = Math.min(contentHeight, CONTENT_MAX_HEIGHT);
        int dialogHeight = DialogUiUtil.dialogHeight(
                headerReserve + buttonRowReserve + contentHeight,
                compactLayout ? DIALOG_MIN_HEIGHT_COMPACT : DIALOG_MIN_HEIGHT_NORMAL
        );
        int pickerHeight = Math.max(
                PICKER_HEIGHT_FLOOR,
                Math.min(
                        compactLayout ? PICKER_HEIGHT_MAX_COMPACT : PICKER_HEIGHT_MAX_NORMAL,
                        contentHeight - UiFactory.scaledPixels(compactLayout ? PICKER_HEIGHT_RESERVE_COMPACT : PICKER_HEIGHT_RESERVE_NORMAL)
                )
        );
        int preferredColumnWidth = compactLayout
                ? Math.max(COMPACT_COLUMN_MIN_WIDTH, dialogWidth - UiFactory.scaledPixels(COMPACT_COLUMN_RESERVE))
                : Math.max(NORMAL_COLUMN_MIN_WIDTH, (dialogWidth - UiFactory.scaledPixels(NORMAL_COLUMN_RESERVE)) / NORMAL_COLUMN_DIVISOR);
        int columnWidth = Math.max(1, Math.min(dialogWidth, preferredColumnWidth));

        FlowLayout dialog = DialogUiUtil.dialogCard(dialogWidth, dialogHeight, DIALOG_GAP);
        dialog.child(UiFactory.title(title));
        FlowLayout content = UiFactory.column().gap(CONTENT_GAP);

        AtomicInteger startRgb = new AtomicInteger(initialStartRgb & 0xFFFFFF);
        AtomicInteger endRgb = new AtomicInteger(initialEndRgb & 0xFFFFFF);
        LabelComponent errorLabel = UiFactory.message("", 0xFF8A8A);

        PickerColumn startColumn = createPickerColumn(
                ItemEditorText.str("dialog.gradient_picker.start"),
                startRgb,
                errorLabel,
                columnWidth,
                compactLayout,
                pickerHeight
        );
        PickerColumn endColumn = createPickerColumn(
                ItemEditorText.str("dialog.gradient_picker.end"),
                endRgb,
                errorLabel,
                columnWidth,
                compactLayout,
                pickerHeight
        );

        FlowLayout pickerRow = compactLayout ? UiFactory.column() : UiFactory.row();
        pickerRow.child(startColumn.layout());
        pickerRow.child(endColumn.layout());
        content.child(pickerRow);

        FlowLayout preview = UiFactory.subCard();
        preview.child(UiFactory.title(ItemEditorText.tr("screen.preview")).shadow(false));
        preview.child(buildGradientPreview(startRgb.get(), endRgb.get()));
        content.child(preview);

        Runnable refreshPreview = () -> {
            preview.clearChildren();
            preview.child(UiFactory.title(ItemEditorText.tr("screen.preview")).shadow(false));
            preview.child(buildGradientPreview(startRgb.get(), endRgb.get()));
        };

        startColumn.onChanged().accept(refreshPreview);
        endColumn.onChanged().accept(refreshPreview);

        FlowLayout savedCard = UiFactory.subCard();
        savedCard.child(UiFactory.title(ItemEditorText.tr("dialog.gradient_picker.saved_title")).shadow(false));
        FlowLayout savedList = UiFactory.column().gap(Math.max(1, UiFactory.scaleProfile().tightSpacing() - 1));
        savedCard.child(savedList);
        AtomicReference<Runnable> refreshSaved = new AtomicReference<>(() -> {});

        FlowLayout presetActions = UiFactory.row();
        ButtonComponent saveCurrentButton = UiFactory.button(
                ItemEditorText.tr("dialog.gradient_picker.save_current"),
                UiFactory.ButtonTextPreset.COMPACT,
                button -> {
                    TextColorPresets.saveGradientPreset(startRgb.get(), endRgb.get());
                    refreshSaved.get().run();
                }
        );
        saveCurrentButton.horizontalSizing(compactButtons ? Sizing.fill(100) : UiFactory.fixed(Math.max(SAVE_ACTION_BUTTON_MIN_WIDTH, dialogWidth / 4)));
        presetActions.child(saveCurrentButton);

        refreshSaved.set(() -> rebuildSavedGradients(savedList, compactLayout, startRgb, endRgb, refreshPreview, onApply, refreshSaved.get()));
        refreshSaved.get().run();
        content.child(presetActions);
        content.child(savedCard);

        content.child(errorLabel);
        dialog.child(DialogUiUtil.scrollCard(content, contentHeight));

        FlowLayout buttonRow = compactButtons ? UiFactory.column() : DialogUiUtil.rightAlignedButtonRow();
        ButtonComponent cancelButton = DialogUiUtil.footerButtonByDivisor(
                ItemEditorText.tr("common.cancel"),
                dialogWidth,
                compactButtons,
                FOOTER_BUTTON_MIN_WIDTH,
                FOOTER_BUTTON_MAX_WIDTH,
                FOOTER_BUTTON_DIVISOR,
                FOOTER_BUTTON_TEXT_MIN_WIDTH,
                FOOTER_BUTTON_TEXT_RESERVE,
                button -> onCancel.run()
        );
        ButtonComponent applyButton = DialogUiUtil.footerButtonByDivisor(
                ItemEditorText.tr("dialog.gradient_picker.apply"),
                dialogWidth,
                compactButtons,
                FOOTER_BUTTON_MIN_WIDTH,
                FOOTER_BUTTON_MAX_WIDTH,
                FOOTER_BUTTON_DIVISOR,
                FOOTER_BUTTON_TEXT_MIN_WIDTH,
                FOOTER_BUTTON_TEXT_RESERVE,
                button -> onApply.accept(startRgb.get(), endRgb.get())
        );
        buttonRow.child(cancelButton);
        buttonRow.child(applyButton);
        dialog.child(buttonRow);

        overlay.child(dialog);
        return overlay;
    }

    private static void rebuildSavedGradients(
            FlowLayout savedList,
            boolean compactLayout,
            AtomicInteger startRgb,
            AtomicInteger endRgb,
            Runnable onApplied,
            BiConsumer<Integer, Integer> onApply,
            Runnable onChanged
    ) {
        savedList.clearChildren();
        List<TextColorPresets.CustomGradientPreset> presets = TextColorPresets.customGradientPresets();
        if (presets.isEmpty()) {
            savedList.child(UiFactory.muted(ItemEditorText.tr("dialog.gradient_picker.saved_none")));
            return;
        }

        for (TextColorPresets.CustomGradientPreset preset : presets) {
            FlowLayout row = UiFactory.row();
            row.horizontalSizing(Sizing.fill(100));
            ButtonComponent applyButton = UiFactory.button(
                    TextColorPresets.gradientLabel(preset.name(), preset.startRgb(), preset.endRgb()),
                    UiFactory.ButtonTextPreset.COMPACT,
                    button -> {
                        startRgb.set(preset.startRgb());
                        endRgb.set(preset.endRgb());
                        onApplied.run();
                        onApply.accept(preset.startRgb(), preset.endRgb());
                    }
            );
            applyButton.horizontalSizing(Sizing.expand(100));
            row.child(applyButton);

            ButtonComponent removeButton = UiFactory.button(
                    ItemEditorText.tr("common.remove"),
                    UiFactory.ButtonTextPreset.COMPACT,
                    button -> {
                        TextColorPresets.removeGradientPreset(preset.id());
                        onChanged.run();
                    }
            );
            removeButton.tooltip(List.of(ItemEditorText.tr("dialog.gradient_picker.saved_remove_hint")));
            removeButton.horizontalSizing(UiFactory.fixed(SAVED_REMOVE_BUTTON_WIDTH));
            row.child(removeButton);
            savedList.child(row);
        }
    }

    private static PickerColumn createPickerColumn(
            String title,
            AtomicInteger colorRef,
            LabelComponent errorLabel,
            int columnWidth,
            boolean compactLayout,
            int pickerHeight
    ) {
        FlowLayout column = UiFactory.subCard();
        column.horizontalSizing(compactLayout ? Sizing.fill(100) : UiFactory.fixed(columnWidth));
        column.child(UiFactory.title(title).shadow(false));

        AtomicBoolean syncing = new AtomicBoolean(false);

        ColorPickerComponent picker = new ColorPickerComponent()
                .selectedColor(Color.ofRgb(colorRef.get()))
                .showAlpha(false)
                .selectorWidth(PICKER_SELECTOR_WIDTH)
                .selectorPadding(PICKER_SELECTOR_PADDING);
        int preferredPickerWidth = Math.max(PICKER_WIDTH_MIN, columnWidth - UiFactory.scaledPixels(PICKER_WIDTH_RESERVE));
        int pickerWidth = Math.max(1, Math.min(columnWidth, preferredPickerWidth));
        picker.sizing(compactLayout ? Sizing.fill(100) : UiFactory.fixed(pickerWidth), UiFactory.fixed(pickerHeight));

        BoxComponent swatch = UIComponents.box(UiFactory.fixed(SWATCH_SIZE), UiFactory.fixed(SWATCH_SIZE))
                .fill(true)
                .color(Color.ofRgb(colorRef.get()));
        LabelComponent swatchLabel = UiFactory.title(ValidationUtil.toHex(colorRef.get())).shadow(false);

        TextBoxComponent hexInput = UiFactory.textBox(ValidationUtil.toHex(colorRef.get()), value -> {});
        hexInput.horizontalSizing(UiFactory.fixed(HEX_INPUT_WIDTH));
        TextBoxComponent redInput = UiFactory.textBox(Integer.toString((colorRef.get() >> 16) & 0xFF), value -> {});
        redInput.horizontalSizing(UiFactory.fixed(RGB_INPUT_WIDTH));
        TextBoxComponent greenInput = UiFactory.textBox(Integer.toString((colorRef.get() >> 8) & 0xFF), value -> {});
        greenInput.horizontalSizing(UiFactory.fixed(RGB_INPUT_WIDTH));
        TextBoxComponent blueInput = UiFactory.textBox(Integer.toString(colorRef.get() & 0xFF), value -> {});
        blueInput.horizontalSizing(UiFactory.fixed(RGB_INPUT_WIDTH));

        Runnable syncInputs = ColorPickerUiUtil.createSyncRunnable(
                syncing,
                colorRef::get,
                picker,
                swatch,
                swatchLabel,
                hexInput,
                () -> {
                    int rgb = colorRef.get();
                    redInput.text(Integer.toString((rgb >> 16) & 0xFF));
                    greenInput.text(Integer.toString((rgb >> 8) & 0xFF));
                    blueInput.text(Integer.toString(rgb & 0xFF));
                    errorLabel.text(Component.empty());
                }
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
        redInput.onChanged().subscribe(value -> updateFromRgb(syncing, errorLabel, redInput, greenInput, blueInput, setSelectedRgb));
        greenInput.onChanged().subscribe(value -> updateFromRgb(syncing, errorLabel, redInput, greenInput, blueInput, setSelectedRgb));
        blueInput.onChanged().subscribe(value -> updateFromRgb(syncing, errorLabel, redInput, greenInput, blueInput, setSelectedRgb));

        FlowLayout sampleRow = UiFactory.row();
        sampleRow.child(swatch);
        sampleRow.child(swatchLabel);
        FlowLayout hexRow = UiFactory.row();
        hexRow.child(compactInputField(ItemEditorText.tr("common.hex"), hexInput, HEX_INPUT_WIDTH));
        hexRow.child(sampleRow);
        FlowLayout rgbRow = UiFactory.row();
        rgbRow.child(compactInputField(ItemEditorText.tr("common.rgb.red"), redInput, RGB_INPUT_WIDTH));
        rgbRow.child(compactInputField(ItemEditorText.tr("common.rgb.green"), greenInput, RGB_INPUT_WIDTH));
        rgbRow.child(compactInputField(ItemEditorText.tr("common.rgb.blue"), blueInput, RGB_INPUT_WIDTH));

        column.child(picker);
        column.child(hexRow);
        column.child(rgbRow);
        return new PickerColumn(column, changeListener::set);
    }

    private static FlowLayout compactInputField(Component label, TextBoxComponent input, int width) {
        FlowLayout field = UiFactory.column();
        field.gap(Math.max(1, UiFactory.scaleProfile().tightSpacing() - 2));
        field.horizontalSizing(UiFactory.fixed(width));
        field.child(UiFactory.muted(label, width));
        input.horizontalSizing(UiFactory.fixed(width));
        field.child(input);
        return field;
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

    private static FlowLayout buildGradientPreview(int startRgb, int endRgb) {
        FlowLayout row = UiFactory.row();
        String text = ItemEditorText.str("toolbar.gradient");
        int codePointCount = text.codePointCount(0, text.length());
        int colorIndex = 0;

        for (int index = 0; index < text.length();) {
            int codePoint = text.codePointAt(index);
            float progress = codePointCount == 1 ? 0f : (float) colorIndex / (codePointCount - 1);
            int color = ColorInterpolationUtil.interpolateRgb(startRgb, endRgb, progress);
            row.child(UiFactory.bodyLabel(Component.literal(Character.toString(codePoint)).withColor(color)));
            colorIndex++;
            index += Character.charCount(codePoint);
        }

        return row;
    }

    private record PickerColumn(FlowLayout layout, Consumer<Runnable> onChanged) {
    }
}
