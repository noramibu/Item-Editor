package me.noramibu.itemeditor.service;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.editor.ValidationMessage;
import me.noramibu.itemeditor.util.ItemEditorText;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public final class ItemPreviewService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ItemPreviewService.class);

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
            if (this.shouldFallbackToDataValidation(exception)) {
                LOGGER.debug("[ItemEditor] Packet round-trip validation fell back to data-codec validation due to runtime exception", exception);
                this.validateDataRoundTrip(preview, registryAccess, messages);
                return;
            }
            String detail = exception.getMessage();
            if (detail == null || detail.isBlank()) {
                detail = exception.getClass().getSimpleName();
            }
            messages.add(ValidationMessage.error(ItemEditorText.str("preview.validation.packet_safe", detail)));
        } finally {
            rawBuffer.release();
        }
    }

    private boolean shouldFallbackToDataValidation(Throwable throwable) {
        return this.isContextDependentPacketException(throwable)
                || this.hasThirdPartyFrame(throwable)
                || this.isBlindNullPointer(throwable);
    }

    private boolean isBlindNullPointer(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof NullPointerException && (current.getMessage() == null || current.getMessage().isBlank())) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private void validateDataRoundTrip(ItemStack preview, RegistryAccess registryAccess, List<ValidationMessage> messages) {
        DynamicOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, registryAccess);
        DataResult<Tag> encoded = ItemStack.OPTIONAL_CODEC.encodeStart(ops, preview);
        Tag tag = encoded.resultOrPartial(problem -> messages.add(ValidationMessage.error(ItemEditorText.str("preview.validation.component_failed", problem))))
                .orElse(null);
        if (tag == null) {
            return;
        }

        ItemStack.OPTIONAL_CODEC.parse(ops, tag)
                .resultOrPartial(problem -> messages.add(ValidationMessage.error(ItemEditorText.str("preview.validation.component_failed", problem))));
    }

    private boolean isContextDependentPacketException(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            String message = current.getMessage();
            if (message != null) {
                String normalized = message.toLowerCase();
                if ((normalized.contains("packetcontext") && normalized.contains("null"))
                        || normalized.contains("orElseThrow(\"net.fabricmc.fabric.api.networking.v1.context.packetcontext")) {
                    return true;
                }
            }
            current = current.getCause();
        }

        return false;
    }

    private boolean hasThirdPartyFrame(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            for (StackTraceElement element : current.getStackTrace()) {
                if (this.isThirdPartyFrame(element.getClassName())) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }

    private boolean isThirdPartyFrame(String className) {
        return !(className.startsWith("java.")
                || className.startsWith("javax.")
                || className.startsWith("jdk.")
                || className.startsWith("sun.")
                || className.startsWith("com.sun.")
                || className.startsWith("io.netty.")
                || className.startsWith("com.mojang.")
                || className.startsWith("net.minecraft.")
                || className.startsWith("me.noramibu.itemeditor."));
    }

    public record PreviewBuildResult(ItemStack previewStack, List<ValidationMessage> messages) {
    }
}
