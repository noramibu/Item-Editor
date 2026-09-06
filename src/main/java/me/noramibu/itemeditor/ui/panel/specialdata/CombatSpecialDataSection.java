package me.noramibu.itemeditor.ui.panel.specialdata;

import static me.noramibu.itemeditor.ui.panel.specialdata.AdvancedItemSpecialDataSection.COMBAT_REPAIRABLE_EMPTY_HINT_WIDTH;
import static me.noramibu.itemeditor.ui.panel.specialdata.AdvancedItemSpecialDataSection.COMPACT_ICON_BUTTON_BASE;
import static me.noramibu.itemeditor.ui.panel.specialdata.AdvancedItemSpecialDataSection.SECTION_ROW_GAP;
import static me.noramibu.itemeditor.ui.panel.specialdata.AdvancedItemSpecialDataSection.SYMBOL_SECTION_COLLAPSED;
import static me.noramibu.itemeditor.ui.panel.specialdata.AdvancedItemSpecialDataSection.SYMBOL_SECTION_EXPANDED;
import static me.noramibu.itemeditor.ui.panel.specialdata.AdvancedItemSpecialDataSection.blockHolderSetEditor;
import static me.noramibu.itemeditor.ui.panel.specialdata.AdvancedItemSpecialDataSection.collapsibleCard;
import static me.noramibu.itemeditor.ui.panel.specialdata.AdvancedItemSpecialDataSection.compactCheckboxRow;
import static me.noramibu.itemeditor.ui.panel.specialdata.AdvancedItemSpecialDataSection.compactField;
import static me.noramibu.itemeditor.ui.panel.specialdata.AdvancedItemSpecialDataSection.compactIconButtonWidth;
import static me.noramibu.itemeditor.ui.panel.specialdata.AdvancedItemSpecialDataSection.compactIdField;
import static me.noramibu.itemeditor.ui.panel.specialdata.AdvancedItemSpecialDataSection.compactIdTextWidth;
import static me.noramibu.itemeditor.ui.panel.specialdata.AdvancedItemSpecialDataSection.compactLongFieldWidth;
import static me.noramibu.itemeditor.ui.panel.specialdata.AdvancedItemSpecialDataSection.compactNumericFieldWidth;
import static me.noramibu.itemeditor.ui.panel.specialdata.AdvancedItemSpecialDataSection.compactPickerButtonWidth;
import static me.noramibu.itemeditor.ui.panel.specialdata.AdvancedItemSpecialDataSection.compactRemoveButtonWidth;
import static me.noramibu.itemeditor.ui.panel.specialdata.AdvancedItemSpecialDataSection.compactTextField;
import static me.noramibu.itemeditor.ui.panel.specialdata.AdvancedItemSpecialDataSection.compactTinyFieldWidth;
import static me.noramibu.itemeditor.ui.panel.specialdata.AdvancedItemSpecialDataSection.compactTriStateBooleanPicker;
import static me.noramibu.itemeditor.ui.panel.specialdata.AdvancedItemSpecialDataSection.damageTypeHolderSetEditor;
import static me.noramibu.itemeditor.ui.panel.specialdata.AdvancedItemSpecialDataSection.denseEquipmentRow;
import static me.noramibu.itemeditor.ui.panel.specialdata.AdvancedItemSpecialDataSection.distributeRowChildren;
import static me.noramibu.itemeditor.ui.panel.specialdata.AdvancedItemSpecialDataSection.expandedBlocksAttacksDamageReductionDraft;
import static me.noramibu.itemeditor.ui.panel.specialdata.AdvancedItemSpecialDataSection.expandedToolRuleDraft;
import static me.noramibu.itemeditor.ui.panel.specialdata.AdvancedItemSpecialDataSection.filledTextBox;
import static me.noramibu.itemeditor.ui.panel.specialdata.AdvancedItemSpecialDataSection.prefersStackedCompactRows;
import static me.noramibu.itemeditor.ui.panel.specialdata.AdvancedItemSpecialDataSection.responsiveRow;
import static me.noramibu.itemeditor.ui.panel.specialdata.AdvancedItemSpecialDataSection.valueOrDefault;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.UIComponent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import me.noramibu.itemeditor.editor.EditorCategory;
import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.ui.component.EditorSearchDialog;
import me.noramibu.itemeditor.ui.component.PickerFieldFactory;
import me.noramibu.itemeditor.ui.component.UiFactory;
import me.noramibu.itemeditor.util.ItemEditorText;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.SwingAnimationType;

public final class CombatSpecialDataSection {
    private static final int BLOCKS_ATTACKS_REDUCTION_EMPTY_HINT_WIDTH = 320;
    private static final int BLOCKS_ATTACKS_REDUCTION_SUMMARY_HINT_WIDTH = 360;

