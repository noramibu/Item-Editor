package me.noramibu.itemeditor.ui.component;

import com.mojang.blaze3d.platform.InputConstants;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.CursorStyle;
import io.wispforest.owo.ui.core.Sizing;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;

final class RawSearchWindow extends FlowLayout {
    interface ResizeHandler {
        void resize(int edges, double dx, double dy);
    }

    private final ResizeHandler resize;
    private final Runnable finished;
    private int edges;

    RawSearchWindow(ResizeHandler resize, Runnable finished) {
        super(Sizing.content(), Sizing.content(), Algorithm.VERTICAL);
        this.resize = resize;
        this.finished = finished;
    }

    @Override
    public boolean canFocus(FocusSource source) {
        return source == FocusSource.MOUSE_CLICK;
    }

    private boolean focusedOnWindow() {
        return this.focusHandler() != null && this.focusHandler().focused() == this;
    }

    @Override
    public boolean onKeyPress(KeyEvent input) {
        return (!this.focusedOnWindow() || input.isCycleFocus()) && super.onKeyPress(input);
    }

    @Override
    public boolean onCharTyped(CharacterEvent input) {
        return !this.focusedOnWindow() && super.onCharTyped(input);
    }

    private int edgesAt(double x, double y) {
        return (x < 5 ? 1 : x >= this.width() - 5 ? 2 : 0) | (y < 5 ? 4 : y >= this.height() - 5 ? 8 : 0);
    }

    @Override
    protected void parentUpdate(float delta, int mouseX, int mouseY) {
        super.parentUpdate(delta, mouseX, mouseY);
        int edges = this.edges == 0 ? this.edgesAt(mouseX - this.x(), mouseY - this.y()) : this.edges;
        this.cursorStyle(
                (edges & 3) != 0
                        ? ((edges & 12) != 0 ? CursorStyle.MOVE : CursorStyle.HORIZONTAL_RESIZE)
                        : (edges & 12) != 0 ? CursorStyle.VERTICAL_RESIZE : CursorStyle.POINTER);
    }

    @Override
    public boolean onMouseDown(MouseButtonEvent click, boolean doubled) {
        if (click.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            this.edges = this.edgesAt(click.x(), click.y());
            if (this.edges != 0) {
                if (this.focusHandler() != null) this.focusHandler().focus(this, FocusSource.MOUSE_CLICK);
                return true;
            }
        }
        return super.onMouseDown(click, doubled);
    }

    @Override
    public boolean onMouseDrag(MouseButtonEvent click, double dx, double dy) {
        if (this.edges == 0) return !this.focusedOnWindow() && super.onMouseDrag(click, dx, dy);
        this.resize.resize(this.edges, dx, dy);
        return true;
    }

    @Override
    public boolean onMouseScroll(double x, double y, double amount) {
        super.onMouseScroll(x, y, amount);
        return true;
    }

    @Override
    public boolean onMouseUp(MouseButtonEvent click) {
        if (this.edges == 0) return !this.focusedOnWindow() && super.onMouseUp(click);
        this.edges = 0;
        this.finished.run();
        return true;
    }
}
