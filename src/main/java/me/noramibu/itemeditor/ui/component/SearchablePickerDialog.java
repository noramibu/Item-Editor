package me.noramibu.itemeditor.ui.component;

import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.component.TextBoxComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.container.UIContainers;
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

        FlowLayout dialog = UiFactory.centeredCard(DIALOG_WIDTH).gap(8);
        dialog.child(UiFactory.title(title));
        if (!body.isBlank()) {
            dialog.child(UiFactory.muted(body, DIALOG_WIDTH - 32));
        }

        TextBoxComponent search = UiFactory.textBox("", value -> {});
        search.horizontalSizing(Sizing.fill(100));
        dialog.child(UiFactory.field(
                ItemEditorText.tr("dialog.searchable_picker.search"),
                Component.empty(),
                search
        ));

        LabelComponent resultsCount = UiFactory.muted("", DIALOG_WIDTH - 32);
        dialog.child(resultsCount);

        FlowLayout results = UiFactory.column();
        ScrollContainer<FlowLayout> resultScroll = DialogUiUtil.vanillaScroll(
                UIContainers.verticalScroll(
                        Sizing.fill(100),
                        Sizing.fixed(RESULTS_HEIGHT),
                        results
                ),
                12
        );

        FlowLayout resultCard = UiFactory.subCard();
        resultCard.child(resultScroll);
        dialog.child(resultCard);

        Runnable refresh = () -> {
            String query = search.getValue().trim().toLowerCase(Locale.ROOT);
            results.clearChildren();
            int matches = 0;

            for (String value : values) {
                String label = labelMapper.apply(value);
                String normalizedLabel = label.toLowerCase(Locale.ROOT);
                String normalizedValue = value.toLowerCase(Locale.ROOT);
                if (!query.isBlank() && !normalizedLabel.contains(query) && !normalizedValue.contains(query)) {
                    continue;
                }

                matches++;
                var button = UiFactory.button(Component.literal(label), component -> onSelect.accept(value));
                button.horizontalSizing(Sizing.fill(100));
                if (!label.equals(value)) {
                    button.tooltip(List.of(Component.literal(value)));
                }
                results.child(button);
            }

            resultsCount.text(ItemEditorText.tr("dialog.searchable_picker.results", matches, values.size()));
            if (matches == 0) {
                results.child(UiFactory.muted(ItemEditorText.tr("dialog.searchable_picker.none"), DIALOG_WIDTH - 48));
            }
        };

        search.onChanged().subscribe(value -> refresh.run());
        refresh.run();

        FlowLayout buttonRow = DialogUiUtil.rightAlignedButtonRow();
        buttonRow.child(UiFactory.button(ItemEditorText.tr("common.cancel"), button -> onCancel.run()).horizontalSizing(Sizing.content()));
        dialog.child(buttonRow);

        overlay.child(dialog);
        return overlay;
    }
}
