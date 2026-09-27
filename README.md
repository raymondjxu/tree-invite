# Treevite

A server-side Minecraft Fabric mod that implements a tree-based whitelist system for managing player access through an invitation chain.

## Overview

Treevite allows you to control who can join your server through a hierarchical invitation system. Starting with just the console as the root administrator, whitelisted players can invite others, creating a "tree" of authorized players. This provides a flexible, organic way to manage server access without manually adding each player to a whitelist.

**Key Features:**
- Tree-based whitelist system starting from console as root
- Player-to-player invitation system with `/invite` command
- Invite revocation with automatic privilege adjustment
- Daily invitation rate limits per player (configurable)
- Support for both Java and Bedrock Edition players
- Invite history tracking
- Permission-based commands (via Fabric Permissions API)

## Requirements

- Minecraft Server 26.3 (or compatible)
- Fabric Loader 0.19.5 or later
- Java 25 or later
- Fabric API

## Installation

1. Download the `tree-invite.jar` mod file
2. Place it in your server's `mods` folder
3. Start your server
4. (Optional) Install [Fabric Permissions API](https://github.com/lucko/fabric-permissions-api) for permission-based command access

## Commands

### Player Commands

**`/invite <player> [java|bedrock]`**
- Invite a player to join the server
- If platform not specified, Java Edition is assumed
- Usernames are looked up via Mojang API (Java) or converted to XUID format (Bedrock)
- Limited by daily rate limit (default: 4 invites per day, resets at 02:00 AM server time)

**`/revokeinvite <player> [java|bedrock]`**
- Revoke an invitation you sent to a player
- Only the original inviter can revoke
- Platform optional; if not specified, both platforms are checked
- Players lose access if they have no alternate path back to console
- Invite data is preserved for automatic reinstatement if re-invited

**`/inviteinfo [player]`**
- View invitation information
- Lists who invited the player and who they've invited
- Defaults to the command executor if no player specified

### Admin Commands

**`/setmaxinvites <player> <count>`**
- Set a custom daily invitation limit for a player
- Overrides the global default rate limit
- Set to 0 to disable invites for a player

**`/setglobalmaxinvites <count>`**
- Set the default daily invitation limit for all players
- Only accessible to console or high-permission admins

## How It Works

1. **Initialization:** Only the console is whitelisted initially
2. **Invitations:** Any whitelisted player can invite others using `/invite`
3. **Access Control:** A player can join if they have at least one valid path back to console through the invitation chain
4. **Revocation:** When an invite is revoked, affected players lose access unless they have another path to console
5. **Reinstatement:** If a revoked player is invited again (directly or indirectly), they automatically regain access

### Example Scenario

```
Console (root)
├── Alice (invited by Console)
│   ├── Bob (invited by Alice)
│   └── Carol (invited by Alice)
└── Dave (invited by Console)
    └── Eve (invited by Dave)
```

- All players can join
- If Console revokes Dave's invite, Eve loses access (no alternate path)
- If Console revokes Alice's invite, Bob and Carol lose access
- If Console invites Dave back, Eve automatically regains access

## Configuration

Configuration is stored in `config/tree-invite-messages.json` and persists across restarts. The mod manages whitelist data automatically—you don't need to modify the vanilla whitelist. Treevite replaces the vanilla whitelist by setting it to false in server.properties.

## Permissions

When Fabric Permissions API is available, the following permissions are recognized:

- `tree-invite.invite` - Allow `/invite` command
- `tree-invite.revoke` - Allow `/revokeinvite` command
- `tree-invite.info` - Allow `/inviteinfo` command
- `tree-invite.admin.setmaxinvites` - Allow `/setmaxinvites` command
- `tree-invite.admin.setglobalmaxinvites` - Allow `/setglobalmaxinvites` command

## Database

Treevite uses SQLite for persistent storage by default, but can also use PostgreSQL. All invite relationships and rate limit data are stored automatically.

## Support

- **Issues:** Report bugs on the GitHub repository
- **Questions:** Check the documentation or ask in the repository discussions
- **Author:** AwesomestOwl

## License

GNU General Public License v3.0 - See LICENSE file for details
