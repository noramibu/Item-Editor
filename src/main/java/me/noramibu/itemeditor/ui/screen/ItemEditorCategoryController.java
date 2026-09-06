package me.noramibu.itemeditor.ui.screen;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.core.Color;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.ParentUIComponent;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.UIComponent;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import me.noramibu.itemeditor.editor.EditorCategory;
import me.noramibu.itemeditor.editor.EditorModule;
import me.noramibu.itemeditor.ui.component.EditorSearchDialog;
import me.noramibu.itemeditor.ui.component.EditorSearchDialog.Target;
import me.noramibu.itemeditor.ui.component.UiFactory;
import me.noramibu.itemeditor.ui.panel.EditorPanel;
import me.noramibu.itemeditor.ui.util.ScrollStateUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

final class ItemEditorCategoryController {
    private static final int PANEL_SCROLL_STEP_BASE = 14;
    private static final int CATEGORY_HEADER_HORIZONTAL_RESERVE_BASE = 16;
    private static final int CATEGORY_HEADER_FALLBACK_MIN = 120;
    private static final int CATEGORY_BUTTON_VERTICAL_GAP_BASE = 5;
    private static final int TABS_MIN_WIDTH = 54;
    private static final int TABS_MAX_WIDTH = 140;
    private static final double CATEGORY_INSET_RATIO = 0.03d;
    private static final double TAB_WIDTH_RATIO = 0.058d;
    private static final int CATEGORY_TEXT_WIDTH_MIN = 18;
    private static final int ESTIMATED_BODY_GAP_BASE = 8;
    private static final float BUTTON_TEXT_SCALE_DEFAULT = 0.96F;

    private final ItemEditorScreen screen;
    private final List<EditorModule> modules;

    private FlowLayout tabs;
    private FlowLayout panelHost;
    private ScrollContainer<FlowLayout> panelScroll;
    private LabelComponent selectedCategoryLabel;
    private EditorSearchDialog.Location searchLocation;
    private int searchTicks;
    private int highlightTicks;
    private UIComponent highlighted;
    private Component highlightMessage;
    private Color highlightColor;

    ItemEditorCategoryController(ItemEditorScreen screen, List<EditorModule> modules) {
        this.screen = screen;
        this.modules = modules;
    }

    void bind(
            FlowLayout tabs,
            FlowLayout panelHost,
            ScrollContainer<FlowLayout> panelScroll,
            LabelComponent selectedCategoryLabel) {
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
        this.panelScroll.scrollStep(UiFactory.scaledScrollStep(PANEL_SCROLL_STEP_BASE));
        double scrollAmount = preserveScroll ? ScrollStateUtil.offset(this.panelScroll) : 0;
        Component fullCategoryTitle = this.screen.categoryTitle(selectedModule);
        this.selectedCategoryLabel.text(UiFactory.fitToWidth(fullCategoryTitle, this.categoryHeaderTextWidthHint()));
        this.selectedCategoryLabel.tooltip(List.of(fullCategoryTitle));
        this.panelHost.clearChildren();

        EditorPanel panel = selectedModule.panelFactory().apply(this.screen);
        UIComponent panelComponent = panel.build();
        UiFactory.appendFillChild(this.panelHost, panelComponent);
        this.screen.restorePanelScroll(scrollAmount);
        this.refreshTabs();
    }

