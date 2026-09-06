package me.noramibu.itemeditor.ui.panel;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.UIComponent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import me.noramibu.itemeditor.editor.EditorCategory;
import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.ui.component.EditorSearchDialog;
import me.noramibu.itemeditor.ui.component.UiFactory;
import me.noramibu.itemeditor.ui.screen.ItemEditorScreen;
import me.noramibu.itemeditor.ui.util.LayoutModeUtil;
import me.noramibu.itemeditor.util.ItemEditorText;
import me.noramibu.itemeditor.util.RegistryUtil;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.enchantment.Enchantment;

public final class EnchantmentEditorPanel implements EditorPanel {
    private static final int COMPACT_LAYOUT_CONTENT_WIDTH_THRESHOLD = 520;
    private static final int SUMMARY_WIDTH_MIN = 200;
    private static final int SUMMARY_WIDTH_RESERVE = 280;
    private static final int LEVEL_STACK_WIDTH_THRESHOLD = 620;
    private static final int LEVEL_FIELD_WIDTH_MIN = 96;
    private static final int LEVEL_FIELD_WIDTH_MAX = 156;
    private static final int LEVEL_FIELD_WIDTH_DIVISOR = 6;
    private static final int LEVEL_BUTTONS_WIDTH_MIN = 110;
    private static final int LEVEL_BUTTONS_WIDTH_BASE = 132;
    private static final int LEVEL_ROW_WIDTH_RESERVE = 140;

    private final ItemEditorScreen screen;

    public EnchantmentEditorPanel(ItemEditorScreen screen) {
        this.screen = screen;
    }

    @Override
    public List<EditorSearchDialog.Target> searchTargets() {
        ItemEditorState state = this.screen.session().state();
        List<EditorSearchDialog.Target> targets = new ArrayList<>();
        for (PanelField field : PanelField.values()) {
            targets.add(field.target(
                    this.screen,
                    EditorCategory.ENCHANTMENTS,
                    "enchantments-options",
                    PanelSearchDeclaration.parents(EditorCategory.ENCHANTMENTS),
                    "",
                    () -> {}));
        }
        for (EnchantmentList list : EnchantmentList.values()) {
            List<ItemEditorState.EnchantmentDraft> drafts = list.drafts(state);
            List<String> parents = PanelSearchDeclaration.parents(EditorCategory.ENCHANTMENTS, list.label());
            targets.add(list.target(
                    this.screen,
                    EditorCategory.ENCHANTMENTS,
                    list.scope(),
                    PanelSearchDeclaration.parents(EditorCategory.ENCHANTMENTS),
                    "",
                    () -> {}));
            for (ListField field : ListField.values()) {
                if (field != ListField.ADD && drafts.isEmpty()) {
                    continue;
                }
                targets.add(field.target(
                        this.screen,
                        EditorCategory.ENCHANTMENTS,
                        list.scope(),
                        parents,
                        EditorSearchDialog.english(list.path()),
                        () -> {}));
            }
            for (int index = 0; index < drafts.size(); index++) {
                ItemEditorState.EnchantmentDraft draft = drafts.get(index);
                List<String> entryParents = PanelSearchDeclaration.parents(
                        EditorCategory.ENCHANTMENTS, list.label(), list.entryLabel(index));
                targets.add(list.entry()
                        .target(
                                this.screen,
                                EditorCategory.ENCHANTMENTS,
                                list.entryScope(index),
                                parents,
                                this.enchantmentSummary(draft),
                                () -> draft.uiCollapsed = false,
                                index + 1));
                for (EntryField field : EntryField.values()) {
                    if (!field.applies(index, drafts.size())) {
                        continue;
                    }
                    targets.add(field.target(
                            this.screen,
                            EditorCategory.ENCHANTMENTS,
                            list.entryScope(index),
                            entryParents,
                            this.enchantmentSummary(draft) + " " + EditorSearchDialog.english(list.path()),
                            () -> draft.uiCollapsed = false));
                }
            }
        }
        return List.copyOf(targets);
    }

