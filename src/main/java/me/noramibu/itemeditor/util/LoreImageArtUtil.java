package me.noramibu.itemeditor.util;

import com.mojang.serialization.JsonOps;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CancellationException;
import me.noramibu.itemeditor.editor.text.RichTextDocument;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.component.ItemLore;

public final class LoreImageArtUtil {
    public static final int MAX_SAFE_SERIALIZED_LENGTH = 1_000_000;

    private static final int CELL_WIDTH = 2;
    private static final int CELL_HEIGHT = 4;
    private static final int MIN_WIDTH = 4;
    private static final int MAX_WIDTH = 256;
    private static final String NARROW_PUSH_CHARACTER = new String(Character.toChars(0x10100));
    private static final int[] CELL_CODEPOINTS = {
        8194, 118432, 118435, 9602, 118040, 9623, 118097, 118219, 118025, 118144, 9622, 118203, 118055, 118172, 118113,
                129862,
        118019, 118136, 118077, 118195, 130023, 118166, 118105, 118227, 118032, 118152, 118091, 118211, 118063, 118180,
                118121, 118241,
        118016, 118132, 118073, 118191, 118044, 118163, 118101, 118223, 130022, 118148, 118088, 118207, 118059, 118176,
                118117, 118238,
        118022, 118140, 118081, 118199, 118051, 118169, 118109, 118231, 118036, 118156, 118094, 118215, 118067, 118184,
                118125, 9606,
        118443, 118130, 118071, 118189, 118042, 118161, 118099, 118221, 118027, 118146, 118086, 118205, 118057, 118174,
                118115, 118236,
        9629, 118138, 118079, 118197, 118049, 129929, 118107, 118229, 118034, 118154, 9630, 118213, 118065, 118182,
                118123, 9631,
        118017, 118134, 118075, 118193, 118046, 118164, 118103, 118225, 118030, 118150, 118089, 118209, 118061, 118178,
                118119, 118239,
        118024, 118142, 118083, 118201, 118053, 118171, 118111, 118233, 118038, 118158, 118096, 118217, 118069, 118186,
                118127, 118245,
        118440, 118129, 118070, 118188, 118041, 118160, 118098, 118220, 118026, 118145, 118085, 118204, 118056, 118173,
                118114, 118235,
        118020, 118137, 118078, 118196, 118048, 118167, 118106, 118228, 118033, 118153, 118092, 118212, 118064, 118181,
                118122, 118242,
        9624, 118133, 118074, 118192, 118045, 9626, 118102, 118224, 118029, 118149, 9611, 118208, 118060, 118177,
                118118, 9625,
        118023, 118141, 118082, 118200, 118052, 118170, 118110, 118232, 118037, 118157, 118095, 118216, 118068, 118185,
                118126, 118244,
        129922, 118131, 118072, 118190, 118043, 118162, 118100, 118222, 118028, 118147, 118087, 118206, 118058, 118175,
                118116, 118237,
        118021, 118139, 118080, 118198, 118050, 118168, 118108, 118230, 118035, 118155, 118093, 118214, 118066, 118183,
                118124, 118243,
        118018, 118135, 118076, 118194, 118047, 118165, 118104, 118226, 118031, 118151, 118090, 118210, 118062, 118179,
                118120, 118240,
        129884, 118143, 118084, 118202, 118054, 9628, 118112, 118234, 118039, 118159, 9627, 118218, 129925, 118187,
                118128, 129885
    };
    private static final byte[] CELL_DOTS = {
        0, 1, 2, 0, 1, 1, 0, 0, 2, 0, 2, 0, 0, 0, 0, 0, 1, 1, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        2, 0, 2, 0, 0, 0, 0, 0, 2, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        1, 1, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        2, 0, 2, 0, 0, 0, 0, 0, 2, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        2, 0, 2, 0, 0, 0, 0, 0, 2, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
    };

    private LoreImageArtUtil() {}

