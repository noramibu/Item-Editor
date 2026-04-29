package me.noramibu.itemeditor.ui.component;

import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.component.TextBoxComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.core.Sizing;
import me.noramibu.itemeditor.util.ItemEditorText;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;

public final class SearchablePickerDialog {

    private static final int DIALOG_WIDTH = 640;
    private static final int RESULTS_HEIGHT = 340;
    private static final int DIALOG_GAP = 8;
    private static final int RESULTS_GAP = 4;
    private static final int BODY_TEXT_MARGIN = 32;
    private static final int LINE_TEXT_MARGIN = 48;
    private static final int BUTTON_RESERVE_EXTRA = 16;
    private static final int COMPACT_BUTTON_WIDTH_THRESHOLD = 360;
    private static final int HEADER_RESERVE_EMPTY_BODY = 96;
    private static final int HEADER_RESERVE_WITH_BODY = 116;
    private static final int FOOTER_ROWS = 1;
    private static final int RESULTS_MIN_HEIGHT = 136;
    private static final int DIALOG_MIN_HEIGHT = 240;
    private static final int LABEL_WIDTH_MIN = 160;
    private static final int LABEL_WIDTH_RESERVE = 24;
    private static final int LABEL_SCROLLBAR_INSET_BASE = 8;
    private static final int RESULTS_SCROLL_STEP = 12;
    private static final int FOOTER_BUTTON_MIN_WIDTH = 72;
    private static final int FOOTER_BUTTON_MAX_WIDTH = 140;
    private static final int FOOTER_BUTTON_DIVISOR = 4;
    private static final int FOOTER_BUTTON_TEXT_MIN_WIDTH = 24;
    private static final int FOOTER_BUTTON_TEXT_RESERVE = 10;
    private static final String EMPTY_TEXT = "";

    private SearchablePickerDialog() {
    }

    public static FlowLayout create(
            String title,
            String body,
            List<String> values,
            Function<String, String> labelMapper,
            Consumer<String> onSelect,
            Runnable onCancel
    ) {
        FlowLayout overlay = DialogUiUtil.overlay();
        int dialogWidth = DialogUiUtil.dialogWidth(DIALOG_WIDTH);
        boolean compactButtons = DialogUiUtil.compactButtons(dialogWidth, COMPACT_BUTTON_WIDTH_THRESHOLD);
        boolean hasBody = !body.isBlank();
        int bodyTextWidth = DialogUiUtil.dialogTextWidth(dialogWidth, BODY_TEXT_MARGIN);
        int lineTextWidth = DialogUiUtil.dialogTextWidth(dialogWidth, LINE_TEXT_MARGIN);
        int controlHeight = UiFactory.scaleProfile().controlHeight();
        int footerReserve = DialogUiUtil.buttonRowReserve(compactButtons, FOOTER_ROWS, BUTTON_RESERVE_EXTRA, BUTTON_RESERVE_EXTRA);
        int headerReserve = UiFactory.scaledPixels(hasBody ? HEADER_RESERVE_WITH_BODY : HEADER_RESERVE_EMPTY_BODY)
                + controlHeight
                + footerReserve;
        DialogUiUtil.ScrollDialogSizing sizing = DialogUiUtil.scrollDialogSizing(
                RESULTS_HEIGHT,
                headerReserve,
                RESULTS_MIN_HEIGHT,
                DIALOG_MIN_HEIGHT
        );

        FlowLayout dialog = DialogUiUtil.dialogCard(dialogWidth, sizing.dialogHeight(), DIALOG_GAP);
        dialog.child(UiFactory.title(title));
        if (hasBody) {
            dialog.child(UiFactory.muted(body, bodyTextWidth));
        }

        TextBoxComponent search = UiFactory.textBox(EMPTY_TEXT, value -> {});
        dialog.child(UiFactory.field(
                ItemEditorText.tr("dialog.searchable_picker.search"),
                Component.empty(),
                search
        ));

        LabelComponent resultsCount = UiFactory.muted("", bodyTextWidth);
        dialog.child(resultsCount);

        int preferredLabelWidth = Math.max(
                LABEL_WIDTH_MIN,
                lineTextWidth - LABEL_WIDTH_RESERVE - UiFactory.scrollContentInset(LABEL_SCROLLBAR_INSET_BASE)
        );
        int maxLabelWidth = Math.max(1, Math.min(lineTextWidth, preferredLabelWidth));
        FlowLayout results = UiFactory.column();
        results.gap(RESULTS_GAP);

        InputSafeScrollContainer<FlowLayout> modalScroll = InputSafeScrollContainer.vertical(
                Sizing.fill(100),
                UiFactory.fixed(sizing.contentHeight()),
                results
        ).consumeScrollWhenHovered(true);

        ScrollContainer<FlowLayout> verticalScroll = DialogUiUtil.vanillaScroll(
                modalScroll,
                RESULTS_SCROLL_STEP
        );

        FlowLayout resultCard = UiFactory.subCard();
        resultCard.child(verticalScroll);
        dialog.child(resultCard);

        Runnable refresh = () -> {
            String query = search.getValue().trim().toLowerCase(Locale.ROOT);
            results.clearChildren();
            int matches = 0;

            for (String value : values) {
                String label = normalizedLabel(value, labelMapper);
                String rawValue = normalizedValue(value);
                String lowerLabel = label.toLowerCase(Locale.ROOT);
                String lowerValue = rawValue.toLowerCase(Locale.ROOT);
                if (!query.isBlank() && !lowerLabel.contains(query) && !lowerValue.contains(query)) {
                    continue;
                }

                matches++;
                Component fitted = UiFactory.fitToWidth(Component.literal(label), maxLabelWidth);
                var button = UiFactory.button(fitted, UiFactory.ButtonTextPreset.STANDARD,  component -> onSelect.accept(value));
                button.horizontalSizing(Sizing.fill(100));
                if (!label.equals(rawValue) || !fitted.getString().equals(label)) {
                    button.tooltip(List.of(Component.literal(label), Component.literal(rawValue)));
                }
                results.child(button);
            }

            resultsCount.text(ItemEditorText.tr("dialog.searchable_picker.results", matches, values.size()));
            if (matches == 0) {
                results.child(UiFactory.muted(ItemEditorText.tr("dialog.searchable_picker.none"), maxLabelWidth));
            }
        };

        search.onChanged().subscribe(value -> refresh.run());
        refresh.run();

        FlowLayout buttonRow = DialogUiUtil.footerRowByDivisor(
                dialogWidth,
                compactButtons,
                FOOTER_BUTTON_MIN_WIDTH,
                FOOTER_BUTTON_MAX_WIDTH,
                FOOTER_BUTTON_DIVISOR,
                FOOTER_BUTTON_TEXT_MIN_WIDTH,
                FOOTER_BUTTON_TEXT_RESERVE,
                new DialogUiUtil.FooterAction(ItemEditorText.tr("common.cancel"), button -> onCancel.run())
        );
        dialog.child(buttonRow);

        overlay.child(dialog);
        return overlay;
    }

    private static String normalizedLabel(String value, Function<String, String> labelMapper) {
        String normalized = normalizedValue(value);
        if (labelMapper == null) {
            return normalized;
        }
        String mapped = labelMapper.apply(normalized);
        return mapped == null ? EMPTY_TEXT : mapped;
    }

    private static String normalizedValue(String value) {
        return value == null ? EMPTY_TEXT : value;
    }
}
