package org.chronopolis.ingest.service;


import org.chronopolis.common.storage.TokenStagingProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Service for running in development mode. We want to be able to easily shutdown the server,
 * so we read stdin for input related to shutting down.
 *
 * Created by shake on 3/26/15.
 */
@Component
@Profile("development")
public class DevelopmentService implements IngestService {
    private final Logger log = LoggerFactory.getLogger(DevelopmentService.class);

    private final TokenStagingProperties properties;

    @Autowired
    public DevelopmentService(TokenStagingProperties properties) {
        this.properties = properties;
    }

    @Override
    public void runServer() {
        boolean done = false;
        printConfiguration();
        System.out.println("Enter 'q' to quit");
        while (!done) {
            if ("q".equalsIgnoreCase(readLine())) {
                done = true;
            }
        }
    }

    /**
     * print out the configuration we read in
     */
    private void printConfiguration() {
        log.info("Loaded StagingProperties:");
        log.info("  tokens.id={}", properties.getPosix().getId());
        log.info("  tokens.path={}", properties.getPosix().getPath());
    }

    private String readLine() {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            return reader.readLine();
        } catch (IOException ex) {
            throw new RuntimeException("Unable to read STDIN");
        }
    }

}
