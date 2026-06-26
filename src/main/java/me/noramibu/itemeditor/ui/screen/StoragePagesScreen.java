package me.noramibu.itemeditor.ui.screen;

import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.component.TextBoxComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.container.StackLayout;
import io.wispforest.owo.ui.container.UIContainers;
import io.wispforest.owo.ui.core.Color;
import io.wispforest.owo.ui.core.HorizontalAlignment;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.OwoUIAdapter;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.Surface;
import io.wispforest.owo.ui.core.VerticalAlignment;
import me.noramibu.itemeditor.editor.text.RichTextDocument;
import me.noramibu.itemeditor.service.ClientInventorySyncService;
import me.noramibu.itemeditor.storage.SavedItemStorageService;
import me.noramibu.itemeditor.storage.StorageServices;
import me.noramibu.itemeditor.storage.StorageSizeText;
import me.noramibu.itemeditor.storage.StorageSortMode;
import me.noramibu.itemeditor.storage.model.SavedIndexItemEntry;
import me.noramibu.itemeditor.ui.component.RichTextAreaComponent;
import me.noramibu.itemeditor.ui.component.RichTextTokenDialog;
import me.noramibu.itemeditor.ui.component.UnifiedColorPickerDialog;
import me.noramibu.itemeditor.ui.component.UiFactory;
import me.noramibu.itemeditor.ui.util.ScrollStateUtil;
import me.noramibu.itemeditor.util.ItemEditorText;
import me.noramibu.itemeditor.util.RawItemDataUtil;
import me.noramibu.itemeditor.util.TextColorPresets;
import me.noramibu.itemeditor.util.TextComponentUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

public final class StoragePagesScreen extends BaseOwoScreen<StackLayout> {

    private static final int ROOT_BLUR_RADIUS = 4;
    private static final int ROOT_BLUR_QUALITY = 8;
    private static final int ROOT_SURFACE_TINT = 0x6610151A;
    private static final int SCROLLBAR_THICKNESS = 8;
    private static final int RENAME_EDITOR_HEIGHT = 34;
    private static final int COLOR_GOOD = 0x7ED67A;
    private static final int COLOR_MUTED = 0xA9B5C0;
    private static final int COLOR_DANGER = 0xFF8A8A;
    private static final Surface PAGE_ROW_SURFACE = Surface.flat(0xAA1B222B).and(Surface.outline(0xFF414B56));
    private static final int ACTION_OPEN_WIDTH = 52;
    private static final int ACTION_RENAME_WIDTH = 66;
    private static final int ACTION_DUPLICATE_WIDTH = 72;
    private static final int ACTION_NEW_EMPTY_WIDTH = 138;
    private static final int ACTION_REMOVE_EMPTY_WIDTH = 156;
    private static final int ACTION_IMPORT_OTHER_WIDTH = 166;
    private static final int ACTION_BACKUP_WIDTH = 58;
    private static final int ACTION_EXPORT_WIDTH = 62;
    private static final int ACTION_DELETE_WIDTH = 58;
    private static final int ACTION_MOVE_WIDTH = 24;
    private static final String DEFAULT_DISPLAY_PAGE_NAME = "Chest";
    private static final String SYMBOL_UP = "^";
    private static final String SYMBOL_DOWN = "v";

    private final Minecraft minecraft;
    private final SavedItemStorageService storage = StorageServices.savedItems();
    private String filter = "";
    private String renamingPageId = "";
    private String renameDraft = "";
    private String confirmingDeletePageId = "";
    private String exportingPageId = "";
    private boolean displayEmptyPages = true;
    private int visiblePageLimit;
    private final int returnPage;
    private final String returnQuery;
    private final StorageSortMode returnSortMode;
    private final StorageScreenMode returnMode;
    private final Screen returnScreen;
    private final Consumer<ItemStack> pickedStackConsumer;
    private LabelComponent statusLabel;
    private LabelComponent summaryLabel;
    private FlowLayout pageList;
    private ScrollContainer<FlowLayout> pageScroll;
    private StackLayout rootLayout;
    private FlowLayout activeDialog;

    public StoragePagesScreen(
            Minecraft minecraft,
            int returnPage,
            String returnQuery,
            StorageSortMode returnSortMode,
            StorageScreenMode returnMode,
            Screen returnScreen
    ) {
        this(minecraft, returnPage, returnQuery, returnSortMode, returnMode, returnScreen, null);
    }

    public StoragePagesScreen(
            Minecraft minecraft,
            int returnPage,
            String returnQuery,
            StorageSortMode returnSortMode,
            StorageScreenMode returnMode,
            Screen returnScreen,
            Consumer<ItemStack> pickedStackConsumer
    ) {
        super(ItemEditorText.tr("storage.pages.title"));
        this.minecraft = minecraft;
        this.returnPage = Math.max(1, returnPage);
        this.visiblePageLimit = this.returnPage;
        this.returnQuery = returnQuery == null ? "" : returnQuery;
        this.returnSortMode = returnSortMode == null ? StorageSortMode.REGULAR : returnSortMode;
        this.returnMode = returnMode == null ? StorageScreenMode.MANAGE : returnMode;
        this.returnScreen = returnScreen;
        this.pickedStackConsumer = pickedStackConsumer;
    }

    @Override
    protected @NotNull OwoUIAdapter<StackLayout> createAdapter() {
        return OwoUIAdapter.create(this, UIContainers::stack);
    }

