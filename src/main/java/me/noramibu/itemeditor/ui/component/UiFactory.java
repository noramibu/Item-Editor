package me.noramibu.itemeditor.ui.component;

import com.mojang.blaze3d.platform.InputConstants;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.CheckboxComponent;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.component.TextAreaComponent;
import io.wispforest.owo.ui.component.TextBoxComponent;
import io.wispforest.owo.ui.component.UIComponents;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.StackLayout;
import io.wispforest.owo.ui.container.UIContainers;
import io.wispforest.owo.ui.core.Color;
import io.wispforest.owo.ui.core.HorizontalAlignment;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.Surface;
import io.wispforest.owo.ui.core.UIComponent;
import io.wispforest.owo.ui.core.VerticalAlignment;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.function.Consumer;
import me.noramibu.itemeditor.editor.ItemEditorFieldReset;
import me.noramibu.itemeditor.ui.scale.UiScaleProfile;
import me.noramibu.itemeditor.ui.scale.UiScaleService;
import me.noramibu.itemeditor.ui.util.UiColors;
import me.noramibu.itemeditor.util.ItemEditorText;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.inventory.MenuType;

public final class UiFactory {
    public static List<Component> tooltipLines(Component text, int width) {
        return Minecraft.getInstance().font.getSplitter().splitLines(text, Math.max(80, width), Style.EMPTY).stream()
                .map(line -> {
                    MutableComponent result = Component.empty();
                    line.visit(
                            (style, part) -> {
                                result.append(Component.literal(part).setStyle(style));
                                return Optional.empty();
                            },
                            Style.EMPTY);
                    return (Component) result;
                })
                .toList();
    }

    private static final Map<UIComponent, String> FIELD_KEYS = new WeakHashMap<>();

    public static <T extends UIComponent> T bindField(Component label, T input) {
        if (label.getContents() instanceof TranslatableContents text && ItemEditorFieldReset.supports(text.getKey())) {
            FIELD_KEYS.put(input, text.getKey());
            persistTextHistory(text.getKey(), input);
        }
        return input;
    }

    public static String fieldKey(UIComponent input) {
        return FIELD_KEYS.get(input);
    }

    static <T extends UIComponent> T persistTextHistory(String key, T input) {
        if (input instanceof UndoableTextBoxComponent textBox) {
            textBox.persistHistory(Minecraft.getInstance().gui.screen(), key);
        }
        return input;
    }

    public static Component withChangedMarker(Component original) {
        return original.copy()
                .withStyle(ChatFormatting.ITALIC)
                .append(Component.literal(" (*)").withStyle(style -> style.withColor(ChatFormatting.YELLOW)
                        .withItalic(false)));
    }

    private static final int UNBOUNDED_TEXT_LIMIT = Integer.MAX_VALUE;

    private static final Surface CARD_SURFACE = Surface.flat(0xAA141A22).and(Surface.outline(0xFF2F3945));
    private static final Surface SUB_CARD_SURFACE = Surface.flat(0xAA1B222B).and(Surface.outline(0xFF414B56));
    private static final Surface EDITOR_FRAME_SURFACE = Surface.flat(0xB0121822).and(Surface.outline(0xFF3F4D63));
    private static final int FIELD_TEXT_MIN = 120;
    private static final int FIELD_TEXT_MAX = 300;
    private static final int BODY_TEXT_MIN = 130;
    private static final int BODY_TEXT_MAX = 320;
    private static final float BUTTON_TEXT_MIN_SCALE = 0.95F;
    private static final float BUTTON_TEXT_MAX_SCALE = 1.55F;
    private static final String BLANK_TEXT = " ";
    private static final String SYMBOL_SECTION_COLLAPSED = "+";
    private static final String SYMBOL_SECTION_EXPANDED = "-";
    private static final int REMOVE_ACTION_WIDTH_MIN = 88;
    private static final int REMOVE_ACTION_WIDTH_BASE = 108;
    private static final int ACTION_POSITIVE_COLOR = 0x78D982;
    private static final int ACTION_NEGATIVE_COLOR = 0xFF8A8A;
    private static final int ACTION_PICKER_COLOR = UiColors.PICKER;

