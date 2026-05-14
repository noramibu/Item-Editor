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
import me.noramibu.itemeditor.ui.component.RawTextAreaComponent;
import me.noramibu.itemeditor.ui.component.UiFactory;
import me.noramibu.itemeditor.util.ItemEditorText;
import me.noramibu.itemeditor.util.RawItemDataUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

public final class RawImportScreen extends BaseOwoScreen<StackLayout> {
    private static final int ROOT_BLUR_RADIUS = 4;
    private static final int ROOT_BLUR_QUALITY = 8;
    private static final int ROOT_SURFACE_TINT = 0x6610151A;

    private final Minecraft minecraft;
    private final Screen returnScreen;
    private final ItemImportService importService = new ItemImportService();
    private RawTextAreaComponent editor;
    private LabelComponent statusLabel;

    public RawImportScreen(Minecraft minecraft, Screen returnScreen) {
        super(ItemEditorText.tr("raw_import.title"));
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

        FlowLayout shell = UiFactory.card();
        shell.horizontalSizing(Sizing.fixed(Math.max(280, this.width - 24)));
        shell.verticalSizing(Sizing.fixed(Math.max(180, this.height - 24)));
        shell.gap(UiFactory.scaledPixels(5));

        FlowLayout header = UiFactory.row();
        header.child(UiFactory.title(ItemEditorText.tr("raw_import.title")).horizontalSizing(Sizing.expand(100)));
        header.child(UiFactory.button(ItemEditorText.tr("common.cancel"), UiFactory.ButtonTextPreset.STANDARD, button -> this.minecraft.setScreen(this.returnScreen)));
        shell.child(header);

        this.editor = new RawTextAreaComponent(Sizing.fill(100), Sizing.expand(100), "");
        shell.child(this.editor);

        this.statusLabel = UiFactory.message(Component.literal(" "), 0xA9B5C0).maxWidth(Math.max(100, this.width - 60));
        shell.child(this.statusLabel);

        FlowLayout actions = UiFactory.row();
        actions.child(UiFactory.button(ItemEditorText.tr("common.save_apply"), UiFactory.ButtonTextPreset.STANDARD, button -> this.importText()));
        actions.child(UiFactory.button(ItemEditorText.tr("dialog.apply.raw.format"), UiFactory.ButtonTextPreset.STANDARD, button -> this.formatText()));
        actions.child(UiFactory.button(ItemEditorText.tr("dialog.apply.raw.minify"), UiFactory.ButtonTextPreset.STANDARD, button -> this.minifyText()));
        actions.child(UiFactory.button(ItemEditorText.tr("common.cancel"), UiFactory.ButtonTextPreset.STANDARD, button -> this.minecraft.setScreen(this.returnScreen)));
        shell.child(actions);

        FlowLayout centered = UiFactory.column();
        centered.horizontalSizing(Sizing.fill(100));
        centered.verticalSizing(Sizing.fill(100));
        centered.padding(Insets.of(8));
        centered.horizontalAlignment(HorizontalAlignment.CENTER);
        centered.verticalAlignment(VerticalAlignment.CENTER);
        centered.child(shell);
        root.child(centered);
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        if (input.hasControlDownWithQuirk() && input.key() == GLFW.GLFW_KEY_S) {
            this.importText();
            return true;
        }
        return super.keyPressed(input);
    }

    private void importText() {
        RawItemDataUtil.ParseResult parsed = this.importService.parseText(this.editor.getValue(), this.registryAccess());
        if (!parsed.success()) {
            this.setStatus(Component.literal(ItemEditorText.str("import.parse_failed", parsed.error())), 0xFF8A8A);
            this.editor.setErrorLocation(parsed.line(), parsed.column(), 1);
            return;
        }
        if (parsed.stack().isEmpty()) {
            this.setStatus(ItemEditorText.tr("import.empty_item"), 0xFF8A8A);
            return;
        }
        this.minecraft.setScreen(new ItemEditorScreen(new ItemEditorSession(this.minecraft, parsed.stack())));
    }

    private void formatText() {
        RawItemDataUtil.ParseResult parsed = this.importService.parseText(this.editor.getValue(), this.registryAccess());
        if (!parsed.success()) {
            this.setStatus(Component.literal(ItemEditorText.str("import.parse_failed", parsed.error())), 0xFF8A8A);
            return;
        }
        this.editor.text(RawItemDataUtil.serialize(parsed.stack(), this.registryAccess()));
        this.setStatus(ItemEditorText.tr("raw_editor.status.valid"), 0x7ED67A);
    }

    private void minifyText() {
        this.editor.text(RawItemDataUtil.minify(this.editor.getValue()));
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
