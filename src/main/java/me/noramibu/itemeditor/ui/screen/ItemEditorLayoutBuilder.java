package me.noramibu.itemeditor.ui.screen;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.ItemComponent;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.component.UIComponents;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.core.HorizontalAlignment;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.UIComponent;
import io.wispforest.owo.ui.core.VerticalAlignment;
import me.noramibu.itemeditor.ui.component.InputSafeScrollContainer;
import me.noramibu.itemeditor.ui.component.UiFactory;
import me.noramibu.itemeditor.util.ItemEditorText;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Consumer;

final class ItemEditorLayoutBuilder {

    private static final int SHELL_MAX_WIDTH = 1600;
    private static final int SHELL_SIDE_PADDING = 8;
    private static final int RAIL_TOGGLE_SIZE = 18;
    private static final int RAIL_MAX_WIDTH = 210;
    private static final int BODY_GAP = 8;
    private static final float PREVIEW_PANEL_TITLE_SCALE = 1.00F;
    private static final float PREVIEW_SECTION_TITLE_SCALE = 1.00F;
    private static final float PREVIEW_SECTION_TOGGLE_SCALE = 1.00F;
    private static final float PREVIEW_NAME_SCALE = 1.10F;
    private static final double PREVIEW_VALIDATION_RATIO = 0.58d;
    private static final int TOP_BAR_MIN_TEXT_WIDTH = 120;
    private static final int TOP_ACTION_BUTTON_MIN_WIDTH = 46;
    private static final int TOP_ACTION_BUTTON_MAX_WIDTH = 116;
    private static final int TOP_ACTION_BUTTON_MAX_WIDTH_COMPACT = 98;
    private static final int TOP_ACTION_BUTTON_ABSOLUTE_MIN_WIDTH = 36;
    private static final int TOP_ACTION_BUTTON_LABEL_PADDING_MIN = 10;
    private static final int TOP_ACTION_BUTTON_LABEL_PADDING_BASE = 14;
    private static final int TOP_ACTION_BUTTON_TEXT_PADDING_COMPACT = 14;
    private static final int TOP_ACTION_BUTTON_TEXT_PADDING_REGULAR = 18;
    private static final int TOP_ACTION_BUTTON_COMPACT_MIN_WIDTH = 34;
    private static final int TOP_ACTION_BUTTON_ADAPTIVE_MIN_FALLBACK = 42;
    private static final int TOP_ACTION_BUTTON_COMPACT_TARGET_MIN = 44;
    private static final int TOP_ACTION_BUTTON_COMPACT_TARGET_MAX = 96;
    private static final int TOP_ACTION_BUTTON_REGULAR_TARGET_MIN = 56;
    private static final int TOP_ACTION_BUTTON_REGULAR_TARGET_MAX = 124;
    private static final int TOP_ACTION_COUNT = 5;
    private static final int SHELL_VERTICAL_SAFE_PADDING = 8;
    private static final int PANEL_SCROLLBAR_THICKNESS = 8;
    private static final int PREVIEW_SCROLLBAR_THICKNESS = 7;
    private static final int TABS_SCROLL_STEP_BASE = 12;
    private static final int PANEL_SCROLL_STEP_BASE = 14;
    private static final int PREVIEW_TOOLTIP_SCROLL_STEP_BASE = 12;
    private static final int PREVIEW_VALIDATION_SCROLL_STEP_BASE = 10;
    private static final int PREVIEW_TOOLTIP_CONTENT_INSET_BASE = 12;
    private static final int PREVIEW_VALIDATION_CONTENT_INSET_BASE = 10;
    private static final int PREVIEW_SECTION_COLLAPSED_MIN_HEIGHT = 44;
    private static final int PREVIEW_SECTION_COLLAPSED_CONTROL_RESERVE = 16;
    private static final int PREVIEW_SECTION_HEADER_MIN_HEIGHT = 20;
    private static final int PREVIEW_SECTION_FIT_HARD_MIN_HEIGHT = 24;
    private static final int PREVIEW_SECTION_COLLAPSED_FIT_MIN_HEIGHT = 28;
    private static final int PREVIEW_SECTION_SIDE_TOGGLE_BASE = 24;
    private static final int PREVIEW_SECTION_HEADER_GAP_BASE = 10;
    private static final int PREVIEW_LEAD_NAME_GAP_BASE = 6;
    private static final int PREVIEW_LEAD_ICON_SLOT_BASE = 18;
    private static final int PREVIEW_LEAD_COLLAPSE_TOGGLE_BASE = 22;
    private static final int TABS_HEADER_GAP_BASE = 6;
    private static final int TEXT_WIDTH_MIN = 24;
    private static final int APPLY_MODE_TEXT_WIDTH_HINT_DEFAULT = 220;
    private static final int PREVIEW_TEXT_WIDTH_HINT_DEFAULT = 210;
    private static final int TOP_BAR_COMPACT_WIDTH_THRESHOLD = 680;
    private static final int TOP_TEXT_GROUP_DYNAMIC_MIN = 92;
    private static final double TOP_ACTION_BUTTON_TARGET_RATIO_COMPACT = 0.10d;
    private static final double TOP_ACTION_BUTTON_TARGET_RATIO_REGULAR = 0.11d;
    private static final double TOP_BAR_TITLE_RATIO_COMPACT = 0.28d;
    private static final double TOP_BAR_TITLE_RATIO_REGULAR = 0.24d;
    private static final double GUI_SCALE_HIGH_THRESHOLD = 3.0d;
    private static final double GUI_SCALE_VERY_HIGH_THRESHOLD = 4.0d;
    private static final int SHELL_BOTTOM_SAFETY_EXTRA_HIGH = 4;
    private static final int SHELL_BOTTOM_SAFETY_EXTRA_VERY_HIGH = 8;
    private static final double TOP_BAR_HEIGHT_MAX_RATIO = 0.18d;
    private static final double PREVIEW_SCALE_NORMAL = 0.70d;
    private static final double PREVIEW_SCALE_HIGH = 0.65d * 0.95d;
    private static final double TABS_SCALE_NORMAL = 1.0d;
    private static final double TABS_SCALE_HIGH = 0.95d;
    private static final double TABS_SCALE_VERY_HIGH = 0.92d;
    private static final int WIDE_BODY_GAP_MIN = 4;
    private static final double WIDE_TABS_MIN_RATIO = 0.06d;
    private static final int WIDE_TABS_MIN_FALLBACK = 64;
    private static final int WIDE_TABS_MAX_EXTRA = 6;
    private static final double WIDE_TABS_MAX_RATIO = 0.22d;
    private static final double WIDE_TABS_TARGET_RATIO = 0.11d;
    private static final int WIDE_TABS_TARGET_FALLBACK = 104;
    private static final double WIDE_EDITOR_MIN_RATIO = 0.36d;
    private static final int WIDE_EDITOR_MIN_FALLBACK = 220;
    private static final int WIDE_PREVIEW_MIN_WIDTH = 100;
    private static final double WIDE_PREVIEW_MIN_RATIO = 0.16d;
    private static final int WIDE_PREVIEW_MIN_FALLBACK = 150;
    private static final int WIDE_PREVIEW_MAX_EXTRA = 8;
    private static final double WIDE_PREVIEW_MAX_RATIO = 0.31d;
    private static final int WIDE_PREVIEW_MAX_FALLBACK = 370;
    private static final double WIDE_PREVIEW_TARGET_RATIO = 0.22d;
    private static final int WIDE_PREVIEW_TARGET_FALLBACK = 208;
    private static final int WIDE_PREVIEW_HARD_FLOOR_MIN = 90;
    private static final double WIDE_PREVIEW_HARD_FLOOR_RATIO = 0.14d;
    private static final int WIDE_PREVIEW_HARD_FLOOR_FALLBACK = 130;
    private static final double WIDE_TABS_HARD_MIN_RATIO = 0.03d;
    private static final int WIDE_TABS_HARD_MIN_FALLBACK = 48;
    private static final int WIDE_ABSOLUTE_EDITOR_FLOOR = 140;
    private static final String SYMBOL_LEFT = "<";
    private static final String SYMBOL_RIGHT = ">";
    private static final String SYMBOL_PLUS = "+";
    private static final String SYMBOL_MINUS = "-";
    private static final String SYMBOL_SECTION_EXPANDED = "[-]";
    private static final String SYMBOL_SECTION_COLLAPSED = "[+]";
    private static final String TOOLTIP_HIDE_CATEGORIES = "Hide categories";
    private static final String TOOLTIP_SHOW_CATEGORIES = "Show categories";
    private static final String TOOLTIP_HIDE_PREVIEW = "Hide preview";
    private static final String TOOLTIP_SHOW_PREVIEW = "Show preview";

