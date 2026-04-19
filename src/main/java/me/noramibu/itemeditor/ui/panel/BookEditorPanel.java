package me.noramibu.itemeditor.ui.panel;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.core.HorizontalAlignment;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.Surface;
import io.wispforest.owo.ui.core.UIComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import me.noramibu.itemeditor.ui.component.InputSafeScrollContainer;
import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.editor.text.RichTextDocument;
import me.noramibu.itemeditor.ui.component.RichTextAreaComponent;
import me.noramibu.itemeditor.ui.component.StyledTextFieldSection;
import me.noramibu.itemeditor.ui.component.TextStylingController;
import me.noramibu.itemeditor.ui.component.UiFactory;
import me.noramibu.itemeditor.ui.screen.ItemEditorScreen;
import me.noramibu.itemeditor.ui.util.ScrollStateUtil;
import me.noramibu.itemeditor.util.BookPageLayoutUtil;
import me.noramibu.itemeditor.util.ItemEditorText;
import me.noramibu.itemeditor.util.TextComponentUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WritableBookContent;
import net.minecraft.world.item.component.WrittenBookContent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.DoubleSupplier;

public final class BookEditorPanel implements EditorPanel {

    private static final int PAGE_MINIMAP_LIST_HEIGHT = 136;
    private static final int PAGE_EDITOR_MIN_WIDTH = 104;
    private static final int PAGE_EDITOR_MAX_WIDTH = 146;
    private static final int PAGE_EDITOR_MIN_HEIGHT = 112;
    private static final int PAGE_EDITOR_MAX_HEIGHT = 164;
    private static final int PAGE_TOOLTIP_MAX_LINES = 4;
    private static final int BOOK_METADATA_WIDE_THRESHOLD = 1380;
    private static final int BOOK_WORKSPACE_EDITOR_RESERVE = 120;
    private static final int PAGE_EDITOR_WIDTH_RESERVE = 40;
    private static final int MINIMAP_SCROLLBAR_BASE_THICKNESS = 8;
    private static final int MINIMAP_SCROLL_STEP_BASE = 10;
    private static final int GENERATION_PICKER_WIDTH = 180;
    private static final int MINIMAP_COUNT_HINT_WIDTH = 50;
    private static final int METADATA_EDITOR_HEIGHT = 42;
    private static final double COMPACT_LAYOUT_SCALE_THRESHOLD = 3.0d;
    private static final int COMPACT_LAYOUT_CONTENT_WIDTH_THRESHOLD = 620;
    private static final String SYMBOL_SECTION_COLLAPSED = "[+]";
    private static final String SYMBOL_SECTION_EXPANDED = "[-]";
    private static final String QUICK_ACTION_COPY = "Copy";
    private static final String QUICK_ACTION_BLANK = "Blank";
    private static final int COLLAPSE_TOGGLE_WIDTH_MIN = 26;
    private static final int COLLAPSE_TOGGLE_WIDTH_BASE = 34;
    private static final int PAGE_FRAME_PADDING = 10;
    private static final int PAGE_INDEX_BUTTON_SINGLE_INSET = 16;
    private static final int PAGE_INDEX_BUTTON_DOUBLE_INSET = 20;

    private static final List<GenerationOption> GENERATION_OPTIONS = List.of(
            new GenerationOption(0, "book.generation.original"),
            new GenerationOption(1, "book.generation.copy_of_original"),
            new GenerationOption(2, "book.generation.copy_of_copy"),
            new GenerationOption(3, "book.generation.tattered")
    );

    private final ItemEditorScreen screen;

    public BookEditorPanel(ItemEditorScreen screen) {
        this.screen = screen;
    }