    @Override
    protected void build(StackLayout root) {
        root.clearChildren();
        this.rootLayout = root;
        root.surface(Surface.blur(ROOT_BLUR_RADIUS, ROOT_BLUR_QUALITY).and(Surface.flat(ROOT_SURFACE_TINT)));

        boolean dense = this.denseLayout();
        FlowLayout shell = UiFactory.card();
        int shellMargin = dense ? 8 : 24;
        shell.horizontalSizing(Sizing.fixed(Math.max(300, this.width - shellMargin)));
        shell.verticalSizing(Sizing.fixed(Math.max(210, this.height - shellMargin)));
        shell.gap(dense ? 1 : UiFactory.scaledPixels(5));

        FlowLayout header = UiFactory.row();
        header.child(UiFactory.title(ItemEditorText.tr("storage.pages.title"), dense ? 0.82F : 1.0F)
                .horizontalSizing(Sizing.expand(100)));
        header.child(this.headerButton(ItemEditorText.tr("common.cancel"), 88, () -> this.openStoragePage(this.returnPage)));
        shell.child(header);

        TextBoxComponent search = UiFactory.textBox(this.filter, value -> {
            this.filter = value == null ? "" : value;
            this.refreshPageList();
        });
        search.horizontalSizing(Sizing.fill(100));
        shell.child(search);
        shell.child(UiFactory.muted(
                ItemEditorText.tr("storage.pages.search_help"),
                this.contentTextWidth(),
                dense ? 0.82F : 1.0F
        ));
        var displayEmpty = UiFactory.checkbox(ItemEditorText.tr("storage.pages.display_empty"), this.displayEmptyPages, checked -> {
            this.displayEmptyPages = checked;
            this.refreshPageList();
        });
        displayEmpty.tooltip(List.of(ItemEditorText.tr("storage.pages.display_empty")));
        FlowLayout pageTools = compactActionRow();
        pageTools.child(this.headerButton(
                ItemEditorText.tr("storage.pages.new_empty").copy().withColor(COLOR_GOOD),
                ACTION_NEW_EMPTY_WIDTH,
                this::createPage
        ));
        pageTools.child(this.headerButton(
                ItemEditorText.tr("storage.pages.remove_empty").copy().withColor(COLOR_DANGER),
                ACTION_REMOVE_EMPTY_WIDTH,
                this::removeEmptyPages
        ));
        pageTools.child(this.headerButton(
                ItemEditorText.tr("storage.import_other_mods").copy().withColor(COLOR_GOOD),
                ACTION_IMPORT_OTHER_WIDTH,
                this::openOtherModsImport
        ));
        if (dense && this.width >= UiFactory.scaledPixels(720)) {
            FlowLayout toolsLine = compactActionRow();
            toolsLine.child(displayEmpty);
            toolsLine.child(pageTools);
            shell.child(toolsLine);
        } else {
            shell.child(displayEmpty);
            shell.child(pageTools);
        }

        this.summaryLabel = UiFactory.muted(Component.literal(" "), this.contentTextWidth(), dense ? 0.82F : 1.0F);
        shell.child(this.summaryLabel);

        this.pageList = UiFactory.scrollContentColumn(SCROLLBAR_THICKNESS);
        this.refreshPageList();
        ScrollContainer<FlowLayout> scroll = UIContainers.verticalScroll(Sizing.fill(100), Sizing.expand(100), this.pageList);
        this.pageScroll = scroll;
        scroll.scrollbar(ScrollContainer.Scrollbar.vanillaFlat());
        scroll.scrollbarThiccness(UiFactory.scaledScrollbarThickness(SCROLLBAR_THICKNESS));
        scroll.scrollStep(UiFactory.scaledScrollStep(10));
        shell.child(scroll);

        this.statusLabel = UiFactory.message(Component.literal(" "), 0xA9B5C0).maxWidth(Math.max(100, this.width - 60));
        shell.child(this.statusLabel);

        FlowLayout centered = UiFactory.column();
        centered.horizontalSizing(Sizing.fill(100));
        centered.verticalSizing(Sizing.fill(100));
        centered.padding(Insets.of(dense ? 3 : 8));
        centered.horizontalAlignment(HorizontalAlignment.CENTER);
        centered.verticalAlignment(VerticalAlignment.CENTER);
        centered.child(shell);
        root.child(centered);
        if (this.activeDialog != null) {
            root.child(this.activeDialog);
        }
    }

    private void refreshPageList() {
        if (this.pageList == null) {
            return;
        }
        double scrollOffset = ScrollStateUtil.offset(this.pageScroll);
        this.pageList.clearChildren();
        List<SavedItemStorageService.PageInfo> pages = this.storage.listPages(Math.max(this.returnPage, this.visiblePageLimit));
        this.updateSummary(pages);
        for (SavedItemStorageService.PageInfo page : this.filteredPages(pages)) {
            this.pageList.child(this.buildPageRow(page));
        }
        this.restorePageScroll(scrollOffset);
    }

