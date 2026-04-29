package me.noramibu.itemeditor.ui.component;

import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Sizing;
import me.noramibu.itemeditor.editor.text.RichTextDocument;
import me.noramibu.itemeditor.editor.text.RichTextStyle;
import me.noramibu.itemeditor.ui.screen.ItemEditorScreen;
import me.noramibu.itemeditor.util.ItemEditorText;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;

public final class StyledTextFieldSection {
    private static final double COMPACT_TOOLBAR_SCALE_THRESHOLD = 3.0d;
    private static final int COMPACT_TOOLBAR_CONTENT_WIDTH_THRESHOLD = 560;
    private static final boolean DEFAULT_COMPACT_TOOLBAR = false;
    private static final int DEFAULT_TOOLBAR_WIDTH_HINT = -1;

    private StyledTextFieldSection() {
    }

    public static BoundEditor create(
            ItemEditorScreen screen,
            RichTextDocument initialDocument,
            Sizing width,
            Sizing height,
            String placeholder,
            RichTextStyle defaultStyle,
            boolean displayCharCount,
            int initialColor,
            int palettePrimary,
            int paletteSecondary,
            int paletteSelection,
            int palettePlaceholder,
            int chromeFill,
            int chromeOutline,
            List<RichTextToolbarUtil.ToolAction> actions,
            String colorDialogTitle,
            String gradientDialogTitle,
            String colorTooltip,
            String gradientTooltip,
            Runnable prepareStyledApply,
            boolean includeColorPicker,
            boolean includeGradient,
            Function<RichTextDocument, String> validator,
            Consumer<RichTextDocument> onDocumentChanged
    ) {
        return createRichTextField(
                screen,
                initialDocument,
                width,
                height,
                placeholder,
                defaultStyle,
                displayCharCount,
                initialColor,
                palettePrimary,
                paletteSecondary,
                paletteSelection,
                palettePlaceholder,
                chromeFill,
                chromeOutline,
                actions,
                colorDialogTitle,
                gradientDialogTitle,
                colorTooltip,
                gradientTooltip,
                prepareStyledApply,
                includeColorPicker,
                includeGradient,
                validator,
                onDocumentChanged,
                DEFAULT_COMPACT_TOOLBAR,
                DEFAULT_TOOLBAR_WIDTH_HINT
        );
    }

    public static BoundEditor create(
            ItemEditorScreen screen,
            RichTextDocument initialDocument,
            Sizing width,
            Sizing height,
            String placeholder,
            RichTextStyle defaultStyle,
            boolean displayCharCount,
            int initialColor,
            int palettePrimary,
            int paletteSecondary,
            int paletteSelection,
            int palettePlaceholder,
            int chromeFill,
            int chromeOutline,
            List<RichTextToolbarUtil.ToolAction> actions,
            String colorDialogTitle,
            String gradientDialogTitle,
            String colorTooltip,
            String gradientTooltip,
            Runnable prepareStyledApply,
            boolean includeColorPicker,
            boolean includeGradient,
            Function<RichTextDocument, String> validator,
            Consumer<RichTextDocument> onDocumentChanged,
            boolean compactToolbar
    ) {
        return createRichTextField(
                screen,
                initialDocument,
                width,
                height,
                placeholder,
                defaultStyle,
                displayCharCount,
                initialColor,
                palettePrimary,
                paletteSecondary,
                paletteSelection,
                palettePlaceholder,
                chromeFill,
                chromeOutline,
                actions,
                colorDialogTitle,
                gradientDialogTitle,
                colorTooltip,
                gradientTooltip,
                prepareStyledApply,
                includeColorPicker,
                includeGradient,
                validator,
                onDocumentChanged,
                compactToolbar,
                DEFAULT_TOOLBAR_WIDTH_HINT
        );
    }

    public static BoundEditor create(
            ItemEditorScreen screen,
            RichTextDocument initialDocument,
            Sizing width,
            Sizing height,
            String placeholder,
            RichTextStyle defaultStyle,
            boolean displayCharCount,
            int initialColor,
            int palettePrimary,
            int paletteSecondary,
            int paletteSelection,
            int palettePlaceholder,
            int chromeFill,
            int chromeOutline,
            List<RichTextToolbarUtil.ToolAction> actions,
            String colorDialogTitle,
            String gradientDialogTitle,
            String colorTooltip,
            String gradientTooltip,
            Runnable prepareStyledApply,
            boolean includeColorPicker,
            boolean includeGradient,
            Function<RichTextDocument, String> validator,
            Consumer<RichTextDocument> onDocumentChanged,
            boolean compactToolbar,
            int toolbarWidthHint
    ) {
        return createRichTextField(
                screen,
                initialDocument,
                width,
                height,
                placeholder,
                defaultStyle,
                displayCharCount,
                initialColor,
                palettePrimary,
                paletteSecondary,
                paletteSelection,
                palettePlaceholder,
                chromeFill,
                chromeOutline,
                actions,
                colorDialogTitle,
                gradientDialogTitle,
                colorTooltip,
                gradientTooltip,
                prepareStyledApply,
                includeColorPicker,
                includeGradient,
                validator,
                onDocumentChanged,
                compactToolbar,
                toolbarWidthHint
        );
    }

