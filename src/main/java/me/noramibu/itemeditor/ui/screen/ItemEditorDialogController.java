package me.noramibu.itemeditor.ui.screen;

import io.wispforest.owo.ui.container.FlowLayout;
import me.noramibu.itemeditor.editor.ItemEditorSession;
import me.noramibu.itemeditor.ui.component.ColorPickerDialog;
import me.noramibu.itemeditor.ui.component.ConfirmationDialog;
import me.noramibu.itemeditor.ui.component.GradientPickerDialog;
import me.noramibu.itemeditor.ui.component.RawItemDataDialog;
import me.noramibu.itemeditor.ui.component.RichTextTokenDialog;
import me.noramibu.itemeditor.ui.component.SearchablePickerDialog;
import me.noramibu.itemeditor.service.PostApplyVerificationService;
import me.noramibu.itemeditor.util.ItemEditorText;
import me.noramibu.itemeditor.util.RawItemDataUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.regex.Pattern;

final class ItemEditorDialogController {
    private static final Pattern INVALID_EXPORT_NAME_CHARS = Pattern.compile("[^a-zA-Z0-9._-]");
    private static final DateTimeFormatter EXPORT_TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final String EXPORT_DIRECTORY = "itemeditor/exports";
    private static final String DEFAULT_EXPORT_BASENAME = "item-data";
    private static final String UNKNOWN_ERROR_MESSAGE = "unknown error";
    private static final String EXPORT_PREFIX_CURRENT_ITEM = "current-item";
    private static final String EXPORT_PREFIX_ORIGINAL_ITEM = "original-item";
    private static final String EXPORT_EXTENSION_NBT = "nbt";
    private static final String EXPORT_EXTENSION_JSON = "json";
    private static final String RAW_DIFF_HELP_TEXT = "Green background = added/changed, Red background = removed";
    private static final String DIFF_LINE_UPDATED_PREFIX = "~ ";
    private static final String DIFF_LINE_UNCHANGED_PREFIX = "  ";
    private static final String DIFF_LINE_REMOVED_PREFIX = "- ";
    private static final String DIFF_LINE_ADDED_PREFIX = "+ ";
    private static final int RAW_DIFF_LOOKAHEAD = 24;
    private static final int RAW_DIFF_BG_UNCHANGED = 0x00000000;
    private static final int RAW_DIFF_BG_ADDED_OR_CHANGED = 0x33276735;
    private static final int RAW_DIFF_BG_REMOVED = 0x334C1F2A;

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
                    this.session().reset();
                    this.screen.refreshCurrentPanel();
                },
                ItemEditorText.str("common.keep_editing"),
                this::clearDialog
        );
    }

    void requestApply() {
        if (this.session().hasErrors()) {
            return;
        }

        ItemStack preview = this.session().previewStack();
        String originalRaw = RawItemDataUtil.serialize(this.session().originalStack(), this.session().registryAccess());
        String currentRaw = this.currentRawForDialog(this.session());
        String body = this.screen.applyModeText();
        if (!body.isBlank()) {
            body += "\n";
        }
        body += RAW_DIFF_HELP_TEXT;
        this.showDialog(RawItemDataDialog.createConfirmation(
                ItemEditorText.str("dialog.apply.title"),
                body,
                this.buildRawDiffLines(originalRaw, currentRaw),
                ItemEditorText.tr("common.save_apply"),
                () -> this.performApply(preview.copy()),
                ItemEditorText.tr("common.cancel"),
                this::clearDialog
        ), () -> this.performApply(preview.copy()));
    }

    void requestClose() {
        if (this.shouldConfirmDiscardOnClose()) {
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

    private boolean shouldConfirmDiscardOnClose() {
        ItemEditorSession session = this.session();
        return session.dirty() || session.hasErrors();
    }

    boolean shouldCloseOnEsc() {
        return this.screen.isDialogClosed();
    }

    boolean handleDialogShortcut(KeyEvent input) {
        if (this.screen.isDialogClosed()) {
            return false;
        }
        if (!input.hasControlDownWithQuirk() || input.key() != GLFW.GLFW_KEY_S) {
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
                color -> this.clearThen(() -> onApply.accept(color)),
                this::clearDialog
        ));
    }

    void openGradientPickerDialog(String title, int initialStartRgb, int initialEndRgb, BiConsumer<Integer, Integer> onApply) {
        this.showDialog(GradientPickerDialog.create(
                title,
                initialStartRgb,
                initialEndRgb,
                (startColor, endColor) -> this.clearThen(() -> onApply.accept(startColor, endColor)),
                this::clearDialog
        ));
    }

    void openRichTextHeadDialog(String title, Consumer<String> onApply) {
        this.showDialog(RichTextTokenDialog.createHead(
                title,
                token -> this.clearThen(() -> onApply.accept(token)),
                this::clearDialog
        ));
    }

    void openRichTextSpriteDialog(String title, Consumer<String> onApply) {
        this.showDialog(RichTextTokenDialog.createSprite(
                title,
                token -> this.clearThen(() -> onApply.accept(token)),
                this::clearDialog
        ));
    }

    void openRichTextEventDialog(String title, boolean includeHoverModes, boolean includeSuggestCommand, String initialText, Consumer<String> onApply) {
        this.showDialog(RichTextTokenDialog.createEvent(
                title,
                includeHoverModes,
                includeSuggestCommand,
                initialText,
                token -> this.clearThen(() -> onApply.accept(token)),
                this::clearDialog
        ));
    }

    void openRawItemDataDialog(String title, boolean previewData) {
        ItemEditorSession session = this.session();
        ItemStack originalStack = session.originalStack();
        String originalRaw = RawItemDataUtil.serialize(originalStack, session.registryAccess());
        String currentRaw = this.currentRawForDialog(session);
        String rawData = previewData ? currentRaw : originalRaw;
        String jsonData = previewData
                ? this.currentJsonForDialog(session, currentRaw)
                : RawItemDataUtil.serializeJson(originalStack, session.registryAccess());
        String exportPrefix = previewData ? EXPORT_PREFIX_CURRENT_ITEM : EXPORT_PREFIX_ORIGINAL_ITEM;
        List<RawItemDataDialog.Line> lines = previewData
                ? this.buildRawDiffLines(originalRaw, currentRaw)
                : this.toNeutralRawLines(rawData);
        String body = previewData ? RAW_DIFF_HELP_TEXT : "";
        this.showDialog(RawItemDataDialog.create(
                title,
                body,
                lines,
                () -> {
                    Minecraft minecraft = this.minecraft();
                    if (minecraft != null) {
                        minecraft.keyboardHandler.setClipboard(rawData);
                    }
                },
                () -> this.exportRawData(exportPrefix, EXPORT_EXTENSION_NBT, rawData),
                () -> this.exportRawData(exportPrefix, EXPORT_EXTENSION_JSON, jsonData),
                this::clearDialog
        ));
    }

    private String currentRawForDialog(ItemEditorSession session) {
        if (session.state().rawEditorEdited) {
            String raw = session.state().rawEditorText;
            if (raw != null && !raw.isBlank()) {
                return raw;
            }
        }
        return RawItemDataUtil.serialize(session.previewStack(), session.registryAccess());
    }

    private String currentJsonForDialog(ItemEditorSession session, String currentRaw) {
        if (session.state().rawEditorEdited) {
            RawItemDataUtil.ParseResult parsed = RawItemDataUtil.parse(currentRaw, session.registryAccess());
            if (parsed.success()) {
                return RawItemDataUtil.serializeJson(parsed.stack(), session.registryAccess());
            }
            return currentRaw;
        }
        return RawItemDataUtil.serializeJson(session.previewStack(), session.registryAccess());
    }

    private List<RawItemDataDialog.Line> toNeutralRawLines(String rawData) {
        List<RawItemDataDialog.Line> lines = new ArrayList<>();
        for (String line : rawData.split("\\R", -1)) {
            lines.add(new RawItemDataDialog.Line(line, RAW_DIFF_BG_UNCHANGED));
        }
        return lines;
    }

    private List<RawItemDataDialog.Line> buildRawDiffLines(String originalRaw, String currentRaw) {
        String[] original = originalRaw.split("\\R", -1);
        String[] current = currentRaw.split("\\R", -1);
        int n = original.length;
        int m = current.length;

        List<RawItemDataDialog.Line> lines = new ArrayList<>();
        int i = 0;
        int j = 0;
        while (i < n && j < m) {
            if (this.sameRawDiffLine(original[i], current[j])) {
                lines.add(new RawItemDataDialog.Line(DIFF_LINE_UNCHANGED_PREFIX + current[j], RAW_DIFF_BG_UNCHANGED));
                i++;
                j++;
                continue;
            }

            int nextOriginal = this.findMatchingRawLine(original, i + 1, current[j]);
            int nextCurrent = this.findMatchingRawLine(current, j + 1, original[i]);
            if (nextOriginal >= 0 && (nextCurrent < 0 || nextOriginal - i <= nextCurrent - j)) {
                while (i < nextOriginal) {
                    lines.add(new RawItemDataDialog.Line(DIFF_LINE_REMOVED_PREFIX + original[i], RAW_DIFF_BG_REMOVED));
                    i++;
                }
                continue;
            }
            if (nextCurrent >= 0) {
                while (j < nextCurrent) {
                    lines.add(new RawItemDataDialog.Line(DIFF_LINE_ADDED_PREFIX + current[j], RAW_DIFF_BG_ADDED_OR_CHANGED));
                    j++;
                }
                continue;
            }

            lines.add(new RawItemDataDialog.Line(DIFF_LINE_UPDATED_PREFIX + current[j], RAW_DIFF_BG_ADDED_OR_CHANGED));
            i++;
            j++;
        }

        while (i < n) {
            lines.add(new RawItemDataDialog.Line(DIFF_LINE_REMOVED_PREFIX + original[i], RAW_DIFF_BG_REMOVED));
            i++;
        }
        while (j < m) {
            lines.add(new RawItemDataDialog.Line(DIFF_LINE_ADDED_PREFIX + current[j], RAW_DIFF_BG_ADDED_OR_CHANGED));
            j++;
        }
        return lines;
    }

    private int findMatchingRawLine(String[] lines, int start, String target) {
        int end = Math.min(lines.length, start + RAW_DIFF_LOOKAHEAD);
        for (int index = start; index < end; index++) {
            if (this.sameRawDiffLine(lines[index], target)) {
                return index;
            }
        }
        return -1;
    }

    private boolean sameRawDiffLine(String original, String current) {
        return Objects.equals(this.normalizedRawDiffLine(original), this.normalizedRawDiffLine(current));
    }

    private String normalizedRawDiffLine(String line) {
        String value = line == null ? "" : line;
        int end = value.length();
        while (end > 0 && Character.isWhitespace(value.charAt(end - 1))) {
            end--;
        }
        if (end > 0 && value.charAt(end - 1) == ',') {
            value = value.substring(0, end - 1) + value.substring(end);
        }
        return value.trim();
    }

    private void exportRawData(String baseName, String extension, String content) {
        Minecraft minecraft = this.minecraft();
        if (minecraft == null) {
            return;
        }

        try {
            Path exportDir = minecraft.gameDirectory.toPath().resolve(EXPORT_DIRECTORY);
            Files.createDirectories(exportDir);

            String safeBase = this.sanitizeFileName(baseName);
            String timestamp = EXPORT_TIMESTAMP_FORMATTER.format(LocalDateTime.now());
            Path file = exportDir.resolve(safeBase + "-" + timestamp + "." + extension);
            Files.writeString(file, content, StandardCharsets.UTF_8);

            this.sendSystemMessage(
                    minecraft,
                    ItemEditorText.str("dialog.raw_data.export_success", file.toString()),
                    ChatFormatting.GREEN
            );
        } catch (IOException | RuntimeException exception) {
            this.sendSystemMessage(
                    minecraft,
                    ItemEditorText.str("dialog.raw_data.export_failed", this.errorMessage(exception)),
                    ChatFormatting.RED
            );
        }
    }

    private String sanitizeFileName(String value) {
        if (value == null || value.isBlank()) {
            return DEFAULT_EXPORT_BASENAME;
        }
        String sanitized = INVALID_EXPORT_NAME_CHARS.matcher(value).replaceAll("-");
        if (sanitized.isBlank()) {
            return DEFAULT_EXPORT_BASENAME;
        }
        return sanitized;
    }

    private String errorMessage(Throwable exception) {
        String message = exception.getMessage();
        return message == null ? UNKNOWN_ERROR_MESSAGE : message;
    }

    private void sendSystemMessage(Minecraft minecraft, String message, ChatFormatting color) {
        if (minecraft.player == null) {
            return;
        }
        minecraft.player.displayClientMessage(Component.literal(message).withStyle(color), false);
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
                value -> this.clearThen(() -> {
                    onSelect.accept(value);
                    this.screen.refreshCurrentPanel();
                    this.screen.refreshPreview();
                }),
                this::clearDialog
        ));
    }

    private void showDialog(String title, String body, String confirmText, Runnable onConfirm, String cancelText, Runnable onCancel) {
        this.showDialog(ConfirmationDialog.create(title, body, confirmText, onConfirm, cancelText, onCancel));
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

        Minecraft minecraft = this.minecraft();
        int selectedSlot = minecraft.player != null ? minecraft.player.getInventory().getSelectedSlot() : -1;
        var result = this.session().apply();

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
        Minecraft minecraft = this.minecraft();
        if (minecraft != null) {
            minecraft.setScreen(null);
        }
    }

    private void clearThen(Runnable action) {
        this.clearDialog();
        action.run();
    }

    private ItemEditorSession session() {
        return this.screen.session();
    }

    private Minecraft minecraft() {
        return this.session().minecraft();
    }
}
