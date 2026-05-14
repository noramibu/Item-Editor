package me.noramibu.itemeditor.storage.model;

import java.util.List;

public final class ColorPresetEntry {

    public String id = "";
    public String name = "";
    public String value = "";
    public List<String> stops;
    public long createdAt;
    @SuppressWarnings("unused")
    public long updatedAt;
}

