package org.chronopolis.tokenize.batch;

import edu.umiacs.ace.ims.api.IMSService;
import edu.umiacs.ace.ims.ws.TokenRequest;
import edu.umiacs.ace.ims.ws.TokenResponse;
import org.chronopolis.common.ace.AceConfiguration;

import java.util.List;

/**
 * Delegate class which we can mock for testing. Since we only need a small subset of the
 * IMSService methods, we only implement what we need.
 *
 * @author shake
 */
public class ImsServiceWrapper {

    private final AceConfiguration.Ims configuration;

    public ImsServiceWrapper(AceConfiguration.Ims configuration) {
        this.configuration = configuration;
    }

    public AceConfiguration.Ims configuration() {
        return configuration;
    }

    public IMSService connect() {
        return IMSService.connect(configuration.getEndpoint(), configuration.getPort(), configuration.isSsl());
    }

    public List<TokenResponse> requestTokensImmediate(String tokenClass, List<TokenRequest> requests) {
        return connect().requestTokensImmediate(tokenClass, requests);
    }

}
