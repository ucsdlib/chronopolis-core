package org.chronopolis.ingest;

import com.opentable.db.postgres.embedded.FlywayPreparer;
import com.opentable.db.postgres.embedded.PreparedDbProvider;
import org.chronopolis.ingest.repository.Authority;
import org.chronopolis.rest.entities.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.security.provisioning.JdbcUserDetailsManager;

import javax.sql.DataSource;
import java.sql.SQLException;

/**
 * Context for our JPA Stuff
 *
 * EntityScan
 * Necessary beans
 *
 * Created by shake on 6/29/17.
 */
@SpringBootApplication
@EntityScan(basePackageClasses = {Authority.class, Node.class})
public class JpaContext {

    public static void main(String[] args) {
        SpringApplication.run(JpaContext.class);
    }

    @Bean
    @Profile("!gitlab")
    public DataSource embeddedDataSource() throws SQLException {
        FlywayPreparer preparer = FlywayPreparer.forClasspathLocation("db/schema");
        PreparedDbProvider provider = PreparedDbProvider.forPreparer(preparer);
        return provider.createDataSource();
    }

    @Bean
    @Profile("gitlab")
    public DataSource serviceDataSource() {
        return DataSourceBuilder.create()
                .url("jdbc:postgresql://postgres/ingest-test")
                .username("runner")
                .driverClassName("org.postgresql.Driver")
                .build();
    }

    @Bean
    public JdbcUserDetailsManager jdbcUserDetailsManager(DataSource dataSource) {
        JdbcUserDetailsManager manager = new JdbcUserDetailsManager();
        manager.setDataSource(dataSource);
        return manager;
    }

}
