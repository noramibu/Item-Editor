package me.noramibu.itemeditor.ui.component;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.CheckboxComponent;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.component.TextAreaComponent;
import io.wispforest.owo.ui.component.TextBoxComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.core.Color;
import io.wispforest.owo.ui.core.HorizontalAlignment;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.Surface;
import io.wispforest.owo.ui.core.VerticalAlignment;
import io.wispforest.owo.ui.core.Component;
import me.noramibu.itemeditor.util.ItemEditorText;
import net.minecraft.client.Minecraft;

import java.util.function.Consumer;

public final class UiFactory {

    private static final Surface CARD_SURFACE = Surface.flat(0xAA141A22).and(Surface.outline(0xFF2F3945));
    private static final Surface SUB_CARD_SURFACE = Surface.flat(0xAA1B222B).and(Surface.outline(0xFF414B56));
    private static final Surface EDITOR_FRAME_SURFACE = Surface.flat(0xB0121822).and(Surface.outline(0xFF3F4D63));
    private static final int FIELD_TEXT_MIN = 120;
    private static final int FIELD_TEXT_MAX = 300;
    private static final int BODY_TEXT_MIN = 130;
    private static final int BODY_TEXT_MAX = 320;

    private UiFactory() {
    }

    public static FlowLayout column() {
        return Containers.verticalFlow(Sizing.fill(100), Sizing.content()).gap(4);
    }

    public static FlowLayout row() {
        FlowLayout row = Containers.ltrTextFlow(Sizing.fill(100), Sizing.content());
        row.gap(4);
        row.verticalAlignment(VerticalAlignment.CENTER);
        return row;
    }

    public static FlowLayout card() {
        FlowLayout card = column();
        card.padding(Insets.of(6));
        card.surface(CARD_SURFACE);
        card.allowOverflow(false);
        return card;
    }

    public static FlowLayout section(net.minecraft.network.chat.Component title, net.minecraft.network.chat.Component description) {
        FlowLayout section = card();
        section.child(title(title));
        if (!description.getString().isBlank()) {
            section.child(muted(description));
        }
        return section;
    }

    public static FlowLayout field(net.minecraft.network.chat.Component label, net.minecraft.network.chat.Component helpText, Component input) {
        FlowLayout field = column().gap(3);
        field.horizontalSizing(Sizing.fill(100));
        int textWidth = responsiveFieldTextWidth();
        field.child(title(label).shadow(false).maxWidth(textWidth));
        if (!helpText.getString().isBlank()) {
            field.child(muted(helpText, textWidth));
        }
        field.child(input.horizontalSizing(Sizing.fill(100)));
        return field;
    }

    public static FlowLayout subCard() {
        FlowLayout card = column();
        card.padding(Insets.of(5));
        card.surface(SUB_CARD_SURFACE);
        card.allowOverflow(false);
        return card;
    }

    public static LabelComponent title(String text) {
        return title(net.minecraft.network.chat.Component.literal(text));
    }

    public static LabelComponent title(net.minecraft.network.chat.Component text) {
        return Components.label(text)
                .color(Color.ofRgb(0xF2F5F8))
                .shadow(true);
    }

    public static LabelComponent muted(String text) {
        return muted(text, responsiveBodyTextWidth());
    }

    public static LabelComponent muted(String text, int maxWidth) {
        return muted(net.minecraft.network.chat.Component.literal(text), maxWidth);
    }

    public static LabelComponent muted(net.minecraft.network.chat.Component text) {
        return muted(text, responsiveBodyTextWidth());
    }

    public static LabelComponent muted(net.minecraft.network.chat.Component text, int maxWidth) {
        return Components.label(text)
                .color(Color.ofRgb(0xA9B5C0))
                .maxWidth(maxWidth);
    }

    public static LabelComponent message(String text, int color) {
        return message(net.minecraft.network.chat.Component.literal(text), color);
    }

    public static LabelComponent message(net.minecraft.network.chat.Component text, int color) {
        return Components.label(text)
                .color(Color.ofRgb(color))
                .maxWidth(responsiveBodyTextWidth());
    }

    public static ButtonComponent button(String text, Consumer<ButtonComponent> onPress) {
        return button(net.minecraft.network.chat.Component.literal(text), onPress);
    }

    public static ButtonComponent button(net.minecraft.network.chat.Component text, Consumer<ButtonComponent> onPress) {
        return Components.button(text, onPress);
    }

    public static ButtonComponent pickerButton(net.minecraft.network.chat.Component text, int width, Consumer<ButtonComponent> onPress) {
        ButtonComponent button = button(text, onPress);
        if (width > 0) {
            button.horizontalSizing(Sizing.fixed(width));
        } else {
            button.horizontalSizing(Sizing.fill(100));
        }
        return button;
    }

