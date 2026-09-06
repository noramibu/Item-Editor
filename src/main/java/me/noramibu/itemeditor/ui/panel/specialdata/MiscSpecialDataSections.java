package me.noramibu.itemeditor.ui.panel.specialdata;

import static me.noramibu.itemeditor.ui.panel.specialdata.AdvancedItemSpecialDataSection.withCurrentId;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Sizing;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import me.noramibu.itemeditor.editor.EditorCategory;
import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.editor.text.RichTextDocument;
import me.noramibu.itemeditor.ui.component.DyeColorSelectorSection;
import me.noramibu.itemeditor.ui.component.EditorSearchDialog;
import me.noramibu.itemeditor.ui.component.PickerFieldFactory;
import me.noramibu.itemeditor.ui.component.StyledTextFieldSection;
import me.noramibu.itemeditor.ui.component.UiFactory;
import me.noramibu.itemeditor.ui.util.LayoutModeUtil;
import me.noramibu.itemeditor.ui.util.UiColors;
import me.noramibu.itemeditor.util.InstrumentDetails;
import me.noramibu.itemeditor.util.ItemEditorCapabilities;
import me.noramibu.itemeditor.util.ItemEditorText;
import me.noramibu.itemeditor.util.RegistryUtil;
import me.noramibu.itemeditor.util.TextComponentUtil;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Instrument;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.MapPostProcessing;

public final class MiscSpecialDataSections {
    public static List<EditorSearchDialog.Target> searchDyedColorTargets(
            SpecialDataPanelContext context, EditorCategory category) {
        if (!supportsDyed(context.originalStack())) return List.of();
        return SpecialDataSearch.targets(
                context, category, "special.misc.dyed.title", "misc-dyedcolor", () -> {}, DyedField.values());
    }

    public static List<EditorSearchDialog.Target> searchDyeTargets(
            SpecialDataPanelContext context, EditorCategory category) {
        if (!supportsDye(context.originalStack())) return List.of();
        return SpecialDataSearch.targets(
                context,
                category,
                "special.dye.title",
                "misc-dye",
                () -> {
                    context.special().uiDyeCollapsed = false;
                },
                DyeField.values());
    }

    public static List<EditorSearchDialog.Target> searchTrimTargets(
            SpecialDataPanelContext context, EditorCategory category) {
        if (!supportsTrim(context.originalStack())) return List.of();
        return SpecialDataSearch.targets(
                context, category, "special.misc.trim.title", "misc-trim", () -> {}, TrimField.values());
    }

    public static List<EditorSearchDialog.Target> searchProfileTargets(
            SpecialDataPanelContext context, EditorCategory category) {
        if (!supportsProfile(context.originalStack())) return List.of();
        return SpecialDataSearch.targets(
                context, category, "special.misc.profile.title", "misc-profile", () -> {}, ProfileField.values());
    }

    public static List<EditorSearchDialog.Target> searchProfileTargets(SpecialDataPanelContext context) {
        return searchProfileTargets(context, EditorCategory.SPECIAL_DATA);
    }

    public static List<EditorSearchDialog.Target> searchInstrumentTargets(
            SpecialDataPanelContext context, EditorCategory category) {
        if (!supportsInstrument(context.originalStack())) return List.of();
        var result = new ArrayList<>(SpecialDataSearch.targets(
                context,
                category,
                "special.misc.instrument.title",
                "misc-instrument",
                () -> {},
                InstrumentField.values()));
        return result;
    }

    public static List<EditorSearchDialog.Target> searchInstrumentTargets(SpecialDataPanelContext context) {
        return searchInstrumentTargets(context, EditorCategory.SPECIAL_DATA);
    }

    public static List<EditorSearchDialog.Target> searchMapTargets(
            SpecialDataPanelContext context, EditorCategory category) {
        if (!supportsMap(context.originalStack())) return List.of();
        return SpecialDataSearch.targets(
                context,
                category,
                "special.misc.map.title",
                "misc-map",
                () -> {
                    context.special().uiMapBasicCollapsed = false;
                },
                MapField.values());
    }

