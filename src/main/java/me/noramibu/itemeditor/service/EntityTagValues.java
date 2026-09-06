package me.noramibu.itemeditor.service;

import com.mojang.serialization.Codec;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.Identifier;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.gossip.GossipContainer;
import net.minecraft.world.entity.animal.armadillo.Armadillo;
import net.minecraft.world.entity.animal.equine.Markings;
import net.minecraft.world.entity.animal.equine.Variant;
import net.minecraft.world.entity.animal.panda.Panda;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.world.entity.monster.warden.AngerManagement;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.item.component.SuspiciousStewEffects;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.vibrations.VibrationSystem;

public final class EntityTagValues {
    private static final Set<String> ITEM_FIELDS = Set.of("Item", "item", "weapon", "FireworksItem");
    private static final Set<String> BLOCK_FIELDS =
            Set.of("inBlockState", "carriedBlockState", "DisplayState", "BlockState", "block_state");

    public record Choice(String value, String label) {}

    private EntityTagValues() {}

    public static int withHorseVariant(int current, int selected, boolean markings) {
        int shift = markings ? 8 : 0;
        return (current & ~(255 << shift)) | ((selected & 255) << shift);
    }

    public static boolean isItem(String key) {
        return ITEM_FIELDS.contains(key);
    }

    public static boolean isBlock(String key) {
        return BLOCK_FIELDS.contains(key);
    }

    public static boolean isPackInventory(String entityId) {
        return Set.of("minecraft:donkey", "minecraft:mule", "minecraft:llama", "minecraft:trader_llama")
                .contains(entityId);
    }

    public static int inventorySize(String entityId, CompoundTag entity) {
        return switch (entityId) {
            case "minecraft:allay" -> 1;
            case "minecraft:pillager", "minecraft:hopper_minecart" -> 5;
            case "minecraft:piglin", "minecraft:villager", "minecraft:wandering_trader" -> 8;
            case "minecraft:donkey", "minecraft:mule" -> 15;
            case "minecraft:llama", "minecraft:trader_llama" -> 3 * Math.clamp(entity.getIntOr("Strength", 1), 1, 5);
            default -> 27;
        };
    }

    static boolean validInventory(String entityId, String key, CompoundTag entity) {
        ListTag items = entity.getListOrEmpty(key);
        int size = inventorySize(entityId, entity);
        if (items.size() > size) return false;
        if (!key.equals("Items")) return true;
        if (!items.isEmpty() && !entity.getStringOr("LootTable", "").isEmpty()) return false;
        if (!items.isEmpty() && isPackInventory(entityId) && !entity.getBooleanOr("ChestedHorse", false)) return false;
        Set<Integer> slots = new HashSet<>();
        for (Tag item : items) {
            if (!(item instanceof CompoundTag stack) || !(stack.get("Slot") instanceof ByteTag slot)) return false;
            int index = slot.byteValue() & 255;
            if (index >= size || !slots.add(index)) return false;
        }
        return true;
    }

