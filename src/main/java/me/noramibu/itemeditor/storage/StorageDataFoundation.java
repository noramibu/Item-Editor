package me.noramibu.itemeditor.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.noramibu.itemeditor.storage.io.AtomicFileUtil;
import me.noramibu.itemeditor.storage.io.GsonJsonCodec;
import me.noramibu.itemeditor.storage.model.ColorsFileModel;
import me.noramibu.itemeditor.storage.model.RawEditorFileModel;
import me.noramibu.itemeditor.storage.model.SavedIndexFileModel;
import me.noramibu.itemeditor.storage.model.SavedIndexItemEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;

public final class StorageDataFoundation {

    private static final Logger LOGGER = LoggerFactory.getLogger(StorageDataFoundation.class);
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
                    this.paths.savedDataDirectory()
            );
        } catch (IOException exception) {
            LOGGER.error("Failed to create item editor storage directories", exception);
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
        model.items = new ArrayList<>();

        ListTag items = root.getListOrEmpty("items");
        for (int index = 0; index < items.size(); index++) {
            CompoundTag entryTag = items.getCompoundOrEmpty(index);
            SavedIndexItemEntry entry = new SavedIndexItemEntry();
            entry.id = entryTag.getStringOr("id", "");
            entry.chunkId = entryTag.getStringOr("chunkId", "");
            entry.slotInChunk = entryTag.getIntOr("slotInChunk", -1);
            entry.page = entryTag.getIntOr("page", 1);
            entry.slotInPage = entryTag.getIntOr("slotInPage", entry.slotInChunk);
            entry.savedAt = entryTag.getLongOr("savedAt", 0L);
            entry.updatedAt = entryTag.getLongOr("updatedAt", 0L);
            entry.itemRegistryKey = entryTag.getStringOr("itemRegistryKey", "");
            entry.stackCount = entryTag.getIntOr("stackCount", 1);
            entry.nbtBytes = Math.max(0, entryTag.getIntOr("nbtBytes", 0));
            entry.customNamePlain = entryTag.getStringOr("customNamePlain", "");
            entry.lorePlain = new ArrayList<>();
            ListTag lore = entryTag.getListOrEmpty("lorePlain");
            for (int loreIndex = 0; loreIndex < lore.size(); loreIndex++) {
                entry.lorePlain.add(lore.getStringOr(loreIndex, ""));
            }
            model.items.add(entry);
        }
        return model;
    }

    private static CompoundTag savedIndexToTag(SavedIndexFileModel model) {
        CompoundTag root = new CompoundTag();
        root.putInt("schemaVersion", model.schemaVersion);
        root.putInt("pageSize", model.pageSize);
        root.putInt("chunkSize", model.chunkSize);

        ListTag items = new ListTag();
        for (SavedIndexItemEntry entry : model.items) {
            if (entry == null || entry.id == null || entry.id.isBlank()) {
                continue;
            }
            CompoundTag entryTag = new CompoundTag();
            entryTag.putString("id", entry.id);
            entryTag.putString("chunkId", entry.chunkId == null ? "" : entry.chunkId);
            entryTag.putInt("slotInChunk", entry.slotInChunk);
            entryTag.putInt("page", entry.page);
            entryTag.putInt("slotInPage", entry.slotInPage);
            entryTag.putLong("savedAt", entry.savedAt);
            entryTag.putLong("updatedAt", entry.updatedAt);
            entryTag.putString("itemRegistryKey", entry.itemRegistryKey == null ? "" : entry.itemRegistryKey);
            entryTag.putInt("stackCount", Math.max(1, entry.stackCount));
            entryTag.putInt("nbtBytes", Math.max(0, entry.nbtBytes));
            entryTag.putString("customNamePlain", entry.customNamePlain == null ? "" : entry.customNamePlain);
            ListTag lore = new ListTag();
            if (entry.lorePlain != null) {
                for (String line : entry.lorePlain) {
                    lore.add(StringTag.valueOf(line == null ? "" : line));
                }
            }
            entryTag.put("lorePlain", lore);
            items.add(entryTag);
        }
        root.put("items", items);
        return root;
    }

    private static ColorsFileModel sanitizeColors(ColorsFileModel source) {
        ColorsFileModel model = source == null ? new ColorsFileModel() : source;
        model.schemaVersion = StorageConstants.COLOR_SCHEMA_VERSION;
        if (model.colors == null) {
            model.colors = new ArrayList<>();
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
        if (model.items == null) {
            model.items = new ArrayList<>();
        }
        return model;
    }
}
