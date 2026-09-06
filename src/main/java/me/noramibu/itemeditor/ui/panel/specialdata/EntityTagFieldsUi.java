package me.noramibu.itemeditor.ui.panel.specialdata;

import com.mojang.serialization.Codec;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Sizing;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import me.noramibu.itemeditor.editor.EditorCategory;
import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.service.EntitySpawnDataUtil;
import me.noramibu.itemeditor.service.EntityTagFields;
import me.noramibu.itemeditor.service.EntityTagValues;
import me.noramibu.itemeditor.ui.component.CompactFieldLayout;
import me.noramibu.itemeditor.ui.component.EditorSearchDialog;
import me.noramibu.itemeditor.ui.component.RawTextAreaComponent;
import me.noramibu.itemeditor.ui.component.UiFactory;
import me.noramibu.itemeditor.ui.util.UiColors;
import me.noramibu.itemeditor.util.ItemEditorCapabilities;
import me.noramibu.itemeditor.util.ItemEditorText;
import me.noramibu.itemeditor.util.ItemEditorTypes;
import me.noramibu.itemeditor.util.LootTableIds;
import me.noramibu.itemeditor.util.ValidationUtil;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.equine.Markings;
import net.minecraft.world.entity.animal.equine.Variant;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.item.component.SuspiciousStewEffects;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

public final class EntityTagFieldsUi {
    record Flag(
            String key,
            Predicate<ItemEditorState.EntitySpawnDraft> read,
            BiConsumer<ItemEditorState.EntitySpawnDraft, Boolean> write) {}

    static final List<Flag> FLAGS = List.of(
            new Flag("special.entity.no_ai", d -> d.noAi, (d, v) -> d.noAi = v),
            new Flag("special.entity.silent", d -> d.silent, (d, v) -> d.silent = v),
            new Flag("special.entity.no_gravity", d -> d.noGravity, (d, v) -> d.noGravity = v),
            new Flag("special.entity.glowing", d -> d.glowing, (d, v) -> d.glowing = v),
            new Flag("special.entity.invulnerable", d -> d.invulnerable, (d, v) -> d.invulnerable = v),
            new Flag("special.spawn_egg.persistent", d -> d.persistenceRequired, (d, v) -> d.persistenceRequired = v),
            new Flag("special.entity.name_visible", d -> d.customNameVisible, (d, v) -> d.customNameVisible = v));
    private final SpecialDataPanelContext context;
    private final ItemEditorState.EntitySpawnDraft draft;
    private final Map<String, List<EntityTagFields.Field>> sections;

    EntityTagFieldsUi(SpecialDataPanelContext context, ItemEditorState.EntitySpawnDraft draft) {
        this.context = context;
        this.draft = draft;
        this.sections = sections(draft.entityId);
    }

    public static List<EditorSearchDialog.Target> searchTargets(SpecialDataPanelContext context) {
        return searchTargets(context, context.special().spawnEggEntity, ItemEditorText.str("special.spawn_egg.title"));
    }

    public static List<EditorSearchDialog.Target> searchTargets(
            SpecialDataPanelContext context, ItemEditorState.EntitySpawnDraft draft, String title) {
        return searchTargets(context, draft, title, "", () -> {});
    }

    public static List<EditorSearchDialog.Target> searchTargets(
            SpecialDataPanelContext context,
            ItemEditorState.EntitySpawnDraft draft,
            String title,
            String scope,
            Runnable expand) {
        var result = new ArrayList<EditorSearchDialog.Target>();
        var id = Identifier.tryParse(draft.entityId);
        if (id == null) return result;
        for (var flag : FLAGS) {
            result.add(new EditorSearchDialog.Target(
                    searchPath(
                            context, title, ItemEditorText.str("special.entity.flags"), ItemEditorText.str(flag.key())),
                    flag.key() + " " + EditorSearchDialog.english(flag.key()),
                    () -> {
                        expand.run();
                        draft.uiExpandedTagGroups.add("flags");
                        context.screen()
                                .revealSearchTarget(
                                        EditorCategory.SPECIAL_DATA,
                                        new EditorSearchDialog.Location(scope, ItemEditorText.key(flag.key())));
                    }));
        }
        for (var section : sections(draft.entityId).entrySet()) {
            for (var field : section.getValue()) {
                if (!visible(context, draft, field)) continue;
                String label = EntityTagInputUi.label(draft.entityId, field).getString();
                String labelKey =
                        EntityTagInputUi.breedingAge(draft.entityId, field.key()) ? "breeding_age" : field.key();
                result.add(new EditorSearchDialog.Target(
                        searchPath(
                                context,
                                title,
                                sectionLabel(draft, section.getKey()).getString(),
                                label),
                        field.key() + " " + fieldLabel(field.key()) + " "
                                + EditorSearchDialog.english("special.entity.tags.field." + labelKey),
                        () -> {
                            expand.run();
                            draft.uiExpandedTagGroups.add(section.getKey());
                            if (section.getKey().equals("equipment")) draft.equipment.uiCollapsed = false;
                            context.screen()
                                    .revealSearchTarget(
                                            EditorCategory.SPECIAL_DATA,
                                            new EditorSearchDialog.Location(scope, "entity-tag:" + field.key()));
                        }));
                Runnable showField = () -> {
                    expand.run();
                    draft.uiExpandedTagGroups.add(section.getKey());
                    if (section.getKey().equals("equipment")) draft.equipment.uiCollapsed = false;
                };
                var nestedPath = new ArrayList<String>(List.of(title.split(" > ")));
                nestedPath.add(sectionLabel(draft, section.getKey()).getString());
                nestedPath.add(label);
                result.addAll(nestedSearchTargets(context, draft, field, nestedPath, showField));
            }
        }
        return result;
    }

