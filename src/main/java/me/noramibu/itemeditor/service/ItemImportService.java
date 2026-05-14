package me.noramibu.itemeditor.service;

import com.mojang.datafixers.DataFixer;
import com.mojang.serialization.DataResult;
import me.noramibu.itemeditor.util.IdFieldNormalizer;
import me.noramibu.itemeditor.util.ItemEditorText;
import me.noramibu.itemeditor.util.RawItemDataUtil;
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
import net.minecraft.resources.Identifier;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ItemImportService {
    private static final int HOTBAR_SLOT_COUNT = 9;
    private static final int DEFAULT_HOTBAR_DATA_VERSION = 1343;
    private static final ExecutorService IMPORT_EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "itemeditor-import");
        thread.setDaemon(true);
        return thread;
    });

    public RawItemDataUtil.ParseResult parseText(String input, RegistryAccess registryAccess) {
        return RawItemDataUtil.parseFlexible(input, registryAccess);
    }

    public CompletableFuture<ImportResult> importFile(Path path, RegistryAccess registryAccess, DataFixer fixerUpper) {
        return CompletableFuture.supplyAsync(() -> {
            if (path == null) {
                return ImportResult.failure(ItemEditorText.str("import.cancelled"));
            }
            String extension = this.extension(path);
            try {
                return switch (extension) {
                    case "json", "snbt" -> this.resultFromParse(RawItemDataUtil.parseFlexible(Files.readString(path, StandardCharsets.UTF_8), registryAccess));
                    case "nbt" -> this.importNbt(path, registryAccess, fixerUpper);
                    default -> ImportResult.failure(ItemEditorText.str("import.unsupported_extension", extension.isBlank() ? path.getFileName().toString() : extension));
                };
            } catch (IOException exception) {
                return ImportResult.failure(ItemEditorText.str("import.read_failed", this.errorMessage(exception)));
            } catch (RuntimeException exception) {
                return ImportResult.failure(ItemEditorText.str("import.failed", this.errorMessage(exception)));
            }
        }, IMPORT_EXECUTOR);
    }

    private ImportResult importNbt(Path path, RegistryAccess registryAccess, DataFixer fixerUpper) throws IOException {
        try {
            CompoundTag tag = NbtIo.readCompressed(path, NbtAccounter.unlimitedHeap());
            return this.resultFromTag(tag, registryAccess, fixerUpper);
        } catch (IOException | RuntimeException compressedFailure) {
            try {
                CompoundTag tag = NbtIo.read(path);
                return this.resultFromTag(tag, registryAccess, fixerUpper);
            } catch (IOException | RuntimeException rawFailure) {
                return this.resultFromParse(RawItemDataUtil.parseFlexible(Files.readString(path, StandardCharsets.UTF_8), registryAccess));
            }
        }
    }

    private ImportResult resultFromTag(CompoundTag tag, RegistryAccess registryAccess, DataFixer fixerUpper) {
        List<ItemStack> stacks = this.extractMultiItemStacks(tag, registryAccess, fixerUpper);
        if (stacks.size() > 1) {
            return ImportResult.successMany(stacks, ItemEditorText.str("import.multi_success", stacks.size()));
        }
        if (stacks.size() == 1) {
            return ImportResult.success(stacks.getFirst(), ItemEditorText.str("import.success"));
        }
        return this.resultFromParse(RawItemDataUtil.parseTagFlexible(tag, registryAccess));
    }

    private ImportResult resultFromParse(RawItemDataUtil.ParseResult parseResult) {
        if (parseResult == null || !parseResult.success()) {
            String error = parseResult == null ? ItemEditorText.str("raw.unknown_error") : parseResult.error();
            return ImportResult.failure(ItemEditorText.str("import.parse_failed", error));
        }
        ItemStack stack = parseResult.stack().copy();
        if (stack.isEmpty()) {
            return ImportResult.failure(ItemEditorText.str("import.empty_item"));
        }
        return ImportResult.success(stack, ItemEditorText.str("import.success"));
    }

    private List<ItemStack> extractMultiItemStacks(CompoundTag tag, RegistryAccess registryAccess, DataFixer fixerUpper) {
        List<ItemStack> hotbarStacks = this.extractHotbarStacks(tag, registryAccess, fixerUpper);
        if (!hotbarStacks.isEmpty()) {
            return hotbarStacks;
        }
        return this.extractItemsList(tag, registryAccess);
    }

    private List<ItemStack> extractHotbarStacks(CompoundTag tag, RegistryAccess registryAccess, DataFixer fixerUpper) {
        if (!this.hasHotbarList(tag)) {
            return List.of();
        }

        List<ItemStack> directStacks = this.extractHotbarStacksWithCodec(tag, registryAccess);
        if (!directStacks.isEmpty()) {
            return directStacks;
        }

        if (fixerUpper != null) {
            try {
                int version = NbtUtils.getDataVersion(tag, DEFAULT_HOTBAR_DATA_VERSION);
                CompoundTag fixedTag = DataFixTypes.HOTBAR.updateToCurrentVersion(fixerUpper, tag.copy(), version);
                List<ItemStack> fixedStacks = this.extractHotbarStacksWithCodec(fixedTag, registryAccess);
                if (!fixedStacks.isEmpty()) {
                    return fixedStacks;
                }
            } catch (RuntimeException ignored) {
                // Some hotbar-like files are not in vanilla HOTBAR datafixer shape; parse their lists directly below.
            }
        }

        return this.extractHotbarStacksFromLists(tag, registryAccess);
    }

    private List<ItemStack> extractHotbarStacksWithCodec(CompoundTag tag, RegistryAccess registryAccess) {
        List<ItemStack> stacks = new ArrayList<>();
        for (int index = 0; index < HOTBAR_SLOT_COUNT; index++) {
            Tag hotbarEntry = tag.get(Integer.toString(index));
            if (hotbarEntry == null) {
                continue;
            }
            DataResult<Hotbar> hotbar = Hotbar.CODEC.parse(NbtOps.INSTANCE, hotbarEntry);
            hotbar.result().ifPresent(value -> stacks.addAll(this.nonEmptyCopies(value.load(registryAccess))));
        }
        return stacks;
    }

    private List<ItemStack> extractHotbarStacksFromLists(CompoundTag tag, RegistryAccess registryAccess) {
        List<ItemStack> stacks = new ArrayList<>();
        for (int index = 0; index < HOTBAR_SLOT_COUNT; index++) {
            Tag hotbarEntry = tag.get(Integer.toString(index));
            if (hotbarEntry instanceof ListTag list) {
                stacks.addAll(this.parseItemList(list, registryAccess));
            }
        }
        return stacks;
    }

    private boolean hasHotbarList(CompoundTag tag) {
        for (int index = 0; index < HOTBAR_SLOT_COUNT; index++) {
            if (tag.get(Integer.toString(index)) instanceof ListTag) {
                return true;
            }
        }
        return false;
    }

    private List<ItemStack> extractItemsList(CompoundTag tag, RegistryAccess registryAccess) {
        return tag.getList("Items")
                .map(list -> this.parseItemList(list, registryAccess))
                .orElseGet(List::of);
    }

    private List<ItemStack> parseItemList(ListTag list, RegistryAccess registryAccess) {
        List<SlottedStack> slottedStacks = new ArrayList<>();
        for (int index = 0; index < list.size(); index++) {
            Tag entry = list.get(index);
            if (!(entry instanceof CompoundTag itemTag)) {
                continue;
            }
            ItemStack stack = this.parseItemStack(itemTag, registryAccess);
            if (stack.isEmpty()) {
                continue;
            }
            slottedStacks.add(new SlottedStack(itemTag.getByteOr("Slot", (byte) index), index, stack.copy()));
        }
        slottedStacks.sort(Comparator.comparingInt(SlottedStack::slot).thenComparingInt(SlottedStack::index));
        return slottedStacks.stream().map(SlottedStack::stack).toList();
    }

    private ItemStack parseItemStack(CompoundTag itemTag, RegistryAccess registryAccess) {
        DataResult<ItemStack> optional = ItemStack.OPTIONAL_CODEC.parse(registryAccess.createSerializationContext(NbtOps.INSTANCE), itemTag);
        if (optional.result().isPresent()) {
            return optional.result().get();
        }
        DataResult<ItemStack> strict = ItemStack.CODEC.parse(registryAccess.createSerializationContext(NbtOps.INSTANCE), itemTag);
        if (strict.result().isPresent()) {
            return strict.result().get();
        }
        return this.parseLegacyItemStack(itemTag);
    }

    private ItemStack parseLegacyItemStack(CompoundTag itemTag) {
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
        if (count <= 0) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(item, count);
    }

    private List<ItemStack> nonEmptyCopies(List<ItemStack> stacks) {
        return stacks.stream()
                .filter(stack -> stack != null && !stack.isEmpty())
                .map(ItemStack::copy)
                .toList();
    }

    private String extension(Path path) {
        String filename = path == null || path.getFileName() == null ? "" : path.getFileName().toString();
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot >= filename.length() - 1) {
            return "";
        }
        return filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private String errorMessage(Throwable throwable) {
        String message = throwable.getMessage();
        return message == null || message.isBlank() ? throwable.getClass().getSimpleName() : message;
    }

    private record SlottedStack(int slot, int index, ItemStack stack) {
    }

    public record ImportResult(boolean success, ItemStack stack, List<ItemStack> stacks, String message) {
        public static ImportResult success(ItemStack stack, String message) {
            return new ImportResult(true, stack.copy(), List.of(stack.copy()), message);
        }

        public static ImportResult successMany(List<ItemStack> stacks, String message) {
            List<ItemStack> copies = stacks.stream()
                    .filter(stack -> stack != null && !stack.isEmpty())
                    .map(ItemStack::copy)
                    .toList();
            return new ImportResult(true, ItemStack.EMPTY, copies, message);
        }

        public static ImportResult failure(String message) {
            return new ImportResult(false, ItemStack.EMPTY, List.of(), message);
        }

        public boolean hasManyStacks() {
            return this.stacks.size() > 1;
        }
    }
}
