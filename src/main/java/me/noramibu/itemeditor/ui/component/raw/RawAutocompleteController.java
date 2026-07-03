package me.noramibu.itemeditor.ui.component.raw;

import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.ui.component.RawTextAreaComponent;
import me.noramibu.itemeditor.util.RawAutocompleteAsyncService;
import me.noramibu.itemeditor.util.RawAutocompleteUtil;
import net.minecraft.core.RegistryAccess;

import java.util.List;
import java.util.function.Supplier;

public final class RawAutocompleteController {
    private static final int VERY_LARGE_TEXT_THRESHOLD = 350000;
    private static final long THROTTLE_HEAVY_MS = 260L;

    private final ItemEditorState state;
    private final RawTextAreaComponent editor;
    private final Supplier<RegistryAccess> registryAccess;
    private final Supplier<String> fallbackItemId;
    private final Supplier<List<String>> lootTableIds;
    private final Runnable saveOptions;
    private final RawAutocompleteAsyncService service = new RawAutocompleteAsyncService();
    private RawAutocompleteUtil.AutocompleteResult result;
    private RawAutocompleteAsyncService.EditDelta pendingDelta;
    private int selectedSuggestion;
    private int caret;
    private int suppressedCaret = -1;
    private boolean virtualCaret;
    private boolean forced;
    private long lastRequestAt;

    public RawAutocompleteController(
            ItemEditorState state,
            RawTextAreaComponent editor,
            Supplier<RegistryAccess> registryAccess,
            Supplier<String> fallbackItemId,
            Supplier<List<String>> lootTableIds,
            Runnable saveOptions
    ) {
        this.state = state;
        this.editor = editor;
        this.registryAccess = registryAccess;
        this.fallbackItemId = fallbackItemId;
        this.lootTableIds = lootTableIds;
        this.saveOptions = saveOptions;
        this.result = RawAutocompleteUtil.AutocompleteResult.empty(editor.caretIndex());
        this.caret = editor.caretIndex();
        this.virtualCaret = editor.hasVirtualCaret();
    }

    public void setDisabled(boolean disabled) {
        this.state.rawEditorAutocompleteDisabled = disabled;
        this.suppressedCaret = disabled ? this.editor.caretIndex() : -1;
        this.clear();
        this.saveOptions.run();
        if (!disabled) {
            this.request();
        }
    }

    public void onChanged(RawTextAreaComponent.ChangeDelta delta) {
        this.pendingDelta = delta == null ? null : new RawAutocompleteAsyncService.EditDelta(
                delta.start(),
                delta.end(),
                delta.replacement(),
                delta.structural()
        );
        this.resetAutoState();
        this.request();
    }

    public void onViewportChanged() {
        if (this.editor.hasSelection()) {
            this.forced = false;
            this.clear();
            return;
        }
        if (this.editor.caretIndex() == this.caret && this.editor.hasVirtualCaret() == this.virtualCaret) {
            return;
        }
        this.resetAutoState();
        this.request();
    }

    public boolean applySelected() {
        return RawAutocompleteUi.applySelected(
                this.editor,
                this.result,
                this.selectedSuggestion,
                this.registryAccess.get(),
                this.fallbackItemId.get(),
                this.lootTableIds.get()
        );
    }

    public boolean force() {
        if (this.state.rawEditorAutocompleteDisabled) {
            this.clear();
            return false;
        }
        this.forced = true;
        this.suppressedCaret = -1;
        this.selectedSuggestion = 0;
        this.request();
        return true;
    }

    public void dismiss() {
        this.suppressedCaret = this.editor.caretIndex();
        this.forced = false;
        this.clear();
    }

    public boolean moveSelection(int delta) {
        if (this.result.suggestions().isEmpty()) {
            return false;
        }
        this.selectedSuggestion = Math.floorMod(this.selectedSuggestion + delta, this.result.suggestions().size());
        this.refreshPresentation();
        return true;
    }

    public void request() {
        String rawText = this.editor.getValue();
        if (this.state.rawEditorAutocompleteDisabled) {
            this.pendingDelta = null;
            this.clear();
            return;
        }
        if (!this.forced && this.shouldThrottle(rawText.length())) {
            return;
        }
        if (this.editor.hasVirtualCaret() && !this.editor.hasSelection()) {
            this.clear();
            return;
        }
        RawAutocompleteAsyncService.EditDelta editDelta = this.pendingDelta;
        this.pendingDelta = null;
        this.service.request(
                rawText,
                this.editor.caretIndex(),
                this.registryAccess.get(),
                this.fallbackItemId.get(),
                this.lootTableIds.get(),
                editDelta,
                autocompleteResult -> {
                    if (autocompleteResult.requestedCaret() != this.editor.caretIndex()) {
                        return;
                    }
                    this.result = autocompleteResult;
                    if (!autocompleteResult.suggestions().isEmpty()
                            && this.selectedSuggestion >= autocompleteResult.suggestions().size()) {
                        this.selectedSuggestion = 0;
                    }
                    if (autocompleteResult.suggestions().isEmpty()) {
                        this.forced = false;
                    }
                    this.refreshPresentation();
                }
        );
    }

    private boolean shouldThrottle(int textLength) {
        if (textLength < VERY_LARGE_TEXT_THRESHOLD) {
            return false;
        }
        long now = System.currentTimeMillis();
        if (now - this.lastRequestAt < THROTTLE_HEAVY_MS) {
            return true;
        }
        this.lastRequestAt = now;
        return false;
    }

    private void refreshPresentation() {
        boolean suppressed = this.state.rawEditorAutocompleteDisabled
                || (!this.forced && this.suppressedCaret == this.editor.caretIndex());
        this.selectedSuggestion = RawAutocompleteUi.refresh(
                this.editor,
                this.result,
                this.selectedSuggestion,
                this.forced,
                suppressed
        );
    }

    private void resetAutoState() {
        this.caret = this.editor.caretIndex();
        this.virtualCaret = this.editor.hasVirtualCaret();
        this.forced = false;
        this.suppressedCaret = -1;
        this.selectedSuggestion = 0;
    }

    private void clear() {
        this.selectedSuggestion = 0;
        this.result = RawAutocompleteUtil.AutocompleteResult.empty(this.editor.caretIndex());
        this.editor.clearAutocompleteOverlay();
    }
}