    private enum Control implements ComponentSearchField {
        COMBAT_DAMAGE_RESISTANT_TYPES("resistant", "special.advanced.combat.damage_resistant_types"),
        COMBAT_BEHAVIOR_TITLE("behavior", "special.advanced.combat.behavior_title"),
        COMBAT_BLOCKS_ATTACKS_TITLE("blocks", "special.advanced.combat.blocks_attacks_title"),
        COMMON_EQUIPMENT("equipment", "common.equipment"),
        COMBAT_SWING_ANIMATION_TYPE("behavior", "special.advanced.combat.swing_animation_type"),
        COMBAT_SWING_ANIMATION_DURATION("behavior", "special.advanced.combat.swing_animation_duration"),
        COMBAT_PIERCING_KNOCKBACK("behavior", "special.advanced.combat.piercing_knockback"),
        COMBAT_PIERCING_DISMOUNTS("behavior", "special.advanced.combat.piercing_dismounts"),
        COMBAT_PIERCING_SOUND("behavior", "special.advanced.combat.piercing_sound"),
        COMBAT_PIERCING_HIT_SOUND("behavior", "special.advanced.combat.piercing_hit_sound"),
        COMBAT_KINETIC_TITLE("behavior", "special.advanced.combat.kinetic_title"),
        COMBAT_KINETIC_CONTACT_COOLDOWN("behavior", "special.advanced.combat.kinetic_contact_cooldown"),
        COMBAT_KINETIC_DELAY_TICKS("behavior", "special.advanced.combat.kinetic_delay_ticks"),
        COMBAT_KINETIC_FORWARD_MOVEMENT("behavior", "special.advanced.combat.kinetic_forward_movement"),
        COMBAT_KINETIC_DAMAGE_MULTIPLIER("behavior", "special.advanced.combat.kinetic_damage_multiplier"),
        COMBAT_KINETIC_SOUND("behavior", "special.advanced.combat.kinetic_sound"),
        COMBAT_KINETIC_HIT_SOUND("behavior", "special.advanced.combat.kinetic_hit_sound"),
        COMBAT_BLOCKS_ATTACKS_DELAY("blocks", "special.advanced.combat.blocks_attacks_delay"),
        COMBAT_BLOCKS_ATTACKS_DISABLE_SCALE("blocks", "special.advanced.combat.blocks_attacks_disable_scale"),
        COMBAT_BLOCKS_ATTACKS_BYPASSED_BY("blocks", "special.advanced.combat.blocks_attacks_bypassed_by"),
        COMBAT_BLOCKS_ATTACKS_BLOCK_SOUND("blocks", "special.advanced.combat.blocks_attacks_block_sound"),
        COMBAT_BLOCKS_ATTACKS_DISABLE_SOUND("blocks", "special.advanced.combat.blocks_attacks_disable_sound"),
        COMBAT_BLOCKS_ATTACKS_DAMAGE_REDUCTIONS("blocks", "special.advanced.combat.blocks_attacks_damage_reductions"),
        COMBAT_BLOCKS_ATTACKS_ADD_DAMAGE_REDUCTION(
                "blocks", "special.advanced.combat.blocks_attacks_add_damage_reduction"),
        COMBAT_BLOCKS_ATTACKS_DAMAGE_REDUCTION("entryTitle", "special.advanced.combat.blocks_attacks_damage_reduction"),
        COMBAT_BLOCKS_ATTACKS_REDUCTION_TYPES("reduction", "special.advanced.combat.blocks_attacks_reduction_types"),
        COMBAT_BLOCKS_ATTACKS_REDUCTION_ANGLE("reduction", "special.advanced.combat.blocks_attacks_reduction_angle"),
        COMBAT_BLOCKS_ATTACKS_REDUCTION_BASE("reduction", "special.advanced.combat.blocks_attacks_reduction_base"),
        COMBAT_BLOCKS_ATTACKS_REDUCTION_FACTOR("reduction", "special.advanced.combat.blocks_attacks_reduction_factor"),
        COMBAT_BLOCKS_ATTACKS_ITEM_DAMAGE_TITLE("blocks", "special.advanced.combat.blocks_attacks_item_damage_title"),
        COMBAT_BLOCKS_ATTACKS_ITEM_DAMAGE_THRESHOLD(
                "blocks", "special.advanced.combat.blocks_attacks_item_damage_threshold"),
        COMBAT_BLOCKS_ATTACKS_ITEM_DAMAGE_BASE("blocks", "special.advanced.combat.blocks_attacks_item_damage_base"),
        COMBAT_BLOCKS_ATTACKS_ITEM_DAMAGE_FACTOR("blocks", "special.advanced.combat.blocks_attacks_item_damage_factor"),
        COMBAT_WEAPON_TITLE("equipment", "special.advanced.combat.weapon_title"),
        COMBAT_WEAPON_DAMAGE("equipment", "special.advanced.combat.weapon_damage"),
        COMBAT_WEAPON_DISABLE("equipment", "special.advanced.combat.weapon_disable"),
        COMBAT_TOOL_TITLE("equipment", "special.advanced.combat.tool_title"),
        COMBAT_TOOL_SPEED("equipment", "special.advanced.combat.tool_speed"),
        COMBAT_TOOL_DAMAGE("equipment", "special.advanced.combat.tool_damage"),
        COMBAT_TOOL_CREATIVE("equipment", "special.advanced.combat.tool_creative"),
        COMBAT_TOOL_RULES_TITLE("equipment", "special.advanced.combat.tool_rules_title"),
        COMBAT_TOOL_RULES_ADD("equipment", "special.advanced.combat.tool_rules_add"),
        COMMON_CLEAR_ALL("listAction", "common.clear_all"),
        COMMON_EXPAND_ALL("listAction", "common.expand_all"),
        COMMON_COLLAPSE_ALL("listAction", "common.collapse_all"),
        COMBAT_TOOL_RULE("entryTitle", "special.advanced.combat.tool_rule"),
        COMMON_UP("entryAction", "common.up"),
        COMMON_DOWN("entryAction", "common.down"),
        COMMON_DUPLICATE("entryAction", "common.duplicate"),
        COMMON_REMOVE("entryAction", "common.remove"),
        COMBAT_TOOL_RULE_BLOCKS("rule", "special.advanced.combat.tool_rule_blocks"),
        COMBAT_TOOL_RULE_SPEED("rule", "special.advanced.combat.tool_rule_speed"),
        COMBAT_TOOL_RULE_CORRECT_FOR_DROPS("rule", "special.advanced.combat.tool_rule_correct_for_drops"),
        COMBAT_REPAIRABLE_TITLE("equipment", "special.advanced.combat.repairable_title"),
        COMBAT_REPAIRABLE_ADD("equipment", "special.advanced.combat.repairable_add"),
        COMBAT_REPAIR_ITEM("repair", "special.advanced.combat.repair_item"),
        COMBAT_ATTACK_RANGE_TITLE("equipment", "special.advanced.combat.attack_range_title"),
        COMBAT_RANGE_MIN_REACH("equipment", "special.advanced.combat.range_min_reach"),
        COMBAT_RANGE_MAX_REACH("equipment", "special.advanced.combat.range_max_reach"),
        COMBAT_RANGE_MIN_CREATIVE("equipment", "special.advanced.combat.range_min_creative"),
        COMBAT_RANGE_MAX_CREATIVE("equipment", "special.advanced.combat.range_max_creative"),
        COMBAT_RANGE_HITBOX("equipment", "special.advanced.combat.range_hitbox"),
        COMBAT_RANGE_MOB_FACTOR("equipment", "special.advanced.combat.range_mob_factor");

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

