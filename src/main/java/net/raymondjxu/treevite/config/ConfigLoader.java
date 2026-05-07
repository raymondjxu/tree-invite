package net.raymondjxu.treevite.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import net.raymondjxu.treevite.TreeInvite;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Loads configuration from file or remote URL.
 * Supports environment variable TREEINVITE_FETCH_CONFIG_URL for remote config.
 */
public class ConfigLoader {
	private static final String CONFIG_DIR = "config";
	private static final String CONFIG_FILE = "tree-invite-messages.json";
	private static final String ENV_URL = "TREEINVITE_FETCH_CONFIG_URL";
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	/**
	 * Loads the config from file or creates it from defaults/remote.
	 * Priority:
	 * 1. If config file exists, load from file
	 * 2. If TREEINVITE_FETCH_CONFIG_URL env var is set, fetch from URL and save
	 * 3. Otherwise, create from built-in defaults
	 */
	public static Config load(Path serverDirectory) {
		Path configDir = serverDirectory.resolve(CONFIG_DIR);
		Path configPath = configDir.resolve(CONFIG_FILE);

		// If config file exists, load and return it
		if (Files.exists(configPath)) {
			try (FileReader reader = new FileReader(configPath.toFile())) {
				return GSON.fromJson(reader, Config.class);
			} catch (IOException | JsonSyntaxException e) {
				TreeInvite.LOGGER.warn("Failed to load config from {}, using defaults", configPath, e);
				return new Config();
			}
		}

		// Check for remote config URL
		String remoteUrl = System.getenv(ENV_URL);
		if (remoteUrl != null && !remoteUrl.isBlank()) {
			TreeInvite.LOGGER.info("Fetching remote config from {}", remoteUrl);
			try {
				Config config = fetchConfigFromUrl(remoteUrl);
				// Save fetched config locally
				ensureConfigDirExists(configDir);
				saveConfigToFile(config, configPath);
				return config;
			} catch (Exception e) {
				TreeInvite.LOGGER.warn("Failed to fetch remote config from {}, using defaults", remoteUrl, e);
				return new Config();
			}
		}

		// Create default config and save it
		Config defaultConfig = new Config();
		try {
			ensureConfigDirExists(configDir);
			saveConfigToFile(defaultConfig, configPath);
		} catch (IOException e) {
			TreeInvite.LOGGER.warn("Failed to save default config, using in-memory defaults", e);
		}

		return defaultConfig;
	}

	/**
	 * Fetches config from a remote URL via HTTP GET.
	 */
	private static Config fetchConfigFromUrl(String url) throws IOException, InterruptedException {
		HttpClient httpClient = HttpClient.newHttpClient();
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(url))
				.GET()
				.build();

		HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

		if (response.statusCode() != 200) {
			throw new IOException("HTTP " + response.statusCode() + ": " + response.body());
		}

		return GSON.fromJson(response.body(), Config.class);
	}

	/**
	 * Saves config to a JSON file.
	 */
	private static void saveConfigToFile(Config config, Path path) throws IOException {
		try (FileWriter writer = new FileWriter(path.toFile())) {
			GSON.toJson(config, writer);
		}
	}

	/**
	 * Ensures the config directory exists.
	 */
	private static void ensureConfigDirExists(Path configDir) throws IOException {
		Files.createDirectories(configDir);
	}
}