    private static BoundEditor createRichTextField(
            ItemEditorScreen screen,
            RichTextDocument initialDocument,
            Sizing width,
            Sizing height,
            String placeholder,
            RichTextStyle defaultStyle,
            boolean displayCharCount,
            int initialColor,
            int palettePrimary,
            int paletteSecondary,
            int paletteSelection,
            int palettePlaceholder,
            int chromeFill,
            int chromeOutline,
            List<RichTextToolbarUtil.ToolAction> actions,
            String colorDialogTitle,
            String gradientDialogTitle,
            String colorTooltip,
            String gradientTooltip,
            Runnable prepareStyledApply,
            boolean includeColorPicker,
            boolean includeGradient,
            Function<RichTextDocument, String> validator,
            Consumer<RichTextDocument> onDocumentChanged,
            boolean compactToolbar,
            int toolbarWidthHint
    ) {
        int effectiveToolbarWidthHint = toolbarWidthHint > 1
                ? toolbarWidthHint
                : screen.editorContentWidthHint();
        boolean effectiveCompactToolbar = compactToolbar
                || screen.session().minecraft().getWindow().getGuiScale() >= COMPACT_TOOLBAR_SCALE_THRESHOLD
                || effectiveToolbarWidthHint < UiFactory.scaledPixels(COMPACT_TOOLBAR_CONTENT_WIDTH_THRESHOLD);
        RichTextAreaComponent editor = new RichTextAreaComponent(width, height, initialDocument);
        editor.placeholder(placeholder);
        editor.displayCharCount(displayCharCount);
        if (defaultStyle != null) {
            editor.defaultStyle(defaultStyle);
        }
        editor.palette(palettePrimary, paletteSecondary, paletteSelection, palettePlaceholder);
        editor.chrome(chromeFill, chromeOutline);

        AtomicInteger selectedColor = TextStylingController.initialColor(editor, initialColor);
        LabelComponent validation = UiFactory.message("", 0xF2C26B);
        FlowLayout toolbar = RichTextToolbarUtil.buildToolbar(
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
                effectiveCompactToolbar,
                toolbarWidthHint
        );

        TextStylingController.bindValidatedDocument(
                editor,
                validation,
                validator,
                onDocumentChanged
        );

        return new BoundEditor(editor, toolbar, validation);
    }

    public static BoundEditor create(
            ItemEditorScreen screen,
            RichTextDocument initialDocument,
            Sizing width,
            Sizing height,
            String placeholder,
            StylePreset preset,
            String colorDialogTitle,
            String gradientDialogTitle,
            String colorTooltip,
            String gradientTooltip,
            Runnable prepareStyledApply,
            Function<RichTextDocument, String> validator,
            Consumer<RichTextDocument> onDocumentChanged
    ) {
        return createPresetField(
                screen,
                initialDocument,
                width,
                height,
                placeholder,
                preset,
                colorDialogTitle,
                gradientDialogTitle,
                colorTooltip,
                gradientTooltip,
                prepareStyledApply,
                validator,
                onDocumentChanged,
                DEFAULT_COMPACT_TOOLBAR,
                DEFAULT_TOOLBAR_WIDTH_HINT
        );
    }

    public static BoundEditor create(
            ItemEditorScreen screen,
            RichTextDocument initialDocument,
            Sizing width,
            Sizing height,
            String placeholder,
            StylePreset preset,
            String colorDialogTitle,
            String gradientDialogTitle,
            String colorTooltip,
            String gradientTooltip,
            Runnable prepareStyledApply,
            Function<RichTextDocument, String> validator,
            Consumer<RichTextDocument> onDocumentChanged,
            boolean compactToolbar
    ) {
        return createPresetField(
                screen,
                initialDocument,
                width,
                height,
                placeholder,
                preset,
                colorDialogTitle,
                gradientDialogTitle,
                colorTooltip,
                gradientTooltip,
                prepareStyledApply,
                validator,
                onDocumentChanged,
                compactToolbar,
                DEFAULT_TOOLBAR_WIDTH_HINT
        );
    }

    public static BoundEditor create(
            ItemEditorScreen screen,
            RichTextDocument initialDocument,
            Sizing width,
            Sizing height,
            String placeholder,
            StylePreset preset,
            String colorDialogTitle,
            String gradientDialogTitle,
            String colorTooltip,
            String gradientTooltip,
            Runnable prepareStyledApply,
            Function<RichTextDocument, String> validator,
            Consumer<RichTextDocument> onDocumentChanged,
            boolean compactToolbar,
            int toolbarWidthHint
    ) {
        return createPresetField(
                screen,
                initialDocument,
                width,
                height,
                placeholder,
                preset,
                colorDialogTitle,
                gradientDialogTitle,
                colorTooltip,
                gradientTooltip,
                prepareStyledApply,
                validator,
                onDocumentChanged,
                compactToolbar,
                toolbarWidthHint
        );
    }

