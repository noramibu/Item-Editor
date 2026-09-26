package me.noramibu.itemeditor.ui.component.raw;

import com.mojang.brigadier.suggestion.Suggestion;
import io.wispforest.owo.ui.core.UIComponent.FocusSource;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import me.noramibu.itemeditor.ui.component.RawTextAreaComponent;
import me.noramibu.itemeditor.util.ItemEditorText;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/** Shared, cursor-relative command completion for inline and expanded editors. */
public final class CommandSuggestions {
    private final RawTextAreaComponent editor;
    private final BooleanSupplier active;
    private final Consumer<Component> status;
    private List<Suggestion> suggestions = List.of();
    private String text = "";
    private int caret;
    private int selected;
    private int generation;

    public CommandSuggestions(RawTextAreaComponent editor, BooleanSupplier active, Consumer<Component> status) {
        this.editor = editor;
        this.active = active;
        this.status = status;
        editor.onAutocompleteRefreshRequested(() -> {
                    this.request();
                    return true;
                })
                .onAutocompleteRequested(this::accept)
                .onAutocompleteNextRequested(() -> this.move(1))
                .onAutocompletePreviousRequested(() -> this.move(-1))
                .onAutocompleteDismissed(this::dismiss);
        editor.onChanged().subscribe((value, delta) -> this.dismiss());
    }

    public void request() {
        if (!this.active.getAsBoolean()) return;
        this.dismiss();
        if (this.editor.focusHandler() != null)
            this.editor.focusHandler().focus(this.editor, FocusSource.KEYBOARD_CYCLE);
        var minecraft = Minecraft.getInstance();
        var connection = minecraft.getConnection();
        if (connection == null || this.editor.getValue().length() > CommandTextTools.MAX_PARSE_LENGTH) {
            this.status.accept(ItemEditorText.tr("command_editor.unchecked"));
            return;
        }
        this.text = this.editor.getValue();
        this.caret = this.editor.caretIndex();
        int request = this.generation;
        this.status.accept(ItemEditorText.tr("raw_editor.loading"));
        try {
            var parsed = connection
                    .getCommands()
                    .parse(CommandTextTools.reader(this.text), connection.getSuggestionsProvider());
            connection
                    .getCommands()
                    .getCompletionSuggestions(parsed, this.caret)
                    .whenComplete((result, error) -> minecraft.execute(() -> {
                        if (request != this.generation || !this.active.getAsBoolean() || this.cursorChanged()) return;
                        if (error != null) {
                            this.status.accept(ItemEditorText.tr("command_editor.unchecked"));
                            return;
                        }
                        this.suggestions = result.getList().stream().limit(80).toList();
                        this.selected = 0;
                        this.show();
                        this.status.accept(
                                this.suggestions.isEmpty()
                                        ? ItemEditorText.tr("command_editor.no_suggestions")
                                        : Component.empty());
                    }));
        } catch (RuntimeException | StackOverflowError error) {
            this.status.accept(ItemEditorText.tr("command_editor.unchecked"));
        }
    }

    private boolean cursorChanged() {
        return this.caret != this.editor.caretIndex() || !this.text.equals(this.editor.getValue());
    }

    private void show() {
        this.editor.autocompletePopup(
                this.suggestions.stream()
                        .map(value -> new RawTextAreaComponent.AutocompletePopupEntry(
                                value.getText(),
                                value.getTooltip() == null
                                        ? ""
                                        : value.getTooltip().getString()))
                        .toList(),
                this.selected);
    }

    private boolean move(int direction) {
        if (this.suggestions.isEmpty()) return false;
        this.selected = Math.floorMod(this.selected + direction, this.suggestions.size());
        this.show();
        return true;
    }

    private boolean accept() {
        if (this.suggestions.isEmpty()) return false;
        if (this.cursorChanged()) {
            this.dismiss();
            return false;
        }
        Suggestion suggestion = this.suggestions.get(this.selected);
        this.editor.replaceRange(
                suggestion.getRange().getStart(), suggestion.getRange().getEnd(), suggestion.getText());
        this.dismiss();
        return true;
    }

    public boolean dismiss() {
        boolean visible = !this.suggestions.isEmpty();
        this.generation++;
        this.suggestions = List.of();
        this.editor.clearAutocompleteOverlay();
        return visible;
    }
}
