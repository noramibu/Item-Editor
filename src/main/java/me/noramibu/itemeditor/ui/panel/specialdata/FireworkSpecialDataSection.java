package me.noramibu.itemeditor.ui.panel.specialdata;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Sizing;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;
import me.noramibu.itemeditor.editor.EditorCategory;
import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.ui.component.ColorTokenListEditor;
import me.noramibu.itemeditor.ui.component.EditorSearchDialog;
import me.noramibu.itemeditor.ui.component.PickerFieldFactory;
import me.noramibu.itemeditor.ui.component.UiFactory;
import me.noramibu.itemeditor.ui.component.UnifiedColorPickerDialog;
import me.noramibu.itemeditor.util.ItemEditorText;
import me.noramibu.itemeditor.util.ValidationUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.FireworkExplosion;

public final class FireworkSpecialDataSection {
    public static List<EditorSearchDialog.Target> searchRocketTargets(SpecialDataPanelContext context) {
        var result = new ArrayList<>(SpecialDataSearch.targets(
                context,
                EditorCategory.SPECIAL_DATA,
                "special.firework.rocket.title",
                "firework-rocket",
                () -> {},
                Field.values()));
        for (int index = 0; index < context.special().rocketExplosions.size(); index++) {
            var draft = context.special().rocketExplosions.get(index);
            result.addAll(explosionSearchTargets(
                    context,
                    draft,
                    List.of(
                            ItemEditorText.str("special.firework.rocket.title"),
                            ItemEditorText.str("special.firework.explosion", index + 1)),
                    true));
        }
        return result;
    }

    public static List<EditorSearchDialog.Target> searchStarTargets(SpecialDataPanelContext context) {
        return explosionSearchTargets(
                context,
                context.special().starExplosion,
                List.of(ItemEditorText.str("special.firework.star.title")),
                false);
    }

    private static final SpecialDataSearch.Field MOVE_LEFT = () -> "special.firework.color_move_left";
    private static final SpecialDataSearch.Field MOVE_RIGHT = () -> "special.firework.color_move_right";

    private static List<EditorSearchDialog.Target> explosionSearchTargets(
            SpecialDataPanelContext context,
            ItemEditorState.FireworkExplosionDraft draft,
            List<String> path,
            boolean removable) {
        String scope = SpecialDataSearch.scope("firework-explosion", draft);
        var result = new ArrayList<>(SpecialDataSearch.targets(
                context, EditorCategory.SPECIAL_DATA, path, scope, () -> {}, ExplosionField.values()));
        for (FireworkExplosion.Shape shape : FireworkExplosion.Shape.values()) {
            result.add(materialControl(shape).target(context, EditorCategory.SPECIAL_DATA, path, scope, () -> {}));
        }
        if (removable) {
            result.addAll(SpecialDataSearch.targets(
                    context,
                    EditorCategory.SPECIAL_DATA,
                    path,
                    scope,
                    () -> {},
                    SpecialDataPanelContext.ItemAction.REMOVE));
        }
        for (ExplosionField field : List.of(ExplosionField.COLORS, ExplosionField.FADE_COLORS)) {
            String raw = field == ExplosionField.COLORS ? draft.colors : draft.fadeColors;
            int fallback = field == ExplosionField.COLORS ? 0xFF0000 : 0xFFFFFF;
            var colorPath = new ArrayList<>(path);
            colorPath.add(field.text().getString());
            String colorScope = colorScope(draft, field.key());
            for (var control : colorHeaderControls()) {
                result.add(control.target(context, EditorCategory.SPECIAL_DATA, colorPath, colorScope, () -> {}));
            }
            var tokens = ColorTokenListEditor.splitColorTokens(raw);
            for (int index = 0; index < tokens.size(); index++) {
                var tokenPath = new ArrayList<>(colorPath);
                tokenPath.add(ItemEditorText.str("special.firework.color_pick_existing", index + 1));
                for (var control : colorTokenControls(tokens, index, fallback)) {
                    result.add(control.target(context, EditorCategory.SPECIAL_DATA, tokenPath, colorScope, () -> {}));
                }
            }
        }
        return result;
    }

