package me.noramibu.itemeditor.storage;

public enum StorageSortMode {
    REGULAR,
    SAVED_AT_DESC,
    NAME_ASC,
    AMOUNT_DESC,
    NBT_SIZE_DESC;

    public StorageSortMode next() {
        StorageSortMode[] modes = values();
        return modes[(this.ordinal() + 1) % modes.length];
    }

    public static StorageSortMode fromCommandToken(String token) {
        if (token == null) {
            return null;
        }
        return switch (token.trim().toLowerCase()) {
            case "regular", "slots" -> REGULAR;
            case "saved", "time", "saved_at" -> SAVED_AT_DESC;
            case "name" -> NAME_ASC;
            case "amount", "count", "stack" -> AMOUNT_DESC;
            case "size", "bytes", "nbt", "nbt_size" -> NBT_SIZE_DESC;
            default -> null;
        };
    }
}
