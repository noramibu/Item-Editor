package me.noramibu.itemeditor.ui.panel.specialdata;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Sizing;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;
import java.util.UUID;
import java.util.function.Consumer;
import me.noramibu.itemeditor.editor.EditorCategory;
import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.service.EntityTagFields;
import me.noramibu.itemeditor.ui.component.EditorSearchDialog;
import me.noramibu.itemeditor.ui.component.UiFactory;
import me.noramibu.itemeditor.util.ItemEditorText;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

final class EntityMemoryInputUi {
    private static final String SELECTION = "memory:";

    private EntityMemoryInputUi() {}

    static List<EditorSearchDialog.Target> searchTargets(
            SpecialDataPanelContext context,
            ItemEditorState.EntitySpawnDraft draft,
            EntityTagFields.Field field,
            List<String> path,
            String scope,
            Runnable expand) {
        var result = new ArrayList<EditorSearchDialog.Target>();
        CompoundTag root;
        try {
            root = readRoot(draft, field);
        } catch (Exception exception) {
            return result;
        }
        var ids = new TreeSet<>(root.getCompoundOrEmpty("memories").keySet());
        draft.uiExpandedTagGroups.stream()
                .filter(key -> key.startsWith(SELECTION))
                .map(key -> key.substring(SELECTION.length()))
                .filter(key -> !key.isBlank())
                .forEach(ids::add);
        for (String id : ids) {
            var identifier = Identifier.tryParse(id);
            var module = identifier == null
                    ? null
                    : BuiltInRegistries.MEMORY_MODULE_TYPE
                            .getOptional(identifier)
                            .orElse(null);
            if (module == null || module.getCodec().isEmpty()) continue;
            var memoryPath = new ArrayList<>(path);
            memoryPath.add(id);
            Runnable reveal = () -> {
                expand.run();
                select(draft, id);
            };
            result.addAll(SpecialDataSearch.targets(
                    context, EditorCategory.SPECIAL_DATA, memoryPath, scope, reveal, Field.values()));
            if (module == MemoryModuleType.ANGRY_AT || module == MemoryModuleType.LIKED_PLAYER) {
                result.addAll(SpecialDataSearch.targets(
                        context, EditorCategory.SPECIAL_DATA, memoryPath, scope, reveal, PlayerField.values()));
            }
        }
        return result;
    }

    static void select(ItemEditorState.EntitySpawnDraft draft, String id) {
        draft.uiExpandedTagGroups.removeIf(key -> key.startsWith(SELECTION));
        draft.uiExpandedTagGroups.add(SELECTION + id);
    }

