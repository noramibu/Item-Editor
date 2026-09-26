package me.noramibu.itemeditor.ui.component;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.UIComponent;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import me.noramibu.itemeditor.editor.text.RichTextStyle;
import me.noramibu.itemeditor.ui.screen.ItemEditorScreen;
import me.noramibu.itemeditor.util.ItemEditorText;
import me.noramibu.itemeditor.util.TextColorPresets;
import me.noramibu.itemeditor.util.ValidationUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;

public final class RichTextToolbarUtil {
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
    private static final int BASIC_RICH_CONTENT_ACTION_COUNT = 4;
    private static final String TOKEN_PLACEHOLDER = "text";

    public static final List<ToolAction> BASIC_ACTIONS = List.of(
            deferredAction("toolbar.head", RichTextToolbarUtil::openHeadTokenDialog),
            deferredAction("toolbar.sprite", RichTextToolbarUtil::openSpriteTokenDialog),
            deferredAction("toolbar.translation", RichTextToolbarUtil::openTranslationTokenDialog),
            deferredAction("toolbar.font", RichTextToolbarUtil::openFontDialog),
            formatAction("toolbar.short.bold", ChatFormatting.BOLD, RichTextAreaComponent::toggleBold, false),
            formatAction("toolbar.short.italic", ChatFormatting.ITALIC, RichTextAreaComponent::toggleItalic, false),
            formatAction(
                    "toolbar.short.underline", ChatFormatting.UNDERLINE, RichTextAreaComponent::toggleUnderline, false),
            formatAction(
                    "toolbar.short.strikethrough",
                    ChatFormatting.STRIKETHROUGH,
                    RichTextAreaComponent::toggleStrikethrough,
                    false),
            textAction("toolbar.obf", RichTextAreaComponent::toggleObfuscated, false),
            textAction("toolbar.cap", RichTextAreaComponent::capitalizeSelectionOrAll, false),
            textAction("toolbar.low", RichTextAreaComponent::lowercaseSelectionOrAll, false),
            textAction("common.reset", RichTextAreaComponent::clearFormatting, false));

    public static final List<ToolAction> EXTENDED_ACTIONS = BASIC_ACTIONS;

    public static final List<ToolAction> BOOK_METADATA_ACTIONS =
            BASIC_ACTIONS.subList(BASIC_RICH_CONTENT_ACTION_COUNT, BASIC_ACTIONS.size());

    public static final List<ToolAction> BOOK_OUTPUT_ACTIONS = outputActions(true, false);

    public static final List<ToolAction> SIGN_OUTPUT_ACTIONS = outputActions(false, true);

    private static List<ToolAction> outputActions(boolean includeHoverModes, boolean includeSuggestCommand) {
        return List.of(
                deferredAction("toolbar.head", RichTextToolbarUtil::openHeadTokenDialog),
                deferredAction("toolbar.sprite", RichTextToolbarUtil::openSpriteTokenDialog),
                deferredAction("toolbar.translation", RichTextToolbarUtil::openTranslationTokenDialog),
                deferredAction("toolbar.font", RichTextToolbarUtil::openFontDialog),
                deferredAction(
                        "toolbar.event",
                        (screen, editor) ->
                                openEventTokenDialog(screen, editor, includeHoverModes, includeSuggestCommand)),
                formatAction("toolbar.short.bold", ChatFormatting.BOLD, RichTextAreaComponent::toggleBold, true),
                formatAction("toolbar.short.italic", ChatFormatting.ITALIC, RichTextAreaComponent::toggleItalic, true),
                formatAction(
                        "toolbar.short.underline",
                        ChatFormatting.UNDERLINE,
                        RichTextAreaComponent::toggleUnderline,
                        true),
                formatAction(
                        "toolbar.short.strikethrough",
                        ChatFormatting.STRIKETHROUGH,
                        RichTextAreaComponent::toggleStrikethrough,
                        true),
                textAction("toolbar.obf", RichTextAreaComponent::toggleObfuscated, true),
                textAction("toolbar.cap", RichTextAreaComponent::capitalizeSelectionOrAll, false),
                textAction("toolbar.low", RichTextAreaComponent::lowercaseSelectionOrAll, false),
                textAction("common.reset", RichTextAreaComponent::clearFormatting, false));
    }

    private RichTextToolbarUtil() {}

    private static ToolAction formatAction(
            String labelKey,
            ChatFormatting formatting,
            Consumer<RichTextAreaComponent> action,
            boolean requiresPreparation) {
        return new ToolAction(styled(labelKey, formatting), tooltipFor(labelKey), action, requiresPreparation);
    }

