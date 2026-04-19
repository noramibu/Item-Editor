package me.noramibu.itemeditor.service;

import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.editor.ValidationMessage;
import me.noramibu.itemeditor.util.IdFieldNormalizer;
import me.noramibu.itemeditor.util.ItemEditorText;
import me.noramibu.itemeditor.util.TextComponentUtil;
import me.noramibu.itemeditor.util.ValidationUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.component.TypedEntityData;

import java.util.List;
import java.util.Objects;

final class SpawnEggSpecialDataApplier extends AbstractPreviewApplierSupport implements SpecialDataApplier {

    @Override
    public void apply(SpecialDataApplyContext context) {
        if (!this.supportsSpawnEggData(context)) {
            return;
        }

        if (this.sameSpawnEggData(context.special(), context.baselineSpecial())) {
            this.restoreOriginalComponent(context.originalStack(), context.previewStack(), DataComponents.ENTITY_DATA);
            return;
        }

        if (this.isSpawnEggDataDefault(context.special())) {
            this.clearToPrototype(context.previewStack(), DataComponents.ENTITY_DATA);
            return;
        }

        CompoundTag entityTag = new CompoundTag();
        TypedEntityData<EntityType<?>> originalData = context.originalStack().get(DataComponents.ENTITY_DATA);
        if (originalData != null) {
            entityTag = originalData.copyTagWithoutId();
        }

        setBooleanKey(entityTag, "NoAI", context.special().spawnEggNoAi);
        setBooleanKey(entityTag, "Silent", context.special().spawnEggSilent);
        setBooleanKey(entityTag, "NoGravity", context.special().spawnEggNoGravity);
        setBooleanKey(entityTag, "Glowing", context.special().spawnEggGlowing);
        setBooleanKey(entityTag, "Invulnerable", context.special().spawnEggInvulnerable);
        setBooleanKey(entityTag, "PersistenceRequired", context.special().spawnEggPersistenceRequired);
        setBooleanKey(entityTag, "CustomNameVisible", context.special().spawnEggCustomNameVisible);
        this.applyCustomName(entityTag, context.special().spawnEggCustomName);
        if (!this.applyHealth(entityTag, context)) {
            return;
        }

        EntityType<?> entityType = this.resolveEntityType(context, context.special().spawnEggEntityId);
        if (entityType == null) {
            context.messages().add(ValidationMessage.error(ItemEditorText.str(
                    "preview.validation.component_failed",
                    ItemEditorText.str("special.spawn_egg.entity")
            )));
            return;
        }

        if (!this.applyVillagerDataAndTrades(entityTag, entityType, context)) {
            return;
        }

        context.previewStack().set(DataComponents.ENTITY_DATA, TypedEntityData.of(entityType, entityTag));
    }

    private boolean supportsSpawnEggData(SpecialDataApplyContext context) {
        return context.previewStack().getItem() instanceof SpawnEggItem
                || context.originalStack().getItem() instanceof SpawnEggItem;
    }

    private EntityType<?> resolveEntityType(SpecialDataApplyContext context, String rawId) {
        String normalized = rawId == null ? "" : rawId.trim();
        if (!normalized.isBlank()) {
            Identifier identifier = IdFieldNormalizer.parse(normalized);
            if (identifier == null) {
                return null;
            }
            return BuiltInRegistries.ENTITY_TYPE.getOptional(identifier).orElse(null);
        }

        if (context.previewStack().getItem() instanceof SpawnEggItem) {
            return SpawnEggItem.getType(context.previewStack());
        }
        if (context.originalStack().getItem() instanceof SpawnEggItem) {
            return SpawnEggItem.getType(context.originalStack());
        }
        TypedEntityData<EntityType<?>> previewData = context.previewStack().get(DataComponents.ENTITY_DATA);
        if (previewData != null) {
            return previewData.type();
        }
        TypedEntityData<EntityType<?>> originalData = context.originalStack().get(DataComponents.ENTITY_DATA);
        return originalData == null ? null : originalData.type();
    }

