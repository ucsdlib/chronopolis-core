package org.chronopolis.replicate.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Created by shake on 2/23/15.
 */
@Component
@Profile("production")
public class DaemonService implements ReplicationService {
    private final Logger log = LoggerFactory.getLogger(DaemonService.class);

    @Autowired
    ApplicationContext context;

    @Override
    public void replicate() {
        try {
            while (true) {
                Thread.sleep(30000);
                log.trace("going back to sleep");
            }
        } catch (InterruptedException e) {
            log.info("Thread interrupted, exiting application");
            SpringApplication.exit(context);
        }

    }

}
