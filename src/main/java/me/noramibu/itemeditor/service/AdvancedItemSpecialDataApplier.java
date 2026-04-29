package me.noramibu.itemeditor.service;

import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.editor.ValidationMessage;
import me.noramibu.itemeditor.util.IdFieldNormalizer;
import me.noramibu.itemeditor.util.ItemEditorText;
import me.noramibu.itemeditor.util.RegistryUtil;
import me.noramibu.itemeditor.util.TextComponentUtil;
import me.noramibu.itemeditor.util.ValidationUtil;
import net.minecraft.advancements.criterion.DataComponentMatchers;
import net.minecraft.advancements.criterion.ItemPredicate;
import net.minecraft.advancements.criterion.MinMaxBounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Unit;
import net.minecraft.world.LockCode;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.painting.PaintingVariant;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SwingAnimationType;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.item.consume_effects.ConsumeEffect;
import net.minecraft.world.item.consume_effects.PlaySoundConsumeEffect;
import net.minecraft.world.item.component.AttackRange;
import net.minecraft.world.item.component.Bees;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.DamageResistant;
import net.minecraft.world.item.component.DeathProtection;
import net.minecraft.world.item.component.KineticWeapon;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.item.component.MapDecorations;
import net.minecraft.world.item.component.OminousBottleAmplifier;
import net.minecraft.world.item.component.PiercingWeapon;
import net.minecraft.world.item.component.SeededContainerLoot;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.item.component.UseCooldown;
import net.minecraft.world.item.component.UseEffects;
import net.minecraft.world.item.component.UseRemainder;
import net.minecraft.world.item.component.Weapon;
import net.minecraft.world.item.enchantment.Enchantable;
import net.minecraft.world.item.enchantment.Repairable;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.entity.PotDecorations;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;
import net.minecraft.world.level.saveddata.maps.MapId;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

final class AdvancedItemSpecialDataApplier extends AbstractPreviewApplierSupport implements SpecialDataApplier {

    @Override
    public void apply(SpecialDataApplyContext context) {
        this.applyFood(context);
        this.applyConsumable(context);
        this.applyUseEffects(context);
        this.applyUseRemainder(context);
        this.applyUseCooldown(context);

        this.applyLock(context);
        this.applyContainerLoot(context);
        this.applyBees(context);
        this.applyPotDecorations(context);

        this.applyChargedProjectiles(context);

        this.applyMapId(context);
        this.applyMapDecorations(context);
        this.applyLodestone(context);

        this.applyEquippable(context);
        this.applyWeapon(context);
        this.applyTool(context);
        this.applyRepairable(context);
        this.applyAttackRange(context);

        this.applyItemName(context);
        this.applyMaxStackSize(context);
        this.applyMinimumAttackCharge(context);
        this.applyEnchantable(context);
        this.applyOminousBottleAmplifier(context);
        this.applyTooltipStyle(context);
        this.applyDamageType(context);
        this.applyDamageResistant(context);
        this.applyNoteBlockSound(context);
        this.applyBreakSound(context);
        this.applyPaintingVariant(context);
        this.applyBlockState(context);
        this.applyDeathProtection(context);
        this.applyGlider(context);
        this.applyIntangibleProjectile(context);
        this.applyBlocksAttacks(context);
        this.applyPiercingWeapon(context);
        this.applyKineticWeapon(context);
        this.applySwingAnimation(context);
    }

    private void applyFood(SpecialDataApplyContext context) {
        if (this.handleUnchangedOrCleared(
                context,
                DataComponents.FOOD,
                Objects.equals(context.special().foodNutrition, context.baselineSpecial().foodNutrition)
                        && Objects.equals(context.special().foodSaturation, context.baselineSpecial().foodSaturation)
                        && context.special().foodCanAlwaysEat == context.baselineSpecial().foodCanAlwaysEat,
                context.special().foodNutrition.isBlank() && context.special().foodSaturation.isBlank() && !context.special().foodCanAlwaysEat
        )) {
            return;
        }

        FoodProperties original = context.originalStack().get(DataComponents.FOOD);
        Integer nutrition = context.special().foodNutrition.isBlank()
                ? this.valueFromOriginal(original, FoodProperties::nutrition, 0)
                : ValidationUtil.parseInt(context.special().foodNutrition, ItemEditorText.str("special.advanced.food.nutrition"), 0, 4096, context.messages());
        if (nutrition == null) {
            return;
        }

        Float saturation = context.special().foodSaturation.isBlank()
                ? this.valueFromOriginal(original, FoodProperties::saturation, 0.0F)
                : ValidationUtil.parseFloat(context.special().foodSaturation, ItemEditorText.str("special.advanced.food.saturation"), context.messages());
        if (saturation == null) {
            return;
        }

        context.previewStack().set(DataComponents.FOOD, new FoodProperties(nutrition, saturation, context.special().foodCanAlwaysEat));
    }

    private void applyConsumable(SpecialDataApplyContext context) {
        if (this.handleUnchangedOrCleared(
                context,
                DataComponents.CONSUMABLE,
                Objects.equals(context.special().consumableConsumeSeconds, context.baselineSpecial().consumableConsumeSeconds)
                        && Objects.equals(context.special().consumableAnimation, context.baselineSpecial().consumableAnimation)
                        && Objects.equals(context.special().consumableSoundId, context.baselineSpecial().consumableSoundId)
                        && context.special().consumableHasParticles == context.baselineSpecial().consumableHasParticles
                        && this.sameConsumableEffects(context.special().consumableOnConsumeEffects, context.baselineSpecial().consumableOnConsumeEffects),
                context.special().consumableConsumeSeconds.isBlank()
                        && context.special().consumableAnimation.isBlank()
                        && context.special().consumableSoundId.isBlank()
                        && !context.special().consumableHasParticles
                        && context.special().consumableOnConsumeEffects.isEmpty()
        )) {
            return;
        }

        Consumable original = context.originalStack().get(DataComponents.CONSUMABLE);
        Float consumeSeconds = context.special().consumableConsumeSeconds.isBlank()
                ? this.valueFromOriginal(original, Consumable::consumeSeconds, Consumable.DEFAULT_CONSUME_SECONDS)
                : ValidationUtil.parseFloat(context.special().consumableConsumeSeconds, ItemEditorText.str("special.advanced.consumable.consume_seconds"), context.messages());
        if (consumeSeconds == null || consumeSeconds < 0.0F) {
            return;
        }

        ItemUseAnimation animation = this.parseAnimation(
                context.special().consumableAnimation,
                this.valueFromOriginal(original, Consumable::animation, ItemUseAnimation.EAT),
                context.messages()
        );
        if (animation == null) {
            return;
        }

        Registry<SoundEvent> soundRegistry = context.registryAccess().lookupOrThrow(Registries.SOUND_EVENT);
        Holder<SoundEvent> sound = this.resolveSoundHolder(
                soundRegistry,
                context.special().consumableSoundId,
                this.valueFromOriginal(original, Consumable::sound, this.defaultConsumeSound(animation)),
                ItemEditorText.str("special.advanced.consumable.sound"),
                context.messages()
        );
        if (sound == null) {
            return;
        }

        List<ConsumeEffect> effects;
        if (this.sameConsumableEffects(context.special().consumableOnConsumeEffects, context.baselineSpecial().consumableOnConsumeEffects)) {
            effects = this.valueFromOriginal(original, Consumable::onConsumeEffects, List.of());
        } else {
            effects = this.parseConsumableEffects(context);
            if (effects == null) {
                return;
            }
        }
        context.previewStack().set(
                DataComponents.CONSUMABLE,
                new Consumable(consumeSeconds, animation, sound, context.special().consumableHasParticles, effects)
        );
    }

    private void applyUseEffects(SpecialDataApplyContext context) {
        if (this.handleUnchangedOrCleared(
                context,
                DataComponents.USE_EFFECTS,
                context.special().useEffectsCanSprint == context.baselineSpecial().useEffectsCanSprint
                        && context.special().useEffectsInteractVibrations == context.baselineSpecial().useEffectsInteractVibrations
                        && Objects.equals(context.special().useEffectsSpeedMultiplier, context.baselineSpecial().useEffectsSpeedMultiplier),
                context.special().useEffectsSpeedMultiplier.isBlank()
                        && !context.special().useEffectsCanSprint
                        && !context.special().useEffectsInteractVibrations
        )) {
            return;
        }

        UseEffects original = context.originalStack().get(DataComponents.USE_EFFECTS);
        Float speed = context.special().useEffectsSpeedMultiplier.isBlank()
                ? this.valueFromOriginal(original, UseEffects::speedMultiplier, 1.0F)
                : ValidationUtil.parseFloat(context.special().useEffectsSpeedMultiplier, ItemEditorText.str("special.advanced.use_effects.speed_multiplier"), context.messages());
        if (speed == null) {
            return;
        }

        context.previewStack().set(
                DataComponents.USE_EFFECTS,
                new UseEffects(
                        context.special().useEffectsCanSprint,
                        context.special().useEffectsInteractVibrations,
                        speed
                )
        );
    }

