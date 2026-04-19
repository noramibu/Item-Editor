package me.noramibu.itemeditor.ui.component;

import io.wispforest.owo.ui.component.ItemComponent;
import io.wispforest.owo.ui.core.OwoUIGraphics;
import io.wispforest.owo.ui.core.Sizing;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3x2fStack;
import org.lwjgl.glfw.GLFW;

public final class RotatableItemPreviewComponent extends ItemComponent {

    private float rotationDegrees = -20.0F;
    private boolean allowMouseRotation = true;

    public RotatableItemPreviewComponent(Sizing size, ItemStack stack) {
        super(stack);
        this.horizontalSizing(size);
        this.verticalSizing(size);
    }

    @Override
    public void draw(OwoUIGraphics context, int mouseX, int mouseY, float partialTicks, float delta) {
        Matrix3x2fStack matrices = context.pose();
        matrices.pushMatrix();
        float centerX = this.x() + (this.width() / 2.0F);
        float centerY = this.y() + (this.height() / 2.0F);
        matrices.translate(centerX, centerY);
        matrices.rotate((float) Math.toRadians(this.rotationDegrees));
        matrices.translate(-centerX, -centerY);
        super.draw(context, mouseX, mouseY, partialTicks, delta);
        matrices.popMatrix();
    }

    @Override
    public boolean onMouseDrag(MouseButtonEvent click, double deltaX, double deltaY) {
        boolean handled = super.onMouseDrag(click, deltaX, deltaY);
        if (this.allowMouseRotation && click.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            this.rotationDegrees += (float) (deltaX * 0.75D);
            return true;
        }
        return handled;
    }

    public RotatableItemPreviewComponent allowMouseRotation(boolean allowMouseRotation) {
        this.allowMouseRotation = allowMouseRotation;
        return this;
    }

    public RotatableItemPreviewComponent rotation(float rotationDegrees) {
        this.rotationDegrees = rotationDegrees;
        return this;
    }
}
