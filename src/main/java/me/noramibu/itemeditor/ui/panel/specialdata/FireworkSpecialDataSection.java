package me.noramibu.itemeditor.ui.panel.specialdata;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Sizing;
import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.ui.component.ButtonFitUtil;
import me.noramibu.itemeditor.ui.component.ColorTokenListEditor;
import me.noramibu.itemeditor.ui.component.PickerFieldFactory;
import me.noramibu.itemeditor.ui.component.UiFactory;
import me.noramibu.itemeditor.ui.util.LayoutModeUtil;
import me.noramibu.itemeditor.util.ItemEditorText;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.FireworkExplosion;

import java.util.Arrays;
import java.util.Locale;

public final class FireworkSpecialDataSection {
    private static final int COMPACT_LAYOUT_WIDTH_THRESHOLD = 600;
    private static final int FLIGHT_DURATION_FIELD_WIDTH = 100;
    private static final int SHAPE_PICKER_BUTTON_WIDTH = 180;
    private static final int SHAPE_LABEL_WIDTH = 92;
    private static final int SHAPE_QUICK_PICK_BUTTON_MIN = 72;
    private static final int SHAPE_QUICK_PICK_BUTTON_MAX = 140;
    private static final int SHAPE_QUICK_PICK_ROW_RESERVE = 48;
    private static final int SHAPE_QUICK_PICK_BUTTON_COUNT = 5;
    private static final int SHAPE_QUICK_PICK_TEXT_MIN = 24;
    private static final int SHAPE_QUICK_PICK_TEXT_RESERVE = 10;
    private static final int MATERIAL_HINT_WIDTH = 260;
    private static final int ADD_EXPLOSION_BUTTON_MIN = 92;
    private static final int ADD_EXPLOSION_BUTTON_MAX = 180;
    private static final int ADD_EXPLOSION_BUTTON_TEXT_MIN = 24;
    private static final int ADD_EXPLOSION_BUTTON_TEXT_RESERVE = 10;

    private FireworkSpecialDataSection() {
    }

    public static boolean supportsRocket(ItemStack stack) {
        return stack.has(DataComponents.FIREWORKS) || stack.is(Items.FIREWORK_ROCKET);
    }

    public static boolean supportsStar(ItemStack stack) {
        return stack.has(DataComponents.FIREWORK_EXPLOSION) || stack.is(Items.FIREWORK_STAR);
    }

    public static FlowLayout buildRocket(SpecialDataPanelContext context) {
        ItemEditorState.SpecialData special = context.special();
        boolean compactLayout = isCompactLayout(context);
        FlowLayout section = UiFactory.section(ItemEditorText.tr("special.firework.rocket.title"), Component.empty());

        section.child(UiFactory.field(
                ItemEditorText.tr("special.firework.flight_duration"),
                Component.empty(),
                UiFactory.textBox(special.fireworkFlightDuration, context.bindText(value -> special.fireworkFlightDuration = value))
                        .horizontalSizing(compactLayout ? Sizing.fill(100) : UiFactory.fixed(FLIGHT_DURATION_FIELD_WIDTH))
        ));
        Component addExplosionText = ItemEditorText.tr("special.firework.add_explosion");
        ButtonComponent addExplosionButton = UiFactory.button(addExplosionText, UiFactory.ButtonTextPreset.STANDARD, button ->
                context.mutateRefresh(() -> special.rocketExplosions.add(new ItemEditorState.FireworkExplosionDraft()))
        );
        int contentWidth = context.panelWidthHint();
        int addExplosionWidth = Math.max(
                ADD_EXPLOSION_BUTTON_MIN,
                Math.min(ADD_EXPLOSION_BUTTON_MAX, contentWidth / 2)
        );
        addExplosionWidth = Math.min(contentWidth, addExplosionWidth);
        applyResponsiveButtonSizing(
                addExplosionButton,
                compactLayout,
                addExplosionText,
                addExplosionWidth,
                ADD_EXPLOSION_BUTTON_TEXT_MIN,
                ADD_EXPLOSION_BUTTON_TEXT_RESERVE
        );
        section.child(addExplosionButton);

        for (int index = 0; index < special.rocketExplosions.size(); index++) {
            int currentIndex = index;
            section.child(buildExplosionEditor(
                    context,
                    ItemEditorText.str("special.firework.explosion", index + 1),
                    special.rocketExplosions.get(currentIndex),
                    () -> special.rocketExplosions.remove(currentIndex)
            ));
        }

        return section;
    }

