package me.noramibu.itemeditor.ui.component;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.ColorPickerComponent;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.component.TextBoxComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Color;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.Sizing;
import me.noramibu.itemeditor.util.ItemEditorText;
import me.noramibu.itemeditor.util.TextComponentUtil;
import me.noramibu.itemeditor.util.ValidationUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;

public final class RichTextTokenDialog {

    private static final int DIALOG_WIDTH = 460;
    private static final int DIALOG_WIDTH_WITH_OBJECT_PICKER = 620;
    private static final int DIALOG_GAP = 8;
    private static final int CONTENT_GAP = 8;
    private static final int COMPACT_BUTTON_WIDTH_THRESHOLD = 360;
    private static final int COMPACT_BUTTON_ROWS = 2;
    private static final int BUTTON_RESERVE_COMPACT_EXTRA = 22;
    private static final int BUTTON_RESERVE_NORMAL_EXTRA = 16;
    private static final int HEADER_RESERVE = 88;
    private static final int CONTENT_PREFERRED_HEIGHT = 210;
    private static final int CONTENT_MIN_HEIGHT = 120;
    private static final int DIALOG_MIN_HEIGHT = 210;
    private static final int CONTENT_TEXT_MARGIN = 36;
    private static final int FOOTER_BUTTON_MIN_WIDTH = 72;
    private static final int FOOTER_BUTTON_MAX_WIDTH = 140;
    private static final int FOOTER_BUTTON_DIVISOR = 4;
    private static final int FOOTER_BUTTON_TEXT_MIN_WIDTH = 24;
    private static final int FOOTER_BUTTON_TEXT_RESERVE = 10;
    private static final int CONTENT_SCROLLBAR_GUTTER_BASE = 10;
    private static final int CONTENT_TEXT_MARGIN_OBJECT_PICKER_EXTRA = 12;
    private static final int ERROR_COLOR = 0xFF8A8A;
    private static final String TOKEN_PLACEHOLDER = "text";
    private static final int UNBOUNDED_TEXT_LIMIT = Integer.MAX_VALUE;
    private static final int OBJECT_PICKER_HEIGHT_COMPACT = 72;
    private static final int OBJECT_PICKER_HEIGHT_NORMAL = 86;
    private static final int OBJECT_PICKER_SELECTOR_WIDTH = 14;
    private static final int OBJECT_PICKER_SELECTOR_PADDING = 6;
    private static final int OBJECT_SWATCH_SIZE = 12;
    private static final int OBJECT_HEX_INPUT_MIN_WIDTH = 72;
    private static final int OBJECT_HEX_ROW_LEFT_RESERVE = 96;

    private RichTextTokenDialog() {
    }

    public static FlowLayout createHead(String title, Consumer<String> onApply, Runnable onCancel) {
        List<ModeSpec> modes = List.of(
                new ModeSpec(
                        ItemEditorText.tr("dialog.rich_text.head.mode.username"),
                        List.of(new FieldSpec(ItemEditorText.tr("dialog.rich_text.head.field.username"), "Notch")),
                        values -> {
                            String username = firstRequiredTrimmed(values);
                            if (username == null) {
                                return null;
                            }
                            return "[ie:head:" + TextComponentUtil.escapeStructuredTokenValue(username) + ":true]";
                        }
                ),
                new ModeSpec(
                        ItemEditorText.tr("dialog.rich_text.head.mode.texture"),
                        List.of(new FieldSpec(ItemEditorText.tr("dialog.rich_text.head.field.texture"), "")),
                        values -> {
                            String texture = firstRequiredTrimmed(values);
                            if (texture == null) {
                                return null;
                            }
                            return "[ie:head_texture:" + TextComponentUtil.escapeStructuredTokenValue(texture) + "::true]";
                        }
                )
        );
        return create(
                title,
                ItemEditorText.tr("dialog.rich_text.head.body"),
                modes,
                true,
                onApply,
                onCancel
        );
    }

    public static FlowLayout createSprite(String title, Consumer<String> onApply, Runnable onCancel) {
        List<ModeSpec> modes = List.of(
                new ModeSpec(
                        ItemEditorText.tr("dialog.rich_text.sprite.mode"),
                        List.of(
                                new FieldSpec(ItemEditorText.tr("dialog.rich_text.sprite.field.atlas"), "minecraft:blocks"),
                                new FieldSpec(ItemEditorText.tr("dialog.rich_text.sprite.field.sprite"), "minecraft:block/stone")
                        ),
                        values -> {
                            String atlas = firstRequiredTrimmed(values);
                            String sprite = secondRequiredTrimmed(values);
                            if (atlas == null || sprite == null) {
                                return null;
                            }
                            if (Identifier.tryParse(atlas) == null || Identifier.tryParse(sprite) == null) {
                                return null;
                            }
                            return "[ie:sprite:"
                                    + TextComponentUtil.escapeStructuredTokenValue(atlas)
                                    + "|"
                                    + TextComponentUtil.escapeStructuredTokenValue(sprite)
                                    + "]";
                        }
                )
        );
        return create(
                title,
                ItemEditorText.tr("dialog.rich_text.sprite.body"),
                modes,
                true,
                onApply,
                onCancel
        );
    }

