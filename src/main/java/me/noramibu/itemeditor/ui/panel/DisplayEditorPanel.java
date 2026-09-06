package me.noramibu.itemeditor.ui.panel;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.Surface;
import io.wispforest.owo.ui.core.UIComponent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import me.noramibu.itemeditor.editor.EditorCategory;
import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.editor.text.RichTextDocument;
import me.noramibu.itemeditor.editor.text.RichTextStyle;
import me.noramibu.itemeditor.ui.component.EditorSearchDialog;
import me.noramibu.itemeditor.ui.component.RichTextAreaComponent;
import me.noramibu.itemeditor.ui.component.RichTextHorizontalScrollbarComponent;
import me.noramibu.itemeditor.ui.component.StyledTextFieldSection;
import me.noramibu.itemeditor.ui.component.UiFactory;
import me.noramibu.itemeditor.ui.panel.specialdata.MiscSpecialDataSections;
import me.noramibu.itemeditor.ui.panel.specialdata.SpecialDataPanelContext;
import me.noramibu.itemeditor.ui.screen.ItemEditorScreen;
import me.noramibu.itemeditor.ui.util.LayoutModeUtil;
import me.noramibu.itemeditor.util.ItemEditorText;
import me.noramibu.itemeditor.util.TextComponentCompactor;
import me.noramibu.itemeditor.util.TextComponentUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

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
    private static final int COMPACT_LAYOUT_CONTENT_WIDTH_THRESHOLD = 720;
    private static final int LORE_HEIGHT_RATIO_THRESHOLD_LARGE = 760;
    private static final int LORE_HEIGHT_RATIO_THRESHOLD_MEDIUM = 620;
    private static final double LORE_HEIGHT_RATIO_LARGE = 0.30d;
    private static final double LORE_HEIGHT_RATIO_MEDIUM = 0.26d;
    private static final double LORE_HEIGHT_RATIO_SMALL = 0.22d;
    private static final int LORE_WIDTH_RATIO_PENALTY_THRESHOLD = 1000;
    private static final double LORE_WIDTH_RATIO_PENALTY = 0.03d;
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
    public List<EditorSearchDialog.Target> searchTargets() {
        List<EditorSearchDialog.Target> targets = new ArrayList<>();
        for (LoreField field : LoreField.values()) {
            targets.add(field.target(
                    this.screen,
                    EditorCategory.DISPLAY,
                    "display-lore",
                    field == LoreField.TITLE
                            ? PanelSearchDeclaration.parents(EditorCategory.DISPLAY)
                            : PanelSearchDeclaration.parents(EditorCategory.DISPLAY, LoreField.TITLE.label()),
                    EditorSearchDialog.english(LoreField.TITLE.path()),
                    () -> {}));
        }
        SpecialDataPanelContext context = new SpecialDataPanelContext(this.screen);
        this.addVisualSearchTargets(
                targets, MiscSpecialDataSections.searchDyedColorTargets(context, EditorCategory.DISPLAY));
        this.addVisualSearchTargets(
                targets, MiscSpecialDataSections.searchTrimTargets(context, EditorCategory.DISPLAY));
        return List.copyOf(targets);
    }

    private void addVisualSearchTargets(
            List<EditorSearchDialog.Target> targets, List<EditorSearchDialog.Target> visualTargets) {
        String category = EditorCategory.DISPLAY.title().getString();
        for (EditorSearchDialog.Target target : visualTargets) {
            List<String> path = new ArrayList<>();
            if (target.path().isEmpty() || !category.equals(target.path().getFirst())) {
                path.add(category);
            }
            path.addAll(target.path());
            targets.add(new EditorSearchDialog.Target(path, target.terms(), target.open()));
        }
    }

    @Override
    public UIComponent build() {
        ItemEditorState state = this.screen.session().state();
        FlowLayout root = UiFactory.column();
        RichTextStyle defaultLoreStyle = this.defaultLoreStyle();
        int loreBaseColor = defaultLoreStyle.color() != null ? defaultLoreStyle.color() : LORE_BASE_COLOR_FALLBACK;
        int loreEditorHeight = this.resolveLoreEditorHeight();

        FlowLayout section = UiFactory.section(LoreField.TITLE.label(), Component.empty());
        section.id("display-lore");

        RichTextDocument initialDocument = this.documentFromState(state);
        LabelComponent lineCount = UiFactory.muted(this.lineCountText(initialDocument.logicalLineCount()));
        Consumer<RichTextDocument> commitDocument = document -> PanelBindings.mutate(this.screen, () -> {
            this.applyLoreDocument(state, document);
            lineCount.text(Component.literal(this.lineCountText(document.logicalLineCount())));
        });

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
                true);
        this.applyRichEditorRenderMode(loreSection.editor(), state.uiRenderObjectsInLore);

        FlowLayout editorFrame = UiFactory.subCard();
        editorFrame.padding(Insets.of(EDITOR_FRAME_PADDING));
        editorFrame.surface(Surface.flat(EDITOR_FRAME_FILL_COLOR).and(Surface.outline(EDITOR_FRAME_OUTLINE_COLOR)));
        editorFrame.child(loreSection.toolbar());
        ButtonComponent imageArtButton = UiFactory.button(
                LoreField.IMAGE.label(),
                UiFactory.ButtonTextPreset.STANDARD,
                ignored ->
                        this.screen.openLoreImageArtDialog((lines, append) -> this.applyLoreArt(state, lines, append)));
        imageArtButton.horizontalSizing(Sizing.fill(100));
        imageArtButton.tooltip(List.of(ItemEditorText.tr("display.lore.image_art.tooltip")));
        editorFrame.child(imageArtButton);
        editorFrame.child(UiFactory.checkbox(LoreField.RENDER.label(), state.uiRenderObjectsInLore, value -> {
            state.uiRenderObjectsInLore = value;
            this.applyRichEditorRenderMode(loreSection.editor(), value);
        }));
        FlowLayout editorStack = UiFactory.column();
        editorStack.gap(0);
        editorStack.child(loreSection.editor());
        editorStack.child(new RichTextHorizontalScrollbarComponent(Sizing.fill(100), loreSection.editor()));
        editorFrame.child(editorStack);
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
    }

    private FlowLayout buildFooter(
            RichTextAreaComponent editor, Consumer<RichTextDocument> commitDocument, LabelComponent lineCount) {
        int contentWidth = this.screen.editorContentWidthHint();
        boolean compactLayout = LayoutModeUtil.isCompactWidth(contentWidth, COMPACT_LAYOUT_CONTENT_WIDTH_THRESHOLD);
        FlowLayout row = compactLayout ? UiFactory.column() : UiFactory.row();
        row.gap(Math.max(2, UiFactory.scaleProfile().tightSpacing()));

        Component fullCount = Component.literal(lineCount.text().getString());
        int preferredCountWidth = compactLayout
                ? Math.max(
                        FOOTER_COUNT_WIDTH_MIN,
                        contentWidth - UiFactory.scaledPixels(FOOTER_COUNT_WIDTH_COMPACT_RESERVE))
                : Math.max(
                        FOOTER_COUNT_WIDTH_MIN,
                        contentWidth - UiFactory.scaledPixels(FOOTER_COUNT_WIDTH_REGULAR_RESERVE));
        int countWidth = Math.min(contentWidth, preferredCountWidth);
        Component fittedCount = UiFactory.fitToWidth(fullCount, countWidth);
        lineCount.text(fittedCount);
        if (!fittedCount.getString().equals(fullCount.getString())) {
            lineCount.tooltip(List.of(fullCount));
        }
        lineCount.horizontalSizing(compactLayout ? Sizing.fill(100) : Sizing.expand(100));
        row.child(lineCount);

        Component clearLabel = LoreField.CLEAR.label();
        ButtonComponent clearLore = UiFactory.button(clearLabel, UiFactory.ButtonTextPreset.STANDARD, button -> {
            RichTextDocument empty = RichTextDocument.empty();
            editor.document(empty);
            commitDocument.accept(empty);
        });
        int preferredClearWidth = Math.clamp(
                UiFactory.scaledPixels(CLEAR_BUTTON_WIDTH_BASE), CLEAR_BUTTON_WIDTH_MIN, CLEAR_BUTTON_WIDTH_MAX);
        int clearWidth = Math.min(contentWidth, preferredClearWidth);
        clearLore.horizontalSizing(compactLayout ? Sizing.fill(100) : UiFactory.fixed(clearWidth));
        if (!compactLayout) {
            clearLore.tooltip(List.of(clearLabel));
        }
        row.child(clearLore);
        return row;
    }

    private RichTextDocument documentFromState(ItemEditorState state) {
        List<Component> lines = new ArrayList<>();
        for (ItemEditorState.LoreLineDraft line : state.loreLines) {
            lines.add(TextComponentUtil.parseStyledLine(line.rawText, line.style, new ArrayList<>()));
        }
        return RichTextDocument.fromLines(lines);
    }

    private void applyLoreDocument(ItemEditorState state, RichTextDocument document) {
        List<ItemEditorState.LoreLineDraft> previousLines = new ArrayList<>(state.loreLines);
        state.loreLines.clear();
        String serialized = TextComponentUtil.serializeEditorDocument(document);
        if (TextComponentUtil.containsStructuredToken(serialized)) {
            String[] lines = serialized.split("\n", -1);
            for (int index = 0; index < lines.length; index++) {
                ItemEditorState.LoreLineDraft draft = new ItemEditorState.LoreLineDraft();
                draft.rawText = lines[index];
                this.preserveOriginalLoreComponent(draft, previousLines, index);
                state.loreLines.add(draft);
            }
            return;
        }
        List<Component> lineComponents = document.toLineComponents();
        for (int index = 0; index < lineComponents.size(); index++) {
            Component lineComponent = lineComponents.get(index);
            ItemEditorState.LoreLineDraft draft = new ItemEditorState.LoreLineDraft();
            draft.rawText = TextComponentUtil.toMarkup(lineComponent);
            this.preserveOriginalLoreComponent(draft, previousLines, index);
            state.loreLines.add(draft);
        }
    }

    private void applyLoreArt(ItemEditorState state, List<Component> lines, boolean append) {
        PanelBindings.mutateRefresh(this.screen, () -> {
            if (!append) {
                state.loreLines.clear();
            }
            for (Component line : lines) {
                ItemEditorState.LoreLineDraft draft = new ItemEditorState.LoreLineDraft();
                draft.rawText = TextComponentUtil.toMarkup(line);
                draft.originalComponent = TextComponentCompactor.compact(line);
                state.loreLines.add(draft);
            }
        });
    }

    private void preserveOriginalLoreComponent(
            ItemEditorState.LoreLineDraft draft, List<ItemEditorState.LoreLineDraft> previousLines, int index) {
        if (index >= previousLines.size()) {
            return;
        }
        ItemEditorState.LoreLineDraft previous = previousLines.get(index);
        if (previous.originalComponent == null) {
            return;
        }

        Component draftComponent = TextComponentUtil.parseStyledLine(draft.rawText, draft.style, new ArrayList<>());
        if (TextComponentUtil.sameVisibleContent(draftComponent, previous.originalComponent)) {
            draft.originalComponent = previous.originalComponent.copy();
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
        return RichTextStyle.fromStyle(
                Component.empty().withStyle(ChatFormatting.DARK_PURPLE).getStyle());
    }

    private int resolveLoreEditorHeight() {
        int contentHeight = this.screen.editorContentHeightHint();
        int contentWidth = this.screen.editorContentWidthHint();
        double ratio = this.resolveLoreHeightRatio(contentHeight, contentWidth);
        int target = (int) Math.round(contentHeight * Math.clamp(ratio, LORE_RATIO_MIN, LORE_RATIO_MAX));
        int min = Math.max(LORE_EDITOR_HEIGHT_MIN, UiFactory.scaledPixels(LORE_EDITOR_MIN_SCALED));
        int max = Math.max(min, UiFactory.scaledPixels(LORE_EDITOR_MAX_SCALED));
        return Math.clamp(target, min, max);
    }

    private double resolveLoreHeightRatio(int contentHeight, int contentWidth) {
        double ratio = contentHeight >= LORE_HEIGHT_RATIO_THRESHOLD_LARGE
                ? LORE_HEIGHT_RATIO_LARGE
                : (contentHeight >= LORE_HEIGHT_RATIO_THRESHOLD_MEDIUM
                        ? LORE_HEIGHT_RATIO_MEDIUM
                        : LORE_HEIGHT_RATIO_SMALL);
        if (contentWidth < LORE_WIDTH_RATIO_PENALTY_THRESHOLD) {
            ratio -= LORE_WIDTH_RATIO_PENALTY;
        }
        return ratio;
    }

    private void applyRichEditorRenderMode(RichTextAreaComponent editor, boolean renderStructured) {
        editor.structuredRenderMode(renderStructured).lineWrap(!renderStructured);
    }

    private enum LoreField implements PanelSearchDeclaration {
        TITLE("display.lore.title"),
        IMAGE("display.lore.image_art.button"),
        RENDER("common.render_objects"),
        CLEAR("display.lore.clear");

        private final String path;

        LoreField(String path) {
            this.path = path;
        }

        @Override
        public String path() {
            return this.path;
        }
    }
}
