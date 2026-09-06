package me.noramibu.itemeditor.ui.panel;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.container.UIContainers;
import io.wispforest.owo.ui.core.Color;
import io.wispforest.owo.ui.core.HorizontalAlignment;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.Surface;
import io.wispforest.owo.ui.core.UIComponent;
import io.wispforest.owo.ui.core.VerticalAlignment;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.DoubleSupplier;
import me.noramibu.itemeditor.editor.EditorCategory;
import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.editor.text.RichTextDocument;
import me.noramibu.itemeditor.editor.text.RichTextLayoutUtil;
import me.noramibu.itemeditor.ui.component.EditorSearchDialog;
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

public final class BookEditorPanel implements EditorPanel {

    private static final int PAGE_MINIMAP_LIST_HEIGHT = 136;
    private static final int PAGE_EDITOR_HEIGHT = 134;
    private static final int PAGE_CONTROLS_IDEAL_WIDTH = 208;
    private static final int PAGE_CONTROLS_MIN_WIDTH = 150;
    private static final int PAGE_CONTROLS_MAX_WIDTH = 264;
    private static final int PAGE_WORKSPACE_MIN_EDITOR_WIDTH = 300;
    private static final int PAGE_MINIMAP_TWO_COLUMN_THRESHOLD = 220;
    private static final int PAGE_TOOLTIP_MAX_LINES = 4;
    private static final int BOOK_METADATA_WIDE_THRESHOLD = 1380;
    private static final int PAGE_LINE_NUMBER_WIDTH = 26;
    private static final int PAGE_LINE_NUMBER_TOP_OFFSET = 0;
    private static final int PAGE_LINE_NUMBER_RIGHT_PADDING = 2;
    private static final int PAGE_EDITOR_WRAP_PADDING = 0;

    private static final List<GenerationOption> GENERATION_OPTIONS = List.of(
            new GenerationOption(0, "book.generation.original"),
            new GenerationOption(1, "book.generation.copy_of_original"),
            new GenerationOption(2, "book.generation.copy_of_copy"),
            new GenerationOption(3, "book.generation.tattered"));

    private enum Field {
        EDITING("book.editing.title", false, ""),
        OUTPUT(
                "book.output_written",
                false,
                "minecraft:written_book_content minecraft:writable_book_content writtenBook"),
        METADATA("book.metadata.title", true, "minecraft:written_book_content"),
        TITLE("book.metadata.title_field", true, "title"),
        AUTHOR("book.metadata.author_field", true, "author"),
        GENERATION("book.metadata.generation", true, "generation"),
        PAGES("book.pages.title", false, "pages"),
        EDITOR("book.pages.editor", false, "pages text"),
        RENDER("common.render_objects", false, "uiRenderObjectsInBook"),
        CURRENT("book.pages.current", false, "selectedPage"),
        PREV("common.prev", false, "previous page"),
        NEXT("common.next", false, "next page"),
        ADD("common.add", false, "add page"),
        REMOVE("common.remove", false, "remove page"),
        UP("book.pages.swap_up", false, "move page up"),
        DOWN("book.pages.swap_down", false, "move page down"),
        MINIMAP("book.pages.minimap", false, "miniMapScrollOffset page index"),
        PAGE_COLOR("book.pages.color_title", true, "color shadow"),
        PAGE_GRADIENT("book.pages.gradient_title", true, "gradient"),
        META_COLOR("book.metadata.picker.color", true, "color shadow"),
        META_GRADIENT("book.metadata.picker.gradient", true, "gradient");

        private final DocumentPanelSearch.Label descriptor;
        private final boolean writtenOnly;

        Field(String key, boolean writtenOnly, String aliases) {
            this.descriptor = new DocumentPanelSearch.Label(key, aliases);
            this.writtenOnly = writtenOnly;
        }

        Component label(Object... args) {
            return descriptor.label(args);
        }

        String text(Object... args) {
            return label(args).getString();
        }
    }

    static boolean metadataVisible(ItemEditorState.BookData book) {
        return book.writtenBook;
    }

    private static StyledTextFieldSection.StylePreset pagePreset(ItemEditorState.BookData book) {
        return metadataVisible(book)
                ? StyledTextFieldSection.StylePreset.bookPage()
                : StyledTextFieldSection.StylePreset.writableBookPage();
    }

    private static String pageIndexKey(int index) {
        return Field.MINIMAP.descriptor.fullKey() + ".page." + index;
    }

