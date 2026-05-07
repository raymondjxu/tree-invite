package net.raymondjxu.treevite.lookup;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.raymondjxu.treevite.data.Platform;

import java.math.BigInteger;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class ProfileLookup {
	private static final HttpClient CLIENT = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(10))
			.build();

	private ProfileLookup() {
	}

	public static CompletableFuture<LookupResult> lookup(String name, Platform platform) {
		if (platform == Platform.BEDROCK) {
			return lookupBedrock(name);
		}
		return lookupJava(name);
	}

	public static CompletableFuture<LookupResult> lookupJava(String name) {
		if (name == null || name.isBlank()) {
			return CompletableFuture.completedFuture(LookupResult.error("Missing player name"));
		}
		String encoded = URLEncoder.encode(name, StandardCharsets.UTF_8);
		String url = "https://api.mojang.com/users/profiles/minecraft/" + encoded;
		HttpRequest request = HttpRequest.newBuilder(URI.create(url))
				.timeout(Duration.ofSeconds(10))
				.GET()
				.build();

		return CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
				.thenApply(response -> {
					if (response.statusCode() != 200) {
						return LookupResult.error("Mojang lookup failed (HTTP " + response.statusCode() + ")");
					}
					String body = response.body();
					JsonObject obj = parseObject(body);
					if (obj == null) {
						return LookupResult.error("Mojang lookup failed (invalid response)");
					}
					String id = getAsString(obj, "id");
					String displayName = getAsString(obj, "name");
					UUID uuid = parseMojangUuid(id);
					if (uuid == null) {
						return LookupResult.error("Mojang lookup failed (invalid UUID)");
					}
					return LookupResult.success(uuid, displayName, Platform.JAVA);
				});
	}

	public static CompletableFuture<LookupResult> lookupBedrock(String gamertag) {
		if (gamertag == null || gamertag.isBlank()) {
			return CompletableFuture.completedFuture(LookupResult.error("Missing player name"));
		}
		String encoded = URLEncoder.encode(gamertag, StandardCharsets.UTF_8);
		String url = "https://api.geysermc.org/v2/xbox/xuid/" + encoded;
		HttpRequest request = HttpRequest.newBuilder(URI.create(url))
				.timeout(Duration.ofSeconds(10))
				.GET()
				.build();

		return CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
				.thenApply(response -> {
					if (response.statusCode() != 200) {
						return LookupResult.error("Bedrock lookup failed (HTTP " + response.statusCode() + ")");
					}
					String body = response.body();
					JsonObject obj = parseObject(body);
					if (obj == null) {
						return LookupResult.error("Bedrock lookup failed (invalid response)");
					}
					String xuid = getAsString(obj, "xuid");
					if (xuid == null) {
						return LookupResult.error("Bedrock lookup failed (missing XUID)");
					}
					UUID uuid = bedrockUuidFromXuid(xuid);
					if (uuid == null) {
						return LookupResult.error("Bedrock lookup failed (invalid XUID)");
					}
					String displayName = getAsString(obj, "display_name");
					return LookupResult.success(uuid, displayName == null ? gamertag : displayName, Platform.BEDROCK);
				});
	}

	private static JsonObject parseObject(String json) {
		if (json == null || json.isBlank()) {
			return null;
		}
		try {
			JsonElement element = JsonParser.parseString(json);
			if (element == null || !element.isJsonObject()) {
				return null;
			}
			return element.getAsJsonObject();
		} catch (Exception ex) {
			return null;
		}
	}

	private static String getAsString(JsonObject obj, String key) {
		if (obj == null || key == null) {
			return null;
		}
		JsonElement element = obj.get(key);
		if (element == null || element.isJsonNull()) {
			return null;
		}
		try {
			return element.getAsString();
		} catch (Exception ex) {
			return null;
		}
	}

	private static UUID parseMojangUuid(String value) {
		if (value == null || value.length() != 32) {
			return null;
		}
		String dashed = value.substring(0, 8) + "-" + value.substring(8, 12) + "-" + value.substring(12, 16) + "-" + value.substring(16, 20) + "-" + value.substring(20);
		try {
			return UUID.fromString(dashed);
		} catch (IllegalArgumentException ex) {
			return null;
		}
	}

	private static UUID bedrockUuidFromXuid(String xuid) {
		try {
			BigInteger value = new BigInteger(xuid);
			String hex = value.toString(16);
			if (hex.length() > 16) {
				return null;
			}
			String padded = String.format("%16s", hex).replace(' ', '0');
			String uuidText = "00000000-0000-0000-" + padded.substring(0, 4) + "-" + padded.substring(4);
			return UUID.fromString(uuidText);
		} catch (Exception ex) {
			return null;
		}
	}

	public static final class LookupResult {
		private final UUID uuid;
		private final String displayName;
		private final Platform platform;
		private final String error;

		private LookupResult(UUID uuid, String displayName, Platform platform, String error) {
			this.uuid = uuid;
			this.displayName = displayName;
			this.platform = platform;
			this.error = error;
		}

		public static LookupResult success(UUID uuid, String displayName, Platform platform) {
			return new LookupResult(uuid, displayName, platform, null);
		}

		public static LookupResult error(String message) {
			return new LookupResult(null, null, Platform.UNKNOWN, message == null ? "Lookup failed" : message);
		}

		public boolean isSuccess() {
			return uuid != null;
		}

		public UUID getUuid() {
			return uuid;
		}

		public String getDisplayName() {
			return displayName;
		}

		public Platform getPlatform() {
			return platform;
		}

		public String getError() {
			return error;
		}
	}
}
