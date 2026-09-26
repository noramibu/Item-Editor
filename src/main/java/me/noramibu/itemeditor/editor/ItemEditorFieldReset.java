package me.noramibu.itemeditor.editor;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;

public final class ItemEditorFieldReset {
    public record Binding<T>(Function<ItemEditorState, T> read, BiConsumer<ItemEditorState, T> write) {
        boolean changed(ItemEditorState state, ItemEditorState original) {
            return !Objects.equals(read.apply(state), read.apply(original));
        }

        void restore(ItemEditorState state, ItemEditorState original) {
            write.accept(state, read.apply(original));
        }
    }

    private static final Map<String, Binding<String>> VARIANTS = Map.ofEntries(
            textField(
                    "special.entity_variant.axolotl_variant",
                    state -> state.special.bucketAxolotlVariant,
                    (state, value) -> state.special.bucketAxolotlVariant = value),
            textField(
                    "special.entity_variant.cat_variant",
                    state -> state.special.entityCatVariant,
                    (state, value) -> state.special.entityCatVariant = value),
            textField(
                    "special.entity_variant.cat_sound_variant",
                    state -> state.special.entityCatSoundVariant,
                    (state, value) -> state.special.entityCatSoundVariant = value),
            textField(
                    "special.entity_variant.cat_collar",
                    state -> state.special.entityCatCollar,
                    (state, value) -> state.special.entityCatCollar = value),
            textField(
                    "special.entity_variant.chicken_variant",
                    state -> state.special.entityChickenVariant,
                    (state, value) -> state.special.entityChickenVariant = value),
            textField(
                    "special.entity_variant.chicken_sound_variant",
                    state -> state.special.entityChickenSoundVariant,
                    (state, value) -> state.special.entityChickenSoundVariant = value),
            textField(
                    "special.entity_variant.cow_variant",
                    state -> state.special.entityCowVariant,
                    (state, value) -> state.special.entityCowVariant = value),
            textField(
                    "special.entity_variant.cow_sound_variant",
                    state -> state.special.entityCowSoundVariant,
                    (state, value) -> state.special.entityCowSoundVariant = value),
            textField(
                    "special.entity_variant.fox_variant",
                    state -> state.special.entityFoxVariant,
                    (state, value) -> state.special.entityFoxVariant = value),
            textField(
                    "special.entity_variant.frog_variant",
                    state -> state.special.entityFrogVariant,
                    (state, value) -> state.special.entityFrogVariant = value),
            textField(
                    "special.entity_variant.horse_variant",
                    state -> state.special.entityHorseVariant,
                    (state, value) -> state.special.entityHorseVariant = value),
            textField(
                    "special.entity_variant.llama_variant",
                    state -> state.special.entityLlamaVariant,
                    (state, value) -> state.special.entityLlamaVariant = value),
            textField(
                    "special.entity_variant.mooshroom_variant",
                    state -> state.special.entityMooshroomVariant,
                    (state, value) -> state.special.entityMooshroomVariant = value),
            textField(
                    "special.entity_variant.painting_variant",
                    state -> state.special.paintingVariantId,
                    (state, value) -> state.special.paintingVariantId = value),
            textField(
                    "special.entity_variant.parrot_variant",
                    state -> state.special.entityParrotVariant,
                    (state, value) -> state.special.entityParrotVariant = value),
            textField(
                    "special.entity_variant.pig_variant",
                    state -> state.special.entityPigVariant,
                    (state, value) -> state.special.entityPigVariant = value),
            textField(
                    "special.entity_variant.pig_sound_variant",
                    state -> state.special.entityPigSoundVariant,
                    (state, value) -> state.special.entityPigSoundVariant = value),
            textField(
                    "special.entity_variant.rabbit_variant",
                    state -> state.special.entityRabbitVariant,
                    (state, value) -> state.special.entityRabbitVariant = value),
            textField(
                    "special.entity_variant.salmon_size",
                    state -> state.special.bucketSalmonSize,
                    (state, value) -> state.special.bucketSalmonSize = value),
            textField(
                    "special.entity_variant.sheep_color",
                    state -> state.special.entitySheepColor,
                    (state, value) -> state.special.entitySheepColor = value),
            textField(
                    "special.entity_variant.shulker_color",
                    state -> state.special.entityShulkerColor,
                    (state, value) -> state.special.entityShulkerColor = value),
            textField(
                    "special.entity_variant.tropical_pattern",
                    state -> state.special.bucketTropicalPattern,
                    (state, value) -> state.special.bucketTropicalPattern = value),
            textField(
                    "special.entity_variant.tropical_base_color",
                    state -> state.special.bucketTropicalBaseColor,
                    (state, value) -> state.special.bucketTropicalBaseColor = value),
            textField(
                    "special.entity_variant.tropical_pattern_color",
                    state -> state.special.bucketTropicalPatternColor,
                    (state, value) -> state.special.bucketTropicalPatternColor = value),
            textField(
                    "special.entity_variant.villager_variant",
                    state -> state.special.entityVillagerVariant,
                    (state, value) -> state.special.entityVillagerVariant = value),
            textField(
                    "special.entity_variant.wolf_variant",
                    state -> state.special.entityWolfVariant,
                    (state, value) -> state.special.entityWolfVariant = value),
            textField(
                    "special.entity_variant.wolf_sound_variant",
                    state -> state.special.entityWolfSoundVariant,
                    (state, value) -> state.special.entityWolfSoundVariant = value),
            textField(
                    "special.entity_variant.wolf_collar",
                    state -> state.special.entityWolfCollar,
                    (state, value) -> state.special.entityWolfCollar = value),
            textField(
                    "special.entity_variant.zombie_nautilus_variant",
                    state -> state.special.entityZombieNautilusVariant,
                    (state, value) -> state.special.entityZombieNautilusVariant = value));