    private static final int COMPACT_LAYOUT_WIDTH_THRESHOLD = 560;
    private static final int COLLAPSE_TOGGLE_WIDTH_MIN = 36;
    private static final int COLLAPSE_TOGGLE_WIDTH_BASE = 42;
    private static final int PROFILE_NAME_FIELD_WIDTH = 220;
    private static final int PROFILE_UUID_FIELD_WIDTH = 260;
    private static final int MAP_POST_PICKER_WIDTH = 190;
    private static final int PROFILE_ACTION_BUTTON_WIDTH_MIN = 86;
    private static final int PROFILE_ACTION_BUTTON_WIDTH_MAX = 168;
    private static final int PROFILE_ACTION_BUTTON_ROW_RESERVE = 12;
    private static final int PROFILE_IDENTITY_ROW_RESERVE = 12;
    private static final int INSTRUMENT_PICK_BUTTON_WIDTH = 84;
    private static final int INSTRUMENT_NUMBER_FIELD_WIDTH = 150;
    private static final int INSTRUMENT_DESCRIPTION_EDITOR_HEIGHT = 54;

    private MiscSpecialDataSections() {}

    public static boolean supportsDyed(ItemStack stack) {
        return stack.has(DataComponents.DYED_COLOR)
                || stack.is(Items.LEATHER_HELMET)
                || stack.is(Items.LEATHER_CHESTPLATE)
                || stack.is(Items.LEATHER_LEGGINGS)
                || stack.is(Items.LEATHER_BOOTS)
                || stack.is(Items.LEATHER_HORSE_ARMOR)
                || stack.is(Items.WOLF_ARMOR);
    }

    public static boolean supportsDye(ItemStack stack) {
        return ItemEditorCapabilities.supportsDyeData(stack);
    }

    public static boolean supportsTrim(ItemStack stack) {
        return stack.has(DataComponents.TRIM) || stack.is(ItemTags.TRIMMABLE_ARMOR);
    }

    public static boolean supportsProfile(ItemStack stack) {
        return stack.has(DataComponents.PROFILE) || isHeadItem(stack);
    }

    public static boolean supportsInstrument(ItemStack stack) {
        return stack.has(DataComponents.INSTRUMENT) || stack.is(Items.GOAT_HORN);
    }

    public static boolean supportsMap(ItemStack stack) {
        return stack.has(DataComponents.MAP_COLOR)
                || stack.has(DataComponents.MAP_POST_PROCESSING)
                || stack.is(Items.FILLED_MAP);
    }

    public static FlowLayout buildDyedColor(SpecialDataPanelContext context) {
        ItemEditorState.SpecialData special = context.special();
        FlowLayout section = UiFactory.section(ItemEditorText.tr("special.misc.dyed.title"), Component.empty());
        section.id("misc-dyedcolor");
        section.child(UiFactory.field(
                DyedField.HEX_COLOR.text(),
                Component.empty(),
                context.colorInputWithPicker(
                                special.dyedColor,
                                value -> special.dyedColor = value,
                                () -> special.dyedColor,
                                ItemEditorText.str("special.misc.dyed.title"),
                                0xA06540)
                        .horizontalSizing(Sizing.fill(100))));
        return section;
    }

    public static FlowLayout buildDye(SpecialDataPanelContext context) {
        ItemEditorState.SpecialData special = context.special();
        return collapsibleCard(
                context,
                ItemEditorText.tr("special.dye.title"),
                special.uiDyeCollapsed,
                value -> special.uiDyeCollapsed = value,
                () -> {
                    FlowLayout content = UiFactory.column();
                    content.child(DyeColorSelectorSection.build(
                            context,
                            DyeField.COLOR.text(),
                            Component.empty(),
                            ItemEditorText.tr("common.unset").copy().withColor(UiColors.PICKER),
                            special.dyeColor,
                            Math.clamp(context.panelWidthHint() / 2, 120, 240),
                            DyeField.QUICK_PICK.text(),
                            color -> special.dyeColor = color.name()));
                    ButtonComponent clear = UiFactory.negativeButton(
                            DyeField.RESET.text(),
                            UiFactory.ButtonTextPreset.STANDARD,
                            button -> context.mutateRefresh(() -> special.dyeColor = ""));
                    clear.active(!special.dyeColor.isBlank());
                    clear.horizontalSizing(Sizing.fill(100));
                    content.child(clear);
                    return content;
                });
    }

