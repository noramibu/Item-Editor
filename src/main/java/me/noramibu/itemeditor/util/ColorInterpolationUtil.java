package me.noramibu.itemeditor.util;

import java.util.List;

public final class ColorInterpolationUtil {

    private ColorInterpolationUtil() {
    }

    public static int interpolateRgb(int startColor, int endColor, float progress) {
        int red = interpolateChannel((startColor >> 16) & 0xFF, (endColor >> 16) & 0xFF, progress);
        int green = interpolateChannel((startColor >> 8) & 0xFF, (endColor >> 8) & 0xFF, progress);
        int blue = interpolateChannel(startColor & 0xFF, endColor & 0xFF, progress);
        return (red << 16) | (green << 8) | blue;
    }

    public static int interpolateRgb(List<Integer> colors, float progress) {
        if (colors == null || colors.isEmpty()) {
            return 0xFFFFFF;
        }
        if (colors.size() == 1) {
            return colors.getFirst() & 0xFFFFFF;
        }

        float clamped = Math.clamp(progress, 0f, 1f);
        float scaled = clamped * (colors.size() - 1);
        int startIndex = Math.min(colors.size() - 2, (int) Math.floor(scaled));
        int endIndex = startIndex + 1;
        float segmentProgress = scaled - startIndex;
        return interpolateRgb(colors.get(startIndex), colors.get(endIndex), segmentProgress);
    }

    public static double colorDistanceSquared(int left, int right) {
        int redDelta = ((left >> 16) & 0xFF) - ((right >> 16) & 0xFF);
        int greenDelta = ((left >> 8) & 0xFF) - ((right >> 8) & 0xFF);
        int blueDelta = (left & 0xFF) - (right & 0xFF);
        return redDelta * redDelta + greenDelta * greenDelta + blueDelta * blueDelta;
    }

    private static int interpolateChannel(int start, int end, float progress) {
        return Math.round(start + (end - start) * progress);
    }
}
