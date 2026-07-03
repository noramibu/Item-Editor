package me.noramibu.itemeditor.ui.panel.specialdata;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.UIComponent;
import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.ui.component.PickerFieldFactory;
import me.noramibu.itemeditor.ui.component.UiFactory;
import me.noramibu.itemeditor.util.ItemEditorText;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.SwingAnimationType;

import java.util.Arrays;
import java.util.List;

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
import static me.noramibu.itemeditor.ui.panel.specialdata.AdvancedItemSpecialDataSection.denseEquipmentRow;
import static me.noramibu.itemeditor.ui.panel.specialdata.AdvancedItemSpecialDataSection.damageTypeHolderSetEditor;
import static me.noramibu.itemeditor.ui.panel.specialdata.AdvancedItemSpecialDataSection.distributeRowChildren;
import static me.noramibu.itemeditor.ui.panel.specialdata.AdvancedItemSpecialDataSection.expandedBlocksAttacksDamageReductionDraft;
import static me.noramibu.itemeditor.ui.panel.specialdata.AdvancedItemSpecialDataSection.expandedToolRuleDraft;
import static me.noramibu.itemeditor.ui.panel.specialdata.AdvancedItemSpecialDataSection.filledTextBox;
import static me.noramibu.itemeditor.ui.panel.specialdata.AdvancedItemSpecialDataSection.prefersStackedCompactRows;
import static me.noramibu.itemeditor.ui.panel.specialdata.AdvancedItemSpecialDataSection.responsiveRow;
import static me.noramibu.itemeditor.ui.panel.specialdata.AdvancedItemSpecialDataSection.valueOrDefault;

public final class CombatSpecialDataSection {
    private static final int BLOCKS_ATTACKS_REDUCTION_EMPTY_HINT_WIDTH = 320;
    private static final int BLOCKS_ATTACKS_REDUCTION_SUMMARY_HINT_WIDTH = 360;

    private CombatSpecialDataSection() {
    }

    public static FlowLayout buildDamageResistant(SpecialDataPanelContext context) {
        ItemEditorState.SpecialData special = context.special();
        return collapsibleCard(
                context,
                ItemEditorText.tr("special.advanced.combat.damage_resistant_types_title"),
                special.uiCombatDamageResistantCollapsed,
                value -> special.uiCombatDamageResistantCollapsed = value,
                () -> {
                    FlowLayout content = UiFactory.column();
                    content.child(compactField(
                            ItemEditorText.tr("special.advanced.combat.damage_resistant_types"),
                            damageTypeHolderSetEditor(
                                    context,
                                    special.damageResistantTypeIds,
                                    value -> special.damageResistantTypeIds = value,
                                    () -> special.damageResistantTypeIds,
                                    ItemEditorText.str("special.advanced.combat.damage_resistant_types"),
                                    special.uiDamageResistantTypesCollapsed,
                                    value -> special.uiDamageResistantTypesCollapsed = value,
                                    special.allowDamageResistantTagExpansion,
                                    value -> special.allowDamageResistantTagExpansion = value
                            ),
                            compactLongFieldWidth() + 70
                    ));
                    return content;
                }
        );
    }

    public static FlowLayout buildBehavior(SpecialDataPanelContext context) {
        ItemEditorState.SpecialData special = context.special();
        return collapsibleCard(
                context,
                ItemEditorText.tr("special.advanced.combat.behavior_title"),
                special.uiCombatBehaviorCollapsed,
                value -> special.uiCombatBehaviorCollapsed = value,
                () -> {
                    FlowLayout content = UiFactory.column();
                    content.child(buildCombatBehaviorCard(context, special));
                    return content;
                }
        );
    }

    public static FlowLayout buildBlocksAttacks(SpecialDataPanelContext context) {
        ItemEditorState.SpecialData special = context.special();
        return collapsibleCard(
                context,
                ItemEditorText.tr("special.advanced.combat.blocks_attacks_title"),
                special.uiCombatBlocksAttacksCollapsed,
                value -> special.uiCombatBlocksAttacksCollapsed = value,
                () -> {
                    FlowLayout content = UiFactory.column();
                    content.child(buildBlocksAttacksCard(context, special));
                    return content;
                }
        );
    }

