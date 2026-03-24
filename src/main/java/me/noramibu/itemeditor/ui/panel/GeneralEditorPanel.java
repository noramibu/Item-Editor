package me.noramibu.itemeditor.ui.panel;

import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.UIComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.editor.text.RichTextDocument;
import me.noramibu.itemeditor.ui.component.StyledTextFieldSection;
import me.noramibu.itemeditor.ui.component.UiFactory;
import me.noramibu.itemeditor.ui.screen.ItemEditorScreen;
import me.noramibu.itemeditor.util.ItemEditorCapabilities;
import me.noramibu.itemeditor.util.ItemEditorText;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

import java.util.Arrays;

public final class GeneralEditorPanel implements EditorPanel {

    private final ItemEditorScreen screen;

    public GeneralEditorPanel(ItemEditorScreen screen) {
        this.screen = screen;
    }

    @Override
    public UIComponent build() {
        ItemEditorState state = this.screen.session().state();
        ItemStack stack = this.screen.session().originalStack();
        boolean supportsDurability = ItemEditorCapabilities.supportsDurability(stack);
        boolean supportsRepairCost = ItemEditorCapabilities.supportsRepairCost(stack);

        FlowLayout root = UiFactory.column();
        root.child(this.buildIdentitySection(state));

        if (supportsDurability || supportsRepairCost) {
            root.child(this.buildDurabilitySection(state, supportsDurability, supportsRepairCost));
        }

        root.child(this.buildVisualOverridesSection(state));
        root.child(this.buildItemModelSection(state));
        return root;
    }

    private FlowLayout buildIdentitySection(ItemEditorState state) {
        FlowLayout identity = UiFactory.section(ItemEditorText.tr("general.identity.title"), Component.empty());

        StyledTextFieldSection.BoundEditor nameSection = StyledTextFieldSection.create(
                this.screen,
                RichTextDocument.fromMarkup(state.customName),
                Sizing.fill(100),
                Sizing.fixed(54),
                ItemEditorText.str("general.identity.custom_name.placeholder"),
                StyledTextFieldSection.StylePreset.name(),
                ItemEditorText.str("general.identity.custom_name.color_title"),
                ItemEditorText.str("general.identity.custom_name.gradient_title"),
                "",
                "",
                null,
                document -> document.logicalLineCount() > 1 ? ItemEditorText.str("general.identity.custom_name.single_line") : null,
                document -> PanelBindings.text(this.screen, value -> state.customName = value).accept(document.toMarkup())
        );

        FlowLayout editorFrame = UiFactory.framedEditorCard();
        editorFrame.child(nameSection.toolbar());
        editorFrame.child(nameSection.editor());
        editorFrame.child(nameSection.validation());
        identity.child(editorFrame);

        FlowLayout row = UiFactory.row();
        row.child(UiFactory.field(
                ItemEditorText.tr("general.stack_count"),
                Component.empty(),
                UiFactory.textBox(state.count, PanelBindings.text(this.screen, value -> state.count = value)).horizontalSizing(Sizing.fixed(120))
        ));

        var rarityButton = UiFactory.button(state.rarity, button -> this.screen.openDropdown(
                button,
                Arrays.asList(Rarity.values()),
                Rarity::name,
                PanelBindings.value(this.screen, rarity -> state.rarity = rarity.name())
        ));
        row.child(UiFactory.field(ItemEditorText.tr("general.rarity"), Component.empty(), rarityButton.horizontalSizing(Sizing.fixed(140))));
        identity.child(row);
        return identity;
    }

    private FlowLayout buildDurabilitySection(ItemEditorState state, boolean supportsDurability, boolean supportsRepairCost) {
        FlowLayout durability = UiFactory.section(ItemEditorText.tr("general.durability.title"), Component.empty());

        FlowLayout row = UiFactory.row();
        if (supportsDurability) {
            row.child(UiFactory.field(
                    ItemEditorText.tr("general.current_damage"),
                    Component.empty(),
                    UiFactory.textBox(state.currentDamage, PanelBindings.text(this.screen, value -> state.currentDamage = value)).horizontalSizing(Sizing.fixed(120))
            ));
            row.child(UiFactory.field(
                    ItemEditorText.tr("general.max_damage"),
                    Component.empty(),
                    UiFactory.textBox(state.maxDamage, PanelBindings.text(this.screen, value -> state.maxDamage = value)).horizontalSizing(Sizing.fixed(120))
            ));
        }
        if (supportsRepairCost) {
            row.child(UiFactory.field(
                    ItemEditorText.tr("general.repair_cost"),
                    Component.empty(),
                    UiFactory.textBox(state.repairCost, PanelBindings.text(this.screen, value -> state.repairCost = value)).horizontalSizing(Sizing.fixed(120))
            ));
        }
        durability.child(row);

        if (supportsDurability) {
            durability.child(UiFactory.checkbox(ItemEditorText.tr("general.unbreakable"), state.unbreakable, PanelBindings.toggle(this.screen, value -> state.unbreakable = value)));
        }
        return durability;
    }

    private FlowLayout buildVisualOverridesSection(ItemEditorState state) {
        FlowLayout visual = UiFactory.section(ItemEditorText.tr("general.visual_overrides.title"), Component.empty());
        visual.child(UiFactory.checkbox(ItemEditorText.tr("general.glint_override.enable"), state.glintOverrideEnabled, PanelBindings.toggle(this.screen, value -> state.glintOverrideEnabled = value)));
        visual.child(UiFactory.checkbox(ItemEditorText.tr("general.glint_override.force"), state.glintOverride, PanelBindings.toggle(this.screen, value -> state.glintOverride = value)));
        return visual;
    }

    private FlowLayout buildItemModelSection(ItemEditorState state) {
        FlowLayout itemModel = UiFactory.section(ItemEditorText.tr("general.item_model.title"), Component.empty());
        itemModel.child(UiFactory.field(
                ItemEditorText.tr("general.item_model.id"),
                Component.empty(),
                UiFactory.textBox(state.itemModelId, PanelBindings.text(this.screen, value -> state.itemModelId = value))
        ));

        FlowLayout customModelRow = UiFactory.row();
        customModelRow.child(UiFactory.field(
                ItemEditorText.tr("general.item_model.float"),
                Component.empty(),
                UiFactory.textBox(state.customModelFloat, PanelBindings.text(this.screen, value -> state.customModelFloat = value)).horizontalSizing(Sizing.fixed(140))
        ));
        customModelRow.child(UiFactory.field(
                ItemEditorText.tr("general.item_model.string"),
                Component.empty(),
                UiFactory.textBox(state.customModelString, PanelBindings.text(this.screen, value -> state.customModelString = value)).horizontalSizing(Sizing.fixed(160))
        ));
        itemModel.child(customModelRow);

        FlowLayout customModelRowTwo = UiFactory.row();
        customModelRowTwo.child(UiFactory.field(
                ItemEditorText.tr("general.item_model.color"),
                Component.empty(),
                UiFactory.textBox(state.customModelColor, PanelBindings.text(this.screen, value -> state.customModelColor = value)).horizontalSizing(Sizing.fixed(140))
        ));
        customModelRowTwo.child(UiFactory.checkbox(ItemEditorText.tr("general.item_model.flag_enable"), state.customModelFlagEnabled, PanelBindings.toggle(this.screen, value -> state.customModelFlagEnabled = value)));
        customModelRowTwo.child(UiFactory.checkbox(ItemEditorText.tr("general.item_model.flag_value"), state.customModelFlag, PanelBindings.toggle(this.screen, value -> state.customModelFlag = value)));
        itemModel.child(customModelRowTwo);
        return itemModel;
    }
}
