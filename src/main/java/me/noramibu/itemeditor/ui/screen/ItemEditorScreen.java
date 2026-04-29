package me.noramibu.itemeditor.ui.screen;

import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.DropdownComponent;
import io.wispforest.owo.ui.component.ItemComponent;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.container.StackLayout;
import io.wispforest.owo.ui.container.UIContainers;
import io.wispforest.owo.ui.core.OwoUIAdapter;
import io.wispforest.owo.ui.core.Surface;
import me.noramibu.itemeditor.editor.EditorCategory;
import me.noramibu.itemeditor.editor.EditorModule;
import me.noramibu.itemeditor.editor.EditorModuleRegistry;
import me.noramibu.itemeditor.editor.ItemEditorSession;
import me.noramibu.itemeditor.editor.ValidationMessage;
import me.noramibu.itemeditor.ui.component.UiFactory;
import me.noramibu.itemeditor.ui.util.ScrollStateUtil;
import me.noramibu.itemeditor.util.ItemEditorCapabilities;
import me.noramibu.itemeditor.util.ItemEditorText;
import net.minecraft.core.component.DataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntConsumer;

public final class ItemEditorScreen extends BaseOwoScreen<StackLayout> {
    private static final float PREVIEW_UI_SCALE = 0.70F;
    private static final float PREVIEW_STATUS_TEXT_SCALE = 0.90F;
    private static final int PANEL_SCROLLBAR_BASE_THICKNESS = 8;
    private static final int PREVIEW_SCROLLBAR_BASE_THICKNESS = 7;
    private static final int ROOT_BLUR_RADIUS = 4;
    private static final int ROOT_BLUR_QUALITY = 8;
    private static final int ROOT_SURFACE_TINT = 0x6610151A;
    private static final int PREVIEW_TEXT_RENDER_MIN_WIDTH = 40;
    private static final int PREVIEW_TEXT_RIGHT_SAFETY_BASE = 2;
    private static final int APPLY_MODE_TEXT_RENDER_MIN_WIDTH = 40;
    private static final int PREVIEW_NAME_EXTRA_RESERVE = 24;
    private static final int APPLY_MODE_TEXT_WIDTH_HINT_DEFAULT = 220;
    private static final int PREVIEW_TEXT_WIDTH_HINT_DEFAULT = 210;
    private static final int DROPDOWN_VIEWPORT_INSET = 4;
    private static final int DROPDOWN_ESTIMATED_MAX_HEIGHT = 220;
    private static final int DROPDOWN_MIN_ESTIMATED_WIDTH = 120;
    private static final int DROPDOWN_MAX_ESTIMATED_WIDTH = 360;
    private static final int DROPDOWN_WIDTH_CHROME_RESERVE = 30;
    private static final int DROPDOWN_WIDTH_SAMPLE_LIMIT = 64;
    private static final int DROPDOWN_VIEWPORT_HEIGHT_MIN_BUDGET = 24;
    private static final int DROPDOWN_ANCHOR_VERTICAL_GAP = 2;
    private static final double DROPDOWN_ROW_HEIGHT_ESTIMATE = 18d;
    private static final int DROPDOWN_CHROME_RESERVE = 12;
    private static final int DROPDOWN_ROW_HEIGHT_EXTRA_CHROME = 4;
    private static final int SHELL_MAX_WIDTH = 1600;
    private static final int SHELL_SIDE_PADDING_TOTAL = 16;
    private static final int BODY_GAP_BASE = 8;
    private static final int RAIL_TOGGLE_BASE = 16;
    private static final int ESTIMATED_TABS_MIN = 56;
    private static final int ESTIMATED_TABS_MAX = 140;
    private static final double ESTIMATED_TABS_RATIO = 0.09d;
    private static final int ESTIMATED_PREVIEW_MIN = 96;
    private static final int ESTIMATED_PREVIEW_MAX = 220;
    private static final double ESTIMATED_PREVIEW_RATIO = 0.22d;
    private static final int UNMEASURED_RESERVE_BASE = 42;
    private static final int UNMEASURED_RESERVE_EXTRA = 16;
    private static final int EDITOR_CONTENT_WIDTH_FLOOR = 132;
    private static final double EDITOR_CONTENT_VIEWPORT_FLOOR_RATIO = 0.34d;
    private static final int EDITOR_CONTENT_HEIGHT_FLOOR = 140;
    private static final double EDITOR_CONTENT_HEIGHT_VIEWPORT_RATIO = 0.72d;
    private static final int RESPONSIVE_SIGNATURE_SEED = 17;
    private static final int RESPONSIVE_SIGNATURE_MULTIPLIER = 31;
    private static final int EDITOR_CONTENT_HINT_CHROME_BASE = 4;
    private static final int RESPONSIVE_RELAYOUT_DELAY_TICKS = 0;
    private static final int RESPONSIVE_RELAYOUT_PASS_BUDGET = 4;
    private static final int RESPONSIVE_RELAYOUT_MAX_WAIT_TICKS = 40;
    private static final int COLOR_MODE_CREATIVE = 0x7ED67A;
    private static final int COLOR_MODE_SINGLEPLAYER = 0xF2C26B;
    private static final int COLOR_MODE_MULTIPLAYER = 0xFF8A8A;
    private static final int COLOR_MESSAGE_ERROR = 0xFF8A8A;
    private static final int COLOR_MESSAGE_WARNING = 0xF2C26B;
    private static final int COLOR_MESSAGE_INFO = 0x7EC8F8;
    private static final String EMPTY_TEXT = "";

