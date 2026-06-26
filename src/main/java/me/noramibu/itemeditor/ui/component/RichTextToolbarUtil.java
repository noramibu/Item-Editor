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
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class RichTextToolbarUtil {
    private static final double FORCED_COMPACT_SCALE_THRESHOLD = 3.0d;
    private static final int TOOLBAR_BUTTON_MAX_WIDTH = 132;
    private static final int TOOLBAR_BUTTON_MIN_WIDTH = 30;
    private static final int TOOLBAR_BUTTON_HEIGHT = 18;
    private static final int TOOLBAR_BUTTON_CHROME_PADDING = 14;
    private static final int TOOLBAR_COMPACT_WIDTH_THRESHOLD = 420;
    private static final int TOOLBAR_COMPACT_BUTTON_MAX_WIDTH = 112;
    private static final int TOOLBAR_COMPACT_BUTTON_MIN_WIDTH = 26;
    private static final int TOOLBAR_COMPACT_BUTTON_HEIGHT = 16;
    private static final int TOOLBAR_COMPACT_BUTTON_CHROME_PADDING = 10;
    private static final int TOOLBAR_CONTENT_WIDTH_MIN = 140;
    private static final int DEFAULT_SHADOW_COLOR = 0xFF000000;
    private static final String TOKEN_PLACEHOLDER = "text";

    public static final List<ToolAction> BASIC_ACTIONS = List.of(
            deferredAction("toolbar.head", RichTextToolbarUtil::openHeadTokenDialog),
            deferredAction("toolbar.sprite", RichTextToolbarUtil::openSpriteTokenDialog),
            formatAction("toolbar.short.bold", ChatFormatting.BOLD, RichTextAreaComponent::toggleBold, false),
            formatAction("toolbar.short.italic", ChatFormatting.ITALIC, RichTextAreaComponent::toggleItalic, false),
            formatAction("toolbar.short.underline", ChatFormatting.UNDERLINE, RichTextAreaComponent::toggleUnderline, false),
            formatAction("toolbar.short.strikethrough", ChatFormatting.STRIKETHROUGH, RichTextAreaComponent::toggleStrikethrough, false),
            textAction("toolbar.obf", RichTextAreaComponent::toggleObfuscated, false),
            textAction("toolbar.cap", RichTextAreaComponent::capitalizeSelectionOrAll, false),
            textAction("toolbar.low", RichTextAreaComponent::lowercaseSelectionOrAll, false),
            textAction("toolbar.reset", RichTextAreaComponent::clearFormatting, false)
    );

    public static final List<ToolAction> EXTENDED_ACTIONS = List.of(
            deferredAction("toolbar.head", RichTextToolbarUtil::openHeadTokenDialog),
            deferredAction("toolbar.sprite", RichTextToolbarUtil::openSpriteTokenDialog),
            formatAction("toolbar.short.bold", ChatFormatting.BOLD, RichTextAreaComponent::toggleBold, false),
            formatAction("toolbar.short.italic", ChatFormatting.ITALIC, RichTextAreaComponent::toggleItalic, false),
            formatAction("toolbar.short.underline", ChatFormatting.UNDERLINE, RichTextAreaComponent::toggleUnderline, false),
            formatAction("toolbar.short.strikethrough", ChatFormatting.STRIKETHROUGH, RichTextAreaComponent::toggleStrikethrough, false),
            textAction("toolbar.obf", RichTextAreaComponent::toggleObfuscated, false),
            textAction("toolbar.cap", RichTextAreaComponent::capitalizeSelectionOrAll, false),
            textAction("toolbar.low", RichTextAreaComponent::lowercaseSelectionOrAll, false),
            textAction("toolbar.reset", RichTextAreaComponent::clearFormatting, false)
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
            deferredAction("toolbar.head", RichTextToolbarUtil::openHeadTokenDialog),
            deferredAction("toolbar.sprite", RichTextToolbarUtil::openSpriteTokenDialog),
            deferredAction("toolbar.event", (screen, editor) -> openEventTokenDialog(screen, editor, includeHoverModes, includeSuggestCommand)),
            formatAction("toolbar.short.bold", ChatFormatting.BOLD, RichTextAreaComponent::toggleBold, true),
            formatAction("toolbar.short.italic", ChatFormatting.ITALIC, RichTextAreaComponent::toggleItalic, true),
            formatAction("toolbar.short.underline", ChatFormatting.UNDERLINE, RichTextAreaComponent::toggleUnderline, true),
            formatAction("toolbar.short.strikethrough", ChatFormatting.STRIKETHROUGH, RichTextAreaComponent::toggleStrikethrough, true),
            textAction("toolbar.obf", RichTextAreaComponent::toggleObfuscated, true),
            textAction("toolbar.cap", RichTextAreaComponent::capitalizeSelectionOrAll, false),
            textAction("toolbar.low", RichTextAreaComponent::lowercaseSelectionOrAll, false),
            textAction("toolbar.reset", RichTextAreaComponent::clearFormatting, false)
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
        return new ToolAction(styled(labelKey, formatting), tooltipFor(labelKey), action, requiresPreparation);
    }

    private static ToolAction textAction(
            String labelKey,
            Consumer<RichTextAreaComponent> action,
            boolean requiresPreparation
    ) {
        return new ToolAction(ItemEditorText.tr(labelKey), tooltipFor(labelKey), action, requiresPreparation);
    }

    private static ToolAction deferredAction(
            String labelKey,
            BiConsumer<ItemEditorScreen, RichTextAreaComponent> action
    ) {
        return new ToolAction(
                ItemEditorText.tr(labelKey),
                tooltipFor(labelKey),
                action,
                false,
                true,
                ToolActionPlacement.BEFORE_COLORS
        );
    }

    public static Component tooltipFor(String labelKey) {
        if (labelKey == null || labelKey.isBlank()) {
            return Component.empty();
        }

        String suffix;
        if (labelKey.startsWith("toolbar.short.")) {
            suffix = labelKey.substring("toolbar.short.".length());
        } else if (labelKey.startsWith("toolbar.")) {
            suffix = labelKey.substring("toolbar.".length());
        } else {
            return Component.empty();
        }

        return suffix.isBlank() ? Component.empty() : ItemEditorText.tr("toolbar.tooltip." + suffix);
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
        AtomicReference<List<Integer>> gradientColors = new AtomicReference<>(
                TextColorPresets.normalizeGradientStops(List.of(selectedColor.get(), TextColorPresets.gradientEndFor(selectedColor.get())))
        );
        AtomicInteger selectedShadowColor = TextStylingController.initialShadowColor(editor, DEFAULT_SHADOW_COLOR);
        AtomicReference<UnifiedColorPickerDialog.ColorPickerResult> lastColorResult = new AtomicReference<>(
                new UnifiedColorPickerDialog.ColorPickerResult(
                        UnifiedColorPickerDialog.PaintMode.COLOR,
                        false,
                        List.of(selectedColor.get())
                )
        );
        Runnable preparation = prepareStyledApply == null ? () -> {} : prepareStyledApply;
        List<ToolbarItem> toolbarItems = new ArrayList<>();
        int maxRowWidth = toolbarAvailableWidth(screen, compactToolbar, toolbarWidthHint);

        ButtonComponent colorButton = null;
        if (includeColorPicker) {
            String unifiedColorDialogTitle = includeGradient && !gradientDialogTitle.isBlank()
                    ? gradientDialogTitle
                    : colorDialogTitle;
            colorButton = UiFactory.button(
                    toolbarColorLabel(selectedColor.get()),
                    UiFactory.ButtonTextPreset.STANDARD,
                    button -> {
                        UnifiedColorPickerDialog.ColorPickerResult initial = lastColorResult.get();
                        List<Integer> initialColors = initial.colors();
                        if (!initial.shadow() && initial.mode() == UnifiedColorPickerDialog.PaintMode.GRADIENT) {
                            initialColors = gradientColorsForSelectedStart(initialColors, selectedColor.get());
                        }
                        screen.openUnifiedColorPickerDialog(
                                unifiedColorDialogTitle,
                                new UnifiedColorPickerDialog.Options(
                                        initial.mode(),
                                        initial.shadow(),
                                        initialColors,
                                        true,
                                        includeGradient,
                                        true,
                                        true,
                                        includeGradient,
                                        true,
                                        editor.selectedTextOr("")
                                ),
                                result -> applyUnifiedColor(editor, preparation, selectedColor, gradientColors, selectedShadowColor, lastColorResult, result, button)
                        );
                    }
            );
            Component tooltip = tooltipFor("toolbar.color");
            if (!tooltip.getString().isBlank()) {
                colorButton.tooltip(List.of(tooltip));
            } else if (!colorTooltip.isBlank() || !gradientTooltip.isBlank()) {
                colorButton.tooltip(List.of(Component.literal(colorTooltip.isBlank() ? gradientTooltip : colorTooltip)));
            }
            toolbarItems.add(toolbarItem(colorButton, maxRowWidth));
        }
        ButtonComponent finalColorButton = colorButton;

        appendActionButtons(toolbarItems, actions, ToolActionPlacement.BEFORE_COLORS, screen, editor, preparation, maxRowWidth);

        for (TextColorPresets.Preset preset : TextColorPresets.STANDARD) {
            ButtonComponent presetButton = UiFactory.button(
                    standardColorLabel(preset), UiFactory.ButtonTextPreset.STANDARD,
                    button -> applySolidColor(editor, preparation, selectedColor, lastColorResult, preset.rgb(), finalColorButton)
            );
            presetButton.tooltip(List.of(Component.literal(preset.label() + " " + ValidationUtil.toHex(preset.rgb())).withColor(preset.rgb())));
            toolbarItems.add(toolbarItem(presetButton, maxRowWidth));
        }

        appendActionButtons(toolbarItems, actions, ToolActionPlacement.AFTER_COLORS, screen, editor, preparation, maxRowWidth);

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

    private static ToolbarItem toolbarItem(ButtonComponent button, int maxRowWidth) {
        UiFactory.applyButtonPreset(button, UiFactory.ButtonPreset.TINY);
        button.verticalSizing(Sizing.fixed(toolbarButtonHeight(maxRowWidth)));
        int spacing = Math.max(1, UiFactory.scaleProfile().tightSpacing() - 1);
        int minWidth = toolbarButtonMinWidth(maxRowWidth);
        int maxButtonWidth = Math.max(minWidth, maxRowWidth - spacing);
        int buttonWidth = Math.clamp(toolbarButtonWidth(button, maxRowWidth), minWidth, maxButtonWidth);
        fitToolbarButtonLabel(button, buttonWidth, maxRowWidth);
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
            AtomicReference<UnifiedColorPickerDialog.ColorPickerResult> lastColorResult,
            int color,
            ButtonComponent pickColorButton
    ) {
        boolean hadSelection = editor.hasSelection();
        preparation.run();
        selectedColor.set(color);
        lastColorResult.set(new UnifiedColorPickerDialog.ColorPickerResult(
                UnifiedColorPickerDialog.PaintMode.COLOR,
                false,
                List.of(color)
        ));
        editor.applyColor(color);
        if (pickColorButton != null) {
            pickColorButton.setMessage(toolbarColorLabel(color));
        }
        editor.resumeEditing();
        editor.collapseUnexpectedSelection(hadSelection);
    }

    private static void applyUnifiedColor(
            RichTextAreaComponent editor,
            Runnable preparation,
            AtomicInteger selectedColor,
            AtomicReference<List<Integer>> gradientColors,
            AtomicInteger selectedShadowColor,
            AtomicReference<UnifiedColorPickerDialog.ColorPickerResult> lastColorResult,
            UnifiedColorPickerDialog.ColorPickerResult result,
            ButtonComponent button
    ) {
        boolean hadSelection = editor.hasSelection();
        preparation.run();
        List<Integer> colors = result.mode() == UnifiedColorPickerDialog.PaintMode.GRADIENT
                ? TextColorPresets.normalizeGradientStops(result.colors())
                : result.colors();
        lastColorResult.set(new UnifiedColorPickerDialog.ColorPickerResult(result.mode(), result.shadow(), colors));
        if (result.shadow()) {
            if (result.mode() == UnifiedColorPickerDialog.PaintMode.GRADIENT) {
                selectedShadowColor.set(colors.getFirst() | 0xFF000000);
                editor.applyShadowGradientSelectionOrAll(colors);
                button.setMessage(toolbarShadowGradientLabel(colors));
            } else {
                int color = colors.getFirst() | 0xFF000000;
                selectedShadowColor.set(color);
                editor.applyShadowColor(color);
                button.setMessage(toolbarShadowLabel(color));
            }
        } else if (result.mode() == UnifiedColorPickerDialog.PaintMode.GRADIENT) {
            selectedColor.set(colors.getFirst());
            gradientColors.set(colors);
            editor.applyGradientSelectionOrAll(colors);
            button.setMessage(TextColorPresets.gradientLabel(ItemEditorText.str("toolbar.color"), colors));
        } else {
            int color = colors.getFirst();
            selectedColor.set(color);
            editor.applyColor(color);
            button.setMessage(toolbarColorLabel(color));
        }
        editor.resumeEditing();
        editor.collapseUnexpectedSelection(hadSelection);
    }

    private static Component toolbarColorLabel(int color) {
        return ItemEditorText.tr("toolbar.color").copy().withColor(color & 0xFFFFFF);
    }

    private static Component toolbarShadowLabel(int color) {
        return ItemEditorText.tr("toolbar.color")
                .copy()
                .withColor(0xFFFFFF)
                .withStyle(style -> style.withShadowColor(color | 0xFF000000));
    }

    private static Component toolbarShadowGradientLabel(List<Integer> colors) {
        return Component.literal(ItemEditorText.str("toolbar.color"))
                .withColor(0xFFFFFF)
                .withStyle(style -> style.withShadowColor((TextColorPresets.normalizeGradientStops(colors).getFirst() & 0xFFFFFF) | 0xFF000000));
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
        return Math.clamp(available, Math.min(preferredMin, contentWidth), contentWidth);
    }

    private static int toolbarButtonWidth(ButtonComponent button, int maxRowWidth) {
        String text = button.getMessage().getString();
        int textWidth = Minecraft.getInstance().font.width(text);
        int chromePadding = toolbarButtonChromePadding(maxRowWidth);
        int minWidth = toolbarButtonMinWidth(maxRowWidth);
        int maxWidth = Math.clamp(maxRowWidth, minWidth, Math.max(minWidth, toolbarButtonMaxWidth(maxRowWidth)));
        int fitted = Math.clamp(textWidth + chromePadding, minWidth, maxWidth);
        int current = button.width();
        if (current > 0) {
            fitted = Math.clamp(current, fitted, maxWidth);
        }
        return fitted;
    }

    private static void fitToolbarButtonLabel(ButtonComponent button, int buttonWidth, int maxRowWidth) {
        Component message = button.getMessage();
        int chromePadding = toolbarButtonChromePadding(maxRowWidth);
        int textBudget = Math.max(10, buttonWidth - chromePadding);
        Component fitted = UiFactory.fitToWidth(message, textBudget);
        button.setMessage(fitted);
        if (!fitted.getString().equals(message.getString())) {
            button.tooltip(List.of(message));
        }
    }

    private static int toolbarButtonHeight(int maxRowWidth) {
        if (useExtraCompactToolbarButtons(maxRowWidth)) {
            return Math.max(14, UiFactory.scaledPixels(TOOLBAR_COMPACT_BUTTON_HEIGHT));
        }
        return Math.max(UiFactory.scaleProfile().controlHeight(), UiFactory.scaledPixels(TOOLBAR_BUTTON_HEIGHT));
    }

    private static int toolbarButtonChromePadding(int maxRowWidth) {
        int base = useExtraCompactToolbarButtons(maxRowWidth)
                ? TOOLBAR_COMPACT_BUTTON_CHROME_PADDING
                : TOOLBAR_BUTTON_CHROME_PADDING;
        return Math.max(6, UiFactory.scaledPixels(base));
    }

    private static int toolbarButtonMinWidth(int maxRowWidth) {
        int base = useExtraCompactToolbarButtons(maxRowWidth)
                ? TOOLBAR_COMPACT_BUTTON_MIN_WIDTH
                : TOOLBAR_BUTTON_MIN_WIDTH;
        return UiFactory.scaledPixels(base);
    }

    private static int toolbarButtonMaxWidth(int maxRowWidth) {
        int base = useExtraCompactToolbarButtons(maxRowWidth)
                ? TOOLBAR_COMPACT_BUTTON_MAX_WIDTH
                : TOOLBAR_BUTTON_MAX_WIDTH;
        return UiFactory.scaledPixels(base);
    }

    private static boolean useExtraCompactToolbarButtons(int maxRowWidth) {
        return maxRowWidth < UiFactory.scaledPixels(TOOLBAR_COMPACT_WIDTH_THRESHOLD);
    }

    private static Component standardColorLabel(TextColorPresets.Preset preset) {
        return Component.literal(preset.label()).withColor(preset.rgb());
    }

    private static List<Integer> gradientColorsForSelectedStart(List<Integer> colors, int selectedColor) {
        List<Integer> normalized = new ArrayList<>(TextColorPresets.normalizeGradientStops(colors));
        normalized.set(0, selectedColor & 0xFFFFFF);
        return TextColorPresets.normalizeGradientStops(normalized);
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
            toolbarItems.add(toolbarItem(button, maxRowWidth));
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
