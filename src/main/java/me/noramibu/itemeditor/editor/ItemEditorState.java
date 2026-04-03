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
    public boolean hideTooltip;
    public final Set<String> hiddenTooltipComponents = new LinkedHashSet<>();
    public final List<LoreLineDraft> loreLines = new ArrayList<>();
    public final List<AttributeModifierDraft> attributeModifiers = new ArrayList<>();
    public final List<EnchantmentDraft> enchantments = new ArrayList<>();
    public final List<EnchantmentDraft> storedEnchantments = new ArrayList<>();
    public boolean unsafeEnchantments;
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

        public static AttributeModifierDraft fromEntry(ItemAttributeModifiers.Entry entry) {
            AttributeModifierDraft draft = new AttributeModifierDraft();
            draft.attributeId = entry.attribute().unwrapKey().map(key -> key.location().toString()).orElse("");
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

        public static EnchantmentDraft fromEntry(Holder<net.minecraft.world.item.enchantment.Enchantment> enchantment, int level) {
            EnchantmentDraft draft = new EnchantmentDraft();
            draft.enchantmentId = enchantment.unwrapKey().map(key -> key.location().toString()).orElse("");
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
    }

    public static final class SpecialData {
        public final List<ContainerEntryDraft> containerEntries = new ArrayList<>();
        public int selectedContainerSlot = -1;
        public int draggingContainerSlot = -1;
        public final List<ContainerEntryDraft> bundleEntries = new ArrayList<>();
        public int selectedBundleIndex = -1;

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

        public String bannerBaseColor = "";
        public final List<BannerLayerDraft> bannerLayers = new ArrayList<>();

        public String fireworkFlightDuration = "1";
        public final List<FireworkExplosionDraft> rocketExplosions = new ArrayList<>();
        public final FireworkExplosionDraft starExplosion = new FireworkExplosionDraft();
    }

    public static final class SpawnerPotentialDraft {
        public String entityId = "";
        public String weight = "1";
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
            draft.patternId = layer.pattern().unwrapKey().map(key -> key.location().toString()).orElse("");
            draft.color = layer.color().name();
            return draft;
        }
    }
}
