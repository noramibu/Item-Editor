package me.noramibu.itemeditor.service;

import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.util.TextComponentUtil;
import me.noramibu.itemeditor.util.ItemEditorCapabilities;
import me.noramibu.itemeditor.util.ValidationUtil;
import net.minecraft.advancements.criterion.DataComponentMatchers;
import net.minecraft.advancements.criterion.ItemPredicate;
import net.minecraft.advancements.criterion.MinMaxBounds;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.network.Filterable;
import net.minecraft.resources.Identifier;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.LockCode;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.decoration.painting.PaintingVariant;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.AdventureModePredicate;
import net.minecraft.world.item.EitherHolder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.JukeboxPlayable;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.component.AttackRange;
import net.minecraft.world.item.component.Bees;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.DamageResistant;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.item.component.InstrumentComponent;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.KineticWeapon;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.item.component.MapDecorations;
import net.minecraft.world.item.component.MapItemColor;
import net.minecraft.world.item.component.MapPostProcessing;
import net.minecraft.world.item.component.OminousBottleAmplifier;
import net.minecraft.world.item.component.PiercingWeapon;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.item.component.SeededContainerLoot;
import net.minecraft.world.item.component.SuspiciousStewEffects;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.component.UseCooldown;
import net.minecraft.world.item.component.UseEffects;
import net.minecraft.world.item.component.UseRemainder;
import net.minecraft.world.item.component.Weapon;
import net.minecraft.world.item.component.WritableBookContent;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.item.consume_effects.ConsumeEffect;
import net.minecraft.world.item.consume_effects.PlaySoundConsumeEffect;
import net.minecraft.world.item.enchantment.Enchantable;
import net.minecraft.world.item.enchantment.Repairable;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.animal.fish.Salmon;
import net.minecraft.world.entity.animal.fish.TropicalFish;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.PotDecorations;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.saveddata.maps.MapId;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ItemEditorStateMapper {

    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("([a-z0-9_.-]+:[a-z0-9_./-]+)");

    public ItemEditorState map(ItemStack stack, RegistryAccess registryAccess) {
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

        FoodProperties foodProperties = stack.get(DataComponents.FOOD);
        if (foodProperties != null) {
            state.special.foodNutrition = Integer.toString(foodProperties.nutrition());
            state.special.foodSaturation = trimTrailingZeros(foodProperties.saturation());
            state.special.foodCanAlwaysEat = foodProperties.canAlwaysEat();
        }

        Consumable consumable = stack.get(DataComponents.CONSUMABLE);
        if (consumable != null) {
            state.special.consumableConsumeSeconds = trimTrailingZeros(consumable.consumeSeconds());
            state.special.consumableAnimation = consumable.animation().name();
            setIdFromHolder(consumable.sound(), id -> state.special.consumableSoundId = id);
            state.special.consumableHasParticles = consumable.hasConsumeParticles();
            readConsumableEffects(consumable.onConsumeEffects(), state.special);
        }

        UseEffects useEffects = stack.get(DataComponents.USE_EFFECTS);
        if (useEffects != null) {
            state.special.useEffectsCanSprint = useEffects.canSprint();
            state.special.useEffectsInteractVibrations = useEffects.interactVibrations();
            state.special.useEffectsSpeedMultiplier = trimTrailingZeros(useEffects.speedMultiplier());
        }

        UseRemainder useRemainder = stack.get(DataComponents.USE_REMAINDER);
        if (useRemainder != null) {
            ItemStack remainder = useRemainder.convertInto();
            state.special.useRemainderItemId = identifierOrEmpty(BuiltInRegistries.ITEM.getKey(remainder.getItem()));
            state.special.useRemainderCount = Integer.toString(remainder.getCount());
        }

        UseCooldown useCooldown = stack.get(DataComponents.USE_COOLDOWN);
        if (useCooldown != null) {
            state.special.useCooldownSeconds = trimTrailingZeros(useCooldown.seconds());
            useCooldown.cooldownGroup().ifPresent(identifier -> state.special.useCooldownGroup = identifier.toString());
        }

        Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
        if (equippable != null) {
            state.special.equippableSlot = equippable.slot().name();
            setIdFromHolder(equippable.equipSound(), id -> state.special.equippableEquipSoundId = id);
            setIdFromHolder(equippable.shearingSound(), id -> state.special.equippableShearingSoundId = id);
            state.special.equippableDispensable = equippable.dispensable();
            state.special.equippableSwappable = equippable.swappable();
            state.special.equippableDamageOnHurt = equippable.damageOnHurt();
            state.special.equippableEquipOnInteract = equippable.equipOnInteract();
            state.special.equippableCanBeSheared = equippable.canBeSheared();
        }

        Weapon weapon = stack.get(DataComponents.WEAPON);
        if (weapon != null) {
            state.special.weaponItemDamagePerAttack = Integer.toString(weapon.itemDamagePerAttack());
            state.special.weaponDisableBlockingForSeconds = trimTrailingZeros(weapon.disableBlockingForSeconds());
        }

        Tool tool = stack.get(DataComponents.TOOL);
        if (tool != null) {
            state.special.toolDefaultMiningSpeed = trimTrailingZeros(tool.defaultMiningSpeed());
            state.special.toolDamagePerBlock = Integer.toString(tool.damagePerBlock());
            state.special.toolCanDestroyBlocksInCreative = tool.canDestroyBlocksInCreative();
        }

        Repairable repairable = stack.get(DataComponents.REPAIRABLE);
        if (repairable != null) {
            repairable.items().stream()
                    .map(Holder::unwrapKey)
                    .flatMap(Optional::stream)
                    .map(key -> key.identifier().toString())
                    .forEach(state.special.repairableItemIds::add);
        }

        AttackRange attackRange = stack.get(DataComponents.ATTACK_RANGE);
        if (attackRange != null) {
            state.special.attackRangeMinReach = trimTrailingZeros(attackRange.minRange());
            state.special.attackRangeMaxReach = trimTrailingZeros(attackRange.maxRange());
            state.special.attackRangeMinCreativeReach = trimTrailingZeros(attackRange.minCreativeRange());
            state.special.attackRangeMaxCreativeReach = trimTrailingZeros(attackRange.maxCreativeRange());
            state.special.attackRangeHitboxMargin = trimTrailingZeros(attackRange.hitboxMargin());
            state.special.attackRangeMobFactor = trimTrailingZeros(attackRange.mobFactor());
        }

        ChargedProjectiles chargedProjectiles = stack.get(DataComponents.CHARGED_PROJECTILES);
        if (chargedProjectiles != null) {
            chargedProjectiles.getItems().stream()
                    .filter(projectile -> !projectile.isEmpty())
                    .map(ItemEditorState.ChargedProjectileDraft::fromStack)
                    .forEach(state.special.chargedProjectiles::add);
        }

        Component itemName = stack.get(DataComponents.ITEM_NAME);
        if (itemName != null) {
            state.special.itemName = TextComponentUtil.toMarkup(itemName);
        }

        Integer maxStackSize = stack.get(DataComponents.MAX_STACK_SIZE);
        if (maxStackSize != null) {
            state.special.maxStackSize = Integer.toString(maxStackSize);
        }

        Float minimumAttackCharge = stack.get(DataComponents.MINIMUM_ATTACK_CHARGE);
        if (minimumAttackCharge != null) {
            state.special.minimumAttackCharge = trimTrailingZeros(minimumAttackCharge);
        }

        Enchantable enchantable = stack.get(DataComponents.ENCHANTABLE);
        if (enchantable != null) {
            state.special.enchantableValue = Integer.toString(enchantable.value());
        }

        OminousBottleAmplifier ominousBottleAmplifier = stack.get(DataComponents.OMINOUS_BOTTLE_AMPLIFIER);
        if (ominousBottleAmplifier != null) {
            state.special.ominousBottleAmplifier = Integer.toString(ominousBottleAmplifier.value());
        }

        Identifier tooltipStyle = stack.get(DataComponents.TOOLTIP_STYLE);
        if (tooltipStyle != null) {
            state.special.tooltipStyleId = tooltipStyle.toString();
        }

        state.special.glider = stack.has(DataComponents.GLIDER);
        state.special.intangibleProjectile = stack.has(DataComponents.INTANGIBLE_PROJECTILE);
        state.special.deathProtection = stack.has(DataComponents.DEATH_PROTECTION);

        EitherHolder<DamageType> damageType = stack.get(DataComponents.DAMAGE_TYPE);
        if (damageType != null) {
            setIdFromEitherHolder(damageType, id -> state.special.damageTypeId = id);
        }

        DamageResistant damageResistant = stack.get(DataComponents.DAMAGE_RESISTANT);
        if (damageResistant != null) {
            state.special.damageResistantTypeIds = damageResistant.types().location().toString();
        }

        Identifier noteBlockSound = stack.get(DataComponents.NOTE_BLOCK_SOUND);
        if (noteBlockSound != null) {
            state.special.noteBlockSoundId = noteBlockSound.toString();
        }

        Holder<net.minecraft.sounds.SoundEvent> breakSound = stack.get(DataComponents.BREAK_SOUND);
        if (breakSound != null) {
            setIdFromHolder(breakSound, id -> state.special.breakSoundId = id);
        }

        Holder<PaintingVariant> paintingVariant = stack.get(DataComponents.PAINTING_VARIANT);
        if (paintingVariant != null) {
            setIdFromHolder(paintingVariant, id -> state.special.paintingVariantId = id);
        }

        BlockItemStateProperties blockStateProperties = stack.get(DataComponents.BLOCK_STATE);
        if (blockStateProperties != null && !blockStateProperties.properties().isEmpty()) {
            state.special.blockStateProperties = String.join(
                    ", ",
                    blockStateProperties.properties().entrySet().stream()
                            .map(entry -> entry.getKey() + "=" + entry.getValue())
                            .toList()
            );
        }

        BlocksAttacks blocksAttacks = stack.get(DataComponents.BLOCKS_ATTACKS);
        if (blocksAttacks != null) {
            state.special.blocksAttacksBlockDelaySeconds = trimTrailingZeros(blocksAttacks.blockDelaySeconds());
            state.special.blocksAttacksDisableCooldownScale = trimTrailingZeros(blocksAttacks.disableCooldownScale());
            setIdFromHolder(blocksAttacks.blockSound().orElse(null), id -> state.special.blocksAttacksBlockSoundId = id);
            setIdFromHolder(blocksAttacks.disableSound().orElse(null), id -> state.special.blocksAttacksDisableSoundId = id);
        }

        PiercingWeapon piercingWeapon = stack.get(DataComponents.PIERCING_WEAPON);
        if (piercingWeapon != null) {
            state.special.piercingDealsKnockback = piercingWeapon.dealsKnockback();
            state.special.piercingDismounts = piercingWeapon.dismounts();
            setIdFromHolder(piercingWeapon.sound().orElse(null), id -> state.special.piercingSoundId = id);
            setIdFromHolder(piercingWeapon.hitSound().orElse(null), id -> state.special.piercingHitSoundId = id);
        }

        KineticWeapon kineticWeapon = stack.get(DataComponents.KINETIC_WEAPON);
        if (kineticWeapon != null) {
            state.special.kineticContactCooldownTicks = Integer.toString(kineticWeapon.contactCooldownTicks());
            state.special.kineticDelayTicks = Integer.toString(kineticWeapon.delayTicks());
            state.special.kineticForwardMovement = trimTrailingZeros(kineticWeapon.forwardMovement());
            state.special.kineticDamageMultiplier = trimTrailingZeros(kineticWeapon.damageMultiplier());
            setIdFromHolder(kineticWeapon.sound().orElse(null), id -> state.special.kineticSoundId = id);
            setIdFromHolder(kineticWeapon.hitSound().orElse(null), id -> state.special.kineticHitSoundId = id);
        }

        SwingAnimation swingAnimation = stack.get(DataComponents.SWING_ANIMATION);
        if (swingAnimation != null) {
            state.special.swingAnimationType = swingAnimation.type().name();
            state.special.swingAnimationDuration = Integer.toString(swingAnimation.duration());
        }

        AdventureModePredicate canBreak = stack.get(DataComponents.CAN_BREAK);
        if (canBreak != null) {
            state.canBreakBlockIds.addAll(this.extractBlockIds(canBreak, registryAccess));
        }

        AdventureModePredicate canPlaceOn = stack.get(DataComponents.CAN_PLACE_ON);
        if (canPlaceOn != null) {
            state.canPlaceOnBlockIds.addAll(this.extractBlockIds(canPlaceOn, registryAccess));
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
        state.canBreakShowInTooltip = !state.hiddenTooltipComponents.contains("minecraft:can_break");
        state.canPlaceOnShowInTooltip = !state.hiddenTooltipComponents.contains("minecraft:can_place_on");

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
            content.pages().stream().map(this::pageMarkup).forEach(state.book.pages::add);
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
                        .map(draft -> parseSlotOrMinusOne(draft.slot))
                        .filter(slot -> slot >= 0)
                        .findFirst()
                        .orElse(0);
            }
        }

        LockCode lockCode = stack.get(DataComponents.LOCK);
        if (lockCode != null && !LockCode.NO_LOCK.equals(lockCode)) {
            this.mapLockCode(lockCode, state.special, registryAccess);
        }

        SeededContainerLoot containerLoot = stack.get(DataComponents.CONTAINER_LOOT);
        if (containerLoot != null) {
            state.special.containerLootTableId = containerLoot.lootTable().identifier().toString();
            state.special.containerLootSeed = Long.toString(containerLoot.seed());
        }

        Bees bees = stack.get(DataComponents.BEES);
        if (bees != null) {
            state.special.beesOccupants.clear();
            for (BeehiveBlockEntity.Occupant occupant : bees.bees()) {
                ItemEditorState.BeeOccupantDraft draft = new ItemEditorState.BeeOccupantDraft();
                var beeEntityData = occupant.entityData();
                var key = BuiltInRegistries.ENTITY_TYPE.getKey(beeEntityData.type());
                draft.entityId = identifierOrEmpty(key);
                draft.ticksInHive = Integer.toString(Math.max(0, occupant.ticksInHive()));
                draft.minTicksInHive = Integer.toString(Math.max(0, occupant.minTicksInHive()));
                state.special.beesOccupants.add(draft);
            }
        }

        PotDecorations potDecorations = stack.get(DataComponents.POT_DECORATIONS);
        if (potDecorations != null) {
            setItemId(potDecorations.back().orElse(null), id -> state.special.potBackItemId = id);
            setItemId(potDecorations.left().orElse(null), id -> state.special.potLeftItemId = id);
            setItemId(potDecorations.right().orElse(null), id -> state.special.potRightItemId = id);
            setItemId(potDecorations.front().orElse(null), id -> state.special.potFrontItemId = id);
        }

        BundleContents bundleContents = stack.get(DataComponents.BUNDLE_CONTENTS);
        if (bundleContents != null) {
            int index = 0;
            for (ItemStack entryStack : bundleContents.items()) {
                Item entryItem = entryStack.getItem();
                if (entryItem == Items.AIR) {
                    continue;
                }
                int templateCount = Math.max(1, entryStack.getCount());
                ItemEditorState.ContainerEntryDraft draft = new ItemEditorState.ContainerEntryDraft();
                draft.slot = Integer.toString(index);
                draft.itemId = identifierOrEmpty(BuiltInRegistries.ITEM.getKey(entryItem));
                draft.count = Integer.toString(templateCount);
                int safePreviewCount = Math.max(1, Math.min(templateCount, entryItem.getDefaultMaxStackSize()));
                draft.templateStack = new ItemStack(entryItem, safePreviewCount);
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

        TypedEntityData<BlockEntityType<?>> blockEntityData = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (blockEntityData != null
                && (blockEntityData.type() == BlockEntityType.SIGN || blockEntityData.type() == BlockEntityType.HANGING_SIGN)) {
            var blockTag = blockEntityData.copyTagWithoutId();
            SignText front = blockTag.read("front_text", SignText.DIRECT_CODEC).orElseGet(SignText::new);
            SignText back = blockTag.read("back_text", SignText.DIRECT_CODEC).orElseGet(SignText::new);

            this.readSignSide(front, state.special.sign.front);
            this.readSignSide(back, state.special.sign.back);
            state.special.sign.waxed = blockTag.getBooleanOr("is_waxed", false);
        }
        if (blockEntityData != null && blockEntityData.type() == BlockEntityType.MOB_SPAWNER) {
            this.readSpawnerData(blockEntityData.copyTagWithoutId(), state.special);
        }
        TypedEntityData<EntityType<?>> entityData = stack.get(DataComponents.ENTITY_DATA);
        if (entityData != null && entityData.type() == EntityType.ARMOR_STAND) {
            this.readArmorStandData(entityData.copyTagWithoutId(), state.special);
        }
        if (entityData != null
                && (entityData.type() == EntityType.ITEM_FRAME || entityData.type() == EntityType.GLOW_ITEM_FRAME)) {
            this.readItemFrameData(entityData.copyTagWithoutId(), state.special);
        }
        if (ItemEditorCapabilities.supportsSpawnEggData(stack)) {
            this.readSpawnEggData(stack, entityData, state.special);
        }

        PotionContents potionContents = stack.get(DataComponents.POTION_CONTENTS);
        if (potionContents != null) {
            setIdFromHolder(potionContents.potion().orElse(null), id -> state.special.potionId = id);
            potionContents.customColor().ifPresent(color -> state.special.potionCustomColor = ValidationUtil.toHex(color));
            potionContents.customName().ifPresent(name -> state.special.potionCustomName = name);
            potionContents.customEffects().forEach(effect -> {
                ItemEditorState.PotionEffectDraft draft = new ItemEditorState.PotionEffectDraft();
                draft.effectId = idOrEmpty(effect.getEffect());
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
                draft.effectId = idOrEmpty(effect.effect());
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
        setEnumName(bannerBaseColor, name -> state.special.bannerBaseColor = name);

        DyedItemColor dyedItemColor = stack.get(DataComponents.DYED_COLOR);
        if (dyedItemColor != null) {
            state.special.dyedColor = ValidationUtil.toHex(dyedItemColor.rgb());
        }

        ArmorTrim trim = stack.get(DataComponents.TRIM);
        if (trim != null) {
            setIdFromHolder(trim.material(), id -> state.special.trimMaterialId = id);
            setIdFromHolder(trim.pattern(), id -> state.special.trimPatternId = id);
        }

        ResolvableProfile profile = stack.get(DataComponents.PROFILE);
        if (profile != null) {
            if (profile.partialProfile().name() != null) {
                state.special.profileName = profile.partialProfile().name();
            }
            if (profile.partialProfile().id() != null) {
                state.special.profileUuid = profile.partialProfile().id().toString();
            }
            profile.partialProfile().properties().get("textures").stream().findFirst().ifPresent(property -> {
                state.special.profileTextureValue = property.value();
                state.special.profileTextureSignature = property.signature() == null ? "" : property.signature();
            });
        }

        Axolotl.Variant axolotlVariant = stack.get(DataComponents.AXOLOTL_VARIANT);
        setSerializedName(axolotlVariant, name -> state.special.bucketAxolotlVariant = name);

        Salmon.Variant salmonVariant = stack.get(DataComponents.SALMON_SIZE);
        setSerializedName(salmonVariant, name -> state.special.bucketSalmonSize = name);

        TropicalFish.Pattern tropicalPattern = stack.get(DataComponents.TROPICAL_FISH_PATTERN);
        setSerializedName(tropicalPattern, name -> state.special.bucketTropicalPattern = name);

        DyeColor tropicalBaseColor = stack.get(DataComponents.TROPICAL_FISH_BASE_COLOR);
        setEnumName(tropicalBaseColor, name -> state.special.bucketTropicalBaseColor = name);

        DyeColor tropicalPatternColor = stack.get(DataComponents.TROPICAL_FISH_PATTERN_COLOR);
        setEnumName(tropicalPatternColor, name -> state.special.bucketTropicalPatternColor = name);

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
            setIdFromEitherHolder(instrument.instrument(), id -> state.special.instrumentId = id);
        }

        JukeboxPlayable jukeboxPlayable = stack.get(DataComponents.JUKEBOX_PLAYABLE);
        if (jukeboxPlayable != null) {
            setIdFromEitherHolder(jukeboxPlayable.song(), id -> state.special.jukeboxSongId = id);
        }

        MapItemColor mapColor = stack.get(DataComponents.MAP_COLOR);
        if (mapColor != null) {
            state.special.mapColor = ValidationUtil.toHex(mapColor.rgb());
        }

        MapPostProcessing mapPostProcessing = stack.get(DataComponents.MAP_POST_PROCESSING);
        setEnumName(mapPostProcessing, name -> state.special.mapPostProcessing = name);

        MapId mapId = stack.get(DataComponents.MAP_ID);
        if (mapId != null) {
            state.special.mapId = Integer.toString(mapId.id());
        }

        MapDecorations mapDecorations = stack.get(DataComponents.MAP_DECORATIONS);
        if (mapDecorations != null) {
            mapDecorations.decorations().forEach((key, entry) -> {
                ItemEditorState.MapDecorationDraft draft = new ItemEditorState.MapDecorationDraft();
                draft.key = key;
                setIdFromHolder(entry.type(), id -> draft.typeId = id);
                draft.x = trimTrailingZeros(entry.x());
                draft.z = trimTrailingZeros(entry.z());
                draft.rotation = trimTrailingZeros(entry.rotation());
                state.special.mapDecorations.add(draft);
            });
        }

        LodestoneTracker lodestoneTracker = stack.get(DataComponents.LODESTONE_TRACKER);
        if (lodestoneTracker != null) {
            state.special.lodestoneEnabled = true;
            state.special.lodestoneTracked = lodestoneTracker.tracked();
            lodestoneTracker.target().ifPresent(target -> {
                state.special.lodestoneDimensionId = target.dimension().identifier().toString();
                state.special.lodestoneX = Integer.toString(target.pos().getX());
                state.special.lodestoneY = Integer.toString(target.pos().getY());
                state.special.lodestoneZ = Integer.toString(target.pos().getZ());
            });
        }

        return state;
    }

    private List<String> extractBlockIds(AdventureModePredicate predicate, RegistryAccess registryAccess) {
        String encoded = this.encodePredicate(predicate, registryAccess);
        Matcher matcher = IDENTIFIER_PATTERN.matcher(encoded);
        LinkedHashSet<String> blockIds = new LinkedHashSet<>();
        while (matcher.find()) {
            String candidate = matcher.group(1);
            Identifier identifier = Identifier.tryParse(candidate);
            if (identifier == null) {
                continue;
            }
            if (BuiltInRegistries.BLOCK.containsKey(identifier)) {
                blockIds.add(identifier.toString());
            }
        }
        return blockIds.stream().toList();
    }

    private String pageMarkup(Filterable<Component> page) {
        String rawMarkup = TextComponentUtil.toMarkup(page.raw());
        return page.filtered()
                .map(TextComponentUtil::toMarkup)
                .filter(filteredMarkup -> isRicherBookMarkup(filteredMarkup, rawMarkup))
                .orElse(rawMarkup);
    }

    private static boolean isRicherBookMarkup(String candidate, String fallback) {
        if (candidate == null || candidate.isBlank()) {
            return false;
        }
        if (fallback == null || fallback.isBlank()) {
            return true;
        }

        int candidateScore = bookMarkupScore(candidate);
        int fallbackScore = bookMarkupScore(fallback);
        if (candidateScore != fallbackScore) {
            return candidateScore > fallbackScore;
        }
        return candidate.length() > fallback.length();
    }

    private static int bookMarkupScore(String markup) {
        int score = 0;
        score += tokenCount(markup, "[ie:click:") * 5;
        score += tokenCount(markup, "[ie:hover:") * 5;
        score += tokenCount(markup, "[ie:head:") * 3;
        score += tokenCount(markup, "[ie:head_texture:") * 3;
        score += tokenCount(markup, "[ie:sprite:") * 3;
        score += tokenCount(markup, "&#");
        return score;
    }

    private static int tokenCount(String text, String token) {
        int count = 0;
        int from = 0;
        while (from >= 0 && from < text.length()) {
            int index = text.indexOf(token, from);
            if (index < 0) {
                break;
            }
            count++;
            from = index + token.length();
        }
        return count;
    }

    private String encodePredicate(AdventureModePredicate predicate, RegistryAccess registryAccess) {
        try {
            var ops = registryAccess.createSerializationContext(NbtOps.INSTANCE);
            return AdventureModePredicate.CODEC.encodeStart(ops, predicate)
                    .result()
                    .map(Tag::toString)
                    .orElseGet(predicate::toString);
        } catch (RuntimeException ignored) {
            return predicate.toString();
        }
    }

    private void mapLockCode(LockCode lockCode, ItemEditorState.SpecialData special, RegistryAccess registryAccess) {
        ItemPredicate predicate = lockCode.predicate();
        if (this.isSimpleItemLockPredicate(predicate)) {
            predicate.items()
                    .flatMap(items -> items.stream().findFirst())
                    .ifPresent(holder -> setIdFromHolder(holder, id -> special.lockItemId = id));
            return;
        }

        special.lockPredicateSnbt = this.encodeItemPredicate(predicate, registryAccess);
    }

    private boolean isSimpleItemLockPredicate(ItemPredicate predicate) {
        if (predicate == null) {
            return false;
        }
        if (!MinMaxBounds.Ints.ANY.equals(predicate.count())) {
            return false;
        }
        if (!DataComponentMatchers.ANY.equals(predicate.components())) {
            return false;
        }
        return predicate.items()
                .stream()
                .flatMap(HolderSet::stream)
                .limit(2)
                .count() == 1;
    }

    private String encodeItemPredicate(ItemPredicate predicate, RegistryAccess registryAccess) {
        try {
            var ops = registryAccess.createSerializationContext(NbtOps.INSTANCE);
            return ItemPredicate.CODEC.encodeStart(ops, predicate)
                    .result()
                    .map(Tag::toString)
                    .orElse("");
        } catch (RuntimeException ignored) {
            return "";
        }
    }

    private static int parseSlotOrMinusOne(String rawSlot) {
        try {
            return Integer.parseInt(rawSlot.trim());
        } catch (NumberFormatException ignored) {
            return -1;
        }
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
        Style style = line.getStyle();
        TextColor color = style.getColor();
        if (color != null && color.getValue() == baseColor) {
            style = style.withColor((TextColor) null);
        }

        MutableComponent rebuilt = MutableComponent.create(line.getContents()).withStyle(style);
        for (Component sibling : line.getSiblings()) {
            rebuilt.append(this.stripBaseSignColor(sibling, baseColor));
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

    private void readArmorStandData(net.minecraft.nbt.CompoundTag entityTag, ItemEditorState.SpecialData special) {
        special.armorStandSmall = entityTag.getBooleanOr("Small", false);
        special.armorStandShowArms = entityTag.getBooleanOr("ShowArms", false);
        special.armorStandNoBasePlate = entityTag.getBooleanOr("NoBasePlate", false);
        special.armorStandInvisible = entityTag.getBooleanOr("Invisible", false);
        special.armorStandNoGravity = entityTag.getBooleanOr("NoGravity", false);
        special.armorStandInvulnerable = entityTag.getBooleanOr("Invulnerable", false);
        special.armorStandCustomNameVisible = entityTag.getBooleanOr("CustomNameVisible", false);
        special.armorStandMarker = entityTag.getBooleanOr("Marker", false);
        entityTag.read("CustomName", ComponentSerialization.CODEC)
                .ifPresent(component -> special.armorStandCustomName = TextComponentUtil.toMarkup(component));
        special.armorStandDisabledSlots = readOptionalInt(entityTag, "DisabledSlots");

        net.minecraft.nbt.CompoundTag poseTag = entityTag.getCompoundOrEmpty("Pose");
        this.readPosePart(
                poseTag,
                "Head",
                special.armorStandPose.head,
                0.0F,
                0.0F
        );
        this.readPosePart(
                poseTag,
                "Body",
                special.armorStandPose.body,
                0.0F,
                0.0F
        );
        this.readPosePart(
                poseTag,
                "LeftArm",
                special.armorStandPose.leftArm,
                -10.0F,
                -10.0F
        );
        this.readPosePart(
                poseTag,
                "RightArm",
                special.armorStandPose.rightArm,
                -15.0F,
                10.0F
        );
        this.readPosePart(
                poseTag,
                "LeftLeg",
                special.armorStandPose.leftLeg,
                -1.0F,
                -1.0F
        );
        this.readPosePart(
                poseTag,
                "RightLeg",
                special.armorStandPose.rightLeg,
                1.0F,
                1.0F
        );

        ListTag attributes = entityTag.getListOrEmpty("attributes");
        for (int index = 0; index < attributes.size(); index++) {
            net.minecraft.nbt.CompoundTag attributeTag = attributes.getCompoundOrEmpty(index);
            String id = attributeTag.getStringOr("id", "");
            if (!"minecraft:scale".equals(id)) {
                continue;
            }
            double scale = attributeTag.getDoubleOr("base", 1.0D);
            special.armorStandScale = trimTrailingZeros(scale);
            break;
        }
    }

    private void readItemFrameData(net.minecraft.nbt.CompoundTag entityTag, ItemEditorState.SpecialData special) {
        special.itemFrameInvisible = entityTag.getBooleanOr("Invisible", false);
        special.itemFrameFixed = entityTag.getBooleanOr("Fixed", false);
        special.itemFrameNoGravity = entityTag.getBooleanOr("NoGravity", false);
        special.itemFrameInvulnerable = entityTag.getBooleanOr("Invulnerable", false);
        special.itemFrameCustomNameVisible = entityTag.getBooleanOr("CustomNameVisible", false);
        entityTag.read("CustomName", ComponentSerialization.CODEC)
                .ifPresent(component -> special.itemFrameCustomName = TextComponentUtil.toMarkup(component));
        special.itemFrameRotation = readOptionalInt(entityTag, "ItemRotation");
        special.itemFrameFacing = readOptionalInt(entityTag, "Facing");
        entityTag.getFloat("ItemDropChance")
                .ifPresent(value -> special.itemFrameDropChance = trimTrailingZeros(value));
    }

    private void readSpawnEggData(
            ItemStack stack,
            TypedEntityData<EntityType<?>> entityData,
            ItemEditorState.SpecialData special
    ) {
        if (entityData != null) {
            var key = BuiltInRegistries.ENTITY_TYPE.getKey(entityData.type());
            special.spawnEggEntityId = identifierOrEmpty(key);
        } else if (stack.getItem() instanceof SpawnEggItem spawnEggItem) {
            EntityType<?> spawnEggType = spawnEggItem.getType(stack);
            if (spawnEggType != null) {
                var key = BuiltInRegistries.ENTITY_TYPE.getKey(spawnEggType);
                special.spawnEggEntityId = identifierOrEmpty(key);
            }
        }

        if (entityData == null) {
            return;
        }

        net.minecraft.nbt.CompoundTag entityTag = entityData.copyTagWithoutId();
        special.spawnEggNoAi = entityTag.getBooleanOr("NoAI", false);
        special.spawnEggSilent = entityTag.getBooleanOr("Silent", false);
        special.spawnEggNoGravity = entityTag.getBooleanOr("NoGravity", false);
        special.spawnEggGlowing = entityTag.getBooleanOr("Glowing", false);
        special.spawnEggInvulnerable = entityTag.getBooleanOr("Invulnerable", false);
        special.spawnEggPersistenceRequired = entityTag.getBooleanOr("PersistenceRequired", false);
        special.spawnEggCustomNameVisible = entityTag.getBooleanOr("CustomNameVisible", false);
        entityTag.read("CustomName", ComponentSerialization.CODEC)
                .ifPresent(component -> special.spawnEggCustomName = TextComponentUtil.toMarkup(component));
        entityTag.getFloat("Health")
                .ifPresent(value -> special.spawnEggHealth = trimTrailingZeros(value));
        this.readVillagerDataAndTrades(entityTag, special);
    }

    private void readVillagerDataAndTrades(net.minecraft.nbt.CompoundTag entityTag, ItemEditorState.SpecialData special) {
        net.minecraft.nbt.CompoundTag villagerData = entityTag.getCompoundOrEmpty("VillagerData");
        special.spawnEggVillagerTypeId = villagerData.getStringOr("type", "");
        special.spawnEggVillagerProfessionId = villagerData.getStringOr("profession", "");
        special.spawnEggVillagerLevel = readOptionalInt(villagerData, "level");

        special.spawnEggVillagerTrades.clear();
        net.minecraft.nbt.CompoundTag offersTag = entityTag.getCompoundOrEmpty("Offers");
        ListTag recipesTag = offersTag.getListOrEmpty("Recipes");
        for (int index = 0; index < recipesTag.size(); index++) {
            net.minecraft.nbt.CompoundTag recipeTag = recipesTag.getCompoundOrEmpty(index);
            ItemEditorState.VillagerTradeDraft draft = new ItemEditorState.VillagerTradeDraft();
            this.readTradeStack(recipeTag, "buy", draft.buy);
            this.readTradeStack(recipeTag, "buyB", draft.buyB);
            this.readTradeStack(recipeTag, "sell", draft.sell);
            draft.maxUses = readOptionalInt(recipeTag, "maxUses");
            draft.uses = readOptionalInt(recipeTag, "uses");
            draft.villagerXp = readOptionalInt(recipeTag, "xp");
            recipeTag.getFloat("priceMultiplier")
                    .ifPresent(value -> draft.priceMultiplier = trimTrailingZeros(value));
            draft.demand = readOptionalInt(recipeTag, "demand");
            draft.specialPrice = readOptionalInt(recipeTag, "specialPrice");
            draft.rewardExp = recipeTag.getBooleanOr("rewardExp", true);
            special.spawnEggVillagerTrades.add(draft);
        }
    }

    private void readTradeStack(
            net.minecraft.nbt.CompoundTag recipeTag,
            String key,
            ItemEditorState.TradeStackDraft stackDraft
    ) {
        net.minecraft.nbt.CompoundTag stackTag = recipeTag.getCompoundOrEmpty(key);
        stackDraft.itemId = stackTag.getStringOr("id", "");
        stackTag.getInt("count").ifPresent(value -> stackDraft.count = Integer.toString(value));
        stackTag.getByte("count").ifPresent(value -> stackDraft.count = Integer.toString(value));
    }

    private void readConsumableEffects(List<ConsumeEffect> effects, ItemEditorState.SpecialData special) {
        for (ConsumeEffect consumeEffect : effects) {
            if (consumeEffect instanceof ApplyStatusEffectsConsumeEffect(var effectInstances, var probability)) {
                ItemEditorState.ConsumableEffectDraft draft = new ItemEditorState.ConsumableEffectDraft();
                draft.type = ItemEditorState.ConsumableEffectDraft.TYPE_APPLY_EFFECTS;
                draft.probability = trimTrailingZeros(probability);
                for (MobEffectInstance effectInstance : effectInstances) {
                    ItemEditorState.PotionEffectDraft effectDraft = new ItemEditorState.PotionEffectDraft();
                    effectDraft.effectId = effectInstance.getEffect().unwrapKey().map(key -> key.identifier().toString()).orElse("");
                    effectDraft.duration = Integer.toString(effectInstance.getDuration());
                    effectDraft.amplifier = Integer.toString(effectInstance.getAmplifier());
                    effectDraft.ambient = effectInstance.isAmbient();
                    effectDraft.visible = effectInstance.isVisible();
                    effectDraft.showIcon = effectInstance.showIcon();
                    draft.effects.add(effectDraft);
                }
                special.consumableOnConsumeEffects.add(draft);
                continue;
            }

            if (consumeEffect instanceof PlaySoundConsumeEffect(var sound)) {
                ItemEditorState.ConsumableEffectDraft draft = new ItemEditorState.ConsumableEffectDraft();
                draft.type = ItemEditorState.ConsumableEffectDraft.TYPE_PLAY_SOUND;
                setIdFromHolder(sound, id -> draft.soundId = id);
                special.consumableOnConsumeEffects.add(draft);
            }
        }
    }

    private void readPosePart(
            net.minecraft.nbt.CompoundTag poseTag,
            String key,
            ItemEditorState.RotationDraft rotation,
            float defaultX,
            float defaultZ
    ) {
        ListTag values = poseTag.getListOrEmpty(key);
        rotation.x = trimTrailingZeros(values.getFloatOr(0, defaultX));
        rotation.y = trimTrailingZeros(values.getFloatOr(1, 0.0F));
        rotation.z = trimTrailingZeros(values.getFloatOr(2, defaultZ));
    }

    private static String readOptionalInt(net.minecraft.nbt.CompoundTag tag, String key) {
        return tag.getInt(key).map(String::valueOf).orElse("");
    }

    private static void setIdFromHolder(Holder<?> holder, Consumer<String> setter) {
        String id = idOrEmpty(holder);
        if (!id.isEmpty()) {
            setter.accept(id);
        }
    }

    private static void setIdFromEitherHolder(EitherHolder<?> holder, Consumer<String> setter) {
        if (holder == null) {
            return;
        }
        String id = holder.key().map(key -> key.identifier().toString()).orElse("");
        if (!id.isEmpty()) {
            setter.accept(id);
        }
    }

    private static String idOrEmpty(Holder<?> holder) {
        if (holder == null) {
            return "";
        }
        return holder.unwrapKey().map(key -> key.identifier().toString()).orElse("");
    }

    private static void setItemId(Item item, Consumer<String> setter) {
        if (item == null) {
            return;
        }
        Identifier id = BuiltInRegistries.ITEM.getKey(item);
        setter.accept(identifierOrEmpty(id));
    }

    private static String identifierOrEmpty(@Nullable Identifier identifier) {
        return identifier == null ? "" : identifier.toString();
    }

    private static void setSerializedName(StringRepresentable value, Consumer<String> setter) {
        if (value != null) {
            setter.accept(value.getSerializedName());
        }
    }

    private static <E extends Enum<E>> void setEnumName(E value, Consumer<String> setter) {
        if (value != null) {
            setter.accept(value.name());
        }
    }

    private static <T> String joinHolderSetIds(HolderSet<T> holderSet) {
        return String.join(
                ", ",
                holderSet.stream()
                        .map(Holder::unwrapKey)
                        .flatMap(Optional::stream)
                        .map(key -> key.identifier().toString())
                        .toList()
        );
    }

    private static String trimTrailingZeros(double value) {
        return ValidationUtil.trimTrailingZeros(value);
    }
}
