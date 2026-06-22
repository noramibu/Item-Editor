package me.noramibu.itemeditor.ui.component;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.CheckboxComponent;
import io.wispforest.owo.ui.component.ColorPickerComponent;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.component.TextBoxComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.core.Color;
import io.wispforest.owo.ui.core.HorizontalAlignment;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.VerticalAlignment;
import me.noramibu.itemeditor.ui.util.ScrollStateUtil;
import me.noramibu.itemeditor.util.ItemEditorText;
import me.noramibu.itemeditor.util.TextColorPresets;
import me.noramibu.itemeditor.util.ValidationUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public final class UnifiedColorPickerDialog {

    private static final double PICKER_SIZE_SCALE = 0.70d;
    private static final int DIALOG_GAP = 6;
    private static final int CONTENT_GAP = 6;
    private static final int COMPACT_BUTTON_WIDTH_THRESHOLD = 360;
    private static final int COMPACT_BUTTON_ROWS = 2;
    private static final int BUTTON_RESERVE_COMPACT_EXTRA = 22;
    private static final int BUTTON_RESERVE_NORMAL_EXTRA = 16;
    private static final int HEADER_RESERVE = 66;
    private static final int CONTENT_WIDTH_RESERVE = 24;
    private static final int SCROLLBAR_GUTTER_BASE = 8;
    private static final int CONTENT_MIN_HEIGHT = 168;
    private static final int PREVIEW_TEXT_WIDTH_RESERVE = 8;
    private static final int DIALOG_MIN_HEIGHT = 252;
    private static final int WIDE_LAYOUT_WIDTH_THRESHOLD = 430;
    private static final int LEFT_COLUMN_WIDTH_PERCENT = 42;
    private static final int LEFT_COLUMN_COMPACT_WIDTH_PERCENT = 36;
    private static final int PICKER_MIN_SIZE = 72;
    private static final int PICKER_MAX_SIZE = 148;
    private static final int PICKER_SELECTOR_WIDTH = 14;
    private static final int PICKER_SELECTOR_PADDING = 6;
    private static final int HEX_INPUT_WIDTH = 90;
    private static final int RGB_INPUT_WIDTH = 48;
    private static final int RGB_INPUT_COMPACT_WIDTH = 38;
    private static final int RGB_INPUT_MIN_WIDTH = 28;
    private static final int RGB_INPUT_COMPACT_THRESHOLD = 300;
    private static final int RGB_ROW_EDGE_PADDING = 6;
    private static final int SWATCH_SIZE = 18;
    private static final int SWATCH_COMPACT_SIZE = 14;
    private static final int STOP_ROW_ACTION_WIDTH = 24;
    private static final int SAVED_ACTION_BUTTON_WIDTH = 24;
    private static final int SAVED_ACTION_COUNT = 3;
    private static final int SAVED_EDIT_ACTION_COUNT = 4;
    private static final int SAVED_LABEL_TEXT_RESERVE = 12;
    private static final int SAVED_LABEL_MIN_WIDTH = 28;
    private static final int FOOTER_BUTTON_MIN_WIDTH = 72;
    private static final int FOOTER_BUTTON_MAX_WIDTH = 150;
    private static final int FOOTER_BUTTON_DIVISOR = 4;
    private static final int FOOTER_BUTTON_TEXT_RESERVE = 10;
    private static final int FOOTER_BUTTON_TEXT_MIN_WIDTH = 24;
    private static final int MODE_BUTTON_MIN_WIDTH = 64;
    private static final int MODE_BUTTON_TEXT_RESERVE = 26;
    private static final int MODE_BUTTON_TEXT_MIN_WIDTH = 18;
    private static final int MODE_CHECKBOX_TEXT_RESERVE = 34;
    private static final int CONTENT_BOTTOM_PADDING_EXTRA = 8;
    private static final String SAMPLE_TEXT = "lorem ipsum dolar sit amet consectetur adipiscing elit sed do eiusmod tempor";

    private UnifiedColorPickerDialog() {
    }

    public static FlowLayout create(
            String title,
            Options options,
            Consumer<ColorPickerResult> onApply,
            Runnable onCancel
    ) {
        Options normalizedOptions = options == null ? Options.richText(0xFFFFFF, List.of(0xFFFFFF, 0x55FFFF)) : options;
        PickerState state = new PickerState(normalizedOptions);
        FlowLayout overlay = DialogUiUtil.overlay();
        int dialogWidth = DialogUiUtil.dialogWidth(Integer.MAX_VALUE);
        boolean compactButtons = DialogUiUtil.compactButtons(dialogWidth, COMPACT_BUTTON_WIDTH_THRESHOLD);
        boolean compactLayout = dialogWidth < 420 || compactButtons;
        int buttonRowReserve = DialogUiUtil.buttonRowReserve(
                compactButtons,
                COMPACT_BUTTON_ROWS,
                BUTTON_RESERVE_COMPACT_EXTRA,
                BUTTON_RESERVE_NORMAL_EXTRA
        );
        int headerReserve = UiFactory.scaledPixels(HEADER_RESERVE);
        int dialogHeight = DialogUiUtil.dialogHeight(Integer.MAX_VALUE, DIALOG_MIN_HEIGHT);
        int contentHeight = Math.max(CONTENT_MIN_HEIGHT, dialogHeight - headerReserve - buttonRowReserve);
        int contentWidth = Math.max(1, dialogWidth - UiFactory.scaledPixels(CONTENT_WIDTH_RESERVE));

        FlowLayout dialog = DialogUiUtil.dialogCard(dialogWidth, dialogHeight, DIALOG_GAP);
        dialog.child(UiFactory.title(title));

        FlowLayout modeSlot = UiFactory.column().gap(Math.max(1, UiFactory.scaleProfile().tightSpacing()));
        dialog.child(modeSlot);

        FlowLayout contentSlot = UiFactory.column();
        ScrollContainer<FlowLayout> contentScroll = DialogUiUtil.scrollContentExpand(contentSlot);
        dialog.child(contentScroll);

        FlowLayout footerSlot = UiFactory.column();
        dialog.child(footerSlot);

        AtomicReference<Runnable> rebuildAll = new AtomicReference<>(() -> {});
        Runnable rebuildChrome = () -> {
            modeSlot.clearChildren();
            modeSlot.child(buildModeControls(normalizedOptions, state, contentWidth, rebuildAll.get()));
        };
        Runnable rebuildFooter = () -> {
            footerSlot.clearChildren();
            FlowLayout buttonRow = DialogUiUtil.footerRowByDivisor(
                    dialogWidth,
                    false,
                    FOOTER_BUTTON_MIN_WIDTH,
                    FOOTER_BUTTON_MAX_WIDTH,
                    FOOTER_BUTTON_DIVISOR,
                    FOOTER_BUTTON_TEXT_MIN_WIDTH,
                    FOOTER_BUTTON_TEXT_RESERVE,
                    new DialogUiUtil.FooterAction(ItemEditorText.tr("common.cancel"), button -> onCancel.run()),
                    new DialogUiUtil.FooterAction(ItemEditorText.tr(applyKey(state)), button -> onApply.accept(state.result()))
            );
            buttonRow.horizontalAlignment(HorizontalAlignment.RIGHT);
            footerSlot.child(buttonRow);
        };
        rebuildAll.set(() -> {
            boolean restoreScroll = !contentSlot.children().isEmpty();
            double scrollOffset = restoreScroll ? ScrollStateUtil.offset(contentScroll) : 0;
            state.ensureAllowedMode(normalizedOptions);
            rebuildChrome.run();
            rebuildBody(contentSlot, normalizedOptions, state, contentWidth, contentHeight, compactLayout, rebuildAll.get());
            rebuildFooter.run();
            if (restoreScroll) {
                restoreScroll(contentScroll, scrollOffset);
            }
        });
        rebuildAll.get().run();

        overlay.child(dialog);
        return overlay;
    }

    private static FlowLayout buildModeControls(Options options, PickerState state, int contentWidth, Runnable rebuildAll) {
        FlowLayout controls = UiFactory.column().gap(Math.max(1, UiFactory.scaleProfile().tightSpacing()));
        List<ModeButtonSpec> buttons = new ArrayList<>();
        Component shadowLabel = options.allowShadow() ? ItemEditorText.tr("dialog.unified_color_picker.shadow") : Component.empty();
        if (options.allowColorMode()) {
            Component label = ItemEditorText.tr("dialog.unified_color_picker.mode.color");
            buttons.add(new ModeButtonSpec(label, state.mode() != PaintMode.COLOR, () -> {
                state.setMode(PaintMode.COLOR);
                rebuildAll.run();
            }));
        }
        if (options.allowGradientMode()) {
            Component label = ItemEditorText.tr("dialog.unified_color_picker.mode.gradient");
            buttons.add(new ModeButtonSpec(label, state.mode() != PaintMode.GRADIENT, () -> {
                state.setMode(PaintMode.GRADIENT);
                rebuildAll.run();
            }));
        }
        if (!buttons.isEmpty()) {
            int modeButtonWidth = modeButtonWidth(contentWidth, buttons);
            if (modeControlsFitWithShadow(contentWidth, buttons, shadowLabel)) {
                FlowLayout row = UiFactory.row();
                for (ModeButtonSpec spec : buttons) {
                    row.child(modeButton(spec, modeButtonDesiredWidth(spec.label())));
                }
                row.child(UiFactory.checkbox(shadowLabel, state.shadow(), checked -> {
                    state.shadow(checked);
                    rebuildAll.run();
                }));
                controls.child(row);
            } else if (modeButtonsNeedStacking(contentWidth, buttons)) {
                for (ModeButtonSpec spec : buttons) {
                    controls.child(modeButton(spec, modeButtonWidth));
                }
            } else {
                FlowLayout row = UiFactory.row();
                for (ModeButtonSpec spec : buttons) {
                    row.child(modeButton(spec, modeButtonDesiredWidth(spec.label())));
                }
                controls.child(row);
            }
        }
        if (options.allowShadow() && !modeControlsFitWithShadow(contentWidth, buttons, shadowLabel)) {
            controls.child(UiFactory.checkbox(shadowLabel, state.shadow(), checked -> {
                state.shadow(checked);
                rebuildAll.run();
            }));
        }
        return controls;
    }

    private static void rebuildBody(
            FlowLayout contentSlot,
            Options options,
            PickerState state,
            int contentWidth,
            int contentHeight,
            boolean compactLayout,
            Runnable rebuildAll
    ) {
        contentSlot.clearChildren();

        boolean wideLayout = contentWidth >= WIDE_LAYOUT_WIDTH_THRESHOLD;
        int columnGap = UiFactory.scaleProfile().spacing();
        int leftColumnPercent = compactLayout ? LEFT_COLUMN_COMPACT_WIDTH_PERCENT : LEFT_COLUMN_WIDTH_PERCENT;
        int leftColumnWidth = wideLayout
                ? Math.max(PICKER_MIN_SIZE, (contentWidth - columnGap) * leftColumnPercent / 100)
                : contentWidth;
        int pickerContentWidth = wideLayout ? leftColumnWidth : contentWidth;
        int savedContentWidth = wideLayout
                ? Math.max(PICKER_MIN_SIZE, contentWidth - leftColumnWidth - columnGap)
                : pickerContentWidth;
        int controlsContentWidth = wideLayout ? savedContentWidth : pickerContentWidth;
        int previewTextWidth = Math.max(1, pickerContentWidth - UiFactory.scaledPixels(PREVIEW_TEXT_WIDTH_RESERVE));
        int pickerSize = pickerSize(Math.min(pickerContentWidth, contentHeight));
        int rgbInputWidth = rgbInputWidth(pickerContentWidth);

        FlowLayout pickerSection = section();
        LabelComponent selectedTitle = UiFactory.title(selectedStopTitle(state)).shadow(false).maxWidth(pickerContentWidth);
        pickerSection.child(selectedTitle);

        LabelComponent errorLabel = UiFactory.message("", 0xFF8A8A);
        AtomicBoolean syncing = new AtomicBoolean(false);
        ColorPickerComponent picker = new ColorPickerComponent()
                .selectedColor(Color.ofRgb(state.selectedRgb()))
                .showAlpha(false)
                .selectorWidth(PICKER_SELECTOR_WIDTH)
                .selectorPadding(PICKER_SELECTOR_PADDING);
        ColorPickerUiUtil.applyPickerSizing(picker, pickerSize, pickerSize);

        ColorPickerUiUtil.Swatch swatch = ColorPickerUiUtil.createSwatch(state.selectedRgb(), compactLayout ? SWATCH_COMPACT_SIZE : SWATCH_SIZE);
        TextBoxComponent hexInput = UiFactory.textBox(ValidationUtil.toHex(state.selectedRgb()), value -> {});
        TextBoxComponent redInput = UiFactory.textBox(Integer.toString((state.selectedRgb() >> 16) & 0xFF), value -> {});
        TextBoxComponent greenInput = UiFactory.textBox(Integer.toString((state.selectedRgb() >> 8) & 0xFF), value -> {});
        TextBoxComponent blueInput = UiFactory.textBox(Integer.toString(state.selectedRgb() & 0xFF), value -> {});

        FlowLayout stopsSection = section();
        FlowLayout stopsList = UiFactory.column().gap(Math.max(1, UiFactory.scaleProfile().tightSpacing() - 1));
        FlowLayout stopActions = UiFactory.column().gap(Math.max(1, UiFactory.scaleProfile().tightSpacing()));
        if (state.mode() == PaintMode.GRADIENT) {
            stopsSection.child(UiFactory.title(ItemEditorText.tr(stopsTitleKey(state))).shadow(false).maxWidth(controlsContentWidth));
            stopsSection.child(stopsList);
            stopsSection.child(stopActions);
        }

        FlowLayout preview = section();
        Runnable refreshPreview = () -> {
            preview.clearChildren();
            preview.child(UiFactory.title(ItemEditorText.tr("screen.preview")).shadow(false).maxWidth(previewTextWidth));
            preview.child(buildPreview(state, options.previewText(), previewTextWidth));
        };

        AtomicReference<Runnable> refreshUi = new AtomicReference<>(() -> {});
        Runnable syncInputs = ColorPickerUiUtil.createSyncRunnable(
                syncing,
                state::selectedRgb,
                picker,
                swatch.swatch(),
                swatch.label(),
                hexInput,
                ColorPickerUiUtil.rgbPostSync(state::selectedRgb, redInput, greenInput, blueInput, errorLabel)
        );
        ColorPickerUiUtil.bindPickerAndRgbInputs(
                picker,
                syncing,
                hexInput,
                redInput,
                greenInput,
                blueInput,
                errorLabel,
                rgb -> {
                    state.setSelectedRgb(rgb);
                    syncInputs.run();
                    refreshUi.get().run();
                }
        );

        FlowLayout sampleRow = UiFactory.row();
        sampleRow.child(swatch.swatch());
        sampleRow.child(swatch.label());
        FlowLayout hexRow = UiFactory.row();
        hexRow.child(ColorPickerUiUtil.compactInputField(ItemEditorText.tr("common.hex"), hexInput, HEX_INPUT_WIDTH));
        hexRow.child(sampleRow);
        FlowLayout rgbRow = UiFactory.row();
        rgbRow.child(ColorPickerUiUtil.compactInputFieldPixels(ItemEditorText.tr("common.rgb.red"), redInput, rgbInputWidth));
        rgbRow.child(ColorPickerUiUtil.compactInputFieldPixels(ItemEditorText.tr("common.rgb.green"), greenInput, rgbInputWidth));
        rgbRow.child(ColorPickerUiUtil.compactInputFieldPixels(ItemEditorText.tr("common.rgb.blue"), blueInput, rgbInputWidth));
        pickerSection.child(picker);
        pickerSection.child(hexRow);
        pickerSection.child(rgbRow);

        FlowLayout savedSection = section();
        savedSection.child(UiFactory.title(ItemEditorText.tr("dialog.unified_color_picker.saved_presets")).shadow(false).maxWidth(savedContentWidth));
        FlowLayout savedList = UiFactory.column().gap(Math.max(1, UiFactory.scaleProfile().tightSpacing() - 1));
        AtomicReference<Runnable> refreshSaved = new AtomicReference<>(() -> {});
        Component saveLabel = ItemEditorText.tr(saveKey(state));
        ButtonComponent saveCurrentButton = UiFactory.button(
                saveLabel,
                UiFactory.ButtonTextPreset.COMPACT,
                button -> {
                    savePreset(state);
                    refreshSaved.get().run();
                }
        );
        saveCurrentButton.setMessage(UiFactory.fitToWidth(saveLabel, Math.max(24, savedContentWidth - UiFactory.scaledPixels(SAVED_LABEL_TEXT_RESERVE))));
        if (!saveCurrentButton.getMessage().getString().equals(saveLabel.getString())) {
            saveCurrentButton.tooltip(List.of(saveLabel));
        }
        saveCurrentButton.horizontalSizing(Sizing.fill(100));
        savedSection.child(saveCurrentButton);
        savedSection.child(buildSavedFilters(options, state, savedContentWidth, () -> refreshSaved.get().run()));
        savedSection.child(savedList);

        refreshUi.set(() -> {
            selectedTitle.text(selectedStopTitle(state));
            if (state.mode() == PaintMode.GRADIENT) {
                rebuildStops(stopsList, state, syncInputs, refreshUi.get(), controlsContentWidth);
                rebuildStopActions(stopActions, state, syncInputs, refreshUi.get());
            }
            refreshPreview.run();
            refreshSaved.get().run();
        });
        refreshSaved.set(() -> rebuildSavedPresets(options, state, savedList, savedContentWidth, rebuildAll, refreshSaved.get()));
        refreshUi.get().run();

        FlowLayout content = wideLayout ? UiFactory.row().gap(CONTENT_GAP) : UiFactory.column().gap(CONTENT_GAP);
        int contentBottomPadding = UiFactory.scaleProfile().controlHeight()
                + UiFactory.scaledPixels(CONTENT_BOTTOM_PADDING_EXTRA);
        content.padding(Insets.of(0, contentBottomPadding, 0, UiFactory.scrollContentInset(SCROLLBAR_GUTTER_BASE)));
        if (wideLayout) {
            content.verticalAlignment(VerticalAlignment.TOP);
            FlowLayout leftColumn = UiFactory.column().gap(CONTENT_GAP);
            FlowLayout rightColumn = UiFactory.column().gap(CONTENT_GAP);
            leftColumn.child(pickerSection);
            leftColumn.child(preview);
            leftColumn.child(errorLabel);
            if (state.mode() == PaintMode.GRADIENT) {
                rightColumn.child(stopsSection);
            }
            rightColumn.child(savedSection);
            leftColumn.horizontalSizing(Sizing.fixed(leftColumnWidth));
            rightColumn.horizontalSizing(Sizing.expand(100));
            content.child(leftColumn);
            content.child(rightColumn);
        } else {
            content.child(pickerSection);
            if (state.mode() == PaintMode.GRADIENT) {
                content.child(stopsSection);
            }
            content.child(preview);
            content.child(savedSection);
            content.child(errorLabel);
        }
        contentSlot.child(content);
    }

    private static FlowLayout section() {
        FlowLayout section = UiFactory.column().gap(Math.max(1, UiFactory.scaleProfile().tightSpacing()));
        section.horizontalSizing(Sizing.fill(100));
        return section;
    }

    private static FlowLayout buildSavedFilters(Options options, PickerState state, int maxWidth, Runnable refreshSaved) {
        FlowLayout filters = UiFactory.column().gap(Math.max(1, UiFactory.scaleProfile().tightSpacing() - 1));
        List<SavedFilterSpec> specs = new ArrayList<>();
        if (options.showColorPresets()) {
            specs.add(new SavedFilterSpec(ItemEditorText.tr("dialog.unified_color_picker.show_colors"), state.showColorPresets(), checked -> {
                state.showColorPresets(checked);
                refreshSaved.run();
            }));
        }
        if (options.showGradientPresets()) {
            specs.add(new SavedFilterSpec(ItemEditorText.tr("dialog.unified_color_picker.show_gradients"), state.showGradientPresets(), checked -> {
                state.showGradientPresets(checked);
                refreshSaved.run();
            }));
        }
        if (options.showShadowPresets()) {
            specs.add(new SavedFilterSpec(ItemEditorText.tr("dialog.unified_color_picker.show_shadows"), state.showShadowPresets(), checked -> {
                state.showShadowPresets(checked);
                refreshSaved.run();
            }));
        }
        appendSavedFilterRows(filters, specs, maxWidth);
        return filters;
    }

    private static void appendSavedFilterRows(FlowLayout filters, List<SavedFilterSpec> specs, int maxWidth) {
        if (specs.isEmpty()) {
            return;
        }
        int availableWidth = Math.max(1, maxWidth);
        int gap = UiFactory.scaleProfile().spacing();
        FlowLayout row = savedFilterRow();
        int rowWidth = 0;
        for (SavedFilterSpec spec : specs) {
            int checkboxWidth = Math.min(availableWidth, checkboxDesiredWidth(spec.label()));
            int projectedWidth = rowWidth == 0 ? checkboxWidth : rowWidth + gap + checkboxWidth;
            if (rowWidth > 0 && projectedWidth > availableWidth) {
                filters.child(row);
                row = savedFilterRow();
                rowWidth = 0;
            }
            CheckboxComponent checkbox = UiFactory.checkbox(spec.label(), spec.checked(), spec.onChanged());
            checkbox.horizontalSizing(Sizing.fixed(checkboxWidth));
            row.child(checkbox);
            rowWidth = rowWidth == 0 ? checkboxWidth : rowWidth + gap + checkboxWidth;
        }
        if (!row.children().isEmpty()) {
            filters.child(row);
        }
    }

    private static FlowLayout savedFilterRow() {
        FlowLayout row = UiFactory.row();
        row.horizontalSizing(Sizing.fill(100));
        return row;
    }

    private static void rebuildStops(
            FlowLayout stopsList,
            PickerState state,
            Runnable syncInputs,
            Runnable refreshUi,
            int rowWidth
    ) {
        stopsList.clearChildren();
        int selectButtonWidth = stopSelectButtonWidth(rowWidth);
        for (int index = 0; index < state.size(); index++) {
            int stopIndex = index;
            int rgb = state.color(stopIndex);
            FlowLayout row = UiFactory.row();
            row.horizontalSizing(Sizing.fill(100));
            Component label = Component.literal(stopButtonLabel(state, stopIndex)).withColor(rgb);
            ButtonComponent select = ButtonFitUtil.fixedWidthFittedButton(
                    label,
                    UiFactory.ButtonTextPreset.COMPACT,
                    selectButtonWidth,
                    24,
                    8,
                    button -> {
                        state.select(stopIndex);
                        syncInputs.run();
                        refreshUi.run();
                    }
            );
            row.child(select);
            row.child(stopRowAction("^", stopIndex > 0, () -> {
                state.move(stopIndex, stopIndex - 1);
                syncInputs.run();
                refreshUi.run();
            }));
            row.child(stopRowAction("v", stopIndex < state.size() - 1, () -> {
                state.move(stopIndex, stopIndex + 1);
                syncInputs.run();
                refreshUi.run();
            }));
            row.child(stopRowAction("x", state.size() > state.minimumStops(), () -> {
                state.remove(stopIndex);
                syncInputs.run();
                refreshUi.run();
            }));
            stopsList.child(row);
        }
    }

    private static void rebuildStopActions(
            FlowLayout stopActions,
            PickerState state,
            Runnable syncInputs,
            Runnable refreshUi
    ) {
        stopActions.clearChildren();
        ButtonComponent add = stopAction(ItemEditorText.tr("common.add"), true, () -> {
            state.addAfterSelected();
            syncInputs.run();
            refreshUi.run();
        });
        ButtonComponent remove = stopAction(ItemEditorText.tr("common.remove"), state.size() > state.minimumStops(), () -> {
            state.removeSelected();
            syncInputs.run();
            refreshUi.run();
        });
        FlowLayout row = UiFactory.row();
        add.horizontalSizing(Sizing.expand(100));
        remove.horizontalSizing(Sizing.expand(100));
        row.child(add);
        row.child(remove);
        stopActions.child(row);
    }

    private static ButtonComponent stopAction(Component label, boolean active, Runnable action) {
        ButtonComponent button = UiFactory.button(label, UiFactory.ButtonTextPreset.TINY, ignored -> action.run());
        button.active(active);
        return button;
    }

    private static ButtonComponent stopRowAction(String label, boolean active, Runnable action) {
        ButtonComponent button = UiFactory.button(Component.literal(label), UiFactory.ButtonTextPreset.TINY, ignored -> action.run());
        button.active(active);
        button.horizontalSizing(UiFactory.fixed(STOP_ROW_ACTION_WIDTH));
        return button;
    }

    private static void savePreset(PickerState state) {
        if (state.shadow()) {
            String editId = state.editingShadowPresetId();
            if (editId == null || editId.isBlank() || !TextColorPresets.updateShadowPreset(editId, state.resultColors())) {
                TextColorPresets.saveShadowPreset(state.resultColors());
            }
        } else if (state.mode() == PaintMode.COLOR) {
            TextColorPresets.saveColorPreset(state.selectedRgb());
        } else {
            String editId = state.editingGradientPresetId();
            if (editId == null || editId.isBlank() || !TextColorPresets.updateGradientPreset(editId, state.resultColors())) {
                TextColorPresets.saveGradientPreset(state.resultColors());
            }
        }
    }

    private static void rebuildSavedPresets(
            Options options,
            PickerState state,
            FlowLayout savedList,
            int savedContentWidth,
            Runnable rebuildAll,
            Runnable refreshSaved
    ) {
        savedList.clearChildren();
        int rowCount = 0;
        if (options.showColorPresets() && state.showColorPresets()) {
            rowCount += rebuildSavedPresetGroup(
                    savedList,
                    TextColorPresets.customColorPresets(),
                    "dialog.unified_color_picker.saved_colors",
                    savedContentWidth,
                    SAVED_ACTION_COUNT,
                    (preset, width) -> savedColorButtonLabel(options.previewText(), preset.rgb(), width),
                    preset -> Component.literal(ValidationUtil.toHex(preset.rgb())).withColor(preset.rgb()),
                    preset -> matchesColorPreset(state, preset.rgb()),
                    preset -> {
                        state.load(PaintMode.COLOR, false, List.of(preset.rgb()));
                        rebuildAll.run();
                    },
                    null,
                    Component.empty(),
                    (preset, direction) -> TextColorPresets.moveColorPreset(preset.id(), direction),
                    preset -> TextColorPresets.removeColorPreset(preset.id()),
                    ItemEditorText.tr("dialog.unified_color_picker.saved_color_remove_hint"),
                    refreshSaved
            );
        }
        if (options.showGradientPresets() && state.showGradientPresets()) {
            rowCount += rebuildSavedPresetGroup(
                    savedList,
                    TextColorPresets.customGradientPresets(),
                    "dialog.unified_color_picker.saved_gradients",
                    savedContentWidth,
                    SAVED_EDIT_ACTION_COUNT,
                    (preset, width) -> savedGradientButtonLabel(options.previewText(), preset.colors(), width),
                    preset -> colorCodesTooltip("", TextColorPresets.normalizeGradientStops(preset.colors())),
                    preset -> matchesGradientPreset(state, preset.colors()),
                    preset -> {
                        state.load(PaintMode.GRADIENT, false, preset.colors());
                        rebuildAll.run();
                    },
                    preset -> {
                        state.editingGradientPresetId(preset.id());
                        state.load(PaintMode.GRADIENT, false, preset.colors());
                        rebuildAll.run();
                    },
                    ItemEditorText.tr("dialog.unified_color_picker.saved_gradient_edit_hint"),
                    (preset, direction) -> TextColorPresets.moveGradientPreset(preset.id(), direction),
                    preset -> TextColorPresets.removeGradientPreset(preset.id()),
                    ItemEditorText.tr("dialog.unified_color_picker.saved_gradient_remove_hint"),
                    refreshSaved
            );
        }
        if (options.showShadowPresets() && state.showShadowPresets()) {
            rowCount += rebuildSavedPresetGroup(
                    savedList,
                    TextColorPresets.customShadowPresets(),
                    "dialog.unified_color_picker.saved_shadows",
                    savedContentWidth,
                    SAVED_EDIT_ACTION_COUNT,
                    (preset, width) -> savedShadowButtonLabel(options.previewText(), preset.colors(), width),
                    preset -> colorCodesTooltip("Shadow ", TextColorPresets.normalizeShadowStopsOrDefault(preset.colors())),
                    preset -> matchesShadowPreset(state, preset.colors()),
                    preset -> {
                        state.loadShadowPreset(preset.colors());
                        rebuildAll.run();
                    },
                    preset -> {
                        state.editingShadowPresetId(preset.id());
                        state.loadShadowPreset(preset.colors());
                        rebuildAll.run();
                    },
                    ItemEditorText.tr("dialog.unified_color_picker.saved_shadow_edit_hint"),
                    (preset, direction) -> TextColorPresets.moveShadowPreset(preset.id(), direction),
                    preset -> TextColorPresets.removeShadowPreset(preset.id()),
                    ItemEditorText.tr("dialog.unified_color_picker.saved_shadow_remove_hint"),
                    refreshSaved
            );
        }
        if (rowCount == 0) {
            savedList.child(UiFactory.muted(ItemEditorText.tr("dialog.unified_color_picker.saved_empty"), savedContentWidth));
        }
    }

    private static <T> int rebuildSavedPresetGroup(
            FlowLayout savedList,
            List<T> presets,
            String titleKey,
            int savedContentWidth,
            int actionCount,
            BiFunction<T, Integer, Component> applyLabel,
            Function<T, Component> applyHint,
            Predicate<T> selected,
            Consumer<T> apply,
            Consumer<T> edit,
            Component editHint,
            BiConsumer<T, Integer> move,
            Consumer<T> remove,
            Component removeHint,
            Runnable refreshSaved
    ) {
        if (presets.isEmpty()) {
            return 0;
        }
        savedList.child(UiFactory.muted(savedGroupTitle(titleKey, presets.size()), savedContentWidth));
        int savedApplyButtonWidth = savedApplyButtonWidth(savedContentWidth, actionCount);
        for (int index = 0; index < presets.size(); index++) {
            T preset = presets.get(index);
            boolean selectedPreset = selected != null && selected.test(preset);
            savedList.child(ColorPickerUiUtil.savedPresetRow(
                    savedPresetLabel(applyLabel.apply(preset, savedApplyButtonWidth), selectedPreset),
                    applyHint == null ? Component.empty() : applyHint.apply(preset),
                    () -> apply.accept(preset),
                    edit == null ? null : () -> edit.accept(preset),
                    editHint,
                    index > 0,
                    () -> {
                        move.accept(preset, -1);
                        refreshSaved.run();
                    },
                    index < presets.size() - 1,
                    () -> {
                        move.accept(preset, 1);
                        refreshSaved.run();
                    },
                    () -> {
                        remove.accept(preset);
                        refreshSaved.run();
                    },
                    savedApplyButtonWidth,
                    SAVED_ACTION_BUTTON_WIDTH,
                    removeHint
            ));
        }
        return presets.size();
    }

    private static Component savedGroupTitle(String titleKey, int count) {
        return ItemEditorText.tr(titleKey).copy().append(Component.literal(" (" + count + ")"));
    }

    private static Component savedPresetLabel(Component label, boolean selected) {
        if (!selected) {
            return label;
        }
        return Component.literal("> ").append(label.copy());
    }

    private static boolean matchesColorPreset(PickerState state, int rgb) {
        return !state.shadow()
                && state.mode() == PaintMode.COLOR
                && (state.selectedRgb() & 0xFFFFFF) == (rgb & 0xFFFFFF);
    }

    private static boolean matchesGradientPreset(PickerState state, List<Integer> colors) {
        return !state.shadow()
                && state.mode() == PaintMode.GRADIENT
                && sameColors(state.resultColors(), TextColorPresets.normalizeGradientStops(colors));
    }

    private static boolean matchesShadowPreset(PickerState state, List<Integer> colors) {
        return state.shadow()
                && sameColors(
                TextColorPresets.normalizeShadowStopsOrDefault(state.resultColors()),
                TextColorPresets.normalizeShadowStopsOrDefault(colors)
        );
    }

    private static boolean sameColors(List<Integer> left, List<Integer> right) {
        if (left.size() != right.size()) {
            return false;
        }
        for (int index = 0; index < left.size(); index++) {
            if ((left.get(index) & 0xFFFFFF) != (right.get(index) & 0xFFFFFF)) {
                return false;
            }
        }
        return true;
    }

    private static void restoreScroll(ScrollContainer<?> scroll, double scrollOffset) {
        ScrollStateUtil.restore(scroll, scrollOffset);
        Minecraft.getInstance().execute(() -> ScrollStateUtil.restore(scroll, scrollOffset));
    }

    private static FlowLayout buildPreview(PickerState state, String previewText, int maxWidth) {
        FlowLayout row = UiFactory.row();
        row.gap(0);
        row.horizontalSizing(Sizing.fill(100));
        String text = normalizePreviewText(previewText);
        Component label;
        if (state.shadow()) {
            label = state.mode() == PaintMode.GRADIENT
                    ? shadowGradientLabel(text, state.resultColors())
                    : shadowLabel(text, state.selectedRgb());
        } else if (state.mode() == PaintMode.GRADIENT) {
            label = TextColorPresets.gradientLabel(text, state.resultColors());
        } else {
            label = Component.literal(text).withColor(state.selectedRgb());
        }
        LabelComponent previewLabel = UiFactory.bodyLabel(label).maxWidth(maxWidth);
        previewLabel.horizontalSizing(Sizing.fill(100));
        row.child(previewLabel);
        return row;
    }

    private static Component shadowLabel(String text, int rgb) {
        return Component.literal(text).withColor(0xFFFFFF).withStyle(style -> style.withShadowColor((rgb & 0xFFFFFF) | 0xFF000000));
    }

    private static Component shadowGradientLabel(String text, List<Integer> colors) {
        List<Integer> gradientColors = TextColorPresets.normalizeGradientStops(colors);
        MutableComponent root = Component.empty();
        if (text.isEmpty()) {
            return root;
        }

        int codePointCount = text.codePointCount(0, text.length());
        int colorIndex = 0;
        for (int index = 0; index < text.length();) {
            int codePoint = text.codePointAt(index);
            float progress = codePointCount == 1 ? 0f : (float) colorIndex / (codePointCount - 1);
            int shadowColor = me.noramibu.itemeditor.util.ColorInterpolationUtil.interpolateRgb(gradientColors, progress) | 0xFF000000;
            root.append(Component.literal(Character.toString(codePoint))
                    .withColor(0xFFFFFF)
                    .withStyle(style -> style.withShadowColor(shadowColor)));
            colorIndex++;
            index += Character.charCount(codePoint);
        }
        return root;
    }

    private static Component savedColorButtonLabel(String previewText, int rgb, int buttonWidth) {
        return Component.literal(fittedPresetPreviewText(previewText, buttonWidth)).withColor(rgb & 0xFFFFFF);
    }

    private static Component savedGradientButtonLabel(String previewText, List<Integer> colors, int buttonWidth) {
        List<Integer> normalized = TextColorPresets.normalizeGradientStops(colors);
        String fittedText = fittedPresetPreviewText(previewText, buttonWidth, normalized.size());
        return TextColorPresets.gradientLabel(fittedText, normalized);
    }

    private static Component savedShadowButtonLabel(String previewText, List<Integer> colors, int buttonWidth) {
        List<Integer> normalized = TextColorPresets.normalizeShadowStopsOrDefault(colors);
        String fittedText = fittedPresetPreviewText(previewText, buttonWidth, normalized.size() > 1 ? normalized.size() : 0);
        if (normalized.size() == 1) {
            return shadowLabel(fittedText, normalized.getFirst());
        }
        return shadowGradientLabel(fittedText, normalized);
    }

    private static String fittedPresetPreviewText(String previewText, int buttonWidth) {
        return fittedPresetPreviewText(previewText, buttonWidth, 0);
    }

    private static String fittedPresetPreviewText(String previewText, int buttonWidth, int colorCount) {
        String text = normalizePreviewText(previewText).replace('\n', ' ').trim();
        if (text.isBlank()) {
            text = SAMPLE_TEXT;
        }
        String suffix = colorCount > 1 ? " (" + colorCount + ")" : "";
        int textBudget = Math.max(SAVED_LABEL_MIN_WIDTH, buttonWidth - UiFactory.scaledPixels(SAVED_LABEL_TEXT_RESERVE));
        Minecraft minecraft = Minecraft.getInstance();
        String fullText = text + suffix;
        if (minecraft.font.width(fullText) <= textBudget) {
            return fullText;
        }

        String ellipsis = "...";
        int suffixWidth = minecraft.font.width(suffix);
        if (!suffix.isEmpty() && suffixWidth >= textBudget) {
            return suffix.trim();
        }
        int prefixBudget = Math.max(0, textBudget - suffixWidth - minecraft.font.width(ellipsis));
        String prefix = minecraft.font.plainSubstrByWidth(text, prefixBudget).trim();
        if (prefix.isEmpty()) {
            return suffix.isEmpty() ? ellipsis : suffix.trim();
        }
        return prefix + ellipsis + suffix;
    }

    private static Component colorCodesTooltip(String prefix, List<Integer> colors) {
        List<Integer> normalized = colors == null ? List.of() : colors;
        MutableComponent tooltip = Component.empty();
        if (prefix != null && !prefix.isBlank()) {
            tooltip.append(Component.literal(prefix));
        }
        if (normalized.size() > 1) {
            tooltip.append(Component.literal(normalized.size() + " colors: "));
        }
        for (int index = 0; index < normalized.size(); index++) {
            if (index > 0) {
                tooltip.append(Component.literal(" -> "));
            }
            int rgb = normalized.get(index) & 0xFFFFFF;
            tooltip.append(Component.literal(ValidationUtil.toHex(rgb)).withColor(rgb));
        }
        return tooltip;
    }

    private static String normalizePreviewText(String value) {
        if (value == null || value.isBlank()) {
            return SAMPLE_TEXT;
        }
        return value.replace("\r\n", "\n").replace('\r', '\n');
    }

    private static int savedApplyButtonWidth(int rowWidth, int actionCount) {
        int actionWidth = UiFactory.scaledPixels(SAVED_ACTION_BUTTON_WIDTH) * actionCount;
        int gaps = UiFactory.scaleProfile().spacing() * actionCount;
        return Math.max(32, rowWidth - actionWidth - gaps);
    }

    private static int rgbInputWidth(int pickerContentWidth) {
        int gaps = UiFactory.scaleProfile().spacing() * 2;
        int fieldExtras = UiFactory.scaledPixels(ColorPickerUiUtil.INPUT_BORDER_CLIP_PADDING) * 3;
        int edgePadding = UiFactory.scaledPixels(RGB_ROW_EDGE_PADDING);
        int maxInputWidth = Math.max(1, (pickerContentWidth - gaps - fieldExtras - edgePadding) / 3);
        int preferredWidth = pickerContentWidth < UiFactory.scaledPixels(RGB_INPUT_COMPACT_THRESHOLD)
                ? UiFactory.scaledPixels(RGB_INPUT_COMPACT_WIDTH)
                : UiFactory.scaledPixels(RGB_INPUT_WIDTH);
        int minimumWidth = UiFactory.scaledPixels(RGB_INPUT_MIN_WIDTH);
        if (maxInputWidth < minimumWidth) {
            return maxInputWidth;
        }
        return Math.max(minimumWidth, Math.min(preferredWidth, maxInputWidth));
    }

    private static int stopSelectButtonWidth(int rowWidth) {
        int actionWidth = UiFactory.scaledPixels(STOP_ROW_ACTION_WIDTH) * 3;
        int gaps = UiFactory.scaleProfile().spacing() * 3;
        return Math.max(24, rowWidth - actionWidth - gaps);
    }

    private static ButtonComponent modeButton(ModeButtonSpec spec, int width) {
        ButtonComponent button = ButtonFitUtil.fixedWidthFittedButton(
                spec.label(),
                UiFactory.ButtonTextPreset.STANDARD,
                width,
                MODE_BUTTON_TEXT_MIN_WIDTH,
                MODE_BUTTON_TEXT_RESERVE,
                ignored -> spec.action().run()
        );
        button.active(spec.active());
        return button;
    }

    private static boolean modeButtonsNeedStacking(int contentWidth, List<ModeButtonSpec> buttons) {
        int desiredWidth = UiFactory.scaleProfile().spacing() * Math.max(0, buttons.size() - 1);
        for (ModeButtonSpec button : buttons) {
            desiredWidth += modeButtonDesiredWidth(button.label());
        }
        return desiredWidth > contentWidth;
    }

    private static boolean modeControlsFitWithShadow(int contentWidth, List<ModeButtonSpec> buttons, Component shadowLabel) {
        if (shadowLabel == null || shadowLabel.getString().isBlank()) {
            return false;
        }
        int gaps = UiFactory.scaleProfile().spacing() * buttons.size();
        int desiredWidth = gaps + checkboxDesiredWidth(shadowLabel);
        for (ModeButtonSpec button : buttons) {
            desiredWidth += modeButtonDesiredWidth(button.label());
        }
        return desiredWidth <= contentWidth;
    }

    private static int checkboxDesiredWidth(Component label) {
        int textWidth = Minecraft.getInstance().font.width(label.getString());
        return textWidth + UiFactory.scaledPixels(MODE_CHECKBOX_TEXT_RESERVE);
    }

    private static int modeButtonWidth(int contentWidth, List<ModeButtonSpec> buttons) {
        if (buttons.size() <= 1 || modeButtonsNeedStacking(contentWidth, buttons)) {
            return Math.max(UiFactory.scaledPixels(MODE_BUTTON_MIN_WIDTH), contentWidth);
        }
        return Math.max(UiFactory.scaledPixels(MODE_BUTTON_MIN_WIDTH), (contentWidth - UiFactory.scaleProfile().spacing()) / buttons.size());
    }

    private static int modeButtonDesiredWidth(Component label) {
        int textWidth = Minecraft.getInstance().font.width(label.getString());
        return Math.max(UiFactory.scaledPixels(MODE_BUTTON_MIN_WIDTH), textWidth + UiFactory.scaledPixels(MODE_BUTTON_TEXT_RESERVE));
    }

    private static int pickerSize(int available) {
        int scaled = (int) Math.round(available * PICKER_SIZE_SCALE);
        return Math.max(PICKER_MIN_SIZE, Math.min(PICKER_MAX_SIZE, scaled));
    }

    private static Component selectedStopTitle(PickerState state) {
        return ItemEditorText.tr("dialog.unified_color_picker.selected_color", state.selectedIndex() + 1);
    }

    private static String stopButtonLabel(PickerState state, int index) {
        String prefix = state.selectedIndex() == index ? "> " : "";
        return prefix + ItemEditorText.str("dialog.unified_color_picker.stop_label", index + 1)
                + " " + ValidationUtil.toHex(state.color(index));
    }

    private static String applyKey(PickerState state) {
        if (state.shadow()) {
            return state.mode() == PaintMode.GRADIENT
                    ? "dialog.unified_color_picker.apply_shadow_gradient"
                    : "dialog.unified_color_picker.apply_shadow";
        }
        return state.mode() == PaintMode.GRADIENT
                ? "dialog.unified_color_picker.apply_gradient"
                : "dialog.unified_color_picker.apply_color";
    }

    private static String saveKey(PickerState state) {
        if (state.shadow()) {
            return state.mode() == PaintMode.GRADIENT
                    ? "dialog.unified_color_picker.save_shadow_gradient"
                    : "dialog.unified_color_picker.save_shadow_color";
        }
        return state.mode() == PaintMode.GRADIENT
                ? "dialog.unified_color_picker.save_gradient"
                : "dialog.unified_color_picker.save_color";
    }

    private static String stopsTitleKey(PickerState state) {
        return state.shadow()
                ? "dialog.unified_color_picker.shadow_gradient_title"
                : "dialog.unified_color_picker.stops_title";
    }

    public enum PaintMode {
        COLOR,
        GRADIENT
    }

    private record ModeButtonSpec(Component label, boolean active, Runnable action) {
    }

    private record SavedFilterSpec(Component label, boolean checked, Consumer<Boolean> onChanged) {
    }

    public record ColorPickerResult(PaintMode mode, boolean shadow, List<Integer> colors) {
        public ColorPickerResult {
            mode = mode == null ? PaintMode.COLOR : mode;
            colors = mode == PaintMode.GRADIENT
                    ? TextColorPresets.normalizeGradientStops(colors)
                    : normalizeColor(colors);
        }
    }

    public record Options(
            PaintMode initialMode,
            boolean initialShadow,
            List<Integer> initialColors,
            boolean allowColorMode,
            boolean allowGradientMode,
            boolean allowShadow,
            boolean showColorPresets,
            boolean showGradientPresets,
            boolean showShadowPresets,
            String previewText
    ) {
        public Options {
            if (!allowColorMode && !allowGradientMode) {
                allowColorMode = true;
            }
            initialMode = initialMode == null ? PaintMode.COLOR : initialMode;
            if (initialMode == PaintMode.COLOR && !allowColorMode) {
                initialMode = PaintMode.GRADIENT;
            }
            if (initialMode == PaintMode.GRADIENT && !allowGradientMode) {
                initialMode = PaintMode.COLOR;
            }
            initialShadow = allowShadow && initialShadow;
            initialColors = initialColors == null ? List.of(0xFFFFFF) : List.copyOf(initialColors);
            showColorPresets = showColorPresets && allowColorMode;
            showGradientPresets = showGradientPresets && allowGradientMode;
            showShadowPresets = showShadowPresets && allowShadow;
            previewText = normalizePreviewText(previewText);
        }

        public static Options plainColor(int initialRgb) {
            return new Options(
                    PaintMode.COLOR,
                    false,
                    List.of(initialRgb & 0xFFFFFF),
                    true,
                    false,
                    false,
                    true,
                    false,
                    false,
                    SAMPLE_TEXT
            );
        }

        public static Options richText(int initialRgb, List<Integer> initialGradientColors) {
            return richText(initialRgb, initialGradientColors, SAMPLE_TEXT);
        }

        public static Options richText(int initialRgb, List<Integer> initialGradientColors, String previewText) {
            List<Integer> colors = initialGradientColors == null || initialGradientColors.isEmpty()
                    ? List.of(initialRgb & 0xFFFFFF)
                    : TextColorPresets.normalizeGradientStops(initialGradientColors);
            return new Options(
                    PaintMode.COLOR,
                    false,
                    colors,
                    true,
                    true,
                    true,
                    true,
                    true,
                    true,
                    previewText
            );
        }
    }

    private static final class PickerState {
        private final List<Integer> colors = new ArrayList<>();
        private final RememberedColors normalColor = new RememberedColors();
        private final RememberedColors normalGradient = new RememberedColors();
        private final RememberedColors shadowColor = new RememberedColors();
        private final RememberedColors shadowGradient = new RememberedColors();
        private PaintMode mode;
        private boolean shadow;
        private boolean showColorPresets;
        private boolean showGradientPresets;
        private boolean showShadowPresets;
        private int selectedIndex;
        private String editingGradientPresetId = "";
        private String editingShadowPresetId = "";

        private PickerState(Options options) {
            this.mode = options.initialMode();
            this.shadow = options.initialShadow();
            this.showColorPresets = options.showColorPresets();
            this.showGradientPresets = options.showGradientPresets();
            this.showShadowPresets = options.showShadowPresets();
            this.replace(options.initialColors());
        }

        private PaintMode mode() {
            return this.mode;
        }

        private void setMode(PaintMode mode) {
            PaintMode nextMode = mode == null ? PaintMode.COLOR : mode;
            if (this.mode == nextMode) {
                return;
            }
            this.rememberCurrentColors();
            List<Integer> fallbackColors = this.fallbackColorsFor(nextMode);
            int fallbackSelectedIndex = this.fallbackSelectedIndexFor(nextMode);
            this.mode = nextMode;
            this.clearInactiveEditIds();
            this.restoreRememberedOrFallback(fallbackColors, fallbackSelectedIndex);
        }

        private void ensureAllowedMode(Options options) {
            PaintMode allowedMode = this.mode;
            if (this.mode == PaintMode.COLOR && !options.allowColorMode()) {
                allowedMode = PaintMode.GRADIENT;
            }
            if (this.mode == PaintMode.GRADIENT && !options.allowGradientMode()) {
                allowedMode = PaintMode.COLOR;
            }
            boolean allowedShadow = options.allowShadow() && this.shadow;
            if (allowedMode == this.mode && allowedShadow == this.shadow) {
                return;
            }
            this.rememberCurrentColors();
            List<Integer> fallbackColors = this.fallbackColorsFor(allowedMode);
            int fallbackSelectedIndex = this.fallbackSelectedIndexFor(allowedMode);
            this.mode = allowedMode;
            this.shadow = allowedShadow;
            this.clearInactiveEditIds();
            this.restoreRememberedOrFallback(fallbackColors, fallbackSelectedIndex);
        }

        private boolean shadow() {
            return this.shadow;
        }

        private void shadow(boolean shadow) {
            if (this.shadow == shadow) {
                return;
            }
            this.rememberCurrentColors();
            List<Integer> fallbackColors = List.copyOf(this.colors);
            int fallbackSelectedIndex = this.selectedIndex;
            this.shadow = shadow;
            this.clearInactiveEditIds();
            this.restoreRememberedOrFallback(fallbackColors, fallbackSelectedIndex);
        }

        private boolean showColorPresets() {
            return this.showColorPresets;
        }

        private void showColorPresets(boolean showColorPresets) {
            this.showColorPresets = showColorPresets;
        }

        private boolean showGradientPresets() {
            return this.showGradientPresets;
        }

        private void showGradientPresets(boolean showGradientPresets) {
            this.showGradientPresets = showGradientPresets;
        }

        private boolean showShadowPresets() {
            return this.showShadowPresets;
        }

        private void showShadowPresets(boolean showShadowPresets) {
            this.showShadowPresets = showShadowPresets;
        }

        private String editingGradientPresetId() {
            return this.editingGradientPresetId;
        }

        private void editingGradientPresetId(String editingGradientPresetId) {
            this.editingGradientPresetId = editingGradientPresetId == null ? "" : editingGradientPresetId;
        }

        private String editingShadowPresetId() {
            return this.editingShadowPresetId;
        }

        private void editingShadowPresetId(String editingShadowPresetId) {
            this.editingShadowPresetId = editingShadowPresetId == null ? "" : editingShadowPresetId;
        }

        private ColorPickerResult result() {
            return new ColorPickerResult(this.mode, this.shadow, this.resultColors());
        }

        private List<Integer> resultColors() {
            return this.mode == PaintMode.GRADIENT
                    ? TextColorPresets.normalizeGradientStops(this.colors)
                    : List.of(this.selectedRgb());
        }

        private int selectedIndex() {
            return this.selectedIndex;
        }

        private int selectedRgb() {
            return this.colors.get(this.selectedIndex);
        }

        private int color(int index) {
            return this.colors.get(index);
        }

        private int size() {
            return this.colors.size();
        }

        private int minimumStops() {
            return this.mode == PaintMode.GRADIENT ? 2 : 1;
        }

        private void setSelectedRgb(int rgb) {
            this.colors.set(this.selectedIndex, rgb & 0xFFFFFF);
            if (!this.shadow) {
                this.editingShadowPresetId = "";
            }
            if (this.mode != PaintMode.GRADIENT) {
                this.editingGradientPresetId = "";
            }
            this.rememberCurrentColors();
        }

        private void select(int index) {
            this.selectedIndex = Math.max(0, Math.min(index, this.colors.size() - 1));
        }

        private void addAfterSelected() {
            int insertIndex = Math.min(this.colors.size(), this.selectedIndex + 1);
            this.colors.add(insertIndex, TextColorPresets.gradientEndFor(this.selectedRgb()));
            this.selectedIndex = insertIndex;
            this.rememberCurrentColors();
        }

        private void remove(int index) {
            if (this.colors.size() <= this.minimumStops()) {
                return;
            }
            this.colors.remove(index);
            this.select(Math.min(index, this.colors.size() - 1));
            this.rememberCurrentColors();
        }

        private void removeSelected() {
            this.remove(this.selectedIndex);
        }

        private void move(int from, int to) {
            if (from < 0 || from >= this.colors.size() || to < 0 || to >= this.colors.size() || from == to) {
                return;
            }
            int color = this.colors.remove(from);
            this.colors.add(to, color);
            this.selectedIndex = to;
            this.rememberCurrentColors();
        }

        private void load(PaintMode mode, boolean shadow, List<Integer> colors) {
            this.mode = mode == null ? PaintMode.COLOR : mode;
            this.shadow = shadow;
            this.clearInactiveEditIds();
            this.replace(colors);
        }

        private void loadShadowPreset(List<Integer> colors) {
            List<Integer> normalized = TextColorPresets.normalizeShadowStopsOrDefault(colors);
            this.load(normalized.size() > 1 ? PaintMode.GRADIENT : PaintMode.COLOR, true, normalized);
        }

        private void replace(List<Integer> colors) {
            this.replaceWithoutRemembering(colors);
            this.rememberCurrentColors();
        }

        private void replaceWithoutRemembering(List<Integer> colors) {
            this.colors.clear();
            this.colors.addAll(this.mode == PaintMode.GRADIENT
                    ? TextColorPresets.normalizeGradientStops(colors)
                    : normalizeColor(colors));
            this.select(Math.min(this.selectedIndex, this.colors.size() - 1));
        }

        private void rememberCurrentColors() {
            RememberedColors target = this.rememberedColors(this.shadow, this.mode);
            target.colors.clear();
            target.colors.addAll(this.colors);
            target.selectedIndex = this.selectedIndex;
            target.remembered = true;
        }

        private void restoreRememberedOrFallback(List<Integer> fallbackColors, int fallbackSelectedIndex) {
            RememberedColors target = this.rememberedColors(this.shadow, this.mode);
            if (target.remembered) {
                this.replaceWithoutRemembering(target.colors);
                this.select(target.selectedIndex);
                return;
            }
            this.replaceWithoutRemembering(fallbackColors);
            this.select(fallbackSelectedIndex);
            this.rememberCurrentColors();
        }

        private List<Integer> fallbackColorsFor(PaintMode targetMode) {
            if (this.colors.isEmpty()) {
                return List.of(0xFFFFFF);
            }
            if (targetMode == PaintMode.COLOR) {
                return List.of(this.selectedRgb());
            }
            return this.mode == PaintMode.GRADIENT
                    ? List.copyOf(this.colors)
                    : List.of(this.selectedRgb());
        }

        private int fallbackSelectedIndexFor(PaintMode targetMode) {
            return targetMode == PaintMode.GRADIENT && this.mode == PaintMode.GRADIENT
                    ? this.selectedIndex
                    : 0;
        }

        private RememberedColors rememberedColors(boolean shadow, PaintMode mode) {
            if (shadow) {
                return mode == PaintMode.GRADIENT ? this.shadowGradient : this.shadowColor;
            }
            return mode == PaintMode.GRADIENT ? this.normalGradient : this.normalColor;
        }

        private void clearInactiveEditIds() {
            if (!this.shadow) {
                this.editingShadowPresetId = "";
            }
            if (this.mode != PaintMode.GRADIENT || this.shadow) {
                this.editingGradientPresetId = "";
            }
        }

        private static final class RememberedColors {
            private final List<Integer> colors = new ArrayList<>();
            private int selectedIndex;
            private boolean remembered;
        }
    }

    private static List<Integer> normalizeColor(List<Integer> colors) {
        if (colors != null) {
            for (Integer color : colors) {
                if (color != null) {
                    return List.of(color & 0xFFFFFF);
                }
            }
        }
        return List.of(0xFFFFFF);
    }
}
