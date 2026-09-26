package me.noramibu.itemeditor.ui.component;

import com.mojang.blaze3d.platform.InputConstants;
import io.wispforest.owo.mixin.ui.access.CheckboxAccessor;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.component.TextBoxComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Color;
import io.wispforest.owo.ui.core.CursorStyle;
import io.wispforest.owo.ui.core.HorizontalAlignment;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.ParentUIComponent;
import io.wispforest.owo.ui.core.Positioning;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.Surface;
import io.wispforest.owo.ui.core.UIComponent;
import io.wispforest.owo.ui.core.UIComponent.FocusSource;
import io.wispforest.owo.ui.core.VerticalAlignment;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.regex.PatternSyntaxException;
import me.noramibu.itemeditor.storage.RawEditorOptionsService;
import me.noramibu.itemeditor.storage.model.RawEditorOptions;
import me.noramibu.itemeditor.ui.component.raw.RawSearchEdits;
import me.noramibu.itemeditor.ui.component.raw.RawTextSearchMatcher;
import me.noramibu.itemeditor.util.ItemEditorText;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

public final class RawTextSearchDialog {
    private RawTextAreaComponent editor;
    private FlowLayout host;
    private boolean closed;
    private boolean suspended;
    private boolean finderFocused = true;
    private final FlowLayout bar = new RawSearchWindow(this::resize, this::saveOptions);
    private final FlowLayout settings = UiFactory.column();
    private final FlowLayout searchContent = UiFactory.column();
    private final FlowLayout replacementContent = UiFactory.column();
    private final FlowLayout results = UiFactory.column();
    private final FlowLayout goToContent = UiFactory.column();
    private final FlowLayout infoContent = UiFactory.column();
    private TextBoxComponent goToLine;
    private int mode;
    private final TextBoxComponent replacement = UiFactory.textBox("", ignored -> {});
    private final LabelComponent replacementStatus = UiFactory.muted("");
    private final List<ButtonComponent> replaceButtons = new ArrayList<>();
    private final List<ButtonComponent> tabs = new ArrayList<>();
    private int resultPage;
    private int[] lineStarts = {0};
    private boolean busy;
    private InputSafeScrollContainer<FlowLayout> settingsScroll;
    private final TextBoxComponent input;
    private final LabelComponent status = UiFactory.muted("");
    private final List<ButtonComponent> navigationButtons = new ArrayList<>();
    private final RawEditorOptions options = RawEditorOptionsService.instance().load();
    private int selected = -1;
    private int replacementNextOffset = -1;
    private String lastText = "";
    private int[] ends = new int[0];
    private int[] matches = new int[0];
    private int[] ranges = new int[0];
    private final AtomicInteger generation = new AtomicInteger();
    private long searchAfter;
    private boolean pending;
    private boolean dragging;
    private boolean saved;
    private boolean navigateAfterSearch;
    private boolean selectionOnly;
    private int selectionStart;
    private int selectionEnd;
    private String selectionDocument;

