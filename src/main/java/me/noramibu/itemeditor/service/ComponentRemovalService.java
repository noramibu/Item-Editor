package me.noramibu.itemeditor.service;

import me.noramibu.itemeditor.editor.ItemEditorState;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;

public final class ComponentRemovalService {
    private ComponentRemovalService() {}

    public static void read(ItemStack stack, ItemEditorState state) {
        stack.getComponentsPatch().entrySet().forEach(entry -> {
            if (entry.getValue().isEmpty()) state.removedComponents.add(entry.getKey());
        });
    }

    public static boolean hasDefault(ItemStack stack, DataComponentType<?> type) {
        return stack.getPrototype().get(type) != null;
    }

    public static void apply(ItemStack preview, ItemEditorState state, ItemEditorState baseline) {
        baseline.removedComponents.stream()
                .filter(type -> !state.removedComponents.contains(type) && !preview.has(type))
                .forEach(type -> restoreDefault(preview, type));
        state.removedComponents.forEach(preview::remove);
    }

    private static <T> void restoreDefault(ItemStack preview, DataComponentType<T> type) {
        preview.set(type, preview.getPrototype().get(type));
    }
}
