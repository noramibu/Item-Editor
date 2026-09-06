package me.noramibu.itemeditor.ui.panel.specialdata;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.UIComponent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import me.noramibu.itemeditor.editor.EditorCategory;
import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.service.EntitySpawnDataUtil;
import me.noramibu.itemeditor.service.EntityTagFields;
import me.noramibu.itemeditor.ui.component.EditorSearchDialog;
import me.noramibu.itemeditor.ui.component.PickerFieldFactory;
import me.noramibu.itemeditor.ui.component.UiFactory;
import me.noramibu.itemeditor.util.IdFieldNormalizer;
import me.noramibu.itemeditor.util.ItemEditorCapabilities;
import me.noramibu.itemeditor.util.ItemEditorText;
import me.noramibu.itemeditor.util.RegistryUtil;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;

public final class SpawnerSpecialDataSection {

    private static final int COMPACT_LAYOUT_WIDTH_THRESHOLD = 560;
    private static final int INT_FIELD_WIDTH = 100;
    private static final int POTENTIAL_WEIGHT_FIELD_WIDTH = 120;
    private static final int LIGHT_FIELD_WIDTH = 74;
    private static final int PICKER_BUTTON_WIDTH = 70;

    private SpawnerSpecialDataSection() {}

    public static boolean supports(ItemStack stack) {
        return ItemEditorCapabilities.supportsSpawnerData(stack);
    }

    public static List<EditorSearchDialog.Target> searchTargets(SpecialDataPanelContext context) {
        var result = new ArrayList<>(SpecialDataSearch.targets(
                context, EditorCategory.SPECIAL_DATA, "special.spawner.title", "spawner", () -> {}, Field.values()));
        var special = context.special();
        if (special.spawnerUsePotentials) {
            result.addAll(SpecialDataSearch.targets(
                    context,
                    EditorCategory.SPECIAL_DATA,
                    "special.spawner.title",
                    "spawner",
                    () -> {},
                    PotentialListField.values()));
        }
        for (int index = -1; index < special.spawnerPotentials.size(); index++) {
            var potential = index < 0 ? null : special.spawnerPotentials.get(index);
            var draft = potential == null ? special.spawnerSpawnData : potential.spawnData;
            String title = potential == null
                    ? ItemEditorText.str("special.spawner.spawn_entity")
                    : ItemEditorText.str("special.spawner.potential", index + 1);
            String scope = SpecialDataSearch.scope("spawner-entry", draft);
            Runnable expand = () -> {
                if (potential != null) {
                    special.spawnerUsePotentials = true;
                    potential.uiCollapsed = false;
                }
            };
            var path = List.of(ItemEditorText.str("special.spawner.title"), title);
            result.addAll(SpecialDataSearch.targets(
                    context, EditorCategory.SPECIAL_DATA, path, scope, expand, SpawnField.values()));
            if (potential != null) {
                result.addAll(SpecialDataSearch.targets(
                        context, EditorCategory.SPECIAL_DATA, path, scope, expand, PotentialField.values()));
            }
            result.addAll(
                    EntityTagFieldsUi.searchTargets(context, draft.entity, String.join(" > ", path), scope, expand));
            result.addAll(EntitySpawnDataUi.searchTargets(context, draft.entity, path, scope, expand));
        }
        return result;
    }

