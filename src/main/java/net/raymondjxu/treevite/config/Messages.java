package net.raymondjxu.treevite.config;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.raymondjxu.treevite.data.Node;
import net.raymondjxu.treevite.data.Platform;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Helper class for formatting messages from config with template placeholders.
 * 
 * String-based methods (legacy): Replace {key} placeholders with string values.
 * Component-based methods: Build styled MutableComponent instances with colors.
 *   - Success messages are light green
 *   - Player names are aqua
 */
public class Messages {
	private Messages() {
	}

	// ============ String-based formatting (legacy, backwards compatible) ============

	/**
	 * Formats a template string by replacing placeholders with values.
	 * Example: format("Invited {player}", {player: "Alice"}) -> "Invited Alice"
	 */
	public static String format(String template, Map<String, Object> params) {
		if (params == null || params.isEmpty()) {
			return template;
		}

		String result = template;
		for (Map.Entry<String, Object> entry : params.entrySet()) {
			String placeholder = "{" + entry.getKey() + "}";
			String value = entry.getValue() != null ? entry.getValue().toString() : "";
			result = result.replace(placeholder, value);
		}
		return result;
	}

	/**
	 * Convenience method for single parameter formatting.
	 */
	public static String format(String template, String key, Object value) {
		Map<String, Object> params = new HashMap<>();
		params.put(key, value);
		return format(template, params);
	}

	/**
	 * Convenience method for two parameters.
	 */
	public static String format(String template, String key1, Object value1, String key2, Object value2) {
		Map<String, Object> params = new HashMap<>();
		params.put(key1, value1);
		params.put(key2, value2);
		return format(template, params);
	}

	/**
	 * Convenience method for three parameters.
	 */
	public static String format(String template, String key1, Object value1, String key2, Object value2, String key3, Object value3) {
		Map<String, Object> params = new HashMap<>();
		params.put(key1, value1);
		params.put(key2, value2);
		params.put(key3, value3);
		return format(template, params);
	}

	// ============ Component-based formatting (with colors) ============

	/**
	 * Formats a template string into a styled MutableComponent by replacing placeholders with Components.
	 * The result is wrapped in light green for success messages.
	 * 
	 * Example: formatComponent("Invited {player}", {player: aquaPlayerComponent}) 
	 *          -> light green message with aqua player name
	 */
	public static MutableComponent formatComponent(String template, Map<String, Component> params) {
		if (params == null || params.isEmpty()) {
			return Component.literal(template).withStyle(ChatFormatting.GREEN);
		}

		MutableComponent result = Component.literal("");
		String remaining = template;

		while (!remaining.isEmpty()) {
			// Find whichever placeholder appears earliest in the remaining string
			int earliestIndex = -1;
			String earliestKey = null;

			for (String key : params.keySet()) {
				String placeholder = "{" + key + "}";
				int index = remaining.indexOf(placeholder);
				if (index >= 0 && (earliestIndex < 0 || index < earliestIndex)) {
					earliestIndex = index;
					earliestKey = key;
				}
			}

			if (earliestKey == null) {
				// No more placeholders found — append the rest as plain text
				result.append(Component.literal(remaining));
				break;
			}

			String placeholder = "{" + earliestKey + "}";

			// Append text before this placeholder
			if (earliestIndex > 0) {
				result.append(Component.literal(remaining.substring(0, earliestIndex)));
			}

			// Append the component
			result.append(params.get(earliestKey));

			// Advance past the placeholder
			remaining = remaining.substring(earliestIndex + placeholder.length());
		}

		return result.withStyle(ChatFormatting.GREEN);
	}

	/**
	 * Convenience method for single component parameter.
	 */
	public static MutableComponent formatComponent(String template, String key, Component value) {
		Map<String, Component> params = new HashMap<>();
		params.put(key, value);
		return formatComponent(template, params);
	}

	/**
	 * Convenience method for two component parameters.
	 */
	public static MutableComponent formatComponent(String template, String key1, Component value1, String key2, Component value2) {
		Map<String, Component> params = new HashMap<>();
		params.put(key1, value1);
		params.put(key2, value2);
		return formatComponent(template, params);
	}

	/**
	 * Convenience method for three component parameters.
	 */
	public static MutableComponent formatComponent(String template, String key1, Component value1, String key2, Component value2, String key3, Component value3) {
		Map<String, Component> params = new HashMap<>();
		params.put(key1, value1);
		params.put(key2, value2);
		params.put(key3, value3);
		return formatComponent(template, params);
	}

	// ============ Player component builders ============

	/**
	 * Creates an aqua-colored component describing a player based on a Node.
	 * Format: "PlayerName (java)" or "PlayerName (bedrock)" or "uuid (platform)" if name is missing.
	 */
	public static Component playerComponent(Node node) {
		if (node == null) {
			return Component.literal("<unknown>").withStyle(ChatFormatting.AQUA);
		}

		String name = node.getDisplayName();
		String platform = node.getPlatform() == null ? Platform.UNKNOWN.name() : node.getPlatform().name();
		if (name == null || name.isBlank()) {
			return Component.literal(node.getId() + " (" + platform.toLowerCase(Locale.ROOT) + ")")
					.withStyle(Style.EMPTY.withClickEvent(new ClickEvent.RunCommand("inviteinfo " + node.getId())))
					.withStyle(ChatFormatting.AQUA);
		}
		return Component.literal(name)
				.withStyle(Style.EMPTY.withClickEvent(new ClickEvent.RunCommand("inviteinfo " + node.getId())))
				.withStyle(ChatFormatting.AQUA);
	}

	/**
	 * Creates an aqua-colored component describing a player by name and platform.
	 */
	public static Component playerComponent(String name, Platform platform) {
		String platformStr = platform == null ? Platform.UNKNOWN.name() : platform.name();
		return Component.literal(name + " (" + platformStr.toLowerCase(Locale.ROOT) + ")")
				.withStyle(ChatFormatting.AQUA);
	}

	// ASCII Art Niceties
	/**
	 * Creates a equal sign based delimiter with a title for better readability in logs or messages.
	 */
	public static Component generateDelimiter(String title) {
		String line = "=".repeat(32);
		if (title != null && !title.isBlank()) {
			int totalPadding = line.length() - title.length() - 2; // 2 for the spaces
			int leftPadding = totalPadding / 2;
			int rightPadding = totalPadding - leftPadding; // absorbs the odd remainder
			String paddedTitle = "=".repeat(leftPadding) + " " + title + " " + "=".repeat(rightPadding);
			return Component.literal(paddedTitle).withStyle(ChatFormatting.YELLOW);
		}
		return Component.literal(line).withStyle(ChatFormatting.YELLOW);
	}

	public static final Component DELIMITER = generateDelimiter(null);
}
