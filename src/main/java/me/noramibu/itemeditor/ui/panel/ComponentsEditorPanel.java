package me.noramibu.itemeditor.ui.panel;

import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.UIComponent;
import me.noramibu.itemeditor.ui.component.UiFactory;
import me.noramibu.itemeditor.ui.panel.specialdata.AdvancedItemSpecialDataSection;
import me.noramibu.itemeditor.ui.panel.specialdata.MiscSpecialDataSections;
import me.noramibu.itemeditor.ui.panel.specialdata.SpecialDataPanelContext;
import me.noramibu.itemeditor.ui.screen.ItemEditorScreen;
import me.noramibu.itemeditor.util.ItemEditorText;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

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
        var registryAccess = this.screen.session().registryAccess();
        FlowLayout root = UiFactory.column();
        List<ComponentSection> sections = List.of(
                new ComponentSection(
                        () -> AdvancedItemSpecialDataSection.supportsFoodConsumable(stack, registryAccess),
                        () -> AdvancedItemSpecialDataSection.buildFoodConsumable(this.context)
                ),
                new ComponentSection(
                        () -> AdvancedItemSpecialDataSection.supportsContainerMetadata(stack, registryAccess),
                        () -> AdvancedItemSpecialDataSection.buildContainerMetadata(this.context)
                ),
                new ComponentSection(
                        () -> AdvancedItemSpecialDataSection.supportsCrossbow(stack, registryAccess),
                        () -> AdvancedItemSpecialDataSection.buildCrossbow(this.context)
                ),
                new ComponentSection(
                        () -> MiscSpecialDataSections.supportsMap(stack),
                        () -> MiscSpecialDataSections.buildMap(this.context)
                ),
                new ComponentSection(
                        () -> AdvancedItemSpecialDataSection.supportsMapAdvanced(stack, registryAccess),
                        () -> AdvancedItemSpecialDataSection.buildMapAdvanced(this.context)
                ),
                new ComponentSection(
                        () -> AdvancedItemSpecialDataSection.supportsEquipmentCombat(stack, registryAccess),
                        () -> AdvancedItemSpecialDataSection.buildEquipmentCombat(this.context)
                ),
                new ComponentSection(
                        () -> AdvancedItemSpecialDataSection.supportsComponentTweaksNaming(stack, registryAccess),
                        () -> AdvancedItemSpecialDataSection.buildComponentTweakNamingSection(this.context)
                ),
                new ComponentSection(
                        () -> AdvancedItemSpecialDataSection.supportsComponentTweaksRegistry(stack, registryAccess),
                        () -> AdvancedItemSpecialDataSection.buildComponentTweakRegistrySection(this.context)
                ),
                new ComponentSection(
                        () -> AdvancedItemSpecialDataSection.supportsComponentTweaksBlock(stack, registryAccess),
                        () -> AdvancedItemSpecialDataSection.buildComponentTweakBlockSection(this.context)
                ),
                new ComponentSection(
                        () -> AdvancedItemSpecialDataSection.supportsComponentTweaksBehavior(stack, registryAccess),
                        () -> AdvancedItemSpecialDataSection.buildComponentTweakBehaviorSection(this.context)
                )
        );
        boolean hasAnySection = false;
        for (ComponentSection section : sections) {
            if (!section.supported().getAsBoolean()) {
                continue;
            }
            UiFactory.appendFillChild(root, section.builder().get());
            hasAnySection = true;
        }

        if (!hasAnySection) {
            int hintWidth = Math.max(1, this.screen.editorContentWidthHint());
            UiFactory.appendFillChild(root, UiFactory.muted(ItemEditorText.tr("category.components.empty"), hintWidth));
        }
        return root;
    }

    private record ComponentSection(BooleanSupplier supported, Supplier<UIComponent> builder) {
    }
}
