package me.noramibu.itemeditor.service;

import com.mojang.serialization.DataResult;
import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.editor.ValidationMessage;
import me.noramibu.itemeditor.util.IdFieldNormalizer;
import me.noramibu.itemeditor.util.ItemEditorText;
import me.noramibu.itemeditor.util.TextComponentUtil;
import me.noramibu.itemeditor.util.ValidationUtil;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.DropChances;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.item.ItemStack;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class EntitySpawnDataUtil {

    private EntitySpawnDataUtil() {
    }

    static void readEntity(
            CompoundTag entityTag,
            ItemEditorState.EntitySpawnDraft draft,
            RegistryAccess registryAccess
    ) {
        draft.originalEntityTag = entityTag.copy();
        draft.entityId = entityTag.getStringOr("id", "");
        draft.noAi = entityTag.getBooleanOr("NoAI", false);
        draft.silent = entityTag.getBooleanOr("Silent", false);
        draft.noGravity = entityTag.getBooleanOr("NoGravity", false);
        draft.glowing = entityTag.getBooleanOr("Glowing", false);
        draft.invulnerable = entityTag.getBooleanOr("Invulnerable", false);
        draft.persistenceRequired = entityTag.getBooleanOr("PersistenceRequired", false);
        draft.customNameVisible = entityTag.getBooleanOr("CustomNameVisible", false);
        entityTag.read("CustomName", ComponentSerialization.CODEC)
                .ifPresent(component -> draft.customName = TextComponentUtil.toMarkup(component));
        entityTag.getFloat("Health")
                .ifPresent(value -> draft.health = ValidationUtil.trimTrailingZeros(value));
        readAttributes(entityTag, draft.attributes, Set.of());
        readEquipment(entityTag, draft.equipment, registryAccess);
        DropChances dropChances = entityTag.read("drop_chances", DropChances.CODEC)
                .orElse(DropChances.DEFAULT);
        for (EquipmentSlot slot : EquipmentSlot.VALUES) {
            String chance = ValidationUtil.trimTrailingZeros(dropChances.byEquipment(slot));
            draft.equipment.setDropChance(slot, chance);
        }
        draft.equipment.uiCollapsed = draft.equipment.isEmpty();
    }

    static CompoundTag applyEntity(
            ItemEditorState.EntitySpawnDraft draft,
            SpecialDataApplyContext context,
            String fieldLabel
    ) {
        CompoundTag entityTag = draft.originalEntityTag.copy();
        String entityIdRaw = draft.entityId == null ? "" : draft.entityId.trim();
        if (entityIdRaw.isBlank()) {
            if (!isEntityDefault(draft)) {
                context.messages().add(ValidationMessage.error(ItemEditorText.str(
                        "preview.validation.component_failed",
                        fieldLabel
                )));
            }
            return null;
        }

        Identifier entityId = IdFieldNormalizer.parse(entityIdRaw);
        if (entityId == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(entityId)) {
            context.messages().add(ValidationMessage.error(ItemEditorText.str(
                    "validation.registry_missing",
                    fieldLabel,
                    entityIdRaw
            )));
            return null;
        }

        entityTag.putString("id", entityId.toString());
        NbtTagUtil.setBooleanKey(entityTag, "NoAI", draft.noAi);
        NbtTagUtil.setBooleanKey(entityTag, "Silent", draft.silent);
        NbtTagUtil.setBooleanKey(entityTag, "NoGravity", draft.noGravity);
        NbtTagUtil.setBooleanKey(entityTag, "Glowing", draft.glowing);
        NbtTagUtil.setBooleanKey(entityTag, "Invulnerable", draft.invulnerable);
        NbtTagUtil.setBooleanKey(entityTag, "PersistenceRequired", draft.persistenceRequired);
        NbtTagUtil.setBooleanKey(entityTag, "CustomNameVisible", draft.customNameVisible);
        NbtTagUtil.setTextComponentKey(entityTag, "CustomName", draft.customName);
        if (!applyAttributes(entityTag, draft.attributes, Set.of(), context, fieldLabel)
                || !applyHealth(
                        entityTag,
                        draft.health,
                        draft.entityId,
                        0.0F,
                        context,
                        fieldLabel + " " + ItemEditorText.str("special.spawn_egg.health")
                )) {
            return null;
        }
        if (!applyEquipment(entityTag, draft.equipment, context, fieldLabel)) {
            return null;
        }
        return applyDropChances(entityTag, draft.equipment, context, fieldLabel) ? entityTag : null;
    }

    static void readEquipment(
            CompoundTag entityTag,
            ItemEditorState.EntityEquipmentDraft draft,
            RegistryAccess registryAccess
    ) {
        draft.reset();
        CompoundTag equipmentTag = entityTag.getCompoundOrEmpty("equipment");
        var ops = registryAccess.createSerializationContext(NbtOps.INSTANCE);
        for (EquipmentSlot slot : EquipmentSlot.VALUES) {
            CompoundTag stackTag = equipmentTag.getCompoundOrEmpty(slot.getSerializedName());
            if (stackTag.isEmpty()) {
                continue;
            }
            ItemStack.CODEC.parse(ops, stackTag).result().ifPresent(stack -> draft.load(slot, stack));
        }
        draft.uiCollapsed = draft.isEmpty();
    }

    static boolean applyEquipment(
            CompoundTag entityTag,
            ItemEditorState.EntityEquipmentDraft draft,
            SpecialDataApplyContext context,
            String fieldLabel
    ) {
        CompoundTag equipmentTag = entityTag.getCompoundOrEmpty("equipment").copy();
        var ops = context.registryAccess().createSerializationContext(NbtOps.INSTANCE);
        for (EquipmentSlot slot : EquipmentSlot.VALUES) {
            if (!draft.edited(slot)) {
                continue;
            }
            ItemStack stack = draft.stack(slot);
            if (stack.isEmpty()) {
                equipmentTag.remove(slot.getSerializedName());
                continue;
            }
            DataResult<Tag> encoded = ItemStack.CODEC.encodeStart(ops, stack.copyWithCount(1));
            CompoundTag stackTag = encoded.result()
                    .filter(CompoundTag.class::isInstance)
                    .map(CompoundTag.class::cast)
                    .orElse(null);
            if (stackTag == null) {
                context.messages().add(ValidationMessage.error(ItemEditorText.str(
                        "preview.validation.component_failed",
                        fieldLabel
                )));
                return false;
            }
            equipmentTag.put(slot.getSerializedName(), stackTag);
        }
        if (equipmentTag.isEmpty()) {
            entityTag.remove("equipment");
        } else {
            entityTag.put("equipment", equipmentTag);
        }
        return true;
    }

    static boolean sameEquipment(
            ItemEditorState.EntityEquipmentDraft current,
            ItemEditorState.EntityEquipmentDraft baseline
    ) {
        for (EquipmentSlot slot : EquipmentSlot.VALUES) {
            if (!ItemStack.matches(current.stack(slot), baseline.stack(slot))
                    || !Objects.equals(current.dropChance(slot), baseline.dropChance(slot))) {
                return false;
            }
        }
        return true;
    }

    static void readAttributes(
            CompoundTag entityTag,
            List<ItemEditorState.EntityAttributeDraft> target,
            Set<String> excludedIds
    ) {
        target.clear();
        ListTag attributes = entityTag.getListOrEmpty("attributes");
        for (int index = 0; index < attributes.size(); index++) {
            CompoundTag attributeTag = attributes.getCompoundOrEmpty(index);
            String attributeId = attributeTag.getStringOr("id", "");
            Double baseValue = attributeTag.getDouble("base").orElse(null);
            if (attributeId.isBlank() || baseValue == null || excludedIds.contains(attributeId)) {
                continue;
            }
            ItemEditorState.EntityAttributeDraft draft = new ItemEditorState.EntityAttributeDraft();
            draft.attributeId = attributeId;
            draft.baseValue = ValidationUtil.trimTrailingZeros(baseValue);
            draft.originalTag = attributeTag.copy();
            target.add(draft);
        }
    }

    static boolean applyAttributes(
            CompoundTag entityTag,
            List<ItemEditorState.EntityAttributeDraft> drafts,
            Set<String> preservedIds,
            SpecialDataApplyContext context,
            String fieldLabel
    ) {
        ListTag encoded = new ListTag();
        ListTag original = entityTag.getListOrEmpty("attributes");
        for (int index = 0; index < original.size(); index++) {
            CompoundTag attributeTag = original.getCompoundOrEmpty(index);
            String attributeId = attributeTag.getStringOr("id", "");
            if (attributeId.isBlank()
                    || attributeTag.getDouble("base").isEmpty()
                    || preservedIds.contains(attributeId)) {
                encoded.add(attributeTag.copy());
            }
        }

        Registry<Attribute> registry = context.registryAccess().lookupOrThrow(Registries.ATTRIBUTE);
        String attributeField = fieldLabel + " " + ItemEditorText.str("attributes.modifier.attribute");
        for (ItemEditorState.EntityAttributeDraft draft : drafts) {
            String rawId = draft.attributeId == null ? "" : draft.attributeId.trim();
            Identifier attributeId = IdFieldNormalizer.parse(rawId);
            Double baseValue = ValidationUtil.parseDouble(draft.baseValue, attributeField, context.messages());
            if (attributeId == null || baseValue == null) {
                if (attributeId == null) {
                    context.messages().add(ValidationMessage.error(ItemEditorText.str(
                            "validation.registry_missing",
                            attributeField,
                            rawId
                    )));
                }
                return false;
            }

            var holder = registry.get(attributeId).orElse(null);
            if (holder == null) {
                Double originalBase = draft.originalTag.getDouble("base").orElse(null);
                if (rawId.equals(draft.originalTag.getStringOr("id", ""))
                        && originalBase != null
                        && Double.compare(baseValue, originalBase) == 0) {
                    encoded.add(draft.originalTag.copy());
                    continue;
                }
                context.messages().add(ValidationMessage.error(ItemEditorText.str(
                        "validation.registry_missing",
                        attributeField,
                        rawId
                )));
                return false;
            }

            Attribute attribute = holder.value();
            double sanitized = attribute.sanitizeValue(baseValue);
            if (!Double.isFinite(baseValue) || Double.compare(baseValue, sanitized) != 0) {
                context.messages().add(ValidationMessage.error(ItemEditorText.str(
                        "validation.range",
                        attributeField,
                        ValidationUtil.trimTrailingZeros(attribute.sanitizeValue(-Double.MAX_VALUE)),
                        ValidationUtil.trimTrailingZeros(attribute.sanitizeValue(Double.MAX_VALUE))
                )));
                return false;
            }

            CompoundTag attributeTag = draft.originalTag.copy();
            attributeTag.putString("id", attributeId.toString());
            attributeTag.putDouble("base", baseValue);
            encoded.add(attributeTag);
        }

        if (encoded.isEmpty()) {
            entityTag.remove("attributes");
        } else {
            entityTag.put("attributes", encoded);
        }
        return true;
    }

    static boolean applyHealth(
            CompoundTag entityTag,
            String rawValue,
            String entityId,
            float minimum,
            SpecialDataApplyContext context,
            String fieldLabel
    ) {
        String raw = rawValue == null ? "" : rawValue.trim();
        if (raw.isBlank()) {
            entityTag.remove("Health");
            return true;
        }

        Float health = ValidationUtil.parseFloat(raw, fieldLabel, context.messages());
        if (health == null) {
            return false;
        }
        Double maximum = effectiveMaxHealth(entityTag, context.registryAccess(), entityId);
        if (!Float.isFinite(health)
                || health < minimum
                || maximum != null && health > maximum) {
            context.messages().add(ValidationMessage.error(ItemEditorText.str(
                    "validation.range",
                    fieldLabel,
                    ValidationUtil.trimTrailingZeros(minimum),
                    maximum == null ? Float.MAX_VALUE : ValidationUtil.trimTrailingZeros(maximum)
            )));
            return false;
        }
        entityTag.putFloat("Health", health);
        return true;
    }

    static boolean sameAttributes(
            List<ItemEditorState.EntityAttributeDraft> current,
            List<ItemEditorState.EntityAttributeDraft> baseline
    ) {
        if (current.size() != baseline.size()) {
            return false;
        }
        for (int index = 0; index < current.size(); index++) {
            ItemEditorState.EntityAttributeDraft left = current.get(index);
            ItemEditorState.EntityAttributeDraft right = baseline.get(index);
            if (!Objects.equals(left.attributeId, right.attributeId)
                    || !Objects.equals(left.baseValue, right.baseValue)) {
                return false;
            }
        }
        return true;
    }

    public static Map<String, Double> defaultAttributeValues(
            RegistryAccess registryAccess,
            String rawEntityId
    ) {
        Identifier entityId = IdFieldNormalizer.parse(rawEntityId == null ? "" : rawEntityId);
        EntityType<?> entityType = entityId == null
                ? null
                : BuiltInRegistries.ENTITY_TYPE.getOptional(entityId).orElse(null);
        if (entityType == null || !DefaultAttributes.hasSupplier(entityType)) {
            return Map.of();
        }

        AttributeSupplier defaults = defaultAttributes(entityType);
        Registry<Attribute> registry = registryAccess.lookupOrThrow(Registries.ATTRIBUTE);
        Map<String, Double> values = new LinkedHashMap<>();
        registry.registryKeySet().stream()
                .sorted((left, right) -> left.identifier().compareTo(right.identifier()))
                .forEach(key -> registry.get(key.identifier())
                        .filter(defaults::hasAttribute)
                        .ifPresent(holder -> values.put(
                                key.identifier().toString(),
                                defaults.getBaseValue(holder)
                        )));
        return values;
    }

    public static String maxHealthAttributeId() {
        return Attributes.MAX_HEALTH.unwrapKey().orElseThrow().identifier().toString();
    }

    @SuppressWarnings("unchecked")
    private static AttributeSupplier defaultAttributes(EntityType<?> entityType) {
        return DefaultAttributes.getSupplier((EntityType<? extends LivingEntity>) entityType);
    }

    private static Double effectiveMaxHealth(
            CompoundTag entityTag,
            RegistryAccess registryAccess,
            String entityId
    ) {
        String maxHealthId = maxHealthAttributeId();
        ListTag attributes = entityTag.getListOrEmpty("attributes");
        for (int index = 0; index < attributes.size(); index++) {
            CompoundTag attributeTag = attributes.getCompoundOrEmpty(index);
            if (maxHealthId.equals(attributeTag.getStringOr("id", ""))) {
                return attributeTag.getDouble("base").orElse(null);
            }
        }
        return defaultAttributeValues(registryAccess, entityId).get(maxHealthId);
    }

    private static boolean applyDropChances(
            CompoundTag entityTag,
            ItemEditorState.EntityEquipmentDraft draft,
            SpecialDataApplyContext context,
            String fieldLabel
    ) {
        Map<EquipmentSlot, Float> values = new EnumMap<>(EquipmentSlot.class);
        for (EquipmentSlot slot : EquipmentSlot.VALUES) {
            String chanceLabel = fieldLabel + " " + slot.getSerializedName() + " "
                    + ItemEditorText.str("special.entity.equipment.drop_chance");
            Float chance = ValidationUtil.parseFloat(draft.dropChance(slot), chanceLabel, context.messages());
            if (chance == null) {
                return false;
            }
            if (!Float.isFinite(chance) || chance < 0.0F) {
                context.messages().add(ValidationMessage.error(ItemEditorText.str(
                        "validation.range",
                        chanceLabel,
                        0,
                        Float.MAX_VALUE
                )));
                return false;
            }
            values.put(slot, chance);
        }

        DropChances dropChances = new DropChances(values);
        if (dropChances.equals(DropChances.DEFAULT)) {
            entityTag.remove("drop_chances");
        } else {
            entityTag.store("drop_chances", DropChances.CODEC, dropChances);
        }
        return true;
    }

    static boolean sameEntity(
            ItemEditorState.EntitySpawnDraft current,
            ItemEditorState.EntitySpawnDraft baseline
    ) {
        return Objects.equals(current.entityId, baseline.entityId)
                && current.noAi == baseline.noAi
                && current.silent == baseline.silent
                && current.noGravity == baseline.noGravity
                && current.glowing == baseline.glowing
                && current.invulnerable == baseline.invulnerable
                && current.persistenceRequired == baseline.persistenceRequired
                && current.customNameVisible == baseline.customNameVisible
                && Objects.equals(current.customName, baseline.customName)
                && Objects.equals(current.health, baseline.health)
                && sameAttributes(current.attributes, baseline.attributes)
                && sameEquipment(current.equipment, baseline.equipment);
    }

    static boolean isEntityDefault(ItemEditorState.EntitySpawnDraft draft) {
        return (draft.entityId == null || draft.entityId.isBlank())
                && !draft.noAi
                && !draft.silent
                && !draft.noGravity
                && !draft.glowing
                && !draft.invulnerable
                && !draft.persistenceRequired
                && !draft.customNameVisible
                && (draft.customName == null || draft.customName.isBlank())
                && (draft.health == null || draft.health.isBlank())
                && draft.attributes.isEmpty()
                && draft.equipment.isEmpty();
    }
}
