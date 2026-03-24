package me.noramibu.itemeditor.util;

import com.mojang.serialization.DataResult;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.SnbtPrinterTagVisitor;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

public final class RawItemDataUtil {

    private RawItemDataUtil() {
    }

    public static String serialize(ItemStack stack, RegistryAccess registryAccess) {
        if (stack.isEmpty()) {
            return ItemEditorText.str("raw.empty");
        }

        DataResult<Tag> result = ItemStack.CODEC.encodeStart(registryAccess.createSerializationContext(NbtOps.INSTANCE), stack);
        return result.result()
                .map(RawItemDataUtil::printTag)
                .orElseGet(() -> ItemEditorText.str(
                        "raw.serialize_failed",
                        result.error().map(error -> error.message()).orElse(ItemEditorText.str("raw.unknown_error"))
                ));
    }

    private static String printTag(Tag tag) {
        return new SnbtPrinterTagVisitor().visit(tag);
    }
}
