package me.noramibu.itemeditor.service;

import com.mojang.datafixers.DataFixer;
import com.mojang.serialization.Dynamic;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;
import me.noramibu.itemeditor.storage.SavedItemStorageService;
import me.noramibu.itemeditor.storage.StorageConstants;
import me.noramibu.itemeditor.storage.StorageMetadataUtil;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.world.item.ItemStack;
import org.slf4j.LoggerFactory;

public final class StoragePageImportService {
    private StoragePageImportService() {}

    public static SavedItemStorageService.ExternalPageImport read(Path path, RegistryAccess registries, DataFixer fixer)
            throws IOException {
        return decode(readTag(path), registries, fixer);
    }

    static CompoundTag readTag(Path path) throws IOException {
        boolean compressed;
        try (var input = Files.newInputStream(path)) {
            compressed = input.read() == 0x1f && input.read() == 0x8b;
        }
        return compressed ? NbtIo.readCompressed(path, NbtAccounter.create(64L * 1024 * 1024)) : NbtIo.read(path);
    }

    static SavedItemStorageService.ExternalPageImport decode(
            CompoundTag root, RegistryAccess registries, DataFixer fixer) throws IOException {
        return decode(root, registries, fixer, () -> false, ignored -> {});
    }

    static SavedItemStorageService.ExternalPageImport decode(
            CompoundTag root,
            RegistryAccess registries,
            DataFixer fixer,
            BooleanSupplier cancelled,
            IntConsumer progress)
            throws IOException {
        if (root == null
                || !root.getStringOr("backupType", "").equals("storage_page")
                || root.getIntOr("schemaVersion", 0) != 1) {
            throw new IOException("Not a supported Item Editor storage-page backup.");
        }
        CompoundTag page = root.getCompound("page").orElseThrow(() -> new IOException("Missing page metadata."));
        var entries = root.getList("items").orElseThrow(() -> new IOException("Missing page items."));
        if (entries.size() > StorageConstants.PAGE_SIZE) throw new IOException("Too many items in the page backup.");
        int current = StorageMetadataUtil.currentDataVersion();
        int sourceVersion = root.getIntOr("dataVersion", 0);
        if (sourceVersion <= 0 || sourceVersion > current) {
            throw new IOException("Unsupported backup data version: " + sourceVersion);
        }
        var slots = new HashSet<Integer>();
        var items = new ArrayList<SavedItemStorageService.ExternalItemImport>();
        for (var tag : entries) {
            if (cancelled.getAsBoolean()) throw new CancellationException();
            if (!(tag instanceof CompoundTag entry)) throw new IOException("Invalid page item entry.");
            int slot = entry.getIntOr("slotInPage", -1);
            if (slot < 0 || slot >= StorageConstants.PAGE_SIZE || !slots.add(slot)) {
                throw new IOException("Invalid or duplicate page slot: " + slot);
            }
            int version = entry.getIntOr("dataVersion", sourceVersion);
            if (version <= 0 || version > current)
                throw new IOException("Unsupported data version in slot " + (slot + 1));
            CompoundTag item =
                    entry.getCompound("item").orElseThrow(() -> new IOException("Missing item in slot " + (slot + 1)));
            if (version < current) {
                if (fixer == null) throw new IOException("Data conversion required for slot " + (slot + 1));
                var fixed = fixer.update(
                                References.ITEM_STACK, new Dynamic<>(NbtOps.INSTANCE, item.copy()), version, current)
                        .getValue();
                if (!(fixed instanceof CompoundTag compound))
                    throw new IOException("Data conversion failed in slot " + (slot + 1));
                item = compound;
            }
            var decoded = ItemStack.CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE), item);
            decoded.error().ifPresent(error -> {
                String message = error.message();
                LoggerFactory.getLogger(StoragePageImportService.class)
                        .warn(
                                "Storage page import failed at slot {}: {}",
                                slot + 1,
                                message.substring(0, Math.min(message.length(), 16000)));
            });
            ItemStack stack = decoded.result()
                    .orElseThrow(() ->
                            new IOException("Cannot decode item in slot " + (slot + 1) + ". Nothing was imported."));
            if (stack.isEmpty()) throw new IOException("Empty item in slot " + (slot + 1));
            items.add(new SavedItemStorageService.ExternalItemImport(slot, stack, item.copy(), current));
            progress.accept(items.size());
        }
        return new SavedItemStorageService.ExternalPageImport(page.getStringOr("name", ""), items);
    }
}
