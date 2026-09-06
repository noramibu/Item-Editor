package me.noramibu.itemeditor.ui.panel;

import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.UIComponent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import me.noramibu.itemeditor.ui.component.EditorSearchDialog;
import me.noramibu.itemeditor.ui.component.UiFactory;
import me.noramibu.itemeditor.ui.panel.specialdata.ArmorStandSpecialDataSection;
import me.noramibu.itemeditor.ui.panel.specialdata.BannerSpecialDataSection;
import me.noramibu.itemeditor.ui.panel.specialdata.BucketCreatureSpecialDataSection;
import me.noramibu.itemeditor.ui.panel.specialdata.BundleSpecialDataSection;
import me.noramibu.itemeditor.ui.panel.specialdata.CommandBlockSpecialDataSection;
import me.noramibu.itemeditor.ui.panel.specialdata.ContainerSpecialDataSection;
import me.noramibu.itemeditor.ui.panel.specialdata.DebugStickSpecialDataSection;
import me.noramibu.itemeditor.ui.panel.specialdata.EntityVariantSpecialDataSection;
import me.noramibu.itemeditor.ui.panel.specialdata.FireworkSpecialDataSection;
import me.noramibu.itemeditor.ui.panel.specialdata.ItemFrameSpecialDataSection;
import me.noramibu.itemeditor.ui.panel.specialdata.MiscSpecialDataSections;
import me.noramibu.itemeditor.ui.panel.specialdata.PotionSpecialDataSection;
import me.noramibu.itemeditor.ui.panel.specialdata.SignSpecialDataSection;
import me.noramibu.itemeditor.ui.panel.specialdata.SpawnEggSpecialDataSection;
import me.noramibu.itemeditor.ui.panel.specialdata.SpawnerSpecialDataSection;
import me.noramibu.itemeditor.ui.panel.specialdata.SpecialDataPanelContext;
import me.noramibu.itemeditor.ui.panel.specialdata.StewSpecialDataSection;
import me.noramibu.itemeditor.ui.screen.ItemEditorScreen;
import me.noramibu.itemeditor.util.ItemEditorText;
import net.minecraft.world.item.ItemStack;

public final class SpecialDataEditorPanel implements EditorPanel {
    private record Section(
            Predicate<ItemStack> supports,
            Function<SpecialDataPanelContext, UIComponent> build,
            Function<SpecialDataPanelContext, List<EditorSearchDialog.Target>> search) {}

    private static final List<Section> SECTIONS = List.of(
            new Section(
                    PotionSpecialDataSection::supports,
                    PotionSpecialDataSection::build,
                    PotionSpecialDataSection::searchTargets),
            new Section(
                    StewSpecialDataSection::supports,
                    StewSpecialDataSection::build,
                    StewSpecialDataSection::searchTargets),
            new Section(
                    FireworkSpecialDataSection::supportsRocket,
                    FireworkSpecialDataSection::buildRocket,
                    FireworkSpecialDataSection::searchRocketTargets),
            new Section(
                    FireworkSpecialDataSection::supportsStar,
                    FireworkSpecialDataSection::buildStar,
                    FireworkSpecialDataSection::searchStarTargets),
            new Section(
                    BannerSpecialDataSection::supports,
                    BannerSpecialDataSection::build,
                    BannerSpecialDataSection::searchTargets),
            new Section(
                    ArmorStandSpecialDataSection::supports,
                    ArmorStandSpecialDataSection::build,
                    ArmorStandSpecialDataSection::searchTargets),
            new Section(
                    ItemFrameSpecialDataSection::supports,
                    ItemFrameSpecialDataSection::build,
                    ItemFrameSpecialDataSection::searchTargets),
            new Section(
                    SpawnEggSpecialDataSection::supports,
                    SpawnEggSpecialDataSection::build,
                    SpawnEggSpecialDataSection::searchTargets),
            new Section(
                    EntityVariantSpecialDataSection::supports,
                    EntityVariantSpecialDataSection::build,
                    EntityVariantSpecialDataSection::searchTargets),
            new Section(
                    BucketCreatureSpecialDataSection::supports,
                    BucketCreatureSpecialDataSection::build,
                    BucketCreatureSpecialDataSection::searchTargets),
            new Section(
                    MiscSpecialDataSections::supportsProfile,
                    MiscSpecialDataSections::buildProfile,
                    MiscSpecialDataSections::searchProfileTargets),
            new Section(
                    MiscSpecialDataSections::supportsInstrument,
                    MiscSpecialDataSections::buildInstrument,
                    MiscSpecialDataSections::searchInstrumentTargets),
            new Section(
                    DebugStickSpecialDataSection::supports,
                    DebugStickSpecialDataSection::build,
                    DebugStickSpecialDataSection::searchTargets),
            new Section(
                    ContainerSpecialDataSection::supports,
                    ContainerSpecialDataSection::build,
                    ContainerSpecialDataSection::searchTargets),
            new Section(
                    BundleSpecialDataSection::supports,
                    BundleSpecialDataSection::build,
                    BundleSpecialDataSection::searchTargets),
            new Section(
                    SignSpecialDataSection::supports,
                    SignSpecialDataSection::build,
                    SignSpecialDataSection::searchTargets),
            new Section(
                    CommandBlockSpecialDataSection::supports,
                    CommandBlockSpecialDataSection::build,
                    CommandBlockSpecialDataSection::searchTargets),
            new Section(
                    SpawnerSpecialDataSection::supports,
                    SpawnerSpecialDataSection::build,
                    SpawnerSpecialDataSection::searchTargets));

    private final SpecialDataPanelContext context;

    public SpecialDataEditorPanel(ItemEditorScreen screen) {
        this.context = new SpecialDataPanelContext(screen);
    }

    @Override
    public List<EditorSearchDialog.Target> searchTargets() {
        var result = new ArrayList<EditorSearchDialog.Target>();
        for (Section section : SECTIONS) {
            if (section.supports().test(context.originalStack())) {
                result.addAll(section.search().apply(context));
            }
        }
        return result;
    }

    @Override
    public UIComponent build() {
        FlowLayout root = UiFactory.column();
        for (Section section : SECTIONS) {
            if (section.supports().test(context.originalStack())) {
                root.child(section.build().apply(context));
            }
        }
        if (root.children().isEmpty()) {
            root.child(UiFactory.muted(ItemEditorText.tr("special.empty"), context.panelWidthHint()));
        }
        return root;
    }
}