    static SpecialDataSearch.Control materialControl(FireworkExplosion.Shape shape) {
        String key = shapeMaterialLabelKey(shape.name());
        return new SpecialDataSearch.Control(
                "firework-material:" + shape.name(),
                ItemEditorText.tr(key),
                shape.name() + " " + EditorSearchDialog.english(key));
    }

    private static String colorScope(ItemEditorState.FireworkExplosionDraft draft, String key) {
        return SpecialDataSearch.scope("firework-colors", draft) + ":" + key;
    }

    static List<SpecialDataSearch.Control> colorHeaderControls() {
        return List.of(
                new SpecialDataSearch.Control("color-add", SpecialDataPanelContext.ItemAction.PICK.text(), "add color"),
                new SpecialDataSearch.Control(
                        "color-remove-last", SpecialDataPanelContext.ItemAction.REMOVE.text(), "remove last color"));
    }

    static List<SpecialDataSearch.Control> colorTokenControls(List<String> tokens, int index, int fallback) {
        var controls = new ArrayList<SpecialDataSearch.Control>();
        if (index > 0) {
            controls.add(new SpecialDataSearch.Control(
                    "color-left:" + index, MOVE_LEFT.text(), EditorSearchDialog.english(MOVE_LEFT.key())));
        }
        if (index + 1 < tokens.size()) {
            controls.add(new SpecialDataSearch.Control(
                    "color-right:" + index, MOVE_RIGHT.text(), EditorSearchDialog.english(MOVE_RIGHT.key())));
        }
        int color = ValidationUtil.parseHexColorOrDefault(tokens.get(index), fallback);
        controls.add(new SpecialDataSearch.Control(
                "color-edit:" + index,
                Component.literal(ValidationUtil.toHex(color)).withColor(color),
                tokens.get(index) + " edit color"));
        controls.add(new SpecialDataSearch.Control(
                "color-remove:" + index,
                SpecialDataPanelContext.ItemAction.REMOVE.text(),
                "remove color " + tokens.get(index)));
        return List.copyOf(controls);
    }

    private static final int COMPACT_LAYOUT_WIDTH_THRESHOLD = 600;
    private static final int FLIGHT_DURATION_FIELD_WIDTH = 100;
    private static final int SHAPE_PICKER_BUTTON_WIDTH = 180;
    private static final int SHAPE_LABEL_WIDTH = 92;
    private static final int SHAPE_QUICK_PICK_BUTTON_MIN = 72;
    private static final int SHAPE_QUICK_PICK_BUTTON_MAX = 140;
    private static final int SHAPE_QUICK_PICK_ROW_RESERVE = 48;
    private static final int MATERIAL_HINT_WIDTH = 260;
    private static final int ADD_EXPLOSION_BUTTON_MIN = 92;
    private static final int ADD_EXPLOSION_BUTTON_MAX = 180;

    private FireworkSpecialDataSection() {}

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
        section.id("firework-rocket");

        section.child(UiFactory.field(
                Field.FLIGHT_DURATION.text(),
                Component.empty(),
                UiFactory.textBox(
                                special.fireworkFlightDuration,
                                context.bindText(value -> special.fireworkFlightDuration = value))
                        .horizontalSizing(
                                compactLayout ? Sizing.fill(100) : UiFactory.fixed(FLIGHT_DURATION_FIELD_WIDTH))));
        Component addExplosionText = Field.ADD_EXPLOSION.text();
        ButtonComponent addExplosionButton = UiFactory.button(
                addExplosionText,
                UiFactory.ButtonTextPreset.STANDARD,
                button -> context.mutateRefresh(
                        () -> special.rocketExplosions.add(new ItemEditorState.FireworkExplosionDraft())));
        int contentWidth = context.panelWidthHint();
        int addExplosionWidth = Math.clamp(contentWidth / 2, ADD_EXPLOSION_BUTTON_MIN, ADD_EXPLOSION_BUTTON_MAX);
        addExplosionWidth = Math.min(contentWidth, addExplosionWidth);
        applyResponsiveButtonSizing(addExplosionButton, compactLayout, addExplosionText, addExplosionWidth);
        section.child(addExplosionButton);

