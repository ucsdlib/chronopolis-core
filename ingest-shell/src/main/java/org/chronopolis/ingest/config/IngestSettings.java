package org.chronopolis.ingest.config;

import org.chronopolis.amqp.RoutingKey;
import org.chronopolis.common.settings.ChronopolisSettings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by shake on 8/8/14.
 */
@Component
public class IngestSettings extends ChronopolisSettings {

    @Value("${ingest.replication.server:chronopolis-stage.umiacs.umd.edu}")
    private String storageServer;

    @Value("${ingest.external.user:chrono}")
    private String externalUser;

    @Value("${ingest.dpn:false}")
    private Boolean dpnPush;

    // TODO: Would a set make more sense? Not that it matters much...
    private List<String> chronNodes;

    // TODO: Would these be better suited in some AMQP Settings?
    private final String broadcastQueueBinding = RoutingKey.INGEST_BROADCAST.asRoute();
    private String directQueueBinding;
    private String broadcastQueueName;
    private String directQueueName;

    // Getters + Setters

    public Boolean getDpnPush() {
        return dpnPush;
    }

    public void setDpnPush(final Boolean dpnPush) {
        this.dpnPush = dpnPush;
    }

    public String getExternalUser() {
        return externalUser;
    }

    public void setExternalUser(final String externalUser) {
        this.externalUser = externalUser;
    }

    public String getStorageServer() {
        return storageServer;
    }

    public void setStorageServer(final String storageServer) {
        this.storageServer = storageServer;
    }

    public List<String> getChronNodes() {
        return chronNodes;
    }

    // In order to load the comma separated values, we need to inject the
    // property reference here, split the string, and load that to the list
    @Value("${ingest.nodes:ncar,sdsc,umiacs}")
    public void setChronNodes(String chronNodes) {
        this.chronNodes = new ArrayList<>();
        Collections.addAll(this.chronNodes, chronNodes.split(","));
    }

    public String getBroadcastQueueName() {
        if (broadcastQueueName == null) {
            broadcastQueueName = "ingest-broadcast-" + getNode();
        }
        return broadcastQueueName;
    }

    public String getBroadcastQueueBinding() {
        return broadcastQueueBinding;
    }

    public String getDirectQueueBinding() {
        if (directQueueBinding == null) {
            directQueueBinding = "ingest.direct." + getNode();
        }
        return directQueueBinding;
    }

    public String getDirectQueueName() {
        if (directQueueName == null) {
            directQueueName = "ingest-direct-" + getNode();
        }
        return directQueueName;
    }

    @Override
    public String getInboundKey() {
        return directQueueBinding;
    }

}