    private static Component pageIndexLabel(int index) {
        return Component.translatableWithFallback(pageIndexKey(index), Integer.toString(index + 1));
    }

    @Override
    public List<EditorSearchDialog.Target> searchTargets() {
        return searchTargets(this.screen, this.screen.session().state().book);
    }

    static List<EditorSearchDialog.Target> searchTargets(ItemEditorScreen screen, ItemEditorState.BookData book) {
        var targets = new ArrayList<EditorSearchDialog.Target>();
        var category = EditorCategory.BOOK;
        for (Field field : Field.values()) {
            if (field.writtenOnly && !metadataVisible(book)) continue;
            if (field == Field.META_COLOR
                    || field == Field.META_GRADIENT
                    || field == Field.PAGE_COLOR
                    || field == Field.PAGE_GRADIENT) continue;
            if (field == Field.CURRENT && book.pages.isEmpty()) continue;
            List<String> parents =
                    switch (field) {
                        case EDITING, METADATA, PAGES -> List.of();
                        case OUTPUT -> List.of(Field.EDITING.text());
                        case TITLE, AUTHOR, GENERATION -> List.of(Field.METADATA.text());
                        default -> List.of(Field.PAGES.text());
                    };
            Object[] args =
                    field == Field.CURRENT ? new Object[] {book.selectedPage + 1, book.pages.size()} : new Object[0];
            targets.add(DocumentPanelSearch.target(
                    screen,
                    category,
                    parents,
                    field.text(args),
                    field.descriptor.terms(),
                    "",
                    field.descriptor.fullKey(),
                    () -> {}));
        }
        if (metadataVisible(book)) {
            for (GenerationOption option : GENERATION_OPTIONS) {
                targets.add(DocumentPanelSearch.target(
                        screen,
                        category,
                        List.of(Field.METADATA.text(), Field.GENERATION.text()),
                        option.label().getString(),
                        option.descriptor.terms(),
                        "",
                        Field.GENERATION.descriptor.fullKey(),
                        () -> {}));
            }
        }
        for (int index = 0; index < book.pages.size(); index++) {
            targets.add(DocumentPanelSearch.target(
                    screen,
                    category,
                    List.of(Field.PAGES.text(), Field.MINIMAP.text()),
                    Field.CURRENT.text(index + 1, book.pages.size()),
                    Field.CURRENT.descriptor.terms() + " " + Field.MINIMAP.descriptor.terms() + " " + (index + 1),
                    "",
                    pageIndexKey(index),
                    () -> {}));
        }
        return List.copyOf(targets);
    }

    private final ItemEditorScreen screen;

    public BookEditorPanel(ItemEditorScreen screen) {
        this.screen = screen;
    }

