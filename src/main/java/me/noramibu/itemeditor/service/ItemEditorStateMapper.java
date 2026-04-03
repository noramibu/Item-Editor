package me.noramibu.itemeditor.service;

import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.util.NbtCompatUtil;
import me.noramibu.itemeditor.util.TextComponentUtil;
import me.noramibu.itemeditor.util.ValidationUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Instrument;
import net.minecraft.world.item.JukeboxPlayable;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.MapItemColor;
import net.minecraft.world.item.component.MapPostProcessing;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.item.component.SuspiciousStewEffects;
import net.minecraft.world.item.component.WritableBookContent;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.level.block.entity.SignText;

import java.util.Optional;

public final class ItemEditorStateMapper {

    private static final java.util.List<String> DEFAULT_HIDDEN_COMPONENT_IDS = java.util.List.of(
            "minecraft:enchantments",
            "minecraft:stored_enchantments",
            "minecraft:attribute_modifiers",
            "minecraft:unbreakable",
            "minecraft:dyed_color",
            "minecraft:trim",
            "minecraft:can_break",
            "minecraft:can_place_on",
            "minecraft:potion_contents",
            "minecraft:fireworks",
            "minecraft:firework_explosion",
            "minecraft:banner_patterns",
            "minecraft:map_color"
    );

    public ItemEditorState map(ItemStack stack) {
        ItemEditorState state = new ItemEditorState();

        state.customName = Optional.ofNullable(stack.get(DataComponents.CUSTOM_NAME))
                .map(TextComponentUtil::toMarkup)
                .orElse("");
        state.count = Integer.toString(stack.getCount());

        if (stack.isDamageableItem() || stack.has(DataComponents.MAX_DAMAGE)) {
            state.currentDamage = Integer.toString(stack.getDamageValue());
            state.maxDamage = Integer.toString(stack.getMaxDamage());
        }

        Integer repairCost = stack.get(DataComponents.REPAIR_COST);
        if (repairCost != null) {
            state.repairCost = Integer.toString(repairCost);
        }

        state.unbreakable = stack.has(DataComponents.UNBREAKABLE);

        Boolean glintOverride = stack.get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE);
        if (glintOverride != null) {
            state.glintOverrideEnabled = true;
            state.glintOverride = glintOverride;
        }

        state.rarity = stack.getRarity().name();

        var itemModel = stack.get(DataComponents.ITEM_MODEL);
        if (itemModel != null) {
            state.itemModelId = itemModel.toString();
        }

        CustomModelData customModelData = stack.get(DataComponents.CUSTOM_MODEL_DATA);
        if (customModelData != null) {
            Float firstFloat = customModelData.getFloat(0);
            if (firstFloat != null) state.customModelFloat = firstFloat.toString();
            Boolean firstFlag = customModelData.getBoolean(0);
            if (firstFlag != null) {
                state.customModelFlagEnabled = true;
                state.customModelFlag = firstFlag;
            }
            String firstString = customModelData.getString(0);
            if (firstString != null) state.customModelString = firstString;
            Integer firstColor = customModelData.getColor(0);
            if (firstColor != null) state.customModelColor = ValidationUtil.toHex(firstColor);
        }

        ItemLore lore = stack.get(DataComponents.LORE);
        if (lore != null) {
            lore.lines().stream()
                    .map(ItemEditorState.LoreLineDraft::fromComponent)
                    .forEach(state.loreLines::add);
        }

        state.hideTooltip = stack.has(DataComponents.HIDE_TOOLTIP);
        if (stack.has(DataComponents.HIDE_ADDITIONAL_TOOLTIP)) {
            state.hiddenTooltipComponents.addAll(DEFAULT_HIDDEN_COMPONENT_IDS);
        }

        ItemEnchantments enchantments = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        enchantments.entrySet().stream()
                .map(entry -> ItemEditorState.EnchantmentDraft.fromEntry(entry.getKey(), entry.getIntValue()))
                .forEach(state.enchantments::add);