    private FlowLayout buildPageRow(SavedItemStorageService.PageInfo page) {
        FlowLayout row = UIContainers.verticalFlow(Sizing.fill(100), Sizing.content());
        row.gap(this.denseLayout() ? 1 : Math.max(1, UiFactory.scaleProfile().tightSpacing()));
        row.padding(Insets.of(this.denseLayout() ? 2 : Math.max(4, UiFactory.scaleProfile().padding() - 1)));
        row.surface(PAGE_ROW_SURFACE);
        row.allowOverflow(false);
        FlowLayout summary = UiFactory.row();
        summary.child(UiFactory.title(pageTitleComponent(page), this.denseLayout() ? 0.74F : 1.0F)
                .shadow(false)
                .horizontalSizing(Sizing.expand(100)));
        row.child(summary);
        if (this.denseLayout()) {
            row.child(UiFactory.muted(
                    Component.literal(pageMetaLine(page) + " | " + pageTimeLine(page)),
                    this.contentTextWidth(),
                    0.78F
            ));
        } else {
            row.child(UiFactory.muted(Component.literal(pageMetaLine(page)), this.contentTextWidth()));
            row.child(UiFactory.muted(Component.literal(pageTimeLine(page)), this.contentTextWidth()));
        }

        if (page.id().equals(this.renamingPageId)) {
            row.child(this.buildRenameEditor());
            row.child(UiFactory.actionButtonRow(
                    UiFactory.positiveButton(ItemEditorText.tr("common.save"), UiFactory.ButtonTextPreset.STANDARD, button -> this.renamePage(page)),
                    UiFactory.button(ItemEditorText.tr("common.cancel"), UiFactory.ButtonTextPreset.STANDARD, button -> this.updatePageState(this.filter, "", "", "", ""))
            ));
            return row;
        }

        if (page.id().equals(this.confirmingDeletePageId)) {
            row.child(UiFactory.message(ItemEditorText.tr("storage.pages.delete_confirm"), COLOR_DANGER));
            row.child(UiFactory.actionButtonRow(
                    UiFactory.negativeButton(ItemEditorText.tr("common.delete"), UiFactory.ButtonTextPreset.STANDARD, button -> this.deletePage(page, true)),
                    UiFactory.button(ItemEditorText.tr("common.cancel"), UiFactory.ButtonTextPreset.STANDARD, button -> this.updatePageState(this.filter, "", "", "", ""))
            ));
            return row;
        }

        if (page.id().equals(this.exportingPageId)) {
            row.child(UiFactory.actionButtonRow(
                    UiFactory.button(ItemEditorText.tr("storage.pages.export_chest"), UiFactory.ButtonTextPreset.STANDARD, button -> this.exportPage(page, false)),
                    UiFactory.button(ItemEditorText.tr("storage.pages.export_shulker"), UiFactory.ButtonTextPreset.STANDARD, button -> this.exportPage(page, true)),
                    UiFactory.button(ItemEditorText.tr("common.cancel"), UiFactory.ButtonTextPreset.STANDARD, button -> this.updatePageState(this.filter, "", "", "", ""))
            ));
            row.child(UiFactory.actionButtonRow(
                    UiFactory.button(ItemEditorText.tr("storage.pages.export_give_chest"), UiFactory.ButtonTextPreset.STANDARD, button -> this.exportPageCommands(page, false, false)),
                    UiFactory.button(ItemEditorText.tr("storage.pages.export_item_chest"), UiFactory.ButtonTextPreset.STANDARD, button -> this.exportPageCommands(page, false, true)),
                    UiFactory.button(ItemEditorText.tr("storage.pages.export_give_shulker"), UiFactory.ButtonTextPreset.STANDARD, button -> this.exportPageCommands(page, true, false)),
                    UiFactory.button(ItemEditorText.tr("storage.pages.export_item_shulker"), UiFactory.ButtonTextPreset.STANDARD, button -> this.exportPageCommands(page, true, true))
            ));
            return row;
        }

        row.child(this.buildPageActions(page));
        return row;
    }

    private io.wispforest.owo.ui.component.ButtonComponent headerButton(Component label, int widthPixels, Runnable action) {
        float factor = this.denseLayout() && widthPixels <= 80 ? 0.74F : 0.95F;
        int width = UiFactory.scaledPixels(this.denseLayout() ? Math.max(22, Math.round(widthPixels * factor)) : widthPixels);
        io.wispforest.owo.ui.component.ButtonComponent button = UiFactory.scaledTextButton(
                UiFactory.fitToWidth(label.copy(), Math.max(8, width - 6)),
                this.denseLayout() ? 0.70F : 1.0F,
                this.denseLayout() ? UiFactory.ButtonTextPreset.COMPACT : UiFactory.ButtonTextPreset.STANDARD,
                ignored -> action.run()
        );
        button.horizontalSizing(Sizing.fixed(width));
        button.verticalSizing(Sizing.fixed(UiFactory.scaledPixels(this.denseLayout() ? 15 : 18)));
        button.tooltip(List.of(label));
        return button;
    }

    private FlowLayout buildPageActions(SavedItemStorageService.PageInfo page) {
        if (this.width >= fullActionRowWidth() + UiFactory.scaledPixels(48)) {
            return this.buildPrimaryPageActionRow(page);
        }

        FlowLayout stacked = UiFactory.column();
        stacked.gap(Math.max(1, UiFactory.scaleProfile().tightSpacing()));
        FlowLayout firstRow = compactActionRow();
        this.addPrimaryPageButtons(firstRow, page);
        FlowLayout secondRow = compactActionRow();
        this.addSecondaryPageButtons(secondRow, page);
        FlowLayout moveRow = compactActionRow();
        moveRow.child(this.smallMoveButton(SYMBOL_UP, ItemEditorText.tr("common.up"), !page.placeholderPage(), () -> this.movePage(page, -1)));
        moveRow.child(this.smallMoveButton(SYMBOL_DOWN, ItemEditorText.tr("common.down"), !page.placeholderPage(), () -> this.movePage(page, 1)));
        stacked.child(firstRow);
        stacked.child(secondRow);
        stacked.child(moveRow);
        return stacked;
    }

