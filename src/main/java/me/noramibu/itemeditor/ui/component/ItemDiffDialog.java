package me.noramibu.itemeditor.ui.component;

import io.wispforest.owo.ui.container.FlowLayout;
import me.noramibu.itemeditor.util.ItemComponentDiffUtil;
import me.noramibu.itemeditor.util.ItemEditorText;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class ItemDiffDialog {

    private static final int DIALOG_WIDTH = 900;
    private static final int DIALOG_GAP = 8;
    private static final int DIFF_HEIGHT = 360;
    private static final int BODY_TEXT_MARGIN = 32;
    private static final int LINE_TEXT_MARGIN = 48;
    private static final int VALUE_TEXT_MARGIN = 120;
    private static final int COMPACT_BUTTON_WIDTH_THRESHOLD = 420;
    private static final int COMPACT_BUTTON_ROWS = 2;
    private static final int BUTTON_RESERVE_COMPACT_EXTRA = 22;
    private static final int BUTTON_RESERVE_NORMAL_EXTRA = 16;
    private static final int HEADER_RESERVE = 96;
    private static final int CONTENT_MIN_HEIGHT = 140;
    private static final int DIALOG_MIN_HEIGHT = 240;
    private static final int COLOR_ADDED = 0x7ED67A;
    private static final int COLOR_REMOVED = 0xFF8A8A;
    private static final int COLOR_CHANGED = 0xF2C26B;
    private static final int FOOTER_BUTTON_MIN_WIDTH = 72;
    private static final int FOOTER_BUTTON_MAX_WIDTH = 160;
    private static final int FOOTER_BUTTON_WIDTH_DIVISOR = 4;
    private static final int FOOTER_BUTTON_TEXT_MIN_WIDTH = 24;
    private static final int FOOTER_BUTTON_TEXT_RESERVE = 10;
    private static final String EMPTY_TEXT = "";

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
        int dialogWidth = DialogUiUtil.dialogWidth(DIALOG_WIDTH);
        int bodyTextWidth = DialogUiUtil.dialogTextWidth(dialogWidth, BODY_TEXT_MARGIN);
        int lineTextWidth = DialogUiUtil.dialogTextWidth(dialogWidth, LINE_TEXT_MARGIN);
        int valueTextWidth = DialogUiUtil.dialogTextWidth(dialogWidth, VALUE_TEXT_MARGIN);
        boolean compactButtons = DialogUiUtil.compactButtons(dialogWidth, COMPACT_BUTTON_WIDTH_THRESHOLD);
        int buttonReserve = DialogUiUtil.buttonRowReserve(
                compactButtons,
                COMPACT_BUTTON_ROWS,
                BUTTON_RESERVE_COMPACT_EXTRA,
                BUTTON_RESERVE_NORMAL_EXTRA
        );
        int headerReserve = UiFactory.scaledPixels(HEADER_RESERVE);
        int diffHeight = Math.min(
                DialogUiUtil.scrollHeight(DIFF_HEIGHT),
                DialogUiUtil.availableDialogContentHeight(headerReserve + buttonReserve, CONTENT_MIN_HEIGHT)
        );
        int dialogHeight = DialogUiUtil.dialogHeight(headerReserve + buttonReserve + diffHeight, DIALOG_MIN_HEIGHT);

        FlowLayout dialog = DialogUiUtil.dialogCard(dialogWidth, dialogHeight, DIALOG_GAP);
        dialog.child(UiFactory.title(title));
        dialog.child(UiFactory.muted(body, bodyTextWidth));
        dialog.child(UiFactory.muted(ItemEditorText.tr("dialog.apply.diff.summary", entries.size()), bodyTextWidth));

        FlowLayout lines = UiFactory.column();
        if (entries.isEmpty()) {
            lines.child(UiFactory.muted(ItemEditorText.tr("dialog.apply.diff.none"), lineTextWidth));
        } else {
            for (ItemComponentDiffUtil.Entry entry : entries) {
                lines.child(entryCard(entry, valueTextWidth));
            }
        }

        dialog.child(DialogUiUtil.scrollCard(lines, diffHeight));

        FlowLayout buttonRow = compactButtons ? UiFactory.column() : DialogUiUtil.rightAlignedButtonRow();
        buttonRow.child(DialogUiUtil.footerButtonByDivisor(
                ItemEditorText.tr("common.cancel"),
                dialogWidth,
                compactButtons,
                FOOTER_BUTTON_MIN_WIDTH,
                FOOTER_BUTTON_MAX_WIDTH,
                FOOTER_BUTTON_WIDTH_DIVISOR,
                FOOTER_BUTTON_TEXT_MIN_WIDTH,
                FOOTER_BUTTON_TEXT_RESERVE,
                button -> onCancel.run()
        ));
        buttonRow.child(DialogUiUtil.footerButtonByDivisor(
                ItemEditorText.tr("common.save_apply"),
                dialogWidth,
                compactButtons,
                FOOTER_BUTTON_MIN_WIDTH,
                FOOTER_BUTTON_MAX_WIDTH,
                FOOTER_BUTTON_WIDTH_DIVISOR,
                FOOTER_BUTTON_TEXT_MIN_WIDTH,
                FOOTER_BUTTON_TEXT_RESERVE,
                button -> onConfirm.run()
        ));
        dialog.child(buttonRow);

        overlay.child(dialog);
        return overlay;
    }

    private static FlowLayout entryCard(ItemComponentDiffUtil.Entry entry, int valueTextWidth) {
        FlowLayout card = UiFactory.subCard();
        int color = switch (entry.type()) {
            case ADDED -> COLOR_ADDED;
            case REMOVED -> COLOR_REMOVED;
            case CHANGED -> COLOR_CHANGED;
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
                    UiFactory.muted(Component.literal(safeText(entry.originalValue())), valueTextWidth)
            ));
        }
        if (entry.type() != ItemComponentDiffUtil.EntryType.REMOVED) {
            card.child(UiFactory.field(
                    ItemEditorText.tr("screen.preview"),
                    Component.empty(),
                    UiFactory.muted(Component.literal(safeText(entry.previewValue())), valueTextWidth)
            ));
        }
        return card;
    }

    private static String safeText(String value) {
        return value == null ? EMPTY_TEXT : value;
    }

}
