package me.noramibu.itemeditor.util;

import me.noramibu.itemeditor.storage.ColorPresetService;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.ArrayList;
import java.util.List;

public final class TextColorPresets {

    public static final List<Preset> STANDARD = List.of(
            new Preset("builtin:white", "color.preset.white", 0xFFFFFF, true),
            new Preset("builtin:gray", "color.preset.gray", 0xAAAAAA, true),
            new Preset("builtin:gold", "color.preset.gold", 0xFFAA00, true),
            new Preset("builtin:red", "color.preset.red", 0xFF5555, true),
            new Preset("builtin:green", "color.preset.green", 0x55FF55, true),
            new Preset("builtin:aqua", "color.preset.aqua", 0x55FFFF, true),
            new Preset("builtin:blue", "color.preset.blue", 0x5555FF, true),
            new Preset("builtin:purple", "color.preset.purple", 0xFF55FF, true)
    );

    private TextColorPresets() {
    }

    public static List<CustomColorPreset> customColorPresets() {
        return ColorPresetService.instance().customColorPresets();
    }

    public static List<CustomGradientPreset> customGradientPresets() {
        return ColorPresetService.instance().customGradientPresets();
    }

    public static void saveColorPreset(int rgb) {
        ColorPresetService.instance().saveColorPreset("", rgb);
    }

    public static void saveGradientPreset(int startRgb, int endRgb) {
        ColorPresetService.instance().saveGradientPreset("", startRgb, endRgb);
    }

    public static void removeColorPreset(String id) {
        ColorPresetService.instance().removeColorPreset(id);
    }

    public static void removeGradientPreset(String id) {
        ColorPresetService.instance().removeGradientPreset(id);
    }

    public static int gradientEndFor(int startRgb) {
        List<Preset> presets = new ArrayList<>(STANDARD);
        for (CustomColorPreset custom : customColorPresets()) {
            presets.add(new Preset(custom.id(), custom.name(), custom.rgb(), false));
        }
        for (Preset preset : presets.reversed()) {
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

        int codePointCount = text.codePointCount(0, text.length());
        int colorIndex = 0;
        for (int index = 0; index < text.length();) {
            int codePoint = text.codePointAt(index);
            float progress = codePointCount == 1 ? 0f : (float) colorIndex / (codePointCount - 1);
            root.append(Component.literal(Character.toString(codePoint)).withColor(ColorInterpolationUtil.interpolateRgb(startRgb, endRgb, progress)));
            colorIndex++;
            index += Character.charCount(codePoint);
        }
        return root;
    }

    public static Component colorLabel(int color) {
        return ItemEditorText.tr("color.label", ValidationUtil.toHex(color)).copy().withColor(color);
    }

    public record Preset(String id, String labelValue, int rgb, boolean translatable) {
        public String label() {
            return this.translatable ? ItemEditorText.str(this.labelValue) : this.labelValue;
        }
    }

    public record CustomColorPreset(String id, String name, int rgb) {
    }

    public record CustomGradientPreset(String id, String name, int startRgb, int endRgb) {
    }
}