    private void applyUseRemainder(SpecialDataApplyContext context) {
        if (this.handleUnchangedOrCleared(
                context,
                DataComponents.USE_REMAINDER,
                Objects.equals(context.special().useRemainderItemId, context.baselineSpecial().useRemainderItemId)
                        && Objects.equals(context.special().useRemainderCount, context.baselineSpecial().useRemainderCount),
                context.special().useRemainderItemId.isBlank()
        )) {
            return;
        }

        Item item = this.resolveItem(
                context.special().useRemainderItemId,
                ItemEditorText.str("special.advanced.use_remainder.item_id"),
                context.messages()
        );
        if (item == null) {
            return;
        }

        int maxCount = Math.max(1, new ItemStack(item).getMaxStackSize());
        Integer count = context.special().useRemainderCount.isBlank()
                ? Integer.valueOf(1)
                : ValidationUtil.parseInt(
                        context.special().useRemainderCount,
                        ItemEditorText.str("special.advanced.use_remainder.count"),
                        1,
                        maxCount,
                        context.messages()
                );
        if (count == null) {
            return;
        }

        context.previewStack().set(DataComponents.USE_REMAINDER, new UseRemainder(new ItemStackTemplate(item, count)));
    }

    private void applyUseCooldown(SpecialDataApplyContext context) {
        if (this.handleUnchangedOrCleared(
                context,
                DataComponents.USE_COOLDOWN,
                Objects.equals(context.special().useCooldownSeconds, context.baselineSpecial().useCooldownSeconds)
                        && Objects.equals(context.special().useCooldownGroup, context.baselineSpecial().useCooldownGroup),
                context.special().useCooldownSeconds.isBlank()
        )) {
            return;
        }

        Float seconds = ValidationUtil.parseFloat(context.special().useCooldownSeconds, ItemEditorText.str("special.advanced.use_cooldown.seconds"), context.messages());
        if (seconds == null || seconds < 0.0F) {
            return;
        }

        if (context.special().useCooldownGroup.isBlank()) {
            context.previewStack().set(DataComponents.USE_COOLDOWN, new UseCooldown(seconds));
            return;
        }

        Identifier groupId = IdFieldNormalizer.parse(context.special().useCooldownGroup);
        if (groupId == null) {
            context.messages().add(ValidationMessage.error(ItemEditorText.str("preview.validation.component_failed", ItemEditorText.str("special.advanced.use_cooldown.group"))));
            return;
        }

        context.previewStack().set(DataComponents.USE_COOLDOWN, new UseCooldown(seconds, Optional.of(groupId)));
    }

    private void applyLock(SpecialDataApplyContext context) {
        String lockItemId = context.special().lockItemId == null ? "" : context.special().lockItemId.trim();
        String lockPredicateSnbt = context.special().lockPredicateSnbt == null ? "" : context.special().lockPredicateSnbt.trim();
        String baselineLockItemId = context.baselineSpecial().lockItemId == null ? "" : context.baselineSpecial().lockItemId.trim();
        String baselineLockPredicateSnbt = context.baselineSpecial().lockPredicateSnbt == null ? "" : context.baselineSpecial().lockPredicateSnbt.trim();

        if (this.handleUnchangedOrCleared(
                context,
                DataComponents.LOCK,
                Objects.equals(lockItemId, baselineLockItemId)
                        && Objects.equals(lockPredicateSnbt, baselineLockPredicateSnbt),
                lockItemId.isBlank() && lockPredicateSnbt.isBlank()
        )) {
            return;
        }

        if (!lockPredicateSnbt.isBlank()) {
            ItemPredicate predicate = this.parseLockPredicate(lockPredicateSnbt, context);
            if (predicate == null) {
                return;
            }
            context.previewStack().set(DataComponents.LOCK, new LockCode(predicate));
            return;
        }

        Holder<Item> itemHolder = RegistryUtil.resolveHolder(
                context.registryAccess().lookupOrThrow(Registries.ITEM),
                lockItemId
        );
        if (itemHolder == null) {
            context.messages().add(ValidationMessage.error(ItemEditorText.str(
                    "validation.registry_missing",
                    ItemEditorText.str("special.advanced.container_meta.lock_item"),
                    lockItemId
            )));
            return;
        }

        ItemPredicate predicate = new ItemPredicate(Optional.of(HolderSet.direct(itemHolder)), MinMaxBounds.Ints.ANY, DataComponentMatchers.ANY);
        context.previewStack().set(DataComponents.LOCK, new LockCode(predicate));
    }

    private ItemPredicate parseLockPredicate(String raw, SpecialDataApplyContext context) {
        try {
            var ops = context.registryAccess().createSerializationContext(NbtOps.INSTANCE);
            var parsedTag = TagParser.create(ops).parseFully(raw);
            return ItemPredicate.CODEC.parse(ops, parsedTag)
                    .resultOrPartial(error -> context.messages().add(
                            ValidationMessage.error(ItemEditorText.str(
                                    "preview.validation.component_failed",
                                    ItemEditorText.str("special.advanced.container_meta.lock_predicate") + ": " + error
                            ))
                    ))
                    .orElse(null);
        } catch (CommandSyntaxException exception) {
            context.messages().add(ValidationMessage.error(ItemEditorText.str(
                    "preview.validation.component_failed",
                    ItemEditorText.str("special.advanced.container_meta.lock_predicate") + ": " + exception.getMessage()
            )));
            return null;
        }
    }

    private void applyContainerLoot(SpecialDataApplyContext context) {
        if (this.handleUnchangedOrCleared(
                context,
                DataComponents.CONTAINER_LOOT,
                Objects.equals(context.special().containerLootTableId, context.baselineSpecial().containerLootTableId)
                        && Objects.equals(context.special().containerLootSeed, context.baselineSpecial().containerLootSeed),
                context.special().containerLootTableId.isBlank()
        )) {
            return;
        }

        Identifier lootId = IdFieldNormalizer.parse(context.special().containerLootTableId);
        if (lootId == null) {
            context.messages().add(ValidationMessage.error(ItemEditorText.str("preview.validation.component_failed", ItemEditorText.str("special.advanced.container_meta.loot_table"))));
            return;
        }

        long seed = 0L;
        if (!context.special().containerLootSeed.isBlank()) {
            try {
                seed = Long.parseLong(context.special().containerLootSeed.trim());
            } catch (NumberFormatException exception) {
                context.messages().add(ValidationMessage.error(ItemEditorText.str("validation.whole_number", ItemEditorText.str("special.advanced.container_meta.loot_seed"))));
                return;
            }
        }

        ResourceKey<LootTable> lootKey = ResourceKey.create(Registries.LOOT_TABLE, lootId);
        context.previewStack().set(DataComponents.CONTAINER_LOOT, new SeededContainerLoot(lootKey, seed));
    }

    private void applyBees(SpecialDataApplyContext context) {
        if (this.handleUnchangedOrCleared(
                context,
                DataComponents.BEES,
                this.sameBeeOccupants(context.special().beesOccupants, context.baselineSpecial().beesOccupants),
                context.special().beesOccupants.isEmpty()
        )) {
            return;
        }

        if (context.special().beesOccupants.isEmpty()) {
            this.clearToPrototype(context.previewStack(), DataComponents.BEES);
            return;
        }

        List<BeehiveBlockEntity.Occupant> occupants = new ArrayList<>();
        for (int index = 0; index < context.special().beesOccupants.size(); index++) {
            ItemEditorState.BeeOccupantDraft draft = context.special().beesOccupants.get(index);

            String normalizedEntityId = draft.entityId == null || draft.entityId.isBlank()
                    ? "minecraft:bee"
                    : IdFieldNormalizer.normalize(draft.entityId);
            Identifier entityId = IdFieldNormalizer.parse(normalizedEntityId);
            if (entityId == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(entityId)) {
                context.messages().add(ValidationMessage.error(ItemEditorText.str(
                        "validation.registry_missing",
                        ItemEditorText.str("special.advanced.container_meta.bees_entity"),
                        normalizedEntityId
                )));
                return;
            }
            EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.getOptional(entityId).orElse(null);
            if (entityType == null) {
                context.messages().add(ValidationMessage.error(ItemEditorText.str(
                        "validation.registry_missing",
                        ItemEditorText.str("special.advanced.container_meta.bees_entity"),
                        normalizedEntityId
                )));
                return;
            }

            String ticksRaw = draft.ticksInHive == null || draft.ticksInHive.isBlank() ? "0" : draft.ticksInHive;
            String minTicksRaw = draft.minTicksInHive == null || draft.minTicksInHive.isBlank() ? "0" : draft.minTicksInHive;

            Integer ticks = ValidationUtil.parseInt(
                    ticksRaw,
                    ItemEditorText.str("special.advanced.container_meta.bees_ticks"),
                    0,
                    72000,
                    context.messages()
            );
            Integer minTicks = ValidationUtil.parseInt(
                    minTicksRaw,
                    ItemEditorText.str("special.advanced.container_meta.bees_min_ticks"),
                    0,
                    72000,
                    context.messages()
            );
            if (ticks == null || minTicks == null) {
                return;
            }

            net.minecraft.nbt.CompoundTag entityTag = new net.minecraft.nbt.CompoundTag();
            occupants.add(new BeehiveBlockEntity.Occupant(TypedEntityData.of(entityType, entityTag), ticks, minTicks));
        }
        context.previewStack().set(DataComponents.BEES, new Bees(occupants));
    }