    public static FlowLayout createEvent(String title, boolean includeHoverModes, boolean includeSuggestCommand, Consumer<String> onApply, Runnable onCancel) {
        List<ModeSpec> modes = new ArrayList<>();
        modes.add(clickMode("dialog.rich_text.click.mode.run_command", "dialog.rich_text.click.field.command", "/say hi", value ->
                "[ie:click:run_command:" + TextComponentUtil.escapeStructuredTokenValue(value) + "]"));
        if (includeSuggestCommand) {
            modes.add(clickMode("dialog.rich_text.click.mode.suggest_command", "dialog.rich_text.click.field.command", "/help", value ->
                    "[ie:click:suggest_command:" + TextComponentUtil.escapeStructuredTokenValue(value) + "]"));
        }
        modes.add(clickMode("dialog.rich_text.click.mode.open_url", "dialog.rich_text.click.field.url", "https://minecraft.wiki", value -> {
            try {
                URI uri = URI.create(value);
                if (uri.getScheme() == null) {
                    return null;
                }
            } catch (RuntimeException exception) {
                return null;
            }
            return "[ie:click:open_url:" + TextComponentUtil.escapeStructuredTokenValue(value) + "]";
        }));
        modes.add(clickMode("dialog.rich_text.click.mode.copy_to_clipboard", "dialog.rich_text.click.field.value", TOKEN_PLACEHOLDER, value ->
                "[ie:click:copy_to_clipboard:" + TextComponentUtil.escapeStructuredTokenValue(value) + "]"));
        modes.add(clickMode("dialog.rich_text.click.mode.change_page", "dialog.rich_text.click.field.page", "1", value -> {
            try {
                int page = Integer.parseInt(value.trim());
                if (page <= 0) {
                    return null;
                }
                return "[ie:click:change_page:" + page + "]";
            } catch (RuntimeException exception) {
                return null;
            }
        }));
        modes.add(new ModeSpec(
                ItemEditorText.tr("dialog.rich_text.click.mode.custom"),
                List.of(
                        new FieldSpec(ItemEditorText.tr("dialog.rich_text.click.field.id"), "namespace:id"),
                        new FieldSpec(ItemEditorText.tr("dialog.rich_text.click.field.payload"), "payload string")
                ),
                values -> {
                    String id = firstRequiredTrimmed(values);
                    if (id == null || Identifier.tryParse(id) == null) {
                        return null;
                    }
                    String payload = secondOptionalRaw(values);
                    StringBuilder token = new StringBuilder("[ie:click:custom:")
                            .append(escapeEventField(id));
                    if (payload != null) {
                        token.append('|').append(escapeEventField(payload));
                    }
                    return token.append(']').toString();
                }
        ));
        if (includeHoverModes) {
            modes.add(new ModeSpec(
                    ItemEditorText.tr("dialog.rich_text.hover.mode.show_text"),
                    List.of(new FieldSpec(ItemEditorText.tr("dialog.rich_text.hover.field.text"), TOKEN_PLACEHOLDER)),
                    values -> {
                        String text = firstRequiredRaw(values);
                        if (text == null) {
                            return null;
                        }
                        return "[ie:hover:show_text:" + TextComponentUtil.escapeStructuredTokenValue(text) + "]";
                    }
            ));
            modes.add(new ModeSpec(
                    ItemEditorText.tr("dialog.rich_text.hover.mode.show_item"),
                    List.of(
                            new FieldSpec(ItemEditorText.tr("dialog.rich_text.hover.field.item"), "minecraft:golden_chestplate"),
                            new FieldSpec(ItemEditorText.tr("dialog.rich_text.hover.field.count"), "1")
                    ),
                    values -> {
                        String itemId = firstRequiredTrimmed(values);
                        if (itemId == null || Identifier.tryParse(itemId) == null) {
                            return null;
                        }
                        String countValue = secondOptionalTrimmed(values);
                        Integer count = null;
                        if (countValue != null) {
                            try {
                                int parsed = Integer.parseInt(countValue);
                                if (parsed <= 0) {
                                    return null;
                                }
                                count = parsed;
                            } catch (RuntimeException exception) {
                                return null;
                            }
                        }
                        StringBuilder token = new StringBuilder("[ie:hover:show_item:")
                                .append(escapeEventField(itemId));
                        if (count != null && count != 1) {
                            token.append('|').append(count);
                        }
                        return token.append(']').toString();
                    }
            ));
            modes.add(new ModeSpec(
                    ItemEditorText.tr("dialog.rich_text.hover.mode.show_entity"),
                    List.of(
                            new FieldSpec(ItemEditorText.tr("dialog.rich_text.hover.field.entity"), "minecraft:player"),
                            new FieldSpec(ItemEditorText.tr("dialog.rich_text.hover.field.uuid"), "00000000-0000-0000-0000-000000000000"),
                            new FieldSpec(ItemEditorText.tr("dialog.rich_text.hover.field.name"), TOKEN_PLACEHOLDER)
                    ),
                    values -> {
                        String entityId = firstRequiredTrimmed(values);
                        if (entityId == null || Identifier.tryParse(entityId) == null) {
                            return null;
                        }
                        String uuidValue = secondRequiredTrimmed(values);
                        if (uuidValue == null) {
                            return null;
                        }
                        try {
                            UUID.fromString(uuidValue);
                        } catch (RuntimeException exception) {
                            return null;
                        }
                        String name = thirdOptionalRaw(values);
                        StringBuilder token = new StringBuilder("[ie:hover:show_entity:")
                                .append(escapeEventField(entityId))
                                .append('|')
                                .append(escapeEventField(uuidValue.toLowerCase(Locale.ROOT)));
                        if (name != null) {
                            token.append('|').append(escapeEventField(name));
                        }
                        return token.append(']').toString();
                    }
            ));
        }
        return create(
                title,
                ItemEditorText.tr("dialog.rich_text.event.body"),
                modes,
                false,
                onApply,
                onCancel
        );
    }

