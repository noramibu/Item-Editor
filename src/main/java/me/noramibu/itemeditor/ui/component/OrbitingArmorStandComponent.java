package me.noramibu.itemeditor.ui.component;

import io.wispforest.owo.ui.component.EntityComponent;
import io.wispforest.owo.ui.core.Sizing;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

public final class OrbitingArmorStandComponent extends EntityComponent<@NotNull ArmorStand> {

    private float pitchDegrees = 18.0F;

    public OrbitingArmorStandComponent(@NotNull Sizing size, @NotNull CompoundTag tag) {
        super(size, EntityType.ARMOR_STAND, tag);
        this.transform(matrix -> matrix.rotateX((float) Math.toRadians(-this.pitchDegrees)));
    }

    public OrbitingArmorStandComponent pitch(float degrees) {
        this.pitchDegrees = clampPitch(degrees);
        return this;
    }

    public float pitch() {
        return this.pitchDegrees;
    }

    @Override
    public boolean onMouseDrag(@NotNull MouseButtonEvent click, double deltaX, double deltaY) {
        boolean handled = super.onMouseDrag(click, deltaX, deltaY);
        if (click.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT && this.allowMouseRotation()) {
            this.pitchDegrees = clampPitch(this.pitchDegrees - (float) (deltaY * 0.75D));
            return true;
        }
        return handled;
    }

    private static float clampPitch(float value) {
        return Math.max(-80.0F, Math.min(80.0F, value));
    }
}
