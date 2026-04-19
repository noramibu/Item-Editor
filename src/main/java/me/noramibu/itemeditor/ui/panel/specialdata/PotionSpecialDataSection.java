package me.noramibu.itemeditor.ui.panel.specialdata;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.LabelComponent;
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
    private static final double COMPACT_LAYOUT_SCALE_THRESHOLD = 3.0d;
    private static final int COMPACT_LAYOUT_WIDTH_THRESHOLD = 560;
    private static final int EFFECT_PICKER_WIDTH = 220;
    private static final int EFFECT_NUMERIC_FIELD_WIDTH = 90;
    private static final int EFFECT_ACTION_BUTTON_WIDTH = 116;

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
        boolean compactLayout = isCompactLayout(context);

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

        FlowLayout row = compactLayout ? UiFactory.column() : UiFactory.row();
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
                UiFactory.textBox(special.potionCustomName, context.bindText(value -> special.potionCustomName = value)).horizontalSizing(Sizing.fill(100))
        ));
        section.child(row);

        ButtonComponent addEffectButton = UiFactory.button(
                ItemEditorText.tr("special.potion.add_effect"),
                UiFactory.ButtonTextPreset.STANDARD,
                button -> context.mutateRefresh(() -> special.potionEffects.add(new ItemEditorState.PotionEffectDraft()))
        );
        addEffectButton.horizontalSizing(compactLayout ? Sizing.fill(100) : UiFactory.fixed(EFFECT_ACTION_BUTTON_WIDTH));
        section.child(addEffectButton);

        for (int index = 0; index < special.potionEffects.size(); index++) {
            int currentIndex = index;
            ItemEditorState.PotionEffectDraft draft = special.potionEffects.get(currentIndex);
            FlowLayout card = createPotionEffectCard(
                    context,
                    ItemEditorText.tr("special.potion.effect", index + 1),
                    compactLayout,
                    () -> special.potionEffects.remove(currentIndex)
            );

            FlowLayout inputs = compactLayout ? UiFactory.column() : UiFactory.row();
            inputs.child(PickerFieldFactory.searchableField(
                    context,
                    ItemEditorText.tr("special.potion.effect_id"),
                    Component.empty(),
                    PickerFieldFactory.selectedOrFallback(draft.effectId, ItemEditorText.tr("special.potion.select_effect")),
                    compactLayout ? -1 : EFFECT_PICKER_WIDTH,
                    ItemEditorText.str("special.potion.effect_id"),
                    "",
                    effectIds,
                    id -> id,
                    id -> context.mutateRefresh(() -> draft.effectId = id)
            ));
            inputs.child(UiFactory.field(
                    ItemEditorText.tr("special.potion.duration"),
                    Component.empty(),
                    UiFactory.textBox(draft.duration, context.bindText(value -> draft.duration = value)).horizontalSizing(compactLayout ? Sizing.fill(100) : UiFactory.fixed(EFFECT_NUMERIC_FIELD_WIDTH))
            ));
            inputs.child(UiFactory.field(
                    ItemEditorText.tr("special.potion.amplifier"),
                    Component.empty(),
                    UiFactory.textBox(draft.amplifier, context.bindText(value -> draft.amplifier = value)).horizontalSizing(compactLayout ? Sizing.fill(100) : UiFactory.fixed(EFFECT_NUMERIC_FIELD_WIDTH))
            ));
            card.child(inputs);

            FlowLayout toggles = compactLayout ? UiFactory.column() : UiFactory.row();
            toggles.child(UiFactory.checkbox(ItemEditorText.tr("special.potion.ambient"), draft.ambient, context.bindToggle(value -> draft.ambient = value)));
            toggles.child(UiFactory.checkbox(ItemEditorText.tr("special.potion.visible"), draft.visible, context.bindToggle(value -> draft.visible = value)));
            toggles.child(UiFactory.checkbox(ItemEditorText.tr("special.potion.show_icon"), draft.showIcon, context.bindToggle(value -> draft.showIcon = value)));
            card.child(toggles);
            section.child(card);
        }

        return section;
    }

    private static FlowLayout createPotionEffectCard(
            SpecialDataPanelContext context,
            Component title,
            boolean compactLayout,
            Runnable removeAction
    ) {
        FlowLayout card = UiFactory.subCard();
        FlowLayout header = compactLayout ? UiFactory.column() : UiFactory.row();

        LabelComponent titleLabel = UiFactory.title(title).shadow(false);
        if (!compactLayout) {
            titleLabel.horizontalSizing(Sizing.expand(100));
        }
        header.child(titleLabel);

        ButtonComponent removeButton = UiFactory.button(
                ItemEditorText.tr("common.remove"),
                UiFactory.ButtonTextPreset.STANDARD,
                button -> context.mutateRefresh(removeAction)
        );
        removeButton.horizontalSizing(compactLayout ? Sizing.fill(100) : UiFactory.fixed(EFFECT_ACTION_BUTTON_WIDTH));
        header.child(removeButton);

        card.child(header);
        return card;
    }

    private static boolean isCompactLayout(SpecialDataPanelContext context) {
        return context.guiScale() >= COMPACT_LAYOUT_SCALE_THRESHOLD
                || context.panelWidthHint() < UiFactory.scaledPixels(COMPACT_LAYOUT_WIDTH_THRESHOLD);
    }
}
