package me.noramibu.itemeditor.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.editor.ValidationMessage;
import me.noramibu.itemeditor.util.ItemEditorText;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.ShortTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.ComponentSerialization;

public final class EntityTagFields {
    public enum Kind {
        BOOLEAN,
        BYTE,
        SHORT,
        INT,
        LONG,
        FLOAT,
        DOUBLE,
        STRING,
        UUID,
        POSITION,
        MOTION,
        ROTATION,
        STRINGS,
        COMPOUND,
        COMPOUNDS,
        UUIDS,
        LEASH,
        STRING_OR_COMPOUND,
        COMPONENT
    }

    public record Field(String key, Kind kind) {
        public String read(CompoundTag tag) {
            Tag value = get(tag, key);
            if (value == null) return "";
            if (kind == Kind.STRING && value instanceof StringTag(String string)) return string;
            if (kind == Kind.UUID) {
                var uuid = UUIDUtil.CODEC.parse(NbtOps.INSTANCE, value).result();
                if (uuid.isPresent()) return uuid.get().toString();
            }
            if (value instanceof NumericTag number) return number.box().toString();
            return value.toString();
        }

        public Tag parse(String input) throws Exception {
            String raw = input.trim();
            return switch (kind) {
                case BOOLEAN -> {
                    if (!List.of("true", "false", "0", "1").contains(raw)) throw new IllegalArgumentException();
                    yield ByteTag.valueOf(raw.equals("true") || raw.equals("1"));
                }
                case BYTE -> ByteTag.valueOf(Byte.parseByte(raw));
                case SHORT -> ShortTag.valueOf(Short.parseShort(raw));
                case INT -> IntTag.valueOf(Integer.parseInt(raw));
                case LONG -> LongTag.valueOf(Long.parseLong(raw));
                case FLOAT -> {
                    float value = Float.parseFloat(raw);
                    if (!Float.isFinite(value)) throw new IllegalArgumentException();
                    yield FloatTag.valueOf(value);
                }
                case DOUBLE -> {
                    double value = Double.parseDouble(raw);
                    if (!Double.isFinite(value)) throw new IllegalArgumentException();
                    yield DoubleTag.valueOf(value);
                }
                case STRING -> StringTag.valueOf(input);
                case UUID ->
                    UUIDUtil.CODEC
                            .encodeStart(NbtOps.INSTANCE, UUID.fromString(raw))
                            .getOrThrow();
                default -> parseStructured(raw);
            };
        }

        private Tag parseStructured(String raw) throws Exception {
            Tag value = TagParser.create(NbtOps.INSTANCE).parseFully(raw);
            boolean valid =
                    switch (kind) {
                        case COMPOUND -> value instanceof CompoundTag;
                        case COMPOUNDS ->
                            value instanceof ListTag list && list.stream().allMatch(CompoundTag.class::isInstance);
                        case UUIDS ->
                            value instanceof ListTag list
                                    && list.stream().allMatch(entry -> UUIDUtil.CODEC
                                            .parse(NbtOps.INSTANCE, entry)
                                            .result()
                                            .isPresent());
                        case LEASH ->
                            value instanceof IntArrayTag array && array.size() == 3
                                    || value instanceof CompoundTag compound
                                            && compound.read("UUID", UUIDUtil.CODEC)
                                                    .isPresent();
                        case STRING_OR_COMPOUND -> value instanceof StringTag || value instanceof CompoundTag;
                        case COMPONENT ->
                            ComponentSerialization.CODEC
                                    .parse(NbtOps.INSTANCE, value)
                                    .result()
                                    .isPresent();
                        case POSITION -> value instanceof IntArrayTag array && array.size() == 3;
                        case MOTION, ROTATION ->
                            value instanceof ListTag list
                                    && list.size() == (kind == Kind.MOTION ? 3 : 2)
                                    && list.stream()
                                            .allMatch(entry -> entry instanceof NumericTag number
                                                    && Double.isFinite(number.doubleValue()));
                        case STRINGS ->
                            value instanceof ListTag list && list.stream().allMatch(StringTag.class::isInstance);
                        default -> false;
                    };
            if (!valid) throw new IllegalArgumentException();
            if (kind == Kind.MOTION || kind == Kind.ROTATION) {
                ListTag converted = new ListTag();
                for (Tag entry : (ListTag) value) {
                    NumericTag number = (NumericTag) entry;
                    if (kind == Kind.ROTATION) {
                        if (!Float.isFinite(number.floatValue())) throw new IllegalArgumentException();
                        converted.add(FloatTag.valueOf(number.floatValue()));
                    } else converted.add(DoubleTag.valueOf(number.doubleValue()));
                }
                return converted;
            }
            return value;
        }
    }

