package me.noramibu.itemeditor.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class RawAutocompleteIndex {

    private final String text;
    private final boolean[] insideStringAt;
    private final int[] lastKeyRefAt;
    private final List<String> keyTable;
    private final List<String> seenKeys;
    private final Map<String, List<String>> seenKeysByContainer;

    private RawAutocompleteIndex(
            String text,
            boolean[] insideStringAt,
            int[] lastKeyRefAt,
            List<String> keyTable,
            List<String> seenKeys,
            Map<String, List<String>> seenKeysByContainer
    ) {
        this.text = text;
        this.insideStringAt = insideStringAt;
        this.lastKeyRefAt = lastKeyRefAt;
        this.keyTable = keyTable;
        this.seenKeys = seenKeys;
        this.seenKeysByContainer = seenKeysByContainer;
    }

    public static RawAutocompleteIndex create(String rawText) {
        String text = rawText == null ? "" : rawText;
        int length = text.length();
        boolean[] insideStringAt = new boolean[length + 1];
        int[] lastKeyRefAt = new int[length + 1];
        List<String> keyTable = new ArrayList<>();
        keyTable.add("");
        Set<String> seen = new LinkedHashSet<>();
        Map<String, Set<String>> seenByContainer = new LinkedHashMap<>();
        List<Frame> stack = new ArrayList<>();
        StringBuilder token = new StringBuilder();
        int pendingStringRef = 0;
        int pendingIdentifierRef = 0;
        int lastKeyRef = 0;
        boolean inString = false;
        boolean escaping = false;

        for (int index = 0; index <= length; index++) {
            insideStringAt[index] = inString;
            lastKeyRefAt[index] = lastKeyRef;

            if (index == length) {
                break;
            }

            char value = text.charAt(index);
            if (inString) {
                if (escaping) {
                    escaping = false;
                    token.append(value);
                    continue;
                }
                if (value == '\\') {
                    escaping = true;
                    token.append(value);
                    continue;
                }
                if (value == '"') {
                    inString = false;
                    pendingStringRef = internKey(keyTable, token.toString().replace("\\\"", "\""));
                    token.setLength(0);
                    continue;
                }
                token.append(value);
                continue;
            }

            if (!Character.isWhitespace(value)) {
                lastKeyRef = flushPendingRefs(
                        pendingStringRef,
                        pendingIdentifierRef,
                        value,
                        stack,
                        keyTable,
                        seen,
                        seenByContainer,
                        lastKeyRef
                );
                pendingStringRef = 0;
                pendingIdentifierRef = 0;
            }

            if (pendingStringRef == 0 && pendingIdentifierRef == 0 && isUnquotedKeyChar(value)) {
                int tokenStart = index;
                int tokenEnd = index + 1;
                while (tokenEnd < length && isUnquotedKeyChar(text.charAt(tokenEnd))) {
                    tokenEnd++;
                }
                pendingIdentifierRef = internKey(keyTable, text.substring(tokenStart, tokenEnd));
                index = tokenEnd - 1;
                continue;
            }

            switch (value) {
                case '"' -> {
                    inString = true;
                    token.setLength(0);
                }
                case ':' -> {
                }
                case '{' -> stack.add(new Frame(true, resolveParentKeyRef(stack)));
                case '[' -> stack.add(new Frame(false, resolveParentKeyRef(stack)));
                case '}', ']' -> {
                    if (!stack.isEmpty()) {
                        stack.removeLast();
                    }
                }
                case ',' -> {
                    Frame frame = top(stack);
                    if (frame != null && frame.object) {
                        frame.pendingKeyRef = 0;
                    }
                }
                default -> {
                    if (!Character.isWhitespace(value)) {
                        consumePendingKey(stack);
                    }
                }
            }
        }

        Map<String, List<String>> finalizedSeenByContainer = new LinkedHashMap<>();
        for (Map.Entry<String, Set<String>> entry : seenByContainer.entrySet()) {
            finalizedSeenByContainer.put(entry.getKey(), List.copyOf(entry.getValue()));
        }

        return new RawAutocompleteIndex(
                text,
                insideStringAt,
                lastKeyRefAt,
                keyTable,
                List.copyOf(seen),
                Map.copyOf(finalizedSeenByContainer)
        );
    }

    public static RawAutocompleteIndex update(
            RawAutocompleteIndex previous,
            String updatedText,
            int editStart,
            int editEnd,
            String replacement,
            boolean structural
    ) {
        String text = updatedText == null ? "" : updatedText;
        if (previous == null || structural) {
            return create(text);
        }
        if (!canFastSplice(previous.text, text, editStart, editEnd, replacement)) {
            return create(text);
        }

        int oldLength = previous.text.length();
        int safeStart = Math.clamp(editStart, 0, oldLength);
        int safeEnd = Math.clamp(editEnd, safeStart, oldLength);
        String safeReplacement = replacement == null ? "" : replacement;
        int insertedLength = safeReplacement.length();
        int newLength = text.length();

        boolean[] insideStringAt = spliceBooleanArray(
                previous.insideStringAt,
                oldLength,
                newLength,
                safeStart,
                safeEnd,
                insertedLength
        );
        int[] lastKeyRefAt = spliceIntArray(
                previous.lastKeyRefAt,
                oldLength,
                newLength,
                safeStart,
                safeEnd,
                insertedLength
        );

        return new RawAutocompleteIndex(
                text,
                insideStringAt,
                lastKeyRefAt,
                previous.keyTable,
                previous.seenKeys,
                previous.seenKeysByContainer
        );
    }

    public boolean matches(String candidate) {
        return this.text.equals(candidate == null ? "" : candidate);
    }

    public boolean insideStringAt(int cursor) {
        return this.insideStringAt[clamp(cursor)];
    }

    public String lastObjectKeyAt(int cursor) {
        return resolveKey(this.keyTable, this.lastKeyRefAt[clamp(cursor)]);
    }

    public List<String> seenKeys() {
        return this.seenKeys;
    }

    public List<String> seenKeysForContainer(String containerKey) {
        return this.seenKeysByContainer.getOrDefault(normalizeContainerKey(containerKey), List.of());
    }

    private int clamp(int cursor) {
        return Math.clamp(cursor, 0, this.text.length());
    }

    private static int internKey(List<String> keyTable, String key) {
        for (int index = 1; index < keyTable.size(); index++) {
            if (keyTable.get(index).equals(key)) {
                return index;
            }
        }
        keyTable.add(key);
        return keyTable.size() - 1;
    }

    private static String resolveKey(List<String> keyTable, int keyRef) {
        if (keyRef <= 0 || keyRef >= keyTable.size()) {
            return "";
        }
        return keyTable.get(keyRef);
    }

    private static int consumePendingKey(List<Frame> stack) {
        Frame top = top(stack);
        if (top == null || !top.object || top.pendingKeyRef == 0) {
            return 0;
        }
        int value = top.pendingKeyRef;
        top.pendingKeyRef = 0;
        return value;
    }

    private static int resolveParentKeyRef(List<Frame> stack) {
        int explicitKeyRef = consumePendingKey(stack);
        if (explicitKeyRef != 0) {
            return explicitKeyRef;
        }
        Frame top = top(stack);
        if (top != null && !top.object) {
            return top.parentKeyRef;
        }
        return 0;
    }

    private static Frame top(List<Frame> stack) {
        return stack.isEmpty() ? null : stack.getLast();
    }

    private static int consumePendingRef(
            int pendingRef,
            char token,
            List<Frame> stack,
            List<String> keyTable,
            Set<String> seen,
            Map<String, Set<String>> seenByContainer,
            int currentLastKeyRef
    ) {
        if (pendingRef == 0 || token != ':') {
            return currentLastKeyRef;
        }
        Frame frame = top(stack);
        if (frame != null && frame.object) {
            frame.pendingKeyRef = pendingRef;
        }
        String keyName = resolveKey(keyTable, pendingRef);
        seen.add(keyName);
        addSeenKey(seenByContainer, normalizeContainerKey(resolveKey(keyTable, frame == null ? 0 : frame.parentKeyRef)), keyName);
        return pendingRef;
    }

    private static int flushPendingRefs(
            int firstPendingRef,
            int secondPendingRef,
            char token,
            List<Frame> stack,
            List<String> keyTable,
            Set<String> seen,
            Map<String, Set<String>> seenByContainer,
            int currentLastKeyRef
    ) {
        int lastKeyRef = consumePendingRef(
                firstPendingRef,
                token,
                stack,
                keyTable,
                seen,
                seenByContainer,
                currentLastKeyRef
        );
        return consumePendingRef(
                secondPendingRef,
                token,
                stack,
                keyTable,
                seen,
                seenByContainer,
                lastKeyRef
        );
    }

    private static void addSeenKey(Map<String, Set<String>> seenByContainer, String containerKey, String keyName) {
        if (keyName == null || keyName.isBlank()) {
            return;
        }
        seenByContainer.computeIfAbsent(containerKey, ignored -> new LinkedHashSet<>()).add(keyName);
    }

    private static String normalizeContainerKey(String containerKey) {
        if (containerKey == null || containerKey.isBlank()) {
            return "";
        }
        return containerKey.toLowerCase(Locale.ROOT);
    }

    private static boolean isUnquotedKeyChar(char value) {
        return Character.isLetterOrDigit(value)
                || value == '_'
                || value == '-'
                || value == '.';
    }

    private static boolean canFastSplice(
            String previousText,
            String updatedText,
            int editStart,
            int editEnd,
            String replacement
    ) {
        if (previousText == null) {
            return false;
        }
        String safeUpdated = updatedText == null ? "" : updatedText;
        String safeReplacement = replacement == null ? "" : replacement;
        int oldLength = previousText.length();
        int safeStart = Math.clamp(editStart, 0, oldLength);
        int safeEnd = Math.clamp(editEnd, safeStart, oldLength);

        int expectedNewLength = oldLength - (safeEnd - safeStart) + safeReplacement.length();
        if (safeUpdated.length() != expectedNewLength) {
            return false;
        }

        if (!previousText.regionMatches(0, safeUpdated, 0, safeStart)) {
            return false;
        }
        int newSuffixStart = safeStart + safeReplacement.length();
        int suffixLength = oldLength - safeEnd;
        return previousText.regionMatches(safeEnd, safeUpdated, newSuffixStart, suffixLength);
    }

    private static boolean[] spliceBooleanArray(
            boolean[] source,
            int oldLength,
            int newLength,
            int editStart,
            int editEnd,
            int insertedLength
    ) {
        boolean[] target = new boolean[newLength + 1];
        int safeStart = Math.clamp(editStart, 0, oldLength);
        int safeEnd = Math.clamp(editEnd, safeStart, oldLength);
        int newSegmentEnd = safeStart + insertedLength;

        System.arraycopy(source, 0, target, 0, safeStart + 1);
        boolean fill = source[Math.min(safeStart, oldLength)];
        for (int index = safeStart + 1; index < newSegmentEnd; index++) {
            target[index] = fill;
        }

        int suffixCopyLength = oldLength - safeEnd + 1;
        System.arraycopy(source, safeEnd, target, newSegmentEnd, suffixCopyLength);
        return target;
    }

    private static int[] spliceIntArray(
            int[] source,
            int oldLength,
            int newLength,
            int editStart,
            int editEnd,
            int insertedLength
    ) {
        int[] target = new int[newLength + 1];
        int safeStart = Math.clamp(editStart, 0, oldLength);
        int safeEnd = Math.clamp(editEnd, safeStart, oldLength);
        int newSegmentEnd = safeStart + insertedLength;

        System.arraycopy(source, 0, target, 0, safeStart + 1);
        int fill = source[Math.min(safeStart, oldLength)];
        for (int index = safeStart + 1; index < newSegmentEnd; index++) {
            target[index] = fill;
        }

        int suffixCopyLength = oldLength - safeEnd + 1;
        System.arraycopy(source, safeEnd, target, newSegmentEnd, suffixCopyLength);
        return target;
    }

    public record Context(
            boolean inObject,
            boolean inRootObject,
            boolean inComponentsObject,
            String containerKey,
            String containerPath
    ) {
    }

    private static final class Frame {
        private final boolean object;
        private final int parentKeyRef;
        private int pendingKeyRef;

        private Frame(boolean object, int parentKeyRef) {
            this.object = object;
            this.parentKeyRef = parentKeyRef;
        }
    }
}
