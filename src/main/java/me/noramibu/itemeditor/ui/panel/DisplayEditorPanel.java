package me.noramibu.itemeditor.ui.panel;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.Surface;
import io.wispforest.owo.ui.core.UIComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.editor.text.RichTextDocument;
import me.noramibu.itemeditor.editor.text.RichTextStyle;
import me.noramibu.itemeditor.ui.component.RichTextAreaComponent;
import me.noramibu.itemeditor.ui.component.StyledTextFieldSection;
import me.noramibu.itemeditor.ui.component.UiFactory;
import me.noramibu.itemeditor.ui.panel.specialdata.MiscSpecialDataSections;
import me.noramibu.itemeditor.ui.panel.specialdata.SpecialDataPanelContext;
import me.noramibu.itemeditor.ui.screen.ItemEditorScreen;
import me.noramibu.itemeditor.util.ItemEditorText;
import me.noramibu.itemeditor.util.TextComponentUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class DisplayEditorPanel implements EditorPanel {
    private static final int LORE_BASE_COLOR_FALLBACK = 0xB387FF;
    private static final int EDITOR_FRAME_PADDING = 6;
    private static final int EDITOR_FRAME_FILL_COLOR = 0xB014101E;
    private static final int EDITOR_FRAME_OUTLINE_COLOR = 0xFF4C3F63;
    private static final int FOOTER_COUNT_WIDTH_MIN = 100;
    private static final int FOOTER_COUNT_WIDTH_COMPACT_RESERVE = 12;
    private static final int FOOTER_COUNT_WIDTH_REGULAR_RESERVE = 170;
    private static final int CLEAR_BUTTON_WIDTH_MIN = 72;
    private static final int CLEAR_BUTTON_WIDTH_MAX = 140;
    private static final int CLEAR_BUTTON_WIDTH_BASE = 116;
    private static final int CLEAR_BUTTON_TEXT_WIDTH_MIN = 40;
    private static final int CLEAR_BUTTON_TEXT_WIDTH_RESERVE = 10;
    private static final int COMPACT_LAYOUT_WIDTH_THRESHOLD = 1150;
    private static final int COMPACT_LAYOUT_CONTENT_WIDTH_THRESHOLD = 720;
    private static final double COMPACT_LAYOUT_SCALE_THRESHOLD = 3.0d;
    private static final int LORE_HEIGHT_RATIO_THRESHOLD_LARGE = 760;
    private static final int LORE_HEIGHT_RATIO_THRESHOLD_MEDIUM = 620;
    private static final double LORE_HEIGHT_RATIO_LARGE = 0.30d;
    private static final double LORE_HEIGHT_RATIO_MEDIUM = 0.26d;
    private static final double LORE_HEIGHT_RATIO_SMALL = 0.22d;
    private static final int LORE_WIDTH_RATIO_PENALTY_THRESHOLD = 1000;
    private static final double LORE_WIDTH_RATIO_PENALTY = 0.03d;
    private static final double LORE_SCALE_RATIO_PENALTY = 0.02d;
    private static final double LORE_RATIO_MIN = 0.18d;
    private static final double LORE_RATIO_MAX = 0.34d;
    private static final int LORE_EDITOR_HEIGHT_MIN = 100;
    private static final int LORE_EDITOR_MIN_SCALED = 110;
    private static final int LORE_EDITOR_MAX_SCALED = 260;

    private final ItemEditorScreen screen;

    public DisplayEditorPanel(ItemEditorScreen screen) {
        this.screen = screen;
    }

    @Override
    public UIComponent build() {
        ItemEditorState state = this.screen.session().state();
        FlowLayout root = UiFactory.column();
        RichTextStyle defaultLoreStyle = this.defaultLoreStyle();
        int loreBaseColor = defaultLoreStyle.color() != null ? defaultLoreStyle.color() : LORE_BASE_COLOR_FALLBACK;
        int loreEditorHeight = this.resolveLoreEditorHeight();

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
                UiFactory.fixed(loreEditorHeight),
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
                commitDocument,
                true
        );

        FlowLayout editorFrame = UiFactory.subCard();
        editorFrame.padding(Insets.of(EDITOR_FRAME_PADDING));
        editorFrame.surface(Surface.flat(EDITOR_FRAME_FILL_COLOR).and(Surface.outline(EDITOR_FRAME_OUTLINE_COLOR)));
        editorFrame.child(loreSection.toolbar());
        editorFrame.child(loreSection.editor());
        editorFrame.child(this.buildFooter(loreSection.editor(), commitDocument, lineCount));
        editorFrame.child(loreSection.validation());

        section.child(editorFrame);

        UiFactory.appendFillChild(root, section);
        this.addVisualSpecialSections(root);
        return root;
    }

    private void addVisualSpecialSections(FlowLayout root) {
        ItemStack stack = this.screen.session().originalStack();
        SpecialDataPanelContext context = new SpecialDataPanelContext(this.screen);

        if (MiscSpecialDataSections.supportsDyed(stack)) {
            UiFactory.appendFillChild(root, MiscSpecialDataSections.buildDyedColor(context));
        }
        if (MiscSpecialDataSections.supportsTrim(stack)) {
            UiFactory.appendFillChild(root, MiscSpecialDataSections.buildTrim(context));
        }
        if (MiscSpecialDataSections.supportsProfile(stack)) {
            UiFactory.appendFillChild(root, MiscSpecialDataSections.buildProfile(context));
        }
    }

    private FlowLayout buildFooter(RichTextAreaComponent editor, Consumer<RichTextDocument> commitDocument, LabelComponent lineCount) {
        boolean compactLayout = this.useCompactLayout();
        int contentWidth = this.screen.editorContentWidthHint();
        FlowLayout row = compactLayout ? UiFactory.column() : UiFactory.row();
        row.gap(Math.max(2, UiFactory.scaleProfile().tightSpacing()));

        Component fullCount = Component.literal(lineCount.text().getString());
        int preferredCountWidth = compactLayout
                ? Math.max(FOOTER_COUNT_WIDTH_MIN, contentWidth - UiFactory.scaledPixels(FOOTER_COUNT_WIDTH_COMPACT_RESERVE))
                : Math.max(FOOTER_COUNT_WIDTH_MIN, contentWidth - UiFactory.scaledPixels(FOOTER_COUNT_WIDTH_REGULAR_RESERVE));
        int countWidth = Math.min(contentWidth, preferredCountWidth);
        Component fittedCount = UiFactory.fitToWidth(fullCount, countWidth);
        lineCount.text(fittedCount);
        if (!fittedCount.getString().equals(fullCount.getString())) {
            lineCount.tooltip(List.of(fullCount));
        }
        lineCount.horizontalSizing(compactLayout ? Sizing.fill(100) : Sizing.expand(100));
        row.child(lineCount);

        Component clearLabel = ItemEditorText.tr("display.lore.clear");
        ButtonComponent clearLore = UiFactory.button(clearLabel, UiFactory.ButtonTextPreset.STANDARD,  button -> {
            RichTextDocument empty = RichTextDocument.empty();
            editor.document(empty);
            commitDocument.accept(empty);
        });
        int preferredClearWidth = Math.max(CLEAR_BUTTON_WIDTH_MIN, Math.min(CLEAR_BUTTON_WIDTH_MAX, UiFactory.scaledPixels(CLEAR_BUTTON_WIDTH_BASE)));
        int clearWidth = Math.min(contentWidth, preferredClearWidth);
        clearLore.horizontalSizing(compactLayout ? Sizing.fill(100) : UiFactory.fixed(clearWidth));
        if (!compactLayout) {
            clearLore.setMessage(UiFactory.fitToWidth(clearLabel, Math.max(CLEAR_BUTTON_TEXT_WIDTH_MIN, clearWidth - UiFactory.scaledPixels(CLEAR_BUTTON_TEXT_WIDTH_RESERVE))));
            clearLore.tooltip(List.of(clearLabel));
        }
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

    private boolean useCompactLayout() {
        var window = this.screen.session().minecraft().getWindow();
        return window.getGuiScaledWidth() < COMPACT_LAYOUT_WIDTH_THRESHOLD
                || window.getGuiScale() >= COMPACT_LAYOUT_SCALE_THRESHOLD
                || this.screen.editorContentWidthHint() < UiFactory.scaledPixels(COMPACT_LAYOUT_CONTENT_WIDTH_THRESHOLD);
    }

    private int resolveLoreEditorHeight() {
        Minecraft minecraft = this.screen.session().minecraft();
        var window = minecraft.getWindow();
        int guiHeight = window.getGuiScaledHeight();
        int guiWidth = window.getGuiScaledWidth();
        double ratio = this.resolveLoreHeightRatio(window, guiHeight, guiWidth);
        int target = (int) Math.round(guiHeight * Math.clamp(ratio, LORE_RATIO_MIN, LORE_RATIO_MAX));
        int min = Math.max(LORE_EDITOR_HEIGHT_MIN, UiFactory.scaledPixels(LORE_EDITOR_MIN_SCALED));
        int max = Math.max(min, UiFactory.scaledPixels(LORE_EDITOR_MAX_SCALED));
        return Math.max(min, Math.min(max, target));
    }

    private double resolveLoreHeightRatio(com.mojang.blaze3d.platform.Window window, int guiHeight, int guiWidth) {
        double ratio = guiHeight >= LORE_HEIGHT_RATIO_THRESHOLD_LARGE
                ? LORE_HEIGHT_RATIO_LARGE
                : (guiHeight >= LORE_HEIGHT_RATIO_THRESHOLD_MEDIUM ? LORE_HEIGHT_RATIO_MEDIUM : LORE_HEIGHT_RATIO_SMALL);
        if (guiWidth < LORE_WIDTH_RATIO_PENALTY_THRESHOLD) {
            ratio -= LORE_WIDTH_RATIO_PENALTY;
        }
        if (window.getGuiScale() >= COMPACT_LAYOUT_SCALE_THRESHOLD) {
            ratio -= LORE_SCALE_RATIO_PENALTY;
        }
        return ratio;
    }
}
