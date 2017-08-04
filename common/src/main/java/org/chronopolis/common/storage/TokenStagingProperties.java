package org.chronopolis.common.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for Token Staging areas in Chronopolis
 *
 * @author shake
 */
@ConfigurationProperties(prefix = "chron.stage.tokens")
public class TokenStagingProperties {

    private Posix posix = new Posix();

    public Posix getPosix() {
        return posix;
    }

    public TokenStagingProperties setPosix(Posix posix) {
        this.posix = posix;
        return this;
    }
}
