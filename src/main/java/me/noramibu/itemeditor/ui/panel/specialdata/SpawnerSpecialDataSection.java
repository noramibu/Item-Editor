package me.noramibu.itemeditor.ui.panel.specialdata;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Sizing;
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

    private SpawnerSpecialDataSection() {
    }

    public static boolean supports(ItemStack stack) {
        return ItemEditorCapabilities.supportsSpawnerData(stack);
    }

    public static FlowLayout build(SpecialDataPanelContext context) {
        ItemEditorState.SpecialData special = context.special();
        Registry<EntityType<?>> entityRegistry = context.screen().session().registryAccess().lookupOrThrow(Registries.ENTITY_TYPE);
        List<String> entityIds = RegistryUtil.ids(entityRegistry);

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

        FlowLayout delayRow = UiFactory.row();
        delayRow.child(buildIntField(context, ItemEditorText.tr("special.spawner.delay"), special.spawnerDelay, value -> special.spawnerDelay = value));
        delayRow.child(buildIntField(context, ItemEditorText.tr("special.spawner.min_spawn_delay"), special.spawnerMinSpawnDelay, value -> special.spawnerMinSpawnDelay = value));
        delayRow.child(buildIntField(context, ItemEditorText.tr("special.spawner.max_spawn_delay"), special.spawnerMaxSpawnDelay, value -> special.spawnerMaxSpawnDelay = value));
        section.child(delayRow);

        FlowLayout countRow = UiFactory.row();
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

        section.child(UiFactory.button(
                ItemEditorText.tr("special.spawner.add_potential"),
                button -> context.mutateRefresh(() -> special.spawnerPotentials.add(new ItemEditorState.SpawnerPotentialDraft()))
        ).horizontalSizing(Sizing.content()));

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
                UiFactory.textBox(draft.weight, context.bindText(value -> draft.weight = value)).horizontalSizing(Sizing.fixed(120))
        ));

        return card;
    }

    private static FlowLayout buildIntField(
            SpecialDataPanelContext context,
            Component label,
            String value,
            java.util.function.Consumer<String> setter
    ) {
        FlowLayout field = UiFactory.field(
                label,
                Component.empty(),
                UiFactory.textBox(value, context.bindText(setter)).horizontalSizing(Sizing.fixed(100))
        );
        field.horizontalSizing(Sizing.fill(100));
        return field;
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

        FlowLayout inputRow = UiFactory.row();
        inputRow.child(UiFactory.textBox(
                rawInput,
                value -> context.mutateRefresh(() -> setter.accept(IdFieldNormalizer.normalize(value)))
        ));
        inputRow.child(buildPickerButton(context, entityIds, currentValueSupplier, setter));
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
            Consumer<String> setter
    ) {
        ButtonComponent button = UiFactory.button(ItemEditorText.tr("common.pick"), component ->
                context.openSearchablePicker(
                        ItemEditorText.str("special.spawner.entity_picker_title"),
                        "",
                        entityIds,
                        id -> id,
                        id -> context.mutateRefresh(() -> setter.accept(id))
                )
        );
        button.horizontalSizing(Sizing.fixed(70));
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

        FlowLayout row = UiFactory.row();
        row.gap(2);
        for (String entityId : chips) {
            Component label = UiFactory.fitToWidth(Component.literal(shortEntityId(entityId)), 86);
            ButtonComponent chip = UiFactory.button(label, button ->
                    context.mutateRefresh(() -> setter.accept(entityId))
            );
            chip.horizontalSizing(Sizing.content());
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
                    UiFactory.fitToWidth(Component.literal(match), 320),
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

    private static String shortEntityId(String entityId) {
        int separator = entityId.indexOf(':');
        return separator >= 0 && separator + 1 < entityId.length() ? entityId.substring(separator + 1) : entityId;
    }
}