    public static FlowLayout buildEquipment(SpecialDataPanelContext context) {
        ItemEditorState.SpecialData special = context.special();
        return collapsibleCard(
                context,
                ItemEditorText.tr("special.advanced.combat.title"),
                special.uiCombatEquipmentCollapsed,
                value -> special.uiCombatEquipmentCollapsed = value,
                () -> {
                    FlowLayout content = UiFactory.column();
                    content.child(buildWeaponCard(context, special));
                    content.child(buildToolCard(context, special));
                    content.child(buildRepairableCard(context, special));
                    content.child(buildAttackRangeCard(context, special));
                    return content;
                }
        );
    }

    private static FlowLayout buildCombatBehaviorCard(
            SpecialDataPanelContext context,
            ItemEditorState.SpecialData special
    ) {
        FlowLayout card = UiFactory.subCard();
        boolean stacked = prefersStackedCompactRows();
        int numericWidth = compactNumericFieldWidth();
        int pickerWidth = compactPickerButtonWidth();
        int idWidth = compactIdTextWidth();

        ButtonComponent swingButton = UiFactory.button(
                PickerFieldFactory.selectedOrFallback(
                        special.swingAnimationType,
                        ItemEditorText.tr("special.advanced.select")
                ),
                UiFactory.ButtonTextPreset.STANDARD,
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
                ItemEditorText.tr("special.advanced.combat.swing_animation_type"),
                swingButton,
                pickerWidth + 40
        );
        FlowLayout swingDurationField = compactField(
                ItemEditorText.tr("special.advanced.combat.swing_animation_duration"),
                filledTextBox(context, special.swingAnimationDuration, value -> special.swingAnimationDuration = value),
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

        UIComponent piercingKnockback = compactTriStateBooleanPicker(
                context,
                ItemEditorText.tr("special.advanced.combat.piercing_knockback"),
                special.piercingDealsKnockback,
                value -> special.piercingDealsKnockback = value,
                compactPickerButtonWidth()
        );
        UIComponent piercingDismounts = UiFactory.checkbox(
                ItemEditorText.tr("special.advanced.combat.piercing_dismounts"),
                special.piercingDismounts,
                context.bindToggle(value -> special.piercingDismounts = value)
        );
        card.child(compactCheckboxRow(piercingKnockback, piercingDismounts));

        card.child(compactIdField(
                context,
                ItemEditorText.tr("special.advanced.combat.piercing_sound"),
                special.piercingSoundId,
                value -> special.piercingSoundId = value,
                context.optionalRegistryIds(Registries.SOUND_EVENT),
                ItemEditorText.str("special.advanced.combat.piercing_sound"),
                idWidth
        ));
        card.child(compactIdField(
                context,
                ItemEditorText.tr("special.advanced.combat.piercing_hit_sound"),
                special.piercingHitSoundId,
                value -> special.piercingHitSoundId = value,
                context.optionalRegistryIds(Registries.SOUND_EVENT),
                ItemEditorText.str("special.advanced.combat.piercing_hit_sound"),
                idWidth
        ));

        FlowLayout kineticCard = UiFactory.subCard();
        kineticCard.child(UiFactory.title(
                ItemEditorText.tr("special.advanced.combat.kinetic_title")
        ).shadow(false));

        FlowLayout kineticContactField = compactField(
                ItemEditorText.tr("special.advanced.combat.kinetic_contact_cooldown"),
                filledTextBox(context, special.kineticContactCooldownTicks, value -> special.kineticContactCooldownTicks = value),
                numericWidth + 40
        );
        FlowLayout kineticDelayField = compactField(
                ItemEditorText.tr("special.advanced.combat.kinetic_delay_ticks"),
                filledTextBox(context, special.kineticDelayTicks, value -> special.kineticDelayTicks = value),
                numericWidth + 40
        );
        FlowLayout kineticForwardField = compactField(
                ItemEditorText.tr("special.advanced.combat.kinetic_forward_movement"),
                filledTextBox(context, special.kineticForwardMovement, value -> special.kineticForwardMovement = value),
                numericWidth + 40
        );
        FlowLayout kineticDamageField = compactField(
                ItemEditorText.tr("special.advanced.combat.kinetic_damage_multiplier"),
                filledTextBox(context, special.kineticDamageMultiplier, value -> special.kineticDamageMultiplier = value),
                numericWidth + 40
        );
        kineticCard.child(denseEquipmentRow(
                kineticContactField,
                kineticDelayField,
                kineticForwardField,
                kineticDamageField
        ));
        kineticCard.child(compactIdField(
                context,
                ItemEditorText.tr("special.advanced.combat.kinetic_sound"),
                special.kineticSoundId,
                value -> special.kineticSoundId = value,
                context.optionalRegistryIds(Registries.SOUND_EVENT),
                ItemEditorText.str("special.advanced.combat.kinetic_sound"),
                idWidth
        ));
        kineticCard.child(compactIdField(
                context,
                ItemEditorText.tr("special.advanced.combat.kinetic_hit_sound"),
                special.kineticHitSoundId,
                value -> special.kineticHitSoundId = value,
                context.optionalRegistryIds(Registries.SOUND_EVENT),
                ItemEditorText.str("special.advanced.combat.kinetic_hit_sound"),
                idWidth
        ));
        card.child(kineticCard);
        return card;
    }

    private static FlowLayout buildBlocksAttacksCard(
            SpecialDataPanelContext context,
            ItemEditorState.SpecialData special
    ) {
        FlowLayout card = UiFactory.subCard();
        int numericWidth = compactNumericFieldWidth();
        int idWidth = compactIdTextWidth();

        FlowLayout blockDelayField = compactField(
                ItemEditorText.tr("special.advanced.combat.blocks_attacks_delay"),
                filledTextBox(
                        context,
                        special.blocksAttacksBlockDelaySeconds,
                        value -> special.blocksAttacksBlockDelaySeconds = value
                ),
                numericWidth + 40
        );
        FlowLayout disableScaleField = compactField(
                ItemEditorText.tr("special.advanced.combat.blocks_attacks_disable_scale"),
                filledTextBox(
                        context,
                        special.blocksAttacksDisableCooldownScale,
                        value -> special.blocksAttacksDisableCooldownScale = value
                ),
                numericWidth + 40
        );
        card.child(denseEquipmentRow(blockDelayField, disableScaleField));

        card.child(buildBlocksAttacksDamageReductionsEditor(context, special));
        card.child(buildBlocksAttacksItemDamageCard(context, special));
        card.child(compactField(
                ItemEditorText.tr("special.advanced.combat.blocks_attacks_bypassed_by"),
                damageTypeHolderSetEditor(
                        context,
                        special.blocksAttacksBypassedByTypeIds,
                        value -> special.blocksAttacksBypassedByTypeIds = value,
                        () -> special.blocksAttacksBypassedByTypeIds,
                        ItemEditorText.str("special.advanced.combat.blocks_attacks_bypassed_by"),
                        special.uiBlocksAttacksBypassedByTypesCollapsed,
                        value -> special.uiBlocksAttacksBypassedByTypesCollapsed = value,
                        special.allowBlocksAttacksBypassedByTagExpansion,
                        value -> special.allowBlocksAttacksBypassedByTagExpansion = value
                ),
                idWidth + 80
        ));
        card.child(compactIdField(
                context,
                ItemEditorText.tr("special.advanced.combat.blocks_attacks_block_sound"),
                special.blocksAttacksBlockSoundId,
                value -> special.blocksAttacksBlockSoundId = value,
                context.optionalRegistryIds(Registries.SOUND_EVENT),
                ItemEditorText.str("special.advanced.combat.blocks_attacks_block_sound"),
                idWidth
        ));
        card.child(compactIdField(
                context,
                ItemEditorText.tr("special.advanced.combat.blocks_attacks_disable_sound"),
                special.blocksAttacksDisableSoundId,
                value -> special.blocksAttacksDisableSoundId = value,
                context.optionalRegistryIds(Registries.SOUND_EVENT),
                ItemEditorText.str("special.advanced.combat.blocks_attacks_disable_sound"),
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
                "special.advanced.combat.blocks_attacks_damage_reductions"
        )).shadow(false));

        ButtonComponent addButton = UiFactory.button(
                ItemEditorText.tr("special.advanced.combat.blocks_attacks_add_damage_reduction"),
                UiFactory.ButtonTextPreset.STANDARD,
                button -> context.mutateRefresh(() ->
                        special.blocksAttacksDamageReductions.add(expandedBlocksAttacksDamageReductionDraft()))
        );
        addButton.horizontalSizing(Sizing.fill(100));
        card.child(addButton);

        if (special.blocksAttacksDamageReductions.isEmpty()) {
            card.child(UiFactory.muted(
                    ItemEditorText.tr("special.advanced.combat.blocks_attacks_damage_reductions_empty"),
                    BLOCKS_ATTACKS_REDUCTION_EMPTY_HINT_WIDTH
            ));
            return card;
        }

        int idWidth = compactIdTextWidth();
        int numericWidth = compactTinyFieldWidth();
        for (int index = 0; index < special.blocksAttacksDamageReductions.size(); index++) {
            int currentIndex = index;
            ItemEditorState.BlocksAttacksDamageReductionDraft draft =
                    special.blocksAttacksDamageReductions.get(index);
            FlowLayout reductionCard = context.createReorderableCard(
                    ItemEditorText.tr(
                            "special.advanced.combat.blocks_attacks_damage_reduction",
                            index + 1
                    ),
                    currentIndex > 0,
                    () -> context.swapEntries(
                            special.blocksAttacksDamageReductions,
                            currentIndex,
                            currentIndex - 1
                    ),
                    currentIndex < special.blocksAttacksDamageReductions.size() - 1,
                    () -> context.swapEntries(
                            special.blocksAttacksDamageReductions,
                            currentIndex,
                            currentIndex + 1
                    ),
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
                        ItemEditorText.tr("special.advanced.combat.blocks_attacks_reduction_types"),
                        damageTypeHolderSetEditor(
                                context,
                                draft.typeIds,
                                value -> draft.typeIds = value,
                                () -> draft.typeIds,
                                ItemEditorText.str(
                                        "special.advanced.combat.blocks_attacks_reduction_types"
                                ),
                                false,
                                null,
                                draft.allowTagExpansion,
                                value -> draft.allowTagExpansion = value
                        ),
                        idWidth + 80
                ));
                FlowLayout angle = compactTextField(
                        context,
                        ItemEditorText.tr("special.advanced.combat.blocks_attacks_reduction_angle"),
                        draft.horizontalBlockingAngle,
                        value -> draft.horizontalBlockingAngle = value,
                        numericWidth
                );
                FlowLayout base = compactTextField(
                        context,
                        ItemEditorText.tr("special.advanced.combat.blocks_attacks_reduction_base"),
                        draft.base,
                        value -> draft.base = value,
                        numericWidth
                );
                FlowLayout factor = compactTextField(
                        context,
                        ItemEditorText.tr("special.advanced.combat.blocks_attacks_reduction_factor"),
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
                "special.advanced.combat.blocks_attacks_item_damage_title"
        )).shadow(false));

        int numericWidth = compactTinyFieldWidth();
        FlowLayout threshold = compactTextField(
                context,
                ItemEditorText.tr("special.advanced.combat.blocks_attacks_item_damage_threshold"),
                special.blocksAttacksItemDamageThreshold,
                value -> special.blocksAttacksItemDamageThreshold = value,
                numericWidth
        );
        FlowLayout base = compactTextField(
                context,
                ItemEditorText.tr("special.advanced.combat.blocks_attacks_item_damage_base"),
                special.blocksAttacksItemDamageBase,
                value -> special.blocksAttacksItemDamageBase = value,
                numericWidth
        );
        FlowLayout factor = compactTextField(
                context,
                ItemEditorText.tr("special.advanced.combat.blocks_attacks_item_damage_factor"),
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
        String types = valueOrDefault(draft.typeIds, "all damage");
        String angle = valueOrDefault(draft.horizontalBlockingAngle, "90");
        String base = valueOrDefault(draft.base, "0");
        String factor = valueOrDefault(draft.factor, "1");
        return types + " - angle " + angle + " - " + base + " + " + factor + "x";
    }

    private static FlowLayout buildWeaponCard(
            SpecialDataPanelContext context,
            ItemEditorState.SpecialData special
    ) {
        FlowLayout card = UiFactory.subCard();
        card.child(UiFactory.title(ItemEditorText.tr("special.advanced.combat.weapon_title")).shadow(false));
        int numberWidth = compactNumericFieldWidth();

        FlowLayout damageField = compactField(
                ItemEditorText.tr("special.advanced.combat.weapon_damage"),
                filledTextBox(context, special.weaponItemDamagePerAttack, value -> special.weaponItemDamagePerAttack = value),
                numberWidth + 40
        );
        FlowLayout disableField = compactField(
                ItemEditorText.tr("special.advanced.combat.weapon_disable"),
                filledTextBox(
                        context,
                        special.weaponDisableBlockingForSeconds,
                        value -> special.weaponDisableBlockingForSeconds = value
                ),
                numberWidth + 40
        );
        card.child(denseEquipmentRow(damageField, disableField));
        return card;
    }

    private static FlowLayout buildToolCard(
            SpecialDataPanelContext context,
            ItemEditorState.SpecialData special
    ) {
        FlowLayout card = UiFactory.subCard();
        card.child(UiFactory.title(ItemEditorText.tr("special.advanced.combat.tool_title")).shadow(false));
        int numberWidth = compactNumericFieldWidth();

        FlowLayout speedField = compactField(
                ItemEditorText.tr("special.advanced.combat.tool_speed"),
                filledTextBox(context, special.toolDefaultMiningSpeed, value -> special.toolDefaultMiningSpeed = value),
                numberWidth + 40
        );
        FlowLayout damageField = compactField(
                ItemEditorText.tr("special.advanced.combat.tool_damage"),
                filledTextBox(context, special.toolDamagePerBlock, value -> special.toolDamagePerBlock = value),
                numberWidth + 40
        );
        UIComponent creativeField = UiFactory.checkbox(
                ItemEditorText.tr("special.advanced.combat.tool_creative"),
                special.toolCanDestroyBlocksInCreative,
                context.bindToggle(value -> special.toolCanDestroyBlocksInCreative = value)
        );
        card.child(denseEquipmentRow(speedField, damageField, creativeField));
        card.child(buildToolRulesEditor(context, special));
        return card;
    }

    private static FlowLayout buildToolRulesEditor(
            SpecialDataPanelContext context,
            ItemEditorState.SpecialData special
    ) {
        FlowLayout section = UiFactory.subCard();
        section.child(UiFactory.title(ItemEditorText.tr("special.advanced.combat.tool_rules_title")).shadow(false));

        ButtonComponent addRule = UiFactory.positiveButton(
                ItemEditorText.tr("special.advanced.combat.tool_rules_add"),
                UiFactory.ButtonTextPreset.STANDARD,
                button -> context.mutateRefresh(() -> special.toolRules.add(expandedToolRuleDraft()))
        );
        ButtonComponent clearAll = UiFactory.negativeButton(
                ItemEditorText.tr("common.clear_all"),
                UiFactory.ButtonTextPreset.STANDARD,
                button -> context.mutateRefresh(special.toolRules::clear)
        );
        section.child(UiFactory.actionButtonRow(addRule, clearAll));

        if (special.toolRules.isEmpty()) {
            section.child(UiFactory.muted(
                    ItemEditorText.tr("special.advanced.combat.tool_rules_empty"),
                    COMBAT_REPAIRABLE_EMPTY_HINT_WIDTH
            ));
            return section;
        }

        ButtonComponent expandAll = UiFactory.button(
                ItemEditorText.tr("common.expand_all"),
                UiFactory.ButtonTextPreset.STANDARD,
                button -> context.mutateRefresh(() -> special.toolRules.forEach(entry -> entry.uiCollapsed = false))
        );
        ButtonComponent collapseAll = UiFactory.button(
                ItemEditorText.tr("common.collapse_all"),
                UiFactory.ButtonTextPreset.STANDARD,
                button -> context.mutateRefresh(() -> special.toolRules.forEach(entry -> entry.uiCollapsed = true))
        );
        section.child(UiFactory.actionButtonRow(expandAll, collapseAll));

        for (int index = 0; index < special.toolRules.size(); index++) {
            section.child(buildToolRuleCard(context, special, index));
        }
        return section;
    }

    private static FlowLayout buildToolRuleCard(
            SpecialDataPanelContext context,
            ItemEditorState.SpecialData special,
            int index
    ) {
        ItemEditorState.ToolRuleDraft draft = special.toolRules.get(index);
        FlowLayout card = UiFactory.subCard();
        FlowLayout header = UiFactory.row();
        header.child(UiFactory.title(ItemEditorText.str("special.advanced.combat.tool_rule", index + 1))
                .shadow(false)
                .horizontalSizing(Sizing.expand(100)));
        ButtonComponent collapseToggle = UiFactory.button(
                Component.literal(draft.uiCollapsed ? SYMBOL_SECTION_COLLAPSED : SYMBOL_SECTION_EXPANDED),
                UiFactory.ButtonTextPreset.STANDARD,
                button -> context.mutateRefresh(() -> draft.uiCollapsed = !draft.uiCollapsed)
        );
        collapseToggle.horizontalSizing(Sizing.fixed(COMPACT_ICON_BUTTON_BASE));
        header.child(collapseToggle);
        card.child(header);
        card.child(UiFactory.muted(toolRuleSummary(draft), COMBAT_REPAIRABLE_EMPTY_HINT_WIDTH));

        ButtonComponent upButton = UiFactory.button(
                ItemEditorText.tr("common.up"),
                UiFactory.ButtonTextPreset.COMPACT,
                button -> context.mutateRefresh(() -> context.swapEntries(special.toolRules, index, index - 1))
        );
        upButton.active(index > 0);
        ButtonComponent downButton = UiFactory.button(
                ItemEditorText.tr("common.down"),
                UiFactory.ButtonTextPreset.COMPACT,
                button -> context.mutateRefresh(() -> context.swapEntries(special.toolRules, index, index + 1))
        );
        downButton.active(index < special.toolRules.size() - 1);
        ButtonComponent duplicateButton = UiFactory.button(
                ItemEditorText.tr("common.duplicate"),
                UiFactory.ButtonTextPreset.COMPACT,
                button -> context.mutateRefresh(() ->
                        special.toolRules.add(index + 1, ItemEditorState.ToolRuleDraft.copy(draft)))
        );
        ButtonComponent removeButton = UiFactory.negativeButton(
                ItemEditorText.tr("common.remove"),
                UiFactory.ButtonTextPreset.COMPACT,
                button -> context.mutateRefresh(() -> special.toolRules.remove(index))
        );
        card.child(UiFactory.actionButtonRow(upButton, downButton, duplicateButton, removeButton));

        if (draft.uiCollapsed) {
            return card;
        }

        int numericWidth = compactNumericFieldWidth();
        card.child(compactField(
                ItemEditorText.tr("special.advanced.combat.tool_rule_blocks"),
                blockHolderSetEditor(
                        context,
                        draft.blockIds,
                        value -> draft.blockIds = value,
                        () -> draft.blockIds,
                        ItemEditorText.str("special.advanced.combat.tool_rule_blocks"),
                        draft.allowTagExpansion,
                        value -> draft.allowTagExpansion = value
                ),
                compactLongFieldWidth() + 100
        ));

        UIComponent speed = compactTextField(
                context,
                ItemEditorText.tr("special.advanced.combat.tool_rule_speed"),
                draft.speed,
                value -> draft.speed = value,
                numericWidth
        );
        UIComponent correct = compactTriStateBooleanPicker(
                context,
                ItemEditorText.tr("special.advanced.combat.tool_rule_correct_for_drops"),
                draft.correctForDrops,
                value -> draft.correctForDrops = value,
                compactPickerButtonWidth()
        );
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
            SpecialDataPanelContext context,
            ItemEditorState.SpecialData special
    ) {
        FlowLayout card = UiFactory.subCard();
        card.child(UiFactory.title(ItemEditorText.tr("special.advanced.combat.repairable_title")).shadow(false));
        int idWidth = compactIdTextWidth();
        List<String> availableItems = context.itemIdsWithoutAir();

        ButtonComponent addButton = UiFactory.button(
                ItemEditorText.tr("special.advanced.combat.repairable_add"),
                UiFactory.ButtonTextPreset.STANDARD,
                button -> context.mutateRefresh(() -> special.repairableItemIds.add(""))
        );
        addButton.horizontalSizing(Sizing.fill(100));
        card.child(addButton);

        if (special.repairableItemIds.isEmpty()) {
            card.child(UiFactory.muted(
                    ItemEditorText.tr("special.advanced.combat.repairable_empty"),
                    COMBAT_REPAIRABLE_EMPTY_HINT_WIDTH
            ));
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
            ButtonComponent remove = UiFactory.button(
                    ItemEditorText.tr("common.remove"),
                    UiFactory.ButtonTextPreset.STANDARD,
                    button -> context.mutateRefresh(() -> special.repairableItemIds.remove(currentIndex))
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

    private static FlowLayout buildAttackRangeCard(
            SpecialDataPanelContext context,
            ItemEditorState.SpecialData special
    ) {
        FlowLayout card = UiFactory.subCard();
        card.child(UiFactory.title(ItemEditorText.tr("special.advanced.combat.attack_range_title")).shadow(false));
        int numberWidth = compactNumericFieldWidth();

        FlowLayout minReach = compactTextField(
                context,
                ItemEditorText.tr("special.advanced.combat.range_min_reach"),
                special.attackRangeMinReach,
                value -> special.attackRangeMinReach = value,
                numberWidth
        );
        FlowLayout maxReach = compactTextField(
                context,
                ItemEditorText.tr("special.advanced.combat.range_max_reach"),
                special.attackRangeMaxReach,
                value -> special.attackRangeMaxReach = value,
                numberWidth
        );
        FlowLayout minCreative = compactTextField(
                context,
                ItemEditorText.tr("special.advanced.combat.range_min_creative"),
                special.attackRangeMinCreativeReach,
                value -> special.attackRangeMinCreativeReach = value,
                numberWidth
        );
        FlowLayout maxCreative = compactTextField(
                context,
                ItemEditorText.tr("special.advanced.combat.range_max_creative"),
                special.attackRangeMaxCreativeReach,
                value -> special.attackRangeMaxCreativeReach = value,
                numberWidth
        );
        card.child(denseEquipmentRow(minReach, maxReach, minCreative, maxCreative));

        FlowLayout hitbox = compactTextField(
                context,
                ItemEditorText.tr("special.advanced.combat.range_hitbox"),
                special.attackRangeHitboxMargin,
                value -> special.attackRangeHitboxMargin = value,
                numberWidth
        );
        FlowLayout mobFactor = compactTextField(
                context,
                ItemEditorText.tr("special.advanced.combat.range_mob_factor"),
                special.attackRangeMobFactor,
                value -> special.attackRangeMobFactor = value,
                numberWidth
        );
        card.child(denseEquipmentRow(hitbox, mobFactor));
        return card;
    }
}
