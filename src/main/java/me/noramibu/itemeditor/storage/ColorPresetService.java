package me.noramibu.itemeditor.storage;

import me.noramibu.itemeditor.storage.model.ColorPresetEntry;
import me.noramibu.itemeditor.storage.model.ColorsFileModel;
import me.noramibu.itemeditor.util.TextColorPresets;
import me.noramibu.itemeditor.util.ValidationUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

public final class ColorPresetService {

    private static final ColorPresetService INSTANCE = new ColorPresetService();

    private ColorPresetService() {
    }

    public static ColorPresetService instance() {
        return INSTANCE;
    }

    public synchronized List<TextColorPresets.CustomColorPreset> customColorPresets() {
        ColorsFileModel model = loadColorsWithStableIds();
        List<TextColorPresets.CustomColorPreset> presets = new ArrayList<>();
        for (ColorPresetEntry entry : model.color) {
            Integer rgb = parseColor(entry);
            if (rgb == null) {
                continue;
            }
            String name = entry.name == null || entry.name.isBlank() ? ValidationUtil.toHex(rgb) : entry.name;
            presets.add(new TextColorPresets.CustomColorPreset(entry.id, name, rgb));
        }
        return presets;
    }

    public synchronized List<TextColorPresets.CustomGradientPreset> customGradientPresets() {
        ColorsFileModel model = loadColorsWithStableIds();
        List<TextColorPresets.CustomGradientPreset> presets = new ArrayList<>();
        for (ColorPresetEntry entry : model.gradient) {
            List<Integer> stops = parseGradientStops(entry);
            if (stops.size() < 2) {
                continue;
            }
            String name = entry.name == null || entry.name.isBlank()
                    ? TextColorPresets.gradientSummary(stops)
                    : entry.name;
            presets.add(new TextColorPresets.CustomGradientPreset(entry.id, name, stops));
        }
        return presets;
    }

    public synchronized List<TextColorPresets.CustomShadowPreset> customShadowPresets() {
        ColorsFileModel model = loadColorsWithStableIds();
        List<TextColorPresets.CustomShadowPreset> presets = new ArrayList<>();
        for (ColorPresetEntry entry : model.shadow) {
            List<Integer> stops = parseShadowStops(entry);
            if (stops.isEmpty()) {
                continue;
            }
            String name = entry.name == null || entry.name.isBlank()
                    ? TextColorPresets.shadowSummary(stops)
                    : entry.name;
            presets.add(new TextColorPresets.CustomShadowPreset(entry.id, name, stops));
        }
        return presets;
    }

    public synchronized void saveColorPreset(String name, int rgb) {
        ColorsFileModel model = StorageServices.foundation().loadColors();
        long now = System.currentTimeMillis();
        String hex = ValidationUtil.toHex(rgb);
        String normalizedName = normalizeName(name, hex);

        ColorPresetEntry existing = null;
        for (ColorPresetEntry candidate : model.color) {
            Integer candidateRgb = parseColor(candidate);
            if (candidateRgb == null) {
                continue;
            }
            boolean sameName = candidate.name != null && candidate.name.equalsIgnoreCase(normalizedName);
            boolean sameValue = ValidationUtil.toHex(candidateRgb).equalsIgnoreCase(hex);
            if (sameName || sameValue) {
                existing = candidate;
                break;
            }
        }

        if (existing == null) {
            ColorPresetEntry created = new ColorPresetEntry();
            created.id = UUID.randomUUID().toString();
            created.name = normalizedName;
            created.value = hex;
            created.createdAt = now;
            created.updatedAt = now;
            model.color.add(created);
        } else {
            existing.name = normalizedName;
            existing.value = hex;
            existing.updatedAt = now;
            if (existing.createdAt <= 0L) {
                existing.createdAt = now;
            }
        }
        StorageServices.foundation().saveColors(model);
    }

    public synchronized void saveGradientPreset(String name, List<Integer> colors) {
        List<Integer> normalizedStops = TextColorPresets.normalizeGradientStops(colors);
        saveStopsPreset(PresetBucket.GRADIENT, name, normalizedStops, TextColorPresets.gradientSummary(normalizedStops), 2);
    }

    public synchronized void saveShadowPreset(String name, List<Integer> colors) {
        List<Integer> normalizedStops = TextColorPresets.normalizeShadowStops(colors);
        saveStopsPreset(PresetBucket.SHADOW, name, normalizedStops, TextColorPresets.shadowSummary(normalizedStops), 1);
    }

