package net.raymondjxu.treevite.data;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class InviteTree {
	public static final UUID ROOT_ID = new UUID(0L, 0L);

	private final Map<UUID, Node> nodes = new HashMap<>();
	private int globalMaxInvites = 4;

	public InviteTree() {
		ensureRoot();
	}

	public int getGlobalMaxInvites() {
		return globalMaxInvites;
	}

	public void setGlobalMaxInvites(int globalMaxInvites) {
		this.globalMaxInvites = Math.max(0, globalMaxInvites);
	}

	public Collection<Node> getNodes() {
		return nodes.values();
	}

	public Node getNode(UUID id) {
		return nodes.get(id);
	}

	public boolean hasNode(UUID id) {
		return nodes.containsKey(id);
	}

	public Node getOrCreateNode(UUID id, String displayName, Platform platform) {
		Node node = nodes.get(id);
		if (node == null) {
			node = new Node(id, displayName, platform == null ? Platform.UNKNOWN : platform);
			node.normalize();
			nodes.put(id, node);
			return node;
		}
		node.normalize();
		if (node.getDisplayName() == null && displayName != null && !displayName.isBlank()) {
			node.setDisplayName(displayName);
		}
		if (node.getPlatform() == Platform.UNKNOWN && platform != null && platform != Platform.UNKNOWN) {
			node.setPlatform(platform);
		}
		return node;
	}

	public boolean isWhitelisted(UUID id) {
		if (isRoot(id)) {
			return true;
		}
		if (!nodes.containsKey(id)) {
			return false;
		}
		return computeReachable().contains(id);
	}

	public boolean hasInviteEdge(UUID inviterId, UUID inviteeId) {
		Node inviter = nodes.get(inviterId);
		if (inviter == null) {
			return false;
		}
		return inviter.getInvitees().contains(inviteeId);
	}

	public boolean addInviteEdge(UUID inviterId, UUID inviteeId) {
		Node inviter = nodes.get(inviterId);
		Node invitee = nodes.get(inviteeId);
		if (inviter == null || invitee == null) {
			return false;
		}
		boolean added = inviter.getInvitees().add(inviteeId);
		if (added) {
			invitee.getInviters().add(inviterId);
		}
		return added;
	}

	public boolean removeInviteEdge(UUID inviterId, UUID inviteeId) {
		Node inviter = nodes.get(inviterId);
		Node invitee = nodes.get(inviteeId);
		if (inviter == null || invitee == null) {
			return false;
		}
		boolean removed = inviter.getInvitees().remove(inviteeId);
		if (removed) {
			invitee.getInviters().remove(inviterId);
		}
		return removed;
	}

	public Set<UUID> findUnreachable() {
		Set<UUID> reachable = computeReachable();
		Set<UUID> unreachable = new HashSet<>();

		for (UUID id : nodes.keySet()) {
			if (!isRoot(id) && !reachable.contains(id)) {
				unreachable.add(id);
			}
		}

		return unreachable;
	}

	public List<Node> findByDisplayName(String name) {
		if (name == null) {
			return List.of();
		}
		String target = name.toLowerCase(Locale.ROOT);
		List<Node> results = new ArrayList<>();
		for (Node node : nodes.values()) {
			String display = node.getDisplayName();
			if (display == null) {
				continue;
			}
			if (display.toLowerCase(Locale.ROOT).equals(target)) {
				results.add(node);
			}
		}
		return results;
	}

	public InviteLimitResult tryConsumeInvite(UUID inviterId, Instant now, ZoneId zone) {
		ZonedDateTime nextReset = nextResetTime(now, zone);
		if (isRoot(inviterId)) {
			return InviteLimitResult.allowed(Integer.MAX_VALUE, 0, nextReset);
		}
		Node node = nodes.get(inviterId);
		if (node == null) {
			return InviteLimitResult.denied(0, 0, nextReset);
		}
		LocalDate day = currentInviteDay(now, zone);
		if (node.getInviteDay() == null || !node.getInviteDay().equals(day)) {
			node.setInviteDay(day);
			node.setInvitesUsed(0);
		}
		int limit = getEffectiveMaxInvites(node);
		if (limit <= 0) {
			return InviteLimitResult.denied(limit, node.getInvitesUsed(), nextReset);
		}
		if (node.getInvitesUsed() >= limit) {
			return InviteLimitResult.denied(limit, node.getInvitesUsed(), nextReset);
		}
		node.setInvitesUsed(node.getInvitesUsed() + 1);
		return InviteLimitResult.allowed(limit, node.getInvitesUsed(), nextReset);
	}

	public int getEffectiveMaxInvites(Node node) {
		if (node == null) {
			return 0;
		}
		if (isRoot(node.getId())) {
			return Integer.MAX_VALUE;
		}
		Integer override = node.getMaxInvitesOverride();
		if (override != null) {
			return Math.max(0, override);
		}
		return globalMaxInvites;
	}

	public void ensureRoot() {
		Node root = nodes.get(ROOT_ID);
		if (root == null) {
			root = new Node(ROOT_ID, "(System Root)", Platform.SYSTEM);
			root.normalize();
			nodes.put(ROOT_ID, root);
			return;
		}
		root.normalize();
		root.setPlatform(Platform.SYSTEM);
		if (root.getDisplayName() == null || root.getDisplayName().isBlank()) {
			root.setDisplayName("(System Root)");
		}
	}

	public void addNode(Node node) {
		if (node == null || node.getId() == null) {
			return;
		}
		node.normalize();
		nodes.put(node.getId(), node);
	}

	public static boolean isRoot(UUID id) {
		return ROOT_ID.equals(id);
	}

	public static LocalDate currentInviteDay(Instant now, ZoneId zone) {
		return ZonedDateTime.ofInstant(now, zone).minusHours(2).toLocalDate();
	}

	public static ZonedDateTime nextResetTime(Instant now, ZoneId zone) {
		ZonedDateTime zoned = ZonedDateTime.ofInstant(now, zone);
		ZonedDateTime reset = zoned.withHour(2).withMinute(0).withSecond(0).withNano(0);
		if (!zoned.isBefore(reset)) {
			reset = reset.plusDays(1);
		}
		return reset;
	}

	private Set<UUID> computeReachable() {
		ensureRoot();
		Set<UUID> reachable = new HashSet<>();
		Deque<UUID> queue = new ArrayDeque<>();
		reachable.add(ROOT_ID);
		queue.add(ROOT_ID);

		while (!queue.isEmpty()) {
			UUID current = queue.removeFirst();
			Node node = nodes.get(current);
			if (node == null) {
				continue;
			}
			for (UUID invitee : node.getInvitees()) {
				if (reachable.add(invitee)) {
					queue.addLast(invitee);
				}
			}
		}

		return reachable;
	}

	public static final class InviteLimitResult {
		private final boolean allowed;
		private final int limit;
		private final int used;
		private final ZonedDateTime nextReset;

		private InviteLimitResult(boolean allowed, int limit, int used, ZonedDateTime nextReset) {
			this.allowed = allowed;
			this.limit = limit;
			this.used = used;
			this.nextReset = nextReset;
		}

		public static InviteLimitResult allowed(int limit, int used, ZonedDateTime nextReset) {
			return new InviteLimitResult(true, limit, used, nextReset);
		}

		public static InviteLimitResult denied(int limit, int used, ZonedDateTime nextReset) {
			return new InviteLimitResult(false, limit, used, nextReset);
		}

		public boolean isAllowed() {
			return allowed;
		}

		public int getLimit() {
			return limit;
		}

		public int getUsed() {
			return used;
		}

		public ZonedDateTime getNextReset() {
			return nextReset;
		}
	}
}
