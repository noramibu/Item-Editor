package me.noramibu.itemeditor.ui.component;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Sizing;
import me.noramibu.itemeditor.ui.screen.ItemEditorScreen;
import me.noramibu.itemeditor.util.ItemEditorText;
import me.noramibu.itemeditor.util.TextColorPresets;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public final class RichTextToolbarUtil {

    public static final List<ToolAction> BASIC_ACTIONS = List.of(
            new ToolAction(styled("toolbar.short.bold", ChatFormatting.BOLD), Component.empty(), RichTextAreaComponent::toggleBold, false),
            new ToolAction(styled("toolbar.short.italic", ChatFormatting.ITALIC), Component.empty(), RichTextAreaComponent::toggleItalic, false),
            new ToolAction(styled("toolbar.short.underline", ChatFormatting.UNDERLINE), Component.empty(), RichTextAreaComponent::toggleUnderline, false),
            new ToolAction(styled("toolbar.short.strikethrough", ChatFormatting.STRIKETHROUGH), Component.empty(), RichTextAreaComponent::toggleStrikethrough, false),
            new ToolAction(ItemEditorText.tr("toolbar.cap"), Component.empty(), RichTextAreaComponent::capitalizeSelectionOrAll, false),
            new ToolAction(ItemEditorText.tr("toolbar.low"), Component.empty(), RichTextAreaComponent::lowercaseSelectionOrAll, false),
            new ToolAction(ItemEditorText.tr("toolbar.reset"), Component.empty(), RichTextAreaComponent::clearFormatting, false)
    );

    public static final List<ToolAction> EXTENDED_ACTIONS = List.of(
            new ToolAction(styled("toolbar.short.bold", ChatFormatting.BOLD), Component.empty(), RichTextAreaComponent::toggleBold, false),
            new ToolAction(styled("toolbar.short.italic", ChatFormatting.ITALIC), Component.empty(), RichTextAreaComponent::toggleItalic, false),
            new ToolAction(styled("toolbar.short.underline", ChatFormatting.UNDERLINE), Component.empty(), RichTextAreaComponent::toggleUnderline, false),
            new ToolAction(styled("toolbar.short.strikethrough", ChatFormatting.STRIKETHROUGH), Component.empty(), RichTextAreaComponent::toggleStrikethrough, false),
            new ToolAction(ItemEditorText.tr("toolbar.obf"), Component.empty(), RichTextAreaComponent::toggleObfuscated, false),
            new ToolAction(ItemEditorText.tr("toolbar.cap"), Component.empty(), RichTextAreaComponent::capitalizeSelectionOrAll, false),
            new ToolAction(ItemEditorText.tr("toolbar.low"), Component.empty(), RichTextAreaComponent::lowercaseSelectionOrAll, false),
            new ToolAction(ItemEditorText.tr("toolbar.reset"), Component.empty(), RichTextAreaComponent::clearFormatting, false)
    );

    public static final List<ToolAction> WRITTEN_OUTPUT_ACTIONS = List.of(
            new ToolAction(styled("toolbar.short.bold", ChatFormatting.BOLD), Component.empty(), RichTextAreaComponent::toggleBold, true),
            new ToolAction(styled("toolbar.short.italic", ChatFormatting.ITALIC), Component.empty(), RichTextAreaComponent::toggleItalic, true),
            new ToolAction(styled("toolbar.short.underline", ChatFormatting.UNDERLINE), Component.empty(), RichTextAreaComponent::toggleUnderline, true),
            new ToolAction(styled("toolbar.short.strikethrough", ChatFormatting.STRIKETHROUGH), Component.empty(), RichTextAreaComponent::toggleStrikethrough, true),
            new ToolAction(ItemEditorText.tr("toolbar.obf"), Component.empty(), RichTextAreaComponent::toggleObfuscated, true),
            new ToolAction(ItemEditorText.tr("toolbar.cap"), Component.empty(), RichTextAreaComponent::capitalizeSelectionOrAll, false),
            new ToolAction(ItemEditorText.tr("toolbar.low"), Component.empty(), RichTextAreaComponent::lowercaseSelectionOrAll, false),
            new ToolAction(ItemEditorText.tr("toolbar.reset"), Component.empty(), RichTextAreaComponent::clearFormatting, false)
    );

    private RichTextToolbarUtil() {
    }

    public static FlowLayout buildToolbar(
            ItemEditorScreen screen,
            RichTextAreaComponent editor,
            AtomicInteger selectedColor,
            List<ToolAction> actions,
            String colorDialogTitle,
            String gradientDialogTitle,
            String colorTooltip,
            String gradientTooltip,
            Runnable prepareStyledApply,
            boolean includeColorPicker,
            boolean includeGradient
    ) {
        FlowLayout tools = UiFactory.row();
        AtomicInteger gradientEndColor = new AtomicInteger(TextColorPresets.gradientEndFor(selectedColor.get()));
        Runnable preparation = prepareStyledApply == null ? () -> {} : prepareStyledApply;

        for (ToolAction action : actions) {
            ButtonComponent button = UiFactory.button(action.label(), component -> {
                if (action.requiresPreparation()) {
                    preparation.run();
                }
                action.action().accept(editor);
                editor.resumeEditing();
            });
            button.horizontalSizing(Sizing.content());
            if (!action.tooltip().getString().isBlank()) {
                button.tooltip(List.of(action.tooltip()));
            }
            tools.child(button);
        }

        ButtonComponent pickColor = null;
        if (includeColorPicker) {
            pickColor = UiFactory.button(
                    TextColorPresets.colorLabel(selectedColor.get()),
                    button -> screen.openColorPickerDialog(colorDialogTitle, selectedColor.get(), color -> {
                        preparation.run();
                        selectedColor.set(color);
                        editor.applyColor(color);
                        button.setMessage(TextColorPresets.colorLabel(color));
                        editor.resumeEditing();
                    })
            );
            pickColor.horizontalSizing(Sizing.content());
            if (!colorTooltip.isBlank()) {
                pickColor.tooltip(List.of(Component.literal(colorTooltip)));
            }
            tools.child(pickColor);
        }

        if (includeGradient) {
            ButtonComponent finalPickColor = pickColor;
            ButtonComponent gradientButton = UiFactory.button(
                    TextColorPresets.gradientLabel(ItemEditorText.str("toolbar.gradient"), selectedColor.get(), gradientEndColor.get()),
                    button -> screen.openGradientPickerDialog(gradientDialogTitle, selectedColor.get(), gradientEndColor.get(), (startColor, endColor) -> {
                        preparation.run();
                        selectedColor.set(startColor);
                        gradientEndColor.set(endColor);
                        editor.applyGradientSelectionOrAll(startColor, endColor);
                        if (finalPickColor != null) {
                            finalPickColor.setMessage(TextColorPresets.colorLabel(startColor));
                        }
                        button.setMessage(TextColorPresets.gradientLabel(ItemEditorText.str("toolbar.gradient"), startColor, endColor));
                        editor.resumeEditing();
                    })
            );
            gradientButton.horizontalSizing(Sizing.content());
            if (!gradientTooltip.isBlank()) {
                gradientButton.tooltip(List.of(Component.literal(gradientTooltip)));
            }
            tools.child(gradientButton);
        }

        ButtonComponent finalPickColor = pickColor;
        for (TextColorPresets.Preset preset : TextColorPresets.STANDARD) {
            ButtonComponent presetButton = UiFactory.button(
                    Component.literal(preset.label()).withColor(preset.rgb()),
                    button -> {
                        preparation.run();
                        selectedColor.set(preset.rgb());
                        editor.applyColor(preset.rgb());
                        if (finalPickColor != null) {
                            finalPickColor.setMessage(TextColorPresets.colorLabel(preset.rgb()));
                        }
                        editor.resumeEditing();
                    }
            );
            presetButton.horizontalSizing(Sizing.content());
            tools.child(presetButton);
        }

        return tools;
    }

    private static Component styled(String key, ChatFormatting formatting) {
        return ItemEditorText.tr(key).copy().withStyle(formatting);
    }

    public record ToolAction(Component label, Component tooltip, Consumer<RichTextAreaComponent> action, boolean requiresPreparation) {
    }
}
