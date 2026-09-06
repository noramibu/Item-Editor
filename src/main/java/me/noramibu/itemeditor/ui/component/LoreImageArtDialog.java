package me.noramibu.itemeditor.ui.component;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.DiscreteSliderComponent;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.component.TextBoxComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.Surface;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import javax.imageio.ImageIO;
import me.noramibu.itemeditor.util.ImageArtGenerationService;
import me.noramibu.itemeditor.util.ItemEditorText;
import me.noramibu.itemeditor.util.LoreImageArtUtil;
import me.noramibu.itemeditor.util.LoreImageArtUtil.ColorMode;
import me.noramibu.itemeditor.util.LoreImageArtUtil.FullColorDither;
import me.noramibu.itemeditor.util.LoreImageArtUtil.MaskMode;
import me.noramibu.itemeditor.util.RawItemDataUtil;
import me.noramibu.itemeditor.util.ValidationUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

public final class LoreImageArtDialog {
    private static final int DIALOG_GAP = 8;
    private static final int DIALOG_MIN_HEIGHT = 220;
    private static final int DEFAULT_WIDTH = 48;
    private static final int PREVIEW_OUTLINE = 0xFF445066;

    private final BiConsumer<List<Component>, Boolean> onApply;
    private final Runnable onClose;
    private final String keyPrefix;
    private final Minecraft minecraft = Minecraft.getInstance();
    private final ImageArtGenerationService generation = new ImageArtGenerationService(minecraft);
    private final FlowLayout options = UiFactory.column();
    private final FlowLayout preview = UiFactory.column().gap(0);
    private final FlowLayout previewArea = UiFactory.column();
    private final ScrollContainer<FlowLayout> previewScroll = DialogUiUtil.scrollContentExpand(preview);
    private final LabelComponent fileLabel = UiFactory.muted(ItemEditorText.tr("display.lore.image_art.no_file"));
    private final LabelComponent status = UiFactory.message("", 0xF2C26B);
    private final TextBoxComponent widthBox = UiFactory.textBox(String.valueOf(DEFAULT_WIDTH), ignored -> {});
    private String widthText = String.valueOf(DEFAULT_WIDTH);
    private ColorMode colorMode = ColorMode.FULL_COLOR;
    private int backgroundColor;
    private int textColor = 0xFFFFFF;
    private double threshold = 35;
    private double bilevelDither = 1;
    private boolean invert;
    private MaskMode maskMode = MaskMode.OPTIMAL;
    private double compression;
    private FullColorDither fullColorDither = FullColorDither.DISABLED;
    private boolean append;
    private ButtonComponent fullColorButton;
    private ButtonComponent bilevelButton;
    private ButtonComponent replaceButton;
    private ButtonComponent appendButton;
    private ButtonComponent applyButton;
    private LoreImageArtUtil.Generator generator;
    private boolean closed;
    private LoreImageArtUtil.Result result;

    private LoreImageArtDialog(
            BiConsumer<List<Component>, Boolean> onApply, Runnable onClose, String keyPrefix, int backgroundColor) {
        this.onApply = onApply;
        this.onClose = onClose;
        this.keyPrefix = keyPrefix;
        this.backgroundColor = backgroundColor & 0xFFFFFF;
    }

    public static LoreImageArtDialog create(BiConsumer<List<Component>, Boolean> onApply, Runnable onClose) {
        return new LoreImageArtDialog(onApply, onClose, "display.lore.image_art.", 0x110211);
    }

    public static LoreImageArtDialog createTextDisplay(
            int backgroundColor, BiConsumer<List<Component>, Boolean> onApply, Runnable onClose) {
        return new LoreImageArtDialog(onApply, onClose, "special.entity.display.image_art.", backgroundColor);
    }

