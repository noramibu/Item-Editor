package me.noramibu.itemeditor.ui.panel.specialdata;

import io.wispforest.owo.ui.core.UIComponent;
import java.util.function.BiConsumer;
import java.util.function.Function;
import me.noramibu.itemeditor.ui.component.UiFactory;
import me.noramibu.itemeditor.util.ItemEditorText;
import net.minecraft.network.chat.Component;

record EntityEditorField<T, V>(String section, String key, Function<T, V> read, BiConsumer<T, V> write)
        implements SpecialDataSearch.Field {

    static <T> UIComponent checkbox(EntityEditorField<T, Boolean> field, T draft, SpecialDataPanelContext context) {
        return UiFactory.checkbox(
                ItemEditorText.tr(field.key()),
                field.read().apply(draft),
                value -> context.mutateRefresh(() -> field.write().accept(draft, value)));
    }

    static <T> UIComponent text(EntityEditorField<T, String> field, T draft, SpecialDataPanelContext context) {
        return UiFactory.field(
                ItemEditorText.tr(field.key()),
                Component.empty(),
                UiFactory.textBox(field.read().apply(draft), context.bindText(value -> field.write()
                        .accept(draft, value))));
    }
}
