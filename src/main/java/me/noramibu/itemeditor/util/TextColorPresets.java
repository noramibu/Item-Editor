package me.noramibu.itemeditor.util;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.List;
import java.util.Locale;

public final class TextColorPresets {

    public static final List<Preset> STANDARD = List.of(
            new Preset("color.preset.white", 0xFFFFFF),
            new Preset("color.preset.gray", 0xAAAAAA),
            new Preset("color.preset.gold", 0xFFAA00),
            new Preset("color.preset.red", 0xFF5555),
            new Preset("color.preset.green", 0x55FF55),
            new Preset("color.preset.aqua", 0x55FFFF),
            new Preset("color.preset.blue", 0x5555FF),
            new Preset("color.preset.purple", 0xFF55FF)
    );

    private TextColorPresets() {
    }

    public static int gradientEndFor(int startRgb) {
        for (Preset preset : STANDARD.reversed()) {
            if ((preset.rgb() & 0xFFFFFF) != (startRgb & 0xFFFFFF)) {
                return preset.rgb();
            }
        }
        return 0x55FFFF;
    }

    public static Component gradientLabel(String text, int startRgb, int endRgb) {
        MutableComponent root = Component.empty();
        if (text.isEmpty()) {
            return root;
        }

        for (int index = 0; index < text.length(); index++) {
            float progress = text.length() == 1 ? 0f : (float) index / (text.length() - 1);
            root.append(Component.literal(Character.toString(text.charAt(index))).withColor(ColorInterpolationUtil.interpolateRgb(startRgb, endRgb, progress)));
        }
        return root;
    }

    public static Component colorLabel(int color) {
        return ItemEditorText.tr("color.label", hexString(color)).copy().withColor(color);
    }

    public static String hexString(int color) {
        return "#" + String.format(Locale.ROOT, "%06X", color & 0xFFFFFF);
    }

    public record Preset(String labelKey, int rgb) {
        public String label() {
            return ItemEditorText.str(this.labelKey);
        }
    }
}
