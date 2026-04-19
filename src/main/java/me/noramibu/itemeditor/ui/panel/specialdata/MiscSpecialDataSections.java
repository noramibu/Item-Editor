package me.noramibu.itemeditor.ui.panel.specialdata;

import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.component.ButtonComponent;
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
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class MiscSpecialDataSections {
    private static final double COMPACT_LAYOUT_SCALE_THRESHOLD = 3.0d;
    private static final int COMPACT_LAYOUT_WIDTH_THRESHOLD = 560;
    private static final String SYMBOL_SECTION_COLLAPSED = "[+]";
    private static final String SYMBOL_SECTION_EXPANDED = "[-]";
    private static final int COLLAPSE_TOGGLE_WIDTH_MIN = 26;
    private static final int COLLAPSE_TOGGLE_WIDTH_BASE = 34;
    private static final int PROFILE_NAME_FIELD_WIDTH = 220;
    private static final int PROFILE_UUID_FIELD_WIDTH = 260;
    private static final int MAP_POST_PICKER_WIDTH = 190;
    private static final int JUKEBOX_AUTOCOMPLETE_HINT_WIDTH = 280;
    private static final int JUKEBOX_AUTOCOMPLETE_FIT_WIDTH = 280;
    private static final int PROFILE_ACTION_BUTTON_WIDTH_MIN = 86;
    private static final int PROFILE_ACTION_BUTTON_WIDTH_MAX = 168;
    private static final int PROFILE_ACTION_BUTTON_ROW_RESERVE = 12;
    private static final int PROFILE_ACTION_BUTTON_TEXT_MIN = 20;
    private static final int PROFILE_ACTION_BUTTON_TEXT_RESERVE = 10;
    private static final int PROFILE_IDENTITY_ROW_RESERVE = 12;

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
        boolean compactLayout = isCompactLayout(context);

        FlowLayout identityRow = compactLayout ? UiFactory.column() : UiFactory.row();
        int identityWidth = context.panelWidthHint() - UiFactory.scaledPixels(PROFILE_IDENTITY_ROW_RESERVE);
        int profileNameWidth = Math.min(identityWidth, PROFILE_NAME_FIELD_WIDTH);
        int profileUuidWidth = Math.min(identityWidth, PROFILE_UUID_FIELD_WIDTH);
        identityRow.child(UiFactory.field(
                ItemEditorText.tr("special.misc.profile.name"),
                Component.empty(),
                UiFactory.textBox(special.profileName, context.bindText(value -> special.profileName = value)).horizontalSizing(Sizing.fill(100))
        ).horizontalSizing(compactLayout ? Sizing.fill(100) : UiFactory.fixed(profileNameWidth)));
        identityRow.child(UiFactory.field(
                ItemEditorText.tr("special.misc.profile.uuid"),
                Component.empty(),
                UiFactory.textBox(special.profileUuid, context.bindText(value -> special.profileUuid = value)).horizontalSizing(Sizing.fill(100))
        ).horizontalSizing(compactLayout ? Sizing.fill(100) : UiFactory.fixed(profileUuidWidth)));
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

        FlowLayout actions = compactLayout ? UiFactory.column() : UiFactory.row();
        int contentWidth = context.panelWidthHint();
        int profileActionButtonWidth = resolveProfileActionButtonWidth(contentWidth);
        Component useLocalSkinText = ItemEditorText.tr("special.misc.profile.use_local_skin");
        var useLocalSkinButton = boundedProfileActionButton(useLocalSkinText, profileActionButtonWidth, button -> {
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
        useLocalSkinButton.horizontalSizing(compactLayout ? Sizing.fill(100) : Sizing.fixed(profileActionButtonWidth));
        actions.child(useLocalSkinButton);

        Component clearSkinText = ItemEditorText.tr("special.misc.profile.clear_skin");
        var clearSkinButton = boundedProfileActionButton(clearSkinText, profileActionButtonWidth, button ->
                context.mutateRefresh(() -> {
                    special.profileTextureValue = "";
                    special.profileTextureSignature = "";
                })
        );
        clearSkinButton.horizontalSizing(compactLayout ? Sizing.fill(100) : Sizing.fixed(profileActionButtonWidth));
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

        input.child(UiFactory.button(
                PickerFieldFactory.selectedOrFallback(special.jukeboxSongId, ItemEditorText.tr("special.misc.jukebox.select_song")), UiFactory.ButtonTextPreset.STANDARD, 
                button -> context.openSearchablePicker(
                        ItemEditorText.str("special.misc.jukebox.id"),
                        "",
                        songIds,
                        id -> id,
                        id -> context.mutateRefresh(() -> special.jukeboxSongId = id)
                )
        ).horizontalSizing(Sizing.fill(100)));

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
        section.child(collapsibleCard(
                context,
                ItemEditorText.tr("special.misc.map.title"),
                special.uiMapBasicCollapsed,
                value -> special.uiMapBasicCollapsed = value,
                () -> {
                    FlowLayout content = UiFactory.column();
                    FlowLayout row = isCompactLayout(context) ? UiFactory.column() : UiFactory.row();
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
                            isCompactLayout(context) ? -1 : MAP_POST_PICKER_WIDTH,
                            Arrays.asList(MapPostProcessing.values()),
                            MapPostProcessing::name,
                            mode -> context.mutate(() -> special.mapPostProcessing = mode.name())
                    ));
                    content.child(row);
                    return content;
                }
        ));

        return section;
    }

    private static FlowLayout collapsibleCard(
            SpecialDataPanelContext context,
            Component title,
            boolean collapsed,
            Consumer<Boolean> setter,
            Supplier<FlowLayout> contentBuilder
    ) {
        FlowLayout card = UiFactory.subCard();
        FlowLayout header = UiFactory.row();
        header.child(UiFactory.title(title).shadow(false).horizontalSizing(Sizing.expand(100)));
        ButtonComponent toggle = UiFactory.button(Component.literal(collapsed ? SYMBOL_SECTION_COLLAPSED : SYMBOL_SECTION_EXPANDED), UiFactory.ButtonTextPreset.STANDARD,  button -> {
            setter.accept(!collapsed);
            context.screen().refreshCurrentPanel();
        });
        int toggleWidth = Math.max(COLLAPSE_TOGGLE_WIDTH_MIN, UiFactory.scaledPixels(COLLAPSE_TOGGLE_WIDTH_BASE));
        toggle.horizontalSizing(Sizing.fixed(toggleWidth));
        header.child(toggle);
        card.child(header);
        if (!collapsed) {
            card.child(contentBuilder.get());
        }
        return card;
    }

    private static boolean isCompactLayout(SpecialDataPanelContext context) {
        return context.guiScale() >= COMPACT_LAYOUT_SCALE_THRESHOLD
                || context.panelWidthHint() < UiFactory.scaledPixels(COMPACT_LAYOUT_WIDTH_THRESHOLD);
    }

    private static ButtonComponent boundedProfileActionButton(
            Component fullText,
            int width,
            Consumer<ButtonComponent> onPress
    ) {
        Component fitted = UiFactory.fitToWidth(
                fullText,
                Math.max(PROFILE_ACTION_BUTTON_TEXT_MIN, width - UiFactory.scaledPixels(PROFILE_ACTION_BUTTON_TEXT_RESERVE))
        );
        ButtonComponent button = UiFactory.button(fitted, UiFactory.ButtonTextPreset.STANDARD, onPress);
        button.horizontalSizing(Sizing.fixed(width));
        if (!fitted.getString().equals(fullText.getString())) {
            button.tooltip(List.of(fullText));
        }
        return button;
    }

    private static int resolveProfileActionButtonWidth(int contentWidth) {
        int preferred = Math.max(
                PROFILE_ACTION_BUTTON_WIDTH_MIN,
                Math.min(
                        PROFILE_ACTION_BUTTON_WIDTH_MAX,
                        (contentWidth - UiFactory.scaledPixels(PROFILE_ACTION_BUTTON_ROW_RESERVE)) / 2
                )
        );
        return Math.max(1, Math.min(contentWidth, preferred));
    }

    private static boolean isMusicDisc(ItemStack stack) {
        var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id.getPath().startsWith("music_disc_");
    }

    private static List<String> availableJukeboxSongIds(SpecialDataPanelContext context, String currentId) {
        List<String> ids = new ArrayList<>();
        try {
            Registry<?> songRegistry = context.screen().session().registryAccess().lookupOrThrow(Registries.JUKEBOX_SONG);
            ids.addAll(RegistryUtil.ids(songRegistry));
        } catch (RuntimeException ignored) {
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
            suggestions.child(UiFactory.muted(ItemEditorText.tr("special.misc.jukebox.autocomplete.none"), JUKEBOX_AUTOCOMPLETE_HINT_WIDTH));
            return suggestions;
        }

        for (String match : matches) {
            var suggestion = UiFactory.button(UiFactory.fitToWidth(Component.literal(match), JUKEBOX_AUTOCOMPLETE_FIT_WIDTH), UiFactory.ButtonTextPreset.STANDARD,  button ->
                    context.mutateRefresh(() -> special.jukeboxSongId = match)
            );
            suggestion.horizontalSizing(Sizing.fill(100));
            suggestion.tooltip(List.of(Component.literal(match)));
            suggestions.child(suggestion);
        }
        return suggestions;
    }
}
