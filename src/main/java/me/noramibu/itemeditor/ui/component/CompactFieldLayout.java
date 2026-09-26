package me.noramibu.itemeditor.ui.component;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.CheckboxComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.Size;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.Surface;
import io.wispforest.owo.ui.core.UIComponent;
import io.wispforest.owo.ui.core.VerticalAlignment;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.IntUnaryOperator;
import me.noramibu.itemeditor.ui.screen.ItemEditorScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

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

    public enum Width {
        NUMERIC(0.065, 64, 104),
        TINY(0.05, 54, 80),
        ID(0.22, 104, 220),
        GROUP(0.18, 96, 190),
        PICKER(0.16, 86, 150),
        LONG(0.26, 136, 280),
        NARROW_ID(0.14, 130, 220),
        HOLDER_KIND(0, 34, 42),
        HOLDER_REMOVE(0, 34, 40),
        ICON(0, 36, 42),
        CLEAR(0, 52, 68),
        REMOVE(0, 72, 88),
        FIXED_PICK(0, 46, 56);

        private final double ratio;
        private final int min;
        private final int max;

        Width(double ratio, int min, int max) {
            this.ratio = ratio;
            this.min = min;
            this.max = max;
        }

        public int pixels() {
            return pixels(guiWidth(), UiFactory::scaledPixels);
        }

        int pixels(int available, IntUnaryOperator scale) {
            int preferred = ratio == 0
                    ? Math.max(min, scale.applyAsInt(max))
                    : Math.clamp((int) Math.round(available * ratio), min, max);
            return Math.clamp(preferred, 1, Math.max(1, available));
        }
    }

    public static FlowLayout compactField(Component label, UIComponent input, int labelWidth) {
        FlowLayout field = UiFactory.column();
        field.gap(2);
        int panelWidth = guiWidth();
        int availableLabelWidth = Math.max(80, panelWidth - UiFactory.scaledPixels(44));
        int preferredLabelWidth = prefersStackedCompactRows()
                ? availableLabelWidth
                : Math.clamp(availableLabelWidth, 40, Math.max(40, labelWidth));
        int effectiveLabelWidth = Math.clamp(preferredLabelWidth, 1, Math.max(1, panelWidth));
        Component fittedLabel = UiFactory.fitToWidth(label, effectiveLabelWidth);
        var labelComponent = UiFactory.muted(fittedLabel, effectiveLabelWidth);
        if (label.getContents() instanceof TranslatableContents translation) {
            labelComponent.id(translation.getKey());
        }
        labelComponent.horizontalSizing(Sizing.fill(100));
        if (!Objects.equals(fittedLabel.getString(), label.getString())) {
            labelComponent.tooltip(List.of(label));
        }
        field.child(UiFactory.bindField(label, labelComponent));
        field.child(UiFactory.bindField(label, input).horizontalSizing(Sizing.fill(100)));
        return field;
    }

    public static FlowLayout selectorRow(Component label, ButtonComponent button) {
        return selectorRow(label, button, false);
    }

    public static FlowLayout selectorRow(Component label, ButtonComponent button, boolean changed) {
        return selectorRow(label, button, changed, UiFactory.scaledPixels(120));
    }

    public static FlowLayout selectorRow(Component label, ButtonComponent button, boolean changed, int preferredWidth) {
        var name = UiFactory.muted(changed ? UiFactory.withChangedMarker(label) : label);
        if (changed) button.setMessage(UiFactory.withChangedMarker(button.getMessage()));
        UiFactory.bindField(label, name);
        UiFactory.bindField(label, button);
        int preferredGap = Math.max(2, UiFactory.scaleProfile().tightSpacing());
        FlowLayout row = new FlowLayout(Sizing.fill(100), Sizing.content(), Algorithm.LTR_TEXT) {
            @Override
            public void layout(Size space) {
                int available = Math.max(1, calculateChildSpace(space).width());
                SelectorWidths widths = selectorWidths(
                        available,
                        Math.min(
                                UiFactory.scaledPixels(140),
                                Minecraft.getInstance().font.width(name.text())
                                        + UiFactory.scaleProfile().tightSpacing()),
                        selectorWidth(available, preferredWidth, UiFactory.scaledPixels(120)),
                        preferredGap);
                name.maxWidth(widths.label());
                name.horizontalSizing(Sizing.fixed(widths.label()));
                button.horizontalSizing(Sizing.fixed(widths.button()));
                super.layout(space);
            }
        };
        row.gap(preferredGap);
        row.verticalAlignment(VerticalAlignment.CENTER);
        row.child(name).child(button);
        FlowLayout divider = UiFactory.row();
        divider.verticalSizing(Sizing.fixed(1));
        divider.surface(Surface.flat(0xFF414B56));
        return UiFactory.column().child(row).child(divider);
    }

    static int selectorWidth(int available, int preferred, int fallback) {
        return Math.clamp(preferred > 0 ? preferred : fallback, 1, Math.max(1, available));
    }

    static SelectorWidths selectorWidths(int available, int labelPreferred, int buttonPreferred, int gap) {
        int usable = Math.max(2, available - Math.max(0, gap));
        int label = Math.clamp(labelPreferred, 1, Math.max(1, usable / 2));
        int button = Math.clamp(buttonPreferred, 1, Math.max(1, usable - label));
        return new SelectorWidths(label, button);
    }

    record SelectorWidths(int label, int button) {}

    public static int guiWidth() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gui.screen() instanceof ItemEditorScreen itemEditorScreen) {
            int hinted = itemEditorScreen.editorContentWidthHint();
            int reserve = Math.max(2, UiFactory.scaledPixels(20));
            return Math.max(1, hinted - reserve);
        }
        return UiFactory.responsiveBodyTextWidth();
    }

    public static boolean isNarrowLayout() {
        return guiWidth() <= 900;
    }

    public static boolean prefersStackedCompactRows() {
        return guiWidth() <= 1080;
    }

    public static FlowLayout responsiveRow() {
        return prefersStackedCompactRows() ? UiFactory.column() : UiFactory.row();
    }

    public static FlowLayout denseEquipmentRow() {
        return guiWidth() <= 620 ? UiFactory.column() : UiFactory.row();
    }

    public static FlowLayout denseEquipmentRow(UIComponent... children) {
        FlowLayout row = denseEquipmentRow();
        if (children.length == 0) {
            return row;
        }
        Sizing childWidth = guiWidth() <= 620 ? Sizing.fill(100) : Sizing.expand(100 / children.length);
        for (UIComponent child : children) {
            child.horizontalSizing(childWidth);
            row.child(child);
        }
        return row;
    }

    public static FlowLayout compactCheckboxRow(UIComponent... children) {
        int preferredWidth = Arrays.stream(children)
                        .filter(CheckboxComponent.class::isInstance)
                        .map(CheckboxComponent.class::cast)
                        .mapToInt(checkbox -> Minecraft.getInstance().font.width(checkbox.getMessage()))
                        .max()
                        .orElse(0)
                + UiFactory.scaleProfile().controlHeight()
                + UiFactory.scaleProfile().padding() * 2;
        return new CompactFieldLayout(List.of(children), preferredWidth);
    }

    public static void distributeRowChildren(FlowLayout row, UIComponent... children) {
        if (children.length == 0) {
            return;
        }
        Sizing childWidth = prefersStackedCompactRows() ? Sizing.fill(100) : Sizing.expand(100 / children.length);
        for (UIComponent child : children) {
            child.horizontalSizing(childWidth);
            row.child(child);
        }
    }

    static int cellWidth(int availableWidth, int preferredWidth, int gap, int fieldCount) {
        int available = Math.max(1, availableWidth);
        int columns =
                Math.clamp((available + gap) / (Math.max(1, preferredWidth) + gap), 1, Math.clamp(fieldCount, 1, 4));
        return Math.max(1, (available - gap * (columns - 1)) / columns);
    }
}