    public RawTextSearchDialog(RawTextAreaComponent editor, FlowLayout host, String query, Consumer<String> remember) {
        this.editor = editor;
        this.host = host;
        this.captureSelection();
        this.bar.padding(Insets.of(6));
        this.bar.gap(3);
        this.bar.positioning(Positioning.absolute(0, 0));
        this.bar.mouseDown().subscribe((click, doubled) -> true);
        this.surface();
        LabelComponent handle = new LabelComponent(Component.literal("- - -")) {
            @Override
            protected Style styleAt(int mouseX, int mouseY) {
                Style style = super.styleAt(mouseX, mouseY);
                return style == null ? Style.EMPTY : style;
            }

            @Override
            public boolean canFocus(FocusSource source) {
                return true;
            }
        };
        handle.horizontalSizing(Sizing.expand(100));
        handle.horizontalTextAlignment(HorizontalAlignment.CENTER);
        handle.verticalTextAlignment(VerticalAlignment.CENTER);
        handle.cursorStyle(CursorStyle.MOVE);
        handle.verticalSizing(Sizing.fixed(UiFactory.scaledPixels(18)));
        handle.tooltip(List.of(ItemEditorText.tr("raw_editor.find.drag")));
        handle.mouseDown().subscribe((click, doubled) -> {
            this.dragging = click.button() == InputConstants.MOUSE_BUTTON_LEFT;
            return this.dragging;
        });
        handle.mouseDrag().subscribe((click, dx, dy) -> {
            if (!this.dragging) return false;
            int rangeX = Math.max(0, this.editor.width() - this.bar.width() - 8);
            int rangeY = Math.max(0, this.editor.height() - this.bar.height() - 8);
            this.options.searchX = rangeX == 0 ? 0 : Math.clamp(this.options.searchX + dx / rangeX, 0, 1);
            this.options.searchY = rangeY == 0 ? 0 : Math.clamp(this.options.searchY + dy / rangeY, 0, 1);
            this.saved = false;
            this.position();
            return true;
        });
        handle.mouseUp().subscribe(click -> {
            if (!this.dragging) return false;
            this.dragging = false;
            this.saveOptions();
            return true;
        });
        ButtonComponent close =
                UiFactory.negativeButton(Component.literal("X"), UiFactory.ButtonTextPreset.COMPACT, b -> this.close());
        close.sizing(Sizing.fixed(UiFactory.scaledPixels(18)));
        close.tooltip(List.of(ItemEditorText.tr("common.close")));
        this.bar.child(UiFactory.row().child(handle).child(close));
        String[] tabKeys = {"title", "replace", "go_to", "options", "info"};
        String[] tabShortcuts = {"Ctrl+F", "Ctrl+H", "Ctrl+G", "", "F1"};
        for (int i = 0; i < tabKeys.length; i++) {
            int tab = i;
            ButtonComponent button = UiFactory.actionToneButton(
                    ItemEditorText.tr("raw_editor.find." + tabKeys[i]),
                    UiFactory.ButtonTextPreset.COMPACT,
                    UiFactory.ActionTone.PICKER,
                    ignored -> this.openTab(tab));
            button.horizontalSizing(Sizing.expand(20));
            if (!tabShortcuts[i].isEmpty())
                button.tooltip(List.of(ItemEditorText.tr("raw_editor.find." + tabKeys[i])
                        .copy()
                        .append(" (" + tabShortcuts[i] + ")")));
            this.tabs.add(button);
        }
        this.bar.child(UiFactory.actionButtonRow(this.tabs.toArray(ButtonComponent[]::new)));
        this.input = UiFactory.textBox(query, value -> {
            remember.accept(value);
            this.requestSearch();
        });
        this.input.setHint(ItemEditorText.tr("dialog.searchable_picker.search"));
        this.searchContent.child(this.input);
        var scope = UiFactory.checkbox(ItemEditorText.tr("raw_editor.find.selection_only"), false, checked -> {
            this.selectionOnly = checked;
            this.requestSearch();
        });
        scope.tooltip(this.help("selection_only.tooltip"));
        this.searchContent.child(scope);
        this.replacement.setHint(ItemEditorText.tr("raw_editor.find.replacement"));
        this.replacement.tooltip(this.help("replacement.tooltip"));
        this.replacementContent.child(this.replacement);
        var literalReplacement = UiFactory.checkbox(
                ItemEditorText.tr("raw_editor.find.literal_replacement"),
                this.options.searchLiteralReplacement,
                checked -> {
                    this.options.searchLiteralReplacement = checked;
                    this.requestSearch(false);
                    this.saveOptions();
                });
        literalReplacement.tooltip(this.help("literal_replacement.tooltip"));
        this.replacementContent.child(literalReplacement);
        for (boolean all : new boolean[] {false, true}) {
            ButtonComponent button = UiFactory.positiveButton(
                    ItemEditorText.tr(all ? "raw_editor.find.replace_all" : "raw_editor.find.replace"),
                    UiFactory.ButtonTextPreset.COMPACT,
                    ignored -> this.replace(all));
            this.replaceButtons.add(button);
        }
        this.replacementContent.child(
                UiFactory.actionButtonRow(false, this.replaceButtons.toArray(ButtonComponent[]::new)));
        this.replacementContent.child(this.replacementStatus);
        this.status.horizontalSizing(Sizing.fill(100));
        this.searchContent.child(this.status);
        FlowLayout navigation = UiFactory.actionButtonRow(
                this.navigationButton("|<", "first", () -> this.selectMatch(0)),
                this.navigationButton("<", "previous", () -> this.move(-1)),
                this.navigationButton(">", "next", () -> this.move(1)),
                this.navigationButton(">|", "last", () -> this.selectMatch(this.matches.length - 1)));
        this.searchContent.child(navigation);
        this.searchContent.child(this.results);
        this.buildGoTo();
        for (String key : List.of("open", "find", "edit", "navigation", "indent", "suggestions", "groups")) {
            var heading = UiFactory.bodyLabel(ItemEditorText.tr("raw_editor.find.info." + key + ".title")
                    .copy()
                    .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
            heading.horizontalSizing(Sizing.fill(100));
            heading.margins(Insets.top(UiFactory.scaledPixels(5)));
            this.infoContent.child(heading);
            var label = UiFactory.bodyLabel(ItemEditorText.tr("raw_editor.find.info." + key));
            label.horizontalSizing(Sizing.fill(100));
            this.infoContent.child(label);
        }
        this.settings.child(UiFactory.checkbox(
                ItemEditorText.tr("raw_editor.find.match_case"), this.options.searchMatchCase, checked -> {
                    this.options.searchMatchCase = checked;
                    this.requestSearch();
                    this.saveOptions();
                }));
        this.settings.child(UiFactory.checkbox(
                ItemEditorText.tr("raw_editor.find.whole_word"), this.options.searchWholeWord, checked -> {
                    this.options.searchWholeWord = checked;
                    this.requestSearch();
                    this.saveOptions();
                }));
        this.settings.child(UiFactory.checkbox(
                ItemEditorText.tr("raw_editor.find.highlight_all"), this.options.searchHighlightAll, checked -> {
                    this.options.searchHighlightAll = checked;
                    this.highlight();
                    this.saveOptions();
                }));
        this.searchOption("regex", this.options.searchRegex, checked -> this.options.searchRegex = checked);
        this.searchOption("dot_all", this.options.searchDotAll, checked -> this.options.searchDotAll = checked);
        this.searchOption("wrap", this.options.searchWrap, checked -> this.options.searchWrap = checked);
        LabelComponent opacityLabel =
                UiFactory.muted(ItemEditorText.tr("raw_editor.find.opacity", this.options.searchOpacity));
        this.settings.child(opacityLabel);
        var opacity = new SafeDiscreteSliderComponent(Sizing.fill(100), 25, 100);
        opacity.decimalPlaces(0).snap(true);
        opacity.setFromDiscreteValue(this.options.searchOpacity);
        opacity.onChanged().subscribe(value -> {
            this.options.searchOpacity = (int) Math.round(value);
            opacityLabel.text(ItemEditorText.tr("raw_editor.find.opacity", this.options.searchOpacity));
            this.surface();
            this.saved = false;
        });
        this.settings.child(opacity);
        this.settings.child(UiFactory.negativeButton(
                ItemEditorText.tr("raw_editor.find.reset_window"), UiFactory.ButtonTextPreset.COMPACT, button -> {
                    this.options.searchWidth = 330;
                    this.options.searchHeight = 230;
                    this.options.searchX = 1;
                    this.options.searchY = 0;
                    this.position();
                    this.saveOptions();
                }));
        host.child(this.bar);
        this.showTab(0);
        this.surface();
        this.position();
        host.queue(() -> this.focusTab(this.mode));
        this.requestSearch();
    }

    private void showTab(int tab) {
        this.mode = tab;
        if (this.settingsScroll != null) this.bar.removeChild(this.settingsScroll);
        if (this.searchContent.children().contains(this.replacementContent))
            this.searchContent.removeChild(this.replacementContent);
        if (tab == 1) this.searchContent.child(1, this.replacementContent);
        FlowLayout content = tab == 4
                ? this.infoContent
                : tab == 3 ? this.settings : tab == 2 ? this.goToContent : this.searchContent;
        // The scrollbar overlays its child; reserve its lane inside every tab's content.
        content.padding(Insets.right(6 + UiFactory.scaledPixels(4)));
        this.settingsScroll = InputSafeScrollContainer.vertical(Sizing.fill(100), Sizing.expand(100), content);
        this.settingsScroll.consumeScrollWhenHovered(true);
        this.settingsScroll.scrollbarThiccness(6);
        this.settingsScroll.scrollbar(
                (context, x, y, width, height, trackX, trackY, trackWidth, trackHeight, time, direction, active) -> {
                    if (!active) return;
                    context.fill(trackX, trackY, trackX + trackWidth, trackY + trackHeight, 0xFF293A44);
                    context.fill(x, y, x + width, y + height, 0xFF79D6E8);
                });
        this.bar.child(this.settingsScroll);
        for (int i = 0; i < this.tabs.size(); i++) this.tabs.get(i).active(i != tab);
        this.surface();
    }

    public void openTab(int tab) {
        if (tab == 0
                && this.editor.focusHandler() != null
                && this.editor.focusHandler().focused() == this.editor) {
            int start = Math.min(this.editor.caretIndex(), this.editor.selectionIndex());
            int end = Math.max(this.editor.caretIndex(), this.editor.selectionIndex());
            if (start < end) this.input.text(this.editor.getValue().substring(start, end));
        }
        this.finderFocused = true;
        this.showTab(tab);
        this.host.queue(() -> this.focusTab(tab));
    }

    private void focusTab(int tab) {
        if (!this.isOpen() || this.bar.focusHandler() == null) return;
        UIComponent target = tab == 2 ? this.goToLine : tab < 2 ? this.input : this.bar;
        this.bar.focusHandler().focus(target, FocusSource.KEYBOARD_CYCLE);
    }

    public static int shortcutTab(KeyEvent key) {
        boolean ctrl = key.hasControlDownWithQuirk() || (key.modifiers() & InputConstants.MOD_CONTROL) != 0;
        if (!ctrl || key.hasAltDown() || key.hasShiftDown()) return -1;
        return switch (key.key()) {
            case InputConstants.KEY_F -> 0;
            case InputConstants.KEY_H -> 1;
            case InputConstants.KEY_G -> 2;
            default -> -1;
        };
    }

    private List<Component> help(String key) {
        int width = Math.clamp(this.editor.width() - 20, 80, 260);
        return UiFactory.tooltipLines(ItemEditorText.tr("raw_editor.find." + key), width);
    }

    private void captureSelection() {
        this.selectionStart = Math.min(this.editor.caretIndex(), this.editor.selectionIndex());
        this.selectionEnd = Math.max(this.editor.caretIndex(), this.editor.selectionIndex());
        this.selectionDocument = this.editor.getValue();
    }

    private void updateSelectionAfterReplacement(int oldLength) {
        if (!this.selectionOnly) return;
        this.selectionEnd += this.editor.getValue().length() - oldLength;
        this.selectionDocument = this.editor.getValue();
    }

    private void buildGoTo() {
        TextBoxComponent line = UiFactory.textBox(Integer.toString(this.editor.caretLine()), ignored -> {});
        this.goToLine = line;
        TextBoxComponent column = UiFactory.textBox("1", ignored -> {});
        this.goToContent.child(UiFactory.bodyLabel(ItemEditorText.tr("raw_editor.find.line")));
        this.goToContent.child(line);
        this.goToContent.child(UiFactory.bodyLabel(ItemEditorText.tr("raw_editor.find.column")));
        this.goToContent.child(column);
        LabelComponent message = UiFactory.muted("");
        Runnable go = () -> {
            int offset = -1;
            try {
                offset = RawSearchEdits.position(
                        this.editor.getValue(), Integer.parseInt(line.getValue()), Integer.parseInt(column.getValue()));
            } catch (NumberFormatException ignored) {
            }
            if (offset < 0) {
                message.text(ItemEditorText.tr("raw_editor.find.invalid_position"));
                return;
            }
            this.editor.selectSearchMatch(offset, offset);
            message.text(Component.empty());
            if (this.editor.focusHandler() != null)
                this.editor.focusHandler().focus(this.editor, FocusSource.KEYBOARD_CYCLE);
        };
        this.goToContent.child(UiFactory.positiveButton(
                ItemEditorText.tr("raw_editor.find.go_to"), UiFactory.ButtonTextPreset.COMPACT, ignored -> go.run()));
        for (TextBoxComponent field : List.of(line, column))
            field.keyPress().subscribe(key -> {
                if (key.key() != InputConstants.KEY_RETURN) return false;
                go.run();
                return true;
            });
        this.goToContent.child(message);
    }

    private void replace(boolean all) {
        if (this.busy || this.matches.length == 0 || !this.lastText.equals(this.editor.getValue())) return;
        boolean expandGroups = this.options.searchRegex && !this.options.searchLiteralReplacement;
        if (!all && this.selected < 0) {
            this.selectMatch(0);
            return;
        }
        if (!all && !expandGroups) {
            int nextOffset =
                    this.matches[this.selected] + this.replacement.getValue().length();
            if (this.matches[this.selected] == this.ends[this.selected]) nextOffset++;
            this.editor.replaceRange(
                    this.matches[this.selected], this.ends[this.selected], this.replacement.getValue());
            this.updateSelectionAfterReplacement(this.lastText.length());
            this.requestSearch(false);
            this.replacementNextOffset = nextOffset;
            this.replacementStatus.text(ItemEditorText.tr("raw_editor.find.replaced", 1));
            return;
        }
        String text = this.lastText, replacement = this.replacement.getValue();
        int[] starts = all ? this.matches : new int[] {this.matches[this.selected]};
        int[] ends = all ? this.ends : new int[] {this.ends[this.selected]};
        String query = this.input.getValue();
        boolean matchCase = this.options.searchMatchCase, dotAll = this.options.searchDotAll;
        int scopeStart = this.selectionOnly ? this.selectionStart : 0;
        int scopeEnd = this.selectionOnly ? this.selectionEnd : text.length();
        int request = this.generation.incrementAndGet();
        this.pending = false;
        this.busy = true;
        this.replaceButtons.forEach(button -> button.active(false));
        this.replacementStatus.text(ItemEditorText.tr("raw_editor.find.searching"));
        CompletableFuture.supplyAsync(() -> expandGroups
                        ? RawSearchEdits.replaceRegex(
                                text,
                                query,
                                starts,
                                ends,
                                replacement,
                                matchCase,
                                dotAll,
                                () -> this.generation.get() != request,
                                scopeStart,
                                scopeEnd)
                        : RawSearchEdits.replaceAll(
                                text, starts, ends, replacement, () -> this.generation.get() != request))
                .whenComplete((result, error) -> Minecraft.getInstance().execute(() -> {
                    if (this.generation.get() != request || !this.isOpen() || !text.equals(this.editor.getValue()))
                        return;
                    this.busy = false;
                    if (error != null) {
                        this.replacementStatus.text(ItemEditorText.tr(
                                expandGroups
                                        ? "raw_editor.find.invalid_replacement"
                                        : "raw_editor.find.replace_failed"));
                        this.replaceButtons.forEach(button -> button.active(true));
                        return;
                    }
                    this.editor.replaceRange(0, text.length(), result.text());
                    this.updateSelectionAfterReplacement(text.length());
                    this.requestSearch(false);
                    if (!all) this.replacementNextOffset = result.nextOffset();
                    this.replacementStatus.text(ItemEditorText.tr("raw_editor.find.replaced", result.count()));
                }));
    }

    private void renderResults() {
        this.results.clearChildren();
        if (this.matches.length == 0) return;
        int pages = (this.matches.length + 9) / 10;
        this.resultPage = Math.clamp(this.resultPage, 0, pages - 1);
        ButtonComponent previous =
                UiFactory.button(Component.literal("<"), UiFactory.ButtonTextPreset.COMPACT, ignored -> {
                    this.resultPage--;
                    this.renderResults();
                });
        ButtonComponent next = UiFactory.button(Component.literal(">"), UiFactory.ButtonTextPreset.COMPACT, ignored -> {
            this.resultPage++;
            this.renderResults();
        });
        previous.tooltip(List.of(ItemEditorText.tr("common.prev")));
        next.tooltip(List.of(ItemEditorText.tr("common.next")));
        previous.active(this.resultPage > 0);
        next.active(this.resultPage < pages - 1);
        this.results.child(
                UiFactory.bodyLabel(ItemEditorText.tr("raw_editor.find.results_page", this.resultPage + 1, pages)));
        this.results.child(UiFactory.actionButtonRow(false, previous, next));
        for (int i = this.resultPage * 10; i < Math.min(this.matches.length, (this.resultPage + 1) * 10); i++) {
            int match = i, start = this.matches[i], end = this.ends[i];
            int line = Math.max(0, RawTextSearchMatcher.lowerBound(this.lineStarts, start + 1) - 1);
            int from = Math.max(this.lineStarts[line], start - 24);
            int to = Math.min(this.lastText.length(), start + 72);
            int hitEnd = Math.min(end, to);
            Component label = Component.literal((line + 1) + ":" + (start - this.lineStarts[line] + 1) + "  ")
                    .withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(this.snippet(from, start)).withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(start == end ? "|" : this.snippet(start, hitEnd))
                            .withStyle(i == this.selected ? ChatFormatting.GOLD : ChatFormatting.AQUA))
                    .append(Component.literal(this.snippet(hitEnd, to)).withStyle(ChatFormatting.GRAY));
            ButtonComponent button =
                    UiFactory.button(label, UiFactory.ButtonTextPreset.COMPACT, ignored -> this.selectMatch(match));
            button.horizontalSizing(Sizing.fill(100));
            this.results.child(button);
        }
        this.surface();
    }

