package net.raymondjxu.treevite.config;

import java.util.HashMap;
import java.util.Map;

/**
 * Helper class for formatting messages from config with template placeholders.
 * Replaces {key} placeholders with values from the provided map.
 */
public class Messages {
	private Messages() {
	}

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
}
