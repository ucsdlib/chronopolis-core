package org.chronopolis.intake.config;

import org.chronopolis.common.ace.CredentialRequestInterceptor;
import org.chronopolis.common.mail.MailUtil;
import org.chronopolis.common.settings.IngestAPISettings;
import org.chronopolis.common.settings.SMTPSettings;
import org.chronopolis.earth.api.LocalAPI;
import org.chronopolis.intake.duracloud.PropertiesDataCollector;
import org.chronopolis.intake.duracloud.batch.SnapshotJobManager;
import org.chronopolis.intake.duracloud.batch.SnapshotTasklet;
import org.chronopolis.intake.duracloud.config.IntakeSettings;
import org.chronopolis.rest.api.ErrorLogger;
import org.chronopolis.rest.api.IngestAPI;
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
import retrofit.RestAdapter;

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
    ErrorLogger logger() {
        return new ErrorLogger();
    }

    @Bean
    IngestAPI ingestAPI(IngestAPISettings settings) {
        String endpoint = settings.getIngestEndpoints().get(0);

        // TODO: This can timeout on long polls, see SO for potential fix
        // http://stackoverflow.com/questions/24669309/how-to-increase-timeout-for-retrofit-requests-in-robospice-android
        RestAdapter adapter = new RestAdapter.Builder()
                .setEndpoint(endpoint)
                .setErrorHandler(logger())
                .setLogLevel(RestAdapter.LogLevel.FULL)
                .setRequestInterceptor(new CredentialRequestInterceptor(
                        settings.getIngestAPIUsername(),
                        settings.getIngestAPIPassword()))
                .build();

        return adapter.create(IngestAPI.class);

    }

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
                                    IngestAPI ingestAPI,
                                    LocalAPI localAPI) {
        return new SnapshotTasklet(snapshotID,
                collectionName,
                depositor,
                intakeSettings,
                ingestAPI,
                localAPI);
    }

    @Bean(destroyMethod = "destroy")
    SnapshotJobManager snapshotJobManager(JobBuilderFactory jobBuilderFactory,
                                          StepBuilderFactory stepBuilderFactory,
                                          JobLauncher jobLauncher,
                                          SnapshotTasklet snapshotTasklet,
                                          IntakeSettings settings) {
        return new SnapshotJobManager(jobBuilderFactory,
                stepBuilderFactory,
                jobLauncher,
                snapshotTasklet,
                null,
                null,
                new PropertiesDataCollector(settings));
    }


}
