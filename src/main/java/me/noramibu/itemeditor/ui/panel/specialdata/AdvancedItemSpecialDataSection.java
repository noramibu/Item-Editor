package me.noramibu.itemeditor.ui.panel.specialdata;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.HorizontalAlignment;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.UIComponent;
import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.ui.component.PickerFieldFactory;
import me.noramibu.itemeditor.ui.component.RawTextAreaComponent;
import me.noramibu.itemeditor.ui.component.UiFactory;
import me.noramibu.itemeditor.ui.screen.ItemEditorScreen;
import me.noramibu.itemeditor.ui.util.TriStateBooleanUi;
import me.noramibu.itemeditor.util.IdFieldNormalizer;
import me.noramibu.itemeditor.util.ItemEditorCapabilities;
import me.noramibu.itemeditor.util.ItemEditorText;
import me.noramibu.itemeditor.util.LootTableIds;
import me.noramibu.itemeditor.util.ValidationUtil;
import net.minecraft.advancements.predicates.DataComponentMatchers;
import net.minecraft.advancements.predicates.ItemPredicate;
import net.minecraft.advancements.predicates.MinMaxBounds;
import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponentExactPredicate;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class AdvancedItemSpecialDataSection {
    private static final int NARROW_LAYOUT_WIDTH_THRESHOLD = 900;
    private static final int STACKED_COMPACT_WIDTH_THRESHOLD = 1080;
    private static final int EQUIPMENT_DENSE_STACK_WIDTH_THRESHOLD = 620;
    private static final int COMPACT_ICON_BUTTON_MIN = 36;
    static final int COMPACT_ICON_BUTTON_BASE = 42;
    private static final int COMPACT_CLEAR_BUTTON_MIN = 52;
    private static final int COMPACT_CLEAR_BUTTON_BASE = 68;
    private static final int COMPACT_REMOVE_BUTTON_MIN = 72;
    private static final int COMPACT_REMOVE_BUTTON_BASE = 88;
    private static final int COMPACT_FIXED_PICK_BUTTON_MIN = 46;
    private static final int COMPACT_FIXED_PICK_BUTTON_BASE = 56;
    private static final int PICKER_ROW_INLINE_MIN_WIDTH = 360;
    private static final int PICKER_ROW_INPUT_MIN_WIDTH = 180;
    private static final int STORAGE_PICK_INPUT_MIN_WIDTH = 96;
    private static final int STORAGE_PICK_BUTTON_MIN = 92;
    private static final int STORAGE_PICK_BUTTON_BASE = 122;
    private static final int BLOCK_STATE_STACKED_ROW_WIDTH_THRESHOLD = 420;
    private static final int BLOCK_STATE_VALUE_WIDTH_PERCENT = 76;
    private static final int BLOCK_STATE_VALUE_WITH_RESET_WIDTH_PERCENT = 56;
    static final int SECTION_ROW_GAP = 10;
    private static final int COLLAPSIBLE_HEADER_TITLE_RESERVE = 26;
    private static final int COMPACT_FIELD_LABEL_RESERVE = 44;
    private static final int PANEL_WIDTH_SAFETY_RESERVE = 20;
    static final String SYMBOL_SECTION_COLLAPSED = "+";
    static final String SYMBOL_SECTION_EXPANDED = "-";
    private static final int CONSUMABLE_EFFECTS_EMPTY_HINT_WIDTH = 300;
    private static final int CONSUMABLE_EFFECT_SUMMARY_HINT_WIDTH = 300;
    private static final int CONSUMABLE_APPLY_EFFECTS_EMPTY_HINT_WIDTH = 280;
    private static final int MAP_DECORATIONS_EMPTY_HINT_WIDTH = 280;
    static final int COMBAT_REPAIRABLE_EMPTY_HINT_WIDTH = 280;
    private static final int CONTAINER_META_LOCK_HINT_WIDTH = 360;
    private static final int CONTAINER_META_BEES_EMPTY_HINT_WIDTH = 320;
    private static final int CONTAINER_META_BEE_SUMMARY_HINT_WIDTH = 340;
    private static final int CROSSBOW_EMPTY_HINT_WIDTH = 320;
    private static final int CROSSBOW_PROJECTILE_SUMMARY_HINT_WIDTH = 340;
    private static final int MAP_DECORATION_SUMMARY_HINT_WIDTH = 360;
    private static final int CUSTOM_DATA_EDITOR_HEIGHT = 220;
    private static final int CUSTOM_DATA_CONTENT_PADDING = 4;
    private static final int CUSTOM_DATA_TEXT_WIDTH_RESERVE = 14;
    private static final int CUSTOM_DATA_HINT_MIN_WIDTH = 96;

    private AdvancedItemSpecialDataSection() {
    }

    public static boolean supportsContainerMetadata(ItemStack stack) {
        return stack.has(DataComponents.LOCK)
                || stack.has(DataComponents.CONTAINER_LOOT)
                || stack.has(DataComponents.BEES)
                || stack.has(DataComponents.POT_DECORATIONS)
                || ItemEditorCapabilities.supportsContainerData(stack)
                || stack.is(Items.BEEHIVE)
                || stack.is(Items.BEE_NEST)
                || stack.is(Items.DECORATED_POT);
    }

    public static boolean supportsCrossbow(ItemStack stack) {
        return stack.has(DataComponents.CHARGED_PROJECTILES)
                || stack.is(Items.CROSSBOW);
    }

    public static boolean supportsMapAdvanced(ItemStack stack) {
        return stack.has(DataComponents.MAP_ID)
                || stack.has(DataComponents.MAP_DECORATIONS)
                || stack.has(DataComponents.LODESTONE_TRACKER)
                || stack.is(Items.FILLED_MAP);
    }

    public static boolean supportsBlockState(ItemStack stack) {
        return stack.getItem() instanceof BlockItem blockItem
                && !blockItem.getBlock().defaultBlockState().getProperties().isEmpty();
    }

    public static FlowLayout buildComponentTweakNamingSection(SpecialDataPanelContext context) {
        return buildComponentTweakNamingSection(context, context.special());
    }

    public static FlowLayout buildBlockState(SpecialDataPanelContext context) {
        return buildBlockState(context, context.special());
    }

    public static FlowLayout buildCustomData(SpecialDataPanelContext context) {
        ItemEditorState.SpecialData special = context.special();
        return collapsibleCard(
                context,
                ItemEditorText.tr("special.advanced.custom_data.title"),
                special.uiCustomDataCollapsed,
                value -> special.uiCustomDataCollapsed = value,
                () -> {
                    int contentWidth = customDataContentWidth(context);
                    FlowLayout content = UiFactory.column();
                    content.padding(Insets.of(
                            0,
                            0,
                            UiFactory.scaledPixels(CUSTOM_DATA_CONTENT_PADDING),
                            UiFactory.scaledPixels(CUSTOM_DATA_CONTENT_PADDING)
                    ));
                    content.child(wrappedMutedText(ItemEditorText.tr("special.advanced.custom_data.hint"), contentWidth));
                    content.child(compactField(
                            ItemEditorText.tr("special.advanced.custom_data.editor"),
                            customDataEditor(context, special),
                            contentWidth
                    ));
                    return content;
                }
        );
    }

    private static RawTextAreaComponent customDataEditor(SpecialDataPanelContext context, ItemEditorState.SpecialData special) {
        RawTextAreaComponent editor = new RawTextAreaComponent(
                Sizing.fill(100),
                UiFactory.fixed(CUSTOM_DATA_EDITOR_HEIGHT),
                special.customDataSnbt
        );
        editor.wordWrap(true);
        editor.onChanged().subscribe((value, delta) -> context.mutate(() -> special.customDataSnbt = value));
        return editor;
    }

    private static int customDataContentWidth(SpecialDataPanelContext context) {
        int padding = UiFactory.scaledPixels(CUSTOM_DATA_CONTENT_PADDING * 2);
        int reserve = UiFactory.scaledPixels(CUSTOM_DATA_TEXT_WIDTH_RESERVE);
        return Math.clamp(
                context.panelWidthHint() - padding - reserve,
                CUSTOM_DATA_HINT_MIN_WIDTH,
                Math.max(CUSTOM_DATA_HINT_MIN_WIDTH, guiWidth())
        );
    }

    private static FlowLayout wrappedMutedText(Component text, int maxWidth) {
        FlowLayout lines = UiFactory.column();
        lines.gap(Math.max(1, UiFactory.scaleProfile().tightSpacing() - 2));
        for (String line : wrapText(text.getString(), maxWidth)) {
            lines.child(UiFactory.muted(Component.literal(line), maxWidth));
        }
        return lines;
    }

    private static List<String> wrapText(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        String remaining = text == null ? "" : text.trim();
        var font = Minecraft.getInstance().font;
        while (!remaining.isEmpty()) {
            String line = font.plainSubstrByWidth(remaining, maxWidth);
            if (line.isEmpty()) {
                int next = Character.charCount(remaining.codePointAt(0));
                line = remaining.substring(0, next);
            } else if (line.length() < remaining.length()) {
                int breakAt = line.lastIndexOf(' ');
                if (breakAt > 0) {
                    line = line.substring(0, breakAt);
                }
            }
            lines.add(line);
            remaining = remaining.substring(line.length()).trim();
        }
        if (lines.isEmpty()) {
            lines.add("");
        }
        return lines;
    }

    public static FlowLayout buildFoodConsumable(SpecialDataPanelContext context) {
        ItemEditorState.SpecialData special = context.special();
        return collapsibleCard(
                context,
                ItemEditorText.tr("special.advanced.food.title"),
                special.uiFoodConsumableCollapsed,
                value -> special.uiFoodConsumableCollapsed = value,
                () -> {
                    boolean narrowLayout = isNarrowLayout();
                    int compactNumberWidth = compactNumericFieldWidth();
                    int compactTinyWidth = compactTinyFieldWidth();
                    int compactIdWidth = narrowLayout ? clampWidth(guiWidth(), 0.14, 130, 220) : compactIdTextWidth();
                    int compactGroupWidth = compactGroupFieldWidth();
                    int animationButtonWidth = narrowLayout ? clampWidth(guiWidth(), 0.11, 110, 180) : compactPickerButtonWidth();
                    FlowLayout content = UiFactory.column();
                    content.child(denseEquipmentRow(
                            compactTextField(
                                    context,
                                    ItemEditorText.tr("special.advanced.food.nutrition"),
                                    special.foodNutrition,
                                    value -> special.foodNutrition = value,
                                    compactNumberWidth
                            ),
                            compactTextField(
                                    context,
                                    ItemEditorText.tr("special.advanced.food.saturation"),
                                    special.foodSaturation,
                                    value -> special.foodSaturation = value,
                                    compactNumberWidth
                            ),
                            compactTextField(
                                    context,
                                    ItemEditorText.tr("special.advanced.consumable.consume_seconds"),
                                    special.consumableConsumeSeconds,
                                    value -> special.consumableConsumeSeconds = value,
                                    compactNumberWidth
                            )
                    ));

                    content.child(compactCheckboxRow(
                            UiFactory.checkbox(
                                    ItemEditorText.tr("special.advanced.food.can_always_eat"),
                                    special.foodCanAlwaysEat,
                                    context.bindToggle(value -> special.foodCanAlwaysEat = value)
                            ),
                            compactTriStateBooleanPicker(
                                    context,
                                    ItemEditorText.tr("special.advanced.consumable.has_particles"),
                                    special.consumableHasParticles,
                                    value -> special.consumableHasParticles = value,
                                    compactPickerButtonWidth()
                            )
                    ));

                    content.child(compactAnimationPicker(context, special, animationButtonWidth));

                    content.child(compactIdField(
                            context,
                            ItemEditorText.tr("special.advanced.consumable.sound"),
                            special.consumableSoundId,
                            value -> special.consumableSoundId = value,
                            context.optionalRegistryIds(Registries.SOUND_EVENT),
                            ItemEditorText.str("special.advanced.consumable.sound"),
                            compactIdWidth
                    ));
                    content.child(buildOnConsumeEffectsEditor(context, special));

                    content.child(compactCheckboxRow(
                            UiFactory.checkbox(
                                    ItemEditorText.tr("special.advanced.use_effects.can_sprint"),
                                    special.useEffectsCanSprint,
                                    context.bindToggle(value -> special.useEffectsCanSprint = value)
                            ),
                            UiFactory.checkbox(
                                    ItemEditorText.tr("special.advanced.use_effects.interact_vibrations"),
                                    special.useEffectsInteractVibrations,
                                    context.bindToggle(value -> special.useEffectsInteractVibrations = value)
                            )
                    ));
                    content.child(denseEquipmentRow(
                            compactTextField(
                                    context,
                                    ItemEditorText.tr("special.advanced.use_effects.speed_multiplier"),
                                    special.useEffectsSpeedMultiplier,
                                    value -> special.useEffectsSpeedMultiplier = value,
                                    compactNumberWidth
                            )
                    ));
                    content.child(compactField(
                            ItemEditorText.tr("special.advanced.use_remainder.item_id"),
                            itemIdInputWithStoragePick(
                                    context,
                                    special.useRemainderItemId,
                                    value -> {
                                        special.useRemainderItemId = value;
                                        special.useRemainderTemplateSnbt = "";
                                    },
                                    stack -> {
                                        special.useRemainderItemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                                        special.useRemainderCount = Integer.toString(Math.max(1, stack.getCount()));
                                        special.useRemainderTemplateSnbt = encodeItemStackTemplate(context, stack);
                                    },
                                    context.itemIdsWithoutAir(),
                                    ItemEditorText.str("special.advanced.use_remainder.item_id")
                            ),
                            compactLongFieldWidth() + 170
                    ));
                    content.child(denseEquipmentRow(
                            compactTextField(
                                    context,
                                    ItemEditorText.tr("special.advanced.use_remainder.count"),
                                    special.useRemainderCount,
                                    value -> special.useRemainderCount = value,
                                    compactTinyWidth
                            )
                    ));
                    content.child(denseEquipmentRow(
                            compactTextField(
                                    context,
                                    ItemEditorText.tr("special.advanced.use_cooldown.seconds"),
                                    special.useCooldownSeconds,
                                    value -> special.useCooldownSeconds = value,
                                    compactNumberWidth
                            ),
                            compactTextField(
                                    context,
                                    ItemEditorText.tr("special.advanced.use_cooldown.group"),
                                    special.useCooldownGroup,
                                    value -> special.useCooldownGroup = value,
                                    compactGroupWidth
                            )
                    ));

                    FlowLayout actions = responsiveRow();
                    ButtonComponent resetAll = UiFactory.button(ItemEditorText.tr("common.reset"), UiFactory.ButtonTextPreset.STANDARD,  button ->
                            context.mutateRefresh(() -> {
                                special.foodNutrition = "";
                                special.foodSaturation = "";
                                special.foodCanAlwaysEat = false;
                                special.consumableConsumeSeconds = "";
                                special.consumableAnimation = "";
                                special.consumableSoundId = "";
                                special.consumableHasParticles = "";
                                special.consumableOnConsumeEffects.clear();
                                special.useEffectsCanSprint = false;
                                special.useEffectsInteractVibrations = false;
                                special.useEffectsSpeedMultiplier = "";
                                special.useRemainderItemId = "";
                                special.useRemainderCount = "";
                                special.useRemainderTemplateSnbt = "";
                                special.useCooldownSeconds = "";
                                special.useCooldownGroup = "";
                            })
                    );
                    resetAll.horizontalSizing(Sizing.fill(100));
                    actions.child(resetAll);
                    content.child(actions);
                    return content;
                }
        );
    }

    static FlowLayout collapsibleCard(
            SpecialDataPanelContext context,
            Component title,
            boolean collapsed,
            Consumer<Boolean> setter,
            Supplier<FlowLayout> contentBuilder
    ) {
        FlowLayout card = UiFactory.subCard();
        FlowLayout header = UiFactory.row();
        int toggleWidth = compactIconButtonWidth();
        int preferredTitleWidth = Math.max(30, guiWidth() - toggleWidth - UiFactory.scaledPixels(COLLAPSIBLE_HEADER_TITLE_RESERVE));
        int titleWidth = Math.clamp(preferredTitleWidth, 1, Math.max(1, guiWidth()));
        Component fittedTitle = UiFactory.fitToWidth(title, titleWidth);
        var titleLabel = UiFactory.title(fittedTitle).shadow(false).horizontalSizing(Sizing.expand(100));
        if (!Objects.equals(fittedTitle.getString(), title.getString())) {
            titleLabel.tooltip(List.of(title));
        }
        header.child(titleLabel);
        ButtonComponent toggle = UiFactory.button(Component.literal(collapsed ? SYMBOL_SECTION_COLLAPSED : SYMBOL_SECTION_EXPANDED), UiFactory.ButtonTextPreset.STANDARD,  button -> {
            setter.accept(!collapsed);
            context.screen().refreshCurrentPanel();
        });
        toggle.horizontalSizing(Sizing.fixed(toggleWidth));
        header.child(toggle);
        card.child(header);
        if (!collapsed) {
            card.child(contentBuilder.get());
        }
        return card;
    }

    static FlowLayout compactTextField(
            SpecialDataPanelContext context,
            Component label,
            String value,
            Consumer<String> setter,
            int width
    ) {
        return compactField(label, filledTextBox(context, value, setter), width + 40);
    }

    static UIComponent filledTextBox(
            SpecialDataPanelContext context,
            String value,
            Consumer<String> setter
    ) {
        return UiFactory.textBox(value, context.bindText(setter)).horizontalSizing(Sizing.fill(100));
    }

    private static FlowLayout compactAnimationPicker(
            SpecialDataPanelContext context,
            ItemEditorState.SpecialData special,
            int buttonWidth
    ) {
        ButtonComponent button = UiFactory.button(
                PickerFieldFactory.selectedOrFallback(special.consumableAnimation, ItemEditorText.tr("special.advanced.select")), UiFactory.ButtonTextPreset.STANDARD,
                anchor -> context.openClearableDropdown(
                        anchor,
                        ItemEditorText.tr("common.none"),
                        () -> context.mutate(() -> special.consumableAnimation = ""),
                        Arrays.asList(ItemUseAnimation.values()),
                        ItemUseAnimation::name,
                        animation -> context.mutate(() -> special.consumableAnimation = animation.name())
                )
        );
        button.horizontalSizing(Sizing.fill(100));
        return compactField(ItemEditorText.tr("special.advanced.consumable.animation"), button, buttonWidth + 40);
    }

    static FlowLayout compactTriStateBooleanPicker(
            SpecialDataPanelContext context,
            Component label,
            String value,
            Consumer<String> setter,
            int buttonWidth
    ) {
        ButtonComponent button = UiFactory.actionToneButton(
                TriStateBooleanUi.label(value),
                UiFactory.ButtonTextPreset.STANDARD,
                TriStateBooleanUi.tone(value),
                anchor -> context.openDropdown(
                        anchor,
                        TriStateBooleanUi.VALUES,
                        TriStateBooleanUi::text,
                        selected -> context.mutateRefresh(() -> setter.accept(selected))
                )
        );
        button.horizontalSizing(Sizing.fill(100));
        return compactField(label, button, buttonWidth + 40);
    }

    static FlowLayout compactIdField(
            SpecialDataPanelContext context,
            Component label,
            String value,
            Consumer<String> setter,
            List<String> entries,
            String pickerTitle,
            int textWidth
    ) {
        return compactField(label, textWithPickerCompact(context, value, setter, entries, pickerTitle, true), textWidth + 110);
    }

    private static FlowLayout itemIdInputWithStoragePick(
            SpecialDataPanelContext context,
            String value,
            Consumer<String> setter,
            Consumer<ItemStack> storageSelectionConsumer,
            List<String> entries,
            String pickerTitle
    ) {
        int rowGap = holderSetRowGap();
        int panelWidth = guiWidth();
        int pickWidth = compactFixedPickButtonWidth();
        int storageWidth = storagePickButtonWidth();
        int minInputWidth = Math.min(Math.max(1, panelWidth), STORAGE_PICK_INPUT_MIN_WIDTH);
        boolean stacked = panelWidth < minInputWidth + pickWidth + storageWidth + (rowGap * 2);
        FlowLayout row = stacked ? UiFactory.column() : UiFactory.row();
        row.gap(rowGap);

        UIComponent input = UiFactory.textBox(
                value,
                text -> context.mutate(() -> setter.accept(IdFieldNormalizer.normalize(text)))
        ).horizontalSizing(Sizing.expand(100));
        ButtonComponent pickButton = UiFactory.button(ItemEditorText.tr("common.pick"), UiFactory.ButtonTextPreset.STANDARD, button ->
                context.openSearchablePicker(
                        pickerTitle,
                        "",
                        entries,
                        id -> id,
                        id -> context.mutateRefresh(() -> setter.accept(id))
                )
        );

        ButtonComponent storageButton = context.storagePickButton(storageSelectionConsumer);
        if (stacked) {
            row.child(input.horizontalSizing(Sizing.fill(100)));
            FlowLayout buttons = UiFactory.row();
            buttons.gap(rowGap);
            pickButton.horizontalSizing(Sizing.fill(50));
            storageButton.horizontalSizing(Sizing.fill(50));
            buttons.child(pickButton);
            buttons.child(storageButton);
            row.child(buttons.horizontalSizing(Sizing.fill(100)));
        } else {
            row.child(input);
            pickButton.horizontalSizing(Sizing.fixed(pickWidth));
            row.child(pickButton);
            storageButton.horizontalSizing(Sizing.fixed(storageWidth));
            row.child(storageButton);
        }
        return row;
    }

    private static int storagePickButtonWidth() {
        return clampToPanelWidth(Math.max(
                STORAGE_PICK_BUTTON_MIN,
                UiFactory.scaledPixels(STORAGE_PICK_BUTTON_BASE)
        ));
    }

    private static String encodeItemStackTemplate(SpecialDataPanelContext context, ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "";
        }
        try {
            var ops = context.screen().session().registryAccess().createSerializationContext(NbtOps.INSTANCE);
            return ItemStackTemplate.CODEC.encodeStart(ops, ItemStackTemplate.fromNonEmptyStack(stack))
                    .result()
                    .map(Tag::toString)
                    .orElse("");
        } catch (RuntimeException ignored) {
            return "";
        }
    }

    private static ItemStack decodeItemStackTemplate(SpecialDataPanelContext context, String raw) {
        if (raw == null || raw.isBlank()) {
            return ItemStack.EMPTY;
        }
        try {
            var ops = context.screen().session().registryAccess().createSerializationContext(NbtOps.INSTANCE);
            Tag parsedTag = TagParser.create(ops).parseFully(raw);
            return ItemStackTemplate.CODEC.parse(ops, parsedTag)
                    .result()
                    .map(ItemStackTemplate::create)
                    .orElse(ItemStack.EMPTY);
        } catch (Exception ignored) {
            return ItemStack.EMPTY;
        }
    }

    private static String encodeLockPredicate(
            SpecialDataPanelContext context,
            ItemStack stack,
            ItemEditorState.SpecialData special
    ) {
        if (stack == null || stack.isEmpty()) {
            return "";
        }
        DataComponentMatchers componentMatchers = lockComponentMatchers(stack, special);
        MinMaxBounds.Ints count = special.lockMatchCount
                ? MinMaxBounds.Ints.exactly(Math.max(1, stack.getCount()))
                : MinMaxBounds.Ints.ANY;
        if (MinMaxBounds.Ints.ANY.equals(count) && DataComponentMatchers.ANY.equals(componentMatchers)) {
            return "";
        }
        ItemPredicate predicate = new ItemPredicate(
                Optional.of(HolderSet.direct(stack.typeHolder())),
                count,
                componentMatchers
        );
        try {
            var ops = context.screen().session().registryAccess().createSerializationContext(NbtOps.INSTANCE);
            return ItemPredicate.CODEC.encodeStart(ops, predicate)
                    .result()
                    .map(Tag::toString)
                    .orElse("");
        } catch (RuntimeException ignored) {
            return "";
        }
    }

    private static DataComponentMatchers lockComponentMatchers(ItemStack stack, ItemEditorState.SpecialData special) {
        if (special.lockMatchAllComponents) {
            return new DataComponentMatchers(DataComponentExactPredicate.allOf(stack.getComponents()), Map.of());
        }

        DataComponentExactPredicate.Builder builder = DataComponentExactPredicate.builder();
        boolean matched = false;
        if (special.lockMatchName) {
            matched |= expectComponent(builder, stack, DataComponents.CUSTOM_NAME);
            matched |= expectComponent(builder, stack, DataComponents.ITEM_NAME);
        }
        if (special.lockMatchLore) {
            matched |= expectComponent(builder, stack, DataComponents.LORE);
        }
        if (special.lockMatchEnchantments) {
            matched |= expectComponent(builder, stack, DataComponents.ENCHANTMENTS);
            matched |= expectComponent(builder, stack, DataComponents.STORED_ENCHANTMENTS);
        }
        if (special.lockMatchCustomData) {
            matched |= expectComponent(builder, stack, DataComponents.CUSTOM_DATA);
        }
        return matched ? new DataComponentMatchers(builder.build(), Map.of()) : DataComponentMatchers.ANY;
    }

    private static <T> boolean expectComponent(
            DataComponentExactPredicate.Builder builder,
            ItemStack stack,
            DataComponentType<T> componentType
    ) {
        T value = stack.get(componentType);
        if (value == null) {
            return false;
        }
        builder.expect(componentType, value);
        return true;
    }

    static FlowLayout compactField(Component label, UIComponent input, int labelWidth) {
        FlowLayout field = UiFactory.column();
        field.gap(2);
        int panelWidth = guiWidth();
        int availableLabelWidth = Math.max(
                80,
                panelWidth - UiFactory.scaledPixels(COMPACT_FIELD_LABEL_RESERVE)
        );
        int preferredLabelWidth = prefersStackedCompactRows()
                ? availableLabelWidth
                : Math.clamp(availableLabelWidth, 40, Math.max(40, labelWidth));
        int effectiveLabelWidth = Math.clamp(preferredLabelWidth, 1, Math.max(1, panelWidth));
        Component fittedLabel = UiFactory.fitToWidth(label, effectiveLabelWidth);
        var labelComponent = UiFactory.muted(fittedLabel, effectiveLabelWidth);
        labelComponent.horizontalSizing(Sizing.fill(100));
        if (!Objects.equals(fittedLabel.getString(), label.getString())) {
            labelComponent.tooltip(List.of(label));
        }
        field.child(labelComponent);
        field.child(input.horizontalSizing(Sizing.fill(100)));
        return field;
    }

    static int compactNumericFieldWidth() {
        return clampWidth(guiWidth(), 0.065, 64, 104);
    }

    static int compactTinyFieldWidth() {
        return clampWidth(guiWidth(), 0.05, 54, 80);
    }

    static int compactIdTextWidth() {
        return clampWidth(guiWidth(), 0.22, 104, 220);
    }

    private static int compactGroupFieldWidth() {
        return clampWidth(guiWidth(), 0.18, 96, 190);
    }

    static int compactPickerButtonWidth() {
        return clampWidth(guiWidth(), 0.16, 86, 150);
    }

    static int compactIconButtonWidth() {
        return clampToPanelWidth(Math.max(COMPACT_ICON_BUTTON_MIN, UiFactory.scaledPixels(COMPACT_ICON_BUTTON_BASE)));
    }

    private static int compactClearButtonWidth() {
        return clampToPanelWidth(Math.max(COMPACT_CLEAR_BUTTON_MIN, UiFactory.scaledPixels(COMPACT_CLEAR_BUTTON_BASE)));
    }

    static int compactRemoveButtonWidth() {
        return clampToPanelWidth(Math.max(COMPACT_REMOVE_BUTTON_MIN, UiFactory.scaledPixels(COMPACT_REMOVE_BUTTON_BASE)));
    }

    private static int compactFixedPickButtonWidth() {
        return clampToPanelWidth(Math.max(COMPACT_FIXED_PICK_BUTTON_MIN, UiFactory.scaledPixels(COMPACT_FIXED_PICK_BUTTON_BASE)));
    }

    static int compactLongFieldWidth() {
        return clampWidth(guiWidth(), 0.26, 136, 280);
    }

    private static int clampWidth(int sourceWidth, double ratio, int min, int max) {
        int value = (int) Math.round(sourceWidth * ratio);
        int preferred = Math.clamp(value, min, max);
        return Math.clamp(preferred, 1, Math.max(1, sourceWidth));
    }

    private static int clampToPanelWidth(int preferredWidth) {
        return Math.clamp(preferredWidth, 1, Math.max(1, guiWidth()));
    }

    private static int guiWidth() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gui.screen() instanceof ItemEditorScreen itemEditorScreen) {
            int hinted = itemEditorScreen.editorContentWidthHint();
            int reserve = Math.max(2, UiFactory.scaledPixels(PANEL_WIDTH_SAFETY_RESERVE));
            return Math.max(1, hinted - reserve);
        }
        return UiFactory.responsiveBodyTextWidth();
    }

    private static boolean isNarrowLayout() {
        return guiWidth() <= NARROW_LAYOUT_WIDTH_THRESHOLD;
    }

    static boolean prefersStackedCompactRows() {
        return guiWidth() <= STACKED_COMPACT_WIDTH_THRESHOLD;
    }

    private static boolean usesStackedPickerRows() {
        int requiredInlineWidth = Math.max(
                PICKER_ROW_INLINE_MIN_WIDTH,
                UiFactory.scaledPixels(PICKER_ROW_INPUT_MIN_WIDTH)
                        + compactFixedPickButtonWidth()
                        + UiFactory.scaledPixels(SECTION_ROW_GAP)
        );
        return guiWidth() < requiredInlineWidth;
    }

    private static boolean usesStackedBlockStateRows() {
        return guiWidth() <= UiFactory.scaledPixels(BLOCK_STATE_STACKED_ROW_WIDTH_THRESHOLD);
    }

    private static int blockStateLabelWidth() {
        return clampWidth(guiWidth(), 0.10, 72, 128);
    }

    static FlowLayout responsiveRow() {
        return prefersStackedCompactRows() ? UiFactory.column() : UiFactory.row();
    }

    static FlowLayout denseEquipmentRow() {
        return guiWidth() <= EQUIPMENT_DENSE_STACK_WIDTH_THRESHOLD ? UiFactory.column() : UiFactory.row();
    }

    static FlowLayout denseEquipmentRow(UIComponent... children) {
        FlowLayout row = denseEquipmentRow();
        if (children.length == 0) {
            return row;
        }
        if (guiWidth() <= EQUIPMENT_DENSE_STACK_WIDTH_THRESHOLD) {
            for (UIComponent child : children) {
                child.horizontalSizing(Sizing.fill(100));
                row.child(child);
            }
            return row;
        }
        int childWidth = distributedRowChildWidth(children.length);
        for (UIComponent child : children) {
            child.horizontalSizing(Sizing.fill(childWidth));
            row.child(child);
        }
        return row;
    }

    static FlowLayout compactCheckboxRow(UIComponent... children) {
        boolean stacked = guiWidth() <= EQUIPMENT_DENSE_STACK_WIDTH_THRESHOLD;
        FlowLayout row = stacked ? UiFactory.column() : UiFactory.row();
        row.gap(Math.max(1, UiFactory.scaleProfile().tightSpacing()));
        for (UIComponent child : children) {
            child.horizontalSizing(stacked ? Sizing.fill(100) : Sizing.content());
            row.child(child);
        }
        return row;
    }

    static void distributeRowChildren(FlowLayout row, UIComponent... children) {
        if (children.length == 0) {
            return;
        }
        if (prefersStackedCompactRows()) {
            for (UIComponent child : children) {
                child.horizontalSizing(Sizing.fill(100));
                row.child(child);
            }
            return;
        }
        int childWidth = distributedRowChildWidth(children.length);
        for (UIComponent child : children) {
            child.horizontalSizing(Sizing.fill(childWidth));
            row.child(child);
        }
    }

    private static int distributedRowChildWidth(int childCount) {
        return Math.max(1, (100 - Math.max(1, childCount)) / Math.max(1, childCount));
    }

    private static FlowLayout buildOnConsumeEffectsEditor(SpecialDataPanelContext context, ItemEditorState.SpecialData special) {
        return buildConsumeEffectsEditor(
                context,
                special.consumableOnConsumeEffects,
                "special.advanced.consumable.on_consume_effects",
                "special.advanced.consumable.add_effect",
                "special.advanced.consumable.effects_empty",
                "special.advanced.consumable.effect",
                false
        );
    }

    private static void setDeathProtectionEnabled(ItemEditorState.SpecialData special, boolean enabled) {
        special.deathProtection = enabled;
        if (enabled && special.deathProtectionEffects.isEmpty()) {
            addDefaultDeathProtectionEffects(special.deathProtectionEffects);
        }
    }

    private static void addDefaultDeathProtectionEffects(List<ItemEditorState.ConsumableEffectDraft> drafts) {
        ItemEditorState.ConsumableEffectDraft clearAll = new ItemEditorState.ConsumableEffectDraft();
        clearAll.type = ItemEditorState.ConsumableEffectDraft.TYPE_CLEAR_ALL_EFFECTS;
        drafts.add(clearAll);

        ItemEditorState.ConsumableEffectDraft apply = expandedConsumableEffectDraft();
        apply.effects.add(deathProtectionPotionEffect("minecraft:regeneration", 900, 1));
        apply.effects.add(deathProtectionPotionEffect("minecraft:absorption", 100, 1));
        apply.effects.add(deathProtectionPotionEffect("minecraft:fire_resistance", 800, 0));
        drafts.add(apply);
    }

    private static ItemEditorState.PotionEffectDraft deathProtectionPotionEffect(
            String effectId,
            int duration,
            int amplifier
    ) {
        ItemEditorState.PotionEffectDraft draft = new ItemEditorState.PotionEffectDraft();
        draft.effectId = effectId;
        draft.duration = Integer.toString(duration);
        draft.amplifier = Integer.toString(amplifier);
        return draft;
    }

    private static ItemEditorState.ConsumableEffectDraft expandedConsumableEffectDraft() {
        ItemEditorState.ConsumableEffectDraft draft = new ItemEditorState.ConsumableEffectDraft();
        draft.uiCollapsed = false;
        return draft;
    }

    static ItemEditorState.BlocksAttacksDamageReductionDraft expandedBlocksAttacksDamageReductionDraft() {
        ItemEditorState.BlocksAttacksDamageReductionDraft draft = new ItemEditorState.BlocksAttacksDamageReductionDraft();
        draft.uiCollapsed = false;
        return draft;
    }

    static ItemEditorState.ToolRuleDraft expandedToolRuleDraft() {
        ItemEditorState.ToolRuleDraft draft = new ItemEditorState.ToolRuleDraft();
        draft.uiCollapsed = false;
        return draft;
    }

    private static FlowLayout buildDeathProtectionEffectsEditor(
            SpecialDataPanelContext context,
            ItemEditorState.SpecialData special
    ) {
        return buildConsumeEffectsEditor(
                context,
                special.deathProtectionEffects,
                "special.advanced.component_tweaks.death_effects",
                "special.advanced.component_tweaks.add_death_effect",
                "special.advanced.component_tweaks.death_effects_empty",
                "special.advanced.component_tweaks.death_effect",
                true
        );
    }

    private static FlowLayout buildConsumeEffectsEditor(
            SpecialDataPanelContext context,
            List<ItemEditorState.ConsumableEffectDraft> drafts,
            String titleKey,
            String addKey,
            String emptyKey,
            String effectTitleKey,
            boolean includeClearAllEffects
    ) {
        FlowLayout card = UiFactory.subCard();
        card.child(UiFactory.title(ItemEditorText.tr(titleKey)).shadow(false));

        ButtonComponent addButton = UiFactory.button(ItemEditorText.tr(addKey), UiFactory.ButtonTextPreset.STANDARD,  button ->
                context.mutateRefresh(() -> drafts.add(expandedConsumableEffectDraft()))
        );
        addButton.horizontalSizing(Sizing.fill(100));
        card.child(addButton);

        if (drafts.isEmpty()) {
            card.child(UiFactory.muted(ItemEditorText.tr(emptyKey), CONSUMABLE_EFFECTS_EMPTY_HINT_WIDTH));
            return card;
        }

        List<String> effectTypeValues = includeClearAllEffects
                ? List.of(
                        ItemEditorState.ConsumableEffectDraft.TYPE_CLEAR_ALL_EFFECTS,
                        ItemEditorState.ConsumableEffectDraft.TYPE_APPLY_EFFECTS,
                        ItemEditorState.ConsumableEffectDraft.TYPE_PLAY_SOUND
                )
                : List.of(
                        ItemEditorState.ConsumableEffectDraft.TYPE_APPLY_EFFECTS,
                        ItemEditorState.ConsumableEffectDraft.TYPE_PLAY_SOUND
                );
        List<String> effectIds = context.optionalRegistryIds(Registries.MOB_EFFECT);
        List<String> sounds = context.optionalRegistryIds(Registries.SOUND_EVENT);

        for (int index = 0; index < drafts.size(); index++) {
            int currentIndex = index;
            ItemEditorState.ConsumableEffectDraft draft = drafts.get(index);
            String currentType = draft.type == null || draft.type.isBlank()
                    ? ItemEditorState.ConsumableEffectDraft.TYPE_APPLY_EFFECTS
                    : draft.type;

            FlowLayout effectCard = context.createReorderableCard(
                    ItemEditorText.tr(effectTitleKey, index + 1),
                    currentIndex > 0,
                    () -> context.swapEntries(drafts, currentIndex, currentIndex - 1),
                    currentIndex < drafts.size() - 1,
                    () -> context.swapEntries(drafts, currentIndex, currentIndex + 1),
                    () -> drafts.remove(currentIndex)
            );
            FlowLayout collapseRow = responsiveRow();
            UIComponent summary = UiFactory.muted(Component.literal(consumableEffectSummary(draft, currentType)), CONSUMABLE_EFFECT_SUMMARY_HINT_WIDTH);
            summary.horizontalSizing(Sizing.expand(100));
            collapseRow.child(summary);
            ButtonComponent collapseToggle = UiFactory.button(Component.literal(draft.uiCollapsed ? SYMBOL_SECTION_COLLAPSED : SYMBOL_SECTION_EXPANDED), UiFactory.ButtonTextPreset.STANDARD,  button ->
                    context.mutateRefresh(() -> draft.uiCollapsed = !draft.uiCollapsed)
            );
            collapseToggle.horizontalSizing(Sizing.fixed(compactIconButtonWidth()));
            collapseRow.child(collapseToggle);
            effectCard.child(collapseRow);

            if (draft.uiCollapsed) {
                card.child(effectCard);
                continue;
            }

            effectCard.child(PickerFieldFactory.dropdownField(
                    context,
                    ItemEditorText.tr("special.advanced.consumable.effect_type"),
                    Component.empty(),
                    Component.literal(effectTypeLabel(currentType)),
                    240,
                    effectTypeValues,
                    AdvancedItemSpecialDataSection::effectTypeLabel,
                    selectedType -> context.mutateRefresh(() -> draft.type = selectedType)
            ));

            if (Objects.equals(currentType, ItemEditorState.ConsumableEffectDraft.TYPE_CLEAR_ALL_EFFECTS)) {
                card.child(effectCard);
                continue;
            }

            if (Objects.equals(currentType, ItemEditorState.ConsumableEffectDraft.TYPE_PLAY_SOUND)) {
                effectCard.child(UiFactory.field(
                        ItemEditorText.tr("special.advanced.consumable.effect_sound"),
                        Component.empty(),
                        textWithPickerCompact(
                                context,
                                draft.soundId,
                                value -> draft.soundId = value,
                                sounds,
                                ItemEditorText.str("special.advanced.consumable.effect_sound"),
                                true
                        )
                ));
                card.child(effectCard);
                continue;
            }

            effectCard.child(UiFactory.field(
                    ItemEditorText.tr("special.advanced.consumable.effect_probability"),
                    Component.empty(),
                    filledTextBox(context, draft.probability, value -> draft.probability = value)
            ));

            ButtonComponent addPotionEffect = UiFactory.button(ItemEditorText.tr("special.potion.add_effect"), UiFactory.ButtonTextPreset.STANDARD,  button ->
                    context.mutateRefresh(() -> draft.effects.add(new ItemEditorState.PotionEffectDraft()))
            );
            addPotionEffect.horizontalSizing(Sizing.fill(100));
            effectCard.child(addPotionEffect);

            if (draft.effects.isEmpty()) {
                effectCard.child(UiFactory.muted(ItemEditorText.tr("special.advanced.consumable.apply_effects_empty"), CONSUMABLE_APPLY_EFFECTS_EMPTY_HINT_WIDTH));
                card.child(effectCard);
                continue;
            }

            for (int effectIndex = 0; effectIndex < draft.effects.size(); effectIndex++) {
                int currentEffectIndex = effectIndex;
                ItemEditorState.PotionEffectDraft effectDraft = draft.effects.get(effectIndex);

                FlowLayout potionCard = context.createRemovableCard(
                        ItemEditorText.tr("special.potion.effect", effectIndex + 1),
                        () -> draft.effects.remove(currentEffectIndex)
                );
                FlowLayout inputs = responsiveRow();
                UIComponent effectField = PickerFieldFactory.searchableField(
                        context,
                        ItemEditorText.tr("special.potion.effect_id"),
                        Component.empty(),
                        PickerFieldFactory.selectedOrFallback(effectDraft.effectId, ItemEditorText.tr("special.potion.select_effect")),
                        220,
                        ItemEditorText.str("special.potion.effect_id"),
                        "",
                        effectIds,
                        id -> id,
                        id -> context.mutateRefresh(() -> effectDraft.effectId = id)
                );
                UIComponent durationField = UiFactory.field(
                        ItemEditorText.tr("special.potion.duration"),
                        Component.empty(),
                        filledTextBox(context, effectDraft.duration, value -> effectDraft.duration = value)
                );
                UIComponent amplifierField = UiFactory.field(
                        ItemEditorText.tr("special.potion.amplifier"),
                        Component.empty(),
                        filledTextBox(context, effectDraft.amplifier, value -> effectDraft.amplifier = value)
                );
                distributeRowChildren(inputs, effectField, durationField, amplifierField);
                potionCard.child(inputs);

                UIComponent ambientToggle = UiFactory.checkbox(
                        ItemEditorText.tr("special.potion.ambient"),
                        effectDraft.ambient,
                        context.bindToggle(value -> effectDraft.ambient = value)
                );
                UIComponent visibleToggle = compactTriStateBooleanPicker(
                        context,
                        ItemEditorText.tr("special.potion.visible"),
                        effectDraft.visible,
                        value -> effectDraft.visible = value,
                        compactPickerButtonWidth()
                );
                UIComponent iconToggle = compactTriStateBooleanPicker(
                        context,
                        ItemEditorText.tr("special.potion.show_icon"),
                        effectDraft.showIcon,
                        value -> effectDraft.showIcon = value,
                        compactPickerButtonWidth()
                );
                potionCard.child(denseEquipmentRow(ambientToggle, visibleToggle, iconToggle));
                effectCard.child(potionCard);
            }

            card.child(effectCard);
        }

        return card;
    }

    private static String consumableEffectSummary(ItemEditorState.ConsumableEffectDraft draft, String currentType) {
        if (Objects.equals(currentType, ItemEditorState.ConsumableEffectDraft.TYPE_CLEAR_ALL_EFFECTS)) {
            return effectTypeLabel(currentType);
        }
        if (Objects.equals(currentType, ItemEditorState.ConsumableEffectDraft.TYPE_PLAY_SOUND)) {
            String sound = valueOrDefault(draft.soundId, "-");
            return effectTypeLabel(currentType) + " - " + sound;
        }
        int effectCount = draft.effects.size();
        String probability = draft.probability.isBlank() ? "1.0" : draft.probability;
        return ItemEditorText.str(
                "special.advanced.consumable.effect_summary",
                effectTypeLabel(currentType),
                effectCount,
                probability
        );
    }

    private static String beeSummary(ItemEditorState.BeeOccupantDraft draft) {
        String entity = valueOrDefault(draft.entityId, "minecraft:bee");
        String ticks = valueOrDefault(draft.ticksInHive, "0");
        String minTicks = valueOrDefault(draft.minTicksInHive, "0");
        return ItemEditorText.str("special.advanced.container_meta.bee_summary", entity, ticks, minTicks);
    }

    private static String projectileSummary(ItemEditorState.ChargedProjectileDraft draft) {
        String item = valueOrDefault(draft.itemId, "-");
        String count = valueOrDefault(draft.count, "1");
        String summary = item + " x" + count;
        return draft.templateSnbt == null || draft.templateSnbt.isBlank()
                ? summary
                : summary + " | " + ItemEditorText.str("special.advanced.crossbow.full_item_data");
    }

    private static String mapDecorationSummary(ItemEditorState.MapDecorationDraft draft) {
        String key = valueOrDefault(draft.key, "-");
        String type = valueOrDefault(draft.typeId, "-");
        String x = valueOrDefault(draft.x, "0");
        String z = valueOrDefault(draft.z, "0");
        return key + " - " + type + " (" + x + ", " + z + ")";
    }

    static String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static int adjustNumericString(String raw, int delta) {
        int value = ValidationUtil.parseIntOrDefault(raw, 1);
        value += delta;
        return Math.max(1, value);
    }

    private static String effectTypeLabel(String effectTypeId) {
        if (Objects.equals(effectTypeId, ItemEditorState.ConsumableEffectDraft.TYPE_CLEAR_ALL_EFFECTS)) {
            return ItemEditorText.str("special.advanced.consumable.effect_type.clear_all_effects");
        }
        if (Objects.equals(effectTypeId, ItemEditorState.ConsumableEffectDraft.TYPE_PLAY_SOUND)) {
            return ItemEditorText.str("special.advanced.consumable.effect_type.play_sound");
        }
        if (Objects.equals(effectTypeId, ItemEditorState.ConsumableEffectDraft.TYPE_APPLY_EFFECTS)) {
            return ItemEditorText.str("special.advanced.consumable.effect_type.apply_effects");
        }
        return effectTypeId;
    }

    private static void configureLockKeyFromStack(
            SpecialDataPanelContext context,
            ItemEditorState.SpecialData special,
            ItemStack stack
    ) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        special.lockItemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        special.lockKeyTemplateSnbt = encodeItemStackTemplate(context, stack);
        special.lockMatchCount = true;
        special.lockMatchName = true;
        special.lockMatchLore = true;
        special.lockMatchEnchantments = true;
        special.lockMatchCustomData = true;
        special.lockMatchAllComponents = true;
        refreshLockPredicateFromSource(context, special);
    }

    private static void resetLockPredicateConfig(ItemEditorState.SpecialData special) {
        special.lockPredicateSnbt = "";
        special.lockKeyTemplateSnbt = "";
        special.lockMatchCount = false;
        special.lockMatchName = false;
        special.lockMatchLore = false;
        special.lockMatchEnchantments = false;
        special.lockMatchCustomData = false;
        special.lockMatchAllComponents = false;
    }

    private static void refreshLockPredicateFromSource(
            SpecialDataPanelContext context,
            ItemEditorState.SpecialData special
    ) {
        ItemStack keyStack = decodeItemStackTemplate(context, special.lockKeyTemplateSnbt);
        if (keyStack.isEmpty()) {
            special.lockPredicateSnbt = "";
            return;
        }
        if (special.lockItemId == null || special.lockItemId.isBlank()) {
            special.lockItemId = BuiltInRegistries.ITEM.getKey(keyStack.getItem()).toString();
        }
        special.lockPredicateSnbt = encodeLockPredicate(context, keyStack, special);
    }

    private static FlowLayout buildLockMatchOptions(
            SpecialDataPanelContext context,
            ItemEditorState.SpecialData special
    ) {
        FlowLayout card = UiFactory.subCard();
        card.child(UiFactory.title(ItemEditorText.tr("special.advanced.container_meta.lock_match_title")).shadow(false));
        card.child(UiFactory.muted(ItemEditorText.tr("special.advanced.container_meta.lock_match_hint"), CONTAINER_META_LOCK_HINT_WIDTH));

        FlowLayout rows = UiFactory.column();
        rows.gap(2);
        rows.child(lockMatchRow(
                lockMatchCheckbox(context, special, "special.advanced.container_meta.lock_match_count", special.lockMatchCount, value -> special.lockMatchCount = value),
                lockMatchCheckbox(context, special, "special.advanced.container_meta.lock_match_name", special.lockMatchName, value -> special.lockMatchName = value),
                lockMatchCheckbox(context, special, "special.advanced.container_meta.lock_match_lore", special.lockMatchLore, value -> special.lockMatchLore = value)
        ));
        rows.child(lockMatchRow(
                lockMatchCheckbox(context, special, "special.advanced.container_meta.lock_match_enchantments", special.lockMatchEnchantments, value -> special.lockMatchEnchantments = value),
                lockMatchCheckbox(context, special, "special.advanced.container_meta.lock_match_custom_data", special.lockMatchCustomData, value -> special.lockMatchCustomData = value),
                lockMatchCheckbox(context, special, "special.advanced.container_meta.lock_match_all_components", special.lockMatchAllComponents, value -> special.lockMatchAllComponents = value)
        ));
        card.child(rows);

        ButtonComponent fullItem = UiFactory.actionRowButton(
                ItemEditorText.tr("special.advanced.container_meta.lock_match_full_item"),
                UiFactory.ButtonTextPreset.STANDARD,
                UiFactory.ActionTone.PICKER,
                button -> context.mutateRefresh(() -> {
                    special.lockMatchCount = true;
                    special.lockMatchName = true;
                    special.lockMatchLore = true;
                    special.lockMatchEnchantments = true;
                    special.lockMatchCustomData = true;
                    special.lockMatchAllComponents = true;
                    refreshLockPredicateFromSource(context, special);
                })
        );
        ButtonComponent simple = UiFactory.negativeButton(
                ItemEditorText.tr("special.advanced.container_meta.lock_reset_simple"),
                UiFactory.ButtonTextPreset.STANDARD,
                button -> context.mutateRefresh(() -> resetLockPredicateConfig(special))
        );
        card.child(UiFactory.actionButtonRow(fullItem, simple));
        return card;
    }

    private static UIComponent lockMatchCheckbox(
            SpecialDataPanelContext context,
            ItemEditorState.SpecialData special,
            String labelKey,
            boolean checked,
            Consumer<Boolean> setter
    ) {
        return UiFactory.checkbox(
                ItemEditorText.tr(labelKey),
                checked,
                value -> context.mutateRefresh(() -> {
                    setter.accept(value);
                    refreshLockPredicateFromSource(context, special);
                })
        );
    }

    private static FlowLayout lockMatchRow(UIComponent first, UIComponent second, UIComponent third) {
        if (guiWidth() <= UiFactory.scaledPixels(640)) {
            FlowLayout column = UiFactory.column();
            column.gap(2);
            column.child(first);
            column.child(second);
            column.child(third);
            return column;
        }
        FlowLayout row = UiFactory.row();
        row.gap(Math.max(1, UiFactory.scaledPixels(4)));
        first.horizontalSizing(Sizing.fill(33));
        second.horizontalSizing(Sizing.fill(33));
        third.horizontalSizing(Sizing.fill(33));
        row.child(first);
        row.child(second);
        row.child(third);
        return row;
    }

    private static Component lockSummary(ItemEditorState.SpecialData special) {
        String itemId = special.lockItemId == null ? "" : special.lockItemId.trim();
        String predicate = special.lockPredicateSnbt == null ? "" : special.lockPredicateSnbt.trim();
        if (itemId.isBlank() && predicate.isBlank()) {
            return ItemEditorText.tr("special.advanced.container_meta.lock_summary_none");
        }

        List<String> parts = new ArrayList<>();
        parts.add(itemId.isBlank() ? "-" : itemId);
        if (predicate.isBlank()) {
            parts.add(ItemEditorText.str("special.advanced.container_meta.lock_summary_simple"));
        } else if (special.lockKeyTemplateSnbt == null || special.lockKeyTemplateSnbt.isBlank()) {
            parts.add(ItemEditorText.str("special.advanced.container_meta.lock_summary_advanced"));
        } else {
            if (special.lockMatchCount) {
                parts.add(ItemEditorText.str("special.advanced.container_meta.lock_summary_count"));
            }
            if (special.lockMatchAllComponents) {
                parts.add(ItemEditorText.str("special.advanced.container_meta.lock_summary_all_components"));
            } else {
                addLockSummaryPart(parts, special.lockMatchName, "special.advanced.container_meta.lock_summary_name");
                addLockSummaryPart(parts, special.lockMatchLore, "special.advanced.container_meta.lock_summary_lore");
                addLockSummaryPart(parts, special.lockMatchEnchantments, "special.advanced.container_meta.lock_summary_enchantments");
                addLockSummaryPart(parts, special.lockMatchCustomData, "special.advanced.container_meta.lock_summary_custom_data");
            }
        }
        return Component.literal(String.join(" | ", parts));
    }

    private static void addLockSummaryPart(List<String> parts, boolean enabled, String labelKey) {
        if (enabled) {
            parts.add(ItemEditorText.str(labelKey));
        }
    }

    public static FlowLayout buildContainerMetadata(SpecialDataPanelContext context) {
        ItemEditorState.SpecialData special = context.special();
        List<String> availableItems = context.itemIdsWithoutAir();
        return collapsibleCard(
                context,
                ItemEditorText.tr("special.advanced.container_meta.title"),
                special.uiContainerMetadataCollapsed,
                value -> special.uiContainerMetadataCollapsed = value,
                () -> {
                    FlowLayout content = UiFactory.column();
                    int idWidth = compactIdTextWidth();
                    int lockPredicateWidth = compactLongFieldWidth();
                    int seedWidth = compactNumericFieldWidth();
                    List<String> lootTables = LootTableIds.fromResources(
                            context.screen().session().minecraft().getResourceManager()
                    );

                     content.child(compactField(
                             ItemEditorText.tr("special.advanced.container_meta.lock_item"),
                             itemIdInputWithStoragePick(
                                     context,
                                     special.lockItemId,
                                     value -> {
                                         special.lockItemId = value;
                                        resetLockPredicateConfig(special);
                                     },
                                     stack -> configureLockKeyFromStack(context, special, stack),
                                     availableItems,
                                     ItemEditorText.str("special.advanced.container_meta.lock_item")
                             ),
                             idWidth + 170
                     ));
                    content.child(UiFactory.muted(lockSummary(special), CONTAINER_META_LOCK_HINT_WIDTH));
                    if (!special.lockKeyTemplateSnbt.isBlank()) {
                        content.child(buildLockMatchOptions(context, special));
                    }
                    content.child(compactIdField(
                            context,
                            ItemEditorText.tr("special.advanced.container_meta.loot_table"),
                            special.containerLootTableId,
                            value -> special.containerLootTableId = value,
                            lootTables,
                            ItemEditorText.str("special.advanced.container_meta.loot_table"),
                            idWidth
                    ));

                    content.child(compactTextField(
                            context,
                            ItemEditorText.tr("special.advanced.container_meta.lock_predicate"),
                            special.lockPredicateSnbt,
                            value -> {
                                special.lockPredicateSnbt = value;
                                special.lockKeyTemplateSnbt = "";
                            },
                            lockPredicateWidth
                    ));
                    content.child(UiFactory.muted(ItemEditorText.tr("special.advanced.container_meta.lock_predicate_hint"), CONTAINER_META_LOCK_HINT_WIDTH));

                    content.child(compactTextField(
                            context,
                            ItemEditorText.tr("special.advanced.container_meta.loot_seed"),
                            special.containerLootSeed,
                            value -> special.containerLootSeed = value,
                            seedWidth
                    ));

                    content.child(buildBeesEditor(context, special));

                    content.child(buildPotDecorations(context, special, availableItems));
                    return content;
                }
        );
    }

    private static FlowLayout buildBeesEditor(SpecialDataPanelContext context, ItemEditorState.SpecialData special) {
        FlowLayout card = UiFactory.subCard();
        card.child(UiFactory.title(ItemEditorText.tr("special.advanced.container_meta.bees_title")).shadow(false));

        ButtonComponent addButton = UiFactory.button(ItemEditorText.tr("special.advanced.container_meta.bees_add"), UiFactory.ButtonTextPreset.STANDARD,  button ->
                context.mutateRefresh(() -> {
                    ItemEditorState.BeeOccupantDraft draft = new ItemEditorState.BeeOccupantDraft();
                    draft.uiCollapsed = false;
                    special.beesOccupants.add(draft);
                })
        );
        ButtonComponent clearAll = UiFactory.button(ItemEditorText.tr("common.clear_all"), UiFactory.ButtonTextPreset.STANDARD,  button ->
                context.mutateRefresh(special.beesOccupants::clear)
        );
        clearAll.active = !special.beesOccupants.isEmpty();
        card.child(UiFactory.actionButtonRow(addButton, clearAll));
        if (!special.beesOccupants.isEmpty()) {
            ButtonComponent expandAll = UiFactory.button(ItemEditorText.tr("common.expand_all"), UiFactory.ButtonTextPreset.STANDARD,  button ->
                    context.mutateRefresh(() -> special.beesOccupants.forEach(entry -> entry.uiCollapsed = false))
            );
            ButtonComponent collapseAll = UiFactory.button(ItemEditorText.tr("common.collapse_all"), UiFactory.ButtonTextPreset.STANDARD,  button ->
                    context.mutateRefresh(() -> special.beesOccupants.forEach(entry -> entry.uiCollapsed = true))
            );

            card.child(UiFactory.actionButtonRow(expandAll, collapseAll));
        }

        if (special.beesOccupants.isEmpty()) {
            card.child(UiFactory.muted(ItemEditorText.tr("special.advanced.container_meta.bees_empty"), CONTAINER_META_BEES_EMPTY_HINT_WIDTH));
            return card;
        }

        int idWidth = compactIdTextWidth();
        int numberWidth = compactNumericFieldWidth();
        List<String> entityTypeIds = context.optionalRegistryIds(Registries.ENTITY_TYPE);
        for (int index = 0; index < special.beesOccupants.size(); index++) {
            int currentIndex = index;
            ItemEditorState.BeeOccupantDraft draft = special.beesOccupants.get(index);
            FlowLayout beeCard = context.createReorderableCard(
                    ItemEditorText.tr("special.advanced.container_meta.bee", index + 1),
                    currentIndex > 0,
                    () -> context.swapEntries(special.beesOccupants, currentIndex, currentIndex - 1),
                    currentIndex < special.beesOccupants.size() - 1,
                    () -> context.swapEntries(special.beesOccupants, currentIndex, currentIndex + 1),
                    () -> special.beesOccupants.remove(currentIndex)
            );

            FlowLayout summaryRow = responsiveRow();
            UIComponent summary = UiFactory.muted(Component.literal(beeSummary(draft)), CONTAINER_META_BEE_SUMMARY_HINT_WIDTH);
            summary.horizontalSizing(Sizing.expand(100));
            summaryRow.child(summary);
            ButtonComponent collapseToggle = UiFactory.button(Component.literal(draft.uiCollapsed ? SYMBOL_SECTION_COLLAPSED : SYMBOL_SECTION_EXPANDED), UiFactory.ButtonTextPreset.STANDARD,  button ->
                    context.mutateRefresh(() -> draft.uiCollapsed = !draft.uiCollapsed)
            );
            collapseToggle.horizontalSizing(Sizing.fixed(compactIconButtonWidth()));
            summaryRow.child(collapseToggle);
            beeCard.child(summaryRow);

            if (!draft.uiCollapsed) {
                beeCard.child(compactIdField(
                        context,
                        ItemEditorText.tr("special.advanced.container_meta.bees_entity"),
                        draft.entityId,
                        value -> draft.entityId = value,
                        entityTypeIds,
                        ItemEditorText.str("special.advanced.container_meta.bees_entity"),
                        idWidth
                ));

                FlowLayout ticksField = compactTextField(
                        context,
                        ItemEditorText.tr("special.advanced.container_meta.bees_ticks"),
                        draft.ticksInHive,
                        value -> draft.ticksInHive = value,
                        numberWidth
                );
                FlowLayout minTicksField = compactTextField(
                        context,
                        ItemEditorText.tr("special.advanced.container_meta.bees_min_ticks"),
                        draft.minTicksInHive,
                        value -> draft.minTicksInHive = value,
                        numberWidth
                );
                beeCard.child(denseEquipmentRow(ticksField, minTicksField));
            }
            card.child(beeCard);
        }
        return card;
    }

    public static FlowLayout buildCrossbow(SpecialDataPanelContext context) {
        ItemEditorState.SpecialData special = context.special();
        List<String> availableItems = context.itemIdsWithoutAir();
        return collapsibleCard(
                context,
                ItemEditorText.tr("special.advanced.crossbow.title"),
                special.uiCrossbowCollapsed,
                value -> special.uiCrossbowCollapsed = value,
                () -> {
                    FlowLayout content = UiFactory.column();
                    int idWidth = compactIdTextWidth();
                    int countWidth = compactTinyFieldWidth();

                    ButtonComponent addButton = UiFactory.button(ItemEditorText.tr("special.advanced.crossbow.add_projectile"), UiFactory.ButtonTextPreset.STANDARD,  button ->
                            context.mutateRefresh(() -> {
                                ItemEditorState.ChargedProjectileDraft draft = new ItemEditorState.ChargedProjectileDraft();
                                draft.uiCollapsed = false;
                                special.chargedProjectiles.add(draft);
                            })
                    );
                    addButton.horizontalSizing(Sizing.fill(100));
                    if (!special.chargedProjectiles.isEmpty()) {
                        ButtonComponent clearAll = UiFactory.button(ItemEditorText.tr("common.clear_all"), UiFactory.ButtonTextPreset.STANDARD,  button ->
                                context.mutateRefresh(special.chargedProjectiles::clear)
                        );
                        content.child(UiFactory.actionButtonRow(addButton, clearAll));

                        ButtonComponent expandAll = UiFactory.button(ItemEditorText.tr("common.expand_all"), UiFactory.ButtonTextPreset.STANDARD,  button ->
                                context.mutateRefresh(() -> special.chargedProjectiles.forEach(entry -> entry.uiCollapsed = false))
                        );
                        ButtonComponent collapseAll = UiFactory.button(ItemEditorText.tr("common.collapse_all"), UiFactory.ButtonTextPreset.STANDARD,  button ->
                                context.mutateRefresh(() -> special.chargedProjectiles.forEach(entry -> entry.uiCollapsed = true))
                        );

                        content.child(UiFactory.actionButtonRow(expandAll, collapseAll));
                    } else {
                        content.child(addButton);
                    }

                    if (special.chargedProjectiles.isEmpty()) {
                        content.child(UiFactory.muted(
                                ItemEditorText.tr("special.advanced.crossbow.empty"),
                                CROSSBOW_EMPTY_HINT_WIDTH
                        ));
                        return content;
                    }

                    for (int index = 0; index < special.chargedProjectiles.size(); index++) {
                        int currentIndex = index;
                        ItemEditorState.ChargedProjectileDraft draft = special.chargedProjectiles.get(index);
                        FlowLayout card = UiFactory.reorderableCollapsibleSubCard(
                                ItemEditorText.tr("special.advanced.crossbow.projectile", index + 1),
                                Component.literal(projectileSummary(draft)),
                                CROSSBOW_PROJECTILE_SUMMARY_HINT_WIDTH,
                                draft.uiCollapsed,
                                () -> context.mutateRefresh(() -> draft.uiCollapsed = !draft.uiCollapsed),
                                currentIndex > 0,
                                () -> context.mutateRefresh(() -> context.swapEntries(special.chargedProjectiles, currentIndex, currentIndex - 1)),
                                currentIndex < special.chargedProjectiles.size() - 1,
                                () -> context.mutateRefresh(() -> context.swapEntries(special.chargedProjectiles, currentIndex, currentIndex + 1)),
                                () -> context.mutateRefresh(() -> special.chargedProjectiles.remove(currentIndex))
                        );

                        if (!draft.uiCollapsed) {
                            card.child(compactField(
                                    ItemEditorText.tr("special.advanced.crossbow.item"),
                                    itemIdInputWithStoragePick(
                                            context,
                                            draft.itemId,
                                            value -> {
                                                draft.itemId = value;
                                                draft.templateSnbt = "";
                                            },
                                            stack -> {
                                                draft.itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                                                draft.count = Integer.toString(Math.max(1, stack.getCount()));
                                                draft.templateSnbt = encodeItemStackTemplate(context, stack);
                                            },
                                            availableItems,
                                            ItemEditorText.str("special.advanced.crossbow.item")
                                    ),
                                    idWidth + 170
                            ));

                            FlowLayout countRow = responsiveRow();
                            FlowLayout countField = compactTextField(
                                    context,
                                    ItemEditorText.tr("special.advanced.crossbow.count"),
                                    draft.count,
                                    value -> draft.count = value,
                                    countWidth
                            );
                            ButtonComponent decrement = UiFactory.button(Component.literal("-"), UiFactory.ButtonTextPreset.STANDARD,  button ->
                                    context.mutateRefresh(() -> draft.count = Integer.toString(adjustNumericString(draft.count, -1)))
                            );
                            decrement.horizontalSizing(Sizing.fixed(compactIconButtonWidth()));
                            ButtonComponent increment = UiFactory.button(Component.literal("+"), UiFactory.ButtonTextPreset.STANDARD,  button ->
                                    context.mutateRefresh(() -> draft.count = Integer.toString(adjustNumericString(draft.count, 1)))
                            );
                            increment.horizontalSizing(Sizing.fixed(compactIconButtonWidth()));
                            distributeRowChildren(countRow, countField, decrement, increment);
                            card.child(countRow);
                        }
                        content.child(card);
                    }

                    return content;
                }
        );
    }

    public static FlowLayout buildMapAdvanced(SpecialDataPanelContext context) {
        ItemEditorState.SpecialData special = context.special();
        return collapsibleCard(
                context,
                ItemEditorText.tr("special.advanced.map.title"),
                special.uiMapAdvancedCollapsed,
                value -> special.uiMapAdvancedCollapsed = value,
                () -> {
                    FlowLayout content = UiFactory.column();
                    content.child(UiFactory.field(
                            ItemEditorText.tr("special.advanced.map.map_id"),
                            Component.empty(),
                            filledTextBox(context, special.mapId, value -> special.mapId = value)
                    ));
                    content.child(buildMapDecorationsEditor(context, special));
                    content.child(buildLodestoneEditor(context, special));
                    return content;
                }
        );
    }

    private static FlowLayout buildPotDecorations(
            SpecialDataPanelContext context,
            ItemEditorState.SpecialData special,
            List<String> itemIds
    ) {
        FlowLayout card = UiFactory.subCard();
        card.child(UiFactory.title(ItemEditorText.tr("special.advanced.container_meta.pot_title")).shadow(false));
        int idWidth = compactIdTextWidth();
        boolean narrowLayout = isNarrowLayout();

        if (narrowLayout) {
            card.child(compactIdField(
                    context,
                    ItemEditorText.tr("special.advanced.container_meta.pot_back"),
                    special.potBackItemId,
                    value -> special.potBackItemId = value,
                    itemIds,
                    ItemEditorText.str("special.advanced.container_meta.pot_back"),
                    idWidth
            ));
            card.child(compactIdField(
                    context,
                    ItemEditorText.tr("special.advanced.container_meta.pot_left"),
                    special.potLeftItemId,
                    value -> special.potLeftItemId = value,
                    itemIds,
                    ItemEditorText.str("special.advanced.container_meta.pot_left"),
                    idWidth
            ));
            card.child(compactIdField(
                    context,
                    ItemEditorText.tr("special.advanced.container_meta.pot_right"),
                    special.potRightItemId,
                    value -> special.potRightItemId = value,
                    itemIds,
                    ItemEditorText.str("special.advanced.container_meta.pot_right"),
                    idWidth
            ));
            card.child(compactIdField(
                    context,
                    ItemEditorText.tr("special.advanced.container_meta.pot_front"),
                    special.potFrontItemId,
                    value -> special.potFrontItemId = value,
                    itemIds,
                    ItemEditorText.str("special.advanced.container_meta.pot_front"),
                    idWidth
            ));
            return card;
        }

        FlowLayout rowA = responsiveRow();
        FlowLayout backField = compactIdField(
                context,
                ItemEditorText.tr("special.advanced.container_meta.pot_back"),
                special.potBackItemId,
                value -> special.potBackItemId = value,
                itemIds,
                ItemEditorText.str("special.advanced.container_meta.pot_back"),
                idWidth
        );
        FlowLayout leftField = compactIdField(
                context,
                ItemEditorText.tr("special.advanced.container_meta.pot_left"),
                special.potLeftItemId,
                value -> special.potLeftItemId = value,
                itemIds,
                ItemEditorText.str("special.advanced.container_meta.pot_left"),
                idWidth
        );
        distributeRowChildren(rowA, backField, leftField);
        card.child(rowA);

        FlowLayout rowB = responsiveRow();
        FlowLayout rightField = compactIdField(
                context,
                ItemEditorText.tr("special.advanced.container_meta.pot_right"),
                special.potRightItemId,
                value -> special.potRightItemId = value,
                itemIds,
                ItemEditorText.str("special.advanced.container_meta.pot_right"),
                idWidth
        );
        FlowLayout frontField = compactIdField(
                context,
                ItemEditorText.tr("special.advanced.container_meta.pot_front"),
                special.potFrontItemId,
                value -> special.potFrontItemId = value,
                itemIds,
                ItemEditorText.str("special.advanced.container_meta.pot_front"),
                idWidth
        );
        distributeRowChildren(rowB, rightField, frontField);
        card.child(rowB);
        return card;
    }

    private static FlowLayout buildMapDecorationsEditor(SpecialDataPanelContext context, ItemEditorState.SpecialData special) {
        List<String> decorationTypeIds = context.optionalRegistryIds(Registries.MAP_DECORATION_TYPE);
        FlowLayout card = UiFactory.subCard();
        card.child(UiFactory.title(ItemEditorText.tr("special.advanced.map.decorations_title")).shadow(false));

        int keyWidth = compactGroupFieldWidth();
        int idWidth = compactIdTextWidth();
        int numberWidth = compactNumericFieldWidth();

        ButtonComponent addButton = UiFactory.button(ItemEditorText.tr("special.advanced.map.add_decoration"), UiFactory.ButtonTextPreset.STANDARD,  button ->
                context.mutateRefresh(() -> {
                    ItemEditorState.MapDecorationDraft draft = new ItemEditorState.MapDecorationDraft();
                    draft.uiCollapsed = false;
                    special.mapDecorations.add(draft);
                })
        );
        ButtonComponent clearAll = UiFactory.button(ItemEditorText.tr("common.clear_all"), UiFactory.ButtonTextPreset.STANDARD,  button ->
                context.mutateRefresh(special.mapDecorations::clear)
        );
        clearAll.active = !special.mapDecorations.isEmpty();
        card.child(UiFactory.actionButtonRow(addButton, clearAll));
        if (!special.mapDecorations.isEmpty()) {
            ButtonComponent expandAll = UiFactory.button(ItemEditorText.tr("common.expand_all"), UiFactory.ButtonTextPreset.STANDARD,  button ->
                    context.mutateRefresh(() -> special.mapDecorations.forEach(entry -> entry.uiCollapsed = false))
            );
            ButtonComponent collapseAll = UiFactory.button(ItemEditorText.tr("common.collapse_all"), UiFactory.ButtonTextPreset.STANDARD,  button ->
                    context.mutateRefresh(() -> special.mapDecorations.forEach(entry -> entry.uiCollapsed = true))
            );

            card.child(UiFactory.actionButtonRow(expandAll, collapseAll));
        }

        if (special.mapDecorations.isEmpty()) {
            card.child(UiFactory.muted(ItemEditorText.tr("special.advanced.map.decorations_empty"), MAP_DECORATIONS_EMPTY_HINT_WIDTH));
            return card;
        }

        for (int index = 0; index < special.mapDecorations.size(); index++) {
            int currentIndex = index;
            ItemEditorState.MapDecorationDraft draft = special.mapDecorations.get(index);
            FlowLayout entry = context.createReorderableCard(
                    ItemEditorText.tr("special.advanced.map.decoration", index + 1),
                    currentIndex > 0,
                    () -> context.swapEntries(special.mapDecorations, currentIndex, currentIndex - 1),
                    currentIndex < special.mapDecorations.size() - 1,
                    () -> context.swapEntries(special.mapDecorations, currentIndex, currentIndex + 1),
                    () -> special.mapDecorations.remove(currentIndex)
            );

            FlowLayout summaryRow = responsiveRow();
            UIComponent summary = UiFactory.muted(Component.literal(mapDecorationSummary(draft)), MAP_DECORATION_SUMMARY_HINT_WIDTH);
            summary.horizontalSizing(Sizing.expand(100));
            summaryRow.child(summary);
            ButtonComponent collapseToggle = UiFactory.button(Component.literal(draft.uiCollapsed ? SYMBOL_SECTION_COLLAPSED : SYMBOL_SECTION_EXPANDED), UiFactory.ButtonTextPreset.STANDARD,  button ->
                    context.mutateRefresh(() -> draft.uiCollapsed = !draft.uiCollapsed)
            );
            collapseToggle.horizontalSizing(Sizing.fixed(compactIconButtonWidth()));
            summaryRow.child(collapseToggle);
            entry.child(summaryRow);

            if (!draft.uiCollapsed) {
                entry.child(compactField(
                        ItemEditorText.tr("special.advanced.map.decoration_key"),
                        filledTextBox(context, draft.key, value -> draft.key = value),
                        keyWidth + 40
                ));
                entry.child(compactIdField(
                        context,
                        ItemEditorText.tr("special.advanced.map.decoration_type"),
                        draft.typeId,
                        value -> draft.typeId = value,
                        decorationTypeIds,
                        ItemEditorText.str("special.advanced.map.decoration_type"),
                        idWidth
                ));

                FlowLayout position = responsiveRow();
                FlowLayout xField = compactTextField(
                        context,
                        ItemEditorText.tr("special.advanced.map.decoration_x"),
                        draft.x,
                        value -> draft.x = value,
                        numberWidth
                );
                FlowLayout zField = compactTextField(
                        context,
                        ItemEditorText.tr("special.advanced.map.decoration_z"),
                        draft.z,
                        value -> draft.z = value,
                        numberWidth
                );
                FlowLayout rotationField = compactTextField(
                        context,
                        ItemEditorText.tr("special.advanced.map.decoration_rotation"),
                        draft.rotation,
                        value -> draft.rotation = value,
                        numberWidth
                );
                distributeRowChildren(position, xField, zField, rotationField);
                entry.child(position);
            }
            card.child(entry);
        }

        return card;
    }

    private static FlowLayout buildLodestoneEditor(SpecialDataPanelContext context, ItemEditorState.SpecialData special) {
        FlowLayout card = UiFactory.subCard();
        card.child(UiFactory.title(ItemEditorText.tr("special.advanced.map.lodestone_title")).shadow(false));
        card.child(UiFactory.checkbox(
                ItemEditorText.tr("special.advanced.map.lodestone_enabled"),
                special.lodestoneEnabled,
                value -> context.mutateRefresh(() -> special.lodestoneEnabled = value)
        ));
        if (!special.lodestoneEnabled) {
            return card;
        }

        card.child(UiFactory.checkbox(
                ItemEditorText.tr("special.advanced.map.lodestone_tracked"),
                special.lodestoneTracked,
                context.bindToggle(value -> special.lodestoneTracked = value)
        ));
        card.child(UiFactory.field(
                ItemEditorText.tr("special.advanced.map.lodestone_dimension"),
                Component.empty(),
                textWithPickerCompact(
                        context,
                        special.lodestoneDimensionId,
                        value -> special.lodestoneDimensionId = value,
                        context.optionalRegistryIds(Registries.DIMENSION),
                        ItemEditorText.str("special.advanced.map.lodestone_dimension"),
                        true
                )
        ));

        int numberWidth = compactNumericFieldWidth();
        FlowLayout xField = compactTextField(
                context,
                ItemEditorText.tr("special.advanced.map.lodestone_x"),
                special.lodestoneX,
                value -> special.lodestoneX = value,
                numberWidth
        );
        FlowLayout yField = compactTextField(
                context,
                ItemEditorText.tr("special.advanced.map.lodestone_y"),
                special.lodestoneY,
                value -> special.lodestoneY = value,
                numberWidth
        );
        FlowLayout zField = compactTextField(
                context,
                ItemEditorText.tr("special.advanced.map.lodestone_z"),
                special.lodestoneZ,
                value -> special.lodestoneZ = value,
                numberWidth
        );
        card.child(denseEquipmentRow(xField, yField, zField));
        return card;
    }

    private static FlowLayout buildComponentTweakNamingSection(SpecialDataPanelContext context, ItemEditorState.SpecialData special) {
        return collapsibleCard(
                context,
                ItemEditorText.tr("special.advanced.component_tweaks.naming_title"),
                special.uiComponentTweaksNamingCollapsed,
                value -> special.uiComponentTweaksNamingCollapsed = value,
                () -> {
                    FlowLayout content = UiFactory.column();
                    content.child(buildNamingAndStackCard(context, special));
                    return content;
                }
        );
    }

    private static FlowLayout buildBlockState(SpecialDataPanelContext context, ItemEditorState.SpecialData special) {
        return collapsibleCard(
                context,
                ItemEditorText.tr("special.advanced.block_state.title"),
                special.uiBlockStateCollapsed,
                value -> special.uiBlockStateCollapsed = value,
                () -> {
                    FlowLayout content = UiFactory.column();
                    content.child(buildBlockStateCard(context, special));
                    return content;
                }
        );
    }

    private static FlowLayout buildNamingAndStackCard(SpecialDataPanelContext context, ItemEditorState.SpecialData special) {
        FlowLayout card = UiFactory.subCard();
        int numericWidth = compactNumericFieldWidth();
        int mediumWidth = compactGroupFieldWidth();
        int longWidth = compactLongFieldWidth();

        card.child(compactField(
                ItemEditorText.tr("special.advanced.component_tweaks.item_name"),
                filledTextBox(context, special.itemName, value -> special.itemName = value),
                longWidth + 40
        ));

        FlowLayout minAttackField = compactField(
                ItemEditorText.tr("special.advanced.component_tweaks.min_attack_charge"),
                filledTextBox(context, special.minimumAttackCharge, value -> special.minimumAttackCharge = value),
                numericWidth + 40
        );
        FlowLayout enchantableField = compactField(
                ItemEditorText.tr("special.advanced.component_tweaks.enchantable"),
                filledTextBox(context, special.enchantableValue, value -> special.enchantableValue = value),
                numericWidth + 40
        );
        FlowLayout ominousField = compactField(
                ItemEditorText.tr("special.advanced.component_tweaks.ominous_amplifier"),
                filledTextBox(context, special.ominousBottleAmplifier, value -> special.ominousBottleAmplifier = value),
                numericWidth + 40
        );

        card.child(denseEquipmentRow(minAttackField, enchantableField, ominousField));

        card.child(compactField(
                ItemEditorText.tr("special.advanced.component_tweaks.tooltip_style"),
                filledTextBox(context, special.tooltipStyleId, value -> special.tooltipStyleId = value),
                mediumWidth + 40
        ));

        UIComponent gliderToggle = UiFactory.checkbox(
                ItemEditorText.tr("special.advanced.component_tweaks.glider"),
                special.glider,
                context.bindToggle(value -> special.glider = value)
        );
        UIComponent intangibleToggle = UiFactory.checkbox(
                ItemEditorText.tr("special.advanced.component_tweaks.intangible_projectile"),
                special.intangibleProjectile,
                context.bindToggle(value -> special.intangibleProjectile = value)
        );
        UIComponent deathProtectionToggle = UiFactory.checkbox(
                ItemEditorText.tr("special.advanced.component_tweaks.death_protection"),
                special.deathProtection,
                value -> context.mutateRefresh(() -> setDeathProtectionEnabled(special, value))
        );

        card.child(compactCheckboxRow(gliderToggle, intangibleToggle, deathProtectionToggle));
        if (special.deathProtection) {
            card.child(buildDeathProtectionEffectsEditor(context, special));
        }
        return card;
    }

    private static FlowLayout buildBlockStateCard(SpecialDataPanelContext context, ItemEditorState.SpecialData special) {
        FlowLayout card = UiFactory.subCard();
        List<BlockStatePropertyMeta> availableProperties = blockStatePropertyMeta(context);

        card.child(UiFactory.title(ItemEditorText.tr("special.advanced.component_tweaks.block_state")).shadow(false));

        Map<String, String> currentValues = parseBlockStatePropertyMap(special.blockStateProperties);
        FlowLayout stateActions = UiFactory.row();
        stateActions.horizontalAlignment(HorizontalAlignment.RIGHT);
        stateActions.gap(Math.max(1, UiFactory.scaleProfile().tightSpacing()));
        ButtonComponent clearProperties = UiFactory.button(ItemEditorText.tr("common.reset"), UiFactory.ButtonTextPreset.COMPACT,  button ->
                context.mutateRefresh(() -> special.blockStateProperties = "")
        );
        clearProperties.active(!currentValues.isEmpty());
        clearProperties.horizontalSizing(Sizing.fixed(compactClearButtonWidth()));
        card.child(stateActions);
        stateActions.child(clearProperties);

        for (BlockStatePropertyMeta property : availableProperties) {
            UIComponent entry = blockStatePropertyRow(context, special, currentValues, property);
            entry.horizontalSizing(Sizing.fill(100));
            card.child(entry);
        }
        return card;
    }

    private static FlowLayout blockStatePropertyRow(
            SpecialDataPanelContext context,
            ItemEditorState.SpecialData special,
            Map<String, String> currentValues,
            BlockStatePropertyMeta property
    ) {
        String currentValue = selectedBlockStateValue(currentValues, property);
        boolean hasOverride = currentValues.containsKey(property.key()) && !currentValues.getOrDefault(property.key(), "").isBlank();
        boolean stacked = usesStackedBlockStateRows();
        FlowLayout row = stacked ? UiFactory.column() : UiFactory.row();
        row.gap(Math.max(1, UiFactory.scaleProfile().tightSpacing()));

        int labelWidth = blockStateLabelWidth();
        Component labelText = Component.literal(property.key());
        Component fittedLabel = UiFactory.fitToWidth(labelText, labelWidth);
        var label = UiFactory.muted(fittedLabel, labelWidth);
        if (!Objects.equals(fittedLabel.getString(), labelText.getString())) {
            label.tooltip(List.of(labelText));
        }
        label.horizontalSizing(stacked ? Sizing.fill(100) : Sizing.fixed(labelWidth));
        row.child(label);

        ButtonComponent valueButton = UiFactory.button(
                Component.literal(currentValue),
                UiFactory.ButtonTextPreset.STANDARD,
                anchor -> context.openDropdown(
                        anchor,
                        property.values(),
                        value -> value,
                        value -> context.mutateRefresh(() -> setBlockStateProperty(special, property.key(), value))
                )
        );
        valueButton.active(!property.values().isEmpty());
        valueButton.horizontalSizing(stacked
                ? Sizing.fill(100)
                : Sizing.fill(hasOverride
                        ? BLOCK_STATE_VALUE_WITH_RESET_WIDTH_PERCENT
                        : BLOCK_STATE_VALUE_WIDTH_PERCENT));
        row.child(valueButton);

        if (hasOverride) {
            ButtonComponent resetButton = UiFactory.button(ItemEditorText.tr("common.reset"), UiFactory.ButtonTextPreset.COMPACT, button ->
                    context.mutateRefresh(() -> removeBlockStateProperty(special, property.key()))
            );
            resetButton.horizontalSizing(stacked ? Sizing.fill(100) : Sizing.fixed(compactClearButtonWidth()));
            row.child(resetButton);
        }
        return row;
    }

    private static FlowLayout textWithPickerCompact(
            SpecialDataPanelContext context,
            String value,
            Consumer<String> setter,
            List<String> entries,
            String pickerTitle,
            boolean normalizeInput
    ) {
        FlowLayout row = UiFactory.row();
        row.gap(holderSetRowGap());
        row.child(UiFactory.textBox(value, text -> context.mutate(() ->
                        setter.accept(normalizeInput ? IdFieldNormalizer.normalize(text) : text)))
                .horizontalSizing(Sizing.expand(100)));
        row.child(UiFactory.button(ItemEditorText.tr("common.pick"), UiFactory.ButtonTextPreset.STANDARD, button ->
                context.openSearchablePicker(
                        pickerTitle,
                        "",
                        entries,
                        id -> id,
                        id -> context.mutateRefresh(() -> setter.accept(id))
                )
        ).horizontalSizing(Sizing.fixed(compactFixedPickButtonWidth())));
        return row;
    }

    static FlowLayout damageTypeHolderSetEditor(
            SpecialDataPanelContext context,
            String value,
            Consumer<String> setter,
            Supplier<String> currentValueSupplier,
            String pickerTitle,
            boolean collapsed,
            Consumer<Boolean> collapsedSetter,
            boolean allowTagExpansion,
            Consumer<Boolean> allowTagExpansionSetter
    ) {
        FlowLayout editor = UiFactory.column();
        editor.gap(SECTION_ROW_GAP);

        List<String> entries = splitIdentifierTokens(value);
        FlowLayout summaryRow = UiFactory.row();
        UIComponent summary = UiFactory.muted(holderSetSummary(entries), compactLongFieldWidth() + 120);
        summary.horizontalSizing(Sizing.expand(100));
        summaryRow.child(summary);
        if (collapsedSetter != null) {
            ButtonComponent toggle = UiFactory.button(
                    Component.literal(collapsed ? SYMBOL_SECTION_COLLAPSED : SYMBOL_SECTION_EXPANDED),
                    UiFactory.ButtonTextPreset.STANDARD,
                    button -> context.mutateRefresh(() -> collapsedSetter.accept(!collapsed))
            );
            toggle.horizontalSizing(Sizing.fixed(compactIconButtonWidth()));
            summaryRow.child(toggle);
        }
        editor.child(summaryRow);

        if (hasHolderSetExpansionWarning(context, entries)) {
            var warning = UiFactory.message(
                    ItemEditorText.str("special.advanced.component_tweaks.tag_expansion_warning"),
                    0xFF8A8A
            );
            warning.maxWidth(Math.min(
                    UiFactory.responsiveBodyTextWidth(),
                    compactLongFieldWidth() + 60
            ));
            warning.horizontalSizing(Sizing.fill(100));
            editor.child(warning);
            if (allowTagExpansionSetter != null) {
                editor.child(UiFactory.checkbox(
                        ItemEditorText.tr("special.advanced.component_tweaks.allow_tag_expansion"),
                        allowTagExpansion,
                        context.bindToggle(allowTagExpansionSetter)
                ));
            }
        }

        if (collapsed) {
            return editor;
        }

        boolean compactHolderRows = usesStackedPickerRows();
        FlowLayout pickers = holderSetPickerButtons(
                context,
                setter,
                currentValueSupplier,
                pickerTitle,
                compactHolderRows
        );
        if (compactHolderRows) {
            editor.child(pickers);
        }

        List<String> displayedEntries = entries.isEmpty() ? List.of("") : entries;
        for (int index = 0; index < displayedEntries.size(); index++) {
            int currentIndex = index;
            boolean emptyPlaceholder = entries.isEmpty();
            String entryValue = displayedEntries.get(index);
            FlowLayout row = UiFactory.row();
            row.gap(holderSetRowGap());
            UIComponent kind = UiFactory.muted(holderSetEntryKind(entryValue), holderSetKindWidth());
            kind.horizontalSizing(Sizing.fixed(holderSetKindWidth()));
            row.child(kind);
            row.child(UiFactory.textBox(entryValue, context.bindText(text ->
                    setter.accept(replaceIdentifierListValue(currentValueSupplier.get(), currentIndex, text))
            )).horizontalSizing(Sizing.expand(100)));
            ButtonComponent remove = UiFactory.button(
                    compactHolderRows
                            ? Component.literal("X").withColor(0xFF8A8A)
                            : ItemEditorText.tr("common.remove"),
                    UiFactory.ButtonTextPreset.COMPACT,
                    button -> context.mutateRefresh(() -> setter.accept(removeIdentifierListValue(
                            currentValueSupplier.get(),
                            currentIndex
                    )))
            );
            if (compactHolderRows) {
                remove.tooltip(List.of(ItemEditorText.tr("common.remove")));
            }
            remove.active(!emptyPlaceholder);
            remove.horizontalSizing(Sizing.fixed(holderSetRemoveButtonWidth()));
            row.child(remove);
            editor.child(row);
        }

        if (!compactHolderRows) {
            editor.child(pickers);
        }
        return editor;
    }

    static FlowLayout blockHolderSetEditor(
            SpecialDataPanelContext context,
            String value,
            Consumer<String> setter,
            Supplier<String> currentValueSupplier,
            String pickerTitle,
            boolean allowTagExpansion,
            Consumer<Boolean> allowTagExpansionSetter
    ) {
        FlowLayout editor = UiFactory.column();
        editor.gap(SECTION_ROW_GAP);

        List<String> entries = splitIdentifierTokens(value);
        UIComponent summary = UiFactory.muted(blockHolderSetSummary(entries), compactLongFieldWidth() + 120);
        summary.horizontalSizing(Sizing.fill(100));
        editor.child(summary);

        editor.child(textWithPickerCompact(
                context,
                value,
                setter,
                context.optionalRegistryIds(Registries.BLOCK),
                pickerTitle,
                false
        ));
        editor.child(holderSetPickerButtons(
                context,
                setter,
                currentValueSupplier,
                pickerTitle,
                usesStackedPickerRows(),
                context.optionalRegistryIds(Registries.BLOCK),
                context.registryTagIds(Registries.BLOCK, "")
        ));

        if (hasBlockHolderSetExpansionWarning(context, entries)) {
            var warning = UiFactory.message(
                    ItemEditorText.str("special.advanced.combat.tool_rule_tag_expansion_warning"),
                    0xFF8A8A
            );
            warning.maxWidth(Math.min(
                    UiFactory.responsiveBodyTextWidth(),
                    compactLongFieldWidth() + 60
            ));
            warning.horizontalSizing(Sizing.fill(100));
            editor.child(warning);
            editor.child(UiFactory.checkbox(
                    ItemEditorText.tr("special.advanced.component_tweaks.allow_tag_expansion"),
                    allowTagExpansion,
                    context.bindToggle(allowTagExpansionSetter)
            ));
        }
        return editor;
    }

    private static FlowLayout holderSetPickerButtons(
            SpecialDataPanelContext context,
            Consumer<String> setter,
            Supplier<String> currentValueSupplier,
            String pickerTitle,
            boolean compact
    ) {
        return holderSetPickerButtons(
                context,
                setter,
                currentValueSupplier,
                pickerTitle,
                compact,
                context.optionalRegistryIds(Registries.DAMAGE_TYPE),
                context.registryTagIds(Registries.DAMAGE_TYPE, "")
        );
    }

    private static FlowLayout holderSetPickerButtons(
            SpecialDataPanelContext context,
            Consumer<String> setter,
            Supplier<String> currentValueSupplier,
            String pickerTitle,
            boolean compact,
            List<String> typeIds,
            List<String> tagIds
    ) {
        FlowLayout pickers = UiFactory.row();
        pickers.gap(holderSetRowGap());
        ButtonComponent pickType = UiFactory.button(ItemEditorText.tr("common.add_type").copy().withColor(0x91E68C), UiFactory.ButtonTextPreset.COMPACT, button ->
                context.openSearchablePicker(
                        pickerTitle,
                        "",
                        typeIds,
                        id -> id,
                        id -> context.mutateRefresh(() -> setter.accept(appendIdentifierListValue(currentValueSupplier.get(), id)))
                )
        );
        pickType.horizontalSizing(compact ? Sizing.fill(49) : Sizing.fixed(compactPickerButtonWidth()));
        pickers.child(pickType);

        ButtonComponent pickTag = UiFactory.button(ItemEditorText.tr("common.add_tag").copy().withColor(0x8AC8FF), UiFactory.ButtonTextPreset.COMPACT, button ->
                context.openSearchablePicker(
                        pickerTitle,
                        "",
                        tagIds,
                        id -> id,
                        id -> context.mutateRefresh(() -> setter.accept(appendIdentifierListValue(currentValueSupplier.get(), "#" + id)))
                )
        );
        pickTag.horizontalSizing(compact ? Sizing.fill(49) : Sizing.fixed(compactPickerButtonWidth()));
        pickers.child(pickTag);
        return pickers;
    }

    private static int holderSetKindWidth() {
        return clampToPanelWidth(Math.max(34, UiFactory.scaledPixels(42)));
    }

    private static int holderSetRemoveButtonWidth() {
        if (usesStackedPickerRows()) {
            return clampToPanelWidth(Math.max(34, UiFactory.scaledPixels(40)));
        }
        return compactRemoveButtonWidth();
    }

    private static int holderSetRowGap() {
        return Math.max(1, UiFactory.scaleProfile().tightSpacing() - 2);
    }

    private static Component holderSetSummary(List<String> entries) {
        if (entries.isEmpty()) {
            return ItemEditorText.tr("special.advanced.component_tweaks.damage_types_none");
        }

        int tags = 0;
        for (String entry : entries) {
            if (entry.startsWith("#")) {
                tags++;
            }
        }
        int types = entries.size() - tags;
        return ItemEditorText.tr(
                "special.advanced.component_tweaks.damage_types_summary",
                entries.size(),
                types,
                tags
        );
    }

    private static Component blockHolderSetSummary(List<String> entries) {
        if (entries.isEmpty()) {
            return ItemEditorText.tr("special.advanced.combat.tool_rule_blocks_none");
        }

        int tags = 0;
        for (String entry : entries) {
            if (entry.startsWith("#")) {
                tags++;
            }
        }
        int blocks = entries.size() - tags;
        return ItemEditorText.tr(
                "special.advanced.combat.tool_rule_blocks_summary",
                entries.size(),
                blocks,
                tags
        );
    }

    private static boolean hasHolderSetExpansionWarning(
            SpecialDataPanelContext context,
            List<String> entries
    ) {
        if (entries.size() <= 1) {
            return false;
        }
        List<String> typeIds = context.optionalRegistryIds(Registries.DAMAGE_TYPE);
        List<String> tagIds = context.registryTagIds(Registries.DAMAGE_TYPE, "");
        for (String entry : entries) {
            if (entry.startsWith("#")) {
                return true;
            }
            if (!typeIds.contains(entry) && tagIds.contains(entry)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasBlockHolderSetExpansionWarning(
            SpecialDataPanelContext context,
            List<String> entries
    ) {
        if (entries.size() <= 1) {
            return false;
        }
        List<String> typeIds = context.optionalRegistryIds(Registries.BLOCK);
        List<String> tagIds = context.registryTagIds(Registries.BLOCK, "");
        for (String entry : entries) {
            if (entry.startsWith("#")) {
                return true;
            }
            if (!typeIds.contains(entry) && tagIds.contains(entry)) {
                return true;
            }
        }
        return false;
    }

    private static Component holderSetEntryKind(String value) {
        if (value != null && value.trim().startsWith("#")) {
            return ItemEditorText.tr("common.tag").copy().withColor(0x8AC8FF);
        }
        if (value != null && !value.isBlank()) {
            return ItemEditorText.tr("common.type").copy().withColor(0x91E68C);
        }
        return ItemEditorText.tr("common.entry");
    }

    private static String appendIdentifierListValue(String raw, String selected) {
        List<String> values = splitIdentifierTokens(raw);
        List<String> selectedValues = splitIdentifierTokens(selected);
        if (selectedValues.isEmpty()) {
            return serializeIdentifierTokens(values);
        }
        for (String selectedValue : selectedValues) {
            if (!containsIdentifierToken(values, selectedValue)) {
                values.add(selectedValue);
            }
        }
        return serializeIdentifierTokens(values);
    }

    private static String replaceIdentifierListValue(String raw, int index, String replacement) {
        List<String> values = splitIdentifierTokens(raw);
        List<String> replacementValues = splitIdentifierTokens(replacement);
        if (values.isEmpty()) {
            values.addAll(replacementValues);
            return serializeIdentifierTokens(values);
        }
        if (index < 0 || index >= values.size()) {
            return serializeIdentifierTokens(values);
        }
        values.remove(index);
        values.addAll(index, replacementValues);
        return serializeIdentifierTokens(values);
    }

    private static String removeIdentifierListValue(String raw, int index) {
        List<String> values = splitIdentifierTokens(raw);
        if (index >= 0 && index < values.size()) {
            values.remove(index);
        }
        return serializeIdentifierTokens(values);
    }

    private static boolean containsIdentifierToken(List<String> values, String candidate) {
        for (String value : values) {
            if (value.equalsIgnoreCase(candidate)) {
                return true;
            }
        }
        return false;
    }

    private static List<String> splitIdentifierTokens(String raw) {
        if (raw == null || raw.isBlank()) {
            return new ArrayList<>();
        }
        List<String> values = new ArrayList<>();
        for (String part : raw.split("[,\\r\\n]+")) {
            String normalized = normalizeIdentifierToken(part);
            if (!normalized.isBlank()) {
                values.add(normalized);
            }
        }
        return values;
    }

    private static String normalizeIdentifierToken(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (value.startsWith("#")) {
            String tag = IdFieldNormalizer.normalize(value.substring(1));
            return tag.isBlank() ? "" : "#" + tag;
        }
        return IdFieldNormalizer.normalize(value);
    }

    private static String serializeIdentifierTokens(List<String> values) {
        return String.join(", ", values);
    }

    private static Map<String, String> parseBlockStatePropertyMap(String raw) {
        Map<String, String> valuesByKey = new LinkedHashMap<>();
        if (raw == null || raw.isBlank()) {
            return valuesByKey;
        }

        for (String part : raw.split("[,\\r\\n]+")) {
            String token = part.trim();
            if (token.isEmpty()) {
                continue;
            }
            int separator = token.indexOf('=');
            String key = separator < 0 ? token.trim() : token.substring(0, separator).trim();
            String value = separator < 0 ? "" : token.substring(separator + 1).trim();
            if (!key.isEmpty()) {
                valuesByKey.put(key, value);
            }
        }
        return valuesByKey;
    }

    private static String selectedBlockStateValue(Map<String, String> currentValues, BlockStatePropertyMeta property) {
        String currentValue = currentValues.get(property.key());
        if (currentValue != null && !currentValue.isBlank()) {
            return currentValue;
        }
        if (property.defaultValue() != null && !property.defaultValue().isBlank()) {
            return property.defaultValue();
        }
        return property.values().isEmpty() ? ItemEditorText.str("special.advanced.select") : property.values().getFirst();
    }

    private static void setBlockStateProperty(ItemEditorState.SpecialData special, String key, String value) {
        Map<String, String> entries = parseBlockStatePropertyMap(special.blockStateProperties);
        String normalizedKey = key == null ? "" : key.trim();
        if (normalizedKey.isEmpty()) {
            return;
        }

        String normalizedValue = value == null ? "" : value.trim();
        if (!normalizedValue.isEmpty()) {
            entries.put(normalizedKey, normalizedValue);
        } else {
            entries.remove(normalizedKey);
        }
        special.blockStateProperties = serializeBlockStateProperties(entries);
    }

    private static void removeBlockStateProperty(ItemEditorState.SpecialData special, String key) {
        String normalizedKey = key == null ? "" : key.trim();
        Map<String, String> entries = parseBlockStatePropertyMap(special.blockStateProperties);
        entries.remove(normalizedKey);
        special.blockStateProperties = serializeBlockStateProperties(entries);
    }

    private static List<BlockStatePropertyMeta> blockStatePropertyMeta(SpecialDataPanelContext context) {
        ItemStack stack = context.originalStack();
        if (!(stack.getItem() instanceof BlockItem blockItem)) {
            return List.of();
        }

        List<BlockStatePropertyMeta> metas = new ArrayList<>();
        BlockState defaultState = blockItem.getBlock().defaultBlockState();
        for (Property<?> property : defaultState.getProperties()) {
            metas.add(blockStatePropertyMeta(defaultState, property));
        }
        metas.sort(Comparator.comparing(BlockStatePropertyMeta::key));
        return metas;
    }

    private static <T extends Comparable<T>> BlockStatePropertyMeta blockStatePropertyMeta(BlockState defaultState, Property<T> property) {
        return new BlockStatePropertyMeta(
                property.getName(),
                blockStatePropertyValues(property),
                property.getName(defaultState.getValue(property))
        );
    }

    private static <T extends Comparable<T>> List<String> blockStatePropertyValues(Property<T> property) {
        List<String> values = new ArrayList<>();
        for (T value : property.getPossibleValues()) {
            values.add(property.getName(value));
        }
        values.sort(Comparator.naturalOrder());
        return values;
    }

    private static String serializeBlockStateProperties(Map<String, String> entries) {
        List<String> tokens = new ArrayList<>();
        for (Map.Entry<String, String> entry : entries.entrySet()) {
            String key = entry.getKey() == null ? "" : entry.getKey().trim();
            String value = entry.getValue() == null ? "" : entry.getValue().trim();
            if (key.isEmpty() && value.isEmpty()) {
                continue;
            }
            tokens.add(key + "=" + value);
        }
        return String.join(", ", tokens);
    }

    private record BlockStatePropertyMeta(String key, List<String> values, String defaultValue) {
    }

    static List<String> jukeboxSongIds(SpecialDataPanelContext context, String currentId) {
        return withCurrentId(context.optionalRegistryIds(Registries.JUKEBOX_SONG), currentId);
    }

    static List<String> withCurrentId(List<String> values, String currentId) {
        List<String> ids = new ArrayList<>(values);
        String normalizedCurrent = IdFieldNormalizer.normalize(currentId);
        if (!normalizedCurrent.isBlank() && !ids.contains(normalizedCurrent)) {
            ids.add(normalizedCurrent);
        }
        ids.sort(String::compareTo);
        return ids;
    }

}
