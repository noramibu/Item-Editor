package me.noramibu.itemeditor.ui.component;

import me.noramibu.itemeditor.editor.text.RichTextDocument;
import me.noramibu.itemeditor.editor.text.RichTextStyle;
import me.noramibu.itemeditor.util.TextComponentUtil;

import java.util.ArrayDeque;
import java.util.function.UnaryOperator;

final class RichTextInputController {

    TextChangeResult updateDocumentFromPlainTextChange(
            RichTextDocument previousDocument,
            String beforeText,
            String afterText,
            RichTextStyle insertionStyle,
            RichTextStyle defaultInsertionStyle
    ) {
        int prefix = 0;
        int maxPrefix = Math.min(beforeText.length(), afterText.length());
        while (prefix < maxPrefix && beforeText.charAt(prefix) == afterText.charAt(prefix)) {
            prefix++;
        }

        int beforeSuffix = beforeText.length();
        int afterSuffix = afterText.length();
        while (beforeSuffix > prefix
                && afterSuffix > prefix
                && beforeText.charAt(beforeSuffix - 1) == afterText.charAt(afterSuffix - 1)) {
            beforeSuffix--;
            afterSuffix--;
        }

        String insertedText = afterText.substring(prefix, afterSuffix);
        DeletionRange range = this.expandAtomicSourceDeletion(
                beforeText,
                new DeletionRange(prefix, beforeSuffix)
        );
        range = this.normalizeEventBoundaryDeletion(
                beforeText,
                range,
                insertedText
        );
        if (insertedText.isEmpty()) {
            range = this.expandEmptyEventDeletion(beforeText, range.start(), range.end());
        }
        range = this.expandAtomicSourceDeletion(beforeText, range);

        RichTextDocument updated = previousDocument.copy();
        updated.replace(range.start(), range.end(), insertedText, this.normalizedStyle(insertionStyle, defaultInsertionStyle));
        int cursorOverride = range.start() != prefix || range.end() != beforeSuffix ? range.start() : -1;
        if (insertedText.isEmpty()) {
            CleanupResult cleanup = this.removeEmptyEventPairs(updated, cursorOverride >= 0 ? cursorOverride : prefix, defaultInsertionStyle);
            updated = cleanup.document();
            if (cleanup.changed()) {
                cursorOverride = cleanup.cursor();
            }
        }
        return new TextChangeResult(updated, cursorOverride);
    }

    private DeletionRange expandAtomicSourceDeletion(String text, DeletionRange range) {
        DeletionRange expanded = range;
        DeletionRange previous;
        do {
            previous = expanded;
            expanded = this.expandOneAtomicSourceDeletion(text, previous);
        } while (!expanded.equals(previous));
        return expanded;
    }

    private DeletionRange expandOneAtomicSourceDeletion(String text, DeletionRange range) {
        if (text == null || text.isEmpty()) {
            return range;
        }
        for (int cursor = 0; cursor < text.length(); ) {
            int length = this.atomicSourceLengthAt(text, cursor);
            if (length > 0) {
                int end = cursor + length;
                if (this.rangeTouchesAtomicSource(range, cursor, end)) {
                    return new DeletionRange(Math.min(range.start(), cursor), Math.max(range.end(), end));
                }
                cursor = end;
                continue;
            }
            cursor += Character.charCount(text.codePointAt(cursor));
        }
        return range;
    }

    private int atomicSourceLengthAt(String text, int cursor) {
        int tokenLength = TextComponentUtil.structuredTokenLengthAt(text, cursor);
        if (tokenLength > 0) {
            return tokenLength;
        }
        return TextComponentUtil.formattingCodeLengthAt(text, cursor);
    }

    private boolean rangeTouchesAtomicSource(DeletionRange range, int start, int end) {
        if (range.start() == range.end()) {
            return range.start() > start && range.start() < end;
        }
        return range.start() < end && range.end() > start;
    }

    private DeletionRange expandEmptyEventDeletion(String text, int start, int end) {
        DeletionRange range = new DeletionRange(start, end);
        DeletionRange expanded;
        do {
            expanded = range;
            range = this.expandOneEmptyEventDeletion(text, expanded.start(), expanded.end());
        } while (!range.equals(expanded));
        return range;
    }

    private DeletionRange expandOneEmptyEventDeletion(String text, int start, int end) {
        EventToken open = this.eventOpenTokenEndingAt(text, start);
        if (open == null) {
            return new DeletionRange(start, end);
        }

        EventToken close = this.eventCloseTokenStartingAt(text, end);
        if (close == null || !open.type().equals(close.type())) {
            return new DeletionRange(start, end);
        }
        return new DeletionRange(open.start(), close.end());
    }

    private DeletionRange normalizeEventBoundaryDeletion(String text, DeletionRange range, String insertedText) {
        DeletionRange normalized = this.preserveOpeningEventBoundary(text, range);
        return this.preserveClosingEventBoundary(text, normalized, insertedText);
    }

