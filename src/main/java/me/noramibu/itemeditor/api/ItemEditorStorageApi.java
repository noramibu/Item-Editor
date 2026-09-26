package me.noramibu.itemeditor.api;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import me.noramibu.itemeditor.storage.SavedItemStorageService;
import me.noramibu.itemeditor.storage.StorageServices;
import me.noramibu.itemeditor.storage.StorageSortMode;
import me.noramibu.itemeditor.ui.screen.StorageScreen;
import me.noramibu.itemeditor.ui.screen.StorageScreenMode;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.ItemStack;

/** Client-side access to Item Editor's saved-item storage. */
@Environment(EnvType.CLIENT)
public final class ItemEditorStorageApi {

    private ItemEditorStorageApi() {}

    /**
     * Opens and highlights an item by stable ID. Returns false if missing, outside a world,
     * or the user changes screens during lookup. Storage failures complete exceptionally.
     */
    public static CompletableFuture<Boolean> openStorageItem(String itemId) {
        return openStorage(() -> storage().findItemLocationAsync(itemId));
    }

    /** Opens an occupied zero-based slot by stable page ID, with the same result semantics as openStorageItem. */
    public static CompletableFuture<Boolean> openStorageSlot(String pageId, int slot) {
        return openStorage(() -> storage().findSlotLocationAsync(pageId, slot));
    }

    private static CompletableFuture<Boolean> openStorage(
            Supplier<CompletableFuture<Optional<SavedItemStorageService.ItemLocation>>> lookup) {
        Minecraft minecraft = Minecraft.getInstance();
        CompletableFuture<Boolean> result = new CompletableFuture<>();
        minecraft.execute(() -> {
            if (minecraft.player == null || minecraft.level == null) {
                result.complete(false);
                return;
            }
            var previous = minecraft.screen;
            var level = minecraft.level;
            try {
                lookup.get()
                        .whenComplete((location, failure) -> minecraft.execute(() -> {
                            if (failure != null) {
                                result.completeExceptionally(failure);
                            } else if (location.isEmpty()
                                    || minecraft.player == null
                                    || minecraft.level != level
                                    || minecraft.screen != previous) {
                                result.complete(false);
                            } else {
                                try {
                                    var target = location.get();
                                    minecraft.setScreen(new StorageScreen(
                                                    target.pageNumber(),
                                                    "",
                                                    StorageSortMode.REGULAR,
                                                    StorageScreenMode.COPY_IMPORT,
                                                    previous,
                                                    null)
                                            .highlightItem(target.itemId()));
                                    result.complete(true);
                                } catch (RuntimeException exception) {
                                    result.completeExceptionally(exception);
                                }
                            }
                        }));
            } catch (RuntimeException exception) {
                result.completeExceptionally(exception);
            }
        });
        return result;
    }

    /** Returns all persistent pages in their current order. */
    public static CompletableFuture<List<StoragePage>> listPages() {
        return searchPages("");
    }

    /** Creates and persists a page, returning its stable ID. */
    public static CompletableFuture<StoragePage> createPage(String name) {
        return storage().enqueueCreatePage(name).thenApply(ItemEditorStorageApi::page);
    }

    /** Returns a copy of the item in a page slot, or {@link ItemStack#EMPTY}. */
    public static CompletableFuture<ItemStack> getItem(String pageId, int slot) {
        return storage().loadItemAtAsync(pageId, slot, registryAccess());
    }

    /** Adds an item without replacing an occupied slot. */
    public static CompletableFuture<Boolean> addItem(String pageId, int slot, ItemStack stack) {
        return storage().enqueueAddItem(pageId, slot, stack, registryAccess());
    }

