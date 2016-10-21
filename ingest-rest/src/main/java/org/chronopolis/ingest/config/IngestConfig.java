package org.chronopolis.ingest.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.apache.catalina.connector.Connector;
import org.chronopolis.ingest.IngestSettings;
import org.chronopolis.common.concurrent.TrackingThreadPoolExecutor;
import org.chronopolis.rest.entities.Bag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.io.IOException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
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
        builder.serializerByType(ZonedDateTime.class, new JsonSerializer<ZonedDateTime>() {
            @Override
            public void serialize(ZonedDateTime localDateTime,
                                  JsonGenerator jsonGenerator,
                                  SerializerProvider serializerProvider) throws IOException {
                DateTimeFormatter fmt = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneOffset.UTC);
                jsonGenerator.writeString(fmt.format(localDateTime));
            }
        });
        builder.deserializerByType(ZonedDateTime.class, new JsonDeserializer<ZonedDateTime>() {
            @Override
            public ZonedDateTime deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
                DateTimeFormatter fmt = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneOffset.UTC);
                String text = jsonParser.getText();
                return ZonedDateTime.from(fmt.parse(text));
            }
        });
        return builder;
    }

}