    public enum ActionTone {
        POSITIVE,
        NEGATIVE,
        PICKER,
        EDIT,
        NEUTRAL
    }

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

    private UiFactory() {}

    public static FlowLayout column() {
        UiScaleProfile profile = scaleProfile();
        return baseFlow(profile, false);
    }

    public static FlowLayout row() {
        UiScaleProfile profile = scaleProfile();
        FlowLayout row = baseFlow(profile, true);
        row.mouseDown().subscribe((click, doubled) -> {
            if (click.button() != InputConstants.MOUSE_BUTTON_LEFT) return false;
            for (UIComponent child : row.children()) {
                if (!(child instanceof LabelComponent)
                        && child.isInBoundingBox(row.x() + click.x(), row.y() + click.y())) return false;
            }
            for (UIComponent child : row.children()) {
                Runnable toggle = COLLAPSE_ACTIONS.get(child);
                if (toggle != null) {
                    toggle.run();
                    return true;
                }
            }
            return false;
        });
        return row;
    }

    private static final Map<UIComponent, Runnable> COLLAPSE_ACTIONS = new WeakHashMap<>();

    public static void appendFillChild(FlowLayout parent, UIComponent child) {
        child.horizontalSizing(Sizing.fill(100));
        if (child instanceof FlowLayout flowLayout) {
            applyFlowContract(flowLayout);
        }
        parent.child(child);
    }

    public static FlowLayout horizontalScrollbarRow(RichTextAreaComponent editor, int gutterWidth) {
        FlowLayout scrollbarRow = row();
        scrollbarRow.horizontalSizing(Sizing.fill(100));
        scrollbarRow.gap(0);
        FlowLayout gutterSpacer = row();
        gutterSpacer.horizontalSizing(fixed(gutterWidth));
        scrollbarRow.child(gutterSpacer);
        scrollbarRow.child(new RichTextHorizontalScrollbarComponent(Sizing.fill(100), editor));
        return scrollbarRow;
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
        field.child(bindField(label, input).horizontalSizing(Sizing.fill(100)));
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
        return styledText(text, TextPreset.CAPTION).color(Color.ofRgb(0xA9B5C0)).maxWidth(maxWidth);
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
        return createAdaptiveButton(
                semanticallyTintActionText(text), buttonTextScale(preset.textPreset), preset.buttonPreset, onPress);
    }

    public static ButtonComponent positiveButton(
            Component text, ButtonTextPreset preset, Consumer<ButtonComponent> onPress) {
        return actionToneButton(text, preset, ActionTone.POSITIVE, onPress);
    }

    public static ButtonComponent negativeButton(
            Component text, ButtonTextPreset preset, Consumer<ButtonComponent> onPress) {
        return actionToneButton(text, preset, ActionTone.NEGATIVE, onPress);
    }

    public static ButtonComponent actionToneButton(
            Component text, ButtonTextPreset preset, ActionTone tone, Consumer<ButtonComponent> onPress) {
        return button(tintedActionText(text, tone), preset, onPress);
    }

    public static ButtonComponent actionRowButton(
            Component text, ButtonTextPreset preset, ActionTone tone, Consumer<ButtonComponent> onPress) {
        ButtonComponent button = new ScrollingButtonComponent(tintedActionText(text, tone), onPress);
        button.tooltip(List.of(text));
        int controlHeight = Math.max(scaleProfile().controlHeight(), scaledPixels(preset.buttonPreset.minHeight));
        button.verticalSizing(Sizing.fixed(controlHeight));
        applyButtonPreset(button, preset.buttonPreset);
        return button;
    }

    public static FlowLayout actionButtonRow(ButtonComponent... buttons) {
        return actionButtonRow(true, buttons);
    }

