package me.noramibu.itemeditor.ui.panel.specialdata;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Sizing;
import java.util.ArrayList;
import java.util.List;
import me.noramibu.itemeditor.editor.EditorCategory;
import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.ui.component.EditorSearchDialog;
import me.noramibu.itemeditor.ui.component.UiFactory;
import me.noramibu.itemeditor.util.ItemEditorText;
import me.noramibu.itemeditor.util.RegistryUtil;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class StewSpecialDataSection {
    public static List<EditorSearchDialog.Target> searchTargets(SpecialDataPanelContext context) {
        var result = new ArrayList<>(SpecialDataSearch.targets(
                context, EditorCategory.SPECIAL_DATA, "special.stew.title", "stew", () -> {}, Field.values()));
        for (int index = 0; index < context.special().stewEffects.size(); index++) {
            var draft = context.special().stewEffects.get(index);
            result.addAll(SpecialDataSearch.targets(
                    context,
                    EditorCategory.SPECIAL_DATA,
                    List.of(
                            ItemEditorText.str("special.stew.title"),
                            ItemEditorText.str("special.stew.effect", index + 1)),
                    SpecialDataSearch.scope("stew-effect", draft),
                    () -> {},
                    EffectFieldLayoutUtil.Field.EFFECT_ID,
                    EffectFieldLayoutUtil.Field.DURATION));
        }
        return result;
    }

    private StewSpecialDataSection() {}

    public static boolean supports(ItemStack stack) {
        return stack.has(DataComponents.SUSPICIOUS_STEW_EFFECTS) || stack.is(Items.SUSPICIOUS_STEW);
    }

    public static FlowLayout build(SpecialDataPanelContext context) {
        ItemEditorState.SpecialData special = context.special();
        Registry<MobEffect> effectRegistry =
                context.screen().session().registryAccess().lookupOrThrow(Registries.MOB_EFFECT);
        List<String> effectIds = RegistryUtil.ids(effectRegistry);

        FlowLayout section = UiFactory.section(ItemEditorText.tr("special.stew.title"), Component.empty());
        section.id("stew");
        section.child(addStewEffectButton(context, special));

        for (int index = 0; index < special.stewEffects.size(); index++) {
            int currentIndex = index;
            ItemEditorState.SuspiciousStewEffectDraft draft = special.stewEffects.get(currentIndex);
            FlowLayout row = context.createRemovableCard(
                    ItemEditorText.tr("special.stew.effect", index + 1),
                    () -> special.stewEffects.remove(currentIndex));

            row.id(SpecialDataSearch.scope("stew-effect", draft));
            row.child(EffectFieldLayoutUtil.buildEffectFields(
                    context,
                    effectIds,
                    draft.effectId,
                    id -> context.mutateRefresh(() -> draft.effectId = id),
                    draft.duration,
                    context.bindText(value -> draft.duration = value),
                    null,
                    null));
            section.child(row);
        }
        return section;
    }

    private static ButtonComponent addStewEffectButton(
            SpecialDataPanelContext context, ItemEditorState.SpecialData special) {
        Component label = Field.ADD_EFFECT.text();
        ButtonComponent button = UiFactory.positiveButton(
                label,
                UiFactory.ButtonTextPreset.STANDARD,
                ignored -> context.mutateRefresh(
                        () -> special.stewEffects.add(new ItemEditorState.SuspiciousStewEffectDraft())));
        button.tooltip(List.of(label));
        button.horizontalSizing(Sizing.fill(100));
        return button;
    }

    private enum Field implements SpecialDataSearch.Field {
        ADD_EFFECT("special.stew.add_effect");

        private final String key;

        Field(String key) {
            this.key = key;
        }

        public String key() {
            return key;
        }
    }
}