    private void applyPotDecorations(SpecialDataApplyContext context) {
        if (Objects.equals(context.special().potBackItemId, context.baselineSpecial().potBackItemId)
                && Objects.equals(context.special().potLeftItemId, context.baselineSpecial().potLeftItemId)
                && Objects.equals(context.special().potRightItemId, context.baselineSpecial().potRightItemId)
                && Objects.equals(context.special().potFrontItemId, context.baselineSpecial().potFrontItemId)) {
            this.restoreOriginalComponent(context.originalStack(), context.previewStack(), DataComponents.POT_DECORATIONS);
            return;
        }

        Optional<Item> back = this.resolveOptionalItem(context.special().potBackItemId, ItemEditorText.str("special.advanced.container_meta.pot_back"), context.messages());
        Optional<Item> left = this.resolveOptionalItem(context.special().potLeftItemId, ItemEditorText.str("special.advanced.container_meta.pot_left"), context.messages());
        Optional<Item> right = this.resolveOptionalItem(context.special().potRightItemId, ItemEditorText.str("special.advanced.container_meta.pot_right"), context.messages());
        Optional<Item> front = this.resolveOptionalItem(context.special().potFrontItemId, ItemEditorText.str("special.advanced.container_meta.pot_front"), context.messages());
        if (this.hasInvalidOptionalInput(context.special().potBackItemId, back.isPresent())
                || this.hasInvalidOptionalInput(context.special().potLeftItemId, left.isPresent())
                || this.hasInvalidOptionalInput(context.special().potRightItemId, right.isPresent())
                || this.hasInvalidOptionalInput(context.special().potFrontItemId, front.isPresent())) {
            return;
        }

        if (back.isEmpty() && left.isEmpty() && right.isEmpty() && front.isEmpty()) {
            this.clearToPrototype(context.previewStack(), DataComponents.POT_DECORATIONS);
            return;
        }

        context.previewStack().set(DataComponents.POT_DECORATIONS, new PotDecorations(back, left, right, front));
    }

    private void applyChargedProjectiles(SpecialDataApplyContext context) {
        if (this.handleUnchangedOrCleared(
                context,
                DataComponents.CHARGED_PROJECTILES,
                this.sameChargedProjectiles(context.special().chargedProjectiles, context.baselineSpecial().chargedProjectiles),
                context.special().chargedProjectiles.isEmpty()
        )) {
            return;
        }

        List<ItemStack> projectiles = new ArrayList<>();
        for (ItemEditorState.ChargedProjectileDraft draft : context.special().chargedProjectiles) {
            if (draft.itemId.isBlank()) {
                continue;
            }

            Item item = this.resolveItem(draft.itemId, ItemEditorText.str("special.advanced.crossbow.item"), context.messages());
            if (item == null) {
                continue;
            }

            int max = Math.max(1, new ItemStack(item).getMaxStackSize());
            Integer count = ValidationUtil.parseInt(draft.count, ItemEditorText.str("special.advanced.crossbow.count"), 1, max, context.messages());
            if (count == null) {
                continue;
            }
            projectiles.add(new ItemStack(item, count));
        }

        if (projectiles.isEmpty()) {
            this.clearToPrototype(context.previewStack(), DataComponents.CHARGED_PROJECTILES);
            return;
        }

        context.previewStack().set(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.ofNonEmpty(projectiles));
    }

    private void applyMapId(SpecialDataApplyContext context) {
        if (this.handleUnchangedOrCleared(
                context,
                DataComponents.MAP_ID,
                Objects.equals(context.special().mapId, context.baselineSpecial().mapId),
                context.special().mapId.isBlank()
        )) {
            return;
        }

        Integer id = ValidationUtil.parseInt(context.special().mapId, ItemEditorText.str("special.advanced.map.map_id"), 0, Integer.MAX_VALUE, context.messages());
        if (id == null) {
            return;
        }

        context.previewStack().set(DataComponents.MAP_ID, new MapId(id));
    }

    private void applyMapDecorations(SpecialDataApplyContext context) {
        if (this.handleUnchangedOrCleared(
                context,
                DataComponents.MAP_DECORATIONS,
                this.sameMapDecorations(context.special().mapDecorations, context.baselineSpecial().mapDecorations),
                context.special().mapDecorations.isEmpty()
        )) {
            return;
        }

        Registry<MapDecorationType> typeRegistry = context.registryAccess().lookupOrThrow(Registries.MAP_DECORATION_TYPE);
        LinkedHashMap<String, MapDecorations.Entry> entries = new LinkedHashMap<>();
        for (int index = 0; index < context.special().mapDecorations.size(); index++) {
            ItemEditorState.MapDecorationDraft draft = context.special().mapDecorations.get(index);
            String key = draft.key.isBlank() ? "decoration_" + index : draft.key.trim();

            Holder<MapDecorationType> type = RegistryUtil.resolveHolder(typeRegistry, draft.typeId);
            if (type == null) {
                context.messages().add(ValidationMessage.error(ItemEditorText.str(
                        "validation.registry_missing",
                        ItemEditorText.str("special.advanced.map.decoration_type"),
                        draft.typeId
                )));
                continue;
            }

            Double x = ValidationUtil.parseDouble(draft.x, ItemEditorText.str("special.advanced.map.decoration_x"), context.messages());
            Double z = ValidationUtil.parseDouble(draft.z, ItemEditorText.str("special.advanced.map.decoration_z"), context.messages());
            Float rotation = ValidationUtil.parseFloat(draft.rotation, ItemEditorText.str("special.advanced.map.decoration_rotation"), context.messages());
            if (x == null || z == null || rotation == null) {
                continue;
            }

            entries.put(key, new MapDecorations.Entry(type, x, z, rotation));
        }

        if (entries.isEmpty()) {
            this.clearToPrototype(context.previewStack(), DataComponents.MAP_DECORATIONS);
            return;
        }

        context.previewStack().set(DataComponents.MAP_DECORATIONS, new MapDecorations(entries));
    }

    private void applyLodestone(SpecialDataApplyContext context) {
        if (this.handleUnchangedOrCleared(
                context,
                DataComponents.LODESTONE_TRACKER,
                context.special().lodestoneEnabled == context.baselineSpecial().lodestoneEnabled
                        && context.special().lodestoneTracked == context.baselineSpecial().lodestoneTracked
                        && Objects.equals(context.special().lodestoneDimensionId, context.baselineSpecial().lodestoneDimensionId)
                        && Objects.equals(context.special().lodestoneX, context.baselineSpecial().lodestoneX)
                        && Objects.equals(context.special().lodestoneY, context.baselineSpecial().lodestoneY)
                        && Objects.equals(context.special().lodestoneZ, context.baselineSpecial().lodestoneZ),
                !context.special().lodestoneEnabled
        )) {
            return;
        }

        Identifier dimensionId = IdFieldNormalizer.parse(context.special().lodestoneDimensionId);
        if (dimensionId == null) {
            context.messages().add(ValidationMessage.error(ItemEditorText.str("preview.validation.component_failed", ItemEditorText.str("special.advanced.map.lodestone_dimension"))));
            return;
        }

        Integer x = ValidationUtil.parseInt(context.special().lodestoneX, ItemEditorText.str("special.advanced.map.lodestone_x"), -30000000, 30000000, context.messages());
        Integer y = ValidationUtil.parseInt(context.special().lodestoneY, ItemEditorText.str("special.advanced.map.lodestone_y"), -2048, 4096, context.messages());
        Integer z = ValidationUtil.parseInt(context.special().lodestoneZ, ItemEditorText.str("special.advanced.map.lodestone_z"), -30000000, 30000000, context.messages());
        if (x == null || y == null || z == null) {
            return;
        }

        ResourceKey<Level> dimensionKey = ResourceKey.create(Registries.DIMENSION, dimensionId);
        GlobalPos target = GlobalPos.of(dimensionKey, new BlockPos(x, y, z));
        context.previewStack().set(DataComponents.LODESTONE_TRACKER, new LodestoneTracker(Optional.of(target), context.special().lodestoneTracked));
    }

