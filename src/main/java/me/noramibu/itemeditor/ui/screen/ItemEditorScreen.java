package me.noramibu.itemeditor.ui.screen;

import com.mojang.blaze3d.platform.InputConstants;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.DropdownComponent;
import io.wispforest.owo.ui.component.ItemComponent;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.container.StackLayout;
import io.wispforest.owo.ui.container.UIContainers;
import io.wispforest.owo.ui.core.CursorStyle;
import io.wispforest.owo.ui.core.OwoUIAdapter;
import io.wispforest.owo.ui.core.Positioning;
import io.wispforest.owo.ui.core.Surface;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import me.noramibu.itemeditor.editor.EditorCategory;
import me.noramibu.itemeditor.editor.EditorModule;
import me.noramibu.itemeditor.editor.EditorModuleRegistry;
import me.noramibu.itemeditor.editor.ItemEditorChangeSet;
import me.noramibu.itemeditor.editor.ItemEditorSession;
import me.noramibu.itemeditor.editor.ItemEditorSessionOrigin;
import me.noramibu.itemeditor.editor.ValidationMessage;
import me.noramibu.itemeditor.service.ItemApplyService;
import me.noramibu.itemeditor.ui.component.EditorSearchDialog;
import me.noramibu.itemeditor.ui.component.RawTextAreaComponent;
import me.noramibu.itemeditor.ui.component.UiFactory;
import me.noramibu.itemeditor.ui.component.UnifiedColorPickerDialog;
import me.noramibu.itemeditor.ui.util.MenuBackgroundSurface;
import me.noramibu.itemeditor.ui.util.ScrollStateUtil;
import me.noramibu.itemeditor.ui.util.UiColors;
import me.noramibu.itemeditor.util.ItemEditorCapabilities;
import me.noramibu.itemeditor.util.ItemEditorText;
import net.minecraft.ChatFormatting;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.NotNull;

public final class ItemEditorScreen extends BaseOwoScreen<StackLayout> {
    private static final float PREVIEW_UI_SCALE = 0.70F;
    private static final float PREVIEW_STATUS_TEXT_SCALE = 0.90F;
    private static final int PANEL_SCROLLBAR_BASE_THICKNESS = 8;
    private static final int PREVIEW_SCROLLBAR_BASE_THICKNESS = 7;
    private static final int PREVIEW_TEXT_RENDER_MIN_WIDTH = 40;
    private static final int PREVIEW_TEXT_RIGHT_SAFETY_BASE = 2;
    private static final int APPLY_MODE_TEXT_RENDER_MIN_WIDTH = 40;
    private static final int PREVIEW_NAME_EXTRA_RESERVE = 24;
    private static final int APPLY_MODE_TEXT_WIDTH_HINT_DEFAULT = 220;
    private static final int PREVIEW_TEXT_WIDTH_HINT_DEFAULT = 210;
    private static final int DROPDOWN_VIEWPORT_INSET = 4;
    private static final int DROPDOWN_ANCHOR_VERTICAL_GAP = 2;
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
    private static final int RESPONSIVE_RELAYOUT_PASS_BUDGET = 4;
    private static final int RESPONSIVE_RELAYOUT_MAX_WAIT_TICKS = 40;
    private static final int PANEL_SCROLL_RESTORE_RETRY_TICKS = 8;
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
    private ButtonComponent changedOnlyButton;
    private int previewTextWidthHint = PREVIEW_TEXT_WIDTH_HINT_DEFAULT;
    private boolean sessionListenerRegistered;
    private boolean previewTooltipCollapsed;
    private boolean previewValidationCollapsed;
    private boolean categoriesRailCollapsed;
    private boolean previewRailCollapsed;
    private boolean rawFocusMode;
    private RawTextAreaComponent pendingRawEditorBinding;
    private boolean changedOnly;
    private CompletableFuture<?> rawPanelPreparation;
    private boolean pendingInitialResponsiveRefresh;
    private int initialRelayoutPassBudget;
    private int initialRelayoutWaitTicks;
    private boolean initialRelayoutRanAtLeastOnce;
    private Double pendingPanelScrollOffset;
    private Double pendingTooltipScrollOffset;
    private Double pendingMessageScrollOffset;
    private Double deferredPanelScrollOffset;
    private int deferredPanelScrollRestoreTicks;

