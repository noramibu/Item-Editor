package me.noramibu.itemeditor.storage;

import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

final class SavedItemRuntimeCaches {

    private final Map<Integer, Map<String, ItemStack>> decodedTagMemoCache;
    private final Map<Object, CachedPage> hotPageCache;
    private final ReentrantReadWriteLock decodeMemoLock = new ReentrantReadWriteLock();
    private final Object hotPageCacheLock = new Object();

    SavedItemRuntimeCaches(int decodeMemoSize, int hotPageSize) {
        this.decodedTagMemoCache = lruCache(decodeMemoSize);
        this.hotPageCache = lruCache(hotPageSize);
    }

    ItemStack memoizedDecodedStack(SavedChunkCodec.DecodeKey key) {
        this.decodeMemoLock.readLock().lock();
        try {
            Map<String, ItemStack> byFingerprint = this.decodedTagMemoCache.get(key.tagHash());
            if (byFingerprint == null) {
                return null;
            }
            ItemStack stack = byFingerprint.get(key.fingerprint());
            return stack == null ? null : stack.copy();
        } finally {
            this.decodeMemoLock.readLock().unlock();
        }
    }

    void storeMemoizedDecodedStack(SavedChunkCodec.DecodeKey key, ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        this.decodeMemoLock.writeLock().lock();
        try {
            Map<String, ItemStack> byFingerprint = this.decodedTagMemoCache.computeIfAbsent(key.tagHash(), ignored -> new HashMap<>());
            byFingerprint.put(key.fingerprint(), stack.copy());
        } finally {
            this.decodeMemoLock.writeLock().unlock();
        }
    }

    Map<String, ItemStack> hotPageStacks(Object key, long signature) {
        synchronized (this.hotPageCacheLock) {
            CachedPage cached = this.hotPageCache.get(key);
            if (cached == null || cached.entriesSignature() != signature) {
                return null;
            }
            return copyStacksMap(cached.loadedStacks());
        }
    }

    void putHotPage(Object key, long signature, Map<String, ItemStack> stacks) {
        if (stacks == null) {
            return;
        }
        synchronized (this.hotPageCacheLock) {
            this.hotPageCache.put(key, new CachedPage(signature, copyStacksMap(stacks)));
        }
    }

    void invalidateHotPageCache() {
        synchronized (this.hotPageCacheLock) {
            this.hotPageCache.clear();
        }
    }

    private static Map<String, ItemStack> copyStacksMap(Map<String, ItemStack> source) {
        Map<String, ItemStack> copy = new HashMap<>();
        for (Map.Entry<String, ItemStack> entry : source.entrySet()) {
            ItemStack stack = entry.getValue();
            copy.put(entry.getKey(), stack == null ? ItemStack.EMPTY : stack.copy());
        }
        return copy;
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

    private record CachedPage(
            long entriesSignature,
            Map<String, ItemStack> loadedStacks
    ) {
    }
}
