package me.noramibu.itemeditor.ui.component;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.core.OwoUIGraphics;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

final class ScrollingButtonComponent extends ButtonComponent {

    ScrollingButtonComponent(Component message, Consumer<ButtonComponent> onPress) {
        super(message, onPress);
    }

    @Override
    public void renderContents(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        if (!this.textShadow) {
            super.renderContents(graphics, mouseX, mouseY, delta);
            return;
        }

        this.renderer.draw((OwoUIGraphics) graphics, this, delta);
        Component label = this.active
                ? this.getMessage()
                : this.getMessage().copy().withColor(0xA0A0A0);
        this.renderScrollingStringOverContents(
                graphics.textRendererForWidget(this, GuiGraphics.HoveredTextEffects.NONE),
                label,
                2
        );
    }
}
