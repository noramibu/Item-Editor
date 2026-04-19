package me.noramibu.itemeditor.ui.panel.specialdata;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Sizing;
import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.editor.text.RichTextDocument;
import me.noramibu.itemeditor.ui.component.OrbitingArmorStandComponent;
import me.noramibu.itemeditor.ui.component.StyledTextFieldSection;
import me.noramibu.itemeditor.ui.component.UiFactory;
import me.noramibu.itemeditor.util.ItemEditorCapabilities;
import me.noramibu.itemeditor.util.ItemEditorText;
import me.noramibu.itemeditor.util.TextComponentUtil;
import me.noramibu.itemeditor.util.ValidationUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class ArmorStandSpecialDataSection {

    private static final int AXIS_STEP_DEGREES = 5;
    private static final int DISABLE_TAKING_OFFSET = 8;
    private static final int DISABLE_PUTTING_OFFSET = 16;
    private static final int FLAG_ROW_GAP = 8;
    private static final int NAME_EDITOR_HEIGHT = 54;
    private static final int FIELD_WIDTH = 54;
    private static final int AXIS_LABEL_WIDTH = 12;
    private static final int POSE_PART_LABEL_WIDTH = 92;
    private static final int DISABLED_SLOT_LABEL_WIDTH = 80;
    private static final int DISABLED_SLOT_HEADER_WIDTH = 84;
    private static final int DISABLED_SLOT_COMPACT_LABEL_WIDTH = 220;
    private static final int DISABLED_SLOT_CHECK_FILL_SHARE = 32;
    private static final int DISABLED_SLOT_HINT_WIDTH = 320;
    private static final int SCALE_FIELD_WIDTH = 120;
    private static final int DISABLED_ACTION_BUTTON_WIDTH_MIN = 74;
    private static final int DISABLED_ACTION_BUTTON_WIDTH_MAX = 180;
    private static final int DISABLED_ACTION_ROW_RESERVE = 12;
    private static final int DISABLED_ACTION_LABEL_PADDING_BASE = 22;
    private static final int PRESET_BUTTON_WIDTH_MIN = 82;
    private static final int PRESET_BUTTON_WIDTH_MAX = 170;
    private static final int PRESET_BUTTONS_PER_ROW_MAX = 6;
    private static final int PRESET_LABEL_PADDING_BASE = 22;
    private static final int PRESET_BUTTON_ROW_RESERVE = 12;
    private static final int POSE_RESET_BUTTON_WIDTH_MIN = 72;
    private static final int POSE_RESET_BUTTON_WIDTH_MAX = 132;
    private static final int POSE_RESET_BUTTON_ROW_RESERVE = 18;
    private static final int AXIS_STEP_BUTTON_WIDTH_MIN = 28;
    private static final int AXIS_STEP_BUTTON_WIDTH_MAX = 52;
    private static final int AXIS_STEP_BUTTON_WIDTH_BASE = 34;
    private static final int PRESET_NONE = -1;
    private static final String AXIS_DECREASE_LABEL = "-";
    private static final String AXIS_INCREASE_LABEL = "+";
    private static final float MIN_SCALE = 0.01F;
    private static final float MAX_SCALE = 10.0F;
    private static final double COMPACT_LAYOUT_SCALE_THRESHOLD = 3.0d;
    private static final int COMPACT_LAYOUT_WIDTH_THRESHOLD = 640;
    private static final List<EquipmentSlot> DISABLED_SLOT_ORDER = List.of(
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET,
            EquipmentSlot.MAINHAND,
            EquipmentSlot.OFFHAND
    );

    private static final PosePreset DEFAULT_PRESET = new PosePreset(
            "special.armor_stand.preset.default",
            false,
            false,
            false,
            rotation(0, 0),
            rotation(0, 0),
            rotation(-10, -10),
            rotation(-15, 10),
            rotation(-1, -1),
            rotation(1, 1)
    );

    private static final List<PosePreset> POSE_PRESETS = List.of(
            DEFAULT_PRESET,
            new PosePreset(
                    "special.armor_stand.preset.salute",
                    true,
                    false,
                    false,
                    rotation(0, 0),
                    rotation(0, 0),
                    rotation(-10, -10),
                    rotation(-92, 0),
                    rotation(-1, -1),
                    rotation(1, 1)
            ),
            new PosePreset(
                    "special.armor_stand.preset.t_pose",
                    true,
                    false,
                    false,
                    rotation(0, 0),
                    rotation(0, 0),
                    rotation(0, -90),
                    rotation(0, 90),
                    rotation(-1, -1),
                    rotation(1, 1)
            ),
            new PosePreset(
                    "special.armor_stand.preset.dab",
                    true,
                    false,
                    false,
                    rotation(18, 0),
                    rotation(0, 0),
                    rotation(72, -52),
                    rotation(-64, 42),
                    rotation(-1, -1),
                    rotation(1, 1)
            ),
            new PosePreset(
                    "special.armor_stand.preset.kneel",
                    true,
                    false,
                    false,
                    rotation(10, 0),
                    rotation(14, 0),
                    rotation(-30, -10),
                    rotation(-35, 10),
                    rotation(32, -4),
                    rotation(-44, 8)
            ),
            new PosePreset(
                    "special.armor_stand.preset.arms_up",
                    true,
                    false,
                    false,
                    rotation(0, 0),
                    rotation(0, 0),
                    rotation(-170, 8),
                    rotation(-170, -8),
                    rotation(-1, -1),
                    rotation(1, 1)
            ),
            new PosePreset(
                    "special.armor_stand.preset.crossed_arms",
                    true,
                    false,
                    false,
                    rotation(2, 0),
                    rotation(0, 0),
                    rotation(-30, 48),
                    rotation(-30, -48),
                    rotation(-1, -1),
                    rotation(1, 1)
            ),
            new PosePreset(
                    "special.armor_stand.preset.lean_back",
                    true,
                    false,
                    false,
                    rotation(-12, 0),
                    rotation(-18, 0),
                    rotation(-18, -14),
                    rotation(-22, 14),
                    rotation(8, -2),
                    rotation(8, 2)
            ),
            new PosePreset(
                    "special.armor_stand.preset.run",
                    true,
                    false,
                    false,
                    rotation(12, 0),
                    rotation(8, 0),
                    rotation(52, -18),
                    rotation(-62, 16),
                    rotation(56, 0),
                    rotation(-52, 0)
            ),
            new PosePreset(
                    "special.armor_stand.preset.wave",
                    true,
                    false,
                    false,
                    rotation(4, 0),
                    rotation(0, 0),
                    rotation(-8, -8),
                    rotation(-118, 26),
                    rotation(-1, -1),
                    rotation(1, 1)
            ),
            new PosePreset(
                    "special.armor_stand.preset.point_forward",
                    true,
                    false,
                    false,
                    rotation(0, 0),
                    rotation(0, 0),
                    rotation(-8, -10),
                    rotation(-74, 0),
                    rotation(-1, -1),
                    rotation(1, 1)
            ),
            new PosePreset(
                    "special.armor_stand.preset.bow",
                    true,
                    false,
                    false,
                    rotation(16, 0),
                    rotation(12, 0),
                    rotation(-20, -32),
                    rotation(-36, 24),
                    rotation(10, -2),
                    rotation(8, 2)
            ),
            new PosePreset(
                    "special.armor_stand.preset.zombie",
                    true,
                    false,
                    false,
                    rotation(0, 0),
                    rotation(0, 0),
                    rotation(-132, -8),
                    rotation(-132, 8),
                    rotation(-2, -2),
                    rotation(2, 2)
            ),
            new PosePreset(
                    "special.armor_stand.preset.seated",
                    true,
                    false,
                    false,
                    rotation(0, 0),
                    rotation(0, 0),
                    rotation(-24, -8),
                    rotation(-24, 8),
                    rotation(90, 0),
                    rotation(90, 0)
            ),
            new PosePreset(
                    "special.armor_stand.preset.surrender",
                    true,
                    false,
                    false,
                    rotation(0, 0),
                    rotation(0, 0),
                    rotation(-178, 18),
                    rotation(-178, -18),
                    rotation(0, 0),
                    rotation(0, 0)
            ),
            new PosePreset(
                    "special.armor_stand.preset.fighter",
                    true,
                    false,
                    false,
                    rotation(8, 0),
                    rotation(6, 0),
                    rotation(-42, -24),
                    rotation(-22, 18),
                    rotation(14, -4),
                    rotation(-18, 6)
            ),
            new PosePreset(
                    "special.armor_stand.preset.sneak",
                    true,
                    false,
                    false,
                    rotation(20, 0),
                    rotation(18, 0),
                    rotation(-34, -10),
                    rotation(-28, 10),
                    rotation(30, -3),
                    rotation(24, 4)
            ),
            new PosePreset(
                    "special.armor_stand.preset.pedestal",
                    false,
                    true,
                    false,
                    rotation(0, 0),
                    rotation(0, 0),
                    rotation(-10, -10),
                    rotation(-15, 10),
                    rotation(-1, -1),
                    rotation(1, 1)
            ),
            new PosePreset(
                    "special.armor_stand.preset.marker_display",
                    false,
                    true,
                    true,
                    rotation(0, 0),
                    rotation(0, 0),
                    rotation(-10, -10),
                    rotation(-15, 10),
                    rotation(-1, -1),
                    rotation(1, 1)
            ),
            new PosePreset(
                    "special.armor_stand.preset.yoga",
                    true,
                    false,
                    false,
                    rotation(4, 0),
                    rotation(0, 0),
                    rotation(-18, -44),
                    rotation(-18, 44),
                    rotation(88, -22),
                    rotation(88, 22)
            )
    );

    private ArmorStandSpecialDataSection() {
    }

    public static boolean supports(ItemStack stack) {
        return ItemEditorCapabilities.supportsArmorStandData(stack);
    }

    public static FlowLayout build(SpecialDataPanelContext context) {
        ItemEditorState.SpecialData special = context.special();
        FlowLayout section = UiFactory.section(ItemEditorText.tr("special.armor_stand.title"), Component.empty());
        section.child(buildPreviewCard(special));
        section.child(buildNameCard(context, special));
        section.child(buildFlagsCard(context, special));
        section.child(buildDisabledSlotsCard(context, special));
        section.child(buildNumericCard(context, special));
        section.child(buildPoseCard(context, special));
        return section;
    }

    private static FlowLayout buildPreviewCard(ItemEditorState.SpecialData special) {
        FlowLayout card = UiFactory.subCard();
        card.child(UiFactory.title(ItemEditorText.tr("special.armor_stand.preview")).shadow(false));

        FlowLayout row = isNarrowLayout() ? UiFactory.column() : UiFactory.row();
        row.gap(FLAG_ROW_GAP);
        int previewSize = previewSize();

        OrbitingArmorStandComponent preview = new OrbitingArmorStandComponent(
                UiFactory.fixed(previewSize),
                previewTag(special)
        );
        preview.horizontalSizing(UiFactory.fixed(previewSize));
        preview.verticalSizing(UiFactory.fixed(previewSize));
        preview.scaleToFit(true);
        preview.allowMouseRotation(true);
        preview.lookAtCursor(false);
        preview.showNametag(special.armorStandCustomNameVisible && !special.armorStandCustomName.isBlank());
        row.child(preview);

        FlowLayout meta = UiFactory.column();
        meta.child(UiFactory.muted(ItemEditorText.tr("special.armor_stand.preview.drag_hint"), previewHintWidth()));
        if (!special.armorStandScale.isBlank()) {
            meta.child(UiFactory.muted(ItemEditorText.tr("special.armor_stand.preview.scale", special.armorStandScale), previewHintWidth()));
        }
        if (special.armorStandInvisible) {
            meta.child(UiFactory.muted(ItemEditorText.tr("special.armor_stand.preview.invisible"), previewHintWidth()));
        }
        if (special.armorStandMarker) {
            meta.child(UiFactory.muted(ItemEditorText.tr("special.armor_stand.preview.marker"), previewHintWidth()));
        }
        row.child(meta);

        card.child(row);
        if (special.armorStandCustomNameVisible && !special.armorStandCustomName.isBlank()) {
            card.child(UiFactory.message(displayPreviewName(special.armorStandCustomName), 0xE6EEF8));
        }
        return card;
    }

    private static FlowLayout buildNameCard(SpecialDataPanelContext context, ItemEditorState.SpecialData special) {
        FlowLayout card = UiFactory.subCard();
        card.child(UiFactory.title(ItemEditorText.tr("special.armor_stand.name")).shadow(false));

        StyledTextFieldSection.BoundEditor nameSection = StyledTextFieldSection.create(
                context.screen(),
                RichTextDocument.fromMarkup(special.armorStandCustomName),
                Sizing.fill(100),
                UiFactory.fixed(NAME_EDITOR_HEIGHT),
                ItemEditorText.str("special.armor_stand.name.placeholder"),
                StyledTextFieldSection.StylePreset.name(),
                ItemEditorText.str("special.armor_stand.name.color_title"),
                ItemEditorText.str("special.armor_stand.name.gradient_title"),
                "",
                "",
                null,
                document -> document.logicalLineCount() > 1
                        ? ItemEditorText.str("special.armor_stand.name.single_line")
                        : null,
                document -> context.mutate(() -> {
                    special.armorStandCustomName = document.toMarkup();
                    special.armorStandSelectedPreset = PRESET_NONE;
                })
        );

        FlowLayout frame = UiFactory.framedEditorCard();
        frame.child(nameSection.toolbar());
        frame.child(nameSection.editor());
        frame.child(nameSection.validation());
        card.child(frame);
        return card;
    }

    private static FlowLayout buildFlagsCard(SpecialDataPanelContext context, ItemEditorState.SpecialData special) {
        boolean compactLayout = isCompactLayout(context);
        FlowLayout card = UiFactory.subCard();
        card.child(UiFactory.title(ItemEditorText.tr("special.armor_stand.flags")).shadow(false));

        FlowLayout rowA = compactLayout ? UiFactory.column() : UiFactory.row();
        rowA.child(UiFactory.checkbox(
                ItemEditorText.tr("special.armor_stand.small"),
                special.armorStandSmall,
                value -> context.mutateRefresh(() -> {
                    special.armorStandSmall = value;
                    special.armorStandSelectedPreset = PRESET_NONE;
                })
        ));
        rowA.child(UiFactory.checkbox(
                ItemEditorText.tr("special.armor_stand.show_arms"),
                special.armorStandShowArms,
                value -> context.mutateRefresh(() -> {
                    special.armorStandShowArms = value;
                    special.armorStandSelectedPreset = PRESET_NONE;
                })
        ));
        rowA.child(UiFactory.checkbox(
                ItemEditorText.tr("special.armor_stand.no_base_plate"),
                special.armorStandNoBasePlate,
                value -> context.mutateRefresh(() -> {
                    special.armorStandNoBasePlate = value;
                    special.armorStandSelectedPreset = PRESET_NONE;
                })
        ));
        card.child(rowA);

        FlowLayout rowB = compactLayout ? UiFactory.column() : UiFactory.row();
        rowB.child(UiFactory.checkbox(
                ItemEditorText.tr("special.armor_stand.invisible"),
                special.armorStandInvisible,
                value -> context.mutateRefresh(() -> {
                    special.armorStandInvisible = value;
                    special.armorStandSelectedPreset = PRESET_NONE;
                })
        ));
        rowB.child(UiFactory.checkbox(
                ItemEditorText.tr("special.armor_stand.no_gravity"),
                special.armorStandNoGravity,
                value -> context.mutateRefresh(() -> {
                    special.armorStandNoGravity = value;
                    special.armorStandSelectedPreset = PRESET_NONE;
                })
        ));
        rowB.child(UiFactory.checkbox(
                ItemEditorText.tr("special.armor_stand.invulnerable"),
                special.armorStandInvulnerable,
                value -> context.mutateRefresh(() -> {
                    special.armorStandInvulnerable = value;
                    special.armorStandSelectedPreset = PRESET_NONE;
                })
        ));
        card.child(rowB);

        FlowLayout rowC = compactLayout ? UiFactory.column() : UiFactory.row();
        rowC.child(UiFactory.checkbox(
                ItemEditorText.tr("special.armor_stand.name_visible"),
                special.armorStandCustomNameVisible,
                value -> context.mutateRefresh(() -> {
                    special.armorStandCustomNameVisible = value;
                    special.armorStandSelectedPreset = PRESET_NONE;
                })
        ));
        rowC.child(UiFactory.checkbox(
                ItemEditorText.tr("special.armor_stand.marker"),
                special.armorStandMarker,
                value -> context.mutateRefresh(() -> {
                    special.armorStandMarker = value;
                    special.armorStandSelectedPreset = PRESET_NONE;
                })
        ));
        card.child(rowC);
        return card;
    }

    private static FlowLayout buildNumericCard(SpecialDataPanelContext context, ItemEditorState.SpecialData special) {
        boolean compactLayout = isCompactLayout(context);
        FlowLayout card = UiFactory.subCard();
        card.child(UiFactory.title(ItemEditorText.tr("special.armor_stand.scale")).shadow(false));

        FlowLayout row = compactLayout ? UiFactory.column() : UiFactory.row();
        row.child(UiFactory.field(
                ItemEditorText.tr("special.armor_stand.scale"),
                Component.empty(),
                UiFactory.textBox(
                        special.armorStandScale,
                        value -> context.mutateRefresh(() -> {
                            special.armorStandScale = value;
                            special.armorStandSelectedPreset = PRESET_NONE;
                        })
                ).horizontalSizing(compactLayout ? Sizing.fill(100) : UiFactory.fixed(SCALE_FIELD_WIDTH))
        ));
        card.child(row);

        return card;
    }

    private static FlowLayout buildDisabledSlotsCard(SpecialDataPanelContext context, ItemEditorState.SpecialData special) {
        boolean compactLayout = isCompactLayout(context);
        FlowLayout card = UiFactory.subCard();
        card.child(UiFactory.title(ItemEditorText.tr("special.armor_stand.disabled_slots")).shadow(false));

        int mask = parseDisabledSlotsMask(special.armorStandDisabledSlots);
        card.child(UiFactory.muted(ItemEditorText.tr("special.armor_stand.disabled_slots.hint"), DISABLED_SLOT_HINT_WIDTH));
        card.child(UiFactory.muted(ItemEditorText.tr("special.armor_stand.disabled_slots.value", mask), DISABLED_SLOT_HINT_WIDTH));

        if (!compactLayout) {
            FlowLayout header = UiFactory.row();
        header.child(UiFactory.muted(ItemEditorText.tr("special.armor_stand.disabled_slots.slot"), DISABLED_SLOT_LABEL_WIDTH)
                .horizontalSizing(UiFactory.fixed(DISABLED_SLOT_LABEL_WIDTH)));
        header.child(UiFactory.muted(ItemEditorText.tr("special.armor_stand.disabled_slots.lock"), DISABLED_SLOT_HEADER_WIDTH)
                .horizontalSizing(UiFactory.fixed(DISABLED_SLOT_HEADER_WIDTH)));
        header.child(UiFactory.muted(ItemEditorText.tr("special.armor_stand.disabled_slots.take"), DISABLED_SLOT_HEADER_WIDTH)
                .horizontalSizing(UiFactory.fixed(DISABLED_SLOT_HEADER_WIDTH)));
        header.child(UiFactory.muted(ItemEditorText.tr("special.armor_stand.disabled_slots.put"), DISABLED_SLOT_HEADER_WIDTH)
                .horizontalSizing(UiFactory.fixed(DISABLED_SLOT_HEADER_WIDTH)));
            card.child(header);
        }

        for (EquipmentSlot slot : DISABLED_SLOT_ORDER) {
            card.child(buildDisabledSlotRow(context, special, mask, slot));
        }

        int allMask = allDisabledSlotsMask();
        Component lockAllLabel = ItemEditorText.tr("special.armor_stand.disabled_slots.lock_all");
        Component unlockAllLabel = ItemEditorText.tr("special.armor_stand.disabled_slots.unlock_all");
        Component reverseLabel = ItemEditorText.tr("special.armor_stand.disabled_slots.reverse");

        ButtonComponent lockAllButton = UiFactory.button(lockAllLabel, UiFactory.ButtonTextPreset.STANDARD, button ->
                context.mutateRefresh(() -> special.armorStandDisabledSlots = Integer.toString(allMask))
        );
        ButtonComponent unlockAllButton = UiFactory.button(unlockAllLabel, UiFactory.ButtonTextPreset.STANDARD, button ->
                context.mutateRefresh(() -> special.armorStandDisabledSlots = "")
        );
        ButtonComponent reverseButton = UiFactory.button(reverseLabel, UiFactory.ButtonTextPreset.STANDARD, button ->
                context.mutateRefresh(() -> {
                    int inverted = allMask & ~parseDisabledSlotsMask(special.armorStandDisabledSlots);
                    special.armorStandDisabledSlots = inverted == 0 ? "" : Integer.toString(inverted);
                })
        );

        if (compactLayout) {
            FlowLayout actions = UiFactory.column();
            actions.child(lockAllButton.horizontalSizing(Sizing.fill(100)));
            actions.child(unlockAllButton.horizontalSizing(Sizing.fill(100)));
            actions.child(reverseButton.horizontalSizing(Sizing.fill(100)));
            card.child(actions);
            return card;
        }

        int lockWidth = adaptiveTextButtonWidth(lockAllLabel, DISABLED_ACTION_BUTTON_WIDTH_MIN, DISABLED_ACTION_BUTTON_WIDTH_MAX, DISABLED_ACTION_LABEL_PADDING_BASE);
        int unlockWidth = adaptiveTextButtonWidth(unlockAllLabel, DISABLED_ACTION_BUTTON_WIDTH_MIN, DISABLED_ACTION_BUTTON_WIDTH_MAX, DISABLED_ACTION_LABEL_PADDING_BASE);
        int reverseWidth = adaptiveTextButtonWidth(reverseLabel, DISABLED_ACTION_BUTTON_WIDTH_MIN, DISABLED_ACTION_BUTTON_WIDTH_MAX, DISABLED_ACTION_LABEL_PADDING_BASE);
        int spacing = UiFactory.scaleProfile().spacing();
        int availableWidth = Math.max(1, context.panelWidthHint() - UiFactory.scaledPixels(DISABLED_ACTION_ROW_RESERVE));
        int requiredRowWidth = lockWidth + unlockWidth + reverseWidth + (spacing * 2);

        if (requiredRowWidth <= availableWidth) {
            FlowLayout actions = UiFactory.row();
            actions.child(lockAllButton.horizontalSizing(Sizing.fixed(lockWidth)));
            actions.child(unlockAllButton.horizontalSizing(Sizing.fixed(unlockWidth)));
            actions.child(reverseButton.horizontalSizing(Sizing.fixed(reverseWidth)));
            card.child(actions);
            return card;
        }

        FlowLayout stackedActions = UiFactory.column();
        stackedActions.child(lockAllButton.horizontalSizing(Sizing.fill(100)));
        stackedActions.child(unlockAllButton.horizontalSizing(Sizing.fill(100)));
        stackedActions.child(reverseButton.horizontalSizing(Sizing.fill(100)));
        card.child(stackedActions);
        return card;
    }

    private static FlowLayout buildDisabledSlotRow(
            SpecialDataPanelContext context,
            ItemEditorState.SpecialData special,
            int mask,
            EquipmentSlot slot
    ) {
        boolean compactLayout = isCompactLayout(context);
        FlowLayout row = compactLayout ? UiFactory.column() : UiFactory.row();
        if (compactLayout) {
            row.child(UiFactory.muted(ItemEditorText.tr(slotLabelKey(slot)), DISABLED_SLOT_COMPACT_LABEL_WIDTH));
            FlowLayout checks = UiFactory.row();
            checks.child(UiFactory.checkbox(
                    ItemEditorText.tr("special.armor_stand.disabled_slots.lock"),
                    isDisabledSlotFlagSet(mask, slot, 0),
                    value -> toggleDisabledSlotFlag(context, special, slot, 0, value)
            ).horizontalSizing(Sizing.fill(DISABLED_SLOT_CHECK_FILL_SHARE)));
            checks.child(UiFactory.checkbox(
                    ItemEditorText.tr("special.armor_stand.disabled_slots.take"),
                    isDisabledSlotFlagSet(mask, slot, DISABLE_TAKING_OFFSET),
                    value -> toggleDisabledSlotFlag(context, special, slot, DISABLE_TAKING_OFFSET, value)
            ).horizontalSizing(Sizing.fill(DISABLED_SLOT_CHECK_FILL_SHARE)));
            checks.child(UiFactory.checkbox(
                    ItemEditorText.tr("special.armor_stand.disabled_slots.put"),
                    isDisabledSlotFlagSet(mask, slot, DISABLE_PUTTING_OFFSET),
                    value -> toggleDisabledSlotFlag(context, special, slot, DISABLE_PUTTING_OFFSET, value)
            ).horizontalSizing(Sizing.fill(DISABLED_SLOT_CHECK_FILL_SHARE)));
            row.child(checks);
            return row;
        }

        row.child(UiFactory.muted(ItemEditorText.tr(slotLabelKey(slot)), DISABLED_SLOT_LABEL_WIDTH).horizontalSizing(UiFactory.fixed(DISABLED_SLOT_LABEL_WIDTH)));
        row.child(UiFactory.checkbox(
                Component.empty(),
                isDisabledSlotFlagSet(mask, slot, 0),
                value -> toggleDisabledSlotFlag(context, special, slot, 0, value)
        ).horizontalSizing(UiFactory.fixed(DISABLED_SLOT_HEADER_WIDTH)));
        row.child(UiFactory.checkbox(
                Component.empty(),
                isDisabledSlotFlagSet(mask, slot, DISABLE_TAKING_OFFSET),
                value -> toggleDisabledSlotFlag(context, special, slot, DISABLE_TAKING_OFFSET, value)
        ).horizontalSizing(UiFactory.fixed(DISABLED_SLOT_HEADER_WIDTH)));
        row.child(UiFactory.checkbox(
                Component.empty(),
                isDisabledSlotFlagSet(mask, slot, DISABLE_PUTTING_OFFSET),
                value -> toggleDisabledSlotFlag(context, special, slot, DISABLE_PUTTING_OFFSET, value)
        ).horizontalSizing(UiFactory.fixed(DISABLED_SLOT_HEADER_WIDTH)));
        return row;
    }

    private static void toggleDisabledSlotFlag(
            SpecialDataPanelContext context,
            ItemEditorState.SpecialData special,
            EquipmentSlot slot,
            int offset,
            boolean enabled
    ) {
        context.mutateRefresh(() -> {
            int currentMask = parseDisabledSlotsMask(special.armorStandDisabledSlots);
            int bit = 1 << slot.getFilterBit(offset);
            int updatedMask = enabled ? currentMask | bit : currentMask & ~bit;
            special.armorStandDisabledSlots = updatedMask == 0 ? "" : Integer.toString(updatedMask);
        });
    }

    private static boolean isDisabledSlotFlagSet(int mask, EquipmentSlot slot, int offset) {
        int bit = 1 << slot.getFilterBit(offset);
        return (mask & bit) != 0;
    }

    private static int parseDisabledSlotsMask(String raw) {
        Integer parsed = parseNonNegativeIntOrNull(raw);
        return parsed == null ? 0 : parsed;
    }

    private static Integer parseNonNegativeIntOrNull(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Math.max(0, Integer.parseInt(raw.trim()));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static int allDisabledSlotsMask() {
        int all = 0;
        for (EquipmentSlot slot : DISABLED_SLOT_ORDER) {
            all |= 1 << slot.getFilterBit(0);
            all |= 1 << slot.getFilterBit(DISABLE_TAKING_OFFSET);
            all |= 1 << slot.getFilterBit(DISABLE_PUTTING_OFFSET);
        }
        return all;
    }

    private static Component parsePreviewName(String markup) {
        try {
            return TextComponentUtil.parseMarkup(markup);
        } catch (RuntimeException ignored) {
            return Component.literal(markup);
        }
    }

    private static Component displayPreviewName(String markup) {
        Component parsed = parsePreviewName(markup);
        if (parsed.getString().isBlank() && !markup.isBlank()) {
            return Component.literal(markup);
        }
        return parsed;
    }

    private static String slotLabelKey(EquipmentSlot slot) {
        return switch (slot) {
            case OFFHAND -> "special.armor_stand.slot.offhand";
            case FEET -> "special.armor_stand.slot.feet";
            case LEGS -> "special.armor_stand.slot.legs";
            case CHEST, BODY -> "special.armor_stand.slot.chest";
            case HEAD -> "special.armor_stand.slot.head";
            default -> "special.armor_stand.slot.mainhand";
        };
    }

    private static FlowLayout buildPoseCard(SpecialDataPanelContext context, ItemEditorState.SpecialData special) {
        FlowLayout card = UiFactory.subCard();
        card.child(UiFactory.title(ItemEditorText.tr("special.armor_stand.pose")).shadow(false));
        card.child(buildPresetRow(context, special));
        card.child(buildPoseRow(context, ItemEditorText.tr("special.armor_stand.part.head"), special.armorStandPose.head, DEFAULT_PRESET.head));
        card.child(buildPoseRow(context, ItemEditorText.tr("special.armor_stand.part.body"), special.armorStandPose.body, DEFAULT_PRESET.body));
        card.child(buildPoseRow(context, ItemEditorText.tr("special.armor_stand.part.left_arm"), special.armorStandPose.leftArm, DEFAULT_PRESET.leftArm));
        card.child(buildPoseRow(context, ItemEditorText.tr("special.armor_stand.part.right_arm"), special.armorStandPose.rightArm, DEFAULT_PRESET.rightArm));
        card.child(buildPoseRow(context, ItemEditorText.tr("special.armor_stand.part.left_leg"), special.armorStandPose.leftLeg, DEFAULT_PRESET.leftLeg));
        card.child(buildPoseRow(context, ItemEditorText.tr("special.armor_stand.part.right_leg"), special.armorStandPose.rightLeg, DEFAULT_PRESET.rightLeg));
        return card;
    }

    private static FlowLayout buildPresetRow(SpecialDataPanelContext context, ItemEditorState.SpecialData special) {
        FlowLayout presetCard = UiFactory.subCard();
        presetCard.child(UiFactory.title(ItemEditorText.tr("special.armor_stand.presets")).shadow(false));
        boolean compactLayout = isCompactLayout(context);
        int availableWidth = Math.max(1, context.panelWidthHint() - UiFactory.scaledPixels(PRESET_BUTTON_ROW_RESERVE));
        int spacing = UiFactory.scaleProfile().spacing();
        int widestLabelWidth = 0;
        for (PosePreset posePreset : POSE_PRESETS) {
            widestLabelWidth = Math.max(widestLabelWidth, textPixelWidth(ItemEditorText.tr(posePreset.key())));
        }
        int desiredButtonWidth = clampWidth(
                widestLabelWidth + UiFactory.scaledPixels(PRESET_LABEL_PADDING_BASE),
                Math.min(PRESET_BUTTON_WIDTH_MIN, availableWidth),
                Math.min(PRESET_BUTTON_WIDTH_MAX, availableWidth)
        );
        int buttonsPerRow = compactLayout
                ? 1
                : Math.max(1, Math.min(PRESET_BUTTONS_PER_ROW_MAX, (availableWidth + spacing) / Math.max(1, desiredButtonWidth + spacing)));
        int rowButtonWidth = buttonsPerRow <= 1
                ? availableWidth
                : clampWidth(
                (availableWidth - (spacing * (buttonsPerRow - 1))) / buttonsPerRow,
                Math.min(PRESET_BUTTON_WIDTH_MIN, availableWidth),
                Math.min(PRESET_BUTTON_WIDTH_MAX, availableWidth)
        );
        FlowLayout rows = UiFactory.column();
        FlowLayout currentRow = compactLayout ? null : UiFactory.row();
        int inRow = 0;
        for (int presetIndex = 0; presetIndex < POSE_PRESETS.size(); presetIndex++) {
            final int selectedIndex = presetIndex;
            PosePreset preset = POSE_PRESETS.get(presetIndex);
            ButtonComponent button = UiFactory.button(ItemEditorText.tr(preset.key), UiFactory.ButtonTextPreset.STANDARD,  ignored ->
                    context.mutateRefresh(() -> {
                        applyPreset(special, preset);
                        special.armorStandSelectedPreset = selectedIndex;
                    })
            );
            button.tooltip(presetTooltip(preset));
            button.active(special.armorStandSelectedPreset != selectedIndex);
            if (compactLayout) {
                rows.child(button.horizontalSizing(Sizing.fill(100)));
                continue;
            }
            button.horizontalSizing(Sizing.fixed(rowButtonWidth));
            currentRow.child(button);
            inRow++;
            if (inRow >= buttonsPerRow) {
                rows.child(currentRow);
                currentRow = UiFactory.row();
                inRow = 0;
            }
        }
        if (!compactLayout && currentRow != null && inRow > 0) {
            rows.child(currentRow);
        }
        presetCard.child(rows);
        return presetCard;
    }

    private static List<Component> presetTooltip(PosePreset preset) {
        List<Component> lines = new ArrayList<>();
        lines.add(ItemEditorText.tr(preset.key));
        lines.add(rotationTooltipLine("special.armor_stand.part.head", preset.head));
        lines.add(rotationTooltipLine("special.armor_stand.part.body", preset.body));
        lines.add(rotationTooltipLine("special.armor_stand.part.left_arm", preset.leftArm));
        lines.add(rotationTooltipLine("special.armor_stand.part.right_arm", preset.rightArm));
        lines.add(rotationTooltipLine("special.armor_stand.part.left_leg", preset.leftLeg));
        lines.add(rotationTooltipLine("special.armor_stand.part.right_leg", preset.rightLeg));
        String flags = "Arms "
                + (preset.showArms ? "ON" : "OFF")
                + " | No Base Plate "
                + (preset.noBasePlate ? "ON" : "OFF")
                + " | Marker "
                + (preset.marker ? "ON" : "OFF");
        lines.add(Component.literal(flags));
        return lines;
    }

    private static Component rotationTooltipLine(String partKey, Rotation rotation) {
                String value = ItemEditorText.str(partKey)
                + " (x,y,z): "
                + formatFloat(rotation.x)
                + ", "
                + formatFloat(0.0F)
                + ", "
                + formatFloat(rotation.z);
        return Component.literal(value);
    }

    private static FlowLayout buildPoseRow(
            SpecialDataPanelContext context,
            Component partLabel,
            ItemEditorState.RotationDraft rotation,
            Rotation defaultRotation
    ) {
        boolean compactLayout = isCompactLayout(context);
        int resetButtonWidth = resolveBoundedButtonWidth(
                context.panelWidthHint(),
                5,
                POSE_RESET_BUTTON_WIDTH_MIN,
                POSE_RESET_BUTTON_WIDTH_MAX,
                POSE_RESET_BUTTON_ROW_RESERVE
        );
        FlowLayout row = compactLayout ? UiFactory.column() : UiFactory.row();
        row.child(UiFactory.muted(partLabel, POSE_PART_LABEL_WIDTH).horizontalSizing(compactLayout ? Sizing.fill(100) : UiFactory.fixed(POSE_PART_LABEL_WIDTH)));
        row.child(axisBox(context, "X", rotation.x, value -> rotation.x = value, defaultRotation.x));
        row.child(axisBox(context, "Y", rotation.y, value -> rotation.y = value, 0.0F));
        row.child(axisBox(context, "Z", rotation.z, value -> rotation.z = value, defaultRotation.z));
        row.child(UiFactory.button(ItemEditorText.tr("common.reset"), UiFactory.ButtonTextPreset.STANDARD,  button ->
                context.mutateRefresh(() -> {
                    setRotation(rotation, defaultRotation);
                    context.special().armorStandSelectedPreset = PRESET_NONE;
                })
        ).horizontalSizing(compactLayout ? Sizing.fill(100) : Sizing.fixed(resetButtonWidth)));
        return row;
    }

    private static int previewSize() {
        int responsive = UiFactory.responsiveSquareSize(0.15, 0.26, 82, 200);
        if (isNarrowLayout()) {
            return Math.min(responsive, 136);
        }
        return responsive;
    }

    private static int previewHintWidth() {
        return isNarrowLayout() ? 180 : 240;
    }

    private static boolean isNarrowLayout() {
        var window = Minecraft.getInstance().getWindow();
        return window.getGuiScaledWidth() <= 980;
    }

    private static FlowLayout axisBox(
            SpecialDataPanelContext context,
            String axis,
            String value,
            Consumer<String> setter,
            float defaultValue
    ) {
        boolean compactLayout = isCompactLayout(context);
        int axisStepButtonWidth = Math.max(
                AXIS_STEP_BUTTON_WIDTH_MIN,
                Math.min(AXIS_STEP_BUTTON_WIDTH_MAX, UiFactory.scaledPixels(AXIS_STEP_BUTTON_WIDTH_BASE))
        );
        FlowLayout row = compactLayout ? UiFactory.column() : UiFactory.row();
        row.child(UiFactory.muted(axis, AXIS_LABEL_WIDTH).horizontalSizing(compactLayout ? Sizing.fill(100) : UiFactory.fixed(AXIS_LABEL_WIDTH)));
        row.child(UiFactory.button(Component.literal(AXIS_DECREASE_LABEL), UiFactory.ButtonTextPreset.STANDARD,  button ->
                context.mutateRefresh(() -> {
                    setter.accept(adjustAxis(value, -AXIS_STEP_DEGREES, defaultValue));
                    context.special().armorStandSelectedPreset = PRESET_NONE;
                })
        ).horizontalSizing(compactLayout ? Sizing.fill(100) : Sizing.fixed(axisStepButtonWidth)));
        row.child(UiFactory.textBox(
                value,
                next -> context.mutateRefresh(() -> {
                    setter.accept(next);
                    context.special().armorStandSelectedPreset = PRESET_NONE;
                })
        ).horizontalSizing(compactLayout ? Sizing.fill(100) : UiFactory.fixed(FIELD_WIDTH)));
        row.child(UiFactory.button(Component.literal(AXIS_INCREASE_LABEL), UiFactory.ButtonTextPreset.STANDARD,  button ->
                context.mutateRefresh(() -> {
                    setter.accept(adjustAxis(value, AXIS_STEP_DEGREES, defaultValue));
                    context.special().armorStandSelectedPreset = PRESET_NONE;
                })
        ).horizontalSizing(compactLayout ? Sizing.fill(100) : Sizing.fixed(axisStepButtonWidth)));
        return row;
    }

    private static boolean isCompactLayout(SpecialDataPanelContext context) {
        return context.guiScale() >= COMPACT_LAYOUT_SCALE_THRESHOLD
                || context.panelWidthHint() < UiFactory.scaledPixels(COMPACT_LAYOUT_WIDTH_THRESHOLD);
    }

    private static String adjustAxis(String raw, int delta, float fallback) {
        float base = parseOrDefault(raw, fallback);
        return formatFloat(base + delta);
    }

    private static String formatFloat(float value) {
        return ValidationUtil.trimTrailingZeros(value);
    }

    private static int resolveBoundedButtonWidth(
            int panelWidthHint,
            int buttonCount,
            int minWidth,
            int maxWidth,
            int rowReserve
    ) {
        int contentWidth = Math.max(1, panelWidthHint);
        int preferred = Math.max(
                minWidth,
                Math.min(
                        maxWidth,
                        (contentWidth - UiFactory.scaledPixels(rowReserve)) / Math.max(1, buttonCount)
                )
        );
        return Math.max(1, Math.min(contentWidth, preferred));
    }

    private static int adaptiveTextButtonWidth(
            Component label,
            int minWidth,
            int maxWidth,
            int horizontalPaddingBase
    ) {
        int desired = textPixelWidth(label) + UiFactory.scaledPixels(horizontalPaddingBase);
        return clampWidth(desired, minWidth, maxWidth);
    }

    private static int textPixelWidth(Component text) {
        if (text == null) {
            return 0;
        }
        return Math.max(0, Minecraft.getInstance().font.width(text.getString()));
    }

    private static int clampWidth(int value, int minWidth, int maxWidth) {
        int safeMax = Math.max(1, maxWidth);
        int safeMin = Math.max(1, Math.min(minWidth, safeMax));
        return Math.max(safeMin, Math.min(safeMax, value));
    }

    private static float parseOrDefault(String raw, float fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Float.parseFloat(raw.trim());
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static CompoundTag previewTag(ItemEditorState.SpecialData special) {
        CompoundTag entityTag = new CompoundTag();
        setBooleanKey(entityTag, "Small", special.armorStandSmall);
        setBooleanKey(entityTag, "ShowArms", special.armorStandShowArms);
        setBooleanKey(entityTag, "NoBasePlate", special.armorStandNoBasePlate);
        setBooleanKey(entityTag, "Invisible", special.armorStandInvisible);
        setBooleanKey(entityTag, "NoGravity", special.armorStandNoGravity);
        setBooleanKey(entityTag, "Invulnerable", special.armorStandInvulnerable);
        setBooleanKey(entityTag, "CustomNameVisible", special.armorStandCustomNameVisible);
        setBooleanKey(entityTag, "Marker", special.armorStandMarker);
        if (!special.armorStandCustomName.isBlank()) {
            entityTag.store("CustomName", ComponentSerialization.CODEC, parsePreviewName(special.armorStandCustomName));
        }

        Integer disabledSlots = parseNonNegativeIntOrNull(special.armorStandDisabledSlots);
        if (disabledSlots != null) {
            entityTag.putInt("DisabledSlots", disabledSlots);
        }

        float scale = parseOrDefault(special.armorStandScale, 1.0F);
        if (!special.armorStandScale.isBlank() && scale >= MIN_SCALE && scale <= MAX_SCALE && scale != 1.0F) {
            ListTag attributes = new ListTag();
            CompoundTag scaleTag = new CompoundTag();
            scaleTag.putString("id", "minecraft:scale");
            scaleTag.putDouble("base", scale);
            attributes.add(scaleTag);
            entityTag.put("attributes", attributes);
        }

        CompoundTag poseTag = new CompoundTag();
        putPose(poseTag, "Head", special.armorStandPose.head, DEFAULT_PRESET.head);
        putPose(poseTag, "Body", special.armorStandPose.body, DEFAULT_PRESET.body);
        putPose(poseTag, "LeftArm", special.armorStandPose.leftArm, DEFAULT_PRESET.leftArm);
        putPose(poseTag, "RightArm", special.armorStandPose.rightArm, DEFAULT_PRESET.rightArm);
        putPose(poseTag, "LeftLeg", special.armorStandPose.leftLeg, DEFAULT_PRESET.leftLeg);
        putPose(poseTag, "RightLeg", special.armorStandPose.rightLeg, DEFAULT_PRESET.rightLeg);
        entityTag.put("Pose", poseTag);
        return entityTag;
    }

    private static void putPose(
            CompoundTag poseTag,
            String key,
            ItemEditorState.RotationDraft rotation,
            Rotation fallback
    ) {
        float x = parseOrDefault(rotation.x, fallback.x);
        float y = parseOrDefault(rotation.y, 0.0F);
        float z = parseOrDefault(rotation.z, fallback.z);
        ListTag values = new ListTag();
        values.add(FloatTag.valueOf(x));
        values.add(FloatTag.valueOf(y));
        values.add(FloatTag.valueOf(z));
        poseTag.put(key, values);
    }

    private static void setBooleanKey(CompoundTag tag, String key, boolean value) {
        if (value) {
            tag.putBoolean(key, true);
        }
    }

    private static void applyPreset(ItemEditorState.SpecialData special, PosePreset preset) {
        special.armorStandShowArms = preset.showArms;
        special.armorStandNoBasePlate = preset.noBasePlate;
        special.armorStandMarker = preset.marker;
        setRotation(special.armorStandPose.head, preset.head);
        setRotation(special.armorStandPose.body, preset.body);
        setRotation(special.armorStandPose.leftArm, preset.leftArm);
        setRotation(special.armorStandPose.rightArm, preset.rightArm);
        setRotation(special.armorStandPose.leftLeg, preset.leftLeg);
        setRotation(special.armorStandPose.rightLeg, preset.rightLeg);
    }

    private static void setRotation(ItemEditorState.RotationDraft target, Rotation source) {
        target.x = formatFloat(source.x);
        target.y = formatFloat(0.0F);
        target.z = formatFloat(source.z);
    }

    private static Rotation rotation(float x, float z) {
        return new Rotation(x, z);
    }

    private record Rotation(float x, float z) {
    }

    private record PosePreset(
            String key,
            boolean showArms,
            boolean noBasePlate,
            boolean marker,
            Rotation head,
            Rotation body,
            Rotation leftArm,
            Rotation rightArm,
            Rotation leftLeg,
            Rotation rightLeg
    ) {
    }
}