    private static List<EditorSearchDialog.Target> nestedSearchTargets(
            SpecialDataPanelContext context,
            ItemEditorState.EntitySpawnDraft draft,
            EntityTagFields.Field field,
            List<String> path,
            Runnable expand) {
        var result = new ArrayList<EditorSearchDialog.Target>();
        String scope = fieldScope(draft, field);
        result.addAll(EntityTagInputUi.searchTargets(context, draft, field, path, scope, expand));
        if (field.key().equals("Brain")) {
            result.addAll(EntityMemoryInputUi.searchTargets(context, draft, field, path, scope, expand));
        }
        if (field.kind() == EntityTagFields.Kind.LEASH) {
            result.addAll(EntityLeashInputUi.searchTargets(context, path, scope, expand));
        }
        if (field.kind() == EntityTagFields.Kind.BOOLEAN) return result;
        var actions = new ArrayList<SpecialDataSearch.Field>();
        if (Set.of("potion_contents", "stew_effects", "profile", "description", "LastOutput")
                .contains(field.key())) {
            actions.add(Action.EDIT);
        }
        if (EntityTagValues.enumChoices(field.key()).isEmpty() && !field.key().equals("UUID") && hasPicker(field)) {
            actions.add(Action.PICK);
        }
        if (field.key().equals("Variant")) actions.addAll(List.of(HorseAction.values()));
        if (structured(field)) {
            result.add(nestedTarget(context, path, "SNBT", "SNBT " + field.key(), scope, "entity-raw", () -> {
                expand.run();
                draft.uiExpandedTagGroups.add("raw:" + field.key());
            }));
            if (field.kind() != EntityTagFields.Kind.LEASH) actions.add(Action.UNSET);
        }
        if (EntityTagValues.isItem(field.key()) || Set.of("Items", "Inventory").contains(field.key())) {
            actions.add(SpecialDataPanelContext.ItemAction.PICK);
            actions.add(SpecialDataPanelContext.ItemAction.PICK_FROM_STORAGE);
        }
        result.addAll(SpecialDataSearch.targets(
                context,
                EditorCategory.SPECIAL_DATA,
                path,
                scope,
                expand,
                actions.toArray(SpecialDataSearch.Field[]::new)));
        Tag payload = parsed(field, currentValue(draft, field));
        if (EntityTagValues.isItem(field.key()) || Set.of("Items", "Inventory").contains(field.key())) {
            boolean list = !EntityTagValues.isItem(field.key());
            int count = list && payload instanceof ListTag entries
                    ? entries.size()
                    : !list && payload instanceof CompoundTag ? 1 : 0;
            for (int index = 0; index < count; index++) {
                Tag entry = list ? ((ListTag) payload).get(index) : payload;
                String label = itemEntryLabel(context, entry, index).getString();
                result.add(nestedTarget(
                        context, path, label, field.key() + " " + label, scope, "entity-item:" + index, expand));
                if (list) {
                    var entryPath = new ArrayList<>(path);
                    entryPath.add(label);
                    result.add(nestedTarget(
                            context,
                            entryPath,
                            Action.REMOVE.text().getString(),
                            Action.REMOVE.key(),
                            scope,
                            "entity-item-remove:" + index,
                            expand));
                }
            }
        }
        if (field.key().equals("Tags") && payload instanceof ListTag entries) {
            for (int index = 0; index < entries.size(); index++) {
                String label = stringEntryLabel(((StringTag) entries.get(index)).value(), index);
                result.add(nestedTarget(
                        context, path, label, field.key() + " " + label, scope, "entity-string:" + index, expand));
                var entryPath = new ArrayList<>(path);
                entryPath.add(label);
                result.add(nestedTarget(
                        context,
                        entryPath,
                        Action.REMOVE.text().getString(),
                        Action.REMOVE.key(),
                        scope,
                        "entity-string-remove:" + index,
                        expand));
            }
        }
        if (EntityTagValues.isBlock(field.key()) && payload instanceof CompoundTag blockTag) {
            var id = Identifier.tryParse(blockTag.getStringOr("Name", ""));
            var block =
                    id == null ? null : BuiltInRegistries.BLOCK.getOptional(id).orElse(null);
            if (block != null)
                for (Property<?> property : block.getStateDefinition().getProperties()) {
                    result.add(nestedTarget(
                            context,
                            path,
                            property.getName(),
                            property.getName(),
                            scope,
                            "entity-block-property:" + property.getName(),
                            expand));
                }
        }
        return result;
    }

    private static EditorSearchDialog.Target nestedTarget(
            SpecialDataPanelContext context,
            List<String> path,
            String label,
            String terms,
            String scope,
            String field,
            Runnable expand) {
        var fullPath = new ArrayList<String>();
        fullPath.add(SpecialDataSearch.categoryTitle(context, EditorCategory.SPECIAL_DATA));
        fullPath.addAll(path);
        fullPath.add(label);
        return new EditorSearchDialog.Target(fullPath, terms, () -> {
            expand.run();
            context.screen()
                    .revealSearchTarget(EditorCategory.SPECIAL_DATA, new EditorSearchDialog.Location(scope, field));
        });
    }

    private static boolean structured(EntityTagFields.Field field) {
        return switch (field.kind()) {
            case COMPOUND, COMPOUNDS, COMPONENT, LEASH, UUIDS, STRING_OR_COMPOUND, STRINGS -> true;
            default -> false;
        };
    }

    private static Component itemEntryLabel(SpecialDataPanelContext context, Tag entry, int index) {
        Component name = ItemStack.CODEC
                .parse(context.screen().session().registryAccess().createSerializationContext(NbtOps.INSTANCE), entry)
                .result()
                .map(stack -> stack.getHoverName().copy().append(" x" + stack.getCount()))
                .map(Component.class::cast)
                .orElseGet(Action.EDIT::text);
        return Component.literal((index + 1) + ": ").append(name);
    }

    private static String stringEntryLabel(String value, int index) {
        return (index + 1) + ": " + value;
    }

    private static List<String> searchPath(
            SpecialDataPanelContext context, String title, String section, String label) {
        var path = new ArrayList<String>();
        path.add(SpecialDataSearch.categoryTitle(context, EditorCategory.SPECIAL_DATA));
        path.addAll(List.of(title.split(" > ")));
        path.add(section);
        path.add(label);
        return path;
    }

