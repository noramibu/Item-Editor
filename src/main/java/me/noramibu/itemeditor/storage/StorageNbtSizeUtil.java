package me.noramibu.itemeditor.storage;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public final class StorageNbtSizeUtil {

    private StorageNbtSizeUtil() {
    }

    public static int nbtByteSize(CompoundTag tag) {
        if (tag == null) {
            return 0;
        }
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream(256);
            try (DataOutputStream dataOutput = new DataOutputStream(output)) {
                NbtIo.write(tag, dataOutput);
                dataOutput.flush();
            }
            return Math.max(1, output.size());
        } catch (IOException ignored) {
            return Math.max(1, tag.toString().length());
        }
    }
}