    public static Result generate(BufferedImage image, int requestedWidth, Options options) {
        return new Generator(image).generate(requestedWidth, options);
    }

    public static final class Generator {
        private final BufferedImage image;
        private int cachedWidth = -1;
        private int width;
        private Rgb[][] pixels;
        private Options cachedOptions;
        private final List<PreparedRow> rows = new ArrayList<>();

        public Generator(BufferedImage image) {
            this.image = image;
        }

        public Result generate(int requestedWidth, Options options) {
            checkCancelled();
            int clampedWidth = Math.clamp(requestedWidth, MIN_WIDTH, MAX_WIDTH);
            if (pixels == null || cachedWidth != clampedWidth) {
                prepare(clampedWidth);
                cachedWidth = clampedWidth;
                rows.clear();
            }
            if (cachedOptions == null
                    || cachedOptions.backgroundColor() != options.backgroundColor()
                    || cachedOptions.maskMode() != options.maskMode()
                    || cachedOptions.fullColorDither() != options.fullColorDither()) rows.clear();
            cachedOptions = options;
            return options.colorMode() == ColorMode.BILEVEL
                    ? bilevel(pixels, width, options)
                    : fullColor(pixels, width, options, rows);
        }

        private void prepare(int requestedWidth) {
            int width = Math.clamp(requestedWidth, MIN_WIDTH, MAX_WIDTH);
            int pixelWidth = width * CELL_WIDTH;
            int pixelHeight = Math.max(1, (int) Math.round((double) image.getHeight() * pixelWidth / image.getWidth()));
            int maxHeight = ItemLore.MAX_LINES * CELL_HEIGHT;
            if (pixelHeight > maxHeight) {
                pixelHeight = maxHeight;
                pixelWidth = Math.max(1, (int) Math.round((double) image.getWidth() * pixelHeight / image.getHeight()));
                width = (pixelWidth + CELL_WIDTH - 1) / CELL_WIDTH;
            }

            pixels = resize(image, pixelWidth, pixelHeight);
            this.width = width;
        }
    }

    private static void checkCancelled() {
        if (Thread.currentThread().isInterrupted()) throw new CancellationException();
    }

    private record PreparedRow(List<List<List<Rgb>>> source, List<ProcessedCell> cells) {}

    private static Result bilevel(Rgb[][] pixels, int width, Options options) {
        boolean[][] grid = new boolean[pixels.length][pixels[0].length];
        for (int y = 0; y < pixels.length; y++) {
            checkCancelled();
            for (int x = 0; x < pixels[y].length; x++) {
                double luma = rgbToYuv(pixels[y][x]).y() * 100;
                double actual = options.invert() ? 100 - luma : luma;
                double threshold = options.invert() ? 100 - options.threshold() : options.threshold();
                grid[y][x] = bilevelPixel(actual, threshold, options.bilevelDither(), x, y);
            }
        }
        List<Component> lines = new ArrayList<>();
        for (String text : gridToChars(grid)) {
            lines.add(appendSegment(null, text, options.textColor()));
        }
        return new Result(lines, width, (pixels.length + CELL_HEIGHT - 1) / CELL_HEIGHT);
    }

