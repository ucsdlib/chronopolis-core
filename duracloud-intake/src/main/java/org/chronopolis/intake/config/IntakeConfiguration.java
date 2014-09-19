package org.chronopolis.intake.config;

import org.chronopolis.amqp.ChronProducer;
import org.chronopolis.common.mail.MailUtil;
import org.chronopolis.common.settings.SMTPSettings;
import org.chronopolis.db.intake.StatusRepository;
import org.chronopolis.intake.duracloud.batch.SnapshotJobManager;
import org.chronopolis.intake.duracloud.batch.SnapshotProcessor;
import org.chronopolis.intake.duracloud.batch.SnapshotTasklet;
import org.chronopolis.intake.duracloud.batch.SnapshotWriter;
import org.chronopolis.intake.duracloud.config.IntakeSettings;
import org.chronopolis.intake.duracloud.model.DuracloudRequest;
import org.chronopolis.messaging.factory.MessageFactory;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
    MailUtil mailUtil(SMTPSettings smtpSettings) {
        MailUtil mailUtil = new MailUtil();
        mailUtil.setSmtpFrom(smtpSettings.getFrom());
        mailUtil.setSmtpTo(smtpSettings.getTo());
        mailUtil.setSmtpHost(smtpSettings.getHost());
        mailUtil.setSmtpSend(smtpSettings.getSend());
        return mailUtil;
    }

    @Bean
    @JobScope
    SnapshotTasklet snapshotTasklet(@Value("#{jobParameters[snapshotID]}") String snapshotID,
                                    @Value("#{jobParameters[depositor]}") String depositor,
                                    @Value("#{jobParameters[collectionName]}") String collectionName,
                                    IntakeSettings settings,
                                    ChronProducer producer,
                                    MessageFactory messageFactory) {
        return new SnapshotTasklet(snapshotID,
                depositor,
                collectionName,
                intakeSettings,
                producer,
                messageFactory);
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
                                          JobLauncher jobLauncher,
                                          SnapshotTasklet snapshotTasklet) {
        return new SnapshotJobManager(processor,
                writer,
                intakeSettings,
                statusRepository,
                jobBuilderFactory,
                stepBuilderFactory,
                jobLauncher,
                snapshotTasklet);
    }


}
