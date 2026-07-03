package me.noramibu.itemeditor.ui.component.raw;

import me.noramibu.itemeditor.ui.component.RawTextAreaComponent;
import me.noramibu.itemeditor.util.RawAutocompleteIndex;
import me.noramibu.itemeditor.util.RawAutocompleteUtil;
import me.noramibu.itemeditor.util.RawItemDataUtil;
import net.minecraft.core.RegistryAccess;

import java.util.List;

public final class RawAutocompleteUi {
    private RawAutocompleteUi() {
    }

    public static boolean applySelected(
            RawTextAreaComponent editor,
            RawAutocompleteUtil.AutocompleteResult result,
            int selectedIndex,
            RegistryAccess registryAccess,
            String fallbackItemId,
            List<String> lootTableIds
    ) {
        RawAutocompleteUtil.AutocompleteResult effective = resolve(
                editor,
                result,
                registryAccess,
                fallbackItemId,
                lootTableIds
        );
        if (effective.suggestions().isEmpty()) {
            return false;
        }
        int index = Math.clamp(selectedIndex, 0, effective.suggestions().size() - 1);
        return apply(editor, effective, effective.suggestions().get(index).insertText(), registryAccess);
    }

    public static int refresh(
            RawTextAreaComponent editor,
            RawAutocompleteUtil.AutocompleteResult result,
            int selected,
            boolean forced,
            boolean suppressed
    ) {
        if (result.suggestions().isEmpty() || suppressed || (!forced && !shouldAutoShow(editor, result))) {
            hide(editor);
            return 0;
        }

        boolean correctionMode = isCorrection(editor, result);
        boolean hasStructural = hasStructural(result);
        int predictive = firstGhostSuggestion(editor, result);
        if (!forced && predictive < 0 && !correctionMode && !hasStructural) {
            hide(editor);
            return 0;
        }

        int selectedSuggestion = selected < 0 || selected >= result.suggestions().size()
                ? Math.max(predictive, 0)
                : selected;
        String ghost = ghostSuffix(editor, result, result.suggestions().get(selectedSuggestion).insertText());
        if (ghost.isEmpty() && predictive >= 0 && predictive != selectedSuggestion) {
            selectedSuggestion = predictive;
            ghost = ghostSuffix(editor, result, result.suggestions().get(selectedSuggestion).insertText());
        }

        editor.ghostSuggestion(correctionMode ? "" : ghost);
        editor.autocompletePopup(popupEntries(result.suggestions()), selectedSuggestion);
        return selectedSuggestion;
    }

    private static RawAutocompleteUtil.AutocompleteResult resolve(
            RawTextAreaComponent editor,
            RawAutocompleteUtil.AutocompleteResult result,
            RegistryAccess registryAccess,
            String fallbackItemId,
            List<String> lootTableIds
    ) {
        if (editor.hasVirtualCaret() && !editor.hasSelection()) {
            return RawAutocompleteUtil.AutocompleteResult.empty(editor.caretIndex());
        }
        if (result.requestedCaret() == editor.caretIndex()) {
            return result;
        }
        RawAutocompleteIndex index = RawAutocompleteIndex.create(editor.getValue());
        return RawAutocompleteUtil.suggest(
                editor.getValue(),
                editor.caretIndex(),
                registryAccess,
                index,
                fallbackItemId,
                lootTableIds
        );
    }

    private static boolean apply(
            RawTextAreaComponent editor,
            RawAutocompleteUtil.AutocompleteResult result,
            String insertText,
            RegistryAccess registryAccess
    ) {
        if (insertText == null || insertText.isEmpty()) {
            return false;
        }
        if (editor.hasSelection()) {
            int start = Math.min(editor.caretIndex(), editor.selectionIndex());
            int end = Math.max(editor.caretIndex(), editor.selectionIndex());
            editor.replaceRange(start, end, insertText);
            return true;
        }
        int replaceStart = Math.clamp(result.replaceStart(), 0, editor.getValue().length());
        int replaceEnd = Math.clamp(result.replaceEnd(), replaceStart, editor.getValue().length());
        String insert = repairedInsert(editor.getValue(), replaceStart, replaceEnd, insertText, registryAccess);
        int caretOffset = snippetCaretOffset(insert);
        editor.replaceRange(replaceStart, replaceEnd, insert);
        if (caretOffset >= 0) {
            editor.moveCaret(replaceStart + caretOffset);
        }
        return true;
    }