    private enum EnchantmentList implements PanelSearchDeclaration {
        REGULAR("enchantments.regular"),
        STORED("enchantments.stored");

        private final String prefix;

        EnchantmentList(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public String path() {
            return this.prefix + ".title";
        }

        PanelSearchDeclaration entry() {
            return () -> this.prefix + ".entry";
        }

        Component entryLabel(int index) {
            return entry().label(index + 1);
        }

        String scope() {
            return this.prefix;
        }

        String entryScope(int index) {
            return scope() + ":" + index;
        }

        List<ItemEditorState.EnchantmentDraft> drafts(ItemEditorState state) {
            return this == REGULAR ? state.enchantments : state.storedEnchantments;
        }
    }

    @Override
    public UIComponent build() {
        ItemEditorState state = this.screen.session().state();
        Registry<Enchantment> enchantmentRegistry =
                this.screen.session().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        List<String> enchantmentIds = RegistryUtil.ids(enchantmentRegistry);
        FlowLayout root = UiFactory.column();

        FlowLayout intro = UiFactory.section(PanelField.TITLE.label(), Component.empty());
        intro.id("enchantments-options");
        intro.child(UiFactory.checkbox(
                PanelField.UNSAFE.label(),
                state.unsafeEnchantments,
                PanelBindings.toggle(this.screen, value -> state.unsafeEnchantments = value)));
        UiFactory.appendFillChild(root, intro);

        for (EnchantmentList list : EnchantmentList.values()) {
            UiFactory.appendFillChild(root, this.buildListSection(list, list.drafts(state), enchantmentIds));
        }
        return root;
    }

