package me.noramibu.itemeditor.service;

import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.editor.ValidationMessage;
import me.noramibu.itemeditor.util.ItemEditorText;
import me.noramibu.itemeditor.util.RegistryUtil;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.BannerPatternLayers;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

final class BannerSpecialDataApplier extends AbstractPreviewApplierSupport implements SpecialDataApplier {

    @Override
    public void apply(SpecialDataApplyContext context) {
        if (this.sameBannerData(context.state(), context.baselineState())) {
            this.restoreOriginalComponent(context.originalStack(), context.previewStack(), DataComponents.BANNER_PATTERNS);
            this.restoreOriginalComponent(context.originalStack(), context.previewStack(), DataComponents.BASE_COLOR);
            return;
        }

        Registry<BannerPattern> bannerRegistry = context.registryAccess().lookupOrThrow(Registries.BANNER_PATTERN);
        List<BannerPatternLayers.Layer> layers = new ArrayList<>();

        for (ItemEditorState.BannerLayerDraft draft : context.special().bannerLayers) {
            if (draft.patternId.isBlank()) continue;

            Holder<BannerPattern> pattern = RegistryUtil.resolveHolder(bannerRegistry, draft.patternId);
            if (pattern == null) {
                context.messages().add(ValidationMessage.error(ItemEditorText.str(
                        "validation.registry_missing",
                        ItemEditorText.str("special.banner.pattern"),
                        draft.patternId
                )));
                continue;
            }

            try {
                layers.add(new BannerPatternLayers.Layer(pattern, DyeColor.valueOf(draft.color)));
            } catch (IllegalArgumentException exception) {
                context.messages().add(ValidationMessage.error(ItemEditorText.str(
                        "validation.registry_missing",
                        ItemEditorText.str("special.banner.color"),
                        draft.color
                )));
            }
        }

        if (layers.isEmpty()) {
            this.clearToPrototype(context.previewStack(), DataComponents.BANNER_PATTERNS);
        } else {
            context.previewStack().set(DataComponents.BANNER_PATTERNS, new BannerPatternLayers(layers));
        }

        String baseColorId = context.special().bannerBaseColor;
        if (context.previewStack().is(Items.SHIELD) && baseColorId.isBlank() && !layers.isEmpty()) {
            baseColorId = DyeColor.WHITE.name();
        }

        if (baseColorId.isBlank()) {
            this.clearToPrototype(context.previewStack(), DataComponents.BASE_COLOR);
        } else {
            try {
                context.previewStack().set(DataComponents.BASE_COLOR, DyeColor.valueOf(baseColorId));
            } catch (IllegalArgumentException exception) {
                context.messages().add(ValidationMessage.error(ItemEditorText.str(
                        "validation.registry_missing",
                        ItemEditorText.str("special.banner.base_color"),
                        baseColorId
                )));
            }
        }
    }

    private boolean sameBannerData(ItemEditorState state, ItemEditorState baselineState) {
        return Objects.equals(state.special.bannerBaseColor, baselineState.special.bannerBaseColor)
                && this.sameBannerLayers(state.special.bannerLayers, baselineState.special.bannerLayers);
    }

    private boolean sameBannerLayers(List<ItemEditorState.BannerLayerDraft> current, List<ItemEditorState.BannerLayerDraft> baseline) {
        return this.sameList(current, baseline, (left, right) -> Objects.equals(left.patternId, right.patternId)
                && Objects.equals(left.color, right.color));
    }
}
