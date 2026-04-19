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

    static FlowLayout rightAlignedButtonRow() {
        FlowLayout row = UiFactory.row();
        row.horizontalAlignment(HorizontalAlignment.RIGHT);
        return row;
    }

    static FlowLayout dialogCard(int dialogWidth, int dialogHeight, int gap) {
        FlowLayout dialog = UiFactory.centeredCard(dialogWidth).gap(gap);
        dialog.verticalSizing(Sizing.fixed(dialogHeight));
        return dialog;
    }

    static <C extends UIComponent> FlowLayout scrollCard(C content, int height) {
        InputSafeScrollContainer<C> modalScroll = InputSafeScrollContainer.vertical(
                Sizing.fill(100),
                Sizing.fixed(scrollHeight(height)),
                content
        ).consumeScrollWhenHovered(true);
        ScrollContainer<C> scroll = vanillaScroll(
                modalScroll,
                DEFAULT_SCROLL_STEP
        );

        FlowLayout card = UiFactory.subCard();
        card.padding(Insets.of(CARD_PADDING));
        card.child(scroll);
        return card;
    }

    static <C extends UIComponent> ScrollContainer<C> vanillaScroll(ScrollContainer<C> scroll, int step) {
        scroll.scrollbar(ScrollContainer.Scrollbar.vanillaFlat());
        scroll.scrollbarThiccness(UiFactory.scaledScrollbarThickness(DEFAULT_SCROLLBAR_THICKNESS));
        scroll.scrollStep(UiFactory.scaledScrollStep(step));
        return scroll;
    }

    static int dialogWidth(int preferredWidth) {
        int available = Math.max(VIEWPORT_MIN, availableViewportWidth());
        int minimum = Math.min(Math.max(MIN_DIALOG_WIDTH, VIEWPORT_MIN), available);
        return Math.max(minimum, Math.min(preferredWidth, available));
    }

    static int dialogHeight(int preferredHeight, int minHeight) {
        int available = Math.max(VIEWPORT_MIN, availableViewportHeight());
        int minimum = Math.min(Math.max(MIN_DIALOG_HEIGHT, minHeight), available);
        return Math.max(minimum, Math.min(preferredHeight, available));
    }

    static int availableDialogContentHeight(int reservedHeight, int minHeight) {
        int viewport = Math.max(VIEWPORT_MIN, availableViewportHeight());
        int reserved = Math.max(0, reservedHeight);
        int available = Math.max(VIEWPORT_MIN, viewport - reserved);
        int hardCap = Math.max(
                VIEWPORT_MIN,
                viewport - Math.max(DIALOG_HARDCAP_MIN_RESERVE, UiFactory.scaledPixels(DIALOG_HARDCAP_SCALED_RESERVE))
        );
        return Math.min(available, hardCap);
    }

    static int dialogTextWidth(int dialogWidth, int horizontalPadding) {
        int available = Math.max(VIEWPORT_MIN, dialogWidth - horizontalPadding);
        int preferred = Math.max(MIN_TEXT_WIDTH, available);
        return Math.max(VIEWPORT_MIN, Math.min(dialogWidth, preferred));
    }

    static int scrollHeight(int preferredHeight) {
        int reserved = Math.max(MIN_SCROLL_RESERVED_HEIGHT, (int) Math.round(viewportHeight() * SCROLL_RESERVED_HEIGHT_RATIO));
        int available = Math.max(VIEWPORT_MIN, availableViewportHeight() - reserved);
        int minimum = Math.min(MIN_SCROLL_HEIGHT, available);
        return Math.max(minimum, Math.min(preferredHeight, available));
    }

    static boolean compactButtons(int dialogWidth, int widthThreshold) {
        return dialogWidth < widthThreshold || guiScale() >= COMPACT_BUTTON_SCALE_THRESHOLD;
    }

    static int buttonRowReserve(boolean compactButtons, int compactRows, int compactExtra, int regularExtra) {
        int controlHeight = UiFactory.scaleProfile().controlHeight();
        if (compactButtons) {
            return (controlHeight * Math.max(1, compactRows)) + UiFactory.scaledPixels(compactExtra);
        }
        return controlHeight + UiFactory.scaledPixels(regularExtra);
    }

    static ButtonComponent footerButtonByDivisor(
            Component fullText,
            int dialogWidth,
            boolean compactButtons,
            int minWidth,
            int maxWidth,
            int widthDivisor,
            int textMinWidth,
            int textReserve,
            Consumer<ButtonComponent> onPress
    ) {
        ButtonComponent button = baseFooterButton(fullText, compactButtons, onPress);
        if (compactButtons) return button;
        int buttonWidth = clampFooterButtonWidth(minWidth, maxWidth, dialogWidth / Math.max(1, widthDivisor));
        return configureFooterButton(button, fullText, buttonWidth, textMinWidth, textReserve);
    }

    static ButtonComponent footerButtonByCount(
            Component fullText,
            int dialogWidth,
            boolean compactButtons,
            int minWidth,
            int maxWidth,
            int buttonCount,
            int rowReserve,
            int textMinWidth,
            int textReserve,
            Consumer<ButtonComponent> onPress
    ) {
        ButtonComponent button = baseFooterButton(fullText, compactButtons, onPress);
        if (compactButtons) return button;
        int buttonWidth = clampFooterButtonWidth(
                minWidth,
                maxWidth,
                (dialogWidth - UiFactory.scaledPixels(rowReserve)) / Math.max(1, buttonCount)
        );
        return configureFooterButton(button, fullText, buttonWidth, textMinWidth, textReserve);
    }

    private static ButtonComponent baseFooterButton(
            Component fullText,
            boolean compactButtons,
            Consumer<ButtonComponent> onPress
    ) {
        ButtonComponent button = UiFactory.button(fullText, UiFactory.ButtonTextPreset.STANDARD, onPress);
        if (compactButtons) {
            button.horizontalSizing(Sizing.fill(100));
        }
        return button;
    }

    private static ButtonComponent configureFooterButton(
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
        return button;
    }

    private static int clampFooterButtonWidth(int minWidth, int maxWidth, int candidateWidth) {
        return Math.max(minWidth, Math.min(maxWidth, candidateWidth));
    }

    private static int overlayPadding() {
        int computed = (int) Math.round(Math.min(guiWidth(), guiHeight()) * OVERLAY_PADDING_RATIO);
        return Math.max(MIN_OVERLAY_PADDING, Math.min(MAX_OVERLAY_PADDING, computed));
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

    private static double guiScale() {
        return minecraft().getWindow().getGuiScale();
    }

    private static int viewportWidth() {
        return Math.max(VIEWPORT_MIN, guiWidth());
    }

    private static int viewportHeight() {
        return Math.max(VIEWPORT_MIN, guiHeight());
    }

    private static int availableViewportWidth() {
        return Math.max(VIEWPORT_MIN, viewportWidth() - overlayInset());
    }

    private static int availableViewportHeight() {
        return Math.max(VIEWPORT_MIN, viewportHeight() - overlayInset());
    }

    private static Minecraft minecraft() {
        return Minecraft.getInstance();
    }
}
