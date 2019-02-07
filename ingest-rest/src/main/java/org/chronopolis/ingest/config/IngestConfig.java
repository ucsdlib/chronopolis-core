package org.chronopolis.ingest.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.module.kotlin.KotlinModule;
import org.apache.catalina.connector.Connector;
import org.chronopolis.common.concurrent.TrackingThreadPoolExecutor;
import org.chronopolis.common.storage.TokenStagingProperties;
import org.chronopolis.common.storage.TokenStagingPropertiesValidator;
import org.chronopolis.ingest.IngestProperties;
import org.chronopolis.ingest.repository.dao.BagDao;
import org.chronopolis.ingest.repository.dao.BagFileDao;
import org.chronopolis.ingest.repository.dao.PagedDao;
import org.chronopolis.ingest.repository.dao.ReplicationDao;
import org.chronopolis.ingest.repository.dao.StagingDao;
import org.chronopolis.ingest.repository.dao.TokenDao;
import org.chronopolis.ingest.support.BagFileCSVProcessor;
import org.chronopolis.rest.entities.AceToken;
import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.entities.BagFile;
import org.chronopolis.rest.entities.Replication;
import org.chronopolis.rest.entities.TokenStore;
import org.chronopolis.rest.entities.depositor.Depositor;
import org.chronopolis.rest.entities.depositor.DepositorContact;
import org.chronopolis.rest.entities.projections.CompleteBag;
import org.chronopolis.rest.entities.projections.PartialBag;
import org.chronopolis.rest.entities.projections.ReplicationView;
import org.chronopolis.rest.entities.repair.Repair;
import org.chronopolis.rest.entities.serializers.AceTokenSerializer;
import org.chronopolis.rest.entities.serializers.BagSerializer;
import org.chronopolis.rest.entities.serializers.CompleteBagSerializer;
import org.chronopolis.rest.entities.serializers.DataFileSerializer;
import org.chronopolis.rest.entities.serializers.DepositorContactSerializer;
import org.chronopolis.rest.entities.serializers.DepositorSerializer;
import org.chronopolis.rest.entities.serializers.PartialBagSerializer;
import org.chronopolis.rest.entities.serializers.RepairSerializer;
import org.chronopolis.rest.entities.serializers.ReplicationSerializer;
import org.chronopolis.rest.entities.serializers.ReplicationViewSerializer;
import org.chronopolis.rest.entities.serializers.StagingStorageSerializer;
import org.chronopolis.rest.entities.serializers.StorageRegionSerializer;
import org.chronopolis.rest.entities.storage.StagingStorage;
import org.chronopolis.rest.entities.storage.StorageRegion;
import org.chronopolis.rest.models.FulfillmentStrategy;
import org.chronopolis.rest.models.enums.FixityAlgorithm;
import org.chronopolis.rest.models.serializers.FixityAlgorithmDeserializer;
import org.chronopolis.rest.models.serializers.FixityAlgorithmSerializer;
import org.chronopolis.rest.models.serializers.FulfillmentStrategyDeserializer;
import org.chronopolis.rest.models.serializers.ZonedDateTimeDeserializer;
import org.chronopolis.rest.models.serializers.ZonedDateTimeSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.validation.Validator;

import javax.persistence.EntityManager;
import java.time.ZonedDateTime;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Configuration for the Ingest Restful Server
 * <p>
 * Created by shake on 3/3/15.
 */
@Configuration
@EnableConfigurationProperties({IngestProperties.class, TokenStagingProperties.class})
public class IngestConfig {
    private final Logger log = LoggerFactory.getLogger(IngestConfig.class);

    private final int MAX_SIZE = 6;
    private final int CORE_SIZE = 4;
    private final int KEEP_ALIVE = 30;

    @Bean
    @Primary
    public PagedDao pagedDao(EntityManager entityManager) {
        return new PagedDao(entityManager);
    }

    @Bean
    public BagDao bagDao(EntityManager entityManager) {
        return new BagDao(entityManager);
    }

    @Bean
    public ReplicationDao replicationDao(EntityManager entityManager) {
        return new ReplicationDao(entityManager);
    }

    @Bean
    public TokenDao tokenDao(EntityManager entityManager) {
        return new TokenDao(entityManager);
    }

    @Bean
    public BagFileDao bagFileDao(EntityManager entityManager) {
        return new BagFileDao(entityManager);
    }

    @Bean
    public StagingDao stagingDao(EntityManager entityManager) {
        return new StagingDao(entityManager);
    }

    @Bean
    public BagFileCSVProcessor processor(PagedDao pagedDao, IngestProperties properties) {
        return new BagFileCSVProcessor(pagedDao, properties);
    }

    @Bean(name = "tokenExecutor", destroyMethod = "destroy")
    public TrackingThreadPoolExecutor<Bag> tokenizingThreadPoolExecutor() {
        return new TrackingThreadPoolExecutor<>(CORE_SIZE, MAX_SIZE, KEEP_ALIVE, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
    }

    @Bean(name = "bagExecutor", destroyMethod = "destroy")
    public TrackingThreadPoolExecutor<Bag> bagThreadPoolExecutor() {
        return new TrackingThreadPoolExecutor<>(CORE_SIZE, MAX_SIZE, KEEP_ALIVE, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
    }

    @Bean
    public ServletWebServerFactory embeddedServletContainerFactory(IngestProperties properties) {
        IngestProperties.Ajp ajp = properties.getAjp();
        String AJP_SCHEME = "http";
        String AJP_PROTOCOL = "AJP/1.3";
        TomcatServletWebServerFactory bean = new TomcatServletWebServerFactory();

        if (ajp.isEnabled()) {
            log.info("Setting up ajp connector");
            Connector connector = new Connector(AJP_PROTOCOL);
            connector.setPort(ajp.getPort());
            connector.setSecure(false);
            connector.setAllowTrace(false);
            connector.setScheme(AJP_SCHEME);
            bean.addAdditionalTomcatConnectors(connector);
        }

        return bean;
    }

    @Bean
    public static Validator configurationPropertiesValidator() {
        return new TokenStagingPropertiesValidator();
    }

}
