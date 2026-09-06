package me.noramibu.itemeditor.ui.panel.specialdata;

import io.wispforest.owo.ui.core.ParentUIComponent;
import io.wispforest.owo.ui.core.UIComponent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import me.noramibu.itemeditor.editor.EditorCategory;
import me.noramibu.itemeditor.ui.component.EditorSearchDialog;
import me.noramibu.itemeditor.util.ItemEditorCapabilities;
import me.noramibu.itemeditor.util.ItemEditorText;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

public final class SpecialDataSearch {
    public interface Field {
        String key();

        default Component text() {
            return ItemEditorText.tr(key());
        }
    }

    private SpecialDataSearch() {}

    public static List<EditorSearchDialog.Target> targets(
            SpecialDataPanelContext context,
            EditorCategory category,
            String title,
            String scope,
            Runnable expand,
            Field... fields) {
        return targets(context, category, List.of(ItemEditorText.str(title)), scope, expand, fields);
    }

    public static List<EditorSearchDialog.Target> targets(
            SpecialDataPanelContext context,
            EditorCategory category,
            List<String> path,
            String scope,
            Runnable expand,
            Field... fields) {
        return targets(
                categoryTitle(context, category),
                path,
                scope,
                expand,
                location -> context.screen().revealSearchTarget(category, location),
                fields);
    }

    static List<EditorSearchDialog.Target> targets(
            String category,
            List<String> path,
            String scope,
            Runnable expand,
            Consumer<EditorSearchDialog.Location> reveal,
            Field... fields) {
        var result = new ArrayList<EditorSearchDialog.Target>();
        for (Field field : fields) {
            var fieldPath = new ArrayList<String>();
            fieldPath.add(category);
            fieldPath.addAll(path);
            fieldPath.add(field.text().getString());
            result.add(new EditorSearchDialog.Target(
                    fieldPath,
                    field.key() + " " + field.text().getString() + " " + EditorSearchDialog.english(field.key()),
                    () -> {
                        expand.run();
                        reveal.accept(new EditorSearchDialog.Location(scope, ItemEditorText.key(field.key())));
                    }));
        }
        return result;
    }

    record Control(String id, Component label, String terms) {
        EditorSearchDialog.Target target(
                String category,
                List<String> parents,
                String scope,
                Runnable expand,
                Consumer<EditorSearchDialog.Location> reveal) {
            var path = new ArrayList<String>();
            path.add(category);
            path.addAll(parents);
            path.add(label.getString());
            return new EditorSearchDialog.Target(path, terms + " " + label.getString(), () -> {
                expand.run();
                reveal.accept(new EditorSearchDialog.Location(scope, id));
            });
        }

        EditorSearchDialog.Target target(
                SpecialDataPanelContext context,
                EditorCategory category,
                List<String> parents,
                String scope,
                Runnable expand) {
            return target(categoryTitle(context, category), parents, scope, expand, location -> context.screen()
                    .revealSearchTarget(category, location));
        }
    }

    static void bindControls(UIComponent component, List<Control> controls) {
        if (component instanceof AbstractWidget widget) {
            Component message = widget.getMessage();
            for (Control control : controls) {
                if ((message.getContents() instanceof TranslatableContents text
                                && text.getKey().equals(control.id()))
                        || message.getString().equals(control.label().getString())) {
                    component.id(control.id());
                    break;
                }
            }
        }
        if (component instanceof ParentUIComponent parent) {
            for (UIComponent child : parent.children()) bindControls(child, controls);
        }
    }

    static String scope(String prefix, Object entry) {
        return prefix + "-" + Integer.toUnsignedString(System.identityHashCode(entry));
    }

    static String categoryTitle(SpecialDataPanelContext context, EditorCategory category) {
        return (category == EditorCategory.SPECIAL_DATA
                        ? ItemEditorCapabilities.specialDataTitle(context.originalStack())
                        : category.title())
                .getString();
    }
}
