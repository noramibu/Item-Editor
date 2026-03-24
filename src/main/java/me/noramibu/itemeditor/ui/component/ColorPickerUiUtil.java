package me.noramibu.itemeditor.ui.component;

import io.wispforest.owo.ui.component.BoxComponent;
import io.wispforest.owo.ui.component.ColorPickerComponent;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.component.TextBoxComponent;
import io.wispforest.owo.ui.core.Color;
import me.noramibu.itemeditor.util.ItemEditorText;
import me.noramibu.itemeditor.util.ValidationUtil;
import net.minecraft.network.chat.Component;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

final class ColorPickerUiUtil {

    private ColorPickerUiUtil() {
    }

    static Runnable createSyncRunnable(
            AtomicBoolean syncing,
            IntSupplier rgbSupplier,
            ColorPickerComponent picker,
            BoxComponent swatch,
            LabelComponent swatchLabel,
            TextBoxComponent hexInput,
            Runnable postSync
    ) {
        return () -> {
            syncing.set(true);
            int rgb = rgbSupplier.getAsInt() & 0xFFFFFF;
            picker.selectedColor(Color.ofRgb(rgb));
            swatch.color(Color.ofRgb(rgb));
            swatchLabel.text(Component.literal(ValidationUtil.toHex(rgb)).withColor(rgb));
            hexInput.text(ValidationUtil.toHex(rgb));
            postSync.run();
            syncing.set(false);
        };
    }

    static void bindHexInput(
            TextBoxComponent hexInput,
            AtomicBoolean syncing,
            LabelComponent errorLabel,
            IntConsumer setSelectedRgb
    ) {
        hexInput.onChanged().subscribe(value -> {
            if (syncing.get()) return;
            Integer parsed = ValidationUtil.tryParseHexColor(value);
            if (parsed == null) {
                errorLabel.text(ItemEditorText.tr("dialog.color_picker.hex_error"));
                return;
            }
            setSelectedRgb.accept(parsed);
        });
    }
}
