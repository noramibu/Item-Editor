package me.noramibu.itemeditor.util;

import me.noramibu.itemeditor.storage.ColorPresetService;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class TextColorPresets {

    public static final List<Preset> STANDARD = List.of(
            Preset.builtIn("builtin:white", "color.preset.white", 0xFFFFFF),
            Preset.builtIn("builtin:gray", "color.preset.gray", 0xAAAAAA),
            Preset.builtIn("builtin:gold", "color.preset.gold", 0xFFAA00),
            Preset.builtIn("builtin:red", "color.preset.red", 0xFF5555),
            Preset.builtIn("builtin:green", "color.preset.green", 0x55FF55),
            Preset.builtIn("builtin:aqua", "color.preset.aqua", 0x55FFFF),
            Preset.builtIn("builtin:blue", "color.preset.blue", 0x5555FF),
            Preset.builtIn("builtin:purple", "color.preset.purple", 0xFF55FF)
    );

    private TextColorPresets() {
    }

    public static List<Preset> colorPresets() {
        List<Preset> presets = new ArrayList<>(STANDARD);
        for (CustomColorPreset custom : customColorPresets()) {
            presets.add(Preset.custom(custom.id(), custom.name(), custom.rgb()));
        }
        return presets;
    }

    public static List<GradientPreset> gradientPresets() {
        List<GradientPreset> presets = new ArrayList<>();
        for (CustomGradientPreset custom : customGradientPresets()) {
            presets.add(new GradientPreset(custom.id(), custom.name(), custom.startRgb(), custom.endRgb()));
        }
        return presets;
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

    public static void saveColorPreset(String name, int rgb) {
        ColorPresetService.instance().saveColorPreset(name, rgb);
    }

    public static void saveGradientPreset(String name, int startRgb, int endRgb) {
        ColorPresetService.instance().saveGradientPreset(name, startRgb, endRgb);
    }

    public static void removeColorPreset(String id) {
        ColorPresetService.instance().removeColorPreset(id);
    }

    public static void removeGradientPreset(String id) {
        ColorPresetService.instance().removeGradientPreset(id);
    }

    public static int gradientEndFor(int startRgb) {
        for (Preset preset : colorPresets().reversed()) {
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
        return ItemEditorText.tr("color.label", hexString(color)).copy().withColor(color);
    }

    public static String hexString(int color) {
        return "#" + String.format(Locale.ROOT, "%06X", color & 0xFFFFFF);
    }

    public record Preset(String id, String labelValue, int rgb, boolean translatable) {
        public static Preset builtIn(String id, String labelKey, int rgb) {
            return new Preset(id, labelKey, rgb, true);
        }

        public static Preset custom(String id, String label, int rgb) {
            return new Preset(id, label, rgb, false);
        }

        public String label() {
            return this.translatable ? ItemEditorText.str(this.labelValue) : this.labelValue;
        }
    }

    public record GradientPreset(String id, String name, int startRgb, int endRgb) {
    }

    public record CustomColorPreset(String id, String name, int rgb) {
    }

    public record CustomGradientPreset(String id, String name, int startRgb, int endRgb) {
    }
}