    private void applyCustomName(CompoundTag entityTag, String rawName) {
        if (rawName == null || rawName.isBlank()) {
            entityTag.remove("CustomName");
            return;
        }
        entityTag.store(
                "CustomName",
                ComponentSerialization.CODEC,
                this.withPlainBaseline(TextComponentUtil.parseMarkup(rawName))
        );
    }

    private boolean applyHealth(CompoundTag entityTag, SpecialDataApplyContext context) {
        String raw = context.special().spawnEggHealth == null ? "" : context.special().spawnEggHealth.trim();
        if (raw.isBlank()) {
            entityTag.remove("Health");
            return true;
        }

        Float health = ValidationUtil.parseFloat(
                raw,
                ItemEditorText.str("special.spawn_egg.health"),
                context.messages()
        );
        if (health == null) {
            return false;
        }
        if (health < 0.0F) {
            context.messages().add(ValidationMessage.error(ItemEditorText.str(
                    "validation.range",
                    ItemEditorText.str("special.spawn_egg.health"),
                    0,
                    2048
            )));
            return false;
        }
        entityTag.putFloat("Health", health);
        return true;
    }

    private boolean applyVillagerDataAndTrades(
            CompoundTag entityTag,
            EntityType<?> entityType,
            SpecialDataApplyContext context
    ) {
        if (!isVillagerEntity(entityType)) {
            entityTag.remove("VillagerData");
            entityTag.remove("Offers");
            return true;
        }

        if (!this.applyVillagerData(entityTag, context)) {
            return false;
        }
        if (!this.applyVillagerTrades(entityTag, context)) {
            return false;
        }
        return this.ensureVillagerTradeCompatibility(entityTag, entityType, context);
    }

    private boolean ensureVillagerTradeCompatibility(
            CompoundTag entityTag,
            EntityType<?> entityType,
            SpecialDataApplyContext context
    ) {
        ItemEditorState.SpecialData special = context.special();
        if (entityType != EntityType.VILLAGER || special.spawnEggVillagerTrades.isEmpty()) {
            return true;
        }

        CompoundTag villagerDataTag = entityTag.getCompoundOrEmpty("VillagerData");
        String professionId = villagerDataTag.getStringOr("profession", "");
        if (professionId.isBlank()) {
            context.messages().add(ValidationMessage.error(ItemEditorText.str(
                    "preview.validation.component_failed",
                    ItemEditorText.str("special.spawn_egg.villager.profession")
            )));
            return false;
        }
        Identifier profession = IdFieldNormalizer.parse(professionId);
        if (profession == null || !BuiltInRegistries.VILLAGER_PROFESSION.containsKey(profession)) {
            context.messages().add(ValidationMessage.error(ItemEditorText.str(
                    "validation.registry_missing",
                    ItemEditorText.str("special.spawn_egg.villager.profession"),
                    professionId
            )));
            return false;
        }

        if (villagerDataTag.getInt("level").orElse(0) <= 0) {
            villagerDataTag.putInt("level", 1);
        }
        if (villagerDataTag.getStringOr("type", "").isBlank()) {
            villagerDataTag.putString("type", "minecraft:plains");
        }
        entityTag.put("VillagerData", villagerDataTag);
        return true;
    }