    @Override
    public UIComponent build() {
        ItemEditorState.BookData book = this.screen.session().state().book;
        if (book.pages.isEmpty()) {
            book.pages.add("");
        }
        book.selectedPage = Math.clamp(book.selectedPage, 0, book.pages.size() - 1);
        boolean compactLayout = this.isCompactLayout();

        FlowLayout root = UiFactory.column();

        boolean canConvertWritable = this.screen.session().originalStack().is(Items.WRITABLE_BOOK);
        FlowLayout overview = UiFactory.section(
                ItemEditorText.tr("book.editing.title"),
                Component.empty()
        );
        var writtenOutputToggle = UiFactory.checkbox(
                ItemEditorText.tr("book.output_written"),
                book.writtenBook,
                PanelBindings.value(this.screen, value -> {
                    if (!canConvertWritable) {
                        return;
                    }
                    book.writtenBook = value;
                    if (value && book.author.isBlank() && this.screen.session().minecraft().player != null) {
                        book.author = this.screen.session().minecraft().player.getName().getString();
                    }
                }, this.screen::refreshCurrentPanel)
        );
        overview.child(writtenOutputToggle);
        if (!this.screen.session().state().customName.isBlank()) {
            overview.child(UiFactory.message(ItemEditorText.tr("book.title_hidden_by_name"), 0xFF8A8A));
        }
        if (!book.writtenBook) {
            overview.child(UiFactory.message(ItemEditorText.tr("book.writable_format_notice"), 0xF2C26B));
        }
        UiFactory.appendFillChild(root, overview);

        if (book.writtenBook) {
            FlowLayout metadata = UiFactory.section(
                    ItemEditorText.tr("book.metadata.title"),
                    Component.empty()
            );
            FlowLayout titleField = this.buildStyledStringField(
                    ItemEditorText.str("book.metadata.title_field"),
                    book.title,
                    WrittenBookContent.TITLE_MAX_LENGTH,
                    true,
                    PanelBindings.text(this.screen, value -> book.title = value)
            );

            FlowLayout authorField = this.buildStyledStringField(
                    ItemEditorText.str("book.metadata.author_field"),
                    book.author,
                    0,
                    false,
                    PanelBindings.text(this.screen, value -> book.author = value)
            );
            var generationButton = UiFactory.button(this.generationLabel(book.generation), UiFactory.ButtonTextPreset.STANDARD,  button -> this.screen.openDropdown(
                    button,
                    GENERATION_OPTIONS,
                    option -> ItemEditorText.str(option.labelKey()),
                    PanelBindings.value(this.screen, value -> {
                        book.generation = Integer.toString(value.value());
                        button.setMessage(this.generationLabel(book.generation));
                    })
            ));
            FlowLayout generationField = UiFactory.field(
                    ItemEditorText.tr("book.metadata.generation"),
                    Component.empty(),
                    generationButton.horizontalSizing(compactLayout ? Sizing.fill(100) : UiFactory.fixed(this.nonCompactGenerationPickerWidth()))
            );

            if (!compactLayout && this.guiWidth() >= BOOK_METADATA_WIDE_THRESHOLD) {
                FlowLayout topRow = UiFactory.row();
                topRow.child(titleField.horizontalSizing(Sizing.expand(100)));
                topRow.child(authorField.horizontalSizing(Sizing.expand(100)));
                metadata.child(topRow);
                metadata.child(generationField);
            } else {
                metadata.child(titleField);
                FlowLayout row = compactLayout ? UiFactory.column() : UiFactory.row();
                row.child(authorField);
                if (compactLayout) {
                    row.child(generationField);
                } else {
                    row.child(generationField.horizontalSizing(UiFactory.fixed(this.nonCompactGenerationPickerWidth())));
                }
                metadata.child(row);
            }
            UiFactory.appendFillChild(root, metadata);
        }

        FlowLayout pages = UiFactory.section(
                ItemEditorText.tr("book.pages.title"),
                Component.empty()
        );

        RichTextDocument initialDocument = this.documentForPage(book);
        LabelComponent stats = UiFactory.muted("");
        final RichTextAreaComponent[] editorHolder = new RichTextAreaComponent[1];
        Runnable refreshStats = () -> {
            BookPageLayoutUtil.PageMetrics metrics = BookPageLayoutUtil.measure(editorHolder[0].document(), Minecraft.getInstance().font);
            stats.text(Component.literal(this.pageStats(metrics, book.writtenBook)));
        };

        int pageEditorWidth = this.pageEditorWidth();
        int pageEditorHeight = this.pageEditorHeight();
        StyledTextFieldSection.BoundEditor pageSection = StyledTextFieldSection.create(
                this.screen,
                initialDocument,
                UiFactory.fixed(pageEditorWidth),
                UiFactory.fixed(pageEditorHeight),
                ItemEditorText.str("book.pages.blank"),
                book.writtenBook ? StyledTextFieldSection.StylePreset.bookPage() : StyledTextFieldSection.StylePreset.writableBookPage(),
                ItemEditorText.str("book.pages.color_title"),
                ItemEditorText.str("book.pages.gradient_title"),
                "",
                "",
                null,
                document -> this.validatePage(document, book.writtenBook),
                document -> {
                    PanelBindings.mutate(this.screen, () -> {
                        this.storePageDocument(book, document);
                        refreshStats.run();
                    });
                }
        );
        editorHolder[0] = pageSection.editor();
        refreshStats.run();

        FlowLayout pageFrame = UiFactory.subCard();
        int pageFrameWidth = pageEditorWidth + (PAGE_FRAME_PADDING * 2);
        pageFrame.horizontalSizing(UiFactory.fixed(pageFrameWidth));
        pageFrame.padding(Insets.of(PAGE_FRAME_PADDING));
        pageFrame.surface(Surface.flat(0xFFE7D7B3).and(Surface.outline(0xFF9C8765)));
        pageFrame.child(pageSection.editor());

        FlowLayout pageShell = UiFactory.column();
        pageShell.horizontalAlignment(HorizontalAlignment.CENTER);
        pageShell.horizontalSizing(UiFactory.fixed(pageFrameWidth));
        pageShell.child(pageSection.toolbar());
        pageShell.child(pageFrame);
        pageShell.child(stats);
        pageShell.child(pageSection.validation());

        FlowLayout editorField = UiFactory.field(
                ItemEditorText.tr("book.pages.editor"),
                Component.empty(),
                pageShell
        );

        FlowLayout workspace = compactLayout ? UiFactory.column() : UiFactory.row();
        int controlsWidth = this.pageControlsWidth();
        FlowLayout controls = this.buildPageControls(book, controlsWidth, compactLayout);
        FlowLayout miniMap = this.buildPageMiniMap(book, controlsWidth, compactLayout);
        FlowLayout sideColumn = UiFactory.column();
        sideColumn.horizontalSizing(compactLayout ? Sizing.fill(100) : UiFactory.fixed(controlsWidth));
        sideColumn.child(controls);
        sideColumn.child(miniMap);
        workspace.child(editorField);
        workspace.child(sideColumn);

        pages.child(workspace);

        UiFactory.appendFillChild(root, pages);
        return root;
    }