    public static FlowLayout actionButtonRow(boolean stackWhenNarrow, ButtonComponent... buttons) {
        return actionButtonLayout(!stackWhenNarrow, true, buttons);
    }

    public static ButtonComponent collapseAllButton(boolean anyCollapsed, Consumer<Boolean> setCollapsed) {
        return button(
                ItemEditorText.tr(anyCollapsed ? "common.expand_all" : "common.collapse_all"),
                ButtonTextPreset.STANDARD,
                button -> setCollapsed.accept(!anyCollapsed));
    }

    public static FlowLayout packedActionButtonRow(ButtonComponent... buttons) {
        return actionButtonLayout(false, false, buttons);
    }

    private static FlowLayout actionButtonLayout(boolean forceSingleRow, boolean fillRows, ButtonComponent... buttons) {
        List<ButtonComponent> present = buttons == null
                ? List.of()
                : Arrays.stream(buttons).filter(Objects::nonNull).toList();
        if (present.isEmpty()) {
            return row();
        }
        if (present.stream().allMatch(button -> itemActionOrder(button.getMessage()) >= 0)) {
            present = present.stream()
                    .sorted(Comparator.comparingInt(button -> itemActionOrder(button.getMessage())))
                    .toList();
        }
        return new PackedActionLayout(present, Math.max(1, scaleProfile().tightSpacing()), forceSingleRow, fillRows);
    }

