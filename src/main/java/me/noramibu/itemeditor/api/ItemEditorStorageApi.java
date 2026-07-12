package me.noramibu.itemeditor.api;

import me.noramibu.itemeditor.storage.SavedItemStorageService;
import me.noramibu.itemeditor.storage.StorageServices;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/** Client-side access to Item Editor's saved-item storage. */
@Environment(EnvType.CLIENT)
public final class ItemEditorStorageApi {

    private ItemEditorStorageApi() {
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
        return storage().enqueueAddToFirstEmptySlot(pageId, stack, registryAccess())
                .thenApply(result -> {
                    if (result.isEmpty()) {
                        return new StoreResult(Status.PAGE_NOT_FOUND, -1);
                    }
                    int slot = result.getAsInt();
                    return slot < 0
                            ? new StoreResult(Status.PAGE_FULL, -1)
                            : new StoreResult(Status.SAVED, slot);
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
        return storage().findPageByNumberAsync(pageNumber)
                .thenApply(page -> page.map(ItemEditorStorageApi::summary));
    }

    /** Finds one persistent page by its exact stable ID. */
    public static CompletableFuture<Optional<StoragePage>> findPageById(String pageId) {
        return storage().findPageByIdAsync(pageId)
                .thenApply(page -> page.map(ItemEditorStorageApi::summary));
    }

    /** Finds persistent pages by case-insensitive page name. */
    public static CompletableFuture<List<StoragePage>> searchPages(String query) {
        return storage().searchPagesAsync(query)
                .thenApply(pages -> pages.stream().map(ItemEditorStorageApi::summary).toList());
    }

    private static SavedItemStorageService storage() {
        return StorageServices.savedItems();
    }

    private static RegistryAccess registryAccess() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.level == null ? RegistryAccess.EMPTY : minecraft.level.registryAccess();
    }

    private static StoragePage page(SavedItemStorageService.PageInfo page) {
        return new StoragePage(
                page.id(),
                page.pageNumber(),
                page.name(),
                page.namePlain(),
                page.itemCount()
        );
    }

    private static StoragePage summary(SavedItemStorageService.PageSummary page) {
        return new StoragePage(
                page.id(),
                page.pageNumber(),
                page.name(),
                page.namePlain(),
                page.itemCount()
        );
    }

    /** Immutable page metadata returned by storage operations. */
    public record StoragePage(String id, int number, String name, String plainName, int itemCount) {
    }

    /** Result of an atomic first-empty-slot store operation. */
    public record StoreResult(Status status, int slot) {
    }

    public enum Status {
        SAVED,
        PAGE_NOT_FOUND,
        PAGE_FULL,
        INVALID_ITEM
    }
}