        ItemEnchantments storedEnchantments = stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
        storedEnchantments.entrySet().stream()
                .map(entry -> ItemEditorState.EnchantmentDraft.fromEntry(entry.getKey(), entry.getIntValue()))
                .forEach(state.storedEnchantments::add);

        ItemAttributeModifiers modifiers = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        modifiers.modifiers().stream()
                .map(ItemEditorState.AttributeModifierDraft::fromEntry)
                .forEach(state.attributeModifiers::add);

        if (stack.is(Items.WRITTEN_BOOK)) {
            state.book.writtenBook = true;
            WrittenBookContent content = stack.getOrDefault(DataComponents.WRITTEN_BOOK_CONTENT, WrittenBookContent.EMPTY);
            state.book.title = content.title().raw();
            state.book.author = content.author();
            state.book.generation = Integer.toString(content.generation());
            content.pages().stream().map(Filterable::raw).map(TextComponentUtil::toMarkup).forEach(state.book.pages::add);
        } else if (stack.is(Items.WRITABLE_BOOK)) {
            WritableBookContent content = stack.getOrDefault(DataComponents.WRITABLE_BOOK_CONTENT, WritableBookContent.EMPTY);
            content.pages().stream().map(Filterable::raw).forEach(state.book.pages::add);
        }

        ItemContainerContents containerContents = stack.get(DataComponents.CONTAINER);
        if (containerContents != null) {
            var containerStacks = containerContents.stream().toList();
            for (int slot = 0; slot < containerStacks.size(); slot++) {
                ItemStack entryStack = containerStacks.get(slot);
                if (!entryStack.isEmpty()) {
                    state.special.containerEntries.add(ItemEditorState.ContainerEntryDraft.fromSlot(slot, entryStack));
                }
            }
            if (!state.special.containerEntries.isEmpty()) {
                state.special.selectedContainerSlot = state.special.containerEntries.stream()
                        .map(draft -> {
                            try {
                                return Integer.parseInt(draft.slot.trim());
                            } catch (NumberFormatException ignored) {
                                return -1;
                            }
                        })
                        .filter(slot -> slot >= 0)
                        .findFirst()
                        .orElse(0);
            }
        }

        BundleContents bundleContents = stack.get(DataComponents.BUNDLE_CONTENTS);
        if (bundleContents != null) {
            int index = 0;
            for (ItemStack entryStack : bundleContents.itemCopyStream().toList()) {
                if (entryStack.isEmpty()) {
                    continue;
                }
                ItemEditorState.ContainerEntryDraft draft = new ItemEditorState.ContainerEntryDraft();
                draft.slot = Integer.toString(index);
                draft.itemId = BuiltInRegistries.ITEM.getKey(entryStack.getItem()).toString();
                draft.count = Integer.toString(entryStack.getCount());
                draft.templateStack = entryStack.copy();
                state.special.bundleEntries.add(draft);
                index++;
            }

            if (!state.special.bundleEntries.isEmpty()) {
                int preferred = bundleContents.getSelectedItem();
                if (preferred == BundleContents.NO_SELECTED_ITEM_INDEX) {
                    preferred = 0;
                }
                state.special.selectedBundleIndex = Math.clamp(preferred, 0, state.special.bundleEntries.size() - 1);
            }
        }

        CustomData blockEntityData = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (blockEntityData != null && (stack.getItem() instanceof net.minecraft.world.item.SignItem || stack.getItem() instanceof net.minecraft.world.item.HangingSignItem)) {
            var blockTag = blockEntityData.copyTag();
            SignText front = NbtCompatUtil.readOrDefault(blockTag, "front_text", SignText.DIRECT_CODEC, new SignText());
            SignText back = NbtCompatUtil.readOrDefault(blockTag, "back_text", SignText.DIRECT_CODEC, new SignText());

            this.readSignSide(front, state.special.sign.front);
            this.readSignSide(back, state.special.sign.back);
            state.special.sign.waxed = NbtCompatUtil.getBooleanOr(blockTag, "is_waxed", false);
        }
        if (blockEntityData != null && stack.is(Items.SPAWNER)) {
            this.readSpawnerData(blockEntityData.copyTag(), state.special);
        }