    public FlowLayout build() {
        int dialogWidth = DialogUiUtil.dialogWidth(Integer.MAX_VALUE);
        int dialogHeight = DialogUiUtil.dialogHeight(Integer.MAX_VALUE, DIALOG_MIN_HEIGHT);
        FlowLayout overlay = DialogUiUtil.overlay(() -> {
            closed = true;
            generation.close();
        });
        FlowLayout dialog = DialogUiUtil.dialogCard(dialogWidth, dialogHeight, DIALOG_GAP);
        dialog.horizontalSizing(Sizing.fill(100));
        dialog.verticalSizing(Sizing.fill(100));
        dialog.child(UiFactory.title(ItemEditorText.tr(targetKey("title"))));

        FlowLayout content = UiFactory.column();
        content.padding(Insets.right(UiFactory.scaledScrollbarThickness(8) + 2));
        ButtonComponent choose = UiFactory.button(
                ItemEditorText.tr("display.lore.image_art.choose"),
                UiFactory.ButtonTextPreset.STANDARD,
                ignored -> openFile());
        choose.horizontalSizing(Sizing.fill(100));
        choose.tooltip(List.of(ItemEditorText.tr(targetKey("description"))));
        content.child(choose);
        content.child(fileLabel.horizontalSizing(Sizing.fill(100)));
        content.child(UiFactory.field(
                ItemEditorText.tr("display.lore.image_art.width"),
                Component.empty(),
                widthBox.horizontalSizing(Sizing.fixed(90))));

        fullColorButton = modeButton("display.lore.image_art.full_color", () -> {
            colorMode = ColorMode.FULL_COLOR;
            rebuildOptions();
            regenerate();
        });
        bilevelButton = modeButton("display.lore.image_art.bilevel", () -> {
            colorMode = ColorMode.BILEVEL;
            rebuildOptions();
            regenerate();
        });
        content.child(modeRow("display.lore.image_art.color_mode", fullColorButton, bilevelButton));
        content.child(options);

        updatePreviewSurface();
        content.child(status.horizontalSizing(Sizing.fill(100)));
        preview.padding(Insets.right(UiFactory.scaledScrollbarThickness(8) + 2));
        previewScroll.verticalSizing(Sizing.fill(100));
        previewArea.child(DialogUiUtil.vanillaScroll(
                InputSafeScrollContainer.horizontal(Sizing.fill(100), Sizing.expand(100), previewScroll), 16));
        var settings = DialogUiUtil.scrollContentExpand(content);
        FlowLayout body = new FlowLayout(Sizing.fill(100), Sizing.expand(100), container -> {
            boolean wide = container.width() >= UiFactory.scaledPixels(560);
            settings.horizontalSizing(
                    wide
                            ? Sizing.fixed(Math.min(container.width() / 3, UiFactory.scaledPixels(300)))
                            : Sizing.fill(100));
            settings.verticalSizing(wide ? Sizing.fill(100) : Sizing.fill(45));
            previewArea.horizontalSizing(wide ? Sizing.expand(100) : Sizing.fill(100));
            previewArea.verticalSizing(wide ? Sizing.fill(100) : Sizing.expand(100));
            (wide ? FlowLayout.Algorithm.HORIZONTAL : FlowLayout.Algorithm.VERTICAL).layout(container);
        }) {};
        body.gap(UiFactory.scaleProfile().spacing());
        body.child(settings);
        body.child(previewArea);
        dialog.child(body);

        replaceButton = modeButton(targetKey("replace"), () -> {
            append = false;
            updateInsertButtons();
        });
        appendButton = modeButton(targetKey("append_mode"), () -> {
            append = true;
            updateInsertButtons();
        });
        dialog.child(modeRow("display.lore.image_art.insert_mode", replaceButton, appendButton));
        updateInsertButtons();

        applyButton = UiFactory.positiveButton(
                ItemEditorText.tr(targetKey("insert")), UiFactory.ButtonTextPreset.STANDARD, ignored -> apply());
        applyButton.active(false);
        dialog.child(UiFactory.actionButtonRow(
                UiFactory.button(
                        ItemEditorText.tr("common.cancel"),
                        UiFactory.ButtonTextPreset.STANDARD,
                        ignored -> onClose.run()),
                applyButton));

        widthBox.onChanged().subscribe(value -> {
            widthText = value;
            regenerate();
        });
        rebuildOptions();
        overlay.child(dialog);
        return overlay;
    }

