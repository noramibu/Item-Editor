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
import me.noramibu.itemeditor.ui.screen.ItemEditorScreen;
import me.noramibu.itemeditor.util.ItemEditorText;
import me.noramibu.itemeditor.util.RawAutocompleteAsyncService;
import me.noramibu.itemeditor.util.RawAutocompleteUtil;
import me.noramibu.itemeditor.util.RawItemDataUtil;
import me.noramibu.itemeditor.util.RawValidationAsyncService;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class RawEditorPanel implements EditorPanel {
    private static final int EDITOR_MIN_HEIGHT = 180;
    private static final int EDITOR_RESERVED_SPACE = 140;
    private static final int STATUS_WIDTH_MIN = 140;
    private static final String STATUS_VALIDATING_TEXT = "Validating...";
    private static final String STATUS_EMPTY_TEXT = " ";
    private static final int COLOR_PARSE_ERROR = 0xFF8A8A;
    private static final int COLOR_PARSE_OK = 0x7ED67A;
    private static final int COLOR_DIFF_INFO = 0xA9B5C0;
    private static final int LARGE_TEXT_THRESHOLD = 12000;
    private static final int VERY_LARGE_TEXT_THRESHOLD = 350000;
    private static final long RAW_AUTOCOMPLETE_THROTTLE_HEAVY_MS = 260L;
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
    private static final double CONTROL_COMPACT_SCALE_THRESHOLD = 3.0d;
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
    private static final int HEIGHT_RATIO_THRESHOLD_XXL = 900;
    private static final int HEIGHT_RATIO_THRESHOLD_XL = 760;
    private static final int HEIGHT_RATIO_THRESHOLD_L = 620;
    private static final int HEIGHT_RATIO_THRESHOLD_M = 520;
    private static final double HEIGHT_RATIO_XXL = 0.86d;
    private static final double HEIGHT_RATIO_XL = 0.84d;
    private static final double HEIGHT_RATIO_L = 0.82d;
    private static final double HEIGHT_RATIO_M = 0.76d;
    private static final double HEIGHT_RATIO_BASE = 0.70d;
    private static final int WIDTH_RATIO_BONUS_THRESHOLD = 1500;
    private static final int WIDTH_RATIO_PENALTY_THRESHOLD_L = 1200;
    private static final int WIDTH_RATIO_PENALTY_THRESHOLD_M = 960;
    private static final double WIDTH_RATIO_BONUS = 0.03d;
    private static final double WIDTH_RATIO_PENALTY_L = 0.04d;
    private static final double WIDTH_RATIO_PENALTY_M = 0.04d;
    private static final double SCALE_RATIO_PENALTY = 0.02d;
    private static final double HEIGHT_RATIO_MIN = 0.66d;
    private static final double HEIGHT_RATIO_MAX = 0.90d;
    private static final int RESERVED_EXTRA_SCALE_THRESHOLD_WIDTH = 1100;
    private static final int RESERVED_EXTRA_SMALL_HEIGHT = 560;
    private static final int RESERVED_SCALE_EXTRA = 24;
    private static final int RESERVED_NARROW_EXTRA = 12;
    private static final int RESERVED_SHORT_EXTRA = 16;
    private static final int SECTION_RIGHT_SAFETY_PADDING_BASE = 12;
    private static final int ACTION_BUTTON_WIDTH_MIN = 78;
    private static final int ACTION_BUTTON_WIDTH_BASE = 96;
    private static final int ACTION_BUTTON_TEXT_WIDTH_MIN = 24;
    private static final int ACTION_BUTTON_TEXT_HORIZONTAL_RESERVE = 10;
    private static final int ACTION_BUTTON_TEXT_NON_COMPACT_RESERVE = 16;

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
        section.padding(Insets.of(0, rightSafetyPadding, 0, 0));
        int editorHeight = this.resolveEditorHeight();
        int editorWidthHint = this.screen.editorContentWidthHint();
        ControlLayout controlLayout = this.resolveControlLayout(editorWidthHint);

        RawTextAreaComponent editor = new RawTextAreaComponent(Sizing.fill(100), UiFactory.fixed(editorHeight), state.rawEditorText);
        editor.displayCharCount(false);
        editor.wordWrap(state.rawEditorWordWrap);
        editor.horizontalScroll(state.rawEditorHorizontalScroll);
        editor.fontSizePercent(state.rawEditorFontSizePercent);
        this.restoreEditorUiState(state, editor);
        RawAutocompleteAsyncService autocompleteService = new RawAutocompleteAsyncService();
        RawValidationAsyncService validationService = new RawValidationAsyncService();
        RawAutocompleteUtil.AutocompleteResult[] autocomplete = new RawAutocompleteUtil.AutocompleteResult[]{
                RawAutocompleteUtil.AutocompleteResult.empty(editor.caretIndex())
        };
        int[] selectedSuggestion = new int[]{0};
        int[] autocompleteCaret = new int[]{editor.caretIndex()};
        boolean[] autocompleteVirtualCaret = new boolean[]{editor.hasVirtualCaret()};
        int[] activeLine = new int[]{editor.caretLine()};
        int[] errorLine = new int[]{-1};
        int[] errorColumn = new int[]{-1};
        long[] lastAutocompleteRequestedAt = new long[]{0L};
        RawAutocompleteAsyncService.EditDelta[] pendingAutocompleteDelta = new RawAutocompleteAsyncService.EditDelta[]{null};

        ButtonComponent optionsButton = this.rawActionButton(
                ItemEditorText.tr("raw_editor.options"),
                controlLayout.compactControls(),
                controlLayout.secondaryButtonWidth(),
                controlLayout.compactControlHeight(),
                controlLayout.stackActionRows(),
                button -> {
                    state.uiRawEditorOptionsExpanded = !state.uiRawEditorOptionsExpanded;
                    this.screen.refreshCurrentPanel();
                }
        );
        ButtonComponent undoButton = this.rawActionButton(
                ItemEditorText.tr("raw_editor.undo"),
                controlLayout.compactControls(),
                controlLayout.undoRedoButtonWidth(),
                controlLayout.compactControlHeight(),
                false,
                button -> editor.undo()
        );
        ButtonComponent redoButton = this.rawActionButton(
                ItemEditorText.tr("raw_editor.redo"),
                controlLayout.compactControls(),
                controlLayout.undoRedoButtonWidth(),
                controlLayout.compactControlHeight(),
                false,
                button -> editor.redo()
        );

        if (controlLayout.stackActionRows()) {
            FlowLayout optionsHeader = UiFactory.column();
            optionsHeader.child(optionsButton);
            section.child(optionsHeader);

            FlowLayout toolActions = UiFactory.column();
            toolActions.child(undoButton);
            toolActions.child(redoButton);
            section.child(toolActions);
        } else {
            FlowLayout topActions = UiFactory.row();
            topActions.child(optionsButton);
            topActions.child(undoButton);
            topActions.child(redoButton);
            section.child(topActions);
        }

        if (state.uiRawEditorOptionsExpanded) {
            FlowLayout optionsPanel = UiFactory.subCard();
            optionsPanel.child(UiFactory.checkbox(
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
            ).horizontalSizing(Sizing.fill(100)));
            optionsPanel.child(UiFactory.checkbox(
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
            ).horizontalSizing(Sizing.fill(100)));

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

            int hintWidth = Math.max(120, editorWidthHint - UiFactory.scaledPixels(20));
            optionsPanel.child(UiFactory.muted(ItemEditorText.tr("raw_editor.autocomplete.hint.navigate"), hintWidth));
            optionsPanel.child(UiFactory.muted(ItemEditorText.tr("raw_editor.autocomplete.hint.accept"), hintWidth));

            FlowLayout dataActions = controlLayout.stackActionRows() ? UiFactory.column() : UiFactory.row();
            UIComponent formatButton = this.rawActionButton(
                    ItemEditorText.tr("dialog.apply.raw.format"),
                    controlLayout.compactControls(),
                    controlLayout.secondaryButtonWidth(),
                    controlLayout.compactControlHeight(),
                    controlLayout.stackActionRows(),
                    button -> this.setRawText(
                            state,
                            RawItemDataUtil.format(
                                    state.rawEditorText,
                                    this.screen.session().registryAccess(),
                                    state.rawEditorShowDefaults
                            )
                    )
            );
            UIComponent minifyButton = this.rawActionButton(
                    ItemEditorText.tr("dialog.apply.raw.minify"),
                    controlLayout.compactControls(),
                    controlLayout.secondaryButtonWidth(),
                    controlLayout.compactControlHeight(),
                    controlLayout.stackActionRows(),
                    button -> this.setRawText(state, RawItemDataUtil.minify(state.rawEditorText))
            );
            dataActions.child(formatButton);
            dataActions.child(minifyButton);
            optionsPanel.child(dataActions);
            section.child(optionsPanel);
        }

        boolean[] autocompleteForced = new boolean[]{false};

        section.child(editor);

        int preferredStatusWidth = Math.max(STATUS_WIDTH_MIN, editorWidthHint - UiFactory.scrollContentInset(PANEL_SCROLLBAR_BASE_THICKNESS));
        int statusWidth = Math.max(1, Math.min(editorWidthHint, preferredStatusWidth));
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
                                    editor,
                                    errorLine,
                                    errorColumn
                            );
                            return;
                        }

                        errorLine[0] = -1;
                        errorColumn[0] = -1;
                        parseStatus.text(ItemEditorText.tr("raw_editor.status.valid"));
                        parseStatus.color(Color.ofRgb(COLOR_PARSE_OK));
                        editor.setErrorLocation(-1, -1, 0);
                        diffStatus.text(Component.literal(STATUS_VALIDATING_TEXT));
                        diffStatus.color(Color.ofRgb(COLOR_DIFF_INFO));
                        this.screen.session().queueRebuildPreview(0L);
                    },
                    result -> {
                        if (!result.success()) {
                            this.applyParseFailure(
                                    ParseFailure.from(result),
                                    parseStatus,
                                    diffStatus,
                                    editor,
                                    errorLine,
                                    errorColumn
                            );
                            return;
                        }

                        if (result.diffError() != null) {
                            diffStatus.text(Component.literal(ItemEditorText.str("dialog.apply.diff_failed", result.diffError())));
                            diffStatus.color(Color.ofRgb(COLOR_PARSE_ERROR));
                        } else {
                            String diffText = result.diffEntries() == 0
                                    ? ItemEditorText.str("raw_editor.status.no_changes")
                                    : ItemEditorText.str("raw_editor.status.changes", result.diffEntries());
                            diffStatus.text(Component.literal(diffText));
                            diffStatus.color(Color.ofRgb(COLOR_DIFF_INFO));
                        }
                    }
            );
        };

        Runnable refreshHistoryButtons = () -> {
            undoButton.active(editor.canUndo());
            redoButton.active(editor.canRedo());
        };

        Runnable refreshAutocompletePresentation = () -> {
            boolean silent = !autocompleteForced[0] && !this.shouldAutoShowAutocomplete(editor, autocomplete[0]);
            if (autocomplete[0].suggestions().isEmpty() || silent) {
                selectedSuggestion[0] = 0;
                editor.ghostSuggestion("");
                editor.autocompletePopup(List.of(), 0);
                return;
            }

            boolean correctionMode = this.isCorrectionAutocomplete(editor, autocomplete[0]);
            boolean hasStructural = this.hasStructuralCandidate(autocomplete[0]);
            int predictiveIndex = this.firstSuggestionWithGhost(editor, autocomplete[0]);
            if (!autocompleteForced[0] && predictiveIndex < 0 && !correctionMode && !hasStructural) {
                selectedSuggestion[0] = 0;
                editor.ghostSuggestion("");
                editor.autocompletePopup(List.of(), 0);
                return;
            }

            if (selectedSuggestion[0] < 0 || selectedSuggestion[0] >= autocomplete[0].suggestions().size()) {
                selectedSuggestion[0] = predictiveIndex >= 0 ? predictiveIndex : 0;
            }

            RawAutocompleteUtil.Suggestion suggestion = autocomplete[0].suggestions().get(selectedSuggestion[0]);
            String ghostSuffix = this.computeGhostSuffix(editor, autocomplete[0], suggestion.insertText());
            if (ghostSuffix.isEmpty()) {
                int fallback = predictiveIndex >= 0 ? predictiveIndex : this.firstSuggestionWithGhost(editor, autocomplete[0]);
                if (fallback >= 0 && fallback != selectedSuggestion[0]) {
                    selectedSuggestion[0] = fallback;
                    suggestion = autocomplete[0].suggestions().get(selectedSuggestion[0]);
                    ghostSuffix = this.computeGhostSuffix(editor, autocomplete[0], suggestion.insertText());
                }
            }
            if (correctionMode) {
                ghostSuffix = "";
            }
            editor.ghostSuggestion(ghostSuffix);
            editor.autocompletePopup(this.toPopupEntries(autocomplete[0].suggestions()), selectedSuggestion[0]);
        };
        Runnable requestAutocomplete = () -> {
            String rawText = editor.getValue();
                if (!autocompleteForced[0] && this.shouldThrottleHeavyRequest(rawText.length(), lastAutocompleteRequestedAt)) {
                return;
            }
            if (editor.hasVirtualCaret() && !editor.hasSelection()) {
                autocomplete[0] = RawAutocompleteUtil.AutocompleteResult.empty(editor.caretIndex());
                refreshAutocompletePresentation.run();
                return;
            }
            RawAutocompleteAsyncService.EditDelta editDelta = pendingAutocompleteDelta[0];
            pendingAutocompleteDelta[0] = null;
            autocompleteService.request(
                    rawText,
                    editor.caretIndex(),
                    this.screen.session().registryAccess(),
                    this.currentContextItemId(),
                    editDelta,
                    result -> {
                        if (result.requestedCaret() != editor.caretIndex()) {
                            return;
                        }
                        autocomplete[0] = result;
                        if (!result.suggestions().isEmpty() && selectedSuggestion[0] >= result.suggestions().size()) {
                            selectedSuggestion[0] = 0;
                        }
                        if (result.suggestions().isEmpty()) {
                            autocompleteForced[0] = false;
                        }
                        refreshAutocompletePresentation.run();
                    }
            );
        };

        editor.onAutocompleteRequested(() -> this.applyAutocompleteSelected(editor, autocomplete[0], selectedSuggestion[0]));
        editor.onAutocompleteRefreshRequested(() -> {
            autocompleteForced[0] = true;
            selectedSuggestion[0] = 0;
            requestAutocomplete.run();
            return true;
        });
        editor.onAutocompleteNextRequested(() -> {
            boolean moved = this.cycleAutocompleteSelection(selectedSuggestion, autocomplete[0], 1);
            if (moved) {
                refreshAutocompletePresentation.run();
            }
            return moved;
        });
        editor.onAutocompletePreviousRequested(() -> {
            boolean moved = this.cycleAutocompleteSelection(selectedSuggestion, autocomplete[0], -1);
            if (moved) {
                refreshAutocompletePresentation.run();
            }
            return moved;
        });
        editor.onHistoryChanged(() -> {
            refreshHistoryButtons.run();
            this.persistEditorUiState(state, editor);
        });

        editor.onChanged().subscribe((text, delta) -> {
            state.rawEditorText = text;
            state.rawEditorEdited = true;
            boolean likelyIncomplete = this.isLikelyIncompleteRawState(text);
            pendingAutocompleteDelta[0] = delta == null ? null : new RawAutocompleteAsyncService.EditDelta(
                    delta.start(),
                    delta.end(),
                    delta.replacement(),
                    delta.structural()
            );
            this.screen.session().cancelQueuedRebuild();
            activeLine[0] = editor.caretLine();
            autocompleteCaret[0] = editor.caretIndex();
            autocompleteVirtualCaret[0] = editor.hasVirtualCaret();
            autocompleteForced[0] = false;
            selectedSuggestion[0] = 0;
            requestAutocomplete.run();
            requestValidation.run();
            this.persistEditorUiState(state, editor);
        });

        editor.onViewportChanged(() -> {
            int caretLine = editor.caretLine();
            caretStatus.text(Component.literal(ItemEditorText.str("raw_editor.caret", caretLine, editor.caretColumn())));
            activeLine[0] = caretLine;

            int caretIndex = editor.caretIndex();
            boolean virtualCaret = editor.hasVirtualCaret();
            if (editor.hasSelection()) {
                autocompleteForced[0] = false;
                selectedSuggestion[0] = 0;
                autocomplete[0] = RawAutocompleteUtil.AutocompleteResult.empty(caretIndex);
                refreshAutocompletePresentation.run();
                this.persistEditorUiState(state, editor);
                return;
            }
            if (caretIndex != autocompleteCaret[0] || virtualCaret != autocompleteVirtualCaret[0]) {
                autocompleteCaret[0] = caretIndex;
                autocompleteVirtualCaret[0] = virtualCaret;
                autocompleteForced[0] = false;
                selectedSuggestion[0] = 0;
                requestAutocomplete.run();
            }
            this.persistEditorUiState(state, editor);
        });

        refreshHistoryButtons.run();
        requestValidation.run();
        requestAutocomplete.run();
        caretStatus.text(Component.literal(ItemEditorText.str("raw_editor.caret", activeLine[0], editor.caretColumn())));
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
            java.util.function.Consumer<ButtonComponent> onPress
    ) {
        int width = fixedWidth > 0 ? fixedWidth : Math.max(ACTION_BUTTON_WIDTH_MIN, UiFactory.scaledPixels(ACTION_BUTTON_WIDTH_BASE));
        int textReserve = compact ? ACTION_BUTTON_TEXT_HORIZONTAL_RESERVE : ACTION_BUTTON_TEXT_NON_COMPACT_RESERVE;
        int textWidth = Math.max(ACTION_BUTTON_TEXT_WIDTH_MIN, width - UiFactory.scaledPixels(textReserve));
        Component fitted = UiFactory.fitToWidth(label, textWidth);
        ButtonComponent button = UiFactory.button(fitted, UiFactory.ButtonTextPreset.STANDARD, onPress);
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

    private boolean shouldThrottleHeavyRequest(int textLength, long[] lastRequestAt) {
        if (textLength < VERY_LARGE_TEXT_THRESHOLD) {
            return false;
        }
        long now = System.currentTimeMillis();
        if (now - lastRequestAt[0] < RAW_AUTOCOMPLETE_THROTTLE_HEAVY_MS) {
            return true;
        }
        lastRequestAt[0] = now;
        return false;
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
        options.horizontalScroll = !state.rawEditorWordWrap;
        options.showDefaultKeys = state.rawEditorShowDefaults;
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
        boolean compactControls = guiScale >= CONTROL_COMPACT_SCALE_THRESHOLD || scaledWidth < CONTROL_COMPACT_WIDTH_THRESHOLD;
        int nonCompactButtonWidth = UiFactory.scaledPixels(ACTION_BUTTON_WIDTH_BASE);
        int nonCompactActionRowMinWidth = (nonCompactButtonWidth * ACTION_ROW_BUTTON_COUNT) + UiFactory.scaleProfile().spacing();
        int staticStackThreshold = UiFactory.scaledPixels(CONTROL_STACK_WIDTH_HINT);
        int stackThreshold = Math.max(nonCompactActionRowMinWidth, staticStackThreshold);
        boolean stackActionRows = compactControls || editorWidthHint < stackThreshold;
        int baseControlHeight = UiFactory.scaleProfile().controlHeight();
        int compactControlHeight = compactControls
                ? (int) Math.round(baseControlHeight * CONTROL_COMPACT_HEIGHT_RATIO)
                : baseControlHeight;
        int secondaryButtonWidth = compactControls
                ? Math.max(CONTROL_SECONDARY_BUTTON_MIN, Math.min(CONTROL_SECONDARY_BUTTON_MAX, scaledWidth / CONTROL_SECONDARY_BUTTON_DIVISOR))
                : -1;
        int undoRedoButtonWidth = compactControls
                ? Math.max(CONTROL_UNDO_REDO_BUTTON_MIN, Math.min(CONTROL_UNDO_REDO_BUTTON_MAX, scaledWidth / CONTROL_UNDO_REDO_BUTTON_DIVISOR))
                : -1;
        return new ControlLayout(
                compactControls,
                stackActionRows,
                compactControlHeight,
                secondaryButtonWidth,
                undoRedoButtonWidth
        );
    }

    private record ControlLayout(
            boolean compactControls,
            boolean stackActionRows,
            int compactControlHeight,
            int secondaryButtonWidth,
            int undoRedoButtonWidth
    ) {
    }

    private List<RawTextAreaComponent.HistorySnapshot> toHistorySnapshots(List<ItemEditorState.RawEditorHistoryEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return List.of();
        }
        return entries.stream()
                .filter(entry -> entry != null)
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

    private boolean applyAutocompleteSelected(
            RawTextAreaComponent editor,
            RawAutocompleteUtil.AutocompleteResult autocomplete,
            int selectedIndex
    ) {
        RawAutocompleteUtil.AutocompleteResult effective = this.resolveAutocompleteForCurrentCaret(editor, autocomplete);
        if (effective.suggestions().isEmpty()) {
            return false;
        }
        int index = Math.clamp(selectedIndex, 0, effective.suggestions().size() - 1);
        return this.applyAutocomplete(editor, effective, effective.suggestions().get(index).insertText());
    }

    private RawAutocompleteUtil.AutocompleteResult resolveAutocompleteForCurrentCaret(
            RawTextAreaComponent editor,
            RawAutocompleteUtil.AutocompleteResult autocomplete
    ) {
        if (editor.hasVirtualCaret() && !editor.hasSelection()) {
            return RawAutocompleteUtil.AutocompleteResult.empty(editor.caretIndex());
        }
        if (autocomplete.requestedCaret() == editor.caretIndex()) {
            return autocomplete;
        }
        return RawAutocompleteUtil.suggest(
                editor.getValue(),
                editor.caretIndex(),
                this.screen.session().registryAccess(),
                this.currentContextItemId()
        );
    }

    private boolean applyAutocomplete(
            RawTextAreaComponent editor,
            RawAutocompleteUtil.AutocompleteResult autocomplete,
            String insertText
    ) {
        if (insertText == null || insertText.isEmpty()) {
            return false;
        }

        if (editor.hasSelection()) {
            int start = Math.min(editor.caretIndex(), editor.selectionIndex());
            int end = Math.max(editor.caretIndex(), editor.selectionIndex());
            editor.replaceRange(start, end, insertText);
            return true;
        }

        int replaceStart = Math.clamp(autocomplete.replaceStart(), 0, editor.getValue().length());
        int replaceEnd = Math.clamp(autocomplete.replaceEnd(), replaceStart, editor.getValue().length());
        editor.replaceRange(replaceStart, replaceEnd, insertText);
        return true;
    }

    private String computeGhostSuffix(
            RawTextAreaComponent editor,
            RawAutocompleteUtil.AutocompleteResult autocomplete,
            String insertText
    ) {
        if (insertText == null || insertText.isEmpty() || editor.hasSelection()) {
            return "";
        }

        int caret = editor.caretIndex();
        int replaceStart = Math.clamp(autocomplete.replaceStart(), 0, caret);
        int replaceEnd = Math.clamp(autocomplete.replaceEnd(), replaceStart, editor.getValue().length());
        if (replaceEnd < caret) {
            return "";
        }
        int typedEnd = Math.min(caret, replaceEnd);
        int typedLength = Math.max(0, typedEnd - replaceStart);
        if (typedLength == 0) {
            return insertText;
        }
        if (typedLength >= insertText.length()) {
            return "";
        }

        String typed = editor.getValue().substring(replaceStart, typedEnd);
        if (!insertText.regionMatches(true, 0, typed, 0, typed.length())) {
            return "";
        }

        return insertText.substring(typed.length());
    }

    private int firstSuggestionWithGhost(
            RawTextAreaComponent editor,
            RawAutocompleteUtil.AutocompleteResult autocomplete
    ) {
        List<RawAutocompleteUtil.Suggestion> suggestions = autocomplete.suggestions();
        for (int index = 0; index < suggestions.size(); index++) {
            String suffix = this.computeGhostSuffix(editor, autocomplete, suggestions.get(index).insertText());
            if (!suffix.isEmpty()) {
                return index;
            }
        }
        return -1;
    }

    private boolean cycleAutocompleteSelection(
            int[] selectedSuggestion,
            RawAutocompleteUtil.AutocompleteResult autocomplete,
            int delta
    ) {
        if (autocomplete.suggestions().isEmpty()) {
            return false;
        }
        int size = autocomplete.suggestions().size();
        selectedSuggestion[0] = Math.floorMod(selectedSuggestion[0] + delta, size);
        return true;
    }

    private List<RawTextAreaComponent.AutocompletePopupEntry> toPopupEntries(List<RawAutocompleteUtil.Suggestion> suggestions) {
        if (suggestions == null || suggestions.isEmpty()) {
            return List.of();
        }
        return suggestions.stream()
                .map(suggestion -> new RawTextAreaComponent.AutocompletePopupEntry(
                        suggestion.label(),
                        suggestion.insertText().equals(suggestion.label()) ? suggestion.kind().name() : suggestion.insertText()
                ))
                .toList();
    }

    private boolean shouldAutoShowAutocomplete(
            RawTextAreaComponent editor,
            RawAutocompleteUtil.AutocompleteResult autocomplete
    ) {
        if (autocomplete.suggestions().isEmpty()) {
            return false;
        }
        if (editor.hasSelection()) {
            return true;
        }

        int caret = editor.caretIndex();
        int replaceStart = Math.clamp(autocomplete.replaceStart(), 0, caret);
        int replaceEnd = Math.clamp(autocomplete.replaceEnd(), replaceStart, editor.getValue().length());
        boolean correctionMode = replaceEnd < caret;
        int typedEnd = Math.min(caret, replaceEnd);
        String typed = editor.getValue().substring(replaceStart, typedEnd);
        if (correctionMode) {
            return true;
        }
        if (!typed.isBlank() && this.shouldSuppressCompletedKeyEcho(editor, autocomplete, typed)) {
            return false;
        }
        if (!typed.isBlank() && this.isCursorAtCompletedToken(editor, autocomplete)) {
            return false;
        }
        if (!typed.isBlank() && !this.hasPredictiveCandidate(autocomplete, typed) && !this.hasStructuralCandidate(autocomplete)) {
            return false;
        }
        if (!typed.isBlank() && this.onlyEchoesTypedToken(autocomplete, typed)) {
            return false;
        }
        if (replaceStart < caret) {
            return true;
        }

        String text = editor.getValue();
        if (caret == 0 || caret > text.length()) {
            return false;
        }

        char previous = text.charAt(caret - 1);
        return previous == ':' || previous == '"' || previous == '\'' || previous == '[' || previous == '{' || previous == ',';
    }

    private boolean isCursorAtCompletedToken(
            RawTextAreaComponent editor,
            RawAutocompleteUtil.AutocompleteResult autocomplete
    ) {
        int caret = editor.caretIndex();
        String text = editor.getValue();
        if (caret == 0 || caret > text.length()) {
            return false;
        }
        int replaceStart = Math.clamp(autocomplete.replaceStart(), 0, caret);
        if (replaceStart == caret) {
            return false;
        }
        int tokenEnd = caret;
        while (tokenEnd < text.length() && this.isTokenChar(text.charAt(tokenEnd))) {
            tokenEnd++;
        }
        return tokenEnd > caret;
    }

    private boolean isTokenChar(char value) {
        return Character.isLetterOrDigit(value)
                || value == '_'
                || value == ':'
                || value == '.'
                || value == '-'
                || value == '/';
    }

    private boolean hasPredictiveCandidate(
            RawAutocompleteUtil.AutocompleteResult autocomplete,
            String typed
    ) {
        for (RawAutocompleteUtil.Suggestion suggestion : autocomplete.suggestions()) {
            String insert = suggestion.insertText();
            if (insert == null || insert.isBlank()) {
                continue;
            }
            if (this.isPredictiveMatch(insert, typed)) {
                return true;
            }
            String label = suggestion.label();
            if (this.isPredictiveMatch(label, typed)) {
                return true;
            }
        }
        return false;
    }

    private boolean isPredictiveMatch(String candidate, String typed) {
        if (candidate == null || typed == null) {
            return false;
        }
        if (candidate.length() > typed.length() && candidate.regionMatches(true, 0, typed, 0, typed.length())) {
            return true;
        }
        int candidateSeparator = candidate.indexOf(':');
        if (candidateSeparator < 0) {
            return false;
        }
        String localCandidate = candidate.substring(candidateSeparator + 1);
        String localTyped = typed;
        int typedSeparator = typed.indexOf(':');
        if (typedSeparator >= 0 && typedSeparator + 1 < typed.length()) {
            localTyped = typed.substring(typedSeparator + 1);
        }
        return localCandidate.length() > localTyped.length()
                && localCandidate.regionMatches(true, 0, localTyped, 0, localTyped.length());
    }

    private boolean onlyEchoesTypedToken(
            RawAutocompleteUtil.AutocompleteResult autocomplete,
            String typed
    ) {
        if (autocomplete.suggestions().isEmpty()) {
            return false;
        }
        for (RawAutocompleteUtil.Suggestion suggestion : autocomplete.suggestions()) {
            String insert = suggestion.insertText();
            if (insert == null) {
                return false;
            }
            if (!insert.equalsIgnoreCase(typed)) {
                return false;
            }
        }
        return true;
    }

    private boolean hasStructuralCandidate(RawAutocompleteUtil.AutocompleteResult autocomplete) {
        for (RawAutocompleteUtil.Suggestion suggestion : autocomplete.suggestions()) {
            if (suggestion.kind() == RawAutocompleteUtil.SuggestionKind.STRUCTURAL) {
                return true;
            }
        }
        return false;
    }

    private boolean shouldSuppressCompletedKeyEcho(
            RawTextAreaComponent editor,
            RawAutocompleteUtil.AutocompleteResult autocomplete,
            String typed
    ) {
        if (typed != null && typed.contains(":")) {
            return false;
        }
        if (!this.hasColonAheadOnSameLine(editor.getValue(), editor.caretIndex())) {
            return false;
        }

        String normalizedTyped = this.normalizeKeyToken(typed);
        if (!normalizedTyped.isBlank()
                && this.hasExactKeySuggestion(autocomplete, normalizedTyped)
                && !this.hasLongerKeyPrefixSuggestion(autocomplete, normalizedTyped)) {
            return true;
        }

        for (RawAutocompleteUtil.Suggestion suggestion : autocomplete.suggestions()) {
            String insert = suggestion.insertText();
            if (suggestion.kind() == RawAutocompleteUtil.SuggestionKind.STRUCTURAL
                    && insert != null
                    && insert.trim().startsWith(":")) {
                continue;
            }
            if (insert != null && !insert.equalsIgnoreCase(typed)) {
                return false;
            }
            String label = suggestion.label();
            if (label != null && !label.isBlank() && !label.equalsIgnoreCase(typed)) {
                return false;
            }
        }
        return true;
    }

    private boolean hasExactKeySuggestion(
            RawAutocompleteUtil.AutocompleteResult autocomplete,
            String normalizedTyped
    ) {
        for (RawAutocompleteUtil.Suggestion suggestion : autocomplete.suggestions()) {
            if (suggestion.kind() != RawAutocompleteUtil.SuggestionKind.KEY) {
                continue;
            }
            String normalizedInsert = this.normalizeKeyToken(suggestion.insertText());
            if (normalizedInsert.equalsIgnoreCase(normalizedTyped)) {
                return true;
            }
            String normalizedLabel = this.normalizeKeyToken(suggestion.label());
            if (normalizedLabel.equalsIgnoreCase(normalizedTyped)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasLongerKeyPrefixSuggestion(
            RawAutocompleteUtil.AutocompleteResult autocomplete,
            String normalizedTyped
    ) {
        for (RawAutocompleteUtil.Suggestion suggestion : autocomplete.suggestions()) {
            if (suggestion.kind() != RawAutocompleteUtil.SuggestionKind.KEY) {
                continue;
            }
            String normalizedInsert = this.normalizeKeyToken(suggestion.insertText());
            if (normalizedInsert.length() > normalizedTyped.length()
                    && normalizedInsert.regionMatches(true, 0, normalizedTyped, 0, normalizedTyped.length())) {
                return true;
            }
            String normalizedLabel = this.normalizeKeyToken(suggestion.label());
            if (normalizedLabel.length() > normalizedTyped.length()
                    && normalizedLabel.regionMatches(true, 0, normalizedTyped, 0, normalizedTyped.length())) {
                return true;
            }
        }
        return false;
    }

    private String normalizeKeyToken(String token) {
        if (token == null || token.isBlank()) {
            return "";
        }
        String value = token.trim();
        if (value.endsWith(":")) {
            value = value.substring(0, value.length() - 1).trim();
        }
        if (value.length() >= 2 && value.charAt(0) == '"' && value.charAt(value.length() - 1) == '"') {
            value = value.substring(1, value.length() - 1);
        }
        return value;
    }

    private boolean hasColonAheadOnSameLine(String text, int cursor) {
        if (text == null || text.isBlank()) {
            return false;
        }
        int safeCursor = Math.clamp(cursor, 0, text.length());
        int lineStart = text.lastIndexOf('\n', Math.max(0, safeCursor - 1)) + 1;

        boolean inString = false;
        boolean escaping = false;
        char quote = '\0';

        for (int index = lineStart; index < safeCursor; index++) {
            char value = text.charAt(index);
            if (inString) {
                if (escaping) {
                    escaping = false;
                } else if (value == '\\') {
                    escaping = true;
                } else if (value == quote) {
                    inString = false;
                    quote = '\0';
                }
                continue;
            }
            if (value == '"' || value == '\'') {
                inString = true;
                quote = value;
            }
        }

        for (int index = safeCursor; index < text.length(); index++) {
            char value = text.charAt(index);
            if (value == '\n' || value == '\r') {
                return false;
            }
            if (inString) {
                if (escaping) {
                    escaping = false;
                } else if (value == '\\') {
                    escaping = true;
                } else if (value == quote) {
                    inString = false;
                    quote = '\0';
                }
                continue;
            }
            if (value == '"' || value == '\'') {
                inString = true;
                quote = value;
                continue;
            }
            if (Character.isWhitespace(value)) {
                continue;
            }
            return value == ':';
        }
        return false;
    }

    private boolean isCorrectionAutocomplete(
            RawTextAreaComponent editor,
            RawAutocompleteUtil.AutocompleteResult autocomplete
    ) {
        int caret = editor.caretIndex();
        int replaceStart = Math.clamp(autocomplete.replaceStart(), 0, editor.getValue().length());
        int replaceEnd = Math.clamp(autocomplete.replaceEnd(), replaceStart, editor.getValue().length());
        return replaceStart < caret && replaceEnd < caret;
    }

    private LabelComponent statusLabel(int width) {
        LabelComponent label = UiFactory.message("", COLOR_DIFF_INFO).maxWidth(width);
        label.horizontalSizing(Sizing.fill(100));
        return label;
    }

    private long resolveValidationDelay(int textLength, boolean likelyIncomplete, long baseDelay, long largeDelay, long veryLargeDelay, long incompleteDelay) {
        long delay = textLength >= VERY_LARGE_TEXT_THRESHOLD ? veryLargeDelay : textLength >= LARGE_TEXT_THRESHOLD ? largeDelay : baseDelay;
        return likelyIncomplete ? Math.max(delay, incompleteDelay) : delay;
    }

    private String currentContextItemId() {
        var id = BuiltInRegistries.ITEM.getKey(this.screen.session().previewStack().getItem());
        return id.toString();
    }

    private int clampEditorHeight(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private int resolveEditorHeight() {
        Minecraft minecraft = Minecraft.getInstance();
        var window = minecraft.getWindow();
        int guiHeight = window.getGuiScaledHeight();
        int guiWidth = window.getGuiScaledWidth();
        double guiScale = window.getGuiScale();

        double ratio;
        if (guiHeight >= HEIGHT_RATIO_THRESHOLD_XXL) {
            ratio = HEIGHT_RATIO_XXL;
        } else if (guiHeight >= HEIGHT_RATIO_THRESHOLD_XL) {
            ratio = HEIGHT_RATIO_XL;
        } else if (guiHeight >= HEIGHT_RATIO_THRESHOLD_L) {
            ratio = HEIGHT_RATIO_L;
        } else if (guiHeight >= HEIGHT_RATIO_THRESHOLD_M) {
            ratio = HEIGHT_RATIO_M;
        } else {
            ratio = HEIGHT_RATIO_BASE;
        }

        if (guiWidth >= WIDTH_RATIO_BONUS_THRESHOLD) {
            ratio += WIDTH_RATIO_BONUS;
        } else if (guiWidth < WIDTH_RATIO_PENALTY_THRESHOLD_L) {
            ratio -= WIDTH_RATIO_PENALTY_L;
        }
        if (guiWidth < WIDTH_RATIO_PENALTY_THRESHOLD_M) {
            ratio -= WIDTH_RATIO_PENALTY_M;
        }
        if (guiScale >= CONTROL_COMPACT_SCALE_THRESHOLD) {
            ratio -= SCALE_RATIO_PENALTY;
        }
        ratio = Math.clamp(ratio, HEIGHT_RATIO_MIN, HEIGHT_RATIO_MAX);

        int targetHeight = (int) Math.round(guiHeight * ratio);
        int reserved = EDITOR_RESERVED_SPACE;
        if (guiScale >= CONTROL_COMPACT_SCALE_THRESHOLD) {
            reserved += UiFactory.scaledPixels(RESERVED_SCALE_EXTRA);
        }
        if (guiWidth < RESERVED_EXTRA_SCALE_THRESHOLD_WIDTH) {
            reserved += UiFactory.scaledPixels(RESERVED_NARROW_EXTRA);
        }
        if (guiHeight < RESERVED_EXTRA_SMALL_HEIGHT) {
            reserved += UiFactory.scaledPixels(RESERVED_SHORT_EXTRA);
        }
        int maxHeight = Math.max(1, guiHeight - reserved);
        int minHeight = Math.min(EDITOR_MIN_HEIGHT, maxHeight);
        return this.clampEditorHeight(targetHeight, minHeight, maxHeight);
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
        parseStatus.text(Component.literal(parseMessage));
        parseStatus.color(Color.ofRgb(COLOR_PARSE_ERROR));
        diffStatus.text(Component.literal(STATUS_EMPTY_TEXT));
        diffStatus.color(Color.ofRgb(COLOR_DIFF_INFO));
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
        return new ErrorHighlight(line, column, Math.max(1, key.length()));
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
        while (end < lineText.length() && this.isErrorTokenChar(lineText.charAt(end))) {
            end++;
        }
        return Math.max(1, end - start);
    }

    private boolean isErrorTokenChar(char value) {
        return Character.isLetterOrDigit(value)
                || value == '_'
                || value == '-'
                || value == '.'
                || value == ':'
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
