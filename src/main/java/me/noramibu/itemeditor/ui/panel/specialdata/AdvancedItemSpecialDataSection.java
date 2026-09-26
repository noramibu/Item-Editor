package me.noramibu.itemeditor.ui.panel.specialdata;

import static me.noramibu.itemeditor.ui.component.CompactFieldLayout.compactCheckboxRow;
import static me.noramibu.itemeditor.ui.component.CompactFieldLayout.compactField;
import static me.noramibu.itemeditor.ui.component.CompactFieldLayout.denseEquipmentRow;
import static me.noramibu.itemeditor.ui.component.CompactFieldLayout.distributeRowChildren;
import static me.noramibu.itemeditor.ui.component.CompactFieldLayout.guiWidth;
import static me.noramibu.itemeditor.ui.component.CompactFieldLayout.isNarrowLayout;
import static me.noramibu.itemeditor.ui.component.CompactFieldLayout.responsiveRow;
import static me.noramibu.itemeditor.ui.panel.specialdata.SpecialDataFieldFactory.compactTextField;
import static me.noramibu.itemeditor.ui.panel.specialdata.SpecialDataFieldFactory.compactTriStateBooleanPicker;
import static me.noramibu.itemeditor.ui.panel.specialdata.SpecialDataFieldFactory.filledTextBox;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.HorizontalAlignment;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.UIComponent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import me.noramibu.itemeditor.editor.EditorCategory;
import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.service.ComponentRemovalService;
import me.noramibu.itemeditor.ui.component.CompactFieldLayout;
import me.noramibu.itemeditor.ui.component.CompactFieldLayout.Width;
import me.noramibu.itemeditor.ui.component.EditorSearchDialog;
import me.noramibu.itemeditor.ui.component.EditorSectionSummary;
import me.noramibu.itemeditor.ui.component.PickerFieldFactory;
import me.noramibu.itemeditor.ui.component.RawTextAreaComponent;
import me.noramibu.itemeditor.ui.component.UiFactory;
import me.noramibu.itemeditor.util.IdFieldNormalizer;
import me.noramibu.itemeditor.util.ItemEditorCapabilities;
import me.noramibu.itemeditor.util.ItemEditorText;
import me.noramibu.itemeditor.util.LootTableIds;
import me.noramibu.itemeditor.util.TextComponentUtil;
import me.noramibu.itemeditor.util.ValidationUtil;
import net.minecraft.advancements.predicates.DataComponentMatchers;
import net.minecraft.advancements.predicates.ItemPredicate;
import net.minecraft.advancements.predicates.MinMaxBounds;
import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentExactPredicate;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

public final class AdvancedItemSpecialDataSection {
    private static final int PICKER_ROW_INLINE_MIN_WIDTH = 360;
    private static final int PICKER_ROW_INPUT_MIN_WIDTH = 180;
    private static final int STORAGE_PICK_INPUT_MIN_WIDTH = 96;
    static final int SECTION_ROW_GAP = 10;
    private static final int COLLAPSIBLE_HEADER_TITLE_RESERVE = 26;
    static final String SYMBOL_SECTION_COLLAPSED = "+";
    static final String SYMBOL_SECTION_EXPANDED = "-";
    private static final int CONSUMABLE_EFFECTS_EMPTY_HINT_WIDTH = 300;
    private static final int CONSUMABLE_APPLY_EFFECTS_EMPTY_HINT_WIDTH = 280;
    private static final int MAP_DECORATIONS_EMPTY_HINT_WIDTH = 280;
    static final int COMBAT_REPAIRABLE_EMPTY_HINT_WIDTH = 280;
    private static final int CONTAINER_META_LOCK_HINT_WIDTH = 360;
    private static final int CONTAINER_META_BEES_EMPTY_HINT_WIDTH = 320;
    private static final int CONTAINER_META_BEE_SUMMARY_HINT_WIDTH = 340;
    private static final int CROSSBOW_EMPTY_HINT_WIDTH = 320;
    private static final int CROSSBOW_PROJECTILE_SUMMARY_HINT_WIDTH = 340;
    private static final int MAP_DECORATION_SUMMARY_HINT_WIDTH = 360;
    private static final int CUSTOM_DATA_EDITOR_HEIGHT = 220;
    private static final int CUSTOM_DATA_CONTENT_PADDING = 4;
    private static final int CUSTOM_DATA_TEXT_WIDTH_RESERVE = 14;
    private static final int CUSTOM_DATA_HINT_MIN_WIDTH = 96;

    private enum Control implements ComponentSearchField {
        CUSTOM_DATA_TITLE("custom", "special.advanced.custom_data.title"),
        CUSTOM_DATA_EDITOR("custom", "special.advanced.custom_data.editor"),
        FOOD_TITLE("food", "special.advanced.food.title"),
        FOOD_NUTRITION("food", "special.advanced.food.nutrition"),
        FOOD_SATURATION("food", "special.advanced.food.saturation"),
        CONSUMABLE_CONSUME_SECONDS("food", "special.advanced.consumable.consume_seconds"),
        FOOD_CAN_ALWAYS_EAT("food", "special.advanced.food.can_always_eat"),
        CONSUMABLE_HAS_PARTICLES("food", "special.advanced.consumable.has_particles"),
        CONSUMABLE_SOUND("food", "special.advanced.consumable.sound"),
        USE_EFFECTS_CAN_SPRINT("food", "special.advanced.use_effects.can_sprint"),
        USE_EFFECTS_INTERACT_VIBRATIONS("food", "special.advanced.use_effects.interact_vibrations"),
        USE_EFFECTS_SPEED_MULTIPLIER("food", "special.advanced.use_effects.speed_multiplier"),
        USE_REMAINDER_TITLE("food", "special.advanced.use_remainder.title"),
        COMMON_RESTORE("action", "common.restore"),
        COMMON_REMOVE("action", "common.remove"),
        USE_REMAINDER_ITEM_ID("remainder", "special.advanced.use_remainder.item_id"),
        USE_REMAINDER_COUNT("remainder", "special.advanced.use_remainder.count"),
        USE_COOLDOWN_SECONDS("food", "special.advanced.use_cooldown.seconds"),
        USE_COOLDOWN_GROUP("food", "special.advanced.use_cooldown.group"),
        COMMON_RESET("action", "common.reset"),
        CONSUMABLE_ANIMATION("food", "special.advanced.consumable.animation"),
        CONSUMABLE_ON_CONSUME_EFFECTS("food", "special.advanced.consumable.on_consume_effects"),
        CONSUMABLE_ADD_EFFECT("food", "special.advanced.consumable.add_effect"),
        CONSUMABLE_EFFECT("entryTitle", "special.advanced.consumable.effect"),
        COMPONENT_TWEAKS_DEATH_EFFECTS("death", "special.advanced.component_tweaks.death_effects"),
        COMPONENT_TWEAKS_ADD_DEATH_EFFECT("death", "special.advanced.component_tweaks.add_death_effect"),
        COMPONENT_TWEAKS_DEATH_EFFECT("entryTitle", "special.advanced.component_tweaks.death_effect"),
        CONSUMABLE_EFFECT_TYPE("effect", "special.advanced.consumable.effect_type"),
        CONSUMABLE_EFFECT_SOUND("soundEffect", "special.advanced.consumable.effect_sound"),
        CONSUMABLE_REMOVE_EFFECTS("removeEffect", "special.advanced.consumable.remove_effects"),
        CONSUMABLE_DIAMETER("teleportEffect", "special.advanced.consumable.diameter"),
        CONSUMABLE_EFFECT_PROBABILITY("apply", "special.advanced.consumable.effect_probability"),
        POTION_ADD_EFFECT("apply", "special.potion.add_effect"),
        POTION_EFFECT("entryTitle", "special.potion.effect"),
        POTION_EFFECT_ID("potion", "special.potion.effect_id"),
        POTION_DURATION("potion", "special.potion.duration"),
        POTION_AMPLIFIER("potion", "special.potion.amplifier"),
        POTION_AMBIENT("potion", "special.potion.ambient"),
        POTION_VISIBLE("potion", "special.potion.visible"),
        POTION_SHOW_ICON("potion", "special.potion.show_icon"),
        CONTAINER_META_LOCK_MATCH_TITLE("lock", "special.advanced.container_meta.lock_match_title"),
        CONTAINER_META_LOCK_MATCH_COUNT("lock", "special.advanced.container_meta.lock_match_count"),
        CONTAINER_META_LOCK_MATCH_NAME("lock", "special.advanced.container_meta.lock_match_name"),
        CONTAINER_META_LOCK_MATCH_LORE("lock", "special.advanced.container_meta.lock_match_lore"),
        CONTAINER_META_LOCK_MATCH_ENCHANTMENTS("lock", "special.advanced.container_meta.lock_match_enchantments"),
        CONTAINER_META_LOCK_MATCH_CUSTOM_DATA("lock", "special.advanced.container_meta.lock_match_custom_data"),
        CONTAINER_META_LOCK_MATCH_ALL_COMPONENTS("lock", "special.advanced.container_meta.lock_match_all_components"),
        CONTAINER_META_LOCK_MATCH_FULL_ITEM("lock", "special.advanced.container_meta.lock_match_full_item"),
        CONTAINER_META_LOCK_RESET_SIMPLE("lock", "special.advanced.container_meta.lock_reset_simple"),
        COMMON_COUNT("projectile", "common.count"),
        CONTAINER_META_TITLE("container", "special.advanced.container_meta.title"),
        CONTAINER_META_LOCK_ITEM("container", "special.advanced.container_meta.lock_item"),
        CONTAINER_META_LOOT_TABLE("container", "special.advanced.container_meta.loot_table"),
        CONTAINER_META_LOCK_PREDICATE("container", "special.advanced.container_meta.lock_predicate"),
        CONTAINER_META_LOOT_SEED("container", "special.advanced.container_meta.loot_seed"),
        CONTAINER_META_BEES_TITLE("container", "special.advanced.container_meta.bees_title"),
        CONTAINER_META_BEES_ADD("container", "special.advanced.container_meta.bees_add"),
        COMMON_CLEAR_ALL("action", "common.clear_all"),
        COMMON_EXPAND_ALL("action", "common.expand_all"),
        COMMON_COLLAPSE_ALL("action", "common.collapse_all"),
        CONTAINER_META_BEE("entryTitle", "special.advanced.container_meta.bee"),
        COMMON_ENTITY_ID("bee", "common.entity_id"),
        CONTAINER_META_BEES_TICKS("bee", "special.advanced.container_meta.bees_ticks"),
        CONTAINER_META_BEES_MIN_TICKS("bee", "special.advanced.container_meta.bees_min_ticks"),
        CROSSBOW_TITLE("crossbow", "special.advanced.crossbow.title"),
        CROSSBOW_ADD_PROJECTILE("crossbow", "special.advanced.crossbow.add_projectile"),
        CROSSBOW_PROJECTILE("entryTitle", "special.advanced.crossbow.projectile"),
        CROSSBOW_ITEM("projectile", "special.advanced.crossbow.item"),
        MAP_TITLE("map", "special.advanced.map.title"),
        MAP_MAP_ID("map", "special.advanced.map.map_id"),
        CONTAINER_META_POT_TITLE("container", "special.advanced.container_meta.pot_title"),
        CONTAINER_META_POT_BACK("container", "special.advanced.container_meta.pot_back"),
        CONTAINER_META_POT_LEFT("container", "special.advanced.container_meta.pot_left"),
        CONTAINER_META_POT_RIGHT("container", "special.advanced.container_meta.pot_right"),
        CONTAINER_META_POT_FRONT("container", "special.advanced.container_meta.pot_front"),
        MAP_DECORATIONS_TITLE("map", "special.advanced.map.decorations_title"),
        MAP_ADD_DECORATION("map", "special.advanced.map.add_decoration"),
        MAP_DECORATION("entryTitle", "special.advanced.map.decoration"),
        MAP_DECORATION_KEY("decoration", "special.advanced.map.decoration_key"),
        MAP_DECORATION_TYPE("decoration", "special.advanced.map.decoration_type"),
        MAP_DECORATION_X("decoration", "special.advanced.map.decoration_x"),
        MAP_DECORATION_Z("decoration", "special.advanced.map.decoration_z"),
        MAP_DECORATION_ROTATION("decoration", "special.advanced.map.decoration_rotation"),
        MAP_LODESTONE_TITLE("map", "special.advanced.map.lodestone_title"),
        MAP_LODESTONE_ENABLED("map", "special.advanced.map.lodestone_enabled"),
        MAP_LODESTONE_TRACKED("lodestone", "special.advanced.map.lodestone_tracked"),
        MAP_LODESTONE_DIMENSION("lodestone", "special.advanced.map.lodestone_dimension"),
        MAP_LODESTONE_X("lodestone", "special.advanced.map.lodestone_x"),
        MAP_LODESTONE_Y("lodestone", "special.advanced.map.lodestone_y"),
        MAP_LODESTONE_Z("lodestone", "special.advanced.map.lodestone_z"),
        COMPONENT_TWEAKS_NAMING_TITLE("naming", "special.advanced.component_tweaks.naming_title"),
        BLOCK_STATE_TITLE("block", "special.advanced.block_state.title"),
        COMPONENT_TWEAKS_ITEM_NAME("naming", "special.advanced.component_tweaks.item_name"),
        COMPONENT_TWEAKS_MIN_ATTACK_CHARGE("naming", "special.advanced.component_tweaks.min_attack_charge"),
        COMPONENT_TWEAKS_ENCHANTABLE("naming", "special.advanced.component_tweaks.enchantable"),
        COMPONENT_TWEAKS_OMINOUS_AMPLIFIER("naming", "special.advanced.component_tweaks.ominous_amplifier"),
        COMPONENT_TWEAKS_TOOLTIP_STYLE("naming", "special.advanced.component_tweaks.tooltip_style"),
        COMPONENT_TWEAKS_GLIDER("naming", "special.advanced.component_tweaks.glider"),
        COMPONENT_TWEAKS_INTANGIBLE_PROJECTILE("naming", "special.advanced.component_tweaks.intangible_projectile"),
        COMPONENT_TWEAKS_DEATH_PROTECTION("naming", "special.advanced.component_tweaks.death_protection"),
        COMPONENT_TWEAKS_BLOCK_STATE("block", "special.advanced.component_tweaks.block_state"),
        COMMON_PICK("action", "common.pick"),
        COMPONENT_TWEAKS_ALLOW_TAG_EXPANSION("holder", "special.advanced.component_tweaks.allow_tag_expansion"),
        COMMON_ADD_TYPE("action", "common.add_type"),
        COMMON_ADD_TAG("action", "common.add_tag"),
        COMMON_TAG("action", "common.tag"),
        COMMON_TYPE("action", "common.type"),
        COMMON_ENTRY("action", "common.entry");