    private static Result fullColor(Rgb[][] pixels, int width, Options options, List<PreparedRow> rows) {
        Rgb background = new Rgb(
                options.backgroundColor() >> 16 & 0xFF,
                options.backgroundColor() >> 8 & 0xFF,
                options.backgroundColor() & 0xFF);
        LinearRgb linearBackground = linearRgb(background);
        double compression = Math.pow(Math.clamp(options.compression(), 0, 100) / 100, 3) * 100;
        List<Component> lines = new ArrayList<>();
        for (int y = 0; y < pixels.length; y += CELL_HEIGHT) {
            checkCancelled();
            int rowIndex = y / CELL_HEIGHT;
            if (rows.size() <= rowIndex) {
                List<List<List<Rgb>>> sourceRow = new ArrayList<>();
                List<ProcessedCell> cells = new ArrayList<>();
                for (int x = 0; x < pixels[y].length; x += CELL_WIDTH) {
                    List<List<Rgb>> cell = cell(pixels, x, y);
                    sourceRow.add(cell);
                    cells.add(fastProcessCell(cell, linearBackground, options.maskMode(), options.fullColorDither()));
                }
                rows.add(new PreparedRow(sourceRow, cells));
            }
            PreparedRow prepared = rows.get(rowIndex);
            List<ProcessedCell> row = prepared.cells();
            if (compression > 0) {
                row = lossify(
                        prepared.source(), row, background, options.maskMode(), options.fullColorDither(), compression);
            }

            MutableComponent line = null;
            int lastColor = -1;
            StringBuilder text = new StringBuilder();
            for (ProcessedCell cell : row) {
                String glyphs = gridToChars(cell.active()).getFirst();
                if (lastColor != -1 && lastColor != cell.color()) {
                    line = appendSegment(line, text.toString(), lastColor);
                    text.setLength(0);
                }
                text.append(glyphs);
                lastColor = cell.color();
            }
            if (!text.isEmpty()) {
                line = appendSegment(line, text.toString(), lastColor);
            }
            lines.add(line);
        }
        return new Result(lines, width, (pixels.length + CELL_HEIGHT - 1) / CELL_HEIGHT);
    }

    private static List<ProcessedCell> lossify(
            List<List<List<Rgb>>> source,
            List<ProcessedCell> processed,
            Rgb background,
            MaskMode mode,
            FullColorDither ditherMode,
            double compression) {
        if (processed.size() < 2) return processed;
        LinearRgb linearBackground = linearRgb(background);
        double sphericalTolerance = 1.1 * compression;
        List<ProcessedCell> result = new ArrayList<>();
        for (int start = 0; start < processed.size(); ) {
            int end = start + 1;
            LinearRgb average = processed.get(start).average();
            while (end < processed.size()) {
                checkCancelled();
                LinearRgb candidateAverage = average.include(processed.get(end).average(), end - start + 1);
                boolean fits = true;
                for (int index = start; index <= end; index++) {
                    Delta delta = deltas(processed.get(index).average(), candidateAverage, linearBackground);
                    if (!(Math.abs(delta.spherical()) < sphericalTolerance
                            || (mode != MaskMode.DENSE && Math.abs(delta.angular()) < compression))) {
                        fits = false;
                        break;
                    }
                }
                if (!fits) break;
                average = candidateAverage;
                end++;
            }
            List<List<Rgb>> merged = new ArrayList<>();
            for (int y = 0; y < source.getFirst().size(); y++) {
                List<Rgb> mergedRow = new ArrayList<>();
                for (int x = start; x < end; x++) mergedRow.addAll(source.get(x).get(y));
                merged.add(mergedRow);
            }
            result.add(processMergedCell(merged, background, mode, ditherMode));
            start = end;
        }
        return result;
    }

