package me.noramibu.itemeditor.ui.panel;

import io.wispforest.owo.ui.core.UIComponent;
import java.util.ArrayList;
import java.util.List;
import me.noramibu.itemeditor.editor.EditorCategory;
import me.noramibu.itemeditor.ui.component.EditorSearchDialog;
import me.noramibu.itemeditor.ui.panel.specialdata.AdvancedItemSpecialDataSection;
import me.noramibu.itemeditor.ui.panel.specialdata.EquippableSpecialDataSection;
import me.noramibu.itemeditor.ui.panel.specialdata.MiscSpecialDataSections;
import me.noramibu.itemeditor.ui.panel.specialdata.RegistrySpecialDataSection;
import me.noramibu.itemeditor.ui.panel.specialdata.SpecialDataPanelContext;
import me.noramibu.itemeditor.ui.screen.ItemEditorScreen;
import me.noramibu.itemeditor.util.ItemEditorText;

public final class ComponentsEditorPanel implements EditorPanel {
    private final ItemEditorScreen screen;
    private final SpecialDataPanelContext context;

    public ComponentsEditorPanel(ItemEditorScreen screen) {
        this.screen = screen;
        this.context = new SpecialDataPanelContext(screen);
    }

    @Override
    public List<EditorSearchDialog.Target> searchTargets() {
        List<EditorSearchDialog.Target> targets = new ArrayList<>();
        targets.addAll(AdvancedItemSpecialDataSection.searchTargets(this.context));
        targets.addAll(RegistrySpecialDataSection.searchTargets(this.context));
        targets.addAll(EquippableSpecialDataSection.searchTargets(this.context));
        targets.addAll(MiscSpecialDataSections.searchDyeTargets(this.context, EditorCategory.COMPONENTS));
        targets.addAll(MiscSpecialDataSections.searchMapTargets(this.context, EditorCategory.COMPONENTS));
        return List.copyOf(targets);
    }

    @Override
    public UIComponent build() {
        var stack = this.screen.session().originalStack();
        List<SectionedEditorPanel.Section> sections = List.of(
                SectionedEditorPanel.Section.always(
                        () -> AdvancedItemSpecialDataSection.buildComponentTweakNamingSection(this.context)),
                SectionedEditorPanel.Section.always(() -> RegistrySpecialDataSection.build(this.context)),
                SectionedEditorPanel.Section.always(() -> EquippableSpecialDataSection.build(this.context)),
                SectionedEditorPanel.Section.always(
                        () -> AdvancedItemSpecialDataSection.buildFoodConsumable(this.context)),
                new SectionedEditorPanel.Section(
                        () -> AdvancedItemSpecialDataSection.supportsBlockState(stack),
                        () -> AdvancedItemSpecialDataSection.buildBlockState(this.context)),
                new SectionedEditorPanel.Section(
                        () -> MiscSpecialDataSections.supportsDye(stack),
                        () -> MiscSpecialDataSections.buildDye(this.context)),
                new SectionedEditorPanel.Section(
                        () -> AdvancedItemSpecialDataSection.supportsCrossbow(stack),
                        () -> AdvancedItemSpecialDataSection.buildCrossbow(this.context)),
                new SectionedEditorPanel.Section(
                        () -> MiscSpecialDataSections.supportsMap(stack),
                        () -> MiscSpecialDataSections.buildMap(this.context)),
                new SectionedEditorPanel.Section(
                        () -> AdvancedItemSpecialDataSection.supportsMapAdvanced(stack),
                        () -> AdvancedItemSpecialDataSection.buildMapAdvanced(this.context)),
                new SectionedEditorPanel.Section(
                        () -> AdvancedItemSpecialDataSection.supportsContainerMetadata(stack),
                        () -> AdvancedItemSpecialDataSection.buildContainerMetadata(this.context)),
                SectionedEditorPanel.Section.always(
                        () -> AdvancedItemSpecialDataSection.buildCustomData(this.context)));
        return SectionedEditorPanel.build(
                sections, ItemEditorText.tr("category.components.empty"), this.screen.editorContentWidthHint());
    }
}
