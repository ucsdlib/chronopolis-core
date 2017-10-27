package org.chronopolis.tokenize.config;

import org.chronopolis.common.ace.AceConfiguration;
import org.chronopolis.common.concurrent.TrackingThreadPoolExecutor;
import org.chronopolis.common.storage.BagStagingPropertiesValidator;
import org.chronopolis.rest.api.IngestAPIProperties;
import org.chronopolis.rest.api.IngestGenerator;
import org.chronopolis.rest.api.ServiceGenerator;
import org.chronopolis.rest.api.TokenService;
import org.chronopolis.rest.models.Bag;
import org.chronopolis.tokenize.batch.ChronopolisTokenRequestBatch;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.Validator;

import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Beans required to spawn a TokenTask component
 *
 * @author shake
 */
@Configuration
@EnableConfigurationProperties({IngestAPIProperties.class, AceConfiguration.class})
public class TokenTaskConfiguration {

    @Bean
    public ServiceGenerator generator(IngestAPIProperties properties) {
        return new IngestGenerator(properties);
    }

    @Bean
    public TrackingThreadPoolExecutor<Bag> executor() {
        return new TrackingThreadPoolExecutor<>(4, 8, 1, TimeUnit.MINUTES, new LinkedBlockingQueue<>());
    }

    @Bean
    public Executor executorForBatch() {
        return new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
    }

    @Bean(destroyMethod = "close")
    public ChronopolisTokenRequestBatch batch(Executor executorForBatch, AceConfiguration configuration, TokenService tokens) {
        ChronopolisTokenRequestBatch batch = new ChronopolisTokenRequestBatch(configuration, tokens);
        executorForBatch.execute(batch);
        return batch;
    }

    @Bean
    public static Validator configurationPropertiesValidator() {
        return new BagStagingPropertiesValidator();
    }

}
