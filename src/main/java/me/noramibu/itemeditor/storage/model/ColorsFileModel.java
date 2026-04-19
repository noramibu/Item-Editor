package me.noramibu.itemeditor.storage.model;

import me.noramibu.itemeditor.storage.StorageConstants;

import java.util.ArrayList;
import java.util.List;

public final class ColorsFileModel {

    public int schemaVersion = StorageConstants.COLOR_SCHEMA_VERSION;
    public List<ColorPresetEntry> colors = new ArrayList<>();
}

