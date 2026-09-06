package me.noramibu.itemeditor.service;

import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class PlayerUuidResolver {
    private static final HttpClient CLIENT =
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    private PlayerUuidResolver() {}

    public static CompletableFuture<Result> resolve(String name) {
        String query = name.trim();
        if (!query.matches("[A-Za-z0-9_]{1,16}"))
            return CompletableFuture.completedFuture(new Result(null, "", "invalid"));
        var request = HttpRequest.newBuilder(URI.create("https://api.mojang.com/users/profiles/minecraft/" + query))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();
        return CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> parse(response.statusCode(), response.body()))
                .exceptionally(error -> new Result(null, "", "failed"));
    }

    static Result parse(int status, String body) {
        if (status != 200)
            return new Result(
                    null, "", status == 204 || status == 404 ? "not_found" : status == 429 ? "rate_limit" : "failed");
        try {
            var json = JsonParser.parseString(body).getAsJsonObject();
            String id = json.get("id").getAsString();
            String name = json.get("name").getAsString();
            if (!id.matches("[0-9a-fA-F]{32}") || !name.matches("[A-Za-z0-9_]{1,16}"))
                return new Result(null, "", "failed");
            UUID uuid = new UUID(
                    Long.parseUnsignedLong(id.substring(0, 16), 16), Long.parseUnsignedLong(id.substring(16), 16));
            return new Result(uuid, name, "");
        } catch (RuntimeException exception) {
            return new Result(null, "", "failed");
        }
    }

    public record Result(UUID uuid, String name, String error) {}
}