    public static FlowLayout buildTrim(SpecialDataPanelContext context) {
        ItemEditorState.SpecialData special = context.special();
        List<String> materialIds = context.registryIds(Registries.TRIM_MATERIAL);
        List<String> patternIds = context.registryIds(Registries.TRIM_PATTERN);

        FlowLayout section = UiFactory.section(ItemEditorText.tr("special.misc.trim.title"), Component.empty());
        section.id("misc-trim");

        section.child(trimPickerField(
                context,
                TrimField.MATERIAL_ID,
                "select_material",
                special.trimMaterialId,
                materialIds,
                id -> special.trimMaterialId = id));

        section.child(trimPickerField(
                context,
                TrimField.PATTERN_ID,
                "select_pattern",
                special.trimPatternId,
                patternIds,
                id -> special.trimPatternId = id));
        return section;
    }

    private static FlowLayout trimPickerField(
            SpecialDataPanelContext context,
            TrimField field,
            String emptyKey,
            String value,
            List<String> ids,
            Consumer<String> setter) {
        String labelKey = field.key();
        return PickerFieldFactory.searchableField(
                context,
                ItemEditorText.tr(labelKey),
                Component.empty(),
                PickerFieldFactory.selectedOrFallback(value, ItemEditorText.tr("special.misc.trim." + emptyKey)),
                -1,
                ItemEditorText.str(labelKey),
                "",
                ids,
                id -> id,
                id -> context.mutateRefresh(() -> setter.accept(id)));
    }

    public static FlowLayout buildProfile(SpecialDataPanelContext context) {
        ItemEditorState.SpecialData special = context.special();
        FlowLayout section = UiFactory.section(ItemEditorText.tr("special.misc.profile.title"), Component.empty());
        section.id("misc-profile");
        boolean compactLayout = isCompactLayout(context);

        FlowLayout identityRow = compactLayout ? UiFactory.column() : UiFactory.row();
        int identityWidth = context.panelWidthHint() - UiFactory.scaledPixels(PROFILE_IDENTITY_ROW_RESERVE);
        int profileNameWidth = Math.min(identityWidth, PROFILE_NAME_FIELD_WIDTH);
        int profileUuidWidth = Math.min(identityWidth, PROFILE_UUID_FIELD_WIDTH);
        identityRow.child(UiFactory.field(
                        ProfileField.NAME.text(),
                        Component.empty(),
                        UiFactory.textBox(special.profileName, context.bindText(value -> special.profileName = value))
                                .horizontalSizing(Sizing.fill(100)))
                .horizontalSizing(compactLayout ? Sizing.fill(100) : UiFactory.fixed(profileNameWidth)));
        identityRow.child(UiFactory.field(
                        ProfileField.UUID.text(),
                        Component.empty(),
                        UiFactory.textBox(special.profileUuid, context.bindText(value -> special.profileUuid = value))
                                .horizontalSizing(Sizing.fill(100)))
                .horizontalSizing(compactLayout ? Sizing.fill(100) : UiFactory.fixed(profileUuidWidth)));
        section.child(identityRow);
        section.child(UiFactory.actionButtonRow(
                UiFactory.button(ProfileField.PICK_PLAYER.text(), UiFactory.ButtonTextPreset.STANDARD, button -> {
                    var players = new LinkedHashMap<>(EntitySpawnDataUi.onlinePlayers(context));
                    context.screen()
                            .openPlayerUuidPicker(
                                    ProfileField.PICK_PLAYER.text().getString(),
                                    players,
                                    uuid -> context.mutateRefresh(() -> {
                                        special.profileName = players.get(uuid);
                                        special.profileUuid = uuid;
                                    }));
                })));

        section.child(UiFactory.field(
                ProfileField.TEXTURE_VALUE.text(),
                Component.empty(),
                UiFactory.textArea(
                        special.profileTextureValue,
                        54,
                        context.bindText(value -> special.profileTextureValue = value))));
        section.child(UiFactory.field(
                ProfileField.TEXTURE_SIGNATURE.text(),
                Component.empty(),
                UiFactory.textBox(
                        special.profileTextureSignature,
                        context.bindText(value -> special.profileTextureSignature = value))));

        FlowLayout actions = compactLayout ? UiFactory.column() : UiFactory.row();
        int contentWidth = context.panelWidthHint();
        int profileActionButtonWidth = Math.clamp(
                Math.clamp(
                        (contentWidth - UiFactory.scaledPixels(PROFILE_ACTION_BUTTON_ROW_RESERVE)) / 2,
                        PROFILE_ACTION_BUTTON_WIDTH_MIN,
                        PROFILE_ACTION_BUTTON_WIDTH_MAX),
                1,
                Math.max(1, contentWidth));
        Component useLocalSkinText = ProfileField.USE_LOCAL_SKIN.text();
        var useLocalSkinButton = UiFactory.fixedWidthButton(
                useLocalSkinText, UiFactory.ButtonTextPreset.STANDARD, profileActionButtonWidth, button -> {
                    var player = context.screen().session().minecraft().player;
                    if (player == null) {
                        return;
                    }

                    var textures = player.getGameProfile().properties().get("textures").stream()
                            .findFirst()
                            .orElse(null);
                    if (textures == null) {
                        return;
                    }

                    context.mutateRefresh(() -> {
                        special.profileName = player.getGameProfile().name();
                        special.profileUuid = player.getGameProfile().id() == null
                                ? ""
                                : player.getGameProfile().id().toString();
                        special.profileTextureValue = textures.value();
                        special.profileTextureSignature = textures.signature() == null ? "" : textures.signature();
                    });
                });
        useLocalSkinButton.horizontalSizing(compactLayout ? Sizing.fill(100) : Sizing.fixed(profileActionButtonWidth));
        actions.child(useLocalSkinButton);

        Component clearSkinText = ProfileField.CLEAR_SKIN.text();
        var clearSkinButton = UiFactory.fixedWidthButton(
                clearSkinText,
                UiFactory.ButtonTextPreset.STANDARD,
                profileActionButtonWidth,
                button -> context.mutateRefresh(() -> {
                    special.profileTextureValue = "";
                    special.profileTextureSignature = "";
                }));
        clearSkinButton.horizontalSizing(compactLayout ? Sizing.fill(100) : Sizing.fixed(profileActionButtonWidth));
        actions.child(clearSkinButton);

        section.child(actions);
        return section;
    }

