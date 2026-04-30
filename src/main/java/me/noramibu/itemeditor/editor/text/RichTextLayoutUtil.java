package me.noramibu.itemeditor.editor.text;

import me.noramibu.itemeditor.util.TextComponentUtil;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class RichTextLayoutUtil {
    private static final String TOKEN_PREFIX = "[ie:";
    private static final String TOKEN_CLICK_OPEN = "[ie:click:";
    private static final String TOKEN_CLICK_CLOSE = "[ie:/click]";
    private static final String TOKEN_HOVER_OPEN = "[ie:hover:";
    private static final String TOKEN_HOVER_CLOSE = "[ie:/hover]";
    private static final String OBJECT_LAYOUT_FALLBACK_TEXT = "■";
    private static final float OBJECT_LAYOUT_MIN_WIDTH = 8f;
    private static final float EVENT_TOKEN_WRAP_WIDTH = 0f;

    private RichTextLayoutUtil() {
    }

    public static List<LineLayout> layout(
            RichTextDocument document,
            Font font,
            int maxWidth,
            boolean collapseStructuredTokens
    ) {
        Objects.requireNonNull(document, "document");
        Objects.requireNonNull(font, "font");

        String source = document.plainText();
        int safeWidth = Math.max(1, maxWidth);
        if (!collapseStructuredTokens) {
            return layoutRawSourceInternal(
                    source,
                    safeWidth,
                    codePoint -> rawCodePointWidth(font, codePoint),
                    document::sliceToComponent,
                    false
            );
        }

        return layoutStructuredSourceInternal(
                source,
                safeWidth,
                codePoint -> rawCodePointWidth(font, codePoint),
                (fullText, tokenStart, tokenLength) -> objectTokenWidth(fullText, tokenStart, tokenLength, font),
                true,
                true,
                document::sliceToComponent
        );
    }

    public static List<LineLayout> layoutDocumentSource(
            RichTextDocument document,
            Font font,
            int maxWidth,
            boolean renderStructuredEvents,
            boolean renderStructuredObjects
    ) {
        Objects.requireNonNull(document, "document");
        Objects.requireNonNull(font, "font");
        if (!renderStructuredEvents && !renderStructuredObjects) {
            return layoutRawSourceInternal(
                    document.plainText(),
                    Math.max(1, maxWidth),
                    codePoint -> rawCodePointWidth(font, codePoint),
                    document::sliceToComponent,
                    true
            );
        }
        return layoutSourceInternal(
                document.plainText(),
                maxWidth,
                codePoint -> rawCodePointWidth(font, codePoint),
                (fullText, tokenStart, tokenLength) -> objectTokenWidth(fullText, tokenStart, tokenLength, font),
                renderStructuredEvents,
                renderStructuredObjects,
                (start, end) -> renderedDocumentComponentForRange(
                        document,
                        start,
                        end,
                        renderStructuredEvents,
                        renderStructuredObjects
                )
        );
    }

    public static LogicalMetrics logicalMetricsForEventPayload(String sourceText) {
        if (sourceText == null || sourceText.isEmpty()) {
            return new LogicalMetrics(0, 0);
        }
        int logicalChars = 0;
        int logicalLines = 1;

        for (int cursor = 0; cursor < sourceText.length(); ) {
            int tokenLength = logicalRenderableTokenLength(sourceText, cursor);
            if (tokenLength > 0) {
                if (TextComponentUtil.objectTokenLengthAt(sourceText, cursor) > 0) {
                    logicalChars++;
                }
                cursor += tokenLength;
                continue;
            }

            int codePoint = sourceText.codePointAt(cursor);
            if (codePoint == '\n') {
                logicalLines++;
            } else {
                logicalChars += Character.charCount(codePoint);
            }
            cursor += Character.charCount(codePoint);
        }
        return new LogicalMetrics(logicalChars, logicalLines);
    }

    public static List<EventOverlayRange> eventOverlayRanges(String sourceText) {
        if (sourceText == null || sourceText.isEmpty()) {
            return List.of();
        }
        List<EventOverlayRange> ranges = new ArrayList<>();
        ArrayDeque<EventState> clickStack = new ArrayDeque<>();
        ArrayDeque<EventState> hoverStack = new ArrayDeque<>();
        int rangeStart = -1;
        String rangeToken = "";
        boolean malformed = false;

        for (int cursor = 0; cursor < sourceText.length(); ) {
            int tokenLength = TextComponentUtil.structuredTokenLengthAt(sourceText, cursor);
            if (tokenLength > 0) {
                if (rangeStart >= 0 && cursor > rangeStart) {
                    ranges.add(new EventOverlayRange(rangeStart, cursor, rangeToken));
                    rangeStart = -1;
                }

                int tokenEnd = cursor + tokenLength;
                String token = sourceText.substring(cursor, tokenEnd);
                if (TOKEN_CLICK_CLOSE.equals(token)) {
                    if (!clickStack.isEmpty()) {
                        clickStack.removeLast();
                    } else {
                        malformed = true;
                        hoverStack.clear();
                    }
                } else if (TOKEN_HOVER_CLOSE.equals(token)) {
                    if (!hoverStack.isEmpty()) {
                        hoverStack.removeLast();
                    } else {
                        malformed = true;
                        clickStack.clear();
                    }
                } else if (token.startsWith(TOKEN_CLICK_OPEN)) {
                    clickStack.addLast(new EventState(token));
                } else if (token.startsWith(TOKEN_HOVER_OPEN)) {
                    hoverStack.addLast(new EventState(token));
                }

                EventState active = hoverStack.peekLast();
                if (active == null) {
                    active = clickStack.peekLast();
                }
                if (active != null) {
                    rangeStart = tokenEnd;
                    rangeToken = active.openToken();
                } else {
                    rangeToken = "";
                }
                cursor = tokenEnd;
                continue;
            }

            if (sourceText.startsWith(TOKEN_PREFIX, cursor)) {
                if (rangeStart >= 0 && cursor > rangeStart) {
                    ranges.add(new EventOverlayRange(rangeStart, cursor, rangeToken));
                }
                clickStack.clear();
                hoverStack.clear();
                rangeStart = -1;
                rangeToken = "";
            }

            EventState active = hoverStack.peekLast();
            if (active == null) {
                active = clickStack.peekLast();
            }
            if (active != null) {
                if (rangeStart < 0) {
                    rangeStart = cursor;
                    rangeToken = active.openToken();
                } else if (!rangeToken.equals(active.openToken())) {
                    ranges.add(new EventOverlayRange(rangeStart, cursor, rangeToken));
                    rangeStart = cursor;
                    rangeToken = active.openToken();
                }
            } else if (rangeStart >= 0) {
                ranges.add(new EventOverlayRange(rangeStart, cursor, rangeToken));
                rangeStart = -1;
                rangeToken = "";
            }

            int codePoint = sourceText.codePointAt(cursor);
            cursor += Character.charCount(codePoint);
        }

        if (!clickStack.isEmpty() || !hoverStack.isEmpty()) {
            malformed = true;
        }
        if (rangeStart >= 0 && rangeStart < sourceText.length()) {
            if (!malformed) {
                ranges.add(new EventOverlayRange(rangeStart, sourceText.length(), rangeToken));
            }
        }
        if (malformed) {
            return List.of();
        }
        return List.copyOf(ranges);
    }

    private static int logicalRenderableTokenLength(String sourceText, int cursor) {
        return TextComponentUtil.renderableTokenLengthAt(sourceText, cursor);
    }

    public static List<VisualSpan> selectionVisualSpans(
            String sourceText,
            LineLayout line,
            int selectionStart,
            int selectionEnd,
            boolean renderStructuredEvents,
            boolean renderStructuredObjects
    ) {
        if (line == null || selectionEnd <= line.start() || selectionStart >= line.end()) {
            return List.of();
        }
        int start = Math.max(selectionStart, line.start());
        int end = Math.min(selectionEnd, line.end());
        if (end <= start) {
            return List.of();
        }

        List<VisualSpan> spans = new ArrayList<>();
        for (VisualUnit unit : visualUnits(sourceText, line, renderStructuredEvents, renderStructuredObjects)) {
            if (unit.hidden() || !rangesOverlap(start, end, unit.start(), unit.end())) {
                continue;
            }
            addVisualSpan(spans, unit.startX(), unit.endX());
        }
        return List.copyOf(spans);
    }

    public static float caretVisualX(
            String sourceText,
            LineLayout line,
            int cursor,
            boolean renderStructuredEvents,
            boolean renderStructuredObjects
    ) {
        if (line == null) {
            return 0f;
        }
        int clamped = Math.clamp(cursor, line.start(), line.end());
        for (VisualUnit unit : visualUnits(sourceText, line, renderStructuredEvents, renderStructuredObjects)) {
            if (clamped < unit.start() || clamped > unit.end()) {
                continue;
            }
            if (unit.hidden()) {
                return unit.startX();
            }
            if (unit.atomic() && clamped > unit.start() && clamped < unit.end()) {
                int midpoint = unit.start() + Math.max(1, unit.end() - unit.start()) / 2;
                return clamped <= midpoint ? unit.startX() : unit.endX();
            }
            return line.xForPosition(clamped);
        }
        return line.xForPosition(clamped);
    }

    public static int positionForVisualX(
            String sourceText,
            LineLayout line,
            double x,
            boolean renderStructuredEvents,
            boolean renderStructuredObjects
    ) {
        if (line == null) {
            return 0;
        }
        List<VisualUnit> allUnits = visualUnits(sourceText, line, renderStructuredEvents, renderStructuredObjects);
        List<Integer> visibleIndices = new ArrayList<>();
        for (int index = 0; index < allUnits.size(); index++) {
            if (!allUnits.get(index).hidden()) {
                visibleIndices.add(index);
            }
        }
        if (visibleIndices.isEmpty()) {
            return x <= 0 ? line.start() : line.end();
        }

        int firstVisibleIndex = visibleIndices.getFirst();
        VisualUnit first = allUnits.get(firstVisibleIndex);
        if (x <= first.startX()) {
            return visualBoundaryStart(allUnits, firstVisibleIndex);
        }
        int lastVisibleIndex = visibleIndices.getLast();
        for (int visibleIndex : visibleIndices) {
            VisualUnit unit = allUnits.get(visibleIndex);
            if (x > unit.endX()) {
                continue;
            }
            float midpoint = (unit.startX() + unit.endX()) * 0.5f;
            if (x < midpoint) {
                return unit.start();
            }
            return visibleIndex == lastVisibleIndex ? visualBoundaryEnd(allUnits, visibleIndex) : unit.end();
        }

        return visualBoundaryEnd(allUnits, lastVisibleIndex);
    }

    public static SourceRange wordRangeAtVisualPosition(
            String sourceText,
            LineLayout line,
            int cursor,
            boolean renderStructuredEvents,
            boolean renderStructuredObjects
    ) {
        if (line == null) {
            return new SourceRange(0, 0);
        }
        List<VisualUnit> units = visualUnits(sourceText, line, renderStructuredEvents, renderStructuredObjects)
                .stream()
                .filter(unit -> !unit.hidden())
                .toList();
        if (units.isEmpty()) {
            return new SourceRange(line.start(), line.start());
        }

        int clamped = Math.clamp(cursor, line.start(), line.end());
        int selectedUnitIndex = -1;
        for (int index = 0; index < units.size(); index++) {
            VisualUnit unit = units.get(index);
            if ((clamped >= unit.start() && clamped < unit.end())
                    || (clamped == line.end() && index == units.size() - 1)) {
                selectedUnitIndex = index;
                break;
            }
        }
        if (selectedUnitIndex < 0) {
            selectedUnitIndex = nearestVisualUnitIndex(units, clamped);
        }

        VisualUnit selected = units.get(selectedUnitIndex);
        SourceRange selectedRange = new SourceRange(selected.start(), selected.end());
        if (selected.atomic()) {
            return selectedRange;
        }

        int selectedCodePoint = codePointAtUnit(sourceText, selected);
        if (isWordBoundaryCodePoint(selectedCodePoint)) {
            return selectedRange;
        }

        int startIndex = selectedUnitIndex;
        while (startIndex > 0) {
            VisualUnit previous = units.get(startIndex - 1);
            if (previous.atomic() || previous.end() != units.get(startIndex).start() || isWordBoundaryCodePoint(codePointAtUnit(sourceText, previous))) {
                break;
            }
            startIndex--;
        }

        int endIndex = selectedUnitIndex;
        while (endIndex + 1 < units.size()) {
            VisualUnit next = units.get(endIndex + 1);
            if (next.atomic() || units.get(endIndex).end() != next.start() || isWordBoundaryCodePoint(codePointAtUnit(sourceText, next))) {
                break;
            }
            endIndex++;
        }

        return new SourceRange(units.get(startIndex).start(), units.get(endIndex).end());
    }

    public static List<LineLayout> layoutRawPlainText(String sourceText, Font font, int maxWidth) {
        Objects.requireNonNull(font, "font");
        return layoutRawSourceInternal(
                sourceText,
                Math.max(1, maxWidth),
                codePoint -> rawCodePointWidth(font, codePoint),
                (start, end) -> Component.literal(safeSlice(sourceText, start, end)),
                false
        );
    }

    private static List<LineLayout> layoutSourceInternal(
            String sourceText,
            int maxWidth,
            CodePointWidthMeasurer codePointWidthMeasurer,
            ObjectTokenWidthResolver objectTokenWidthResolver,
            boolean renderStructuredEvents,
            boolean renderStructuredObjects,
            LineComponentFactory componentFactory
    ) {
        int safeWidth = Math.max(1, maxWidth);
        return layoutStructuredSourceInternal(
                sourceText,
                safeWidth,
                codePointWidthMeasurer,
                objectTokenWidthResolver,
                renderStructuredEvents,
                renderStructuredObjects,
                componentFactory
        );
    }

    private static List<LineLayout> layoutRawSourceInternal(
            String sourceText,
            int wrapWidth,
            CodePointWidthMeasurer codePointWidthMeasurer,
            LineComponentFactory componentFactory,
            boolean structuredTokenWrapCost
    ) {
        return layoutLogicalLines(
                sourceText,
                wrapWidth,
                componentFactory,
                (text, start, end) -> structuredTokenWrapCost
                        ? buildRawUnitsWithStructuredTokenWrapCost(text, start, end, codePointWidthMeasurer)
                        : buildRawUnits(text, start, end, codePointWidthMeasurer)
        );
    }

    private static List<LineLayout> layoutStructuredSourceInternal(
            String sourceText,
            int wrapWidth,
            CodePointWidthMeasurer codePointWidthMeasurer,
            ObjectTokenWidthResolver objectTokenWidthResolver,
            boolean renderStructuredEvents,
            boolean renderStructuredObjects,
            LineComponentFactory componentFactory
    ) {
        return layoutLogicalLines(
                sourceText,
                wrapWidth,
                componentFactory,
                (text, start, end) -> buildStructuredUnits(
                        text,
                        start,
                        end,
                        codePointWidthMeasurer,
                        objectTokenWidthResolver,
                        renderStructuredEvents,
                        renderStructuredObjects
                )
        );
    }

    private static List<LineLayout> layoutLogicalLines(
            String sourceText,
            int wrapWidth,
            LineComponentFactory componentFactory,
            LineUnitBuilder unitBuilder
    ) {
        String text = sourceText == null ? "" : sourceText;
        if (text.isEmpty()) {
            return List.of(emptyLine(0, 1));
        }

        List<LineLayout> lines = new ArrayList<>();
        int cursor = 0;
        int logicalLineNumber = 1;
        while (cursor < text.length()) {
            int newlineIndex = text.indexOf('\n', cursor);
            int logicalEnd = newlineIndex >= 0 ? newlineIndex : text.length();
            if (logicalEnd == cursor) {
                lines.add(emptyLine(cursor, logicalLineNumber));
            } else {
                List<Unit> units = unitBuilder.build(text, cursor, logicalEnd);
                wrapUnitsIntoLines(units, wrapWidth, componentFactory, lines, logicalLineNumber);
            }

            if (newlineIndex >= 0) {
                cursor = newlineIndex + 1;
                logicalLineNumber++;
            } else {
                cursor = text.length();
            }
        }

        if (text.charAt(text.length() - 1) == '\n') {
            lines.add(emptyLine(text.length(), logicalLineNumber));
        }
        return lines;
    }

    private static LineLayout emptyLine(int position, int logicalLineNumber) {
        return new LineLayout(position, position, Component.empty(), new int[]{position}, new float[]{0f}, logicalLineNumber);
    }

    private static List<Unit> buildRawUnits(
            String sourceText,
            int start,
            int end,
            CodePointWidthMeasurer codePointWidthMeasurer
    ) {
        List<Unit> units = new ArrayList<>();
        addRawUnits(sourceText, start, end, codePointWidthMeasurer, units, 1f, true);
        return units;
    }

    private static void addRawUnits(
            String sourceText,
            int start,
            int end,
            CodePointWidthMeasurer codePointWidthMeasurer,
            List<Unit> units,
            float layoutScale,
            boolean allowWrapBreaks
    ) {
        for (int index = start; index < end; ) {
            index = addMeasuredUnit(sourceText, index, codePointWidthMeasurer, units, layoutScale, allowWrapBreaks);
        }
    }

    private static List<Unit> buildRawUnitsWithStructuredTokenWrapCost(
            String sourceText,
            int start,
            int end,
            CodePointWidthMeasurer codePointWidthMeasurer
    ) {
        return buildUnitsWithStructuredTokenHandler(
                sourceText,
                start,
                end,
                codePointWidthMeasurer,
                (text, tokenStart, tokenEnd, measurer, units) -> {
                    if (!isEventTokenTokenBody(text, tokenStart, tokenEnd)) {
                        return tokenStart;
                    }
                    addRawUnits(text, tokenStart, tokenEnd, measurer, units, EVENT_TOKEN_WRAP_WIDTH, false);
                    return tokenEnd;
                }
        );
    }

    private static List<Unit> buildUnitsWithStructuredTokenHandler(
            String sourceText,
            int start,
            int end,
            CodePointWidthMeasurer codePointWidthMeasurer,
            StructuredTokenUnitHandler tokenHandler
    ) {
        List<Unit> units = new ArrayList<>();
        for (int index = start; index < end; ) {
            SourceRange tokenRange = structuredTokenRange(sourceText, index, end);
            if (tokenRange != null) {
                int next = tokenHandler.append(sourceText, index, tokenRange.end(), codePointWidthMeasurer, units);
                if (next > index) {
                    index = next;
                    continue;
                }
            }

            index = addRawUnit(sourceText, index, codePointWidthMeasurer, units);
        }
        return units;
    }

    private static List<Unit> buildStructuredUnits(
            String sourceText,
            int start,
            int end,
            CodePointWidthMeasurer codePointWidthMeasurer,
            ObjectTokenWidthResolver objectTokenWidthResolver,
            boolean renderStructuredEvents,
            boolean renderStructuredObjects
    ) {
        return buildUnitsWithStructuredTokenHandler(
                sourceText,
                start,
                end,
                codePointWidthMeasurer,
                (text, tokenStart, tokenEnd, measurer, units) -> {
                    int tokenLength = tokenEnd - tokenStart;
                    boolean objectToken = TextComponentUtil.objectTokenLengthAt(text, tokenStart) > 0;
                    boolean eventToken = isEventTokenTokenBody(text, tokenStart, tokenEnd);

                    boolean rendered = (objectToken && renderStructuredObjects)
                            || (eventToken && renderStructuredEvents);
                    boolean hiddenEvent = eventToken && renderStructuredEvents;

                    if (!rendered) {
                        addRawUnits(text, tokenStart, tokenEnd, measurer, units, 1f, true);
                        return tokenEnd;
                    }

                    float width;
                    if (objectToken) {
                        width = Math.max(
                                OBJECT_LAYOUT_MIN_WIDTH,
                                Math.max(1f, objectTokenWidthResolver.width(text, tokenStart, tokenLength))
                        );
                    } else {
                        width = 0f;
                    }
                    units.add(new Unit(tokenStart, tokenEnd, width, width, !hiddenEvent));
                    return tokenEnd;
                }
        );
    }

    private static int addRawUnit(
            String sourceText,
            int index,
            CodePointWidthMeasurer codePointWidthMeasurer,
            List<Unit> units
    ) {
        return addMeasuredUnit(sourceText, index, codePointWidthMeasurer, units, 1f, true);
    }

    private static int addMeasuredUnit(
            String sourceText,
            int index,
            CodePointWidthMeasurer codePointWidthMeasurer,
            List<Unit> units,
            float layoutScale,
            boolean allowWrapBreaks
    ) {
        int codePoint = sourceText.codePointAt(index);
        int next = index + Character.charCount(codePoint);
        float width = Math.max(0f, codePointWidthMeasurer.width(codePoint));
        units.add(new Unit(index, next, width * layoutScale, width, allowWrapBreaks && isWrapBreak(codePoint)));
        return next;
    }

    private static SourceRange structuredTokenRange(String sourceText, int index, int end) {
        int tokenLength = TextComponentUtil.structuredTokenLengthAt(sourceText, index);
        if (tokenLength <= 0 || index + tokenLength > end) {
            return null;
        }
        return new SourceRange(index, index + tokenLength);
    }

    private static void wrapUnitsIntoLines(
            List<Unit> units,
            int wrapWidth,
            LineComponentFactory componentFactory,
            List<LineLayout> out,
            int logicalLineNumber
    ) {
        if (units.isEmpty()) {
            return;
        }

        int unitStart = 0;
        while (unitStart < units.size()) {
            float width = 0f;
            int unitEndExclusive = unitStart;
            int lastBreakExclusive = -1;

            while (unitEndExclusive < units.size()) {
                Unit unit = units.get(unitEndExclusive);
                if (unitEndExclusive > unitStart && width + unit.layoutWidth() > wrapWidth) {
                    break;
                }
                width += unit.layoutWidth();
                unitEndExclusive++;
                if (unit.breakAfter()) {
                    lastBreakExclusive = unitEndExclusive;
                }
            }

            int lineEndExclusive = unitEndExclusive;
            if (unitEndExclusive < units.size() && lastBreakExclusive > unitStart) {
                lineEndExclusive = lastBreakExclusive;
            }

            out.add(buildLineFromUnits(units, unitStart, lineEndExclusive, componentFactory, logicalLineNumber));
            unitStart = lineEndExclusive;
        }
    }

    private static LineLayout buildLineFromUnits(
            List<Unit> units,
            int unitStart,
            int unitEndExclusive,
            LineComponentFactory componentFactory,
            int logicalLineNumber
    ) {
        Unit first = units.get(unitStart);
        Unit last = units.get(unitEndExclusive - 1);
        int start = first.start();
        int end = last.end();

        int[] positions = new int[(unitEndExclusive - unitStart) + 1];
        float[] boundaries = new float[(unitEndExclusive - unitStart) + 1];
        positions[0] = start;

        float width = 0f;
        int outputIndex = 1;
        for (int unitIndex = unitStart; unitIndex < unitEndExclusive; unitIndex++) {
            Unit unit = units.get(unitIndex);
            width += unit.visualWidth();
            positions[outputIndex] = unit.end();
            boundaries[outputIndex] = width;
            outputIndex++;
        }

        return new LineLayout(
                start,
                end,
                componentFactory.component(start, end),
                positions,
                boundaries,
                logicalLineNumber
        );
    }

    private static boolean isEventTokenTokenBody(String sourceText, int tokenStart, int tokenEndExclusive) {
        if (tokenEndExclusive - tokenStart <= TOKEN_PREFIX.length() + 1) {
            return false;
        }
        String token = sourceText.substring(tokenStart + TOKEN_PREFIX.length(), tokenEndExclusive - 1);
        return token.equals("/click")
                || token.equals("/hover")
                || token.startsWith("click:")
                || token.startsWith("hover:");
    }

    private static Component renderedComponentForRange(
            String sourceText,
            int start,
            int end,
            boolean renderStructuredEvents,
            boolean renderStructuredObjects
    ) {
        String text = sourceText == null ? "" : sourceText;
        int safeStart = Math.clamp(start, 0, text.length());
        int safeEnd = Math.clamp(end, safeStart, text.length());
        if (safeEnd <= safeStart) {
            return Component.empty();
        }
        if (!renderStructuredEvents && !renderStructuredObjects) {
            return Component.literal(text.substring(safeStart, safeEnd));
        }
        String prefix = legacyFormattingPrefixBefore(text, safeStart);
        return TextComponentUtil.parseMarkup(
                prefix + text.substring(safeStart, safeEnd),
                renderStructuredEvents,
                renderStructuredObjects
        );
    }

    private static Component renderedDocumentComponentForRange(
            RichTextDocument document,
            int start,
            int end,
            boolean renderStructuredEvents,
            boolean renderStructuredObjects
    ) {
        String sourceText = document.plainText();
        MutableComponent root = Component.empty();
        int offset = 0;
        for (RichTextDocument.Segment segment : document.segments()) {
            int segmentStart = offset;
            int segmentEnd = offset + segment.text().length();
            int overlapStart = Math.max(start, segmentStart);
            int overlapEnd = Math.min(end, segmentEnd);
            if (overlapStart < overlapEnd) {
                Component parsed = renderedComponentForRange(
                        sourceText,
                        overlapStart,
                        overlapEnd,
                        renderStructuredEvents,
                        renderStructuredObjects
                );
                root.append(copyWithBaseStyle(parsed, segment.style().toStyle()));
            }
            offset = segmentEnd;
            if (offset >= end) {
                break;
            }
        }
        return root;
    }

    private static MutableComponent copyWithBaseStyle(Component component, Style parentStyle) {
        Style effectiveStyle = component.getStyle().applyTo(parentStyle);
        MutableComponent copy = MutableComponent.create(component.getContents()).withStyle(effectiveStyle);
        for (Component sibling : component.getSiblings()) {
            copy.append(copyWithBaseStyle(sibling, effectiveStyle));
        }
        return copy;
    }

    private static String legacyFormattingPrefixBefore(String text, int offset) {
        if (text == null || text.isEmpty() || offset <= 0) {
            return "";
        }
        int lineStart = text.lastIndexOf('\n', Math.min(offset, text.length()) - 1) + 1;
        StringBuilder activeCodes = new StringBuilder();
        for (int index = lineStart; index < offset; index++) {
            char marker = text.charAt(index);
            if ((marker != '&' && marker != '§') || index + 1 >= offset) {
                continue;
            }
            char next = text.charAt(index + 1);
            if (next == marker) {
                index++;
                continue;
            }
            if ((next == 'x' || next == 'X') && index + 13 < offset && isLegacyHexSequence(text, index, marker)) {
                activeCodes.setLength(0);
                activeCodes.append(text, index, index + 14);
                index += 13;
                continue;
            }
            if (next == '#' && index + 7 < offset && isHexColor(text, index + 2, index + 8)) {
                activeCodes.setLength(0);
                activeCodes.append(text, index, index + 8);
                index += 7;
                continue;
            }
            if (next == '$' && index + 9 < offset && isHexColor(text, index + 2, index + 10)) {
                activeCodes.append(text, index, index + 10);
                index += 9;
                continue;
            }
            if (isLegacyFormattingCode(next)) {
                if (isLegacyColorOrReset(next)) {
                    activeCodes.setLength(0);
                }
                activeCodes.append(marker).append(next);
                index++;
            }
        }
        return activeCodes.toString();
    }

    private static boolean isLegacyHexSequence(String text, int start, char marker) {
        for (int digit = 0; digit < 6; digit++) {
            int markerIndex = start + 2 + digit * 2;
            int digitIndex = markerIndex + 1;
            if (markerIndex >= text.length()
                    || digitIndex >= text.length()
                    || text.charAt(markerIndex) != marker
                    || Character.digit(text.charAt(digitIndex), 16) < 0) {
                return false;
            }
        }
        return true;
    }

    private static boolean isHexColor(String text, int start, int end) {
        if (start < 0 || end > text.length() || end <= start) {
            return false;
        }
        for (int index = start; index < end; index++) {
            if (Character.digit(text.charAt(index), 16) < 0) {
                return false;
            }
        }
        return true;
    }

    private static boolean isLegacyFormattingCode(char code) {
        char normalized = Character.toLowerCase(code);
        return (normalized >= '0' && normalized <= '9')
                || (normalized >= 'a' && normalized <= 'f')
                || normalized == 'k'
                || normalized == 'l'
                || normalized == 'm'
                || normalized == 'n'
                || normalized == 'o'
                || normalized == 'r';
    }

    private static boolean isLegacyColorOrReset(char code) {
        char normalized = Character.toLowerCase(code);
        return (normalized >= '0' && normalized <= '9')
                || (normalized >= 'a' && normalized <= 'f')
                || normalized == 'r';
    }

    private static boolean isWrapBreak(int codePoint) {
        if (Character.isWhitespace(codePoint)) {
            return true;
        }
        return switch (codePoint) {
            case ',', ';', ':', ']', '}', ')', '/', '\\', '-', '_' -> true;
            default -> false;
        };
    }

    private static String safeSlice(String sourceText, int start, int end) {
        if (sourceText == null || sourceText.isEmpty()) {
            return "";
        }
        int safeStart = Math.clamp(start, 0, sourceText.length());
        int safeEnd = Math.clamp(end, safeStart, sourceText.length());
        return sourceText.substring(safeStart, safeEnd);
    }

    private static float rawCodePointWidth(Font font, int codePoint) {
        if (codePoint == '\n') {
            return 0f;
        }
        return font.width(Character.toString(codePoint));
    }

    private static float objectTokenWidth(String fullText, int tokenStart, int tokenLength, Font font) {
        float fallback = Math.max(OBJECT_LAYOUT_MIN_WIDTH, font.width(OBJECT_LAYOUT_FALLBACK_TEXT));
        try {
            String tokenMarkup = fullText.substring(tokenStart, tokenStart + tokenLength);
            int measuredWidth = font.width(TextComponentUtil.parseMarkup(tokenMarkup));
            return measuredWidth > 0 ? measuredWidth : fallback;
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    private static List<VisualUnit> visualUnits(
            String sourceText,
            LineLayout line,
            boolean renderStructuredEvents,
            boolean renderStructuredObjects
    ) {
        if (line == null || line.end() <= line.start()) {
            return List.of();
        }
        String text = sourceText == null ? "" : sourceText;
        if (text.isEmpty()) {
            return List.of();
        }
        int lineStart = Math.clamp(line.start(), 0, text.length());
        int lineEnd = Math.clamp(line.end(), lineStart, text.length());
        List<VisualUnit> units = new ArrayList<>();

        for (int cursor = lineStart; cursor < lineEnd; ) {
            int tokenLength = TextComponentUtil.structuredTokenLengthAt(text, cursor);
            if (tokenLength > 0 && cursor + tokenLength <= lineEnd) {
                int tokenEnd = cursor + tokenLength;
                boolean objectToken = TextComponentUtil.objectTokenLengthAt(text, cursor) > 0;
                boolean eventToken = isEventTokenTokenBody(text, cursor, tokenEnd);
                if (eventToken && renderStructuredEvents) {
                    units.add(new VisualUnit(
                            cursor,
                            tokenEnd,
                            line.xForPosition(cursor),
                            line.xForPosition(tokenEnd),
                            true,
                            false
                    ));
                    cursor = tokenEnd;
                    continue;
                }
                if (objectToken && renderStructuredObjects) {
                    units.add(new VisualUnit(
                            cursor,
                            tokenEnd,
                            line.xForPosition(cursor),
                            line.xForPosition(tokenEnd),
                            false,
                            true
                    ));
                    cursor = tokenEnd;
                    continue;
                }
            }

            int next = cursor + Character.charCount(text.codePointAt(cursor));
            units.add(new VisualUnit(
                    cursor,
                    next,
                    line.xForPosition(cursor),
                    line.xForPosition(next),
                    false,
                    false
            ));
            cursor = next;
        }
        return List.copyOf(units);
    }

    private static boolean rangesOverlap(int firstStart, int firstEnd, int secondStart, int secondEnd) {
        return firstStart < secondEnd && firstEnd > secondStart;
    }

    private static void addVisualSpan(List<VisualSpan> spans, float left, float right) {
        if (right <= left) {
            return;
        }
        if (!spans.isEmpty()) {
            VisualSpan previous = spans.getLast();
            if (Math.abs(previous.endX() - left) < 0.001f) {
                spans.set(spans.size() - 1, new VisualSpan(previous.startX(), right));
                return;
            }
        }
        spans.add(new VisualSpan(left, right));
    }

    private static int nearestVisualUnitIndex(List<VisualUnit> units, int cursor) {
        int nearest = 0;
        int nearestDistance = Integer.MAX_VALUE;
        for (int index = 0; index < units.size(); index++) {
            VisualUnit unit = units.get(index);
            int distance;
            if (cursor < unit.start()) {
                distance = unit.start() - cursor;
            } else if (cursor > unit.end()) {
                distance = cursor - unit.end();
            } else {
                return index;
            }
            if (distance < nearestDistance) {
                nearest = index;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    private static int codePointAtUnit(String sourceText, VisualUnit unit) {
        if (sourceText == null || sourceText.isEmpty() || unit.start() >= sourceText.length()) {
            return -1;
        }
        return sourceText.codePointAt(unit.start());
    }

    private static boolean isWordBoundaryCodePoint(int codePoint) {
        return !Character.isLetterOrDigit(codePoint)
                && codePoint != '_'
                && codePoint != '-'
                && codePoint != ':'
                && codePoint != '/'
                && codePoint != '.';
    }

    private static int visualBoundaryStart(List<VisualUnit> units, int visibleIndex) {
        int start = units.get(visibleIndex).start();
        for (int index = visibleIndex - 1; index >= 0; index--) {
            VisualUnit previous = units.get(index);
            if (!previous.hidden() || previous.end() != start) {
                break;
            }
            start = previous.start();
        }
        return start;
    }

    private static int visualBoundaryEnd(List<VisualUnit> units, int visibleIndex) {
        int end = units.get(visibleIndex).end();
        for (int index = visibleIndex + 1; index < units.size(); index++) {
            VisualUnit next = units.get(index);
            if (!next.hidden() || next.start() != end) {
                break;
            }
            end = next.end();
        }
        return end;
    }

    private record Unit(int start, int end, float layoutWidth, float visualWidth, boolean breakAfter) {
    }

    private record EventState(String openToken) {
    }

    private record VisualUnit(int start, int end, float startX, float endX, boolean hidden, boolean atomic) {
    }

    public record VisualSpan(float startX, float endX) {
    }

    public record SourceRange(int start, int end) {
    }

    public record LineLayout(int start, int end, Component component, int[] positions, float[] boundaries, int logicalLineNumber) {

        public LineLayout(int start, int end, Component component, int[] positions, float[] boundaries) {
            this(start, end, component, positions, boundaries, 1);
        }

        public float xForPosition(int position) {
            if (position <= this.start) {
                return 0f;
            }

            for (int index = 1; index < this.positions.length; index++) {
                if (position <= this.positions[index]) {
                    return this.boundaries[index];
                }
            }

            return this.boundaries[this.boundaries.length - 1];
        }
    }

    public record LogicalMetrics(int charCount, int lineCount) {
    }

    public record EventOverlayRange(int start, int end, String openToken) {
    }

    @FunctionalInterface
    private interface CodePointWidthMeasurer {
        float width(int codePoint);
    }

    @FunctionalInterface
    private interface ObjectTokenWidthResolver {
        float width(String fullText, int tokenStart, int tokenLength);
    }

    @FunctionalInterface
    private interface LineComponentFactory {
        Component component(int start, int end);
    }

    @FunctionalInterface
    private interface LineUnitBuilder {
        List<Unit> build(String sourceText, int start, int end);
    }

    @FunctionalInterface
    private interface StructuredTokenUnitHandler {
        int append(
                String sourceText,
                int tokenStart,
                int tokenEnd,
                CodePointWidthMeasurer codePointWidthMeasurer,
                List<Unit> units
        );
    }
}
