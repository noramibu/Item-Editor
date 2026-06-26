package me.noramibu.itemeditor.service;

import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.editor.ValidationMessage;
import me.noramibu.itemeditor.util.ItemEditorText;
import me.noramibu.itemeditor.util.ValidationUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.List;
import java.util.Objects;

final class SpawnerSpecialDataApplier extends AbstractPreviewApplierSupport implements SpecialDataApplier {

    private static final int MIN_SPAWNER_VALUE = 0;
    private static final int MAX_SPAWNER_VALUE = Short.MAX_VALUE;
    private static final int MIN_DELAY_VALUE = -1;
    private static final int MIN_LIGHT_VALUE = 0;
    private static final int MAX_LIGHT_VALUE = 15;

    @Override
    public void apply(SpecialDataApplyContext context) {
        BlockEntityType<?> spawnerType = this.resolveSpawnerType(context);
        if (spawnerType == null) {
            return;
        }

        if (this.sameSpawnerData(context.special(), context.baselineSpecial())) {
            this.restoreOriginalComponent(context.originalStack(), context.previewStack(), DataComponents.BLOCK_ENTITY_DATA);
            return;
        }

        if (this.isSpawnerDataDefault(context.special())) {
            this.clearToPrototype(context.previewStack(), DataComponents.BLOCK_ENTITY_DATA);
            return;
        }

        CompoundTag blockTag = new CompoundTag();
        TypedEntityData<BlockEntityType<?>> originalData = context.originalStack().get(DataComponents.BLOCK_ENTITY_DATA);
        if (originalData != null && originalData.type() == BlockEntityType.MOB_SPAWNER) {
            blockTag = originalData.copyTagWithoutId();
        }

        this.applyPrimaryEntity(blockTag, context);
        this.applySpawnPotentials(blockTag, context);
        this.putOptionalIntTag(
                blockTag,
                "Delay",
                context.special().spawnerDelay,
                ItemEditorText.str("special.spawner.delay"),
                MIN_DELAY_VALUE,
                MAX_SPAWNER_VALUE,
                context.messages()
        );
        this.putOptionalIntTag(
                blockTag,
                "MinSpawnDelay",
                context.special().spawnerMinSpawnDelay,
                ItemEditorText.str("special.spawner.min_spawn_delay"),
                MIN_SPAWNER_VALUE,
                MAX_SPAWNER_VALUE,
                context.messages()
        );
        this.putOptionalIntTag(
                blockTag,
                "MaxSpawnDelay",
                context.special().spawnerMaxSpawnDelay,
                ItemEditorText.str("special.spawner.max_spawn_delay"),
                MIN_SPAWNER_VALUE,
                MAX_SPAWNER_VALUE,
                context.messages()
        );
        this.putOptionalIntTag(
                blockTag,
                "SpawnCount",
                context.special().spawnerSpawnCount,
                ItemEditorText.str("special.spawner.spawn_count"),
                MIN_SPAWNER_VALUE,
                MAX_SPAWNER_VALUE,
                context.messages()
        );
        this.putOptionalIntTag(
                blockTag,
                "MaxNearbyEntities",
                context.special().spawnerMaxNearbyEntities,
                ItemEditorText.str("special.spawner.max_nearby_entities"),
                MIN_SPAWNER_VALUE,
                MAX_SPAWNER_VALUE,
                context.messages()
        );
        this.putOptionalIntTag(
                blockTag,
                "RequiredPlayerRange",
                context.special().spawnerRequiredPlayerRange,
                ItemEditorText.str("special.spawner.required_player_range"),
                MIN_SPAWNER_VALUE,
                MAX_SPAWNER_VALUE,
                context.messages()
        );
        this.putOptionalIntTag(
                blockTag,
                "SpawnRange",
                context.special().spawnerSpawnRange,
                ItemEditorText.str("special.spawner.spawn_range"),
                MIN_SPAWNER_VALUE,
                MAX_SPAWNER_VALUE,
                context.messages()
        );

        if (blockTag.isEmpty()) {
            this.clearToPrototype(context.previewStack(), DataComponents.BLOCK_ENTITY_DATA);
            return;
        }
        context.previewStack().set(DataComponents.BLOCK_ENTITY_DATA, TypedEntityData.of(spawnerType, blockTag));
    }

