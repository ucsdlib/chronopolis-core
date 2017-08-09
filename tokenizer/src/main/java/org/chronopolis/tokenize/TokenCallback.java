package org.chronopolis.tokenize;

import edu.umiacs.ace.exception.StatusCode;
import edu.umiacs.ace.ims.api.IMSUtil;
import edu.umiacs.ace.ims.api.RequestBatchCallback;
import edu.umiacs.ace.ims.ws.TokenRequest;
import edu.umiacs.ace.ims.ws.TokenResponse;
import org.chronopolis.common.ace.Tokenizer;
import org.chronopolis.rest.api.TokenAPI;
import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.models.AceTokenModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.List;

/**
 * Callback for our {@link Tokenizer}. When receiving tokens from the IMS, we want to
 * push them to the Ingest Server.
 *
 * Created by shake on 2/5/15.
 */
public class TokenCallback implements RequestBatchCallback {
    private final Logger log = LoggerFactory.getLogger(TokenCallback.class);

    private Bag bag;
    private TokenAPI tokens;

    public TokenCallback(Bag bag, TokenAPI tokens) {
        this.bag = bag;
        this.tokens = tokens;
    }

    @Override
    public void tokensReceived(final List<TokenRequest> requests,
                               final List<TokenResponse> responses) {
        for (TokenResponse tr : responses) {
            if (tr.getStatusCode() == StatusCode.SUCCESS) {
                log.trace("Success for token {}", tr.getName());

                AceTokenModel model = new AceTokenModel()
                        .setAlgorithm(tr.getDigestService())
                        .setFilename(tr.getName())
                        .setProof(IMSUtil.formatProof(tr))
                        .setImsService(tr.getTokenClassName())
                        .setRound(tr.getRoundId())
                        .setCreateDate(tr.getTimestamp().toGregorianCalendar().toZonedDateTime());

                Call<AceTokenModel> token = tokens.createToken(bag.getId(), model);
                token.enqueue(new Callback<AceTokenModel>() {
                    @Override
                    public void onResponse(Call<AceTokenModel> call, Response<AceTokenModel> response) {
                        if (response.isSuccessful()) {
                            log.trace("[Bag {}] Successfully registered token(path={})", bag.getId(), model.getFilename());
                        } else {
                            log.warn("[Bag {}] Unable to regsiter token(path={}) with ingest server; response={}",
                                    new Object[] {bag.getId(), model.getFilename(), response.code()});
                        }
                    }

                    @Override
                    public void onFailure(Call<AceTokenModel> call, Throwable t) {
                        log.error("[Bag {}] Error calling ingest server", bag.getId(), t);
                    }
                });
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