    FlowLayout build() {
        FlowLayout section = UiFactory.column();
        var entityId = Identifier.tryParse(draft.entityId);
        var entityType = entityId == null
                ? null
                : BuiltInRegistries.ENTITY_TYPE.getOptional(entityId).orElse(null);
        if (draft == context.special().spawnEggEntity && entityType != null && entityType.onlyOpCanSetNbt()) {
            section.child(UiFactory.muted(ItemEditorText.tr("special.entity.tags.operator_only")));
        }
        if (Set.of(
                                "minecraft:armor_stand",
                                "minecraft:item_frame",
                                "minecraft:glow_item_frame",
                                "minecraft:spawner_minecart")
                        .contains(draft.entityId)
                || draft != context.special().spawnEggEntity
                        && Set.of("minecraft:villager", "minecraft:zombie_villager", "minecraft:wandering_trader")
                                .contains(draft.entityId)) {
            section.child(UiFactory.button(
                            ItemEditorText.tr(
                                    draft.entityId.equals("minecraft:spawner_minecart")
                                            ? "special.entity.tags.spawner"
                                            : "special.entity.tags.editor"),
                            UiFactory.ButtonTextPreset.STANDARD,
                            button -> openExistingEditor(context, draft))
                    .horizontalSizing(Sizing.fill(100)));
        }
        for (var group : sections.entrySet()) {
            if (group.getValue().stream().noneMatch(field -> visible(context, draft, field))) continue;
            if (group.getKey().equals("entity")
                    && draft == context.special().spawnEggEntity
                    && ItemEditorCapabilities.supportsVillagerTrades(entityType)) continue;
            if (group.getKey().equals("flags") || group.getKey().equals("equipment")) continue;
            if (group.getKey().equals("values") && EntitySpawnDataUtil.supportsStatusEffects(draft.entityId)) continue;
            FlowLayout header = UiFactory.row();
            boolean expanded = draft.uiExpandedTagGroups.contains(group.getKey());
            header.child(UiFactory.title(sectionLabel(draft, group.getKey())).horizontalSizing(Sizing.expand(100)));
            header.child(UiFactory.muted(ItemEditorText.tr(
                    "special.entity.effects.summary",
                    configuredCount(
                            draft,
                            group.getValue().stream()
                                    .filter(field -> visible(context, draft, field))
                                    .toList()))));
            header.child(UiFactory.collapseToggleButton(
                    !expanded,
                    () -> context.mutateRefresh(() -> {
                        if (expanded) draft.uiExpandedTagGroups.remove(group.getKey());
                        else draft.uiExpandedTagGroups.add(group.getKey());
                    })));
            section.child(header);
            if (expanded) section.child(fields(group.getKey()));
        }
        Set<String> available = sections.values().stream()
                .flatMap(List::stream)
                .map(EntityTagFields.Field::key)
                .collect(Collectors.toSet());
        if (available.contains("Brain")) available.add("Brain.memories");
        for (String key : List.copyOf(draft.entityTagEdits.keySet())) {
            if (available.contains(key)) continue;
            section.child(UiFactory.button(
                            ItemEditorText.tr("special.entity.tags.discard", key),
                            UiFactory.ButtonTextPreset.STANDARD,
                            button -> context.mutateRefresh(() -> draft.entityTagEdits.remove(key)))
                    .horizontalSizing(Sizing.fill(100)));
        }
        return section;
    }

    private static Component sectionLabel(ItemEditorState.EntitySpawnDraft draft, String section) {
        return switch (section) {
            case "equipment" -> ItemEditorText.tr("common.equipment");
            case "flags" -> ItemEditorText.tr("special.entity.flags");
            case "values" -> ItemEditorText.tr("special.entity.values");
            case "entity" -> Component.translatable("entity." + draft.entityId.replace(':', '.'));
            default -> ItemEditorText.tr("special.entity.tags." + section);
        };
    }

    static Map<String, List<EntityTagFields.Field>> sections(String entityId) {
        Map<String, List<EntityTagFields.Field>> sections = new LinkedHashMap<>();
        Set<String> seen = new HashSet<>();
        for (var group : EntityTagFields.groups(entityId)) {
            for (var field : group.fields()) {
                if (field.key().equals("Brain.memories") || !seen.add(field.key())) continue;
                String section =
                        switch (field.key()) {
                            case "CanPickUpLoot", "LeftHanded" -> "equipment";
                            case "Pos",
                                    "Motion",
                                    "Rotation",
                                    "OnGround",
                                    "FallFlying",
                                    "fall_distance",
                                    "PortalCooldown",
                                    "home_pos",
                                    "home_radius",
                                    "sleeping_pos",
                                    "leash",
                                    "current_explosion_impact_pos" -> "movement";
                            case "last_hurt_by_mob",
                                    "last_hurt_by_player",
                                    "last_hurt_by_player_memory_time",
                                    "ticks_since_last_hurt_by_mob",
                                    "HurtTime",
                                    "Team" -> "S10";
                            case "Inventory" -> "S09";
                            default ->
                                switch (group.id()) {
                                    case "S01", "S02" ->
                                        switch (field.kind()) {
                                            case BOOLEAN -> "flags";
                                            case BYTE, SHORT, INT, LONG, FLOAT, DOUBLE -> "values";
                                            default -> "S01";
                                        };
                                    case "S03" -> "S03";
                                    case "S09", "S13" -> "S09";
                                    case "S04" -> "S04";
                                    case "S05", "S08" -> "S05";
                                    case "S06", "S07", "S10" -> "S10";
                                    default -> "entity";
                                };
                        };
                sections.computeIfAbsent(section, ignored -> new ArrayList<>()).add(field);
            }
        }
        return sections;
    }

