package me.noramibu.itemeditor.ui.component;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.ColorPickerComponent;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.component.TextBoxComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Color;
import io.wispforest.owo.ui.core.HorizontalAlignment;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.Sizing;
import me.noramibu.itemeditor.util.ItemEditorText;
import me.noramibu.itemeditor.util.TextColorPresets;
import me.noramibu.itemeditor.util.ValidationUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.IntConsumer;

public final class ColorPickerDialog {

    private static final int DIALOG_WIDTH = 430;
    private static final int DIALOG_FULL_WIDTH_THRESHOLD = 520;
    private static final int DIALOG_GAP = 8;
    private static final int CONTENT_GAP = 8;
    private static final int COMPACT_WIDTH_THRESHOLD = 360;
    private static final int COMPACT_BUTTON_ROWS = 2;
    private static final int BUTTON_RESERVE_COMPACT_EXTRA = 22;
    private static final int BUTTON_RESERVE_NORMAL_EXTRA = 16;
    private static final int HEADER_RESERVE = 54;
    private static final int CONTENT_MIN_HEIGHT_COMPACT = 160;
    private static final int CONTENT_MIN_HEIGHT_NORMAL = 145;
    private static final int CONTENT_MAX_HEIGHT = 248;
    private static final int DIALOG_MIN_HEIGHT_COMPACT = 240;
    private static final int DIALOG_MIN_HEIGHT_NORMAL = 220;
    private static final int PICKER_WIDTH_MIN = 150;
    private static final int PICKER_WIDTH_RESERVE = 12;
    private static final int PICKER_HEIGHT_COMPACT = 112;
    private static final int PICKER_HEIGHT_NORMAL = 142;
    private static final int SWATCH_SIZE_MIN = 12;
    private static final int SWATCH_SIZE_MAX = 18;
    private static final int SWATCH_SIZE_DIVISOR = 24;
    private static final int HEX_INPUT_MIN = 84;
    private static final int HEX_INPUT_MAX = 128;
    private static final int HEX_INPUT_DIVISOR = 4;
    private static final int RGB_INPUT_MIN = 52;
    private static final int RGB_INPUT_MAX = 64;
    private static final int RGB_INPUT_DIVISOR = 9;
    private static final int FOOTER_BUTTON_MIN_WIDTH = 72;
    private static final int FOOTER_BUTTON_MAX_WIDTH = 140;
    private static final int FOOTER_BUTTON_DIVISOR = 4;
    private static final int FOOTER_BUTTON_TEXT_RESERVE = 10;
    private static final int FOOTER_BUTTON_TEXT_MIN_WIDTH = 24;
    private static final int SAVE_ACTION_BUTTON_MIN_WIDTH = 96;
    private static final int SAVED_REMOVE_BUTTON_WIDTH = 58;
    private static final int CONTENT_WIDTH_RESERVE = 38;
    private static final int CONTENT_SCROLLBAR_GUTTER_BASE = 10;

    private ColorPickerDialog() {
    }

