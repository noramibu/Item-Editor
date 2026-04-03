package me.noramibu.itemeditor.ui.panel.specialdata;

import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.StackLayout;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.Surface;
import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.editor.text.RichTextDocument;
import me.noramibu.itemeditor.ui.component.DyeColorSelectorSection;
import me.noramibu.itemeditor.ui.component.PickerFieldFactory;
import me.noramibu.itemeditor.ui.component.PixelCanvasPreview;
import me.noramibu.itemeditor.ui.component.StyledTextFieldSection;
import me.noramibu.itemeditor.ui.component.UiFactory;
import me.noramibu.itemeditor.util.ItemEditorCapabilities;
import me.noramibu.itemeditor.util.ItemEditorText;
import me.noramibu.itemeditor.util.TextComponentUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.HangingSignItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class SignSpecialDataSection {

    private static final int SIGN_LINE_COUNT = 4;
    private static final int BOARD_PIXEL_SIZE = 2;
    private static final SignPreviewLayout STANDING_LAYOUT = new SignPreviewLayout(90, 10, 49, 26, 5, 3, 5, 3);
    private static final SignPreviewLayout HANGING_LAYOUT = new SignPreviewLayout(60, 9, 37, 30, 10, 6, 10, 6);

    private SignSpecialDataSection() {
    }

    public static boolean supports(ItemStack stack) {
        return ItemEditorCapabilities.supportsSignData(stack);
    }

    public static FlowLayout build(SpecialDataPanelContext context) {
        ItemEditorState.SignData signData = context.special().sign;
        SignPreviewLayout layout = resolveLayout(context.originalStack());
        FlowLayout section = UiFactory.section(ItemEditorText.tr("special.sign.title"), Component.empty());
        section.child(UiFactory.checkbox(ItemEditorText.tr("special.sign.waxed"), signData.waxed, context.bindToggle(value -> signData.waxed = value)));

        SignBoardStyle activeStyle = resolveBoardStyle(signData.boardStyle);
        section.child(PickerFieldFactory.dropdownField(
                context,
                ItemEditorText.tr("special.sign.board_style"),
                Component.empty(),
                ItemEditorText.tr(activeStyle.labelKey()),
                180,
                Arrays.asList(SignBoardStyle.values()),
                style -> ItemEditorText.str(style.labelKey()),
                style -> context.mutateRefresh(() -> signData.boardStyle = style.id())
        ));

        section.child(buildSignSideSection(context, ItemEditorText.tr("special.sign.front"), signData.front, activeStyle, layout));
        section.child(buildSignSideSection(context, ItemEditorText.tr("special.sign.back"), signData.back, activeStyle, layout));
        return section;
    }

    private static FlowLayout buildSignSideSection(
            SpecialDataPanelContext context,
            Component title,
            ItemEditorState.SignSideDraft sideDraft,
            SignBoardStyle boardStyle,
            SignPreviewLayout layout
    ) {
        ensureSignLineCount(sideDraft);

        FlowLayout card = UiFactory.subCard();
        card.child(UiFactory.title(title).shadow(false));

        card.child(DyeColorSelectorSection.build(
                context,
                ItemEditorText.tr("special.sign.color"),
                Component.empty(),
                ItemEditorText.tr("special.sign.color"),
                sideDraft.color,
                180,
                null,
                color -> sideDraft.color = color.name()
        ));
        card.child(UiFactory.checkbox(
                ItemEditorText.tr("special.sign.glowing"),
                sideDraft.glowing,
                value -> context.mutateRefresh(() -> sideDraft.glowing = value)
        ));

        SignPreviewWidgets preview = buildPreviewWidgets(sideDraft, boardStyle, layout);
        StyledTextFieldSection.BoundEditor editorSection = StyledTextFieldSection.create(
                context.screen(),
                documentFromSideDraft(sideDraft),
                Sizing.fill(100),
                Sizing.fixed(92),
                ItemEditorText.str("special.sign.placeholder"),
                StyledTextFieldSection.StylePreset.bookMetadata(false),
                ItemEditorText.str("special.sign.color_dialog"),
                ItemEditorText.str("special.sign.gradient_dialog"),
                "",
                "",
                null,
                document -> validateSignDocument(document, sideDraft, layout),
                document -> {
                    context.mutate(() -> {
                        applyDocumentToSideDraft(sideDraft, document);
                        refreshPreview(preview, sideDraft);
                    });
                }
        );

        FlowLayout editorFrame = UiFactory.framedEditorCard();
        editorFrame.child(editorSection.toolbar());
        editorFrame.child(editorSection.editor());
        editorFrame.child(editorSection.validation());

        card.child(UiFactory.field(ItemEditorText.tr("special.sign.editor"), Component.empty(), editorFrame));
        card.child(preview.card());

        return card;
    }

    private static String validateSignDocument(
            RichTextDocument document,
            ItemEditorState.SignSideDraft currentDraft,
            SignPreviewLayout layout
    ) {
        RichTextDocument baseline = documentFromSideDraft(currentDraft);
        int baselineLineCount = baseline.logicalLineCount();
        int candidateLineCount = document.logicalLineCount();

        if (candidateLineCount > SIGN_LINE_COUNT && candidateLineCount > baselineLineCount) {
            return ItemEditorText.str("special.sign.lines_limit", SIGN_LINE_COUNT);
        }

        Minecraft minecraft = Minecraft.getInstance();
        List<Component> candidateLines = document.toLineComponents();
        List<Component> baselineLines = baseline.toLineComponents();

        for (int index = 0; index < candidateLines.size(); index++) {
            int width = minecraft.font.width(candidateLines.get(index));
            if (width <= layout.maxTextLineWidth()) {
                continue;
            }

            int baselineWidth = index < baselineLines.size() ? minecraft.font.width(baselineLines.get(index)) : 0;
            if (width > baselineWidth) {
                return ItemEditorText.str("special.sign.line_width_limit", index + 1, layout.maxTextLineWidth());
            }
        }

        return null;
    }

    private static RichTextDocument documentFromSideDraft(ItemEditorState.SignSideDraft sideDraft) {
        ensureSignLineCount(sideDraft);
        List<Component> lines = new ArrayList<>(SIGN_LINE_COUNT);
        for (int index = 0; index < SIGN_LINE_COUNT; index++) {
            lines.add(TextComponentUtil.parseMarkup(Objects.toString(sideDraft.lines.get(index), "")));
        }
        return RichTextDocument.fromLines(lines);
    }

    private static void applyDocumentToSideDraft(ItemEditorState.SignSideDraft sideDraft, RichTextDocument document) {
        sideDraft.lines.clear();
        List<Component> lines = document.toLineComponents();
        for (int index = 0; index < SIGN_LINE_COUNT; index++) {
            Component lineComponent = index < lines.size() ? lines.get(index) : Component.empty();
            sideDraft.lines.add(TextComponentUtil.toMarkup(lineComponent));
        }
    }

    private static SignPreviewWidgets buildPreviewWidgets(ItemEditorState.SignSideDraft sideDraft, SignBoardStyle boardStyle, SignPreviewLayout layout) {
        FlowLayout previewCard = UiFactory.subCard();
        previewCard.child(UiFactory.title(ItemEditorText.tr("screen.preview")).shadow(false));

        StackLayout signFace = Containers.stack(Sizing.content(), Sizing.content());
        signFace.child(buildBoardTexture(boardStyle, layout));

        FlowLayout textLayer = UiFactory.column();
        textLayer.gap(Math.max(0, layout.lineHeight() - Minecraft.getInstance().font.lineHeight));
        textLayer.horizontalSizing(Sizing.fixed(layout.boardWidth()));
        textLayer.verticalSizing(Sizing.fixed(layout.boardHeight()));
        textLayer.padding(Insets.of(layout.paddingTop(), layout.paddingRight(), layout.paddingBottom(), layout.paddingLeft()));

        List<LabelComponent> lines = new ArrayList<>(SIGN_LINE_COUNT);
        for (int index = 0; index < SIGN_LINE_COUNT; index++) {
            LabelComponent line = Components.label(Component.literal(" "))
                    .maxWidth(layout.maxTextLineWidth());
            textLayer.child(line);
            lines.add(line);
        }

        signFace.child(textLayer);
        previewCard.child(signFace);
        SignPreviewWidgets widgets = new SignPreviewWidgets(previewCard, lines);
        refreshPreview(widgets, sideDraft);
        return widgets;
    }

    private static FlowLayout buildBoardTexture(SignBoardStyle style, SignPreviewLayout layout) {
        int[][] pixels = PixelCanvasPreview.fill(layout.boardGridWidth(), layout.boardGridHeight(), style.baseColor());
        for (int y = 0; y < layout.boardGridHeight(); y++) {
            int grainColor = style.grainColors()[y % style.grainColors().length];
            for (int x = 0; x < layout.boardGridWidth(); x++) {
                pixels[y][x] = grainColor;
            }
        }
        return PixelCanvasPreview.fromColors(
                pixels,
                BOARD_PIXEL_SIZE,
                0,
                Insets.none(),
                Surface.flat(style.baseColor()).and(Surface.outline(style.outlineColor()))
        );
    }

    private static void refreshPreview(SignPreviewWidgets preview, ItemEditorState.SignSideDraft sideDraft) {
        ensureSignLineCount(sideDraft);
        DyeColor color = resolveSignColor(sideDraft.color);
        int textColor = color.getTextColor();

        for (int index = 0; index < SIGN_LINE_COUNT; index++) {
            String raw = Objects.toString(sideDraft.lines.get(index), "");
            Component line = TextComponentUtil.parseMarkup(raw);
            line = line.copy().withStyle(line.getStyle().withColor(textColor).withItalic(false));
            if (line.getString().isBlank()) {
                line = Component.literal(" ");
            }

            LabelComponent label = preview.lines().get(index);
            label.text(line);
            label.shadow(sideDraft.glowing);
        }
    }

    private static DyeColor resolveSignColor(String colorName) {
        if (colorName == null || colorName.isBlank()) {
            return DyeColor.BLACK;
        }
        try {
            return DyeColor.valueOf(colorName);
        } catch (IllegalArgumentException ignored) {
            return DyeColor.BLACK;
        }
    }

    private static SignBoardStyle resolveBoardStyle(String raw) {
        if (raw == null || raw.isBlank()) {
            return SignBoardStyle.OAK;
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        for (SignBoardStyle style : SignBoardStyle.values()) {
            if (style.id().equals(normalized)) {
                return style;
            }
        }
        return SignBoardStyle.OAK;
    }

    private static SignPreviewLayout resolveLayout(ItemStack stack) {
        return isHangingSign(stack) ? HANGING_LAYOUT : STANDING_LAYOUT;
    }

    private static boolean isHangingSign(ItemStack stack) {
        if (stack.getItem() instanceof HangingSignItem) {
            return true;
        }
        CustomData blockEntityData = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (blockEntityData == null) {
            return false;
        }
        String blockEntityId = blockEntityData.copyTag().getStringOr("id", "").toLowerCase(Locale.ROOT);
        return blockEntityId.contains("hanging_sign");
    }

    private static void ensureSignLineCount(ItemEditorState.SignSideDraft sideDraft) {
        while (sideDraft.lines.size() < SIGN_LINE_COUNT) {
            sideDraft.lines.add("");
        }
        while (sideDraft.lines.size() > SIGN_LINE_COUNT) {
            sideDraft.lines.removeLast();
        }
    }

    private record SignPreviewWidgets(
            FlowLayout card,
            List<LabelComponent> lines
    ) {
    }

    private record SignPreviewLayout(
            int maxTextLineWidth,
            int lineHeight,
            int boardGridWidth,
            int boardGridHeight,
            int paddingTop,
            int paddingRight,
            int paddingBottom,
            int paddingLeft
    ) {
        int boardWidth() {
            return this.boardGridWidth * BOARD_PIXEL_SIZE;
        }

        int boardHeight() {
            return this.boardGridHeight * BOARD_PIXEL_SIZE;
        }
    }

    private enum SignBoardStyle {
        OAK(
                "oak",
                "special.sign.board_style.oak",
                0x8A663B,
                0x4A3219,
                new int[]{0x9E7747, 0x8E693E, 0xA57A4A, 0x86613A}
        ),
        SPRUCE(
                "spruce",
                "special.sign.board_style.spruce",
                0x6F5532,
                0x3A2B18,
                new int[]{0x7C5F39, 0x6A502E, 0x735732, 0x5F472A}
        );

        private final String id;
        private final String labelKey;
        private final int baseColor;
        private final int outlineColor;
        private final int[] grainColors;

        SignBoardStyle(String id, String labelKey, int baseColor, int outlineColor, int[] grainColors) {
            this.id = id;
            this.labelKey = labelKey;
            this.baseColor = baseColor;
            this.outlineColor = outlineColor;
            this.grainColors = grainColors;
        }

        public String id() {
            return this.id;
        }

        public String labelKey() {
            return this.labelKey;
        }

        public int baseColor() {
            return this.baseColor;
        }

        public int outlineColor() {
            return this.outlineColor;
        }

        public int[] grainColors() {
            return this.grainColors;
        }
    }
}
