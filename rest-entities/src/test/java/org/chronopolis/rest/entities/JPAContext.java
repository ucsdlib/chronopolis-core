package org.chronopolis.rest.entities;

import com.opentable.db.postgres.embedded.FlywayPreparer;
import com.opentable.db.postgres.embedded.PreparedDbProvider;
import org.chronopolis.rest.entities.depositor.Depositor;
import org.chronopolis.rest.models.enums.BagStatus;
import org.flywaydb.core.Flyway;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;
import java.sql.SQLException;

/**
 * Basic context for running tests against our JPA entities
 * <p>
 * Probably will try to migrate off of spring eventually but for now just carbon copies of the
 * ingest-rest versions
 *
 * @author shake
 */
@SpringBootApplication
@EntityScan(basePackageClasses = Bag.class)
public class JPAContext {

    public static final Long LONG_VALUE = 1L;
    public static final String FIXITY_VALUE =
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
    public static final String FIXITY_ALGORITHM = "SHA-256";
    public static final String PROOF = "test-proof";
    public static final String IMS_SERVICE = "test-ims-service";
    public static final String IMS_HOST = "test-ims-host";
    private static final String SCHEMA_LOCATION = "db/schema";

    public static void main(String[] args) {
        SpringApplication.run(JPAContext.class);
    }

    public static Bag createBag(String name, String creator, Depositor depositor) {
        Bag persist = new Bag();
        persist.setName(name);
        persist.setCreator(creator);
        persist.setSize(LONG_VALUE);
        persist.setDepositor(depositor);
        persist.setTotalFiles(LONG_VALUE);
        persist.setStatus(BagStatus.DEPOSITED);
        return persist;
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
        Flyway fly = Flyway.configure().dataSource(dataSource)
                .locations(SCHEMA_LOCATION)
                .load();
        fly.clean();
        fly.migrate();
        return fly;
    }

}
