package me.noramibu.itemeditor.ui.screen;

import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.DropdownComponent;
import io.wispforest.owo.ui.component.ItemComponent;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.component.UIComponents;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.container.StackLayout;
import io.wispforest.owo.ui.container.UIContainers;
import io.wispforest.owo.ui.core.OwoUIAdapter;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.Surface;
import me.noramibu.itemeditor.editor.EditorCategory;
import me.noramibu.itemeditor.editor.EditorModule;
import me.noramibu.itemeditor.editor.EditorModuleRegistry;
import me.noramibu.itemeditor.editor.ItemEditorSession;
import me.noramibu.itemeditor.editor.ValidationMessage;
import me.noramibu.itemeditor.ui.component.UiFactory;
import me.noramibu.itemeditor.ui.panel.EditorPanel;
import me.noramibu.itemeditor.ui.util.ScrollStateUtil;
import me.noramibu.itemeditor.util.ItemEditorCapabilities;
import me.noramibu.itemeditor.util.ItemEditorText;
import me.noramibu.itemeditor.util.RawItemDataUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import java.util.ArrayList;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntConsumer;

public final class ItemEditorScreen extends BaseOwoScreen<StackLayout> {

    private final ItemEditorSession session;
    private final List<EditorModule> modules;
    private final ItemEditorDialogController dialogController;

    private EditorModule selectedModule;
    private StackLayout rootLayout;
    private FlowLayout tabs;
    private FlowLayout panelHost;
    private ScrollContainer<FlowLayout> panelScroll;
    private ScrollContainer<FlowLayout> tooltipScroll;
    private ScrollContainer<FlowLayout> messageScroll;
    private FlowLayout tooltipLines;
    private FlowLayout messages;
    private FlowLayout activeDialog;
    private LabelComponent selectedCategoryLabel;
    private LabelComponent applyModeLabel;
    private LabelComponent previewNameLabel;
    private LabelComponent rawDataStatusLabel;
    private ItemComponent previewItem;
    private ButtonComponent applyButton;
    private ButtonComponent resetButton;
    private int previewTextWidthHint = 210;
    private boolean sessionListenerRegistered;
    private Double pendingPanelScrollOffset;
    private Double pendingTooltipScrollOffset;
    private Double pendingMessageScrollOffset;

    public ItemEditorScreen(ItemEditorSession session) {
        super(ItemEditorText.tr("screen.title"));
        this.session = session;
        this.modules = EditorModuleRegistry.modules().stream().filter(this::isModuleEnabled).toList();
        this.selectedModule = this.modules.getFirst();
        this.dialogController = new ItemEditorDialogController(this);
    }

    @Override
    protected OwoUIAdapter<StackLayout> createAdapter() {
        return OwoUIAdapter.create(this, UIContainers::stack);
    }

    @Override
    protected void build(StackLayout root) {
        this.rootLayout = root;
        root.surface(Surface.blur(4, 8).and(Surface.flat(0x6610151A)));

        ItemEditorLayoutBuilder.BuildResult layout = new ItemEditorLayoutBuilder(this).build();
        root.child(layout.shell());
        this.tabs = layout.tabs();
        this.panelHost = layout.panelHost();
        this.panelScroll = layout.panelScroll();
        this.tooltipScroll = layout.tooltipScroll();
        this.messageScroll = layout.messageScroll();
        this.tooltipLines = layout.tooltipLines();
        this.messages = layout.messages();
        this.selectedCategoryLabel = layout.selectedCategoryLabel();
        this.applyModeLabel = layout.applyModeLabel();
        this.previewNameLabel = layout.previewNameLabel();
        this.rawDataStatusLabel = layout.rawDataStatusLabel();
        this.previewItem = layout.previewItem();
        this.applyButton = layout.applyButton();
        this.resetButton = layout.resetButton();
        this.previewTextWidthHint = layout.previewTextWidthHint();

        if (!this.sessionListenerRegistered) {
            this.session.addListener(this::refreshPreview);
            this.sessionListenerRegistered = true;
        }
        this.refreshTabs();
        this.refreshCurrentPanel(false);
        this.refreshPreview();
        this.restorePendingScrollState();
        this.schedulePostLayoutTabRefresh();
    }