    @Override
    public UIComponent build() {
        ItemEditorState state = this.screen.session().state();
        ItemEditorState.BookData book = state.book;
        if (book.pages.isEmpty()) {
            book.pages.add("");
        }
        book.selectedPage = Math.clamp(book.selectedPage, 0, book.pages.size() - 1);

        FlowLayout root = UiFactory.column();

        boolean canConvertWritable = this.screen.session().originalStack().is(Items.WRITABLE_BOOK);
        FlowLayout overview = UiFactory.section(Field.EDITING.label(), Component.empty());
        var writtenOutputToggle = UiFactory.checkbox(
                Field.OUTPUT.label(),
                book.writtenBook,
                PanelBindings.value(
                        this.screen,
                        value -> {
                            if (!canConvertWritable) {
                                return;
                            }
                            book.writtenBook = value;
                            if (value
                                    && book.author.isBlank()
                                    && this.screen.session().minecraft().player != null) {
                                book.author = this.screen
                                        .session()
                                        .minecraft()
                                        .player
                                        .getName()
                                        .getString();
                            }
                        },
                        this.screen::refreshCurrentPanel));
        overview.child(writtenOutputToggle);
        if (!state.customName.isBlank()) {
            overview.child(UiFactory.message(ItemEditorText.tr("book.title_hidden_by_name"), 0xFF8A8A));
        }
        if (!book.writtenBook) {
            overview.child(UiFactory.message(ItemEditorText.tr("book.writable_format_notice"), 0xF2C26B));
        }
        root.child(overview);

        if (metadataVisible(book)) {
            FlowLayout metadata = UiFactory.section(Field.METADATA.label(), Component.empty());
            FlowLayout titleField = this.buildStyledStringField(
                    Field.TITLE,
                    book.title,
                    WrittenBookContent.TITLE_MAX_LENGTH,
                    true,
                    PanelBindings.text(this.screen, value -> book.title = value));

            FlowLayout authorField = this.buildStyledStringField(
                    Field.AUTHOR, book.author, 0, false, PanelBindings.text(this.screen, value -> book.author = value));
            var generationButton = UiFactory.button(
                    this.generationLabel(book.generation),
                    UiFactory.ButtonTextPreset.STANDARD,
                    button -> this.screen.openDropdown(
                            button,
                            GENERATION_OPTIONS,
                            option -> option.label().getString(),
                            PanelBindings.value(this.screen, value -> {
                                book.generation = Integer.toString(value.value());
                                button.setMessage(this.generationLabel(book.generation));
                            })));
            FlowLayout generationField = UiFactory.field(
                    Field.GENERATION.label(), Component.empty(), generationButton.horizontalSizing(Sizing.fill(100)));

            if (this.availableContentWidth() >= BOOK_METADATA_WIDE_THRESHOLD) {
                FlowLayout topRow = UiFactory.row();
                topRow.child(titleField.horizontalSizing(Sizing.fill(49)));
                topRow.child(authorField.horizontalSizing(Sizing.fill(49)));
                metadata.child(topRow);
                metadata.child(generationField);
            } else {
                metadata.child(titleField);
                metadata.child(authorField);
                metadata.child(generationField);
            }
            root.child(metadata);
        }

        FlowLayout pages = UiFactory.section(Field.PAGES.label(), Component.empty());
        int availableWidth = this.availableContentWidth();
        WorkspaceLayout workspaceLayout = this.resolveWorkspaceLayout(availableWidth);

        RichTextDocument initialDocument = this.documentForPage(book);
        LabelComponent stats = UiFactory.muted("");
        final RichTextAreaComponent[] editorHolder = new RichTextAreaComponent[1];
        final List<LabelComponent> lineNumberLabels = new ArrayList<>();
        Runnable refreshStats = () -> {
            RichTextDocument currentDocument = editorHolder[0].document();
            BookPageLayoutUtil.PageMetrics metrics = this.measureVisiblePageMetrics(currentDocument);
            stats.text(Component.literal(this.pageStats(metrics, book.writtenBook)));
            this.updateBookPageLineNumbers(editorHolder[0], lineNumberLabels);
        };

        StyledTextFieldSection.BoundEditor pageSection = StyledTextFieldSection.create(
                this.screen,
                initialDocument,
                Sizing.fill(100),
                Sizing.fixed(PAGE_EDITOR_HEIGHT),
                ItemEditorText.str("book.pages.blank"),
                pagePreset(book),
                Field.PAGE_COLOR.label().getString(),
                Field.PAGE_GRADIENT.label().getString(),
                "",
                "",
                null,
                document -> this.validatePage(document, book.writtenBook),
                document -> PanelBindings.mutate(this.screen, () -> {
                    this.storePageDocument(book, document);
                    refreshStats.run();
                }),
                false,
                workspaceLayout.editorWidthHint());
        editorHolder[0] = pageSection.editor();
        pageSection
                .editor()
                .onViewportChanged()
                .subscribe(() -> this.updateBookPageLineNumbers(pageSection.editor(), lineNumberLabels));
        this.applyBookEditorRenderMode(editorHolder[0], state.uiRenderObjectsInBook);

        FlowLayout pageFrame = UiFactory.subCard();
        pageFrame.horizontalSizing(Sizing.fill(100));
        pageFrame.gap(0);
        pageFrame.padding(Insets.of(10));
        pageFrame.allowOverflow(false);
        pageFrame.surface(Surface.flat(0xFFE7D7B3).and(Surface.outline(0xFF9C8765)));
        FlowLayout editorRow = UiFactory.row();
        editorRow.horizontalSizing(Sizing.fill(100));
        editorRow.verticalAlignment(VerticalAlignment.TOP);
        editorRow.child(this.buildPageLineNumberGutter(pageSection.editor(), lineNumberLabels));
        editorRow.child(pageSection.editor().horizontalSizing(Sizing.expand(100)));
        pageFrame.child(editorRow);
        pageFrame.child(UiFactory.horizontalScrollbarRow(pageSection.editor(), PAGE_LINE_NUMBER_WIDTH));
        refreshStats.run();

        FlowLayout pageShell = UiFactory.column();
        pageShell.id(Field.EDITOR.descriptor.fullKey());
        pageShell.horizontalAlignment(HorizontalAlignment.CENTER);
        pageShell.horizontalSizing(Sizing.fill(100));
        pageShell.child(pageSection.toolbar().horizontalSizing(Sizing.fill(100)));
        pageShell.child(UiFactory.checkbox(Field.RENDER.label(), state.uiRenderObjectsInBook, value -> {
            state.uiRenderObjectsInBook = value;
            if (editorHolder[0] != null) {
                this.applyBookEditorRenderMode(editorHolder[0], value);
                refreshStats.run();
            }
        }));
        pageShell.child(pageFrame);
        pageShell.child(stats);
        pageShell.child(pageSection.validation());

        FlowLayout editorField = UiFactory.field(Field.EDITOR.label(), Component.empty(), pageShell);

        boolean stackWorkspace = workspaceLayout.stackWorkspace();
        int controlsWidth = workspaceLayout.controlsWidth();

        FlowLayout workspace = stackWorkspace ? UiFactory.column() : UiFactory.row();
        FlowLayout controls = this.buildPageControls(book, controlsWidth);
        FlowLayout miniMap = this.buildPageMiniMap(book, controlsWidth);
        FlowLayout sideColumn = UiFactory.column();
        sideColumn.horizontalSizing(stackWorkspace ? Sizing.fill(100) : Sizing.fixed(controlsWidth));
        sideColumn.child(controls);
        sideColumn.child(miniMap);
        if (stackWorkspace) {
            workspace.child(editorField.horizontalSizing(Sizing.fill(100)));
            workspace.child(sideColumn);
        } else {
            workspace.child(editorField.horizontalSizing(Sizing.expand(100)));
            workspace.child(sideColumn);
        }

        pages.child(workspace);

        root.child(pages);
        return root;
    }