        private final String group;
        private final String key;

        Control(String group, String key) {
            this.group = group;
            this.key = key;
        }

        @Override
        public String key() {
            return key;
        }
    }

    private AdvancedItemSpecialDataSection() {}

    public static List<EditorSearchDialog.Target> searchTargets(SpecialDataPanelContext context) {
        List<EditorSearchDialog.Target> targets = new ArrayList<>();
        ItemEditorState.SpecialData special = context.special();
        Runnable naming = () -> special.uiComponentTweaksNamingCollapsed = false;
        Runnable food = () -> special.uiFoodConsumableCollapsed = false;
        Runnable container = () -> special.uiContainerMetadataCollapsed = false;
        Runnable crossbow = () -> special.uiCrossbowCollapsed = false;
        Runnable map = () -> special.uiMapAdvancedCollapsed = false;
        addGroup(targets, context, "naming", List.of(Control.COMPONENT_TWEAKS_NAMING_TITLE.text()), () -> "", naming);

        addGroup(targets, context, "food", List.of(Control.FOOD_TITLE.text()), () -> "", food);
        targets.add(Control.COMMON_RESET.target(
                context,
                EditorCategory.COMPONENTS,
                List.of(Control.FOOD_TITLE.text()),
                () -> sectionScope(Control.FOOD_TITLE),
                food));
        if (hasUseRemainderDefault(context)) {
            Control action =
                    context.screen().session().state().removedComponents.contains(DataComponents.USE_REMAINDER)
                            ? Control.COMMON_RESTORE
                            : Control.COMMON_REMOVE;
            targets.add(action.target(
                    context,
                    EditorCategory.COMPONENTS,
                    List.of(Control.FOOD_TITLE.text(), Control.USE_REMAINDER_TITLE.text()),
                    () -> "component-remainder",
                    food));
        }
        if (!context.screen().session().state().removedComponents.contains(DataComponents.USE_REMAINDER)) {
            addGroup(
                    targets,
                    context,
                    "remainder",
                    List.of(Control.FOOD_TITLE.text(), Control.USE_REMAINDER_TITLE.text()),
                    () -> "",
                    food);
        }
        addEffects(
                targets,
                context,
                special.consumableOnConsumeEffects,
                Control.FOOD_TITLE,
                Control.CONSUMABLE_EFFECT,
                "consume",
                food);
        if (special.deathProtection) {
            addGroup(
                    targets, context, "death", List.of(Control.COMPONENT_TWEAKS_NAMING_TITLE.text()), () -> "", naming);
            addEffects(
                    targets,
                    context,
                    special.deathProtectionEffects,
                    Control.COMPONENT_TWEAKS_NAMING_TITLE,
                    Control.COMPONENT_TWEAKS_DEATH_EFFECT,
                    "death",
                    naming);
        }
        addGroup(
                targets,
                context,
                "custom",
                List.of(Control.CUSTOM_DATA_TITLE.text()),
                () -> "",
                () -> special.uiCustomDataCollapsed = false);
        if (supportsBlockState(context.originalStack())) {
            Runnable block = () -> special.uiBlockStateCollapsed = false;
            addGroup(targets, context, "block", List.of(Control.BLOCK_STATE_TITLE.text()), () -> "", block);

            targets.add(Control.COMMON_RESET.target(
                    context,
                    EditorCategory.COMPONENTS,
                    List.of(Control.BLOCK_STATE_TITLE.text()),
                    () -> sectionScope(Control.BLOCK_STATE_TITLE),
                    block));
            for (BlockStatePropertyMeta property : blockStatePropertyMeta(context)) {
                targets.add(new EditorSearchDialog.Target(
                        List.of(
                                EditorCategory.COMPONENTS.title().getString(),
                                Control.BLOCK_STATE_TITLE.text(),
                                property.key()),
                        property.key() + " " + String.join(" ", property.values()),
                        () -> {
                            block.run();
                            context.screen().revealSearchTarget(EditorCategory.COMPONENTS, blockStateAnchor(property));
                        }));
            }
        }
        if (supportsContainerMetadata(context.originalStack())) {
            addGroup(targets, context, "container", List.of(Control.CONTAINER_META_TITLE.text()), () -> "", container);
            if (!special.lockKeyTemplateSnbt.isBlank()) {
                addGroup(
                        targets,
                        context,
                        "lock",
                        List.of(Control.CONTAINER_META_TITLE.text(), Control.CONTAINER_META_LOCK_MATCH_TITLE.text()),
                        () -> "",
                        container);
            }

            addListActions(
                    targets,
                    context,
                    List.of(Control.CONTAINER_META_TITLE.text(), Control.CONTAINER_META_BEES_TITLE.text()),
                    "component-list-bees",
                    container,
                    !special.beesOccupants.isEmpty(),
                    true);
            addEntries(
                    targets,
                    context,
                    "bee",
                    Control.CONTAINER_META_TITLE,
                    Control.CONTAINER_META_BEE,
                    special.beesOccupants,
                    container,
                    draft -> draft.uiCollapsed = false);
        }
        if (supportsCrossbow(context.originalStack())) {
            addGroup(targets, context, "crossbow", List.of(Control.CROSSBOW_TITLE.text()), () -> "", crossbow);

            addListActions(
                    targets,
                    context,
                    List.of(Control.CROSSBOW_TITLE.text()),
                    sectionScope(Control.CROSSBOW_TITLE),
                    crossbow,
                    !special.chargedProjectiles.isEmpty(),
                    !special.chargedProjectiles.isEmpty());
            addEntries(
                    targets,
                    context,
                    "projectile",
                    Control.CROSSBOW_TITLE,
                    Control.CROSSBOW_PROJECTILE,
                    special.chargedProjectiles,
                    crossbow,
                    draft -> draft.uiCollapsed = false);
        }
        if (supportsMapAdvanced(context.originalStack())) {
            addGroup(targets, context, "map", List.of(Control.MAP_TITLE.text()), () -> "", map);
            if (special.lodestoneEnabled) {
                addGroup(
                        targets,
                        context,
                        "lodestone",
                        List.of(Control.MAP_TITLE.text(), Control.MAP_LODESTONE_TITLE.text()),
                        () -> "",
                        map);
            }

            addListActions(
                    targets,
                    context,
                    List.of(Control.MAP_TITLE.text(), Control.MAP_DECORATIONS_TITLE.text()),
                    "component-list-decorations",
                    map,
                    !special.mapDecorations.isEmpty(),
                    true);
            addEntries(
                    targets,
                    context,
                    "decoration",
                    Control.MAP_TITLE,
                    Control.MAP_DECORATION,
                    special.mapDecorations,
                    map,
                    draft -> draft.uiCollapsed = false);
        }
        return List.copyOf(targets);
    }

    private static String sectionScope(Control title) {
        return "component-section-" + ItemEditorText.key(title.key());
    }

    private static boolean hasUseRemainderDefault(SpecialDataPanelContext context) {
        return ComponentRemovalService.hasDefault(context.originalStack(), DataComponents.USE_REMAINDER);
    }

    private static void addListActions(
            List<EditorSearchDialog.Target> targets,
            SpecialDataPanelContext context,
            List<String> path,
            String scope,
            Runnable expand,
            boolean hasEntries,
            boolean hasClear) {
        if (hasClear) {
            targets.add(Control.COMMON_CLEAR_ALL.target(context, EditorCategory.COMPONENTS, path, () -> scope, expand));
        }
        if (hasEntries) {
            targets.add(
                    Control.COMMON_EXPAND_ALL.target(context, EditorCategory.COMPONENTS, path, () -> scope, expand));
            targets.add(
                    Control.COMMON_COLLAPSE_ALL.target(context, EditorCategory.COMPONENTS, path, () -> scope, expand));
        }
    }

    private static void addGroup(
            List<EditorSearchDialog.Target> targets,
            SpecialDataPanelContext context,
            String group,
            List<String> path,
            Supplier<String> scope,
            Runnable expand) {
        for (Control field : Control.values()) {
            if (field.group.equals(group)) {
                targets.add(field.target(context, EditorCategory.COMPONENTS, path, scope, expand));
            }
        }
    }

    private static <T> void addEntries(
            List<EditorSearchDialog.Target> targets,
            SpecialDataPanelContext context,
            String group,
            Control section,
            Control title,
            List<T> entries,
            Runnable expandSection,
            Consumer<T> expandEntry) {
        for (int index = 0; index < entries.size(); index++) {
            T entry = entries.get(index);
            addGroup(
                    targets,
                    context,
                    group,
                    List.of(section.text(), title.text(index + 1)),
                    () -> ComponentSearchField.scope(group, entries, entry),
                    () -> {
                        expandSection.run();
                        expandEntry.accept(entry);
                    });
        }
    }

    private static void addEffects(
            List<EditorSearchDialog.Target> targets,
            SpecialDataPanelContext context,
            List<ItemEditorState.ConsumableEffectDraft> entries,
            Control section,
            Control title,
            String group,
            Runnable expandSection) {
        for (int index = 0; index < entries.size(); index++) {
            ItemEditorState.ConsumableEffectDraft entry = entries.get(index);
            List<String> path = List.of(section.text(), title.text(index + 1));
            Supplier<String> scope = () -> ComponentSearchField.scope(group, entries, entry);
            Runnable expand = () -> {
                expandSection.run();
                entry.uiCollapsed = false;
            };
            addGroup(targets, context, "effect", path, scope, expand);
            String type = consumeEffectType(entry);
            switch (type) {
                case ItemEditorState.ConsumableEffectDraft.TYPE_REMOVE_EFFECTS ->
                    addGroup(targets, context, "removeEffect", path, scope, expand);
                case ItemEditorState.ConsumableEffectDraft.TYPE_CLEAR_ALL_EFFECTS -> {}
                case ItemEditorState.ConsumableEffectDraft.TYPE_PLAY_SOUND ->
                    addGroup(targets, context, "soundEffect", path, scope, expand);
                case ItemEditorState.ConsumableEffectDraft.TYPE_TELEPORT_RANDOMLY ->
                    addGroup(targets, context, "teleportEffect", path, scope, expand);
                default -> {
                    addGroup(targets, context, "apply", path, scope, expand);
                    for (int effectIndex = 0; effectIndex < entry.effects.size(); effectIndex++) {
                        ItemEditorState.PotionEffectDraft effect = entry.effects.get(effectIndex);
                        addGroup(
                                targets,
                                context,
                                "potion",
                                List.of(
                                        section.text(),
                                        title.text(index + 1),
                                        Control.POTION_EFFECT.text(effectIndex + 1)),
                                () -> {
                                    String parent = scope.get();
                                    int current = ComponentSearchField.identityIndex(entry.effects, effect);
                                    return parent == null || current < 0 ? null : parent + "-potion-" + current;
                                },
                                expand);
                    }
                }
            }
        }
    }

    private static String consumeEffectType(ItemEditorState.ConsumableEffectDraft draft) {
        return draft.type == null || draft.type.isBlank()
                ? ItemEditorState.ConsumableEffectDraft.TYPE_APPLY_EFFECTS
                : draft.type;
    }

    private static String blockStateAnchor(BlockStatePropertyMeta property) {
        return "component-block-state-" + property.key();
    }

    public static boolean supportsContainerMetadata(ItemStack stack) {
        return stack.has(DataComponents.LOCK)
                || stack.has(DataComponents.CONTAINER_LOOT)
                || stack.has(DataComponents.BEES)
                || stack.has(DataComponents.POT_DECORATIONS)
                || ItemEditorCapabilities.supportsContainerData(stack)
                || stack.is(Items.BEEHIVE)
                || stack.is(Items.BEE_NEST)
                || stack.is(Items.DECORATED_POT);
    }

