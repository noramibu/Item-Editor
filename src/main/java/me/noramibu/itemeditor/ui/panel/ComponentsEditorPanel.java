package me.noramibu.itemeditor.ui.panel;

import io.wispforest.owo.ui.core.UIComponent;
import me.noramibu.itemeditor.ui.panel.specialdata.AdvancedItemSpecialDataSection;
import me.noramibu.itemeditor.ui.panel.specialdata.EquippableSpecialDataSection;
import me.noramibu.itemeditor.ui.panel.specialdata.MiscSpecialDataSections;
import me.noramibu.itemeditor.ui.panel.specialdata.RegistrySpecialDataSection;
import me.noramibu.itemeditor.ui.panel.specialdata.SpecialDataPanelContext;
import me.noramibu.itemeditor.ui.screen.ItemEditorScreen;
import me.noramibu.itemeditor.util.ItemEditorText;

import java.util.List;

public final class ComponentsEditorPanel implements EditorPanel {
    private final ItemEditorScreen screen;
    private final SpecialDataPanelContext context;

    public ComponentsEditorPanel(ItemEditorScreen screen) {
        this.screen = screen;
        this.context = new SpecialDataPanelContext(screen);
    }

    @Override
    public UIComponent build() {
        var stack = this.screen.session().originalStack();
        List<SectionedEditorPanel.Section> sections = List.of(
                SectionedEditorPanel.Section.always(
                        () -> AdvancedItemSpecialDataSection.buildComponentTweakNamingSection(this.context)
                ),
                SectionedEditorPanel.Section.always(
                        () -> RegistrySpecialDataSection.build(this.context)
                ),
                SectionedEditorPanel.Section.always(
                        () -> EquippableSpecialDataSection.build(this.context)
                ),
                SectionedEditorPanel.Section.always(
                        () -> AdvancedItemSpecialDataSection.buildFoodConsumable(this.context)
                ),
                new SectionedEditorPanel.Section(
                        () -> AdvancedItemSpecialDataSection.supportsBlockState(stack),
                        () -> AdvancedItemSpecialDataSection.buildBlockState(this.context)
                ),
                new SectionedEditorPanel.Section(
                        () -> MiscSpecialDataSections.supportsDye(stack),
                        () -> MiscSpecialDataSections.buildDye(this.context)
                ),
                new SectionedEditorPanel.Section(
                        () -> AdvancedItemSpecialDataSection.supportsCrossbow(stack),
                        () -> AdvancedItemSpecialDataSection.buildCrossbow(this.context)
                ),
                new SectionedEditorPanel.Section(
                        () -> MiscSpecialDataSections.supportsMap(stack),
                        () -> MiscSpecialDataSections.buildMap(this.context)
                ),
                new SectionedEditorPanel.Section(
                        () -> AdvancedItemSpecialDataSection.supportsMapAdvanced(stack),
                        () -> AdvancedItemSpecialDataSection.buildMapAdvanced(this.context)
                ),
                new SectionedEditorPanel.Section(
                        () -> AdvancedItemSpecialDataSection.supportsContainerMetadata(stack),
                        () -> AdvancedItemSpecialDataSection.buildContainerMetadata(this.context)
                ),
                SectionedEditorPanel.Section.always(
                        () -> AdvancedItemSpecialDataSection.buildCustomData(this.context)
                )
        );
        return SectionedEditorPanel.build(
                sections,
                ItemEditorText.tr("category.components.empty"),
                this.screen.editorContentWidthHint()
        );
    }
}
