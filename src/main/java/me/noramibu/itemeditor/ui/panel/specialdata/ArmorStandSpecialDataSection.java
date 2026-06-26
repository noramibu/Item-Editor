package me.noramibu.itemeditor.ui.panel.specialdata;

import io.wispforest.owo.ui.component.DiscreteSliderComponent;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.VerticalAlignment;
import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.editor.text.RichTextDocument;
import me.noramibu.itemeditor.ui.component.OrbitingArmorStandComponent;
import me.noramibu.itemeditor.ui.component.SafeDiscreteSliderComponent;
import me.noramibu.itemeditor.ui.component.StyledTextFieldSection;
import me.noramibu.itemeditor.ui.component.UiFactory;
import me.noramibu.itemeditor.util.ItemEditorCapabilities;
import me.noramibu.itemeditor.util.ItemEditorText;
import me.noramibu.itemeditor.util.TextComponentUtil;
import me.noramibu.itemeditor.util.ValidationUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Rotations;
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

    private static final int DISABLE_TAKING_OFFSET = 8;
    private static final int DISABLE_PUTTING_OFFSET = 16;
    private static final int FLAG_ROW_GAP = 8;
    private static final int NAME_EDITOR_HEIGHT = 54;
    private static final int AXIS_LABEL_WIDTH = 12;
    private static final int AXIS_VALUE_WIDTH = 28;
    private static final int AXIS_CONTROL_WIDTH = 96;
    private static final int AXIS_SLIDER_HEIGHT = 12;
    private static final int POSE_PART_LABEL_WIDTH = 82;
    private static final int POSE_COMPACT_GAP = 2;
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
    private static final int PRESET_BUTTON_WIDTH_MIN = 48;
    private static final int PRESET_BUTTON_WIDTH_MAX = 96;
    private static final int PRESET_BUTTON_HEIGHT = AXIS_SLIDER_HEIGHT;
    private static final int PRESET_BUTTONS_PER_ROW_MAX = 8;
    private static final float PRESET_BUTTON_TEXT_SCALE = 0.78F;
    private static final int PRESET_LABEL_PADDING_BASE = 12;
    private static final int PRESET_BUTTON_ROW_RESERVE = 12;
    private static final int POSE_RESET_BUTTON_WIDTH_MIN = 58;
    private static final int POSE_RESET_BUTTON_WIDTH_MAX = 86;
    private static final int POSE_RESET_BUTTON_HEIGHT = 14;
    private static final int POSE_RESET_BUTTON_ROW_RESERVE = 18;
    private static final int AXIS_SLIDER_MIN = 0;
    private static final int AXIS_SLIDER_MAX = 359;
    private static final int PRESET_NONE = -1;
    private static final int POSE_ROW_COMPACT_WIDTH_THRESHOLD = 540;
    private static final double PREVIEW_COLUMN_WIDTH_RATIO = 0.32D;
    private static final int PREVIEW_COLUMN_WIDTH_MIN = 96;
    private static final int PREVIEW_COLUMN_WIDTH_MAX = 360;
    private static final int POSE_COLUMN_WIDTH_MIN = 220;
    private static final int PREVIEW_COLUMN_RESERVE = 32;
    private static final int PREVIEW_INLINE_META_MIN_WIDTH = 96;
    private static final int PREVIEW_HINT_WIDTH_MAX = 150;
    private static final int WORKBENCH_GAP = 8;
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
        int previewColumnWidth = previewColumnWidth(context);
        int poseColumnWidth = poseColumnWidth(context, previewColumnWidth);
        OrbitingArmorStandComponent preview = createPreviewComponent(special, previewColumnWidth);
        LabelComponent previewNameLabel = createPreviewNameLabel(special);
        section.child(buildPoseWorkbench(context, special, preview, previewNameLabel, previewColumnWidth, poseColumnWidth));
        section.child(buildNameCard(context, special, preview, previewNameLabel));
        section.child(buildDisabledSlotsCard(context, special));
        section.child(buildNumericCard(context, special));
        return section;
    }

    private static FlowLayout buildPoseWorkbench(
            SpecialDataPanelContext context,
            ItemEditorState.SpecialData special,
            OrbitingArmorStandComponent preview,
            LabelComponent previewNameLabel,
            int previewColumnWidth,
            int poseColumnWidth
    ) {
        FlowLayout workbench = UiFactory.row();
        workbench.gap(UiFactory.scaledPixels(WORKBENCH_GAP));
        workbench.verticalAlignment(VerticalAlignment.TOP);

        FlowLayout poseCard = buildPoseCard(context, special, preview, poseColumnWidth);
        FlowLayout previewCard = buildPreviewCard(special, preview, previewNameLabel, previewColumnWidth);
        poseCard.horizontalSizing(Sizing.expand(100));
        previewCard.horizontalSizing(Sizing.fixed(previewColumnWidth));
        poseCard.verticalSizing(Sizing.fill(100));
        previewCard.verticalSizing(Sizing.fill(100));
        workbench.child(poseCard);
        workbench.child(previewCard);
        return workbench;
    }

    private static FlowLayout buildPreviewCard(
            ItemEditorState.SpecialData special,
            OrbitingArmorStandComponent preview,
            LabelComponent previewNameLabel,
            int previewColumnWidth
    ) {
        FlowLayout card = UiFactory.subCard();
        card.child(UiFactory.title(ItemEditorText.tr("special.armor_stand.preview")).shadow(false));

        boolean inlineMeta = useInlinePreviewMeta(previewColumnWidth, previewSize(previewColumnWidth));
        FlowLayout row = inlineMeta ? UiFactory.row() : UiFactory.column();
        row.gap(FLAG_ROW_GAP);
        row.child(preview);

        FlowLayout meta = UiFactory.column();
        int hintWidth = previewHintWidth(previewColumnWidth, inlineMeta);
        if (!special.armorStandScale.isBlank()) {
            meta.child(UiFactory.muted(ItemEditorText.tr("special.armor_stand.preview.scale", special.armorStandScale), hintWidth));
        }
        if (special.armorStandInvisible) {
            meta.child(UiFactory.muted(ItemEditorText.tr("special.armor_stand.preview.invisible"), hintWidth));
        }
        if (special.armorStandMarker) {
            meta.child(UiFactory.muted(ItemEditorText.tr("special.armor_stand.preview.marker"), hintWidth));
        }
        row.child(meta);

        card.child(row);
        if (previewNameLabel != null) {
            card.child(previewNameLabel);
        }
        return card;
    }

    private static OrbitingArmorStandComponent createPreviewComponent(
            ItemEditorState.SpecialData special,
            int previewColumnWidth
    ) {
        int previewSize = previewSize(previewColumnWidth);
        OrbitingArmorStandComponent preview = new OrbitingArmorStandComponent(
                Sizing.fixed(previewSize),
                previewTag(special)
        );
        preview.horizontalSizing(Sizing.fixed(previewSize));
        preview.verticalSizing(Sizing.fixed(previewSize));
        preview.scaleToFit(true);
        preview.allowMouseRotation(true);
        preview.lookAtCursor(false);
        preview.showNametag(special.armorStandCustomNameVisible && !special.armorStandCustomName.isBlank());
        return preview;
    }

    private static LabelComponent createPreviewNameLabel(ItemEditorState.SpecialData special) {
        if (!special.armorStandCustomNameVisible) {
            return null;
        }
        return UiFactory.message(previewNameComponent(special), 0xE6EEF8);
    }

    private static FlowLayout buildNameCard(
            SpecialDataPanelContext context,
            ItemEditorState.SpecialData special,
            OrbitingArmorStandComponent preview,
            LabelComponent previewNameLabel
    ) {
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
                    updatePreviewName(preview, previewNameLabel, special);
                })
        );

        FlowLayout frame = UiFactory.framedEditorCard();
        frame.child(nameSection.toolbar());
        frame.child(nameSection.editor());
        frame.child(nameSection.validation());
        card.child(frame);
        return card;
    }

    private static FlowLayout buildPoseFlagsBlock(SpecialDataPanelContext context, ItemEditorState.SpecialData special, int poseColumnWidth) {
        boolean compactLayout = useCompactPoseRows(poseColumnWidth);
        FlowLayout block = UiFactory.column();
        block.gap(POSE_COMPACT_GAP);
        block.child(UiFactory.title(ItemEditorText.tr("special.armor_stand.flags")).shadow(false));

        FlowLayout rowA = compactLayout ? UiFactory.column() : UiFactory.row();
        rowA.gap(POSE_COMPACT_GAP);
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
        block.child(rowA);

        FlowLayout rowB = compactLayout ? UiFactory.column() : UiFactory.row();
        rowB.gap(POSE_COMPACT_GAP);
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
        block.child(rowB);

        FlowLayout rowC = compactLayout ? UiFactory.column() : UiFactory.row();
        rowC.gap(POSE_COMPACT_GAP);
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
        block.child(rowC);
        return block;
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

        int lockWidth = adaptiveDisabledActionButtonWidth(lockAllLabel);
        int unlockWidth = adaptiveDisabledActionButtonWidth(unlockAllLabel);
        int reverseWidth = adaptiveDisabledActionButtonWidth(reverseLabel);
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

    private static FlowLayout buildPoseCard(
            SpecialDataPanelContext context,
            ItemEditorState.SpecialData special,
            OrbitingArmorStandComponent preview,
            int poseColumnWidth
    ) {
        FlowLayout card = UiFactory.subCard();
        card.gap(POSE_COMPACT_GAP);
        card.child(UiFactory.title(ItemEditorText.tr("special.armor_stand.pose")).shadow(false));
        card.child(buildPresetRow(context, special, poseColumnWidth));
        card.child(buildPoseFlagsBlock(context, special, poseColumnWidth));
        card.child(buildPoseRow(context, preview, poseColumnWidth, ItemEditorText.tr("special.armor_stand.part.head"), special.armorStandPose.head, DEFAULT_PRESET.head));
        card.child(buildPoseRow(context, preview, poseColumnWidth, ItemEditorText.tr("special.armor_stand.part.body"), special.armorStandPose.body, DEFAULT_PRESET.body));
        card.child(buildPoseRow(context, preview, poseColumnWidth, ItemEditorText.tr("special.armor_stand.part.left_arm"), special.armorStandPose.leftArm, DEFAULT_PRESET.leftArm));
        card.child(buildPoseRow(context, preview, poseColumnWidth, ItemEditorText.tr("special.armor_stand.part.right_arm"), special.armorStandPose.rightArm, DEFAULT_PRESET.rightArm));
        card.child(buildPoseRow(context, preview, poseColumnWidth, ItemEditorText.tr("special.armor_stand.part.left_leg"), special.armorStandPose.leftLeg, DEFAULT_PRESET.leftLeg));
        card.child(buildPoseRow(context, preview, poseColumnWidth, ItemEditorText.tr("special.armor_stand.part.right_leg"), special.armorStandPose.rightLeg, DEFAULT_PRESET.rightLeg));
        return card;
    }

    private static FlowLayout buildPresetRow(SpecialDataPanelContext context, ItemEditorState.SpecialData special, int poseColumnWidth) {
        FlowLayout presetCard = UiFactory.subCard();
        presetCard.gap(POSE_COMPACT_GAP);
        presetCard.child(UiFactory.title(ItemEditorText.tr("special.armor_stand.presets")).shadow(false));
        int availableWidth = Math.max(1, poseColumnWidth - UiFactory.scaledPixels(PRESET_BUTTON_ROW_RESERVE));
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
        int buttonsPerRow = Math.clamp((availableWidth + spacing) / Math.max(1, desiredButtonWidth + spacing), 1, PRESET_BUTTONS_PER_ROW_MAX);
        int rowButtonWidth = Math.min(availableWidth, desiredButtonWidth);
        FlowLayout rows = UiFactory.column();
        rows.gap(POSE_COMPACT_GAP);
        FlowLayout currentRow = UiFactory.row();
        currentRow.gap(POSE_COMPACT_GAP);
        int inRow = 0;
        for (int presetIndex = 0; presetIndex < POSE_PRESETS.size(); presetIndex++) {
            final int selectedIndex = presetIndex;
            PosePreset preset = POSE_PRESETS.get(presetIndex);
            Component label = ItemEditorText.tr(preset.key);
            ButtonComponent button = UiFactory.scaledTextButton(
                    UiFactory.fitToWidth(label, Math.max(1, rowButtonWidth - UiFactory.scaledPixels(8))),
                    PRESET_BUTTON_TEXT_SCALE,
                    UiFactory.ButtonTextPreset.TINY,
                    ignored ->
                    context.mutateRefresh(() -> {
                        applyPreset(special, preset);
                        special.armorStandSelectedPreset = selectedIndex;
                    })
            );
            button.tooltip(presetTooltip(preset));
            button.active(special.armorStandSelectedPreset != selectedIndex);
            button.horizontalSizing(Sizing.fixed(rowButtonWidth));
            button.verticalSizing(Sizing.fixed(PRESET_BUTTON_HEIGHT));
            currentRow.child(button);
            inRow++;
            if (inRow >= buttonsPerRow) {
                rows.child(currentRow);
                currentRow = UiFactory.row();
                currentRow.gap(POSE_COMPACT_GAP);
                inRow = 0;
            }
        }
        if (inRow > 0) {
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
            OrbitingArmorStandComponent preview,
            int poseColumnWidth,
            Component partLabel,
            ItemEditorState.RotationDraft rotation,
            Rotation defaultRotation
    ) {
        boolean compactLayout = useCompactPoseRows(poseColumnWidth);
        FlowLayout row = compactLayout ? UiFactory.column() : UiFactory.row();
        row.gap(POSE_COMPACT_GAP);
        if (!compactLayout) {
            row.verticalAlignment(VerticalAlignment.TOP);
        }
        row.child(UiFactory.muted(partLabel, POSE_PART_LABEL_WIDTH).horizontalSizing(compactLayout ? Sizing.fill(100) : Sizing.fixed(POSE_PART_LABEL_WIDTH)));
        row.child(axisBox(context, preview, poseColumnWidth, "X", rotation.x, value -> rotation.x = value, defaultRotation.x));
        row.child(axisBox(context, preview, poseColumnWidth, "Y", rotation.y, value -> rotation.y = value, 0.0F));
        row.child(axisBox(context, preview, poseColumnWidth, "Z", rotation.z, value -> rotation.z = value, defaultRotation.z));
        row.child(resetControl(context, compactLayout, poseColumnWidth, () -> {
            setRotation(rotation, defaultRotation);
            context.special().armorStandSelectedPreset = PRESET_NONE;
        }));
        return row;
    }

    private static int previewSize(int previewColumnWidth) {
        int responsiveSize = UiFactory.responsiveSquareSize(0.16, 0.30, 82, 200);
        return Math.clamp(
                previewColumnWidth - PREVIEW_COLUMN_RESERVE,
                Math.min(56, responsiveSize),
                responsiveSize
        );
    }

    private static int previewHintWidth(int previewColumnWidth, boolean inlineMeta) {
        int available = inlineMeta
                ? previewColumnWidth / 3
                : previewColumnWidth - PREVIEW_COLUMN_RESERVE;
        return Math.clamp(available, 48, PREVIEW_HINT_WIDTH_MAX);
    }

    private static int previewColumnWidth(SpecialDataPanelContext context) {
        int panelWidth = effectivePanelWidth(context);
        int gap = UiFactory.scaledPixels(WORKBENCH_GAP);
        int available = Math.max(1, panelWidth - gap);
        int maxByPoseColumn = Math.max(1, available - POSE_COLUMN_WIDTH_MIN);
        int candidate = (int) Math.round(panelWidth * PREVIEW_COLUMN_WIDTH_RATIO);
        int preferredMin = Math.clamp(available / 3, 1, PREVIEW_COLUMN_WIDTH_MIN);
        int preferred = clampWidth(candidate, preferredMin, Math.min(PREVIEW_COLUMN_WIDTH_MAX, available));
        return Math.clamp(maxByPoseColumn, 1, preferred);
    }

    private static int poseColumnWidth(SpecialDataPanelContext context, int previewColumnWidth) {
        int panelWidth = effectivePanelWidth(context);
        return Math.max(1, panelWidth - previewColumnWidth - UiFactory.scaledPixels(WORKBENCH_GAP));
    }

    private static boolean useInlinePreviewMeta(int previewColumnWidth, int previewSize) {
        return previewColumnWidth - previewSize - FLAG_ROW_GAP >= PREVIEW_INLINE_META_MIN_WIDTH;
    }

    private static boolean useCompactPoseRows(int poseColumnWidth) {
        return poseColumnWidth < POSE_ROW_COMPACT_WIDTH_THRESHOLD;
    }

    private static int effectivePanelWidth(SpecialDataPanelContext context) {
        int hintedWidth = Math.max(1, context.panelWidthHint());
        int windowWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        return Math.max(hintedWidth, windowWidth - 96);
    }

    private static FlowLayout axisBox(
            SpecialDataPanelContext context,
            OrbitingArmorStandComponent preview,
            int poseColumnWidth,
            String axis,
            String value,
            Consumer<String> setter,
            float defaultValue
    ) {
        boolean compactLayout = useCompactPoseRows(poseColumnWidth);
        float initialDegrees = normalizedDegrees(parseOrDefault(value, defaultValue));
        FlowLayout control = UiFactory.column();
        control.gap(0);
        control.horizontalSizing(compactLayout ? Sizing.fill(100) : Sizing.fixed(AXIS_CONTROL_WIDTH));

        FlowLayout header = UiFactory.row();
        header.child(UiFactory.muted(Component.literal(axis), AXIS_LABEL_WIDTH).horizontalSizing(Sizing.fixed(AXIS_LABEL_WIDTH)));
        LabelComponent valueLabel = UiFactory.muted(Component.literal(formatDegrees(initialDegrees)), AXIS_VALUE_WIDTH);
        valueLabel.horizontalSizing(Sizing.fixed(AXIS_VALUE_WIDTH));
        header.child(valueLabel);
        control.child(header);

        DiscreteSliderComponent slider = new SafeDiscreteSliderComponent(Sizing.fill(100), AXIS_SLIDER_MIN, AXIS_SLIDER_MAX);
        slider.verticalSizing(Sizing.fixed(AXIS_SLIDER_HEIGHT));
        slider.decimalPlaces(0).snap(true);
        slider.setFromDiscreteValue(initialDegrees);
        slider.onChanged().subscribe(next -> {
            int degrees = Math.clamp((int) Math.round(next), AXIS_SLIDER_MIN, AXIS_SLIDER_MAX);
            String stored = formatFloat(signedDegrees(degrees));
            context.mutate(() -> {
                setter.accept(stored);
                context.special().armorStandSelectedPreset = PRESET_NONE;
            });
            updatePreviewPose(preview, context.special());
            valueLabel.text(Component.literal(formatDegrees(degrees)));
        });
        control.child(slider);
        return control;
    }

    private static FlowLayout resetControl(
            SpecialDataPanelContext context,
            boolean compactLayout,
            int poseColumnWidth,
            Runnable onReset
    ) {
        ButtonComponent resetButton = UiFactory.button(ItemEditorText.tr("common.reset"), UiFactory.ButtonTextPreset.TINY, button ->
                context.mutateRefresh(onReset)
        );
        resetButton.verticalSizing(Sizing.fixed(POSE_RESET_BUTTON_HEIGHT));
        if (compactLayout) {
            resetButton.horizontalSizing(Sizing.fill(100));
            FlowLayout compact = UiFactory.column();
            compact.child(resetButton);
            return compact;
        }

        FlowLayout control = UiFactory.column();
        control.gap(0);
        control.horizontalSizing(Sizing.fixed(resolvePoseResetButtonWidth(poseColumnWidth)));
        control.child(UiFactory.muted(Component.literal(" "), AXIS_VALUE_WIDTH).horizontalSizing(Sizing.fixed(AXIS_VALUE_WIDTH)));
        resetButton.horizontalSizing(Sizing.fill(100));
        control.child(resetButton);
        return control;
    }

    private static boolean isCompactLayout(SpecialDataPanelContext context) {
        return context.guiScale() >= COMPACT_LAYOUT_SCALE_THRESHOLD
                || context.panelWidthHint() < UiFactory.scaledPixels(COMPACT_LAYOUT_WIDTH_THRESHOLD);
    }

    private static void updatePreviewPose(OrbitingArmorStandComponent preview, ItemEditorState.SpecialData special) {
        if (preview == null) {
            return;
        }
        preview.entity().setHeadPose(rotations(special.armorStandPose.head, DEFAULT_PRESET.head));
        preview.entity().setBodyPose(rotations(special.armorStandPose.body, DEFAULT_PRESET.body));
        preview.entity().setLeftArmPose(rotations(special.armorStandPose.leftArm, DEFAULT_PRESET.leftArm));
        preview.entity().setRightArmPose(rotations(special.armorStandPose.rightArm, DEFAULT_PRESET.rightArm));
        preview.entity().setLeftLegPose(rotations(special.armorStandPose.leftLeg, DEFAULT_PRESET.leftLeg));
        preview.entity().setRightLegPose(rotations(special.armorStandPose.rightLeg, DEFAULT_PRESET.rightLeg));
    }

    private static void updatePreviewName(
            OrbitingArmorStandComponent preview,
            LabelComponent previewNameLabel,
            ItemEditorState.SpecialData special
    ) {
        boolean visibleName = special.armorStandCustomNameVisible && !special.armorStandCustomName.isBlank();
        if (preview != null) {
            preview.showNametag(visibleName);
            preview.entity().setCustomName(visibleName ? displayPreviewName(special.armorStandCustomName) : null);
            preview.entity().setCustomNameVisible(visibleName);
        }
        if (previewNameLabel != null) {
            previewNameLabel.text(previewNameComponent(special));
        }
    }

    private static Component previewNameComponent(ItemEditorState.SpecialData special) {
        if (special == null || special.armorStandCustomName.isBlank()) {
            return Component.literal(" ");
        }
        return displayPreviewName(special.armorStandCustomName);
    }

    private static Rotations rotations(ItemEditorState.RotationDraft rotation, Rotation fallback) {
        return new Rotations(
                parseOrDefault(rotation.x, fallback.x),
                parseOrDefault(rotation.y, 0.0F),
                parseOrDefault(rotation.z, fallback.z)
        );
    }

    private static float normalizedDegrees(float value) {
        float normalized = value % 360.0F;
        if (normalized < 0.0F) {
            normalized += 360.0F;
        }
        return normalized;
    }

    private static float signedDegrees(int degrees) {
        int normalized = Math.floorMod(degrees, 360);
        return normalized > 180 ? normalized - 360.0F : normalized;
    }

    private static String formatDegrees(float value) {
        return Integer.toString(Math.clamp(Math.round(normalizedDegrees(value)), AXIS_SLIDER_MIN, AXIS_SLIDER_MAX));
    }

    private static String formatFloat(float value) {
        return ValidationUtil.trimTrailingZeros(value);
    }

    private static int resolvePoseResetButtonWidth(int panelWidthHint) {
        int contentWidth = Math.max(1, panelWidthHint);
        int preferred = Math.clamp(
                (contentWidth - UiFactory.scaledPixels(POSE_RESET_BUTTON_ROW_RESERVE)) / 5,
                POSE_RESET_BUTTON_WIDTH_MIN,
                POSE_RESET_BUTTON_WIDTH_MAX
        );
        return Math.min(contentWidth, preferred);
    }

    private static int adaptiveDisabledActionButtonWidth(Component label) {
        int desired = textPixelWidth(label) + UiFactory.scaledPixels(DISABLED_ACTION_LABEL_PADDING_BASE);
        return clampWidth(desired, DISABLED_ACTION_BUTTON_WIDTH_MIN, DISABLED_ACTION_BUTTON_WIDTH_MAX);
    }

    private static int textPixelWidth(Component text) {
        if (text == null) {
            return 0;
        }
        return Math.max(0, Minecraft.getInstance().font.width(text.getString()));
    }

    private static int clampWidth(int value, int minWidth, int maxWidth) {
        int safeMax = Math.max(1, maxWidth);
        int safeMin = Math.clamp(safeMax, 1, minWidth);
        return Math.clamp(value, safeMin, safeMax);
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

