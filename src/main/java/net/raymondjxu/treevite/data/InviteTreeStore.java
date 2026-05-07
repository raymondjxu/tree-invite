package net.raymondjxu.treevite.data;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.raymondjxu.treevite.TreeInvite;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.UUID;

public final class InviteTreeStore implements AutoCloseable {
	private final Connection connection;

	private InviteTreeStore(Connection connection) {
		this.connection = connection;
	}

	public static InviteTreeStore open(MinecraftServer server) throws SQLException {
		DatabaseConfig config = DatabaseConfig.load(server);
		Connection connection;
		if (config.getUser() == null || config.getUser().isBlank()) {
			connection = DriverManager.getConnection(config.getUrl());
		} else {
			connection = DriverManager.getConnection(config.getUrl(), config.getUser(), config.getPassword());
		}
		connection.setAutoCommit(false);
		InviteTreeStore store = new InviteTreeStore(connection);
		store.ensureSchema();
		return store;
	}

	public InviteTree load() {
		InviteTree tree = new InviteTree();
		try {
			ensureSchema();
			loadSettings(tree);
			loadNodes(tree);
			loadEdges(tree);
			connection.commit();
		} catch (SQLException ex) {
			TreeInvite.LOGGER.error("Failed to load invite tree from SQL", ex);
			rollbackQuietly();
		}
		tree.ensureRoot();
		return tree;
	}

	public void save(InviteTree tree) {
		try {
			tree.ensureRoot();
			ensureSchema();
			try (Statement statement = connection.createStatement()) {
				statement.executeUpdate("DELETE FROM settings");
				statement.executeUpdate("DELETE FROM nodes");
				statement.executeUpdate("DELETE FROM edges");
			}

			saveSettings(tree);
			saveNodes(tree);
			saveEdges(tree);
			connection.commit();
		} catch (SQLException ex) {
			TreeInvite.LOGGER.error("Failed to save invite tree to SQL", ex);
			rollbackQuietly();
		}
	}

	@Override
	public void close() {
		try {
			connection.close();
		} catch (SQLException ex) {
			TreeInvite.LOGGER.error("Failed to close invite tree database", ex);
		}
	}

	private void ensureSchema() throws SQLException {
		try (Statement statement = connection.createStatement()) {
			statement.executeUpdate("CREATE TABLE IF NOT EXISTS settings (key TEXT PRIMARY KEY, value TEXT)");
			statement.executeUpdate("CREATE TABLE IF NOT EXISTS nodes (uuid TEXT PRIMARY KEY, display_name TEXT, platform TEXT, max_override INTEGER, invite_day TEXT, invites_used INTEGER)");
			statement.executeUpdate("CREATE TABLE IF NOT EXISTS edges (inviter_uuid TEXT NOT NULL, invitee_uuid TEXT NOT NULL, PRIMARY KEY (inviter_uuid, invitee_uuid))");
			statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_edges_inviter ON edges(inviter_uuid)");
			statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_edges_invitee ON edges(invitee_uuid)");
		}
	}

