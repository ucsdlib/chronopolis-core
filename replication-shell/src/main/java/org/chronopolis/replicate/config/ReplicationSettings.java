package org.chronopolis.replicate.config;

import org.chronopolis.amqp.RoutingKey;
import org.chronopolis.common.settings.ChronopolisSettings;
import org.springframework.stereotype.Component;

/**
 * Old replication settings to defining queue bindings and names
 *
 * Created by shake on 8/12/14.
 */
@Component
public class ReplicationSettings extends ChronopolisSettings {

    @Deprecated
    private final String broadcastQueueBinding = RoutingKey.REPLICATE_BROADCAST.asRoute();
    @Deprecated
    private String directQueueBinding;
    @Deprecated
    private String broadcastQueueName;
    @Deprecated
    private String directQueueName;

    @Deprecated
    public String getBroadcastQueueBinding() {
        return broadcastQueueBinding;
    }

    @Deprecated
    public String getDirectQueueBinding() {
        if (directQueueBinding == null) {
            directQueueBinding = "replicate.direct." + getNode();
        }
        return directQueueBinding;
    }

    @Deprecated
    public String getBroadcastQueueName() {
        if (broadcastQueueName == null) {
            broadcastQueueName = "replicate-broadcast-" + getNode();
        }
        return broadcastQueueName;
    }

    @Deprecated
    public String getDirectQueueName() {
        if (directQueueName == null) {
            directQueueName = "replicate-direct-" + getNode();
        }
        return directQueueName;
    }

    @Override
    @Deprecated
    public String getInboundKey() {
        return directQueueBinding;
    }

}
