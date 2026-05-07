package net.raymondjxu.treevite.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.raymondjxu.treevite.TreeInvite;
import net.raymondjxu.treevite.config.Config;
import net.raymondjxu.treevite.config.Messages;
import net.raymondjxu.treevite.data.InviteTree;

public final class SetGlobalMaxInvitesCommand {
	private SetGlobalMaxInvitesCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("setglobalmaxinvites")
				.requires(source -> Permissions.check(source, "treeinvite.admin", 3))
				.then(Commands.argument("count", IntegerArgumentType.integer(0))
						.executes(SetGlobalMaxInvitesCommand::execute))
		);
	}

	private static int execute(CommandContext<CommandSourceStack> context) {
		CommandSourceStack source = context.getSource();
		InviteTree tree = TreeInvite.getTree();
		if (tree == null) {
			Config config = TreeInvite.getConfig();
			source.sendFailure(Component.literal(config != null ? config.error_systemNotReady : "Invite system is not ready yet"));
			return 0;
		}
		int count = IntegerArgumentType.getInteger(context, "count");
		tree.setGlobalMaxInvites(count);
		TreeInvite.saveTree(source.getServer());
		Config config = TreeInvite.getConfig();
		String message = config != null ? Messages.format(config.success_setGlobalMaxInvites, "count", count) : ("Set global max invites to " + count);
		source.sendSuccess(() -> Component.literal(message), false);
		return 1;
	}
}