    private final ItemEditorSession session;
    private final ItemEditorDialogController dialogController;
    private final ItemEditorCategoryController categoryController;

    private EditorModule selectedModule;
    private StackLayout rootLayout;
    private ScrollContainer<FlowLayout> panelScroll;
    private ScrollContainer<FlowLayout> tooltipScroll;
    private ScrollContainer<FlowLayout> messageScroll;
    private FlowLayout tooltipLines;
    private FlowLayout messages;
    private FlowLayout activeDialog;
    private LabelComponent applyModeLabel;
    private int applyModeTextWidthHint = APPLY_MODE_TEXT_WIDTH_HINT_DEFAULT;
    private LabelComponent previewNameLabel;
    private ItemComponent previewItem;
    private ButtonComponent applyButton;
    private ButtonComponent resetButton;
    private int previewTextWidthHint = PREVIEW_TEXT_WIDTH_HINT_DEFAULT;
    private boolean sessionListenerRegistered;
    private boolean previewTooltipCollapsed;
    private boolean previewValidationCollapsed;
    private boolean categoriesRailCollapsed;
    private boolean previewRailCollapsed;
    private boolean pendingInitialResponsiveRefresh;
    private int initialRelayoutDelayTicks;
    private int initialRelayoutPassBudget;
    private int initialRelayoutWaitTicks;
    private boolean initialRelayoutRanAtLeastOnce;
    private Double pendingPanelScrollOffset;
    private Double pendingTooltipScrollOffset;
    private Double pendingMessageScrollOffset;

    public ItemEditorScreen(ItemEditorSession session) {
        super(ItemEditorText.tr("screen.title"));
        this.session = session;
        this.categoriesRailCollapsed = session.state().uiCategoriesRailCollapsed;
        this.previewRailCollapsed = session.state().uiPreviewRailCollapsed;
        this.previewTooltipCollapsed = session.state().uiPreviewTooltipCollapsed;
        this.previewValidationCollapsed = session.state().uiPreviewValidationCollapsed;
        List<EditorModule> modules = EditorModuleRegistry.modules().stream()
                .filter(module -> module.enabled().test(this.session))
                .toList();
        this.selectedModule = modules.getFirst();
        this.dialogController = new ItemEditorDialogController(this);
        this.categoryController = new ItemEditorCategoryController(this, modules);
    }

    @Override
    protected @NotNull OwoUIAdapter<StackLayout> createAdapter() {
        OwoUIAdapter<StackLayout> adapter = OwoUIAdapter.create(this, UIContainers::stack);
        adapter.enableInspector = false;
        adapter.globalInspector = false;
        return adapter;
    }

