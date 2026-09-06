package me.noramibu.itemeditor.ui.screen;

import com.mojang.serialization.DataResult;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;
import me.noramibu.itemeditor.compat.rei.ItemEditorReiClientPlugin;
import me.noramibu.itemeditor.editor.ItemEditorSession;
import me.noramibu.itemeditor.editor.ItemEditorSessionOrigin;
import me.noramibu.itemeditor.service.ClientInventorySyncService;
import me.noramibu.itemeditor.storage.SavedItemStorageService;
import me.noramibu.itemeditor.storage.StorageNbtSizeUtil;
import me.noramibu.itemeditor.storage.StorageServices;
import me.noramibu.itemeditor.storage.StorageSizeText;
import me.noramibu.itemeditor.storage.StorageSortMode;
import me.noramibu.itemeditor.storage.model.PreferencesFileModel;
import me.noramibu.itemeditor.storage.model.SavedIndexItemEntry;
import me.noramibu.itemeditor.storage.search.StorageSearchAutocompleteUtil;
import me.noramibu.itemeditor.storage.search.StorageSearchParser;
import me.noramibu.itemeditor.storage.search.StorageSearchQuery;
import me.noramibu.itemeditor.ui.util.UiColors;
import me.noramibu.itemeditor.util.ItemEditorText;
import me.noramibu.itemeditor.util.TextComponentUtil;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

public final class StorageScreen extends ContainerScreen {

