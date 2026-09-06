package me.noramibu.itemeditor.ui.panel.specialdata;

import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Sizing;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import me.noramibu.itemeditor.editor.EditorCategory;
import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.ui.component.DyeColorSelectorSection;
import me.noramibu.itemeditor.ui.component.EditorSearchDialog;
import me.noramibu.itemeditor.ui.component.PickerFieldFactory;
import me.noramibu.itemeditor.ui.component.UiFactory;
import me.noramibu.itemeditor.util.ItemEditorCapabilities;
import me.noramibu.itemeditor.util.ItemEditorText;
import me.noramibu.itemeditor.util.ItemEditorTypes;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.animal.fish.Salmon;
import net.minecraft.world.entity.animal.fish.TropicalFish;
import net.minecraft.world.item.ItemStack;

public final class BucketCreatureSpecialDataSection {
    public static List<EditorSearchDialog.Target> searchTargets(SpecialDataPanelContext context) {
        var result = new ArrayList<EditorSearchDialog.Target>();
        var stack = context.originalStack();
        var type = detectBucketType(stack);
        var special = context.special();
        var fields = new ArrayList<SpecialDataSearch.Field>();
        if (supportsAxolotlVariant(type)) fields.add(Field.AXOLOTL_VARIANT);
        if (supportsSalmonVariant(type)) fields.add(Field.SALMON_SIZE);
        if (supportsTropicalFishPattern(type)) fields.add(Field.TROPICAL_PATTERN);
        if (supportsTropicalFishColors(type)) {
            fields.add(Field.TROPICAL_BASE_COLOR);
            fields.add(Field.TROPICAL_PATTERN_COLOR);
        }
        if (supportsPufferState(type, special)) fields.add(Field.PUFFER_STATE);
        if (supportsBucketEntityData(stack)) {
            fields.addAll(List.of(Flag.values()));
            if (supportsAge(type, special)) {
                fields.add(Field.AGE);
                fields.add(Field.AGE_LOCKED);
            }
            if (supportsHuntingCooldown(type, special)) fields.add(Field.HUNTING_COOLDOWN);
            var path = List.of(ItemEditorText.str("special.bucket.title"));
            result.addAll(EntitySpawnDataUi.healthSearchTargets(
                    context, type.entityId(), special.bucketAttributes, path, "bucket", () -> {}));
            result.addAll(EntitySpawnDataUi.attributeSearchTargets(
                    context,
                    type.entityId(),
                    special.bucketAttributes,
                    path,
                    "bucket",
                    () -> special.uiBucketAttributesCollapsed = false,
                    Set.of()));
        }
        result.addAll(SpecialDataSearch.targets(
                context,
                EditorCategory.SPECIAL_DATA,
                "special.bucket.title",
                "bucket",
                () -> {},
                fields.toArray(SpecialDataSearch.Field[]::new)));
        return result;
    }

    private static final List<PufferStateOption> PUFFER_STATES = List.of(
            new PufferStateOption("0", "special.bucket.puffer.small"),
            new PufferStateOption("1", "special.bucket.puffer.medium"),
            new PufferStateOption("2", "special.bucket.puffer.full"));
    private static final int COMPACT_LAYOUT_WIDTH_THRESHOLD = 560;
    private static final int PICKER_BUTTON_WIDTH = 210;
    private static final int HEALTH_FIELD_WIDTH = 120;

    private BucketCreatureSpecialDataSection() {}

    public static boolean supports(ItemStack stack) {
        return ItemEditorCapabilities.supportsBucketCreature(stack);
    }

