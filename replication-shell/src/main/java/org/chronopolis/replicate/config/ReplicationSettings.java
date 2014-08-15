package org.chronopolis.replicate.config;

import org.chronopolis.amqp.RoutingKey;
import org.chronopolis.common.settings.ChronopolisSettings;
import org.springframework.stereotype.Component;

/**
 * Created by shake on 8/12/14.
 */
@Component
public class ReplicationSettings extends ChronopolisSettings {

    private final String broadcastQueueBinding = RoutingKey.REPLICATE_BROADCAST.asRoute();
    private String directQueueBinding = "replicate-" + getNode() + "-inbound";
    private String broadcastQueueName = "replicate.broadcast." + getNode();
    private String directQueueName = "replicate.direct." + getNode();

    public String getBroadcastQueueBinding() {
        return broadcastQueueBinding;
    }

    public String getDirectQueueBinding() {
        return directQueueBinding;
    }

    public String getBroadcastQueueName() {
        return broadcastQueueName;
    }

    public String getDirectQueueName() {
        return directQueueName;
    }
}
