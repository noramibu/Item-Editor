package me.noramibu.itemeditor.ui.screen;

import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.base.BaseUIComponent;
import io.wispforest.owo.ui.component.TextBoxComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.StackLayout;
import io.wispforest.owo.ui.container.UIContainers;
import io.wispforest.owo.ui.core.HorizontalAlignment;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.OwoUIAdapter;
import io.wispforest.owo.ui.core.OwoUIGraphics;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.Surface;
import io.wispforest.owo.ui.core.VerticalAlignment;
import me.noramibu.itemeditor.editor.ItemEditorSession;
import me.noramibu.itemeditor.ui.component.UiFactory;
import me.noramibu.itemeditor.util.ItemEditorText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public final class ItemPickerScreen extends BaseOwoScreen<StackLayout> {
    private static final int ROOT_BLUR_RADIUS = 4;
    private static final int ROOT_BLUR_QUALITY = 8;
    private static final int ROOT_SURFACE_TINT = 0x6610151A;
    private static final int SHELL_MAX_WIDTH = 980;
    private static final int CELL_SIZE = 30;
    private static final int SCROLLBAR_THICKNESS = 8;
    private static final int GRID_SIDE_PADDING = 8;
    private static final int GRID_SCROLL_STEP_ROWS = 3;
    private static final int MAX_VISIBLE_ITEM_RENDERS = 420;

    private final Minecraft minecraft;
    private final Screen returnScreen;
    private final List<PickableItem> vanillaItems;
    private final List<PickableItem> allItems;

    private VirtualItemGridComponent itemGrid;
    private TextBoxComponent searchBox;
    private String currentQuery = "";
    private boolean includeModded;

    public ItemPickerScreen(Minecraft minecraft, Screen returnScreen) {
        super(ItemEditorText.tr("item_picker.title"));
        this.minecraft = minecraft;
        this.returnScreen = returnScreen;
        List<PickableItem> vanilla = new ArrayList<>();
        List<PickableItem> modded = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (item == Items.AIR) {
                continue;
            }
            Identifier id = BuiltInRegistries.ITEM.getKey(item);
            ItemStack stack = new ItemStack(item);
            Component displayName = stack.getHoverName();
            PickableItem pickable = new PickableItem(id, item, stack, displayName, buildSearchIndex(id, displayName));
            if ("minecraft".equals(id.getNamespace())) {
                vanilla.add(pickable);
            } else {
                modded.add(pickable);
            }
        }
        Comparator<PickableItem> comparator = Comparator
                .comparing((PickableItem item) -> item.displayName().getString().toLowerCase(Locale.ROOT))
                .thenComparing(item -> item.id().toString());
        vanilla.sort(comparator);
        modded.sort(comparator);
        List<PickableItem> all = new ArrayList<>(vanilla.size() + modded.size());
        all.addAll(vanilla);
        all.addAll(modded);
        all.sort(comparator);
        this.vanillaItems = List.copyOf(vanilla);
        this.allItems = List.copyOf(all);
    }

    @Override
    protected @NotNull OwoUIAdapter<StackLayout> createAdapter() {
        return OwoUIAdapter.create(this, UIContainers::stack);
    }

    @Override
    protected void build(StackLayout root) {
        root.clearChildren();
        root.surface(Surface.blur(ROOT_BLUR_RADIUS, ROOT_BLUR_QUALITY).and(Surface.flat(ROOT_SURFACE_TINT)));

        FlowLayout shell = UiFactory.card();
        shell.horizontalSizing(Sizing.fixed(Math.min(SHELL_MAX_WIDTH, Math.max(260, this.width - 24))));
        shell.verticalSizing(Sizing.fixed(Math.max(180, this.height - 24)));
        shell.gap(UiFactory.scaledPixels(5));

        FlowLayout header = UiFactory.row();
        header.child(UiFactory.title(ItemEditorText.tr("item_picker.title")).horizontalSizing(Sizing.expand(100)));
        header.child(UiFactory.button(ItemEditorText.tr("item_picker.back"), UiFactory.ButtonTextPreset.STANDARD, button -> this.minecraft.setScreen(this.returnScreen)));
        shell.child(header);

        FlowLayout controls = UiFactory.row();
        this.searchBox = UiFactory.textBox(this.currentQuery, value -> {
            this.currentQuery = value == null ? "" : value;
            this.refreshGrid();
        });
        this.searchBox.setHint(ItemEditorText.tr("item_picker.search"));
        controls.child(this.searchBox.horizontalSizing(Sizing.expand(100)));
        controls.child(UiFactory.checkbox(ItemEditorText.tr("item_picker.include_modded"), this.includeModded, checked -> {
            this.includeModded = checked;
            this.refreshGrid();
        }));
        shell.child(controls);

        this.itemGrid = new VirtualItemGridComponent(pickable ->
                this.minecraft.setScreen(new ItemEditorScreen(new ItemEditorSession(this.minecraft, new ItemStack(pickable.item()))))
        );
        this.itemGrid.horizontalSizing(Sizing.fill(100));
        this.itemGrid.verticalSizing(Sizing.expand(100));
        shell.child(this.itemGrid);
        this.refreshGrid();

        FlowLayout centered = UiFactory.column();
        centered.horizontalSizing(Sizing.fill(100));
        centered.verticalSizing(Sizing.fill(100));
        centered.padding(Insets.of(8));
        centered.horizontalAlignment(HorizontalAlignment.CENTER);
        centered.verticalAlignment(VerticalAlignment.CENTER);
        centered.child(shell);
        root.child(centered);
    }

    private void refreshGrid() {
        if (this.itemGrid == null) {
            return;
        }
        this.itemGrid.items(this.filteredItems());
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
            return true;
        }
        if (this.itemGrid != null && this.itemGrid.acceptsBodyScroll(mouseX, mouseY)) {
            this.itemGrid.scrollBy(scrollY);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubleClick) {
        if (click.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT && this.itemGrid != null) {
            PickableItem pickable = this.itemGrid.itemAt(click.x(), click.y());
            if (pickable != null) {
                this.itemGrid.pick(pickable);
                return true;
            }
        }
        return super.mouseClicked(click, doubleClick);
    }

    private List<PickableItem> filteredItems() {
        List<PickableItem> source = this.includeModded ? this.allItems : this.vanillaItems;
        List<String> terms = searchTerms(this.searchBox == null ? this.currentQuery : this.searchBox.getValue());
        if (terms.isEmpty()) {
            return source;
        }
        return source.stream()
                .filter(item -> terms.stream().allMatch(term -> item.searchIndex().contains(term)))
                .toList();
    }

    private static String buildSearchIndex(Identifier id, Component displayName) {
        String path = id.getPath();
        return normalizeSearch(String.join(
                " ",
                displayName.getString(),
                id.toString(),
                id.getNamespace(),
                path,
                path.replace('_', ' '),
                path.replace('-', ' ')
        ));
    }

    private static List<String> searchTerms(String rawQuery) {
        String normalized = normalizeSearch(rawQuery);
        if (normalized.isBlank()) {
            return List.of();
        }
        return List.of(normalized.split(" "));
    }

    private static String normalizeSearch(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String lower = raw.toLowerCase(Locale.ROOT);
        StringBuilder normalized = new StringBuilder(lower.length());
        boolean lastWasSpace = true;
        for (int offset = 0; offset < lower.length(); ) {
            int codePoint = lower.codePointAt(offset);
            offset += Character.charCount(codePoint);
            if (Character.isLetterOrDigit(codePoint)) {
                normalized.appendCodePoint(codePoint);
                lastWasSpace = false;
            } else if (!lastWasSpace) {
                normalized.append(' ');
                lastWasSpace = true;
            }
        }
        int length = normalized.length();
        if (length > 0 && normalized.charAt(length - 1) == ' ') {
            normalized.setLength(length - 1);
        }
        return normalized.toString();
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.returnScreen);
    }

    private record PickableItem(Identifier id, Item item, ItemStack previewStack, Component displayName, String searchIndex) {
    }

    private final class VirtualItemGridComponent extends BaseUIComponent {
        private static final int COLOR_CELL_HOVER = 0xCC243142;
        private static final int COLOR_CELL_BORDER = 0xFF414B56;
        private static final int COLOR_SCROLL_TRACK = 0x660A0F18;
        private static final int COLOR_SCROLL_THUMB = 0xFFC8D0DC;
        private static final int COLOR_SCROLL_THUMB_HOVER = 0xFFE4E9F1;

        private final Consumer<PickableItem> onPick;
        private List<PickableItem> items = List.of();
        private double scrollAmount;
        private boolean draggingScrollbar;
        private double scrollbarDragOffset;

        private VirtualItemGridComponent(Consumer<PickableItem> onPick) {
            this.onPick = onPick;
            this.cursorStyle(io.wispforest.owo.ui.core.CursorStyle.HAND);
        }

        private void items(List<PickableItem> items) {
            this.items = items == null ? List.of() : List.copyOf(items);
            this.scrollAmount = 0d;
        }

        @Override
        public void draw(OwoUIGraphics context, int mouseX, int mouseY, float partialTicks, float delta) {
            GridMetrics metrics = this.gridMetrics();
            int cell = metrics.cellSize();
            int columns = metrics.columns();
            int rows = this.rows(columns);
            int contentHeight = rows * cell;
            this.clampScroll();

            context.fill(this.x, this.y, this.x + this.width, this.y + this.height, 0x44000000);
            if (this.items.isEmpty()) {
                context.text(Minecraft.getInstance().font, ItemEditorText.tr("item_picker.none"), this.x + 6, this.y + 6, 0xA9B5C0);
                return;
            }

            int firstRow = Math.max(0, (int) Math.floor(this.scrollAmount / cell));
            int lastRow = Math.min(rows - 1, (int) Math.ceil((this.scrollAmount + this.height) / cell));
            context.enableScissor(this.x, this.y, this.x + metrics.availableWidth(), this.y + this.height);
            for (int row = firstRow; row <= lastRow; row++) {
                int y = this.y + (row * cell) - (int) Math.round(this.scrollAmount);
                for (int column = 0; column < columns; column++) {
                    int itemIndex = row * columns + column;
                    if (itemIndex < 0 || itemIndex >= this.items.size()) {
                        continue;
                    }
                    int x = metrics.startX() + (column * cell);
                    boolean hovered = mouseX >= x && mouseX < x + cell && mouseY >= y && mouseY < y + cell;
                    if (hovered) {
                        context.fill(x + 1, y + 1, x + cell - 1, y + cell - 1, COLOR_CELL_HOVER);
                        context.outline(x, y, cell, cell, COLOR_CELL_BORDER);
                    }
                    PickableItem pickable = this.items.get(itemIndex);
                    int iconX = x + Math.max(0, (cell - 16) / 2);
                    int iconY = y + Math.max(0, (cell - 16) / 2);
                    context.item(pickable.previewStack(), iconX, iconY);
                    if (hovered) {
                        context.setComponentTooltipForNextFrame(
                                Minecraft.getInstance().font,
                                List.of(
                                        pickable.displayName(),
                                        Component.literal(ItemEditorText.str("item_picker.tooltip.registry_id", pickable.id().toString()))
                                ),
                                mouseX,
                                mouseY
                        );
                    }
                }
            }
            context.disableScissor();
            this.drawScrollbar(context, contentHeight, mouseX, mouseY);
        }

        @Override
        public boolean onMouseDown(MouseButtonEvent click, boolean doubled) {
            if (click.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT || !this.isInBoundingBox(click.x(), click.y())) {
                return super.onMouseDown(click, doubled);
            }
            if (this.isOverScrollbar(click.x(), click.y())) {
                ScrollbarGeometry geometry = this.scrollbarGeometry();
                this.draggingScrollbar = true;
                this.scrollbarDragOffset = click.y() - geometry.thumbY();
                return true;
            }
            PickableItem pickable = this.itemAt(click.x(), click.y());
            if (pickable != null) {
                this.pick(pickable);
                return true;
            }
            return true;
        }

        @Override
        public boolean onMouseUp(MouseButtonEvent click) {
            this.draggingScrollbar = false;
            return super.onMouseUp(click);
        }

        @Override
        public boolean onMouseDrag(MouseButtonEvent click, double deltaX, double deltaY) {
            if (!this.draggingScrollbar) {
                return super.onMouseDrag(click, deltaX, deltaY);
            }
            ScrollbarGeometry geometry = this.scrollbarGeometry();
            double trackRange = Math.max(1d, geometry.trackHeight() - geometry.thumbHeight());
            double thumbY = Math.clamp(click.y() - this.scrollbarDragOffset, geometry.trackY(), geometry.trackY() + trackRange);
            double scrollRange = Math.max(0d, this.contentHeight() - this.height);
            this.scrollAmount = ((thumbY - geometry.trackY()) / trackRange) * scrollRange;
            this.clampScroll();
            return true;
        }

        @Override
        public boolean onMouseScroll(double mouseX, double mouseY, double amount) {
            if (!this.isInBoundingBox(mouseX, mouseY)) {
                return super.onMouseScroll(mouseX, mouseY, amount);
            }
            this.scrollBy(amount);
            return true;
        }

        private boolean acceptsBodyScroll(double mouseX, double mouseY) {
            return mouseX >= 0
                    && mouseX < ItemPickerScreen.this.width
                    && mouseY >= this.y
                    && mouseY < this.y + this.height;
        }

        private void scrollBy(double amount) {
            this.scrollAmount -= amount * this.cellSize() * GRID_SCROLL_STEP_ROWS;
            this.clampScroll();
        }

        private void pick(PickableItem pickable) {
            this.onPick.accept(pickable);
        }

        private PickableItem itemAt(double mouseX, double mouseY) {
            GridMetrics metrics = this.gridMetrics();
            int usedWidth = metrics.columns() * metrics.cellSize();
            if (mouseX < metrics.startX() || mouseX >= metrics.startX() + usedWidth || mouseY < this.y || mouseY >= this.y + this.height) {
                return null;
            }
            int cell = metrics.cellSize();
            int column = (int) ((mouseX - metrics.startX()) / cell);
            int row = (int) ((mouseY - this.y + this.scrollAmount) / cell);
            int index = row * metrics.columns() + column;
            if (index < 0 || index >= this.items.size()) {
                return null;
            }
            return this.items.get(index);
        }

        private void drawScrollbar(OwoUIGraphics context, int contentHeight, int mouseX, int mouseY) {
            if (contentHeight <= this.height) {
                return;
            }
            ScrollbarGeometry geometry = this.scrollbarGeometry();
            boolean hovered = mouseX >= geometry.trackX()
                    && mouseX < geometry.trackX() + geometry.trackWidth()
                    && mouseY >= geometry.thumbY()
                    && mouseY < geometry.thumbY() + geometry.thumbHeight();
            context.fill(
                    geometry.trackX(),
                    geometry.trackY(),
                    geometry.trackX() + geometry.trackWidth(),
                    geometry.trackY() + geometry.trackHeight(),
                    COLOR_SCROLL_TRACK
            );
            context.fill(
                    geometry.trackX(),
                    geometry.thumbY(),
                    geometry.trackX() + geometry.trackWidth(),
                    geometry.thumbY() + geometry.thumbHeight(),
                    hovered || this.draggingScrollbar ? COLOR_SCROLL_THUMB_HOVER : COLOR_SCROLL_THUMB
            );
        }

        private boolean isOverScrollbar(double mouseX, double mouseY) {
            if (this.contentHeight() <= this.height) {
                return false;
            }
            ScrollbarGeometry geometry = this.scrollbarGeometry();
            return mouseX >= geometry.trackX()
                    && mouseX < geometry.trackX() + geometry.trackWidth()
                    && mouseY >= geometry.trackY()
                    && mouseY < geometry.trackY() + geometry.trackHeight();
        }

        private ScrollbarGeometry scrollbarGeometry() {
            int thickness = UiFactory.scaledScrollbarThickness(SCROLLBAR_THICKNESS);
            int trackX = this.x + this.width - thickness;
            int trackY = this.y;
            int trackHeight = this.height;
            int contentHeight = this.contentHeight();
            int thumbHeight = Math.max(18, (int) Math.round((this.height / (double) Math.max(1, contentHeight)) * trackHeight));
            thumbHeight = Math.min(trackHeight, thumbHeight);
            int scrollRange = Math.max(1, contentHeight - this.height);
            int thumbRange = Math.max(0, trackHeight - thumbHeight);
            int thumbY = trackY + (int) Math.round((this.scrollAmount / scrollRange) * thumbRange);
            return new ScrollbarGeometry(trackX, trackY, thickness, trackHeight, thumbY, thumbHeight);
        }

        private int contentHeight() {
            GridMetrics metrics = this.gridMetrics();
            return this.rows(metrics.columns()) * metrics.cellSize();
        }

        private int rows(int columns) {
            return Math.max(1, (int) Math.ceil(this.items.size() / (double) Math.max(1, columns)));
        }

        private GridMetrics gridMetrics() {
            int cell = this.cellSize();
            int sidePadding = UiFactory.scaledPixels(GRID_SIDE_PADDING);
            int availableWidth = Math.max(cell, this.width - UiFactory.scaledScrollbarThickness(SCROLLBAR_THICKNESS));
            int columnAreaWidth = Math.max(cell, availableWidth - (sidePadding * 2));
            int columns = Math.max(1, Math.min(Math.max(1, this.items.size()), columnAreaWidth / Math.max(1, cell)));
            int usedWidth = columns * cell;
            int balancedPadding = Math.max(0, (availableWidth - usedWidth) / 2);
            int startX = this.x + balancedPadding;
            return new GridMetrics(startX, availableWidth, cell, columns);
        }

        private int cellSize() {
            int base = UiFactory.scaledPixels(CELL_SIZE);
            int sidePadding = UiFactory.scaledPixels(GRID_SIDE_PADDING);
            int gridWidth = Math.max(1, this.width - UiFactory.scaledScrollbarThickness(SCROLLBAR_THICKNESS) - (sidePadding * 2));
            int viewportArea = Math.max(1, gridWidth * Math.max(1, this.height));
            int adaptive = (int) Math.ceil(Math.sqrt(viewportArea / (double) MAX_VISIBLE_ITEM_RENDERS));
            return Math.max(base, adaptive);
        }

        private void clampScroll() {
            this.scrollAmount = Math.clamp(this.scrollAmount, 0d, Math.max(0d, this.contentHeight() - this.height));
        }

        private record ScrollbarGeometry(int trackX, int trackY, int trackWidth, int trackHeight, int thumbY, int thumbHeight) {
        }

        private record GridMetrics(int startX, int availableWidth, int cellSize, int columns) {
        }
    }
}
