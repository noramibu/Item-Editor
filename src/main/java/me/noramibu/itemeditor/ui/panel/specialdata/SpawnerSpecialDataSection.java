package me.noramibu.itemeditor.ui.panel.specialdata;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.UIComponent;
import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.ui.component.UiFactory;
import me.noramibu.itemeditor.util.IdFieldNormalizer;
import me.noramibu.itemeditor.util.ItemEditorCapabilities;
import me.noramibu.itemeditor.util.ItemEditorText;
import me.noramibu.itemeditor.util.RegistryUtil;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class SpawnerSpecialDataSection {

    private static final int AUTOCOMPLETE_LIMIT = 8;
    private static final int CHIP_LIMIT = 6;
    private static final double COMPACT_LAYOUT_SCALE_THRESHOLD = 3.0d;
    private static final int COMPACT_LAYOUT_WIDTH_THRESHOLD = 560;
    private static final int INT_FIELD_WIDTH = 100;
    private static final int POTENTIAL_WEIGHT_FIELD_WIDTH = 120;
    private static final int PICKER_BUTTON_WIDTH = 70;
    private static final int CHIP_LABEL_FIT_WIDTH = 86;
    private static final int AUTOCOMPLETE_SUGGESTION_FIT_WIDTH = 320;
    private static final int ACTION_BUTTON_WIDTH_MIN = 92;
    private static final int ACTION_BUTTON_WIDTH_MAX = 170;
    private static final int ACTION_BUTTON_ROW_RESERVE = 10;
    private static final int CHIP_BUTTON_WIDTH_MIN = 68;
    private static final int CHIP_BUTTON_WIDTH_MAX = 108;
    private static final int CHIP_BUTTON_ROW_RESERVE = 12;

    private SpawnerSpecialDataSection() {
    }

    public static boolean supports(ItemStack stack) {
        return ItemEditorCapabilities.supportsSpawnerData(stack);
    }

    public static FlowLayout build(SpecialDataPanelContext context) {
        ItemEditorState.SpecialData special = context.special();
        Registry<EntityType<?>> entityRegistry = context.screen().session().registryAccess().lookupOrThrow(Registries.ENTITY_TYPE);
        List<String> entityIds = RegistryUtil.ids(entityRegistry);
        boolean compactLayout = isCompactLayout(context);

        FlowLayout section = UiFactory.section(ItemEditorText.tr("special.spawner.title"), Component.empty());
        section.child(UiFactory.field(
                ItemEditorText.tr("special.spawner.entity_id"),
                Component.empty(),
                buildEntityInput(
                        context,
                        entityIds,
                        special.spawnerEntityId,
                        value -> special.spawnerEntityId = value,
                        () -> special.spawnerEntityId
                )
        ));

        FlowLayout delayRow = compactLayout ? UiFactory.column() : UiFactory.row();
        delayRow.child(buildIntField(context, ItemEditorText.tr("special.spawner.delay"), special.spawnerDelay, value -> special.spawnerDelay = value));
        delayRow.child(buildIntField(context, ItemEditorText.tr("special.spawner.min_spawn_delay"), special.spawnerMinSpawnDelay, value -> special.spawnerMinSpawnDelay = value));
        delayRow.child(buildIntField(context, ItemEditorText.tr("special.spawner.max_spawn_delay"), special.spawnerMaxSpawnDelay, value -> special.spawnerMaxSpawnDelay = value));
        section.child(delayRow);

        FlowLayout countRow = compactLayout ? UiFactory.column() : UiFactory.row();
        countRow.child(buildIntField(context, ItemEditorText.tr("special.spawner.spawn_count"), special.spawnerSpawnCount, value -> special.spawnerSpawnCount = value));
        countRow.child(buildIntField(context, ItemEditorText.tr("special.spawner.max_nearby_entities"), special.spawnerMaxNearbyEntities, value -> special.spawnerMaxNearbyEntities = value));
        countRow.child(buildIntField(context, ItemEditorText.tr("special.spawner.required_player_range"), special.spawnerRequiredPlayerRange, value -> special.spawnerRequiredPlayerRange = value));
        countRow.child(buildIntField(context, ItemEditorText.tr("special.spawner.spawn_range"), special.spawnerSpawnRange, value -> special.spawnerSpawnRange = value));
        section.child(countRow);

        section.child(UiFactory.checkbox(
                ItemEditorText.tr("special.spawner.use_potentials"),
                special.spawnerUsePotentials,
                value -> context.mutateRefresh(() -> {
                    special.spawnerUsePotentials = value;
                    if (value && special.spawnerPotentials.isEmpty()) {
                        ItemEditorState.SpawnerPotentialDraft draft = new ItemEditorState.SpawnerPotentialDraft();
                        draft.entityId = special.spawnerEntityId;
                        special.spawnerPotentials.add(draft);
                    }
                })
        ));

        if (!special.spawnerUsePotentials) {
            return section;
        }

        int contentWidth = Math.max(1, context.panelWidthHint());
        int addPotentialButtonWidth = resolveButtonWidth(contentWidth, 1, ACTION_BUTTON_WIDTH_MIN, ACTION_BUTTON_WIDTH_MAX, ACTION_BUTTON_ROW_RESERVE);
        ButtonComponent addPotentialButton = UiFactory.button(
                ItemEditorText.tr("special.spawner.add_potential"), UiFactory.ButtonTextPreset.STANDARD, 
                button -> context.mutateRefresh(() -> special.spawnerPotentials.add(new ItemEditorState.SpawnerPotentialDraft()))
        );
        addPotentialButton.horizontalSizing(compactLayout ? Sizing.fill(100) : Sizing.fixed(addPotentialButtonWidth));
        section.child(addPotentialButton);

        for (int index = 0; index < special.spawnerPotentials.size(); index++) {
            section.child(buildPotentialCard(context, entityIds, special, index));
        }

        return section;
    }

    private static FlowLayout buildPotentialCard(
            SpecialDataPanelContext context,
            List<String> entityIds,
            ItemEditorState.SpecialData special,
            int index
    ) {
        ItemEditorState.SpawnerPotentialDraft draft = special.spawnerPotentials.get(index);

        FlowLayout card = context.createReorderableCard(
                ItemEditorText.tr("special.spawner.potential", index + 1),
                index > 0,
                () -> movePotential(special.spawnerPotentials, index, index - 1),
                index < special.spawnerPotentials.size() - 1,
                () -> movePotential(special.spawnerPotentials, index, index + 1),
                () -> special.spawnerPotentials.remove(index)
        );

        card.child(UiFactory.field(
                ItemEditorText.tr("special.spawner.entity_id"),
                Component.empty(),
                        buildEntityInput(
                                context,
                                entityIds,
                                draft.entityId,
                                value -> draft.entityId = value,
                        () -> draft.entityId
                )
        ));

        card.child(UiFactory.field(
                ItemEditorText.tr("special.spawner.potential_weight"),
                Component.empty(),
                UiFactory.textBox(
                        draft.weight,
                        context.bindText(value -> draft.weight = value)
                ).horizontalSizing(isCompactLayout(context) ? Sizing.fill(100) : UiFactory.fixed(POTENTIAL_WEIGHT_FIELD_WIDTH))
        ));

        return card;
    }

    private static UIComponent buildIntField(
            SpecialDataPanelContext context,
            Component label,
            String value,
            Consumer<String> setter
    ) {
        return UiFactory.field(
                label,
                Component.empty(),
        UiFactory.textBox(value, context.bindText(setter)).horizontalSizing(UiFactory.fixed(INT_FIELD_WIDTH))
        ).horizontalSizing(Sizing.fill(100));
    }

    private static void movePotential(List<ItemEditorState.SpawnerPotentialDraft> drafts, int from, int to) {
        if (from < 0 || from >= drafts.size() || to < 0 || to >= drafts.size() || from == to) {
            return;
        }
        ItemEditorState.SpawnerPotentialDraft moved = drafts.remove(from);
        drafts.add(to, moved);
    }

    private static FlowLayout buildEntityInput(
            SpecialDataPanelContext context,
            List<String> entityIds,
            String rawInput,
            Consumer<String> setter,
            Supplier<String> currentValueSupplier
    ) {
        FlowLayout input = UiFactory.column().gap(2);
        boolean compactLayout = isCompactLayout(context);

        FlowLayout inputRow = compactLayout ? UiFactory.column() : UiFactory.row();
        inputRow.child(UiFactory.textBox(
                rawInput,
                value -> context.mutateRefresh(() -> setter.accept(IdFieldNormalizer.normalize(value)))
        ));
        inputRow.child(buildPickerButton(context, entityIds, currentValueSupplier, setter, compactLayout));
        input.child(inputRow);

        FlowLayout chips = buildEntityChips(context, entityIds, rawInput, setter);
        if (!chips.children().isEmpty()) {
            input.child(chips);
        }

        FlowLayout autocomplete = buildEntityAutocomplete(context, entityIds, rawInput, setter);
        if (!autocomplete.children().isEmpty()) {
            input.child(autocomplete);
        }

        return input;
    }

    private static ButtonComponent buildPickerButton(
            SpecialDataPanelContext context,
            List<String> entityIds,
            Supplier<String> currentValueSupplier,
            Consumer<String> setter,
            boolean compactLayout
    ) {
        ButtonComponent button = UiFactory.button(ItemEditorText.tr("common.pick"), UiFactory.ButtonTextPreset.STANDARD,  component ->
                context.openSearchablePicker(
                        ItemEditorText.str("special.spawner.entity_picker_title"),
                        "",
                        entityIds,
                        id -> id,
                        id -> context.mutateRefresh(() -> setter.accept(id))
                )
        );
        button.horizontalSizing(compactLayout ? Sizing.fill(100) : UiFactory.fixed(PICKER_BUTTON_WIDTH));
        String current = currentValueSupplier.get();
        if (current != null && !current.isBlank()) {
            button.tooltip(List.of(Component.literal(current)));
        }
        return button;
    }

    private static FlowLayout buildEntityChips(
            SpecialDataPanelContext context,
            List<String> entityIds,
            String rawInput,
            Consumer<String> setter
    ) {
        String normalized = IdFieldNormalizer.normalize(rawInput);
        List<String> chips = normalized.isBlank()
                ? defaultEntityChips(entityIds)
                : entityIds.stream().filter(id -> id.contains(normalized)).limit(CHIP_LIMIT).toList();

        boolean compactLayout = isCompactLayout(context);
        int contentWidth = Math.max(1, context.panelWidthHint());
        FlowLayout row = compactLayout ? UiFactory.column() : UiFactory.row();
        row.gap(2);
        int chipWidth = resolveButtonWidth(contentWidth, Math.max(1, chips.size()), CHIP_BUTTON_WIDTH_MIN, CHIP_BUTTON_WIDTH_MAX, CHIP_BUTTON_ROW_RESERVE);
        for (String entityId : chips) {
            Component label = UiFactory.fitToWidth(Component.literal(shortEntityId(entityId)), CHIP_LABEL_FIT_WIDTH);
            ButtonComponent chip = UiFactory.button(label, UiFactory.ButtonTextPreset.STANDARD,  button ->
                    context.mutateRefresh(() -> setter.accept(entityId))
            );
            chip.horizontalSizing(compactLayout ? Sizing.fill(100) : Sizing.fixed(chipWidth));
            chip.tooltip(List.of(Component.literal(entityId)));
            row.child(chip);
        }
        return row;
    }

    private static FlowLayout buildEntityAutocomplete(
            SpecialDataPanelContext context,
            List<String> entityIds,
            String rawInput,
            Consumer<String> setter
    ) {
        FlowLayout suggestions = UiFactory.column().gap(1);
        String query = IdFieldNormalizer.normalize(rawInput);
        if (query.isBlank()) {
            return suggestions;
        }

        List<String> matches = entityIds.stream()
                .filter(id -> id.contains(query))
                .limit(AUTOCOMPLETE_LIMIT)
                .toList();
        for (String match : matches) {
            ButtonComponent suggestion = UiFactory.button(
                    UiFactory.fitToWidth(Component.literal(match), AUTOCOMPLETE_SUGGESTION_FIT_WIDTH), UiFactory.ButtonTextPreset.STANDARD,
                    button -> context.mutateRefresh(() -> setter.accept(match))
            );
            suggestion.horizontalSizing(Sizing.fill(100));
            suggestion.tooltip(List.of(Component.literal(match)));
            suggestions.child(suggestion);
        }
        return suggestions;
    }

    private static List<String> defaultEntityChips(List<String> entityIds) {
        List<String> defaults = List.of(
                "minecraft:zombie",
                "minecraft:skeleton",
                "minecraft:creeper",
                "minecraft:spider",
                "minecraft:enderman",
                "minecraft:pig"
        );
        List<String> chips = new ArrayList<>(CHIP_LIMIT);
        for (String id : defaults) {
            if (entityIds.contains(id)) {
                chips.add(id);
            }
            if (chips.size() >= CHIP_LIMIT) {
                return chips;
            }
        }
        for (String id : entityIds) {
            if (!chips.contains(id)) {
                chips.add(id);
            }
            if (chips.size() >= CHIP_LIMIT) {
                return chips;
            }
        }
        return chips;
    }

    private static boolean isCompactLayout(SpecialDataPanelContext context) {
        return context.guiScale() >= COMPACT_LAYOUT_SCALE_THRESHOLD
                || context.panelWidthHint() < UiFactory.scaledPixels(COMPACT_LAYOUT_WIDTH_THRESHOLD);
    }

    private static String shortEntityId(String entityId) {
        int separator = entityId.indexOf(':');
        return separator >= 0 && separator + 1 < entityId.length() ? entityId.substring(separator + 1) : entityId;
    }

    private static int resolveButtonWidth(
            int contentWidth,
            int buttonCount,
            int minWidth,
            int maxWidth,
            int rowReserve
    ) {
        int preferred = Math.max(
                minWidth,
                Math.min(
                        maxWidth,
                        (contentWidth - UiFactory.scaledPixels(rowReserve)) / Math.max(1, buttonCount)
                )
        );
        return Math.max(1, Math.min(contentWidth, preferred));
    }
}