    public static FlowLayout buildStar(SpecialDataPanelContext context) {
        FlowLayout section = UiFactory.section(ItemEditorText.tr("special.firework.star.title"), Component.empty());
        section.child(buildExplosionEditor(context, ItemEditorText.str("special.firework.star_explosion"), context.special().starExplosion, null));
        return section;
    }

    private static FlowLayout buildExplosionEditor(
            SpecialDataPanelContext context,
            String title,
            ItemEditorState.FireworkExplosionDraft draft,
            Runnable removeAction
    ) {
        boolean compactLayout = isCompactLayout(context);
        FlowLayout card = removeAction == null
                ? UiFactory.reorderableSubCard(Component.literal(title), false, null, false, null, null)
                : context.createRemovableCard(Component.literal(title), removeAction);

        card.child(PickerFieldFactory.dropdownField(
                context,
                ItemEditorText.tr("special.firework.shape"),
                Component.empty(),
                PickerFieldFactory.selectedOrFallback(draft.shape, Component.literal(FireworkExplosion.Shape.SMALL_BALL.name())),
                compactLayout ? -1 : SHAPE_PICKER_BUTTON_WIDTH,
                Arrays.asList(FireworkExplosion.Shape.values()),
                FireworkExplosion.Shape::name,
                shape -> context.mutateRefresh(() -> draft.shape = shape.name())
        ));
        card.child(buildShapeMaterialQuickPick(context, draft));
        card.child(UiFactory.muted(ItemEditorText.tr("special.firework.material.current", ItemEditorText.str(shapeMaterialLabelKey(draft.shape))), MATERIAL_HINT_WIDTH));

        FlowLayout colors = UiFactory.column().gap(3);
        colors.child(ColorTokenListEditor.buildField(
                ItemEditorText.tr("special.firework.colors"),
                Component.empty(),
                () -> draft.colors,
                value -> draft.colors = value,
                0xFF0000,
                context::mutateRefresh,
                (initialColor, onApply) -> context.screen().openColorPickerDialog(ItemEditorText.str("special.firework.colors"), initialColor, onApply),
                index -> ItemEditorText.tr("special.firework.color_pick_existing", index + 1)
        ));
        colors.child(ColorTokenListEditor.buildField(
                ItemEditorText.tr("special.firework.fade_colors"),
                Component.empty(),
                () -> draft.fadeColors,
                value -> draft.fadeColors = value,
                0xFFFFFF,
                context::mutateRefresh,
                (initialColor, onApply) -> context.screen().openColorPickerDialog(ItemEditorText.str("special.firework.fade_colors"), initialColor, onApply),
                index -> ItemEditorText.tr("special.firework.color_pick_existing", index + 1)
        ));
        card.child(colors);

        FlowLayout toggles = compactLayout ? UiFactory.column() : UiFactory.row();
        toggles.child(UiFactory.checkbox(ItemEditorText.tr("special.firework.trail"), draft.trail, context.bindToggle(value -> draft.trail = value)));
        toggles.child(UiFactory.checkbox(ItemEditorText.tr("special.firework.twinkle"), draft.twinkle, context.bindToggle(value -> draft.twinkle = value)));
        card.child(toggles);
        card.child(UiFactory.muted(ItemEditorText.tr("special.firework.material.trail_twinkle"), MATERIAL_HINT_WIDTH));
        return card;
    }