    public synchronized boolean updateGradientPreset(String id, String name, List<Integer> colors) {
        List<Integer> normalizedStops = TextColorPresets.normalizeGradientStops(colors);
        return updateStopsPreset(
                loadColorsWithStableIds(),
                PresetBucket.GRADIENT,
                id,
                name,
                normalizedStops,
                TextColorPresets.gradientSummary(normalizedStops)
        );
    }

    public synchronized boolean updateShadowPreset(String id, String name, List<Integer> colors) {
        List<Integer> normalizedStops = TextColorPresets.normalizeShadowStops(colors);
        if (normalizedStops.isEmpty()) {
            return false;
        }
        return updateStopsPreset(
                loadColorsWithStableIds(),
                PresetBucket.SHADOW,
                id,
                name,
                normalizedStops,
                TextColorPresets.shadowSummary(normalizedStops)
        );
    }

    public synchronized void removeColorPreset(String id) {
        removeById(id, PresetBucket.COLOR);
    }

    public synchronized void removeGradientPreset(String id) {
        removeById(id, PresetBucket.GRADIENT);
    }

    public synchronized void removeShadowPreset(String id) {
        removeById(id, PresetBucket.SHADOW);
    }

    public synchronized void moveColorPreset(String id, int direction) {
        moveById(id, PresetBucket.COLOR, direction);
    }

    public synchronized void moveGradientPreset(String id, int direction) {
        moveById(id, PresetBucket.GRADIENT, direction);
    }

    public synchronized void moveShadowPreset(String id, int direction) {
        moveById(id, PresetBucket.SHADOW, direction);
    }

    private boolean updateStopsPreset(
            ColorsFileModel model,
            PresetBucket bucket,
            String id,
            String name,
            List<Integer> colors,
            String defaultName
    ) {
        String normalizedId = id == null ? "" : id.trim();
        if (normalizedId.isEmpty()) {
            return false;
        }

        long now = System.currentTimeMillis();
        String normalizedName = normalizeName(name, defaultName);
        List<String> hexStops = hexStops(bucket, colors);

        for (ColorPresetEntry entry : entries(model, bucket)) {
            if (entry == null || !normalizedId.equals(entry.id)) {
                continue;
            }
            writeStopsEntry(entry, normalizedName, hexStops, now);
            StorageServices.foundation().saveColors(model);
            return true;
        }
        return false;
    }

    private static void saveStopsPreset(
            PresetBucket bucket,
            String name,
            List<Integer> colors,
            String defaultName,
            int minimumStops
    ) {
        if (colors.size() < minimumStops) {
            return;
        }

        ColorsFileModel model = StorageServices.foundation().loadColors();
        long now = System.currentTimeMillis();
        List<String> hexStops = hexStops(bucket, colors);
        String normalizedName = normalizeName(name, defaultName);
        List<ColorPresetEntry> entries = entries(model, bucket);
        ColorPresetEntry entry = matchingStopsEntry(entries, normalizedName, hexStops, minimumStops);
        if (entry == null) {
            entry = new ColorPresetEntry();
            entry.id = UUID.randomUUID().toString();
            entry.createdAt = now;
            entries.add(entry);
        }
        writeStopsEntry(entry, normalizedName, hexStops, now);
        StorageServices.foundation().saveColors(model);
    }

    private static void writeStopsEntry(ColorPresetEntry entry, String name, List<String> hexStops, long now) {
        entry.name = name;
        entry.stops = new ArrayList<>(hexStops);
        entry.updatedAt = now;
        if (entry.createdAt <= 0L) {
            entry.createdAt = now;
        }
    }

    private static ColorPresetEntry matchingStopsEntry(
            List<ColorPresetEntry> entries,
            String normalizedName,
            List<String> hexStops,
            int minimumStops
    ) {
        for (ColorPresetEntry candidate : entries) {
            if (candidate == null) {
                continue;
            }
            boolean sameName = candidate.name != null && candidate.name.equalsIgnoreCase(normalizedName);
            List<Integer> candidateStops = minimumStops <= 1 ? parseShadowStops(candidate) : parseGradientStops(candidate);
            boolean sameStops = candidateStops.size() >= minimumStops && hexRawStops(candidateStops).equals(hexStops);
            if (sameName || sameStops) {
                return candidate;
            }
        }
        return null;
    }

    private static Integer parseColor(ColorPresetEntry entry) {
        if (entry == null) {
            return null;
        }
        return ValidationUtil.tryParseHexColor(entry.value);
    }

    private static List<Integer> parseGradientStops(ColorPresetEntry entry) {
        List<Integer> parsedStops = parseRawStops(entry);
        return parsedStops.size() >= 2 ? TextColorPresets.normalizeGradientStops(parsedStops) : List.of();
    }