    private DeletionRange preserveOpeningEventBoundary(String text, DeletionRange range) {
        EventToken open = this.eventOpenTokenStartingAt(text, range.start());
        if (open == null || range.end() <= open.end()) {
            return range;
        }

        EventToken close = this.matchingCloseAfterOpen(text, open);
        if (close == null || range.end() >= close.start()) {
            return range;
        }

        return new DeletionRange(open.end(), range.end());
    }

    private DeletionRange preserveClosingEventBoundary(String text, DeletionRange range, String insertedText) {
        EventToken close = this.eventCloseTokenEndingAt(text, range.end());
        if (close == null || range.start() >= close.start()) {
            return range;
        }

        EventToken open = this.matchingOpenBeforeClose(text, close);
        if (open == null || range.start() <= open.start()) {
            return range;
        }

        boolean payloadRemains = range.start() > open.end() || !insertedText.isEmpty();
        if (payloadRemains) {
            return new DeletionRange(range.start(), close.start());
        }
        return new DeletionRange(open.start(), close.end());
    }

    private EventToken eventOpenTokenEndingAt(String text, int end) {
        if (end <= 0 || end > text.length()) {
            return null;
        }

        int tokenStart = text.lastIndexOf("[ie:", end - 1);
        if (tokenStart < 0) {
            return null;
        }

        int tokenLength = TextComponentUtil.structuredTokenLengthAt(text, tokenStart);
        if (tokenLength <= 0 || tokenStart + tokenLength != end) {
            return null;
        }

        return this.eventOpenTokenFromBounds(text, tokenStart, end);
    }

    private EventToken eventCloseTokenStartingAt(String text, int start) {
        if (start < 0 || start >= text.length()) {
            return null;
        }

        int tokenLength = TextComponentUtil.structuredTokenLengthAt(text, start);
        if (tokenLength <= 0) {
            return null;
        }

        return this.eventCloseTokenFromBounds(text, start, start + tokenLength);
    }

    private EventToken eventCloseTokenEndingAt(String text, int end) {
        if (end <= 0 || end > text.length()) {
            return null;
        }

        int tokenStart = text.lastIndexOf("[ie:", end - 1);
        if (tokenStart < 0) {
            return null;
        }

        int tokenLength = TextComponentUtil.structuredTokenLengthAt(text, tokenStart);
        if (tokenLength <= 0 || tokenStart + tokenLength != end) {
            return null;
        }
        return this.eventCloseTokenFromBounds(text, tokenStart, end);
    }

    private CleanupResult removeEmptyEventPairs(RichTextDocument document, int cursor, RichTextStyle defaultInsertionStyle) {
        RichTextDocument cleaned = document;
        int safeCursor = Math.clamp(cursor, 0, cleaned.length());
        boolean changed = false;

        DeletionRange pair;
        while ((pair = this.firstEmptyEventPair(cleaned.plainText())) != null) {
            cleaned = cleaned.copy();
            cleaned.replace(pair.start(), pair.end(), "", defaultInsertionStyle);
            safeCursor = this.cursorAfterDeletedRange(safeCursor, pair);
            changed = true;
        }
        return new CleanupResult(cleaned, safeCursor, changed);
    }

    private DeletionRange firstEmptyEventPair(String text) {
        for (int index = 0; index < text.length(); ) {
            EventToken open = this.eventOpenTokenStartingAt(text, index);
            if (open != null) {
                EventToken close = this.eventCloseTokenStartingAt(text, open.end());
                if (close != null && open.type().equals(close.type())) {
                    return new DeletionRange(open.start(), close.end());
                }
                index = open.end();
                continue;
            }

            int tokenLength = TextComponentUtil.structuredTokenLengthAt(text, index);
            if (tokenLength > 0) {
                index += tokenLength;
            } else {
                index += Character.charCount(text.codePointAt(index));
            }
        }
        return null;
    }

    private EventToken eventOpenTokenStartingAt(String text, int start) {
        if (start < 0 || start >= text.length()) {
            return null;
        }

        int tokenLength = TextComponentUtil.structuredTokenLengthAt(text, start);
        if (tokenLength <= 0) {
            return null;
        }

        return this.eventOpenTokenFromBounds(text, start, start + tokenLength);
    }

    private EventToken eventOpenTokenFromBounds(String text, int tokenStart, int tokenEnd) {
        String body = text.substring(tokenStart + "[ie:".length(), tokenEnd - 1);
        if (body.startsWith("click:")) {
            return new EventToken("click", tokenStart, tokenEnd);
        }
        if (body.startsWith("hover:")) {
            return new EventToken("hover", tokenStart, tokenEnd);
        }
        return null;
    }

    private EventToken eventCloseTokenFromBounds(String text, int tokenStart, int tokenEnd) {
        String body = text.substring(tokenStart + "[ie:".length(), tokenEnd - 1);
        return switch (body) {
            case "/click" -> new EventToken("click", tokenStart, tokenEnd);
            case "/hover" -> new EventToken("hover", tokenStart, tokenEnd);
            default -> null;
        };
    }

