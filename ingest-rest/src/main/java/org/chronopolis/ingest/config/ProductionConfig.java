package org.chronopolis.ingest.config;

import org.chronopolis.ingest.models.UserRequest;
import org.chronopolis.ingest.repository.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
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
@SuppressWarnings("unused")
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
     * TODO: Use the UserDetailsManager
     *
     */
    @Bean
    public boolean checkUsers(UserService service, DataSource dataSource) {
        JdbcTemplate template = new JdbcTemplate(dataSource);
        List<String> usernames = template.query(SELECT_USERNAMES,
                new Object[]{},
                (resultSet, i) -> resultSet.getString(1));

        if (usernames.isEmpty()) {
            log.info("No users found, registering default admin user");
            service.createUser(new UserRequest(DEFAULT_ADMIN, DEFAULT_ADMIN, true, false));
        }

        return true;
    }

}