    private FlowLayout buildPageControls(ItemEditorState.BookData book, int controlsWidth, boolean compactLayout) {
        FlowLayout controls = UiFactory.subCard();
        controls.horizontalSizing(compactLayout ? Sizing.fill(100) : UiFactory.fixed(controlsWidth));
        controls.gap(2);
        FlowLayout header = UiFactory.row();
        header.child(UiFactory.title(ItemEditorText.tr("book.pages.current", book.selectedPage + 1, book.pages.size())).shadow(false).horizontalSizing(Sizing.expand(100)));
        ButtonComponent collapseToggle = UiFactory.button(Component.literal(book.uiPageControlsCollapsed ? SYMBOL_SECTION_COLLAPSED : SYMBOL_SECTION_EXPANDED), UiFactory.ButtonTextPreset.STANDARD,  button ->
                PanelBindings.mutateRefresh(this.screen, () -> book.uiPageControlsCollapsed = !book.uiPageControlsCollapsed)
        );
        collapseToggle.horizontalSizing(Sizing.fixed(this.collapseToggleWidth()));
        header.child(collapseToggle);
        controls.child(header);

        if (book.uiPageControlsCollapsed) {
            return controls;
        }

        FlowLayout navigation = compactLayout ? UiFactory.column() : UiFactory.row();
        ButtonComponent prev = this.actionButton(ItemEditorText.str("common.prev"), button -> {
            if (book.selectedPage > 0) {
                book.selectedPage--;
                this.screen.refreshCurrentPanel();
            }
        });
        ButtonComponent next = this.actionButton(ItemEditorText.str("common.next"), button -> {
            if (book.selectedPage < book.pages.size() - 1) {
                book.selectedPage++;
                this.screen.refreshCurrentPanel();
            }
        });
        ButtonComponent add = this.actionButton(ItemEditorText.str("common.add"), button -> {
            PanelBindings.mutateRefresh(this.screen, () -> {
                book.pages.add(book.selectedPage + 1, "");
                book.selectedPage++;
            });
        });
        ButtonComponent remove = this.actionButton(ItemEditorText.str("common.remove"), button -> {
            if (book.pages.size() > 1) {
                PanelBindings.mutateRefresh(this.screen, () -> {
                    book.pages.remove(book.selectedPage);
                    book.selectedPage = Math.max(0, book.selectedPage - 1);
                });
            }
        });
        prev.horizontalSizing(compactLayout ? Sizing.fill(100) : Sizing.expand(100));
        next.horizontalSizing(compactLayout ? Sizing.fill(100) : Sizing.expand(100));
        add.horizontalSizing(compactLayout ? Sizing.fill(100) : Sizing.expand(100));
        remove.horizontalSizing(compactLayout ? Sizing.fill(100) : Sizing.expand(100));
        navigation.child(prev);
        navigation.child(next);
        navigation.child(add);
        navigation.child(remove);

        FlowLayout reorder = compactLayout ? UiFactory.column() : UiFactory.row();
        ButtonComponent swapUp = this.actionButton(ItemEditorText.str("book.pages.swap_up"), button -> {
            if (book.selectedPage > 0) {
                PanelBindings.mutateRefresh(this.screen, () -> {
                    Collections.swap(book.pages, book.selectedPage, book.selectedPage - 1);
                    book.selectedPage--;
                });
            }
        });
        ButtonComponent swapDown = this.actionButton(ItemEditorText.str("book.pages.swap_down"), button -> {
            if (book.selectedPage < book.pages.size() - 1) {
                PanelBindings.mutateRefresh(this.screen, () -> {
                    Collections.swap(book.pages, book.selectedPage, book.selectedPage + 1);
                    book.selectedPage++;
                });
            }
        });
        swapUp.horizontalSizing(compactLayout ? Sizing.fill(100) : Sizing.expand(100));
        swapDown.horizontalSizing(compactLayout ? Sizing.fill(100) : Sizing.expand(100));
        reorder.child(swapUp);
        reorder.child(swapDown);

        FlowLayout quick = compactLayout ? UiFactory.column() : UiFactory.row();
        ButtonComponent duplicate = this.actionButton(QUICK_ACTION_COPY, button -> {
            PanelBindings.mutateRefresh(this.screen, () -> {
                String currentPage = book.pages.get(book.selectedPage);
                book.pages.add(book.selectedPage + 1, currentPage);
                book.selectedPage++;
            });
        });
        ButtonComponent blank = this.actionButton(QUICK_ACTION_BLANK, button -> {
            PanelBindings.mutateRefresh(this.screen, () -> book.pages.set(book.selectedPage, ""));
        });
        duplicate.horizontalSizing(compactLayout ? Sizing.fill(100) : Sizing.expand(100));
        blank.horizontalSizing(compactLayout ? Sizing.fill(100) : Sizing.expand(100));
        quick.child(duplicate);
        quick.child(blank);

        controls.child(navigation);
        controls.child(reorder);
        controls.child(quick);
        return controls;
    }