    private String snippet(int from, int to) {
        return this.lastText
                .substring(from, to)
                .replace('\n', ' ')
                .replace('\r', ' ')
                .replace('\t', ' ');
    }

    private ButtonComponent navigationButton(String symbol, String key, Runnable action) {
        ButtonComponent button = UiFactory.actionToneButton(
                Component.literal(symbol),
                UiFactory.ButtonTextPreset.COMPACT,
                UiFactory.ActionTone.PICKER,
                ignored -> action.run());
        button.horizontalSizing(Sizing.fixed(UiFactory.scaledPixels(26)));
        button.tooltip(List.of(ItemEditorText.tr("raw_editor.find." + key)));
        this.navigationButtons.add(button);
        return button;
    }

    private void surface() {
        int alpha = Math.clamp((long) this.options.searchOpacity * 255 / 100, 0, 255);
        this.bar.surface(Surface.flat((alpha << 24) | 0x101820).and(Surface.outline((alpha << 24) | 0x8899AA)));
        this.opacity(this.bar, alpha);
        this.opacity(this.settings, alpha);
    }

    private void opacity(UIComponent component, int alpha) {
        if (component instanceof LabelComponent label)
            label.color(Color.ofArgb((alpha << 24) | (label.color().get().argb() & 0xFFFFFF)));
        if (component instanceof AbstractWidget widget) widget.setAlpha(alpha / 255f);
        if (component instanceof CheckboxAccessor checkbox)
            checkbox.owo$getTextWidget().setAlpha(alpha / 255f);
        if (component instanceof TextBoxComponent textBox) textBox.setTextColor((alpha << 24) | 0xFFFFFF);
        if (component instanceof ButtonComponent button)
            button.renderer(ButtonComponent.Renderer.flat(
                    (alpha << 24) | 0x35434F, (alpha << 24) | 0x516777, (alpha << 24) | 0x262C32));
        if (component instanceof ParentUIComponent parent)
            for (UIComponent child : parent.children()) this.opacity(child, alpha);
    }