    private static ProcessedCell fastProcessCell(
            List<List<Rgb>> cell, LinearRgb background, MaskMode mode, FullColorDither ditherMode) {
        List<LinearPixel> pixels = new ArrayList<>();
        boolean[][] active = new boolean[cell.size()][cell.getFirst().size()];
        double totalLumaDelta = 0;
        double totalR = 0;
        double totalG = 0;
        double totalB = 0;
        double minLuma = 1;
        double maxLuma = 0;
        for (int y = 0; y < cell.size(); y++) {
            for (int x = 0; x < cell.get(y).size(); x++) {
                LinearRgb color = linearRgb(cell.get(y).get(x));
                double luma = color.luma();
                pixels.add(new LinearPixel(
                        luma - background.luma(),
                        luma,
                        x,
                        y,
                        y * cell.get(y).size() + (y % 2 == 0 ? x : cell.get(y).size() - 1 - x)));
                totalLumaDelta += luma - background.luma();
                totalR += color.r();
                totalG += color.g();
                totalB += color.b();
                minLuma = Math.min(minLuma, luma);
                maxLuma = Math.max(maxLuma, luma);
            }
        }

        int pixelCount = pixels.size();
        LinearRgb average = new LinearRgb(totalR / pixelCount, totalG / pixelCount, totalB / pixelCount);
        if (mode == MaskMode.DENSE) return denseCell(active, average);

        double deltaR = totalR - pixelCount * background.r();
        double deltaG = totalG - pixelCount * background.g();
        double deltaB = totalB - pixelCount * background.b();
        int activeCount = Math.max(
                1,
                Math.max(
                        minimumActivePixels(deltaR, background.r()),
                        Math.max(
                                minimumActivePixels(deltaG, background.g()),
                                minimumActivePixels(deltaB, background.b()))));
        if (mode != MaskMode.SPARSE) {
            double extremeLuma = totalLumaDelta > 0 ? maxLuma : minLuma;
            int optimalCount = totalLumaDelta == 0
                    ? pixelCount
                    : (int) Math.round(totalLumaDelta / (extremeLuma - background.luma()));
            activeCount = Math.max(activeCount, optimalCount);
        }
        activeCount = Math.min(activeCount, pixelCount);
        if (activeCount == pixelCount) return denseCell(active, average);

        double foregroundR = background.r() + deltaR / activeCount;
        double foregroundG = background.g() + deltaG / activeCount;
        double foregroundB = background.b() + deltaB / activeCount;
        double foregroundLuma = .2126 * foregroundR + .7152 * foregroundG + .0722 * foregroundB;
        Comparator<LinearPixel> order = comparator(totalLumaDelta);
        pixels.sort(
                ditherMode == FullColorDither.SERPENTINE
                        ? Comparator.comparingInt(LinearPixel::serpIndex).reversed()
                        : order);
        if (ditherMode != FullColorDither.DISABLED) {
            diffuseLuma(pixels, foregroundLuma, background.luma());
            pixels.sort(order);
        }
        activate(active, pixels, activeCount);
        return new ProcessedCell(active, rgbFromLinear(new LinearRgb(foregroundR, foregroundG, foregroundB)), average);
    }

