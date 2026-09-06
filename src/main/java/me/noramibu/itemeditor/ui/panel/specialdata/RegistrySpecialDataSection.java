package me.noramibu.itemeditor.ui.panel.specialdata;

import static me.noramibu.itemeditor.ui.panel.specialdata.AdvancedItemSpecialDataSection.collapsibleCard;
import static me.noramibu.itemeditor.ui.panel.specialdata.AdvancedItemSpecialDataSection.compactIdField;
import static me.noramibu.itemeditor.ui.panel.specialdata.AdvancedItemSpecialDataSection.compactIdTextWidth;
import static me.noramibu.itemeditor.ui.panel.specialdata.AdvancedItemSpecialDataSection.compactLongFieldWidth;
import static me.noramibu.itemeditor.ui.panel.specialdata.AdvancedItemSpecialDataSection.distributeRowChildren;
import static me.noramibu.itemeditor.ui.panel.specialdata.AdvancedItemSpecialDataSection.jukeboxSongIds;
import static me.noramibu.itemeditor.ui.panel.specialdata.AdvancedItemSpecialDataSection.responsiveRow;
import static me.noramibu.itemeditor.ui.panel.specialdata.AdvancedItemSpecialDataSection.withCurrentId;

import io.wispforest.owo.ui.container.FlowLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import me.noramibu.itemeditor.editor.EditorCategory;
import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.ui.component.EditorSearchDialog;
import me.noramibu.itemeditor.ui.component.UiFactory;
import me.noramibu.itemeditor.util.ItemEditorText;
import net.minecraft.core.registries.Registries;

public final class RegistrySpecialDataSection {

    private enum Control implements ComponentSearchField {
        COMPONENT_TWEAKS_REGISTRY_TITLE("special.advanced.component_tweaks.registry_title"),
        COMPONENT_TWEAKS_DAMAGE_TYPE("special.advanced.component_tweaks.damage_type"),
        COMPONENT_TWEAKS_NOTE_BLOCK_SOUND("special.advanced.component_tweaks.note_block_sound"),
        COMPONENT_TWEAKS_JUKEBOX_PLAYABLE("special.advanced.component_tweaks.jukebox_playable"),
        COMPONENT_TWEAKS_PROVIDES_BANNER_PATTERNS("special.advanced.component_tweaks.provides_banner_patterns"),
        COMPONENT_TWEAKS_BREAK_SOUND("special.advanced.component_tweaks.break_sound"),
        COMPONENT_TWEAKS_PROVIDES_TRIM_MATERIAL("special.advanced.component_tweaks.provides_trim_material"),
        COMPONENT_TWEAKS_PAINTING_VARIANT("special.advanced.component_tweaks.painting_variant");

        private final String key;

        Control(String key) {
            this.key = key;
        }

        @Override
        public String key() {
            return key;
        }
    }

    private RegistrySpecialDataSection() {}

    public static List<EditorSearchDialog.Target> searchTargets(SpecialDataPanelContext context) {
        List<EditorSearchDialog.Target> targets = new ArrayList<>();
        for (Control field : Control.values()) {
            targets.add(field.target(
                    context,
                    EditorCategory.COMPONENTS,
                    List.of(Control.COMPONENT_TWEAKS_REGISTRY_TITLE.text()),
                    () -> "",
                    () -> context.special().uiComponentTweaksRegistryCollapsed = false));
        }
        return List.copyOf(targets);
    }

    public static FlowLayout build(SpecialDataPanelContext context) {
        ItemEditorState.SpecialData special = context.special();
        return collapsibleCard(
                context,
                Control.COMPONENT_TWEAKS_REGISTRY_TITLE.label(),
                special.uiComponentTweaksRegistryCollapsed,
                value -> special.uiComponentTweaksRegistryCollapsed = value,
                () -> UiFactory.column().child(buildRegistryAndFlagsCard(context, special)));
    }

    private static FlowLayout buildRegistryAndFlagsCard(
            SpecialDataPanelContext context, ItemEditorState.SpecialData special) {
        FlowLayout card = UiFactory.subCard();
        int idWidth = compactIdTextWidth();
        int longWidth = compactLongFieldWidth();

        FlowLayout damageTypeField = registryIdField(
                context,
                Control.COMPONENT_TWEAKS_DAMAGE_TYPE,
                special.damageTypeId,
                value -> special.damageTypeId = value,
                context.optionalRegistryIds(Registries.DAMAGE_TYPE),
                idWidth);
        card.child(damageTypeField);

        FlowLayout soundRow = responsiveRow();
        FlowLayout noteBlockSoundField = registryIdField(
                context,
                Control.COMPONENT_TWEAKS_NOTE_BLOCK_SOUND,
                special.noteBlockSoundId,
                value -> special.noteBlockSoundId = value,
                context.optionalRegistryIds(Registries.SOUND_EVENT),
                idWidth);
        FlowLayout jukeboxPlayableField = registryIdField(
                context,
                Control.COMPONENT_TWEAKS_JUKEBOX_PLAYABLE,
                special.jukeboxSongId,
                value -> special.jukeboxSongId = value,
                jukeboxSongIds(context, special.jukeboxSongId),
                idWidth);
        FlowLayout providesBannerPatternsField = registryIdField(
                context,
                Control.COMPONENT_TWEAKS_PROVIDES_BANNER_PATTERNS,
                special.providesBannerPatternsTagId,
                value -> special.providesBannerPatternsTagId = value,
                withCurrentId(
                        context.registryTagIds(Registries.BANNER_PATTERN, "#"), special.providesBannerPatternsTagId),
                longWidth);
        FlowLayout breakSoundField = registryIdField(
                context,
                Control.COMPONENT_TWEAKS_BREAK_SOUND,
                special.breakSoundId,
                value -> special.breakSoundId = value,
                context.optionalRegistryIds(Registries.SOUND_EVENT),
                idWidth);
        distributeRowChildren(soundRow, noteBlockSoundField, jukeboxPlayableField, breakSoundField);
        card.child(soundRow);

        FlowLayout variantRow = responsiveRow();
        FlowLayout providesTrimMaterialField = registryIdField(
                context,
                Control.COMPONENT_TWEAKS_PROVIDES_TRIM_MATERIAL,
                special.providesTrimMaterialId,
                value -> special.providesTrimMaterialId = value,
                context.optionalRegistryIds(Registries.TRIM_MATERIAL),
                longWidth);
        FlowLayout paintingVariantField = registryIdField(
                context,
                Control.COMPONENT_TWEAKS_PAINTING_VARIANT,
                special.paintingVariantId,
                value -> special.paintingVariantId = value,
                context.optionalRegistryIds(Registries.PAINTING_VARIANT),
                idWidth);
        distributeRowChildren(variantRow, providesTrimMaterialField, providesBannerPatternsField, paintingVariantField);
        card.child(variantRow);
        return card;
    }

    private static FlowLayout registryIdField(
            SpecialDataPanelContext context,
            Control field,
            String value,
            Consumer<String> setter,
            List<String> options,
            int width) {
        String key = field.key();
        return compactIdField(context, ItemEditorText.tr(key), value, setter, options, ItemEditorText.str(key), width);
    }
}
