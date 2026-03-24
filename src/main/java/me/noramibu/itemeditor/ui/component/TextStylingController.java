package me.noramibu.itemeditor.ui.component;

import io.wispforest.owo.ui.component.LabelComponent;
import me.noramibu.itemeditor.editor.text.RichTextDocument;
import me.noramibu.itemeditor.util.ItemEditorText;
import me.noramibu.itemeditor.util.TextComponentUtil;
import net.minecraft.network.chat.Component;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;

public final class TextStylingController {

    private TextStylingController() {
    }

    public static AtomicInteger initialColor(RichTextAreaComponent editor, int fallbackColor) {
        Integer color = editor.document().insertionStyleAt(0).color();
        return new AtomicInteger(color == null ? fallbackColor : color);
    }

    public static void bindValidatedDocument(
            RichTextAreaComponent editor,
            LabelComponent validationLabel,
            Function<RichTextDocument, String> validator,
            Consumer<RichTextDocument> onAccepted
    ) {
        editor.validator(validator);
        editor.onRejected(message -> validationLabel.text(Component.literal(message)));
        editor.onDocumentChanged().subscribe(document -> {
            validationLabel.text(Component.empty());
            onAccepted.accept(document);
        });
    }

    public static String validateSingleLineLegacy(RichTextDocument document, String fieldLabel, int maxLength) {
        if (document.logicalLineCount() > 1) {
            return ItemEditorText.str("text.validation.single_line", fieldLabel);
        }

        if (maxLength > 0) {
            String serialized = TextComponentUtil.toLegacyPaletteString(document.toComponent());
            if (serialized.length() > maxLength) {
                return ItemEditorText.str("text.validation.max_length", fieldLabel, maxLength);
            }
        }

        return null;
    }
}