    private CombatSpecialDataSection() {}

    public static List<EditorSearchDialog.Target> searchTargets(SpecialDataPanelContext context) {
        List<EditorSearchDialog.Target> targets = new ArrayList<>();
        ItemEditorState.SpecialData special = context.special();
        Runnable resistant = () -> {
            special.uiCombatDamageResistantCollapsed = false;
            special.uiDamageResistantTypesCollapsed = false;
        };
        Runnable behavior = () -> special.uiCombatBehaviorCollapsed = false;
        Runnable blocks = () -> special.uiCombatBlocksAttacksCollapsed = false;
        Runnable equipment = () -> special.uiCombatEquipmentCollapsed = false;
        addGroup(
                targets,
                context,
                "resistant",
                List.of(Control.COMBAT_DAMAGE_RESISTANT_TYPES.text()),
                () -> "",
                resistant);

        targets.addAll(AdvancedItemSpecialDataSection.holderSetSearchTargets(
                context,
                List.of(Control.COMBAT_DAMAGE_RESISTANT_TYPES.text()),
                () -> "combat-resistant-types",
                resistant,
                special.damageResistantTypeIds,
                false));
        Runnable bypass = () -> {
            blocks.run();
            special.uiBlocksAttacksBypassedByTypesCollapsed = false;
        };
        targets.addAll(AdvancedItemSpecialDataSection.holderSetSearchTargets(
                context,
                List.of(Control.COMBAT_BLOCKS_ATTACKS_TITLE.text(), Control.COMBAT_BLOCKS_ATTACKS_BYPASSED_BY.text()),
                () -> "combat-bypass-types",
                bypass,
                special.blocksAttacksBypassedByTypeIds,
                false));
        for (int index = 0; index < special.blocksAttacksDamageReductions.size(); index++) {
            ItemEditorState.BlocksAttacksDamageReductionDraft draft = special.blocksAttacksDamageReductions.get(index);
            targets.addAll(AdvancedItemSpecialDataSection.holderSetSearchTargets(
                    context,
                    List.of(
                            Control.COMBAT_BLOCKS_ATTACKS_TITLE.text(),
                            Control.COMBAT_BLOCKS_ATTACKS_DAMAGE_REDUCTION.text(index + 1),
                            Control.COMBAT_BLOCKS_ATTACKS_REDUCTION_TYPES.text()),
                    () -> holderScope("reduction", special.blocksAttacksDamageReductions, draft),
                    () -> {
                        blocks.run();
                        draft.uiCollapsed = false;
                    },
                    draft.typeIds,
                    false));
        }
        for (int index = 0; index < special.toolRules.size(); index++) {
            ItemEditorState.ToolRuleDraft draft = special.toolRules.get(index);
            targets.addAll(AdvancedItemSpecialDataSection.holderSetSearchTargets(
                    context,
                    List.of(
                            Control.COMMON_EQUIPMENT.text(),
                            Control.COMBAT_TOOL_RULE.text(index + 1),
                            Control.COMBAT_TOOL_RULE_BLOCKS.text()),
                    () -> holderScope("rule", special.toolRules, draft),
                    () -> {
                        equipment.run();
                        draft.uiCollapsed = false;
                    },
                    draft.blockIds,
                    true));
        }
        addGroup(targets, context, "behavior", List.of(Control.COMBAT_BEHAVIOR_TITLE.text()), () -> "", behavior);
        for (Control field : Control.values()) {
            if (field.group.equals("blocks")) {
                targets.add(field.target(
                        context,
                        EditorCategory.COMBAT,
                        List.of(Control.COMBAT_BLOCKS_ATTACKS_TITLE.text()),
                        () -> "",
                        () -> {
                            blocks.run();
                            if (field == Control.COMBAT_BLOCKS_ATTACKS_BYPASSED_BY) {
                                special.uiBlocksAttacksBypassedByTypesCollapsed = false;
                            }
                        }));
            }
        }
        addGroup(targets, context, "equipment", List.of(Control.COMMON_EQUIPMENT.text()), () -> "", equipment);

        for (Control action : Control.values()) {
            if (action.group.equals("listAction")
                    && (action == Control.COMMON_CLEAR_ALL || !special.toolRules.isEmpty())) {
                targets.add(action.target(
                        context,
                        EditorCategory.COMBAT,
                        List.of(Control.COMMON_EQUIPMENT.text(), Control.COMBAT_TOOL_RULES_TITLE.text()),
                        () -> "combat-tool-rules",
                        equipment));
            }
        }
        addEntries(
                targets,
                context,
                "reduction",
                Control.COMBAT_BLOCKS_ATTACKS_TITLE,
                Control.COMBAT_BLOCKS_ATTACKS_DAMAGE_REDUCTION,
                special.blocksAttacksDamageReductions,
                blocks,
                draft -> draft.uiCollapsed = false);
        addEntries(
                targets,
                context,
                "rule",
                Control.COMMON_EQUIPMENT,
                Control.COMBAT_TOOL_RULE,
                special.toolRules,
                equipment,
                draft -> draft.uiCollapsed = false);
        for (int index = 0; index < special.repairableItemIds.size(); index++) {
            int currentIndex = index;
            addGroup(
                    targets,
                    context,
                    "repair",
                    List.of(
                            Control.COMMON_EQUIPMENT.text(),
                            Control.COMBAT_REPAIRABLE_TITLE.text(),
                            Control.COMBAT_REPAIR_ITEM.text() + " " + (index + 1)),
                    () -> currentIndex < special.repairableItemIds.size()
                            ? ComponentSearchField.scope("repair", currentIndex)
                            : null,
                    equipment);
            targets.add(Control.COMMON_REMOVE.target(
                    context,
                    EditorCategory.COMBAT,
                    List.of(Control.COMMON_EQUIPMENT.text(), Control.COMBAT_REPAIR_ITEM.text() + " " + (index + 1)),
                    () -> currentIndex < special.repairableItemIds.size()
                            ? ComponentSearchField.scope("repair", currentIndex)
                            : null,
                    equipment));
        }
        return List.copyOf(targets);
    }

