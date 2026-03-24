package me.noramibu.itemeditor.editor.text;

import me.noramibu.itemeditor.util.ColorInterpolationUtil;
import me.noramibu.itemeditor.util.TextComponentUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.UnaryOperator;

public final class RichTextDocument {

    private final List<Segment> segments = new ArrayList<>();

    public static RichTextDocument empty() {
        return new RichTextDocument();
    }

    public static RichTextDocument fromPlainText(String text) {
        RichTextDocument document = new RichTextDocument();
        if (!text.isEmpty()) {
            document.segments.add(new Segment(text, RichTextStyle.EMPTY));
        }
        return document;
    }

    public static RichTextDocument fromMarkup(String markup) {
        return fromComponent(TextComponentUtil.parseMarkup(markup));
    }

    public static RichTextDocument fromComponent(Component component) {
        RichTextDocument document = new RichTextDocument();
        if (component == null) {
            return document;
        }

        for (Component flatComponent : component.toFlatList(Style.EMPTY)) {
            String text = flatComponent.getString();
            if (!text.isEmpty()) {
                document.segments.add(new Segment(text, RichTextStyle.fromStyle(flatComponent.getStyle())));
            }
        }

        document.normalize();
        return document;
    }

    public static RichTextDocument fromLines(List<Component> lines) {
        RichTextDocument document = new RichTextDocument();
        for (int index = 0; index < lines.size(); index++) {
            document.segments.addAll(fromComponent(lines.get(index)).segments);
            if (index < lines.size() - 1) {
                document.segments.add(new Segment("\n", RichTextStyle.EMPTY));
            }
        }
        document.normalize();
        return document;
    }

    public RichTextDocument copy() {
        RichTextDocument document = new RichTextDocument();
        document.segments.addAll(this.segments);
        return document;
    }

    public List<Segment> segments() {
        return Collections.unmodifiableList(this.segments);
    }

    public String plainText() {
        StringBuilder builder = new StringBuilder();
        for (Segment segment : this.segments) {
            builder.append(segment.text());
        }
        return builder.toString();
    }

    public int length() {
        int length = 0;
        for (Segment segment : this.segments) {
            length += segment.text().length();
        }
        return length;
    }

    public boolean isEmpty() {
        return this.segments.isEmpty();
    }

    public int logicalLineCount() {
        if (this.isEmpty()) {
            return 0;
        }
        int lines = 1;
        String text = this.plainText();
        for (int index = 0; index < text.length(); index++) {
            if (text.charAt(index) == '\n') {
                lines++;
            }
        }
        return lines;
    }

    public RichTextStyle insertionStyleAt(int index) {
        if (this.segments.isEmpty()) {
            return RichTextStyle.EMPTY;
        }

        if (index <= 0) {
            return this.segments.getFirst().style();
        }

        int offset = 0;
        for (Segment segment : this.segments) {
            int end = offset + segment.text().length();
            if (index <= end) {
                return segment.style();
            }
            offset = end;
        }

        return this.segments.getLast().style();
    }

    public void replace(int start, int end, String insertedText, RichTextStyle insertedStyle) {
        IntRange range = this.clampRange(start, end);

        List<Segment> updated = new ArrayList<>(this.sliceSegments(0, range.start()));
        if (!insertedText.isEmpty()) {
            updated.add(new Segment(insertedText, insertedStyle));
        }
        updated.addAll(this.sliceSegments(range.end(), this.length()));

        this.segments.clear();
        this.segments.addAll(updated);
        this.normalize();
    }

    public int replace(int start, int end, RichTextDocument insertedDocument) {
        IntRange range = this.clampRange(start, end);

        List<Segment> updated = new ArrayList<>(this.sliceSegments(0, range.start()));
        if (insertedDocument != null && !insertedDocument.segments.isEmpty()) {
            updated.addAll(insertedDocument.copy().segments);
        }
        updated.addAll(this.sliceSegments(range.end(), this.length()));

        this.segments.clear();
        this.segments.addAll(updated);
        this.normalize();
        return insertedDocument == null ? 0 : insertedDocument.length();
    }

    public void applyStyle(int start, int end, UnaryOperator<RichTextStyle> transformer) {
        IntRange range = this.clampRange(start, end);
        if (range.start() == range.end()) {
            return;
        }

        List<Segment> updated = new ArrayList<>(this.sliceSegments(0, range.start()));
        for (Segment segment : this.sliceSegments(range.start(), range.end())) {
            updated.add(new Segment(segment.text(), transformer.apply(segment.style())));
        }
        updated.addAll(this.sliceSegments(range.end(), this.length()));

        this.segments.clear();
        this.segments.addAll(updated);
        this.normalize();
    }

