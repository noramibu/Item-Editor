package me.noramibu.itemeditor.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

final class RawCursorContext {

    private final Slot slot;
    private final boolean insideString;
    private final boolean inObject;
    private final boolean inRootObject;
    private final boolean inComponentsObject;
    private final String currentKey;
    private final String containerKey;
    private final String containerPath;

    private RawCursorContext(
            Slot slot,
            boolean insideString,
            boolean inObject,
            boolean inRootObject,
            boolean inComponentsObject,
            String currentKey,
            String containerKey,
            String containerPath
    ) {
        this.slot = slot;
        this.insideString = insideString;
        this.inObject = inObject;
        this.inRootObject = inRootObject;
        this.inComponentsObject = inComponentsObject;
        this.currentKey = Objects.requireNonNullElse(currentKey, "");
        this.containerKey = Objects.requireNonNullElse(containerKey, "");
        this.containerPath = Objects.requireNonNullElse(containerPath, "");
    }

    static RawCursorContext create(String rawText, int caretIndex) {
        String text = Objects.requireNonNullElse(rawText, "");
        int cursor = Math.clamp(caretIndex, 0, text.length());
        Scanner scanner = new Scanner(text, cursor);
        return scanner.scan();
    }

    Slot slot() {
        return this.slot;
    }

    boolean insideString() {
        return this.insideString;
    }

    String currentKey() {
        return this.currentKey;
    }

    RawAutocompleteIndex.Context indexContext() {
        return new RawAutocompleteIndex.Context(
                this.inObject,
                this.inRootObject,
                this.inComponentsObject,
                this.containerKey,
                this.containerPath
        );
    }

    enum Slot {
        OBJECT_KEY,
        VALUE,
        AFTER_VALUE,
        UNKNOWN
    }

    private static final class Scanner {
        private final String text;
        private final int cursor;
        private final List<Frame> stack = new ArrayList<>();
        private Slot activeSlot = Slot.UNKNOWN;
        private String activeKey = "";
        private boolean insideString;

        private Scanner(String text, int cursor) {
            this.text = text;
            this.cursor = cursor;
        }

        private RawCursorContext scan() {
            int index = 0;
            while (index < this.cursor) {
                char value = this.text.charAt(index);
                if (Character.isWhitespace(value)) {
                    index++;
                    continue;
                }

                switch (value) {
                    case '"' -> index = scanString(index);
                    case '{' -> {
                        openFrame(true);
                        index++;
                    }
                    case '[' -> {
                        openFrame(false);
                        index++;
                    }
                    case '}', ']' -> {
                        closeFrame();
                        index++;
                    }
                    case ':' -> {
                        consumeColon();
                        index++;
                    }
                    case ',' -> {
                        consumeComma();
                        index++;
                    }
                    default -> {
                        if (isTokenCharacter(value, currentTokenSlot())) {
                            index = scanToken(index);
                        } else {
                            index++;
                        }
                    }
                }
            }
            return createContext();
        }

        private int scanString(int quoteStart) {
            Frame frame = top();
            Slot stringSlot = frame == null ? Slot.VALUE : frame.tokenSlot();
            String keyForValue = frame == null ? "" : frame.pendingKey();
            StringBuilder token = new StringBuilder();
            boolean escaping = false;
            int index = quoteStart + 1;
            while (index < this.cursor) {
                char value = this.text.charAt(index);
                if (escaping) {
                    token.append(value);
                    escaping = false;
                    index++;
                    continue;
                }
                if (value == '\\') {
                    escaping = true;
                    index++;
                    continue;
                }
                if (value == '"') {
                    consumeStringToken(frame, stringSlot, token.toString());
                    return index + 1;
                }
                token.append(value);
                index++;
            }

            this.insideString = true;
            this.activeSlot = stringSlot;
            this.activeKey = stringSlot == Slot.VALUE ? keyForValue : "";
            return this.cursor;
        }

        private int scanToken(int tokenStart) {
            int end = tokenStart + 1;
            Slot tokenSlot = currentTokenSlot();
            while (end < this.cursor && isTokenCharacter(this.text.charAt(end), tokenSlot)) {
                end++;
            }

            Frame frame = top();
            String token = this.text.substring(tokenStart, end);
            if (frame == null) {
                this.activeSlot = Slot.VALUE;
                return end;
            }

            if (end == this.cursor) {
                this.activeSlot = tokenSlot;
                this.activeKey = tokenSlot == Slot.VALUE ? frame.pendingKey() : "";
                return end;
            }

            if (tokenSlot == Slot.OBJECT_KEY) {
                frame.acceptKey(token);
            } else if (tokenSlot == Slot.VALUE) {
                frame.consumeValue();
            }
            return end;
        }

        private void openFrame(boolean object) {
            Frame parent = top();
            String parentKey = parent == null ? "" : parent.childParentKey();
            if (parent != null && parent.tokenSlot() == Slot.VALUE) {
                parent.consumeValue();
            }
            this.stack.add(new Frame(object, parentKey));
            this.activeSlot = object ? Slot.OBJECT_KEY : Slot.VALUE;
            this.activeKey = "";
        }

