package me.noramibu.itemeditor.service;

import com.mojang.datafixers.DataFixer;
import com.mojang.serialization.DataResult;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import me.noramibu.itemeditor.util.IdFieldNormalizer;
import me.noramibu.itemeditor.util.ItemEditorText;
import me.noramibu.itemeditor.util.RawItemDataUtil;
import net.minecraft.client.player.inventory.Hotbar;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.item.ItemStack;

public final class ItemImportService {
    private static final int HOTBAR_SLOT_COUNT = 9;
    private static final int DEFAULT_HOTBAR_DATA_VERSION = 1343;

    public RawItemDataUtil.ParseResult parseText(String input, RegistryAccess registryAccess) {
        return RawItemDataUtil.parseFlexible(input, registryAccess);
    }

    ImportResult resultFromTag(CompoundTag tag, RegistryAccess registryAccess, DataFixer fixerUpper) {
        if (tag != null && tag.getStringOr("backupType", "").equals("storage_page")) {
            return ImportResult.failure(ItemEditorText.str("import.storage_page"));
        }
        List<ItemStack> stacks = this.extractHotbarStacks(tag, registryAccess, fixerUpper);
        if (stacks.isEmpty()) {
            stacks = this.extractItemsList(tag, registryAccess);
        }
        if (stacks.size() > 1) {
            return ImportResult.successMany(stacks, ItemEditorText.str("import.multi_success", stacks.size()));
        }
        if (stacks.size() == 1) {
            return ImportResult.success(stacks.getFirst(), ItemEditorText.str("import.success"));
        }
        return this.resultFromParse(RawItemDataUtil.parseTagFlexible(tag, registryAccess));
    }

    ImportResult resultFromParse(RawItemDataUtil.ParseResult parseResult) {
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
            ItemStack stack = parseItemStack(itemTag, registryAccess);
            if (stack.isEmpty()) {
                continue;
            }
            slottedStacks.add(new SlottedStack(itemTag.getByteOr("Slot", (byte) index), index, stack.copy()));
        }
        slottedStacks.sort(Comparator.comparingInt(SlottedStack::slot).thenComparingInt(SlottedStack::index));
        return slottedStacks.stream().map(SlottedStack::stack).toList();
    }

    static ItemStack parseItemStack(CompoundTag itemTag, RegistryAccess registryAccess) {
        DataResult<ItemStack> optional =
                ItemStack.OPTIONAL_CODEC.parse(registryAccess.createSerializationContext(NbtOps.INSTANCE), itemTag);
        return optional.result().orElseGet(() -> {
            DataResult<ItemStack> strict =
                    ItemStack.CODEC.parse(registryAccess.createSerializationContext(NbtOps.INSTANCE), itemTag);
            return strict.result().orElseGet(() -> parseLegacyItemStack(itemTag));
        });
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
        int count = itemTag.getInt("count")
                .orElseGet(() -> itemTag.getByte("Count").map(Byte::intValue).orElse(1));
        return count <= 0 ? ItemStack.EMPTY : new ItemStack(item, count);
    }

    private List<ItemStack> nonEmptyCopies(List<ItemStack> stacks) {
        return stacks.stream()
                .filter(stack -> stack != null && !stack.isEmpty())
                .map(ItemStack::copy)
                .toList();
    }

    private record SlottedStack(int slot, int index, ItemStack stack) {}

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