    private void rebuildOptions() {
        options.clearChildren();
        if (colorMode == ColorMode.FULL_COLOR) {
            options.child(hexField("display.lore.image_art.background", backgroundColor, value -> {
                backgroundColor = value;
                regenerate();
            }));
            options.child(modeRow(
                    "display.lore.image_art.mask",
                    selectedButton(
                            "display.lore.image_art.optimal",
                            maskMode == MaskMode.OPTIMAL,
                            () -> setMask(MaskMode.OPTIMAL)),
                    selectedButton(
                            "display.lore.image_art.sparse",
                            maskMode == MaskMode.SPARSE,
                            () -> setMask(MaskMode.SPARSE)),
                    selectedButton(
                            "display.lore.image_art.dense",
                            maskMode == MaskMode.DENSE,
                            () -> setMask(MaskMode.DENSE))));
            options.child(slider("display.lore.image_art.compression", compression, 0, 100, 0, value -> {
                compression = value;
                regenerate();
            }));
            options.child(modeRow(
                    "display.lore.image_art.dithering",
                    selectedButton(
                            "display.lore.image_art.disabled",
                            fullColorDither == FullColorDither.DISABLED,
                            () -> setFullColorDither(FullColorDither.DISABLED)),
                    selectedButton(
                            "display.lore.image_art.serpentine",
                            fullColorDither == FullColorDither.SERPENTINE,
                            () -> setFullColorDither(FullColorDither.SERPENTINE)),
                    selectedButton(
                            "display.lore.image_art.light_to_dark",
                            fullColorDither == FullColorDither.LIGHT_TO_DARK,
                            () -> setFullColorDither(FullColorDither.LIGHT_TO_DARK))));
        } else {
            options.child(hexField("display.lore.image_art.text_color", textColor, value -> {
                textColor = value;
                regenerate();
            }));
            options.child(slider("display.lore.image_art.threshold", threshold, .1, 99.9, 1, value -> {
                threshold = value;
                regenerate();
            }));
            options.child(slider("display.lore.image_art.bilevel_dither", bilevelDither, 0, 9.99, 2, value -> {
                bilevelDither = value;
                regenerate();
            }));
            options.child(modeRow(
                    "display.lore.image_art.invert",
                    selectedButton("display.lore.image_art.normal", !invert, () -> setInvert(false)),
                    selectedButton("display.lore.image_art.inverted", invert, () -> setInvert(true))));
        }
        updateColorModeButtons();
    }

    private FlowLayout hexField(String labelKey, int color, IntConsumer setter) {
        TextBoxComponent input = UiFactory.textBox(ValidationUtil.toHex(color), ignored -> {});
        input.onChanged().subscribe(value -> {
            Integer parsed = ValidationUtil.tryParseHexColor(value);
            if (parsed != null) setter.accept(parsed);
        });
        return UiFactory.field(ItemEditorText.tr(labelKey), Component.empty(), input);
    }

    private FlowLayout slider(
            String labelKey, double value, double min, double max, int decimals, DoubleConsumer setter) {
        LabelComponent label = UiFactory.muted(sliderLabel(labelKey, value));
        DiscreteSliderComponent slider = new SafeDiscreteSliderComponent(Sizing.fill(100), min, max);
        slider.decimalPlaces(decimals).snap(true);
        slider.setFromDiscreteValue(value);
        slider.onChanged().subscribe(next -> {
            setter.accept(next);
            label.text(sliderLabel(labelKey, next));
        });
        FlowLayout row = UiFactory.column();
        row.child(label);
        row.child(slider);
        return row;
    }