    /** Atomically adds an item to the first empty slot on a page. */
    public static CompletableFuture<StoreResult> addToFirstEmptySlot(String pageId, ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return CompletableFuture.completedFuture(new StoreResult(Status.INVALID_ITEM, -1));
        }
        return storage()
                .enqueueAddToFirstEmptySlot(pageId, stack, registryAccess())
                .thenApply(result -> {
                    if (result.isEmpty()) {
                        return new StoreResult(Status.PAGE_NOT_FOUND, -1);
                    }
                    int slot = result.getAsInt();
                    return slot < 0 ? new StoreResult(Status.PAGE_FULL, -1) : new StoreResult(Status.SAVED, slot);
                });
    }

    /** Deletes a page and its contents by stable page ID. */
    public static CompletableFuture<Boolean> deletePage(String pageId) {
        return storage().enqueueDeletePage(pageId);
    }

    /** Returns the first empty slot, or {@code -1} when the page is missing or full. */
    public static CompletableFuture<Integer> firstEmptySlot(String pageId) {
        return storage().firstEmptySlotAsync(pageId);
    }

    /** Finds one persistent page by its exact one-based page number. */
    public static CompletableFuture<Optional<StoragePage>> findPageByNumber(int pageNumber) {
        return storage().findPageByNumberAsync(pageNumber).thenApply(page -> page.map(ItemEditorStorageApi::summary));
    }

    /** Finds one persistent page by its exact stable ID. */
    public static CompletableFuture<Optional<StoragePage>> findPageById(String pageId) {
        return storage().findPageByIdAsync(pageId).thenApply(page -> page.map(ItemEditorStorageApi::summary));
    }

    /** Finds persistent pages by case-insensitive page name. */
    public static CompletableFuture<List<StoragePage>> searchPages(String query) {
        return storage().searchPagesAsync(query).thenApply(pages -> pages.stream()
                .map(ItemEditorStorageApi::summary)
                .toList());
    }

    private static SavedItemStorageService storage() {
        return StorageServices.savedItems();
    }

    /** Finds an exact item-data match anywhere in storage, including count. Does not save or show UI. */
    public static CompletableFuture<Optional<DuplicateMatch>> findDuplicate(ItemStack stack) {
        return storage()
                .findDuplicateItemAsync(stack, registryAccess())
                .thenApply(match -> match.map(ItemEditorStorageApi::duplicate));
    }

    /**
     * Atomically checks all storage pages and saves to the target page's first empty slot only if absent.
     * Matching includes count and all encoded item data. Completion is on a storage worker; dispatch UI work
     * to the client thread. No warning or notification is shown by this API.
     */
    public static CompletableFuture<UniqueStoreResult> addIfAbsent(String pageId, ItemStack stack) {
        return storage()
                .enqueueAddIfAbsent(pageId, stack, registryAccess())
                .thenApply(result -> new UniqueStoreResult(
                        UniqueStoreStatus.valueOf(result.status().name()),
                        result.slot(),
                        Optional.ofNullable(result.duplicate()).map(ItemEditorStorageApi::duplicate)));
    }

    private static DuplicateMatch duplicate(SavedItemStorageService.DuplicateItem match) {
        return new DuplicateMatch(match.pageId(), match.pageNumber(), match.slot(), match.itemId());
    }

    public record DuplicateMatch(String pageId, int pageNumber, int slot, String itemId) {}

    public enum UniqueStoreStatus {
        SAVED,
        DUPLICATE,
        PAGE_NOT_FOUND,
        PAGE_FULL,
        INVALID_ITEM
    }

    public record UniqueStoreResult(UniqueStoreStatus status, int slot, Optional<DuplicateMatch> duplicate) {}

    private static RegistryAccess registryAccess() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.level == null ? RegistryAccess.EMPTY : minecraft.level.registryAccess();
    }

    private static StoragePage page(SavedItemStorageService.PageInfo page) {
        return new StoragePage(page.id(), page.pageNumber(), page.name(), page.namePlain(), page.itemCount());
    }

    private static StoragePage summary(SavedItemStorageService.PageSummary page) {
        return new StoragePage(page.id(), page.pageNumber(), page.name(), page.namePlain(), page.itemCount());
    }

    /** Immutable page metadata returned by storage operations. */
    public record StoragePage(String id, int number, String name, String plainName, int itemCount) {}

    /** Result of an atomic first-empty-slot store operation. */
    public record StoreResult(Status status, int slot) {}

    public enum Status {
        SAVED,
        PAGE_NOT_FOUND,
        PAGE_FULL,
        INVALID_ITEM
    }
}
