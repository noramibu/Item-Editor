package me.noramibu.itemeditor.ui.component;

import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.core.OwoUIGraphics;
import io.wispforest.owo.ui.core.Sizing;
import net.minecraft.network.chat.Component;
import org.joml.Matrix3x2fStack;

public final class ScaledLabelComponent extends LabelComponent {

    private float textScale = 1.0F;

    public ScaledLabelComponent(Component text) {
        super(text);
    }

    public ScaledLabelComponent textScale(float scale) {
        this.textScale = Math.max(0.5F, Math.min(2.0F, scale));
        this.notifyParentIfMounted();
        return this;
    }

    public float textScale() {
        return this.textScale;
    }

    @Override
    protected int determineHorizontalContentSize(Sizing sizing) {
        int base = super.determineHorizontalContentSize(sizing);
        return scaledSize(base);
    }

    @Override
    protected int determineVerticalContentSize(Sizing sizing) {
        int base = super.determineVerticalContentSize(sizing);
        return scaledSize(base) + 1;
    }

    @Override
    public void draw(OwoUIGraphics context, int mouseX, int mouseY, float partialTicks, float delta) {
        if (Math.abs(this.textScale - 1.0F) < 0.001F) {
            super.draw(context, mouseX, mouseY, partialTicks, delta);
            return;
        }

        Matrix3x2fStack matrices = context.pose();
        matrices.pushMatrix();
        matrices.translate(this.x(), this.y());
        matrices.scale(this.textScale, this.textScale);
        matrices.translate(-this.x(), -this.y());
        super.draw(context, mouseX, mouseY, partialTicks, delta);
        matrices.popMatrix();
    }

    @Override
    public void drawTooltip(OwoUIGraphics context, int mouseX, int mouseY, float partialTicks, float delta) {
        try {
            super.drawTooltip(context, mouseX, mouseY, partialTicks, delta);
        } catch (NullPointerException ignored) {
            return;
        }
    }

    private int scaledSize(int base) {
        return Math.max(1, (int) Math.ceil(base * this.textScale));
    }
}
