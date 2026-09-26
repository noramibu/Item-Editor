package me.noramibu.itemeditor.ui.component;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import java.util.function.Supplier;
import me.noramibu.itemeditor.ui.component.raw.CommandTextTools;
import me.noramibu.itemeditor.util.ItemEditorText;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class CommandEditorControls {
    private CommandEditorControls() {}

    public static FlowLayout create(RawTextAreaComponent editor) {
        ButtonComponent copy = copyOriginalButton(editor::commandValue);
        ButtonComponent breaks =
                UiFactory.button(lineBreaksLabel(editor), UiFactory.ButtonTextPreset.COMPACT, button -> {
                    editor.commandLineBreaks(!editor.commandLineBreaks());
                    button.setMessage(lineBreaksLabel(editor));
                });
        breaks.tooltip(UiFactory.tooltipLines(ItemEditorText.tr("command_editor.line_breaks_tooltip"), 260));
        return UiFactory.actionButtonRow(copy, breaks);
    }

    public static ButtonComponent copyOriginalButton(Supplier<String> source) {
        ButtonComponent copy = UiFactory.button(
                ItemEditorText.tr("raw_editor.string.copy_original"),
                UiFactory.ButtonTextPreset.COMPACT,
                button -> Minecraft.getInstance()
                        .keyboardHandler
                        .setClipboard(CommandTextTools.singleLine(source.get())));
        copy.tooltip(UiFactory.tooltipLines(ItemEditorText.tr("raw_editor.string.copy_original_hint"), 260));
        return copy;
    }

    private static Component lineBreaksLabel(RawTextAreaComponent editor) {
        return ItemEditorText.tr(
                "command_editor.line_breaks",
                ItemEditorText.str(editor.commandLineBreaks() ? "common.true" : "common.false"));
    }
}