    public record Group(String id, List<Field> fields) {}

    private static final List<Field> COMMON = List.of(
            new Field("Air", Kind.SHORT), new Field("data", Kind.COMPOUND),
            new Field("fall_distance", Kind.DOUBLE), new Field("Fire", Kind.SHORT),
            new Field("Motion", Kind.MOTION), new Field("OnGround", Kind.BOOLEAN),
            new Field("Tags", Kind.STRINGS), new Field("PortalCooldown", Kind.INT),
            new Field("Pos", Kind.MOTION), new Field("Rotation", Kind.ROTATION),
            new Field("TicksFrozen", Kind.INT), new Field("UUID", Kind.UUID));

    private EntityTagFields() {}

    public static List<Group> groups(String entityId) {
        List<Group> result = new ArrayList<>();
        result.add(new Group("S01", COMMON));
        result.addAll(EntityTagSchema.groups(entityId));
        return List.copyOf(result);
    }

    public static Tag get(CompoundTag root, String path) {
        int dot = path.indexOf('.');
        return dot < 0 ? root.get(path) : get(root.getCompoundOrEmpty(path.substring(0, dot)), path.substring(dot + 1));
    }

    private static void put(CompoundTag root, String path, Tag value) {
        int dot = path.indexOf('.');
        if (dot < 0) {
            if (value == null) root.remove(path);
            else root.put(path, value);
            return;
        }
        String parent = path.substring(0, dot);
        CompoundTag child = root.getCompoundOrEmpty(parent).copy();
        put(child, path.substring(dot + 1), value);
        if (child.isEmpty()) root.remove(parent);
        else root.put(parent, child);
    }

    public static void edit(ItemEditorState.EntitySpawnDraft draft, Field field, String value) {
        if (value.equals(field.read(draft.originalEntityTag))) draft.entityTagEdits.remove(field.key());
        else draft.entityTagEdits.put(field.key(), value);
    }

    static boolean apply(ItemEditorState.EntitySpawnDraft draft, CompoundTag target, SpecialDataApplyContext context) {
        Map<String, Field> fields = new LinkedHashMap<>();
        groups(draft.entityId).forEach(group -> group.fields().forEach(field -> fields.put(field.key(), field)));
        boolean valid = true;
        for (var entry : draft.entityTagEdits.entrySet()) {
            Field field = fields.get(entry.getKey());
            try {
                if (field == null) throw new IllegalArgumentException();
                if (!entry.getValue().isBlank()) {
                    var choices = EntityTagValues.enumChoices(field.key());
                    String selected = field.key().equals("Variant")
                            ? Integer.toString(Integer.parseInt(entry.getValue().trim()) & 65535)
                            : entry.getValue().trim();
                    if (!choices.isEmpty()
                            && choices.stream()
                                    .noneMatch(choice -> choice.value().equals(selected))) {
                        throw new IllegalArgumentException();
                    }
                }
                Tag parsed = entry.getValue().isBlank() ? null : field.parse(entry.getValue());
                if (parsed != null)
                    EntityTagValues.validate(
                            draft.entityId, field.key(), parsed, draft.originalEntityTag, context.registryAccess());
                put(target, field.key(), parsed);
            } catch (Exception exception) {
                context.messages()
                        .add(ValidationMessage.error(ItemEditorText.str(
                                "special.entity.tags.invalid",
                                entry.getKey(),
                                exception.getMessage() == null
                                        ? ItemEditorText.str(
                                                "special.entity.tags.expected",
                                                field == null
                                                        ? "?"
                                                        : field.kind().name())
                                        : exception.getMessage())));
                valid = false;
            }
        }
        for (String key : List.of("Items", "Inventory")) {
            if (fields.containsKey(key)
                    && (draft.entityTagEdits.containsKey(key)
                            || draft.entityTagEdits.containsKey("Strength")
                            || draft.entityTagEdits.containsKey("ChestedHorse")
                            || draft.entityTagEdits.containsKey("LootTable"))
                    && !EntityTagValues.validInventory(draft.entityId, key, target)) {
                context.messages()
                        .add(ValidationMessage.error(ItemEditorText.str("special.entity.tags.inventory_invalid", key)));
                valid = false;
            }
        }
        return valid;
    }
}