    private boolean applyVillagerData(CompoundTag entityTag, SpecialDataApplyContext context) {
        ItemEditorState.SpecialData special = context.special();
        String typeId = normalizeId(special.spawnEggVillagerTypeId);
        String professionId = normalizeId(special.spawnEggVillagerProfessionId);
        String rawLevel = special.spawnEggVillagerLevel == null ? "" : special.spawnEggVillagerLevel.trim();

        if (typeId.isBlank() && professionId.isBlank() && rawLevel.isBlank()) {
            entityTag.remove("VillagerData");
            return true;
        }

        CompoundTag villagerDataTag = new CompoundTag();

        if (!typeId.isBlank()) {
            Identifier villagerType = IdFieldNormalizer.parse(typeId);
            if (villagerType == null || !BuiltInRegistries.VILLAGER_TYPE.containsKey(villagerType)) {
                context.messages().add(ValidationMessage.error(ItemEditorText.str(
                        "validation.registry_missing",
                        ItemEditorText.str("special.spawn_egg.villager.type"),
                        typeId
                )));
                return false;
            }
            villagerDataTag.putString("type", villagerType.toString());
        }

        if (!professionId.isBlank()) {
            Identifier profession = IdFieldNormalizer.parse(professionId);
            if (profession == null || !BuiltInRegistries.VILLAGER_PROFESSION.containsKey(profession)) {
                context.messages().add(ValidationMessage.error(ItemEditorText.str(
                        "validation.registry_missing",
                        ItemEditorText.str("special.spawn_egg.villager.profession"),
                        professionId
                )));
                return false;
            }
            villagerDataTag.putString("profession", profession.toString());
        }

        if (!rawLevel.isBlank()) {
            Integer level = ValidationUtil.parseInt(
                    rawLevel,
                    ItemEditorText.str("special.spawn_egg.villager.level"),
                    1,
                    5,
                    context.messages()
            );
            if (level == null) {
                return false;
            }
            villagerDataTag.putInt("level", level);
        }

        if (villagerDataTag.isEmpty()) {
            entityTag.remove("VillagerData");
        } else {
            entityTag.put("VillagerData", villagerDataTag);
        }
        return true;
    }

    private boolean applyVillagerTrades(CompoundTag entityTag, SpecialDataApplyContext context) {
        List<ItemEditorState.VillagerTradeDraft> trades = context.special().spawnEggVillagerTrades;
        if (trades.isEmpty()) {
            entityTag.remove("Offers");
            return true;
        }

        ListTag recipesTag = new ListTag();
        for (int index = 0; index < trades.size(); index++) {
            ItemEditorState.VillagerTradeDraft draft = trades.get(index);
            CompoundTag recipeTag = new CompoundTag();

            CompoundTag buyTag = this.buildTradeStackTag(
                    draft.buy,
                    ItemEditorText.str("special.spawn_egg.villager.trade.buy"),
                    index,
                    context
            );
            if (buyTag == null) {
                return false;
            }
            recipeTag.put("buy", buyTag);

            if (!draft.buyB.itemId.isBlank()) {
                CompoundTag buyBTag = this.buildTradeStackTag(
                        draft.buyB,
                        ItemEditorText.str("special.spawn_egg.villager.trade.buy_b"),
                        index,
                        context
                );
                if (buyBTag == null) {
                    return false;
                }
                recipeTag.put("buyB", buyBTag);
            }

            CompoundTag sellTag = this.buildTradeStackTag(
                    draft.sell,
                    ItemEditorText.str("special.spawn_egg.villager.trade.sell"),
                    index,
                    context
            );
            if (sellTag == null) {
                return false;
            }
            recipeTag.put("sell", sellTag);

            Integer maxUses = parseOptionalIntOrDefault(
                    draft.maxUses,
                    ItemEditorText.str("special.spawn_egg.villager.trade.max_uses"),
                    1,
                    16,
                    context.messages()
            );
            Integer uses = parseOptionalIntOrDefault(
                    draft.uses,
                    ItemEditorText.str("special.spawn_egg.villager.trade.uses"),
                    0,
                    0,
                    context.messages()
            );
            Integer villagerXp = parseOptionalIntOrDefault(
                    draft.villagerXp,
                    ItemEditorText.str("special.spawn_egg.villager.trade.xp"),
                    0,
                    1,
                    context.messages()
            );
            Integer demand = parseOptionalIntOrDefault(
                    draft.demand,
                    ItemEditorText.str("special.spawn_egg.villager.trade.demand"),
                    Integer.MIN_VALUE,
                    0,
                    context.messages()
            );
            Integer specialPrice = parseOptionalIntOrDefault(
                    draft.specialPrice,
                    ItemEditorText.str("special.spawn_egg.villager.trade.special_price"),
                    Integer.MIN_VALUE,
                    0,
                    context.messages()
            );
            Float priceMultiplier = parseOptionalFloatOrDefault(
                    draft.priceMultiplier,
                    ItemEditorText.str("special.spawn_egg.villager.trade.price_multiplier"),
                    context.messages()
            );

            if (maxUses == null
                    || uses == null
                    || villagerXp == null
                    || demand == null
                    || specialPrice == null
                    || priceMultiplier == null) {
                return false;
            }

            recipeTag.putInt("maxUses", maxUses);
            recipeTag.putInt("uses", uses);
            recipeTag.putInt("xp", villagerXp);
            recipeTag.putFloat("priceMultiplier", priceMultiplier);
            recipeTag.putInt("demand", demand);
            recipeTag.putInt("specialPrice", specialPrice);
            recipeTag.putBoolean("rewardExp", draft.rewardExp);

            recipesTag.add(recipeTag);
        }

        CompoundTag offersTag = new CompoundTag();
        offersTag.put("Recipes", recipesTag);
        entityTag.put("Offers", offersTag);
        return true;
    }