    private FlowLayout buildPageControls(ItemEditorState.BookData book, int controlsWidth) {
        FlowLayout controls = UiFactory.subCard();
        controls.horizontalSizing(Sizing.fixed(controlsWidth));
        controls.gap(2);
        controls.child(UiFactory.title(Field.CURRENT.label(book.selectedPage + 1, book.pages.size()))
                .shadow(false));

        Component prevLabel = Field.PREV.label();
        Component nextLabel = Field.NEXT.label();
        Component addLabel = Field.ADD.label();
        Component removeLabel = Field.REMOVE.label();
        Component swapUpLabel = Field.UP.label();
        Component swapDownLabel = Field.DOWN.label();

        ButtonComponent prev = this.actionButton(prevLabel, button -> {
            if (book.selectedPage > 0) {
                book.selectedPage--;
                this.screen.refreshCurrentPanel();
            }
        });
        ButtonComponent next = this.actionButton(nextLabel, button -> {
            if (book.selectedPage < book.pages.size() - 1) {
                book.selectedPage++;
                this.screen.refreshCurrentPanel();
            }
        });
        ButtonComponent add = this.actionButton(
                addLabel,
                button -> PanelBindings.mutateRefresh(this.screen, () -> {
                    book.pages.add(book.selectedPage + 1, "");
                    book.selectedPage++;
                }));
        ButtonComponent remove = this.actionButton(removeLabel, button -> {
            if (book.pages.size() > 1) {
                PanelBindings.mutateRefresh(this.screen, () -> {
                    book.pages.remove(book.selectedPage);
                    book.selectedPage = Math.max(0, book.selectedPage - 1);
                });
            }
        });

        ButtonComponent swapUp = this.actionButton(swapUpLabel, button -> {
            if (book.selectedPage > 0) {
                PanelBindings.mutateRefresh(this.screen, () -> {
                    Collections.swap(book.pages, book.selectedPage, book.selectedPage - 1);
                    book.selectedPage--;
                });
            }
        });
        ButtonComponent swapDown = this.actionButton(swapDownLabel, button -> {
            if (book.selectedPage < book.pages.size() - 1) {
                PanelBindings.mutateRefresh(this.screen, () -> {
                    Collections.swap(book.pages, book.selectedPage, book.selectedPage + 1);
                    book.selectedPage++;
                });
            }
        });
        controls.child(UiFactory.actionButtonRow(prev, next, add, remove));
        controls.child(UiFactory.actionButtonRow(swapUp, swapDown));
        return controls;
    }

