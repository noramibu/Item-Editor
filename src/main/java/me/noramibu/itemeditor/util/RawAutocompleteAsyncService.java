package me.noramibu.itemeditor.util;

import net.minecraft.core.RegistryAccess;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public final class RawAutocompleteAsyncService {

    private static final ExecutorService EXECUTOR = AsyncDispatchUtil.newSingleThreadExecutor("itemeditor-raw-autocomplete");

    private final AtomicLong requestVersion = new AtomicLong();
    private final Object cacheLock = new Object();
    private RawAutocompleteIndex cachedIndex;
    private String cachedText = "";

    public void request(
            String text,
            int caret,
            RegistryAccess registryAccess,
            Consumer<RawAutocompleteUtil.AutocompleteResult> onResult
    ) {
        this.request(text, caret, registryAccess, "", null, onResult);
    }

    public void request(
            String text,
            int caret,
            RegistryAccess registryAccess,
            String fallbackItemId,
            Consumer<RawAutocompleteUtil.AutocompleteResult> onResult
    ) {
        this.request(text, caret, registryAccess, fallbackItemId, null, onResult);
    }

    public void request(
            String text,
            int caret,
            RegistryAccess registryAccess,
            String fallbackItemId,
            EditDelta editDelta,
            Consumer<RawAutocompleteUtil.AutocompleteResult> onResult
    ) {
        String safeText = text == null ? "" : text;
        String safeFallbackItemId = fallbackItemId == null ? "" : fallbackItemId;
        Consumer<RawAutocompleteUtil.AutocompleteResult> resultConsumer = AsyncDispatchUtil.nullSafeConsumer(onResult);
        AsyncDispatchUtil.requestLatest(this.requestVersion, EXECUTOR, () -> {
            RawAutocompleteIndex index = this.ensureIndex(safeText, editDelta);
            return RawAutocompleteUtil.suggest(safeText, caret, registryAccess, index, safeFallbackItemId);
        }, resultConsumer);
    }

    public void cancelPending() {
        this.requestVersion.incrementAndGet();
    }

    private RawAutocompleteIndex ensureIndex(String text, EditDelta editDelta) {
        synchronized (this.cacheLock) {
            if (this.cachedIndex == null || !this.cachedText.equals(text)) {
                boolean forceStructural = this.shouldForceStructuralReindex(editDelta);
                this.cachedIndex = RawAutocompleteIndex.update(
                        this.cachedIndex,
                        text,
                        editDelta == null ? 0 : editDelta.start(),
                        editDelta == null ? 0 : editDelta.end(),
                        editDelta == null ? "" : editDelta.replacement(),
                        forceStructural
                );
                this.cachedText = text;
            }
            return this.cachedIndex;
        }
    }

    private boolean shouldForceStructuralReindex(EditDelta editDelta) {
        if (editDelta == null || editDelta.structural() || this.cachedIndex == null || this.cachedText == null || this.cachedText.isEmpty()) {
            return true;
        }

        int safeStart = Math.clamp(editDelta.start(), 0, this.cachedText.length());
        int safeEnd = Math.clamp(editDelta.end(), safeStart, this.cachedText.length());
        if (this.editTouchesQuotedObjectKey(this.cachedText, safeStart, safeEnd)) {
            return true;
        }
        return this.editTouchesUnquotedObjectKey(this.cachedText, safeStart, safeEnd);
    }

    private boolean editTouchesQuotedObjectKey(String text, int start, int end) {
        int probe = Math.max(0, Math.min(start, text.length()));
        if (!this.insideStringAt(text, probe) && probe > 0 && this.insideStringAt(text, probe - 1)) {
            probe--;
        }
        if (!this.insideStringAt(text, probe)) {
            return false;
        }

        int quoteStart = this.findStringStart(text, probe);
        if (quoteStart < 0) {
            return false;
        }
        int quoteEnd = this.findStringEnd(text, quoteStart);
        if (quoteEnd < 0) {
            return true;
        }
        if (end <= quoteStart || start > quoteEnd) {
            return false;
        }
        int next = this.skipWhitespaceForward(text, quoteEnd + 1);
        return next < text.length() && text.charAt(next) == ':';
    }

    private boolean editTouchesUnquotedObjectKey(String text, int start, int end) {
        int tokenStart = this.findIdentifierStart(text, start);
        int tokenEnd = this.findIdentifierEnd(text, Math.max(start, end));
        if (tokenStart < 0 || tokenEnd <= tokenStart) {
            return false;
        }
        int next = this.skipWhitespaceForward(text, tokenEnd);
        return next < text.length() && text.charAt(next) == ':';
    }

    private boolean insideStringAt(String text, int index) {
        if (index <= 0 || text.isEmpty()) {
            return false;
        }
        int clamped = Math.clamp(index, 0, text.length());
        return this.cachedIndex.insideStringAt(clamped);
    }

    private int findStringStart(String text, int index) {
        int cursor = Math.clamp(index, 0, text.length() - 1);
        for (int position = cursor; position >= 0; position--) {
            if (text.charAt(position) != '"') {
                continue;
            }
            int backslashCount = 0;
            for (int probe = position - 1; probe >= 0 && text.charAt(probe) == '\\'; probe--) {
                backslashCount++;
            }
            if ((backslashCount & 1) == 0) {
                return position;
            }
        }
        return -1;
    }

    private int findStringEnd(String text, int quoteStart) {
        boolean escaping = false;
        for (int index = quoteStart + 1; index < text.length(); index++) {
            char value = text.charAt(index);
            if (escaping) {
                escaping = false;
                continue;
            }
            if (value == '\\') {
                escaping = true;
                continue;
            }
            if (value == '"') {
                return index;
            }
        }
        return -1;
    }

    private int findIdentifierStart(String text, int index) {
        if (text.isEmpty()) {
            return -1;
        }
        int cursor = Math.clamp(index, 0, text.length() - 1);
        if (!isIdentifierChar(text.charAt(cursor))) {
            if (cursor > 0 && isIdentifierChar(text.charAt(cursor - 1))) {
                cursor--;
            } else {
                return -1;
            }
        }
        while (cursor > 0 && isIdentifierChar(text.charAt(cursor - 1))) {
            cursor--;
        }
        return cursor;
    }

    private int findIdentifierEnd(String text, int index) {
        if (text.isEmpty()) {
            return -1;
        }
        int cursor = Math.clamp(index, 0, text.length());
        while (cursor < text.length() && isIdentifierChar(text.charAt(cursor))) {
            cursor++;
        }
        return cursor;
    }

    private int skipWhitespaceForward(String text, int index) {
        int cursor = Math.max(0, index);
        while (cursor < text.length() && Character.isWhitespace(text.charAt(cursor))) {
            cursor++;
        }
        return cursor;
    }

    private static boolean isIdentifierChar(char value) {
        return Character.isLetterOrDigit(value)
                || value == '_'
                || value == '-'
                || value == '.'
                || value == ':';
    }

    public record EditDelta(int start, int end, String replacement, boolean structural) {
        public EditDelta {
            replacement = replacement == null ? "" : replacement;
        }
    }
}
