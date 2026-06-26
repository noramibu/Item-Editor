package me.noramibu.itemeditor.ui.screen;

import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.service.ClientInventorySyncService;
import me.noramibu.itemeditor.ui.panel.specialdata.ContainerEntryDraftUtil;
import me.noramibu.itemeditor.util.ItemEditorText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.HopperBlock;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class ContainerEditorScreen extends ContainerScreen {
    private static final int COLUMNS = 9;
    private static final int BUTTON_WIDTH = 72;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_GAP = 4;

    private final ItemEditorScreen returnScreen;
    private final ItemEditorState.SpecialData special;
    private final SimpleContainer container;
    private final Inventory playerInventory;
    private final Map<Integer, ItemStack> inventoryBeforeEdit;
    private final int editableSlots;
    private final EditorMode mode;
    private int bundlePage;
    private Button previousPageButton;
    private Button nextPageButton;

    public ContainerEditorScreen(
            ItemEditorScreen returnScreen,
            ItemEditorState.SpecialData special,
            ItemStack originalStack
    ) {
        this(returnScreen, special, screenData(special, originalStack));
    }

    private ContainerEditorScreen(
            ItemEditorScreen returnScreen,
            ItemEditorState.SpecialData special,
            ScreenData data
    ) {
        this(returnScreen, special, data, EditorMode.CONTAINER, requireInventory());
    }

    public static ContainerEditorScreen bundle(
            ItemEditorScreen returnScreen,
            ItemEditorState.SpecialData special
    ) {
        int page = Math.max(0, special.bundleEditorPage);
        return new ContainerEditorScreen(returnScreen, special, bundleScreenData(special, page), EditorMode.BUNDLE, requireInventory());
    }

    private ContainerEditorScreen(
            ItemEditorScreen returnScreen,
            ItemEditorState.SpecialData special,
            ScreenData data,
            EditorMode mode,
            Inventory inventory
    ) {
        super(
                new ChestMenu(menuType(data.rows()), 0, inventory, data.container(), data.rows()),
                inventory,
                data.title()
        );
        this.returnScreen = returnScreen;
        this.special = special;
        this.container = data.container();
        this.playerInventory = inventory;
        this.inventoryBeforeEdit = ClientInventorySyncService.snapshot(Minecraft.getInstance());
        this.editableSlots = data.rows() * COLUMNS;
        this.mode = mode;
        this.bundlePage = data.bundlePage();
    }

    @Override
    protected void init() {
        super.init();
        if (this.mode == EditorMode.BUNDLE) {
            this.addBundleSideControls();
            return;
        }
        int cancelX = this.centeredButtonRowX();
        int doneX = cancelX + BUTTON_WIDTH + BUTTON_GAP;
        int actionRowY = Math.max(4, this.topPos - BUTTON_HEIGHT - BUTTON_GAP);
        this.addRenderableWidget(Button.builder(ItemEditorText.tr("common.cancel"), ignored -> this.cancel())
                .bounds(cancelX, actionRowY, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
        this.addRenderableWidget(Button.builder(ItemEditorText.tr("special.container.done"), ignored -> this.done())
                .bounds(doneX, actionRowY, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
    }

    private void addBundleSideControls() {
        int buttonWidth = this.sideButtonWidth();
        boolean compact = buttonWidth < BUTTON_WIDTH;
        int leftX = Math.max(4, this.leftPos - buttonWidth - BUTTON_GAP);
        int rightX = Math.min(this.width - buttonWidth - 4, this.leftPos + this.imageWidth + BUTTON_GAP);
        int firstY = this.topPos + (this.imageHeight - BUTTON_HEIGHT * 2 - BUTTON_GAP) / 2;
        int secondY = firstY + BUTTON_HEIGHT + BUTTON_GAP;

        this.previousPageButton = Button.builder(compact ? Component.literal("<") : ItemEditorText.tr("common.prev"), ignored -> this.switchBundlePage(-1))
                .bounds(leftX, firstY, buttonWidth, BUTTON_HEIGHT)
                .build();
        this.nextPageButton = Button.builder(compact ? Component.literal(">") : ItemEditorText.tr("common.next"), ignored -> this.switchBundlePage(1))
                .bounds(rightX, firstY, buttonWidth, BUTTON_HEIGHT)
                .build();
        this.addRenderableWidget(this.previousPageButton);
        this.addRenderableWidget(this.nextPageButton);
        this.addRenderableWidget(Button.builder(compact ? Component.literal("X") : ItemEditorText.tr("common.cancel"), ignored -> this.cancel())
                .bounds(leftX, secondY, buttonWidth, BUTTON_HEIGHT)
                .build());
        this.addRenderableWidget(Button.builder(compact ? Component.literal("OK") : ItemEditorText.tr("special.container.done"), ignored -> this.done())
                .bounds(rightX, secondY, buttonWidth, BUTTON_HEIGHT)
                .build());
        this.updateBundlePageButtons();
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics context, int mouseX, int mouseY) {
        context.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, -12566464, false);
        context.drawString(
                this.font,
                ItemEditorText.tr("special.container.inventory_copy"),
                this.inventoryLabelX,
                this.inventoryLabelY,
                -12566464,
                false
        );
        if (this.mode == EditorMode.BUNDLE) {
            context.drawString(
                    this.font,
                    ItemEditorText.tr("special.bundle.page", this.bundlePage + 1),
                    this.titleLabelX + this.font.width(this.title) + 8,
                    this.titleLabelY,
                    -12566464,
                    false
            );
        }
    }

    @Override
    protected void slotClicked(@Nullable Slot slot, int slotId, int button, @NotNull ClickType type) {
        if (slot != null) {
            slotId = slot.index;
        }
        if (!allowedClick(slotId, type)) {
            return;
        }
        if (this.minecraft.player == null) {
            return;
        }
        this.menu.clicked(slotId, button, type, this.minecraft.player);
        this.menu.broadcastChanges();
        if (this.mode == EditorMode.BUNDLE) {
            this.updateBundlePageButtons();
        }
    }

    @Override
    public boolean keyPressed(@NotNull KeyEvent input) {
        if (input.key() == GLFW.GLFW_KEY_ESCAPE || this.minecraft.options.keyInventory.matches(input)) {
            this.cancel();
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public void onClose() {
        this.cancel();
    }

    private void done() {
        this.putCarriedBack();
        this.writeBack();
        ClientInventorySyncService.syncChangedSlots(this.minecraft, this.inventoryBeforeEdit);
        this.returnScreen.session().rebuildPreview();
        this.returnToEditor();
    }

    private void cancel() {
        this.menu.setCarried(ItemStack.EMPTY);
        this.restorePlayerInventory();
        this.returnToEditor();
    }

    private void returnToEditor() {
        this.returnScreen.resize(this.width, this.height);
        this.returnScreen.requestResponsiveRelayout();
        this.minecraft.setScreen(this.returnScreen);
    }

    private void putCarriedBack() {
        ItemStack carried = this.menu.getCarried();
        if (carried.isEmpty()) {
            return;
        }
        ItemStack copy = carried.copy();
        this.playerInventory.add(copy);
        ItemStack remainder = copy.isEmpty() ? ItemStack.EMPTY : this.container.addItem(copy);
        this.menu.setCarried(remainder.isEmpty() ? ItemStack.EMPTY : remainder);
    }

    private void restorePlayerInventory() {
        for (Map.Entry<Integer, ItemStack> entry : this.inventoryBeforeEdit.entrySet()) {
            this.playerInventory.setItem(entry.getKey(), entry.getValue().copy());
        }
    }

    private void writeBack() {
        if (this.mode == EditorMode.BUNDLE) {
            this.writeBundlePageBack();
            return;
        }

        List<ItemEditorState.ContainerEntryDraft> preserved = this.special.containerEntries.stream()
                .filter(draft -> {
                    Integer slot = parseSlot(draft.slot);
                    return slot == null || slot < 0 || slot >= this.editableSlots;
                })
                .toList();

        this.special.containerEntries.clear();
        for (int slot = 0; slot < this.editableSlots; slot++) {
            ItemStack stack = this.container.getItem(slot);
            if (!stack.isEmpty()) {
                this.special.containerEntries.add(ItemEditorState.ContainerEntryDraft.fromSlot(slot, stack.copy()));
            }
        }
        this.special.containerEntries.addAll(preserved);
        this.special.containerEntries.sort(Comparator.comparingInt(ContainerEditorScreen::slotOrMax));
    }

    private void switchBundlePage(int delta) {
        this.putCarriedBack();
        this.writeBundlePageBack();
        this.bundlePage = Math.clamp(this.bundlePage + delta, 0, this.maxBundlePage());
        this.special.bundleEditorPage = this.bundlePage;
        this.loadBundlePage();
        this.menu.broadcastChanges();
        this.updateBundlePageButtons();
    }

    private void updateBundlePageButtons() {
        if (this.previousPageButton != null) {
            this.previousPageButton.active = this.bundlePage > 0;
        }
        if (this.nextPageButton != null) {
            this.nextPageButton.active = this.bundlePage < this.maxBundlePage()
                    || this.currentBundlePageIsFull();
        }
    }

    private int centeredButtonRowX() {
        int rowWidth = (2 * BUTTON_WIDTH) + BUTTON_GAP;
        return this.leftPos + (this.imageWidth - rowWidth) / 2;
    }

    private int sideButtonWidth() {
        int rightSpace = this.width - (this.leftPos + this.imageWidth);
        int available = Math.min(this.leftPos, rightSpace) - BUTTON_GAP - 4;
        return Math.clamp(available, 1, BUTTON_WIDTH);
    }

    private int maxBundlePage() {
        return Math.max(0, this.special.bundleEntries.size() / ContainerEntryDraftUtil.BUNDLE_PAGE_SIZE);
    }

    private boolean currentBundlePageIsFull() {
        for (int slot = 0; slot < this.editableSlots; slot++) {
            if (this.container.getItem(slot).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private void loadBundlePage() {
        for (int slot = 0; slot < this.editableSlots; slot++) {
            this.container.setItem(slot, ItemStack.EMPTY);
        }
        int start = this.bundlePage * ContainerEntryDraftUtil.BUNDLE_PAGE_SIZE;
        int end = Math.min(this.special.bundleEntries.size(), start + this.editableSlots);
        for (int index = start; index < end; index++) {
            this.container.setItem(index - start, stackForDraft(this.special.bundleEntries.get(index)));
        }
    }

    private void writeBundlePageBack() {
        int start = this.bundlePage * ContainerEntryDraftUtil.BUNDLE_PAGE_SIZE;
        int end = Math.min(this.special.bundleEntries.size(), start + this.editableSlots);
        List<ItemEditorState.ContainerEntryDraft> updated = new ArrayList<>(
                this.special.bundleEntries.subList(0, Math.min(start, this.special.bundleEntries.size()))
        );
        for (int slot = 0; slot < this.editableSlots; slot++) {
            ItemStack stack = this.container.getItem(slot);
            if (!stack.isEmpty()) {
                updated.add(ItemEditorState.ContainerEntryDraft.fromSlot(updated.size(), stack.copy()));
            }
        }
        if (end < this.special.bundleEntries.size()) {
            updated.addAll(this.special.bundleEntries.subList(end, this.special.bundleEntries.size()));
        }
        this.special.bundleEntries.clear();
        this.special.bundleEntries.addAll(updated);
        syncBundleSlots(this.special.bundleEntries);
        if (this.special.bundleEntries.isEmpty()) {
            this.special.selectedBundleIndex = -1;
            this.bundlePage = 0;
        } else {
            this.special.selectedBundleIndex = Math.clamp(this.special.selectedBundleIndex, 0, this.special.bundleEntries.size() - 1);
            this.bundlePage = Math.clamp(this.bundlePage, 0, this.maxBundlePage());
        }
        this.special.bundleEditorPage = this.bundlePage;
    }

    private boolean allowedClick(int slotId, ClickType type) {
        if (type == ClickType.QUICK_CRAFT) {
            return slotId == -999 || isMenuSlot(slotId);
        }
        return isMenuSlot(slotId)
                && type != ClickType.SWAP
                && type != ClickType.THROW;
    }

    private boolean isMenuSlot(int slotId) {
        return slotId >= 0 && slotId < this.menu.slots.size();
    }

    private static ScreenData screenData(
            ItemEditorState.SpecialData special,
            ItemStack originalStack
    ) {
        int rows = rowsFor(originalStack);
        SimpleContainer container = new SimpleContainer(rows * COLUMNS);
        for (ItemEditorState.ContainerEntryDraft draft : special.containerEntries) {
            Integer slot = parseSlot(draft.slot);
            if (slot != null && slot >= 0 && slot < container.getContainerSize()) {
                container.setItem(slot, stackForDraft(draft));
            }
        }
        return new ScreenData(container, rows, ItemEditorText.tr("special.container.editor_title"), 0);
    }

    private static ScreenData bundleScreenData(ItemEditorState.SpecialData special, int page) {
        SimpleContainer container = new SimpleContainer(ContainerEntryDraftUtil.BUNDLE_PAGE_SIZE);
        int safePage = Math.clamp(
                page,
                0,
                Math.max(0, special.bundleEntries.size() / ContainerEntryDraftUtil.BUNDLE_PAGE_SIZE)
        );
        int start = safePage * ContainerEntryDraftUtil.BUNDLE_PAGE_SIZE;
        int end = Math.min(special.bundleEntries.size(), start + ContainerEntryDraftUtil.BUNDLE_PAGE_SIZE);
        for (int index = start; index < end; index++) {
            container.setItem(index - start, stackForDraft(special.bundleEntries.get(index)));
        }
        return new ScreenData(container, 6, ItemEditorText.tr("special.bundle.editor_title"), safePage);
    }

    private static ItemStack stackForDraft(ItemEditorState.ContainerEntryDraft draft) {
        if (draft.templateStack != null && !draft.templateStack.isEmpty()) {
            int count = Math.max(1, ContainerEntryDraftUtil.parseIntOrDefault(
                    draft.count,
                    draft.templateStack.getCount()
            ));
            return draft.templateStack.copyWithCount(count);
        }
        Item item = ContainerEntryDraftUtil.resolveItem(draft.itemId);
        if (item == null || item == Items.AIR) {
            return ItemStack.EMPTY;
        }
        int count = Math.max(1, ContainerEntryDraftUtil.parseIntOrDefault(draft.count, 1));
        return new ItemStack(item, count);
    }

    private static int rowsFor(ItemStack stack) {
        int slots = slotsFor(stack);
        return Math.clamp((int) Math.ceil(slots / (double) COLUMNS), 1, 6);
    }

    private static int slotsFor(ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem blockItem)) {
            return 27;
        }
        if (blockItem.getBlock() instanceof HopperBlock) {
            return 9;
        }
        if (blockItem.getBlock() instanceof DispenserBlock) {
            return 9;
        }
        return 27;
    }

    private static MenuType<?> menuType(int rows) {
        return switch (rows) {
            case 1 -> MenuType.GENERIC_9x1;
            case 2 -> MenuType.GENERIC_9x2;
            case 3 -> MenuType.GENERIC_9x3;
            case 4 -> MenuType.GENERIC_9x4;
            case 5 -> MenuType.GENERIC_9x5;
            default -> MenuType.GENERIC_9x6;
        };
    }

    private static int slotOrMax(ItemEditorState.ContainerEntryDraft draft) {
        Integer slot = parseSlot(draft.slot);
        return slot == null ? Integer.MAX_VALUE : slot;
    }

    private static Integer parseSlot(String rawSlot) {
        int parsed = ContainerEntryDraftUtil.parseIntOrDefault(rawSlot, Integer.MIN_VALUE);
        return parsed == Integer.MIN_VALUE ? null : parsed;
    }

    private static void syncBundleSlots(List<ItemEditorState.ContainerEntryDraft> entries) {
        for (int index = 0; index < entries.size(); index++) {
            entries.get(index).slot = Integer.toString(index);
        }
    }

    private static Inventory requireInventory() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            throw new IllegalStateException("Player is required to open container editor");
        }
        return minecraft.player.getInventory();
    }

    private enum EditorMode {
        CONTAINER,
        BUNDLE
    }

    private record ScreenData(SimpleContainer container, int rows, Component title, int bundlePage) {
    }
}
