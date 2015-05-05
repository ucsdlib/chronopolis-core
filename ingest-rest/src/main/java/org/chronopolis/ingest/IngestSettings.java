package org.chronopolis.ingest;

import org.chronopolis.common.settings.ChronopolisSettings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Settings for our ingest-server
 *
 * Created by shake on 11/6/14.
 */
@Component
public class IngestSettings extends ChronopolisSettings {

    @Value("${ingest.replication.user:chrono}")
    private String replicationUser;

    @Value("${ingest.replication.server:chronopolis-stage.umiacs.umd.edu}")
    private String replicationServer;

    public String getReplicationUser() {
        return replicationUser;
    }

    public String getStorageServer() {
        return replicationServer;
    }

    public void setReplicationUser(final String replicationUser) {
        this.replicationUser = replicationUser;
    }

    public void setReplicationServer(final String replicationServer) {
        this.replicationServer = replicationServer;
    }
}
