package me.noramibu.itemeditor.util;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;
import java.util.regex.Pattern;

public final class HeadTextureUtil {

    private static final Pattern HASH_PATTERN = Pattern.compile("^[0-9a-fA-F]{32,64}$");

    private HeadTextureUtil() {
    }

    public static String normalizeTextureInput(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }

        String value = raw.trim();
        if (value.startsWith("{") && value.endsWith("}")) {
            return encodeBase64(value);
        }

        String hostPath = stripScheme(value);
        if (hostPath.toLowerCase(Locale.ROOT).startsWith("textures.minecraft.net/texture/")) {
            return encodeBase64(textureJson("https://" + hostPath));
        }

        if (HASH_PATTERN.matcher(value).matches()) {
            return encodeBase64(textureJson("https://textures.minecraft.net/texture/" + value.toLowerCase(Locale.ROOT)));
        }

        return value;
    }

    public static boolean isBase64(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }

        try {
            Base64.getDecoder().decode(value.trim());
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private static String textureJson(String url) {
        String escapedUrl = url.replace("\\", "\\\\").replace("\"", "\\\"");
        return "{\"textures\":{\"SKIN\":{\"url\":\"" + escapedUrl + "\"}}}";
    }

    private static String stripScheme(String value) {
        int schemeSeparator = value.indexOf("://");
        if (schemeSeparator < 0) {
            return value;
        }
        return value.substring(schemeSeparator + 3);
    }

    private static String encodeBase64(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
