package me.noramibu.itemeditor.ui.panel;

import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.UIComponent;
import me.noramibu.itemeditor.ui.component.UiFactory;
import me.noramibu.itemeditor.ui.screen.ItemEditorScreen;
import me.noramibu.itemeditor.ui.panel.specialdata.BannerSpecialDataSection;
import me.noramibu.itemeditor.ui.panel.specialdata.BundleSpecialDataSection;
import me.noramibu.itemeditor.ui.panel.specialdata.BucketCreatureSpecialDataSection;
import me.noramibu.itemeditor.ui.panel.specialdata.ContainerSpecialDataSection;
import me.noramibu.itemeditor.ui.panel.specialdata.FireworkSpecialDataSection;
import me.noramibu.itemeditor.ui.panel.specialdata.MiscSpecialDataSections;
import me.noramibu.itemeditor.ui.panel.specialdata.PotionSpecialDataSection;
import me.noramibu.itemeditor.ui.panel.specialdata.SignSpecialDataSection;
import me.noramibu.itemeditor.ui.panel.specialdata.SpecialDataPanelContext;
import me.noramibu.itemeditor.ui.panel.specialdata.SpawnerSpecialDataSection;
import me.noramibu.itemeditor.ui.panel.specialdata.StewSpecialDataSection;

public final class SpecialDataEditorPanel implements EditorPanel {

    private final SpecialDataPanelContext context;

    public SpecialDataEditorPanel(ItemEditorScreen screen) {
        this.context = new SpecialDataPanelContext(screen);
    }

    @Override
    public UIComponent build() {
        var stack = this.context.originalStack();
        FlowLayout root = UiFactory.column();

        if (PotionSpecialDataSection.supports(stack)) {
            root.child(PotionSpecialDataSection.build(this.context));
        }
        if (StewSpecialDataSection.supports(stack)) {
            root.child(StewSpecialDataSection.build(this.context));
        }
        if (FireworkSpecialDataSection.supportsRocket(stack)) {
            root.child(FireworkSpecialDataSection.buildRocket(this.context));
        }
        if (FireworkSpecialDataSection.supportsStar(stack)) {
            root.child(FireworkSpecialDataSection.buildStar(this.context));
        }
        if (BannerSpecialDataSection.supports(stack)) {
            root.child(BannerSpecialDataSection.build(this.context));
        }
        if (BucketCreatureSpecialDataSection.supports(stack)) {
            root.child(BucketCreatureSpecialDataSection.build(this.context));
        }
        if (MiscSpecialDataSections.supportsDyed(stack)) {
            root.child(MiscSpecialDataSections.buildDyedColor(this.context));
        }
        if (MiscSpecialDataSections.supportsTrim(stack)) {
            root.child(MiscSpecialDataSections.buildTrim(this.context));
        }
        if (MiscSpecialDataSections.supportsProfile(stack)) {
            root.child(MiscSpecialDataSections.buildProfile(this.context));
        }
        if (MiscSpecialDataSections.supportsInstrument(stack)) {
            root.child(MiscSpecialDataSections.buildInstrument(this.context));
        }
        if (MiscSpecialDataSections.supportsJukebox(stack)) {
            root.child(MiscSpecialDataSections.buildJukebox(this.context));
        }
        if (MiscSpecialDataSections.supportsMap(stack)) {
            root.child(MiscSpecialDataSections.buildMap(this.context));
        }
        if (ContainerSpecialDataSection.supports(stack)) {
            root.child(ContainerSpecialDataSection.build(this.context));
        }
        if (BundleSpecialDataSection.supports(stack)) {
            root.child(BundleSpecialDataSection.build(this.context));
        }
        if (SignSpecialDataSection.supports(stack)) {
            root.child(SignSpecialDataSection.build(this.context));
        }
        if (SpawnerSpecialDataSection.supports(stack)) {
            root.child(SpawnerSpecialDataSection.build(this.context));
        }

        return root;
    }
}
