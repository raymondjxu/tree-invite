package net.raymondjxu.treevite;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.raymondjxu.treevite.command.InviteCommand;
import net.raymondjxu.treevite.command.InviteInfoCommand;
import net.raymondjxu.treevite.command.RevokeInviteCommand;
import net.raymondjxu.treevite.command.SetGlobalMaxInvitesCommand;
import net.raymondjxu.treevite.command.SetMaxInvitesCommand;
import net.raymondjxu.treevite.config.Config;
import net.raymondjxu.treevite.config.ConfigLoader;
import net.raymondjxu.treevite.data.InviteTree;
import net.raymondjxu.treevite.data.InviteTreeStore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.UUID;

public class TreeInvite implements ModInitializer {
	public static final String MOD_ID = "tree-invite";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static Config config;
	private static InviteTree tree = new InviteTree();
	private static InviteTreeStore store;

	@Override
	public void onInitialize() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			InviteCommand.register(dispatcher);
			RevokeInviteCommand.register(dispatcher);
			InviteInfoCommand.register(dispatcher);
			SetMaxInvitesCommand.register(dispatcher);
			SetGlobalMaxInvitesCommand.register(dispatcher);
		});

		ServerLifecycleEvents.SERVER_STARTING.register(server -> {
		    if (server.isUsingWhitelist()) {
				server.setUsingWhitelist();
				LOGGER.warn("Disabled the vanilla whitelist, Treevite manages the whitelist");
				}
		});

		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			// Load config from file or remote URL
			config = ConfigLoader.load(server.getServerDirectory());

			try {
				store = InviteTreeStore.open(server);
				tree = store.load();
				LOGGER.info("Loaded invite tree with {} nodes", tree.getNodes().size());
			} catch (SQLException ex) {
				LOGGER.error("Failed to open invite tree database", ex);
				tree = new InviteTree();
			}
		});

		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			if (store != null) {
				store.save(tree);
				store.close();
				store = null;
			}
		});

		ServerConfigurationConnectionEvents.CONFIGURE.register((handler, server) -> {
			UUID id = handler.getOwner().id();
			InviteTree currentTree = tree;
			if (currentTree == null) {
				return;
			}
			if (!currentTree.isWhitelisted(id)) {

				String message = config.error_notWhitelisted.replace("{source}", handler.getOwner().name());
				handler.disconnect(Component.literal(message));
			}
		});
	}

	public static Config getConfig() {
		return config;
	}

	public static InviteTree getTree() {
		return tree;
	}

	public static InviteTreeStore getStore() {
		return store;
	}

	public static void saveTree(MinecraftServer server) {
		if (store != null && tree != null) {
			store.save(tree);
		}
	}

	public static UUID getSourceUuid(CommandSourceStack source) {
		if (source.getEntity() != null) {
			return source.getEntity().getUUID();
		}
		return InviteTree.ROOT_ID;
	}

	public static String getSourceName(CommandSourceStack source) {
		if (source == null) {
			return "(System Root)";
		}
		return source.getTextName();
	}
}
