package me.noramibu.itemeditor.ui.screen;

import io.wispforest.owo.ui.container.FlowLayout;
import me.noramibu.itemeditor.ui.component.ColorPickerDialog;
import me.noramibu.itemeditor.ui.component.ConfirmationDialog;
import me.noramibu.itemeditor.ui.component.GradientPickerDialog;
import me.noramibu.itemeditor.ui.component.ItemDiffDialog;
import me.noramibu.itemeditor.ui.component.RawItemDataDialog;
import me.noramibu.itemeditor.ui.component.SearchablePickerDialog;
import me.noramibu.itemeditor.service.PostApplyVerificationService;
import me.noramibu.itemeditor.util.ItemComponentDiffUtil;
import me.noramibu.itemeditor.util.ItemEditorText;
import me.noramibu.itemeditor.util.RawItemDataUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.BiConsumer;
import java.util.function.IntConsumer;

final class ItemEditorDialogController {

    private final ItemEditorScreen screen;
    private Runnable dialogConfirmShortcut;

    ItemEditorDialogController(ItemEditorScreen screen) {
        this.screen = screen;
    }

    void requestReset() {
        this.showDialog(
                ItemEditorText.str("dialog.reset.title"),
                "",
                ItemEditorText.str("common.reset"),
                () -> {
                    this.clearDialog();
                    this.screen.session().reset();
                    this.screen.refreshCurrentPanel();
                },
                ItemEditorText.str("common.keep_editing"),
                this::clearDialog
        );
    }

    void requestApply() {
        if (this.screen.session().hasErrors()) {
            return;
        }

        ItemStack original = this.screen.session().originalStack();
        ItemStack preview = this.screen.session().previewStack();
        ItemComponentDiffUtil.Result diff = ItemComponentDiffUtil.diff(
                original,
                preview,
                this.screen.session().registryAccess()
        );

        if (diff.error() != null) {
            this.showDialog(
                    ItemEditorText.str("dialog.apply.title"),
                    ItemEditorText.str("dialog.apply.diff_failed", diff.error()),
                    ItemEditorText.str("common.save_apply"),
                    () -> this.performApply(preview),
                    ItemEditorText.str("common.cancel"),
                    this::clearDialog,
                    () -> this.performApply(preview)
            );
            return;
        }

        this.showDialog(
                ItemDiffDialog.create(
                ItemEditorText.str("dialog.apply.title"),
                this.screen.applyModeText(),
                diff.entries(),
                () -> this.performApply(preview),
                this::clearDialog
        ),
                () -> this.performApply(preview)
        );
    }

    void requestClose() {
        if (this.screen.session().dirty()) {
            this.showDialog(
                    ItemEditorText.str("dialog.discard.title"),
                    "",
                    ItemEditorText.str("common.discard"),
                    this::closeWithoutPrompt,
                    ItemEditorText.str("common.stay"),
                    this::clearDialog
            );
        } else {
            this.closeWithoutPrompt();
        }
    }

    boolean shouldCloseOnEsc() {
        return !this.screen.hasActiveDialog();
    }

    boolean handleDialogShortcut(KeyEvent input) {
        if (!this.screen.hasActiveDialog()) {
            return false;
        }
        if (!input.hasControlDown() || input.key() != GLFW.GLFW_KEY_S) {
            return false;
        }
        if (this.dialogConfirmShortcut == null) {
            return false;
        }
        this.dialogConfirmShortcut.run();
        return true;
    }

    void openColorPickerDialog(String title, int initialRgb, IntConsumer onApply) {
        this.showDialog(ColorPickerDialog.create(
                title,
                initialRgb,
                color -> {
                    this.clearDialog();
                    onApply.accept(color);
                },
                this::clearDialog
        ));
    }

    void openGradientPickerDialog(String title, int initialStartRgb, int initialEndRgb, BiConsumer<Integer, Integer> onApply) {
        this.showDialog(GradientPickerDialog.create(
                title,
                initialStartRgb,
                initialEndRgb,
                (startColor, endColor) -> {
                    this.clearDialog();
                    onApply.accept(startColor, endColor);
                },
                this::clearDialog
        ));
    }

    void openRawItemDataDialog(String title, boolean previewData) {
        String rawData = previewData
                ? RawItemDataUtil.serialize(this.screen.session().previewStack(), this.screen.session().registryAccess())
                : RawItemDataUtil.serialize(this.screen.session().originalStack(), this.screen.session().registryAccess());
        this.showDialog(RawItemDataDialog.create(
                title,
                "",
                rawData,
                () -> {
                    if (this.screen.session().minecraft() != null) {
                        this.screen.session().minecraft().keyboardHandler.setClipboard(rawData);
                    }
                },
                this::clearDialog
        ));
    }

    void openSearchablePickerDialog(
            String title,
            String body,
            List<String> values,
            Function<String, String> labelMapper,
            Consumer<String> onSelect
    ) {
        this.showDialog(SearchablePickerDialog.create(
                title,
                body,
                values,
                labelMapper,
                value -> {
                    this.clearDialog();
                    onSelect.accept(value);
                    this.screen.refreshCurrentPanel();
                    this.screen.refreshPreview();
                },
                this::clearDialog
        ));
    }

    private void showDialog(String title, String body, String confirmText, Runnable onConfirm, String cancelText, Runnable onCancel) {
        this.showDialog(title, body, confirmText, onConfirm, cancelText, onCancel, null);
    }

    private void showDialog(
            String title,
            String body,
            String confirmText,
            Runnable onConfirm,
            String cancelText,
            Runnable onCancel,
            Runnable confirmShortcut
    ) {
        this.showDialog(ConfirmationDialog.create(title, body, confirmText, onConfirm, cancelText, onCancel), confirmShortcut);
    }

    private void showDialog(FlowLayout dialog) {
        this.showDialog(dialog, null);
    }

    private void showDialog(FlowLayout dialog, Runnable confirmShortcut) {
        this.clearDialog();
        this.dialogConfirmShortcut = confirmShortcut;
        this.screen.attachDialog(dialog);
    }

    private void showInfoDialog(String title, String body) {
        this.showDialog(title, body, ItemEditorText.str("common.close"), this::clearDialog, ItemEditorText.str("common.dismiss"), this::clearDialog);
    }

    private void performApply(ItemStack expectedPreview) {
        this.clearDialog();

        Minecraft minecraft = this.screen.session().minecraft();
        int selectedSlot = minecraft.player != null ? minecraft.player.getInventory().getSelectedSlot() : -1;
        var result = this.screen.session().apply();

        if (minecraft.player != null) {
            minecraft.player.displayClientMessage(
                    Component.literal(result.message()).withStyle(result.success() ? ChatFormatting.GREEN : ChatFormatting.RED),
                    true
            );
        }

        if (!result.success()) {
            this.showInfoDialog(ItemEditorText.str("dialog.apply_blocked.title"), result.message());
            return;
        }

        PostApplyVerificationService.schedule(minecraft, expectedPreview, selectedSlot, verification -> {
            if (minecraft.player == null || verification.matchesExpected() || verification.message().isBlank()) {
                return;
            }
            minecraft.player.displayClientMessage(
                    Component.literal(verification.message()).withStyle(ChatFormatting.YELLOW),
                    false
            );
        });

        this.closeWithoutPrompt();
    }

    private void clearDialog() {
        this.dialogConfirmShortcut = null;
        this.screen.clearDialog();
    }

    private void closeWithoutPrompt() {
        this.clearDialog();
        Minecraft.getInstance().setScreen(null);
    }
}