    private static <T> String holderScope(String group, List<T> entries, T draft) {
        String scope = ComponentSearchField.scope(group, entries, draft);
        return scope == null ? null : scope + "-types";
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
                targets.add(field.target(context, EditorCategory.COMBAT, path, scope, expand));
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
            List<String> path = List.of(section.text(), title.text(index + 1));
            Supplier<String> scope = () -> ComponentSearchField.scope(group, entries, entry);
            Runnable expand = () -> {
                expandSection.run();
                expandEntry.accept(entry);
            };
            addGroup(targets, context, group, path, scope, expand);
            for (Control action : Control.values()) {
                if (!action.group.equals("entryAction")) continue;
                if (action == Control.COMMON_DUPLICATE && !group.equals("rule")) continue;
                if (action == Control.COMMON_UP && index == 0) continue;
                if (action == Control.COMMON_DOWN && index == entries.size() - 1) continue;
                targets.add(action.target(context, EditorCategory.COMBAT, path, scope, expand));
            }
        }
    }

    public static FlowLayout buildDamageResistant(SpecialDataPanelContext context) {
        ItemEditorState.SpecialData special = context.special();
        return collapsibleCard(
                context,
                Control.COMBAT_DAMAGE_RESISTANT_TYPES.label(),
                special.uiCombatDamageResistantCollapsed,
                value -> special.uiCombatDamageResistantCollapsed = value,
                () -> UiFactory.column()
                        .child(compactField(
                                        Control.COMBAT_DAMAGE_RESISTANT_TYPES.label(),
                                        damageTypeHolderSetEditor(
                                                context,
                                                special.damageResistantTypeIds,
                                                value -> special.damageResistantTypeIds = value,
                                                () -> special.damageResistantTypeIds,
                                                Control.COMBAT_DAMAGE_RESISTANT_TYPES.text(),
                                                special.uiDamageResistantTypesCollapsed,
                                                value -> special.uiDamageResistantTypesCollapsed = value,
                                                special.allowDamageResistantTagExpansion,
                                                value -> special.allowDamageResistantTagExpansion = value),
                                        compactLongFieldWidth() + 70)
                                .id("combat-resistant-types")));
    }

    public static FlowLayout buildBehavior(SpecialDataPanelContext context) {
        ItemEditorState.SpecialData special = context.special();
        return collapsibleCard(
                context,
                Control.COMBAT_BEHAVIOR_TITLE.label(),
                special.uiCombatBehaviorCollapsed,
                value -> special.uiCombatBehaviorCollapsed = value,
                () -> UiFactory.column().child(buildCombatBehaviorCard(context, special)));
    }

    public static FlowLayout buildBlocksAttacks(SpecialDataPanelContext context) {
        ItemEditorState.SpecialData special = context.special();
        return collapsibleCard(
                context,
                Control.COMBAT_BLOCKS_ATTACKS_TITLE.label(),
                special.uiCombatBlocksAttacksCollapsed,
                value -> special.uiCombatBlocksAttacksCollapsed = value,
                () -> UiFactory.column().child(buildBlocksAttacksCard(context, special)));
    }

    public static FlowLayout buildEquipment(SpecialDataPanelContext context) {
        ItemEditorState.SpecialData special = context.special();
        return collapsibleCard(
                context,
                Control.COMMON_EQUIPMENT.label(),
                special.uiCombatEquipmentCollapsed,
                value -> special.uiCombatEquipmentCollapsed = value,
                () -> UiFactory.column()
                        .child(buildWeaponCard(context, special))
                        .child(buildToolCard(context, special))
                        .child(buildRepairableCard(context, special))
                        .child(buildAttackRangeCard(context, special)));
    }

    private static FlowLayout buildCombatBehaviorCard(
            SpecialDataPanelContext context, ItemEditorState.SpecialData special) {
        FlowLayout card = UiFactory.subCard();
        boolean stacked = prefersStackedCompactRows();
        int numericWidth = compactNumericFieldWidth();
        int pickerWidth = compactPickerButtonWidth();
        int idWidth = compactIdTextWidth();

        ButtonComponent swingButton = UiFactory.button(
                PickerFieldFactory.selectedOrFallback(
                        special.swingAnimationType, ItemEditorText.tr("special.advanced.select")),
                UiFactory.ButtonTextPreset.STANDARD,
                anchor -> context.openClearableDropdown(
                        anchor,
                        ItemEditorText.tr("common.none"),
                        () -> context.mutate(() -> special.swingAnimationType = ""),
                        Arrays.asList(SwingAnimationType.values()),
                        SwingAnimationType::name,
                        type -> context.mutate(() -> special.swingAnimationType = type.name())));
        swingButton.horizontalSizing(Sizing.fill(100));
        FlowLayout swingTypeField =
                compactField(Control.COMBAT_SWING_ANIMATION_TYPE.label(), swingButton, pickerWidth + 40);
        FlowLayout swingDurationField = compactField(
                Control.COMBAT_SWING_ANIMATION_DURATION.label(),
                filledTextBox(context, special.swingAnimationDuration, value -> special.swingAnimationDuration = value),
                numericWidth + 40);
        if (stacked) {
            card.child(swingTypeField);
            card.child(swingDurationField);
        } else {
            FlowLayout swingRow = responsiveRow();
            swingRow.gap(SECTION_ROW_GAP);
            distributeRowChildren(swingRow, swingTypeField, swingDurationField);
            card.child(swingRow);
        }

        UIComponent piercingKnockback = compactTriStateBooleanPicker(
                context,
                Control.COMBAT_PIERCING_KNOCKBACK.label(),
                special.piercingDealsKnockback,
                value -> special.piercingDealsKnockback = value,
                compactPickerButtonWidth());
        UIComponent piercingDismounts = UiFactory.checkbox(
                Control.COMBAT_PIERCING_DISMOUNTS.label(),
                special.piercingDismounts,
                context.bindToggle(value -> special.piercingDismounts = value));
        card.child(compactCheckboxRow(piercingKnockback, piercingDismounts));

        card.child(compactIdField(
                context,
                Control.COMBAT_PIERCING_SOUND.label(),
                special.piercingSoundId,
                value -> special.piercingSoundId = value,
                context.optionalRegistryIds(Registries.SOUND_EVENT),
                Control.COMBAT_PIERCING_SOUND.text(),
                idWidth));
        card.child(compactIdField(
                context,
                Control.COMBAT_PIERCING_HIT_SOUND.label(),
                special.piercingHitSoundId,
                value -> special.piercingHitSoundId = value,
                context.optionalRegistryIds(Registries.SOUND_EVENT),
                Control.COMBAT_PIERCING_HIT_SOUND.text(),
                idWidth));

        FlowLayout kineticCard = UiFactory.subCard();
        kineticCard.child(UiFactory.title(Control.COMBAT_KINETIC_TITLE.label()).shadow(false));

        FlowLayout kineticContactField = compactField(
                Control.COMBAT_KINETIC_CONTACT_COOLDOWN.label(),
                filledTextBox(
                        context,
                        special.kineticContactCooldownTicks,
                        value -> special.kineticContactCooldownTicks = value),
                numericWidth + 40);
        FlowLayout kineticDelayField = compactField(
                Control.COMBAT_KINETIC_DELAY_TICKS.label(),
                filledTextBox(context, special.kineticDelayTicks, value -> special.kineticDelayTicks = value),
                numericWidth + 40);
        FlowLayout kineticForwardField = compactField(
                Control.COMBAT_KINETIC_FORWARD_MOVEMENT.label(),
                filledTextBox(context, special.kineticForwardMovement, value -> special.kineticForwardMovement = value),
                numericWidth + 40);
        FlowLayout kineticDamageField = compactField(
                Control.COMBAT_KINETIC_DAMAGE_MULTIPLIER.label(),
                filledTextBox(
                        context, special.kineticDamageMultiplier, value -> special.kineticDamageMultiplier = value),
                numericWidth + 40);
        kineticCard.child(
                denseEquipmentRow(kineticContactField, kineticDelayField, kineticForwardField, kineticDamageField));
        kineticCard.child(compactIdField(
                context,
                Control.COMBAT_KINETIC_SOUND.label(),
                special.kineticSoundId,
                value -> special.kineticSoundId = value,
                context.optionalRegistryIds(Registries.SOUND_EVENT),
                Control.COMBAT_KINETIC_SOUND.text(),
                idWidth));
        kineticCard.child(compactIdField(
                context,
                Control.COMBAT_KINETIC_HIT_SOUND.label(),
                special.kineticHitSoundId,
                value -> special.kineticHitSoundId = value,
                context.optionalRegistryIds(Registries.SOUND_EVENT),
                Control.COMBAT_KINETIC_HIT_SOUND.text(),
                idWidth));
        card.child(kineticCard);
        return card;
    }