    private FlowLayout buildPageMiniMap(ItemEditorState.BookData book, int controlsWidth, boolean compactLayout) {
        FlowLayout card = UiFactory.subCard();
        card.horizontalSizing(compactLayout ? Sizing.fill(100) : UiFactory.fixed(controlsWidth));
        FlowLayout header = UiFactory.row();
        header.child(UiFactory.title(ItemEditorText.tr("book.pages.minimap")).shadow(false).horizontalSizing(Sizing.expand(100)));
        header.child(UiFactory.muted(Component.literal("(" + book.pages.size() + ")"), MINIMAP_COUNT_HINT_WIDTH));
        ButtonComponent collapseToggle = UiFactory.button(Component.literal(book.uiPageMiniMapCollapsed ? SYMBOL_SECTION_COLLAPSED : SYMBOL_SECTION_EXPANDED), UiFactory.ButtonTextPreset.STANDARD,  button ->
                PanelBindings.mutateRefresh(this.screen, () -> book.uiPageMiniMapCollapsed = !book.uiPageMiniMapCollapsed)
        );
        collapseToggle.horizontalSizing(Sizing.fixed(this.collapseToggleWidth()));
        header.child(collapseToggle);
        card.child(header);

        if (book.uiPageMiniMapCollapsed) {
            return card;
        }

        int miniMapHeight = this.pageMiniMapHeight();
        AtomicReference<ScrollContainer<FlowLayout>> scrollRef = new AtomicReference<>();
        FlowLayout pageList = this.buildPageIndexGrid(book, controlsWidth, () -> {
            ScrollContainer<FlowLayout> scroll = scrollRef.get();
            return scroll != null ? ScrollStateUtil.offset(scroll) : 0;
        });

        ScrollContainer<FlowLayout> scroll = InputSafeScrollContainer.vertical(Sizing.fill(100), UiFactory.fixed(miniMapHeight), pageList);
        scroll.verticalSizing(UiFactory.fixed(miniMapHeight));
        scroll.scrollbar(ScrollContainer.Scrollbar.vanillaFlat());
        scroll.scrollbarThiccness(UiFactory.scaledScrollbarThickness(MINIMAP_SCROLLBAR_BASE_THICKNESS));
        scroll.scrollStep(UiFactory.scaledScrollStep(MINIMAP_SCROLL_STEP_BASE));
        scrollRef.set(scroll);
        card.child(scroll);
        ScrollStateUtil.restore(scroll, book.miniMapScrollOffset);
        this.screen.session().minecraft().execute(() -> ScrollStateUtil.restore(scroll, book.miniMapScrollOffset));
        return card;
    }

