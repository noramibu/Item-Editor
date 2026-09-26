package me.noramibu.itemeditor.ui.panel;

import io.wispforest.owo.ui.component.TextBoxComponent;
import java.util.function.Consumer;
import me.noramibu.itemeditor.editor.ItemEditorFieldReset;
import me.noramibu.itemeditor.ui.component.UiFactory;
import me.noramibu.itemeditor.ui.screen.ItemEditorScreen;
import me.noramibu.itemeditor.util.ItemEditorText;
import net.minecraft.network.chat.Component;

public final class PanelBindings {

    private PanelBindings() {}

    public static TextBoxComponent textBox(ItemEditorScreen screen, String key) {
        return textBox(screen, key, mutation -> mutate(screen, mutation));
    }

    public static TextBoxComponent textBox(ItemEditorScreen screen, String key, Consumer<Runnable> mutate) {
        String fullKey = ItemEditorText.key(key);
        var binding = ItemEditorFieldReset.text(fullKey);
        var state = screen.session().state();
        return UiFactory.bindField(
                Component.translatable(fullKey),
                UiFactory.textBox(
                        binding.read().apply(state),
                        value -> mutate.accept(() -> binding.write().accept(state, value))));
    }

    public static void mutate(ItemEditorScreen screen, Runnable mutation) {
        screen.session().state().rawEditorEdited = false;
        mutation.run();
        screen.session().rebuildPreview();
    }

    public static void mutate(ItemEditorScreen screen, Runnable mutation, Runnable afterRebuild) {
        mutate(screen, mutation);
        afterRebuild.run();
    }

    public static void mutateRefresh(ItemEditorScreen screen, Runnable mutation) {
        mutate(screen, mutation, screen::refreshCurrentPanel);
    }

    static Consumer<String> text(ItemEditorScreen screen, Consumer<String> updater) {
        return value -> mutate(screen, () -> updater.accept(value));
    }

    static Consumer<Boolean> toggle(ItemEditorScreen screen, Consumer<Boolean> updater) {
        return value -> mutate(screen, () -> updater.accept(value));
    }

    static <T> Consumer<T> value(ItemEditorScreen screen, Consumer<T> updater) {
        return value -> mutate(screen, () -> updater.accept(value));
    }

    static <T> Consumer<T> value(ItemEditorScreen screen, Consumer<T> updater, Runnable afterRebuild) {
        return value -> mutate(screen, () -> updater.accept(value), afterRebuild);
    }
}
