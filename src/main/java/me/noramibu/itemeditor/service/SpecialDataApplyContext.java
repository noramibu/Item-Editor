package me.noramibu.itemeditor.service;

import java.util.List;
import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.editor.ValidationMessage;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;

record SpecialDataApplyContext(
        ItemStack originalStack,
        ItemStack previewStack,
        ItemEditorState state,
        ItemEditorState baselineState,
        RegistryAccess registryAccess,
        List<ValidationMessage> messages) {
    BlockEntityType<?> resolveBlockEntityType(BlockEntityType<?> expected, boolean matchingItem) {
        var preview = previewStack.get(DataComponents.BLOCK_ENTITY_DATA);
        var original = originalStack.get(DataComponents.BLOCK_ENTITY_DATA);
        return matchingItem
                        || preview != null && preview.type() == expected
                        || original != null && original.type() == expected
                ? expected
                : null;
    }

    ItemEditorState.SpecialData special() {
        return this.state.special;
    }

    ItemEditorState.SpecialData baselineSpecial() {
        return this.baselineState.special;
    }
}