    private FlowLayout buildPrimaryPageActionRow(SavedItemStorageService.PageInfo page) {
        FlowLayout primary = compactActionRow();
        this.addPrimaryPageButtons(primary, page);
        primary.child(this.smallMoveButton(SYMBOL_UP, ItemEditorText.tr("common.up"), !page.placeholderPage(), () -> this.movePage(page, -1)));
        primary.child(this.smallMoveButton(SYMBOL_DOWN, ItemEditorText.tr("common.down"), !page.placeholderPage(), () -> this.movePage(page, 1)));
        this.addSecondaryPageButtons(primary, page);
        return primary;
    }

    private void addPrimaryPageButtons(FlowLayout row, SavedItemStorageService.PageInfo page) {
        row.child(pageActionButton(ItemEditorText.tr("storage.pages.open"), ACTION_OPEN_WIDTH, COLOR_GOOD, true, () -> this.openStoragePage(page.pageNumber())));
        row.child(pageActionButton(ItemEditorText.tr("storage.pages.rename"), ACTION_RENAME_WIDTH, COLOR_MUTED, true, () -> this.updatePageState(this.filter, page.id(), page.name(), "", "")));
        row.child(pageActionButton(ItemEditorText.tr("storage.pages.duplicate"), ACTION_DUPLICATE_WIDTH, COLOR_MUTED, !page.placeholderPage(), () -> this.duplicatePage(page)));
    }

    private void addSecondaryPageButtons(FlowLayout row, SavedItemStorageService.PageInfo page) {
        row.child(pageActionButton(ItemEditorText.tr("storage.pages.backup"), ACTION_BACKUP_WIDTH, COLOR_MUTED, true, () -> this.backupPage(page)));
        row.child(pageActionButton(ItemEditorText.tr("storage.pages.export"), ACTION_EXPORT_WIDTH, COLOR_MUTED, !page.placeholderPage(), () -> this.updatePageState(this.filter, "", "", "", page.id())));
        row.child(pageActionButton(ItemEditorText.tr("common.delete"), ACTION_DELETE_WIDTH, COLOR_DANGER, true, () -> this.deletePage(page, false)));
    }

    private FlowLayout buildRenameEditor() {
        FlowLayout box = UiFactory.column();
        box.gap(Math.max(1, UiFactory.scaleProfile().tightSpacing()));
        RichTextAreaComponent editor = new RichTextAreaComponent(
                Sizing.fill(100),
                Sizing.fixed(UiFactory.scaledPixels(RENAME_EDITOR_HEIGHT)),
                RichTextDocument.fromMarkup(this.renameDraft)
        );
        editor.placeholder(ItemEditorText.str("storage.pages.rename"));
        editor.displayCharCount(true);
        editor.structuredRenderMode(true);
        editor.palette(0xFFF2F5F8, 0x90768496, 0xC56E93FF, 0xFFF2F5F8);
        editor.chrome(0xD0111620, 0xFF42506A);
        editor.validator(document -> document.logicalLineCount() > 1
                ? ItemEditorText.str("text.validation.single_line", ItemEditorText.str("storage.pages.rename"))
                : null);
        editor.onDocumentChanged().subscribe(document -> this.renameDraft = TextComponentUtil.toMarkup(document.toComponent()));
        box.child(this.buildRenameToolbar(editor));
        box.child(editor);
        return box;
    }

    private FlowLayout buildRenameToolbar(RichTextAreaComponent editor) {
        FlowLayout tools = UiFactory.row();
        tools.gap(Math.max(1, UiFactory.scaleProfile().tightSpacing()));
        tools.child(toolButton("B", ItemEditorText.tr("toolbar.tooltip.bold"), editor::toggleBold));
        tools.child(toolButton("I", ItemEditorText.tr("toolbar.tooltip.italic"), editor::toggleItalic));
        tools.child(toolButton("U", ItemEditorText.tr("toolbar.tooltip.underline"), editor::toggleUnderline));
        tools.child(toolButton("S", ItemEditorText.tr("toolbar.tooltip.strikethrough"), editor::toggleStrikethrough));
        tools.child(toolButton(ItemEditorText.str("toolbar.color"), ItemEditorText.tr("toolbar.tooltip.color"), () -> this.openColorDialog(editor)));
        tools.child(toolButton(ItemEditorText.str("toolbar.head"), ItemEditorText.tr("toolbar.tooltip.head"), () -> this.openHeadDialog(editor)));
        tools.child(toolButton(ItemEditorText.str("toolbar.sprite"), ItemEditorText.tr("toolbar.tooltip.sprite"), () -> this.openSpriteDialog(editor)));
        tools.child(toolButton(ItemEditorText.str("toolbar.reset"), ItemEditorText.tr("toolbar.tooltip.reset"), editor::clearFormatting));
        return tools;
    }

    private List<SavedItemStorageService.PageInfo> filteredPages(List<SavedItemStorageService.PageInfo> pages) {
        String normalized = this.filter.trim().toLowerCase(Locale.ROOT);
        List<SavedItemStorageService.PageInfo> filtered = new ArrayList<>();
        for (SavedItemStorageService.PageInfo page : pages) {
            if (!this.displayEmptyPages && page.itemCount() == 0) {
                continue;
            }
            if (normalized.isBlank()) {
                filtered.add(page);
                continue;
            }
            if (this.matchesPageSearch(page, normalized)) {
                filtered.add(page);
            }
        }
        return filtered;
    }

