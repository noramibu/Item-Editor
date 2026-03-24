package me.noramibu.itemeditor.service;

import me.noramibu.itemeditor.editor.ValidationMessage;
import me.noramibu.itemeditor.util.ItemEditorText;
import me.noramibu.itemeditor.util.RegistryUtil;
import me.noramibu.itemeditor.util.ValidationUtil;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;

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

    protected final Holder<MobEffect> resolveEffectOrReport(
            Registry<MobEffect> effectRegistry,
            String effectId,
            String effectFieldTranslationKey,
            List<ValidationMessage> messages
    ) {
        Holder<MobEffect> effect = RegistryUtil.resolveHolder(effectRegistry, effectId);
        if (effect == null) {
            messages.add(ValidationMessage.error(ItemEditorText.str(
                    "validation.registry_missing",
                    ItemEditorText.str(effectFieldTranslationKey),
                    effectId
            )));
        }
        return effect;
    }
}