    private Component sliderLabel(String key, double value) {
        return ItemEditorText.tr(key).copy().append(Component.literal(": " + format(value)));
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.2f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    private ButtonComponent modeButton(String key, Runnable action) {
        return UiFactory.actionRowButton(
                ItemEditorText.tr(key),
                UiFactory.ButtonTextPreset.COMPACT,
                UiFactory.ActionTone.NEUTRAL,
                ignored -> action.run());
    }

    private ButtonComponent selectedButton(String key, boolean selected, Runnable action) {
        ButtonComponent button = modeButton(key, action);
        button.active(!selected);
        return button;
    }

    private FlowLayout modeRow(String labelKey, ButtonComponent... buttons) {
        FlowLayout group = UiFactory.column();
        group.child(UiFactory.muted(ItemEditorText.tr(labelKey)));
        group.child(UiFactory.actionButtonRow(buttons));
        return group;
    }

    private void setMask(MaskMode mode) {
        maskMode = mode;
        rebuildOptions();
        regenerate();
    }

    private void setFullColorDither(FullColorDither mode) {
        fullColorDither = mode;
        rebuildOptions();
        regenerate();
    }

    private void setInvert(boolean value) {
        invert = value;
        rebuildOptions();
        regenerate();
    }

    private void updateColorModeButtons() {
        fullColorButton.active(colorMode != ColorMode.FULL_COLOR);
        bilevelButton.active(colorMode != ColorMode.BILEVEL);
    }

    private void updateInsertButtons() {
        replaceButton.active(append);
        appendButton.active(!append);
    }

    private void openFile() {
        CompletableFuture.runAsync(() -> {
            String path;
            try (MemoryStack stack = MemoryStack.stackPush()) {
                PointerBuffer patterns = stack.mallocPointer(2);
                patterns.put(stack.UTF8("*.png"));
                patterns.put(stack.UTF8("*.jpg;*.jpeg"));
                patterns.flip();
                path = TinyFileDialogs.tinyfd_openFileDialog(
                        ItemEditorText.str(targetKey("file_title")),
                        minecraft.gameDirectory.toPath().toString(),
                        patterns,
                        ItemEditorText.str("display.lore.image_art.file_filter"),
                        false);
            } catch (RuntimeException | LinkageError failure) {
                minecraft.execute(() -> showError(failure.getMessage()));
                return;
            }
            if (path == null || path.isBlank()) return;
            try {
                BufferedImage loaded = ImageIO.read(Path.of(path).toFile());
                minecraft.execute(() -> setImage(path, loaded));
            } catch (Exception failure) {
                minecraft.execute(() -> showError(failure.getMessage()));
            }
        });
    }

    private void setImage(String path, BufferedImage loaded) {
        if (closed) return;
        if (loaded == null) {
            showError(ItemEditorText.str("display.lore.image_art.invalid"));
            return;
        }
        generator = new LoreImageArtUtil.Generator(loaded);
        fileLabel.text(Component.literal(Path.of(path).getFileName().toString()));
        regenerate();
    }

    private void regenerate() {
        if (closed || generator == null || applyButton == null) return;
        applyButton.active(false);
        int width;
        try {
            width = Integer.parseInt(widthText.trim());
        } catch (NumberFormatException ignored) {
            generation.cancel();
            applyButton.active(false);
            showError(ItemEditorText.str("display.lore.image_art.invalid_width"));
            return;
        }
        status.text(ItemEditorText.tr("display.lore.image_art.processing"));
        generation.request(
                generator,
                width,
                new LoreImageArtUtil.Options(
                        colorMode,
                        backgroundColor,
                        textColor,
                        threshold,
                        bilevelDither,
                        invert,
                        maskMode,
                        compression,
                        fullColorDither),
                !keyPrefix.equals("display.lore.image_art."),
                this::publish,
                failure -> showError(failure.getMessage()));
    }

    private void publish(ImageArtGenerationService.Result generatedResult) {
        result = generatedResult.art();
        int serializedLength = generatedResult.serializedLength();
        String error = RawItemDataUtil.networkNbtError(generatedResult.nbtUsage());
        if (error == null && serializedLength > LoreImageArtUtil.MAX_SAFE_SERIALIZED_LENGTH) {
            error = ItemEditorText.str(
                    "display.lore.image_art.too_large",
                    serializedLength,
                    LoreImageArtUtil.MAX_SAFE_SERIALIZED_LENGTH,
                    serializedLength - LoreImageArtUtil.MAX_SAFE_SERIALIZED_LENGTH);
        }
        applyButton.active(error == null);
        String generated =
                ItemEditorText.str(targetKey("generated"), result.width(), result.height(), serializedLength);
        status.text(Component.literal(error == null ? generated : generated + "\n" + error));
        updatePreviewSurface();
        preview.clearChildren();
        int previewWidth =
                result.lines().stream().mapToInt(minecraft.font::width).max().orElse(1);
        previewScroll.horizontalSizing(Sizing.fixed(previewWidth + UiFactory.scaledScrollbarThickness(8) + 4));
        for (Component line : result.lines()) {
            preview.child(new ScaledLabelComponent(line).textScale(1).horizontalSizing(Sizing.fixed(previewWidth)));
        }
    }

    private void updatePreviewSurface() {
        int color = colorMode == ColorMode.FULL_COLOR ? backgroundColor : invert ? 0xFFFFFF : 0;
        previewArea.surface(Surface.flat(0xFF000000 | color).and(Surface.outline(PREVIEW_OUTLINE)));
    }

    private void showError(String message) {
        if (closed) return;
        status.text(Component.literal(
                message == null || message.isBlank() ? ItemEditorText.str("raw.unknown_error") : message));
        if (applyButton != null) applyButton.active(false);
    }

    public void apply() {
        if (result != null && applyButton != null && applyButton.active()) onApply.accept(result.lines(), append);
    }

    private String targetKey(String suffix) {
        return keyPrefix + suffix;
    }
}
