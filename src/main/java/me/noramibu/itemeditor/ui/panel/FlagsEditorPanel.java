package me.noramibu.itemeditor.ui.panel;

import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.UIComponent;
import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.ui.component.UiFactory;
import me.noramibu.itemeditor.ui.screen.ItemEditorScreen;
import me.noramibu.itemeditor.util.ItemEditorText;
import net.minecraft.client.Minecraft;
import net.minecraft.locale.Language;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class FlagsEditorPanel implements EditorPanel {
    private static final int FLAG_CHECKBOX_RESERVE = 42;
    private static final int COMPACT_LAYOUT_WIDTH_THRESHOLD = 1150;
    private static final int COMPACT_LAYOUT_CONTENT_WIDTH_THRESHOLD = 560;
    private static final double COMPACT_LAYOUT_SCALE_THRESHOLD = 3.0d;
    private static final int OPTION_LABEL_WIDTH_MIN = 96;
    private static final int OPTION_COLUMN_WIDTH_MIN = 120;
    private static final int OPTION_COLUMN_RESERVE = 20;
    private static final int INLINE_CHECKBOX_SIZE_BASE = 18;
    private static final int INLINE_CHECKBOX_SIZE_MIN = 14;
    private static final String HIDE_ALL_LISTED_TEXT = "Hide all listed";
    private static final String SHOW_ALL_LISTED_TEXT = "Show all listed";

    private static final List<FlagOption> OPTIONS = allVanillaOptions();

    private final ItemEditorScreen screen;

    public FlagsEditorPanel(ItemEditorScreen screen) {
        this.screen = screen;
    }

    @Override
    public UIComponent build() {
        ItemEditorState state = this.screen.session().state();
        int contentWidth = Math.max(1, this.screen.editorContentWidthHint());
        boolean compactLayout = this.useCompactLayout(contentWidth);
        FlowLayout root = UiFactory.column();

        FlowLayout global = UiFactory.section(ItemEditorText.tr("flags.tooltip.title"), Component.empty());
        global.child(UiFactory.checkbox(
                ItemEditorText.tr("flags.tooltip.hide_all"),
                state.hideTooltip,
                PanelBindings.toggle(this.screen, value -> state.hideTooltip = value)
        ).horizontalSizing(Sizing.fill(100)));
        UiFactory.appendFillChild(root, global);

        FlowLayout common = UiFactory.section(ItemEditorText.tr("flags.hidden.title"), Component.empty());
        FlowLayout actions = compactLayout ? UiFactory.column() : UiFactory.row();
        UIComponent hideAllButton = UiFactory.button(Component.literal(HIDE_ALL_LISTED_TEXT), UiFactory.ButtonTextPreset.STANDARD,  button ->
                PanelBindings.mutateRefresh(this.screen, () -> {
                    for (FlagOption option : OPTIONS) {
                        state.hiddenTooltipComponents.add(option.id());
                    }
                })
        );
        UIComponent showAllButton = UiFactory.button(Component.literal(SHOW_ALL_LISTED_TEXT), UiFactory.ButtonTextPreset.STANDARD,  button ->
                PanelBindings.mutateRefresh(this.screen, () -> {
                    for (FlagOption option : OPTIONS) {
                        state.hiddenTooltipComponents.remove(option.id());
                    }
                })
        );
        hideAllButton.horizontalSizing(compactLayout ? Sizing.fill(100) : Sizing.expand(100));
        showAllButton.horizontalSizing(compactLayout ? Sizing.fill(100) : Sizing.expand(100));
        actions.child(hideAllButton);
        actions.child(showAllButton);
        common.child(actions);

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
            left.horizontalSizing(Sizing.expand(100));
            right.horizontalSizing(Sizing.expand(100));

            int columnWidth = Math.max(1, Math.max(OPTION_COLUMN_WIDTH_MIN, (contentWidth - UiFactory.scaledPixels(OPTION_COLUMN_RESERVE)) / 2));
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

    private boolean useCompactLayout(int contentWidth) {
        var window = Minecraft.getInstance().getWindow();
        int scaledWidth = window.getGuiScaledWidth();
        double guiScale = window.getGuiScale();
        return scaledWidth < COMPACT_LAYOUT_WIDTH_THRESHOLD
                || contentWidth < COMPACT_LAYOUT_CONTENT_WIDTH_THRESHOLD
                || guiScale >= COMPACT_LAYOUT_SCALE_THRESHOLD;
    }

    private int clampLabelWidth(int contentWidth) {
        int available = Math.max(1, contentWidth - UiFactory.scaledPixels(FLAG_CHECKBOX_RESERVE));
        int preferred = Math.max(OPTION_LABEL_WIDTH_MIN, available);
        return Math.max(1, Math.min(contentWidth, preferred));
    }

    private UIComponent checkboxForOption(ItemEditorState state, FlagOption option, int labelWidth) {
        FlowLayout row = UiFactory.row();

        var checkbox = UiFactory.checkbox(Component.empty(), state.hiddenTooltipComponents.contains(option.id()), value ->
                PanelBindings.mutate(this.screen, () -> {
                    if (value) {
                        state.hiddenTooltipComponents.add(option.id());
                    } else {
                        state.hiddenTooltipComponents.remove(option.id());
                    }
                })
        );
        int checkboxSize = Math.max(INLINE_CHECKBOX_SIZE_MIN, UiFactory.scaledPixels(INLINE_CHECKBOX_SIZE_BASE));
        checkbox.horizontalSizing(Sizing.fixed(checkboxSize));
        row.child(checkbox);

        Component fullText = this.optionLabel(option);
        Component fitted = UiFactory.fitToWidth(fullText, labelWidth);
        LabelComponent label = UiFactory.muted(fitted, labelWidth);
        label.horizontalSizing(Sizing.expand(100));
        if (!fitted.getString().equals(fullText.getString())) {
            label.tooltip(List.of(fullText));
        }
        row.child(label);
        return row;
    }

    private Component optionLabel(FlagOption option) {
        if (option.labelPath() != null) {
            String key = ItemEditorText.key(option.labelPath());
            if (Language.getInstance().has(key)) {
                return Component.translatable(key);
            }
        }
        return Component.literal(option.fallback());
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
    }
}