    private FlowLayout buildPageMiniMap(ItemEditorState.BookData book, int controlsWidth) {
        FlowLayout card = UiFactory.subCard();
        card.horizontalSizing(Sizing.fixed(controlsWidth));
        card.child(UiFactory.title(Field.MINIMAP.label()).shadow(false));
        AtomicReference<ScrollContainer<FlowLayout>> scrollRef = new AtomicReference<>();
        FlowLayout pageList = this.buildPageIndexGrid(
                book,
                () -> {
                    ScrollContainer<FlowLayout> scroll = scrollRef.get();
                    return scroll != null ? ScrollStateUtil.offset(scroll) : 0;
                },
                controlsWidth);

        ScrollContainer<FlowLayout> scroll = UIContainers.verticalScroll(Sizing.fill(100), Sizing.fixed(136), pageList);
        scroll.verticalSizing(Sizing.fixed(PAGE_MINIMAP_LIST_HEIGHT));
        scroll.scrollbar(ScrollContainer.Scrollbar.vanillaFlat());
        scroll.scrollbarThiccness(8);
        scroll.scrollStep(10);
        scrollRef.set(scroll);
        card.child(scroll);
        ScrollStateUtil.restore(scroll, book.miniMapScrollOffset);
        this.screen.session().minecraft().execute(() -> ScrollStateUtil.restore(scroll, book.miniMapScrollOffset));
        return card;
    }

    private FlowLayout buildPageIndexGrid(
            ItemEditorState.BookData book, DoubleSupplier miniMapOffsetSupplier, int controlsWidth) {
        boolean twoColumns = controlsWidth >= PAGE_MINIMAP_TWO_COLUMN_THRESHOLD;
        FlowLayout pageList = UiFactory.column().gap(2);
        if (!twoColumns) {
            for (int index = 0; index < book.pages.size(); index++) {
                ButtonComponent button = this.buildPageIndexButton(book, index, miniMapOffsetSupplier);
                button.horizontalSizing(Sizing.fill(100));
                pageList.child(button);
            }
            return pageList;
        }

        for (int index = 0; index < book.pages.size(); index += 2) {
            FlowLayout row = UiFactory.row();
            ButtonComponent left = this.buildPageIndexButton(book, index, miniMapOffsetSupplier);
            boolean hasRight = index + 1 < book.pages.size();
            left.horizontalSizing(Sizing.fill(hasRight ? 49 : 100));
            row.child(left);
            if (hasRight) {
                ButtonComponent right = this.buildPageIndexButton(book, index + 1, miniMapOffsetSupplier);
                right.horizontalSizing(Sizing.fill(49));
                row.child(right);
            }
            pageList.child(row);
        }
        return pageList;
    }

    private ButtonComponent buildPageIndexButton(
            ItemEditorState.BookData book, int pageIndex, DoubleSupplier miniMapOffsetSupplier) {
        ButtonComponent button =
                UiFactory.button(pageIndexLabel(pageIndex), UiFactory.ButtonTextPreset.STANDARD, component -> {
                    if (book.selectedPage == pageIndex) {
                        return;
                    }
                    PanelBindings.mutateRefresh(this.screen, () -> {
                        double offset = miniMapOffsetSupplier.getAsDouble();
                        book.miniMapScrollOffset = Double.isFinite(offset) ? offset : 0;
                        book.selectedPage = pageIndex;
                    });
                });
        button.active(pageIndex != book.selectedPage);
        button.tooltip(this.pageTooltip(book, pageIndex));
        return button;
    }

    private List<Component> pageTooltip(ItemEditorState.BookData book, int pageIndex) {
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(Field.CURRENT.label(pageIndex + 1, book.pages.size()));
        List<Component> lines = this.pagePreviewLines(book.pages.get(pageIndex), book.writtenBook);
        if (lines.isEmpty()) {
            tooltip.add(ItemEditorText.tr("book.pages.blank"));
        } else {
            tooltip.addAll(lines);
        }
        return tooltip;
    }