    private final Component nestedPath;

    public ItemEditorScreen(ItemEditorSession session) {
        this(session, null);
    }

    public ItemEditorScreen(ItemEditorSession session, EditorCategory onlyCategory) {
        this(session, onlyCategory, null);
    }

    private ItemEditorScreen(ItemEditorSession session, EditorCategory onlyCategory, Component contextTitle) {
        super(
                contextTitle != null
                        ? ItemEditorText.tr("screen.nested.context", contextTitle)
                        : ItemEditorText.tr(
                                session.origin() instanceof ItemEditorSessionOrigin.External external
                                                && external.returnScreen() instanceof ItemEditorScreen
                                        ? "screen.nested.title"
                                        : "screen.title"));
        this.nestedPath = contextTitle;
        this.session = session;
        this.categoriesRailCollapsed = session.state().uiCategoriesRailCollapsed;
        this.previewRailCollapsed = session.state().uiPreviewRailCollapsed;
        this.previewTooltipCollapsed = session.state().uiPreviewTooltipCollapsed;
        this.previewValidationCollapsed = session.state().uiPreviewValidationCollapsed;
        List<EditorModule> modules = EditorModuleRegistry.modules(onlyCategory).stream()
                .filter(module -> module.enabled().test(this.session))
                .toList();
        this.selectedModule = modules.getFirst();
        this.dialogController = new ItemEditorDialogController(this);
        this.categoryController = new ItemEditorCategoryController(this, modules);
    }

    public void openNestedEditor(
            ItemStack stack, EditorCategory onlyCategory, Consumer<ItemStack> onApply, Component contextTitle) {
        double scroll = this.panelScrollOffset();
        Objects.requireNonNull(contextTitle, "Nested editor field context");
        Component path = this.nestedPath == null
                ? contextTitle
                : this.nestedPath.copy().append(" > ").append(contextTitle);
        var origin = new ItemEditorSessionOrigin.External(
                this,
                edited -> {
                    onApply.accept(edited);
                    this.refreshCurrentPanel();
                    this.preservePanelScrollOnNextBuild(scroll);
                    this.restorePanelScroll(scroll);
                    return ItemApplyService.ApplyResult.success("");
                },
                -1);
        var editor = new ItemEditorScreen(
                new ItemEditorSession(this.session.minecraft(), stack.copy(), origin), onlyCategory, path);
        this.session.minecraft().setScreen(editor);
    }

    public void chooseItemSource(Runnable itemList, Runnable storage) {
        this.dialogController.chooseItemSource(itemList, storage);
    }

    public void choosePickedItem(ItemStack stack, Consumer<ItemStack> onUse) {
        this.dialogController.choosePickedItem(
                stack,
                onUse,
                onUse instanceof ContextualSelection selection ? selection.title() : stack.getHoverName());
    }

    public Consumer<ItemStack> contextualSelection(Consumer<ItemStack> onUse, Component title) {
        return new ContextualSelection(onUse, Objects.requireNonNull(title, "Picker field context"));
    }

    private record ContextualSelection(Consumer<ItemStack> action, Component title) implements Consumer<ItemStack> {
        @Override
        public void accept(ItemStack stack) {
            action.accept(stack);
        }
    }

