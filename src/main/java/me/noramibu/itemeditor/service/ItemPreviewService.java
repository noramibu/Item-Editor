package me.noramibu.itemeditor.service;

import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.editor.ValidationMessage;
import me.noramibu.itemeditor.util.ItemEditorCapabilities;
import me.noramibu.itemeditor.util.ItemEditorText;
import me.noramibu.itemeditor.util.RawItemDataUtil;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.Item;
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
        if (originalStack.is(Items.ARROW)
                && (hasText(state.special.potionId)
                || hasText(state.special.potionCustomColor)
                || hasText(state.special.potionCustomName)
                || state.special.potionEffects.stream().anyMatch(effect -> hasText(effect.effectId)))) {
            preview = preview.transmuteCopy(Items.TIPPED_ARROW, preview.getCount());
        }
        Item commandBlockItem = commandBlockItem(state.special.commandBlockItemId);
        if (commandBlockItem != null && ItemEditorCapabilities.supportsCommandBlockData(originalStack)) {
            preview = preview.transmuteCopy(commandBlockItem, preview.getCount());
        }
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

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static Item commandBlockItem(String itemId) {
        return switch (itemId == null ? "" : itemId) {
            case "minecraft:command_block" -> Items.COMMAND_BLOCK;
            case "minecraft:chain_command_block" -> Items.CHAIN_COMMAND_BLOCK;
            case "minecraft:repeating_command_block" -> Items.REPEATING_COMMAND_BLOCK;
            default -> null;
        };
    }

    public record PreviewBuildResult(ItemStack previewStack, List<ValidationMessage> messages) {
    }
}
