package me.noramibu.itemeditor.ui.panel.specialdata;

import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Sizing;
import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.ui.component.DyeColorSelectorSection;
import me.noramibu.itemeditor.ui.component.PickerFieldFactory;
import me.noramibu.itemeditor.ui.component.UiFactory;
import me.noramibu.itemeditor.util.ItemEditorCapabilities;
import me.noramibu.itemeditor.util.ItemEditorText;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.animal.Salmon;
import net.minecraft.world.entity.animal.TropicalFish;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Arrays;
import java.util.List;

public final class BucketCreatureSpecialDataSection {

    private static final List<PufferStateOption> PUFFER_STATES = List.of(
            new PufferStateOption("0", "special.bucket.puffer.small"),
            new PufferStateOption("1", "special.bucket.puffer.medium"),
            new PufferStateOption("2", "special.bucket.puffer.full")
    );

    private BucketCreatureSpecialDataSection() {
    }

    public static boolean supports(ItemStack stack) {
        return ItemEditorCapabilities.supportsBucketCreature(stack);
    }

    public static FlowLayout build(SpecialDataPanelContext context) {
        ItemStack stack = context.originalStack();
        BucketType bucketType = detectBucketType(stack);
        ItemEditorState.SpecialData special = context.special();

        FlowLayout section = UiFactory.section(ItemEditorText.tr("special.bucket.title"), Component.empty());

        if (supportsAxolotlVariant(bucketType)) {
            section.child(PickerFieldFactory.dropdownField(
                    context,
                    ItemEditorText.tr("special.bucket.axolotl_variant"),
                    Component.empty(),
                    PickerFieldFactory.selectedOrFallback(
                            special.bucketAxolotlVariant,
                            ItemEditorText.tr("special.bucket.select.axolotl_variant")
                    ),
                    210,
                    Arrays.asList(Axolotl.Variant.values()),
                    Axolotl.Variant::getSerializedName,
                    variant -> context.mutateRefresh(() -> special.bucketAxolotlVariant = variant.getSerializedName())
            ));
        }

        if (supportsSalmonVariant(bucketType)) {
            section.child(PickerFieldFactory.dropdownField(
                    context,
                    ItemEditorText.tr("special.bucket.salmon_size"),
                    Component.empty(),
                    PickerFieldFactory.selectedOrFallback(
                            special.bucketSalmonSize,
                            ItemEditorText.tr("special.bucket.select.salmon_size")
                    ),
                    210,
                    Arrays.asList(Salmon.Variant.values()),
                    Salmon.Variant::getSerializedName,
                    variant -> context.mutateRefresh(() -> special.bucketSalmonSize = variant.getSerializedName())
            ));
        }

        if (supportsTropicalFishPattern(bucketType)) {
            section.child(PickerFieldFactory.dropdownField(
                    context,
                    ItemEditorText.tr("special.bucket.tropical_pattern"),
                    Component.empty(),
                    PickerFieldFactory.selectedOrFallback(
                            special.bucketTropicalPattern,
                            ItemEditorText.tr("special.bucket.select.tropical_pattern")
                    ),
                    210,
                    Arrays.asList(TropicalFish.Pattern.values()),
                    TropicalFish.Pattern::getSerializedName,
                    pattern -> context.mutateRefresh(() -> special.bucketTropicalPattern = pattern.getSerializedName())
            ));
        }

        if (supportsTropicalFishColors(bucketType)) {
            section.child(DyeColorSelectorSection.build(
                    context,
                    ItemEditorText.tr("special.bucket.tropical_base_color"),
                    Component.empty(),
                    ItemEditorText.tr("special.bucket.select.tropical_base_color"),
                    special.bucketTropicalBaseColor,
                    210,
                    null,
                    color -> special.bucketTropicalBaseColor = color.name()
            ));

            section.child(DyeColorSelectorSection.build(
                    context,
                    ItemEditorText.tr("special.bucket.tropical_pattern_color"),
                    Component.empty(),
                    ItemEditorText.tr("special.bucket.select.tropical_pattern_color"),
                    special.bucketTropicalPatternColor,
                    210,
                    null,
                    color -> special.bucketTropicalPatternColor = color.name()
            ));
        }

        if (supportsPufferState(bucketType, special)) {
            section.child(PickerFieldFactory.dropdownField(
                    context,
                    ItemEditorText.tr("special.bucket.puffer_state"),
                    Component.empty(),
                    pufferStateLabel(special.bucketPuffState),
                    210,
                    PUFFER_STATES,
                    option -> ItemEditorText.str(option.labelKey()),
                    option -> context.mutateRefresh(() -> special.bucketPuffState = option.value())
            ));
        }

        if (supportsBucketEntityData(stack)) {
            FlowLayout bucketEntityCard = UiFactory.subCard();
            bucketEntityCard.child(UiFactory.title(ItemEditorText.tr("special.bucket.entity_data")).shadow(false));

            bucketEntityCard.child(UiFactory.checkbox(ItemEditorText.tr("special.bucket.no_ai"), special.bucketNoAi, context.bindToggle(value -> special.bucketNoAi = value)));
            bucketEntityCard.child(UiFactory.checkbox(ItemEditorText.tr("special.bucket.silent"), special.bucketSilent, context.bindToggle(value -> special.bucketSilent = value)));
            bucketEntityCard.child(UiFactory.checkbox(ItemEditorText.tr("special.bucket.no_gravity"), special.bucketNoGravity, context.bindToggle(value -> special.bucketNoGravity = value)));
            bucketEntityCard.child(UiFactory.checkbox(ItemEditorText.tr("special.bucket.glowing"), special.bucketGlowing, context.bindToggle(value -> special.bucketGlowing = value)));
            bucketEntityCard.child(UiFactory.checkbox(ItemEditorText.tr("special.bucket.invulnerable"), special.bucketInvulnerable, context.bindToggle(value -> special.bucketInvulnerable = value)));
            bucketEntityCard.child(UiFactory.field(
                    ItemEditorText.tr("special.bucket.health"),
                    Component.empty(),
                    UiFactory.textBox(special.bucketHealth, context.bindText(value -> special.bucketHealth = value)).horizontalSizing(Sizing.fixed(120))
            ));
            section.child(bucketEntityCard);
        }

        return section;
    }