    private void searchOption(String key, boolean checked, Consumer<Boolean> update) {
        var option = UiFactory.checkbox(ItemEditorText.tr("raw_editor.find." + key), checked, value -> {
            update.accept(value);
            this.requestSearch();
            this.saveOptions();
        });
        option.tooltip(this.help(key + ".tooltip"));
        this.settings.child(option);
    }

    private void resize(int edges, double dx, double dy) {
        int oldWidth = this.bar.width(), oldHeight = this.bar.height();
        double x = this.options.searchX * Math.max(0, this.editor.width() - oldWidth - 8);
        double y = this.options.searchY * Math.max(0, this.editor.height() - oldHeight - 8);
        int maxWidth = Math.max(1, this.editor.width() - 8), maxHeight = Math.max(1, this.editor.height() - 8);
        int width = Math.clamp(
                oldWidth + (int) ((edges & 1) != 0 ? -dx : (edges & 2) != 0 ? dx : 0),
                Math.min(240, maxWidth),
                maxWidth);
        int height = Math.clamp(
                oldHeight + (int) ((edges & 4) != 0 ? -dy : (edges & 8) != 0 ? dy : 0),
                Math.min(150, maxHeight),
                maxHeight);
        if ((edges & 1) != 0) x += oldWidth - width;
        if ((edges & 4) != 0) y += oldHeight - height;
        this.options.searchWidth = width;
        this.options.searchHeight = height;
        this.saved = false;
        this.options.searchX = Math.clamp(x / Math.max(1, maxWidth - width), 0, 1);
        this.options.searchY = Math.clamp(y / Math.max(1, maxHeight - height), 0, 1);
        this.position();
    }

