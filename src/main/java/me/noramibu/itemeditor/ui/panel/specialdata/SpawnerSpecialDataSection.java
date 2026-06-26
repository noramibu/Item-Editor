package me.noramibu.itemeditor.ui.panel.specialdata;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.UIComponent;
import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.ui.component.UiFactory;
import me.noramibu.itemeditor.ui.util.LayoutModeUtil;
import me.noramibu.itemeditor.util.IdFieldNormalizer;
import me.noramibu.itemeditor.util.ItemEditorCapabilities;
import me.noramibu.itemeditor.util.ItemEditorText;
import me.noramibu.itemeditor.util.RegistryUtil;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class SpawnerSpecialDataSection {

    private static final int COMPACT_LAYOUT_WIDTH_THRESHOLD = 560;
    private static final int INT_FIELD_WIDTH = 100;
    private static final int POTENTIAL_WEIGHT_FIELD_WIDTH = 120;
    private static final int LIGHT_FIELD_WIDTH = 74;
    private static final int PICKER_BUTTON_WIDTH = 70;
    private static final String ACTION_DUPLICATE = "Duplicate";

    private SpawnerSpecialDataSection() {
    }

    public static boolean supports(ItemStack stack) {
        return ItemEditorCapabilities.supportsSpawnerData(stack);
    }

    public static FlowLayout build(SpecialDataPanelContext context) {
        ItemEditorState.SpecialData special = context.special();
        Registry<EntityType<?>> entityRegistry = context.screen().session().registryAccess().lookupOrThrow(Registries.ENTITY_TYPE);
        List<String> entityIds = RegistryUtil.ids(entityRegistry);
        boolean compactLayout = isCompactLayout(context);

        FlowLayout section = UiFactory.section(ItemEditorText.tr("special.spawner.title"), Component.empty());
        section.child(buildSpawnDataCard(
                context,
                entityIds,
                special.spawnerSpawnData,
                ItemEditorText.tr("special.spawner.spawn_entity")
        ));

        addPackedRows(
                section,
                compactLayout ? 2 : 3,
                buildIntField(context, ItemEditorText.tr("special.spawner.delay"), special.spawnerDelay, value -> special.spawnerDelay = value),
                buildIntField(context, ItemEditorText.tr("special.spawner.min_spawn_delay"), special.spawnerMinSpawnDelay, value -> special.spawnerMinSpawnDelay = value),
                buildIntField(context, ItemEditorText.tr("special.spawner.max_spawn_delay"), special.spawnerMaxSpawnDelay, value -> special.spawnerMaxSpawnDelay = value)
        );
        addPackedRows(
                section,
                compactLayout ? 2 : 4,
                buildIntField(context, ItemEditorText.tr("special.spawner.spawn_count"), special.spawnerSpawnCount, value -> special.spawnerSpawnCount = value),
                buildIntField(context, ItemEditorText.tr("special.spawner.max_nearby_entities"), special.spawnerMaxNearbyEntities, value -> special.spawnerMaxNearbyEntities = value),
                buildIntField(context, ItemEditorText.tr("special.spawner.required_player_range"), special.spawnerRequiredPlayerRange, value -> special.spawnerRequiredPlayerRange = value),
                buildIntField(context, ItemEditorText.tr("special.spawner.spawn_range"), special.spawnerSpawnRange, value -> special.spawnerSpawnRange = value)
        );

        section.child(UiFactory.checkbox(
                ItemEditorText.tr("special.spawner.use_potentials"),
                special.spawnerUsePotentials,
                value -> context.mutateRefresh(() -> {
                    special.spawnerUsePotentials = value;
                    if (value && special.spawnerPotentials.isEmpty()) {
                        ItemEditorState.SpawnerPotentialDraft draft = new ItemEditorState.SpawnerPotentialDraft();
                        copySpawnData(special.spawnerSpawnData, draft.spawnData);
                        special.spawnerPotentials.add(draft);
                    }
                })
        ));

        if (!special.spawnerUsePotentials) {
            return section;
        }

        ButtonComponent addPotentialButton = UiFactory.button(
                ItemEditorText.tr("special.spawner.add_potential"), UiFactory.ButtonTextPreset.STANDARD,
                button -> context.mutateRefresh(() -> special.spawnerPotentials.add(new ItemEditorState.SpawnerPotentialDraft()))
        );
        ButtonComponent resetPotentialButton = UiFactory.button(
                ItemEditorText.tr("common.reset"), UiFactory.ButtonTextPreset.STANDARD,
                button -> context.mutateRefresh(() -> resetPotentialsFromSpawnData(special))
        );
        section.child(UiFactory.actionButtonRow(addPotentialButton, resetPotentialButton));

        if (!special.spawnerPotentials.isEmpty()) {
            ButtonComponent expandAll = UiFactory.button(ItemEditorText.tr("common.expand_all"), UiFactory.ButtonTextPreset.STANDARD, button ->
                    context.mutateRefresh(() -> special.spawnerPotentials.forEach(potential -> potential.uiCollapsed = false))
            );
            ButtonComponent collapseAll = UiFactory.button(ItemEditorText.tr("common.collapse_all"), UiFactory.ButtonTextPreset.STANDARD, button ->
                    context.mutateRefresh(() -> special.spawnerPotentials.forEach(potential -> potential.uiCollapsed = true))
            );
            section.child(UiFactory.actionButtonRow(expandAll, collapseAll));
        }

        for (int index = 0; index < special.spawnerPotentials.size(); index++) {
            section.child(buildPotentialCard(context, entityIds, special, index));
        }

        return section;
    }

    private static FlowLayout buildPotentialCard(
            SpecialDataPanelContext context,
            List<String> entityIds,
            ItemEditorState.SpecialData special,
            int index
    ) {
        ItemEditorState.SpawnerPotentialDraft draft = special.spawnerPotentials.get(index);

        FlowLayout card = potentialCard(context, special, draft, index);
        if (draft.uiCollapsed) {
            card.child(UiFactory.muted(Component.literal(potentialSummary(draft))));
            return card;
        }

        card.child(UiFactory.field(
                ItemEditorText.tr("special.spawner.potential_weight"),
                Component.empty(),
                UiFactory.textBox(
                        draft.weight,
                        context.bindText(value -> draft.weight = value)
                ).horizontalSizing(isCompactLayout(context) ? Sizing.fill(100) : UiFactory.fixed(POTENTIAL_WEIGHT_FIELD_WIDTH))
        ));

        addSpawnDataFields(context, entityIds, draft.spawnData, card);

        return card;
    }

    private static String potentialSummary(ItemEditorState.SpawnerPotentialDraft draft) {
        String entityId = draft.spawnData.entity.entityId == null || draft.spawnData.entity.entityId.isBlank()
                ? "?"
                : draft.spawnData.entity.entityId;
        String weight = draft.weight == null || draft.weight.isBlank() ? "1" : draft.weight;
        return entityId + " | " + ItemEditorText.str("special.spawner.potential_weight") + " " + weight;
    }

    private static FlowLayout buildSpawnDataCard(
            SpecialDataPanelContext context,
            List<String> entityIds,
            ItemEditorState.SpawnerSpawnDataDraft draft,
            Component title
    ) {
        FlowLayout card = UiFactory.subCard();
        card.child(UiFactory.title(title).shadow(false));
        addSpawnDataFields(context, entityIds, draft, card);
        return card;
    }

    private static void addSpawnDataFields(
            SpecialDataPanelContext context,
            List<String> entityIds,
            ItemEditorState.SpawnerSpawnDataDraft draft,
            FlowLayout card
    ) {
        card.child(UiFactory.field(
                ItemEditorText.tr("special.spawner.entity_id"),
                Component.empty(),
                buildEntityInput(
                        context,
                        entityIds,
                        draft.entity.entityId,
                        value -> draft.entity.entityId = value,
                        () -> draft.entity.entityId
                )
        ));
        card.child(EntitySpawnDataUi.nameEditor(
                context,
                draft.entity,
                ItemEditorText.tr("special.spawn_egg.name"),
                "special.spawner.name.placeholder",
                "special.spawner.name.color_title",
                "special.spawner.name.gradient_title"
        ));
        card.child(EntitySpawnDataUi.flags(context, draft.entity, flagColumns(context)));
        card.child(EntitySpawnDataUi.health(context, draft.entity, isCompactLayout(context)));
        card.child(buildCustomSpawnRules(context, draft));
    }

    private static FlowLayout buildCustomSpawnRules(
            SpecialDataPanelContext context,
            ItemEditorState.SpawnerSpawnDataDraft draft
    ) {
        FlowLayout group = UiFactory.column().gap(2);
        group.child(UiFactory.muted(ItemEditorText.tr("special.spawner.custom_spawn_rules")));
        boolean compactLayout = isCompactLayout(context);
        addPackedRows(
                group,
                compactLayout ? 2 : 4,
                lightField(context, ItemEditorText.tr("special.spawner.block_light_min"), draft.blockLightMin, value -> draft.blockLightMin = value),
                lightField(context, ItemEditorText.tr("special.spawner.block_light_max"), draft.blockLightMax, value -> draft.blockLightMax = value),
                lightField(context, ItemEditorText.tr("special.spawner.sky_light_min"), draft.skyLightMin, value -> draft.skyLightMin = value),
                lightField(context, ItemEditorText.tr("special.spawner.sky_light_max"), draft.skyLightMax, value -> draft.skyLightMax = value)
        );
        return group;
    }

    private static UIComponent lightField(
            SpecialDataPanelContext context,
            Component label,
            String value,
            Consumer<String> setter
    ) {
        return UiFactory.field(
                label,
                Component.empty(),
                UiFactory.textBox(value, context.bindText(setter))
                        .horizontalSizing(isCompactLayout(context) ? Sizing.fill(100) : UiFactory.fixed(LIGHT_FIELD_WIDTH))
        );
    }

    private static UIComponent buildIntField(
            SpecialDataPanelContext context,
            Component label,
            String value,
            Consumer<String> setter
    ) {
        return UiFactory.field(
                label,
                Component.empty(),
                UiFactory.textBox(value, context.bindText(setter))
                        .horizontalSizing(UiFactory.fixed(INT_FIELD_WIDTH))
        ).horizontalSizing(Sizing.fill(100));
    }

    private static FlowLayout potentialCard(
            SpecialDataPanelContext context,
            ItemEditorState.SpecialData special,
            ItemEditorState.SpawnerPotentialDraft draft,
            int currentIndex
    ) {
        FlowLayout card = UiFactory.subCard();
        FlowLayout titleRow = UiFactory.row();
        titleRow.child(UiFactory.title(ItemEditorText.tr("special.spawner.potential", currentIndex + 1))
                .shadow(false)
                .horizontalSizing(Sizing.expand(100)));
        titleRow.child(UiFactory.collapseToggleButton(
                draft.uiCollapsed,
                () -> context.mutateRefresh(() -> draft.uiCollapsed = !draft.uiCollapsed)
        ));
        card.child(titleRow);

        ButtonComponent upButton = UiFactory.button(ItemEditorText.tr("common.up"), UiFactory.ButtonTextPreset.COMPACT, button ->
                context.mutateRefresh(() -> movePotential(special.spawnerPotentials, currentIndex, currentIndex - 1))
        );
        upButton.active(currentIndex > 0);
        ButtonComponent downButton = UiFactory.button(ItemEditorText.tr("common.down"), UiFactory.ButtonTextPreset.COMPACT, button ->
                context.mutateRefresh(() -> movePotential(special.spawnerPotentials, currentIndex, currentIndex + 1))
        );
        downButton.active(currentIndex < special.spawnerPotentials.size() - 1);
        ButtonComponent duplicateButton = UiFactory.button(Component.literal(ACTION_DUPLICATE), UiFactory.ButtonTextPreset.COMPACT, button ->
                context.mutateRefresh(() -> special.spawnerPotentials.add(currentIndex + 1, copyPotential(draft)))
        );
        ButtonComponent removeButton = UiFactory.negativeButton(ItemEditorText.tr("common.remove"), UiFactory.ButtonTextPreset.COMPACT, button ->
                context.mutateRefresh(() -> special.spawnerPotentials.remove(currentIndex))
        );
        card.child(UiFactory.actionButtonRow(upButton, downButton, duplicateButton, removeButton));
        return card;
    }

    private static void movePotential(List<ItemEditorState.SpawnerPotentialDraft> drafts, int from, int to) {
        if (from < 0 || from >= drafts.size() || to < 0 || to >= drafts.size() || from == to) {
            return;
        }
        ItemEditorState.SpawnerPotentialDraft moved = drafts.remove(from);
        drafts.add(to, moved);
    }

    private static void copySpawnData(
            ItemEditorState.SpawnerSpawnDataDraft source,
            ItemEditorState.SpawnerSpawnDataDraft target
    ) {
        target.entity.entityId = source.entity.entityId;
        target.entity.noAi = source.entity.noAi;
        target.entity.silent = source.entity.silent;
        target.entity.noGravity = source.entity.noGravity;
        target.entity.glowing = source.entity.glowing;
        target.entity.invulnerable = source.entity.invulnerable;
        target.entity.persistenceRequired = source.entity.persistenceRequired;
        target.entity.customNameVisible = source.entity.customNameVisible;
        target.entity.customName = source.entity.customName;
        target.entity.health = source.entity.health;
        target.entity.originalEntityTag = source.entity.originalEntityTag.copy();
        target.blockLightMin = source.blockLightMin;
        target.blockLightMax = source.blockLightMax;
        target.skyLightMin = source.skyLightMin;
        target.skyLightMax = source.skyLightMax;
        target.originalDataTag = source.originalDataTag.copy();
    }

    private static ItemEditorState.SpawnerPotentialDraft copyPotential(ItemEditorState.SpawnerPotentialDraft source) {
        ItemEditorState.SpawnerPotentialDraft copy = new ItemEditorState.SpawnerPotentialDraft();
        copy.weight = source.weight;
        copy.uiCollapsed = source.uiCollapsed;
        copySpawnData(source.spawnData, copy.spawnData);
        return copy;
    }

    private static void resetPotentialsFromSpawnData(ItemEditorState.SpecialData special) {
        special.spawnerPotentials.clear();
        ItemEditorState.SpawnerPotentialDraft draft = new ItemEditorState.SpawnerPotentialDraft();
        copySpawnData(special.spawnerSpawnData, draft.spawnData);
        special.spawnerPotentials.add(draft);
    }

    private static FlowLayout buildEntityInput(
            SpecialDataPanelContext context,
            List<String> entityIds,
            String rawInput,
            Consumer<String> setter,
            Supplier<String> currentValueSupplier
    ) {
        FlowLayout input = UiFactory.column().gap(2);
        boolean compactLayout = isCompactLayout(context);

        FlowLayout inputRow = compactLayout ? UiFactory.column() : UiFactory.row();
        inputRow.child(UiFactory.textBox(
                rawInput,
                value -> context.mutateRefresh(() -> setter.accept(IdFieldNormalizer.normalize(value)))
        ));
        inputRow.child(buildPickerButton(context, entityIds, currentValueSupplier, setter, compactLayout));
        input.child(inputRow);

        return input;
    }

    private static ButtonComponent buildPickerButton(
            SpecialDataPanelContext context,
            List<String> entityIds,
            Supplier<String> currentValueSupplier,
            Consumer<String> setter,
            boolean compactLayout
    ) {
        ButtonComponent button = UiFactory.button(ItemEditorText.tr("common.pick"), UiFactory.ButtonTextPreset.STANDARD,  component ->
                context.openSearchablePicker(
                        ItemEditorText.str("special.spawner.entity_picker_title"),
                        "",
                        entityIds,
                        id -> id,
                        id -> context.mutateRefresh(() -> setter.accept(id))
                )
        );
        button.horizontalSizing(compactLayout ? Sizing.fill(100) : UiFactory.fixed(PICKER_BUTTON_WIDTH));
        String current = currentValueSupplier.get();
        if (current != null && !current.isBlank()) {
            button.tooltip(List.of(Component.literal(current)));
        }
        return button;
    }

    private static boolean isCompactLayout(SpecialDataPanelContext context) {
        return LayoutModeUtil.isCompactPanel(context.guiScale(), context.panelWidthHint(), COMPACT_LAYOUT_WIDTH_THRESHOLD);
    }

    private static int flagColumns(SpecialDataPanelContext context) {
        if (isCompactLayout(context)) {
            return 2;
        }
        int width = context.panelWidthHint();
        if (width >= 760) {
            return 7;
        }
        if (width >= 560) {
            return 5;
        }
        return width >= 420 ? 4 : 3;
    }

    private static void addPackedRows(FlowLayout parent, int perRow, UIComponent... components) {
        for (int index = 0; index < components.length; index += perRow) {
            FlowLayout row = UiFactory.row();
            int rowEnd = Math.min(components.length, index + perRow);
            int rowSize = rowEnd - index;
            int width = Math.max(1, (100 - rowSize) / Math.max(1, rowSize));
            for (int componentIndex = index; componentIndex < rowEnd; componentIndex++) {
                row.child(components[componentIndex].horizontalSizing(Sizing.fill(width)));
            }
            parent.child(row);
        }
    }

}