    static void validate(String entityId, String key, Tag value, CompoundTag original, RegistryAccess registries) {
        Codec<?> codec =
                switch (key) {
                    case "Item", "item", "weapon", "FireworksItem" -> ItemStack.CODEC;
                    case "Items", "Inventory" -> ItemStack.CODEC.listOf();
                    case "inBlockState", "carriedBlockState", "DisplayState", "BlockState", "block_state" ->
                        BlockState.CODEC;
                    case "profile" -> ResolvableProfile.CODEC;
                    case "potion_contents" -> PotionContents.CODEC;
                    case "stew_effects" -> SuspiciousStewEffects.CODEC;
                    case "Gossips" -> GossipContainer.CODEC;
                    case "Offers" -> MerchantOffers.CODEC;
                    case "listener" -> VibrationSystem.Data.CODEC;
                    case "anger" -> AngerManagement.codec(entity -> true);
                    case "description", "LastOutput" -> ComponentSerialization.CODEC;
                    default -> null;
                };
        var ops = registries.createSerializationContext(NbtOps.INSTANCE);
        if (codec != null) codec.parse(ops, value).getOrThrow();
        if (key.equals("Brain")) {
            CompoundTag brain = (CompoundTag) value;
            if (brain.contains("memories") && !(brain.get("memories") instanceof CompoundTag))
                throw new IllegalArgumentException();
            validate(entityId, "Brain.memories", brain.getCompoundOrEmpty("memories"), original, registries);
        }
        if (isBlock(key)) {
            CompoundTag tag = (CompoundTag) value;
            var block = BuiltInRegistries.BLOCK
                    .getOptional(Identifier.parse(tag.getStringOr("Name", "")))
                    .orElseThrow();
            CompoundTag properties = tag.getCompoundOrEmpty("Properties");
            for (String name : properties.keySet()) {
                var property = block.getStateDefinition().getProperty(name);
                if (property == null
                        || property.getValue(properties.getStringOr(name, "")).isEmpty())
                    throw new IllegalArgumentException();
            }
        }
        if (key.equals("attack") || key.equals("interaction")) {
            CompoundTag event = (CompoundTag) value;
            event.read("player", UUIDUtil.CODEC).orElseThrow();
            if (!(event.get("timestamp") instanceof LongTag)) throw new IllegalArgumentException();
        }
        if (key.equals("locator_bar_icon")) {
            CompoundTag icon = (CompoundTag) value;
            if (icon.contains("color") && !(icon.get("color") instanceof IntTag)) throw new IllegalArgumentException();
            if (icon.contains("style")) Identifier.parse(icon.getStringOr("style", ""));
        }
        if (key.equals("hidden_layers")) {
            if (!(value instanceof ListTag layers)) throw new IllegalArgumentException();
            Set<String> parts = Arrays.stream(PlayerModelPart.values())
                    .map(PlayerModelPart::getSerializedName)
                    .collect(Collectors.toSet());
            for (Tag part : layers)
                if (!(part instanceof StringTag(String name)) || !parts.contains(name))
                    throw new IllegalArgumentException();
        }
        if (key.equals("Brain.memories")) {
            CompoundTag memories = (CompoundTag) value;
            for (String name : memories.keySet()) {
                if (Objects.equals(
                        memories.get(name),
                        original.getCompoundOrEmpty("Brain")
                                .getCompoundOrEmpty("memories")
                                .get(name))) continue;
                var module = BuiltInRegistries.MEMORY_MODULE_TYPE
                        .getOptional(Identifier.parse(name))
                        .orElseThrow();
                module.getCodec().orElseThrow().parse(ops, memories.get(name)).getOrThrow();
            }
        }
        if (key.equals("SoundEvent") || key.equals("variant")) {
            Identifier id = Identifier.parse(((StringTag) value).value());
            boolean found = key.equals("SoundEvent")
                    ? registries.lookupOrThrow(Registries.SOUND_EVENT).containsKey(id)
                    : registries.lookupOrThrow(Registries.PAINTING_VARIANT).containsKey(id);
            if (!found) throw new IllegalArgumentException();
        }
        if (key.equals("LootTable") || key.equals("DeathLootTable")) Identifier.parse(((StringTag) value).value());
        if (value instanceof NumericTag number) {
            double min =
                    switch (key) {
                        case "Strength", "Count" -> 1;
                        case "Temper",
                                "PuffState",
                                "Peek",
                                "AttachFace",
                                "facing",
                                "size",
                                "Size",
                                "explosion_power",
                                "Radius",
                                "potion_duration_scale" -> 0;
                        case "DrownedConversionTime", "InWaterTime", "StrayConversionTime", "ConversionTime" -> -1;
                        default -> -Double.MAX_VALUE;
                    };
            double max =
                    switch (key) {
                        case "Strength", "AttachFace" -> 5;
                        case "PuffState" -> 2;
                        case "facing" -> 3;
                        case "Temper" ->
                            Set.of("minecraft:llama", "minecraft:trader_llama").contains(entityId) ? 30 : 100;
                        case "Peek" -> 100;
                        case "size" -> 64;
                        case "explosion_power" -> 128;
                        default -> Double.MAX_VALUE;
                    };
            if (number.doubleValue() < min || number.doubleValue() > max) throw new IllegalArgumentException();
        }
        if (key.equals("Motion")) {
            if (!(value instanceof ListTag motion)) throw new IllegalArgumentException();
            for (Tag entry : motion)
                if (!(entry instanceof NumericTag number) || Math.abs(number.doubleValue()) > 10)
                    throw new IllegalArgumentException();
        }
    }

    public static List<Choice> enumChoices(String key) {
        return switch (key) {
            case "state" -> named(Armadillo.ArmadilloState.values());
            case "MainGene", "HiddenGene" -> named(Panda.Gene.values());
            case "weather_state" -> named(WeatheringCopper.WeatherState.values());
            case "main_hand" -> named(HumanoidArm.values());
            case "pose" ->
                named(Arrays.stream(Pose.values())
                        .filter(pose -> Mannequin.POSE_CODEC
                                .parse(NbtOps.INSTANCE, StringTag.valueOf(pose.getSerializedName()))
                                .result()
                                .isPresent())
                        .toArray(Pose[]::new));
            case "pickup" ->
                Arrays.stream(AbstractArrow.Pickup.values())
                        .map(value -> new Choice(Integer.toString(value.ordinal()), value.name()))
                        .toList();
            case "DragonPhase" ->
                IntStream.range(0, EnderDragonPhase.getCount())
                        .mapToObj(EnderDragonPhase::getById)
                        .map(value -> new Choice(Integer.toString(value.getId()), value.toString()))
                        .toList();
            case "facing" ->
                Arrays.stream(Direction.values())
                        .filter(direction -> direction.getAxis().isHorizontal())
                        .map(direction ->
                                new Choice(Integer.toString(direction.get2DDataValue()), direction.getSerializedName()))
                        .toList();
            case "AttachFace" ->
                Arrays.stream(Direction.values())
                        .map(direction ->
                                new Choice(Integer.toString(direction.get3DDataValue()), direction.getSerializedName()))
                        .toList();
            case "Variant" ->
                Arrays.stream(Variant.values())
                        .flatMap(color -> Arrays.stream(Markings.values())
                                .map(marking -> new Choice(
                                        Integer.toString(color.getId() | marking.getId() << 8),
                                        color.getSerializedName() + " / " + marking.name())))
                        .toList();
            default -> List.of();
        };
    }

    private static List<Choice> named(StringRepresentable[] values) {
        return Arrays.stream(values)
                .map(value -> new Choice(value.getSerializedName(), value.getSerializedName()))
                .toList();
    }
}
