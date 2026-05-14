package me.noramibu.itemeditor.ui.component.raw;

public interface RawEditorTextMeasurer {
    int textWidth(String value);

    int indexAtWidth(String value, double targetWidth);

    float glyphAdvance(int codePoint);
}
