package me.noramibu.itemeditor.ui.component;

import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Sizing;
import net.minecraft.network.chat.Component;

public final class ConfirmationDialog {

    private static final int DIALOG_WIDTH = 400;

    private ConfirmationDialog() {
    }

    public static FlowLayout create(
            String title,
            String body,
            String confirmText,
            Runnable onConfirm,
            String cancelText,
            Runnable onCancel
    ) {
        FlowLayout overlay = DialogUiUtil.overlay();

        FlowLayout dialog = UiFactory.centeredCard(DIALOG_WIDTH).gap(8);
        dialog.child(UiFactory.title(title));
        if (!body.isBlank()) {
            dialog.child(UiFactory.message(body, 0xA9B5C0).maxWidth(DIALOG_WIDTH - 32));
        }

        FlowLayout buttonRow = DialogUiUtil.rightAlignedButtonRow();
        buttonRow.child(UiFactory.button(Component.literal(cancelText), button -> onCancel.run()).horizontalSizing(Sizing.content()));
        buttonRow.child(UiFactory.button(Component.literal(confirmText), button -> onConfirm.run()).horizontalSizing(Sizing.content()));

        dialog.child(buttonRow);
        overlay.child(dialog);
        return overlay;
    }
}
