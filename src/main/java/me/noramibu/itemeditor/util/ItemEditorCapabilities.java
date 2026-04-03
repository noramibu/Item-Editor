package me.noramibu.itemeditor.util;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PlayerHeadItem;
import net.minecraft.world.item.SignItem;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.DropperBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.TrappedChestBlock;

public final class ItemEditorCapabilities {

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
        CustomData blockEntityData = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        return stack.is(Items.SPAWNER)
                || (blockEntityData != null && looksLikeSpawnerTag(blockEntityData.copyTag()));
    }

    public static boolean supportsBucketCreature(ItemStack stack) {
        return stack.has(DataComponents.BUCKET_ENTITY_DATA)
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
            case BUCKET_CREATURE -> ItemEditorText.tr("category.special_data.bucket.title");
            case POTION -> ItemEditorText.tr("category.special_data.potion.title");
            case FIREWORK -> ItemEditorText.tr("category.special_data.firework.title");
            case BANNER -> ItemEditorText.tr("category.special_data.banner.title");
            case MAP -> ItemEditorText.tr("category.special_data.map.title");
            case DYED -> ItemEditorText.tr("category.special_data.dyed.title");
            case TRIM -> ItemEditorText.tr("category.special_data.trim.title");
            case PROFILE -> ItemEditorText.tr("category.special_data.profile.title");
            case INSTRUMENT -> ItemEditorText.tr("category.special_data.instrument.title");
            default -> ItemEditorText.tr("category.special_data.title");
        };
    }

    public static Component specialDataDescription(ItemStack stack) {
        return Component.empty();
    }

    public static boolean supportsSpecialData(ItemStack stack) {
        return stack.has(DataComponents.POTION_CONTENTS)
                || stack.has(DataComponents.SUSPICIOUS_STEW_EFFECTS)
                || stack.has(DataComponents.FIREWORKS)
                || stack.has(DataComponents.FIREWORK_EXPLOSION)
                || supportsBucketCreature(stack)
                || stack.has(DataComponents.DYED_COLOR)
                || stack.has(DataComponents.TRIM)
                || stack.has(DataComponents.PROFILE)
                || stack.has(DataComponents.INSTRUMENT)
                || stack.has(DataComponents.JUKEBOX_PLAYABLE)
                || stack.has(DataComponents.MAP_COLOR)
                || stack.has(DataComponents.MAP_POST_PROCESSING)
                || stack.has(DataComponents.BANNER_PATTERNS)
                || stack.getItem() instanceof BannerItem
                || stack.is(Items.SHIELD)
                || stack.getItem() instanceof PlayerHeadItem
                || stack.is(Items.POTION)
                || stack.is(Items.SPLASH_POTION)
                || stack.is(Items.LINGERING_POTION)
                || stack.is(Items.TIPPED_ARROW)
                || stack.is(Items.SUSPICIOUS_STEW)
                || stack.is(Items.FIREWORK_ROCKET)
                || stack.is(Items.FIREWORK_STAR)
                || stack.is(Items.FILLED_MAP)
                || stack.is(Items.GOAT_HORN)
                || stack.is(Items.JUKEBOX)
                || isMusicDisc(stack)
                || stack.is(ItemTags.TRIMMABLE_ARMOR)
                || stack.is(Items.LEATHER_HELMET)
                || stack.is(Items.LEATHER_CHESTPLATE)
                || stack.is(Items.LEATHER_LEGGINGS)
                || stack.is(Items.LEATHER_BOOTS)
                || stack.is(Items.LEATHER_HORSE_ARMOR)
                || stack.is(Items.WOLF_ARMOR)
                || supportsContainerData(stack)
                || supportsBundleData(stack)
                || supportsSignData(stack)
                || supportsSpawnerData(stack);
    }

    private static boolean isContainerBlockItem(ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem blockItem)) {
            return false;
        }

        var block = blockItem.getBlock();
        return block instanceof ChestBlock
                || block instanceof TrappedChestBlock
                || block instanceof BarrelBlock
                || block instanceof ShulkerBoxBlock
                || block instanceof HopperBlock
                || block instanceof DispenserBlock
                || block instanceof DropperBlock;
    }

    private static boolean hasSignBlockEntityData(ItemStack stack) {
        CustomData blockEntityData = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (blockEntityData == null) {
            return false;
        }
        var tag = blockEntityData.copyTag();
        return tag.contains("front_text") || tag.contains("back_text");
    }

    private static boolean looksLikeSpawnerTag(net.minecraft.nbt.CompoundTag tag) {
        return tag.contains("SpawnData")
                || tag.contains("SpawnPotentials")
                || tag.contains("Delay")
                || tag.contains("MinSpawnDelay")
                || tag.contains("MaxSpawnDelay")
                || tag.contains("SpawnCount")
                || tag.contains("MaxNearbyEntities")
                || tag.contains("RequiredPlayerRange")
                || tag.contains("SpawnRange");
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
        if (isBucketCreatureRelated(stack)) {
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
        if (isMapRelated(stack)) {
            return SpecialDataFocus.MAP;
        }
        if (isDyedRelated(stack)) {
            return SpecialDataFocus.DYED;
        }
        if (isTrimRelated(stack)) {
            return SpecialDataFocus.TRIM;
        }
        if (isProfileRelated(stack)) {
            return SpecialDataFocus.PROFILE;
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

    private static boolean isBucketCreatureRelated(ItemStack stack) {
        return supportsBucketCreature(stack);
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

    private static boolean isMapRelated(ItemStack stack) {
        return stack.has(DataComponents.MAP_COLOR)
                || stack.has(DataComponents.MAP_POST_PROCESSING)
                || stack.is(Items.FILLED_MAP);
    }

    private static boolean isDyedRelated(ItemStack stack) {
        return stack.has(DataComponents.DYED_COLOR)
                || stack.is(Items.LEATHER_HELMET)
                || stack.is(Items.LEATHER_CHESTPLATE)
                || stack.is(Items.LEATHER_LEGGINGS)
                || stack.is(Items.LEATHER_BOOTS)
                || stack.is(Items.LEATHER_HORSE_ARMOR)
                || stack.is(Items.WOLF_ARMOR);
    }

    private static boolean isTrimRelated(ItemStack stack) {
        return stack.has(DataComponents.TRIM) || stack.is(ItemTags.TRIMMABLE_ARMOR);
    }

    private static boolean isProfileRelated(ItemStack stack) {
        return stack.has(DataComponents.PROFILE) || stack.getItem() instanceof PlayerHeadItem;
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
        return id != null && id.getPath().startsWith("music_disc_");
    }

    private enum SpecialDataFocus {
        GENERAL,
        CONTAINER,
        BUNDLE,
        SIGN,
        SPAWNER,
        BUCKET_CREATURE,
        POTION,
        FIREWORK,
        BANNER,
        MAP,
        DYED,
        TRIM,
        PROFILE,
        INSTRUMENT
    }
}
