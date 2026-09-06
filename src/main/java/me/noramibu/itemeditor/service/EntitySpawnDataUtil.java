package me.noramibu.itemeditor.service;

import com.mojang.math.Transformation;
import com.mojang.serialization.DataResult;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.editor.ValidationMessage;
import me.noramibu.itemeditor.util.IdFieldNormalizer;
import me.noramibu.itemeditor.util.ItemEditorText;
import me.noramibu.itemeditor.util.TextComponentCompactor;
import me.noramibu.itemeditor.util.TextComponentUtil;
import me.noramibu.itemeditor.util.ValidationUtil;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Brightness;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.DropChances;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public final class EntitySpawnDataUtil {

    private EntitySpawnDataUtil() {}

    public static CompoundTag toEntityTag(ItemEditorState.EntitySpawnDraft draft, RegistryAccess registries) {
        return applyEntity(
                draft,
                new SpecialDataApplyContext(
                        ItemStack.EMPTY,
                        ItemStack.EMPTY,
                        new ItemEditorState(),
                        new ItemEditorState(),
                        registries,
                        new ArrayList<>()),
                draft.entityId);
    }

    public static void replaceEntity(
            ItemEditorState.EntitySpawnDraft draft, CompoundTag tag, RegistryAccess registries) {
        draft.entityTagEdits.clear();
        draft.customName = "";
        draft.health = "";
        readEntity(tag, draft, registries);
    }

    static void readEntity(
            CompoundTag entityTag, ItemEditorState.EntitySpawnDraft draft, RegistryAccess registryAccess) {
        draft.originalEntityTag = entityTag.copy();
        draft.entityId = entityTag.getStringOr("id", "");
        draft.noAi = entityTag.getBooleanOr("NoAI", false);
        draft.silent = entityTag.getBooleanOr("Silent", false);
        draft.noGravity = entityTag.getBooleanOr("NoGravity", false);
        draft.glowing = entityTag.getBooleanOr("Glowing", false);
        draft.invulnerable = entityTag.getBooleanOr("Invulnerable", false);
        draft.persistenceRequired = entityTag.getBooleanOr("PersistenceRequired", false);
        draft.customNameVisible = entityTag.getBooleanOr("CustomNameVisible", false);
        entityTag
                .read("CustomName", ComponentSerialization.CODEC)
                .ifPresent(component -> draft.customName = TextComponentUtil.toMarkup(component));
        entityTag.getFloat("Health").ifPresent(value -> draft.health = ValidationUtil.trimTrailingZeros(value));
        readEffects(entityTag, draft.effects);
        draft.uiEffectsCollapsed = draft.effects.isEmpty();
        draft.displayValues.clear();
        draft.displayItemStack = ItemStack.EMPTY;
        if (isItemEntity(draft.entityId)) {
            readItemEntityData(entityTag, draft, registryAccess);
        } else if (isDisplayEntity(draft.entityId)) {
            readDisplayEntityData(entityTag, draft, registryAccess);
        }
        readAttributes(entityTag, draft.attributes, Set.of());
        readEquipment(entityTag, draft.equipment, registryAccess);
        DropChances dropChances =
                entityTag.read("drop_chances", DropChances.CODEC).orElse(DropChances.DEFAULT);
        for (EquipmentSlot slot : EquipmentSlot.VALUES) {
            String chance = ValidationUtil.trimTrailingZeros(dropChances.byEquipment(slot));
            draft.equipment.setDropChance(slot, chance);
        }
        draft.equipment.uiCollapsed = draft.equipment.isEmpty();
    }

    static CompoundTag applyEntity(
            ItemEditorState.EntitySpawnDraft draft, SpecialDataApplyContext context, String fieldLabel) {
        CompoundTag result = applyKnownEntity(draft, context, fieldLabel);
        if (result != null && !EntityTagFields.apply(draft, result, context)) {
            return null;
        }
        return result;
    }

    private static CompoundTag applyKnownEntity(
            ItemEditorState.EntitySpawnDraft draft, SpecialDataApplyContext context, String fieldLabel) {
        CompoundTag entityTag = draft.originalEntityTag.copy();
        String entityIdRaw = draft.entityId == null ? "" : draft.entityId.trim();
        if (entityIdRaw.isBlank()) {
            if (!isEntityDefault(draft)) {
                context.messages()
                        .add(ValidationMessage.error(
                                ItemEditorText.str("preview.validation.component_failed", fieldLabel)));
            }
            return null;
        }

        Identifier entityId = IdFieldNormalizer.parse(entityIdRaw);
        if (entityId == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(entityId)) {
            context.messages()
                    .add(ValidationMessage.error(
                            ItemEditorText.str("validation.registry_missing", fieldLabel, entityIdRaw)));
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
        if (!applyEffects(entityTag, draft.effects, context, fieldLabel)) {
            return null;
        }
        if (isItemEntity(entityId.toString())) {
            return applyItemEntityData(entityTag, draft, context, fieldLabel) ? entityTag : null;
        }
        if (isDisplayEntity(entityId.toString())) {
            return applyDisplayEntityData(entityTag, draft, context, fieldLabel) ? entityTag : null;
        }
        if (!supportsStatusEffects(entityId.toString())) {
            return entityTag;
        }
        if (!applyAttributes(entityTag, draft.attributes, Set.of(), context, fieldLabel)
                || !applyHealth(
                        entityTag,
                        draft.health,
                        draft.entityId,
                        0.0F,
                        context,
                        fieldLabel + " " + ItemEditorText.str("special.entity.health"))) {
            return null;
        }
        if (!applyEquipment(entityTag, draft.equipment, context, fieldLabel)) {
            return null;
        }
        return applyDropChances(entityTag, draft.equipment, context, fieldLabel) ? entityTag : null;
    }

    public static boolean isItemEntity(String entityId) {
        return "minecraft:item".equals(IdFieldNormalizer.normalize(entityId == null ? "" : entityId));
    }

    public static boolean isDisplayEntity(String entityId) {
        return !displayType(entityId).isEmpty();
    }

    public static boolean supportsStatusEffects(String rawEntityId) {
        Identifier entityId = IdFieldNormalizer.parse(rawEntityId == null ? "" : rawEntityId);
        EntityType<?> entityType = entityId == null
                ? null
                : BuiltInRegistries.ENTITY_TYPE.getOptional(entityId).orElse(null);
        return entityType != null && DefaultAttributes.hasSupplier(entityType);
    }

    public static String displayType(String entityId) {
        return switch (IdFieldNormalizer.normalize(entityId == null ? "" : entityId)) {
            case "minecraft:block_display" -> "block";
            case "minecraft:item_display" -> "item";
            case "minecraft:text_display" -> "text";
            default -> "";
        };
    }

    public static String displayValue(ItemEditorState.EntitySpawnDraft draft, String key, String fallback) {
        return draft.displayValues.getOrDefault(key, fallback);
    }

    public static boolean displayFlag(ItemEditorState.EntitySpawnDraft draft, String key) {
        return Boolean.parseBoolean(displayValue(draft, key, "false"));
    }

    private static void readEffects(CompoundTag entityTag, List<ItemEditorState.PotionEffectDraft> effects) {
        effects.clear();
        ListTag effectsTag = entityTag.getListOrEmpty("active_effects");
        for (int index = 0; index < effectsTag.size(); index++) {
            CompoundTag effectTag = effectsTag.getCompoundOrEmpty(index);
            ItemEditorState.PotionEffectDraft draft = new ItemEditorState.PotionEffectDraft();
            draft.effectId = effectTag.getStringOr("id", "");
            draft.duration = Integer.toString(effectTag.getIntOr("duration", 200));
            draft.amplifier = Integer.toString(effectTag.getIntOr("amplifier", 0));
            draft.ambient = effectTag.getBooleanOr("ambient", false);
            draft.visible = optionalBoolean(effectTag, "show_particles");
            draft.showIcon = optionalBoolean(effectTag, "show_icon");
            draft.originalTag = effectTag.copy();
            effects.add(draft);
        }
    }

    private static boolean applyEffects(
            CompoundTag entityTag,
            List<ItemEditorState.PotionEffectDraft> effects,
            SpecialDataApplyContext context,
            String fieldLabel) {
        ListTag effectsTag = new ListTag();
        Registry<?> effectRegistry = context.registryAccess().lookupOrThrow(Registries.MOB_EFFECT);
        for (ItemEditorState.PotionEffectDraft draft : effects) {
            String rawId = draft.effectId == null ? "" : draft.effectId.trim();
            if (rawId.isBlank()) {
                if (!draft.originalTag.isEmpty()) {
                    effectsTag.add(draft.originalTag.copy());
                }
                continue;
            }

            Identifier effectId = IdFieldNormalizer.parse(rawId);
            if (effectId == null || !effectRegistry.containsKey(effectId)) {
                context.messages()
                        .add(ValidationMessage.error(ItemEditorText.str(
                                "validation.registry_missing",
                                fieldLabel + " " + ItemEditorText.str("special.entity.effects"),
                                rawId)));
                return false;
            }
            Integer duration = ValidationUtil.parseInt(
                    draft.duration,
                    ItemEditorText.str("special.potion.duration"),
                    -1,
                    Integer.MAX_VALUE,
                    context.messages());
            Integer amplifier = ValidationUtil.parseInt(
                    draft.amplifier,
                    ItemEditorText.str("special.potion.amplifier"),
                    0,
                    MobEffectInstance.MAX_AMPLIFIER,
                    context.messages());
            if (duration == null || amplifier == null) {
                return false;
            }

            CompoundTag effectTag = draft.originalTag.copy();
            effectTag.putString("id", effectId.toString());
            effectTag.putInt("duration", duration);
            effectTag.putInt("amplifier", amplifier);
            NbtTagUtil.setBooleanKey(effectTag, "ambient", draft.ambient);
            NbtTagUtil.setOptionalBooleanKey(effectTag, "show_particles", draft.visible);
            NbtTagUtil.setOptionalBooleanKey(effectTag, "show_icon", draft.showIcon);
            effectsTag.add(effectTag);
        }
        if (effectsTag.isEmpty()) {
            entityTag.remove("active_effects");
        } else {
            entityTag.put("active_effects", effectsTag);
        }
        return true;
    }

    private static String optionalBoolean(CompoundTag tag, String key) {
        return tag.contains(key) ? Boolean.toString(tag.getBooleanOr(key, false)) : "";
    }

    private static void readItemEntityData(
            CompoundTag entityTag, ItemEditorState.EntitySpawnDraft draft, RegistryAccess registryAccess) {
        var ops = registryAccess.createSerializationContext(NbtOps.INSTANCE);
        ItemStack.CODEC
                .parse(ops, entityTag.getCompoundOrEmpty("Item"))
                .result()
                .ifPresent(stack -> {
                    draft.itemEntityStack = stack;
                    draft.itemEntityCount = Integer.toString(stack.getCount());
                });
        entityTag.getShort("Age").ifPresent(value -> draft.itemEntityAge = Short.toString(value));
        entityTag.getShort("PickupDelay").ifPresent(value -> draft.itemEntityPickupDelay = Short.toString(value));
        draft.itemEntityOwner =
                entityTag.read("Owner", UUIDUtil.CODEC).map(UUID::toString).orElse("");
        draft.itemEntityThrower =
                entityTag.read("Thrower", UUIDUtil.CODEC).map(UUID::toString).orElse("");
    }

    private static boolean applyItemEntityData(
            CompoundTag entityTag,
            ItemEditorState.EntitySpawnDraft draft,
            SpecialDataApplyContext context,
            String fieldLabel) {
        if (draft.itemEntityStack == null || draft.itemEntityStack.isEmpty()) {
            context.messages()
                    .add(ValidationMessage.error(ItemEditorText.str(
                            "preview.validation.component_failed",
                            fieldLabel + " " + ItemEditorText.str("special.entity.item.stack"))));
            return false;
        }

        Integer count = ValidationUtil.parseInt(
                draft.itemEntityCount, ItemEditorText.str("common.count"), 1, 99, context.messages());
        if (count == null) {
            return false;
        }

        var ops = context.registryAccess().createSerializationContext(NbtOps.INSTANCE);
        CompoundTag itemTag = ItemStack.CODEC
                .encodeStart(ops, draft.itemEntityStack.copyWithCount(count))
                .result()
                .filter(CompoundTag.class::isInstance)
                .map(CompoundTag.class::cast)
                .orElse(null);
        if (itemTag == null) {
            context.messages()
                    .add(ValidationMessage.error(ItemEditorText.str(
                            "preview.validation.component_failed",
                            fieldLabel + " " + ItemEditorText.str("special.entity.item.stack"))));
            return false;
        }
        entityTag.put("Item", itemTag);

        return putOptionalShort(
                        entityTag,
                        "Health",
                        draft.health,
                        ItemEditorText.str("special.entity.health"),
                        context.messages())
                && putOptionalShort(
                        entityTag,
                        "Age",
                        draft.itemEntityAge,
                        ItemEditorText.str("special.bucket.age"),
                        context.messages())
                && putOptionalShort(
                        entityTag,
                        "PickupDelay",
                        draft.itemEntityPickupDelay,
                        ItemEditorText.str("special.entity.item.pickup_delay"),
                        context.messages())
                && putOptionalUuid(
                        entityTag,
                        "Owner",
                        draft.itemEntityOwner,
                        ItemEditorText.str("special.entity.item.owner"),
                        context.messages())
                && putOptionalUuid(
                        entityTag,
                        "Thrower",
                        draft.itemEntityThrower,
                        ItemEditorText.str("special.entity.item.thrower"),
                        context.messages());
    }

    private static boolean putOptionalShort(
            CompoundTag tag, String key, String rawValue, String fieldLabel, List<ValidationMessage> messages) {
        if (rawValue == null || rawValue.isBlank()) {
            tag.remove(key);
            return true;
        }
        Integer value = ValidationUtil.parseInt(rawValue, fieldLabel, Short.MIN_VALUE, Short.MAX_VALUE, messages);
        if (value == null) {
            return false;
        }
        tag.putShort(key, value.shortValue());
        return true;
    }

    private static boolean putOptionalUuid(
            CompoundTag tag, String key, String rawValue, String fieldLabel, List<ValidationMessage> messages) {
        if (rawValue == null || rawValue.isBlank()) {
            tag.remove(key);
            return true;
        }
        try {
            tag.store(key, UUIDUtil.CODEC, UUID.fromString(rawValue.trim()));
            return true;
        } catch (IllegalArgumentException exception) {
            messages.add(ValidationMessage.error(ItemEditorText.str("validation.uuid", fieldLabel)));
            return false;
        }
    }

    private static void readDisplayEntityData(
            CompoundTag entityTag, ItemEditorState.EntitySpawnDraft draft, RegistryAccess registryAccess) {
        Transformation transformation =
                entityTag.read("transformation", Transformation.EXTENDED_CODEC).orElse(Transformation.IDENTITY);
        putVector(draft, "translation", transformation.translation());
        putVector(draft, "scale", transformation.scale());
        putQuaternion(draft, "left_rotation", transformation.leftRotation());
        putQuaternion(draft, "right_rotation", transformation.rightRotation());
        putDisplayValue(draft, "interpolation_duration", entityTag.getIntOr("interpolation_duration", 0));
        putDisplayValue(draft, "start_interpolation", entityTag.getIntOr("start_interpolation", 0));
        putDisplayValue(draft, "teleport_duration", entityTag.getIntOr("teleport_duration", 0));
        draft.displayValues.put("billboard", entityTag.getStringOr("billboard", "fixed"));
        putDisplayValue(draft, "view_range", entityTag.getFloatOr("view_range", 1.0F));
        putDisplayValue(draft, "shadow_radius", entityTag.getFloatOr("shadow_radius", 0.0F));
        putDisplayValue(draft, "shadow_strength", entityTag.getFloatOr("shadow_strength", 1.0F));
        putDisplayValue(draft, "width", entityTag.getFloatOr("width", 0.0F));
        putDisplayValue(draft, "height", entityTag.getFloatOr("height", 0.0F));
        putDisplayValue(draft, "glow_color_override", entityTag.getIntOr("glow_color_override", -1));
        entityTag.read("brightness", Brightness.CODEC).ifPresent(brightness -> {
            putDisplayValue(draft, "brightness.block", brightness.block());
            putDisplayValue(draft, "brightness.sky", brightness.sky());
        });

        switch (displayType(draft.entityId)) {
            case "block" -> readBlockDisplayData(entityTag, draft);
            case "item" -> readItemDisplayData(entityTag, draft, registryAccess);
            case "text" -> readTextDisplayData(entityTag, draft);
            default -> {}
        }
    }

    private static void readBlockDisplayData(CompoundTag entityTag, ItemEditorState.EntitySpawnDraft draft) {
        BlockState state = entityTag.read("block_state", BlockState.CODEC).orElse(Blocks.AIR.defaultBlockState());
        draft.displayValues.put(
                "block", BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
        Map<String, String> properties = new LinkedHashMap<>();
        for (Property<?> property : state.getProperties()) {
            properties.put(property.getName(), propertyValueName(state, property));
        }
        draft.displayValues.put("block_properties", serializeDisplayProperties(properties));
    }

    private static void readItemDisplayData(
            CompoundTag entityTag, ItemEditorState.EntitySpawnDraft draft, RegistryAccess registryAccess) {
        var ops = registryAccess.createSerializationContext(NbtOps.INSTANCE);
        ItemStack.CODEC
                .parse(ops, entityTag.getCompoundOrEmpty("item"))
                .result()
                .ifPresent(stack -> draft.displayItemStack = stack);
        draft.displayValues.put(
                "item_display",
                entityTag
                        .read("item_display", ItemDisplayContext.CODEC)
                        .map(ItemDisplayContext::getSerializedName)
                        .orElse("none"));
    }

    private static void readTextDisplayData(CompoundTag entityTag, ItemEditorState.EntitySpawnDraft draft) {
        entityTag
                .read("text", ComponentSerialization.CODEC)
                .ifPresent(text -> draft.displayValues.put("text", TextComponentUtil.toMarkup(text)));
        putDisplayValue(draft, "line_width", entityTag.getIntOr("line_width", 200));
        putDisplayValue(draft, "background", entityTag.getIntOr("background", 1073741824));
        putDisplayValue(draft, "text_opacity", entityTag.getByteOr("text_opacity", (byte) -1));
        putDisplayValue(draft, "shadow", entityTag.getBooleanOr("shadow", false));
        putDisplayValue(draft, "see_through", entityTag.getBooleanOr("see_through", false));
        putDisplayValue(draft, "default_background", entityTag.getBooleanOr("default_background", false));
        draft.displayValues.put("alignment", entityTag.getStringOr("alignment", "center"));
    }

    private static boolean applyDisplayEntityData(
            CompoundTag entityTag,
            ItemEditorState.EntitySpawnDraft draft,
            SpecialDataApplyContext context,
            String fieldLabel) {
        String transformLabel = fieldLabel + " " + ItemEditorText.str("special.entity.display.transform");
        Float tx = displayFloat(draft, "translation.x", "0", transformLabel, context);
        Float ty = displayFloat(draft, "translation.y", "0", transformLabel, context);
        Float tz = displayFloat(draft, "translation.z", "0", transformLabel, context);
        Float sx = displayFloat(draft, "scale.x", "1", transformLabel, context);
        Float sy = displayFloat(draft, "scale.y", "1", transformLabel, context);
        Float sz = displayFloat(draft, "scale.z", "1", transformLabel, context);
        Float lrx = displayFloat(draft, "left_rotation.x", "0", transformLabel, context);
        Float lry = displayFloat(draft, "left_rotation.y", "0", transformLabel, context);
        Float lrz = displayFloat(draft, "left_rotation.z", "0", transformLabel, context);
        Float lrw = displayFloat(draft, "left_rotation.w", "1", transformLabel, context);
        Float rrx = displayFloat(draft, "right_rotation.x", "0", transformLabel, context);
        Float rry = displayFloat(draft, "right_rotation.y", "0", transformLabel, context);
        Float rrz = displayFloat(draft, "right_rotation.z", "0", transformLabel, context);
        Float rrw = displayFloat(draft, "right_rotation.w", "1", transformLabel, context);
        if (tx == null
                || ty == null
                || tz == null
                || sx == null
                || sy == null
                || sz == null
                || lrx == null
                || lry == null
                || lrz == null
                || lrw == null
                || rrx == null
                || rry == null
                || rrz == null
                || rrw == null) {
            return false;
        }
        entityTag.store(
                "transformation",
                Transformation.EXTENDED_CODEC,
                new Transformation(
                        new Vector3f(tx, ty, tz),
                        new Quaternionf(lrx, lry, lrz, lrw),
                        new Vector3f(sx, sy, sz),
                        new Quaternionf(rrx, rry, rrz, rrw)));

        String renderingLabel = fieldLabel + " " + ItemEditorText.str("special.entity.display.rendering");
        Integer interpolationDuration = displayInt(
                draft, "interpolation_duration", "0", Integer.MIN_VALUE, Integer.MAX_VALUE, renderingLabel, context);
        Integer startInterpolation = displayInt(
                draft, "start_interpolation", "0", Integer.MIN_VALUE, Integer.MAX_VALUE, renderingLabel, context);
        Integer teleportDuration = displayInt(draft, "teleport_duration", "0", 0, 59, renderingLabel, context);
        Float viewRange = displayFloat(draft, "view_range", "1", renderingLabel, context);
        Float shadowRadius = displayFloat(draft, "shadow_radius", "0", renderingLabel, context);
        Float shadowStrength = displayFloat(draft, "shadow_strength", "1", renderingLabel, context);
        Float width = displayFloat(draft, "width", "0", renderingLabel, context);
        Float height = displayFloat(draft, "height", "0", renderingLabel, context);
        Integer glowColor = displayInt(
                draft, "glow_color_override", "-1", Integer.MIN_VALUE, Integer.MAX_VALUE, renderingLabel, context);
        if (interpolationDuration == null
                || startInterpolation == null
                || teleportDuration == null
                || viewRange == null
                || shadowRadius == null
                || shadowStrength == null
                || width == null
                || height == null
                || glowColor == null) {
            return false;
        }

        entityTag.putInt("interpolation_duration", interpolationDuration);
        entityTag.putInt("start_interpolation", startInterpolation);
        entityTag.putInt("teleport_duration", teleportDuration);
        entityTag.putString("billboard", displayValue(draft, "billboard", "fixed"));
        entityTag.putFloat("view_range", viewRange);
        entityTag.putFloat("shadow_radius", shadowRadius);
        entityTag.putFloat("shadow_strength", shadowStrength);
        entityTag.putFloat("width", width);
        entityTag.putFloat("height", height);
        entityTag.putInt("glow_color_override", glowColor);
        if (!applyDisplayBrightness(entityTag, draft, renderingLabel, context)) {
            return false;
        }

        clearDisplaySpecificData(entityTag);
        return switch (displayType(draft.entityId)) {
            case "block" -> applyBlockDisplayData(entityTag, draft, context, fieldLabel);
            case "item" -> applyItemDisplayData(entityTag, draft, context, fieldLabel);
            case "text" -> applyTextDisplayData(entityTag, draft, context, fieldLabel);
            default -> false;
        };
    }

    private static boolean applyDisplayBrightness(
            CompoundTag entityTag,
            ItemEditorState.EntitySpawnDraft draft,
            String fieldLabel,
            SpecialDataApplyContext context) {
        String blockRaw = displayValue(draft, "brightness.block", "");
        String skyRaw = displayValue(draft, "brightness.sky", "");
        if (blockRaw.isBlank() && skyRaw.isBlank()) {
            entityTag.remove("brightness");
            return true;
        }
        Integer block = ValidationUtil.parseInt(blockRaw, fieldLabel, 0, 15, context.messages());
        Integer sky = ValidationUtil.parseInt(skyRaw, fieldLabel, 0, 15, context.messages());
        if (block == null || sky == null) {
            return false;
        }
        entityTag.store("brightness", Brightness.CODEC, new Brightness(block, sky));
        return true;
    }

    private static boolean applyBlockDisplayData(
            CompoundTag entityTag,
            ItemEditorState.EntitySpawnDraft draft,
            SpecialDataApplyContext context,
            String fieldLabel) {
        String rawBlockId = displayValue(draft, "block", "minecraft:air");
        Identifier blockId = IdFieldNormalizer.parse(rawBlockId);
        Block block = blockId == null
                ? null
                : BuiltInRegistries.BLOCK.getOptional(blockId).orElse(null);
        if (block == null) {
            context.messages()
                    .add(ValidationMessage.error(ItemEditorText.str(
                            "validation.registry_missing",
                            fieldLabel + " " + ItemEditorText.str("special.entity.display.block"),
                            rawBlockId)));
            return false;
        }

        BlockState state = block.defaultBlockState();
        for (Map.Entry<String, String> entry : parseDisplayProperties(displayValue(draft, "block_properties", ""))
                .entrySet()) {
            Property<?> property = block.getStateDefinition().getProperty(entry.getKey());
            state = property == null ? null : setProperty(state, property, entry.getValue());
            if (state == null) {
                context.messages()
                        .add(ValidationMessage.error(ItemEditorText.str(
                                "preview.validation.component_failed",
                                fieldLabel + " " + ItemEditorText.str("special.entity.display.block_state"))));
                return false;
            }
        }
        entityTag.store("block_state", BlockState.CODEC, state);
        return true;
    }

    private static boolean applyItemDisplayData(
            CompoundTag entityTag,
            ItemEditorState.EntitySpawnDraft draft,
            SpecialDataApplyContext context,
            String fieldLabel) {
        if (draft.displayItemStack != null && !draft.displayItemStack.isEmpty()) {
            DataResult<Tag> encoded = ItemStack.CODEC.encodeStart(
                    context.registryAccess().createSerializationContext(NbtOps.INSTANCE), draft.displayItemStack);
            Tag itemTag = encoded.result().orElse(null);
            if (itemTag == null) {
                context.messages()
                        .add(ValidationMessage.error(ItemEditorText.str(
                                "preview.validation.component_failed",
                                fieldLabel + " " + ItemEditorText.str("special.entity.display.item"))));
                return false;
            }
            entityTag.put("item", itemTag);
        }
        String displayContext = displayValue(draft, "item_display", "none");
        ItemDisplayContext itemDisplayContext = Arrays.stream(ItemDisplayContext.values())
                .filter(value -> value.getSerializedName().equals(displayContext))
                .findFirst()
                .orElse(ItemDisplayContext.NONE);
        entityTag.store("item_display", ItemDisplayContext.CODEC, itemDisplayContext);
        return true;
    }

    private static boolean applyTextDisplayData(
            CompoundTag entityTag,
            ItemEditorState.EntitySpawnDraft draft,
            SpecialDataApplyContext context,
            String fieldLabel) {
        String textLabel = fieldLabel + " " + ItemEditorText.str("special.entity.display.text");
        Integer lineWidth =
                displayInt(draft, "line_width", "200", Integer.MIN_VALUE, Integer.MAX_VALUE, textLabel, context);
        Integer background =
                displayInt(draft, "background", "1073741824", Integer.MIN_VALUE, Integer.MAX_VALUE, textLabel, context);
        Integer opacity = displayInt(draft, "text_opacity", "-1", Byte.MIN_VALUE, Byte.MAX_VALUE, textLabel, context);
        if (lineWidth == null || background == null || opacity == null) {
            return false;
        }
        entityTag.store(
                "text",
                ComponentSerialization.CODEC,
                TextComponentCompactor.compact(TextComponentUtil.parseMarkup(displayValue(draft, "text", ""))));
        entityTag.putInt("line_width", lineWidth);
        entityTag.putInt("background", background);
        entityTag.putByte("text_opacity", opacity.byteValue());
        entityTag.putBoolean("shadow", displayFlag(draft, "shadow"));
        entityTag.putBoolean("see_through", displayFlag(draft, "see_through"));
        entityTag.putBoolean("default_background", displayFlag(draft, "default_background"));
        entityTag.putString("alignment", displayValue(draft, "alignment", "center"));
        return true;
    }

    private static Float displayFloat(
            ItemEditorState.EntitySpawnDraft draft,
            String key,
            String fallback,
            String fieldLabel,
            SpecialDataApplyContext context) {
        Float value = ValidationUtil.parseFloat(displayValue(draft, key, fallback), fieldLabel, context.messages());
        if (value == null || Float.isFinite(value)) {
            return value;
        }
        context.messages()
                .add(ValidationMessage.error(
                        ItemEditorText.str("validation.range", fieldLabel, -Float.MAX_VALUE, Float.MAX_VALUE)));
        return null;
    }

    private static Integer displayInt(
            ItemEditorState.EntitySpawnDraft draft,
            String key,
            String fallback,
            int minimum,
            int maximum,
            String fieldLabel,
            SpecialDataApplyContext context) {
        return ValidationUtil.parseInt(
                displayValue(draft, key, fallback), fieldLabel, minimum, maximum, context.messages());
    }

    private static void clearDisplaySpecificData(CompoundTag entityTag) {
        for (String key : List.of(
                "block_state",
                "item",
                "item_display",
                "text",
                "line_width",
                "background",
                "text_opacity",
                "shadow",
                "see_through",
                "default_background",
                "alignment")) {
            entityTag.remove(key);
        }
    }

    private static void putVector(ItemEditorState.EntitySpawnDraft draft, String key, Vector3fc value) {
        putDisplayValue(draft, key + ".x", value.x());
        putDisplayValue(draft, key + ".y", value.y());
        putDisplayValue(draft, key + ".z", value.z());
    }

    private static void putQuaternion(ItemEditorState.EntitySpawnDraft draft, String key, Quaternionfc value) {
        putDisplayValue(draft, key + ".x", value.x());
        putDisplayValue(draft, key + ".y", value.y());
        putDisplayValue(draft, key + ".z", value.z());
        putDisplayValue(draft, key + ".w", value.w());
    }

    private static void putDisplayValue(ItemEditorState.EntitySpawnDraft draft, String key, Object value) {
        String serialized;
        if (value instanceof Float number) {
            serialized = ValidationUtil.trimTrailingZeros(number);
        } else if (value instanceof Double number) {
            serialized = ValidationUtil.trimTrailingZeros(number);
        } else {
            serialized = String.valueOf(value);
        }
        draft.displayValues.put(key, serialized);
    }

    public static Map<String, String> parseDisplayProperties(String raw) {
        Map<String, String> properties = new LinkedHashMap<>();
        if (raw == null || raw.isBlank()) {
            return properties;
        }
        for (String token : raw.split("[,\\r\\n]+")) {
            int separator = token.indexOf('=');
            if (separator > 0) {
                properties.put(
                        token.substring(0, separator).trim(),
                        token.substring(separator + 1).trim());
            }
        }
        return properties;
    }

    public static String serializeDisplayProperties(Map<String, String> properties) {
        return properties.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .reduce((left, right) -> left + ", " + right)
                .orElse("");
    }

    private static <T extends Comparable<T>> String propertyValueName(BlockState state, Property<T> property) {
        return property.getName(state.getValue(property));
    }

    private static <T extends Comparable<T>> BlockState setProperty(
            BlockState state, Property<T> property, String value) {
        return property.getValue(value)
                .map(parsed -> state.setValue(property, parsed))
                .orElse(null);
    }

    static void readEquipment(
            CompoundTag entityTag, ItemEditorState.EntityEquipmentDraft draft, RegistryAccess registryAccess) {
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
            String fieldLabel) {
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
                context.messages()
                        .add(ValidationMessage.error(
                                ItemEditorText.str("preview.validation.component_failed", fieldLabel)));
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
            ItemEditorState.EntityEquipmentDraft current, ItemEditorState.EntityEquipmentDraft baseline) {
        for (EquipmentSlot slot : EquipmentSlot.VALUES) {
            if (!ItemStack.matches(current.stack(slot), baseline.stack(slot))
                    || !Objects.equals(current.dropChance(slot), baseline.dropChance(slot))) {
                return false;
            }
        }
        return true;
    }

    static void readAttributes(
            CompoundTag entityTag, List<ItemEditorState.EntityAttributeDraft> target, Set<String> excludedIds) {
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
            String fieldLabel) {
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
                    context.messages()
                            .add(ValidationMessage.error(
                                    ItemEditorText.str("validation.registry_missing", attributeField, rawId)));
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
                context.messages()
                        .add(ValidationMessage.error(
                                ItemEditorText.str("validation.registry_missing", attributeField, rawId)));
                return false;
            }

            Attribute attribute = holder.value();
            double sanitized = attribute.sanitizeValue(baseValue);
            if (!Double.isFinite(baseValue) || Double.compare(baseValue, sanitized) != 0) {
                context.messages()
                        .add(ValidationMessage.error(ItemEditorText.str(
                                "validation.range",
                                attributeField,
                                ValidationUtil.trimTrailingZeros(attribute.sanitizeValue(-Double.MAX_VALUE)),
                                ValidationUtil.trimTrailingZeros(attribute.sanitizeValue(Double.MAX_VALUE)))));
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
            String fieldLabel) {
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
        if (!Float.isFinite(health) || health < minimum || maximum != null && health > maximum) {
            context.messages()
                    .add(ValidationMessage.error(ItemEditorText.str(
                            "validation.range",
                            fieldLabel,
                            ValidationUtil.trimTrailingZeros(minimum),
                            maximum == null ? Float.MAX_VALUE : ValidationUtil.trimTrailingZeros(maximum))));
            return false;
        }
        entityTag.putFloat("Health", health);
        return true;
    }

    static boolean sameAttributes(
            List<ItemEditorState.EntityAttributeDraft> current, List<ItemEditorState.EntityAttributeDraft> baseline) {
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

    public static Map<String, Double> defaultAttributeValues(RegistryAccess registryAccess, String rawEntityId) {
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
                .sorted(Comparator.comparing(ResourceKey::identifier))
                .forEach(key -> registry.get(key.identifier())
                        .filter(defaults::hasAttribute)
                        .ifPresent(holder -> values.put(key.identifier().toString(), defaults.getBaseValue(holder))));
        return values;
    }

    public static String maxHealthAttributeId() {
        return Attributes.MAX_HEALTH.unwrapKey().orElseThrow().identifier().toString();
    }

    @SuppressWarnings("unchecked")
    private static AttributeSupplier defaultAttributes(EntityType<?> entityType) {
        return DefaultAttributes.getSupplier((EntityType<? extends LivingEntity>) entityType);
    }

    private static Double effectiveMaxHealth(CompoundTag entityTag, RegistryAccess registryAccess, String entityId) {
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
            String fieldLabel) {
        Map<EquipmentSlot, Float> values = new EnumMap<>(EquipmentSlot.class);
        for (EquipmentSlot slot : EquipmentSlot.VALUES) {
            String chanceLabel = fieldLabel + " " + slot.getSerializedName() + " "
                    + ItemEditorText.str("special.entity.equipment.drop_chance");
            Float chance = ValidationUtil.parseFloat(draft.dropChance(slot), chanceLabel, context.messages());
            if (chance == null) {
                return false;
            }
            if (!Float.isFinite(chance) || chance < 0.0F) {
                context.messages()
                        .add(ValidationMessage.error(
                                ItemEditorText.str("validation.range", chanceLabel, 0, Float.MAX_VALUE)));
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

    static boolean sameEntity(ItemEditorState.EntitySpawnDraft current, ItemEditorState.EntitySpawnDraft baseline) {
        return Objects.equals(current.entityId, baseline.entityId)
                && Objects.equals(current.entityTagEdits, baseline.entityTagEdits)
                && Objects.equals(current.originalEntityTag, baseline.originalEntityTag)
                && current.noAi == baseline.noAi
                && current.silent == baseline.silent
                && current.noGravity == baseline.noGravity
                && current.glowing == baseline.glowing
                && current.invulnerable == baseline.invulnerable
                && current.persistenceRequired == baseline.persistenceRequired
                && current.customNameVisible == baseline.customNameVisible
                && Objects.equals(current.customName, baseline.customName)
                && Objects.equals(current.health, baseline.health)
                && ItemStack.matches(current.itemEntityStack, baseline.itemEntityStack)
                && Objects.equals(current.itemEntityCount, baseline.itemEntityCount)
                && Objects.equals(current.itemEntityAge, baseline.itemEntityAge)
                && Objects.equals(current.itemEntityPickupDelay, baseline.itemEntityPickupDelay)
                && Objects.equals(current.itemEntityOwner, baseline.itemEntityOwner)
                && Objects.equals(current.itemEntityThrower, baseline.itemEntityThrower)
                && Objects.equals(current.displayValues, baseline.displayValues)
                && ItemStack.matches(current.displayItemStack, baseline.displayItemStack)
                && sameAttributes(current.attributes, baseline.attributes)
                && sameEffects(current.effects, baseline.effects)
                && sameEquipment(current.equipment, baseline.equipment);
    }

    private static boolean sameEffects(
            List<ItemEditorState.PotionEffectDraft> current, List<ItemEditorState.PotionEffectDraft> baseline) {
        if (current.size() != baseline.size()) {
            return false;
        }
        for (int index = 0; index < current.size(); index++) {
            if (!current.get(index).hasSameValues(baseline.get(index))) {
                return false;
            }
        }
        return true;
    }

    static boolean isEntityDefault(ItemEditorState.EntitySpawnDraft draft) {
        return (draft.entityId == null || draft.entityId.isBlank())
                && draft.entityTagEdits.isEmpty()
                && !draft.noAi
                && !draft.silent
                && !draft.noGravity
                && !draft.glowing
                && !draft.invulnerable
                && !draft.persistenceRequired
                && !draft.customNameVisible
                && (draft.customName == null || draft.customName.isBlank())
                && (draft.health == null || draft.health.isBlank())
                && (draft.itemEntityStack == null || draft.itemEntityStack.isEmpty())
                && (draft.itemEntityCount == null
                        || draft.itemEntityCount.isBlank()
                        || "1".equals(draft.itemEntityCount.trim()))
                && (draft.itemEntityAge == null || draft.itemEntityAge.isBlank())
                && (draft.itemEntityPickupDelay == null || draft.itemEntityPickupDelay.isBlank())
                && (draft.itemEntityOwner == null || draft.itemEntityOwner.isBlank())
                && (draft.itemEntityThrower == null || draft.itemEntityThrower.isBlank())
                && draft.displayValues.isEmpty()
                && (draft.displayItemStack == null || draft.displayItemStack.isEmpty())
                && draft.attributes.isEmpty()
                && draft.effects.isEmpty()
                && draft.equipment.isEmpty();
    }
}
