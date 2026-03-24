package me.noramibu.itemeditor.ui.component;

import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.container.UIContainers;
import io.wispforest.owo.ui.core.HorizontalAlignment;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.Surface;
import io.wispforest.owo.ui.core.UIComponent;
import io.wispforest.owo.ui.core.VerticalAlignment;

final class DialogUiUtil {

    private static final int DEFAULT_SCROLLBAR_THICKNESS = 8;
    private static final int DEFAULT_SCROLL_STEP = 14;

    private DialogUiUtil() {
    }

    static FlowLayout overlay() {
        FlowLayout overlay = UIContainers.verticalFlow(Sizing.fill(), Sizing.fill());
        overlay.surface(Surface.flat(0xAA050607));
        overlay.horizontalAlignment(HorizontalAlignment.CENTER);
        overlay.verticalAlignment(VerticalAlignment.CENTER);
        overlay.padding(Insets.of(16));
        return overlay;
    }

    static FlowLayout rightAlignedButtonRow() {
        FlowLayout row = UiFactory.row();
        row.horizontalAlignment(HorizontalAlignment.RIGHT);
        return row;
    }

    static <C extends UIComponent> FlowLayout scrollCard(C content, int height) {
        ScrollContainer<C> scroll = vanillaScroll(
                UIContainers.verticalScroll(
                        Sizing.fill(100),
                        Sizing.fixed(height),
                        content
                ),
                DEFAULT_SCROLL_STEP
        );

        FlowLayout card = UiFactory.subCard();
        card.padding(Insets.of(6));
        card.child(scroll);
        return card;
    }

    static <C extends UIComponent> ScrollContainer<C> vanillaScroll(ScrollContainer<C> scroll, int step) {
        scroll.scrollbar(ScrollContainer.Scrollbar.vanillaFlat());
        scroll.scrollbarThiccness(DEFAULT_SCROLLBAR_THICKNESS);
        scroll.scrollStep(step);
        return scroll;
    }
}
