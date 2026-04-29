package me.noramibu.itemeditor.ui.panel.specialdata;

import io.wispforest.owo.ui.component.BoxComponent;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.ItemComponent;
import io.wispforest.owo.ui.component.UIComponents;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.container.UIContainers;
import io.wispforest.owo.ui.core.Color;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.Sizing;
import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.ui.component.DyeColorSelectorSection;
import me.noramibu.itemeditor.ui.component.InputSafeScrollContainer;
import me.noramibu.itemeditor.ui.component.PickerFieldFactory;
import me.noramibu.itemeditor.ui.component.RotatableItemPreviewComponent;
import me.noramibu.itemeditor.ui.component.UiFactory;
import me.noramibu.itemeditor.ui.util.LayoutModeUtil;
import me.noramibu.itemeditor.util.ItemEditorText;
import me.noramibu.itemeditor.util.RegistryUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.BannerPatternLayers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class BannerSpecialDataSection {
    private static final int COMPACT_LAYOUT_WIDTH_THRESHOLD = 620;

    private static final int PATTERN_BUTTON_WIDTH = 280;
    private static final int LAYER_PREVIEW_WIDTH = 84;
    private static final int LAYER_PREVIEW_WIDTH_NARROW = 72;
    private static final int LAYER_STRIP_HEIGHT = 64;
    private static final int LAYER_STRIP_HEIGHT_NARROW = 56;
    private static final int LAYER_STRIP_EMPTY_HINT_WIDTH = 260;
    private static final int LAYER_STRIP_SCROLLBAR_THICKNESS = 8;
    private static final int LAYER_STRIP_SCROLL_STEP = 14;
    private static final int LAYER_STAGE_LABEL_WIDTH = 46;
    private static final int LAYER_ITEM_PREVIEW_SIZE = 28;
    private static final int LAYER_ITEM_PREVIEW_SIZE_NARROW = 24;
    private static final int PREVIEW_HINT_WIDTH_NARROW = 180;
    private static final int PREVIEW_HINT_WIDTH_WIDE = 240;
    private static final int PREVIEW_SIZE_NARROW_MAX = 144;
    private static final int NARROW_LAYOUT_WIDTH_THRESHOLD = 980;
    private static final int LAYER_COLOR_CHIP_SIZE = 10;
    private static final int DRAG_HINT_WIDTH = 300;
    private static final int ACTION_BUTTON_WIDTH_BASE = 96;
    private static final int ACTION_BUTTON_WIDTH_MIN = 72;
    private static final int ACTION_BUTTON_WIDTH_MAX = 136;
    private static final int ACTION_BUTTON_TEXT_MIN = 20;
    private static final int ACTION_BUTTON_TEXT_RESERVE = 10;

    private BannerSpecialDataSection() {
    }

    public static boolean supports(ItemStack stack) {
        return stack.has(DataComponents.BANNER_PATTERNS)
                || stack.is(Items.WHITE_BANNER)
                || stack.is(Items.SHIELD)
                || stack.getItem() instanceof BannerItem;
    }

    public static FlowLayout build(SpecialDataPanelContext context) {
        ItemEditorState.SpecialData special = context.special();
        boolean compactLayout = isCompactLayout(context);
        if (special.draggingBannerLayer < 0 || special.draggingBannerLayer >= special.bannerLayers.size()) {
            special.draggingBannerLayer = -1;
        }
        FlowLayout section = UiFactory.section(
                ItemEditorText.tr(isShieldContext(context) ? "special.banner.shield_title" : "special.banner.title"),
                Component.empty()
        );
        List<String> availablePatterns = availablePatternIds(context, special);

        section.child(buildFinalPreviewCard(context, special));
        section.child(buildLayerPreviewStrip(context, special));
        section.child(buildBaseColorEditor(context, special));

        Component addLayerText = ItemEditorText.tr("special.banner.add_layer");
        section.child(boundedActionButton(addLayerText, compactLayout, () ->
                context.mutateRefresh(() -> {
                    if (isShieldContext(context) && special.bannerBaseColor.isBlank()) {
                        special.bannerBaseColor = DyeColor.WHITE.name();
                    }
                    special.draggingBannerLayer = -1;
                    ItemEditorState.BannerLayerDraft draft = new ItemEditorState.BannerLayerDraft();
                    draft.color = DyeColor.WHITE.name();
                    special.bannerLayers.add(draft);
                })
        ));

        if (special.draggingBannerLayer >= 0) {
            FlowLayout dragRow = compactLayout ? UiFactory.column() : UiFactory.row();
            dragRow.child(UiFactory.muted(ItemEditorText.tr(
                    "special.banner.dragging",
                    special.draggingBannerLayer + 1
            ), DRAG_HINT_WIDTH));
            Component dragCancelText = ItemEditorText.tr("special.banner.drag_cancel");
            dragRow.child(boundedActionButton(dragCancelText, compactLayout, () ->
                    context.mutateRefresh(() -> special.draggingBannerLayer = -1)
            ));
            section.child(dragRow);
        }

        for (int index = 0; index < special.bannerLayers.size(); index++) {
            section.child(buildLayerCard(context, special, availablePatterns, index));
        }
        return section;
    }

    private static FlowLayout buildFinalPreviewCard(SpecialDataPanelContext context, ItemEditorState.SpecialData special) {
        FlowLayout card = UiFactory.subCard();
        card.child(UiFactory.title(ItemEditorText.tr("screen.preview")).shadow(false));

        ItemStack finalPreview = buildFinalPreviewStack(context, special);
        int previewSize = previewSize();
        FlowLayout previewRow = isNarrowLayout() ? UiFactory.column() : UiFactory.row();
        RotatableItemPreviewComponent preview = new RotatableItemPreviewComponent(UiFactory.fixed(previewSize), finalPreview.copy());
        preview.allowMouseRotation(true);
        preview.showOverlay(true);
        if (!isNarrowLayout()) {
            preview.margins(Insets.right(8));
        }
        previewRow.child(preview);

        previewRow.child(UiFactory.muted(
                ItemEditorText.tr("special.banner.preview.layers", special.bannerLayers.size()),
                previewHintWidth()
        ));
        card.child(previewRow);
        return card;
    }

    private static FlowLayout buildLayerPreviewStrip(SpecialDataPanelContext context, ItemEditorState.SpecialData special) {
        FlowLayout card = UiFactory.subCard();
        card.child(UiFactory.title(ItemEditorText.tr("special.banner.layer_strip")).shadow(false));
        List<ItemStack> stages = buildLayerStageStacks(context, special);
        if (stages.size() <= 1) {
            card.child(UiFactory.muted(ItemEditorText.tr("special.banner.layer_strip.empty"), LAYER_STRIP_EMPTY_HINT_WIDTH));
            return card;
        }

        FlowLayout strip = UIContainers.horizontalFlow(Sizing.content(), Sizing.content());
        strip.gap(4);
        strip.allowOverflow(false);
        for (int index = 0; index < stages.size(); index++) {
            ItemStack stage = stages.get(index);
            strip.child(buildStageThumbnail(special, stage, index));
        }
        ScrollContainer<FlowLayout> stripScroll = InputSafeScrollContainer.horizontal(
                Sizing.fill(100),
                UiFactory.fixed(layerStripHeight()),
                strip
        );
        stripScroll.scrollbar(ScrollContainer.Scrollbar.vanillaFlat());
        stripScroll.scrollbarThiccness(UiFactory.scaledScrollbarThickness(LAYER_STRIP_SCROLLBAR_THICKNESS));
        stripScroll.scrollStep(UiFactory.scaledScrollStep(LAYER_STRIP_SCROLL_STEP));
        card.child(stripScroll);
        return card;
    }

    private static FlowLayout buildStageThumbnail(ItemEditorState.SpecialData special, ItemStack stage, int index) {
        FlowLayout thumb = UiFactory.subCard();
        thumb.gap(2);
        thumb.horizontalSizing(UiFactory.fixed(layerPreviewWidth()));

        FlowLayout header = UiFactory.row();
        if (index == 0) {
            header.child(UiFactory.muted(ItemEditorText.tr("special.banner.stage_base"), LAYER_STAGE_LABEL_WIDTH));
        } else {
            header.child(UiFactory.muted(ItemEditorText.tr("special.banner.layer_index", index), LAYER_STAGE_LABEL_WIDTH));
        }
        BoxComponent colorChip = UIComponents.box(UiFactory.fixed(LAYER_COLOR_CHIP_SIZE), UiFactory.fixed(LAYER_COLOR_CHIP_SIZE)).fill(true);
        DyeColor parsedColor = index == 0
                ? DyeColorSelectorSection.parse(special.bannerBaseColor)
                : DyeColorSelectorSection.parse(special.bannerLayers.get(index - 1).color);
        int chipColor = parsedColor == null ? 0xB0B0B0 : parsedColor.getTextColor();
        colorChip.color(Color.ofRgb(chipColor));
        header.child(colorChip);
        thumb.child(header);

        ItemComponent itemPreview = UIComponents.item(stage).showOverlay(true);
        int iconSize = layerItemPreviewSize();
        itemPreview.horizontalSizing(UiFactory.fixed(iconSize));
        itemPreview.verticalSizing(UiFactory.fixed(iconSize));
        thumb.child(itemPreview);

        if (index == 0) {
            thumb.tooltip(List.of(
                    ItemEditorText.tr("special.banner.stage_base"),
                    DyeColorSelectorSection.buttonLabel(special.bannerBaseColor, ItemEditorText.tr("special.banner.select_base_color"))
            ));
            return thumb;
        }

        ItemEditorState.BannerLayerDraft layer = special.bannerLayers.get(index - 1);
        String patternLabel = layer.patternId.isBlank()
                ? ItemEditorText.str("special.misc.trim.select_pattern")
                : patternOptionLabel(layer.patternId);
        thumb.tooltip(List.of(
                ItemEditorText.tr("special.banner.layer", index),
                Component.literal(patternLabel),
                DyeColorSelectorSection.buttonLabel(layer.color, ItemEditorText.tr("special.banner.select_color"))
        ));
        return thumb;
    }

    private static ItemStack buildFinalPreviewStack(SpecialDataPanelContext context, ItemEditorState.SpecialData special) {
        List<ItemStack> stages = buildLayerStageStacks(context, special);
        if (stages.isEmpty()) {
            return context.screen().session().previewStack().copy();
        }
        return stages.getLast();
    }

    private static List<ItemStack> buildLayerStageStacks(SpecialDataPanelContext context, ItemEditorState.SpecialData special) {
        List<ItemStack> stages = new ArrayList<>();
        ItemStack base = createBasePreviewStack(context, special);
        stages.add(base.copy());

        Registry<BannerPattern> bannerRegistry;
        try {
            bannerRegistry = context.screen().session().registryAccess().lookupOrThrow(Registries.BANNER_PATTERN);
        } catch (RuntimeException ignored) {
            return stages;
        }

        List<BannerPatternLayers.Layer> applied = new ArrayList<>();
        for (ItemEditorState.BannerLayerDraft draft : special.bannerLayers) {
            if (draft.patternId.isBlank()) {
                continue;
            }
            var pattern = RegistryUtil.resolveHolder(bannerRegistry, draft.patternId);
            DyeColor color = DyeColorSelectorSection.parse(draft.color);
            if (pattern == null || color == null) {
                continue;
            }

            applied.add(new BannerPatternLayers.Layer(pattern, color));
            ItemStack stage = base.copy();
            stage.set(DataComponents.BANNER_PATTERNS, new BannerPatternLayers(new ArrayList<>(applied)));
            stages.add(stage);
        }
        return stages;
    }

    private static ItemStack createBasePreviewStack(SpecialDataPanelContext context, ItemEditorState.SpecialData special) {
        DyeColor baseColor = DyeColorSelectorSection.parse(special.bannerBaseColor);
        ItemStack original = context.originalStack();
        if (original.is(Items.SHIELD) && baseColor == null && !special.bannerLayers.isEmpty()) {
            baseColor = DyeColor.WHITE;
        }

        Item baseItem;
        if (original.is(Items.SHIELD)) {
            baseItem = Items.SHIELD;
        } else if (original.getItem() instanceof BannerItem) {
            baseItem = baseColor == null ? original.getItem() : bannerItemForColor(baseColor);
        } else if (context.screen().session().previewStack().getItem() instanceof BannerItem) {
            baseItem = baseColor == null ? context.screen().session().previewStack().getItem() : bannerItemForColor(baseColor);
        } else {
            baseItem = baseColor == null ? Items.WHITE_BANNER : bannerItemForColor(baseColor);
        }

        ItemStack stack = new ItemStack(baseItem);
        if (baseColor != null) {
            stack.set(DataComponents.BASE_COLOR, baseColor);
        } else {
            stack.remove(DataComponents.BASE_COLOR);
        }
        return stack;
    }

    private static int previewSize() {
        int responsive = UiFactory.responsiveSquareSize(0.17, 0.30, 78, 220);
        if (isNarrowLayout()) {
            return Math.min(responsive, PREVIEW_SIZE_NARROW_MAX);
        }
        return responsive;
    }

    private static int previewHintWidth() {
        return isNarrowLayout() ? PREVIEW_HINT_WIDTH_NARROW : PREVIEW_HINT_WIDTH_WIDE;
    }

    private static int layerPreviewWidth() {
        return isNarrowLayout() ? LAYER_PREVIEW_WIDTH_NARROW : LAYER_PREVIEW_WIDTH;
    }

    private static int layerStripHeight() {
        return isNarrowLayout() ? LAYER_STRIP_HEIGHT_NARROW : LAYER_STRIP_HEIGHT;
    }

    private static int layerItemPreviewSize() {
        return isNarrowLayout() ? LAYER_ITEM_PREVIEW_SIZE_NARROW : LAYER_ITEM_PREVIEW_SIZE;
    }

    private static boolean isNarrowLayout() {
        var window = Minecraft.getInstance().getWindow();
        return window.getGuiScaledWidth() <= NARROW_LAYOUT_WIDTH_THRESHOLD;
    }

    private static Item bannerItemForColor(DyeColor color) {
        return switch (color) {
            case WHITE -> Items.WHITE_BANNER;
            case ORANGE -> Items.ORANGE_BANNER;
            case MAGENTA -> Items.MAGENTA_BANNER;
            case LIGHT_BLUE -> Items.LIGHT_BLUE_BANNER;
            case YELLOW -> Items.YELLOW_BANNER;
            case LIME -> Items.LIME_BANNER;
            case PINK -> Items.PINK_BANNER;
            case GRAY -> Items.GRAY_BANNER;
            case LIGHT_GRAY -> Items.LIGHT_GRAY_BANNER;
            case CYAN -> Items.CYAN_BANNER;
            case PURPLE -> Items.PURPLE_BANNER;
            case BLUE -> Items.BLUE_BANNER;
            case BROWN -> Items.BROWN_BANNER;
            case GREEN -> Items.GREEN_BANNER;
            case RED -> Items.RED_BANNER;
            case BLACK -> Items.BLACK_BANNER;
        };
    }

    private static boolean isShieldContext(SpecialDataPanelContext context) {
        return context.originalStack().is(Items.SHIELD) || context.screen().session().previewStack().is(Items.SHIELD);
    }

    private static FlowLayout buildBaseColorEditor(SpecialDataPanelContext context, ItemEditorState.SpecialData special) {
        FlowLayout card = UiFactory.subCard();

        card.child(DyeColorSelectorSection.buildPaletteOnly(
                context,
                ItemEditorText.tr("special.banner.base_color"),
                Component.empty(),
                ItemEditorText.tr("special.banner.select_base_color"),
                special.bannerBaseColor,
                color -> special.bannerBaseColor = color.name()
        ));

        return card;
    }

    private static FlowLayout buildLayerCard(
            SpecialDataPanelContext context,
            ItemEditorState.SpecialData special,
            List<String> availablePatterns,
            int index
    ) {
        boolean compactLayout = isCompactLayout(context);
        ItemEditorState.BannerLayerDraft draft = special.bannerLayers.get(index);
        FlowLayout card = UiFactory.subCard();
        card.child(buildLayerHeader(context, special, index));

        card.child(PickerFieldFactory.searchableField(
                context,
                ItemEditorText.tr("special.banner.pattern"),
                Component.empty(),
                patternButtonLabel(draft.patternId),
                compactLayout ? -1 : PATTERN_BUTTON_WIDTH,
                ItemEditorText.str("special.banner.pattern"),
                ItemEditorText.str("special.banner.pattern_search_hint"),
                patternValues(availablePatterns, draft.patternId),
                BannerSpecialDataSection::patternOptionLabel,
                patternId -> context.mutateRefresh(() -> draft.patternId = patternId)
        ));
        card.child(DyeColorSelectorSection.buildPaletteOnly(
                context,
                ItemEditorText.tr("special.banner.color"),
                Component.empty(),
                ItemEditorText.tr("special.banner.select_color"),
                draft.color,
                color -> draft.color = color.name()
        ));
        return card;
    }

    private static FlowLayout buildLayerHeader(
            SpecialDataPanelContext context,
            ItemEditorState.SpecialData special,
            int index
    ) {
        boolean compactLayout = isCompactLayout(context);
        FlowLayout header = compactLayout ? UiFactory.column() : UiFactory.row();
        header.child(UiFactory.title(ItemEditorText.tr("special.banner.layer", index + 1)).shadow(false));

        if (index > 0) {
            header.child(boundedActionButton(ItemEditorText.tr("common.up"), compactLayout, () ->
                    context.mutateRefresh(() -> {
                        special.draggingBannerLayer = -1;
                        Collections.swap(special.bannerLayers, index, index - 1);
                    })
            ));
        }
        if (index < special.bannerLayers.size() - 1) {
            header.child(boundedActionButton(ItemEditorText.tr("common.down"), compactLayout, () ->
                    context.mutateRefresh(() -> {
                        special.draggingBannerLayer = -1;
                        Collections.swap(special.bannerLayers, index, index + 1);
                    })
            ));
        }

        String dragLabel = dragActionLabel(special.draggingBannerLayer, index);
        header.child(boundedActionButton(Component.literal(dragLabel), compactLayout, () ->
                context.mutateRefresh(() -> handleDragAction(special, index))
        ));

        header.child(boundedActionButton(ItemEditorText.tr("special.banner.duplicate"), compactLayout, () ->
                context.mutateRefresh(() -> {
                    special.draggingBannerLayer = -1;
                    special.bannerLayers.add(index + 1, copyLayer(special.bannerLayers.get(index)));
                })
        ));

        header.child(boundedActionButton(ItemEditorText.tr("common.remove"), compactLayout, () ->
                context.mutateRefresh(() -> {
                    special.bannerLayers.remove(index);
                    if (special.draggingBannerLayer == index) {
                        special.draggingBannerLayer = -1;
                    } else if (special.draggingBannerLayer > index) {
                        special.draggingBannerLayer--;
                    }
                })
        ));

        return header;
    }

    private static boolean isCompactLayout(SpecialDataPanelContext context) {
        return LayoutModeUtil.isCompactPanel(context.guiScale(), context.panelWidthHint(), COMPACT_LAYOUT_WIDTH_THRESHOLD);
    }

    private static String dragActionLabel(int draggingLayer, int layerIndex) {
        if (draggingLayer < 0) {
            return ItemEditorText.str("special.banner.drag");
        }
        if (draggingLayer == layerIndex) {
            return ItemEditorText.str("special.banner.drag_cancel");
        }
        return ItemEditorText.str("special.banner.drop_here");
    }

    private static void handleDragAction(ItemEditorState.SpecialData special, int targetIndex) {
        if (targetIndex < 0 || targetIndex >= special.bannerLayers.size()) {
            special.draggingBannerLayer = -1;
            return;
        }

        int sourceIndex = special.draggingBannerLayer;
        if (sourceIndex < 0) {
            special.draggingBannerLayer = targetIndex;
            return;
        }
        if (sourceIndex == targetIndex) {
            special.draggingBannerLayer = -1;
            return;
        }

        ItemEditorState.BannerLayerDraft source = special.bannerLayers.remove(sourceIndex);
        int insertIndex = sourceIndex < targetIndex ? targetIndex : targetIndex + 1;
        insertIndex = Math.clamp(insertIndex, 0, special.bannerLayers.size());
        special.bannerLayers.add(insertIndex, source);
        special.draggingBannerLayer = -1;
    }

    private static ItemEditorState.BannerLayerDraft copyLayer(ItemEditorState.BannerLayerDraft source) {
        ItemEditorState.BannerLayerDraft copy = new ItemEditorState.BannerLayerDraft();
        copy.patternId = source.patternId;
        copy.color = source.color;
        return copy;
    }

    private static List<String> availablePatternIds(SpecialDataPanelContext context, ItemEditorState.SpecialData special) {
        List<String> ids = new ArrayList<>();
        Registry<BannerPattern> bannerRegistry = null;
        try {
            bannerRegistry = context.screen().session().registryAccess().lookupOrThrow(Registries.BANNER_PATTERN);
        } catch (RuntimeException ignored) {
        }

        if (bannerRegistry != null) {
            ids.addAll(RegistryUtil.ids(bannerRegistry));
        }

        for (ItemEditorState.BannerLayerDraft layer : special.bannerLayers) {
            if (!layer.patternId.isBlank() && !ids.contains(layer.patternId)) {
                ids.add(layer.patternId);
            }
        }
        ids.sort(String::compareTo);
        return ids;
    }

    private static List<String> patternValues(List<String> availablePatterns, String currentPatternId) {
        if (currentPatternId.isBlank() || availablePatterns.contains(currentPatternId)) {
            return availablePatterns;
        }
        List<String> values = new ArrayList<>(availablePatterns);
        values.add(currentPatternId);
        values.sort(String::compareTo);
        return values;
    }

    private static Component patternButtonLabel(String patternId) {
        return patternId.isBlank()
                ? ItemEditorText.tr("special.misc.trim.select_pattern")
                : Component.literal(patternOptionLabel(patternId));
    }

    private static String patternOptionLabel(String patternId) {
        String normalized = patternId.trim();
        int split = normalized.indexOf(':');
        String path = split >= 0 && split < normalized.length() - 1 ? normalized.substring(split + 1) : normalized;
        String pretty = DyeColorSelectorSection.toTitleCase(path.replace('_', ' '));
        return pretty + " (" + normalized + ")";
    }

    private static ButtonComponent boundedActionButton(Component fullText, boolean compactLayout, Runnable action) {
        int buttonWidth = Math.max(
                ACTION_BUTTON_WIDTH_MIN,
                Math.min(ACTION_BUTTON_WIDTH_MAX, UiFactory.scaledPixels(ACTION_BUTTON_WIDTH_BASE))
        );
        Component fitted = UiFactory.fitToWidth(
                fullText,
                Math.max(ACTION_BUTTON_TEXT_MIN, buttonWidth - UiFactory.scaledPixels(ACTION_BUTTON_TEXT_RESERVE))
        );
        ButtonComponent button = UiFactory.button(fitted, UiFactory.ButtonTextPreset.STANDARD, component -> action.run());
        button.horizontalSizing(compactLayout ? Sizing.fill(100) : Sizing.fixed(buttonWidth));
        if (!fitted.getString().equals(fullText.getString())) {
            button.tooltip(List.of(fullText));
        }
        return button;
    }
}