    private final ItemEditorScreen screen;
    private int shellWidth;
    private int previewTextWidthHint = PREVIEW_TEXT_WIDTH_HINT_DEFAULT;
    private FlowLayout tabs;
    private FlowLayout panelHost;
    private ScrollContainer<FlowLayout> panelScroll;
    private ScrollContainer<FlowLayout> tooltipScroll;
    private ScrollContainer<FlowLayout> messageScroll;
    private FlowLayout tooltipLines;
    private FlowLayout messages;
    private LabelComponent selectedCategoryLabel;
    private LabelComponent applyModeLabel;
    private int applyModeTextWidthHint = APPLY_MODE_TEXT_WIDTH_HINT_DEFAULT;
    private LabelComponent previewNameLabel;
    private ItemComponent previewItem;
    private ButtonComponent applyButton;
    private ButtonComponent resetButton;
    private int previewCardHeightHint;

    ItemEditorLayoutBuilder(ItemEditorScreen screen) {
        this.screen = screen;
    }

    BuildResult build() {
        UIComponent shell = this.buildShell();
        return new BuildResult(
                shell,
                this.tabs,
                this.panelHost,
                this.panelScroll,
                this.tooltipScroll,
                this.messageScroll,
                this.tooltipLines,
                this.messages,
                this.selectedCategoryLabel,
                this.applyModeLabel,
                this.applyModeTextWidthHint,
                this.previewNameLabel,
                this.previewItem,
                this.applyButton,
                this.resetButton,
                this.previewTextWidthHint
        );
    }

    private UIComponent buildShell() {
        double guiScale = this.currentGuiScale();
        boolean fullViewportShell = guiScale >= GUI_SCALE_HIGH_THRESHOLD;
        this.shellWidth = fullViewportShell
                ? Math.max(1, this.screen.screenWidth())
                : Math.max(1, Math.min(SHELL_MAX_WIDTH, this.screen.screenWidth() - (SHELL_SIDE_PADDING * 2)));
        int outerPadding = fullViewportShell ? 0 : UiFactory.scaledPixels(SHELL_SIDE_PADDING / 2);
        int verticalPadding = fullViewportShell ? 0 : this.scaledMin(2, 2);
        int bottomSafetyExtra = guiScale >= GUI_SCALE_VERY_HIGH_THRESHOLD
                ? SHELL_BOTTOM_SAFETY_EXTRA_VERY_HIGH
                : (guiScale >= GUI_SCALE_HIGH_THRESHOLD ? SHELL_BOTTOM_SAFETY_EXTRA_HIGH : 0);
        int bottomSafety = fullViewportShell
                ? 0
                : Math.max(
                2,
                UiFactory.scaledPixels(SHELL_VERTICAL_SAFE_PADDING + bottomSafetyExtra)
        );
        int shellGap = UiFactory.scaleProfile().spacing();
        int availableShellHeight = Math.max(
                1,
                this.screen.screenHeight() - ((verticalPadding * 2) + bottomSafety)
        );
        FlowLayout shell = UiFactory.column();
        shell.horizontalSizing(Sizing.fixed(this.shellWidth));
        shell.verticalSizing(Sizing.fixed(availableShellHeight));

        FlowLayout topBar = this.buildTopBar();
        int estimatedTopBarHeight = Math.max(
                UiFactory.scaledPixels(28),
                UiFactory.scaleProfile().controlHeight() + (UiFactory.scaleProfile().padding() * 2) + UiFactory.scaledPixels(8)
        );
        int minimumBodyHeight = 1;
        int topBarHeightCandidate = Math.min(
                estimatedTopBarHeight,
                Math.max(UiFactory.scaledPixels(PREVIEW_SECTION_FIT_HARD_MIN_HEIGHT), (int) Math.round(availableShellHeight * TOP_BAR_HEIGHT_MAX_RATIO))
        );
        int topBarMaxHeight = Math.max(1, availableShellHeight - shellGap - minimumBodyHeight);
        int topBarHeight = Math.min(topBarHeightCandidate, topBarMaxHeight);
        topBar.verticalSizing(Sizing.fixed(topBarHeight));
        shell.child(topBar);
        int bodyHeight = Math.max(minimumBodyHeight, availableShellHeight - topBarHeight - shellGap);
        int bodyBottomPadding = fullViewportShell ? 0 : this.scaledMin(2, 3);

        boolean categoriesCollapsed = this.screen.categoriesRailCollapsed();
        boolean previewCollapsed = this.screen.previewRailCollapsed();
        int railToggleWidth = this.railToggleSize();
        WideLayoutMetrics metrics = this.wideLayoutMetrics(this.shellWidth, categoriesCollapsed, railToggleWidth, previewCollapsed, railToggleWidth);
        FlowLayout body = UiFactory.row();
        body.gap(UiFactory.scaledPixels(BODY_GAP));
        body.padding(Insets.bottom(bodyBottomPadding));
        body.verticalAlignment(VerticalAlignment.TOP);
        if (categoriesCollapsed) {
            body.child(this.buildRailSideToggleCard(
                    Component.literal(SYMBOL_RIGHT),
                    Component.literal(TOOLTIP_SHOW_CATEGORIES),
                    () -> {
                        this.screen.setCategoriesRailCollapsed(false);
                        this.screen.rebuildLayout();
                    }
            ).horizontalSizing(Sizing.fixed(railToggleWidth)));
        } else {
            body.child(this.buildTabsCard(metrics.tabsWidth()).horizontalSizing(Sizing.fixed(metrics.tabsWidth())));
        }
        body.child(this.buildEditorCard().horizontalSizing(Sizing.fixed(metrics.editorWidth())).verticalSizing(Sizing.fill(100)));
        if (previewCollapsed) {
            body.child(this.buildRailSideToggleCard(
                    Component.literal(SYMBOL_LEFT),
                    Component.literal(TOOLTIP_SHOW_PREVIEW),
                    () -> {
                        this.screen.setPreviewRailCollapsed(false);
                        this.screen.rebuildLayout();
                    }
            ).horizontalSizing(Sizing.fixed(railToggleWidth)));
        } else {
            int previewHeightHint = Math.max(1, bodyHeight - bodyBottomPadding);
            body.child(this.buildPreviewCard(metrics.previewWidth(), previewHeightHint).horizontalSizing(Sizing.fixed(metrics.previewWidth())));
        }
        shell.child(body.verticalSizing(Sizing.fixed(bodyHeight)));

        FlowLayout centered = UiFactory.column();
        centered.gap(0);
        centered.horizontalSizing(Sizing.fill(100));
        centered.verticalSizing(Sizing.fill(100));
        centered.padding(Insets.of(verticalPadding, outerPadding, verticalPadding + bottomSafety, outerPadding));
        centered.horizontalAlignment(HorizontalAlignment.CENTER);
        centered.verticalAlignment(VerticalAlignment.TOP);
        centered.child(shell);
        return centered;
    }

