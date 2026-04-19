package me.noramibu.itemeditor.ui.component;

import io.wispforest.owo.ui.component.DiscreteSliderComponent;
import io.wispforest.owo.ui.core.Sizing;

public final class SafeDiscreteSliderComponent extends DiscreteSliderComponent {

    public SafeDiscreteSliderComponent(Sizing horizontalSizing, double min, double max) {
        super(horizontalSizing, min, max);
    }

    @Override
    public boolean onMouseScroll(double mouseX, double mouseY, double amount) {
        return false;
    }
}
