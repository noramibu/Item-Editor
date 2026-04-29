package me.noramibu.itemeditor.ui.screen;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.UIComponent;
import me.noramibu.itemeditor.editor.EditorCategory;
import me.noramibu.itemeditor.editor.EditorModule;
import me.noramibu.itemeditor.ui.component.UiFactory;
import me.noramibu.itemeditor.ui.panel.EditorPanel;
import me.noramibu.itemeditor.ui.util.ScrollStateUtil;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

final class ItemEditorCategoryController {
    private static final int PANEL_SCROLL_STEP_BASE = 14;
    private static final int CATEGORY_HEADER_HORIZONTAL_RESERVE_BASE = 16;
    private static final int CATEGORY_HEADER_FALLBACK_MIN = 120;
    private static final int CATEGORY_BUTTON_HORIZONTAL_INSET_BASE = 8;
    private static final int CATEGORY_BUTTON_VERTICAL_GAP_BASE = 5;
    private static final int TABS_MIN_WIDTH = 54;
    private static final int TABS_MAX_WIDTH = 140;
    private static final double CATEGORY_INSET_RATIO = 0.03d;
    private static final double TAB_WIDTH_RATIO = 0.058d;
    private static final int CATEGORY_TEXT_WIDTH_MIN = 18;
    private static final int ESTIMATED_BODY_GAP_BASE = 8;
    private static final double GUI_SCALE_THRESHOLD_HIGH = 3.0d;
    private static final double GUI_SCALE_THRESHOLD_VERY_HIGH = 4.0d;
    private static final double GUI_SCALE_THRESHOLD_EXTREME = 5.0d;
    private static final int BUTTON_TEXT_WIDTH_WIDE = 140;
    private static final int BUTTON_TEXT_WIDTH_MEDIUM = 120;
    private static final int BUTTON_TEXT_WIDTH_NARROW = 90;
    private static final float BUTTON_TEXT_SCALE_MIN = 0.70F;
    private static final float BUTTON_TEXT_SCALE_MAX = 1.04F;
    private static final float BUTTON_TEXT_SCALE_GS5 = 0.78F;
    private static final float BUTTON_TEXT_SCALE_GS4 = 0.82F;
    private static final float BUTTON_TEXT_SCALE_GS3 = 0.88F;
    private static final float BUTTON_TEXT_SCALE_DEFAULT = 0.96F;
    private static final float BUTTON_TEXT_SCALE_WIDE_BONUS = 0.06F;
    private static final float BUTTON_TEXT_SCALE_MEDIUM_BONUS = 0.03F;
    private static final float BUTTON_TEXT_SCALE_NARROW_PENALTY = 0.06F;

    private final ItemEditorScreen screen;
    private final List<EditorModule> modules;

    private FlowLayout tabs;
    private FlowLayout panelHost;
    private ScrollContainer<FlowLayout> panelScroll;
    private LabelComponent selectedCategoryLabel;

    ItemEditorCategoryController(ItemEditorScreen screen, List<EditorModule> modules) {
        this.screen = screen;
        this.modules = modules;
    }

    void bind(
            FlowLayout tabs,
            FlowLayout panelHost,
            ScrollContainer<FlowLayout> panelScroll,
            LabelComponent selectedCategoryLabel
    ) {
        this.tabs = tabs;
        this.panelHost = panelHost;
        this.panelScroll = panelScroll;
        this.selectedCategoryLabel = selectedCategoryLabel;
    }

    void refreshCurrentPanel(boolean preserveScroll) {
        if (this.panelHost == null || this.panelScroll == null || this.selectedCategoryLabel == null) {
            return;
        }

        EditorModule selectedModule = this.screen.selectedModule();
        boolean rawEditor = selectedModule.category() == EditorCategory.RAW_EDITOR;
        if (!rawEditor && this.screen.session().state().rawEditorEdited) {
            this.screen.session().state().rawEditorEdited = false;
            this.screen.session().rebuildPreview();
        }
        this.panelScroll.scrollStep(UiFactory.scaledScrollStep(PANEL_SCROLL_STEP_BASE));
        double scrollAmount = preserveScroll ? ScrollStateUtil.offset(this.panelScroll) : 0;
        Component fullCategoryTitle = this.screen.categoryTitle(selectedModule);
        this.selectedCategoryLabel.text(UiFactory.fitToWidth(fullCategoryTitle, this.categoryHeaderTextWidthHint()));
        this.selectedCategoryLabel.tooltip(this.categoryTooltip(fullCategoryTitle, this.screen.categoryDescription(selectedModule)));
        this.panelHost.clearChildren();

        EditorPanel panel = selectedModule.panelFactory().apply(this.screen);
        UIComponent panelComponent = panel.build();
        UiFactory.appendFillChild(this.panelHost, panelComponent);
        this.screen.restorePanelScroll(scrollAmount);
        this.refreshTabs();
    }

