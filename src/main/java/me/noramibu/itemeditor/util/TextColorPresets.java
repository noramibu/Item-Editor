package me.noramibu.itemeditor.util;

import me.noramibu.itemeditor.storage.ColorPresetService;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.ArrayList;
import java.util.List;

public final class TextColorPresets {

    public static final List<Preset> STANDARD = List.of(
            new Preset("builtin:black", "&0", 0x000000),
            new Preset("builtin:dark_blue", "&1", 0x0000AA),
            new Preset("builtin:dark_green", "&2", 0x00AA00),
            new Preset("builtin:dark_aqua", "&3", 0x00AAAA),
            new Preset("builtin:dark_red", "&4", 0xAA0000),
            new Preset("builtin:dark_purple", "&5", 0xAA00AA),
            new Preset("builtin:gold", "&6", 0xFFAA00),
            new Preset("builtin:gray", "&7", 0xAAAAAA),
            new Preset("builtin:dark_gray", "&8", 0x555555),
            new Preset("builtin:blue", "&9", 0x5555FF),
            new Preset("builtin:green", "&a", 0x55FF55),
            new Preset("builtin:aqua", "&b", 0x55FFFF),
            new Preset("builtin:red", "&c", 0xFF5555),
            new Preset("builtin:purple", "&d", 0xFF55FF),
            new Preset("builtin:yellow", "&e", 0xFFFF55),
            new Preset("builtin:white", "&f", 0xFFFFFF)
    );

    private TextColorPresets() {
    }

    public static List<CustomColorPreset> customColorPresets() {
        return ColorPresetService.instance().customColorPresets();
    }

    public static List<CustomGradientPreset> customGradientPresets() {
        return ColorPresetService.instance().customGradientPresets();
    }

    public static List<CustomShadowPreset> customShadowPresets() {
        return ColorPresetService.instance().customShadowPresets();
    }

    public static void saveColorPreset(int rgb) {
        ColorPresetService.instance().saveColorPreset("", rgb);
    }

    public static void saveGradientPreset(List<Integer> colors) {
        ColorPresetService.instance().saveGradientPreset("", normalizeGradientStops(colors));
    }

    public static void saveShadowPreset(List<Integer> colors) {
        ColorPresetService.instance().saveShadowPreset("", normalizeShadowStops(colors));
    }

    public static boolean updateGradientPreset(String id, List<Integer> colors) {
        return ColorPresetService.instance().updateGradientPreset(id, "", normalizeGradientStops(colors));
    }

    public static boolean updateShadowPreset(String id, List<Integer> colors) {
        return ColorPresetService.instance().updateShadowPreset(id, "", normalizeShadowStops(colors));
    }

    public static void removeColorPreset(String id) {
        ColorPresetService.instance().removeColorPreset(id);
    }

    public static void removeGradientPreset(String id) {
        ColorPresetService.instance().removeGradientPreset(id);
    }

    public static void removeShadowPreset(String id) {
        ColorPresetService.instance().removeShadowPreset(id);
    }

    public static void moveColorPreset(String id, int direction) {
        ColorPresetService.instance().moveColorPreset(id, direction);
    }

    public static void moveGradientPreset(String id, int direction) {
        ColorPresetService.instance().moveGradientPreset(id, direction);
    }

    public static void moveShadowPreset(String id, int direction) {
        ColorPresetService.instance().moveShadowPreset(id, direction);
    }

    public static int gradientEndFor(int startRgb) {
        List<Preset> presets = new ArrayList<>(STANDARD);
        for (CustomColorPreset custom : customColorPresets()) {
            presets.add(new Preset(custom.id(), custom.name(), custom.rgb()));
        }
        for (Preset preset : presets.reversed()) {
            if ((preset.rgb() & 0xFFFFFF) != (startRgb & 0xFFFFFF)) {
                return preset.rgb();
            }
        }
        return 0x55FFFF;
    }

    public static Component gradientLabel(String text, List<Integer> colors) {
        List<Integer> gradientColors = normalizeGradientStops(colors);
        MutableComponent root = Component.empty();
        if (text.isEmpty()) {
            return root;
        }

        int codePointCount = text.codePointCount(0, text.length());
        int colorIndex = 0;
        for (int index = 0; index < text.length();) {
            int codePoint = text.codePointAt(index);
            float progress = codePointCount == 1 ? 0f : (float) colorIndex / (codePointCount - 1);
            root.append(Component.literal(Character.toString(codePoint)).withColor(ColorInterpolationUtil.interpolateRgb(gradientColors, progress)));
            colorIndex++;
            index += Character.charCount(codePoint);
        }
        return root;
    }

    public static List<Integer> normalizeGradientStops(List<Integer> colors) {
        List<Integer> normalized = new ArrayList<>();
        if (colors != null) {
            for (Integer color : colors) {
                if (color != null) {
                    normalized.add(color & 0xFFFFFF);
                }
            }
        }
        if (normalized.isEmpty()) {
            return List.of(0xFFFFFF, 0x55FFFF);
        }
        if (normalized.size() == 1) {
            int start = normalized.getFirst();
            return List.of(start, gradientEndFor(start));
        }
        return List.copyOf(normalized);
    }

    public static String gradientSummary(List<Integer> colors) {
        List<Integer> normalized = normalizeGradientStops(colors);
        StringBuilder builder = new StringBuilder()
                .append(normalized.size())
                .append(" colors: ");
        for (int index = 0; index < normalized.size(); index++) {
            if (index > 0) {
                builder.append(" -> ");
            }
            builder.append(ValidationUtil.toHex(normalized.get(index)));
        }
        return builder.toString();
    }

    public static List<Integer> normalizeShadowStops(List<Integer> colors) {
        List<Integer> normalized = new ArrayList<>();
        if (colors != null) {
            for (Integer color : colors) {
                if (color != null) {
                    normalized.add(color & 0xFFFFFF);
                }
            }
        }
        return List.copyOf(normalized);
    }

    public static List<Integer> normalizeShadowStopsOrDefault(List<Integer> colors) {
        List<Integer> normalized = normalizeShadowStops(colors);
        return normalized.isEmpty() ? List.of(0x000000) : normalized;
    }

    public static String shadowSummary(List<Integer> colors) {
        List<Integer> normalized = normalizeShadowStopsOrDefault(colors);
        if (normalized.size() == 1) {
            return "Shadow " + ValidationUtil.toHex(normalized.getFirst());
        }
        return "Shadow " + gradientSummary(normalized);
    }

    public record Preset(String id, String label, int rgb) {
        public String label() {
            return this.label;
        }
    }

    public record CustomColorPreset(String id, String name, int rgb) {
    }

    public record CustomGradientPreset(String id, String name, List<Integer> colors) {
        public CustomGradientPreset {
            colors = normalizeGradientStops(colors);
        }
    }

    public record CustomShadowPreset(String id, String name, List<Integer> colors) {
        public CustomShadowPreset {
            colors = normalizeShadowStops(colors);
        }
    }
}
