package me.noramibu.itemeditor.ui.screen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import me.noramibu.itemeditor.service.FileImportService;
import me.noramibu.itemeditor.service.ItemImportService;
import me.noramibu.itemeditor.storage.SavedItemStorageService.ExternalItemImport;
import me.noramibu.itemeditor.storage.SavedItemStorageService.ExternalPageImport;
import me.noramibu.itemeditor.ui.component.UiFactory;
import me.noramibu.itemeditor.util.ItemEditorText;
import me.noramibu.itemeditor.util.TextComponentUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

final class ImportPreviewScreen extends ContainerScreen {
    private final ImportScreen owner;
    private final FileImportService.Preview source;
    private final SimpleContainer container;
    private final List<ItemStack> items;
    private final List<Button> controls = new ArrayList<>();
    private int page;
    private Button previous;
    private Button next;
    private Button confirm;

    ImportPreviewScreen(ImportScreen owner, FileImportService.Preview preview) {
        this(owner, preview, new SimpleContainer(54));
    }

    private ImportPreviewScreen(ImportScreen owner, FileImportService.Preview preview, SimpleContainer container) {
        super(
                new ChestMenu(
                        UiFactory.chestMenuType(6),
                        0,
                        Minecraft.getInstance().player.getInventory(),
                        container,
                        6),
                Minecraft.getInstance().player.getInventory(),
                preview.page() == null || preview.page().name().isBlank()
                        ? ItemEditorText.tr("imported_items.title")
                        : Component.literal(
                                TextComponentUtil.parseMarkup(preview.page().name())
                                        .getString()));
        this.owner = owner;
        this.source = preview;
        SimpleContainer excluded = new SimpleContainer(36);
        for (int i = 54; i < this.menu.slots.size(); i++) {
            Slot original = this.menu.slots.get(i);
            Slot replacement = new Slot(excluded, i - 54, original.x, original.y);
            replacement.index = i;
            this.menu.slots.set(i, replacement);
        }
        this.container = container;
        this.items = new ArrayList<>(slots(preview));
        while (this.items.size() < 54 || this.items.size() % 54 != 0) this.items.add(ItemStack.EMPTY);
        this.fillPage();
    }

    static List<ItemStack> slots(FileImportService.Preview preview) {
        if (preview.page() == null)
            return preview.items().stacks().stream().map(ItemStack::copy).toList();
        List<ItemStack> slots = new ArrayList<>(Collections.nCopies(54, ItemStack.EMPTY));
        for (var item : preview.page().items())
            slots.set(item.slotInPage(), item.stack().copy());
        return slots;
    }

    @Override
    protected void init() {
        super.init();
        this.controls.clear();
        int width = Math.clamp((this.width - this.imageWidth) / 2 - 8, 20, 100);
        int left = Math.max(2, this.leftPos - width - 4);
        int right = Math.min(this.width - width - 2, this.leftPos + this.imageWidth + 4);
        int y = this.topPos + 35;
        this.previous = this.button("common.prev", "<", left, y, width, () -> this.changePage(-1));
        this.next = this.button("common.next", ">", right, y, width, () -> this.changePage(1));
        this.button("common.cancel", "X", left, y + 24, width, this::onClose);
        this.confirm = this.button(
                this.source.page() == null ? "import.confirm_items" : "import.confirm_page",
                "OK",
                right,
                y + 24,
                width,
                this::confirmImport);
        this.updateControls();
    }

    private Button button(String key, String compact, int x, int y, int width, Runnable action) {
        Component label = ItemEditorText.tr(key);
        Button button = Button.builder(
                        this.font.width(label) <= width - 8 ? label : Component.literal(compact),
                        ignored -> action.run())
                .bounds(x, y, width, 20)
                .build();
        button.setTooltip(Tooltip.create(label));
        this.controls.add(button);
        return this.addRenderableWidget(button);
    }

    @Override
    public void containerTick() {
        super.containerTick();
        this.updateControls();
    }

    private void updateControls() {
        for (Button button : this.controls) button.active = !this.owner.importBusy();
        this.previous.active &= this.page > 0;
        this.next.active &= (this.page + 1) * 54 < this.items.size();
        this.confirm.active &= this.menu.getCarried().isEmpty();
        this.tooltip(
                this.confirm,
                this.source.page() == null ? "import.confirm_items" : "import.confirm_page",
                this.owner.importStatus());
        Component count = ItemEditorText.tr(
                "imported_items.page",
                this.page + 1,
                this.items.size() / 54,
                this.items.stream().filter(item -> !item.isEmpty()).count());
        this.tooltip(this.previous, "common.prev", count);
        this.tooltip(this.next, "common.next", count);
    }