    private static FlowLayout buildBlocksAttacksCard(
            SpecialDataPanelContext context, ItemEditorState.SpecialData special) {
        FlowLayout card = UiFactory.subCard();
        int numericWidth = compactNumericFieldWidth();
        int idWidth = compactIdTextWidth();

        FlowLayout blockDelayField = compactField(
                Control.COMBAT_BLOCKS_ATTACKS_DELAY.label(),
                filledTextBox(
                        context,
                        special.blocksAttacksBlockDelaySeconds,
                        value -> special.blocksAttacksBlockDelaySeconds = value),
                numericWidth + 40);
        FlowLayout disableScaleField = compactField(
                Control.COMBAT_BLOCKS_ATTACKS_DISABLE_SCALE.label(),
                filledTextBox(
                        context,
                        special.blocksAttacksDisableCooldownScale,
                        value -> special.blocksAttacksDisableCooldownScale = value),
                numericWidth + 40);
        card.child(denseEquipmentRow(blockDelayField, disableScaleField));

        card.child(buildBlocksAttacksDamageReductionsEditor(context, special));
        card.child(buildBlocksAttacksItemDamageCard(context, special));
        card.child(compactField(
                        Control.COMBAT_BLOCKS_ATTACKS_BYPASSED_BY.label(),
                        damageTypeHolderSetEditor(
                                context,
                                special.blocksAttacksBypassedByTypeIds,
                                value -> special.blocksAttacksBypassedByTypeIds = value,
                                () -> special.blocksAttacksBypassedByTypeIds,
                                Control.COMBAT_BLOCKS_ATTACKS_BYPASSED_BY.text(),
                                special.uiBlocksAttacksBypassedByTypesCollapsed,
                                value -> special.uiBlocksAttacksBypassedByTypesCollapsed = value,
                                special.allowBlocksAttacksBypassedByTagExpansion,
                                value -> special.allowBlocksAttacksBypassedByTagExpansion = value),
                        idWidth + 80)
                .id("combat-bypass-types"));
        card.child(compactIdField(
                context,
                Control.COMBAT_BLOCKS_ATTACKS_BLOCK_SOUND.label(),
                special.blocksAttacksBlockSoundId,
                value -> special.blocksAttacksBlockSoundId = value,
                context.optionalRegistryIds(Registries.SOUND_EVENT),
                Control.COMBAT_BLOCKS_ATTACKS_BLOCK_SOUND.text(),
                idWidth));
        card.child(compactIdField(
                context,
                Control.COMBAT_BLOCKS_ATTACKS_DISABLE_SOUND.label(),
                special.blocksAttacksDisableSoundId,
                value -> special.blocksAttacksDisableSoundId = value,
                context.optionalRegistryIds(Registries.SOUND_EVENT),
                Control.COMBAT_BLOCKS_ATTACKS_DISABLE_SOUND.text(),
                idWidth));
        return card;
    }

