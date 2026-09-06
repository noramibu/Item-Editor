package me.noramibu.itemeditor.ui.panel.specialdata;

import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Sizing;
import java.util.ArrayList;
import java.util.List;
import me.noramibu.itemeditor.editor.EditorCategory;
import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.ui.component.EditorSearchDialog;
import me.noramibu.itemeditor.ui.component.UiFactory;
import me.noramibu.itemeditor.util.ItemEditorCapabilities;
import me.noramibu.itemeditor.util.ItemEditorText;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public final class ItemFrameSpecialDataSection {
    public static List<EditorSearchDialog.Target> searchTargets(SpecialDataPanelContext context) {
        var fields = new ArrayList<SpecialDataSearch.Field>();
        fields.addAll(FLAGS);
        fields.addAll(VALUES);
        var result = new ArrayList<>(SpecialDataSearch.targets(
                context,
                EditorCategory.SPECIAL_DATA,
                "special.item_frame.title",
                "item-frame",
                () -> {},
                fields.toArray(SpecialDataSearch.Field[]::new)));
        result.addAll(SpecialDataSearch.targets(
                context,
                EditorCategory.SPECIAL_DATA,
                "special.item_frame.title",
                "item-frame",
                () -> {},
                ItemField.values()));
        result.addAll(SpecialDataSearch.targets(
                context,
                EditorCategory.SPECIAL_DATA,
                "special.item_frame.title",
                "item-frame",
                () -> {},
                SpecialDataPanelContext.NameField.values()));
        result.addAll(context.itemActionSearchTargets(
                EditorCategory.SPECIAL_DATA,
                List.of(
                        ItemEditorText.str("special.item_frame.title"),
                        ItemField.TITLE.text().getString()),
                "item-frame",
                () -> {},
                context.special().itemFrameItem,
                true));
        return result;
    }

    private static final List<EntityEditorField<ItemEditorState.SpecialData, Boolean>> FLAGS = List.of(
            new EntityEditorField<>(
                    "special.entity.flags",
                    "special.entity.invisible",
                    d -> d.itemFrameInvisible,
                    (d, v) -> d.itemFrameInvisible = v),
            new EntityEditorField<>(
                    "special.entity.flags",
                    "special.item_frame.fixed",
                    d -> d.itemFrameFixed,
                    (d, v) -> d.itemFrameFixed = v),
            new EntityEditorField<>(
                    "special.entity.flags",
                    "special.entity.no_gravity",
                    d -> d.itemFrameNoGravity,
                    (d, v) -> d.itemFrameNoGravity = v),
            new EntityEditorField<>(
                    "special.entity.flags",
                    "special.entity.invulnerable",
                    d -> d.itemFrameInvulnerable,
                    (d, v) -> d.itemFrameInvulnerable = v),
            new EntityEditorField<>(
                    "special.entity.flags",
                    "special.entity.name_visible",
                    d -> d.itemFrameCustomNameVisible,
                    (d, v) -> d.itemFrameCustomNameVisible = v));
    private static final List<EntityEditorField<ItemEditorState.SpecialData, String>> VALUES = List.of(
            new EntityEditorField<>(
                    "special.entity.values",
                    "special.item_frame.item_rotation",
                    d -> d.itemFrameRotation,
                    (d, v) -> d.itemFrameRotation = v),
            new EntityEditorField<>(
                    "special.entity.values",
                    "special.item_frame.item_drop_chance",
                    d -> d.itemFrameDropChance,
                    (d, v) -> d.itemFrameDropChance = v),
            new EntityEditorField<>(
                    "special.entity.values",
                    "special.item_frame.facing",
                    d -> d.itemFrameFacing,
                    (d, v) -> d.itemFrameFacing = v));

    private static final int COMPACT_LAYOUT_WIDTH_THRESHOLD = 560;

    private ItemFrameSpecialDataSection() {}

    public static boolean supports(ItemStack stack) {
        return ItemEditorCapabilities.supportsItemFrameData(stack);
    }

    public static FlowLayout build(SpecialDataPanelContext context) {
        ItemEditorState.SpecialData special = context.special();
        FlowLayout section = UiFactory.section(ItemEditorText.tr("special.item_frame.title"), Component.empty());
        section.id("item-frame");
        section.child(buildNameCard(context, special));
        section.child(buildItem(context, special));
        section.child(buildFlagsCard(context, special));
        section.child(buildValuesCard(context, special));
        return section;
    }

    private static FlowLayout buildItem(SpecialDataPanelContext context, ItemEditorState.SpecialData special) {
        ItemStack item = special.itemFrameItem;
        return UiFactory.column()
                .gap(2)
                .child(UiFactory.title(ItemField.TITLE.text()).shadow(false))
                .child(context.itemSummary(
                        item, item.isEmpty() ? ItemEditorText.tr("common.none") : item.getHoverName(), true))
                .child(context.itemActions(item, stack -> special.itemFrameItem = stack, () -> context.screen()
                        .openNestedEditor(
                                item,
                                null,
                                edited -> context.mutateRefresh(() -> special.itemFrameItem = edited.copy()))));
    }

    private static FlowLayout buildNameCard(SpecialDataPanelContext context, ItemEditorState.SpecialData special) {
        return context.entityNameEditor(
                special.itemFrameCustomName, "special.item_frame.name", value -> special.itemFrameCustomName = value);
    }

    private static FlowLayout buildFlagsCard(SpecialDataPanelContext context, ItemEditorState.SpecialData special) {
        boolean compactLayout = isCompactLayout(context);
        FlowLayout card = UiFactory.subCard();
        card.child(UiFactory.title(ItemEditorText.tr("special.entity.flags")).shadow(false));

        FlowLayout rowA = compactLayout ? UiFactory.column() : UiFactory.row();
        FLAGS.subList(0, 3).forEach(field -> rowA.child(EntityEditorField.checkbox(field, special, context)));
        card.child(rowA);
        FlowLayout rowB = compactLayout ? UiFactory.column() : UiFactory.row();
        FLAGS.subList(3, FLAGS.size())
                .forEach(field -> rowB.child(EntityEditorField.checkbox(field, special, context)));
        card.child(rowB);
        return card;
    }

    private static FlowLayout buildValuesCard(SpecialDataPanelContext context, ItemEditorState.SpecialData special) {
        boolean compactLayout = isCompactLayout(context);
        FlowLayout card = UiFactory.subCard();
        card.child(UiFactory.title(ItemEditorText.tr("special.entity.values")).shadow(false));

        FlowLayout row = compactLayout ? UiFactory.column() : UiFactory.row();
        VALUES.forEach(field -> row.child(EntityEditorField.text(field, special, context)
                .horizontalSizing(compactLayout ? Sizing.fill(100) : Sizing.expand(100))));
        card.child(row);
        return card;
    }

    private static boolean isCompactLayout(SpecialDataPanelContext context) {
        return context.isCompactPanel(COMPACT_LAYOUT_WIDTH_THRESHOLD);
    }

    private enum ItemField implements SpecialDataSearch.Field {
        TITLE("special.entity.item.title");

        private final String key;

        ItemField(String key) {
            this.key = key;
        }

        public String key() {
            return key;
        }
    }
}
