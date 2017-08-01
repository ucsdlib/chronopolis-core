package org.chronopolis.ingest;

import org.chronopolis.ingest.api.BagController;
import org.chronopolis.ingest.controller.SiteController;
import org.chronopolis.ingest.repository.Authority;
import org.chronopolis.ingest.service.IngestService;
import org.chronopolis.ingest.task.TokenTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;

/**
 * Moo moo moo
 *
 * Created by shake on 11/6/14.
 */
@ComponentScan(basePackageClasses = {
        IngestService.class,
        BagController.class,
        SiteController.class,
        TokenTask.class
})
@EnableConfigurationProperties
@EntityScan(basePackages = "org.chronopolis.rest.entities", basePackageClasses = Authority.class)
@EnableAutoConfiguration
public class Application implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(Application.class);

    private final IngestService service;

    @Autowired
    public Application(IngestService service) {
        this.service = service;
    }

    public static void main(String[] args) {
        log.debug("Started with args: {}", args);
        SpringApplication.exit(SpringApplication.run(Application.class, args));
    }

    @Override
    public void run(final String... args) throws Exception {
        service.runServer();
    }

}
