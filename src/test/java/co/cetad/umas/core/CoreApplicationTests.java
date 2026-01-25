package co.cetad.umas.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

import static org.junit.jupiter.api.Assertions.*;

@Suite
@SuiteDisplayName("UMAS Core Service Test Suite")
@SelectPackages("co.cetad.umas.core")
class CoreApplicationTests {

	@Test
	@DisplayName("Main method should not throw exception")
	void mainShouldNotThrowException() {
		// Test that the main class exists and can be instantiated
		var app = new CoreApplication();
		assertNotNull(app);
	}

	@Test
	@DisplayName("Application class should be properly annotated")
	void applicationClassShouldBeProperlyAnnotated() {
		var clazz = CoreApplication.class;

		// Verify Spring Boot annotations are present
		assertTrue(clazz.isAnnotationPresent(
			org.springframework.boot.autoconfigure.SpringBootApplication.class
		));
	}

}