    public static FlowLayout buildInstrument(SpecialDataPanelContext context) {
        ItemEditorState.SpecialData special = context.special();
        Registry<Instrument> instrumentRegistry = instrumentRegistry(context);
        boolean compactLayout = isCompactLayout(context);
        FlowLayout section = UiFactory.section(ItemEditorText.tr("special.misc.instrument.title"), Component.empty());
        section.id("misc-instrument");

        section.child(PickerFieldFactory.searchableTextField(
                context,
                InstrumentField.ID.text(),
                special.instrumentId,
                value -> special.instrumentId = value,
                INSTRUMENT_PICK_BUTTON_WIDTH,
                InstrumentField.ID.text().getString(),
                "",
                instrumentIds(instrumentRegistry, special.instrumentId),
                id -> id,
                id -> context.mutateRefresh(() -> {
                    special.instrumentId = id;
                    var holder = instrumentRegistry == null ? null : RegistryUtil.resolveHolder(instrumentRegistry, id);
                    if (holder != null) {
                        InstrumentDetails.fromInstrument(holder.value()).applyTo(special);
                    }
                })));
        section.child(PickerFieldFactory.searchableTextField(
                context,
                InstrumentField.SOUND_EVENT.text(),
                special.instrumentSoundEventId,
                value -> special.instrumentSoundEventId = value,
                INSTRUMENT_PICK_BUTTON_WIDTH,
                InstrumentField.SOUND_EVENT.text().getString(),
                "",
                goatHornSoundEventIds(context, special.instrumentSoundEventId),
                id -> id,
                id -> context.mutateRefresh(() -> special.instrumentSoundEventId = id)));
        section.child(instrumentDescriptionEditor(context, special));

        FlowLayout values = compactLayout ? UiFactory.column() : UiFactory.row();
        values.child(UiFactory.field(
                        InstrumentField.USE_DURATION.text(),
                        Component.empty(),
                        UiFactory.textBox(
                                        special.instrumentUseDuration,
                                        context.bindText(value -> special.instrumentUseDuration = value))
                                .horizontalSizing(Sizing.fill(100)))
                .horizontalSizing(compactLayout ? Sizing.fill(100) : UiFactory.fixed(INSTRUMENT_NUMBER_FIELD_WIDTH)));
        values.child(UiFactory.field(
                        InstrumentField.RANGE.text(),
                        Component.empty(),
                        UiFactory.textBox(
                                        special.instrumentRange,
                                        context.bindText(value -> special.instrumentRange = value))
                                .horizontalSizing(Sizing.fill(100)))
                .horizontalSizing(compactLayout ? Sizing.fill(100) : UiFactory.fixed(INSTRUMENT_NUMBER_FIELD_WIDTH)));
        section.child(values);
        return section;
    }

