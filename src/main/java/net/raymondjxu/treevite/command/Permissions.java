package net.raymondjxu.treevite.command;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.raymondjxu.treevite.TreeInvite;
import net.raymondjxu.treevite.config.Config;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public final class Permissions {
	private static final String PERMISSIONS_CLASS = "me.lucko.fabric.api.permissions.v0.Permissions";
	private static final Method CHECK_METHOD;
	private static final AtomicBoolean WARNED_CONSOLE = new AtomicBoolean(false);
	private static final AtomicBoolean WARNED_CONSOLE_CALLER = new AtomicBoolean(false);
	private static final Set<UUID> WARNED_CALLERS = ConcurrentHashMap.newKeySet();

	static {
		Method method = null;
		try {
			Class<?> clazz = Class.forName(PERMISSIONS_CLASS);
			method = clazz.getMethod("check", Entity.class, String.class, int.class);
		} catch (Exception _) {
        }
		CHECK_METHOD = method;
		if (CHECK_METHOD == null) {
			warnMissingDependency(null);
		}
	}

	private Permissions() {
	}

	public static boolean check(CommandSourceStack source, String permission, int fallbackLevel) {
		if (source == null) {
			return false;
		}

		if (CHECK_METHOD == null) {
			warnMissingDependency(source);
		} else if(source.getEntity() != null) {
			try {
				Object result = CHECK_METHOD.invoke(null, source.getEntity(), permission, fallbackLevel);
				if (result instanceof Boolean) {
					return (Boolean) result;
				}
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		Boolean result = tryInvokeLevelCheck(source, "hasPermission", fallbackLevel);
		if (result == null) {
			result = tryInvokeLevelCheck(source, "hasPermissions", fallbackLevel);
		}
		if (result == null && source.getEntity() != null) {
			result = tryInvokeLevelCheck(source.getEntity(), "hasPermission", fallbackLevel);
			if (result == null) {
				result = tryInvokeLevelCheck(source.getEntity(), "hasPermissions", fallbackLevel);
			}
		}
		if (result != null) {
			return result;
		}
		return source.getEntity() == null;
	}

	private static void warnMissingDependency(CommandSourceStack source) {
		if (WARNED_CONSOLE.compareAndSet(false, true)) {
			Config config = TreeInvite.getConfig();
			String message = config.error_permissionsApiMissing;
			TreeInvite.LOGGER.warn(message);
		}
		if (source == null) {
			return;
		}
		UUID callerId = source.getEntity() == null ? null : source.getEntity().getUUID();
		if (callerId != null) {
			if (WARNED_CALLERS.add(callerId)) {
				Config config = TreeInvite.getConfig();
				String message = config.error_permissionsApiMissing;
				source.sendFailure(Component.literal(message));
			}
			return;
		}
		if (WARNED_CONSOLE_CALLER.compareAndSet(false, true)) {
			Config config = TreeInvite.getConfig();
			String message = config.error_permissionsApiMissing;
			source.sendFailure(Component.literal(message));
		}
	}

	private static Boolean tryInvokeLevelCheck(Object target, String methodName, int level) {
		if (target == null) {
			return null;
		}
		try {
			Method method = target.getClass().getMethod(methodName, int.class);
			Object result = method.invoke(target, level);
			if (result instanceof Boolean) {
				return (Boolean) result;
			}
			return null;
		} catch (Exception ex) {
			return null;
		}
	}
}