        for (int index = 0; index < special.rocketExplosions.size(); index++) {
            int currentIndex = index;
            section.child(buildExplosionEditor(
                    context,
                    ItemEditorText.str("special.firework.explosion", index + 1),
                    special.rocketExplosions.get(currentIndex),
                    () -> special.rocketExplosions.remove(currentIndex)));
        }

        return section;
    }

    public static FlowLayout buildStar(SpecialDataPanelContext context) {
        FlowLayout section = UiFactory.section(ItemEditorText.tr("special.firework.star.title"), Component.empty());
        section.child(buildExplosionEditor(
                context, ItemEditorText.str("special.firework.star_explosion"), context.special().starExplosion, null));
        return section;
    }

    private static FlowLayout buildExplosionEditor(
            SpecialDataPanelContext context,
            String title,
            ItemEditorState.FireworkExplosionDraft draft,
            Runnable removeAction) {
        boolean compactLayout = isCompactLayout(context);
        FlowLayout card = removeAction == null
                ? UiFactory.reorderableSubCard(Component.literal(title), false, null, false, null, null)
                : context.createRemovableCard(Component.literal(title), removeAction);

        card.id(SpecialDataSearch.scope("firework-explosion", draft));
        card.child(PickerFieldFactory.dropdownField(
                context,
                ExplosionField.SHAPE.text(),
                Component.empty(),
                PickerFieldFactory.selectedOrFallback(
                        draft.shape, Component.literal(FireworkExplosion.Shape.SMALL_BALL.name())),
                compactLayout ? -1 : SHAPE_PICKER_BUTTON_WIDTH,
                Arrays.asList(FireworkExplosion.Shape.values()),
                FireworkExplosion.Shape::name,
                shape -> context.mutateRefresh(() -> draft.shape = shape.name())));
        card.child(buildShapeMaterialQuickPick(context, draft));
        card.child(UiFactory.muted(
                ItemEditorText.tr(
                        "special.firework.material.current", ItemEditorText.str(shapeMaterialLabelKey(draft.shape))),
                MATERIAL_HINT_WIDTH));

        FlowLayout colors = UiFactory.column().gap(3);
        colors.child(colorTokenField(
                context,
                draft,
                ExplosionField.COLORS.key(),
                () -> draft.colors,
                value -> draft.colors = value,
                0xFF0000));
        colors.child(colorTokenField(
                context,
                draft,
                ExplosionField.FADE_COLORS.key(),
                () -> draft.fadeColors,
                value -> draft.fadeColors = value,
                0xFFFFFF));
        card.child(colors);

        FlowLayout toggles = compactLayout ? UiFactory.column() : UiFactory.row();
        toggles.child(UiFactory.checkbox(
                ExplosionField.TRAIL.text(), draft.trail, context.bindToggle(value -> draft.trail = value)));
        toggles.child(UiFactory.checkbox(
                ExplosionField.TWINKLE.text(), draft.twinkle, context.bindToggle(value -> draft.twinkle = value)));
        card.child(toggles);
        card.child(UiFactory.muted(ItemEditorText.tr("special.firework.material.trail_twinkle"), MATERIAL_HINT_WIDTH));
        return card;
    }

    private static FlowLayout colorTokenField(
            SpecialDataPanelContext context,
            ItemEditorState.FireworkExplosionDraft draft,
            String labelKey,
            Supplier<String> currentValueSupplier,
            Consumer<String> setter,
            int fallbackColor) {
        FlowLayout field = ColorTokenListEditor.buildField(
                ItemEditorText.tr(labelKey),
                Component.empty(),
                currentValueSupplier,
                setter,
                fallbackColor,
                context.panelWidthHint(),
                context::mutateRefresh,
                (initialColor, onApply) -> context.screen()
                        .openUnifiedColorPickerDialog(
                                ItemEditorText.str(labelKey),
                                UnifiedColorPickerDialog.Options.plainColor(initialColor),
                                result -> onApply.accept(result.colors().getFirst())),
                index -> ItemEditorText.tr("special.firework.color_pick_existing", index + 1));
        field.id(colorScope(draft, labelKey));
        FlowLayout content = (FlowLayout) field.children().getLast();
        SpecialDataSearch.bindControls(content.children().getFirst(), colorHeaderControls());
        var tokens = ColorTokenListEditor.splitColorTokens(currentValueSupplier.get());
        if (!tokens.isEmpty()) {
            FlowLayout chips = (FlowLayout) content.children().getLast();
            for (int index = 0; index < tokens.size(); index++) {
                SpecialDataSearch.bindControls(
                        chips.children().get(index), colorTokenControls(tokens, index, fallbackColor));
            }
        }
        return field;
    }

    private static FlowLayout buildShapeMaterialQuickPick(
            SpecialDataPanelContext context, ItemEditorState.FireworkExplosionDraft draft) {
        boolean compactLayout = isCompactLayout(context);
        FlowLayout row = compactLayout ? UiFactory.column() : UiFactory.row();
        int contentWidth = context.panelWidthHint();
        int quickPickButtonWidth = Math.clamp(
                (contentWidth
                                - UiFactory.scaledPixels(SHAPE_QUICK_PICK_ROW_RESERVE)
                                - UiFactory.scaledPixels(SHAPE_LABEL_WIDTH))
                        / FireworkExplosion.Shape.values().length,
                SHAPE_QUICK_PICK_BUTTON_MIN,
                SHAPE_QUICK_PICK_BUTTON_MAX);
        quickPickButtonWidth = Math.min(contentWidth, quickPickButtonWidth);
        row.child(UiFactory.muted(ItemEditorText.tr("special.firework.material.shape"), SHAPE_LABEL_WIDTH));
        for (FireworkExplosion.Shape shape : FireworkExplosion.Shape.values()) {
            row.child(shapeMaterialButton(context, draft, shape, compactLayout, quickPickButtonWidth));
        }
        return row;
    }

    private static ButtonComponent shapeMaterialButton(
            SpecialDataPanelContext context,
            ItemEditorState.FireworkExplosionDraft draft,
            FireworkExplosion.Shape shape,
            boolean compactLayout,
            int buttonWidth) {
        var control = materialControl(shape);
        Component label = control.label();
        ButtonComponent button = UiFactory.button(
                label,
                UiFactory.ButtonTextPreset.STANDARD,
                component -> context.mutateRefresh(() -> draft.shape = shape.name()));
        button.id(control.id());
        button.active(!shape.name().equalsIgnoreCase(draft.shape));
        applyResponsiveButtonSizing(button, compactLayout, label, buttonWidth);
        return button;
    }

    private static void applyResponsiveButtonSizing(
            ButtonComponent button, boolean compactLayout, Component label, int fixedWidth) {
        if (compactLayout) {
            button.horizontalSizing(Sizing.fill(100));
            return;
        }
        UiFactory.applyFixedButtonLabel(button, label, fixedWidth);
    }

    private static boolean isCompactLayout(SpecialDataPanelContext context) {
        return context.isCompactPanel(COMPACT_LAYOUT_WIDTH_THRESHOLD);
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

    private enum Field implements SpecialDataSearch.Field {
        FLIGHT_DURATION("special.firework.flight_duration"),
        ADD_EXPLOSION("special.firework.add_explosion");

        private final String key;

        Field(String key) {
            this.key = key;
        }

        public String key() {
            return key;
        }
    }

    private enum ExplosionField implements SpecialDataSearch.Field {
        SHAPE("special.firework.shape"),
        COLORS("special.firework.colors"),
        FADE_COLORS("special.firework.fade_colors"),
        TRAIL("special.firework.trail"),
        TWINKLE("special.firework.twinkle");

        private final String key;

        ExplosionField(String key) {
            this.key = key;
        }

        public String key() {
            return key;
        }
    }
}
