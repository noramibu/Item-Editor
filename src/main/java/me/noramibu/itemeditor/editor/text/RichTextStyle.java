package me.noramibu.itemeditor.editor.text;

import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

public record RichTextStyle(
        Integer color,
        Integer shadowColor,
        boolean bold,
        boolean italic,
        boolean underlined,
        boolean strikethrough,
        boolean obfuscated
) {

    public static final RichTextStyle EMPTY = new RichTextStyle(null, null, false, false, false, false, false);

    public static RichTextStyle fromStyle(Style style) {
        return new RichTextStyle(
                style.getColor() == null ? null : style.getColor().getValue(),
                style.getShadowColor(),
                style.isBold(),
                style.isItalic(),
                style.isUnderlined(),
                style.isStrikethrough(),
                style.isObfuscated()
        );
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
        return style;
    }

    public RichTextStyle withColor(Integer color) {
        return new RichTextStyle(color, this.shadowColor, this.bold, this.italic, this.underlined, this.strikethrough, this.obfuscated);
    }

    public RichTextStyle withShadowColor(Integer shadowColor) {
        return new RichTextStyle(this.color, shadowColor, this.bold, this.italic, this.underlined, this.strikethrough, this.obfuscated);
    }

    public RichTextStyle toggleBold() {
        return new RichTextStyle(this.color, this.shadowColor, !this.bold, this.italic, this.underlined, this.strikethrough, this.obfuscated);
    }

    public RichTextStyle toggleItalic() {
        return new RichTextStyle(this.color, this.shadowColor, this.bold, !this.italic, this.underlined, this.strikethrough, this.obfuscated);
    }

    public RichTextStyle toggleUnderlined() {
        return new RichTextStyle(this.color, this.shadowColor, this.bold, this.italic, !this.underlined, this.strikethrough, this.obfuscated);
    }

    public RichTextStyle toggleStrikethrough() {
        return new RichTextStyle(this.color, this.shadowColor, this.bold, this.italic, this.underlined, !this.strikethrough, this.obfuscated);
    }

    public RichTextStyle toggleObfuscated() {
        return new RichTextStyle(this.color, this.shadowColor, this.bold, this.italic, this.underlined, this.strikethrough, !this.obfuscated);
    }
}
