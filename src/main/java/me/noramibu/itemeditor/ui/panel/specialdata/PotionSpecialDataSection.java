package me.noramibu.itemeditor.ui.panel.specialdata;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Sizing;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import me.noramibu.itemeditor.editor.EditorCategory;
import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.ui.component.EditorSearchDialog;
import me.noramibu.itemeditor.ui.component.PickerFieldFactory;
import me.noramibu.itemeditor.ui.component.UiFactory;
import me.noramibu.itemeditor.ui.util.TriStateBooleanUi;
import me.noramibu.itemeditor.util.ItemEditorText;
import me.noramibu.itemeditor.util.RegistryUtil;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;

public final class PotionSpecialDataSection {
    public static List<EditorSearchDialog.Target> searchTargets(SpecialDataPanelContext context) {
        var result = new ArrayList<>(SpecialDataSearch.targets(
                context, EditorCategory.SPECIAL_DATA, "special.potion.title", "potion", () -> {}, Field.values()));
        result.addAll(effectSearchTargets(
                context,
                context.special().potionEffects,
                EditorCategory.SPECIAL_DATA,
                List.of(ItemEditorText.str("special.potion.title")),
                () -> {}));
        return result;
    }

    public static List<EditorSearchDialog.Target> effectSearchTargets(
            SpecialDataPanelContext context,
            List<ItemEditorState.PotionEffectDraft> effects,
            EditorCategory category,
            List<String> path,
            Runnable expand) {
        var result = new ArrayList<EditorSearchDialog.Target>();
        for (int index = 0; index < effects.size(); index++) {
            var draft = effects.get(index);
            var entryPath = new ArrayList<>(path);
            entryPath.add(ItemEditorText.str("special.potion.effect", index + 1));
            String scope = SpecialDataSearch.scope("potion-effect", draft);
            Runnable reveal = () -> {
                expand.run();
                draft.uiCollapsed = false;
            };
            result.addAll(SpecialDataSearch.targets(context, category, entryPath, scope, reveal, EffectField.values()));
            result.addAll(SpecialDataSearch.targets(
                    context, category, entryPath, scope, reveal, EffectFieldLayoutUtil.Field.values()));
        }
        return result;
    }

    private static final int COMPACT_LAYOUT_WIDTH_THRESHOLD = 560;
    private static final int EFFECT_ACTION_BUTTON_WIDTH = 116;

    private PotionSpecialDataSection() {}

    public static boolean supports(ItemStack stack) {
        return stack.has(DataComponents.POTION_CONTENTS)
                || stack.has(DataComponents.POTION_DURATION_SCALE)
                || stack.is(Items.POTION)
                || stack.is(Items.SPLASH_POTION)
                || stack.is(Items.LINGERING_POTION)
                || stack.is(Items.ARROW)
                || stack.is(Items.TIPPED_ARROW);
    }

    public static FlowLayout build(SpecialDataPanelContext context) {
        ItemEditorState.SpecialData special = context.special();
        Registry<Potion> potionRegistry =
                context.screen().session().registryAccess().lookupOrThrow(Registries.POTION);
        Registry<MobEffect> effectRegistry =
                context.screen().session().registryAccess().lookupOrThrow(Registries.MOB_EFFECT);
        List<String> potionIds = RegistryUtil.ids(potionRegistry);
        List<String> effectIds = RegistryUtil.ids(effectRegistry);
        boolean compactLayout = context.isCompactPanel(COMPACT_LAYOUT_WIDTH_THRESHOLD);

        FlowLayout section = UiFactory.section(ItemEditorText.tr("special.potion.title"), Component.empty());
        section.id("potion");

        section.child(PickerFieldFactory.searchableField(
                context,
                Field.POTION_ID.text(),
                Component.empty(),
                PickerFieldFactory.selectedOrFallback(
                        special.potionId, ItemEditorText.tr("special.potion.select_potion")),
                -1,
                Field.POTION_ID.text().getString(),
                "",
                potionIds,
                id -> id,
                id -> context.mutateRefresh(() -> special.potionId = id)));

        section.child(UiFactory.field(
                Field.DURATION_SCALE.text(),
                Component.empty(),
                UiFactory.textBox(
                                special.potionDurationScale,
                                context.bindText(value -> special.potionDurationScale = value))
                        .horizontalSizing(Sizing.fill(100))));

        FlowLayout row = compactLayout ? UiFactory.column() : UiFactory.row();
        row.child(UiFactory.field(
                Field.COLOR.text(),
                Component.empty(),
                context.colorInputWithPicker(
                                special.potionCustomColor,
                                value -> special.potionCustomColor = value,
                                () -> special.potionCustomColor,
                                Field.COLOR.text().getString(),
                                0xF9801D)
                        .horizontalSizing(Sizing.fill(100))));
        row.child(UiFactory.field(
                Field.CUSTOM_NAME.text(),
                Component.empty(),
                UiFactory.textBox(special.potionCustomName, context.bindText(value -> special.potionCustomName = value))
                        .horizontalSizing(Sizing.fill(100))));
        section.child(row);

        section.child(buildEffectsEditor(context, special.potionEffects, effectIds, Field.ADD_EFFECT.text()));
        return section;
    }

