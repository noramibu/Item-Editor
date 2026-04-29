package me.noramibu.itemeditor.ui.component;

import io.wispforest.owo.ui.base.BaseUIComponent;
import io.wispforest.owo.ui.core.OwoUIGraphics;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.UIComponent;
import io.wispforest.owo.ui.inject.GreedyInputUIComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.MouseButtonEvent;
import org.lwjgl.glfw.GLFW;

import java.util.Objects;

public final class RichTextHorizontalScrollbarComponent extends BaseUIComponent implements GreedyInputUIComponent {
    private static final int HEIGHT = 8;
    private static final int TRACK_HEIGHT = 5;
    private static final int TRACK_LEFT_PADDING = 3;
    private static final int TRACK_RIGHT_PADDING = 20;
    private static final int MIN_THUMB_WIDTH = 16;
    private static final int TRACK_COLOR = 0x55202A38;
    private static final int THUMB_COLOR = 0xAA9AA6B5;

    private final RichTextAreaComponent editor;
    private boolean dragging;
    private int dragOffset;

    public RichTextHorizontalScrollbarComponent(Sizing horizontalSizing, RichTextAreaComponent editor) {
        this.editor = Objects.requireNonNull(editor, "editor");
        this.horizontalSizing(horizontalSizing);
        this.verticalSizing(Sizing.fixed(HEIGHT));
    }

    @Override
    protected int determineHorizontalContentSize(Sizing sizing) {
        return 80;
    }

    @Override
    protected int determineVerticalContentSize(Sizing sizing) {
        return HEIGHT;
    }

    @Override
    public boolean canFocus(UIComponent.FocusSource source) {
        return true;
    }

    @Override
    public void draw(OwoUIGraphics context, int mouseX, int mouseY, float partialTicks, float delta) {
        if (this.editor.horizontalScrollMaximum() <= 0) {
            return;
        }

        int left = this.trackLeft();
        int right = this.trackRight();
        int top = this.trackTop();
        int bottom = top + TRACK_HEIGHT;
        if (right <= left || bottom <= top) {
            return;
        }

        context.fill(left, top, right, bottom, TRACK_COLOR);
        int thumbLeft = this.currentThumbLeft(left, right);
        int thumbRight = Math.min(right, thumbLeft + this.currentThumbWidth(left, right));
        context.fill(thumbLeft, top, thumbRight, bottom, THUMB_COLOR);
    }

    @Override
    public boolean onMouseDown(MouseButtonEvent click, boolean doubled) {
        return this.handleMouseDown(click.x(), click.y(), click.button())
                || super.onMouseDown(click, doubled);
    }

    @Override
    public boolean onMouseDrag(MouseButtonEvent click, double deltaX, double deltaY) {
        return this.handleMouseDrag(click.button())
                || super.onMouseDrag(click, deltaX, deltaY);
    }

    @Override
    public boolean onMouseUp(MouseButtonEvent click) {
        return this.handleMouseUp(click.button())
                || super.onMouseUp(click);
    }

    @Override
    public boolean onMouseScroll(double mouseX, double mouseY, double amount) {
        if (this.eventOutside(mouseX, mouseY) || this.editor.horizontalScrollMaximum() <= 0 || amount == 0d) {
            return super.onMouseScroll(mouseX, mouseY, amount);
        }

        double step = Math.max(8d, this.editor.horizontalScrollViewportWidth() / 8d);
        this.editor.setHorizontalScrollAmount(this.editor.horizontalScrollAmount() - amount * step);
        return true;
    }

    @Override
    public void onFocusLost() {
        this.dragging = false;
        super.onFocusLost();
    }

    private boolean handleMouseDown(double mouseX, double mouseY, int button) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT
                || this.eventOutside(mouseX, mouseY)
                || this.editor.horizontalScrollMaximum() <= 0) {
            return false;
        }

        this.dragging = true;
        this.startDrag(this.screenMouseX(mouseX, mouseY));
        return true;
    }

    private boolean handleMouseDrag(int button) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT || !this.dragging) {
            return false;
        }

        this.updateFromMouse(this.currentGuiMouseX());
        return true;
    }

    private boolean handleMouseUp(int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && this.dragging) {
            this.dragging = false;
            return true;
        }
        return false;
    }

    private void startDrag(double mouseX) {
        int left = this.trackLeft();
        int right = this.trackRight();
        int thumbLeft = this.currentThumbLeft(left, right);
        int thumbRight = thumbLeft + this.currentThumbWidth(left, right);
        int mouse = (int) Math.floor(mouseX);
        if (mouse >= thumbLeft && mouse <= thumbRight) {
            this.dragOffset = mouse - thumbLeft;
            return;
        }

        this.dragOffset = this.currentThumbWidth(left, right) / 2;
        this.updateFromMouse(mouseX);
    }

    private void updateFromMouse(double mouseX) {
        int max = this.editor.horizontalScrollMaximum();
        if (max <= 0) {
            this.editor.setHorizontalScrollAmount(0);
            return;
        }

        int left = this.trackLeft();
        int right = this.trackRight();
        int thumbWidth = this.currentThumbWidth(left, right);
        int travel = Math.max(1, (right - left) - thumbWidth);
        int thumbLeft = Math.clamp((int) Math.floor(mouseX) - this.dragOffset, left, left + travel);
        double ratio = (double) (thumbLeft - left) / (double) travel;
        this.editor.setHorizontalScrollAmount(ratio * max);
    }

    private int currentThumbLeft(int left, int right) {
        int max = this.editor.horizontalScrollMaximum();
        if (max <= 0) {
            return left;
        }

        int thumbWidth = this.currentThumbWidth(left, right);
        int travel = Math.max(1, (right - left) - thumbWidth);
        return left + (int) Math.round((this.editor.horizontalScrollAmount() / max) * travel);
    }

    private int currentThumbWidth(int left, int right) {
        int trackWidth = Math.max(1, right - left);
        int viewportWidth = Math.max(1, this.editor.horizontalScrollViewportWidth());
        int contentWidth = Math.max(viewportWidth, this.editor.horizontalScrollContentWidth());
        int thumbWidth = (int) Math.round((viewportWidth / (double) contentWidth) * trackWidth);
        return Math.clamp(thumbWidth, Math.min(trackWidth, MIN_THUMB_WIDTH), trackWidth);
    }

    private int trackLeft() {
        return this.x() + this.trackLeftPadding();
    }

    private int trackRight() {
        return this.x() + this.width() - this.trackRightPadding();
    }

    private int trackTop() {
        return this.y() + Math.max(1, (this.height() - TRACK_HEIGHT) / 2);
    }

    private boolean eventOutside(double eventX, double eventY) {
        return !this.isInBoundingBox(eventX, eventY)
                && !this.isInBoundingBox(this.x() + eventX, this.y() + eventY);
    }

    private double screenMouseX(double eventX, double eventY) {
        return this.isInBoundingBox(eventX, eventY) ? eventX : this.x() + eventX;
    }

    private double currentGuiMouseX() {
        Minecraft minecraft = Minecraft.getInstance();
        double scaleX = (double) minecraft.getWindow().getGuiScaledWidth() / (double) minecraft.getWindow().getScreenWidth();
        return minecraft.mouseHandler.xpos() * scaleX;
    }

    private int trackLeftPadding() {
        return UiFactory.scaledPixels(TRACK_LEFT_PADDING);
    }

    private int trackRightPadding() {
        return UiFactory.scaledPixels(TRACK_RIGHT_PADDING);
    }
}
