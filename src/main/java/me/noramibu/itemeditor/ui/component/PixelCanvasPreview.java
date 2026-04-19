package me.noramibu.itemeditor.ui.component;

import io.wispforest.owo.ui.component.BoxComponent;
import io.wispforest.owo.ui.component.UIComponents;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Color;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.Surface;

import java.util.ArrayList;
import java.util.List;

public final class PixelCanvasPreview {

    public static final int TRANSPARENT = Integer.MIN_VALUE;

    private PixelCanvasPreview() {
    }

    public static FlowLayout fromColors(
            int[][] colors,
            int pixelSize,
            int gap,
            Insets padding,
            Surface surface
    ) {
        int height = colors.length;
        int width = height == 0 ? 0 : colors[0].length;
        PixelCanvas canvas = create(width, height, pixelSize, gap, padding, surface);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                setPixel(canvas, x, y, colors[y][x]);
            }
        }
        return canvas.root();
    }

    public static PixelCanvas create(
            int width,
            int height,
            int pixelSize,
            int gap,
            Insets padding,
            Surface surface
    ) {
        FlowLayout canvas = UiFactory.column();
        canvas.gap(gap);
        canvas.padding(padding);
        canvas.surface(surface);
        int rowWidth = Math.max(1, width * pixelSize + Math.max(0, width - 1) * gap);
        canvas.horizontalSizing(UiFactory.fixed(rowWidth + padding.left() + padding.right()));

        List<List<BoxComponent>> grid = new ArrayList<>(height);
        for (int y = 0; y < height; y++) {
            FlowLayout row = UiFactory.row();
            row.horizontalSizing(UiFactory.fixed(rowWidth));
            row.verticalSizing(UiFactory.fixed(pixelSize));
            row.gap(gap);
            List<BoxComponent> rowPixels = new ArrayList<>(width);
            for (int x = 0; x < width; x++) {
                BoxComponent pixel = UIComponents.box(UiFactory.fixed(pixelSize), UiFactory.fixed(pixelSize))
                        .fill(true)
                        .color(Color.ofArgb(0x00000000));
                rowPixels.add(pixel);
                row.child(pixel);
            }
            grid.add(rowPixels);
            canvas.child(row);
        }
        return new PixelCanvas(canvas, grid, width, height);
    }

    public static void setPixel(PixelCanvas canvas, int x, int y, int argbOrTransparent) {
        if (x < 0 || y < 0 || x >= canvas.width() || y >= canvas.height()) {
            return;
        }
        BoxComponent pixel = canvas.grid().get(y).get(x);
        if (argbOrTransparent == TRANSPARENT) {
            pixel.color(Color.ofArgb(0x00000000));
        } else if ((argbOrTransparent & 0xFF000000) == 0) {
            pixel.color(Color.ofRgb(argbOrTransparent));
        } else {
            pixel.color(Color.ofArgb(argbOrTransparent));
        }
    }

    public static int[][] fill(int width, int height, int color) {
        int[][] pixels = new int[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                pixels[y][x] = color;
            }
        }
        return pixels;
    }

    public record PixelCanvas(FlowLayout root, List<List<BoxComponent>> grid, int width, int height) {
    }
}
