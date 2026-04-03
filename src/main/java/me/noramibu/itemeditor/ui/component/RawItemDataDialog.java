package me.noramibu.itemeditor.ui.component;

import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Sizing;
import me.noramibu.itemeditor.util.ItemEditorText;

public final class RawItemDataDialog {

    private static final int DIALOG_WIDTH = 760;
    private static final int DATA_HEIGHT = 360;

    private RawItemDataDialog() {
    }

    public static FlowLayout create(
            String title,
            String body,
            String rawData,
            Runnable onCopy,
            Runnable onClose
    ) {
        FlowLayout overlay = DialogUiUtil.overlay();
        int dialogWidth = DialogUiUtil.dialogWidth(DIALOG_WIDTH);
        int bodyTextWidth = DialogUiUtil.dialogTextWidth(dialogWidth, 32);
        int lineTextWidth = DialogUiUtil.dialogTextWidth(dialogWidth, 48);

        FlowLayout dialog = UiFactory.centeredCard(dialogWidth).gap(8);
        dialog.child(UiFactory.title(title));
        dialog.child(UiFactory.muted(body, bodyTextWidth));

        FlowLayout lines = UiFactory.column();
        for (String line : rawData.split("\\R", -1)) {
            lines.child(UiFactory.muted(line, lineTextWidth));
        }

        dialog.child(DialogUiUtil.scrollCard(lines, DATA_HEIGHT));

        FlowLayout buttonRow = DialogUiUtil.rightAlignedButtonRow();
        buttonRow.child(UiFactory.button(ItemEditorText.tr("common.copy"), button -> onCopy.run()).horizontalSizing(Sizing.content()));
        buttonRow.child(UiFactory.button(ItemEditorText.tr("common.close"), button -> onClose.run()).horizontalSizing(Sizing.content()));

        dialog.child(buttonRow);
        overlay.child(dialog);
        return overlay;
    }
}
