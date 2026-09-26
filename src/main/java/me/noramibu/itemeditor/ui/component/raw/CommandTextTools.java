package me.noramibu.itemeditor.ui.component.raw;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.StringReader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import me.noramibu.itemeditor.util.RawTextScan;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.TagParser;

/** Offset-preserving inspection only; never executes command text. */
public final class CommandTextTools {
    public static final int MAX_PARSE_LENGTH = 32_768;

    private CommandTextTools() {}

    public record Range(int start, int end) {}

    public record Embedded(Range range, RawEmbeddedString.Slice string) {}

    public static StringReader reader(String command) {
        StringReader reader = new StringReader(command);
        reader.skipWhitespace();
        if (reader.canRead() && reader.peek() == '/') reader.skip();
        return reader;
    }

    public static StringReader validationReader(String command) {
        // Keep original offsets and stored text; completion still needs trailing separators.
        return reader(command.stripTrailing());
    }

    public static boolean hasCommandRoot(CommandDispatcher<?> dispatcher, String command) {
        return dispatcher.getRoot().getChild(reader(command).readUnquotedString()) != null;
    }

    public static Embedded at(String text, int caret, int anchor) {
        if (text.length() > RawEmbeddedString.MAX_LENGTH) throw new IllegalArgumentException("too_large");
        RawEmbeddedString.Slice string = RawEmbeddedString.at(text, caret, anchor);
        if (string != null) return new Embedded(new Range(string.start(), string.end()), string);
        int left = Math.min(caret, anchor), right = Math.max(caret, anchor);
        ArrayDeque<Integer> starts = new ArrayDeque<>();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\'' || c == '"') {
                int end = RawTextScan.quotedEnd(text, i + 1, c);
                if (end < 0) break;
                i = end;
            } else if (c == '{' || c == '[') starts.push(i);
            else if ((c == '}' || c == ']') && !starts.isEmpty()) {
                int start = starts.pop();
                if (text.charAt(start) != (c == '}' ? '{' : '[')) continue;
                // Selectors and block-state suffixes are not standalone NBT arrays.
                boolean suffix = text.charAt(start) == '['
                        && start > 0
                        && !Character.isWhitespace(text.charAt(start - 1))
                        && ":,[{".indexOf(text.charAt(start - 1)) < 0;
                if (!suffix && left >= start && right <= i + 1 && left <= i)
                    return new Embedded(new Range(start, i + 1), null);
            }
        }
        return null;
    }

    public static List<RawEmbeddedString.Slice> commands(String text) {
        if (text.length() > RawEmbeddedString.MAX_LENGTH) throw new IllegalArgumentException("too_large");
        List<RawEmbeddedString.Slice> commands = new ArrayList<>();
        int depth = 0;
        for (int i = 0; i < text.length() && commands.size() < 2048; i++) {
            char c = text.charAt(i);
            if (c == '{' || c == '[') {
                depth++;
                continue;
            }
            if (c == '}' || c == ']') {
                depth--;
                continue;
            }
            String key;
            if (c == '\'' || c == '"') {
                int end = RawTextScan.quotedEnd(text, i + 1, c);
                if (end < 0) break;
                key = text.substring(i + 1, end);
                i = end;
            } else if (Character.isLetter(c) && depth > 0) {
                int start = i;
                while (i + 1 < text.length() && Character.isJavaIdentifierPart(text.charAt(i + 1))) i++;
                key = text.substring(start, i + 1);
            } else continue;
            if (depth <= 0 || !key.equals("Command")) continue;
            int colon = whitespace(text, i + 1);
            if (colon >= text.length() || text.charAt(colon) != ':') continue;
            int start = whitespace(text, colon + 1);
            if (start >= text.length() || (text.charAt(start) != '"' && text.charAt(start) != '\'')) continue;
            int end = RawTextScan.quotedEnd(text, start + 1, text.charAt(start));
            if (end < 0) break;
            try {
                if (TagParser.create(NbtOps.INSTANCE).parseFully(text.substring(start, end + 1))
                        instanceof StringTag(String value))
                    commands.add(new RawEmbeddedString.Slice(start, end + 1, value));
            } catch (Exception ignored) {
                // Incomplete values remain editable in the main command editor.
            }
            i = end;
        }
        return List.copyOf(commands);
    }

    private static int whitespace(String text, int offset) {
        while (offset < text.length() && Character.isWhitespace(text.charAt(offset))) offset++;
        return offset;
    }

    public static String compactData(String value) {
        if (value.length() > RawEmbeddedString.MAX_LENGTH) throw new IllegalArgumentException("too_large");
        try {
            TagParser.create(NbtOps.INSTANCE).parseFully(value);
        } catch (Exception error) {
            throw new IllegalArgumentException("invalid", error);
        }
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '"' || c == '\'') i = RawTextScan.appendQuoted(result, value, i);
            else if (!Character.isWhitespace(c)) result.append(c);
        }
        return result.toString();
    }

    public static String singleLine(String command) {
        if (command.indexOf('\n') < 0 && command.indexOf('\r') < 0) return command;
        StringBuilder result = new StringBuilder(command.length());
        for (int i = 0; i < command.length(); i++) {
            char c = command.charAt(i);
            if (c == '"' || c == '\'') {
                i = RawTextScan.appendQuoted(result, command, i);
            } else if (c == '\n' || c == '\r') {
                while (i + 1 < command.length() && Character.isWhitespace(command.charAt(i + 1))) i++;
                int length = result.length();
                while (length > 0 && (result.charAt(length - 1) == ' ' || result.charAt(length - 1) == '\t'))
                    result.setLength(--length);
                char before = length == 0 ? 0 : result.charAt(length - 1);
                char after = i + 1 == command.length() ? 0 : command.charAt(i + 1);
                if (before != 0 && after != 0 && "{[,:".indexOf(before) < 0 && "}],:".indexOf(after) < 0)
                    result.append(' ');
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    public static String displayQuotedNewlines(String command) {
        if (command.indexOf('\n') < 0 && command.indexOf('\r') < 0) return command;
        StringBuilder result = new StringBuilder(command.length());
        char quote = 0;
        boolean escaped = false;
        for (int i = 0; i < command.length(); i++) {
            char c = command.charAt(i);
            if (quote != 0 && c == '\n') result.append("\\n");
            else if (quote != 0 && c == '\r') result.append("\\r");
            else result.append(c);
            if (escaped) escaped = false;
            else if (quote != 0 && c == '\\') escaped = true;
            else if (c == quote) quote = 0;
            else if (quote == 0 && (c == '"' || c == '\'')) quote = c;
        }
        return result.toString();
    }
}
