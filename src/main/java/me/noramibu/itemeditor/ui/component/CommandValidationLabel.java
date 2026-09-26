package me.noramibu.itemeditor.ui.component;

import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.core.Color;
import io.wispforest.owo.ui.core.Sizing;
import java.util.function.BooleanSupplier;
import me.noramibu.itemeditor.ui.component.raw.CommandTextTools;
import me.noramibu.itemeditor.util.ItemEditorText;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

/** Debounced command validation with a direct link to the current error. */
public final class CommandValidationLabel extends LabelComponent {
    private final RawTextAreaComponent editor;
    private final BooleanSupplier active;
    private String observed;
    private long dueAt;

    public CommandValidationLabel(RawTextAreaComponent editor) {
        this(editor, () -> true);
    }

    public CommandValidationLabel(RawTextAreaComponent editor, BooleanSupplier active) {
        super(Component.empty());
        this.editor = editor;
        this.active = active;
        this.horizontalSizing(Sizing.fill(100));
        this.shadow(false);
        this.tooltip(UiFactory.tooltipLines(ItemEditorText.tr("command_editor.validation_hint"), 260));
        this.mouseDown().subscribe((click, doubled) -> {
            if (click.button() != 0 || !this.active.getAsBoolean()) return false;
            validate(this.editor, this);
            return this.editor.selectError();
        });
    }

    @Override
    protected Style styleAt(int mouseX, int mouseY) {
        Style style = super.styleAt(mouseX, mouseY);
        return style == null ? Style.EMPTY : style;
    }

    @Override
    public void update(float delta, int mouseX, int mouseY) {
        super.update(delta, mouseX, mouseY);
        if (!this.active.getAsBoolean()) return;
        String value = this.editor.getValue();
        if (!value.equals(this.observed)) {
            this.observed = value;
            this.dueAt = System.currentTimeMillis() + 250;
            this.editor.setErrorLocation(-1, -1, 0);
            this.text(Component.empty());
        }
        if (this.dueAt != 0 && System.currentTimeMillis() >= this.dueAt) {
            this.dueAt = 0;
            validate(this.editor, this);
        }
    }

    public void requestValidation() {
        this.dueAt = 1;
    }

    private static void validate(RawTextAreaComponent editor, LabelComponent status) {
        String text = editor.getValue();
        editor.setErrorLocation(-1, -1, 0);
        var connection = Minecraft.getInstance().getConnection();
        status.color(Color.ofRgb(0xFFD36A));
        if (connection == null || text.length() > CommandTextTools.MAX_PARSE_LENGTH) {
            status.text(ItemEditorText.tr("command_editor.unchecked"));
            return;
        }
        if (text.isBlank()) {
            status.text(ItemEditorText.tr("command_editor.empty"));
            return;
        }
        try {
            var commands = connection.getCommands();
            if (!CommandTextTools.hasCommandRoot(commands, text)) {
                status.text(ItemEditorText.tr("command_editor.unavailable"));
                return;
            }
            var parsed = commands.parse(CommandTextTools.validationReader(text), connection.getSuggestionsProvider());
            var error = Commands.getParseException(parsed);
            boolean incomplete = parsed.getContext().getLastChild().getCommand() == null;
            if (error == null && !incomplete) {
                status.text(ItemEditorText.tr("command_editor.valid"));
                status.color(Color.ofRgb(0x91E68C));
                return;
            }
            int offset = error == null ? text.length() : Math.clamp(error.getCursor(), 0, text.length());
            int line = 1, column = 1;
            for (int i = 0; i < offset; i++) {
                if (text.charAt(i) == '\n') {
                    line++;
                    column = 1;
                } else column++;
            }
            editor.setErrorLocation(line, column, 1);
            status.text((error == null
                            ? ItemEditorText.tr("command_editor.incomplete")
                            : Component.literal(error.getRawMessage().getString()))
                    .copy()
                    .append(" (" + line + ":" + column + ")"));
            status.color(Color.ofRgb(0xFF8A8A));
        } catch (RuntimeException | StackOverflowError error) {
            status.text(ItemEditorText.tr("command_editor.unchecked"));
        }
    }
}
