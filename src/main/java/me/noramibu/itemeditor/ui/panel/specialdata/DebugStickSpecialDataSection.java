package me.noramibu.itemeditor.ui.panel.specialdata;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.UIComponent;
import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.ui.component.UiFactory;
import me.noramibu.itemeditor.util.IdFieldNormalizer;
import me.noramibu.itemeditor.util.ItemEditorCapabilities;
import me.noramibu.itemeditor.util.ItemEditorText;
import me.noramibu.itemeditor.util.RegistryUtil;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.Comparator;
import java.util.List;

public final class DebugStickSpecialDataSection {
    private static final int EMPTY_HINT_WIDTH = 320;
    private static final int SUMMARY_WIDTH = 360;
    private static final int ROW_GAP = 4;

    private DebugStickSpecialDataSection() {
    }

    public static boolean supports(ItemStack stack) {
        return ItemEditorCapabilities.supportsDebugStickData(stack);
    }

    public static FlowLayout build(SpecialDataPanelContext context) {
        ItemEditorState.SpecialData special = context.special();
        FlowLayout card = UiFactory.card();
        card.child(UiFactory.title(ItemEditorText.tr("special.debug_stick.title")));

        ButtonComponent add = UiFactory.positiveButton(
                ItemEditorText.tr("special.debug_stick.add"),
                UiFactory.ButtonTextPreset.STANDARD,
                button -> context.mutateRefresh(() -> special.debugStickStates.add(new ItemEditorState.DebugStickStateDraft()))
        );
        ButtonComponent clear = UiFactory.negativeButton(
                ItemEditorText.tr("common.clear_all"),
                UiFactory.ButtonTextPreset.STANDARD,
                button -> context.mutateRefresh(special.debugStickStates::clear)
        );
        card.child(UiFactory.actionButtonRow(add, clear));

        if (special.debugStickStates.isEmpty()) {
            card.child(UiFactory.muted(ItemEditorText.tr("special.debug_stick.empty"), EMPTY_HINT_WIDTH));
            return card;
        }

        List<String> blockIds = blockIdsWithProperties(context);
        for (int index = 0; index < special.debugStickStates.size(); index++) {
            card.child(buildEntry(context, special, index, blockIds));
        }
        return card;
    }

    private static FlowLayout buildEntry(
            SpecialDataPanelContext context,
            ItemEditorState.SpecialData special,
            int index,
            List<String> blockIds
    ) {
        ItemEditorState.DebugStickStateDraft draft = special.debugStickStates.get(index);
        FlowLayout entry = UiFactory.subCard();
        entry.child(UiFactory.title(ItemEditorText.str("special.debug_stick.entry", index + 1)).shadow(false));
        entry.child(UiFactory.muted(summary(draft), SUMMARY_WIDTH));

        entry.child(OrderedListControls.actionRow(
                context,
                special.debugStickStates,
                index,
                ItemEditorState.DebugStickStateDraft::copy
        ));

        FlowLayout row = usesStackedRows(context) ? UiFactory.column() : UiFactory.row();
        row.gap(ROW_GAP);
        UIComponent block = blockField(context, draft, blockIds);
        UIComponent property = propertyField(context, draft);
        if (usesStackedRows(context)) {
            row.child(block.horizontalSizing(Sizing.fill(100)));
            row.child(property.horizontalSizing(Sizing.fill(100)));
        } else {
            row.child(block.horizontalSizing(Sizing.fill(50)));
            row.child(property.horizontalSizing(Sizing.fill(50)));
        }
        entry.child(row);
        return entry;
    }

    private static UIComponent blockField(
            SpecialDataPanelContext context,
            ItemEditorState.DebugStickStateDraft draft,
            List<String> blockIds
    ) {
        FlowLayout row = UiFactory.row();
        row.gap(ROW_GAP);
        row.child(UiFactory.textBox(
                draft.blockId,
                text -> context.mutate(() -> {
                    draft.blockId = IdFieldNormalizer.normalize(text);
                    if (!propertyNames(context, draft.blockId).contains(draft.propertyName)) {
                        draft.propertyName = "";
                    }
                })
        ).horizontalSizing(Sizing.expand(100)));
        ButtonComponent button = UiFactory.button(
                ItemEditorText.tr("common.pick"),
                UiFactory.ButtonTextPreset.STANDARD,
                anchor -> context.openSearchablePicker(
                        ItemEditorText.str("special.debug_stick.block"),
                        "",
                        blockIds,
                        id -> id,
                        id -> context.mutateRefresh(() -> {
                            draft.blockId = id;
                            if (!propertyNames(context, id).contains(draft.propertyName)) {
                                draft.propertyName = "";
                            }
                        })
                )
        );
        button.horizontalSizing(Sizing.fixed(buttonWidth(context)));
        row.child(button);
        return UiFactory.field(ItemEditorText.tr("special.debug_stick.block"), Component.empty(), row);
    }

    private static UIComponent propertyField(
            SpecialDataPanelContext context,
            ItemEditorState.DebugStickStateDraft draft
    ) {
        FlowLayout row = UiFactory.row();
        row.gap(ROW_GAP);
        row.child(UiFactory.textBox(
                draft.propertyName,
                context.bindText(value -> draft.propertyName = value == null ? "" : value.trim())
        ).horizontalSizing(Sizing.expand(100)));
        ButtonComponent button = UiFactory.button(
                ItemEditorText.tr("common.pick"),
                UiFactory.ButtonTextPreset.STANDARD,
                anchor -> context.openClearableDropdown(
                        anchor,
                        ItemEditorText.tr("common.none"),
                        () -> context.mutateRefresh(() -> draft.propertyName = ""),
                        propertyNames(context, draft.blockId),
                        value -> value,
                        value -> context.mutateRefresh(() -> draft.propertyName = value)
                )
        );
        button.active(draft.blockId != null && !draft.blockId.isBlank());
        button.horizontalSizing(Sizing.fixed(buttonWidth(context)));
        row.child(button);
        return UiFactory.field(ItemEditorText.tr("special.debug_stick.property"), Component.empty(), row);
    }

    private static String summary(ItemEditorState.DebugStickStateDraft draft) {
        if (draft.blockId == null || draft.blockId.isBlank()) {
            return ItemEditorText.str("special.debug_stick.summary_empty");
        }
        if (draft.propertyName == null || draft.propertyName.isBlank()) {
            return ItemEditorText.str("special.debug_stick.summary_block_only", draft.blockId);
        }
        return ItemEditorText.str("special.debug_stick.summary", draft.blockId, draft.propertyName);
    }

    private static List<String> blockIdsWithProperties(SpecialDataPanelContext context) {
        return context.registryIds(Registries.BLOCK).stream()
                .filter(id -> !propertyNames(context, id).isEmpty())
                .toList();
    }

    private static List<String> propertyNames(SpecialDataPanelContext context, String blockId) {
        if (blockId == null || blockId.isBlank()) {
            return List.of();
        }
        Registry<Block> registry = context.screen().session().registryAccess().lookupOrThrow(Registries.BLOCK);
        Holder<Block> block = RegistryUtil.resolveHolder(registry, blockId);
        if (block == null) {
            return List.of();
        }
        return block.value().defaultBlockState().getProperties().stream()
                .map(Property::getName)
                .sorted(Comparator.naturalOrder())
                .toList();
    }

    private static int buttonWidth(SpecialDataPanelContext context) {
        return Math.clamp(context.panelWidthHint() / 8, 48, 72);
    }

    private static boolean usesStackedRows(SpecialDataPanelContext context) {
        return context.isCompactPanel(620);
    }

}