        PotionContents potionContents = stack.get(DataComponents.POTION_CONTENTS);
        if (potionContents != null) {
            potionContents.potion().flatMap(holder -> holder.unwrapKey().map(key -> key.location())).ifPresent(identifier -> state.special.potionId = identifier.toString());
            potionContents.customColor().ifPresent(color -> state.special.potionCustomColor = ValidationUtil.toHex(color));
            potionContents.customName().ifPresent(name -> state.special.potionCustomName = name);
            potionContents.customEffects().forEach(effect -> {
                ItemEditorState.PotionEffectDraft draft = new ItemEditorState.PotionEffectDraft();
                draft.effectId = effect.getEffect().unwrapKey().map(key -> key.location().toString()).orElse("");
                draft.duration = Integer.toString(effect.getDuration());
                draft.amplifier = Integer.toString(effect.getAmplifier());
                draft.ambient = effect.isAmbient();
                draft.visible = effect.isVisible();
                draft.showIcon = effect.showIcon();
                state.special.potionEffects.add(draft);
            });
        }

        SuspiciousStewEffects stewEffects = stack.get(DataComponents.SUSPICIOUS_STEW_EFFECTS);
        if (stewEffects != null) {
            stewEffects.effects().forEach(effect -> {
                ItemEditorState.SuspiciousStewEffectDraft draft = new ItemEditorState.SuspiciousStewEffectDraft();
                draft.effectId = effect.effect().unwrapKey().map(key -> key.location().toString()).orElse("");
                draft.duration = Integer.toString(effect.duration());
                state.special.stewEffects.add(draft);
            });
        }

        Fireworks fireworks = stack.get(DataComponents.FIREWORKS);
        if (fireworks != null) {
            state.special.fireworkFlightDuration = Integer.toString(fireworks.flightDuration());
            fireworks.explosions().stream()
                    .map(ItemEditorState.FireworkExplosionDraft::fromExplosion)
                    .forEach(state.special.rocketExplosions::add);
        }

        FireworkExplosion fireworkExplosion = stack.get(DataComponents.FIREWORK_EXPLOSION);
        if (fireworkExplosion != null) {
            ItemEditorState.FireworkExplosionDraft draft = ItemEditorState.FireworkExplosionDraft.fromExplosion(fireworkExplosion);
            state.special.starExplosion.shape = draft.shape;
            state.special.starExplosion.colors = draft.colors;
            state.special.starExplosion.fadeColors = draft.fadeColors;
            state.special.starExplosion.trail = draft.trail;
            state.special.starExplosion.twinkle = draft.twinkle;
        }

        BannerPatternLayers bannerPatterns = stack.get(DataComponents.BANNER_PATTERNS);
        if (bannerPatterns != null) {
            bannerPatterns.layers().stream()
                    .map(ItemEditorState.BannerLayerDraft::fromLayer)
                    .forEach(state.special.bannerLayers::add);
        }
        var bannerBaseColor = stack.get(DataComponents.BASE_COLOR);
        if (bannerBaseColor != null) {
            state.special.bannerBaseColor = bannerBaseColor.name();
        }

        DyedItemColor dyedItemColor = stack.get(DataComponents.DYED_COLOR);
        if (dyedItemColor != null) {
            state.special.dyedColor = ValidationUtil.toHex(dyedItemColor.rgb());
        }

        ArmorTrim trim = stack.get(DataComponents.TRIM);
        if (trim != null) {
            trim.material().unwrapKey().ifPresent(key -> state.special.trimMaterialId = key.location().toString());
            trim.pattern().unwrapKey().ifPresent(key -> state.special.trimPatternId = key.location().toString());
        }