    public static FlowLayout build(SpecialDataPanelContext context) {
        ItemEditorState.SpecialData special = context.special();
        Registry<EntityType<?>> entityRegistry =
                context.screen().session().registryAccess().lookupOrThrow(Registries.ENTITY_TYPE);
        List<String> entityIds = RegistryUtil.ids(entityRegistry);
        boolean compactLayout = isCompactLayout(context);

        FlowLayout section = UiFactory.section(ItemEditorText.tr("special.spawner.title"), Component.empty());
        section.id("spawner");
        section.child(buildSpawnDataCard(
                context, entityIds, special.spawnerSpawnData, ItemEditorText.tr("special.spawner.spawn_entity")));

        UiFactory.addPackedRows(
                section,
                compactLayout ? 2 : 3,
                buildIntField(context, Field.DELAY.text(), special.spawnerDelay, value -> special.spawnerDelay = value),
                buildIntField(
                        context,
                        Field.MIN_SPAWN_DELAY.text(),
                        special.spawnerMinSpawnDelay,
                        value -> special.spawnerMinSpawnDelay = value),
                buildIntField(
                        context,
                        Field.MAX_SPAWN_DELAY.text(),
                        special.spawnerMaxSpawnDelay,
                        value -> special.spawnerMaxSpawnDelay = value));
        UiFactory.addPackedRows(
                section,
                compactLayout ? 2 : 4,
                buildIntField(
                        context,
                        Field.SPAWN_COUNT.text(),
                        special.spawnerSpawnCount,
                        value -> special.spawnerSpawnCount = value),
                buildIntField(
                        context,
                        Field.MAX_NEARBY_ENTITIES.text(),
                        special.spawnerMaxNearbyEntities,
                        value -> special.spawnerMaxNearbyEntities = value),
                buildIntField(
                        context,
                        Field.REQUIRED_PLAYER_RANGE.text(),
                        special.spawnerRequiredPlayerRange,
                        value -> special.spawnerRequiredPlayerRange = value),
                buildIntField(
                        context,
                        Field.SPAWN_RANGE.text(),
                        special.spawnerSpawnRange,
                        value -> special.spawnerSpawnRange = value));

        section.child(UiFactory.checkbox(
                Field.USE_POTENTIALS.text(),
                special.spawnerUsePotentials,
                value -> context.mutateRefresh(() -> {
                    special.spawnerUsePotentials = value;
                    if (value && special.spawnerPotentials.isEmpty()) {
                        ItemEditorState.SpawnerPotentialDraft draft = new ItemEditorState.SpawnerPotentialDraft();
                        copySpawnData(special.spawnerSpawnData, draft.spawnData);
                        special.spawnerPotentials.add(draft);
                    }
                })));

        if (!special.spawnerUsePotentials) {
            return section;
        }

        section.child(UiFactory.actionButtonRow(
                UiFactory.button(
                        PotentialListField.ADD_POTENTIAL.text(),
                        UiFactory.ButtonTextPreset.STANDARD,
                        button -> context.mutateRefresh(
                                () -> special.spawnerPotentials.add(new ItemEditorState.SpawnerPotentialDraft()))),
                UiFactory.button(
                        PotentialListField.RESET.text(),
                        UiFactory.ButtonTextPreset.STANDARD,
                        button -> context.mutateRefresh(() -> resetPotentialsFromSpawnData(special)))));

        if (!special.spawnerPotentials.isEmpty()) {
            section.child(UiFactory.actionButtonRow(
                    UiFactory.button(
                            PotentialListField.EXPAND_ALL.text(),
                            UiFactory.ButtonTextPreset.STANDARD,
                            button -> context.mutateRefresh(() ->
                                    special.spawnerPotentials.forEach(potential -> potential.uiCollapsed = false))),
                    UiFactory.button(
                            PotentialListField.COLLAPSE_ALL.text(),
                            UiFactory.ButtonTextPreset.STANDARD,
                            button -> context.mutateRefresh(() ->
                                    special.spawnerPotentials.forEach(potential -> potential.uiCollapsed = true)))));
        }

        for (int index = 0; index < special.spawnerPotentials.size(); index++) {
            section.child(buildPotentialCard(context, entityIds, special, index));
        }

        return section;
    }

    private static FlowLayout buildPotentialCard(
            SpecialDataPanelContext context, List<String> entityIds, ItemEditorState.SpecialData special, int index) {
        ItemEditorState.SpawnerPotentialDraft draft = special.spawnerPotentials.get(index);

        FlowLayout card = potentialCard(context, special, draft, index);
        card.id(SpecialDataSearch.scope("spawner-entry", draft.spawnData));
        if (draft.uiCollapsed) {
            card.child(UiFactory.muted(Component.literal(potentialSummary(draft))));
            return card;
        }

        card.child(UiFactory.field(
                PotentialField.POTENTIAL_WEIGHT.text(),
                Component.empty(),
                UiFactory.textBox(draft.weight, context.bindText(value -> draft.weight = value))
                        .horizontalSizing(
                                isCompactLayout(context)
                                        ? Sizing.fill(100)
                                        : UiFactory.fixed(POTENTIAL_WEIGHT_FIELD_WIDTH))));

        addSpawnDataFields(context, entityIds, draft.spawnData, card);

        return card;
    }