    public static FlowLayout create(String title, int initialRgb, IntConsumer onApply, Runnable onCancel) {
        FlowLayout overlay = DialogUiUtil.overlay();
        int viewportWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int preferredDialogWidth = viewportWidth <= DIALOG_FULL_WIDTH_THRESHOLD ? viewportWidth : DIALOG_WIDTH;
        int dialogWidth = DialogUiUtil.dialogWidth(preferredDialogWidth);
        boolean compactLayout = DialogUiUtil.compactButtons(dialogWidth, COMPACT_WIDTH_THRESHOLD);
        int buttonRowReserve = DialogUiUtil.buttonRowReserve(
                compactLayout,
                COMPACT_BUTTON_ROWS,
                BUTTON_RESERVE_COMPACT_EXTRA,
                BUTTON_RESERVE_NORMAL_EXTRA
        );
        int headerReserve = UiFactory.scaledPixels(HEADER_RESERVE);
        int contentHeight = DialogUiUtil.availableDialogContentHeight(
                headerReserve + buttonRowReserve,
                compactLayout ? CONTENT_MIN_HEIGHT_COMPACT : CONTENT_MIN_HEIGHT_NORMAL
        );
        if (!compactLayout) {
            contentHeight = Math.min(contentHeight, CONTENT_MAX_HEIGHT);
        }
        int dialogHeight = DialogUiUtil.dialogHeight(
                headerReserve + buttonRowReserve + contentHeight,
                compactLayout ? DIALOG_MIN_HEIGHT_COMPACT : DIALOG_MIN_HEIGHT_NORMAL
        );
        int contentWidthHint = Math.max(1, dialogWidth - UiFactory.scaledPixels(CONTENT_WIDTH_RESERVE));
        int preferredPickerWidth = Math.max(PICKER_WIDTH_MIN, contentWidthHint - PICKER_WIDTH_RESERVE);
        int pickerWidth = Math.min(contentWidthHint, preferredPickerWidth);
        int pickerHeight = compactLayout ? PICKER_HEIGHT_COMPACT : PICKER_HEIGHT_NORMAL;
        int swatchSize = Math.max(SWATCH_SIZE_MIN, Math.min(SWATCH_SIZE_MAX, dialogWidth / SWATCH_SIZE_DIVISOR));
        int hexInputWidth = Math.min(contentWidthHint, Math.max(HEX_INPUT_MIN, Math.min(HEX_INPUT_MAX, contentWidthHint / HEX_INPUT_DIVISOR)));
        int rgbInputWidth = Math.min(contentWidthHint, Math.max(RGB_INPUT_MIN, Math.min(RGB_INPUT_MAX, contentWidthHint / RGB_INPUT_DIVISOR)));

        FlowLayout dialog = DialogUiUtil.dialogCard(dialogWidth, dialogHeight, DIALOG_GAP);
        dialog.child(UiFactory.title(title));
        FlowLayout content = UiFactory.column().gap(CONTENT_GAP);
        content.padding(Insets.of(0, UiFactory.scrollContentInset(CONTENT_SCROLLBAR_GUTTER_BASE), 0, 0));

        AtomicInteger selectedRgb = new AtomicInteger(initialRgb & 0xFFFFFF);
        AtomicBoolean syncing = new AtomicBoolean(false);

        ColorPickerComponent picker = new ColorPickerComponent()
                .selectedColor(Color.ofRgb(selectedRgb.get()))
                .showAlpha(false)
                .selectorWidth(18)
                .selectorPadding(8);
        ColorPickerUiUtil.applyPickerSizing(picker, compactLayout, pickerWidth, pickerHeight);

        ColorPickerUiUtil.Swatch swatch = ColorPickerUiUtil.createSwatch(selectedRgb.get(), swatchSize);
        LabelComponent errorLabel = UiFactory.message("", 0xFF8A8A);

        FlowLayout sampleRow = UiFactory.row();
        sampleRow.horizontalSizing(Sizing.content());
        sampleRow.child(swatch.swatch());
        sampleRow.child(swatch.label());

        TextBoxComponent hexInput = UiFactory.textBox(ValidationUtil.toHex(selectedRgb.get()), value -> {});
        hexInput.horizontalSizing(UiFactory.fixed(hexInputWidth));
        TextBoxComponent redInput = UiFactory.textBox(Integer.toString((selectedRgb.get() >> 16) & 0xFF), value -> {});
        TextBoxComponent greenInput = UiFactory.textBox(Integer.toString((selectedRgb.get() >> 8) & 0xFF), value -> {});
        TextBoxComponent blueInput = UiFactory.textBox(Integer.toString(selectedRgb.get() & 0xFF), value -> {});

        Runnable syncInputs = ColorPickerUiUtil.createSyncRunnable(
                syncing,
                selectedRgb::get,
                picker,
                swatch.swatch(),
                swatch.label(),
                hexInput,
                ColorPickerUiUtil.rgbPostSync(selectedRgb::get, redInput, greenInput, blueInput, errorLabel)
        );

        IntConsumer setSelectedRgb = rgb -> {
            selectedRgb.set(rgb & 0xFFFFFF);
            syncInputs.run();
        };

        ColorPickerUiUtil.bindPickerAndRgbInputs(
                picker,
                syncing,
                hexInput,
                redInput,
                greenInput,
                blueInput,
                errorLabel,
                setSelectedRgb
        );

        FlowLayout hexField = ColorPickerUiUtil.compactInputField(ItemEditorText.tr("common.hex"), hexInput, hexInputWidth, false);
        FlowLayout redField = ColorPickerUiUtil.compactInputField(ItemEditorText.tr("common.rgb.red"), redInput, rgbInputWidth, true);
        FlowLayout greenField = ColorPickerUiUtil.compactInputField(ItemEditorText.tr("common.rgb.green"), greenInput, rgbInputWidth, true);
        FlowLayout blueField = ColorPickerUiUtil.compactInputField(ItemEditorText.tr("common.rgb.blue"), blueInput, rgbInputWidth, true);
        redField.horizontalSizing(compactLayout ? Sizing.fill(100) : Sizing.fill(33));
        greenField.horizontalSizing(compactLayout ? Sizing.fill(100) : Sizing.fill(33));
        blueField.horizontalSizing(compactLayout ? Sizing.fill(100) : Sizing.fill(33));

        FlowLayout hexRow = compactLayout ? UiFactory.column() : UiFactory.row();
        hexRow.horizontalSizing(Sizing.fill(100));
        if (compactLayout) {
            hexRow.child(sampleRow);
            hexRow.child(hexField);
        } else {
            hexRow.child(hexField);
            hexRow.child(sampleRow);
        }

        FlowLayout rgbRow = compactLayout ? UiFactory.column() : UiFactory.row();
        rgbRow.horizontalSizing(Sizing.fill(100));
        if (compactLayout) {
            FlowLayout topRgb = UiFactory.row();
            topRgb.horizontalSizing(Sizing.fill(100));
            topRgb.child(redField);
            topRgb.child(greenField);
            rgbRow.child(topRgb);
            rgbRow.child(blueField);
        } else {
            rgbRow.child(redField);
            rgbRow.child(greenField);
            rgbRow.child(blueField);
        }

        content.child(picker);
        content.child(hexRow);
        content.child(rgbRow);

        FlowLayout savedCard = UiFactory.subCard();
        savedCard.horizontalSizing(Sizing.fill(100));
        savedCard.child(UiFactory.title(ItemEditorText.tr("dialog.color_picker.saved_title")).shadow(false));
        FlowLayout savedList = UiFactory.column().gap(Math.max(1, UiFactory.scaleProfile().tightSpacing() - 1));
        savedList.horizontalSizing(Sizing.fill(100));
        savedCard.child(savedList);
        AtomicReference<Runnable> refreshSaved = new AtomicReference<>(() -> {});

        FlowLayout presetActions = UiFactory.row();
        presetActions.horizontalSizing(Sizing.fill(100));
        presetActions.horizontalAlignment(compactLayout ? HorizontalAlignment.CENTER : HorizontalAlignment.LEFT);
        ButtonComponent saveCurrentButton = UiFactory.button(
                ItemEditorText.tr("dialog.color_picker.save_current"),
                UiFactory.ButtonTextPreset.COMPACT,
                button -> {
                    TextColorPresets.saveColorPreset(selectedRgb.get());
                    refreshSaved.get().run();
                }
        );
        saveCurrentButton.horizontalSizing(compactLayout ? Sizing.fill(100) : UiFactory.fixed(Math.max(SAVE_ACTION_BUTTON_MIN_WIDTH, dialogWidth / 4)));
        presetActions.child(saveCurrentButton);

        refreshSaved.set(() -> rebuildSavedColors(savedList, setSelectedRgb, onApply, refreshSaved.get()));
        refreshSaved.get().run();
        content.child(presetActions);
        content.child(savedCard);
        content.child(errorLabel);
        dialog.child(DialogUiUtil.scrollCard(content, contentHeight));

        FlowLayout buttonRow = DialogUiUtil.footerRowByDivisor(
                dialogWidth,
                compactLayout,
                FOOTER_BUTTON_MIN_WIDTH,
                FOOTER_BUTTON_MAX_WIDTH,
                FOOTER_BUTTON_DIVISOR,
                FOOTER_BUTTON_TEXT_MIN_WIDTH,
                FOOTER_BUTTON_TEXT_RESERVE,
                new DialogUiUtil.FooterAction(ItemEditorText.tr("common.cancel"), button -> onCancel.run()),
                new DialogUiUtil.FooterAction(ItemEditorText.tr("dialog.color_picker.apply"), button -> onApply.accept(selectedRgb.get()))
        );
        dialog.child(buttonRow);

        overlay.child(dialog);
        return overlay;
    }

    private static void rebuildSavedColors(
            FlowLayout savedList,
            IntConsumer applyColor,
            IntConsumer onApply,
            Runnable onChanged
    ) {
        savedList.clearChildren();
        List<TextColorPresets.CustomColorPreset> presets = TextColorPresets.customColorPresets();
        if (presets.isEmpty()) {
            savedList.child(UiFactory.muted(ItemEditorText.tr("dialog.color_picker.saved_none")));
            return;
        }

        for (TextColorPresets.CustomColorPreset preset : presets) {
            savedList.child(ColorPickerUiUtil.savedPresetRow(
                    Component.literal(preset.name()).withColor(preset.rgb()),
                    () -> {
                        applyColor.accept(preset.rgb());
                        onApply.accept(preset.rgb());
                    },
                    () -> {
                        TextColorPresets.removeColorPreset(preset.id());
                        onChanged.run();
                    },
                    SAVED_REMOVE_BUTTON_WIDTH,
                    ItemEditorText.tr("dialog.color_picker.saved_remove_hint")
            ));
        }
    }

}
