package me.noramibu.itemeditor.ui.panel.specialdata;

import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Sizing;
import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.ui.component.PickerFieldFactory;
import me.noramibu.itemeditor.ui.component.UiFactory;
import me.noramibu.itemeditor.util.IdFieldNormalizer;
import me.noramibu.itemeditor.util.ItemEditorText;
import me.noramibu.itemeditor.util.RegistryUtil;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PlayerHeadItem;
import net.minecraft.world.item.component.MapPostProcessing;
import net.minecraft.world.item.equipment.trim.TrimMaterial;
import net.minecraft.world.item.equipment.trim.TrimPattern;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class MiscSpecialDataSections {

    private MiscSpecialDataSections() {
    }

    private static final int JUKEBOX_AUTOCOMPLETE_LIMIT = 8;

    public static boolean supportsDyed(ItemStack stack) {
        return stack.has(DataComponents.DYED_COLOR)
                || stack.is(Items.LEATHER_HELMET)
                || stack.is(Items.LEATHER_CHESTPLATE)
                || stack.is(Items.LEATHER_LEGGINGS)
                || stack.is(Items.LEATHER_BOOTS)
                || stack.is(Items.LEATHER_HORSE_ARMOR)
                || stack.is(Items.WOLF_ARMOR);
    }

    public static boolean supportsTrim(ItemStack stack) {
        return stack.has(DataComponents.TRIM) || stack.is(ItemTags.TRIMMABLE_ARMOR);
    }

    public static boolean supportsProfile(ItemStack stack) {
        return stack.has(DataComponents.PROFILE) || stack.getItem() instanceof PlayerHeadItem;
    }

    public static boolean supportsInstrument(ItemStack stack) {
        return stack.has(DataComponents.INSTRUMENT) || stack.is(Items.GOAT_HORN);
    }

    public static boolean supportsJukebox(ItemStack stack) {
        return stack.has(DataComponents.JUKEBOX_PLAYABLE)
                || stack.is(Items.JUKEBOX)
                || isMusicDisc(stack);
    }

    public static boolean supportsMap(ItemStack stack) {
        return stack.has(DataComponents.MAP_COLOR) || stack.has(DataComponents.MAP_POST_PROCESSING) || stack.is(Items.FILLED_MAP);
    }

    public static FlowLayout buildDyedColor(SpecialDataPanelContext context) {
        ItemEditorState.SpecialData special = context.special();
        FlowLayout section = UiFactory.section(ItemEditorText.tr("special.misc.dyed.title"), Component.empty());
        section.child(UiFactory.field(
                ItemEditorText.tr("special.misc.dyed.hex_color"),
                Component.empty(),
                context.colorInputWithPicker(
                        special.dyedColor,
                        value -> special.dyedColor = value,
                        () -> special.dyedColor,
                        ItemEditorText.str("special.misc.dyed.title"),
                        0xA06540
                ).horizontalSizing(Sizing.fill(100))
        ));
        return section;
    }

    public static FlowLayout buildTrim(SpecialDataPanelContext context) {
        ItemEditorState.SpecialData special = context.special();
        Registry<TrimMaterial> materialRegistry = context.screen().session().registryAccess().lookupOrThrow(Registries.TRIM_MATERIAL);
        Registry<TrimPattern> patternRegistry = context.screen().session().registryAccess().lookupOrThrow(Registries.TRIM_PATTERN);
        List<String> materialIds = RegistryUtil.ids(materialRegistry);
        List<String> patternIds = RegistryUtil.ids(patternRegistry);

        FlowLayout section = UiFactory.section(ItemEditorText.tr("special.misc.trim.title"), Component.empty());

        section.child(PickerFieldFactory.searchableField(
                context,
                ItemEditorText.tr("special.misc.trim.material_id"),
                Component.empty(),
                PickerFieldFactory.selectedOrFallback(special.trimMaterialId, ItemEditorText.tr("special.misc.trim.select_material")),
                -1,
                ItemEditorText.str("special.misc.trim.material_id"),
                "",
                materialIds,
                id -> id,
                id -> context.mutateRefresh(() -> special.trimMaterialId = id)
        ));

        section.child(PickerFieldFactory.searchableField(
                context,
                ItemEditorText.tr("special.misc.trim.pattern_id"),
                Component.empty(),
                PickerFieldFactory.selectedOrFallback(special.trimPatternId, ItemEditorText.tr("special.misc.trim.select_pattern")),
                -1,
                ItemEditorText.str("special.misc.trim.pattern_id"),
                "",
                patternIds,
                id -> id,
                id -> context.mutateRefresh(() -> special.trimPatternId = id)
        ));
        return section;
    }

    public static FlowLayout buildProfile(SpecialDataPanelContext context) {
        ItemEditorState.SpecialData special = context.special();
        FlowLayout section = UiFactory.section(ItemEditorText.tr("special.misc.profile.title"), Component.empty());

        FlowLayout identityRow = UiFactory.row();
        identityRow.child(UiFactory.field(
                ItemEditorText.tr("special.misc.profile.name"),
                Component.empty(),
                UiFactory.textBox(special.profileName, context.bindText(value -> special.profileName = value)).horizontalSizing(Sizing.fixed(200))
        ).horizontalSizing(Sizing.fixed(220)));
        identityRow.child(UiFactory.field(
                ItemEditorText.tr("special.misc.profile.uuid"),
                Component.empty(),
                UiFactory.textBox(special.profileUuid, context.bindText(value -> special.profileUuid = value)).horizontalSizing(Sizing.fixed(240))
        ).horizontalSizing(Sizing.fixed(260)));
        section.child(identityRow);

        section.child(UiFactory.field(
                ItemEditorText.tr("special.misc.profile.texture_value"),
                Component.empty(),
                UiFactory.textArea(special.profileTextureValue, 54, context.bindText(value -> special.profileTextureValue = value))
        ));
        section.child(UiFactory.field(
                ItemEditorText.tr("special.misc.profile.texture_signature"),
                Component.empty(),
                UiFactory.textBox(special.profileTextureSignature, context.bindText(value -> special.profileTextureSignature = value))
        ));

        FlowLayout actions = UiFactory.row();
        var useLocalSkinButton = UiFactory.button(ItemEditorText.tr("special.misc.profile.use_local_skin"), button -> {
            var player = context.screen().session().minecraft().player;
            if (player == null) {
                return;
            }

            var textures = player.getGameProfile().properties().get("textures").stream().findFirst().orElse(null);
            if (textures == null) {
                return;
            }

            context.mutateRefresh(() -> {
                special.profileName = player.getGameProfile().name();
                special.profileUuid = player.getGameProfile().id() == null ? "" : player.getGameProfile().id().toString();
                special.profileTextureValue = textures.value();
                special.profileTextureSignature = textures.signature() == null ? "" : textures.signature();
            });
        });
        useLocalSkinButton.horizontalSizing(Sizing.content());
        actions.child(useLocalSkinButton);

        var clearSkinButton = UiFactory.button(ItemEditorText.tr("special.misc.profile.clear_skin"), button ->
                context.mutateRefresh(() -> {
                    special.profileTextureValue = "";
                    special.profileTextureSignature = "";
                })
        );
        clearSkinButton.horizontalSizing(Sizing.content());
        actions.child(clearSkinButton);

        section.child(actions);
        return section;
    }

    public static FlowLayout buildInstrument(SpecialDataPanelContext context) {
        ItemEditorState.SpecialData special = context.special();
        FlowLayout section = UiFactory.section(ItemEditorText.tr("special.misc.instrument.title"), Component.empty());
        section.child(UiFactory.field(
                ItemEditorText.tr("special.misc.instrument.id"),
                Component.empty(),
                UiFactory.textBox(special.instrumentId, context.bindText(value -> special.instrumentId = value))
        ));
        return section;
    }

    public static FlowLayout buildJukebox(SpecialDataPanelContext context) {
        ItemEditorState.SpecialData special = context.special();
        FlowLayout section = UiFactory.section(ItemEditorText.tr("special.misc.jukebox.title"), Component.empty());
        List<String> songIds = availableJukeboxSongIds(context, special.jukeboxSongId);

        FlowLayout input = UiFactory.column().gap(2);
        input.child(UiFactory.textBox(
                special.jukeboxSongId,
                context.bindText(value -> special.jukeboxSongId = IdFieldNormalizer.normalize(value))
        ));
        input.child(buildJukeboxAutocomplete(context, special, songIds));

        FlowLayout pickerRow = UiFactory.row();
        pickerRow.child(UiFactory.button(
                PickerFieldFactory.selectedOrFallback(special.jukeboxSongId, ItemEditorText.tr("special.misc.jukebox.select_song")),
                button -> context.openSearchablePicker(
                        ItemEditorText.str("special.misc.jukebox.id"),
                        "",
                        songIds,
                        id -> id,
                        id -> context.mutateRefresh(() -> special.jukeboxSongId = id)
                )
        ).horizontalSizing(Sizing.content()));
        input.child(pickerRow);

        section.child(UiFactory.field(
                ItemEditorText.tr("special.misc.jukebox.id"),
                Component.empty(),
                input
        ));
        return section;
    }

    public static FlowLayout buildMap(SpecialDataPanelContext context) {
        ItemEditorState.SpecialData special = context.special();
        FlowLayout section = UiFactory.section(ItemEditorText.tr("special.misc.map.title"), Component.empty());

        FlowLayout row = UiFactory.row();
        row.child(UiFactory.field(
                ItemEditorText.tr("special.misc.map.color"),
                Component.empty(),
                context.colorInputWithPicker(
                        special.mapColor,
                        value -> special.mapColor = value,
                        () -> special.mapColor,
                        ItemEditorText.str("special.misc.map.color"),
                        0x7FB238
                ).horizontalSizing(Sizing.fill(100))
        ));
        row.child(PickerFieldFactory.dropdownField(
                context,
                ItemEditorText.tr("special.misc.map.post"),
                Component.empty(),
                PickerFieldFactory.selectedOrFallback(special.mapPostProcessing, ItemEditorText.tr("special.misc.map.post.none")),
                190,
                Arrays.asList(MapPostProcessing.values()),
                MapPostProcessing::name,
                mode -> context.mutate(() -> special.mapPostProcessing = mode.name())
        ));
        section.child(row);

        return section;
    }

    private static boolean isMusicDisc(ItemStack stack) {
        var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id != null && id.getPath().startsWith("music_disc_");
    }

    private static List<String> availableJukeboxSongIds(SpecialDataPanelContext context, String currentId) {
        List<String> ids = new ArrayList<>();
        try {
            Registry<?> songRegistry = context.screen().session().registryAccess().lookupOrThrow(Registries.JUKEBOX_SONG);
            ids.addAll(RegistryUtil.ids(songRegistry));
        } catch (Exception ignored) {
        }

        String normalizedCurrent = IdFieldNormalizer.normalize(currentId);
        if (!normalizedCurrent.isBlank() && !ids.contains(normalizedCurrent)) {
            ids.add(normalizedCurrent);
        }
        ids.sort(String::compareTo);
        return ids;
    }

    private static FlowLayout buildJukeboxAutocomplete(
            SpecialDataPanelContext context,
            ItemEditorState.SpecialData special,
            List<String> songIds
    ) {
        FlowLayout suggestions = UiFactory.column().gap(1);
        String query = IdFieldNormalizer.normalize(special.jukeboxSongId);
        if (query.isBlank()) {
            return suggestions;
        }

        List<String> matches = songIds.stream()
                .filter(id -> id.contains(query))
                .limit(JUKEBOX_AUTOCOMPLETE_LIMIT)
                .toList();
        if (matches.isEmpty()) {
            suggestions.child(UiFactory.muted(ItemEditorText.tr("special.misc.jukebox.autocomplete.none"), 280));
            return suggestions;
        }

        for (String match : matches) {
            var suggestion = UiFactory.button(UiFactory.fitToWidth(Component.literal(match), 280), button ->
                    context.mutateRefresh(() -> special.jukeboxSongId = match)
            );
            suggestion.horizontalSizing(Sizing.fill(100));
            suggestion.tooltip(List.of(Component.literal(match)));
            suggestions.child(suggestion);
        }
        return suggestions;
    }
}
