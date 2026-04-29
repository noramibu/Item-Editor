package me.noramibu.itemeditor.client.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import me.noramibu.itemeditor.storage.StorageSortMode;
import me.noramibu.itemeditor.storage.search.StorageSearchAutocompleteUtil;
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
import java.util.concurrent.CompletableFuture;

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
                                        .suggests(StorageCommands::suggestSearchQuery)
                                        .executes(context -> openStorage(context, 1, StringArgumentType.getString(context, "query"), StorageSortMode.REGULAR))))
                        .then(ClientCommands.literal("s")
                                .then(ClientCommands.argument("query", StringArgumentType.greedyString())
                                        .suggests(StorageCommands::suggestSearchQuery)
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
        minecraft.execute(() -> minecraft.setScreen(new StorageScreen(safePage, query, sortMode)));
        return 1;
    }

    private static CompletableFuture<Suggestions> suggestSearchQuery(
            CommandContext<FabricClientCommandSource> ignoredContext,
            SuggestionsBuilder builder
    ) {
        var completion = StorageSearchAutocompleteUtil.complete(builder.getRemaining(), SEARCH_QUERY_SUGGESTIONS);
        return SharedSuggestionProvider.suggest(completion.withBase(), builder);
    }

}
