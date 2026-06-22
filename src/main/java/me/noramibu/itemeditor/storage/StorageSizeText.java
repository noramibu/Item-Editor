package me.noramibu.itemeditor.storage;

import java.util.Locale;

public final class StorageSizeText {
    private static final int UNIT_STEP = 1024;
    private static final int MAX_UNIT_INDEX = 4;

    private StorageSizeText() {
    }

    public static String sizeLine(int bytes) {
        int safeBytes = Math.max(0, bytes);
        return "Size: " + readableSize(safeBytes);
    }

    private static String readableSize(int bytes) {
        double value = bytes;
        int unitIndex = 0;
        while (value >= UNIT_STEP && unitIndex < MAX_UNIT_INDEX) {
            value /= UNIT_STEP;
            unitIndex++;
        }

        if (unitIndex == 0) {
            return bytes + " bytes";
        }
        return formatDecimal(value) + unitName(unitIndex) + " (" + bytes + " bytes)";
    }

    private static String formatDecimal(double value) {
        if (Math.rint(value) == value) {
            return Long.toString(Math.round(value));
        }
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private static String unitName(int unitIndex) {
        return switch (unitIndex) {
            case 1 -> "KB";
            case 2 -> "MB";
            case 3 -> "GB";
            case 4 -> "TB";
            default -> "bytes";
        };
    }
}
