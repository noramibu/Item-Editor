package me.noramibu.itemeditor.ui.component;

import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.Size;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.UIComponent;
import java.util.List;

public final class CompactFieldLayout extends FlowLayout {
    private final List<? extends UIComponent> fields;
    private final int preferredWidth;

    public CompactFieldLayout(List<? extends UIComponent> fields, int preferredWidth) {
        super(Sizing.fill(100), Sizing.content(), Algorithm.LTR_TEXT);
        this.fields = List.copyOf(fields);
        this.preferredWidth = preferredWidth;
        gap(Math.max(1, UiFactory.scaleProfile().spacing()));
        allowOverflow(false);
        children(fields);
    }

    @Override
    public void layout(Size space) {
        int width = cellWidth(calculateChildSpace(space).width(), preferredWidth, gap(), fields.size());
        for (UIComponent field : fields) {
            field.horizontalSizing(Sizing.fixed(width));
            field.margins(Insets.bottom(gap()));
        }
        super.layout(space);
    }

    static int cellWidth(int availableWidth, int preferredWidth, int gap, int fieldCount) {
        int available = Math.max(1, availableWidth);
        int columns =
                Math.clamp((available + gap) / (Math.max(1, preferredWidth) + gap), 1, Math.clamp(fieldCount, 1, 4));
        return Math.max(1, (available - gap * (columns - 1)) / columns);
    }
}