    public static boolean supportsCrossbow(ItemStack stack) {
        return stack.has(DataComponents.CHARGED_PROJECTILES) || stack.is(Items.CROSSBOW);
    }

    public static boolean supportsMapAdvanced(ItemStack stack) {
        return stack.has(DataComponents.MAP_ID)
                || stack.has(DataComponents.MAP_DECORATIONS)
                || stack.has(DataComponents.LODESTONE_TRACKER)
                || stack.is(Items.FILLED_MAP);
    }

    public static boolean supportsBlockState(ItemStack stack) {
        return stack.getItem() instanceof BlockItem blockItem
                && !blockItem.getBlock().defaultBlockState().getProperties().isEmpty();
    }

    public static FlowLayout buildComponentTweakNamingSection(SpecialDataPanelContext context) {
        return buildComponentTweakNamingSection(context, context.special());
    }

    public static FlowLayout buildBlockState(SpecialDataPanelContext context) {
        return buildBlockState(context, context.special());
    }

    public static FlowLayout buildCustomData(SpecialDataPanelContext context) {
        ItemEditorState.SpecialData special = context.special();
        return collapsibleCard(
                context,
                Control.CUSTOM_DATA_TITLE.label(),
                special.uiCustomDataCollapsed,
                value -> special.uiCustomDataCollapsed = value,
                () -> {
                    int contentWidth = customDataContentWidth(context);
                    FlowLayout content = UiFactory.column();
                    content.padding(Insets.of(
                            0,
                            0,
                            UiFactory.scaledPixels(CUSTOM_DATA_CONTENT_PADDING),
                            UiFactory.scaledPixels(CUSTOM_DATA_CONTENT_PADDING)));
                    content.child(
                            wrappedMutedText(ItemEditorText.tr("special.advanced.custom_data.hint"), contentWidth));
                    RawTextAreaComponent editor = customDataEditor(context, special);
                    ButtonComponent openString = UiFactory.button(
                            ItemEditorText.tr("raw_editor.string.open"),
                            UiFactory.ButtonTextPreset.COMPACT,
                            button -> context.screen().openRawStringEditor(editor));
                    openString.tooltip(UiFactory.tooltipLines(ItemEditorText.tr("raw_editor.string.tooltip"), 260));
                    content.child(UiFactory.actionButtonRow(openString));
                    content.child(compactField(Control.CUSTOM_DATA_EDITOR.label(), editor, contentWidth));
                    return content;
                });
    }

