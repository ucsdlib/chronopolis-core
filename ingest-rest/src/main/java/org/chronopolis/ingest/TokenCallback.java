package org.chronopolis.ingest;

import edu.umiacs.ace.exception.StatusCode;
import edu.umiacs.ace.ims.api.IMSUtil;
import edu.umiacs.ace.ims.api.RequestBatchCallback;
import edu.umiacs.ace.ims.ws.TokenRequest;
import edu.umiacs.ace.ims.ws.TokenResponse;
import org.chronopolis.common.ace.Tokenizer;
import org.chronopolis.ingest.repository.TokenRepository;
import org.chronopolis.rest.models.AceToken;
import org.chronopolis.rest.models.Bag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Callback for our {@link Tokenizer}. When receiving tokens from the IMS, we want to
 * save them to our database
 *
 * Created by shake on 2/5/15.
 */
public class TokenCallback implements RequestBatchCallback {
    private final Logger log = LoggerFactory.getLogger(TokenCallback.class);

    private TokenRepository repository;
    private Bag bag;

    public TokenCallback(TokenRepository repository, Bag bag) {
        this.repository = repository;
        this.bag = bag;
    }


    @Override
    public void tokensReceived(final List<TokenRequest> requests,
                               final List<TokenResponse> responses) {
        for (TokenResponse tr : responses) {
            if (tr.getStatusCode() == StatusCode.SUCCESS) {
                log.trace("Success for token {}", tr.getName());
                AceToken token = new AceToken(
                        bag,
                        tr.getTimestamp().toGregorianCalendar().getTime(),
                        tr.getName(),
                        IMSUtil.formatProof(tr),
                        tr.getTokenClassName(),
                        tr.getDigestService(),
                        tr.getRoundId()
                );

                // TODO: Can batch insert if needed
                repository.save(token);
            } else {
                log.error("Received error for token: {} Code {}",
                        tr.getName(), tr.getStatusCode());
            }
        }

    }

    @Override
    public void exceptionThrown(final List<TokenRequest> list, final Throwable throwable) {
        log.error("Error registering tokens", throwable);
    }

    @Override
    public void unexpectedException(final Throwable throwable) {
        log.error("Unexpected exception during token registration", throwable);

    }
}