    private static List<Integer> parseShadowStops(ColorPresetEntry entry) {
        return TextColorPresets.normalizeShadowStops(parseRawStops(entry));
    }

    private static List<Integer> parseRawStops(ColorPresetEntry entry) {
        List<Integer> parsedStops = new ArrayList<>();
        if (entry == null || entry.stops == null) {
            return parsedStops;
        }
        for (String stop : entry.stops) {
            Integer color = ValidationUtil.tryParseHexColor(stop);
            if (color != null) {
                parsedStops.add(color);
            }
        }
        return parsedStops;
    }

    private static List<String> hexGradientStops(List<Integer> colors) {
        return hexRawStops(TextColorPresets.normalizeGradientStops(colors));
    }

    private static List<String> hexShadowStops(List<Integer> colors) {
        return hexRawStops(TextColorPresets.normalizeShadowStops(colors));
    }

    private static List<String> hexStops(PresetBucket bucket, List<Integer> colors) {
        return bucket == PresetBucket.SHADOW ? hexShadowStops(colors) : hexGradientStops(colors);
    }

    private static List<String> hexRawStops(List<Integer> colors) {
        List<String> hexStops = new ArrayList<>();
        if (colors == null) {
            return hexStops;
        }
        for (Integer color : colors) {
            if (color != null) {
                hexStops.add(ValidationUtil.toHex(color));
            }
        }
        return hexStops;
    }

    private void removeById(String id, PresetBucket bucket) {
        String normalizedId = id == null ? "" : id.trim();
        if (normalizedId.isEmpty()) {
            return;
        }
        ColorsFileModel model = StorageServices.foundation().loadColors();
        if (entries(model, bucket).removeIf(entry -> entry != null && normalizedId.equals(entry.id))) {
            StorageServices.foundation().saveColors(model);
        }
    }

    private void moveById(String id, PresetBucket bucket, int direction) {
        String normalizedId = id == null ? "" : id.trim();
        if (normalizedId.isEmpty() || direction == 0) {
            return;
        }

        ColorsFileModel model = loadColorsWithStableIds();
        List<ColorPresetEntry> entries = entries(model, bucket);
        Predicate<ColorPresetEntry> visible = visiblePredicate(bucket);
        int currentIndex = visibleEntryIndex(entries, normalizedId, visible);
        if (currentIndex < 0) {
            return;
        }

        int step = direction < 0 ? -1 : 1;
        for (int index = currentIndex + step; index >= 0 && index < entries.size(); index += step) {
            if (!visible.test(entries.get(index))) {
                continue;
            }
            ColorPresetEntry current = entries.get(currentIndex);
            entries.set(currentIndex, entries.get(index));
            entries.set(index, current);
            StorageServices.foundation().saveColors(model);
            return;
        }
    }

    private static int visibleEntryIndex(List<ColorPresetEntry> entries, String id, Predicate<ColorPresetEntry> visible) {
        for (int index = 0; index < entries.size(); index++) {
            ColorPresetEntry entry = entries.get(index);
            if (entry != null && id.equals(entry.id) && visible.test(entry)) {
                return index;
            }
        }
        return -1;
    }

    private static Predicate<ColorPresetEntry> visiblePredicate(PresetBucket bucket) {
        return switch (bucket) {
            case COLOR -> entry -> parseColor(entry) != null;
            case GRADIENT -> entry -> parseGradientStops(entry).size() >= 2;
            case SHADOW -> entry -> !parseShadowStops(entry).isEmpty();
        };
    }

    private static List<ColorPresetEntry> entries(ColorsFileModel model, PresetBucket bucket) {
        return switch (bucket) {
            case COLOR -> model.color;
            case GRADIENT -> model.gradient;
            case SHADOW -> model.shadow;
        };
    }

    private static String normalizeName(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return fallback;
        }
        if (trimmed.length() > 64) {
            return trimmed.substring(0, 64);
        }
        return trimmed;
    }

    private ColorsFileModel loadColorsWithStableIds() {
        ColorsFileModel model = StorageServices.foundation().loadColors();
        boolean changed = false;
        for (PresetBucket bucket : PresetBucket.values()) {
            for (ColorPresetEntry entry : entries(model, bucket)) {
                if (entry == null) {
                    continue;
                }
                if (entry.id == null || entry.id.isBlank()) {
                    entry.id = UUID.randomUUID().toString();
                    changed = true;
                }
            }
        }
        if (changed) {
            StorageServices.foundation().saveColors(model);
        }
        return model;
    }

    private enum PresetBucket {
        COLOR,
        GRADIENT,
        SHADOW
    }
}
