package org.chronopolis.common.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * Configuration for Bag Staging areas in Chronopolis
 *
 * "storage.staging" instead of "chron.stage"?
 *
 * @author shake
 */
@ConfigurationProperties(prefix = "chron.stage.bags")
public class BagStagingProperties {

    @NestedConfigurationProperty
    private Posix posix;

    public Posix getPosix() {
        return posix;
    }

    public BagStagingProperties setPosix(Posix posix) {
        this.posix = posix;
        return this;
    }
}
