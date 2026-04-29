package me.noramibu.itemeditor.storage.io;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.function.Supplier;

public final class AtomicFileUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(AtomicFileUtil.class);

    private AtomicFileUtil() {
    }

    public static void ensureDirectories(Path... directories) throws IOException {
        for (Path directory : directories) {
            if (directory != null) {
                Files.createDirectories(directory);
            }
        }
    }

    public static <T> T readJson(Path file, JsonCodec<T> codec, Supplier<T> fallbackSupplier) {
        if (!Files.exists(file)) {
            T fallback = fallbackSupplier.get();
            writeJson(file, codec, fallback);
            return fallback;
        }

        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return codec.read(reader);
        } catch (Exception primaryFailure) {
            Path backup = backupPath(file);
            if (Files.exists(backup)) {
                try (Reader reader = Files.newBufferedReader(backup, StandardCharsets.UTF_8)) {
                    T recovered = codec.read(reader);
                    writeJson(file, codec, recovered);
                    LOGGER.warn("Recovered '{}' from backup '{}'", file, backup);
                    return recovered;
                } catch (Exception backupFailure) {
                    LOGGER.warn("Failed to recover '{}' from backup '{}'", file, backup, backupFailure);
                }
            }

            LOGGER.warn("Failed to read '{}', restoring defaults", file, primaryFailure);
            T fallback = fallbackSupplier.get();
            writeJson(file, codec, fallback);
            return fallback;
        }
    }

    public static <T> void writeJson(Path file, JsonCodec<T> codec, T value) {
        try {
            writeAtomically(file, writer -> codec.write(writer, value));
        } catch (IOException exception) {
            LOGGER.error("Failed to write json '{}'", file, exception);
        }
    }

    public static CompoundTag readNbt(Path file, Supplier<CompoundTag> fallbackSupplier) {
        if (!Files.exists(file)) {
            return fallbackSupplier.get();
        }
        try (BufferedInputStream stream = new BufferedInputStream(Files.newInputStream(file, StandardOpenOption.READ))) {
            return NbtIo.read(new DataInputStream(stream));
        } catch (Exception primaryFailure) {
            Path backup = backupPath(file);
            if (Files.exists(backup)) {
                try (BufferedInputStream stream = new BufferedInputStream(Files.newInputStream(backup, StandardOpenOption.READ))) {
                    CompoundTag recovered = NbtIo.read(new DataInputStream(stream));
                    writeNbt(file, recovered);
                    LOGGER.warn("Recovered NBT '{}' from backup '{}'", file, backup);
                    return recovered;
                } catch (Exception backupFailure) {
                    LOGGER.warn("Failed backup NBT recovery for '{}'", file, backupFailure);
                }
            }
            LOGGER.warn("Failed to read NBT '{}'", file, primaryFailure);
            return fallbackSupplier.get();
        }
    }

    public static void writeNbt(Path file, CompoundTag value) {
        writeNbt(file, value, true);
    }

    public static void writeNbt(Path file, CompoundTag value, boolean fsync) {
        try {
            writeAtomicallyNbt(file, value == null ? new CompoundTag() : value, fsync);
        } catch (IOException exception) {
            LOGGER.error("Failed to write NBT '{}'", file, exception);
        }
    }

    private static void writeAtomically(Path target, WriterAction action) throws IOException {
        Files.createDirectories(target.getParent());
        Path temp = tempPath(target);
        Path backup = backupPath(target);

        try (Writer writer = Files.newBufferedWriter(
                temp,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
        )) {
            action.write(writer);
        }

        fsync(temp);
        backupExisting(target, backup);
        moveReplace(temp, target);
    }

    private static void writeAtomicallyNbt(Path target, CompoundTag value, boolean fsync) throws IOException {
        Files.createDirectories(target.getParent());
        Path temp = tempPath(target);

        try (BufferedOutputStream stream = new BufferedOutputStream(Files.newOutputStream(
                temp,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
        ))) {
            NbtIo.write(value, new DataOutputStream(stream));
            stream.flush();
        }
        if (fsync) {
            fsync(temp);
        }
        moveReplace(temp, target);
    }

    private static void fsync(Path file) throws IOException {
        try (FileOutputStream stream = new FileOutputStream(file.toFile(), true)) {
            stream.getFD().sync();
        }
    }

    private static void backupExisting(Path target, Path backup) throws IOException {
        if (!Files.exists(target)) {
            return;
        }
        Files.copy(target, backup, StandardCopyOption.REPLACE_EXISTING);
    }

    private static void moveReplace(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException ignored) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static Path tempPath(Path target) {
        return target.resolveSibling(target.getFileName() + ".tmp");
    }

    private static Path backupPath(Path target) {
        return target.resolveSibling(target.getFileName() + ".bak");
    }

    public interface JsonCodec<T> {
        T read(Reader reader);

        void write(Writer writer, T value);
    }

    @FunctionalInterface
    private interface WriterAction {
        void write(Writer writer);
    }
}
