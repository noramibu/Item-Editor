package me.noramibu.itemeditor.service;

import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.editor.ValidationMessage;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class SpecialDataPreviewService {

    private static final List<SpecialDataApplier> APPLIERS = List.of(
            new ContainerSpecialDataApplier(),
            new BundleSpecialDataApplier(),
            new SignSpecialDataApplier(),
            new SpawnerSpecialDataApplier(),
            new PotionSpecialDataApplier(),
            new StewSpecialDataApplier(),
            new FireworkSpecialDataApplier(),
            new BannerSpecialDataApplier(),
            new BucketCreatureSpecialDataApplier(),
            new DyedColorSpecialDataApplier(),
            new TrimSpecialDataApplier(),
            new ProfileSpecialDataApplier(),
            new InstrumentSpecialDataApplier(),
            new JukeboxSpecialDataApplier(),
            new MapSpecialDataApplier()
    );

    public void applySpecialData(
            ItemStack originalStack,
            ItemStack preview,
            ItemEditorState state,
            ItemEditorState baselineState,
            RegistryAccess registryAccess,
            List<ValidationMessage> messages
    ) {
        SpecialDataApplyContext context = new SpecialDataApplyContext(
                originalStack,
                preview,
                state,
                baselineState,
                registryAccess,
                messages
        );

        for (SpecialDataApplier applier : APPLIERS) {
            applier.apply(context);
        }
    }
}
