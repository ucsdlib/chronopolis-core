package org.chronopolis.tokenize.supervisor;

import edu.umiacs.ace.ims.ws.TokenResponse;

/**
 * Encapsulate some information about the Work we have to do for Tokenization
 *
 * Considering having the states advance themselves instead of relying on the setter. TBD.
 *
 * @author shake
 */
class WorkUnit {
    private State state = State.QUEUED_FOR_REGISTRATION;
    private TokenResponse response;

    public State getState() {
        return state;
    }

    public WorkUnit setState(State state) {
        this.state = state;
        return this;
    }

    public TokenResponse getResponse() {
        return response;
    }

    public WorkUnit setResponse(TokenResponse response) {
        this.response = response;
        return this;
    }

    protected enum State {
        QUEUED_FOR_REGISTRATION, QUEUED_FOR_TOKENIZATION, REQUESTING_TOKEN, REGISTERING_TOKEN
    }
}
