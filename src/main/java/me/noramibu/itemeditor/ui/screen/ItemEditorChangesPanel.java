package me.noramibu.itemeditor.ui.screen;

import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Color;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.VerticalAlignment;
import java.util.List;
import me.noramibu.itemeditor.editor.EditorCategory;
import me.noramibu.itemeditor.editor.ItemEditorChangeSet.Change;
import me.noramibu.itemeditor.editor.ItemEditorChangeSet.ChangeKind;
import me.noramibu.itemeditor.ui.component.ScaledLabelComponent;
import me.noramibu.itemeditor.ui.component.UiFactory;
import me.noramibu.itemeditor.ui.util.UiColors;
import me.noramibu.itemeditor.util.ItemEditorText;
import net.minecraft.network.chat.Component;

final class ItemEditorChangesPanel {
    private static final int MIN_VALUE_PREVIEW_LENGTH = 512;
    private static final int MAX_VALUE_PREVIEW_LENGTH = 16384;
    private static final int SPLIT_VALUES_MIN_WIDTH = 520;
    private static final int APPROXIMATE_CHARACTER_WIDTH = 6;
    private static final int MAX_PREVIEW_LINES = 80;

    private ItemEditorChangesPanel() {}

    static FlowLayout build(ItemEditorScreen screen, EditorCategory category) {
        List<Change> changes = screen.session().changesWithValues().forCategory(category);
        FlowLayout root =
                UiFactory.column().gap(Math.max(6, UiFactory.scaleProfile().spacing() * 2));
        if (changes.isEmpty()) {
            FlowLayout empty = UiFactory.section(
                    ItemEditorText.tr("changes.empty.title"), ItemEditorText.tr("changes.empty.description"));
            UiFactory.appendFillChild(root, empty);
            return root;
        }

        int contentWidth = Math.max(
                80, screen.editorContentWidthHint() - UiFactory.scaleProfile().padding() * 4);
        ChangeLayout layout = changeLayout(screen, changes.size(), contentWidth);
        appendGroup(root, changes, ChangeKind.ADDED, "changes.added", UiColors.SUCCESS, layout);
        appendGroup(root, changes, ChangeKind.MODIFIED, "changes.modified", UiColors.WARNING, layout);
        appendGroup(root, changes, ChangeKind.REMOVED, "changes.removed", UiColors.DANGER, layout);
        return root;
    }

    private static void appendGroup(
            FlowLayout root, List<Change> changes, ChangeKind kind, String titleKey, int color, ChangeLayout layout) {
        List<Change> matching =
                changes.stream().filter(change -> change.kind() == kind).toList();
        if (matching.isEmpty()) {
            return;
        }

        FlowLayout section = UiFactory.card();
        section.child(UiFactory.message(ItemEditorText.tr(titleKey, matching.size()), color));
        for (Change change : matching) {
            FlowLayout row = UiFactory.subCard();
            row.child(UiFactory.title(Component.literal(change.id())).shadow(false));
            row.child(changeValues(change, layout));
            row.horizontalSizing(Sizing.fill(100));
            section.child(row);
        }
        UiFactory.appendFillChild(root, section);
    }

    private static FlowLayout changeValues(Change change, ChangeLayout layout) {
        if (change.kind() == ChangeKind.ADDED) {
            return valueBlock("changes.value", Component.literal(truncate(change.after(), layout.previewLength())));
        }
        if (change.kind() == ChangeKind.REMOVED) {
            return valueBlock("changes.value", Component.literal(truncate(change.before(), layout.previewLength())));
        }

        FlowLayout values = layout.splitValues() ? UiFactory.row() : UiFactory.column();
        values.verticalAlignment(VerticalAlignment.TOP);
        Sizing width = layout.splitValues() ? Sizing.expand(50) : Sizing.fill(100);
        ItemEditorValueDiff.Result diff =
                ItemEditorValueDiff.between(change.before(), change.after(), layout.previewLength());
        values.child(valueBlock("changes.before", diff.before()).horizontalSizing(width));
        values.child(valueBlock("changes.after", diff.after()).horizontalSizing(width));
        return values;
    }

    private static FlowLayout valueBlock(String titleKey, Component value) {
        FlowLayout block =
                UiFactory.column().gap(Math.max(1, UiFactory.scaleProfile().tightSpacing()));
        block.child(UiFactory.message(ItemEditorText.tr(titleKey), UiColors.INFO));
        block.child(new ScaledLabelComponent(value)
                .color(Color.ofRgb(UiColors.MUTED))
                .lineSpacing(UiFactory.scaleProfile().bodyLineSpacing())
                .horizontalSizing(Sizing.fill(100)));
        return block;
    }

    private static ChangeLayout changeLayout(ItemEditorScreen screen, int changeCount, int contentWidth) {
        int lineHeight = Math.max(
                1,
                UiFactory.scaleProfile().captionLineHeight()
                        + UiFactory.scaleProfile().bodyLineSpacing());
        int availableLines = Math.clamp(screen.editorContentHeightHint() / lineHeight, 4, MAX_PREVIEW_LINES);
        int linesPerChange = Math.max(3, availableLines / Math.max(1, Math.min(changeCount, 6)));
        int charactersPerLine = Math.max(20, contentWidth / APPROXIMATE_CHARACTER_WIDTH);
        int previewLength =
                Math.clamp(charactersPerLine * linesPerChange, MIN_VALUE_PREVIEW_LENGTH, MAX_VALUE_PREVIEW_LENGTH);
        return new ChangeLayout(contentWidth >= SPLIT_VALUES_MIN_WIDTH, previewLength);
    }

    private static String truncate(String value, int limit) {
        if (value.length() <= limit) {
            return value;
        }
        return value.substring(0, limit - 3) + "...";
    }

    private record ChangeLayout(boolean splitValues, int previewLength) {}
}
