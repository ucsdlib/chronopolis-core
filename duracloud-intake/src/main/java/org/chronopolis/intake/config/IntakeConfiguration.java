package org.chronopolis.intake.config;

import org.chronopolis.amqp.ChronProducer;
import org.chronopolis.common.mail.MailUtil;
import org.chronopolis.db.intake.StatusRepository;
import org.chronopolis.intake.duracloud.batch.SnapshotJobManager;
import org.chronopolis.intake.duracloud.batch.SnapshotProcessor;
import org.chronopolis.intake.duracloud.batch.SnapshotWriter;
import org.chronopolis.intake.duracloud.config.IntakeSettings;
import org.chronopolis.messaging.factory.MessageFactory;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Created by shake on 8/4/14.
 */
@Configuration
@EnableBatchProcessing
@EnableAutoConfiguration
public class IntakeConfiguration {

    @Autowired
    IntakeSettings intakeSettings;

    @Bean
    MailUtil mailUtil() {
        return new MailUtil();
    }

    @Bean
    SnapshotProcessor snapshotProcessor() {
        return new SnapshotProcessor(intakeSettings);
    }

    @Bean
    SnapshotWriter snapshotWriter(ChronProducer producer,
                                  MessageFactory messageFactory) {
        return new SnapshotWriter(producer, messageFactory, intakeSettings);
    }

    @Bean(destroyMethod = "destroy")
    SnapshotJobManager snapshotJobManager(StatusRepository statusRepository,
                                          SnapshotWriter writer,
                                          SnapshotProcessor processor,
                                          JobBuilderFactory jobBuilderFactory,
                                          StepBuilderFactory stepBuilderFactory,
                                          JobLauncher jobLauncher) {
        return new SnapshotJobManager(processor,
                writer,
                intakeSettings,
                statusRepository,
                jobBuilderFactory,
                stepBuilderFactory,
                jobLauncher);
    }


}
