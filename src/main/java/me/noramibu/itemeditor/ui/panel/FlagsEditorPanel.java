package me.noramibu.itemeditor.ui.panel;

import io.wispforest.owo.ui.container.FlowLayout;
import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.ui.component.UiFactory;
import me.noramibu.itemeditor.ui.screen.ItemEditorScreen;
import me.noramibu.itemeditor.util.ItemEditorText;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class FlagsEditorPanel implements EditorPanel {

    private static final List<FlagOption> OPTIONS = List.of(
            new FlagOption("minecraft:enchantments", "flags.hidden.enchantments"),
            new FlagOption("minecraft:stored_enchantments", "flags.hidden.stored_enchantments"),
            new FlagOption("minecraft:attribute_modifiers", "flags.hidden.attribute_modifiers"),
            new FlagOption("minecraft:unbreakable", "flags.hidden.unbreakable"),
            new FlagOption("minecraft:dyed_color", "flags.hidden.dyed_color"),
            new FlagOption("minecraft:trim", "flags.hidden.trim"),
            new FlagOption("minecraft:can_break", "flags.hidden.can_break"),
            new FlagOption("minecraft:can_place_on", "flags.hidden.can_place_on"),
            new FlagOption("minecraft:potion_contents", "flags.hidden.potion_contents"),
            new FlagOption("minecraft:fireworks", "flags.hidden.fireworks"),
            new FlagOption("minecraft:firework_explosion", "flags.hidden.firework_explosion"),
            new FlagOption("minecraft:banner_patterns", "flags.hidden.banner_patterns"),
            new FlagOption("minecraft:map_color", "flags.hidden.map_color")
    );

    private final ItemEditorScreen screen;

    public FlagsEditorPanel(ItemEditorScreen screen) {
        this.screen = screen;
    }

    @Override
    public FlowLayout build() {
        ItemEditorState state = this.screen.session().state();
        FlowLayout root = UiFactory.column();

        FlowLayout global = UiFactory.section(ItemEditorText.tr("flags.tooltip.title"), Component.empty());
        global.child(UiFactory.checkbox(ItemEditorText.tr("flags.tooltip.hide_all"), state.hideTooltip, PanelBindings.toggle(this.screen, value -> state.hideTooltip = value)));
        root.child(global);

        FlowLayout common = UiFactory.section(ItemEditorText.tr("flags.hidden.title"), Component.empty());
        for (FlagOption option : OPTIONS) {
            common.child(UiFactory.checkbox(ItemEditorText.tr(option.labelKey()), state.hiddenTooltipComponents.contains(option.id()), value -> {
                PanelBindings.mutate(this.screen, () -> {
                    if (value) {
                        state.hiddenTooltipComponents.add(option.id());
                    } else {
                        state.hiddenTooltipComponents.remove(option.id());
                    }
                });
            }));
        }
        root.child(common);
        return root;
    }

    private record FlagOption(String id, String labelKey) {
    }
}
