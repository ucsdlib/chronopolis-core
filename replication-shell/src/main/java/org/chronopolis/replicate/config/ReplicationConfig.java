package org.chronopolis.replicate.config;

import okhttp3.OkHttpClient;
import org.chronopolis.common.ace.AceConfiguration;
import org.chronopolis.common.ace.AceService;
import org.chronopolis.common.mail.MailUtil;
import org.chronopolis.common.mail.SmtpProperties;
import org.chronopolis.common.storage.BucketBroker;
import org.chronopolis.common.storage.PreservationProperties;
import org.chronopolis.common.storage.PreservationPropertiesValidator;
import org.chronopolis.replicate.ReplicationProperties;
import org.chronopolis.replicate.batch.Submitter;
import org.chronopolis.rest.api.ErrorLogger;
import org.chronopolis.rest.api.IngestAPIProperties;
import org.chronopolis.rest.api.IngestGenerator;
import org.chronopolis.rest.api.ServiceGenerator;
import org.chronopolis.rest.support.OkBasicInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.Validator;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Configuration for the beans used by the replication-shell
 * <p>
 * Created by shake on 4/16/14.
 */
@Configuration
@EnableConfigurationProperties({SmtpProperties.class,
        PreservationProperties.class,
        ReplicationProperties.class,
        AceConfiguration.class})
public class ReplicationConfig {
    public final Logger log = LoggerFactory.getLogger(ReplicationConfig.class);

    @Value("${debug.retrofit:NONE}")
    public String retrofitLogLevel;

    @Value("${ace.timeout:5}")
    public Long timeout;

    /**
     * Logger to capture why errors happened in Retrofit
     *
     * @return
     */
    @Bean
    public ErrorLogger logger() {
        return new ErrorLogger();
    }

    /**
     * Retrofit adapter for interacting with the ACE REST API
     *
     * @param configuration the ACE AM configuration properties
     * @return the AceService for connecting to ACE
     */
    @Bean
    public AceService aceService(AceConfiguration configuration) {
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(new OkBasicInterceptor(configuration.getUsername(), configuration.getPassword()))
                .readTimeout(timeout, TimeUnit.MINUTES)
                .writeTimeout(timeout, TimeUnit.MINUTES)
                .build();

        Retrofit restAdapter = new Retrofit.Builder()
                .baseUrl(configuration.getAm())
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                // .setErrorHandler(logger())
                // .setLogLevel(Retrofit.LogLevel.valueOf(retrofitLogLevel))
                .build();

        return restAdapter.create(AceService.class);
    }

    /**
     * ServiceGenerator for creating services which can send requests to the Ingest REST API
     *
     * @param properties the API properties for configuration
     * @return the ServiceGenerator
     */
    @Bean
    public ServiceGenerator serviceGenerator(IngestAPIProperties properties) {
        return new IngestGenerator(properties);
    }

    /**
     * The main replication submission bean
     *
     * @param mail          the mail utility for sending success/failure notifications
     * @param ace           the service to connect to the ACE-AM REST API
     * @param configuration the configuration properties for ACE-AM
     * @param properties    the configuration for... general replication properties
     * @param generator     the ServiceGenerator to use for creating Ingest API services
     * @param broker        the BucketBroker for handling distribution of replications into Buckets
     * @return
     */
    @Bean
    public Submitter submitter(MailUtil mail,
                               AceService ace,
                               AceConfiguration configuration,
                               ReplicationProperties properties,
                               ServiceGenerator generator,
                               BucketBroker broker) {
        return new Submitter(mail, ace, broker, generator, configuration, properties, io(), http());
    }

    @Bean
    public ThreadPoolExecutor http() {
        return new ThreadPoolExecutor(4, 8, 30, TimeUnit.SECONDS, new LinkedBlockingDeque<>());
    }

    @Bean
    public ThreadPoolExecutor io() {
        return new ThreadPoolExecutor(2, 2, 30, TimeUnit.SECONDS, new LinkedBlockingDeque<>());
    }

    /**
     * Class to send email notifications regarding replications
     *
     * @param properties the SMTP configuration properties
     * @return the MailUtil helper
     */
    @Bean
    public MailUtil mailUtil(SmtpProperties properties) {
        MailUtil mailUtil = new MailUtil();
        mailUtil.setSmtpFrom(properties.getFrom());
        mailUtil.setSmtpTo(properties.getTo());
        mailUtil.setSmtpHost(properties.getHost());
        mailUtil.setSmtpSend(properties.getSend());
        return mailUtil;
    }

    /**
     * Our BucketBroker to determine placement of Replications
     *
     * @param preservationProperties the properties containing our Storage Spaces
     * @return the BucketBroker
     */
    @Bean
    public BucketBroker bucketBroker(PreservationProperties preservationProperties) {
        return BucketBroker.fromProperties(preservationProperties);
    }

    /**
     * Validator to make sure the PreservationProperties contain storage areas exist, can be
     * read from, and can be written to.
     *
     * @return the PreservationPropertiesValidator
     */
    @Bean
    static Validator configurationPropertiesValidator() {
        return new PreservationPropertiesValidator();
    }

}