    private static boolean supportsBucketEntityData(ItemStack stack) {
        return stack.has(DataComponents.BUCKET_ENTITY_DATA)
                || ItemEditorCapabilities.isBucketCreatureBucketItem(stack);
    }

    private static boolean supportsAxolotlVariant(BucketType bucketType) {
        return bucketType == BucketType.AXOLOTL;
    }

    private static boolean supportsSalmonVariant(BucketType bucketType) {
        return bucketType == BucketType.SALMON;
    }

    private static boolean supportsTropicalFishPattern(BucketType bucketType) {
        return bucketType == BucketType.TROPICAL_FISH;
    }

    private static boolean supportsTropicalFishColors(BucketType bucketType) {
        return bucketType == BucketType.TROPICAL_FISH;
    }

    private static boolean supportsPufferState(BucketType bucketType, ItemEditorState.SpecialData special) {
        return bucketType == BucketType.PUFFERFISH || !special.bucketPuffState.isBlank();
    }

    private static BucketType detectBucketType(ItemStack stack) {
        if (stack.is(Items.AXOLOTL_BUCKET)) {
            return BucketType.AXOLOTL;
        }
        if (stack.is(Items.SALMON_BUCKET)) {
            return BucketType.SALMON;
        }
        if (stack.is(Items.TROPICAL_FISH_BUCKET)) {
            return BucketType.TROPICAL_FISH;
        }
        if (stack.is(Items.PUFFERFISH_BUCKET)) {
            return BucketType.PUFFERFISH;
        }
        if (stack.is(Items.COD_BUCKET)) {
            return BucketType.COD;
        }
        if (stack.is(Items.TADPOLE_BUCKET)) {
            return BucketType.TADPOLE;
        }
        return BucketType.UNKNOWN;
    }

    private static Component pufferStateLabel(String value) {
        String normalized = value == null ? "" : value.trim();
        for (PufferStateOption option : PUFFER_STATES) {
            if (option.value().equals(normalized)) {
                return ItemEditorText.tr(option.labelKey());
            }
        }
        return ItemEditorText.tr("special.bucket.select.puffer_state");
    }

    private record PufferStateOption(String value, String labelKey) {
    }

    private enum BucketType {
        AXOLOTL,
        SALMON,
        TROPICAL_FISH,
        PUFFERFISH,
        COD,
        TADPOLE,
        UNKNOWN
    }
}
