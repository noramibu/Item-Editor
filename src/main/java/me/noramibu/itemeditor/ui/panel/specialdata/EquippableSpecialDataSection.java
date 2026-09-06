package me.noramibu.itemeditor.ui.panel.specialdata;

import static me.noramibu.itemeditor.ui.panel.specialdata.AdvancedItemSpecialDataSection.collapsibleCard;
import static me.noramibu.itemeditor.ui.panel.specialdata.AdvancedItemSpecialDataSection.compactCheckboxRow;
import static me.noramibu.itemeditor.ui.panel.specialdata.AdvancedItemSpecialDataSection.compactField;
import static me.noramibu.itemeditor.ui.panel.specialdata.AdvancedItemSpecialDataSection.compactIdField;
import static me.noramibu.itemeditor.ui.panel.specialdata.AdvancedItemSpecialDataSection.compactIdTextWidth;
import static me.noramibu.itemeditor.ui.panel.specialdata.AdvancedItemSpecialDataSection.compactPickerButtonWidth;
import static me.noramibu.itemeditor.ui.panel.specialdata.AdvancedItemSpecialDataSection.compactTriStateBooleanPicker;
import static me.noramibu.itemeditor.ui.panel.specialdata.AdvancedItemSpecialDataSection.denseEquipmentRow;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.UIComponent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import me.noramibu.itemeditor.editor.EditorCategory;
import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.ui.component.EditorSearchDialog;
import me.noramibu.itemeditor.ui.component.PickerFieldFactory;
import me.noramibu.itemeditor.ui.component.UiFactory;
import me.noramibu.itemeditor.util.ItemEditorText;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EquipmentSlot;

public final class EquippableSpecialDataSection {

    private enum Control implements ComponentSearchField {
        COMPONENT_TWEAKS_EQUIPPABLE_TITLE("special.advanced.component_tweaks.equippable_title"),
        COMPONENT_TWEAKS_EQUIPPABLE_SLOT("special.advanced.component_tweaks.equippable_slot"),
        COMPONENT_TWEAKS_EQUIPPABLE_SOUND("special.advanced.component_tweaks.equippable_sound"),
        COMPONENT_TWEAKS_EQUIPPABLE_SHEARING_SOUND("special.advanced.component_tweaks.equippable_shearing_sound"),
        COMPONENT_TWEAKS_EQUIPPABLE_ASSET_ID("special.advanced.component_tweaks.equippable_asset_id"),
        COMPONENT_TWEAKS_EQUIPPABLE_CAMERA_OVERLAY("special.advanced.component_tweaks.equippable_camera_overlay"),
        COMPONENT_TWEAKS_EQUIPPABLE_DISPENSABLE("special.advanced.component_tweaks.equippable_dispensable"),
        COMPONENT_TWEAKS_EQUIPPABLE_SWAPPABLE("special.advanced.component_tweaks.equippable_swappable"),
        COMPONENT_TWEAKS_EQUIPPABLE_DAMAGE_ON_HURT("special.advanced.component_tweaks.equippable_damage_on_hurt"),
        COMPONENT_TWEAKS_EQUIPPABLE_EQUIP_ON_INTERACT("special.advanced.component_tweaks.equippable_equip_on_interact"),
        COMPONENT_TWEAKS_EQUIPPABLE_CAN_BE_SHEARED("special.advanced.component_tweaks.equippable_can_be_sheared");

        private final String key;

        Control(String key) {
            this.key = key;
        }

        @Override
        public String key() {
            return key;
        }
    }

    private EquippableSpecialDataSection() {}

    public static List<EditorSearchDialog.Target> searchTargets(SpecialDataPanelContext context) {
        List<EditorSearchDialog.Target> targets = new ArrayList<>();
        for (Control field : Control.values()) {
            targets.add(field.target(
                    context,
                    EditorCategory.COMPONENTS,
                    List.of(Control.COMPONENT_TWEAKS_EQUIPPABLE_TITLE.text()),
                    () -> "",
                    () -> context.special().uiEquippableCollapsed = false));
        }
        return List.copyOf(targets);
    }

    public static FlowLayout build(SpecialDataPanelContext context) {
        ItemEditorState.SpecialData special = context.special();
        return collapsibleCard(
                context,
                Control.COMPONENT_TWEAKS_EQUIPPABLE_TITLE.label(),
                special.uiEquippableCollapsed,
                value -> special.uiEquippableCollapsed = value,
                () -> buildCard(context, special));
    }

