package me.noramibu.itemeditor.util;

import java.util.List;
import java.util.Objects;
import net.minecraft.server.packs.resources.ResourceManager;

public final class LootTableIds {

    private LootTableIds() {}

    public static List<String> fromResources(ResourceManager resourceManager) {
        try {
            return resourceManager
                    .listResources("loot_table", id -> id.getPath().endsWith(".json"))
                    .keySet()
                    .stream()
                    .map(id -> RegistryUtil.resourceId(id, "loot_table/", ".json"))
                    .filter(Objects::nonNull)
                    .distinct()
                    .sorted()
                    .toList();
        } catch (RuntimeException ignored) {
            return List.of();
        }
    }
}
