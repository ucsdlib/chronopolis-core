package org.chronopolis.common.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Properties for the Replication Service in order to locate
 * filesystems to preserve Chronopolis data
 *
 * @author shake
 */
@ConfigurationProperties("storage.preservation")
public class PreservationProperties {

    private List<Posix> posix = new ArrayList<>();

    public List<Posix> getPosix() {
        return posix;
    }

    public PreservationProperties setPosix(List<Posix> posix) {
        this.posix = posix;
        return this;
    }
}
