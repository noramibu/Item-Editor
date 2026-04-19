package me.noramibu.itemeditor.ui.panel.specialdata;

import io.wispforest.owo.ui.component.UIComponents;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.VerticalAlignment;
import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.editor.text.RichTextDocument;
import me.noramibu.itemeditor.ui.component.DyeColorSelectorSection;
import me.noramibu.itemeditor.ui.component.PickerFieldFactory;
import me.noramibu.itemeditor.ui.component.StyledTextFieldSection;
import me.noramibu.itemeditor.ui.component.UiFactory;
import me.noramibu.itemeditor.util.ItemEditorCapabilities;
import me.noramibu.itemeditor.util.ItemEditorText;
import me.noramibu.itemeditor.util.TextComponentUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.HangingSignItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public final class SignSpecialDataSection {
    private static final double COMPACT_LAYOUT_SCALE_THRESHOLD = 3.0d;
    private static final int COMPACT_LAYOUT_WIDTH_THRESHOLD = 620;
    private static final int BOARD_STYLE_PICKER_WIDTH = 180;
    private static final int SIGN_EDITOR_VERTICAL_PADDING_BASE = 24;
    private static final int SIGN_PREVIEW_SIZE_MIN = 84;
    private static final int SIGN_PREVIEW_SIZE_MAX = 180;
    private static final int SIGN_PREVIEW_HINT_WIDTH = 220;
    private static final int SIGN_LINE_NUMBER_WIDTH = 18;
    private static final int SIGN_LINE_NUMBER_TOP_OFFSET = 6;
    private static final int SIGN_LINE_SLOT_HEIGHT = 9;

    private static final int SIGN_LINE_COUNT = 4;

    private SignSpecialDataSection() {
    }

    public static boolean supports(ItemStack stack) {
        return ItemEditorCapabilities.supportsSignData(stack);
    }

    public static FlowLayout build(SpecialDataPanelContext context) {
        ItemEditorState.SignData signData = context.special().sign;
        boolean hangingSign = isHangingSign(context.originalStack());
        boolean compactLayout = isCompactLayout(context);
        FlowLayout section = UiFactory.section(ItemEditorText.tr("special.sign.title"), Component.empty());
        section.child(UiFactory.checkbox(ItemEditorText.tr("special.sign.waxed"), signData.waxed, context.bindToggle(value -> signData.waxed = value)));

        SignBoardStyle activeStyle = resolveBoardStyle(signData.boardStyle);
        section.child(PickerFieldFactory.dropdownField(
                context,
                ItemEditorText.tr("special.sign.board_style"),
                Component.empty(),
                ItemEditorText.tr(activeStyle.labelKey()),
                compactLayout ? -1 : BOARD_STYLE_PICKER_WIDTH,
                Arrays.asList(SignBoardStyle.values()),
                style -> ItemEditorText.str(style.labelKey()),
                style -> context.mutateRefresh(() -> signData.boardStyle = style.id())
        ));

        section.child(buildSignSideSection(context, ItemEditorText.tr("special.sign.front"), signData.front, activeStyle, hangingSign));
        section.child(buildSignSideSection(context, ItemEditorText.tr("special.sign.back"), signData.back, activeStyle, hangingSign));
        return section;
    }

    private static FlowLayout buildSignSideSection(
            SpecialDataPanelContext context,
            Component title,
            ItemEditorState.SignSideDraft sideDraft,
            SignBoardStyle boardStyle,
            boolean hangingSign
    ) {
        boolean compactLayout = isCompactLayout(context);
        ensureSignLineCount(sideDraft);

        FlowLayout card = UiFactory.subCard();
        card.child(UiFactory.title(title).shadow(false));

        card.child(DyeColorSelectorSection.build(
                context,
                ItemEditorText.tr("special.sign.color"),
                Component.empty(),
                ItemEditorText.tr("special.sign.color"),
                sideDraft.color,
                compactLayout ? -1 : BOARD_STYLE_PICKER_WIDTH,
                null,
                color -> sideDraft.color = color.name()
        ));
        card.child(UiFactory.checkbox(
                ItemEditorText.tr("special.sign.glowing"),
                sideDraft.glowing,
                value -> context.mutateRefresh(() -> sideDraft.glowing = value)
        ));

        SignPreviewWidgets preview = buildPreviewWidgets(sideDraft, boardStyle, hangingSign);
        AtomicBoolean normalizingDocument = new AtomicBoolean(false);
        AtomicReference<StyledTextFieldSection.BoundEditor> editorRef = new AtomicReference<>();
        StyledTextFieldSection.BoundEditor editorSection = StyledTextFieldSection.create(
                context.screen(),
                documentFromSideDraft(sideDraft),
                Sizing.fill(100),
                Sizing.fixed(signEditorHeight()),
                ItemEditorText.str("special.sign.placeholder"),
                StyledTextFieldSection.StylePreset.bookMetadata(false),
                ItemEditorText.str("special.sign.color_dialog"),
                ItemEditorText.str("special.sign.gradient_dialog"),
                "",
                "",
                null,
                SignSpecialDataSection::validateSignDocument,
                document -> {
                    if (normalizingDocument.get()) {
                        return;
                    }
                    RichTextDocument normalized = normalizeSignDocument(document);
                    if (normalized.logicalLineCount() != document.logicalLineCount()) {
                        StyledTextFieldSection.BoundEditor editor = editorRef.get();
                        if (editor != null) {
                            normalizingDocument.set(true);
                            editor.editor().document(normalized);
                            normalizingDocument.set(false);
                        }
                    }
                    context.mutate(() -> {
                        applyDocumentToSideDraft(sideDraft, normalized);
                        double panelScrollOffset = context.screen().panelScrollOffset();
                        refreshPreview(preview, sideDraft);
                        context.screen().restorePanelScroll(panelScrollOffset);
                    });
                }
        );
        editorRef.set(editorSection);

        FlowLayout editorFrame = UiFactory.framedEditorCard();
        editorFrame.child(editorSection.toolbar());
        FlowLayout editorRow = UiFactory.row();
        editorRow.horizontalSizing(Sizing.fill(100));
        editorRow.verticalAlignment(VerticalAlignment.TOP);
        editorRow.child(buildLineNumberGutter());
        editorRow.child(editorSection.editor().horizontalSizing(Sizing.fill(100)));
        editorFrame.child(editorRow);
        editorFrame.child(editorSection.validation());

        card.child(UiFactory.field(ItemEditorText.tr("special.sign.editor"), Component.empty(), editorFrame));
        card.child(preview.card());

        return card;
    }

    private static String validateSignDocument(RichTextDocument document) {
        int candidateLineCount = document.logicalLineCount();
        if (candidateLineCount <= SIGN_LINE_COUNT) {
            return null;
        }

        List<Component> lines = document.toLineComponents();
        for (int index = SIGN_LINE_COUNT; index < lines.size(); index++) {
            Component overflow = lines.get(index);
            if (overflow != null && !overflow.getString().isBlank()) {
                return ItemEditorText.str("special.sign.lines_limit", SIGN_LINE_COUNT);
            }
        }

        return null;
    }

    private static RichTextDocument normalizeSignDocument(RichTextDocument document) {
        List<Component> lines = new ArrayList<>(document.toLineComponents());
        if (lines.size() > SIGN_LINE_COUNT) {
            lines = new ArrayList<>(lines.subList(0, SIGN_LINE_COUNT));
        }
        while (lines.size() < SIGN_LINE_COUNT) {
            lines.add(Component.empty());
        }
        return RichTextDocument.fromLines(lines);
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

    private static SignPreviewWidgets buildPreviewWidgets(
            ItemEditorState.SignSideDraft sideDraft,
            SignBoardStyle boardStyle,
            boolean hangingSign
    ) {
        FlowLayout previewCard = UiFactory.subCard();
        previewCard.child(UiFactory.title(ItemEditorText.tr("screen.preview")).shadow(false));
        FlowLayout previewRow = UiFactory.row();
        previewRow.child(buildRealSignPreview(sideDraft, boardStyle, hangingSign));
        previewRow.child(UiFactory.muted(ItemEditorText.tr("special.sign.preview_hint"), SIGN_PREVIEW_HINT_WIDTH));
        previewCard.child(previewRow);
        return new SignPreviewWidgets(previewCard, previewRow, boardStyle, hangingSign);
    }

    private static FlowLayout buildRealSignPreview(
            ItemEditorState.SignSideDraft sideDraft,
            SignBoardStyle boardStyle,
            boolean hangingSign
    ) {
        int previewSize = UiFactory.responsiveSquareSize(0.13, 0.24, SIGN_PREVIEW_SIZE_MIN, SIGN_PREVIEW_SIZE_MAX);
        Block previewBlock = boardStyle.previewBlock(hangingSign);
        BlockState previewState = orientedPreviewState(previewBlock.defaultBlockState());
        CompoundTag signTag = new CompoundTag();
        SignText sideText = buildSignText(sideDraft);
        // Mirror both sides for preview reliability regardless of the model side currently facing the camera.
        signTag.store("front_text", SignText.DIRECT_CODEC, sideText);
        signTag.store("back_text", SignText.DIRECT_CODEC, sideText);
        signTag.putBoolean("is_waxed", false);
        return UiFactory.row().child(UIComponents.block(previewState, signTag)
                .horizontalSizing(UiFactory.fixed(previewSize))
                .verticalSizing(UiFactory.fixed(previewSize)));
    }

    private static BlockState orientedPreviewState(BlockState state) {
        if (state.hasProperty(BlockStateProperties.FACING)) {
            state = state.setValue(BlockStateProperties.FACING, Direction.SOUTH);
        }
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            state = state.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH);
        }
        if (state.hasProperty(BlockStateProperties.ROTATION_16)) {
            state = state.setValue(BlockStateProperties.ROTATION_16, 8);
        }
        return state;
    }

    private static void refreshPreview(SignPreviewWidgets preview, ItemEditorState.SignSideDraft sideDraft) {
        ensureSignLineCount(sideDraft);
        preview.previewRow().clearChildren();
        preview.previewRow().child(buildRealSignPreview(sideDraft, preview.boardStyle(), preview.hangingSign()));
        preview.previewRow().child(UiFactory.muted(ItemEditorText.tr("special.sign.preview_hint"), SIGN_PREVIEW_HINT_WIDTH));
    }

    private static SignText buildSignText(ItemEditorState.SignSideDraft sideDraft) {
        SignText signText = new SignText()
                .setColor(resolveSignColor(sideDraft.color))
                .setHasGlowingText(sideDraft.glowing);
        for (int index = 0; index < SIGN_LINE_COUNT; index++) {
            String raw = Objects.toString(sideDraft.lines.get(index), "");
            signText = signText.setMessage(index, TextComponentUtil.parseMarkup(raw));
        }
        return signText;
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

    private static boolean isHangingSign(ItemStack stack) {
        if (stack.getItem() instanceof HangingSignItem) {
            return true;
        }
        TypedEntityData<BlockEntityType<?>> blockEntityData = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        return blockEntityData != null && blockEntityData.type() == BlockEntityType.HANGING_SIGN;
    }

    private static void ensureSignLineCount(ItemEditorState.SignSideDraft sideDraft) {
        while (sideDraft.lines.size() < SIGN_LINE_COUNT) {
            sideDraft.lines.add("");
        }
        while (sideDraft.lines.size() > SIGN_LINE_COUNT) {
            sideDraft.lines.removeLast();
        }
    }

    private static boolean isCompactLayout(SpecialDataPanelContext context) {
        return context.guiScale() >= COMPACT_LAYOUT_SCALE_THRESHOLD
                || context.panelWidthHint() < UiFactory.scaledPixels(COMPACT_LAYOUT_WIDTH_THRESHOLD);
    }

    private static FlowLayout buildLineNumberGutter() {
        FlowLayout gutter = UiFactory.column();
        gutter.horizontalSizing(UiFactory.fixed(SIGN_LINE_NUMBER_WIDTH));
        gutter.gap(0);
        for (int line = 1; line <= SIGN_LINE_COUNT; line++) {
            FlowLayout lineSlot = UiFactory.row();
            lineSlot.horizontalSizing(Sizing.fill(100));
            lineSlot.verticalSizing(UiFactory.fixed(SIGN_LINE_SLOT_HEIGHT));
            lineSlot.verticalAlignment(VerticalAlignment.TOP);
            lineSlot.child(UiFactory.muted(Component.literal(Integer.toString(line)), SIGN_LINE_NUMBER_WIDTH).shadow(false));
            gutter.child(lineSlot);
        }
        gutter.margins(Insets.top(UiFactory.scaledPixels(SIGN_LINE_NUMBER_TOP_OFFSET)));
        return gutter;
    }

    private static int signEditorHeight() {
        return (SIGN_LINE_COUNT * SIGN_LINE_SLOT_HEIGHT) + UiFactory.scaledPixels(SIGN_EDITOR_VERTICAL_PADDING_BASE);
    }

    private record SignPreviewWidgets(
            FlowLayout card,
            FlowLayout previewRow,
            SignBoardStyle boardStyle,
            boolean hangingSign
    ) {
    }

    private enum SignBoardStyle {
        OAK(
                "oak",
                "special.sign.board_style.oak",
                Blocks.OAK_WALL_SIGN,
                Blocks.OAK_WALL_HANGING_SIGN
        ),
        SPRUCE(
                "spruce",
                "special.sign.board_style.spruce",
                Blocks.SPRUCE_WALL_SIGN,
                Blocks.SPRUCE_WALL_HANGING_SIGN
        );

        private final String id;
        private final String labelKey;
        private final Block standingBlock;
        private final Block hangingBlock;

        SignBoardStyle(String id, String labelKey, Block standingBlock, Block hangingBlock) {
            this.id = id;
            this.labelKey = labelKey;
            this.standingBlock = standingBlock;
            this.hangingBlock = hangingBlock;
        }

        public String id() {
            return this.id;
        }

        public String labelKey() {
            return this.labelKey;
        }

        public Block previewBlock(boolean hangingSign) {
            return hangingSign ? this.hangingBlock : this.standingBlock;
        }
    }
}
