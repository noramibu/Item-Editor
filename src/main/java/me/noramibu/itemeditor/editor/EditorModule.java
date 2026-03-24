package me.noramibu.itemeditor.editor;

import me.noramibu.itemeditor.ui.panel.EditorPanel;
import me.noramibu.itemeditor.ui.screen.ItemEditorScreen;

import java.util.function.Function;
import java.util.function.Predicate;

public record EditorModule(
        EditorCategory category,
        Predicate<ItemEditorSession> enabled,
        Function<ItemEditorScreen, EditorPanel> panelFactory
) {
}
