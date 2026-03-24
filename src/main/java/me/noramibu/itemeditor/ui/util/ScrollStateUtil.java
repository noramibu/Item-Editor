package me.noramibu.itemeditor.ui.util;

import io.wispforest.owo.ui.container.ScrollContainer;

import java.lang.reflect.Field;

public final class ScrollStateUtil {

    private ScrollStateUtil() {
    }

    public static double offset(ScrollContainer<?> scroll) {
        if (scroll == null) return 0;
        try {
            Field field = ScrollContainer.class.getDeclaredField("scrollOffset");
            field.setAccessible(true);
            return field.getDouble(scroll);
        } catch (ReflectiveOperationException ignored) {
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
        try {
            Field field = ScrollContainer.class.getDeclaredField("maxScroll");
            field.setAccessible(true);
            return field.getInt(scroll);
        } catch (ReflectiveOperationException ignored) {
            return 0;
        }
    }

    private static void setField(ScrollContainer<?> scroll, String fieldName, double value) {
        try {
            Field field = ScrollContainer.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            if (field.getType() == int.class) {
                field.setInt(scroll, (int) Math.round(value));
            } else if (field.getType() == double.class) {
                field.setDouble(scroll, value);
            }
        } catch (ReflectiveOperationException ignored) {
        }
    }
}
