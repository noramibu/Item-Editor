package me.noramibu.itemeditor.storage;

import me.noramibu.itemeditor.storage.model.RawEditorFileModel;
import me.noramibu.itemeditor.storage.model.RawEditorOptions;

public final class RawEditorOptionsService {

    private static final RawEditorOptionsService INSTANCE = new RawEditorOptionsService();

    private RawEditorOptionsService() {
    }

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
        copy.showDefaultKeys = source.showDefaultKeys;
        copy.autocompleteDisabled = source.autocompleteDisabled;
        copy.fontSizePercent = source.fontSizePercent <= 0 ? 100 : Math.clamp(source.fontSizePercent, 1, 500);
        return copy;
    }
}
