package me.noramibu.itemeditor.ui.component.raw;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class RawSyntaxHighlighter {
    private static final int COLOR_TEXT = 0xFFE7ECF3;
    private static final int COLOR_LITERAL = 0xFFF7A86D;
    private static final int COLOR_PUNCT = 0xFFBFA6FF;
    private static final int COLOR_IDENTIFIER = 0xFF93C5FD;
    private static final int COLOR_BOOLEAN = 0xFFF9A8D4;
    private static final int COLOR_NULL = 0xFFFDA4AF;
    private static final int COLOR_OPERATOR = 0xFF9CA3AF;
    private static final int[] KEY_DEPTH_COLORS = {
            0xFF7EC8F8,
            0xFF67E8F9,
            0xFF93C5FD,
            0xFFA5B4FC,
            0xFFC4B5FD,
            0xFF6EE7B7
    };
    private static final int[] BRACKET_DEPTH_COLORS = {
            0xFFC4B5FD,
            0xFF93C5FD,
            0xFFA7F3D0,
            0xFF67E8F9,
            0xFFFDE68A,
            0xFFF9A8D4
    };

    private final Map<Integer, SyntaxLineCache> cache = new HashMap<>();

    public void clear() {
        this.cache.clear();
    }

    public List<SyntaxSpan> spansForLine(int lineIndex, int lineStartOffset, String line, int baseDepth) {
        String safeLine = line == null ? "" : line;
        SyntaxLineCache cached = this.cache.get(lineIndex);
        if (cached != null && cached.matches(lineStartOffset, safeLine, baseDepth)) {
            return cached.spans();
        }
        List<SyntaxSpan> spans = this.computeSpans(lineStartOffset, safeLine, baseDepth);
        this.cache.put(lineIndex, new SyntaxLineCache(lineStartOffset, safeLine, baseDepth, spans));
        return spans;
    }

    public int[] lineDepthStarts(String text, int[] lineStarts, int maxChars) {
        String safeText = text == null ? "" : text;
        if (safeText.length() > maxChars) {
            return null;
        }
        int[] starts = lineStarts == null || lineStarts.length == 0 ? new int[]{0} : lineStarts;
        int[] depths = new int[starts.length];
        int depth = 0;
        int line = 0;
        boolean inString = false;
        boolean escaping = false;
        char quote = '"';
        depths[0] = 0;

        for (int index = 0; index < safeText.length(); index++) {
            char value = safeText.charAt(index);
            if (value == '\n') {
                line = Math.min(line + 1, starts.length - 1);
                depths[line] = Math.max(0, depth);
                continue;
            }

            if (inString) {
                if (escaping) {
                    escaping = false;
                    continue;
                }
                if (value == '\\') {
                    escaping = true;
                    continue;
                }
                if (value == quote) {
                    inString = false;
                }
                continue;
            }

            if (value == '"' || value == '\'') {
                inString = true;
                quote = value;
                continue;
            }
            if (value == '{' || value == '[') {
                depth++;
                continue;
            }
            if (value == '}' || value == ']') {
                depth = Math.max(0, depth - 1);
            }
        }
        return depths;
    }

    private Integer hexColorFromToken(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        if (token.length() >= 2) {
            char first = token.charAt(0);
            char last = token.charAt(token.length() - 1);
            if ((first == '"' || first == '\'') && first == last) {
                String inner = token.substring(1, token.length() - 1);
                if (inner.indexOf('\\') >= 0) {
                    return null;
                }
                return this.parseHexColorLiteral(inner);
            }
        }
        return this.parseHexColorLiteral(token);
    }

    private boolean isQuotedLiteralToken(String token) {
        if (token == null || token.length() < 2) {
            return false;
        }
        char first = token.charAt(0);
        char last = token.charAt(token.length() - 1);
        return (first == '"' || first == '\'') && first == last;
    }

    private List<SyntaxSpan> computeSpans(int lineStartOffset, String line, int baseDepth) {
        List<SyntaxSpan> spans = new ArrayList<>();
        int cursor = 0;
        int depth = Math.max(0, baseDepth);
        while (cursor < line.length()) {
            char value = line.charAt(cursor);
            int next;

            if (Character.isWhitespace(value)) {
                next = this.scanWhitespace(line, cursor + 1);
                this.addSpan(spans, lineStartOffset, cursor, next, SyntaxKind.PLAIN, COLOR_TEXT);
                cursor = next;
                continue;
            }

            if (value == '"' || value == '\'') {
                int end = this.findStringEnd(line, cursor + 1, value);
                next = end < 0 ? line.length() : end + 1;
                String token = line.substring(cursor, next);
                boolean keyToken = this.isKeyToken(line, next);
                boolean idToken = !keyToken && this.isResourceIdentifierString(token);
                Integer hexColor = keyToken ? null : this.hexColorFromToken(token);

                if (keyToken) {
                    this.addSpan(spans, lineStartOffset, cursor, next, SyntaxKind.PLAIN, this.keyColorForDepth(depth));
                } else if (hexColor != null && this.isQuotedLiteralToken(token)) {
                    this.addSpan(spans, lineStartOffset, cursor, cursor + 1, SyntaxKind.STRING, 0);
                    this.addSpan(spans, lineStartOffset, cursor + 1, next - 1, SyntaxKind.COLOR_LITERAL, hexColor);
                    this.addSpan(spans, lineStartOffset, next - 1, next, SyntaxKind.STRING, 0);
                } else if (hexColor != null) {
                    this.addSpan(spans, lineStartOffset, cursor, next, SyntaxKind.COLOR_LITERAL, hexColor);
                } else if (idToken) {
                    this.addSpan(spans, lineStartOffset, cursor, next, SyntaxKind.PLAIN, COLOR_IDENTIFIER);
                } else {
                    this.addSpan(spans, lineStartOffset, cursor, next, SyntaxKind.STRING, 0);
                }
                cursor = next;
                continue;
            }

            if (this.isNumberStart(line, cursor)) {
                next = this.scanNumber(line, cursor);
                String token = line.substring(cursor, next);
                Integer shadowColor = this.shadowColorFromNumberToken(line, cursor, token);
                if (shadowColor != null) {
                    this.addSpan(spans, lineStartOffset, cursor, next, SyntaxKind.COLOR_LITERAL, shadowColor);
                } else {
                    this.addSpan(spans, lineStartOffset, cursor, next, SyntaxKind.NUMERIC, 0);
                }
                cursor = next;
                continue;
            }

            if (this.isWordStart(value)) {
                next = this.scanWord(line, cursor + 1);
                String word = line.substring(cursor, next);
                Integer hexColor = this.hexColorFromToken(word);
                if (hexColor != null) {
                    this.addSpan(spans, lineStartOffset, cursor, next, SyntaxKind.COLOR_LITERAL, hexColor);
                } else if (this.isNumericWord(word)) {
                    this.addSpan(spans, lineStartOffset, cursor, next, SyntaxKind.NUMERIC, 0);
                } else {
                    int color = this.resolveWordColor(line, word, cursor, next, depth);
                    this.addSpan(spans, lineStartOffset, cursor, next, SyntaxKind.PLAIN, color);
                }
                cursor = next;
                continue;
            }

            if (value == '}' || value == ']') {
                depth = Math.max(0, depth - 1);
            }
            this.addSpan(
                    spans,
                    lineStartOffset,
                    cursor,
                    cursor + 1,
                    SyntaxKind.PLAIN,
                    this.punctuationColor(value, depth)
            );
            if (value == '{' || value == '[') {
                depth++;
            }
            cursor++;
        }
        return spans;
    }

    private void addSpan(
            List<SyntaxSpan> spans,
            int lineStartOffset,
            int localStart,
            int localEnd,
            SyntaxKind kind,
            int color
    ) {
        if (localEnd <= localStart) {
            return;
        }
        int start = lineStartOffset + localStart;
        int end = lineStartOffset + localEnd;
        if (!spans.isEmpty()) {
            SyntaxSpan last = spans.getLast();
            if (last.endOffset() == start && last.kind() == kind && last.color() == color && kind == SyntaxKind.PLAIN) {
                spans.set(spans.size() - 1, new SyntaxSpan(last.startOffset(), end, kind, color));
                return;
            }
        }
        spans.add(new SyntaxSpan(start, end, kind, color));
    }

    private int findStringEnd(String line, int start, char quote) {
        boolean escaping = false;
        for (int index = start; index < line.length(); index++) {
            char value = line.charAt(index);
            if (escaping) {
                escaping = false;
                continue;
            }
            if (value == '\\') {
                escaping = true;
                continue;
            }
            if (value == quote) {
                return index;
            }
        }
        return -1;
    }

    private boolean isKeyToken(String line, int tokenEndExclusive) {
        int next = tokenEndExclusive;
        while (next < line.length() && Character.isWhitespace(line.charAt(next))) {
            next++;
        }
        return next < line.length() && line.charAt(next) == ':';
    }

    private int scanWhitespace(String line, int index) {
        int cursor = index;
        while (cursor < line.length() && Character.isWhitespace(line.charAt(cursor))) {
            cursor++;
        }
        return cursor;
    }

    private boolean isNumberStart(String line, int index) {
        char value = line.charAt(index);
        if (Character.isDigit(value)) {
            return true;
        }
        if ((value == '-' || value == '+') && index + 1 < line.length()) {
            char next = line.charAt(index + 1);
            return Character.isDigit(next) || next == '.';
        }
        return value == '.' && index + 1 < line.length() && Character.isDigit(line.charAt(index + 1));
    }

    private int scanNumber(String line, int index) {
        int cursor = index;
        boolean seenDigits = false;
        boolean seenDot = false;
        boolean seenExponent = false;

        if (cursor < line.length() && (line.charAt(cursor) == '+' || line.charAt(cursor) == '-')) {
            cursor++;
        }

        while (cursor < line.length()) {
            char value = line.charAt(cursor);
            if (Character.isDigit(value)) {
                seenDigits = true;
                cursor++;
                continue;
            }
            if (value == '_') {
                cursor++;
                continue;
            }
            if (value == '.' && !seenDot && !seenExponent) {
                seenDot = true;
                cursor++;
                continue;
            }
            if ((value == 'e' || value == 'E') && seenDigits && !seenExponent) {
                seenExponent = true;
                cursor++;
                if (cursor < line.length() && (line.charAt(cursor) == '+' || line.charAt(cursor) == '-')) {
                    cursor++;
                }
                seenDigits = false;
                continue;
            }
            break;
        }

        if (cursor < line.length() && this.isNumberSuffix(line.charAt(cursor))) {
            cursor++;
        }
        if (cursor <= index) {
            return Math.min(line.length(), index + 1);
        }
        return cursor;
    }

    private Integer shadowColorFromNumberToken(String line, int tokenStartInclusive, String token) {
        String key = this.keyBeforeValue(line, tokenStartInclusive);
        if (!"shadow_color".equals(key) && !"shadowColor".equals(key)) {
            return null;
        }
        if (!this.isIntegerNumberToken(token)) {
            return null;
        }
        try {
            long parsed = Long.parseLong(token.replace("_", ""));
            if (parsed < Integer.MIN_VALUE || parsed > 0xFFFFFFFFL) {
                return null;
            }
            int argb = (int) parsed;
            return 0xFF000000 | (argb & 0xFFFFFF);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private boolean isIntegerNumberToken(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }
        int start = token.charAt(0) == '-' || token.charAt(0) == '+' ? 1 : 0;
        if (start >= token.length()) {
            return false;
        }
        for (int index = start; index < token.length(); index++) {
            char value = token.charAt(index);
            if (!Character.isDigit(value) && value != '_') {
                return false;
            }
        }
        return true;
    }

    private String keyBeforeValue(String line, int tokenStartInclusive) {
        int cursor = this.previousNonWhitespace(line, tokenStartInclusive - 1);
        if (cursor < 0 || line.charAt(cursor) != ':') {
            return "";
        }
        cursor = this.previousNonWhitespace(line, cursor - 1);
        if (cursor < 0) {
            return "";
        }

        char end = line.charAt(cursor);
        if (end == '"' || end == '\'') {
            return this.quotedKeyBefore(line, cursor, end);
        }

        int keyEnd = cursor + 1;
        for (int index = cursor; index >= 0; index--) {
            char value = line.charAt(index);
            if (!Character.isLetterOrDigit(value) && value != '_' && value != '-') {
                return line.substring(index + 1, keyEnd);
            }
        }
        return line.substring(0, keyEnd);
    }

    private String quotedKeyBefore(String line, int keyEnd, char quote) {
        for (int cursor = keyEnd - 1; cursor >= 0; cursor--) {
            if (line.charAt(cursor) == quote) {
                return line.substring(cursor + 1, keyEnd);
            }
        }
        return "";
    }

    private int previousNonWhitespace(String line, int cursor) {
        for (int index = cursor; index >= 0; index--) {
            if (!Character.isWhitespace(line.charAt(index))) {
                return index;
            }
        }
        return -1;
    }

    private boolean isWordStart(char value) {
        return Character.isLetter(value) || value == '_' || value == '#' || value == '$';
    }

    private int scanWord(String line, int index) {
        int cursor = index;
        while (cursor < line.length()) {
            char value = line.charAt(cursor);
            if (Character.isLetterOrDigit(value)
                    || value == '_'
                    || value == '-'
                    || value == '.'
                    || value == '/'
                    || value == '#'
                    || value == '$') {
                cursor++;
            } else {
                break;
            }
        }
        return cursor;
    }

    private int resolveWordColor(String line, String word, int tokenStartInclusive, int tokenEndExclusive, int depth) {
        if (this.isKeyToken(line, tokenEndExclusive)) {
            return this.keyColorForDepth(depth);
        }
        if (this.isBooleanWord(word)) {
            return COLOR_BOOLEAN;
        }
        if ("null".equalsIgnoreCase(word)) {
            return COLOR_NULL;
        }
        if (this.isTypedArrayTypeToken(line, word, tokenStartInclusive, tokenEndExclusive)) {
            return COLOR_LITERAL;
        }
        if (this.looksLikeResourceIdentifier(word)) {
            return COLOR_IDENTIFIER;
        }
        return COLOR_TEXT;
    }

    private int punctuationColor(char value, int depth) {
        return switch (value) {
            case '{', '}', '[', ']', '(', ')' -> this.bracketColorForDepth(depth);
            case ':', ',', ';', '=' -> COLOR_OPERATOR;
            default -> COLOR_PUNCT;
        };
    }

    private int keyColorForDepth(int depth) {
        int index = Math.floorMod(depth, KEY_DEPTH_COLORS.length);
        return KEY_DEPTH_COLORS[index];
    }

    private int bracketColorForDepth(int depth) {
        int index = Math.floorMod(depth, BRACKET_DEPTH_COLORS.length);
        return BRACKET_DEPTH_COLORS[index];
    }

    private boolean isBooleanWord(String word) {
        return "true".equalsIgnoreCase(word) || "false".equalsIgnoreCase(word);
    }

    private boolean isNumericWord(String word) {
        return "Infinity".equalsIgnoreCase(word)
                || "Infinityf".equalsIgnoreCase(word)
                || "Infinityd".equalsIgnoreCase(word)
                || "NaN".equalsIgnoreCase(word)
                || "NaNf".equalsIgnoreCase(word)
                || "NaNd".equalsIgnoreCase(word);
    }

    private boolean isNumberSuffix(char value) {
        char c = Character.toLowerCase(value);
        return c == 'b' || c == 's' || c == 'l' || c == 'f' || c == 'd';
    }

    private boolean isTypedArrayTypeToken(
            String line,
            String word,
            int tokenStartInclusive,
            int tokenEndExclusive
    ) {
        if (word.length() != 1) {
            return false;
        }
        char marker = Character.toUpperCase(word.charAt(0));
        if (marker != 'B' && marker != 'I' && marker != 'L') {
            return false;
        }
        int left = tokenStartInclusive - 1;
        while (left >= 0 && Character.isWhitespace(line.charAt(left))) {
            left--;
        }
        int right = tokenEndExclusive;
        while (right < line.length() && Character.isWhitespace(line.charAt(right))) {
            right++;
        }
        return left >= 0 && right < line.length() && line.charAt(left) == '[' && line.charAt(right) == ';';
    }

    private boolean isResourceIdentifierString(String token) {
        if (token.length() < 3) {
            return false;
        }
        char quote = token.charAt(0);
        if ((quote != '"' && quote != '\'') || token.charAt(token.length() - 1) != quote) {
            return false;
        }
        String inner = token.substring(1, token.length() - 1);
        if (inner.indexOf('\\') >= 0) {
            return false;
        }
        return this.looksLikeResourceIdentifier(inner);
    }

    private Integer parseHexColorLiteral(String literal) {
        if (literal == null) {
            return null;
        }
        String value = literal.trim();
        if (value.length() != 7 && value.length() != 9) {
            return null;
        }
        char prefix = value.charAt(0);
        if (prefix != '#' && prefix != '$') {
            return null;
        }
        if (prefix == '$' && value.length() != 9) {
            return null;
        }
        for (int index = 1; index < value.length(); index++) {
            if (isNonHexChar(value.charAt(index))) {
                return null;
            }
        }
        try {
            long parsed = Long.parseLong(value.substring(1), 16);
            int rgb = value.length() == 7 ? (int) parsed : (int) (parsed & 0xFFFFFFL);
            return 0xFF000000 | rgb;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private boolean looksLikeResourceIdentifier(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        String candidate = value.charAt(0) == '#' ? value.substring(1) : value;
        int separator = candidate.indexOf(':');
        if (separator <= 0 || separator >= candidate.length() - 1) {
            return false;
        }
        String namespace = candidate.substring(0, separator);
        String path = candidate.substring(separator + 1);
        for (int index = 0; index < namespace.length(); index++) {
            char c = namespace.charAt(index);
            if (!(Character.isLowerCase(c) || Character.isDigit(c) || c == '_' || c == '-' || c == '.')) {
                return false;
            }
        }
        for (int index = 0; index < path.length(); index++) {
            char c = path.charAt(index);
            if (!(Character.isLowerCase(c) || Character.isDigit(c) || c == '_' || c == '-' || c == '.' || c == '/')) {
                return false;
            }
        }
        return true;
    }

    private static boolean isNonHexChar(char value) {
        return (value < '0' || value > '9')
                && (value < 'a' || value > 'f')
                && (value < 'A' || value > 'F');
    }

    public enum SyntaxKind {
        PLAIN,
        STRING,
        NUMERIC,
        COLOR_LITERAL
    }

    public record SyntaxSpan(int startOffset, int endOffset, SyntaxKind kind, int color) {}

    private record SyntaxLineCache(int lineStartOffset, String line, int baseDepth, List<SyntaxSpan> spans) {
        private boolean matches(int lineStartOffset, String line, int baseDepth) {
            return this.lineStartOffset == lineStartOffset
                    && this.baseDepth == baseDepth
                    && this.line.equals(line);
        }
    }
}
