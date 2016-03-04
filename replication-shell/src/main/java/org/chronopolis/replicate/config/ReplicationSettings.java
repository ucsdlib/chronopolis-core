package org.chronopolis.replicate.config;

import org.chronopolis.common.settings.ChronopolisSettings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Old replication settings to defining queue bindings and names
 *
 * Created by shake on 8/12/14.
 */
@Component
public class ReplicationSettings extends ChronopolisSettings {

    @Value("${smtp.send-on-success:false}")
    private Boolean sendOnSuccess;

    public Boolean sendOnSuccess() {
        return sendOnSuccess;
    }

    public ReplicationSettings setSendOnSuccess(Boolean sendOnSuccess) {
        this.sendOnSuccess = sendOnSuccess;
        return this;
    }

}