    private static FlowLayout buildBlocksAttacksDamageReductionsEditor(
            SpecialDataPanelContext context, ItemEditorState.SpecialData special) {
        FlowLayout card = UiFactory.subCard();
        card.child(UiFactory.title(Control.COMBAT_BLOCKS_ATTACKS_DAMAGE_REDUCTIONS.label())
                .shadow(false));

        ButtonComponent addButton = UiFactory.button(
                Control.COMBAT_BLOCKS_ATTACKS_ADD_DAMAGE_REDUCTION.label(),
                UiFactory.ButtonTextPreset.STANDARD,
                button -> context.mutateRefresh(
                        () -> special.blocksAttacksDamageReductions.add(expandedBlocksAttacksDamageReductionDraft())));
        addButton.horizontalSizing(Sizing.fill(100));
        card.child(addButton);

        if (special.blocksAttacksDamageReductions.isEmpty()) {
            card.child(UiFactory.muted(
                    ItemEditorText.tr("special.advanced.combat.blocks_attacks_damage_reductions_empty"),
                    BLOCKS_ATTACKS_REDUCTION_EMPTY_HINT_WIDTH));
            return card;
        }

        int idWidth = compactIdTextWidth();
        int numericWidth = compactTinyFieldWidth();
        for (int index = 0; index < special.blocksAttacksDamageReductions.size(); index++) {
            int currentIndex = index;
            ItemEditorState.BlocksAttacksDamageReductionDraft draft = special.blocksAttacksDamageReductions.get(index);
            FlowLayout reductionCard = context.createReorderableCard(
                    Control.COMBAT_BLOCKS_ATTACKS_DAMAGE_REDUCTION.label(index + 1),
                    currentIndex > 0,
                    () -> context.swapEntries(special.blocksAttacksDamageReductions, currentIndex, currentIndex - 1),
                    currentIndex < special.blocksAttacksDamageReductions.size() - 1,
                    () -> context.swapEntries(special.blocksAttacksDamageReductions, currentIndex, currentIndex + 1),
                    () -> special.blocksAttacksDamageReductions.remove(currentIndex));
            reductionCard.id(ComponentSearchField.scope("reduction", index));

            FlowLayout summaryRow = responsiveRow();
            UIComponent summary = UiFactory.muted(
                    Component.literal(blocksAttacksDamageReductionSummary(draft)),
                    BLOCKS_ATTACKS_REDUCTION_SUMMARY_HINT_WIDTH);
            summary.horizontalSizing(Sizing.expand(100));
            summaryRow.child(summary);
            ButtonComponent collapseToggle = UiFactory.button(
                    Component.literal(draft.uiCollapsed ? SYMBOL_SECTION_COLLAPSED : SYMBOL_SECTION_EXPANDED),
                    UiFactory.ButtonTextPreset.STANDARD,
                    button -> context.mutateRefresh(() -> draft.uiCollapsed = !draft.uiCollapsed));
            collapseToggle.horizontalSizing(Sizing.fixed(compactIconButtonWidth()));
            summaryRow.child(collapseToggle);
            reductionCard.child(summaryRow);

            if (!draft.uiCollapsed) {
                reductionCard.child(compactField(
                                Control.COMBAT_BLOCKS_ATTACKS_REDUCTION_TYPES.label(),
                                damageTypeHolderSetEditor(
                                        context,
                                        draft.typeIds,
                                        value -> draft.typeIds = value,
                                        () -> draft.typeIds,
                                        Control.COMBAT_BLOCKS_ATTACKS_REDUCTION_TYPES.text(),
                                        false,
                                        null,
                                        draft.allowTagExpansion,
                                        value -> draft.allowTagExpansion = value),
                                idWidth + 80)
                        .id(ComponentSearchField.scope("reduction", index) + "-types"));
                FlowLayout angle = compactTextField(
                        context,
                        Control.COMBAT_BLOCKS_ATTACKS_REDUCTION_ANGLE.label(),
                        draft.horizontalBlockingAngle,
                        value -> draft.horizontalBlockingAngle = value,
                        numericWidth);
                FlowLayout base = compactTextField(
                        context,
                        Control.COMBAT_BLOCKS_ATTACKS_REDUCTION_BASE.label(),
                        draft.base,
                        value -> draft.base = value,
                        numericWidth);
                FlowLayout factor = compactTextField(
                        context,
                        Control.COMBAT_BLOCKS_ATTACKS_REDUCTION_FACTOR.label(),
                        draft.factor,
                        value -> draft.factor = value,
                        numericWidth);
                reductionCard.child(denseEquipmentRow(angle, base, factor));
            }

            card.child(reductionCard);
        }

        return card;
    }

    private static FlowLayout buildBlocksAttacksItemDamageCard(
            SpecialDataPanelContext context, ItemEditorState.SpecialData special) {
        FlowLayout card = UiFactory.subCard();
        card.child(UiFactory.title(Control.COMBAT_BLOCKS_ATTACKS_ITEM_DAMAGE_TITLE.label())
                .shadow(false));

        int numericWidth = compactTinyFieldWidth();
        FlowLayout threshold = compactTextField(
                context,
                Control.COMBAT_BLOCKS_ATTACKS_ITEM_DAMAGE_THRESHOLD.label(),
                special.blocksAttacksItemDamageThreshold,
                value -> special.blocksAttacksItemDamageThreshold = value,
                numericWidth);
        FlowLayout base = compactTextField(
                context,
                Control.COMBAT_BLOCKS_ATTACKS_ITEM_DAMAGE_BASE.label(),
                special.blocksAttacksItemDamageBase,
                value -> special.blocksAttacksItemDamageBase = value,
                numericWidth);
        FlowLayout factor = compactTextField(
                context,
                Control.COMBAT_BLOCKS_ATTACKS_ITEM_DAMAGE_FACTOR.label(),
                special.blocksAttacksItemDamageFactor,
                value -> special.blocksAttacksItemDamageFactor = value,
                numericWidth);
        card.child(denseEquipmentRow(threshold, base, factor));
        return card;
    }

    private static String blocksAttacksDamageReductionSummary(ItemEditorState.BlocksAttacksDamageReductionDraft draft) {
        String types = valueOrDefault(draft.typeIds, "all damage");
        String angle = valueOrDefault(draft.horizontalBlockingAngle, "90");
        String base = valueOrDefault(draft.base, "0");
        String factor = valueOrDefault(draft.factor, "1");
        return types + " - angle " + angle + " - " + base + " + " + factor + "x";
    }

    private static FlowLayout buildWeaponCard(SpecialDataPanelContext context, ItemEditorState.SpecialData special) {
        FlowLayout card = UiFactory.subCard();
        card.child(UiFactory.title(Control.COMBAT_WEAPON_TITLE.label()).shadow(false));
        int numberWidth = compactNumericFieldWidth();

        FlowLayout damageField = compactField(
                Control.COMBAT_WEAPON_DAMAGE.label(),
                filledTextBox(
                        context, special.weaponItemDamagePerAttack, value -> special.weaponItemDamagePerAttack = value),
                numberWidth + 40);
        FlowLayout disableField = compactField(
                Control.COMBAT_WEAPON_DISABLE.label(),
                filledTextBox(
                        context,
                        special.weaponDisableBlockingForSeconds,
                        value -> special.weaponDisableBlockingForSeconds = value),
                numberWidth + 40);
        card.child(denseEquipmentRow(damageField, disableField));
        return card;
    }