    private static ToolAction textAction(
            String labelKey, Consumer<RichTextAreaComponent> action, boolean requiresPreparation) {
        return new ToolAction(ItemEditorText.tr(labelKey), tooltipFor(labelKey), action, requiresPreparation);
    }

    private static ToolAction deferredAction(
            String labelKey, BiConsumer<ItemEditorScreen, RichTextAreaComponent> action) {
        return new ToolAction(
                ItemEditorText.tr(labelKey),
                tooltipFor(labelKey),
                action,
                false,
                true,
                ToolActionPlacement.BEFORE_COLORS);
    }

    public static Component tooltipFor(String labelKey) {
        if (labelKey == null || labelKey.isBlank()) {
            return Component.empty();
        }

        String suffix;
        if (labelKey.equals("common.reset")) {
            suffix = "reset";
        } else if (labelKey.startsWith("toolbar.short.")) {
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
            int toolbarWidthHint) {
        FlowLayout tools = UiFactory.column();
        tools.gap(Math.max(1, UiFactory.scaleProfile().tightSpacing() - 1));
        AtomicInteger selectedShadowColor = TextStylingController.initialShadowColor(editor, DEFAULT_SHADOW_COLOR);
        AtomicReference<UnifiedColorPickerDialog.PaintLayer> textDraft =
                new AtomicReference<>(new UnifiedColorPickerDialog.PaintLayer(
                        UnifiedColorPickerDialog.PaintMode.COLOR, List.of(selectedColor.get())));
        AtomicReference<UnifiedColorPickerDialog.PaintLayer> shadowDraft =
                new AtomicReference<>(new UnifiedColorPickerDialog.PaintLayer(
                        UnifiedColorPickerDialog.PaintMode.COLOR, List.of(selectedShadowColor.get())));
        AtomicBoolean shadowEnabled = new AtomicBoolean(editor.selectionStyle().shadowColor() != null);
        Runnable preparation = prepareStyledApply == null ? () -> {} : prepareStyledApply;
        List<ToolbarItem> toolbarItems = new ArrayList<>();
        int maxRowWidth = toolbarAvailableWidth(screen, compactToolbar, toolbarWidthHint);

        ButtonComponent colorButton = null;
        if (includeColorPicker) {
            String unifiedColorDialogTitle =
                    includeGradient && !gradientDialogTitle.isBlank() ? gradientDialogTitle : colorDialogTitle;
            colorButton = UiFactory.button(
                    toolbarColorLabel(selectedColor.get()), UiFactory.ButtonTextPreset.STANDARD, button -> {
                        RichTextStyle style = editor.selectionStyle();
                        int foreground = style.color() == null ? selectedColor.get() : style.color();
                        int shadow = style.shadowColor() == null ? selectedShadowColor.get() : style.shadowColor();
                        UnifiedColorPickerDialog.PaintLayer initialText = textDraft.get();
                        if (initialText.mode() == UnifiedColorPickerDialog.PaintMode.COLOR) {
                            initialText = new UnifiedColorPickerDialog.PaintLayer(
                                    UnifiedColorPickerDialog.PaintMode.COLOR, List.of(foreground));
                        }
                        UnifiedColorPickerDialog.PaintLayer initialShadow = shadowDraft.get();
                        if (initialShadow.mode() == UnifiedColorPickerDialog.PaintMode.COLOR) {
                            initialShadow = new UnifiedColorPickerDialog.PaintLayer(
                                    UnifiedColorPickerDialog.PaintMode.COLOR, List.of(shadow));
                        }
                        screen.openPairedColorPickerDialog(
                                unifiedColorDialogTitle,
                                new UnifiedColorPickerDialog.Options(
                                        initialText.mode(),
                                        false,
                                        initialText.colors(),
                                        true,
                                        includeGradient,
                                        true,
                                        true,
                                        includeGradient,
                                        true,
                                        selectedColor.get(),
                                        editor.selectedTextOr(""),
                                        false),
                                initialText,
                                initialShadow,
                                style.shadowColor() != null || shadowEnabled.get(),
                                result -> applyPairedColor(
                                        editor,
                                        preparation,
                                        selectedColor,
                                        selectedShadowColor,
                                        textDraft,
                                        shadowDraft,
                                        shadowEnabled,
                                        result,
                                        button));
                    });
            Component tooltip = tooltipFor("toolbar.color");
            if (!tooltip.getString().isBlank()) {
                colorButton.tooltip(List.of(tooltip));
            } else if (!colorTooltip.isBlank() || !gradientTooltip.isBlank()) {
                colorButton.tooltip(
                        List.of(Component.literal(colorTooltip.isBlank() ? gradientTooltip : colorTooltip)));
            }
            toolbarItems.add(toolbarItem(colorButton, maxRowWidth));
        }
        ButtonComponent finalColorButton = colorButton;

        appendActionButtons(
                toolbarItems, actions, ToolActionPlacement.BEFORE_COLORS, screen, editor, preparation, maxRowWidth);

        for (TextColorPresets.Preset preset : TextColorPresets.STANDARD) {
            ButtonComponent presetButton = UiFactory.button(
                    standardColorLabel(preset),
                    UiFactory.ButtonTextPreset.STANDARD,
                    button -> applySolidColor(
                            editor, preparation, selectedColor, textDraft, preset.rgb(), finalColorButton));
            presetButton.tooltip(List.of(Component.literal(preset.label() + " " + ValidationUtil.toHex(preset.rgb()))
                    .withColor(preset.rgb())));
            toolbarItems.add(toolbarItem(presetButton, maxRowWidth));
        }

        appendActionButtons(
                toolbarItems, actions, ToolActionPlacement.AFTER_COLORS, screen, editor, preparation, maxRowWidth);

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
        preserveToolbarButtonLabel(button);
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
            AtomicReference<UnifiedColorPickerDialog.PaintLayer> textDraft,
            int color,
            ButtonComponent pickColorButton) {
        boolean hadSelection = editor.hasSelection();
        preparation.run();
        selectedColor.set(color);
        textDraft.set(
                new UnifiedColorPickerDialog.PaintLayer(UnifiedColorPickerDialog.PaintMode.COLOR, List.of(color)));
        editor.applyColor(color);
        if (pickColorButton != null) {
            pickColorButton.setMessage(toolbarColorLabel(color));
        }
        editor.resumeEditing();
        editor.collapseUnexpectedSelection(hadSelection);
    }

    private static void applyPairedColor(
            RichTextAreaComponent editor,
            Runnable preparation,
            AtomicInteger selectedColor,
            AtomicInteger selectedShadowColor,
            AtomicReference<UnifiedColorPickerDialog.PaintLayer> textDraft,
            AtomicReference<UnifiedColorPickerDialog.PaintLayer> shadowDraft,
            AtomicBoolean shadowEnabled,
            UnifiedColorPickerDialog.PairedColorResult result,
            ButtonComponent button) {
        boolean hadSelection = editor.hasSelection();
        preparation.run();
        editor.applyPairedColors(result);
        textDraft.set(result.text());
        shadowDraft.set(result.shadow());
        shadowEnabled.set(result.shadowEnabled());
        selectedColor.set(result.text().colors().getFirst());
        selectedShadowColor.set(result.shadow().colors().getFirst() | 0xFF000000);
        Component label = result.text().mode() == UnifiedColorPickerDialog.PaintMode.GRADIENT
                ? TextColorPresets.gradientLabel(
                        ItemEditorText.str("toolbar.color"), result.text().colors())
                : toolbarColorLabel(selectedColor.get());
        if (result.shadowEnabled()) {
            int color = selectedShadowColor.get();
            label = label.copy().withStyle(style -> style.withShadowColor(color));
        }
        button.setMessage(label);
        editor.resumeEditing();
        editor.collapseUnexpectedSelection(hadSelection);
    }

    private static Component toolbarColorLabel(int color) {
        return ItemEditorText.tr("toolbar.color").copy().withColor(color & 0xFFFFFF);
    }

    private static int toolbarAvailableWidth(ItemEditorScreen screen, boolean compactToolbar, int toolbarWidthHint) {
        int hintedWidth = toolbarWidthHint > 1 ? toolbarWidthHint : Math.max(1, screen.editorContentWidthHint());
        int contentWidth = Math.max(TOOLBAR_CONTENT_WIDTH_MIN, hintedWidth);
        int sideInsets = UiFactory.scaledPixels(compactToolbar ? 10 : 14);
        int safety = UiFactory.scaledPixels(compactToolbar ? 12 : 18);
        int preferredMin = compactToolbar ? 160 : 200;
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

    private static void preserveToolbarButtonLabel(ButtonComponent button) {
        Component message = button.getMessage();
        button.tooltip(List.of(message));
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
            int maxRowWidth) {
        for (ToolAction action : actions) {
            if (action.placement() != placement) {
                continue;
            }
            ButtonComponent button = UiFactory.button(
                    action.label(),
                    UiFactory.ButtonTextPreset.STANDARD,
                    component -> applyToolbarAction(screen, editor, action, preparation));
            if (!action.tooltip().getString().isBlank()) {
                button.tooltip(List.of(action.tooltip()));
            }
            toolbarItems.add(toolbarItem(button, maxRowWidth));
        }
    }

    private static void applyToolbarAction(
            ItemEditorScreen screen, RichTextAreaComponent editor, ToolAction action, Runnable preparation) {
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
        screen.openRichTextHeadDialog(ItemEditorText.str("toolbar.tooltip.head"), tokenInserter(editor));
    }

    private static void openFontDialog(ItemEditorScreen screen, RichTextAreaComponent editor) {
        FileToIdConverter converter = FileToIdConverter.json("font");
        TreeSet<String> fonts = new TreeSet<>();
        converter.listMatchingResources(Minecraft.getInstance().getResourceManager()).keySet().stream()
                .map(converter::fileToId)
                .filter(id ->
                        !id.getNamespace().equals("minecraft") || !id.getPath().startsWith("include/"))
                .map(Identifier::toString)
                .forEach(fonts::add);
        String current = editor.currentFont() instanceof FontDescription.Resource(var id)
                ? id.toString()
                : FontDescription.DEFAULT.id().toString();
        fonts.add(current);
        fonts.remove(FontDescription.DEFAULT.id().toString());
        List<String> choices = new ArrayList<>();
        choices.add(FontDescription.DEFAULT.id().toString());
        choices.addAll(fonts);
        boolean hadSelection = editor.hasSelection();
        screen.openSearchablePickerDialog(
                ItemEditorText.str("toolbar.font"),
                ItemEditorText.str("dialog.font.current", current),
                choices,
                value -> value,
                value -> {
                    editor.applyFont(new FontDescription.Resource(Identifier.parse(value)));
                    editor.resumeEditing();
                    editor.collapseUnexpectedSelection(hadSelection);
                });
    }

    private static void openSpriteTokenDialog(ItemEditorScreen screen, RichTextAreaComponent editor) {
        screen.openRichTextSpriteDialog(ItemEditorText.str("toolbar.tooltip.sprite"), tokenInserter(editor));
    }

    private static void openTranslationTokenDialog(ItemEditorScreen screen, RichTextAreaComponent editor) {
        screen.openRichTextTranslationDialog(
                ItemEditorText.str("dialog.rich_text.translation.title"),
                editor.selectedTextOr(""),
                tokenInserter(editor));
    }

    private static void openEventTokenDialog(
            ItemEditorScreen screen,
            RichTextAreaComponent editor,
            boolean includeHoverModes,
            boolean includeSuggestCommand) {
        screen.openRichTextEventDialog(
                ItemEditorText.str("toolbar.event"),
                includeHoverModes,
                includeSuggestCommand,
                editor.selectedTextOr(TOKEN_PLACEHOLDER),
                tokenInserter(editor));
    }

    private static Consumer<String> tokenInserter(RichTextAreaComponent editor) {
        boolean hadSelection = editor.hasSelection();
        return token -> {
            editor.insertTemplate(token);
            editor.resumeEditing();
            editor.collapseUnexpectedSelection(hadSelection);
        };
    }

    private record ToolbarItem(UIComponent component, int layoutWidth) {}

    public record ToolAction(
            Component label,
            Component tooltip,
            BiConsumer<ItemEditorScreen, RichTextAreaComponent> action,
            boolean requiresPreparation,
            boolean deferredMutation,
            ToolActionPlacement placement) {
        public ToolAction(
                Component label,
                Component tooltip,
                Consumer<RichTextAreaComponent> action,
                boolean requiresPreparation,
                ToolActionPlacement placement) {
            this(label, tooltip, (screen, editor) -> action.accept(editor), requiresPreparation, false, placement);
        }

        public ToolAction(
                Component label,
                Component tooltip,
                Consumer<RichTextAreaComponent> action,
                boolean requiresPreparation) {
            this(label, tooltip, action, requiresPreparation, ToolActionPlacement.BEFORE_COLORS);
        }
    }

    public enum ToolActionPlacement {
        BEFORE_COLORS,
        AFTER_COLORS
    }
}