    private static int snippetCaretOffset(String insertText) {
        int separator = insertText == null ? -1 : insertText.indexOf(": ");
        if (separator < 0) {
            return -1;
        }

        for (int index = separator + 2; index < insertText.length(); index++) {
            char value = insertText.charAt(index);
            if (Character.isWhitespace(value)) {
                continue;
            }
            return switch (value) {
                case '{', '[', '"' -> index + 1;
                default -> -1;
            };
        }
        return -1;
    }

    private static String repairedInsert(
            String text,
            int replaceStart,
            int replaceEnd,
            String insertText,
            RegistryAccess registryAccess
    ) {
        if (registryAccess == null || !text.substring(replaceEnd).isBlank()) {
            return insertText;
        }

        String candidate = text.substring(0, replaceStart) + insertText + text.substring(replaceEnd);
        if (RawItemDataUtil.parse(candidate, registryAccess).success()) {
            return insertText;
        }

        String closingSuffix = RawAutocompleteUtil.closingSuffix(candidate);
        if (!closingSuffix.isBlank()
                && RawItemDataUtil.parse(candidate + closingSuffix, registryAccess).success()) {
            return insertText + closingSuffix;
        }
        return insertText;
    }

    private static void hide(RawTextAreaComponent editor) {
        editor.clearAutocompleteOverlay();
    }

    private static String ghostSuffix(
            RawTextAreaComponent editor,
            RawAutocompleteUtil.AutocompleteResult result,
            String insertText
    ) {
        if (insertText == null || insertText.isEmpty() || editor.hasSelection()) {
            return "";
        }
        int caret = editor.caretIndex();
        int replaceStart = Math.clamp(result.replaceStart(), 0, caret);
        int replaceEnd = Math.clamp(result.replaceEnd(), replaceStart, editor.getValue().length());
        if (replaceEnd < caret) {
            return "";
        }
        int typedLength = caret - replaceStart;
        if (typedLength == 0) {
            return insertText;
        }
        if (typedLength >= insertText.length()) {
            return "";
        }
        String typed = editor.getValue().substring(replaceStart, caret);
        if (insertText.regionMatches(true, 0, typed, 0, typed.length())) {
            return insertText.substring(typed.length());
        }
        return namespaceLocalGhostSuffix(insertText, typed);
    }

    private static String namespaceLocalGhostSuffix(String insertText, String typed) {
        int separator = insertText.indexOf(':');
        if (separator < 0 || typed == null || typed.isBlank()) {
            return "";
        }
        String local = insertText.substring(separator + 1);
        int typedSeparator = typed.indexOf(':');
        String localTyped = typedSeparator >= 0 && typedSeparator + 1 < typed.length()
                ? typed.substring(typedSeparator + 1)
                : typed;
        return local.length() > localTyped.length()
                && local.regionMatches(true, 0, localTyped, 0, localTyped.length())
                ? local.substring(localTyped.length())
                : "";
    }

    private static int firstGhostSuggestion(
            RawTextAreaComponent editor,
            RawAutocompleteUtil.AutocompleteResult result
    ) {
        for (int index = 0; index < result.suggestions().size(); index++) {
            if (!ghostSuffix(editor, result, result.suggestions().get(index).insertText()).isEmpty()) {
                return index;
            }
        }
        return -1;
    }

    private static List<RawTextAreaComponent.AutocompletePopupEntry> popupEntries(
            List<RawAutocompleteUtil.Suggestion> suggestions
    ) {
        return suggestions.stream()
                .map(suggestion -> new RawTextAreaComponent.AutocompletePopupEntry(
                        suggestion.label(),
                        popupDescription(suggestion)
                ))
                .toList();
    }

