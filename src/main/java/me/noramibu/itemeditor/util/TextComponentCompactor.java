package me.noramibu.itemeditor.util;

import com.mojang.serialization.JsonOps;
import java.util.List;
import java.util.Objects;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;

public final class TextComponentCompactor {
    private static final List<String> STYLES =
            List.of("color", "shadow_color", "font", "bold", "italic", "underlined", "strikethrough", "obfuscated");

    private TextComponentCompactor() {}

    public static Component compactIfSmaller(Component source) {
        Component optimized = compact(source);
        return isSmaller(source, optimized) ? optimized : source;
    }

    private static boolean isSmaller(Component source, Component optimized) {
        var beforeJson = ComponentSerialization.CODEC
                .encodeStart(JsonOps.INSTANCE, source)
                .result();
        var afterJson = ComponentSerialization.CODEC
                .encodeStart(JsonOps.INSTANCE, optimized)
                .result();
        var beforeNbt = ComponentSerialization.CODEC
                .encodeStart(NbtOps.INSTANCE, source)
                .result();
        var afterNbt = ComponentSerialization.CODEC
                .encodeStart(NbtOps.INSTANCE, optimized)
                .result();
        return beforeJson.isPresent()
                && afterJson.isPresent()
                && beforeNbt.isPresent()
                && afterNbt.isPresent()
                && afterJson.get().toString().length()
                        < beforeJson.get().toString().length()
                && afterNbt.get().sizeInBytes() <= beforeNbt.get().sizeInBytes();
    }

    static Component compactGeneratedArt(Component source) {
        MutableComponent candidate = source.plainCopy().setStyle(source.getStyle());
        boolean changed = false;
        for (Component child : source.getSiblings()) {
            Component next = child;
            var style = child.getStyle().applyTo(source.getStyle());
            if (child.getSiblings().isEmpty()
                    && !style.isUnderlined()
                    && !style.isStrikethrough()
                    && !style.isObfuscated()
                    && child.getString().codePoints().allMatch(c -> c == ' ' || c == 0x200c)
                    && child.getStyle().getColor() != null) {
                next = child.copy().setStyle(child.getStyle().withColor((TextColor) null));
                changed = true;
            }
            candidate.append(next);
        }
        if (!changed) return source;
        Component original = compact(source);
        Component optimized = compact(candidate);
        return isSmaller(original, optimized) ? candidate : source;
    }

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
        return compact(tag, inherited, true);
    }

    private static Tag compact(Tag tag, CompoundTag inherited, boolean optimizeDefaults) {
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
        if (children.isEmpty() && invisibleWhitespace(node, effective)) node.remove("color");
        ListTag optimized = new ListTag();
        CompoundTag previous = null;
        StringBuilder text = new StringBuilder();
        for (Tag child : groupFonts(children, effective)) {
            Tag next = compact(child, effective, optimizeDefaults);
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
        return optimizeDefaults ? optimizeDefaults(node, inherited) : node;
    }

    private static CompoundTag optimizeDefaults(CompoundTag node, CompoundTag inherited) {
        if (!node.contains("text")) return node;
        for (String key : List.of("bold", "italic")) {
            Tag current = node.get(key);
            if (!(current instanceof ByteTag flag)) continue;
            ByteTag replacement = ByteTag.valueOf(flag.byteValue() == 0);
            ListTag children = node.getListOrEmpty("extra");
            long matches = children.stream()
                    .filter(child -> child instanceof CompoundTag value && replacement.equals(value.get(key)))
                    .count();
            if (matches < 2) continue;
            CompoundTag candidate = node.copy();
            candidate.put(key, replacement);
            candidate.putString("text", "");
            ListTag members = new ListTag();
            String ownText = node.getStringOr("text", "");
            if (!ownText.isEmpty()) {
                CompoundTag own = new CompoundTag();
                own.putString("text", ownText);
                own.put(key, current);
                members.add(own);
            }
            for (Tag child : children) {
                CompoundTag member;
                if (child instanceof CompoundTag value) {
                    member = value.copy();
                } else {
                    member = new CompoundTag();
                    member.put("text", child.copy());
                }
                if (!member.contains(key)) member.put(key, current);
                members.add(member);
            }
            candidate.put("extra", members);
            candidate = (CompoundTag) compact(candidate, inherited, false);
            if (candidate.sizeInBytes() < node.sizeInBytes()) node = candidate;
        }
        return node;
    }

    private static boolean sameStyle(CompoundTag first, CompoundTag second) {
        return first.size() == second.size()
                && first.keySet().stream()
                        .allMatch(key -> key.equals("text") || Objects.equals(first.get(key), second.get(key)));
    }

    private static boolean invisibleWhitespace(CompoundTag node, CompoundTag effective) {
        String font = effective.getStringOr("font", "");
        if (!font.equals("minecraft:uniform") && !font.equals("minecraft:default")) return false;
        for (String key : List.of("underlined", "strikethrough", "obfuscated")) {
            if (!ByteTag.valueOf(false).equals(effective.get(key))) return false;
        }
        return node.getString("text")
                .filter(text -> !text.isEmpty())
                .filter(text -> text.codePoints().allMatch(c -> c == ' ' || c == 0x200c))
                .isPresent();
    }

    private static ListTag groupFonts(ListTag children, CompoundTag inherited) {
        ListTag grouped = new ListTag();
        for (int start = 0; start < children.size(); ) {
            Tag first = children.get(start);
            Tag font = first instanceof CompoundTag node ? node.get("font") : null;
            int end = start + 1;
            if (font != null && !font.equals(inherited.get("font"))) {
                while (end < children.size()
                        && children.get(end) instanceof CompoundTag next
                        && font.equals(next.get("font"))) end++;
            }
            if (end == start + 1) {
                grouped.add(first);
            } else {
                CompoundTag group = new CompoundTag();
                group.putString("text", "");
                group.put("font", font);
                ListTag members = new ListTag();
                for (int index = start; index < end; index++) members.add(children.get(index));
                group.put("extra", members);
                grouped.add(group);
            }
            start = end;
        }
        return grouped;
    }
}