    private static RawTextAreaComponent customDataEditor(
            SpecialDataPanelContext context, ItemEditorState.SpecialData special) {
        RawTextAreaComponent editor = new RawTextAreaComponent(
                Sizing.fill(100), UiFactory.fixed(CUSTOM_DATA_EDITOR_HEIGHT), special.customDataSnbt);
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
                Math.max(CUSTOM_DATA_HINT_MIN_WIDTH, guiWidth()));
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
                Control.FOOD_TITLE.label(),
                special.uiFoodConsumableCollapsed,
                value -> special.uiFoodConsumableCollapsed = value,
                () -> {
                    boolean narrowLayout = isNarrowLayout();
                    int compactNumberWidth = Width.NUMERIC.pixels();
                    int compactIdWidth = narrowLayout ? Width.NARROW_ID.pixels() : Width.ID.pixels();
                    int compactGroupWidth = Width.GROUP.pixels();
                    FlowLayout content = UiFactory.column();
                    content.child(denseEquipmentRow(
                            context.boundTextField(Control.FOOD_NUTRITION.key(), compactNumberWidth),
                            context.boundTextField(Control.FOOD_SATURATION.key(), compactNumberWidth),
                            context.boundTextField(Control.CONSUMABLE_CONSUME_SECONDS.key(), compactNumberWidth)));

                    Component alwaysEatLabel = Control.FOOD_CAN_ALWAYS_EAT.label();
                    Component particlesLabel = Control.CONSUMABLE_HAS_PARTICLES.label();
                    int toggleWidth = Math.max(
                                    Minecraft.getInstance().font.width(alwaysEatLabel)
                                            + UiFactory.scaleProfile().controlHeight(),
                                    Minecraft.getInstance().font.width(particlesLabel))
                            + UiFactory.scaleProfile().padding() * 2;
                    content.child(new CompactFieldLayout(
                            List.of(
                                    compactTriStateBooleanPicker(
                                            context,
                                            alwaysEatLabel,
                                            special.foodCanAlwaysEat,
                                            value -> special.foodCanAlwaysEat = value,
                                            Width.PICKER.pixels()),
                                    compactTriStateBooleanPicker(
                                            context,
                                            particlesLabel,
                                            special.consumableHasParticles,
                                            value -> special.consumableHasParticles = value,
                                            Width.PICKER.pixels())),
                            toggleWidth));

                    content.child(PickerFieldFactory.enumField(
                            context,
                            Control.CONSUMABLE_ANIMATION.label(),
                            special.consumableAnimation,
                            ItemUseAnimation.values(),
                            value -> special.consumableAnimation = value));

                    content.child(context.boundIdField(
                            Control.CONSUMABLE_SOUND.key(),
                            context.optionalRegistryIds(Registries.SOUND_EVENT),
                            compactIdWidth));
                    content.child(buildOnConsumeEffectsEditor(context, special));

                    content.child(compactCheckboxRow(
                            UiFactory.checkbox(
                                    Control.USE_EFFECTS_CAN_SPRINT.label(),
                                    special.useEffectsCanSprint,
                                    context.bindToggle(value -> special.useEffectsCanSprint = value)),
                            UiFactory.checkbox(
                                    Control.USE_EFFECTS_INTERACT_VIBRATIONS.label(),
                                    special.useEffectsInteractVibrations,
                                    context.bindToggle(value -> special.useEffectsInteractVibrations = value))));
                    content.child(denseEquipmentRow(
                            context.boundTextField(Control.USE_EFFECTS_SPEED_MULTIPLIER.key(), compactNumberWidth)));
                    var state = context.screen().session().state();
                    boolean useRemainderHasDefault = hasUseRemainderDefault(context);
                    boolean useRemainderRemoved = state.removedComponents.contains(DataComponents.USE_REMAINDER);
                    FlowLayout remainderHeader = UiFactory.row();
                    remainderHeader.id("component-remainder");
                    remainderHeader.child(UiFactory.title(Control.USE_REMAINDER_TITLE.label())
                            .shadow(false)
                            .horizontalSizing(Sizing.expand(100)));
                    if (useRemainderHasDefault) {
                        ButtonComponent removalButton = UiFactory.actionToneButton(
                                ItemEditorText.tr(
                                        useRemainderRemoved
                                                ? Control.COMMON_RESTORE.key()
                                                : Control.COMMON_REMOVE.key()),
                                UiFactory.ButtonTextPreset.COMPACT,
                                useRemainderRemoved ? UiFactory.ActionTone.PICKER : UiFactory.ActionTone.NEGATIVE,
                                button -> context.mutateRefresh(() -> {
                                    if (useRemainderRemoved)
                                        state.removedComponents.remove(DataComponents.USE_REMAINDER);
                                    else state.removedComponents.add(DataComponents.USE_REMAINDER);
                                }));
                        removalButton.horizontalSizing(Sizing.fixed(Width.PICKER.pixels()));
                        remainderHeader.child(removalButton);
                    }
                    content.child(remainderHeader);
                    if (!useRemainderRemoved) {
                        content.child(context.itemRow(
                                () -> editableItem(
                                        context,
                                        special.useRemainderItemId,
                                        special.useRemainderCount,
                                        special.useRemainderTemplateSnbt),
                                stack -> {
                                    special.useRemainderItemId = BuiltInRegistries.ITEM
                                            .getKey(stack.getItem())
                                            .toString();
                                    special.useRemainderCount = Integer.toString(stack.getCount());
                                    special.useRemainderTemplateSnbt = encodeItemStackTemplate(context, stack);
                                },
                                null,
                                ItemEditorText.tr("special.advanced.use_remainder.title")));
                    }
                    content.child(denseEquipmentRow(
                            context.boundTextField(Control.USE_COOLDOWN_SECONDS.key(), compactNumberWidth),
                            context.boundTextField(Control.USE_COOLDOWN_GROUP.key(), compactGroupWidth)));

                    FlowLayout actions = responsiveRow();
                    ButtonComponent resetAll = UiFactory.button(
                            Control.COMMON_RESET.label(),
                            UiFactory.ButtonTextPreset.STANDARD,
                            button -> context.mutateRefresh(() -> {
                                special.foodNutrition = "";
                                special.foodSaturation = "";
                                special.foodCanAlwaysEat = "";
                                special.consumableConsumeSeconds = "";
                                special.consumableAnimation = "";
                                special.consumableSoundId = "";
                                special.consumableHasParticles = "";
                                special.consumableOnConsumeEffects.clear();
                                special.useEffectsCanSprint = false;
                                special.useEffectsInteractVibrations = false;
                                special.useEffectsSpeedMultiplier = "";
                                special.useRemainderItemId = "";
                                special.useRemainderCount = "";
                                special.useRemainderTemplateSnbt = "";
                                state.removedComponents.remove(DataComponents.USE_REMAINDER);
                                special.useCooldownSeconds = "";
                                special.useCooldownGroup = "";
                            }));
                    resetAll.horizontalSizing(Sizing.fill(100));
                    actions.child(resetAll);
                    content.child(actions);
                    return content;
                });
    }

    static FlowLayout collapsibleCard(
            SpecialDataPanelContext context,
            Component title,
            boolean collapsed,
            Consumer<Boolean> setter,
            Supplier<FlowLayout> contentBuilder) {
        FlowLayout card = UiFactory.subCard();
        if (title.getContents() instanceof TranslatableContents translation) {
            card.id("component-section-" + translation.getKey());
        }
        int toggleWidth = Width.ICON.pixels();
        int preferredTitleWidth =
                Math.max(30, guiWidth() - toggleWidth - UiFactory.scaledPixels(COLLAPSIBLE_HEADER_TITLE_RESERVE));
        int titleWidth = Math.clamp(preferredTitleWidth, 1, Math.max(1, guiWidth()));
        Component fittedTitle = UiFactory.fitToWidth(title, titleWidth);
        var titleLabel = UiFactory.title(fittedTitle).shadow(false).horizontalSizing(Sizing.expand(100));
        if (!Objects.equals(fittedTitle.getString(), title.getString())) {
            titleLabel.tooltip(List.of(title));
        }
        if (title.getContents() instanceof TranslatableContents translation) {
            titleLabel.id(translation.getKey());
        }
        card.child(UiFactory.collapsibleHeader(titleLabel, collapsed, () -> {
            setter.accept(!collapsed);
            context.screen().refreshCurrentPanel();
        }));
        if (!collapsed) {
            card.child(contentBuilder.get());
        } else {
            Component summary = EditorSectionSummary.forSection(
                    title, context.screen().session().state());
            if (!summary.getString().isBlank()) card.child(EditorSectionSummary.label(summary, titleWidth));
        }
        return card;
    }

    static FlowLayout compactIdField(
            SpecialDataPanelContext context,
            Component label,
            String value,
            Consumer<String> setter,
            List<String> entries,
            String pickerTitle,
            int textWidth) {
        return compactField(
                label, textWithPickerCompact(context, value, setter, entries, pickerTitle, true), textWidth + 110);
    }

    private static FlowLayout itemIdInputWithStoragePick(
            SpecialDataPanelContext context,
            String value,
            Consumer<String> setter,
            Consumer<ItemStack> storageSelectionConsumer,
            String pickerTitle,
            Supplier<ItemStack> currentItem) {
        int rowGap = holderSetRowGap();
        int panelWidth = guiWidth();
        int pickWidth = Width.FIXED_PICK.pixels();
        int minInputWidth = Math.clamp(panelWidth, 1, STORAGE_PICK_INPUT_MIN_WIDTH);
        boolean stacked = panelWidth < minInputWidth + pickWidth * 2 + (rowGap * 2);
        FlowLayout row = stacked ? UiFactory.column() : UiFactory.row();
        row.gap(rowGap);

        ButtonComponent editButton =
                context.itemEditButton(currentItem, storageSelectionConsumer, Component.literal(pickerTitle));
        UIComponent input = UiFactory.textBox(value, text -> {
                    context.mutate(() -> setter.accept(IdFieldNormalizer.normalize(text)));
                    context.updateItemEditButton(editButton, currentItem.get());
                })
                .horizontalSizing(Sizing.expand(100));
        ButtonComponent pickButton = context.itemPickButton(pickerTitle, storageSelectionConsumer);

        if (stacked) {
            row.child(input.horizontalSizing(Sizing.fill(100)));
            row.child(UiFactory.actionButtonRow(editButton, pickButton));
        } else {
            row.child(input);
            editButton.horizontalSizing(Sizing.fixed(pickWidth));
            row.child(editButton);
            pickButton.horizontalSizing(Sizing.fixed(pickWidth));
            row.child(pickButton);
        }
        return row;
    }

    private static String encodeItemStackTemplate(SpecialDataPanelContext context, ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "";
        }
        try {
            var ops = context.screen().session().registryAccess().createSerializationContext(NbtOps.INSTANCE);
            return ItemStackTemplate.CODEC
                    .encodeStart(ops, ItemStackTemplate.fromNonEmptyStack(stack))
                    .result()
                    .map(Tag::toString)
                    .orElse("");
        } catch (RuntimeException ignored) {
            return "";
        }
    }

    private static ItemStack editableItem(SpecialDataPanelContext context, String id, String count, String template) {
        Identifier key = Identifier.tryParse(id);
        if (key == null || !BuiltInRegistries.ITEM.containsKey(key)) return ItemStack.EMPTY;
        ItemStack stack = template == null || template.isBlank()
                ? new ItemStack(BuiltInRegistries.ITEM.getValue(key))
                : decodeItemStackTemplate(context, template);
        return stack.isEmpty()
                ? ItemStack.EMPTY
                : stack.copyWithCount(Math.max(1, ValidationUtil.parseIntOrDefault(count, 1)));
    }

    private static ItemStack decodeItemStackTemplate(SpecialDataPanelContext context, String raw) {
        if (raw == null || raw.isBlank()) {
            return ItemStack.EMPTY;
        }
        try {
            var ops = context.screen().session().registryAccess().createSerializationContext(NbtOps.INSTANCE);
            Tag parsedTag = TagParser.create(ops).parseFully(raw);
            return ItemStackTemplate.CODEC
                    .parse(ops, parsedTag)
                    .result()
                    .map(ItemStackTemplate::create)
                    .orElse(ItemStack.EMPTY);
        } catch (Exception ignored) {
            return ItemStack.EMPTY;
        }
    }

    private static String encodeLockPredicate(
            SpecialDataPanelContext context, ItemStack stack, ItemEditorState.SpecialData special) {
        if (stack == null || stack.isEmpty()) {
            return "";
        }
        DataComponentMatchers componentMatchers = lockComponentMatchers(stack, special);
        MinMaxBounds.Ints count = special.lockMatchCount
                ? MinMaxBounds.Ints.exactly(Math.max(1, stack.getCount()))
                : MinMaxBounds.Ints.ANY;
        if (MinMaxBounds.Ints.ANY.equals(count) && DataComponentMatchers.ANY.equals(componentMatchers)) {
            return "";
        }
        ItemPredicate predicate =
                new ItemPredicate(Optional.of(HolderSet.direct(stack.typeHolder())), count, componentMatchers);
        try {
            var ops = context.screen().session().registryAccess().createSerializationContext(NbtOps.INSTANCE);
            return ItemPredicate.CODEC
                    .encodeStart(ops, predicate)
                    .result()
                    .map(Tag::toString)
                    .orElse("");
        } catch (RuntimeException ignored) {
            return "";
        }
    }

    private static DataComponentMatchers lockComponentMatchers(ItemStack stack, ItemEditorState.SpecialData special) {
        if (special.lockMatchAllComponents) {
            return new DataComponentMatchers(DataComponentExactPredicate.allOf(stack.getComponents()), Map.of());
        }

        DataComponentExactPredicate.Builder builder = DataComponentExactPredicate.builder();
        boolean matched = false;
        if (special.lockMatchName) {
            matched |= expectComponent(builder, stack, DataComponents.CUSTOM_NAME);
            matched |= expectComponent(builder, stack, DataComponents.ITEM_NAME);
        }
        if (special.lockMatchLore) {
            matched |= expectComponent(builder, stack, DataComponents.LORE);
        }
        if (special.lockMatchEnchantments) {
            matched |= expectComponent(builder, stack, DataComponents.ENCHANTMENTS);
            matched |= expectComponent(builder, stack, DataComponents.STORED_ENCHANTMENTS);
        }
        if (special.lockMatchCustomData) {
            matched |= expectComponent(builder, stack, DataComponents.CUSTOM_DATA);
        }
        return matched ? new DataComponentMatchers(builder.build(), Map.of()) : DataComponentMatchers.ANY;
    }

    private static <T> boolean expectComponent(
            DataComponentExactPredicate.Builder builder, ItemStack stack, DataComponentType<T> componentType) {
        T value = stack.get(componentType);
        if (value == null) {
            return false;
        }
        builder.expect(componentType, value);
        return true;
    }

    private static boolean usesStackedPickerRows() {
        int requiredInlineWidth = Math.max(
                PICKER_ROW_INLINE_MIN_WIDTH,
                UiFactory.scaledPixels(PICKER_ROW_INPUT_MIN_WIDTH)
                        + Width.FIXED_PICK.pixels()
                        + UiFactory.scaledPixels(SECTION_ROW_GAP));
        return guiWidth() < requiredInlineWidth;
    }

    private static FlowLayout buildOnConsumeEffectsEditor(
            SpecialDataPanelContext context, ItemEditorState.SpecialData special) {
        return buildConsumeEffectsEditor(
                context,
                special.consumableOnConsumeEffects,
                Control.CONSUMABLE_ON_CONSUME_EFFECTS.key(),
                Control.CONSUMABLE_ADD_EFFECT.key(),
                "special.advanced.consumable.effects_empty",
                Control.CONSUMABLE_EFFECT.key());
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
            String effectId, int duration, int amplifier) {
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

    static ItemEditorState.BlocksAttacksDamageReductionDraft expandedBlocksAttacksDamageReductionDraft() {
        ItemEditorState.BlocksAttacksDamageReductionDraft draft =
                new ItemEditorState.BlocksAttacksDamageReductionDraft();
        draft.uiCollapsed = false;
        return draft;
    }

    static ItemEditorState.ToolRuleDraft expandedToolRuleDraft() {
        ItemEditorState.ToolRuleDraft draft = new ItemEditorState.ToolRuleDraft();
        draft.uiCollapsed = false;
        return draft;
    }

    private static FlowLayout buildDeathProtectionEffectsEditor(
            SpecialDataPanelContext context, ItemEditorState.SpecialData special) {
        return buildConsumeEffectsEditor(
                context,
                special.deathProtectionEffects,
                Control.COMPONENT_TWEAKS_DEATH_EFFECTS.key(),
                Control.COMPONENT_TWEAKS_ADD_DEATH_EFFECT.key(),
                "special.advanced.component_tweaks.death_effects_empty",
                Control.COMPONENT_TWEAKS_DEATH_EFFECT.key());
    }

    private static FlowLayout buildConsumeEffectsEditor(
            SpecialDataPanelContext context,
            List<ItemEditorState.ConsumableEffectDraft> drafts,
            String titleKey,
            String addKey,
            String emptyKey,
            String effectTitleKey) {
        FlowLayout card = UiFactory.subCard();
        card.child(UiFactory.title(ItemEditorText.tr(titleKey)).shadow(false));

        ButtonComponent addButton = UiFactory.button(
                ItemEditorText.tr(addKey),
                UiFactory.ButtonTextPreset.STANDARD,
                button -> context.mutateRefresh(() -> drafts.add(expandedConsumableEffectDraft())));
        addButton.horizontalSizing(Sizing.fill(100));
        card.child(addButton);

        if (drafts.isEmpty()) {
            card.child(UiFactory.muted(ItemEditorText.tr(emptyKey), CONSUMABLE_EFFECTS_EMPTY_HINT_WIDTH));
            return card;
        }

        List<String> effectTypeValues = ItemEditorState.ConsumableEffectDraft.EDITABLE_TYPES;
        List<String> effectIds = context.optionalRegistryIds(Registries.MOB_EFFECT);
        List<String> sounds = context.optionalRegistryIds(Registries.SOUND_EVENT);

        for (int index = 0; index < drafts.size(); index++) {
            int currentIndex = index;
            ItemEditorState.ConsumableEffectDraft draft = drafts.get(index);
            String currentType = consumeEffectType(draft);

            FlowLayout effectCard = draft.uiCollapsed || !context.screen().isSelectedEntry(drafts, draft)
                    ? UiFactory.subCard()
                    : context.createReorderableCard(
                            ItemEditorText.tr(effectTitleKey, index + 1),
                            currentIndex > 0,
                            () -> context.swapEntries(drafts, currentIndex, currentIndex - 1),
                            currentIndex < drafts.size() - 1,
                            () -> context.swapEntries(drafts, currentIndex, currentIndex + 1),
                            () -> drafts.remove(currentIndex));
            String effectScope = ComponentSearchField.scope(
                    drafts == context.special().deathProtectionEffects ? "death" : "consume", index);
            effectCard.id(effectScope);
            FlowLayout collapseRow = responsiveRow();
            UIComponent summary = EditorSectionSummary.label(
                    ItemEditorText.tr(effectTitleKey, index + 1)
                            .copy()
                            .append(" | ")
                            .append(consumableEffectSummary(draft, currentType)),
                    Math.max(1, guiWidth() - Width.ICON.pixels() - 48));
            summary.horizontalSizing(Sizing.expand(100));
            collapseRow.child(summary);
            ButtonComponent collapseToggle = UiFactory.collapseToggleButton(
                    draft.uiCollapsed,
                    () -> context.mutateRefresh(() -> {
                        context.screen().selectEntry(drafts, draft);
                        draft.uiCollapsed = !draft.uiCollapsed;
                    }));
            collapseRow.child(collapseToggle);
            effectCard.child(collapseRow);

            if (draft.uiCollapsed) {
                card.child(effectCard);
                continue;
            }

            effectCard.child(PickerFieldFactory.dropdownField(
                    context,
                    Control.CONSUMABLE_EFFECT_TYPE.label(),
                    Component.empty(),
                    Component.literal(effectTypeLabel(currentType)),
                    240,
                    effectTypeValues,
                    AdvancedItemSpecialDataSection::effectTypeLabel,
                    selectedType -> context.mutateRefresh(() -> draft.type = selectedType)));

            switch (currentType) {
                case ItemEditorState.ConsumableEffectDraft.TYPE_REMOVE_EFFECTS -> {
                    effectCard.child(UiFactory.field(
                            Control.CONSUMABLE_REMOVE_EFFECTS.label(),
                            Component.empty(),
                            filledTextBox(context, draft.removedEffects, value -> draft.removedEffects = value)));
                    effectCard.child(holderSetPickerButtons(
                            context,
                            value -> draft.removedEffects = value,
                            () -> draft.removedEffects,
                            Control.POTION_EFFECT_ID.text(),
                            effectIds,
                            context.registryTagIds(Registries.MOB_EFFECT, "")));
                    card.child(effectCard);
                    continue;
                }
                case ItemEditorState.ConsumableEffectDraft.TYPE_PLAY_SOUND -> {
                    effectCard.child(UiFactory.field(
                            Control.CONSUMABLE_EFFECT_SOUND.label(),
                            Component.empty(),
                            textWithPickerCompact(
                                    context,
                                    draft.soundId,
                                    value -> draft.soundId = value,
                                    sounds,
                                    Control.CONSUMABLE_EFFECT_SOUND.text(),
                                    true)));
                    card.child(effectCard);
                    continue;
                }

                case ItemEditorState.ConsumableEffectDraft.TYPE_TELEPORT_RANDOMLY -> {
                    effectCard.child(UiFactory.field(
                            Control.CONSUMABLE_DIAMETER.label(),
                            Component.empty(),
                            filledTextBox(context, draft.diameter, value -> draft.diameter = value)));
                    card.child(effectCard);
                    continue;
                }

                case ItemEditorState.ConsumableEffectDraft.TYPE_APPLY_EFFECTS -> {}
                default -> {
                    card.child(effectCard);
                    continue;
                }
            }

            effectCard.child(UiFactory.field(
                    Control.CONSUMABLE_EFFECT_PROBABILITY.label(),
                    Component.empty(),
                    filledTextBox(context, draft.probability, value -> draft.probability = value)));

            ButtonComponent addPotionEffect = UiFactory.button(
                    Control.POTION_ADD_EFFECT.label(),
                    UiFactory.ButtonTextPreset.STANDARD,
                    button -> context.mutateRefresh(() -> draft.effects.add(new ItemEditorState.PotionEffectDraft())));
            addPotionEffect.horizontalSizing(Sizing.fill(100));
            effectCard.child(addPotionEffect);

            if (draft.effects.isEmpty()) {
                effectCard.child(UiFactory.muted(
                        ItemEditorText.tr("special.advanced.consumable.apply_effects_empty"),
                        CONSUMABLE_APPLY_EFFECTS_EMPTY_HINT_WIDTH));
                card.child(effectCard);
                continue;
            }

            for (int effectIndex = 0; effectIndex < draft.effects.size(); effectIndex++) {
                int currentEffectIndex = effectIndex;
                ItemEditorState.PotionEffectDraft effectDraft = draft.effects.get(effectIndex);

                FlowLayout potionCard = context.createRemovableCard(
                        Control.POTION_EFFECT.label(effectIndex + 1), () -> draft.effects.remove(currentEffectIndex));
                potionCard.id(effectScope + "-potion-" + effectIndex);
                FlowLayout inputs = responsiveRow();
                UIComponent effectField = PickerFieldFactory.searchableField(
                        context,
                        Control.POTION_EFFECT_ID.label(),
                        Component.empty(),
                        PickerFieldFactory.selectedOrFallback(
                                effectDraft.effectId, ItemEditorText.tr("special.potion.select_effect")),
                        220,
                        Control.POTION_EFFECT_ID.text(),
                        "",
                        effectIds,
                        id -> id,
                        id -> context.mutateRefresh(() -> effectDraft.effectId = id));
                UIComponent durationField = EffectFieldLayoutUtil.numberField(
                        Control.POTION_DURATION.label(),
                        effectDraft.duration,
                        context.bindText(value -> effectDraft.duration = value));
                UIComponent amplifierField = EffectFieldLayoutUtil.numberField(
                        Control.POTION_AMPLIFIER.label(),
                        effectDraft.amplifier,
                        context.bindText(value -> effectDraft.amplifier = value));
                distributeRowChildren(inputs, effectField, durationField, amplifierField);
                potionCard.child(inputs);

                UIComponent ambientToggle = UiFactory.checkbox(
                        Control.POTION_AMBIENT.label(),
                        effectDraft.ambient,
                        context.bindToggle(value -> effectDraft.ambient = value));
                UIComponent visibleToggle = SpecialDataFieldFactory.effectVisibilityField(
                        context,
                        Control.POTION_VISIBLE.label(),
                        effectDraft.visible,
                        effectDraft.originalVisible,
                        value -> effectDraft.visible = value);
                UIComponent iconToggle = SpecialDataFieldFactory.effectVisibilityField(
                        context,
                        Control.POTION_SHOW_ICON.label(),
                        effectDraft.showIcon,
                        effectDraft.originalShowIcon,
                        value -> effectDraft.showIcon = value);
                potionCard.child(denseEquipmentRow(ambientToggle, visibleToggle, iconToggle));
                effectCard.child(potionCard);
            }

            card.child(effectCard);
        }

        return card;
    }

    private static Component consumableEffectSummary(ItemEditorState.ConsumableEffectDraft draft, String currentType) {
        if (Objects.equals(currentType, ItemEditorState.ConsumableEffectDraft.TYPE_REMOVE_EFFECTS)) {
            return Component.literal(effectTypeLabel(currentType))
                    .withColor(0x55FFFF)
                    .append(EditorSectionSummary.separator())
                    .append(EditorSectionSummary.value(valueOrDefault(draft.removedEffects, "-")));
        }
        if (Objects.equals(currentType, ItemEditorState.ConsumableEffectDraft.TYPE_TELEPORT_RANDOMLY)) {
            return Component.literal(effectTypeLabel(currentType))
                    .withColor(0x55FFFF)
                    .append(EditorSectionSummary.separator())
                    .append(EditorSectionSummary.value(valueOrDefault(draft.diameter, "16")));
        }
        if (Objects.equals(currentType, ItemEditorState.ConsumableEffectDraft.TYPE_CLEAR_ALL_EFFECTS)) {
            return Component.literal(effectTypeLabel(currentType)).withColor(0x55FFFF);
        }
        if (Objects.equals(currentType, ItemEditorState.ConsumableEffectDraft.TYPE_PLAY_SOUND)) {
            String sound = valueOrDefault(draft.soundId, "-");
            return Component.literal(effectTypeLabel(currentType))
                    .withColor(0x55FFFF)
                    .append(EditorSectionSummary.separator())
                    .append(EditorSectionSummary.value(sound));
        }
        int effectCount = draft.effects.size();
        String probability = draft.probability.isBlank() ? "1.0" : draft.probability;
        return ItemEditorText.tr(
                        "special.advanced.consumable.effect_summary",
                        Component.literal(effectTypeLabel(currentType)).withColor(0x55FFFF),
                        EditorSectionSummary.value(Integer.toString(effectCount)),
                        EditorSectionSummary.value(probability))
                .copy()
                .withColor(0xAAAAAA);
    }

    private static String beeSummary(ItemEditorState.BeeOccupantDraft draft) {
        String entity = valueOrDefault(draft.entityId, "minecraft:bee");
        String ticks = valueOrDefault(draft.ticksInHive, "0");
        String minTicks = valueOrDefault(draft.minTicksInHive, "0");
        return ItemEditorText.str("special.advanced.container_meta.bee_summary", entity, ticks, minTicks);
    }

    private static String projectileSummary(ItemEditorState.ChargedProjectileDraft draft) {
        String item = valueOrDefault(draft.itemId, "-");
        String count = valueOrDefault(draft.count, "1");
        String summary = item + " x" + count;
        return draft.templateSnbt == null || draft.templateSnbt.isBlank()
                ? summary
                : summary + " | " + ItemEditorText.str("special.advanced.crossbow.full_item_data");
    }

    private static String mapDecorationSummary(ItemEditorState.MapDecorationDraft draft) {
        String key = valueOrDefault(draft.key, "-");
        String type = valueOrDefault(draft.typeId, "-");
        String x = valueOrDefault(draft.x, "0");
        String z = valueOrDefault(draft.z, "0");
        return key + " - " + type + " (" + x + ", " + z + ")";
    }

    static String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static int adjustNumericString(String raw, int delta) {
        int value = ValidationUtil.parseIntOrDefault(raw, 1);
        value += delta;
        return Math.max(1, value);
    }

    private static String effectTypeLabel(String effectTypeId) {
        return switch (effectTypeId) {
            case ItemEditorState.ConsumableEffectDraft.TYPE_TELEPORT_RANDOMLY ->
                ItemEditorText.str("special.advanced.consumable.effect_type.teleport_randomly");
            case ItemEditorState.ConsumableEffectDraft.TYPE_CLEAR_ALL_EFFECTS ->
                ItemEditorText.str("special.advanced.consumable.effect_type.clear_all_effects");
            case ItemEditorState.ConsumableEffectDraft.TYPE_PLAY_SOUND ->
                ItemEditorText.str("special.advanced.consumable.effect_type.play_sound");
            case ItemEditorState.ConsumableEffectDraft.TYPE_APPLY_EFFECTS ->
                ItemEditorText.str("special.advanced.consumable.effect_type.apply_effects");
            case null, default -> effectTypeId;
        };
    }

    private static void configureLockKeyFromStack(
            SpecialDataPanelContext context, ItemEditorState.SpecialData special, ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        special.lockItemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        special.lockKeyTemplateSnbt = encodeItemStackTemplate(context, stack);
        special.lockMatchCount = true;
        special.lockMatchName = true;
        special.lockMatchLore = true;
        special.lockMatchEnchantments = true;
        special.lockMatchCustomData = true;
        special.lockMatchAllComponents = true;
        refreshLockPredicateFromSource(context, special);
    }

    private static void resetLockPredicateConfig(ItemEditorState.SpecialData special) {
        special.lockPredicateSnbt = "";
        special.lockKeyTemplateSnbt = "";
        special.lockMatchCount = false;
        special.lockMatchName = false;
        special.lockMatchLore = false;
        special.lockMatchEnchantments = false;
        special.lockMatchCustomData = false;
        special.lockMatchAllComponents = false;
    }

    private static void refreshLockPredicateFromSource(
            SpecialDataPanelContext context, ItemEditorState.SpecialData special) {
        ItemStack keyStack = decodeItemStackTemplate(context, special.lockKeyTemplateSnbt);
        if (keyStack.isEmpty()) {
            special.lockPredicateSnbt = "";
            return;
        }
        if (special.lockItemId == null || special.lockItemId.isBlank()) {
            special.lockItemId =
                    BuiltInRegistries.ITEM.getKey(keyStack.getItem()).toString();
        }
        special.lockPredicateSnbt = encodeLockPredicate(context, keyStack, special);
    }

    private static FlowLayout buildLockMatchOptions(
            SpecialDataPanelContext context, ItemEditorState.SpecialData special) {
        FlowLayout card = UiFactory.subCard();
        card.child(
                UiFactory.title(Control.CONTAINER_META_LOCK_MATCH_TITLE.label()).shadow(false));
        card.child(UiFactory.muted(
                ItemEditorText.tr("special.advanced.container_meta.lock_match_hint"), CONTAINER_META_LOCK_HINT_WIDTH));

        FlowLayout rows = UiFactory.column();
        rows.gap(2);
        rows.child(lockMatchRow(
                lockMatchCheckbox(
                        context,
                        special,
                        Control.CONTAINER_META_LOCK_MATCH_COUNT.key(),
                        special.lockMatchCount,
                        value -> special.lockMatchCount = value),
                lockMatchCheckbox(
                        context,
                        special,
                        Control.CONTAINER_META_LOCK_MATCH_NAME.key(),
                        special.lockMatchName,
                        value -> special.lockMatchName = value),
                lockMatchCheckbox(
                        context,
                        special,
                        Control.CONTAINER_META_LOCK_MATCH_LORE.key(),
                        special.lockMatchLore,
                        value -> special.lockMatchLore = value)));
        rows.child(lockMatchRow(
                lockMatchCheckbox(
                        context,
                        special,
                        Control.CONTAINER_META_LOCK_MATCH_ENCHANTMENTS.key(),
                        special.lockMatchEnchantments,
                        value -> special.lockMatchEnchantments = value),
                lockMatchCheckbox(
                        context,
                        special,
                        Control.CONTAINER_META_LOCK_MATCH_CUSTOM_DATA.key(),
                        special.lockMatchCustomData,
                        value -> special.lockMatchCustomData = value),
                lockMatchCheckbox(
                        context,
                        special,
                        Control.CONTAINER_META_LOCK_MATCH_ALL_COMPONENTS.key(),
                        special.lockMatchAllComponents,
                        value -> special.lockMatchAllComponents = value)));
        card.child(rows);

        ButtonComponent fullItem = UiFactory.actionRowButton(
                Control.CONTAINER_META_LOCK_MATCH_FULL_ITEM.label(),
                UiFactory.ButtonTextPreset.STANDARD,
                UiFactory.ActionTone.PICKER,
                button -> context.mutateRefresh(() -> {
                    special.lockMatchCount = true;
                    special.lockMatchName = true;
                    special.lockMatchLore = true;
                    special.lockMatchEnchantments = true;
                    special.lockMatchCustomData = true;
                    special.lockMatchAllComponents = true;
                    refreshLockPredicateFromSource(context, special);
                }));
        ButtonComponent simple = UiFactory.negativeButton(
                Control.CONTAINER_META_LOCK_RESET_SIMPLE.label(),
                UiFactory.ButtonTextPreset.STANDARD,
                button -> context.mutateRefresh(() -> resetLockPredicateConfig(special)));
        card.child(UiFactory.actionButtonRow(fullItem, simple));
        return card;
    }

    private static UIComponent lockMatchCheckbox(
            SpecialDataPanelContext context,
            ItemEditorState.SpecialData special,
            String labelKey,
            boolean checked,
            Consumer<Boolean> setter) {
        return UiFactory.checkbox(
                ItemEditorText.tr(labelKey),
                checked,
                value -> context.mutateRefresh(() -> {
                    setter.accept(value);
                    refreshLockPredicateFromSource(context, special);
                }));
    }

    private static FlowLayout lockMatchRow(UIComponent first, UIComponent second, UIComponent third) {
        return new CompactFieldLayout(List.of(first, second, third), UiFactory.scaledPixels(210));
    }

    private static Component lockSummary(ItemEditorState.SpecialData special) {
        String itemId = special.lockItemId == null ? "" : special.lockItemId.trim();
        String predicate = special.lockPredicateSnbt == null ? "" : special.lockPredicateSnbt.trim();
        if (itemId.isBlank() && predicate.isBlank()) {
            return ItemEditorText.tr("special.advanced.container_meta.lock_summary_none");
        }

        List<String> parts = new ArrayList<>();
        parts.add(itemId.isBlank() ? "-" : itemId);
        if (predicate.isBlank()) {
            parts.add(ItemEditorText.str("special.advanced.container_meta.lock_summary_simple"));
        } else if (special.lockKeyTemplateSnbt == null || special.lockKeyTemplateSnbt.isBlank()) {
            parts.add(ItemEditorText.str("special.advanced.container_meta.lock_summary_advanced"));
        } else {
            if (special.lockMatchCount) {
                parts.add(Control.COMMON_COUNT.text());
            }
            if (special.lockMatchAllComponents) {
                parts.add(ItemEditorText.str("special.advanced.container_meta.lock_summary_all_components"));
            } else {
                addLockSummaryPart(parts, special.lockMatchName, "special.advanced.container_meta.lock_summary_name");
                addLockSummaryPart(parts, special.lockMatchLore, "special.advanced.container_meta.lock_summary_lore");
                addLockSummaryPart(
                        parts,
                        special.lockMatchEnchantments,
                        "special.advanced.container_meta.lock_summary_enchantments");
                addLockSummaryPart(
                        parts, special.lockMatchCustomData, "special.advanced.container_meta.lock_summary_custom_data");
            }
        }
        return Component.literal(String.join(" | ", parts));
    }

    private static void addLockSummaryPart(List<String> parts, boolean enabled, String labelKey) {
        if (enabled) {
            parts.add(ItemEditorText.str(labelKey));
        }
    }

    public static FlowLayout buildContainerMetadata(SpecialDataPanelContext context) {
        ItemEditorState.SpecialData special = context.special();
        List<String> availableItems = context.itemIdsWithoutAir();
        return collapsibleCard(
                context,
                Control.CONTAINER_META_TITLE.label(),
                special.uiContainerMetadataCollapsed,
                value -> special.uiContainerMetadataCollapsed = value,
                () -> {
                    FlowLayout content = UiFactory.column();
                    int idWidth = Width.ID.pixels();
                    int lockPredicateWidth = Width.LONG.pixels();
                    int seedWidth = Width.NUMERIC.pixels();
                    List<String> lootTables = LootTableIds.fromResources(
                            context.screen().session().minecraft().getResourceManager());

                    content.child(compactField(
                            Control.CONTAINER_META_LOCK_ITEM.label(),
                            itemIdInputWithStoragePick(
                                    context,
                                    special.lockItemId,
                                    value -> {
                                        special.lockItemId = value;
                                        resetLockPredicateConfig(special);
                                    },
                                    stack -> configureLockKeyFromStack(context, special, stack),
                                    Control.CONTAINER_META_LOCK_ITEM.text(),
                                    () -> editableItem(context, special.lockItemId, "1", special.lockKeyTemplateSnbt)),
                            idWidth + 170));
                    content.child(UiFactory.muted(lockSummary(special), CONTAINER_META_LOCK_HINT_WIDTH));
                    if (!special.lockKeyTemplateSnbt.isBlank()) {
                        content.child(buildLockMatchOptions(context, special));
                    }
                    content.child(context.boundIdField(Control.CONTAINER_META_LOOT_TABLE.key(), lootTables, idWidth));

                    content.child(compactTextField(
                            context,
                            Control.CONTAINER_META_LOCK_PREDICATE.label(),
                            special.lockPredicateSnbt,
                            value -> {
                                special.lockPredicateSnbt = value;
                                special.lockKeyTemplateSnbt = "";
                            },
                            lockPredicateWidth));
                    content.child(UiFactory.muted(
                            ItemEditorText.tr("special.advanced.container_meta.lock_predicate_hint"),
                            CONTAINER_META_LOCK_HINT_WIDTH));

                    content.child(context.boundTextField(Control.CONTAINER_META_LOOT_SEED.key(), seedWidth));

                    content.child(buildBeesEditor(context, special));

                    content.child(buildPotDecorations(context, availableItems));
                    return content;
                });
    }

    private static FlowLayout buildBeesEditor(SpecialDataPanelContext context, ItemEditorState.SpecialData special) {
        FlowLayout card = UiFactory.subCard();
        card.id("component-list-bees");
        card.child(UiFactory.title(Control.CONTAINER_META_BEES_TITLE.label()).shadow(false));

        ButtonComponent addButton = UiFactory.button(
                Control.CONTAINER_META_BEES_ADD.label(),
                UiFactory.ButtonTextPreset.STANDARD,
                button -> context.mutateRefresh(() -> {
                    ItemEditorState.BeeOccupantDraft draft = new ItemEditorState.BeeOccupantDraft();
                    draft.uiCollapsed = false;
                    special.beesOccupants.add(draft);
                }));
        ButtonComponent clearAll = UiFactory.button(
                Control.COMMON_CLEAR_ALL.label(),
                UiFactory.ButtonTextPreset.STANDARD,
                button -> context.mutateRefresh(special.beesOccupants::clear));
        clearAll.active = !special.beesOccupants.isEmpty();
        card.child(UiFactory.actionButtonRow(addButton, clearAll));
        if (!special.beesOccupants.isEmpty()) {
            card.child(UiFactory.collapseAllButton(
                    special.beesOccupants.stream().anyMatch(entry -> entry.uiCollapsed),
                    collapsed -> context.mutateRefresh(
                            () -> special.beesOccupants.forEach(entry -> entry.uiCollapsed = collapsed))));
        }

        if (special.beesOccupants.isEmpty()) {
            card.child(UiFactory.muted(
                    ItemEditorText.tr("special.advanced.container_meta.bees_empty"),
                    CONTAINER_META_BEES_EMPTY_HINT_WIDTH));
            return card;
        }

        int idWidth = Width.ID.pixels();
        int numberWidth = Width.NUMERIC.pixels();
        List<String> entityTypeIds = context.optionalRegistryIds(Registries.ENTITY_TYPE);
        for (int index = 0; index < special.beesOccupants.size(); index++) {
            int currentIndex = index;
            ItemEditorState.BeeOccupantDraft draft = special.beesOccupants.get(index);
            FlowLayout beeCard = context.createReorderableCard(
                    Control.CONTAINER_META_BEE.label(index + 1),
                    currentIndex > 0,
                    () -> context.swapEntries(special.beesOccupants, currentIndex, currentIndex - 1),
                    currentIndex < special.beesOccupants.size() - 1,
                    () -> context.swapEntries(special.beesOccupants, currentIndex, currentIndex + 1),
                    () -> special.beesOccupants.remove(currentIndex));
            beeCard.id(ComponentSearchField.scope("bee", index));

            FlowLayout summaryRow = responsiveRow();
            UIComponent summary =
                    UiFactory.muted(Component.literal(beeSummary(draft)), CONTAINER_META_BEE_SUMMARY_HINT_WIDTH);
            summary.horizontalSizing(Sizing.expand(100));
            summaryRow.child(summary);
            ButtonComponent collapseToggle = UiFactory.button(
                    Component.literal(draft.uiCollapsed ? SYMBOL_SECTION_COLLAPSED : SYMBOL_SECTION_EXPANDED),
                    UiFactory.ButtonTextPreset.STANDARD,
                    button -> context.mutateRefresh(() -> draft.uiCollapsed = !draft.uiCollapsed));
            collapseToggle.horizontalSizing(Sizing.fixed(Width.ICON.pixels()));
            summaryRow.child(collapseToggle);
            beeCard.child(summaryRow);

            if (!draft.uiCollapsed) {
                beeCard.child(compactIdField(
                        context,
                        Control.COMMON_ENTITY_ID.label(),
                        draft.entityId,
                        value -> draft.entityId = value,
                        entityTypeIds,
                        Control.COMMON_ENTITY_ID.text(),
                        idWidth));

                FlowLayout ticksField = compactTextField(
                        context,
                        Control.CONTAINER_META_BEES_TICKS.label(),
                        draft.ticksInHive,
                        value -> draft.ticksInHive = value,
                        numberWidth);
                FlowLayout minTicksField = compactTextField(
                        context,
                        Control.CONTAINER_META_BEES_MIN_TICKS.label(),
                        draft.minTicksInHive,
                        value -> draft.minTicksInHive = value,
                        numberWidth);
                beeCard.child(denseEquipmentRow(ticksField, minTicksField));
            }
            card.child(beeCard);
        }
        return card;
    }

    public static FlowLayout buildCrossbow(SpecialDataPanelContext context) {
        ItemEditorState.SpecialData special = context.special();
        return collapsibleCard(
                context,
                Control.CROSSBOW_TITLE.label(),
                special.uiCrossbowCollapsed,
                value -> special.uiCrossbowCollapsed = value,
                () -> {
                    FlowLayout content = UiFactory.column();
                    int countWidth = Width.TINY.pixels();

                    ButtonComponent addButton = UiFactory.button(
                            Control.CROSSBOW_ADD_PROJECTILE.label(),
                            UiFactory.ButtonTextPreset.STANDARD,
                            button -> context.mutateRefresh(() -> {
                                ItemEditorState.ChargedProjectileDraft draft =
                                        new ItemEditorState.ChargedProjectileDraft();
                                draft.uiCollapsed = false;
                                special.chargedProjectiles.add(draft);
                            }));
                    addButton.horizontalSizing(Sizing.fill(100));
                    if (!special.chargedProjectiles.isEmpty()) {
                        ButtonComponent clearAll = UiFactory.button(
                                Control.COMMON_CLEAR_ALL.label(),
                                UiFactory.ButtonTextPreset.STANDARD,
                                button -> context.mutateRefresh(special.chargedProjectiles::clear));
                        content.child(UiFactory.actionButtonRow(addButton, clearAll));

                        content.child(UiFactory.collapseAllButton(
                                special.chargedProjectiles.stream().anyMatch(entry -> entry.uiCollapsed),
                                collapsed -> context.mutateRefresh(() ->
                                        special.chargedProjectiles.forEach(entry -> entry.uiCollapsed = collapsed))));
                    } else {
                        content.child(addButton);
                    }

                    if (special.chargedProjectiles.isEmpty()) {
                        content.child(UiFactory.muted(
                                ItemEditorText.tr("special.advanced.crossbow.empty"), CROSSBOW_EMPTY_HINT_WIDTH));
                        return content;
                    }

                    for (int index = 0; index < special.chargedProjectiles.size(); index++) {
                        int currentIndex = index;
                        ItemEditorState.ChargedProjectileDraft draft = special.chargedProjectiles.get(index);
                        FlowLayout card = UiFactory.reorderableCollapsibleSubCard(
                                Control.CROSSBOW_PROJECTILE.label(index + 1),
                                Component.literal(projectileSummary(draft)),
                                CROSSBOW_PROJECTILE_SUMMARY_HINT_WIDTH,
                                draft.uiCollapsed,
                                () -> context.mutateRefresh(() -> draft.uiCollapsed = !draft.uiCollapsed),
                                currentIndex > 0,
                                () -> context.mutateRefresh(() -> context.swapEntries(
                                        special.chargedProjectiles, currentIndex, currentIndex - 1)),
                                currentIndex < special.chargedProjectiles.size() - 1,
                                () -> context.mutateRefresh(() -> context.swapEntries(
                                        special.chargedProjectiles, currentIndex, currentIndex + 1)),
                                () -> context.mutateRefresh(() -> special.chargedProjectiles.remove(currentIndex)));
                        card.id(ComponentSearchField.scope("projectile", index));

                        if (!draft.uiCollapsed) {
                            card.child(context.itemRow(
                                    () -> editableItem(context, draft.itemId, draft.count, draft.templateSnbt),
                                    stack -> {
                                        draft.itemId = BuiltInRegistries.ITEM
                                                .getKey(stack.getItem())
                                                .toString();
                                        draft.count = Integer.toString(stack.getCount());
                                        draft.templateSnbt = encodeItemStackTemplate(context, stack);
                                    },
                                    null,
                                    ItemEditorText.tr("special.advanced.crossbow.title")
                                            .copy()
                                            .append(" > " + (index + 1))));

                            FlowLayout countRow = responsiveRow();
                            FlowLayout countField = compactTextField(
                                    context,
                                    Control.COMMON_COUNT.label(),
                                    draft.count,
                                    value -> draft.count = value,
                                    countWidth);
                            ButtonComponent decrement = UiFactory.button(
                                    Component.literal("-"),
                                    UiFactory.ButtonTextPreset.STANDARD,
                                    button -> context.mutateRefresh(() ->
                                            draft.count = Integer.toString(adjustNumericString(draft.count, -1))));
                            decrement.horizontalSizing(Sizing.fixed(Width.ICON.pixels()));
                            ButtonComponent increment = UiFactory.button(
                                    Component.literal("+"),
                                    UiFactory.ButtonTextPreset.STANDARD,
                                    button -> context.mutateRefresh(
                                            () -> draft.count = Integer.toString(adjustNumericString(draft.count, 1))));
                            increment.horizontalSizing(Sizing.fixed(Width.ICON.pixels()));
                            distributeRowChildren(countRow, countField, decrement, increment);
                            card.child(countRow);
                        }
                        content.child(card);
                    }

                    return content;
                });
    }

    public static FlowLayout buildMapAdvanced(SpecialDataPanelContext context) {
        ItemEditorState.SpecialData special = context.special();
        return collapsibleCard(
                context,
                Control.MAP_TITLE.label(),
                special.uiMapAdvancedCollapsed,
                value -> special.uiMapAdvancedCollapsed = value,
                () -> {
                    FlowLayout content = UiFactory.column();
                    content.child(UiFactory.field(
                            Control.MAP_MAP_ID.label(),
                            Component.empty(),
                            context.boundTextBox(Control.MAP_MAP_ID.key()).horizontalSizing(Sizing.fill(100))));
                    content.child(buildMapDecorationsEditor(context, special));
                    content.child(buildLodestoneEditor(context, special));
                    return content;
                });
    }

    private static FlowLayout buildPotDecorations(SpecialDataPanelContext context, List<String> itemIds) {
        FlowLayout card = UiFactory.subCard();
        card.child(UiFactory.title(Control.CONTAINER_META_POT_TITLE.label()).shadow(false));
        int idWidth = Width.ID.pixels();
        boolean narrowLayout = isNarrowLayout();

        List<FlowLayout> fields = List.of(
                context.boundIdField(Control.CONTAINER_META_POT_BACK.key(), itemIds, idWidth),
                context.boundIdField(Control.CONTAINER_META_POT_LEFT.key(), itemIds, idWidth),
                context.boundIdField(Control.CONTAINER_META_POT_RIGHT.key(), itemIds, idWidth),
                context.boundIdField(Control.CONTAINER_META_POT_FRONT.key(), itemIds, idWidth));
        if (narrowLayout) {
            fields.forEach(card::child);
        } else {
            for (int index = 0; index < fields.size(); index += 2) {
                FlowLayout row = responsiveRow();
                distributeRowChildren(row, fields.get(index), fields.get(index + 1));
                card.child(row);
            }
        }
        return card;
    }

    private static FlowLayout buildMapDecorationsEditor(
            SpecialDataPanelContext context, ItemEditorState.SpecialData special) {
        List<String> decorationTypeIds = context.optionalRegistryIds(Registries.MAP_DECORATION_TYPE);
        FlowLayout card = UiFactory.subCard();
        card.id("component-list-decorations");
        card.child(UiFactory.title(Control.MAP_DECORATIONS_TITLE.label()).shadow(false));

        int keyWidth = Width.GROUP.pixels();
        int idWidth = Width.ID.pixels();
        int numberWidth = Width.NUMERIC.pixels();

        ButtonComponent addButton = UiFactory.button(
                Control.MAP_ADD_DECORATION.label(),
                UiFactory.ButtonTextPreset.STANDARD,
                button -> context.mutateRefresh(() -> {
                    ItemEditorState.MapDecorationDraft draft = new ItemEditorState.MapDecorationDraft();
                    draft.uiCollapsed = false;
                    special.mapDecorations.add(draft);
                }));
        ButtonComponent clearAll = UiFactory.button(
                Control.COMMON_CLEAR_ALL.label(),
                UiFactory.ButtonTextPreset.STANDARD,
                button -> context.mutateRefresh(special.mapDecorations::clear));
        clearAll.active = !special.mapDecorations.isEmpty();
        card.child(UiFactory.actionButtonRow(addButton, clearAll));
        if (!special.mapDecorations.isEmpty()) {
            card.child(UiFactory.collapseAllButton(
                    special.mapDecorations.stream().anyMatch(entry -> entry.uiCollapsed),
                    collapsed -> context.mutateRefresh(
                            () -> special.mapDecorations.forEach(entry -> entry.uiCollapsed = collapsed))));
        }

        if (special.mapDecorations.isEmpty()) {
            card.child(UiFactory.muted(
                    ItemEditorText.tr("special.advanced.map.decorations_empty"), MAP_DECORATIONS_EMPTY_HINT_WIDTH));
            return card;
        }

        for (int index = 0; index < special.mapDecorations.size(); index++) {
            int currentIndex = index;
            ItemEditorState.MapDecorationDraft draft = special.mapDecorations.get(index);
            FlowLayout entry = context.createReorderableCard(
                    Control.MAP_DECORATION.label(index + 1),
                    currentIndex > 0,
                    () -> context.swapEntries(special.mapDecorations, currentIndex, currentIndex - 1),
                    currentIndex < special.mapDecorations.size() - 1,
                    () -> context.swapEntries(special.mapDecorations, currentIndex, currentIndex + 1),
                    () -> special.mapDecorations.remove(currentIndex));
            entry.id(ComponentSearchField.scope("decoration", index));

            FlowLayout summaryRow = responsiveRow();
            UIComponent summary =
                    UiFactory.muted(Component.literal(mapDecorationSummary(draft)), MAP_DECORATION_SUMMARY_HINT_WIDTH);
            summary.horizontalSizing(Sizing.expand(100));
            summaryRow.child(summary);
            ButtonComponent collapseToggle = UiFactory.button(
                    Component.literal(draft.uiCollapsed ? SYMBOL_SECTION_COLLAPSED : SYMBOL_SECTION_EXPANDED),
                    UiFactory.ButtonTextPreset.STANDARD,
                    button -> context.mutateRefresh(() -> draft.uiCollapsed = !draft.uiCollapsed));
            collapseToggle.horizontalSizing(Sizing.fixed(Width.ICON.pixels()));
            summaryRow.child(collapseToggle);
            entry.child(summaryRow);

            if (!draft.uiCollapsed) {
                entry.child(compactField(
                        Control.MAP_DECORATION_KEY.label(),
                        filledTextBox(context, draft.key, value -> draft.key = value),
                        keyWidth + 40));
                entry.child(compactIdField(
                        context,
                        Control.MAP_DECORATION_TYPE.label(),
                        draft.typeId,
                        value -> draft.typeId = value,
                        decorationTypeIds,
                        Control.MAP_DECORATION_TYPE.text(),
                        idWidth));

                FlowLayout position = responsiveRow();
                FlowLayout xField = compactTextField(
                        context, Control.MAP_DECORATION_X.label(), draft.x, value -> draft.x = value, numberWidth);
                FlowLayout zField = compactTextField(
                        context, Control.MAP_DECORATION_Z.label(), draft.z, value -> draft.z = value, numberWidth);
                FlowLayout rotationField = compactTextField(
                        context,
                        Control.MAP_DECORATION_ROTATION.label(),
                        draft.rotation,
                        value -> draft.rotation = value,
                        numberWidth);
                distributeRowChildren(position, xField, zField, rotationField);
                entry.child(position);
            }
            card.child(entry);
        }

        return card;
    }

    private static FlowLayout buildLodestoneEditor(
            SpecialDataPanelContext context, ItemEditorState.SpecialData special) {
        FlowLayout card = UiFactory.subCard();
        card.child(UiFactory.title(Control.MAP_LODESTONE_TITLE.label()).shadow(false));
        card.child(UiFactory.checkbox(
                Control.MAP_LODESTONE_ENABLED.label(),
                special.lodestoneEnabled,
                value -> context.mutateRefresh(() -> special.lodestoneEnabled = value)));
        if (!special.lodestoneEnabled) {
            return card;
        }

        card.child(UiFactory.checkbox(
                Control.MAP_LODESTONE_TRACKED.label(),
                special.lodestoneTracked,
                context.bindToggle(value -> special.lodestoneTracked = value)));
        card.child(UiFactory.field(
                Control.MAP_LODESTONE_DIMENSION.label(),
                Component.empty(),
                textWithPickerCompact(
                        context,
                        special.lodestoneDimensionId,
                        value -> special.lodestoneDimensionId = value,
                        context.optionalRegistryIds(Registries.DIMENSION),
                        Control.MAP_LODESTONE_DIMENSION.text(),
                        true)));

        int numberWidth = Width.NUMERIC.pixels();
        FlowLayout xField = context.boundTextField(Control.MAP_LODESTONE_X.key(), numberWidth);
        FlowLayout yField = context.boundTextField(Control.MAP_LODESTONE_Y.key(), numberWidth);
        FlowLayout zField = context.boundTextField(Control.MAP_LODESTONE_Z.key(), numberWidth);
        card.child(denseEquipmentRow(xField, yField, zField));
        return card;
    }

    private static FlowLayout buildComponentTweakNamingSection(
            SpecialDataPanelContext context, ItemEditorState.SpecialData special) {
        return collapsibleCard(
                context,
                Control.COMPONENT_TWEAKS_NAMING_TITLE.label(),
                special.uiComponentTweaksNamingCollapsed,
                value -> special.uiComponentTweaksNamingCollapsed = value,
                () -> UiFactory.column().child(buildNamingAndStackCard(context, special)));
    }

    private static FlowLayout buildBlockState(SpecialDataPanelContext context, ItemEditorState.SpecialData special) {
        return collapsibleCard(
                context,
                Control.BLOCK_STATE_TITLE.label(),
                special.uiBlockStateCollapsed,
                value -> special.uiBlockStateCollapsed = value,
                () -> UiFactory.column().child(buildBlockStateCard(context, special)));
    }

    private static FlowLayout buildNamingAndStackCard(
            SpecialDataPanelContext context, ItemEditorState.SpecialData special) {
        FlowLayout card = UiFactory.subCard();
        int numericWidth = Width.NUMERIC.pixels();
        int longWidth = Width.LONG.pixels();

        Component inheritedName = context.originalStack().getItem().components().get(DataComponents.ITEM_NAME);
        String defaultName = inheritedName == null ? null : TextComponentUtil.toMarkup(inheritedName);
        var defaultHint = UiFactory.muted(
                special.itemName.equals(defaultName) ? ItemEditorText.tr("screen.field.default") : Component.empty());
        var nameInput = context.boundTextBox(Control.COMPONENT_TWEAKS_ITEM_NAME.key());
        nameInput
                .onChanged()
                .subscribe(value -> defaultHint.text(
                        value.equals(defaultName) ? ItemEditorText.tr("screen.field.default") : Component.empty()));
        card.child(compactField(Control.COMPONENT_TWEAKS_ITEM_NAME.label(), nameInput, longWidth + 40));
        card.child(defaultHint);

        FlowLayout minAttackField =
                context.boundTextField(Control.COMPONENT_TWEAKS_MIN_ATTACK_CHARGE.key(), numericWidth);
        FlowLayout enchantableField = context.boundTextField(Control.COMPONENT_TWEAKS_ENCHANTABLE.key(), numericWidth);
        FlowLayout ominousField =
                context.boundTextField(Control.COMPONENT_TWEAKS_OMINOUS_AMPLIFIER.key(), numericWidth);

        card.child(denseEquipmentRow(minAttackField, enchantableField, ominousField));

        card.child(PickerFieldFactory.searchableTextField(
                context,
                Control.COMPONENT_TWEAKS_TOOLTIP_STYLE.label(),
                special.tooltipStyleId,
                value -> special.tooltipStyleId = value,
                Width.PICKER.pixels(),
                Control.COMPONENT_TWEAKS_TOOLTIP_STYLE.text(),
                "",
                withCurrentId(context.tooltipStyleIds(), special.tooltipStyleId),
                id -> id,
                id -> context.mutateRefresh(() -> special.tooltipStyleId = id)));

        UIComponent gliderToggle = UiFactory.checkbox(
                Control.COMPONENT_TWEAKS_GLIDER.label(),
                special.glider,
                context.bindToggle(value -> special.glider = value));
        UIComponent intangibleToggle = UiFactory.checkbox(
                Control.COMPONENT_TWEAKS_INTANGIBLE_PROJECTILE.label(),
                special.intangibleProjectile,
                context.bindToggle(value -> special.intangibleProjectile = value));
        UIComponent deathProtectionToggle = UiFactory.checkbox(
                Control.COMPONENT_TWEAKS_DEATH_PROTECTION.label(),
                special.deathProtection,
                value -> context.mutateRefresh(() -> setDeathProtectionEnabled(special, value)));

        card.child(compactCheckboxRow(gliderToggle, intangibleToggle, deathProtectionToggle));
        if (special.deathProtection) {
            card.child(buildDeathProtectionEffectsEditor(context, special));
        }
        return card;
    }

    private static FlowLayout buildBlockStateCard(
            SpecialDataPanelContext context, ItemEditorState.SpecialData special) {
        FlowLayout card = UiFactory.subCard();
        List<BlockStatePropertyMeta> availableProperties = blockStatePropertyMeta(context);

        card.child(UiFactory.title(Control.COMPONENT_TWEAKS_BLOCK_STATE.label()).shadow(false));

        Map<String, String> currentValues = parseBlockStatePropertyMap(special.blockStateProperties);
        if (!currentValues.isEmpty()) {
            FlowLayout stateActions = UiFactory.row();
            stateActions.horizontalAlignment(HorizontalAlignment.RIGHT);
            stateActions.gap(Math.max(1, UiFactory.scaleProfile().tightSpacing()));
            ButtonComponent clearProperties = UiFactory.button(
                    Control.COMMON_RESET.label(),
                    UiFactory.ButtonTextPreset.COMPACT,
                    button -> context.mutateRefresh(() -> special.blockStateProperties = ""));
            clearProperties.horizontalSizing(Sizing.fixed(Width.CLEAR.pixels()));
            card.child(stateActions);
            stateActions.child(clearProperties);
        }

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
            BlockStatePropertyMeta property) {
        String currentValue = currentValues.getOrDefault(property.key(), "");
        var originalProperties = context.originalStack().get(DataComponents.BLOCK_STATE);
        String originalValue = originalProperties == null
                ? ""
                : originalProperties.properties().getOrDefault(property.key(), "");
        boolean changed = !Objects.equals(currentValue, originalValue);
        boolean hasOverride = !currentValue.isBlank();

        Component valueLabel = hasOverride
                ? Component.literal(currentValue).withColor(0x55FFFF)
                : ItemEditorText.tr("common.unset").copy().withColor(0xAAAAAA);
        ButtonComponent valueButton = UiFactory.button(
                valueLabel,
                UiFactory.ButtonTextPreset.STANDARD,
                button -> context.mutateRefresh(() -> setBlockStateProperty(
                        special,
                        property.key(),
                        nextBlockStateValue(
                                currentValue,
                                property.values(),
                                Minecraft.getInstance().hasShiftDown() ? -1 : 1))));
        valueButton.active(hasOverride || !property.values().isEmpty());
        FlowLayout row = CompactFieldLayout.selectorRow(Component.literal(property.key()), valueButton, changed);
        row.id(blockStateAnchor(property));
        return row;
    }

    private static FlowLayout textWithPickerCompact(
            SpecialDataPanelContext context,
            String value,
            Consumer<String> setter,
            List<String> entries,
            String pickerTitle,
            boolean normalizeInput) {
        FlowLayout row = UiFactory.row();
        row.gap(holderSetRowGap());
        row.child(UiFactory.textBox(
                        value,
                        text -> context.mutate(
                                () -> setter.accept(normalizeInput ? IdFieldNormalizer.normalize(text) : text)))
                .horizontalSizing(Sizing.expand(100)));
        row.child(UiFactory.button(
                        Control.COMMON_PICK.label(),
                        UiFactory.ButtonTextPreset.STANDARD,
                        button -> context.openSearchablePicker(
                                pickerTitle,
                                "",
                                entries,
                                id -> id,
                                id -> context.mutateRefresh(() -> setter.accept(id))))
                .horizontalSizing(Sizing.fixed(Width.FIXED_PICK.pixels())));
        return row;
    }

    static List<EditorSearchDialog.Target> holderSetSearchTargets(
            SpecialDataPanelContext context,
            List<String> path,
            Supplier<String> scope,
            Runnable expand,
            String value,
            boolean blocks) {
        List<EditorSearchDialog.Target> targets = new ArrayList<>();
        for (Control action : List.of(Control.COMMON_ADD_TYPE, Control.COMMON_ADD_TAG)) {
            targets.add(action.target(context, EditorCategory.COMBAT, path, scope, expand));
        }
        List<String> entries = splitIdentifierTokens(value);
        boolean expansionWarning = blocks
                ? hasHolderSetExpansionWarning(context, entries, Registries.BLOCK)
                : hasHolderSetExpansionWarning(context, entries, Registries.DAMAGE_TYPE);
        if (expansionWarning) {
            targets.add(Control.COMPONENT_TWEAKS_ALLOW_TAG_EXPANSION.target(
                    context, EditorCategory.COMBAT, path, scope, expand));
        }
        if (!blocks) {
            for (int index = 0; index < Math.max(1, entries.size()); index++) {
                List<String> entryPath = new ArrayList<>(path);
                entryPath.add(Control.COMMON_ENTRY.text() + " " + (index + 1));
                targets.add(Control.COMMON_ENTRY.target(
                        context, EditorCategory.COMBAT, entryPath, scope, expand, holderEntryAnchor(index)));
                if (!entries.isEmpty()) {
                    targets.add(Control.COMMON_REMOVE.target(
                            context, EditorCategory.COMBAT, entryPath, scope, expand, holderEntryAnchor(index)));
                }
            }
        }
        return List.copyOf(targets);
    }

    private static String holderEntryAnchor(int index) {
        return "component-holder-entry-" + index;
    }

    static FlowLayout damageTypeHolderSetEditor(
            SpecialDataPanelContext context,
            String value,
            Consumer<String> setter,
            Supplier<String> currentValueSupplier,
            String pickerTitle,
            boolean collapsed,
            Consumer<Boolean> collapsedSetter,
            boolean allowTagExpansion,
            Consumer<Boolean> allowTagExpansionSetter) {
        FlowLayout editor = UiFactory.column();
        editor.gap(SECTION_ROW_GAP);

        List<String> entries = splitIdentifierTokens(value);
        FlowLayout summaryRow = UiFactory.row();
        UIComponent summary = UiFactory.muted(
                holderSetSummary(entries, "special.advanced.component_tweaks.damage_types"), Width.LONG.pixels() + 120);
        summary.horizontalSizing(Sizing.expand(100));
        summaryRow.child(summary);
        if (collapsedSetter != null) {
            ButtonComponent toggle = UiFactory.button(
                    Component.literal(collapsed ? SYMBOL_SECTION_COLLAPSED : SYMBOL_SECTION_EXPANDED),
                    UiFactory.ButtonTextPreset.STANDARD,
                    button -> context.mutateRefresh(() -> collapsedSetter.accept(!collapsed)));
            toggle.horizontalSizing(Sizing.fixed(Width.ICON.pixels()));
            summaryRow.child(toggle);
        }
        editor.child(summaryRow);

        if (hasHolderSetExpansionWarning(context, entries, Registries.DAMAGE_TYPE)) {
            var warning = UiFactory.message(
                    ItemEditorText.str("special.advanced.component_tweaks.tag_expansion_warning"), 0xFF8A8A);
            warning.maxWidth(Math.min(UiFactory.responsiveBodyTextWidth(), Width.LONG.pixels() + 60));
            warning.horizontalSizing(Sizing.fill(100));
            editor.child(warning);
            if (allowTagExpansionSetter != null) {
                editor.child(UiFactory.checkbox(
                        Control.COMPONENT_TWEAKS_ALLOW_TAG_EXPANSION.label(),
                        allowTagExpansion,
                        context.bindToggle(allowTagExpansionSetter)));
            }
        }

        if (collapsed) {
            return editor;
        }

        boolean compactHolderRows = usesStackedPickerRows();
        FlowLayout pickers = holderSetPickerButtons(context, setter, currentValueSupplier, pickerTitle);
        if (compactHolderRows) {
            editor.child(pickers);
        }

        List<String> displayedEntries = entries.isEmpty() ? List.of("") : entries;
        for (int index = 0; index < displayedEntries.size(); index++) {
            int currentIndex = index;
            boolean emptyPlaceholder = entries.isEmpty();
            String entryValue = displayedEntries.get(index);
            FlowLayout row = UiFactory.row();
            row.id(holderEntryAnchor(index));
            row.gap(holderSetRowGap());
            UIComponent kind = UiFactory.muted(holderSetEntryKind(entryValue), holderSetKindWidth());
            kind.horizontalSizing(Sizing.fixed(holderSetKindWidth()));
            row.child(kind);
            row.child(UiFactory.textBox(
                            entryValue,
                            context.bindText(text -> setter.accept(
                                    replaceIdentifierListValue(currentValueSupplier.get(), currentIndex, text))))
                    .horizontalSizing(Sizing.expand(100)));
            ButtonComponent remove = UiFactory.button(
                    compactHolderRows ? Component.literal("X").withColor(0xFF8A8A) : Control.COMMON_REMOVE.label(),
                    UiFactory.ButtonTextPreset.COMPACT,
                    button -> context.mutateRefresh(
                            () -> setter.accept(removeIdentifierListValue(currentValueSupplier.get(), currentIndex))));
            if (compactHolderRows) {
                remove.tooltip(List.of(Control.COMMON_REMOVE.label()));
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

    static FlowLayout blockHolderSetEditor(
            SpecialDataPanelContext context,
            String value,
            Consumer<String> setter,
            Supplier<String> currentValueSupplier,
            String pickerTitle,
            boolean allowTagExpansion,
            Consumer<Boolean> allowTagExpansionSetter) {
        FlowLayout editor = UiFactory.column();
        editor.gap(SECTION_ROW_GAP);

        List<String> entries = splitIdentifierTokens(value);
        UIComponent summary = UiFactory.muted(
                holderSetSummary(entries, "special.advanced.combat.tool_rule_blocks"), Width.LONG.pixels() + 120);
        summary.horizontalSizing(Sizing.fill(100));
        editor.child(summary);

        editor.child(textWithPickerCompact(
                context, value, setter, context.optionalRegistryIds(Registries.BLOCK), pickerTitle, false));
        editor.child(holderSetPickerButtons(
                context,
                setter,
                currentValueSupplier,
                pickerTitle,
                context.optionalRegistryIds(Registries.BLOCK),
                context.registryTagIds(Registries.BLOCK, "")));

        if (hasHolderSetExpansionWarning(context, entries, Registries.BLOCK)) {
            var warning = UiFactory.message(
                    ItemEditorText.str("special.advanced.combat.tool_rule_tag_expansion_warning"), 0xFF8A8A);
            warning.maxWidth(Math.min(UiFactory.responsiveBodyTextWidth(), Width.LONG.pixels() + 60));
            warning.horizontalSizing(Sizing.fill(100));
            editor.child(warning);
            editor.child(UiFactory.checkbox(
                    Control.COMPONENT_TWEAKS_ALLOW_TAG_EXPANSION.label(),
                    allowTagExpansion,
                    context.bindToggle(allowTagExpansionSetter)));
        }
        return editor;
    }

    private static FlowLayout holderSetPickerButtons(
            SpecialDataPanelContext context,
            Consumer<String> setter,
            Supplier<String> currentValueSupplier,
            String pickerTitle) {
        return holderSetPickerButtons(
                context,
                setter,
                currentValueSupplier,
                pickerTitle,
                context.optionalRegistryIds(Registries.DAMAGE_TYPE),
                context.registryTagIds(Registries.DAMAGE_TYPE, ""));
    }

    private static FlowLayout holderSetPickerButtons(
            SpecialDataPanelContext context,
            Consumer<String> setter,
            Supplier<String> currentValueSupplier,
            String pickerTitle,
            List<String> typeIds,
            List<String> tagIds) {
        ButtonComponent pickType = UiFactory.button(
                Control.COMMON_ADD_TYPE.label().copy().withColor(0x91E68C),
                UiFactory.ButtonTextPreset.COMPACT,
                button -> context.openSearchablePicker(
                        pickerTitle,
                        "",
                        typeIds,
                        id -> id,
                        id -> context.mutateRefresh(
                                () -> setter.accept(appendIdentifierListValue(currentValueSupplier.get(), id)))));

        ButtonComponent pickTag = UiFactory.button(
                Control.COMMON_ADD_TAG.label().copy().withColor(0x8AC8FF),
                UiFactory.ButtonTextPreset.COMPACT,
                button -> context.openSearchablePicker(
                        pickerTitle,
                        "",
                        tagIds,
                        id -> id,
                        id -> context.mutateRefresh(
                                () -> setter.accept(appendIdentifierListValue(currentValueSupplier.get(), "#" + id)))));
        return UiFactory.actionButtonRow(pickType, pickTag);
    }

    private static int holderSetKindWidth() {
        return Width.HOLDER_KIND.pixels();
    }

    private static int holderSetRemoveButtonWidth() {
        if (usesStackedPickerRows()) {
            return Width.HOLDER_REMOVE.pixels();
        }
        return Width.REMOVE.pixels();
    }

    private static int holderSetRowGap() {
        return Math.max(1, UiFactory.scaleProfile().tightSpacing() - 2);
    }

    private static Component holderSetSummary(List<String> entries, String translationPrefix) {
        if (entries.isEmpty()) {
            return ItemEditorText.tr(translationPrefix + "_none");
        }
        int tags = 0;
        for (String entry : entries) {
            if (entry.startsWith("#")) {
                tags++;
            }
        }
        return ItemEditorText.tr(translationPrefix + "_summary", entries.size(), entries.size() - tags, tags);
    }

    private static <T> boolean hasHolderSetExpansionWarning(
            SpecialDataPanelContext context, List<String> entries, ResourceKey<? extends Registry<T>> registryKey) {
        if (entries.size() <= 1) {
            return false;
        }
        List<String> typeIds = context.optionalRegistryIds(registryKey);
        List<String> tagIds = context.registryTagIds(registryKey, "");
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
            return Control.COMMON_TAG.label().copy().withColor(0x8AC8FF);
        }
        if (value != null && !value.isBlank()) {
            return Control.COMMON_TYPE.label().copy().withColor(0x91E68C);
        }
        return Control.COMMON_ENTRY.label();
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
            String key =
                    separator < 0 ? token.trim() : token.substring(0, separator).trim();
            String value = separator < 0 ? "" : token.substring(separator + 1).trim();
            if (!key.isEmpty()) {
                valuesByKey.put(key, value);
            }
        }
        return valuesByKey;
    }

    static String nextBlockStateValue(String current, List<String> values) {
        return nextBlockStateValue(current, values, 1);
    }

    static String nextBlockStateValue(String current, List<String> values, int direction) {
        if (values.isEmpty()) return "";
        if (current != null && !current.isBlank() && !values.contains(current)) return "";
        int index = current == null || current.isBlank() ? -1 : values.indexOf(current);
        int next = Math.floorMod(index + 1 + direction, values.size() + 1);
        return next == 0 ? "" : values.get(next - 1);
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

    private static <T extends Comparable<T>> BlockStatePropertyMeta blockStatePropertyMeta(
            BlockState defaultState, Property<T> property) {
        return new BlockStatePropertyMeta(
                property.getName(),
                SpecialDataPanelContext.propertyValues(property, true),
                property.getName(defaultState.getValue(property)));
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

    private record BlockStatePropertyMeta(String key, List<String> values, String defaultValue) {}

    static List<String> jukeboxSongIds(SpecialDataPanelContext context, String currentId) {
        return withCurrentId(context.optionalRegistryIds(Registries.JUKEBOX_SONG), currentId);
    }

    static List<String> withCurrentId(List<String> values, String currentId) {
        List<String> ids = new ArrayList<>(values);
        String normalizedCurrent = IdFieldNormalizer.normalize(currentId);
        if (!normalizedCurrent.isBlank() && !ids.contains(normalizedCurrent)) {
            ids.add(normalizedCurrent);
        }
        ids.sort(String::compareTo);
        return ids;
    }
}
