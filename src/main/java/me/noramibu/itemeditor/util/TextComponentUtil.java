package me.noramibu.itemeditor.util;

import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.editor.ValidationMessage;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.util.List;
import java.util.Locale;

public final class TextComponentUtil {

    private static final char SECTION_SIGN = '§';

    private TextComponentUtil() {
    }

    public static Component parseMarkup(String input) {
        MutableComponent root = Component.empty();
        StringBuilder buffer = new StringBuilder();
        Style currentStyle = Style.EMPTY;

        for (int index = 0; index < input.length(); index++) {
            char character = input.charAt(index);
            if ((character == '&' || character == SECTION_SIGN) && index + 1 < input.length()) {
                char next = input.charAt(index + 1);
                if (next == character) {
                    buffer.append(character);
                    index++;
                    continue;
                }

                if ((next == 'x' || next == 'X') && index + 13 < input.length()) {
                    StringBuilder hexBuilder = new StringBuilder(6);
                    boolean validHexSequence = true;
                    for (int offset = 0; offset < 6; offset++) {
                        int prefixIndex = index + 2 + offset * 2;
                        int digitIndex = prefixIndex + 1;
                        if (input.charAt(prefixIndex) != character) {
                            validHexSequence = false;
                            break;
                        }

                        char hexChar = input.charAt(digitIndex);
                        if (Character.digit(hexChar, 16) < 0) {
                            validHexSequence = false;
                            break;
                        }
                        hexBuilder.append(hexChar);
                    }

                    if (validHexSequence) {
                        flush(root, buffer, currentStyle);
                        currentStyle = currentStyle.withColor(TextColor.fromRgb(Integer.parseInt(hexBuilder.toString(), 16)));
                        index += 13;
                        continue;
                    }
                }

                if (next == '#' && index + 7 < input.length()) {
                    String hex = input.substring(index + 2, index + 8);
                    if (ValidationUtil.isHexColor(hex)) {
                        flush(root, buffer, currentStyle);
                        currentStyle = currentStyle.withColor(TextColor.fromRgb(Integer.parseInt(hex, 16)));
                        index += 7;
                        continue;
                    }
                }

                ChatFormatting formatting = ChatFormatting.getByCode(next);
                if (formatting != null) {
                    flush(root, buffer, currentStyle);
                    currentStyle = formatting == ChatFormatting.RESET
                            ? Style.EMPTY
                            : currentStyle.applyLegacyFormat(formatting);
                    index++;
                    continue;
                }
            }

            buffer.append(character);
        }

        flush(root, buffer, currentStyle);
        return root;
    }

    public static Component applyLineStyle(Component component, ItemEditorState.TextStyleDraft styleDraft, List<ValidationMessage> messages) {
        Style style = component.getStyle();

        if (!styleDraft.colorHex.isBlank()) {
            Integer color = ValidationUtil.parseColor(styleDraft.colorHex, ItemEditorText.str("display.lore.color_title"), messages);
            if (color != null) {
                style = style.withColor(color);
            }
        }

        if (styleDraft.bold) style = style.withBold(true);
        if (styleDraft.italic) style = style.withItalic(true);
        if (styleDraft.underlined) style = style.withUnderlined(true);
        if (styleDraft.strikethrough) style = style.withStrikethrough(true);
        if (styleDraft.obfuscated) style = style.withObfuscated(true);

        return component.copy().withStyle(style);
    }

    public static String toMarkup(Component component) {
        return toFormattedString(component, '&', false);
    }

    public static String toLegacyPaletteString(Component component) {
        return toFormattedString(component, SECTION_SIGN, true);
    }

    private static String toFormattedString(Component component, char prefix, boolean legacyPaletteOnly) {
        StringBuilder builder = new StringBuilder();
        Style previous = Style.EMPTY;

        for (Component flatComponent : component.toFlatList(Style.EMPTY)) {
            Style style = flatComponent.getStyle();
            if (!style.equals(previous)) {
                appendStyle(builder, style, prefix, legacyPaletteOnly);
                previous = style;
            }
            builder.append(escape(flatComponent.getString(), prefix));
        }

        return builder.toString();
    }

    private static void appendStyle(StringBuilder builder, Style style, char prefix, boolean legacyPaletteOnly) {
        builder.append(prefix).append('r');
        if (style.getColor() != null) {
            ChatFormatting formatting = findLegacyColor(style.getColor(), legacyPaletteOnly);
            if (formatting != null) {
                builder.append(prefix).append(formatting.getChar());
            } else if (!legacyPaletteOnly) {
                appendLegacyHex(builder, style.getColor().getValue(), prefix);
            }
        }
        if (style.isBold()) builder.append(prefix).append('l');
        if (style.isItalic()) builder.append(prefix).append('o');
        if (style.isUnderlined()) builder.append(prefix).append('n');
        if (style.isStrikethrough()) builder.append(prefix).append('m');
        if (style.isObfuscated()) builder.append(prefix).append('k');
    }

    private static ChatFormatting findLegacyColor(TextColor color, boolean approximate) {
        ChatFormatting bestFormatting = null;
        double bestDistance = Double.MAX_VALUE;

        for (ChatFormatting formatting : ChatFormatting.values()) {
            if (!formatting.isColor() || formatting.getColor() == null) {
                continue;
            }

            if (formatting.getColor() == color.getValue()) {
                return formatting;
            }

            if (approximate) {
                double distance = ColorInterpolationUtil.colorDistanceSquared(formatting.getColor(), color.getValue());
                if (distance < bestDistance) {
                    bestDistance = distance;
                    bestFormatting = formatting;
                }
            }
        }
        return approximate ? bestFormatting : null;
    }

    private static void appendLegacyHex(StringBuilder builder, int color, char prefix) {
        String hex = String.format(Locale.ROOT, "%06X", color & 0xFFFFFF);
        if (prefix == SECTION_SIGN) {
            builder.append(prefix).append('x');
            for (int index = 0; index < hex.length(); index++) {
                builder.append(prefix).append(hex.charAt(index));
            }
        } else {
            builder.append(prefix).append('#').append(hex);
        }
    }

    private static String escape(String text, char prefix) {
        String marker = Character.toString(prefix);
        return text.replace(marker, marker + marker);
    }

    private static void flush(MutableComponent root, StringBuilder buffer, Style style) {
        if (buffer.isEmpty()) return;
        root.append(Component.literal(buffer.toString()).withStyle(style));
        buffer.setLength(0);
    }
}
