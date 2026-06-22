package me.noramibu.itemeditor.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.noramibu.itemeditor.storage.io.AtomicFileUtil;
import me.noramibu.itemeditor.storage.io.GsonJsonCodec;
import me.noramibu.itemeditor.storage.model.ColorsFileModel;
import me.noramibu.itemeditor.storage.model.RawEditorFileModel;
import me.noramibu.itemeditor.storage.model.SavedIndexFileModel;
import me.noramibu.itemeditor.storage.model.SavedIndexItemEntry;
import me.noramibu.itemeditor.storage.model.SavedPageEntry;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class StorageDataFoundation {

    private static final Logger LOGGER = LoggerFactory.getLogger(StorageDataFoundation.class);
    private static final String DEFAULT_PAGE_NAME = "";
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private final StoragePaths paths;
    private final GsonJsonCodec<ColorsFileModel> colorsCodec;
    private final GsonJsonCodec<RawEditorFileModel> rawEditorCodec;

    private StorageDataFoundation(StoragePaths paths) {
        this.paths = paths;
        this.colorsCodec = new GsonJsonCodec<>(GSON, ColorsFileModel.class);
        this.rawEditorCodec = new GsonJsonCodec<>(GSON, RawEditorFileModel.class);
    }

    public static StorageDataFoundation create(Minecraft minecraft) {
        return new StorageDataFoundation(new StoragePaths(Objects.requireNonNull(minecraft, "minecraft")));
    }

    public void initialize() {
        try {
            AtomicFileUtil.ensureDirectories(
                    this.paths.root(),
                    this.paths.dataDirectory(),
                    this.paths.savedDirectory(),
                    this.paths.savedDataDirectory(),
                    this.paths.storageBackupsDirectory()
            );
        } catch (IOException exception) {
            LOGGER.error("[Item Editor] Failed to create item editor storage directories", exception);
            return;
        }

        ColorsFileModel colors = this.loadColors();
        RawEditorFileModel rawEditor = this.loadRawEditor();
        SavedIndexFileModel savedIndex = this.loadSavedIndex();

        this.saveColors(colors);
        this.saveRawEditor(rawEditor);
        this.saveSavedIndex(savedIndex);
    }

    public StoragePaths paths() {
        return this.paths;
    }

    public ColorsFileModel loadColors() {
        ColorsFileModel file = AtomicFileUtil.readJson(
                this.paths.colorsFile(),
                this.colorsCodec,
                ColorsFileModel::new
        );
        return sanitizeColors(file);
    }

    public void saveColors(ColorsFileModel model) {
        AtomicFileUtil.writeJson(
                this.paths.colorsFile(),
                this.colorsCodec,
                sanitizeColors(model)
        );
    }

    public RawEditorFileModel loadRawEditor() {
        RawEditorFileModel file = AtomicFileUtil.readJson(
                this.paths.rawEditorOptionsFile(),
                this.rawEditorCodec,
                RawEditorFileModel::new
        );
        return sanitizeRawEditor(file);
    }

    public void saveRawEditor(RawEditorFileModel model) {
        AtomicFileUtil.writeJson(
                this.paths.rawEditorOptionsFile(),
                this.rawEditorCodec,
                sanitizeRawEditor(model)
        );
    }

    public SavedIndexFileModel loadSavedIndex() {
        CompoundTag root = AtomicFileUtil.readNbt(
                this.paths.savedIndexFile(),
                CompoundTag::new
        );
        return sanitizeSavedIndex(savedIndexFromTag(root));
    }

    public void saveSavedIndex(SavedIndexFileModel model) {
        AtomicFileUtil.writeNbt(this.paths.savedIndexFile(), savedIndexToTag(sanitizeSavedIndex(model)));
    }

    private static SavedIndexFileModel savedIndexFromTag(CompoundTag root) {
        SavedIndexFileModel model = new SavedIndexFileModel();
        model.schemaVersion = root.getIntOr("schemaVersion", StorageConstants.SAVED_INDEX_SCHEMA_VERSION);
        model.pageSize = root.getIntOr("pageSize", StorageConstants.PAGE_SIZE);
        model.chunkSize = root.getIntOr("chunkSize", StorageConstants.CHUNK_SIZE);
        model.pages = new ArrayList<>();
        model.items = new ArrayList<>();

        ListTag pages = root.getListOrEmpty("pages");
        for (int index = 0; index < pages.size(); index++) {
            model.pages.add(savedPageEntryFromTag(pages.getCompoundOrEmpty(index)));
        }

        ListTag items = root.getListOrEmpty("items");
        for (int index = 0; index < items.size(); index++) {
            SavedIndexItemEntry entry = savedIndexEntryFromTag(items.getCompoundOrEmpty(index));
            model.items.add(entry);
        }
        return model;
    }

    private static CompoundTag savedIndexToTag(SavedIndexFileModel model) {
        CompoundTag root = new CompoundTag();
        root.putInt("schemaVersion", model.schemaVersion);
        root.putInt("pageSize", model.pageSize);
        root.putInt("chunkSize", model.chunkSize);

        ListTag pages = new ListTag();
        for (SavedPageEntry page : model.pages) {
            if (page == null || page.id == null || page.id.isBlank()) {
                continue;
            }
            pages.add(savedPageEntryToTag(page));
        }
        root.put("pages", pages);

        ListTag items = new ListTag();
        for (SavedIndexItemEntry entry : model.items) {
            if (entry == null || entry.id == null || entry.id.isBlank()) {
                continue;
            }
            items.add(savedIndexEntryToTag(entry));
        }
        root.put("items", items);
        return root;
    }

    private static SavedPageEntry savedPageEntryFromTag(CompoundTag pageTag) {
        SavedPageEntry page = new SavedPageEntry();
        page.id = pageTag.getStringOr("id", "");
        page.chunkId = pageTag.getStringOr("chunkId", "");
        page.order = pageTag.getIntOr("order", 0);
        page.name = pageTag.getStringOr("name", "");
        page.namePlain = pageTag.getStringOr("namePlain", "");
        page.createdAt = pageTag.getLongOr("createdAt", 0L);
        page.updatedAt = pageTag.getLongOr("updatedAt", 0L);
        return page;
    }

    private static CompoundTag savedPageEntryToTag(SavedPageEntry page) {
        CompoundTag pageTag = new CompoundTag();
        pageTag.putString("id", page.id == null ? "" : page.id);
        pageTag.putString("chunkId", page.chunkId == null ? "" : page.chunkId);
        pageTag.putInt("order", Math.max(0, page.order));
        pageTag.putString("name", page.name == null ? "" : page.name);
        pageTag.putString("namePlain", page.namePlain == null ? "" : page.namePlain);
        pageTag.putLong("createdAt", Math.max(0L, page.createdAt));
        pageTag.putLong("updatedAt", Math.max(0L, page.updatedAt));
        return pageTag;
    }

    private static SavedIndexItemEntry savedIndexEntryFromTag(CompoundTag entryTag) {
        SavedIndexItemEntry entry = new SavedIndexItemEntry();
        populateSavedIndexEntryScalars(entry, entryTag);
        entry.lorePlain = loreLinesFromTag(entryTag.getListOrEmpty("lorePlain"));
        return entry;
    }

    private static CompoundTag savedIndexEntryToTag(SavedIndexItemEntry entry) {
        CompoundTag entryTag = new CompoundTag();
        writeSavedIndexEntryScalars(entryTag, entry);
        entryTag.put("lorePlain", loreTagFromLines(entry.lorePlain));
        return entryTag;
    }

    private static void populateSavedIndexEntryScalars(SavedIndexItemEntry entry, CompoundTag entryTag) {
        entry.id = entryTag.getStringOr("id", "");
        entry.pageId = entryTag.getStringOr("pageId", "");
        entry.chunkId = entryTag.getStringOr("chunkId", "");
        entry.slotInChunk = entryTag.getIntOr("slotInChunk", -1);
        entry.page = entryTag.getIntOr("page", 1);
        entry.slotInPage = entryTag.getIntOr("slotInPage", entry.slotInChunk);
        entry.savedAt = entryTag.getLongOr("savedAt", 0L);
        entry.updatedAt = entryTag.getLongOr("updatedAt", 0L);
        entry.minecraftVersion = entryTag.getStringOr("minecraftVersion", currentMinecraftVersion());
        entry.dataVersion = entryTag.getIntOr("dataVersion", currentDataVersion());
        entry.itemRegistryKey = entryTag.getStringOr("itemRegistryKey", "");
        entry.stackCount = entryTag.getIntOr("stackCount", 1);
        entry.nbtBytes = Math.max(0, entryTag.getIntOr("nbtBytes", 0));
        entry.customNamePlain = entryTag.getStringOr("customNamePlain", "");
    }

    private static void writeSavedIndexEntryScalars(CompoundTag entryTag, SavedIndexItemEntry entry) {
        entryTag.putString("id", entry.id);
        entryTag.putString("pageId", entry.pageId == null ? "" : entry.pageId);
        entryTag.putString("chunkId", entry.chunkId == null ? "" : entry.chunkId);
        entryTag.putInt("slotInChunk", entry.slotInChunk);
        entryTag.putInt("page", entry.page);
        entryTag.putInt("slotInPage", entry.slotInPage);
        entryTag.putLong("savedAt", entry.savedAt);
        entryTag.putLong("updatedAt", entry.updatedAt);
        entryTag.putString(
                "minecraftVersion",
                entry.minecraftVersion == null || entry.minecraftVersion.isBlank()
                        ? currentMinecraftVersion()
                        : entry.minecraftVersion
        );
        entryTag.putInt("dataVersion", normalizedDataVersion(entry.dataVersion));
        entryTag.putString("itemRegistryKey", entry.itemRegistryKey == null ? "" : entry.itemRegistryKey);
        entryTag.putInt("stackCount", Math.max(1, entry.stackCount));
        entryTag.putInt("nbtBytes", Math.max(0, entry.nbtBytes));
        entryTag.putString("customNamePlain", entry.customNamePlain == null ? "" : entry.customNamePlain);
    }

    private static List<String> loreLinesFromTag(ListTag loreTag) {
        List<String> lines = new ArrayList<>();
        for (int loreIndex = 0; loreIndex < loreTag.size(); loreIndex++) {
            lines.add(loreTag.getStringOr(loreIndex, ""));
        }
        return lines;
    }

    private static ListTag loreTagFromLines(List<String> loreLines) {
        ListTag lore = new ListTag();
        if (loreLines == null) {
            return lore;
        }
        for (String line : loreLines) {
            lore.add(StringTag.valueOf(line == null ? "" : line));
        }
        return lore;
    }

    private static ColorsFileModel sanitizeColors(ColorsFileModel source) {
        ColorsFileModel model = source == null ? new ColorsFileModel() : source;
        model.schemaVersion = StorageConstants.COLOR_SCHEMA_VERSION;
        if (model.color == null) {
            model.color = new ArrayList<>();
        }
        if (model.gradient == null) {
            model.gradient = new ArrayList<>();
        }
        if (model.shadow == null) {
            model.shadow = new ArrayList<>();
        }
        return model;
    }

    private static RawEditorFileModel sanitizeRawEditor(RawEditorFileModel source) {
        RawEditorFileModel model = source == null ? new RawEditorFileModel() : source;
        model.schemaVersion = StorageConstants.RAW_EDITOR_SCHEMA_VERSION;
        if (model.options == null) {
            model.options = new me.noramibu.itemeditor.storage.model.RawEditorOptions();
        }
        return model;
    }

    private static SavedIndexFileModel sanitizeSavedIndex(SavedIndexFileModel source) {
        SavedIndexFileModel model = source == null ? new SavedIndexFileModel() : source;
        model.schemaVersion = StorageConstants.SAVED_INDEX_SCHEMA_VERSION;
        model.pageSize = StorageConstants.PAGE_SIZE;
        model.chunkSize = StorageConstants.CHUNK_SIZE;
        if (model.pages == null) {
            model.pages = new ArrayList<>();
        }
        if (model.items == null) {
            model.items = new ArrayList<>();
        }
        normalizeSavedPages(model);
        normalizeSavedItems(model);
        return model;
    }

    private static void normalizeSavedPages(SavedIndexFileModel model) {
        Set<String> ids = new HashSet<>();
        Set<String> chunkIds = new HashSet<>();
        List<SavedPageEntry> normalized = new ArrayList<>();
        for (SavedPageEntry page : model.pages) {
            if (page == null) {
                continue;
            }
            int order = Math.max(0, page.order);
            if (page.chunkId == null || page.chunkId.isBlank()) {
                page.chunkId = nextChunkId(chunkIds);
            }
            if (page.id == null || page.id.isBlank()) {
                page.id = page.chunkId;
            }
            if (!ids.add(page.id) || !chunkIds.add(page.chunkId)) {
                continue;
            }
            page.order = order;
            if (isGeneratedDefaultName(page.name, order)) {
                page.name = DEFAULT_PAGE_NAME;
            }
            if (page.namePlain == null || isGeneratedDefaultName(page.namePlain, order)) {
                page.namePlain = page.name;
            }
            normalized.add(page);
        }

        Map<Integer, SavedPageEntry> pagesByOrder = new LinkedHashMap<>();
        for (SavedPageEntry page : normalized) {
            pagesByOrder.put(page.order, page);
        }
        for (SavedIndexItemEntry entry : model.items) {
            if (entry == null) {
                continue;
            }
            int pageIndex = Math.max(0, entry.page - 1);
            if (!pagesByOrder.containsKey(pageIndex)) {
                SavedPageEntry page = defaultPage(pageIndex);
                page.chunkId = entry.chunkId == null || entry.chunkId.isBlank()
                        ? nextChunkId(chunkIds)
                        : entry.chunkId;
                page.id = page.chunkId;
                while (chunkIds.contains(page.chunkId) || ids.contains(page.id)) {
                    page.chunkId = nextChunkId(chunkIds);
                    page.id = page.chunkId;
                }
                pagesByOrder.put(pageIndex, page);
                normalized.add(page);
                chunkIds.add(page.chunkId);
                ids.add(page.id);
            }
        }
        normalized.sort(java.util.Comparator.comparingInt(page -> page.order));
        model.pages = normalized;
    }

    private static void normalizeSavedItems(SavedIndexFileModel model) {
        Map<String, SavedPageEntry> byId = new LinkedHashMap<>();
        Map<String, SavedPageEntry> byChunkId = new LinkedHashMap<>();
        for (SavedPageEntry page : model.pages) {
            byId.put(page.id, page);
            byChunkId.put(page.chunkId, page);
        }
        for (SavedIndexItemEntry entry : model.items) {
            if (entry == null) {
                continue;
            }
            SavedPageEntry page = byId.get(entry.pageId);
            if (page == null) {
                page = byChunkId.get(entry.chunkId);
            }
            if (page == null) {
                int index = Math.max(0, entry.page - 1);
                page = model.pages.get(Math.min(index, model.pages.size() - 1));
            }
            entry.pageId = page.id;
            entry.chunkId = page.chunkId;
            entry.page = page.order + 1;
            entry.slotInChunk = Math.clamp(entry.slotInChunk, 0, StorageConstants.CHUNK_SIZE - 1);
            entry.slotInPage = Math.clamp(entry.slotInPage, 0, StorageConstants.PAGE_SIZE - 1);
            if (entry.minecraftVersion == null || entry.minecraftVersion.isBlank()) {
                entry.minecraftVersion = currentMinecraftVersion();
            }
            entry.dataVersion = normalizedDataVersion(entry.dataVersion);
            if (entry.lorePlain == null) {
                entry.lorePlain = new ArrayList<>();
            }
        }
    }

    private static SavedPageEntry defaultPage(int index) {
        SavedPageEntry page = new SavedPageEntry();
        page.id = chunkId(index);
        page.chunkId = chunkId(index);
        page.order = Math.max(0, index);
        page.name = DEFAULT_PAGE_NAME;
        page.namePlain = page.name;
        return page;
    }

    private static boolean isGeneratedDefaultName(String name, int index) {
        return name == null || name.isBlank() || generatedDefaultPageName(index).equals(name);
    }

    private static String generatedDefaultPageName(int index) {
        return "Page " + (Math.max(0, index) + 1);
    }

    private static String nextChunkId(Set<String> usedChunkIds) {
        int index = 0;
        while (usedChunkIds.contains(chunkId(index))) {
            index++;
        }
        return chunkId(index);
    }

    private static String chunkId(int index) {
        return "chunk-" + Math.max(0, index);
    }

    private static String currentMinecraftVersion() {
        try {
            return SharedConstants.getCurrentVersion().id();
        } catch (RuntimeException ignored) {
            return "";
        }
    }

    private static int currentDataVersion() {
        try {
            return SharedConstants.getCurrentVersion().dataVersion().version();
        } catch (RuntimeException ignored) {
            return 0;
        }
    }

    private static int normalizedDataVersion(int dataVersion) {
        return dataVersion > 0 ? dataVersion : currentDataVersion();
    }
}
