package me.noramibu.itemeditor.ui.screen;

import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.StackLayout;
import io.wispforest.owo.ui.container.UIContainers;
import io.wispforest.owo.ui.core.Color;
import io.wispforest.owo.ui.core.OwoUIAdapter;
import io.wispforest.owo.ui.core.Sizing;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicBoolean;
import me.noramibu.itemeditor.editor.ItemEditorSession;
import me.noramibu.itemeditor.editor.ItemEditorSessionOrigin;
import me.noramibu.itemeditor.service.FileImportService;
import me.noramibu.itemeditor.storage.StorageServices;
import me.noramibu.itemeditor.storage.StorageSortMode;
import me.noramibu.itemeditor.ui.component.UiFactory;
import me.noramibu.itemeditor.ui.util.MenuBackgroundSurface;
import me.noramibu.itemeditor.ui.util.UiColors;
import me.noramibu.itemeditor.util.ItemEditorText;
import me.noramibu.itemeditor.util.NativeFileDialog;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public final class ImportScreen extends BaseOwoScreen<StackLayout> {
    private static final int CARD_WIDTH = 270;
    private final Minecraft minecraft;
    private final Screen returnScreen;
    private StackLayout root;
    private LabelComponent statusLabel;
    private Component status = Component.literal(" ");
    private int statusColor = UiColors.MUTED;
    private AtomicBoolean pending;
    private boolean saving;
    private String duplicatePageId;
    private boolean pickerOpen;
    private FileImportService.Preview preview;
    private RegistryAccess previewAccess;

    public ImportScreen(Minecraft minecraft, Screen returnScreen) {
        super(ItemEditorText.tr("import.title"));
        this.minecraft = minecraft;
        this.returnScreen = returnScreen;
    }

    @Override
    protected @NotNull OwoUIAdapter<StackLayout> createAdapter() {
        return OwoUIAdapter.create(this, UIContainers::stack);
    }

    @Override
    protected void build(StackLayout root) {
        this.root = root;
        root.clearChildren();
        root.surface(MenuBackgroundSurface.standard());
        FlowLayout card = UiFactory.centeredCard(CARD_WIDTH);
        card.child(UiFactory.title(ItemEditorText.tr("import.title")));
        this.option(
                card,
                "import.paste_item",
                "import.paste_item.tooltip",
                () -> this.minecraft.setScreenAndShow(new RawImportScreen(this.minecraft, this)));
        this.option(card, "import.file", "import.file.tooltip", this::openFileDialog);
        this.option(
                card,
                "import.storage",
                "import.storage.tooltip",
                () -> this.minecraft.setScreenAndShow(new StorageScreen(StorageScreenMode.COPY_IMPORT, this, null)));
        this.option(
                card,
                "storage.import_other_mods",
                "import.other_mods.tooltip",
                () -> this.minecraft.setScreenAndShow(new OtherModsImportScreen(this.minecraft, this)));
        this.statusLabel = UiFactory.message(this.status, this.statusColor).maxWidth(UiFactory.scaledPixels(230));
        card.child(this.statusLabel);
        var back = UiFactory.button(
                ItemEditorText.tr(this.pending != null || this.preview != null ? "common.cancel" : "entry.back"),
                UiFactory.ButtonTextPreset.STANDARD,
                button -> {
                    if (this.pending != null || this.preview != null) this.cancelImport();
                    else this.onClose();
                });
        back.active(!this.saving);
        back.horizontalSizing(Sizing.fill(100));
        card.child(back);
        UiFactory.centerInRoot(root, card, 8);
    }

    private void option(FlowLayout card, String label, String tooltip, Runnable action) {
        var button =
                UiFactory.button(ItemEditorText.tr(label), UiFactory.ButtonTextPreset.LARGE, ignored -> action.run());
        button.horizontalSizing(Sizing.fill(100));
        button.active(!this.importBusy() && !this.pickerOpen);
        button.tooltip(List.of(ItemEditorText.tr(tooltip)));
        card.child(button);
    }

    boolean importBusy() {
        return this.saving || this.pending != null;
    }

    Component importStatus() {
        return this.status;
    }

    void cancelPreview() {
        this.cancelImport();
        this.minecraft.setScreenAndShow(this);
    }

    private void refresh() {
        if (this.root != null) this.build(this.root);
    }

    private void openFileDialog() {
        if (this.importBusy() || this.pickerOpen) return;
        this.pickerOpen = true;
        AtomicBoolean cancelled = new AtomicBoolean();
        this.pending = cancelled;
        RegistryAccess access = this.registryAccess();
        var fixer = this.minecraft.getFixerUpper();
        this.setStatus(ItemEditorText.tr("import.file_picker_opening"), UiColors.MUTED);
        this.refresh();
        NativeFileDialog.open(
                        ItemEditorText.str("import.file_dialog_title"),
                        this.minecraft.gameDirectory.toPath(),
                        ItemEditorText.str("import.file_dialog_filter"),
                        "nbt;snbt;json")
                .whenComplete((path, failure) -> this.minecraft.execute(() -> {
                    this.pickerOpen = false;
                    this.refresh();
                }))
                .thenApplyAsync(path -> {
                    if (path == null || cancelled.get()) return null;
                    try {
                        return FileImportService.read(
                                path,
                                access,
                                fixer,
                                cancelled::get,
                                message -> this.minecraft.execute(() -> {
                                    if (this.pending == cancelled && !cancelled.get())
                                        this.setStatus(message, UiColors.MUTED);
                                }));
                    } catch (Exception | LinkageError failure) {
                        throw new CompletionException(failure);
                    }
                })
                .whenComplete((result, failure) -> this.minecraft.execute(() -> {
                    if (this.pending != cancelled || cancelled.get()) return;
                    this.pending = null;
                    if (failure != null) {
                        this.setStatus(
                                ItemEditorText.tr("import.failed", FileImportService.errorMessage(failure)),
                                UiColors.DANGER);
                    } else if (result == null) {
                        this.setStatus(ItemEditorText.tr("import.cancelled"), UiColors.MUTED);
                    } else {
                        this.preview = result;
                        this.duplicatePageId = null;
                        this.previewAccess = access;
                        this.setStatus(ItemEditorText.tr("import.preview_ready"), UiColors.MUTED);
                        this.minecraft.setScreenAndShow(new ImportPreviewScreen(this, result));
                        return;
                    }
                    this.refresh();
                }));
    }

    void confirmPreview(FileImportService.Preview edited) {
        if (this.importBusy()) return;
        this.preview = edited;
        this.duplicatePageId = null;
        this.confirmImport(false);
    }

    void confirmImport(boolean allowDuplicate) {
        if (this.preview == null || this.saving) return;
        if (this.preview.page() == null) {
            var items = this.preview.items();
            this.preview = null;
            if (items.hasManyStacks())
                this.minecraft.setScreenAndShow(new ImportedItemsScreen(this.minecraft, this, items.stacks()));
            else
                this.minecraft.setScreenAndShow(new ItemEditorScreen(
                        new ItemEditorSession(this.minecraft, items.stack(), ItemEditorSessionOrigin.IMPORTED)));
            return;
        }
        this.saving = true;
        this.setStatus(ItemEditorText.tr("import.saving"), UiColors.MUTED);
        this.refresh();
        StorageServices.savedItems()
                .enqueueImportPages(
                        List.of(this.preview.page()),
                        this.previewAccess,
                        progress -> this.minecraft.execute(() -> this.setStatus(
                                ItemEditorText.tr("import.saving_items", progress.items()), UiColors.MUTED)),
                        true,
                        !allowDuplicate)
                .whenComplete((result, failure) -> this.minecraft.execute(() -> {
                    this.saving = false;
                    if (failure == null && !result.duplicates().isEmpty()) {
                        var duplicate = result.duplicates().getFirst();
                        this.duplicatePageId = duplicate.id();
                        this.setStatus(
                                ItemEditorText.tr("import.duplicate_page", duplicate.pageNumber()), UiColors.DANGER);
                        this.minecraft.setScreenAndShow(new ImportDuplicateScreen(
                                this.minecraft.gui.screen(),
                                this,
                                ItemEditorText.tr("import.duplicate_prompt", duplicate.pageNumber())));
                        return;
                    }
                    if (failure == null && !result.createdPageIds().isEmpty()) {
                        this.preview = null;
                        this.setStatus(
                                ItemEditorText.tr("storage.pages.imported_page", result.items()), UiColors.SUCCESS);
                        this.openPage(result.createdPageIds().getFirst());
                        return;
                    }
                    this.setStatus(
                            failure == null
                                    ? ItemEditorText.tr("storage.pages.imported_page", result.items())
                                    : ItemEditorText.tr("import.failed", FileImportService.errorMessage(failure)),
                            failure == null ? UiColors.SUCCESS : UiColors.DANGER);
                    this.refresh();
                }));
    }

    void openExisting() {
        if (this.duplicatePageId == null || this.importBusy()) return;
        this.openPage(this.duplicatePageId);
    }

    private void openPage(String pageId) {
        AtomicBoolean cancelled = new AtomicBoolean();
        this.pending = cancelled;
        this.refresh();
        StorageServices.savedItems()
                .findPageByIdAsync(pageId)
                .whenComplete((page, failure) -> this.minecraft.execute(() -> {
                    if (this.pending != cancelled || cancelled.get()) return;
                    this.pending = null;
                    if (failure != null) {
                        this.setStatus(
                                ItemEditorText.tr("import.failed", FileImportService.errorMessage(failure)),
                                UiColors.DANGER);
                    } else if (page.isEmpty()) {
                        this.duplicatePageId = null;
                        this.setStatus(ItemEditorText.tr("import.existing_missing"), UiColors.DANGER);
                    } else {
                        this.preview = null;
                        this.previewAccess = null;
                        this.duplicatePageId = null;
                        this.refresh();
                        this.minecraft.setScreenAndShow(new StorageScreen(
                                page.get().pageNumber(),
                                "",
                                StorageSortMode.REGULAR,
                                StorageScreenMode.COPY_IMPORT,
                                this.returnScreen,
                                null));
                        return;
                    }
                    this.refresh();
                }));
    }

    private void cancelImport() {
        if (this.saving) return;
        if (this.pending != null) this.pending.set(true);
        this.pending = null;
        this.preview = null;
        this.previewAccess = null;
        this.duplicatePageId = null;
        this.setStatus(ItemEditorText.tr("import.cancelled"), UiColors.MUTED);
        this.refresh();
    }

    private void setStatus(Component message, int color) {
        this.status = message;
        this.statusColor = color;
        if (this.statusLabel != null) {
            this.statusLabel.text(message);
            this.statusLabel.color(Color.ofRgb(color));
        }
    }

    private RegistryAccess registryAccess() {
        return this.minecraft.level == null ? RegistryAccess.EMPTY : this.minecraft.level.registryAccess();
    }

    @Override
    public void removed() {
        if (this.pending != null) this.pending.set(true);
        this.pending = null;
        super.removed();
    }

    @Override
    public void onClose() {
        if (this.saving) return;
        this.cancelImport();
        this.minecraft.setScreenAndShow(this.returnScreen);
    }
}