    private static FlowLayout buildCard(SpecialDataPanelContext context, ItemEditorState.SpecialData special) {
        FlowLayout card = UiFactory.subCard();
        int pickerWidth = compactPickerButtonWidth();
        int idWidth = compactIdTextWidth();

        ButtonComponent slotButton = UiFactory.button(
                PickerFieldFactory.selectedOrFallback(
                        special.equippableSlot, ItemEditorText.tr("special.advanced.select")),
                UiFactory.ButtonTextPreset.STANDARD,
                anchor -> context.openClearableDropdown(
                        anchor,
                        ItemEditorText.tr("common.none"),
                        () -> context.mutate(() -> special.equippableSlot = ""),
                        Arrays.asList(EquipmentSlot.values()),
                        EquipmentSlot::name,
                        slot -> context.mutate(() -> special.equippableSlot = slot.name())));
        slotButton.horizontalSizing(Sizing.fill(100));
        card.child(compactField(Control.COMPONENT_TWEAKS_EQUIPPABLE_SLOT.label(), slotButton, pickerWidth + 40));

        List<String> sounds = context.optionalRegistryIds(Registries.SOUND_EVENT);
        card.child(compactIdField(
                context,
                Control.COMPONENT_TWEAKS_EQUIPPABLE_SOUND.label(),
                special.equippableEquipSoundId,
                value -> special.equippableEquipSoundId = value,
                sounds,
                Control.COMPONENT_TWEAKS_EQUIPPABLE_SOUND.text(),
                idWidth));
        card.child(compactIdField(
                context,
                Control.COMPONENT_TWEAKS_EQUIPPABLE_SHEARING_SOUND.label(),
                special.equippableShearingSoundId,
                value -> special.equippableShearingSoundId = value,
                sounds,
                Control.COMPONENT_TWEAKS_EQUIPPABLE_SHEARING_SOUND.text(),
                idWidth));
        card.child(PickerFieldFactory.searchableTextField(
                context,
                Control.COMPONENT_TWEAKS_EQUIPPABLE_ASSET_ID.label(),
                special.equippableAssetId,
                value -> special.equippableAssetId = value,
                pickerWidth,
                Control.COMPONENT_TWEAKS_EQUIPPABLE_ASSET_ID.text(),
                "",
                AdvancedItemSpecialDataSection.withCurrentId(context.equipmentAssetIds(), special.equippableAssetId),
                id -> id,
                id -> context.mutateRefresh(() -> special.equippableAssetId = id)));
        card.child(PickerFieldFactory.searchableTextField(
                context,
                Control.COMPONENT_TWEAKS_EQUIPPABLE_CAMERA_OVERLAY.label(),
                special.equippableCameraOverlayId,
                value -> special.equippableCameraOverlayId = value,
                pickerWidth,
                Control.COMPONENT_TWEAKS_EQUIPPABLE_CAMERA_OVERLAY.text(),
                "",
                AdvancedItemSpecialDataSection.withCurrentId(
                        context.cameraOverlayIds(), special.equippableCameraOverlayId),
                id -> id,
                id -> context.mutateRefresh(() -> special.equippableCameraOverlayId = id)));

        UIComponent dispensable = equippableTriStateBooleanPicker(
                context,
                Control.COMPONENT_TWEAKS_EQUIPPABLE_DISPENSABLE,
                special.equippableDispensable,
                value -> special.equippableDispensable = value);
        UIComponent swappable = equippableTriStateBooleanPicker(
                context,
                Control.COMPONENT_TWEAKS_EQUIPPABLE_SWAPPABLE,
                special.equippableSwappable,
                value -> special.equippableSwappable = value);
        UIComponent damageOnHurt = equippableTriStateBooleanPicker(
                context,
                Control.COMPONENT_TWEAKS_EQUIPPABLE_DAMAGE_ON_HURT,
                special.equippableDamageOnHurt,
                value -> special.equippableDamageOnHurt = value);
        UIComponent equipOnInteract = equippableCheckbox(
                context,
                Control.COMPONENT_TWEAKS_EQUIPPABLE_EQUIP_ON_INTERACT,
                special.equippableEquipOnInteract,
                value -> special.equippableEquipOnInteract = value);
        UIComponent canBeSheared = equippableCheckbox(
                context,
                Control.COMPONENT_TWEAKS_EQUIPPABLE_CAN_BE_SHEARED,
                special.equippableCanBeSheared,
                value -> special.equippableCanBeSheared = value);
        card.child(denseEquipmentRow(dispensable, swappable, damageOnHurt));
        card.child(compactCheckboxRow(equipOnInteract, canBeSheared));
        return card;
    }

    private static UIComponent equippableTriStateBooleanPicker(
            SpecialDataPanelContext context, Control field, String value, Consumer<String> setter) {
        return compactTriStateBooleanPicker(context, field.label(), value, setter, compactPickerButtonWidth());
    }

    private static UIComponent equippableCheckbox(
            SpecialDataPanelContext context, Control field, boolean selected, Consumer<Boolean> setter) {
        return UiFactory.checkbox(field.label(), selected, context.bindToggle(setter));
    }
}
