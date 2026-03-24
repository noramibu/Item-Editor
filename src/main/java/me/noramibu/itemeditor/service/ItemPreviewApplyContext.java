package me.noramibu.itemeditor.service;

import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.editor.ValidationMessage;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.ItemStack;

import java.util.List;

record ItemPreviewApplyContext(
        ItemStack originalStack,
        ItemStack previewStack,
        ItemEditorState state,
        ItemEditorState baselineState,
        RegistryAccess registryAccess,
        List<ValidationMessage> messages
) {
}