    public static Binding<String> variant(String name) {
        return Objects.requireNonNull(VARIANTS.get("itemeditor.special.entity_variant." + name), name);
    }

    private static Map.Entry<String, Binding<String>> textField(
            String key, Function<ItemEditorState, String> read, BiConsumer<ItemEditorState, String> write) {
        return Map.entry("itemeditor." + key, new Binding<>(read, write));
    }

    private static final Map<String, Binding<String>> TEXT_FIELDS = Map.ofEntries(
            textField(
                    "special.advanced.container_meta.loot_table",
                    state -> state.special.containerLootTableId,
                    (state, value) -> state.special.containerLootTableId = value),
            textField(
                    "special.advanced.container_meta.pot_back",
                    state -> state.special.potBackItemId,
                    (state, value) -> state.special.potBackItemId = value),
            textField(
                    "special.advanced.container_meta.pot_left",
                    state -> state.special.potLeftItemId,
                    (state, value) -> state.special.potLeftItemId = value),
            textField(
                    "special.advanced.container_meta.pot_right",
                    state -> state.special.potRightItemId,
                    (state, value) -> state.special.potRightItemId = value),
            textField(
                    "special.advanced.container_meta.pot_front",
                    state -> state.special.potFrontItemId,
                    (state, value) -> state.special.potFrontItemId = value),
            textField(
                    "special.advanced.combat.piercing_sound",
                    state -> state.special.piercingSoundId,
                    (state, value) -> state.special.piercingSoundId = value),
            textField(
                    "special.advanced.combat.piercing_hit_sound",
                    state -> state.special.piercingHitSoundId,
                    (state, value) -> state.special.piercingHitSoundId = value),
            textField(
                    "special.advanced.combat.kinetic_sound",
                    state -> state.special.kineticSoundId,
                    (state, value) -> state.special.kineticSoundId = value),
            textField(
                    "special.advanced.combat.kinetic_hit_sound",
                    state -> state.special.kineticHitSoundId,
                    (state, value) -> state.special.kineticHitSoundId = value),
            textField(
                    "special.advanced.combat.blocks_attacks_block_sound",
                    state -> state.special.blocksAttacksBlockSoundId,
                    (state, value) -> state.special.blocksAttacksBlockSoundId = value),
            textField(
                    "special.advanced.combat.blocks_attacks_disable_sound",
                    state -> state.special.blocksAttacksDisableSoundId,
                    (state, value) -> state.special.blocksAttacksDisableSoundId = value),
            textField(
                    "special.advanced.component_tweaks.equippable_sound",
                    state -> state.special.equippableEquipSoundId,
                    (state, value) -> state.special.equippableEquipSoundId = value),
            textField(
                    "special.advanced.component_tweaks.equippable_shearing_sound",
                    state -> state.special.equippableShearingSoundId,
                    (state, value) -> state.special.equippableShearingSoundId = value),
            textField(
                    "special.advanced.container_meta.loot_seed",
                    state -> state.special.containerLootSeed,
                    (state, value) -> state.special.containerLootSeed = value),
            textField(
                    "special.advanced.map.map_id",
                    state -> state.special.mapId,
                    (state, value) -> state.special.mapId = value),
            textField(
                    "special.advanced.map.lodestone_x",
                    state -> state.special.lodestoneX,
                    (state, value) -> state.special.lodestoneX = value),
            textField(
                    "special.advanced.map.lodestone_y",
                    state -> state.special.lodestoneY,
                    (state, value) -> state.special.lodestoneY = value),
            textField(
                    "special.advanced.map.lodestone_z",
                    state -> state.special.lodestoneZ,
                    (state, value) -> state.special.lodestoneZ = value),
            textField(
                    "special.bucket.age",
                    state -> state.special.bucketAge,
                    (state, value) -> state.special.bucketAge = value),
            textField(
                    "special.bucket.hunting_cooldown",
                    state -> state.special.bucketHuntingCooldown,
                    (state, value) -> state.special.bucketHuntingCooldown = value),
            textField(
                    "special.advanced.combat.swing_animation_duration",
                    state -> state.special.swingAnimationDuration,
                    (state, value) -> state.special.swingAnimationDuration = value),
            textField(
                    "special.advanced.combat.kinetic_contact_cooldown",
                    state -> state.special.kineticContactCooldownTicks,
                    (state, value) -> state.special.kineticContactCooldownTicks = value),
            textField(
                    "special.advanced.combat.kinetic_delay_ticks",
                    state -> state.special.kineticDelayTicks,
                    (state, value) -> state.special.kineticDelayTicks = value),
            textField(
                    "special.advanced.combat.kinetic_forward_movement",
                    state -> state.special.kineticForwardMovement,
                    (state, value) -> state.special.kineticForwardMovement = value),
            textField(
                    "special.advanced.combat.kinetic_damage_multiplier",
                    state -> state.special.kineticDamageMultiplier,
                    (state, value) -> state.special.kineticDamageMultiplier = value),
            textField(
                    "special.advanced.combat.blocks_attacks_delay",
                    state -> state.special.blocksAttacksBlockDelaySeconds,
                    (state, value) -> state.special.blocksAttacksBlockDelaySeconds = value),
            textField(
                    "special.advanced.combat.blocks_attacks_disable_scale",
                    state -> state.special.blocksAttacksDisableCooldownScale,
                    (state, value) -> state.special.blocksAttacksDisableCooldownScale = value),
            textField(
                    "special.advanced.combat.blocks_attacks_item_damage_threshold",
                    state -> state.special.blocksAttacksItemDamageThreshold,
                    (state, value) -> state.special.blocksAttacksItemDamageThreshold = value),
            textField(
                    "special.advanced.combat.blocks_attacks_item_damage_base",
                    state -> state.special.blocksAttacksItemDamageBase,
                    (state, value) -> state.special.blocksAttacksItemDamageBase = value),
            textField(
                    "special.advanced.combat.blocks_attacks_item_damage_factor",
                    state -> state.special.blocksAttacksItemDamageFactor,
                    (state, value) -> state.special.blocksAttacksItemDamageFactor = value),
            textField(
                    "special.advanced.combat.weapon_damage",
                    state -> state.special.weaponItemDamagePerAttack,
                    (state, value) -> state.special.weaponItemDamagePerAttack = value),
            textField(
                    "special.advanced.combat.weapon_disable",
                    state -> state.special.weaponDisableBlockingForSeconds,
                    (state, value) -> state.special.weaponDisableBlockingForSeconds = value),
            textField(
                    "special.advanced.combat.tool_speed",
                    state -> state.special.toolDefaultMiningSpeed,
                    (state, value) -> state.special.toolDefaultMiningSpeed = value),
            textField(
                    "special.advanced.combat.tool_damage",
                    state -> state.special.toolDamagePerBlock,
                    (state, value) -> state.special.toolDamagePerBlock = value),
            textField(
                    "special.advanced.combat.range_min_reach",
                    state -> state.special.attackRangeMinReach,
                    (state, value) -> state.special.attackRangeMinReach = value),
            textField(
                    "special.advanced.combat.range_max_reach",
                    state -> state.special.attackRangeMaxReach,
                    (state, value) -> state.special.attackRangeMaxReach = value),
            textField(
                    "special.advanced.combat.range_min_creative",
                    state -> state.special.attackRangeMinCreativeReach,
                    (state, value) -> state.special.attackRangeMinCreativeReach = value),
            textField(
                    "special.advanced.combat.range_max_creative",
                    state -> state.special.attackRangeMaxCreativeReach,
                    (state, value) -> state.special.attackRangeMaxCreativeReach = value),
            textField(
                    "special.advanced.combat.range_hitbox",
                    state -> state.special.attackRangeHitboxMargin,
                    (state, value) -> state.special.attackRangeHitboxMargin = value),
            textField(
                    "special.advanced.combat.range_mob_factor",
                    state -> state.special.attackRangeMobFactor,
                    (state, value) -> state.special.attackRangeMobFactor = value),
            textField(
                    "special.firework.flight_duration",
                    state -> state.special.fireworkFlightDuration,
                    (state, value) -> state.special.fireworkFlightDuration = value),
            textField(
                    "special.misc.profile.name",
                    state -> state.special.profileName,
                    (state, value) -> state.special.profileName = value),
            textField(
                    "special.misc.profile.uuid",
                    state -> state.special.profileUuid,
                    (state, value) -> state.special.profileUuid = value),
            textField(
                    "special.misc.profile.texture_signature",
                    state -> state.special.profileTextureSignature,
                    (state, value) -> state.special.profileTextureSignature = value),
            textField(
                    "special.misc.instrument.use_duration",
                    state -> state.special.instrumentUseDuration,
                    (state, value) -> state.special.instrumentUseDuration = value),
            textField(
                    "special.misc.instrument.range",
                    state -> state.special.instrumentRange,
                    (state, value) -> state.special.instrumentRange = value),
            textField(
                    "special.potion.duration_scale",
                    state -> state.special.potionDurationScale,
                    (state, value) -> state.special.potionDurationScale = value),
            textField(
                    "special.potion.custom_name",
                    state -> state.special.potionCustomName,
                    (state, value) -> state.special.potionCustomName = value),
            textField(
                    "special.spawn_egg.villager.level",
                    state -> state.special.spawnEggVillagerLevel,
                    (state, value) -> state.special.spawnEggVillagerLevel = value),
            textField(
                    "special.command_block.success_count",
                    state -> state.special.commandBlockSuccessCount,
                    (state, value) -> state.special.commandBlockSuccessCount = value),
            textField(
                    "special.command_block.last_execution",
                    state -> state.special.commandBlockLastExecution,
                    (state, value) -> state.special.commandBlockLastExecution = value),
            textField(
                    "special.spawner.delay",
                    state -> state.special.spawnerDelay,
                    (state, value) -> state.special.spawnerDelay = value),
            textField(
                    "special.spawner.min_spawn_delay",
                    state -> state.special.spawnerMinSpawnDelay,
                    (state, value) -> state.special.spawnerMinSpawnDelay = value),
            textField(
                    "special.spawner.max_spawn_delay",
                    state -> state.special.spawnerMaxSpawnDelay,
                    (state, value) -> state.special.spawnerMaxSpawnDelay = value),
            textField(
                    "special.spawner.spawn_count",
                    state -> state.special.spawnerSpawnCount,
                    (state, value) -> state.special.spawnerSpawnCount = value),
            textField(
                    "special.spawner.max_nearby_entities",
                    state -> state.special.spawnerMaxNearbyEntities,
                    (state, value) -> state.special.spawnerMaxNearbyEntities = value),
            textField(
                    "special.spawner.required_player_range",
                    state -> state.special.spawnerRequiredPlayerRange,
                    (state, value) -> state.special.spawnerRequiredPlayerRange = value),
            textField(
                    "special.spawner.spawn_range",
                    state -> state.special.spawnerSpawnRange,
                    (state, value) -> state.special.spawnerSpawnRange = value),
            textField(
                    "special.advanced.food.nutrition",
                    state -> state.special.foodNutrition,
                    (state, value) -> state.special.foodNutrition = value),
            textField(
                    "special.advanced.food.saturation",
                    state -> state.special.foodSaturation,
                    (state, value) -> state.special.foodSaturation = value),
            textField(
                    "special.advanced.consumable.consume_seconds",
                    state -> state.special.consumableConsumeSeconds,
                    (state, value) -> state.special.consumableConsumeSeconds = value),
            textField(
                    "special.advanced.use_effects.speed_multiplier",
                    state -> state.special.useEffectsSpeedMultiplier,
                    (state, value) -> state.special.useEffectsSpeedMultiplier = value),
            textField(
                    "special.advanced.use_cooldown.seconds",
                    state -> state.special.useCooldownSeconds,
                    (state, value) -> state.special.useCooldownSeconds = value),
            textField(
                    "special.advanced.use_cooldown.group",
                    state -> state.special.useCooldownGroup,
                    (state, value) -> state.special.useCooldownGroup = value),
            textField(
                    "special.advanced.component_tweaks.note_block_sound",
                    state -> state.special.noteBlockSoundId,
                    (state, value) -> state.special.noteBlockSoundId = value),
            textField(
                    "special.advanced.component_tweaks.break_sound",
                    state -> state.special.breakSoundId,
                    (state, value) -> state.special.breakSoundId = value),
            textField(
                    "special.advanced.component_tweaks.provides_banner_patterns",
                    state -> state.special.providesBannerPatternsTagId,
                    (state, value) -> state.special.providesBannerPatternsTagId = value),
            textField(
                    "special.advanced.component_tweaks.provides_trim_material",
                    state -> state.special.providesTrimMaterialId,
                    (state, value) -> state.special.providesTrimMaterialId = value),
            textField(
                    "special.advanced.component_tweaks.painting_variant",
                    state -> state.special.paintingVariantId,
                    (state, value) -> state.special.paintingVariantId = value),
            textField(
                    "special.advanced.component_tweaks.damage_type",
                    state -> state.special.damageTypeId,
                    (state, value) -> state.special.damageTypeId = value),
            textField(
                    "general.identity.custom_name.placeholder",
                    state -> state.customName,
                    (state, value) -> state.customName = value),
            textField("general.stack_count", state -> state.count, (state, value) -> state.count = value),
            textField(
                    "general.current_damage",
                    state -> state.currentDamage,
                    (state, value) -> state.currentDamage = value),
            textField("general.max_damage", state -> state.maxDamage, (state, value) -> state.maxDamage = value),
            textField("general.repair_cost", state -> state.repairCost, (state, value) -> state.repairCost = value),
            textField("general.rarity", state -> state.rarity, (state, value) -> state.rarity = value),
            textField(
                    "general.glint_override.enable",
                    state -> state.glintOverride,
                    (state, value) -> state.glintOverride = value),
            textField("general.item_model.id", state -> state.itemModelId, (state, value) -> state.itemModelId = value),
            textField(
                    "general.item_model.float",
                    state -> state.customModelFloat,
                    (state, value) -> state.customModelFloat = value),
            textField(
                    "general.item_model.string",
                    state -> state.customModelString,
                    (state, value) -> state.customModelString = value),
            textField(
                    "general.item_model.color",
                    state -> state.customModelColor,
                    (state, value) -> state.customModelColor = value),
            textField(
                    "general.item_model.flag_value",
                    state -> state.customModelFlags,
                    (state, value) -> state.customModelFlags = value),
            textField(
                    "special.advanced.component_tweaks.max_stack_size",
                    state -> state.special.maxStackSize,
                    (state, value) -> state.special.maxStackSize = value),
            textField(
                    "special.advanced.food.can_always_eat",
                    state -> state.special.foodCanAlwaysEat,
                    (state, value) -> state.special.foodCanAlwaysEat = value),
            textField(
                    "special.advanced.consumable.animation",
                    state -> state.special.consumableAnimation,
                    (state, value) -> state.special.consumableAnimation = value),
            textField(
                    "special.advanced.consumable.sound",
                    state -> state.special.consumableSoundId,
                    (state, value) -> state.special.consumableSoundId = value),
            textField(
                    "special.advanced.consumable.has_particles",
                    state -> state.special.consumableHasParticles,
                    (state, value) -> state.special.consumableHasParticles = value),
            textField(
                    "special.advanced.component_tweaks.item_name",
                    state -> state.special.itemName,
                    (state, value) -> state.special.itemName = value),
            textField(
                    "special.advanced.component_tweaks.min_attack_charge",
                    state -> state.special.minimumAttackCharge,
                    (state, value) -> state.special.minimumAttackCharge = value),
            textField(
                    "special.advanced.component_tweaks.enchantable",
                    state -> state.special.enchantableValue,
                    (state, value) -> state.special.enchantableValue = value),
            textField(
                    "special.advanced.component_tweaks.ominous_amplifier",
                    state -> state.special.ominousBottleAmplifier,
                    (state, value) -> state.special.ominousBottleAmplifier = value),
            textField(
                    "special.advanced.component_tweaks.tooltip_style",
                    state -> state.special.tooltipStyleId,
                    (state, value) -> state.special.tooltipStyleId = value),
            textField(
                    "special.advanced.component_tweaks.jukebox_playable",
                    state -> state.special.jukeboxSongId,
                    (state, value) -> state.special.jukeboxSongId = value));

