package org.chronopolis.ingest;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.net.URL;

/**
 * Class to force certain properties to be set so that our tests are done
 * in a consistent manner.
 *
 * Created by shake on 3/26/15.
 */
public class IngestTest {

    private static final String BAG_STAGE = "chron.stage.bags";
    private static final String TOKEN_STAGE = "chron.stage.tokens";
    private static final String AJP_ENABLED = "ingest.ajp.enabled";
    private static final String LOG_FILENAME = "logging.file";
    private static final String FLYWAY_ENABLED = "flyway.enabled";
    private static final String DATASOURCE_INITIALIZE = "spring.datasource.initialize";

    @BeforeClass
    public static void init() {
        System.setProperty(AJP_ENABLED, "false");
        System.setProperty(FLYWAY_ENABLED, "false");
        System.setProperty(LOG_FILENAME, "test.log");
        System.setProperty(DATASOURCE_INITIALIZE, "false");

        // Get the test resources root
        URL bags = ClassLoader.getSystemClassLoader().getResource("bags");
        System.setProperty(BAG_STAGE, bags.getFile());
        System.setProperty(TOKEN_STAGE, bags.getFile());
    }

    @AfterClass
    public static void clearProperties() {
        System.clearProperty(BAG_STAGE);
        System.clearProperty(TOKEN_STAGE);
        System.clearProperty(AJP_ENABLED);
        System.clearProperty(LOG_FILENAME);
        System.clearProperty(FLYWAY_ENABLED);
        System.clearProperty(DATASOURCE_INITIALIZE);
    }

}
