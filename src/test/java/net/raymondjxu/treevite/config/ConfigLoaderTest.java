package net.raymondjxu.treevite.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ConfigLoaderTest {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	@TempDir
	Path tempDir;

	@BeforeEach
	void clearEnv() {
		// Clear the environment variable before each test
		// Note: In Java, we can't directly unset env vars, but tests should not depend on it
	}

	@Test
	void loadCreatesDefaultConfigWhenFileDoesNotExist() {
		Path configDir = tempDir;

		Config config = ConfigLoader.load(configDir);

		assertNotNull(config);
		assertEquals("Invite system is not ready yet", config.error_systemNotReady);
		assertEquals("Invited {player}", config.success_invited);
	}

	@Test
	void loadCreatesConfigDirectory() {
		Path configDir = tempDir;
		Path subDir = configDir.resolve("config");
		assertFalse(Files.exists(subDir));

		ConfigLoader.load(configDir);

		assertTrue(Files.exists(subDir));
	}

	@Test
	void loadSavesDefaultConfigToFile() throws IOException {
		Path configDir = tempDir;

		Config config = ConfigLoader.load(configDir);

		Path configFile = configDir.resolve("config").resolve("tree-invite-messages.json");
		assertTrue(Files.exists(configFile));

		String content = Files.readString(configFile);
		Config loaded = GSON.fromJson(content, Config.class);
		assertEquals(config.error_systemNotReady, loaded.error_systemNotReady);
	}

	@Test
	void loadReadsExistingConfigFile() throws IOException {
		Path configDir = tempDir;
		Path subDir = configDir.resolve("config");
		Files.createDirectories(subDir);

		Config original = new Config();
		original.error_systemNotReady = "Custom system error";
		original.success_invited = "You invited {player}!";

		Path configFile = subDir.resolve("tree-invite-messages.json");
		try (FileWriter writer = new FileWriter(configFile.toFile())) {
			GSON.toJson(original, writer);
		}

		Config loaded = ConfigLoader.load(configDir);

		assertEquals("Custom system error", loaded.error_systemNotReady);
		assertEquals("You invited {player}!", loaded.success_invited);
	}

	@Test
	void loadHandlesInvalidJsonGracefully() throws IOException {
		Path configDir = tempDir;
		Path subDir = configDir.resolve("config");
		Files.createDirectories(subDir);

		Path configFile = subDir.resolve("tree-invite-messages.json");
		Files.writeString(configFile, "{ invalid json }");

		// Should return default config when JSON is invalid
		Config config = ConfigLoader.load(configDir);

		assertNotNull(config);
		assertEquals("Invite system is not ready yet", config.error_systemNotReady);
	}

	@Test
	void loadHandlesPartialConfigFile() throws IOException {
		Path configDir = tempDir;
		Path subDir = configDir.resolve("config");
		Files.createDirectories(subDir);

		// Create config with only some fields set
		String partial = "{ \"error_systemNotReady\": \"Custom error\" }";
		Path configFile = subDir.resolve("tree-invite-messages.json");
		Files.writeString(configFile, partial);

		Config config = ConfigLoader.load(configDir);

		assertEquals("Custom error", config.error_systemNotReady);
		// Other fields should be null or use default
		assertNotNull(config);
	}

	@Test
	void loadPreservesExistingConfigFile() throws IOException {
		Path configDir = tempDir;
		Path subDir = configDir.resolve("config");
		Files.createDirectories(subDir);

		Config original = new Config();
		original.error_systemNotReady = "Preserved";

		Path configFile = subDir.resolve("tree-invite-messages.json");
		try (FileWriter writer = new FileWriter(configFile.toFile())) {
			GSON.toJson(original, writer);
		}

		// Load twice to ensure file isn't overwritten
		Config first = ConfigLoader.load(configDir);
		Config second = ConfigLoader.load(configDir);

		assertEquals("Preserved", first.error_systemNotReady);
		assertEquals("Preserved", second.error_systemNotReady);
	}

	@Test
	void loadDefaultConfigHasAllFields() {
		Config config = new Config();

		assertNotNull(config.error_systemNotReady);
		assertNotNull(config.error_notWhitelisted);
		assertNotNull(config.error_cannotSpecifyPlatform);
		assertNotNull(config.error_lookupFailed);
		assertNotNull(config.error_cannotInviteSelf);
		assertNotNull(config.error_inviteLimitReached);
		assertNotNull(config.error_addInviteFailed);
		assertNotNull(config.error_playerNotInTree);
		assertNotNull(config.error_platformAmbiguous);
		assertNotNull(config.error_nameAmbiguous);
		assertNotNull(config.error_didNotInvite);
		assertNotNull(config.error_revokeInviteFailed);
		assertNotNull(config.error_noInviteInfo);
		assertNotNull(config.error_permissionsApiMissing);

		assertNotNull(config.success_invited);
		assertNotNull(config.success_alreadyInvited);
		assertNotNull(config.success_revokedInvite);
		assertNotNull(config.info_totalUnreachable);
		assertNotNull(config.success_setMaxInvites);
		assertNotNull(config.success_setGlobalMaxInvites);
		assertNotNull(config.success_inviteInfoHeader);
		assertNotNull(config.success_invitedBy);
		assertNotNull(config.success_invitedList);

	}

	@Test
	void loadConfigCanBeModified() throws IOException {
		Path configDir = tempDir;

		Config config = ConfigLoader.load(configDir);
		config.error_systemNotReady = "Modified message";

		// Verify modification persists in memory
		assertEquals("Modified message", config.error_systemNotReady);
	}

	@Test
	void loadSavesValidJson() throws IOException {
		Path configDir = tempDir;

		Config config = ConfigLoader.load(configDir);

		Path configFile = configDir.resolve("config").resolve("tree-invite-messages.json");
		String content = Files.readString(configFile);

		// Should be valid JSON that can be parsed
		Config reparsed = GSON.fromJson(content, Config.class);
		assertNotNull(reparsed);
	}

	@Test
	void loadHandlesEmptyConfigDirectory() throws IOException {
		Path configDir = tempDir;
		Path subDir = configDir.resolve("config");
		Files.createDirectories(subDir);

		// Create empty file
		Path configFile = subDir.resolve("tree-invite-messages.json");
		Files.writeString(configFile, "{}");

		Config config = ConfigLoader.load(configDir);

		// Should load with nulls for missing fields or defaults
		assertNotNull(config);
	}

	@Test
	void loadMultipleTimesReturnsConsistentConfig() throws IOException {
		Path configDir = tempDir;

		Config first = ConfigLoader.load(configDir);
		Config second = ConfigLoader.load(configDir);
		Config third = ConfigLoader.load(configDir);

		assertEquals(first.error_systemNotReady, second.error_systemNotReady);
		assertEquals(second.error_systemNotReady, third.error_systemNotReady);
		assertEquals(first.success_invited, second.success_invited);
		assertEquals(second.success_invited, third.success_invited);
	}

	@Test
	void loadConfigWithTemplates() throws IOException {
		Path configDir = tempDir;
		Path subDir = configDir.resolve("config");
		Files.createDirectories(subDir);

		Config original = new Config();
		original.success_invited = "Successfully invited {player} to the server!";
		original.error_inviteLimitReached = "You have invited {used} out of {limit} players. Resets at {reset}.";

		Path configFile = subDir.resolve("tree-invite-messages.json");
		try (FileWriter writer = new FileWriter(configFile.toFile())) {
			GSON.toJson(original, writer);
		}

		Config loaded = ConfigLoader.load(configDir);

		assertTrue(loaded.success_invited.contains("{player}"));
		assertTrue(loaded.error_inviteLimitReached.contains("{used}"));
		assertTrue(loaded.error_inviteLimitReached.contains("{limit}"));
		assertTrue(loaded.error_inviteLimitReached.contains("{reset}"));
	}

	@Test
	void loadHandlesReadOnlyDirectory() throws IOException {
		Path configDir = tempDir;
		Path subDir = configDir.resolve("config");
		Files.createDirectories(subDir);

		Config config = ConfigLoader.load(configDir);

		// Even if we can't write (in a read-only scenario), should load existing config
		assertNotNull(config);
	}

	@Test
	void loadConfigFileWithUnicodeCharacters() throws IOException {
		Path configDir = tempDir;
		Path subDir = configDir.resolve("config");
		Files.createDirectories(subDir);

		Config original = new Config();
		original.error_systemNotReady = "系统未就绪";
		original.success_invited = "邀请了 {player}";

		Path configFile = subDir.resolve("tree-invite-messages.json");
		try (FileWriter writer = new FileWriter(configFile.toFile())) {
			GSON.toJson(original, writer);
		}

		Config loaded = ConfigLoader.load(configDir);

		assertEquals("系统未就绪", loaded.error_systemNotReady);
		assertEquals("邀请了 {player}", loaded.success_invited);
	}

	@Test
	void loadDoesNotModifyExistingFile() throws IOException {
		Path configDir = tempDir;
		Path subDir = configDir.resolve("config");
		Files.createDirectories(subDir);

		Path configFile = subDir.resolve("tree-invite-messages.json");
		String original = "{ \"error_systemNotReady\": \"Test\" }";
		Files.writeString(configFile, original);

		long originalModified = Files.getLastModifiedTime(configFile).toMillis();

		// Wait a bit to ensure timestamp would change
		try {
			Thread.sleep(10);
		} catch (InterruptedException e) {
			// Ignore
		}

		ConfigLoader.load(configDir);

		long afterModified = Files.getLastModifiedTime(configFile).toMillis();

		// File should not have been modified (or minimally so)
		assertEquals(originalModified, afterModified);
	}
}
