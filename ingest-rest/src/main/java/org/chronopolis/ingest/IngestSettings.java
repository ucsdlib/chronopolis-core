package org.chronopolis.ingest;

import org.chronopolis.common.settings.ChronopolisSettings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Created by shake on 11/6/14.
 */
@Component
public class IngestSettings extends ChronopolisSettings {

    @Value("${ingest.external.user:chrono}")
    private String externalUser;

    @Value("${ingest.replication.server:chronopolis-stage.umiacs.umd.edu}")
    private String replicationServer;

    public String getExternalUser() {
        return externalUser;
    }

    public String getStorageServer() {
        return replicationServer;
    }

    public void setExternalUser(final String externalUser) {
        this.externalUser = externalUser;
    }

    public void setReplicationServer(final String replicationServer) {
        this.replicationServer = replicationServer;
    }
}
