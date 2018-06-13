package org.chronopolis.ingest.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.apache.catalina.connector.Connector;
import org.chronopolis.common.concurrent.TrackingThreadPoolExecutor;
import org.chronopolis.common.storage.TokenStagingProperties;
import org.chronopolis.common.storage.TokenStagingPropertiesValidator;
import org.chronopolis.ingest.IngestProperties;
import org.chronopolis.ingest.api.serializer.AceTokenSerializer;
import org.chronopolis.ingest.api.serializer.BagSerializer;
import org.chronopolis.ingest.api.serializer.DepositorContactSerializer;
import org.chronopolis.ingest.api.serializer.DepositorSerializer;
import org.chronopolis.ingest.api.serializer.RepairSerializer;
import org.chronopolis.ingest.api.serializer.ReplicationSerializer;
import org.chronopolis.ingest.api.serializer.StagingStorageSerializer;
import org.chronopolis.ingest.api.serializer.StorageRegionSerializer;
import org.chronopolis.rest.models.serializers.ZonedDateTimeDeserializer;
import org.chronopolis.rest.models.serializers.ZonedDateTimeSerializer;
import org.chronopolis.ingest.repository.BagRepository;
import org.chronopolis.ingest.repository.RepairRepository;
import org.chronopolis.ingest.repository.StorageRegionRepository;
import org.chronopolis.ingest.repository.StorageRepository;
import org.chronopolis.ingest.repository.TokenRepository;
import org.chronopolis.ingest.repository.dao.BagService;
import org.chronopolis.ingest.repository.dao.PagedDAO;
import org.chronopolis.ingest.repository.dao.SearchService;
import org.chronopolis.ingest.repository.dao.StagingService;
import org.chronopolis.ingest.repository.dao.StorageRegionService;
import org.chronopolis.rest.entities.AceToken;
import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.entities.Depositor;
import org.chronopolis.rest.entities.DepositorContact;
import org.chronopolis.rest.entities.Repair;
import org.chronopolis.rest.entities.Replication;
import org.chronopolis.rest.entities.storage.StagingStorage;
import org.chronopolis.rest.entities.storage.StorageRegion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.validation.Validator;

import javax.persistence.EntityManager;
import java.time.ZonedDateTime;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Configuration for the Ingest Restful Server
 *
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
    public PagedDAO pagedDAO(EntityManager entityManager) {
        return new PagedDAO(entityManager);
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
    public SearchService<Repair, Long, RepairRepository> repairService(RepairRepository repository) {
        return new SearchService<>(repository);
    }

    @Bean
    public BagService bagService(BagRepository repository, EntityManager entityManager) {
        return new BagService(repository, entityManager);
    }

    @Bean
    public StorageRegionService storageRegionService(StorageRegionRepository repository, EntityManager entityManager) {
        return new StorageRegionService(repository, entityManager);
    }

    @Bean
    public StagingService stagingService(StorageRepository repository, EntityManager entityManager) {
        return new StagingService(repository, entityManager);
    }

    @Bean
    public SearchService<AceToken, Long, TokenRepository> tokenService(TokenRepository repository) {
        return new SearchService<>(repository);
    }

    @Bean
    public EmbeddedServletContainerFactory embeddedServletContainerFactory(IngestProperties properties) {
        IngestProperties.Ajp ajp = properties.getAjp();
        String AJP_SCHEME = "http";
        String AJP_PROTOCOL = "AJP/1.3";
        TomcatEmbeddedServletContainerFactory bean = new TomcatEmbeddedServletContainerFactory();

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
    public Jackson2ObjectMapperBuilder jacksonBuilder() {
        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
        builder.indentOutput(true);
        builder.serializationInclusion(JsonInclude.Include.NON_NULL);
        builder.serializerByType(Bag.class, new BagSerializer());
        builder.serializerByType(Repair.class, new RepairSerializer());
        builder.serializerByType(AceToken.class, new AceTokenSerializer());
        builder.serializerByType(Depositor.class, new DepositorSerializer());
        builder.serializerByType(Replication.class, new ReplicationSerializer());
        builder.serializerByType(StorageRegion.class, new StorageRegionSerializer());
        builder.serializerByType(ZonedDateTime.class, new ZonedDateTimeSerializer());
        builder.serializerByType(StagingStorage.class, new StagingStorageSerializer());
        builder.serializerByType(DepositorContact.class, new DepositorContactSerializer());
        builder.deserializerByType(ZonedDateTime.class, new ZonedDateTimeDeserializer());
        return builder;
    }

    @Bean
    public static Validator configurationPropertiesValidator() {
        return new TokenStagingPropertiesValidator();
    }

}
