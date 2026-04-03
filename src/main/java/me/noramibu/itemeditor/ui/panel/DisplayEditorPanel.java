package me.noramibu.itemeditor.ui.panel;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.Surface;
import io.wispforest.owo.ui.container.FlowLayout;
import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.editor.text.RichTextDocument;
import me.noramibu.itemeditor.editor.text.RichTextStyle;
import me.noramibu.itemeditor.ui.component.RichTextAreaComponent;
import me.noramibu.itemeditor.ui.component.StyledTextFieldSection;
import me.noramibu.itemeditor.ui.component.UiFactory;
import me.noramibu.itemeditor.ui.screen.ItemEditorScreen;
import me.noramibu.itemeditor.util.ItemEditorText;
import me.noramibu.itemeditor.util.TextComponentUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class DisplayEditorPanel implements EditorPanel {

    private final ItemEditorScreen screen;

    public DisplayEditorPanel(ItemEditorScreen screen) {
        this.screen = screen;
    }

    @Override
    public FlowLayout build() {
        ItemEditorState state = this.screen.session().state();
        FlowLayout root = UiFactory.column();
        RichTextStyle defaultLoreStyle = this.defaultLoreStyle();
        int loreBaseColor = defaultLoreStyle.color() != null ? defaultLoreStyle.color() : 0xB387FF;

        FlowLayout section = UiFactory.section(
                ItemEditorText.tr("display.lore.title"),
                Component.empty()
        );

        RichTextDocument initialDocument = this.documentFromState(state);
        LabelComponent lineCount = UiFactory.muted(this.lineCountText(initialDocument.logicalLineCount()));
        Consumer<RichTextDocument> commitDocument = document -> {
            PanelBindings.mutate(this.screen, () -> {
                this.applyLoreDocument(state, document);
                lineCount.text(Component.literal(this.lineCountText(document.logicalLineCount())));
            });
        };

        StyledTextFieldSection.BoundEditor loreSection = StyledTextFieldSection.create(
                this.screen,
                initialDocument,
                Sizing.fill(100),
                Sizing.fixed(176),
                ItemEditorText.str("display.lore.placeholder"),
                StyledTextFieldSection.StylePreset.lore(loreBaseColor, defaultLoreStyle),
                ItemEditorText.str("display.lore.color_title"),
                ItemEditorText.str("display.lore.gradient_title"),
                "",
                "",
                null,
                document -> document.logicalLineCount() > ItemLore.MAX_LINES
                        ? ItemEditorText.str("display.lore.max_lines", ItemLore.MAX_LINES)
                        : null,
                commitDocument
        );

        FlowLayout editorFrame = UiFactory.subCard();
        editorFrame.padding(Insets.of(6));
        editorFrame.surface(Surface.flat(0xB014101E).and(Surface.outline(0xFF4C3F63)));
        editorFrame.child(loreSection.toolbar());
        editorFrame.child(loreSection.editor());
        editorFrame.child(this.buildFooter(loreSection.editor(), commitDocument, lineCount));
        editorFrame.child(loreSection.validation());

        section.child(editorFrame);

        root.child(section);
        return root;
    }

    private FlowLayout buildFooter(RichTextAreaComponent editor, Consumer<RichTextDocument> commitDocument, LabelComponent lineCount) {
        FlowLayout row = UiFactory.row();
        row.child(lineCount);

        ButtonComponent clearLore = UiFactory.button(ItemEditorText.tr("display.lore.clear"), button -> {
            RichTextDocument empty = RichTextDocument.empty();
            editor.document(empty);
            commitDocument.accept(empty);
        });
        clearLore.horizontalSizing(Sizing.content());
        row.child(clearLore);
        return row;
    }

    private RichTextDocument documentFromState(ItemEditorState state) {
        List<Component> lines = new ArrayList<>();
        for (ItemEditorState.LoreLineDraft line : state.loreLines) {
            lines.add(TextComponentUtil.applyLineStyle(
                    TextComponentUtil.parseMarkup(line.rawText),
                    line.style,
                    new ArrayList<>()
            ));
        }
        return RichTextDocument.fromLines(lines);
    }

    private void applyLoreDocument(ItemEditorState state, RichTextDocument document) {
        state.loreLines.clear();
        for (Component lineComponent : document.toLineComponents()) {
            ItemEditorState.LoreLineDraft draft = new ItemEditorState.LoreLineDraft();
            draft.rawText = TextComponentUtil.toMarkup(lineComponent);
            state.loreLines.add(draft);
        }
    }

    private String lineCountText(int lineCount) {
        if (lineCount == 0) {
            return ItemEditorText.str("display.lore.line_count.none");
        }
        return lineCount == 1
                ? ItemEditorText.str("display.lore.line_count.one")
                : ItemEditorText.str("display.lore.line_count.many", lineCount);
    }

    private RichTextStyle defaultLoreStyle() {
        return RichTextStyle.fromStyle(Component.empty().withStyle(ChatFormatting.DARK_PURPLE).getStyle());
    }
}
