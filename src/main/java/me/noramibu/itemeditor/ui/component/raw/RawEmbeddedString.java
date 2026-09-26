package me.noramibu.itemeditor.ui.component.raw;

import com.google.gson.JsonPrimitive;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.TagParser;

/** Locates SNBT strings without interpreting or executing embedded commands. */
public final class RawEmbeddedString {
    public static final int MAX_LENGTH = 2_000_000;

    private RawEmbeddedString() {}

    public record Slice(int start, int end, String value) {
        public String encoded(String edited) {
            // JSON quoting also works in SNBT; SNBT's optional single quotes would break nested JSON.
            return new JsonPrimitive(edited).toString();
        }
    }

    public static Slice at(String text, int caret, int anchor) {
        if (text.length() > MAX_LENGTH) throw new IllegalArgumentException("too_large");
        int left = Math.min(caret, anchor), right = Math.max(caret, anchor);
        for (int i = 0; i < text.length(); i++) {
            char quote = text.charAt(i);
            if (quote != '\'' && quote != '"') continue;
            int end = quotedEnd(text, i);
            if (end < 0) return null;
            int next = end;
            while (next < text.length() && Character.isWhitespace(text.charAt(next))) next++;
            boolean key = next < text.length() && text.charAt(next) == ':';
            if (!key && left >= i && right <= end && (left < end || left != right)) {
                try {
                    if (TagParser.create(NbtOps.INSTANCE).parseFully(text.substring(i, end))
                            instanceof StringTag(String value)) {
                        return new Slice(i, end, value);
                    }
                } catch (Exception ignored) {
                    throw new IllegalArgumentException("invalid");
                }
            }
            i = end - 1;
        }
        return null;
    }

    private static int quotedEnd(String text, int start) {
        char quote = text.charAt(start);
        for (int i = start + 1; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\\') i++;
            else if (c == quote) return i + 1;
        }
        return -1;
    }

    public static Slice fromView(String source, String displayed, int caret, int anchor) {
        Slice selected = at(displayed, caret, anchor);
        if (selected == null || source.equals(displayed)) return selected;
        int ordinal = 0;
        for (int i = 0; i < selected.start(); i++) {
            if (displayed.charAt(i) != '\'' && displayed.charAt(i) != '"') continue;
            i = quotedEnd(displayed, i) - 1;
            ordinal++;
        }
        for (int i = 0; i < source.length(); i++) {
            if (source.charAt(i) != '\'' && source.charAt(i) != '"') continue;
            int end = quotedEnd(source, i);
            if (end < 0) return null;
            if (ordinal-- == 0) return new Slice(i, end, selected.value());
            i = end - 1;
        }
        return null;
    }

    /** Maps edited tokens back while retaining original whitespace around unchanged tokens. */
    public static String applyFormatted(String source, String baseline, String edited) {
        if (baseline.equals(edited)) return source;
        if (edited.length() > MAX_LENGTH * 4) throw new IllegalArgumentException("too_large");
        List<Token> original = tokens(source), updated = tokens(edited);
        int prefix = 0, suffix = 0;
        while (prefix < Math.min(original.size(), updated.size())
                && original.get(prefix).text().equals(updated.get(prefix).text())) prefix++;
        if (prefix == original.size() && prefix == updated.size()) return source;
        while (suffix < Math.min(original.size(), updated.size()) - prefix
                && original.get(original.size() - suffix - 1)
                        .text()
                        .equals(updated.get(updated.size() - suffix - 1).text())) suffix++;
        StringBuilder output = new StringBuilder();
        for (int i = 0; i < updated.size(); i++) {
            Token token = updated.get(i);
            int originalIndex =
                    i < prefix ? i : i >= updated.size() - suffix ? original.size() - (updated.size() - i) : -1;
            boolean originalGap =
                    originalIndex >= 0 && (i == 0 ? originalIndex == 0 : i < prefix || i > updated.size() - suffix);
            if (originalGap) {
                output.append(
                        source,
                        originalIndex == 0 ? 0 : original.get(originalIndex - 1).end(),
                        original.get(originalIndex).start());
            } else {
                int gapStart = i == 0 ? 0 : updated.get(i - 1).end();
                String gap = edited.substring(gapStart, token.start());
                // Structural line breaks are layout, but quoted token contents are copied verbatim.
                boolean layoutGap = gap.indexOf('\n') >= 0 || gap.indexOf('\r') >= 0;
                if (!layoutGap) output.append(gap);
                else if (i > 0 && !endsStructure(updated.get(i - 1).text()) && !startsStructure(token.text()))
                    output.append(' ');
            }
            output.append(token.text());
        }
        if (suffix > 0) {
            output.append(source, original.getLast().end(), source.length());
        } else if (!updated.isEmpty()) {
            String tail = edited.substring(updated.getLast().end());
            if (tail.indexOf('\n') < 0 && tail.indexOf('\r') < 0) output.append(tail);
        }
        if (output.length() > MAX_LENGTH) throw new IllegalArgumentException("too_large");
        return output.toString();
    }

