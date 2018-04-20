package org.chronopolis.ingest.config;

import com.google.common.collect.ImmutableList;
import org.chronopolis.common.ace.AceConfiguration;
import org.chronopolis.common.concurrent.TrackingThreadPoolExecutor;
import org.chronopolis.common.storage.BagStagingProperties;
import org.chronopolis.common.storage.BagStagingPropertiesValidator;
import org.chronopolis.ingest.repository.dao.PagedDAO;
import org.chronopolis.ingest.tokens.DatabasePredicate;
import org.chronopolis.ingest.tokens.IngestTokenRegistrar;
import org.chronopolis.rest.entities.Bag;
import org.chronopolis.tokenize.ManifestEntry;
import org.chronopolis.tokenize.batch.ChronopolisTokenRequestBatch;
import org.chronopolis.tokenize.supervisor.DefaultSupervisor;
import org.chronopolis.tokenize.supervisor.TokenWorkSupervisor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.validation.Validator;

import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

/**
 * Configuration when doing local tokenization
 *
 * @author shake
 */
@Configuration
@Profile("local-tokenizer")
@EnableConfigurationProperties({AceConfiguration.class, BagStagingProperties.class})
public class TokenizeConfig {

    @Bean
    public TokenWorkSupervisor supervisor() {
        return new DefaultSupervisor();
    }

    @Bean
    public TrackingThreadPoolExecutor<Bag> executor() {
        return new TrackingThreadPoolExecutor<>(4, 4, 0, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
    }

    @Bean
    public Collection<Predicate<ManifestEntry>> predicates(PagedDAO dao) {
        return ImmutableList.of(new DatabasePredicate(dao));
    }

    @Bean(destroyMethod = "close")
    public ChronopolisTokenRequestBatch tokenRequestBatch(AceConfiguration configuration,
                                               TokenWorkSupervisor supervisor) {
        return new ChronopolisTokenRequestBatch(configuration, supervisor);
    }

    @Bean(destroyMethod = "close")
    public IngestTokenRegistrar tokenRegistrar(PagedDAO dao, TokenWorkSupervisor supervisor) {
        return new IngestTokenRegistrar(dao, supervisor);
    }

    @Bean
    public ExecutorService lifetimefitness(ChronopolisTokenRequestBatch batch,
                                           IngestTokenRegistrar registrar) {
        ExecutorService service = Executors.newFixedThreadPool(2);
        service.submit(batch);
        service.submit(registrar);
        return service;
    }

    @Bean
    public static Validator configurationPropertiesValidator() {
        return new BagStagingPropertiesValidator();
    }



}
