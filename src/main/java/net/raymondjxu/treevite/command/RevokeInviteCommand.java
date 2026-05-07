package net.raymondjxu.treevite.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.raymondjxu.treevite.TreeInvite;
import net.raymondjxu.treevite.config.Config;
import net.raymondjxu.treevite.config.Messages;
import net.raymondjxu.treevite.data.InviteTree;
import net.raymondjxu.treevite.data.Node;
import net.raymondjxu.treevite.data.Platform;
import net.raymondjxu.treevite.lookup.ProfileLookup;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class RevokeInviteCommand {
	private RevokeInviteCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("revokeinvite")
				.then(Commands.argument("player", StringArgumentType.word())
						.executes(context -> execute(context, null))
						.then(Commands.literal("java")
								.executes(context -> execute(context, Platform.JAVA)))
						.then(Commands.literal("bedrock")
								.executes(context -> execute(context, Platform.BEDROCK)))));
	}

	private static int execute(CommandContext<CommandSourceStack> context, Platform platformArg) {
		CommandSourceStack source = context.getSource();
		InviteTree tree = TreeInvite.getTree();
		if (tree == null) {
			Config config = TreeInvite.getConfig();
			source.sendFailure(Component.literal(config != null ? config.error_systemNotReady : "Invite system is not ready yet"));
			return 0;
		}
		String input = StringArgumentType.getString(context, "player");
		UUID inviterId = TreeInvite.getSourceUuid(source);
		if (!tree.isWhitelisted(inviterId)) {
			Config config = TreeInvite.getConfig();
			source.sendFailure(Component.literal(config != null ? config.error_notWhitelisted.replace("{source}", source.getTextName()) : "Missing configuration message: error_notWhitelisted"));
			return 0;
		}

		UUID direct = parseUuid(input);
		if (direct != null) {
			if (platformArg != null) {
				Config config = TreeInvite.getConfig();
				source.sendFailure(Component.literal(config != null ? config.error_cannotSpecifyPlatform : "Cannot specify platform given raw UUID"));
				return 0;
			}
			revokeResolved(source, tree, inviterId, direct);
			return 1;
		}

		if (platformArg != null) {
			resolveWithPlatform(source, tree, inviterId, input, platformArg);
			return 1;
		}

		resolveWithoutPlatform(source, tree, inviterId, input);
		return 1;
	}

	private static void resolveWithPlatform(CommandSourceStack source, InviteTree tree, UUID inviterId, String name, Platform platform) {
		List<Node> matches = findByNameAndPlatform(tree, name, platform);
		if (!matches.isEmpty()) {
			revokeResolved(source, tree, inviterId, matches.get(0).getId());
			return;
		}

		MinecraftServer server = source.getServer();
		ProfileLookup.lookup(name, platform).thenAccept(result -> server.execute(() -> {
			if (!result.isSuccess()) {
				source.sendFailure(Component.literal(result.getError()));
				return;
			}
			if (!tree.hasNode(result.getUuid())) {
				Config config = TreeInvite.getConfig();
				source.sendFailure(Component.literal(config != null ? config.error_playerNotInTree : "Player is not in the invite tree"));
				return;
			}
			revokeResolved(source, tree, inviterId, result.getUuid());
		})).exceptionally(ex -> {
			server.execute(() -> {
				Config config = TreeInvite.getConfig();
				String message = config != null ? Messages.format(config.error_lookupFailed, "message", ex.getMessage()) : "Lookup failed: " + ex.getMessage();
				source.sendFailure(Component.literal(message));
			});
			return null;
		});
	}

	private static void resolveWithoutPlatform(CommandSourceStack source, InviteTree tree, UUID inviterId, String name) {
		List<Node> matches = tree.findByDisplayName(name);
		Node javaNode = firstMatch(matches, Platform.JAVA);
		Node bedrockNode = firstMatch(matches, Platform.BEDROCK);

		if (javaNode != null && bedrockNode != null) {
			Config config = TreeInvite.getConfig();
			source.sendFailure(Component.literal(config != null ? config.error_platformAmbiguous : "Player exists on both platforms; specify java or bedrock"));
			return;
		}
		if (javaNode != null) {
			revokeResolved(source, tree, inviterId, javaNode.getId());
			return;
		}
		if (bedrockNode != null) {
			revokeResolved(source, tree, inviterId, bedrockNode.getId());
			return;
		}
		if (matches.size() > 1) {
			Config config = TreeInvite.getConfig();
			source.sendFailure(Component.literal(config != null ? config.error_nameAmbiguous : "Player name is ambiguous; specify java or bedrock"));
			return;
		}

		MinecraftServer server = source.getServer();
		CompletableFuture<ProfileLookup.LookupResult> javaFuture = ProfileLookup.lookupJava(name);
		CompletableFuture<ProfileLookup.LookupResult> bedrockFuture = ProfileLookup.lookupBedrock(name);
		CompletableFuture.allOf(javaFuture, bedrockFuture).thenAccept(ignored -> server.execute(() -> {
			ProfileLookup.LookupResult javaResult = safeJoin(javaFuture);
			ProfileLookup.LookupResult bedrockResult = safeJoin(bedrockFuture);

			List<UUID> candidates = new ArrayList<>();
			if (javaResult != null && javaResult.isSuccess() && tree.hasNode(javaResult.getUuid())) {
				candidates.add(javaResult.getUuid());
			}
			if (bedrockResult != null && bedrockResult.isSuccess() && tree.hasNode(bedrockResult.getUuid())) {
				candidates.add(bedrockResult.getUuid());
			}

			if (candidates.size() > 1) {
				Config config = TreeInvite.getConfig();
				source.sendFailure(Component.literal(config != null ? config.error_platformAmbiguous : "Player exists on both platforms; specify java or bedrock"));
				return;
			}
			if (candidates.size() == 1) {
				revokeResolved(source, tree, inviterId, candidates.get(0));
				return;
			}

			Config config = TreeInvite.getConfig();
			source.sendFailure(Component.literal(config != null ? config.error_playerNotInTree : "Player is not in the invite tree"));
		})).exceptionally(ex -> {
			server.execute(() -> {
				Config config = TreeInvite.getConfig();
				String message = config != null ? Messages.format(config.error_lookupFailed, "message", ex.getMessage()) : "Lookup failed: " + ex.getMessage();
				source.sendFailure(Component.literal(message));
			});
			return null;
		});
	}

	private static void revokeResolved(CommandSourceStack source, InviteTree tree, UUID inviterId, UUID inviteeId) {
		if (!tree.hasInviteEdge(inviterId, inviteeId)) {
			Config config = TreeInvite.getConfig();
			source.sendFailure(Component.literal(config != null ? config.error_didNotInvite : "You did not invite that player"));
			return;
		}
		boolean removed = tree.removeInviteEdge(inviterId, inviteeId);
		if (!removed) {
			Config config = TreeInvite.getConfig();
			source.sendFailure(Component.literal(config != null ? config.error_revokeInviteFailed : "Failed to revoke invite"));
			return;
		}

		Set<UUID> removedIds = tree.pruneUnreachable();
		kickRemovedPlayers(source.getServer(), removedIds);
		TreeInvite.saveTree(source.getServer());

		String name = describePlayer(tree.getNode(inviteeId));
		Config config = TreeInvite.getConfig();
		String message = config != null ? Messages.format(config.success_revokedInvite, "player", name) : ("Revoked invite for " + name);
		source.sendSuccess(() -> Component.literal(message), false);
		if (!removedIds.isEmpty()) {
			String removeMessage = config != null ? Messages.format(config.success_removedUnreachable, "count", removedIds.size(), "entries", (removedIds.size() == 1 ? "y" : "ies")) : ("Removed " + removedIds.size() + " unreachable entr" + (removedIds.size() == 1 ? "y" : "ies"));
			source.sendSuccess(() -> Component.literal(removeMessage), false);
		}
	}

	private static void kickRemovedPlayers(MinecraftServer server, Set<UUID> removed) {
		for (UUID id : removed) {
			ServerPlayer player = server.getPlayerList().getPlayer(id);
			if (player != null) {
				Config config = TreeInvite.getConfig();
				String message = config != null ? config.disconnect_noLongerWhitelisted : "You are no longer whitelisted";
				player.connection.disconnect(Component.literal(message));
			}
		}
	}

	private static Node firstMatch(List<Node> matches, Platform platform) {
		if (matches == null || matches.isEmpty()) {
			return null;
		}
		for (Node node : matches) {
			if (node.getPlatform() == platform) {
				return node;
			}
		}
		return null;
	}

	private static UUID parseUuid(String value) {
		try {
			return UUID.fromString(value);
		} catch (Exception ex) {
			return null;
		}
	}

	private static List<Node> findByNameAndPlatform(InviteTree tree, String name, Platform platform) {
		List<Node> results = new ArrayList<>();
		for (Node node : tree.findByDisplayName(name)) {
			if (node.getPlatform() == platform) {
				results.add(node);
			}
		}
		return results;
	}

	private static String describePlayer(Node node) {
		if (node == null) {
			return "<unknown>";
		}
		String name = node.getDisplayName();
		String platform = node.getPlatform() == null ? Platform.UNKNOWN.name() : node.getPlatform().name();
		if (name == null || name.isBlank()) {
			return node.getId() + " (" + platform.toLowerCase(Locale.ROOT) + ")";
		}
		return name + " (" + platform.toLowerCase(Locale.ROOT) + ")";
	}

	private static ProfileLookup.LookupResult safeJoin(CompletableFuture<ProfileLookup.LookupResult> future) {
		try {
			return future.join();
		} catch (Exception ex) {
			return null;
		}
	}
}
