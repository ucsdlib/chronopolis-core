package org.chronopolis.ingest;

import com.opentable.db.postgres.embedded.FlywayPreparer;
import com.opentable.db.postgres.embedded.PreparedDbProvider;
import org.chronopolis.ingest.repository.Authority;
import org.chronopolis.rest.entities.AceToken;
import org.flywaydb.core.Flyway;
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
 * <p>
 * EntityScan
 * Necessary beans
 * <p>
 * Created by shake on 6/29/17.
 */
@SpringBootApplication
@EntityScan(basePackageClasses = {Authority.class, AceToken.class})
public class JpaContext {

    private static final String SCHEMA_LOCATION = "db/schema";
    public static final String CREATE_SCRIPT = "classpath:sql/create.sql";
    public static final String DELETE_SCRIPT = "classpath:sql/delete.sql";

    public static void main(String[] args) {
        SpringApplication.run(JpaContext.class);
    }

    @Bean
    @Profile("!gitlab")
    public DataSource embeddedDataSource() throws SQLException {
        FlywayPreparer preparer = FlywayPreparer.forClasspathLocation(SCHEMA_LOCATION);
        PreparedDbProvider provider = PreparedDbProvider.forPreparer(preparer);
        return provider.createDataSource();
    }

    /*
    @Bean
    @Profile("!gitlab")
    public DataSource dataSource() throws SQLException {
        String driver = "org.postgresql.Driver";
        String url = "jdbc:postgresql://172.17.0.2/ingest3";
        String username = "readonly";
        String password = "ro";

        return DataSourceBuilder.create()
                .url(url)
                .username(username)
                .password(password)
                .driverClassName(driver)
                .build();
    }

    */
    @Bean
    @Profile("gitlab")
    public DataSource serviceDataSource() {
        String driver = "org.postgresql.Driver";
        String url = "jdbc:postgresql://postgres/ingest-test";
        String username = "runner";

        return DataSourceBuilder.create()
                .url(url)
                .username(username)
                .driverClassName(driver)
                .build();
    }

    @Bean
    @Profile("gitlab")
    public Flyway flyway(DataSource dataSource) {
        Flyway fly = new Flyway();
        fly.setDataSource(dataSource);
        fly.setLocations(SCHEMA_LOCATION);
        fly.clean();
        fly.migrate();
        return fly;
    }

    @Bean
    public JdbcUserDetailsManager jdbcUserDetailsManager(DataSource dataSource) {
        JdbcUserDetailsManager manager = new JdbcUserDetailsManager();
        manager.setDataSource(dataSource);
        return manager;
    }

}
