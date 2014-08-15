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
    private String directQueueBinding;
    private String broadcastQueueName;
    private String directQueueName;

    public String getBroadcastQueueBinding() {
        return broadcastQueueBinding;
    }

    public String getDirectQueueBinding() {
        if (directQueueBinding == null) {
            directQueueBinding = "replicate.direct." + getNode();
        }
        return directQueueBinding;
    }

    public String getBroadcastQueueName() {
        if (broadcastQueueName == null) {
            broadcastQueueName = "replicate-broadcast-" + getNode();
        }
        return broadcastQueueName;
    }

    public String getDirectQueueName() {
        if (directQueueName == null) {
            directQueueName = "replicate-direct-" + getNode();
        }
        return directQueueName;
    }

    @Override
    public String getInboundKey() {
        return directQueueBinding;
    }

}