    private CompoundTag buildTradeStackTag(
            ItemEditorState.TradeStackDraft stackDraft,
            String label,
            int tradeIndex,
            SpecialDataApplyContext context
    ) {
        String itemId = normalizeId(stackDraft.itemId);
        if (itemId.isBlank()) {
            context.messages().add(ValidationMessage.error(ItemEditorText.str(
                    "preview.validation.component_failed",
                    ItemEditorText.str("special.spawn_egg.villager.trade")
                            + " " + (tradeIndex + 1) + " - " + label
            )));
            return null;
        }

        Identifier identifier = IdFieldNormalizer.parse(itemId);
        if (identifier == null) {
            context.messages().add(ValidationMessage.error(ItemEditorText.str(
                    "preview.validation.component_failed",
                    ItemEditorText.str("special.spawn_egg.villager.trade")
                            + " " + (tradeIndex + 1) + " - " + label
            )));
            return null;
        }

        Item item = BuiltInRegistries.ITEM.getOptional(identifier).orElse(null);
        if (item == null) {
            context.messages().add(ValidationMessage.error(ItemEditorText.str(
                    "validation.registry_missing",
                    label,
                    itemId
            )));
            return null;
        }

        Integer count = ValidationUtil.parseInt(
                stackDraft.count,
                label + " " + ItemEditorText.str("special.spawn_egg.villager.trade.count_suffix"),
                1,
                127,
                context.messages()
        );
        if (count == null) {
            return null;
        }

        CompoundTag stackTag = new CompoundTag();
        stackTag.putString("id", BuiltInRegistries.ITEM.getKey(item).toString());
        stackTag.putInt("count", count);
        return stackTag;
    }

    private static Integer parseOptionalIntOrDefault(
            String raw,
            String field,
            int min,
            int fallback,
            List<ValidationMessage> messages
    ) {
        String normalized = raw == null ? "" : raw.trim();
        if (normalized.isBlank()) {
            return fallback;
        }
        return ValidationUtil.parseInt(normalized, field, min, Integer.MAX_VALUE, messages);
    }

    private static Float parseOptionalFloatOrDefault(
            String raw,
            String field,
            List<ValidationMessage> messages
    ) {
        String normalized = raw == null ? "" : raw.trim();
        if (normalized.isBlank()) {
            return 0.05F;
        }
        return ValidationUtil.parseFloat(normalized, field, messages);
    }

    private static String normalizeId(String raw) {
        return raw == null ? "" : IdFieldNormalizer.normalize(raw);
    }