    static FlowLayout build(
            SpecialDataPanelContext context,
            ItemEditorState.EntitySpawnDraft draft,
            EntityTagFields.Field field,
            Consumer<String> setter) {
        FlowLayout result = UiFactory.column();
        String id = draft.uiExpandedTagGroups.stream()
                .filter(key -> key.startsWith(SELECTION))
                .map(key -> key.substring(SELECTION.length()))
                .findFirst()
                .orElse("");
        if (id.isBlank()) return result;
        var module = BuiltInRegistries.MEMORY_MODULE_TYPE
                .getOptional(Identifier.parse(id))
                .orElse(null);
        if (module == null || module.getCodec().isEmpty()) return result;
        CompoundTag previous;
        try {
            previous = readRoot(draft, field);
        } catch (Exception exception) {
            return result;
        }
        CompoundTag original = previous.getCompoundOrEmpty("memories").getCompoundOrEmpty(id);
        String[] value = {
            Objects.toString(original.get("value"), ""),
            original.contains("ttl") ? Long.toString(original.getLongOr("ttl", 0)) : ""
        };
        CompoundTag[] validated = {null};
        ButtonComponent apply =
                UiFactory.button(Field.MEMORY_APPLY.text(), UiFactory.ButtonTextPreset.COMPACT, button -> {
                    try {
                        if (validated[0] == null) return;
                        CompoundTag root = withEntry(readRoot(draft, field), id, validated[0]);
                        context.mutateRefresh(() -> setter.accept(root.toString()));
                    } catch (Exception exception) {
                        button.active(false);
                    }
                });
        Runnable validate = () -> {
            try {
                CompoundTag entry = entry(value[0], value[1]);
                module.getCodec()
                        .orElseThrow()
                        .parse(
                                context.screen().session().registryAccess().createSerializationContext(NbtOps.INSTANCE),
                                entry)
                        .getOrThrow();
                validated[0] = entry;
            } catch (Exception exception) {
                validated[0] = null;
            }
            apply.active(validated[0] != null);
            apply.tooltip(List.of(ItemEditorText.tr(
                    validated[0] != null ? "special.entity.tags.memory_apply" : "special.entity.tags.memory_invalid")));
        };
        result.child(UiFactory.muted(Component.literal(id)).horizontalSizing(Sizing.fill(100)));
        result.child(UiFactory.muted(Field.MEMORY_VALUE.text()).horizontalSizing(Sizing.fill(100)));
        var input = UiFactory.textBox(value[0], text -> {
            value[0] = text;
            validate.run();
        });
        input.setHint(Field.MEMORY_VALUE.text());
        result.child(input.horizontalSizing(Sizing.fill(100)));
        if (module == MemoryModuleType.ANGRY_AT || module == MemoryModuleType.LIKED_PLAYER) {
            result.child(UiFactory.button(
                    PlayerField.PICK_PLAYER.text(), UiFactory.ButtonTextPreset.COMPACT, button -> context.screen()
                            .openPlayerUuidPicker(
                                    id,
                                    EntitySpawnDataUi.onlinePlayers(context),
                                    uuid -> input.setValue(UUIDUtil.CODEC
                                            .encodeStart(NbtOps.INSTANCE, UUID.fromString(uuid))
                                            .getOrThrow()
                                            .toString()))));
        }
        result.child(UiFactory.muted(Field.MEMORY_EXPIRY.text()).horizontalSizing(Sizing.fill(100)));
        var expiry = UiFactory.textBox(value[1], text -> {
            value[1] = text;
            validate.run();
        });
        expiry.setHint(ItemEditorText.tr("special.entity.tags.memory_no_expiry"));
        result.child(expiry.horizontalSizing(Sizing.fill(100)));
        result.child(UiFactory.packedActionButtonRow(apply, EntityTagInputUi.action(context, Field.REMOVE.key(), () -> {
            try {
                String latest = draft.entityTagEdits.getOrDefault(field.key(), field.read(draft.originalEntityTag));
                if (latest.isBlank()) return;
                CompoundTag root = ((CompoundTag) field.parse(latest)).copy();
                CompoundTag memories = root.getCompoundOrEmpty("memories");
                memories.remove(id);
                root.put("memories", memories);
                setter.accept(root.toString());
            } catch (Exception exception) {
            }
        })));
        validate.run();
        return result;
    }

    private static CompoundTag readRoot(ItemEditorState.EntitySpawnDraft draft, EntityTagFields.Field field)
            throws Exception {
        String current = draft.entityTagEdits.getOrDefault(field.key(), field.read(draft.originalEntityTag));
        return current.isBlank() ? new CompoundTag() : (CompoundTag) field.parse(current);
    }

    static CompoundTag entry(String value, String expiry) throws Exception {
        CompoundTag entry = new CompoundTag();
        entry.put("value", TagParser.create(NbtOps.INSTANCE).parseFully(value));
        if (!expiry.isBlank()) {
            long ticks = Long.parseLong(expiry.trim());
            if (ticks < 0) throw new IllegalArgumentException();
            entry.putLong("ttl", ticks);
        }
        return entry;
    }

    static CompoundTag withEntry(CompoundTag original, String id, CompoundTag value) {
        CompoundTag root = original.copy();
        CompoundTag memories = root.getCompoundOrEmpty("memories");
        CompoundTag entry = memories.getCompoundOrEmpty(id);
        entry.remove("ttl");
        entry.merge(value);
        memories.put(id, entry);
        root.put("memories", memories);
        return root;
    }

    private enum Field implements SpecialDataSearch.Field {
        MEMORY_APPLY("special.entity.tags.memory_apply"),
        MEMORY_VALUE("special.entity.tags.memory_value"),
        MEMORY_EXPIRY("special.entity.tags.memory_expiry"),
        REMOVE("common.remove");

        private final String key;

        Field(String key) {
            this.key = key;
        }

        public String key() {
            return key;
        }
    }

    private enum PlayerField implements SpecialDataSearch.Field {
        PICK_PLAYER("special.misc.profile.pick_player");

        private final String key;

        PlayerField(String key) {
            this.key = key;
        }

        public String key() {
            return key;
        }
    }
}