    boolean isNestedEditor() {
        return this.session.origin() instanceof ItemEditorSessionOrigin.External external
                && external.returnScreen() instanceof ItemEditorScreen;
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
        root.surface(MenuBackgroundSurface.standard());

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
            int maxWidth = Math.max(
                    PREVIEW_TEXT_RENDER_MIN_WIDTH,
                    this.previewTextContentWidth() - UiFactory.scaledPixels(PREVIEW_NAME_EXTRA_RESERVE));
            this.previewNameLabel.text(UiFactory.fitToWidth(fullName, maxWidth));
            this.previewNameLabel.tooltip(List.of(fullName));
        }

        if (this.applyModeLabel != null) {
            Component fullApplyModeText =
                    Component.literal(this.applyModeText()).withStyle(this.applyModeColor());
            this.applyModeLabel.text(UiFactory.fitToWidth(
                    fullApplyModeText, Math.max(APPLY_MODE_TEXT_RENDER_MIN_WIDTH, this.applyModeTextWidthHint)));
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
                this.messages.child(UiFactory.muted(
                        ItemEditorText.tr("screen.validation.none"),
                        this.scaledTextWidth(contentWidth, PREVIEW_STATUS_TEXT_SCALE),
                        PREVIEW_STATUS_TEXT_SCALE));
            } else {
                for (ValidationMessage message : this.session.messages()) {
                    int color =
                            switch (message.severity()) {
                                case ERROR -> UiColors.DANGER;
                                case WARNING -> UiColors.WARNING;
                                case INFO -> UiColors.INFO;
                            };
                    var label = UiFactory.message(Component.literal(message.message()), color, PREVIEW_UI_SCALE)
                            .maxWidth(scaledContentWidth);
                    label.cursorStyle(CursorStyle.HAND);
                    label.tooltip(List.of(ItemEditorText.tr("screen.validation.locate")));
                    label.mouseDown().subscribe((click, doubled) -> {
                        if (click.button() != InputConstants.MOUSE_BUTTON_LEFT || !this.isDialogClosed()) return false;
                        this.openValidationField(message.message());
                        return true;
                    });
                    this.messages.child(label);
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
        ItemEditorChangeSet changes = this.session.changes();
        this.categoryController.refreshTabs(changes);
        this.categoryController.refreshChangedMarkers(changes);
    }

    private void openValidationField(String message) {
        var targets = this.categoryController.searchTargets();
        String normalized = message.toLowerCase(Locale.ROOT);
        var matches = targets.stream()
                .filter(target -> !target.label().isBlank()
                        && normalized.contains(target.label().toLowerCase(Locale.ROOT)))
                .toList();
        int longest = matches.stream()
                .mapToInt(target -> target.label().length())
                .max()
                .orElse(0);
        matches = matches.stream()
                .filter(target -> target.label().length() == longest)
                .toList();
        if (matches.size() == 1) matches.getFirst().open().run();
        else this.dialogController.openEditorSearch(matches.isEmpty() ? targets : matches);
    }

    public void refreshCurrentPanel() {
        this.categoryController.refreshCurrentPanel(true);
    }

    boolean changedOnly() {
        return this.changedOnly;
    }

    void toggleChangedOnly() {
        this.changedOnly = !this.changedOnly;
        this.updateChangedOnlyButton();
        this.categoryController.refreshCurrentPanel(true);
        this.requestResponsiveRelayout();
    }

    void showAllFields() {
        this.changedOnly = false;
        this.updateChangedOnlyButton();
    }

    Component changedOnlyButtonText() {
        Component text = ItemEditorText.tr(this.changedOnly ? "changes.show_all" : "changes.show_only");
        return this.changedOnly ? text.copy().withStyle(ChatFormatting.YELLOW) : text;
    }

    void bindChangedOnlyButton(ButtonComponent button) {
        this.changedOnlyButton = button;
    }

    public void bindApplyButton(ButtonComponent button) {
        this.applyButton = button;
        button.active(!this.session.hasErrors());
    }

    private void updateChangedOnlyButton() {
        if (this.changedOnlyButton != null) {
            this.changedOnlyButton.setMessage(this.changedOnlyButtonText());
        }
    }

    public void refreshRawPanelWhenReady(CompletableFuture<?> preparation) {
        if (preparation == null || this.rawPanelPreparation == preparation) {
            return;
        }
        this.rawPanelPreparation = preparation;
        preparation.whenComplete((ignored, error) -> this.minecraft.execute(() -> {
            if (this.rawPanelPreparation != preparation) {
                return;
            }
            this.rawPanelPreparation = null;
            if (this.minecraft.screen != this || this.selectedModule.category() != EditorCategory.RAW_EDITOR) {
                return;
            }
            if (error == null || isSupersededPreparation(error)) {
                this.refreshCurrentPanel();
            }
        }));
    }

    private static boolean isSupersededPreparation(Throwable error) {
        Throwable cause = error instanceof CompletionException && error.getCause() != null ? error.getCause() : error;
        return cause instanceof CancellationException;
    }

    public <T> void openDropdown(
            ButtonComponent anchor, List<T> values, Function<T, String> labelMapper, Consumer<T> selectionConsumer) {
        this.openDropdown(anchor, values, labelMapper, selectionConsumer, null, null);
    }

    public <T> void openClearableDropdown(
            ButtonComponent anchor,
            Component clearLabel,
            Runnable clearAction,
            List<T> values,
            Function<T, String> labelMapper,
            Consumer<T> selectionConsumer) {
        this.openDropdown(anchor, values, labelMapper, selectionConsumer, clearLabel, clearAction);
    }

    private <T> void openDropdown(
            ButtonComponent anchor,
            List<T> values,
            Function<T, String> labelMapper,
            Consumer<T> selectionConsumer,
            Component clearLabel,
            Runnable clearAction) {
        if (this.rootLayout == null || (values.isEmpty() && clearAction == null)) {
            return;
        }

        int menuX = anchor.x();
        int menuY = anchor.y() + anchor.height() + DROPDOWN_ANCHOR_VERTICAL_GAP;

        DropdownComponent dropdown =
                DropdownComponent.openContextMenu(this, this.rootLayout, StackLayout::child, menuX, menuY, menu -> {
                    if (clearAction != null) {
                        menu.button(
                                clearLabel == null ? ItemEditorText.tr("common.none") : clearLabel,
                                dropdownComponent -> {
                                    clearAction.run();
                                    dropdownComponent.remove();
                                    this.refreshCurrentPanel();
                                    this.refreshPreview();
                                });
                    }
                    values.forEach(value -> menu.button(
                            Component.literal(this.dropdownLabelText(value, labelMapper)), dropdownComponent -> {
                                selectionConsumer.accept(value);
                                dropdownComponent.remove();
                                this.refreshCurrentPanel();
                                this.refreshPreview();
                            }));
                });
        dropdown.closeWhenNotHovered(false);
        if (dropdown.width() > this.width - DROPDOWN_VIEWPORT_INSET * 2
                || dropdown.height() > this.height - DROPDOWN_VIEWPORT_INSET * 2) {
            dropdown.remove();
            List<String> options = new ArrayList<>();
            if (clearAction != null) options.add("-1");
            for (int index = 0; index < values.size(); index++) options.add(Integer.toString(index));
            this.openSearchablePickerDialog(
                    anchor.getMessage().getString(),
                    options,
                    index -> index.equals("-1")
                            ? (clearLabel == null ? ItemEditorText.tr("common.none") : clearLabel).getString()
                            : this.dropdownLabelText(values.get(Integer.parseInt(index)), labelMapper),
                    index -> {
                        if (index.equals("-1")) clearAction.run();
                        else selectionConsumer.accept(values.get(Integer.parseInt(index)));
                        this.refreshCurrentPanel();
                        this.refreshPreview();
                    });
            return;
        }
        var position = DropdownPlacement.at(
                anchor.x(), anchor.y(), anchor.height(), dropdown.width(), dropdown.height(), this.width, this.height);
        dropdown.positioning(
                Positioning.absolute(position.x() - this.rootLayout.x(), position.y() - this.rootLayout.y()));
        dropdown.surface(Surface.flat(0xFF101418).and(Surface.outline(0xFF78909C)));
    }

    private <T> String dropdownLabelText(T value, Function<T, String> labelMapper) {
        String mapped = labelMapper.apply(value);
        return mapped == null ? EMPTY_TEXT : mapped;
    }

    public void openSearchablePickerDialog(
            String title,
            List<String> values,
            Function<String, String> labelMapper,
            Consumer<String> selectionConsumer) {
        this.openSearchablePickerDialog(title, "", values, labelMapper, selectionConsumer);
    }

    public void openSearchablePickerDialog(
            String title,
            String body,
            List<String> values,
            Function<String, String> labelMapper,
            Consumer<String> selectionConsumer) {
        this.dialogController.openSearchablePickerDialog(title, body, values, labelMapper, selectionConsumer);
    }

    public void requestReset() {
        this.dialogController.requestReset();
    }

    public void openRawStringEditor(RawTextAreaComponent editor) {
        if (this.isDialogClosed()) this.dialogController.openRawStringEditor(editor);
    }

    public void openRawTextSearch(RawTextAreaComponent editor, int tab) {
        if (this.isDialogClosed()) this.dialogController.openRawTextSearch(editor, tab);
    }

    void suspendRawSearch() {
        this.pendingRawEditorBinding = null;
        this.dialogController.suspendRawSearch();
    }

    public void bindRawSearchEditor(RawTextAreaComponent editor) {
        this.pendingRawEditorBinding = editor;
    }

    private void bindMountedRawEditor() {
        RawTextAreaComponent editor = this.pendingRawEditorBinding;
        if (editor == null || this.rootLayout == null || editor.focusHandler() == null) return;
        this.pendingRawEditorBinding = null;
        if (editor.root() == this.rootLayout) this.dialogController.bindRawSearchEditor(editor);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        this.dialogController.rawSearchMouseClicked(click.x(), click.y());
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        return this.dialogController.rawSearchMouseScrolled(mouseX, mouseY, verticalAmount)
                || super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    public void confirmRestore(Component label, String current, String original, Runnable restore) {
        if (!this.isDialogClosed() || Objects.equals(current, original)) return;
        this.dialogController.confirmRestore(label, current, original, restore);
    }

    private final Map<List<?>, Object> selectedListEntries = new IdentityHashMap<>();

    public boolean isSelectedEntry(List<?> entries, Object entry) {
        Object selected = this.selectedListEntries.get(entries);
        if (!entries.contains(selected)) {
            selected = entries.isEmpty() ? null : entries.getFirst();
            this.selectedListEntries.put(entries, selected);
        }
        return selected == entry;
    }

    public void selectEntry(List<?> entries, Object entry) {
        this.selectedListEntries.put(entries, entry);
    }

    public void openPlayerUuidPicker(String title, Map<String, String> players, Consumer<String> onSelect) {
        this.dialogController.openPlayerUuidPicker(title, players, uuid -> {
            onSelect.accept(uuid);
            this.refreshCurrentPanel();
            this.refreshPreview();
        });
    }

    public Component applyActionLabel() {
        if (this.isNestedEditor()) return ItemEditorText.tr("screen.nested.apply");
        if (this.session.origin() instanceof ItemEditorSessionOrigin.External external) {
            return external.verificationSlot() >= 0
                    ? ItemEditorText.tr("editor.apply.inventory_slot", external.verificationSlot() + 1)
                    : ItemEditorText.tr("editor.apply.return_source");
        }
        return ItemEditorText.tr(
                this.session.origin() instanceof ItemEditorSessionOrigin.Imported
                                && this.minecraft.player != null
                                && !this.minecraft.player.getMainHandItem().isEmpty()
                        ? "editor.apply.replace_held"
                        : "common.save");
    }

    public void requestApply() {
        this.dialogController.requestApply();
    }

    public void requestSaveStorage() {
        this.dialogController.requestSaveStorage();
    }

    public void requestPlaceAndSaveStorage() {
        this.dialogController.requestPlaceAndSaveStorage();
    }

    public void requestClose() {
        this.dialogController.requestClose();
    }

    public void openUnifiedColorPickerDialog(
            String title,
            UnifiedColorPickerDialog.Options options,
            Consumer<UnifiedColorPickerDialog.ColorPickerResult> onApply) {
        this.dialogController.openUnifiedColorPickerDialog(title, options, onApply);
    }

    public void openPairedColorPickerDialog(
            String title,
            UnifiedColorPickerDialog.Options options,
            UnifiedColorPickerDialog.PaintLayer text,
            UnifiedColorPickerDialog.PaintLayer shadow,
            boolean shadowEnabled,
            Consumer<UnifiedColorPickerDialog.PairedColorResult> onApply) {
        this.dialogController.openPairedColorPickerDialog(title, options, text, shadow, shadowEnabled, onApply);
    }

    public void openRichTextHeadDialog(String title, Consumer<String> onApply) {
        this.dialogController.openRichTextHeadDialog(title, onApply);
    }

    public void openRichTextSpriteDialog(String title, Consumer<String> onApply) {
        this.dialogController.openRichTextSpriteDialog(title, onApply);
    }

    public void openRichTextTranslationDialog(String title, String initialText, Consumer<String> onApply) {
        this.dialogController.openRichTextTranslationDialog(title, initialText, onApply);
    }

    public void openRichTextEventDialog(
            String title,
            boolean includeHoverModes,
            boolean includeSuggestCommand,
            String initialText,
            Consumer<String> onApply) {
        this.dialogController.openRichTextEventDialog(
                title, includeHoverModes, includeSuggestCommand, initialText, onApply);
    }

    public void openLoreImageArtDialog(BiConsumer<List<Component>, Boolean> onApply) {
        this.dialogController.openLoreImageArtDialog(onApply);
    }

    public void openTextDisplayImageArtDialog(int backgroundColor, BiConsumer<List<Component>, Boolean> onApply) {
        this.dialogController.openTextDisplayImageArtDialog(backgroundColor, onApply);
    }

    public void openRawItemDataDialog(String title, boolean previewData) {
        this.dialogController.openRawItemDataDialog(title, previewData);
    }

    @Override
    public void resize(int width, int height) {
        this.pendingRawEditorBinding = null;
        var stringEditor = this.dialogController.rawStringEditor();
        this.dialogController.suspendRawSearch();
        if (this.pendingPanelScrollOffset == null) {
            this.pendingPanelScrollOffset = ScrollStateUtil.offset(this.panelScroll);
        }
        if (this.pendingTooltipScrollOffset == null) {
            this.pendingTooltipScrollOffset = ScrollStateUtil.offset(this.tooltipScroll);
        }
        if (this.pendingMessageScrollOffset == null) {
            this.pendingMessageScrollOffset = ScrollStateUtil.offset(this.messageScroll);
        }
        this.clearDialog();
        if (this.uiAdapter != null) {
            this.uiAdapter.dispose();
            this.uiAdapter = null;
        }
        super.resize(width, height);
        this.dialogController.restoreRawStringEditor(stringEditor);
    }

    @Override
    public void removed() {
        this.pendingRawEditorBinding = null;
        this.dialogController.closeRawSearch();
        this.preservePanelScrollOnNextBuild(this.panelScrollOffset());
        this.pendingTooltipScrollOffset = ScrollStateUtil.offset(this.tooltipScroll);
        this.pendingMessageScrollOffset = ScrollStateUtil.offset(this.messageScroll);
        super.removed();
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

        if (input.key() == InputConstants.KEY_LSHIFT && input.hasControlDownWithQuirk()) {
            return true;
        }

        if (this.dialogController.handleRawSearchShortcut(input)) return true;
        if (this.dialogController.handleDialogShortcut(input)) {
            return true;
        }

        if (input.hasControlDownWithQuirk()) {
            if (this.isDialogClosed()
                    && !input.hasShiftDown()
                    && input.key() >= InputConstants.KEY_1
                    && input.key() <= InputConstants.KEY_9) {
                this.categoryController.selectCategory(input.key() - InputConstants.KEY_1);
                return true;
            }
            if (input.key() == InputConstants.KEY_F && this.isDialogClosed()) {
                if (super.keyPressed(input)) return true;
                this.openEditorSearch();
                return true;
            }
            if (input.key() == InputConstants.KEY_S) {
                this.requestApply();
                return true;
            }
            if (input.key() == InputConstants.KEY_R) {
                this.requestReset();
                return true;
            }
            if (input.key() == InputConstants.KEY_TAB) {
                this.categoryController.selectAdjacentCategory(input.hasShiftDown() ? -1 : 1);
                return true;
            }
        }
        return super.keyPressed(input);
    }

    @Override
    public void tick() {
        super.tick();
        this.bindMountedRawEditor();
        this.dialogController.tick();
        this.runPendingInitialResponsiveRefresh();
        this.runDeferredPanelScrollRestore();
        this.categoryController.tickSearch();
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

    void openEditorSearch() {
        if (this.isDialogClosed()) this.dialogController.openEditorSearch(this.categoryController.searchTargets());
    }

    public void revealSearchTarget(EditorCategory category, String label) {
        revealSearchTarget(category, new EditorSearchDialog.Location("", label));
    }

    public void revealSearchTarget(EditorCategory category, EditorSearchDialog.Location location) {
        this.categoryController.revealSearchTarget(category, location);
    }

    boolean searchLayoutReady() {
        return !this.pendingInitialResponsiveRefresh && this.deferredPanelScrollRestoreTicks <= 0;
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
        return this.rawFocusMode || this.categoriesRailCollapsed;
    }

    void setCategoriesRailCollapsed(boolean value) {
        this.categoriesRailCollapsed = value;
        this.session.state().uiCategoriesRailCollapsed = value;
    }

    boolean previewRailCollapsed() {
        return this.rawFocusMode || this.previewRailCollapsed;
    }

    void setPreviewRailCollapsed(boolean value) {
        this.previewRailCollapsed = value;
        this.session.state().uiPreviewRailCollapsed = value;
    }

    void rebuildLayout() {
        this.preservePanelScrollOnNextBuild(this.panelScrollOffset());
        this.resize(this.width, this.height);
    }

    public boolean rawFocusMode() {
        return this.rawFocusMode;
    }

    public void toggleRawFocusMode() {
        if (this.selectedModule.category() != EditorCategory.RAW_EDITOR) return;
        this.rawFocusMode = !this.rawFocusMode;
        this.rebuildLayout();
    }

    boolean leaveRawFocusMode() {
        if (!this.rawFocusMode) return false;
        this.rawFocusMode = false;
        this.rebuildLayout();
        return true;
    }

    Component categoryTitle(EditorModule module) {
        if (module.category() == EditorCategory.SPECIAL_DATA) {
            return ItemEditorCapabilities.specialDataTitle(this.session.originalStack());
        }
        return module.category().title();
    }

    String applyModeText() {
        if (this.isNestedEditor()) return ItemEditorText.str("screen.nested.mode");
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
            case GREEN -> UiColors.SUCCESS;
            case GOLD -> UiColors.WARNING;
            default -> UiColors.DANGER;
        };
    }

    private List<Component> safeTooltipLines(ItemStack stack, Item.TooltipContext context) {
        ItemStack safe = stack.copy();
        if (safe.has(DataComponents.BUNDLE_CONTENTS)) {
            safe.remove(DataComponents.BUNDLE_CONTENTS);
        }
        try {
            return this.withoutTooltipTitleLine(
                    safe.getTooltipLines(context, this.minecraft.player, TooltipFlag.NORMAL), safe.getHoverName());
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
        if (this.isNestedEditor()) return ChatFormatting.GOLD;
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

    public void preservePanelScrollOnNextBuild(double scrollAmount) {
        this.pendingPanelScrollOffset = scrollAmount;
        this.deferredPanelScrollOffset = scrollAmount;
        this.deferredPanelScrollRestoreTicks = PANEL_SCROLL_RESTORE_RETRY_TICKS;
    }

    public double panelScrollOffset() {
        return ScrollStateUtil.offset(this.panelScroll);
    }

    private int previewTextContentWidth() {
        int hinted = this.previewTextWidthHint;
        int measured = 0;
        if (this.tooltipScroll != null && this.tooltipScroll.width() > 0) {
            measured = Math.max(
                    measured,
                    this.tooltipScroll.width() - UiFactory.scrollContentInset(PREVIEW_SCROLLBAR_BASE_THICKNESS));
        }
        if (this.messageScroll != null && this.messageScroll.width() > 0) {
            measured = Math.max(
                    measured,
                    this.messageScroll.width() - UiFactory.scrollContentInset(PREVIEW_SCROLLBAR_BASE_THICKNESS));
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
        if (this.rawFocusMode) return Math.max(1, shellWidth - UiFactory.scaledPixels(48));
        int bodyGap = UiFactory.scaledPixels(BODY_GAP_BASE);
        int railToggleWidth = UiFactory.scaledPixels(RAIL_TOGGLE_BASE);
        int toggleWidth = (this.categoriesRailCollapsed ? railToggleWidth : 0)
                + (this.previewRailCollapsed ? railToggleWidth : 0);
        int available = Math.max(1, shellWidth - (bodyGap * 2) - toggleWidth);
        int estimatedTabs = 0;
        if (!this.categoriesRailCollapsed) {
            int preferredTabs = Math.clamp(
                    (int) Math.round(available * ESTIMATED_TABS_RATIO), ESTIMATED_TABS_MIN, ESTIMATED_TABS_MAX);
            estimatedTabs = Math.min(available, preferredTabs);
        }
        int estimatedPreview = 0;
        if (!this.previewRailCollapsed) {
            int previewBudget = available - estimatedTabs;
            int preferredPreview = Math.clamp(
                    (int) Math.round(available * ESTIMATED_PREVIEW_RATIO),
                    ESTIMATED_PREVIEW_MIN,
                    ESTIMATED_PREVIEW_MAX);
            estimatedPreview = Math.min(previewBudget, preferredPreview);
        }
        int unmeasuredReserve = Math.max(
                UiFactory.scaledPixels(UNMEASURED_RESERVE_BASE),
                UiFactory.scrollContentInset(PANEL_SCROLLBAR_BASE_THICKNESS)
                        + UiFactory.scaledPixels(UNMEASURED_RESERVE_EXTRA));
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
        this.initialRelayoutPassBudget = RESPONSIVE_RELAYOUT_PASS_BUDGET;
        this.initialRelayoutWaitTicks = 0;
        this.initialRelayoutRanAtLeastOnce = false;
    }

    private void runPendingInitialResponsiveRefresh() {
        if (!this.pendingInitialResponsiveRefresh) {
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

    private void runDeferredPanelScrollRestore() {
        if (this.deferredPanelScrollOffset == null || this.deferredPanelScrollRestoreTicks <= 0) {
            return;
        }
        ScrollStateUtil.restore(this.panelScroll, this.deferredPanelScrollOffset);
        this.deferredPanelScrollRestoreTicks--;
        if (this.deferredPanelScrollRestoreTicks <= 0) {
            this.deferredPanelScrollOffset = null;
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
        return ItemEditorLayoutBuilder.estimatedShellWidth(this.width, this.height);
    }
}