    public static String labelKey(String key) {
        return key.equals("itemeditor.special.potion.custom_name") ? "itemeditor.common.custom_name" : key;
    }

    public static Binding<String> text(String key) {
        return Objects.requireNonNull(TEXT_FIELDS.get(key), key);
    }

    private static final Map<String, Binding<?>> FIELDS = fields();

    private static Map<String, Binding<?>> fields() {
        Map<String, Binding<?>> fields = new HashMap<>(VARIANTS);
        fields.putAll(TEXT_FIELDS);
        fields.putAll(Map.ofEntries(
                field(
                        "book.metadata.generation",
                        state -> state.book.generation,
                        (state, value) -> state.book.generation = value),
                field(
                        "special.advanced.component_tweaks.equippable_slot",
                        state -> state.special.equippableSlot,
                        (state, value) -> state.special.equippableSlot = value),
                field(
                        "special.advanced.combat.swing_animation_type",
                        state -> state.special.swingAnimationType,
                        (state, value) -> state.special.swingAnimationType = value),
                field(
                        "special.dye.color",
                        state -> state.special.dyeColor,
                        (state, value) -> state.special.dyeColor = value),
                field(
                        "special.advanced.component_tweaks.equippable_dispensable",
                        state -> state.special.equippableDispensable,
                        (state, value) -> state.special.equippableDispensable = value),
                field(
                        "special.advanced.component_tweaks.equippable_swappable",
                        state -> state.special.equippableSwappable,
                        (state, value) -> state.special.equippableSwappable = value),
                field(
                        "special.advanced.component_tweaks.equippable_damage_on_hurt",
                        state -> state.special.equippableDamageOnHurt,
                        (state, value) -> state.special.equippableDamageOnHurt = value),
                field("general.unbreakable", state -> state.unbreakable, (state, value) -> state.unbreakable = value),
                field(
                        "special.advanced.use_effects.can_sprint",
                        state -> state.special.useEffectsCanSprint,
                        (state, value) -> state.special.useEffectsCanSprint = value),
                field(
                        "special.advanced.use_effects.interact_vibrations",
                        state -> state.special.useEffectsInteractVibrations,
                        (state, value) -> state.special.useEffectsInteractVibrations = value),
                field(
                        "special.advanced.component_tweaks.glider",
                        state -> state.special.glider,
                        (state, value) -> state.special.glider = value),
                field(
                        "special.advanced.component_tweaks.intangible_projectile",
                        state -> state.special.intangibleProjectile,
                        (state, value) -> state.special.intangibleProjectile = value)));
        return Map.copyOf(fields);
    }

    private ItemEditorFieldReset() {}

    private static <T> Map.Entry<String, Binding<?>> field(
            String key, Function<ItemEditorState, T> read, BiConsumer<ItemEditorState, T> write) {
        return Map.entry("itemeditor." + key, new Binding<>(read, write));
    }

    public static String value(String key, ItemEditorState state) {
        Binding<?> binding = FIELDS.get(key);
        return binding == null ? "" : Objects.toString(binding.read().apply(state), "");
    }

    public static boolean supports(String key) {
        return FIELDS.containsKey(key);
    }

    public static boolean changed(String key, ItemEditorState state, ItemEditorState original) {
        Binding<?> binding = FIELDS.get(key);
        return binding != null && binding.changed(state, original);
    }

    public static boolean restore(String key, ItemEditorState state, ItemEditorState original) {
        Binding<?> binding = FIELDS.get(key);
        if (binding == null || !binding.changed(state, original)) return false;
        binding.restore(state, original);
        return true;
    }
}