    FlowLayout fields(String section) {
        List<EntityTagFields.Field> fields = sections.getOrDefault(section, List.of());
        var controls = fields.stream()
                .filter(field -> visible(context, draft, field))
                .map(field -> scopedField(context, draft, field))
                .toList();
        FlowLayout result = UiFactory.column().gap(2);
        if (!controls.isEmpty()) result.child(new CompactFieldLayout(controls, UiFactory.scaledPixels(190)));
        return result;
    }

    private static String fieldScope(ItemEditorState.EntitySpawnDraft draft, EntityTagFields.Field field) {
        return SpecialDataSearch.scope("entity-field", draft) + ":" + field.key();
    }

    private static FlowLayout scopedField(
            SpecialDataPanelContext context, ItemEditorState.EntitySpawnDraft draft, EntityTagFields.Field field) {
        FlowLayout wrapper = UiFactory.column();
        wrapper.id(fieldScope(draft, field));
        wrapper.child(field(context, draft, field).id("entity-tag:" + field.key()));
        return wrapper;
    }

    static long configuredCount(ItemEditorState.EntitySpawnDraft draft, List<EntityTagFields.Field> fields) {
        return fields.stream()
                .filter(field -> !currentValue(draft, field).isBlank())
                .count();
    }

    private static boolean visible(
            SpecialDataPanelContext context, ItemEditorState.EntitySpawnDraft draft, EntityTagFields.Field field) {
        return !field.key().equals("Offers") || draft != context.special().spawnEggEntity;
    }

    static String nextBoolean(String current) {
        return current.isBlank() ? "1" : current.equals("1") || current.equals("true") ? "0" : "";
    }