    private static ProcessedCell processMergedCell(
            List<List<Rgb>> cell, Rgb background, MaskMode mode, FullColorDither ditherMode) {
        Yuv backgroundYuv = rgbToYuv(background);
        List<LinearPixel> pixels = new ArrayList<>();
        double totalLumaDelta = 0;
        double totalUpower = 0;
        double totalVpower = 0;
        double totalR = 0;
        double totalG = 0;
        double totalB = 0;
        for (int y = 0; y < cell.size(); y++) {
            for (int x = 0; x < cell.get(y).size(); x++) {
                LinearRgb linear = linearRgb(cell.get(y).get(x));
                Yuv pixel = rgbToYuv(linear);
                pixels.add(new LinearPixel(
                        pixel.y() - backgroundYuv.y(),
                        pixel.y(),
                        x,
                        y,
                        y * cell.get(y).size() + (y % 2 == 0 ? x : cell.get(y).size() - 1 - x)));
                totalLumaDelta += pixel.y() - backgroundYuv.y();
                totalUpower += pixel.y() * pixel.u();
                totalVpower += pixel.y() * pixel.v();
                totalR += linear.r();
                totalG += linear.g();
                totalB += linear.b();
            }
        }
        Comparator<LinearPixel> order = comparator(totalLumaDelta);
        pixels.sort(order);
        if (ditherMode == FullColorDither.SERPENTINE) {
            pixels.sort(Comparator.comparingInt(LinearPixel::serpIndex).reversed());
        }

        double foregroundDelta = totalLumaDelta > 0
                ? Math.min(totalLumaDelta, pixels.getFirst().delta())
                : Math.max(totalLumaDelta, pixels.getFirst().delta());
        double realLuma = 0;
        double realU = .210;
        double realV = .474;
        double accumulatedDelta = 0;
        int activeCount = 0;
        for (LinearPixel ignored : pixels) {
            double currentDifference = Math.abs(accumulatedDelta - totalLumaDelta);
            double nextDifference = Math.abs(accumulatedDelta + foregroundDelta - totalLumaDelta);
            if (activeCount == 0
                    || mode == MaskMode.DENSE
                    || (mode != MaskMode.SPARSE && nextDifference < currentDifference)
                    || !inSrgb(realLuma, realU, realV)) {
                activeCount++;
                accumulatedDelta += foregroundDelta;
                realLuma = backgroundYuv.y() + totalLumaDelta / activeCount;
                realU = (totalUpower - (pixels.size() - activeCount) * backgroundYuv.y() * backgroundYuv.u())
                        / (activeCount * Math.max(.00001, realLuma));
                realV = (totalVpower - (pixels.size() - activeCount) * backgroundYuv.y() * backgroundYuv.v())
                        / (activeCount * Math.max(.00001, realLuma));
            } else {
                break;
            }
        }

        if (ditherMode != FullColorDither.DISABLED) {
            diffuseLuma(pixels, realLuma, backgroundYuv.y());
            pixels.sort(order);
        }

        boolean[][] active = new boolean[cell.size()][cell.getFirst().size()];
        activate(active, pixels, activeCount);
        Yuv foreground =
                new Yuv(Math.clamp(realLuma, 0, 1), Math.clamp(realU, .125, .450), Math.clamp(realV, .156, .547));
        return new ProcessedCell(
                active,
                rgbFromYuv(foreground),
                new LinearRgb(totalR / pixels.size(), totalG / pixels.size(), totalB / pixels.size()));
    }

    private static ProcessedCell denseCell(boolean[][] active, LinearRgb average) {
        for (boolean[] row : active) Arrays.fill(row, true);
        return new ProcessedCell(active, rgbFromLinear(average), average);
    }

    private static int minimumActivePixels(double delta, double background) {
        if (delta == 0) return 0;
        return (int) Math.ceil(delta < 0 ? -delta / background : delta / (1 - background));
    }

    private static void diffuseLuma(List<LinearPixel> pixels, double foreground, double background) {
        for (int i = 0; i < pixels.size() - 1; i++) {
            double foregroundError = pixels.get(i).luma() - foreground;
            double backgroundError = pixels.get(i).luma() - background;
            boolean useForeground = Math.abs(foregroundError) < Math.abs(backgroundError);
            pixels.get(i).setLuma(useForeground ? foreground : background);
            pixels.get(i + 1).setLuma(pixels.get(i + 1).luma() + (useForeground ? foregroundError : backgroundError));
        }
    }

    private static void activate(boolean[][] active, List<LinearPixel> pixels, int count) {
        for (int i = 0; i < count; i++) {
            LinearPixel pixel = pixels.get(i);
            active[pixel.yIndex()][pixel.xIndex()] = true;
        }
    }

    private static Comparator<LinearPixel> comparator(double totalDelta) {
        return (a, b) -> a.luma() == b.luma()
                ? Integer.compare(a.serpIndex(), b.serpIndex())
                : Double.compare(totalDelta > 0 ? b.luma() : a.luma(), totalDelta > 0 ? a.luma() : b.luma());
    }

    private static List<List<Rgb>> cell(Rgb[][] pixels, int x, int y) {
        List<List<Rgb>> result = new ArrayList<>();
        for (int dy = 0; dy < CELL_HEIGHT && y + dy < pixels.length; dy++) {
            List<Rgb> row = new ArrayList<>();
            for (int dx = 0; dx < CELL_WIDTH && x + dx < pixels[y + dy].length; dx++) row.add(pixels[y + dy][x + dx]);
            result.add(row);
        }
        return result;
    }

