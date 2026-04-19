package me.noramibu.itemeditor.util;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.entity.EntityType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SignItem;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.List;
import java.util.Locale;

public final class ItemEditorCapabilities {

    private static final RawRuntimeSuggestionProvider RUNTIME_SUGGESTIONS = new RawRuntimeSuggestionProvider();
    private static final RawAutocompleteSchema AUTOCOMPLETE_SCHEMA = RawAutocompleteSchema.load();

    private ItemEditorCapabilities() {
    }

    public static boolean supportsBook(ItemStack stack) {
        return stack.is(Items.WRITABLE_BOOK) || stack.is(Items.WRITTEN_BOOK);
    }

    public static boolean supportsDurability(ItemStack stack) {
        return stack.isDamageableItem() || stack.has(DataComponents.MAX_DAMAGE);
    }

    public static boolean supportsRepairCost(ItemStack stack) {
        return stack.has(DataComponents.REPAIR_COST) || supportsDurability(stack);
    }

    public static boolean supportsContainerData(ItemStack stack) {
        return stack.has(DataComponents.CONTAINER)
                || stack.has(DataComponents.CONTAINER_LOOT)
                || isContainerBlockItem(stack);
    }

    public static boolean supportsBundleData(ItemStack stack) {
        return stack.has(DataComponents.BUNDLE_CONTENTS) || stack.is(Items.BUNDLE);
    }

    public static boolean supportsSignData(ItemStack stack) {
        return stack.getItem() instanceof SignItem || hasSignBlockEntityData(stack);
    }

    public static boolean supportsSpawnerData(ItemStack stack) {
        TypedEntityData<BlockEntityType<?>> blockEntityData = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        return stack.is(Items.SPAWNER)
                || (blockEntityData != null && blockEntityData.type() == BlockEntityType.MOB_SPAWNER);
    }

    public static boolean supportsArmorStandData(ItemStack stack) {
        TypedEntityData<EntityType<?>> entityData = stack.get(DataComponents.ENTITY_DATA);
        return stack.is(Items.ARMOR_STAND)
                || (entityData != null && entityData.type() == EntityType.ARMOR_STAND);
    }

    public static boolean supportsItemFrameData(ItemStack stack) {
        TypedEntityData<EntityType<?>> entityData = stack.get(DataComponents.ENTITY_DATA);
        return stack.is(Items.ITEM_FRAME)
                || stack.is(Items.GLOW_ITEM_FRAME)
                || (entityData != null
                && (entityData.type() == EntityType.ITEM_FRAME || entityData.type() == EntityType.GLOW_ITEM_FRAME));
    }

    public static boolean supportsSpawnEggData(ItemStack stack) {
        return stack.getItem() instanceof SpawnEggItem;
    }

    public static boolean supportsBucketCreature(ItemStack stack) {
        return stack.has(DataComponents.BUCKET_ENTITY_DATA)
                || stack.has(DataComponents.AXOLOTL_VARIANT)
                || stack.has(DataComponents.SALMON_SIZE)
                || stack.has(DataComponents.TROPICAL_FISH_PATTERN)
                || stack.has(DataComponents.TROPICAL_FISH_BASE_COLOR)
                || stack.has(DataComponents.TROPICAL_FISH_PATTERN_COLOR)
                || isBucketCreatureBucketItem(stack);
    }

    public static boolean isBucketCreatureBucketItem(ItemStack stack) {
        return stack.is(Items.COD_BUCKET)
                || stack.is(Items.SALMON_BUCKET)
                || stack.is(Items.PUFFERFISH_BUCKET)
                || stack.is(Items.TROPICAL_FISH_BUCKET)
                || stack.is(Items.AXOLOTL_BUCKET)
                || stack.is(Items.TADPOLE_BUCKET);
    }

