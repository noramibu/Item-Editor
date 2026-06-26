package me.noramibu.itemeditor.ui.panel.specialdata;

import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.UIComponent;
import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.editor.text.RichTextDocument;
import me.noramibu.itemeditor.ui.component.StyledTextFieldSection;
import me.noramibu.itemeditor.ui.component.UiFactory;
import me.noramibu.itemeditor.util.ItemEditorText;
import me.noramibu.itemeditor.util.TextComponentUtil;
import net.minecraft.network.chat.Component;

final class EntitySpawnDataUi {

    private static final int HEALTH_FIELD_WIDTH = 120;
    private static final int NAME_EDITOR_HEIGHT = 54;

    private EntitySpawnDataUi() {
    }

    static UIComponent nameEditor(
            SpecialDataPanelContext context,
            ItemEditorState.EntitySpawnDraft draft,
            Component label,
            String placeholderKey,
            String colorTitleKey,
            String gradientTitleKey
    ) {
        StyledTextFieldSection.BoundEditor nameSection = StyledTextFieldSection.create(
                context.screen(),
                RichTextDocument.fromMarkup(draft.customName),
                Sizing.fill(100),
                UiFactory.fixed(NAME_EDITOR_HEIGHT),
                ItemEditorText.str(placeholderKey),
                StyledTextFieldSection.StylePreset.name(),
                ItemEditorText.str(colorTitleKey),
                ItemEditorText.str(gradientTitleKey),
                "",
                "",
                null,
                document -> document.logicalLineCount() > 1
                        ? ItemEditorText.str("special.spawn_egg.name.single_line")
                        : null,
                document -> context.mutate(() ->
                        draft.customName = TextComponentUtil.serializeEditorDocument(document))
        );

        FlowLayout frame = UiFactory.framedEditorCard();
        frame.child(nameSection.toolbar());
        frame.child(nameSection.editor());
        frame.child(nameSection.validation());
        return UiFactory.field(label, Component.empty(), frame);
    }

    static FlowLayout flags(
            SpecialDataPanelContext context,
            ItemEditorState.EntitySpawnDraft draft,
            int columns
    ) {
        FlowLayout group = UiFactory.column().gap(2);
        group.child(UiFactory.muted(ItemEditorText.tr("special.spawn_egg.flags")));

        addPackedRows(
                group,
                Math.max(1, columns),
                UiFactory.checkbox(ItemEditorText.tr("special.spawn_egg.no_ai"), draft.noAi,
                        value -> context.mutateRefresh(() -> draft.noAi = value)),
                UiFactory.checkbox(ItemEditorText.tr("special.spawn_egg.silent"), draft.silent,
                        value -> context.mutateRefresh(() -> draft.silent = value)),
                UiFactory.checkbox(ItemEditorText.tr("special.spawn_egg.no_gravity"), draft.noGravity,
                        value -> context.mutateRefresh(() -> draft.noGravity = value)),
                UiFactory.checkbox(ItemEditorText.tr("special.spawn_egg.glowing"), draft.glowing,
                        value -> context.mutateRefresh(() -> draft.glowing = value)),
                UiFactory.checkbox(ItemEditorText.tr("special.spawn_egg.invulnerable"), draft.invulnerable,
                        value -> context.mutateRefresh(() -> draft.invulnerable = value)),
                UiFactory.checkbox(ItemEditorText.tr("special.spawn_egg.persistent"), draft.persistenceRequired,
                        value -> context.mutateRefresh(() -> draft.persistenceRequired = value)),
                UiFactory.checkbox(ItemEditorText.tr("special.spawn_egg.name_visible"), draft.customNameVisible,
                        value -> context.mutateRefresh(() -> draft.customNameVisible = value))
        );
        return group;
    }

    static UIComponent health(
            SpecialDataPanelContext context,
            ItemEditorState.EntitySpawnDraft draft,
            boolean compactLayout
    ) {
        return UiFactory.field(
                ItemEditorText.tr("special.spawn_egg.health"),
                Component.empty(),
                UiFactory.textBox(draft.health, context.bindText(value -> draft.health = value))
                        .horizontalSizing(compactLayout ? Sizing.fill(100) : UiFactory.fixed(HEALTH_FIELD_WIDTH))
        );
    }

    private static void addPackedRows(FlowLayout group, int perRow, UIComponent... components) {
        for (int index = 0; index < components.length; index += perRow) {
            FlowLayout row = UiFactory.row();
            int rowEnd = Math.min(components.length, index + perRow);
            for (int componentIndex = index; componentIndex < rowEnd; componentIndex++) {
                row.child(components[componentIndex].horizontalSizing(Sizing.content()));
            }
            group.child(row);
        }
    }
}
