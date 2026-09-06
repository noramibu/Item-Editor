package me.noramibu.itemeditor.ui.component;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.core.CursorStyle;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.OwoUIGraphics;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.Surface;
import io.wispforest.owo.ui.core.UIComponent.FocusSource;
import io.wispforest.owo.ui.core.VerticalAlignment;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import me.noramibu.itemeditor.ui.util.ScrollStateUtil;
import me.noramibu.itemeditor.util.ItemEditorText;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public final class EditorSearchDialog {
    private static final JsonObject ENGLISH = loadEnglish();

    public record Target(List<String> path, String terms, Runnable open) {
        public Target {
            path = List.copyOf(path);
        }

        public Target(String label, String terms, Runnable open) {
            this(List.of(label), terms, open);
        }

        public String label() {
            return path.getLast();
        }
    }

    private record Indexed(Target target, String label, String terms) {}

    public record Location(String scope, String field) {}

    private static final class HighlightState {
        boolean hovered;
        boolean focused;
    }

    private EditorSearchDialog() {}

    public static String english(String key) {
        String fullKey = ItemEditorText.key(key);
        return ENGLISH.has(fullKey) ? ENGLISH.get(fullKey).getAsString() : "";
    }

    private static JsonObject loadEnglish() {
        try (var stream = EditorSearchDialog.class.getResourceAsStream("/assets/itemeditor/lang/en_us.json")) {
            if (stream == null) return new JsonObject();
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
                    .getAsJsonObject();
        } catch (IOException exception) {
            return new JsonObject();
        }
    }

    public static String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFKD)
                .replaceAll("\\p{M}+", "")
                .replaceAll("([a-z])([A-Z])", "$1 $2")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}]+", " ")
                .trim();
    }

    public static int score(String label, String terms, String query) {
        if (query.isEmpty() || label.equals(query)) return 0;
        if (label.startsWith(query)) return 1;
        if (label.replace(" ", "").equals(query)) return 2;
        String searchable = " " + label + " " + terms;
        for (String word : query.split(" ")) {
            if (!searchable.contains(" " + word)) return -1;
        }
        for (String word : query.split(" ")) {
            if (!(" " + label).contains(" " + word)) return 3;
        }
        return 2;
    }

    static final class Node {
        final List<String> path;
        final Map<String, Node> children = new LinkedHashMap<>();
        Target target;
        int matches;

        Node(List<String> path) {
            this.path = List.copyOf(path);
        }
    }

    static List<Node> tree(List<Target> targets) {
        Node root = new Node(List.of());
        for (Target target : targets) {
            Node parent = root;
            for (int depth = 0; depth < target.path().size(); depth++) {
                List<String> path = target.path().subList(0, depth + 1);
                parent = parent.children.computeIfAbsent(path.getLast(), ignored -> new Node(path));
                parent.matches++;
            }
            parent.target = target;
        }
        return List.copyOf(root.children.values());
    }

    private static final class TreeView {
        final FlowLayout rows = UiFactory.column().gap(2);
        final ScrollContainer<FlowLayout> scroll = DialogUiUtil.scrollContentExpand(rows);
        final Set<List<String>> collapsed = new HashSet<>();
        final Map<List<String>, FlowLayout> focusableRows = new LinkedHashMap<>();
        final Consumer<Target> select;
        List<Node> roots = List.of();
        boolean searching;

        TreeView(Consumer<Target> select) {
            this.select = select;
        }

        void update(List<Target> matches, boolean searching) {
            this.searching = searching;
            roots = tree(matches);
            collapsed.clear();
            if (!searching) roots.forEach(root -> root.children.values().forEach(node -> collapsed.add(node.path)));
            render();
            ScrollStateUtil.restore(scroll, 0);
        }

        void render() {
            rows.clearChildren();
            focusableRows.clear();
            roots.forEach(this::append);
        }

        void append(Node node) {
            FlowLayout row = new FlowLayout(Sizing.fill(100), Sizing.content(), FlowLayout.Algorithm.HORIZONTAL) {
                @Override
                public boolean canFocus(FocusSource source) {
                    return true;
                }

                @Override
                public void drawFocusHighlight(
                        OwoUIGraphics graphics, int mouseX, int mouseY, float partialTicks, float delta) {}
            };
            row.gap(4);
            row.verticalAlignment(VerticalAlignment.CENTER);
            row.padding(Insets.vertical(UiFactory.scaledPixels(4))
                    .withLeft(UiFactory.scaledPixels(8 * Math.min(3, node.path.size() - 1)))
                    .withRight(UiFactory.scaledPixels(4)));
            boolean branch = !node.children.isEmpty();
            Component pathTooltip = Component.literal(String.join(" > ", node.path));
            var arrow = Component.literal(collapsed.contains(node.path) ? "▶" : "▼");
            var toggle = branch ? UiFactory.bodyLabel(arrow, 1.25f) : null;
            if (branch) {
                toggle.horizontalSizing(UiFactory.fixed(18));
                toggle.cursorStyle(CursorStyle.HAND);
                toggle.mouseDown().subscribe((click, doubled) -> {
                    if (click.button() != 0) return false;
                    toggle(node);
                    return true;
                });
                toggle.tooltip(pathTooltip);
                row.mouseDown().subscribe((click, doubled) -> {
                    double left = toggle.x() - row.x();
                    if (click.button() != 0 || click.x() < left || click.x() >= left + toggle.width()) return false;
                    toggle(node);
                    return true;
                });
                row.child(toggle);
            } else row.child(UiFactory.row().horizontalSizing(UiFactory.fixed(18)));
            var label = Component.literal(node.path.getLast());
            label.withStyle(
                    node.path.size() == 1 ? ChatFormatting.AQUA : branch ? ChatFormatting.GOLD : ChatFormatting.WHITE);
            if (branch) label.withStyle(ChatFormatting.BOLD);
            Runnable activate = () -> {
                if (node.target != null) select.accept(node.target);
                else toggle(node);
            };
            var text = UiFactory.column().gap(2);
            text.horizontalSizing(Sizing.expand(100));
            var heading = UiFactory.bodyLabel(label);
            text.child(heading.horizontalSizing(Sizing.fill(100)));
            if (searching && node.target != null && node.path.size() > 1)
                text.child(UiFactory.muted(
                                Component.literal(String.join(" > ", node.path.subList(0, node.path.size() - 1))),
                                Integer.MAX_VALUE,
                                0.8f)
                        .horizontalSizing(Sizing.fill(100)));
            row.tooltip(pathTooltip);
            text.mouseDown().subscribe((click, doubled) -> {
                if (click.button() != 0) return false;
                activate.run();
                return true;
            });
            text.cursorStyle(CursorStyle.HAND);
            var active = new HighlightState();
            Runnable updateHighlight = () -> {
                boolean highlighted = active.hovered || active.focused;
                row.surface(highlighted ? Surface.flat(0xA0356078) : Surface.BLANK);
                heading.text(highlighted ? label.copy().withStyle(ChatFormatting.YELLOW) : label);
                if (toggle != null) toggle.text(highlighted ? arrow.copy().withStyle(ChatFormatting.YELLOW) : arrow);
            };
            row.mouseEnter().subscribe(() -> {
                active.hovered = true;
                updateHighlight.run();
            });
            row.mouseLeave().subscribe(() -> {
                active.hovered = false;
                updateHighlight.run();
            });
            row.focusGained().subscribe(source -> {
                active.focused = true;
                updateHighlight.run();
            });
            row.focusLost().subscribe(() -> {
                active.focused = false;
                updateHighlight.run();
            });
            row.keyPress().subscribe(input -> {
                if (input.key() == GLFW.GLFW_KEY_ENTER
                        || input.key() == GLFW.GLFW_KEY_KP_ENTER
                        || input.key() == GLFW.GLFW_KEY_SPACE) {
                    activate.run();
                    return true;
                }
                if (input.key() == GLFW.GLFW_KEY_RIGHT && branch) {
                    collapsed.remove(node.path);
                    render();
                    focus(node.path);
                    return true;
                }
                if (input.key() == GLFW.GLFW_KEY_LEFT) {
                    if (branch && !collapsed.contains(node.path)) toggle(node);
                    else if (node.path.size() > 1) focus(node.path.subList(0, node.path.size() - 1));
                    return true;
                }
                if (input.key() == GLFW.GLFW_KEY_UP || input.key() == GLFW.GLFW_KEY_DOWN) {
                    var paths = new ArrayList<>(focusableRows.keySet());
                    int index = paths.indexOf(node.path);
                    int next = index + (input.key() == GLFW.GLFW_KEY_UP ? -1 : 1);
                    focus(paths.get(Math.clamp(next, 0, paths.size() - 1)));
                    return true;
                }
                return false;
            });
            focusableRows.put(node.path, row);
            row.child(text);
            if (branch)
                row.child(UiFactory.muted(Component.literal(Integer.toString(node.matches)))
                        .horizontalSizing(UiFactory.fixed(28)));
            rows.child(row);
            if (branch && !collapsed.contains(node.path)) node.children.values().forEach(this::append);
        }

        void toggle(Node node) {
            double offset = ScrollStateUtil.offset(scroll);
            if (!collapsed.remove(node.path)) collapsed.add(node.path);
            render();
            ScrollStateUtil.restore(scroll, offset);
            focus(node.path);
        }

        void focus(List<String> path) {
            var button = focusableRows.get(path);
            if (button == null || scroll.focusHandler() == null) return;
            scroll.focusHandler().focus(button, FocusSource.KEYBOARD_CYCLE);
            double offset = ScrollStateUtil.offset(scroll);
            if (button.y() < scroll.y()) offset += button.y() - scroll.y();
            else if (button.y() + button.height() > scroll.y() + scroll.height())
                offset += button.y() + button.height() - scroll.y() - scroll.height();
            ScrollStateUtil.restore(scroll, offset);
        }
    }

    public static FlowLayout create(List<Target> targets, Consumer<Target> select, Runnable cancel) {
        List<Indexed> index = targets.stream()
                .map(target -> new Indexed(
                        target,
                        normalize(target.label()),
                        normalize(target.terms() + " " + String.join(" ", target.path()))))
                .toList();
        FlowLayout overlay = DialogUiUtil.overlay();
        int width = DialogUiUtil.dialogWidth(640);
        var sizing = DialogUiUtil.scrollDialogSizing(340, UiFactory.scaledPixels(100), 64, 160);
        FlowLayout dialog = DialogUiUtil.dialogCard(width, sizing.dialogHeight(), 4);
        dialog.child(UiFactory.title(ItemEditorText.tr("dialog.searchable_picker.search")));
        var search = UiFactory.textBox("", value -> {});
        search.id("editor-search-input");
        search.setHint(ItemEditorText.tr("dialog.searchable_picker.search"));
        dialog.child(search);
        var count = UiFactory.muted("");
        dialog.child(count);
        TreeView tree = new TreeView(select);
        dialog.child(tree.scroll);
        var shown = new ArrayList<Target>();
        Runnable refresh = () -> {
            String query = normalize(search.getValue());
            shown.clear();
            index.stream()
                    .filter(entry -> score(entry.label(), entry.terms(), query) >= 0)
                    .sorted(Comparator.comparingInt(entry -> score(entry.label(), entry.terms(), query)))
                    .map(Indexed::target)
                    .forEach(shown::add);
            count.text(ItemEditorText.tr("dialog.searchable_picker.results", shown.size(), index.size()));
            tree.update(shown, !query.isEmpty());
            if (shown.isEmpty()) tree.rows.child(UiFactory.muted(ItemEditorText.tr("dialog.searchable_picker.none")));
        };
        search.keyPress().subscribe(input -> {
            if (shown.isEmpty()) return false;
            if (input.key() == GLFW.GLFW_KEY_ENTER || input.key() == GLFW.GLFW_KEY_KP_ENTER) {
                select.accept(shown.getFirst());
                return true;
            }
            if (input.key() == GLFW.GLFW_KEY_DOWN && !tree.focusableRows.isEmpty()) {
                tree.focus(tree.focusableRows.keySet().iterator().next());
                return true;
            }
            return false;
        });
        search.onChanged().subscribe(value -> refresh.run());
        refresh.run();
        dialog.child(UiFactory.button(
                        ItemEditorText.tr("common.cancel"),
                        UiFactory.ButtonTextPreset.STANDARD,
                        ignored -> cancel.run())
                .horizontalSizing(Sizing.fill(100)));
        overlay.child(dialog);
        return overlay;
    }
}