	private void loadSettings(InviteTree tree) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement("SELECT value FROM settings WHERE key = ?")) {
			statement.setString(1, "globalMaxInvites");
			try (ResultSet result = statement.executeQuery()) {
				if (result.next()) {
					String value = result.getString(1);
					if (value != null && !value.isBlank()) {
						try {
							tree.setGlobalMaxInvites(Integer.parseInt(value));
						} catch (NumberFormatException ignored) {
							// Keep default
						}
					}
				}
			}
		}
	}

	private void loadNodes(InviteTree tree) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement("SELECT uuid, display_name, platform, max_override, invite_day, invites_used FROM nodes")) {
			try (ResultSet result = statement.executeQuery()) {
				while (result.next()) {
					String uuidText = result.getString(1);
					if (uuidText == null || uuidText.isBlank()) {
						continue;
					}
					UUID id;
					try {
						id = UUID.fromString(uuidText);
					} catch (IllegalArgumentException ex) {
						continue;
					}
					String displayName = result.getString(2);
					String platformText = result.getString(3);
					Platform platform = parsePlatform(platformText);
					Node node = new Node(id, displayName, platform);
					Integer override = null;
					if (result.getObject(4) != null) {
						override = result.getInt(4);
					}
					node.setMaxInvitesOverride(override);
					String inviteDay = result.getString(5);
					if (inviteDay != null && !inviteDay.isBlank()) {
						try {
							node.setInviteDay(LocalDate.parse(inviteDay));
						} catch (Exception ignored) {
							// Keep null
						}
					}
					node.setInvitesUsed(result.getInt(6));
					node.normalize();
					tree.addNode(node);
				}
			}
		}
	}

	private void loadEdges(InviteTree tree) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement("SELECT inviter_uuid, invitee_uuid FROM edges")) {
			try (ResultSet result = statement.executeQuery()) {
				while (result.next()) {
					UUID inviterId = parseUuid(result.getString(1));
					UUID inviteeId = parseUuid(result.getString(2));
					if (inviterId == null || inviteeId == null) {
						continue;
					}
					tree.getOrCreateNode(inviterId, null, Platform.UNKNOWN);
					tree.getOrCreateNode(inviteeId, null, Platform.UNKNOWN);
					tree.addInviteEdge(inviterId, inviteeId);
				}
			}
		}
	}

	private void saveSettings(InviteTree tree) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement("INSERT INTO settings (key, value) VALUES (?, ?)") ) {
			statement.setString(1, "globalMaxInvites");
			statement.setString(2, String.valueOf(tree.getGlobalMaxInvites()));
			statement.executeUpdate();
		}
	}

	private void saveNodes(InviteTree tree) throws SQLException {
		String sql = "INSERT INTO nodes (uuid, display_name, platform, max_override, invite_day, invites_used) VALUES (?, ?, ?, ?, ?, ?)";
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			for (Node node : tree.getNodes()) {
				statement.setString(1, node.getId().toString());
				statement.setString(2, node.getDisplayName());
				statement.setString(3, node.getPlatform() == null ? Platform.UNKNOWN.name() : node.getPlatform().name());
				if (node.getMaxInvitesOverride() == null) {
					statement.setNull(4, java.sql.Types.INTEGER);
				} else {
					statement.setInt(4, node.getMaxInvitesOverride());
				}
				if (node.getInviteDay() == null) {
					statement.setNull(5, java.sql.Types.VARCHAR);
				} else {
					statement.setString(5, node.getInviteDay().toString());
				}
				statement.setInt(6, node.getInvitesUsed());
				statement.addBatch();
			}
			statement.executeBatch();
		}
	}

	private void saveEdges(InviteTree tree) throws SQLException {
		String sql = "INSERT INTO edges (inviter_uuid, invitee_uuid) VALUES (?, ?)";
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			for (Node node : tree.getNodes()) {
				for (UUID inviteeId : node.getInvitees()) {
					statement.setString(1, node.getId().toString());
					statement.setString(2, inviteeId.toString());
					statement.addBatch();
				}
			}
			statement.executeBatch();
		}
	}

	private void rollbackQuietly() {
		try {
			connection.rollback();
		} catch (SQLException ex) {
			TreeInvite.LOGGER.error("Failed to rollback invite tree database", ex);
		}
	}

	private static Platform parsePlatform(String platformText) {
		if (platformText == null || platformText.isBlank()) {
			return Platform.UNKNOWN;
		}
		try {
			return Platform.valueOf(platformText.trim().toUpperCase());
		} catch (IllegalArgumentException ex) {
			return Platform.UNKNOWN;
		}
	}

	private static UUID parseUuid(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		try {
			return UUID.fromString(value);
		} catch (IllegalArgumentException ex) {
			return null;
		}
	}

	public static Path defaultSqlitePath(MinecraftServer server) {
		return server.getWorldPath(LevelResource.ROOT).resolve("tree-invite.db");
	}
}
