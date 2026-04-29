package me.noramibu.itemeditor.ui.util;

import io.wispforest.owo.ui.container.ScrollContainer;

import java.lang.reflect.Field;

public final class ScrollStateUtil {

    private static final Field SCROLL_OFFSET_FIELD = findField("scrollOffset");
    private static final Field CURRENT_SCROLL_POSITION_FIELD = findField("currentScrollPosition");
    private static final Field LAST_SCROLL_POSITION_FIELD = findField("lastScrollPosition");
    private static final Field MAX_SCROLL_FIELD = findField("maxScroll");

    private ScrollStateUtil() {
    }

    public static double offset(ScrollContainer<?> scroll) {
        if (scroll == null || SCROLL_OFFSET_FIELD == null) return 0;
        try {
            return SCROLL_OFFSET_FIELD.getDouble(scroll);
        } catch (IllegalAccessException ignored) {
            return 0;
        }
    }

    public static void restore(ScrollContainer<?> scroll, double scrollAmount) {
        if (scroll == null) return;

        double clamped = Math.max(0, Math.min(scrollAmount, max(scroll)));
        setField(scroll, "scrollOffset", clamped);
        setField(scroll, "currentScrollPosition", clamped);
        setField(scroll, "lastScrollPosition", -1);
    }

    public static void sync(ScrollContainer<?> scroll) {
        restore(scroll, offset(scroll));
    }

    private static int max(ScrollContainer<?> scroll) {
        if (scroll == null) return 0;
        if (MAX_SCROLL_FIELD == null) return 0;
        try {
            return MAX_SCROLL_FIELD.getInt(scroll);
        } catch (IllegalAccessException ignored) {
            return 0;
        }
    }

    private static void setField(ScrollContainer<?> scroll, String fieldName, double value) {
        Field field = switch (fieldName) {
            case "scrollOffset" -> SCROLL_OFFSET_FIELD;
            case "currentScrollPosition" -> CURRENT_SCROLL_POSITION_FIELD;
            case "lastScrollPosition" -> LAST_SCROLL_POSITION_FIELD;
            default -> null;
        };
        if (field == null) {
            return;
        }
        try {
            if (field.getType() == int.class) {
                field.setInt(scroll, (int) Math.round(value));
            } else if (field.getType() == double.class) {
                field.setDouble(scroll, value);
            }
        } catch (IllegalAccessException ignored) {
        }
    }

    private static Field findField(String name) {
        try {
            Field field = ScrollContainer.class.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }
}
