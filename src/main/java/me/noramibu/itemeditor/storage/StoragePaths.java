package me.noramibu.itemeditor.storage;

import net.minecraft.client.Minecraft;

import java.nio.file.Path;

public final class StoragePaths {

    private final Path root;
    private final Path dataDirectory;
    private final Path savedDirectory;
    private final Path savedDataDirectory;

    public StoragePaths(Minecraft minecraft) {
        Path gameDir = minecraft.gameDirectory.toPath();
        this.root = gameDir.resolve("itemeditor");
        this.dataDirectory = this.root.resolve("data");
        this.savedDirectory = this.root.resolve("saved");
        this.savedDataDirectory = this.savedDirectory.resolve("data");
    }

    public Path root() {
        return this.root;
    }

    public Path dataDirectory() {
        return this.dataDirectory;
    }

    public Path savedDirectory() {
        return this.savedDirectory;
    }

    public Path savedDataDirectory() {
        return this.savedDataDirectory;
    }

    public Path colorsFile() {
        return this.dataDirectory.resolve("colors.json");
    }

    public Path rawEditorOptionsFile() {
        return this.dataDirectory.resolve("raw-editor.json");
    }

    public Path savedIndexFile() {
        return this.savedDirectory.resolve("index.nbt");
    }

    public Path chunkFile(String chunkId) {
        return this.savedDataDirectory.resolve(chunkId + ".nbt");
    }
}