    public static FlowLayout build(SpecialDataPanelContext context) {
        ItemStack stack = context.originalStack();
        BucketType bucketType = detectBucketType(stack);
        ItemEditorState.SpecialData special = context.special();
        boolean compactLayout = context.isCompactPanel(COMPACT_LAYOUT_WIDTH_THRESHOLD);

        FlowLayout section = UiFactory.section(ItemEditorText.tr("special.bucket.title"), Component.empty());
        section.id("bucket");

        if (supportsAxolotlVariant(bucketType)) {
            section.child(PickerFieldFactory.dropdownField(
                    context,
                    Field.AXOLOTL_VARIANT.text(),
                    Component.empty(),
                    PickerFieldFactory.selectedOrFallback(
                            special.bucketAxolotlVariant, ItemEditorText.tr("special.bucket.select.axolotl_variant")),
                    compactLayout ? -1 : PICKER_BUTTON_WIDTH,
                    Arrays.asList(Axolotl.Variant.values()),
                    Axolotl.Variant::getSerializedName,
                    variant ->
                            context.mutateRefresh(() -> special.bucketAxolotlVariant = variant.getSerializedName())));
        }

        if (supportsSalmonVariant(bucketType)) {
            section.child(PickerFieldFactory.dropdownField(
                    context,
                    Field.SALMON_SIZE.text(),
                    Component.empty(),
                    PickerFieldFactory.selectedOrFallback(
                            special.bucketSalmonSize, ItemEditorText.tr("special.bucket.select.salmon_size")),
                    compactLayout ? -1 : PICKER_BUTTON_WIDTH,
                    Arrays.asList(Salmon.Variant.values()),
                    Salmon.Variant::getSerializedName,
                    variant -> context.mutateRefresh(() -> special.bucketSalmonSize = variant.getSerializedName())));
        }

        if (supportsTropicalFishPattern(bucketType)) {
            section.child(PickerFieldFactory.dropdownField(
                    context,
                    Field.TROPICAL_PATTERN.text(),
                    Component.empty(),
                    PickerFieldFactory.selectedOrFallback(
                            special.bucketTropicalPattern, ItemEditorText.tr("special.bucket.select.tropical_pattern")),
                    compactLayout ? -1 : PICKER_BUTTON_WIDTH,
                    Arrays.asList(TropicalFish.Pattern.values()),
                    TropicalFish.Pattern::getSerializedName,
                    pattern ->
                            context.mutateRefresh(() -> special.bucketTropicalPattern = pattern.getSerializedName())));
        }

        if (supportsTropicalFishColors(bucketType)) {
            section.child(DyeColorSelectorSection.build(
                    context,
                    Field.TROPICAL_BASE_COLOR.text(),
                    Component.empty(),
                    ItemEditorText.tr("special.bucket.select.tropical_base_color"),
                    special.bucketTropicalBaseColor,
                    compactLayout ? -1 : PICKER_BUTTON_WIDTH,
                    null,
                    color -> special.bucketTropicalBaseColor = color.name()));

            section.child(DyeColorSelectorSection.build(
                    context,
                    Field.TROPICAL_PATTERN_COLOR.text(),
                    Component.empty(),
                    ItemEditorText.tr("special.bucket.select.tropical_pattern_color"),
                    special.bucketTropicalPatternColor,
                    compactLayout ? -1 : PICKER_BUTTON_WIDTH,
                    null,
                    color -> special.bucketTropicalPatternColor = color.name()));
        }

        if (supportsPufferState(bucketType, special)) {
            section.child(PickerFieldFactory.dropdownField(
                    context,
                    Field.PUFFER_STATE.text(),
                    Component.empty(),
                    pufferStateLabel(special.bucketPuffState),
                    compactLayout ? -1 : PICKER_BUTTON_WIDTH,
                    PUFFER_STATES,
                    option -> ItemEditorText.str(option.labelKey()),
                    option -> context.mutateRefresh(() -> special.bucketPuffState = option.value())));
        }

        if (supportsBucketEntityData(stack)) {
            FlowLayout bucketEntityCard = UiFactory.subCard();
            bucketEntityCard.child(UiFactory.title(ItemEditorText.tr("special.bucket.entity_data"))
                    .shadow(false));

            bucketEntityCard.child(UiFactory.checkbox(
                    Flag.NO_AI.text(), special.bucketNoAi, context.bindToggle(value -> special.bucketNoAi = value)));
            bucketEntityCard.child(UiFactory.checkbox(
                    Flag.SILENT.text(),
                    special.bucketSilent,
                    context.bindToggle(value -> special.bucketSilent = value)));
            bucketEntityCard.child(UiFactory.checkbox(
                    Flag.NO_GRAVITY.text(),
                    special.bucketNoGravity,
                    context.bindToggle(value -> special.bucketNoGravity = value)));
            bucketEntityCard.child(UiFactory.checkbox(
                    Flag.GLOWING.text(),
                    special.bucketGlowing,
                    context.bindToggle(value -> special.bucketGlowing = value)));
            bucketEntityCard.child(UiFactory.checkbox(
                    Flag.INVULNERABLE.text(),
                    special.bucketInvulnerable,
                    context.bindToggle(value -> special.bucketInvulnerable = value)));
            if (supportsAge(bucketType, special)) {
                bucketEntityCard.child(UiFactory.field(
                        Field.AGE.text(),
                        Component.empty(),
                        UiFactory.textBox(special.bucketAge, context.bindText(value -> special.bucketAge = value))
                                .horizontalSizing(
                                        compactLayout ? Sizing.fill(100) : UiFactory.fixed(HEALTH_FIELD_WIDTH))));
                bucketEntityCard.child(UiFactory.checkbox(
                        Field.AGE_LOCKED.text(),
                        special.bucketAgeLocked,
                        context.bindToggle(value -> special.bucketAgeLocked = value)));
            }
            if (supportsHuntingCooldown(bucketType, special)) {
                bucketEntityCard.child(UiFactory.field(
                        Field.HUNTING_COOLDOWN.text(),
                        Component.empty(),
                        UiFactory.textBox(
                                        special.bucketHuntingCooldown,
                                        context.bindText(value -> special.bucketHuntingCooldown = value))
                                .horizontalSizing(
                                        compactLayout ? Sizing.fill(100) : UiFactory.fixed(HEALTH_FIELD_WIDTH))));
            }
            String entityId = bucketType.entityId();
            bucketEntityCard.child(EntitySpawnDataUi.health(
                    context,
                    special.bucketHealth,
                    value -> special.bucketHealth = value,
                    entityId,
                    special.bucketAttributes,
                    compactLayout));
            bucketEntityCard.child(EntitySpawnDataUi.attributes(
                    context,
                    entityId,
                    special.bucketAttributes,
                    special.uiBucketAttributesCollapsed,
                    () -> context.mutateRefresh(
                            () -> special.uiBucketAttributesCollapsed = !special.uiBucketAttributesCollapsed),
                    Set.of()));
            section.child(bucketEntityCard);
        }

        return section;
    }

