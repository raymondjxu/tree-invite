package net.raymondjxu.treevite.command;

import com.mojang.brigadier.CommandDispatcher;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public final class InviteInfoCommand {
	private InviteInfoCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("inviteinfo")
				.executes(context -> execute(context, null))
				.then(Commands.argument("player", StringArgumentType.word())
						.executes(context -> execute(context, StringArgumentType.getString(context, "player")))));
	}

	private static int execute(CommandContext<CommandSourceStack> context, String input) {
		CommandSourceStack source = context.getSource();
		InviteTree tree = TreeInvite.getTree();
		if (tree == null) {
			Config config = TreeInvite.getConfig();
			source.sendFailure(Component.literal(config != null ? config.error_systemNotReady : "Invite system is not ready yet"));
			return 0;
		}

		if (input == null) {
			UUID targetId = TreeInvite.getSourceUuid(source);
			Node node = tree.getNode(targetId);
			if (node == null && InviteTree.isRoot(targetId)) {
				node = tree.getOrCreateNode(targetId, "console", Platform.CONSOLE);
			}
			if (node == null) {
				Config config = TreeInvite.getConfig();
				source.sendFailure(Component.literal(config != null ? config.error_noInviteInfo : "No invite info available"));
				return 0;
			}
			showInfo(source, tree, node);
			return 1;
		}

		UUID direct = parseUuid(input);
		if (direct != null) {
			Node node = tree.getNode(direct);
			if (node == null) {
				Config config = TreeInvite.getConfig();
				source.sendFailure(Component.literal(config != null ? config.error_playerNotInTree : "Player is not in the invite tree"));
				return 0;
			}
			showInfo(source, tree, node);
			return 1;
		}

		List<Node> matches = tree.findByDisplayName(input);
		if (matches.isEmpty()) {
			Config config = TreeInvite.getConfig();
			source.sendFailure(Component.literal(config != null ? config.error_playerNotInTree : "Player is not in the invite tree"));
			return 0;
		}
		for (Node node : matches) {
			showInfo(source, tree, node);
		}
		return 1;
	}

	private static void showInfo(CommandSourceStack source, InviteTree tree, Node node) {
		String header = describePlayer(node);
		Config config = TreeInvite.getConfig();
		String headerMessage = config != null ? Messages.format(config.success_inviteInfoHeader, "player", header) : ("Invite info for " + header);
		source.sendSuccess(() -> Component.literal(headerMessage), false);

		Set<UUID> inviters = node.getInviters();
		Set<UUID> invitees = node.getInvitees();
		String invitedByMessage = config != null ? Messages.format(config.success_invitedBy, "list", joinIds(tree, inviters)) : ("Invited by: " + joinIds(tree, inviters));
		String invitedMessage = config != null ? Messages.format(config.success_invitedList, "list", joinIds(tree, invitees)) : ("Invited: " + joinIds(tree, invitees));
		source.sendSuccess(() -> Component.literal(invitedByMessage), false);
		source.sendSuccess(() -> Component.literal(invitedMessage), false);
	}

	private static String joinIds(InviteTree tree, Set<UUID> ids) {
		if (ids == null || ids.isEmpty()) {
			return "none";
		}
		List<String> names = new ArrayList<>();
		for (UUID id : ids) {
			names.add(describePlayer(tree.getNode(id), id));
		}
		return String.join(", ", names);
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

	private static String describePlayer(Node node, UUID id) {
		if (node == null) {
			return id.toString();
		}
		return describePlayer(node);
	}

	private static UUID parseUuid(String value) {
		try {
			return UUID.fromString(value);
		} catch (Exception ex) {
			return null;
		}
	}
}
