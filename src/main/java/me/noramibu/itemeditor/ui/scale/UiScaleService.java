package me.noramibu.itemeditor.ui.scale;

import net.minecraft.client.Minecraft;

public final class UiScaleService {

    private static final int BASE_WIDTH = 1920;
    private static final int BASE_HEIGHT = 1080;
    private static final double GUI_SCALE_HIGH_THRESHOLD = 3.0d;
    private static final double GUI_SCALE_LOW_THRESHOLD = 1.0d;
    private static final double GUI_SCALE_HIGH_COMPENSATION = 0.96d;
    private static final double GUI_SCALE_LOW_COMPENSATION = 1.04d;
    private static final double WIDTH_ASPECT_PENALTY_THRESHOLD = 0.58d;
    private static final double HEIGHT_COMPRESSION_THRESHOLD = 0.62d;
    private static final double HEIGHT_COMPRESSION_FACTOR = 0.92d;
    private static final double SCALE_MIN = 0.90d;
    private static final double SCALE_MAX = 1.55d;
    private static final int SPACING_BASE = 4;
    private static final int SPACING_MIN = 3;
    private static final int SPACING_MAX = 9;
    private static final int TIGHT_SPACING_BASE = 3;
    private static final int TIGHT_SPACING_MIN = 2;
    private static final int TIGHT_SPACING_MAX = 7;
    private static final int PADDING_BASE = 6;
    private static final int PADDING_MIN = 4;
    private static final int PADDING_MAX = 12;
    private static final int CONTROL_HEIGHT_BASE = 20;
    private static final int CONTROL_HEIGHT_MIN = 18;
    private static final int CONTROL_HEIGHT_MAX = 34;
    private static final int TITLE_LINE_BASE = 10;
    private static final int TITLE_LINE_MIN = 10;
    private static final int TITLE_LINE_MAX = 16;
    private static final int BODY_LINE_BASE = 9;
    private static final int BODY_LINE_MIN = 9;
    private static final int BODY_LINE_MAX = 14;
    private static final int CAPTION_LINE_BASE = 8;
    private static final int CAPTION_LINE_MIN = 9;
    private static final int CAPTION_LINE_MAX = 13;
    private static final double BODY_LINE_SPACING_SCALE_FACTOR = 1.2d;
    private static final int BODY_LINE_SPACING_MIN = 0;
    private static final int BODY_LINE_SPACING_MAX = 3;
    private static final double FIELD_TEXT_WIDTH_RATIO = 0.18d;
    private static final int FIELD_TEXT_WIDTH_MIN = 120;
    private static final int FIELD_TEXT_WIDTH_MAX = 420;
    private static final double BODY_TEXT_WIDTH_RATIO = 0.22d;
    private static final int BODY_TEXT_WIDTH_MIN = 130;
    private static final int BODY_TEXT_WIDTH_MAX = 520;
    private static final int SCROLLBAR_BASE = 7;
    private static final int SCROLLBAR_MIN = 6;
    private static final int SCROLLBAR_MAX = 13;
    private static final int SCROLL_STEP_BASE = 12;
    private static final int SCROLL_STEP_MIN = 10;
    private static final int SCROLL_STEP_MAX = 28;

    private static int cachedWidth = -1;
    private static int cachedHeight = -1;
    private static long cachedGuiScaleBucket = Long.MIN_VALUE;
    private static UiScaleProfile cachedProfile = new UiScaleProfile(
            1.0d,
            4,
            3,
            6,
            20,
            1.0F,
            1.0F,
            0.95F,
            10,
            9,
            8,
            1,
            180,
            220,
            7,
            12
    );

    private UiScaleService() {
    }

    public static UiScaleProfile profile() {
        Minecraft minecraft = Minecraft.getInstance();
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        long guiScaleBucket = Math.round(minecraft.getWindow().getGuiScale() * 100.0d);

        if (width == cachedWidth && height == cachedHeight && guiScaleBucket == cachedGuiScaleBucket) {
            return cachedProfile;
        }

        cachedWidth = width;
        cachedHeight = height;
        cachedGuiScaleBucket = guiScaleBucket;
        cachedProfile = compute(width, height, minecraft.getWindow().getGuiScale());
        return cachedProfile;
    }

