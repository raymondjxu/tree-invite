package net.raymondjxu.treevite.data;

import net.minecraft.server.MinecraftServer;

import java.nio.file.Path;

public final class DatabaseConfig {
	private final String url;
	private final String user;
	private final String password;

	private DatabaseConfig(String url, String user, String password) {
		this.url = url;
		this.user = user;
		this.password = password;
	}

	public static DatabaseConfig load(MinecraftServer server) {
		String url = firstNonBlank(System.getProperty("treeinvite.db.url"), System.getenv("TREEINVITE_DB_URL"));
		String user = firstNonBlank(System.getProperty("treeinvite.db.user"), System.getenv("TREEINVITE_DB_USER"));
		String password = firstNonBlank(System.getProperty("treeinvite.db.password"), System.getenv("TREEINVITE_DB_PASSWORD"));

		if (url == null || url.isBlank()) {
			Path path = InviteTreeStore.defaultSqlitePath(server).toAbsolutePath();
			url = "jdbc:sqlite:" + path;
		}

		return new DatabaseConfig(url, user, password);
	}

	public String getUrl() {
		return url;
	}

	public String getUser() {
		return user;
	}

	public String getPassword() {
		return password;
	}

	private static String firstNonBlank(String first, String second) {
		if (first != null && !first.isBlank()) {
			return first;
		}
		if (second != null && !second.isBlank()) {
			return second;
		}
		return null;
	}
}