    private void position() {
        int width = Math.clamp(this.options.searchWidth, 1, Math.max(1, this.editor.width() - 8));
        this.bar.horizontalSizing(Sizing.fixed(width));
        this.bar.verticalSizing(
                Sizing.fixed(Math.clamp(this.options.searchHeight, 1, Math.max(1, this.editor.height() - 8))));
        int rangeX = Math.max(0, this.editor.width() - width - 8);
        int rangeY = Math.max(0, this.editor.height() - this.bar.height() - 8);
        this.bar.positioning(Positioning.absolute(
                this.editor.x()
                        - this.host.x()
                        - this.host.padding().get().left()
                        + 4
                        + (int) (rangeX * this.options.searchX),
                this.editor.y()
                        - this.host.y()
                        - this.host.padding().get().top()
                        + 4
                        + (int) (rangeY * this.options.searchY)));
    }

    public boolean handle(KeyEvent key) {
        if (this.bar.focusHandler() == null) return false;
        UIComponent focused = this.bar.focusHandler().focused();
        boolean inside = this.finderFocused || focused == this.editor;
        for (UIComponent node = focused; node != null; node = node.parent()) inside |= node == this.bar;
        if (!inside) return false;
        int tab = shortcutTab(key);
        if (tab >= 0) {
            this.openTab(tab);
            return true;
        }
        if (key.key() == InputConstants.KEY_F1 && focused != this.editor) {
            this.openTab(4);
            return true;
        }
        if (key.key() == InputConstants.KEY_ESCAPE) {
            this.close();
            return true;
        }
        if (key.key() == InputConstants.KEY_F3 || (key.key() == InputConstants.KEY_RETURN && focused == this.input)) {
            this.move(key.hasShiftDown() ? -1 : 1);
            return true;
        }
        return false;
    }

