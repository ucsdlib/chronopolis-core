package org.chronopolis.ingest;

import org.junit.BeforeClass;

/**
 * Created by shake on 3/26/15.
 */
public class IngestTest {

    @BeforeClass
    public static void init() {
        System.setProperty("spring.datasource.url", "jdbc:hsqldb:mem:memdb");
        System.setProperty("spring.datasource.initialize", "true");
    }

}
