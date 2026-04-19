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
import me.noramibu.itemeditor.ui.panel.specialdata.ArmorStandSpecialDataSection;
import me.noramibu.itemeditor.ui.panel.specialdata.ItemFrameSpecialDataSection;
import me.noramibu.itemeditor.ui.panel.specialdata.MiscSpecialDataSections;
import me.noramibu.itemeditor.ui.panel.specialdata.PotionSpecialDataSection;
import me.noramibu.itemeditor.ui.panel.specialdata.SignSpecialDataSection;
import me.noramibu.itemeditor.ui.panel.specialdata.SpecialDataPanelContext;
import me.noramibu.itemeditor.ui.panel.specialdata.SpawnerSpecialDataSection;
import me.noramibu.itemeditor.ui.panel.specialdata.SpawnEggSpecialDataSection;
import me.noramibu.itemeditor.ui.panel.specialdata.StewSpecialDataSection;
import net.minecraft.network.chat.Component;

import java.util.function.Supplier;

public final class SpecialDataEditorPanel implements EditorPanel {
    private static final String EMPTY_SPECIAL_DATA_TEXT = "No special data editors available for this item.";

    private final SpecialDataPanelContext context;

    public SpecialDataEditorPanel(ItemEditorScreen screen) {
        this.context = new SpecialDataPanelContext(screen);
    }

    @Override
    public UIComponent build() {
        var stack = this.context.originalStack();
        FlowLayout root = UiFactory.column();
        int sectionCount = 0;

        if (this.addIf(root, PotionSpecialDataSection.supports(stack), () -> PotionSpecialDataSection.build(this.context))) sectionCount++;
        if (this.addIf(root, StewSpecialDataSection.supports(stack), () -> StewSpecialDataSection.build(this.context))) sectionCount++;
        if (this.addIf(root, FireworkSpecialDataSection.supportsRocket(stack), () -> FireworkSpecialDataSection.buildRocket(this.context))) sectionCount++;
        if (this.addIf(root, FireworkSpecialDataSection.supportsStar(stack), () -> FireworkSpecialDataSection.buildStar(this.context))) sectionCount++;
        if (this.addIf(root, BannerSpecialDataSection.supports(stack), () -> BannerSpecialDataSection.build(this.context))) sectionCount++;
        if (this.addIf(root, ArmorStandSpecialDataSection.supports(stack), () -> ArmorStandSpecialDataSection.build(this.context))) sectionCount++;
        if (this.addIf(root, ItemFrameSpecialDataSection.supports(stack), () -> ItemFrameSpecialDataSection.build(this.context))) sectionCount++;
        if (this.addIf(root, SpawnEggSpecialDataSection.supports(stack), () -> SpawnEggSpecialDataSection.build(this.context))) sectionCount++;
        if (this.addIf(root, BucketCreatureSpecialDataSection.supports(stack), () -> BucketCreatureSpecialDataSection.build(this.context))) sectionCount++;
        if (this.addIf(root, MiscSpecialDataSections.supportsProfile(stack), () -> MiscSpecialDataSections.buildProfile(this.context))) sectionCount++;
        if (this.addIf(root, MiscSpecialDataSections.supportsInstrument(stack), () -> MiscSpecialDataSections.buildInstrument(this.context))) sectionCount++;
        if (this.addIf(root, MiscSpecialDataSections.supportsJukebox(stack), () -> MiscSpecialDataSections.buildJukebox(this.context))) sectionCount++;
        if (this.addIf(root, ContainerSpecialDataSection.supports(stack), () -> ContainerSpecialDataSection.build(this.context))) sectionCount++;
        if (this.addIf(root, BundleSpecialDataSection.supports(stack), () -> BundleSpecialDataSection.build(this.context))) sectionCount++;
        if (this.addIf(root, SignSpecialDataSection.supports(stack), () -> SignSpecialDataSection.build(this.context))) sectionCount++;
        if (this.addIf(root, SpawnerSpecialDataSection.supports(stack), () -> SpawnerSpecialDataSection.build(this.context))) sectionCount++;

        if (sectionCount == 0) {
            root.child(UiFactory.muted(Component.literal(EMPTY_SPECIAL_DATA_TEXT), this.context.panelWidthHint()));
        }

        return root;
    }

    private boolean addIf(FlowLayout root, boolean condition, Supplier<UIComponent> sectionBuilder) {
        if (condition) {
            root.child(sectionBuilder.get());
            return true;
        }
        return false;
    }
}