    public static Component specialDataTitle(ItemStack stack) {
        return switch (detectSpecialDataFocus(stack)) {
            case CONTAINER -> ItemEditorText.tr("category.special_data.container.title");
            case BUNDLE -> ItemEditorText.tr("category.special_data.bundle.title");
            case SIGN -> ItemEditorText.tr("category.special_data.sign.title");
            case SPAWNER -> ItemEditorText.tr("category.special_data.spawner.title");
            case ARMOR_STAND -> ItemEditorText.tr("category.special_data.armor_stand.title");
            case ITEM_FRAME -> ItemEditorText.tr("category.special_data.item_frame.title");
            case SPAWN_EGG -> ItemEditorText.tr("category.special_data.spawn_egg.title");
            case BUCKET_CREATURE -> ItemEditorText.tr("category.special_data.bucket.title");
            case POTION -> ItemEditorText.tr("category.special_data.potion.title");
            case FIREWORK -> ItemEditorText.tr("category.special_data.firework.title");
            case BANNER -> ItemEditorText.tr("category.special_data.banner.title");
            case INSTRUMENT -> ItemEditorText.tr("category.special_data.instrument.title");
            default -> ItemEditorText.tr("category.special_data.title");
        };
    }

    public static Component specialDataDescription(ItemStack stack) {
        return Component.empty();
    }

    public static boolean supportsSpecialData(ItemStack stack) {
        return detectSpecialDataFocus(stack) != SpecialDataFocus.GENERAL;
    }

    public static boolean supportsComponents(ItemStack stack, RegistryAccess registryAccess) {
        return supportsAnyComponent(
                stack,
                registryAccess,
                "minecraft:food",
                "minecraft:consumable",
                "minecraft:use_effects",
                "minecraft:use_remainder",
                "minecraft:use_cooldown",
                "minecraft:lock",
                "minecraft:container_loot",
                "minecraft:bees",
                "minecraft:pot_decorations",
                "minecraft:charged_projectiles",
                "minecraft:map_color",
                "minecraft:map_post_processing",
                "minecraft:map_id",
                "minecraft:map_decorations",
                "minecraft:lodestone_tracker",
                "minecraft:equippable",
                "minecraft:weapon",
                "minecraft:tool",
                "minecraft:repairable",
                "minecraft:attack_range",
                "minecraft:item_name",
                "minecraft:minimum_attack_charge",
                "minecraft:enchantable",
                "minecraft:ominous_bottle_amplifier",
                "minecraft:tooltip_style",
                "minecraft:glider",
                "minecraft:intangible_projectile",
                "minecraft:death_protection",
                "minecraft:damage_type",
                "minecraft:damage_resistant",
                "minecraft:note_block_sound",
                "minecraft:break_sound",
                "minecraft:painting_variant",
                "minecraft:block_state",
                "minecraft:blocks_attacks",
                "minecraft:swing_animation",
                "minecraft:piercing_weapon",
                "minecraft:kinetic_weapon"
        );
    }

    public static boolean supportsAnyComponent(ItemStack stack, RegistryAccess registryAccess, String... componentIds) {
        if (componentIds == null) {
            return false;
        }

        for (String componentId : componentIds) {
            if (supportsComponent(stack, registryAccess, componentId)) {
                return true;
            }
        }
        return false;
    }

