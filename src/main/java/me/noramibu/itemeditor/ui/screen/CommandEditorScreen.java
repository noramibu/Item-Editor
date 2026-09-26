package me.noramibu.itemeditor.ui.screen;

import com.mojang.blaze3d.platform.InputConstants;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.StackLayout;
import io.wispforest.owo.ui.container.UIContainers;
import io.wispforest.owo.ui.core.Color;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.OwoUIAdapter;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.UIComponent.FocusSource;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import me.noramibu.itemeditor.ui.component.CommandEditorControls;
import me.noramibu.itemeditor.ui.component.CommandValidationLabel;
import me.noramibu.itemeditor.ui.component.ConfirmationDialog;
import me.noramibu.itemeditor.ui.component.RawStringEditorDialog;
import me.noramibu.itemeditor.ui.component.RawTextAreaComponent;
import me.noramibu.itemeditor.ui.component.RawTextSearchDialog;
import me.noramibu.itemeditor.ui.component.SearchablePickerDialog;
import me.noramibu.itemeditor.ui.component.UiFactory;
import me.noramibu.itemeditor.ui.component.raw.CommandSuggestions;
import me.noramibu.itemeditor.ui.component.raw.CommandTextTools;
import me.noramibu.itemeditor.ui.component.raw.RawEmbeddedString;
import me.noramibu.itemeditor.ui.util.MenuBackgroundSurface;
import me.noramibu.itemeditor.util.ItemEditorText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

/** A draft-only command workspace; command execution is deliberately unavailable. */
public final class CommandEditorScreen extends BaseOwoScreen<StackLayout> {
    private static final int WARNING = 0xFFD36A;
    private final Screen parent;
    private final String original;
    private final Consumer<String> onApply;
    private final RawTextAreaComponent editor;
    private StackLayout root;
    private FlowLayout shell;
    private FlowLayout modal;
    private RawStringEditorDialog stringEditor;
    private RawTextSearchDialog finder;
    private String query = "";
    private CommandValidationLabel status;
    private ButtonComponent applyButton;
    private ButtonComponent undoButton;
    private ButtonComponent redoButton;
    private final CommandSuggestions suggestions;
    private int revision;
    private boolean focusPending = true;
    private boolean wrap = true;
    private boolean busy;

    public CommandEditorScreen(Screen parent, String command, Consumer<String> onApply) {
        super(ItemEditorText.tr("command_editor.title"));
        this.parent = parent;
        this.original = command;
        this.onApply = onApply;
        this.editor = new RawTextAreaComponent(Sizing.fill(100), Sizing.expand(100), command).commandMode(true);
        this.editor.onChanged().subscribe((text, delta) -> {
            this.revision++;
            if (this.applyButton != null)
                this.applyButton.active(!this.editor.commandValue().equals(this.original));
        });
        this.suggestions = new CommandSuggestions(
                this.editor,
                () -> Minecraft.getInstance().screen == this && !this.busy && this.modal == null,
                this::setStatus);
        this.editor.onHistoryChanged(this::refreshHistoryButtons);
    }

    @Override
    protected @NotNull OwoUIAdapter<StackLayout> createAdapter() {
        return OwoUIAdapter.create(this, UIContainers::stack);
    }

    @Override
    protected void init() {
        super.init();
        this.focusPending = true;
        this.status.requestValidation();
        if (this.stringEditor != null) this.stringEditor.resize();
    }

    @Override
    protected void build(StackLayout root) {
        this.root = root;
        root.clearChildren();
        root.surface(MenuBackgroundSurface.standard());
        this.shell = UiFactory.column();
        this.shell.verticalSizing(Sizing.fill(100));
        this.shell.padding(Insets.of(6));
        this.shell.child(
                UiFactory.title(ItemEditorText.tr("command_editor.title")).horizontalSizing(Sizing.fill(100)));
        ButtonComponent wrapButton = UiFactory.button(this.wrapLabel(), UiFactory.ButtonTextPreset.COMPACT, button -> {
            this.wrap = !this.wrap;
            this.editor.wordWrap(this.wrap);
            button.setMessage(this.wrapLabel());
        });
        wrapButton.tooltip(UiFactory.tooltipLines(ItemEditorText.tr("command_editor.wrap_tooltip"), 260));
        this.shell.child(UiFactory.actionButtonRow(
                this.action("command_editor.suggest", this.suggestions::request),
                this.action("raw_editor.find.title", () -> this.openFinder(0)),
                this.action("raw_editor.string.open", this::openEmbedded),
                this.action("command_editor.assembly", this::openAssembly),
                wrapButton));
        this.shell.child(CommandEditorControls.create(this.editor));
        this.shell.child(this.editor.wordWrap(this.wrap));
        this.status = new CommandValidationLabel(this.editor, () -> !this.busy && this.modal == null);
        this.shell.child(this.status);
        this.applyButton = UiFactory.positiveButton(
                ItemEditorText.tr("screen.nested.apply"), UiFactory.ButtonTextPreset.COMPACT, button -> this.apply());
        this.applyButton.active(!this.editor.commandValue().equals(this.original));
        this.applyButton.tooltip(UiFactory.tooltipLines(ItemEditorText.tr("command_editor.apply_tooltip"), 260));
        this.undoButton = this.action("raw_editor.undo", this.editor::undo);
        this.redoButton = this.action("raw_editor.redo", this.editor::redo);
        this.refreshHistoryButtons();
        ButtonComponent cancel = UiFactory.negativeButton(
                ItemEditorText.tr("common.cancel"), UiFactory.ButtonTextPreset.COMPACT, button -> this.onClose());
        cancel.tooltip(UiFactory.tooltipLines(ItemEditorText.tr("command_editor.cancel_tooltip"), 260));
        this.shell.child(UiFactory.actionButtonRow(this.undoButton, this.redoButton, cancel, this.applyButton));
        root.child(this.shell);
        if (this.stringEditor != null) {
            this.stringEditor.resize();
            this.modal = this.stringEditor.component();
            root.child(this.modal);
        } else if (this.modal != null) {
            this.modal = null;
        }
        this.focusPending = true;
    }

