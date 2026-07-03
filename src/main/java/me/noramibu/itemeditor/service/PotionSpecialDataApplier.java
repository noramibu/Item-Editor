package me.noramibu.itemeditor.service;

import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.editor.ValidationMessage;
import me.noramibu.itemeditor.util.ItemEditorText;
import me.noramibu.itemeditor.util.RegistryUtil;
import me.noramibu.itemeditor.util.ValidationUtil;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

final class PotionSpecialDataApplier extends AbstractPreviewApplierSupport implements SpecialDataApplier {

    @Override
    public void apply(SpecialDataApplyContext context) {
        this.applyPotionContents(context);
        this.applyPotionDurationScale(context);
    }

    private void applyPotionContents(SpecialDataApplyContext context) {
        if (this.samePotionContents(context.state(), context.baselineState())) {
            this.restoreOriginalComponent(context.originalStack(), context.previewStack(), DataComponents.POTION_CONTENTS);
            return;
        }

        Registry<Potion> potionRegistry = context.registryAccess().lookupOrThrow(Registries.POTION);
        Optional<Holder<Potion>> potionHolder = RegistryUtil.resolveOptionalHolder(potionRegistry, context.special().potionId, ItemEditorText.str("special.potion.potion_id"), context.messages());
        Optional<Integer> customColor = ValidationUtil.parseOptionalColor(context.special().potionCustomColor, ItemEditorText.str("special.potion.color"), context.messages());

        Registry<MobEffect> effectRegistry = context.registryAccess().lookupOrThrow(Registries.MOB_EFFECT);
        List<MobEffectInstance> effects = this.parsePotionEffectInstances(
                context.special().potionEffects,
                effectRegistry,
                context.messages()
        );

        if (potionHolder.isEmpty() && customColor.isEmpty() && effects.isEmpty() && context.special().potionCustomName.isBlank()) {
            this.clearToPrototype(context.previewStack(), DataComponents.POTION_CONTENTS);
            return;
        }

        context.previewStack().set(DataComponents.POTION_CONTENTS, new PotionContents(
                potionHolder,
                customColor,
                effects,
                context.special().potionCustomName.isBlank() ? Optional.empty() : Optional.of(context.special().potionCustomName)
        ));
    }

    private void applyPotionDurationScale(SpecialDataApplyContext context) {
        if (Objects.equals(context.special().potionDurationScale, context.baselineSpecial().potionDurationScale)) {
            this.restoreOriginalComponent(
                    context.originalStack(),
                    context.previewStack(),
                    DataComponents.POTION_DURATION_SCALE
            );
            return;
        }

        if (context.special().potionDurationScale.isBlank()) {
            this.clearToPrototype(context.previewStack(), DataComponents.POTION_DURATION_SCALE);
            return;
        }

        Float scale = ValidationUtil.parseFloat(
                context.special().potionDurationScale,
                ItemEditorText.str("special.potion.duration_scale"),
                context.messages()
        );
        if (scale == null) {
            return;
        }
        if (!Float.isFinite(scale) || scale <= 0.0F) {
            context.messages().add(ValidationMessage.error(ItemEditorText.str(
                    "validation.positive_decimal",
                    ItemEditorText.str("special.potion.duration_scale")
            )));
            return;
        }
        context.previewStack().set(DataComponents.POTION_DURATION_SCALE, scale);
    }

    private boolean samePotionContents(ItemEditorState state, ItemEditorState baselineState) {
        return Objects.equals(state.special.potionId, baselineState.special.potionId)
                && Objects.equals(state.special.potionCustomColor, baselineState.special.potionCustomColor)
                && Objects.equals(state.special.potionCustomName, baselineState.special.potionCustomName)
                && this.sameList(state.special.potionEffects, baselineState.special.potionEffects,
                        (left, right) -> Objects.equals(left.effectId, right.effectId)
                                && Objects.equals(left.duration, right.duration)
                                && Objects.equals(left.amplifier, right.amplifier)
                                && left.ambient == right.ambient
                                && Objects.equals(left.visible, right.visible)
                                && Objects.equals(left.showIcon, right.showIcon));
    }
}
