/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.ingest;

import org.chronopolis.ingest.config.IngestConfiguration;
import org.chronopolis.ingest.config.IngestJPAConfiguration;
import org.chronopolis.ingest.config.IngestSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;


/**
 *
 * @author shake
 */
@Component
@ComponentScan(basePackageClasses = {
        IngestSettings.class,
        IngestJPAConfiguration.class,
        IngestConfiguration.class
}, basePackages = {
        "org.chronopolis.common.settings"
})
@EnableAutoConfiguration
public class IngestConsumer implements CommandLineRunner {
    private static final Logger LOG = LoggerFactory.getLogger(IngestConsumer.class);

    private static String readLine() {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            return reader.readLine();
        } catch (IOException ex) {
            throw new RuntimeException("Can't read from STDIN");
        }
    }

    public static void main(String [] args) {
        SpringApplication.exit(SpringApplication.run(IngestConsumer.class, args));
    }

    @Override
    public void run(final String... strings) throws Exception {
        boolean done = false;

        while (!done) {

            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                LOG.error("Interrupted {}", ex);
            }

            System.out.println("Enter 'q' to exit: ");
            if ("q".equalsIgnoreCase(readLine())) {
                LOG.info("Shutting down");
                done = true;
            }
        }
    }
}

