package me.noramibu.itemeditor.ui.util;

import io.wispforest.owo.ui.core.Surface;
import net.minecraft.client.Minecraft;

public final class MenuBackgroundSurface {
    private static final int STANDARD_BLUR_QUALITY = 8;
    private static final int STANDARD_TINT = 0x6610151A;

    private MenuBackgroundSurface() {
    }

    public static Surface standard() {
        return tinted(STANDARD_TINT, STANDARD_BLUR_QUALITY);
    }

    public static Surface tinted(int tint, int blurQuality) {
        int blurRadius = Math.max(0, Minecraft.getInstance().options.getMenuBackgroundBlurriness());
        Surface tintSurface = Surface.flat(tint);
        return blurRadius == 0 ? tintSurface : Surface.blur(blurRadius, blurQuality).and(tintSurface);
    }
}
