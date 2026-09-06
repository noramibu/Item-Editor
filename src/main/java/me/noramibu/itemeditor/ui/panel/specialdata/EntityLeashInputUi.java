package me.noramibu.itemeditor.ui.panel.specialdata;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Sizing;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import me.noramibu.itemeditor.editor.EditorCategory;
import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.service.EntityTagFields;
import me.noramibu.itemeditor.ui.component.EditorSearchDialog;
import me.noramibu.itemeditor.ui.component.UiFactory;
import me.noramibu.itemeditor.util.ItemEditorText;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

final class EntityLeashInputUi {
    private EntityLeashInputUi() {}

    static List<EditorSearchDialog.Target> searchTargets(
            SpecialDataPanelContext context, List<String> path, String scope, Runnable expand) {
        return SpecialDataSearch.targets(context, EditorCategory.SPECIAL_DATA, path, scope, expand, Field.values());
    }

    static FlowLayout build(
            SpecialDataPanelContext context,
            ItemEditorState.EntitySpawnDraft draft,
            EntityTagFields.Field field,
            String current,
            Consumer<String> setter) {
        var minecraft = context.screen().session().minecraft();
        UUID self = ownUuid(draft);
        String targetValue = attachment(minecraft.crosshairPickEntity, minecraft.level, self);
        String fenceValue = null;
        if (minecraft.level != null
                && minecraft.hitResult instanceof BlockHitResult hit
                && hit.getType() == HitResult.Type.BLOCK
                && minecraft.level.getBlockState(hit.getBlockPos()).is(BlockTags.FENCES)) {
            fenceValue = BlockPos.CODEC
                    .encodeStart(NbtOps.INSTANCE, hit.getBlockPos())
                    .getOrThrow()
                    .toString();
        }

        FlowLayout result = UiFactory.column().gap(2);
        result.child(UiFactory.muted(summary(field, current)).horizontalSizing(Sizing.fill(100)));
        var input = UiFactory.textBox(
                uuidText(current), context.bindText(value -> setter.accept(withUuid(current, value))));
        input.setHint(Component.literal("UUID"));
        input.tooltip(List.of(Component.literal("UUID")));
        result.child(input.horizontalSizing(Sizing.fill(100)));
        result.child(UiFactory.packedActionButtonRow(
                UiFactory.button(
                        Field.LEASH_ATTACH_PLAYER.text(), UiFactory.ButtonTextPreset.COMPACT, button -> context.screen()
                                .openPlayerUuidPicker(
                                        Field.LEASH_ATTACH_PLAYER.text().getString(),
                                        new LinkedHashMap<>(EntitySpawnDataUi.onlinePlayers(context)),
                                        uuid -> {
                                            if (!UUID.fromString(uuid).equals(self))
                                                context.mutateRefresh(() -> setter.accept(withUuid(current, uuid)));
                                        })),
                action(context, Field.LEASH_ATTACH_TARGET, targetValue, setter),
                action(context, Field.LEASH_ATTACH_FENCE, fenceValue, setter),
                action(context, Field.UNSET, "", setter)));
        return result;
    }

    static String uuidText(String current) {
        CompoundTag tag = attachmentTag(current);
        return tag.read("UUID", UUIDUtil.CODEC).map(UUID::toString).orElseGet(() -> tag.getStringOr("UUID", ""));
    }

    static String withUuid(String current, String value) {
        if (value.isBlank()) return "";
        CompoundTag tag = attachmentTag(current);
        try {
            tag.store("UUID", UUIDUtil.CODEC, UUID.fromString(value.trim()));
        } catch (IllegalArgumentException exception) {
            tag.putString("UUID", value);
        }
        return tag.toString();
    }

    private static CompoundTag attachmentTag(String current) {
        try {
            Tag tag = TagParser.create(NbtOps.INSTANCE).parseFully(current);
            if (tag instanceof CompoundTag compound) return compound;
        } catch (Exception exception) {
            return new CompoundTag();
        }
        return new CompoundTag();
    }

    private static ButtonComponent action(
            SpecialDataPanelContext context, Field field, String value, Consumer<String> setter) {
        var button = UiFactory.button(field.text(), UiFactory.ButtonTextPreset.COMPACT, ignored -> {
            if (value != null) context.mutateRefresh(() -> setter.accept(value));
        });
        button.active(value != null);
        if (value != null && !value.isEmpty()) button.tooltip(List.of(Component.literal(value)));
        return button;
    }

    private static String attachment(Entity entity, Level level, UUID self) {
        if (entity == null
                || level == null
                || entity.level() != level
                || entity.isRemoved()
                || !entity.isAlive()
                || entity.getUUID().equals(self)) return null;
        CompoundTag attachment = new CompoundTag();
        attachment.store("UUID", UUIDUtil.CODEC, entity.getUUID());
        return attachment.toString();
    }

    private static UUID ownUuid(ItemEditorState.EntitySpawnDraft draft) {
        if (!draft.entityTagEdits.containsKey("UUID")) {
            return draft.originalEntityTag.read("UUID", UUIDUtil.CODEC).orElse(null);
        }
        try {
            return UUID.fromString(draft.entityTagEdits.get("UUID").trim());
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static Component summary(EntityTagFields.Field field, String current) {
        if (current.isBlank()) return ItemEditorText.tr("special.entity.tags.leash_none");
        try {
            Tag value = field.parse(current);
            var position = BlockPos.CODEC.parse(NbtOps.INSTANCE, value).result();
            if (position.isPresent()) {
                BlockPos pos = position.get();
                return ItemEditorText.tr("special.entity.tags.leash_fence", pos.getX(), pos.getY(), pos.getZ());
            }
            if (value instanceof CompoundTag compound) {
                var uuid = compound.read("UUID", UUIDUtil.CODEC);
                if (uuid.isPresent()) {
                    return Component.literal("UUID");
                }
            }
        } catch (Exception exception) {
            return ItemEditorText.tr("special.entity.tags.leash_imported");
        }
        return ItemEditorText.tr("special.entity.tags.leash_imported");
    }

    private enum Field implements SpecialDataSearch.Field {
        LEASH_ATTACH_PLAYER("special.entity.tags.leash_attach_player"),
        LEASH_ATTACH_TARGET("special.entity.tags.leash_attach_target"),
        LEASH_ATTACH_FENCE("special.entity.tags.leash_attach_fence"),
        UNSET("common.unset");

        private final String key;

        Field(String key) {
            this.key = key;
        }

        public String key() {
            return key;
        }
    }
}
