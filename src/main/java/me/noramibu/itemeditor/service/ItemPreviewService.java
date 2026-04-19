package me.noramibu.itemeditor.service;

import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.editor.ValidationMessage;
import me.noramibu.itemeditor.util.ItemEditorText;
import me.noramibu.itemeditor.util.RawItemDataUtil;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

public final class ItemPreviewService {
    private static final List<ItemPreviewApplier> APPLIERS = List.of(
            new GeneralPreviewApplier(),
            new DisplayPreviewApplier(),
            new AttributesPreviewApplier(),
            new EnchantmentsPreviewApplier(),
            new FlagsPreviewApplier(),
            new BookPreviewApplier()
    );

    private final SpecialDataPreviewService specialDataPreviewService = new SpecialDataPreviewService();

    public PreviewBuildResult buildPreview(
            ItemStack originalStack,
            ItemEditorState state,
            ItemEditorState baselineState,
            RegistryAccess registryAccess
    ) {
        if (state.rawEditorEdited) {
            RawItemDataUtil.ParseResult parsed = RawItemDataUtil.parse(state.rawEditorText, registryAccess);
            return this.buildRawPreviewFromParsed(originalStack, parsed, registryAccess);
        }

        ItemStack preview = (originalStack.is(Items.WRITABLE_BOOK) && state.book.writtenBook)
                ? originalStack.transmuteCopy(Items.WRITTEN_BOOK, originalStack.getCount())
                : originalStack.copy();
        List<ValidationMessage> messages = new ArrayList<>();

        ItemPreviewApplyContext context = new ItemPreviewApplyContext(
                originalStack,
                preview,
                state,
                baselineState,
                registryAccess,
                messages
        );

        for (ItemPreviewApplier applier : APPLIERS) {
            applier.apply(context);
        }

        this.specialDataPreviewService.applySpecialData(originalStack, preview, state, baselineState, registryAccess, messages);
        boolean unchangedFromOriginal = ItemStack.isSameItemSameComponents(preview, originalStack)
                && preview.getCount() == originalStack.getCount();
        if (!unchangedFromOriginal) {
            messages.addAll(RawItemDataUtil.validatePreviewStack(preview, registryAccess));
        }

        return new PreviewBuildResult(preview, messages);
    }

    public PreviewBuildResult buildRawPreviewFromParsed(
            ItemStack originalStack,
            RawItemDataUtil.ParseResult parsed,
            RegistryAccess registryAccess
    ) {
        List<ValidationMessage> messages = new ArrayList<>();

        if (!parsed.success()) {
            String message = parsed.hasPosition()
                    ? ItemEditorText.str("dialog.apply.parse.error_position", parsed.error(), parsed.line(), parsed.column())
                    : ItemEditorText.str("preview.validation.component_failed", parsed.error());
            messages.add(ValidationMessage.error(message));
            return new PreviewBuildResult(originalStack.copy(), messages);
        }

        ItemStack preview = parsed.stack().copy();
        List<ValidationMessage> validationMessages = RawItemDataUtil.validatePreviewStack(preview, registryAccess);
        messages.addAll(validationMessages);
        if (validationMessages.stream().anyMatch(message -> message.severity() == ValidationMessage.Severity.ERROR)) {
            return new PreviewBuildResult(originalStack.copy(), messages);
        }
        return new PreviewBuildResult(preview, messages);
    }

    public record PreviewBuildResult(ItemStack previewStack, List<ValidationMessage> messages) {
    }
}
