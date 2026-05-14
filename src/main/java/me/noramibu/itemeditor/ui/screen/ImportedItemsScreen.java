package me.noramibu.itemeditor.ui.screen;

import me.noramibu.itemeditor.editor.ItemEditorSession;
import me.noramibu.itemeditor.util.ItemEditorText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public final class ImportedItemsScreen extends ContainerScreen {
    private static final int COLUMNS = 9;
    private static final int MAX_ROWS = 6;
    private static final int BUTTON_WIDTH = 52;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_GAP = 4;

    private final Minecraft minecraft;
    private final Screen returnScreen;
    private final List<ItemStack> importedItems;
    private final SimpleContainer container;
    private final int pageSize;
    private int page;

    public ImportedItemsScreen(Minecraft minecraft, Screen returnScreen, List<ItemStack> importedItems) {
        this(minecraft, returnScreen, screenData(importedItems));
    }

    private ImportedItemsScreen(Minecraft minecraft, Screen returnScreen, ImportedItemsData data) {
        this(
                minecraft,
                returnScreen,
                data.items(),
                data.rows()
        );
    }

    private ImportedItemsScreen(Minecraft minecraft, Screen returnScreen, List<ItemStack> importedItems, int rows) {
        this(minecraft, returnScreen, importedItems, rows, new SimpleContainer(rows * COLUMNS), requireInventory());
    }

    private ImportedItemsScreen(
            Minecraft minecraft,
            Screen returnScreen,
            List<ItemStack> importedItems,
            int rows,
            SimpleContainer container,
            Inventory inventory
    ) {
        super(new ChestMenu(menuType(rows), 0, inventory, container, rows), inventory, ItemEditorText.tr("imported_items.title"));
        this.minecraft = minecraft;
        this.returnScreen = returnScreen;
        this.importedItems = importedItems;
        this.pageSize = rows * COLUMNS;
        this.page = 0;
        this.container = container;
        this.fillPage();
    }

    @Override
    protected void init() {
        super.init();
        if (this.maxPage() <= 0) {
            return;
        }
        int y = Math.max(4, this.topPos - BUTTON_HEIGHT - BUTTON_GAP);
        int nextX = this.leftPos + this.imageWidth - BUTTON_WIDTH;
        int prevX = nextX - BUTTON_WIDTH - BUTTON_GAP;
        this.addRenderableWidget(Button.builder(ItemEditorText.tr("common.prev"), button -> this.changePage(-1))
                .bounds(prevX, y, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
        this.addRenderableWidget(Button.builder(ItemEditorText.tr("common.next"), button -> this.changePage(1))
                .bounds(nextX, y, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
    }

    @Override
    public void renderBackground(@NotNull GuiGraphics context, int mouseX, int mouseY, float delta) {
        super.renderBackground(context, mouseX, mouseY, delta);
        context.drawString(
                this.font,
                Component.literal(ItemEditorText.str("imported_items.page", this.page + 1, this.maxPage() + 1, this.importedItems.size())),
                this.leftPos + 8,
                this.topPos + 6,
                0xD5DEE8,
                false
        );
    }

    @Override
    protected void slotClicked(@Nullable Slot slot, int slotId, int button, @NotNull ClickType input) {
        if (button == 0 && slot != null && slotId >= 0 && slotId < this.pageSize && slot.hasItem()) {
            this.openEditor(slot.getItem());
        }
    }

    @Override
    public boolean keyPressed(@NotNull KeyEvent input) {
        if (input.key() == GLFW.GLFW_KEY_I) {
            Slot hovered = this.hoveredSlot;
            int slotId = hovered == null ? -1 : this.menu.slots.indexOf(hovered);
            if (slotId >= 0 && slotId < this.pageSize && hovered.hasItem()) {
                this.openEditor(hovered.getItem());
                return true;
            }
        }
        return super.keyPressed(input);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.returnScreen);
    }

    private void openEditor(ItemStack stack) {
        this.minecraft.setScreen(new ItemEditorScreen(new ItemEditorSession(this.minecraft, stack.copy())));
    }

    private void changePage(int delta) {
        this.page = Math.clamp(this.page + delta, 0, this.maxPage());
        this.fillPage();
    }

    private void fillPage() {
        for (int slot = 0; slot < this.pageSize; slot++) {
            int itemIndex = this.page * this.pageSize + slot;
            this.container.setItem(slot, itemIndex < this.importedItems.size() ? this.importedItems.get(itemIndex).copy() : ItemStack.EMPTY);
        }
        this.container.setChanged();
        this.menu.broadcastChanges();
    }

    private static List<ItemStack> sanitizeImportedItems(List<ItemStack> importedItems) {
        if (importedItems == null) {
            return List.of();
        }
        return importedItems.stream()
                .filter(stack -> stack != null && !stack.isEmpty())
                .map(ItemStack::copy)
                .toList();
    }

    private static ImportedItemsData screenData(List<ItemStack> importedItems) {
        List<ItemStack> sanitized = sanitizeImportedItems(importedItems);
        return new ImportedItemsData(sanitized, rowsFor(sanitized.size()));
    }

    private int maxPage() {
        return Math.max(0, (int) Math.ceil(this.importedItems.size() / (double) this.pageSize) - 1);
    }

    private static int rowsFor(int itemCount) {
        return Math.clamp((int) Math.ceil(Math.max(1, itemCount) / (double) COLUMNS), 1, MAX_ROWS);
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

    private static Inventory requireInventory() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            throw new IllegalStateException("Player is required to open imported items screen");
        }
        return minecraft.player.getInventory();
    }

    private record ImportedItemsData(List<ItemStack> items, int rows) {
    }
}
