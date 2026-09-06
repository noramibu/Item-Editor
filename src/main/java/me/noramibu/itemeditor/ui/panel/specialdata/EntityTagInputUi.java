package me.noramibu.itemeditor.ui.panel.specialdata;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Sizing;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import me.noramibu.itemeditor.editor.EditorCategory;
import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.service.EntityTagFields;
import me.noramibu.itemeditor.ui.component.CompactFieldLayout;
import me.noramibu.itemeditor.ui.component.EditorSearchDialog;
import me.noramibu.itemeditor.ui.component.UiFactory;
import me.noramibu.itemeditor.util.ItemEditorText;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NumericTag;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

final class EntityTagInputUi {
    private EntityTagInputUi() {}

    static boolean breedingAge(String entityId, String key) {
        return key.equals("Age")
                && !entityId.equals("minecraft:parrot")
                && EntityTagFields.groups(entityId).stream()
                        .anyMatch(group -> group.id().equals("S04"));
    }

    static boolean seconds(String entityId, String key) {
        if (parrotBreeding(entityId, key)) return false;
        return breedingAge(entityId, key)
                || Set.of("InLove", "ForcedAge", "PortalCooldown", "TicksFrozen", "HurtTime")
                        .contains(key);
    }

    static boolean playerReference(String entityId, String key) {
        return Set.of("LoveCause", "ConversionPlayer", "last_hurt_by_player").contains(key)
                || key.equals("Owner")
                        && EntityTagFields.groups(entityId).stream()
                                .anyMatch(group ->
                                        group.id().equals("S05") || group.id().equals("S08"));
    }

    static String secondsFromTicks(String value) {
        if (value.startsWith("invalid seconds: ")) return value.substring("invalid seconds: ".length());
        try {
            return new BigDecimal(new BigInteger(value))
                    .divide(BigDecimal.valueOf(20), 2, RoundingMode.UNNECESSARY)
                    .stripTrailingZeros()
                    .toPlainString();
        } catch (NumberFormatException exception) {
            return value;
        }
    }

    static String ticksFromSeconds(String value) {
        if (value.isBlank()) return "";
        return Integer.toString(
                new BigDecimal(value.trim()).multiply(BigDecimal.valueOf(20)).intValueExact());
    }

    static Component label(String entityId, EntityTagFields.Field field) {
        String key = breedingAge(entityId, field.key()) ? "breeding_age" : field.key();
        Component label = Component.translatableWithFallback(
                ItemEditorText.key("special.entity.tags.field." + key), EntityTagFieldsUi.fieldLabel(field.key()));
        return seconds(entityId, field.key()) ? ItemEditorText.tr("special.entity.tags.seconds", label) : label;
    }

