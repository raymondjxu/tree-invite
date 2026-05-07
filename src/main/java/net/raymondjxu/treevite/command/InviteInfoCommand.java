package net.raymondjxu.treevite.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.raymondjxu.treevite.TreeInvite;
import net.raymondjxu.treevite.config.Config;
import net.raymondjxu.treevite.config.Messages;
import net.raymondjxu.treevite.data.InviteTree;
import net.raymondjxu.treevite.data.Node;
import net.raymondjxu.treevite.data.Platform;

import java.util.ArrayList;
import java.util.List;
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
			source.sendFailure(Component.literal(config.error_systemNotReady));
			return 0;
		}

		if (input == null) {
			UUID targetId = TreeInvite.getSourceUuid(source);
			Node node = tree.getNode(targetId);
			if (node == null && InviteTree.isRoot(targetId)) {
				node = tree.getOrCreateNode(targetId, "(System Root)", Platform.SYSTEM);
			}
			if (node == null) {
				Config config = TreeInvite.getConfig();
				source.sendFailure(Component.literal(config.error_noInviteInfo));
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
				source.sendFailure(Component.literal(config.error_playerNotInTree));
				return 0;
			}
			showInfo(source, tree, node);
			return 1;
		}

		List<Node> matches = tree.findByDisplayName(input);
		if (matches.isEmpty()) {
			Config config = TreeInvite.getConfig();
			source.sendFailure(Component.literal(config.error_playerNotInTree));
			return 0;
		}
		for (Node node : matches) {
			showInfo(source, tree, node);
		}
		return 1;
	}

	private static void showInfo(CommandSourceStack source, InviteTree tree, Node node) {
		Config config = TreeInvite.getConfig();
		source.sendSuccess(() -> Messages.generateDelimiter(node.getDisplayName() + "'s Invite Info"), false);

		Set<UUID> inviters = node.getInviters();
		Set<UUID> invitees = node.getInvitees();
		Component platformMessage = Messages.formatComponent("Platform: {platform}", "platform", Component.literal(node.getPlatform().toString()));
		Component invitedByMessage = Messages.formatComponent(config.success_invitedBy, "list", joinPlayerList(tree, inviters));
		Component invitedMessage = Messages.formatComponent(config.success_invitedList, "list", joinPlayerList(tree, invitees));
		Component usesMessage = Messages.formatComponent(config.success_invitesUsed, "used", Component.literal(Integer.toString(node.getInvitesUsed())), "max", Component.literal(Integer.toString(node.getMaxInvitesOverride() != null ? node.getMaxInvitesOverride() : tree.getGlobalMaxInvites())));
		source.sendSuccess(() -> platformMessage, false);
		source.sendSuccess(() -> invitedByMessage, false);
		source.sendSuccess(() -> invitedMessage, false);
		source.sendSuccess(() -> usesMessage, false);
		source.sendSuccess(() -> Messages.DELIMITER, false);
	}

	private static Component joinPlayerList(InviteTree tree, Set<UUID> ids) {
		if (ids == null || ids.isEmpty()) {
			return Component.literal("(nobody)");
		}
		List<Component> components = new ArrayList<>();
		List<UUID> idList = new ArrayList<>(ids);
		for (int i = 0; i < idList.size(); i++) {
			if (i > 0) {
				components.add(Component.literal(", "));
			}
			components.add(Messages.playerComponent(tree.getNode(idList.get(i))));
		}
		
		MutableComponent result = Component.literal("");
		for (Component comp : components) {
			result.append(comp);
		}
		return result;
	}

	private static UUID parseUuid(String value) {
		try {
			return UUID.fromString(value);
		} catch (Exception ex) {
			return null;
		}
	}
}