    private FlowLayout buildTopBar() {
        FlowLayout card = UiFactory.card();
        card.gap(2);
        boolean highGuiScale = this.currentGuiScale() >= GUI_SCALE_HIGH_THRESHOLD;
        boolean compact = this.shellWidth <= TOP_BAR_COMPACT_WIDTH_THRESHOLD || highGuiScale;
        int effectivePadding = UiFactory.scaleProfile().padding();
        if (highGuiScale) {
            effectivePadding = this.tightSpacingFloor2();
            card.padding(Insets.of(effectivePadding));
        }

        FlowLayout topRow = UiFactory.row();

        int topButtonHeight = UiFactory.scaleProfile().controlHeight();
        if (highGuiScale) {
            topButtonHeight -= this.scaledMin(2, 3);
        }
        int rowGap = this.tightSpacingFloor2();
        int minTextGroupWidth = this.clamp(
                TOP_BAR_MIN_TEXT_WIDTH,
                TOP_TEXT_GROUP_DYNAMIC_MIN,
                (int) Math.round(this.shellWidth * 0.34d)
        );
        int availableRowWidth = Math.max(1, this.shellWidth - (effectivePadding * 2));
        int maxRowGapBudget = TOP_ACTION_COUNT > 1
                ? Math.max(0, (availableRowWidth - TOP_ACTION_COUNT) / (TOP_ACTION_COUNT - 1))
                : rowGap;
        rowGap = Math.min(rowGap, maxRowGapBudget);
        TopActionLayout actionLayout = this.computeTopActionLayout(compact, availableRowWidth, rowGap, minTextGroupWidth);
        int buttonGroupWidth = this.sumTopActionWidths(actionLayout.widths(), rowGap);
        FlowLayout buttonGroup = this.buildTopActionButtons(actionLayout, topButtonHeight, rowGap, buttonGroupWidth);
        int textGroupWidth = Math.max(1, availableRowWidth - rowGap - buttonGroupWidth);

        FlowLayout textGroup = this.buildTopBarTextGroup(textGroupWidth, rowGap, compact);

        topRow.child(textGroup);
        topRow.child(buttonGroup);
        card.child(topRow);
        return card;
    }

    private FlowLayout buildTabsCard(int width) {
        FlowLayout card = UiFactory.card();
        card.verticalSizing(Sizing.fill(100));
        int titleWidth = Math.max(TEXT_WIDTH_MIN, width - (UiFactory.scaleProfile().padding() * 2) - 10);
        Component categoriesTitle = ItemEditorText.tr("screen.categories");
        FlowLayout header = UiFactory.row();
        int toggleSize = this.railToggleSize();
        int categoriesLabelWidth = Math.max(20, titleWidth - toggleSize - UiFactory.scaledPixels(TABS_HEADER_GAP_BASE));
        UIComponent categoriesLabel = UiFactory.muted(
                UiFactory.fitToWidth(categoriesTitle, categoriesLabelWidth),
                categoriesLabelWidth
        ).tooltip(List.of(categoriesTitle));
        categoriesLabel.horizontalSizing(Sizing.expand(100));
        header.child(categoriesLabel);
        ButtonComponent collapse = UiFactory.button(Component.literal(SYMBOL_LEFT), UiFactory.ButtonTextPreset.STANDARD,  button -> {
            this.screen.setCategoriesRailCollapsed(true);
            this.screen.rebuildLayout();
        });
        this.configureRailToggleButton(collapse, toggleSize, Component.literal(TOOLTIP_HIDE_CATEGORIES), false);
        header.child(collapse);
        card.child(header);
        this.tabs = UiFactory.scrollContentColumn(PREVIEW_SCROLLBAR_THICKNESS);
        ScrollContainer<FlowLayout> tabsScroll = this.configureScroll(
                InputSafeScrollContainer.vertical(
                        Sizing.fill(100),
                        Sizing.expand(100),
                        this.tabs
                ),
                TABS_SCROLL_STEP_BASE,
                PREVIEW_SCROLLBAR_THICKNESS
        );
        card.child(tabsScroll);
        return card;
    }

    private FlowLayout buildEditorCard() {
        FlowLayout card = UiFactory.card();
        card.verticalSizing(Sizing.fill(100));
        card.gap(4);
        this.selectedCategoryLabel = UiFactory.title(this.screen.categoryTitle(this.screen.selectedModule()));

        this.panelHost = UiFactory.scrollContentColumn(PANEL_SCROLLBAR_THICKNESS);
        this.panelScroll = this.configureScroll(
                InputSafeScrollContainer.vertical(
                        Sizing.fill(100),
                        Sizing.expand(100),
                        this.panelHost
                ),
                PANEL_SCROLL_STEP_BASE,
                PANEL_SCROLLBAR_THICKNESS
        );

        card.child(this.selectedCategoryLabel);
        card.child(this.panelScroll);
        return card;
    }