    private static String potentialSummary(ItemEditorState.SpawnerPotentialDraft draft) {
        String entityId = draft.spawnData.entity.entityId == null || draft.spawnData.entity.entityId.isBlank()
                ? "?"
                : draft.spawnData.entity.entityId;
        String weight = draft.weight == null || draft.weight.isBlank() ? "1" : draft.weight;
        return entityId + " | " + PotentialField.POTENTIAL_WEIGHT.text().getString() + " " + weight;
    }

    private static FlowLayout buildSpawnDataCard(
            SpecialDataPanelContext context,
            List<String> entityIds,
            ItemEditorState.SpawnerSpawnDataDraft draft,
            Component title) {
        FlowLayout card = UiFactory.subCard();
        card.child(UiFactory.title(title).shadow(false));
        card.id(SpecialDataSearch.scope("spawner-entry", draft));
        addSpawnDataFields(context, entityIds, draft, card);
        return card;
    }

    private static void addSpawnDataFields(
            SpecialDataPanelContext context,
            List<String> entityIds,
            ItemEditorState.SpawnerSpawnDataDraft draft,
            FlowLayout card) {
        card.child(PickerFieldFactory.searchableTextField(
                context,
                SpawnField.ENTITY_ID.text(),
                draft.entity.entityId,
                value -> updateEntityId(context, draft.entity, value, entityIds),
                PICKER_BUTTON_WIDTH,
                ItemEditorText.str("special.spawner.entity_picker_title"),
                "",
                entityIds,
                id -> id,
                id -> context.mutateRefresh(() -> draft.entity.entityId = id)));
        card.child(EntitySpawnDataUi.nameEditor(
                context,
                draft.entity,
                SpawnField.CUSTOM_NAME.text(),
                "special.spawner.name.placeholder",
                "special.spawner.name.color_title",
                "special.spawner.name.gradient_title"));
        EntityTagFieldsUi tags = new EntityTagFieldsUi(context, draft.entity);
        card.child(EntitySpawnDataUi.flags(context, draft.entity, tags));
        card.child(EntitySpawnDataUi.details(context, draft.entity, tags));
        card.child(tags.build());
        card.child(buildCustomSpawnRules(context, draft));
    }

    private static void updateEntityId(
            SpecialDataPanelContext context,
            ItemEditorState.EntitySpawnDraft draft,
            String value,
            List<String> entityIds) {
        String previousType = entityEditorType(draft.entityId);
        boolean previousValid = entityIds.contains(draft.entityId);
        var previousFields = EntityTagFields.groups(draft.entityId);
        draft.entityId = IdFieldNormalizer.normalize(value);
        if (!entityIds.contains(draft.entityId)) return;
        if (!previousValid
                || !Objects.equals(previousType, entityEditorType(draft.entityId))
                || !previousFields.equals(EntityTagFields.groups(draft.entityId))) {
            context.screen().refreshCurrentPanel();
        }
    }

    private static String entityEditorType(String entityId) {
        return EntitySpawnDataUtil.isItemEntity(entityId) ? "dropped_item" : EntitySpawnDataUtil.displayType(entityId);
    }

    private static FlowLayout buildCustomSpawnRules(
            SpecialDataPanelContext context, ItemEditorState.SpawnerSpawnDataDraft draft) {
        FlowLayout group = UiFactory.column().gap(2);
        group.child(UiFactory.muted(ItemEditorText.tr("special.spawner.custom_spawn_rules")));
        boolean compactLayout = isCompactLayout(context);
        UiFactory.addPackedRows(
                group,
                compactLayout ? 2 : 4,
                lightField(
                        context,
                        SpawnField.BLOCK_LIGHT_MIN.text(),
                        draft.blockLightMin,
                        value -> draft.blockLightMin = value),
                lightField(
                        context,
                        SpawnField.BLOCK_LIGHT_MAX.text(),
                        draft.blockLightMax,
                        value -> draft.blockLightMax = value),
                lightField(
                        context,
                        SpawnField.SKY_LIGHT_MIN.text(),
                        draft.skyLightMin,
                        value -> draft.skyLightMin = value),
                lightField(
                        context,
                        SpawnField.SKY_LIGHT_MAX.text(),
                        draft.skyLightMax,
                        value -> draft.skyLightMax = value));
        return group;
    }

