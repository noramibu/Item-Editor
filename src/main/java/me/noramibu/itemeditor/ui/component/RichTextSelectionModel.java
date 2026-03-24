package me.noramibu.itemeditor.ui.component;

record RichTextSelectionModel(int cursor, int selectionCursor) {

    int start() {
        return Math.min(this.cursor, this.selectionCursor);
    }

    int end() {
        return Math.max(this.cursor, this.selectionCursor);
    }

    boolean hasSelection() {
        return this.cursor != this.selectionCursor;
    }
}
