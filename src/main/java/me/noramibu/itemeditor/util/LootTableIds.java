package me.noramibu.itemeditor.util;

import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;

import java.util.List;
import java.util.Objects;

public final class LootTableIds {

    private LootTableIds() {
    }

    public static List<String> fromResources(ResourceManager resourceManager) {
        try {
            return resourceManager.listResources("loot_table", id -> id.getPath().endsWith(".json"))
                    .keySet()
                    .stream()
                    .map(LootTableIds::fromResourceId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .sorted()
                    .toList();
        } catch (RuntimeException ignored) {
            return List.of();
        }
    }

    private static String fromResourceId(Identifier resourceId) {
        String path = resourceId.getPath();
        String prefix = "loot_table/";
        String suffix = ".json";
        if (!path.startsWith(prefix) || !path.endsWith(suffix)) {
            return null;
        }

        String lootPath = path.substring(prefix.length(), path.length() - suffix.length());
        return lootPath.isBlank() ? null : resourceId.getNamespace() + ":" + lootPath;
    }
}