    private FlowLayout buildPreviewCard(int width, int availableHeightHint) {
        this.previewCardHeightHint = Math.max(1, availableHeightHint);
        int cardPadding = UiFactory.scaleProfile().padding();
        int innerWidth = Math.max(1, width - (cardPadding * 2));
        int preferredTextWidth = Math.max(
                TEXT_WIDTH_MIN,
                innerWidth - UiFactory.scrollContentInset(PREVIEW_SCROLLBAR_THICKNESS)
        );
        int textWidth = Math.max(1, Math.min(innerWidth, preferredTextWidth));
        this.previewTextWidthHint = textWidth;
        FlowLayout card = UiFactory.card();
        card.verticalSizing(Sizing.fill(100));
        card.gap(4);
        this.previewItem = UIComponents.item(this.screen.session().previewStack())
                .showOverlay(true);

        ButtonComponent collapse = UiFactory.button(Component.literal(SYMBOL_LEFT), UiFactory.ButtonTextPreset.STANDARD,  button -> {
            this.screen.setPreviewRailCollapsed(true);
            this.screen.rebuildLayout();
        });
        int railToggleSize = this.railToggleSize();
        this.configureRailToggleButton(collapse, railToggleSize, Component.literal(TOOLTIP_HIDE_PREVIEW), false);

        this.tooltipLines = UiFactory.scrollContentColumn(PREVIEW_SCROLLBAR_THICKNESS, PREVIEW_TOOLTIP_CONTENT_INSET_BASE);
        this.messages = UiFactory.scrollContentColumn(PREVIEW_SCROLLBAR_THICKNESS, PREVIEW_VALIDATION_CONTENT_INSET_BASE);

        int verticalGap = this.tightSpacingFloor2();

        boolean tooltipCollapsed = this.screen.previewTooltipCollapsed();
        boolean validationCollapsed = this.screen.previewValidationCollapsed();
        int collapsedSectionHeight = Math.max(
                UiFactory.scaledPixels(PREVIEW_SECTION_COLLAPSED_MIN_HEIGHT),
                UiFactory.scaleProfile().controlHeight() + UiFactory.scaledPixels(PREVIEW_SECTION_COLLAPSED_CONTROL_RESERVE)
        );
        ScrollContainer<FlowLayout> tooltipSectionScroll = this.configureScroll(
                InputSafeScrollContainer.vertical(
                        Sizing.fill(100),
                        Sizing.expand(100),
                        this.tooltipLines
                ),
                PREVIEW_TOOLTIP_SCROLL_STEP_BASE,
                PREVIEW_SCROLLBAR_THICKNESS
        );
        this.tooltipScroll = tooltipSectionScroll;

        ScrollContainer<FlowLayout> validationSectionScroll = this.configureScroll(
                InputSafeScrollContainer.vertical(
                        Sizing.fill(100),
                        Sizing.expand(100),
                        this.messages
                ),
                PREVIEW_VALIDATION_SCROLL_STEP_BASE,
                PREVIEW_SCROLLBAR_THICKNESS
        );
        this.messageScroll = validationSectionScroll;

        FlowLayout previewContent = UiFactory.column();
        previewContent.verticalSizing(Sizing.expand(100));
        previewContent.gap(verticalGap);
        FlowLayout tooltipSection = this.collapsibleTooltipSection(
                tooltipCollapsed,
                this.screen::setPreviewTooltipCollapsed,
                tooltipSectionScroll,
                textWidth
        );
        boolean validationExpanded = !validationCollapsed;
        boolean tooltipExpanded = !tooltipCollapsed;
        PreviewSectionHeights sectionHeights = this.computePreviewSectionHeights(
                tooltipExpanded,
                validationExpanded,
                collapsedSectionHeight,
                collapsedSectionHeight,
                verticalGap
        );
        tooltipSection.verticalSizing(Sizing.fixed(sectionHeights.tooltipHeight()));
        previewContent.child(tooltipSection);
        FlowLayout validationSection = this.collapsiblePreviewSection(
                ItemEditorText.tr("screen.validation"),
                validationCollapsed,
                this.screen::setPreviewValidationCollapsed,
                validationSectionScroll
        );
        validationSection.verticalSizing(Sizing.fixed(sectionHeights.validationHeight()));
        previewContent.child(validationSection);

        FlowLayout previewHeader = UiFactory.row();
        int previewTitleWidth = Math.max(TEXT_WIDTH_MIN, this.previewTextWidthHint - railToggleSize - UiFactory.scaledPixels(8));
        UIComponent previewTitle = UiFactory.title(
                UiFactory.fitToWidth(ItemEditorText.tr("screen.preview"), previewTitleWidth),
                PREVIEW_PANEL_TITLE_SCALE
        ).maxWidth(previewTitleWidth).tooltip(List.of(ItemEditorText.tr("screen.preview")));
        previewTitle.horizontalSizing(Sizing.expand(100));
        previewHeader.child(previewTitle);
        previewHeader.child(collapse);
        card.child(previewHeader);
        card.child(previewContent);
        return card;
    }

    private FlowLayout buildRailSideToggleCard(
            Component label,
            Component tooltip,
            Runnable action
    ) {
        FlowLayout card = UiFactory.card();
        card.verticalSizing(Sizing.fill(100));
        card.padding(Insets.of(this.tightSpacingFloor2()));
        ButtonComponent toggle = UiFactory.button(label, UiFactory.ButtonTextPreset.STANDARD, button -> action.run());
        this.configureRailToggleButton(toggle, this.railToggleSize(), tooltip, true);
        card.child(toggle);
        return card;
    }

    private FlowLayout buildPreviewLeadRow(int textWidth, boolean collapsed, Consumer<Boolean> setter) {
        FlowLayout row = UiFactory.row();
        row.child(this.previewItem);
        Component fullName = this.screen.session().previewStack().getHoverName();
        int toggleWidth = this.previewSectionToggleSize();
        int rowGap = this.tightSpacingFloor2();
        int iconSlotWidth = UiFactory.scaledPixels(PREVIEW_LEAD_ICON_SLOT_BASE);
        int nameWidth = Math.max(
                TEXT_WIDTH_MIN,
                textWidth - iconSlotWidth - toggleWidth - (rowGap * 2) - UiFactory.scaledPixels(PREVIEW_LEAD_NAME_GAP_BASE)
        );
        Component fittedName = UiFactory.fitToWidth(fullName, nameWidth);
        this.previewNameLabel = UiFactory.title(fittedName, PREVIEW_NAME_SCALE).maxWidth(nameWidth);
        this.previewNameLabel.horizontalSizing(Sizing.fixed(nameWidth));
        this.previewNameLabel.tooltip(List.of(fullName));
        row.child(this.previewNameLabel);

        ButtonComponent toggle = UiFactory.scaledTextButton(
                Component.literal(collapsed ? SYMBOL_PLUS : SYMBOL_MINUS),
                PREVIEW_SECTION_TOGGLE_SCALE,
                UiFactory.ButtonTextPreset.COMPACT,
                button -> {
                    setter.accept(!collapsed);
                    this.screen.rebuildLayout();
                }
        );
        int toggleSize = UiFactory.scaledPixels(PREVIEW_LEAD_COLLAPSE_TOGGLE_BASE);
        toggle.horizontalSizing(Sizing.fixed(toggleSize));
        toggle.verticalSizing(Sizing.fixed(toggleSize));
        row.child(toggle);
        return row;
    }

