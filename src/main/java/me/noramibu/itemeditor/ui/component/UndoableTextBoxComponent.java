package me.noramibu.itemeditor.ui.component;

import com.mojang.blaze3d.platform.InputConstants;
import io.wispforest.owo.ui.component.TextBoxComponent;
import io.wispforest.owo.ui.core.Sizing;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Supplier;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.PreeditEvent;

final class UndoableTextBoxComponent extends TextBoxComponent {
    private static final int HISTORY_LIMIT = 100;
    private static final long COALESCE_NANOS = 750_000_000L;
    private static final Map<Object, Map<String, History>> PERSISTED_HISTORIES = new WeakHashMap<>();

    private History history = new History(State.EMPTY);
    private State editStart;
    private EditKind editKind = EditKind.ATOMIC;
    private int editDepth;
    private int highlightPosition;
    private boolean restoring;
    private boolean composing;

    UndoableTextBoxComponent(Sizing horizontalSizing) {
        super(horizontalSizing);
        this.onChanged().subscribe(this::recordChange);
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        if (!this.isActive() || !this.isFocused()) return super.keyPressed(input);

        boolean control = input.hasControlDownWithQuirk() || (input.modifiers() & InputConstants.MOD_CONTROL) != 0;
        if (control && input.key() == InputConstants.KEY_Z) {
            if (input.hasShiftDown()) this.redo();
            else this.undo();
            return true;
        }
        if (control && input.key() == InputConstants.KEY_Y) {
            this.redo();
            return true;
        }

        this.finishComposition();
        EditKind kind =
                switch (input.key()) {
                    case InputConstants.KEY_BACKSPACE, InputConstants.KEY_DELETE -> EditKind.DELETING;
                    default -> input.isPaste() || input.isCut() ? EditKind.ATOMIC : null;
                };
        if (kind == null) {
            this.history.breakCoalescing();
            return super.keyPressed(input);
        }
        return this.edit(kind, () -> super.keyPressed(input));
    }

    @Override
    public boolean charTyped(CharacterEvent input) {
        return this.edit(this.composing ? EditKind.COMPOSITION : EditKind.TYPING, () -> super.charTyped(input));
    }

    @Override
    public boolean preeditUpdated(PreeditEvent event) {
        if (event != null && !this.composing) {
            this.history.breakCoalescing();
            this.composing = true;
        } else if (event == null) {
            this.finishComposition();
        }
        return super.preeditUpdated(event);
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        this.finishComposition();
        this.history.breakCoalescing();
        super.onClick(event, doubleClick);
    }

    @Override
    protected void onDrag(MouseButtonEvent event, double dx, double dy) {
        this.history.breakCoalescing();
        super.onDrag(event, dx, dy);
    }

    @Override
    public void setValue(String value) {
        this.edit(() -> super.setValue(value));
    }

    @Override
    public void setHighlightPos(int position) {
        super.setHighlightPos(position);
        this.highlightPosition = Math.clamp(position, 0, this.getValue().length());
    }

    void clearHistory() {
        this.history.reset(this.snapshot());
    }

    void persistHistory(Object owner, String key) {
        if (owner == null) return;
        State current = this.snapshot();
        synchronized (PERSISTED_HISTORIES) {
            History persisted = PERSISTED_HISTORIES
                    .computeIfAbsent(owner, ignored -> new HashMap<>())
                    .computeIfAbsent(key, ignored -> new History(current));
            if (!persisted.current().value().equals(current.value())) persisted.reset(current);
            this.history = persisted;
        }
        this.restoreSelection(this.history.current());
    }

    private <T> T edit(EditKind kind, Supplier<T> operation) {
        if (this.restoring || this.editDepth > 0) return operation.get();

        this.editDepth++;
        this.editStart = this.snapshot();
        EditKind previousKind = this.editKind;
        this.editKind = this.editStart.hasSelection() ? EditKind.ATOMIC : kind;
        try {
            return operation.get();
        } finally {
            this.editKind = previousKind;
            this.editStart = null;
            this.editDepth--;
        }
    }

    private void edit(Runnable operation) {
        this.edit(EditKind.ATOMIC, () -> {
            operation.run();
            return null;
        });
    }

    private void recordChange(String value) {
        if (this.restoring) return;
        State after = this.snapshot();
        State before = this.editStart == null ? this.history.current() : this.editStart;
        if (before.value().equals(value)) return;
        this.history.record(before, after, this.editKind, System.nanoTime());
    }

    private void undo() {
        State state = this.history.undo(this.snapshot());
        if (state != null) this.restore(state);
    }

    private void redo() {
        State state = this.history.redo(this.snapshot());
        if (state != null) this.restore(state);
    }

    private void restore(State state) {
        this.finishComposition();
        this.restoring = true;
        try {
            super.setValue(state.value());
            this.restoreSelection(state);
        } finally {
            this.restoring = false;
        }
    }

    private void restoreSelection(State state) {
        super.setCursorPosition(Math.min(state.cursor(), this.getValue().length()));
        this.setHighlightPos(Math.min(state.highlight(), this.getValue().length()));
    }

    private State snapshot() {
        return new State(this.getValue(), this.getCursorPosition(), this.highlightPosition);
    }

    private void finishComposition() {
        if (!this.composing) return;
        this.composing = false;
        this.history.breakCoalescing();
    }

    enum EditKind {
        ATOMIC,
        TYPING,
        DELETING,
        COMPOSITION;

        boolean coalesces() {
            return this != ATOMIC;
        }
    }

    record State(String value, int cursor, int highlight) {
        static final State EMPTY = new State("", 0, 0);

        boolean hasSelection() {
            return this.cursor != this.highlight;
        }
    }

    static final class History {
        private final Deque<State> undo = new ArrayDeque<>();
        private final Deque<State> redo = new ArrayDeque<>();
        private State current;
        private EditKind lastKind = EditKind.ATOMIC;
        private long lastEditNanos;

        History(State initial) {
            this.current = initial;
        }

        State current() {
            return this.current;
        }

        void reset(State state) {
            this.undo.clear();
            this.redo.clear();
            this.current = state;
            this.breakCoalescing();
        }

        void record(State before, State after, EditKind kind, long now) {
            if (before.value().equals(after.value())) return;
            boolean merge = kind.coalesces()
                    && kind == this.lastKind
                    && now - this.lastEditNanos <= COALESCE_NANOS
                    && this.current.equals(before)
                    && !before.hasSelection()
                    && !after.hasSelection();
            if (!merge) push(this.undo, before);
            this.redo.clear();
            this.current = after;
            this.lastKind = kind;
            this.lastEditNanos = now;
        }

        State undo(State visibleState) {
            if (this.undo.isEmpty()) return null;
            push(this.redo, visibleState);
            this.current = this.undo.removeLast();
            this.breakCoalescing();
            return this.current;
        }

        State redo(State visibleState) {
            if (this.redo.isEmpty()) return null;
            push(this.undo, visibleState);
            this.current = this.redo.removeLast();
            this.breakCoalescing();
            return this.current;
        }

        void breakCoalescing() {
            this.lastKind = EditKind.ATOMIC;
            this.lastEditNanos = 0;
        }

        private static void push(Deque<State> entries, State state) {
            if (entries.size() == HISTORY_LIMIT) entries.removeFirst();
            entries.addLast(state);
        }
    }
}
