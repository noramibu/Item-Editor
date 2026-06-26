package me.noramibu.itemeditor.ui.component;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.core.HorizontalAlignment;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.Surface;
import io.wispforest.owo.ui.core.UIComponent;
import io.wispforest.owo.ui.core.VerticalAlignment;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Consumer;

final class DialogUiUtil {

    private static final int DEFAULT_SCROLLBAR_THICKNESS = 8;
    private static final int DEFAULT_SCROLL_STEP = 14;
    private static final int MIN_DIALOG_WIDTH = 220;
    private static final int MIN_DIALOG_HEIGHT = 64;
    private static final int MIN_SCROLL_HEIGHT = 96;
    private static final int MIN_TEXT_WIDTH = 80;
    private static final int CARD_PADDING = 6;
    private static final int OVERLAY_DOUBLE_PADDING = 2;
    private static final int DIALOG_HARDCAP_MIN_RESERVE = 48;
    private static final int DIALOG_HARDCAP_SCALED_RESERVE = 44;
    private static final double SCROLL_RESERVED_HEIGHT_RATIO = 0.28d;
    private static final double OVERLAY_PADDING_RATIO = 0.02d;
    private static final int MIN_OVERLAY_PADDING = 6;
    private static final int MAX_OVERLAY_PADDING = 16;
    private static final int MIN_SCROLL_RESERVED_HEIGHT = 132;
    private static final double COMPACT_BUTTON_SCALE_THRESHOLD = 4.0d;
    private static final int VIEWPORT_MIN = 1;
    private static final int FOOTER_BUTTON_HEIGHT = 18;
    private static final int FOOTER_COMPACT_BUTTON_HEIGHT = 16;
    private static final int FOOTER_BUTTON_COLUMNS_MAX = 3;

    private DialogUiUtil() {
    }

    static FlowLayout overlay() {
        FlowLayout overlay = new ModalOverlayLayout();
        overlay.gap(0);
        overlay.horizontalSizing(Sizing.fill(100));
        overlay.verticalSizing(Sizing.fill(100));
        overlay.surface(Surface.flat(0xAA050607));
        overlay.horizontalAlignment(HorizontalAlignment.CENTER);
        overlay.verticalAlignment(VerticalAlignment.CENTER);
        overlay.padding(Insets.of(overlayPadding()));
        return overlay;
    }

    static FlowLayout dialogCard(int dialogWidth, int dialogHeight, int gap) {
        FlowLayout dialog = UiFactory.centeredCard(dialogWidth).gap(gap);
        dialog.verticalSizing(Sizing.fixed(dialogHeight));
        return dialog;
    }

    static <C extends UIComponent> FlowLayout scrollCard(C content, int height) {
        FlowLayout card = UiFactory.subCard();
        card.padding(Insets.of(CARD_PADDING));
        card.child(scrollContent(content, height));
        return card;
    }

    static <C extends UIComponent> ScrollContainer<C> scrollContent(C content, int height) {
        InputSafeScrollContainer<C> modalScroll = InputSafeScrollContainer.vertical(
                Sizing.fill(100),
                Sizing.fixed(scrollHeight(height)),
                content
        ).consumeScrollWhenHovered(true);
        return vanillaScroll(modalScroll, DEFAULT_SCROLL_STEP);
    }

    static <C extends UIComponent> ScrollContainer<C> scrollContentExpand(C content) {
        InputSafeScrollContainer<C> modalScroll = InputSafeScrollContainer.vertical(
                Sizing.fill(100),
                Sizing.expand(100),
                content
        ).consumeScrollWhenHovered(true);
        return vanillaScroll(modalScroll, DEFAULT_SCROLL_STEP);
    }

    static <C extends UIComponent> ScrollContainer<C> vanillaScroll(ScrollContainer<C> scroll, int step) {
        scroll.scrollbar(ScrollContainer.Scrollbar.vanillaFlat());
        scroll.scrollbarThiccness(UiFactory.scaledScrollbarThickness(DEFAULT_SCROLLBAR_THICKNESS));
        scroll.scrollStep(UiFactory.scaledScrollStep(step));
        return scroll;
    }

    static int dialogWidth(int preferredWidth) {
        int available = Math.max(VIEWPORT_MIN, Math.max(VIEWPORT_MIN, guiWidth()) - overlayInset());
        int minimum = Math.clamp(MIN_DIALOG_WIDTH, VIEWPORT_MIN, available);
        return Math.clamp(preferredWidth, minimum, available);
    }

    static int dialogHeight(int preferredHeight, int minHeight) {
        int available = Math.max(VIEWPORT_MIN, availableViewportHeight());
        int minimum = Math.clamp(MIN_DIALOG_HEIGHT, minHeight, available);
        return Math.clamp(preferredHeight, minimum, available);
    }

