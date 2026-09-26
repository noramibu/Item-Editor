package me.noramibu.itemeditor.ui.component;

import com.mojang.blaze3d.platform.InputConstants;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.UIComponent.FocusSource;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;
import me.noramibu.itemeditor.ui.component.raw.CommandTextTools;
import me.noramibu.itemeditor.ui.component.raw.RawEmbeddedString;
import me.noramibu.itemeditor.util.ItemEditorText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

/** Keeps editable formatting separate from the stored command's whitespace. */
public final class RawStringEditorDialog {
    private final FlowLayout overlay = DialogUiUtil.overlay();
    private final Deque<Page> pages = new ArrayDeque<>();
    private final Consumer<String> apply;
    private final Runnable close;
    private RawTextAreaComponent visible;
    private LabelComponent status;
    private boolean busy;
    private boolean confirming;
    private boolean focusPending;
    private int generation;

    public RawStringEditorDialog(
            RawEmbeddedString.Slice slice, String formatted, Consumer<String> apply, Runnable close) {
        this.apply = apply;
        this.close = close;
        this.pages.push(new Page(slice, formatted));
        this.show();
    }

    public FlowLayout component() {
        return this.overlay;
    }

    public void showError(Component message) {
        this.status.text(message);
    }

    public void resize() {
        this.cancelWork();
        this.show();
    }

    private void cancelWork() {
        this.generation++;
        this.busy = false;
        this.pages.peek().readOnly(false);
    }

    private void show() {
        this.confirming = false;
        this.overlay.clearChildren();
        Page page = this.pages.peek();
        int width = DialogUiUtil.dialogWidth(900);
        FlowLayout card = DialogUiUtil.dialogCard(width, DialogUiUtil.dialogHeight(560, 220), 4);
        card.child(UiFactory.title(ItemEditorText.tr("raw_editor.string.title", this.pages.size())));
        ButtonComponent formatted = UiFactory.button(
                ItemEditorText.tr("raw_editor.string.formatted"),
                UiFactory.ButtonTextPreset.COMPACT,
                button -> this.showFormatted());
        ButtonComponent edit = UiFactory.button(
                ItemEditorText.tr("raw_editor.string.edit"),
                UiFactory.ButtonTextPreset.COMPACT,
                button -> this.withDecoded(() -> {
                    page.editing = true;
                    this.show();
                }));
        ButtonComponent open = UiFactory.button(
                ItemEditorText.tr("raw_editor.string.open"),
                UiFactory.ButtonTextPreset.COMPACT,
                button -> this.withDecoded(this::openNestedDecoded));
        open.tooltip(List.of(ItemEditorText.tr("raw_editor.string.tooltip")));
        formatted.active(page.editing);
        edit.active(!page.editing);
        card.child(UiFactory.actionButtonRow(false, formatted, edit, open));
        card.child(UiFactory.muted(
                        ItemEditorText.tr(page.editing ? "raw_editor.string.edit_hint" : "raw_editor.string.view_hint"),
                        width - 32)
                .horizontalSizing(Sizing.fill(100)));
        this.visible = page.editing ? page.editor : page.preview;
        card.child(this.visible);
        this.status = UiFactory.muted("");
        this.status.horizontalSizing(Sizing.fill(100));
        card.child(this.status);
        ButtonComponent cancel = UiFactory.negativeButton(
                ItemEditorText.tr(this.pages.size() > 1 ? "raw_editor.string.back" : "common.cancel"),
                UiFactory.ButtonTextPreset.COMPACT,
                button -> this.requestClose());
        ButtonComponent copy = UiFactory.button(
                ItemEditorText.tr("common.copy"),
                UiFactory.ButtonTextPreset.COMPACT,
                button -> Minecraft.getInstance().keyboardHandler.setClipboard(this.visible.getValue()));
        copy.tooltip(List.of(
                ItemEditorText.tr(page.editing ? "raw_editor.string.copy_edit" : "raw_editor.string.copy_view")));
        ButtonComponent copyOriginal =
                page.editing ? null : CommandEditorControls.copyOriginalButton(page.editor::getValue);
        ButtonComponent save = UiFactory.positiveButton(
                ItemEditorText.tr("raw_editor.string.apply"),
                UiFactory.ButtonTextPreset.COMPACT,
                button -> this.apply());
        save.active(page.changed());
        page.save = save;
        card.child(UiFactory.actionButtonRow(true, cancel, copy, copyOriginal, save));
        this.overlay.child(card);
        this.focusPending = true;
    }

    public void tick() {
        // Construction and resize happen before the overlay has a parent or focus handler.
        if (!this.focusPending || this.confirming || !this.overlay.hasParent() || this.visible.focusHandler() == null)
            return;
        this.visible.focusHandler().focus(this.visible, FocusSource.KEYBOARD_CYCLE);
        this.focusPending = false;
    }

    private void showFormatted() {
        Page page = this.pages.peek();
        String text = page.editor.getValue();
        this.work(() -> RawEmbeddedString.format(text), result -> {
            page.formatted = result;
            page.preview.text(result);
            page.editing = false;
            this.show();
        });
    }

