package me.noramibu.itemeditor.storage;

import me.noramibu.itemeditor.storage.model.RawEditorFileModel;
import me.noramibu.itemeditor.storage.model.RawEditorOptions;

public final class RawEditorOptionsService {

    private static final RawEditorOptionsService INSTANCE = new RawEditorOptionsService();

    private RawEditorOptionsService() {}

    public static RawEditorOptionsService instance() {
        return INSTANCE;
    }

    public synchronized RawEditorOptions load() {
        RawEditorFileModel model = StorageServices.foundation().loadRawEditor();
        return copy(model.options);
    }

    public synchronized void save(RawEditorOptions options) {
        RawEditorFileModel model = StorageServices.foundation().loadRawEditor();
        model.options = copy(options);
        StorageServices.foundation().saveRawEditor(model);
    }

    private static RawEditorOptions copy(RawEditorOptions source) {
        RawEditorOptions copy = new RawEditorOptions();
        if (source == null) {
            return copy;
        }
        copy.wordWrap = source.wordWrap;
        copy.searchMatchCase = source.searchMatchCase;
        copy.searchWholeWord = source.searchWholeWord;
        copy.searchRegex = source.searchRegex;
        copy.searchLiteralReplacement = source.searchLiteralReplacement;
        copy.searchDotAll = source.searchDotAll;
        copy.searchWrap = source.searchWrap;
        copy.searchWidth = Math.clamp(source.searchWidth, 240, 2000);
        copy.searchHeight = Math.clamp(source.searchHeight, 110, 2000);
        copy.searchHighlightAll = source.searchHighlightAll;
        copy.searchOpacity = Math.clamp(source.searchOpacity, 25, 100);
        copy.searchX = Double.isFinite(source.searchX) ? Math.clamp(source.searchX, 0, 1) : 1;
        copy.searchY = Double.isFinite(source.searchY) ? Math.clamp(source.searchY, 0, 1) : 0;
        copy.showDefaultKeys = source.showDefaultKeys;
        copy.autocompleteDisabled = source.autocompleteDisabled;
        copy.fontSizePercent = source.fontSizePercent <= 0 ? 100 : Math.clamp(source.fontSizePercent, 1, 500);
        return copy;
    }
}
