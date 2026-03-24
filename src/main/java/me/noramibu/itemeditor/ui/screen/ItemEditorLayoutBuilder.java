package me.noramibu.itemeditor.ui.screen;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.ItemComponent;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.component.UIComponents;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.container.UIContainers;
import io.wispforest.owo.ui.core.HorizontalAlignment;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.UIComponent;
import me.noramibu.itemeditor.ui.component.UiFactory;
import me.noramibu.itemeditor.util.ItemEditorText;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Consumer;

final class ItemEditorLayoutBuilder {

    private static final int SHELL_MAX_WIDTH = 1140;
    private static final int SHELL_SIDE_PADDING = 8;
    private static final int BODY_RESERVED_HEIGHT = 118;
    private static final int BODY_MIN_HEIGHT = 260;
    private static final int EDITOR_TARGET_MIN_WIDTH = 420;

    private final ItemEditorScreen screen;
    private int shellWidth;
    private int previewTextWidthHint = 210;
    private FlowLayout tabs;
    private FlowLayout panelHost;
    private ScrollContainer<FlowLayout> panelScroll;
    private ScrollContainer<FlowLayout> tooltipScroll;
    private ScrollContainer<FlowLayout> messageScroll;
    private FlowLayout tooltipLines;
    private FlowLayout messages;
    private LabelComponent selectedCategoryLabel;
    private LabelComponent applyModeLabel;
    private LabelComponent previewNameLabel;
    private LabelComponent rawDataStatusLabel;
    private ItemComponent previewItem;
    private ButtonComponent applyButton;
    private ButtonComponent resetButton;

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
                this.previewNameLabel,
                this.rawDataStatusLabel,
                this.previewItem,
                this.applyButton,
                this.resetButton,
                this.previewTextWidthHint
        );
    }

    private UIComponent buildShell() {
        this.shellWidth = Math.max(1, Math.min(SHELL_MAX_WIDTH, this.screen.screenWidth() - SHELL_SIDE_PADDING));
        int bodyHeight = this.availableBodyHeight();

        FlowLayout shell = UIContainers.verticalFlow(Sizing.fixed(this.shellWidth), Sizing.content());
        shell.gap(4);
        shell.allowOverflow(false);

        FlowLayout topBar = this.buildTopBar();
        topBar.horizontalSizing(Sizing.fill(100));
        shell.child(topBar);

        WideLayoutMetrics metrics = this.wideLayoutMetrics(this.shellWidth);
        FlowLayout body = UIContainers.horizontalFlow(Sizing.fill(100), Sizing.fixed(bodyHeight));
        body.gap(8);
        body.allowOverflow(false);
        body.child(this.buildTabsCard().horizontalSizing(Sizing.fixed(metrics.tabsWidth())));
        body.child(this.buildEditorCard().horizontalSizing(Sizing.fixed(metrics.editorWidth())));
        body.child(this.buildPreviewCard(metrics.previewWidth()).horizontalSizing(Sizing.fixed(metrics.previewWidth())));
        shell.child(body);

        FlowLayout centered = UIContainers.verticalFlow(Sizing.fill(), Sizing.fill());
        centered.padding(Insets.of(2));
        centered.horizontalAlignment(HorizontalAlignment.CENTER);
        centered.child(shell);
        return centered;
    }

    private FlowLayout buildTopBar() {
        FlowLayout card = UiFactory.card();
        card.gap(4);

        FlowLayout topRow = UiFactory.row();
        int titleWidth = this.clamp((int) Math.round(this.shellWidth * 0.32), 120, 320);
        topRow.child(UiFactory.title(ItemEditorText.tr("screen.title")).maxWidth(titleWidth));
        this.applyModeLabel = UiFactory.message(this.screen.applyModeText(), this.screen.applyModeColorInt());
        this.applyModeLabel.maxWidth(this.clamp((int) Math.round(this.shellWidth * 0.56), 120, 560));
        this.applyModeLabel.tooltip(List.of(Component.literal(this.screen.applyModeText())));
        topRow.child(this.applyModeLabel);
        card.child(topRow);

        FlowLayout bottomRow = UiFactory.row();
        int helperWidth = this.clamp((int) Math.round(this.shellWidth * 0.30), 100, 260);
        Component helperText = ItemEditorText.tr("screen.preview_apply");
        bottomRow.child(UiFactory.muted(UiFactory.fitToWidth(helperText, helperWidth), helperWidth).tooltip(List.of(helperText)));
        bottomRow.horizontalAlignment(HorizontalAlignment.RIGHT);

        int buttonTextWidth = this.clamp((int) Math.round(this.shellWidth * 0.11), 42, 120);
        this.resetButton = this.topActionButton(ItemEditorText.tr("common.reset"), buttonTextWidth, button -> this.screen.requestReset());
        ButtonComponent cancelButton = this.topActionButton(ItemEditorText.tr("common.cancel"), buttonTextWidth, button -> this.screen.requestClose());
        this.applyButton = this.topActionButton(ItemEditorText.tr("common.save_apply"), this.clamp(buttonTextWidth + 40, 70, 170), button -> this.screen.requestApply());

        bottomRow.child(this.resetButton);
        bottomRow.child(cancelButton);
        bottomRow.child(this.applyButton);
        card.child(bottomRow);
        return card;
    }

    private FlowLayout buildTabsCard() {
        FlowLayout card = UiFactory.card();
        card.child(UiFactory.title(ItemEditorText.tr("screen.categories")));
        this.tabs = UiFactory.column();
        card.child(this.tabs);
        return card;
    }

    private FlowLayout buildEditorCard() {
        FlowLayout card = UiFactory.card();
        card.verticalSizing(Sizing.fill(100));
        card.gap(4);
        this.selectedCategoryLabel = UiFactory.title(this.screen.categoryTitle(this.screen.selectedModule()));

        this.panelHost = UiFactory.column();
        this.panelHost.allowOverflow(false);
        this.panelHost.padding(Insets.of(0, 8, 8, 0));
        this.panelScroll = this.configureScroll(
                UIContainers.verticalScroll(
                        Sizing.fill(100),
                        Sizing.fill(100),
                        this.panelHost
                ),
                14,
                8
        );

        card.child(this.selectedCategoryLabel);
        card.child(this.panelScroll);
        return card;
    }

    private FlowLayout buildPreviewCard(int width) {
        int textWidth = Math.max(68, width - 24);
        this.previewTextWidthHint = textWidth;
        FlowLayout card = UiFactory.card();
        card.verticalSizing(Sizing.fill(100));
        card.gap(8);
        this.previewItem = UIComponents.item(this.screen.session().previewStack())
                .showOverlay(true);

        FlowLayout previewHeader = UiFactory.row();
        previewHeader.child(this.previewItem.margins(Insets.right(8)));
        this.previewNameLabel = UiFactory.title(this.screen.session().previewStack().getHoverName().getString()).maxWidth(textWidth);
        previewHeader.child(this.previewNameLabel);

        this.tooltipLines = UiFactory.column();
        this.tooltipLines.padding(Insets.right(10));
        this.messages = UiFactory.column();
        this.messages.padding(Insets.right(10));

        this.tooltipScroll = this.configureScroll(
                UIContainers.verticalScroll(
                        Sizing.fill(100),
                        Sizing.fixed(220),
                        this.tooltipLines
                ),
                12,
                7
        );

        this.messageScroll = this.configureScroll(
                UIContainers.verticalScroll(
                        Sizing.fill(100),
                        Sizing.fill(100),
                        this.messages
                ),
                12,
                7
        );

        card.child(UiFactory.title(ItemEditorText.tr("screen.preview")));
        card.child(previewHeader);
        card.child(UiFactory.title(ItemEditorText.tr("screen.tooltip")));
        card.child(this.tooltipScroll);
        card.child(this.buildRawDataSection(textWidth));
        card.child(UiFactory.title(ItemEditorText.tr("screen.validation")));
        card.child(this.messageScroll);
        return card;
    }

    private FlowLayout buildRawDataSection(int textWidth) {
        FlowLayout section = UiFactory.subCard();
        section.child(UiFactory.title(ItemEditorText.tr("screen.raw_data")).shadow(false));
        this.rawDataStatusLabel = UiFactory.muted(this.screen.rawDataStatusText(), textWidth);
        section.child(this.rawDataStatusLabel);

        FlowLayout buttons = UiFactory.row();
        ButtonComponent originalButton = UiFactory.button(
                ItemEditorText.tr("screen.raw_data.original"),
                button -> this.screen.openRawItemDataDialog(ItemEditorText.str("dialog.raw_data.original_title"), false)
        );
        originalButton.horizontalSizing(Sizing.content());
        ButtonComponent previewButton = UiFactory.button(
                ItemEditorText.tr("screen.raw_data.preview"),
                button -> this.screen.openRawItemDataDialog(ItemEditorText.str("dialog.raw_data.preview_title"), true)
        );
        previewButton.horizontalSizing(Sizing.content());
        buttons.child(originalButton);
        buttons.child(previewButton);
        section.child(buttons);
        return section;
    }

    private <C extends UIComponent> ScrollContainer<C> configureScroll(ScrollContainer<C> scroll, int step, int thickness) {
        scroll.scrollStep(step);
        scroll.scrollbar(ScrollContainer.Scrollbar.vanillaFlat());
        scroll.scrollbarThiccness(thickness);
        return scroll;
    }

    private int availableBodyHeight() {
        return Math.max(BODY_MIN_HEIGHT, this.screen.screenHeight() - BODY_RESERVED_HEIGHT);
    }

    private WideLayoutMetrics wideLayoutMetrics(int shellWidth) {
        int tabsMin = 40;
        int previewMin = 72;
        int tabsWidth = this.clamp((int) Math.round(shellWidth * 0.11), tabsMin, 150);
        int previewWidth = this.clamp((int) Math.round(shellWidth * 0.19), previewMin, 220);
        int editorWidth = shellWidth - tabsWidth - previewWidth - 16;

        if (editorWidth < EDITOR_TARGET_MIN_WIDTH) {
            int deficit = EDITOR_TARGET_MIN_WIDTH - editorWidth;
            int previewCut = Math.min(deficit, Math.max(0, previewWidth - previewMin));
            previewWidth -= previewCut;
            deficit -= previewCut;

            int tabsCut = Math.min(deficit, Math.max(0, tabsWidth - tabsMin));
            tabsWidth -= tabsCut;

            editorWidth = shellWidth - tabsWidth - previewWidth - 16;
        }

        if (editorWidth < 220) {
            int deficit = 220 - editorWidth;
            int previewCut = Math.min(deficit, Math.max(0, previewWidth - 64));
            previewWidth -= previewCut;
            deficit -= previewCut;

            int tabsCut = Math.min(deficit, Math.max(0, tabsWidth - 36));
            tabsWidth -= tabsCut;

            editorWidth = shellWidth - tabsWidth - previewWidth - 16;
        }

        return new WideLayoutMetrics(tabsWidth, editorWidth, previewWidth);
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private ButtonComponent topActionButton(Component fullText, int maxTextWidth, Consumer<ButtonComponent> onPress) {
        ButtonComponent button = UiFactory.button(UiFactory.fitToWidth(fullText, maxTextWidth), onPress);
        button.horizontalSizing(Sizing.content());
        button.tooltip(List.of(fullText));
        return button;
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
            LabelComponent previewNameLabel,
            LabelComponent rawDataStatusLabel,
            ItemComponent previewItem,
            ButtonComponent applyButton,
            ButtonComponent resetButton,
            int previewTextWidthHint
    ) {
    }

    private record WideLayoutMetrics(int tabsWidth, int editorWidth, int previewWidth) {
    }
}
