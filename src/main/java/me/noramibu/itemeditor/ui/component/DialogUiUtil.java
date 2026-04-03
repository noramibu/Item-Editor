package me.noramibu.itemeditor.ui.component;

import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.core.HorizontalAlignment;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.Surface;
import io.wispforest.owo.ui.core.Component;
import io.wispforest.owo.ui.core.VerticalAlignment;
import net.minecraft.client.Minecraft;

final class DialogUiUtil {

    private static final int OVERLAY_PADDING = 16;
    private static final int DEFAULT_SCROLLBAR_THICKNESS = 8;
    private static final int DEFAULT_SCROLL_STEP = 14;
    private static final int MIN_DIALOG_WIDTH = 220;
    private static final int MIN_SCROLL_HEIGHT = 96;
    private static final int SCROLL_RESERVED_HEIGHT = 220;

    private DialogUiUtil() {
    }

    static FlowLayout overlay() {
        FlowLayout overlay = Containers.verticalFlow(Sizing.fill(), Sizing.fill());
        overlay.surface(Surface.flat(0xAA050607));
        overlay.horizontalAlignment(HorizontalAlignment.CENTER);
        overlay.verticalAlignment(VerticalAlignment.CENTER);
        overlay.padding(Insets.of(OVERLAY_PADDING));
        return overlay;
    }

    static FlowLayout rightAlignedButtonRow() {
        FlowLayout row = UiFactory.row();
        row.horizontalAlignment(HorizontalAlignment.RIGHT);
        return row;
    }

    static <C extends Component> FlowLayout scrollCard(C content, int height) {
        ScrollContainer<C> scroll = vanillaScroll(
                Containers.verticalScroll(
                        Sizing.fill(100),
                        Sizing.fixed(scrollHeight(height)),
                        content
                ),
                DEFAULT_SCROLL_STEP
        );

        FlowLayout card = UiFactory.subCard();
        card.padding(Insets.of(6));
        card.child(scroll);
        return card;
    }

    static <C extends Component> ScrollContainer<C> vanillaScroll(ScrollContainer<C> scroll, int step) {
        scroll.scrollbar(ScrollContainer.Scrollbar.vanillaFlat());
        scroll.scrollbarThiccness(DEFAULT_SCROLLBAR_THICKNESS);
        scroll.scrollStep(step);
        return scroll;
    }

    static int dialogWidth(int preferredWidth) {
        int available = Math.max(MIN_DIALOG_WIDTH, guiWidth() - (OVERLAY_PADDING * 2));
        return Math.max(MIN_DIALOG_WIDTH, Math.min(preferredWidth, available));
    }

    static int dialogTextWidth(int dialogWidth, int horizontalPadding) {
        return Math.max(80, dialogWidth - horizontalPadding);
    }

    static int scrollHeight(int preferredHeight) {
        int available = Math.max(MIN_SCROLL_HEIGHT, guiHeight() - SCROLL_RESERVED_HEIGHT);
        return Math.max(MIN_SCROLL_HEIGHT, Math.min(preferredHeight, available));
    }

    private static int guiWidth() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft != null ? minecraft.getWindow().getGuiScaledWidth() : 854;
    }

    private static int guiHeight() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft != null ? minecraft.getWindow().getGuiScaledHeight() : 480;
    }
}