    private static void openExistingEditor(SpecialDataPanelContext context, ItemEditorState.EntitySpawnDraft draft) {
        CompoundTag tag = EntitySpawnDataUtil.toEntityTag(
                draft, context.screen().session().registryAccess());
        if (tag == null) return;
        boolean spawner = draft.entityId.equals("minecraft:spawner_minecart");
        var item =
                switch (draft.entityId) {
                    case "minecraft:armor_stand" -> Items.ARMOR_STAND;
                    case "minecraft:glow_item_frame" -> Items.GLOW_ITEM_FRAME;
                    case "minecraft:spawner_minecart" -> Items.SPAWNER;
                    case "minecraft:villager" -> Items.VILLAGER_SPAWN_EGG;
                    case "minecraft:zombie_villager" -> Items.ZOMBIE_VILLAGER_SPAWN_EGG;
                    case "minecraft:wandering_trader" -> Items.WANDERING_TRADER_SPAWN_EGG;
                    default -> Items.ITEM_FRAME;
                };
        ItemStack stack = new ItemStack(item);
        tag.remove("id");
        if (spawner) stack.set(DataComponents.BLOCK_ENTITY_DATA, TypedEntityData.of(ItemEditorTypes.MOB_SPAWNER, tag));
        else
            stack.set(
                    DataComponents.ENTITY_DATA,
                    TypedEntityData.of(BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse(draft.entityId)), tag));
        context.screen()
                .openNestedEditor(
                        stack,
                        EditorCategory.SPECIAL_DATA,
                        edited -> context.mutate(() -> {
                            var data = spawner
                                    ? edited.get(DataComponents.BLOCK_ENTITY_DATA)
                                    : edited.get(DataComponents.ENTITY_DATA);
                            CompoundTag updated = data == null ? new CompoundTag() : data.copyTagWithoutId();
                            updated.putString("id", draft.entityId);
                            EntitySpawnDataUtil.replaceEntity(
                                    draft, updated, context.screen().session().registryAccess());
                        }));
    }

    private static FlowLayout field(
            SpecialDataPanelContext context, ItemEditorState.EntitySpawnDraft draft, EntityTagFields.Field field) {
        FlowLayout row = UiFactory.column();
        String current = currentValue(draft, field);
        if (field.kind() == EntityTagFields.Kind.BOOLEAN) {
            Component label = ItemEditorText.tr(
                    current.isBlank()
                            ? "common.unset"
                            : current.equals("1") || current.equals("true") ? "common.true" : "common.false");
            return UiFactory.column()
                    .gap(2)
                    .child(UiFactory.muted(EntityTagInputUi.label(draft.entityId, field))
                            .horizontalSizing(Sizing.fill(100))
                            .tooltip(EntityTagInputUi.tooltip(draft.entityId, field)))
                    .child(UiFactory.button(
                                    label,
                                    UiFactory.ButtonTextPreset.COMPACT,
                                    button -> context.mutateRefresh(() -> EntityTagFields.edit(
                                            draft, field, nextBoolean(currentValue(draft, field)))))
                            .horizontalSizing(Sizing.fill(100))
                            .tooltip(EntityTagInputUi.tooltip(draft.entityId, field)));
        }
        FlowLayout content = UiFactory.column();
        List<ButtonComponent> actions = new ArrayList<>();
        Consumer<String> setter = value -> EntityTagFields.edit(draft, field, value);
        FlowLayout friendlyInput = field.key().equals("Tags")
                ? stringEntries(context, field, current, setter)
                : EntityTagInputUi.input(context, draft, field, current, setter);
        EntityTagInputUi.actions(context, draft, field, setter, actions);
        var choices = EntityTagValues.enumChoices(field.key());
        if (Set.of("potion_contents", "stew_effects", "profile", "description", "LastOutput")
                .contains(field.key())) {
            actions.add(UiFactory.button(
                    Action.EDIT.text(),
                    UiFactory.ButtonTextPreset.STANDARD,
                    button -> editComponent(context, draft, field, setter)));
        }
        boolean structured = structured(field);
        if (structured) {
            if (friendlyInput != null) content.child(friendlyInput);
            String expansionKey = "raw:" + field.key();
            boolean expanded = draft.uiExpandedTagGroups.contains(expansionKey);
            actions.add(UiFactory.button(
                    Component.literal("SNBT"),
                    UiFactory.ButtonTextPreset.COMPACT,
                    button -> context.mutateRefresh(() -> {
                        if (expanded) draft.uiExpandedTagGroups.remove(expansionKey);
                        else draft.uiExpandedTagGroups.add(expansionKey);
                    })));
            if (expanded) {
                var editor = new RawTextAreaComponent(Sizing.fill(100), UiFactory.fixed(54), current).wordWrap(true);
                editor.onChanged().subscribe((value, delta) -> context.mutate(() -> setter.accept(value)));
                editor.id("entity-raw");
                content.child(editor);
            }
        } else if (!choices.isEmpty()) {
            Component valueLabel = current.isBlank()
                    ? Action.UNSET.text()
                    : Component.literal(choices.stream()
                            .filter(choice -> choice.value().equals(current))
                            .map(EntityTagValues.Choice::label)
                            .findFirst()
                            .orElse(current));
            row.child(UiFactory.button(valueLabel, UiFactory.ButtonTextPreset.COMPACT, button -> {
                        List<EntityTagValues.Choice> options = new ArrayList<>();
                        options.add(new EntityTagValues.Choice(
                                "", Action.UNSET.text().getString()));
                        options.addAll(choices);
                        context.openDropdown(
                                button,
                                options,
                                EntityTagValues.Choice::label,
                                choice -> context.mutateRefresh(() -> setter.accept(choice.value())));
                    })
                    .horizontalSizing(Sizing.fill(100)));
        } else if (friendlyInput != null) {
            row.child(friendlyInput);
        } else {
            var input = UiFactory.textBox(current, context.bindText(setter));
            input.setHint(Action.UNSET.text());
            row.child(input.horizontalSizing(Sizing.fill(100)));
        }
        if (choices.isEmpty() && !field.key().equals("UUID") && hasPicker(field))
            actions.add(UiFactory.button(
                    Action.PICK.text().copy().withColor(UiColors.PICKER),
                    UiFactory.ButtonTextPreset.COMPACT,
                    button -> pick(context, draft, field, setter, button)));
        if (field.key().equals("Variant")) {
            for (boolean markings : new boolean[] {false, true}) {
                actions.add(UiFactory.button(
                        ItemEditorText.tr(
                                markings ? "special.entity.tags.horse_markings" : "special.entity.tags.horse_color"),
                        UiFactory.ButtonTextPreset.COMPACT,
                        button -> {
                            List<EntityTagValues.Choice> horseChoices = markings
                                    ? Arrays.stream(Markings.values())
                                            .map(value -> new EntityTagValues.Choice(
                                                    Integer.toString(value.getId()), value.name()))
                                            .toList()
                                    : Arrays.stream(Variant.values())
                                            .map(value -> new EntityTagValues.Choice(
                                                    Integer.toString(value.getId()), value.getSerializedName()))
                                            .toList();
                            context.openDropdown(
                                    button,
                                    horseChoices,
                                    EntityTagValues.Choice::label,
                                    choice -> context.mutateRefresh(
                                            () -> setter.accept(Integer.toString(EntityTagValues.withHorseVariant(
                                                    ValidationUtil.parseIntOrDefault(currentValue(draft, field), 0),
                                                    Integer.parseInt(choice.value()),
                                                    markings)))));
                        }));
            }
        }
        if (EntityTagValues.isItem(field.key()) || Set.of("Items", "Inventory").contains(field.key())) {
            Consumer<ItemStack> applyStack = stack -> {
                Tag encoded = ItemStack.CODEC
                        .encodeStart(
                                context.screen().session().registryAccess().createSerializationContext(NbtOps.INSTANCE),
                                stack)
                        .getOrThrow();
                if (EntityTagValues.isItem(field.key())) setter.accept(encoded.toString());
                else {
                    String latest = currentValue(draft, field);
                    Tag parsed = parsed(field, latest);
                    if (!latest.isBlank() && !(parsed instanceof ListTag)) return;
                    ListTag list = parsed instanceof ListTag existing ? existing.copy() : new ListTag();
                    CompoundTag entity = draft.originalEntityTag.copy();
                    entity.putInt(
                            "Strength",
                            ValidationUtil.parseIntOrDefault(
                                    draft.entityTagEdits.getOrDefault(
                                            "Strength", Integer.toString(entity.getIntOr("Strength", 1))),
                                    1));
                    if (list.size() >= EntityTagValues.inventorySize(draft.entityId, entity)) return;
                    if (field.key().equals("Items")) {
                        Set<Integer> slots = new HashSet<>();
                        for (Tag entry : list)
                            if (entry instanceof CompoundTag item) slots.add(item.getByteOr("Slot", (byte) -1) & 255);
                        int slot = 0;
                        while (slots.contains(slot)) slot++;
                        ((CompoundTag) encoded).putByte("Slot", (byte) slot);
                        if (EntityTagValues.isPackInventory(draft.entityId))
                            draft.entityTagEdits.put("ChestedHorse", "1");
                        else if (entity.contains("LootTable") || draft.entityTagEdits.containsKey("LootTable"))
                            draft.entityTagEdits.put("LootTable", "");
                    }
                    list.add(encoded);
                    setter.accept(list.toString());
                }
            };
            actions.add(context.itemPickButton(applyStack));
            actions.add(context.storagePickButton(applyStack));
        }
        if (structured && field.kind() != EntityTagFields.Kind.LEASH)
            actions.add(UiFactory.button(
                    Action.UNSET.text().copy().withColor(UiColors.PICKER),
                    UiFactory.ButtonTextPreset.COMPACT,
                    button -> context.mutateRefresh(() -> EntityTagFields.edit(draft, field, ""))));
        if (!actions.isEmpty()) row.child(UiFactory.packedActionButtonRow(actions.toArray(ButtonComponent[]::new)));
        content.child(row);
        if (EntityTagValues.isItem(field.key()) || Set.of("Items", "Inventory").contains(field.key())) {
            boolean list = !EntityTagValues.isItem(field.key());
            Tag payload = parsed(field, current);
            int count = list && payload instanceof ListTag entries
                    ? entries.size()
                    : !list && payload instanceof CompoundTag ? 1 : 0;
            for (int index = 0; index < count; index++) {
                int selected = index;
                List<ButtonComponent> itemActions = new ArrayList<>();
                Tag entry = list ? ((ListTag) payload).get(index) : payload;
                itemActions.add(UiFactory.button(
                        itemEntryLabel(context, entry, index), UiFactory.ButtonTextPreset.COMPACT, button -> {
                            Tag latest = parsed(field, currentValue(draft, field));
                            if (list && !(latest instanceof ListTag)) return;
                            Tag item = list && latest instanceof ListTag entries && selected < entries.size()
                                    ? entries.get(selected)
                                    : latest;
                            if (!(item instanceof CompoundTag itemTag)) return;
                            var ops = context.screen()
                                    .session()
                                    .registryAccess()
                                    .createSerializationContext(NbtOps.INSTANCE);
                            ItemStack.CODEC
                                    .parse(ops, item)
                                    .result()
                                    .ifPresent(stack -> EntitySpawnDataUi.editStack(context, stack, edited -> {
                                        CompoundTag replacement = itemTag.copy();
                                        replacement.remove("id");
                                        replacement.remove("count");
                                        replacement.remove("components");
                                        replacement.merge((CompoundTag) ItemStack.CODEC
                                                .encodeStart(ops, edited)
                                                .getOrThrow());
                                        if (list) {
                                            if (!(latest instanceof ListTag entries)) return;
                                            ListTag updated = entries.copy();
                                            updated.set(selected, replacement);
                                            setter.accept(updated.toString());
                                        } else setter.accept(replacement.toString());
                                    }));
                        }));
                itemActions.getFirst().id("entity-item:" + index);
                if (list)
                    itemActions.add(UiFactory.button(
                            Action.REMOVE.text(),
                            UiFactory.ButtonTextPreset.COMPACT,
                            button -> context.mutateRefresh(() -> {
                                if (!(parsed(field, currentValue(draft, field)) instanceof ListTag entries)
                                        || selected >= entries.size()) return;
                                ListTag updated = entries.copy();
                                updated.remove(selected);
                                setter.accept(updated.toString());
                            })));
                if (list) itemActions.getLast().id("entity-item-remove:" + index);
                content.child(UiFactory.packedActionButtonRow(itemActions.toArray(ButtonComponent[]::new)));
            }
        }
        if (EntityTagValues.isBlock(field.key()) && parsed(field, current) instanceof CompoundTag blockTag) {
            var id = Identifier.tryParse(blockTag.getStringOr("Name", ""));
            var block =
                    id == null ? null : BuiltInRegistries.BLOCK.getOptional(id).orElse(null);
            if (block != null)
                for (Property<?> property : block.getStateDefinition().getProperties()) {
                    content.child(UiFactory.button(
                                    Component.literal(property.getName() + ": "
                                            + blockTag.getCompoundOrEmpty("Properties")
                                                    .getStringOr(property.getName(), "")),
                                    UiFactory.ButtonTextPreset.COMPACT,
                                    button -> context.openDropdown(
                                            button,
                                            SpecialDataPanelContext.propertyValues(property, false),
                                            value -> value,
                                            value -> context.mutateRefresh(() -> {
                                                if (!(parsed(field, currentValue(draft, field))
                                                        instanceof CompoundTag latest)) return;
                                                CompoundTag updated = latest.copy();
                                                CompoundTag properties = updated.getCompoundOrEmpty("Properties")
                                                        .copy();
                                                properties.putString(property.getName(), value);
                                                updated.put("Properties", properties);
                                                setter.accept(updated.toString());
                                            })))
                            .id("entity-block-property:" + property.getName())
                            .horizontalSizing(Sizing.fill(100)));
                }
        }
        FlowLayout result = UiFactory.column().gap(2);
        result.child(UiFactory.muted(EntityTagInputUi.label(draft.entityId, field))
                .horizontalSizing(Sizing.fill(100))
                .tooltip(EntityTagInputUi.tooltip(draft.entityId, field)));
        result.child(content);
        return result;
    }

    private static FlowLayout stringEntries(
            SpecialDataPanelContext context, EntityTagFields.Field field, String current, Consumer<String> setter) {
        Tag parsed = current.isBlank() ? new ListTag() : parsed(field, current);
        if (!(parsed instanceof ListTag entries))
            return UiFactory.column()
                    .child(UiFactory.textBox(current, context.bindText(setter)).horizontalSizing(Sizing.fill(100)));
        FlowLayout result = UiFactory.column().gap(2);
        for (int index = 0; index < entries.size(); index++) {
            int selected = index;
            FlowLayout row = UiFactory.row();
            row.child(UiFactory.textBox(((StringTag) entries.get(index)).value(), context.bindText(value -> {
                        entries.set(selected, StringTag.valueOf(value));
                        setter.accept(entries.toString());
                    }))
                    .horizontalSizing(Sizing.expand(100)));
            row.child(UiFactory.button(
                            Component.literal("-"),
                            UiFactory.ButtonTextPreset.COMPACT,
                            button -> context.mutateRefresh(() -> {
                                entries.remove(selected);
                                setter.accept(entries.isEmpty() ? "" : entries.toString());
                            }))
                    .horizontalSizing(Sizing.fixed(UiFactory.scaledPixels(22)))
                    .tooltip(List.of(Action.REMOVE.text())));
            row.children().getFirst().id("entity-string:" + index);
            row.children()
                    .getFirst()
                    .tooltip(List.of(
                            Component.literal(stringEntryLabel(((StringTag) entries.get(index)).value(), index))));
            row.children().getLast().id("entity-string-remove:" + index);
            result.child(row);
        }
        String[] pending = {""};
        var add = UiFactory.button(
                Action.ADD.text(),
                UiFactory.ButtonTextPreset.COMPACT,
                button -> context.mutateRefresh(() -> {
                    if (pending[0].isBlank()) return;
                    StringTag tag = StringTag.valueOf(pending[0]);
                    if (!entries.contains(tag)) entries.add(tag);
                    setter.accept(entries.toString());
                }));
        add.active(false);
        var input = UiFactory.textBox("", value -> {
            pending[0] = value;
            add.active(!value.isBlank() && !entries.contains(StringTag.valueOf(value)));
        });
        input.setHint(ItemEditorText.tr("special.entity.tags.field.Tags"));
        result.child(input.horizontalSizing(Sizing.fill(100)));
        result.child(UiFactory.packedActionButtonRow(add));
        return result;
    }

    static String fieldLabel(String key) {
        String words = key.replace('_', ' ').replace('.', ' ').replaceAll("(?<=[a-z0-9])(?=[A-Z])", " ");
        return words.isEmpty() ? words : Character.toUpperCase(words.charAt(0)) + words.substring(1);
    }

    private static String currentValue(ItemEditorState.EntitySpawnDraft draft, EntityTagFields.Field field) {
        return draft.entityTagEdits.getOrDefault(field.key(), field.read(draft.originalEntityTag));
    }

    private static void editComponent(
            SpecialDataPanelContext context,
            ItemEditorState.EntitySpawnDraft draft,
            EntityTagFields.Field field,
            Consumer<String> setter) {
        switch (field.key()) {
            case "potion_contents" ->
                editComponent(
                        context,
                        draft,
                        field,
                        setter,
                        Items.POTION,
                        DataComponents.POTION_CONTENTS,
                        PotionContents.CODEC);
            case "stew_effects" ->
                editComponent(
                        context,
                        draft,
                        field,
                        setter,
                        Items.SUSPICIOUS_STEW,
                        DataComponents.SUSPICIOUS_STEW_EFFECTS,
                        SuspiciousStewEffects.CODEC);
            case "profile" ->
                editComponent(
                        context,
                        draft,
                        field,
                        setter,
                        Items.PLAYER_HEAD,
                        DataComponents.PROFILE,
                        ResolvableProfile.CODEC);
            default ->
                editComponent(
                        context,
                        draft,
                        field,
                        setter,
                        Items.PAPER,
                        DataComponents.CUSTOM_NAME,
                        ComponentSerialization.CODEC);
        }
    }

    private static <T> void editComponent(
            SpecialDataPanelContext context,
            ItemEditorState.EntitySpawnDraft draft,
            EntityTagFields.Field field,
            Consumer<String> setter,
            Item item,
            DataComponentType<T> type,
            Codec<T> codec) {
        ItemStack stack = new ItemStack(item);
        String current = currentValue(draft, field);
        var ops = context.screen().session().registryAccess().createSerializationContext(NbtOps.INSTANCE);
        if (!current.isBlank()) {
            Tag raw = parsed(field, current);
            if (raw == null) return;
            var value = codec.parse(ops, raw).result();
            if (value.isEmpty()) return;
            stack.set(type, value.get());
        }
        context.screen()
                .openNestedEditor(
                        stack,
                        type == DataComponents.CUSTOM_NAME ? EditorCategory.GENERAL : EditorCategory.SPECIAL_DATA,
                        edited -> context.mutate(() -> setter.accept(
                                edited.has(type)
                                        ? codec.encodeStart(ops, edited.get(type))
                                                .getOrThrow()
                                                .toString()
                                        : "")));
    }

    private static Tag parsed(EntityTagFields.Field field, String value) {
        try {
            return field.parse(value);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static boolean hasPicker(EntityTagFields.Field field) {
        return field.kind() == EntityTagFields.Kind.UUID
                || field.kind() == EntityTagFields.Kind.UUIDS
                || EntityTagValues.isBlock(field.key())
                || Set.of(
                                "SoundEvent",
                                "variant",
                                "Team",
                                "profile",
                                "Brain",
                                "Brain.memories",
                                "locator_bar_icon",
                                "LootTable",
                                "DeathLootTable",
                                "Tags",
                                "hidden_layers")
                        .contains(field.key());
    }

    private static void pick(
            SpecialDataPanelContext context,
            ItemEditorState.EntitySpawnDraft draft,
            EntityTagFields.Field field,
            Consumer<String> setter,
            ButtonComponent button) {
        if (Set.of("LootTable", "DeathLootTable").contains(field.key())) {
            var minecraft = context.screen().session().minecraft();
            var server = minecraft.getSingleplayerServer();
            if (server != null) {
                button.active(false);
                server.submit(() -> server.reloadableRegistries()
                                .lookup()
                                .lookupOrThrow(Registries.LOOT_TABLE)
                                .listElementIds()
                                .map(key -> key.identifier().toString())
                                .sorted()
                                .toList())
                        .whenComplete((ids, error) -> minecraft.execute(() -> {
                            button.active(true);
                            if (minecraft.gui.screen() != context.screen()) return;
                            context.openSearchablePicker(
                                    field.key(),
                                    ItemEditorText.str(
                                            error == null
                                                    ? "special.entity.tags.server_loot_hint"
                                                    : "special.entity.tags.loot_hint"),
                                    error == null ? ids : LootTableIds.fromResources(minecraft.getResourceManager()),
                                    value -> value,
                                    value -> context.mutateRefresh(() -> setter.accept(value)));
                        }));
                return;
            }
        }
        var enums = EntityTagValues.enumChoices(field.key());
        if (!enums.isEmpty()) {
            context.openDropdown(
                    button,
                    enums,
                    EntityTagValues.Choice::label,
                    choice -> context.mutateRefresh(() -> setter.accept(choice.value())));
            return;
        }
        if (EntityTagValues.isBlock(field.key())) {
            choose(
                    context,
                    field.key(),
                    context.registryIds(Registries.BLOCK),
                    id -> context.mutateRefresh(() -> setter.accept(BlockState.CODEC
                            .encodeStart(
                                    NbtOps.INSTANCE,
                                    BuiltInRegistries.BLOCK
                                            .getValue(Identifier.parse(id))
                                            .defaultBlockState())
                            .getOrThrow()
                            .toString())));
            return;
        }
        if (field.kind() == EntityTagFields.Kind.UUID
                || field.kind() == EntityTagFields.Kind.UUIDS
                || field.key().equals("profile")) {
            Map<String, String> players = new LinkedHashMap<>(EntitySpawnDataUi.onlinePlayers(context));
            var target = context.screen().session().minecraft().crosshairPickEntity;
            if (target != null
                    && !field.key().equals("profile")
                    && (target instanceof Player
                            || field.kind() == EntityTagFields.Kind.UUID
                                    && !EntityTagInputUi.playerReference(draft.entityId, field.key()))) {
                players.put(target.getUUID().toString(), target.getName().getString());
            }
            context.screen()
                    .openPlayerUuidPicker(
                            field.key(),
                            players,
                            uuid -> context.mutateRefresh(() -> {
                                if (field.kind() == EntityTagFields.Kind.UUIDS) {
                                    Tag previous = parsed(field, currentValue(draft, field));
                                    if (!currentValue(draft, field).isBlank() && !(previous instanceof ListTag)) return;
                                    ListTag list = previous instanceof ListTag entries ? entries.copy() : new ListTag();
                                    Tag value = UUIDUtil.CODEC
                                            .encodeStart(NbtOps.INSTANCE, UUID.fromString(uuid))
                                            .getOrThrow();
                                    if (!list.contains(value)) list.add(value);
                                    setter.accept(list.toString());
                                } else
                                    setter.accept(
                                            field.key().equals("profile")
                                                    ? StringTag.valueOf(players.get(uuid))
                                                            .toString()
                                                    : uuid);
                            }));
            return;
        }
        List<String> values =
                switch (field.key()) {
                    case "SoundEvent" -> context.optionalRegistryIds(Registries.SOUND_EVENT);
                    case "variant" -> context.optionalRegistryIds(Registries.PAINTING_VARIANT);
                    case "Team" ->
                        context.screen().session().minecraft().getConnection() == null
                                ? List.of()
                                : List.copyOf(context.screen()
                                        .session()
                                        .minecraft()
                                        .getConnection()
                                        .getSuggestionsProvider()
                                        .getAllTeams());
                    case "Brain", "Brain.memories" ->
                        BuiltInRegistries.MEMORY_MODULE_TYPE.stream()
                                .filter(MemoryModuleType::canSerialize)
                                .map(module -> BuiltInRegistries.MEMORY_MODULE_TYPE
                                        .getKey(module)
                                        .toString())
                                .sorted()
                                .toList();
                    case "locator_bar_icon" -> context.waypointStyleIds();
                    case "Tags" -> observedTags(context, draft);
                    case "hidden_layers" ->
                        Arrays.stream(PlayerModelPart.values())
                                .map(PlayerModelPart::getSerializedName)
                                .toList();
                    case "LootTable", "DeathLootTable" ->
                        LootTableIds.fromResources(
                                context.screen().session().minecraft().getResourceManager());
                    default -> List.of();
                };
        choose(
                context,
                field.key(),
                values,
                value -> context.mutateRefresh(() -> {
                    switch (field.key()) {
                        case "Brain", "Brain.memories" -> EntityMemoryInputUi.select(draft, value);
                        case "hidden_layers", "Tags" -> {
                            Tag previous = parsed(field, currentValue(draft, field));
                            if (!currentValue(draft, field).isBlank() && !(previous instanceof ListTag)) return;
                            ListTag parts = previous instanceof ListTag list ? list.copy() : new ListTag();
                            Tag part = StringTag.valueOf(value);
                            if (parts.contains(part) && field.key().equals("hidden_layers")) parts.remove(part);
                            else if (parts.contains(part)) return;
                            else parts.add(part);
                            setter.accept(parts.toString());
                        }
                        case "locator_bar_icon" -> {
                            Tag previous = parsed(
                                    field,
                                    draft.entityTagEdits.getOrDefault(
                                            field.key(), field.read(draft.originalEntityTag)));
                            CompoundTag compound = previous instanceof CompoundTag tag ? tag.copy() : new CompoundTag();
                            compound.putString("style", value);
                            setter.accept(compound.toString());
                        }
                        default -> setter.accept(value);
                    }
                }));
    }

    private static void choose(
            SpecialDataPanelContext context, String title, List<String> values, Consumer<String> setter) {
        String hint =
                switch (title) {
                    case "LootTable", "DeathLootTable" -> ItemEditorText.str("special.entity.tags.loot_hint");
                    case "Brain", "Brain.memories" -> ItemEditorText.str("special.entity.tags.memory_hint");
                    default -> "";
                };
        context.openSearchablePicker(title, hint, values, value -> value, setter);
    }

    private static List<String> observedTags(SpecialDataPanelContext context, ItemEditorState.EntitySpawnDraft draft) {
        Set<String> names = new TreeSet<>();
        for (Tag tag : draft.originalEntityTag.getListOrEmpty("Tags"))
            if (tag instanceof StringTag(String text)) names.add(text);
        var minecraft = context.screen().session().minecraft();
        if (minecraft.player != null) names.addAll(minecraft.player.entityTags());
        if (minecraft.crosshairPickEntity != null) names.addAll(minecraft.crosshairPickEntity.entityTags());
        return List.copyOf(names);
    }

    private enum Action implements SpecialDataSearch.Field {
        EDIT("common.edit"),
        PICK("common.pick"),
        UNSET("common.unset"),
        REMOVE("common.remove"),
        ADD("common.add");

        private final String key;

        Action(String key) {
            this.key = key;
        }

        public String key() {
            return key;
        }
    }

    private enum HorseAction implements SpecialDataSearch.Field {
        HORSE_MARKINGS("special.entity.tags.horse_markings"),
        HORSE_COLOR("special.entity.tags.horse_color");

        private final String key;

        HorseAction(String key) {
            this.key = key;
        }

        public String key() {
            return key;
        }
    }
}