    private boolean matchesPageSearch(SavedItemStorageService.PageInfo page, String normalized) {
        String title = pageTitle(page).toLowerCase(Locale.ROOT);
        String name = page.namePlain() == null ? "" : page.namePlain().toLowerCase(Locale.ROOT);
        for (String token : normalized.split("\\s+")) {
            if (token.isBlank()) {
                continue;
            }
            if (token.startsWith("items:") || token.startsWith("item-count:")) {
                if (failsNumberFilter(page.itemCount(), token.substring(token.indexOf(':') + 1), false)) {
                    return false;
                }
                continue;
            }
            if (token.startsWith("size:") || token.startsWith("bytes:")) {
                if (failsNumberFilter(page.nbtBytes(), token.substring(token.indexOf(':') + 1), true)) {
                    return false;
                }
                continue;
            }
            if (token.startsWith("page-name:")) {
                String pageName = token.substring("page-name:".length());
                if (pageName.isBlank() || !name.contains(pageName)) {
                    return false;
                }
                continue;
            }
            if (!title.contains(token) && !name.contains(token)) {
                return false;
            }
        }
        return true;
    }

    private void updateSummary(List<SavedItemStorageService.PageInfo> pages) {
        if (this.summaryLabel == null) {
            return;
        }
        int named = 0;
        int empty = 0;
        int placeholders = 0;
        int items = 0;
        int bytes = 0;
        for (SavedItemStorageService.PageInfo page : pages) {
            if (page.namePlain() != null && !page.namePlain().isBlank()) {
                named++;
            }
            if (page.itemCount() == 0) {
                empty++;
            }
            if (page.placeholderPage()) {
                placeholders++;
            }
            items += page.itemCount();
            bytes += page.nbtBytes();
        }
        this.summaryLabel.text(Component.literal(ItemEditorText.str(
                "storage.pages.summary",
                pages.size(),
                named,
                empty,
                placeholders,
                items,
                StorageSizeText.sizeLine(bytes)
        )));
    }

    private static boolean failsNumberFilter(int value, String expression, boolean byteSize) {
        String filter = expression == null ? "" : expression.trim().toLowerCase(Locale.ROOT);
        if (filter.isBlank()) {
            return false;
        }
        int rangeIndex = filter.indexOf('-', 1);
        if (rangeIndex > 0) {
            long minimum = parseFilterNumber(filter.substring(0, rangeIndex), byteSize);
            long maximum = parseFilterNumber(filter.substring(rangeIndex + 1), byteSize);
            if (minimum < 0L || maximum < 0L) {
                return true;
            }
            return value < Math.min(minimum, maximum) || value > Math.max(minimum, maximum);
        }
        if (filter.startsWith(">=")) {
            long target = parseFilterNumber(filter.substring(2), byteSize);
            return target < 0L || value < target;
        }
        if (filter.startsWith("<=")) {
            long target = parseFilterNumber(filter.substring(2), byteSize);
            return target < 0L || value > target;
        }
        if (filter.startsWith(">")) {
            long target = parseFilterNumber(filter.substring(1), byteSize);
            return target < 0L || value <= target;
        }
        if (filter.startsWith("<")) {
            long target = parseFilterNumber(filter.substring(1), byteSize);
            return target < 0L || value >= target;
        }
        if (filter.startsWith("=")) {
            long target = parseFilterNumber(filter.substring(1), byteSize);
            return target < 0L || value != target;
        }
        long target = parseFilterNumber(filter, byteSize);
        return target < 0L || value != target;
    }

    private static long parseFilterNumber(String raw, boolean byteSize) {
        String value = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        long multiplier = 1L;
        if (byteSize && (value.endsWith("kb") || value.endsWith("k"))) {
            multiplier = 1024L;
            value = value.replaceAll("kb?$", "");
        } else if (byteSize && (value.endsWith("mb") || value.endsWith("m"))) {
            multiplier = 1024L * 1024L;
            value = value.replaceAll("mb?$", "");
        } else if (byteSize && value.endsWith("b")) {
            value = value.substring(0, value.length() - 1);
        }
        try {
            return Math.max(0L, Math.round(Double.parseDouble(value) * multiplier));
        } catch (NumberFormatException exception) {
            return -1L;
        }
    }

    private io.wispforest.owo.ui.component.ButtonComponent smallMoveButton(
            String label,
            Component tooltip,
            boolean enabled,
            Runnable action
    ) {
        io.wispforest.owo.ui.component.ButtonComponent button = UiFactory.button(
                Component.literal(label),
                UiFactory.ButtonTextPreset.TINY,
                ignored -> action.run()
        );
        button.tooltip(List.of(tooltip));
        button.horizontalSizing(Sizing.fixed(UiFactory.scaledPixels(ACTION_MOVE_WIDTH)));
        button.verticalSizing(Sizing.fixed(UiFactory.scaledPixels(this.denseLayout() ? 14 : 18)));
        button.active(enabled);
        return button;
    }

    private static FlowLayout compactActionRow() {
        FlowLayout row = UiFactory.row();
        row.gap(Math.max(1, UiFactory.scaleProfile().tightSpacing()));
        return row;
    }

