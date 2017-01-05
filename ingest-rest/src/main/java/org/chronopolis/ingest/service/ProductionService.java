package org.chronopolis.ingest.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Production service - sleep until we're interrupted at which point we shut down
 * via Spring
 *
 * Created by shake on 3/26/15.
 */
@Component
@Profile("production")
public class ProductionService implements IngestService {
    private final Logger log = LoggerFactory.getLogger(ProductionService.class);

    private final ApplicationContext context;

    @Autowired
    public ProductionService(ApplicationContext context) {
        this.context = context;
    }

    @Override
    public void runServer() {
        System.out.close();
        System.err.close();

        while(true) {
            try {
                TimeUnit.SECONDS.sleep(300);
                log.trace("sleep low, sweet chariot");
            } catch (InterruptedException e) {
                log.info("Shutting down");
                SpringApplication.exit(context);
            }
        }

    }
}
