package org.chronopolis.ingest.service;

import org.chronopolis.ingest.Application;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import sun.misc.Signal;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

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

    private final ApplicationContext context;
    private final AtomicBoolean RUN = new AtomicBoolean(true);

    @Autowired
    public ProductionService(ApplicationContext context) {
        this.context = context;
    }

    @Override
    public void runServer() {
        int exitCode = 0;
        Signal.handle(new Signal("TERM"), signal -> close());

        System.out.close();
        System.err.close();

        while(RUN.get()) {
            try {
                TimeUnit.SECONDS.sleep(300);
            } catch (InterruptedException e) {
                log.info("Shutting down");
                exitCode = 42;
            } finally {
                final int finalExit = exitCode;
                SpringApplication.exit(context, () -> finalExit);
            }
        }

    }

    private void close() {
        log.info("Received sigterm");
        RUN.set(false);
    }
}
