package me.noramibu.itemeditor.util;

import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.editor.ValidationMessage;
import me.noramibu.itemeditor.editor.text.RichTextDocument;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.SnbtPrinterTagVisitor;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.contents.ObjectContents;
import net.minecraft.network.chat.contents.objects.AtlasSprite;
import net.minecraft.network.chat.contents.objects.PlayerSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.component.ResolvableProfile;

import java.net.URI;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class TextComponentUtil {

    private static final char SECTION_SIGN = '§';
    private static final String TOKEN_PREFIX = "[ie:";
    private static final String TOKEN_SUFFIX = "]";
    private static final String TOKEN_CLICK_OPEN = "[ie:click:";
    private static final String TOKEN_CLICK_CLOSE = "[ie:/click]";
    private static final String TOKEN_HOVER_OPEN = "[ie:hover:";
    private static final String TOKEN_HOVER_CLOSE = "[ie:/hover]";
    private static final String TOKEN_HEAD_TEXTURE_OPEN = "[ie:head_texture:";
    private static final String TOKEN_HEAD_OPEN = "[ie:head:";
    private static final String TOKEN_SPRITE_OPEN = "[ie:sprite:";

    private TextComponentUtil() {
    }

    public static Component parseMarkup(String input) {
        return parseMarkupInternal(input, true, true);
    }

    public static Component parseMarkup(String input, boolean parseEventTokens, boolean parseObjectTokens) {
        return parseMarkupInternal(input, parseEventTokens, parseObjectTokens);
    }

    private static Component parseMarkupInternal(String input, boolean parseEventTokens, boolean parseObjectTokens) {
        MutableComponent root = Component.empty();
        if (input == null || input.isEmpty()) {
            return root;
        }

        StringBuilder buffer = new StringBuilder();
        Style style = Style.EMPTY;
        ArrayDeque<ClickEvent> clickStack = new ArrayDeque<>();
        ArrayDeque<HoverEvent> hoverStack = new ArrayDeque<>();

        for (int index = 0; index < input.length(); index++) {
            int tokenEnd = tryConsumeToken(
                    input,
                    index,
                    root,
                    buffer,
                    style,
                    clickStack,
                    hoverStack,
                    parseEventTokens,
                    parseObjectTokens
            );
            if (tokenEnd >= 0) {
                index = tokenEnd;
                continue;
            }

            char character = input.charAt(index);
            if ((character == '&' || character == SECTION_SIGN) && index + 1 < input.length()) {
                char next = input.charAt(index + 1);
                if (next == character) {
                    buffer.append(character);
                    index++;
                    continue;
                }
                if (next == '$' && index + 9 < input.length()) {
                    String hex = input.substring(index + 2, index + 10);
                    if (isHexColor(hex, 8)) {
                        flushWithEvents(root, buffer, style, clickStack, hoverStack);
                        style = style.withShadowColor((int) Long.parseLong(hex, 16));
                        index += 9;
                        continue;
                    }
                }
                if ((next == 'x' || next == 'X') && index + 13 < input.length()) {
                    Integer rgb = parseLegacyHex(input, index, character);
                    if (rgb != null) {
                        flushWithEvents(root, buffer, style, clickStack, hoverStack);
                        style = style.withColor(TextColor.fromRgb(rgb));
                        index += 13;
                        continue;
                    }
                }
                if (next == '#' && index + 7 < input.length()) {
                    String hex = input.substring(index + 2, index + 8);
                    if (ValidationUtil.isHexColor(hex)) {
                        flushWithEvents(root, buffer, style, clickStack, hoverStack);
                        style = style.withColor(TextColor.fromRgb(Integer.parseInt(hex, 16)));
                        index += 7;
                        continue;
                    }
                }
                ChatFormatting formatting = ChatFormatting.getByCode(next);
                if (formatting != null) {
                    flushWithEvents(root, buffer, style, clickStack, hoverStack);
                    style = formatting == ChatFormatting.RESET ? Style.EMPTY : style.applyLegacyFormat(formatting);
                    index++;
                    continue;
                }
            }

            buffer.append(character);
        }

        flushWithEvents(root, buffer, style, clickStack, hoverStack);
        return collapseEmptyRoot(root);
    }

    public static Component applyLineStyle(Component component, ItemEditorState.TextStyleDraft styleDraft, List<ValidationMessage> messages) {
        Style style = component.getStyle();

        if (!styleDraft.colorHex.isBlank()) {
            Integer color = ValidationUtil.parseColor(styleDraft.colorHex, ItemEditorText.str("display.lore.color_title"), messages);
            if (color != null) {
                style = style.withColor(color);
            }
        }
        if (!styleDraft.shadowColorHex.isBlank()) {
            Integer color = ValidationUtil.parseColor(styleDraft.shadowColorHex, ItemEditorText.str("text.shadow_color"), messages);
            if (color != null) {
                style = style.withShadowColor(color | 0xFF000000);
            }
        }

        if (styleDraft.bold) style = style.withBold(true);
        if (styleDraft.italic) style = style.withItalic(true);
        if (styleDraft.underlined) style = style.withUnderlined(true);
        if (styleDraft.strikethrough) style = style.withStrikethrough(true);
        if (styleDraft.obfuscated) style = style.withObfuscated(true);
        return component.copy().withStyle(style);
    }

    public static Component parseStyledLine(String rawText, ItemEditorState.TextStyleDraft styleDraft, List<ValidationMessage> messages) {
        Component component = parseMarkup(rawText);
        if (containsStructuredToken(rawText) || containsFormattingCode(rawText)) {
            return component;
        }
        return applyLineStyle(component, styleDraft, messages);
    }

    public static String toMarkup(Component component) {
        return toFormattedString(component, '&', false);
    }

    public static String toLegacyPaletteString(Component component) {
        return toFormattedString(component, SECTION_SIGN, true);
    }

    public static String serializeEditorDocument(RichTextDocument document) {
        if (document == null) {
            return "";
        }
        return toMarkup(document.toComponent());
    }

    public static Component compactStyleFlags(Component component) {
        if (component == null) {
            return Component.empty();
        }
        MutableComponent copy = MutableComponent.create(component.getContents());
        copy.setStyle(compactStyle(component.getStyle()));
        for (Component sibling : component.getSiblings()) {
            copy.append(compactStyleFlags(sibling));
        }
        return copy;
    }

    public static String canonicalLoreMarkup(Component component) {
        return canonicalLoreMarkup(toMarkup(compactStyleFlags(component)));
    }

    public static String canonicalLoreMarkup(String text) {
        return Objects.requireNonNullElse(text, "");
    }

    public static boolean sameVisibleContent(Component first, Component second) {
        List<StyledChunk> firstChunks = flatten(first == null ? Component.empty() : first);
        List<StyledChunk> secondChunks = flatten(second == null ? Component.empty() : second);
        if (firstChunks.size() != secondChunks.size()) {
            return false;
        }
        for (int index = 0; index < firstChunks.size(); index++) {
            StyledChunk firstChunk = firstChunks.get(index);
            StyledChunk secondChunk = secondChunks.get(index);
            if (!Objects.equals(firstChunk.text(), secondChunk.text())) {
                return false;
            }
            if (!visibleStyle(firstChunk.style()).equals(visibleStyle(secondChunk.style()))) {
                return false;
            }
        }
        return true;
    }

    public static boolean containsStructuredToken(String input) {
        return input != null && input.contains(TOKEN_PREFIX);
    }

    public static boolean containsEventToken(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }
        return input.contains(TOKEN_CLICK_OPEN)
                || input.contains(TOKEN_CLICK_CLOSE)
                || input.contains(TOKEN_HOVER_OPEN)
                || input.contains(TOKEN_HOVER_CLOSE);
    }

    public static boolean containsFormattingCode(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }
        for (int index = 0; index < input.length(); index++) {
            int codeLength = formattingCodeLengthAt(input, index);
            if (codeLength > 0) {
                return true;
            }
        }
        return false;
    }

    public static String ensureLeadingWhiteForStructuredTokens(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        if (containsColorBaselineSensitiveToken(input)) {
            char prefix = preferredLegacyPrefix(input);
            String[] lines = input.split("\n", -1);
            StringBuilder normalized = new StringBuilder(input.length() + lines.length * 2);
            for (int index = 0; index < lines.length; index++) {
                if (index > 0) {
                    normalized.append('\n');
                }
                normalized.append(thisLineWithLeadingWhite(lines[index], prefix));
            }
            return normalized.toString();
        }
        return input;
    }

    public static int structuredTokenLengthAt(String input, int index) {
        if (missingTokenPrefixAt(input, index)) {
            return -1;
        }
        int tokenEnd = findTokenEnd(input, index + TOKEN_PREFIX.length());
        if (tokenEnd < index) {
            return -1;
        }
        return tokenEnd - index + 1;
    }

    public static int objectTokenLengthAt(String input, int index) {
        if (missingTokenPrefixAt(input, index)) {
            return -1;
        }
        int tokenEnd = findTokenEnd(input, index + TOKEN_PREFIX.length());
        if (tokenEnd < index) {
            return -1;
        }
        String token = input.substring(index + TOKEN_PREFIX.length(), tokenEnd);
        return isObjectToken(token) ? tokenEnd - index + 1 : -1;
    }

    public static int renderableTokenLengthAt(String input, int index) {
        if (missingTokenPrefixAt(input, index)) {
            return -1;
        }
        int tokenEnd = findTokenEnd(input, index + TOKEN_PREFIX.length());
        if (tokenEnd < index) {
            return -1;
        }
        String token = input.substring(index + TOKEN_PREFIX.length(), tokenEnd);
        return isRenderableToken(token) ? tokenEnd - index + 1 : -1;
    }

    public static int formattingCodeLengthAt(String input, int index) {
        if (input == null || index < 0 || index + 1 >= input.length()) {
            return -1;
        }
        char prefix = input.charAt(index);
        if (prefix != '&' && prefix != SECTION_SIGN) {
            return -1;
        }

        char code = input.charAt(index + 1);
        if (code == '$' && index + 9 < input.length() && isHexColor(input.substring(index + 2, index + 10), 8)) {
            return 10;
        }
        if (code == '#' && index + 7 < input.length() && ValidationUtil.isHexColor(input.substring(index + 2, index + 8))) {
            return 8;
        }
        if ((code == 'x' || code == 'X') && index + 13 < input.length() && parseLegacyHex(input, index, prefix) != null) {
            return 14;
        }
        return ChatFormatting.getByCode(code) != null || code == prefix ? 2 : -1;
    }

    private static boolean missingTokenPrefixAt(String input, int index) {
        return input == null || index < 0 || index >= input.length() || !input.startsWith(TOKEN_PREFIX, index);
    }

    public static String escapeStructuredTokenValue(String value) {
        return escapeTokenValue(value);
    }

    private static String thisLineWithLeadingWhite(String line, char prefix) {
        if (line == null || line.isEmpty()) {
            return Objects.requireNonNullElse(line, "");
        }
        int firstContentIndex = 0;
        while (firstContentIndex < line.length() && Character.isWhitespace(line.charAt(firstContentIndex))) {
            firstContentIndex++;
        }
        if (firstContentIndex >= line.length()) {
            return line;
        }
        if (startsWithLegacyStyleCode(line, firstContentIndex)) {
            return line;
        }
        if (!startsWithColorBaselineSensitiveToken(line, firstContentIndex)) {
            return line;
        }
        return line.substring(0, firstContentIndex) + prefix + "r" + line.substring(firstContentIndex);
    }

    private static Style compactStyle(Style style) {
        Style compact = Style.EMPTY;
        if (style.getColor() != null) {
            compact = compact.withColor(style.getColor());
        }
        if (style.getShadowColor() != null) {
            compact = compact.withShadowColor(style.getShadowColor());
        }
        if (style.getClickEvent() != null) {
            compact = compact.withClickEvent(style.getClickEvent());
        }
        if (style.getHoverEvent() != null) {
            compact = compact.withHoverEvent(style.getHoverEvent());
        }
        if (style.getInsertion() != null) {
            compact = compact.withInsertion(style.getInsertion());
        }
        if (style.isBold()) {
            compact = compact.withBold(true);
        }
        if (style.isItalic()) {
            compact = compact.withItalic(true);
        }
        if (style.isUnderlined()) {
            compact = compact.withUnderlined(true);
        }
        if (style.isStrikethrough()) {
            compact = compact.withStrikethrough(true);
        }
        if (style.isObfuscated()) {
            compact = compact.withObfuscated(true);
        }
        return compact;
    }

    private static char preferredLegacyPrefix(String input) {
        if (input.indexOf(SECTION_SIGN) >= 0) {
            return SECTION_SIGN;
        }
        if (input.indexOf('&') >= 0) {
            return '&';
        }
        return SECTION_SIGN;
    }

    private static boolean startsWithColorBaselineSensitiveToken(String input, int index) {
        if (input == null || index < 0 || index >= input.length() || !input.startsWith(TOKEN_PREFIX, index)) {
            return false;
        }
        int tokenEnd = findTokenEnd(input, index + TOKEN_PREFIX.length());
        if (tokenEnd <= index) {
            return false;
        }
        String token = input.substring(index + TOKEN_PREFIX.length(), tokenEnd);
        return token.startsWith("head:")
                || token.startsWith("head_texture:")
                || token.startsWith("sprite:");
    }

    private static boolean startsWithLegacyStyleCode(String input, int index) {
        if (index + 1 >= input.length()) {
            return false;
        }
        char marker = input.charAt(index);
        if (marker != '&' && marker != SECTION_SIGN) {
            return false;
        }
        char code = input.charAt(index + 1);
        if (ChatFormatting.getByCode(code) != null) {
            return true;
        }
        return code == '#' || code == 'x' || code == 'X';
    }

    private static String toFormattedString(Component component, char prefix, boolean legacyPaletteOnly) {
        StringBuilder out = new StringBuilder();
        Style previous = Style.EMPTY;
        ClickEvent activeClick = null;
        HoverEvent activeHover = null;
        List<StyledChunk> chunks = flatten(component);
        for (int chunkIndex = 0; chunkIndex < chunks.size(); chunkIndex++) {
            StyledChunk chunk = chunks.get(chunkIndex);
            Style style = chunk.style();
            ClickEvent rawNextClick = style.getClickEvent();
            HoverEvent rawNextHover = style.getHoverEvent();
            String nextClickOpen = openClickToken(rawNextClick);
            String nextHoverOpen = openHoverToken(rawNextHover, prefix, legacyPaletteOnly);
            ClickEvent nextClick = nextClickOpen.isBlank() ? null : rawNextClick;
            HoverEvent nextHover = nextHoverOpen.isBlank() ? null : rawNextHover;
            if (!Objects.equals(activeClick, nextClick) || !Objects.equals(activeHover, nextHover)) {
                if (activeHover != null && !Objects.equals(activeHover, nextHover)) {
                    out.append(TOKEN_HOVER_CLOSE);
                }
                if (activeClick != null && !Objects.equals(activeClick, nextClick)) {
                    out.append(TOKEN_CLICK_CLOSE);
                }
                if (nextClick != null && !Objects.equals(activeClick, nextClick)) {
                    out.append(nextClickOpen);
                }
                if (nextHover != null && !Objects.equals(activeHover, nextHover)) {
                    out.append(nextHoverOpen);
                }
                activeClick = nextClick;
                activeHover = nextHover;
            }

            Style styleNoEvents = withoutEvents(style);
            Style previousNoEvents = withoutEvents(previous);
            if (!styleNoEvents.equals(previousNoEvents)) {
                appendStyle(out, styleNoEvents, prefix, legacyPaletteOnly, previousNoEvents);
                previous = style;
            }
            appendEscapedChunkText(out, chunk.text(), prefix, activeClick, activeHover, legacyPaletteOnly);

            if (chunkIndex + 1 < chunks.size()) {
                StyledChunk nextChunk = chunks.get(chunkIndex + 1);
                if (shouldPreserveEventChunkBoundary(style, nextChunk.style(), prefix, legacyPaletteOnly)) {
                    if (activeHover != null) {
                        out.append(TOKEN_HOVER_CLOSE);
                    }
                    if (activeClick != null) {
                        out.append(TOKEN_CLICK_CLOSE);
                    }
                    appendOpenEvents(out, activeClick, activeHover, prefix, legacyPaletteOnly);
                }
            }
        }

        if (activeHover != null) out.append(TOKEN_HOVER_CLOSE);
        if (activeClick != null) out.append(TOKEN_CLICK_CLOSE);
        return out.toString();
    }

    private static boolean shouldPreserveEventChunkBoundary(
            Style current,
            Style next,
            char prefix,
            boolean legacyPaletteOnly
    ) {
        if (current == null || next == null) {
            return false;
        }
        ClickEvent currentClick = serializableClickEvent(current.getClickEvent());
        ClickEvent nextClick = serializableClickEvent(next.getClickEvent());
        HoverEvent currentHover = serializableHoverEvent(current.getHoverEvent(), prefix, legacyPaletteOnly);
        HoverEvent nextHover = serializableHoverEvent(next.getHoverEvent(), prefix, legacyPaletteOnly);
        if (!Objects.equals(currentClick, nextClick) || !Objects.equals(currentHover, nextHover)) {
            return false;
        }
        if (currentClick == null && currentHover == null) {
            return false;
        }
        return withoutEvents(current).equals(withoutEvents(next));
    }

    private static Integer parseLegacyHex(String input, int index, char prefix) {
        StringBuilder hexBuilder = new StringBuilder(6);
        for (int offset = 0; offset < 6; offset++) {
            int prefixIndex = index + 2 + offset * 2;
            int digitIndex = prefixIndex + 1;
            if (input.charAt(prefixIndex) != prefix) {
                return null;
            }
            char hexChar = input.charAt(digitIndex);
            if (Character.digit(hexChar, 16) < 0) {
                return null;
            }
            hexBuilder.append(hexChar);
        }
        return Integer.parseInt(hexBuilder.toString(), 16);
    }

    private static boolean isHexColor(String value, int length) {
        if (value == null || value.length() != length) {
            return false;
        }
        for (int index = 0; index < value.length(); index++) {
            if (Character.digit(value.charAt(index), 16) < 0) {
                return false;
            }
        }
        return true;
    }

    private static int tryConsumeToken(
            String input,
            int index,
            MutableComponent root,
            StringBuilder buffer,
            Style currentStyle,
            ArrayDeque<ClickEvent> clickStack,
            ArrayDeque<HoverEvent> hoverStack,
            boolean parseEventTokens,
            boolean parseObjectTokens
    ) {
        if (!input.startsWith(TOKEN_PREFIX, index)) {
            return -1;
        }
        int tokenEnd = findTokenEnd(input, index + TOKEN_PREFIX.length());
        if (tokenEnd <= index) {
            return -1;
        }

        String token = input.substring(index + TOKEN_PREFIX.length(), tokenEnd);
        if (parseEventTokens) {
            if ("/click".equals(token)) {
                if (!clickStack.isEmpty()) {
                    flushWithEvents(root, buffer, currentStyle, clickStack, hoverStack);
                    clickStack.removeLast();
                }
                return tokenEnd;
            }
            if ("/hover".equals(token)) {
                if (!hoverStack.isEmpty()) {
                    flushWithEvents(root, buffer, currentStyle, clickStack, hoverStack);
                    hoverStack.removeLast();
                }
                return tokenEnd;
            }
            if (token.startsWith("click:")) {
                ClickEvent clickEvent = parseClickToken(token.substring("click:".length()));
                if (clickEvent != null) {
                    flushWithEvents(root, buffer, currentStyle, clickStack, hoverStack);
                    clickStack.addLast(clickEvent);
                }
                return tokenEnd;
            }
            if (token.startsWith("hover:")) {
                HoverEvent hoverEvent = parseHoverToken(token.substring("hover:".length()));
                if (hoverEvent != null) {
                    flushWithEvents(root, buffer, currentStyle, clickStack, hoverStack);
                    hoverStack.addLast(hoverEvent);
                }
                return tokenEnd;
            }
        }
        if (parseObjectTokens) {
            ObjectToken object = parseObjectToken(token);
            if (object != null) {
                flushWithEvents(root, buffer, currentStyle, clickStack, hoverStack);
                Style objectRenderStyle = styleWithEvents(currentStyle, clickStack, hoverStack);
                if (object.color() != null) {
                    objectRenderStyle = objectRenderStyle.withColor(TextColor.fromRgb(object.color()));
                }
                root.append(object.component().copy().withStyle(objectStyle(objectRenderStyle)));
                return tokenEnd;
            }
        }
        return -1;
    }

    private static ObjectToken parseObjectToken(String token) {
        if (token.startsWith("head:")) {
            return parseHeadToken(token.substring("head:".length()));
        }
        if (token.startsWith("head_texture:")) {
            return parseHeadTextureToken(token.substring("head_texture:".length()));
        }
        if (token.startsWith("sprite:")) {
            return parseSpriteToken(token.substring("sprite:".length()));
        }
        return null;
    }

    private static int findTokenEnd(String input, int contentStart) {
        boolean escaped = false;
        for (int i = contentStart; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '\n' || c == '\r') {
                return -1;
            }
            if (escaped) {
                escaped = false;
                continue;
            }
            if (c == '\\') {
                escaped = true;
                continue;
            }
            if (c == ']') return i;
        }
        return -1;
    }

    private static boolean containsColorBaselineSensitiveToken(String input) {
        if (input == null || input.isEmpty() || !containsStructuredToken(input)) {
            return false;
        }
        for (int index = 0; index < input.length(); index++) {
            if (!input.startsWith(TOKEN_PREFIX, index)) {
                continue;
            }
            int tokenEnd = findTokenEnd(input, index + TOKEN_PREFIX.length());
            if (tokenEnd <= index) {
                continue;
            }
            String token = input.substring(index + TOKEN_PREFIX.length(), tokenEnd);
            if (isObjectToken(token)) {
                return true;
            }
            index = tokenEnd;
        }
        return false;
    }

    private static Component collapseEmptyRoot(MutableComponent root) {
        if (!Style.EMPTY.equals(root.getStyle())
                || root.getSiblings().size() != 1
                || !isContentsEmpty(root.getContents())) {
            return root;
        }
        return root.getSiblings().getFirst().copy();
    }

    private static boolean isContentsEmpty(ComponentContents contents) {
        return contents.visit(chunk -> chunk.isEmpty() ? Optional.empty() : Optional.of(Boolean.TRUE)).isEmpty();
    }

    private static boolean isObjectToken(String token) {
        return token.startsWith("head:")
                || token.startsWith("head_texture:")
                || token.startsWith("sprite:");
    }

    private static boolean isRenderableToken(String token) {
        return isObjectToken(token)
                || "/click".equals(token)
                || "/hover".equals(token)
                || token.startsWith("click:")
                || token.startsWith("hover:");
    }

    private static ClickEvent parseClickToken(String payload) {
        int separator = payload.indexOf(':');
        if (separator < 0) return null;
        String action = payload.substring(0, separator).trim().toLowerCase(Locale.ROOT);
        String rawValue = payload.substring(separator + 1);

        try {
            return switch (action) {
                case "open_url" -> {
                    String value = unescapeTokenValue(rawValue);
                    if (value.isBlank()) {
                        yield null;
                    }
                    yield new ClickEvent.OpenUrl(URI.create(value));
                }
                case "run_command" -> {
                    String value = unescapeTokenValue(rawValue);
                    if (value.isBlank()) {
                        yield null;
                    }
                    yield new ClickEvent.RunCommand(value);
                }
                case "suggest_command" -> {
                    String value = unescapeTokenValue(rawValue);
                    if (value.isBlank()) {
                        yield null;
                    }
                    yield new ClickEvent.SuggestCommand(value);
                }
                case "copy_to_clipboard" -> {
                    String value = unescapeTokenValue(rawValue);
                    if (value.isBlank()) {
                        yield null;
                    }
                    yield new ClickEvent.CopyToClipboard(value);
                }
                case "change_page" -> {
                    String value = unescapeTokenValue(rawValue);
                    if (value.isBlank()) {
                        yield null;
                    }
                    yield new ClickEvent.ChangePage(Integer.parseInt(value.trim()));
                }
                case "custom" -> parseCustomClickToken(rawValue);
                default -> null;
            };
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static HoverEvent parseHoverToken(String payload) {
        int separator = payload.indexOf(':');
        if (separator < 0) {
            String value = unescapeTokenValue(payload);
            return value.isBlank() ? null : new HoverEvent.ShowText(parseMarkup(value));
        }
        String action = payload.substring(0, separator).trim().toLowerCase(Locale.ROOT);
        String rawValue = payload.substring(separator + 1);
        try {
            return switch (action) {
                case "show_text" -> {
                    String value = unescapeTokenValue(rawValue);
                    yield value.isBlank() ? null : new HoverEvent.ShowText(parseMarkup(value));
                }
                case "show_item" -> parseHoverShowItemToken(rawValue);
                case "show_entity" -> parseHoverShowEntityToken(rawValue);
                default -> null;
            };
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static ObjectToken parseHeadToken(String payload) {
        ObjectColorSplit split = splitObjectColor(payload);
        String[] parts = split.payload().split(":", 2);
        String playerName = parts.length > 0 ? unescapeTokenValue(parts[0]).trim() : "";
        if (playerName.isBlank()) return null;
        boolean hat = parts.length > 1 && "true".equalsIgnoreCase(unescapeTokenValue(parts[1]).trim());
        return new ObjectToken(
                Component.object(new PlayerSprite(ResolvableProfile.createUnresolved(playerName), hat)),
                split.color()
        );
    }

    private static ObjectToken parseHeadTextureToken(String payload) {
        ObjectColorSplit split = splitObjectColor(payload);
        int hatSeparator = split.payload().lastIndexOf(':');
        if (hatSeparator <= 0 || hatSeparator >= split.payload().length() - 1) {
            return null;
        }

        String body = split.payload().substring(0, hatSeparator);
        String hatRaw = unescapeTokenValue(split.payload().substring(hatSeparator + 1).trim());
        int textureSeparator = body.indexOf(':');
        if (textureSeparator < 0) {
            return null;
        }

        String textureValue = unescapeTokenValue(body.substring(0, textureSeparator).trim());
        String textureSignature = unescapeTokenValue(body.substring(textureSeparator + 1).trim());
        if (textureValue.isBlank()) {
            return null;
        }

        boolean hat = "true".equalsIgnoreCase(hatRaw);
        return new ObjectToken(
                Component.object(new PlayerSprite(profileFromTextures(textureValue, textureSignature), hat)),
                split.color()
        );
    }

    private static ObjectToken parseSpriteToken(String payload) {
        ObjectColorSplit split = splitObjectColor(payload);
        String raw = Objects.requireNonNullElse(split.payload(), "").trim();
        if (raw.isEmpty()) {
            return null;
        }

        int pipeSeparator = raw.indexOf('|');
        if (pipeSeparator <= 0 || pipeSeparator >= raw.length() - 1) {
            return null;
        }

        Identifier atlas = Identifier.tryParse(unescapeTokenValue(raw.substring(0, pipeSeparator)).trim());
        Identifier sprite = Identifier.tryParse(unescapeTokenValue(raw.substring(pipeSeparator + 1)).trim());
        if (atlas == null || sprite == null) return null;
        return new ObjectToken(
                Component.object(new AtlasSprite(atlas, sprite)),
                split.color()
        );
    }

    private static String openClickToken(ClickEvent clickEvent) {
        if (clickEvent == null) {
            return "";
        }
        return switch (clickEvent) {
            case ClickEvent.OpenUrl(var uri) ->
                    TOKEN_CLICK_OPEN + "open_url:" + escapeTokenValue(uri.toString()) + TOKEN_SUFFIX;
            case ClickEvent.RunCommand(var command) ->
                    TOKEN_CLICK_OPEN + "run_command:" + escapeTokenValue(command) + TOKEN_SUFFIX;
            case ClickEvent.SuggestCommand(var command) ->
                    TOKEN_CLICK_OPEN + "suggest_command:" + escapeTokenValue(command) + TOKEN_SUFFIX;
            case ClickEvent.CopyToClipboard(var value) ->
                    TOKEN_CLICK_OPEN + "copy_to_clipboard:" + escapeTokenValue(value) + TOKEN_SUFFIX;
            case ClickEvent.ChangePage(var page) ->
                    TOKEN_CLICK_OPEN + "change_page:" + page + TOKEN_SUFFIX;
            case ClickEvent.Custom(var id, var payload) -> {
                StringBuilder customPayload = new StringBuilder(escapeEventField(id.toString()));
                payload.map(TextComponentUtil::serializeTagPayload)
                        .filter(text -> !text.isBlank())
                        .ifPresent(text -> customPayload.append('|').append(escapeEventField(text)));
                yield TOKEN_CLICK_OPEN + "custom:" + customPayload + TOKEN_SUFFIX;
            }
            default -> "";
        };
    }

    private static String openHoverToken(HoverEvent hoverEvent, char prefix, boolean legacyPaletteOnly) {
        if (hoverEvent == null) {
            return "";
        }
        return switch (hoverEvent) {
            case HoverEvent.ShowText(var component) -> {
                String value = toFormattedString(component, prefix, legacyPaletteOnly);
                yield TOKEN_HOVER_OPEN + "show_text:" + escapeTokenValue(value) + TOKEN_SUFFIX;
            }
            case HoverEvent.ShowItem(var itemTemplate) -> {
                Identifier itemId = BuiltInRegistries.ITEM.getKey(itemTemplate.item().value());
                StringBuilder encoded = new StringBuilder(escapeEventField(itemId.toString()));
                if (itemTemplate.count() != 1) {
                    encoded.append('|').append(itemTemplate.count());
                }
                yield TOKEN_HOVER_OPEN + "show_item:" + encoded + TOKEN_SUFFIX;
            }
            case HoverEvent.ShowEntity(var entityInfo) -> {
                Identifier entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entityInfo.type);
                StringBuilder encoded = new StringBuilder()
                        .append(escapeEventField(entityId.toString()))
                        .append('|')
                        .append(escapeEventField(entityInfo.uuid.toString()));
                entityInfo.name
                        .map(component -> toFormattedString(component, prefix, legacyPaletteOnly))
                        .filter(text -> !text.isBlank())
                        .ifPresent(text -> encoded.append('|').append(escapeEventField(text)));
                yield TOKEN_HOVER_OPEN + "show_entity:" + encoded + TOKEN_SUFFIX;
            }
            default -> "";
        };
    }

    private static ClickEvent parseCustomClickToken(String rawValue) {
        List<String> fields = splitEventFields(rawValue, 2);
        if (fields.isEmpty()) {
            return null;
        }
        String idValue = unescapeTokenValue(fields.getFirst()).trim();
        Identifier id = Identifier.tryParse(idValue);
        if (id == null) {
            return null;
        }

        Optional<Tag> payload = Optional.empty();
        if (fields.size() > 1) {
            String rawPayload = unescapeTokenValue(fields.get(1)).trim();
            payload = parseCustomPayload(rawPayload);
        }
        return new ClickEvent.Custom(id, payload);
    }

    private static HoverEvent parseHoverShowItemToken(String rawValue) {
        List<String> fields = splitEventFields(rawValue, 3);
        if (fields.isEmpty()) {
            return null;
        }
        String itemIdValue = unescapeTokenValue(fields.getFirst()).trim();
        Identifier itemId = parseIdentifierOrNull(itemIdValue);
        if (itemId == null) {
            return null;
        }
        Item item = BuiltInRegistries.ITEM.getOptional(itemId).orElse(null);
        if (item == null) {
            return null;
        }

        int count = 1;
        if (fields.size() > 1) {
            String countValue = unescapeTokenValue(fields.get(1)).trim();
            if (!countValue.isBlank()) {
                count = Integer.parseInt(countValue);
                if (count <= 0) {
                    return null;
                }
            }
        }
        return new HoverEvent.ShowItem(new ItemStackTemplate(item, count));
    }

    private static HoverEvent parseHoverShowEntityToken(String rawValue) {
        List<String> fields = splitEventFields(rawValue, 3);
        if (fields.size() < 2) {
            return null;
        }
        String entityIdValue = unescapeTokenValue(fields.getFirst()).trim();
        Identifier entityId = parseIdentifierOrNull(entityIdValue);
        if (entityId == null) {
            return null;
        }
        EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.getOptional(entityId).orElse(null);
        if (entityType == null) {
            return null;
        }

        String uuidValue = unescapeTokenValue(fields.get(1)).trim();
        if (uuidValue.isBlank()) {
            return null;
        }
        UUID uuid = UUID.fromString(uuidValue);
        Optional<Component> name = Optional.empty();
        if (fields.size() > 2) {
            String nameValue = unescapeTokenValue(fields.get(2));
            if (!nameValue.isBlank()) {
                name = Optional.of(parseMarkup(nameValue));
            }
        }
        return new HoverEvent.ShowEntity(new HoverEvent.EntityTooltipInfo(entityType, uuid, name));
    }

    private static Optional<Tag> parseCustomPayload(String rawPayload) {
        if (rawPayload.isBlank()) {
            return Optional.empty();
        }
        try {
            Tag tag = TagParser.create(NbtOps.INSTANCE).parseFully(rawPayload);
            return Optional.of(tag);
        } catch (Exception ignored) {
            return Optional.of(StringTag.valueOf(rawPayload));
        }
    }

    private static String serializeTagPayload(Tag tag) {
        if (tag == null) {
            return "";
        }
        return new SnbtPrinterTagVisitor().visit(tag);
    }

    private static String escapeEventField(String value) {
        return escapeTokenValue(value).replace("|", "\\|");
    }

    private static List<String> splitEventFields(String rawValue, int maxParts) {
        String value = Objects.requireNonNullElse(rawValue, "");
        if (maxParts <= 1) {
            return List.of(value);
        }
        List<String> fields = new ArrayList<>(maxParts);
        int fieldStart = 0;
        for (int index = 0; index < value.length(); index++) {
            if (value.charAt(index) != '|'
                    || isEscapedSeparator(value, index)
                    || fields.size() + 1 >= maxParts) {
                continue;
            }
            fields.add(value.substring(fieldStart, index));
            fieldStart = index + 1;
        }
        fields.add(value.substring(fieldStart));
        return fields;
    }

    private static boolean isEscapedSeparator(String value, int separatorIndex) {
        int backslashCount = 0;
        for (int index = separatorIndex - 1; index >= 0 && value.charAt(index) == '\\'; index--) {
            backslashCount++;
        }
        return (backslashCount & 1) == 1;
    }

    private static Identifier parseIdentifierOrNull(String value) {
        try {
            return Identifier.parse(value);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static String objectToken(ObjectContents objectContents, TextColor color) {
        String colorSuffix = color == null ? "" : "|" + escapeTokenValue(ValidationUtil.toHex(color.getValue()));
        return switch (objectContents.contents()) {
            case PlayerSprite(var player, var hat) -> {
                ProfileTextures textures = extractProfileTextures(player);
                if (textures != null) {
                    yield TOKEN_HEAD_TEXTURE_OPEN
                            + escapeTokenValue(textures.value())
                            + ":"
                            + escapeTokenValue(textures.signature())
                            + ":"
                            + (hat ? "true" : "false")
                            + colorSuffix
                            + TOKEN_SUFFIX;
                }
                String name = player.name().orElseGet(() -> {
                    String fallback = player.partialProfile().name();
                    return Objects.requireNonNullElse(fallback, "");
                });
                if (!name.isBlank()) {
                    yield TOKEN_HEAD_OPEN
                            + escapeTokenValue(name)
                            + ":"
                            + (hat ? "true" : "false")
                            + colorSuffix
                            + TOKEN_SUFFIX;
                }
                yield objectContents.fallback().map(Component::getString).orElse("");
            }
            case AtlasSprite(var atlas, var sprite) -> TOKEN_SPRITE_OPEN
                    + escapeTokenValue(atlas.toString())
                    + "|"
                    + escapeTokenValue(sprite.toString())
                    + colorSuffix
                    + TOKEN_SUFFIX;
            default -> objectContents.fallback().map(Component::getString).orElse("");
        };
    }

    private static ProfileTextures extractProfileTextures(ResolvableProfile profile) {
        if (profile == null) {
            return null;
        }
        return profile.partialProfile().properties().get("textures").stream().findFirst()
                .map(property -> new ProfileTextures(
                        Objects.toString(property.value(), ""),
                        Objects.toString(property.signature(), "")
                ))
                .filter(textures -> !textures.value().isBlank())
                .orElse(null);
    }

    private static ResolvableProfile profileFromTextures(String textureValue, String textureSignature) {
        CompoundTag profileTag = new CompoundTag();
        ListTag propertyTags = new ListTag();
        CompoundTag texture = new CompoundTag();
        texture.putString("name", "textures");
        texture.putString("value", textureValue);
        if (!textureSignature.isBlank()) {
            texture.putString("signature", textureSignature);
        }
        propertyTags.add(texture);
        profileTag.put("properties", propertyTags);

        return ResolvableProfile.CODEC.parse(NbtOps.INSTANCE, profileTag).result().orElseThrow();
    }

    private static List<StyledChunk> flatten(Component component) {
        List<StyledChunk> chunks = new ArrayList<>();
        appendChunks(component, Style.EMPTY, chunks);
        return chunks;
    }

    private static void appendChunks(Component component, Style parentStyle, List<StyledChunk> out) {
        Style effective = component.getStyle().applyTo(parentStyle);
        var contents = component.getContents();
        if (contents instanceof ObjectContents objectContents) {
            String token = objectToken(objectContents, effective.getColor());
            if (!token.isEmpty()) {
                out.add(new StyledChunk(token, objectTokenStyle(effective)));
            }
        } else {
            String text = textFromContents(contents);
            if (!text.isEmpty()) {
                out.add(new StyledChunk(text, effective));
            }
        }
        for (Component sibling : component.getSiblings()) {
            appendChunks(sibling, effective, out);
        }
    }

    private static String textFromContents(ComponentContents contents) {
        StringBuilder text = new StringBuilder();
        contents.visit((String chunk) -> {
            text.append(chunk);
            return Optional.empty();
        });
        return text.toString();
    }

    private static ObjectColorSplit splitObjectColor(String payload) {
        String raw = Objects.requireNonNullElse(payload, "");
        int colorSeparator = raw.lastIndexOf('|');
        if (colorSeparator <= 0 || colorSeparator >= raw.length() - 1) {
            return new ObjectColorSplit(raw, null);
        }
        String colorRaw = unescapeTokenValue(raw.substring(colorSeparator + 1)).trim();
        Integer color = ValidationUtil.tryParseHexColor(colorRaw);
        if (color == null) {
            return new ObjectColorSplit(raw, null);
        }
        return new ObjectColorSplit(raw.substring(0, colorSeparator), color);
    }

    private static ClickEvent serializableClickEvent(ClickEvent clickEvent) {
        if (clickEvent == null) {
            return null;
        }
        return openClickToken(clickEvent).isBlank() ? null : clickEvent;
    }

    private static HoverEvent serializableHoverEvent(HoverEvent hoverEvent, char prefix, boolean legacyPaletteOnly) {
        if (hoverEvent == null) {
            return null;
        }
        return openHoverToken(hoverEvent, prefix, legacyPaletteOnly).isBlank() ? null : hoverEvent;
    }

    private static Style withoutEvents(Style style) {
        return style.withClickEvent(null).withHoverEvent(null);
    }

    private static Style styleWithEvents(Style style, ArrayDeque<ClickEvent> clickStack, ArrayDeque<HoverEvent> hoverStack) {
        ClickEvent clickEvent = clickStack.peekLast();
        HoverEvent hoverEvent = hoverStack.peekLast();
        if (clickEvent != null) style = style.withClickEvent(clickEvent);
        if (hoverEvent != null) style = style.withHoverEvent(hoverEvent);
        return style;
    }

    private static void flushWithEvents(
            MutableComponent root,
            StringBuilder buffer,
            Style style,
            ArrayDeque<ClickEvent> clickStack,
            ArrayDeque<HoverEvent> hoverStack
    ) {
        flush(root, buffer, styleWithEvents(style, clickStack, hoverStack));
    }

    private static Style objectStyle(Style style) {
        Style objectStyle = Style.EMPTY;
        if (style.getColor() != null) {
            objectStyle = objectStyle.withColor(style.getColor());
        }
        if (style.getShadowColor() != null) {
            objectStyle = objectStyle.withShadowColor(style.getShadowColor());
        }
        if (style.getClickEvent() != null) {
            objectStyle = objectStyle.withClickEvent(style.getClickEvent());
        }
        if (style.getHoverEvent() != null) {
            objectStyle = objectStyle.withHoverEvent(style.getHoverEvent());
        }
        return copyTrueDecorations(style, objectStyle);
    }

    private static Style objectTokenStyle(Style style) {
        Style tokenStyle = Style.EMPTY;
        if (style.getShadowColor() != null) {
            tokenStyle = tokenStyle.withShadowColor(style.getShadowColor());
        }
        if (style.getClickEvent() != null) {
            tokenStyle = tokenStyle.withClickEvent(style.getClickEvent());
        }
        if (style.getHoverEvent() != null) {
            tokenStyle = tokenStyle.withHoverEvent(style.getHoverEvent());
        }
        return copyTrueDecorations(style, tokenStyle);
    }

    private static Style copyTrueDecorations(Style source, Style target) {
        if (source.isBold()) {
            target = target.withBold(true);
        }
        if (source.isItalic()) {
            target = target.withItalic(true);
        }
        if (source.isUnderlined()) {
            target = target.withUnderlined(true);
        }
        if (source.isStrikethrough()) {
            target = target.withStrikethrough(true);
        }
        if (source.isObfuscated()) {
            target = target.withObfuscated(true);
        }
        return target;
    }

    private static void appendStyle(StringBuilder builder, Style style, char prefix, boolean legacyPaletteOnly, Style previousStyle) {
        if (requiresStyleReset(previousStyle, style)) {
            builder.append(prefix).append('r');
        }
        if (style.getColor() != null) {
            ChatFormatting formatting = findLegacyColor(style.getColor(), legacyPaletteOnly);
            if (formatting != null) {
                builder.append(prefix).append(formatting.getChar());
            } else if (!legacyPaletteOnly) {
                appendLegacyHex(builder, style.getColor().getValue(), prefix);
            }
        }
        if (!legacyPaletteOnly && style.getShadowColor() != null) {
            builder.append(prefix).append('$').append(String.format(Locale.ROOT, "%08X", style.getShadowColor()));
        }
        if (style.isBold()) builder.append(prefix).append('l');
        if (style.isItalic()) builder.append(prefix).append('o');
        if (style.isUnderlined()) builder.append(prefix).append('n');
        if (style.isStrikethrough()) builder.append(prefix).append('m');
        if (style.isObfuscated()) builder.append(prefix).append('k');
    }

    private static boolean requiresStyleReset(Style previousStyle, Style nextStyle) {
        if (isLegacyStyleEmpty(previousStyle)) {
            return false;
        }
        if (previousStyle.isBold() && !nextStyle.isBold()) {
            return true;
        }
        if (previousStyle.isItalic() && !nextStyle.isItalic()) {
            return true;
        }
        if (previousStyle.isUnderlined() && !nextStyle.isUnderlined()) {
            return true;
        }
        if (previousStyle.isStrikethrough() && !nextStyle.isStrikethrough()) {
            return true;
        }
        if (previousStyle.isObfuscated() && !nextStyle.isObfuscated()) {
            return true;
        }
        return previousStyle.getColor() != null && nextStyle.getColor() == null
                || previousStyle.getShadowColor() != null && nextStyle.getShadowColor() == null;
    }

    private static boolean isLegacyStyleEmpty(Style style) {
        return style.getColor() == null
                && style.getShadowColor() == null
                && !style.isBold()
                && !style.isItalic()
                && !style.isUnderlined()
                && !style.isStrikethrough()
                && !style.isObfuscated();
    }

    private static ChatFormatting findLegacyColor(TextColor color, boolean approximate) {
        ChatFormatting best = null;
        double bestDistance = Double.MAX_VALUE;
        for (ChatFormatting formatting : ChatFormatting.values()) {
            if (!formatting.isColor() || formatting.getColor() == null) continue;
            if (formatting.getColor() == color.getValue()) return formatting;
            if (approximate) {
                double distance = ColorInterpolationUtil.colorDistanceSquared(formatting.getColor(), color.getValue());
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = formatting;
                }
            }
        }
        return approximate ? best : null;
    }

    private static void appendLegacyHex(StringBuilder builder, int color, char prefix) {
        String hex = String.format(Locale.ROOT, "%06X", color & 0xFFFFFF);
        if (prefix == SECTION_SIGN) {
            builder.append(prefix).append('x');
            for (int i = 0; i < hex.length(); i++) builder.append(prefix).append(hex.charAt(i));
        } else {
            builder.append(prefix).append('#').append(hex);
        }
    }

    private static void appendEscapedChunkText(
            StringBuilder out,
            String text,
            char prefix,
            ClickEvent activeClick,
            HoverEvent activeHover,
            boolean legacyPaletteOnly
    ) {
        if (text.isEmpty()) {
            return;
        }

        int segmentStart = 0;
        for (int index = 0; index < text.length(); index++) {
            if (text.charAt(index) != '\n') {
                continue;
            }

            if (index > segmentStart) {
                out.append(escape(text.substring(segmentStart, index), prefix));
            }
            if (activeHover != null) {
                out.append(TOKEN_HOVER_CLOSE);
            }
            if (activeClick != null) {
                out.append(TOKEN_CLICK_CLOSE);
            }
            out.append('\n');
            appendOpenEvents(out, activeClick, activeHover, prefix, legacyPaletteOnly);
            segmentStart = index + 1;
        }

        if (segmentStart < text.length()) {
            out.append(escape(text.substring(segmentStart), prefix));
        }
    }

    private static void appendOpenEvents(
            StringBuilder out,
            ClickEvent activeClick,
            HoverEvent activeHover,
            char prefix,
            boolean legacyPaletteOnly
    ) {
        if (activeClick != null) {
            String openClick = openClickToken(activeClick);
            if (!openClick.isBlank()) {
                out.append(openClick);
            }
        }
        if (activeHover != null) {
            String openHover = openHoverToken(activeHover, prefix, legacyPaletteOnly);
            if (!openHover.isBlank()) {
                out.append(openHover);
            }
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

    private static String escapeTokenValue(String value) {
        if (value == null || value.isEmpty()) return "";
        return value.replace("\\", "\\\\").replace("]", "\\]").replace("[", "\\[");
    }

    private static String unescapeTokenValue(String value) {
        if (value == null || value.isEmpty()) return "";
        StringBuilder out = new StringBuilder(value.length());
        boolean escaped = false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (escaped) {
                out.append(c);
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else {
                out.append(c);
            }
        }
        if (escaped) out.append('\\');
        return out.toString();
    }

    private record StyledChunk(String text, Style style) {
    }

    private static VisibleStyle visibleStyle(Style style) {
        return new VisibleStyle(
                style.getColor() == null ? null : style.getColor().getValue(),
                style.getShadowColor(),
                style.isBold(),
                style.isItalic(),
                style.isUnderlined(),
                style.isStrikethrough(),
                style.isObfuscated(),
                serializableClickEvent(style.getClickEvent()),
                serializableHoverEvent(style.getHoverEvent(), '&', false)
        );
    }

    private record VisibleStyle(
            Integer color,
            Integer shadowColor,
            boolean bold,
            boolean italic,
            boolean underlined,
            boolean strikethrough,
            boolean obfuscated,
            ClickEvent clickEvent,
            HoverEvent hoverEvent
    ) {
    }

    private record ProfileTextures(String value, String signature) {
    }

    private record ObjectColorSplit(String payload, Integer color) {
    }

    private record ObjectToken(Component component, Integer color) {
    }
}