    private static boolean endsStructure(String token) {
        return token.length() == 1 && "{[,:".contains(token);
    }

    private static boolean startsStructure(String token) {
        return token.length() == 1 && "}],:".contains(token);
    }

    private record Token(String text, int start, int end) {}

    private static List<Token> tokens(String text) {
        List<Token> tokens = new ArrayList<>();
        Deque<Character> brackets = new ArrayDeque<>();
        for (int i = 0; i < text.length(); ) {
            char c = text.charAt(i);
            if (Character.isWhitespace(c)) {
                i++;
                continue;
            }
            int start = i;
            if (c == '\'' || c == '"') {
                i = quotedEnd(text, i);
                if (i < 0) throw new IllegalArgumentException("invalid");
                try {
                    TagParser.create(NbtOps.INSTANCE).parseFully(text.substring(start, i));
                } catch (Exception error) {
                    throw new IllegalArgumentException("invalid", error);
                }
            } else if ("{}[],:".indexOf(c) >= 0) {
                if (c == '{' || c == '[') brackets.push(c);
                else if (c == '}' || c == ']') {
                    if (brackets.isEmpty() || brackets.pop() != (c == '}' ? '{' : '['))
                        throw new IllegalArgumentException("invalid");
                }
                i++;
            } else {
                while (i < text.length()
                        && !Character.isWhitespace(text.charAt(i))
                        && "{}[],:\"'".indexOf(text.charAt(i)) < 0) i++;
            }
            tokens.add(new Token(text.substring(start, i), start, i));
        }
        if (!brackets.isEmpty()) throw new IllegalArgumentException("invalid");
        return tokens;
    }

    /** Readable projection; use applyFormatted to exclude layout whitespace when saving edits. */
    public static String format(String text) {
        if (text.length() > MAX_LENGTH) throw new IllegalArgumentException("too_large");
        StringBuilder output = new StringBuilder();
        Deque<Character> frames = new ArrayDeque<>();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\'' || c == '"') {
                int end = quotedEnd(text, i);
                if (end < 0) end = text.length();
                output.append(text, i, end);
                i = end - 1;
            } else if (c == '{' || c == '[') {
                // Selectors and block-state suffixes stay inline, unlike lists and compounds.
                boolean inline = (!frames.isEmpty() && frames.peek() == '=')
                        || (c == '['
                                && i > 0
                                && !Character.isWhitespace(text.charAt(i - 1))
                                && ":,[{".indexOf(text.charAt(i - 1)) < 0);
                output.append(c);
                frames.push(inline ? '=' : c);
                if (!inline) newline(output, frames.size());
            } else if (c == '}' || c == ']') {
                boolean inline = !frames.isEmpty() && frames.pop() == '=';
                if (!inline) newline(output, frames.size());
                output.append(c);
            } else if (c == ',' && !frames.isEmpty() && frames.peek() != '=') {
                output.append(c);
                newline(output, frames.size());
            } else if (Character.isWhitespace(c)
                    && !frames.isEmpty()
                    && frames.peek() != '='
                    && (output.isEmpty() || Character.isWhitespace(output.charAt(output.length() - 1)))) {
                // Ignore source indentation immediately after an inserted line break.
            } else {
                output.append(c);
            }
            if (output.length() > MAX_LENGTH * 4) throw new IllegalArgumentException("too_large");
        }
        return output.toString();
    }

    private static void newline(StringBuilder output, int depth) {
        while (!output.isEmpty() && Character.isWhitespace(output.charAt(output.length() - 1)))
            output.setLength(output.length() - 1);
        output.append('\n').repeat("    ", Math.min(depth, 12));
    }
}
