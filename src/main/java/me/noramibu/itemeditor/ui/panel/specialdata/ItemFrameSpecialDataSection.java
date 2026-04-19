package me.noramibu.itemeditor.ui.panel.specialdata;

import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Sizing;
import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.editor.text.RichTextDocument;
import me.noramibu.itemeditor.ui.component.StyledTextFieldSection;
import me.noramibu.itemeditor.ui.component.UiFactory;
import me.noramibu.itemeditor.util.ItemEditorCapabilities;
import me.noramibu.itemeditor.util.ItemEditorText;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public final class ItemFrameSpecialDataSection {
    private static final double COMPACT_LAYOUT_SCALE_THRESHOLD = 3.0d;
    private static final int COMPACT_LAYOUT_WIDTH_THRESHOLD = 560;
    private static final int NAME_EDITOR_HEIGHT = 54;
    private static final int VALUE_FIELD_WIDTH = 120;

    private ItemFrameSpecialDataSection() {
    }

    public static boolean supports(ItemStack stack) {
        return ItemEditorCapabilities.supportsItemFrameData(stack);
    }

    public static FlowLayout build(SpecialDataPanelContext context) {
        ItemEditorState.SpecialData special = context.special();
        FlowLayout section = UiFactory.section(ItemEditorText.tr("special.item_frame.title"), Component.empty());
        section.child(buildNameCard(context, special));
        section.child(buildFlagsCard(context, special));
        section.child(buildValuesCard(context, special));
        return section;
    }

    private static FlowLayout buildNameCard(SpecialDataPanelContext context, ItemEditorState.SpecialData special) {
        FlowLayout card = UiFactory.subCard();
        card.child(UiFactory.title(ItemEditorText.tr("special.item_frame.name")).shadow(false));

        StyledTextFieldSection.BoundEditor nameSection = StyledTextFieldSection.create(
                context.screen(),
                RichTextDocument.fromMarkup(special.itemFrameCustomName),
                Sizing.fill(100),
                UiFactory.fixed(NAME_EDITOR_HEIGHT),
                ItemEditorText.str("special.item_frame.name.placeholder"),
                StyledTextFieldSection.StylePreset.name(),
                ItemEditorText.str("special.item_frame.name.color_title"),
                ItemEditorText.str("special.item_frame.name.gradient_title"),
                "",
                "",
                null,
                document -> document.logicalLineCount() > 1
                        ? ItemEditorText.str("special.item_frame.name.single_line")
                        : null,
                document -> context.mutate(() -> special.itemFrameCustomName = document.toMarkup())
        );

        FlowLayout frame = UiFactory.framedEditorCard();
        frame.child(nameSection.toolbar());
        frame.child(nameSection.editor());
        frame.child(nameSection.validation());
        card.child(frame);
        return card;
    }

    private static FlowLayout buildFlagsCard(SpecialDataPanelContext context, ItemEditorState.SpecialData special) {
        boolean compactLayout = isCompactLayout(context);
        FlowLayout card = UiFactory.subCard();
        card.child(UiFactory.title(ItemEditorText.tr("special.item_frame.flags")).shadow(false));

        FlowLayout rowA = compactLayout ? UiFactory.column() : UiFactory.row();
        rowA.child(UiFactory.checkbox(
                ItemEditorText.tr("special.item_frame.invisible"),
                special.itemFrameInvisible,
                value -> context.mutateRefresh(() -> special.itemFrameInvisible = value)
        ));
        rowA.child(UiFactory.checkbox(
                ItemEditorText.tr("special.item_frame.fixed"),
                special.itemFrameFixed,
                value -> context.mutateRefresh(() -> special.itemFrameFixed = value)
        ));
        rowA.child(UiFactory.checkbox(
                ItemEditorText.tr("special.item_frame.no_gravity"),
                special.itemFrameNoGravity,
                value -> context.mutateRefresh(() -> special.itemFrameNoGravity = value)
        ));
        card.child(rowA);

        FlowLayout rowB = compactLayout ? UiFactory.column() : UiFactory.row();
        rowB.child(UiFactory.checkbox(
                ItemEditorText.tr("special.item_frame.invulnerable"),
                special.itemFrameInvulnerable,
                value -> context.mutateRefresh(() -> special.itemFrameInvulnerable = value)
        ));
        rowB.child(UiFactory.checkbox(
                ItemEditorText.tr("special.item_frame.name_visible"),
                special.itemFrameCustomNameVisible,
                value -> context.mutateRefresh(() -> special.itemFrameCustomNameVisible = value)
        ));
        card.child(rowB);
        return card;
    }

    private static FlowLayout buildValuesCard(SpecialDataPanelContext context, ItemEditorState.SpecialData special) {
        boolean compactLayout = isCompactLayout(context);
        FlowLayout card = UiFactory.subCard();
        card.child(UiFactory.title(ItemEditorText.tr("special.item_frame.values")).shadow(false));

        FlowLayout row = compactLayout ? UiFactory.column() : UiFactory.row();
        row.child(UiFactory.field(
                ItemEditorText.tr("special.item_frame.item_rotation"),
                Component.empty(),
                UiFactory.textBox(
                        special.itemFrameRotation,
                        context.bindText(value -> special.itemFrameRotation = value)
                ).horizontalSizing(compactLayout ? Sizing.fill(100) : UiFactory.fixed(VALUE_FIELD_WIDTH))
        ));
        row.child(UiFactory.field(
                ItemEditorText.tr("special.item_frame.item_drop_chance"),
                Component.empty(),
                UiFactory.textBox(
                        special.itemFrameDropChance,
                        context.bindText(value -> special.itemFrameDropChance = value)
                ).horizontalSizing(compactLayout ? Sizing.fill(100) : UiFactory.fixed(VALUE_FIELD_WIDTH))
        ));
        row.child(UiFactory.field(
                ItemEditorText.tr("special.item_frame.facing"),
                Component.empty(),
                UiFactory.textBox(
                        special.itemFrameFacing,
                        context.bindText(value -> special.itemFrameFacing = value)
                ).horizontalSizing(compactLayout ? Sizing.fill(100) : UiFactory.fixed(VALUE_FIELD_WIDTH))
        ));
        card.child(row);
        return card;
    }

    private static boolean isCompactLayout(SpecialDataPanelContext context) {
        return context.guiScale() >= COMPACT_LAYOUT_SCALE_THRESHOLD
                || context.panelWidthHint() < UiFactory.scaledPixels(COMPACT_LAYOUT_WIDTH_THRESHOLD);
    }
}
