package me.noramibu.itemeditor.storage.model;

import java.util.ArrayList;
import java.util.List;

public final class SavedIndexItemEntry {

    public String id = "";
    public String chunkId = "";
    public int slotInChunk = -1;
    public int page = 1;
    public int slotInPage = -1;
    public long savedAt;
    public long updatedAt;
    public String itemRegistryKey = "";
    public int stackCount = 1;
    public int nbtBytes = 0;
    public String customNamePlain = "";
    public List<String> lorePlain = new ArrayList<>();
}
