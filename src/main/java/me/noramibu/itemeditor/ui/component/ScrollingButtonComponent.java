package me.noramibu.itemeditor.ui.component;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.core.OwoUIGraphics;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

final class ScrollingButtonComponent extends ButtonComponent {

    ScrollingButtonComponent(Component message, Consumer<ButtonComponent> onPress) {
        super(message, onPress);
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        if (!this.textShadow) {
            super.extractContents(graphics, mouseX, mouseY, delta);
            return;
        }

        this.renderer.draw((OwoUIGraphics) graphics, this, delta);
        Component label =
                this.active ? this.getMessage() : this.getMessage().copy().withColor(0xA0A0A0);
        this.extractScrollingStringOverContents(
                graphics.textRendererForWidget(this, GuiGraphicsExtractor.HoveredTextEffects.NONE), label, 2);
    }
}
