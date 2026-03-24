package me.noramibu.itemeditor.ui.panel.specialdata;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Sizing;
import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.ui.component.ColorTokenListEditor;
import me.noramibu.itemeditor.ui.component.PickerFieldFactory;
import me.noramibu.itemeditor.ui.component.UiFactory;
import me.noramibu.itemeditor.util.ItemEditorText;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.FireworkExplosion;

import java.util.Arrays;
import java.util.Locale;

public final class FireworkSpecialDataSection {

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
        FlowLayout section = UiFactory.section(ItemEditorText.tr("special.firework.rocket.title"), Component.empty());

        section.child(UiFactory.field(
                ItemEditorText.tr("special.firework.flight_duration"),
                Component.empty(),
                UiFactory.textBox(special.fireworkFlightDuration, context.bindText(value -> special.fireworkFlightDuration = value))
                        .horizontalSizing(Sizing.fixed(100))
        ));
        section.child(UiFactory.button(ItemEditorText.tr("special.firework.add_explosion"), button ->
                context.mutateRefresh(() -> special.rocketExplosions.add(new ItemEditorState.FireworkExplosionDraft()))
        ));

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
        FlowLayout card = removeAction == null
                ? UiFactory.reorderableSubCard(Component.literal(title), false, null, false, null, null)
                : context.createRemovableCard(Component.literal(title), removeAction);

        card.child(PickerFieldFactory.dropdownField(
                context,
                ItemEditorText.tr("special.firework.shape"),
                Component.empty(),
                PickerFieldFactory.selectedOrFallback(draft.shape, Component.literal(FireworkExplosion.Shape.SMALL_BALL.name())),
                180,
                Arrays.asList(FireworkExplosion.Shape.values()),
                FireworkExplosion.Shape::name,
                shape -> context.mutateRefresh(() -> draft.shape = shape.name())
        ));
        card.child(buildShapeMaterialQuickPick(context, draft));
        card.child(UiFactory.muted(ItemEditorText.tr("special.firework.material.current", ItemEditorText.str(shapeMaterialLabelKey(draft.shape))), 260));

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

        FlowLayout toggles = UiFactory.row();
        toggles.child(UiFactory.checkbox(ItemEditorText.tr("special.firework.trail"), draft.trail, context.bindToggle(value -> draft.trail = value)));
        toggles.child(UiFactory.checkbox(ItemEditorText.tr("special.firework.twinkle"), draft.twinkle, context.bindToggle(value -> draft.twinkle = value)));
        card.child(toggles);
        card.child(UiFactory.muted(ItemEditorText.tr("special.firework.material.trail_twinkle"), 260));
        return card;
    }

    private static FlowLayout buildShapeMaterialQuickPick(SpecialDataPanelContext context, ItemEditorState.FireworkExplosionDraft draft) {
        FlowLayout row = UiFactory.row();
        row.child(UiFactory.muted(ItemEditorText.tr("special.firework.material.shape"), 92));
        row.child(shapeMaterialButton(
                context,
                draft,
                ItemEditorText.tr("special.firework.material.small_ball"),
                FireworkExplosion.Shape.SMALL_BALL
        ));
        row.child(shapeMaterialButton(
                context,
                draft,
                ItemEditorText.tr("special.firework.material.large_ball"),
                FireworkExplosion.Shape.LARGE_BALL
        ));
        row.child(shapeMaterialButton(
                context,
                draft,
                ItemEditorText.tr("special.firework.material.star"),
                FireworkExplosion.Shape.STAR
        ));
        row.child(shapeMaterialButton(
                context,
                draft,
                ItemEditorText.tr("special.firework.material.creeper"),
                FireworkExplosion.Shape.CREEPER
        ));
        row.child(shapeMaterialButton(
                context,
                draft,
                ItemEditorText.tr("special.firework.material.burst"),
                FireworkExplosion.Shape.BURST
        ));
        return row;
    }

    private static ButtonComponent shapeMaterialButton(
            SpecialDataPanelContext context,
            ItemEditorState.FireworkExplosionDraft draft,
            Component label,
            FireworkExplosion.Shape shape
    ) {
        ButtonComponent button = UiFactory.button(label, component -> context.mutateRefresh(() -> draft.shape = shape.name()));
        button.active(!shape.name().equalsIgnoreCase(draft.shape));
        button.horizontalSizing(Sizing.content());
        return button;
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
