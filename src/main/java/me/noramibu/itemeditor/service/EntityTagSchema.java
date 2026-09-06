package me.noramibu.itemeditor.service;

import static me.noramibu.itemeditor.service.EntityTagFields.Kind.*;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import me.noramibu.itemeditor.service.EntityTagFields.Field;
import me.noramibu.itemeditor.service.EntityTagFields.Group;
import me.noramibu.itemeditor.service.EntityTagFields.Kind;
import net.minecraft.resources.Identifier;

final class EntityTagSchema {
    private record Binding(Set<String> entities, Group group) {
        boolean supports(String name, boolean mob) {
            return entities.contains(name)
                    || entities.contains("@mob") && mob
                    || entities.contains("@minecart") && (name.equals("minecart") || name.endsWith("_minecart"))
                    || entities.contains("@container")
                            && (name.endsWith("_chest_boat")
                                    || name.endsWith("_chest_raft")
                                    || name.equals("chest_minecart")
                                    || name.equals("hopper_minecart"));
        }
    }

    private static final List<Binding> BINDINGS = List.of(
            binding(
                    "S02",
                    "@mob",
                    fields(FLOAT, "AbsorptionAmount"),
                    fields(BOOLEAN, "CanPickUpLoot", "FallFlying", "LeftHanded"),
                    fields(MOTION, "current_explosion_impact_pos"),
                    fields(POSITION, "home_pos", "sleeping_pos"),
                    fields(INT, "home_radius", "last_hurt_by_player_memory_time", "ticks_since_last_hurt_by_mob"),
                    fields(SHORT, "HurtTime"),
                    fields(UUID, "last_hurt_by_mob", "last_hurt_by_player"),
                    fields(LEASH, "leash"),
                    fields(COMPOUND, "locator_bar_icon"),
                    fields(STRING, "Team")),
            binding("S03", "@mob", fields(STRING, "DeathLootTable"), fields(LONG, "DeathLootTableSeed")),
            binding(
                    "S04",
                    "armadillo axolotl bee camel cat chicken cow donkey fox frog goat happy_ghast hoglin horse llama mooshroom mule nautilus ocelot panda parrot pig polar_bear rabbit sheep sniffer strider trader_llama turtle wolf",
                    fields(INT, "Age", "ForcedAge", "InLove"),
                    fields(BOOLEAN, "AgeLocked"),
                    fields(UUID, "LoveCause")),
            binding(
                    "S05",
                    "cat wolf parrot nautilus zombie_nautilus",
                    fields(UUID, "Owner"),
                    fields(BOOLEAN, "Sitting")),
            binding(
                    "S06",
                    "evoker illusioner pillager ravager vindicator witch",
                    fields(BOOLEAN, "CanJoinRaid", "PatrolLeader", "Patrolling"),
                    fields(POSITION, "patrol_target"),
                    fields(INT, "RaidId", "Wave")),
            binding(
                    "S07",
                    "bee enderman iron_golem polar_bear wolf zombified_piglin",
                    fields(LONG, "anger_end_time"),
                    fields(UUID, "angry_at")),
            binding(
                    "S08",
                    "camel camel_husk donkey horse llama mule skeleton_horse trader_llama zombie_horse",
                    fields(BOOLEAN, "Bred", "EatingHaystack", "Tame"),
                    fields(UUID, "Owner"),
                    fields(INT, "Temper")),
            binding(
                    "S09",
                    "@container",
                    fields(COMPOUNDS, "Items"),
                    fields(STRING, "LootTable"),
                    fields(LONG, "LootTableSeed")),
            binding(
                    "S10",
                    "allay armadillo axolotl camel copper_golem frog goat hoglin piglin piglin_brute sniffer villager warden zombie_villager",
                    fields(COMPOUND, "Brain", "Brain.memories")),
            binding("S11", "axolotl cod pufferfish salmon tadpole tropical_fish", fields(BOOLEAN, "FromBucket")),
            binding("S12", "camel camel_husk", fields(LONG, "LastPoseTick")),
            binding(
                    "S13",
                    "donkey llama mule trader_llama",
                    fields(BOOLEAN, "ChestedHorse"),
                    fields(COMPOUNDS, "Items")),
            binding(
                    "S14",
                    "drowned husk zombie zombie_villager zombified_piglin",
                    fields(BOOLEAN, "CanBreakDoors", "IsBaby"),
                    fields(INT, "DrownedConversionTime", "InWaterTime")),
            binding("S15", "evoker illusioner", fields(INT, "SpellTicks")),
            binding(
                    "S16",
                    "hoglin piglin piglin_brute",
                    fields(BOOLEAN, "IsImmuneToZombification"),
                    fields(INT, "TimeInOverworld")),
            binding("S17", "llama trader_llama", fields(INT, "Strength")),
            binding("S18", "magma_cube slime sulfur_cube", fields(INT, "Size"), fields(BOOLEAN, "wasOnGround")),
            binding(
                    "S19",
                    "arrow breeze_wind_charge dragon_fireball egg ender_pearl experience_bottle fireball firework_rocket lingering_potion shulker_bullet small_fireball snowball spectral_arrow splash_potion trident wind_charge wither_skull",
                    fields(BOOLEAN, "HasBeenShot", "LeftOwner"),
                    fields(UUID, "Owner")),
            binding(
                    "S20",
                    "arrow spectral_arrow trident",
                    fields(BOOLEAN, "crit", "inGround"),
                    fields(DOUBLE, "damage"),
                    fields(COMPOUND, "inBlockState", "item", "weapon"),
                    fields(SHORT, "life"),
                    fields(BYTE, "pickup", "PierceLevel", "shake"),
                    fields(STRING, "SoundEvent")),
            binding(
                    "S21",
                    "egg ender_pearl experience_bottle fireball lingering_potion small_fireball snowball splash_potion",
                    fields(COMPOUND, "Item")),
            binding(
                    "S22",
                    "breeze_wind_charge dragon_fireball fireball small_fireball wind_charge",
                    fields(DOUBLE, "acceleration_power")),
            binding("A01", "armadillo", fields(INT, "scute_time"), fields(STRING, "state")),
            binding("A03", "bat", fields(BOOLEAN, "BatFlags")),
            binding(
                    "A04",
                    "bee",
                    fields(INT, "CannotEnterHiveTicks", "CropsGrownSincePollination", "TicksSincePollination"),
                    fields(POSITION, "flower_pos", "hive_pos"),
                    fields(BOOLEAN, "HasNectar", "HasStung")),
            binding("A06", "chicken", fields(INT, "EggLayTime"), fields(BOOLEAN, "IsChickenJockey")),
            binding("A08", "dolphin", fields(BOOLEAN, "GotFish"), fields(INT, "Moistness")),
            binding("A10", "fox", fields(BOOLEAN, "Crouching", "Sitting", "Sleeping"), fields(UUIDS, "Trusted")),
            binding("A12", "glow_squid", fields(INT, "DarkTicksRemaining")),
            binding("A13", "goat", fields(BOOLEAN, "HasLeftHorn", "HasRightHorn", "IsScreamingGoat")),
            binding("A14", "horse", fields(INT, "Variant")),
            binding("A16", "mooshroom", fields(COMPOUNDS, "stew_effects")),
            binding("A18", "ocelot", fields(BOOLEAN, "Trusting")),
            binding("A19", "panda", fields(STRING, "MainGene", "HiddenGene")),
            binding("A20", "pufferfish", fields(INT, "PuffState")),
            binding("A21", "rabbit", fields(INT, "MoreCarrotTicks")),
            binding("A23", "sheep", fields(BOOLEAN, "Sheared")),
            binding("A24", "tadpole", fields(INT, "Age")),
            binding("A27", "turtle", fields(BOOLEAN, "has_egg")),
            binding("M01", "allay", fields(LONG, "DuplicationCooldown"), fields(COMPOUNDS, "Inventory")),
            binding("M03", "copper_golem", fields(LONG, "next_weather_age"), fields(STRING, "weather_state")),
            binding(
                    "M04",
                    "creeper",
                    fields(BYTE, "ExplosionRadius"),
                    fields(SHORT, "Fuse"),
                    fields(BOOLEAN, "ignited", "powered")),
            binding(
                    "M06",
                    "ender_dragon",
                    fields(INT, "DragonDeathTime", "DragonPhase"),
                    fields(FLOAT, "sitting_damage_recieved")),
            binding("M07", "enderman", fields(COMPOUND, "carriedBlockState")),
            binding("M08", "endermite", fields(INT, "Lifetime")),
            binding("M10", "ghast", fields(BYTE, "ExplosionPower")),
            binding("M11", "happy_ghast", fields(INT, "still_timeout")),
            binding("M12", "hoglin", fields(BOOLEAN, "CannotBeHunted")),
            binding("M15", "iron_golem", fields(BOOLEAN, "PlayerCreated")),
            binding("M17", "phantom", fields(INT, "size"), fields(POSITION, "anchor_pos")),
            binding("M18", "piglin", fields(BOOLEAN, "CannotHunt", "IsBaby"), fields(COMPOUNDS, "Inventory")),
            binding("M20", "pillager", fields(COMPOUNDS, "Inventory")),
            binding("M21", "ravager", fields(INT, "AttackTick", "RoarTick", "StunTick")),
            binding("M22", "shulker", fields(BYTE, "AttachFace", "Peek")),
            binding("M23", "skeleton", fields(INT, "StrayConversionTime")),
            binding("M24", "skeleton_horse", fields(BOOLEAN, "SkeletonTrap"), fields(INT, "SkeletonTrapTime")),
            binding("M27", "snow_golem", fields(BOOLEAN, "Pumpkin")),
            binding("M28", "sulfur_cube", fields(INT, "pickup_timer", "fuse"), fields(BOOLEAN, "from_bucket")),
            binding("M29", "vex", fields(POSITION, "bound_pos"), fields(INT, "life_ticks"), fields(UUID, "owner")),
            binding(
                    "M30",
                    "villager",
                    fields(BYTE, "FoodLevel"),
                    fields(COMPOUNDS, "Gossips", "Inventory"),
                    fields(LONG, "LastGossipDecay", "LastRestock"),
                    fields(INT, "RestocksToday", "Xp"),
                    fields(BOOLEAN, "VillagerDataFinalized")),
            binding("M31", "vindicator", fields(BOOLEAN, "Johnny")),
            binding(
                    "M32",
                    "wandering_trader",
                    fields(INT, "DespawnDelay"),
                    fields(COMPOUND, "Offers"),
                    fields(POSITION, "wander_target"),
                    fields(COMPOUNDS, "Inventory")),
            binding("M33", "warden", fields(COMPOUND, "anger", "listener")),
            binding("M34", "wither", fields(INT, "Invul")),
            binding("M35", "zoglin", fields(BOOLEAN, "IsBaby")),
            binding(
                    "M37",
                    "zombie_villager",
                    fields(INT, "ConversionTime", "Xp"),
                    fields(UUID, "ConversionPlayer"),
                    fields(COMPOUNDS, "Gossips"),
                    fields(BOOLEAN, "VillagerDataFinalized")),
            binding(
                    "P08",
                    "firework_rocket",
                    fields(COMPOUND, "FireworksItem"),
                    fields(INT, "Life", "LifeTime"),
                    fields(BOOLEAN, "ShotAtAngle")),
            binding(
                    "P10",
                    "shulker_bullet",
                    fields(INT, "Steps"),
                    fields(UUID, "Target"),
                    fields(DOUBLE, "TXD", "TYD", "TZD")),
            binding("P13", "spectral_arrow", fields(INT, "Duration")),
            binding("P15", "trident", fields(BOOLEAN, "DealtDamage")),
            binding("P17", "wither_skull", fields(BOOLEAN, "dangerous")),
            binding("V01", "@minecart", fields(INT, "DisplayOffset"), fields(COMPOUND, "DisplayState")),
            binding(
                    "V04",
                    "command_block_minecart",
                    fields(STRING, "Command"),
                    fields(COMPONENT, "LastOutput"),
                    fields(INT, "SuccessCount"),
                    fields(BOOLEAN, "TrackOutput")),
            binding("V05", "furnace_minecart", fields(SHORT, "Fuel"), fields(DOUBLE, "PushX", "PushZ")),
            binding("V06", "hopper_minecart", fields(BOOLEAN, "Enabled")),
            binding(
                    "V08",
                    "tnt_minecart",
                    fields(INT, "fuse"),
                    fields(FLOAT, "explosion_power", "explosion_speed_factor")),
            binding(
                    "E01",
                    "area_effect_cloud",
                    fields(INT, "Age", "Color", "Duration", "DurationOnUse", "ReapplicationDelay", "WaitTime"),
                    fields(STRING_OR_COMPOUND, "potion_contents"),
                    fields(FLOAT, "potion_duration_scale", "Radius", "RadiusOnUse", "RadiusPerTick")),
            binding("E03", "end_crystal", fields(POSITION, "beam_target"), fields(BOOLEAN, "ShowBottom")),
            binding("E04", "evoker_fangs", fields(UUID, "Owner"), fields(INT, "Warmup")),
            binding("E05", "experience_orb", fields(SHORT, "Age", "Health", "Value"), fields(INT, "Count")),
            binding("E06", "eye_of_ender", fields(COMPOUND, "Item")),
            binding(
                    "E07",
                    "falling_block",
                    fields(COMPOUND, "BlockState", "TileEntityData"),
                    fields(BOOLEAN, "CancelDrop", "DropItem", "HurtEntities"),
                    fields(FLOAT, "FallHurtAmount"),
                    fields(INT, "FallHurtMax", "Time")),
            binding("E08", "item_frame glow_item_frame", fields(COMPOUND, "Item")),
            binding(
                    "E09",
                    "interaction",
                    fields(FLOAT, "width", "height"),
                    fields(BOOLEAN, "response"),
                    fields(COMPOUND, "attack", "interaction")),
            binding("E10", "item", fields(SHORT, "Health")),
            binding(
                    "E11",
                    "mannequin",
                    fields(STRING_OR_COMPOUND, "profile"),
                    fields(STRINGS, "hidden_layers"),
                    fields(STRING, "main_hand", "pose"),
                    fields(BOOLEAN, "immovable", "hide_description"),
                    fields(COMPONENT, "description")),
            binding("E12", "ominous_item_spawner", fields(COMPOUND, "item"), fields(LONG, "spawn_item_after_ticks")),
            binding("E13", "painting", fields(BYTE, "facing"), fields(STRING, "variant")),
            binding(
                    "E14",
                    "tnt",
                    fields(SHORT, "fuse"),
                    fields(COMPOUND, "block_state"),
                    fields(FLOAT, "explosion_power"),
                    fields(UUID, "owner")));

    private EntityTagSchema() {}

    static List<Group> groups(String rawId) {
        Identifier id = Identifier.tryParse(rawId == null ? "" : rawId);
        if (id == null || !id.getNamespace().equals("minecraft")) return List.of();
        String name = id.getPath();
        boolean mob = EntitySpawnDataUtil.supportsStatusEffects(rawId)
                && !Set.of("armor_stand", "mannequin", "player").contains(name);
        return BINDINGS.stream()
                .filter(binding -> binding.supports(name, mob))
                .map(Binding::group)
                .toList();
    }

    private static Binding binding(String id, String entities, Field[]... fields) {
        return new Binding(
                Set.of(entities.split(" ")),
                new Group(id, Arrays.stream(fields).flatMap(Arrays::stream).toList()));
    }

    private static Field[] fields(Kind kind, String... keys) {
        return Arrays.stream(keys).map(key -> new Field(key, kind)).toArray(Field[]::new);
    }
}