    public ItemEditorSession session() {
        return this.session;
    }

    public void refreshPreview() {
        if (this.previewItem != null) {
            this.previewItem.stack(this.session.previewStack());
        }

        if (this.previewNameLabel != null) {
            this.previewNameLabel.text(this.session.previewStack().getHoverName());
        }

        if (this.applyModeLabel != null) {
            this.applyModeLabel.text(Component.literal(this.applyModeText()).withStyle(this.applyModeColor()));
        }

        if (this.tooltipLines != null) {
            this.tooltipLines.clearChildren();
            var context = this.minecraft.level != null
                    ? Item.TooltipContext.of(this.minecraft.level)
                    : Item.TooltipContext.of(this.session.registryAccess());
            this.session.previewStack().getTooltipLines(context, this.minecraft.player, TooltipFlag.NORMAL).forEach(this::addTooltipLine);
            ScrollStateUtil.sync(this.tooltipScroll);
        }

        if (this.messages != null) {
            this.messages.clearChildren();
            if (this.session.messages().isEmpty()) {
                this.messages.child(UiFactory.muted(ItemEditorText.tr("screen.validation.none"), this.previewTextWidthHint));
            } else {
                for (ValidationMessage message : this.session.messages()) {
                    this.messages.child(UiFactory.message(message.message(), this.messageColor(message)).maxWidth(this.previewTextWidthHint));
                }
            }
            ScrollStateUtil.sync(this.messageScroll);
        }

        if (this.applyButton != null) {
            this.applyButton.active(!this.session.hasErrors());
        }
        if (this.resetButton != null) {
            this.resetButton.active(this.session.dirty());
        }
        if (this.rawDataStatusLabel != null) {
            this.rawDataStatusLabel.text(Component.literal(this.rawDataStatusText()));
        }
    }

    public void refreshCurrentPanel() {
        this.refreshCurrentPanel(true);
    }

    private void refreshCurrentPanel(boolean preserveScroll) {
        if (this.panelHost == null) return;

        double scrollAmount = preserveScroll ? ScrollStateUtil.offset(this.panelScroll) : 0;
        Component fullCategoryTitle = this.categoryTitle(this.selectedModule);
        this.selectedCategoryLabel.text(UiFactory.fitToWidth(fullCategoryTitle, this.categoryHeaderTextWidthHint()));
        this.selectedCategoryLabel.tooltip(this.categoryTooltip(fullCategoryTitle, this.categoryDescription(this.selectedModule)));
        this.panelHost.clearChildren();

        EditorPanel panel = this.selectedModule.panelFactory().apply(this);
        this.panelHost.child(panel.build());
        this.panelHost.child(UIContainers.verticalFlow(Sizing.fill(100), Sizing.fixed(120)));
        this.restorePanelScroll(scrollAmount);
        this.refreshTabs();
    }

    private void refreshTabs() {
        if (this.tabs == null) return;

        this.tabs.clearChildren();
        int textWidthHint = this.categoryButtonTextWidthHint();
        for (EditorModule module : this.modules) {
            Component fullTitle = this.categoryTitle(module);
            Component description = this.categoryDescription(module);
            ButtonComponent button = UiFactory.button(UiFactory.fitToWidth(fullTitle, textWidthHint), component -> {
                this.selectedModule = module;
                this.refreshCurrentPanel(false);
            });
            button.active(module != this.selectedModule);
            button.tooltip(this.categoryTooltip(fullTitle, description));
            this.tabs.child(button.horizontalSizing(Sizing.fill(100)));
        }
    }

