package me.noramibu.itemeditor.ui.screen;

import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.CheckboxComponent;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.StackLayout;
import io.wispforest.owo.ui.container.UIContainers;
import io.wispforest.owo.ui.core.Color;
import io.wispforest.owo.ui.core.HorizontalAlignment;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.OwoUIAdapter;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.Surface;
import io.wispforest.owo.ui.core.VerticalAlignment;
import me.noramibu.itemeditor.service.ExternalStorageImportService;
import me.noramibu.itemeditor.storage.SavedItemStorageService;
import me.noramibu.itemeditor.storage.StorageServices;
import me.noramibu.itemeditor.storage.StorageSortMode;
import me.noramibu.itemeditor.ui.component.UiFactory;
import me.noramibu.itemeditor.util.ItemEditorText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public final class OtherModsImportScreen extends BaseOwoScreen<StackLayout> {

    private static final int ROOT_BLUR_RADIUS = 4;
    private static final int ROOT_BLUR_QUALITY = 8;
    private static final int ROOT_SURFACE_TINT = 0x6610151A;
    private static final int CARD_WIDTH = 330;
    private static final int COLOR_MUTED = 0xA9B5C0;
    private static final int COLOR_GOOD = 0x7ED67A;
    private static final int COLOR_DANGER = 0xFF8A8A;

    private final Minecraft minecraft;
    private final int returnPage;
    private final String returnQuery;
    private final StorageSortMode returnSortMode;
    private final StorageScreenMode returnMode;
    private final Screen returnScreen;
    private final SavedItemStorageService storage = StorageServices.savedItems();
    private final ExternalStorageImportService importService = new ExternalStorageImportService();
    private boolean importNbtEditor = true;
    private boolean importLibrarian = true;
    private int nbtEditorPages;
    private int librarianPages;
    private boolean importRunning;
    private LabelComponent nbtEditorLabel;
    private LabelComponent librarianLabel;
    private LabelComponent statusLabel;

    public OtherModsImportScreen(
            Minecraft minecraft,
            int returnPage,
            String returnQuery,
            StorageSortMode returnSortMode,
            StorageScreenMode returnMode,
            Screen returnScreen
    ) {
        super(ItemEditorText.tr("storage.import_other_mods.title"));
        this.minecraft = minecraft;
        this.returnPage = Math.max(1, returnPage);
        this.returnQuery = returnQuery == null ? "" : returnQuery;
        this.returnSortMode = returnSortMode == null ? StorageSortMode.REGULAR : returnSortMode;
        this.returnMode = returnMode == null ? StorageScreenMode.MANAGE : returnMode;
        this.returnScreen = returnScreen;
    }

    @Override
    protected @NotNull OwoUIAdapter<StackLayout> createAdapter() {
        return OwoUIAdapter.create(this, UIContainers::stack);
    }

    @Override
    protected void build(StackLayout root) {
        root.clearChildren();
        root.surface(Surface.blur(ROOT_BLUR_RADIUS, ROOT_BLUR_QUALITY).and(Surface.flat(ROOT_SURFACE_TINT)));

        FlowLayout card = UiFactory.centeredCard(CARD_WIDTH);
        card.child(UiFactory.title(ItemEditorText.tr("storage.import_other_mods.title")));
        card.child(UiFactory.muted(ItemEditorText.tr("storage.import_other_mods.help"), UiFactory.scaledPixels(280)));

        CheckboxComponent nbtEditor = UiFactory.checkbox(
                ItemEditorText.tr("storage.import_other_mods.nbt_editor"),
                this.importNbtEditor,
                checked -> this.importNbtEditor = checked
        );
        card.child(nbtEditor);
        this.nbtEditorLabel = UiFactory.muted(Component.literal(" "), UiFactory.scaledPixels(280));
        card.child(this.nbtEditorLabel);

        CheckboxComponent librarian = UiFactory.checkbox(
                ItemEditorText.tr("storage.import_other_mods.librarian"),
                this.importLibrarian,
                checked -> this.importLibrarian = checked
        );
        card.child(librarian);
        this.librarianLabel = UiFactory.muted(Component.literal(" "), UiFactory.scaledPixels(280));
        card.child(this.librarianLabel);

        card.child(UiFactory.actionButtonRow(
                UiFactory.positiveButton(ItemEditorText.tr("storage.import_other_mods.import_selected"), UiFactory.ButtonTextPreset.STANDARD, button -> this.importSelected()),
                UiFactory.button(ItemEditorText.tr("common.cancel"), UiFactory.ButtonTextPreset.STANDARD, button -> this.openPages())
        ));

        this.statusLabel = UiFactory.message(Component.literal(" "), COLOR_MUTED).maxWidth(UiFactory.scaledPixels(280));
        card.child(this.statusLabel);

        FlowLayout centered = UiFactory.column();
        centered.horizontalSizing(Sizing.fill(100));
        centered.verticalSizing(Sizing.fill(100));
        centered.padding(Insets.of(8));
        centered.horizontalAlignment(HorizontalAlignment.CENTER);
        centered.verticalAlignment(VerticalAlignment.CENTER);
        centered.child(card);
        root.child(centered);
        this.refreshScan();
    }

    private void refreshScan() {
        this.setStatus(ItemEditorText.tr("storage.import_other_mods.scanning"), COLOR_MUTED);
        this.importService.scan(this.minecraft).whenComplete((result, throwable) -> this.minecraft.execute(() -> {
            if (throwable != null || result == null) {
                this.setStatus(ItemEditorText.tr("storage.import_other_mods.scan_failed"), COLOR_DANGER);
                return;
            }
            this.nbtEditorPages = result.nbtEditorPages();
            this.librarianPages = result.librarianPages();
            this.updateSourceLabels();
            this.setStatus(Component.literal(" "), COLOR_MUTED);
        }));
    }

    private void updateSourceLabels() {
        if (this.nbtEditorLabel != null) {
            this.nbtEditorLabel.text(Component.literal(ItemEditorText.str(
                    "storage.import_other_mods.source_count",
                    this.nbtEditorPages
            )));
        }
        if (this.librarianLabel != null) {
            this.librarianLabel.text(Component.literal(ItemEditorText.str(
                    "storage.import_other_mods.source_count",
                    this.librarianPages
            )));
        }
    }

    private void importSelected() {
        if (!this.importNbtEditor && !this.importLibrarian) {
            this.setStatus(ItemEditorText.tr("storage.import_other_mods.none_selected"), COLOR_DANGER);
            return;
        }
        this.importRunning = true;
        this.setStatus(ItemEditorText.tr("storage.import_other_mods.importing"), COLOR_MUTED);
        this.importService
                .readImports(
                        this.minecraft,
                        this.registryAccess(),
                        this.importNbtEditor,
                        this.importLibrarian,
                        this::showReadProgress
                )
                .thenCompose(read -> this.storage.enqueueImportPages(
                                read.pages(),
                                this.registryAccess(),
                                this::showSaveProgress
                        )
                        .thenApply(saved -> new ImportUiResult(read, saved)))
                .whenComplete((result, throwable) -> this.minecraft.execute(() -> this.handleImportResult(result, throwable)));
    }

    private void handleImportResult(ImportUiResult result, Throwable throwable) {
        this.importRunning = false;
        if (throwable != null || result == null) {
            this.setStatus(Component.literal(ItemEditorText.str(
                    "storage.import_other_mods.failed",
                    throwable == null ? "unknown error" : throwable.getMessage()
            )), COLOR_DANGER);
            return;
        }
        this.storage.flushQueuedWrites();
        this.setStatus(Component.literal(ItemEditorText.str(
                "storage.import_other_mods.imported",
                result.saved().pages(),
                result.saved().items(),
                result.read().warnings().size()
        )), result.saved().pages() > 0 ? COLOR_GOOD : COLOR_DANGER);
    }

    private void showReadProgress(ExternalStorageImportService.ProgressUpdate progress) {
        if (progress == null) {
            return;
        }
        this.minecraft.execute(() -> {
            if (!this.importRunning) {
                return;
            }
            String message = switch (progress.phase()) {
                case "read_items" -> "Reading " + progress.source() + " page "
                        + progress.current() + "/" + Math.max(1, progress.total())
                        + " (" + progress.items() + " slots checked)";
                case "read_done" -> "Decoded " + progress.current() + " page(s), saving...";
                default -> "Reading " + progress.source() + " page "
                        + progress.current() + "/" + Math.max(1, progress.total());
            };
            this.setStatus(Component.literal(message), COLOR_MUTED);
        });
    }

    private void showSaveProgress(SavedItemStorageService.StorageImportProgress progress) {
        if (progress == null) {
            return;
        }
        this.minecraft.execute(() -> {
            if (!this.importRunning) {
                return;
            }
            String message = switch (progress.phase()) {
                case "save_items" -> "Saving imported page " + progress.current()
                        + "/" + Math.max(1, progress.total())
                        + " (" + progress.items() + " items saved)";
                case "finalize" -> "Finalizing storage index...";
                default -> "Saving imported page " + progress.current() + "/" + Math.max(1, progress.total());
            };
            this.setStatus(Component.literal(message), COLOR_MUTED);
        });
    }

    private void setStatus(Component message, int color) {
        if (this.statusLabel == null) {
            return;
        }
        this.statusLabel.text(message == null ? Component.literal(" ") : message);
        this.statusLabel.color(Color.ofRgb(color));
    }

    private RegistryAccess registryAccess() {
        return this.minecraft.level == null ? RegistryAccess.EMPTY : this.minecraft.level.registryAccess();
    }

    private void openPages() {
        this.minecraft.setScreen(new StoragePagesScreen(
                this.minecraft,
                this.returnPage,
                this.returnQuery,
                this.returnSortMode,
                this.returnMode,
                this.returnScreen
        ));
    }

    @Override
    public void onClose() {
        this.openPages();
    }

    private record ImportUiResult(
            ExternalStorageImportService.ImportReadResult read,
            SavedItemStorageService.StorageImportResult saved
    ) {
    }
}