    private void applyEquippable(SpecialDataApplyContext context) {
        if (this.handleUnchangedOrCleared(
                context,
                DataComponents.EQUIPPABLE,
                Objects.equals(context.special().equippableSlot, context.baselineSpecial().equippableSlot)
                        && Objects.equals(context.special().equippableEquipSoundId, context.baselineSpecial().equippableEquipSoundId)
                        && Objects.equals(context.special().equippableShearingSoundId, context.baselineSpecial().equippableShearingSoundId)
                        && context.special().equippableDispensable == context.baselineSpecial().equippableDispensable
                        && context.special().equippableSwappable == context.baselineSpecial().equippableSwappable
                        && context.special().equippableDamageOnHurt == context.baselineSpecial().equippableDamageOnHurt
                        && context.special().equippableEquipOnInteract == context.baselineSpecial().equippableEquipOnInteract
                        && context.special().equippableCanBeSheared == context.baselineSpecial().equippableCanBeSheared,
                context.special().equippableSlot.isBlank()
        )) {
            return;
        }

        EquipmentSlot slot;
        try {
            slot = EquipmentSlot.valueOf(context.special().equippableSlot);
        } catch (IllegalArgumentException exception) {
            context.messages().add(ValidationMessage.error(ItemEditorText.str("preview.validation.component_failed", ItemEditorText.str("special.advanced.combat.equippable_slot"))));
            return;
        }

        Equippable original = context.originalStack().get(DataComponents.EQUIPPABLE);
        Registry<SoundEvent> soundRegistry = context.registryAccess().lookupOrThrow(Registries.SOUND_EVENT);
        Holder<SoundEvent> equipSound = this.resolveSoundHolder(
                soundRegistry,
                context.special().equippableEquipSoundId,
                this.valueFromOriginal(original, Equippable::equipSound, SoundEvents.ARMOR_EQUIP_GENERIC),
                ItemEditorText.str("special.advanced.combat.equippable_sound"),
                context.messages()
        );
        if (equipSound == null) {
            return;
        }

        Holder<SoundEvent> shearingSound = this.resolveSoundHolder(
                soundRegistry,
                context.special().equippableShearingSoundId,
                this.valueFromOriginal(original, Equippable::shearingSound, equipSound),
                ItemEditorText.str("special.advanced.combat.equippable_shearing_sound"),
                context.messages()
        );
        if (shearingSound == null) {
            return;
        }

        Equippable.Builder builder = Equippable.builder(slot)
                .setEquipSound(equipSound)
                .setDispensable(context.special().equippableDispensable)
                .setSwappable(context.special().equippableSwappable)
                .setDamageOnHurt(context.special().equippableDamageOnHurt)
                .setEquipOnInteract(context.special().equippableEquipOnInteract)
                .setCanBeSheared(context.special().equippableCanBeSheared)
                .setShearingSound(shearingSound);

        if (original != null) {
            original.allowedEntities().ifPresent(builder::setAllowedEntities);
            original.assetId().ifPresent(builder::setAsset);
            original.cameraOverlay().ifPresent(builder::setCameraOverlay);
        }

        context.previewStack().set(DataComponents.EQUIPPABLE, builder.build());
    }

    private void applyWeapon(SpecialDataApplyContext context) {
        if (this.handleUnchangedOrCleared(
                context,
                DataComponents.WEAPON,
                Objects.equals(context.special().weaponItemDamagePerAttack, context.baselineSpecial().weaponItemDamagePerAttack)
                        && Objects.equals(context.special().weaponDisableBlockingForSeconds, context.baselineSpecial().weaponDisableBlockingForSeconds),
                context.special().weaponItemDamagePerAttack.isBlank() && context.special().weaponDisableBlockingForSeconds.isBlank()
        )) {
            return;
        }

        Weapon original = context.originalStack().get(DataComponents.WEAPON);
        Integer damage = context.special().weaponItemDamagePerAttack.isBlank()
                ? this.valueFromOriginal(original, Weapon::itemDamagePerAttack, 1)
                : ValidationUtil.parseInt(context.special().weaponItemDamagePerAttack, ItemEditorText.str("special.advanced.combat.weapon_damage"), 0, 4096, context.messages());
        Float disable = context.special().weaponDisableBlockingForSeconds.isBlank()
                ? this.valueFromOriginal(original, Weapon::disableBlockingForSeconds, 0.0F)
                : ValidationUtil.parseFloat(context.special().weaponDisableBlockingForSeconds, ItemEditorText.str("special.advanced.combat.weapon_disable"), context.messages());
        if (damage == null || disable == null || disable < 0.0F) {
            return;
        }

        context.previewStack().set(DataComponents.WEAPON, new Weapon(damage, disable));
    }

    private void applyTool(SpecialDataApplyContext context) {
        if (this.handleUnchangedOrCleared(
                context,
                DataComponents.TOOL,
                Objects.equals(context.special().toolDefaultMiningSpeed, context.baselineSpecial().toolDefaultMiningSpeed)
                        && Objects.equals(context.special().toolDamagePerBlock, context.baselineSpecial().toolDamagePerBlock)
                        && context.special().toolCanDestroyBlocksInCreative == context.baselineSpecial().toolCanDestroyBlocksInCreative,
                context.special().toolDefaultMiningSpeed.isBlank()
                        && context.special().toolDamagePerBlock.isBlank()
                        && !context.special().toolCanDestroyBlocksInCreative
        )) {
            return;
        }

        Tool original = context.originalStack().get(DataComponents.TOOL);
        Float speed = context.special().toolDefaultMiningSpeed.isBlank()
                ? this.valueFromOriginal(original, Tool::defaultMiningSpeed, 1.0F)
                : ValidationUtil.parseFloat(context.special().toolDefaultMiningSpeed, ItemEditorText.str("special.advanced.combat.tool_speed"), context.messages());
        Integer damage = context.special().toolDamagePerBlock.isBlank()
                ? this.valueFromOriginal(original, Tool::damagePerBlock, 1)
                : ValidationUtil.parseInt(context.special().toolDamagePerBlock, ItemEditorText.str("special.advanced.combat.tool_damage"), 0, 4096, context.messages());
        if (speed == null || damage == null) {
            return;
        }

        List<Tool.Rule> rules = this.valueFromOriginal(original, Tool::rules, List.of());
        context.previewStack().set(
                DataComponents.TOOL,
                new Tool(rules, speed, damage, context.special().toolCanDestroyBlocksInCreative)
        );
    }

    private void applyRepairable(SpecialDataApplyContext context) {
        if (this.handleUnchangedOrCleared(
                context,
                DataComponents.REPAIRABLE,
                Objects.equals(context.special().repairableItemIds, context.baselineSpecial().repairableItemIds),
                context.special().repairableItemIds.isEmpty()
        )) {
            return;
        }

        Registry<Item> itemRegistry = context.registryAccess().lookupOrThrow(Registries.ITEM);
        List<Holder<Item>> items = new ArrayList<>();
        for (String rawId : context.special().repairableItemIds) {
            Holder<Item> holder = RegistryUtil.resolveHolder(itemRegistry, rawId);
            if (holder == null) {
                context.messages().add(ValidationMessage.error(ItemEditorText.str(
                        "validation.registry_missing",
                        ItemEditorText.str("special.advanced.combat.repair_item"),
                        rawId
                )));
                continue;
            }
            items.add(holder);
        }

        if (items.isEmpty()) {
            this.clearToPrototype(context.previewStack(), DataComponents.REPAIRABLE);
            return;
        }

        context.previewStack().set(DataComponents.REPAIRABLE, new Repairable(HolderSet.direct(items)));
    }

    private void applyAttackRange(SpecialDataApplyContext context) {
        if (this.handleUnchangedOrCleared(
                context,
                DataComponents.ATTACK_RANGE,
                Objects.equals(context.special().attackRangeMinReach, context.baselineSpecial().attackRangeMinReach)
                        && Objects.equals(context.special().attackRangeMaxReach, context.baselineSpecial().attackRangeMaxReach)
                        && Objects.equals(context.special().attackRangeMinCreativeReach, context.baselineSpecial().attackRangeMinCreativeReach)
                        && Objects.equals(context.special().attackRangeMaxCreativeReach, context.baselineSpecial().attackRangeMaxCreativeReach)
                        && Objects.equals(context.special().attackRangeHitboxMargin, context.baselineSpecial().attackRangeHitboxMargin)
                        && Objects.equals(context.special().attackRangeMobFactor, context.baselineSpecial().attackRangeMobFactor),
                context.special().attackRangeMinReach.isBlank()
                        && context.special().attackRangeMaxReach.isBlank()
                        && context.special().attackRangeMinCreativeReach.isBlank()
                        && context.special().attackRangeMaxCreativeReach.isBlank()
                        && context.special().attackRangeHitboxMargin.isBlank()
                        && context.special().attackRangeMobFactor.isBlank()
        )) {
            return;
        }

        AttackRange original = context.originalStack().get(DataComponents.ATTACK_RANGE);
        Float minReach = this.readRangeField(context.special().attackRangeMinReach, original == null ? 0.0F : original.minReach(), ItemEditorText.str("special.advanced.combat.range_min_reach"), context.messages());
        Float maxReach = this.readRangeField(context.special().attackRangeMaxReach, original == null ? 0.0F : original.maxReach(), ItemEditorText.str("special.advanced.combat.range_max_reach"), context.messages());
        Float minCreative = this.readRangeField(context.special().attackRangeMinCreativeReach, original == null ? 0.0F : original.minCreativeReach(), ItemEditorText.str("special.advanced.combat.range_min_creative"), context.messages());
        Float maxCreative = this.readRangeField(context.special().attackRangeMaxCreativeReach, original == null ? 0.0F : original.maxCreativeReach(), ItemEditorText.str("special.advanced.combat.range_max_creative"), context.messages());
        Float hitboxMargin = this.readRangeField(context.special().attackRangeHitboxMargin, original == null ? 0.0F : original.hitboxMargin(), ItemEditorText.str("special.advanced.combat.range_hitbox"), context.messages());
        Float mobFactor = this.readRangeField(context.special().attackRangeMobFactor, original == null ? 0.0F : original.mobFactor(), ItemEditorText.str("special.advanced.combat.range_mob_factor"), context.messages());
        if (minReach == null || maxReach == null || minCreative == null || maxCreative == null || hitboxMargin == null || mobFactor == null) {
            return;
        }

        context.previewStack().set(DataComponents.ATTACK_RANGE, new AttackRange(minReach, maxReach, minCreative, maxCreative, hitboxMargin, mobFactor));
    }

