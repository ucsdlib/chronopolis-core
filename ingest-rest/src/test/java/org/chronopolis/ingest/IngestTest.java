package org.chronopolis.ingest;

import org.junit.BeforeClass;

import java.net.URL;

/**
 * Class to force certain properties to be set so that our tests are done
 * in a consistent manner.
 *
 * Created by shake on 3/26/15.
 */
public class IngestTest {

    @BeforeClass
    public static void init() {
        System.setProperty("spring.datasource.url", "jdbc:hsqldb:mem:memdb");
        System.setProperty("spring.datasource.initialize", "true");
        System.setProperty("spring.datasource.data", "classpath:/data.sql");
        System.setProperty("spring.datasource.schema", "classpath:/schema.sql");
        System.setProperty("spring.jpa.hibernate.ddl-auto", "validate");
        System.setProperty("flyway.enabled", "false");
        System.setProperty("ingest.ajp.enabled", "false");
        System.setProperty("ace.am.validate", "false");

        // Get the test resources root
        URL bags = ClassLoader.getSystemClassLoader().getResource("bags");
        System.setProperty("chron.stage.bags", bags.getFile());
        System.setProperty("chron.stage.tokens", bags.getFile());

        // TODO: Play around with creating h2 specific migrations
        // System.setProperty("flyway.locations", "classpath:test/db/migration");
    }

}
