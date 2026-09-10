package me.noramibu.itemeditor.ui.panel;

import java.util.ArrayList;
import java.util.List;
import me.noramibu.itemeditor.editor.EditorCategory;
import me.noramibu.itemeditor.ui.component.EditorSearchDialog;
import me.noramibu.itemeditor.ui.screen.ItemEditorScreen;
import me.noramibu.itemeditor.util.ItemEditorText;

final class DocumentPanelSearch {
    private DocumentPanelSearch() {}

    record Label(String key, String aliases) implements PanelSearchDeclaration {
        Label(String key) {
            this(key, "");
        }

        @Override
        public String path() {
            return key;
        }

        String fullKey() {
            return ItemEditorText.key(key);
        }

        String terms() {
            return fullKey() + " " + EditorSearchDialog.english(key) + " " + aliases;
        }
    }

    static EditorSearchDialog.Target target(
            ItemEditorScreen screen,
            EditorCategory category,
            List<String> parents,
            String label,
            String terms,
            String scope,
            String anchor,
            Runnable expand) {
        var path = new ArrayList<String>();
        path.add(category.title().getString());
        path.addAll(parents);
        if (!path.getLast().equals(label)) path.add(label);
        EditorSearchDialog.Location location = new EditorSearchDialog.Location(scope, anchor);
        return new EditorSearchDialog.Target(path, terms, location, () -> {
            expand.run();
            screen.revealSearchTarget(category, location);
        });
    }
}
