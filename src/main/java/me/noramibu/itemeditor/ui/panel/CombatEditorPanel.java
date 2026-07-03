package me.noramibu.itemeditor.ui.panel;

import io.wispforest.owo.ui.core.UIComponent;
import me.noramibu.itemeditor.ui.panel.specialdata.CombatSpecialDataSection;
import me.noramibu.itemeditor.ui.panel.specialdata.SpecialDataPanelContext;
import me.noramibu.itemeditor.ui.screen.ItemEditorScreen;

import java.util.List;

public final class CombatEditorPanel implements EditorPanel {
    private final SpecialDataPanelContext context;

    public CombatEditorPanel(ItemEditorScreen screen) {
        this.context = new SpecialDataPanelContext(screen);
    }

    @Override
    public UIComponent build() {
        return SectionedEditorPanel.build(List.of(
                SectionedEditorPanel.Section.always(
                        () -> CombatSpecialDataSection.buildDamageResistant(this.context)
                ),
                SectionedEditorPanel.Section.always(
                        () -> CombatSpecialDataSection.buildBehavior(this.context)
                ),
                SectionedEditorPanel.Section.always(
                        () -> CombatSpecialDataSection.buildBlocksAttacks(this.context)
                ),
                SectionedEditorPanel.Section.always(
                        () -> CombatSpecialDataSection.buildEquipment(this.context)
                )
        ));
    }
}
