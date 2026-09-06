package me.noramibu.itemeditor.ui.panel;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.UIComponent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import me.noramibu.itemeditor.editor.EditorCategory;
import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.ui.component.EditorSearchDialog;
import me.noramibu.itemeditor.ui.component.UiFactory;
import me.noramibu.itemeditor.ui.screen.ItemEditorScreen;
import me.noramibu.itemeditor.ui.util.LayoutModeUtil;
import me.noramibu.itemeditor.util.ItemEditorText;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class FlagsEditorPanel implements EditorPanel {
    private static final int FLAG_CHECKBOX_RESERVE = 42;
    private static final int COMPACT_LAYOUT_CONTENT_WIDTH_THRESHOLD = 560;
    private static final int OPTION_LABEL_WIDTH_MIN = 96;
    private static final int OPTION_COLUMN_WIDTH_MIN = 120;
    private static final int OPTION_COLUMN_RESERVE = 20;
    private static final int INLINE_CHECKBOX_SIZE_BASE = 18;
    private static final int INLINE_CHECKBOX_SIZE_MIN = 14;
    private static final int BOTTOM_PADDING_BASE = 12;
    private static final List<FlagOption> OPTIONS = allVanillaOptions();

    private final ItemEditorScreen screen;

    public FlagsEditorPanel(ItemEditorScreen screen) {
        this.screen = screen;
    }

    @Override
    public UIComponent build() {
        ItemEditorState state = this.screen.session().state();
        int contentWidth = Math.max(1, this.screen.editorContentWidthHint());
        boolean compactLayout = LayoutModeUtil.isCompactWidth(contentWidth, COMPACT_LAYOUT_CONTENT_WIDTH_THRESHOLD);
        FlowLayout root = UiFactory.column();
        root.padding(Insets.bottom(UiFactory.scaledPixels(BOTTOM_PADDING_BASE)));

        FlowLayout global = UiFactory.section(GlobalField.TITLE.label(), Component.empty());
        global.id("flags-global");
        global.child(UiFactory.checkbox(
                        GlobalField.HIDE.label(),
                        state.hideTooltip,
                        PanelBindings.toggle(this.screen, value -> state.hideTooltip = value))
                .horizontalSizing(Sizing.fill(100)));
        UiFactory.appendFillChild(root, global);

        FlowLayout common = UiFactory.section(HiddenField.TITLE.label(), Component.empty());
        common.id("flags-hidden");
        ButtonComponent selectAllButton = UiFactory.actionToneButton(
                HiddenField.SELECT.label(),
                UiFactory.ButtonTextPreset.STANDARD,
                UiFactory.ActionTone.POSITIVE,
                button -> PanelBindings.mutateRefresh(this.screen, () -> {
                    for (FlagOption option : OPTIONS) {
                        state.hiddenTooltipComponents.add(option.id());
                    }
                }));
        ButtonComponent deselectAllButton = UiFactory.actionToneButton(
                HiddenField.DESELECT.label(),
                UiFactory.ButtonTextPreset.STANDARD,
                UiFactory.ActionTone.NEGATIVE,
                button -> PanelBindings.mutateRefresh(this.screen, () -> {
                    for (FlagOption option : OPTIONS) {
                        state.hiddenTooltipComponents.remove(option.id());
                    }
                }));
        ButtonComponent revertAllButton = UiFactory.actionToneButton(
                HiddenField.REVERT.label(),
                UiFactory.ButtonTextPreset.STANDARD,
                UiFactory.ActionTone.PICKER,
                button -> PanelBindings.mutateRefresh(this.screen, () -> {
                    for (FlagOption option : OPTIONS) {
                        if (state.hiddenTooltipComponents.contains(option.id())) {
                            state.hiddenTooltipComponents.remove(option.id());
                        } else {
                            state.hiddenTooltipComponents.add(option.id());
                        }
                    }
                }));
        common.child(UiFactory.actionButtonRow(selectAllButton, deselectAllButton, revertAllButton));

        if (compactLayout) {
            FlowLayout optionsColumn = UiFactory.column();
            int labelWidth = this.clampLabelWidth(contentWidth);
            for (FlagOption option : OPTIONS) {
                optionsColumn.child(this.checkboxForOption(state, option, labelWidth));
            }
            common.child(optionsColumn);
        } else {
            FlowLayout optionsRow = UiFactory.row();
            FlowLayout left = UiFactory.column();
            FlowLayout right = UiFactory.column();
            left.horizontalSizing(Sizing.fill(49));
            right.horizontalSizing(Sizing.fill(49));

            int columnWidth = Math.max(
                    1,
                    Math.max(
                            OPTION_COLUMN_WIDTH_MIN,
                            (contentWidth - UiFactory.scaledPixels(OPTION_COLUMN_RESERVE)) / 2));
            int labelWidth = this.clampLabelWidth(columnWidth);
            for (int index = 0; index < OPTIONS.size(); index++) {
                UIComponent checkbox = this.checkboxForOption(state, OPTIONS.get(index), labelWidth);
                if ((index & 1) == 0) {
                    left.child(checkbox);
                } else {
                    right.child(checkbox);
                }
            }

            optionsRow.child(left);
            optionsRow.child(right);
            common.child(optionsRow);
        }

        UiFactory.appendFillChild(root, common);
        return root;
    }

    private int clampLabelWidth(int contentWidth) {
        int available = Math.max(1, contentWidth - UiFactory.scaledPixels(FLAG_CHECKBOX_RESERVE));
        int preferred = Math.max(OPTION_LABEL_WIDTH_MIN, available);
        return Math.clamp(preferred, 1, Math.max(1, contentWidth));
    }

    private UIComponent checkboxForOption(ItemEditorState state, FlagOption option, int labelWidth) {
        FlowLayout row = UiFactory.row();
        row.id(option.scope());

        var checkbox = UiFactory.checkbox(
                Component.empty(),
                state.hiddenTooltipComponents.contains(option.id()),
                value -> PanelBindings.mutate(this.screen, () -> {
                    if (value) {
                        state.hiddenTooltipComponents.add(option.id());
                    } else {
                        state.hiddenTooltipComponents.remove(option.id());
                    }
                }));
        int checkboxSize = Math.max(INLINE_CHECKBOX_SIZE_MIN, UiFactory.scaledPixels(INLINE_CHECKBOX_SIZE_BASE));
        checkbox.horizontalSizing(Sizing.fixed(checkboxSize));
        row.child(checkbox);

        Component fullText = option.label();
        Component fitted = UiFactory.fitToWidth(fullText, labelWidth);
        LabelComponent label = UiFactory.muted(fitted, labelWidth);
        label.id(option.scope() + ":label");
        label.horizontalSizing(Sizing.expand(100));
        if (!fitted.getString().equals(fullText.getString())) {
            label.tooltip(List.of(fullText));
        }
        row.child(label);
        return row;
    }

    @Override
    public List<EditorSearchDialog.Target> searchTargets() {
        List<EditorSearchDialog.Target> targets = new ArrayList<>();
        List<String> parents = PanelSearchDeclaration.parents(EditorCategory.FLAGS);
        for (GlobalField field : GlobalField.values()) {
            targets.add(field.target(
                    this.screen,
                    EditorCategory.FLAGS,
                    "flags-global",
                    field == GlobalField.TITLE
                            ? parents
                            : PanelSearchDeclaration.parents(EditorCategory.FLAGS, GlobalField.TITLE.label()),
                    "",
                    () -> {}));
        }
        for (HiddenField field : HiddenField.values()) {
            targets.add(field.target(
                    this.screen,
                    EditorCategory.FLAGS,
                    "flags-hidden",
                    field == HiddenField.TITLE
                            ? parents
                            : PanelSearchDeclaration.parents(EditorCategory.FLAGS, HiddenField.TITLE.label()),
                    "",
                    () -> {}));
        }
        targets.addAll(searchTargets(this.screen, EditorCategory.FLAGS.title().getString()));
        return List.copyOf(targets);
    }

    public static List<EditorSearchDialog.Target> searchTargets(ItemEditorScreen screen, String category) {
        return OPTIONS.stream()
                .map(option -> new EditorSearchDialog.Target(
                        List.of(
                                category,
                                HiddenField.TITLE.text(),
                                option.label().getString()),
                        option.id() + " " + option.fallback() + " "
                                + EditorSearchDialog.english(option.labelPath()) + " "
                                + EditorSearchDialog.english(HiddenField.TITLE.path()),
                        () -> screen.revealSearchTarget(
                                EditorCategory.FLAGS,
                                new EditorSearchDialog.Location(option.scope(), option.scope() + ":label"))))
                .toList();
    }

    private static List<FlagOption> allVanillaOptions() {
        return BuiltInRegistries.DATA_COMPONENT_TYPE.keySet().stream()
                .filter(identifier -> "minecraft".equals(identifier.getNamespace()))
                .sorted(Comparator.comparing(Identifier::getPath))
                .map(FlagsEditorPanel::fromComponentId)
                .toList();
    }

    private static FlagOption fromComponentId(Identifier componentId) {
        String path = componentId.getPath();
        String labelPath = "flags.hidden." + path;
        return new FlagOption(componentId.toString(), labelPath, "Hide " + humanize(path));
    }

    private static String humanize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String text = value.replace('_', ' ');
        return Character.toUpperCase(text.charAt(0)) + text.substring(1).toLowerCase(Locale.ROOT);
    }

    private record FlagOption(String id, String labelPath, String fallback) {
        String scope() {
            return "tooltip-option:" + this.id;
        }

        Component label() {
            String key = ItemEditorText.key(this.labelPath);
            return Language.getInstance().has(key) ? Component.translatable(key) : Component.literal(this.fallback);
        }
    }

    private enum GlobalField implements PanelSearchDeclaration {
        TITLE("flags.tooltip.title"),
        HIDE("flags.tooltip.hide_all");

        private final String path;

        GlobalField(String path) {
            this.path = path;
        }

        @Override
        public String path() {
            return this.path;
        }
    }

    private enum HiddenField implements PanelSearchDeclaration {
        TITLE("flags.hidden.title"),
        SELECT("common.select_all"),
        DESELECT("common.deselect_all"),
        REVERT("common.revert_all");

        private final String path;

        HiddenField(String path) {
            this.path = path;
        }

        @Override
        public String path() {
            return this.path;
        }
    }
}
