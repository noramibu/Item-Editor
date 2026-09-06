package me.noramibu.itemeditor.util;

import java.util.List;
import java.util.Objects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;

public final class TextComponentCompactor {
    private static final List<String> STYLES =
            List.of("color", "shadow_color", "font", "bold", "italic", "underlined", "strikethrough", "obfuscated");

    private TextComponentCompactor() {}

    public static Component compact(Component component) {
        return ComponentSerialization.CODEC
                .encodeStart(NbtOps.INSTANCE, component)
                .result()
                .flatMap(tag -> ComponentSerialization.CODEC
                        .parse(NbtOps.INSTANCE, compact(tag, new CompoundTag()))
                        .result())
                .orElse(component);
    }

    private static Tag compact(Tag tag, CompoundTag inherited) {
        if (!(tag instanceof CompoundTag node)) return tag;
        ListTag children = node.getListOrEmpty("extra");
        if (node.getString("text").filter(String::isEmpty).isPresent() && children.size() > 1) {
            for (String key : STYLES) {
                Tag common = children.getFirst() instanceof CompoundTag first ? first.get(key) : null;
                if (common != null
                        && children.stream()
                                .allMatch(
                                        child -> child instanceof CompoundTag value && common.equals(value.get(key)))) {
                    node.put(key, common);
                }
            }
        }
        CompoundTag effective = inherited.copy();
        for (String key : STYLES) {
            Tag value = node.get(key);
            if (value == null) continue;
            if (value.equals(inherited.get(key))) node.remove(key);
            effective.put(key, value);
        }
        ListTag optimized = new ListTag();
        CompoundTag previous = null;
        StringBuilder text = new StringBuilder();
        for (Tag child : children) {
            Tag next = compact(child, effective);
            if (next instanceof CompoundTag literal && literal.contains("text") && !literal.contains("extra")) {
                if (previous != null
                        && text.length() + literal.getStringOr("text", "").length() <= 16000
                        && sameStyle(previous, literal)) {
                    text.append(literal.getStringOr("text", ""));
                    continue;
                }
                if (previous != null) previous.putString("text", text.toString());
                previous = literal;
                text.setLength(0);
                text.append(literal.getStringOr("text", ""));
            } else {
                if (previous != null) previous.putString("text", text.toString());
                previous = null;
            }
            optimized.add(next);
        }
        if (previous != null) previous.putString("text", text.toString());
        if (!optimized.isEmpty()) node.put("extra", optimized);
        else node.remove("extra");
        if (node.getString("text").filter(String::isEmpty).isPresent()
                && optimized.size() == 1
                && optimized.getFirst() instanceof CompoundTag child) {
            node.remove("text");
            node.remove("extra");
            node.merge(child);
        }
        return node;
    }

    private static boolean sameStyle(CompoundTag first, CompoundTag second) {
        return first.size() == second.size()
                && first.keySet().stream()
                        .allMatch(key -> key.equals("text") || Objects.equals(first.get(key), second.get(key)));
    }
}