    private void tooltip(Button button, String key, Component details) {
        button.setTooltip(
                Tooltip.create(ItemEditorText.tr(key).copy().append("\n").append(details)));
    }

    private void changePage(int delta) {
        this.savePage();
        this.page = Math.clamp(this.page + delta, 0, Math.max(0, (this.items.size() - 1) / 54));
        this.fillPage();
        this.updateControls();
    }

    private void fillPage() {
        for (int slot = 0; slot < 54; slot++) {
            int index = this.page * 54 + slot;
            this.container.setItem(
                    slot, index < this.items.size() ? this.items.get(index).copy() : ItemStack.EMPTY);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(
                this.font, this.font.plainSubstrByWidth(this.title.getString(), 160), 8, 6, 0xFF404040, false);
        graphics.drawString(
                this.font,
                this.font.plainSubstrByWidth(ItemEditorText.str("import.excluded_items"), 160),
                8,
                this.inventoryLabelY,
                0xFF404040,
                false);
        if (!this.owner.importStatus().getString().isBlank()) {
            int wrapWidth = Math.min(360, this.width - 16);
            int x = Math.clamp(this.leftPos + (this.imageWidth - wrapWidth) / 2, 8, this.width - wrapWidth - 8);
            var lines = this.font.split(this.owner.importStatus(), wrapWidth);
            int lineHeight = this.font.lineHeight + 2;
            int y = this.topPos - lines.size() * lineHeight - 6;
            int availableHeight = this.topPos - 12;
            if (y < 6) {
                x = this.leftPos + this.imageWidth + 8;
                wrapWidth = this.width - x - 8;
                if (wrapWidth < 60) {
                    x = 8;
                    wrapWidth = this.leftPos - 16;
                }
                if (wrapWidth < 60) return;
                lines = this.font.split(this.owner.importStatus(), wrapWidth);
                y = this.topPos + 90;
                availableHeight = this.height - y - 8;
            }
            int visibleLines = Math.clamp(availableHeight / lineHeight, 0, lines.size());
            for (int i = 0; i < visibleLines; i++) {
                if (i == visibleLines - 1 && visibleLines < lines.size()) {
                    graphics.drawString(this.font, "...", x - this.leftPos, y - this.topPos, 0xFFFFFFFF, true);
                } else {
                    graphics.drawString(this.font, lines.get(i), x - this.leftPos, y - this.topPos, 0xFFFFFFFF, true);
                }
                y += lineHeight;
            }
        }
    }

    @Override
    protected void slotClicked(Slot slot, int slotId, int button, ClickType type) {
        if (this.owner.importBusy() || this.minecraft.player == null || type == ClickType.SWAP) return;
        if (slot != null) slotId = slot.index;
        if (type == ClickType.THROW) {
            if (slot != null && slot.hasItem())
                slot.remove(button == 0 ? 1 : slot.getItem().getCount());
        } else if (slotId >= 0 || type == ClickType.QUICK_CRAFT) {
            this.menu.clicked(slotId, button, type, this.minecraft.player);
        }
        this.savePage();
        this.menu.broadcastChanges();
    }

    private void savePage() {
        for (int i = 0; i < 54; i++)
            this.items.set(this.page * 54 + i, this.container.getItem(i).copy());
    }

    private void confirmImport() {
        if (!this.menu.getCarried().isEmpty()) return;
        this.savePage();
        FileImportService.Preview edited;
        if (this.source.page() != null) {
            List<ExternalItemImport> entries = new ArrayList<>();
            for (int i = 0; i < this.items.size(); i++) {
                ItemStack stack = this.items.get(i);
                if (stack.isEmpty()) continue;
                var original = this.source.page().items().stream()
                        .filter(item -> ItemStack.matches(item.stack(), stack))
                        .findFirst();
                entries.add(new ExternalItemImport(
                        i,
                        stack.copy(),
                        original.map(ExternalItemImport::itemTag)
                                .map(CompoundTag::copy)
                                .orElse(null),
                        original.map(ExternalItemImport::dataVersion).orElse(0)));
            }
            edited = new FileImportService.Preview(
                    new ExternalPageImport(this.source.page().name(), entries), null);
        } else {
            List<ItemStack> remaining =
                    this.items.stream().filter(item -> !item.isEmpty()).toList();
            if (remaining.isEmpty()) return;
            var result = remaining.size() == 1
                    ? ItemImportService.ImportResult.success(remaining.getFirst(), "")
                    : ItemImportService.ImportResult.successMany(remaining, "");
            edited = new FileImportService.Preview(null, result);
        }
        this.owner.confirmPreview(edited);
    }

    @Override
    public void onClose() {
        if (!this.owner.importBusy()) this.owner.cancelPreview();
    }
}