    private List<Component> pagePreviewLines(String page, boolean writtenBook) {
        RichTextDocument document = !writtenBook
                ? RichTextDocument.fromPlainText(page)
                : TextComponentUtil.containsStructuredToken(page)
                        ? RichTextDocument.fromComponentForLayout(TextComponentUtil.parseMarkup(page))
                        : RichTextDocument.fromMarkup(page);
        List<Component> lines = document.toLineComponents();
        return lines.isEmpty() ? List.of() : lines.subList(0, Math.min(PAGE_TOOLTIP_MAX_LINES, lines.size()));
    }

    private ButtonComponent actionButton(Component label, Consumer<ButtonComponent> action) {
        ButtonComponent button = UiFactory.button(label, UiFactory.ButtonTextPreset.STANDARD, action);
        button.horizontalSizing(Sizing.content());
        return button;
    }

    private FlowLayout buildStyledStringField(
            Field declaration, String initialValue, int maxLength, boolean paletteOnly, Consumer<String> onChanged) {
        String label = declaration.text();
        FlowLayout field = UiFactory.field(declaration.label(), Component.empty(), UiFactory.column());
        field.id(declaration.descriptor.fullKey());
        FlowLayout content = (FlowLayout) field.children().getLast();
        content.clearChildren();

        StyledTextFieldSection.BoundEditor metadataSection = StyledTextFieldSection.create(
                this.screen,
                RichTextDocument.fromMarkup(initialValue),
                Sizing.fill(100),
                Sizing.fixed(42),
                label,
                StyledTextFieldSection.StylePreset.bookMetadata(paletteOnly),
                Field.META_COLOR.text(label),
                Field.META_GRADIENT.text(label),
                "",
                "",
                null,
                document -> TextStylingController.validateSingleLineLegacy(document, label, maxLength),
                document -> onChanged.accept(TextComponentUtil.toLegacyPaletteString(document.toComponent())));
        metadataSection.editor().lineWrap(false);

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

        BookPageLayoutUtil.PageMetrics metrics =
                BookPageLayoutUtil.measure(document, Minecraft.getInstance().font, true);
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
        book.pages.set(
                book.selectedPage,
                book.writtenBook ? TextComponentUtil.serializeEditorDocument(document) : document.plainText());
    }

    private void applyBookEditorRenderMode(RichTextAreaComponent editor, boolean renderStructured) {
        double previousScroll = editor.scrollAmount();
        editor.lineWrap(true).lineWrapPadding(PAGE_EDITOR_WRAP_PADDING).showSoftWrapMarkers(true);
        if (renderStructured) {
            editor.structuredRenderMode(true)
                    .lineWrapWidthOverride(BookPageLayoutUtil.TEXT_WIDTH)
                    .renderStructuredObjects(true);
            editor.setScrollAmount(previousScroll);
            return;
        }
        editor.structuredRenderMode(false).lineWrapWidthOverride(-1).renderStructuredObjects(false);
        editor.setScrollAmount(previousScroll);
    }

    private BookPageLayoutUtil.PageMetrics measureVisiblePageMetrics(RichTextDocument document) {
        List<RichTextLayoutUtil.LineLayout> logicalLines =
                RichTextLayoutUtil.layout(document, Minecraft.getInstance().font, BookPageLayoutUtil.TEXT_WIDTH, true);
        int totalLines = logicalLines.size();
        boolean overflow = totalLines > BookPageLayoutUtil.MAX_VISIBLE_LINES;
        int visibleCharCount = this.visibleBookCharacterCount(document.plainText());
        return new BookPageLayoutUtil.PageMetrics(visibleCharCount, totalLines, overflow);
    }

    private FlowLayout buildPageLineNumberGutter(RichTextAreaComponent editor, List<LabelComponent> labels) {
        FlowLayout gutter = UiFactory.column();
        gutter.horizontalSizing(UiFactory.fixed(PAGE_LINE_NUMBER_WIDTH));
        gutter.gap(0);
        int lineSlotHeight = Math.max(1, editor.displayedLineHeight());
        int topInset = Math.max(0, editor.displayedTopInset());
        for (int line = 1; line <= BookPageLayoutUtil.MAX_VISIBLE_LINES; line++) {
            FlowLayout lineSlot = UiFactory.row();
            lineSlot.horizontalSizing(Sizing.fill(100));
            lineSlot.verticalSizing(Sizing.fixed(lineSlotHeight));
            lineSlot.horizontalAlignment(HorizontalAlignment.RIGHT);
            lineSlot.verticalAlignment(VerticalAlignment.TOP);
            lineSlot.padding(Insets.right(UiFactory.scaledPixels(PAGE_LINE_NUMBER_RIGHT_PADDING)));
            LabelComponent number = new PlainLabelComponent(Component.literal(Integer.toString(line)))
                    .lineHeight(lineSlotHeight)
                    .lineSpacing(0)
                    .color(Color.ofRgb(0xA9B5C0))
                    .shadow(false);
            labels.add(number);
            lineSlot.child(number);
            gutter.child(lineSlot);
        }
        gutter.margins(Insets.top(topInset + UiFactory.scaledPixels(PAGE_LINE_NUMBER_TOP_OFFSET)));
        return gutter;
    }

