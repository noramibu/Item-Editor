package me.noramibu.itemeditor.ui.panel.specialdata;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Sizing;
import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.ui.component.UiFactory;
import me.noramibu.itemeditor.ui.screen.ItemEditorScreen;
import me.noramibu.itemeditor.util.ItemEditorText;
import me.noramibu.itemeditor.util.ValidationUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public record SpecialDataPanelContext(ItemEditorScreen screen) {

    public ItemStack originalStack() {
        return this.screen.session().originalStack();
    }

    public ItemEditorState.SpecialData special() {
        return this.screen.session().state().special;
    }

    public void rebuildPreview() {
        this.screen.session().rebuildPreview();
    }

    public void mutate(Runnable mutation) {
        mutation.run();
        this.rebuildPreview();
    }

    public void mutateRefresh(Runnable mutation) {
        this.mutate(mutation);
        this.screen.refreshCurrentPanel();
    }

    public Consumer<String> bindText(Consumer<String> setter) {
        return value -> this.mutate(() -> setter.accept(value));
    }

    public Consumer<Boolean> bindToggle(Consumer<Boolean> setter) {
        return value -> this.mutate(() -> setter.accept(value));
    }

    public <T> void openDropdown(
            ButtonComponent anchor,
            List<T> values,
            Function<T, String> labelMapper,
            Consumer<T> selectionConsumer
    ) {
        this.screen.openDropdown(anchor, values, labelMapper, selectionConsumer);
    }

    public void openSearchablePicker(
            String title,
            String body,
            List<String> values,
            Function<String, String> labelMapper,
            Consumer<String> selectionConsumer
    ) {
        this.screen.openSearchablePickerDialog(title, body, values, labelMapper, selectionConsumer);
    }

    public FlowLayout createRemovableCard(Component title, Runnable removeAction) {
        return UiFactory.removableSubCard(title, () -> this.mutateRefresh(removeAction));
    }

    public FlowLayout createReorderableCard(
            Component title,
            boolean canMoveUp,
            Runnable moveUp,
            boolean canMoveDown,
            Runnable moveDown,
            Runnable remove
    ) {
        return UiFactory.reorderableSubCard(
                title,
                canMoveUp,
                moveUp == null ? null : () -> this.mutateRefresh(moveUp),
                canMoveDown,
                moveDown == null ? null : () -> this.mutateRefresh(moveDown),
                remove == null ? null : () -> this.mutateRefresh(remove)
        );
    }

    public FlowLayout colorInputWithPicker(
            String initialValue,
            Consumer<String> setter,
            Supplier<String> currentValueSupplier,
            String pickerTitle,
            int fallbackColor
    ) {
        FlowLayout row = UiFactory.row();
        row.child(UiFactory.textBox(initialValue, this.bindText(setter)).horizontalSizing(Sizing.fixed(140)));

        int selectedColor = this.parseHexColorOrDefault(currentValueSupplier.get(), fallbackColor);
        ButtonComponent pickButton = UiFactory.button(
                Component.literal(ItemEditorText.str("common.pick")).withColor(selectedColor),
                button -> this.screen.openColorPickerDialog(
                        pickerTitle,
                        this.parseHexColorOrDefault(currentValueSupplier.get(), fallbackColor),
                        color -> this.mutateRefresh(() -> setter.accept(ValidationUtil.toHex(color)))
                )
        );
        pickButton.tooltip(List.of(Component.literal(ValidationUtil.toHex(selectedColor)).withColor(selectedColor)));
        row.child(pickButton);
        return row;
    }

    private int parseHexColorOrDefault(String raw, int fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        Integer parsed = ValidationUtil.tryParseHexColor(raw);
        return parsed == null ? fallback : parsed;
    }
}