        ResolvableProfile profile = stack.get(DataComponents.PROFILE);
        if (profile != null) {
            profile.name().ifPresent(name -> state.special.profileName = name);
            profile.id().ifPresent(id -> state.special.profileUuid = id.toString());
            profile.properties().get("textures").stream().findFirst().ifPresent(property -> {
                state.special.profileTextureValue = property.value();
                state.special.profileTextureSignature = property.signature() == null ? "" : property.signature();
            });
        }

        CustomData bucketEntityData = stack.get(DataComponents.BUCKET_ENTITY_DATA);
        if (bucketEntityData != null) {
            var tag = bucketEntityData.copyTag();
            if (tag.contains("PuffState", net.minecraft.nbt.Tag.TAG_ANY_NUMERIC)) {
                state.special.bucketPuffState = Integer.toString(tag.getInt("PuffState"));
            }
            state.special.bucketNoAi = NbtCompatUtil.getBooleanOr(tag, "NoAI", false);
            state.special.bucketSilent = NbtCompatUtil.getBooleanOr(tag, "Silent", false);
            state.special.bucketNoGravity = NbtCompatUtil.getBooleanOr(tag, "NoGravity", false);
            state.special.bucketGlowing = NbtCompatUtil.getBooleanOr(tag, "Glowing", false);
            state.special.bucketInvulnerable = NbtCompatUtil.getBooleanOr(tag, "Invulnerable", false);
            if (tag.contains("Health", net.minecraft.nbt.Tag.TAG_ANY_NUMERIC)) {
                state.special.bucketHealth = Float.toString(tag.getFloat("Health"));
            }
            if (stack.is(Items.AXOLOTL_BUCKET) && tag.contains("Variant", net.minecraft.nbt.Tag.TAG_ANY_NUMERIC)) {
                state.special.bucketAxolotlVariant = Axolotl.Variant.byId(tag.getInt("Variant")).getSerializedName();
            }
            if (stack.is(Items.SALMON_BUCKET) && tag.contains("type", net.minecraft.nbt.Tag.TAG_STRING)) {
                state.special.bucketSalmonSize = tag.getString("type");
            }
            if (stack.is(Items.TROPICAL_FISH_BUCKET) && tag.contains("BucketVariantTag", net.minecraft.nbt.Tag.TAG_ANY_NUMERIC)) {
                int variantId = tag.getInt("BucketVariantTag");
                state.special.bucketTropicalPattern = net.minecraft.world.entity.animal.TropicalFish.getPattern(variantId).getSerializedName();
                state.special.bucketTropicalBaseColor = net.minecraft.world.entity.animal.TropicalFish.getBaseColor(variantId).name();
                state.special.bucketTropicalPatternColor = net.minecraft.world.entity.animal.TropicalFish.getPatternColor(variantId).name();
            }
        }

        net.minecraft.core.Holder<Instrument> instrument = stack.get(DataComponents.INSTRUMENT);
        if (instrument != null) {
            instrument.unwrapKey().map(key -> key.location().toString()).ifPresent(id -> state.special.instrumentId = id);
        }

        JukeboxPlayable jukeboxPlayable = stack.get(DataComponents.JUKEBOX_PLAYABLE);
        if (jukeboxPlayable != null) {
            state.special.jukeboxSongId = jukeboxPlayable.song().key().location().toString();
        }

        MapItemColor mapColor = stack.get(DataComponents.MAP_COLOR);
        if (mapColor != null) {
            state.special.mapColor = ValidationUtil.toHex(mapColor.rgb());
        }

        MapPostProcessing mapPostProcessing = stack.get(DataComponents.MAP_POST_PROCESSING);
        if (mapPostProcessing != null) {
            state.special.mapPostProcessing = mapPostProcessing.name();
        }

