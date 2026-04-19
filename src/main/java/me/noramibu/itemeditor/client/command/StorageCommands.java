package me.noramibu.itemeditor.client.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import me.noramibu.itemeditor.storage.StorageSortMode;
import me.noramibu.itemeditor.ui.screen.StorageScreen;
import me.noramibu.itemeditor.util.ItemEditorText;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Locale;

public final class StorageCommands {

    private static final String[] SEARCH_QUERY_SUGGESTIONS = {
            "item:\"minecraft:*_shulker_*\"",
            "item:\"minecraft:stone\"",
            "name:\"\"",
            "lore:\"\"",
            "amount:64",
            "a:>=16",
            "amount:1-32",
            "size:>=128",
            "size:256-2048",
            "before:30m",
            "before:24h",
            "before:7d",
            "after:30m",
            "after:24h",
            "after:7d"
    };
    private static final List<String> SORT_MODE_SUGGESTIONS = List.of("regular", "saved", "name", "amount", "size");

    private StorageCommands() {
    }

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, access) -> dispatcher.register(
                ClientCommands.literal("storage")
                        .executes(context -> openStorage(context, 1, "", StorageSortMode.REGULAR))
                        .then(ClientCommands.literal("page")
                                .then(ClientCommands.argument("pageNumber", IntegerArgumentType.integer())
                                        .executes(context -> openStorage(context, IntegerArgumentType.getInteger(context, "pageNumber"), "", StorageSortMode.REGULAR))))
                        .then(ClientCommands.literal("p")
                                .then(ClientCommands.argument("pageNumber", IntegerArgumentType.integer())
                                        .executes(context -> openStorage(context, IntegerArgumentType.getInteger(context, "pageNumber"), "", StorageSortMode.REGULAR))))
                        .then(ClientCommands.literal("search")
                                .then(ClientCommands.argument("query", StringArgumentType.greedyString())
                                        .suggests((context, builder) -> {
                                            String remaining = builder.getRemaining();
                                            int lastSpace = remaining.lastIndexOf(' ');
                                            String base = lastSpace < 0 ? "" : remaining.substring(0, lastSpace + 1);
                                            String tokenPrefix = lastSpace < 0 ? remaining : remaining.substring(lastSpace + 1);
                                            String normalizedPrefix = tokenPrefix.toLowerCase(Locale.ROOT);
                                            List<String> candidates = java.util.Arrays.stream(SEARCH_QUERY_SUGGESTIONS)
                                                    .filter(token -> normalizedPrefix.isBlank() || token.toLowerCase(Locale.ROOT).startsWith(normalizedPrefix))
                                                    .map(token -> base + token)
                                                    .toList();
                                            return SharedSuggestionProvider.suggest(candidates, builder);
                                        })
                                        .executes(context -> openStorage(context, 1, StringArgumentType.getString(context, "query"), StorageSortMode.REGULAR))))
                        .then(ClientCommands.literal("s")
                                .then(ClientCommands.argument("query", StringArgumentType.greedyString())
                                        .suggests((context, builder) -> {
                                            String remaining = builder.getRemaining();
                                            int lastSpace = remaining.lastIndexOf(' ');
                                            String base = lastSpace < 0 ? "" : remaining.substring(0, lastSpace + 1);
                                            String tokenPrefix = lastSpace < 0 ? remaining : remaining.substring(lastSpace + 1);
                                            String normalizedPrefix = tokenPrefix.toLowerCase(Locale.ROOT);
                                            List<String> candidates = java.util.Arrays.stream(SEARCH_QUERY_SUGGESTIONS)
                                                    .filter(token -> normalizedPrefix.isBlank() || token.toLowerCase(Locale.ROOT).startsWith(normalizedPrefix))
                                                    .map(token -> base + token)
                                                    .toList();
                                            return SharedSuggestionProvider.suggest(candidates, builder);
                                        })
                                        .executes(context -> openStorage(context, 1, StringArgumentType.getString(context, "query"), StorageSortMode.REGULAR))))
                        .then(ClientCommands.literal("sort")
                                .then(ClientCommands.argument("mode", StringArgumentType.word())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(SORT_MODE_SUGGESTIONS, builder))
                                        .executes(context -> {
                                            String modeToken = StringArgumentType.getString(context, "mode");
                                            StorageSortMode sortMode = StorageSortMode.fromCommandToken(modeToken);
                                            if (sortMode == null) {
                                                context.getSource().sendError(
                                                        Component.literal("Unknown sort mode: " + modeToken + " (use: regular, saved, name, amount, size)")
                                                                .withStyle(ChatFormatting.RED)
                                                );
                                                return 0;
                                            }
                                            return openStorage(context, 1, "", sortMode);
                                        })))
        ));
    }

    private static int openStorage(
            CommandContext<FabricClientCommandSource> context,
            int page,
            String query,
            StorageSortMode sortMode
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            context.getSource().sendError(Component.literal(ItemEditorText.str("storage.command.no_world")).withStyle(ChatFormatting.RED));
            return 0;
        }
        int safePage = Math.max(1, page);
        StorageSortMode mode = sortMode == null ? StorageSortMode.REGULAR : sortMode;
        minecraft.execute(() -> minecraft.setScreen(new StorageScreen(safePage, query, mode)));
        return 1;
    }

}
