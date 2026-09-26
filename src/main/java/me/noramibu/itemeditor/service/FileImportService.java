package me.noramibu.itemeditor.service;

import com.mojang.datafixers.DataFixer;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import me.noramibu.itemeditor.storage.SavedItemStorageService.ExternalPageImport;
import me.noramibu.itemeditor.util.ItemEditorText;
import me.noramibu.itemeditor.util.RawItemDataUtil;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import org.slf4j.LoggerFactory;

public final class FileImportService {
    private FileImportService() {}

    public static Preview read(
            Path path, RegistryAccess access, DataFixer fixer, BooleanSupplier cancelled, Consumer<Component> progress)
            throws IOException {
        checkCancelled(cancelled);
        progress.accept(ItemEditorText.tr("import.reading"));
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        var importer = new ItemImportService();
        ItemImportService.ImportResult items;
        if (!name.endsWith(".nbt") && !name.endsWith(".snbt") && !name.endsWith(".json")) {
            throw new IOException("Choose an NBT, SNBT, or JSON file.");
        }
        if (name.endsWith(".nbt") && isBinaryNbt(path)) {
            var root = StoragePageImportService.readTag(path);
            checkCancelled(cancelled);
            if (root != null && root.getStringOr("backupType", "").equals("storage_page")) {
                var page = StoragePageImportService.decode(
                        root,
                        access,
                        fixer,
                        cancelled,
                        count -> progress.accept(ItemEditorText.tr("import.decoding_items", count)));
                checkCancelled(cancelled);
                return new Preview(page, null);
            }
            progress.accept(ItemEditorText.tr("import.decoding"));
            items = importer.resultFromTag(root, access, fixer);
        } else {
            String text = Files.readString(path);
            checkCancelled(cancelled);
            progress.accept(ItemEditorText.tr("import.decoding"));
            items = importer.resultFromParse(RawItemDataUtil.parseFlexible(text, access));
        }
        checkCancelled(cancelled);
        if (!items.success()) {
            LoggerFactory.getLogger(FileImportService.class)
                    .warn("Item import rejected: {}", bounded(items.message(), 16000));
            throw new IOException(
                    "Cannot decode this item file. Check the format and Minecraft version. Details are in the log.");
        }
        return new Preview(null, items);
    }

    private static void checkCancelled(BooleanSupplier cancelled) {
        if (cancelled.getAsBoolean()) throw new CancellationException();
    }

    private static boolean isBinaryNbt(Path path) throws IOException {
        try (var input = Files.newInputStream(path)) {
            int first = input.read();
            return first == 10 || first == 0x1f;
        }
    }

    public static String errorMessage(Throwable failure) {
        while (failure instanceof CompletionException && failure.getCause() != null) {
            failure = failure.getCause();
        }
        LoggerFactory.getLogger(FileImportService.class)
                .warn(
                        "File import failed: {}: {}",
                        failure.getClass().getSimpleName(),
                        bounded(failure.getMessage(), 16000));
        return failure instanceof IOException
                ? bounded(failure.getMessage(), 240)
                : "Unable to import this file. Details are in the log.";
    }

    private static String bounded(String text, int length) {
        if (text == null || text.isBlank()) return "Unable to read the file.";
        String clean = text.replace('\n', ' ').replace('\r', ' ');
        return clean.length() <= length ? clean : clean.substring(0, length) + "...";
    }

    public record Preview(ExternalPageImport page, ItemImportService.ImportResult items) {}
}
