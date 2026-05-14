package me.noramibu.itemeditor.ui.component.raw;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

import java.util.HashMap;
import java.util.Map;

public final class RawFontMetrics implements RawEditorTextMeasurer {
    private static final Style RAW_STYLE = Style.EMPTY;
    private static final double TEXT_SCALE_THRESHOLD_GS5 = 5.0d;
    private static final double TEXT_SCALE_THRESHOLD_GS4 = 4.0d;
    private static final double TEXT_SCALE_THRESHOLD_GS3 = 3.0d;
    private static final float TEXT_SCALE_GS5 = 0.72F;
    private static final float TEXT_SCALE_GS4 = 0.80F;
    private static final float TEXT_SCALE_GS3 = 0.90F;
    private static final int TEXT_SCALE_NARROW_WIDTH_THRESHOLD = 560;
    private static final int TEXT_SCALE_NARROW_HEIGHT_THRESHOLD = 300;
    private static final float TEXT_SCALE_NARROW_PENALTY = 0.04F;
    private static final int TEXT_SCALE_WIDE_WIDTH_THRESHOLD = 1200;
    private static final int TEXT_SCALE_WIDE_HEIGHT_THRESHOLD = 700;
    private static final float TEXT_SCALE_WIDE_BONUS = 0.04F;
    private static final float TEXT_SCALE_MIN = 0.10F;
    private static final float TEXT_SCALE_MAX = 5.00F;

    private final Map<Integer, Float> glyphAdvanceCache = new HashMap<>();
    private int fontSizePercent;
    private float cachedTextScale = Float.NaN;

    public RawFontMetrics(int fontSizePercent) {
        this.fontSizePercent = Math.clamp(fontSizePercent, 1, 500);
    }

    public void setFontSizePercent(int fontSizePercent) {
        int clamped = Math.clamp(fontSizePercent, 1, 500);
        if (this.fontSizePercent == clamped) {
            return;
        }
        this.fontSizePercent = clamped;
        this.invalidate();
    }

    public void invalidate() {
        this.cachedTextScale = Float.NaN;
        this.glyphAdvanceCache.clear();
    }

    public float textScale() {
        this.ensureScale();
        return this.cachedTextScale;
    }

    public int lineHeight() {
        this.ensureScale();
        return Math.max(6, (int) Math.ceil(Minecraft.getInstance().font.lineHeight * this.cachedTextScale));
    }

    public int lineHeightWithSpacing(int lineSpacing) {
        this.ensureScale();
        int scaledSpacing = Math.max(1, (int) Math.ceil(lineSpacing * this.cachedTextScale));
        return this.lineHeight() + scaledSpacing;
    }

    @Override
    public int textWidth(String value) {
        if (value == null || value.isEmpty()) {
            return 0;
        }
        this.ensureScale();
        return (int) Math.ceil(Minecraft.getInstance().font.width(this.formattedSequence(value)) * this.cachedTextScale);
    }

    @Override
    public int indexAtWidth(String value, double targetWidth) {
        if (value == null || value.isEmpty() || targetWidth <= 0d) {
            return 0;
        }
        int boundary = 0;
        double width = 0d;
        while (boundary < value.length()) {
            int codePoint = value.codePointAt(boundary);
            int nextBoundary = boundary + Character.charCount(codePoint);
            double glyphWidth = this.glyphAdvance(codePoint);
            double midpoint = width + (glyphWidth * 0.5d);
            if (targetWidth < midpoint) {
                return boundary;
            }
            width += glyphWidth;
            if (targetWidth < width) {
                return nextBoundary;
            }
            boundary = nextBoundary;
        }
        return value.length();
    }

    public String trimToWidth(String value, int maxWidth) {
        if (value == null || value.isEmpty() || maxWidth <= 0) {
            return "";
        }
        int end = this.indexAtWidth(value, maxWidth);
        return end <= 0 ? "" : value.substring(0, end);
    }

    @Override
    public float glyphAdvance(int codePoint) {
        this.ensureScale();
        return this.glyphAdvanceCache.computeIfAbsent(codePoint, cp -> {
            String symbol = new String(Character.toChars(cp));
            return Minecraft.getInstance().font.width(symbol) * this.cachedTextScale;
        });
    }

    public FormattedCharSequence formattedSequence(String value) {
        if (value == null || value.isEmpty()) {
            return FormattedCharSequence.EMPTY;
        }
        return sink -> {
            int index = 0;
            int cursor = 0;
            while (cursor < value.length()) {
                int codePoint = value.codePointAt(cursor);
                if (!sink.accept(index, RAW_STYLE, codePoint)) {
                    return false;
                }
                cursor += Character.charCount(codePoint);
                index++;
            }
            return true;
        };
    }

    private void ensureScale() {
        float scale = this.computeTextScale();
        if (Math.abs(scale - this.cachedTextScale) < 0.001F) {
            return;
        }
        this.cachedTextScale = scale;
        this.glyphAdvanceCache.clear();
    }

    private float computeTextScale() {
        Minecraft minecraft = Minecraft.getInstance();
        double guiScale = minecraft.getWindow().getGuiScale();
        int guiWidth = minecraft.getWindow().getGuiScaledWidth();
        int guiHeight = minecraft.getWindow().getGuiScaledHeight();

        float scale = this.baseScaleForGuiScale(guiScale);

        if (guiWidth <= TEXT_SCALE_NARROW_WIDTH_THRESHOLD || guiHeight <= TEXT_SCALE_NARROW_HEIGHT_THRESHOLD) {
            scale -= TEXT_SCALE_NARROW_PENALTY;
        } else if (guiWidth >= TEXT_SCALE_WIDE_WIDTH_THRESHOLD && guiHeight >= TEXT_SCALE_WIDE_HEIGHT_THRESHOLD) {
            scale += TEXT_SCALE_WIDE_BONUS;
        }

        scale *= (this.fontSizePercent / 100.0F);
        return Math.max(TEXT_SCALE_MIN, Math.min(TEXT_SCALE_MAX, scale));
    }

    private float baseScaleForGuiScale(double guiScale) {
        int guiScaleTier = guiScale >= TEXT_SCALE_THRESHOLD_GS5 ? 3
                : guiScale >= TEXT_SCALE_THRESHOLD_GS4 ? 2
                : guiScale >= TEXT_SCALE_THRESHOLD_GS3 ? 1
                : 0;
        return switch (guiScaleTier) {
            case 3 -> TEXT_SCALE_GS5;
            case 2 -> TEXT_SCALE_GS4;
            case 1 -> TEXT_SCALE_GS3;
            default -> 1.0F;
        };
    }
}
