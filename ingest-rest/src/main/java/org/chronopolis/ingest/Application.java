package org.chronopolis.ingest;

import com.sun.akuma.Daemon;
import org.chronopolis.ingest.api.StagingController;
import org.chronopolis.ingest.controller.SiteController;
import org.chronopolis.ingest.service.IngestService;
import org.chronopolis.ingest.task.TokenTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.orm.jpa.EntityScan;
import org.springframework.context.annotation.ComponentScan;

import java.util.Arrays;

/**
 * Created by shake on 11/6/14.
 */
@ComponentScan(basePackageClasses = {
        IngestSettings.class,
        IngestService.class,
        StagingController.class,
        SiteController.class,
        TokenTask.class
})
@EntityScan(basePackages = "org.chronopolis.rest.models")
@EnableAutoConfiguration
public class Application implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(Application.class);

    @Autowired
    IngestService service;

    public static void main(String[] args) {
        log.debug("Started with args: {}", args);
        Daemon d = new Daemon.WithoutChdir();
        try {
            if (d.isDaemonized()) {
                d.init();
            } else {
                // We never have a long list of args so I don't think we need
                // to care about performance
                // But basically only go into daemon mode if we specify
                if (Arrays.asList(args).contains("--daemonize")) {
                    d.daemonize();
                    System.exit(0);
                }
            }
        } catch (Exception e) {
            log.error("", e);
        }

        SpringApplication.run(Application.class, args);
    }

    @Override
    public void run(final String... args) throws Exception {
        service.runServer();
    }

}