    public void applyGradient(int start, int end, int startColor, int endColor) {
        IntRange range = this.clampRange(start, end);
        if (range.start() == range.end()) {
            return;
        }

        List<Segment> selectedSegments = this.sliceSegments(range.start(), range.end());
        int gradientSteps = this.gradientCharacterCount(selectedSegments);
        if (gradientSteps <= 0) {
            return;
        }

        List<Segment> updated = new ArrayList<>(this.sliceSegments(0, range.start()));

        int colorIndex = 0;
        for (Segment segment : selectedSegments) {
            for (int index = 0; index < segment.text().length(); index++) {
                char character = segment.text().charAt(index);
                if (character == '\n') {
                    updated.add(new Segment("\n", segment.style()));
                    continue;
                }

                float progress = gradientSteps == 1 ? 0f : (float) colorIndex / (gradientSteps - 1);
                updated.add(new Segment(Character.toString(character), segment.style().withColor(ColorInterpolationUtil.interpolateRgb(startColor, endColor, progress))));
                colorIndex++;
            }
        }

        updated.addAll(this.sliceSegments(range.end(), this.length()));

        this.segments.clear();
        this.segments.addAll(updated);
        this.normalize();
    }

    public int transformText(int start, int end, UnaryOperator<String> transformer) {
        IntRange range = this.clampRange(start, end);
        if (range.start() == range.end()) {
            return 0;
        }

        List<Segment> updated = new ArrayList<>(this.sliceSegments(0, range.start()));

        int transformedLength = 0;
        for (Segment segment : this.sliceSegments(range.start(), range.end())) {
            String transformed = transformer.apply(segment.text());
            if (!transformed.isEmpty()) {
                updated.add(new Segment(transformed, segment.style()));
                transformedLength += transformed.length();
            }
        }

        updated.addAll(this.sliceSegments(range.end(), this.length()));

        this.segments.clear();
        this.segments.addAll(updated);
        this.normalize();
        return transformedLength;
    }

    public Component toComponent() {
        MutableComponent root = Component.empty();
        for (Segment segment : this.segments) {
            root.append(Component.literal(segment.text()).withStyle(segment.style().toStyle()));
        }
        return root;
    }

    public Component sliceToComponent(int start, int end) {
        MutableComponent root = Component.empty();
        for (Segment segment : this.sliceSegments(start, end)) {
            root.append(Component.literal(segment.text()).withStyle(segment.style().toStyle()));
        }
        return root;
    }

    public String toMarkup() {
        return TextComponentUtil.toMarkup(this.toComponent());
    }

    public List<Component> toLineComponents() {
        if (this.segments.isEmpty()) {
            return List.of();
        }

        List<Component> lines = new ArrayList<>();
        MutableComponent current = Component.empty();

        for (Segment segment : this.segments) {
            int lastBreak = 0;
            String text = segment.text();

            for (int index = 0; index < text.length(); index++) {
                if (text.charAt(index) != '\n') {
                    continue;
                }

                if (index > lastBreak) {
                    current.append(Component.literal(text.substring(lastBreak, index)).withStyle(segment.style().toStyle()));
                }
                lines.add(current);
                current = Component.empty();
                lastBreak = index + 1;
            }

            if (lastBreak < text.length()) {
                current.append(Component.literal(text.substring(lastBreak)).withStyle(segment.style().toStyle()));
            }
        }

        lines.add(current);
        return lines;
    }

    private List<Segment> sliceSegments(int start, int end) {
        if (start >= end || this.segments.isEmpty()) {
            return List.of();
        }

        List<Segment> result = new ArrayList<>();
        int offset = 0;

        for (Segment segment : this.segments) {
            int segmentEnd = offset + segment.text().length();
            if (segmentEnd <= start) {
                offset = segmentEnd;
                continue;
            }
            if (offset >= end) {
                break;
            }

            int localStart = Math.max(0, start - offset);
            int localEnd = Math.min(segment.text().length(), end - offset);
            if (localStart < localEnd) {
                result.add(new Segment(segment.text().substring(localStart, localEnd), segment.style()));
            }
            offset = segmentEnd;
        }

        return result;
    }

    private void normalize() {
        if (this.segments.isEmpty()) {
            return;
        }

        List<Segment> merged = new ArrayList<>();
        Segment current = null;

        for (Segment segment : this.segments) {
            if (segment.text().isEmpty()) {
                continue;
            }

            if (current != null && current.style().equals(segment.style())) {
                current = new Segment(current.text() + segment.text(), current.style());
                merged.set(merged.size() - 1, current);
            } else {
                current = segment;
                merged.add(segment);
            }
        }

        this.segments.clear();
        this.segments.addAll(merged);
    }

    private int gradientCharacterCount(List<Segment> segments) {
        int count = 0;
        for (Segment segment : segments) {
            for (int index = 0; index < segment.text().length(); index++) {
                if (segment.text().charAt(index) != '\n') {
                    count++;
                }
            }
        }
        return count;
    }

    private IntRange clampRange(int start, int end) {
        int clampedStart = Math.max(0, Math.min(start, this.length()));
        int clampedEnd = Math.max(clampedStart, Math.min(end, this.length()));
        return new IntRange(clampedStart, clampedEnd);
    }

    public record Segment(String text, RichTextStyle style) {
    }

    private record IntRange(int start, int end) {
    }
}