    public static ButtonComponent scaledTextButton(
            Component fullText, float textScale, ButtonTextPreset preset, Consumer<ButtonComponent> onPress) {
        float preferredScale = Math.clamp(textScale, BUTTON_TEXT_MIN_SCALE, BUTTON_TEXT_MAX_SCALE);
        return createAdaptiveButton(fullText, preferredScale, preset.buttonPreset, onPress);
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

    public static ButtonComponent pickerButton(Component text, int width, Consumer<ButtonComponent> onPress) {
        ButtonComponent button = button(text, ButtonTextPreset.STANDARD, onPress);
        if (width > 0) {
            button.horizontalSizing(fixed(width));
        } else {
            button.horizontalSizing(Sizing.fill(100));
        }
        return button;
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
            Runnable onRemove) {
        FlowLayout card = subCard();
        card.child(reorderableHeader(title, canMoveUp, onMoveUp, canMoveDown, onMoveDown, onRemove));
        return card;
    }

    public static FlowLayout reorderableCollapsibleSubCard(
            Component title,
            Component summary,
            int summaryMaxWidth,
            boolean collapsed,
            Runnable onToggle,
            boolean canMoveUp,
            Runnable onMoveUp,
            boolean canMoveDown,
            Runnable onMoveDown,
            Runnable onRemove) {
        FlowLayout card = subCard();
        card.child(reorderableCollapsibleHeader(
                title,
                summary,
                summaryMaxWidth,
                collapsed,
                onToggle,
                canMoveUp,
                onMoveUp,
                canMoveDown,
                onMoveDown,
                onRemove));
        return card;
    }

    public static MenuType<?> chestMenuType(int rows) {
        return switch (rows) {
            case 1 -> MenuType.GENERIC_9x1;
            case 2 -> MenuType.GENERIC_9x2;
            case 3 -> MenuType.GENERIC_9x3;
            case 4 -> MenuType.GENERIC_9x4;
            case 5 -> MenuType.GENERIC_9x5;
            default -> MenuType.GENERIC_9x6;
        };
    }

    public static void addPackedRows(FlowLayout parent, int perRow, UIComponent... components) {
        for (int index = 0; index < components.length; index += perRow) {
            FlowLayout row = UiFactory.row();
            int rowEnd = Math.min(components.length, index + perRow);
            for (int componentIndex = index; componentIndex < rowEnd; componentIndex++) {
                row.child(components[componentIndex].horizontalSizing(Sizing.expand(100 / (rowEnd - index))));
            }
            parent.child(row);
        }
    }

    public static FlowLayout reorderableHeader(
            Component title,
            boolean canMoveUp,
            Runnable onMoveUp,
            boolean canMoveDown,
            Runnable onMoveDown,
            Runnable onRemove) {
        FlowLayout header = column().gap(Math.max(1, scaleProfile().tightSpacing()));
        FlowLayout titleRow = row();
        titleRow.child(title(title).shadow(false).horizontalSizing(Sizing.expand(100)));
        header.child(titleRow);

        addReorderActions(header, canMoveUp, onMoveUp, canMoveDown, onMoveDown, onRemove);
        return header;
    }

    public static FlowLayout reorderableCollapsibleHeader(
            Component title,
            Component summary,
            int summaryMaxWidth,
            boolean collapsed,
            Runnable onToggle,
            boolean canMoveUp,
            Runnable onMoveUp,
            boolean canMoveDown,
            Runnable onMoveDown,
            Runnable onRemove) {
        FlowLayout header = column().gap(Math.max(1, scaleProfile().tightSpacing()));
        header.child(collapsibleHeader(title(title).shadow(false), collapsed, onToggle));
        header.child(muted(summary, summaryMaxWidth));

        addReorderActions(header, canMoveUp, onMoveUp, canMoveDown, onMoveDown, onRemove);
        return header;
    }

    private static void addReorderActions(
            FlowLayout header,
            boolean canMoveUp,
            Runnable onMoveUp,
            boolean canMoveDown,
            Runnable onMoveDown,
            Runnable onRemove) {
        FlowLayout actionRow = entryActions(
                canMoveUp,
                onMoveUp,
                canMoveDown,
                onMoveDown,
                null,
                onRemove == null ? null : actionButton(ItemEditorText.tr("common.remove"), true, onRemove));
        if (!actionRow.children().isEmpty()) {
            header.child(actionRow);
        }
    }

    public static FlowLayout collapsibleHeader(UIComponent title, boolean collapsed, Runnable onToggle) {
        return row().child(title.horizontalSizing(Sizing.expand(100))).child(collapseToggleButton(collapsed, onToggle));
    }

    public static FlowLayout entryActions(
            boolean canMoveUp,
            Runnable onMoveUp,
            boolean canMoveDown,
            Runnable onMoveDown,
            Runnable onDuplicate,
            ButtonComponent... trailing) {
        ButtonComponent[] buttons = new ButtonComponent[3 + trailing.length];
        buttons[0] = onMoveUp == null ? null : actionButton(ItemEditorText.tr("common.up"), canMoveUp, onMoveUp);
        buttons[1] =
                onMoveDown == null ? null : actionButton(ItemEditorText.tr("common.down"), canMoveDown, onMoveDown);
        buttons[2] =
                onDuplicate == null ? null : actionButton(ItemEditorText.tr("common.duplicate"), true, onDuplicate);
        System.arraycopy(trailing, 0, buttons, 3, trailing.length);
        return actionButtonRow(buttons);
    }

    public static ButtonComponent collapseToggleButton(boolean collapsed, Runnable onToggle) {
        ButtonComponent collapseToggle = button(
                Component.literal(collapsed ? SYMBOL_SECTION_COLLAPSED : SYMBOL_SECTION_EXPANDED),
                ButtonTextPreset.COMPACT,
                button -> onToggle.run());
        collapseToggle.horizontalSizing(Sizing.fixed(scaledPixels(16)));
        COLLAPSE_ACTIONS.put(collapseToggle, onToggle);
        return collapseToggle;
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
            return Component.literal(ellipsis).withStyle(text.getStyle());
        }

        MutableComponent shortened = Component.empty();
        int[] remainingWidth = {Math.max(0, maxPixelWidth - ellipsisWidth)};
        appendFittedText(shortened, text, Style.EMPTY, remainingWidth);
        if (shortened.getString().isBlank()) {
            return Component.literal(ellipsis).withStyle(text.getStyle());
        }
        shortened.append(Component.literal(ellipsis).withStyle(text.getStyle()));
        return shortened;
    }