    static int availableDialogContentHeight(int reservedHeight, int minHeight) {
        int viewport = Math.max(VIEWPORT_MIN, availableViewportHeight());
        int reserved = Math.max(0, reservedHeight);
        int available = Math.max(VIEWPORT_MIN, viewport - reserved);
        int hardCap = Math.max(
                VIEWPORT_MIN,
                viewport - Math.max(DIALOG_HARDCAP_MIN_RESERVE, UiFactory.scaledPixels(DIALOG_HARDCAP_SCALED_RESERVE))
        );
        int minimum = Math.clamp(VIEWPORT_MIN, minHeight, hardCap);
        return Math.clamp(available, minimum, hardCap);
    }

    static int dialogTextWidth(int dialogWidth, int horizontalPadding) {
        int available = Math.max(VIEWPORT_MIN, dialogWidth - horizontalPadding);
        int preferred = Math.max(MIN_TEXT_WIDTH, available);
        return Math.clamp(preferred, VIEWPORT_MIN, Math.max(VIEWPORT_MIN, dialogWidth));
    }

    static int scrollHeight(int preferredHeight) {
        int reserved = Math.max(MIN_SCROLL_RESERVED_HEIGHT, (int) Math.round(viewportHeight() * SCROLL_RESERVED_HEIGHT_RATIO));
        int available = Math.max(VIEWPORT_MIN, availableViewportHeight() - reserved);
        int minimum = Math.min(MIN_SCROLL_HEIGHT, available);
        return Math.clamp(preferredHeight, minimum, available);
    }

    static boolean compactButtons(int dialogWidth, int widthThreshold) {
        return dialogWidth < widthThreshold || minecraft().getWindow().getGuiScale() >= COMPACT_BUTTON_SCALE_THRESHOLD;
    }

    static int buttonRowReserve(boolean compactButtons, int compactRows, int compactExtra, int regularExtra) {
        int controlHeight = footerButtonHeight(compactButtons);
        if (compactButtons) {
            int rows = Math.max(1, compactRows);
            int gaps = footerGap() * Math.max(0, rows - 1);
            return (controlHeight * rows) + gaps + UiFactory.scaledPixels(compactExtra);
        }
        return controlHeight + UiFactory.scaledPixels(regularExtra);
    }

    static int compactFooterRowCount(int dialogWidth, int minWidth, int actionCount) {
        int columns = compactFooterColumns(dialogWidth, minWidth, actionCount);
        return Math.max(1, (Math.max(1, actionCount) + columns - 1) / columns);
    }

    static FlowLayout footerRowByDivisor(
            int dialogWidth,
            boolean compactButtons,
            int minWidth,
            int maxWidth,
            int widthDivisor,
            int textMinWidth,
            int textReserve,
            FooterAction... actions
    ) {
        int buttonWidth = compactButtons
                ? 0
                : clampFooterButtonWidth(minWidth, maxWidth, dialogWidth / Math.max(1, widthDivisor));
        if (compactButtons) {
            return compactFooterRows(dialogWidth, minWidth, maxWidth, textMinWidth, textReserve, actions);
        }
        return footerRow(buttonWidth, textMinWidth, textReserve, actions);
    }

    static FlowLayout footerRowByCount(
            int dialogWidth,
            boolean compactButtons,
            int minWidth,
            int maxWidth,
            int buttonCount,
            int rowReserve,
            int textMinWidth,
            int textReserve,
            FooterAction... actions
    ) {
        int buttonWidth = compactButtons
                ? 0
                : clampFooterButtonWidth(
                minWidth,
                maxWidth,
                (dialogWidth - UiFactory.scaledPixels(rowReserve)) / Math.max(1, buttonCount)
        );
        if (compactButtons) {
            return compactFooterRows(dialogWidth, minWidth, maxWidth, textMinWidth, textReserve, actions);
        }
        return footerRow(buttonWidth, textMinWidth, textReserve, actions);
    }

    private static FlowLayout footerRow(
            int buttonWidth,
            int textMinWidth,
            int textReserve,
            FooterAction... actions
    ) {
        FlowLayout row = footerActionRow();
        row.horizontalAlignment(HorizontalAlignment.RIGHT);
        for (FooterAction action : actions) {
            ButtonComponent button = baseFooterButton(action.fullText(), false, action.onPress());
            configureFooterButton(button, action.fullText(), buttonWidth, textMinWidth, textReserve);
            row.child(button);
        }
        return row;
    }