    public void close() {
        this.closed = true;
        this.generation.incrementAndGet();
        this.pending = false;
        this.editor.clearSearchHighlights();
        this.saveOptions();
        this.host.removeChild(this.bar);
        if (this.editor.focusHandler() != null)
            this.editor.focusHandler().focus(this.editor, FocusSource.KEYBOARD_CYCLE);
    }

    public boolean isOpen() {
        return !this.closed && !this.suspended && this.host.children().contains(this.bar) && this.editor.hasParent();
    }

    public boolean isClosed() {
        return this.closed;
    }

    public void suspend() {
        if (this.closed || this.suspended) return;
        this.generation.incrementAndGet();
        this.pending = false;
        this.suspended = true;
        this.dragging = false;
        this.finderFocused = false;
        this.editor.clearSearchHighlights();
        this.saveOptions();
        this.host.removeChild(this.bar);
    }

    public void rebind(RawTextAreaComponent editor) {
        if (this.closed || !(editor.parent() instanceof FlowLayout editorHost)) return;
        if (editor == this.editor && this.isOpen()) return;
        this.suspend();
        this.editor = editor;
        this.host = editorHost;
        this.suspended = false;
        editorHost.child(this.bar);
        this.position();
        this.requestSearch(false);
    }

    public void mouseClicked(double x, double y) {
        this.finderFocused = this.isOpen() && this.bar.isInBoundingBox(x, y);
    }

