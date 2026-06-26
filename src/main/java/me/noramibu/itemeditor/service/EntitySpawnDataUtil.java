package me.noramibu.itemeditor.service;

import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.editor.ValidationMessage;
import me.noramibu.itemeditor.util.IdFieldNormalizer;
import me.noramibu.itemeditor.util.ItemEditorText;
import me.noramibu.itemeditor.util.TextComponentUtil;
import me.noramibu.itemeditor.util.ValidationUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.Identifier;

import java.util.Objects;

final class EntitySpawnDataUtil {

    private EntitySpawnDataUtil() {
    }

    static void readEntity(CompoundTag entityTag, ItemEditorState.EntitySpawnDraft draft) {
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
                .ifPresent(value -> draft.health = trimTrailingZeros(value));
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
        setBooleanKey(entityTag, "NoAI", draft.noAi);
        setBooleanKey(entityTag, "Silent", draft.silent);
        setBooleanKey(entityTag, "NoGravity", draft.noGravity);
        setBooleanKey(entityTag, "Glowing", draft.glowing);
        setBooleanKey(entityTag, "Invulnerable", draft.invulnerable);
        setBooleanKey(entityTag, "PersistenceRequired", draft.persistenceRequired);
        setBooleanKey(entityTag, "CustomNameVisible", draft.customNameVisible);
        applyCustomName(entityTag, draft.customName);
        return applyHealth(entityTag, draft.health, context, fieldLabel) ? entityTag : null;
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
                && Objects.equals(current.health, baseline.health);
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
                && (draft.health == null || draft.health.isBlank());
    }

    private static void applyCustomName(CompoundTag entityTag, String rawName) {
        if (rawName == null || rawName.isBlank()) {
            entityTag.remove("CustomName");
            return;
        }
        entityTag.store(
                "CustomName",
                ComponentSerialization.CODEC,
                TextComponentUtil.parseMarkup(rawName)
        );
    }

    private static boolean applyHealth(
            CompoundTag entityTag,
            String rawHealth,
            SpecialDataApplyContext context,
            String fieldLabel
    ) {
        String raw = rawHealth == null ? "" : rawHealth.trim();
        if (raw.isBlank()) {
            entityTag.remove("Health");
            return true;
        }

        Float health = ValidationUtil.parseFloat(raw, fieldLabel + " Health", context.messages());
        if (health == null) {
            return false;
        }
        if (health < 0.0F) {
            context.messages().add(ValidationMessage.error(ItemEditorText.str(
                    "validation.range",
                    fieldLabel + " Health",
                    0,
                    2048
            )));
            return false;
        }
        entityTag.putFloat("Health", health);
        return true;
    }

    private static void setBooleanKey(CompoundTag tag, String key, boolean value) {
        if (value) {
            tag.putBoolean(key, true);
        } else {
            tag.remove(key);
        }
    }

    private static String trimTrailingZeros(float value) {
        if (value == (long) value) {
            return Long.toString((long) value);
        }
        return Float.toString(value);
    }
}
