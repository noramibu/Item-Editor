package me.noramibu.itemeditor.ui.panel.specialdata;

import static me.noramibu.itemeditor.ui.component.CompactFieldLayout.compactField;

import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.UIComponent;
import java.util.function.Consumer;
import me.noramibu.itemeditor.ui.component.UiFactory;
import me.noramibu.itemeditor.ui.util.TriStateBooleanUi;
import net.minecraft.network.chat.Component;

final class SpecialDataFieldFactory {
    private SpecialDataFieldFactory() {}

    static FlowLayout compactTextField(
            SpecialDataPanelContext context, Component label, String value, Consumer<String> setter, int width) {
        return compactField(label, filledTextBox(context, value, setter), width + 40);
    }

    static UIComponent filledTextBox(SpecialDataPanelContext context, String value, Consumer<String> setter) {
        return UiFactory.textBox(value, context.bindText(setter)).horizontalSizing(Sizing.fill(100));
    }

    static FlowLayout compactTriStateBooleanPicker(
            SpecialDataPanelContext context, Component label, String value, Consumer<String> setter, int buttonWidth) {
        return TriStateBooleanUi.field(
                label, value, false, buttonWidth, next -> context.mutateRefresh(() -> setter.accept(next)));
    }

    static FlowLayout effectVisibilityField(
            SpecialDataPanelContext context, Component label, String value, String original, Consumer<String> setter) {
        return TriStateBooleanUi.field(
                label, value, !value.equals(original), -1, next -> context.mutateRefresh(() -> setter.accept(next)));
    }
}
