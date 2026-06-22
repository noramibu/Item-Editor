package me.noramibu.itemeditor.service;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.datafixers.DataFixer;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import me.noramibu.itemeditor.storage.SavedItemStorageService;
import me.noramibu.itemeditor.storage.StorageItemBackupService;
import me.noramibu.itemeditor.storage.StorageConstants;
import me.noramibu.itemeditor.util.IdFieldNormalizer;
import me.noramibu.itemeditor.util.TextComponentUtil;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.inventory.Hotbar;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.Identifier;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ExternalStorageImportService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExternalStorageImportService.class);
    private static final int DEFAULT_HOTBAR_DATA_VERSION = 1343;
    private static final Pattern NBT_EDITOR_PAGE = Pattern.compile("page(\\d+)\\.nbt");
    private static final Pattern LIBRARIAN_PAGE = Pattern.compile("hotbar\\.(-?\\d+)\\.nbt");
    private static final ExecutorService IMPORT_EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "itemeditor-external-storage-import");
        thread.setDaemon(true);
        return thread;
    });

    public CompletableFuture<ScanResult> scan(Minecraft minecraft) {
        Path gameDirectory = minecraft.gameDirectory.toPath();
        return CompletableFuture.supplyAsync(() -> new ScanResult(
                countMatchingFiles(nbtEditorClientChestPath(gameDirectory), NBT_EDITOR_PAGE),
                countMatchingFiles(gameDirectory.resolve("hotbars"), LIBRARIAN_PAGE)
        ), IMPORT_EXECUTOR);
    }

    public CompletableFuture<ImportReadResult> readImports(
            Minecraft minecraft,
            RegistryAccess registryAccess,
            boolean nbtEditor,
            boolean librarian,
            Consumer<ProgressUpdate> progress
    ) {
        Path gameDirectory = minecraft.gameDirectory.toPath();
        DataFixer fixer = minecraft.getFixerUpper();
        RegistryAccess access = registryAccess == null ? RegistryAccess.EMPTY : registryAccess;
        return CompletableFuture.supplyAsync(() -> {
            List<SavedItemStorageService.ExternalPageImport> pages = new ArrayList<>();
            List<String> warnings = new ArrayList<>();
            StorageItemBackupService backupService = new StorageItemBackupService(
                    gameDirectory.resolve("itemeditor").resolve("backups").resolve("storage")
            );
            LOGGER.info(
                    "[Item Editor] External storage import started [nbtEditor={}] [librarian={}] [gameDir={}]",
                    nbtEditor,
                    librarian,
                    gameDirectory
            );
            if (nbtEditor) {
                pages.addAll(this.readNbtEditorPages(
                        nbtEditorClientChestPath(gameDirectory),
                        access,
                        fixer,
                        backupService,
                        warnings,
                        progress
                ));
            }
            if (librarian) {
                pages.addAll(this.readLibrarianPages(
                        gameDirectory.resolve("hotbars"),
                        access,
                        fixer,
                        backupService,
                        warnings,
                        progress
                ));
            }
            emitProgress(progress, "read_done", "", pages.size(), pages.size(), 0);
            logImportResult(pages, warnings);
            return new ImportReadResult(pages, warnings);
        }, IMPORT_EXECUTOR);
    }

    private List<SavedItemStorageService.ExternalPageImport> readNbtEditorPages(
            Path directory,
            RegistryAccess registryAccess,
            DataFixer fixer,
            StorageItemBackupService backupService,
            List<String> warnings,
            Consumer<ProgressUpdate> progress
    ) {
        List<NumberedPath> files = matchingFiles(directory, NBT_EDITOR_PAGE);
        if (files.isEmpty()) {
            return List.of();
        }
        Map<Integer, String> namesByPage = readNbtEditorPageNames(directory.resolve("page_names.json"));
        List<SavedItemStorageService.ExternalPageImport> pages = new ArrayList<>();
        for (int fileIndex = 0; fileIndex < files.size(); fileIndex++) {
            NumberedPath file = files.get(fileIndex);
            emitProgress(progress, "read_page", "NBT Editor", fileIndex + 1, files.size(), 0);
            try {
                CompoundTag root = readNbt(file.path());
                int dataVersion = root.getInt("DataVersion").orElse(currentDataVersion());
                ListTag items = root.getList("items").orElseGet(ListTag::new);
                List<SavedItemStorageService.ExternalItemImport> importedItems = new ArrayList<>();
                int itemsToRead = Math.min(items.size(), StorageConstants.PAGE_SIZE);
                for (int index = 0; index < itemsToRead; index++) {
                    if (index % 9 == 0) {
                        emitProgress(progress, "read_items", "NBT Editor", fileIndex + 1, files.size(), index);
                    }
                    Tag rawItem = items.get(index);
                    if (!(rawItem instanceof CompoundTag itemTag)) {
                        continue;
                    }
                    ImportedItem imported = this.decodeItem(
                            itemTag.copy(),
                            dataVersion,
                            registryAccess,
                            fixer,
                            backupService,
                            warnings,
                            ImportWarningContext.nbtEditor(file, index + 1)
                    );
                    if (imported != null && !imported.stack().isEmpty()) {
                        importedItems.add(new SavedItemStorageService.ExternalItemImport(
                                index,
                                imported.stack(),
                                imported.itemTag(),
                                imported.dataVersion()
                        ));
                    }
                }
                if (!importedItems.isEmpty()) {
                    pages.add(new SavedItemStorageService.ExternalPageImport(
                            importedName(namesByPage.get(file.number()), "NBT Editor"),
                            importedItems
                    ));
                }
            } catch (IOException | RuntimeException exception) {
                warnings.add(ImportWarningContext.nbtEditor(file, -1)
                        .withStage("read_page")
                        .withError(errorMessage(exception)));
            }
        }
        return pages;
    }

    private List<SavedItemStorageService.ExternalPageImport> readLibrarianPages(
            Path directory,
            RegistryAccess registryAccess,
            DataFixer fixer,
            StorageItemBackupService backupService,
            List<String> warnings,
            Consumer<ProgressUpdate> progress
    ) {
        List<NumberedPath> files = matchingFiles(directory, LIBRARIAN_PAGE);
        if (files.isEmpty()) {
            return List.of();
        }
        List<SavedItemStorageService.ExternalPageImport> pages = new ArrayList<>();
        for (int fileIndex = 0; fileIndex < files.size(); fileIndex++) {
            NumberedPath file = files.get(fileIndex);
            emitProgress(progress, "read_page", "Librarian", fileIndex + 1, files.size(), 0);
            try {
                CompoundTag root = readNbt(file.path());
                int sourceDataVersion = NbtUtils.getDataVersion(root, DEFAULT_HOTBAR_DATA_VERSION);
                ImportWarningContext context = ImportWarningContext.librarian(file, -1);
                CompoundTag hotbarRoot = updateHotbarRoot(
                        root,
                        sourceDataVersion,
                        fixer,
                        backupService,
                        warnings,
                        context
                );
                int itemDataVersion = currentDataVersion();
                Map<Integer, List<SavedItemStorageService.ExternalItemImport>> itemsByPart = new HashMap<>();
                int baseSlot = 0;
                for (int row : numericRowKeys(hotbarRoot)) {
                    emitProgress(progress, "read_items", "Librarian", fileIndex + 1, files.size(), baseSlot);
                    Tag rowTag = hotbarRoot.get(Integer.toString(row));
                    if (rowTag == null) {
                        continue;
                    }
                    DataResult<Hotbar> hotbar = Hotbar.CODEC.parse(NbtOps.INSTANCE, rowTag);
                    if (hotbar.result().isEmpty()) {
                        CompoundTag rowBackup = new CompoundTag();
                        rowBackup.put("row", rowTag.copy());
                        String backup = backupExternalItem(
                                backupService,
                                "external_import_failed",
                                rowBackup,
                                ImportWarningContext.librarian(file, row),
                                sourceDataVersion,
                                "failed to decode Librarian hotbar row"
                        );
                        warnings.add(ImportWarningContext.librarian(file, row)
                                .withStage("decode_row")
                                .withError("failed to decode row" + backup));
                        continue;
                    }
                    for (ItemStack stack : hotbar.result().get().load(registryAccess)) {
                        if (!stack.isEmpty()) {
                            int part = baseSlot / StorageConstants.PAGE_SIZE;
                            itemsByPart.computeIfAbsent(part, ignored -> new ArrayList<>()).add(new SavedItemStorageService.ExternalItemImport(
                                    baseSlot % StorageConstants.PAGE_SIZE,
                                    stack.copy(),
                                    null,
                                    itemDataVersion
                            ));
                        }
                        baseSlot++;
                    }
                }
                if (!itemsByPart.isEmpty()) {
                    String name = readLibrarianName(root);
                    boolean split = itemsByPart.size() > 1;
                    List<Integer> parts = new ArrayList<>(itemsByPart.keySet());
                    parts.sort(Comparator.naturalOrder());
                    for (int part : parts) {
                        List<SavedItemStorageService.ExternalItemImport> importedItems = itemsByPart.get(part);
                        if (importedItems == null || importedItems.isEmpty()) {
                            continue;
                        }
                        pages.add(new SavedItemStorageService.ExternalPageImport(
                                split ? importedLibrarianPartName(name, part + 1) : importedName(name, "Librarian"),
                                importedItems
                        ));
                    }
                }
            } catch (IOException | RuntimeException exception) {
                warnings.add(ImportWarningContext.librarian(file, -1)
                        .withStage("read_hotbar")
                        .withError(errorMessage(exception)));
            }
        }
        return pages;
    }

    private static String importedLibrarianPartName(String rawName, int part) {
        String base = rawName == null ? "" : rawName.trim();
        String suffix = "(part " + Math.max(1, part) + ") - imported from Librarian.";
        return base.isBlank() ? suffix : base + " " + suffix;
    }

    private ImportedItem decodeItem(
            CompoundTag originalTag,
            int rawDataVersion,
            RegistryAccess registryAccess,
            DataFixer fixer,
            StorageItemBackupService backupService,
            List<String> warnings,
            ImportWarningContext context
    ) {
        boolean dynamic = originalTag.getByte("dynamic").map(value -> value != 0).orElse(false);
        if (dynamic) {
            originalTag.remove("dynamic");
        }
        int sourceDataVersion = rawDataVersion > 0 ? rawDataVersion : currentDataVersion();
        ItemStack directStack = parseItemStack(originalTag, registryAccess);
        if (!directStack.isEmpty()) {
            return new ImportedItem(directStack.copy(), originalTag.copy(), sourceDataVersion);
        }

        CompoundTag decodeTag = originalTag;
        int storedDataVersion = sourceDataVersion;
        String backup = "";
        String dfuStatus = "dfu=not_applicable";
        if (fixer == null) {
            dfuStatus = "dfu=unavailable";
        } else if (sourceDataVersion > 0 && sourceDataVersion < currentDataVersion()) {
            dfuStatus = "dfu=attempted";
            try {
                Tag fixed = fixer.update(
                        References.ITEM_STACK,
                        new Dynamic<>(NbtOps.INSTANCE, originalTag.copy()),
                        sourceDataVersion,
                        currentDataVersion()
                ).getValue();
                if (fixed instanceof CompoundTag fixedCompound) {
                    decodeTag = fixedCompound;
                    storedDataVersion = currentDataVersion();
                    dfuStatus = "dfu=applied";
                }
            } catch (RuntimeException exception) {
                dfuStatus = "dfu=failed";
                backup = backupExternalItem(
                        backupService,
                        "external_import_warning",
                        originalTag,
                        context,
                        sourceDataVersion,
                        errorMessage(exception)
                );
                warnings.add(itemWarningContext(context, originalTag, sourceDataVersion, "dfu_item")
                        + " error=\""
                        + errorMessage(exception)
                        + backup
                        + "\"");
            }
        }
        ItemStack stack = parseItemStack(decodeTag, registryAccess);
        if (stack.isEmpty()) {
            if (backup.isBlank()) {
                backup = backupExternalItem(
                        backupService,
                        "external_import_failed",
                        originalTag,
                        context,
                        sourceDataVersion,
                        "skipped because the item could not be decoded; " + dfuStatus
                );
            }
            warnings.add(itemWarningContext(context, originalTag, sourceDataVersion, "decode_item")
                    + " error=\"skipped because the item could not be decoded"
                    + "; "
                    + dfuStatus
                    + backup
                    + "\"");
            return null;
        }
        return new ImportedItem(stack.copy(), decodeTag.copy(), storedDataVersion);
    }

    private static CompoundTag updateHotbarRoot(
            CompoundTag root,
            int dataVersion,
            DataFixer fixer,
            StorageItemBackupService backupService,
            List<String> warnings,
            ImportWarningContext context
    ) {
        if (fixer == null || dataVersion <= 0 || dataVersion >= currentDataVersion()) {
            return root;
        }
        try {
            return DataFixTypes.HOTBAR.updateToCurrentVersion(fixer, root.copy(), dataVersion);
        } catch (RuntimeException exception) {
            String backup = backupExternalItem(
                    backupService,
                    "external_import_warning",
                    root,
                    context,
                    dataVersion,
                    errorMessage(exception)
            );
            warnings.add(context.withStage("dfu_hotbar")
                    + " dataVersion="
                    + dataVersion
                    + " error=\""
                    + errorMessage(exception)
                    + backup
                    + "\"");
            return root;
        }
    }

    private static String itemWarningContext(
            ImportWarningContext context,
            CompoundTag itemTag,
            int dataVersion,
            String stage
    ) {
        return context.withStage(stage)
                + " item="
                + itemId(itemTag)
                + " dv="
                + dataVersion;
    }

    private static String backupExternalItem(
            StorageItemBackupService backupService,
            String reason,
            CompoundTag itemTag,
            ImportWarningContext context,
            int sourceDataVersion,
            String message
    ) {
        if (backupService == null || itemTag == null) {
            return "";
        }
        Path backup = backupService.backup(new StorageItemBackupService.BackupEvent(
                "import",
                reason,
                context == null ? "" : context.source(),
                context == null ? -1 : context.page(),
                context == null ? -1 : context.slot(),
                "",
                -1,
                "",
                sourceDataVersion,
                currentDataVersion(),
                Integer.toHexString(itemTag.hashCode()) + "|dv=" + sourceDataVersion,
                context == null ? "" : context.file(),
                context == null ? -1 : context.row(),
                context == null ? -1 : context.slot(),
                message
        ), itemTag);
        return backup == null || backup.getFileName() == null ? "" : "; backup=" + backup.getFileName();
    }

    private static String itemId(CompoundTag itemTag) {
        if (itemTag == null) {
            return "<unknown>";
        }
        return itemTag.getString("id").filter(id -> !id.isBlank()).orElse("<unknown>");
    }

    private static void logImportResult(
            List<SavedItemStorageService.ExternalPageImport> pages,
            List<String> warnings
    ) {
        int pageCount = pages == null ? 0 : pages.size();
        int itemCount = pages == null
                ? 0
                : pages.stream().mapToInt(page -> page.items() == null ? 0 : page.items().size()).sum();
        int warningCount = warnings == null ? 0 : warnings.size();
        LOGGER.info(
                "[Item Editor] External storage import decoded [pages={}] [items={}] [warnings={}]",
                pageCount,
                itemCount,
                warningCount
        );
        if (warningCount == 0) {
            return;
        }
        LOGGER.warn("[Item Editor] External storage import warning details follow [count={}]", warnings.size());
        for (int index = 0; index < warnings.size(); index++) {
            LOGGER.warn("[Item Editor] External storage import warning [{}/{}] {}", index + 1, warnings.size(), warnings.get(index));
        }
    }

    private static ItemStack parseItemStack(CompoundTag itemTag, RegistryAccess registryAccess) {
        DataResult<ItemStack> optional = ItemStack.OPTIONAL_CODEC.parse(
                registryAccess.createSerializationContext(NbtOps.INSTANCE),
                itemTag
        );
        if (optional.result().isPresent()) {
            return optional.result().get();
        }
        DataResult<ItemStack> strict = ItemStack.CODEC.parse(
                registryAccess.createSerializationContext(NbtOps.INSTANCE),
                itemTag
        );
        if (strict.result().isPresent()) {
            return strict.result().get();
        }
        return parseLegacyItemStack(itemTag);
    }

    private static ItemStack parseLegacyItemStack(CompoundTag itemTag) {
        String rawId = itemTag.getString("id").orElse("");
        if (rawId.isBlank()) {
            return ItemStack.EMPTY;
        }
        Identifier id = Identifier.tryParse(IdFieldNormalizer.normalize(rawId));
        if (id == null) {
            return ItemStack.EMPTY;
        }
        var item = BuiltInRegistries.ITEM.getOptional(id).orElse(null);
        if (item == null) {
            return ItemStack.EMPTY;
        }
        int count = itemTag.getInt("count").orElseGet(() -> itemTag.getByte("Count").map(Byte::intValue).orElse(1));
        return count <= 0 ? ItemStack.EMPTY : new ItemStack(item, count);
    }

    private static List<Integer> numericRowKeys(CompoundTag tag) {
        List<Integer> rows = new ArrayList<>();
        for (String key : tag.keySet()) {
            try {
                rows.add(Integer.parseInt(key));
            } catch (NumberFormatException ignored) {
            }
        }
        rows.sort(Comparator.naturalOrder());
        return rows;
    }

    private static Map<Integer, String> readNbtEditorPageNames(Path path) {
        if (!Files.isRegularFile(path)) {
            return Map.of();
        }
        Map<Integer, String> names = new HashMap<>();
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonElement root = JsonParser.parseReader(reader);
            if (!root.isJsonObject()) {
                return Map.of();
            }
            JsonObject object = root.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                if (entry.getValue().isJsonPrimitive() && entry.getValue().getAsJsonPrimitive().isNumber()) {
                    names.put(entry.getValue().getAsInt(), entry.getKey());
                }
            }
        } catch (IOException | RuntimeException ignored) {
            return Map.of();
        }
        return names;
    }

    private static String readLibrarianName(CompoundTag root) {
        return root.getCompound("librarian")
                .flatMap(meta -> meta.getString("name"))
                .map(ExternalStorageImportService::componentJsonToMarkup)
                .orElse("");
    }

    private static String componentJsonToMarkup(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            return "";
        }
        try {
            DataResult<Component> parsed = ComponentSerialization.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(rawJson));
            return parsed.result().map(TextComponentUtil::toMarkup).orElse(rawJson);
        } catch (RuntimeException ignored) {
            return rawJson;
        }
    }

    private static String importedName(String rawName, String source) {
        String base = rawName == null ? "" : rawName.trim();
        String suffix = " (imported from " + source + ")";
        return base.isBlank() ? suffix.trim() : base + suffix;
    }

    private static CompoundTag readNbt(Path path) throws IOException {
        try {
            return NbtIo.readCompressed(path, NbtAccounter.unlimitedHeap());
        } catch (IOException | RuntimeException compressedFailure) {
            return NbtIo.read(path);
        }
    }

    private static int countMatchingFiles(Path directory, Pattern pattern) {
        return matchingFiles(directory, pattern).size();
    }

    private static Path nbtEditorClientChestPath(Path gameDirectory) {
        return gameDirectory.resolve("nbteditor").resolve("client_chest");
    }

    private static List<NumberedPath> matchingFiles(Path directory, Pattern pattern) {
        if (!Files.isDirectory(directory)) {
            return List.of();
        }
        try (var stream = Files.list(directory)) {
            return stream
                    .filter(Files::isRegularFile)
                    .map(path -> numberedPath(path, pattern))
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparingInt(NumberedPath::number))
                    .toList();
        } catch (IOException ignored) {
            return List.of();
        }
    }

    private static NumberedPath numberedPath(Path path, Pattern pattern) {
        String name = path.getFileName() == null ? "" : path.getFileName().toString().toLowerCase(Locale.ROOT);
        Matcher matcher = pattern.matcher(name);
        if (!matcher.matches()) {
            return null;
        }
        try {
            return new NumberedPath(Integer.parseInt(matcher.group(1)), path);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static int currentDataVersion() {
        try {
            return SharedConstants.getCurrentVersion().dataVersion().version();
        } catch (RuntimeException ignored) {
            return 0;
        }
    }

    private static String errorMessage(Throwable throwable) {
        if (throwable == null) {
            return "unknown error";
        }
        return throwable.getMessage() == null ? throwable.getClass().getSimpleName() : throwable.getMessage();
    }

    private static String safeText(String value) {
        return value == null || value.isBlank() ? "<unknown>" : value;
    }

    private static void emitProgress(
            Consumer<ProgressUpdate> progress,
            String phase,
            String source,
            int current,
            int total,
            int items
    ) {
        if (progress != null) {
            progress.accept(new ProgressUpdate(phase, source, current, total, items));
        }
    }

    public record ScanResult(
            int nbtEditorPages,
            int librarianPages
    ) {
    }

    public record ImportReadResult(
            List<SavedItemStorageService.ExternalPageImport> pages,
            List<String> warnings
    ) {
    }

    public record ProgressUpdate(
            String phase,
            String source,
            int current,
            int total,
            int items
    ) {
    }

    private record ImportedItem(
            ItemStack stack,
            CompoundTag itemTag,
            int dataVersion
    ) {
    }

    private record ImportWarningContext(
            String source,
            int page,
            int slot,
            int row,
            String file,
            String stage
    ) {
        private static ImportWarningContext nbtEditor(NumberedPath file, int slot) {
            return new ImportWarningContext(
                    "NBT Editor",
                    file == null ? -1 : file.number(),
                    slot,
                    -1,
                    fileName(file),
                    ""
            );
        }

        private static ImportWarningContext librarian(NumberedPath file, int row) {
            return new ImportWarningContext(
                    "Librarian",
                    file == null ? -1 : file.number(),
                    -1,
                    row,
                    fileName(file),
                    ""
            );
        }

        private ImportWarningContext withStage(String stage) {
            return new ImportWarningContext(this.source, this.page, this.slot, this.row, this.file, stage);
        }

        private String withError(String error) {
            return this + " error=\"" + error + "\"";
        }

        @Override
        public @NotNull String toString() {
            StringBuilder builder = new StringBuilder()
                    .append("source=\"")
                    .append(safeText(this.source))
                    .append("\"");
            appendNumber(builder, "page", this.page);
            appendNumber(builder, "slot", this.slot);
            appendNumber(builder, "row", this.row);
            builder.append(" file=\"").append(safeText(this.file)).append("\"");
            if (this.stage != null && !this.stage.isBlank()) {
                builder.append(" stage=\"").append(this.stage).append("\"");
            }
            return builder.toString();
        }

        private static String fileName(NumberedPath file) {
            if (file == null || file.path() == null || file.path().getFileName() == null) {
                return "";
            }
            return file.path().getFileName().toString();
        }

        private static void appendNumber(StringBuilder builder, String name, int value) {
            if (value >= 0) {
                builder.append(" ").append(name).append("=").append(value);
            }
        }
    }

    private record NumberedPath(
            int number,
            Path path
    ) {
    }
}