    private static UIComponent lightField(
            SpecialDataPanelContext context, Component label, String value, Consumer<String> setter) {
        return UiFactory.field(
                label,
                Component.empty(),
                UiFactory.textBox(value, context.bindText(setter))
                        .horizontalSizing(
                                isCompactLayout(context) ? Sizing.fill(100) : UiFactory.fixed(LIGHT_FIELD_WIDTH)));
    }

    private static UIComponent buildIntField(
            SpecialDataPanelContext context, Component label, String value, Consumer<String> setter) {
        return UiFactory.field(
                        label,
                        Component.empty(),
                        UiFactory.textBox(value, context.bindText(setter))
                                .horizontalSizing(UiFactory.fixed(INT_FIELD_WIDTH)))
                .horizontalSizing(Sizing.fill(100));
    }

    private static FlowLayout potentialCard(
            SpecialDataPanelContext context,
            ItemEditorState.SpecialData special,
            ItemEditorState.SpawnerPotentialDraft draft,
            int currentIndex) {
        FlowLayout card = UiFactory.subCard();
        FlowLayout titleRow = UiFactory.row();
        titleRow.child(UiFactory.title(ItemEditorText.tr("special.spawner.potential", currentIndex + 1))
                .shadow(false)
                .horizontalSizing(Sizing.expand(100)));
        titleRow.child(UiFactory.collapseToggleButton(
                draft.uiCollapsed, () -> context.mutateRefresh(() -> draft.uiCollapsed = !draft.uiCollapsed)));
        card.child(titleRow);

        ButtonComponent upButton = UiFactory.button(
                PotentialField.UP.text(),
                UiFactory.ButtonTextPreset.COMPACT,
                button -> context.mutateRefresh(
                        () -> movePotential(special.spawnerPotentials, currentIndex, currentIndex - 1)));
        upButton.active(currentIndex > 0);
        ButtonComponent downButton = UiFactory.button(
                PotentialField.DOWN.text(),
                UiFactory.ButtonTextPreset.COMPACT,
                button -> context.mutateRefresh(
                        () -> movePotential(special.spawnerPotentials, currentIndex, currentIndex + 1)));
        downButton.active(currentIndex < special.spawnerPotentials.size() - 1);
        card.child(UiFactory.actionButtonRow(
                upButton,
                downButton,
                UiFactory.button(
                        PotentialField.DUPLICATE.text(),
                        UiFactory.ButtonTextPreset.COMPACT,
                        button -> context.mutateRefresh(
                                () -> special.spawnerPotentials.add(currentIndex + 1, copyPotential(draft)))),
                UiFactory.negativeButton(
                        PotentialField.REMOVE.text(),
                        UiFactory.ButtonTextPreset.COMPACT,
                        button -> context.mutateRefresh(() -> special.spawnerPotentials.remove(currentIndex)))));
        return card;
    }

    private static void movePotential(List<ItemEditorState.SpawnerPotentialDraft> drafts, int from, int to) {
        if (from < 0 || from >= drafts.size() || to < 0 || to >= drafts.size() || from == to) {
            return;
        }
        ItemEditorState.SpawnerPotentialDraft moved = drafts.remove(from);
        drafts.add(to, moved);
    }

