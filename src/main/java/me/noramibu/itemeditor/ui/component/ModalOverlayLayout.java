package me.noramibu.itemeditor.ui.component;

import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.inject.GreedyInputUIComponent;
import net.minecraft.client.input.MouseButtonEvent;

final class ModalOverlayLayout extends FlowLayout implements GreedyInputUIComponent {

    ModalOverlayLayout() {
        super(Sizing.fill(100), Sizing.fill(100), FlowLayout.Algorithm.VERTICAL);
    }

    @Override
    public boolean onMouseDown(MouseButtonEvent click, boolean doubled) {
        boolean handled = super.onMouseDown(click, doubled);
        return handled || this.containsPointer(click.x(), click.y());
    }

    @Override
    public boolean onMouseUp(MouseButtonEvent click) {
        boolean handled = super.onMouseUp(click);
        return handled || this.containsPointer(click.x(), click.y());
    }

    @Override
    public boolean onMouseDrag(MouseButtonEvent click, double deltaX, double deltaY) {
        boolean handled = super.onMouseDrag(click, deltaX, deltaY);
        return handled || this.containsPointer(click.x(), click.y());
    }

    @Override
    public boolean onMouseScroll(double mouseX, double mouseY, double amount) {
        boolean handled = super.onMouseScroll(mouseX, mouseY, amount);
        return handled || this.containsPointer(mouseX, mouseY);
    }

    private boolean containsPointer(double eventX, double eventY) {
        if (this.isInBoundingBox(eventX, eventY)) {
            return true;
        }
        return this.isInBoundingBox(this.x() + eventX, this.y() + eventY);
    }
}