    private FlowLayout collapsibleTooltipSection(
            boolean collapsed,
            Consumer<Boolean> setter,
            UIComponent content,
            int textWidth
    ) {
        FlowLayout section = UiFactory.subCard();
        section.child(this.buildPreviewLeadRow(textWidth, collapsed, setter));
        if (!collapsed) {
            section.child(content.verticalSizing(Sizing.expand(100)));
        }
        return section;
    }

    private FlowLayout collapsiblePreviewSection(
            Component title,
            boolean collapsed,
            Consumer<Boolean> setter,
            UIComponent content
    ) {
        FlowLayout section = UiFactory.subCard();
        FlowLayout header = UiFactory.row();
        header.horizontalSizing(Sizing.fill(100));
        int toggleWidth = this.previewSectionToggleSize();
        int titleWidth = Math.max(TEXT_WIDTH_MIN, this.previewTextWidthHint - toggleWidth - UiFactory.scaledPixels(PREVIEW_SECTION_HEADER_GAP_BASE));
        Component fitted = UiFactory.fitToWidth(title, titleWidth);
        UIComponent titleLabel = UiFactory.title(fitted, PREVIEW_SECTION_TITLE_SCALE).shadow(false).maxWidth(titleWidth).tooltip(List.of(title));
        titleLabel.horizontalSizing(Sizing.expand(100));
        header.child(titleLabel);
        ButtonComponent toggle = UiFactory.scaledTextButton(Component.literal(collapsed ? SYMBOL_SECTION_COLLAPSED : SYMBOL_SECTION_EXPANDED),  PREVIEW_SECTION_TOGGLE_SCALE, UiFactory.ButtonTextPreset.STANDARD,  button -> {
            setter.accept(!collapsed);
            this.screen.rebuildLayout();
        });
        toggle.horizontalSizing(Sizing.fixed(toggleWidth));
        toggle.verticalSizing(Sizing.fixed(toggleWidth));
        header.child(toggle);
        section.child(header);
        if (!collapsed) {
            section.child(content.verticalSizing(Sizing.expand(100)));
        }
        return section;
    }

    private PreviewSectionHeights computePreviewSectionHeights(
            boolean tooltipExpanded,
            boolean validationExpanded,
            int tooltipCollapsedHeight,
            int validationCollapsedHeight,
            int verticalGap
    ) {
        int available = this.estimatedPreviewContentHeight();
        int tooltipMinExpanded = Math.max(UiFactory.scaledPixels(72), UiFactory.scaleProfile().controlHeight() * 3);
        int validationMinExpanded = Math.max(UiFactory.scaledPixels(70), UiFactory.scaleProfile().controlHeight() * 2);
        int expandedMinCombined = tooltipMinExpanded + validationMinExpanded + verticalGap;

        int tooltipHeight;
        int validationHeight;
        if (!tooltipExpanded && !validationExpanded) {
            tooltipHeight = tooltipCollapsedHeight;
            validationHeight = validationCollapsedHeight;
            return this.fitPreviewHeights(
                    tooltipHeight,
                    validationHeight,
                    Math.min(tooltipCollapsedHeight, Math.max(UiFactory.scaledPixels(PREVIEW_SECTION_COLLAPSED_FIT_MIN_HEIGHT), available / 2)),
                    Math.min(validationCollapsedHeight, Math.max(UiFactory.scaledPixels(PREVIEW_SECTION_COLLAPSED_FIT_MIN_HEIGHT), available / 2)),
                    available,
                    verticalGap
            );
        }

        if (!tooltipExpanded) {
            tooltipHeight = tooltipCollapsedHeight;
            validationHeight = Math.max(validationMinExpanded, available - tooltipHeight - verticalGap);
            return this.fitPreviewHeights(
                    tooltipHeight,
                    validationHeight,
                    Math.min(tooltipCollapsedHeight, Math.max(UiFactory.scaledPixels(PREVIEW_SECTION_COLLAPSED_FIT_MIN_HEIGHT), available - verticalGap)),
                    validationMinExpanded,
                    available,
                    verticalGap
            );
        }

        if (!validationExpanded) {
            validationHeight = validationCollapsedHeight;
            tooltipHeight = Math.max(tooltipMinExpanded, available - validationHeight - verticalGap);
            return this.fitPreviewHeights(
                    tooltipHeight,
                    validationHeight,
                    tooltipMinExpanded,
                    Math.min(validationCollapsedHeight, Math.max(UiFactory.scaledPixels(PREVIEW_SECTION_COLLAPSED_FIT_MIN_HEIGHT), available - verticalGap)),
                    available,
                    verticalGap
            );
        }

        int shared = Math.max(
                expandedMinCombined,
                available
        );
        int tooltipTarget = Math.max(tooltipMinExpanded, (int) Math.round(shared * (1.0d - PREVIEW_VALIDATION_RATIO)));
        int validationTarget = Math.max(validationMinExpanded, shared - tooltipTarget - verticalGap);
        return this.fitPreviewHeights(
                tooltipTarget,
                validationTarget,
                tooltipMinExpanded,
                validationMinExpanded,
                available,
                verticalGap
        );
    }