    void refreshTabs() {
        if (this.tabs == null) return;

        this.tabs.clearChildren();
        this.tabs.gap(UiFactory.scaledPixels(CATEGORY_BUTTON_VERTICAL_GAP_BASE));
        int tabsWidth = this.currentTabsWidth();
        double guiScale = net.minecraft.client.Minecraft.getInstance().getWindow().getGuiScale();
        int horizontalInset = Math.max(1, (int) Math.round(tabsWidth * CATEGORY_INSET_RATIO));
        int buttonTextWidth = this.categoryButtonTextWidthHint();
        float textScale = this.categoryButtonTextScale(buttonTextWidth, guiScale);
        int buttonHeight = UiFactory.scaleProfile().controlHeight();
        EditorModule selected = this.screen.selectedModule();
        for (EditorModule module : this.modules) {
            Component fullTitle = this.screen.categoryTitle(module);
            Component description = this.screen.categoryDescription(module);
            Component fittedTitle = UiFactory.fitToWidth(fullTitle, buttonTextWidth);
            ButtonComponent button = UiFactory.scaledTextButton(
                    fittedTitle,
                    textScale,
                    UiFactory.ButtonTextPreset.STANDARD,
                    component -> this.switchModule(module)
            );
            button.active(module != selected);
            button.tooltip(this.categoryTooltip(fullTitle, description));
            button.horizontalSizing(Sizing.fill(100));
            button.verticalSizing(Sizing.fixed(buttonHeight));
            button.margins(Insets.of(0, 0, horizontalInset, horizontalInset));
            this.tabs.child(button);
        }
    }

    void selectAdjacentCategory(int direction) {
        if (this.modules.isEmpty()) {
            return;
        }

        int currentIndex = this.modules.indexOf(this.screen.selectedModule());
        if (currentIndex < 0) {
            currentIndex = 0;
        }
        int nextIndex = Math.floorMod(currentIndex + direction, this.modules.size());
        this.switchModule(this.modules.get(nextIndex));
    }

    private void switchModule(EditorModule module) {
        this.screen.setSelectedModule(module);
        this.refreshCurrentPanel(false);
        this.screen.requestResponsiveRelayout();
    }

    boolean hasMeasuredResponsiveWidths() {
        if (this.panelScroll == null || this.panelScroll.width() <= 0) {
            return false;
        }
        if (this.screen.categoriesRailCollapsed()) {
            return true;
        }
        return this.tabs != null && this.tabs.width() > 0;
    }

    int measuredTabsWidth() {
        if (this.tabs != null && this.tabs.width() > 0) {
            return this.tabs.width();
        }
        return this.currentTabsWidth();
    }

    int measuredPanelWidth() {
        return this.panelScroll == null ? 0 : this.panelScroll.width();
    }

    private int categoryButtonTextWidthHint() {
        int tabsWidth = this.currentTabsWidth();
        int horizontalInsets = (UiFactory.scaleProfile().padding() * 2) + UiFactory.scaledPixels(CATEGORY_BUTTON_HORIZONTAL_INSET_BASE);
        return Math.max(CATEGORY_TEXT_WIDTH_MIN, tabsWidth - horizontalInsets);
    }

    private int categoryHeaderTextWidthHint() {
        int headerReserve = Math.max(8, UiFactory.scaledPixels(CATEGORY_HEADER_HORIZONTAL_RESERVE_BASE));
        int panelWidth = this.panelScroll == null ? 0 : this.panelScroll.width();
        if (panelWidth > 0) {
            return Math.max(CATEGORY_TEXT_WIDTH_MIN, panelWidth - headerReserve);
        }

        int panelHint = this.screen.editorContentWidthHint();
        if (panelHint > 0) {
            return Math.max(CATEGORY_TEXT_WIDTH_MIN, panelHint - headerReserve);
        }

        return Math.max(CATEGORY_HEADER_FALLBACK_MIN, this.screen.screenWidth() / 3);
    }

    private int currentTabsWidth() {
        if (this.tabs != null) {
            int tabsWidth = this.tabs.width();
            if (tabsWidth > 0) {
                return tabsWidth;
            }
        }
        int shellWidth = this.screen.estimatedShellWidth();
        int estimatedBodyGap = UiFactory.scaledPixels(ESTIMATED_BODY_GAP_BASE);
        int available = Math.max(1, shellWidth - (estimatedBodyGap * 2));
        int base = (int) Math.round(available * TAB_WIDTH_RATIO);
        int preferred = Math.clamp(base, TABS_MIN_WIDTH, TABS_MAX_WIDTH);
        return Math.min(available, preferred);
    }

    private float categoryButtonTextScale(int buttonTextWidth, double guiScale) {
        int scaleTier = guiScale >= GUI_SCALE_THRESHOLD_EXTREME ? 3
                : guiScale >= GUI_SCALE_THRESHOLD_VERY_HIGH ? 2
                : guiScale >= GUI_SCALE_THRESHOLD_HIGH ? 1
                : 0;
        float scale = switch (scaleTier) {
            case 3 -> BUTTON_TEXT_SCALE_GS5;
            case 2 -> BUTTON_TEXT_SCALE_GS4;
            case 1 -> BUTTON_TEXT_SCALE_GS3;
            default -> BUTTON_TEXT_SCALE_DEFAULT;
        };
        if (buttonTextWidth >= BUTTON_TEXT_WIDTH_WIDE) {
            scale += BUTTON_TEXT_SCALE_WIDE_BONUS;
        } else if (buttonTextWidth >= BUTTON_TEXT_WIDTH_MEDIUM) {
            scale += BUTTON_TEXT_SCALE_MEDIUM_BONUS;
        } else if (buttonTextWidth <= BUTTON_TEXT_WIDTH_NARROW) {
            scale -= BUTTON_TEXT_SCALE_NARROW_PENALTY;
        }
        return Math.clamp(scale, BUTTON_TEXT_SCALE_MIN, BUTTON_TEXT_SCALE_MAX);
    }

    private List<Component> categoryTooltip(Component title, Component description) {
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(title);
        if (!description.getString().isBlank()) {
            tooltip.add(description);
        }
        return tooltip;
    }

}
