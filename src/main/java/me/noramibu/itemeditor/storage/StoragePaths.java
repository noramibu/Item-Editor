package me.noramibu.itemeditor.storage;

import java.nio.file.Path;
import net.minecraft.client.Minecraft;

public record StoragePaths(Path root) {

    public StoragePaths(Minecraft minecraft) {
        this(minecraft.gameDirectory.toPath().resolve("itemeditor"));
    }

    public Path dataDirectory() {
        return this.root.resolve("data");
    }

    public Path savedDirectory() {
        return this.root.resolve("saved");
    }

    public Path savedDataDirectory() {
        return this.savedDirectory().resolve("data");
    }

    public Path storageBackupsDirectory() {
        return this.root.resolve("backups").resolve("storage");
    }

    public Path colorsFile() {
        return this.dataDirectory().resolve("colors.json");
    }

    public Path rawEditorOptionsFile() {
        return this.dataDirectory().resolve("raw-editor.json");
    }

    public Path preferencesFile() {
        return this.dataDirectory().resolve("preferences.json");
    }

    public Path savedIndexFile() {
        return this.savedDirectory().resolve("index.nbt");
    }

    public Path chunkFile(String chunkId) {
        return this.savedDataDirectory().resolve(chunkId + ".nbt");
    }
}
