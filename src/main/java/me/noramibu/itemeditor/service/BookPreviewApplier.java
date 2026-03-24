package me.noramibu.itemeditor.service;

import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.editor.ValidationMessage;
import me.noramibu.itemeditor.util.ItemEditorText;
import me.noramibu.itemeditor.util.TextComponentUtil;
import me.noramibu.itemeditor.util.ValidationUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WritableBookContent;
import net.minecraft.world.item.component.WrittenBookContent;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

final class BookPreviewApplier extends AbstractPreviewApplierSupport implements ItemPreviewApplier {

    @Override
    public void apply(ItemPreviewApplyContext context) {
        if (this.sameBook(context.state().book, context.baselineState().book)) {
            this.restoreOriginalComponent(context.originalStack(), context.previewStack(), DataComponents.WRITTEN_BOOK_CONTENT);
            this.restoreOriginalComponent(context.originalStack(), context.previewStack(), DataComponents.WRITABLE_BOOK_CONTENT);
            return;
        }

        if (context.previewStack().is(Items.WRITTEN_BOOK)) {
            if (context.state().book.title.isBlank()) {
                context.messages().add(ValidationMessage.error(ItemEditorText.str("preview.validation.book_title_required")));
                return;
            }
            if (context.state().book.title.length() > WrittenBookContent.TITLE_MAX_LENGTH) {
                context.messages().add(ValidationMessage.error(ItemEditorText.str("preview.validation.book_title_length", WrittenBookContent.TITLE_MAX_LENGTH)));
            }

            List<Filterable<net.minecraft.network.chat.Component>> pages = new ArrayList<>();
            for (String page : context.state().book.pages) {
                pages.add(Filterable.passThrough(TextComponentUtil.parseMarkup(page)));
            }

            Integer generation = ValidationUtil.parseInt(
                    context.state().book.generation,
                    ItemEditorText.str("book.metadata.generation"),
                    0,
                    WrittenBookContent.MAX_GENERATION,
                    context.messages()
            );
            if (generation == null) {
                generation = 0;
            }

            context.previewStack().set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(
                    Filterable.passThrough(context.state().book.title),
                    context.state().book.author.isBlank() ? ItemEditorText.str("screen.title") : context.state().book.author,
                    generation,
                    pages,
                    false
            ));
            this.clearToPrototype(context.previewStack(), DataComponents.WRITABLE_BOOK_CONTENT);
        } else if (context.previewStack().is(Items.WRITABLE_BOOK)) {
            if (context.state().book.pages.size() > WritableBookContent.MAX_PAGES) {
                context.messages().add(ValidationMessage.error(ItemEditorText.str("preview.validation.book_pages", WritableBookContent.MAX_PAGES)));
                return;
            }

            List<Filterable<String>> pages = new ArrayList<>();
            for (String page : context.state().book.pages) {
                if (page.length() > WritableBookContent.PAGE_EDIT_LENGTH) {
                    context.messages().add(ValidationMessage.error(ItemEditorText.str("preview.validation.book_page_length", WritableBookContent.PAGE_EDIT_LENGTH)));
                    continue;
                }
                pages.add(Filterable.passThrough(page));
            }
            context.previewStack().set(DataComponents.WRITABLE_BOOK_CONTENT, new WritableBookContent(pages));
            this.clearToPrototype(context.previewStack(), DataComponents.WRITTEN_BOOK_CONTENT);
        }
    }

    private boolean sameBook(ItemEditorState.BookData current, ItemEditorState.BookData baseline) {
        return current.writtenBook == baseline.writtenBook
                && Objects.equals(current.title, baseline.title)
                && Objects.equals(current.author, baseline.author)
                && Objects.equals(current.generation, baseline.generation)
                && current.pages.equals(baseline.pages);
    }
}
