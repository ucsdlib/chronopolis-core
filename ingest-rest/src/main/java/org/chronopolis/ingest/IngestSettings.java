package org.chronopolis.ingest;

import org.chronopolis.common.settings.ChronopolisSettings;
import org.springframework.stereotype.Component;

/**
 * Created by shake on 11/6/14.
 */
@Component
public class IngestSettings extends ChronopolisSettings {
    public String getExternalUser() {
        return "chrono";
    }

    public String getStorageServer() {
        return "chronopolis-stage.umiacs.umd.edu";
    }
}
