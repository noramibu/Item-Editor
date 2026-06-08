package me.noramibu.itemeditor.ui.screen;

import io.wispforest.owo.ui.base.BaseOwoScreen;
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
import me.noramibu.itemeditor.editor.ItemEditorSession;
import me.noramibu.itemeditor.service.ItemImportService;
import me.noramibu.itemeditor.storage.StorageSortMode;
import me.noramibu.itemeditor.ui.component.UiFactory;
import me.noramibu.itemeditor.util.ItemEditorText;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public final class ImportScreen extends BaseOwoScreen<StackLayout> {
    private static final int ROOT_BLUR_RADIUS = 4;
    private static final int ROOT_BLUR_QUALITY = 8;
    private static final int ROOT_SURFACE_TINT = 0x6610151A;
    private static final int CARD_WIDTH = 270;
    private final Minecraft minecraft;
    private final Screen returnScreen;
    private final ItemImportService importService = new ItemImportService();
    private LabelComponent statusLabel;

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
        root.clearChildren();
        root.surface(Surface.blur(ROOT_BLUR_RADIUS, ROOT_BLUR_QUALITY).and(Surface.flat(ROOT_SURFACE_TINT)));

        FlowLayout card = UiFactory.centeredCard(CARD_WIDTH);
        card.child(UiFactory.title(ItemEditorText.tr("import.title")));
        var paste = UiFactory.button(ItemEditorText.tr("import.paste_item"), UiFactory.ButtonTextPreset.LARGE,
                button -> this.minecraft.setScreen(new RawImportScreen(this.minecraft, this)));
        paste.horizontalSizing(Sizing.fill(100));
        card.child(paste);

        var file = UiFactory.button(ItemEditorText.tr("import.file"), UiFactory.ButtonTextPreset.LARGE, button -> this.openFileDialog());
        file.horizontalSizing(Sizing.fill(100));
        card.child(file);

        var storage = UiFactory.button(
                ItemEditorText.tr("import.storage"),
                UiFactory.ButtonTextPreset.LARGE,
                button -> this.minecraft.setScreen(new StorageScreen(
                        1,
                        "",
                        StorageSortMode.REGULAR,
                        StorageScreenMode.PICK_FOR_EDIT,
                        this
                ))
        );
        storage.horizontalSizing(Sizing.fill(100));
        card.child(storage);

        this.statusLabel = UiFactory.message(Component.literal(" "), 0xA9B5C0).maxWidth(UiFactory.scaledPixels(230));
        card.child(this.statusLabel);

        var back = UiFactory.button(ItemEditorText.tr("entry.back"), UiFactory.ButtonTextPreset.STANDARD, button -> this.minecraft.setScreen(this.returnScreen));
        back.horizontalSizing(Sizing.fill(100));
        card.child(back);

        FlowLayout centered = UiFactory.column();
        centered.horizontalSizing(Sizing.fill(100));
        centered.verticalSizing(Sizing.fill(100));
        centered.padding(Insets.of(8));
        centered.horizontalAlignment(HorizontalAlignment.CENTER);
        centered.verticalAlignment(VerticalAlignment.CENTER);
        centered.child(card);
        root.child(centered);
    }

    private void openFileDialog() {
        this.setStatus(ItemEditorText.tr("import.file_picker_opening"), 0xA9B5C0);
        CompletableFuture.runAsync(() -> {
            String path;
            try {
                try (MemoryStack stack = MemoryStack.stackPush()) {
                    PointerBuffer patterns = stack.mallocPointer(3);
                    patterns.put(stack.UTF8("*.nbt"));
                    patterns.put(stack.UTF8("*.snbt"));
                    patterns.put(stack.UTF8("*.json"));
                    patterns.flip();
                    path = TinyFileDialogs.tinyfd_openFileDialog(
                            ItemEditorText.str("import.file_dialog_title"),
                            this.minecraft.gameDirectory.toPath().toString(),
                            patterns,
                            "Item data (*.nbt, *.snbt, *.json)",
                            false
                    );
                }
            } catch (RuntimeException | LinkageError failure) {
                this.minecraft.execute(() -> this.showFilePickerFailure(failure));
                return;
            }
            if (path == null || path.isBlank()) {
                this.minecraft.execute(() -> this.setStatus(ItemEditorText.tr("import.cancelled"), 0xA9B5C0));
                return;
            }
            this.importService.importFile(Path.of(path), this.registryAccess(), this.minecraft.getFixerUpper())
                    .whenComplete((result, throwable) -> this.minecraft.execute(() -> this.handleImportResult(result, throwable)));
        });
    }

    private void handleImportResult(ItemImportService.ImportResult result, Throwable throwable) {
        if (throwable != null) {
            this.setStatus(Component.literal(ItemEditorText.str("import.failed", throwable.getMessage())), 0xFF8A8A);
            return;
        }
        if (result == null || !result.success()) {
            this.setStatus(Component.literal(result == null ? ItemEditorText.str("raw.unknown_error") : result.message()), 0xFF8A8A);
            return;
        }
        if (result.hasManyStacks()) {
            this.minecraft.setScreen(new ImportedItemsScreen(this.minecraft, this, result.stacks()));
            return;
        }
        this.minecraft.setScreen(new ItemEditorScreen(new ItemEditorSession(this.minecraft, result.stack())));
    }

    private void showFilePickerFailure(Throwable failure) {
        String message = ItemEditorText.str("import.file_picker_failed", failure.getMessage() == null ? failure.getClass().getSimpleName() : failure.getMessage());
        this.setStatus(Component.literal(message), 0xFF8A8A);
        if (this.minecraft.player != null) {
            this.minecraft.player.displayClientMessage(Component.literal(message).withStyle(ChatFormatting.RED), false);
        }
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

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.returnScreen);
    }
}
