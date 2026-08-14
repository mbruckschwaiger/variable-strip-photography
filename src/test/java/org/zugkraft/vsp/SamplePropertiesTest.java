package org.zugkraft.vsp;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

public class SamplePropertiesTest {

	private static final Properties sampleProperties = new Properties();

	private static final List<String> PROPERTIES_ENTRIES = List.of(
			"vsp.method",
			"vsp.debug",
			"vsp.simple.width",
			"vsp.flow.algorithm",
			"vsp.flow.raft.maxSizeUnits"
	);

	@BeforeAll
	public static void setup() throws IOException {
		try (var is = SamplePropertiesTest.class.getResourceAsStream("/vsp.properties");
		     var reader = new InputStreamReader(Objects.requireNonNull(is), StandardCharsets.UTF_8)) {
			sampleProperties.load(reader);
		}
	}

	@Test
	public void testAllSamplePropertiesPresent() {
		for (var pe : PROPERTIES_ENTRIES) {
			var value = sampleProperties.getProperty(pe);
			Assertions.assertNotNull(value, String.format("property '%s' not in sample file", pe));
		}
	}
}