    private static int fullActionRowWidth() {
        int buttons = ACTION_OPEN_WIDTH
                + ACTION_RENAME_WIDTH
                + ACTION_DUPLICATE_WIDTH
                + ACTION_BACKUP_WIDTH
                + ACTION_EXPORT_WIDTH
                + ACTION_DELETE_WIDTH
                + (ACTION_MOVE_WIDTH * 2);
        return UiFactory.scaledPixels(buttons) + (Math.max(1, UiFactory.scaleProfile().tightSpacing()) * 7);
    }

    private io.wispforest.owo.ui.component.ButtonComponent pageActionButton(
            Component label,
            int width,
            int color,
            boolean enabled,
            Runnable action
    ) {
        int scaledWidth = UiFactory.scaledPixels(width);
        Component displayLabel = UiFactory.fitToWidth(label.copy().withColor(color), Math.max(8, scaledWidth - 8));
        io.wispforest.owo.ui.component.ButtonComponent button = UiFactory.scaledTextButton(
                displayLabel,
                this.denseLayout() ? 0.68F : 1.0F,
                UiFactory.ButtonTextPreset.TINY,
                ignored -> action.run()
        );
        button.horizontalSizing(Sizing.fixed(scaledWidth));
        button.verticalSizing(Sizing.fixed(UiFactory.scaledPixels(this.denseLayout() ? 14 : 18)));
        button.tooltip(List.of(label));
        button.active(enabled);
        return button;
    }

    private static io.wispforest.owo.ui.component.ButtonComponent toolButton(
            String label,
            Component tooltip,
            Runnable action
    ) {
        io.wispforest.owo.ui.component.ButtonComponent button = UiFactory.button(
                Component.literal(label),
                UiFactory.ButtonTextPreset.TINY,
                ignored -> action.run()
        );
        if (tooltip != null && !tooltip.getString().isBlank()) {
            button.tooltip(List.of(tooltip));
        }
        button.horizontalSizing(Sizing.fixed(UiFactory.scaledPixels(42)));
        return button;
    }

    private void openColorDialog(RichTextAreaComponent editor) {
        this.attachDialog(UnifiedColorPickerDialog.create(
                ItemEditorText.str("storage.pages.color_title"),
                new UnifiedColorPickerDialog.Options(
                        UnifiedColorPickerDialog.PaintMode.COLOR,
                        false,
                        List.of(0xFFFFFF, TextColorPresets.gradientEndFor(0xFFFFFF)),
                        true,
                        true,
                        true,
                        true,
                        true,
                        true,
                        editor.selectedTextOr("")
                ),
                result -> {
                    this.clearDialog();
                    if (result.shadow()) {
                        if (result.mode() == UnifiedColorPickerDialog.PaintMode.GRADIENT) {
                            editor.applyShadowGradientSelectionOrAll(result.colors());
                        } else if (!result.colors().isEmpty()) {
                            editor.applyShadowColor(result.colors().getFirst());
                        }
                    } else if (result.mode() == UnifiedColorPickerDialog.PaintMode.GRADIENT) {
                        editor.applyGradientSelectionOrAll(result.colors());
                    } else if (!result.colors().isEmpty()) {
                        editor.applyColor(result.colors().getFirst());
                    }
                    editor.resumeEditing();
                },
                this::clearDialog
        ));
    }

    private void openHeadDialog(RichTextAreaComponent editor) {
        this.attachDialog(RichTextTokenDialog.createHead(
                ItemEditorText.str("dialog.rich_text.head.title"),
                token -> {
                    this.clearDialog();
                    editor.insertTemplate(token);
                    editor.resumeEditing();
                },
                this::clearDialog
        ));
    }

    private void openSpriteDialog(RichTextAreaComponent editor) {
        this.attachDialog(RichTextTokenDialog.createSprite(
                ItemEditorText.str("dialog.rich_text.sprite.title"),
                token -> {
                    this.clearDialog();
                    editor.insertTemplate(token);
                    editor.resumeEditing();
                },
                this::clearDialog
        ));
    }

    private void attachDialog(FlowLayout dialog) {
        this.clearDialog();
        this.activeDialog = dialog;
        if (this.rootLayout != null) {
            this.rootLayout.child(dialog);
        }
    }

    private void clearDialog() {
        if (this.activeDialog == null) {
            return;
        }
        if (this.rootLayout != null) {
            this.rootLayout.removeChild(this.activeDialog);
        }
        this.activeDialog = null;
    }

    private void renamePage(SavedItemStorageService.PageInfo page) {
        this.storage.enqueueRenamePage(page.id(), page.pageNumber(), this.renameDraft);
        this.storage.flushQueuedWrites();
        this.updatePageState(this.filter, "", "", "", "");
    }

    private void createPage() {
        this.visiblePageLimit = Math.max(this.visiblePageLimit, this.storage.nextEmptyPageNumber(this.visiblePageLimit));
        this.setStatus(Component.literal(ItemEditorText.str("storage.pages.created", this.visiblePageLimit)), COLOR_GOOD);
        this.refreshPageList();
    }

    private void removeEmptyPages() {
        int includePage = Math.max(this.returnPage, this.visiblePageLimit);
        long visibleEmptyPages = this.storage.listPages(includePage).stream()
                .filter(page -> page.itemCount() == 0)
                .count();
        int removedStoredPages = this.storage.enqueueRemoveEmptyPages().join();
        this.storage.flushQueuedWrites();
        this.visiblePageLimit = Math.max(1, this.storage.nextEmptyPageNumber(1) - 1);
        this.setStatus(Component.literal(ItemEditorText.str(
                "storage.pages.removed_empty",
                Math.max(visibleEmptyPages, removedStoredPages)
        )), COLOR_GOOD);
        this.updatePageState(this.filter, "", "", "", "");
    }

