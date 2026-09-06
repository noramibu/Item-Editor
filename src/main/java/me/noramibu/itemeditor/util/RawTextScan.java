package me.noramibu.itemeditor.util;

public final class RawTextScan {
    private RawTextScan() {}

    public static int quotedEnd(String text, int start, char quote) {
        boolean escaping = false;
        for (int index = start; index < text.length(); index++) {
            char value = text.charAt(index);
            if (escaping) escaping = false;
            else if (value == '\\') escaping = true;
            else if (value == quote) return index;
        }
        return -1;
    }

    public static int appendQuoted(StringBuilder output, String text, int start) {
        int end = quotedEnd(text, start + 1, text.charAt(start));
        if (end < 0) end = text.length() - 1;
        output.append(text, start, end + 1);
        return end;
    }
}
