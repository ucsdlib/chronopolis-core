package org.chronopolis.replicate;

import com.sun.akuma.Daemon;
import org.chronopolis.replicate.scheduled.ReplicationQueryTask;
import org.chronopolis.replicate.service.ReplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Main class for the replication shell
 *
 * Created by shake on 2/12/14.
 */
@Component
@ComponentScan(basePackageClasses = {
        ReplicationService.class,     // scan the o.c.r.service package
        ReplicationQueryTask.class
})
@EnableAutoConfiguration
public class ReplicationConsumer implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(ReplicationConsumer.class);

    private final ReplicationService service;

    @Autowired
    public ReplicationConsumer(ReplicationService service) {
        this.service = service;
    }

    public static void main(String[] args) {
        log.debug("Started with args: {}", args);

        // Try to daemonize if necessary
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

        SpringApplication.run(ReplicationConsumer.class, args);
    }

    @Override
    public void run(final String... strings) throws Exception {
        service.replicate();
    }

}
