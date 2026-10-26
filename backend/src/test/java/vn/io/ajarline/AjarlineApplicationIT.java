package vn.io.ajarline;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import vn.io.ajarline.support.PostgreSqlTestConfiguration;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(PostgreSqlTestConfiguration.class)
public class AjarlineApplicationIT {

    @Autowired
    private DataSource dataSource;
    @Test
    void context_ShouldStart_WhenBaselineConfigurationIsValid() throws SQLException {
        try (var connection = dataSource.getConnection()) {
            String databaseProductName = connection.getMetaData().getDatabaseProductName();
            assertThat(databaseProductName).isEqualTo("PostgreSQL");
        }
    }
}