    static FlowLayout buildEffectsEditor(
            SpecialDataPanelContext context,
            List<ItemEditorState.PotionEffectDraft> effects,
            List<String> effectIds,
            Component addLabel) {
        boolean compactLayout = context.isCompactPanel(COMPACT_LAYOUT_WIDTH_THRESHOLD);
        FlowLayout editor = UiFactory.column();
        ButtonComponent addEffectButton = UiFactory.button(
                addLabel,
                UiFactory.ButtonTextPreset.STANDARD,
                button -> context.mutateRefresh(() -> effects.add(new ItemEditorState.PotionEffectDraft())));
        addEffectButton.horizontalSizing(
                compactLayout ? Sizing.fill(100) : UiFactory.fixed(EFFECT_ACTION_BUTTON_WIDTH));
        editor.child(addEffectButton);

        for (int index = 0; index < effects.size(); index++) {
            int currentIndex = index;
            ItemEditorState.PotionEffectDraft draft = effects.get(currentIndex);
            FlowLayout card = createPotionEffectCard(
                    context,
                    effectTitle(draft, index),
                    compactLayout,
                    draft.uiCollapsed,
                    () -> context.mutateRefresh(() -> draft.uiCollapsed = !draft.uiCollapsed),
                    () -> effects.remove(currentIndex));
            card.id(SpecialDataSearch.scope("potion-effect", draft));
            if (draft.uiCollapsed) {
                editor.child(card);
                continue;
            }

            card.child(EffectFieldLayoutUtil.buildEffectFields(
                    context,
                    effectIds,
                    draft.effectId,
                    id -> context.mutateRefresh(() -> draft.effectId = id),
                    draft.duration,
                    context.bindText(value -> draft.duration = value),
                    draft.amplifier,
                    context.bindText(value -> draft.amplifier = value)));

            FlowLayout toggles = compactLayout ? UiFactory.column() : UiFactory.row();
            toggles.child(UiFactory.checkbox(
                    EffectField.AMBIENT.text(), draft.ambient, context.bindToggle(value -> draft.ambient = value)));
            FlowLayout visibility = UiFactory.actionButtonRow(
                    triStateBooleanButton(
                            context, EffectField.VISIBLE.text(), draft.visible, value -> draft.visible = value),
                    triStateBooleanButton(
                            context, EffectField.SHOW_ICON.text(), draft.showIcon, value -> draft.showIcon = value));
            visibility.horizontalSizing(compactLayout ? Sizing.fill(100) : Sizing.expand(100));
            toggles.child(visibility);
            card.child(toggles);
            editor.child(card);
        }
        return editor;
    }

    private static FlowLayout createPotionEffectCard(
            SpecialDataPanelContext context,
            Component title,
            boolean compactLayout,
            boolean collapsed,
            Runnable toggleAction,
            Runnable removeAction) {
        FlowLayout card = UiFactory.subCard();
        FlowLayout header = UiFactory.row();
        LabelComponent titleLabel = UiFactory.title(title).shadow(false);
        titleLabel.tooltip(List.of(title));
        header.child(titleLabel.horizontalSizing(Sizing.expand(100)));
        header.child(UiFactory.collapseToggleButton(collapsed, toggleAction));

        ButtonComponent removeButton = UiFactory.button(
                EffectField.REMOVE.text(),
                UiFactory.ButtonTextPreset.STANDARD,
                button -> context.mutateRefresh(removeAction));
        removeButton.horizontalSizing(compactLayout ? Sizing.fill(100) : UiFactory.fixed(EFFECT_ACTION_BUTTON_WIDTH));
        card.child(header);
        if (compactLayout) {
            card.child(removeButton);
        } else {
            header.child(removeButton);
        }
        return card;
    }

    private static ButtonComponent triStateBooleanButton(
            SpecialDataPanelContext context, Component label, String value, Consumer<String> setter) {
        Component text = label.copy().append(": ").append(TriStateBooleanUi.label(value));
        ButtonComponent button = UiFactory.actionToneButton(
                text,
                UiFactory.ButtonTextPreset.STANDARD,
                TriStateBooleanUi.tone(value),
                anchor -> context.mutateRefresh(() -> setter.accept(TriStateBooleanUi.next(value))));
        button.tooltip(List.of(text));
        return button;
    }

    private static Component effectTitle(ItemEditorState.PotionEffectDraft draft, int index) {
        Component title = ItemEditorText.tr("special.potion.effect", index + 1);
        return draft.effectId == null || draft.effectId.isBlank()
                ? title
                : title.copy().append(" | ").append(draft.effectId);
    }

    private enum Field implements SpecialDataSearch.Field {
        POTION_ID("special.potion.potion_id"),
        DURATION_SCALE("special.potion.duration_scale"),
        COLOR("special.potion.color"),
        CUSTOM_NAME("common.custom_name"),
        ADD_EFFECT("special.potion.add_effect");

        private final String key;

        Field(String key) {
            this.key = key;
        }

        public String key() {
            return key;
        }
    }

    private enum EffectField implements SpecialDataSearch.Field {
        AMBIENT("special.potion.ambient"),
        VISIBLE("special.potion.visible"),
        SHOW_ICON("special.potion.show_icon"),
        REMOVE("common.remove");

        private final String key;

        EffectField(String key) {
            this.key = key;
        }

        public String key() {
            return key;
        }
    }
}
