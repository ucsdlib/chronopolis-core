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
        System.setProperty("spring.datasource.data", "classpath:/data.sql");
        System.setProperty("spring.datasource.schema", "classpath:/schema.sql");
        System.setProperty("spring.jpa.hibernate.ddl-auto", "validate");
    }

}