    private static void appendFittedText(
            MutableComponent output, Component component, Style parentStyle, int[] remainingWidth) {
        if (component == null || remainingWidth[0] <= 0) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Style effectiveStyle = component.getStyle().applyTo(parentStyle);
        component.getContents().visit((String chunk) -> {
            if (remainingWidth[0] <= 0 || chunk.isEmpty()) {
                return Optional.empty();
            }

            int chunkWidth = minecraft.font.width(chunk);
            if (chunkWidth <= remainingWidth[0]) {
                output.append(Component.literal(chunk).withStyle(effectiveStyle));
                remainingWidth[0] -= chunkWidth;
                return Optional.empty();
            }

            String partial = minecraft.font.plainSubstrByWidth(chunk, remainingWidth[0]);
            if (!partial.isEmpty()) {
                output.append(Component.literal(partial).withStyle(effectiveStyle));
            }
            remainingWidth[0] = 0;
            return Optional.empty();
        });

        for (Component sibling : component.getSiblings()) {
            appendFittedText(output, sibling, effectiveStyle, remainingWidth);
        }
    }

    public static TextBoxComponent textBox(String value, Consumer<String> onChanged) {
        UndoableTextBoxComponent box = new UndoableTextBoxComponent(Sizing.fill(100));
        box.setMaxLength(UNBOUNDED_TEXT_LIMIT);
        box.text(value == null ? "" : value);
        box.clearHistory();
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
        CheckboxComponent checkbox = UIComponents.checkbox(text);
        checkbox.verticalSizing(Sizing.fixed(scaleProfile().controlHeight()));
        checkbox.checked(checked);
        checkbox.onChanged(onChanged);
        if (!text.getString().isBlank()) {
            checkbox.tooltip(List.of(text));
        }
        return checkbox;
    }

    public static FlowLayout centeredCard(int width) {
        FlowLayout card = applyFlowContract(new BoundedDialogLayout(DialogUiUtil.dialogWidth(width)));
        card.gap(scaleProfile().spacing());
        card.padding(Insets.of(scaleProfile().padding()));
        card.surface(CARD_SURFACE);
        card.horizontalAlignment(HorizontalAlignment.CENTER);
        return card;
    }

    public static void centerInRoot(StackLayout root, UIComponent child, int padding) {
        FlowLayout centered = column();
        centered.horizontalSizing(Sizing.fill(100));
        centered.verticalSizing(Sizing.fill(100));
        centered.padding(Insets.of(padding));
        centered.horizontalAlignment(HorizontalAlignment.CENTER);
        centered.verticalAlignment(VerticalAlignment.CENTER);
        centered.child(child);
        root.child(centered);
    }

    public static FlowLayout framedEditorCard() {
        FlowLayout frame = subCard();
        frame.padding(Insets.of(5));
        frame.surface(EDITOR_FRAME_SURFACE);
        return frame;
    }

    public static int responsiveFieldTextWidth() {
        UiScaleProfile profile = scaleProfile();
        return Math.clamp(profile.fieldTextWidth(), FIELD_TEXT_MIN, FIELD_TEXT_MAX + 120);
    }

    public static int responsiveBodyTextWidth() {
        UiScaleProfile profile = scaleProfile();
        return Math.clamp(profile.bodyTextWidth(), BODY_TEXT_MIN, BODY_TEXT_MAX + 200);
    }

    public static int responsiveSquareSize(
            int availableWidth, int availableHeight, double widthRatio, double heightRatio, int min, int max) {
        int widthBased = (int) Math.round(availableWidth * widthRatio);
        int heightBased = (int) Math.round(availableHeight * heightRatio);
        int responsive = Math.min(widthBased, heightBased);
        return Math.clamp(responsive, min, max);
    }

    public static UiScaleProfile scaleProfile() {
        return UiScaleService.profile();
    }