    @Override
    protected void build(StackLayout root) {
        this.rootLayout = root;
        root.clearChildren();
        root.surface(Surface.blur(ROOT_BLUR_RADIUS, ROOT_BLUR_QUALITY).and(Surface.flat(ROOT_SURFACE_TINT)));

        ItemEditorLayoutBuilder.BuildResult layout = new ItemEditorLayoutBuilder(this).build();
        root.child(layout.shell());
        FlowLayout tabs = layout.tabs();
        FlowLayout panelHost = layout.panelHost();
        this.panelScroll = layout.panelScroll();
        this.tooltipScroll = layout.tooltipScroll();
        this.messageScroll = layout.messageScroll();
        this.tooltipLines = layout.tooltipLines();
        this.messages = layout.messages();
        LabelComponent selectedCategoryLabel = layout.selectedCategoryLabel();
        this.applyModeLabel = layout.applyModeLabel();
        this.applyModeTextWidthHint = layout.applyModeTextWidthHint();
        this.previewNameLabel = layout.previewNameLabel();
        this.previewItem = layout.previewItem();
        this.applyButton = layout.applyButton();
        this.resetButton = layout.resetButton();
        this.previewTextWidthHint = layout.previewTextWidthHint();
        this.categoryController.bind(tabs, panelHost, this.panelScroll, selectedCategoryLabel);

        if (!this.sessionListenerRegistered) {
            this.session.addListener(this::refreshPreview);
            this.sessionListenerRegistered = true;
        }
        this.categoryController.refreshTabs();
        this.categoryController.refreshCurrentPanel(false);
        this.refreshPreview();
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
        this.requestResponsiveRelayout();
    }

    public ItemEditorSession session() {
        return this.session;
    }

