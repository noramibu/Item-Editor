package me.noramibu.itemeditor.util;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

public final class NativeFileDialog {
    private NativeFileDialog() {}

    public static CompletableFuture<@Nullable Path> open(
            String title, Path initialDirectory, String filterName, String filterPattern) {
        return CompletableFuture.supplyAsync(() -> {
            String[] extensions = filterPattern.split(";");
            try (MemoryStack memory = MemoryStack.stackPush()) {
                var patterns = memory.mallocPointer(extensions.length);
                for (String extension : extensions) patterns.put(memory.UTF8("*." + extension));
                patterns.flip();
                String selected = TinyFileDialogs.tinyfd_openFileDialog(
                        title, initialDirectory.toString(), patterns, filterName, false);
                return selected == null ? null : Path.of(selected);
            }
        });
    }
}
