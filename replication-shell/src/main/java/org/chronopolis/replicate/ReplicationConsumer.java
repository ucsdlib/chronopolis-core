package org.chronopolis.replicate;

import org.chronopolis.replicate.config.ReplicationConfig;
import org.chronopolis.replicate.scheduled.ReplicationQueryTask;
import org.chronopolis.replicate.service.ReplicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.context.ApplicationPidFileWriter;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Component;

/**
 * Main class for the replication shell
 *
 * todo we should either disable auto config and choose only the configuration we need
 *      or update the rest-common module as we don't need to pull in the jpa stuff anywhere
 *      but ingest
 *
 * Created by shake on 2/12/14.
 */
@Component
@ComponentScan(basePackageClasses = {
        ReplicationService.class,     // scan the o.c.r.service package
        ReplicationQueryTask.class,
        ReplicationConfig.class
})
@EnableAutoConfiguration(exclude = {HibernateJpaAutoConfiguration.class, DataSourceAutoConfiguration.class})
public class ReplicationConsumer implements CommandLineRunner {

    private final ReplicationService service;

    @Autowired
    public ReplicationConsumer(ReplicationService service) {
        this.service = service;
    }

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(ReplicationConsumer.class);
        app.addListeners(new ApplicationPidFileWriter());
        SpringApplication.exit(app.run(args));
    }

    @Override
    public void run(final String... strings) {
        service.replicate();
    }

}
