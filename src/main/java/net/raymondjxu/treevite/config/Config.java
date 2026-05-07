package net.raymondjxu.treevite.config;

/**
 * Configuration class for player-visible messages.
 * All fields are JSON-serializable and support template placeholders like {player}, {count}, etc.
 */
public class Config {
	// Error messages
	public String error_systemNotReady = "Invite system is not ready yet";
	public String error_notWhitelisted = "You are not whitelisted! Ask a friend to run /invite {source}";
	public String error_cannotSpecifyPlatform = "Cannot specify platform given raw UUID";
	public String error_lookupFailed = "Lookup failed: {message}";
	public String error_cannotInviteSelf = "You cannot invite yourself";
	public String error_inviteLimitReached = "Invite limit reached ({used}/{limit}). Resets at {reset}";
	public String error_addInviteFailed = "Failed to add invite";
	public String error_playerNotInTree = "Player is not in the invite tree";
	public String error_platformAmbiguous = "Player exists on both platforms; specify java or bedrock";
	public String error_nameAmbiguous = "Player name is ambiguous; specify java or bedrock";
	public String error_didNotInvite = "You did not invite that player";
	public String error_revokeInviteFailed = "Failed to revoke invite";
	public String error_noInviteInfo = "No invite info available";
	public String error_permissionsApiMissing = "Permissions API not present; using operator-level checks. Install fabric-permissions-api to enable permission nodes.";

	// Success messages
	public String success_invited = "Invited {player}";
	public String success_alreadyInvited = "That player is already invited by you";
	public String success_revokedInvite = "Revoked invite for {player}";
	public String success_removedUnreachable = "Removed {count} unreachable {entries}";
	public String success_setMaxInvites = "Set max invites for {player} to {count}";
	public String success_setGlobalMaxInvites = "Set global max invites to {count}";
	public String success_inviteInfoHeader = "Invite info for {player}";
	public String success_invitedBy = "Invited by: {list}";
	public String success_invitedList = "Invited: {list}";

	// Disconnect messages
	public String disconnect_noLongerWhitelisted = "You are no longer whitelisted";
}