    private void updateBookPageLineNumbers(RichTextAreaComponent editor, List<LabelComponent> labels) {
        if (editor == null || labels.isEmpty()) {
            return;
        }

        int firstVisibleDisplayLine = editor.firstVisibleDisplayedLineIndex();

        for (int slot = 0; slot < labels.size(); slot++) {
            int pageLineNumber = editor.logicalLineNumberForDisplayedLineIndex(firstVisibleDisplayLine + slot);
            labels.get(slot).text(Component.literal(Integer.toString(pageLineNumber)));
        }
    }

    private int visibleBookCharacterCount(String rawText) {
        return RichTextLayoutUtil.logicalMetricsForEventPayload(rawText).charCount();
    }

    private WorkspaceLayout resolveWorkspaceLayout(int availableContentWidth) {
        int availableWidth = Math.max(1, availableContentWidth);
        int horizontalSafetyGap = UiFactory.scaledPixels(18);
        boolean stackWorkspace =
                availableWidth < PAGE_CONTROLS_MIN_WIDTH + PAGE_WORKSPACE_MIN_EDITOR_WIDTH + horizontalSafetyGap;

        int controlsWidth;
        if (stackWorkspace) {
            controlsWidth = Math.max(PAGE_CONTROLS_MIN_WIDTH, availableWidth - UiFactory.scaledPixels(8));
        } else {
            int proportional = (int) Math.round(availableWidth * 0.30d);
            controlsWidth = Math.max(
                    PAGE_CONTROLS_IDEAL_WIDTH,
                    Math.clamp(proportional, PAGE_CONTROLS_MIN_WIDTH, PAGE_CONTROLS_MAX_WIDTH));
        }

        int editorWidth =
                stackWorkspace ? availableWidth : Math.max(1, availableWidth - controlsWidth - horizontalSafetyGap);
        int toolbarWidthHint = Math.max(120, editorWidth - UiFactory.scaledPixels(22));
        return new WorkspaceLayout(stackWorkspace, controlsWidth, toolbarWidthHint);
    }

    private String pageStats(BookPageLayoutUtil.PageMetrics metrics, boolean writtenBook) {
        return writtenBook
                ? ItemEditorText.str(
                        "book.pages.stats.written",
                        metrics.rawLength(),
                        Math.max(metrics.totalLines(), 1),
                        BookPageLayoutUtil.MAX_VISIBLE_LINES)
                : ItemEditorText.str(
                        "book.pages.stats.writable",
                        metrics.rawLength(),
                        WritableBookContent.PAGE_EDIT_LENGTH,
                        Math.max(metrics.totalLines(), 1),
                        BookPageLayoutUtil.MAX_VISIBLE_LINES);
    }

    private Component generationLabel(String generation) {
        int parsedValue;
        try {
            parsedValue = Integer.parseInt(generation);
        } catch (NumberFormatException exception) {
            parsedValue = 0;
        }
        final int generationValue = parsedValue;
        return GENERATION_OPTIONS.stream()
                .filter(option -> option.value() == generationValue)
                .findFirst()
                .orElse(GENERATION_OPTIONS.getFirst())
                .label();
    }

    private int availableContentWidth() {
        return Math.max(1, this.screen.editorContentWidthHint());
    }

    private static final class PlainLabelComponent extends LabelComponent {
        private PlainLabelComponent(Component text) {
            super(text);
        }
    }

    private record WorkspaceLayout(boolean stackWorkspace, int controlsWidth, int editorWidthHint) {}

    private record GenerationOption(int value, DocumentPanelSearch.Label descriptor) {
        GenerationOption(int value, String key) {
            this(value, new DocumentPanelSearch.Label(key, "generation " + value));
        }

        Component label() {
            return descriptor.label();
        }
    }
}
