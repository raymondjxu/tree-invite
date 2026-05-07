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
import java.util.HashSet;
import java.util.List;
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
		dispatcher.register(Commands.literal("revokeallinvites")
				.requires(source -> Permissions.check(source, "treeinvite.admin", 3))
				.then(Commands.argument("player", StringArgumentType.word())
						.executes(RevokeInviteCommand::executeRevokeAll)));
	}

	private static int execute(CommandContext<CommandSourceStack> context, Platform platformArg) {
		CommandSourceStack source = context.getSource();
		InviteTree tree = TreeInvite.getTree();
		if (tree == null) {
			Config config = TreeInvite.getConfig();
			source.sendFailure(Component.literal(config.error_systemNotReady));
			return 0;
		}
		String input = StringArgumentType.getString(context, "player");
		UUID callerId = TreeInvite.getSourceUuid(source);
		if (!tree.isWhitelisted(callerId)) {
			Config config = TreeInvite.getConfig();
			source.sendFailure(Component.literal(config.error_notWhitelisted.replace("{source}", source.getTextName())));
			return 0;
		}

		UUID direct = parseUuid(input);
		if (direct != null) {
			if (platformArg != null) {
				Config config = TreeInvite.getConfig();
				source.sendFailure(Component.literal(config.error_cannotSpecifyPlatform));
				return 0;
			}
			revokeResolved(source, tree, callerId, direct);
			return 1;
		}

		if (platformArg != null) {
			resolveWithPlatform(source, tree, callerId, input, platformArg);
			return 1;
		}

		resolveWithoutPlatform(source, tree, callerId, input);
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
				source.sendFailure(Component.literal(config.error_playerNotInTree));
				return;
			}
			revokeResolved(source, tree, inviterId, result.getUuid());
		})).exceptionally(ex -> {
			server.execute(() -> {
				Config config = TreeInvite.getConfig();
				String message = Messages.format(config.error_lookupFailed, "message", ex.getMessage()) + ex.getMessage();
				source.sendFailure(Component.literal(message));
			});
			return null;
		});
	}

	private static void resolveWithoutPlatform(CommandSourceStack source, InviteTree tree, UUID inviterId, String name) {
		resolveName(source, tree, name, (source2, tree2, uuid) -> revokeResolved(source2, tree2, inviterId, uuid));
	}

	@FunctionalInterface
	private interface ResolvedNameCallback {
		void onResolved(CommandSourceStack source, InviteTree tree, UUID uuid);
	}

	private static void resolveName(CommandSourceStack source, InviteTree tree, String input, ResolvedNameCallback callback) {
		UUID direct = parseUuid(input);
		if (direct != null) {
			callback.onResolved(source, tree, direct);
			return;
		}

		List<Node> matches = tree.findByDisplayName(input);
		Node javaNode = firstMatch(matches, Platform.JAVA);
		Node bedrockNode = firstMatch(matches, Platform.BEDROCK);

		if (javaNode != null && bedrockNode != null) {
			Config config = TreeInvite.getConfig();
			source.sendFailure(Component.literal(config.error_platformAmbiguous));
			return;
		}
		if (javaNode != null) {
			callback.onResolved(source, tree, javaNode.getId());
			return;
		}
		if (bedrockNode != null) {
			callback.onResolved(source, tree, bedrockNode.getId());
			return;
		}
		if (matches.size() > 1) {
			Config config = TreeInvite.getConfig();
			source.sendFailure(Component.literal(config.error_nameAmbiguous));
			return;
		}

		MinecraftServer server = source.getServer();
		CompletableFuture<ProfileLookup.LookupResult> javaFuture = ProfileLookup.lookupJava(input);
		CompletableFuture<ProfileLookup.LookupResult> bedrockFuture = ProfileLookup.lookupBedrock(input);
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
				source.sendFailure(Component.literal(config.error_platformAmbiguous));
				return;
			}
			if (candidates.size() == 1) {
				callback.onResolved(source, tree, candidates.get(0));
				return;
			}

			Config config = TreeInvite.getConfig();
			source.sendFailure(Component.literal(config.error_playerNotInTree));
		})).exceptionally(ex -> {
			server.execute(() -> {
				Config config = TreeInvite.getConfig();
				String message = Messages.format(config.error_lookupFailed, "message", ex.getMessage()) + ex.getMessage();
				source.sendFailure(Component.literal(message));
			});
			return null;
		});
	}

	private static void revokeResolved(CommandSourceStack source, InviteTree tree, UUID callerId, UUID inviteeId) {
		if (!tree.hasInviteEdge(callerId, inviteeId)) {
			Config config = TreeInvite.getConfig();
			source.sendFailure(Component.literal(config.error_didNotInvite));
			return;
		}
		boolean removed = tree.removeInviteEdge(callerId, inviteeId);
		if (!removed) {
			Config config = TreeInvite.getConfig();
			source.sendFailure(Component.literal(config.error_revokeInviteFailed));
			return;
		}

		Set<UUID> unreachableIds = tree.findUnreachable();
		kickRemovedPlayers(source.getServer(), unreachableIds);
		TreeInvite.saveTree(source.getServer());

		Config config = TreeInvite.getConfig();
		Component message = Messages.formatComponent(config.success_revokedInvite, "player", Messages.playerComponent(tree.getNode(inviteeId)));
		source.sendSuccess(() -> message, false);
		if (!unreachableIds.isEmpty()) {
			String entries = unreachableIds.size() == 1 ? "entry" : "entries";
			Component removeMessage = Messages.formatComponent(config.info_totalUnreachable, "count", Component.literal(String.valueOf(unreachableIds.size())), "entries", Component.literal(entries));
			source.sendSuccess(() -> removeMessage, false);
		}
	}

	private static void kickRemovedPlayers(MinecraftServer server, Set<UUID> removed) {
		for (UUID id : removed) {
			ServerPlayer player = server.getPlayerList().getPlayer(id);
			if (player != null) {
				Config config = TreeInvite.getConfig();
				String message = config.disconnect_noLongerWhitelisted;
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

	private static ProfileLookup.LookupResult safeJoin(CompletableFuture<ProfileLookup.LookupResult> future) {
		try {
			return future.join();
		} catch (Exception ex) {
			return null;
		}
	}

	private static int executeRevokeAll(CommandContext<CommandSourceStack> context) {
		CommandSourceStack source = context.getSource();
		InviteTree tree = TreeInvite.getTree();
		if (tree == null) {
			Config config = TreeInvite.getConfig();
			source.sendFailure(Component.literal(config.error_systemNotReady));
			return 0;
		}

		String input = StringArgumentType.getString(context, "player");
		resolveName(source, tree, input, (source2, tree2, uuid) -> revokeAllInvitesResolved(source2, tree2, uuid));
		return 1;
	}

	private static void revokeAllInvitesResolved(CommandSourceStack source, InviteTree tree, UUID playerId) {
		Node node = tree.getNode(playerId);
		if (node == null) {
			Config config = TreeInvite.getConfig();
			source.sendFailure(Component.literal(config.error_playerNotInTree));
			return;
		}

		Set<UUID> inviters = new HashSet<>(node.getInviters());

		for (UUID inviterId : inviters) {
			tree.removeInviteEdge(inviterId, playerId);
		}

		Set<UUID> unreachableIds = tree.findUnreachable();
		kickRemovedPlayers(source.getServer(), unreachableIds);
		TreeInvite.saveTree(source.getServer());

		Config config = TreeInvite.getConfig();
		Component message = Messages.formatComponent(config.success_revokedInvite, "player", Messages.playerComponent(node));
		source.sendSuccess(() -> message, false);
		if (!unreachableIds.isEmpty()) {
			String entries = unreachableIds.size() == 1 ? "entry" : "entries";
			Component removeMessage = Messages.formatComponent(config.info_totalUnreachable, "count", Component.literal(String.valueOf(unreachableIds.size())), "entries", Component.literal(entries));
			source.sendSuccess(() -> removeMessage, false);
		}
	}
}