    private static final int SLOT_COUNT = 54;
    private static final int PANEL_MIN_WIDTH = 148;
    private static final int PANEL_HARD_MIN_WIDTH = 116;
    private static final int BUTTON_HEIGHT = 20;
    private static final int GAP = 2;
    private static final int PANEL_MARGIN = 8;
    private static final int PANEL_TEXT_LINE_HEIGHT = 10;
    private static final long SEARCH_DEBOUNCE_MS = 220L;
    private static final long PAGE_STATS_REFRESH_MS = 250L;
    private static int lastPage = 1;
    private static String lastQuery = "";
    private static StorageSortMode lastSortMode = StorageSortMode.REGULAR;
    private static boolean lastReverseSort;
    private static final String[] SEARCH_AUTOCOMPLETE_TOKENS = {
        "item:\"minecraft:*_shulker_*\"",
        "item:\"minecraft:stone\"",
        "name:\"\"",
        "lore:\"\"",
        "amount:64",
        "a:>=16",
        "amount:1-32",
        "size:>=128",
        "size:256-2048",
        "before:30m",
        "before:24h",
        "before:7d",
        "after:30m",
        "after:24h",
        "after:7d"
    };
    private static final int COLOR_TEXT = 0xD5DEE8;
    private static final int COLOR_HINT = 0x8EA0B0;
    private static final DateTimeFormatter SAVED_AT_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());
    private static final Component HINT_OPEN_EDITOR = ItemEditorText.tr("storage.hint.open_editor");
    private static final Component HINT_LOCK = ItemEditorText.tr("storage.hint.lock");
    private static final Component HINT_PICK_STACK = ItemEditorText.tr("storage.pick_stack_hint");
    private static final Component HINT_VANILLA = ItemEditorText.tr("storage.hint.vanilla");
    private static final Component HINT_SYNC = ItemEditorText.tr("storage.hint.sync");
    private static final Component HINT_SEARCH = ItemEditorText.tr("storage.hint.search");

    private final SimpleContainer storageContainer;
    private final SavedItemStorageService storage = StorageServices.savedItems();
    private final Map<Integer, SavedIndexItemEntry> slotEntries = new HashMap<>();
    private final Map<Integer, ItemStack> baselineVisibleStacks = new HashMap<>();
    private final Map<Integer, ItemStack> playerInventoryBeforeInteraction = new HashMap<>();
    private StorageScreenMode mode;
    private StorageScreenMode preferredMode;
    private final Screen returnScreen;
    private final Consumer<ItemStack> pickedStackConsumer;

    private int currentPage;
    private String currentQuery;
    private StorageSortMode sortMode;
    private boolean reverseSort;

    private SavedItemStorageService.PageResult currentResult;

    private final List<AbstractWidget> panelWidgets = new ArrayList<>();
    private EditBox searchInput;
    private EditBox jumpInput;
    private Button sortButton;
    private Button reverseSortButton;
    private Button modeButton;
    private int panelX;
    private int panelWidth = PANEL_MIN_WIDTH;
    private int panelTextStartY;
    private long liveSearchDueAt = -1L;
    private boolean pageStatsRefreshPending;
    private long pageStatsRefreshDueAt;

    private Component pageLabel = Component.empty();
    private Component storedPagesLabel = Component.empty();
    private Component totalLabel = Component.empty();
    private Component searchLabel = Component.empty();
    private Component searchMetaLabel = Component.empty();
    private Component searchAutoLabel = Component.empty();
    private Component feedbackLabel = Component.literal(" ");
    private Component storageTitleLabel = ItemEditorText.tr("storage.title");
    private int feedbackColor = UiColors.MUTED;
    private InteractionSnapshot interactionSnapshot;
    private long refreshRequestSequence;
    private long activeRefreshRequest;

    public StorageScreen() {
        this(lastPage, lastQuery, lastSortMode, rememberedMode(), null, null);
    }

    public StorageScreen(StorageScreenMode mode, Screen returnScreen, Consumer<ItemStack> pickedStackConsumer) {
        this(lastPage, lastQuery, lastSortMode, mode, returnScreen, pickedStackConsumer);
    }

    public StorageScreen(int initialPage, String initialQuery, StorageSortMode initialSortMode) {
        this(initialPage, initialQuery, initialSortMode, rememberedMode(), null, null);
    }

    public StorageScreen(
            int initialPage,
            String initialQuery,
            StorageSortMode initialSortMode,
            StorageScreenMode mode,
            Screen returnScreen,
            Consumer<ItemStack> pickedStackConsumer) {
        this(
                new SimpleContainer(SLOT_COUNT),
                requireInventory(),
                initialPage,
                initialQuery,
                initialSortMode,
                mode,
                returnScreen,
                pickedStackConsumer);
    }

    private StorageScreen(
            SimpleContainer storageContainer,
            Inventory inventory,
            int initialPage,
            String initialQuery,
            StorageSortMode initialSortMode,
            StorageScreenMode mode,
            Screen returnScreen,
            Consumer<ItemStack> pickedStackConsumer) {
        super(ChestMenu.sixRows(0, inventory, storageContainer), inventory, ItemEditorText.tr("storage.title"));
        this.storageContainer = storageContainer;
        this.mode = mode == null ? StorageScreenMode.MANAGE : mode;
        this.preferredMode = this.mode;
        this.returnScreen = returnScreen;
        this.pickedStackConsumer = pickedStackConsumer;
        this.currentPage = Math.max(1, initialPage);
        this.currentQuery = initialQuery == null ? "" : initialQuery;
        this.sortMode = initialSortMode == null ? StorageSortMode.REGULAR : initialSortMode;
        this.reverseSort = lastReverseSort;
        this.updateModeForCurrentView();
        this.rememberRuntimeView();
    }

    @Override
    protected void init() {
        super.init();
        this.resolvePanelLayout();
        this.buildPanelWidgets();
        this.storage.prewarmOnOpen(this.currentPage, this.currentQuery, this.sortMode, this.sessionRegistryAccess());
        this.refreshData();
    }

    @Override
    public void extractBackground(@NotNull GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractBackground(context, mouseX, mouseY, delta);
        ScreenRectangle bounds = this.controllerBounds();
        context.fill(bounds.left(), bounds.top(), bounds.right(), bounds.bottom(), 0x88000000);
        this.extractPanelText(context);
    }

    @Override
    protected void extractLabels(@NotNull GuiGraphicsExtractor context, int mouseX, int mouseY) {
        context.text(this.font, this.storageTitleLabel, this.titleLabelX, this.titleLabelY, -12566464, false);
        context.text(
                this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, -12566464, false);
    }

    @Override
    protected void slotClicked(@Nullable Slot slot, int slotId, int button, @NotNull ContainerInput input) {
        if (this.isSelectMode()) {
            if (button == 0 && slot != null && slot.hasItem() && this.pickClickedStack(slot, slotId)) {
                return;
            }
            return;
        }
        if (this.isLockMode() && this.isStorageSlotId(slotId)) {
            if (slot != null && slot.hasItem()) {
                if (button == 0 && input == ContainerInput.QUICK_MOVE) {
                    this.copyStorageItemToInventory(slot);
                } else if (input == ContainerInput.PICKUP && button == 0) {
                    this.copyStorageItem(slot);
                } else if (input == ContainerInput.PICKUP && button == 1) {
                    this.openStorageItem(slot.index);
                }
            }
            return;
        }
        if (this.isLockMode() && input != ContainerInput.PICKUP) {
            return;
        }
        if (this.isReadOnlyStorageSlot(slotId)) {
            this.feedback(ItemEditorText.tr("storage.edit_requires_regular"), UiColors.MUTED);
            this.refreshData();
            return;
        }
        this.beginInteractionSnapshot();
        if (this.minecraft.player == null) {
            return;
        }
        this.menu.clicked(slotId, button, input, this.minecraft.player);
    }

    @Override
    public boolean mouseClicked(@NotNull MouseButtonEvent input, boolean doubleClick) {
        if (this.isPanelMouse(input.x(), input.y())) {
            for (AbstractWidget widget : this.panelWidgets) {
                if (widget.isMouseOver(input.x(), input.y()) && widget.mouseClicked(input, doubleClick)) {
                    this.setFocused(widget);
                    if (input.button() == 0) {
                        this.setDragging(true);
                    }
                    return true;
                }
            }
            this.setFocused(null);
            return true;
        }
        return super.mouseClicked(input, doubleClick);
    }

    @Override
    public boolean mouseReleased(@NotNull MouseButtonEvent input) {
        if (input.button() == 0
                && this.isDragging()
                && this.getFocused() instanceof AbstractWidget focused
                && this.panelWidgets.contains(focused)) {
            this.setDragging(false);
            focused.mouseReleased(input);
            return true;
        }
        if (this.isPanelMouse(input.x(), input.y())) {
            return true;
        }
        boolean handled = super.mouseReleased(input);
        this.finishInteractionSync();
        return handled;
    }

    @Override
    public boolean mouseDragged(@NotNull MouseButtonEvent input, double dragX, double dragY) {
        if (this.isDragging()
                && this.getFocused() instanceof AbstractWidget focused
                && this.panelWidgets.contains(focused)) {
            return focused.mouseDragged(input, dragX, dragY);
        }
        return super.mouseDragged(input, dragX, dragY);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        long now = System.currentTimeMillis();
        if (this.liveSearchDueAt > 0L && this.searchInput != null && now >= this.liveSearchDueAt) {
            this.liveSearchDueAt = -1L;
            this.applySearchIfChanged(this.searchInput.getValue().trim());
        }
        if (this.pageStatsRefreshPending && this.currentResult != null && now >= this.pageStatsRefreshDueAt) {
            if (this.storage.hasPendingWrites()) {
                this.pageStatsRefreshDueAt = now + PAGE_STATS_REFRESH_MS;
            } else {
                this.refreshStoredPagesLabel(this.storage.pageStats());
                this.pageStatsRefreshPending = false;
            }
        }
    }

    @Override
    public boolean keyPressed(@NotNull KeyEvent input) {
        if (input.key() == GLFW.GLFW_KEY_I) {
            Slot hovered = this.hoveredSlot;
            if (hovered != null && hovered.hasItem()) {
                if (!this.isManageMode()) {
                    int slotId = this.menu.slots.indexOf(hovered);
                    if (this.isSelectMode() && this.pickClickedStack(hovered, slotId)) {
                        return true;
                    }
                    int storageSlot = this.hoveredStorageSlotIndex();
                    return storageSlot >= 0 && this.openStorageItem(storageSlot);
                }
                this.minecraft.setScreenAndShow(new ItemEditorScreen(
                        new ItemEditorSession(this.minecraft, hovered.getItem().copy())));
                return true;
            }
        }
        if (this.handleSearchShortcuts(input)) {
            return true;
        }
        if (input.key() == GLFW.GLFW_KEY_ENTER || input.key() == GLFW.GLFW_KEY_KP_ENTER) {
            if (this.searchInput != null && this.searchInput.isFocused()) {
                this.applySearch();
                return true;
            }
            if (this.jumpInput != null && this.jumpInput.isFocused()) {
                this.jumpToPage();
                return true;
            }
        }
        if (this.searchInput != null && this.searchInput.keyPressed(input)) {
            return true;
        }
        if (this.jumpInput != null && this.jumpInput.keyPressed(input)) {
            return true;
        }
        boolean typingInInputs = (this.searchInput != null && this.searchInput.isFocused())
                || (this.jumpInput != null && this.jumpInput.isFocused());
        if (!typingInInputs && this.minecraft.options.keyDrop.matches(input)) {
            if (this.isLockMode()) {
                this.dropHoveredStorageItem(input.hasControlDownWithQuirk(), false);
                return true;
            }
            if (this.isManageMode() && this.dropHoveredStorageItem(input.hasControlDownWithQuirk(), true)) {
                return true;
            }
        }
        if (!this.isManageMode() && input.key() == GLFW.GLFW_KEY_ESCAPE && !typingInInputs) {
            this.returnToPreviousScreen();
            return true;
        }
        boolean inventoryCloseKey = this.minecraft.options.keyInventory.matches(input);
        if (typingInInputs && inventoryCloseKey) {
            return true;
        }
        if (!this.isManageMode()) {
            return true;
        }
        this.beginInteractionSnapshot();
        boolean handled = super.keyPressed(input);
        if (handled) {
            this.finishInteractionSync();
        } else {
            this.interactionSnapshot = null;
        }
        return handled;
    }

    @Override
    public boolean charTyped(@NotNull CharacterEvent input) {
        if (this.searchInput != null && this.searchInput.charTyped(input)) {
            return true;
        }
        if (this.jumpInput != null && this.jumpInput.charTyped(input)) {
            return true;
        }
        return super.charTyped(input);
    }

    @Override
    protected @NotNull List<Component> getTooltipFromContainerItem(@NotNull ItemStack stack) {
        List<Component> tooltip = new ArrayList<>(super.getTooltipFromContainerItem(stack));
        SavedIndexItemEntry entry = this.hoveredStorageEntry();
        if (entry == null) {
            return tooltip;
        }
        tooltip.add(Component.empty());
        tooltip.add(ItemEditorText.tr("storage.info.title"));
        int nbtBytes = entry.nbtBytes > 0 ? entry.nbtBytes : this.estimateStackNbtBytes(stack);
        tooltip.add(Component.literal(StorageSizeText.sizeLine(nbtBytes)));
        tooltip.add(ItemEditorText.tr("storage.info.saved", this.formatSavedAt(entry.savedAt)));
        tooltip.add(ItemEditorText.tr("storage.info.version", this.storageMinecraftVersion(entry)));
        tooltip.add(ItemEditorText.tr("storage.info.data_version", this.storageDataVersion(entry)));
        tooltip.add(ItemEditorText.tr(
                "storage.info.location", Math.max(1, entry.page), Math.max(0, entry.slotInChunk) + 1));
        return tooltip;
    }

    private void buildPanelWidgets() {
        this.removePanelWidgets();

        int y = this.topPos;
        int halfWidth = (this.panelWidth - GAP) / 2;

        this.searchInput = new EditBox(
                this.font, this.panelX, y, this.panelWidth, BUTTON_HEIGHT, ItemEditorText.tr("storage.filter"));
        this.searchInput.setMaxLength(256);
        this.searchInput.setValue(this.currentQuery);
        this.searchInput.setHint(ItemEditorText.tr("storage.filter_hint"));
        this.searchInput.setResponder(value -> {
            this.liveSearchDueAt = System.currentTimeMillis() + SEARCH_DEBOUNCE_MS;
            this.updateAutocompleteHint(value);
        });
        this.addPanelWidget(this.searchInput);
        y += BUTTON_HEIGHT + GAP;

        int thirdWidth = (this.panelWidth - (GAP * 2)) / 3;
        this.addTokenButton(this.panelX, y, thirdWidth, "item:\"minecraft:stone\"");
        this.addTokenButton(this.panelX + thirdWidth + GAP, y, thirdWidth, "name:\"\"");
        this.addTokenButton(
                this.panelX + ((thirdWidth + GAP) * 2), y, this.panelWidth - ((thirdWidth + GAP) * 2), "lore:\"\"");
        y += BUTTON_HEIGHT + GAP;

        this.addTokenButton(this.panelX, y, thirdWidth, "amount:64");
        this.addTokenButton(this.panelX + thirdWidth + GAP, y, thirdWidth, "size:>=128");
        this.addTokenButton(
                this.panelX + ((thirdWidth + GAP) * 2), y, this.panelWidth - ((thirdWidth + GAP) * 2), "before:7d");
        y += BUTTON_HEIGHT + GAP;

        this.addPanelButton(
                this.panelX, y, this.panelWidth, ItemEditorText.tr("storage.search_clear"), this::clearSearch);
        y += BUTTON_HEIGHT + GAP;

        this.addPanelButton(
                this.panelX,
                y,
                halfWidth,
                ItemEditorText.tr("common.prev"),
                () -> this.changeView(() -> this.currentPage = Math.max(1, this.currentPage - 1)));
        this.addPanelButton(
                this.panelX + halfWidth + GAP,
                y,
                halfWidth,
                ItemEditorText.tr("common.next"),
                () -> this.changeView(() -> this.currentPage++));
        y += BUTTON_HEIGHT + GAP;

        int jumpButtonWidth = 40;
        int jumpInputWidth = this.panelWidth - jumpButtonWidth - GAP;
        this.jumpInput = new EditBox(
                this.font, this.panelX, y, jumpInputWidth, BUTTON_HEIGHT, ItemEditorText.tr("storage.jump"));
        this.jumpInput.setMaxLength(8);
        this.jumpInput.setValue(Integer.toString(this.currentPage));
        this.jumpInput.setHint(ItemEditorText.tr("storage.jump"));
        this.addPanelWidget(this.jumpInput);
        this.addPanelButton(
                this.panelX + jumpInputWidth + GAP,
                y,
                jumpButtonWidth,
                ItemEditorText.tr("storage.jump_apply"),
                this::jumpToPage);
        y += BUTTON_HEIGHT + GAP;

        int reverseWidth = 72;
        int sortWidth = Math.max(32, this.panelWidth - reverseWidth - GAP);
        this.sortButton = this.addPanelButton(
                this.panelX,
                y,
                sortWidth,
                ItemEditorText.tr("storage.sort_saved"),
                () -> this.changeView(() -> this.sortMode = this.sortMode.next()));
        this.reverseSortButton = this.addPanelButton(
                this.panelX + sortWidth + GAP,
                y,
                this.panelWidth - sortWidth - GAP,
                this.reverseSortButtonLabel(),
                () -> this.changeView(() -> this.reverseSort = !this.reverseSort));
        this.reverseSortButton.setTooltip(Tooltip.create(this.reverseSortTooltip()));
        y += BUTTON_HEIGHT + GAP;

        this.modeButton = this.addPanelButton(this.panelX, y, halfWidth, this.modeButtonLabel(), this::toggleMode);
        this.modeButton.active = !this.isSelectMode();
        this.modeButton.setTooltip(Tooltip.create(this.modeButtonTooltip()));
        this.addPanelButton(
                this.panelX + halfWidth + GAP,
                y,
                halfWidth,
                ItemEditorText.tr("storage.pages.button"),
                this::openPages);
        this.panelTextStartY = y + BUTTON_HEIGHT + 4;
    }

    private <T extends AbstractWidget> T addPanelWidget(T widget) {
        this.panelWidgets.add(widget);
        return this.addRenderableWidget(widget);
    }

    private Button addPanelButton(int x, int y, int width, Component label, Runnable action) {
        Button button = Button.builder(label, ignored -> action.run())
                .bounds(x, y, Math.max(1, width), BUTTON_HEIGHT)
                .build();
        this.updatePanelButtonLabel(button, label);
        return this.addPanelWidget(button);
    }

    private void updatePanelButtonLabel(Button button, Component label) {
        button.setMessage(label);
        button.setTooltip(this.font.width(label) > Math.max(4, button.getWidth() - 6) ? Tooltip.create(label) : null);
    }

    private void addTokenButton(int x, int y, int width, String tokenTemplate) {
        String label = tokenTemplate.contains(":")
                ? tokenTemplate.substring(0, tokenTemplate.indexOf(':') + 1)
                : tokenTemplate;
        Button button =
                this.addPanelButton(x, y, width, Component.literal(label), () -> this.insertSearchToken(tokenTemplate));
        button.setTooltip(Tooltip.create(Component.literal(tokenTemplate)));
    }

    private void insertSearchToken(String tokenTemplate) {
        if (this.searchInput == null) {
            return;
        }
        String current = this.searchInput.getValue().trim();
        String next = current.isBlank() ? tokenTemplate : current + " " + tokenTemplate;
        this.updateSearchInput(next, tokenTemplate);
    }

    private void resolvePanelLayout() {
        int rightStart = this.leftPos + this.imageWidth + PANEL_MARGIN;
        int rightSpace = Math.max(0, (this.width - PANEL_MARGIN) - rightStart);
        int leftSpace = Math.max(0, (this.leftPos - PANEL_MARGIN) - PANEL_MARGIN);
        boolean useRight = rightSpace >= leftSpace;
        if (FabricLoader.getInstance().isModLoaded("roughlyenoughitems")) {
            boolean preferredRight = ItemEditorReiClientPlugin.preferRightController(useRight);
            if ((preferredRight ? rightSpace : leftSpace) >= PANEL_HARD_MIN_WIDTH) useRight = preferredRight;
        }
        int space = useRight ? rightSpace : leftSpace;
        this.panelWidth = Math.clamp(space, PANEL_HARD_MIN_WIDTH, this.imageWidth - 8);
        if (useRight) {
            this.panelX = rightStart;
        } else {
            this.panelX = this.leftPos - PANEL_MARGIN - this.panelWidth;
        }
        if (this.panelX < PANEL_MARGIN) {
            this.panelX = PANEL_MARGIN;
        }
        int overflow = (this.panelX + this.panelWidth) - (this.width - PANEL_MARGIN);
        if (overflow > 0) {
            this.panelX = Math.max(PANEL_MARGIN, this.panelX - overflow);
        }
    }

    public ScreenRectangle controllerBounds() {
        int top = this.topPos;
        int bottom = Math.min(this.height - 4, top + this.imageHeight);
        return new ScreenRectangle(this.panelX - 4, top, this.panelWidth + 8, Math.max(0, bottom - top));
    }

    private void refreshData() {
        long requestId = ++this.refreshRequestSequence;
        this.activeRefreshRequest = requestId;
        String query = this.currentQuery;
        this.feedback(ItemEditorText.tr("storage.loading"), COLOR_HINT);
        this.applyContainerEntries(List.of(), Map.of(), true);
        this.captureBaselineVisibleStacks();
        this.storage
                .loadSnapshotAsync(
                        this.currentPage, query, this.sortMode, this.reverseSort, this.sessionRegistryAccess())
                .whenComplete((snapshot, throwable) ->
                        this.minecraft.execute(() -> this.applyLoadedSnapshot(requestId, snapshot, throwable)));
    }

    private void applyLoadedSnapshot(
            long requestId, SavedItemStorageService.PageSnapshot snapshot, Throwable throwable) {
        if (requestId != this.activeRefreshRequest) {
            return;
        }
        if (throwable != null) {
            Throwable root = throwable instanceof CompletionException completion && completion.getCause() != null
                    ? completion.getCause()
                    : throwable;
            String reason = root.getMessage() == null ? ItemEditorText.str("raw.unknown_error") : root.getMessage();
            this.feedback(ItemEditorText.tr("storage.load_failed", this.trimToPanel(reason)), UiColors.DANGER);
            return;
        }
        if (snapshot == null) {
            this.feedback(ItemEditorText.tr("storage.load_empty"), UiColors.DANGER);
            return;
        }

        SavedItemStorageService.PageResult result = snapshot.result();
        if (result == null) {
            this.feedback(ItemEditorText.tr("storage.load_empty_page"), UiColors.DANGER);
            return;
        }
        this.currentResult = result;
        this.currentPage = result.currentPage();
        this.rememberRuntimeView();
        this.storageTitleLabel = this.storageTitleComponent(result);
        this.pageLabel = Component.literal(this.pageLabelText(result));
        this.refreshStoredPagesLabel(snapshot.stats());
        this.totalLabel = ItemEditorText.tr("storage.total", result.totalResults());
        String searchText = this.currentQuery.isBlank() ? ItemEditorText.str("storage.all_items") : this.currentQuery;
        this.searchLabel = ItemEditorText.tr("storage.search", this.trimToPanel(searchText));
        if (this.currentQuery.isBlank()) {
            this.searchMetaLabel = ItemEditorText.tr("storage.filters_none");
        } else {
            StorageSearchQuery query = StorageSearchParser.parse(this.currentQuery);
            this.searchMetaLabel = ItemEditorText.tr(
                    "storage.filters_summary",
                    query.itemTokens.size(),
                    query.nameTokens.size(),
                    query.loreTokens.size(),
                    query.amountFilters.size(),
                    query.nbtSizeFilters.size(),
                    query.beforeDurationsMs.size() + query.afterDurationsMs.size(),
                    query.freeTokens.size());
        }
        this.searchAutoLabel =
                this.buildAutocompleteLabel(this.searchInput == null ? this.currentQuery : this.searchInput.getValue());
        if (this.jumpInput != null) {
            this.jumpInput.setValue(Integer.toString(this.currentPage));
        }

        if (this.sortButton != null && this.currentResult != null) {
            Component label =
                    switch (this.sortMode) {
                        case REGULAR -> ItemEditorText.tr("storage.sort_regular");
                        case SAVED_AT_DESC -> ItemEditorText.tr("storage.sort_saved");
                        case NAME_ASC -> ItemEditorText.tr("storage.sort_name");
                        case AMOUNT_DESC -> ItemEditorText.tr("storage.sort_amount");
                        case NBT_SIZE_DESC -> ItemEditorText.tr("storage.sort_size_bytes");
                    };
            this.updatePanelButtonLabel(this.sortButton, label);
            this.sortButton.visible = true;
            this.sortButton.active = true;
            if (this.reverseSortButton != null) {
                this.reverseSortButton.visible = true;
                this.reverseSortButton.active = this.sortMode != StorageSortMode.REGULAR;
                this.updatePanelButtonLabel(this.reverseSortButton, this.reverseSortButtonLabel());
                this.reverseSortButton.setTooltip(Tooltip.create(this.reverseSortTooltip()));
            }
            this.updateModeButton();
            int sortY = this.sortButton.getY();
            this.panelTextStartY = (this.modeButton == null ? sortY : this.modeButton.getY()) + BUTTON_HEIGHT + 4;
        }
        this.applyContainerEntries(result.entries(), snapshot.loadedStacks(), false);
        this.captureBaselineVisibleStacks();
        this.pageStatsRefreshPending = false;
    }

    private void applyContainerEntries(
            List<SavedIndexItemEntry> entries, Map<String, ItemStack> loadedStacks, boolean forceBroadcast) {
        this.slotEntries.clear();
        boolean changed = false;
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            ItemStack before = this.storageContainer.getItem(slot);
            if (!before.isEmpty()) {
                changed = true;
            }
            this.storageContainer.setItem(slot, ItemStack.EMPTY);
        }

        for (SavedIndexItemEntry entry : entries) {
            int slot = entry.slotInPage;
            if (slot < 0 || slot >= SLOT_COUNT) {
                continue;
            }
            ItemStack stack =
                    loadedStacks.getOrDefault(entry.id, ItemStack.EMPTY).copy();
            if (!ItemStack.matches(this.storageContainer.getItem(slot), stack)) {
                changed = true;
            }
            this.storageContainer.setItem(slot, stack);
            this.slotEntries.put(slot, entry);
        }
        if (forceBroadcast || changed) {
            this.storageContainer.setChanged();
            this.menu.broadcastChanges();
        }
    }

    private void applySearch() {
        if (this.searchInput == null) {
            return;
        }
        this.applySearchIfChanged(this.searchInput.getValue().trim());
    }

    private void clearSearch() {
        if (this.searchInput != null) {
            this.searchInput.setValue("");
        }
        this.applySearchIfChanged("");
    }

    private void jumpToPage() {
        int parsedPage = 1;
        try {
            parsedPage = Integer.parseInt((this.jumpInput == null ? "" : this.jumpInput.getValue()).trim());
        } catch (NumberFormatException ignored) {
        }
        final int requestedPage = parsedPage;
        this.changeView(() -> this.currentPage = Math.max(1, requestedPage));
    }

    private void extractPanelText(GuiGraphicsExtractor context) {
        String header = this.pageLabel.getString() + " | " + this.storedPagesLabel.getString();
        int headerMaxWidth = Math.max(60, this.imageWidth - 16);
        String fittedHeader = header;
        if (header.isBlank()) {
            fittedHeader = "";
        } else if (this.font.width(header) > headerMaxWidth) {
            String ellipsis = "...";
            int end = header.length();
            while (end > 0 && this.font.width(header.substring(0, end) + ellipsis) > headerMaxWidth) {
                end--;
            }
            fittedHeader = end <= 0 ? ellipsis : header.substring(0, end) + ellipsis;
        }
        context.text(this.font, Component.literal(fittedHeader), this.leftPos + 8, this.topPos + 6, UiColors.SUCCESS);

        int textX = this.panelX;
        ScreenRectangle bounds = this.controllerBounds();
        context.enableScissor(bounds.left(), bounds.top(), bounds.right(), bounds.bottom());
        int y = Math.max(this.topPos + 108, this.panelTextStartY) - PANEL_TEXT_LINE_HEIGHT;
        if (!this.feedbackLabel.getString().isBlank()) {
            y = this.drawPanelLine(context, textX, y, this.feedbackLabel, this.feedbackColor);
        }
        y = this.drawPanelLine(context, textX, y, this.searchLabel, COLOR_HINT);
        y = this.drawPanelLine(context, textX, y, this.pageLabel, COLOR_TEXT);
        y = this.drawPanelLine(context, textX, y, this.storedPagesLabel, COLOR_TEXT);
        y = this.drawPanelLine(context, textX, y, this.totalLabel, UiColors.MUTED);
        y = this.drawPanelLine(context, textX, y, this.searchMetaLabel, COLOR_HINT);
        y = this.drawPanelLine(context, textX, y, this.searchAutoLabel, COLOR_HINT);
        y += 4;
        SavedIndexItemEntry hoveredEntry = null;
        int hoveredStorageSlot = this.hoveredStorageSlotIndex();
        if (hoveredStorageSlot >= 0) {
            hoveredEntry = this.slotEntries.get(hoveredStorageSlot);
        }
        if (hoveredEntry == null) {
            y = this.drawPanelLine(context, textX, y, ItemEditorText.tr("storage.hover_location"), COLOR_HINT);
        } else {
            y = this.drawPanelLine(context, textX, y, ItemEditorText.tr("storage.item_location"), UiColors.SUCCESS);
            y = this.drawPanelLine(
                    context,
                    textX,
                    y,
                    ItemEditorText.tr("storage.info.location", hoveredEntry.page, hoveredEntry.slotInChunk + 1),
                    UiColors.SUCCESS);
            if (hoveredEntry.pageNamePlain != null && !hoveredEntry.pageNamePlain.isBlank()) {
                y = this.drawPanelLine(
                        context, textX, y, Component.literal(this.trimToPanel(hoveredEntry.pageNamePlain)), COLOR_HINT);
            }
            if (hoveredEntry.lorePlain != null && !hoveredEntry.lorePlain.isEmpty()) {
                y = this.drawPanelLine(
                        context,
                        textX,
                        y,
                        ItemEditorText.tr("storage.lore", this.trimToPanel(hoveredEntry.lorePlain.getFirst())),
                        COLOR_HINT);
            }
        }
        y += 6;
        y = this.drawPanelLine(context, textX, y, HINT_SEARCH, COLOR_HINT);
        y = this.drawPanelLine(context, textX, y, this.pickHint(), COLOR_HINT);
        if (this.isManageMode()) {
            y = this.drawPanelLine(context, textX, y, HINT_VANILLA, COLOR_HINT);
            this.drawPanelLine(context, textX, y, HINT_SYNC, COLOR_HINT);
        }
        context.disableScissor();
    }

    private void feedback(Component message, int color) {
        this.feedbackLabel = message;
        this.feedbackColor = color;
    }

    private String pageLabelText(SavedItemStorageService.PageResult result) {
        String base = ItemEditorText.str("storage.page_current", result.currentPage());
        if (result.pageNamePlain() == null || result.pageNamePlain().isBlank()) {
            return base + ": " + ItemEditorText.str("storage.default_page");
        }
        return base + ": " + result.pageNamePlain();
    }

    private Component storageTitleComponent(SavedItemStorageService.PageResult result) {
        String name = result.pageName() == null || result.pageName().isBlank()
                ? ItemEditorText.str("storage.default_page")
                : result.pageName();
        return ItemEditorText.tr("storage.page_short", result.currentPage())
                .copy()
                .append(" | ")
                .append(TextComponentUtil.parseMarkup(name));
    }

    private void beginInteractionSnapshot() {
        if (this.interactionSnapshot != null) {
            return;
        }
        Map<Integer, ItemStack> snapshot = new HashMap<>();
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            snapshot.put(slot, this.storageContainer.getItem(slot).copy());
        }
        this.interactionSnapshot = new InteractionSnapshot(new HashMap<>(this.slotEntries), snapshot);
        this.capturePlayerInventoryBeforeInteraction();
    }

    private void finishInteractionSync() {
        if (this.isSelectMode()) {
            this.interactionSnapshot = null;
            this.playerInventoryBeforeInteraction.clear();
            return;
        }
        if (this.interactionSnapshot == null) {
            return;
        }
        InteractionSnapshot snapshot = this.interactionSnapshot;
        this.interactionSnapshot = null;
        if (this.isLockMode()) {
            this.syncChangedPlayerInventorySlots();
            return;
        }
        if (this.isReadOnlyLayoutView()) {
            this.refreshData();
            this.feedback(ItemEditorText.tr("storage.edit_requires_regular"), UiColors.MUTED);
            return;
        }
        this.persistMutations(snapshot.beforeEntries(), snapshot.beforeStacks());
        this.syncChangedPlayerInventorySlots();
    }

    private void capturePlayerInventoryBeforeInteraction() {
        this.playerInventoryBeforeInteraction.clear();
        this.playerInventoryBeforeInteraction.putAll(ClientInventorySyncService.snapshot(this.minecraft));
    }

    private void syncChangedPlayerInventorySlots() {
        int synced = ClientInventorySyncService.syncChangedSlots(this.minecraft, this.playerInventoryBeforeInteraction);
        this.playerInventoryBeforeInteraction.clear();
        if (synced > 0) {
            this.feedback(ItemEditorText.tr("storage.synced_slots", synced), UiColors.SUCCESS);
        }
    }

    private boolean isReadOnlyLayoutView() {
        return this.sortMode != StorageSortMode.REGULAR
                || !this.currentQuery.isBlank()
                || this.currentResult == null
                || this.currentResult.searchMode();
    }

    private boolean isReadOnlyStorageSlot(int slotId) {
        return this.isStorageSlotId(slotId) && this.isReadOnlyLayoutView();
    }

    private boolean handleSearchShortcuts(KeyEvent input) {
        if (this.searchInput == null) {
            return false;
        }
        if (input.key() == GLFW.GLFW_KEY_TAB && (this.jumpInput == null || !this.jumpInput.isFocused())) {
            if (!this.searchInput.isFocused()) {
                this.focusPanelInput(this.searchInput);
                this.searchInput.setCursorPosition(this.searchInput.getValue().length());
            }
            return this.applyAutocomplete(input.hasShiftDown());
        }
        if (input.hasControlDownWithQuirk() && input.key() == GLFW.GLFW_KEY_F) {
            this.focusPanelInput(this.searchInput);
            this.searchInput.setCursorPosition(this.searchInput.getValue().length());
            return true;
        }
        if (input.hasControlDownWithQuirk() && input.key() == GLFW.GLFW_KEY_L) {
            this.clearSearch();
            this.focusPanelInput(this.searchInput);
            return true;
        }
        if (input.key() == GLFW.GLFW_KEY_ESCAPE && this.searchInput.isFocused()) {
            this.setFocused(null);
            return true;
        }
        return false;
    }

    private void focusPanelInput(EditBox input) {
        this.setFocused(input);
        input.setFocused(true);
    }

    private void applySearchIfChanged(String value) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.equals(this.currentQuery)) {
            return;
        }
        this.commitStorageBeforeViewChange();
        this.currentQuery = normalized;
        this.currentPage = 1;
        this.updateModeForCurrentView();
        this.rememberRuntimeView();
        this.refreshData();
    }

    private void saveModePreference() {
        PreferencesFileModel preferences = StorageServices.foundation().loadPreferences();
        preferences.storageMode = this.mode.name();
        StorageServices.foundation().savePreferences(preferences);
    }

    @Override
    public void onClose() {
        this.commitStorageBeforeViewChange();
        if (this.isManageMode()) {
            this.storage.flushQueuedWrites();
        }
        super.onClose();
    }

    @Override
    public void removed() {
        this.removePanelWidgets();
        super.removed();
    }

    private void removePanelWidgets() {
        for (AbstractWidget widget : this.panelWidgets) {
            this.removeWidget(widget);
        }
        this.panelWidgets.clear();
        this.searchInput = null;
        this.jumpInput = null;
        this.sortButton = null;
        this.reverseSortButton = null;
        this.modeButton = null;
    }

    private boolean isPanelMouse(double mouseX, double mouseY) {
        ScreenRectangle bounds = this.controllerBounds();
        return mouseX >= bounds.left() && mouseX < bounds.right() && mouseY >= bounds.top() && mouseY < bounds.bottom();
    }

    private String trimToPanel(String value) {
        int maxChars = Math.max(16, (this.panelWidth - 12) / 6);
        if (value == null || value.isBlank()) {
            return "";
        }
        if (value.length() <= maxChars) {
            return value;
        }
        return value.substring(0, Math.max(3, maxChars - 3)) + "...";
    }

    private void updateAutocompleteHint(String value) {
        this.searchAutoLabel = this.buildAutocompleteLabel(value);
    }

    private void toggleMode() {
        if (this.isSelectMode()) {
            return;
        }
        if (this.isLockMode()) {
            this.mode = StorageScreenMode.MANAGE;
            this.preferredMode = this.mode;
            this.currentQuery = "";
            this.sortMode = StorageSortMode.REGULAR;
            this.reverseSort = false;
            if (this.searchInput != null) {
                this.searchInput.setValue("");
            }
            this.updateModeButton();
            this.saveModePreference();
            this.rememberRuntimeView();
            this.refreshData();
            return;
        }

        if (this.currentPageSaveFailedForModeChange()) {
            return;
        }
        this.mode = StorageScreenMode.COPY_IMPORT;
        this.preferredMode = this.mode;
        this.updateModeButton();
        this.saveModePreference();
        this.refreshData();
        this.feedback(ItemEditorText.tr("storage.mode_lock_enabled"), COLOR_HINT);
    }

    private boolean currentPageSaveFailedForModeChange() {
        if (!this.isManageMode()) {
            return false;
        }
        try {
            this.commitStorageBeforeViewChange();
            this.storage.flushQueuedWrites();
            this.pageStatsRefreshPending = false;
            this.refreshStoredPagesLabel(this.storage.pageStats());
            return false;
        } catch (RuntimeException exception) {
            String reason =
                    exception.getMessage() == null ? ItemEditorText.str("raw.unknown_error") : exception.getMessage();
            this.feedback(
                    ItemEditorText.tr("editor.apply.save_storage_failed", this.trimToPanel(reason)), UiColors.DANGER);
            return true;
        }
    }

    private void updateModeButton() {
        if (this.modeButton == null) {
            return;
        }
        this.updatePanelButtonLabel(this.modeButton, this.modeButtonLabel());
        this.modeButton.active = !this.isSelectMode();
        this.modeButton.setTooltip(Tooltip.create(this.modeButtonTooltip()));
    }

    private void openPages() {
        if (this.currentPageSaveFailedForModeChange()) {
            return;
        }
        this.minecraft.setScreenAndShow(new StoragePagesScreen(
                this.minecraft,
                this.currentPage,
                this.currentQuery,
                this.sortMode,
                this.preferredMode,
                this.returnScreen,
                this.pickedStackConsumer));
    }

    private Component pickHint() {
        return switch (this.mode) {
            case MANAGE -> HINT_OPEN_EDITOR;
            case COPY_IMPORT -> HINT_LOCK;
            case SELECT -> HINT_PICK_STACK;
        };
    }

    private Component modeButtonLabel() {
        return ItemEditorText.tr(
                switch (this.mode) {
                    case MANAGE -> "storage.mode_manage";
                    case COPY_IMPORT -> "storage.mode_lock";
                    case SELECT -> "storage.mode_selection";
                });
    }

    private Component modeButtonTooltip() {
        return ItemEditorText.tr(
                switch (this.mode) {
                    case MANAGE -> "storage.mode_manage.tooltip";
                    case COPY_IMPORT -> "storage.mode_lock.tooltip";
                    case SELECT -> "storage.mode_selection.tooltip";
                });
    }

    private Component buildAutocompleteLabel(String value) {
        StorageSearchAutocompleteUtil.Completion autocomplete =
                StorageSearchAutocompleteUtil.complete(value, SEARCH_AUTOCOMPLETE_TOKENS);
        String[] suggestions = autocomplete.suggestions();
        if (suggestions.length == 0) {
            return ItemEditorText.tr("storage.autocomplete.none");
        }
        String preview = suggestions[0];
        if (suggestions.length > 1) {
            preview += " | " + suggestions[1];
        }
        return ItemEditorText.tr("storage.autocomplete.suggestion", this.trimToPanel(preview));
    }

    private boolean applyAutocomplete(boolean reverse) {
        if (this.searchInput == null) {
            return false;
        }
        String current = this.searchInput.getValue();
        StorageSearchAutocompleteUtil.Completion autocomplete =
                StorageSearchAutocompleteUtil.complete(current, SEARCH_AUTOCOMPLETE_TOKENS);
        String[] suggestions = autocomplete.suggestions();
        if (suggestions.length == 0) {
            return false;
        }

        String currentToken = autocomplete.prefix();
        int nextIndex = 0;
        for (int i = 0; i < suggestions.length; i++) {
            if (suggestions[i].equalsIgnoreCase(currentToken)) {
                nextIndex =
                        reverse ? Math.floorMod(i - 1, suggestions.length) : Math.floorMod(i + 1, suggestions.length);
                break;
            }
        }

        String chosen = suggestions[nextIndex];
        String nextValue = autocomplete.base() + chosen;
        this.updateSearchInput(nextValue, chosen);
        this.updateAutocompleteHint(nextValue);
        return true;
    }

    private void refreshStoredPagesLabel(SavedItemStorageService.PageStats stats) {
        int storedPages = stats.storedPages();
        this.storedPagesLabel = Component.literal(
                stats.emptyPages() > 0
                        ? ItemEditorText.str("storage.stored_pages_with_empty", storedPages, stats.emptyPages())
                        : ItemEditorText.str("storage.stored_pages", storedPages));
    }

    private Component reverseSortButtonLabel() {
        String key =
                switch (this.sortMode) {
                    case REGULAR -> "storage.sort.direction.slot";
                    case SAVED_AT_DESC ->
                        this.reverseSort ? "storage.sort.direction.old_new" : "storage.sort.direction.new_old";
                    case NAME_ASC -> this.reverseSort ? "storage.sort.direction.z_a" : "storage.sort.direction.a_z";
                    case AMOUNT_DESC ->
                        this.reverseSort ? "storage.sort.direction.low_high" : "storage.sort.direction.high_low";
                    case NBT_SIZE_DESC ->
                        this.reverseSort ? "storage.sort.direction.small_big" : "storage.sort.direction.big_small";
                };
        return ItemEditorText.tr(key);
    }

    private Component reverseSortTooltip() {
        String key =
                switch (this.sortMode) {
                    case REGULAR -> "storage.sort.tooltip.regular";
                    case SAVED_AT_DESC ->
                        this.reverseSort ? "storage.sort.tooltip.saved_asc" : "storage.sort.tooltip.saved_desc";
                    case NAME_ASC ->
                        this.reverseSort ? "storage.sort.tooltip.name_desc" : "storage.sort.tooltip.name_asc";
                    case AMOUNT_DESC ->
                        this.reverseSort ? "storage.sort.tooltip.amount_asc" : "storage.sort.tooltip.amount_desc";
                    case NBT_SIZE_DESC ->
                        this.reverseSort ? "storage.sort.tooltip.size_asc" : "storage.sort.tooltip.size_desc";
                };
        return ItemEditorText.tr(key);
    }

    private @Nullable SavedIndexItemEntry hoveredStorageEntry() {
        int storageSlot = this.hoveredStorageSlotIndex();
        if (storageSlot < 0) {
            return null;
        }
        return this.slotEntries.get(storageSlot);
    }

    private String storageMinecraftVersion(SavedIndexItemEntry entry) {
        if (entry == null || entry.minecraftVersion == null || entry.minecraftVersion.isBlank()) {
            return ItemEditorText.str("common.current");
        }
        return entry.minecraftVersion;
    }

    private String storageDataVersion(SavedIndexItemEntry entry) {
        if (entry == null || entry.dataVersion <= 0) {
            return ItemEditorText.str("common.current");
        }
        return Integer.toString(entry.dataVersion);
    }

    private String formatSavedAt(long savedAt) {
        if (savedAt <= 0L) {
            return ItemEditorText.str("raw.unknown_error");
        }
        return SAVED_AT_FORMATTER.format(Instant.ofEpochMilli(savedAt));
    }

    private int estimateStackNbtBytes(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }
        DataResult<Tag> encoded = ItemStack.CODEC.encodeStart(
                this.sessionRegistryAccess().createSerializationContext(NbtOps.INSTANCE), stack);
        Tag tag = encoded.result().orElse(null);
        if (!(tag instanceof CompoundTag compound)) {
            return 0;
        }
        return StorageNbtSizeUtil.nbtByteSize(compound);
    }

    private int drawPanelLine(GuiGraphicsExtractor context, int textX, int y, Component text, int color) {
        context.text(this.font, text, textX, y, color);
        return y + PANEL_TEXT_LINE_HEIGHT;
    }

    private void updateSearchInput(String value, String insertedToken) {
        if (this.searchInput == null) {
            return;
        }
        this.searchInput.setValue(value);
        int cursor = value.length();
        if (insertedToken != null && insertedToken.endsWith("\"\"")) {
            cursor--;
        }
        this.searchInput.setCursorPosition(cursor);
        this.focusPanelInput(this.searchInput);
        this.liveSearchDueAt = System.currentTimeMillis() + SEARCH_DEBOUNCE_MS;
    }

    private RegistryAccess sessionRegistryAccess() {
        if (this.minecraft.level != null) {
            return this.minecraft.level.registryAccess();
        }
        return RegistryAccess.EMPTY;
    }

    private void commitStorageBeforeViewChange() {
        if (this.isSelectMode()) {
            return;
        }
        this.finishInteractionSync();
        if (!this.isManageMode() || this.isReadOnlyLayoutView()) {
            return;
        }
        this.persistMutations(this.slotEntries, this.baselineVisibleStacks);
    }

    private void changeView(Runnable viewChange) {
        this.commitStorageBeforeViewChange();
        viewChange.run();
        this.updateModeForCurrentView();
        this.rememberRuntimeView();
        this.refreshData();
    }

    private void updateModeForCurrentView() {
        if (this.isSelectMode()) {
            return;
        }
        this.mode = this.sortMode != StorageSortMode.REGULAR || !this.currentQuery.isBlank()
                ? StorageScreenMode.COPY_IMPORT
                : this.preferredMode;
        this.updateModeButton();
    }

    private void persistMutations(
            Map<Integer, SavedIndexItemEntry> entryBySlot, Map<Integer, ItemStack> baselineStacks) {
        RegistryAccess registryAccess = this.sessionRegistryAccess();
        List<SavedItemStorageService.SlotMutation> mutations = this.collectSlotMutations(entryBySlot, baselineStacks);
        if (mutations.isEmpty()) {
            return;
        }
        this.storage.enqueueApplySlotMutations(this.currentPage, mutations, registryAccess);
        this.feedback(ItemEditorText.tr("storage.saved_ok"), UiColors.SUCCESS);
        this.captureBaselineVisibleStacks();
        this.pageStatsRefreshPending = true;
        this.pageStatsRefreshDueAt = System.currentTimeMillis() + PAGE_STATS_REFRESH_MS;
    }

    private List<SavedItemStorageService.SlotMutation> collectSlotMutations(
            Map<Integer, SavedIndexItemEntry> entryBySlot, Map<Integer, ItemStack> baselineStacks) {
        List<SavedItemStorageService.SlotMutation> mutations = new ArrayList<>();
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            ItemStack before = baselineStacks.getOrDefault(slot, ItemStack.EMPTY);
            ItemStack after = this.storageContainer.getItem(slot).copy();
            if (ItemStack.matches(before, after)) {
                continue;
            }
            SavedIndexItemEntry entry = entryBySlot.get(slot);
            String entryId = entry == null || entry.id == null || entry.id.isBlank() ? null : entry.id;
            mutations.add(new SavedItemStorageService.SlotMutation(slot, entryId, after));
        }
        return mutations;
    }

    private void captureBaselineVisibleStacks() {
        this.baselineVisibleStacks.clear();
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            this.baselineVisibleStacks.put(
                    slot, this.storageContainer.getItem(slot).copy());
        }
    }

    private boolean openStorageItem(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= SLOT_COUNT) {
            return false;
        }
        SavedIndexItemEntry entry = this.slotEntries.get(slotIndex);
        ItemStack stack = this.storageContainer.getItem(slotIndex).copy();
        if (entry == null || stack.isEmpty()) {
            this.feedback(ItemEditorText.tr("storage.pick_empty"), COLOR_HINT);
            return true;
        }
        ItemEditorSessionOrigin.Storage origin = new ItemEditorSessionOrigin.Storage(entry, stack.copy());
        this.minecraft.setScreenAndShow(new ItemEditorScreen(new ItemEditorSession(this.minecraft, stack, origin)));
        return true;
    }

    private void copyStorageItem(Slot slot) {
        ItemStack stored = slot.getItem();
        ItemStack carried = this.menu.getCarried();
        if (!carried.isEmpty() && !ItemStack.isSameItemSameComponents(stored, carried)) {
            this.feedback(ItemEditorText.tr("storage.copy_cursor_occupied"), UiColors.DANGER);
            return;
        }
        if (carried.isEmpty()) {
            this.menu.setCarried(stored.copy());
        } else {
            carried.setCount(Math.min(carried.getMaxStackSize(), carried.getCount() + stored.getCount()));
            this.menu.setCarried(carried);
        }
        this.feedback(ItemEditorText.tr("storage.copy_ok"), UiColors.SUCCESS);
    }

    private void copyStorageItemToInventory(Slot slot) {
        if (ClientInventorySyncService.putInFreeSlot(this.minecraft, slot.getItem())) {
            this.feedback(ItemEditorText.tr("storage.copy_inventory_ok"), UiColors.SUCCESS);
        } else {
            this.feedback(ItemEditorText.tr("storage.copy_inventory_failed"), UiColors.DANGER);
        }
    }

    private boolean dropHoveredStorageItem(boolean wholeStack, boolean removeFromStorage) {
        Slot slot = this.hoveredSlot;
        int slotId = slot == null ? -1 : this.menu.slots.indexOf(slot);
        if (slot == null || !this.isStorageSlotId(slotId) || !slot.hasItem()) {
            return false;
        }
        if (removeFromStorage && this.isReadOnlyStorageSlot(slotId)) {
            this.feedback(ItemEditorText.tr("storage.edit_requires_regular"), UiColors.MUTED);
            return true;
        }
        int amount = dropAmount(slot.getItem(), wholeStack, removeFromStorage);
        if (!ClientInventorySyncService.dropStack(this.minecraft, slot.getItem().copyWithCount(amount))) {
            this.feedback(ItemEditorText.tr("storage.drop_unavailable"), UiColors.DANGER);
            return true;
        }
        if (removeFromStorage) {
            this.beginInteractionSnapshot();
            slot.remove(amount);
            this.finishInteractionSync();
        }
        return true;
    }

    static int dropAmount(ItemStack stack, boolean wholeStack, boolean removeFromStorage) {
        return wholeStack ? (removeFromStorage ? stack.getCount() : stack.getMaxStackSize()) : 1;
    }

    private boolean pickClickedStack(Slot slot, int slotId) {
        if (this.pickedStackConsumer == null
                || slot == null
                || !slot.hasItem()
                || slotId < 0
                || slotId >= this.menu.slots.size()) {
            return false;
        }
        ItemStack picked = slot.getItem().copy();
        if (this.returnScreen instanceof ItemEditorScreen editor) {
            this.returnToPreviousScreen();
            editor.choosePickedItem(picked, this.pickedStackConsumer);
        } else {
            this.pickedStackConsumer.accept(picked);
            this.returnToPreviousScreen();
        }
        return true;
    }

    private void returnToPreviousScreen() {
        if (this.returnScreen instanceof ItemEditorScreen itemEditorScreen) {
            itemEditorScreen.resize(this.width, this.height);
            itemEditorScreen.requestResponsiveRelayout();
        }
        this.minecraft.setScreenAndShow(this.returnScreen);
    }

    private boolean isManageMode() {
        return this.mode == StorageScreenMode.MANAGE;
    }

    private boolean isLockMode() {
        return this.mode == StorageScreenMode.COPY_IMPORT;
    }

    private boolean isSelectMode() {
        return this.mode == StorageScreenMode.SELECT;
    }

    private boolean isStorageSlotId(int slotId) {
        return slotId >= 0 && slotId < SLOT_COUNT;
    }

    static int lastRuntimePage() {
        return lastPage;
    }

    static String lastRuntimeQuery() {
        return lastQuery;
    }

    static StorageSortMode lastRuntimeSortMode() {
        return lastSortMode;
    }

    static StorageScreenMode rememberedMode() {
        return StorageScreenMode.valueOf(StorageServices.foundation().loadPreferences().storageMode);
    }

    private void rememberRuntimeView() {
        lastPage = this.currentPage;
        lastQuery = this.currentQuery;
        lastSortMode = this.sortMode;
        lastReverseSort = this.reverseSort;
    }

    private int hoveredStorageSlotIndex() {
        Slot hovered = this.hoveredSlot;
        if (hovered == null) {
            return -1;
        }
        int slotId = this.menu.slots.indexOf(hovered);
        if (!this.isStorageSlotId(slotId)) {
            return -1;
        }
        return hovered.index;
    }

    private static Inventory requireInventory() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            throw new IllegalStateException("Player is required to open storage screen");
        }
        return minecraft.player.getInventory();
    }

    private record InteractionSnapshot(
            Map<Integer, SavedIndexItemEntry> beforeEntries, Map<Integer, ItemStack> beforeStacks) {}
}
