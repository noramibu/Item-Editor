package me.noramibu.itemeditor.storage.model;

import me.noramibu.itemeditor.storage.StorageConstants;

import java.util.ArrayList;
import java.util.List;

public final class ColorsFileModel {

    @SuppressWarnings("unused")
    public int schemaVersion = StorageConstants.COLOR_SCHEMA_VERSION;
    public List<ColorPresetEntry> color = new ArrayList<>();
    public List<ColorPresetEntry> gradient = new ArrayList<>();
    public List<ColorPresetEntry> shadow = new ArrayList<>();
}