    private FlowLayout buildPageIndexGrid(ItemEditorState.BookData book, int controlsWidth, DoubleSupplier miniMapOffsetSupplier) {
        boolean twoColumns = controlsWidth >= 190;
        int buttonWidth = twoColumns
                ? Math.max(1, (controlsWidth - PAGE_INDEX_BUTTON_DOUBLE_INSET) / 2)
                : Math.max(1, controlsWidth - PAGE_INDEX_BUTTON_SINGLE_INSET);

        FlowLayout pageList = UiFactory.column().gap(2);
        if (!twoColumns) {
            for (int index = 0; index < book.pages.size(); index++) {
                pageList.child(this.buildPageIndexButton(book, index, buttonWidth, miniMapOffsetSupplier));
            }
            return pageList;
        }

        for (int index = 0; index < book.pages.size(); index += 2) {
            FlowLayout row = UiFactory.row();
            row.child(this.buildPageIndexButton(book, index, buttonWidth, miniMapOffsetSupplier));
            if (index + 1 < book.pages.size()) {
                row.child(this.buildPageIndexButton(book, index + 1, buttonWidth, miniMapOffsetSupplier));
            }
            pageList.child(row);
        }
        return pageList;
    }

    private ButtonComponent buildPageIndexButton(ItemEditorState.BookData book, int pageIndex, int width, DoubleSupplier miniMapOffsetSupplier) {
        ButtonComponent button = UiFactory.button(Component.literal(Integer.toString(pageIndex + 1)), UiFactory.ButtonTextPreset.STANDARD,  component -> {
            if (book.selectedPage == pageIndex) {
                return;
            }
            PanelBindings.mutateRefresh(this.screen, () -> {
                double offset = miniMapOffsetSupplier.getAsDouble();
                book.miniMapScrollOffset = Double.isFinite(offset) ? offset : 0;
                book.selectedPage = pageIndex;
            });
        });
        button.horizontalSizing(UiFactory.fixed(width));
        button.active(pageIndex != book.selectedPage);
        button.tooltip(this.pageTooltip(book, pageIndex));
        return button;
    }

