package org.chronopolis.intake.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Created by shake on 8/6/14.
 */
@Component
public class AMQPSettings {

    @Value("${amqp.virtual.host:chronopolis}")
    private String virtualHost;

    @Value("${amqp.exchange:chronopolis-control}")
    private String exchange;

    @Value("${amqp.address:adapt-mq.umiacs.umd.edu}")
    private String address;


    public String getAddress() {
        return address;
    }

    public void setAddress(final String address) {
        this.address = address;
    }

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
}
