package me.noramibu.itemeditor.ui.component;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.UIComponent;
import me.noramibu.itemeditor.ui.screen.ItemEditorScreen;
import me.noramibu.itemeditor.util.ItemEditorText;
import me.noramibu.itemeditor.util.TextColorPresets;
import me.noramibu.itemeditor.util.ValidationUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class RichTextToolbarUtil {
    private static final double FORCED_COMPACT_SCALE_THRESHOLD = 3.0d;
    private static final int COMPACT_BUTTON_MAX_WIDTH_SCALED = 172;
    private static final int REGULAR_BUTTON_MAX_WIDTH_SCALED = 260;
    private static final int COMPACT_BUTTON_ROW_HALF_MIN = 88;
    private static final int REGULAR_BUTTON_ROW_HALF_MIN = 112;
    private static final int TOOLBAR_CONTENT_WIDTH_MIN = 140;
    private static final int DEFAULT_SHADOW_COLOR = 0xFF000000;
    private static final String CLICK_CLOSE_TEMPLATE = "[ie:/click]";
    private static final String HOVER_CLOSE_TEMPLATE = "[ie:/hover]";
    private static final String TOKEN_PLACEHOLDER = "text";

    public static final List<ToolAction> BASIC_ACTIONS = List.of(
            formatAction("toolbar.short.bold", ChatFormatting.BOLD, RichTextAreaComponent::toggleBold, false),
            formatAction("toolbar.short.italic", ChatFormatting.ITALIC, RichTextAreaComponent::toggleItalic, false),
            formatAction("toolbar.short.underline", ChatFormatting.UNDERLINE, RichTextAreaComponent::toggleUnderline, false),
            formatAction("toolbar.short.strikethrough", ChatFormatting.STRIKETHROUGH, RichTextAreaComponent::toggleStrikethrough, false),
            textAction("toolbar.cap", RichTextAreaComponent::capitalizeSelectionOrAll, false),
            textAction("toolbar.low", RichTextAreaComponent::lowercaseSelectionOrAll, false),
            textAction("toolbar.reset", RichTextAreaComponent::clearFormatting, false),
            deferredAction("toolbar.head", RichTextToolbarUtil::openHeadTokenDialog),
            deferredAction("toolbar.sprite", RichTextToolbarUtil::openSpriteTokenDialog)
    );

    public static final List<ToolAction> EXTENDED_ACTIONS = List.of(
            formatAction("toolbar.short.bold", ChatFormatting.BOLD, RichTextAreaComponent::toggleBold, false),
            formatAction("toolbar.short.italic", ChatFormatting.ITALIC, RichTextAreaComponent::toggleItalic, false),
            formatAction("toolbar.short.underline", ChatFormatting.UNDERLINE, RichTextAreaComponent::toggleUnderline, false),
            formatAction("toolbar.short.strikethrough", ChatFormatting.STRIKETHROUGH, RichTextAreaComponent::toggleStrikethrough, false),
            textAction("toolbar.obf", RichTextAreaComponent::toggleObfuscated, false),
            textAction("toolbar.cap", RichTextAreaComponent::capitalizeSelectionOrAll, false),
            textAction("toolbar.low", RichTextAreaComponent::lowercaseSelectionOrAll, false),
            textAction("toolbar.reset", RichTextAreaComponent::clearFormatting, false),
            deferredAction("toolbar.head", RichTextToolbarUtil::openHeadTokenDialog),
            deferredAction("toolbar.sprite", RichTextToolbarUtil::openSpriteTokenDialog)
    );

    public static final List<ToolAction> BOOK_METADATA_ACTIONS = List.of(
            formatAction("toolbar.short.bold", ChatFormatting.BOLD, RichTextAreaComponent::toggleBold, false),
            formatAction("toolbar.short.italic", ChatFormatting.ITALIC, RichTextAreaComponent::toggleItalic, false),
            formatAction("toolbar.short.underline", ChatFormatting.UNDERLINE, RichTextAreaComponent::toggleUnderline, false),
            formatAction("toolbar.short.strikethrough", ChatFormatting.STRIKETHROUGH, RichTextAreaComponent::toggleStrikethrough, false),
            textAction("toolbar.obf", RichTextAreaComponent::toggleObfuscated, false),
            textAction("toolbar.cap", RichTextAreaComponent::capitalizeSelectionOrAll, false),
            textAction("toolbar.low", RichTextAreaComponent::lowercaseSelectionOrAll, false),
            textAction("toolbar.reset", RichTextAreaComponent::clearFormatting, false)
    );

    public static final List<ToolAction> BOOK_OUTPUT_ACTIONS = outputActions(true, false);

    public static final List<ToolAction> SIGN_OUTPUT_ACTIONS = outputActions(false, true);

    private static List<ToolAction> outputActions(boolean includeHoverModes, boolean includeSuggestCommand) {
        return List.of(
            formatAction("toolbar.short.bold", ChatFormatting.BOLD, RichTextAreaComponent::toggleBold, true),
            formatAction("toolbar.short.italic", ChatFormatting.ITALIC, RichTextAreaComponent::toggleItalic, true),
            formatAction("toolbar.short.underline", ChatFormatting.UNDERLINE, RichTextAreaComponent::toggleUnderline, true),
            formatAction("toolbar.short.strikethrough", ChatFormatting.STRIKETHROUGH, RichTextAreaComponent::toggleStrikethrough, true),
            textAction("toolbar.obf", RichTextAreaComponent::toggleObfuscated, true),
            textAction("toolbar.cap", RichTextAreaComponent::capitalizeSelectionOrAll, false),
            textAction("toolbar.low", RichTextAreaComponent::lowercaseSelectionOrAll, false),
            textAction("toolbar.reset", RichTextAreaComponent::clearFormatting, false),
            deferredAction("toolbar.event", (screen, editor) -> openEventTokenDialog(screen, editor, includeHoverModes, includeSuggestCommand)),
            deferredAction("toolbar.head", RichTextToolbarUtil::openHeadTokenDialog),
            deferredAction("toolbar.sprite", RichTextToolbarUtil::openSpriteTokenDialog)
        );
    }

    private RichTextToolbarUtil() {
    }

    private static ToolAction formatAction(
            String labelKey,
            ChatFormatting formatting,
            Consumer<RichTextAreaComponent> action,
            boolean requiresPreparation
    ) {
        return new ToolAction(styled(labelKey, formatting), Component.empty(), action, requiresPreparation);
    }

    private static ToolAction textAction(
            String labelKey,
            Consumer<RichTextAreaComponent> action,
            boolean requiresPreparation
    ) {
        return new ToolAction(ItemEditorText.tr(labelKey), Component.empty(), action, requiresPreparation);
    }

    private static ToolAction deferredAction(
            String labelKey,
            BiConsumer<ItemEditorScreen, RichTextAreaComponent> action
    ) {
        return new ToolAction(
                ItemEditorText.tr(labelKey),
                Component.empty(),
                action,
                false,
                true,
                ToolActionPlacement.AFTER_COLORS
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
            boolean compactToolbar,
            int toolbarWidthHint
    ) {
        FlowLayout tools = UiFactory.column();
        tools.gap(Math.max(1, UiFactory.scaleProfile().tightSpacing() - 1));
        AtomicInteger gradientEndColor = new AtomicInteger(TextColorPresets.gradientEndFor(selectedColor.get()));
        AtomicInteger selectedShadowColor = TextStylingController.initialShadowColor(editor, DEFAULT_SHADOW_COLOR);
        Runnable preparation = prepareStyledApply == null ? () -> {} : prepareStyledApply;
        List<ToolbarItem> toolbarItems = new ArrayList<>();
        int maxRowWidth = toolbarAvailableWidth(screen, compactToolbar, toolbarWidthHint);

        appendActionButtons(toolbarItems, actions, ToolActionPlacement.BEFORE_COLORS, screen, editor, preparation, compactToolbar, maxRowWidth);

        ButtonComponent pickColor = null;
        if (includeColorPicker) {
            pickColor = UiFactory.button(
                    TextColorPresets.colorLabel(selectedColor.get()), UiFactory.ButtonTextPreset.STANDARD, 
                    button -> screen.openColorPickerDialog(
                            colorDialogTitle,
                            selectedColor.get(),
                            color -> applySolidColor(editor, preparation, selectedColor, color, button)
                    )
            );
            if (!colorTooltip.isBlank()) {
                pickColor.tooltip(List.of(Component.literal(colorTooltip)));
            }
            toolbarItems.add(toolbarItem(pickColor, compactToolbar, maxRowWidth));
        }

        if (includeColorPicker) {
            ButtonComponent shadowColor = UiFactory.button(
                    shadowLabel(selectedShadowColor.get()),
                    UiFactory.ButtonTextPreset.STANDARD,
                    button -> screen.openColorPickerDialog(
                            ItemEditorText.str("toolbar.shadow"),
                            selectedShadowColor.get() & 0xFFFFFF,
                            color -> applyShadowColor(editor, preparation, selectedShadowColor, color | 0xFF000000, button)
                    )
            );
            toolbarItems.add(toolbarItem(shadowColor, compactToolbar, maxRowWidth));
        }

        if (includeGradient) {
            ButtonComponent finalPickColor = pickColor;
            ButtonComponent gradientButton = UiFactory.button(
                    TextColorPresets.gradientLabel(ItemEditorText.str("toolbar.gradient"), selectedColor.get(), gradientEndColor.get()), UiFactory.ButtonTextPreset.STANDARD, 
                    button -> screen.openGradientPickerDialog(gradientDialogTitle, selectedColor.get(), gradientEndColor.get(), (startColor, endColor) -> {
                        boolean hadSelection = editor.hasSelection();
                        preparation.run();
                        selectedColor.set(startColor);
                        gradientEndColor.set(endColor);
                        editor.applyGradientSelectionOrAll(startColor, endColor);
                        if (finalPickColor != null) {
                            finalPickColor.setMessage(TextColorPresets.colorLabel(startColor));
                        }
                        button.setMessage(TextColorPresets.gradientLabel(ItemEditorText.str("toolbar.gradient"), startColor, endColor));
                        editor.resumeEditing();
                        editor.collapseUnexpectedSelection(hadSelection);
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
                    button -> applySolidColor(editor, preparation, selectedColor, preset.rgb(), finalPickColor)
            );
            toolbarItems.add(toolbarItem(presetButton, compactToolbar, maxRowWidth));
        }

        appendActionButtons(toolbarItems, actions, ToolActionPlacement.AFTER_COLORS, screen, editor, preparation, compactToolbar, maxRowWidth);

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

    private static void applySolidColor(
            RichTextAreaComponent editor,
            Runnable preparation,
            AtomicInteger selectedColor,
            int color,
            ButtonComponent pickColorButton
    ) {
        boolean hadSelection = editor.hasSelection();
        preparation.run();
        selectedColor.set(color);
        editor.applyColor(color);
        if (pickColorButton != null) {
            pickColorButton.setMessage(TextColorPresets.colorLabel(color));
        }
        editor.resumeEditing();
        editor.collapseUnexpectedSelection(hadSelection);
    }

    private static void applyShadowColor(
            RichTextAreaComponent editor,
            Runnable preparation,
            AtomicInteger selectedShadowColor,
            int color,
            ButtonComponent button
    ) {
        boolean hadSelection = editor.hasSelection();
        preparation.run();
        selectedShadowColor.set(color);
        editor.applyShadowColor(color);
        button.setMessage(shadowLabel(color));
        editor.resumeEditing();
        editor.collapseUnexpectedSelection(hadSelection);
    }

    private static Component shadowLabel(int color) {
        return ItemEditorText.tr("toolbar.shadow_short", ValidationUtil.toHex(color)).copy().withColor(color & 0xFFFFFF);
    }

    private static int toolbarAvailableWidth(ItemEditorScreen screen, boolean compactToolbar, int toolbarWidthHint) {
        double guiScale = screen.session().minecraft().getWindow().getGuiScale();
        boolean forcedCompact = compactToolbar || guiScale >= FORCED_COMPACT_SCALE_THRESHOLD;
        int viewportFallback = (int) Math.round(screen.session().minecraft().getWindow().getGuiScaledWidth() * (forcedCompact ? 0.34d : 0.42d));
        int hintedWidth = toolbarWidthHint > 1
                ? toolbarWidthHint
                : Math.max(1, screen.editorContentWidthHint());
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

    private static void appendActionButtons(
            List<ToolbarItem> toolbarItems,
            List<ToolAction> actions,
            ToolActionPlacement placement,
            ItemEditorScreen screen,
            RichTextAreaComponent editor,
            Runnable preparation,
            boolean compactToolbar,
            int maxRowWidth
    ) {
        for (ToolAction action : actions) {
            if (action.placement() != placement) {
                continue;
            }
            ButtonComponent button = UiFactory.button(action.label(), UiFactory.ButtonTextPreset.STANDARD, component ->
                    applyToolbarAction(screen, editor, action, preparation)
            );
            if (!action.tooltip().getString().isBlank()) {
                button.tooltip(List.of(action.tooltip()));
            }
            toolbarItems.add(toolbarItem(button, compactToolbar, maxRowWidth));
        }
    }

    private static void applyToolbarAction(ItemEditorScreen screen, RichTextAreaComponent editor, ToolAction action, Runnable preparation) {
        boolean hadSelection = editor.hasSelection();
        if (action.requiresPreparation()) {
            preparation.run();
        }
        action.action().accept(screen, editor);
        if (action.deferredMutation()) {
            return;
        }
        editor.resumeEditing();
        editor.collapseUnexpectedSelection(hadSelection);
    }

    private static void openHeadTokenDialog(ItemEditorScreen screen, RichTextAreaComponent editor) {
        boolean hadSelection = editor.hasSelection();
        screen.openRichTextHeadDialog(ItemEditorText.str("dialog.rich_text.head.title"), token -> {
            editor.insertTemplate(token);
            editor.resumeEditing();
            editor.collapseUnexpectedSelection(hadSelection);
        });
    }

    private static void openSpriteTokenDialog(ItemEditorScreen screen, RichTextAreaComponent editor) {
        boolean hadSelection = editor.hasSelection();
        screen.openRichTextSpriteDialog(ItemEditorText.str("dialog.rich_text.sprite.title"), token -> {
            editor.insertTemplate(token);
            editor.resumeEditing();
            editor.collapseUnexpectedSelection(hadSelection);
        });
    }

    private static void openEventTokenDialog(
            ItemEditorScreen screen,
            RichTextAreaComponent editor,
            boolean includeHoverModes,
            boolean includeSuggestCommand
    ) {
        boolean hadSelection = editor.hasSelection();
        screen.openRichTextEventDialog(
                ItemEditorText.str("dialog.rich_text.event.title"),
                includeHoverModes,
                includeSuggestCommand,
                editor.selectedTextOr(TOKEN_PLACEHOLDER),
                token -> {
                    editor.insertTemplate(token);
                    editor.resumeEditing();
                    editor.collapseUnexpectedSelection(hadSelection);
                }
        );
    }

    private record ToolbarItem(UIComponent component, int layoutWidth) {
    }

    public record ToolAction(
            Component label,
            Component tooltip,
            BiConsumer<ItemEditorScreen, RichTextAreaComponent> action,
            boolean requiresPreparation,
            boolean deferredMutation,
            ToolActionPlacement placement
    ) {
        public ToolAction(
                Component label,
                Component tooltip,
                Consumer<RichTextAreaComponent> action,
                boolean requiresPreparation,
                ToolActionPlacement placement
        ) {
            this(label, tooltip, (screen, editor) -> action.accept(editor), requiresPreparation, false, placement);
        }

        public ToolAction(Component label, Component tooltip, Consumer<RichTextAreaComponent> action, boolean requiresPreparation) {
            this(label, tooltip, action, requiresPreparation, ToolActionPlacement.BEFORE_COLORS);
        }

    }

    public enum ToolActionPlacement {
        BEFORE_COLORS,
        AFTER_COLORS
    }
}