    private void openNestedDecoded() {
        if (this.pages.size() >= 32) {
            this.status.text(ItemEditorText.tr("raw_editor.string.too_large"));
            return;
        }
        Page page = this.pages.peek();
        String source = page.editor.getValue(), display = this.visible.getValue();
        int caret = this.visible.caretIndex(), anchor = this.visible.selectionIndex();
        this.work(
                () -> {
                    var slice = RawEmbeddedString.fromView(source, display, caret, anchor);
                    if (slice == null) throw new IllegalArgumentException("select");
                    return new Prepared(slice, RawEmbeddedString.format(slice.value()));
                },
                prepared -> {
                    this.pages.push(new Page(prepared.slice(), prepared.formatted()));
                    this.show();
                });
    }

    private <T> void work(Supplier<T> work, Consumer<T> done) {
        if (this.busy) return;
        this.busy = true;
        Page page = this.pages.peek();
        page.readOnly(true);
        int request = ++this.generation;
        this.status.text(ItemEditorText.tr("raw_editor.loading"));
        CompletableFuture.supplyAsync(work)
                .whenComplete((result, error) -> Minecraft.getInstance().execute(() -> {
                    if (request != this.generation || !this.overlay.hasParent()) return;
                    this.busy = false;
                    page.readOnly(false);
                    if (error == null) done.accept(result);
                    else this.status.text(ItemEditorText.tr("raw_editor.string." + errorKey(error)));
                }));
    }

    public static String errorKey(Throwable error) {
        while (error.getCause() != null) error = error.getCause();
        String key = String.valueOf(error.getMessage());
        return List.of("select", "too_large", "invalid").contains(key) ? key : "invalid";
    }

    private void apply() {
        this.withDecoded(this::applyDecoded);
    }

    private void withDecoded(Runnable next) {
        if (this.busy || this.confirming) return;
        Page page = this.pages.peek();
        String edited = page.preview.getValue();
        if (page.editing || page.formatted.equals(edited)) {
            next.run();
            return;
        }
        String source = page.editor.getValue(), baseline = page.formatted;
        this.work(() -> RawEmbeddedString.applyFormatted(source, baseline, edited), value -> {
            page.editor.replaceRange(0, source.length(), value);
            page.formatted = edited;
            page.refreshSave();
            next.run();
        });
    }

    private void applyDecoded() {
        Page page = this.pages.peek();
        if (!page.changed()) return;
        if (page.editor.getValue().length() > RawEmbeddedString.MAX_LENGTH) {
            this.status.text(ItemEditorText.tr("raw_editor.string.too_large"));
            return;
        }
        if (this.pages.size() == 1) this.apply.accept(page.editor.getValue());
        else {
            this.pages.pop();
            Page parent = this.pages.peek();
            parent.editor.replaceRange(
                    page.slice.start(), page.slice.end(), page.slice.encoded(page.editor.getValue()));
            boolean formatted = !parent.editing;
            parent.editing = true;
            this.show();
            if (formatted) this.showFormatted();
        }
    }

    public void requestClose() {
        this.cancelWork();
        if (!this.pages.peek().changed()) {
            this.back();
            return;
        }
        this.confirming = true;
        this.overlay.clearChildren();
        this.overlay.child(ConfirmationDialog.create(
                ItemEditorText.str("dialog.discard.title"),
                "",
                ItemEditorText.str("common.discard"),
                this::back,
                ItemEditorText.str("common.stay"),
                this::show));
    }

    private void back() {
        if (this.pages.size() == 1) this.close.run();
        else {
            this.pages.pop();
            this.show();
        }
    }

    public boolean handle(KeyEvent input) {
        if (input.key() == InputConstants.KEY_ESCAPE) {
            if (this.confirming) this.show();
            else this.requestClose();
            return true;
        }
        if (!input.hasControlDownWithQuirk()) return false;
        if (input.key() == InputConstants.KEY_S) {
            this.apply();
            return true;
        }
        return input.key() == InputConstants.KEY_R || input.key() == InputConstants.KEY_TAB;
    }

    private record Prepared(RawEmbeddedString.Slice slice, String formatted) {}

    private static final class Page {
        private final RawEmbeddedString.Slice slice;
        private final RawTextAreaComponent editor;
        private final RawTextAreaComponent preview;
        private final String originalDisplay;
        private boolean editing;
        private String formatted;
        private ButtonComponent save;

        private Page(RawEmbeddedString.Slice slice, String formatted) {
            this.slice = slice;
            String display = CommandTextTools.displayQuotedNewlines(slice.value());
            this.originalDisplay = display;
            this.formatted = display.equals(slice.value()) ? formatted : RawEmbeddedString.format(display);
            this.editor = new RawTextAreaComponent(Sizing.fill(100), Sizing.expand(100), display);
            this.preview = new RawTextAreaComponent(Sizing.fill(100), Sizing.expand(100), this.formatted);
            for (RawTextAreaComponent area : List.of(this.editor, this.preview)) {
                area.onChanged().subscribe((text, delta) -> this.refreshSave());
            }
        }

        private void refreshSave() {
            if (this.save != null) this.save.active(this.changed());
        }

        private void readOnly(boolean value) {
            this.editor.readOnly(value);
            this.preview.readOnly(value);
        }

        private boolean changed() {
            return !this.editor.getValue().equals(this.originalDisplay)
                    || (!this.editing && !this.preview.getValue().equals(this.formatted));
        }
    }
}
