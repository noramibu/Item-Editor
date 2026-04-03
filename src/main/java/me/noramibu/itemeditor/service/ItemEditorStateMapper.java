package me.noramibu.itemeditor.service;

import me.noramibu.itemeditor.editor.ItemEditorState;
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
import net.minecraft.world.item.JukeboxPlayable;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.item.component.InstrumentComponent;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.MapItemColor;
import net.minecraft.world.item.component.MapPostProcessing;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.item.component.SuspiciousStewEffects;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.component.WritableBookContent;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.animal.Salmon;
import net.minecraft.world.entity.animal.TropicalFish;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.level.block.entity.SignText;

import java.util.Optional;

public final class ItemEditorStateMapper {

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

        TooltipDisplay tooltipDisplay = stack.getOrDefault(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT);
        state.hideTooltip = tooltipDisplay.hideTooltip();
        tooltipDisplay.hiddenComponents().forEach(componentType -> {
            var key = net.minecraft.core.registries.BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(componentType);
            if (key != null) {
                state.hiddenTooltipComponents.add(key.toString());
            }
        });

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
            SignText front = blockTag.read("front_text", SignText.DIRECT_CODEC).orElseGet(SignText::new);
            SignText back = blockTag.read("back_text", SignText.DIRECT_CODEC).orElseGet(SignText::new);

            this.readSignSide(front, state.special.sign.front);
            this.readSignSide(back, state.special.sign.back);
            state.special.sign.waxed = blockTag.getBooleanOr("is_waxed", false);
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

        Axolotl.Variant axolotlVariant = stack.get(DataComponents.AXOLOTL_VARIANT);
        if (axolotlVariant != null) {
            state.special.bucketAxolotlVariant = axolotlVariant.getSerializedName();
        }

        Salmon.Variant salmonVariant = stack.get(DataComponents.SALMON_SIZE);
        if (salmonVariant != null) {
            state.special.bucketSalmonSize = salmonVariant.getSerializedName();
        }

        TropicalFish.Pattern tropicalPattern = stack.get(DataComponents.TROPICAL_FISH_PATTERN);
        if (tropicalPattern != null) {
            state.special.bucketTropicalPattern = tropicalPattern.getSerializedName();
        }

        DyeColor tropicalBaseColor = stack.get(DataComponents.TROPICAL_FISH_BASE_COLOR);
        if (tropicalBaseColor != null) {
            state.special.bucketTropicalBaseColor = tropicalBaseColor.name();
        }

        DyeColor tropicalPatternColor = stack.get(DataComponents.TROPICAL_FISH_PATTERN_COLOR);
        if (tropicalPatternColor != null) {
            state.special.bucketTropicalPatternColor = tropicalPatternColor.name();
        }

        CustomData bucketEntityData = stack.get(DataComponents.BUCKET_ENTITY_DATA);
        if (bucketEntityData != null) {
            var tag = bucketEntityData.copyTag();
            tag.getInt("PuffState").ifPresent(value -> state.special.bucketPuffState = Integer.toString(value));
            state.special.bucketNoAi = tag.getBooleanOr("NoAI", false);
            state.special.bucketSilent = tag.getBooleanOr("Silent", false);
            state.special.bucketNoGravity = tag.getBooleanOr("NoGravity", false);
            state.special.bucketGlowing = tag.getBooleanOr("Glowing", false);
            state.special.bucketInvulnerable = tag.getBooleanOr("Invulnerable", false);
            tag.getFloat("Health").ifPresent(value -> state.special.bucketHealth = Float.toString(value));
        }

        InstrumentComponent instrument = stack.get(DataComponents.INSTRUMENT);
        if (instrument != null) {
            instrument.instrument().key().map(key -> key.location().toString()).ifPresent(id -> state.special.instrumentId = id);
        }

        JukeboxPlayable jukeboxPlayable = stack.get(DataComponents.JUKEBOX_PLAYABLE);
        if (jukeboxPlayable != null) {
            jukeboxPlayable.song().key().map(key -> key.location().toString()).ifPresent(id -> state.special.jukeboxSongId = id);
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

        net.minecraft.nbt.CompoundTag spawnDataTag = blockTag.getCompoundOrEmpty("SpawnData");
        net.minecraft.nbt.CompoundTag spawnEntityTag = spawnDataTag.getCompoundOrEmpty("entity");
        special.spawnerEntityId = spawnEntityTag.getStringOr("id", "");
        if (special.spawnerEntityId.isBlank()) {
            special.spawnerEntityId = spawnDataTag.getStringOr("id", "");
        }

        ListTag potentialsTag = blockTag.getListOrEmpty("SpawnPotentials");
        if (!potentialsTag.isEmpty()) {
            special.spawnerUsePotentials = true;
            special.spawnerPotentials.clear();
            for (int index = 0; index < potentialsTag.size(); index++) {
                var potentialTag = potentialsTag.getCompoundOrEmpty(index);
                var dataTag = potentialTag.getCompoundOrEmpty("data");
                var entityTag = dataTag.getCompoundOrEmpty("entity");
                String entityId = entityTag.getStringOr("id", "");
                if (entityId.isBlank()) {
                    entityId = dataTag.getStringOr("id", "");
                }

                ItemEditorState.SpawnerPotentialDraft draft = new ItemEditorState.SpawnerPotentialDraft();
                draft.entityId = entityId;
                int weight = Math.max(1, potentialTag.getIntOr("weight", 1));
                draft.weight = Integer.toString(weight);
                special.spawnerPotentials.add(draft);
            }
        }
    }

    private static String readOptionalInt(net.minecraft.nbt.CompoundTag tag, String key) {
        return tag.getInt(key).map(String::valueOf).orElse("");
    }
}