    public static boolean supportsComponent(ItemStack stack, RegistryAccess registryAccess, String componentId) {
        if (componentId == null || componentId.isBlank()) {
            return false;
        }

        String normalizedComponentId = normalizeComponentId(componentId);
        Identifier componentIdentifier = Identifier.tryParse(normalizedComponentId);
        if (componentIdentifier == null) {
            return false;
        }

        var builtInType = BuiltInRegistries.DATA_COMPONENT_TYPE.getOptional(componentIdentifier).orElse(null);
        if (builtInType != null && stack.has(builtInType)) {
            return true;
        }

        List<String> allowed = allowedComponents(stack, registryAccess);
        if (allowed.isEmpty()) {
            return true;
        }
        for (String allowedComponentId : allowed) {
            if (normalizeComponentId(allowedComponentId).equals(normalizedComponentId)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isContainerBlockItem(ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem blockItem)) {
            return false;
        }

        var block = blockItem.getBlock();
        return block instanceof ChestBlock
                || block instanceof BarrelBlock
                || block instanceof ShulkerBoxBlock
                || block instanceof HopperBlock
                || block instanceof DispenserBlock;
    }

    private static boolean hasSignBlockEntityData(ItemStack stack) {
        TypedEntityData<BlockEntityType<?>> blockEntityData = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        return blockEntityData != null
                && (blockEntityData.type() == BlockEntityType.SIGN || blockEntityData.type() == BlockEntityType.HANGING_SIGN);
    }

    private static SpecialDataFocus detectSpecialDataFocus(ItemStack stack) {
        if (supportsContainerData(stack)) {
            return SpecialDataFocus.CONTAINER;
        }
        if (supportsBundleData(stack)) {
            return SpecialDataFocus.BUNDLE;
        }
        if (supportsSignData(stack)) {
            return SpecialDataFocus.SIGN;
        }
        if (supportsSpawnerData(stack)) {
            return SpecialDataFocus.SPAWNER;
        }
        if (supportsArmorStandData(stack)) {
            return SpecialDataFocus.ARMOR_STAND;
        }
        if (supportsItemFrameData(stack)) {
            return SpecialDataFocus.ITEM_FRAME;
        }
        if (supportsSpawnEggData(stack)) {
            return SpecialDataFocus.SPAWN_EGG;
        }
        if (supportsBucketCreature(stack)) {
            return SpecialDataFocus.BUCKET_CREATURE;
        }
        if (isPotionRelated(stack)) {
            return SpecialDataFocus.POTION;
        }
        if (isFireworkRelated(stack)) {
            return SpecialDataFocus.FIREWORK;
        }
        if (isBannerRelated(stack)) {
            return SpecialDataFocus.BANNER;
        }
        if (isInstrumentRelated(stack)) {
            return SpecialDataFocus.INSTRUMENT;
        }
        return SpecialDataFocus.GENERAL;
    }

    private static boolean isPotionRelated(ItemStack stack) {
        return stack.has(DataComponents.POTION_CONTENTS)
                || stack.has(DataComponents.SUSPICIOUS_STEW_EFFECTS)
                || stack.is(Items.POTION)
                || stack.is(Items.SPLASH_POTION)
                || stack.is(Items.LINGERING_POTION)
                || stack.is(Items.TIPPED_ARROW)
                || stack.is(Items.SUSPICIOUS_STEW);
    }

    private static boolean isFireworkRelated(ItemStack stack) {
        return stack.has(DataComponents.FIREWORKS)
                || stack.has(DataComponents.FIREWORK_EXPLOSION)
                || stack.is(Items.FIREWORK_ROCKET)
                || stack.is(Items.FIREWORK_STAR);
    }

    private static boolean isBannerRelated(ItemStack stack) {
        return stack.has(DataComponents.BANNER_PATTERNS)
                || stack.getItem() instanceof BannerItem
                || stack.is(Items.SHIELD)
                || stack.is(Items.WHITE_BANNER);
    }

    private static boolean isInstrumentRelated(ItemStack stack) {
        return stack.has(DataComponents.INSTRUMENT)
                || stack.has(DataComponents.JUKEBOX_PLAYABLE)
                || stack.is(Items.GOAT_HORN)
                || stack.is(Items.JUKEBOX)
                || isMusicDisc(stack);
    }

    private static boolean isMusicDisc(ItemStack stack) {
        var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id.getPath().startsWith("music_disc_");
    }

    private static List<String> allowedComponents(ItemStack stack, RegistryAccess registryAccess) {
        RegistryAccess effectiveRegistryAccess = registryAccess == null ? RegistryAccess.EMPTY : registryAccess;
        String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        List<String> profiles = RUNTIME_SUGGESTIONS.itemProfiles(itemId, effectiveRegistryAccess);
        return AUTOCOMPLETE_SCHEMA.componentsForProfiles(profiles);
    }

    private static String normalizeComponentId(String componentId) {
        String normalized = componentId.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return normalized;
        }
        if (normalized.contains(":")) {
            return normalized;
        }
        return "minecraft:" + normalized;
    }

    private enum SpecialDataFocus {
        GENERAL,
        CONTAINER,
        BUNDLE,
        SIGN,
        SPAWNER,
        ARMOR_STAND,
        ITEM_FRAME,
        SPAWN_EGG,
        BUCKET_CREATURE,
        POTION,
        FIREWORK,
        BANNER,
        INSTRUMENT
    }
}