    private List<Component> pageTooltip(ItemEditorState.BookData book, int pageIndex) {
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(ItemEditorText.tr("book.pages.current", pageIndex + 1, book.pages.size()));
        List<Component> lines = this.pagePreviewLines(book.pages.get(pageIndex), book.writtenBook);
        if (lines.isEmpty()) {
            tooltip.add(Component.literal(ItemEditorText.str("book.pages.blank")));
        } else {
            tooltip.addAll(lines);
        }
        return tooltip;
    }

    private List<Component> pagePreviewLines(String page, boolean writtenBook) {
        RichTextDocument document = writtenBook
                ? RichTextDocument.fromMarkup(page)
                : RichTextDocument.fromPlainText(page);
        List<Component> lines = document.toLineComponents();
        if (lines.isEmpty()) {
            return List.of();
        }
        return lines.subList(0, Math.min(PAGE_TOOLTIP_MAX_LINES, lines.size()));
    }

    private ButtonComponent actionButton(String label, Consumer<ButtonComponent> action) {
        ButtonComponent button = UiFactory.button(label, UiFactory.ButtonTextPreset.STANDARD, action);
        button.horizontalSizing(Sizing.fill(100));
        return button;
    }

    private FlowLayout buildStyledStringField(
            String label,
            String initialValue,
            int maxLength,
            boolean paletteOnly,
            Consumer<String> onChanged
    ) {
        FlowLayout field = UiFactory.field(Component.literal(label), Component.empty(), UiFactory.column());
        FlowLayout content = (FlowLayout) field.children().getLast();
        content.clearChildren();

        StyledTextFieldSection.BoundEditor metadataSection = StyledTextFieldSection.create(
                this.screen,
                RichTextDocument.fromMarkup(initialValue),
                Sizing.fill(100),
                UiFactory.fixed(METADATA_EDITOR_HEIGHT),
                label,
                StyledTextFieldSection.StylePreset.bookMetadata(paletteOnly),
                ItemEditorText.str("book.metadata.picker.color", label),
                ItemEditorText.str("book.metadata.picker.gradient", label),
                "",
                "",
                null,
                document -> TextStylingController.validateSingleLineLegacy(document, label, maxLength),
                document -> onChanged.accept(TextComponentUtil.toLegacyPaletteString(document.toComponent()))
        );

        FlowLayout editorFrame = UiFactory.subCard();
        editorFrame.padding(Insets.of(5));
        editorFrame.surface(Surface.flat(0xB0121822).and(Surface.outline(0xFF3F4D63)));
        editorFrame.child(metadataSection.toolbar());
        editorFrame.child(metadataSection.editor());
        editorFrame.child(metadataSection.validation());

        content.child(editorFrame);
        return field;
    }

    private String validatePage(RichTextDocument document, boolean writtenBook) {
        if (!writtenBook && document.plainText().length() > WritableBookContent.PAGE_EDIT_LENGTH) {
            return ItemEditorText.str("book.pages.max_chars", WritableBookContent.PAGE_EDIT_LENGTH);
        }

        BookPageLayoutUtil.PageMetrics metrics = BookPageLayoutUtil.measure(document, Minecraft.getInstance().font);
        if (metrics.overflow()) {
            return writtenBook
                    ? ItemEditorText.str("book.pages.full_written")
                    : ItemEditorText.str("book.pages.full_writable");
        }

        return null;
    }

