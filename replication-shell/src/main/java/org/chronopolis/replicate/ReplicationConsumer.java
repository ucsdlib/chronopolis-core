package org.chronopolis.replicate;

import org.chronopolis.db.common.model.RestoreRequest;
import org.chronopolis.replicate.batch.TokenDownloadStep;
import org.chronopolis.replicate.config.JPAConfiguration;
import org.chronopolis.replicate.config.ReplicationConfig;
import org.chronopolis.replicate.config.ReplicationSettings;
import org.chronopolis.rest.api.IngestAPI;
import org.chronopolis.rest.models.Replication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.orm.jpa.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
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
        ReplicationSettings.class
}, basePackages = {
        "org.chronopolis.common.settings"
})
@EntityScan(basePackageClasses = RestoreRequest.class)
@EnableAutoConfiguration
public class ReplicationConsumer implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(ReplicationConsumer.class);

    @Autowired
    ReplicationSettings settings;

    @Autowired
    IngestAPI ingestAPI;

    private enum OPTION {
        RESTFUL_QUERY, QUIT, UNKNOWN;

        private static OPTION fromString(String text) {
            switch (text) {
                case "R":
                case "r":
                    return RESTFUL_QUERY;
                case "Q":
                case "q":
                    return QUIT;
                default:
                    return UNKNOWN;
            }
        }
    }

    private OPTION inputOption() {
        OPTION option = OPTION.UNKNOWN;
        while (option.equals(OPTION.UNKNOWN)) {
            StringBuilder sb = new StringBuilder("Enter Option: ");
            String sep = " | ";
            for (OPTION value : OPTION.values()) {
                if (!value.equals(OPTION.UNKNOWN)) {
                    sb.append(value.name());
                    sb.append(" [");
                    sb.append(value.name().charAt(0));
                    sb.append("]");
                    sb.append(sep);
                }
            }

            //The one difference, mwahhaha
            sb.replace(sb.length() - sep.length(), sb.length(), " -> ");
            System.out.println(sb.toString());
            option = OPTION.fromString(readLine().trim());
        }
        return option;
    }


    private static String readLine() {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            return reader.readLine();
        } catch (IOException ex) {
            throw new RuntimeException("Can't read from STDIN");
        }
    }

    public static void main(String [] args) {
        SpringApplication.exit(SpringApplication.run(ReplicationConsumer.class, args));
    }

    @Override
    public void run(final String... strings) throws Exception {
        log.info("{}", settings.getInboundKey());

        boolean done = false;
        while (!done) {
            OPTION option = inputOption();
            if (option.equals(OPTION.RESTFUL_QUERY)) {
                log.info("Query {} for replications");

                List<Replication> replications = ingestAPI.getReplications();
                log.debug("Found {} replications", replications.size());

                for (Replication replication : replications) {
                    log.info("Starting job for replication id {}", replication.getReplicationID());
                }

            } else if (option.equals(OPTION.QUIT)) {
                log.info("Quitting");
                done = true;
            }

        }
    }
}
