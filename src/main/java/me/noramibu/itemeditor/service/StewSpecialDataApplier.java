package me.noramibu.itemeditor.service;

import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.util.ItemEditorText;
import me.noramibu.itemeditor.util.ValidationUtil;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.component.SuspiciousStewEffects;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

final class StewSpecialDataApplier extends AbstractPreviewApplierSupport implements SpecialDataApplier {

    @Override
    public void apply(SpecialDataApplyContext context) {
        if (this.sameStewData(context.state(), context.baselineState())) {
            this.restoreOriginalComponent(context.originalStack(), context.previewStack(), DataComponents.SUSPICIOUS_STEW_EFFECTS);
            return;
        }

        Registry<MobEffect> effectRegistry = context.registryAccess().lookupOrThrow(Registries.MOB_EFFECT);
        List<SuspiciousStewEffects.Entry> entries = new ArrayList<>();

        for (ItemEditorState.SuspiciousStewEffectDraft draft : context.special().stewEffects) {
            if (draft.effectId.isBlank()) continue;

            Holder<MobEffect> effect = this.resolveEffectOrReport(
                    effectRegistry,
                    draft.effectId,
                    "special.potion.effect_id",
                    context.messages()
            );
            if (effect == null) continue;

            Integer duration = ValidationUtil.parseInt(draft.duration, ItemEditorText.str("special.potion.duration"), 1, Integer.MAX_VALUE, context.messages());
            if (duration == null) continue;

            entries.add(new SuspiciousStewEffects.Entry(effect, duration));
        }

        if (entries.isEmpty()) {
            this.clearToPrototype(context.previewStack(), DataComponents.SUSPICIOUS_STEW_EFFECTS);
        } else {
            context.previewStack().set(DataComponents.SUSPICIOUS_STEW_EFFECTS, new SuspiciousStewEffects(entries));
        }
    }

    private boolean sameStewData(ItemEditorState state, ItemEditorState baselineState) {
        return this.sameStewEffects(state.special.stewEffects, baselineState.special.stewEffects);
    }

    private boolean sameStewEffects(List<ItemEditorState.SuspiciousStewEffectDraft> current, List<ItemEditorState.SuspiciousStewEffectDraft> baseline) {
        return this.sameList(current, baseline, (left, right) -> Objects.equals(left.effectId, right.effectId)
                && Objects.equals(left.duration, right.duration));
    }
}
