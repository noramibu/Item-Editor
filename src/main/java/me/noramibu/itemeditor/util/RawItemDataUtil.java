package me.noramibu.itemeditor.util;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import me.noramibu.itemeditor.editor.ValidationMessage;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.SnbtPrinterTagVisitor;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class RawItemDataUtil {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();
    private static final String PACKET_CONTEXT_TOKEN = "packetcontext";
    private static final String NULL_TOKEN = "null";
    private static final String PACKET_CONTEXT_THROW_TOKEN =
            "orelsethrow(\"net.fabricmc.fabric.api.networking.v1.context.packetcontext";

    private RawItemDataUtil() {
    }

    public static String serialize(ItemStack stack, RegistryAccess registryAccess) {
        return serialize(stack, registryAccess, false);
    }

    public static String serialize(ItemStack stack, RegistryAccess registryAccess, boolean showKnownDefaults) {
        if (stack.isEmpty()) {
            return ItemEditorText.str("raw.empty");
        }

        DataResult<Tag> result = ItemStack.CODEC.encodeStart(registryAccess.createSerializationContext(NbtOps.INSTANCE), stack);
        return result.result()
                .map(tag -> printTag(withKnownDefaults(tag, showKnownDefaults)))
                .orElseGet(() -> ItemEditorText.str(
                        "raw.serialize_failed",
                        result.error().map(error -> error.message()).orElse(ItemEditorText.str("raw.unknown_error"))
                ));
    }

    public static String serializeJson(ItemStack stack, RegistryAccess registryAccess) {
        return serializeJson(stack, registryAccess, false);
    }

    public static String serializeJson(ItemStack stack, RegistryAccess registryAccess, boolean showKnownDefaults) {
        if (stack.isEmpty()) {
            return ItemEditorText.str("raw.empty");
        }

        DataResult<Tag> nbtResult = ItemStack.CODEC.encodeStart(registryAccess.createSerializationContext(NbtOps.INSTANCE), stack);
        if (nbtResult.result().isEmpty()) {
            return ItemEditorText.str(
                    "raw.serialize_failed",
                    nbtResult.error().map(error -> error.message()).orElse(ItemEditorText.str("raw.unknown_error"))
            );
        }

        Tag withDefaults = withKnownDefaults(nbtResult.result().get(), showKnownDefaults);
        try {
            JsonElement jsonElement = NbtOps.INSTANCE.convertTo(JsonOps.INSTANCE, withDefaults);
            return GSON.toJson(jsonElement);
        } catch (RuntimeException exception) {
            return ItemEditorText.str(
                    "raw.serialize_failed",
                    exception.getMessage() == null ? ItemEditorText.str("raw.unknown_error") : exception.getMessage()
            );
        }
    }

    private static String printTag(Tag tag) {
        return new SnbtPrinterTagVisitor().visit(tag);
    }

    public static ParseResult parse(String rawData, RegistryAccess registryAccess) {
        if (rawData == null || rawData.isBlank()) {
            return new ParseResult(null, ItemEditorText.str("raw.unknown_error"), -1, -1);
        }

        Tag parsedTag;
        try {
            parsedTag = parseWithNegativeSpecialLiteralFallback(rawData);
            parsedTag = normalizeSpecialFloatingLiterals(parsedTag, null);
        } catch (CommandSyntaxException exception) {
            int cursor = Math.clamp(exception.getCursor(), 0, rawData.length());
            int line = 1;
            int column = 1;
            for (int index = 0; index < cursor; index++) {
                if (rawData.charAt(index) == '\n') {
                    line++;
                    column = 1;
                } else {
                    column++;
                }
            }
            return new ParseResult(null, exception.getRawMessage().getString(), line, column);
        } catch (RuntimeException exception) {
            return new ParseResult(
                    null,
                    exception.getMessage() == null ? ItemEditorText.str("raw.unknown_error") : exception.getMessage(),
                    -1,
                    -1
            );
        }

        DataResult<ItemStack> result = ItemStack.CODEC.parse(
                registryAccess.createSerializationContext(NbtOps.INSTANCE),
                parsedTag
        );

        return result.result()
                .map(stack -> new ParseResult(stack, null, -1, -1))
                .orElseGet(() -> new ParseResult(
                        null,
                        result.error().map(error -> error.message()).orElse(ItemEditorText.str("raw.unknown_error")),
                        -1,
                        -1
                ));
    }

    private static Tag parseWithNegativeSpecialLiteralFallback(String rawData) throws CommandSyntaxException {
        try {
            return TagParser.create(NbtOps.INSTANCE).parseFully(rawData);
        } catch (CommandSyntaxException firstFailure) {
            String adjusted = quoteNegativeSpecialFloatingLiterals(rawData);
            if (adjusted.equals(rawData)) {
                throw firstFailure;
            }
            try {
                return TagParser.create(NbtOps.INSTANCE).parseFully(adjusted);
            } catch (CommandSyntaxException ignored) {
                throw firstFailure;
            }
        }
    }

    private static String quoteNegativeSpecialFloatingLiterals(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        StringBuilder output = new StringBuilder(input.length() + 16);
        boolean inString = false;
        boolean escaping = false;
        char quote = 0;

        for (int index = 0; index < input.length(); ) {
            char value = input.charAt(index);
            if (inString) {
                output.append(value);
                if (escaping) {
                    escaping = false;
                } else if (value == '\\') {
                    escaping = true;
                } else if (value == quote) {
                    inString = false;
                }
                index++;
                continue;
            }

            if (value == '"' || value == '\'') {
                inString = true;
                quote = value;
                output.append(value);
                index++;
                continue;
            }

            int end = scanNegativeSpecialFloatingLiteral(input, index);
            if (end > index
                    && (index == 0 || !isIdentifierLikeCharacter(input.charAt(index - 1)))
                    && (end >= input.length() || !isIdentifierLikeCharacter(input.charAt(end)))) {
                output.append('"').append(input, index, end).append('"');
                index = end;
                continue;
            }

            output.append(value);
            index++;
        }
        return output.toString();
    }

    private static int scanNegativeSpecialFloatingLiteral(String input, int index) {
        if (index < 0 || index >= input.length() || input.charAt(index) != '-') {
            return -1;
        }
        int cursor = index + 1;
        if (cursor + 8 <= input.length() && input.regionMatches(true, cursor, "infinity", 0, 8)) {
            cursor += 8;
        } else if (cursor + 3 <= input.length() && input.regionMatches(true, cursor, "nan", 0, 3)) {
            cursor += 3;
        } else {
            return -1;
        }
        if (cursor < input.length()) {
            char suffix = Character.toLowerCase(input.charAt(cursor));
            if (suffix == 'd' || suffix == 'f') {
                cursor++;
            }
        }
        return cursor;
    }

    private static boolean isIdentifierLikeCharacter(char value) {
        return Character.isLetterOrDigit(value)
                || value == '_'
                || value == '-'
                || value == '.'
                || value == '/'
                || value == ':'
                || value == '#';
    }

    private static Tag normalizeSpecialFloatingLiterals(Tag tag, String keyHint) {
        if (tag instanceof CompoundTag compoundTag) {
            for (String key : new ArrayList<>(compoundTag.keySet())) {
                Tag child = compoundTag.get(key);
                Tag normalized = normalizeSpecialFloatingLiterals(child, key);
                if (normalized != child) {
                    compoundTag.put(key, normalized);
                }
            }
            return compoundTag;
        }
        if (tag instanceof ListTag listTag) {
            for (int index = 0; index < listTag.size(); index++) {
                Tag child = listTag.get(index);
                Tag normalized = normalizeSpecialFloatingLiterals(child, keyHint);
                if (normalized != child) {
                    listTag.setTag(index, normalized);
                }
            }
            return listTag;
        }
        if (tag instanceof StringTag stringTag && shouldTreatAsNumericSlot(keyHint)) {
            Double parsed = parseSpecialFloatingLiteral(stringTag.value());
            if (parsed != null) {
                return DoubleTag.valueOf(parsed);
            }
        }
        return tag;
    }

    private static Double parseSpecialFloatingLiteral(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String token = value.trim();
        if (token.endsWith("d") || token.endsWith("D") || token.endsWith("f") || token.endsWith("F")) {
            token = token.substring(0, token.length() - 1);
        }
        if (token.isEmpty()) {
            return null;
        }

        int sign = 1;
        if (token.charAt(0) == '+') {
            token = token.substring(1);
        } else if (token.charAt(0) == '-') {
            sign = -1;
            token = token.substring(1);
        }
        if (token.isEmpty()) {
            return null;
        }

        if ("infinity".equalsIgnoreCase(token)) {
            return sign > 0 ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
        }
        if ("nan".equalsIgnoreCase(token)) {
            return Double.NaN;
        }
        return null;
    }

    private static boolean shouldTreatAsNumericSlot(String keyHint) {
        if (keyHint == null || keyHint.isBlank()) {
            return true;
        }
        String key = keyHint.toLowerCase(Locale.ROOT);
        return !(key.contains("text")
                || key.contains("name")
                || key.contains("title")
                || key.contains("subtitle")
                || key.contains("description")
                || key.contains("id")
                || key.contains("type")
                || key.contains("pattern")
                || key.contains("sound")
                || key.contains("translate")
                || key.contains("author")
                || key.contains("message"));
    }

    public static List<ValidationMessage> validatePreviewStack(ItemStack preview, RegistryAccess registryAccess) {
        return validatePreviewStack(preview, registryAccess, false);
    }

    public static List<ValidationMessage> validatePreviewStack(
            ItemStack preview,
            RegistryAccess registryAccess,
            boolean includePacketRoundTrip
    ) {
        List<ValidationMessage> messages = new ArrayList<>();
        boolean runtimeValid = validateRuntimeReferences(preview, messages);
        if (!runtimeValid) {
            return messages;
        }

        DataResult<ItemStack> strictResult = ItemStack.validateStrict(preview);
        strictResult.resultOrPartial(problem ->
                messages.add(ValidationMessage.error(ItemEditorText.str("preview.validation.component_failed", problem))));
        if (includePacketRoundTrip) {
            validatePacketRoundTrip(preview, registryAccess, messages);
        }
        return messages;
    }

    public static String format(String rawData, RegistryAccess registryAccess) {
        return format(rawData, registryAccess, false);
    }

    public static String format(String rawData, RegistryAccess registryAccess, boolean showKnownDefaults) {
        ParseResult parsed = parse(rawData, registryAccess);
        return parsed.success() ? serialize(parsed.stack(), registryAccess, showKnownDefaults) : rawData;
    }

    public static String minify(String rawData) {
        if (rawData == null || rawData.isEmpty()) {
            return "";
        }

        StringBuilder output = new StringBuilder(rawData.length());
        boolean inString = false;
        boolean escaping = false;
        for (int index = 0; index < rawData.length(); index++) {
            char value = rawData.charAt(index);
            if (inString) {
                output.append(value);
                if (escaping) {
                    escaping = false;
                } else if (value == '\\') {
                    escaping = true;
                } else if (value == '"') {
                    inString = false;
                }
                continue;
            }

            if (value == '"') {
                inString = true;
                output.append(value);
                continue;
            }

            if (!Character.isWhitespace(value)) {
                output.append(value);
            }
        }

        return output.toString();
    }

    public record ParseResult(ItemStack stack, String error, int line, int column) {
        public boolean success() {
            return this.stack != null && this.error == null;
        }

        public boolean hasPosition() {
            return this.line > 0 && this.column > 0;
        }
    }

    private static Tag withKnownDefaults(Tag source, boolean showKnownDefaults) {
        if (!showKnownDefaults) {
            return source;
        }

        Tag copy = source.copy();
        if (copy instanceof CompoundTag compoundTag) {
            applyKnownDefaults(compoundTag);
        }
        return copy;
    }

    private static void applyKnownDefaults(CompoundTag itemTag) {
        var componentsOptional = itemTag.getCompound("components");
        if (componentsOptional.isEmpty()) {
            return;
        }

        CompoundTag components = componentsOptional.get();
        applyFoodDefaults(components);
        applyConsumableDefaults(components);
    }

    private static void applyFoodDefaults(CompoundTag components) {
        var foodOptional = components.getCompound("minecraft:food");
        if (foodOptional.isEmpty()) {
            return;
        }

        CompoundTag food = foodOptional.get();
        if (!food.contains("can_always_eat")) {
            food.putBoolean("can_always_eat", false);
        }
    }

    private static void applyConsumableDefaults(CompoundTag components) {
        var consumableOptional = components.getCompound("minecraft:consumable");
        if (consumableOptional.isEmpty()) {
            return;
        }

        CompoundTag consumable = consumableOptional.get();
        if (!consumable.contains("consume_seconds")) {
            consumable.putFloat("consume_seconds", 1.6f);
        }
        if (!consumable.contains("animation")) {
            consumable.putString("animation", "eat");
        }
        if (!consumable.contains("sound")) {
            consumable.putString("sound", "minecraft:entity.generic.eat");
        }
        if (!consumable.contains("has_consume_particles")) {
            consumable.putBoolean("has_consume_particles", true);
        }
        if (!consumable.contains("on_consume_effects")) {
            consumable.put("on_consume_effects", new ListTag());
        }
    }

    private static boolean validateRuntimeReferences(ItemStack preview, List<ValidationMessage> messages) {
        return validateSpawnerEntityReferences(preview, messages);
    }

    private static boolean validateSpawnerEntityReferences(ItemStack preview, List<ValidationMessage> messages) {
        TypedEntityData<BlockEntityType<?>> blockEntityData = preview.get(DataComponents.BLOCK_ENTITY_DATA);
        if (blockEntityData == null || blockEntityData.type() != BlockEntityType.MOB_SPAWNER) {
            return true;
        }

        boolean valid = true;
        CompoundTag blockTag = blockEntityData.copyTagWithoutId();

        CompoundTag spawnData = blockTag.getCompoundOrEmpty("SpawnData");
        CompoundTag spawnDataEntity = spawnData.getCompoundOrEmpty("entity");
        String spawnEntityId = spawnDataEntity.getStringOr("id", "");
        if (spawnEntityId.isBlank()) {
            spawnEntityId = spawnData.getStringOr("id", "");
        }
        valid &= validateEntityTypeId(spawnEntityId, messages);

        ListTag potentials = blockTag.getListOrEmpty("SpawnPotentials");
        for (int index = 0; index < potentials.size(); index++) {
            CompoundTag potential = potentials.getCompoundOrEmpty(index);
            CompoundTag dataTag = potential.getCompoundOrEmpty("data");
            CompoundTag entityTag = dataTag.getCompoundOrEmpty("entity");
            String entityId = entityTag.getStringOr("id", "");
            if (entityId.isBlank()) {
                entityId = dataTag.getStringOr("id", "");
            }
            valid &= validateEntityTypeId(entityId, messages);
        }

        return valid;
    }

    private static boolean validateEntityTypeId(String entityIdRaw, List<ValidationMessage> messages) {
        if (entityIdRaw == null || entityIdRaw.isBlank()) {
            return true;
        }
        Identifier entityId = IdFieldNormalizer.parse(entityIdRaw);
        if (entityId != null && BuiltInRegistries.ENTITY_TYPE.containsKey(entityId)) {
            return true;
        }
        messages.add(ValidationMessage.error(ItemEditorText.str(
                "validation.registry_missing",
                ItemEditorText.str("special.spawner.entity_id"),
                entityIdRaw
        )));
        return false;
    }

    private static void validatePacketRoundTrip(ItemStack preview, RegistryAccess registryAccess, List<ValidationMessage> messages) {
        ByteBuf rawBuffer = Unpooled.buffer();
        try {
            ItemStack.OPTIONAL_STREAM_CODEC.encode(new RegistryFriendlyByteBuf(rawBuffer, registryAccess), preview);
            ItemStack.OPTIONAL_STREAM_CODEC.decode(new RegistryFriendlyByteBuf(rawBuffer, registryAccess));
        } catch (RuntimeException exception) {
            if (shouldFallbackToDataValidation(exception)) {
                validateDataRoundTrip(preview, registryAccess, messages);
                return;
            }
            String detail = exception.getMessage();
            if (detail == null || detail.isBlank()) {
                detail = exception.getClass().getSimpleName();
            }
            messages.add(ValidationMessage.error(ItemEditorText.str("preview.validation.packet_safe", detail)));
        } finally {
            rawBuffer.release();
        }
    }

    private static boolean shouldFallbackToDataValidation(Throwable throwable) {
        return isContextDependentPacketException(throwable)
                || hasThirdPartyFrame(throwable)
                || isBlindNullPointer(throwable);
    }

    private static boolean isBlindNullPointer(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof NullPointerException && (current.getMessage() == null || current.getMessage().isBlank())) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static void validateDataRoundTrip(ItemStack preview, RegistryAccess registryAccess, List<ValidationMessage> messages) {
        DynamicOps<Tag> ops = registryAccess.createSerializationContext(NbtOps.INSTANCE);
        DataResult<Tag> encoded = ItemStack.OPTIONAL_CODEC.encodeStart(ops, preview);
        Tag tag = encoded.resultOrPartial(problem ->
                        messages.add(ValidationMessage.error(ItemEditorText.str("preview.validation.component_failed", problem))))
                .orElse(null);
        if (tag == null) {
            return;
        }

        ItemStack.OPTIONAL_CODEC.parse(ops, tag)
                .resultOrPartial(problem ->
                        messages.add(ValidationMessage.error(ItemEditorText.str("preview.validation.component_failed", problem))));
    }

    private static boolean isContextDependentPacketException(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            String message = current.getMessage();
            if (message != null) {
                String normalized = message.toLowerCase(Locale.ROOT);
                if ((normalized.contains(PACKET_CONTEXT_TOKEN) && normalized.contains(NULL_TOKEN))
                        || normalized.contains(PACKET_CONTEXT_THROW_TOKEN)) {
                    return true;
                }
            }
            current = current.getCause();
        }

        return false;
    }

    private static boolean hasThirdPartyFrame(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            for (StackTraceElement element : current.getStackTrace()) {
                if (isThirdPartyFrame(element.getClassName())) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }

    private static boolean isThirdPartyFrame(String className) {
        return !(className.startsWith("java.")
                || className.startsWith("javax.")
                || className.startsWith("jdk.")
                || className.startsWith("sun.")
                || className.startsWith("com.sun.")
                || className.startsWith("io.netty.")
                || className.startsWith("com.mojang.")
                || className.startsWith("net.minecraft.")
                || className.startsWith("me.noramibu.itemeditor."));
    }

}
