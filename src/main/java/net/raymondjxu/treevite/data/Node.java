package net.raymondjxu.treevite.data;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class Node {
	private UUID id;
	private String displayName;
	private Platform platform;
	private Set<UUID> inviters;
	private Set<UUID> invitees;
	private Integer maxInvitesOverride;
	private LocalDate inviteDay;
	private int invitesUsed;

	public Node() {
		// Gson constructor
	}

	public Node(UUID id, String displayName, Platform platform) {
		this.id = id;
		this.displayName = displayName;
		this.platform = platform;
		this.inviters = new HashSet<>();
		this.invitees = new HashSet<>();
	}

	public void normalize() {
		if (platform == null) {
			platform = Platform.UNKNOWN;
		}
		if (inviters == null) {
			inviters = new HashSet<>();
		}
		if (invitees == null) {
			invitees = new HashSet<>();
		}
	}

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public Platform getPlatform() {
		return platform;
	}

	public void setPlatform(Platform platform) {
		this.platform = platform;
	}

	public Set<UUID> getInviters() {
		return inviters;
	}

	public Set<UUID> getInvitees() {
		return invitees;
	}

	public Integer getMaxInvitesOverride() {
		return maxInvitesOverride;
	}

	public void setMaxInvitesOverride(Integer maxInvitesOverride) {
		this.maxInvitesOverride = maxInvitesOverride;
	}

	public LocalDate getInviteDay() {
		return inviteDay;
	}

	public void setInviteDay(LocalDate inviteDay) {
		this.inviteDay = inviteDay;
	}

	public int getInvitesUsed() {
		return invitesUsed;
	}

	public void setInvitesUsed(int invitesUsed) {
		this.invitesUsed = invitesUsed;
	}
}
