package org.chronopolis.common.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for Bag Staging areas in Chronopolis
 *
 * "storage.staging" instead of "chron.stage"?
 *
 * @author shake
 */
@ConfigurationProperties(prefix = "chron.stage.bags")
public class BagStagingProperties {

    private Posix posix = new Posix();

    public Posix getPosix() {
        return posix;
    }

    public BagStagingProperties setPosix(Posix posix) {
        this.posix = posix;
        return this;
    }
}
