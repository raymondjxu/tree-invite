package net.raymondjxu.treevite.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.raymondjxu.treevite.TreeInvite;
import net.raymondjxu.treevite.config.Config;
import net.raymondjxu.treevite.config.Messages;
import net.raymondjxu.treevite.data.InviteTree;
import net.raymondjxu.treevite.data.Node;
import net.raymondjxu.treevite.data.Platform;
import net.raymondjxu.treevite.lookup.ProfileLookup;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class InviteCommand {
	private InviteCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("invite")
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
			source.sendFailure(Component.literal(config.error_systemNotReady));
			return 0;
		}
		String input = StringArgumentType.getString(context, "player");
		UUID inviterId = TreeInvite.getSourceUuid(source);
		if (!tree.isWhitelisted(inviterId)) {
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
			handleResolvedInvite(source, tree, inviterId, direct, null, Platform.UNKNOWN);
			return 1;
		}

		Platform platform = platformArg == null ? Platform.JAVA : platformArg;
		CompletableFuture<ProfileLookup.LookupResult> future = ProfileLookup.lookup(input, platform);
		MinecraftServer server = source.getServer();
		future.thenAccept(result -> server.execute(() -> {
			if (!result.isSuccess()) {
				source.sendFailure(Component.literal(result.getError()));
				return;
			}
			handleResolvedInvite(source, tree, inviterId, result.getUuid(), result.getDisplayName(), result.getPlatform());
		})).exceptionally(ex -> {
			server.execute(() -> {
				Config config = TreeInvite.getConfig();
				String message = Messages.format(config.error_lookupFailed, "message", ex.getMessage()) + ex.getMessage();
				source.sendFailure(Component.literal(message));
			});
			return null;
		});

		return 1;
	}

	private static void handleResolvedInvite(CommandSourceStack source, InviteTree tree, UUID inviterId, UUID inviteeId, String displayName, Platform platform) {
		if (inviterId.equals(inviteeId)) {
			Config config = TreeInvite.getConfig();
			source.sendFailure(Component.literal(config.error_cannotInviteSelf));
			return;
		}
		Node inviter = tree.getOrCreateNode(inviterId, TreeInvite.getSourceName(source), Platform.UNKNOWN);
		Node invitee = tree.getOrCreateNode(inviteeId, displayName, platform);
		if (tree.hasInviteEdge(inviter.getId(), invitee.getId())) {
			Config config = TreeInvite.getConfig();
			source.sendSuccess(() -> Component.literal(config.success_alreadyInvited).withStyle(ChatFormatting.RED), false);
			return;
		}

		InviteTree.InviteLimitResult limit = tree.tryConsumeInvite(inviter.getId(), Instant.now(), ZoneId.systemDefault());
		if (!limit.isAllowed()) {
			String reset = limit.getNextReset().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
			Config config = TreeInvite.getConfig();
			String message = Messages.format(config.error_inviteLimitReached, "used", limit.getUsed(), "limit", limit.getLimit(), "reset", reset);
			source.sendFailure(Component.literal(message));
			return;
		}

		boolean added = tree.addInviteEdge(inviter.getId(), invitee.getId());
		if (!added) {
			Config config = TreeInvite.getConfig();
			source.sendFailure(Component.literal(config.error_addInviteFailed));
			return;
		}

		TreeInvite.saveTree(source.getServer());
		Config config = TreeInvite.getConfig();
		Component message = Messages.formatComponent(config.success_invited, "player", Messages.playerComponent(invitee));
		source.sendSuccess(() -> message, false);
	}

	private static UUID parseUuid(String value) {
		try {
			return UUID.fromString(value);
		} catch (Exception ex) {
			return null;
		}
	}
}
