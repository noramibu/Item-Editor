package me.noramibu.itemeditor.ui.panel.specialdata;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.UIComponent;
import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.editor.text.RichTextDocument;
import me.noramibu.itemeditor.ui.component.StyledTextFieldSection;
import me.noramibu.itemeditor.ui.component.UiFactory;
import me.noramibu.itemeditor.ui.util.LayoutModeUtil;
import me.noramibu.itemeditor.util.ItemEditorCapabilities;
import me.noramibu.itemeditor.util.ItemEditorText;
import me.noramibu.itemeditor.util.TextComponentUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.function.Consumer;

public final class CommandBlockSpecialDataSection {

    private static final int COMPACT_LAYOUT_WIDTH_THRESHOLD = 560;
    private static final int COMMAND_EDITOR_HEIGHT = 78;
    private static final int NAME_EDITOR_HEIGHT = 54;
    private static final int LAST_OUTPUT_EDITOR_HEIGHT = 68;
    private static final int NUMBER_FIELD_WIDTH = 150;
    private static final int HINT_WIDTH = 360;
    private static final int HINT_WIDTH_RESERVE = 28;
    private static final String COMMAND_BLOCK_NORMAL_ID = "minecraft:command_block";
    private static final String COMMAND_BLOCK_CHAIN_ID = "minecraft:chain_command_block";
    private static final String COMMAND_BLOCK_REPEATING_ID = "minecraft:repeating_command_block";

    private CommandBlockSpecialDataSection() {
    }

    public static boolean supports(ItemStack stack) {
        return ItemEditorCapabilities.supportsCommandBlockData(stack);
    }

    public static FlowLayout build(SpecialDataPanelContext context) {
        ItemEditorState.SpecialData special = context.special();
        boolean compactLayout = isCompactLayout(context);

        FlowLayout section = UiFactory.section(ItemEditorText.tr("special.command_block.title"), Component.empty());
        section.child(commandBlockTypeRow(context, special));
        section.child(UiFactory.muted(commandBlockMode(special.commandBlockItemId), HINT_WIDTH));
        section.child(commandField(context, special));
        section.child(richTextField(
                context,
                ItemEditorText.tr("special.command_block.custom_name"),
                special.commandBlockCustomName,
                NAME_EDITOR_HEIGHT,
                "special.command_block.custom_name.placeholder",
                "special.command_block.custom_name.color_title",
                "special.command_block.custom_name.gradient_title",
                document -> special.commandBlockCustomName = TextComponentUtil.serializeEditorDocument(document)
        ));
        section.child(activationCard(context, special, compactLayout));
        section.child(runtimeCard(context, special, compactLayout));
        return section;
    }

    private static FlowLayout commandBlockTypeRow(
            SpecialDataPanelContext context,
            ItemEditorState.SpecialData special
    ) {
        ButtonComponent normal = commandBlockTypeButton(
                context,
                special,
                ItemEditorText.tr("special.command_block.type.normal"),
                COMMAND_BLOCK_NORMAL_ID
        );
        ButtonComponent chain = commandBlockTypeButton(
                context,
                special,
                ItemEditorText.tr("special.command_block.type.chain"),
                COMMAND_BLOCK_CHAIN_ID
        );
        ButtonComponent repeating = commandBlockTypeButton(
                context,
                special,
                ItemEditorText.tr("special.command_block.type.repeating"),
                COMMAND_BLOCK_REPEATING_ID
        );
        return UiFactory.actionButtonRow(normal, chain, repeating);
    }

    private static ButtonComponent commandBlockTypeButton(
            SpecialDataPanelContext context,
            ItemEditorState.SpecialData special,
            Component label,
            String itemId
    ) {
        boolean selected = itemId.equals(commandBlockItemId(special.commandBlockItemId));
        ButtonComponent button = UiFactory.button(
                selected ? label.copy().withColor(0x6DFF8D) : label,
                UiFactory.ButtonTextPreset.COMPACT,
                ignored -> context.mutateRefresh(() -> special.commandBlockItemId = itemId)
        );
        button.active(!selected);
        return button;
    }

