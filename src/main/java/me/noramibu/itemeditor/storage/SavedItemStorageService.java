package me.noramibu.itemeditor.storage;

import com.mojang.serialization.DataResult;
import me.noramibu.itemeditor.storage.io.AtomicFileUtil;
import me.noramibu.itemeditor.storage.model.SavedIndexFileModel;
import me.noramibu.itemeditor.storage.model.SavedIndexItemEntry;
import me.noramibu.itemeditor.storage.search.StorageSearchEngine;
import me.noramibu.itemeditor.storage.search.StorageSearchParser;
import me.noramibu.itemeditor.storage.search.StorageSearchQuery;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.stream.Stream;

public final class SavedItemStorageService {

    private static final String CHUNK_PREFIX = "chunk-";
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
    private final ReentrantReadWriteLock indexLock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock chunkLock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock itemCacheLock = new ReentrantReadWriteLock();
    private final Object prefetchStateLock = new Object();
    private final ExecutorService decodeExecutor;
    private final int decodeThreadCount;
    private final ExecutorService prefetchExecutor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "itemeditor-storage-prefetch");
        thread.setDaemon(true);
        return thread;
    });
    private final ExecutorService readExecutor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "itemeditor-storage-reads");
        thread.setDaemon(true);
        return thread;
    });
    private final ExecutorService writeExecutor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "itemeditor-storage-writes");
        thread.setDaemon(true);
        return thread;
    });
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
        this.decodeThreadCount = computeDecodeThreadCount();
        this.decodeExecutor = Executors.newFixedThreadPool(this.decodeThreadCount, runnable -> {
            Thread thread = new Thread(runnable, "itemeditor-storage-decode");
            thread.setDaemon(true);
            return thread;
        });
    }

    public Map<String, ItemStack> loadItems(List<SavedIndexItemEntry> entries, RegistryAccess registryAccess) {
        Map<String, ItemStack> loaded = new HashMap<>();
        if (entries == null || entries.isEmpty()) {
            return loaded;
        }
        List<DecodeRequest> decodeRequests = new ArrayList<>();
        Map<String, List<SavedIndexItemEntry>> chunkEntries = new HashMap<>();
        for (SavedIndexItemEntry entry : entries) {
            if (entry == null || entry.id == null || entry.id.isBlank()) {
                continue;
            }
            ItemStack cached = this.withItemCacheRead(() -> {
                ItemStack fromCache = this.itemCache.get(entry.id);
                return fromCache == null ? null : fromCache.copy();
            });
            if (cached != null) {
                loaded.put(entry.id, cached);
                continue;
            }
            chunkEntries.computeIfAbsent(entry.chunkId, ignored -> new ArrayList<>()).add(entry);
        }

        for (Map.Entry<String, List<SavedIndexItemEntry>> chunkGroup : chunkEntries.entrySet()) {
            SavedChunkCodec.SavedChunkData chunk = this.readChunk(chunkGroup.getKey());
            for (SavedIndexItemEntry entry : chunkGroup.getValue()) {
                SavedChunkCodec.SavedChunkEntry chunkEntry = chunk.entries().get(entry.slotInChunk);
                if (chunkEntry == null) {
                    continue;
                }
                CompoundTag itemTag = chunkEntry.itemTag().copy();
                SavedChunkCodec.DecodeKey key = SavedChunkCodec.decodeKey(itemTag);
                decodeRequests.add(new DecodeRequest(entry.id, itemTag, key.tagHash(), key.fingerprint()));
            }
        }

        if (decodeRequests.isEmpty()) {
            return loaded;
        }
        Map<String, ItemStack> decoded = this.decodeRequestsParallel(decodeRequests, registryAccess);
        this.withItemCacheWrite(() -> {
            for (Map.Entry<String, ItemStack> entry : decoded.entrySet()) {
                ItemStack stack = entry.getValue();
                if (stack == null || stack.isEmpty()) {
                    continue;
                }
                this.itemCache.put(entry.getKey(), stack.copy());
                loaded.put(entry.getKey(), stack);
            }
        });
        return loaded;
    }

    public PageResult page(int requestedPage, StorageSortMode sortMode) {
        this.ensureIndexLoaded();
        return this.withIndexRead(() -> this.page(this.indexCache, requestedPage, sortMode, false));
    }

    private PageResult page(SavedIndexFileModel index, int requestedPage, StorageSortMode sortMode, boolean reverseSort) {
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

        int maxPage = Math.max(1, this.maxPage(index.items));
        int page = clampMinPage(requestedPage);

        List<SavedIndexItemEntry> pageEntries = new ArrayList<>();
        for (SavedIndexItemEntry entry : index.items) {
            if (entry == null) {
                continue;
            }
            if (entry.page == page) {
                pageEntries.add(copy(entry));
            }
        }
        pageEntries.sort(Comparator.comparingInt((SavedIndexItemEntry entry) -> entry.slotInPage));
        return new PageResult(pageEntries, page, maxPage, false, index.items.size());
    }

    public PageResult search(String queryRaw, int requestedPage, StorageSortMode sortMode) {
        StorageSearchQuery query = StorageSearchParser.parse(queryRaw);
        if (query.isEmpty()) {
            return this.page(requestedPage, sortMode);
        }
        this.ensureIndexLoaded();
        return this.withIndexRead(() -> this.search(this.indexCache, query, requestedPage, sortMode, false));
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

    public int trimTrailingEmptyPages() {
        this.ensureIndexLoaded();
        IndexChunkScan scan = this.withIndexRead(() -> {
            Set<String> chunkIds = new HashSet<>();
            int maxChunkIndex = -1;
            for (SavedIndexItemEntry entry : this.indexCache.items) {
                if (entry == null) {
                    continue;
                }
                chunkIds.add(entry.chunkId);
                maxChunkIndex = Math.max(maxChunkIndex, chunkIndexFromId(entry.chunkId));
            }
            return new IndexChunkScan(chunkIds, maxChunkIndex);
        });
        Set<String> referencedChunkIds = scan.chunkIds();
        int maxReferencedChunkIndex = scan.maxChunkIndex();

        int removed = 0;
        Path dataDir = this.foundation.paths().savedDataDirectory();
        if (!Files.isDirectory(dataDir)) {
            return 0;
        }

        try (Stream<Path> files = Files.list(dataDir)) {
            for (Path file : (Iterable<Path>) files::iterator) {
                String filename = file.getFileName().toString();
                if (!filename.endsWith(".nbt")) {
                    continue;
                }
                String chunkId = filename.substring(0, filename.length() - 4);
                int chunkIndex = chunkIndexFromId(chunkId);
                if (chunkIndex < 0) {
                    continue;
                }
                boolean trailing = chunkIndex > maxReferencedChunkIndex;
                boolean unreferenced = !referencedChunkIds.contains(chunkId);
                if (!trailing || !unreferenced) {
                    continue;
                }
                try {
                    Files.deleteIfExists(file);
                    this.withChunkWrite(() -> {
                        this.chunkCache.remove(chunkId);
                    });
                    removed++;
                } catch (IOException ignored) {
                }
            }
        } catch (IOException ignored) {
        }
        if (removed > 0) {
            this.runtimeCaches.invalidateHotPageCache();
        }
        return removed;
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
            ItemStack targetStack = mutation.targetStack == null ? ItemStack.EMPTY : mutation.targetStack.copy();
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
        this.withIndexWrite(() -> {
            this.flushIndexNow();
        });
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
            int slotInChunk,
            long savedAt,
            long updatedAt,
            ItemStack stack,
            int nbtBytes
    ) {
        SavedIndexItemEntry entry = new SavedIndexItemEntry();
        entry.id = id;
        entry.chunkId = chunkId;
        entry.slotInChunk = slotInChunk;
        entry.page = chunkIndexFromId(chunkId) + 1;
        entry.slotInPage = slotInChunk;
        entry.savedAt = savedAt;
        entry.updatedAt = updatedAt;
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

    private static SavedIndexItemEntry copy(SavedIndexItemEntry source) {
        SavedIndexItemEntry copy = new SavedIndexItemEntry();
        copy.id = source.id;
        copy.chunkId = source.chunkId;
        copy.slotInChunk = source.slotInChunk;
        copy.page = source.page;
        copy.slotInPage = source.slotInPage;
        copy.savedAt = source.savedAt;
        copy.updatedAt = source.updatedAt;
        copy.itemRegistryKey = source.itemRegistryKey;
        copy.stackCount = Math.max(1, source.stackCount);
        copy.nbtBytes = Math.max(0, source.nbtBytes);
        copy.customNamePlain = source.customNamePlain;
        copy.lorePlain = source.lorePlain == null ? new ArrayList<>() : new ArrayList<>(source.lorePlain);
        return copy;
    }

    private SavedIndexItemEntry findEntry(List<SavedIndexItemEntry> entries, String id) {
        for (SavedIndexItemEntry entry : entries) {
            if (entry != null && id.equals(entry.id)) {
                return entry;
            }
        }
        return null;
    }

    private void replaceEntry(List<SavedIndexItemEntry> entries, SavedIndexItemEntry replacement) {
        for (int index = 0; index < entries.size(); index++) {
            SavedIndexItemEntry current = entries.get(index);
            if (current != null && replacement.id.equals(current.id)) {
                entries.set(index, replacement);
                return;
            }
        }
        entries.add(replacement);
    }

    private SavedIndexItemEntry findEntryAtSlot(List<SavedIndexItemEntry> entries, int page, int slotInPage) {
        for (SavedIndexItemEntry entry : entries) {
            if (entry == null) {
                continue;
            }
            if (entry.page == page && entry.slotInPage == slotInPage) {
                return entry;
            }
        }
        return null;
    }

    private int maxPage(List<SavedIndexItemEntry> entries) {
        int maxChunk = 0;
        for (SavedIndexItemEntry entry : entries) {
            if (entry == null) {
                continue;
            }
            maxChunk = Math.max(maxChunk, chunkIndexFromId(entry.chunkId));
        }
        return maxChunk + 1;
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
        return new PageResult(pageEntries, page, maxPage, searchMode, total);
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
        PageResult result = this.withIndexRead(() -> {
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
            this.withChunkWrite(() -> {
                String targetChunkId = chunkId(page - 1);
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
                    SavedIndexItemEntry existing = this.resolveMutationEntry(index.items, page, slot, mutation.entryId);

                    if (targetStack == null || targetStack.isEmpty()) {
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
                        this.withItemCacheWrite(() -> {
                            this.itemCache.remove(existing.id);
                        });
                        continue;
                    }

                    CompoundTag encodedItemTag = this.encodeItemTag(targetStack, registryAccess);
                    int encodedNbtBytes = nbtByteSize(encodedItemTag);
                    long now = System.currentTimeMillis();
                    if (existing != null) {
                        SavedChunkCodec.SavedChunkData existingChunk = this.readChunk(existing.chunkId);
                        SavedChunkCodec.SavedChunkEntry current = existingChunk.entries().get(existing.slotInChunk);
                        long savedAt = current == null ? existing.savedAt : current.savedAt();
                        if (savedAt <= 0L) {
                            savedAt = now;
                        }
                        existingChunk.entries().put(existing.slotInChunk, new SavedChunkCodec.SavedChunkEntry(existing.id, savedAt, now, encodedItemTag.copy()));
                        if (existing.chunkId.equals(targetChunkId)) {
                            chunk = existingChunk;
                            chunkChanged = true;
                        } else {
                            this.writeChunk(existingChunk);
                        }
                        SavedIndexItemEntry refreshed = buildEntry(existing.id, existing.chunkId, existing.slotInChunk, savedAt, now, targetStack, encodedNbtBytes);
                        this.replaceEntry(index.items, refreshed);
                        this.withItemCacheWrite(() -> {
                            this.itemCache.put(existing.id, targetStack.copy());
                        });
                        indexChanged = true;
                        continue;
                    }

                    String id = UUID.randomUUID().toString();
                    chunk.entries().put(slot, new SavedChunkCodec.SavedChunkEntry(id, now, now, encodedItemTag.copy()));
                    SavedIndexItemEntry created = buildEntry(id, targetChunkId, slot, now, now, targetStack, encodedNbtBytes);
                    index.items.add(created);
                    this.onIndexEntryAdded(created);
                    this.withItemCacheWrite(() -> {
                        this.itemCache.put(id, targetStack.copy());
                    });
                    chunkChanged = true;
                    indexChanged = true;
                }

                if (chunkChanged) {
                    this.writeChunk(chunk);
                }
                if (indexChanged) {
                    this.markIndexDirty();
                    this.flushIndexIfDue();
                }
                if (chunkChanged || indexChanged) {
                    this.runtimeCaches.invalidateHotPageCache();
                }
            });
        });
    }

    private SavedIndexItemEntry resolveMutationEntry(
            List<SavedIndexItemEntry> entries,
            int page,
            int slotInPage,
            String entryId
    ) {
        if (entryId != null && !entryId.isBlank()) {
            SavedIndexItemEntry byId = this.findEntry(entries, entryId);
            if (byId != null) {
                return byId;
            }
        }
        return this.findEntryAtSlot(entries, page, slotInPage);
    }

    private void ensureIndexLoaded() {
        if (this.indexCache != null) {
            return;
        }
        this.withIndexWrite(() -> {
            if (this.indexCache == null) {
                this.indexCache = this.foundation.loadSavedIndex();
                this.rebuildPageStats(this.indexCache.items);
                this.indexDirty = false;
                if (this.backfillMissingNbtBytes(this.indexCache)) {
                    this.markIndexDirty();
                    this.flushIndexNow();
                }
            }
        });
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
            if (count <= 1) {
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
        int storedPages = Math.max(0, this.maxTrackedPage);
        int occupiedPages = this.pageOccupancy.size();
        int emptyPages = Math.max(0, storedPages - occupiedPages);
        this.pageStatsCache = new PageStats(storedPages, occupiedPages, emptyPages);
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
            ItemStack stack = mutation.targetStack == null ? ItemStack.EMPTY : mutation.targetStack.copy();
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
                    chunkId(base.currentPage() - 1),
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
        return new PageResult(entries, base.currentPage(), base.maxPage(), base.searchMode(), base.totalResults());
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
        PageResult result = this.withIndexRead(() -> this.page(this.indexCache, Math.max(1, page), StorageSortMode.REGULAR, false));
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
        Map<SavedChunkCodec.DecodeKey, List<String>> idsByKey = new HashMap<>();

        for (DecodeRequest request : requests) {
            SavedChunkCodec.DecodeKey key = new SavedChunkCodec.DecodeKey(request.tagHash(), request.tagFingerprint());
            ItemStack memoized = this.runtimeCaches.memoizedDecodedStack(key);
            if (memoized != null && !memoized.isEmpty()) {
                decoded.put(request.id(), memoized);
                continue;
            }
            uniqueRequests.putIfAbsent(key, request);
            idsByKey.computeIfAbsent(key, ignored -> new ArrayList<>()).add(request.id());
        }
        if (uniqueRequests.isEmpty()) {
            return decoded;
        }

        Map<SavedChunkCodec.DecodeKey, ItemStack> decodedUnique = new HashMap<>();
        if (this.decodeThreadCount <= 1 || uniqueRequests.size() <= 1) {
            for (Map.Entry<SavedChunkCodec.DecodeKey, DecodeRequest> entry : uniqueRequests.entrySet()) {
                ItemStack stack = this.decodeItemTag(entry.getValue().itemTag(), access);
                if (!stack.isEmpty()) {
                    decodedUnique.put(entry.getKey(), stack);
                }
            }
        } else {
            List<CompletableFuture<DecodedByKey>> futures = new ArrayList<>(uniqueRequests.size());
            for (Map.Entry<SavedChunkCodec.DecodeKey, DecodeRequest> entry : uniqueRequests.entrySet()) {
                SavedChunkCodec.DecodeKey key = entry.getKey();
                DecodeRequest request = entry.getValue();
                futures.add(CompletableFuture.supplyAsync(
                        () -> new DecodedByKey(key, this.decodeItemTag(request.itemTag(), access)),
                        this.decodeExecutor
                ));
            }
            for (CompletableFuture<DecodedByKey> future : futures) {
                DecodedByKey decodedByKey = future.join();
                if (decodedByKey != null && decodedByKey.stack() != null && !decodedByKey.stack().isEmpty()) {
                    decodedUnique.put(decodedByKey.key(), decodedByKey.stack());
                }
            }
        }

        for (Map.Entry<SavedChunkCodec.DecodeKey, ItemStack> entry : decodedUnique.entrySet()) {
            SavedChunkCodec.DecodeKey key = entry.getKey();
            ItemStack stack = entry.getValue();
            this.runtimeCaches.storeMemoizedDecodedStack(key, stack);
            List<String> ids = idsByKey.get(key);
            if (ids == null) {
                continue;
            }
            for (String id : ids) {
                decoded.put(id, stack.copy());
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

    private void withIndexRead(Runnable action) {
        withReadLock(this.indexLock, action);
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

    private <T> T withItemCacheRead(Supplier<T> action) {
        return withReadLock(this.itemCacheLock, action);
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

    private static void withReadLock(ReentrantReadWriteLock lock, Runnable action) {
        lock.readLock().lock();
        try {
            action.run();
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
        SavedChunkCodec.SavedChunkData cached = this.withChunkWrite(() -> this.chunkCache.get(chunkId));
        if (cached != null) {
            return cached;
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
        throw new IllegalStateException(encoded.error().map(error -> error.message()).orElse("Failed to encode item stack"));
    }

    private ItemStack decodeItemTag(CompoundTag itemTag, RegistryAccess registryAccess) {
        DataResult<ItemStack> decoded = ItemStack.CODEC.parse(
                registryAccess.createSerializationContext(NbtOps.INSTANCE),
                itemTag
        );
        return decoded.result().map(ItemStack::copy).orElse(ItemStack.EMPTY);
    }

    private static int nbtByteSize(CompoundTag itemTag) {
        if (itemTag == null) {
            return 0;
        }
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream(256);
            try (DataOutputStream dataOutput = new DataOutputStream(output)) {
                NbtIo.write(itemTag, dataOutput);
                dataOutput.flush();
            }
            return Math.max(1, output.size());
        } catch (IOException ignored) {
            return Math.max(1, itemTag.toString().length());
        }
    }

    private static int chunkIndexFromId(String chunkId) {
        if (chunkId == null || chunkId.isBlank()) {
            return 0;
        }
        if (chunkId.startsWith(CHUNK_PREFIX)) {
            try {
                return Math.max(0, Integer.parseInt(chunkId.substring(CHUNK_PREFIX.length())));
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }

    private static String chunkId(int index) {
        return CHUNK_PREFIX + Math.max(0, index);
    }

    private static int clampPage(int requestedPage, int maxPage) {
        if (requestedPage < 1) {
            return 1;
        }
        return Math.min(requestedPage, maxPage);
    }

    private static int clampMinPage(int requestedPage) {
        return Math.max(1, requestedPage);
    }

    public record PageResult(
            List<SavedIndexItemEntry> entries,
            int currentPage,
            int maxPage,
            boolean searchMode,
            int totalResults
    ) {
    }

    public record PageStats(
            int storedPages,
            int occupiedPages,
            int emptyPages
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
            String entryId,
            ItemStack targetStack
    ) {
    }

    private record DecodeRequest(
            String id,
            CompoundTag itemTag,
            int tagHash,
            String tagFingerprint
    ) {
    }

    private record DecodedByKey(
            SavedChunkCodec.DecodeKey key,
            ItemStack stack
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

    private record IndexChunkScan(
            Set<String> chunkIds,
            int maxChunkIndex
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

    private void enqueueWrite(Runnable task) {
        synchronized (this.writeQueueLock) {
            this.writeQueue = this.writeQueue
                    .handle((ignored, throwable) -> {
                        if (throwable != null && this.queuedWriteFailure == null) {
                            this.queuedWriteFailure = unwrapCompletion(throwable);
                        }
                        return null;
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
