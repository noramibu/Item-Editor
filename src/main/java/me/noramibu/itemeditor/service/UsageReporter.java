package me.noramibu.itemeditor.service;

import com.google.gson.JsonObject;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import me.noramibu.itemeditor.ItemEditorClient;
import me.noramibu.itemeditor.storage.StorageServices;
import me.noramibu.itemeditor.storage.model.PreferencesFileModel;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class UsageReporter {
    private static final String STARTUP_URL = "http://webhook.noramibu.me:26900/webhook/Item%20Editor/Startup";
    private static final String ACTIVE_URL = "http://webhook.noramibu.me:26900/webhook/Item%20Editor/Active";
    private static final String STARTUP_API_KEY = webhookKey("itemeditor:startup-webhook-key");
    private static final String ACTIVE_API_KEY = webhookKey("itemeditor:active-webhook-key");
    private static final Logger LOGGER = LoggerFactory.getLogger(ItemEditorClient.MOD_ID);
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private static final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "itemeditor-usage-reporter");
        thread.setDaemon(true);
        return thread;
    });
    private static volatile boolean statisticsEnabled;

    private UsageReporter() {}

    public static void initialize() {
        statisticsEnabled = StorageServices.foundation().loadPreferences().statisticsEnabled;
        registerCommand();
        logStatus();
        EXECUTOR.schedule(() -> send(STARTUP_URL, STARTUP_API_KEY), 5, TimeUnit.SECONDS);
        EXECUTOR.scheduleAtFixedRate(() -> send(ACTIVE_URL, ACTIVE_API_KEY), 5, 5, TimeUnit.MINUTES);
    }

    private static void send(String url, String apiKey) {
        if (!enabled() || apiKey.isBlank()) {
            return;
        }
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/json")
                    .header("X-API-Key", apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(buildPayload().toString()))
                    .build();
            HTTP_CLIENT
                    .sendAsync(request, HttpResponse.BodyHandlers.discarding())
                    .exceptionally(ignored -> null);
        } catch (RuntimeException ignored) {
        }
    }

    private static JsonObject buildPayload() {
        FabricLoader loader = FabricLoader.getInstance();
        var user = Minecraft.getInstance().getUser();
        JsonObject payload = new JsonObject();
        payload.addProperty(
                "minecraft-version",
                loader.getModContainer("minecraft")
                        .orElseThrow()
                        .getMetadata()
                        .getVersion()
                        .getFriendlyString());
        payload.addProperty(
                "mod-version",
                Integer.parseInt(loader.getModContainer(ItemEditorClient.MOD_ID)
                        .orElseThrow()
                        .getMetadata()
                        .getVersion()
                        .getFriendlyString()));
        payload.addProperty("username", user.getName());
        payload.addProperty("uuid", user.getProfileId().toString());
        return payload;
    }

    private static void registerCommand() {
        ClientCommandRegistrationCallback.EVENT.register(
                (dispatcher, access) -> dispatcher.register(ClientCommands.literal("itemeditor")
                        .then(ClientCommands.literal("statistics").executes(context -> {
                            boolean enabled = !enabled();
                            PreferencesFileModel preferences =
                                    StorageServices.foundation().loadPreferences();
                            preferences.statisticsEnabled = enabled;
                            StorageServices.foundation().savePreferences(preferences);
                            statisticsEnabled = enabled;
                            context.getSource()
                                    .sendFeedback(Component.literal(
                                            "Item Editor usage statistics " + (enabled ? "enabled." : "disabled.")));
                            logStatus();
                            return 1;
                        }))));
    }

    private static boolean enabled() {
        return statisticsEnabled;
    }

    private static void logStatus() {
        if (enabled()) {
            LOGGER.info(
                    "[Item Editor] Usage statistics are enabled. Minecraft and mod versions are collected; username and UUID are collected for online account validation. Run /itemeditor statistics to disable them.");
        } else {
            LOGGER.info("[Item Editor] Usage statistics are disabled. Run /itemeditor statistics to enable them.");
        }
    }

    private static String webhookKey(String key) {
        var metadata = FabricLoader.getInstance()
                .getModContainer(ItemEditorClient.MOD_ID)
                .orElseThrow()
                .getMetadata();
        return metadata.containsCustomValue(key) ? metadata.getCustomValue(key).getAsString() : "";
    }
}
