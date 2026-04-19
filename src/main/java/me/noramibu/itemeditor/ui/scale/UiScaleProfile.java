package me.noramibu.itemeditor.ui.scale;

public record UiScaleProfile(
        double scale,
        int spacing,
        int tightSpacing,
        int padding,
        int controlHeight,
        float titleTextScale,
        float bodyTextScale,
        float captionTextScale,
        int titleLineHeight,
        int bodyLineHeight,
        int captionLineHeight,
        int bodyLineSpacing,
        int fieldTextWidth,
        int bodyTextWidth,
        int scrollbarThickness,
        int scrollStep
) {
}
