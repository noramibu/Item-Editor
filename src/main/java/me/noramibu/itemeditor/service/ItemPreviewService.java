package me.noramibu.itemeditor.service;

import com.mojang.serialization.DataResult;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.editor.ValidationMessage;
import me.noramibu.itemeditor.util.ItemEditorText;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
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
        ItemStack preview = this.createPreviewBaseStack(originalStack, state);
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

        DataResult<ItemStack> validationResult = ItemStack.validateStrict(preview);
        validationResult.resultOrPartial(problem -> messages.add(ValidationMessage.error(ItemEditorText.str("preview.validation.component_failed", problem))));
        this.validatePacketRoundTrip(preview, registryAccess, messages);

        return new PreviewBuildResult(preview, messages);
    }

    private ItemStack createPreviewBaseStack(ItemStack originalStack, ItemEditorState state) {
        if (originalStack.is(Items.WRITABLE_BOOK) && state.book.writtenBook) {
            return originalStack.transmuteCopy(Items.WRITTEN_BOOK, originalStack.getCount());
        }

        return originalStack.copy();
    }

    private void validatePacketRoundTrip(ItemStack preview, RegistryAccess registryAccess, List<ValidationMessage> messages) {
        ByteBuf rawBuffer = Unpooled.buffer();
        try {
            ItemStack.OPTIONAL_STREAM_CODEC.encode(new RegistryFriendlyByteBuf(rawBuffer, registryAccess), preview);
            ItemStack.OPTIONAL_STREAM_CODEC.decode(new RegistryFriendlyByteBuf(rawBuffer, registryAccess));
        } catch (RuntimeException exception) {
            String detail = exception.getMessage();
            if (detail == null || detail.isBlank()) {
                detail = exception.getClass().getSimpleName();
            }
            messages.add(ValidationMessage.error(ItemEditorText.str("preview.validation.packet_safe", detail)));
        } finally {
            rawBuffer.release();
        }
    }

    public record PreviewBuildResult(ItemStack previewStack, List<ValidationMessage> messages) {
    }
}