    private int categoryButtonTextWidthHint() {
        int tabsWidth = this.tabs.width();
        if (tabsWidth <= 0) {
            tabsWidth = Math.max(64, Math.min(180, this.screenWidth() / 6));
        }
        return Math.max(24, tabsWidth - 16);
    }

    public <T> void openDropdown(ButtonComponent anchor, List<T> values, Function<T, String> labelMapper, Consumer<T> selectionConsumer) {
        if (this.rootLayout == null || values.isEmpty()) {
            return;
        }

        double menuX = Math.clamp(anchor.x(), 4, this.screenWidth() - 4);
        double preferredY = anchor.y() + anchor.height() + 2;
        double estimatedHeight = Math.min(220, values.size() * 18d + 12);
        double menuY = preferredY;
        if (preferredY + estimatedHeight > this.screenHeight() - 4) {
            menuY = Math.max(4, anchor.y() - estimatedHeight - 2);
        }

        DropdownComponent dropdown = DropdownComponent.openContextMenu(
                this,
                this.rootLayout,
                (parent, menu) -> parent.child(menu),
                menuX,
                menuY,
                menu -> values.forEach(value ->
                        menu.button(Component.literal(labelMapper.apply(value)), dropdownComponent -> {
                            selectionConsumer.accept(value);
                            dropdownComponent.remove();
                            this.refreshCurrentPanel(true);
                            this.refreshPreview();
                        })
                )
        );
        dropdown.closeWhenNotHovered(false);
    }

    public void openSearchablePickerDialog(
            String title,
            String body,
            List<String> values,
            Function<String, String> labelMapper,
            Consumer<String> selectionConsumer
    ) {
        this.dialogController.openSearchablePickerDialog(title, body, values, labelMapper, selectionConsumer);
    }

    public void requestReset() {
        this.dialogController.requestReset();
    }

    public void requestApply() {
        this.dialogController.requestApply();
    }

    public void requestClose() {
        this.dialogController.requestClose();
    }

    public void openColorPickerDialog(String title, int initialRgb, IntConsumer onApply) {
        this.dialogController.openColorPickerDialog(title, initialRgb, onApply);
    }

    public void openGradientPickerDialog(String title, int initialStartRgb, int initialEndRgb, BiConsumer<Integer, Integer> onApply) {
        this.dialogController.openGradientPickerDialog(title, initialStartRgb, initialEndRgb, onApply);
    }

    public void openRawItemDataDialog(String title, boolean previewData) {
        this.dialogController.openRawItemDataDialog(title, previewData);
    }

    @Override
    public void resize(int width, int height) {
        this.captureScrollStateForRebuild();
        this.clearDialog();
        if (this.uiAdapter != null) {
            this.uiAdapter.dispose();
            this.uiAdapter = null;
        }
        super.resize(width, height);
    }

    @Override
    public void onClose() {
        this.requestClose();
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return this.dialogController.shouldCloseOnEsc();
    }

    void attachDialog(FlowLayout dialog) {
        this.clearDialog();
        this.activeDialog = dialog;
        if (this.rootLayout != null) {
            this.rootLayout.child(dialog);
        }
    }

    void clearDialog() {
        if (this.activeDialog == null || this.rootLayout == null) return;
        this.rootLayout.removeChild(this.activeDialog);
        this.activeDialog = null;
    }

    boolean hasActiveDialog() {
        return this.activeDialog != null;
    }

    EditorModule selectedModule() {
        return this.selectedModule;
    }

    int screenWidth() {
        return this.width;
    }

    int screenHeight() {
        return this.height;
    }

    Component categoryTitle(EditorModule module) {
        if (module.category() == EditorCategory.SPECIAL_DATA) {
            return ItemEditorCapabilities.specialDataTitle(this.session.originalStack());
        }
        return module.category().title();
    }

    Component categoryDescription(EditorModule module) {
        if (module.category() == EditorCategory.SPECIAL_DATA) {
            return ItemEditorCapabilities.specialDataDescription(this.session.originalStack());
        }
        return module.category().description();
    }