    public static boolean supportsBucketEntityData(ItemStack stack) {
        return stack.has(DataComponents.BUCKET_ENTITY_DATA) || ItemEditorCapabilities.isBucketCreatureBucketItem(stack);
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

    private static boolean supportsAge(BucketType bucketType, ItemEditorState.SpecialData special) {
        return bucketType == BucketType.AXOLOTL
                || bucketType == BucketType.TADPOLE
                || !special.bucketAge.isBlank()
                || special.bucketAgeLocked;
    }

    private static boolean supportsHuntingCooldown(BucketType bucketType, ItemEditorState.SpecialData special) {
        return bucketType == BucketType.AXOLOTL || !special.bucketHuntingCooldown.isBlank();
    }

    private static BucketType detectBucketType(ItemStack stack) {
        EntityType<?> entityType = ItemEditorCapabilities.bucketCreatureEntityType(stack);
        for (BucketType bucketType : BucketType.values()) {
            if (bucketType.entityType == entityType) {
                return bucketType;
            }
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

    private record PufferStateOption(String value, String labelKey) {}

    private enum BucketType {
        AXOLOTL(ItemEditorTypes.AXOLOTL),
        SALMON(ItemEditorTypes.SALMON),
        TROPICAL_FISH(ItemEditorTypes.TROPICAL_FISH),
        PUFFERFISH(ItemEditorTypes.PUFFERFISH),
        COD(ItemEditorTypes.COD),
        TADPOLE(ItemEditorTypes.TADPOLE),
        UNKNOWN(null);

        private final EntityType<?> entityType;

        BucketType(EntityType<?> entityType) {
            this.entityType = entityType;
        }

        String entityId() {
            return this.entityType == null
                    ? ""
                    : EntityType.getKey(this.entityType).toString();
        }
    }

    private enum Flag implements SpecialDataSearch.Field {
        NO_AI("special.entity.no_ai"),
        SILENT("special.entity.silent"),
        NO_GRAVITY("special.entity.no_gravity"),
        GLOWING("special.entity.glowing"),
        INVULNERABLE("special.entity.invulnerable");

        private final String key;

        Flag(String key) {
            this.key = key;
        }

        public String key() {
            return key;
        }
    }

    private enum Field implements SpecialDataSearch.Field {
        AXOLOTL_VARIANT("special.entity_variant.axolotl_variant"),
        SALMON_SIZE("special.entity_variant.salmon_size"),
        TROPICAL_PATTERN("special.entity_variant.tropical_pattern"),
        TROPICAL_BASE_COLOR("special.bucket.tropical_base_color"),
        TROPICAL_PATTERN_COLOR("special.bucket.tropical_pattern_color"),
        PUFFER_STATE("special.bucket.puffer_state"),
        AGE("special.bucket.age"),
        AGE_LOCKED("special.bucket.age_locked"),
        HUNTING_COOLDOWN("special.bucket.hunting_cooldown");

        private final String key;

        Field(String key) {
            this.key = key;
        }

        public String key() {
            return key;
        }
    }
}
