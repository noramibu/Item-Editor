package me.noramibu.itemeditor.editor;

import me.noramibu.itemeditor.util.TextComponentUtil;
import me.noramibu.itemeditor.util.ValidationUtil;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.block.entity.BannerPatternLayers;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class ItemEditorState {

    public String customName = "";
    public String count = "1";
    public String currentDamage = "";
    public String maxDamage = "";
    public String repairCost = "";
    public boolean unbreakable;
    public boolean glintOverrideEnabled;
    public boolean glintOverride;
    public String rarity = "";
    public String itemModelId = "";
    public String customModelFloat = "";
    public boolean customModelFlagEnabled;
    public boolean customModelFlag;
    public String customModelString = "";
    public String customModelColor = "";
    public final List<String> canBreakBlockIds = new ArrayList<>();
    public boolean canBreakShowInTooltip = true;
    public boolean canBreakCollapsed = true;
    public final List<String> canPlaceOnBlockIds = new ArrayList<>();
    public boolean canPlaceOnShowInTooltip = true;
    public boolean canPlaceOnCollapsed = true;
    public boolean hideTooltip;
    public final Set<String> hiddenTooltipComponents = new LinkedHashSet<>();
    public final List<LoreLineDraft> loreLines = new ArrayList<>();
    public final List<AttributeModifierDraft> attributeModifiers = new ArrayList<>();
    public final List<EnchantmentDraft> enchantments = new ArrayList<>();
    public final List<EnchantmentDraft> storedEnchantments = new ArrayList<>();
    public boolean unsafeEnchantments;
    public boolean rawEditorEdited;
    public String rawEditorText = "";
    public boolean rawEditorShowDefaults;
    public boolean rawEditorWordWrap = true;
    public boolean rawEditorHorizontalScroll;
    public int rawEditorFontSizePercent = 100;
    public boolean rawEditorOptionsLoaded;
    public int uiRawEditorCursor;
    public int uiRawEditorSelectionCursor;
    public double uiRawEditorScrollAmount;
    public boolean uiRawEditorOptionsExpanded;
    public boolean uiCategoriesRailCollapsed;
    public boolean uiPreviewRailCollapsed;
    public boolean uiPreviewTooltipHiddenBySide;
    public boolean uiPreviewTooltipCollapsed;
    public boolean uiPreviewValidationCollapsed;
    public final List<RawEditorHistoryEntry> uiRawEditorUndoHistory = new ArrayList<>();
    public final List<RawEditorHistoryEntry> uiRawEditorRedoHistory = new ArrayList<>();
    public final BookData book = new BookData();
    public final SpecialData special = new SpecialData();

    public static final class LoreLineDraft {
        public String rawText = "";
        public final TextStyleDraft style = new TextStyleDraft();

        public static LoreLineDraft fromComponent(Component component) {
            LoreLineDraft draft = new LoreLineDraft();
            draft.rawText = TextComponentUtil.toMarkup(component);
            draft.style.readFrom(component.getStyle());
            return draft;
        }
    }

    public static final class TextStyleDraft {
        public String colorHex = "";
        public boolean bold;
        public boolean italic;
        public boolean underlined;
        public boolean strikethrough;
        public boolean obfuscated;

        public void readFrom(Style style) {
            if (style.getColor() != null) {
                this.colorHex = ValidationUtil.toHex(style.getColor().getValue());
            }
            this.bold = style.isBold();
            this.italic = style.isItalic();
            this.underlined = style.isUnderlined();
            this.strikethrough = style.isStrikethrough();
            this.obfuscated = style.isObfuscated();
        }
    }

    public static final class AttributeModifierDraft {
        public String attributeId = "";
        public String modifierId = "";
        public String amount = "0";
        public String operation = AttributeModifier.Operation.ADD_VALUE.name();
        public String slotGroup = EquipmentSlotGroup.ANY.name();
        public boolean uiCollapsed = true;

        public static AttributeModifierDraft fromEntry(ItemAttributeModifiers.Entry entry) {
            AttributeModifierDraft draft = new AttributeModifierDraft();
            draft.attributeId = entry.attribute().unwrapKey().map(key -> key.identifier().toString()).orElse("");
            draft.modifierId = entry.modifier().id().toString();
            draft.amount = String.valueOf(entry.modifier().amount());
            draft.operation = entry.modifier().operation().name();
            draft.slotGroup = entry.slot().name();
            return draft;
        }
    }

    public static final class EnchantmentDraft {
        public String enchantmentId = "";
        public String level = "1";
        public boolean uiCollapsed = true;

        public static EnchantmentDraft fromEntry(Holder<net.minecraft.world.item.enchantment.Enchantment> enchantment, int level) {
            EnchantmentDraft draft = new EnchantmentDraft();
            draft.enchantmentId = enchantment.unwrapKey().map(key -> key.identifier().toString()).orElse("");
            draft.level = Integer.toString(level);
            return draft;
        }
    }

    public static final class BookData {
        public boolean writtenBook;
        public String title = "";
        public String author = "";
        public String generation = "0";
        public final List<String> pages = new ArrayList<>();
        public int selectedPage;
        public double miniMapScrollOffset;
        public boolean uiPageControlsCollapsed = true;
        public boolean uiPageMiniMapCollapsed = true;
    }

    public static final class RawEditorHistoryEntry {
        public String text = "";
        public int cursor;
        public int selection;
        public double scroll;

        public static RawEditorHistoryEntry of(String text, int cursor, int selection, double scroll) {
            RawEditorHistoryEntry entry = new RawEditorHistoryEntry();
            entry.text = text == null ? "" : text;
            entry.cursor = cursor;
            entry.selection = selection;
            entry.scroll = scroll;
            return entry;
        }
    }

    public static final class SpecialData {
        public final List<ContainerEntryDraft> containerEntries = new ArrayList<>();
        public int selectedContainerSlot = -1;
        public int draggingContainerSlot = -1;
        public final List<ContainerEntryDraft> bundleEntries = new ArrayList<>();
        public int selectedBundleIndex = -1;
        public String lockItemId = "";
        public String lockPredicateSnbt = "";
        public String containerLootTableId = "";
        public String containerLootSeed = "";
        public final List<BeeOccupantDraft> beesOccupants = new ArrayList<>();
        public String potBackItemId = "";
        public String potLeftItemId = "";
        public String potRightItemId = "";
        public String potFrontItemId = "";

        public String foodNutrition = "";
        public String foodSaturation = "";
        public boolean foodCanAlwaysEat;
        public boolean uiFoodConsumableCollapsed = true;
        public String consumableConsumeSeconds = "";
        public String consumableAnimation = "";
        public String consumableSoundId = "";
        public boolean consumableHasParticles;
        public final List<ConsumableEffectDraft> consumableOnConsumeEffects = new ArrayList<>();
        public boolean useEffectsCanSprint;
        public boolean useEffectsInteractVibrations;
        public String useEffectsSpeedMultiplier = "";
        public String useRemainderItemId = "";
        public String useRemainderCount = "";
        public String useCooldownSeconds = "";
        public String useCooldownGroup = "";
        public boolean uiContainerMetadataCollapsed = true;
        public boolean uiCrossbowCollapsed = true;
        public boolean uiMapBasicCollapsed = true;
        public boolean uiMapAdvancedCollapsed = true;
        public boolean uiEquipmentCombatCollapsed = true;
        public boolean uiCombatEquippableCollapsed = true;
        public boolean uiCombatWeaponCollapsed = true;
        public boolean uiCombatToolCollapsed = true;
        public boolean uiCombatRepairableCollapsed = true;
        public boolean uiCombatAttackRangeCollapsed = true;
        public boolean uiComponentTweaksCollapsed = true;
        public boolean uiComponentTweaksNamingCollapsed = true;
        public boolean uiComponentTweaksRegistryCollapsed = true;
        public boolean uiComponentTweaksBlockCollapsed = true;
        public boolean uiComponentTweaksBehaviorCollapsed = true;
        public final List<ChargedProjectileDraft> chargedProjectiles = new ArrayList<>();
        public String itemName = "";
        public String maxStackSize = "";
        public String minimumAttackCharge = "";
        public String enchantableValue = "";
        public String ominousBottleAmplifier = "";
        public String tooltipStyleId = "";
        public String damageTypeId = "";
        public String damageResistantTypeIds = "";
        public String noteBlockSoundId = "";
        public String breakSoundId = "";
        public String paintingVariantId = "";
        public String blockStateProperties = "";
        public String blocksAttacksBlockDelaySeconds = "";
        public String blocksAttacksDisableCooldownScale = "";
        public String blocksAttacksBlockSoundId = "";
        public String blocksAttacksDisableSoundId = "";
        public boolean deathProtection;
        public boolean glider;
        public boolean intangibleProjectile;
        public boolean piercingDealsKnockback;
        public boolean piercingDismounts;
        public String piercingSoundId = "";
        public String piercingHitSoundId = "";
        public String kineticContactCooldownTicks = "";
        public String kineticDelayTicks = "";
        public String kineticForwardMovement = "";
        public String kineticDamageMultiplier = "";
        public String kineticSoundId = "";
        public String kineticHitSoundId = "";
        public String swingAnimationType = "";
        public String swingAnimationDuration = "";

        public final SignData sign = new SignData();

        public String spawnerEntityId = "";
        public String spawnerDelay = "";
        public String spawnerMinSpawnDelay = "";
        public String spawnerMaxSpawnDelay = "";
        public String spawnerSpawnCount = "";
        public String spawnerMaxNearbyEntities = "";
        public String spawnerRequiredPlayerRange = "";
        public String spawnerSpawnRange = "";
        public boolean spawnerUsePotentials;
        public final List<SpawnerPotentialDraft> spawnerPotentials = new ArrayList<>();

        public boolean armorStandSmall;
        public boolean armorStandShowArms;
        public boolean armorStandNoBasePlate;
        public boolean armorStandInvisible;
        public boolean armorStandNoGravity;
        public boolean armorStandInvulnerable;
        public boolean armorStandCustomNameVisible;
        public boolean armorStandMarker;
        public String armorStandCustomName = "";
        public String armorStandDisabledSlots = "";
        public String armorStandScale = "";
        public int armorStandSelectedPreset = -1;
        public final ArmorStandPoseDraft armorStandPose = new ArmorStandPoseDraft();

        public boolean itemFrameInvisible;
        public boolean itemFrameFixed;
        public boolean itemFrameNoGravity;
        public boolean itemFrameInvulnerable;
        public boolean itemFrameCustomNameVisible;
        public String itemFrameCustomName = "";
        public String itemFrameRotation = "";
        public String itemFrameDropChance = "";
        public String itemFrameFacing = "";

        public String spawnEggEntityId = "";
        public boolean uiSpawnEggEntityCollapsed;
        public boolean uiSpawnEggNameCollapsed;
        public boolean uiSpawnEggFlagsCollapsed;
        public boolean uiSpawnEggValuesCollapsed;
        public boolean uiSpawnEggVillagerCollapsed;
        public boolean spawnEggNoAi;
        public boolean spawnEggSilent;
        public boolean spawnEggNoGravity;
        public boolean spawnEggGlowing;
        public boolean spawnEggInvulnerable;
        public boolean spawnEggPersistenceRequired;
        public boolean spawnEggCustomNameVisible;
        public String spawnEggCustomName = "";
        public String spawnEggHealth = "";
        public String spawnEggVillagerTypeId = "";
        public String spawnEggVillagerProfessionId = "";
        public String spawnEggVillagerLevel = "";
        public final List<VillagerTradeDraft> spawnEggVillagerTrades = new ArrayList<>();

        public String potionId = "";
        public String potionCustomColor = "";
        public String potionCustomName = "";
        public final List<PotionEffectDraft> potionEffects = new ArrayList<>();

        public final List<SuspiciousStewEffectDraft> stewEffects = new ArrayList<>();

        public String dyedColor = "";

        public String trimMaterialId = "";
        public String trimPatternId = "";

        public String profileName = "";
        public String profileUuid = "";
        public String profileTextureValue = "";
        public String profileTextureSignature = "";

        public String bucketAxolotlVariant = "";
        public String bucketSalmonSize = "";
        public String bucketTropicalPattern = "";
        public String bucketTropicalBaseColor = "";
        public String bucketTropicalPatternColor = "";
        public String bucketPuffState = "";
        public boolean bucketNoAi;
        public boolean bucketSilent;
        public boolean bucketNoGravity;
        public boolean bucketGlowing;
        public boolean bucketInvulnerable;
        public String bucketHealth = "";

        public String instrumentId = "";
        public String jukeboxSongId = "";

        public String mapColor = "";
        public String mapPostProcessing = "";
        public String mapId = "";
        public final List<MapDecorationDraft> mapDecorations = new ArrayList<>();
        public boolean lodestoneEnabled;
        public boolean lodestoneTracked;
        public String lodestoneDimensionId = "";
        public String lodestoneX = "";
        public String lodestoneY = "";
        public String lodestoneZ = "";

        public String equippableSlot = "";
        public String equippableEquipSoundId = "";
        public String equippableShearingSoundId = "";
        public boolean equippableDispensable;
        public boolean equippableSwappable;
        public boolean equippableDamageOnHurt;
        public boolean equippableEquipOnInteract;
        public boolean equippableCanBeSheared;

        public String weaponItemDamagePerAttack = "";
        public String weaponDisableBlockingForSeconds = "";
        public String toolDefaultMiningSpeed = "";
        public String toolDamagePerBlock = "";
        public boolean toolCanDestroyBlocksInCreative;
        public final List<String> repairableItemIds = new ArrayList<>();
        public String attackRangeMinReach = "";
        public String attackRangeMaxReach = "";
        public String attackRangeMinCreativeReach = "";
        public String attackRangeMaxCreativeReach = "";
        public String attackRangeHitboxMargin = "";
        public String attackRangeMobFactor = "";

        public String bannerBaseColor = "";
        public final List<BannerLayerDraft> bannerLayers = new ArrayList<>();
        public int draggingBannerLayer = -1;

        public String fireworkFlightDuration = "1";
        public final List<FireworkExplosionDraft> rocketExplosions = new ArrayList<>();
        public final FireworkExplosionDraft starExplosion = new FireworkExplosionDraft();
    }

    public static final class SpawnerPotentialDraft {
        public String entityId = "";
        public String weight = "1";
    }

    public static final class ArmorStandPoseDraft {
        public final RotationDraft head = new RotationDraft("0", "0", "0");
        public final RotationDraft body = new RotationDraft("0", "0", "0");
        public final RotationDraft leftArm = new RotationDraft("-10", "0", "-10");
        public final RotationDraft rightArm = new RotationDraft("-15", "0", "10");
        public final RotationDraft leftLeg = new RotationDraft("-1", "0", "-1");
        public final RotationDraft rightLeg = new RotationDraft("1", "0", "1");
    }

    public static final class RotationDraft {
        public String x;
        public String y;
        public String z;

        public RotationDraft(String x, String y, String z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    public static final class ContainerEntryDraft {
        public String slot = "0";
        public String itemId = "";
        public String count = "1";
        public ItemStack templateStack = ItemStack.EMPTY;

        public static ContainerEntryDraft fromSlot(int slotIndex, ItemStack stack) {
            ContainerEntryDraft draft = new ContainerEntryDraft();
            draft.slot = Integer.toString(slotIndex);
            draft.itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            draft.count = Integer.toString(stack.getCount());
            draft.templateStack = stack.copy();
            return draft;
        }
    }

    public static final class SignData {
        public final SignSideDraft front = new SignSideDraft();
        public final SignSideDraft back = new SignSideDraft();
        public boolean waxed;
        public String boardStyle = "oak";
    }

    public static final class SignSideDraft {
        public final List<String> lines = new ArrayList<>();
        public String color = DyeColor.BLACK.name();
        public boolean glowing;

        public SignSideDraft() {
            for (int index = 0; index < 4; index++) {
                this.lines.add("");
            }
        }
    }

    public static final class PotionEffectDraft {
        public String effectId = "";
        public String duration = "200";
        public String amplifier = "0";
        public boolean ambient;
        public boolean visible = true;
        public boolean showIcon = true;
    }

    public static final class SuspiciousStewEffectDraft {
        public String effectId = "";
        public String duration = "160";
    }

    public static final class FireworkExplosionDraft {
        public String shape = FireworkExplosion.Shape.SMALL_BALL.name();
        public String colors = "#FF0000";
        public String fadeColors = "";
        public boolean trail;
        public boolean twinkle;

        public static FireworkExplosionDraft fromExplosion(FireworkExplosion explosion) {
            FireworkExplosionDraft draft = new FireworkExplosionDraft();
            draft.shape = explosion.shape().name();
            draft.colors = ValidationUtil.joinHexColors(explosion.colors());
            draft.fadeColors = ValidationUtil.joinHexColors(explosion.fadeColors());
            draft.trail = explosion.hasTrail();
            draft.twinkle = explosion.hasTwinkle();
            return draft;
        }
    }

    public static final class BannerLayerDraft {
        public String patternId = "";
        public String color = "";

        public static BannerLayerDraft fromLayer(BannerPatternLayers.Layer layer) {
            BannerLayerDraft draft = new BannerLayerDraft();
            draft.patternId = layer.pattern().unwrapKey().map(key -> key.identifier().toString()).orElse("");
            draft.color = layer.color().name();
            return draft;
        }
    }

    public static final class BeeOccupantDraft {
        public String entityId = "minecraft:bee";
        public String ticksInHive = "0";
        public String minTicksInHive = "0";
        public boolean uiCollapsed = true;
    }

    public static final class ChargedProjectileDraft {
        public String itemId = "";
        public String count = "1";
        public boolean uiCollapsed = true;

        public static ChargedProjectileDraft fromStack(ItemStack stack) {
            ChargedProjectileDraft draft = new ChargedProjectileDraft();
            draft.itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            draft.count = Integer.toString(stack.getCount());
            return draft;
        }
    }

    public static final class VillagerTradeDraft {
        public final TradeStackDraft buy = new TradeStackDraft();
        public final TradeStackDraft buyB = new TradeStackDraft();
        public final TradeStackDraft sell = new TradeStackDraft();
        public String maxUses = "16";
        public String uses = "0";
        public String villagerXp = "1";
        public String priceMultiplier = "0.05";
        public String demand = "";
        public String specialPrice = "";
        public boolean rewardExp = true;
        public boolean uiCollapsed;
    }

    public static final class TradeStackDraft {
        public String itemId = "";
        public String count = "1";
    }

    public static final class ConsumableEffectDraft {
        public static final String TYPE_APPLY_EFFECTS = "minecraft:apply_effects";
        public static final String TYPE_PLAY_SOUND = "minecraft:play_sound";

        public String type = TYPE_APPLY_EFFECTS;
        public String probability = "1.0";
        public String soundId = "";
        public boolean uiCollapsed = true;
        public final List<PotionEffectDraft> effects = new ArrayList<>();
    }

    public static final class MapDecorationDraft {
        public String key = "";
        public String typeId = "";
        public String x = "0";
        public String z = "0";
        public String rotation = "0";
        public boolean uiCollapsed = true;
    }
}
