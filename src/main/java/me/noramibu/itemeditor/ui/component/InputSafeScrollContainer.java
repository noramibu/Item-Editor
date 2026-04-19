package me.noramibu.itemeditor.ui.component;

import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.UIComponent;
import io.wispforest.owo.ui.inject.GreedyInputUIComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.MouseButtonEvent;

public class InputSafeScrollContainer<C extends UIComponent> extends ScrollContainer<C> implements GreedyInputUIComponent {

    private static final double CONTINUOUS_SCROLL_MULTIPLIER = 15.0d;
    private static final double SCROLL_EPSILON = 1.0e-6d;
    private boolean consumeScrollWhenHovered;

    protected InputSafeScrollContainer(ScrollDirection direction, Sizing horizontalSizing, Sizing verticalSizing, C child) {
        super(direction, horizontalSizing, verticalSizing, child);
    }

    public static <C extends UIComponent> InputSafeScrollContainer<C> vertical(Sizing horizontalSizing, Sizing verticalSizing, C child) {
        return new InputSafeScrollContainer<>(ScrollDirection.VERTICAL, horizontalSizing, verticalSizing, child);
    }

    public static <C extends UIComponent> InputSafeScrollContainer<C> horizontal(Sizing horizontalSizing, Sizing verticalSizing, C child) {
        return new InputSafeScrollContainer<>(ScrollDirection.HORIZONTAL, horizontalSizing, verticalSizing, child);
    }

    public InputSafeScrollContainer<C> consumeScrollWhenHovered(boolean value) {
        this.consumeScrollWhenHovered = value;
        return this;
    }

    @Override
    public boolean onMouseDown(MouseButtonEvent click, boolean doubled) {
        boolean overScrollbar = this.isInScrollbar(this.currentGuiMouseX(), this.currentGuiMouseY());
        this.scrollbaring = overScrollbar;
        if (overScrollbar) {
            super.onMouseDown(click, doubled);
            return true;
        }
        return super.onMouseDown(click, doubled);
    }

    @Override
    public boolean onMouseScroll(double mouseX, double mouseY, double amount) {
        double eventX = this.x + mouseX;
        double eventY = this.y + mouseY;
        boolean hovered = this.isInBoundingBox(eventX, eventY);
        if (this.isInScrollbar(eventX, eventY)) {
            boolean scrolled = this.scrollSelf(amount);
            return scrolled || (hovered && this.consumeScrollWhenHovered);
        }

        if (hovered && this.child.onMouseScroll(eventX - this.child.x(), eventY - this.child.y(), amount)) {
            return true;
        }

        if (!hovered) {
            return false;
        }

        boolean scrolled = this.scrollSelf(amount);
        return scrolled || this.consumeScrollWhenHovered;
    }

    private boolean scrollSelf(double amount) {
        double before = this.scrollOffset;
        if (this.scrollStep < 1) {
            this.scrollBy(-amount * CONTINUOUS_SCROLL_MULTIPLIER, false, true);
        } else {
            this.scrollBy(-amount * this.scrollStep, true, true);
        }
        return Math.abs(this.scrollOffset - before) > SCROLL_EPSILON;
    }

    private double currentGuiMouseX() {
        Minecraft minecraft = Minecraft.getInstance();
        double scaleX = (double) minecraft.getWindow().getGuiScaledWidth() / (double) minecraft.getWindow().getScreenWidth();
        return minecraft.mouseHandler.xpos() * scaleX;
    }

    private double currentGuiMouseY() {
        Minecraft minecraft = Minecraft.getInstance();
        double scaleY = (double) minecraft.getWindow().getGuiScaledHeight() / (double) minecraft.getWindow().getScreenHeight();
        return minecraft.mouseHandler.ypos() * scaleY;
    }
}