    private static FlowLayout activationCard(
            SpecialDataPanelContext context,
            ItemEditorState.SpecialData special,
            boolean compactLayout
    ) {
        FlowLayout card = UiFactory.subCard();
        card.child(UiFactory.title(ItemEditorText.tr("special.command_block.activation")).shadow(false));
        addPackedRows(
                card,
                compactLayout ? 2 : 3,
                UiFactory.checkbox(
                        ItemEditorText.tr("special.command_block.auto"),
                        special.commandBlockAuto,
                        value -> context.mutateRefresh(() -> special.commandBlockAuto = value)
                ),
                UiFactory.checkbox(
                        ItemEditorText.tr("special.command_block.powered"),
                        special.commandBlockPowered,
                        value -> context.mutateRefresh(() -> special.commandBlockPowered = value)
                ),
                UiFactory.checkbox(
                        ItemEditorText.tr("special.command_block.condition_met"),
                        special.commandBlockConditionMet,
                        value -> context.mutateRefresh(() -> special.commandBlockConditionMet = value)
                )
        );
        return card;
    }

    private static FlowLayout commandField(SpecialDataPanelContext context, ItemEditorState.SpecialData special) {
        FlowLayout field = UiFactory.column().gap(UiFactory.scaleProfile().tightSpacing());
        field.child(UiFactory.title(ItemEditorText.tr("special.command_block.command")).shadow(false));
        field.child(UiFactory.muted(
                ItemEditorText.tr("special.command_block.command_hint"),
                hintWidth(context)
        ));
        field.child(UiFactory.textArea(
                special.commandBlockCommand,
                COMMAND_EDITOR_HEIGHT,
                context.bindText(value -> special.commandBlockCommand = value)
        ).horizontalSizing(Sizing.fill(100)));
        return field;
    }

    private static FlowLayout runtimeCard(
            SpecialDataPanelContext context,
            ItemEditorState.SpecialData special,
            boolean compactLayout
    ) {
        FlowLayout card = UiFactory.subCard();
        FlowLayout header = UiFactory.row();
        header.child(UiFactory.title(ItemEditorText.tr("special.command_block.runtime"))
                .shadow(false)
                .horizontalSizing(Sizing.expand(100)));
        header.child(UiFactory.collapseToggleButton(
                special.uiCommandBlockRuntimeCollapsed,
                () -> context.mutateRefresh(() ->
                        special.uiCommandBlockRuntimeCollapsed = !special.uiCommandBlockRuntimeCollapsed)
        ));
        card.child(header);

        if (special.uiCommandBlockRuntimeCollapsed) {
            card.child(UiFactory.muted(runtimeSummary(special), HINT_WIDTH));
            return card;
        }

        addPackedRows(
                card,
                compactLayout ? 1 : 2,
                UiFactory.checkbox(
                        ItemEditorText.tr("special.command_block.track_output"),
                        special.commandBlockTrackOutput,
                        value -> context.mutateRefresh(() -> special.commandBlockTrackOutput = value)
                ),
                UiFactory.checkbox(
                        ItemEditorText.tr("special.command_block.update_last_execution"),
                        special.commandBlockUpdateLastExecution,
                        value -> context.mutateRefresh(() -> special.commandBlockUpdateLastExecution = value)
                )
        );
        addPackedRows(
                card,
                compactLayout ? 1 : 2,
                numericField(
                        context,
                        ItemEditorText.tr("special.command_block.success_count"),
                        special.commandBlockSuccessCount,
                        value -> special.commandBlockSuccessCount = value
                ),
                numericField(
                        context,
                        ItemEditorText.tr("special.command_block.last_execution"),
                        special.commandBlockLastExecution,
                        value -> special.commandBlockLastExecution = value
                )
        );
        UIComponent lastOutputEditor = richTextField(
                context,
                ItemEditorText.tr("special.command_block.last_output"),
                special.commandBlockLastOutput,
                LAST_OUTPUT_EDITOR_HEIGHT,
                "special.command_block.last_output.placeholder",
                "special.command_block.last_output.color_title",
                "special.command_block.last_output.gradient_title",
                document -> special.commandBlockLastOutput = TextComponentUtil.serializeEditorDocument(document)
        );
        card.child(lastOutputEditor);
        return card;
    }

