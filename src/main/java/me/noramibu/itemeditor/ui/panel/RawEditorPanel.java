package me.noramibu.itemeditor.ui.panel;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.DiscreteSliderComponent;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Color;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.UIComponent;
import me.noramibu.itemeditor.editor.ValidationMessage;
import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.storage.RawEditorOptionsService;
import me.noramibu.itemeditor.storage.model.RawEditorOptions;
import me.noramibu.itemeditor.ui.component.RawTextAreaComponent;
import me.noramibu.itemeditor.ui.component.SafeDiscreteSliderComponent;
import me.noramibu.itemeditor.ui.component.UiFactory;
import me.noramibu.itemeditor.ui.component.raw.RawAutocompleteController;
import me.noramibu.itemeditor.ui.screen.ItemEditorScreen;
import me.noramibu.itemeditor.ui.util.LayoutModeUtil;
import me.noramibu.itemeditor.ui.util.UiColors;
import me.noramibu.itemeditor.util.ItemEditorText;
import me.noramibu.itemeditor.util.LootTableIds;
import me.noramibu.itemeditor.util.RawItemDataUtil;
import me.noramibu.itemeditor.util.RawValidationAsyncService;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class RawEditorPanel implements EditorPanel {
    private static final int EDITOR_MIN_HEIGHT = 180;
    private static final String STATUS_VALIDATING_TEXT = "Validating...";
    private static final String STATUS_EMPTY_TEXT = " ";
    private static final int LARGE_TEXT_THRESHOLD = 12000;
    private static final int VERY_LARGE_TEXT_THRESHOLD = 350000;
    private static final long RAW_VALIDATION_PARSE_IDLE_BASE_MS = 180L;
    private static final long RAW_VALIDATION_PARSE_IDLE_LARGE_MS = 380L;
    private static final long RAW_VALIDATION_PARSE_IDLE_VERY_LARGE_MS = 700L;
    private static final long RAW_VALIDATION_PARSE_IDLE_INCOMPLETE_MS = 950L;
    private static final long RAW_VALIDATION_IDLE_BASE_MS = 350L;
    private static final long RAW_VALIDATION_IDLE_LARGE_MS = 700L;
    private static final long RAW_VALIDATION_IDLE_VERY_LARGE_MS = 1300L;
    private static final long RAW_VALIDATION_IDLE_INCOMPLETE_MS = 1900L;
    private static final int MAX_STATUS_ERROR_LENGTH = 120;
    private static final int MAX_VALIDATION_ERROR_LENGTH = 320;
    private static final Pattern MISSING_KEY_PATTERN = Pattern.compile(
            "No\\s+key\\s+\"?([A-Za-z0-9_:\\-.]+)\"?\\s+in\\s+MapLike\\[\\{\\s*\"?([A-Za-z0-9_:\\-.]+)\"?\\s*:",
            Pattern.CASE_INSENSITIVE
    );
    private static final int PANEL_SCROLLBAR_BASE_THICKNESS = 8;
    private static final int CONTROL_COMPACT_WIDTH_THRESHOLD = 980;
    private static final int CONTROL_STACK_WIDTH_HINT = 420;
    private static final int ACTION_ROW_BUTTON_COUNT = 3;
    private static final double CONTROL_COMPACT_HEIGHT_RATIO = 0.78d;
    private static final int CONTROL_SECONDARY_BUTTON_MIN = 62;
    private static final int CONTROL_SECONDARY_BUTTON_MAX = 108;
    private static final int CONTROL_SECONDARY_BUTTON_DIVISOR = 7;
    private static final int CONTROL_UNDO_REDO_BUTTON_MIN = 84;
    private static final int CONTROL_UNDO_REDO_BUTTON_MAX = 142;
    private static final int CONTROL_UNDO_REDO_BUTTON_DIVISOR = 5;
    private static final int FONT_SIZE_PERCENT_MIN = 1;
    private static final int FONT_SIZE_PERCENT_MAX = 500;
    private static final int SECTION_RIGHT_SAFETY_PADDING_BASE = 12;
    private static final int ACTION_BUTTON_WIDTH_MIN = 78;
    private static final int ACTION_BUTTON_WIDTH_BASE = 96;
    private static final int ACTION_BUTTON_TEXT_WIDTH_MIN = 24;
    private static final int ACTION_BUTTON_TEXT_HORIZONTAL_RESERVE = 10;
    private static final int ACTION_BUTTON_TEXT_NON_COMPACT_RESERVE = 16;
    private static final int STATUS_LINE_COUNT = 3;
    private static final int STATUS_TEXT_SAFETY_PADDING = 4;
    private static final int OPTIONS_INLINE_WIDTH_THRESHOLD = 620;
    private static final int EDITOR_HEIGHT_SAFETY_PADDING = 4;

    private final ItemEditorScreen screen;

    public RawEditorPanel(ItemEditorScreen screen) {
        this.screen = screen;
    }

    @Override
    public UIComponent build() {
        ItemEditorState state = this.screen.session().state();
        this.ensureRawEditorOptionsLoaded(state);
        if (state.rawEditorText == null || state.rawEditorText.isBlank()) {
            state.rawEditorText = RawItemDataUtil.serialize(
                    this.screen.session().previewStack(),
                    this.screen.session().registryAccess(),
                    state.rawEditorShowDefaults
            );
        }

        FlowLayout root = UiFactory.column();
        FlowLayout section = UiFactory.column();
        int rightSafetyPadding = UiFactory.scaledPixels(SECTION_RIGHT_SAFETY_PADDING_BASE);
        section.padding(Insets.right(rightSafetyPadding));
        int editorWidthHint = this.screen.editorContentWidthHint();
        ControlLayout controlLayout = this.resolveControlLayout(editorWidthHint);
        int editorHeight = this.resolveEditorHeight(
                this.screen.editorContentHeightHint(),
                controlLayout,
                state.uiRawEditorOptionsExpanded,
                editorWidthHint
        );

        RawTextAreaComponent editor = new RawTextAreaComponent(Sizing.fill(100), Sizing.fixed(editorHeight), state.rawEditorText);
        editor.displayCharCount(false);
        editor.wordWrap(state.rawEditorWordWrap);
        editor.horizontalScroll(state.rawEditorHorizontalScroll);
        editor.fontSizePercent(state.rawEditorFontSizePercent);
        this.restoreEditorUiState(state, editor);
        RawValidationAsyncService validationService = new RawValidationAsyncService();
        RawAutocompleteController autocomplete = new RawAutocompleteController(
                state,
                editor,
                () -> this.screen.session().registryAccess(),
                this::currentContextItemId,
                () -> LootTableIds.fromResources(this.screen.session().minecraft().getResourceManager()),
                () -> this.persistRawEditorOptions(state)
        );
        int[] activeLine = new int[]{editor.caretLine()};
        int[] errorLine = new int[]{-1};
        int[] errorColumn = new int[]{-1};

        ButtonComponent optionsButton = this.rawActionButton(
                ItemEditorText.tr("raw_editor.options"),
                controlLayout.compactControls(),
                controlLayout.secondaryButtonWidth(),
                controlLayout.actionButtonHeight(),
                controlLayout.stackActionRows(),
                UiFactory.ButtonTextPreset.STANDARD,
                button -> {
                    state.uiRawEditorOptionsExpanded = !state.uiRawEditorOptionsExpanded;
                    this.screen.refreshCurrentPanel();
                }
        );
        ButtonComponent undoButton = this.rawActionButton(
                ItemEditorText.tr("raw_editor.undo"),
                true,
                controlLayout.undoRedoButtonWidth(),
                controlLayout.undoRedoButtonHeight(),
                false,
                UiFactory.ButtonTextPreset.COMPACT,
                button -> editor.undo()
        );
        ButtonComponent redoButton = this.rawActionButton(
                ItemEditorText.tr("raw_editor.redo"),
                true,
                controlLayout.undoRedoButtonWidth(),
                controlLayout.undoRedoButtonHeight(),
                false,
                UiFactory.ButtonTextPreset.COMPACT,
                button -> editor.redo()
        );

        if (controlLayout.stackActionRows()) {
            FlowLayout optionsHeader = UiFactory.column();
            optionsHeader.child(optionsButton);
            section.child(optionsHeader);

            section.child(UiFactory.actionButtonRow(false, undoButton, redoButton));
        } else {
            FlowLayout topActions = UiFactory.row();
            topActions.child(optionsButton);
            topActions.child(undoButton);
            topActions.child(redoButton);
            section.child(topActions);
        }

        if (state.uiRawEditorOptionsExpanded) {
            FlowLayout optionsPanel = UiFactory.subCard();
            boolean inlineOptions = editorWidthHint >= UiFactory.scaledPixels(OPTIONS_INLINE_WIDTH_THRESHOLD);
            UIComponent wordWrapCheckbox = UiFactory.checkbox(
                    ItemEditorText.tr("raw_editor.word_wrap"),
                    state.rawEditorWordWrap,
                    checked -> {
                        if (state.rawEditorWordWrap == checked) {
                            return;
                        }
                        state.rawEditorWordWrap = checked;
                        state.rawEditorHorizontalScroll = !checked;
                        editor.wordWrap(checked);
                        editor.horizontalScroll(!checked);
                        this.persistRawEditorOptions(state);
                    }
            );
            UIComponent showDefaultsCheckbox = UiFactory.checkbox(
                    ItemEditorText.tr("raw_editor.show_defaults"),
                    state.rawEditorShowDefaults,
                    checked -> {
                        if (state.rawEditorShowDefaults == checked) {
                            return;
                        }
                        state.rawEditorShowDefaults = checked;
                        this.persistRawEditorOptions(state);
                        this.setRawText(
                                state,
                                RawItemDataUtil.format(
                                        state.rawEditorText,
                                        this.screen.session().registryAccess(),
                                        state.rawEditorShowDefaults
                                )
                        );
                    }
            );
            UIComponent autocompleteCheckbox = UiFactory.checkbox(
                    ItemEditorText.tr("raw_editor.disable_autocomplete"),
                    state.rawEditorAutocompleteDisabled,
                    checked -> {
                        if (state.rawEditorAutocompleteDisabled == checked) {
                            return;
                        }
                        autocomplete.setDisabled(checked);
                    }
            );
            optionsPanel.child(this.optionGroup(inlineOptions, wordWrapCheckbox, showDefaultsCheckbox, autocompleteCheckbox));

            int fontLabelWidth = Math.max(120, editorWidthHint - UiFactory.scaledPixels(20));
            LabelComponent fontSizeLabel = UiFactory.muted(
                    Component.literal(ItemEditorText.str("raw_editor.font_size.current", state.rawEditorFontSizePercent)),
                    fontLabelWidth
            );
            optionsPanel.child(fontSizeLabel);
            DiscreteSliderComponent fontSizeSlider = new SafeDiscreteSliderComponent(Sizing.fill(100), FONT_SIZE_PERCENT_MIN, FONT_SIZE_PERCENT_MAX);
            fontSizeSlider.decimalPlaces(0).snap(true);
            fontSizeSlider.setFromDiscreteValue(state.rawEditorFontSizePercent);
            fontSizeSlider.onChanged().subscribe(value -> {
                int clamped = Math.clamp((int) Math.round(value), FONT_SIZE_PERCENT_MIN, FONT_SIZE_PERCENT_MAX);
                if (state.rawEditorFontSizePercent == clamped) {
                    return;
                }
                state.rawEditorFontSizePercent = clamped;
                editor.fontSizePercent(clamped);
                fontSizeLabel.text(Component.literal(ItemEditorText.str("raw_editor.font_size.current", clamped)));
                this.persistRawEditorOptions(state);
                this.persistEditorUiState(state, editor);
            });
            optionsPanel.child(fontSizeSlider);

            int availableHintWidth = Math.max(120, editorWidthHint - UiFactory.scaledPixels(20));
            int hintWidth = inlineOptions
                    ? Math.max(120, (availableHintWidth - UiFactory.scaleProfile().spacing()) / 2)
                    : availableHintWidth;
            optionsPanel.child(this.optionGroup(
                    inlineOptions,
                    UiFactory.muted(ItemEditorText.tr("raw_editor.autocomplete.hint.navigate"), hintWidth),
                    UiFactory.muted(ItemEditorText.tr("raw_editor.autocomplete.hint.accept"), hintWidth)
            ));

            ButtonComponent formatButton = this.rawActionButton(
                    ItemEditorText.tr("dialog.apply.raw.format"),
                    controlLayout.compactControls(),
                    controlLayout.secondaryButtonWidth(),
                    controlLayout.actionButtonHeight(),
                    controlLayout.stackActionRows(),
                    UiFactory.ButtonTextPreset.STANDARD,
                    button -> this.setRawText(
                            state,
                            RawItemDataUtil.format(
                                    state.rawEditorText,
                                    this.screen.session().registryAccess(),
                                    state.rawEditorShowDefaults
                            )
                    )
            );
            ButtonComponent minifyButton = this.rawActionButton(
                    ItemEditorText.tr("dialog.apply.raw.minify"),
                    controlLayout.compactControls(),
                    controlLayout.secondaryButtonWidth(),
                    controlLayout.actionButtonHeight(),
                    controlLayout.stackActionRows(),
                    UiFactory.ButtonTextPreset.STANDARD,
                    button -> this.setRawText(state, RawItemDataUtil.minify(state.rawEditorText))
            );
            optionsPanel.child(UiFactory.actionButtonRow(false, formatButton, minifyButton));
            section.child(optionsPanel);
        }

        section.child(editor);

        int safeEditorWidthHint = Math.max(1, editorWidthHint);
        int statusWidth = Math.max(
                1,
                safeEditorWidthHint - UiFactory.scrollContentInset(PANEL_SCROLLBAR_BASE_THICKNESS) - rightSafetyPadding
        );
        LabelComponent parseStatus = this.statusLabel(statusWidth);
        LabelComponent caretStatus = this.statusLabel(statusWidth);
        LabelComponent diffStatus = this.statusLabel(statusWidth);

        section.child(parseStatus);
        section.child(caretStatus);
        section.child(diffStatus);

        Runnable requestValidation = () -> {
            String rawText = editor.getValue();
            boolean likelyIncomplete = this.isLikelyIncompleteRawState(rawText);
            int rawTextLength = rawText.length();
            long parseIdleDelay = this.resolveValidationDelay(rawTextLength, likelyIncomplete, RAW_VALIDATION_PARSE_IDLE_BASE_MS, RAW_VALIDATION_PARSE_IDLE_LARGE_MS, RAW_VALIDATION_PARSE_IDLE_VERY_LARGE_MS, RAW_VALIDATION_PARSE_IDLE_INCOMPLETE_MS);
            long heavyIdleDelay = this.resolveValidationDelay(rawTextLength, likelyIncomplete, RAW_VALIDATION_IDLE_BASE_MS, RAW_VALIDATION_IDLE_LARGE_MS, RAW_VALIDATION_IDLE_VERY_LARGE_MS, RAW_VALIDATION_IDLE_INCOMPLETE_MS);
            validationService.requestTwoPhase(
                    rawText,
                    this.screen.session().originalStack(),
                    this.screen.session().registryAccess(),
                    parseIdleDelay,
                    heavyIdleDelay,
                    parseResult -> {
                        if (!parseResult.success()) {
                            this.screen.session().cancelQueuedRebuild();
                            this.applyParseFailure(
                                    ParseFailure.from(parseResult),
                                    parseStatus,
                                    diffStatus,
                                    statusWidth,
                                    editor,
                                    errorLine,
                                    errorColumn
                            );
                            return;
                        }

                        errorLine[0] = -1;
                        errorColumn[0] = -1;
                        this.setStatusText(parseStatus, ItemEditorText.tr("raw_editor.status.valid"), UiColors.SUCCESS, statusWidth);
                        editor.setErrorLocation(-1, -1, 0);
                        this.setStatusText(diffStatus, Component.literal(STATUS_VALIDATING_TEXT), UiColors.MUTED, statusWidth);
                        this.screen.session().queueRebuildPreview(0L);
                    },
                    result -> {
                        if (!result.success()) {
                            this.applyParseFailure(
                                    ParseFailure.from(result),
                                    parseStatus,
                                    diffStatus,
                                    statusWidth,
                                    editor,
                                    errorLine,
                                    errorColumn
                            );
                            return;
                        }

                        if (result.diffError() != null) {
                            this.setStatusText(
                                    diffStatus,
                                    Component.literal(ItemEditorText.str("dialog.apply.diff_failed", result.diffError())),
                                    UiColors.DANGER,
                                    statusWidth
                            );
                        } else {
                            String diffText = result.diffEntries() == 0
                                    ? ItemEditorText.str("raw_editor.status.no_changes")
                                    : ItemEditorText.str("raw_editor.status.changes", result.diffEntries());
                            this.setStatusText(diffStatus, Component.literal(diffText), UiColors.MUTED, statusWidth);
                        }
                    }
            );
        };

        Runnable refreshHistoryButtons = () -> {
            undoButton.active(editor.canUndo());
            redoButton.active(editor.canRedo());
        };

        editor.onAutocompleteRequested(autocomplete::applySelected);
        editor.onAutocompleteRefreshRequested(autocomplete::force);
        editor.onAutocompleteDismissed(autocomplete::dismiss);
        editor.onAutocompleteNextRequested(() -> autocomplete.moveSelection(1));
        editor.onAutocompletePreviousRequested(() -> autocomplete.moveSelection(-1));
        editor.onHistoryChanged(() -> {
            refreshHistoryButtons.run();
            this.persistEditorUiState(state, editor);
        });

        editor.onChanged().subscribe((text, delta) -> {
            state.rawEditorText = text;
            state.rawEditorEdited = true;
            this.screen.session().cancelQueuedRebuild();
            activeLine[0] = editor.caretLine();
            autocomplete.onChanged(delta);
            requestValidation.run();
            this.persistEditorUiState(state, editor);
        });

        editor.onViewportChanged(() -> {
            int caretLine = editor.caretLine();
            this.setStatusText(
                    caretStatus,
                    Component.literal(ItemEditorText.str("raw_editor.caret", caretLine, editor.caretColumn())),
                    UiColors.MUTED,
                    statusWidth
            );
            activeLine[0] = caretLine;

            autocomplete.onViewportChanged();
            this.persistEditorUiState(state, editor);
        });

        refreshHistoryButtons.run();
        requestValidation.run();
        autocomplete.request();
        this.setStatusText(
                caretStatus,
                Component.literal(ItemEditorText.str("raw_editor.caret", activeLine[0], editor.caretColumn())),
                UiColors.MUTED,
                statusWidth
        );
        this.persistEditorUiState(state, editor);

        UiFactory.appendFillChild(root, section);
        return root;
    }

    private ButtonComponent rawActionButton(
            Component label,
            boolean compact,
            int fixedWidth,
            int fixedHeight,
            boolean fillWidth,
            UiFactory.ButtonTextPreset preset,
            Consumer<ButtonComponent> onPress
    ) {
        int width = fixedWidth > 0 ? fixedWidth : Math.max(ACTION_BUTTON_WIDTH_MIN, UiFactory.scaledPixels(ACTION_BUTTON_WIDTH_BASE));
        int textReserve = compact ? ACTION_BUTTON_TEXT_HORIZONTAL_RESERVE : ACTION_BUTTON_TEXT_NON_COMPACT_RESERVE;
        int textWidth = Math.max(ACTION_BUTTON_TEXT_WIDTH_MIN, width - UiFactory.scaledPixels(textReserve));
        Component fitted = UiFactory.fitToWidth(label, textWidth);
        ButtonComponent button = UiFactory.button(fitted, preset, onPress);
        if (compact || !fitted.getString().equals(label.getString())) {
            button.tooltip(List.of(label));
        }
        if (compact) {
            button.verticalSizing(Sizing.fixed(fixedHeight));
            button.horizontalSizing(fillWidth ? Sizing.fill(100) : Sizing.fixed(width));
            return button;
        }
        button.horizontalSizing(Sizing.fixed(width));
        return button;
    }

    private void setRawText(ItemEditorState state, String text) {
        state.rawEditorText = text;
        state.rawEditorEdited = true;
        this.clearEditorUiState(state);
        this.screen.session().rebuildPreview();
        this.screen.refreshCurrentPanel();
    }

    private void ensureRawEditorOptionsLoaded(ItemEditorState state) {
        if (state.rawEditorOptionsLoaded) {
            state.rawEditorHorizontalScroll = !state.rawEditorWordWrap;
            return;
        }

        RawEditorOptions options = RawEditorOptionsService.instance().load();
        state.rawEditorShowDefaults = options.showDefaultKeys;
        state.rawEditorAutocompleteDisabled = options.autocompleteDisabled;
        state.rawEditorWordWrap = options.wordWrap;
        state.rawEditorHorizontalScroll = !state.rawEditorWordWrap;
        state.rawEditorFontSizePercent = options.fontSizePercent;
        if (state.rawEditorFontSizePercent <= 0) {
            state.rawEditorFontSizePercent = 100;
        }
        state.rawEditorOptionsLoaded = true;
    }

    private void persistRawEditorOptions(ItemEditorState state) {
        RawEditorOptions options = new RawEditorOptions();
        options.wordWrap = state.rawEditorWordWrap;
        options.showDefaultKeys = state.rawEditorShowDefaults;
        options.autocompleteDisabled = state.rawEditorAutocompleteDisabled;
        options.fontSizePercent = state.rawEditorFontSizePercent;
        RawEditorOptionsService.instance().save(options);
    }

    private void restoreEditorUiState(ItemEditorState state, RawTextAreaComponent editor) {
        List<RawTextAreaComponent.HistorySnapshot> undoHistory = this.toHistorySnapshots(state.uiRawEditorUndoHistory);
        List<RawTextAreaComponent.HistorySnapshot> redoHistory = this.toHistorySnapshots(state.uiRawEditorRedoHistory);
        editor.restoreEditorState(
                state.uiRawEditorCursor,
                state.uiRawEditorSelectionCursor,
                state.uiRawEditorScrollAmount,
                undoHistory,
                redoHistory
        );
    }

    private void persistEditorUiState(ItemEditorState state, RawTextAreaComponent editor) {
        state.uiRawEditorCursor = editor.caretIndex();
        state.uiRawEditorSelectionCursor = editor.selectionIndex();
        state.uiRawEditorScrollAmount = editor.scrollOffset();
        this.writeHistorySnapshots(state.uiRawEditorUndoHistory, editor.undoHistorySnapshot());
        this.writeHistorySnapshots(state.uiRawEditorRedoHistory, editor.redoHistorySnapshot());
    }

    private void clearEditorUiState(ItemEditorState state) {
        state.uiRawEditorCursor = 0;
        state.uiRawEditorSelectionCursor = 0;
        state.uiRawEditorScrollAmount = 0d;
        state.uiRawEditorUndoHistory.clear();
        state.uiRawEditorRedoHistory.clear();
    }

    private ControlLayout resolveControlLayout(int editorWidthHint) {
        int scaledWidth = this.screen.session().minecraft().getWindow().getGuiScaledWidth();
        double guiScale = this.screen.session().minecraft().getWindow().getGuiScale();
        boolean compactControls = LayoutModeUtil.isCompactScale(
                guiScale,
                LayoutModeUtil.DEFAULT_COMPACT_LAYOUT_SCALE_THRESHOLD
        ) || scaledWidth < CONTROL_COMPACT_WIDTH_THRESHOLD;
        int nonCompactButtonWidth = UiFactory.scaledPixels(ACTION_BUTTON_WIDTH_BASE);
        int nonCompactActionRowMinWidth = (nonCompactButtonWidth * ACTION_ROW_BUTTON_COUNT) + UiFactory.scaleProfile().spacing();
        int staticStackThreshold = UiFactory.scaledPixels(CONTROL_STACK_WIDTH_HINT);
        int stackThreshold = Math.max(nonCompactActionRowMinWidth, staticStackThreshold);
        boolean stackActionRows = compactControls || editorWidthHint < stackThreshold;
        int baseControlHeight = UiFactory.scaleProfile().controlHeight();
        int compactControlHeight = Math.max(12, (int) Math.round(baseControlHeight * CONTROL_COMPACT_HEIGHT_RATIO));
        int actionButtonHeight = compactControls ? compactControlHeight : baseControlHeight;
        int secondaryButtonWidth = compactControls
                ? Math.clamp(scaledWidth / CONTROL_SECONDARY_BUTTON_DIVISOR, CONTROL_SECONDARY_BUTTON_MIN, CONTROL_SECONDARY_BUTTON_MAX)
                : -1;
        int undoRedoButtonWidth = Math.clamp(
                scaledWidth / CONTROL_UNDO_REDO_BUTTON_DIVISOR,
                CONTROL_UNDO_REDO_BUTTON_MIN,
                CONTROL_UNDO_REDO_BUTTON_MAX
        );
        return new ControlLayout(
                compactControls,
                stackActionRows,
                actionButtonHeight,
                compactControlHeight,
                secondaryButtonWidth,
                undoRedoButtonWidth
        );
    }

    private record ControlLayout(
            boolean compactControls,
            boolean stackActionRows,
            int actionButtonHeight,
            int undoRedoButtonHeight,
            int secondaryButtonWidth,
            int undoRedoButtonWidth
    ) {
    }

    private List<RawTextAreaComponent.HistorySnapshot> toHistorySnapshots(List<ItemEditorState.RawEditorHistoryEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return List.of();
        }
        return entries.stream()
                .filter(Objects::nonNull)
                .map(entry -> new RawTextAreaComponent.HistorySnapshot(
                        entry.text,
                        entry.cursor,
                        entry.selection,
                        entry.scroll
                ))
                .toList();
    }

    private void writeHistorySnapshots(
            List<ItemEditorState.RawEditorHistoryEntry> target,
            List<RawTextAreaComponent.HistorySnapshot> snapshots
    ) {
        target.clear();
        if (snapshots == null || snapshots.isEmpty()) {
            return;
        }
        for (RawTextAreaComponent.HistorySnapshot snapshot : snapshots) {
            target.add(ItemEditorState.RawEditorHistoryEntry.of(
                    snapshot.text(),
                    snapshot.cursor(),
                    snapshot.selection(),
                    snapshot.scroll()
            ));
        }
    }

    private LabelComponent statusLabel(int width) {
        LabelComponent label = UiFactory.message("", UiColors.MUTED).maxWidth(width);
        label.horizontalSizing(Sizing.fill(100));
        return label;
    }

    private void setStatusText(LabelComponent label, Component fullText, int color, int fallbackWidth) {
        Component safeText = fullText == null ? Component.literal(STATUS_EMPTY_TEXT) : fullText;
        int measuredWidth = label.width();
        int textWidth = Math.max(
                1,
                (measuredWidth > 0 ? measuredWidth : fallbackWidth) - UiFactory.scaledPixels(STATUS_TEXT_SAFETY_PADDING)
        );
        Component fitted = UiFactory.fitToWidth(safeText, textWidth);
        label.maxWidth(textWidth);
        label.text(fitted);
        label.color(Color.ofRgb(color));
        label.tooltip(safeText.getString().isBlank() ? List.of() : List.of(safeText));
    }

    private long resolveValidationDelay(int textLength, boolean likelyIncomplete, long baseDelay, long largeDelay, long veryLargeDelay, long incompleteDelay) {
        long delay = textLength >= VERY_LARGE_TEXT_THRESHOLD ? veryLargeDelay : textLength >= LARGE_TEXT_THRESHOLD ? largeDelay : baseDelay;
        return likelyIncomplete ? Math.max(delay, incompleteDelay) : delay;
    }

    private String currentContextItemId() {
        var id = BuiltInRegistries.ITEM.getKey(this.screen.session().previewStack().getItem());
        return id.toString();
    }

    private int resolveEditorHeight(
            int viewportHeight,
            ControlLayout controlLayout,
            boolean optionsExpanded,
            int editorWidthHint
    ) {
        int availableHeight = Math.max(1, viewportHeight);
        int editorHeight = availableHeight - this.nonEditorHeight(controlLayout, optionsExpanded, editorWidthHint);
        return Math.clamp(editorHeight, Math.min(EDITOR_MIN_HEIGHT, availableHeight), availableHeight);
    }

    private int nonEditorHeight(ControlLayout controlLayout, boolean optionsExpanded, int editorWidthHint) {
        int sectionGap = UiFactory.scaleProfile().spacing();
        int topChildCount;
        int topHeight;
        if (controlLayout.stackActionRows()) {
            topHeight = controlLayout.actionButtonHeight() + controlLayout.undoRedoButtonHeight();
            topChildCount = 2;
        } else {
            topHeight = Math.max(controlLayout.actionButtonHeight(), controlLayout.undoRedoButtonHeight());
            topChildCount = 1;
        }

        if (optionsExpanded) {
            topHeight += this.optionsPanelHeight(
                    controlLayout,
                    editorWidthHint >= UiFactory.scaledPixels(OPTIONS_INLINE_WIDTH_THRESHOLD)
            );
            topChildCount++;
        }

        int bodyLineHeight = UiFactory.scaleProfile().bodyLineHeight() + UiFactory.scaleProfile().bodyLineSpacing();
        int statusHeight = (bodyLineHeight * STATUS_LINE_COUNT) + (sectionGap * STATUS_LINE_COUNT);
        int topSectionGaps = sectionGap * topChildCount;
        int safetyPadding = UiFactory.scaledPixels(EDITOR_HEIGHT_SAFETY_PADDING);
        return topHeight + topSectionGaps + statusHeight + safetyPadding;
    }

    private int optionsPanelHeight(ControlLayout controlLayout, boolean inlineOptions) {
        int panelGap = UiFactory.scaleProfile().spacing();
        int subCardPadding = Math.max(4, UiFactory.scaleProfile().padding() - 1) * 2;
        int checkboxHeight = UiFactory.scaleProfile().controlHeight();
        int sliderHeight = UiFactory.scaleProfile().controlHeight();
        int captionLineHeight = UiFactory.scaleProfile().captionLineHeight() + UiFactory.scaleProfile().bodyLineSpacing();
        return subCardPadding
                + (checkboxHeight * (inlineOptions ? 1 : 3))
                + sliderHeight
                + (captionLineHeight * (inlineOptions ? 2 : 3))
                + controlLayout.actionButtonHeight()
                + (panelGap * (inlineOptions ? 4 : 7));
    }

    private FlowLayout optionGroup(boolean inline, UIComponent... children) {
        FlowLayout group = inline ? UiFactory.row() : UiFactory.column();
        for (UIComponent child : children) {
            group.child(child.horizontalSizing(inline ? Sizing.content() : Sizing.fill(100)));
        }
        return group;
    }

    private boolean isLikelyIncompleteRawState(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }

        boolean inString = false;
        boolean escaping = false;
        boolean newlineInsideString = false;
        char quote = '\0';
        int braceDepth = 0;
        int bracketDepth = 0;

        for (int index = 0; index < text.length(); index++) {
            char value = text.charAt(index);
            if (inString) {
                if (value == '\n' || value == '\r') {
                    newlineInsideString = true;
                }
                if (escaping) {
                    escaping = false;
                    continue;
                }
                if (value == '\\') {
                    escaping = true;
                    continue;
                }
                if (value == quote) {
                    inString = false;
                    quote = '\0';
                }
                continue;
            }

            if (value == '"' || value == '\'') {
                inString = true;
                quote = value;
                escaping = false;
                continue;
            }
            if (value == '{') {
                braceDepth++;
                continue;
            }
            if (value == '}') {
                braceDepth--;
                if (braceDepth < 0) {
                    return true;
                }
                continue;
            }
            if (value == '[') {
                bracketDepth++;
                continue;
            }
            if (value == ']') {
                bracketDepth--;
                if (bracketDepth < 0) {
                    return true;
                }
            }
        }

        if (inString || newlineInsideString || braceDepth != 0 || bracketDepth != 0) {
            return true;
        }

        int tail = text.length() - 1;
        while (tail >= 0 && Character.isWhitespace(text.charAt(tail))) {
            tail--;
        }
        if (tail < 0) {
            return false;
        }
        char last = text.charAt(tail);
        return last == ':' || last == ',' || last == '{' || last == '[' || last == '"' || last == '\'';
    }

    private String compactError(String raw, int maxLength) {
        if (raw == null || raw.isBlank()) {
            return ItemEditorText.str("raw.unknown_error");
        }

        String message = raw
                .replace('\r', ' ')
                .replace('\n', ' ')
                .replace('\t', ' ')
                .trim();
        while (message.contains("  ")) {
            message = message.replace("  ", " ");
        }

        String lowered = message.toLowerCase(Locale.ROOT);
        int missedInputIndex = lowered.indexOf(" missed input");
        if (missedInputIndex > 0) {
            message = message.substring(0, missedInputIndex).trim();
        }

        if (message.length() > maxLength) {
            message = message.substring(0, Math.max(0, maxLength - 3)).trim() + "...";
        }
        return message.isBlank() ? ItemEditorText.str("raw.unknown_error") : message;
    }

    private void applyParseFailure(
            ParseFailure failure,
            LabelComponent parseStatus,
            LabelComponent diffStatus,
            int statusWidth,
            RawTextAreaComponent editor,
            int[] errorLine,
            int[] errorColumn
    ) {
        ErrorHighlight highlight = this.resolveErrorHighlight(editor.getValue(), failure);
        errorLine[0] = highlight.line();
        errorColumn[0] = highlight.column();
        String conciseError = this.compactError(failure.error(), MAX_STATUS_ERROR_LENGTH);
        String parseMessage = highlight.hasPosition()
                ? "Parse error L" + highlight.line() + ":" + highlight.column() + " - " + conciseError
                : "Parse error - " + conciseError;
        String validationError = this.compactError(failure.error(), MAX_VALIDATION_ERROR_LENGTH);
        String validationMessage = highlight.hasPosition()
                ? ItemEditorText.str(
                "dialog.apply.parse.error_position",
                validationError,
                highlight.line(),
                highlight.column()
        )
                : ItemEditorText.str(
                "preview.validation.component_failed",
                validationError
        );
        this.setStatusText(parseStatus, Component.literal(parseMessage), UiColors.DANGER, statusWidth);
        this.setStatusText(diffStatus, Component.literal(STATUS_EMPTY_TEXT), UiColors.MUTED, statusWidth);
        editor.setErrorLocation(highlight.line(), highlight.column(), highlight.length());
        this.screen.session().setTransientValidationMessages(
                List.of(ValidationMessage.error(validationMessage))
        );
    }

    private ErrorHighlight resolveErrorHighlight(String rawText, ParseFailure failure) {
        if (failure.hasPosition()) {
            return this.highlightFromLineColumn(rawText, failure.line(), failure.column());
        }
        if (rawText == null || rawText.isBlank() || failure.error() == null || failure.error().isBlank()) {
            return ErrorHighlight.none();
        }

        Matcher missingKeyMatcher = MISSING_KEY_PATTERN.matcher(failure.error());
        if (missingKeyMatcher.find()) {
            String actualKey = missingKeyMatcher.group(2);
            ErrorHighlight actual = this.highlightForKey(rawText, actualKey);
            if (actual.hasPosition()) {
                return actual;
            }
            String expectedKey = missingKeyMatcher.group(1);
            return this.highlightForKey(rawText, expectedKey);
        }
        return ErrorHighlight.none();
    }

    private ErrorHighlight highlightFromLineColumn(String rawText, int line, int column) {
        if (rawText == null || rawText.isBlank() || line <= 0 || column <= 0) {
            return ErrorHighlight.none();
        }
        int lineStart = this.rawLineStart(rawText, line);
        if (lineStart < 0 || lineStart > rawText.length()) {
            return ErrorHighlight.none();
        }
        int lineEnd = this.rawLineEnd(rawText, lineStart);
        String lineText = rawText.substring(lineStart, lineEnd);
        int localStart = Math.clamp(column - 1, 0, lineText.length());
        while (localStart < lineText.length() && Character.isWhitespace(lineText.charAt(localStart))) {
            localStart++;
        }
        int length = this.detectTokenLength(lineText, localStart);
        return new ErrorHighlight(line, localStart + 1, length);
    }

    private ErrorHighlight highlightForKey(String rawText, String key) {
        if (key == null || key.isBlank()) {
            return ErrorHighlight.none();
        }
        Pattern keyPattern = Pattern.compile("(?i)(?:\"" + Pattern.quote(key) + "\"|" + Pattern.quote(key) + ")\\s*:");
        Matcher matcher = keyPattern.matcher(rawText);
        if (!matcher.find()) {
            return ErrorHighlight.none();
        }

        String loweredRaw = rawText.toLowerCase(Locale.ROOT);
        String loweredKey = key.toLowerCase(Locale.ROOT);
        int keyStart = loweredRaw.indexOf(loweredKey, matcher.start());
        if (keyStart < 0) {
            keyStart = matcher.start();
        }

        int line = 1;
        int lineStart = 0;
        for (int index = 0; index < keyStart; index++) {
            if (rawText.charAt(index) == '\n') {
                line++;
                lineStart = index + 1;
            }
        }
        int column = (keyStart - lineStart) + 1;
        return new ErrorHighlight(line, column, key.length());
    }

    private int rawLineStart(String text, int lineOneBased) {
        int targetLine = Math.max(1, lineOneBased);
        int line = 1;
        for (int index = 0; index < text.length(); index++) {
            if (line == targetLine) {
                return index;
            }
            if (text.charAt(index) == '\n') {
                line++;
            }
        }
        return line == targetLine ? text.length() : -1;
    }

    private int rawLineEnd(String text, int lineStart) {
        int index = Math.clamp(lineStart, 0, text.length());
        while (index < text.length()) {
            char value = text.charAt(index);
            if (value == '\n' || value == '\r') {
                break;
            }
            index++;
        }
        return index;
    }

    private int detectTokenLength(String lineText, int startOffset) {
        int start = Math.clamp(startOffset, 0, lineText.length());
        if (start >= lineText.length()) {
            return 1;
        }
        char first = lineText.charAt(start);
        if (first == '"' || first == '\'') {
            int end = start + 1;
            while (end < lineText.length()) {
                char value = lineText.charAt(end);
                if (value == first) {
                    return Math.max(1, end - start + 1);
                }
                if (value == '\\' && end + 1 < lineText.length()) {
                    end += 2;
                    continue;
                }
                end++;
            }
            return Math.max(1, lineText.length() - start);
        }
        int end = start;
        while (end < lineText.length() && this.isTokenChar(lineText.charAt(end))) {
            end++;
        }
        return Math.max(1, end - start);
    }

    private boolean isTokenChar(char value) {
        return Character.isLetterOrDigit(value)
                || value == '_'
                || value == ':'
                || value == '.'
                || value == '-'
                || value == '/';
    }

    private record ParseFailure(String error, int line, int column) {
        static ParseFailure from(RawValidationAsyncService.ParsePhaseResult result) {
            return new ParseFailure(result.parseError(), result.line(), result.column());
        }

        static ParseFailure from(RawValidationAsyncService.Result result) {
            return new ParseFailure(result.parseError(), result.line(), result.column());
        }

        boolean hasPosition() {
            return this.line > 0 && this.column > 0;
        }
    }

    private record ErrorHighlight(int line, int column, int length) {
        static ErrorHighlight none() {
            return new ErrorHighlight(-1, -1, 0);
        }

        boolean hasPosition() {
            return this.line > 0 && this.column > 0;
        }
    }

}
