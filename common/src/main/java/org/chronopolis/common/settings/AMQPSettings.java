package org.chronopolis.common.settings;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Created by shake on 8/13/14.
 */
@Component
public class AMQPSettings {

    @Value("${amqp.virtual.host:chronopolis}")
    String virtualHost;

    @Value("${amqp.exchange:chronopolis-core}")
    String exchange;

    @Value("${amqp.servers:adapt-mq.umiacs.umd.edu}")
    String server;


    public String getVirtualHost() {
        return virtualHost;
    }

    public void setVirtualHost(final String virtualHost) {
        this.virtualHost = virtualHost;
    }

    public String getExchange() {
        return exchange;
    }

    public void setExchange(final String exchange) {
        this.exchange = exchange;
    }

    public String getServer() {
        return server;
    }

    public void setServer(final String server) {
        this.server = server;
    }
}