    public static int scaledPixels(int basePixels) {
        return Math.max(0, basePixels);
    }

    public static Sizing fixed(int basePixels) {
        return Sizing.fixed(scaledPixels(basePixels));
    }

    public static int scaledScrollbarThickness(int baseThickness) {
        int scaled = scaledPixels(baseThickness);
        return Math.clamp(scaled, 6, 14);
    }

    public static int scaledScrollStep(int baseStep) {
        int scaled = scaledPixels(baseStep);
        return Math.clamp(scaled, 8, 32);
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
        content.padding(Insets.of(0, Math.max(0, bottomPadding), 0, scrollContentInset(scrollbarBaseThickness)));
        return content;
    }

    private static ButtonComponent actionButton(Component label, boolean enabled, Runnable onPress) {
        boolean removeAction = label != null && label.getString().equalsIgnoreCase(ItemEditorText.str("common.remove"));
        ButtonComponent button = actionToneButton(
                label,
                removeAction ? ButtonTextPreset.STANDARD : ButtonTextPreset.COMPACT,
                removeAction ? ActionTone.NEGATIVE : ActionTone.NEUTRAL,
                component -> onPress.run());
        if (removeAction) {
            int removeWidth = Math.max(REMOVE_ACTION_WIDTH_MIN, scaledPixels(REMOVE_ACTION_WIDTH_BASE));
            button.horizontalSizing(Sizing.fixed(removeWidth));
        }
        button.active(enabled);
        return button;
    }

    private static Component tintedActionText(Component text, ActionTone tone) {
        Component safeText = text == null ? Component.empty() : text;
        if (tone == null || tone == ActionTone.NEUTRAL) {
            return safeText;
        }
        int color =
                switch (tone) {
                    case POSITIVE -> ACTION_POSITIVE_COLOR;
                    case NEGATIVE -> ACTION_NEGATIVE_COLOR;
                    case PICKER -> ACTION_PICKER_COLOR;
                    case EDIT -> 0xFFFF55;
                    case NEUTRAL -> throw new IllegalStateException("Neutral action tone should not be tinted");
                };
        return safeText.copy().withColor(color);
    }

    private static Component semanticallyTintActionText(Component text) {
        ActionTone tone = inferActionTone(text);
        return tone == ActionTone.NEUTRAL ? text : tintedActionText(text, tone);
    }

    private static ActionTone inferActionTone(Component text) {
        if (text == null || text.getStyle().getColor() != null) {
            return ActionTone.NEUTRAL;
        }
        int action = itemActionOrder(text);
        if (action >= 0) {
            return switch (action) {
                case 0 -> ActionTone.EDIT;
                case 1 -> ActionTone.PICKER;
                default -> ActionTone.NEGATIVE;
            };
        }
        String label = text.getString().trim().toLowerCase(Locale.ROOT);
        if (label.equals("edit") || label.startsWith("edit ")) return ActionTone.EDIT;
        if (label.equals("+")
                || label.equals("true")
                || label.equals("add")
                || label.startsWith("add ")
                || label.startsWith("create")
                || label.startsWith("import")
                || label.startsWith("paste")) {
            return ActionTone.POSITIVE;
        }
        if (label.equals("-")
                || label.equals("x")
                || label.equals("false")
                || label.equals("remove")
                || label.startsWith("remove ")
                || label.startsWith("clear")
                || label.startsWith("reset")
                || label.startsWith("delete")
                || label.startsWith("discard")) {
            return ActionTone.NEGATIVE;
        }
        if (label.equals("pick")
                || label.startsWith("pick ")
                || label.equals("select")
                || label.startsWith("select ")
                || label.equals("choose")
                || label.startsWith("choose ")
                || label.equals("browse")
                || label.startsWith("browse ")
                || label.equals("open")
                || label.startsWith("open ")) {
            return ActionTone.PICKER;
        }
        return ActionTone.NEUTRAL;
    }

