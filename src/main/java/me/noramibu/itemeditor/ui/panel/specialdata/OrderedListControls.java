package me.noramibu.itemeditor.ui.panel.specialdata;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import me.noramibu.itemeditor.ui.component.UiFactory;
import me.noramibu.itemeditor.util.ItemEditorText;

import java.util.List;
import java.util.function.Function;

final class OrderedListControls {

    private OrderedListControls() {
    }

    static <T> FlowLayout actionRow(
            SpecialDataPanelContext context,
            List<T> entries,
            int index,
            Function<T, T> duplicateFactory
    ) {
        ButtonComponent up = UiFactory.button(ItemEditorText.tr("common.up"), UiFactory.ButtonTextPreset.COMPACT, button ->
                context.mutateRefresh(() -> context.swapEntries(entries, index, index - 1))
        );
        up.active(index > 0);
        ButtonComponent down = UiFactory.button(ItemEditorText.tr("common.down"), UiFactory.ButtonTextPreset.COMPACT, button ->
                context.mutateRefresh(() -> context.swapEntries(entries, index, index + 1))
        );
        down.active(index < entries.size() - 1);
        ButtonComponent duplicate = UiFactory.button(ItemEditorText.tr("common.duplicate"), UiFactory.ButtonTextPreset.COMPACT, button ->
                context.mutateRefresh(() -> entries.add(index + 1, duplicateFactory.apply(entries.get(index))))
        );
        ButtonComponent remove = UiFactory.negativeButton(ItemEditorText.tr("common.remove"), UiFactory.ButtonTextPreset.COMPACT, button ->
                context.mutateRefresh(() -> entries.remove(index))
        );
        return UiFactory.actionButtonRow(up, down, duplicate, remove);
    }
}