    private static String popupDescription(RawAutocompleteUtil.Suggestion suggestion) {
        if (!suggestion.reason().isBlank()) {
            return suggestion.reason();
        }
        String insert = suggestion.insertText();
        if (insert == null || insert.equals(suggestion.label())) {
            return suggestion.kind().name();
        }
        if (suggestion.kind() != RawAutocompleteUtil.SuggestionKind.SNIPPET) {
            return insert;
        }

        int separator = insert.indexOf(": ");
        String value = separator < 0 ? insert : insert.substring(separator + 2);
        int newline = value.indexOf('\n');
        return newline < 0 ? value : value.substring(0, newline).trim();
    }

    private static boolean shouldAutoShow(
            RawTextAreaComponent editor,
            RawAutocompleteUtil.AutocompleteResult result
    ) {
        if (result.suggestions().isEmpty()) {
            return false;
        }
        if (editor.hasSelection()) {
            return true;
        }

        int caret = editor.caretIndex();
        int replaceStart = Math.clamp(result.replaceStart(), 0, caret);
        int replaceEnd = Math.clamp(result.replaceEnd(), replaceStart, editor.getValue().length());
        String typed = editor.getValue().substring(replaceStart, caret);
        if (replaceEnd < caret) {
            return true;
        }
        if (!typed.isBlank() && (shouldSuppressCompletedKeyEcho(editor, result, typed)
                || isCursorAtCompletedToken(editor, result)
                || (!hasPredictive(result, typed) && !hasStructural(result))
                || onlyEchoesTypedToken(result, typed))) {
            return false;
        }
        return replaceStart < caret || isTriggerAfterWhitespace(editor.getValue(), caret);
    }

    private static boolean isTriggerAfterWhitespace(String text, int caret) {
        if (caret == 0 || caret > text.length()) {
            return false;
        }
        char previous = previousTriggerChar(text, caret);
        return previous == ':' || previous == '"' || previous == '\'' || previous == '[' || previous == '{' || previous == ',';
    }

    private static char previousTriggerChar(String text, int caret) {
        for (int cursor = Math.clamp(caret - 1, 0, text.length() - 1); cursor >= 0; cursor--) {
            char value = text.charAt(cursor);
            if (value == '\n' || value == '\r') {
                return '\0';
            }
            if (!Character.isWhitespace(value)) {
                return value;
            }
        }
        return '\0';
    }

    private static boolean isCursorAtCompletedToken(
            RawTextAreaComponent editor,
            RawAutocompleteUtil.AutocompleteResult result
    ) {
        int caret = editor.caretIndex();
        String text = editor.getValue();
        if (caret == 0 || caret > text.length() || Math.clamp(result.replaceStart(), 0, caret) == caret) {
            return false;
        }
        int tokenEnd = caret;
        while (tokenEnd < text.length() && isTokenChar(text.charAt(tokenEnd))) {
            tokenEnd++;
        }
        return tokenEnd > caret;
    }

    private static boolean isTokenChar(char value) {
        return Character.isLetterOrDigit(value) || value == '_' || value == ':' || value == '.' || value == '-' || value == '/';
    }

    private static boolean hasPredictive(RawAutocompleteUtil.AutocompleteResult result, String typed) {
        return result.suggestions().stream().anyMatch(suggestion ->
                isPredictiveMatch(suggestion.insertText(), typed) || isPredictiveMatch(suggestion.label(), typed));
    }

    private static boolean isPredictiveMatch(String candidate, String typed) {
        if (candidate == null || typed == null) {
            return false;
        }
        if (candidate.length() > typed.length() && candidate.regionMatches(true, 0, typed, 0, typed.length())) {
            return true;
        }
        int candidateSeparator = candidate.indexOf(':');
        if (candidateSeparator < 0) {
            return false;
        }
        String localCandidate = candidate.substring(candidateSeparator + 1);
        int typedSeparator = typed.indexOf(':');
        String localTyped = typedSeparator >= 0 && typedSeparator + 1 < typed.length()
                ? typed.substring(typedSeparator + 1)
                : typed;
        return localCandidate.length() > localTyped.length()
                && localCandidate.regionMatches(true, 0, localTyped, 0, localTyped.length());
    }

