package me.noramibu.itemeditor.ui.screen;

import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.StackLayout;
import io.wispforest.owo.ui.container.UIContainers;
import io.wispforest.owo.ui.core.HorizontalAlignment;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.OwoUIAdapter;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.Surface;
import io.wispforest.owo.ui.core.VerticalAlignment;
import me.noramibu.itemeditor.storage.StorageSortMode;
import me.noramibu.itemeditor.ui.component.UiFactory;
import me.noramibu.itemeditor.util.ItemEditorText;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;

public final class ItemEntryScreen extends BaseOwoScreen<StackLayout> {
    private static final int ROOT_BLUR_RADIUS = 4;
    private static final int ROOT_BLUR_QUALITY = 8;
    private static final int ROOT_SURFACE_TINT = 0x6610151A;
    private static final int CARD_WIDTH = 250;

    private final Minecraft minecraft;

    public ItemEntryScreen(Minecraft minecraft) {
        super(ItemEditorText.tr("entry.title"));
        this.minecraft = minecraft;
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
        card.gap(UiFactory.scaledPixels(6));
        card.child(UiFactory.title(ItemEditorText.tr("entry.title")));
        card.child(UiFactory.muted(ItemEditorText.tr("entry.subtitle"), UiFactory.scaledPixels(220)));

        boolean canCreate = this.canCreateItems();
        var createButton = UiFactory.button(ItemEditorText.tr("entry.create"), UiFactory.ButtonTextPreset.LARGE, button -> {
            if (!this.canCreateItems()) {
                this.showCreativeRequired();
                return;
            }
            this.minecraft.setScreen(new ItemPickerScreen(this.minecraft, new ItemEntryScreen(this.minecraft)));
        });
        createButton.horizontalSizing(Sizing.fill(100));
        createButton.active(canCreate);
        card.child(createButton);

        var storageButton = UiFactory.button(ItemEditorText.tr("entry.storage"), UiFactory.ButtonTextPreset.LARGE, button ->
                this.minecraft.setScreen(new StorageScreen(1, "", StorageSortMode.REGULAR, StorageScreenMode.PICK_FOR_EDIT, new ItemEntryScreen(this.minecraft)))
        );
        storageButton.horizontalSizing(Sizing.fill(100));
        card.child(storageButton);

        var importButton = UiFactory.button(ItemEditorText.tr("entry.import"), UiFactory.ButtonTextPreset.LARGE, button -> {
            if (!this.canCreateItems()) {
                this.showCreativeRequired();
                return;
            }
            this.minecraft.setScreen(new ImportScreen(this.minecraft, new ItemEntryScreen(this.minecraft)));
        });
        importButton.horizontalSizing(Sizing.fill(100));
        importButton.active(canCreate);
        card.child(importButton);

        if (!canCreate) {
            card.child(UiFactory.message(ItemEditorText.tr("launcher.creation_requires_creative"), 0xFF8A8A, 0.9F)
                    .maxWidth(UiFactory.scaledPixels(220)));
        }

        var cancel = UiFactory.button(ItemEditorText.tr("common.cancel"), UiFactory.ButtonTextPreset.STANDARD, button -> this.minecraft.setScreen(null));
        cancel.horizontalSizing(Sizing.fill(100));
        card.child(cancel);

        FlowLayout centered = UiFactory.column();
        centered.horizontalSizing(Sizing.fill(100));
        centered.verticalSizing(Sizing.fill(100));
        centered.padding(Insets.of(8));
        centered.horizontalAlignment(HorizontalAlignment.CENTER);
        centered.verticalAlignment(VerticalAlignment.CENTER);
        centered.child(card);
        root.child(centered);
    }

    private boolean canCreateItems() {
        return this.minecraft.hasSingleplayerServer()
                || (this.minecraft.player != null && this.minecraft.player.hasInfiniteMaterials());
    }

    private void showCreativeRequired() {
        if (this.minecraft.player == null) {
            return;
        }
        this.minecraft.player.displayClientMessage(
                ItemEditorText.tr("launcher.creation_requires_creative").copy().withStyle(ChatFormatting.RED),
                true
        );
    }
}
