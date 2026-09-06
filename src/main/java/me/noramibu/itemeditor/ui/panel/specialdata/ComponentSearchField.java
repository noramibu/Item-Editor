package me.noramibu.itemeditor.ui.panel.specialdata;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import me.noramibu.itemeditor.editor.EditorCategory;
import me.noramibu.itemeditor.ui.component.EditorSearchDialog;
import me.noramibu.itemeditor.util.ItemEditorText;
import net.minecraft.network.chat.Component;

interface ComponentSearchField {
    String key();

    default Component label(Object... args) {
        return ItemEditorText.tr(key(), args);
    }

    default String text(Object... args) {
        return label(args).getString();
    }

    default EditorSearchDialog.Target target(
            SpecialDataPanelContext context,
            EditorCategory category,
            List<String> parents,
            Supplier<String> scope,
            Runnable expand) {
        return target(context, category, parents, scope, expand, ItemEditorText.key(key()));
    }

    default EditorSearchDialog.Target target(
            SpecialDataPanelContext context,
            EditorCategory category,
            List<String> parents,
            Supplier<String> scope,
            Runnable expand,
            String anchor) {
        List<String> path = new ArrayList<>();
        path.add(category.title().getString());
        path.addAll(parents);
        if (!path.getLast().equals(text())) path.add(text());
        return new EditorSearchDialog.Target(
                path, key() + " " + text() + " " + EditorSearchDialog.english(key()), () -> {
                    String currentScope = scope.get();
                    if (currentScope == null) return;
                    expand.run();
                    context.screen()
                            .revealSearchTarget(category, new EditorSearchDialog.Location(currentScope, anchor));
                });
    }

    static String scope(String group, int index) {
        return "component-entry-" + group + "-" + index;
    }

    static <T> int identityIndex(List<T> entries, T entry) {
        for (int index = 0; index < entries.size(); index++) {
            if (entries.get(index) == entry) return index;
        }
        return -1;
    }

    static <T> String scope(String group, List<T> entries, T entry) {
        int index = identityIndex(entries, entry);
        return index < 0 ? null : scope(group, index);
    }
}
