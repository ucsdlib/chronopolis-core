package org.chronopolis.tokenize.mq.artemis.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.api.core.client.ActiveMQClient;
import org.apache.activemq.artemis.api.core.client.ServerLocator;
import org.apache.activemq.artemis.core.remoting.impl.invm.InVMConnectorFactory;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;
import org.chronopolis.rest.kot.models.serializers.ZonedDateTimeDeserializer;
import org.chronopolis.rest.kot.models.serializers.ZonedDateTimeSerializer;
import org.chronopolis.tokenize.ManifestEntry;
import org.chronopolis.tokenize.ManifestEntryDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.ZonedDateTime;

/**
 * Beans for a message queue with artemis ye
 *
 * @author shake
 */
@Configuration
public class ArtemisConfiguration {

    // Server
    @Bean(destroyMethod = "stop")
    public EmbeddedActiveMQ activeMQServer() throws Exception {
        return new EmbeddedActiveMQ().start();
    }

    @Bean(destroyMethod = "close")
    public ServerLocator serverLocator() {
        return ActiveMQClient.createServerLocatorWithoutHA(
                new TransportConfiguration(InVMConnectorFactory.class.getName()));
    }

    // Mapper

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addSerializer(ZonedDateTime.class, new ZonedDateTimeSerializer());
        module.addDeserializer(ZonedDateTime.class, new ZonedDateTimeDeserializer());
        module.addDeserializer(ManifestEntry.class, new ManifestEntryDeserializer());
        mapper.registerModule(module);
        return mapper;
    }

}
