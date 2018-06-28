package org.chronopolis.tokenize.config;

import org.chronopolis.common.ace.AceConfiguration;
import org.chronopolis.common.concurrent.TrackingThreadPoolExecutor;
import org.chronopolis.common.storage.BagStagingPropertiesValidator;
import org.chronopolis.rest.api.IngestAPIProperties;
import org.chronopolis.rest.api.IngestGenerator;
import org.chronopolis.rest.api.ServiceGenerator;
import org.chronopolis.rest.models.Bag;
import org.chronopolis.tokenize.batch.ChronopolisTokenRequestBatch;
import org.chronopolis.tokenize.registrar.HttpTokenRegistrar;
import org.chronopolis.tokenize.supervisor.DefaultSupervisor;
import org.chronopolis.tokenize.supervisor.TokenWorkSupervisor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.Validator;

import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;

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
        return new TrackingThreadPoolExecutor<>(4, 8, 1, MINUTES, new LinkedBlockingQueue<>());
    }

    @Bean
    public Executor executorForBatch() {
        return new ThreadPoolExecutor(2, 2, 0, MILLISECONDS, new LinkedBlockingQueue<>());
    }

    @Bean
    public TokenWorkSupervisor tokenWorkSupervisor() {
        return new DefaultSupervisor();
    }

    @Bean
    public ChronopolisTokenRequestBatch batch(AceConfiguration configuration,
                                              TokenWorkSupervisor supervisor) {
        return new ChronopolisTokenRequestBatch(configuration, supervisor);
    }

    @Bean
    public HttpTokenRegistrar tokenRegistrar(ServiceGenerator generator,
                                             TokenWorkSupervisor supervisor,
                                             AceConfiguration configuration) {
        return new HttpTokenRegistrar(generator.tokens(), supervisor, configuration);
    }

    @Bean
    public static Validator configurationPropertiesValidator() {
        return new BagStagingPropertiesValidator();
    }

}