    void refreshTabs() {
        if (this.tabs == null) return;

        EditorModule selected = this.screen.selectedModule();
        List<UIComponent> children = this.tabs.children();
        if (children.size() == this.modules.size() && children.stream().allMatch(ButtonComponent.class::isInstance)) {
            for (int index = 0; index < children.size(); index++) {
                ((ButtonComponent) children.get(index)).active(this.modules.get(index) != selected);
            }
            return;
        }

        this.tabs.clearChildren();
        this.tabs.gap(UiFactory.scaledPixels(CATEGORY_BUTTON_VERTICAL_GAP_BASE));
        int tabsWidth = this.currentTabsWidth();
        int horizontalInset = Math.max(1, (int) Math.round(tabsWidth * CATEGORY_INSET_RATIO));
        int buttonHeight = UiFactory.scaleProfile().controlHeight();
        for (EditorModule module : this.modules) {
            Component fullTitle = this.screen.categoryTitle(module);
            ButtonComponent button = UiFactory.scaledTextButton(
                    fullTitle,
                    BUTTON_TEXT_SCALE_DEFAULT,
                    UiFactory.ButtonTextPreset.STANDARD,
                    component -> this.switchModule(module));
            button.active(module != selected);
            button.tooltip(List.of(fullTitle));
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

    List<Target> searchTargets() {
        var results = new LinkedHashMap<List<String>, Target>();
        for (EditorModule module : this.modules) {
            Target category = new Target(
                    this.screen.categoryTitle(module).getString(),
                    module.category().name()
                            + switch (module.category()) {
                                case ATTRIBUTES -> " minecraft:attribute_modifiers";
                                case ENCHANTMENTS -> " minecraft:enchantments minecraft:stored_enchantments";
                                case BOOK -> " minecraft:written_book_content minecraft:writable_book_content";
                                default -> "";
                            },
                    () -> this.screen.revealSearchTarget(module.category(), ""));
            results.put(category.path(), category);
            module.panelFactory()
                    .apply(this.screen)
                    .searchTargets()
                    .forEach(target -> results.putIfAbsent(
                            target.path(),
                            guardSearchTarget(
                                    target,
                                    () -> this.prepareTransition(module),
                                    () -> module.panelFactory()
                                            .apply(this.screen)
                                            .searchTargets(),
                                    () -> this.switchModule(module))));
        }
        return List.copyOf(results.values());
    }

    static Target guardSearchTarget(
            Target target, BooleanSupplier guard, Supplier<List<Target>> currentTargets, Runnable missingTarget) {
        return new Target(target.path(), target.terms(), () -> {
            if (!guard.getAsBoolean()) return;
            currentTargets.get().stream()
                    .filter(current -> current.path().equals(target.path()))
                    .findFirst()
                    .ifPresentOrElse(current -> current.open().run(), missingTarget);
        });
    }

    private boolean prepareTransition(EditorModule module) {
        return module.category() == EditorCategory.RAW_EDITOR
                || this.screen.session().prepareStructuredTransition();
    }

    void revealSearchTarget(EditorCategory category, EditorSearchDialog.Location location) {
        this.modules.stream()
                .filter(module -> module.category() == category)
                .findFirst()
                .ifPresent(module -> {
                    boolean currentPage = this.screen.selectedModule() == module;
                    if (!currentPage && !this.switchModule(module)) return;
                    if (currentPage) this.clearHighlight();
                    this.searchLocation = location;
                    if (currentPage && !location.field().isEmpty()) {
                        UIComponent control = this.panelHost == null ? null : this.findLabel(this.panelHost);
                        if (control == null || control.height() <= 0) {
                            this.refreshCurrentPanel(true);
                            this.screen.requestResponsiveRelayout();
                        }
                    }
                    this.searchTicks = location.field().isEmpty() ? 0 : 20;
                });
    }

    void tickSearch() {
        if (this.highlightTicks > 0 && --this.highlightTicks == 0) this.clearHighlight();
        if (this.searchTicks > 0 && this.panelHost != null && this.screen.searchLayoutReady()) {
            this.searchTicks--;
            UIComponent control = this.findLabel(this.panelHost);
            if (control != null && control.height() > 0) {
                if (control.y() < this.panelScroll.y()
                        || control.y() + control.height() > this.panelScroll.y() + this.panelScroll.height()) {
                    this.screen.restorePanelScroll(Math.max(
                            0, ScrollStateUtil.offset(this.panelScroll) + control.y() - this.panelScroll.y() - 8));
                }
                this.highlighted = control;
                if (control instanceof LabelComponent label) {
                    this.highlightColor = label.color().get();
                    label.color(Color.ofRgb(0xFFFF55));
                } else if (control instanceof AbstractWidget widget) {
                    this.highlightMessage = widget.getMessage();
                    widget.setMessage(this.highlightMessage.copy().withStyle(ChatFormatting.YELLOW));
                }
                this.highlightTicks = 40;
                this.searchTicks = 0;
                this.searchLocation = null;
            }
        }
    }

    private UIComponent findLabel(UIComponent component) {
        if (!this.searchLocation.scope().isEmpty()) {
            component = findScope(component, this.searchLocation.scope());
            if (component == null) return null;
        }
        return this.findLabel(component, false);
    }

    private static UIComponent findScope(UIComponent component, String id) {
        if (id.equals(component.id())) return component;
        if (component instanceof ParentUIComponent parent) {
            for (UIComponent child : parent.children()) {
                UIComponent found = findScope(child, id);
                if (found != null) return found;
            }
        }
        return null;
    }

    private UIComponent findLabel(UIComponent component, boolean anchor) {
        String key = this.searchLocation.field();
        anchor |= key.equals(component.id());
        if (component instanceof LabelComponent label
                && (anchor
                        || label.text().getContents() instanceof TranslatableContents text
                                && text.getKey().equals(key))) return label;
        if (component instanceof AbstractWidget widget
                && (anchor
                        || widget.getMessage().getContents() instanceof TranslatableContents text
                                && text.getKey().equals(key))) return component;
        if (component instanceof ParentUIComponent parent) {
            for (UIComponent child : parent.children()) {
                UIComponent found = this.findLabel(child, anchor);
                if (found != null) return found;
            }
        }
        return null;
    }

    private void clearHighlight() {
        if (this.highlighted instanceof LabelComponent label) label.color(this.highlightColor);
        else if (this.highlighted instanceof AbstractWidget widget) widget.setMessage(this.highlightMessage);
        this.highlighted = null;
    }

    private boolean switchModule(EditorModule module) {
        if (!this.prepareTransition(module)) return false;
        this.clearHighlight();
        this.searchTicks = 0;
        this.screen.setSelectedModule(module);
        this.refreshCurrentPanel(false);
        this.screen.requestResponsiveRelayout();
        return true;
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

    private int categoryHeaderTextWidthHint() {
        int headerReserve = Math.max(8, UiFactory.scaledPixels(CATEGORY_HEADER_HORIZONTAL_RESERVE_BASE + 64));
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
}
