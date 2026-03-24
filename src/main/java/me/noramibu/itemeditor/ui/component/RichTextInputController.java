package me.noramibu.itemeditor.ui.component;

import me.noramibu.itemeditor.editor.text.RichTextDocument;
import me.noramibu.itemeditor.editor.text.RichTextStyle;

import java.util.function.UnaryOperator;

final class RichTextInputController {

    RichTextDocument updateDocumentFromPlainTextChange(
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

        RichTextDocument updated = previousDocument.copy();
        updated.replace(prefix, beforeSuffix, afterText.substring(prefix, afterSuffix), this.normalizedStyle(insertionStyle, defaultInsertionStyle));
        return updated;
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
                transformer.apply(this.resolveStyle(style, defaultInsertionStyle)),
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

    private RichTextStyle resolveStyle(RichTextStyle style, RichTextStyle defaultInsertionStyle) {
        return style.equals(RichTextStyle.EMPTY) ? defaultInsertionStyle : style;
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
}