        return state;
    }

    private void readSignSide(SignText signText, ItemEditorState.SignSideDraft sideDraft) {
        sideDraft.lines.clear();
        int baseColor = signText.getColor().getTextColor();
        for (int index = 0; index < 4; index++) {
            Component line = signText.getMessage(index, false);
            sideDraft.lines.add(TextComponentUtil.toMarkup(this.stripBaseSignColor(line, baseColor)));
        }
        sideDraft.color = signText.getColor().name();
        sideDraft.glowing = signText.hasGlowingText();
    }

    private Component stripBaseSignColor(Component line, int baseColor) {
        MutableComponent rebuilt = Component.empty();
        for (Component flat : line.toFlatList(Style.EMPTY)) {
            Style style = flat.getStyle();
            TextColor color = style.getColor();
            if (color != null && color.getValue() == baseColor) {
                style = style.withColor((TextColor) null);
            }
            rebuilt.append(Component.literal(flat.getString()).withStyle(style));
        }
        return rebuilt;
    }

    private void readSpawnerData(net.minecraft.nbt.CompoundTag blockTag, ItemEditorState.SpecialData special) {
        special.spawnerDelay = readOptionalInt(blockTag, "Delay");
        special.spawnerMinSpawnDelay = readOptionalInt(blockTag, "MinSpawnDelay");
        special.spawnerMaxSpawnDelay = readOptionalInt(blockTag, "MaxSpawnDelay");
        special.spawnerSpawnCount = readOptionalInt(blockTag, "SpawnCount");
        special.spawnerMaxNearbyEntities = readOptionalInt(blockTag, "MaxNearbyEntities");
        special.spawnerRequiredPlayerRange = readOptionalInt(blockTag, "RequiredPlayerRange");
        special.spawnerSpawnRange = readOptionalInt(blockTag, "SpawnRange");

        net.minecraft.nbt.CompoundTag spawnDataTag = NbtCompatUtil.getCompoundOrEmpty(blockTag, "SpawnData");
        net.minecraft.nbt.CompoundTag spawnEntityTag = NbtCompatUtil.getCompoundOrEmpty(spawnDataTag, "entity");
        special.spawnerEntityId = NbtCompatUtil.getStringOr(spawnEntityTag, "id", "");
        if (special.spawnerEntityId.isBlank()) {
            special.spawnerEntityId = NbtCompatUtil.getStringOr(spawnDataTag, "id", "");
        }

        ListTag potentialsTag = NbtCompatUtil.getListOrEmpty(blockTag, "SpawnPotentials");
        if (!potentialsTag.isEmpty()) {
            special.spawnerUsePotentials = true;
            special.spawnerPotentials.clear();
            for (int index = 0; index < potentialsTag.size(); index++) {
                var potentialTag = NbtCompatUtil.getCompoundOrEmpty(potentialsTag, index);
                var dataTag = NbtCompatUtil.getCompoundOrEmpty(potentialTag, "data");
                var entityTag = NbtCompatUtil.getCompoundOrEmpty(dataTag, "entity");
                String entityId = NbtCompatUtil.getStringOr(entityTag, "id", "");
                if (entityId.isBlank()) {
                    entityId = NbtCompatUtil.getStringOr(dataTag, "id", "");
                }

                ItemEditorState.SpawnerPotentialDraft draft = new ItemEditorState.SpawnerPotentialDraft();
                draft.entityId = entityId;
                int weight = Math.max(1, NbtCompatUtil.getIntOr(potentialTag, "weight", 1));
                draft.weight = Integer.toString(weight);
                special.spawnerPotentials.add(draft);
            }
        }
    }

    private static String readOptionalInt(net.minecraft.nbt.CompoundTag tag, String key) {
        return tag.contains(key, net.minecraft.nbt.Tag.TAG_ANY_NUMERIC) ? Integer.toString(tag.getInt(key)) : "";
    }
}
