package me.noramibu.itemeditor.ui.component;

import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.Surface;
import me.noramibu.itemeditor.util.ItemEditorText;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public final class RawItemDataDialog {

    private static final int DIALOG_WIDTH = 760;
    private static final int DIALOG_GAP = 8;
    private static final int DATA_HEIGHT = 360;
    private static final int BODY_TEXT_MARGIN = 32;
    private static final int LINE_TEXT_MARGIN = 48;
    private static final int HEADER_RESERVE_EMPTY_BODY = 84;
    private static final int HEADER_RESERVE_WITH_BODY = 104;
    private static final int DATA_MIN_HEIGHT = 150;
    private static final int DIALOG_MIN_HEIGHT = 260;
    private static final int ROW_PADDING_TOP_BOTTOM = 1;
    private static final int ROW_PADDING_LEFT_RIGHT = 4;
    private static final int LINE_NUMBER_MIN_DIGITS = 2;
    private static final int COLOR_TEXT = 0xA9B5C0;
    private static final int COLOR_LINE_NUMBER = 0x738194;
    private static final int COLOR_KEY = 0x7EC8F8;
    private static final int COLOR_STRING = 0xA7F3D0;
    private static final int COLOR_NUMBER = 0xF2C26B;
    private static final int COLOR_BOOLEAN = 0xF9A8D4;
    private static final int COLOR_NULL = 0xFDA4AF;
    private static final int COLOR_IDENTIFIER = 0x93C5FD;
    private static final int COLOR_PUNCT = 0xC4B5FD;
    private static final int COLOR_OPERATOR = 0x9CA3AF;
    private static final int COLOR_ROW_TEXT = 0xA9B5C0;
    private static final Pattern NUMBER_PATTERN = Pattern.compile("[-+]?\\d+(?:\\.\\d+)?(?:[eE][-+]?\\d+)?[bBsSlLfFdD]?");
    private static final String PUNCTUATION = "{}[]()";
    private static final String OPERATORS = ":,;=";
    private static final int COMPACT_BUTTON_WIDTH_THRESHOLD = 540;
    private static final int COMPACT_BUTTON_ROWS = 4;
    private static final int COMPACT_BUTTON_EXTRA = 26;
    private static final int REGULAR_BUTTON_EXTRA = 16;
    private static final int FOOTER_BUTTON_MIN_WIDTH = 64;
    private static final int FOOTER_BUTTON_MAX_WIDTH = 140;
    private static final int FOOTER_BUTTON_TEXT_MIN_WIDTH = 24;
    private static final int FOOTER_BUTTON_TEXT_RESERVE = 10;
    private static final int FOOTER_BUTTON_ROW_RESERVE = 80;
    private static final int FOOTER_BUTTON_COUNT = 4;
    private static final String EMPTY_TEXT = "";
    private static final String BLANK_RENDERED_TEXT = " ";

    private RawItemDataDialog() {
    }

    public static FlowLayout create(
            String title,
            String body,
            List<Line> rawLines,
            Runnable onCopy,
            Runnable onExportNbt,
            Runnable onExportJson,
            Runnable onClose
    ) {
        return create(
                title,
                body,
                rawLines,
                COMPACT_BUTTON_ROWS,
                FOOTER_BUTTON_COUNT,
                new DialogUiUtil.FooterAction(ItemEditorText.tr("common.copy"), button -> onCopy.run()),
                new DialogUiUtil.FooterAction(ItemEditorText.tr("dialog.raw_data.export_nbt"), button -> onExportNbt.run()),
                new DialogUiUtil.FooterAction(ItemEditorText.tr("dialog.raw_data.export_json"), button -> onExportJson.run()),
                new DialogUiUtil.FooterAction(ItemEditorText.tr("common.close"), button -> onClose.run())
        );
    }

    public static FlowLayout createConfirmation(
            String title,
            String body,
            List<Line> rawLines,
            Component confirmText,
            Runnable onConfirm,
            Component cancelText,
            Runnable onCancel
    ) {
        return create(
                title,
                body,
                rawLines,
                2,
                2,
                new DialogUiUtil.FooterAction(cancelText, button -> onCancel.run()),
                new DialogUiUtil.FooterAction(confirmText, button -> onConfirm.run())
        );
    }

    private static FlowLayout create(
            String title,
            String body,
            List<Line> rawLines,
            int compactButtonRows,
            int footerButtonCount,
            DialogUiUtil.FooterAction... footerActions
    ) {
        String safeTitle = safeText(title);
        String safeBody = safeText(body);
        FlowLayout overlay = DialogUiUtil.overlay();
        int dialogWidth = DialogUiUtil.dialogWidth(DIALOG_WIDTH);
        int bodyTextWidth = DialogUiUtil.dialogTextWidth(dialogWidth, BODY_TEXT_MARGIN);
        int lineTextWidth = DialogUiUtil.dialogTextWidth(dialogWidth, LINE_TEXT_MARGIN);
        boolean compactButtons = DialogUiUtil.compactButtons(dialogWidth, COMPACT_BUTTON_WIDTH_THRESHOLD);
        int buttonReserve = DialogUiUtil.buttonRowReserve(
                compactButtons,
                compactButtonRows,
                COMPACT_BUTTON_EXTRA,
                REGULAR_BUTTON_EXTRA
        );
        int headerReserve = UiFactory.scaledPixels(safeBody.isBlank() ? HEADER_RESERVE_EMPTY_BODY : HEADER_RESERVE_WITH_BODY);
        DialogUiUtil.ScrollDialogSizing sizing = DialogUiUtil.scrollDialogSizing(
                DATA_HEIGHT,
                headerReserve + buttonReserve,
                DATA_MIN_HEIGHT,
                DIALOG_MIN_HEIGHT
        );

        FlowLayout dialog = DialogUiUtil.dialogCard(dialogWidth, sizing.dialogHeight(), DIALOG_GAP);
        dialog.child(UiFactory.title(safeTitle));
        dialog.child(UiFactory.muted(safeBody, bodyTextWidth));

        FlowLayout lines = UiFactory.column();
        int lineNumberDigits = Math.max(LINE_NUMBER_MIN_DIGITS, String.valueOf(Math.max(1, rawLines.size())).length());
        int lineIndex = 1;
        for (Line line : rawLines) {
            FlowLayout row = UiFactory.row();
            row.padding(Insets.of(ROW_PADDING_TOP_BOTTOM, ROW_PADDING_TOP_BOTTOM, ROW_PADDING_LEFT_RIGHT, ROW_PADDING_LEFT_RIGHT));
            row.surface(Surface.flat(line.backgroundColor()));
            row.child(UiFactory.message(renderLine(line, lineIndex, lineNumberDigits), COLOR_ROW_TEXT).maxWidth(lineTextWidth));
            lines.child(row);
            lineIndex++;
        }

        dialog.child(DialogUiUtil.scrollCard(lines, sizing.contentHeight()));

        FlowLayout buttonRow = DialogUiUtil.footerRowByCount(
                dialogWidth,
                compactButtons,
                FOOTER_BUTTON_MIN_WIDTH,
                FOOTER_BUTTON_MAX_WIDTH,
                Math.max(1, footerButtonCount),
                FOOTER_BUTTON_ROW_RESERVE,
                FOOTER_BUTTON_TEXT_MIN_WIDTH,
                FOOTER_BUTTON_TEXT_RESERVE,
                footerActions
        );

        dialog.child(buttonRow);
        overlay.child(dialog);
        return overlay;
    }

    private static Component renderLine(Line line, int lineNumber, int lineNumberDigits) {
        MutableComponent rendered = Component.empty();
        String numberText = String.format(Locale.ROOT, "%" + lineNumberDigits + "d ", lineNumber);
        rendered.append(Component.literal(numberText).withStyle(style -> style.withColor(COLOR_LINE_NUMBER)));

        String rawText = line.text();
        if (rawText == null || rawText.isEmpty()) {
            rendered.append(Component.literal(BLANK_RENDERED_TEXT));
            return rendered;
        }

        String diffPrefix = "";
        String content = rawText;
        if (rawText.startsWith("+ ") || rawText.startsWith("- ") || rawText.startsWith("  ")) {
            diffPrefix = rawText.substring(0, 2);
            content = rawText.substring(2);
        }

        if (!diffPrefix.isEmpty()) {
            rendered.append(Component.literal(diffPrefix).withStyle(style -> style.withColor(COLOR_LINE_NUMBER)));
        }

        appendSyntaxColored(rendered, content);
        return rendered;
    }

    private static void appendSyntaxColored(MutableComponent output, String line) {
        int index = 0;
        while (index < line.length()) {
            char current = line.charAt(index);

            if (Character.isWhitespace(current)) {
                int next = index + 1;
                while (next < line.length() && Character.isWhitespace(line.charAt(next))) {
                    next++;
                }
                output.append(Component.literal(line.substring(index, next)).withStyle(style -> style.withColor(COLOR_TEXT)));
                index = next;
                continue;
            }

            if (isQuote(current)) {
                int next = scanQuotedString(line, index);
                String token = line.substring(index, next);
                int tokenColor = COLOR_STRING;
                if (isKeyToken(line, next)) {
                    tokenColor = COLOR_KEY;
                } else {
                    int hex = parseHexColorToken(token);
                    if (hex != -1) {
                        tokenColor = hex;
                    } else if (isResourceIdentifierLiteral(token)) {
                        tokenColor = COLOR_IDENTIFIER;
                    }
                }
                int finalTokenColor = tokenColor;
                output.append(Component.literal(token).withStyle(style -> style.withColor(finalTokenColor)));
                index = next;
                continue;
            }

            if (PUNCTUATION.indexOf(current) >= 0) {
                output.append(Component.literal(String.valueOf(current)).withStyle(style -> style.withColor(COLOR_PUNCT)));
                index++;
                continue;
            }
            if (OPERATORS.indexOf(current) >= 0) {
                output.append(Component.literal(String.valueOf(current)).withStyle(style -> style.withColor(COLOR_OPERATOR)));
                index++;
                continue;
            }

            int next = index + 1;
            while (next < line.length() && isWordChar(line.charAt(next))) {
                next++;
            }
            String token = line.substring(index, next);
            int tokenColor = resolveWordColor(line, token, next);
            output.append(Component.literal(token).withStyle(style -> style.withColor(tokenColor)));
            index = next;
        }
    }

    private static int resolveWordColor(String line, String token, int tokenEndExclusive) {
        if (isKeyToken(line, tokenEndExclusive)) {
            return COLOR_KEY;
        }
        if ("true".equalsIgnoreCase(token) || "false".equalsIgnoreCase(token)) {
            return COLOR_BOOLEAN;
        }
        if ("null".equalsIgnoreCase(token)) {
            return COLOR_NULL;
        }
        if (NUMBER_PATTERN.matcher(token).matches()) {
            return COLOR_NUMBER;
        }
        if (isResourceIdentifierWord(token)) {
            return COLOR_IDENTIFIER;
        }
        return COLOR_TEXT;
    }

    private static boolean isKeyToken(String line, int tokenEndExclusive) {
        int next = tokenEndExclusive;
        while (next < line.length() && Character.isWhitespace(line.charAt(next))) {
            next++;
        }
        return next < line.length() && line.charAt(next) == ':';
    }

    private static int scanQuotedString(String line, int start) {
        char quote = line.charAt(start);
        int index = start + 1;
        boolean escaping = false;
        while (index < line.length()) {
            char value = line.charAt(index);
            if (escaping) {
                escaping = false;
            } else if (value == '\\') {
                escaping = true;
            } else if (value == quote) {
                return index + 1;
            }
            index++;
        }
        return line.length();
    }

    private static int parseHexColorToken(String token) {
        if (token == null || token.length() < 3) {
            return -1;
        }
        char first = token.charAt(0);
        char last = token.charAt(token.length() - 1);
        if (!isQuote(first) || first != last || token.length() != 9) {
            return -1;
        }
        String value = token.substring(1, token.length() - 1);
        if (value.charAt(0) != '#') {
            return -1;
        }
        for (int i = 1; i < value.length(); i++) {
            if (Character.digit(value.charAt(i), 16) < 0) {
                return -1;
            }
        }
        try {
            return Integer.parseInt(value.substring(1), 16);
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private static boolean isResourceIdentifierLiteral(String token) {
        if (token.length() < 3) {
            return false;
        }
        char first = token.charAt(0);
        char last = token.charAt(token.length() - 1);
        if (!isQuote(first) || first != last) {
            return false;
        }
        return isResourceIdentifierWord(token.substring(1, token.length() - 1));
    }

    private static boolean isResourceIdentifierWord(String token) {
        if (token == null || !token.contains(":")) {
            return false;
        }
        for (int i = 0; i < token.length(); i++) {
            char value = token.charAt(i);
            if (Character.isLetterOrDigit(value) || value == ':' || value == '_' || value == '/' || value == '.' || value == '-') {
                continue;
            }
            return false;
        }
        return true;
    }

    private static boolean isQuote(char value) {
        return value == '"' || value == '\'';
    }

    private static boolean isWordChar(char value) {
        return !Character.isWhitespace(value)
                && PUNCTUATION.indexOf(value) < 0
                && OPERATORS.indexOf(value) < 0
                && value != '"'
                && value != '\'';
    }

    public record Line(String text, int backgroundColor) {
    }

    private static String safeText(String value) {
        return value == null ? EMPTY_TEXT : value;
    }

}
