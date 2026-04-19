package me.noramibu.itemeditor.service;

import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.editor.ValidationMessage;
import me.noramibu.itemeditor.util.ItemEditorText;
import me.noramibu.itemeditor.util.RegistryUtil;
import me.noramibu.itemeditor.util.ValidationUtil;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.ArrayList;
import java.util.List;

abstract class AbstractPreviewApplierSupport extends PreviewServiceSupport {

    protected final Component withPlainBaseline(Component component) {
        return component.copy().withStyle(component.getStyle().withItalic(false));
    }

    protected final void putOptionalIntTag(
            CompoundTag tag,
            String key,
            String raw,
            String fieldName,
            int min,
            int max,
            List<ValidationMessage> messages
    ) {
        String normalized = raw.trim();
        if (normalized.isBlank()) {
            tag.remove(key);
            return;
        }

        Integer parsed = ValidationUtil.parseInt(normalized, fieldName, min, max, messages);
        if (parsed != null) {
            tag.putInt(key, parsed);
        }
    }

    protected final Holder<MobEffect> resolvePotionEffectOrReport(
            Registry<MobEffect> effectRegistry,
            String effectId,
            List<ValidationMessage> messages
    ) {
        Holder<MobEffect> effect = RegistryUtil.resolveHolder(effectRegistry, effectId);
        if (effect == null) {
            messages.add(ValidationMessage.error(ItemEditorText.str(
                    "validation.registry_missing",
                    ItemEditorText.str("special.potion.effect_id"),
                    effectId
            )));
        }
        return effect;
    }

    protected final Integer parsePotionDuration(String rawDuration, List<ValidationMessage> messages) {
        return ValidationUtil.parseInt(
                rawDuration,
                ItemEditorText.str("special.potion.duration"),
                1,
                Integer.MAX_VALUE,
                messages
        );
    }

    protected final Integer parsePotionAmplifier(String rawAmplifier, List<ValidationMessage> messages) {
        return ValidationUtil.parseInt(
                rawAmplifier,
                ItemEditorText.str("special.potion.amplifier"),
                0,
                MobEffectInstance.MAX_AMPLIFIER,
                messages
        );
    }

    protected final List<MobEffectInstance> parsePotionEffectInstances(
            List<ItemEditorState.PotionEffectDraft> drafts,
            Registry<MobEffect> effectRegistry,
            List<ValidationMessage> messages
    ) {
        List<MobEffectInstance> effects = new ArrayList<>();
        for (ItemEditorState.PotionEffectDraft draft : drafts) {
            if (draft.effectId == null || draft.effectId.isBlank()) {
                continue;
            }

            Holder<MobEffect> effect = this.resolvePotionEffectOrReport(effectRegistry, draft.effectId, messages);
            if (effect == null) {
                continue;
            }

            Integer duration = this.parsePotionDuration(draft.duration, messages);
            Integer amplifier = this.parsePotionAmplifier(draft.amplifier, messages);
            if (duration == null || amplifier == null) {
                continue;
            }

            effects.add(new MobEffectInstance(
                    effect,
                    duration,
                    amplifier,
                    draft.ambient,
                    draft.visible,
                    draft.showIcon
            ));
        }
        return effects;
    }
}