    private static UiScaleProfile compute(int width, int height, double guiScale) {
        double areaFactor = Math.sqrt((Math.max(1d, width) * Math.max(1d, height)) / (BASE_WIDTH * (double) BASE_HEIGHT));
        double widthFactor = width / (double) BASE_WIDTH;
        double heightFactor = height / (double) BASE_HEIGHT;
        double aspectPenalty = widthFactor < WIDTH_ASPECT_PENALTY_THRESHOLD ? HEIGHT_COMPRESSION_FACTOR : 1.0d;
        double guiScaleCompensation = guiScale >= GUI_SCALE_HIGH_THRESHOLD
                ? GUI_SCALE_HIGH_COMPENSATION
                : guiScale <= GUI_SCALE_LOW_THRESHOLD ? GUI_SCALE_LOW_COMPENSATION : 1.0d;

        double scale = areaFactor * aspectPenalty * guiScaleCompensation;
        if (heightFactor < HEIGHT_COMPRESSION_THRESHOLD) {
            scale *= HEIGHT_COMPRESSION_FACTOR;
        }
        scale = Math.clamp(scale, SCALE_MIN, SCALE_MAX);

        int spacing = clampInt((int) Math.round(SPACING_BASE * scale), SPACING_MIN, SPACING_MAX);
        int tightSpacing = clampInt((int) Math.round(TIGHT_SPACING_BASE * scale), TIGHT_SPACING_MIN, TIGHT_SPACING_MAX);
        int padding = clampInt((int) Math.round(PADDING_BASE * scale), PADDING_MIN, PADDING_MAX);
        int controlHeight = clampInt((int) Math.round(CONTROL_HEIGHT_BASE * scale), CONTROL_HEIGHT_MIN, CONTROL_HEIGHT_MAX);
        // Keep text raster crisp by rendering at native font scale (1.0) and scaling layout instead.
        float titleTextScale = 1.0F;
        float bodyTextScale = 1.0F;
        float captionTextScale = 1.0F;
        int titleLineHeight = clampInt((int) Math.round(TITLE_LINE_BASE * scale), TITLE_LINE_MIN, TITLE_LINE_MAX);
        int bodyLineHeight = clampInt((int) Math.round(BODY_LINE_BASE * scale), BODY_LINE_MIN, BODY_LINE_MAX);
        int captionLineHeight = clampInt((int) Math.round(CAPTION_LINE_BASE * scale), CAPTION_LINE_MIN, CAPTION_LINE_MAX);
        int bodyLineSpacing = clampInt(
                (int) Math.round((scale - 1.0d) * BODY_LINE_SPACING_SCALE_FACTOR),
                BODY_LINE_SPACING_MIN,
                BODY_LINE_SPACING_MAX
        );
        int fieldTextWidth = clampInt((int) Math.round(width * FIELD_TEXT_WIDTH_RATIO), FIELD_TEXT_WIDTH_MIN, FIELD_TEXT_WIDTH_MAX);
        int bodyTextWidth = clampInt((int) Math.round(width * BODY_TEXT_WIDTH_RATIO), BODY_TEXT_WIDTH_MIN, BODY_TEXT_WIDTH_MAX);
        int scrollbarThickness = clampInt((int) Math.round(SCROLLBAR_BASE * scale), SCROLLBAR_MIN, SCROLLBAR_MAX);
        int scrollStep = clampInt((int) Math.round(SCROLL_STEP_BASE * scale), SCROLL_STEP_MIN, SCROLL_STEP_MAX);

        return new UiScaleProfile(
                scale,
                spacing,
                tightSpacing,
                padding,
                controlHeight,
                titleTextScale,
                bodyTextScale,
                captionTextScale,
                titleLineHeight,
                bodyLineHeight,
                captionLineHeight,
                bodyLineSpacing,
                fieldTextWidth,
                bodyTextWidth,
                scrollbarThickness,
                scrollStep
        );
    }

    private static int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

}
