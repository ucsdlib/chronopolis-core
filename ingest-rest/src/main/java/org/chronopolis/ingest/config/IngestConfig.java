package org.chronopolis.ingest.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.apache.catalina.connector.Connector;
import org.chronopolis.common.concurrent.TrackingThreadPoolExecutor;
import org.chronopolis.ingest.IngestSettings;
import org.chronopolis.ingest.api.serializer.BagSerializer;
import org.chronopolis.ingest.api.serializer.RepairSerializer;
import org.chronopolis.ingest.api.serializer.ReplicationSerializer;
import org.chronopolis.ingest.api.serializer.ZonedDateTimeDeserializer;
import org.chronopolis.ingest.api.serializer.ZonedDateTimeSerializer;
import org.chronopolis.ingest.repository.FulfillmentRepository;
import org.chronopolis.ingest.repository.RepairRepository;
import org.chronopolis.ingest.repository.RepairService;
import org.chronopolis.ingest.repository.SearchService;
import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.entities.Fulfillment;
import org.chronopolis.rest.entities.Repair;
import org.chronopolis.rest.entities.Replication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.time.ZonedDateTime;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Configuration for the Ingest Restful Server
 *
 * Created by shake on 3/3/15.
 */
@Configuration
public class IngestConfig {
    private final Logger log = LoggerFactory.getLogger(IngestConfig.class);

    final String AJP_PROTOCOL = "AJP/1.3";
    final String AJP_SCHEME = "http";

    /*
    @Bean
    public TokenThreadPoolExecutor tokenThreadPoolExecutor() {
        return new TokenThreadPoolExecutor(4, 6, 30, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
    }
    */

    @Bean(name = "tokenExecutor", destroyMethod = "destroy")
    public TrackingThreadPoolExecutor<Bag> tokenizingThreadPoolExecutor() {
        return new TrackingThreadPoolExecutor<>(4, 6, 30, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
    }

    @Bean(name = "bagExecutor", destroyMethod = "destroy")
    public TrackingThreadPoolExecutor<Bag> bagThreadPoolExecutor() {
        return new TrackingThreadPoolExecutor<>(4, 6, 30, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
    }

    @Bean
    public RepairService<Repair, Long, RepairRepository> repairService(RepairRepository repository) {
        return new RepairService<>(repository);
    }

    @Bean
    public SearchService<Fulfillment, Long, FulfillmentRepository> fulfillmentService(FulfillmentRepository repository) {
        return new SearchService<>(repository);
    }

    @Bean
    public EmbeddedServletContainerFactory embeddedServletContainerFactory(IngestSettings settings) {
        TomcatEmbeddedServletContainerFactory bean = new TomcatEmbeddedServletContainerFactory();

        if (settings.isAjpEnabled()) {
            log.info("Setting up ajp connector");
            Connector ajp = new Connector(AJP_PROTOCOL);
            ajp.setProtocol(AJP_PROTOCOL);
            ajp.setPort(settings.getAjpPort());
            ajp.setSecure(false);
            ajp.setAllowTrace(false);
            ajp.setScheme(AJP_SCHEME);
            bean.addAdditionalTomcatConnectors(ajp);
            // bean.
        }

        return bean;
    }

    @Bean
    public Jackson2ObjectMapperBuilder jacksonBuilder() {
        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
        builder.indentOutput(true);
        // builder.dateFormat(DateTimeFormatter.ISO_LOCAL_DATE_TIME.);
        builder.serializationInclusion(JsonInclude.Include.NON_NULL);
        builder.serializerByType(Bag.class, new BagSerializer());
        builder.serializerByType(Repair.class, new RepairSerializer());
        builder.serializerByType(Replication.class, new ReplicationSerializer());
        builder.serializerByType(ZonedDateTime.class, new ZonedDateTimeSerializer());
        builder.deserializerByType(ZonedDateTime.class, new ZonedDateTimeDeserializer());
        return builder;
    }

}
