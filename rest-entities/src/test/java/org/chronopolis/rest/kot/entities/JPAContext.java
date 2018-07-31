package org.chronopolis.rest.kot.entities;

import com.opentable.db.postgres.embedded.FlywayPreparer;
import com.opentable.db.postgres.embedded.PreparedDbProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;
import java.sql.SQLException;

/**
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
    public DataSource embeddedDataSource() throws SQLException {
        FlywayPreparer preparer = FlywayPreparer.forClasspathLocation(SCHEMA_LOCATION);
        PreparedDbProvider provider = PreparedDbProvider.forPreparer(preparer);
        return provider.createDataSource();
    }
}
