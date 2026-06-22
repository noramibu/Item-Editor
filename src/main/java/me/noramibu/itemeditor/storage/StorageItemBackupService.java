package me.noramibu.itemeditor.storage;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import me.noramibu.itemeditor.storage.io.AtomicFileUtil;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

public final class StorageItemBackupService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StorageItemBackupService.class);
    private static final Gson GSON = new Gson();
    private static final DateTimeFormatter DIRECTORY_DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter FILE_TIME_FORMAT = DateTimeFormatter.ofPattern("HHmmss_SSS");
    private static final DateTimeFormatter EVENT_FORMAT = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final Path root;
    private final AtomicInteger fileCounter = new AtomicInteger();

    public StorageItemBackupService(Path root) {
        this.root = root;
    }

    public synchronized @Nullable Path backup(BackupEvent event, CompoundTag itemTag) {
        if (event == null || itemTag == null) {
            return null;
        }
        try {
            Path directory = this.backupDirectory(event.category());
            Path file = directory.resolve(this.fileName(event));
            AtomicFileUtil.writeNbt(file, itemTag.copy());
            this.appendManifest(directory.resolve("manifest.jsonl"), event, itemTag, file);
            LOGGER.info("[Item Editor] Item backup written [reason={}] [file={}]", event.reason(), file.getFileName());
            return file;
        } catch (RuntimeException | IOException exception) {
            LOGGER.warn("[Item Editor] Failed to write item backup [reason={}] [error={}]", event.reason(), errorMessage(exception));
            return null;
        }
    }

    private Path backupDirectory(String category) throws IOException {
        Path directory = this.root.resolve(safeFilePart(category)).resolve(LocalDate.now().format(DIRECTORY_DATE_FORMAT));
        Files.createDirectories(directory);
        return directory;
    }

    private String fileName(BackupEvent event) {
        String source = safeFilePart(event.source());
        String reason = safeFilePart(event.reason());
        int page = event.page();
        int slot = event.slot();
        int dataVersion = event.sourceDataVersion();
        return String.format(
                Locale.ROOT,
                "%s_%04d_%s_page-%s_slot-%s_dv-%s_%s.nbt",
                LocalDateTime.now().format(FILE_TIME_FORMAT),
                this.fileCounter.incrementAndGet(),
                source,
                numberPart(page),
                numberPart(slot),
                numberPart(dataVersion),
                reason
        );
    }

    private void appendManifest(Path manifest, BackupEvent event, CompoundTag itemTag, Path backupFile) throws IOException {
        JsonObject object = new JsonObject();
        object.addProperty("timestamp", EVENT_FORMAT.format(LocalDateTime.now().atZone(ZoneId.systemDefault())));
        addString(object, "reason", event.reason());
        addString(object, "category", event.category());
        addString(object, "source", event.source());
        addNumber(object, "page", event.page());
        addNumber(object, "slot", event.slot());
        addString(object, "chunk", event.chunk());
        addNumber(object, "chunkSlot", event.chunkSlot());
        addString(object, "entryId", event.entryId());
        addString(object, "itemId", itemId(itemTag));
        addNumber(object, "sourceDataVersion", event.sourceDataVersion());
        addNumber(object, "targetDataVersion", event.targetDataVersion());
        addString(object, "fingerprint", event.fingerprint());
        addString(object, "sourceFile", event.sourceFile());
        addNumber(object, "sourceRow", event.sourceRow());
        addNumber(object, "sourceSlot", event.sourceSlot());
        addString(object, "backupFile", backupFile.getFileName().toString());
        addString(object, "note", event.note());
        Files.writeString(
                manifest,
                GSON.toJson(object) + System.lineSeparator(),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
        );
    }

    private static void addString(JsonObject object, String key, String value) {
        if (value != null && !value.isBlank()) {
            object.addProperty(key, value);
        }
    }

    private static void addNumber(JsonObject object, String key, int value) {
        if (value >= 0) {
            object.addProperty(key, value);
        }
    }

    private static String itemId(CompoundTag itemTag) {
        if (itemTag == null) {
            return "";
        }
        return itemTag.getString("id").filter(id -> !id.isBlank()).orElse("");
    }

    private static String safeFilePart(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        String safe = value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]+", "-");
        return safe.isBlank() ? "unknown" : safe;
    }

    private static String numberPart(int value) {
        return value >= 0 ? Integer.toString(value) : "unknown";
    }

    private static String errorMessage(Throwable throwable) {
        if (throwable == null) {
            return "unknown";
        }
        return throwable.getMessage() == null ? throwable.getClass().getSimpleName() : throwable.getMessage();
    }

    public record BackupEvent(
            String category,
            String reason,
            String source,
            int page,
            int slot,
            String chunk,
            int chunkSlot,
            String entryId,
            int sourceDataVersion,
            int targetDataVersion,
            String fingerprint,
            String sourceFile,
            int sourceRow,
            int sourceSlot,
            String note
    ) {
    }
}