    private void duplicatePage(SavedItemStorageService.PageInfo page) {
        int copiedPage = this.storage.enqueueDuplicatePage(page.pageNumber()).join();
        this.storage.flushQueuedWrites();
        if (copiedPage < 1) {
            this.setStatus(ItemEditorText.tr("storage.pages.duplicate_failed"), COLOR_DANGER);
            return;
        }
        this.setStatus(Component.literal(ItemEditorText.str("storage.pages.duplicated", copiedPage)), COLOR_GOOD);
        this.updatePageState(this.filter, "", "", "", "");
    }

    private void movePage(SavedItemStorageService.PageInfo page, int offset) {
        this.storage.enqueueMovePage(page.id(), offset);
        this.storage.flushQueuedWrites();
        this.updatePageState(this.filter, "", "", "", "");
    }

    private void backupPage(SavedItemStorageService.PageInfo page) {
        this.storage.enqueueBackupPage(page.pageNumber()).whenComplete((backup, throwable) -> this.minecraft.execute(() -> {
            if (throwable != null || backup == null) {
                this.setStatus(ItemEditorText.tr("storage.pages.backup_failed"), COLOR_DANGER);
                return;
            }
            Path fileName = backup.getFileName();
            this.setStatus(
                    Component.literal(ItemEditorText.str(
                            "storage.pages.backed_up",
                            fileName == null ? backup.toString() : fileName.toString()
                    )),
                    COLOR_GOOD
            );
        }));
    }

    private void deletePage(SavedItemStorageService.PageInfo page, boolean confirmed) {
        if (!confirmed && page.itemCount() > 0) {
            this.updatePageState(this.filter, "", "", page.id(), "");
            return;
        }
        int firstVirtualTrailingPage = this.storage.nextEmptyPageNumber(this.returnPage);
        if (page.placeholderPage() && page.pageNumber() >= firstVirtualTrailingPage) {
            this.visiblePageLimit = Math.max(this.returnPage, this.visiblePageLimit - 1);
            this.updatePageState(this.filter, "", "", "", "");
            return;
        }
        boolean deleted = this.storage.enqueueDeletePageNumber(page.pageNumber()).join();
        this.storage.flushQueuedWrites();
        if (deleted) {
            if (this.visiblePageLimit >= page.pageNumber()) {
                this.visiblePageLimit = Math.max(this.returnPage, this.visiblePageLimit - 1);
            }
            this.updatePageState(this.filter, "", "", "", "");
        }
    }

    private void exportPage(SavedItemStorageService.PageInfo page, boolean shulker) {
        this.storage
                .loadSnapshotAsync(page.pageNumber(), "", StorageSortMode.REGULAR, false, this.registryAccess())
                .whenComplete((snapshot, throwable) -> this.minecraft.execute(() -> {
                    if (throwable != null || snapshot == null) {
                        this.setStatus(ItemEditorText.tr("storage.pages.export_failed"), 0xFF8A8A);
                        return;
                    }
                    int exported = this.giveExportContainers(page, snapshot, shulker);
                    this.setStatus(Component.literal(ItemEditorText.str("storage.pages.exported", exported)), 0x7ED67A);
                }));
    }

    private void exportPageCommands(SavedItemStorageService.PageInfo page, boolean shulker, boolean itemCommand) {
        this.storage
                .loadSnapshotAsync(page.pageNumber(), "", StorageSortMode.REGULAR, false, this.registryAccess())
                .whenComplete((snapshot, throwable) -> this.minecraft.execute(() -> {
                    if (throwable != null || snapshot == null) {
                        this.setStatus(ItemEditorText.tr("storage.pages.export_failed"), COLOR_DANGER);
                        return;
                    }
                    RegistryAccess access = this.registryAccess();
                    List<String> commands = new ArrayList<>();
                    for (ItemStack container : this.exportContainers(page, snapshot, shulker)) {
                        String command = itemCommand
                                ? RawItemDataUtil.serializeItemCommand(container, access)
                                : RawItemDataUtil.serializeGiveCommand(container, access);
                        if (!command.isBlank()) {
                            commands.add(command);
                        }
                    }
                    this.minecraft.keyboardHandler.setClipboard(String.join("\n", commands));
                    this.setStatus(Component.literal(ItemEditorText.str("storage.pages.commands_copied", commands.size())), COLOR_GOOD);
                    this.updatePageState(this.filter, "", "", "", "");
                }));
    }

    private int giveExportContainers(
            SavedItemStorageService.PageInfo page,
            SavedItemStorageService.PageSnapshot snapshot,
            boolean shulker
    ) {
        int added = 0;
        for (ItemStack container : this.exportContainers(page, snapshot, shulker)) {
            if (this.giveStack(container)) {
                added++;
            }
        }
        return added;
    }

