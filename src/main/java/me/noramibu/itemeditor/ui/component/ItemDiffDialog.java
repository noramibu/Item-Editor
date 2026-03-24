package me.noramibu.itemeditor.ui.component;

import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Sizing;
import me.noramibu.itemeditor.util.ItemComponentDiffUtil;
import me.noramibu.itemeditor.util.ItemEditorText;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class ItemDiffDialog {

    private static final int DIALOG_WIDTH = 900;
    private static final int DIFF_HEIGHT = 360;

    private ItemDiffDialog() {
    }

    public static FlowLayout create(
            String title,
            String body,
            List<ItemComponentDiffUtil.Entry> entries,
            Runnable onConfirm,
            Runnable onCancel
    ) {
        FlowLayout overlay = DialogUiUtil.overlay();

        FlowLayout dialog = UiFactory.centeredCard(DIALOG_WIDTH).gap(8);
        dialog.child(UiFactory.title(title));
        dialog.child(UiFactory.muted(body, DIALOG_WIDTH - 32));
        dialog.child(UiFactory.muted(ItemEditorText.tr("dialog.apply.diff.summary", entries.size()), DIALOG_WIDTH - 32));

        FlowLayout lines = UiFactory.column();
        if (entries.isEmpty()) {
            lines.child(UiFactory.muted(ItemEditorText.tr("dialog.apply.diff.none"), DIALOG_WIDTH - 48));
        } else {
            for (ItemComponentDiffUtil.Entry entry : entries) {
                lines.child(entryCard(entry));
            }
        }

        dialog.child(DialogUiUtil.scrollCard(lines, DIFF_HEIGHT));

        FlowLayout buttonRow = DialogUiUtil.rightAlignedButtonRow();
        buttonRow.child(UiFactory.button(ItemEditorText.tr("common.cancel"), button -> onCancel.run()).horizontalSizing(Sizing.content()));
        buttonRow.child(UiFactory.button(ItemEditorText.tr("common.save_apply"), button -> onConfirm.run()).horizontalSizing(Sizing.content()));
        dialog.child(buttonRow);

        overlay.child(dialog);
        return overlay;
    }

    private static FlowLayout entryCard(ItemComponentDiffUtil.Entry entry) {
        FlowLayout card = UiFactory.subCard();
        int color = switch (entry.type()) {
            case ADDED -> 0x7ED67A;
            case REMOVED -> 0xFF8A8A;
            case CHANGED -> 0xF2C26B;
        };

        String kindKey = switch (entry.type()) {
            case ADDED -> "dialog.apply.diff.type.added";
            case REMOVED -> "dialog.apply.diff.type.removed";
            case CHANGED -> "dialog.apply.diff.type.changed";
        };

        card.child(UiFactory.message(ItemEditorText.tr("dialog.apply.diff.entry", ItemEditorText.str(kindKey), entry.key()), color));
        if (entry.type() != ItemComponentDiffUtil.EntryType.ADDED) {
            card.child(UiFactory.field(
                    ItemEditorText.tr("dialog.apply.diff.original"),
                    Component.empty(),
                    UiFactory.muted(Component.literal(entry.originalValue()), DIALOG_WIDTH - 120)
            ));
        }
        if (entry.type() != ItemComponentDiffUtil.EntryType.REMOVED) {
            card.child(UiFactory.field(
                    ItemEditorText.tr("screen.preview"),
                    Component.empty(),
                    UiFactory.muted(Component.literal(entry.previewValue()), DIALOG_WIDTH - 120)
            ));
        }
        return card;
    }
}