    private FlowLayout buildListSection(
            EnchantmentList list, List<ItemEditorState.EnchantmentDraft> drafts, List<String> enchantmentIds) {
        int contentWidth = Math.max(1, this.screen.editorContentWidthHint());
        boolean compactLayout = LayoutModeUtil.isCompactWidth(contentWidth, COMPACT_LAYOUT_CONTENT_WIDTH_THRESHOLD);
        FlowLayout section = UiFactory.section(list.label(), Component.empty());

        section.id(list.scope());
        boolean hasEntries = !drafts.isEmpty();
        section.child(UiFactory.actionButtonRow(
                UiFactory.positiveButton(
                        ListField.ADD.label(),
                        UiFactory.ButtonTextPreset.STANDARD,
                        button -> PanelBindings.mutateRefresh(this.screen, () -> {
                            ItemEditorState.EnchantmentDraft draft = new ItemEditorState.EnchantmentDraft();
                            draft.uiCollapsed = false;
                            drafts.add(draft);
                        })),
                hasEntries
                        ? UiFactory.negativeButton(
                                ListField.CLEAR.label(),
                                UiFactory.ButtonTextPreset.STANDARD,
                                button -> PanelBindings.mutateRefresh(this.screen, drafts::clear))
                        : null));
        if (hasEntries) {
            Component expandText = ListField.EXPAND.label();
            ButtonComponent expandAll = UiFactory.button(
                    expandText,
                    UiFactory.ButtonTextPreset.STANDARD,
                    button -> PanelBindings.mutateRefresh(
                            this.screen, () -> drafts.forEach(entry -> entry.uiCollapsed = false)));
            expandAll.tooltip(List.of(expandText));

            Component collapseText = ListField.COLLAPSE.label();
            ButtonComponent collapseAll = UiFactory.button(
                    collapseText,
                    UiFactory.ButtonTextPreset.STANDARD,
                    button -> PanelBindings.mutateRefresh(
                            this.screen, () -> drafts.forEach(entry -> entry.uiCollapsed = true)));
            collapseAll.tooltip(List.of(collapseText));
            section.child(UiFactory.actionButtonRow(expandAll, collapseAll));
        }
        int summaryWidth = Math.clamp(
                Math.max(SUMMARY_WIDTH_MIN, contentWidth - UiFactory.scaledPixels(SUMMARY_WIDTH_RESERVE)),
                1,
                contentWidth);

        for (int index = 0; index < drafts.size(); index++) {
            int currentIndex = index;
            ItemEditorState.EnchantmentDraft draft = drafts.get(currentIndex);
            FlowLayout card = this.entryCard(list, drafts, currentIndex, summaryWidth);
            card.id(list.entryScope(index));

            if (draft.uiCollapsed) {
                section.child(card);
                continue;
            }

            ButtonComponent enchantmentButton = UiFactory.pickerButton(
                    draft.enchantmentId.isBlank()
                            ? ItemEditorText.tr("enchantments.select")
                            : Component.literal(draft.enchantmentId),
                    -1,
                    button -> this.screen.openSearchablePickerDialog(
                            EntryField.ENCHANTMENT.text(),
                            "",
                            enchantmentIds,
                            id -> id,
                            id -> PanelBindings.mutate(this.screen, () -> draft.enchantmentId = id)));

            card.child(UiFactory.field(EntryField.ENCHANTMENT.label(), Component.empty(), enchantmentButton));

            int levelFieldWidth =
                    Math.clamp(contentWidth / LEVEL_FIELD_WIDTH_DIVISOR, LEVEL_FIELD_WIDTH_MIN, LEVEL_FIELD_WIDTH_MAX);
            int levelButtonsWidth = Math.max(LEVEL_BUTTONS_WIDTH_MIN, UiFactory.scaledPixels(LEVEL_BUTTONS_WIDTH_BASE));
            int staticStackThreshold = UiFactory.scaledPixels(LEVEL_STACK_WIDTH_THRESHOLD);
            int dynamicStackThreshold =
                    levelFieldWidth + levelButtonsWidth + UiFactory.scaledPixels(LEVEL_ROW_WIDTH_RESERVE);
            boolean stackLevelControls =
                    compactLayout || contentWidth < Math.max(staticStackThreshold, dynamicStackThreshold);
            FlowLayout levelControls = stackLevelControls ? UiFactory.column() : UiFactory.row();
            levelControls.horizontalSizing(Sizing.fill(100));
            levelControls.child(UiFactory.textBox(
                            draft.level, PanelBindings.text(this.screen, value -> draft.level = value))
                    .horizontalSizing(stackLevelControls ? Sizing.fill(100) : UiFactory.fixed(levelFieldWidth)));

            FlowLayout levelButtons = UiFactory.row();
            levelButtons.horizontalSizing(stackLevelControls ? Sizing.fill(100) : Sizing.fixed(levelButtonsWidth));
            ButtonComponent minusLevel = UiFactory.negativeButton(
                    Component.literal("-"),
                    UiFactory.ButtonTextPreset.COMPACT,
                    button -> PanelBindings.mutateRefresh(
                            this.screen, () -> draft.level = Integer.toString(this.adjustLevel(draft.level, -1))));
            minusLevel.horizontalSizing(Sizing.fill(50));
            levelButtons.child(minusLevel);
            ButtonComponent plusLevel = UiFactory.positiveButton(
                    Component.literal("+"),
                    UiFactory.ButtonTextPreset.COMPACT,
                    button -> PanelBindings.mutateRefresh(
                            this.screen, () -> draft.level = Integer.toString(this.adjustLevel(draft.level, 1))));
            plusLevel.horizontalSizing(Sizing.fill(50));
            levelButtons.child(plusLevel);
            levelControls.child(levelButtons);
            card.child(UiFactory.field(EntryField.LEVEL.label(), Component.empty(), levelControls));
            section.child(card);
        }

        return section;
    }