    private static FlowLayout buildToolCard(SpecialDataPanelContext context, ItemEditorState.SpecialData special) {
        FlowLayout card = UiFactory.subCard();
        card.child(UiFactory.title(Control.COMBAT_TOOL_TITLE.label()).shadow(false));
        int numberWidth = compactNumericFieldWidth();

        FlowLayout speedField = compactField(
                Control.COMBAT_TOOL_SPEED.label(),
                filledTextBox(context, special.toolDefaultMiningSpeed, value -> special.toolDefaultMiningSpeed = value),
                numberWidth + 40);
        FlowLayout damageField = compactField(
                Control.COMBAT_TOOL_DAMAGE.label(),
                filledTextBox(context, special.toolDamagePerBlock, value -> special.toolDamagePerBlock = value),
                numberWidth + 40);
        UIComponent creativeField = UiFactory.checkbox(
                Control.COMBAT_TOOL_CREATIVE.label(),
                special.toolCanDestroyBlocksInCreative,
                context.bindToggle(value -> special.toolCanDestroyBlocksInCreative = value));
        card.child(denseEquipmentRow(speedField, damageField, creativeField));
        card.child(buildToolRulesEditor(context, special));
        return card;
    }

    private static FlowLayout buildToolRulesEditor(
            SpecialDataPanelContext context, ItemEditorState.SpecialData special) {
        FlowLayout section = UiFactory.subCard();
        section.id("combat-tool-rules");
        section.child(UiFactory.title(Control.COMBAT_TOOL_RULES_TITLE.label()).shadow(false));

        ButtonComponent addRule = UiFactory.positiveButton(
                Control.COMBAT_TOOL_RULES_ADD.label(),
                UiFactory.ButtonTextPreset.STANDARD,
                button -> context.mutateRefresh(() -> special.toolRules.add(expandedToolRuleDraft())));
        ButtonComponent clearAll = UiFactory.negativeButton(
                Control.COMMON_CLEAR_ALL.label(),
                UiFactory.ButtonTextPreset.STANDARD,
                button -> context.mutateRefresh(special.toolRules::clear));
        section.child(UiFactory.actionButtonRow(addRule, clearAll));

        if (special.toolRules.isEmpty()) {
            section.child(UiFactory.muted(
                    ItemEditorText.tr("special.advanced.combat.tool_rules_empty"), COMBAT_REPAIRABLE_EMPTY_HINT_WIDTH));
            return section;
        }

        ButtonComponent expandAll = UiFactory.button(
                Control.COMMON_EXPAND_ALL.label(),
                UiFactory.ButtonTextPreset.STANDARD,
                button -> context.mutateRefresh(() -> special.toolRules.forEach(entry -> entry.uiCollapsed = false)));
        ButtonComponent collapseAll = UiFactory.button(
                Control.COMMON_COLLAPSE_ALL.label(),
                UiFactory.ButtonTextPreset.STANDARD,
                button -> context.mutateRefresh(() -> special.toolRules.forEach(entry -> entry.uiCollapsed = true)));
        section.child(UiFactory.actionButtonRow(expandAll, collapseAll));

        for (int index = 0; index < special.toolRules.size(); index++) {
            section.child(buildToolRuleCard(context, special, index));
        }
        return section;
    }

    private static FlowLayout buildToolRuleCard(
            SpecialDataPanelContext context, ItemEditorState.SpecialData special, int index) {
        ItemEditorState.ToolRuleDraft draft = special.toolRules.get(index);
        FlowLayout card = UiFactory.subCard();
        card.id(ComponentSearchField.scope("rule", index));
        FlowLayout header = UiFactory.row();
        header.child(UiFactory.title(Control.COMBAT_TOOL_RULE.text(index + 1))
                .shadow(false)
                .horizontalSizing(Sizing.expand(100)));
        ButtonComponent collapseToggle = UiFactory.button(
                Component.literal(draft.uiCollapsed ? SYMBOL_SECTION_COLLAPSED : SYMBOL_SECTION_EXPANDED),
                UiFactory.ButtonTextPreset.STANDARD,
                button -> context.mutateRefresh(() -> draft.uiCollapsed = !draft.uiCollapsed));
        collapseToggle.horizontalSizing(Sizing.fixed(COMPACT_ICON_BUTTON_BASE));
        header.child(collapseToggle);
        card.child(header);
        card.child(UiFactory.muted(toolRuleSummary(draft), COMBAT_REPAIRABLE_EMPTY_HINT_WIDTH));

        ButtonComponent upButton = UiFactory.button(
                Control.COMMON_UP.label(),
                UiFactory.ButtonTextPreset.COMPACT,
                button -> context.mutateRefresh(() -> context.swapEntries(special.toolRules, index, index - 1)));
        upButton.active(index > 0);
        ButtonComponent downButton = UiFactory.button(
                Control.COMMON_DOWN.label(),
                UiFactory.ButtonTextPreset.COMPACT,
                button -> context.mutateRefresh(() -> context.swapEntries(special.toolRules, index, index + 1)));
        downButton.active(index < special.toolRules.size() - 1);
        ButtonComponent duplicateButton = UiFactory.button(
                Control.COMMON_DUPLICATE.label(),
                UiFactory.ButtonTextPreset.COMPACT,
                button -> context.mutateRefresh(
                        () -> special.toolRules.add(index + 1, ItemEditorState.ToolRuleDraft.copy(draft))));
        ButtonComponent removeButton = UiFactory.negativeButton(
                Control.COMMON_REMOVE.label(),
                UiFactory.ButtonTextPreset.COMPACT,
                button -> context.mutateRefresh(() -> special.toolRules.remove(index)));
        card.child(UiFactory.actionButtonRow(upButton, downButton, duplicateButton, removeButton));

        if (draft.uiCollapsed) {
            return card;
        }

        int numericWidth = compactNumericFieldWidth();
        card.child(compactField(
                        Control.COMBAT_TOOL_RULE_BLOCKS.label(),
                        blockHolderSetEditor(
                                context,
                                draft.blockIds,
                                value -> draft.blockIds = value,
                                () -> draft.blockIds,
                                Control.COMBAT_TOOL_RULE_BLOCKS.text(),
                                draft.allowTagExpansion,
                                value -> draft.allowTagExpansion = value),
                        compactLongFieldWidth() + 100)
                .id(ComponentSearchField.scope("rule", index) + "-types"));

        UIComponent speed = compactTextField(
                context,
                Control.COMBAT_TOOL_RULE_SPEED.label(),
                draft.speed,
                value -> draft.speed = value,
                numericWidth);
        UIComponent correct = compactTriStateBooleanPicker(
                context,
                Control.COMBAT_TOOL_RULE_CORRECT_FOR_DROPS.label(),
                draft.correctForDrops,
                value -> draft.correctForDrops = value,
                compactPickerButtonWidth());
        card.child(denseEquipmentRow(speed, correct));
        return card;
    }

