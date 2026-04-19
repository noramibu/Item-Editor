package me.noramibu.itemeditor.ui.component;

import io.wispforest.owo.ui.container.FlowLayout;
import net.minecraft.network.chat.Component;

public final class ConfirmationDialog {

    private static final int DIALOG_WIDTH = 400;
    private static final int DIALOG_GAP = 8;
    private static final int COMPACT_BUTTON_WIDTH_THRESHOLD = 320;
    private static final int COMPACT_BUTTON_ROWS = 2;
    private static final int BUTTON_RESERVE_COMPACT_EXTRA = 22;
    private static final int BUTTON_RESERVE_NORMAL_EXTRA = 16;
    private static final int HEADER_RESERVE_WITH_BODY = 88;
    private static final int BODY_SCROLL_HEIGHT = 150;
    private static final int BODY_SCROLL_MIN_HEIGHT = 72;
    private static final int DIALOG_MIN_HEIGHT = 140;
    private static final int BODY_TEXT_MARGIN = 32;
    private static final int BODY_TEXT_COLOR = 0xA9B5C0;
    private static final int FOOTER_BUTTON_MIN_WIDTH = 72;
    private static final int FOOTER_BUTTON_MAX_WIDTH = 140;
    private static final int FOOTER_BUTTON_WIDTH_DIVISOR = 4;
    private static final int FOOTER_BUTTON_TEXT_MIN_WIDTH = 24;
    private static final int FOOTER_BUTTON_TEXT_RESERVE = 10;

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
        int dialogWidth = DialogUiUtil.dialogWidth(DIALOG_WIDTH);
        boolean compactButtons = DialogUiUtil.compactButtons(dialogWidth, COMPACT_BUTTON_WIDTH_THRESHOLD);
        int buttonReserve = DialogUiUtil.buttonRowReserve(
                compactButtons,
                COMPACT_BUTTON_ROWS,
                BUTTON_RESERVE_COMPACT_EXTRA,
                BUTTON_RESERVE_NORMAL_EXTRA
        );
        boolean hasBody = !body.isBlank();
        int headerReserve = UiFactory.scaledPixels(HEADER_RESERVE_WITH_BODY);
        int bodyHeight = body.isBlank()
                ? 0
                : Math.min(
                DialogUiUtil.scrollHeight(BODY_SCROLL_HEIGHT),
                DialogUiUtil.availableDialogContentHeight(headerReserve + buttonReserve, BODY_SCROLL_MIN_HEIGHT)
        );
        int dialogHeight = DialogUiUtil.dialogHeight(headerReserve + buttonReserve + bodyHeight, DIALOG_MIN_HEIGHT);

        FlowLayout dialog = hasBody
                ? DialogUiUtil.dialogCard(dialogWidth, dialogHeight, DIALOG_GAP)
                : UiFactory.centeredCard(dialogWidth).gap(DIALOG_GAP);
        dialog.child(UiFactory.title(title));
        if (hasBody) {
            FlowLayout bodyContent = UiFactory.column();
            bodyContent.child(UiFactory.message(body, BODY_TEXT_COLOR).maxWidth(DialogUiUtil.dialogTextWidth(dialogWidth, BODY_TEXT_MARGIN)));
            dialog.child(DialogUiUtil.scrollCard(bodyContent, bodyHeight));
        }

        FlowLayout buttonRow = compactButtons ? UiFactory.column() : DialogUiUtil.rightAlignedButtonRow();
        buttonRow.child(DialogUiUtil.footerButtonByDivisor(
                Component.literal(cancelText),
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
                Component.literal(confirmText),
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
}
