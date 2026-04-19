package me.noramibu.itemeditor.ui.component;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.CheckboxComponent;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.component.TextAreaComponent;
import io.wispforest.owo.ui.component.TextBoxComponent;
import io.wispforest.owo.ui.component.UIComponents;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.UIContainers;
import io.wispforest.owo.ui.core.Color;
import io.wispforest.owo.ui.core.HorizontalAlignment;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.Surface;
import io.wispforest.owo.ui.core.UIComponent;
import io.wispforest.owo.ui.core.VerticalAlignment;
import me.noramibu.itemeditor.ui.scale.UiScaleProfile;
import me.noramibu.itemeditor.ui.scale.UiScaleService;
import me.noramibu.itemeditor.util.ItemEditorText;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public final class UiFactory {

    private static final Surface CARD_SURFACE = Surface.flat(0xAA141A22).and(Surface.outline(0xFF2F3945));
    private static final Surface SUB_CARD_SURFACE = Surface.flat(0xAA1B222B).and(Surface.outline(0xFF414B56));
    private static final Surface EDITOR_FRAME_SURFACE = Surface.flat(0xB0121822).and(Surface.outline(0xFF3F4D63));
    private static final int FIELD_TEXT_MIN = 120;
    private static final int FIELD_TEXT_MAX = 300;
    private static final int BODY_TEXT_MIN = 130;
    private static final int BODY_TEXT_MAX = 320;
    private static final int CHECKBOX_VIEWPORT_TEXT_WIDTH_MIN = 120;
    private static final float BUTTON_TEXT_MIN_SCALE = 0.95F;
    private static final float BUTTON_TEXT_MAX_SCALE = 1.55F;
    private static final String BLANK_TEXT = " ";
    private static final String SYMBOL_SECTION_COLLAPSED = "[+]";
    private static final String SYMBOL_SECTION_EXPANDED = "[-]";
    private static final int REMOVE_ACTION_WIDTH_MIN = 88;
    private static final int REMOVE_ACTION_WIDTH_BASE = 108;
    private static final double COMPACT_ACTION_ROW_SCALE_THRESHOLD = 3.0d;
    private static final int COMPACT_ACTION_ROW_WIDTH_THRESHOLD = 1100;

    public enum TextPreset {
        TITLE(1.00F),
        BODY(1.00F),
        CAPTION(1.00F),
        BUTTON_TINY(0.95F),
        BUTTON_COMPACT(1.00F),
        BUTTON_STANDARD(1.00F),
        BUTTON_LARGE(1.10F);

        final float scale;

        TextPreset(float scale) {
            this.scale = scale;
        }
    }

    public enum ButtonPreset {
        TINY(16, 28, 12),
        COMPACT(18, 34, 14),
        STANDARD(20, 44, 18),
        LARGE(22, 58, 22);

        final int minHeight;
        final int minWidth;
        final int horizontalPadding;

        ButtonPreset(int minHeight, int minWidth, int horizontalPadding) {
            this.minHeight = minHeight;
            this.minWidth = minWidth;
            this.horizontalPadding = horizontalPadding;
        }
    }

    public enum ButtonTextPreset {
        TINY(ButtonPreset.TINY, TextPreset.BUTTON_TINY),
        COMPACT(ButtonPreset.COMPACT, TextPreset.BUTTON_COMPACT),
        STANDARD(ButtonPreset.STANDARD, TextPreset.BUTTON_STANDARD),
        LARGE(ButtonPreset.LARGE, TextPreset.BUTTON_LARGE);

        final ButtonPreset buttonPreset;
        final TextPreset textPreset;

        ButtonTextPreset(ButtonPreset buttonPreset, TextPreset textPreset) {
            this.buttonPreset = buttonPreset;
            this.textPreset = textPreset;
        }
    }

    private UiFactory() {
    }

    public static FlowLayout column() {
        UiScaleProfile profile = scaleProfile();
        return baseFlow(profile, false);
    }

    public static FlowLayout row() {
        UiScaleProfile profile = scaleProfile();
        return baseFlow(profile, true);
    }

    public static void appendFillChild(FlowLayout parent, UIComponent child) {
        child.horizontalSizing(Sizing.fill(100));
        if (child instanceof FlowLayout flowLayout) {
            applyFlowContract(flowLayout);
        }
        parent.child(child);
    }

    private static <T extends FlowLayout> T applyFlowContract(T flowLayout) {
        flowLayout.allowOverflow(false);
        return flowLayout;
    }

    private static FlowLayout baseFlow(UiScaleProfile profile, boolean horizontal) {
        FlowLayout flow = horizontal
                ? UIContainers.horizontalFlow(Sizing.fill(100), Sizing.content())
                : UIContainers.verticalFlow(Sizing.fill(100), Sizing.content());
        flow.gap(profile.spacing());
        if (horizontal) {
            flow.verticalAlignment(VerticalAlignment.CENTER);
        }
        return applyFlowContract(flow);
    }

    public static FlowLayout collapsibleSummaryRow(
            Component summaryText,
            int summaryMaxWidth,
            boolean collapsed,
            Runnable onToggle
    ) {
        FlowLayout summary = row();
        summary.child(muted(summaryText, summaryMaxWidth));
        ButtonComponent collapseToggle = button(Component.literal(collapsed ? SYMBOL_SECTION_COLLAPSED : SYMBOL_SECTION_EXPANDED), ButtonTextPreset.COMPACT, button -> onToggle.run());
        collapseToggle.horizontalSizing(Sizing.fixed(Math.max(30, scaledPixels(36))));
        summary.child(collapseToggle);
        return summary;
    }

    public static FlowLayout card() {
        UiScaleProfile profile = scaleProfile();
        FlowLayout card = column();
        card.padding(Insets.of(profile.padding()));
        card.surface(CARD_SURFACE);
        return card;
    }

    public static FlowLayout section(Component title, Component description) {
        FlowLayout section = card();
        section.child(title(title));
        if (!description.getString().isBlank()) {
            section.child(muted(description));
        }
        return section;
    }

    public static FlowLayout field(Component label, Component helpText, UIComponent input) {
        UiScaleProfile profile = scaleProfile();
        FlowLayout field = column().gap(profile.tightSpacing());
        int textWidth = responsiveFieldTextWidth();
        field.child(title(label).shadow(false).maxWidth(textWidth));
        if (!helpText.getString().isBlank()) {
            field.child(muted(helpText, textWidth));
        }
        field.child(input.horizontalSizing(Sizing.fill(100)));
        return field;
    }

    public static FlowLayout subCard() {
        UiScaleProfile profile = scaleProfile();
        FlowLayout card = column();
        card.padding(Insets.of(Math.max(4, profile.padding() - 1)));
        card.surface(SUB_CARD_SURFACE);
        return card;
    }

    public static LabelComponent title(String text) {
        return title(Component.literal(text));
    }

    public static LabelComponent title(Component text) {
        return styledText(text, TextPreset.TITLE);
    }

    public static LabelComponent title(Component text, float scaleFactor) {
        return styledText(text, TextPreset.TITLE, scaleFactor);
    }

    public static LabelComponent muted(String text) {
        return muted(text, responsiveBodyTextWidth());
    }

    public static LabelComponent muted(String text, int maxWidth) {
        return muted(Component.literal(text), maxWidth);
    }

    public static LabelComponent muted(Component text) {
        return muted(text, responsiveBodyTextWidth());
    }

    public static LabelComponent muted(Component text, int maxWidth) {
        return styledText(text, TextPreset.CAPTION)
                .color(Color.ofRgb(0xA9B5C0))
                .maxWidth(maxWidth);
    }

    public static LabelComponent muted(Component text, int maxWidth, float scaleFactor) {
        return styledText(text, TextPreset.CAPTION, scaleFactor)
                .color(Color.ofRgb(0xA9B5C0))
                .maxWidth(maxWidth);
    }

    public static LabelComponent message(String text, int color) {
        return message(Component.literal(text), color);
    }

    public static LabelComponent message(Component text, int color) {
        return styledText(safeMessageText(text), TextPreset.BODY)
                .color(Color.ofRgb(color))
                .maxWidth(responsiveBodyTextWidth());
    }

    public static LabelComponent message(Component text, int color, float scaleFactor) {
        return styledText(safeMessageText(text), TextPreset.BODY, scaleFactor)
                .color(Color.ofRgb(color))
                .maxWidth(responsiveBodyTextWidth());
    }

    public static LabelComponent bodyLabel(Component text) {
        return styledText(text, TextPreset.BODY);
    }

    public static LabelComponent bodyLabel(Component text, float scaleFactor) {
        return styledText(text, TextPreset.BODY, scaleFactor);
    }

    public static ButtonComponent button(String text, ButtonTextPreset preset, Consumer<ButtonComponent> onPress) {
        return button(Component.literal(text), preset, onPress);
    }

    public static ButtonComponent button(Component text, ButtonTextPreset preset, Consumer<ButtonComponent> onPress) {
        return createAdaptiveButton(text, buttonTextScale(preset.textPreset), preset.buttonPreset, onPress);
    }

    public static ButtonComponent button(Component text, ButtonPreset buttonPreset, TextPreset textPreset, Consumer<ButtonComponent> onPress) {
        return createAdaptiveButton(text, buttonTextScale(textPreset), buttonPreset, onPress);
    }

    public static ButtonComponent scaledTextButton(Component fullText, float textScale, ButtonTextPreset preset, Consumer<ButtonComponent> onPress) {
        float preferredScale = Math.max(BUTTON_TEXT_MIN_SCALE, Math.min(BUTTON_TEXT_MAX_SCALE, textScale));
        return createAdaptiveButton(fullText, preferredScale, preset.buttonPreset, onPress);
    }

    public static ButtonComponent scaledTextButton(Component fullText, float textScale, ButtonPreset preset, Consumer<ButtonComponent> onPress) {
        float preferredScale = Math.max(BUTTON_TEXT_MIN_SCALE, Math.min(BUTTON_TEXT_MAX_SCALE, textScale));
        return createAdaptiveButton(fullText, preferredScale, preset, onPress);
    }

    public static ButtonComponent scaledTextButton(Component fullText, ButtonTextPreset preset, Consumer<ButtonComponent> onPress) {
        return createAdaptiveButton(fullText, buttonTextScale(preset.textPreset), preset.buttonPreset, onPress);
    }

    public static void applyButtonPreset(ButtonComponent button, ButtonPreset preset) {
        if (button == null) {
            return;
        }
        int minHeight = Math.max(12, scaledPixels(preset.minHeight));
        if (button.getHeight() <= 0 || button.getHeight() < minHeight) {
            button.verticalSizing(Sizing.fixed(minHeight));
        }
        int minWidth = Math.max(16, scaledPixels(preset.minWidth));
        if (button.getWidth() <= 0 || button.getWidth() < minWidth) {
            button.horizontalSizing(Sizing.fixed(minWidth));
        }
    }

    public static void applyButtonPreset(ButtonComponent button, ButtonTextPreset preset) {
        applyButtonPreset(button, preset.buttonPreset);
    }

    public static ButtonComponent pickerButton(Component text, int width, Consumer<ButtonComponent> onPress) {
        ButtonComponent button = button(text, ButtonTextPreset.STANDARD, onPress);
        if (width > 0) {
            button.horizontalSizing(fixed(width));
        } else {
            button.horizontalSizing(Sizing.fill(100));
        }
        return button;
    }

    public static FlowLayout pickerField(
            Component label,
            Component helpText,
            Component buttonText,
            int buttonWidth,
            Consumer<ButtonComponent> onPress
    ) {
        return field(label, helpText, pickerButton(buttonText, buttonWidth, onPress));
    }

    public static FlowLayout removableSubCard(Component title, Runnable onRemove) {
        return reorderableSubCard(title, false, null, false, null, onRemove);
    }

    public static FlowLayout reorderableSubCard(
            Component title,
            boolean canMoveUp,
            Runnable onMoveUp,
            boolean canMoveDown,
            Runnable onMoveDown,
            Runnable onRemove
    ) {
        FlowLayout card = subCard();
        card.child(reorderableHeader(title, canMoveUp, onMoveUp, canMoveDown, onMoveDown, onRemove));
        return card;
    }

    public static FlowLayout reorderableHeader(
            Component title,
            boolean canMoveUp,
            Runnable onMoveUp,
            boolean canMoveDown,
            Runnable onMoveDown,
            Runnable onRemove
    ) {
        FlowLayout header = column().gap(Math.max(1, scaleProfile().tightSpacing()));
        FlowLayout titleRow = row();
        titleRow.child(title(title).shadow(false).horizontalSizing(Sizing.expand(100)));
        header.child(titleRow);

        boolean compactActions = useCompactActionRow();
        FlowLayout actionRow = compactActions ? column() : row();
        boolean hasActions = false;
        if (onMoveUp != null) {
            actionRow.child(actionButton(ItemEditorText.tr("common.up"), canMoveUp, onMoveUp));
            hasActions = true;
        }
        if (onMoveDown != null) {
            actionRow.child(actionButton(ItemEditorText.tr("common.down"), canMoveDown, onMoveDown));
            hasActions = true;
        }
        if (onRemove != null) {
            actionRow.child(actionButton(ItemEditorText.tr("common.remove"), true, onRemove));
            hasActions = true;
        }
        if (hasActions) {
            header.child(actionRow);
        }
        return header;
    }

    public static Component fitToWidth(Component text, int maxPixelWidth) {
        if (maxPixelWidth <= 0) {
            return Component.empty();
        }

        Minecraft minecraft = Minecraft.getInstance();
        String raw = text.getString();
        if (minecraft.font.width(raw) <= maxPixelWidth) {
            return text;
        }

        String ellipsis = "...";
        int ellipsisWidth = minecraft.font.width(ellipsis);
        if (maxPixelWidth <= ellipsisWidth) {
            return Component.literal(ellipsis);
        }

        String shortened = minecraft.font.plainSubstrByWidth(raw, Math.max(0, maxPixelWidth - ellipsisWidth)).trim();
        if (shortened.isEmpty()) {
            return Component.literal(ellipsis);
        }
        return Component.literal(shortened + ellipsis);
    }

    public static TextBoxComponent textBox(String value, Consumer<String> onChanged) {
        TextBoxComponent box = UIComponents.textBox(Sizing.fill(100), value);
        box.verticalSizing(Sizing.fixed(scaleProfile().controlHeight()));
        box.onChanged().subscribe(onChanged::accept);
        return box;
    }

    public static TextAreaComponent textArea(String value, int height, Consumer<String> onChanged) {
        TextAreaComponent textArea = UIComponents.textArea(Sizing.fill(100), fixed(height), value);
        textArea.onChanged().subscribe(onChanged::accept);
        textArea.displayCharCount(true);
        return textArea;
    }

    public static CheckboxComponent checkbox(Component text, boolean checked, Consumer<Boolean> onChanged) {
        Component displayText = text;
        if (text != null && !text.getString().isBlank()) {
            int viewportBound = Math.max(CHECKBOX_VIEWPORT_TEXT_WIDTH_MIN, guiScaledWidth() / 4);
            int maxTextWidth = Math.max(80, Math.min(responsiveBodyTextWidth(), viewportBound));
            displayText = fitToWidth(text, maxTextWidth);
        }

        CheckboxComponent checkbox = UIComponents.checkbox(displayText);
        checkbox.verticalSizing(Sizing.fixed(scaleProfile().controlHeight()));
        checkbox.checked(checked);
        checkbox.onChanged(onChanged);
        if (text != null && !text.getString().isBlank() && !displayText.getString().equals(text.getString())) {
            checkbox.tooltip(java.util.List.of(text));
        }
        return checkbox;
    }

    public static FlowLayout centeredCard(int width) {
        FlowLayout card = card();
        card.horizontalSizing(Sizing.fixed(DialogUiUtil.dialogWidth(width)));
        card.horizontalAlignment(HorizontalAlignment.CENTER);
        return card;
    }

    public static FlowLayout framedEditorCard() {
        FlowLayout frame = subCard();
        frame.padding(Insets.of(5));
        frame.surface(EDITOR_FRAME_SURFACE);
        return frame;
    }

    public static int responsiveFieldTextWidth() {
        UiScaleProfile profile = scaleProfile();
        return Math.max(FIELD_TEXT_MIN, Math.min(FIELD_TEXT_MAX + 120, profile.fieldTextWidth()));
    }

    public static int responsiveBodyTextWidth() {
        UiScaleProfile profile = scaleProfile();
        return Math.max(BODY_TEXT_MIN, Math.min(BODY_TEXT_MAX + 200, profile.bodyTextWidth()));
    }

    public static int responsiveSquareSize(double widthRatio, double heightRatio, int min, int max) {
        Minecraft minecraft = Minecraft.getInstance();
        int scaledWidth = minecraft.getWindow().getGuiScaledWidth();
        int scaledHeight = minecraft.getWindow().getGuiScaledHeight();
        int widthBased = (int) Math.round(scaledWidth * widthRatio);
        int heightBased = (int) Math.round(scaledHeight * heightRatio);
        int responsive = Math.min(widthBased, heightBased);
        return Math.max(min, Math.min(max, responsive));
    }

    public static UiScaleProfile scaleProfile() {
        return UiScaleService.profile();
    }

    public static int scaledPixels(int basePixels) {
        if (basePixels <= 0) {
            return 0;
        }
        return Math.max(1, (int) Math.round(basePixels * scaleProfile().scale()));
    }

    public static Sizing fixed(int basePixels) {
        return Sizing.fixed(scaledPixels(basePixels));
    }

    public static Sizing fixedClamped(int basePixels, int minPixels, int maxPixels) {
        int scaled = scaledPixels(basePixels);
        return Sizing.fixed(Math.max(minPixels, Math.min(maxPixels, scaled)));
    }

    public static int scaledScrollbarThickness(int baseThickness) {
        int scaled = scaledPixels(baseThickness);
        return Math.max(6, Math.min(14, scaled));
    }

    public static int scaledScrollStep(int baseStep) {
        int scaled = scaledPixels(baseStep);
        return Math.max(8, Math.min(32, scaled));
    }

    public static int scrollContentInset(int scrollbarBaseThickness) {
        int safety = Math.max(2, scaledPixels(4));
        return scaledScrollbarThickness(scrollbarBaseThickness) + safety;
    }

    public static FlowLayout scrollContentColumn(int scrollbarBaseThickness) {
        return scrollContentColumn(scrollbarBaseThickness, Math.max(2, scaledPixels(4)));
    }

    public static FlowLayout scrollContentColumn(int scrollbarBaseThickness, int bottomPadding) {
        FlowLayout content = column();
        // Insets order is (top, bottom, left, right). Keep left flush and reserve space on the right for scrollbar.
        content.padding(Insets.of(0, Math.max(0, bottomPadding), 0, scrollContentInset(scrollbarBaseThickness)));
        return content;
    }

    private static ButtonComponent actionButton(Component label, boolean enabled, Runnable onPress) {
        boolean removeAction = label != null && label.getString().equalsIgnoreCase(ItemEditorText.str("common.remove"));
        ButtonComponent button = button(
                label,
                removeAction ? ButtonTextPreset.STANDARD : ButtonTextPreset.COMPACT,
                component -> onPress.run()
        );
        boolean compactActionRow = useCompactActionRow();
        if (compactActionRow) {
            button.horizontalSizing(Sizing.fill(100));
        } else if (removeAction) {
            int removeWidth = Math.max(REMOVE_ACTION_WIDTH_MIN, scaledPixels(REMOVE_ACTION_WIDTH_BASE));
            button.horizontalSizing(Sizing.fixed(removeWidth));
        }
        button.active(enabled);
        return button;
    }

    private static boolean useCompactActionRow() {
        var window = Minecraft.getInstance().getWindow();
        return window.getGuiScale() >= COMPACT_ACTION_ROW_SCALE_THRESHOLD
                || window.getGuiScaledWidth() < COMPACT_ACTION_ROW_WIDTH_THRESHOLD;
    }

    private static ButtonComponent createAdaptiveButton(Component text, float preferredScale, ButtonPreset preset, Consumer<ButtonComponent> onPress) {
        Component safeText = text == null ? Component.empty() : text;
        int horizontalPadding = Math.max(8, scaledPixels(preset.horizontalPadding));
        int minWidth = Math.max(16, scaledPixels(preset.minWidth));
        int viewportBound = Math.max(minWidth, (int) Math.round(guiScaledWidth() * 0.28d));
        int maxWidth = Math.max(minWidth, Math.min(scaledPixels(320), viewportBound));
        // Button labels are rendered by vanilla at native scale in this UI.
        // Keep adaptive fit based on actual rendered width to avoid tiny/truncated labels.
        float normalizedScale = Math.max(BUTTON_TEXT_MIN_SCALE, Math.min(BUTTON_TEXT_MAX_SCALE, preferredScale));
        int adaptivePadding = horizontalPadding + (normalizedScale > 1.10F ? scaledPixels(2) : 0);
        String fullLabel = safeText.getString();
        int fittedTextBudget = Math.max(8, maxWidth - adaptivePadding);
        Component displayText = fitToWidth(safeText, fittedTextBudget);
        ButtonComponent button = UIComponents.button(displayText, onPress);
        int renderedTextWidth = Math.max(1, Minecraft.getInstance().font.width(displayText.getString()));
        button.horizontalSizing(Sizing.fixed(
                Math.max(
                        minWidth,
                        Math.min(maxWidth, renderedTextWidth + adaptivePadding)
                )
        ));
        int controlHeight = Math.max(scaleProfile().controlHeight(), scaledPixels(preset.minHeight));
        button.verticalSizing(Sizing.fixed(controlHeight));
        applyButtonPreset(button, preset);
        if (!displayText.getString().equals(fullLabel)) {
            button.tooltip(java.util.List.of(safeText));
        }
        return button;
    }

    private static LabelComponent styledText(Component text, TextPreset preset) {
        return styledText(text, preset, 1.0F);
    }

    private static LabelComponent styledText(Component text, TextPreset preset, float scaleFactor) {
        UiScaleProfile profile = scaleProfile();
        int bodyLineSpacing = Math.max(0, profile.bodyLineSpacing());
        float normalizedFactor = Math.max(0.5F, Math.min(2.0F, scaleFactor));
        float textScale = Math.max(0.5F, Math.min(2.0F, baseScaleForPreset(preset) * normalizedFactor));
        LabelComponent label = new ScaledLabelComponent(text).textScale(textScale);
        return switch (preset) {
            case TITLE -> label
                    .lineHeight(Math.max(6, profile.titleLineHeight()))
                    .lineSpacing(bodyLineSpacing)
                    .color(Color.ofRgb(0xF2F5F8))
                    .shadow(true);
            case CAPTION -> label
                    .lineHeight(Math.max(6, profile.captionLineHeight()))
                    .lineSpacing(bodyLineSpacing)
                    .color(Color.ofRgb(0xA9B5C0));
            default -> label
                    .lineHeight(Math.max(6, profile.bodyLineHeight()))
                    .lineSpacing(bodyLineSpacing)
                    .color(Color.ofRgb(0xF2F5F8));
        };
    }

    private static float baseScaleForPreset(TextPreset preset) {
        UiScaleProfile profile = scaleProfile();
        return switch (preset) {
            case TITLE -> profile.titleTextScale() * preset.scale;
            case CAPTION -> profile.captionTextScale() * preset.scale;
            default -> profile.bodyTextScale() * preset.scale;
        };
    }

    private static float buttonTextScale(TextPreset preset) {
        return Math.max(BUTTON_TEXT_MIN_SCALE, Math.min(BUTTON_TEXT_MAX_SCALE, baseScaleForPreset(preset)));
    }

    private static Component safeMessageText(Component text) {
        if (text == null || text.getString().isEmpty()) {
            return Component.literal(BLANK_TEXT);
        }
        return text;
    }

    private static int guiScaledWidth() {
        return Minecraft.getInstance().getWindow().getGuiScaledWidth();
    }

    // Note: button text uses vanilla renderer for consistent readability and avoids custom overlay artifacts.
}
