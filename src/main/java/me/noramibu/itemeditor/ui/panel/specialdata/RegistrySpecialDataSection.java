package me.noramibu.itemeditor.ui.panel.specialdata;

import io.wispforest.owo.ui.container.FlowLayout;
import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.ui.component.UiFactory;
import me.noramibu.itemeditor.util.ItemEditorText;
import net.minecraft.core.registries.Registries;

import java.util.List;
import java.util.function.Consumer;

import static me.noramibu.itemeditor.ui.panel.specialdata.AdvancedItemSpecialDataSection.collapsibleCard;
import static me.noramibu.itemeditor.ui.panel.specialdata.AdvancedItemSpecialDataSection.compactIdField;
import static me.noramibu.itemeditor.ui.panel.specialdata.AdvancedItemSpecialDataSection.compactIdTextWidth;
import static me.noramibu.itemeditor.ui.panel.specialdata.AdvancedItemSpecialDataSection.compactLongFieldWidth;
import static me.noramibu.itemeditor.ui.panel.specialdata.AdvancedItemSpecialDataSection.distributeRowChildren;
import static me.noramibu.itemeditor.ui.panel.specialdata.AdvancedItemSpecialDataSection.jukeboxSongIds;
import static me.noramibu.itemeditor.ui.panel.specialdata.AdvancedItemSpecialDataSection.responsiveRow;
import static me.noramibu.itemeditor.ui.panel.specialdata.AdvancedItemSpecialDataSection.withCurrentId;

public final class RegistrySpecialDataSection {

    private RegistrySpecialDataSection() {
    }

    public static FlowLayout build(SpecialDataPanelContext context) {
        ItemEditorState.SpecialData special = context.special();
        return collapsibleCard(
                context,
                ItemEditorText.tr("special.advanced.component_tweaks.registry_title"),
                special.uiComponentTweaksRegistryCollapsed,
                value -> special.uiComponentTweaksRegistryCollapsed = value,
                () -> {
                    FlowLayout content = UiFactory.column();
                    content.child(buildRegistryAndFlagsCard(context, special));
                    return content;
                }
        );
    }

    private static FlowLayout buildRegistryAndFlagsCard(
            SpecialDataPanelContext context,
            ItemEditorState.SpecialData special
    ) {
        FlowLayout card = UiFactory.subCard();
        int idWidth = compactIdTextWidth();
        int longWidth = compactLongFieldWidth();

        FlowLayout damageTypeField = registryIdField(
                context,
                "damage_type",
                special.damageTypeId,
                value -> special.damageTypeId = value,
                context.optionalRegistryIds(Registries.DAMAGE_TYPE),
                idWidth
        );
        card.child(damageTypeField);

        FlowLayout soundRow = responsiveRow();
        FlowLayout noteBlockSoundField = registryIdField(
                context,
                "note_block_sound",
                special.noteBlockSoundId,
                value -> special.noteBlockSoundId = value,
                context.optionalRegistryIds(Registries.SOUND_EVENT),
                idWidth
        );
        FlowLayout jukeboxPlayableField = registryIdField(
                context,
                "jukebox_playable",
                special.jukeboxSongId,
                value -> special.jukeboxSongId = value,
                jukeboxSongIds(context, special.jukeboxSongId),
                idWidth
        );
        FlowLayout providesBannerPatternsField = registryIdField(
                context,
                "provides_banner_patterns",
                special.providesBannerPatternsTagId,
                value -> special.providesBannerPatternsTagId = value,
                withCurrentId(
                        context.registryTagIds(Registries.BANNER_PATTERN, "#"),
                        special.providesBannerPatternsTagId
                ),
                longWidth
        );
        FlowLayout breakSoundField = registryIdField(
                context,
                "break_sound",
                special.breakSoundId,
                value -> special.breakSoundId = value,
                context.optionalRegistryIds(Registries.SOUND_EVENT),
                idWidth
        );
        distributeRowChildren(soundRow, noteBlockSoundField, jukeboxPlayableField, breakSoundField);
        card.child(soundRow);

        FlowLayout variantRow = responsiveRow();
        FlowLayout providesTrimMaterialField = registryIdField(
                context,
                "provides_trim_material",
                special.providesTrimMaterialId,
                value -> special.providesTrimMaterialId = value,
                context.optionalRegistryIds(Registries.TRIM_MATERIAL),
                longWidth
        );
        FlowLayout paintingVariantField = registryIdField(
                context,
                "painting_variant",
                special.paintingVariantId,
                value -> special.paintingVariantId = value,
                context.optionalRegistryIds(Registries.PAINTING_VARIANT),
                idWidth
        );
        distributeRowChildren(
                variantRow,
                providesTrimMaterialField,
                providesBannerPatternsField,
                paintingVariantField
        );
        card.child(variantRow);
        return card;
    }

    private static FlowLayout registryIdField(
            SpecialDataPanelContext context,
            String keySuffix,
            String value,
            Consumer<String> setter,
            List<String> options,
            int width
    ) {
        String key = "special.advanced.component_tweaks." + keySuffix;
        return compactIdField(
                context,
                ItemEditorText.tr(key),
                value,
                setter,
                options,
                ItemEditorText.str(key),
                width
        );
    }
}