    private void applyPrimaryEntity(CompoundTag blockTag, SpecialDataApplyContext context) {
        CompoundTag spawnDataTag = this.applySpawnData(
                context.special().spawnerSpawnData,
                context,
                ItemEditorText.str("special.spawner.spawn_entity")
        );
        if (spawnDataTag == null) {
            blockTag.remove("SpawnData");
            return;
        }
        blockTag.put("SpawnData", spawnDataTag);
    }

    private void applySpawnPotentials(CompoundTag blockTag, SpecialDataApplyContext context) {
        ItemEditorState.SpecialData special = context.special();
        if (!special.spawnerUsePotentials) {
            blockTag.remove("SpawnPotentials");
            return;
        }

        ListTag potentialsTag = new ListTag();
        for (ItemEditorState.SpawnerPotentialDraft draft : special.spawnerPotentials) {
            CompoundTag dataTag = this.applySpawnData(
                    draft.spawnData,
                    context,
                    ItemEditorText.str("special.spawner.entity_id")
            );
            if (dataTag == null) {
                continue;
            }

            Integer weight = ValidationUtil.parseInt(
                    draft.weight,
                    ItemEditorText.str("special.spawner.potential_weight"),
                    1,
                    MAX_SPAWNER_VALUE,
                    context.messages()
            );
            if (weight == null) {
                continue;
            }

            CompoundTag potentialTag = new CompoundTag();
            potentialTag.putInt("weight", weight);
            potentialTag.put("data", dataTag);
            potentialsTag.add(potentialTag);
        }

        if (potentialsTag.isEmpty()) {
            blockTag.remove("SpawnPotentials");
        } else {
            blockTag.put("SpawnPotentials", potentialsTag);
        }
    }

    private CompoundTag applySpawnData(
            ItemEditorState.SpawnerSpawnDataDraft draft,
            SpecialDataApplyContext context,
            String fieldLabel
    ) {
        CompoundTag entityTag = EntitySpawnDataUtil.applyEntity(draft.entity, context, fieldLabel);
        boolean hasRules = hasCustomSpawnRules(draft);
        if (entityTag == null && !hasRules) {
            return null;
        }
        if (entityTag == null) {
            return null;
        }

        CompoundTag spawnDataTag = draft.originalDataTag.copy();
        spawnDataTag.put("entity", entityTag);
        this.applyCustomSpawnRules(draft, spawnDataTag, context.messages());
        return spawnDataTag;
    }