    static List<Component> tooltip(String entityId, EntityTagFields.Field field) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal(field.key() + " (" + field.kind() + ")"));
        if (parrotBreeding(entityId, field.key())) {
            lines.add(ItemEditorText.tr("special.entity.tags.parrot_breeding_help"));
            return lines;
        }
        String key = breedingAge(entityId, field.key()) ? "breeding_age" : field.key();
        Component help = Component.translatableWithFallback(ItemEditorText.key("special.entity.tags.help." + key), "");
        if (!help.getString().isBlank()) lines.add(help);
        return lines;
    }

    private static boolean parrotBreeding(String entityId, String key) {
        return entityId.equals("minecraft:parrot")
                && Set.of("Age", "ForcedAge", "AgeLocked", "InLove", "LoveCause")
                        .contains(key);
    }

    static FlowLayout input(
            SpecialDataPanelContext context,
            ItemEditorState.EntitySpawnDraft draft,
            EntityTagFields.Field field,
            String current,
            Consumer<String> setter) {
        if (field.key().equals("Brain")) return EntityMemoryInputUi.build(context, draft, field, setter);
        if (field.kind() == EntityTagFields.Kind.LEASH)
            return EntityLeashInputUi.build(context, draft, field, current, setter);
        if (seconds(draft.entityId, field.key())) {
            FlowLayout result = UiFactory.column();
            var input = UiFactory.textBox(secondsFromTicks(current), value -> {
                try {
                    String ticks = ticksFromSeconds(value);
                    context.mutate(() -> setter.accept(ticks));
                } catch (NumberFormatException | ArithmeticException exception) {
                    context.mutate(() -> setter.accept("invalid seconds: " + value));
                }
            });
            input.setHint(ItemEditorText.tr("common.unset"));
            input.tooltip(tooltip(draft.entityId, field));
            result.child(input.horizontalSizing(Sizing.fill(100)));
            return result;
        }
        if (field.kind() != EntityTagFields.Kind.POSITION
                && field.kind() != EntityTagFields.Kind.MOTION
                && field.kind() != EntityTagFields.Kind.ROTATION) return null;
        int dimensions = field.kind() == EntityTagFields.Kind.ROTATION ? 2 : 3;
        String[] values = new String[dimensions];
        Arrays.fill(values, "");
        if (!current.isBlank()) {
            try {
                var parsed = field.parse(current);
                for (int index = 0; index < dimensions; index++) {
                    values[index] = parsed instanceof IntArrayTag ints
                            ? Integer.toString(ints.getAsIntArray()[index])
                            : ((NumericTag) ((ListTag) parsed).get(index)).box().toString();
                }
            } catch (Exception exception) {
                return null;
            }
        }
        List<FlowLayout> axes = new ArrayList<>();
        for (int index = 0; index < dimensions; index++) {
            int axis = index;
            Component name = field.kind() == EntityTagFields.Kind.ROTATION
                    ? ItemEditorText.tr(index == 0 ? "special.entity.tags.yaw" : "special.entity.tags.pitch")
                    : Component.literal(List.of("X", "Y", "Z").get(index));
            FlowLayout cell = UiFactory.row().gap(2);
            cell.child(UiFactory.muted(name).horizontalSizing(Sizing.content()));
            var input = UiFactory.textBox(values[index], value -> {
                values[axis] = value;
                context.mutate(() -> setter.accept(vectorValue(field.kind(), values)));
            });
            input.setHint(Component.literal("0"));
            input.tooltip(List.of(name));
            cell.child(input.horizontalSizing(Sizing.expand(100)));
            axes.add(cell);
        }
        return new CompactFieldLayout(axes, UiFactory.scaledPixels(64));
    }

    static String vectorValue(EntityTagFields.Kind kind, String[] values) {
        if (Arrays.stream(values).allMatch(String::isBlank)) return "";
        String[] components = Arrays.stream(values)
                .map(value -> value.isBlank() ? "0" : value)
                .toArray(String[]::new);
        return (kind == EntityTagFields.Kind.POSITION ? "[I;" : "[") + String.join(",", components) + "]";
    }

    private static final class DeclaredAction implements SpecialDataSearch.Field {
        private final String key;
        private final Runnable mutation;
        private List<Component> tooltip = List.of();

        private DeclaredAction(String key, Runnable mutation) {
            this.key = key;
            this.mutation = mutation;
        }

        public String key() {
            return key;
        }

        void tooltip(List<Component> tooltip) {
            this.tooltip = tooltip;
        }
    }

    private static DeclaredAction declaredAction(SpecialDataPanelContext context, String key, Runnable mutation) {
        return new DeclaredAction(key, mutation);
    }

    static void actions(
            SpecialDataPanelContext context,
            ItemEditorState.EntitySpawnDraft draft,
            EntityTagFields.Field field,
            Consumer<String> setter,
            List<ButtonComponent> actions) {
        declareActions(context, draft, field, setter, declaration -> {
            ButtonComponent button = action(context, declaration.key(), declaration.mutation);
            if (!declaration.tooltip.isEmpty()) button.tooltip(declaration.tooltip);
            actions.add(button);
        });
    }

    static List<EditorSearchDialog.Target> searchTargets(
            SpecialDataPanelContext context,
            ItemEditorState.EntitySpawnDraft draft,
            EntityTagFields.Field field,
            List<String> path,
            String scope,
            Runnable expand) {
        var result = new ArrayList<EditorSearchDialog.Target>();
        declareActions(
                context,
                draft,
                field,
                ignored -> {},
                declaration -> result.addAll(SpecialDataSearch.targets(
                        context, EditorCategory.SPECIAL_DATA, path, scope, expand, declaration)));
        return result;
    }

    private static void declareActions(
            SpecialDataPanelContext context,
            ItemEditorState.EntitySpawnDraft draft,
            EntityTagFields.Field field,
            Consumer<String> setter,
            Consumer<DeclaredAction> actions) {
        var player = context.screen().session().minecraft().player;
        if (!field.key().equals("UUID") && field.kind() == EntityTagFields.Kind.UUID && player != null) {
            actions.accept(declaredAction(
                    context,
                    "special.entity.tags.me",
                    () -> setter.accept(player.getUUID().toString())));
        }
        if (breedingAge(draft.entityId, field.key()) && !draft.entityId.equals("minecraft:parrot")) {
            actions.accept(declaredAction(
                    context,
                    "special.entity.tags.baby",
                    () -> setter.accept(Integer.toString(AgeableMob.BABY_START_AGE))));
            actions.accept(declaredAction(context, "special.entity.tags.adult", () -> setter.accept("0")));
        }
        if (field.key().equals("InLove") && !draft.entityId.equals("minecraft:parrot")) {
            DeclaredAction start = declaredAction(context, "special.entity.tags.start_love", () -> {
                draft.entityTagEdits.put("Age", "0");
                setter.accept("600");
            });
            start.tooltip(List.of(ItemEditorText.tr("special.entity.tags.love_requires_adult")));
            actions.accept(start);
            actions.accept(declaredAction(context, "special.entity.tags.stop_love", () -> setter.accept("0")));
        }
        if (player != null
                && (field.kind() == EntityTagFields.Kind.POSITION || field.key().equals("Pos"))) {
            if (!field.key().equals("sleeping_pos")) {
                DeclaredAction here = declaredAction(
                        context,
                        field.key().equals("home_pos") ? "special.entity.tags.home_here" : "special.entity.tags.here",
                        () -> {
                            setter.accept(
                                    field.kind() == EntityTagFields.Kind.POSITION
                                            ? new IntArrayTag(new int[] {
                                                        player.getBlockX(), player.getBlockY(), player.getBlockZ()
                                                    })
                                                    .toString()
                                            : "[" + player.getX() + "," + player.getY() + "," + player.getZ() + "]");
                            if (field.key().equals("home_pos")) {
                                String radius = draft.entityTagEdits.getOrDefault(
                                        "home_radius",
                                        Integer.toString(draft.originalEntityTag.getIntOr("home_radius", -1)));
                                if (radius.isBlank() || radius.equals("-1"))
                                    draft.entityTagEdits.put("home_radius", "16");
                            }
                        });
                here.tooltip(List.of(ItemEditorText.tr(
                        field.key().equals("home_pos")
                                ? "special.entity.tags.home_here_help"
                                : "special.entity.tags.help.Pos")));
                actions.accept(here);
            }
        }
        if (player != null && field.kind() == EntityTagFields.Kind.ROTATION) {
            actions.accept(declaredAction(
                    context,
                    "special.entity.tags.my_rotation",
                    () -> setter.accept("[" + player.getYRot() + "," + player.getXRot() + "]")));
        }
        var minecraft = context.screen().session().minecraft();
        if (field.key().equals("sleeping_pos")
                && minecraft.level != null
                && minecraft.hitResult instanceof BlockHitResult hit
                && hit.getType() == HitResult.Type.BLOCK
                && minecraft.level.getBlockState(hit.getBlockPos()).is(BlockTags.BEDS)) {
            actions.accept(declaredAction(
                    context,
                    "special.entity.tags.target_bed",
                    () -> setter.accept(BlockPos.CODEC
                            .encodeStart(NbtOps.INSTANCE, hit.getBlockPos())
                            .getOrThrow()
                            .toString())));
        }
        if (field.key().equals("Motion")) {
            actions.accept(declaredAction(context, "special.entity.tags.stop", () -> setter.accept("[0,0,0]")));
        }
        if (field.key().equals("anger_end_time")) {
            actions.accept(declaredAction(context, "special.entity.tags.calm", () -> {
                setter.accept("-1");
                draft.entityTagEdits.put("angry_at", "");
            }));
        }
        if (field.kind() == EntityTagFields.Kind.POSITION
                || field.kind() == EntityTagFields.Kind.MOTION
                || field.kind() == EntityTagFields.Kind.ROTATION) {
            actions.accept(declaredAction(context, "common.unset", () -> {
                setter.accept("");
                if (field.key().equals("home_pos")) draft.entityTagEdits.put("home_radius", "");
            }));
        }
    }

    static ButtonComponent action(SpecialDataPanelContext context, String key, Runnable mutation) {
        return UiFactory.button(
                ItemEditorText.tr(key), UiFactory.ButtonTextPreset.COMPACT, button -> context.mutateRefresh(mutation));
    }
}