    private static BoundEditor createPresetField(
            ItemEditorScreen screen,
            RichTextDocument initialDocument,
            Sizing width,
            Sizing height,
            String placeholder,
            StylePreset preset,
            String colorDialogTitle,
            String gradientDialogTitle,
            String colorTooltip,
            String gradientTooltip,
            Runnable prepareStyledApply,
            Function<RichTextDocument, String> validator,
            Consumer<RichTextDocument> onDocumentChanged,
            boolean compactToolbar,
            int toolbarWidthHint
    ) {
        BoundEditor boundEditor = create(
                screen,
                initialDocument,
                width,
                height,
                placeholder,
                preset.defaultStyle(),
                preset.displayCharCount(),
                preset.initialColor(),
                preset.palettePrimary(),
                preset.paletteSecondary(),
                preset.paletteSelection(),
                preset.palettePlaceholder(),
                preset.chromeFill(),
                preset.chromeOutline(),
                preset.actions(),
                colorDialogTitle,
                gradientDialogTitle,
                colorTooltip,
                gradientTooltip,
                prepareStyledApply,
                preset.includeColorPicker(),
                preset.includeGradient(),
                validator,
                onDocumentChanged,
                compactToolbar,
                toolbarWidthHint
        );
        boundEditor.editor().renderStructuredObjects(preset.renderStructuredObjectsByDefault());
        return boundEditor;
    }

    public record StylePreset(
            RichTextStyle defaultStyle,
            boolean displayCharCount,
            int initialColor,
            int palettePrimary,
            int paletteSecondary,
            int paletteSelection,
            int palettePlaceholder,
            int chromeFill,
            int chromeOutline,
            List<RichTextToolbarUtil.ToolAction> actions,
            boolean includeColorPicker,
            boolean includeGradient,
            boolean renderStructuredObjectsByDefault
    ) {
        public static StylePreset name() {
            return new StylePreset(
                    null,
                    true,
                    0xFFFFFF,
                    0xFFF2F5F8,
                    0x90768496,
                    0xC56E93FF,
                    0xFFF2F5F8,
                    0xD0111620,
                    0xFF42506A,
                    RichTextToolbarUtil.BASIC_ACTIONS,
                    true,
                    true,
                    true
            );
        }

        public static StylePreset lore(int baseColor, RichTextStyle defaultStyle) {
            return new StylePreset(
                    defaultStyle,
                    true,
                    baseColor,
                    baseColor,
                    0x90786692,
                    0xCC8D7BFF,
                    0xFFF6EEFF,
                    0xD0121723,
                    0xFF4B5670,
                    RichTextToolbarUtil.EXTENDED_ACTIONS,
                    true,
                    true,
                    true
            );
        }

        public static StylePreset bookPage() {
            return new StylePreset(
                    null,
                    false,
                    0x3D2D1F,
                    0x3D2D1F,
                    0x9076644E,
                    0xB99AB7FF,
                    0xFF3C2E22,
                    0xFFF3E6C8,
                    0xFFB29A72,
                    RichTextToolbarUtil.BOOK_OUTPUT_ACTIONS,
                    true,
                    true,
                    true
            );
        }

        public static StylePreset writableBookPage() {
            return new StylePreset(
                    RichTextStyle.EMPTY,
                    false,
                    0x3D2D1F,
                    0x3D2D1F,
                    0x9076644E,
                    0xB99AB7FF,
                    0xFF3C2E22,
                    0xFFF3E6C8,
                    0xFFB29A72,
                    List.of(
                            new RichTextToolbarUtil.ToolAction(ItemEditorText.tr("toolbar.cap"), Component.empty(), RichTextAreaComponent::capitalizeSelectionOrAll, false),
                            new RichTextToolbarUtil.ToolAction(ItemEditorText.tr("toolbar.low"), Component.empty(), RichTextAreaComponent::lowercaseSelectionOrAll, false),
                            new RichTextToolbarUtil.ToolAction(ItemEditorText.tr("toolbar.reset"), Component.empty(), RichTextAreaComponent::clearFormatting, false)
                    ),
                    false,
                    false,
                    false
            );
        }

        public static StylePreset bookMetadata(boolean paletteOnly) {
            return new StylePreset(
                    RichTextStyle.EMPTY,
                    true,
                    0xFFFFFF,
                    0xFFF3F5F7,
                    0x90768496,
                    0xC56E93FF,
                    0xFFF2F5F8,
                    0xD0111620,
                    0xFF42506A,
                    RichTextToolbarUtil.BOOK_METADATA_ACTIONS,
                    !paletteOnly,
                    !paletteOnly,
                    false
            );
        }

        public static StylePreset signContent() {
            return new StylePreset(
                    RichTextStyle.EMPTY,
                    true,
                    0xFFFFFF,
                    0xFFF3F5F7,
                    0x90768496,
                    0xC56E93FF,
                    0xFFF2F5F8,
                    0xD0111620,
                    0xFF42506A,
                    RichTextToolbarUtil.SIGN_OUTPUT_ACTIONS,
                    true,
                    true,
                    true
            );
        }
    }

    public record BoundEditor(RichTextAreaComponent editor, FlowLayout toolbar, LabelComponent validation) {
    }
}