    public static FlowLayout pickerField(
            net.minecraft.network.chat.Component label,
            net.minecraft.network.chat.Component helpText,
            net.minecraft.network.chat.Component buttonText,
            int buttonWidth,
            Consumer<ButtonComponent> onPress
    ) {
        return field(label, helpText, pickerButton(buttonText, buttonWidth, onPress));
    }

    public static FlowLayout removableSubCard(net.minecraft.network.chat.Component title, Runnable onRemove) {
        return reorderableSubCard(title, false, null, false, null, onRemove);
    }

    public static FlowLayout reorderableSubCard(
            net.minecraft.network.chat.Component title,
            boolean canMoveUp,
            Runnable onMoveUp,
            boolean canMoveDown,
            Runnable onMoveDown,
            Runnable onRemove
    ) {
        FlowLayout card = subCard();
        card.child(reorderableHeader(title, canMoveUp, onMoveUp, canMoveDown, onMoveDown, onRemove));
        return card;
    }

    public static FlowLayout reorderableHeader(
            net.minecraft.network.chat.Component title,
            boolean canMoveUp,
            Runnable onMoveUp,
            boolean canMoveDown,
            Runnable onMoveDown,
            Runnable onRemove
    ) {
        FlowLayout header = row();
        header.child(title(title).shadow(false));
        if (onMoveUp != null) {
            header.child(actionButton(ItemEditorText.tr("common.up"), canMoveUp, onMoveUp));
        }
        if (onMoveDown != null) {
            header.child(actionButton(ItemEditorText.tr("common.down"), canMoveDown, onMoveDown));
        }
        if (onRemove != null) {
            header.child(actionButton(ItemEditorText.tr("common.remove"), true, onRemove));
        }
        return header;
    }

    public static net.minecraft.network.chat.Component fitToWidth(net.minecraft.network.chat.Component text, int maxPixelWidth) {
        if (maxPixelWidth <= 0) {
            return net.minecraft.network.chat.Component.empty();
        }

        Minecraft minecraft = Minecraft.getInstance();
        String raw = text.getString();
        if (minecraft.font.width(raw) <= maxPixelWidth) {
            return text;
        }

        String ellipsis = "...";
        int ellipsisWidth = minecraft.font.width(ellipsis);
        if (maxPixelWidth <= ellipsisWidth) {
            return net.minecraft.network.chat.Component.literal(ellipsis);
        }

        String shortened = minecraft.font.plainSubstrByWidth(raw, Math.max(0, maxPixelWidth - ellipsisWidth)).trim();
        if (shortened.isEmpty()) {
            return net.minecraft.network.chat.Component.literal(ellipsis);
        }
        return net.minecraft.network.chat.Component.literal(shortened + ellipsis);
    }

    public static TextBoxComponent textBox(String value, Consumer<String> onChanged) {
        TextBoxComponent box = Components.textBox(Sizing.fill(100), value);
        box.onChanged().subscribe(onChanged::accept);
        return box;
    }

    public static TextAreaComponent textArea(String value, int height, Consumer<String> onChanged) {
        TextAreaComponent textArea = Components.textArea(Sizing.fill(100), Sizing.fixed(height), value);
        textArea.onChanged().subscribe(onChanged::accept);
        textArea.displayCharCount(true);
        return textArea;
    }

    public static CheckboxComponent checkbox(net.minecraft.network.chat.Component text, boolean checked, Consumer<Boolean> onChanged) {
        CheckboxComponent checkbox = Components.checkbox(text);
        checkbox.checked(checked);
        checkbox.onChanged(onChanged);
        return checkbox;
    }

    public static FlowLayout centeredCard(int width) {
        FlowLayout card = card();
        card.horizontalSizing(Sizing.fixed(DialogUiUtil.dialogWidth(width)));
        card.horizontalAlignment(HorizontalAlignment.CENTER);
        return card;
    }

    public static FlowLayout framedEditorCard() {
        FlowLayout frame = subCard();
        frame.padding(Insets.of(5));
        frame.surface(EDITOR_FRAME_SURFACE);
        return frame;
    }

    public static int responsiveFieldTextWidth() {
        return responsiveWidth(0.18, FIELD_TEXT_MIN, FIELD_TEXT_MAX);
    }

    public static int responsiveBodyTextWidth() {
        return responsiveWidth(0.22, BODY_TEXT_MIN, BODY_TEXT_MAX);
    }

    private static int responsiveWidth(double ratio, int min, int max) {
        Minecraft minecraft = Minecraft.getInstance();
        int scaledWidth = minecraft.getWindow().getGuiScaledWidth();
        int responsive = (int) Math.round(scaledWidth * ratio);
        return Math.max(min, Math.min(max, responsive));
    }

    private static ButtonComponent actionButton(net.minecraft.network.chat.Component label, boolean enabled, Runnable onPress) {
        ButtonComponent button = button(label, component -> onPress.run());
        button.horizontalSizing(Sizing.content());
        button.active(enabled);
        return button;
    }
}