    private static FlowLayout compactFooterRows(
            int dialogWidth,
            int minWidth,
            int maxWidth,
            int textMinWidth,
            int textReserve,
            FooterAction... actions
    ) {
        FlowLayout column = UiFactory.column();
        column.gap(footerGap());
        int columns = compactFooterColumns(dialogWidth, minWidth, actions.length);
        int gap = footerGap();
        int availableWidth = Math.max(VIEWPORT_MIN, dialogWidth - UiFactory.scaledPixels(CARD_PADDING * 2));
        int rawButtonWidth = Math.max(VIEWPORT_MIN, (availableWidth - gap * Math.max(0, columns - 1)) / columns);
        int buttonWidth = columns <= 1
                ? rawButtonWidth
                : clampFooterButtonWidth(minWidth, maxWidth, rawButtonWidth);

        FlowLayout row = footerActionRow();
        int rowItems = 0;
        for (FooterAction action : actions) {
            if (rowItems >= columns) {
                column.child(row);
                row = footerActionRow();
                rowItems = 0;
            }

            ButtonComponent button = baseFooterButton(action.fullText(), true, action.onPress());
            configureFooterButton(button, action.fullText(), buttonWidth, textMinWidth, textReserve);
            if (columns <= 1) {
                button.horizontalSizing(Sizing.fill(100));
            }
            row.child(button);
            rowItems++;
        }
        if (!row.children().isEmpty()) {
            column.child(row);
        }
        return column;
    }

    static ScrollDialogSizing scrollDialogSizing(
            int preferredScrollHeight,
            int reservedHeight,
            int minContentHeight,
            int minDialogHeight
    ) {
        int contentHeight = Math.min(
                scrollHeight(preferredScrollHeight),
                availableDialogContentHeight(reservedHeight, minContentHeight)
        );
        int dialogHeight = dialogHeight(reservedHeight + contentHeight, minDialogHeight);
        return new ScrollDialogSizing(contentHeight, dialogHeight);
    }

    private static ButtonComponent baseFooterButton(
            Component fullText,
            boolean compactButtons,
            Consumer<ButtonComponent> onPress
    ) {
        ButtonComponent button = UiFactory.button(
                fullText,
                compactButtons ? UiFactory.ButtonTextPreset.COMPACT : UiFactory.ButtonTextPreset.STANDARD,
                onPress
        );
        button.verticalSizing(Sizing.fixed(footerButtonHeight(compactButtons)));
        if (compactButtons) {
            button.horizontalSizing(Sizing.fill(100));
        }
        return button;
    }

    private static void configureFooterButton(
            ButtonComponent button,
            Component fullText,
            int buttonWidth,
            int textMinWidth,
            int textReserve
    ) {
        button.setMessage(UiFactory.fitToWidth(
                fullText,
                Math.max(textMinWidth, buttonWidth - UiFactory.scaledPixels(textReserve))
        ));
        button.tooltip(List.of(fullText));
        button.horizontalSizing(Sizing.fixed(buttonWidth));
    }

    private static int clampFooterButtonWidth(int minWidth, int maxWidth, int candidateWidth) {
        return Math.clamp(candidateWidth, minWidth, maxWidth);
    }

    private static FlowLayout footerActionRow() {
        FlowLayout row = UiFactory.row();
        row.gap(footerGap());
        return row;
    }

    private static int compactFooterColumns(int dialogWidth, int minWidth, int actionCount) {
        if (actionCount <= 1) {
            return 1;
        }
        int gap = footerGap();
        int availableWidth = Math.max(VIEWPORT_MIN, dialogWidth - UiFactory.scaledPixels(CARD_PADDING * 2));
        int minimumButtonWidth = Math.max(1, minWidth);
        int maxColumnsByWidth = Math.max(1, (availableWidth + gap) / (minimumButtonWidth + gap));
        return Math.min(Math.min(FOOTER_BUTTON_COLUMNS_MAX, actionCount), maxColumnsByWidth);
    }

    private static int footerButtonHeight(boolean compactButtons) {
        if (compactButtons) {
            return Math.max(14, UiFactory.scaledPixels(FOOTER_COMPACT_BUTTON_HEIGHT));
        }
        return Math.max(UiFactory.scaleProfile().controlHeight(), UiFactory.scaledPixels(FOOTER_BUTTON_HEIGHT));
    }

    private static int footerGap() {
        return Math.max(1, UiFactory.scaleProfile().tightSpacing());
    }

    private static int overlayPadding() {
        int computed = (int) Math.round(Math.min(guiWidth(), guiHeight()) * OVERLAY_PADDING_RATIO);
        return Math.clamp(computed, MIN_OVERLAY_PADDING, MAX_OVERLAY_PADDING);
    }

    private static int overlayInset() {
        return overlayPadding() * OVERLAY_DOUBLE_PADDING;
    }

    private static int guiWidth() {
        return minecraft().getWindow().getGuiScaledWidth();
    }

    private static int guiHeight() {
        return minecraft().getWindow().getGuiScaledHeight();
    }

    private static int viewportHeight() {
        return Math.max(VIEWPORT_MIN, guiHeight());
    }

    private static int availableViewportHeight() {
        return Math.max(VIEWPORT_MIN, viewportHeight() - overlayInset());
    }

    private static Minecraft minecraft() {
        return Minecraft.getInstance();
    }

    record FooterAction(Component fullText, Consumer<ButtonComponent> onPress) {
    }

    record ScrollDialogSizing(int contentHeight, int dialogHeight) {
    }
}
