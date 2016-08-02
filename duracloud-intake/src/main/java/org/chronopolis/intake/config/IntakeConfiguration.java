package org.chronopolis.intake.config;

import okhttp3.OkHttpClient;
import org.chronopolis.common.ace.OkBasicInterceptor;
import org.chronopolis.common.mail.MailUtil;
import org.chronopolis.common.settings.IngestAPISettings;
import org.chronopolis.common.settings.SMTPSettings;
import org.chronopolis.intake.duracloud.PropertiesDataCollector;
import org.chronopolis.intake.duracloud.batch.SnapshotJobManager;
import org.chronopolis.intake.duracloud.config.IntakeSettings;
import org.chronopolis.rest.api.ErrorLogger;
import org.chronopolis.rest.api.IngestAPI;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import retrofit2.Retrofit;

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

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(new OkBasicInterceptor(
                        settings.getIngestAPIUsername(),
                        settings.getIngestAPIPassword()))
                .build();

        // TODO: This can timeout on long polls, see SO for potential fix
        // http://stackoverflow.com/questions/24669309/how-to-increase-timeout-for-retrofit-requests-in-robospice-android
        Retrofit adapter = new Retrofit.Builder()
                .baseUrl(endpoint)
                .client(client)
                // .setErrorHandler(logger())
                // .setLogLevel(RestAdapter.LogLevel.FULL)
                // .setRequestInterceptor(new CredentialRequestInterceptor(
                //         settings.getIngestAPIUsername(),
                //         settings.getIngestAPIPassword()))
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

    @Bean(destroyMethod = "destroy")
    SnapshotJobManager snapshotJobManager(JobBuilderFactory jobBuilderFactory,
                                          StepBuilderFactory stepBuilderFactory,
                                          JobLauncher jobLauncher,
                                          IntakeSettings settings) {
        return new SnapshotJobManager(jobBuilderFactory,
                stepBuilderFactory,
                jobLauncher,
                null,
                null,
                new PropertiesDataCollector(settings));
    }


}