    private static ModeSpec clickMode(
            String labelKey,
            String fieldKey,
            String initialValue,
            Function<String, String> tokenBuilder
    ) {
        return new ModeSpec(
                ItemEditorText.tr(labelKey),
                List.of(new FieldSpec(ItemEditorText.tr(fieldKey), initialValue)),
                values -> {
                    String value = firstRequiredRaw(values);
                    if (value == null) {
                        return null;
                    }
                    return tokenBuilder.apply(value);
                }
        );
    }

    private static FlowLayout create(
            String title,
            Component body,
            List<ModeSpec> modes,
            boolean includeObjectColorPicker,
            Consumer<String> onApply,
            Runnable onCancel
    ) {
        FlowLayout overlay = DialogUiUtil.overlay();
        int preferredWidth = includeObjectColorPicker ? DIALOG_WIDTH_WITH_OBJECT_PICKER : DIALOG_WIDTH;
        int dialogWidth = DialogUiUtil.dialogWidth(preferredWidth);
        boolean compactButtons = DialogUiUtil.compactButtons(dialogWidth, COMPACT_BUTTON_WIDTH_THRESHOLD);
        int buttonReserve = DialogUiUtil.buttonRowReserve(
                compactButtons,
                COMPACT_BUTTON_ROWS,
                BUTTON_RESERVE_COMPACT_EXTRA,
                BUTTON_RESERVE_NORMAL_EXTRA
        );
        DialogUiUtil.ScrollDialogSizing sizing = DialogUiUtil.scrollDialogSizing(
                CONTENT_PREFERRED_HEIGHT,
                UiFactory.scaledPixels(HEADER_RESERVE) + buttonReserve,
                CONTENT_MIN_HEIGHT,
                DIALOG_MIN_HEIGHT
        );

        FlowLayout dialog = DialogUiUtil.dialogCard(dialogWidth, sizing.dialogHeight(), DIALOG_GAP);
        dialog.child(UiFactory.title(title));

        FlowLayout content = UiFactory.column().gap(CONTENT_GAP);
        content.padding(Insets.of(0, UiFactory.scrollContentInset(CONTENT_SCROLLBAR_GUTTER_BASE), 0, 0));
        int textMargin = CONTENT_TEXT_MARGIN + (includeObjectColorPicker ? CONTENT_TEXT_MARGIN_OBJECT_PICKER_EXTRA : 0);
        int textWidth = DialogUiUtil.dialogTextWidth(dialogWidth, textMargin);
        if (!body.getString().isBlank()) {
            content.child(UiFactory.muted(body, textWidth));
        }

        AtomicInteger selectedMode = new AtomicInteger(0);
        List<ButtonComponent> modeButtons = new ArrayList<>();
        List<TextBoxComponent> inputs = new ArrayList<>();
        AtomicInteger objectColor = new AtomicInteger(0xFFFFFF);

        FlowLayout fieldsList = UiFactory.column();
        fieldsList.gap(Math.max(1, UiFactory.scaleProfile().tightSpacing() - 1));
        fieldsList.horizontalSizing(Sizing.fill(100));

        LabelComponent errorLabel = UiFactory.message("", ERROR_COLOR);

        if (includeObjectColorPicker) {
            FlowLayout colorCard = UiFactory.subCard();
            colorCard.horizontalSizing(Sizing.fill(100));
            colorCard.child(UiFactory.title(ItemEditorText.tr("dialog.rich_text.object_color.title")).shadow(false));

            AtomicBoolean syncingColor = new AtomicBoolean(false);
            ColorPickerComponent picker = new ColorPickerComponent()
                    .selectedColor(Color.ofRgb(objectColor.get()))
                    .showAlpha(false)
                    .selectorWidth(OBJECT_PICKER_SELECTOR_WIDTH)
                    .selectorPadding(OBJECT_PICKER_SELECTOR_PADDING);
            ColorPickerUiUtil.applyPickerSizing(
                    picker,
                    compactButtons,
                    Math.max(1, textWidth),
                    compactButtons ? OBJECT_PICKER_HEIGHT_COMPACT : OBJECT_PICKER_HEIGHT_NORMAL
            );
            ColorPickerUiUtil.Swatch swatch = ColorPickerUiUtil.createSwatch(objectColor.get(), OBJECT_SWATCH_SIZE);
            TextBoxComponent hexInput = UiFactory.textBox(ValidationUtil.toHex(objectColor.get()), ignored -> errorLabel.text(Component.empty()));
            hexInput.setMaxLength(7);
            int hexInputWidth = Math.max(
                    UiFactory.scaledPixels(OBJECT_HEX_INPUT_MIN_WIDTH),
                    textWidth - UiFactory.scaledPixels(OBJECT_HEX_ROW_LEFT_RESERVE)
            );
            hexInput.horizontalSizing(UiFactory.fixed(hexInputWidth));

            Runnable syncColorUi = ColorPickerUiUtil.createSyncRunnable(
                    syncingColor,
                    objectColor::get,
                    picker,
                    swatch.swatch(),
                    swatch.label(),
                    hexInput,
                    () -> errorLabel.text(Component.empty())
            );

            picker.onChanged().subscribe(color -> {
                if (syncingColor.get()) {
                    return;
                }
                objectColor.set(color.rgb() & 0xFFFFFF);
                syncColorUi.run();
            });
            ColorPickerUiUtil.bindHexInput(hexInput, syncingColor, errorLabel, rgb -> {
                objectColor.set(rgb & 0xFFFFFF);
                syncColorUi.run();
            });
            syncColorUi.run();

            FlowLayout hexRow = UiFactory.row();
            hexRow.horizontalSizing(Sizing.fill(100));
            hexRow.child(swatch.swatch());
            hexRow.child(swatch.label());
            hexRow.child(hexInput);

            colorCard.child(hexRow);
            colorCard.child(picker);
            content.child(colorCard);
        }

        Runnable refreshModeButtons = () -> {
            int activeMode = selectedMode.get();
            for (int index = 0; index < modeButtons.size(); index++) {
                modeButtons.get(index).active(index != activeMode);
            }
        };

        Runnable rebuildFields = () -> {
            fieldsList.clearChildren();
            inputs.clear();

            ModeSpec mode = modes.get(selectedMode.get());
            for (FieldSpec field : mode.fields()) {
                TextBoxComponent input = UiFactory.textBox(field.initialValue(), ignored -> errorLabel.text(Component.empty()));
                input.setMaxLength(UNBOUNDED_TEXT_LIMIT);
                input.horizontalSizing(Sizing.fill(100));
                fieldsList.child(UiFactory.field(field.label(), Component.empty(), input));
                inputs.add(input);
            }
        };

        if (modes.size() > 1) {
            FlowLayout modeCard = UiFactory.subCard();
            modeCard.horizontalSizing(Sizing.fill(100));
            modeCard.child(UiFactory.title(ItemEditorText.tr("dialog.rich_text.mode_title")).shadow(false));

            FlowLayout modeList = UiFactory.column();
            modeList.gap(Math.max(1, UiFactory.scaleProfile().tightSpacing() - 1));
            modeList.horizontalSizing(Sizing.fill(100));

            for (int index = 0; index < modes.size(); index++) {
                int modeIndex = index;
                ModeSpec mode = modes.get(modeIndex);
                ButtonComponent button = UiFactory.button(mode.label(), UiFactory.ButtonTextPreset.STANDARD, ignored -> {
                    selectedMode.set(modeIndex);
                    refreshModeButtons.run();
                    rebuildFields.run();
                });
                button.horizontalSizing(Sizing.fill(100));
                modeButtons.add(button);
                modeList.child(button);
            }

            modeCard.child(modeList);
            content.child(modeCard);
        }

        FlowLayout valuesCard = UiFactory.subCard();
        valuesCard.horizontalSizing(Sizing.fill(100));
        valuesCard.child(UiFactory.title(ItemEditorText.tr("dialog.rich_text.values_title")).shadow(false));
        valuesCard.child(fieldsList);
        content.child(valuesCard);
        content.child(errorLabel);

        refreshModeButtons.run();
        rebuildFields.run();

        dialog.child(DialogUiUtil.scrollCard(content, sizing.contentHeight()));

        FlowLayout footer = DialogUiUtil.footerRowByDivisor(
                dialogWidth,
                compactButtons,
                FOOTER_BUTTON_MIN_WIDTH,
                FOOTER_BUTTON_MAX_WIDTH,
                FOOTER_BUTTON_DIVISOR,
                FOOTER_BUTTON_TEXT_MIN_WIDTH,
                FOOTER_BUTTON_TEXT_RESERVE,
                new DialogUiUtil.FooterAction(ItemEditorText.tr("common.cancel"), ignored -> onCancel.run()),
                new DialogUiUtil.FooterAction(ItemEditorText.tr("dialog.rich_text.apply"), ignored -> {
                    ModeSpec mode = modes.get(selectedMode.get());
                    List<String> values = new ArrayList<>(inputs.size());
                    for (TextBoxComponent input : inputs) {
                        values.add(input.getValue());
                    }
                    String token = mode.tokenBuilder().apply(values);
                    if (token == null || token.isBlank()) {
                        errorLabel.text(ItemEditorText.tr("dialog.rich_text.error.invalid"));
                        return;
                    }
                    if (includeObjectColorPicker) {
                        token = appendObjectColor(token, objectColor.get());
                    }
                    onApply.accept(token);
                })
        );
        dialog.child(footer);

        overlay.child(dialog);
        return overlay;
    }