    private static FlowLayout instrumentDescriptionEditor(
            SpecialDataPanelContext context, ItemEditorState.SpecialData special) {
        StyledTextFieldSection.BoundEditor editor = StyledTextFieldSection.create(
                context.screen(),
                RichTextDocument.fromMarkup(special.instrumentDescription),
                Sizing.fill(100),
                UiFactory.fixed(INSTRUMENT_DESCRIPTION_EDITOR_HEIGHT),
                InstrumentField.DESCRIPTION.text().getString(),
                StyledTextFieldSection.StylePreset.name(),
                ItemEditorText.str("special.misc.instrument.description_color"),
                ItemEditorText.str("special.misc.instrument.description_gradient"),
                "",
                "",
                null,
                document -> document.logicalLineCount() > 1
                        ? ItemEditorText.str("special.misc.instrument.description_single_line")
                        : null,
                document -> context.mutate(
                        () -> special.instrumentDescription = TextComponentUtil.serializeEditorDocument(document)),
                true,
                context.panelWidthHint());

        FlowLayout frame = UiFactory.framedEditorCard();
        frame.child(editor.toolbar());
        frame.child(editor.editor());
        frame.child(editor.validation());
        return UiFactory.field(InstrumentField.DESCRIPTION.text(), Component.empty(), frame);
    }