    private static boolean isVillagerEntity(EntityType<?> entityType) {
        return entityType == EntityType.VILLAGER || entityType == EntityType.WANDERING_TRADER;
    }

    private boolean sameSpawnEggData(ItemEditorState.SpecialData current, ItemEditorState.SpecialData baseline) {
        return Objects.equals(current.spawnEggEntityId, baseline.spawnEggEntityId)
                && current.spawnEggNoAi == baseline.spawnEggNoAi
                && current.spawnEggSilent == baseline.spawnEggSilent
                && current.spawnEggNoGravity == baseline.spawnEggNoGravity
                && current.spawnEggGlowing == baseline.spawnEggGlowing
                && current.spawnEggInvulnerable == baseline.spawnEggInvulnerable
                && current.spawnEggPersistenceRequired == baseline.spawnEggPersistenceRequired
                && current.spawnEggCustomNameVisible == baseline.spawnEggCustomNameVisible
                && Objects.equals(current.spawnEggCustomName, baseline.spawnEggCustomName)
                && Objects.equals(current.spawnEggHealth, baseline.spawnEggHealth)
                && Objects.equals(current.spawnEggVillagerTypeId, baseline.spawnEggVillagerTypeId)
                && Objects.equals(current.spawnEggVillagerProfessionId, baseline.spawnEggVillagerProfessionId)
                && Objects.equals(current.spawnEggVillagerLevel, baseline.spawnEggVillagerLevel)
                && this.sameVillagerTrades(current.spawnEggVillagerTrades, baseline.spawnEggVillagerTrades);
    }

    private boolean isSpawnEggDataDefault(ItemEditorState.SpecialData special) {
        return special.spawnEggEntityId.isBlank()
                && !special.spawnEggNoAi
                && !special.spawnEggSilent
                && !special.spawnEggNoGravity
                && !special.spawnEggGlowing
                && !special.spawnEggInvulnerable
                && !special.spawnEggPersistenceRequired
                && !special.spawnEggCustomNameVisible
                && special.spawnEggCustomName.isBlank()
                && special.spawnEggHealth.isBlank()
                && special.spawnEggVillagerTypeId.isBlank()
                && special.spawnEggVillagerProfessionId.isBlank()
                && special.spawnEggVillagerLevel.isBlank()
                && special.spawnEggVillagerTrades.isEmpty();
    }

    private boolean sameVillagerTrades(
            List<ItemEditorState.VillagerTradeDraft> current,
            List<ItemEditorState.VillagerTradeDraft> baseline
    ) {
        if (current.size() != baseline.size()) {
            return false;
        }
        for (int index = 0; index < current.size(); index++) {
            ItemEditorState.VillagerTradeDraft left = current.get(index);
            ItemEditorState.VillagerTradeDraft right = baseline.get(index);
            if (tradeStackDiffers(left.buy, right.buy)
                    || tradeStackDiffers(left.buyB, right.buyB)
                    || tradeStackDiffers(left.sell, right.sell)
                    || !Objects.equals(left.maxUses, right.maxUses)
                    || !Objects.equals(left.uses, right.uses)
                    || !Objects.equals(left.villagerXp, right.villagerXp)
                    || !Objects.equals(left.priceMultiplier, right.priceMultiplier)
                    || !Objects.equals(left.demand, right.demand)
                    || !Objects.equals(left.specialPrice, right.specialPrice)
                    || left.rewardExp != right.rewardExp) {
                return false;
            }
        }
        return true;
    }

    private static boolean tradeStackDiffers(ItemEditorState.TradeStackDraft left, ItemEditorState.TradeStackDraft right) {
        return !Objects.equals(left.itemId, right.itemId)
                || !Objects.equals(left.count, right.count);
    }

    private static void setBooleanKey(CompoundTag tag, String key, boolean value) {
        if (value) {
            tag.putBoolean(key, true);
        } else {
            tag.remove(key);
        }
    }
}
