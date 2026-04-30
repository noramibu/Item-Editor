package me.noramibu.itemeditor.editor.text;

import me.noramibu.itemeditor.util.ColorInterpolationUtil;
import me.noramibu.itemeditor.util.TextComponentUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.ObjectContents;
import net.minecraft.network.chat.contents.objects.ObjectInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;

public final class RichTextDocument {

    private static final String OBJECT_LAYOUT_PLACEHOLDER = "■";
    private final List<Segment> segments = new ArrayList<>();

    public static RichTextDocument empty() {
        return new RichTextDocument();
    }

    public static RichTextDocument fromPlainText(String text) {
        RichTextDocument document = new RichTextDocument();
        String normalized = normalizeLineBreaks(text);
        if (!normalized.isEmpty()) {
            document.segments.add(new Segment(normalized, RichTextStyle.EMPTY));
        }
        return document;
    }

    public static RichTextDocument fromMarkup(String markup) {
        String normalized = normalizeLineBreaks(markup);
        if (TextComponentUtil.containsEventToken(normalized)) {
            return fromPlainText(normalized);
        }
        return fromComponent(TextComponentUtil.parseMarkup(normalized));
    }

    public static RichTextDocument fromComponent(Component component) {
        return fromComponent(component, ObjectContentMode.TOKEN_TEXT);
    }

    public static RichTextDocument fromComponentForLayout(Component component) {
        return fromComponent(component, ObjectContentMode.LAYOUT_PLACEHOLDER);
    }

    private static RichTextDocument fromComponent(Component component, ObjectContentMode mode) {
        RichTextDocument document = new RichTextDocument();
        if (component == null) {
            return document;
        }

        collectSegments(component, Style.EMPTY, document.segments, mode);

        document.normalize();
        return document;
    }

    private static String normalizeLineBreaks(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        return input.replace("\r\n", "\n").replace('\r', '\n');
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
        for (int segmentIndex = 0; segmentIndex < this.segments.size(); segmentIndex++) {
            Segment segment = this.segments.get(segmentIndex);
            int start = offset;
            int end = offset + segment.text().length();
            if (index < end) {
                return segment.style();
            }
            if (index == end) {
                if (segmentIndex + 1 < this.segments.size()) {
                    Segment next = this.segments.get(segmentIndex + 1);
                    boolean nextStartsWithNewline = !next.text().isEmpty() && next.text().charAt(0) == '\n';
                    if (nextStartsWithNewline) {
                        return segment.style();
                    }

                    RichTextStyle nextStyle = next.style();
                    if (nextStyle.equals(RichTextStyle.EMPTY) && !segment.style().equals(RichTextStyle.EMPTY)) {
                        return segment.style();
                    }
                    return nextStyle;
                }
                return segment.style();
            }
            offset = start + segment.text().length();
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
            String text = segment.text();
            for (int index = 0; index < text.length();) {
                int tokenLength = TextComponentUtil.structuredTokenLengthAt(text, index);
                if (tokenLength > 0) {
                    updated.add(new Segment(text.substring(index, index + tokenLength), segment.style()));
                    index += tokenLength;
                    continue;
                }

                int codePoint = text.codePointAt(index);
                if (codePoint == '\n') {
                    int charCount = Character.charCount(codePoint);
                    updated.add(new Segment(text.substring(index, index + charCount), segment.style()));
                    index += charCount;
                    continue;
                }

                float progress = gradientSteps == 1 ? 0f : (float) colorIndex / (gradientSteps - 1);
                updated.add(new Segment(
                        Character.toString(codePoint),
                        segment.style().withColor(ColorInterpolationUtil.interpolateRgb(startColor, endColor, progress))
                ));
                colorIndex++;
                index += Character.charCount(codePoint);
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

    public RichTextDocument slice(int start, int end) {
        IntRange range = this.clampRange(start, end);
        RichTextDocument document = new RichTextDocument();
        if (range.start() >= range.end()) {
            return document;
        }
        document.segments.addAll(this.sliceSegments(range.start(), range.end()));
        document.normalize();
        return document;
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
            String text = segment.text();
            for (int index = 0; index < text.length();) {
                int tokenLength = TextComponentUtil.structuredTokenLengthAt(text, index);
                if (tokenLength > 0) {
                    index += tokenLength;
                    continue;
                }

                int codePoint = text.codePointAt(index);
                if (codePoint != '\n') {
                    count++;
                }
                index += Character.charCount(codePoint);
            }
        }
        return count;
    }

    private IntRange clampRange(int start, int end) {
        int clampedStart = Math.max(0, Math.min(start, this.length()));
        int clampedEnd = Math.max(clampedStart, Math.min(end, this.length()));
        return new IntRange(clampedStart, clampedEnd);
    }

    private static void collectSegments(Component component, Style parentStyle, List<Segment> out, ObjectContentMode mode) {
        ComponentContents contents = component.getContents();
        Style effectiveStyle = component.getStyle().applyTo(parentStyle);
        String text = textFromContents(contents, mode, effectiveStyle);
        if (!text.isEmpty()) {
            Style segmentStyle = effectiveStyle;
            if (mode == ObjectContentMode.TOKEN_TEXT && contents instanceof ObjectContents) {
                segmentStyle = objectTokenStyle(effectiveStyle);
            }
            out.add(new Segment(text, RichTextStyle.fromStyle(segmentStyle)));
        }
        for (Component sibling : component.getSiblings()) {
            collectSegments(sibling, effectiveStyle, out, mode);
        }
    }

    private static String textFromContents(ComponentContents contents, ObjectContentMode mode, Style effectiveStyle) {
        if (contents instanceof ObjectContents objectContents) {
            if (mode == ObjectContentMode.LAYOUT_PLACEHOLDER) {
                return OBJECT_LAYOUT_PLACEHOLDER;
            }
            ObjectInfo object = objectContents.contents();
            Component objectComponent = Component.object(object).withStyle(style -> style.withColor(effectiveStyle.getColor()));
            return TextComponentUtil.toMarkup(objectComponent);
        }
        StringBuilder plain = new StringBuilder();
        contents.visit((String chunk) -> {
            plain.append(chunk);
            return Optional.empty();
        });
        return plain.toString();
    }

    private static Style objectTokenStyle(Style style) {
        Style tokenStyle = Style.EMPTY;
        if (style.getShadowColor() != null) {
            tokenStyle = tokenStyle.withShadowColor(style.getShadowColor());
        }
        if (style.getClickEvent() != null) {
            tokenStyle = tokenStyle.withClickEvent(style.getClickEvent());
        }
        if (style.getHoverEvent() != null) {
            tokenStyle = tokenStyle.withHoverEvent(style.getHoverEvent());
        }
        return copyTrueDecorations(style, tokenStyle);
    }

    private static Style copyTrueDecorations(Style source, Style target) {
        if (source.isBold()) {
            target = target.withBold(true);
        }
        if (source.isItalic()) {
            target = target.withItalic(true);
        }
        if (source.isUnderlined()) {
            target = target.withUnderlined(true);
        }
        if (source.isStrikethrough()) {
            target = target.withStrikethrough(true);
        }
        if (source.isObfuscated()) {
            target = target.withObfuscated(true);
        }
        return target;
    }

    public record Segment(String text, RichTextStyle style) {
    }

    private record IntRange(int start, int end) {
    }

    private enum ObjectContentMode {
        TOKEN_TEXT,
        LAYOUT_PLACEHOLDER
    }
}
