package me.noramibu.itemeditor.storage;

import me.noramibu.itemeditor.storage.model.ColorPresetEntry;
import me.noramibu.itemeditor.storage.model.ColorsFileModel;
import me.noramibu.itemeditor.util.TextColorPresets;
import me.noramibu.itemeditor.util.ValidationUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
        for (ColorPresetEntry entry : model.colors) {
            if (entry == null || !"color".equalsIgnoreCase(entry.type)) {
                continue;
            }
            Integer rgb = ValidationUtil.tryParseHexColor(entry.value);
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
        for (ColorPresetEntry entry : model.colors) {
            if (entry == null || !"gradient".equalsIgnoreCase(entry.type)) {
                continue;
            }
            Integer start = ValidationUtil.tryParseHexColor(entry.start);
            Integer end = ValidationUtil.tryParseHexColor(entry.end);
            if (start == null || end == null) {
                continue;
            }
            String name = entry.name == null || entry.name.isBlank()
                    ? ValidationUtil.toHex(start) + " -> " + ValidationUtil.toHex(end)
                    : entry.name;
            presets.add(new TextColorPresets.CustomGradientPreset(entry.id, name, start, end));
        }
        return presets;
    }

    public synchronized void saveColorPreset(String name, int rgb) {
        ColorsFileModel model = StorageServices.foundation().loadColors();
        long now = System.currentTimeMillis();
        String hex = ValidationUtil.toHex(rgb);
        String normalizedName = normalizeName(name, hex);

        ColorPresetEntry existing = null;
        for (ColorPresetEntry candidate : model.colors) {
            if (candidate == null || !"color".equalsIgnoreCase(candidate.type)) {
                continue;
            }
            boolean sameName = candidate.name != null && candidate.name.equalsIgnoreCase(normalizedName);
            boolean sameValue = candidate.value != null && candidate.value.equalsIgnoreCase(hex);
            if (sameName || sameValue) {
                existing = candidate;
                break;
            }
        }

        if (existing == null) {
            ColorPresetEntry created = new ColorPresetEntry();
            created.id = UUID.randomUUID().toString();
            created.name = normalizedName;
            created.type = "color";
            created.value = hex;
            created.createdAt = now;
            created.updatedAt = now;
            model.colors.add(created);
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

    public synchronized void saveGradientPreset(String name, int startRgb, int endRgb) {
        ColorsFileModel model = StorageServices.foundation().loadColors();
        long now = System.currentTimeMillis();
        String startHex = ValidationUtil.toHex(startRgb);
        String endHex = ValidationUtil.toHex(endRgb);
        String defaultName = startHex + " -> " + endHex;
        String normalizedName = normalizeName(name, defaultName);

        ColorPresetEntry existing = null;
        for (ColorPresetEntry candidate : model.colors) {
            if (candidate == null || !"gradient".equalsIgnoreCase(candidate.type)) {
                continue;
            }
            boolean sameName = candidate.name != null && candidate.name.equalsIgnoreCase(normalizedName);
            boolean sameGradient = candidate.start != null && candidate.end != null
                    && candidate.start.equalsIgnoreCase(startHex)
                    && candidate.end.equalsIgnoreCase(endHex);
            if (sameName || sameGradient) {
                existing = candidate;
                break;
            }
        }

        if (existing == null) {
            ColorPresetEntry created = new ColorPresetEntry();
            created.id = UUID.randomUUID().toString();
            created.name = normalizedName;
            created.type = "gradient";
            created.start = startHex;
            created.end = endHex;
            created.createdAt = now;
            created.updatedAt = now;
            model.colors.add(created);
        } else {
            existing.name = normalizedName;
            existing.start = startHex;
            existing.end = endHex;
            existing.updatedAt = now;
            if (existing.createdAt <= 0L) {
                existing.createdAt = now;
            }
        }
        StorageServices.foundation().saveColors(model);
    }

    public synchronized void removeColorPreset(String id) {
        removeByIdAndType(id, "color");
    }

    public synchronized void removeGradientPreset(String id) {
        removeByIdAndType(id, "gradient");
    }

    private void removeByIdAndType(String id, String type) {
        String normalizedId = id == null ? "" : id.trim();
        if (normalizedId.isEmpty()) {
            return;
        }
        ColorsFileModel model = StorageServices.foundation().loadColors();
        if (model.colors.removeIf(entry -> entry != null
                && type.equalsIgnoreCase(entry.type)
                && normalizedId.equals(entry.id))) {
            StorageServices.foundation().saveColors(model);
        }
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
        for (ColorPresetEntry entry : model.colors) {
            if (entry == null) {
                continue;
            }
            if (entry.id == null || entry.id.isBlank()) {
                entry.id = UUID.randomUUID().toString();
                changed = true;
            }
        }
        if (changed) {
            StorageServices.foundation().saveColors(model);
        }
        return model;
    }
}