    private ButtonComponent action(String key, Runnable action) {
        ButtonComponent button =
                UiFactory.button(ItemEditorText.tr(key), UiFactory.ButtonTextPreset.COMPACT, ignored -> {
                    if (!this.busy) action.run();
                });
        String tooltip =
                switch (key) {
                    case "raw_editor.find.title" -> "command_editor.find_tooltip";
                    case "command_editor.suggest" -> "command_editor.suggest_hint";
                    case "raw_editor.string.open" -> "command_editor.embedded_hint";
                    case "command_editor.assembly" -> "command_editor.assembly_tooltip";
                    case "raw_editor.undo" -> "command_editor.undo_tooltip";
                    case "raw_editor.redo" -> "command_editor.redo_tooltip";
                    default -> key;
                };
        button.tooltip(UiFactory.tooltipLines(ItemEditorText.tr(tooltip), 260));
        return button;
    }

    private Component wrapLabel() {
        return ItemEditorText.tr("raw_editor.word_wrap")
                .copy()
                .append(": ")
                .append(ItemEditorText.tr(this.wrap ? "common.true" : "common.false"))
                .withColor(this.wrap ? 0x91E68C : 0xFF8A8A);
    }

    private void refreshHistoryButtons() {
        if (this.undoButton != null) this.undoButton.active(this.editor.canUndo());
        if (this.redoButton != null) this.redoButton.active(this.editor.canRedo());
    }

    private void setStatus(Component text) {
        this.status.text(text);
        this.status.color(Color.ofRgb(WARNING));
    }

    @Override
    public void tick() {
        super.tick();
        if (this.stringEditor != null) this.stringEditor.tick();
        if (this.focusPending && this.editor.focusHandler() != null) {
            this.focusPending = false;
            if (this.modal == null) this.editor.focusHandler().focus(this.editor, FocusSource.KEYBOARD_CYCLE);
            if (this.finder != null && !this.finder.isClosed() && this.modal == null) this.finder.rebind(this.editor);
        }
        if (this.finder != null && this.modal == null) this.finder.tick();
    }

    private void openFinder(int tab) {
        if (this.modal != null || this.busy) return;
        if (this.finder == null || this.finder.isClosed())
            this.finder = new RawTextSearchDialog(this.editor, this.shell, this.query, value -> this.query = value);
        else this.finder.rebind(this.editor);
        this.finder.openTab(tab);
    }

    private <T> void inspect(Supplier<T> task, Consumer<T> ready) {
        if (this.busy || this.modal != null) return;
        this.busy = true;
        this.editor.readOnly(true);
        int request = ++this.revision;
        this.setStatus(ItemEditorText.tr("raw_editor.loading"));
        CompletableFuture.supplyAsync(task)
                .whenComplete((value, error) -> Minecraft.getInstance().execute(() -> {
                    if (request != this.revision || Minecraft.getInstance().screen != this) return;
                    this.busy = false;
                    this.editor.readOnly(false);
                    if (error != null)
                        this.setStatus(ItemEditorText.tr("raw_editor.string." + RawStringEditorDialog.errorKey(error)));
                    else ready.accept(value);
                }));
    }