    private static int itemActionOrder(Component text) {
        if (text == null) return -1;
        if (text.getContents() instanceof TranslatableContents contents) {
            String key = contents.getKey();
            if (key.startsWith("itemeditor.")) {
                if (key.endsWith(".edit") || key.endsWith(".edit_stack")) return 0;
                if (key.endsWith(".pick")
                        || key.equals("itemeditor.common.pick_item_list")
                        || key.equals("itemeditor.common.pick_storage")) return 1;
                if (key.endsWith(".remove")) return 2;
            }
        }
        return -1;
    }

    private static ButtonComponent createAdaptiveButton(
            Component text, float preferredScale, ButtonPreset preset, Consumer<ButtonComponent> onPress) {
        Component safeText = text == null ? Component.empty() : text;
        int horizontalPadding = Math.max(8, scaledPixels(preset.horizontalPadding));
        int minWidth = Math.max(16, scaledPixels(preset.minWidth));
        float normalizedScale = Math.clamp(preferredScale, BUTTON_TEXT_MIN_SCALE, BUTTON_TEXT_MAX_SCALE);
        int adaptivePadding = horizontalPadding + (normalizedScale > 1.10F ? scaledPixels(2) : 0);
        ButtonComponent button = new ScrollingButtonComponent(safeText, onPress);
        if (!safeText.getString().isBlank()) {
            button.tooltip(List.of(safeText));
        }
        int renderedTextWidth = Math.max(1, Minecraft.getInstance().font.width(safeText));
        button.horizontalSizing(Sizing.fixed(Math.max(minWidth, renderedTextWidth + adaptivePadding)));
        int controlHeight = Math.max(scaleProfile().controlHeight(), scaledPixels(preset.minHeight));
        button.verticalSizing(Sizing.fixed(controlHeight));
        applyButtonPreset(button, preset);
        return button;
    }

    public static ButtonComponent fixedWidthButton(
            Component text, ButtonTextPreset preset, int width, Consumer<ButtonComponent> onPress) {
        ButtonComponent button = button(text, preset, onPress);
        applyFixedButtonLabel(button, text, width);
        return button;
    }

    public static void applyFixedButtonLabel(ButtonComponent button, Component text, int width) {
        button.setMessage(semanticallyTintActionText(text));
        button.horizontalSizing(Sizing.fixed(Math.max(1, width)));
        if (!text.getString().isBlank()) {
            button.tooltip(List.of(text));
        }
    }

    private static LabelComponent styledText(Component text, TextPreset preset) {
        return styledText(text, preset, 1.0F);
    }

    private static LabelComponent styledText(Component text, TextPreset preset, float scaleFactor) {
        UiScaleProfile profile = scaleProfile();
        int bodyLineSpacing = Math.max(0, profile.bodyLineSpacing());
        float normalizedFactor = Math.clamp(scaleFactor, 0.5F, 2.0F);
        float textScale = Math.clamp(baseScaleForPreset(preset) * normalizedFactor, 0.5F, 2.0F);
        LabelComponent label = new ScaledLabelComponent(text).textScale(textScale);
        return switch (preset) {
            case TITLE ->
                label.lineHeight(Math.max(6, profile.titleLineHeight()))
                        .lineSpacing(bodyLineSpacing)
                        .color(Color.ofRgb(0xF2F5F8))
                        .shadow(true);
            case CAPTION ->
                label.lineHeight(Math.max(6, profile.captionLineHeight()))
                        .lineSpacing(bodyLineSpacing)
                        .color(Color.ofRgb(0xA9B5C0));
            default ->
                label.lineHeight(Math.max(6, profile.bodyLineHeight()))
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
        return Math.clamp(baseScaleForPreset(preset), BUTTON_TEXT_MIN_SCALE, BUTTON_TEXT_MAX_SCALE);
    }

    private static Component safeMessageText(Component text) {
        if (text == null || text.getString().isEmpty()) {
            return Component.literal(BLANK_TEXT);
        }
        return text;
    }
}
