package org.chronopolis.ingest.config;

import org.apache.catalina.connector.Connector;
import org.chronopolis.ingest.IngestSettings;
import org.chronopolis.ingest.TrackingThreadPoolExecutor;
import org.chronopolis.ingest.task.TokenThreadPoolExecutor;
import org.chronopolis.rest.models.Bag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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

    @Bean
    public TokenThreadPoolExecutor tokenThreadPoolExecutor() {
        return new TokenThreadPoolExecutor(4, 6, 30, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
    }

    @Bean(destroyMethod = "destroy")
    public TrackingThreadPoolExecutor<Bag> bagThreadPoolExecutor() {
        return new TrackingThreadPoolExecutor<>(4, 6, 30, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
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

}
