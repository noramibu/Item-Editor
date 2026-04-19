package me.noramibu.itemeditor.ui.component;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.UIComponent;
import me.noramibu.itemeditor.ui.screen.ItemEditorScreen;
import me.noramibu.itemeditor.util.ItemEditorText;
import me.noramibu.itemeditor.util.TextColorPresets;
import net.minecraft.client.Minecraft;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public final class RichTextToolbarUtil {
    private static final double FORCED_COMPACT_SCALE_THRESHOLD = 3.0d;
    private static final int COMPACT_BUTTON_MAX_WIDTH_SCALED = 172;
    private static final int REGULAR_BUTTON_MAX_WIDTH_SCALED = 260;
    private static final int COMPACT_BUTTON_ROW_HALF_MIN = 88;
    private static final int REGULAR_BUTTON_ROW_HALF_MIN = 112;
    private static final int TOOLBAR_CONTENT_WIDTH_MIN = 140;

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
        return buildToolbar(
                screen,
                editor,
                selectedColor,
                actions,
                colorDialogTitle,
                gradientDialogTitle,
                colorTooltip,
                gradientTooltip,
                prepareStyledApply,
                includeColorPicker,
                includeGradient,
                false
        );
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
            boolean includeGradient,
            boolean compactToolbar
    ) {
        FlowLayout tools = UiFactory.column();
        tools.gap(Math.max(1, UiFactory.scaleProfile().tightSpacing() - 1));
        AtomicInteger gradientEndColor = new AtomicInteger(TextColorPresets.gradientEndFor(selectedColor.get()));
        Runnable preparation = prepareStyledApply == null ? () -> {} : prepareStyledApply;
        List<ToolbarItem> toolbarItems = new ArrayList<>();
        int maxRowWidth = toolbarAvailableWidth(screen, compactToolbar);

        for (ToolAction action : actions) {
            ButtonComponent button = UiFactory.button(action.label(), UiFactory.ButtonTextPreset.STANDARD,  component -> {
                if (action.requiresPreparation()) {
                    preparation.run();
                }
                action.action().accept(editor);
                editor.resumeEditing();
            });
            if (!action.tooltip().getString().isBlank()) {
                button.tooltip(List.of(action.tooltip()));
            }
            toolbarItems.add(toolbarItem(button, compactToolbar, maxRowWidth));
        }

        ButtonComponent pickColor = null;
        if (includeColorPicker) {
            pickColor = UiFactory.button(
                    TextColorPresets.colorLabel(selectedColor.get()), UiFactory.ButtonTextPreset.STANDARD, 
                    button -> screen.openColorPickerDialog(colorDialogTitle, selectedColor.get(), color -> {
                        preparation.run();
                        selectedColor.set(color);
                        editor.applyColor(color);
                        button.setMessage(TextColorPresets.colorLabel(color));
                        editor.resumeEditing();
                    })
            );
            if (!colorTooltip.isBlank()) {
                pickColor.tooltip(List.of(Component.literal(colorTooltip)));
            }
            toolbarItems.add(toolbarItem(pickColor, compactToolbar, maxRowWidth));
        }

        if (includeGradient) {
            ButtonComponent finalPickColor = pickColor;
            ButtonComponent gradientButton = UiFactory.button(
                    TextColorPresets.gradientLabel(ItemEditorText.str("toolbar.gradient"), selectedColor.get(), gradientEndColor.get()), UiFactory.ButtonTextPreset.STANDARD, 
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
            if (!gradientTooltip.isBlank()) {
                gradientButton.tooltip(List.of(Component.literal(gradientTooltip)));
            }
            toolbarItems.add(toolbarItem(gradientButton, compactToolbar, maxRowWidth));
        }

        ButtonComponent finalPickColor = pickColor;
        for (TextColorPresets.Preset preset : TextColorPresets.STANDARD) {
            ButtonComponent presetButton = UiFactory.button(
                    Component.literal(preset.label()).withColor(preset.rgb()), UiFactory.ButtonTextPreset.STANDARD, 
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
            toolbarItems.add(toolbarItem(presetButton, compactToolbar, maxRowWidth));
        }

        appendWrappedRows(tools, toolbarItems, maxRowWidth);
        return tools;
    }

    private static void appendWrappedRows(FlowLayout root, List<ToolbarItem> items, int maxRowWidth) {
        if (items.isEmpty()) {
            return;
        }
        FlowLayout row = compactToolbarRow();
        int rowWidth = 0;
        for (ToolbarItem entry : items) {
            int itemWidth = entry.layoutWidth();
            if (rowWidth > 0 && rowWidth + itemWidth > maxRowWidth) {
                root.child(row);
                row = compactToolbarRow();
                rowWidth = 0;
            }
            row.child(entry.component());
            rowWidth += itemWidth;
        }
        if (!row.children().isEmpty()) {
            root.child(row);
        }
    }

    private static ToolbarItem toolbarItem(ButtonComponent button, boolean compactToolbar, int maxRowWidth) {
        UiFactory.applyButtonPreset(button, compactToolbar ? UiFactory.ButtonPreset.COMPACT : UiFactory.ButtonPreset.STANDARD);
        int spacing = Math.max(1, UiFactory.scaleProfile().tightSpacing() - 1);
        int minWidth = UiFactory.scaledPixels(compactToolbar ? 30 : 42);
        int maxButtonWidth = Math.max(minWidth, maxRowWidth - spacing);
        int buttonWidth = Math.clamp(toolbarButtonWidth(button, compactToolbar, maxRowWidth), minWidth, maxButtonWidth);
        fitToolbarButtonLabel(button, buttonWidth, compactToolbar);
        button.horizontalSizing(Sizing.fixed(buttonWidth));
        return new ToolbarItem(button, buttonWidth + spacing);
    }

    private static FlowLayout compactToolbarRow() {
        FlowLayout row = UiFactory.row();
        row.gap(Math.max(1, UiFactory.scaleProfile().tightSpacing() - 1));
        return row;
    }

    private static int toolbarAvailableWidth(ItemEditorScreen screen, boolean compactToolbar) {
        double guiScale = screen.session().minecraft().getWindow().getGuiScale();
        boolean forcedCompact = compactToolbar || guiScale >= FORCED_COMPACT_SCALE_THRESHOLD;
        int viewportFallback = (int) Math.round(screen.session().minecraft().getWindow().getGuiScaledWidth() * (forcedCompact ? 0.34d : 0.42d));
        int hintedWidth = Math.max(1, screen.editorContentWidthHint());
        int fallbackWidth = Math.max(TOOLBAR_CONTENT_WIDTH_MIN, viewportFallback);
        int contentWidth = hintedWidth > 1 ? hintedWidth : fallbackWidth;
        int sideInsets = UiFactory.scaledPixels(forcedCompact ? 10 : 14);
        int safety = UiFactory.scaledPixels(forcedCompact ? 12 : 18);
        int preferredMin = forcedCompact ? 160 : 200;
        int available = Math.max(1, contentWidth - sideInsets - safety);
        return Math.min(contentWidth, Math.max(preferredMin, available));
    }

    private static int toolbarButtonWidth(ButtonComponent button, boolean compactToolbar, int maxRowWidth) {
        String text = button.getMessage().getString();
        int textWidth = Minecraft.getInstance().font.width(text);
        int chromePadding = UiFactory.scaledPixels(compactToolbar ? 18 : 22);
        int minWidth = UiFactory.scaledPixels(compactToolbar ? 30 : 42);
        int maxWidth = compactToolbar
                ? Math.max(minWidth, Math.min(UiFactory.scaledPixels(COMPACT_BUTTON_MAX_WIDTH_SCALED), Math.max(COMPACT_BUTTON_ROW_HALF_MIN, maxRowWidth / 2)))
                : Math.max(minWidth, Math.min(UiFactory.scaledPixels(REGULAR_BUTTON_MAX_WIDTH_SCALED), Math.max(REGULAR_BUTTON_ROW_HALF_MIN, maxRowWidth / 2)));
        int fitted = Math.max(minWidth, Math.min(maxWidth, textWidth + chromePadding));
        int current = button.width();
        if (current > 0) {
            fitted = Math.max(fitted, Math.min(maxWidth, current));
        }
        return fitted;
    }

    private static void fitToolbarButtonLabel(ButtonComponent button, int buttonWidth, boolean compactToolbar) {
        Component message = button.getMessage();
        int chromePadding = UiFactory.scaledPixels(compactToolbar ? 18 : 22);
        int textBudget = Math.max(10, buttonWidth - chromePadding);
        Component fitted = UiFactory.fitToWidth(message, textBudget);
        button.setMessage(fitted);
        if (!fitted.getString().equals(message.getString())) {
            button.tooltip(List.of(message));
        }
    }

    private static Component styled(String key, ChatFormatting formatting) {
        return ItemEditorText.tr(key).copy().withStyle(formatting);
    }

    private record ToolbarItem(UIComponent component, int layoutWidth) {
    }

    public record ToolAction(Component label, Component tooltip, Consumer<RichTextAreaComponent> action, boolean requiresPreparation) {
    }
}
