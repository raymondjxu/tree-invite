package net.raymondjxu.treevite.data;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class InviteTreeTest {
	private static final UUID ALICE = UUID.fromString("00000000-0000-0000-0000-000000000001");
	private static final UUID BOB = UUID.fromString("00000000-0000-0000-0000-000000000002");
	private static final UUID CAROL = UUID.fromString("00000000-0000-0000-0000-000000000003");
	private static final UUID DAVE = UUID.fromString("00000000-0000-0000-0000-000000000004");

	@Test
	void inviteChainWhitelists() {
		InviteTree tree = new InviteTree();
		tree.getOrCreateNode(ALICE, "Alice", Platform.JAVA);
		tree.getOrCreateNode(BOB, "Bob", Platform.JAVA);

		tree.addInviteEdge(InviteTree.ROOT_ID, ALICE);
		tree.addInviteEdge(ALICE, BOB);

		assertTrue(tree.isWhitelisted(ALICE));
		assertTrue(tree.isWhitelisted(BOB));
	}

	@Test
	void pruneUnreachableRemovesCascade() {
		InviteTree tree = new InviteTree();
		tree.getOrCreateNode(ALICE, "Alice", Platform.JAVA);
		tree.getOrCreateNode(BOB, "Bob", Platform.JAVA);

		tree.addInviteEdge(InviteTree.ROOT_ID, ALICE);
		tree.addInviteEdge(ALICE, BOB);
		tree.removeInviteEdge(InviteTree.ROOT_ID, ALICE);

		Set<UUID> removed = tree.pruneUnreachable();
		assertTrue(removed.contains(ALICE));
		assertTrue(removed.contains(BOB));
		assertFalse(tree.hasNode(ALICE));
		assertFalse(tree.hasNode(BOB));
	}

	@Test
	void multiInviterKeepsAccess() {
		InviteTree tree = new InviteTree();
		tree.getOrCreateNode(ALICE, "Alice", Platform.JAVA);
		tree.getOrCreateNode(BOB, "Bob", Platform.JAVA);
		tree.getOrCreateNode(CAROL, "Carol", Platform.JAVA);

		tree.addInviteEdge(InviteTree.ROOT_ID, ALICE);
		tree.addInviteEdge(InviteTree.ROOT_ID, CAROL);
		tree.addInviteEdge(ALICE, BOB);
		tree.addInviteEdge(CAROL, BOB);

		tree.removeInviteEdge(ALICE, BOB);
		Set<UUID> removed = tree.pruneUnreachable();

		assertFalse(removed.contains(BOB));
		assertTrue(tree.isWhitelisted(BOB));
	}

	@Test
	void ratelimitResetsAtTwoAm() {
		InviteTree tree = new InviteTree();
		tree.setGlobalMaxInvites(1);
		tree.getOrCreateNode(DAVE, "Dave", Platform.JAVA);

		ZoneId zone = ZoneId.of("UTC");
		Instant beforeReset = ZonedDateTime.of(2024, 1, 2, 1, 0, 0, 0, zone).toInstant();
		Instant afterReset = ZonedDateTime.of(2024, 1, 2, 2, 0, 0, 0, zone).toInstant();

		InviteTree.InviteLimitResult first = tree.tryConsumeInvite(DAVE, beforeReset, zone);
		InviteTree.InviteLimitResult second = tree.tryConsumeInvite(DAVE, beforeReset.plusSeconds(60), zone);
		InviteTree.InviteLimitResult third = tree.tryConsumeInvite(DAVE, afterReset, zone);

		assertTrue(first.isAllowed());
		assertFalse(second.isAllowed());
		assertTrue(third.isAllowed());
	}

	@Test
	void findByDisplayNameIsCaseInsensitive() {
		InviteTree tree = new InviteTree();
		tree.getOrCreateNode(ALICE, "Notch", Platform.JAVA);

		assertEquals(1, tree.findByDisplayName("notch").size());
		assertEquals(1, tree.findByDisplayName("NOTCH").size());
	}
}