    private static boolean hasCustomSpawnRules(ItemEditorState.SpawnerSpawnDataDraft draft) {
        return !blank(draft.blockLightMin)
                || !blank(draft.blockLightMax)
                || !blank(draft.skyLightMin)
                || !blank(draft.skyLightMax);
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private void applyCustomSpawnRules(
            ItemEditorState.SpawnerSpawnDataDraft draft,
            CompoundTag spawnDataTag,
            List<ValidationMessage> messages
    ) {
        CompoundTag rulesTag = spawnDataTag.getCompoundOrEmpty("custom_spawn_rules");
        boolean hasBlock = this.putLightLimit(
                rulesTag,
                "block_light_limit",
                draft.blockLightMin,
                draft.blockLightMax,
                ItemEditorText.str("special.spawner.block_light_limit"),
                messages
        );
        boolean hasSky = this.putLightLimit(
                rulesTag,
                "sky_light_limit",
                draft.skyLightMin,
                draft.skyLightMax,
                ItemEditorText.str("special.spawner.sky_light_limit"),
                messages
        );
        if (hasBlock || hasSky) {
            spawnDataTag.put("custom_spawn_rules", rulesTag);
            return;
        }
        spawnDataTag.remove("custom_spawn_rules");
    }

    private boolean putLightLimit(
            CompoundTag rulesTag,
            String key,
            String rawMin,
            String rawMax,
            String label,
            List<ValidationMessage> messages
    ) {
        String minText = rawMin == null ? "" : rawMin.trim();
        String maxText = rawMax == null ? "" : rawMax.trim();
        if (minText.isBlank() && maxText.isBlank()) {
            rulesTag.remove(key);
            return false;
        }

        Integer min = ValidationUtil.parseInt(minText, label, MIN_LIGHT_VALUE, MAX_LIGHT_VALUE, messages);
        Integer max = maxText.isBlank()
                ? min
                : ValidationUtil.parseInt(maxText, label, MIN_LIGHT_VALUE, MAX_LIGHT_VALUE, messages);
        if (min == null || max == null) {
            return false;
        }
        if (max < min) {
            messages.add(ValidationMessage.error(ItemEditorText.str(
                    "validation.range",
                    label,
                    min,
                    MAX_LIGHT_VALUE
            )));
            return false;
        }
        if (Objects.equals(min, max)) {
            rulesTag.putInt(key, min);
            return true;
        }

        CompoundTag rangeTag = new CompoundTag();
        rangeTag.putInt("min_inclusive", min);
        rangeTag.putInt("max_inclusive", max);
        rulesTag.put(key, rangeTag);
        return true;
    }

    private BlockEntityType<?> resolveSpawnerType(SpecialDataApplyContext context) {
        if (context.previewStack().is(Items.SPAWNER)) {
            return BlockEntityType.MOB_SPAWNER;
        }

        TypedEntityData<BlockEntityType<?>> previewData = context.previewStack().get(DataComponents.BLOCK_ENTITY_DATA);
        if (previewData != null && previewData.type() == BlockEntityType.MOB_SPAWNER) {
            return BlockEntityType.MOB_SPAWNER;
        }

        TypedEntityData<BlockEntityType<?>> originalData = context.originalStack().get(DataComponents.BLOCK_ENTITY_DATA);
        if (originalData != null && originalData.type() == BlockEntityType.MOB_SPAWNER) {
            return BlockEntityType.MOB_SPAWNER;
        }

        return null;
    }

    private boolean sameSpawnerData(ItemEditorState.SpecialData current, ItemEditorState.SpecialData baseline) {
        return sameSpawnData(current.spawnerSpawnData, baseline.spawnerSpawnData)
                && Objects.equals(current.spawnerDelay, baseline.spawnerDelay)
                && Objects.equals(current.spawnerMinSpawnDelay, baseline.spawnerMinSpawnDelay)
                && Objects.equals(current.spawnerMaxSpawnDelay, baseline.spawnerMaxSpawnDelay)
                && Objects.equals(current.spawnerSpawnCount, baseline.spawnerSpawnCount)
                && Objects.equals(current.spawnerMaxNearbyEntities, baseline.spawnerMaxNearbyEntities)
                && Objects.equals(current.spawnerRequiredPlayerRange, baseline.spawnerRequiredPlayerRange)
                && Objects.equals(current.spawnerSpawnRange, baseline.spawnerSpawnRange)
                && current.spawnerUsePotentials == baseline.spawnerUsePotentials
                && this.sameList(current.spawnerPotentials, baseline.spawnerPotentials, (left, right) ->
                sameSpawnData(left.spawnData, right.spawnData) && Objects.equals(left.weight, right.weight)
        );
    }

    private boolean isSpawnerDataDefault(ItemEditorState.SpecialData special) {
        return isSpawnDataDefault(special.spawnerSpawnData)
                && special.spawnerDelay.isBlank()
                && special.spawnerMinSpawnDelay.isBlank()
                && special.spawnerMaxSpawnDelay.isBlank()
                && special.spawnerSpawnCount.isBlank()
                && special.spawnerMaxNearbyEntities.isBlank()
                && special.spawnerRequiredPlayerRange.isBlank()
                && special.spawnerSpawnRange.isBlank()
                && !special.spawnerUsePotentials
                && special.spawnerPotentials.isEmpty();
    }

    private static boolean sameSpawnData(
            ItemEditorState.SpawnerSpawnDataDraft current,
            ItemEditorState.SpawnerSpawnDataDraft baseline
    ) {
        return EntitySpawnDataUtil.sameEntity(current.entity, baseline.entity)
                && Objects.equals(current.blockLightMin, baseline.blockLightMin)
                && Objects.equals(current.blockLightMax, baseline.blockLightMax)
                && Objects.equals(current.skyLightMin, baseline.skyLightMin)
                && Objects.equals(current.skyLightMax, baseline.skyLightMax);
    }

    private static boolean isSpawnDataDefault(ItemEditorState.SpawnerSpawnDataDraft draft) {
        return EntitySpawnDataUtil.isEntityDefault(draft.entity)
                && blank(draft.blockLightMin)
                && blank(draft.blockLightMax)
                && blank(draft.skyLightMin)
                && blank(draft.skyLightMax);
    }
}