    private RichTextDocument documentForPage(ItemEditorState.BookData book) {
        String page = book.pages.get(book.selectedPage);
        return book.writtenBook ? RichTextDocument.fromMarkup(page) : RichTextDocument.fromPlainText(page);
    }

    private void storePageDocument(ItemEditorState.BookData book, RichTextDocument document) {
        book.pages.set(book.selectedPage, book.writtenBook ? document.toMarkup() : document.plainText());
    }

    private String pageStats(BookPageLayoutUtil.PageMetrics metrics, boolean writtenBook) {
        return writtenBook
                ? ItemEditorText.str(
                "book.pages.stats.written",
                metrics.rawLength(),
                Math.max(metrics.totalLines(), 1),
                BookPageLayoutUtil.MAX_VISIBLE_LINES
        )
                : ItemEditorText.str(
                "book.pages.stats.writable",
                metrics.rawLength(),
                WritableBookContent.PAGE_EDIT_LENGTH,
                Math.max(metrics.totalLines(), 1),
                BookPageLayoutUtil.MAX_VISIBLE_LINES
        );
    }

    private Component generationLabel(String generation) {
        int parsedValue;
        try {
            parsedValue = Integer.parseInt(generation);
        } catch (NumberFormatException exception) {
            parsedValue = 0;
        }
        final int generationValue = parsedValue;
        return ItemEditorText.tr(GENERATION_OPTIONS.stream()
                .filter(option -> option.value() == generationValue)
                .findFirst()
                .orElse(GENERATION_OPTIONS.getFirst())
                .labelKey());
    }

    private int guiWidth() {
        return this.screen.session().minecraft().getWindow().getGuiScaledWidth();
    }

    private int pageControlsWidth() {
        int preferred = this.clamp((int) Math.round(this.guiWidth() * 0.15d), 152, 220);
        int available = this.availableContentWidth();
        int maxAllowed = Math.max(1, available - UiFactory.scaledPixels(BOOK_WORKSPACE_EDITOR_RESERVE));
        return Math.max(1, Math.min(preferred, maxAllowed));
    }

    private boolean isCompactLayout() {
        return this.screen.session().minecraft().getWindow().getGuiScale() >= COMPACT_LAYOUT_SCALE_THRESHOLD
                || this.availableContentWidth() < UiFactory.scaledPixels(COMPACT_LAYOUT_CONTENT_WIDTH_THRESHOLD);
    }

    private int pageMiniMapHeight() {
        int guiHeight = this.screen.session().minecraft().getWindow().getGuiScaledHeight();
        int dynamic = (int) Math.round(guiHeight * 0.22d);
        return this.clamp(dynamic, 96, PAGE_MINIMAP_LIST_HEIGHT);
    }

    private int pageEditorWidth() {
        int preferred = this.clamp((int) Math.round(this.guiWidth() * 0.105d), PAGE_EDITOR_MIN_WIDTH, PAGE_EDITOR_MAX_WIDTH);
        int available = Math.max(1, this.availableContentWidth() - UiFactory.scaledPixels(PAGE_EDITOR_WIDTH_RESERVE));
        return Math.max(1, Math.min(preferred, available));
    }

    private int pageEditorHeight() {
        int guiHeight = this.screen.session().minecraft().getWindow().getGuiScaledHeight();
        int dynamic = (int) Math.round(guiHeight * 0.19d);
        return this.clamp(dynamic, PAGE_EDITOR_MIN_HEIGHT, PAGE_EDITOR_MAX_HEIGHT);
    }

    private int collapseToggleWidth() {
        return Math.max(COLLAPSE_TOGGLE_WIDTH_MIN, UiFactory.scaledPixels(COLLAPSE_TOGGLE_WIDTH_BASE));
    }

    private int availableContentWidth() {
        return Math.max(1, this.screen.editorContentWidthHint());
    }

    private int nonCompactGenerationPickerWidth() {
        return Math.max(1, Math.min(this.availableContentWidth(), GENERATION_PICKER_WIDTH));
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private record GenerationOption(int value, String labelKey) {
    }
}
