package org.chronopolis.ingest;

import org.chronopolis.ingest.api.BagController;
import org.chronopolis.ingest.config.IngestConfig;
import org.chronopolis.ingest.controller.SiteController;
import org.chronopolis.ingest.repository.Authority;
import org.chronopolis.ingest.repository.dao.PagedDao;
import org.chronopolis.ingest.service.IngestService;
import org.chronopolis.ingest.task.TokenWriteTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.system.ApplicationPidFileWriter;
import org.springframework.context.annotation.ComponentScan;

/**
 * Moo moo moo
 * Entry point for the ingest application
 *
 * @author shake
 */
@ComponentScan(basePackageClasses = {
        IngestService.class,
        BagController.class,
        SiteController.class,
        TokenWriteTask.class,
        IngestConfig.class,
        PagedDao.class
})
@EntityScan(basePackages = "org.chronopolis.rest.entities",
        basePackageClasses = Authority.class)
@EnableAutoConfiguration
public class Application implements CommandLineRunner {

    private final IngestService service;

    @Autowired
    public Application(IngestService service) {
        this.service = service;
    }

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(Application.class);
        application.addListeners(new ApplicationPidFileWriter());
        SpringApplication.exit(application.run(args));
    }

    @Override
    public void run(final String... args) throws Exception {
        service.runServer();
    }

}