    static void copySpawnData(
            ItemEditorState.SpawnerSpawnDataDraft source, ItemEditorState.SpawnerSpawnDataDraft target) {
        target.entity.entityId = source.entity.entityId;
        target.entity.entityTagEdits.clear();
        target.entity.entityTagEdits.putAll(source.entity.entityTagEdits);
        target.entity.uiExpandedTagGroups.clear();
        target.entity.uiExpandedTagGroups.addAll(source.entity.uiExpandedTagGroups);
        target.entity.noAi = source.entity.noAi;
        target.entity.silent = source.entity.silent;
        target.entity.noGravity = source.entity.noGravity;
        target.entity.glowing = source.entity.glowing;
        target.entity.invulnerable = source.entity.invulnerable;
        target.entity.persistenceRequired = source.entity.persistenceRequired;
        target.entity.customNameVisible = source.entity.customNameVisible;
        target.entity.customName = source.entity.customName;
        target.entity.health = source.entity.health;
        target.entity.itemEntityStack =
                source.entity.itemEntityStack == null ? ItemStack.EMPTY : source.entity.itemEntityStack.copy();
        target.entity.itemEntityCount = source.entity.itemEntityCount;
        target.entity.itemEntityAge = source.entity.itemEntityAge;
        target.entity.itemEntityPickupDelay = source.entity.itemEntityPickupDelay;
        target.entity.itemEntityOwner = source.entity.itemEntityOwner;
        target.entity.itemEntityThrower = source.entity.itemEntityThrower;
        target.entity.displayValues.clear();
        target.entity.displayValues.putAll(source.entity.displayValues);
        target.entity.displayItemStack =
                source.entity.displayItemStack == null ? ItemStack.EMPTY : source.entity.displayItemStack.copy();
        target.entity.uiDisplayTransformCollapsed = source.entity.uiDisplayTransformCollapsed;
        target.entity.uiDisplayRenderingCollapsed = source.entity.uiDisplayRenderingCollapsed;
        target.entity.attributes.clear();
        source.entity.attributes.forEach(attribute -> target.entity.attributes.add(attribute.copy()));
        target.entity.uiAttributesCollapsed = source.entity.uiAttributesCollapsed;
        target.entity.effects.clear();
        source.entity.effects.forEach(effect -> target.entity.effects.add(effect.copy()));
        target.entity.uiEffectsCollapsed = source.entity.uiEffectsCollapsed;
        target.entity.equipment.copyFrom(source.entity.equipment);
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

    private static boolean isCompactLayout(SpecialDataPanelContext context) {
        return context.isCompactPanel(COMPACT_LAYOUT_WIDTH_THRESHOLD);
    }

    private enum Field implements SpecialDataSearch.Field {
        DELAY("special.spawner.delay"),
        MIN_SPAWN_DELAY("special.spawner.min_spawn_delay"),
        MAX_SPAWN_DELAY("special.spawner.max_spawn_delay"),
        SPAWN_COUNT("special.spawner.spawn_count"),
        MAX_NEARBY_ENTITIES("special.spawner.max_nearby_entities"),
        REQUIRED_PLAYER_RANGE("special.spawner.required_player_range"),
        SPAWN_RANGE("special.spawner.spawn_range"),
        USE_POTENTIALS("special.spawner.use_potentials");

        private final String key;

        Field(String key) {
            this.key = key;
        }

        public String key() {
            return key;
        }
    }

    private enum SpawnField implements SpecialDataSearch.Field {
        ENTITY_ID("common.entity_id"),
        CUSTOM_NAME("common.custom_name"),
        BLOCK_LIGHT_MIN("special.spawner.block_light_min"),
        BLOCK_LIGHT_MAX("special.spawner.block_light_max"),
        SKY_LIGHT_MIN("special.spawner.sky_light_min"),
        SKY_LIGHT_MAX("special.spawner.sky_light_max");

        private final String key;

        SpawnField(String key) {
            this.key = key;
        }

        public String key() {
            return key;
        }
    }

    private enum PotentialListField implements SpecialDataSearch.Field {
        ADD_POTENTIAL("special.spawner.add_potential"),
        RESET("common.reset"),
        EXPAND_ALL("common.expand_all"),
        COLLAPSE_ALL("common.collapse_all");

        private final String key;

        PotentialListField(String key) {
            this.key = key;
        }

        public String key() {
            return key;
        }
    }

    private enum PotentialField implements SpecialDataSearch.Field {
        POTENTIAL_WEIGHT("special.spawner.potential_weight"),
        UP("common.up"),
        DOWN("common.down"),
        DUPLICATE("common.duplicate"),
        REMOVE("common.remove");

        private final String key;

        PotentialField(String key) {
            this.key = key;
        }

        public String key() {
            return key;
        }
    }
}
