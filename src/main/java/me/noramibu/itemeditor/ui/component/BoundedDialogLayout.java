package me.noramibu.itemeditor.ui.component;

import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Sizing;

/** Bounds fixed dialog dimensions to the actual overlay, including parent padding. */
final class BoundedDialogLayout extends FlowLayout {
    BoundedDialogLayout(int width) {
        super(Sizing.fixed(width), Sizing.content(), Algorithm.VERTICAL);
    }

    @Override
    protected void applySizing() {
        super.applySizing();
        this.width =
                boundedSize(this.width, this.space.width(), this.margins.get().horizontal());
        if (!this.verticalSizing.get().isContent()) {
            this.height = boundedSize(
                    this.height, this.space.height(), this.margins.get().vertical());
        }
    }

    static int boundedSize(int preferred, int available, int margins) {
        return Math.clamp(preferred, 1, Math.max(1, available - margins));
    }
}
