package org.chronopolis.ingest.config;

import org.chronopolis.common.settings.ChronopolisSettings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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
}
