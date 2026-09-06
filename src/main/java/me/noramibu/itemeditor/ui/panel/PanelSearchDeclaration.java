package me.noramibu.itemeditor.ui.panel;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import me.noramibu.itemeditor.editor.EditorCategory;
import me.noramibu.itemeditor.ui.component.EditorSearchDialog;
import me.noramibu.itemeditor.ui.screen.ItemEditorScreen;
import me.noramibu.itemeditor.util.ItemEditorText;
import net.minecraft.network.chat.Component;

interface PanelSearchDeclaration {
    String path();

    default Component label(Object... args) {
        return ItemEditorText.tr(path(), args);
    }

    default String text(Object... args) {
        return label(args).getString();
    }

    default EditorSearchDialog.Target target(
            ItemEditorScreen screen,
            EditorCategory category,
            String scope,
            List<String> parents,
            String aliases,
            Runnable expand,
            Object... args) {
        return target(scope, parents, aliases, expand, location -> screen.revealSearchTarget(category, location), args);
    }

    default EditorSearchDialog.Target target(
            String scope,
            List<String> parents,
            String aliases,
            Runnable expand,
            Consumer<EditorSearchDialog.Location> reveal,
            Object... args) {
        List<String> labels = new ArrayList<>(parents);
        labels.add(text(args));
        String terms = path() + " " + EditorSearchDialog.english(path()) + " " + aliases;
        return new EditorSearchDialog.Target(labels, terms, () -> {
            expand.run();
            reveal.accept(new EditorSearchDialog.Location(scope, ItemEditorText.key(path())));
        });
    }

    static List<String> parents(EditorCategory category, Component... sections) {
        List<String> labels = new ArrayList<>();
        labels.add(category.title().getString());
        for (Component section : sections) {
            labels.add(section.getString());
        }
        return List.copyOf(labels);
    }
}
