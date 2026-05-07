package net.raymondjxu.treevite.data;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class InviteTreeStoreTest {
	private Connection connection;

	@BeforeEach
	void setUp() throws SQLException {
		// Create in-memory SQLite database
		connection = DriverManager.getConnection("jdbc:sqlite::memory:");
		connection.setAutoCommit(false);
	}

	@AfterEach
	void tearDown() throws SQLException {
		if (connection != null) {
			connection.close();
		}
	}

	@Test
	void loadNodesWithNullMaxOverride() throws SQLException {
		// Arrange: Create schema and insert node with NULL max_override
		setupSchema();
		UUID nodeId = UUID.fromString("00000000-0000-0000-0000-000000000001");
		insertNode(nodeId, "TestPlayer", "JAVA", null, null, 0);

		// Act: Load the tree
		InviteTree tree = new InviteTree();
		tree.ensureRoot();

		// This should not throw "Bad value for type Integer"
		try (PreparedStatement statement = connection.prepareStatement("SELECT uuid, display_name, platform, max_override, invite_day, invites_used FROM nodes")) {
			var result = statement.executeQuery();
			while (result.next()) {
				String uuidText = result.getString(1);
				UUID id = UUID.fromString(uuidText);
				String displayName = result.getString(2);
				String platformText = result.getString(3);
				Platform platform = Platform.valueOf(platformText);
				Node node = new Node(id, displayName, platform);
				
				Integer override = null;
				if (result.getObject(4) != null) {
					override = result.getInt(4);
				}
				node.setMaxInvitesOverride(override);
				
				String inviteDay = result.getString(5);
				if (inviteDay != null && !inviteDay.isBlank()) {
					node.setInviteDay(LocalDate.parse(inviteDay));
				}
				
				node.setInvitesUsed(result.getInt(6));
				node.normalize();
				tree.addNode(node);
			}
		}

		// Assert: Node was loaded correctly with NULL override
		assertTrue(tree.hasNode(nodeId));
		Node loaded = tree.getNode(nodeId);
		assertEquals("TestPlayer", loaded.getDisplayName());
		assertEquals(Platform.JAVA, loaded.getPlatform());
		assertNull(loaded.getMaxInvitesOverride());
		assertNull(loaded.getInviteDay());
		assertEquals(0, loaded.getInvitesUsed());
	}

	@Test
	void loadNodesWithNullInviteDay() throws SQLException {
		// Arrange: Create schema and insert node with NULL invite_day
		setupSchema();
		UUID nodeId = UUID.fromString("00000000-0000-0000-0000-000000000002");
		insertNode(nodeId, "AnotherPlayer", "BEDROCK", 5, null, 2);

		// Act & Assert: Load should not throw
		InviteTree tree = new InviteTree();
		tree.ensureRoot();

		try (PreparedStatement statement = connection.prepareStatement("SELECT uuid, display_name, platform, max_override, invite_day, invites_used FROM nodes")) {
			var result = statement.executeQuery();
			while (result.next()) {
				String uuidText = result.getString(1);
				UUID id = UUID.fromString(uuidText);
				String displayName = result.getString(2);
				String platformText = result.getString(3);
				Platform platform = Platform.valueOf(platformText);
				Node node = new Node(id, displayName, platform);
				
				Integer override = null;
				if (result.getObject(4) != null) {
					override = result.getInt(4);
				}
				node.setMaxInvitesOverride(override);
				
				String inviteDay = result.getString(5);
				if (inviteDay != null && !inviteDay.isBlank()) {
					node.setInviteDay(LocalDate.parse(inviteDay));
				}
				
				node.setInvitesUsed(result.getInt(6));
				node.normalize();
				tree.addNode(node);
			}
		}

		assertTrue(tree.hasNode(nodeId));
		Node loaded = tree.getNode(nodeId);
		assertEquals("AnotherPlayer", loaded.getDisplayName());
		assertEquals(5, loaded.getMaxInvitesOverride());
		assertNull(loaded.getInviteDay());
		assertEquals(2, loaded.getInvitesUsed());
	}

	@Test
	void loadNodesWithAllNullOptionalFields() throws SQLException {
		// Arrange: Create schema with multiple nodes with various NULL combinations
		setupSchema();
		UUID id1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
		UUID id2 = UUID.fromString("00000000-0000-0000-0000-000000000002");
		UUID id3 = UUID.fromString("00000000-0000-0000-0000-000000000003");

		insertNode(id1, "Player1", "JAVA", null, null, 0);
		insertNode(id2, "Player2", "BEDROCK", 10, "2026-05-07", 5);
		insertNode(id3, "Player3", "UNKNOWN", null, "2026-05-07", 1);

		// Act: Load all nodes
		InviteTree tree = new InviteTree();
		tree.ensureRoot();

		try (PreparedStatement statement = connection.prepareStatement("SELECT uuid, display_name, platform, max_override, invite_day, invites_used FROM nodes")) {
			var result = statement.executeQuery();
			while (result.next()) {
				String uuidText = result.getString(1);
				UUID id = UUID.fromString(uuidText);
				String displayName = result.getString(2);
				String platformText = result.getString(3);
				Platform platform = Platform.valueOf(platformText);
				Node node = new Node(id, displayName, platform);
				
				Integer override = null;
				if (result.getObject(4) != null) {
					override = result.getInt(4);
				}
				node.setMaxInvitesOverride(override);
				
				String inviteDay = result.getString(5);
				if (inviteDay != null && !inviteDay.isBlank()) {
					node.setInviteDay(LocalDate.parse(inviteDay));
				}
				
				node.setInvitesUsed(result.getInt(6));
				node.normalize();
				tree.addNode(node);
			}
		}

		// Assert: All nodes loaded correctly (including root node)
		assertEquals(4, tree.getNodes().size());
		
		Node node1 = tree.getNode(id1);
		assertNull(node1.getMaxInvitesOverride());
		assertNull(node1.getInviteDay());

		Node node2 = tree.getNode(id2);
		assertEquals(10, node2.getMaxInvitesOverride());
		assertEquals(LocalDate.parse("2026-05-07"), node2.getInviteDay());

		Node node3 = tree.getNode(id3);
		assertNull(node3.getMaxInvitesOverride());
		assertEquals(LocalDate.parse("2026-05-07"), node3.getInviteDay());
	}

	@Test
	void roundTripWithNullFieldsSaveAndLoad() throws SQLException {
		// Arrange: Create a tree with nodes with NULL fields
		setupSchema();
		UUID nodeId = UUID.fromString("00000000-0000-0000-0000-000000000001");
		
		InviteTree originalTree = new InviteTree();
		Node node = new Node(nodeId, "TestPlayer", Platform.JAVA);
		// Leave maxInvitesOverride and inviteDay as null
		node.setInvitesUsed(0);
		node.normalize();
		originalTree.addNode(node);

		// Act: Save and reload
		saveTree(originalTree);
		connection.commit();

		InviteTree loadedTree = new InviteTree();
		loadedTree.ensureRoot();

		try (PreparedStatement statement = connection.prepareStatement("SELECT uuid, display_name, platform, max_override, invite_day, invites_used FROM nodes")) {
			var result = statement.executeQuery();
			while (result.next()) {
				String uuidText = result.getString(1);
				UUID id = UUID.fromString(uuidText);
				String displayName = result.getString(2);
				String platformText = result.getString(3);
				Platform platform = Platform.valueOf(platformText);
				Node loadedNode = new Node(id, displayName, platform);
				
				Integer override = null;
				if (result.getObject(4) != null) {
					override = result.getInt(4);
				}
				loadedNode.setMaxInvitesOverride(override);
				
				String inviteDay = result.getString(5);
				if (inviteDay != null && !inviteDay.isBlank()) {
					loadedNode.setInviteDay(LocalDate.parse(inviteDay));
				}
				
				loadedNode.setInvitesUsed(result.getInt(6));
				loadedNode.normalize();
				loadedTree.addNode(loadedNode);
			}
		}

		// Assert: Data persisted correctly
		assertTrue(loadedTree.hasNode(nodeId));
		Node loaded = loadedTree.getNode(nodeId);
		assertEquals("TestPlayer", loaded.getDisplayName());
		assertEquals(Platform.JAVA, loaded.getPlatform());
		assertNull(loaded.getMaxInvitesOverride());
		assertNull(loaded.getInviteDay());
	}

	private void setupSchema() throws SQLException {
		try (var statement = connection.createStatement()) {
			statement.executeUpdate("CREATE TABLE IF NOT EXISTS settings (key TEXT PRIMARY KEY, value TEXT)");
			statement.executeUpdate("CREATE TABLE IF NOT EXISTS nodes (uuid TEXT PRIMARY KEY, display_name TEXT, platform TEXT, max_override INTEGER, invite_day TEXT, invites_used INTEGER)");
			statement.executeUpdate("CREATE TABLE IF NOT EXISTS edges (inviter_uuid TEXT NOT NULL, invitee_uuid TEXT NOT NULL, PRIMARY KEY (inviter_uuid, invitee_uuid))");
		}
	}

	private void insertNode(UUID uuid, String displayName, String platform, Integer maxOverride, String inviteDay, int invitesUsed) throws SQLException {
		String sql = "INSERT INTO nodes (uuid, display_name, platform, max_override, invite_day, invites_used) VALUES (?, ?, ?, ?, ?, ?)";
		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, uuid.toString());
			statement.setString(2, displayName);
			statement.setString(3, platform);
			if (maxOverride == null) {
				statement.setNull(4, java.sql.Types.INTEGER);
			} else {
				statement.setInt(4, maxOverride);
			}
			if (inviteDay == null) {
				statement.setNull(5, java.sql.Types.VARCHAR);
			} else {
				statement.setString(5, inviteDay);
			}
			statement.setInt(6, invitesUsed);
			statement.executeUpdate();
		}
	}

	private void saveTree(InviteTree tree) throws SQLException {
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
}