    private static Registry<Instrument> instrumentRegistry(SpecialDataPanelContext context) {
        try {
            return context.screen().session().registryAccess().lookupOrThrow(Registries.INSTRUMENT);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static List<String> instrumentIds(Registry<Instrument> instrumentRegistry, String currentId) {
        return withCurrentId(instrumentRegistry == null ? List.of() : RegistryUtil.ids(instrumentRegistry), currentId);
    }

    private static List<String> goatHornSoundEventIds(SpecialDataPanelContext context, String currentId) {
        List<String> soundEventIds = context.optionalRegistryIds(Registries.SOUND_EVENT);
        List<String> goatHornIds =
                soundEventIds.stream().filter(id -> id.contains("goat_horn")).toList();
        return withCurrentId(goatHornIds.isEmpty() ? soundEventIds : goatHornIds, currentId);
    }

    public static FlowLayout buildMap(SpecialDataPanelContext context) {
        ItemEditorState.SpecialData special = context.special();
        return collapsibleCard(
                context,
                ItemEditorText.tr("special.misc.map.title"),
                special.uiMapBasicCollapsed,
                value -> special.uiMapBasicCollapsed = value,
                () -> {
                    FlowLayout content = UiFactory.column();
                    FlowLayout row = isCompactLayout(context) ? UiFactory.column() : UiFactory.row();
                    row.child(UiFactory.field(
                            MapField.COLOR.text(),
                            Component.empty(),
                            context.colorInputWithPicker(
                                            special.mapColor,
                                            value -> special.mapColor = value,
                                            () -> special.mapColor,
                                            MapField.COLOR.text().getString(),
                                            0x7FB238)
                                    .horizontalSizing(Sizing.fill(100))));
                    row.child(PickerFieldFactory.dropdownField(
                            context,
                            MapField.POST.text(),
                            Component.empty(),
                            PickerFieldFactory.selectedOrFallback(
                                    special.mapPostProcessing, ItemEditorText.tr("special.misc.map.post.none")),
                            isCompactLayout(context) ? -1 : MAP_POST_PICKER_WIDTH,
                            Arrays.asList(MapPostProcessing.values()),
                            MapPostProcessing::name,
                            mode -> context.mutate(() -> special.mapPostProcessing = mode.name())));
                    content.child(row);
                    return content;
                });
    }

    private static FlowLayout collapsibleCard(
            SpecialDataPanelContext context,
            Component title,
            boolean collapsed,
            Consumer<Boolean> setter,
            Supplier<FlowLayout> contentBuilder) {
        FlowLayout card = UiFactory.subCard();
        card.id(title.equals(ItemEditorText.tr("special.dye.title")) ? "misc-dye" : "misc-map");
        FlowLayout header = UiFactory.row();
        header.child(UiFactory.title(title).shadow(false).horizontalSizing(Sizing.expand(100)));
        ButtonComponent toggle = UiFactory.button(
                LayoutModeUtil.sectionToggleText(collapsed), UiFactory.ButtonTextPreset.STANDARD, button -> {
                    setter.accept(!collapsed);
                    context.screen().refreshCurrentPanel();
                });
        int toggleWidth = Math.max(COLLAPSE_TOGGLE_WIDTH_MIN, COLLAPSE_TOGGLE_WIDTH_BASE);
        toggle.horizontalSizing(Sizing.fixed(toggleWidth));
        header.child(toggle);
        card.child(header);
        if (!collapsed) {
            card.child(contentBuilder.get());
        }
        return card;
    }

    private static boolean isCompactLayout(SpecialDataPanelContext context) {
        return context.isCompactPanel(COMPACT_LAYOUT_WIDTH_THRESHOLD);
    }

    private static boolean isHeadItem(ItemStack stack) {
        var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        String path = id.getPath();
        return path.endsWith("_head");
    }

    private enum DyedField implements SpecialDataSearch.Field {
        HEX_COLOR("special.misc.dyed.hex_color");

        private final String key;

        DyedField(String key) {
            this.key = key;
        }

        public String key() {
            return key;
        }
    }

    private enum DyeField implements SpecialDataSearch.Field {
        COLOR("special.dye.color"),
        QUICK_PICK("special.dye.quick_pick"),
        RESET("common.reset");

        private final String key;

        DyeField(String key) {
            this.key = key;
        }

        public String key() {
            return key;
        }
    }

    private enum TrimField implements SpecialDataSearch.Field {
        MATERIAL_ID("special.misc.trim.material_id"),
        PATTERN_ID("special.misc.trim.pattern_id");

        private final String key;

        TrimField(String key) {
            this.key = key;
        }

        public String key() {
            return key;
        }
    }

    private enum ProfileField implements SpecialDataSearch.Field {
        NAME("special.misc.profile.name"),
        UUID("special.misc.profile.uuid"),
        PICK_PLAYER("special.misc.profile.pick_player"),
        TEXTURE_VALUE("special.misc.profile.texture_value"),
        TEXTURE_SIGNATURE("special.misc.profile.texture_signature"),
        USE_LOCAL_SKIN("special.misc.profile.use_local_skin"),
        CLEAR_SKIN("special.misc.profile.clear_skin");

        private final String key;

        ProfileField(String key) {
            this.key = key;
        }

        public String key() {
            return key;
        }
    }

    private enum InstrumentField implements SpecialDataSearch.Field {
        ID("special.misc.instrument.id"),
        SOUND_EVENT("special.misc.instrument.sound_event"),
        DESCRIPTION("special.misc.instrument.description"),
        USE_DURATION("special.misc.instrument.use_duration"),
        RANGE("special.misc.instrument.range");

        private final String key;

        InstrumentField(String key) {
            this.key = key;
        }

        public String key() {
            return key;
        }
    }

    private enum MapField implements SpecialDataSearch.Field {
        COLOR("special.misc.map.color"),
        POST("special.misc.map.post");

        private final String key;

        MapField(String key) {
            this.key = key;
        }

        public String key() {
            return key;
        }
    }
}