    private FlowLayout entryCard(
            EnchantmentList list, List<ItemEditorState.EnchantmentDraft> drafts, int index, int summaryWidth) {
        ItemEditorState.EnchantmentDraft draft = drafts.get(index);
        FlowLayout card = UiFactory.subCard();
        FlowLayout header =
                UiFactory.column().gap(Math.max(1, UiFactory.scaleProfile().tightSpacing()));
        FlowLayout title = UiFactory.row();
        title.child(UiFactory.title(list.entryLabel(index)).shadow(false).horizontalSizing(Sizing.expand(100)));
        title.child(UiFactory.collapseToggleButton(
                draft.uiCollapsed,
                () -> PanelBindings.mutateRefresh(this.screen, () -> draft.uiCollapsed = !draft.uiCollapsed)));
        header.child(title);
        header.child(UiFactory.muted(Component.literal(this.enchantmentSummary(draft)), summaryWidth));
        header.child(UiFactory.actionButtonRow(
                this.entryAction(
                        EntryField.UP,
                        EntryField.UP.applies(index, drafts.size()),
                        () -> PanelBindings.mutateRefresh(
                                this.screen, () -> Collections.swap(drafts, index, index - 1))),
                this.entryAction(
                        EntryField.DOWN,
                        EntryField.DOWN.applies(index, drafts.size()),
                        () -> PanelBindings.mutateRefresh(
                                this.screen, () -> Collections.swap(drafts, index, index + 1))),
                this.entryAction(
                        EntryField.REMOVE,
                        true,
                        () -> PanelBindings.mutateRefresh(this.screen, () -> drafts.remove(index)))));
        card.child(header);
        return card;
    }

    private ButtonComponent entryAction(EntryField field, boolean enabled, Runnable action) {
        boolean remove = field == EntryField.REMOVE;
        ButtonComponent button = UiFactory.actionToneButton(
                field.label(),
                remove ? UiFactory.ButtonTextPreset.STANDARD : UiFactory.ButtonTextPreset.COMPACT,
                remove ? UiFactory.ActionTone.NEGATIVE : UiFactory.ActionTone.NEUTRAL,
                ignored -> action.run());
        button.active(enabled);
        return button;
    }

    private String enchantmentSummary(ItemEditorState.EnchantmentDraft draft) {
        String id = draft.enchantmentId == null || draft.enchantmentId.isBlank() ? "-" : draft.enchantmentId;
        String level = draft.level == null || draft.level.isBlank() ? "1" : draft.level;
        return id + " (" + level + ")";
    }

    private int adjustLevel(String raw, int delta) {
        int value;
        try {
            value = Integer.parseInt(raw == null || raw.isBlank() ? "1" : raw.trim());
        } catch (NumberFormatException ignored) {
            value = 1;
        }
        value += delta;
        return Math.max(1, value);
    }

    private enum PanelField implements PanelSearchDeclaration {
        TITLE("enchantments.title"),
        UNSAFE("enchantments.unsafe");

        private final String path;

        PanelField(String path) {
            this.path = path;
        }

        @Override
        public String path() {
            return this.path;
        }
    }

    private enum ListField implements PanelSearchDeclaration {
        ADD("enchantments.add"),
        CLEAR("common.clear_all"),
        EXPAND("common.expand_all"),
        COLLAPSE("common.collapse_all");

        private final String path;

        ListField(String path) {
            this.path = path;
        }

        @Override
        public String path() {
            return this.path;
        }
    }

    private enum EntryField implements PanelSearchDeclaration {
        ENCHANTMENT("enchantments.entry.enchantment"),
        LEVEL("enchantments.entry.level"),
        UP("common.up"),
        DOWN("common.down"),
        REMOVE("common.remove");

        private final String path;

        EntryField(String path) {
            this.path = path;
        }

        boolean applies(int index, int size) {
            return switch (this) {
                case UP -> index > 0;
                case DOWN -> index < size - 1;
                default -> true;
            };
        }

        @Override
        public String path() {
            return this.path;
        }
    }
}
