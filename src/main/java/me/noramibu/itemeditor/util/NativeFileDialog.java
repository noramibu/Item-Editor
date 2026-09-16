package me.noramibu.itemeditor.util;

import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.sdl.SDLDialog;
import org.lwjgl.sdl.SDLProperties;
import org.lwjgl.sdl.SDL_DialogFileCallback;
import org.lwjgl.sdl.SDL_DialogFileFilter;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

public final class NativeFileDialog {
    private static final Set<SDL_DialogFileCallback> ACTIVE_CALLBACKS = ConcurrentHashMap.newKeySet();

    private NativeFileDialog() {}

    public static CompletableFuture<@Nullable Path> open(
            String title, Path initialDirectory, String filterName, String filterPattern) {
        CompletableFuture<Path> result = new CompletableFuture<>();
        AtomicReference<SDL_DialogFileCallback> callbackReference = new AtomicReference<>();
        SDL_DialogFileCallback callback = SDL_DialogFileCallback.create((_, fileList, _) -> {
            try {
                long pathPointer = fileList == MemoryUtil.NULL ? MemoryUtil.NULL : MemoryUtil.memGetAddress(fileList);
                result.complete(pathPointer == MemoryUtil.NULL ? null : Path.of(MemoryUtil.memUTF8(pathPointer)));
            } catch (RuntimeException failure) {
                result.completeExceptionally(failure);
            } finally {
                SDL_DialogFileCallback activeCallback = callbackReference.getAndSet(null);
                if (activeCallback != null) {
                    ACTIVE_CALLBACKS.remove(activeCallback);
                    activeCallback.free();
                }
            }
        });
        callbackReference.set(callback);
        ACTIVE_CALLBACKS.add(callback);

        int properties = SDLProperties.SDL_CreateProperties();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            SDL_DialogFileFilter.Buffer filters = SDL_DialogFileFilter.calloc(1, stack);
            filters.get(0).name(stack.UTF8(filterName)).pattern(stack.UTF8(filterPattern));

            SDLProperties.SDL_SetPointerProperty(
                    properties, SDLDialog.SDL_PROP_FILE_DIALOG_FILTERS_POINTER, filters.address());
            SDLProperties.SDL_SetNumberProperty(properties, SDLDialog.SDL_PROP_FILE_DIALOG_NFILTERS_NUMBER, 1);
            SDLProperties.SDL_SetPointerProperty(
                    properties,
                    SDLDialog.SDL_PROP_FILE_DIALOG_WINDOW_POINTER,
                    Minecraft.getInstance().getWindow().handle());
            SDLProperties.SDL_SetStringProperty(
                    properties, SDLDialog.SDL_PROP_FILE_DIALOG_LOCATION_STRING, initialDirectory.toString());
            SDLProperties.SDL_SetStringProperty(properties, SDLDialog.SDL_PROP_FILE_DIALOG_TITLE_STRING, title);
            SDLProperties.SDL_SetBooleanProperty(properties, SDLDialog.SDL_PROP_FILE_DIALOG_MANY_BOOLEAN, false);
            SDLDialog.SDL_ShowFileDialogWithProperties(
                    SDLDialog.SDL_FILEDIALOG_OPENFILE, callback, MemoryUtil.NULL, properties);
        } catch (RuntimeException | LinkageError failure) {
            ACTIVE_CALLBACKS.remove(callback);
            callbackReference.set(null);
            callback.free();
            result.completeExceptionally(failure);
        } finally {
            SDLProperties.SDL_DestroyProperties(properties);
        }

        return result;
    }
}