    public boolean mouseScrolled(double x, double y, double amount) {
        if (!this.isOpen() || !this.bar.isInBoundingBox(x, y)) return false;
        this.bar.onMouseScroll(x - this.bar.x(), y - this.bar.y(), amount);
        return true;
    }

    private void requestSearch() {
        this.requestSearch(true);
    }

    private void requestSearch(boolean navigate) {
        this.navigateAfterSearch = navigate;
        this.replacementNextOffset = -1;
        this.generation.incrementAndGet();
        this.pending = true;
        this.searchAfter = System.nanoTime() + 120_000_000L;
        this.lastText = this.editor.getValue();
        this.matches = new int[0];
        this.busy = false;
        this.results.clearChildren();
        this.replaceButtons.forEach(button -> button.active(false));
        this.navigationButtons.forEach(button -> button.active(false));
        this.selected = -1;
        this.editor.clearSearchHighlights();
        this.status.text(ItemEditorText.tr("raw_editor.find.searching"));
        if (this.selectionOnly
                && (this.selectionStart == this.selectionEnd || !this.lastText.equals(this.selectionDocument))) {
            this.pending = false;
            this.status.text(ItemEditorText.tr("raw_editor.find.select_again"));
        }
    }

    public void tick() {
        if (!this.isOpen()) {
            this.generation.incrementAndGet();
            if (!this.saved) this.saveOptions();
            return;
        }
        this.position();
        if (!this.lastText.equals(this.editor.getValue())) this.requestSearch(false);
        if (!this.pending || System.nanoTime() < this.searchAfter) return;
        this.pending = false;
        int request = this.generation.get();
        String text = this.lastText;
        String query = this.input.getValue();
        boolean matchCase = this.options.searchMatchCase;
        boolean wholeWord = this.options.searchWholeWord;
        boolean regex = this.options.searchRegex;
        boolean dotAll = this.options.searchDotAll;
        boolean navigate = this.navigateAfterSearch;
        int scopeStart = this.selectionOnly ? this.selectionStart : 0;
        int scopeEnd = this.selectionOnly ? this.selectionEnd : text.length();
        CompletableFuture.supplyAsync(() -> {
                    var result = RawTextSearchMatcher.searchRange(
                            text,
                            query,
                            matchCase,
                            wholeWord,
                            regex,
                            dotAll,
                            () -> this.generation.get() != request,
                            scopeStart,
                            scopeEnd);
                    return new SearchResults(
                            result.starts(),
                            result.ends(),
                            RawTextSearchMatcher.highlightRanges(result.starts(), result.ends()),
                            RawSearchEdits.lineStarts(text),
                            RawSearchEdits.replacementCount(result.starts(), result.ends()));
                })
                .whenComplete((found, error) -> Minecraft.getInstance().execute(() -> {
                    if (this.generation.get() != request || !this.isOpen() || !text.equals(this.editor.getValue()))
                        return;
                    if (error != null) {
                        Throwable cause = error.getCause() == null ? error : error.getCause();
                        this.status.text(ItemEditorText.tr(
                                        cause instanceof PatternSyntaxException
                                                ? "raw_editor.find.invalid_regex"
                                                : "raw_editor.find.limited")
                                .copy()
                                .withStyle(ChatFormatting.RED));
                        return;
                    }
                    this.matches = found.offsets();
                    this.navigationButtons.forEach(button -> button.active(this.matches.length > 0));
                    this.ranges = found.ranges();
                    this.ends = found.ends();
                    this.lineStarts = found.lines();
                    this.resultPage = 0;
                    this.replaceButtons.forEach(button -> button.active(this.matches.length > 0));
                    this.replaceButtons
                            .get(1)
                            .setMessage(ItemEditorText.tr("raw_editor.find.replace_all_count", found.replacements())
                                    .copy()
                                    .withStyle(ChatFormatting.GREEN));
                    this.renderResults();
                    this.selected = -1;
                    this.status.text(ItemEditorText.tr("raw_editor.find.count", 0, this.matches.length));
                    if (navigate) {
                        this.move(1);
                    } else {
                        this.highlight();
                    }
                    if (this.replacementNextOffset >= 0 && this.matches.length > 0) {
                        int next = RawTextSearchMatcher.lowerBound(this.matches, this.replacementNextOffset);
                        if (next < this.matches.length) this.selectMatch(next);
                        else if (this.options.searchWrap) this.selectMatch(0);
                        this.replacementNextOffset = -1;
                    }
                }));
    }

