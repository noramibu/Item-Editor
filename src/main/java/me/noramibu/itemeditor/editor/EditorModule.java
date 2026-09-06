package me.noramibu.itemeditor.editor;

import java.util.function.Function;
import java.util.function.Predicate;
import me.noramibu.itemeditor.ui.panel.EditorPanel;
import me.noramibu.itemeditor.ui.screen.ItemEditorScreen;

public record EditorModule(
        EditorCategory category,
        Predicate<ItemEditorSession> enabled,
        Function<ItemEditorScreen, EditorPanel> panelFactory) {}
