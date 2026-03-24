package me.noramibu.itemeditor.ui.panel.specialdata;

import io.wispforest.owo.ui.component.BoxComponent;
import io.wispforest.owo.ui.component.UIComponents;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Color;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.Surface;
import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.ui.component.DyeColorSelectorSection;
import me.noramibu.itemeditor.ui.component.PickerFieldFactory;
import me.noramibu.itemeditor.ui.component.PixelCanvasPreview;
import me.noramibu.itemeditor.ui.component.UiFactory;
import me.noramibu.itemeditor.util.ItemEditorText;
import me.noramibu.itemeditor.util.RegistryUtil;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BannerPattern;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class BannerSpecialDataSection {

    private static final int BASE_COLOR_BUTTON_WIDTH = 190;
    private static final int PATTERN_BUTTON_WIDTH = 280;
    private static final int LAYER_COLOR_BUTTON_WIDTH = 170;
    private static final int CANVAS_WIDTH = 18;
    private static final int CANVAS_HEIGHT = 24;
    private static final int CANVAS_PIXEL = 3;
    private static final Surface CANVAS_SURFACE = Surface.flat(0xDD0F131A).and(Surface.outline(0xFF3E4A5A));

    private static final List<PatternMaskRule> PATTERN_MASK_RULES = List.of(
            new PatternMaskRule("stripe_bottom", (x, y) -> y >= CANVAS_HEIGHT - CANVAS_HEIGHT / 3),
            new PatternMaskRule("stripe_top", (x, y) -> y < CANVAS_HEIGHT / 3),
            new PatternMaskRule("stripe_left", (x, y) -> x < CANVAS_WIDTH / 3),
            new PatternMaskRule("stripe_right", (x, y) -> x >= CANVAS_WIDTH - CANVAS_WIDTH / 3),
            new PatternMaskRule("stripe_center", (x, y) -> Math.abs(x - CANVAS_WIDTH / 2) <= 1),
            new PatternMaskRule("stripe_middle", (x, y) -> Math.abs(y - CANVAS_HEIGHT / 2) <= 1),
            new PatternMaskRule("small_stripes", (x, y) -> x % 4 <= 1),
            new PatternMaskRule("straight_cross", (x, y) -> Math.abs(x - CANVAS_WIDTH / 2) <= 1 || Math.abs(y - CANVAS_HEIGHT / 2) <= 1),
            new PatternMaskRule("cross", (x, y) -> Math.abs(x - CANVAS_WIDTH / 2) <= 1 || Math.abs(y - CANVAS_HEIGHT / 2) <= 1),
            new PatternMaskRule("diagonal_left", (x, y) -> Math.abs((CANVAS_WIDTH - 1 - x) - y * CANVAS_WIDTH / CANVAS_HEIGHT) <= 1),
            new PatternMaskRule("diagonal_right", (x, y) -> Math.abs(x - y * CANVAS_WIDTH / CANVAS_HEIGHT) <= 1),
            new PatternMaskRule("diagonal_up_left", (x, y) -> Math.abs((CANVAS_WIDTH - 1 - x) - (CANVAS_HEIGHT - 1 - y) * CANVAS_WIDTH / CANVAS_HEIGHT) <= 1),
            new PatternMaskRule("diagonal_up_right", (x, y) -> Math.abs(x - (CANVAS_HEIGHT - 1 - y) * CANVAS_WIDTH / CANVAS_HEIGHT) <= 1),
            new PatternMaskRule("triangle_bottom", (x, y) -> y >= CANVAS_HEIGHT - CANVAS_HEIGHT / 3 && Math.abs(x - CANVAS_WIDTH / 2) <= (CANVAS_HEIGHT - y)),
            new PatternMaskRule("triangle_top", (x, y) -> y < CANVAS_HEIGHT / 3 && Math.abs(x - CANVAS_WIDTH / 2) <= y + 1),
            new PatternMaskRule("half_vertical_right", (x, y) -> x >= CANVAS_WIDTH / 2),
            new PatternMaskRule("half_vertical", (x, y) -> x < CANVAS_WIDTH / 2),
            new PatternMaskRule("half_horizontal_bottom", (x, y) -> y >= CANVAS_HEIGHT / 2),
            new PatternMaskRule("half_horizontal", (x, y) -> y < CANVAS_HEIGHT / 2),
            new PatternMaskRule("border", (x, y) -> x < 2 || x >= CANVAS_WIDTH - 2 || y < 2 || y >= CANVAS_HEIGHT - 2),
            new PatternMaskRule("gradient_up", (x, y) -> y < CANVAS_HEIGHT - (x + y) / 2),
            new PatternMaskRule("gradient", (x, y) -> y >= (x + y) / 3),
            new PatternMaskRule("circle", (x, y) -> {
                int dx = x - CANVAS_WIDTH / 2;
                int dy = y - CANVAS_HEIGHT / 2;
                int radius = Math.min(CANVAS_WIDTH, CANVAS_HEIGHT) / 4;
                return dx * dx + dy * dy <= radius * radius;
            }),
            new PatternMaskRule("rhombus", (x, y) -> Math.abs(x - CANVAS_WIDTH / 2) + Math.abs(y - CANVAS_HEIGHT / 2) <= Math.min(CANVAS_WIDTH / 4, CANVAS_HEIGHT / 4) + 2),
            new PatternMaskRule("creeper", (x, y) -> {
                int quarterW = CANVAS_WIDTH / 4;
                int quarterH = CANVAS_HEIGHT / 4;
                boolean eyeLeft = x >= quarterW && x <= quarterW + 2 && y >= quarterH && y <= quarterH + 2;
                boolean eyeRight = x >= CANVAS_WIDTH - quarterW - 3 && x <= CANVAS_WIDTH - quarterW - 1 && y >= quarterH && y <= quarterH + 2;
                boolean mouth = y >= CANVAS_HEIGHT / 2 && y <= CANVAS_HEIGHT / 2 + 4 && x >= quarterW + 2 && x <= CANVAS_WIDTH - quarterW - 3;
                return eyeLeft || eyeRight || mouth;
            })
    );

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
        FlowLayout section = UiFactory.section(ItemEditorText.tr("special.banner.title"), Component.empty());
        List<String> availablePatterns = availablePatternIds(context, special);

        section.child(buildMiniBannerCanvas(context, special));
        section.child(buildLayerThumbnailStrip(special));
        section.child(buildBaseColorEditor(context, special));

        section.child(UiFactory.button(ItemEditorText.tr("special.banner.add_layer"), button ->
                context.mutateRefresh(() -> {
                    ItemEditorState.BannerLayerDraft draft = new ItemEditorState.BannerLayerDraft();
                    draft.color = DyeColor.WHITE.name();
                    special.bannerLayers.add(draft);
                })
        ));

        for (int index = 0; index < special.bannerLayers.size(); index++) {
            section.child(buildLayerCard(context, special, availablePatterns, index));
        }
        return section;
    }

    private static FlowLayout buildMiniBannerCanvas(SpecialDataPanelContext context, ItemEditorState.SpecialData special) {
        FlowLayout card = UiFactory.subCard();
        card.child(UiFactory.title(ItemEditorText.tr("screen.preview")).shadow(false));

        FlowLayout previewRow = UiFactory.row();
        previewRow.child(UIComponents.item(context.screen().session().previewStack()).showOverlay(true).margins(Insets.right(8)));
        previewRow.child(UiFactory.muted(ItemEditorText.tr("special.banner.preview.layers", special.bannerLayers.size()), 240));
        card.child(previewRow);

        int[][] pixels = buildBannerPixels(special);
        card.child(PixelCanvasPreview.fromColors(pixels, CANVAS_PIXEL, 0, Insets.of(2), CANVAS_SURFACE));
        return card;
    }

    private static int[][] buildBannerPixels(ItemEditorState.SpecialData special) {
        int[][] pixels = PixelCanvasPreview.fill(CANVAS_WIDTH, CANVAS_HEIGHT, dyeRgb(DyeColorSelectorSection.parse(special.bannerBaseColor), 0xE6E6E6));

        for (ItemEditorState.BannerLayerDraft layer : special.bannerLayers) {
            int layerColor = dyeRgb(DyeColorSelectorSection.parse(layer.color), 0xA0A0A0);
            String patternPath = patternPath(layer.patternId);
            for (int y = 0; y < CANVAS_HEIGHT; y++) {
                for (int x = 0; x < CANVAS_WIDTH; x++) {
                    if (maskForPattern(patternPath, x, y)) {
                        pixels[y][x] = layerColor;
                    }
                }
            }
        }

        return pixels;
    }

    private static boolean maskForPattern(String patternPath, int x, int y) {
        if (patternPath.isBlank()) {
            return false;
        }

        for (PatternMaskRule rule : PATTERN_MASK_RULES) {
            if (patternPath.contains(rule.token())) {
                return rule.mask().matches(x, y);
            }
        }

        int hash = Math.abs(patternPath.hashCode());
        int step = 2 + (hash % 4);
        return ((x + y + hash) % step) == 0;
    }

    private static int dyeRgb(DyeColor color, int fallback) {
        if (color == null) {
            return fallback;
        }
        return color.getTextColor();
    }

    private static String patternPath(String patternId) {
        if (patternId == null || patternId.isBlank()) {
            return "";
        }
        int split = patternId.indexOf(':');
        return split >= 0 && split < patternId.length() - 1
                ? patternId.substring(split + 1).toLowerCase(Locale.ROOT)
                : patternId.toLowerCase(Locale.ROOT);
    }

    private static FlowLayout buildLayerThumbnailStrip(ItemEditorState.SpecialData special) {
        FlowLayout card = UiFactory.subCard();
        card.child(UiFactory.title(ItemEditorText.tr("special.banner.layer_strip")).shadow(false));
        if (special.bannerLayers.isEmpty()) {
            card.child(UiFactory.muted(ItemEditorText.tr("special.banner.layer_strip.empty"), 260));
            return card;
        }

        FlowLayout strip = UiFactory.row();
        for (int index = 0; index < special.bannerLayers.size(); index++) {
            ItemEditorState.BannerLayerDraft layer = special.bannerLayers.get(index);
            strip.child(buildLayerThumbnail(layer, index));
        }
        card.child(strip);
        return card;
    }

    private static FlowLayout buildLayerThumbnail(ItemEditorState.BannerLayerDraft layer, int index) {
        FlowLayout thumb = UiFactory.subCard();
        thumb.gap(2);
        thumb.horizontalSizing(Sizing.fixed(86));

        FlowLayout header = UiFactory.row();
        header.child(UiFactory.muted(ItemEditorText.tr("special.banner.layer_index", index + 1), 40));
        BoxComponent colorChip = UIComponents.box(Sizing.fixed(10), Sizing.fixed(10)).fill(true);
        DyeColor parsedColor = DyeColorSelectorSection.parse(layer.color);
        int chipColor = parsedColor == null ? 0xB0B0B0 : parsedColor.getTextColor();
        colorChip.color(Color.ofRgb(chipColor));
        header.child(colorChip);
        thumb.child(header);

        String patternLabel = layer.patternId.isBlank()
                ? ItemEditorText.str("special.misc.trim.select_pattern")
                : patternOptionLabel(layer.patternId);
        Component shortLabel = UiFactory.fitToWidth(Component.literal(patternLabel), 70);
        thumb.child(UiFactory.muted(shortLabel, 70));
        thumb.tooltip(List.of(
                ItemEditorText.tr("special.banner.layer", index + 1),
                Component.literal(patternLabel),
                DyeColorSelectorSection.buttonLabel(layer.color, ItemEditorText.tr("special.banner.select_color"))
        ));
        return thumb;
    }

    private static FlowLayout buildBaseColorEditor(SpecialDataPanelContext context, ItemEditorState.SpecialData special) {
        FlowLayout card = UiFactory.subCard();

        card.child(DyeColorSelectorSection.build(
                context,
                ItemEditorText.tr("special.banner.base_color"),
                Component.empty(),
                ItemEditorText.tr("special.banner.select_base_color"),
                special.bannerBaseColor,
                BASE_COLOR_BUTTON_WIDTH,
                ItemEditorText.tr("special.banner.color_palette"),
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
        ItemEditorState.BannerLayerDraft draft = special.bannerLayers.get(index);
        FlowLayout card = context.createReorderableCard(
                ItemEditorText.tr("special.banner.layer", index + 1),
                index > 0,
                () -> Collections.swap(special.bannerLayers, index, index - 1),
                index < special.bannerLayers.size() - 1,
                () -> Collections.swap(special.bannerLayers, index, index + 1),
                () -> special.bannerLayers.remove(index)
        );

        card.child(PickerFieldFactory.searchableField(
                context,
                ItemEditorText.tr("special.banner.pattern"),
                Component.empty(),
                patternButtonLabel(draft.patternId),
                PATTERN_BUTTON_WIDTH,
                ItemEditorText.str("special.banner.pattern"),
                "",
                patternValues(availablePatterns, draft.patternId),
                BannerSpecialDataSection::patternOptionLabel,
                patternId -> context.mutateRefresh(() -> draft.patternId = patternId)
        ));
        card.child(DyeColorSelectorSection.build(
                context,
                ItemEditorText.tr("special.banner.color"),
                Component.empty(),
                ItemEditorText.tr("special.banner.select_color"),
                draft.color,
                LAYER_COLOR_BUTTON_WIDTH,
                ItemEditorText.tr("special.banner.color_palette"),
                color -> draft.color = color.name()
        ));
        return card;
    }

    private static List<String> availablePatternIds(SpecialDataPanelContext context, ItemEditorState.SpecialData special) {
        List<String> ids = new ArrayList<>();
        Registry<BannerPattern> bannerRegistry = null;
        try {
            bannerRegistry = context.screen().session().registryAccess().lookupOrThrow(Registries.BANNER_PATTERN);
        } catch (Exception ignored) {
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

    @FunctionalInterface
    private interface PatternMask {
        boolean matches(int x, int y);
    }

    private record PatternMaskRule(String token, PatternMask mask) {
    }
}