    private PreviewSectionHeights fitPreviewHeights(
            int tooltipTarget,
            int validationTarget,
            int tooltipMin,
            int validationMin,
            int available,
            int verticalGap
    ) {
        int tooltip = Math.max(tooltipMin, tooltipTarget);
        int validation = Math.max(validationMin, validationTarget);
        int combined = tooltip + validation + verticalGap;
        if (combined > available) {
            int overflow = combined - available;
            int reduceTooltip = Math.min(overflow, Math.max(0, tooltip - tooltipMin));
            tooltip -= reduceTooltip;
            overflow -= reduceTooltip;
            if (overflow > 0) {
                int reduceValidation = Math.min(overflow, Math.max(0, validation - validationMin));
                validation -= reduceValidation;
            }
        }
        combined = tooltip + validation + verticalGap;
        if (combined > available) {
            int hardMin = Math.max(UiFactory.scaledPixels(PREVIEW_SECTION_FIT_HARD_MIN_HEIGHT), UiFactory.scaleProfile().controlHeight());
            int overflow = combined - available;
            int reduceTooltip = Math.min(overflow, Math.max(0, tooltip - hardMin));
            tooltip -= reduceTooltip;
            overflow -= reduceTooltip;
            if (overflow > 0) {
                int reduceValidation = Math.min(overflow, Math.max(0, validation - hardMin));
                validation -= reduceValidation;
                overflow -= reduceValidation;
            }
            if (overflow > 0) {
                tooltip = Math.max(1, tooltip - overflow);
            }
        }
        combined = tooltip + validation + verticalGap;
        if (combined > available) {
            int contentBudget = Math.max(1, available - verticalGap);
            tooltip = Math.max(1, Math.min(tooltip, contentBudget));
            int remaining = Math.max(1, contentBudget - tooltip);
            validation = Math.max(1, Math.min(validation, remaining));
            combined = tooltip + validation + verticalGap;
            if (combined > available) {
                validation = 1;
                tooltip = Math.max(1, available - verticalGap - validation);
            }
        }
        return new PreviewSectionHeights(tooltip, validation);
    }

    private int estimatedPreviewContentHeight() {
        int bodyHeight = this.previewCardHeightHint;
        int cardPadding = UiFactory.scaleProfile().padding();
        int cardGap = UiFactory.scaledPixels(4);
        int previewHeaderHeight = Math.max(UiFactory.scaleProfile().controlHeight(), UiFactory.scaledPixels(PREVIEW_SECTION_HEADER_MIN_HEIGHT));
        int contentSafety = this.scaledMin(1, 2);
        return Math.max(
                1,
                bodyHeight - (cardPadding * 2) - previewHeaderHeight - cardGap - contentSafety
        );
    }

    private <C extends UIComponent> ScrollContainer<C> configureScroll(ScrollContainer<C> scroll, int step, int thickness) {
        scroll.scrollStep(UiFactory.scaledScrollStep(step));
        scroll.scrollbar(ScrollContainer.Scrollbar.vanillaFlat());
        scroll.scrollbarThiccness(UiFactory.scaledScrollbarThickness(thickness));
        return scroll;
    }