    private EventToken matchingOpenBeforeClose(String text, EventToken close) {
        ArrayDeque<EventToken> stack = new ArrayDeque<>();
        for (int index = 0; index < close.start(); ) {
            int tokenLength = TextComponentUtil.structuredTokenLengthAt(text, index);
            if (tokenLength <= 0) {
                index += Character.charCount(text.codePointAt(index));
                continue;
            }

            int tokenEnd = index + tokenLength;
            EventToken open = this.eventOpenTokenFromBounds(text, index, tokenEnd);
            if (open != null) {
                stack.addLast(open);
            } else {
                EventToken nestedClose = this.eventCloseTokenFromBounds(text, index, tokenEnd);
                if (nestedClose != null && !stack.isEmpty() && stack.getLast().type().equals(nestedClose.type())) {
                    stack.removeLast();
                }
            }
            index = tokenEnd;
        }

        while (!stack.isEmpty()) {
            EventToken open = stack.removeLast();
            if (open.type().equals(close.type())) {
                return open;
            }
        }
        return null;
    }

    private EventToken matchingCloseAfterOpen(String text, EventToken open) {
        int depth = 0;
        for (int index = open.end(); index < text.length(); ) {
            int tokenLength = TextComponentUtil.structuredTokenLengthAt(text, index);
            if (tokenLength <= 0) {
                index += Character.charCount(text.codePointAt(index));
                continue;
            }

            int tokenEnd = index + tokenLength;
            EventToken nestedOpen = this.eventOpenTokenFromBounds(text, index, tokenEnd);
            if (nestedOpen != null && nestedOpen.type().equals(open.type())) {
                depth++;
                index = tokenEnd;
                continue;
            }

            EventToken close = this.eventCloseTokenFromBounds(text, index, tokenEnd);
            if (close != null && close.type().equals(open.type())) {
                if (depth == 0) {
                    return close;
                }
                depth--;
            }
            index = tokenEnd;
        }
        return null;
    }

    private int cursorAfterDeletedRange(int cursor, DeletionRange range) {
        if (cursor <= range.start()) {
            return cursor;
        }
        if (cursor <= range.end()) {
            return range.start();
        }
        return cursor - (range.end() - range.start());
    }

    RichTextDocument applyStyle(
            RichTextDocument source,
            int start,
            int end,
            UnaryOperator<RichTextStyle> transformer,
            RichTextStyle defaultInsertionStyle
    ) {
        RichTextDocument updated = source.copy();
        updated.applyStyle(start, end, style -> this.normalizedStyle(
                transformer.apply(style.equals(RichTextStyle.EMPTY) ? defaultInsertionStyle : style),
                defaultInsertionStyle
        ));
        return updated;
    }

    RichTextDocument applyGradient(
            RichTextDocument source,
            int start,
            int end,
            int startColor,
            int endColor
    ) {
        RichTextDocument updated = source.copy();
        updated.applyGradient(start, end, startColor, endColor);
        return updated;
    }

    TextTransformResult transformSelectionOrAll(
            RichTextDocument source,
            RichTextSelectionModel selection,
            UnaryOperator<String> transformer
    ) {
        if (source.isEmpty()) {
            return new TextTransformResult(source.copy(), selection.cursor(), selection.selectionCursor(), false, 0);
        }

        int start = selection.start();
        int end = selection.end();
        boolean hadSelection = selection.hasSelection();
        if (!hadSelection) {
            start = 0;
            end = source.length();
        }

        RichTextDocument updated = source.copy();
        int transformedLength = updated.transformText(start, end, transformer);

        int newCursor = hadSelection ? start + transformedLength : Math.min(selection.cursor(), updated.length());
        int newSelection = hadSelection ? start : newCursor;
        return new TextTransformResult(updated, newCursor, newSelection, hadSelection, transformedLength);
    }

    String toTitleCase(String text) {
        StringBuilder builder = new StringBuilder(text.length());
        boolean capitalizeNext = true;
        for (int index = 0; index < text.length(); index++) {
            char current = text.charAt(index);
            if (Character.isLetter(current)) {
                builder.append(capitalizeNext ? Character.toTitleCase(current) : Character.toLowerCase(current));
                capitalizeNext = false;
            } else {
                builder.append(current);
                capitalizeNext = Character.isWhitespace(current) || current == '-' || current == '_' || current == '/';
            }
        }
        return builder.toString();
    }

    private RichTextStyle normalizedStyle(RichTextStyle style, RichTextStyle defaultInsertionStyle) {
        return style.equals(defaultInsertionStyle) ? RichTextStyle.EMPTY : style;
    }

    record TextTransformResult(
            RichTextDocument document,
            int newCursor,
            int newSelection,
            boolean hadSelection,
            int transformedLength
    ) {
    }

    record TextChangeResult(RichTextDocument document, int cursorOverride) {
    }

    private record CleanupResult(RichTextDocument document, int cursor, boolean changed) {
    }

    private record DeletionRange(int start, int end) {
    }

    private record EventToken(String type, int start, int end) {
    }
}