    private static UIComponent numericField(
            SpecialDataPanelContext context,
            Component label,
            String value,
            Consumer<String> setter
    ) {
        return UiFactory.field(
                label,
                Component.empty(),
                UiFactory.textBox(value, context.bindText(setter))
                        .horizontalSizing(isCompactLayout(context) ? Sizing.fill(100) : UiFactory.fixed(NUMBER_FIELD_WIDTH))
        ).horizontalSizing(Sizing.fill(100));
    }

    private static UIComponent richTextField(
            SpecialDataPanelContext context,
            Component label,
            String markup,
            int height,
            String placeholderKey,
            String colorTitleKey,
            String gradientTitleKey,
            Consumer<RichTextDocument> setter
    ) {
        StyledTextFieldSection.BoundEditor editor = StyledTextFieldSection.create(
                context.screen(),
                RichTextDocument.fromMarkup(markup),
                Sizing.fill(100),
                UiFactory.fixed(height),
                ItemEditorText.str(placeholderKey),
                StyledTextFieldSection.StylePreset.name(),
                ItemEditorText.str(colorTitleKey),
                ItemEditorText.str(gradientTitleKey),
                "",
                "",
                null,
                document -> null,
                document -> context.mutate(() -> setter.accept(document))
        );

        FlowLayout frame = UiFactory.framedEditorCard();
        frame.child(editor.toolbar());
        frame.child(editor.editor());
        frame.child(editor.validation());
        return UiFactory.field(label, Component.empty(), frame);
    }

    private static String commandBlockMode(String itemId) {
        String normalized = commandBlockItemId(itemId);
        if (COMMAND_BLOCK_REPEATING_ID.equals(normalized)) {
            return ItemEditorText.str("special.command_block.mode.repeating");
        }
        if (COMMAND_BLOCK_CHAIN_ID.equals(normalized)) {
            return ItemEditorText.str("special.command_block.mode.chain");
        }
        return ItemEditorText.str("special.command_block.mode.impulse");
    }

    private static String commandBlockItemId(String itemId) {
        return switch (itemId == null ? "" : itemId) {
            case COMMAND_BLOCK_CHAIN_ID -> COMMAND_BLOCK_CHAIN_ID;
            case COMMAND_BLOCK_REPEATING_ID -> COMMAND_BLOCK_REPEATING_ID;
            default -> COMMAND_BLOCK_NORMAL_ID;
        };
    }

    private static String runtimeSummary(ItemEditorState.SpecialData special) {
        return ItemEditorText.str(
                "special.command_block.runtime_summary",
                special.commandBlockTrackOutput
                        ? ItemEditorText.str("special.command_block.enabled")
                        : ItemEditorText.str("special.command_block.disabled"),
                special.commandBlockSuccessCount.isBlank() ? "0" : special.commandBlockSuccessCount
        );
    }

    private static boolean isCompactLayout(SpecialDataPanelContext context) {
        return LayoutModeUtil.isCompactPanel(
                context.guiScale(),
                context.panelWidthHint(),
                COMPACT_LAYOUT_WIDTH_THRESHOLD
        );
    }

    private static int hintWidth(SpecialDataPanelContext context) {
        return Math.max(1, context.panelWidthHint() - UiFactory.scaledPixels(HINT_WIDTH_RESERVE));
    }

    private static void addPackedRows(FlowLayout parent, int perRow, UIComponent... components) {
        for (int index = 0; index < components.length; index += perRow) {
            FlowLayout row = UiFactory.row();
            int rowEnd = Math.min(components.length, index + perRow);
            int rowSize = rowEnd - index;
            int width = Math.max(1, (100 - rowSize) / Math.max(1, rowSize));
            for (int componentIndex = index; componentIndex < rowEnd; componentIndex++) {
                row.child(components[componentIndex].horizontalSizing(Sizing.fill(width)));
            }
            parent.child(row);
        }
    }
}