    private void move(int direction) {
        if (!this.lastText.equals(this.editor.getValue())) {
            this.requestSearch();
            return;
        }
        if (this.matches.length == 0) return;
        this.selectMatch(
                this.selected < 0
                        ? (direction < 0 ? this.matches.length - 1 : 0)
                        : this.options.searchWrap
                                ? Math.floorMod(this.selected + direction, this.matches.length)
                                : Math.clamp(this.selected + direction, 0, this.matches.length - 1));
    }

    private void selectMatch(int index) {
        if (!this.lastText.equals(this.editor.getValue())) {
            this.requestSearch(false);
            return;
        }
        if (index < 0 || index >= this.matches.length) return;
        this.selected = index;
        this.resultPage = index / 10;
        this.renderResults();
        int start = this.matches[this.selected];
        this.editor.selectSearchMatch(start, this.ends[this.selected]);
        this.highlight();
        this.status.text(ItemEditorText.tr("raw_editor.find.count", this.selected + 1, this.matches.length));
    }

    private void highlight() {
        this.editor.searchHighlights(
                this.lastText, this.matches, this.ranges, this.ends, this.selected, this.options.searchHighlightAll);
    }

    private void saveOptions() {
        RawEditorOptions stored = RawEditorOptionsService.instance().load();
        stored.searchMatchCase = this.options.searchMatchCase;
        stored.searchWholeWord = this.options.searchWholeWord;
        stored.searchRegex = this.options.searchRegex;
        stored.searchLiteralReplacement = this.options.searchLiteralReplacement;
        stored.searchDotAll = this.options.searchDotAll;
        stored.searchWrap = this.options.searchWrap;
        stored.searchWidth = this.options.searchWidth;
        stored.searchHeight = this.options.searchHeight;
        stored.searchHighlightAll = this.options.searchHighlightAll;
        stored.searchOpacity = this.options.searchOpacity;
        stored.searchX = this.options.searchX;
        stored.searchY = this.options.searchY;
        RawEditorOptionsService.instance().save(stored);
        this.saved = true;
    }

    private record SearchResults(int[] offsets, int[] ends, int[] ranges, int[] lines, int replacements) {}
}
