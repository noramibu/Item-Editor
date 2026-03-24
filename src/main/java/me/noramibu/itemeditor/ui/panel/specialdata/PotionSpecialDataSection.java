package me.noramibu.itemeditor.ui.panel.specialdata;

import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Sizing;
import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.ui.component.PickerFieldFactory;
import me.noramibu.itemeditor.ui.component.UiFactory;
import me.noramibu.itemeditor.util.ItemEditorText;
import me.noramibu.itemeditor.util.RegistryUtil;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;

import java.util.List;

public final class PotionSpecialDataSection {

    private PotionSpecialDataSection() {
    }

    public static boolean supports(ItemStack stack) {
        return stack.has(DataComponents.POTION_CONTENTS)
                || stack.is(Items.POTION)
                || stack.is(Items.SPLASH_POTION)
                || stack.is(Items.LINGERING_POTION)
                || stack.is(Items.TIPPED_ARROW);
    }

    public static FlowLayout build(SpecialDataPanelContext context) {
        ItemEditorState.SpecialData special = context.special();
        Registry<Potion> potionRegistry = context.screen().session().registryAccess().lookupOrThrow(Registries.POTION);
        Registry<MobEffect> effectRegistry = context.screen().session().registryAccess().lookupOrThrow(Registries.MOB_EFFECT);
        List<String> potionIds = RegistryUtil.ids(potionRegistry);
        List<String> effectIds = RegistryUtil.ids(effectRegistry);

        FlowLayout section = UiFactory.section(ItemEditorText.tr("special.potion.title"), Component.empty());

        section.child(PickerFieldFactory.searchableField(
                context,
                ItemEditorText.tr("special.potion.potion_id"),
                Component.empty(),
                PickerFieldFactory.selectedOrFallback(special.potionId, ItemEditorText.tr("special.potion.select_potion")),
                -1,
                ItemEditorText.str("special.potion.potion_id"),
                "",
                potionIds,
                id -> id,
                id -> context.mutateRefresh(() -> special.potionId = id)
        ));

        FlowLayout row = UiFactory.row();
        row.child(UiFactory.field(
                ItemEditorText.tr("special.potion.color"),
                Component.empty(),
                context.colorInputWithPicker(
                        special.potionCustomColor,
                        value -> special.potionCustomColor = value,
                        () -> special.potionCustomColor,
                        ItemEditorText.str("special.potion.color"),
                        0xF9801D
                ).horizontalSizing(Sizing.fill(100))
        ));
        row.child(UiFactory.field(
                ItemEditorText.tr("special.potion.custom_name"),
                Component.empty(),
                UiFactory.textBox(special.potionCustomName, context.bindText(value -> special.potionCustomName = value)).horizontalSizing(Sizing.fixed(220))
        ));
        section.child(row);

        section.child(UiFactory.button(ItemEditorText.tr("special.potion.add_effect"), button ->
                context.mutateRefresh(() -> special.potionEffects.add(new ItemEditorState.PotionEffectDraft()))
        ));

        for (int index = 0; index < special.potionEffects.size(); index++) {
            int currentIndex = index;
            ItemEditorState.PotionEffectDraft draft = special.potionEffects.get(currentIndex);
            FlowLayout card = context.createRemovableCard(
                    ItemEditorText.tr("special.potion.effect", index + 1),
                    () -> special.potionEffects.remove(currentIndex)
            );

            FlowLayout inputs = UiFactory.row();
            inputs.child(PickerFieldFactory.searchableField(
                    context,
                    ItemEditorText.tr("special.potion.effect_id"),
                    Component.empty(),
                    PickerFieldFactory.selectedOrFallback(draft.effectId, ItemEditorText.tr("special.potion.select_effect")),
                    220,
                    ItemEditorText.str("special.potion.effect_id"),
                    "",
                    effectIds,
                    id -> id,
                    id -> context.mutateRefresh(() -> draft.effectId = id)
            ));
            inputs.child(UiFactory.field(
                    ItemEditorText.tr("special.potion.duration"),
                    Component.empty(),
                    UiFactory.textBox(draft.duration, context.bindText(value -> draft.duration = value)).horizontalSizing(Sizing.fixed(90))
            ));
            inputs.child(UiFactory.field(
                    ItemEditorText.tr("special.potion.amplifier"),
                    Component.empty(),
                    UiFactory.textBox(draft.amplifier, context.bindText(value -> draft.amplifier = value)).horizontalSizing(Sizing.fixed(90))
            ));
            card.child(inputs);

            FlowLayout toggles = UiFactory.row();
            toggles.child(UiFactory.checkbox(ItemEditorText.tr("special.potion.ambient"), draft.ambient, context.bindToggle(value -> draft.ambient = value)));
            toggles.child(UiFactory.checkbox(ItemEditorText.tr("special.potion.visible"), draft.visible, context.bindToggle(value -> draft.visible = value)));
            toggles.child(UiFactory.checkbox(ItemEditorText.tr("special.potion.show_icon"), draft.showIcon, context.bindToggle(value -> draft.showIcon = value)));
            card.child(toggles);
            section.child(card);
        }

        return section;
    }
}