    private static Component toolRuleSummary(ItemEditorState.ToolRuleDraft draft) {
        String blocks = draft.blockIds == null || draft.blockIds.isBlank()
                ? ItemEditorText.str("special.advanced.combat.tool_rule_summary_no_blocks")
                : draft.blockIds;
        String speed = draft.speed == null || draft.speed.isBlank()
                ? ItemEditorText.str("special.advanced.combat.tool_rule_summary_default_speed")
                : ItemEditorText.str("special.advanced.combat.tool_rule_summary_speed", draft.speed);
        String drops = draft.correctForDrops == null || draft.correctForDrops.isBlank()
                ? ItemEditorText.str("special.advanced.combat.tool_rule_summary_drop_default")
                : ItemEditorText.str("special.advanced.combat.tool_rule_summary_drops", draft.correctForDrops);
        return Component.literal(blocks + " | " + speed + " | " + drops);
    }

    private static FlowLayout buildRepairableCard(
            SpecialDataPanelContext context, ItemEditorState.SpecialData special) {
        FlowLayout card = UiFactory.subCard();
        card.child(UiFactory.title(Control.COMBAT_REPAIRABLE_TITLE.label()).shadow(false));
        int idWidth = compactIdTextWidth();
        List<String> availableItems = context.itemIdsWithoutAir();

        ButtonComponent addButton = UiFactory.button(
                Control.COMBAT_REPAIRABLE_ADD.label(),
                UiFactory.ButtonTextPreset.STANDARD,
                button -> context.mutateRefresh(() -> special.repairableItemIds.add("")));
        addButton.horizontalSizing(Sizing.fill(100));
        card.child(addButton);

        if (special.repairableItemIds.isEmpty()) {
            card.child(UiFactory.muted(
                    ItemEditorText.tr("special.advanced.combat.repairable_empty"), COMBAT_REPAIRABLE_EMPTY_HINT_WIDTH));
            return card;
        }

        for (int index = 0; index < special.repairableItemIds.size(); index++) {
            int currentIndex = index;
            String value = special.repairableItemIds.get(index);
            FlowLayout row = denseEquipmentRow();
            row.id(ComponentSearchField.scope("repair", index));
            FlowLayout itemField = compactIdField(
                    context,
                    Control.COMBAT_REPAIR_ITEM.label(),
                    value,
                    newValue -> special.repairableItemIds.set(currentIndex, newValue),
                    availableItems,
                    Control.COMBAT_REPAIR_ITEM.text(),
                    idWidth);
            ButtonComponent remove = UiFactory.button(
                    Control.COMMON_REMOVE.label(),
                    UiFactory.ButtonTextPreset.STANDARD,
                    button -> context.mutateRefresh(() -> special.repairableItemIds.remove(currentIndex)));
            FlowLayout removeField = compactField(Component.literal(" "), remove, compactRemoveButtonWidth());
            itemField.horizontalSizing(Sizing.expand(100));
            removeField.horizontalSizing(Sizing.fixed(compactRemoveButtonWidth()));
            row.child(itemField);
            row.child(removeField);
            card.child(row);
        }
        return card;
    }

    private static FlowLayout buildAttackRangeCard(
            SpecialDataPanelContext context, ItemEditorState.SpecialData special) {
        FlowLayout card = UiFactory.subCard();
        card.child(UiFactory.title(Control.COMBAT_ATTACK_RANGE_TITLE.label()).shadow(false));
        int numberWidth = compactNumericFieldWidth();

        FlowLayout minReach = compactTextField(
                context,
                Control.COMBAT_RANGE_MIN_REACH.label(),
                special.attackRangeMinReach,
                value -> special.attackRangeMinReach = value,
                numberWidth);
        FlowLayout maxReach = compactTextField(
                context,
                Control.COMBAT_RANGE_MAX_REACH.label(),
                special.attackRangeMaxReach,
                value -> special.attackRangeMaxReach = value,
                numberWidth);
        FlowLayout minCreative = compactTextField(
                context,
                Control.COMBAT_RANGE_MIN_CREATIVE.label(),
                special.attackRangeMinCreativeReach,
                value -> special.attackRangeMinCreativeReach = value,
                numberWidth);
        FlowLayout maxCreative = compactTextField(
                context,
                Control.COMBAT_RANGE_MAX_CREATIVE.label(),
                special.attackRangeMaxCreativeReach,
                value -> special.attackRangeMaxCreativeReach = value,
                numberWidth);
        card.child(denseEquipmentRow(minReach, maxReach, minCreative, maxCreative));

        FlowLayout hitbox = compactTextField(
                context,
                Control.COMBAT_RANGE_HITBOX.label(),
                special.attackRangeHitboxMargin,
                value -> special.attackRangeHitboxMargin = value,
                numberWidth);
        FlowLayout mobFactor = compactTextField(
                context,
                Control.COMBAT_RANGE_MOB_FACTOR.label(),
                special.attackRangeMobFactor,
                value -> special.attackRangeMobFactor = value,
                numberWidth);
        card.child(denseEquipmentRow(hitbox, mobFactor));
        return card;
    }
}
