package org.chronopolis.tokenize.mq;

import edu.umiacs.ace.ims.ws.TokenResponse;
import org.chronopolis.rest.models.Bag;

/**
 * Simple class to encapsulate fields for registering an ace token to an ingest server
 *
 * Todo: Messages should be kotlin data classes
 *
 * @author shake
 */
public class RegisterMessage {

    private Bag bag;
    // private final ManifestEntry entry;
    private TokenResponse token;

    public RegisterMessage() {
    }

    public RegisterMessage(Bag bag,
                           // @JsonProperty("entry") ManifestEntry entry,
                           TokenResponse token) {
        this.bag = bag;
        // this.entry = entry;
        this.token = token;
    }

    public Bag getBag() {
        return bag;
    }

    public TokenResponse getToken() {
        return token;
    }

    public RegisterMessage setBag(Bag bag) {
        this.bag = bag;
        return this;
    }

    public RegisterMessage setToken(TokenResponse token) {
        this.token = token;
        return this;
    }
}