    private static FlowLayout buildShapeMaterialQuickPick(SpecialDataPanelContext context, ItemEditorState.FireworkExplosionDraft draft) {
        boolean compactLayout = isCompactLayout(context);
        FlowLayout row = compactLayout ? UiFactory.column() : UiFactory.row();
        int contentWidth = context.panelWidthHint();
        int quickPickButtonWidth = Math.max(
                SHAPE_QUICK_PICK_BUTTON_MIN,
                Math.min(
                        SHAPE_QUICK_PICK_BUTTON_MAX,
                        (contentWidth - UiFactory.scaledPixels(SHAPE_QUICK_PICK_ROW_RESERVE) - UiFactory.scaledPixels(SHAPE_LABEL_WIDTH))
                                / SHAPE_QUICK_PICK_BUTTON_COUNT
                )
        );
        quickPickButtonWidth = Math.min(contentWidth, quickPickButtonWidth);
        row.child(UiFactory.muted(ItemEditorText.tr("special.firework.material.shape"), SHAPE_LABEL_WIDTH));
        row.child(shapeMaterialButton(
                context,
                draft,
                ItemEditorText.tr("special.firework.material.small_ball"),
                FireworkExplosion.Shape.SMALL_BALL,
                compactLayout,
                quickPickButtonWidth
        ));
        row.child(shapeMaterialButton(
                context,
                draft,
                ItemEditorText.tr("special.firework.material.large_ball"),
                FireworkExplosion.Shape.LARGE_BALL,
                compactLayout,
                quickPickButtonWidth
        ));
        row.child(shapeMaterialButton(
                context,
                draft,
                ItemEditorText.tr("special.firework.material.star"),
                FireworkExplosion.Shape.STAR,
                compactLayout,
                quickPickButtonWidth
        ));
        row.child(shapeMaterialButton(
                context,
                draft,
                ItemEditorText.tr("special.firework.material.creeper"),
                FireworkExplosion.Shape.CREEPER,
                compactLayout,
                quickPickButtonWidth
        ));
        row.child(shapeMaterialButton(
                context,
                draft,
                ItemEditorText.tr("special.firework.material.burst"),
                FireworkExplosion.Shape.BURST,
                compactLayout,
                quickPickButtonWidth
        ));
        return row;
    }

    private static ButtonComponent shapeMaterialButton(
            SpecialDataPanelContext context,
            ItemEditorState.FireworkExplosionDraft draft,
            Component label,
            FireworkExplosion.Shape shape,
            boolean compactLayout,
            int buttonWidth
    ) {
        ButtonComponent button = UiFactory.button(label, UiFactory.ButtonTextPreset.STANDARD,  component -> context.mutateRefresh(() -> draft.shape = shape.name()));
        button.active(!shape.name().equalsIgnoreCase(draft.shape));
        applyResponsiveButtonSizing(
                button,
                compactLayout,
                label,
                buttonWidth,
                SHAPE_QUICK_PICK_TEXT_MIN,
                SHAPE_QUICK_PICK_TEXT_RESERVE
        );
        return button;
    }

    private static void applyResponsiveButtonSizing(
            ButtonComponent button,
            boolean compactLayout,
            Component label,
            int fixedWidth,
            int textMinWidth,
            int textReserve
    ) {
        if (compactLayout) {
            button.horizontalSizing(Sizing.fill(100));
            return;
        }
        ButtonFitUtil.applyFittedFixedLabel(
                button,
                label,
                fixedWidth,
                textMinWidth,
                textReserve
        );
    }

    private static boolean isCompactLayout(SpecialDataPanelContext context) {
        return LayoutModeUtil.isCompactPanel(context.guiScale(), context.panelWidthHint(), COMPACT_LAYOUT_WIDTH_THRESHOLD);
    }

    private static String shapeMaterialLabelKey(String shape) {
        return switch (normalizeShape(shape)) {
            case "large_ball" -> "special.firework.material.large_ball";
            case "star" -> "special.firework.material.star";
            case "creeper" -> "special.firework.material.creeper";
            case "burst" -> "special.firework.material.burst";
            default -> "special.firework.material.small_ball";
        };
    }

    private static String normalizeShape(String shape) {
        return shape == null ? "" : shape.trim().toLowerCase(Locale.ROOT);
    }
}