    private List<ItemStack> exportContainers(
            SavedItemStorageService.PageInfo page,
            SavedItemStorageService.PageSnapshot snapshot,
            boolean shulker
    ) {
        List<ItemStack> first = emptyContainerSlots();
        List<ItemStack> second = emptyContainerSlots();
        Map<String, ItemStack> stacks = snapshot.loadedStacks();
        boolean secondUsed = false;
        for (SavedIndexItemEntry entry : snapshot.result().entries()) {
            ItemStack stack = stacks.getOrDefault(entry.id, ItemStack.EMPTY).copy();
            if (stack.isEmpty()) {
                continue;
            }
            int slot = Math.max(0, entry.slotInPage);
            if (slot < 27) {
                first.set(slot, stack);
            } else {
                second.set(slot - 27, stack);
                secondUsed = true;
            }
        }

        List<ItemStack> containers = new ArrayList<>();
        containers.add(this.createContainer(page.namePlain(), first, shulker));
        if (secondUsed) {
            containers.add(this.createContainer(page.namePlain() + " 2", second, shulker));
        }
        return containers;
    }

    private ItemStack createContainer(String name, List<ItemStack> contents, boolean shulker) {
        Item item = shulker ? Items.SHULKER_BOX : Items.CHEST;
        ItemStack container = new ItemStack(item);
        container.set(DataComponents.CUSTOM_NAME, TextComponentUtil.parseMarkup(name == null || name.isBlank() ? "Storage Page" : name));
        container.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(contents));
        return container;
    }

    private boolean giveStack(ItemStack stack) {
        return ClientInventorySyncService.putInFreeSlot(this.minecraft, stack);
    }

    private static List<ItemStack> emptyContainerSlots() {
        List<ItemStack> stacks = new ArrayList<>();
        for (int slot = 0; slot < 27; slot++) {
            stacks.add(ItemStack.EMPTY);
        }
        return stacks;
    }

    private void openStoragePage(int page) {
        this.minecraft.setScreen(new StorageScreen(
                page,
                this.returnQuery,
                this.returnSortMode,
                this.returnMode,
                this.returnScreen,
                this.pickedStackConsumer
        ));
    }

    private void openOtherModsImport() {
        this.minecraft.setScreen(new OtherModsImportScreen(
                this.minecraft,
                this.returnPage,
                this.returnQuery,
                this.returnSortMode,
                this.returnMode,
                this.returnScreen
        ));
    }

    private void updatePageState(
            String nextFilter,
            String nextRenamingPageId,
            String nextRenameDraft,
            String nextConfirmingDeletePageId,
            String nextExportingPageId
    ) {
        this.filter = nextFilter == null ? "" : nextFilter;
        this.renamingPageId = nextRenamingPageId == null ? "" : nextRenamingPageId;
        this.renameDraft = nextRenameDraft == null ? "" : nextRenameDraft;
        this.confirmingDeletePageId = nextConfirmingDeletePageId == null ? "" : nextConfirmingDeletePageId;
        this.exportingPageId = nextExportingPageId == null ? "" : nextExportingPageId;
        this.refreshPageList();
    }

    private void restorePageScroll(double scrollOffset) {
        if (this.pageScroll == null) {
            return;
        }
        ScrollStateUtil.restore(this.pageScroll, scrollOffset);
        this.minecraft.execute(() -> ScrollStateUtil.restore(this.pageScroll, scrollOffset));
    }

    private boolean denseLayout() {
        return this.width <= 980 || this.height <= 620;
    }

    private int contentTextWidth() {
        return Math.max(160, this.width - (this.denseLayout() ? 48 : 96));
    }

    private void setStatus(Component message, int color) {
        if (this.statusLabel != null) {
            this.statusLabel.text(message == null ? Component.literal(" ") : message);
            this.statusLabel.color(Color.ofRgb(color));
        }
    }

    private RegistryAccess registryAccess() {
        return this.minecraft.level == null ? RegistryAccess.EMPTY : this.minecraft.level.registryAccess();
    }

    private static String pageTitle(SavedItemStorageService.PageInfo page) {
        String name = page.namePlain() == null || page.namePlain().isBlank()
                ? DEFAULT_DISPLAY_PAGE_NAME
                : page.namePlain();
        return "#" + page.pageNumber() + " " + name;
    }

    private static String pageMetaLine(SavedItemStorageService.PageInfo page) {
        String prefix = page.placeholderPage() ? "placeholder, " : "";
        return prefix + page.itemCount() + " items, " + StorageSizeText.sizeLine(page.nbtBytes()) + ", " + page.chunkId();
    }

    private static String pageTimeLine(SavedItemStorageService.PageInfo page) {
        String saved = page.savedAt() <= 0L ? "saved never" : "saved " + relativeTime(page.savedAt());
        String updated = page.updatedAt() <= 0L ? "updated never" : "updated " + relativeTime(page.updatedAt());
        return saved + " | " + updated;
    }

    private static Component pageTitleComponent(SavedItemStorageService.PageInfo page) {
        Component name = TextComponentUtil.parseMarkup(page.name() == null || page.name().isBlank()
                ? DEFAULT_DISPLAY_PAGE_NAME
                : page.name());
        return Component.literal("#" + page.pageNumber() + " ").withColor(0xA9B5C0).append(name);
    }

    private static String relativeTime(long epochMillis) {
        long seconds = Math.max(0L, (System.currentTimeMillis() - epochMillis) / 1000L);
        if (seconds < 60L) {
            return "just now";
        }
        long minutes = seconds / 60L;
        if (minutes < 60L) {
            return minutes + " minute" + (minutes == 1L ? "" : "s") + " ago";
        }
        long hours = minutes / 60L;
        if (hours < 24L) {
            return hours + " hour" + (hours == 1L ? "" : "s") + " ago";
        }
        long days = hours / 24L;
        return days + " day" + (days == 1L ? "" : "s") + " ago";
    }

    @Override
    public void onClose() {
        this.openStoragePage(this.returnPage);
    }
}
