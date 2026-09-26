package me.noramibu.itemeditor.ui.screen;

record DropdownPlacement(int x, int y) {
    static DropdownPlacement at(
            int x, int y, int anchorHeight, int width, int height, int screenWidth, int screenHeight) {
        int below = y + anchorHeight + 2;
        int preferred = below + height <= screenHeight - 4 ? below : y - height - 2;
        return new DropdownPlacement(
                Math.clamp(x, 4, Math.max(4, screenWidth - width - 4)),
                Math.clamp(preferred, 4, Math.max(4, screenHeight - height - 4)));
    }
}
