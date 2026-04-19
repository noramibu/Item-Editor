package me.noramibu.itemeditor.storage;

public enum StorageSortMode {
    REGULAR,
    SAVED_AT_DESC,
    NAME_ASC,
    AMOUNT_DESC,
    NBT_SIZE_DESC;

    public StorageSortMode next() {
        return switch (this) {
            case REGULAR -> SAVED_AT_DESC;
            case SAVED_AT_DESC -> NAME_ASC;
            case NAME_ASC -> AMOUNT_DESC;
            case AMOUNT_DESC -> NBT_SIZE_DESC;
            case NBT_SIZE_DESC -> REGULAR;
        };
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
