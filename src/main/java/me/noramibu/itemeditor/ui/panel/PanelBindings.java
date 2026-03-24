package me.noramibu.itemeditor.ui.panel;

import me.noramibu.itemeditor.ui.screen.ItemEditorScreen;

import java.util.function.Consumer;

public final class PanelBindings {

    private PanelBindings() {
    }

    public static void mutate(ItemEditorScreen screen, Runnable mutation) {
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
