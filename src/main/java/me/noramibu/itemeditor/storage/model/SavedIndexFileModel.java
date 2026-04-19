package me.noramibu.itemeditor.storage.model;

import me.noramibu.itemeditor.storage.StorageConstants;

import java.util.ArrayList;
import java.util.List;

public final class SavedIndexFileModel {

    public int schemaVersion = StorageConstants.SAVED_INDEX_SCHEMA_VERSION;
    public int pageSize = StorageConstants.PAGE_SIZE;
    public int chunkSize = StorageConstants.CHUNK_SIZE;
    public List<SavedIndexItemEntry> items = new ArrayList<>();
}

