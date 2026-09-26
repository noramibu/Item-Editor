package me.noramibu.itemeditor.ui.panel.specialdata;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.UIComponent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import me.noramibu.itemeditor.editor.EditorCategory;
import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.editor.text.RichTextDocument;
import me.noramibu.itemeditor.ui.component.CommandEditorControls;
import me.noramibu.itemeditor.ui.component.CommandValidationLabel;
import me.noramibu.itemeditor.ui.component.EditorSearchDialog;
import me.noramibu.itemeditor.ui.component.RawTextAreaComponent;
import me.noramibu.itemeditor.ui.component.StyledTextFieldSection;
import me.noramibu.itemeditor.ui.component.UiFactory;
import me.noramibu.itemeditor.ui.component.raw.CommandSuggestions;
import me.noramibu.itemeditor.ui.screen.CommandEditorScreen;
import me.noramibu.itemeditor.util.ItemEditorCapabilities;
import me.noramibu.itemeditor.util.ItemEditorText;
import me.noramibu.itemeditor.util.TextComponentUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public final class CommandBlockSpecialDataSection {
    public static List<EditorSearchDialog.Target> searchTargets(SpecialDataPanelContext context) {
        var result = new ArrayList<>(SpecialDataSearch.targets(
                context,
                EditorCategory.SPECIAL_DATA,
                "special.command_block.title",
                "command-block",
                () -> {},
                Field.values()));
        result.addAll(SpecialDataSearch.targets(
                context,
                EditorCategory.SPECIAL_DATA,
                List.of(
                        ItemEditorText.str("special.command_block.title"),
                        ItemEditorText.str("special.command_block.runtime")),
                "command-block",
                () -> context.special().uiCommandBlockRuntimeCollapsed = false,
                RuntimeField.values()));
        return result;
    }

    private static final int COMPACT_LAYOUT_WIDTH_THRESHOLD = 560;
    private static final int COMMAND_EDITOR_HEIGHT = 240;
    private static final int NAME_EDITOR_HEIGHT = 54;
    private static final int LAST_OUTPUT_EDITOR_HEIGHT = 68;
    private static final int NUMBER_FIELD_WIDTH = 150;
    private static final int HINT_WIDTH = 360;
    private static final int HINT_WIDTH_RESERVE = 28;
    private static final String COMMAND_BLOCK_NORMAL_ID = "minecraft:command_block";
    private static final String COMMAND_BLOCK_CHAIN_ID = "minecraft:chain_command_block";
    private static final String COMMAND_BLOCK_REPEATING_ID = "minecraft:repeating_command_block";

    private CommandBlockSpecialDataSection() {}

    public static boolean supports(ItemStack stack) {
        return ItemEditorCapabilities.supportsCommandBlockData(stack);
    }

    public static FlowLayout build(SpecialDataPanelContext context) {
        ItemEditorState.SpecialData special = context.special();
        boolean compactLayout = isCompactLayout(context);

        FlowLayout section = UiFactory.section(ItemEditorText.tr("special.command_block.title"), Component.empty());
        section.id("command-block");
        section.child(commandBlockTypeRow(context, special));
        section.child(UiFactory.muted(commandBlockMode(special.commandBlockItemId), HINT_WIDTH));
        section.child(commandField(context, special));
        section.child(richTextField(
                context,
                Field.CUSTOM_NAME,
                special.commandBlockCustomName,
                NAME_EDITOR_HEIGHT,
                "special.command_block.custom_name.placeholder",
                "special.command_block.custom_name.color_title",
                "special.command_block.custom_name.gradient_title",
                document -> special.commandBlockCustomName = TextComponentUtil.serializeEditorDocument(document)));
        section.child(activationCard(context, special));
        section.child(runtimeCard(context, special, compactLayout));
        return section;
    }

    private static FlowLayout commandBlockTypeRow(
            SpecialDataPanelContext context, ItemEditorState.SpecialData special) {
        ButtonComponent normal = commandBlockTypeButton(context, special, Field.NORMAL.text(), COMMAND_BLOCK_NORMAL_ID);
        ButtonComponent chain = commandBlockTypeButton(context, special, Field.CHAIN.text(), COMMAND_BLOCK_CHAIN_ID);
        ButtonComponent repeating =
                commandBlockTypeButton(context, special, Field.REPEATING.text(), COMMAND_BLOCK_REPEATING_ID);
        return UiFactory.actionButtonRow(normal, chain, repeating);
    }

    private static ButtonComponent commandBlockTypeButton(
            SpecialDataPanelContext context, ItemEditorState.SpecialData special, Component label, String itemId) {
        boolean selected = itemId.equals(commandBlockItemId(special.commandBlockItemId));
        ButtonComponent button = UiFactory.button(
                selected ? label.copy().withColor(0x6DFF8D) : label,
                UiFactory.ButtonTextPreset.COMPACT,
                ignored -> context.mutateRefresh(() -> special.commandBlockItemId = itemId));
        button.active(!selected);
        return button;
    }

    private static FlowLayout activationCard(SpecialDataPanelContext context, ItemEditorState.SpecialData special) {
        FlowLayout card = UiFactory.subCard();
        card.child(UiFactory.title(ItemEditorText.tr("special.command_block.activation"))
                .shadow(false));
        card.child(UiFactory.checkbox(
                Field.AUTO.text(),
                special.commandBlockAuto,
                value -> context.mutateRefresh(() -> special.commandBlockAuto = value)));
        return card;
    }

    private static FlowLayout commandField(SpecialDataPanelContext context, ItemEditorState.SpecialData special) {
        FlowLayout field = UiFactory.column().gap(UiFactory.scaleProfile().tightSpacing());
        field.child(UiFactory.title(Field.COMMAND.text()).shadow(false));
        field.child(UiFactory.button(
                        ItemEditorText.tr("raw_editor.focus.enter"),
                        UiFactory.ButtonTextPreset.COMPACT,
                        button -> Minecraft.getInstance()
                                .setScreenAndShow(new CommandEditorScreen(
                                        context.screen(),
                                        special.commandBlockCommand,
                                        value -> context.mutateRefresh(() -> special.commandBlockCommand = value))))
                .horizontalSizing(Sizing.fill(100)));
        field.child(UiFactory.muted(
                ItemEditorText.tr("special.command_block.command_hint"),
                Math.max(1, context.panelWidthHint() - UiFactory.scaledPixels(HINT_WIDTH_RESERVE))));
        int height = Math.clamp(
                context.screen().height / 2, UiFactory.scaledPixels(96), UiFactory.scaledPixels(COMMAND_EDITOR_HEIGHT));
        RawTextAreaComponent editor = new RawTextAreaComponent(
                        Sizing.fill(100), Sizing.fixed(height), special.commandBlockCommand)
                .commandMode(true)
                .wordWrap(true);
        editor.onChanged()
                .subscribe((value, delta) -> context.mutate(() -> special.commandBlockCommand = editor.commandValue()));
        var status = new CommandValidationLabel(editor);
        var suggestionStatus = UiFactory.muted(Component.empty());
        new CommandSuggestions(
                editor,
                () -> Minecraft.getInstance().gui.screen() == context.screen() && editor.hasParent(),
                suggestionStatus::text);
        field.child(CommandEditorControls.create(editor));
        field.child(editor);
        field.child(status);
        field.child(suggestionStatus);
        return field;
    }

    private static FlowLayout runtimeCard(
            SpecialDataPanelContext context, ItemEditorState.SpecialData special, boolean compactLayout) {
        FlowLayout card = UiFactory.subCard();
        FlowLayout header = UiFactory.collapsibleHeader(
                UiFactory.title(ItemEditorText.tr("special.command_block.runtime"))
                        .shadow(false),
                special.uiCommandBlockRuntimeCollapsed,
                () -> context.mutateRefresh(
                        () -> special.uiCommandBlockRuntimeCollapsed = !special.uiCommandBlockRuntimeCollapsed));
        card.child(header);

        if (special.uiCommandBlockRuntimeCollapsed) {
            card.child(UiFactory.muted(runtimeSummary(special), HINT_WIDTH));
            return card;
        }

        card.child(UiFactory.muted(ItemEditorText.tr("command_editor.runtime_hint"))
                .horizontalSizing(Sizing.fill(100)));
        UiFactory.addPackedRows(
                card,
                compactLayout ? 1 : 2,
                UiFactory.checkbox(
                        RuntimeField.POWERED.text(),
                        special.commandBlockPowered,
                        value -> context.mutateRefresh(() -> special.commandBlockPowered = value)),
                UiFactory.checkbox(
                        RuntimeField.CONDITION_MET.text(),
                        special.commandBlockConditionMet,
                        value -> context.mutateRefresh(() -> special.commandBlockConditionMet = value)));

        UiFactory.addPackedRows(
                card,
                compactLayout ? 1 : 2,
                UiFactory.checkbox(
                        RuntimeField.TRACK_OUTPUT.text(),
                        special.commandBlockTrackOutput,
                        value -> context.mutateRefresh(() -> special.commandBlockTrackOutput = value)),
                UiFactory.checkbox(
                        RuntimeField.UPDATE_LAST_EXECUTION.text(),
                        special.commandBlockUpdateLastExecution,
                        value -> context.mutateRefresh(() -> special.commandBlockUpdateLastExecution = value)));
        UiFactory.addPackedRows(
                card,
                compactLayout ? 1 : 2,
                numericField(context, RuntimeField.SUCCESS_COUNT),
                numericField(context, RuntimeField.LAST_EXECUTION));
        UIComponent lastOutputEditor = richTextField(
                context,
                RuntimeField.LAST_OUTPUT,
                special.commandBlockLastOutput,
                LAST_OUTPUT_EDITOR_HEIGHT,
                "special.command_block.last_output.placeholder",
                "special.command_block.last_output.color_title",
                "special.command_block.last_output.gradient_title",
                document -> special.commandBlockLastOutput = TextComponentUtil.serializeEditorDocument(document));
        card.child(lastOutputEditor);
        return card;
    }

    private static UIComponent numericField(SpecialDataPanelContext context, RuntimeField field) {
        return UiFactory.field(
                        field.text(),
                        Component.empty(),
                        context.boundTextBox(field.key())
                                .horizontalSizing(
                                        isCompactLayout(context)
                                                ? Sizing.fill(100)
                                                : UiFactory.fixed(NUMBER_FIELD_WIDTH)))
                .horizontalSizing(Sizing.fill(100));
    }

    private static UIComponent richTextField(
            SpecialDataPanelContext context,
            SpecialDataSearch.Field label,
            String markup,
            int height,
            String placeholderKey,
            String colorTitleKey,
            String gradientTitleKey,
            Consumer<RichTextDocument> setter) {
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
                document -> context.mutate(() -> setter.accept(document)));

        FlowLayout frame = UiFactory.framedEditorCard();
        frame.child(editor.toolbar());
        frame.child(editor.editor());
        frame.child(editor.validation());
        return UiFactory.field(label.text(), Component.empty(), frame);
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
                special.commandBlockSuccessCount.isBlank() ? "0" : special.commandBlockSuccessCount);
    }

    private static boolean isCompactLayout(SpecialDataPanelContext context) {
        return context.isCompactPanel(COMPACT_LAYOUT_WIDTH_THRESHOLD);
    }

    private enum Field implements SpecialDataSearch.Field {
        CUSTOM_NAME("common.custom_name"),
        NORMAL("special.command_block.type.normal"),
        CHAIN("special.command_block.type.chain"),
        REPEATING("special.command_block.type.repeating"),
        AUTO("special.command_block.auto"),
        COMMAND("special.command_block.command");

        private final String key;

        Field(String key) {
            this.key = key;
        }

        public String key() {
            return key;
        }
    }

    private enum RuntimeField implements SpecialDataSearch.Field {
        POWERED("special.command_block.powered"),
        CONDITION_MET("special.command_block.condition_met"),
        TRACK_OUTPUT("special.command_block.track_output"),
        UPDATE_LAST_EXECUTION("special.command_block.update_last_execution"),
        SUCCESS_COUNT("special.command_block.success_count"),
        LAST_EXECUTION("special.command_block.last_execution"),
        LAST_OUTPUT("special.command_block.last_output");

        private final String key;

        RuntimeField(String key) {
            this.key = key;
        }

        public String key() {
            return key;
        }
    }
}
