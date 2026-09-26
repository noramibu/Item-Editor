package me.noramibu.itemeditor.ui.screen;

import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.container.StackLayout;
import io.wispforest.owo.ui.container.UIContainers;
import io.wispforest.owo.ui.core.OwoUIAdapter;
import io.wispforest.owo.ui.core.Sizing;
import me.noramibu.itemeditor.ui.component.UiFactory;
import me.noramibu.itemeditor.ui.util.MenuBackgroundSurface;
import me.noramibu.itemeditor.ui.util.UiColors;
import me.noramibu.itemeditor.util.ItemEditorText;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

final class ImportDuplicateScreen extends BaseOwoScreen<StackLayout> {
    private final Screen preview;
    private final ImportScreen owner;
    private final Component message;

    ImportDuplicateScreen(Screen preview, ImportScreen owner, Component message) {
        super(ItemEditorText.tr("import.duplicate_title"));
        this.preview = preview;
        this.owner = owner;
        this.message = message;
    }

    @Override
    protected OwoUIAdapter<StackLayout> createAdapter() {
        return OwoUIAdapter.create(this, UIContainers::stack);
    }

    @Override
    protected void build(StackLayout root) {
        root.surface(MenuBackgroundSurface.standard());
        var card = UiFactory.centeredCard(270);
        card.child(UiFactory.title(this.title));
        card.child(UiFactory.message(this.message, UiColors.MUTED).maxWidth(UiFactory.scaledPixels(250)));
        String[] keys = {"import.open_existing", "import.confirm_duplicate", "entry.back"};
        Runnable[] actions = {this.owner::openExisting, () -> this.owner.confirmImport(true), () -> {}};
        for (int i = 0; i < keys.length; i++) {
            Runnable action = actions[i];
            var button = UiFactory.button(ItemEditorText.tr(keys[i]), UiFactory.ButtonTextPreset.STANDARD, ignored -> {
                this.minecraft.setScreen(this.preview);
                action.run();
            });
            button.horizontalSizing(Sizing.fill(100));
            card.child(button);
        }
        UiFactory.centerInRoot(root, card, 8);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.preview);
    }
}
