package me.noramibu.itemeditor.util;

import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class RawRuntimeSuggestionProvider {
    private static final List<String> ALL_ITEMS_PROFILE = List.of("all_items");
    private static final String[] POTION_PATHS = {
            "potion",
            "splash_potion",
            "lingering_potion",
            "tipped_arrow",
            "suspicious_stew"
    };
    private static final String[] BLOCK_ENTITY_PATHS = {
            "chest",
            "trapped_chest",
            "barrel",
            "hopper",
            "furnace",
            "blast_furnace",
            "smoker",
            "brewing_stand",
            "jukebox",
            "beehive",
            "bee_nest",
            "decorated_pot",
            "brushable_block"
    };

    private static final TagKey<Item> TAG_SWORDS = itemTag("swords");
    private static final TagKey<Item> TAG_AXES = itemTag("axes");
    private static final TagKey<Item> TAG_PICKAXES = itemTag("pickaxes");
    private static final TagKey<Item> TAG_SHOVELS = itemTag("shovels");
    private static final TagKey<Item> TAG_HOES = itemTag("hoes");
    private static final TagKey<Item> TAG_TRIMMABLE_ARMOR = itemTag("trimmable_armor");
    private static final TagKey<Item> TAG_HEAD_ARMOR = itemTag("head_armor");
    private static final TagKey<Item> TAG_CHEST_ARMOR = itemTag("chest_armor");
    private static final TagKey<Item> TAG_LEG_ARMOR = itemTag("leg_armor");
    private static final TagKey<Item> TAG_FOOT_ARMOR = itemTag("foot_armor");
    private static final TagKey<Item> TAG_LECTERN_BOOKS = itemTag("lectern_books");
    private static final TagKey<Item> TAG_BOOKSHELF_BOOKS = itemTag("bookshelf_books");
    private static final TagKey<Item> TAG_BOOK_CLONING_TARGET = itemTag("book_cloning_target");
    private static final TagKey<Item> TAG_BANNERS = itemTag("banners");
    private static final TagKey<Item> TAG_BUNDLES = itemTag("bundles");
    private static final TagKey<Item> TAG_SHULKER_BOXES = itemTag("shulker_boxes");
    private static final TagKey<Item> TAG_FISHES = itemTag("fishes");
    private static final TagKey<Item> TAG_SKULLS = itemTag("skulls");
    private static final TagKey<Item> TAG_ENCHANTABLE_ARMOR = itemTag("enchantable/armor");
    private static final TagKey<Item> TAG_ENCHANTABLE_WEAPON = itemTag("enchantable/weapon");
    private static final TagKey<Item> TAG_ENCHANTABLE_MELEE_WEAPON = itemTag("enchantable/melee_weapon");
    private static final TagKey<Item> TAG_ENCHANTABLE_MINING = itemTag("enchantable/mining");
    private static final TagKey<Item> TAG_ENCHANTABLE_BOW = itemTag("enchantable/bow");
    private static final TagKey<Item> TAG_ENCHANTABLE_CROSSBOW = itemTag("enchantable/crossbow");
    private static final TagKey<Item> TAG_ENCHANTABLE_TRIDENT = itemTag("enchantable/trident");
    private static final TagKey<Item> TAG_MUSIC_DISCS = itemTag("music_discs");

    private final Object lock = new Object();
    private final Map<String, List<String>> cache = new HashMap<>();
    private final Map<String, List<String>> profileCache = new HashMap<>();
    private int registryAccessIdentity = -1;

    public <T> List<String> registryIds(
            RegistryAccess registryAccess,
            ResourceKey<Registry<T>> registryKey,
            Registry<T> builtinFallback
    ) {
        return this.ids(registryAccess, registryKey, builtinFallback);
    }

    public <T> List<String> registryTagIds(
            RegistryAccess registryAccess,
            ResourceKey<Registry<T>> registryKey,
            Registry<T> builtinFallback
    ) {
        return this.tagIds(registryAccess, registryKey, builtinFallback);
    }

    public List<String> itemProfiles(String itemId, RegistryAccess registryAccess) {
        if (itemId == null || itemId.isBlank()) {
            return ALL_ITEMS_PROFILE;
        }

        synchronized (this.lock) {
            this.ensureRegistryIdentity(registryAccess);

            String normalizedId = itemId.toLowerCase(Locale.ROOT);
            List<String> cached = this.profileCache.get(normalizedId);
            if (cached != null) {
                return cached;
            }

            List<String> computed = this.computeItemProfiles(normalizedId, registryAccess);
            this.profileCache.put(normalizedId, computed);
            return computed;
        }
    }

    private <T> List<String> ids(
            RegistryAccess registryAccess,
            ResourceKey<Registry<T>> registryKey,
            Registry<T> builtinFallback
    ) {
        synchronized (this.lock) {
            this.ensureRegistryIdentity(registryAccess);

            String key = registryKey.toString();
            List<String> cached = this.cache.get(key);
            if (cached != null) {
                return cached;
            }

            List<String> computed = fromRegistryAccess(registryAccess, registryKey);
            if (computed.isEmpty() && builtinFallback != null) {
                computed = fromBuiltinRegistry(builtinFallback);
            }
            this.cache.put(key, computed);
            return computed;
        }
    }

    public List<String> blockStateProperties(String blockId, RegistryAccess registryAccess) {
        Identifier identifier = Identifier.tryParse(blockId);
        if (identifier == null) {
            return List.of();
        }

        synchronized (this.lock) {
            this.ensureRegistryIdentity(registryAccess);

            String key = "block-state-properties:" + identifier;
            List<String> cached = this.cache.get(key);
            if (cached != null) {
                return cached;
            }

            List<String> computed = this.computeBlockStateProperties(identifier, registryAccess);
            this.cache.put(key, computed);
            return computed;
        }
    }

    private <T> List<String> tagIds(
            RegistryAccess registryAccess,
            ResourceKey<Registry<T>> registryKey,
            Registry<T> builtinFallback
    ) {
        synchronized (this.lock) {
            this.ensureRegistryIdentity(registryAccess);

            String key = registryKey + "#tags";
            List<String> cached = this.cache.get(key);
            if (cached != null) {
                return cached;
            }

            List<String> computed = fromRegistryAccessTags(registryAccess, registryKey);
            if (computed.isEmpty() && builtinFallback != null) {
                computed = fromRegistryTags(builtinFallback);
            }
            this.cache.put(key, computed);
            return computed;
        }
    }

    private void ensureRegistryIdentity(RegistryAccess registryAccess) {
        int currentIdentity = System.identityHashCode(registryAccess);
        if (currentIdentity == this.registryAccessIdentity) {
            return;
        }

        this.cache.clear();
        this.profileCache.clear();
        this.registryAccessIdentity = currentIdentity;
    }

    private List<String> computeItemProfiles(String itemId, RegistryAccess registryAccess) {
        Identifier identifier = Identifier.tryParse(itemId);
        if (identifier == null) {
            return ALL_ITEMS_PROFILE;
        }

        Registry<Item> itemRegistry;
        try {
            itemRegistry = registryAccess.lookupOrThrow(Registries.ITEM);
        } catch (RuntimeException ignored) {
            return ALL_ITEMS_PROFILE;
        }

        var itemHolder = itemRegistry.get(identifier).orElse(null);
        if (itemHolder == null) {
            return ALL_ITEMS_PROFILE;
        }

        ItemStack probe = new ItemStack(itemHolder.value());
        String path = identifier.getPath();
        Set<String> profiles = new LinkedHashSet<>(ALL_ITEMS_PROFILE);

        if (probe.is(TAG_SWORDS)) {
            profiles.add("swords");
        }
        if (probe.is(TAG_AXES)) {
            profiles.add("axes");
        }
        if (probe.is(TAG_PICKAXES)) {
            profiles.add("pickaxes");
        }
        if (probe.is(TAG_SHOVELS)) {
            profiles.add("shovels");
        }
        if (probe.is(TAG_HOES)) {
            profiles.add("hoes");
        }
        if (probe.is(TAG_ENCHANTABLE_BOW) || "bow".equals(path)) {
            profiles.add("bows");
        }
        if (probe.is(TAG_ENCHANTABLE_CROSSBOW) || path.contains("crossbow")) {
            profiles.add("crossbows");
        }
        if (probe.is(TAG_ENCHANTABLE_TRIDENT) || path.endsWith("trident")) {
            profiles.add("tridents");
        }
        if (probe.is(TAG_TRIMMABLE_ARMOR)
                || probe.is(TAG_HEAD_ARMOR)
                || probe.is(TAG_CHEST_ARMOR)
                || probe.is(TAG_LEG_ARMOR)
                || probe.is(TAG_FOOT_ARMOR)
                || probe.is(TAG_ENCHANTABLE_ARMOR)) {
            profiles.add("armor");
        }
        if (probe.is(TAG_LECTERN_BOOKS)
                || probe.is(TAG_BOOKSHELF_BOOKS)
                || probe.is(TAG_BOOK_CLONING_TARGET)
                || "book".equals(path)
                || "writable_book".equals(path)
                || "written_book".equals(path)) {
            profiles.add("books");
        }
        if (probe.is(TAG_BANNERS)) {
            profiles.add("banners");
        }
        if (probe.is(TAG_BUNDLES)) {
            profiles.add("bundles");
        }
        if (probe.is(TAG_SHULKER_BOXES)) {
            profiles.add("shulker_boxes");
        }
        if (path.endsWith("_spawn_egg")) {
            profiles.add("spawn_eggs");
        }
        if ("bucket".equals(path) || path.endsWith("_bucket") || probe.is(TAG_FISHES)) {
            profiles.add("bucket_items");
        }
        if (pathEqualsAny(path, POTION_PATHS)) {
            profiles.add("potions");
        }
        if (pathEqualsAny(path, "firework_rocket", "firework_star")) {
            profiles.add("fireworks");
        }
        if (pathEqualsAny(path, "map", "filled_map")) {
            profiles.add("maps");
        }
        if (probe.is(TAG_MUSIC_DISCS) || path.startsWith("music_disc_") || "disc_fragment_5".equals(path)) {
            profiles.add("music_discs");
        }
        if ("goat_horn".equals(path)) {
            profiles.add("goat_horns");
        }
        if (probe.is(TAG_SKULLS)) {
            profiles.add("heads");
        }
        if (probe.has(DataComponents.FOOD)) {
            profiles.add("food_items");
        }
        if (probe.isDamageableItem() || probe.has(DataComponents.MAX_DAMAGE)) {
            profiles.add("durable_items");
        }
        if (probe.is(TAG_PICKAXES) || probe.is(TAG_SHOVELS) || probe.is(TAG_HOES) || probe.is(TAG_ENCHANTABLE_MINING)) {
            profiles.add("tools");
        }
        if (probe.is(TAG_SWORDS)
                || probe.is(TAG_AXES)
                || probe.is(TAG_ENCHANTABLE_WEAPON)
                || probe.is(TAG_ENCHANTABLE_MELEE_WEAPON)
                || probe.is(TAG_ENCHANTABLE_BOW)
                || probe.is(TAG_ENCHANTABLE_CROSSBOW)
                || probe.is(TAG_ENCHANTABLE_TRIDENT)) {
            profiles.add("weapons");
        }
        if (probe.is(TAG_ENCHANTABLE_TRIDENT)) {
            profiles.add("spears");
        }
        if ("bucket".equals(path) || path.endsWith("_bucket") || probe.is(TAG_FISHES)) {
            profiles.add("buckets");
        }
        if (path.contains("spawner")
                || pathEqualsAny(path, BLOCK_ENTITY_PATHS)
                || probe.is(TAG_BANNERS)
                || probe.is(TAG_SHULKER_BOXES)) {
            profiles.add("block_entity_items");
            profiles.add("containers");
        }
        if (probe.is(TAG_TRIMMABLE_ARMOR)) {
            profiles.add("trimmable_armor");
        }

        if (probe.is(TAG_ENCHANTABLE_WEAPON) || probe.is(TAG_ENCHANTABLE_MELEE_WEAPON)) {
            profiles.add("swords");
            profiles.add("axes");
            profiles.add("tridents");
        }
        if (probe.is(TAG_ENCHANTABLE_MINING)) {
            profiles.add("pickaxes");
            profiles.add("axes");
            profiles.add("shovels");
            profiles.add("hoes");
        }

        return List.copyOf(profiles);
    }

    private List<String> computeBlockStateProperties(Identifier blockId, RegistryAccess registryAccess) {
        Block block = blockById(blockId, registryAccess);
        if (block == null) {
            return List.of();
        }

        return block.defaultBlockState().getProperties().stream()
                .map(Property::getName)
                .sorted()
                .toList();
    }

    private static Block blockById(Identifier blockId, RegistryAccess registryAccess) {
        try {
            Registry<Block> blockRegistry = registryAccess.lookupOrThrow(Registries.BLOCK);
            var holder = blockRegistry.get(blockId).orElse(null);
            if (holder != null) {
                return holder.value();
            }
        } catch (RuntimeException ignored) {
        }
        var holder = BuiltInRegistries.BLOCK.get(blockId).orElse(null);
        return holder == null ? null : holder.value();
    }

    private static TagKey<Item> itemTag(String path) {
        Identifier id = Identifier.fromNamespaceAndPath("minecraft", path);
        return TagKey.create(Registries.ITEM, id);
    }

    private static <T> List<String> fromRegistryAccess(RegistryAccess registryAccess, ResourceKey<Registry<T>> registryKey) {
        try {
            return registryAccess.lookupOrThrow(registryKey).keySet().stream()
                    .map(Identifier::toString)
                    .sorted()
                    .toList();
        } catch (RuntimeException ignored) {
            return List.of();
        }
    }

    private static <T> List<String> fromBuiltinRegistry(Registry<T> registry) {
        List<String> values = new ArrayList<>(registry.size());
        for (Identifier id : registry.keySet()) {
            values.add(id.toString());
        }
        values.sort(String::compareTo);
        return List.copyOf(values);
    }

    private static <T> List<String> fromRegistryAccessTags(
            RegistryAccess registryAccess,
            ResourceKey<Registry<T>> registryKey
    ) {
        try {
            return registryAccess.lookupOrThrow(registryKey).getTags()
                    .map(named -> "#" + named.key().location())
                    .sorted()
                    .toList();
        } catch (RuntimeException ignored) {
            return List.of();
        }
    }

    private static <T> List<String> fromRegistryTags(Registry<T> registry) {
        try {
            return registry.getTags()
                    .map(named -> "#" + named.key().location())
                    .sorted()
                    .toList();
        } catch (RuntimeException ignored) {
            return List.of();
        }
    }

    private static boolean pathEqualsAny(String path, String... values) {
        for (String value : values) {
            if (value.equals(path)) {
                return true;
            }
        }
        return false;
    }
}
