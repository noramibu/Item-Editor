package me.noramibu.itemeditor.storage.search;

import java.util.ArrayList;
import java.util.List;

public final class StorageSearchQuery {

    public final List<String> itemTokens = new ArrayList<>();
    public final List<String> nameTokens = new ArrayList<>();
    public final List<String> loreTokens = new ArrayList<>();
    public final List<String> freeTokens = new ArrayList<>();
    public final List<Long> beforeDurationsMs = new ArrayList<>();
    public final List<Long> afterDurationsMs = new ArrayList<>();
    public final List<NumericFilter> amountFilters = new ArrayList<>();
    public final List<NumericFilter> nbtSizeFilters = new ArrayList<>();

    public boolean isEmpty() {
        return this.itemTokens.isEmpty()
                && this.nameTokens.isEmpty()
                && this.loreTokens.isEmpty()
                && this.freeTokens.isEmpty()
                && this.beforeDurationsMs.isEmpty()
                && this.afterDurationsMs.isEmpty()
                && this.amountFilters.isEmpty()
                && this.nbtSizeFilters.isEmpty();
    }

    public record NumericFilter(Mode mode, int left, int right) {
        public boolean matches(int size) {
            int normalized = Math.max(1, size);
            return switch (this.mode) {
                case EQ -> normalized == this.left;
                case LT -> normalized < this.left;
                case LTE -> normalized <= this.left;
                case GT -> normalized > this.left;
                case GTE -> normalized >= this.left;
                case RANGE -> normalized >= this.left && normalized <= this.right;
            };
        }
    }

    public enum Mode {
        EQ,
        LT,
        LTE,
        GT,
        GTE,
        RANGE
    }
}
