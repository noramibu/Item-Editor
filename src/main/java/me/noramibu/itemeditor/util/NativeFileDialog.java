package me.noramibu.itemeditor.util;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

public final class NativeFileDialog {
    private NativeFileDialog() {}

    public static CompletableFuture<@Nullable Path> open(
            String title, Path initialDirectory, String filterName, String filterPattern) {
        return CompletableFuture.supplyAsync(() -> {
            String selected = TinyFileDialogs.tinyfd_openFileDialog(
                    title, initialDirectory.toAbsolutePath().toString(), null, filterName, false);
            return selected == null ? null : Path.of(selected);
        });
    }
}
