package org.chronopolis.ingest;

import org.chronopolis.common.settings.AceSettings;
import org.chronopolis.ingest.api.StagingController;
import org.chronopolis.ingest.repository.Authority;
import org.chronopolis.ingest.task.BagInitializer;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.orm.jpa.EntityScan;
import org.springframework.context.annotation.ComponentScan;

@ComponentScan(basePackageClasses = {
        AceSettings.class,
        IngestSettings.class,
        StagingController.class,
        BagInitializer.class
})
@EntityScan(basePackages = "org.chronopolis.rest.entities", basePackageClasses = Authority.class)
@EnableAutoConfiguration
public class TestApplication implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.exit(SpringApplication.run(TestApplication.class));
    }

    @Override
    public void run(final String... args) throws Exception {

    }
}