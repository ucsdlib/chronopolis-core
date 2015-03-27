package org.chronopolis.ingest.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;


/**
 * Do a few tasks related to startup when running a production server.
 * These include:
 *   - Making sure we have an admin user, and if not creating a default one
 *
 * Created by shake on 3/24/15.
 */
@Configuration
@Profile("production")
public class ProductionConfig {
    private final Logger log = LoggerFactory.getLogger(ProductionConfig.class);

    private static final String SELECT_USERNAMES = "select username from users";
    private static final String INSERT_ADMIN     = "insert into users (username, password, enabled) values (?, ?, ?)";
    private static final String INSERT_AUTHORITY = "insert into authorities (username, authority) values (?, ?)";
    private static final String DEFAULT_ADMIN    = "admin";
    private static final String ROLE_ADMIN       = "ROLE_ADMIN";

    /**
     * We only need to grab the usernames, and check if any exist
     * If none do, we create a default admin user
     *
     */
    @Bean
    public boolean checkUsers(DataSource dataSource) {
        JdbcTemplate template = new JdbcTemplate(dataSource);
        List<String> usernames = template.query(SELECT_USERNAMES, new Object[]{}, new RowMapper<String>() {
            @Override
            public String mapRow(ResultSet resultSet, int i) throws SQLException {
                return resultSet.getString(1);
            }
        });

        if (usernames.isEmpty()) {
            log.info("No users found, registering default admin user");

            // Insert into our users table
            template.update(INSERT_ADMIN, new PreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement preparedStatement) throws SQLException {
                    preparedStatement.setString(1, DEFAULT_ADMIN);
                    preparedStatement.setString(2, DEFAULT_ADMIN);
                    preparedStatement.setBoolean(3, true);
                }
            });

            // And into our authorities table
            template.update(INSERT_AUTHORITY, new PreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement preparedStatement) throws SQLException {
                    preparedStatement.setString(1, DEFAULT_ADMIN);
                    preparedStatement.setString(2, ROLE_ADMIN);
                }
            });
        }

        return true;
    }

}
