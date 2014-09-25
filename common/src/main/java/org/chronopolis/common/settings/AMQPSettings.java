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

    @Value("${amqp.exchange:chronopolis-control}")
    String exchange;

    // TODO: rename to addresses; make List<String>
    @Value("${amqp.addresses:adapt-mq.umiacs.umd.edu}")
    String addresses;


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

    public String getAddresses() {
        return addresses;
    }

    public void setAddresses(final String addresses) {
        this.addresses = addresses;
    }
}
