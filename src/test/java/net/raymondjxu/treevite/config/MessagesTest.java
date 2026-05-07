package net.raymondjxu.treevite.config;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class MessagesTest {

	@Test
	void formatWithMapReplacesSinglePlaceholder() {
		String template = "Invited {player}";
		Map<String, Object> params = new HashMap<>();
		params.put("player", "Alice");

		String result = Messages.format(template, params);
		assertEquals("Invited Alice", result);
	}

	@Test
	void formatWithMapReplacesMultiplePlaceholders() {
		String template = "Set max invites for {player} to {count}";
		Map<String, Object> params = new HashMap<>();
		params.put("player", "Bob");
		params.put("count", 5);

		String result = Messages.format(template, params);
		assertEquals("Set max invites for Bob to 5", result);
	}

	@Test
	void formatWithMapIgnoresUnusedParams() {
		String template = "Invited {player}";
		Map<String, Object> params = new HashMap<>();
		params.put("player", "Alice");
		params.put("unused", "value");

		String result = Messages.format(template, params);
		assertEquals("Invited Alice", result);
	}

	@Test
	void formatWithMapKeepsUnreplacedPlaceholders() {
		String template = "Invited {player} and {other}";
		Map<String, Object> params = new HashMap<>();
		params.put("player", "Alice");

		String result = Messages.format(template, params);
		assertEquals("Invited Alice and {other}", result);
	}

	@Test
	void formatWithMapHandlesNullParameter() {
		String template = "Invited {player}";
		Map<String, Object> params = new HashMap<>();
		params.put("player", null);

		String result = Messages.format(template, params);
		assertEquals("Invited ", result);
	}

	@Test
	void formatWithMapHandlesEmptyMap() {
		String template = "Invited {player}";
		Map<String, Object> params = new HashMap<>();

		String result = Messages.format(template, params);
		assertEquals("Invited {player}", result);
	}

	@Test
	void formatWithMapHandlesNullMap() {
		String template = "Invited {player}";

		String result = Messages.format(template, null);
		assertEquals("Invited {player}", result);
	}

	@Test
	void formatWithMapHandlesEmptyTemplate() {
		String template = "";
		Map<String, Object> params = new HashMap<>();
		params.put("player", "Alice");

		String result = Messages.format(template, params);
		assertEquals("", result);
	}

	@Test
	void formatWithMapHandlesNoPlaceholders() {
		String template = "Invited player";
		Map<String, Object> params = new HashMap<>();
		params.put("player", "Alice");

		String result = Messages.format(template, params);
		assertEquals("Invited player", result);
	}

	@Test
	void formatWithMapConvertsSObjectToString() {
		String template = "Count: {count}";
		Map<String, Object> params = new HashMap<>();
		params.put("count", 42);

		String result = Messages.format(template, params);
		assertEquals("Count: 42", result);
	}

	@Test
	void formatSingleParameterReplaces() {
		String template = "Invited {player}";
		String result = Messages.format(template, "player", "Alice");
		assertEquals("Invited Alice", result);
	}

	@Test
	void formatSingleParameterWithNull() {
		String template = "Invited {player}";
		String result = Messages.format(template, "player", null);
		assertEquals("Invited ", result);
	}

	@Test
	void formatTwoParametersReplacesBoth() {
		String template = "Set max invites for {player} to {count}";
		String result = Messages.format(template, "player", "Bob", "count", 10);
		assertEquals("Set max invites for Bob to 10", result);
	}

	@Test
	void formatTwoParametersWithOneNull() {
		String template = "Set max invites for {player} to {count}";
		String result = Messages.format(template, "player", null, "count", 10);
		assertEquals("Set max invites for  to 10", result);
	}

	@Test
	void formatThreeParametersReplaceAll() {
		String template = "Removed {count} unreachable {entries} from {player}";
		String result = Messages.format(template, "count", 3, "entries", "entries", "player", "Alice");
		assertEquals("Removed 3 unreachable entries from Alice", result);
	}

	@Test
	void formatThreeParametersWithNulls() {
		String template = "Removed {count} unreachable {entries} from {player}";
		String result = Messages.format(template, "count", null, "entries", null, "player", "Alice");
		assertEquals("Removed  unreachable  from Alice", result);
	}

	@Test
	void formatReplacesRepeatedPlaceholder() {
		String template = "{name} is {name}";
		String result = Messages.format(template, "name", "Echo");
		assertEquals("Echo is Echo", result);
	}

	@Test
	void formatWithSpecialCharactersInValue() {
		String template = "Lookup failed: {message}";
		String result = Messages.format(template, "message", "Connection timeout: details");
		assertEquals("Lookup failed: Connection timeout: details", result);
	}

	@Test
	void formatHandlesPlaceholderWithNumbers() {
		String template = "Reset at {reset}";
		String result = Messages.format(template, "reset", "2024-01-02 14:30");
		assertEquals("Reset at 2024-01-02 14:30", result);
	}

	@Test
	void formatComplexTemplate() {
		String template = "Invite limit reached ({used}/{limit}). Resets at {reset}";
		Map<String, Object> params = new HashMap<>();
		params.put("used", 3);
		params.put("limit", 5);
		params.put("reset", "2024-01-15 02:00");

		String result = Messages.format(template, params);
		assertEquals("Invite limit reached (3/5). Resets at 2024-01-15 02:00", result);
	}

	@Test
	void formatDoesNotReplaceBracesWithoutContent() {
		String template = "Test {} and {player}";
		String result = Messages.format(template, "player", "Alice");
		assertEquals("Test {} and Alice", result);
	}

	@Test
	void formatCaseSensitiveParameterNames() {
		String template = "Invited {player} and {Player}";
		Map<String, Object> params = new HashMap<>();
		params.put("player", "alice");
		params.put("Player", "Bob");

		String result = Messages.format(template, params);
		assertEquals("Invited alice and Bob", result);
	}
}
