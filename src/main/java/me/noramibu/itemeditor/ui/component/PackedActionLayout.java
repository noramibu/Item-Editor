package me.noramibu.itemeditor.ui.component;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.Size;
import io.wispforest.owo.ui.core.Sizing;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

final class PackedActionLayout extends FlowLayout {
    private static final int LABEL_PADDING = 8;

    private final List<ButtonComponent> buttons;
    private final List<Integer> preferredWidths;
    private final boolean forceSingleRow;
    private final boolean fillRows;

    PackedActionLayout(
            List<ButtonComponent> buttons,
            int gap,
            boolean forceSingleRow,
            boolean fillRows
    ) {
        super(Sizing.fill(100), Sizing.content(), FlowLayout.Algorithm.LTR_TEXT);
        this.buttons = List.copyOf(buttons);
        this.preferredWidths = buttons.stream()
                .map(PackedActionLayout::preferredWidth)
                .toList();
        this.forceSingleRow = forceSingleRow;
        this.fillRows = fillRows;
        this.gap(gap);
        this.children(buttons);
    }

    @Override
    public void layout(Size space) {
        int availableWidth = Math.max(1, this.calculateChildSpace(space).width());
        List<List<Integer>> rows = this.forceSingleRow
                ? List.of(allIndexes())
                : packedRows(availableWidth, this.gap(), this.preferredWidths);

        for (List<Integer> row : rows) {
            applyRowWidths(row, availableWidth);
        }
        super.layout(space);
    }

    private void applyRowWidths(List<Integer> row, int availableWidth) {
        int gapsWidth = this.gap() * Math.max(0, row.size() - 1);
        int usableWidth = Math.max(row.size(), availableWidth - gapsWidth);
        if (this.forceSingleRow || this.fillRows) {
            int baseWidth = Math.max(1, usableWidth / row.size());
            int remainder = Math.max(0, usableWidth - (baseWidth * row.size()));
            for (int index : row) {
                setButtonWidth(index, baseWidth + (remainder-- > 0 ? 1 : 0));
            }
            return;
        }

        for (int index : row) {
            setButtonWidth(index, Math.min(availableWidth, this.preferredWidths.get(index)));
        }
    }

    private void setButtonWidth(int index, int width) {
        ButtonComponent button = this.buttons.get(index);
        if (button.getWidth() != width) {
            button.horizontalSizing(Sizing.fixed(width));
        }
        button.margins(Insets.bottom(this.gap()));
    }

    private List<Integer> allIndexes() {
        List<Integer> indexes = new ArrayList<>(this.buttons.size());
        for (int index = 0; index < this.buttons.size(); index++) {
            indexes.add(index);
        }
        return indexes;
    }

    static List<List<Integer>> packedRows(int availableWidth, int gap, List<Integer> preferredWidths) {
        List<List<Integer>> rows = new ArrayList<>();
        List<Integer> row = new ArrayList<>();
        int usedWidth = 0;
        for (int index = 0; index < preferredWidths.size(); index++) {
            int width = Math.min(Math.max(1, availableWidth), Math.max(1, preferredWidths.get(index)));
            int requiredWidth = row.isEmpty() ? width : gap + width;
            if (!row.isEmpty() && usedWidth + requiredWidth > availableWidth) {
                rows.add(row);
                row = new ArrayList<>();
                usedWidth = 0;
                requiredWidth = width;
            }
            row.add(index);
            usedWidth += requiredWidth;
        }
        if (!row.isEmpty()) {
            rows.add(row);
        }
        return rows;
    }

    private static int preferredWidth(ButtonComponent button) {
        Sizing sizing = button.horizontalSizing().get();
        int configuredWidth = sizing.method == Sizing.Method.FIXED ? sizing.value : 0;
        int labelWidth = Minecraft.getInstance().font.width(button.getMessage()) + LABEL_PADDING;
        return Math.max(configuredWidth, labelWidth);
    }
}
