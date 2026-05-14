package me.noramibu.itemeditor.util;

import java.util.List;

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

    public static int interpolateRgb(List<Integer> colors, float progress) {
        if (colors == null || colors.isEmpty()) {
            return 0xFFFFFF;
        }
        if (colors.size() == 1) {
            return colors.getFirst() & 0xFFFFFF;
        }

        float clamped = Math.max(0f, Math.min(1f, progress));
        float scaled = clamped * (colors.size() - 1);
        int startIndex = Math.min(colors.size() - 2, (int) Math.floor(scaled));
        int endIndex = startIndex + 1;
        float segmentProgress = scaled - startIndex;
        return interpolateRgb(colors.get(startIndex), colors.get(endIndex), segmentProgress);
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
