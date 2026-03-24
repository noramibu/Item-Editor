package me.noramibu.itemeditor.service;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.BiPredicate;

abstract class PreviewServiceSupport {

    protected final <T> void clearToPrototype(ItemStack preview, DataComponentType<T> componentType) {
        preview.set(componentType, preview.getPrototype().get(componentType));
    }

    protected final <T> void restoreOriginalComponent(ItemStack originalStack, ItemStack preview, DataComponentType<T> componentType) {
        preview.set(componentType, originalStack.get(componentType));
    }

    protected final <T> boolean sameList(List<T> current, List<T> baseline, BiPredicate<T, T> matcher) {
        if (current.size() != baseline.size()) {
            return false;
        }

        for (int index = 0; index < current.size(); index++) {
            if (!matcher.test(current.get(index), baseline.get(index))) {
                return false;
            }
        }

        return true;
    }
}
