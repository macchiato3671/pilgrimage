package com.moonback.pilgrimage.support;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Provides an isolated MySQL instance for tests that exercise the real
 * database schema. Test classes are responsible for creating their own data.
 */
@Testcontainers(disabledWithoutDocker = true)
public abstract class AbstractMySqlIntegrationTest {

	protected static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
			.withDatabaseName("moonbackdb")
			.withUsername("test")
			.withPassword("test")
			.withCommand("--lower_case_table_names=1")
			.withInitScript("init.sql");

	@BeforeAll
	public static void startSharedMySqlContainer() {
		startContainerIfNecessary();
	}

	@DynamicPropertySource
	public static void registerDataSourceProperties(DynamicPropertyRegistry registry) {
		startContainerIfNecessary();
		registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
		registry.add("spring.datasource.username", MYSQL::getUsername);
		registry.add("spring.datasource.password", MYSQL::getPassword);
		registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
	}

	private static synchronized void startContainerIfNecessary() {
		if (!MYSQL.isRunning()) {
			MYSQL.start();
		}
	}
}
