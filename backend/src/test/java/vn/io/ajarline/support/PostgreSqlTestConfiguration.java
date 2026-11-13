package vn.io.ajarline.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.postgresql.PostgreSQLContainer;

@TestConfiguration(proxyBeanMethods = false)
public class PostgreSqlTestConfiguration {
    private static final String POSTGRES_IMAGE = "postgres:18.6-alpine3.24";

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgreSQLContainer() {
        return new PostgreSQLContainer(POSTGRES_IMAGE)
                .withDatabaseName("ajarline")
                .withUsername("ajarline")
                .withPassword("ajarline-test");
    }

    /**
     * Supplies external Admin configuration to application tests without
     * reading operator secrets or committing a test password.
     */
    @Bean
    DynamicPropertyRegistrar adminTestProperties() {
        return registry -> {
            registry.add("ajarline.security.admin.username",
                    AdminAuthenticationTestData::username);
            registry.add("ajarline.security.admin.password-hash",
                    AdminAuthenticationTestData::passwordHash);
        };
    }


}
