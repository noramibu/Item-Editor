package me.noramibu.itemeditor.ui.panel.specialdata;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.HorizontalAlignment;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.UIComponent;
import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.ui.component.PickerFieldFactory;
import me.noramibu.itemeditor.ui.component.RawTextAreaComponent;
import me.noramibu.itemeditor.ui.component.UiFactory;
import me.noramibu.itemeditor.ui.screen.ItemEditorScreen;
import me.noramibu.itemeditor.util.IdFieldNormalizer;
import me.noramibu.itemeditor.util.ItemEditorCapabilities;
import me.noramibu.itemeditor.util.ItemEditorText;
import me.noramibu.itemeditor.util.RegistryUtil;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SwingAnimationType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class AdvancedItemSpecialDataSection {
    private static final int NARROW_LAYOUT_WIDTH_THRESHOLD = 900;
    private static final double NARROW_LAYOUT_SCALE_THRESHOLD = 3.0d;
    private static final int STACKED_COMPACT_WIDTH_THRESHOLD = 1080;
    private static final double STACKED_COMPACT_SCALE_THRESHOLD = 2.5d;
    private static final int EQUIPMENT_DENSE_STACK_WIDTH_THRESHOLD = 620;
    private static final int EQUIPMENT_FLAGS_SINGLE_ROW_WIDTH_THRESHOLD = 700;
    private static final int COMPACT_ICON_BUTTON_MIN = 36;
    private static final int COMPACT_ICON_BUTTON_BASE = 42;
    private static final int COMPACT_CLEAR_BUTTON_MIN = 52;
    private static final int COMPACT_CLEAR_BUTTON_BASE = 68;
    private static final int COMPACT_REMOVE_BUTTON_MIN = 72;
    private static final int COMPACT_REMOVE_BUTTON_BASE = 88;
    private static final int COMPACT_FIXED_PICK_BUTTON_MIN = 46;
    private static final int COMPACT_FIXED_PICK_BUTTON_BASE = 56;
    private static final int PICKER_ROW_INLINE_MIN_WIDTH = 360;
    private static final int PICKER_ROW_INPUT_MIN_WIDTH = 180;
    private static final int BLOCK_STATE_STACKED_ROW_WIDTH_THRESHOLD = 420;
    private static final int BLOCK_STATE_VALUE_WIDTH_PERCENT = 76;
    private static final int BLOCK_STATE_VALUE_WITH_RESET_WIDTH_PERCENT = 56;
    private static final int SECTION_ROW_GAP = 10;
    private static final int COLLAPSIBLE_HEADER_TITLE_RESERVE = 26;
    private static final int COMPACT_FIELD_LABEL_RESERVE = 44;
    private static final int PANEL_WIDTH_SAFETY_RESERVE = 20;
    private static final String SYMBOL_SECTION_COLLAPSED = "+";
    private static final String SYMBOL_SECTION_EXPANDED = "-";
    private static final String SYMBOL_STEP_DECREMENT = "-";
    private static final String SYMBOL_STEP_INCREMENT = "+";
    private static final String ACTION_CLEAR_ALL = "Clear All";
    private static final String TOOLTIP_EXPAND_ALL_BEES = "Expand all bees";
    private static final String TOOLTIP_COLLAPSE_ALL_BEES = "Collapse all bees";
    private static final String TOOLTIP_EXPAND_ALL_PROJECTILES = "Expand all projectiles";
    private static final String TOOLTIP_COLLAPSE_ALL_PROJECTILES = "Collapse all projectiles";
    private static final String TOOLTIP_EXPAND_ALL_DECORATIONS = "Expand all decorations";
    private static final String TOOLTIP_COLLAPSE_ALL_DECORATIONS = "Collapse all decorations";
    private static final int CONSUMABLE_EFFECTS_EMPTY_HINT_WIDTH = 300;
    private static final int CONSUMABLE_EFFECT_SUMMARY_HINT_WIDTH = 300;
    private static final int CONSUMABLE_APPLY_EFFECTS_EMPTY_HINT_WIDTH = 280;
    private static final int MAP_DECORATIONS_EMPTY_HINT_WIDTH = 280;
    private static final int COMBAT_REPAIRABLE_EMPTY_HINT_WIDTH = 280;
    private static final int CONTAINER_META_LOCK_HINT_WIDTH = 360;
    private static final int CONTAINER_META_BEES_EMPTY_HINT_WIDTH = 320;
    private static final int CONTAINER_META_BEE_SUMMARY_HINT_WIDTH = 340;
    private static final int CROSSBOW_EMPTY_HINT_WIDTH = 320;
    private static final int CROSSBOW_PROJECTILE_SUMMARY_HINT_WIDTH = 340;
    private static final int MAP_DECORATION_SUMMARY_HINT_WIDTH = 360;
    private static final int BLOCKS_ATTACKS_REDUCTION_EMPTY_HINT_WIDTH = 320;
    private static final int BLOCKS_ATTACKS_REDUCTION_SUMMARY_HINT_WIDTH = 360;
    private static final int CUSTOM_DATA_EDITOR_HEIGHT = 220;
    private static final int CUSTOM_DATA_CONTENT_PADDING = 4;
    private static final int CUSTOM_DATA_TEXT_WIDTH_RESERVE = 14;
    private static final int CUSTOM_DATA_HINT_MIN_WIDTH = 96;

    private AdvancedItemSpecialDataSection() {
    }

    public static boolean supportsContainerMetadata(ItemStack stack, RegistryAccess registryAccess) {
        return ItemEditorCapabilities.supportsAnyComponent(
                stack,
                registryAccess,
                "minecraft:lock",
                "minecraft:container_loot",
                "minecraft:bees",
                "minecraft:pot_decorations"
        ) || stack.has(DataComponents.LOCK)
                || stack.has(DataComponents.CONTAINER_LOOT)
                || stack.has(DataComponents.BEES)
                || stack.has(DataComponents.POT_DECORATIONS)
                || ItemEditorCapabilities.supportsContainerData(stack)
                || stack.is(Items.BEEHIVE)
                || stack.is(Items.BEE_NEST)
                || stack.is(Items.DECORATED_POT);
    }

    public static boolean supportsCrossbow(ItemStack stack, RegistryAccess registryAccess) {
        return ItemEditorCapabilities.supportsComponent(stack, registryAccess, "minecraft:charged_projectiles")
                || stack.has(DataComponents.CHARGED_PROJECTILES)
                || stack.is(Items.CROSSBOW);
    }

    public static boolean supportsMapAdvanced(ItemStack stack, RegistryAccess registryAccess) {
        return ItemEditorCapabilities.supportsAnyComponent(
                stack,
                registryAccess,
                "minecraft:map_id",
                "minecraft:map_decorations",
                "minecraft:lodestone_tracker"
        ) || stack.has(DataComponents.MAP_ID)
                || stack.has(DataComponents.MAP_DECORATIONS)
                || stack.has(DataComponents.LODESTONE_TRACKER)
                || stack.is(Items.FILLED_MAP);
    }

    public static boolean supportsBlockState(ItemStack stack) {
        return hasBlockStateProperties(stack);
    }

    public static FlowLayout buildComponentTweakNamingSection(SpecialDataPanelContext context) {
        return buildComponentTweakNamingSection(context, context.special());
    }

    public static FlowLayout buildComponentTweakRegistrySection(SpecialDataPanelContext context) {
        return buildComponentTweakRegistrySection(context, context.special());
    }

    public static FlowLayout buildBlockState(SpecialDataPanelContext context) {
        return buildBlockState(context, context.special());
    }

    public static FlowLayout buildComponentTweakBlockSection(SpecialDataPanelContext context) {
        return buildComponentTweakBlockSection(context, context.special());
    }

    public static FlowLayout buildComponentTweakBehaviorSection(SpecialDataPanelContext context) {
        return buildComponentTweakBehaviorSection(context, context.special());
    }

    public static FlowLayout buildCustomData(SpecialDataPanelContext context) {
        ItemEditorState.SpecialData special = context.special();
        return collapsibleCard(
                context,
                ItemEditorText.tr("special.advanced.custom_data.title"),
                special.uiCustomDataCollapsed,
                value -> special.uiCustomDataCollapsed = value,
                () -> {
                    int contentWidth = customDataContentWidth(context);
                    FlowLayout content = UiFactory.column();
                    content.padding(Insets.of(
                            0,
                            0,
                            UiFactory.scaledPixels(CUSTOM_DATA_CONTENT_PADDING),
                            UiFactory.scaledPixels(CUSTOM_DATA_CONTENT_PADDING)
                    ));
                    content.child(wrappedMutedText(ItemEditorText.tr("special.advanced.custom_data.hint"), contentWidth));
                    content.child(compactField(
                            ItemEditorText.tr("special.advanced.custom_data.editor"),
                            customDataEditor(context, special),
                            contentWidth
                    ));
                    return content;
                }
        );
    }

    private static RawTextAreaComponent customDataEditor(SpecialDataPanelContext context, ItemEditorState.SpecialData special) {
        RawTextAreaComponent editor = new RawTextAreaComponent(
                Sizing.fill(100),
                UiFactory.fixed(CUSTOM_DATA_EDITOR_HEIGHT),
                special.customDataSnbt
        );
        editor.wordWrap(true);
        editor.onChanged().subscribe((value, delta) -> context.mutate(() -> special.customDataSnbt = value));
        return editor;
    }

    private static int customDataContentWidth(SpecialDataPanelContext context) {
        int padding = UiFactory.scaledPixels(CUSTOM_DATA_CONTENT_PADDING * 2);
        int reserve = UiFactory.scaledPixels(CUSTOM_DATA_TEXT_WIDTH_RESERVE);
        return Math.clamp(
                context.panelWidthHint() - padding - reserve,
                CUSTOM_DATA_HINT_MIN_WIDTH,
                Math.max(CUSTOM_DATA_HINT_MIN_WIDTH, guiWidth())
        );
    }

    private static FlowLayout wrappedMutedText(Component text, int maxWidth) {
        FlowLayout lines = UiFactory.column();
        lines.gap(Math.max(1, UiFactory.scaleProfile().tightSpacing() - 2));
        for (String line : wrapText(text.getString(), maxWidth)) {
            lines.child(UiFactory.muted(Component.literal(line), maxWidth));
        }
        return lines;
    }

    private static List<String> wrapText(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        String remaining = text == null ? "" : text.trim();
        var font = Minecraft.getInstance().font;
        while (!remaining.isEmpty()) {
            String line = font.plainSubstrByWidth(remaining, maxWidth);
            if (line.isEmpty()) {
                int next = Character.charCount(remaining.codePointAt(0));
                line = remaining.substring(0, next);
            } else if (line.length() < remaining.length()) {
                int breakAt = line.lastIndexOf(' ');
                if (breakAt > 0) {
                    line = line.substring(0, breakAt);
                }
            }
            lines.add(line);
            remaining = remaining.substring(line.length()).trim();
        }
        if (lines.isEmpty()) {
            lines.add("");
        }
        return lines;
    }

    public static FlowLayout buildFoodConsumable(SpecialDataPanelContext context) {
        ItemEditorState.SpecialData special = context.special();
        return collapsibleCard(
                context,
                ItemEditorText.tr("special.advanced.food.title"),
                special.uiFoodConsumableCollapsed,
                value -> special.uiFoodConsumableCollapsed = value,
                () -> {
                    boolean narrowLayout = isNarrowLayout();
                    int compactNumberWidth = compactNumericFieldWidth();
                    int compactTinyWidth = compactTinyFieldWidth();
                    int compactIdWidth = narrowLayout ? clampWidth(guiWidth(), 0.14, 130, 220) : compactIdTextWidth();
                    int compactGroupWidth = compactGroupFieldWidth();
                    int animationButtonWidth = narrowLayout ? clampWidth(guiWidth(), 0.11, 110, 180) : compactPickerButtonWidth();
                    FlowLayout content = UiFactory.column();
                    content.child(denseEquipmentRow(
                            compactTextField(
                                    context,
                                    ItemEditorText.tr("special.advanced.food.nutrition"),
                                    special.foodNutrition,
                                    value -> special.foodNutrition = value,
                                    compactNumberWidth
                            ),
                            compactTextField(
                                    context,
                                    ItemEditorText.tr("special.advanced.food.saturation"),
                                    special.foodSaturation,
                                    value -> special.foodSaturation = value,
                                    compactNumberWidth
                            ),
                            compactTextField(
                                    context,
                                    ItemEditorText.tr("special.advanced.consumable.consume_seconds"),
                                    special.consumableConsumeSeconds,
                                    value -> special.consumableConsumeSeconds = value,
                                    compactNumberWidth
                            )
                    ));

                    content.child(compactCheckboxRow(
                            UiFactory.checkbox(
                                    ItemEditorText.tr("special.advanced.food.can_always_eat"),
                                    special.foodCanAlwaysEat,
                                    context.bindToggle(value -> special.foodCanAlwaysEat = value)
                            ),
                            UiFactory.checkbox(
                                    ItemEditorText.tr("special.advanced.consumable.has_particles"),
                                    special.consumableHasParticles,
                                    context.bindToggle(value -> special.consumableHasParticles = value)
                            )
                    ));

                    content.child(compactAnimationPicker(context, special, animationButtonWidth));

                    content.child(compactIdField(
                            context,
                            ItemEditorText.tr("special.advanced.consumable.sound"),
                            special.consumableSoundId,
                            value -> special.consumableSoundId = value,
                            soundIds(context),
                            ItemEditorText.str("special.advanced.consumable.sound"),
                            compactIdWidth
                    ));
                    content.child(buildOnConsumeEffectsEditor(context, special));

                    content.child(compactCheckboxRow(
                            UiFactory.checkbox(
                                    ItemEditorText.tr("special.advanced.use_effects.can_sprint"),
                                    special.useEffectsCanSprint,
                                    context.bindToggle(value -> special.useEffectsCanSprint = value)
                            ),
                            UiFactory.checkbox(
                                    ItemEditorText.tr("special.advanced.use_effects.interact_vibrations"),
                                    special.useEffectsInteractVibrations,
                                    context.bindToggle(value -> special.useEffectsInteractVibrations = value)
                            )
                    ));
                    content.child(denseEquipmentRow(
                            compactTextField(
                                    context,
                                    ItemEditorText.tr("special.advanced.use_effects.speed_multiplier"),
                                    special.useEffectsSpeedMultiplier,
                                    value -> special.useEffectsSpeedMultiplier = value,
                                    compactNumberWidth
                            ),
                            compactTextField(
                                    context,
                                    ItemEditorText.tr("special.advanced.use_remainder.count"),
                                    special.useRemainderCount,
                                    value -> special.useRemainderCount = value,
                                    compactTinyWidth
                            ),
                            compactTextField(
                                    context,
                                    ItemEditorText.tr("special.advanced.use_cooldown.seconds"),
                                    special.useCooldownSeconds,
                                    value -> special.useCooldownSeconds = value,
                                    compactNumberWidth
                            ),
                            compactTextField(
                                    context,
                                    ItemEditorText.tr("special.advanced.use_cooldown.group"),
                                    special.useCooldownGroup,
                                    value -> special.useCooldownGroup = value,
                                    compactGroupWidth
                            )
                    ));
                    content.child(compactIdField(
                            context,
                            ItemEditorText.tr("special.advanced.use_remainder.item_id"),
                            special.useRemainderItemId,
                            value -> special.useRemainderItemId = value,
                            itemIds(context),
                            ItemEditorText.str("special.advanced.use_remainder.item_id"),
                            compactIdWidth
                    ));

                    FlowLayout actions = responsiveRow();
                    ButtonComponent resetAll = UiFactory.button(ItemEditorText.tr("common.reset"), UiFactory.ButtonTextPreset.STANDARD,  button ->
                            context.mutateRefresh(() -> {
                                special.foodNutrition = "";
                                special.foodSaturation = "";
                                special.foodCanAlwaysEat = false;
                                special.consumableConsumeSeconds = "";
                                special.consumableAnimation = "";
                                special.consumableSoundId = "";
                                special.consumableHasParticles = false;
                                special.consumableOnConsumeEffects.clear();
                                special.useEffectsCanSprint = false;
                                special.useEffectsInteractVibrations = false;
                                special.useEffectsSpeedMultiplier = "";
                                special.useRemainderItemId = "";
                                special.useRemainderCount = "";
                                special.useCooldownSeconds = "";
                                special.useCooldownGroup = "";
                            })
                    );
                    resetAll.horizontalSizing(Sizing.fill(100));
                    actions.child(resetAll);
                    content.child(actions);
                    return content;
                }
        );
    }

    private static FlowLayout collapsibleCard(
            SpecialDataPanelContext context,
            Component title,
            boolean collapsed,
            Consumer<Boolean> setter,
            Supplier<FlowLayout> contentBuilder
    ) {
        FlowLayout card = UiFactory.subCard();
        FlowLayout header = UiFactory.row();
        int toggleWidth = compactIconButtonWidth();
        int preferredTitleWidth = Math.max(30, guiWidth() - toggleWidth - UiFactory.scaledPixels(COLLAPSIBLE_HEADER_TITLE_RESERVE));
        int titleWidth = Math.clamp(preferredTitleWidth, 1, Math.max(1, guiWidth()));
        Component fittedTitle = UiFactory.fitToWidth(title, titleWidth);
        var titleLabel = UiFactory.title(fittedTitle).shadow(false).horizontalSizing(Sizing.expand(100));
        if (!Objects.equals(fittedTitle.getString(), title.getString())) {
            titleLabel.tooltip(List.of(title));
        }
        header.child(titleLabel);
        ButtonComponent toggle = UiFactory.button(Component.literal(collapsed ? SYMBOL_SECTION_COLLAPSED : SYMBOL_SECTION_EXPANDED), UiFactory.ButtonTextPreset.STANDARD,  button -> {
            setter.accept(!collapsed);
            context.screen().refreshCurrentPanel();
        });
        toggle.horizontalSizing(Sizing.fixed(toggleWidth));
        header.child(toggle);
        card.child(header);
        if (!collapsed) {
            card.child(contentBuilder.get());
        }
        return card;
    }

    private static FlowLayout compactTextField(
            SpecialDataPanelContext context,
            Component label,
            String value,
            Consumer<String> setter,
            int width
    ) {
        UIComponent input = UiFactory.textBox(value, context.bindText(setter)).horizontalSizing(Sizing.fill(100));
        return compactField(label, input, width + 40);
    }

    private static FlowLayout compactAnimationPicker(
            SpecialDataPanelContext context,
            ItemEditorState.SpecialData special,
            int buttonWidth
    ) {
        ButtonComponent button = UiFactory.button(
                PickerFieldFactory.selectedOrFallback(special.consumableAnimation, ItemEditorText.tr("special.advanced.select")), UiFactory.ButtonTextPreset.STANDARD,
                anchor -> context.openClearableDropdown(
                        anchor,
                        ItemEditorText.tr("common.none"),
                        () -> context.mutate(() -> special.consumableAnimation = ""),
                        Arrays.asList(ItemUseAnimation.values()),
                        ItemUseAnimation::name,
                        animation -> context.mutate(() -> special.consumableAnimation = animation.name())
                )
        );
        button.horizontalSizing(Sizing.fill(100));
        return compactField(ItemEditorText.tr("special.advanced.consumable.animation"), button, buttonWidth + 40);
    }

    private static FlowLayout compactIdField(
            SpecialDataPanelContext context,
            Component label,
            String value,
            Consumer<String> setter,
            List<String> entries,
            String pickerTitle,
            int textWidth
    ) {
        return compactField(label, idTextWithPickerCompact(context, value, setter, entries, pickerTitle), textWidth + 110);
    }

    private static FlowLayout compactField(Component label, UIComponent input, int labelWidth) {
        FlowLayout field = UiFactory.column();
        field.gap(2);
        int panelWidth = guiWidth();
        int availableLabelWidth = Math.max(
                80,
                panelWidth - UiFactory.scaledPixels(COMPACT_FIELD_LABEL_RESERVE)
        );
        int preferredLabelWidth = prefersStackedCompactRows()
                ? availableLabelWidth
                : Math.clamp(availableLabelWidth, 40, Math.max(40, labelWidth));
        int effectiveLabelWidth = Math.clamp(preferredLabelWidth, 1, Math.max(1, panelWidth));
        Component fittedLabel = UiFactory.fitToWidth(label, effectiveLabelWidth);
        var labelComponent = UiFactory.muted(fittedLabel, effectiveLabelWidth);
        labelComponent.horizontalSizing(Sizing.fill(100));
        if (!Objects.equals(fittedLabel.getString(), label.getString())) {
            labelComponent.tooltip(List.of(label));
        }
        field.child(labelComponent);
        field.child(input.horizontalSizing(Sizing.fill(100)));
        return field;
    }

    private static int compactNumericFieldWidth() {
        return clampWidth(guiWidth(), 0.065, 64, 104);
    }

    private static int compactTinyFieldWidth() {
        return clampWidth(guiWidth(), 0.05, 54, 80);
    }

    private static int compactIdTextWidth() {
        return clampWidth(guiWidth(), 0.22, 104, 220);
    }

    private static int compactGroupFieldWidth() {
        return clampWidth(guiWidth(), 0.18, 96, 190);
    }

    private static int compactPickerButtonWidth() {
        return clampWidth(guiWidth(), 0.16, 86, 150);
    }

    private static int compactIconButtonWidth() {
        return clampToPanelWidth(Math.max(COMPACT_ICON_BUTTON_MIN, UiFactory.scaledPixels(COMPACT_ICON_BUTTON_BASE)));
    }

    private static int compactClearButtonWidth() {
        return clampToPanelWidth(Math.max(COMPACT_CLEAR_BUTTON_MIN, UiFactory.scaledPixels(COMPACT_CLEAR_BUTTON_BASE)));
    }

    private static int compactRemoveButtonWidth() {
        return clampToPanelWidth(Math.max(COMPACT_REMOVE_BUTTON_MIN, UiFactory.scaledPixels(COMPACT_REMOVE_BUTTON_BASE)));
    }

    private static int compactFixedPickButtonWidth() {
        return clampToPanelWidth(Math.max(COMPACT_FIXED_PICK_BUTTON_MIN, UiFactory.scaledPixels(COMPACT_FIXED_PICK_BUTTON_BASE)));
    }

    private static int compactLongFieldWidth() {
        return clampWidth(guiWidth(), 0.26, 136, 280);
    }

    private static int clampWidth(int sourceWidth, double ratio, int min, int max) {
        int value = (int) Math.round(sourceWidth * ratio);
        int preferred = Math.clamp(value, min, max);
        return Math.clamp(preferred, 1, Math.max(1, sourceWidth));
    }

    private static int clampToPanelWidth(int preferredWidth) {
        return Math.clamp(preferredWidth, 1, Math.max(1, guiWidth()));
    }

    private static int guiWidth() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof ItemEditorScreen itemEditorScreen) {
            int hinted = itemEditorScreen.editorContentWidthHint();
            int reserve = Math.max(2, UiFactory.scaledPixels(PANEL_WIDTH_SAFETY_RESERVE));
            return Math.max(1, hinted - reserve);
        }
        return Math.max(1, minecraft.getWindow().getGuiScaledWidth());
    }

    private static boolean isNarrowLayout() {
        Minecraft minecraft = Minecraft.getInstance();
        double guiScale = minecraft.getWindow().getGuiScale();
        return guiWidth() <= NARROW_LAYOUT_WIDTH_THRESHOLD || guiScale >= NARROW_LAYOUT_SCALE_THRESHOLD;
    }

    private static boolean prefersStackedCompactRows() {
        Minecraft minecraft = Minecraft.getInstance();
        double guiScale = minecraft.getWindow().getGuiScale();
        return guiWidth() <= STACKED_COMPACT_WIDTH_THRESHOLD || guiScale >= STACKED_COMPACT_SCALE_THRESHOLD;
    }

    private static boolean usesStackedPickerRows() {
        int requiredInlineWidth = Math.max(
                PICKER_ROW_INLINE_MIN_WIDTH,
                UiFactory.scaledPixels(PICKER_ROW_INPUT_MIN_WIDTH)
                        + compactFixedPickButtonWidth()
                        + UiFactory.scaledPixels(SECTION_ROW_GAP)
        );
        return guiWidth() < requiredInlineWidth;
    }

    private static boolean usesStackedBlockStateRows() {
        return guiWidth() <= UiFactory.scaledPixels(BLOCK_STATE_STACKED_ROW_WIDTH_THRESHOLD);
    }

    private static int blockStateLabelWidth() {
        return clampWidth(guiWidth(), 0.10, 72, 128);
    }

    private static FlowLayout responsiveRow() {
        return prefersStackedCompactRows() ? UiFactory.column() : UiFactory.row();
    }

    private static FlowLayout denseEquipmentRow() {
        return guiWidth() <= EQUIPMENT_DENSE_STACK_WIDTH_THRESHOLD ? UiFactory.column() : UiFactory.row();
    }

    private static FlowLayout denseEquipmentRow(UIComponent... children) {
        FlowLayout row = denseEquipmentRow();
        if (children.length == 0) {
            return row;
        }
        if (guiWidth() <= EQUIPMENT_DENSE_STACK_WIDTH_THRESHOLD) {
            for (UIComponent child : children) {
                child.horizontalSizing(Sizing.fill(100));
                row.child(child);
            }
            return row;
        }
        int childWidth = distributedRowChildWidth(children.length);
        for (UIComponent child : children) {
            child.horizontalSizing(Sizing.fill(childWidth));
            row.child(child);
        }
        return row;
    }

    private static FlowLayout compactCheckboxRow(UIComponent... children) {
        boolean stacked = guiWidth() <= EQUIPMENT_DENSE_STACK_WIDTH_THRESHOLD;
        FlowLayout row = stacked ? UiFactory.column() : UiFactory.row();
        row.gap(Math.max(1, UiFactory.scaleProfile().tightSpacing()));
        for (UIComponent child : children) {
            child.horizontalSizing(stacked ? Sizing.fill(100) : Sizing.content());
            row.child(child);
        }
        return row;
    }

    private static void distributeRowChildren(FlowLayout row, UIComponent... children) {
        if (children.length == 0) {
            return;
        }
        if (prefersStackedCompactRows()) {
            for (UIComponent child : children) {
                child.horizontalSizing(Sizing.fill(100));
                row.child(child);
            }
            return;
        }
        int childWidth = distributedRowChildWidth(children.length);
        for (UIComponent child : children) {
            child.horizontalSizing(Sizing.fill(childWidth));
            row.child(child);
        }
    }

    private static int distributedRowChildWidth(int childCount) {
        return Math.max(1, (100 - Math.max(1, childCount)) / Math.max(1, childCount));
    }

    private static FlowLayout buildOnConsumeEffectsEditor(SpecialDataPanelContext context, ItemEditorState.SpecialData special) {
        return buildConsumeEffectsEditor(
                context,
                special.consumableOnConsumeEffects,
                "special.advanced.consumable.on_consume_effects",
                "special.advanced.consumable.add_effect",
                "special.advanced.consumable.effects_empty",
                "special.advanced.consumable.effect",
                false
        );
    }

    private static void setDeathProtectionEnabled(ItemEditorState.SpecialData special, boolean enabled) {
        special.deathProtection = enabled;
        if (enabled && special.deathProtectionEffects.isEmpty()) {
            addDefaultDeathProtectionEffects(special.deathProtectionEffects);
        }
    }

    private static void addDefaultDeathProtectionEffects(List<ItemEditorState.ConsumableEffectDraft> drafts) {
        ItemEditorState.ConsumableEffectDraft clearAll = new ItemEditorState.ConsumableEffectDraft();
        clearAll.type = ItemEditorState.ConsumableEffectDraft.TYPE_CLEAR_ALL_EFFECTS;
        drafts.add(clearAll);

        ItemEditorState.ConsumableEffectDraft apply = expandedConsumableEffectDraft();
        apply.effects.add(deathProtectionPotionEffect("minecraft:regeneration", 900, 1));
        apply.effects.add(deathProtectionPotionEffect("minecraft:absorption", 100, 1));
        apply.effects.add(deathProtectionPotionEffect("minecraft:fire_resistance", 800, 0));
        drafts.add(apply);
    }

    private static ItemEditorState.PotionEffectDraft deathProtectionPotionEffect(
            String effectId,
            int duration,
            int amplifier
    ) {
        ItemEditorState.PotionEffectDraft draft = new ItemEditorState.PotionEffectDraft();
        draft.effectId = effectId;
        draft.duration = Integer.toString(duration);
        draft.amplifier = Integer.toString(amplifier);
        return draft;
    }

    private static ItemEditorState.ConsumableEffectDraft expandedConsumableEffectDraft() {
        ItemEditorState.ConsumableEffectDraft draft = new ItemEditorState.ConsumableEffectDraft();
        draft.uiCollapsed = false;
        return draft;
    }

    private static ItemEditorState.BlocksAttacksDamageReductionDraft expandedBlocksAttacksDamageReductionDraft() {
        ItemEditorState.BlocksAttacksDamageReductionDraft draft = new ItemEditorState.BlocksAttacksDamageReductionDraft();
        draft.uiCollapsed = false;
        return draft;
    }

    private static FlowLayout buildDeathProtectionEffectsEditor(
            SpecialDataPanelContext context,
            ItemEditorState.SpecialData special
    ) {
        return buildConsumeEffectsEditor(
                context,
                special.deathProtectionEffects,
                "special.advanced.component_tweaks.death_effects",
                "special.advanced.component_tweaks.add_death_effect",
                "special.advanced.component_tweaks.death_effects_empty",
                "special.advanced.component_tweaks.death_effect",
                true
        );
    }

    private static FlowLayout buildConsumeEffectsEditor(
            SpecialDataPanelContext context,
            List<ItemEditorState.ConsumableEffectDraft> drafts,
            String titleKey,
            String addKey,
            String emptyKey,
            String effectTitleKey,
            boolean includeClearAllEffects
    ) {
        FlowLayout card = UiFactory.subCard();
        card.child(UiFactory.title(ItemEditorText.tr(titleKey)).shadow(false));

        ButtonComponent addButton = UiFactory.button(ItemEditorText.tr(addKey), UiFactory.ButtonTextPreset.STANDARD,  button ->
                context.mutateRefresh(() -> drafts.add(expandedConsumableEffectDraft()))
        );
        addButton.horizontalSizing(Sizing.fill(100));
        card.child(addButton);

        if (drafts.isEmpty()) {
            card.child(UiFactory.muted(ItemEditorText.tr(emptyKey), CONSUMABLE_EFFECTS_EMPTY_HINT_WIDTH));
            return card;
        }

        List<String> effectTypeValues = includeClearAllEffects
                ? List.of(
                        ItemEditorState.ConsumableEffectDraft.TYPE_CLEAR_ALL_EFFECTS,
                        ItemEditorState.ConsumableEffectDraft.TYPE_APPLY_EFFECTS,
                        ItemEditorState.ConsumableEffectDraft.TYPE_PLAY_SOUND
                )
                : List.of(
                        ItemEditorState.ConsumableEffectDraft.TYPE_APPLY_EFFECTS,
                        ItemEditorState.ConsumableEffectDraft.TYPE_PLAY_SOUND
                );
        List<String> effectIds = effectIds(context);
        List<String> sounds = soundIds(context);

        for (int index = 0; index < drafts.size(); index++) {
            int currentIndex = index;
            ItemEditorState.ConsumableEffectDraft draft = drafts.get(index);
            String currentType = draft.type == null || draft.type.isBlank()
                    ? ItemEditorState.ConsumableEffectDraft.TYPE_APPLY_EFFECTS
                    : draft.type;

            FlowLayout effectCard = context.createReorderableCard(
                    ItemEditorText.tr(effectTitleKey, index + 1),
                    currentIndex > 0,
                    () -> swapEntries(drafts, currentIndex, currentIndex - 1),
                    currentIndex < drafts.size() - 1,
                    () -> swapEntries(drafts, currentIndex, currentIndex + 1),
                    () -> drafts.remove(currentIndex)
            );
            FlowLayout collapseRow = responsiveRow();
            UIComponent summary = UiFactory.muted(Component.literal(consumableEffectSummary(draft, currentType)), CONSUMABLE_EFFECT_SUMMARY_HINT_WIDTH);
            summary.horizontalSizing(Sizing.expand(100));
            collapseRow.child(summary);
            ButtonComponent collapseToggle = UiFactory.button(Component.literal(draft.uiCollapsed ? SYMBOL_SECTION_COLLAPSED : SYMBOL_SECTION_EXPANDED), UiFactory.ButtonTextPreset.STANDARD,  button ->
                    context.mutateRefresh(() -> draft.uiCollapsed = !draft.uiCollapsed)
            );
            collapseToggle.horizontalSizing(Sizing.fixed(compactIconButtonWidth()));
            collapseRow.child(collapseToggle);
            effectCard.child(collapseRow);

            if (draft.uiCollapsed) {
                card.child(effectCard);
                continue;
            }

            effectCard.child(PickerFieldFactory.dropdownField(
                    context,
                    ItemEditorText.tr("special.advanced.consumable.effect_type"),
                    Component.empty(),
                    Component.literal(effectTypeLabel(currentType)),
                    240,
                    effectTypeValues,
                    AdvancedItemSpecialDataSection::effectTypeLabel,
                    selectedType -> context.mutateRefresh(() -> draft.type = selectedType)
            ));

            if (Objects.equals(currentType, ItemEditorState.ConsumableEffectDraft.TYPE_CLEAR_ALL_EFFECTS)) {
                card.child(effectCard);
                continue;
            }

            if (Objects.equals(currentType, ItemEditorState.ConsumableEffectDraft.TYPE_PLAY_SOUND)) {
                effectCard.child(UiFactory.field(
                        ItemEditorText.tr("special.advanced.consumable.effect_sound"),
                        Component.empty(),
                        idTextWithPicker(
                                context,
                                draft.soundId,
                                value -> draft.soundId = value,
                                sounds,
                                ItemEditorText.str("special.advanced.consumable.effect_sound")
                        )
                ));
                card.child(effectCard);
                continue;
            }

            effectCard.child(UiFactory.field(
                    ItemEditorText.tr("special.advanced.consumable.effect_probability"),
                    Component.empty(),
                UiFactory.textBox(draft.probability, context.bindText(value -> draft.probability = value)).horizontalSizing(Sizing.fill(100))
            ));

            ButtonComponent addPotionEffect = UiFactory.button(ItemEditorText.tr("special.potion.add_effect"), UiFactory.ButtonTextPreset.STANDARD,  button ->
                    context.mutateRefresh(() -> draft.effects.add(new ItemEditorState.PotionEffectDraft()))
            );
            addPotionEffect.horizontalSizing(Sizing.fill(100));
            effectCard.child(addPotionEffect);

            if (draft.effects.isEmpty()) {
                effectCard.child(UiFactory.muted(ItemEditorText.tr("special.advanced.consumable.apply_effects_empty"), CONSUMABLE_APPLY_EFFECTS_EMPTY_HINT_WIDTH));
                card.child(effectCard);
                continue;
            }

            for (int effectIndex = 0; effectIndex < draft.effects.size(); effectIndex++) {
                int currentEffectIndex = effectIndex;
                ItemEditorState.PotionEffectDraft effectDraft = draft.effects.get(effectIndex);

                FlowLayout potionCard = context.createRemovableCard(
                        ItemEditorText.tr("special.potion.effect", effectIndex + 1),
                        () -> draft.effects.remove(currentEffectIndex)
                );
                FlowLayout inputs = responsiveRow();
                UIComponent effectField = PickerFieldFactory.searchableField(
                        context,
                        ItemEditorText.tr("special.potion.effect_id"),
                        Component.empty(),
                        PickerFieldFactory.selectedOrFallback(effectDraft.effectId, ItemEditorText.tr("special.potion.select_effect")),
                        220,
                        ItemEditorText.str("special.potion.effect_id"),
                        "",
                        effectIds,
                        id -> id,
                        id -> context.mutateRefresh(() -> effectDraft.effectId = id)
                );
                UIComponent durationField = UiFactory.field(
                        ItemEditorText.tr("special.potion.duration"),
                        Component.empty(),
                        UiFactory.textBox(effectDraft.duration, context.bindText(value -> effectDraft.duration = value)).horizontalSizing(Sizing.fill(100))
                );
                UIComponent amplifierField = UiFactory.field(
                        ItemEditorText.tr("special.potion.amplifier"),
                        Component.empty(),
                        UiFactory.textBox(effectDraft.amplifier, context.bindText(value -> effectDraft.amplifier = value)).horizontalSizing(Sizing.fill(100))
                );
                distributeRowChildren(inputs, effectField, durationField, amplifierField);
                potionCard.child(inputs);

                UIComponent ambientToggle = UiFactory.checkbox(
                        ItemEditorText.tr("special.potion.ambient"),
                        effectDraft.ambient,
                        context.bindToggle(value -> effectDraft.ambient = value)
                );
                UIComponent visibleToggle = UiFactory.checkbox(
                        ItemEditorText.tr("special.potion.visible"),
                        effectDraft.visible,
                        context.bindToggle(value -> effectDraft.visible = value)
                );
                UIComponent iconToggle = UiFactory.checkbox(
                        ItemEditorText.tr("special.potion.show_icon"),
                        effectDraft.showIcon,
                        context.bindToggle(value -> effectDraft.showIcon = value)
                );
                potionCard.child(compactCheckboxRow(ambientToggle, visibleToggle, iconToggle));
                effectCard.child(potionCard);
            }

            card.child(effectCard);
        }

        return card;
    }

    private static String consumableEffectSummary(ItemEditorState.ConsumableEffectDraft draft, String currentType) {
        if (Objects.equals(currentType, ItemEditorState.ConsumableEffectDraft.TYPE_CLEAR_ALL_EFFECTS)) {
            return "clear_all_effects";
        }
        if (Objects.equals(currentType, ItemEditorState.ConsumableEffectDraft.TYPE_PLAY_SOUND)) {
            String sound = draft.soundId == null || draft.soundId.isBlank() ? "-" : draft.soundId;
            return "play_sound - " + sound;
        }
        int effectCount = draft.effects.size();
        String probability = draft.probability.isBlank() ? "1.0" : draft.probability;
        return "apply_effects - " + effectCount + " effects - p=" + probability;
    }

    private static String beeSummary(ItemEditorState.BeeOccupantDraft draft) {
        String entity = (draft.entityId == null || draft.entityId.isBlank()) ? "minecraft:bee" : draft.entityId;
        String ticks = (draft.ticksInHive == null || draft.ticksInHive.isBlank()) ? "0" : draft.ticksInHive;
        String minTicks = (draft.minTicksInHive == null || draft.minTicksInHive.isBlank()) ? "0" : draft.minTicksInHive;
        return entity + " - " + ticks + "/" + minTicks + " ticks";
    }

    private static String projectileSummary(ItemEditorState.ChargedProjectileDraft draft) {
        String item = (draft.itemId == null || draft.itemId.isBlank()) ? "-" : draft.itemId;
        String count = (draft.count == null || draft.count.isBlank()) ? "1" : draft.count;
        return item + " x" + count;
    }

    private static String mapDecorationSummary(ItemEditorState.MapDecorationDraft draft) {
        String key = (draft.key == null || draft.key.isBlank()) ? "-" : draft.key;
        String type = (draft.typeId == null || draft.typeId.isBlank()) ? "-" : draft.typeId;
        String x = (draft.x == null || draft.x.isBlank()) ? "0" : draft.x;
        String z = (draft.z == null || draft.z.isBlank()) ? "0" : draft.z;
        return key + " - " + type + " (" + x + ", " + z + ")";
    }

    private static int adjustNumericString(String raw, int delta) {
        int value = ContainerEntryDraftUtil.parseIntOrDefault(raw, 1);
        value += delta;
        return Math.max(1, value);
    }

    private static String effectTypeLabel(String effectTypeId) {
        if (Objects.equals(effectTypeId, ItemEditorState.ConsumableEffectDraft.TYPE_CLEAR_ALL_EFFECTS)) {
            return ItemEditorText.str("special.advanced.consumable.effect_type.clear_all_effects");
        }
        if (Objects.equals(effectTypeId, ItemEditorState.ConsumableEffectDraft.TYPE_PLAY_SOUND)) {
            return ItemEditorText.str("special.advanced.consumable.effect_type.play_sound");
        }
        if (Objects.equals(effectTypeId, ItemEditorState.ConsumableEffectDraft.TYPE_APPLY_EFFECTS)) {
            return ItemEditorText.str("special.advanced.consumable.effect_type.apply_effects");
        }
        return effectTypeId;
    }

    public static FlowLayout buildContainerMetadata(SpecialDataPanelContext context) {
        ItemEditorState.SpecialData special = context.special();
        List<String> availableItems = itemIds(context);
        return collapsibleCard(
                context,
                ItemEditorText.tr("special.advanced.container_meta.title"),
                special.uiContainerMetadataCollapsed,
                value -> special.uiContainerMetadataCollapsed = value,
                () -> {
                    FlowLayout content = UiFactory.column();
                    int idWidth = compactIdTextWidth();
                    int lockPredicateWidth = compactLongFieldWidth();
                    int seedWidth = compactNumericFieldWidth();
                    boolean narrowLayout = isNarrowLayout();
                    List<String> lootTables = lootTableIds(context);

                    if (narrowLayout) {
                        content.child(compactIdField(
                                context,
                                ItemEditorText.tr("special.advanced.container_meta.lock_item"),
                                special.lockItemId,
                                value -> special.lockItemId = value,
                                availableItems,
                                ItemEditorText.str("special.advanced.container_meta.lock_item"),
                                idWidth
                        ));
                        content.child(compactIdField(
                                context,
                                ItemEditorText.tr("special.advanced.container_meta.loot_table"),
                                special.containerLootTableId,
                                value -> special.containerLootTableId = value,
                                lootTables,
                                ItemEditorText.str("special.advanced.container_meta.loot_table"),
                                idWidth
                        ));
                    } else {
                        FlowLayout idRow = responsiveRow();
                        FlowLayout lockItemField = compactIdField(
                                context,
                                ItemEditorText.tr("special.advanced.container_meta.lock_item"),
                                special.lockItemId,
                                value -> special.lockItemId = value,
                                availableItems,
                                ItemEditorText.str("special.advanced.container_meta.lock_item"),
                                idWidth
                        );
                        FlowLayout lootTableField = compactIdField(
                                context,
                                ItemEditorText.tr("special.advanced.container_meta.loot_table"),
                                special.containerLootTableId,
                                value -> special.containerLootTableId = value,
                                lootTables,
                                ItemEditorText.str("special.advanced.container_meta.loot_table"),
                                idWidth
                        );
                        distributeRowChildren(idRow, lockItemField, lootTableField);
                        content.child(idRow);
                    }

                    content.child(compactTextField(
                            context,
                            ItemEditorText.tr("special.advanced.container_meta.lock_predicate"),
                            special.lockPredicateSnbt,
                            value -> special.lockPredicateSnbt = value,
                            lockPredicateWidth
                    ));
                    content.child(UiFactory.muted(ItemEditorText.tr("special.advanced.container_meta.lock_predicate_hint"), CONTAINER_META_LOCK_HINT_WIDTH));

                    content.child(compactTextField(
                            context,
                            ItemEditorText.tr("special.advanced.container_meta.loot_seed"),
                            special.containerLootSeed,
                            value -> special.containerLootSeed = value,
                            seedWidth
                    ));

                    content.child(buildBeesEditor(context, special));

                    content.child(buildPotDecorations(context, special, availableItems));
                    return content;
                }
        );
    }

    private static FlowLayout buildBeesEditor(SpecialDataPanelContext context, ItemEditorState.SpecialData special) {
        FlowLayout card = UiFactory.subCard();
        card.child(UiFactory.title(ItemEditorText.tr("special.advanced.container_meta.bees_title")).shadow(false));

        ButtonComponent addButton = UiFactory.button(ItemEditorText.tr("special.advanced.container_meta.bees_add"), UiFactory.ButtonTextPreset.STANDARD,  button ->
                context.mutateRefresh(() -> {
                    ItemEditorState.BeeOccupantDraft draft = new ItemEditorState.BeeOccupantDraft();
                    draft.uiCollapsed = false;
                    special.beesOccupants.add(draft);
                })
        );
        ButtonComponent clearAll = UiFactory.button(Component.literal(ACTION_CLEAR_ALL), UiFactory.ButtonTextPreset.STANDARD,  button ->
                context.mutateRefresh(special.beesOccupants::clear)
        );
        clearAll.active = !special.beesOccupants.isEmpty();
        card.child(UiFactory.actionButtonRow(addButton, clearAll));
        if (!special.beesOccupants.isEmpty()) {
            ButtonComponent expandAll = UiFactory.button(ItemEditorText.tr("common.expand_all"), UiFactory.ButtonTextPreset.STANDARD,  button ->
                    context.mutateRefresh(() -> special.beesOccupants.forEach(entry -> entry.uiCollapsed = false))
            );
            expandAll.tooltip(List.of(Component.literal(TOOLTIP_EXPAND_ALL_BEES)));

            ButtonComponent collapseAll = UiFactory.button(ItemEditorText.tr("common.collapse_all"), UiFactory.ButtonTextPreset.STANDARD,  button ->
                    context.mutateRefresh(() -> special.beesOccupants.forEach(entry -> entry.uiCollapsed = true))
            );
            collapseAll.tooltip(List.of(Component.literal(TOOLTIP_COLLAPSE_ALL_BEES)));

            card.child(UiFactory.actionButtonRow(expandAll, collapseAll));
        }

        if (special.beesOccupants.isEmpty()) {
            card.child(UiFactory.muted(ItemEditorText.tr("special.advanced.container_meta.bees_empty"), CONTAINER_META_BEES_EMPTY_HINT_WIDTH));
            return card;
        }

        int idWidth = compactIdTextWidth();
        int numberWidth = compactNumericFieldWidth();
        List<String> entityTypeIds = entityTypeIds(context);
        for (int index = 0; index < special.beesOccupants.size(); index++) {
            int currentIndex = index;
            ItemEditorState.BeeOccupantDraft draft = special.beesOccupants.get(index);
            FlowLayout beeCard = context.createReorderableCard(
                    ItemEditorText.tr("special.advanced.container_meta.bee", index + 1),
                    currentIndex > 0,
                    () -> swapEntries(special.beesOccupants, currentIndex, currentIndex - 1),
                    currentIndex < special.beesOccupants.size() - 1,
                    () -> swapEntries(special.beesOccupants, currentIndex, currentIndex + 1),
                    () -> special.beesOccupants.remove(currentIndex)
            );

            FlowLayout summaryRow = responsiveRow();
            UIComponent summary = UiFactory.muted(Component.literal(beeSummary(draft)), CONTAINER_META_BEE_SUMMARY_HINT_WIDTH);
            summary.horizontalSizing(Sizing.expand(100));
            summaryRow.child(summary);
            ButtonComponent collapseToggle = UiFactory.button(Component.literal(draft.uiCollapsed ? SYMBOL_SECTION_COLLAPSED : SYMBOL_SECTION_EXPANDED), UiFactory.ButtonTextPreset.STANDARD,  button ->
                    context.mutateRefresh(() -> draft.uiCollapsed = !draft.uiCollapsed)
            );
            collapseToggle.horizontalSizing(Sizing.fixed(compactIconButtonWidth()));
            summaryRow.child(collapseToggle);
            beeCard.child(summaryRow);

            if (!draft.uiCollapsed) {
                beeCard.child(compactIdField(
                        context,
                        ItemEditorText.tr("special.advanced.container_meta.bees_entity"),
                        draft.entityId,
                        value -> draft.entityId = value,
                        entityTypeIds,
                        ItemEditorText.str("special.advanced.container_meta.bees_entity"),
                        idWidth
                ));

                FlowLayout ticksField = compactTextField(
                        context,
                        ItemEditorText.tr("special.advanced.container_meta.bees_ticks"),
                        draft.ticksInHive,
                        value -> draft.ticksInHive = value,
                        numberWidth
                );
                FlowLayout minTicksField = compactTextField(
                        context,
                        ItemEditorText.tr("special.advanced.container_meta.bees_min_ticks"),
                        draft.minTicksInHive,
                        value -> draft.minTicksInHive = value,
                        numberWidth
                );
                beeCard.child(denseEquipmentRow(ticksField, minTicksField));
            }
            card.child(beeCard);
        }
        return card;
    }

    public static FlowLayout buildCrossbow(SpecialDataPanelContext context) {
        ItemEditorState.SpecialData special = context.special();
        List<String> availableItems = itemIds(context);
        return collapsibleCard(
                context,
                ItemEditorText.tr("special.advanced.crossbow.title"),
                special.uiCrossbowCollapsed,
                value -> special.uiCrossbowCollapsed = value,
                () -> {
                    FlowLayout content = UiFactory.column();
                    int idWidth = compactIdTextWidth();
                    int countWidth = compactTinyFieldWidth();

                    ButtonComponent addButton = UiFactory.button(ItemEditorText.tr("special.advanced.crossbow.add_projectile"), UiFactory.ButtonTextPreset.STANDARD,  button ->
                            context.mutateRefresh(() -> {
                                ItemEditorState.ChargedProjectileDraft draft = new ItemEditorState.ChargedProjectileDraft();
                                draft.uiCollapsed = false;
                                special.chargedProjectiles.add(draft);
                            })
                    );
                    addButton.horizontalSizing(Sizing.fill(100));
                    if (!special.chargedProjectiles.isEmpty()) {
                        ButtonComponent clearAll = UiFactory.button(Component.literal(ACTION_CLEAR_ALL), UiFactory.ButtonTextPreset.STANDARD,  button ->
                                context.mutateRefresh(special.chargedProjectiles::clear)
                        );
                        content.child(UiFactory.actionButtonRow(addButton, clearAll));

                        ButtonComponent expandAll = UiFactory.button(ItemEditorText.tr("common.expand_all"), UiFactory.ButtonTextPreset.STANDARD,  button ->
                                context.mutateRefresh(() -> special.chargedProjectiles.forEach(entry -> entry.uiCollapsed = false))
                        );
                        expandAll.tooltip(List.of(Component.literal(TOOLTIP_EXPAND_ALL_PROJECTILES)));

                        ButtonComponent collapseAll = UiFactory.button(ItemEditorText.tr("common.collapse_all"), UiFactory.ButtonTextPreset.STANDARD,  button ->
                                context.mutateRefresh(() -> special.chargedProjectiles.forEach(entry -> entry.uiCollapsed = true))
                        );
                        collapseAll.tooltip(List.of(Component.literal(TOOLTIP_COLLAPSE_ALL_PROJECTILES)));

                        content.child(UiFactory.actionButtonRow(expandAll, collapseAll));
                    } else {
                        content.child(addButton);
                    }

                    if (special.chargedProjectiles.isEmpty()) {
                        content.child(UiFactory.muted(
                                ItemEditorText.tr("special.advanced.crossbow.empty"),
                                CROSSBOW_EMPTY_HINT_WIDTH
                        ));
                        return content;
                    }

                    for (int index = 0; index < special.chargedProjectiles.size(); index++) {
                        int currentIndex = index;
                        ItemEditorState.ChargedProjectileDraft draft = special.chargedProjectiles.get(index);
                        FlowLayout card = UiFactory.reorderableCollapsibleSubCard(
                                ItemEditorText.tr("special.advanced.crossbow.projectile", index + 1),
                                Component.literal(projectileSummary(draft)),
                                CROSSBOW_PROJECTILE_SUMMARY_HINT_WIDTH,
                                draft.uiCollapsed,
                                () -> context.mutateRefresh(() -> draft.uiCollapsed = !draft.uiCollapsed),
                                currentIndex > 0,
                                () -> context.mutateRefresh(() -> swapEntries(special.chargedProjectiles, currentIndex, currentIndex - 1)),
                                currentIndex < special.chargedProjectiles.size() - 1,
                                () -> context.mutateRefresh(() -> swapEntries(special.chargedProjectiles, currentIndex, currentIndex + 1)),
                                () -> context.mutateRefresh(() -> special.chargedProjectiles.remove(currentIndex))
                        );

                        if (!draft.uiCollapsed) {
                            card.child(compactIdField(
                                    context,
                                    ItemEditorText.tr("special.advanced.crossbow.item"),
                                    draft.itemId,
                                    value -> draft.itemId = value,
                                    availableItems,
                                    ItemEditorText.str("special.advanced.crossbow.item"),
                                    idWidth
                            ));

                            FlowLayout countRow = responsiveRow();
                            FlowLayout countField = compactTextField(
                                    context,
                                    ItemEditorText.tr("special.advanced.crossbow.count"),
                                    draft.count,
                                    value -> draft.count = value,
                                    countWidth
                            );
                            ButtonComponent decrement = UiFactory.button(Component.literal(SYMBOL_STEP_DECREMENT), UiFactory.ButtonTextPreset.STANDARD,  button ->
                                    context.mutateRefresh(() -> draft.count = Integer.toString(adjustNumericString(draft.count, -1)))
                            );
                            decrement.horizontalSizing(Sizing.fixed(compactIconButtonWidth()));
                            ButtonComponent increment = UiFactory.button(Component.literal(SYMBOL_STEP_INCREMENT), UiFactory.ButtonTextPreset.STANDARD,  button ->
                                    context.mutateRefresh(() -> draft.count = Integer.toString(adjustNumericString(draft.count, 1)))
                            );
                            increment.horizontalSizing(Sizing.fixed(compactIconButtonWidth()));
                            distributeRowChildren(countRow, countField, decrement, increment);
                            card.child(countRow);
                        }
                        content.child(card);
                    }

                    return content;
                }
        );
    }

    public static FlowLayout buildMapAdvanced(SpecialDataPanelContext context) {
        ItemEditorState.SpecialData special = context.special();
        return collapsibleCard(
                context,
                ItemEditorText.tr("special.advanced.map.title"),
                special.uiMapAdvancedCollapsed,
                value -> special.uiMapAdvancedCollapsed = value,
                () -> {
                    FlowLayout content = UiFactory.column();
                    content.child(UiFactory.field(
                            ItemEditorText.tr("special.advanced.map.map_id"),
                            Component.empty(),
                        UiFactory.textBox(special.mapId, context.bindText(value -> special.mapId = value)).horizontalSizing(Sizing.fill(100))
                    ));
                    content.child(buildMapDecorationsEditor(context, special));
                    content.child(buildLodestoneEditor(context, special));
                    return content;
                }
        );
    }

    public static FlowLayout buildEquipmentCombat(SpecialDataPanelContext context) {
        ItemEditorState.SpecialData special = context.special();
        return collapsibleCard(
                context,
                ItemEditorText.tr("special.advanced.combat.title"),
                special.uiEquipmentCombatCollapsed,
                value -> special.uiEquipmentCombatCollapsed = value,
                () -> {
                    FlowLayout content = UiFactory.column();
                    content.child(buildEquippableCard(context, special));
                    content.child(buildWeaponCard(context, special));
                    content.child(buildToolCard(context, special));
                    content.child(buildRepairableCard(context, special));
                    content.child(buildAttackRangeCard(context, special));
                    return content;
                }
        );
    }

    private static FlowLayout buildPotDecorations(
            SpecialDataPanelContext context,
            ItemEditorState.SpecialData special,
            List<String> itemIds
    ) {
        FlowLayout card = UiFactory.subCard();
        card.child(UiFactory.title(ItemEditorText.tr("special.advanced.container_meta.pot_title")).shadow(false));
        int idWidth = compactIdTextWidth();
        boolean narrowLayout = isNarrowLayout();

        if (narrowLayout) {
            card.child(compactIdField(
                    context,
                    ItemEditorText.tr("special.advanced.container_meta.pot_back"),
                    special.potBackItemId,
                    value -> special.potBackItemId = value,
                    itemIds,
                    ItemEditorText.str("special.advanced.container_meta.pot_back"),
                    idWidth
            ));
            card.child(compactIdField(
                    context,
                    ItemEditorText.tr("special.advanced.container_meta.pot_left"),
                    special.potLeftItemId,
                    value -> special.potLeftItemId = value,
                    itemIds,
                    ItemEditorText.str("special.advanced.container_meta.pot_left"),
                    idWidth
            ));
            card.child(compactIdField(
                    context,
                    ItemEditorText.tr("special.advanced.container_meta.pot_right"),
                    special.potRightItemId,
                    value -> special.potRightItemId = value,
                    itemIds,
                    ItemEditorText.str("special.advanced.container_meta.pot_right"),
                    idWidth
            ));
            card.child(compactIdField(
                    context,
                    ItemEditorText.tr("special.advanced.container_meta.pot_front"),
                    special.potFrontItemId,
                    value -> special.potFrontItemId = value,
                    itemIds,
                    ItemEditorText.str("special.advanced.container_meta.pot_front"),
                    idWidth
            ));
            return card;
        }

        FlowLayout rowA = responsiveRow();
        FlowLayout backField = compactIdField(
                context,
                ItemEditorText.tr("special.advanced.container_meta.pot_back"),
                special.potBackItemId,
                value -> special.potBackItemId = value,
                itemIds,
                ItemEditorText.str("special.advanced.container_meta.pot_back"),
                idWidth
        );
        FlowLayout leftField = compactIdField(
                context,
                ItemEditorText.tr("special.advanced.container_meta.pot_left"),
                special.potLeftItemId,
                value -> special.potLeftItemId = value,
                itemIds,
                ItemEditorText.str("special.advanced.container_meta.pot_left"),
                idWidth
        );
        distributeRowChildren(rowA, backField, leftField);
        card.child(rowA);

        FlowLayout rowB = responsiveRow();
        FlowLayout rightField = compactIdField(
                context,
                ItemEditorText.tr("special.advanced.container_meta.pot_right"),
                special.potRightItemId,
                value -> special.potRightItemId = value,
                itemIds,
                ItemEditorText.str("special.advanced.container_meta.pot_right"),
                idWidth
        );
        FlowLayout frontField = compactIdField(
                context,
                ItemEditorText.tr("special.advanced.container_meta.pot_front"),
                special.potFrontItemId,
                value -> special.potFrontItemId = value,
                itemIds,
                ItemEditorText.str("special.advanced.container_meta.pot_front"),
                idWidth
        );
        distributeRowChildren(rowB, rightField, frontField);
        card.child(rowB);
        return card;
    }

    private static FlowLayout buildMapDecorationsEditor(SpecialDataPanelContext context, ItemEditorState.SpecialData special) {
        List<String> decorationTypeIds = mapDecorationTypeIds(context);
        FlowLayout card = UiFactory.subCard();
        card.child(UiFactory.title(ItemEditorText.tr("special.advanced.map.decorations_title")).shadow(false));

        int keyWidth = compactGroupFieldWidth();
        int idWidth = compactIdTextWidth();
        int numberWidth = compactNumericFieldWidth();

        ButtonComponent addButton = UiFactory.button(ItemEditorText.tr("special.advanced.map.add_decoration"), UiFactory.ButtonTextPreset.STANDARD,  button ->
                context.mutateRefresh(() -> {
                    ItemEditorState.MapDecorationDraft draft = new ItemEditorState.MapDecorationDraft();
                    draft.uiCollapsed = false;
                    special.mapDecorations.add(draft);
                })
        );
        ButtonComponent clearAll = UiFactory.button(Component.literal(ACTION_CLEAR_ALL), UiFactory.ButtonTextPreset.STANDARD,  button ->
                context.mutateRefresh(special.mapDecorations::clear)
        );
        clearAll.active = !special.mapDecorations.isEmpty();
        card.child(UiFactory.actionButtonRow(addButton, clearAll));
        if (!special.mapDecorations.isEmpty()) {
            ButtonComponent expandAll = UiFactory.button(ItemEditorText.tr("common.expand_all"), UiFactory.ButtonTextPreset.STANDARD,  button ->
                    context.mutateRefresh(() -> special.mapDecorations.forEach(entry -> entry.uiCollapsed = false))
            );
            expandAll.tooltip(List.of(Component.literal(TOOLTIP_EXPAND_ALL_DECORATIONS)));

            ButtonComponent collapseAll = UiFactory.button(ItemEditorText.tr("common.collapse_all"), UiFactory.ButtonTextPreset.STANDARD,  button ->
                    context.mutateRefresh(() -> special.mapDecorations.forEach(entry -> entry.uiCollapsed = true))
            );
            collapseAll.tooltip(List.of(Component.literal(TOOLTIP_COLLAPSE_ALL_DECORATIONS)));

            card.child(UiFactory.actionButtonRow(expandAll, collapseAll));
        }

        if (special.mapDecorations.isEmpty()) {
            card.child(UiFactory.muted(ItemEditorText.tr("special.advanced.map.decorations_empty"), MAP_DECORATIONS_EMPTY_HINT_WIDTH));
            return card;
        }

        for (int index = 0; index < special.mapDecorations.size(); index++) {
            int currentIndex = index;
            ItemEditorState.MapDecorationDraft draft = special.mapDecorations.get(index);
            FlowLayout entry = context.createReorderableCard(
                    ItemEditorText.tr("special.advanced.map.decoration", index + 1),
                    currentIndex > 0,
                    () -> swapEntries(special.mapDecorations, currentIndex, currentIndex - 1),
                    currentIndex < special.mapDecorations.size() - 1,
                    () -> swapEntries(special.mapDecorations, currentIndex, currentIndex + 1),
                    () -> special.mapDecorations.remove(currentIndex)
            );

            FlowLayout summaryRow = responsiveRow();
            UIComponent summary = UiFactory.muted(Component.literal(mapDecorationSummary(draft)), MAP_DECORATION_SUMMARY_HINT_WIDTH);
            summary.horizontalSizing(Sizing.expand(100));
            summaryRow.child(summary);
            ButtonComponent collapseToggle = UiFactory.button(Component.literal(draft.uiCollapsed ? SYMBOL_SECTION_COLLAPSED : SYMBOL_SECTION_EXPANDED), UiFactory.ButtonTextPreset.STANDARD,  button ->
                    context.mutateRefresh(() -> draft.uiCollapsed = !draft.uiCollapsed)
            );
            collapseToggle.horizontalSizing(Sizing.fixed(compactIconButtonWidth()));
            summaryRow.child(collapseToggle);
            entry.child(summaryRow);

            if (!draft.uiCollapsed) {
                entry.child(compactField(
                        ItemEditorText.tr("special.advanced.map.decoration_key"),
                        UiFactory.textBox(draft.key, context.bindText(value -> draft.key = value)).horizontalSizing(Sizing.fill(100)),
                        keyWidth + 40
                ));
                entry.child(compactIdField(
                        context,
                        ItemEditorText.tr("special.advanced.map.decoration_type"),
                        draft.typeId,
                        value -> draft.typeId = value,
                        decorationTypeIds,
                        ItemEditorText.str("special.advanced.map.decoration_type"),
                        idWidth
                ));

                FlowLayout position = responsiveRow();
                FlowLayout xField = compactTextField(
                        context,
                        ItemEditorText.tr("special.advanced.map.decoration_x"),
                        draft.x,
                        value -> draft.x = value,
                        numberWidth
                );
                FlowLayout zField = compactTextField(
                        context,
                        ItemEditorText.tr("special.advanced.map.decoration_z"),
                        draft.z,
                        value -> draft.z = value,
                        numberWidth
                );
                FlowLayout rotationField = compactTextField(
                        context,
                        ItemEditorText.tr("special.advanced.map.decoration_rotation"),
                        draft.rotation,
                        value -> draft.rotation = value,
                        numberWidth
                );
                distributeRowChildren(position, xField, zField, rotationField);
                entry.child(position);
            }
            card.child(entry);
        }

        return card;
    }

    private static FlowLayout buildLodestoneEditor(SpecialDataPanelContext context, ItemEditorState.SpecialData special) {
        FlowLayout card = UiFactory.subCard();
        card.child(UiFactory.title(ItemEditorText.tr("special.advanced.map.lodestone_title")).shadow(false));
        card.child(UiFactory.checkbox(
                ItemEditorText.tr("special.advanced.map.lodestone_enabled"),
                special.lodestoneEnabled,
                value -> context.mutateRefresh(() -> special.lodestoneEnabled = value)
        ));
        if (!special.lodestoneEnabled) {
            return card;
        }

        card.child(UiFactory.checkbox(
                ItemEditorText.tr("special.advanced.map.lodestone_tracked"),
                special.lodestoneTracked,
                context.bindToggle(value -> special.lodestoneTracked = value)
        ));
        card.child(UiFactory.field(
                ItemEditorText.tr("special.advanced.map.lodestone_dimension"),
                Component.empty(),
                idTextWithPicker(
                        context,
                        special.lodestoneDimensionId,
                        value -> special.lodestoneDimensionId = value,
                        dimensionIds(context),
                        ItemEditorText.str("special.advanced.map.lodestone_dimension")
                )
        ));

        int numberWidth = compactNumericFieldWidth();
        FlowLayout xField = compactTextField(
                context,
                ItemEditorText.tr("special.advanced.map.lodestone_x"),
                special.lodestoneX,
                value -> special.lodestoneX = value,
                numberWidth
        );
        FlowLayout yField = compactTextField(
                context,
                ItemEditorText.tr("special.advanced.map.lodestone_y"),
                special.lodestoneY,
                value -> special.lodestoneY = value,
                numberWidth
        );
        FlowLayout zField = compactTextField(
                context,
                ItemEditorText.tr("special.advanced.map.lodestone_z"),
                special.lodestoneZ,
                value -> special.lodestoneZ = value,
                numberWidth
        );
        card.child(denseEquipmentRow(xField, yField, zField));
        return card;
    }

    private static FlowLayout buildEquippableCard(SpecialDataPanelContext context, ItemEditorState.SpecialData special) {
        FlowLayout card = UiFactory.subCard();
        card.child(UiFactory.title(ItemEditorText.tr("special.advanced.combat.equippable_title")).shadow(false));
        int pickerWidth = compactPickerButtonWidth();
        int idWidth = compactIdTextWidth();

        ButtonComponent slotButton = UiFactory.button(
                PickerFieldFactory.selectedOrFallback(special.equippableSlot, ItemEditorText.tr("special.advanced.select")), UiFactory.ButtonTextPreset.STANDARD,
                anchor -> context.openClearableDropdown(
                        anchor,
                        ItemEditorText.tr("common.none"),
                        () -> context.mutate(() -> special.equippableSlot = ""),
                        Arrays.asList(EquipmentSlot.values()),
                        EquipmentSlot::name,
                        slot -> context.mutate(() -> special.equippableSlot = slot.name())
                )
        );
        slotButton.horizontalSizing(Sizing.fill(100));
        card.child(compactField(
                ItemEditorText.tr("special.advanced.combat.equippable_slot"),
                slotButton,
                pickerWidth + 40
        ));

        List<String> sounds = soundIds(context);
        card.child(compactIdField(
                context,
                ItemEditorText.tr("special.advanced.combat.equippable_sound"),
                special.equippableEquipSoundId,
                value -> special.equippableEquipSoundId = value,
                sounds,
                ItemEditorText.str("special.advanced.combat.equippable_sound"),
                idWidth
        ));

        card.child(compactIdField(
                context,
                ItemEditorText.tr("special.advanced.combat.equippable_shearing_sound"),
                special.equippableShearingSoundId,
                value -> special.equippableShearingSoundId = value,
                sounds,
                ItemEditorText.str("special.advanced.combat.equippable_shearing_sound"),
                idWidth
        ));
        card.child(compactField(
                ItemEditorText.tr("special.advanced.combat.equippable_asset_id"),
                UiFactory.textBox(special.equippableAssetId, context.bindText(value -> special.equippableAssetId = value))
                .horizontalSizing(Sizing.fill(100)),
                compactLongFieldWidth() + 40
        ));

        card.child(compactField(
                ItemEditorText.tr("special.advanced.combat.equippable_camera_overlay"),
                UiFactory.textBox(special.equippableCameraOverlayId, context.bindText(value -> special.equippableCameraOverlayId = value))
                .horizontalSizing(Sizing.fill(100)),
                compactLongFieldWidth() + 40
        ));

        UIComponent dispensable = equippableCheckbox(
                context,
                "dispensable",
                special.equippableDispensable,
                value -> special.equippableDispensable = value
        );
        UIComponent swappable = equippableCheckbox(
                context,
                "swappable",
                special.equippableSwappable,
                value -> special.equippableSwappable = value
        );
        UIComponent damageOnHurt = equippableCheckbox(
                context,
                "damage_on_hurt",
                special.equippableDamageOnHurt,
                value -> special.equippableDamageOnHurt = value
        );
        UIComponent equipOnInteract = equippableCheckbox(
                context,
                "equip_on_interact",
                special.equippableEquipOnInteract,
                value -> special.equippableEquipOnInteract = value
        );
        UIComponent canBeSheared = equippableCheckbox(
                context,
                "can_be_sheared",
                special.equippableCanBeSheared,
                value -> special.equippableCanBeSheared = value
        );
        if (guiWidth() >= EQUIPMENT_FLAGS_SINGLE_ROW_WIDTH_THRESHOLD) {
            card.child(compactCheckboxRow(dispensable, swappable, damageOnHurt, equipOnInteract, canBeSheared));
        } else {
            card.child(compactCheckboxRow(dispensable, swappable, damageOnHurt));
            card.child(compactCheckboxRow(equipOnInteract, canBeSheared));
        }

        return card;
    }

    private static UIComponent equippableCheckbox(
            SpecialDataPanelContext context,
            String labelSuffix,
            boolean selected,
            Consumer<Boolean> setter
    ) {
        return UiFactory.checkbox(
                ItemEditorText.tr("special.advanced.combat.equippable_" + labelSuffix),
                selected,
                context.bindToggle(setter)
        );
    }

    private static FlowLayout buildWeaponCard(SpecialDataPanelContext context, ItemEditorState.SpecialData special) {
        FlowLayout card = UiFactory.subCard();
        card.child(UiFactory.title(ItemEditorText.tr("special.advanced.combat.weapon_title")).shadow(false));
        int numberWidth = compactNumericFieldWidth();

        FlowLayout damageField = compactField(
                ItemEditorText.tr("special.advanced.combat.weapon_damage"),
                UiFactory.textBox(special.weaponItemDamagePerAttack, context.bindText(value -> special.weaponItemDamagePerAttack = value))
                .horizontalSizing(Sizing.fill(100)),
                numberWidth + 40
        );
        FlowLayout disableField = compactField(
                ItemEditorText.tr("special.advanced.combat.weapon_disable"),
                UiFactory.textBox(special.weaponDisableBlockingForSeconds, context.bindText(value -> special.weaponDisableBlockingForSeconds = value))
                .horizontalSizing(Sizing.fill(100)),
                numberWidth + 40
        );
        card.child(denseEquipmentRow(damageField, disableField));
        return card;
    }

    private static FlowLayout buildToolCard(SpecialDataPanelContext context, ItemEditorState.SpecialData special) {
        FlowLayout card = UiFactory.subCard();
        card.child(UiFactory.title(ItemEditorText.tr("special.advanced.combat.tool_title")).shadow(false));
        int numberWidth = compactNumericFieldWidth();

        FlowLayout speedField = compactField(
                ItemEditorText.tr("special.advanced.combat.tool_speed"),
                UiFactory.textBox(special.toolDefaultMiningSpeed, context.bindText(value -> special.toolDefaultMiningSpeed = value))
                .horizontalSizing(Sizing.fill(100)),
                numberWidth + 40
        );
        FlowLayout damageField = compactField(
                ItemEditorText.tr("special.advanced.combat.tool_damage"),
                UiFactory.textBox(special.toolDamagePerBlock, context.bindText(value -> special.toolDamagePerBlock = value))
                .horizontalSizing(Sizing.fill(100)),
                numberWidth + 40
        );
        UIComponent creativeField = UiFactory.checkbox(
                ItemEditorText.tr("special.advanced.combat.tool_creative"),
                special.toolCanDestroyBlocksInCreative,
                context.bindToggle(value -> special.toolCanDestroyBlocksInCreative = value)
        );
        card.child(denseEquipmentRow(speedField, damageField, creativeField));
        return card;
    }

    private static FlowLayout buildRepairableCard(SpecialDataPanelContext context, ItemEditorState.SpecialData special) {
        FlowLayout card = UiFactory.subCard();
        card.child(UiFactory.title(ItemEditorText.tr("special.advanced.combat.repairable_title")).shadow(false));
        int idWidth = compactIdTextWidth();
        List<String> availableItems = itemIds(context);

        ButtonComponent addButton = UiFactory.button(ItemEditorText.tr("special.advanced.combat.repairable_add"), UiFactory.ButtonTextPreset.STANDARD,  button ->
                context.mutateRefresh(() -> special.repairableItemIds.add(""))
        );
        addButton.horizontalSizing(Sizing.fill(100));
        card.child(addButton);

        if (special.repairableItemIds.isEmpty()) {
            card.child(UiFactory.muted(ItemEditorText.tr("special.advanced.combat.repairable_empty"), COMBAT_REPAIRABLE_EMPTY_HINT_WIDTH));
            return card;
        }

        for (int index = 0; index < special.repairableItemIds.size(); index++) {
            int currentIndex = index;
            String value = special.repairableItemIds.get(index);
            FlowLayout row = denseEquipmentRow();
            FlowLayout itemField = compactIdField(
                    context,
                    ItemEditorText.tr("special.advanced.combat.repair_item"),
                    value,
                    newValue -> special.repairableItemIds.set(currentIndex, newValue),
                    availableItems,
                    ItemEditorText.str("special.advanced.combat.repair_item"),
                    idWidth
            );
            ButtonComponent remove = UiFactory.button(ItemEditorText.tr("common.remove"), UiFactory.ButtonTextPreset.STANDARD,  button ->
                    context.mutateRefresh(() -> special.repairableItemIds.remove(currentIndex))
            );
            FlowLayout removeField = compactField(Component.literal(" "), remove, compactRemoveButtonWidth());
            itemField.horizontalSizing(Sizing.expand(100));
            removeField.horizontalSizing(Sizing.fixed(compactRemoveButtonWidth()));
            row.child(itemField);
            row.child(removeField);
            card.child(row);
        }
        return card;
    }

    private static FlowLayout buildAttackRangeCard(SpecialDataPanelContext context, ItemEditorState.SpecialData special) {
        FlowLayout card = UiFactory.subCard();
        card.child(UiFactory.title(ItemEditorText.tr("special.advanced.combat.attack_range_title")).shadow(false));
        int numberWidth = compactNumericFieldWidth();

        FlowLayout minReach = compactTextField(context, ItemEditorText.tr("special.advanced.combat.range_min_reach"), special.attackRangeMinReach, value -> special.attackRangeMinReach = value, numberWidth);
        FlowLayout maxReach = compactTextField(context, ItemEditorText.tr("special.advanced.combat.range_max_reach"), special.attackRangeMaxReach, value -> special.attackRangeMaxReach = value, numberWidth);
        FlowLayout minCreative = compactTextField(context, ItemEditorText.tr("special.advanced.combat.range_min_creative"), special.attackRangeMinCreativeReach, value -> special.attackRangeMinCreativeReach = value, numberWidth);
        FlowLayout maxCreative = compactTextField(context, ItemEditorText.tr("special.advanced.combat.range_max_creative"), special.attackRangeMaxCreativeReach, value -> special.attackRangeMaxCreativeReach = value, numberWidth);
        card.child(denseEquipmentRow(minReach, maxReach, minCreative, maxCreative));

        FlowLayout hitbox = compactTextField(context, ItemEditorText.tr("special.advanced.combat.range_hitbox"), special.attackRangeHitboxMargin, value -> special.attackRangeHitboxMargin = value, numberWidth);
        FlowLayout mobFactor = compactTextField(context, ItemEditorText.tr("special.advanced.combat.range_mob_factor"), special.attackRangeMobFactor, value -> special.attackRangeMobFactor = value, numberWidth);
        card.child(denseEquipmentRow(hitbox, mobFactor));
        return card;
    }

    private static FlowLayout buildComponentTweakNamingSection(SpecialDataPanelContext context, ItemEditorState.SpecialData special) {
        return collapsibleCard(
                context,
                ItemEditorText.tr("special.advanced.component_tweaks.naming_title"),
                special.uiComponentTweaksNamingCollapsed,
                value -> special.uiComponentTweaksNamingCollapsed = value,
                () -> {
                    FlowLayout content = UiFactory.column();
                    content.child(buildNamingAndStackCard(context, special));
                    return content;
                }
        );
    }

    private static FlowLayout buildComponentTweakRegistrySection(SpecialDataPanelContext context, ItemEditorState.SpecialData special) {
        return collapsibleCard(
                context,
                ItemEditorText.tr("special.advanced.component_tweaks.registry_title"),
                special.uiComponentTweaksRegistryCollapsed,
                value -> special.uiComponentTweaksRegistryCollapsed = value,
                () -> {
                    FlowLayout content = UiFactory.column();
                    content.child(buildRegistryAndFlagsCard(context, special));
                    return content;
                }
        );
    }

    private static FlowLayout buildBlockState(SpecialDataPanelContext context, ItemEditorState.SpecialData special) {
        return collapsibleCard(
                context,
                ItemEditorText.tr("special.advanced.block_state.title"),
                special.uiBlockStateCollapsed,
                value -> special.uiBlockStateCollapsed = value,
                () -> {
                    FlowLayout content = UiFactory.column();
                    content.child(buildBlockStateCard(context, special));
                    return content;
                }
        );
    }

    private static FlowLayout buildComponentTweakBlockSection(SpecialDataPanelContext context, ItemEditorState.SpecialData special) {
        return collapsibleCard(
                context,
                ItemEditorText.tr("special.advanced.component_tweaks.block_title"),
                special.uiComponentTweaksBlockCollapsed,
                value -> special.uiComponentTweaksBlockCollapsed = value,
                () -> {
                    FlowLayout content = UiFactory.column();
                    content.child(buildBlockComponentsCard(context, special));
                    return content;
                }
        );
    }

    private static FlowLayout buildComponentTweakBehaviorSection(SpecialDataPanelContext context, ItemEditorState.SpecialData special) {
        return collapsibleCard(
                context,
                ItemEditorText.tr("special.advanced.component_tweaks.behavior_title"),
                special.uiComponentTweaksBehaviorCollapsed,
                value -> special.uiComponentTweaksBehaviorCollapsed = value,
                () -> {
                    FlowLayout content = UiFactory.column();
                    content.child(buildCombatBehaviorCard(context, special));
                    return content;
                }
        );
    }

    private static FlowLayout buildNamingAndStackCard(SpecialDataPanelContext context, ItemEditorState.SpecialData special) {
        FlowLayout card = UiFactory.subCard();
        int numericWidth = compactNumericFieldWidth();
        int mediumWidth = compactGroupFieldWidth();
        int longWidth = compactLongFieldWidth();

        card.child(compactField(
                ItemEditorText.tr("special.advanced.component_tweaks.item_name"),
                UiFactory.textBox(special.itemName, context.bindText(value -> special.itemName = value))
                .horizontalSizing(Sizing.fill(100)),
                longWidth + 40
        ));

        FlowLayout minAttackField = compactField(
                ItemEditorText.tr("special.advanced.component_tweaks.min_attack_charge"),
                UiFactory.textBox(special.minimumAttackCharge, context.bindText(value -> special.minimumAttackCharge = value))
                .horizontalSizing(Sizing.fill(100)),
                numericWidth + 40
        );
        FlowLayout enchantableField = compactField(
                ItemEditorText.tr("special.advanced.component_tweaks.enchantable"),
                UiFactory.textBox(special.enchantableValue, context.bindText(value -> special.enchantableValue = value))
                .horizontalSizing(Sizing.fill(100)),
                numericWidth + 40
        );
        FlowLayout ominousField = compactField(
                ItemEditorText.tr("special.advanced.component_tweaks.ominous_amplifier"),
                UiFactory.textBox(special.ominousBottleAmplifier, context.bindText(value -> special.ominousBottleAmplifier = value))
                .horizontalSizing(Sizing.fill(100)),
                numericWidth + 40
        );

        card.child(denseEquipmentRow(minAttackField, enchantableField, ominousField));

        card.child(compactField(
                ItemEditorText.tr("special.advanced.component_tweaks.tooltip_style"),
                UiFactory.textBox(special.tooltipStyleId, context.bindText(value -> special.tooltipStyleId = value))
                .horizontalSizing(Sizing.fill(100)),
                mediumWidth + 40
        ));

        UIComponent gliderToggle = UiFactory.checkbox(
                ItemEditorText.tr("special.advanced.component_tweaks.glider"),
                special.glider,
                context.bindToggle(value -> special.glider = value)
        );
        UIComponent intangibleToggle = UiFactory.checkbox(
                ItemEditorText.tr("special.advanced.component_tweaks.intangible_projectile"),
                special.intangibleProjectile,
                context.bindToggle(value -> special.intangibleProjectile = value)
        );
        UIComponent deathProtectionToggle = UiFactory.checkbox(
                ItemEditorText.tr("special.advanced.component_tweaks.death_protection"),
                special.deathProtection,
                value -> context.mutateRefresh(() -> setDeathProtectionEnabled(special, value))
        );

        card.child(compactCheckboxRow(gliderToggle, intangibleToggle, deathProtectionToggle));
        if (special.deathProtection) {
            card.child(buildDeathProtectionEffectsEditor(context, special));
        }
        return card;
    }

    private static FlowLayout buildRegistryAndFlagsCard(SpecialDataPanelContext context, ItemEditorState.SpecialData special) {
        FlowLayout card = UiFactory.subCard();
        int idWidth = compactIdTextWidth();
        int longWidth = compactLongFieldWidth();

        FlowLayout damageRow = responsiveRow();
        FlowLayout damageTypeField = compactIdField(
                context,
                ItemEditorText.tr("special.advanced.component_tweaks.damage_type"),
                special.damageTypeId,
                value -> special.damageTypeId = value,
                damageTypeIds(context),
                ItemEditorText.str("special.advanced.component_tweaks.damage_type"),
                idWidth
        );
        FlowLayout damageResistantField = damageResistantTagField(context, special, longWidth);
        distributeRowChildren(damageRow, damageTypeField, damageResistantField);
        card.child(damageRow);

        FlowLayout soundRow = responsiveRow();
        FlowLayout noteBlockSoundField = compactIdField(
                context,
                ItemEditorText.tr("special.advanced.component_tweaks.note_block_sound"),
                special.noteBlockSoundId,
                value -> special.noteBlockSoundId = value,
                soundIds(context),
                ItemEditorText.str("special.advanced.component_tweaks.note_block_sound"),
                idWidth
        );
        FlowLayout breakSoundField = compactIdField(
                context,
                ItemEditorText.tr("special.advanced.component_tweaks.break_sound"),
                special.breakSoundId,
                value -> special.breakSoundId = value,
                soundIds(context),
                ItemEditorText.str("special.advanced.component_tweaks.break_sound"),
                idWidth
        );
        distributeRowChildren(soundRow, noteBlockSoundField, breakSoundField);
        card.child(soundRow);

        card.child(compactIdField(
                context,
                ItemEditorText.tr("special.advanced.component_tweaks.painting_variant"),
                special.paintingVariantId,
                value -> special.paintingVariantId = value,
                paintingVariantIds(context),
                ItemEditorText.str("special.advanced.component_tweaks.painting_variant"),
                idWidth
        ));
        return card;
    }

    private static FlowLayout damageResistantTagField(
            SpecialDataPanelContext context,
            ItemEditorState.SpecialData special,
            int textWidth
    ) {
        return compactField(
                ItemEditorText.tr("special.advanced.component_tweaks.damage_resistant_types"),
                damageTypeHolderSetEditor(
                        context,
                        special.damageResistantTypeIds,
                        value -> special.damageResistantTypeIds = value,
                        () -> special.damageResistantTypeIds,
                        ItemEditorText.str("special.advanced.component_tweaks.damage_resistant_types"),
                        special.uiDamageResistantTypesCollapsed,
                        value -> special.uiDamageResistantTypesCollapsed = value,
                        special.allowDamageResistantTagExpansion,
                        value -> special.allowDamageResistantTagExpansion = value
                ),
                textWidth + 70
        );
    }

    private static FlowLayout buildBlockStateCard(SpecialDataPanelContext context, ItemEditorState.SpecialData special) {
        FlowLayout card = UiFactory.subCard();
        List<BlockStatePropertyMeta> availableProperties = blockStatePropertyMeta(context);

        card.child(UiFactory.title(ItemEditorText.tr("special.advanced.component_tweaks.block_state")).shadow(false));

        Map<String, String> currentValues = parseBlockStatePropertyMap(special.blockStateProperties);
        FlowLayout stateActions = UiFactory.row();
        stateActions.horizontalAlignment(HorizontalAlignment.RIGHT);
        stateActions.gap(Math.max(1, UiFactory.scaleProfile().tightSpacing()));
        ButtonComponent clearProperties = UiFactory.button(ItemEditorText.tr("common.reset"), UiFactory.ButtonTextPreset.COMPACT,  button ->
                context.mutateRefresh(() -> special.blockStateProperties = "")
        );
        clearProperties.active(!currentValues.isEmpty());
        clearProperties.horizontalSizing(Sizing.fixed(compactClearButtonWidth()));
        card.child(stateActions);
        stateActions.child(clearProperties);

        for (BlockStatePropertyMeta property : availableProperties) {
            UIComponent entry = blockStatePropertyRow(context, special, currentValues, property);
            entry.horizontalSizing(Sizing.fill(100));
            card.child(entry);
        }
        return card;
    }

    private static FlowLayout blockStatePropertyRow(
            SpecialDataPanelContext context,
            ItemEditorState.SpecialData special,
            Map<String, String> currentValues,
            BlockStatePropertyMeta property
    ) {
        String currentValue = selectedBlockStateValue(currentValues, property);
        boolean hasOverride = currentValues.containsKey(property.key()) && !currentValues.getOrDefault(property.key(), "").isBlank();
        boolean stacked = usesStackedBlockStateRows();
        FlowLayout row = stacked ? UiFactory.column() : UiFactory.row();
        row.gap(Math.max(1, UiFactory.scaleProfile().tightSpacing()));

        int labelWidth = blockStateLabelWidth();
        Component labelText = Component.literal(property.key());
        Component fittedLabel = UiFactory.fitToWidth(labelText, labelWidth);
        var label = UiFactory.muted(fittedLabel, labelWidth);
        if (!Objects.equals(fittedLabel.getString(), labelText.getString())) {
            label.tooltip(List.of(labelText));
        }
        label.horizontalSizing(stacked ? Sizing.fill(100) : Sizing.fixed(labelWidth));
        row.child(label);

        ButtonComponent valueButton = UiFactory.button(
                Component.literal(currentValue),
                UiFactory.ButtonTextPreset.STANDARD,
                anchor -> context.openDropdown(
                        anchor,
                        property.values(),
                        value -> value,
                        value -> context.mutateRefresh(() -> setBlockStateProperty(special, property.key(), value))
                )
        );
        valueButton.active(!property.values().isEmpty());
        valueButton.horizontalSizing(stacked
                ? Sizing.fill(100)
                : Sizing.fill(hasOverride
                        ? BLOCK_STATE_VALUE_WITH_RESET_WIDTH_PERCENT
                        : BLOCK_STATE_VALUE_WIDTH_PERCENT));
        row.child(valueButton);

        if (hasOverride) {
            ButtonComponent resetButton = UiFactory.button(ItemEditorText.tr("common.reset"), UiFactory.ButtonTextPreset.COMPACT, button ->
                    context.mutateRefresh(() -> removeBlockStateProperty(special, property.key()))
            );
            resetButton.horizontalSizing(stacked ? Sizing.fill(100) : Sizing.fixed(compactClearButtonWidth()));
            row.child(resetButton);
        }
        return row;
    }

    private static FlowLayout buildBlockComponentsCard(SpecialDataPanelContext context, ItemEditorState.SpecialData special) {
        FlowLayout card = UiFactory.subCard();
        int numericWidth = compactNumericFieldWidth();
        int idWidth = compactIdTextWidth();

        FlowLayout blockDelayField = compactField(
                ItemEditorText.tr("special.advanced.component_tweaks.blocks_attacks_delay"),
                UiFactory.textBox(special.blocksAttacksBlockDelaySeconds, context.bindText(value -> special.blocksAttacksBlockDelaySeconds = value))
                .horizontalSizing(Sizing.fill(100)),
                numericWidth + 40
        );
        FlowLayout disableScaleField = compactField(
                ItemEditorText.tr("special.advanced.component_tweaks.blocks_attacks_disable_scale"),
                UiFactory.textBox(special.blocksAttacksDisableCooldownScale, context.bindText(value -> special.blocksAttacksDisableCooldownScale = value))
                .horizontalSizing(Sizing.fill(100)),
                numericWidth + 40
        );
        card.child(denseEquipmentRow(blockDelayField, disableScaleField));

        card.child(buildBlocksAttacksDamageReductionsEditor(context, special));
        card.child(buildBlocksAttacksItemDamageCard(context, special));
        card.child(compactField(
                ItemEditorText.tr("special.advanced.component_tweaks.blocks_attacks_bypassed_by"),
                damageTypeHolderSetEditor(
                        context,
                        special.blocksAttacksBypassedByTypeIds,
                        value -> special.blocksAttacksBypassedByTypeIds = value,
                        () -> special.blocksAttacksBypassedByTypeIds,
                        ItemEditorText.str("special.advanced.component_tweaks.blocks_attacks_bypassed_by"),
                        special.uiBlocksAttacksBypassedByTypesCollapsed,
                        value -> special.uiBlocksAttacksBypassedByTypesCollapsed = value,
                        special.allowBlocksAttacksBypassedByTagExpansion,
                        value -> special.allowBlocksAttacksBypassedByTagExpansion = value
                ),
                idWidth + 80
        ));
        card.child(compactIdField(
                context,
                ItemEditorText.tr("special.advanced.component_tweaks.blocks_attacks_block_sound"),
                special.blocksAttacksBlockSoundId,
                value -> special.blocksAttacksBlockSoundId = value,
                soundIds(context),
                ItemEditorText.str("special.advanced.component_tweaks.blocks_attacks_block_sound"),
                idWidth
        ));
        card.child(compactIdField(
                context,
                ItemEditorText.tr("special.advanced.component_tweaks.blocks_attacks_disable_sound"),
                special.blocksAttacksDisableSoundId,
                value -> special.blocksAttacksDisableSoundId = value,
                soundIds(context),
                ItemEditorText.str("special.advanced.component_tweaks.blocks_attacks_disable_sound"),
                idWidth
        ));
        return card;
    }

    private static FlowLayout buildBlocksAttacksDamageReductionsEditor(
            SpecialDataPanelContext context,
            ItemEditorState.SpecialData special
    ) {
        FlowLayout card = UiFactory.subCard();
        card.child(UiFactory.title(ItemEditorText.tr(
                "special.advanced.component_tweaks.blocks_attacks_damage_reductions"
        )).shadow(false));

        ButtonComponent addButton = UiFactory.button(
                ItemEditorText.tr("special.advanced.component_tweaks.blocks_attacks_add_damage_reduction"),
                UiFactory.ButtonTextPreset.STANDARD,
                button -> context.mutateRefresh(() ->
                        special.blocksAttacksDamageReductions.add(expandedBlocksAttacksDamageReductionDraft()))
        );
        addButton.horizontalSizing(Sizing.fill(100));
        card.child(addButton);

        if (special.blocksAttacksDamageReductions.isEmpty()) {
            card.child(UiFactory.muted(
                    ItemEditorText.tr("special.advanced.component_tweaks.blocks_attacks_damage_reductions_empty"),
                    BLOCKS_ATTACKS_REDUCTION_EMPTY_HINT_WIDTH
            ));
            return card;
        }

        int idWidth = compactIdTextWidth();
        int numericWidth = compactTinyFieldWidth();
        for (int index = 0; index < special.blocksAttacksDamageReductions.size(); index++) {
            int currentIndex = index;
            ItemEditorState.BlocksAttacksDamageReductionDraft draft = special.blocksAttacksDamageReductions.get(index);
            FlowLayout reductionCard = context.createReorderableCard(
                    ItemEditorText.tr("special.advanced.component_tweaks.blocks_attacks_damage_reduction", index + 1),
                    currentIndex > 0,
                    () -> swapEntries(special.blocksAttacksDamageReductions, currentIndex, currentIndex - 1),
                    currentIndex < special.blocksAttacksDamageReductions.size() - 1,
                    () -> swapEntries(special.blocksAttacksDamageReductions, currentIndex, currentIndex + 1),
                    () -> special.blocksAttacksDamageReductions.remove(currentIndex)
            );

            FlowLayout summaryRow = responsiveRow();
            UIComponent summary = UiFactory.muted(
                    Component.literal(blocksAttacksDamageReductionSummary(draft)),
                    BLOCKS_ATTACKS_REDUCTION_SUMMARY_HINT_WIDTH
            );
            summary.horizontalSizing(Sizing.expand(100));
            summaryRow.child(summary);
            ButtonComponent collapseToggle = UiFactory.button(
                    Component.literal(draft.uiCollapsed ? SYMBOL_SECTION_COLLAPSED : SYMBOL_SECTION_EXPANDED),
                    UiFactory.ButtonTextPreset.STANDARD,
                    button -> context.mutateRefresh(() -> draft.uiCollapsed = !draft.uiCollapsed)
            );
            collapseToggle.horizontalSizing(Sizing.fixed(compactIconButtonWidth()));
            summaryRow.child(collapseToggle);
            reductionCard.child(summaryRow);

            if (!draft.uiCollapsed) {
                reductionCard.child(compactField(
                        ItemEditorText.tr("special.advanced.component_tweaks.blocks_attacks_reduction_types"),
                        damageTypeHolderSetEditor(
                                context,
                                draft.typeIds,
                                value -> draft.typeIds = value,
                                () -> draft.typeIds,
                                ItemEditorText.str("special.advanced.component_tweaks.blocks_attacks_reduction_types"),
                                false,
                                null,
                                draft.allowTagExpansion,
                                value -> draft.allowTagExpansion = value
                        ),
                        idWidth + 80
                ));
                FlowLayout angle = compactTextField(
                        context,
                        ItemEditorText.tr("special.advanced.component_tweaks.blocks_attacks_reduction_angle"),
                        draft.horizontalBlockingAngle,
                        value -> draft.horizontalBlockingAngle = value,
                        numericWidth
                );
                FlowLayout base = compactTextField(
                        context,
                        ItemEditorText.tr("special.advanced.component_tweaks.blocks_attacks_reduction_base"),
                        draft.base,
                        value -> draft.base = value,
                        numericWidth
                );
                FlowLayout factor = compactTextField(
                        context,
                        ItemEditorText.tr("special.advanced.component_tweaks.blocks_attacks_reduction_factor"),
                        draft.factor,
                        value -> draft.factor = value,
                        numericWidth
                );
                reductionCard.child(denseEquipmentRow(angle, base, factor));
            }

            card.child(reductionCard);
        }

        return card;
    }

    private static FlowLayout buildBlocksAttacksItemDamageCard(
            SpecialDataPanelContext context,
            ItemEditorState.SpecialData special
    ) {
        FlowLayout card = UiFactory.subCard();
        card.child(UiFactory.title(ItemEditorText.tr(
                "special.advanced.component_tweaks.blocks_attacks_item_damage_title"
        )).shadow(false));

        int numericWidth = compactTinyFieldWidth();
        FlowLayout threshold = compactTextField(
                context,
                ItemEditorText.tr("special.advanced.component_tweaks.blocks_attacks_item_damage_threshold"),
                special.blocksAttacksItemDamageThreshold,
                value -> special.blocksAttacksItemDamageThreshold = value,
                numericWidth
        );
        FlowLayout base = compactTextField(
                context,
                ItemEditorText.tr("special.advanced.component_tweaks.blocks_attacks_item_damage_base"),
                special.blocksAttacksItemDamageBase,
                value -> special.blocksAttacksItemDamageBase = value,
                numericWidth
        );
        FlowLayout factor = compactTextField(
                context,
                ItemEditorText.tr("special.advanced.component_tweaks.blocks_attacks_item_damage_factor"),
                special.blocksAttacksItemDamageFactor,
                value -> special.blocksAttacksItemDamageFactor = value,
                numericWidth
        );
        card.child(denseEquipmentRow(threshold, base, factor));
        return card;
    }

    private static String blocksAttacksDamageReductionSummary(
            ItemEditorState.BlocksAttacksDamageReductionDraft draft
    ) {
        String types = draft.typeIds == null || draft.typeIds.isBlank() ? "all damage" : draft.typeIds;
        String angle = draft.horizontalBlockingAngle == null || draft.horizontalBlockingAngle.isBlank()
                ? "90"
                : draft.horizontalBlockingAngle;
        String base = draft.base == null || draft.base.isBlank() ? "0" : draft.base;
        String factor = draft.factor == null || draft.factor.isBlank() ? "1" : draft.factor;
        return types + " - angle " + angle + " - " + base + " + " + factor + "x";
    }

    private static FlowLayout buildCombatBehaviorCard(SpecialDataPanelContext context, ItemEditorState.SpecialData special) {
        FlowLayout card = UiFactory.subCard();
        boolean stacked = prefersStackedCompactRows();
        int numericWidth = compactNumericFieldWidth();
        int pickerWidth = compactPickerButtonWidth();
        int idWidth = compactIdTextWidth();

        ButtonComponent swingButton = UiFactory.button(
                PickerFieldFactory.selectedOrFallback(special.swingAnimationType, ItemEditorText.tr("special.advanced.select")), UiFactory.ButtonTextPreset.STANDARD,
                anchor -> context.openClearableDropdown(
                        anchor,
                        ItemEditorText.tr("common.none"),
                        () -> context.mutate(() -> special.swingAnimationType = ""),
                        Arrays.asList(SwingAnimationType.values()),
                        SwingAnimationType::name,
                        type -> context.mutate(() -> special.swingAnimationType = type.name())
                )
        );
        swingButton.horizontalSizing(Sizing.fill(100));
        FlowLayout swingTypeField = compactField(
                ItemEditorText.tr("special.advanced.component_tweaks.swing_animation_type"),
                swingButton,
                pickerWidth + 40
        );
        FlowLayout swingDurationField = compactField(
                ItemEditorText.tr("special.advanced.component_tweaks.swing_animation_duration"),
                UiFactory.textBox(special.swingAnimationDuration, context.bindText(value -> special.swingAnimationDuration = value))
                .horizontalSizing(Sizing.fill(100)),
                numericWidth + 40
        );
        if (stacked) {
            card.child(swingTypeField);
            card.child(swingDurationField);
        } else {
            FlowLayout swingRow = responsiveRow();
            swingRow.gap(SECTION_ROW_GAP);
            distributeRowChildren(swingRow, swingTypeField, swingDurationField);
            card.child(swingRow);
        }

        UIComponent piercingKnockback = UiFactory.checkbox(
                ItemEditorText.tr("special.advanced.component_tweaks.piercing_knockback"),
                special.piercingDealsKnockback,
                context.bindToggle(value -> special.piercingDealsKnockback = value)
        );
        UIComponent piercingDismounts = UiFactory.checkbox(
                ItemEditorText.tr("special.advanced.component_tweaks.piercing_dismounts"),
                special.piercingDismounts,
                context.bindToggle(value -> special.piercingDismounts = value)
        );
        card.child(compactCheckboxRow(piercingKnockback, piercingDismounts));

        card.child(compactIdField(
                context,
                ItemEditorText.tr("special.advanced.component_tweaks.piercing_sound"),
                special.piercingSoundId,
                value -> special.piercingSoundId = value,
                soundIds(context),
                ItemEditorText.str("special.advanced.component_tweaks.piercing_sound"),
                idWidth
        ));
        card.child(compactIdField(
                context,
                ItemEditorText.tr("special.advanced.component_tweaks.piercing_hit_sound"),
                special.piercingHitSoundId,
                value -> special.piercingHitSoundId = value,
                soundIds(context),
                ItemEditorText.str("special.advanced.component_tweaks.piercing_hit_sound"),
                idWidth
        ));

        FlowLayout kineticContactField = compactField(
                ItemEditorText.tr("special.advanced.component_tweaks.kinetic_contact_cooldown"),
                UiFactory.textBox(special.kineticContactCooldownTicks, context.bindText(value -> special.kineticContactCooldownTicks = value))
                .horizontalSizing(Sizing.fill(100)),
                numericWidth + 40
        );
        FlowLayout kineticDelayField = compactField(
                ItemEditorText.tr("special.advanced.component_tweaks.kinetic_delay_ticks"),
                UiFactory.textBox(special.kineticDelayTicks, context.bindText(value -> special.kineticDelayTicks = value))
                .horizontalSizing(Sizing.fill(100)),
                numericWidth + 40
        );

        FlowLayout kineticForwardField = compactField(
                ItemEditorText.tr("special.advanced.component_tweaks.kinetic_forward_movement"),
                UiFactory.textBox(special.kineticForwardMovement, context.bindText(value -> special.kineticForwardMovement = value))
                .horizontalSizing(Sizing.fill(100)),
                numericWidth + 40
        );
        FlowLayout kineticDamageField = compactField(
                ItemEditorText.tr("special.advanced.component_tweaks.kinetic_damage_multiplier"),
                UiFactory.textBox(special.kineticDamageMultiplier, context.bindText(value -> special.kineticDamageMultiplier = value))
                .horizontalSizing(Sizing.fill(100)),
                numericWidth + 40
        );
        card.child(denseEquipmentRow(kineticContactField, kineticDelayField, kineticForwardField, kineticDamageField));

        card.child(compactIdField(
                context,
                ItemEditorText.tr("special.advanced.component_tweaks.kinetic_sound"),
                special.kineticSoundId,
                value -> special.kineticSoundId = value,
                soundIds(context),
                ItemEditorText.str("special.advanced.component_tweaks.kinetic_sound"),
                idWidth
        ));
        card.child(compactIdField(
                context,
                ItemEditorText.tr("special.advanced.component_tweaks.kinetic_hit_sound"),
                special.kineticHitSoundId,
                value -> special.kineticHitSoundId = value,
                soundIds(context),
                ItemEditorText.str("special.advanced.component_tweaks.kinetic_hit_sound"),
                idWidth
        ));

        return card;
    }

    private static FlowLayout idTextWithPicker(
            SpecialDataPanelContext context,
            String value,
            Consumer<String> setter,
            List<String> entries,
            String pickerTitle
    ) {
        return idTextWithPickerCompact(context, value, setter, entries, pickerTitle);
    }

    private static FlowLayout idTextWithPickerCompact(
            SpecialDataPanelContext context,
            String value,
            Consumer<String> setter,
            List<String> entries,
            String pickerTitle
    ) {
        FlowLayout row = UiFactory.row();
        row.gap(holderSetRowGap());
        UIComponent input = UiFactory.textBox(value, text -> context.mutate(() -> setter.accept(IdFieldNormalizer.normalize(text))))
                .horizontalSizing(Sizing.expand(100));
        row.child(input);
        ButtonComponent pickButton = UiFactory.button(ItemEditorText.tr("common.pick"), UiFactory.ButtonTextPreset.STANDARD,  button ->
                context.openSearchablePicker(
                        pickerTitle,
                        "",
                        entries,
                        id -> id,
                        id -> context.mutateRefresh(() -> setter.accept(id))
                )
        );
        pickButton.horizontalSizing(Sizing.fixed(compactFixedPickButtonWidth()));
        row.child(pickButton);
        return row;
    }

    private static FlowLayout damageTypeHolderSetEditor(
            SpecialDataPanelContext context,
            String value,
            Consumer<String> setter,
            Supplier<String> currentValueSupplier,
            String pickerTitle,
            boolean collapsed,
            Consumer<Boolean> collapsedSetter,
            boolean allowTagExpansion,
            Consumer<Boolean> allowTagExpansionSetter
    ) {
        FlowLayout editor = UiFactory.column();
        editor.gap(SECTION_ROW_GAP);

        List<String> entries = splitIdentifierTokens(value);
        FlowLayout summaryRow = UiFactory.row();
        UIComponent summary = UiFactory.muted(holderSetSummary(entries), compactLongFieldWidth() + 120);
        summary.horizontalSizing(Sizing.expand(100));
        summaryRow.child(summary);
        if (collapsedSetter != null) {
            ButtonComponent toggle = UiFactory.button(
                    Component.literal(collapsed ? SYMBOL_SECTION_COLLAPSED : SYMBOL_SECTION_EXPANDED),
                    UiFactory.ButtonTextPreset.STANDARD,
                    button -> context.mutateRefresh(() -> collapsedSetter.accept(!collapsed))
            );
            toggle.horizontalSizing(Sizing.fixed(compactIconButtonWidth()));
            summaryRow.child(toggle);
        }
        editor.child(summaryRow);

        if (hasHolderSetExpansionWarning(context, entries)) {
            var warning = UiFactory.message(
                    ItemEditorText.str("special.advanced.component_tweaks.tag_expansion_warning"),
                    0xFF8A8A
            );
            warning.maxWidth(Math.min(
                    UiFactory.responsiveBodyTextWidth(),
                    compactLongFieldWidth() + 60
            ));
            warning.horizontalSizing(Sizing.fill(100));
            editor.child(warning);
            if (allowTagExpansionSetter != null) {
                editor.child(UiFactory.checkbox(
                        ItemEditorText.tr("special.advanced.component_tweaks.allow_tag_expansion"),
                        allowTagExpansion,
                        context.bindToggle(allowTagExpansionSetter)
                ));
            }
        }

        if (collapsed) {
            return editor;
        }

        boolean compactHolderRows = usesStackedPickerRows();
        FlowLayout pickers = holderSetPickerButtons(
                context,
                setter,
                currentValueSupplier,
                pickerTitle,
                compactHolderRows
        );
        if (compactHolderRows) {
            editor.child(pickers);
        }

        List<String> displayedEntries = entries.isEmpty() ? List.of("") : entries;
        for (int index = 0; index < displayedEntries.size(); index++) {
            int currentIndex = index;
            boolean emptyPlaceholder = entries.isEmpty();
            String entryValue = displayedEntries.get(index);
            FlowLayout row = UiFactory.row();
            row.gap(holderSetRowGap());
            UIComponent kind = UiFactory.muted(holderSetEntryKind(entryValue), holderSetKindWidth());
            kind.horizontalSizing(Sizing.fixed(holderSetKindWidth()));
            row.child(kind);
            row.child(UiFactory.textBox(entryValue, context.bindText(text ->
                    setter.accept(replaceIdentifierListValue(currentValueSupplier.get(), currentIndex, text))
            )).horizontalSizing(Sizing.expand(100)));
            ButtonComponent remove = UiFactory.button(
                    compactHolderRows
                            ? Component.literal("X").withColor(0xFF8A8A)
                            : ItemEditorText.tr("common.remove"),
                    UiFactory.ButtonTextPreset.COMPACT,
                    button -> context.mutateRefresh(() -> setter.accept(removeIdentifierListValue(
                            currentValueSupplier.get(),
                            currentIndex
                    )))
            );
            if (compactHolderRows) {
                remove.tooltip(List.of(ItemEditorText.tr("common.remove")));
            }
            remove.active(!emptyPlaceholder);
            remove.horizontalSizing(Sizing.fixed(holderSetRemoveButtonWidth()));
            row.child(remove);
            editor.child(row);
        }

        if (!compactHolderRows) {
            editor.child(pickers);
        }
        return editor;
    }

    private static FlowLayout holderSetPickerButtons(
            SpecialDataPanelContext context,
            Consumer<String> setter,
            Supplier<String> currentValueSupplier,
            String pickerTitle,
            boolean compact
    ) {
        FlowLayout pickers = UiFactory.row();
        pickers.gap(holderSetRowGap());
        ButtonComponent pickType = UiFactory.button(ItemEditorText.tr("common.add_type").copy().withColor(0x91E68C), UiFactory.ButtonTextPreset.COMPACT, button ->
                context.openSearchablePicker(
                        pickerTitle,
                        "",
                        damageTypeIds(context),
                        id -> id,
                        id -> context.mutateRefresh(() -> setter.accept(appendIdentifierListValue(currentValueSupplier.get(), id)))
                )
        );
        pickType.horizontalSizing(compact ? Sizing.fill(49) : Sizing.fixed(compactPickerButtonWidth()));
        pickers.child(pickType);

        ButtonComponent pickTag = UiFactory.button(ItemEditorText.tr("common.add_tag").copy().withColor(0x8AC8FF), UiFactory.ButtonTextPreset.COMPACT, button ->
                context.openSearchablePicker(
                        pickerTitle,
                        "",
                        damageTypeTagIds(context),
                        id -> id,
                        id -> context.mutateRefresh(() -> setter.accept(appendIdentifierListValue(currentValueSupplier.get(), "#" + id)))
                )
        );
        pickTag.horizontalSizing(compact ? Sizing.fill(49) : Sizing.fixed(compactPickerButtonWidth()));
        pickers.child(pickTag);
        return pickers;
    }

    private static int holderSetKindWidth() {
        return clampToPanelWidth(Math.max(34, UiFactory.scaledPixels(42)));
    }

    private static int holderSetRemoveButtonWidth() {
        if (usesStackedPickerRows()) {
            return clampToPanelWidth(Math.max(34, UiFactory.scaledPixels(40)));
        }
        return compactRemoveButtonWidth();
    }

    private static int holderSetRowGap() {
        return Math.max(1, UiFactory.scaleProfile().tightSpacing() - 2);
    }

    private static Component holderSetSummary(List<String> entries) {
        if (entries.isEmpty()) {
            return Component.literal("No damage types or tags selected");
        }

        int tags = 0;
        for (String entry : entries) {
            if (entry.startsWith("#")) {
                tags++;
            }
        }
        int types = entries.size() - tags;
        return Component.literal(entries.size() + " entries: " + types + " type" + (types == 1 ? "" : "s")
                + ", " + tags + " tag" + (tags == 1 ? "" : "s"));
    }

    private static boolean hasHolderSetExpansionWarning(
            SpecialDataPanelContext context,
            List<String> entries
    ) {
        if (entries.size() <= 1) {
            return false;
        }
        List<String> typeIds = damageTypeIds(context);
        List<String> tagIds = damageTypeTagIds(context);
        for (String entry : entries) {
            if (entry.startsWith("#")) {
                return true;
            }
            if (!typeIds.contains(entry) && tagIds.contains(entry)) {
                return true;
            }
        }
        return false;
    }

    private static Component holderSetEntryKind(String value) {
        if (value != null && value.trim().startsWith("#")) {
            return Component.literal("Tag").withColor(0x8AC8FF);
        }
        if (value != null && !value.isBlank()) {
            return Component.literal("Type").withColor(0x91E68C);
        }
        return ItemEditorText.tr("common.entry");
    }

    private static String appendIdentifierListValue(String raw, String selected) {
        List<String> values = splitIdentifierTokens(raw);
        List<String> selectedValues = splitIdentifierTokens(selected);
        if (selectedValues.isEmpty()) {
            return serializeIdentifierTokens(values);
        }
        for (String selectedValue : selectedValues) {
            if (!containsIdentifierToken(values, selectedValue)) {
                values.add(selectedValue);
            }
        }
        return serializeIdentifierTokens(values);
    }

    private static String replaceIdentifierListValue(String raw, int index, String replacement) {
        List<String> values = splitIdentifierTokens(raw);
        List<String> replacementValues = splitIdentifierTokens(replacement);
        if (values.isEmpty()) {
            values.addAll(replacementValues);
            return serializeIdentifierTokens(values);
        }
        if (index < 0 || index >= values.size()) {
            return serializeIdentifierTokens(values);
        }
        values.remove(index);
        values.addAll(index, replacementValues);
        return serializeIdentifierTokens(values);
    }

    private static String removeIdentifierListValue(String raw, int index) {
        List<String> values = splitIdentifierTokens(raw);
        if (index >= 0 && index < values.size()) {
            values.remove(index);
        }
        return serializeIdentifierTokens(values);
    }

    private static boolean containsIdentifierToken(List<String> values, String candidate) {
        for (String value : values) {
            if (value.equalsIgnoreCase(candidate)) {
                return true;
            }
        }
        return false;
    }

    private static List<String> splitIdentifierTokens(String raw) {
        if (raw == null || raw.isBlank()) {
            return new ArrayList<>();
        }
        List<String> values = new ArrayList<>();
        for (String part : raw.split("[,\\r\\n]+")) {
            String normalized = normalizeIdentifierToken(part);
            if (!normalized.isBlank()) {
                values.add(normalized);
            }
        }
        return values;
    }

    private static String normalizeIdentifierToken(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (value.startsWith("#")) {
            String tag = IdFieldNormalizer.normalize(value.substring(1));
            return tag.isBlank() ? "" : "#" + tag;
        }
        return IdFieldNormalizer.normalize(value);
    }

    private static String serializeIdentifierTokens(List<String> values) {
        return String.join(", ", values);
    }

    private static boolean hasBlockStateProperties(ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem blockItem)) {
            return false;
        }
        return !blockItem.getBlock().defaultBlockState().getProperties().isEmpty();
    }

    private static Map<String, String> parseBlockStatePropertyMap(String raw) {
        Map<String, String> valuesByKey = new LinkedHashMap<>();
        if (raw == null || raw.isBlank()) {
            return valuesByKey;
        }

        for (String part : raw.split("[,\\r\\n]+")) {
            String token = part.trim();
            if (token.isEmpty()) {
                continue;
            }
            int separator = token.indexOf('=');
            String key = separator < 0 ? token.trim() : token.substring(0, separator).trim();
            String value = separator < 0 ? "" : token.substring(separator + 1).trim();
            if (!key.isEmpty()) {
                valuesByKey.put(key, value);
            }
        }
        return valuesByKey;
    }

    private static String selectedBlockStateValue(Map<String, String> currentValues, BlockStatePropertyMeta property) {
        String currentValue = currentValues.get(property.key());
        if (currentValue != null && !currentValue.isBlank()) {
            return currentValue;
        }
        if (property.defaultValue() != null && !property.defaultValue().isBlank()) {
            return property.defaultValue();
        }
        return property.values().isEmpty() ? ItemEditorText.str("special.advanced.select") : property.values().getFirst();
    }

    private static void setBlockStateProperty(ItemEditorState.SpecialData special, String key, String value) {
        Map<String, String> entries = parseBlockStatePropertyMap(special.blockStateProperties);
        String normalizedKey = key == null ? "" : key.trim();
        if (normalizedKey.isEmpty()) {
            return;
        }

        String normalizedValue = value == null ? "" : value.trim();
        if (!normalizedValue.isEmpty()) {
            entries.put(normalizedKey, normalizedValue);
        } else {
            entries.remove(normalizedKey);
        }
        special.blockStateProperties = serializeBlockStateProperties(entries);
    }

    private static void removeBlockStateProperty(ItemEditorState.SpecialData special, String key) {
        String normalizedKey = key == null ? "" : key.trim();
        Map<String, String> entries = parseBlockStatePropertyMap(special.blockStateProperties);
        entries.remove(normalizedKey);
        special.blockStateProperties = serializeBlockStateProperties(entries);
    }

    private static List<BlockStatePropertyMeta> blockStatePropertyMeta(SpecialDataPanelContext context) {
        ItemStack stack = context.originalStack();
        if (!(stack.getItem() instanceof BlockItem blockItem)) {
            return List.of();
        }

        List<BlockStatePropertyMeta> metas = new ArrayList<>();
        BlockState defaultState = blockItem.getBlock().defaultBlockState();
        for (Property<?> property : defaultState.getProperties()) {
            metas.add(blockStatePropertyMeta(defaultState, property));
        }
        metas.sort(Comparator.comparing(BlockStatePropertyMeta::key));
        return metas;
    }

    private static <T extends Comparable<T>> BlockStatePropertyMeta blockStatePropertyMeta(BlockState defaultState, Property<T> property) {
        return new BlockStatePropertyMeta(
                property.getName(),
                blockStatePropertyValues(property),
                property.getName(defaultState.getValue(property))
        );
    }

    private static <T extends Comparable<T>> List<String> blockStatePropertyValues(Property<T> property) {
        List<String> values = new ArrayList<>();
        for (T value : property.getPossibleValues()) {
            values.add(property.getName(value));
        }
        values.sort(Comparator.naturalOrder());
        return values;
    }

    private static String serializeBlockStateProperties(Map<String, String> entries) {
        List<String> tokens = new ArrayList<>();
        for (Map.Entry<String, String> entry : entries.entrySet()) {
            String key = entry.getKey() == null ? "" : entry.getKey().trim();
            String value = entry.getValue() == null ? "" : entry.getValue().trim();
            if (key.isEmpty() && value.isEmpty()) {
                continue;
            }
            tokens.add(key + "=" + value);
        }
        return String.join(", ", tokens);
    }

    private record BlockStatePropertyMeta(String key, List<String> values, String defaultValue) {
    }

    private static <T> void swapEntries(List<T> drafts, int left, int right) {
        if (left < 0 || right < 0 || left >= drafts.size() || right >= drafts.size()) {
            return;
        }
        T draft = drafts.get(left);
        drafts.set(left, drafts.get(right));
        drafts.set(right, draft);
    }

    private static List<String> itemIds(SpecialDataPanelContext context) {
        Registry<Item> registry = context.screen().session().registryAccess().lookupOrThrow(Registries.ITEM);
        return RegistryUtil.ids(registry).stream()
                .filter(id -> !Objects.equals(id, "minecraft:air"))
                .toList();
    }

    private static List<String> soundIds(SpecialDataPanelContext context) {
        return registryIds(context, Registries.SOUND_EVENT);
    }

    private static List<String> lootTableIds(SpecialDataPanelContext context) {
        return registryIds(context, Registries.LOOT_TABLE);
    }

    private static List<String> mapDecorationTypeIds(SpecialDataPanelContext context) {
        return registryIds(context, Registries.MAP_DECORATION_TYPE);
    }

    private static List<String> dimensionIds(SpecialDataPanelContext context) {
        List<String> ids = registryIds(context, Registries.DIMENSION);
        if (!ids.isEmpty()) {
            return ids;
        }
        List<String> fallback = new ArrayList<>();
        fallback.add("minecraft:overworld");
        fallback.add("minecraft:the_nether");
        fallback.add("minecraft:the_end");
        fallback.sort(Comparator.naturalOrder());
        return fallback;
    }

    private static List<String> damageTypeIds(SpecialDataPanelContext context) {
        return registryIds(context, Registries.DAMAGE_TYPE);
    }

    private static List<String> damageTypeTagIds(SpecialDataPanelContext context) {
        Registry<?> registry = context.screen().session().registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE);
        return registry.getTags()
                .map(tag -> tag.key().location().toString())
                .sorted()
                .toList();
    }

    private static List<String> effectIds(SpecialDataPanelContext context) {
        Registry<MobEffect> registry = context.screen().session().registryAccess().lookupOrThrow(Registries.MOB_EFFECT);
        return RegistryUtil.ids(registry);
    }

    private static List<String> paintingVariantIds(SpecialDataPanelContext context) {
        return registryIds(context, Registries.PAINTING_VARIANT);
    }

    private static List<String> entityTypeIds(SpecialDataPanelContext context) {
        return registryIds(context, Registries.ENTITY_TYPE);
    }

    private static <T> List<String> registryIds(
            SpecialDataPanelContext context,
            ResourceKey<? extends Registry<T>> registryKey
    ) {
        try {
            Registry<T> registry = context.screen().session().registryAccess().lookupOrThrow(registryKey);
            return RegistryUtil.ids(registry);
        } catch (RuntimeException ignored) {
            return List.of();
        }
    }
}

