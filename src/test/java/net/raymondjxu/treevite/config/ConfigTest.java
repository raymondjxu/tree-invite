package net.raymondjxu.treevite.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ConfigTest {

	@Test
	void configHasDefaultValues() {
		Config config = new Config();

		assertNotNull(config.error_systemNotReady);
		assertNotNull(config.error_notWhitelisted);
		assertNotNull(config.success_invited);
	}

	@Test
	void configFieldsCanBeModified() {
		Config config = new Config();
		String original = config.error_systemNotReady;

		config.error_systemNotReady = "Modified";
		assertEquals("Modified", config.error_systemNotReady);

		config.error_systemNotReady = original;
		assertEquals(original, config.error_systemNotReady);
	}

	@Test
	void configErrorSystemNotReadyHasDefaultValue() {
		Config config = new Config();
		assertEquals("Invite system is not ready yet", config.error_systemNotReady);
	}

	private void assertTrue(boolean condition) {
		if (!condition) {
			throw new AssertionError("Expected true but was false");
		}
	}
}