    public void refreshPreview() {
        if (this.previewItem != null) {
            this.previewItem.stack(this.session.previewStack());
        }

        if (this.previewNameLabel != null) {
            Component fullName = this.session.previewStack().getHoverName();
            int maxWidth = Math.max(PREVIEW_TEXT_RENDER_MIN_WIDTH, this.previewTextContentWidth() - UiFactory.scaledPixels(PREVIEW_NAME_EXTRA_RESERVE));
            this.previewNameLabel.text(UiFactory.fitToWidth(fullName, maxWidth));
            this.previewNameLabel.tooltip(List.of(fullName));
        }

        if (this.applyModeLabel != null) {
            Component fullApplyModeText = Component.literal(this.applyModeText()).withStyle(this.applyModeColor());
            this.applyModeLabel.text(UiFactory.fitToWidth(fullApplyModeText, Math.max(APPLY_MODE_TEXT_RENDER_MIN_WIDTH, this.applyModeTextWidthHint)));
            this.applyModeLabel.tooltip(List.of(fullApplyModeText));
        }

        if (this.tooltipLines != null) {
            this.tooltipLines.clearChildren();
            var context = this.minecraft.level != null
                    ? Item.TooltipContext.of(this.minecraft.level)
                    : Item.TooltipContext.of(this.session.registryAccess());
            var tooltipStack = this.session.hasErrors() ? this.session.originalStack() : this.session.previewStack();
            int tooltipContentWidth = this.previewTextContentWidth();
            int scaledTooltipWidth = this.scaledTextWidth(tooltipContentWidth, PREVIEW_UI_SCALE);
            this.safeTooltipLines(tooltipStack, context).stream()
                    .map(line -> UiFactory.bodyLabel(line, PREVIEW_UI_SCALE).maxWidth(scaledTooltipWidth))
                    .forEach(this.tooltipLines::child);
            ScrollStateUtil.sync(this.tooltipScroll);
        }

        if (this.messages != null) {
            this.messages.clearChildren();
            int contentWidth = this.previewTextContentWidth();
            int scaledContentWidth = this.scaledTextWidth(contentWidth, PREVIEW_UI_SCALE);
            if (this.session.messages().isEmpty()) {
                this.messages.child(UiFactory.muted(ItemEditorText.tr("screen.validation.none"), this.scaledTextWidth(contentWidth, PREVIEW_STATUS_TEXT_SCALE), PREVIEW_STATUS_TEXT_SCALE));
            } else {
                for (ValidationMessage message : this.session.messages()) {
                    int color = switch (message.severity()) {
                        case ERROR -> COLOR_MESSAGE_ERROR;
                        case WARNING -> COLOR_MESSAGE_WARNING;
                        case INFO -> COLOR_MESSAGE_INFO;
                    };
                    this.messages.child(UiFactory.message(Component.literal(message.message()), color, PREVIEW_UI_SCALE).maxWidth(scaledContentWidth));
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
    }

    public void refreshCurrentPanel() {
        this.categoryController.refreshCurrentPanel(true);
    }

    public <T> void openDropdown(ButtonComponent anchor, List<T> values, Function<T, String> labelMapper, Consumer<T> selectionConsumer) {
        if (this.rootLayout == null || values.isEmpty()) {
            return;
        }

        int viewportWidth = this.screenWidth();
        int viewportHeight = this.screenHeight();
        int hardCapTextWidth = Math.max(0, DROPDOWN_MAX_ESTIMATED_WIDTH - UiFactory.scaledPixels(DROPDOWN_WIDTH_CHROME_RESERVE));
        int sampledLimit = Math.min(values.size(), DROPDOWN_WIDTH_SAMPLE_LIMIT);
        int maxTextWidth = 0;
        for (int index = 0; index < sampledLimit; index++) {
            maxTextWidth = Math.max(maxTextWidth, this.dropdownLabelWidth(values.get(index), labelMapper));
            if (maxTextWidth >= hardCapTextWidth) {
                break;
            }
        }
        if (maxTextWidth < hardCapTextWidth && sampledLimit < values.size()) {
            int remaining = values.size() - sampledLimit;
            int stride = Math.max(1, remaining / Math.max(1, DROPDOWN_WIDTH_SAMPLE_LIMIT));
            for (int index = sampledLimit; index < values.size(); index += stride) {
                maxTextWidth = Math.max(maxTextWidth, this.dropdownLabelWidth(values.get(index), labelMapper));
                if (maxTextWidth >= hardCapTextWidth) {
                    break;
                }
            }
        }
        int estimatedWidth = Math.clamp(
                maxTextWidth + UiFactory.scaledPixels(DROPDOWN_WIDTH_CHROME_RESERVE),
                DROPDOWN_MIN_ESTIMATED_WIDTH,
                DROPDOWN_MAX_ESTIMATED_WIDTH
        );
        double maxMenuX = Math.max(DROPDOWN_VIEWPORT_INSET, viewportWidth - DROPDOWN_VIEWPORT_INSET - estimatedWidth);
        double menuX = Math.clamp(anchor.x(), DROPDOWN_VIEWPORT_INSET, maxMenuX);
        double preferredY = anchor.y() + anchor.height() + DROPDOWN_ANCHOR_VERTICAL_GAP;
        double estimatedRowHeight = Math.max(
                DROPDOWN_ROW_HEIGHT_ESTIMATE,
                UiFactory.scaleProfile().controlHeight() + UiFactory.scaledPixels(DROPDOWN_ROW_HEIGHT_EXTRA_CHROME)
        );
        double estimatedHeight = Math.min(
                DROPDOWN_ESTIMATED_MAX_HEIGHT,
                values.size() * estimatedRowHeight + UiFactory.scaledPixels(DROPDOWN_CHROME_RESERVE)
        );
        double viewportHeightBudget = Math.max(
                DROPDOWN_VIEWPORT_HEIGHT_MIN_BUDGET,
                viewportHeight - (DROPDOWN_VIEWPORT_INSET * 2)
        );
        estimatedHeight = Math.min(estimatedHeight, viewportHeightBudget);
        double menuY = preferredY;
        if (preferredY + estimatedHeight > viewportHeight - DROPDOWN_VIEWPORT_INSET) {
            menuY = Math.max(DROPDOWN_VIEWPORT_INSET, anchor.y() - estimatedHeight - DROPDOWN_ANCHOR_VERTICAL_GAP);
        }
        double maxMenuY = Math.max(DROPDOWN_VIEWPORT_INSET, viewportHeight - DROPDOWN_VIEWPORT_INSET - estimatedHeight);
        menuY = Math.clamp(menuY, DROPDOWN_VIEWPORT_INSET, maxMenuY);

        DropdownComponent dropdown = DropdownComponent.openContextMenu(
                this,
                this.rootLayout,
                StackLayout::child,
                menuX,
                menuY,
                menu -> values.forEach(value ->
                        menu.button(Component.literal(this.dropdownLabelText(value, labelMapper)), dropdownComponent -> {
                            selectionConsumer.accept(value);
                            dropdownComponent.remove();
                            this.refreshCurrentPanel();
                            this.refreshPreview();
                        })
                )
        );
        dropdown.closeWhenNotHovered(false);
    }

    private <T> int dropdownLabelWidth(T value, Function<T, String> labelMapper) {
        String label = this.dropdownLabelText(value, labelMapper);
        return this.minecraft.font.width(label);
    }

    private <T> String dropdownLabelText(T value, Function<T, String> labelMapper) {
        String mapped = labelMapper.apply(value);
        return mapped == null ? EMPTY_TEXT : mapped;
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
        this.session.flushQueuedRebuild();
        this.dialogController.requestReset();
    }

    public void requestApply() {
        this.session.flushQueuedRebuild();
        this.dialogController.requestApply();
    }

    public void requestClose() {
        this.session.flushQueuedRebuild();
        this.dialogController.requestClose();
    }

    public void openColorPickerDialog(String title, int initialRgb, IntConsumer onApply) {
        this.dialogController.openColorPickerDialog(title, initialRgb, onApply);
    }

    public void openGradientPickerDialog(String title, int initialStartRgb, int initialEndRgb, BiConsumer<Integer, Integer> onApply) {
        this.dialogController.openGradientPickerDialog(title, initialStartRgb, initialEndRgb, onApply);
    }

    public void openRichTextHeadDialog(String title, Consumer<String> onApply) {
        this.dialogController.openRichTextHeadDialog(title, onApply);
    }

    public void openRichTextSpriteDialog(String title, Consumer<String> onApply) {
        this.dialogController.openRichTextSpriteDialog(title, onApply);
    }

    public void openRichTextEventDialog(String title, boolean includeHoverModes, boolean includeSuggestCommand, String initialText, Consumer<String> onApply) {
        this.dialogController.openRichTextEventDialog(title, includeHoverModes, includeSuggestCommand, initialText, onApply);
    }

    public void openRichTextEventDialog(String title, boolean includeHoverModes, Consumer<String> onApply) {
        this.openRichTextEventDialog(title, includeHoverModes, true, "text", onApply);
    }

    public void openRawItemDataDialog(String title, boolean previewData) {
        this.dialogController.openRawItemDataDialog(title, previewData);
    }

    @Override
    public void resize(int width, int height) {
        this.pendingPanelScrollOffset = ScrollStateUtil.offset(this.panelScroll);
        this.pendingTooltipScrollOffset = ScrollStateUtil.offset(this.tooltipScroll);
        this.pendingMessageScrollOffset = ScrollStateUtil.offset(this.messageScroll);
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

    @Override
    public boolean keyPressed(KeyEvent input) {
        if (this.uiAdapter != null && this.uiAdapter.enableInspector) {
            this.uiAdapter.enableInspector = false;
            this.uiAdapter.globalInspector = false;
        }

        if (input.key() == GLFW.GLFW_KEY_LEFT_SHIFT && input.hasControlDownWithQuirk()) {
            return true;
        }

        if (this.dialogController.handleDialogShortcut(input)) {
            return true;
        }

        if (input.hasControlDownWithQuirk()) {
            if (input.key() == GLFW.GLFW_KEY_S) {
                this.requestApply();
                return true;
            }
            if (input.key() == GLFW.GLFW_KEY_R) {
                this.requestReset();
                return true;
            }
            if (input.key() == GLFW.GLFW_KEY_TAB) {
                this.categoryController.selectAdjacentCategory(input.hasShiftDown() ? -1 : 1);
                return true;
            }
        }
        return super.keyPressed(input);
    }

    @Override
    public void tick() {
        super.tick();
        this.session.tick();
        this.runPendingInitialResponsiveRefresh();
    }

    void attachDialog(FlowLayout dialog) {
        this.clearDialog();
        this.activeDialog = dialog;
        if (this.rootLayout != null) {
            this.rootLayout.child(dialog);
        }
    }

    void clearDialog() {
        if (this.activeDialog == null) return;
        if (this.rootLayout != null) {
            this.rootLayout.removeChild(this.activeDialog);
        }
        this.activeDialog = null;
    }

    boolean isDialogClosed() {
        return this.activeDialog == null;
    }

    EditorModule selectedModule() {
        return this.selectedModule;
    }

    void setSelectedModule(EditorModule module) {
        this.selectedModule = module;
    }

    int screenWidth() {
        return this.width;
    }

    int screenHeight() {
        return this.height;
    }

    boolean previewTooltipCollapsed() {
        return this.previewTooltipCollapsed;
    }

    void setPreviewTooltipCollapsed(boolean value) {
        this.previewTooltipCollapsed = value;
        this.session.state().uiPreviewTooltipCollapsed = value;
    }

    boolean previewValidationCollapsed() {
        return this.previewValidationCollapsed;
    }

    void setPreviewValidationCollapsed(boolean value) {
        this.previewValidationCollapsed = value;
        this.session.state().uiPreviewValidationCollapsed = value;
    }

    boolean categoriesRailCollapsed() {
        return this.categoriesRailCollapsed;
    }

    void setCategoriesRailCollapsed(boolean value) {
        this.categoriesRailCollapsed = value;
        this.session.state().uiCategoriesRailCollapsed = value;
    }

    boolean previewRailCollapsed() {
        return this.previewRailCollapsed;
    }

    void setPreviewRailCollapsed(boolean value) {
        this.previewRailCollapsed = value;
        this.session.state().uiPreviewRailCollapsed = value;
    }

    void rebuildLayout() {
        this.resize(this.width, this.height);
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
        if (this.isCreativeMode()) {
            return ItemEditorText.str("screen.mode.creative");
        }
        if (this.isSingleplayerMode()) {
            return ItemEditorText.str("screen.mode.singleplayer");
        }
        return ItemEditorText.str("screen.mode.multiplayer");
    }

    int applyModeColorInt() {
        return switch (this.applyModeColor()) {
            case GREEN -> COLOR_MODE_CREATIVE;
            case GOLD -> COLOR_MODE_SINGLEPLAYER;
            default -> COLOR_MODE_MULTIPLAYER;
        };
    }

    private List<Component> safeTooltipLines(ItemStack stack, Item.TooltipContext context) {
        ItemStack safe = stack.copy();
        if (safe.has(DataComponents.BUNDLE_CONTENTS)) {
            safe.remove(DataComponents.BUNDLE_CONTENTS);
        }
        try {
            return this.withoutTooltipTitleLine(
                    safe.getTooltipLines(context, this.minecraft.player, TooltipFlag.NORMAL),
                    safe.getHoverName()
            );
        } catch (RuntimeException exception) {
            List<Component> fallback = new ArrayList<>();
            fallback.add(safe.getHoverName());
            return this.withoutTooltipTitleLine(fallback, safe.getHoverName());
        }
    }

    private List<Component> withoutTooltipTitleLine(List<Component> lines, Component title) {
        if (lines == null || lines.isEmpty()) {
            return List.of();
        }
        String titleText = title == null ? "" : title.getString();
        if (titleText.isBlank()) {
            return List.copyOf(lines);
        }

        Component first = lines.getFirst();
        if (!first.getString().equals(titleText)) {
            return List.copyOf(lines);
        }
        if (lines.size() == 1) {
            return List.of();
        }
        return List.copyOf(lines.subList(1, lines.size()));
    }

    private ChatFormatting applyModeColor() {
        if (this.isCreativeMode()) {
            return ChatFormatting.GREEN;
        }
        if (this.isSingleplayerMode()) {
            return ChatFormatting.GOLD;
        }
        return ChatFormatting.RED;
    }

    private boolean isCreativeMode() {
        return this.minecraft.player != null && this.minecraft.player.hasInfiniteMaterials();
    }

    private boolean isSingleplayerMode() {
        return this.minecraft.hasSingleplayerServer();
    }

    public void restorePanelScroll(double scrollAmount) {
        ScrollStateUtil.restore(this.panelScroll, scrollAmount);
        this.minecraft.execute(() -> ScrollStateUtil.restore(this.panelScroll, scrollAmount));
    }

    public double panelScrollOffset() {
        return ScrollStateUtil.offset(this.panelScroll);
    }

    private int previewTextContentWidth() {
        int hinted = this.previewTextWidthHint;
        int measured = 0;
        if (this.tooltipScroll != null && this.tooltipScroll.width() > 0) {
            measured = Math.max(measured, this.tooltipScroll.width() - UiFactory.scrollContentInset(PREVIEW_SCROLLBAR_BASE_THICKNESS));
        }
        if (this.messageScroll != null && this.messageScroll.width() > 0) {
            measured = Math.max(measured, this.messageScroll.width() - UiFactory.scrollContentInset(PREVIEW_SCROLLBAR_BASE_THICKNESS));
        }
        if (measured > 0) {
            return measured;
        }
        return hinted;
    }

    public int editorContentWidthHint() {
        if (this.panelScroll != null && this.panelScroll.width() > 0) {
            int contentGutterReserve = UiFactory.scrollContentInset(PANEL_SCROLLBAR_BASE_THICKNESS);
            int chromeReserve = UiFactory.scaledPixels(EDITOR_CONTENT_HINT_CHROME_BASE);
            int measuredHint = this.panelScroll.width() - contentGutterReserve - chromeReserve;
            return Math.min(this.panelScroll.width(), measuredHint);
        }

        int shellWidth = this.estimatedShellWidth();
        int bodyGap = UiFactory.scaledPixels(BODY_GAP_BASE);
        int railToggleWidth = UiFactory.scaledPixels(RAIL_TOGGLE_BASE);
        int toggleWidth = (this.categoriesRailCollapsed ? railToggleWidth : 0)
                + (this.previewRailCollapsed ? railToggleWidth : 0);
        int available = Math.max(1, shellWidth - (bodyGap * 2) - toggleWidth);
        int estimatedTabs = 0;
        if (!this.categoriesRailCollapsed) {
            int preferredTabs = Math.max(ESTIMATED_TABS_MIN, Math.min(ESTIMATED_TABS_MAX, (int) Math.round(available * ESTIMATED_TABS_RATIO)));
            estimatedTabs = Math.min(available, preferredTabs);
        }
        int estimatedPreview = 0;
        if (!this.previewRailCollapsed) {
            int previewBudget = available - estimatedTabs;
            int preferredPreview = Math.max(ESTIMATED_PREVIEW_MIN, Math.min(ESTIMATED_PREVIEW_MAX, (int) Math.round(available * ESTIMATED_PREVIEW_RATIO)));
            estimatedPreview = Math.min(previewBudget, preferredPreview);
        }
        int unmeasuredReserve = Math.max(
                UiFactory.scaledPixels(UNMEASURED_RESERVE_BASE),
                UiFactory.scrollContentInset(PANEL_SCROLLBAR_BASE_THICKNESS) + UiFactory.scaledPixels(UNMEASURED_RESERVE_EXTRA)
        );
        int fallbackHint = available - estimatedTabs - estimatedPreview - unmeasuredReserve;
        int viewportFloor = (int) Math.round(this.screenWidth() * EDITOR_CONTENT_VIEWPORT_FLOOR_RATIO);
        int safeFallback = Math.max(1, fallbackHint);
        int preferredHint = Math.max(EDITOR_CONTENT_WIDTH_FLOOR, viewportFloor);
        return Math.min(preferredHint, safeFallback);
    }

    public int editorContentHeightHint() {
        if (this.panelScroll != null && this.panelScroll.height() > 0) {
            return this.panelScroll.height();
        }

        int viewportFloor = (int) Math.round(this.screenHeight() * EDITOR_CONTENT_HEIGHT_VIEWPORT_RATIO);
        return Math.max(EDITOR_CONTENT_HEIGHT_FLOOR, viewportFloor);
    }

    private int scaledTextWidth(int availableWidth, float textScale) {
        int safeWidth = Math.max(1, availableWidth - UiFactory.scaledPixels(PREVIEW_TEXT_RIGHT_SAFETY_BASE));
        if (textScale <= 0f || Math.abs(textScale - 1f) < 0.001f) {
            return safeWidth;
        }
        return Math.max(1, (int) Math.ceil(safeWidth / textScale));
    }

    void requestResponsiveRelayout() {
        this.pendingInitialResponsiveRefresh = true;
        this.initialRelayoutDelayTicks = RESPONSIVE_RELAYOUT_DELAY_TICKS;
        this.initialRelayoutPassBudget = RESPONSIVE_RELAYOUT_PASS_BUDGET;
        this.initialRelayoutWaitTicks = 0;
        this.initialRelayoutRanAtLeastOnce = false;
    }

    private void runPendingInitialResponsiveRefresh() {
        if (!this.pendingInitialResponsiveRefresh) {
            return;
        }
        if (this.initialRelayoutDelayTicks > 0) {
            this.initialRelayoutDelayTicks--;
            return;
        }
        boolean widthsReady = this.categoryController.hasMeasuredResponsiveWidths();
        if (!widthsReady
                && this.initialRelayoutRanAtLeastOnce
                && this.initialRelayoutWaitTicks < RESPONSIVE_RELAYOUT_MAX_WAIT_TICKS) {
            this.initialRelayoutWaitTicks++;
            return;
        }
        if (this.initialRelayoutPassBudget <= 0) {
            this.pendingInitialResponsiveRefresh = false;
            return;
        }
        int beforeSignature = this.responsiveWidthSignature();
        boolean firstPass = !this.initialRelayoutRanAtLeastOnce;
        this.categoryController.refreshCurrentPanel(true);
        this.refreshPreview();
        this.initialRelayoutRanAtLeastOnce = true;
        int afterSignature = this.responsiveWidthSignature();
        if (!firstPass && afterSignature == beforeSignature) {
            this.pendingInitialResponsiveRefresh = false;
            return;
        }
        this.initialRelayoutPassBudget--;
        if (this.initialRelayoutPassBudget <= 0) {
            this.pendingInitialResponsiveRefresh = false;
        }
    }

    private int responsiveWidthSignature() {
        int tabsWidth = this.categoryController.measuredTabsWidth();
        int panelWidth = this.categoryController.measuredPanelWidth();
        int panelScrollWidth = this.panelScroll == null ? 0 : this.panelScroll.width();
        int tooltipScrollWidth = this.tooltipScroll == null ? 0 : this.tooltipScroll.width();
        int validationScrollWidth = this.messageScroll == null ? 0 : this.messageScroll.width();
        int signature = RESPONSIVE_SIGNATURE_SEED;
        signature = (signature * RESPONSIVE_SIGNATURE_MULTIPLIER) + tabsWidth;
        signature = (signature * RESPONSIVE_SIGNATURE_MULTIPLIER) + panelWidth;
        signature = (signature * RESPONSIVE_SIGNATURE_MULTIPLIER) + panelScrollWidth;
        signature = (signature * RESPONSIVE_SIGNATURE_MULTIPLIER) + tooltipScrollWidth;
        signature = (signature * RESPONSIVE_SIGNATURE_MULTIPLIER) + validationScrollWidth;
        return signature;
    }

    int estimatedShellWidth() {
        return Math.min(SHELL_MAX_WIDTH, this.width - SHELL_SIDE_PADDING_TOTAL);
    }

}
