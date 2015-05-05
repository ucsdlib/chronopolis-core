package org.chronopolis.ingest.service;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
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

    @Autowired
    ApplicationContext context;

    @Override
    public void runServer() {
        boolean done = false;
        System.out.println("Enter 'q' to quit");
        while (!done) {
            if ("q".equalsIgnoreCase(readLine())) {
                done = true;
            }
        }

        SpringApplication.exit(context);
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
