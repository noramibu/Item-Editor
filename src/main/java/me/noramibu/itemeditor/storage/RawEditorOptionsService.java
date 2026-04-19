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
        RawEditorOptions options = copy(model.options);
        options.horizontalScroll = !options.wordWrap;
        return options;
    }

    public synchronized void save(RawEditorOptions options) {
        RawEditorFileModel model = StorageServices.foundation().loadRawEditor();
        model.options = copy(options);
        if (model.options.wordWrap) {
            model.options.horizontalScroll = false;
        }
        model.options.fontSizePercent = sanitizeFontSizePercent(model.options.fontSizePercent);
        StorageServices.foundation().saveRawEditor(model);
    }

    private static RawEditorOptions copy(RawEditorOptions source) {
        RawEditorOptions copy = new RawEditorOptions();
        if (source == null) {
            return copy;
        }
        copy.wordWrap = source.wordWrap;
        copy.horizontalScroll = source.horizontalScroll;
        copy.showDefaultKeys = source.showDefaultKeys;
        copy.fontSizePercent = sanitizeFontSizePercent(source.fontSizePercent);
        copy.horizontalScroll = !copy.wordWrap;
        return copy;
    }

    private static int sanitizeFontSizePercent(int requested) {
        if (requested <= 0) {
            return 100;
        }
        return Math.clamp(requested, 1, 500);
    }
}
