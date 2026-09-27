# AGENTS.md - Tree Invite Agent Guidance

Guidance for AI agents working on the Tree Invite Minecraft mod project.

## Project Context

**Tree Invite** is a Minecraft Fabric server mod implementing a tree-based whitelist system where console is the root, and whitelisted players can invite others. All code is Java with Gradle/Fabric Loom build system.

**Key Stats:**
- Version: 1.0.0
- Minecraft: 26.3
- Java: 25+
- Databases: SQLite (default) or PostgreSQL

## Architecture Overview

```
src/main/java/net/raymondjxu/treevite/
├── TreeInvite.java          # Entry point & command registration
├── command/                 # Command handlers (/invite, /revoke, /info, /admin)
├── config/                  # Configuration & message loading
├── data/                    # Whitelist logic & database persistence
└── lookup/                  # Player UUID/XUID resolution
```

**Core Concept:** Players can only join if they have ≥1 valid path through the invite tree back to console. Revoking an invite removes access unless an alternate path exists.

## Build & Test Workflow

**Build mod:** `./gradlew build`
**Run tests:** `./gradlew test` or `./gradlew test --tests "package.ClassName"`
**Dev server:** `./gradlew runServer` (loads mod, use `run/server.properties`)

Test classes are XML reports in `build/test-results/test/` after running tests.

## Common Tasks

### Adding a Command
1. Create class in `src/main/java/.../command/`
2. Register in `TreeInvite.java`
3. Add unit tests in `src/test/java/.../command/`
4. Follow `ServerCommandSource` feedback pattern (see existing commands)

### Modifying Whitelist/Invite Logic
- Core: `src/main/java/.../data/InviteTree.java`
- Persistence: `src/main/java/.../data/` database layer
- Ensure both SQLite and PostgreSQL work

### Permission Integration
- Fabric Permissions API v0.7.0 is optional (suggested, not required)
- Permissions: `tree-invite.{invite,revoke,info,admin.*}`
- Commands work unrestricted if API unavailable

## Key Implementation Details

- **Rate Limits:** Daily counter resets 02:00 AM server time; configurable per-player
- **Platform Support:** Both Java Edition (UUID) and Bedrock (XUID: `00000000-0000-0000-xxxx-xxxxxxxxxxxx`)
- **Lookups:** Java names → Mojang API; Bedrock names → XUID format
- **Data Persistence:** SQLite or PostgreSQL; auto-managed by mod
- **Configuration:** `run/config/tree-invite-messages.json`

## Dependencies
- fabric-api, fabric-loader, fabric-permissions-api (optional)
- sqlite-jdbc, postgresql (database drivers)
- junit-jupiter (testing)

## Known Issues & Tips

- **SQLite Loading:** If runtime fails, check `/memories/repo/sqlite-loading-fix.md`
- **Debugging:** Run `./gradlew runServer --debug-jvm` for breakpoint debugging
- **Logs:** Check `run/logs/latest.log` for server output
- **Testing:** Most logic is in `InviteTree` and database layer; prioritize testing there

## Conventions for Agents

1. **Always test changes:** Run full suite with `./gradlew test`
2. **Both databases:** Ensure SQLite and PostgreSQL work (separate drivers included)
3. **Bedrock support:** Don't assume UUID-only; handle XUID format
4. **Feedback:** Use `ServerCommandSource.sendFeedback()` for user messages
5. **Comments:** Reference spec.md for behavioral requirements
6. **Multi-path logic:** Verify whitelist correctly handles alternate invite paths

## Useful References

- Spec: [spec.md](spec.md) - Complete feature specification
- User Guide: [README.md](README.md) - End-user documentation
- Gradle: [build.gradle](build.gradle) - Dependencies & build config
- Properties: [gradle.properties](gradle.properties) - Versions & paths
