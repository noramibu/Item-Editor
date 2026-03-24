package me.noramibu.itemeditor.util;

public final class ColorInterpolationUtil {

    private ColorInterpolationUtil() {
    }

    public static int interpolateRgb(int startColor, int endColor, float progress) {
        int startRed = red(startColor);
        int startGreen = green(startColor);
        int startBlue = blue(startColor);
        int endRed = red(endColor);
        int endGreen = green(endColor);
        int endBlue = blue(endColor);

        int red = Math.round(startRed + (endRed - startRed) * progress);
        int green = Math.round(startGreen + (endGreen - startGreen) * progress);
        int blue = Math.round(startBlue + (endBlue - startBlue) * progress);
        return rgb(red, green, blue);
    }

    public static double colorDistanceSquared(int left, int right) {
        int redDelta = red(left) - red(right);
        int greenDelta = green(left) - green(right);
        int blueDelta = blue(left) - blue(right);
        return redDelta * redDelta + greenDelta * greenDelta + blueDelta * blueDelta;
    }

    public static int red(int color) {
        return (color >> 16) & 0xFF;
    }

    public static int green(int color) {
        return (color >> 8) & 0xFF;
    }

    public static int blue(int color) {
        return color & 0xFF;
    }

    public static int rgb(int red, int green, int blue) {
        return (red << 16) | (green << 8) | blue;
    }
}
