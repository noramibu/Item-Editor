package me.noramibu.itemeditor.storage;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import me.noramibu.itemeditor.storage.io.AtomicFileUtil;
import me.noramibu.itemeditor.storage.model.SavedIndexEntryUtil;
import me.noramibu.itemeditor.storage.model.SavedIndexFileModel;
import me.noramibu.itemeditor.storage.model.SavedIndexItemEntry;
import me.noramibu.itemeditor.storage.model.SavedPageEntry;
import me.noramibu.itemeditor.storage.search.StorageSearchEngine;
import me.noramibu.itemeditor.storage.search.StorageSearchParser;
import me.noramibu.itemeditor.storage.search.StorageSearchQuery;
import me.noramibu.itemeditor.util.TextComponentUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class SavedItemStorageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SavedItemStorageService.class);
    private static final String CHUNK_PREFIX = "chunk-";
    private static final String DEFAULT_PAGE_NAME = "";
    private static final int MIN_ITEM_CACHE_SIZE = 128;
    private static final int MAX_ITEM_CACHE_SIZE = 1024;
    private static final int MIN_CHUNK_CACHE_SIZE = 24;
    private static final int MAX_CHUNK_CACHE_SIZE = 128;
    private static final int MIN_DECODE_MEMO_CACHE_SIZE = 64;
    private static final int MAX_DECODE_MEMO_CACHE_SIZE = 512;
    private static final int HOT_PAGE_CACHE_SIZE = 3;
    private static final int MAX_DECODE_THREADS = 4;
    private static final int AVAILABLE_CPUS = Math.max(1, Runtime.getRuntime().availableProcessors());
    private static final long MAX_MEMORY_MB = Runtime.getRuntime().maxMemory() / (1024L * 1024L);
    private static final int MAX_UNSYNCED_CHUNK_WRITES = 8;
    private static final long CHUNK_FSYNC_INTERVAL_MS = 1200L;
    private static final long INDEX_FLUSH_INTERVAL_MS = 350L;
    private static final long PREFETCH_DEBOUNCE_MS = 250L;

    private final StorageDataFoundation foundation;
    private final Map<String, SavedChunkCodec.SavedChunkData> chunkCache;
    private final Map<String, ItemStack> itemCache;
    private final SavedItemRuntimeCaches runtimeCaches;
    private final StorageItemBackupService backupService;
    private final Set<String> autoBackedUpDfuPages = ConcurrentHashMap.newKeySet();
    private final Set<String> storageDfuInProgress = ConcurrentHashMap.newKeySet();
    private final ReentrantReadWriteLock indexLock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock chunkLock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock itemCacheLock = new ReentrantReadWriteLock();
    private final Object prefetchStateLock = new Object();
    private final ExecutorService decodeExecutor;
    private final int decodeThreadCount;
    private final ExecutorService prefetchExecutor = newSingleDaemonExecutor("itemeditor-storage-prefetch");
    private final ExecutorService readExecutor = newSingleDaemonExecutor("itemeditor-storage-reads");
    private final ExecutorService writeExecutor = newSingleDaemonExecutor("itemeditor-storage-writes");
    private final Object writeQueueLock = new Object();
    private CompletableFuture<Void> writeQueue = CompletableFuture.completedFuture(null);
    private Throwable queuedWriteFailure;
    private final Object pendingMutationLock = new Object();
    private final Map<Integer, Map<Integer, PendingSlotMutation>> pendingMutationsByPage = new HashMap<>();
    private long pendingMutationSequence;
    private int lastPrefetchedPage = -1;
    private long lastPrefetchAt;
    private long prefetchGeneration;
    private SavedIndexFileModel indexCache;
    private boolean indexDirty;
    private PageStats pageStatsCache = new PageStats(0, 0, 0);
    private final Map<Integer, Integer> pageOccupancy = new HashMap<>();
    private int maxTrackedPage;
    private long lastIndexFlushAt;
    private int unsyncedChunkWrites;
    private long lastChunkFsyncAt;

    public SavedItemStorageService(StorageDataFoundation foundation) {
        this.foundation = foundation;
        this.chunkCache = lruCache(computeAdaptiveChunkCacheSize());
        this.itemCache = lruCache(computeAdaptiveItemCacheSize());
        this.runtimeCaches = new SavedItemRuntimeCaches(computeAdaptiveDecodeMemoCacheSize(), HOT_PAGE_CACHE_SIZE);
        this.backupService = new StorageItemBackupService(foundation.paths().storageBackupsDirectory());
        this.decodeThreadCount = computeDecodeThreadCount();
        this.decodeExecutor = Executors.newFixedThreadPool(
                this.decodeThreadCount,
                runnable -> newDaemonThread(runnable, "itemeditor-storage-decode")
        );
    }

    public Map<String, ItemStack> loadItems(List<SavedIndexItemEntry> entries, RegistryAccess registryAccess) {
        Map<String, ItemStack> loaded = new HashMap<>();
        if (entries == null || entries.isEmpty()) {
            return loaded;
        }
        this.backupPagesBeforeDfu(entries);
        int currentDataVersion = currentDataVersion();
        List<DecodeRequest> decodeRequests = new ArrayList<>();
        Map<String, SavedChunkCodec.SavedChunkData> chunkById = new HashMap<>();
        for (SavedIndexItemEntry entry : entries) {
            if (entry == null || entry.id == null || entry.id.isBlank()) {
                continue;
            }
            boolean outdated = currentDataVersion > 0 && entry.dataVersion > 0 && entry.dataVersion < currentDataVersion;
            ItemStack cached = withReadLock(this.itemCacheLock, () -> {
                ItemStack fromCache = this.itemCache.get(entry.id);
                return outdated || fromCache == null ? ItemStack.EMPTY : fromCache.copy();
            });
            if (!cached.isEmpty()) {
                loaded.put(entry.id, cached);
                continue;
            }
            if (entry.chunkId == null || entry.chunkId.isBlank()) {
                continue;
            }
            SavedChunkCodec.SavedChunkData chunk = chunkById.computeIfAbsent(entry.chunkId, this::readChunk);
            SavedChunkCodec.SavedChunkEntry chunkEntry = chunk.entries().get(entry.slotInChunk);
            if (chunkEntry == null) {
                continue;
            }
            CompoundTag itemTag = chunkEntry.itemTag().copy();
            SavedChunkCodec.DecodeKey key = SavedChunkCodec.decodeKey(itemTag);
            int dataVersion = Math.max(0, entry.dataVersion);
            decodeRequests.add(new DecodeRequest(
                    entry.id,
                    entry.chunkId,
                    entry.page,
                    entry.slotInPage,
                    entry.slotInChunk,
                    dataVersion,
                    itemTag,
                    key.tagHash(),
                    key.fingerprint() + "|dv=" + dataVersion
            ));
        }

        Map<String, ItemStack> decoded = this.decodeRequestsParallel(decodeRequests, registryAccess);
        this.withItemCacheWrite(() -> {
            for (Map.Entry<String, ItemStack> entry : decoded.entrySet()) {
                ItemStack stack = entry.getValue();
                if (stack.isEmpty()) {
                    continue;
                }
                this.itemCache.put(entry.getKey(), stack.copy());
                loaded.put(entry.getKey(), stack);
            }
        });
        return loaded;
    }

    private void backupPagesBeforeDfu(List<SavedIndexItemEntry> entries) {
        int currentDataVersion = currentDataVersion();
        if (currentDataVersion <= 0 || entries == null || entries.isEmpty()) {
            return;
        }
        Set<Integer> pages = new HashSet<>();
        for (SavedIndexItemEntry entry : entries) {
            if (entry != null && entry.dataVersion > 0 && entry.dataVersion < currentDataVersion) {
                pages.add(Math.max(1, entry.page));
            }
        }
        for (int page : pages) {
            String key = currentDataVersion + ":" + page;
            if (this.autoBackedUpDfuPages.add(key)) {
                this.backupPageSnapshot(page, "storage_page_pre_dfu", "Automatically backed up prior to running DFU");
            }
        }
    }

    private PageResult page(SavedIndexFileModel index, int requestedPage, StorageSortMode sortMode, boolean reverseSort) {
        this.syncPageMetadata(index);
        if (sortMode != StorageSortMode.REGULAR) {
            List<SavedIndexItemEntry> sorted = new ArrayList<>();
            for (SavedIndexItemEntry entry : index.items) {
                if (entry != null) {
                    sorted.add(copy(entry));
                }
            }
            sorted.sort(StorageSearchEngine.bySortMode(sortMode, reverseSort));
            return this.pagedResult(sorted, requestedPage, false);
        }

        int requested = clampMinPage(requestedPage);
        SavedPageEntry pageEntry = this.pageByNumberOrVirtual(index, requested);
        int maxPage = Math.max(requested, this.maxKnownPage(index));
        int page = pageEntry.order + 1;

        List<SavedIndexItemEntry> pageEntries = new ArrayList<>();
        for (SavedIndexItemEntry entry : index.items) {
            if (entry == null) {
                continue;
            }
            if (pageEntry.id.equals(entry.pageId)) {
                pageEntries.add(copy(entry));
            }
        }
        pageEntries.sort(Comparator.comparingInt((SavedIndexItemEntry entry) -> entry.slotInPage));
        return new PageResult(
                pageEntries,
                page,
                maxPage,
                false,
                index.items.size(),
                pageEntry.id,
                pageEntry.chunkId,
                pageEntry.name,
                pageEntry.namePlain
        );
    }

    private PageResult search(
            SavedIndexFileModel index,
            StorageSearchQuery query,
            int requestedPage,
            StorageSortMode sortMode,
            boolean reverseSort
    ) {
        List<SavedIndexItemEntry> matches = StorageSearchEngine.filterAndSort(
                index.items,
                query,
                sortMode,
                reverseSort,
                System.currentTimeMillis()
        );
        return this.pagedResult(matches, requestedPage, true);
    }

    public PageStats pageStats() {
        this.ensureIndexLoaded();
        return this.withIndexRead(() -> this.pageStatsCache);
    }

    public List<PageInfo> listPages(int includePage) {
        this.ensureIndexLoaded();
        return this.withIndexWrite(() -> {
            this.syncPageMetadata(this.indexCache);
            List<PageInfo> pages = new ArrayList<>();
            int maxPage = Math.max(Math.max(1, includePage), this.maxStoredPage(this.indexCache));
            for (int pageNumber = 1; pageNumber <= maxPage; pageNumber++) {
                SavedPageEntry page = this.pageByNumberOrVirtual(this.indexCache, pageNumber);
                pages.add(this.pageInfo(this.indexCache, page, !this.isPersistedPage(this.indexCache, page.id)));
            }
            return pages;
        });
    }

    public void enqueueRenamePage(String pageId, int pageNumber, String name) {
        String targetPageId = pageId == null ? "" : pageId;
        String targetName = name == null || name.isBlank() ? "" : name.trim();
        this.enqueueWrite(() -> this.withIndexWrite(() -> {
            this.ensureIndexLoaded();
            SavedPageEntry page = this.pageById(this.indexCache, targetPageId);
            if (page == null && targetName.isBlank()) {
                return;
            }
            if (page != null && targetName.isBlank() && this.isPlaceholderPage(this.indexCache, page)) {
                String emptyPageId = page.id;
                this.indexCache.pages.removeIf(candidate -> candidate != null && emptyPageId.equals(candidate.id));
                this.syncPageMetadata(this.indexCache);
                this.markIndexDirty();
                this.flushIndexNow();
                this.runtimeCaches.invalidateHotPageCache();
                return;
            }
            if (page == null) {
                page = this.ensurePersistentPageByNumber(this.indexCache, pageNumber);
            }
            long now = System.currentTimeMillis();
            if (page.createdAt <= 0L) {
                page.createdAt = now;
            }
            page.name = targetName;
            page.namePlain = targetName.isBlank() ? "" : TextComponentUtil.parseMarkup(targetName).getString();
            page.updatedAt = now;
            this.syncPageMetadata(this.indexCache);
            this.markIndexDirty();
            this.flushIndexNow();
            this.runtimeCaches.invalidateHotPageCache();
        }));
    }

    public int nextEmptyPageNumber(int includePage) {
        this.ensureIndexLoaded();
        return this.withIndexRead(() -> Math.max(Math.max(1, includePage), this.maxStoredPage(this.indexCache)) + 1);
    }

    public CompletableFuture<Integer> enqueueRemoveEmptyPages() {
        CompletableFuture<Integer> result = new CompletableFuture<>();
        this.enqueueWrite(() -> this.withIndexWrite(() -> {
            this.ensureIndexLoaded();
            Map<String, Boolean> occupied = new HashMap<>();
            for (SavedIndexItemEntry entry : this.indexCache.items) {
                if (entry != null && entry.pageId != null && !entry.pageId.isBlank()) {
                    occupied.put(entry.pageId, true);
                }
            }
            int before = this.indexCache.pages.size();
            this.indexCache.pages.removeIf(page -> page == null || !occupied.containsKey(page.id));
            this.indexCache.pages.sort(Comparator.comparingInt(page -> page.order));
            for (int index = 0; index < this.indexCache.pages.size(); index++) {
                this.indexCache.pages.get(index).order = index;
            }
            int removed = before - this.indexCache.pages.size();
            if (removed <= 0) {
                result.complete(0);
                return;
            }
            this.syncPageMetadata(this.indexCache);
            this.rebuildPageStats(this.indexCache.items);
            this.markIndexDirty();
            this.flushIndexNow();
            this.runtimeCaches.invalidateHotPageCache();
            result.complete(removed);
        }));
        return result;
    }

    public CompletableFuture<Integer> enqueueDuplicatePage(int pageNumber) {
        CompletableFuture<Integer> result = new CompletableFuture<>();
        this.enqueueWrite(() -> this.withIndexWrite(() -> {
            this.ensureIndexLoaded();
            SavedPageEntry sourcePage = this.pageByNumber(this.indexCache, pageNumber);
            if (sourcePage == null || this.isPlaceholderPage(this.indexCache, sourcePage)) {
                result.complete(-1);
                return;
            }
            SavedPageEntry targetPage = this.ensurePersistentPageByNumber(this.indexCache, this.maxKnownPage(this.indexCache) + 1);
            long now = System.currentTimeMillis();
            targetPage.name = sourcePage.name;
            targetPage.namePlain = sourcePage.namePlain;
            targetPage.createdAt = now;
            targetPage.updatedAt = now;

            SavedChunkCodec.SavedChunkData targetChunk = new SavedChunkCodec.SavedChunkData(targetPage.chunkId, new HashMap<>());
            Map<String, SavedChunkCodec.SavedChunkData> sourceChunks = new HashMap<>();
            for (SavedIndexItemEntry entry : new ArrayList<>(this.indexCache.items)) {
                if (entry == null || !sourcePage.id.equals(entry.pageId)) {
                    continue;
                }
                SavedChunkCodec.SavedChunkData sourceChunk = sourceChunks.computeIfAbsent(entry.chunkId, this::readChunk);
                SavedChunkCodec.SavedChunkEntry sourceEntry = sourceChunk.entries().get(entry.slotInChunk);
                if (sourceEntry == null) {
                    continue;
                }
                int slot = clampSlot(entry.slotInPage);
                String id = UUID.randomUUID().toString();
                targetChunk.entries().put(slot, new SavedChunkCodec.SavedChunkEntry(
                        id,
                        now,
                        now,
                        sourceEntry.itemTag().copy()
                ));
                SavedIndexItemEntry copied = copy(entry);
                copied.id = id;
                copied.pageId = targetPage.id;
                copied.chunkId = targetPage.chunkId;
                copied.page = targetPage.order + 1;
                copied.slotInChunk = slot;
                copied.slotInPage = slot;
                copied.savedAt = now;
                copied.updatedAt = now;
                copied.pageNamePlain = targetPage.namePlain;
                this.indexCache.items.add(copied);
                this.onIndexEntryAdded(copied);
            }
            this.withChunkWrite(() -> this.writeChunk(targetChunk));
            this.syncPageMetadata(this.indexCache);
            this.rebuildPageStats(this.indexCache.items);
            this.markIndexDirty();
            this.flushIndexNow();
            this.runtimeCaches.invalidateHotPageCache();
            result.complete(targetPage.order + 1);
        }));
        return result;
    }

    public CompletableFuture<Path> enqueueBackupPage(int pageNumber) {
        CompletableFuture<Path> result = new CompletableFuture<>();
        this.enqueueWrite(() -> {
            try {
                result.complete(this.backupPageSnapshot(
                        pageNumber,
                        "storage_page_manual_backup",
                        "Manually backed up by user"
                ));
            } catch (RuntimeException exception) {
                result.completeExceptionally(exception);
                throw exception;
            }
        });
        return result;
    }

    public CompletableFuture<StorageImportResult> enqueueImportPages(
            List<ExternalPageImport> pages,
            RegistryAccess registryAccess,
            Consumer<StorageImportProgress> progress
    ) {
        CompletableFuture<StorageImportResult> result = new CompletableFuture<>();
        RegistryAccess access = registryAccess == null ? RegistryAccess.EMPTY : registryAccess;
        List<ExternalPageImport> imports = pages == null ? List.of() : pages;
        this.enqueueWrite(() -> {
            try {
                this.withIndexWrite(() -> {
                    this.ensureIndexLoaded();
                    int importedPages = 0;
                    int importedItems = 0;
                    int nextPageNumber = this.maxKnownPage(this.indexCache) + 1;
                    long now = System.currentTimeMillis();
                    for (int importIndex = 0; importIndex < imports.size(); importIndex++) {
                        ExternalPageImport pageImport = imports.get(importIndex);
                        emitImportProgress(progress, "save_page", importIndex + 1, imports.size(), importedItems);
                        List<ExternalItemImport> items = pageImport == null || pageImport.items() == null
                                ? List.of()
                                : pageImport.items().stream()
                                        .filter(item -> item != null && !item.stack().isEmpty())
                                        .toList();
                        if (items.isEmpty()) {
                            continue;
                        }
                        SavedPageEntry page = this.ensurePersistentPageByNumber(this.indexCache, nextPageNumber++);
                        page.name = pageImport.name() == null ? "" : pageImport.name().trim();
                        page.namePlain = page.name.isBlank() ? "" : TextComponentUtil.parseMarkup(page.name).getString();
                        page.createdAt = now;
                        page.updatedAt = now;
                        SavedChunkCodec.SavedChunkData chunk = new SavedChunkCodec.SavedChunkData(page.chunkId, new HashMap<>());
                        for (ExternalItemImport item : items) {
                            if (importedItems % 18 == 0) {
                                emitImportProgress(progress, "save_items", importIndex + 1, imports.size(), importedItems);
                            }
                            int slot = clampSlot(item.slotInPage());
                            ItemStack stack = item.stack().copy();
                            CompoundTag itemTag = item.itemTag() == null
                                    ? this.encodeItemTag(stack, access)
                                    : item.itemTag().copy();
                            String id = UUID.randomUUID().toString();
                            chunk.entries().put(slot, new SavedChunkCodec.SavedChunkEntry(id, now, now, itemTag.copy()));
                            SavedIndexItemEntry entry = buildEntry(id, page, slot, now, now, stack, nbtByteSize(itemTag));
                            entry.dataVersion = item.dataVersion() > 0 ? item.dataVersion() : currentDataVersion();
                            this.indexCache.items.add(entry);
                            this.onIndexEntryAdded(entry);
                            this.withItemCacheWrite(() -> this.itemCache.put(id, stack.copy()));
                            importedItems++;
                        }
                        this.withChunkWrite(() -> this.writeChunk(chunk));
                        importedPages++;
                    }
                    if (importedPages > 0) {
                        emitImportProgress(progress, "finalize", importedPages, imports.size(), importedItems);
                        this.syncPageMetadata(this.indexCache);
                        this.rebuildPageStats(this.indexCache.items);
                        this.markIndexDirty();
                        this.flushIndexNow();
                        this.runtimeCaches.invalidateHotPageCache();
                    }
                    result.complete(new StorageImportResult(importedPages, importedItems));
                });
            } catch (RuntimeException exception) {
                result.completeExceptionally(exception);
                throw exception;
            }
        });
        return result;
    }

    private static void emitImportProgress(
            Consumer<StorageImportProgress> progress,
            String phase,
            int current,
            int total,
            int items
    ) {
        if (progress != null) {
            progress.accept(new StorageImportProgress(phase, current, total, items));
        }
    }

    public void enqueueMovePage(String pageId, int offset) {
        if (offset == 0) {
            return;
        }
        String targetPageId = pageId == null ? "" : pageId;
        this.enqueueWrite(() -> this.withIndexWrite(() -> {
            this.ensureIndexLoaded();
            int from = pageIndexById(this.indexCache, targetPageId);
            if (from < 0) {
                return;
            }
            SavedPageEntry page = this.indexCache.pages.get(from);
            int currentOrder = page.order;
            int targetOrder = Math.max(0, currentOrder + offset);
            SavedPageEntry swap = this.pageByNumber(this.indexCache, targetOrder + 1);
            if (swap != null && page.id.equals(swap.id)) {
                return;
            }
            page.order = targetOrder;
            if (swap != null) {
                if (this.isPlaceholderPage(this.indexCache, swap)) {
                    this.indexCache.pages.removeIf(candidate -> candidate != null && swap.id.equals(candidate.id));
                } else {
                    swap.order = currentOrder;
                }
            }
            this.syncPageMetadata(this.indexCache);
            this.rebuildPageStats(this.indexCache.items);
            this.markIndexDirty();
            this.flushIndexNow();
            this.runtimeCaches.invalidateHotPageCache();
        }));
    }

    public CompletableFuture<Boolean> enqueueDeletePageNumber(int pageNumber) {
        CompletableFuture<Boolean> result = new CompletableFuture<>();
        int targetOrder = Math.max(0, pageNumber - 1);
        this.enqueueWrite(() -> this.withIndexWrite(() -> {
            this.ensureIndexLoaded();
            SavedPageEntry page = this.pageByNumber(this.indexCache, targetOrder + 1);
            boolean placeholder = page == null || this.isPlaceholderPage(this.indexCache, page);
            if (!placeholder) {
                this.indexCache.items.removeIf(entry -> entry != null && page.id.equals(entry.pageId));
                this.withChunkWrite(() -> this.writeChunk(new SavedChunkCodec.SavedChunkData(page.chunkId, new HashMap<>())));
            } else if (targetOrder >= this.maxStoredPage(this.indexCache)) {
                result.complete(false);
                return;
            }
            if (page != null) {
                this.indexCache.pages.removeIf(candidate -> candidate != null && page.id.equals(candidate.id));
            }
            boolean shifted = false;
            for (SavedPageEntry candidate : this.indexCache.pages) {
                if (candidate != null && candidate.order > targetOrder) {
                    candidate.order--;
                    shifted = true;
                }
            }
            if (page == null && !shifted) {
                result.complete(false);
                return;
            }
            this.syncPageMetadata(this.indexCache);
            this.rebuildPageStats(this.indexCache.items);
            this.markIndexDirty();
            this.flushIndexNow();
            this.runtimeCaches.invalidateHotPageCache();
            result.complete(true);
        }));
        return result;
    }

    public void enqueueApplySlotMutations(
            int page,
            List<SlotMutation> mutations,
            RegistryAccess registryAccess
    ) {
        if (mutations == null || mutations.isEmpty()) {
            return;
        }
        int targetPage = Math.max(1, page);
        RegistryAccess access = registryAccess == null ? RegistryAccess.EMPTY : registryAccess;
        List<SlotMutation> sanitized = new ArrayList<>();
        for (SlotMutation mutation : mutations) {
            if (mutation == null) {
                continue;
            }
            ItemStack targetStack = mutation.targetStack.copy();
            sanitized.add(new SlotMutation(clampSlot(mutation.slotInPage), mutation.entryId, targetStack));
        }
        if (sanitized.isEmpty()) {
            return;
        }
        PendingMutationToken token = this.registerPendingMutations(targetPage, sanitized);
        this.enqueueWrite(() -> {
            try {
                this.applySlotMutations(targetPage, sanitized, access);
            } finally {
                this.clearPendingMutations(token);
            }
        });
    }

    public CompletableFuture<StorageReplaceResult> enqueueReplaceSavedItem(
            SavedIndexItemEntry originalEntry,
            ItemStack stack,
            RegistryAccess registryAccess
    ) {
        CompletableFuture<StorageReplaceResult> result = new CompletableFuture<>();
        if (originalEntry == null || originalEntry.id == null || originalEntry.id.isBlank()) {
            result.complete(StorageReplaceResult.failure("Original saved entry is missing."));
            return result;
        }
        ItemStack replacement = stack == null ? ItemStack.EMPTY : stack.copy();
        if (replacement.isEmpty()) {
            result.complete(StorageReplaceResult.failure("Cannot save an empty item back to storage."));
            return result;
        }
        RegistryAccess access = registryAccess == null ? RegistryAccess.EMPTY : registryAccess;
        SavedIndexItemEntry original = copy(originalEntry);
        this.enqueueWrite(() -> {
            try {
                this.ensureIndexLoaded();
                StorageReplaceResult writeResult = this.replaceSavedItemStrict(original, replacement, access);
                result.complete(writeResult);
            } catch (RuntimeException exception) {
                result.complete(StorageReplaceResult.failure(exception.getMessage() == null ? "Storage save failed." : exception.getMessage()));
            }
        });
        return result;
    }

    public void flushQueuedWrites() {
        CompletableFuture<Void> future;
        Throwable queuedFailure;
        synchronized (this.writeQueueLock) {
            future = this.writeQueue;
            queuedFailure = this.queuedWriteFailure;
            this.queuedWriteFailure = null;
        }
        Throwable joinFailure = null;
        try {
            future.join();
        } catch (CompletionException exception) {
            joinFailure = unwrapCompletion(exception);
        }
        Throwable failure = joinFailure != null ? joinFailure : queuedFailure;
        if (failure != null) {
            throw new IllegalStateException("Storage write queue failed", failure);
        }
        this.withIndexWrite(this::flushIndexNow);
        this.withChunkWrite(() -> {
            this.unsyncedChunkWrites = 0;
            this.lastChunkFsyncAt = System.currentTimeMillis();
        });
    }

    public boolean hasPendingWrites() {
        synchronized (this.writeQueueLock) {
            return !this.writeQueue.isDone();
        }
    }

    public CompletableFuture<PageSnapshot> loadSnapshotAsync(
            int requestedPage,
            String queryRaw,
            StorageSortMode sortMode,
            boolean reverseSort,
            RegistryAccess registryAccess
    ) {
        int page = Math.max(1, requestedPage);
        String query = queryRaw == null ? "" : queryRaw.trim();
        StorageSortMode mode = sortMode == null ? StorageSortMode.REGULAR : sortMode;
        RegistryAccess access = registryAccess == null ? RegistryAccess.EMPTY : registryAccess;
        return CompletableFuture.supplyAsync(
                () -> this.loadSnapshot(page, query, mode, reverseSort, access),
                this.readExecutor
        );
    }

    private static SavedIndexItemEntry buildEntry(
            String id,
            String chunkId,
            String pageId,
            int page,
            int slotInChunk,
            long savedAt,
            long updatedAt,
            ItemStack stack,
            int nbtBytes
    ) {
        SavedIndexItemEntry entry = new SavedIndexItemEntry();
        entry.id = id;
        entry.pageId = pageId == null ? "" : pageId;
        entry.chunkId = chunkId;
        entry.slotInChunk = slotInChunk;
        entry.page = Math.max(1, page);
        entry.slotInPage = slotInChunk;
        entry.savedAt = savedAt;
        entry.updatedAt = updatedAt;
        entry.minecraftVersion = currentMinecraftVersion();
        entry.dataVersion = currentDataVersion();
        entry.itemRegistryKey = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        entry.stackCount = Math.max(1, stack.getCount());
        entry.nbtBytes = Math.max(0, nbtBytes);
        Component customName = stack.get(DataComponents.CUSTOM_NAME);
        entry.customNamePlain = customName == null ? "" : customName.getString();
        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore != null) {
            for (Component line : lore.lines()) {
                entry.lorePlain.add(line.getString());
            }
        }
        return entry;
    }

    private static SavedIndexItemEntry buildEntry(
            String id,
            SavedPageEntry page,
            int slotInChunk,
            long savedAt,
            long updatedAt,
            ItemStack stack,
            int nbtBytes
    ) {
        return buildEntry(
                id,
                page.chunkId,
                page.id,
                page.order + 1,
                slotInChunk,
                savedAt,
                updatedAt,
                stack,
                nbtBytes
        );
    }

    private static SavedIndexItemEntry copy(SavedIndexItemEntry source) {
        return SavedIndexEntryUtil.copy(source);
    }

    private @Nullable SavedIndexItemEntry findEntry(List<SavedIndexItemEntry> entries, String id) {
        int index = findEntryIndexById(entries, id);
        return index < 0 ? null : entries.get(index);
    }

    private void replaceEntry(List<SavedIndexItemEntry> entries, SavedIndexItemEntry replacement) {
        int index = findEntryIndexById(entries, replacement.id);
        if (index != -1) {
            entries.set(index, replacement);
            return;
        }
        entries.add(replacement);
    }

    private static int findEntryIndexById(List<SavedIndexItemEntry> entries, String id) {
        for (int index = 0; index < entries.size(); index++) {
            SavedIndexItemEntry entry = entries.get(index);
            if (entry != null && id.equals(entry.id)) {
                return index;
            }
        }
        return -1;
    }

    private @Nullable SavedIndexItemEntry findEntryAtSlot(List<SavedIndexItemEntry> entries, String pageId, int slotInPage) {
        for (SavedIndexItemEntry entry : entries) {
            if (entry == null) {
                continue;
            }
            if (pageId.equals(entry.pageId) && entry.slotInPage == slotInPage) {
                return entry;
            }
        }
        return null;
    }

    private PageResult pagedResult(List<SavedIndexItemEntry> entries, int requestedPage, boolean searchMode) {
        int total = entries.size();
        int maxPage = Math.max(1, (int) Math.ceil(total / (double) StorageConstants.PAGE_SIZE));
        int page = clampPage(requestedPage, maxPage);
        int from = Math.min(total, (page - 1) * StorageConstants.PAGE_SIZE);
        int to = Math.min(total, from + StorageConstants.PAGE_SIZE);
        List<SavedIndexItemEntry> pageEntries = new ArrayList<>();
        for (int index = from; index < to; index++) {
            SavedIndexItemEntry entry = copy(entries.get(index));
            entry.slotInPage = index - from;
            pageEntries.add(entry);
        }
        return new PageResult(pageEntries, page, maxPage, searchMode, total, "", "", "", "");
    }

    private static int clampSlot(int slotInPage) {
        return Math.clamp(slotInPage, 0, StorageConstants.PAGE_SIZE - 1);
    }

    private PageSnapshot loadSnapshot(
            int requestedPage,
            String queryRaw,
            StorageSortMode sortMode,
            boolean reverseSort,
            RegistryAccess registryAccess
    ) {
        this.ensureIndexLoaded();
        String normalizedQuery = queryRaw == null ? "" : queryRaw.trim();
        StorageSortMode mode = sortMode == null ? StorageSortMode.REGULAR : sortMode;
        PageResult result = this.withIndexWrite(() -> {
            StorageSearchQuery query = StorageSearchParser.parse(normalizedQuery);
            return query.isEmpty()
                    ? this.page(this.indexCache, requestedPage, mode, reverseSort)
                    : this.search(this.indexCache, query, requestedPage, mode, reverseSort);
        });
        PageCacheKey cacheKey = new PageCacheKey(result.currentPage(), normalizedQuery, mode);
        long signature = SavedChunkCodec.entriesSignature(result.entries());
        Map<String, ItemStack> loadedStacks = this.runtimeCaches.hotPageStacks(cacheKey, signature);
        if (loadedStacks == null) {
            loadedStacks = this.loadItems(result.entries(), registryAccess);
            this.runtimeCaches.putHotPage(cacheKey, signature, loadedStacks);
        }
        Map<String, ItemStack> snapshotStacks = loadedStacks;
        return this.withIndexRead(() -> {
            PageResult overlayResult = this.applyPendingOverlay(result, normalizedQuery, mode, snapshotStacks);
            this.scheduleNeighborPrefetch(overlayResult.currentPage(), normalizedQuery, mode, registryAccess);
            return new PageSnapshot(overlayResult, snapshotStacks, this.pageStatsCache);
        });
    }

    private void applySlotMutations(
            int page,
            List<SlotMutation> mutations,
            RegistryAccess registryAccess
    ) {
        if (mutations.isEmpty()) {
            return;
        }
        this.ensureIndexLoaded();
        this.withIndexWrite(() -> {
            SavedIndexFileModel index = this.indexCache;
            SavedPageEntry pageEntry = this.pageByNumber(index, page);
            boolean needsPage = pageEntry != null;
            for (SlotMutation mutation : mutations) {
                if (mutation != null && !mutation.targetStack.isEmpty()) {
                    needsPage = true;
                    break;
                }
            }
            if (!needsPage) {
                return;
            }
            if (pageEntry == null) {
                pageEntry = this.ensurePersistentPageByNumber(index, page);
            }
            SavedPageEntry targetPageEntry = pageEntry;
            this.withChunkWrite(() -> {
                String targetChunkId = targetPageEntry.chunkId;
                SavedChunkCodec.SavedChunkData chunk = this.readChunk(targetChunkId);
                Map<Integer, SlotMutation> latestBySlot = new HashMap<>();
                for (SlotMutation mutation : mutations) {
                    latestBySlot.put(clampSlot(mutation.slotInPage), mutation);
                }

                boolean chunkChanged = false;
                boolean indexChanged = false;
                for (SlotMutation mutation : latestBySlot.values()) {
                    int slot = clampSlot(mutation.slotInPage);
                    ItemStack targetStack = mutation.targetStack;
                    SavedIndexItemEntry existing = this.resolveMutationEntry(index.items, targetPageEntry.id, slot, mutation.entryId);

                    if (targetStack.isEmpty()) {
                        if (existing == null) {
                            continue;
                        }
                        SavedChunkCodec.SavedChunkData existingChunk = this.readChunk(existing.chunkId);
                        if (existingChunk.entries().remove(existing.slotInChunk) != null) {
                            if (existing.chunkId.equals(targetChunkId)) {
                                chunk = existingChunk;
                                chunkChanged = true;
                            } else {
                                this.writeChunk(existingChunk);
                            }
                        }
                        if (index.items.removeIf(candidate -> candidate != null && existing.id.equals(candidate.id))) {
                            this.onIndexEntryRemoved(existing);
                            indexChanged = true;
                        }
                        this.withItemCacheWrite(() -> this.itemCache.remove(existing.id));
                        continue;
                    }

                    CompoundTag encodedItemTag = this.encodeItemTag(targetStack, registryAccess);
                    int encodedNbtBytes = nbtByteSize(encodedItemTag);
                    long now = System.currentTimeMillis();
                    if (existing != null) {
                        SavedChunkCodec.SavedChunkData existingChunk = this.readChunk(existing.chunkId);
                        SavedChunkCodec.SavedChunkEntry current = existingChunk.entries().get(existing.slotInChunk);
                        long savedAt = current == null ? existing.savedAt : current.savedAt();
                        if (savedAt == 0L) {
                            savedAt = now;
                        }
                        existingChunk.entries().put(existing.slotInChunk, new SavedChunkCodec.SavedChunkEntry(existing.id, savedAt, now, encodedItemTag.copy()));
                        if (existing.chunkId.equals(targetChunkId)) {
                            chunk = existingChunk;
                            chunkChanged = true;
                        } else {
                            this.writeChunk(existingChunk);
                        }
                        SavedPageEntry existingPage = this.pageById(index, existing.pageId);
                        SavedIndexItemEntry refreshed = buildEntry(
                                existing.id,
                                existingPage == null ? targetPageEntry : existingPage,
                                existing.slotInChunk,
                                savedAt,
                                now,
                                targetStack,
                                encodedNbtBytes
                        );
                        this.replaceEntry(index.items, refreshed);
                        this.withItemCacheWrite(() -> this.itemCache.put(existing.id, targetStack.copy()));
                        indexChanged = true;
                        continue;
                    }

                    String id = UUID.randomUUID().toString();
                    chunk.entries().put(slot, new SavedChunkCodec.SavedChunkEntry(id, now, now, encodedItemTag.copy()));
                    SavedIndexItemEntry created = buildEntry(id, targetPageEntry, slot, now, now, targetStack, encodedNbtBytes);
                    index.items.add(created);
                    this.onIndexEntryAdded(created);
                    this.withItemCacheWrite(() -> this.itemCache.put(id, targetStack.copy()));
                    chunkChanged = true;
                    indexChanged = true;
                }

                if (chunkChanged) {
                    this.writeChunk(chunk);
                }
                if (indexChanged) {
                    this.pruneEmptyDefaultPages(index);
                    this.syncPageMetadata(index);
                    this.rebuildPageStats(index.items);
                    this.markIndexDirty();
                    this.flushIndexIfDue();
                }
                if (chunkChanged || indexChanged) {
                    this.runtimeCaches.invalidateHotPageCache();
                }
            });
        });
    }

    private StorageReplaceResult replaceSavedItemStrict(
            SavedIndexItemEntry originalEntry,
            ItemStack replacement,
            RegistryAccess registryAccess
    ) {
        return this.withIndexWrite(() -> {
            SavedIndexFileModel index = this.indexCache;
            SavedIndexItemEntry existing = this.findEntry(index.items, originalEntry.id);
            if (existing == null) {
                return StorageReplaceResult.failure("Original saved entry is missing.");
            }
            SavedPageEntry existingPage = this.pageById(index, existing.pageId);
            if (existingPage == null) {
                existingPage = this.ensurePersistentPageByNumber(index, existing.page);
            }
            SavedPageEntry pageEntry = existingPage;
            this.withChunkWrite(() -> {
                CompoundTag encodedItemTag = this.encodeItemTag(replacement, registryAccess);
                int encodedNbtBytes = nbtByteSize(encodedItemTag);
                long now = System.currentTimeMillis();
                SavedChunkCodec.SavedChunkData existingChunk = this.readChunk(existing.chunkId);
                SavedChunkCodec.SavedChunkEntry current = existingChunk.entries().get(existing.slotInChunk);
                long savedAt = current == null ? existing.savedAt : current.savedAt();
                if (savedAt == 0L) {
                    savedAt = now;
                }
                existingChunk.entries().put(existing.slotInChunk, new SavedChunkCodec.SavedChunkEntry(existing.id, savedAt, now, encodedItemTag.copy()));
                this.writeChunk(existingChunk);
                SavedIndexItemEntry refreshed = buildEntry(existing.id, pageEntry, existing.slotInChunk, savedAt, now, replacement, encodedNbtBytes);
                this.replaceEntry(index.items, refreshed);
                this.withItemCacheWrite(() -> this.itemCache.put(existing.id, replacement.copy()));
                this.markIndexDirty();
                this.flushIndexIfDue();
                this.runtimeCaches.invalidateHotPageCache();
            });
                return StorageReplaceResult.ok();
        });
    }

    private SavedIndexItemEntry resolveMutationEntry(
            List<SavedIndexItemEntry> entries,
            String pageId,
            int slotInPage,
            String entryId
    ) {
        if (entryId != null && !entryId.isBlank()) {
            SavedIndexItemEntry byId = this.findEntry(entries, entryId);
            if (byId != null) {
                return byId;
            }
        }
        return this.findEntryAtSlot(entries, pageId, slotInPage);
    }

    private void ensureIndexLoaded() {
        if (this.indexCache != null) {
            return;
        }
        this.withIndexWrite(() -> {
            if (this.indexCache == null) {
                this.indexCache = this.foundation.loadSavedIndex();
                this.syncPageMetadata(this.indexCache);
                this.rebuildPageStats(this.indexCache.items);
                this.indexDirty = false;
                if (this.backfillMissingNbtBytes(this.indexCache)) {
                    this.markIndexDirty();
                    this.flushIndexNow();
                }
            }
        });
    }

    private void syncPageMetadata(SavedIndexFileModel index) {
        if (index == null) {
            return;
        }
        if (index.pages == null) {
            index.pages = new ArrayList<>();
        }
        if (index.items == null) {
            index.items = new ArrayList<>();
        }
        for (int pageIndex = 0; pageIndex < index.pages.size(); pageIndex++) {
            SavedPageEntry page = index.pages.get(pageIndex);
            if (page == null) {
                page = defaultPage(pageIndex);
                index.pages.set(pageIndex, page);
            }
            page.order = Math.max(0, page.order);
            if (page.chunkId == null || page.chunkId.isBlank()) {
                page.chunkId = nextChunkId(index);
            }
            if (page.id == null || page.id.isBlank()) {
                page.id = page.chunkId;
            }
            if (isGeneratedDefaultName(page.name, page.order)) {
                page.name = DEFAULT_PAGE_NAME;
            }
            if (page.namePlain == null || isGeneratedDefaultName(page.namePlain, page.order)) {
                page.namePlain = page.name.isBlank() ? "" : TextComponentUtil.parseMarkup(page.name).getString();
            }
        }
        index.pages.sort(Comparator.comparingInt(page -> page.order));

        Map<String, SavedPageEntry> byId = pagesById(index);
        Map<String, SavedPageEntry> byChunkId = pagesByChunkId(index);
        for (SavedIndexItemEntry entry : index.items) {
            if (entry == null) {
                continue;
            }
            SavedPageEntry page = byId.get(entry.pageId);
            if (page == null) {
                page = byChunkId.get(entry.chunkId);
            }
            if (page == null) {
                page = defaultPage(Math.max(0, entry.page - 1));
                page.chunkId = entry.chunkId == null || entry.chunkId.isBlank()
                        ? nextChunkId(index)
                        : entry.chunkId;
                page.id = page.chunkId;
                index.pages.add(page);
                byId.put(page.id, page);
                byChunkId.put(page.chunkId, page);
            }
            entry.pageId = page.id;
            entry.chunkId = page.chunkId;
            entry.page = page.order + 1;
            entry.pageNamePlain = page.namePlain;
        }
    }

    private SavedPageEntry ensurePersistentPageByNumber(SavedIndexFileModel index, int pageNumber) {
        this.syncPageMetadata(index);
        int targetPage = Math.max(1, pageNumber);
        SavedPageEntry existing = this.pageByNumber(index, targetPage);
        if (existing != null) {
            return existing;
        }
        SavedPageEntry page = defaultPage(targetPage - 1);
        page.chunkId = nextChunkId(index);
        page.id = page.chunkId;
        page.createdAt = System.currentTimeMillis();
        index.pages.add(page);
        this.markIndexDirty();
        this.syncPageMetadata(index);
        return page;
    }

    private @Nullable SavedPageEntry pageByNumber(SavedIndexFileModel index, int pageNumber) {
        if (index == null || index.pages == null) {
            return null;
        }
        int targetOrder = Math.max(1, pageNumber) - 1;
        for (SavedPageEntry page : index.pages) {
            if (page != null && page.order == targetOrder) {
                return page;
            }
        }
        return null;
    }

    private SavedPageEntry pageByNumberOrVirtual(SavedIndexFileModel index, int pageNumber) {
        SavedPageEntry page = this.pageByNumber(index, pageNumber);
        return page == null ? virtualPage(pageNumber) : page;
    }

    private boolean isPersistedPage(SavedIndexFileModel index, String pageId) {
        return this.pageById(index, pageId) != null;
    }

    private @Nullable SavedPageEntry pageById(SavedIndexFileModel index, String pageId) {
        if (index == null || pageId == null || pageId.isBlank()) {
            return null;
        }
        this.syncPageMetadata(index);
        for (SavedPageEntry page : index.pages) {
            if (pageId.equals(page.id)) {
                return page;
            }
        }
        return null;
    }

    private static int pageIndexById(SavedIndexFileModel index, String pageId) {
        if (index == null || index.pages == null || pageId == null || pageId.isBlank()) {
            return -1;
        }
        for (int indexValue = 0; indexValue < index.pages.size(); indexValue++) {
            SavedPageEntry page = index.pages.get(indexValue);
            if (page != null && pageId.equals(page.id)) {
                return indexValue;
            }
        }
        return -1;
    }

    private static Map<String, SavedPageEntry> pagesById(SavedIndexFileModel index) {
        Map<String, SavedPageEntry> pages = new LinkedHashMap<>();
        for (SavedPageEntry page : index.pages) {
            pages.put(page.id, page);
        }
        return pages;
    }

    private static Map<String, SavedPageEntry> pagesByChunkId(SavedIndexFileModel index) {
        Map<String, SavedPageEntry> pages = new LinkedHashMap<>();
        for (SavedPageEntry page : index.pages) {
            pages.put(page.chunkId, page);
        }
        return pages;
    }

    private static SavedPageEntry virtualPage(int pageNumber) {
        int pageIndex = Math.max(1, pageNumber) - 1;
        SavedPageEntry page = defaultPage(pageIndex);
        page.id = "virtual-page-" + (pageIndex + 1);
        page.chunkId = page.id;
        return page;
    }

    private int maxKnownPage(SavedIndexFileModel index) {
        return Math.max(1, this.maxStoredPage(index));
    }

    private int maxStoredPage(SavedIndexFileModel index) {
        int max = 0;
        if (index != null && index.pages != null) {
            for (SavedPageEntry page : index.pages) {
                if (page != null) {
                    max = Math.max(max, page.order + 1);
                }
            }
        }
        max = Math.max(max, this.maxTrackedPage);
        return max;
    }

    private static SavedPageEntry defaultPage(int index) {
        SavedPageEntry page = new SavedPageEntry();
        page.id = chunkId(index);
        page.chunkId = page.id;
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

    private static String nextChunkId(SavedIndexFileModel index) {
        int chunkIndex = 0;
        Map<String, SavedPageEntry> existing = pagesByChunkId(index);
        while (existing.containsKey(chunkId(chunkIndex))) {
            chunkIndex++;
        }
        return chunkId(chunkIndex);
    }

    private boolean backfillMissingNbtBytes(SavedIndexFileModel index) {
        if (index == null || index.items == null || index.items.isEmpty()) {
            return false;
        }
        boolean changed = false;
        Map<String, SavedChunkCodec.SavedChunkData> chunkById = new HashMap<>();
        for (SavedIndexItemEntry entry : index.items) {
            if (entry == null || entry.chunkId == null || entry.chunkId.isBlank()) {
                continue;
            }
            if (entry.nbtBytes > 0) {
                continue;
            }
            SavedChunkCodec.SavedChunkData chunk = chunkById.computeIfAbsent(entry.chunkId, this::readChunk);
            SavedChunkCodec.SavedChunkEntry chunkEntry = chunk.entries().get(entry.slotInChunk);
            if (chunkEntry == null) {
                continue;
            }
            int bytes = nbtByteSize(chunkEntry.itemTag());
            if (bytes <= 0) {
                continue;
            }
            entry.nbtBytes = bytes;
            changed = true;
        }
        return changed;
    }

    private void rebuildPageStats(List<SavedIndexItemEntry> entries) {
        this.pageOccupancy.clear();
        this.maxTrackedPage = 0;
        for (SavedIndexItemEntry entry : entries) {
            if (entry == null) {
                continue;
            }
            int page = Math.max(1, entry.page);
            this.pageOccupancy.merge(page, 1, Integer::sum);
            this.maxTrackedPage = Math.max(this.maxTrackedPage, page);
        }
        this.updatePageStatsCache();
    }

    private void onIndexEntryAdded(SavedIndexItemEntry entry) {
        if (entry == null) {
            return;
        }
        int page = Math.max(1, entry.page);
        this.pageOccupancy.merge(page, 1, Integer::sum);
        this.maxTrackedPage = Math.max(this.maxTrackedPage, page);
        this.updatePageStatsCache();
    }

    private void onIndexEntryRemoved(SavedIndexItemEntry entry) {
        if (entry == null) {
            return;
        }
        int page = Math.max(1, entry.page);
        Integer count = this.pageOccupancy.get(page);
        if (count != null) {
            if (count == 1) {
                this.pageOccupancy.remove(page);
            } else {
                this.pageOccupancy.put(page, count - 1);
            }
        }
        if (page == this.maxTrackedPage && !this.pageOccupancy.containsKey(page)) {
            int max = 0;
            for (int candidate : this.pageOccupancy.keySet()) {
                max = Math.max(max, candidate);
            }
            this.maxTrackedPage = max;
        }
        this.updatePageStatsCache();
    }

    private void updatePageStatsCache() {
        int storedPages = this.indexCache == null
                ? Math.max(0, this.maxTrackedPage)
                : this.maxStoredPage(this.indexCache);
        int occupiedPages = this.pageOccupancy.size();
        int emptyPages = Math.max(0, storedPages - occupiedPages);
        this.pageStatsCache = new PageStats(storedPages, occupiedPages, emptyPages);
    }

    private PageInfo pageInfo(SavedIndexFileModel index, SavedPageEntry page, boolean virtualPage) {
        int pageNumber = page.order + 1;
        int itemCount = 0;
        int nbtBytes = 0;
        long savedAt = 0L;
        long updatedAt = 0L;
        if (!virtualPage) {
            for (SavedIndexItemEntry entry : index.items) {
                if (entry == null || !page.id.equals(entry.pageId)) {
                    continue;
                }
                itemCount++;
                nbtBytes += Math.max(0, entry.nbtBytes);
                if (entry.savedAt > 0L) {
                    savedAt = savedAt == 0L ? entry.savedAt : Math.min(savedAt, entry.savedAt);
                }
                updatedAt = Math.max(updatedAt, entry.updatedAt);
            }
        }
        if (savedAt == 0L) {
            savedAt = page.createdAt > 0L ? page.createdAt : page.updatedAt;
        }
        return new PageInfo(
                page.id,
                page.chunkId,
                pageNumber,
                page.name,
                page.namePlain,
                itemCount,
                nbtBytes,
                savedAt,
                Math.max(updatedAt, page.updatedAt),
                virtualPage,
                virtualPage || this.isPlaceholderPage(index, page)
        );
    }

    private boolean isPlaceholderPage(SavedIndexFileModel index, SavedPageEntry page) {
        if (page == null || page.id == null || page.id.isBlank()) {
            return true;
        }
        for (SavedIndexItemEntry entry : index.items) {
            if (entry != null && page.id.equals(entry.pageId)) {
                return false;
            }
        }
        return true;
    }

    private void pruneEmptyDefaultPages(SavedIndexFileModel index) {
        if (index == null || index.pages == null || index.pages.isEmpty()) {
            return;
        }
        Map<String, Boolean> occupied = new HashMap<>();
        for (SavedIndexItemEntry entry : index.items) {
            if (entry != null && entry.pageId != null && !entry.pageId.isBlank()) {
                occupied.put(entry.pageId, true);
            }
        }
        index.pages.removeIf(page -> page != null
                && !occupied.containsKey(page.id)
                && page.updatedAt <= 0L
                && DEFAULT_PAGE_NAME.equals(page.name)
                && DEFAULT_PAGE_NAME.equals(page.namePlain));
    }

    private void markIndexDirty() {
        this.indexDirty = true;
    }

    private void flushIndexIfDue() {
        if (!this.indexDirty) {
            return;
        }
        long now = System.currentTimeMillis();
        if ((now - this.lastIndexFlushAt) >= INDEX_FLUSH_INTERVAL_MS) {
            this.flushIndexNow();
        }
    }

    private void flushIndexNow() {
        if (!this.indexDirty || this.indexCache == null) {
            return;
        }
        this.foundation.saveSavedIndex(this.indexCache);
        this.indexDirty = false;
        this.lastIndexFlushAt = System.currentTimeMillis();
    }

    private PendingMutationToken registerPendingMutations(int page, List<SlotMutation> mutations) {
        Map<Integer, Long> sequenceBySlot = new HashMap<>();
        synchronized (this.pendingMutationLock) {
            Map<Integer, PendingSlotMutation> pagePending = this.pendingMutationsByPage.computeIfAbsent(page, ignored -> new HashMap<>());
            for (SlotMutation mutation : mutations) {
                long sequence = ++this.pendingMutationSequence;
                int slot = clampSlot(mutation.slotInPage);
                pagePending.put(slot, new PendingSlotMutation(sequence, mutation));
                sequenceBySlot.put(slot, sequence);
            }
        }
        return new PendingMutationToken(page, sequenceBySlot);
    }

    private void clearPendingMutations(PendingMutationToken token) {
        if (token == null || token.slotSequences() == null || token.slotSequences().isEmpty()) {
            return;
        }
        synchronized (this.pendingMutationLock) {
            Map<Integer, PendingSlotMutation> pagePending = this.pendingMutationsByPage.get(token.page());
            if (pagePending == null) {
                return;
            }
            for (Map.Entry<Integer, Long> slotEntry : token.slotSequences().entrySet()) {
                PendingSlotMutation pending = pagePending.get(slotEntry.getKey());
                if (pending != null && pending.sequence() == slotEntry.getValue()) {
                    pagePending.remove(slotEntry.getKey());
                }
            }
            if (pagePending.isEmpty()) {
                this.pendingMutationsByPage.remove(token.page());
            }
        }
    }

    private Map<Integer, SlotMutation> pendingMutationsForPage(int page) {
        synchronized (this.pendingMutationLock) {
            Map<Integer, PendingSlotMutation> pending = this.pendingMutationsByPage.get(page);
            if (pending == null || pending.isEmpty()) {
                return Map.of();
            }
            Map<Integer, SlotMutation> copy = new HashMap<>();
            for (Map.Entry<Integer, PendingSlotMutation> entry : pending.entrySet()) {
                copy.put(entry.getKey(), entry.getValue().mutation());
            }
            return copy;
        }
    }

    private PageResult applyPendingOverlay(
            PageResult base,
            String queryRaw,
            StorageSortMode sortMode,
            Map<String, ItemStack> loadedStacks
    ) {
        if (base == null || sortMode != StorageSortMode.REGULAR || (queryRaw != null && !queryRaw.isBlank())) {
            return base;
        }
        Map<Integer, SlotMutation> pending = this.pendingMutationsForPage(base.currentPage());
        if (pending.isEmpty()) {
            return base;
        }

        Map<Integer, SavedIndexItemEntry> bySlot = new HashMap<>();
        for (SavedIndexItemEntry entry : base.entries()) {
            bySlot.put(clampSlot(entry.slotInPage), copy(entry));
        }

        for (Map.Entry<Integer, SlotMutation> pendingEntry : pending.entrySet()) {
            int slot = clampSlot(pendingEntry.getKey());
            SlotMutation mutation = pendingEntry.getValue();
            ItemStack stack = mutation.targetStack.copy();
            SavedIndexItemEntry existing = bySlot.get(slot);
            if (stack.isEmpty()) {
                if (existing != null) {
                    bySlot.remove(slot);
                    loadedStacks.remove(existing.id);
                }
                continue;
            }
            String id = (existing != null && existing.id != null && !existing.id.isBlank())
                    ? existing.id
                    : pendingId(base.currentPage(), slot);
            SavedIndexItemEntry overlay = buildEntry(
                    id,
                    base.pageChunkId(),
                    base.pageId(),
                    base.currentPage(),
                    slot,
                    System.currentTimeMillis(),
                    System.currentTimeMillis(),
                    stack,
                    existing == null ? 0 : existing.nbtBytes
            );
            bySlot.put(slot, overlay);
            loadedStacks.put(id, stack);
        }

        List<SavedIndexItemEntry> entries = new ArrayList<>(bySlot.values());
        entries.sort(Comparator.comparingInt(entry -> entry.slotInPage));
        return new PageResult(
                entries,
                base.currentPage(),
                base.maxPage(),
                base.searchMode(),
                base.totalResults(),
                base.pageId(),
                base.pageChunkId(),
                base.pageName(),
                base.pageNamePlain()
        );
    }

    private void scheduleNeighborPrefetch(int currentPage, String queryRaw, StorageSortMode sortMode, RegistryAccess registryAccess) {
        if (sortMode != StorageSortMode.REGULAR || (queryRaw != null && !queryRaw.isBlank())) {
            return;
        }
        long now = System.currentTimeMillis();
        long generation;
        synchronized (this.prefetchStateLock) {
            if (this.lastPrefetchedPage == currentPage && (now - this.lastPrefetchAt) < PREFETCH_DEBOUNCE_MS) {
                return;
            }
            this.lastPrefetchedPage = currentPage;
            this.lastPrefetchAt = now;
            this.prefetchGeneration++;
            generation = this.prefetchGeneration;
        }
        RegistryAccess access = registryAccess == null ? RegistryAccess.EMPTY : registryAccess;
        int previous = Math.max(1, currentPage - 1);
        int next = currentPage + 1;
        this.prefetchExecutor.execute(() -> this.prefetchPage(generation, previous, access));
        this.prefetchExecutor.execute(() -> this.prefetchPage(generation, next, access));
    }

    public void prewarmOnOpen(int currentPage, String queryRaw, StorageSortMode sortMode, RegistryAccess registryAccess) {
        if (sortMode != StorageSortMode.REGULAR || (queryRaw != null && !queryRaw.isBlank())) {
            return;
        }
        long generation;
        synchronized (this.prefetchStateLock) {
            this.prefetchGeneration++;
            generation = this.prefetchGeneration;
        }
        RegistryAccess access = registryAccess == null ? RegistryAccess.EMPTY : registryAccess;
        int page = Math.max(1, currentPage);
        this.prefetchExecutor.execute(() -> this.prefetchPage(generation, page, access));
        this.prefetchExecutor.execute(() -> this.prefetchPage(generation, page + 1, access));
    }

    private void prefetchPage(long generation, int page, RegistryAccess registryAccess) {
        synchronized (this.prefetchStateLock) {
            if (generation != this.prefetchGeneration) {
                return;
            }
        }
        this.ensureIndexLoaded();
        PageResult result = this.withIndexWrite(() -> this.page(this.indexCache, Math.max(1, page), StorageSortMode.REGULAR, false));
        synchronized (this.prefetchStateLock) {
            if (generation != this.prefetchGeneration) {
                return;
            }
        }
        if (!result.entries().isEmpty()) {
            this.loadItems(result.entries(), registryAccess);
        }
    }

    private static String pendingId(int page, int slotInPage) {
        return "pending-" + page + "-" + slotInPage;
    }

    private Map<String, ItemStack> decodeRequestsParallel(List<DecodeRequest> requests, RegistryAccess registryAccess) {
        Map<String, ItemStack> decoded = new HashMap<>();
        RegistryAccess access = registryAccess == null ? RegistryAccess.EMPTY : registryAccess;
        Map<SavedChunkCodec.DecodeKey, DecodeRequest> uniqueRequests = new HashMap<>();
        Map<SavedChunkCodec.DecodeKey, List<DecodeRequest>> requestsByKey = new HashMap<>();
        int currentDataVersion = currentDataVersion();

        for (DecodeRequest request : requests) {
            boolean outdated = currentDataVersion > 0 && request.dataVersion() > 0 && request.dataVersion() < currentDataVersion;
            SavedChunkCodec.DecodeKey key = new SavedChunkCodec.DecodeKey(
                    request.tagHash(),
                    outdated ? request.tagFingerprint() + "|entry=" + request.id() : request.tagFingerprint()
            );
            ItemStack memoized = outdated ? null : this.runtimeCaches.memoizedDecodedStack(key);
            if (memoized != null && !memoized.isEmpty()) {
                decoded.put(request.id(), memoized);
                continue;
            }
            uniqueRequests.putIfAbsent(key, request);
            requestsByKey.computeIfAbsent(key, ignored -> new ArrayList<>()).add(request);
        }
        if (uniqueRequests.isEmpty()) {
            return decoded;
        }

        Map<SavedChunkCodec.DecodeKey, DecodedItem> decodedUnique = new HashMap<>();
        if (this.decodeThreadCount == 1 || uniqueRequests.size() == 1) {
            for (Map.Entry<SavedChunkCodec.DecodeKey, DecodeRequest> entry : uniqueRequests.entrySet()) {
                DecodedItem item = this.decodeItemTag(entry.getValue(), access);
                if (!item.stack().isEmpty()) {
                    decodedUnique.put(entry.getKey(), item);
                }
            }
        } else {
            List<CompletableFuture<DecodedByKey>> futures = new ArrayList<>(uniqueRequests.size());
            for (Map.Entry<SavedChunkCodec.DecodeKey, DecodeRequest> entry : uniqueRequests.entrySet()) {
                SavedChunkCodec.DecodeKey key = entry.getKey();
                DecodeRequest request = entry.getValue();
                futures.add(CompletableFuture.supplyAsync(
                        () -> new DecodedByKey(key, this.decodeItemTag(request, access)),
                        this.decodeExecutor
                ));
            }
            for (CompletableFuture<DecodedByKey> future : futures) {
                DecodedByKey decodedByKey = future.join();
                if (decodedByKey != null && decodedByKey.item() != null && !decodedByKey.item().stack().isEmpty()) {
                    decodedUnique.put(decodedByKey.key(), decodedByKey.item());
                }
            }
        }

        for (Map.Entry<SavedChunkCodec.DecodeKey, DecodedItem> entry : decodedUnique.entrySet()) {
            SavedChunkCodec.DecodeKey key = entry.getKey();
            DecodedItem item = entry.getValue();
            ItemStack stack = item.stack();
            this.runtimeCaches.storeMemoizedDecodedStack(key, stack);
            List<DecodeRequest> groupedRequests = requestsByKey.get(key);
            if (groupedRequests == null) {
                continue;
            }
            for (DecodeRequest request : groupedRequests) {
                decoded.put(request.id(), stack.copy());
                if (item.upgradedItemTag() != null) {
                    this.persistDataVersionUpgrade(request, item.upgradedItemTag(), item.dataVersion());
                }
            }
        }
        return decoded;
    }

    private static int computeAdaptiveDecodeMemoCacheSize() {
        int byMemory = MAX_MEMORY_MB >= 4096L ? 384 : MAX_MEMORY_MB >= 2048L ? 256 : MAX_MEMORY_MB >= 1024L ? 128 : MIN_DECODE_MEMO_CACHE_SIZE;
        int byCpu = MIN_DECODE_MEMO_CACHE_SIZE + ((Math.min(AVAILABLE_CPUS, 8) - 1) * 16);
        return clampInt(Math.max(byMemory, byCpu), MIN_DECODE_MEMO_CACHE_SIZE, MAX_DECODE_MEMO_CACHE_SIZE);
    }

    private static int computeAdaptiveItemCacheSize() {
        int byMemory = MAX_MEMORY_MB >= 4096L ? 768 : MAX_MEMORY_MB >= 2048L ? 512 : MAX_MEMORY_MB >= 1024L ? 256 : MIN_ITEM_CACHE_SIZE;
        int byCpu = MIN_ITEM_CACHE_SIZE + ((Math.min(AVAILABLE_CPUS, 8) - 1) * 32);
        return clampInt(Math.max(byMemory, byCpu), MIN_ITEM_CACHE_SIZE, MAX_ITEM_CACHE_SIZE);
    }

    private static int computeAdaptiveChunkCacheSize() {
        int byMemory = MAX_MEMORY_MB >= 4096L ? 96 : MAX_MEMORY_MB >= 2048L ? 64 : MAX_MEMORY_MB >= 1024L ? 32 : MIN_CHUNK_CACHE_SIZE;
        int byCpu = MIN_CHUNK_CACHE_SIZE + ((Math.min(AVAILABLE_CPUS, 8) - 1) * 4);
        return clampInt(Math.max(byMemory, byCpu), MIN_CHUNK_CACHE_SIZE, MAX_CHUNK_CACHE_SIZE);
    }

    private static int computeDecodeThreadCount() {
        int threads = clampInt(AVAILABLE_CPUS - 1, 1, MAX_DECODE_THREADS);
        if (MAX_MEMORY_MB < 1024L) {
            threads = Math.min(threads, 2);
        }
        return threads;
    }

    private static int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private <T> T withIndexRead(Supplier<T> action) {
        return withReadLock(this.indexLock, action);
    }

    private <T> T withIndexWrite(Supplier<T> action) {
        return withWriteLock(this.indexLock, action);
    }

    private void withIndexWrite(Runnable action) {
        withWriteLock(this.indexLock, action);
    }

    private <T> T withChunkWrite(Supplier<T> action) {
        return withWriteLock(this.chunkLock, action);
    }

    private void withChunkWrite(Runnable action) {
        withWriteLock(this.chunkLock, action);
    }

    private void withItemCacheWrite(Runnable action) {
        withWriteLock(this.itemCacheLock, action);
    }

    private static <T> T withReadLock(ReentrantReadWriteLock lock, Supplier<T> action) {
        lock.readLock().lock();
        try {
            return action.get();
        } finally {
            lock.readLock().unlock();
        }
    }

    private static <T> T withWriteLock(ReentrantReadWriteLock lock, Supplier<T> action) {
        lock.writeLock().lock();
        try {
            return action.get();
        } finally {
            lock.writeLock().unlock();
        }
    }

    private static void withWriteLock(ReentrantReadWriteLock lock, Runnable action) {
        lock.writeLock().lock();
        try {
            action.run();
        } finally {
            lock.writeLock().unlock();
        }
    }

    private SavedChunkCodec.SavedChunkData readChunk(String chunkId) {
        Optional<SavedChunkCodec.SavedChunkData> cached =
                this.withChunkWrite(() -> Optional.ofNullable(this.chunkCache.get(chunkId)));
        if (cached.isPresent()) {
            return cached.get();
        }

        CompoundTag root = AtomicFileUtil.readNbt(this.foundation.paths().chunkFile(chunkId), CompoundTag::new);
        SavedChunkCodec.SavedChunkData parsed = SavedChunkCodec.fromTag(root, chunkId);
        return this.withChunkWrite(() -> {
            SavedChunkCodec.SavedChunkData existing = this.chunkCache.get(chunkId);
            if (existing != null) {
                return existing;
            }
            this.chunkCache.put(chunkId, parsed);
            return parsed;
        });
    }

    private void writeChunk(SavedChunkCodec.SavedChunkData chunk) {
        CompoundTag root = SavedChunkCodec.toTag(chunk);
        long now = System.currentTimeMillis();
        boolean shouldFsync = this.withChunkWrite(() ->
                this.unsyncedChunkWrites >= (MAX_UNSYNCED_CHUNK_WRITES - 1)
                        || (now - this.lastChunkFsyncAt) >= CHUNK_FSYNC_INTERVAL_MS
        );
        AtomicFileUtil.writeNbt(this.foundation.paths().chunkFile(chunk.chunkId()), root, shouldFsync);
        this.withChunkWrite(() -> {
            if (shouldFsync) {
                this.unsyncedChunkWrites = 0;
                this.lastChunkFsyncAt = now;
            } else {
                this.unsyncedChunkWrites++;
            }
            this.chunkCache.put(chunk.chunkId(), chunk);
        });
    }

    private CompoundTag encodeItemTag(ItemStack stack, RegistryAccess registryAccess) {
        DataResult<Tag> encoded = ItemStack.CODEC.encodeStart(registryAccess.createSerializationContext(NbtOps.INSTANCE), stack);
        Tag tag = encoded.result().orElse(null);
        if (tag instanceof CompoundTag compound) {
            return compound;
        }
        throw new IllegalStateException(encoded.error().map(DataResult.Error::message).orElse("Failed to encode item stack"));
    }

    private DecodedItem decodeItemTag(DecodeRequest request, RegistryAccess registryAccess) {
        int currentDataVersion = currentDataVersion();
        int sourceDataVersion = request.dataVersion() > 0 ? request.dataVersion() : currentDataVersion;
        CompoundTag itemTag = request.itemTag();
        CompoundTag decodeTag = itemTag;
        boolean outdated = sourceDataVersion > 0 && currentDataVersion > 0 && sourceDataVersion < currentDataVersion;
        String dfuUpdateKey = "";
        String preUpdateBackup = "";
        boolean releaseDfuUpdateKey = false;
        try {
            if (outdated) {
                dfuUpdateKey = storageDfuUpdateKey(request, sourceDataVersion, currentDataVersion);
                if (!this.storageDfuInProgress.add(dfuUpdateKey)) {
                    return new DecodedItem(ItemStack.EMPTY, null, sourceDataVersion);
                }
                releaseDfuUpdateKey = true;
                preUpdateBackup = this.backupStorageItem(
                        "storage_dfu_pre_update",
                        request,
                        itemTag,
                        sourceDataVersion,
                        currentDataVersion,
                        "Automatically backed up prior to running DFU"
                );
                try {
                    Tag fixed = Minecraft.getInstance().getFixerUpper().update(
                            References.ITEM_STACK,
                            new Dynamic<>(NbtOps.INSTANCE, itemTag.copy()),
                            sourceDataVersion,
                            currentDataVersion
                    ).getValue();
                    if (fixed instanceof CompoundTag fixedCompound) {
                        decodeTag = fixedCompound;
                    } else {
                        this.removeFailedStorageItem(request);
                        this.notifyStorageDfu(
                                "Removed saved item after DFU returned invalid data: page "
                                        + Math.max(1, request.page())
                                        + ", slot "
                                        + Math.max(1, request.slotInPage() + 1),
                                ChatFormatting.RED
                        );
                        LOGGER.warn(
                                "[Item Editor] Stored item DFU returned invalid data [page={}] [slot={}] [item={}] [fromDv={}] [toDv={}]{}",
                                Math.max(1, request.page()),
                                Math.max(1, request.slotInPage() + 1),
                                itemId(itemTag),
                                sourceDataVersion,
                                currentDataVersion,
                                backupLogSuffix(preUpdateBackup)
                        );
                        return new DecodedItem(ItemStack.EMPTY, null, sourceDataVersion);
                    }
                } catch (RuntimeException exception) {
                    this.removeFailedStorageItem(request);
                    this.notifyStorageDfu(
                            "Removed saved item after DFU failed: page "
                                    + Math.max(1, request.page())
                                    + ", slot "
                                    + Math.max(1, request.slotInPage() + 1),
                            ChatFormatting.RED
                    );
                    LOGGER.warn(
                            "[Item Editor] Stored item DFU failed [page={}] [slot={}] [item={}] [fromDv={}] [toDv={}] [reason={}]{}",
                            Math.max(1, request.page()),
                            Math.max(1, request.slotInPage() + 1),
                            itemId(itemTag),
                            sourceDataVersion,
                            currentDataVersion,
                            errorMessage(exception),
                            backupLogSuffix(preUpdateBackup)
                    );
                    return new DecodedItem(ItemStack.EMPTY, null, sourceDataVersion);
                }
            }
            DataResult<ItemStack> decoded = ItemStack.CODEC.parse(
                    registryAccess.createSerializationContext(NbtOps.INSTANCE),
                    decodeTag
            );
            ItemStack stack = decoded.result().map(ItemStack::copy).orElse(ItemStack.EMPTY);
            if (stack.isEmpty()) {
                String reason = decoded.error().map(DataResult.Error::message).orElse("decode failed");
                String backup = outdated
                        ? ""
                        : this.backupStorageItem(
                                "storage_decode_failed",
                                request,
                                itemTag,
                                sourceDataVersion,
                                currentDataVersion,
                                reason
                        );
                this.removeFailedStorageItem(request);
                this.notifyStorageDfu(
                        "Removed saved item after decode failed: page "
                                + Math.max(1, request.page())
                                + ", slot "
                                + Math.max(1, request.slotInPage() + 1),
                        ChatFormatting.RED
                );
                decoded.error().ifPresent(error -> LOGGER.warn(
                        "[Item Editor] Stored item decode failed [page={}] [slot={}] [item={}] [dv={}] [reason={}]{}",
                        Math.max(1, request.page()),
                        Math.max(1, request.slotInPage() + 1),
                        itemId(itemTag),
                        sourceDataVersion,
                        error.message(),
                        backupLogSuffix(backup)
                ));
                return new DecodedItem(ItemStack.EMPTY, null, sourceDataVersion);
            }
            if (!outdated) {
                return new DecodedItem(stack, null, sourceDataVersion);
            }
            try {
                this.notifyStorageDfu(
                        "Updated saved item with DFU: page "
                                + Math.max(1, request.page())
                                + ", slot "
                                + Math.max(1, request.slotInPage() + 1)
                                + backupChatSuffix(preUpdateBackup),
                        ChatFormatting.YELLOW
                );
                releaseDfuUpdateKey = false;
                return new DecodedItem(stack, this.encodeItemTag(stack, registryAccess), currentDataVersion);
            } catch (RuntimeException exception) {
                this.removeFailedStorageItem(request);
                this.notifyStorageDfu(
                        "Removed saved item after DFU re-encode failed: page "
                                + Math.max(1, request.page())
                                + ", slot "
                                + Math.max(1, request.slotInPage() + 1),
                        ChatFormatting.RED
                );
                LOGGER.warn(
                        "[Item Editor] Updated stored item re-encode failed [page={}] [slot={}] [item={}] [fromDv={}] [reason={}]{}",
                        Math.max(1, request.page()),
                        Math.max(1, request.slotInPage() + 1),
                        itemId(itemTag),
                        sourceDataVersion,
                        errorMessage(exception),
                        ""
                );
                return new DecodedItem(ItemStack.EMPTY, null, sourceDataVersion);
            }
        } finally {
            if (releaseDfuUpdateKey) {
                this.storageDfuInProgress.remove(dfuUpdateKey);
            }
        }
    }

    private void persistDataVersionUpgrade(DecodeRequest request, CompoundTag upgradedItemTag, int dataVersion) {
        if (request == null || upgradedItemTag == null || dataVersion <= 0) {
            return;
        }
        try {
            this.withIndexWrite(() -> {
                this.ensureIndexLoaded();
                SavedIndexItemEntry entry = this.findEntry(this.indexCache.items, request.id());
                if (entry == null
                        || entry.dataVersion >= dataVersion
                        || !Objects.equals(entry.chunkId, request.chunkId())
                        || entry.slotInChunk != request.slotInChunk()) {
                    return;
                }
                SavedChunkCodec.SavedChunkData chunk = this.readChunk(entry.chunkId);
                SavedChunkCodec.SavedChunkEntry chunkEntry = chunk.entries().get(entry.slotInChunk);
                if (chunkEntry == null || !Objects.equals(chunkEntry.id(), entry.id)) {
                    return;
                }
                chunk.entries().put(
                        entry.slotInChunk,
                        new SavedChunkCodec.SavedChunkEntry(
                                chunkEntry.id(),
                                chunkEntry.savedAt(),
                                chunkEntry.updatedAt(),
                                upgradedItemTag.copy()
                        )
                );
                entry.dataVersion = dataVersion;
                entry.nbtBytes = nbtByteSize(upgradedItemTag);
                this.writeChunk(chunk);
                this.replaceEntry(this.indexCache.items, entry);
                this.markIndexDirty();
                this.flushIndexIfDue();
            });
        } finally {
            this.storageDfuInProgress.remove(storageDfuUpdateKey(request, request.dataVersion(), dataVersion));
        }
    }

    private void removeFailedStorageItem(DecodeRequest request) {
        if (request == null || request.id() == null || request.id().isBlank()) {
            return;
        }
        this.withIndexWrite(() -> {
            this.ensureIndexLoaded();
            SavedIndexItemEntry entry = this.findEntry(this.indexCache.items, request.id());
            if (entry == null) {
                return;
            }
            SavedChunkCodec.SavedChunkData chunk = this.readChunk(entry.chunkId);
            if (chunk.entries().remove(entry.slotInChunk) != null) {
                this.writeChunk(chunk);
            }
            if (this.indexCache.items.removeIf(candidate -> candidate != null && entry.id.equals(candidate.id))) {
                this.onIndexEntryRemoved(entry);
                this.withItemCacheWrite(() -> this.itemCache.remove(entry.id));
                this.rebuildPageStats(this.indexCache.items);
                this.markIndexDirty();
                this.flushIndexIfDue();
                this.runtimeCaches.invalidateHotPageCache();
            }
        });
    }

    private void notifyStorageDfu(String message, ChatFormatting color) {
        Minecraft minecraft = Minecraft.getInstance();
        if (message == null || message.isBlank()) {
            return;
        }
        minecraft.execute(() -> {
            if (minecraft.player != null) {
                minecraft.player.displayClientMessage(Component.literal(prefixedMessage(message)).withStyle(color), false);
            }
        });
    }

    private static String prefixedMessage(String message) {
        return message == null || message.startsWith("[Item Editor] ") ? message : "[Item Editor] " + message;
    }

    private static String storageDfuUpdateKey(DecodeRequest request, int sourceDataVersion, int targetDataVersion) {
        if (request == null) {
            return "";
        }
        return request.id()
                + "|"
                + sourceDataVersion
                + "->"
                + targetDataVersion;
    }

    private String backupStorageItem(
            String reason,
            DecodeRequest request,
            CompoundTag itemTag,
            int sourceDataVersion,
            int targetDataVersion,
            String message
    ) {
        if (request == null || itemTag == null) {
            return "";
        }
        Path backup = this.backupService.backup(new StorageItemBackupService.BackupEvent(
                "item",
                reason,
                "storage",
                Math.max(1, request.page()),
                Math.max(1, request.slotInPage() + 1),
                request.chunkId(),
                request.slotInChunk(),
                request.id(),
                sourceDataVersion,
                targetDataVersion,
                request.tagFingerprint(),
                "",
                -1,
                -1,
                message
        ), itemTag);
        return backup == null || backup.getFileName() == null ? "" : backup.getFileName().toString();
    }

    private static String backupLogSuffix(String backupFile) {
        return backupFile == null || backupFile.isBlank() ? "" : " [backup=" + backupFile + "]";
    }

    private static String backupChatSuffix(String backupFile) {
        return backupFile == null || backupFile.isBlank() ? "" : "; backup " + backupFile;
    }

    private Path backupPageSnapshot(int pageNumber, String reason, String note) {
        return this.withIndexWrite(() -> {
            this.ensureIndexLoaded();
            SavedPageEntry page = this.pageByNumberOrVirtual(this.indexCache, pageNumber);
            CompoundTag pageTag = this.pageBackupTag(this.indexCache, page, note);
            return this.backupService.backup(new StorageItemBackupService.BackupEvent(
                    "pages",
                    reason,
                    "storage_page",
                    page.order + 1,
                    -1,
                    page.chunkId,
                    -1,
                    page.id,
                    this.pageSourceDataVersion(page),
                    currentDataVersion(),
                    "",
                    "",
                    -1,
                    -1,
                    note
            ), pageTag);
        });
    }

    private CompoundTag pageBackupTag(SavedIndexFileModel index, SavedPageEntry page, String note) {
        CompoundTag root = new CompoundTag();
        root.putInt("schemaVersion", 1);
        root.putString("backupType", "storage_page");
        root.putString("note", note == null ? "" : note);
        root.putString("minecraftVersion", currentMinecraftVersion());
        root.putInt("dataVersion", currentDataVersion());
        root.putLong("backupAt", System.currentTimeMillis());

        CompoundTag pageTag = new CompoundTag();
        pageTag.putString("id", page.id == null ? "" : page.id);
        pageTag.putString("chunkId", page.chunkId == null ? "" : page.chunkId);
        pageTag.putInt("page", page.order + 1);
        pageTag.putString("name", page.name == null ? "" : page.name);
        pageTag.putString("namePlain", page.namePlain == null ? "" : page.namePlain);
        pageTag.putLong("createdAt", Math.max(0L, page.createdAt));
        pageTag.putLong("updatedAt", Math.max(0L, page.updatedAt));
        root.put("page", pageTag);

        ListTag itemTags = new ListTag();
        SavedChunkCodec.SavedChunkData chunk = page.chunkId == null || page.chunkId.isBlank()
                ? new SavedChunkCodec.SavedChunkData("", new HashMap<>())
                : this.readChunk(page.chunkId);
        for (SavedIndexItemEntry entry : index.items) {
            if (entry == null || !page.id.equals(entry.pageId)) {
                continue;
            }
            SavedChunkCodec.SavedChunkEntry chunkEntry = chunk.entries().get(entry.slotInChunk);
            if (chunkEntry == null) {
                continue;
            }
            CompoundTag entryTag = new CompoundTag();
            entryTag.putString("id", entry.id == null ? "" : entry.id);
            entryTag.putInt("slotInPage", entry.slotInPage);
            entryTag.putInt("slotInChunk", entry.slotInChunk);
            entryTag.putLong("savedAt", Math.max(0L, entry.savedAt));
            entryTag.putLong("updatedAt", Math.max(0L, entry.updatedAt));
            entryTag.putString("minecraftVersion", entry.minecraftVersion == null ? "" : entry.minecraftVersion);
            entryTag.putInt("dataVersion", Math.max(0, entry.dataVersion));
            entryTag.putString("itemRegistryKey", entry.itemRegistryKey == null ? "" : entry.itemRegistryKey);
            entryTag.putInt("stackCount", Math.max(1, entry.stackCount));
            entryTag.putInt("nbtBytes", Math.max(0, entry.nbtBytes));
            entryTag.put("item", chunkEntry.itemTag().copy());
            itemTags.add(entryTag);
        }
        root.put("items", itemTags);
        return root;
    }

    private int pageSourceDataVersion(SavedPageEntry page) {
        int version = 0;
        if (page == null || this.indexCache == null) {
            return version;
        }
        for (SavedIndexItemEntry entry : this.indexCache.items) {
            if (entry == null || !page.id.equals(entry.pageId) || entry.dataVersion <= 0) {
                continue;
            }
            version = version == 0 ? entry.dataVersion : Math.min(version, entry.dataVersion);
        }
        return version;
    }

    private static int nbtByteSize(CompoundTag itemTag) {
        return StorageNbtSizeUtil.nbtByteSize(itemTag);
    }

    private static String errorMessage(Throwable throwable) {
        if (throwable == null) {
            return "unknown error";
        }
        return throwable.getMessage() == null ? throwable.getClass().getSimpleName() : throwable.getMessage();
    }

    private static String itemId(CompoundTag itemTag) {
        if (itemTag == null) {
            return "<unknown>";
        }
        return itemTag.getString("id").filter(id -> !id.isBlank()).orElse("<unknown>");
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

    private static String chunkId(int index) {
        return CHUNK_PREFIX + Math.max(0, index);
    }

    private static int clampPage(int requestedPage, int maxPage) {
        return Math.min(clampMinPage(requestedPage), maxPage);
    }

    private static int clampMinPage(int requestedPage) {
        return Math.max(1, requestedPage);
    }

    public record PageResult(
            List<SavedIndexItemEntry> entries,
            int currentPage,
            int maxPage,
            boolean searchMode,
            int totalResults,
            String pageId,
            String pageChunkId,
            String pageName,
            String pageNamePlain
    ) {
    }

    public record PageStats(
            int storedPages,
            int occupiedPages,
            int emptyPages
    ) {
    }

    public record PageInfo(
            String id,
            String chunkId,
            int pageNumber,
            String name,
            String namePlain,
            int itemCount,
            int nbtBytes,
            long savedAt,
            long updatedAt,
            boolean virtualPage,
            boolean placeholderPage
    ) {
    }

    public record PageSnapshot(
            PageResult result,
            Map<String, ItemStack> loadedStacks,
            PageStats stats
    ) {
    }

    public record SlotMutation(
            int slotInPage,
            @Nullable String entryId,
            ItemStack targetStack
    ) {
        public SlotMutation {
            targetStack = Objects.requireNonNullElse(targetStack, ItemStack.EMPTY);
        }
    }

    public record ExternalPageImport(
            String name,
            List<ExternalItemImport> items
    ) {
    }

    public record ExternalItemImport(
            int slotInPage,
            ItemStack stack,
            @Nullable CompoundTag itemTag,
            int dataVersion
    ) {
        public ExternalItemImport {
            stack = Objects.requireNonNullElse(stack, ItemStack.EMPTY);
        }
    }

    public record StorageImportResult(
            int pages,
            int items
    ) {
    }

    public record StorageImportProgress(
            String phase,
            int current,
            int total,
            int items
    ) {
    }

    public record StorageReplaceResult(boolean success, String message) {
        public static StorageReplaceResult ok() {
            return new StorageReplaceResult(true, "");
        }

        public static StorageReplaceResult failure(String message) {
            return new StorageReplaceResult(false, message == null || message.isBlank() ? "Storage save failed." : message);
        }
    }

    private record DecodeRequest(
            String id,
            String chunkId,
            int page,
            int slotInPage,
            int slotInChunk,
            int dataVersion,
            CompoundTag itemTag,
            int tagHash,
            String tagFingerprint
    ) {
    }

    private record DecodedItem(
            ItemStack stack,
            @Nullable CompoundTag upgradedItemTag,
            int dataVersion
    ) {
    }

    private record DecodedByKey(
            SavedChunkCodec.DecodeKey key,
            DecodedItem item
    ) {
    }

    private record PageCacheKey(
            int page,
            String query,
            StorageSortMode sortMode
    ) {
    }


    private record PendingSlotMutation(
            long sequence,
            SlotMutation mutation
    ) {
    }

    private record PendingMutationToken(
            int page,
            Map<Integer, Long> slotSequences
    ) {
    }

    private static <K, V> Map<K, V> lruCache(int maxSize) {
        int safeMax = Math.max(1, maxSize);
        return new LinkedHashMap<>(32, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > safeMax;
            }
        };
    }

    private static ExecutorService newSingleDaemonExecutor(String threadName) {
        return Executors.newSingleThreadExecutor(runnable -> newDaemonThread(runnable, threadName));
    }

    private static Thread newDaemonThread(Runnable runnable, String threadName) {
        Thread thread = new Thread(runnable, threadName);
        thread.setDaemon(true);
        return thread;
    }

    private void enqueueWrite(Runnable task) {
        synchronized (this.writeQueueLock) {
            this.writeQueue = this.writeQueue
                    .handle((ignored, throwable) -> {
                        if (throwable != null && this.queuedWriteFailure == null) {
                            this.queuedWriteFailure = unwrapCompletion(throwable);
                        }
                        return ignored;
                    })
                    .thenRunAsync(task, this.writeExecutor);
        }
    }

    private static Throwable unwrapCompletion(Throwable throwable) {
        if (throwable instanceof CompletionException completion && completion.getCause() != null) {
            return completion.getCause();
        }
        return throwable;
    }
}