    private static boolean onlyEchoesTypedToken(RawAutocompleteUtil.AutocompleteResult result, String typed) {
        return !result.suggestions().isEmpty()
                && result.suggestions().stream().allMatch(suggestion ->
                suggestion.insertText() != null && suggestion.insertText().equalsIgnoreCase(typed));
    }

    private static boolean hasStructural(RawAutocompleteUtil.AutocompleteResult result) {
        return result.suggestions().stream()
                .anyMatch(suggestion -> suggestion.kind() == RawAutocompleteUtil.SuggestionKind.STRUCTURAL);
    }

    private static boolean shouldSuppressCompletedKeyEcho(
            RawTextAreaComponent editor,
            RawAutocompleteUtil.AutocompleteResult result,
            String typed
    ) {
        if ((typed != null && typed.contains(":")) || !hasColonAheadOnSameLine(editor.getValue(), editor.caretIndex())) {
            return false;
        }

        String normalizedTyped = normalizeKeyToken(typed);
        for (RawAutocompleteUtil.Suggestion suggestion : result.suggestions()) {
            String insert = suggestion.insertText();
            if (suggestion.kind() == RawAutocompleteUtil.SuggestionKind.STRUCTURAL
                    && insert != null
                    && insert.trim().startsWith(":")) {
                continue;
            }
            if (insert != null && !normalizeKeyToken(insert).equalsIgnoreCase(normalizedTyped)) {
                return false;
            }
            String label = suggestion.label();
            if (label != null && !label.isBlank() && !normalizeKeyToken(label).equalsIgnoreCase(normalizedTyped)) {
                return false;
            }
        }
        return true;
    }

    private static String normalizeKeyToken(String token) {
        if (token == null || token.isBlank()) {
            return "";
        }
        String value = token.trim();
        if (value.endsWith(":")) {
            value = value.substring(0, value.length() - 1).trim();
        }
        return value.length() >= 2 && value.charAt(0) == '"' && value.charAt(value.length() - 1) == '"'
                ? value.substring(1, value.length() - 1)
                : value;
    }

    private static boolean hasColonAheadOnSameLine(String text, int cursor) {
        if (text == null || text.isBlank()) {
            return false;
        }
        int safeCursor = Math.clamp(cursor, 0, text.length());
        int lineStart = text.lastIndexOf('\n', Math.max(0, safeCursor - 1)) + 1;
        boolean inString = false;
        boolean escaping = false;
        char quote = '\0';
        for (int index = lineStart; index < safeCursor; index++) {
            char value = text.charAt(index);
            if (escaping) {
                escaping = false;
            } else if (inString && value == '\\') {
                escaping = true;
            } else if (inString && value == quote) {
                inString = false;
            } else if (!inString && (value == '"' || value == '\'')) {
                inString = true;
                quote = value;
            }
        }
        for (int index = safeCursor; index < text.length(); index++) {
            char value = text.charAt(index);
            if (value == '\n' || value == '\r') {
                return false;
            }
            if (inString) {
                if (escaping) {
                    escaping = false;
                } else if (value == '\\') {
                    escaping = true;
                } else if (value == quote) {
                    inString = false;
                }
                continue;
            }
            if (value == '"' || value == '\'') {
                inString = true;
                quote = value;
                continue;
            }
            if (!Character.isWhitespace(value)) {
                return value == ':';
            }
        }
        return false;
    }

    private static boolean isCorrection(RawTextAreaComponent editor, RawAutocompleteUtil.AutocompleteResult result) {
        int caret = editor.caretIndex();
        int replaceStart = Math.clamp(result.replaceStart(), 0, editor.getValue().length());
        int replaceEnd = Math.clamp(result.replaceEnd(), replaceStart, editor.getValue().length());
        return replaceStart < caret && replaceEnd < caret;
    }

}
