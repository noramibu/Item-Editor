package me.noramibu.itemeditor.ui.panel;

import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.UIComponent;
import me.noramibu.itemeditor.ui.component.UiFactory;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

final class SectionedEditorPanel {

    private SectionedEditorPanel() {
    }

    static UIComponent build(List<Section> sections) {
        return build(sections, Component.empty(), 0);
    }

    static UIComponent build(List<Section> sections, Component emptyMessage, int emptyMessageWidth) {
        FlowLayout root = UiFactory.column();
        boolean hasAnySection = false;
        for (Section section : sections) {
            if (!section.supported().getAsBoolean()) {
                continue;
            }
            UiFactory.appendFillChild(root, section.builder().get());
            hasAnySection = true;
        }

        if (!hasAnySection && !emptyMessage.getString().isEmpty()) {
            UiFactory.appendFillChild(root, UiFactory.muted(emptyMessage, Math.max(1, emptyMessageWidth)));
        }
        return root;
    }

    record Section(BooleanSupplier supported, Supplier<UIComponent> builder) {
        static Section always(Supplier<UIComponent> builder) {
            return new Section(() -> true, builder);
        }
    }
}
