package me.noramibu.itemeditor.ui.panel.specialdata;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.UIComponent;
import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.ui.component.PickerFieldFactory;
import me.noramibu.itemeditor.ui.component.UiFactory;
import me.noramibu.itemeditor.util.ItemEditorText;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EquipmentSlot;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import static me.noramibu.itemeditor.ui.panel.specialdata.AdvancedItemSpecialDataSection.compactCheckboxRow;
import static me.noramibu.itemeditor.ui.panel.specialdata.AdvancedItemSpecialDataSection.compactField;
import static me.noramibu.itemeditor.ui.panel.specialdata.AdvancedItemSpecialDataSection.compactIdField;
import static me.noramibu.itemeditor.ui.panel.specialdata.AdvancedItemSpecialDataSection.compactIdTextWidth;
import static me.noramibu.itemeditor.ui.panel.specialdata.AdvancedItemSpecialDataSection.compactLongFieldWidth;
import static me.noramibu.itemeditor.ui.panel.specialdata.AdvancedItemSpecialDataSection.compactPickerButtonWidth;
import static me.noramibu.itemeditor.ui.panel.specialdata.AdvancedItemSpecialDataSection.compactTriStateBooleanPicker;
import static me.noramibu.itemeditor.ui.panel.specialdata.AdvancedItemSpecialDataSection.collapsibleCard;
import static me.noramibu.itemeditor.ui.panel.specialdata.AdvancedItemSpecialDataSection.denseEquipmentRow;
import static me.noramibu.itemeditor.ui.panel.specialdata.AdvancedItemSpecialDataSection.filledTextBox;

public final class EquippableSpecialDataSection {

    private EquippableSpecialDataSection() {
    }

    public static FlowLayout build(SpecialDataPanelContext context) {
        ItemEditorState.SpecialData special = context.special();
        return collapsibleCard(
                context,
                ItemEditorText.tr("special.advanced.component_tweaks.equippable_title"),
                special.uiEquippableCollapsed,
                value -> special.uiEquippableCollapsed = value,
                () -> buildCard(context, special)
        );
    }

    private static FlowLayout buildCard(
            SpecialDataPanelContext context,
            ItemEditorState.SpecialData special
    ) {
        FlowLayout card = UiFactory.subCard();
        int pickerWidth = compactPickerButtonWidth();
        int idWidth = compactIdTextWidth();

        ButtonComponent slotButton = UiFactory.button(
                PickerFieldFactory.selectedOrFallback(
                        special.equippableSlot,
                        ItemEditorText.tr("special.advanced.select")
                ),
                UiFactory.ButtonTextPreset.STANDARD,
                anchor -> context.openClearableDropdown(
                        anchor,
                        ItemEditorText.tr("common.none"),
                        () -> context.mutate(() -> special.equippableSlot = ""),
                        Arrays.asList(EquipmentSlot.values()),
                        EquipmentSlot::name,
                        slot -> context.mutate(() -> special.equippableSlot = slot.name())
                )
        );
        slotButton.horizontalSizing(Sizing.fill(100));
        card.child(compactField(
                ItemEditorText.tr("special.advanced.component_tweaks.equippable_slot"),
                slotButton,
                pickerWidth + 40
        ));

        List<String> sounds = context.optionalRegistryIds(Registries.SOUND_EVENT);
        card.child(compactIdField(
                context,
                ItemEditorText.tr("special.advanced.component_tweaks.equippable_sound"),
                special.equippableEquipSoundId,
                value -> special.equippableEquipSoundId = value,
                sounds,
                ItemEditorText.str("special.advanced.component_tweaks.equippable_sound"),
                idWidth
        ));
        card.child(compactIdField(
                context,
                ItemEditorText.tr("special.advanced.component_tweaks.equippable_shearing_sound"),
                special.equippableShearingSoundId,
                value -> special.equippableShearingSoundId = value,
                sounds,
                ItemEditorText.str("special.advanced.component_tweaks.equippable_shearing_sound"),
                idWidth
        ));
        card.child(compactField(
                ItemEditorText.tr("special.advanced.component_tweaks.equippable_asset_id"),
                filledTextBox(context, special.equippableAssetId, value -> special.equippableAssetId = value),
                compactLongFieldWidth() + 40
        ));
        card.child(compactField(
                ItemEditorText.tr("special.advanced.component_tweaks.equippable_camera_overlay"),
                filledTextBox(
                        context,
                        special.equippableCameraOverlayId,
                        value -> special.equippableCameraOverlayId = value
                ),
                compactLongFieldWidth() + 40
        ));

        UIComponent dispensable = equippableTriStateBooleanPicker(
                context,
                "dispensable",
                special.equippableDispensable,
                value -> special.equippableDispensable = value
        );
        UIComponent swappable = equippableTriStateBooleanPicker(
                context,
                "swappable",
                special.equippableSwappable,
                value -> special.equippableSwappable = value
        );
        UIComponent damageOnHurt = equippableTriStateBooleanPicker(
                context,
                "damage_on_hurt",
                special.equippableDamageOnHurt,
                value -> special.equippableDamageOnHurt = value
        );
        UIComponent equipOnInteract = equippableCheckbox(
                context,
                "equip_on_interact",
                special.equippableEquipOnInteract,
                value -> special.equippableEquipOnInteract = value
        );
        UIComponent canBeSheared = equippableCheckbox(
                context,
                "can_be_sheared",
                special.equippableCanBeSheared,
                value -> special.equippableCanBeSheared = value
        );
        card.child(denseEquipmentRow(dispensable, swappable, damageOnHurt));
        card.child(compactCheckboxRow(equipOnInteract, canBeSheared));
        return card;
    }

    private static UIComponent equippableTriStateBooleanPicker(
            SpecialDataPanelContext context,
            String labelSuffix,
            String value,
            Consumer<String> setter
    ) {
        return compactTriStateBooleanPicker(
                context,
                ItemEditorText.tr("special.advanced.component_tweaks.equippable_" + labelSuffix),
                value,
                setter,
                compactPickerButtonWidth()
        );
    }

    private static UIComponent equippableCheckbox(
            SpecialDataPanelContext context,
            String labelSuffix,
            boolean selected,
            Consumer<Boolean> setter
    ) {
        return UiFactory.checkbox(
                ItemEditorText.tr("special.advanced.component_tweaks.equippable_" + labelSuffix),
                selected,
                context.bindToggle(setter)
        );
    }
}