        private void closeFrame() {
            if (!this.stack.isEmpty()) {
                this.stack.removeLast();
            }
            this.activeSlot = Slot.AFTER_VALUE;
            Frame top = top();
            this.activeKey = top == null ? "" : top.lastKey();
        }

        private void consumeColon() {
            Frame frame = top();
            if (frame != null && frame.object) {
                frame.expectValue();
                this.activeSlot = Slot.VALUE;
                this.activeKey = frame.pendingKey();
            }
        }

        private void consumeComma() {
            Frame frame = top();
            if (frame != null) {
                frame.consumeComma();
                this.activeSlot = frame.object ? Slot.OBJECT_KEY : Slot.VALUE;
                this.activeKey = "";
            }
        }

        private void consumeStringToken(Frame frame, Slot stringSlot, String token) {
            if (frame == null) {
                this.activeSlot = Slot.AFTER_VALUE;
                return;
            }
            if (stringSlot == Slot.OBJECT_KEY) {
                frame.acceptKey(token);
                this.activeSlot = Slot.OBJECT_KEY;
                this.activeKey = "";
            } else if (stringSlot == Slot.VALUE) {
                this.activeKey = frame.pendingKey();
                frame.consumeValue();
                this.activeSlot = Slot.AFTER_VALUE;
            }
        }

        private RawCursorContext createContext() {
            Frame top = top();
            Slot slot = resolveSlot(top);
            String currentKey = resolveCurrentKey(top, slot);
            String containerKey = top == null ? "" : top.parentKey();
            String containerPath = containerPath();
            return new RawCursorContext(
                    slot,
                    this.insideString,
                    top != null && top.object,
                    top != null && top.object && this.stack.size() == 1,
                    top != null && top.object && "components".equals(top.parentKey()),
                    currentKey,
                    containerKey,
                    containerPath
            );
        }

        private Slot resolveSlot(Frame top) {
            if (this.insideString || this.activeSlot != Slot.UNKNOWN) {
                return this.activeSlot;
            }
            if (top == null) {
                return Slot.UNKNOWN;
            }
            return top.tokenSlot();
        }

        private String resolveCurrentKey(Frame top, Slot slot) {
            if (!this.activeKey.isBlank()) {
                return this.activeKey;
            }
            if (top == null) {
                return "";
            }
            if (slot == Slot.VALUE) {
                return top.pendingKey();
            }
            if (slot == Slot.AFTER_VALUE) {
                return top.lastKey();
            }
            return "";
        }

        private String containerPath() {
            if (this.stack.isEmpty()) {
                return "";
            }

            StringBuilder path = new StringBuilder();
            String previousKey = "";
            for (Frame frame : this.stack) {
                String key = frame.parentKey();
                if (key.isBlank() || key.equals(previousKey)) {
                    continue;
                }
                if (!path.isEmpty()) {
                    path.append('/');
                }
                path.append(key.toLowerCase(Locale.ROOT));
                previousKey = key;
            }
            return path.toString();
        }

        private Frame top() {
            return this.stack.isEmpty() ? null : this.stack.getLast();
        }

        private Slot currentTokenSlot() {
            Frame frame = top();
            return frame == null ? Slot.VALUE : frame.tokenSlot();
        }

        private static boolean isTokenCharacter(char value, Slot slot) {
            return Character.isLetterOrDigit(value)
                    || value == '_'
                    || (slot == Slot.VALUE && value == ':')
                    || value == '.'
                    || value == '-'
                    || value == '/'
                    || value == '+';
        }
    }

    private static final class Frame {
        private final boolean object;
        private final String parentKey;
        private boolean expectingKey;
        private boolean expectingValue;
        private String pendingKey = "";
        private String lastKey = "";

        private Frame(boolean object, String parentKey) {
            this.object = object;
            this.parentKey = Objects.requireNonNullElse(parentKey, "");
            this.expectingKey = object;
            this.expectingValue = !object;
        }

        private Slot tokenSlot() {
            if (!this.object) {
                return Slot.VALUE;
            }
            if (this.expectingValue) {
                return Slot.VALUE;
            }
            if (this.expectingKey || !this.pendingKey.isBlank()) {
                return Slot.OBJECT_KEY;
            }
            return Slot.AFTER_VALUE;
        }

        private String childParentKey() {
            if (this.object && this.expectingValue && !this.pendingKey.isBlank()) {
                return this.pendingKey;
            }
            return this.parentKey;
        }

        private void acceptKey(String key) {
            if (!this.object) {
                return;
            }
            this.pendingKey = Objects.requireNonNullElse(key, "");
            this.expectingKey = false;
            this.expectingValue = false;
        }

        private void expectValue() {
            if (!this.object || this.pendingKey.isBlank()) {
                return;
            }
            this.expectingValue = true;
            this.expectingKey = false;
        }

        private void consumeValue() {
            if (this.object) {
                this.lastKey = this.pendingKey;
                this.pendingKey = "";
                this.expectingValue = false;
                this.expectingKey = false;
            }
        }

        private void consumeComma() {
            if (this.object) {
                this.pendingKey = "";
                this.expectingKey = true;
                this.expectingValue = false;
            }
        }

        private String parentKey() {
            return this.parentKey;
        }

        private String pendingKey() {
            return this.pendingKey;
        }

        private String lastKey() {
            return this.lastKey;
        }
    }
}