    private void applyItemName(SpecialDataApplyContext context) {
        if (this.handleUnchangedOrCleared(
                context,
                DataComponents.ITEM_NAME,
                Objects.equals(context.special().itemName, context.baselineSpecial().itemName),
                context.special().itemName.isBlank()
        )) {
            return;
        }

        context.previewStack().set(DataComponents.ITEM_NAME, TextComponentUtil.parseMarkup(context.special().itemName));
    }

    private void applyMaxStackSize(SpecialDataApplyContext context) {
        if (this.handleUnchangedOrCleared(
                context,
                DataComponents.MAX_STACK_SIZE,
                Objects.equals(context.special().maxStackSize, context.baselineSpecial().maxStackSize),
                context.special().maxStackSize.isBlank()
        )) {
            return;
        }

        Integer maxStackSize = ValidationUtil.parseInt(
                context.special().maxStackSize,
                ItemEditorText.str("special.advanced.component_tweaks.max_stack_size"),
                1,
                999,
                context.messages()
        );
        if (maxStackSize == null) {
            return;
        }

        context.previewStack().set(DataComponents.MAX_STACK_SIZE, maxStackSize);
    }

    private void applyMinimumAttackCharge(SpecialDataApplyContext context) {
        if (this.handleUnchangedOrCleared(
                context,
                DataComponents.MINIMUM_ATTACK_CHARGE,
                Objects.equals(context.special().minimumAttackCharge, context.baselineSpecial().minimumAttackCharge),
                context.special().minimumAttackCharge.isBlank()
        )) {
            return;
        }

        Float value = ValidationUtil.parseFloat(
                context.special().minimumAttackCharge,
                ItemEditorText.str("special.advanced.component_tweaks.min_attack_charge"),
                context.messages()
        );
        if (value == null || value < 0.0F) {
            return;
        }

        context.previewStack().set(DataComponents.MINIMUM_ATTACK_CHARGE, value);
    }

    private void applyEnchantable(SpecialDataApplyContext context) {
        if (this.handleUnchangedOrCleared(
                context,
                DataComponents.ENCHANTABLE,
                Objects.equals(context.special().enchantableValue, context.baselineSpecial().enchantableValue),
                context.special().enchantableValue.isBlank()
        )) {
            return;
        }

        Integer value = ValidationUtil.parseInt(
                context.special().enchantableValue,
                ItemEditorText.str("special.advanced.component_tweaks.enchantable"),
                0,
                1024,
                context.messages()
        );
        if (value == null) {
            return;
        }

        context.previewStack().set(DataComponents.ENCHANTABLE, new Enchantable(value));
    }

    private void applyOminousBottleAmplifier(SpecialDataApplyContext context) {
        if (this.handleUnchangedOrCleared(
                context,
                DataComponents.OMINOUS_BOTTLE_AMPLIFIER,
                Objects.equals(context.special().ominousBottleAmplifier, context.baselineSpecial().ominousBottleAmplifier),
                context.special().ominousBottleAmplifier.isBlank()
        )) {
            return;
        }

        Integer value = ValidationUtil.parseInt(
                context.special().ominousBottleAmplifier,
                ItemEditorText.str("special.advanced.component_tweaks.ominous_amplifier"),
                OminousBottleAmplifier.MIN_AMPLIFIER,
                OminousBottleAmplifier.MAX_AMPLIFIER,
                context.messages()
        );
        if (value == null) {
            return;
        }

        context.previewStack().set(DataComponents.OMINOUS_BOTTLE_AMPLIFIER, new OminousBottleAmplifier(value));
    }

    private void applyTooltipStyle(SpecialDataApplyContext context) {
        if (this.handleUnchangedOrCleared(
                context,
                DataComponents.TOOLTIP_STYLE,
                Objects.equals(context.special().tooltipStyleId, context.baselineSpecial().tooltipStyleId),
                context.special().tooltipStyleId.isBlank()
        )) {
            return;
        }

        Identifier styleId = IdFieldNormalizer.parse(context.special().tooltipStyleId);
        if (styleId == null) {
            context.messages().add(ValidationMessage.error(ItemEditorText.str(
                    "preview.validation.component_failed",
                    ItemEditorText.str("special.advanced.component_tweaks.tooltip_style")
            )));
            return;
        }

        context.previewStack().set(DataComponents.TOOLTIP_STYLE, styleId);
    }

    private void applyDamageType(SpecialDataApplyContext context) {
        if (this.handleUnchangedOrCleared(
                context,
                DataComponents.DAMAGE_TYPE,
                Objects.equals(context.special().damageTypeId, context.baselineSpecial().damageTypeId),
                context.special().damageTypeId.isBlank()
        )) {
            return;
        }

        Registry<DamageType> damageTypeRegistry = context.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE);
        Holder<DamageType> damageType = RegistryUtil.resolveHolder(damageTypeRegistry, context.special().damageTypeId);
        if (damageType == null) {
            context.messages().add(ValidationMessage.error(ItemEditorText.str(
                    "validation.registry_missing",
                    ItemEditorText.str("special.advanced.component_tweaks.damage_type"),
                    context.special().damageTypeId
            )));
            return;
        }