    private static String firstRequiredTrimmed(List<String> values) {
        return requiredTrimmed(firstValue(values));
    }

    private static String secondRequiredTrimmed(List<String> values) {
        return requiredTrimmed(secondValue(values));
    }

    private static String firstRequiredRaw(List<String> values) {
        return requiredRaw(firstValue(values));
    }

    private static String secondOptionalTrimmed(List<String> values) {
        return optionalTrimmed(secondValue(values));
    }

    private static String secondOptionalRaw(List<String> values) {
        return optionalRaw(secondValue(values));
    }

    private static String thirdOptionalRaw(List<String> values) {
        return optionalRaw(thirdValue(values));
    }

    private static String firstValue(List<String> values) {
        return values.isEmpty() ? null : values.getFirst();
    }

    private static String secondValue(List<String> values) {
        return values.size() < 2 ? null : values.get(1);
    }

    private static String thirdValue(List<String> values) {
        return values.size() < 3 ? null : values.get(2);
    }

    private static String requiredTrimmed(String value) {
        return optionalTrimmed(value);
    }

    private static String requiredRaw(String value) {
        return value == null || value.trim().isEmpty() ? null : value;
    }

    private static String optionalTrimmed(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String optionalRaw(String value) {
        return value == null || value.trim().isEmpty() ? null : value;
    }

    private static String appendObjectColor(String token, int rgb) {
        if (token == null || token.isBlank()) {
            return token;
        }
        int closeIndex = token.lastIndexOf(']');
        if (closeIndex < 0) {
            return token;
        }
        return token.substring(0, closeIndex)
                + "|"
                + TextComponentUtil.escapeStructuredTokenValue(ValidationUtil.toHex(rgb))
                + token.substring(closeIndex);
    }

    private static String escapeEventField(String value) {
        return TextComponentUtil.escapeStructuredTokenValue(value).replace("|", "\\|");
    }

    private record FieldSpec(Component label, String initialValue) {
    }

    private record ModeSpec(
            Component label,
            List<FieldSpec> fields,
            Function<List<String>, String> tokenBuilder
    ) {
    }
}
