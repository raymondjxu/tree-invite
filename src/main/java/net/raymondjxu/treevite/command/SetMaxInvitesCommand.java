package net.raymondjxu.treevite.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.raymondjxu.treevite.TreeInvite;
import net.raymondjxu.treevite.config.Config;
import net.raymondjxu.treevite.config.Messages;
import net.raymondjxu.treevite.data.InviteTree;
import net.raymondjxu.treevite.data.Node;
import net.raymondjxu.treevite.data.Platform;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class SetMaxInvitesCommand {
	private SetMaxInvitesCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("setmaxinvites")
				.requires(source -> Permissions.check(source, "treeinvite.admin", 3))
				.then(Commands.argument("player", StringArgumentType.word())
						.then(Commands.argument("count", IntegerArgumentType.integer(0))
								.executes(context -> execute(context, null)))
						.then(Commands.literal("java")
								.then(Commands.argument("count", IntegerArgumentType.integer(0))
										.executes(context -> execute(context, Platform.JAVA))))
						.then(Commands.literal("bedrock")
								.then(Commands.argument("count", IntegerArgumentType.integer(0))
										.executes(context -> execute(context, Platform.BEDROCK))))));
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
		int count = IntegerArgumentType.getInteger(context, "count");
		UUID direct = parseUuid(input);
		if (direct != null) {
			if (platformArg != null) {
				Config config = TreeInvite.getConfig();
				source.sendFailure(Component.literal(config != null ? config.error_cannotSpecifyPlatform : "Cannot specify platform given raw UUID"));
				return 0;
			}
			Node node = tree.getNode(direct);
			if (node == null) {
				Config config = TreeInvite.getConfig();
				source.sendFailure(Component.literal(config != null ? config.error_playerNotInTree : "Player is not in the invite tree"));
				return 0;
			}
			node.setMaxInvitesOverride(count);
			TreeInvite.saveTree(source.getServer());
			Config config = TreeInvite.getConfig();
			String message = config != null ? Messages.format(config.success_setMaxInvites, "player", describePlayer(node), "count", count) : ("Set max invites for " + describePlayer(node) + " to " + count);
			source.sendSuccess(() -> Component.literal(message), false);
			return 1;
		}

		if (platformArg != null) {
			Node node = findSingleMatch(tree.findByDisplayName(input), platformArg);
			if (node == null) {
				Config config = TreeInvite.getConfig();
				source.sendFailure(Component.literal(config != null ? config.error_playerNotInTree : "Player is not in the invite tree"));
				return 0;
			}
			node.setMaxInvitesOverride(count);
			TreeInvite.saveTree(source.getServer());
			Config config = TreeInvite.getConfig();
			String message = config != null ? Messages.format(config.success_setMaxInvites, "player", describePlayer(node), "count", count) : ("Set max invites for " + describePlayer(node) + " to " + count);
			source.sendSuccess(() -> Component.literal(message), false);
			return 1;
		}

		List<Node> matches = tree.findByDisplayName(input);
		if (matches.size() > 1) {
			Config config = TreeInvite.getConfig();
			source.sendFailure(Component.literal(config != null ? config.error_nameAmbiguous : "Player name is ambiguous; specify java or bedrock"));
			return 0;
		}
		if (matches.isEmpty()) {
			Config config = TreeInvite.getConfig();
			source.sendFailure(Component.literal(config != null ? config.error_playerNotInTree : "Player is not in the invite tree"));
			return 0;
		}

		Node node = matches.get(0);
		node.setMaxInvitesOverride(count);
		TreeInvite.saveTree(source.getServer());
		Config config = TreeInvite.getConfig();
		String message = config != null ? Messages.format(config.success_setMaxInvites, "player", describePlayer(node), "count", count) : ("Set max invites for " + describePlayer(node) + " to " + count);
		source.sendSuccess(() -> Component.literal(message), false);
		return 1;
	}

	private static UUID parseUuid(String value) {
		try {
			return UUID.fromString(value);
		} catch (Exception ex) {
			return null;
		}
	}

	private static Node findSingleMatch(List<Node> matches, Platform platform) {
		if (matches == null || matches.isEmpty()) {
			return null;
		}
		Node found = null;
		for (Node node : matches) {
			if (platform != null && node.getPlatform() != platform) {
				continue;
			}
			if (found != null) {
				return null;
			}
			found = node;
		}
		return found;
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
}
