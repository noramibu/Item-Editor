package me.noramibu.itemeditor.storage;

import java.util.Locale;

public final class StorageSizeText {
    private static final int UNIT_STEP = 1024;
    private static final String[] UNIT_NAMES = {"bytes", "KB", "MB", "GB", "TB"};

    private StorageSizeText() {
    }

    public static String sizeLine(int bytes) {
        bytes = Math.max(0, bytes);
        double value = bytes;
        int unitIndex = 0;
        while (value >= UNIT_STEP && unitIndex < UNIT_NAMES.length - 1) {
            value /= UNIT_STEP;
            unitIndex++;
        }

        if (unitIndex == 0) {
            return "Size: " + bytes + " bytes";
        }
        String display = Math.rint(value) == value
                ? Long.toString(Math.round(value))
                : String.format(Locale.ROOT, "%.1f", value);
        return "Size: " + display + UNIT_NAMES[unitIndex] + " (" + bytes + " bytes)";
    }
}