    private static Rgb[][] resize(BufferedImage image, int width, int height) {
        BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = scaled.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, width, height);
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.drawImage(image, 0, 0, width, height, null);
        } finally {
            graphics.dispose();
        }

        Rgb[][] pixels = new Rgb[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = scaled.getRGB(x, y);
                pixels[y][x] = new Rgb(rgb >> 16 & 0xFF, rgb >> 8 & 0xFF, rgb & 0xFF);
            }
        }
        return pixels;
    }

    private static boolean bilevelPixel(double luma, double threshold, double dither, int x, int y) {
        if (dither <= 0) return luma == threshold ? noise(x, y) < .5 : luma > threshold;
        double standardDeviations = (luma - threshold) / dither;
        if (Math.abs(standardDeviations) >= 2) return standardDeviations > 0;
        if (standardDeviations == 0) return noise(x, y) < .5;
        double probability = Math.pow(standardDeviations, 3) / (8 * Math.abs(standardDeviations)) + .5;
        return noise(x, y) < probability;
    }

    private static double noise(int x, int y) {
        int value = x * 1103515245 + y * 12345 + 0x6D2B79F5;
        value ^= value >>> 16;
        return (value & 0x7FFFFFFF) / (double) Integer.MAX_VALUE;
    }

    private static List<String> gridToChars(boolean[][] grid) {
        List<String> output = new ArrayList<>();
        for (int y = 0; y < grid.length; y += CELL_HEIGHT) {
            StringBuilder line = new StringBuilder();
            for (int x = 0; x < grid[y].length; x += CELL_WIDTH) {
                int mask = 0;
                int bit = 128;
                for (int dy = 0; dy < CELL_HEIGHT; dy++) {
                    for (int dx = 0; dx < CELL_WIDTH; dx++) {
                        if (y + dy < grid.length && x + dx < grid[y + dy].length && grid[y + dy][x + dx]) {
                            mask |= bit >> dx;
                        }
                    }
                    bit /= 4;
                }
                line.append(mapping(mask));
            }
            output.add(line.toString());
        }
        return output;
    }

    private static String mapping(int mask) {
        String glyph = new String(Character.toChars(CELL_CODEPOINTS[mask]));
        String push = mask == 85 || mask == 170 ? NARROW_PUSH_CHARACTER : ".";
        return CELL_DOTS[mask] == 1 ? push + glyph : CELL_DOTS[mask] == 2 ? glyph + push : glyph;
    }

    private static Yuv rgbToYuv(Rgb rgb) {
        return rgbToYuv(linearRgb(rgb));
    }

    private static Yuv rgbToYuv(LinearRgb rgb) {
        double x = rgb.r() * .4124564 + rgb.g() * .3575761 + rgb.b() * .1804375;
        double y = rgb.luma();
        double z = rgb.r() * .0193339 + rgb.g() * .119192 + rgb.b() * .9503041;
        double denominator = x + 15 * y + 3 * z;
        return denominator == 0 ? new Yuv(0, .2009, .4610) : new Yuv(y, 4 * x / denominator, 9 * y / denominator);
    }

    private static LinearRgb linearRgb(Rgb rgb) {
        return new LinearRgb(linear(rgb.r() / 255d), linear(rgb.g() / 255d), linear(rgb.b() / 255d));
    }

    private static double linear(double value) {
        return value > .04045 ? Math.pow((value + .055) / 1.055, 2.4) : value / 12.92;
    }

    private static int rgbFromYuv(Yuv color) {
        if (color.y() <= 0) return 0;
        double denominator = 4 * color.v();
        double x = color.y() * 9 * color.u() / denominator;
        double z = color.y() * (12 - 3 * color.u() - 20 * color.v()) / denominator;
        double r = x * 3.2404542 + color.y() * -1.5371385 + z * -.4985314;
        double g = x * -.969266 + color.y() * 1.8760108 + z * .041556;
        double b = x * .0556434 + color.y() * -.2040259 + z * 1.0572252;
        return channel(r) << 16 | channel(g) << 8 | channel(b);
    }

    private static int rgbFromLinear(LinearRgb color) {
        return channel(Math.clamp(color.r(), 0, 1)) << 16
                | channel(Math.clamp(color.g(), 0, 1)) << 8
                | channel(Math.clamp(color.b(), 0, 1));
    }

    private static int channel(double value) {
        double gamma = value > .0031308 ? 1.055 * Math.pow(value, 1 / 2.4) - .055 : 12.92 * value;
        return Math.clamp((int) Math.round(gamma * 255), 0, 255);
    }

    private static boolean inSrgb(double y, double u, double v) {
        if (y <= 0) return y == 0;
        if (y > 1 || v <= .001) return false;
        double denominator = 4 * v;
        double x = y * 9 * u / denominator;
        double z = y * (12 - 3 * u - 20 * v) / denominator;
        double r = x * 3.2404542 + y * -1.5371385 + z * -.4985314;
        double g = x * -.969266 + y * 1.8760108 + z * .041556;
        double b = x * .0556434 + y * -.2040259 + z * 1.0572252;
        return r >= -1e-6 && r <= 1 + 1e-6 && g >= -1e-6 && g <= 1 + 1e-6 && b >= -1e-6 && b <= 1 + 1e-6;
    }

    private static Delta deltas(LinearRgb first, LinearRgb second, LinearRgb background) {
        Lab a = lab(first);
        Lab b = lab(second);
        Lab bg = lab(background);
        double dl = a.l() - b.l();
        double du = a.u() - b.u();
        double dv = a.v() - b.v();
        double spherical = Math.sqrt(dl * dl + du * du + dv * dv);
        double aL = a.l() - bg.l(), aU = a.u() - bg.u(), aV = a.v() - bg.v();
        double bL = b.l() - bg.l(), bU = b.u() - bg.u(), bV = b.v() - bg.v();
        double magA = Math.sqrt(aL * aL + aU * aU + aV * aV);
        double magB = Math.sqrt(bL * bL + bU * bU + bV * bV);
        if (magA < 1e-5 || magB < 1e-5) return new Delta(spherical, 0);
        double cosine = Math.clamp((aL * bL + aU * bU + aV * bV) / (magA * magB), -1, 1);
        return new Delta(spherical, Math.toDegrees(Math.acos(cosine)));
    }

    private static Lab lab(LinearRgb color) {
        double x = color.r() * .4124564 + color.g() * .3575761 + color.b() * .1804375;
        double y = color.luma();
        double z = color.r() * .0193339 + color.g() * .119192 + color.b() * .9503041;
        double l = y <= .008856 ? y * 903.3 : 116 * Math.cbrt(y) - 16;
        double denominator = x + 15 * y + 3 * z;
        double u = denominator > .00001 ? 4 * x / denominator : .19783001;
        double v = denominator > .00001 ? 9 * y / denominator : .46831999;
        return l <= 0 ? new Lab(0, 0, 0) : new Lab(l, 13 * l * (u - .19783001), 13 * l * (v - .46831999));
    }

    private static Style colorStyle(int color) {
        ChatFormatting named =
                switch (color & 0xFFFFFF) {
                    case 0x000000 -> ChatFormatting.BLACK;
                    case 0xFFAA00 -> ChatFormatting.GOLD;
                    case 0xAAAAAA -> ChatFormatting.GRAY;
                    case 0x5555FF -> ChatFormatting.BLUE;
                    case 0x55FF55 -> ChatFormatting.GREEN;
                    case 0x55FFFF -> ChatFormatting.AQUA;
                    case 0xFF5555 -> ChatFormatting.RED;
                    case 0xFFFF55 -> ChatFormatting.YELLOW;
                    case 0xFFFFFF -> ChatFormatting.WHITE;
                    default -> null;
                };
        return Style.EMPTY.withColor(named == null ? TextColor.fromRgb(color) : TextColor.fromLegacyFormat(named));
    }

    private static MutableComponent appendSegment(MutableComponent line, String text, int color) {
        int start = 0;
        while (start < text.length()) {
            int blank = text.indexOf('\u2002', start);
            int end = blank < 0 ? text.length() : blank;
            if (end > start) {
                line = append(line, text.substring(start, end), color, false);
            }
            if (blank < 0) break;
            int blankEnd = blank + 1;
            while (blankEnd < text.length() && text.charAt(blankEnd) == '\u2002') blankEnd++;
            line = append(line, " ".repeat(blankEnd - blank), color, true);
            line = append(line, "\u200c", color, false);
            start = blankEnd;
        }
        return line;
    }

    private static MutableComponent append(MutableComponent line, String text, int color, boolean bold) {
        Style style = colorStyle(color)
                .withBold(bold || line != null && line.getStyle().isBold() ? bold : null);
        MutableComponent segment = Component.literal(text).withStyle(style);
        return line == null ? segment.withStyle(value -> value.withItalic(false)) : line.append(segment);
    }

    public record Result(List<Component> lines, int width, int height) {
        public long textDisplayNbtUsage() {
            var encoded = ComponentSerialization.CODEC
                    .encodeStart(
                            NbtOps.INSTANCE,
                            TextComponentCompactor.compact(
                                    RichTextDocument.fromLines(lines).toComponent()))
                    .result();
            if (encoded.isEmpty()) return -1;
            CompoundTag entity = new CompoundTag();
            entity.put("text", encoded.get());
            return RawItemDataUtil.networkNbtUsage(entity);
        }

        public int serializedLength() {
            return 2
                    + Math.max(0, lines.size() - 1)
                    + lines.stream()
                            .mapToInt(line -> ComponentSerialization.CODEC
                                    .encodeStart(JsonOps.INSTANCE, TextComponentCompactor.compact(line))
                                    .result()
                                    .map(value -> value.toString().length())
                                    .orElse(0))
                            .sum();
        }
    }

    public record Options(
            ColorMode colorMode,
            int backgroundColor,
            int textColor,
            double threshold,
            double bilevelDither,
            boolean invert,
            MaskMode maskMode,
            double compression,
            FullColorDither fullColorDither) {}

    public enum ColorMode {
        FULL_COLOR,
        BILEVEL
    }

    public enum MaskMode {
        OPTIMAL,
        SPARSE,
        DENSE
    }

    public enum FullColorDither {
        DISABLED,
        SERPENTINE,
        LIGHT_TO_DARK
    }

    private record Rgb(int r, int g, int b) {}

    private record LinearRgb(double r, double g, double b) {
        private double luma() {
            return .2126 * r + .7152 * g + .0722 * b;
        }

        private LinearRgb include(LinearRgb value, int count) {
            return new LinearRgb(r + (value.r - r) / count, g + (value.g - g) / count, b + (value.b - b) / count);
        }
    }

    private record Yuv(double y, double u, double v) {}

    private record Lab(double l, double u, double v) {}

    private record Delta(double spherical, double angular) {}

    private record ProcessedCell(boolean[][] active, int color, LinearRgb average) {}

    private static final class LinearPixel {
        private final double delta;
        private double luma;
        private final int xIndex;
        private final int yIndex;
        private final int serpIndex;

        private LinearPixel(double delta, double luma, int xIndex, int yIndex, int serpIndex) {
            this.delta = delta;
            this.luma = luma;
            this.xIndex = xIndex;
            this.yIndex = yIndex;
            this.serpIndex = serpIndex;
        }

        private double delta() {
            return delta;
        }

        private double luma() {
            return luma;
        }

        private void setLuma(double value) {
            luma = value;
        }

        private int xIndex() {
            return xIndex;
        }

        private int yIndex() {
            return yIndex;
        }

        private int serpIndex() {
            return serpIndex;
        }
    }
}
