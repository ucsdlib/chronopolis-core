package org.chronopolis.replicate;

import org.chronopolis.db.common.model.RestoreRequest;
import org.chronopolis.replicate.batch.ReplicationJobStarter;
import org.chronopolis.replicate.config.ReplicationConfig;
import org.chronopolis.replicate.config.ReplicationSettings;
import org.chronopolis.replicate.service.ReplicationService;
import org.chronopolis.rest.api.IngestAPI;
import org.chronopolis.rest.models.Replication;
import org.chronopolis.rest.models.ReplicationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.orm.jpa.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

/**
 * Created by shake on 2/12/14.
 */
@Component
@ComponentScan(basePackageClasses = {
        ReplicationConfig.class,
        ReplicationSettings.class,
        ReplicationService.class
}, basePackages = {
        "org.chronopolis.common.settings"
})
@EntityScan(basePackageClasses = RestoreRequest.class)
@EnableAutoConfiguration
public class ReplicationConsumer implements CommandLineRunner {

    @Autowired
    ReplicationService service;

    public static void main(String[] args) {
        SpringApplication.run(ReplicationConsumer.class, args);
    }

    @Override
    public void run(final String... strings) throws Exception {
        service.replicate();
    }

}
