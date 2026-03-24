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

import java.util.List;

public final class StewSpecialDataSection {

    private StewSpecialDataSection() {
    }

    public static boolean supports(ItemStack stack) {
        return stack.has(DataComponents.SUSPICIOUS_STEW_EFFECTS) || stack.is(Items.SUSPICIOUS_STEW);
    }

    public static FlowLayout build(SpecialDataPanelContext context) {
        ItemEditorState.SpecialData special = context.special();
        Registry<MobEffect> effectRegistry = context.screen().session().registryAccess().lookupOrThrow(Registries.MOB_EFFECT);
        List<String> effectIds = RegistryUtil.ids(effectRegistry);

        FlowLayout section = UiFactory.section(ItemEditorText.tr("special.stew.title"), Component.empty());
        section.child(UiFactory.button(ItemEditorText.tr("special.stew.add_effect"), button ->
                context.mutateRefresh(() -> special.stewEffects.add(new ItemEditorState.SuspiciousStewEffectDraft()))
        ));

        for (int index = 0; index < special.stewEffects.size(); index++) {
            int currentIndex = index;
            ItemEditorState.SuspiciousStewEffectDraft draft = special.stewEffects.get(currentIndex);
            FlowLayout row = context.createRemovableCard(
                    ItemEditorText.tr("special.stew.effect", index + 1),
                    () -> special.stewEffects.remove(currentIndex)
            );

            FlowLayout fields = UiFactory.row();
            fields.child(PickerFieldFactory.searchableField(
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
            fields.child(UiFactory.field(
                    ItemEditorText.tr("special.potion.duration"),
                    Component.empty(),
                    UiFactory.textBox(draft.duration, context.bindText(value -> draft.duration = value)).horizontalSizing(Sizing.fixed(100))
            ));
            row.child(fields);
            section.child(row);
        }
        return section;
    }
}
