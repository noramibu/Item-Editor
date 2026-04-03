package me.noramibu.itemeditor.service;

import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.editor.ValidationMessage;
import me.noramibu.itemeditor.util.IdFieldNormalizer;
import me.noramibu.itemeditor.util.ItemEditorText;
import me.noramibu.itemeditor.util.NbtCompatUtil;
import me.noramibu.itemeditor.util.ValidationUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;

import java.util.List;
import java.util.Objects;

final class SpawnerSpecialDataApplier extends AbstractPreviewApplierSupport implements SpecialDataApplier {

    private static final int MIN_SPAWNER_VALUE = 0;
    private static final int MAX_SPAWNER_VALUE = Short.MAX_VALUE;

    @Override
    public void apply(SpecialDataApplyContext context) {
        if (!this.supportsSpawnerData(context)) {
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
        CustomData originalData = context.originalStack().get(DataComponents.BLOCK_ENTITY_DATA);
        if (originalData != null) {
            blockTag = originalData.copyTag();
        }

        this.applyPrimaryEntity(blockTag, context.special(), context.messages());
        this.applySpawnPotentials(blockTag, context.special(), context.messages());
        this.putOptionalIntTag(
                blockTag,
                "Delay",
                context.special().spawnerDelay,
                ItemEditorText.str("special.spawner.delay"),
                MIN_SPAWNER_VALUE,
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
        context.previewStack().set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(blockTag));
    }

    private void applyPrimaryEntity(
            CompoundTag blockTag,
            ItemEditorState.SpecialData special,
            List<ValidationMessage> messages
    ) {
        String entityIdRaw = special.spawnerEntityId.trim();
        if (entityIdRaw.isBlank()) {
            blockTag.remove("SpawnData");
            return;
        }

        ResourceLocation entityId = IdFieldNormalizer.parse(entityIdRaw);
        if (entityId == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(entityId)) {
            messages.add(ValidationMessage.error(ItemEditorText.str(
                    "validation.registry_missing",
                    ItemEditorText.str("special.spawner.entity_id"),
                    entityIdRaw
            )));
            return;
        }

        CompoundTag spawnDataTag = NbtCompatUtil.getCompoundOrEmpty(blockTag, "SpawnData");
        CompoundTag entityTag = NbtCompatUtil.getCompoundOrEmpty(spawnDataTag, "entity");
        entityTag.putString("id", entityId.toString());
        spawnDataTag.put("entity", entityTag);
        blockTag.put("SpawnData", spawnDataTag);
    }

    private void applySpawnPotentials(
            CompoundTag blockTag,
            ItemEditorState.SpecialData special,
            List<ValidationMessage> messages
    ) {
        if (!special.spawnerUsePotentials) {
            blockTag.remove("SpawnPotentials");
            return;
        }

        ListTag potentialsTag = new ListTag();
        for (ItemEditorState.SpawnerPotentialDraft draft : special.spawnerPotentials) {
            String entityIdRaw = draft.entityId.trim();
            if (entityIdRaw.isBlank()) {
                continue;
            }

            ResourceLocation entityId = IdFieldNormalizer.parse(entityIdRaw);
            if (entityId == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(entityId)) {
                messages.add(ValidationMessage.error(ItemEditorText.str(
                        "validation.registry_missing",
                        ItemEditorText.str("special.spawner.entity_id"),
                        entityIdRaw
                )));
                continue;
            }

            Integer weight = ValidationUtil.parseInt(
                    draft.weight,
                    ItemEditorText.str("special.spawner.potential_weight"),
                    1,
                    MAX_SPAWNER_VALUE,
                    messages
            );
            if (weight == null) {
                continue;
            }

            CompoundTag potentialTag = new CompoundTag();
            potentialTag.putInt("weight", weight);
            CompoundTag dataTag = new CompoundTag();
            CompoundTag entityTag = new CompoundTag();
            entityTag.putString("id", entityId.toString());
            dataTag.put("entity", entityTag);
            potentialTag.put("data", dataTag);
            potentialsTag.add(potentialTag);
        }

        if (potentialsTag.isEmpty()) {
            blockTag.remove("SpawnPotentials");
        } else {
            blockTag.put("SpawnPotentials", potentialsTag);
        }
    }

    private boolean supportsSpawnerData(SpecialDataApplyContext context) {
        if (context.previewStack().is(Items.SPAWNER) || context.originalStack().is(Items.SPAWNER)) {
            return true;
        }
        CustomData previewData = context.previewStack().get(DataComponents.BLOCK_ENTITY_DATA);
        if (previewData != null && this.looksLikeSpawnerTag(previewData.copyTag())) {
            return true;
        }
        CustomData originalData = context.originalStack().get(DataComponents.BLOCK_ENTITY_DATA);
        return originalData != null && this.looksLikeSpawnerTag(originalData.copyTag());
    }

    private boolean looksLikeSpawnerTag(CompoundTag blockTag) {
        return blockTag.contains("SpawnData")
                || blockTag.contains("SpawnPotentials")
                || blockTag.contains("Delay")
                || blockTag.contains("MinSpawnDelay")
                || blockTag.contains("MaxSpawnDelay")
                || blockTag.contains("SpawnCount")
                || blockTag.contains("MaxNearbyEntities")
                || blockTag.contains("RequiredPlayerRange")
                || blockTag.contains("SpawnRange");
    }

    private boolean sameSpawnerData(ItemEditorState.SpecialData current, ItemEditorState.SpecialData baseline) {
        return Objects.equals(current.spawnerEntityId, baseline.spawnerEntityId)
                && Objects.equals(current.spawnerDelay, baseline.spawnerDelay)
                && Objects.equals(current.spawnerMinSpawnDelay, baseline.spawnerMinSpawnDelay)
                && Objects.equals(current.spawnerMaxSpawnDelay, baseline.spawnerMaxSpawnDelay)
                && Objects.equals(current.spawnerSpawnCount, baseline.spawnerSpawnCount)
                && Objects.equals(current.spawnerMaxNearbyEntities, baseline.spawnerMaxNearbyEntities)
                && Objects.equals(current.spawnerRequiredPlayerRange, baseline.spawnerRequiredPlayerRange)
                && Objects.equals(current.spawnerSpawnRange, baseline.spawnerSpawnRange)
                && current.spawnerUsePotentials == baseline.spawnerUsePotentials
                && this.sameList(current.spawnerPotentials, baseline.spawnerPotentials, (left, right) ->
                Objects.equals(left.entityId, right.entityId) && Objects.equals(left.weight, right.weight)
        );
    }

    private boolean isSpawnerDataDefault(ItemEditorState.SpecialData special) {
        return special.spawnerEntityId.isBlank()
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
}
