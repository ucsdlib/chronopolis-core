package org.chronopolis.replicate.config;

import org.chronopolis.replicate.ReplicationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;

import javax.annotation.Resource;

import static org.chronopolis.replicate.ReplicationProperties.*;

/**
 * Created by shake on 5/28/14.
 */
@Configuration
@PropertySource({"file:replication.properties"})
public class PropConfig {

    @Resource
    Environment env;

    @Bean()
    ReplicationProperties properties() {
        return new ReplicationProperties(
                env.getProperty(PROPERTIES_NODE_NAME),
                env.getProperty(PROPERTIES_STAGE),
                env.getProperty(PROPERTIES_EXCHANGE),
                env.getProperty(PROPERTIES_INBOUND_ROUTING_KEY),
                env.getProperty(PROPERTIES_BROADCAST_ROUTING_KEY),
                env.getProperty(PROPERTIES_ACE_FQDN),
                env.getProperty(PROPERTIES_ACE_PATH),
                env.getProperty(PROPERTIES_ACE_USER),
                env.getProperty(PROPERTIES_ACE_PASS),
                env.getProperty(PROPERTIES_ACE_PORT, Integer.class));
    }
}