        context.previewStack().set(DataComponents.DAMAGE_TYPE, damageType);
    }

    private void applyDamageResistant(SpecialDataApplyContext context) {
        if (this.handleUnchangedOrCleared(
                context,
                DataComponents.DAMAGE_RESISTANT,
                Objects.equals(context.special().damageResistantTypeIds, context.baselineSpecial().damageResistantTypeIds),
                context.special().damageResistantTypeIds.isBlank()
        )) {
            return;
        }

        Registry<DamageType> damageTypeRegistry = context.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE);
        List<Holder<DamageType>> types = new ArrayList<>();
        boolean hasInvalidEntry = false;
        for (String id : this.splitIdentifierList(context.special().damageResistantTypeIds)) {
            Holder<DamageType> holder = RegistryUtil.resolveHolder(damageTypeRegistry, id);
            if (holder == null) {
                hasInvalidEntry = true;
                context.messages().add(ValidationMessage.error(ItemEditorText.str(
                        "validation.registry_missing",
                        ItemEditorText.str("special.advanced.component_tweaks.damage_resistant_types"),
                        id
                )));
                continue;
            }
            types.add(holder);
        }

        if (types.isEmpty()) {
            if (!hasInvalidEntry) {
                this.clearToPrototype(context.previewStack(), DataComponents.DAMAGE_RESISTANT);
            }
            return;
        }

        context.previewStack().set(DataComponents.DAMAGE_RESISTANT, new DamageResistant(HolderSet.direct(types)));
    }

    private void applyNoteBlockSound(SpecialDataApplyContext context) {
        if (this.handleUnchangedOrCleared(
                context,
                DataComponents.NOTE_BLOCK_SOUND,
                Objects.equals(context.special().noteBlockSoundId, context.baselineSpecial().noteBlockSoundId),
                context.special().noteBlockSoundId.isBlank()
        )) {
            return;
        }

        Identifier soundId = IdFieldNormalizer.parse(context.special().noteBlockSoundId);
        if (soundId == null) {
            context.messages().add(ValidationMessage.error(ItemEditorText.str(
                    "preview.validation.component_failed",
                    ItemEditorText.str("special.advanced.component_tweaks.note_block_sound")
            )));
            return;
        }

        context.previewStack().set(DataComponents.NOTE_BLOCK_SOUND, soundId);
    }

    private void applyBreakSound(SpecialDataApplyContext context) {
        if (this.handleUnchangedOrCleared(
                context,
                DataComponents.BREAK_SOUND,
                Objects.equals(context.special().breakSoundId, context.baselineSpecial().breakSoundId),
                context.special().breakSoundId.isBlank()
        )) {
            return;
        }

        Registry<SoundEvent> soundRegistry = context.registryAccess().lookupOrThrow(Registries.SOUND_EVENT);
        Holder<SoundEvent> breakSound = this.resolveSoundHolder(
                soundRegistry,
                context.special().breakSoundId,
                null,
                ItemEditorText.str("special.advanced.component_tweaks.break_sound"),
                context.messages()
        );
        if (breakSound == null) {
            return;
        }

        context.previewStack().set(DataComponents.BREAK_SOUND, breakSound);
    }

    private void applyPaintingVariant(SpecialDataApplyContext context) {
        if (this.handleUnchangedOrCleared(
                context,
                DataComponents.PAINTING_VARIANT,
                Objects.equals(context.special().paintingVariantId, context.baselineSpecial().paintingVariantId),
                context.special().paintingVariantId.isBlank()
        )) {
            return;
        }

        Registry<PaintingVariant> paintingRegistry = context.registryAccess().lookupOrThrow(Registries.PAINTING_VARIANT);
        Holder<PaintingVariant> variant = RegistryUtil.resolveHolder(paintingRegistry, context.special().paintingVariantId);
        if (variant == null) {
            context.messages().add(ValidationMessage.error(ItemEditorText.str(
                    "validation.registry_missing",
                    ItemEditorText.str("special.advanced.component_tweaks.painting_variant"),
                    context.special().paintingVariantId
            )));
            return;
        }

        context.previewStack().set(DataComponents.PAINTING_VARIANT, variant);
    }

    private void applyBlockState(SpecialDataApplyContext context) {
        if (this.handleUnchangedOrCleared(
                context,
                DataComponents.BLOCK_STATE,
                Objects.equals(context.special().blockStateProperties, context.baselineSpecial().blockStateProperties),
                context.special().blockStateProperties.isBlank()
        )) {
            return;
        }

        LinkedHashMap<String, String> properties = this.parseBlockStateProperties(
                context.special().blockStateProperties,
                ItemEditorText.str("special.advanced.component_tweaks.block_state"),
                context.messages()
        );
        if (properties == null) {
            return;
        }

        if (properties.isEmpty()) {
            this.clearToPrototype(context.previewStack(), DataComponents.BLOCK_STATE);
            return;
        }

        context.previewStack().set(DataComponents.BLOCK_STATE, new BlockItemStateProperties(properties));
    }

    private void applyDeathProtection(SpecialDataApplyContext context) {
        if (this.handleUnchangedOrCleared(
                context,
                DataComponents.DEATH_PROTECTION,
                context.special().deathProtection == context.baselineSpecial().deathProtection,
                !context.special().deathProtection
        )) {
            return;
        }

        context.previewStack().set(DataComponents.DEATH_PROTECTION, DeathProtection.TOTEM_OF_UNDYING);
    }

    private void applyGlider(SpecialDataApplyContext context) {
        if (this.handleUnchangedOrCleared(
                context,
                DataComponents.GLIDER,
                context.special().glider == context.baselineSpecial().glider,
                !context.special().glider
        )) {
            return;
        }

        context.previewStack().set(DataComponents.GLIDER, Unit.INSTANCE);
    }

    private void applyIntangibleProjectile(SpecialDataApplyContext context) {
        if (this.handleUnchangedOrCleared(
                context,
                DataComponents.INTANGIBLE_PROJECTILE,
                context.special().intangibleProjectile == context.baselineSpecial().intangibleProjectile,
                !context.special().intangibleProjectile
        )) {
            return;
        }

        context.previewStack().set(DataComponents.INTANGIBLE_PROJECTILE, Unit.INSTANCE);
    }

    private void applyBlocksAttacks(SpecialDataApplyContext context) {
        if (this.handleUnchangedOrCleared(
                context,
                DataComponents.BLOCKS_ATTACKS,
                Objects.equals(context.special().blocksAttacksBlockDelaySeconds, context.baselineSpecial().blocksAttacksBlockDelaySeconds)
                        && Objects.equals(context.special().blocksAttacksDisableCooldownScale, context.baselineSpecial().blocksAttacksDisableCooldownScale)
                        && Objects.equals(context.special().blocksAttacksBlockSoundId, context.baselineSpecial().blocksAttacksBlockSoundId)
                        && Objects.equals(context.special().blocksAttacksDisableSoundId, context.baselineSpecial().blocksAttacksDisableSoundId),
                context.special().blocksAttacksBlockDelaySeconds.isBlank()
                        && context.special().blocksAttacksDisableCooldownScale.isBlank()
                        && context.special().blocksAttacksBlockSoundId.isBlank()
                        && context.special().blocksAttacksDisableSoundId.isBlank()
        )) {
            return;
        }

        BlocksAttacks original = context.originalStack().get(DataComponents.BLOCKS_ATTACKS);
        Float blockDelay = context.special().blocksAttacksBlockDelaySeconds.isBlank()
                ? this.valueFromOriginal(original, BlocksAttacks::blockDelaySeconds, 0.0F)
                : ValidationUtil.parseFloat(
                        context.special().blocksAttacksBlockDelaySeconds,
                        ItemEditorText.str("special.advanced.component_tweaks.blocks_attacks_delay"),
                        context.messages()
                );
        Float disableCooldownScale = context.special().blocksAttacksDisableCooldownScale.isBlank()
                ? this.valueFromOriginal(original, BlocksAttacks::disableCooldownScale, 1.0F)
                : ValidationUtil.parseFloat(
                        context.special().blocksAttacksDisableCooldownScale,
                        ItemEditorText.str("special.advanced.component_tweaks.blocks_attacks_disable_scale"),
                        context.messages()
                );
        if (blockDelay == null || disableCooldownScale == null || blockDelay < 0.0F || disableCooldownScale < 0.0F) {
            return;
        }

        Registry<SoundEvent> soundRegistry = context.registryAccess().lookupOrThrow(Registries.SOUND_EVENT);
        Optional<Holder<SoundEvent>> blockSound = this.resolveOptionalSoundHolder(
                soundRegistry,
                context.special().blocksAttacksBlockSoundId,
                this.valueFromOriginal(original, value -> value.blockSound().orElse(null), null),
                ItemEditorText.str("special.advanced.component_tweaks.blocks_attacks_block_sound"),
                context.messages()
        );
        Optional<Holder<SoundEvent>> disableSound = this.resolveOptionalSoundHolder(
                soundRegistry,
                context.special().blocksAttacksDisableSoundId,
                this.valueFromOriginal(original, value -> value.disableSound().orElse(null), null),
                ItemEditorText.str("special.advanced.component_tweaks.blocks_attacks_disable_sound"),
                context.messages()
        );
        if (this.hasInvalidOptionalInput(context.special().blocksAttacksBlockSoundId, blockSound.isPresent())
                || this.hasInvalidOptionalInput(context.special().blocksAttacksDisableSoundId, disableSound.isPresent())) {
            return;
        }

        List<BlocksAttacks.DamageReduction> damageReductions = this.valueFromOriginal(original, BlocksAttacks::damageReductions, List.of());
        BlocksAttacks.ItemDamageFunction itemDamage = this.valueFromOriginal(original, BlocksAttacks::itemDamage, BlocksAttacks.ItemDamageFunction.DEFAULT);
        Optional<HolderSet<DamageType>> bypassedBy = this.optionalFromOriginal(original, BlocksAttacks::bypassedBy);
        context.previewStack().set(
                DataComponents.BLOCKS_ATTACKS,
                new BlocksAttacks(blockDelay, disableCooldownScale, damageReductions, itemDamage, bypassedBy, blockSound, disableSound)
        );
    }

    private void applyPiercingWeapon(SpecialDataApplyContext context) {
        if (this.handleUnchangedOrCleared(
                context,
                DataComponents.PIERCING_WEAPON,
                context.special().piercingDealsKnockback == context.baselineSpecial().piercingDealsKnockback
                        && context.special().piercingDismounts == context.baselineSpecial().piercingDismounts
                        && Objects.equals(context.special().piercingSoundId, context.baselineSpecial().piercingSoundId)
                        && Objects.equals(context.special().piercingHitSoundId, context.baselineSpecial().piercingHitSoundId),
                !context.special().piercingDealsKnockback
                        && !context.special().piercingDismounts
                        && context.special().piercingSoundId.isBlank()
                        && context.special().piercingHitSoundId.isBlank()
        )) {
            return;
        }

        PiercingWeapon original = context.originalStack().get(DataComponents.PIERCING_WEAPON);
        Registry<SoundEvent> soundRegistry = context.registryAccess().lookupOrThrow(Registries.SOUND_EVENT);
        Optional<Holder<SoundEvent>> sound = this.resolveOptionalSoundHolder(
                soundRegistry,
                context.special().piercingSoundId,
                this.valueFromOriginal(original, value -> value.sound().orElse(null), null),
                ItemEditorText.str("special.advanced.component_tweaks.piercing_sound"),
                context.messages()
        );
        Optional<Holder<SoundEvent>> hitSound = this.resolveOptionalSoundHolder(
                soundRegistry,
                context.special().piercingHitSoundId,
                this.valueFromOriginal(original, value -> value.hitSound().orElse(null), null),
                ItemEditorText.str("special.advanced.component_tweaks.piercing_hit_sound"),
                context.messages()
        );
        if (this.hasInvalidOptionalInput(context.special().piercingSoundId, sound.isPresent())
                || this.hasInvalidOptionalInput(context.special().piercingHitSoundId, hitSound.isPresent())) {
            return;
        }

        context.previewStack().set(
                DataComponents.PIERCING_WEAPON,
                new PiercingWeapon(context.special().piercingDealsKnockback, context.special().piercingDismounts, sound, hitSound)
        );
    }

    private void applyKineticWeapon(SpecialDataApplyContext context) {
        if (this.handleUnchangedOrCleared(
                context,
                DataComponents.KINETIC_WEAPON,
                Objects.equals(context.special().kineticContactCooldownTicks, context.baselineSpecial().kineticContactCooldownTicks)
                        && Objects.equals(context.special().kineticDelayTicks, context.baselineSpecial().kineticDelayTicks)
                        && Objects.equals(context.special().kineticForwardMovement, context.baselineSpecial().kineticForwardMovement)
                        && Objects.equals(context.special().kineticDamageMultiplier, context.baselineSpecial().kineticDamageMultiplier)
                        && Objects.equals(context.special().kineticSoundId, context.baselineSpecial().kineticSoundId)
                        && Objects.equals(context.special().kineticHitSoundId, context.baselineSpecial().kineticHitSoundId),
                context.special().kineticContactCooldownTicks.isBlank()
                        && context.special().kineticDelayTicks.isBlank()
                        && context.special().kineticForwardMovement.isBlank()
                        && context.special().kineticDamageMultiplier.isBlank()
                        && context.special().kineticSoundId.isBlank()
                        && context.special().kineticHitSoundId.isBlank()
        )) {
            return;
        }

        KineticWeapon original = context.originalStack().get(DataComponents.KINETIC_WEAPON);
        Integer contactCooldownTicks = context.special().kineticContactCooldownTicks.isBlank()
                ? this.valueFromOriginal(original, KineticWeapon::contactCooldownTicks, 0)
                : ValidationUtil.parseInt(
                        context.special().kineticContactCooldownTicks,
                        ItemEditorText.str("special.advanced.component_tweaks.kinetic_contact_cooldown"),
                        0,
                        72000,
                        context.messages()
                );
        Integer delayTicks = context.special().kineticDelayTicks.isBlank()
                ? this.valueFromOriginal(original, KineticWeapon::delayTicks, 0)
                : ValidationUtil.parseInt(
                        context.special().kineticDelayTicks,
                        ItemEditorText.str("special.advanced.component_tweaks.kinetic_delay_ticks"),
                        0,
                        72000,
                        context.messages()
                );
        Float forwardMovement = context.special().kineticForwardMovement.isBlank()
                ? this.valueFromOriginal(original, KineticWeapon::forwardMovement, 0.0F)
                : ValidationUtil.parseFloat(
                        context.special().kineticForwardMovement,
                        ItemEditorText.str("special.advanced.component_tweaks.kinetic_forward_movement"),
                        context.messages()
                );
        Float damageMultiplier = context.special().kineticDamageMultiplier.isBlank()
                ? this.valueFromOriginal(original, KineticWeapon::damageMultiplier, 1.0F)
                : ValidationUtil.parseFloat(
                        context.special().kineticDamageMultiplier,
                        ItemEditorText.str("special.advanced.component_tweaks.kinetic_damage_multiplier"),
                        context.messages()
                );
        if (contactCooldownTicks == null
                || delayTicks == null
                || forwardMovement == null
                || damageMultiplier == null
                || forwardMovement < 0.0F
                || damageMultiplier < 0.0F) {
            return;
        }

        Registry<SoundEvent> soundRegistry = context.registryAccess().lookupOrThrow(Registries.SOUND_EVENT);
        Optional<Holder<SoundEvent>> sound = this.resolveOptionalSoundHolder(
                soundRegistry,
                context.special().kineticSoundId,
                this.valueFromOriginal(original, value -> value.sound().orElse(null), null),
                ItemEditorText.str("special.advanced.component_tweaks.kinetic_sound"),
                context.messages()
        );
        Optional<Holder<SoundEvent>> hitSound = this.resolveOptionalSoundHolder(
                soundRegistry,
                context.special().kineticHitSoundId,
                this.valueFromOriginal(original, value -> value.hitSound().orElse(null), null),
                ItemEditorText.str("special.advanced.component_tweaks.kinetic_hit_sound"),
                context.messages()
        );
        if (this.hasInvalidOptionalInput(context.special().kineticSoundId, sound.isPresent())
                || this.hasInvalidOptionalInput(context.special().kineticHitSoundId, hitSound.isPresent())) {
            return;
        }

        context.previewStack().set(
                DataComponents.KINETIC_WEAPON,
                new KineticWeapon(
                        contactCooldownTicks,
                        delayTicks,
                        this.optionalFromOriginal(original, KineticWeapon::dismountConditions),
                        this.optionalFromOriginal(original, KineticWeapon::knockbackConditions),
                        this.optionalFromOriginal(original, KineticWeapon::damageConditions),
                        forwardMovement,
                        damageMultiplier,
                        sound,
                        hitSound
                )
        );
    }

    private void applySwingAnimation(SpecialDataApplyContext context) {
        if (this.handleUnchangedOrCleared(
                context,
                DataComponents.SWING_ANIMATION,
                Objects.equals(context.special().swingAnimationType, context.baselineSpecial().swingAnimationType)
                        && Objects.equals(context.special().swingAnimationDuration, context.baselineSpecial().swingAnimationDuration),
                context.special().swingAnimationType.isBlank() && context.special().swingAnimationDuration.isBlank()
        )) {
            return;
        }

        SwingAnimation original = context.originalStack().get(DataComponents.SWING_ANIMATION);
        SwingAnimationType type = this.parseSwingAnimationType(
                context.special().swingAnimationType,
                this.valueFromOriginal(original, SwingAnimation::type, SwingAnimation.DEFAULT.type()),
                context.messages()
        );
        Integer duration = context.special().swingAnimationDuration.isBlank()
                ? this.valueFromOriginal(original, SwingAnimation::duration, SwingAnimation.DEFAULT.duration())
                : ValidationUtil.parseInt(
                        context.special().swingAnimationDuration,
                        ItemEditorText.str("special.advanced.component_tweaks.swing_animation_duration"),
                        0,
                        72000,
                        context.messages()
                );
        if (type == null || duration == null) {
            return;
        }

        context.previewStack().set(DataComponents.SWING_ANIMATION, new SwingAnimation(type, duration));
    }

    private <T> boolean handleUnchangedOrCleared(
            SpecialDataApplyContext context,
            DataComponentType<T> componentType,
            boolean unchanged,
            boolean cleared
    ) {
        if (unchanged) {
            this.restoreOriginalComponent(context.originalStack(), context.previewStack(), componentType);
            return true;
        }
        if (cleared) {
            this.clearToPrototype(context.previewStack(), componentType);
            return true;
        }
        return false;
    }

    private SwingAnimationType parseSwingAnimationType(
            String rawType,
            SwingAnimationType fallback,
            List<ValidationMessage> messages
    ) {
        if (rawType == null || rawType.isBlank()) {
            return fallback;
        }
        try {
            return SwingAnimationType.valueOf(rawType);
        } catch (IllegalArgumentException exception) {
            messages.add(ValidationMessage.error(ItemEditorText.str(
                    "preview.validation.component_failed",
                    ItemEditorText.str("special.advanced.component_tweaks.swing_animation_type")
            )));
            return null;
        }
    }

    private Optional<Holder<SoundEvent>> resolveOptionalSoundHolder(
            Registry<SoundEvent> registry,
            String rawId,
            Holder<SoundEvent> fallback,
            String label,
            List<ValidationMessage> messages
    ) {
        if (rawId == null || rawId.isBlank()) {
            return Optional.ofNullable(fallback);
        }
        Holder<SoundEvent> holder = this.resolveSoundHolder(registry, rawId, null, label, messages);
        return Optional.ofNullable(holder);
    }

    private <T, R> R valueFromOriginal(T original, Function<T, R> extractor, R fallback) {
        return original == null ? fallback : extractor.apply(original);
    }

    private <T, R> Optional<R> optionalFromOriginal(T original, Function<T, Optional<R>> extractor) {
        return original == null ? Optional.empty() : extractor.apply(original);
    }

    private List<String> splitIdentifierList(String raw) {
        List<String> values = new ArrayList<>();
        for (String part : raw.split("[,\\r\\n]+")) {
            String normalized = part.trim();
            if (!normalized.isEmpty()) {
                values.add(normalized);
            }
        }
        return values;
    }

    private LinkedHashMap<String, String> parseBlockStateProperties(
            String raw,
            String label,
            List<ValidationMessage> messages
    ) {
        LinkedHashMap<String, String> properties = new LinkedHashMap<>();
        for (String part : raw.split("[,\\r\\n]+")) {
            String token = part.trim();
            if (token.isEmpty()) {
                continue;
            }

            int separator = token.indexOf('=');
            if (separator <= 0 || separator >= token.length() - 1) {
                messages.add(ValidationMessage.error(ItemEditorText.str("preview.validation.component_failed", label)));
                return null;
            }

            String key = token.substring(0, separator).trim();
            String value = token.substring(separator + 1).trim();
            if (key.isEmpty() || value.isEmpty()) {
                messages.add(ValidationMessage.error(ItemEditorText.str("preview.validation.component_failed", label)));
                return null;
            }
            properties.put(key, value);
        }
        return properties;
    }

    private Float readRangeField(String raw, float fallback, String label, List<ValidationMessage> messages) {
        if (raw.isBlank()) {
            return fallback;
        }
        Float parsed = ValidationUtil.parseFloat(raw, label, messages);
        if (parsed == null || parsed < 0.0F) {
            return null;
        }
        return parsed;
    }

    private Item resolveItem(String rawId, String label, List<ValidationMessage> messages) {
        Identifier identifier = IdFieldNormalizer.parse(rawId);
        if (identifier == null) {
            this.reportMissingRegistry(label, rawId, messages);
            return null;
        }

        Item item = BuiltInRegistries.ITEM.getOptional(identifier).orElse(null);
        if (item == null || item == Items.AIR) {
            this.reportMissingRegistry(label, rawId, messages);
            return null;
        }
        return item;
    }

    private Optional<Item> resolveOptionalItem(String rawId, String label, List<ValidationMessage> messages) {
        if (rawId == null || rawId.isBlank()) {
            return Optional.empty();
        }
        Item resolved = this.resolveItem(rawId, label, messages);
        return Optional.ofNullable(resolved);
    }

    private boolean hasInvalidOptionalInput(String rawId, boolean resolvedPresent) {
        return rawId != null && !rawId.isBlank() && !resolvedPresent;
    }

    private void reportMissingRegistry(String label, String rawId, List<ValidationMessage> messages) {
        messages.add(ValidationMessage.error(ItemEditorText.str("validation.registry_missing", label, rawId)));
    }

    private ItemUseAnimation parseAnimation(
            String rawAnimation,
            ItemUseAnimation fallback,
            List<ValidationMessage> messages
    ) {
        if (rawAnimation == null || rawAnimation.isBlank()) {
            return fallback;
        }
        try {
            return ItemUseAnimation.valueOf(rawAnimation);
        } catch (IllegalArgumentException exception) {
            messages.add(ValidationMessage.error(ItemEditorText.str("preview.validation.component_failed", ItemEditorText.str("special.advanced.consumable.animation"))));
            return null;
        }
    }

    private Holder<SoundEvent> resolveSoundHolder(
            Registry<SoundEvent> registry,
            String rawId,
            Holder<SoundEvent> fallback,
            String label,
            List<ValidationMessage> messages
    ) {
        if (rawId == null || rawId.isBlank()) {
            return fallback;
        }

        Holder<SoundEvent> holder = RegistryUtil.resolveHolder(registry, rawId);
        if (holder == null) {
            this.reportMissingRegistry(label, rawId, messages);
            return null;
        }
        return holder;
    }

    private Holder<SoundEvent> defaultConsumeSound(ItemUseAnimation animation) {
        return animation == ItemUseAnimation.DRINK ? SoundEvents.GENERIC_DRINK : SoundEvents.GENERIC_EAT;
    }

    private boolean sameChargedProjectiles(
            List<ItemEditorState.ChargedProjectileDraft> current,
            List<ItemEditorState.ChargedProjectileDraft> baseline
    ) {
        return this.sameList(current, baseline, (left, right) ->
                Objects.equals(left.itemId, right.itemId) && Objects.equals(left.count, right.count)
        );
    }

    private boolean sameBeeOccupants(
            List<ItemEditorState.BeeOccupantDraft> current,
            List<ItemEditorState.BeeOccupantDraft> baseline
    ) {
        return this.sameList(current, baseline, (left, right) ->
                Objects.equals(left.entityId, right.entityId)
                        && Objects.equals(left.ticksInHive, right.ticksInHive)
                        && Objects.equals(left.minTicksInHive, right.minTicksInHive)
        );
    }

    private boolean sameMapDecorations(
            List<ItemEditorState.MapDecorationDraft> current,
            List<ItemEditorState.MapDecorationDraft> baseline
    ) {
        return this.sameList(current, baseline, (left, right) ->
                Objects.equals(left.key, right.key)
                        && Objects.equals(left.typeId, right.typeId)
                        && Objects.equals(left.x, right.x)
                        && Objects.equals(left.z, right.z)
                        && Objects.equals(left.rotation, right.rotation)
        );
    }

    private boolean sameConsumableEffects(
            List<ItemEditorState.ConsumableEffectDraft> current,
            List<ItemEditorState.ConsumableEffectDraft> baseline
    ) {
        return this.sameList(current, baseline, (left, right) ->
                Objects.equals(left.type, right.type)
                        && Objects.equals(left.probability, right.probability)
                        && Objects.equals(left.soundId, right.soundId)
                        && this.sameList(left.effects, right.effects, (leftEffect, rightEffect) ->
                                Objects.equals(leftEffect.effectId, rightEffect.effectId)
                                        && Objects.equals(leftEffect.duration, rightEffect.duration)
                                        && Objects.equals(leftEffect.amplifier, rightEffect.amplifier)
                                        && leftEffect.ambient == rightEffect.ambient
                                        && leftEffect.visible == rightEffect.visible
                                        && leftEffect.showIcon == rightEffect.showIcon
                        )
        );
    }

    private List<ConsumeEffect> parseConsumableEffects(SpecialDataApplyContext context) {
        List<ConsumeEffect> effects = new ArrayList<>();
        Registry<MobEffect> effectRegistry = context.registryAccess().lookupOrThrow(Registries.MOB_EFFECT);
        Registry<SoundEvent> soundRegistry = context.registryAccess().lookupOrThrow(Registries.SOUND_EVENT);

        for (int index = 0; index < context.special().consumableOnConsumeEffects.size(); index++) {
            ItemEditorState.ConsumableEffectDraft draft = context.special().consumableOnConsumeEffects.get(index);
            String normalizedType = draft.type == null ? "" : draft.type.trim();
            if (normalizedType.isBlank()) {
                normalizedType = ItemEditorState.ConsumableEffectDraft.TYPE_APPLY_EFFECTS;
            }

            if (Objects.equals(normalizedType, ItemEditorState.ConsumableEffectDraft.TYPE_APPLY_EFFECTS)) {
                Float probability = draft.probability.isBlank()
                        ? Float.valueOf(1.0F)
                        : ValidationUtil.parseFloat(
                        draft.probability,
                        ItemEditorText.str("special.advanced.consumable.effect_probability"),
                        context.messages()
                );
                if (probability == null) {
                    return null;
                }
                if (probability < 0.0F || probability > 1.0F) {
                    context.messages().add(ValidationMessage.error(ItemEditorText.str(
                            "preview.validation.component_failed",
                            ItemEditorText.str("special.advanced.consumable.effect_probability")
                    )));
                    return null;
                }

                List<MobEffectInstance> mobEffects = this.parsePotionEffectInstances(
                        draft.effects,
                        effectRegistry,
                        context.messages()
                );

                if (mobEffects.isEmpty()) {
                    context.messages().add(ValidationMessage.error(ItemEditorText.str(
                            "preview.validation.component_failed",
                            ItemEditorText.str("special.advanced.consumable.on_consume_effects")
                    )));
                    return null;
                }

                effects.add(new ApplyStatusEffectsConsumeEffect(mobEffects, probability));
                continue;
            }

            if (Objects.equals(normalizedType, ItemEditorState.ConsumableEffectDraft.TYPE_PLAY_SOUND)) {
                Holder<SoundEvent> effectSound = this.resolveSoundHolder(
                        soundRegistry,
                        draft.soundId,
                        null,
                        ItemEditorText.str("special.advanced.consumable.effect_sound"),
                        context.messages()
                );
                if (effectSound == null) {
                    return null;
                }
                effects.add(new PlaySoundConsumeEffect(effectSound));
                continue;
            }

            context.messages().add(ValidationMessage.error(ItemEditorText.str(
                    "preview.validation.component_failed",
                    ItemEditorText.str("special.advanced.consumable.effect_type")
            )));
            return null;
        }

        return effects;
    }
}
