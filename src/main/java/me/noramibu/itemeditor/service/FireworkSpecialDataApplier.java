package me.noramibu.itemeditor.service;

import it.unimi.dsi.fastutil.ints.IntList;
import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.editor.ValidationMessage;
import me.noramibu.itemeditor.util.ItemEditorText;
import me.noramibu.itemeditor.util.ValidationUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

final class FireworkSpecialDataApplier extends AbstractPreviewApplierSupport implements SpecialDataApplier {

    @Override
    public void apply(SpecialDataApplyContext context) {
        if (this.sameRocketData(context.state(), context.baselineState())) {
            this.restoreOriginalComponent(context.originalStack(), context.previewStack(), DataComponents.FIREWORKS);
        } else if (context.previewStack().is(Items.FIREWORK_ROCKET) || !context.special().rocketExplosions.isEmpty()) {
            Integer flight = ValidationUtil.parseInt(context.special().fireworkFlightDuration, ItemEditorText.str("special.firework.flight_duration"), 0, 255, context.messages());
            List<FireworkExplosion> explosions = new ArrayList<>();
            for (ItemEditorState.FireworkExplosionDraft draft : context.special().rocketExplosions) {
                FireworkExplosion explosion = this.buildExplosion(draft, context.messages());
                if (explosion != null) {
                    explosions.add(explosion);
                }
            }

            if (flight != null || !explosions.isEmpty()) {
                context.previewStack().set(DataComponents.FIREWORKS, new Fireworks(flight == null ? 0 : flight, explosions));
            } else {
                this.clearToPrototype(context.previewStack(), DataComponents.FIREWORKS);
            }
        }

        if (this.sameStarData(context.state(), context.baselineState())) {
            this.restoreOriginalComponent(context.originalStack(), context.previewStack(), DataComponents.FIREWORK_EXPLOSION);
            return;
        }

        if (context.previewStack().is(Items.FIREWORK_STAR)) {
            FireworkExplosion explosion = this.buildExplosion(context.special().starExplosion, context.messages());
            if (explosion != null) {
                context.previewStack().set(DataComponents.FIREWORK_EXPLOSION, explosion);
            } else {
                this.clearToPrototype(context.previewStack(), DataComponents.FIREWORK_EXPLOSION);
            }
        }
    }

    private FireworkExplosion buildExplosion(ItemEditorState.FireworkExplosionDraft draft, List<ValidationMessage> messages) {
        if (draft.colors.isBlank()) {
            return null;
        }

        FireworkExplosion.Shape shape;
        try {
            shape = FireworkExplosion.Shape.valueOf(draft.shape);
        } catch (IllegalArgumentException exception) {
            messages.add(ValidationMessage.error(ItemEditorText.str(
                    "validation.registry_missing",
                    ItemEditorText.str("special.firework.shape"),
                    draft.shape
            )));
            return null;
        }

        IntList colors = ValidationUtil.parseColorList(draft.colors, ItemEditorText.str("special.firework.colors"), messages);
        IntList fadeColors = draft.fadeColors.isBlank()
                ? IntList.of()
                : ValidationUtil.parseColorList(draft.fadeColors, ItemEditorText.str("special.firework.fade_colors"), messages);
        return new FireworkExplosion(shape, colors, fadeColors, draft.trail, draft.twinkle);
    }

    private boolean sameRocketData(ItemEditorState state, ItemEditorState baselineState) {
        return Objects.equals(state.special.fireworkFlightDuration, baselineState.special.fireworkFlightDuration)
                && this.sameExplosionDrafts(state.special.rocketExplosions, baselineState.special.rocketExplosions);
    }

    private boolean sameStarData(ItemEditorState state, ItemEditorState baselineState) {
        return this.sameExplosionDraft(state.special.starExplosion, baselineState.special.starExplosion);
    }

    private boolean sameExplosionDrafts(List<ItemEditorState.FireworkExplosionDraft> current, List<ItemEditorState.FireworkExplosionDraft> baseline) {
        return this.sameList(current, baseline, this::sameExplosionDraft);
    }

    private boolean sameExplosionDraft(ItemEditorState.FireworkExplosionDraft current, ItemEditorState.FireworkExplosionDraft baseline) {
        return Objects.equals(current.shape, baseline.shape)
                && Objects.equals(current.colors, baseline.colors)
                && Objects.equals(current.fadeColors, baseline.fadeColors)
                && current.trail == baseline.trail
                && current.twinkle == baseline.twinkle;
    }
}
