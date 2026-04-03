package me.noramibu.itemeditor.ui.panel;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.core.HorizontalAlignment;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.Surface;
import io.wispforest.owo.ui.container.FlowLayout;
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
    private static final int PAGE_EDITOR_WIDTH = 122;
    private static final int PAGE_EDITOR_HEIGHT = 134;
    private static final int PAGE_CONTROLS_WIDTH = 174;
    private static final int PAGE_TOOLTIP_MAX_LINES = 4;
    private static final int BOOK_METADATA_WIDE_THRESHOLD = 1380;

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
    public FlowLayout build() {
        ItemEditorState.BookData book = this.screen.session().state().book;
        if (book.pages.isEmpty()) {
            book.pages.add("");
        }
        book.selectedPage = Math.clamp(book.selectedPage, 0, book.pages.size() - 1);

        FlowLayout root = UiFactory.column();

        boolean canConvertWritable = this.screen.session().originalStack().is(Items.WRITABLE_BOOK);
        FlowLayout overview = UiFactory.section(
                ItemEditorText.tr("book.editing.title"),
                Component.empty()
        );
        if (canConvertWritable) {
            overview.child(UiFactory.checkbox(
                    ItemEditorText.tr("book.output_written"),
                    book.writtenBook,
                    PanelBindings.value(this.screen, value -> {
                        book.writtenBook = value;
                        if (value && book.author.isBlank() && this.screen.session().minecraft().player != null) {
                            book.author = this.screen.session().minecraft().player.getName().getString();
                        }
                    }, this.screen::refreshCurrentPanel)
            ));
        }
        if (!this.screen.session().state().customName.isBlank()) {
            overview.child(UiFactory.message(ItemEditorText.tr("book.title_hidden_by_name"), 0xFF8A8A));
        }
        root.child(overview);

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
            var generationButton = UiFactory.button(this.generationLabel(book.generation), button -> this.screen.openDropdown(
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
                    generationButton.horizontalSizing(Sizing.fixed(180))
            );

            if (this.guiWidth() >= BOOK_METADATA_WIDE_THRESHOLD) {
                FlowLayout topRow = UiFactory.row();
                topRow.child(titleField.horizontalSizing(Sizing.fill(50)));
                topRow.child(authorField.horizontalSizing(Sizing.fill(50)));
                metadata.child(topRow);
                metadata.child(generationField);
            } else {
                metadata.child(titleField);
                FlowLayout row = UiFactory.row();
                row.child(authorField.horizontalSizing(Sizing.fill(100)));
                row.child(generationField.horizontalSizing(Sizing.fixed(180)));
                metadata.child(row);
            }
            root.child(metadata);
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

        StyledTextFieldSection.BoundEditor pageSection = StyledTextFieldSection.create(
                this.screen,
                initialDocument,
                Sizing.fixed(PAGE_EDITOR_WIDTH),
                Sizing.fixed(PAGE_EDITOR_HEIGHT),
                ItemEditorText.str("book.pages.blank"),
                StyledTextFieldSection.StylePreset.bookPage(),
                ItemEditorText.str("book.pages.color_title"),
                ItemEditorText.str("book.pages.gradient_title"),
                "",
                "",
                () -> this.enableWrittenOutput(book, refreshStats),
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
        pageFrame.horizontalSizing(Sizing.content());
        pageFrame.padding(Insets.of(10));
        pageFrame.allowOverflow(false);
        pageFrame.surface(Surface.flat(0xFFE7D7B3).and(Surface.outline(0xFF9C8765)));
        pageFrame.child(pageSection.editor());

        FlowLayout pageShell = UiFactory.column();
        pageShell.horizontalAlignment(HorizontalAlignment.CENTER);
        pageShell.horizontalSizing(Sizing.content());
        pageShell.child(pageSection.toolbar());
        pageShell.child(pageFrame);
        pageShell.child(stats);
        pageShell.child(pageSection.validation());

        FlowLayout editorField = UiFactory.field(
                ItemEditorText.tr("book.pages.editor"),
                Component.empty(),
                pageShell
        );

        FlowLayout workspace = UiFactory.row();
        FlowLayout controls = this.buildPageControls(book);
        FlowLayout miniMap = this.buildPageMiniMap(book);
        FlowLayout sideColumn = UiFactory.column();
        sideColumn.horizontalSizing(Sizing.fixed(PAGE_CONTROLS_WIDTH));
        sideColumn.child(controls);
        sideColumn.child(miniMap);
        workspace.child(editorField.horizontalSizing(Sizing.fill(100)));
        workspace.child(sideColumn);

        pages.child(workspace);

        root.child(pages);
        return root;
    }

    private FlowLayout buildPageControls(ItemEditorState.BookData book) {
        FlowLayout controls = UiFactory.subCard();
        controls.horizontalSizing(Sizing.fixed(PAGE_CONTROLS_WIDTH));
        controls.gap(2);
        controls.child(UiFactory.title(ItemEditorText.tr("book.pages.current", book.selectedPage + 1, book.pages.size())).shadow(false));

        FlowLayout navigation = UiFactory.row();
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
        prev.horizontalSizing(Sizing.fill(25));
        next.horizontalSizing(Sizing.fill(25));
        add.horizontalSizing(Sizing.fill(25));
        remove.horizontalSizing(Sizing.fill(25));
        navigation.child(prev);
        navigation.child(next);
        navigation.child(add);
        navigation.child(remove);

        FlowLayout reorder = UiFactory.row();
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
        swapUp.horizontalSizing(Sizing.fill(50));
        swapDown.horizontalSizing(Sizing.fill(50));
        reorder.child(swapUp);
        reorder.child(swapDown);
        controls.child(navigation);
        controls.child(reorder);
        return controls;
    }

    private FlowLayout buildPageMiniMap(ItemEditorState.BookData book) {
        FlowLayout card = UiFactory.subCard();
        card.horizontalSizing(Sizing.fixed(PAGE_CONTROLS_WIDTH));
        card.child(UiFactory.title(ItemEditorText.tr("book.pages.minimap")).shadow(false));
        AtomicReference<ScrollContainer<FlowLayout>> scrollRef = new AtomicReference<>();
        FlowLayout pageList = this.buildPageIndexGrid(book, () -> {
            ScrollContainer<FlowLayout> scroll = scrollRef.get();
            return scroll != null ? ScrollStateUtil.offset(scroll) : 0;
        });

        ScrollContainer<FlowLayout> scroll = Containers.verticalScroll(Sizing.fill(100), Sizing.fixed(136), pageList);
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

    private FlowLayout buildPageIndexGrid(ItemEditorState.BookData book, DoubleSupplier miniMapOffsetSupplier) {
        boolean twoColumns = PAGE_CONTROLS_WIDTH >= 190;
        int buttonWidth = twoColumns ? (PAGE_CONTROLS_WIDTH - 20) / 2 : PAGE_CONTROLS_WIDTH - 16;

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
        ButtonComponent button = UiFactory.button(Component.literal(Integer.toString(pageIndex + 1)), component -> {
            if (book.selectedPage == pageIndex) {
                return;
            }
            PanelBindings.mutateRefresh(this.screen, () -> {
                double offset = miniMapOffsetSupplier.getAsDouble();
                book.miniMapScrollOffset = Double.isFinite(offset) ? offset : 0;
                book.selectedPage = pageIndex;
            });
        });
        button.horizontalSizing(Sizing.fixed(width));
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
        ButtonComponent button = UiFactory.button(label, action);
        button.horizontalSizing(Sizing.content());
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
                Sizing.fixed(42),
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

    private void enableWrittenOutput(ItemEditorState.BookData book, Runnable refreshStats) {
        if (book.writtenBook) {
            return;
        }

        PanelBindings.mutate(this.screen, () -> {
            book.writtenBook = true;
            if (book.author.isBlank() && this.screen.session().minecraft().player != null) {
                book.author = this.screen.session().minecraft().player.getName().getString();
            }
            refreshStats.run();
        });
    }

    private int guiWidth() {
        return this.screen.session().minecraft().getWindow().getGuiScaledWidth();
    }

    private record GenerationOption(int value, String labelKey) {
    }
}