    private void openEmbedded() {
        String text = this.editor.getValue();
        int caret = this.editor.caretIndex(), anchor = this.editor.selectionIndex();
        this.inspect(
                () -> {
                    var embedded = CommandTextTools.at(text, caret, anchor);
                    if (embedded == null) throw new IllegalArgumentException("select");
                    String value = embedded.string() == null
                            ? text.substring(
                                    embedded.range().start(), embedded.range().end())
                            : embedded.string().value();
                    return new PreparedEmbedded(embedded, value, RawEmbeddedString.format(value));
                },
                prepared -> {
                    var embedded = prepared.embedded();
                    var slice = new RawEmbeddedString.Slice(
                            embedded.range().start(), embedded.range().end(), prepared.value());
                    this.stringEditor = new RawStringEditorDialog(
                            slice,
                            prepared.formatted(),
                            value -> {
                                try {
                                    String replacement = embedded.string() == null
                                            ? CommandTextTools.compactData(value)
                                            : slice.encoded(value);
                                    this.editor.replaceRange(slice.start(), slice.end(), replacement);
                                    this.closeModal();
                                } catch (IllegalArgumentException error) {
                                    this.stringEditor.showError(ItemEditorText.tr("raw_editor.string.invalid"));
                                }
                            },
                            this::closeModal);
                    this.showModal(this.stringEditor.component());
                });
    }

    private void openAssembly() {
        String text = this.editor.getValue();
        this.inspect(() -> CommandTextTools.commands(text), commands -> {
            if (commands.isEmpty()) {
                this.setStatus(ItemEditorText.tr("command_editor.no_commands"));
                return;
            }
            this.showModal(SearchablePickerDialog.create(
                    ItemEditorText.str("command_editor.assembly"),
                    ItemEditorText.str("command_editor.assembly_hint", commands.size()),
                    IntStream.range(0, commands.size())
                            .mapToObj(Integer::toString)
                            .toList(),
                    index -> (Integer.parseInt(index) + 1) + ": "
                            + commands.get(Integer.parseInt(index)).value(),
                    index -> {
                        var slice = commands.get(Integer.parseInt(index));
                        this.closeModal();
                        this.editor.selectSearchMatch(slice.start(), slice.end());
                        Minecraft.getInstance()
                                .setScreen(new CommandEditorScreen(
                                        this,
                                        CommandTextTools.displayQuotedNewlines(slice.value()),
                                        value -> this.editor.replaceRange(
                                                slice.start(), slice.end(), slice.encoded(value))));
                    },
                    this::closeModal,
                    true));
        });
    }

    private void showModal(FlowLayout modal) {
        this.suggestions.dismiss();
        if (this.finder != null) this.finder.suspend();
        this.modal = modal;
        this.root.child(modal);
    }

    private void closeModal() {
        if (this.modal != null) this.root.removeChild(this.modal);
        this.modal = null;
        this.stringEditor = null;
        this.focusPending = true;
        this.status.requestValidation();
    }

    private void apply() {
        if (this.busy || this.modal != null) return;
        this.onApply.accept(this.editor.commandValue());
        Minecraft.getInstance().setScreen(this.parent);
    }

    @Override
    public void onClose() {
        if (this.stringEditor != null) {
            this.stringEditor.requestClose();
            return;
        }
        if (this.modal != null) {
            this.closeModal();
            return;
        }
        this.revision++;
        this.busy = false;
        this.editor.readOnly(false);
        if (this.editor.commandValue().equals(this.original))
            Minecraft.getInstance().setScreen(this.parent);
        else
            this.showModal(ConfirmationDialog.create(
                    ItemEditorText.str("dialog.discard.title"),
                    "",
                    ItemEditorText.str("common.discard"),
                    () -> Minecraft.getInstance().setScreen(this.parent),
                    ItemEditorText.str("common.stay"),
                    this::closeModal));
    }

    @Override
    public boolean keyPressed(KeyEvent key) {
        if (this.stringEditor != null && this.stringEditor.handle(key)) return true;
        if (this.modal != null) {
            if (key.key() == InputConstants.KEY_ESCAPE) {
                this.onClose();
                return true;
            }
            return super.keyPressed(key);
        }
        if (this.finder != null && this.finder.isOpen() && this.finder.handle(key)) return true;
        int tab = RawTextSearchDialog.shortcutTab(key);
        if (tab >= 0) {
            this.openFinder(tab);
            return true;
        }
        if (key.hasControlDownWithQuirk() && key.key() == InputConstants.KEY_S) {
            this.apply();
            return true;
        }
        if ((key.hasControlDownWithQuirk() || (key.modifiers() & InputConstants.MOD_CONTROL) != 0)
                && key.key() == InputConstants.KEY_SPACE) {
            this.suggestions.request();
            return true;
        }
        if (key.key() == InputConstants.KEY_ESCAPE && this.suggestions.dismiss()) return true;
        return super.keyPressed(key);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (this.finder != null && this.modal == null) this.finder.mouseClicked(click.x(), click.y());
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        return (this.modal == null && this.finder != null && this.finder.mouseScrolled(x, y, vertical))
                || super.mouseScrolled(x, y, horizontal, vertical);
    }

    @Override
    public void removed() {
        this.suggestions.dismiss();
        this.revision++;
        this.busy = false;
        this.editor.readOnly(false);
        if (this.finder != null) this.finder.suspend();
        super.removed();
    }

    private record PreparedEmbedded(CommandTextTools.Embedded embedded, String value, String formatted) {}
}
