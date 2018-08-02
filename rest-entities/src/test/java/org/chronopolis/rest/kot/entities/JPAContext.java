package org.chronopolis.rest.kot.entities;

import com.opentable.db.postgres.embedded.FlywayPreparer;
import com.opentable.db.postgres.embedded.PreparedDbProvider;
import org.flywaydb.core.Flyway;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;
import java.sql.SQLException;

/**
 * Basic context for running tests against our JPA entities
 *
 * Probably will try to migrate off of spring eventually but for now just carbon copies of the
 * ingest-rest versions
 *
 * @author shake
 */
@SpringBootApplication
@EntityScan(basePackageClasses = Bag.class)
public class JPAContext {

    private static final String SCHEMA_LOCATION = "db/schema";

    public static void main(String[] args) {
        SpringApplication.run(JPAContext.class);
    }

    @Bean
    @Profile("!gitlab")
    public DataSource embeddedDataSource() throws SQLException {
        FlywayPreparer preparer = FlywayPreparer.forClasspathLocation(SCHEMA_LOCATION);
        PreparedDbProvider provider = PreparedDbProvider.forPreparer(preparer);
        return provider.createDataSource();
    }

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
    @SuppressWarnings("Duplicates")
    public Flyway flyway(DataSource dataSource) {
        Flyway fly = new Flyway();
        fly.setDataSource(dataSource);
        fly.setLocations(SCHEMA_LOCATION);
        fly.clean();
        fly.migrate();
        return fly;
    }

}
