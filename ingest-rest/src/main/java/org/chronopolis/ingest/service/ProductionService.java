package org.chronopolis.ingest.service;

import org.chronopolis.ingest.Application;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Production service - sleep until we're interrupted at which point we shut down
 * via Spring
 *
 * Exiting the context is now handled via {@link Application}, so we just exit the loop here
 *
 * Created by shake on 3/26/15.
 */
@Component
@Profile("production")
public class ProductionService implements IngestService {
    private final Logger log = LoggerFactory.getLogger(ProductionService.class);

    @Autowired
    public ProductionService() {
    }

    @Override
    public void runServer() {
        boolean exit = false;
        System.out.close();
        System.err.close();

        while(!exit) {
            try {
                TimeUnit.SECONDS.sleep(300);
                log.trace("sleep low, sweet chariot");
            } catch (InterruptedException e) {
                log.info("Shutting down");
                exit = true;
            }
        }

    }
}
