package me.noramibu.itemeditor.storage.model;

import me.noramibu.itemeditor.storage.StorageConstants;

public final class RawEditorFileModel {

    public int schemaVersion = StorageConstants.RAW_EDITOR_SCHEMA_VERSION;
    public RawEditorOptions options = new RawEditorOptions();
}