    private WideLayoutMetrics wideLayoutMetrics(
            int shellWidth,
            boolean categoriesCollapsed,
            int categoriesToggleWidth,
            boolean previewCollapsed,
            int previewToggleWidth
    ) {
        int bodyGap = Math.max(WIDE_BODY_GAP_MIN, UiFactory.scaledPixels(BODY_GAP));
        int segmentCount = 3;
        int gapCount = Math.max(0, segmentCount - 1);
        int toggleWidth = (categoriesCollapsed ? Math.max(0, categoriesToggleWidth) : 0)
                + (previewCollapsed ? Math.max(0, previewToggleWidth) : 0);
        int available = Math.max(1, shellWidth - (bodyGap * gapCount) - toggleWidth);
        double guiScale = this.currentGuiScale();
        boolean highGuiScale = guiScale >= GUI_SCALE_HIGH_THRESHOLD;
        double previewScale = highGuiScale ? PREVIEW_SCALE_HIGH : PREVIEW_SCALE_NORMAL;
        double tabsScale = guiScale >= GUI_SCALE_VERY_HIGH_THRESHOLD
                ? TABS_SCALE_VERY_HIGH
                : (highGuiScale ? TABS_SCALE_HIGH : TABS_SCALE_NORMAL);

        int tabsMin = 0;
        int tabsWidth = 0;
        if (!categoriesCollapsed) {
            tabsMin = (int) Math.round(this.widthByRatio(available, WIDE_TABS_MIN_RATIO, WIDE_TABS_MIN_FALLBACK) * tabsScale);
            int tabsMax = Math.max(tabsMin + WIDE_TABS_MAX_EXTRA, (int) Math.round(this.widthByRatio(available, WIDE_TABS_MAX_RATIO, RAIL_MAX_WIDTH) * tabsScale));
            int tabsTarget = (int) Math.round(this.widthByRatio(available, WIDE_TABS_TARGET_RATIO, WIDE_TABS_TARGET_FALLBACK) * tabsScale);
            tabsWidth = this.clamp(tabsTarget, tabsMin, tabsMax);
        }

        int previewWidth = 0;
        int editorMin = this.widthByRatio(available, WIDE_EDITOR_MIN_RATIO, WIDE_EDITOR_MIN_FALLBACK);
        if (!previewCollapsed) {
            int previewMin = Math.max(WIDE_PREVIEW_MIN_WIDTH, (int) Math.round(this.widthByRatio(available, WIDE_PREVIEW_MIN_RATIO, WIDE_PREVIEW_MIN_FALLBACK) * previewScale));
            int previewMax = Math.max(previewMin + WIDE_PREVIEW_MAX_EXTRA, (int) Math.round(this.widthByRatio(available, WIDE_PREVIEW_MAX_RATIO, WIDE_PREVIEW_MAX_FALLBACK) * previewScale));
            int previewTarget = (int) Math.round(this.widthByRatio(available, WIDE_PREVIEW_TARGET_RATIO, WIDE_PREVIEW_TARGET_FALLBACK) * previewScale);
            previewWidth = this.clamp(previewTarget, previewMin, previewMax);
        }
        int editorWidth = available - tabsWidth - previewWidth;

        if (!previewCollapsed && editorWidth < editorMin) {
            int deficit = editorMin - editorWidth;
            int previewHardFloor = Math.max(WIDE_PREVIEW_HARD_FLOOR_MIN, (int) Math.round(this.widthByRatio(available, WIDE_PREVIEW_HARD_FLOOR_RATIO, WIDE_PREVIEW_HARD_FLOOR_FALLBACK) * previewScale));
            int previewCut = Math.min(deficit, Math.max(0, previewWidth - previewHardFloor));
            previewWidth -= previewCut;
            deficit -= previewCut;

            int tabsCut = Math.min(deficit, Math.max(0, tabsWidth - tabsMin));
            tabsWidth -= tabsCut;
        }

        int editorWidthAfterFirstPass = available - tabsWidth - previewWidth;
        int absoluteEditorFloor = WIDE_ABSOLUTE_EDITOR_FLOOR;
        if (editorWidthAfterFirstPass < absoluteEditorFloor) {
            int deficit = absoluteEditorFloor - editorWidthAfterFirstPass;
            int previewHardMin = this.widthByRatio(available, WIDE_PREVIEW_HARD_FLOOR_RATIO, WIDE_PREVIEW_HARD_FLOOR_FALLBACK);
            int tabsHardMin = categoriesCollapsed ? 0 : this.widthByRatio(available, WIDE_TABS_HARD_MIN_RATIO, WIDE_TABS_HARD_MIN_FALLBACK);

            if (!previewCollapsed) {
                int previewCut = Math.min(deficit, Math.max(0, previewWidth - previewHardMin));
                previewWidth -= previewCut;
                deficit -= previewCut;
            }

            int tabsCut = Math.min(deficit, Math.max(0, tabsWidth - tabsHardMin));
            tabsWidth -= tabsCut;
        }

        int editorWidthBeforeCap = available - tabsWidth - previewWidth;
        if (editorWidthBeforeCap < 0) {
            previewWidth = Math.max(0, previewWidth + editorWidthBeforeCap);
        }

        tabsWidth = Math.min(available, Math.max(0, tabsWidth));
        previewWidth = Math.min(Math.max(0, available - tabsWidth), Math.max(0, previewWidth));
        editorWidth = Math.max(0, available - tabsWidth - previewWidth);

        return new WideLayoutMetrics(tabsWidth, editorWidth, previewWidth);
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private int widthByRatio(int sourceWidth, double ratio, int fallbackMin) {
        return Math.max(fallbackMin, (int) Math.round(sourceWidth * ratio));
    }

    private int componentTextWidth(Component component) {
        return Math.max(0, Minecraft.getInstance().font.width(component.getString()));
    }

    private int railToggleSize() {
        return UiFactory.scaledPixels(RAIL_TOGGLE_SIZE);
    }

    private int previewSectionToggleSize() {
        return UiFactory.scaledPixels(PREVIEW_SECTION_SIDE_TOGGLE_BASE);
    }

    private int tightSpacingFloor2() {
        return Math.max(2, UiFactory.scaleProfile().tightSpacing());
    }

    private int scaledMin(int min, int pixels) {
        return Math.max(min, UiFactory.scaledPixels(pixels));
    }

    private void configureRailToggleButton(ButtonComponent button, int size, Component tooltip, boolean fillWidth) {
        button.horizontalSizing(fillWidth ? Sizing.fill(100) : Sizing.fixed(size));
        button.verticalSizing(Sizing.fixed(size));
        button.tooltip(List.of(tooltip));
    }

    private double currentGuiScale() {
        return Minecraft.getInstance().getWindow().getGuiScale();
    }

    private FlowLayout buildTopBarTextGroup(int textGroupWidth, int rowGap, boolean compact) {
        FlowLayout textGroup = this.centeredHorizontalFlow(Sizing.fixed(textGroupWidth), Sizing.content(), rowGap);

        Component titleText = ItemEditorText.tr("screen.title");
        Component applyModeFull = Component.literal(this.screen.applyModeText());
        int textBudget = Math.max(1, textGroupWidth - rowGap);
        int minSegmentWidth = Math.clamp(textBudget / 2, 1, TEXT_WIDTH_MIN);
        int titleFullWidth = this.componentTextWidth(titleText);
        int applyModeFullWidth = this.componentTextWidth(applyModeFull);
        int titleWidth;
        int applyModeWidth;
        if (titleFullWidth + applyModeFullWidth <= textBudget) {
            titleWidth = Math.max(minSegmentWidth, Math.min(textBudget - minSegmentWidth, titleFullWidth));
            applyModeWidth = Math.max(minSegmentWidth, textBudget - titleWidth);
        } else {
            int titleTargetWidth = compact
                    ? (int) Math.round(textGroupWidth * TOP_BAR_TITLE_RATIO_COMPACT)
                    : (int) Math.round(textGroupWidth * TOP_BAR_TITLE_RATIO_REGULAR);
            int titleMaxWidth = Math.max(minSegmentWidth, textGroupWidth - rowGap - minSegmentWidth);
            titleWidth = this.clamp(titleTargetWidth, minSegmentWidth, titleMaxWidth);
            applyModeWidth = Math.max(minSegmentWidth, textGroupWidth - titleWidth - rowGap);
        }

        textGroup.child(UiFactory.title(UiFactory.fitToWidth(titleText, titleWidth)).maxWidth(titleWidth).tooltip(List.of(titleText)));

        this.applyModeTextWidthHint = applyModeWidth;
        this.applyModeLabel = UiFactory.message(UiFactory.fitToWidth(applyModeFull, applyModeWidth), this.screen.applyModeColorInt());
        this.applyModeLabel.maxWidth(applyModeWidth);
        this.applyModeLabel.tooltip(List.of(applyModeFull));
        textGroup.child(this.applyModeLabel);
        return textGroup;
    }

    private ButtonComponent topActionButton(Component fullText, int maxTextWidth, int buttonWidth, int buttonHeight, Consumer<ButtonComponent> onPress) {
        Component fitted = UiFactory.fitToWidth(fullText, maxTextWidth);
        ButtonComponent button = UiFactory.button(fitted, UiFactory.ButtonTextPreset.STANDARD, onPress);
        int minWidth = Math.min(buttonWidth, Math.max(TOP_ACTION_BUTTON_ABSOLUTE_MIN_WIDTH, UiFactory.scaledPixels(TOP_ACTION_BUTTON_ADAPTIVE_MIN_FALLBACK)));
        int labelWidth = Math.max(1, this.componentTextWidth(fitted));
        int horizontalPadding = Math.max(TOP_ACTION_BUTTON_LABEL_PADDING_MIN, UiFactory.scaledPixels(TOP_ACTION_BUTTON_LABEL_PADDING_BASE));
        int adaptiveWidth = Math.max(minWidth, Math.min(buttonWidth, labelWidth + horizontalPadding));
        button.horizontalSizing(Sizing.fixed(adaptiveWidth));
        button.verticalSizing(Sizing.fixed(buttonHeight));
        button.tooltip(List.of(fullText));
        return button;
    }

    private TopActionLayout computeTopActionLayout(
            boolean compact,
            int availableRowWidth,
            int rowGap,
            int minTextGroupWidth
    ) {
        Component[] labels = new Component[]{
                ItemEditorText.tr("screen.raw_data.original_item"),
                ItemEditorText.tr("screen.raw_data.current_item"),
                ItemEditorText.tr("common.reset"),
                ItemEditorText.tr("common.cancel"),
                ItemEditorText.tr("common.save_apply")
        };
        int targetButtonWidth = compact
                ? this.clamp((int) Math.round(this.shellWidth * TOP_ACTION_BUTTON_TARGET_RATIO_COMPACT), TOP_ACTION_BUTTON_COMPACT_TARGET_MIN, TOP_ACTION_BUTTON_COMPACT_TARGET_MAX)
                : this.clamp((int) Math.round(this.shellWidth * TOP_ACTION_BUTTON_TARGET_RATIO_REGULAR), TOP_ACTION_BUTTON_REGULAR_TARGET_MIN, TOP_ACTION_BUTTON_REGULAR_TARGET_MAX);
        int minButtonWidth = compact ? Math.max(TOP_ACTION_BUTTON_COMPACT_MIN_WIDTH, TOP_ACTION_BUTTON_MIN_WIDTH - 8) : TOP_ACTION_BUTTON_MIN_WIDTH;
        int maxButtonWidth = compact ? TOP_ACTION_BUTTON_MAX_WIDTH_COMPACT : TOP_ACTION_BUTTON_MAX_WIDTH;
        int[] desiredWidths = new int[labels.length];
        int[] minWidths = new int[labels.length];
        int basePadding = compact
                ? UiFactory.scaledPixels(TOP_ACTION_BUTTON_TEXT_PADDING_COMPACT)
                : UiFactory.scaledPixels(TOP_ACTION_BUTTON_TEXT_PADDING_REGULAR);
        int minAdaptive = compact ? TOP_ACTION_BUTTON_MIN_WIDTH - 4 : TOP_ACTION_BUTTON_MIN_WIDTH;
        int maxAdaptive = compact ? TOP_ACTION_BUTTON_MAX_WIDTH_COMPACT : TOP_ACTION_BUTTON_MAX_WIDTH;
        int upperBound = Math.max(minAdaptive, Math.min(maxAdaptive, targetButtonWidth));
        for (int index = 0; index < labels.length; index++) {
            int adaptive = this.componentTextWidth(labels[index]) + basePadding;
            desiredWidths[index] = this.clamp(adaptive, minAdaptive, upperBound);
            minWidths[index] = minButtonWidth;
        }
        int maxButtonGroupWidth = Math.max(1, availableRowWidth - rowGap - minTextGroupWidth);
        this.fitTopActionWidths(desiredWidths, minWidths, maxButtonGroupWidth, rowGap);
        return new TopActionLayout(labels, desiredWidths, maxButtonWidth);
    }

    private FlowLayout buildTopActionButtons(TopActionLayout layout, int topButtonHeight, int rowGap, int buttonGroupWidth) {
        Component[] labels = layout.labels();
        int[] widths = layout.widths();
        int maxButtonWidth = layout.maxButtonWidth();
        ButtonComponent originalButton = this.topActionButton(
                labels[0],
                this.topButtonTextWidth(widths[0], maxButtonWidth),
                widths[0],
                topButtonHeight,
                button -> this.screen.openRawItemDataDialog(ItemEditorText.str("dialog.raw_data.original_item_title"), false)
        );
        ButtonComponent currentButton = this.topActionButton(
                labels[1],
                this.topButtonTextWidth(widths[1], maxButtonWidth),
                widths[1],
                topButtonHeight,
                button -> this.screen.openRawItemDataDialog(ItemEditorText.str("dialog.raw_data.current_item_title"), true)
        );
        this.resetButton = this.topActionButton(
                labels[2],
                this.topButtonTextWidth(widths[2], maxButtonWidth),
                widths[2],
                topButtonHeight,
                button -> this.screen.requestReset()
        );
        ButtonComponent cancelButton = this.topActionButton(
                labels[3],
                this.topButtonTextWidth(widths[3], maxButtonWidth),
                widths[3],
                topButtonHeight,
                button -> this.screen.requestClose()
        );
        this.applyButton = this.topActionButton(
                labels[4],
                this.topButtonTextWidth(widths[4], maxButtonWidth),
                widths[4],
                topButtonHeight,
                button -> this.screen.requestApply()
        );
        FlowLayout buttonGroup = this.centeredHorizontalFlow(Sizing.fixed(Math.max(1, buttonGroupWidth)), Sizing.content(), rowGap);
        buttonGroup.child(originalButton);
        buttonGroup.child(currentButton);
        buttonGroup.child(this.resetButton);
        buttonGroup.child(cancelButton);
        buttonGroup.child(this.applyButton);
        return buttonGroup;
    }

    private FlowLayout centeredHorizontalFlow(Sizing width, Sizing height, int gap) {
        FlowLayout row = UiFactory.row();
        row.horizontalSizing(width);
        row.verticalSizing(height);
        row.gap(gap);
        return row;
    }

    private record TopActionLayout(Component[] labels, int[] widths, int maxButtonWidth) {
    }

    private record PreviewSectionHeights(int tooltipHeight, int validationHeight) {
    }

    private int topButtonTextWidth(int buttonWidth, int maxButtonWidth) {
        int width = Math.clamp(buttonWidth, 1, maxButtonWidth);
        return Math.max(1, width - UiFactory.scaledPixels(8));
    }

    private int sumTopActionWidths(int[] widths, int rowGap) {
        int sum = 0;
        for (int width : widths) {
            sum += Math.max(0, width);
        }
        return sum + (Math.max(0, widths.length - 1) * rowGap);
    }

    private void fitTopActionWidths(int[] widths, int[] minWidths, int maxGroupWidth, int rowGap) {
        int current = this.sumTopActionWidths(widths, rowGap);
        if (current <= maxGroupWidth) {
            return;
        }
        int guard = 0;
        while (current > maxGroupWidth && guard++ < 1024) {
            int shrinkIndex = -1;
            int shrinkRoom = 0;
            for (int index = 0; index < widths.length; index++) {
                int room = widths[index] - minWidths[index];
                if (room > shrinkRoom) {
                    shrinkRoom = room;
                    shrinkIndex = index;
                }
            }
            if (shrinkIndex < 0) {
                break;
            }
            widths[shrinkIndex]--;
            current--;
        }
        if (current <= maxGroupWidth) {
            return;
        }
        int guardTight = 0;
        while (current > maxGroupWidth && guardTight++ < 1024) {
            int shrinkIndex = -1;
            int shrinkWidth = 0;
            for (int index = 0; index < widths.length; index++) {
                if (widths[index] > shrinkWidth) {
                    shrinkWidth = widths[index];
                    shrinkIndex = index;
                }
            }
            if (shrinkIndex < 0 || shrinkWidth <= 1) {
                break;
            }
            widths[shrinkIndex]--;
            current--;
        }
    }

    record BuildResult(
            UIComponent shell,
            FlowLayout tabs,
            FlowLayout panelHost,
            ScrollContainer<FlowLayout> panelScroll,
            ScrollContainer<FlowLayout> tooltipScroll,
            ScrollContainer<FlowLayout> messageScroll,
            FlowLayout tooltipLines,
            FlowLayout messages,
            LabelComponent selectedCategoryLabel,
            LabelComponent applyModeLabel,
            int applyModeTextWidthHint,
            LabelComponent previewNameLabel,
            ItemComponent previewItem,
            ButtonComponent applyButton,
            ButtonComponent resetButton,
            int previewTextWidthHint
    ) {
    }

    private record WideLayoutMetrics(int tabsWidth, int editorWidth, int previewWidth) {
    }

}
