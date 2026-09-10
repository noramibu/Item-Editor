package me.noramibu.itemeditor.editor.text;

import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

public record RichTextStyle(
        Integer color,
        Integer shadowColor,
        boolean bold,
        boolean italic,
        boolean underlined,
        boolean strikethrough,
        boolean obfuscated,
        FontDescription font) {

    public RichTextStyle(
            Integer color,
            Integer shadowColor,
            boolean bold,
            boolean italic,
            boolean underlined,
            boolean strikethrough,
            boolean obfuscated) {
        this(color, shadowColor, bold, italic, underlined, strikethrough, obfuscated, FontDescription.DEFAULT);
    }

    public static final RichTextStyle EMPTY = new RichTextStyle(null, null, false, false, false, false, false);

    public static RichTextStyle fromStyle(Style style) {
        return new RichTextStyle(
                style.getColor() == null ? null : style.getColor().getValue(),
                style.getShadowColor(),
                style.isBold(),
                style.isItalic(),
                style.isUnderlined(),
                style.isStrikethrough(),
                style.isObfuscated(),
                style.getFont());
    }

    public static Style objectStyle(Style source) {
        return objectStyle(source, true);
    }

    public static Style objectTokenStyle(Style source) {
        return objectStyle(source, false);
    }

    private static Style objectStyle(Style source, boolean includeColor) {
        RichTextStyle value = fromStyle(source);
        Style target = (includeColor ? value : value.withColor(null)).toStyle();
        if (source.getClickEvent() != null) target = target.withClickEvent(source.getClickEvent());
        if (source.getHoverEvent() != null) target = target.withHoverEvent(source.getHoverEvent());
        return target;
    }

    public Style toStyle() {
        Style style = Style.EMPTY;
        if (this.color != null) {
            style = style.withColor(TextColor.fromRgb(this.color));
        }
        if (this.shadowColor != null) {
            style = style.withShadowColor(this.shadowColor);
        }
        if (this.bold) style = style.withBold(true);
        if (this.italic) style = style.withItalic(true);
        if (this.underlined) style = style.withUnderlined(true);
        if (this.strikethrough) style = style.withStrikethrough(true);
        if (this.obfuscated) style = style.withObfuscated(true);
        if (!FontDescription.DEFAULT.equals(this.font)) style = style.withFont(this.font);
        return style;
    }

    public RichTextStyle withColor(Integer color) {
        return new RichTextStyle(
                color,
                this.shadowColor,
                this.bold,
                this.italic,
                this.underlined,
                this.strikethrough,
                this.obfuscated,
                this.font);
    }

    public RichTextStyle withFont(FontDescription font) {
        return new RichTextStyle(color, shadowColor, bold, italic, underlined, strikethrough, obfuscated, font);
    }

    public RichTextStyle withShadowColor(Integer shadowColor) {
        return new RichTextStyle(
                this.color,
                shadowColor,
                this.bold,
                this.italic,
                this.underlined,
                this.strikethrough,
                this.obfuscated,
                this.font);
    }

    public RichTextStyle toggleBold() {
        return new RichTextStyle(
                this.color,
                this.shadowColor,
                !this.bold,
                this.italic,
                this.underlined,
                this.strikethrough,
                this.obfuscated,
                this.font);
    }

    public RichTextStyle toggleItalic() {
        return new RichTextStyle(
                this.color,
                this.shadowColor,
                this.bold,
                !this.italic,
                this.underlined,
                this.strikethrough,
                this.obfuscated,
                this.font);
    }

    public RichTextStyle toggleUnderlined() {
        return new RichTextStyle(
                this.color,
                this.shadowColor,
                this.bold,
                this.italic,
                !this.underlined,
                this.strikethrough,
                this.obfuscated,
                this.font);
    }

    public RichTextStyle toggleStrikethrough() {
        return new RichTextStyle(
                this.color,
                this.shadowColor,
                this.bold,
                this.italic,
                this.underlined,
                !this.strikethrough,
                this.obfuscated,
                this.font);
    }

    public RichTextStyle toggleObfuscated() {
        return new RichTextStyle(
                this.color,
                this.shadowColor,
                this.bold,
                this.italic,
                this.underlined,
                this.strikethrough,
                !this.obfuscated,
                this.font);
    }
}