    String applyModeText() {
        if (this.minecraft.player != null && this.minecraft.player.hasInfiniteMaterials()) {
            return ItemEditorText.str("screen.mode.creative");
        }
        if (this.minecraft.hasSingleplayerServer()) {
            return ItemEditorText.str("screen.mode.singleplayer");
        }
        return ItemEditorText.str("screen.mode.multiplayer");
    }

    int applyModeColorInt() {
        return switch (this.applyModeColor()) {
            case GREEN -> 0x7ED67A;
            case GOLD -> 0xF2C26B;
            default -> 0xFF8A8A;
        };
    }

    String rawDataStatusText() {
        String originalData = RawItemDataUtil.serialize(this.session.originalStack(), this.session.registryAccess());
        String previewData = RawItemDataUtil.serialize(this.session.previewStack(), this.session.registryAccess());
        return originalData.equals(previewData)
                ? ItemEditorText.str("screen.raw_data.matches")
                : ItemEditorText.str("screen.raw_data.differs");
    }

    private ChatFormatting applyModeColor() {
        if (this.minecraft.player != null && this.minecraft.player.hasInfiniteMaterials()) {
            return ChatFormatting.GREEN;
        }
        if (this.minecraft.hasSingleplayerServer()) {
            return ChatFormatting.GOLD;
        }
        return ChatFormatting.RED;
    }

    private int messageColor(ValidationMessage message) {
        return switch (message.severity()) {
            case ERROR -> 0xFF8A8A;
            case WARNING -> 0xF2C26B;
            case INFO -> 0x7EC8F8;
        };
    }

    private int categoryHeaderTextWidthHint() {
        int widthHint = this.panelScroll != null ? this.panelScroll.width() - 16 : 0;
        if (widthHint <= 0) {
            widthHint = Math.max(120, this.screenWidth() / 3);
        }
        return widthHint;
    }

    private void restorePanelScroll(double scrollAmount) {
        ScrollStateUtil.restore(this.panelScroll, scrollAmount);
        if (this.minecraft != null) {
            this.minecraft.execute(() -> ScrollStateUtil.restore(this.panelScroll, scrollAmount));
        }
    }

    private void captureScrollStateForRebuild() {
        this.pendingPanelScrollOffset = ScrollStateUtil.offset(this.panelScroll);
        this.pendingTooltipScrollOffset = ScrollStateUtil.offset(this.tooltipScroll);
        this.pendingMessageScrollOffset = ScrollStateUtil.offset(this.messageScroll);
    }

    private void restorePendingScrollState() {
        if (this.pendingPanelScrollOffset != null) {
            ScrollStateUtil.restore(this.panelScroll, this.pendingPanelScrollOffset);
            this.pendingPanelScrollOffset = null;
        }
        if (this.pendingTooltipScrollOffset != null) {
            ScrollStateUtil.restore(this.tooltipScroll, this.pendingTooltipScrollOffset);
            this.pendingTooltipScrollOffset = null;
        }
        if (this.pendingMessageScrollOffset != null) {
            ScrollStateUtil.restore(this.messageScroll, this.pendingMessageScrollOffset);
            this.pendingMessageScrollOffset = null;
        }
    }

    private boolean isModuleEnabled(EditorModule module) {
        return module.enabled().test(this.session);
    }

    private void addTooltipLine(Component line) {
        this.tooltipLines.child(UIComponents.label(line).maxWidth(this.previewTextWidthHint));
    }

    private List<Component> categoryTooltip(Component title, Component description) {
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(title);
        if (!description.getString().isBlank()) {
            tooltip.add(description);
        }
        return tooltip;
    }

    private void schedulePostLayoutTabRefresh() {
        if (this.minecraft == null) {
            return;
        }

        this.minecraft.execute(this::refreshTabs);
    }
}
