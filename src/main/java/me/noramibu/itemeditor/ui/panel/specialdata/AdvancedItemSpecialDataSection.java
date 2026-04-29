package me.noramibu.itemeditor.ui.panel.specialdata;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.UIComponent;
import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.ui.component.PickerFieldFactory;
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
    private static final int COMPACT_ICON_BUTTON_MIN = 36;
    private static final int COMPACT_ICON_BUTTON_BASE = 42;
    private static final int COMPACT_CLEAR_BUTTON_MIN = 52;
    private static final int COMPACT_CLEAR_BUTTON_BASE = 68;
    private static final int COMPACT_REMOVE_BUTTON_MIN = 72;
    private static final int COMPACT_REMOVE_BUTTON_BASE = 88;
    private static final int COMPACT_FIXED_PICK_BUTTON_MIN = 46;
    private static final int COMPACT_FIXED_PICK_BUTTON_BASE = 56;
    private static final int SECTION_ROW_GAP = 10;
    private static final int FLAGS_ROW_GAP = 12;
    private static final int COLLAPSIBLE_HEADER_TITLE_RESERVE = 26;
    private static final int COMPACT_FIELD_LABEL_RESERVE = 44;
    private static final int PANEL_WIDTH_SAFETY_RESERVE = 20;
    private static final String SYMBOL_SECTION_COLLAPSED = "[+]";
    private static final String SYMBOL_SECTION_EXPANDED = "[-]";
    private static final String SYMBOL_STEP_DECREMENT = "-";
    private static final String SYMBOL_STEP_INCREMENT = "+";
    private static final String BLOCK_STATE_KEY_LABEL = "Key";
    private static final String BLOCK_STATE_VALUE_LABEL = "Value";
    private static final String BLOCK_STATE_EMPTY_PROPERTIES_TEXT = "No block state properties configured.";
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
    private static final int BLOCK_STATE_EMPTY_PROPERTIES_HINT_WIDTH = 320;

    private AdvancedItemSpecialDataSection() {
    }

    public static boolean supportsFoodConsumable(ItemStack stack, RegistryAccess registryAccess) {
        return ItemEditorCapabilities.supportsAnyComponent(
                stack,
                registryAccess,
                "minecraft:food",
                "minecraft:consumable",
                "minecraft:use_effects",
                "minecraft:use_remainder",
                "minecraft:use_cooldown"
        );
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

    public static boolean supportsEquipmentCombat(ItemStack stack, RegistryAccess registryAccess) {
        return ItemEditorCapabilities.supportsAnyComponent(
                stack,
                registryAccess,
                "minecraft:equippable",
                "minecraft:weapon",
                "minecraft:tool",
                "minecraft:repairable",
                "minecraft:attack_range"
        ) || stack.has(DataComponents.EQUIPPABLE)
                || stack.has(DataComponents.WEAPON)
                || stack.has(DataComponents.TOOL)
                || stack.has(DataComponents.REPAIRABLE)
                || stack.has(DataComponents.ATTACK_RANGE)
                || stack.isDamageableItem();
    }

    public static boolean supportsComponentTweaksNaming(ItemStack stack, RegistryAccess registryAccess) {
        return ItemEditorCapabilities.supportsAnyComponent(
                stack,
                registryAccess,
                "minecraft:item_name",
                "minecraft:minimum_attack_charge",
                "minecraft:enchantable",
                "minecraft:ominous_bottle_amplifier",
                "minecraft:tooltip_style",
                "minecraft:glider",
                "minecraft:intangible_projectile",
                "minecraft:death_protection"
        );
    }

    public static boolean supportsComponentTweaksRegistry(ItemStack stack, RegistryAccess registryAccess) {
        return ItemEditorCapabilities.supportsAnyComponent(
                stack,
                registryAccess,
                "minecraft:damage_type",
                "minecraft:damage_resistant",
                "minecraft:note_block_sound",
                "minecraft:break_sound",
                "minecraft:painting_variant"
        );
    }

    public static boolean supportsComponentTweaksBlock(ItemStack stack, RegistryAccess registryAccess) {
        return ItemEditorCapabilities.supportsAnyComponent(
                stack,
                registryAccess,
                "minecraft:block_state",
                "minecraft:blocks_attacks"
        );
    }

    public static boolean supportsComponentTweaksBehavior(ItemStack stack, RegistryAccess registryAccess) {
        return ItemEditorCapabilities.supportsAnyComponent(
                stack,
                registryAccess,
                "minecraft:swing_animation",
                "minecraft:piercing_weapon",
                "minecraft:kinetic_weapon"
        );
    }

    public static FlowLayout buildComponentTweakNamingSection(SpecialDataPanelContext context) {
        return buildComponentTweakNamingSection(context, context.special());
    }

    public static FlowLayout buildComponentTweakRegistrySection(SpecialDataPanelContext context) {
        return buildComponentTweakRegistrySection(context, context.special());
    }

    public static FlowLayout buildComponentTweakBlockSection(SpecialDataPanelContext context) {
        return buildComponentTweakBlockSection(context, context.special());
    }

    public static FlowLayout buildComponentTweakBehaviorSection(SpecialDataPanelContext context) {
        return buildComponentTweakBehaviorSection(context, context.special());
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
                    if (narrowLayout) {
                        FlowLayout foodValueRow = responsiveRow();
        foodValueRow.gap(SECTION_ROW_GAP);
                        foodValueRow.child(compactTextField(
                                context,
                                ItemEditorText.tr("special.advanced.food.nutrition"),
                                special.foodNutrition,
                                value -> special.foodNutrition = value,
                                compactNumberWidth
                        ));
                        foodValueRow.child(compactTextField(
                                context,
                                ItemEditorText.tr("special.advanced.food.saturation"),
                                special.foodSaturation,
                                value -> special.foodSaturation = value,
                                compactNumberWidth
                        ));
                        equalizeExistingRowChildren(foodValueRow);
                        content.child(foodValueRow);
                        content.child(UiFactory.checkbox(
                                ItemEditorText.tr("special.advanced.food.can_always_eat"),
                                special.foodCanAlwaysEat,
                                context.bindToggle(value -> special.foodCanAlwaysEat = value)
                        ));

                        FlowLayout consumableTopRow = responsiveRow();
        consumableTopRow.gap(SECTION_ROW_GAP);
                        consumableTopRow.child(compactTextField(
                                context,
                                ItemEditorText.tr("special.advanced.consumable.consume_seconds"),
                                special.consumableConsumeSeconds,
                                value -> special.consumableConsumeSeconds = value,
                                compactNumberWidth
                        ));
                        consumableTopRow.child(compactAnimationPicker(context, special, animationButtonWidth));
                        equalizeExistingRowChildren(consumableTopRow);
                        content.child(consumableTopRow);
                        content.child(UiFactory.checkbox(
                                ItemEditorText.tr("special.advanced.consumable.has_particles"),
                                special.consumableHasParticles,
                                context.bindToggle(value -> special.consumableHasParticles = value)
                        ));
                    } else {
                        FlowLayout foodRow = responsiveRow();
        foodRow.gap(SECTION_ROW_GAP);
                        foodRow.child(compactTextField(
                                context,
                                ItemEditorText.tr("special.advanced.food.nutrition"),
                                special.foodNutrition,
                                value -> special.foodNutrition = value,
                                compactNumberWidth
                        ));
                        foodRow.child(compactTextField(
                                context,
                                ItemEditorText.tr("special.advanced.food.saturation"),
                                special.foodSaturation,
                                value -> special.foodSaturation = value,
                                compactNumberWidth
                        ));
                        foodRow.child(UiFactory.checkbox(
                                ItemEditorText.tr("special.advanced.food.can_always_eat"),
                                special.foodCanAlwaysEat,
                                context.bindToggle(value -> special.foodCanAlwaysEat = value)
                        ));
                        equalizeExistingRowChildren(foodRow);
                        content.child(foodRow);

                        FlowLayout consumableRow = responsiveRow();
        consumableRow.gap(SECTION_ROW_GAP);
                        consumableRow.child(compactTextField(
                                context,
                                ItemEditorText.tr("special.advanced.consumable.consume_seconds"),
                                special.consumableConsumeSeconds,
                                value -> special.consumableConsumeSeconds = value,
                                compactNumberWidth
                        ));
                        consumableRow.child(compactAnimationPicker(context, special, animationButtonWidth));
                        consumableRow.child(UiFactory.checkbox(
                                ItemEditorText.tr("special.advanced.consumable.has_particles"),
                                special.consumableHasParticles,
                                context.bindToggle(value -> special.consumableHasParticles = value)
                        ));
                        equalizeExistingRowChildren(consumableRow);
                        content.child(consumableRow);
                    }

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

                    if (narrowLayout) {
                        content.child(UiFactory.checkbox(
                                ItemEditorText.tr("special.advanced.use_effects.can_sprint"),
                                special.useEffectsCanSprint,
                                context.bindToggle(value -> special.useEffectsCanSprint = value)
                        ));
                        content.child(UiFactory.checkbox(
                                ItemEditorText.tr("special.advanced.use_effects.interact_vibrations"),
                                special.useEffectsInteractVibrations,
                                context.bindToggle(value -> special.useEffectsInteractVibrations = value)
                        ));
                        content.child(compactTextField(
                                context,
                                ItemEditorText.tr("special.advanced.use_effects.speed_multiplier"),
                                special.useEffectsSpeedMultiplier,
                                value -> special.useEffectsSpeedMultiplier = value,
                                compactNumberWidth
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
                        content.child(compactTextField(
                                context,
                                ItemEditorText.tr("special.advanced.use_remainder.count"),
                                special.useRemainderCount,
                                value -> special.useRemainderCount = value,
                                compactTinyWidth
                        ));
                        content.child(compactTextField(
                                context,
                                ItemEditorText.tr("special.advanced.use_cooldown.seconds"),
                                special.useCooldownSeconds,
                                value -> special.useCooldownSeconds = value,
                                compactNumberWidth
                        ));
                        content.child(compactTextField(
                                context,
                                ItemEditorText.tr("special.advanced.use_cooldown.group"),
                                special.useCooldownGroup,
                                value -> special.useCooldownGroup = value,
                                compactGroupWidth
                        ));
                    } else {
                        FlowLayout useEffectsRow = responsiveRow();
        useEffectsRow.gap(SECTION_ROW_GAP);
                        useEffectsRow.child(UiFactory.checkbox(
                                ItemEditorText.tr("special.advanced.use_effects.can_sprint"),
                                special.useEffectsCanSprint,
                                context.bindToggle(value -> special.useEffectsCanSprint = value)
                        ));
                        useEffectsRow.child(UiFactory.checkbox(
                                ItemEditorText.tr("special.advanced.use_effects.interact_vibrations"),
                                special.useEffectsInteractVibrations,
                                context.bindToggle(value -> special.useEffectsInteractVibrations = value)
                        ));
                        useEffectsRow.child(compactTextField(
                                context,
                                ItemEditorText.tr("special.advanced.use_effects.speed_multiplier"),
                                special.useEffectsSpeedMultiplier,
                                value -> special.useEffectsSpeedMultiplier = value,
                                compactNumberWidth
                        ));
                        equalizeExistingRowChildren(useEffectsRow);
                        content.child(useEffectsRow);

                        FlowLayout remainderRow = responsiveRow();
        remainderRow.gap(SECTION_ROW_GAP);
                        remainderRow.child(compactIdField(
                                context,
                                ItemEditorText.tr("special.advanced.use_remainder.item_id"),
                                special.useRemainderItemId,
                                value -> special.useRemainderItemId = value,
                                itemIds(context),
                                ItemEditorText.str("special.advanced.use_remainder.item_id"),
                                compactIdWidth
                        ));
                        remainderRow.child(compactTextField(
                                context,
                                ItemEditorText.tr("special.advanced.use_remainder.count"),
                                special.useRemainderCount,
                                value -> special.useRemainderCount = value,
                                compactTinyWidth
                        ));
                        equalizeExistingRowChildren(remainderRow);
                        content.child(remainderRow);

                        FlowLayout cooldownRow = responsiveRow();
        cooldownRow.gap(SECTION_ROW_GAP);
                        cooldownRow.child(compactTextField(
                                context,
                                ItemEditorText.tr("special.advanced.use_cooldown.seconds"),
                                special.useCooldownSeconds,
                                value -> special.useCooldownSeconds = value,
                                compactNumberWidth
                        ));
                        cooldownRow.child(compactTextField(
                                context,
                                ItemEditorText.tr("special.advanced.use_cooldown.group"),
                                special.useCooldownGroup,
                                value -> special.useCooldownGroup = value,
                                compactGroupWidth
                        ));
                        equalizeExistingRowChildren(cooldownRow);
                        content.child(cooldownRow);
                    }

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
        int titleWidth = Math.max(1, Math.min(guiWidth(), preferredTitleWidth));
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
                anchor -> context.openDropdown(
                        anchor,
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
        int preferredLabelWidth = Math.max(40, Math.min(labelWidth, Math.max(80, panelWidth - UiFactory.scaledPixels(COMPACT_FIELD_LABEL_RESERVE))));
        int effectiveLabelWidth = Math.max(1, Math.min(panelWidth, preferredLabelWidth));
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
        int preferred = Math.max(min, Math.min(max, value));
        return Math.max(1, Math.min(sourceWidth, preferred));
    }

    private static int clampToPanelWidth(int preferredWidth) {
        return Math.max(1, Math.min(guiWidth(), preferredWidth));
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

    private static FlowLayout responsiveRow() {
        return prefersStackedCompactRows() ? UiFactory.column() : UiFactory.row();
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
        for (UIComponent child : children) {
            child.horizontalSizing(Sizing.expand(100));
            row.child(child);
        }
    }

    private static void equalizeExistingRowChildren(FlowLayout row) {
        int count = row.children().size();
        if (count == 0) {
            return;
        }
        if (prefersStackedCompactRows()) {
            for (UIComponent child : row.children()) {
                child.horizontalSizing(Sizing.fill(100));
            }
            return;
        }
        for (UIComponent child : row.children()) {
            child.horizontalSizing(Sizing.expand(100));
        }
    }

    private static FlowLayout buildOnConsumeEffectsEditor(SpecialDataPanelContext context, ItemEditorState.SpecialData special) {
        FlowLayout card = UiFactory.subCard();
        card.child(UiFactory.title(ItemEditorText.tr("special.advanced.consumable.on_consume_effects")).shadow(false));

        ButtonComponent addButton = UiFactory.button(ItemEditorText.tr("special.advanced.consumable.add_effect"), UiFactory.ButtonTextPreset.STANDARD,  button ->
                context.mutateRefresh(() -> special.consumableOnConsumeEffects.add(new ItemEditorState.ConsumableEffectDraft()))
        );
        addButton.horizontalSizing(Sizing.fill(100));
        card.child(addButton);

        if (special.consumableOnConsumeEffects.isEmpty()) {
            card.child(UiFactory.muted(ItemEditorText.tr("special.advanced.consumable.effects_empty"), CONSUMABLE_EFFECTS_EMPTY_HINT_WIDTH));
            return card;
        }

        List<String> effectTypeValues = List.of(
                ItemEditorState.ConsumableEffectDraft.TYPE_APPLY_EFFECTS,
                ItemEditorState.ConsumableEffectDraft.TYPE_PLAY_SOUND
        );
        List<String> effectIds = effectIds(context);
        List<String> sounds = soundIds(context);

        for (int index = 0; index < special.consumableOnConsumeEffects.size(); index++) {
            int currentIndex = index;
            ItemEditorState.ConsumableEffectDraft draft = special.consumableOnConsumeEffects.get(index);
            String currentType = draft.type == null || draft.type.isBlank()
                    ? ItemEditorState.ConsumableEffectDraft.TYPE_APPLY_EFFECTS
                    : draft.type;

            FlowLayout effectCard = context.createReorderableCard(
                    ItemEditorText.tr("special.advanced.consumable.effect", index + 1),
                    currentIndex > 0,
                    () -> swapEntries(special.consumableOnConsumeEffects, currentIndex, currentIndex - 1),
                    currentIndex < special.consumableOnConsumeEffects.size() - 1,
                    () -> swapEntries(special.consumableOnConsumeEffects, currentIndex, currentIndex + 1),
                    () -> special.consumableOnConsumeEffects.remove(currentIndex)
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

                FlowLayout toggles = responsiveRow();
                UIComponent ambientToggle = UiFactory.checkbox(ItemEditorText.tr("special.potion.ambient"), effectDraft.ambient, context.bindToggle(value -> effectDraft.ambient = value));
                UIComponent visibleToggle = UiFactory.checkbox(ItemEditorText.tr("special.potion.visible"), effectDraft.visible, context.bindToggle(value -> effectDraft.visible = value));
                UIComponent iconToggle = UiFactory.checkbox(ItemEditorText.tr("special.potion.show_icon"), effectDraft.showIcon, context.bindToggle(value -> effectDraft.showIcon = value));
                distributeRowChildren(toggles, ambientToggle, visibleToggle, iconToggle);
                potionCard.child(toggles);
                effectCard.child(potionCard);
            }

            card.child(effectCard);
        }

        return card;
    }

    private static String consumableEffectSummary(ItemEditorState.ConsumableEffectDraft draft, String currentType) {
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

        FlowLayout actions = responsiveRow();
        ButtonComponent addButton = UiFactory.button(ItemEditorText.tr("special.advanced.container_meta.bees_add"), UiFactory.ButtonTextPreset.STANDARD,  button ->
                context.mutateRefresh(() -> {
                    ItemEditorState.BeeOccupantDraft draft = new ItemEditorState.BeeOccupantDraft();
                    draft.uiCollapsed = false;
                    special.beesOccupants.add(draft);
                })
        );
        addButton.horizontalSizing(Sizing.fill(100));
        actions.child(addButton);
        if (!special.beesOccupants.isEmpty()) {
            ButtonComponent expandAll = UiFactory.button(Component.literal(SYMBOL_SECTION_COLLAPSED), UiFactory.ButtonTextPreset.STANDARD,  button ->
                    context.mutateRefresh(() -> special.beesOccupants.forEach(entry -> entry.uiCollapsed = false))
            );
            expandAll.horizontalSizing(Sizing.fixed(compactIconButtonWidth()));
            expandAll.tooltip(List.of(Component.literal(TOOLTIP_EXPAND_ALL_BEES)));
            actions.child(expandAll);

            ButtonComponent collapseAll = UiFactory.button(Component.literal(SYMBOL_SECTION_EXPANDED), UiFactory.ButtonTextPreset.STANDARD,  button ->
                    context.mutateRefresh(() -> special.beesOccupants.forEach(entry -> entry.uiCollapsed = true))
            );
            collapseAll.horizontalSizing(Sizing.fixed(compactIconButtonWidth()));
            collapseAll.tooltip(List.of(Component.literal(TOOLTIP_COLLAPSE_ALL_BEES)));
            actions.child(collapseAll);

            ButtonComponent clearAll = UiFactory.button(ItemEditorText.tr("general.adventure.clear"), UiFactory.ButtonTextPreset.STANDARD,  button ->
                    context.mutateRefresh(special.beesOccupants::clear)
            );
            clearAll.horizontalSizing(Sizing.fixed(compactClearButtonWidth()));
            actions.child(clearAll);
        }
        card.child(actions);

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

                FlowLayout row = responsiveRow();
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
                distributeRowChildren(row, ticksField, minTicksField);
                beeCard.child(row);
            }
            card.child(beeCard);
        }
        return card;
    }

    public static FlowLayout buildCrossbow(SpecialDataPanelContext context) {
        ItemEditorState.SpecialData special = context.special();
        FlowLayout section = UiFactory.section(ItemEditorText.tr("special.advanced.crossbow.title"), Component.empty());
        List<String> availableItems = itemIds(context);
        section.child(collapsibleCard(
                context,
                ItemEditorText.tr("special.advanced.crossbow.title"),
                special.uiCrossbowCollapsed,
                value -> special.uiCrossbowCollapsed = value,
                () -> {
                    FlowLayout content = UiFactory.column();
                    int idWidth = compactIdTextWidth();
                    int countWidth = compactTinyFieldWidth();

                    FlowLayout actions = responsiveRow();
                    ButtonComponent addButton = UiFactory.button(ItemEditorText.tr("special.advanced.crossbow.add_projectile"), UiFactory.ButtonTextPreset.STANDARD,  button ->
                            context.mutateRefresh(() -> {
                                ItemEditorState.ChargedProjectileDraft draft = new ItemEditorState.ChargedProjectileDraft();
                                draft.uiCollapsed = false;
                                special.chargedProjectiles.add(draft);
                            })
                    );
                    addButton.horizontalSizing(Sizing.fill(100));
                    actions.child(addButton);
                    if (!special.chargedProjectiles.isEmpty()) {
                        ButtonComponent expandAll = UiFactory.button(Component.literal(SYMBOL_SECTION_COLLAPSED), UiFactory.ButtonTextPreset.STANDARD,  button ->
                                context.mutateRefresh(() -> special.chargedProjectiles.forEach(entry -> entry.uiCollapsed = false))
                        );
                        expandAll.horizontalSizing(Sizing.fixed(compactIconButtonWidth()));
                        expandAll.tooltip(List.of(Component.literal(TOOLTIP_EXPAND_ALL_PROJECTILES)));
                        actions.child(expandAll);

                        ButtonComponent collapseAll = UiFactory.button(Component.literal(SYMBOL_SECTION_EXPANDED), UiFactory.ButtonTextPreset.STANDARD,  button ->
                                context.mutateRefresh(() -> special.chargedProjectiles.forEach(entry -> entry.uiCollapsed = true))
                        );
                        collapseAll.horizontalSizing(Sizing.fixed(compactIconButtonWidth()));
                        collapseAll.tooltip(List.of(Component.literal(TOOLTIP_COLLAPSE_ALL_PROJECTILES)));
                        actions.child(collapseAll);

                        ButtonComponent clearAll = UiFactory.button(ItemEditorText.tr("general.adventure.clear"), UiFactory.ButtonTextPreset.STANDARD,  button ->
                                context.mutateRefresh(special.chargedProjectiles::clear)
                        );
                        clearAll.horizontalSizing(Sizing.fixed(compactClearButtonWidth()));
                        actions.child(clearAll);
                    }
                    content.child(actions);

                    if (special.chargedProjectiles.isEmpty()) {
                    content.child(UiFactory.muted(ItemEditorText.tr("special.advanced.crossbow.empty"), CROSSBOW_EMPTY_HINT_WIDTH));
                        return content;
                    }

                    for (int index = 0; index < special.chargedProjectiles.size(); index++) {
                        int currentIndex = index;
                        ItemEditorState.ChargedProjectileDraft draft = special.chargedProjectiles.get(index);
                        FlowLayout card = context.createReorderableCard(
                                ItemEditorText.tr("special.advanced.crossbow.projectile", index + 1),
                                currentIndex > 0,
                                () -> swapEntries(special.chargedProjectiles, currentIndex, currentIndex - 1),
                                currentIndex < special.chargedProjectiles.size() - 1,
                                () -> swapEntries(special.chargedProjectiles, currentIndex, currentIndex + 1),
                                () -> special.chargedProjectiles.remove(currentIndex)
                        );

                        FlowLayout summaryRow = responsiveRow();
            UIComponent summary = UiFactory.muted(Component.literal(projectileSummary(draft)), CROSSBOW_PROJECTILE_SUMMARY_HINT_WIDTH);
                        summary.horizontalSizing(Sizing.expand(100));
                        summaryRow.child(summary);
                        ButtonComponent collapseToggle = UiFactory.button(Component.literal(draft.uiCollapsed ? SYMBOL_SECTION_COLLAPSED : SYMBOL_SECTION_EXPANDED), UiFactory.ButtonTextPreset.STANDARD,  button ->
                                context.mutateRefresh(() -> draft.uiCollapsed = !draft.uiCollapsed)
                        );
                        collapseToggle.horizontalSizing(Sizing.fixed(compactIconButtonWidth()));
                        summaryRow.child(collapseToggle);
                        card.child(summaryRow);

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
        ));
        return section;
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

        FlowLayout actions = responsiveRow();
        ButtonComponent addButton = UiFactory.button(ItemEditorText.tr("special.advanced.map.add_decoration"), UiFactory.ButtonTextPreset.STANDARD,  button ->
                context.mutateRefresh(() -> {
                    ItemEditorState.MapDecorationDraft draft = new ItemEditorState.MapDecorationDraft();
                    draft.uiCollapsed = false;
                    special.mapDecorations.add(draft);
                })
        );
        addButton.horizontalSizing(Sizing.fill(100));
        actions.child(addButton);
        if (!special.mapDecorations.isEmpty()) {
            ButtonComponent expandAll = UiFactory.button(Component.literal(SYMBOL_SECTION_COLLAPSED), UiFactory.ButtonTextPreset.STANDARD,  button ->
                    context.mutateRefresh(() -> special.mapDecorations.forEach(entry -> entry.uiCollapsed = false))
            );
            expandAll.horizontalSizing(Sizing.fixed(compactIconButtonWidth()));
            expandAll.tooltip(List.of(Component.literal(TOOLTIP_EXPAND_ALL_DECORATIONS)));
            actions.child(expandAll);

            ButtonComponent collapseAll = UiFactory.button(Component.literal(SYMBOL_SECTION_EXPANDED), UiFactory.ButtonTextPreset.STANDARD,  button ->
                    context.mutateRefresh(() -> special.mapDecorations.forEach(entry -> entry.uiCollapsed = true))
            );
            collapseAll.horizontalSizing(Sizing.fixed(compactIconButtonWidth()));
            collapseAll.tooltip(List.of(Component.literal(TOOLTIP_COLLAPSE_ALL_DECORATIONS)));
            actions.child(collapseAll);

            ButtonComponent clearAll = UiFactory.button(ItemEditorText.tr("general.adventure.clear"), UiFactory.ButtonTextPreset.STANDARD,  button ->
                    context.mutateRefresh(special.mapDecorations::clear)
            );
            clearAll.horizontalSizing(Sizing.fixed(compactClearButtonWidth()));
            actions.child(clearAll);
        }
        card.child(actions);

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

        FlowLayout posRow = responsiveRow();
        UIComponent xField = UiFactory.field(
                ItemEditorText.tr("special.advanced.map.lodestone_x"),
                Component.empty(),
                        UiFactory.textBox(special.lodestoneX, context.bindText(value -> special.lodestoneX = value)).horizontalSizing(Sizing.fill(100))
        );
        UIComponent yField = UiFactory.field(
                ItemEditorText.tr("special.advanced.map.lodestone_y"),
                Component.empty(),
                        UiFactory.textBox(special.lodestoneY, context.bindText(value -> special.lodestoneY = value)).horizontalSizing(Sizing.fill(100))
        );
        UIComponent zField = UiFactory.field(
                ItemEditorText.tr("special.advanced.map.lodestone_z"),
                Component.empty(),
                        UiFactory.textBox(special.lodestoneZ, context.bindText(value -> special.lodestoneZ = value)).horizontalSizing(Sizing.fill(100))
        );
        distributeRowChildren(posRow, xField, yField, zField);
        card.child(posRow);
        return card;
    }

    private static FlowLayout buildEquippableCard(SpecialDataPanelContext context, ItemEditorState.SpecialData special) {
        FlowLayout card = UiFactory.subCard();
        card.child(UiFactory.title(ItemEditorText.tr("special.advanced.combat.equippable_title")).shadow(false));
        int pickerWidth = compactPickerButtonWidth();
        int idWidth = compactIdTextWidth();

        ButtonComponent slotButton = UiFactory.button(
                PickerFieldFactory.selectedOrFallback(special.equippableSlot, ItemEditorText.tr("special.advanced.select")), UiFactory.ButtonTextPreset.STANDARD, 
                anchor -> context.openDropdown(
                        anchor,
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

        card.child(UiFactory.checkbox(ItemEditorText.tr("special.advanced.combat.equippable_dispensable"), special.equippableDispensable, context.bindToggle(value -> special.equippableDispensable = value)));
        card.child(UiFactory.checkbox(ItemEditorText.tr("special.advanced.combat.equippable_swappable"), special.equippableSwappable, context.bindToggle(value -> special.equippableSwappable = value)));
        card.child(UiFactory.checkbox(ItemEditorText.tr("special.advanced.combat.equippable_damage_on_hurt"), special.equippableDamageOnHurt, context.bindToggle(value -> special.equippableDamageOnHurt = value)));
        card.child(UiFactory.checkbox(ItemEditorText.tr("special.advanced.combat.equippable_equip_on_interact"), special.equippableEquipOnInteract, context.bindToggle(value -> special.equippableEquipOnInteract = value)));
        card.child(UiFactory.checkbox(ItemEditorText.tr("special.advanced.combat.equippable_can_be_sheared"), special.equippableCanBeSheared, context.bindToggle(value -> special.equippableCanBeSheared = value)));

        return card;
    }

    private static FlowLayout buildWeaponCard(SpecialDataPanelContext context, ItemEditorState.SpecialData special) {
        FlowLayout card = UiFactory.subCard();
        card.child(UiFactory.title(ItemEditorText.tr("special.advanced.combat.weapon_title")).shadow(false));
        int numberWidth = compactNumericFieldWidth();

        FlowLayout row = responsiveRow();
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
        distributeRowChildren(row, damageField, disableField);
        card.child(row);
        return card;
    }

    private static FlowLayout buildToolCard(SpecialDataPanelContext context, ItemEditorState.SpecialData special) {
        FlowLayout card = UiFactory.subCard();
        card.child(UiFactory.title(ItemEditorText.tr("special.advanced.combat.tool_title")).shadow(false));
        int numberWidth = compactNumericFieldWidth();

        FlowLayout row = responsiveRow();
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
        distributeRowChildren(row, speedField, damageField, creativeField);
        card.child(row);
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
            FlowLayout row = responsiveRow();
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
            remove.horizontalSizing(Sizing.fixed(compactRemoveButtonWidth()));
            itemField.horizontalSizing(Sizing.expand(100));
            row.child(itemField);
            row.child(remove);
            card.child(row);
        }
        return card;
    }

    private static FlowLayout buildAttackRangeCard(SpecialDataPanelContext context, ItemEditorState.SpecialData special) {
        FlowLayout card = UiFactory.subCard();
        card.child(UiFactory.title(ItemEditorText.tr("special.advanced.combat.attack_range_title")).shadow(false));
        int numberWidth = compactNumericFieldWidth();

        FlowLayout rowOne = responsiveRow();
        FlowLayout minReach = compactTextField(context, ItemEditorText.tr("special.advanced.combat.range_min_reach"), special.attackRangeMinReach, value -> special.attackRangeMinReach = value, numberWidth);
        FlowLayout maxReach = compactTextField(context, ItemEditorText.tr("special.advanced.combat.range_max_reach"), special.attackRangeMaxReach, value -> special.attackRangeMaxReach = value, numberWidth);
        FlowLayout minCreative = compactTextField(context, ItemEditorText.tr("special.advanced.combat.range_min_creative"), special.attackRangeMinCreativeReach, value -> special.attackRangeMinCreativeReach = value, numberWidth);
        FlowLayout maxCreative = compactTextField(context, ItemEditorText.tr("special.advanced.combat.range_max_creative"), special.attackRangeMaxCreativeReach, value -> special.attackRangeMaxCreativeReach = value, numberWidth);
        distributeRowChildren(rowOne, minReach, maxReach, minCreative, maxCreative);
        card.child(rowOne);

        FlowLayout rowTwo = responsiveRow();
        FlowLayout hitbox = compactTextField(context, ItemEditorText.tr("special.advanced.combat.range_hitbox"), special.attackRangeHitboxMargin, value -> special.attackRangeHitboxMargin = value, numberWidth);
        FlowLayout mobFactor = compactTextField(context, ItemEditorText.tr("special.advanced.combat.range_mob_factor"), special.attackRangeMobFactor, value -> special.attackRangeMobFactor = value, numberWidth);
        distributeRowChildren(rowTwo, hitbox, mobFactor);
        card.child(rowTwo);
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
        boolean stacked = prefersStackedCompactRows();
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

        if (stacked) {
            card.child(minAttackField);
            card.child(enchantableField);
            card.child(ominousField);
        } else {
            FlowLayout rowA = responsiveRow();
            distributeRowChildren(rowA, minAttackField, enchantableField);
            card.child(rowA);

            FlowLayout rowB = responsiveRow();
            distributeRowChildren(rowB, ominousField);
            card.child(rowB);
        }

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
                context.bindToggle(value -> special.deathProtection = value)
        );

        if (stacked) {
            card.child(gliderToggle);
            card.child(intangibleToggle);
            card.child(deathProtectionToggle);
        } else {
            FlowLayout togglesA = responsiveRow();
            distributeRowChildren(togglesA, gliderToggle, intangibleToggle);
            card.child(togglesA);

            FlowLayout togglesB = responsiveRow();
            distributeRowChildren(togglesB, deathProtectionToggle);
            card.child(togglesB);
        }
        return card;
    }

    private static FlowLayout buildRegistryAndFlagsCard(SpecialDataPanelContext context, ItemEditorState.SpecialData special) {
        FlowLayout card = UiFactory.subCard();
        int idWidth = compactIdTextWidth();
        int longWidth = compactLongFieldWidth();

        card.child(compactIdField(
                context,
                ItemEditorText.tr("special.advanced.component_tweaks.damage_type"),
                special.damageTypeId,
                value -> special.damageTypeId = value,
                damageTypeIds(context),
                ItemEditorText.str("special.advanced.component_tweaks.damage_type"),
                idWidth
        ));
        card.child(compactField(
                ItemEditorText.tr("special.advanced.component_tweaks.damage_resistant_types"),
                UiFactory.textBox(special.damageResistantTypeIds, context.bindText(value -> special.damageResistantTypeIds = value))
                .horizontalSizing(Sizing.fill(100)),
                longWidth + 40
        ));
        card.child(compactIdField(
                context,
                ItemEditorText.tr("special.advanced.component_tweaks.note_block_sound"),
                special.noteBlockSoundId,
                value -> special.noteBlockSoundId = value,
                soundIds(context),
                ItemEditorText.str("special.advanced.component_tweaks.note_block_sound"),
                idWidth
        ));
        card.child(compactIdField(
                context,
                ItemEditorText.tr("special.advanced.component_tweaks.break_sound"),
                special.breakSoundId,
                value -> special.breakSoundId = value,
                soundIds(context),
                ItemEditorText.str("special.advanced.component_tweaks.break_sound"),
                idWidth
        ));
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

    private static FlowLayout buildBlockComponentsCard(SpecialDataPanelContext context, ItemEditorState.SpecialData special) {
        FlowLayout card = UiFactory.subCard();
        int numericWidth = compactNumericFieldWidth();
        int keyWidth = compactGroupFieldWidth();
        int valueWidth = compactGroupFieldWidth();
        int idWidth = compactIdTextWidth();

        List<BlockStateEntryDraft> stateEntries = parseBlockStateEntries(special.blockStateProperties);
        List<BlockStatePropertyMeta> availableProperties = blockStatePropertyMeta(context);
        List<String> availableKeys = availableProperties.stream().map(BlockStatePropertyMeta::key).toList();
        Map<String, List<String>> valuesByKey = new LinkedHashMap<>();
        for (BlockStatePropertyMeta property : availableProperties) {
            valuesByKey.put(property.key(), property.values());
        }

        FlowLayout stateCard = UiFactory.subCard();
        stateCard.child(UiFactory.title(ItemEditorText.tr("special.advanced.component_tweaks.block_state")).shadow(false));

        FlowLayout stateActions = responsiveRow();
        ButtonComponent addProperty = UiFactory.button(ItemEditorText.tr("common.add"), UiFactory.ButtonTextPreset.STANDARD,  button ->
                context.mutateRefresh(() -> {
                    List<BlockStateEntryDraft> entries = parseBlockStateEntries(special.blockStateProperties);
                    entries.add(new BlockStateEntryDraft("", ""));
                    special.blockStateProperties = serializeBlockStateEntries(entries);
                })
        );
        ButtonComponent clearProperties = UiFactory.button(ItemEditorText.tr("common.reset"), UiFactory.ButtonTextPreset.STANDARD,  button ->
                context.mutateRefresh(() -> special.blockStateProperties = "")
        );
        distributeRowChildren(stateActions, addProperty, clearProperties);
        stateCard.child(stateActions);

        if (stateEntries.isEmpty()) {
            stateCard.child(UiFactory.muted(Component.literal(BLOCK_STATE_EMPTY_PROPERTIES_TEXT), BLOCK_STATE_EMPTY_PROPERTIES_HINT_WIDTH));
        } else {
            for (int index = 0; index < stateEntries.size(); index++) {
                int currentIndex = index;
                BlockStateEntryDraft entry = stateEntries.get(index);
                FlowLayout entryCard = context.createReorderableCard(
                        Component.literal("Property " + (index + 1)),
                        currentIndex > 0,
                        () -> {
                            swapEntries(stateEntries, currentIndex, currentIndex - 1);
                            special.blockStateProperties = serializeBlockStateEntries(stateEntries);
                        },
                        currentIndex < stateEntries.size() - 1,
                        () -> {
                            swapEntries(stateEntries, currentIndex, currentIndex + 1);
                            special.blockStateProperties = serializeBlockStateEntries(stateEntries);
                        },
                        () -> {
                            stateEntries.remove(currentIndex);
                            special.blockStateProperties = serializeBlockStateEntries(stateEntries);
                        }
                );

                Consumer<String> keySetter = value -> {
                    entry.key = value;
                    special.blockStateProperties = serializeBlockStateEntries(stateEntries);
                };
                Consumer<String> valueSetter = value -> {
                    entry.value = value;
                    special.blockStateProperties = serializeBlockStateEntries(stateEntries);
                };

                UIComponent keyInput = availableKeys.isEmpty()
                ? UiFactory.textBox(entry.key, context.bindText(keySetter)).horizontalSizing(Sizing.fill(100))
                        : textWithPickerCompact(
                        context,
                        entry.key,
                        keySetter,
                        availableKeys,
                        "Block State Key"
                );
                List<String> valueOptions = valuesByKey.getOrDefault(entry.key == null ? "" : entry.key.trim(), List.of());
                UIComponent valueInput = valueOptions.isEmpty()
                ? UiFactory.textBox(entry.value, context.bindText(valueSetter)).horizontalSizing(Sizing.fill(100))
                        : textWithPickerCompact(
                        context,
                        entry.value,
                        valueSetter,
                        valueOptions,
                        "Block State Value"
                );

                FlowLayout keyValueRow = responsiveRow();
                FlowLayout keyField = compactField(Component.literal(BLOCK_STATE_KEY_LABEL), keyInput, keyWidth + 40);
                FlowLayout valueField = compactField(Component.literal(BLOCK_STATE_VALUE_LABEL), valueInput, valueWidth + 40);
                distributeRowChildren(keyValueRow, keyField, valueField);
                entryCard.child(keyValueRow);
                stateCard.child(entryCard);
            }
        }
        card.child(stateCard);

        FlowLayout row = responsiveRow();
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
        distributeRowChildren(row, blockDelayField, disableScaleField);
        card.child(row);

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

    private static FlowLayout buildCombatBehaviorCard(SpecialDataPanelContext context, ItemEditorState.SpecialData special) {
        FlowLayout card = UiFactory.subCard();
        boolean stacked = prefersStackedCompactRows();
        int numericWidth = compactNumericFieldWidth();
        int pickerWidth = compactPickerButtonWidth();
        int idWidth = compactIdTextWidth();
        int kineticLabelWidth = Math.max(numericWidth + 40, 240);

        ButtonComponent swingButton = UiFactory.button(
                PickerFieldFactory.selectedOrFallback(special.swingAnimationType, ItemEditorText.tr("special.advanced.select")), UiFactory.ButtonTextPreset.STANDARD, 
                anchor -> context.openDropdown(
                        anchor,
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
        if (stacked) {
            card.child(piercingKnockback);
            card.child(piercingDismounts);
        } else {
            FlowLayout piercingFlags = responsiveRow();
        piercingFlags.gap(FLAGS_ROW_GAP);
            distributeRowChildren(piercingFlags, piercingKnockback, piercingDismounts);
            card.child(piercingFlags);
        }

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
                kineticLabelWidth
        );
        FlowLayout kineticDelayField = compactField(
                ItemEditorText.tr("special.advanced.component_tweaks.kinetic_delay_ticks"),
                UiFactory.textBox(special.kineticDelayTicks, context.bindText(value -> special.kineticDelayTicks = value))
                .horizontalSizing(Sizing.fill(100)),
                kineticLabelWidth
        );
        if (stacked) {
            card.child(kineticContactField);
            card.child(kineticDelayField);
        } else {
            FlowLayout kineticA = responsiveRow();
        kineticA.gap(FLAGS_ROW_GAP);
            distributeRowChildren(kineticA, kineticContactField, kineticDelayField);
            card.child(kineticA);
        }

        FlowLayout kineticForwardField = compactField(
                ItemEditorText.tr("special.advanced.component_tweaks.kinetic_forward_movement"),
                UiFactory.textBox(special.kineticForwardMovement, context.bindText(value -> special.kineticForwardMovement = value))
                .horizontalSizing(Sizing.fill(100)),
                kineticLabelWidth
        );
        FlowLayout kineticDamageField = compactField(
                ItemEditorText.tr("special.advanced.component_tweaks.kinetic_damage_multiplier"),
                UiFactory.textBox(special.kineticDamageMultiplier, context.bindText(value -> special.kineticDamageMultiplier = value))
                .horizontalSizing(Sizing.fill(100)),
                kineticLabelWidth
        );
        if (stacked) {
            card.child(kineticForwardField);
            card.child(kineticDamageField);
        } else {
            FlowLayout kineticB = responsiveRow();
        kineticB.gap(FLAGS_ROW_GAP);
            distributeRowChildren(kineticB, kineticForwardField, kineticDamageField);
            card.child(kineticB);
        }

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
        boolean stacked = prefersStackedCompactRows();
        FlowLayout row = stacked ? UiFactory.column() : responsiveRow();
        UIComponent input = UiFactory.textBox(value, text -> context.mutate(() -> setter.accept(IdFieldNormalizer.normalize(text))))
                .horizontalSizing(stacked ? Sizing.fill(100) : Sizing.expand(100));
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
        int pickWidth = compactFixedPickButtonWidth();
        pickButton.horizontalSizing(stacked ? Sizing.fill(100) : Sizing.fixed(pickWidth));
        row.child(pickButton);
        return row;
    }

    private static FlowLayout textWithPickerCompact(
            SpecialDataPanelContext context,
            String value,
            Consumer<String> setter,
            List<String> entries,
            String pickerTitle
    ) {
        boolean stacked = prefersStackedCompactRows();
        FlowLayout row = stacked ? UiFactory.column() : responsiveRow();
        row.child(UiFactory.textBox(value, context.bindText(setter)).horizontalSizing(stacked ? Sizing.fill(100) : Sizing.expand(100)));
        ButtonComponent pickButton = UiFactory.button(ItemEditorText.tr("common.pick"), UiFactory.ButtonTextPreset.STANDARD,  button ->
                context.openSearchablePicker(
                        pickerTitle,
                        "",
                        entries,
                        entry -> entry,
                        selected -> context.mutateRefresh(() -> setter.accept(selected))
                )
        );
        int pickWidth = compactFixedPickButtonWidth();
        pickButton.horizontalSizing(stacked ? Sizing.fill(100) : Sizing.fixed(pickWidth));
        pickButton.active(!entries.isEmpty());
        row.child(pickButton);
        return row;
    }

    private static List<BlockStatePropertyMeta> blockStatePropertyMeta(SpecialDataPanelContext context) {
        ItemStack stack = context.originalStack();
        if (!(stack.getItem() instanceof BlockItem blockItem)) {
            return List.of();
        }

        List<BlockStatePropertyMeta> metas = new ArrayList<>();
        for (Property<?> property : blockItem.getBlock().defaultBlockState().getProperties()) {
            metas.add(new BlockStatePropertyMeta(property.getName(), blockStatePropertyValues(property)));
        }
        metas.sort(Comparator.comparing(BlockStatePropertyMeta::key));
        return metas;
    }

    private static <T extends Comparable<T>> List<String> blockStatePropertyValues(Property<T> property) {
        List<String> values = new ArrayList<>();
        for (T value : property.getPossibleValues()) {
            values.add(property.getName(value));
        }
        values.sort(Comparator.naturalOrder());
        return values;
    }

    private static List<BlockStateEntryDraft> parseBlockStateEntries(String raw) {
        List<BlockStateEntryDraft> entries = new ArrayList<>();
        if (raw == null || raw.isBlank()) {
            return entries;
        }

        for (String part : raw.split("[,\\r\\n]+")) {
            String token = part.trim();
            if (token.isEmpty()) {
                continue;
            }
            int separator = token.indexOf('=');
            if (separator < 0) {
                entries.add(new BlockStateEntryDraft(token, ""));
                continue;
            }
            entries.add(new BlockStateEntryDraft(
                    token.substring(0, separator).trim(),
                    token.substring(separator + 1).trim()
            ));
        }

        return entries;
    }

    private static String serializeBlockStateEntries(List<BlockStateEntryDraft> entries) {
        List<String> tokens = new ArrayList<>();
        for (BlockStateEntryDraft entry : entries) {
            String key = entry.key == null ? "" : entry.key.trim();
            String value = entry.value == null ? "" : entry.value.trim();
            if (key.isEmpty() && value.isEmpty()) {
                continue;
            }
            tokens.add(key + "=" + value);
        }
        return String.join(", ", tokens);
    }

    private record BlockStatePropertyMeta(String key, List<String> values) {
    }

    private static final class BlockStateEntryDraft {
        private String key;
        private String value;

        private BlockStateEntryDraft(String key, String value) {
            this.key = key;
            this.value = value;
        }
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

