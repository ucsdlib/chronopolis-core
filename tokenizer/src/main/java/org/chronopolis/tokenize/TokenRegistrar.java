package org.chronopolis.tokenize;

import com.google.common.annotations.VisibleForTesting;
import edu.umiacs.ace.ims.api.IMSUtil;
import edu.umiacs.ace.ims.ws.TokenResponse;
import org.chronopolis.rest.api.TokenAPI;
import org.chronopolis.rest.models.AceTokenModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Runnable to register ACE Tokens with a Chronopolis Ingest Server
 *
 * @author shake
 */
public class TokenRegistrar implements Supplier<TokenResponse> {
    private final Logger log = LoggerFactory.getLogger(TokenRegistrar.class);

    /**
     * A regex Pattern to match the TokenResponse's name on
     * Some notes
     * - we should look into the regex used to see if we can refine it at all
     *   - depositor/bag/path names can have all sorts of symbols, maybe wildcards are the best?
     */
    private static Pattern pattern = Pattern.compile("\\(.*?,.*?\\)::(.*)");

    private TokenAPI tokens;
    private ManifestEntry entry;
    private TokenResponse response;

    public TokenRegistrar(TokenAPI tokens, ManifestEntry entry, TokenResponse response) {
        this.tokens = tokens;
        this.entry = entry;
        this.response = response;
    }

    /**
     * Register an AceToken with the Chronopolis Ingest Server and forward the TokenResponse
     * for the next action
     *
     * todo: We might want to return something other than the TokenResponse so that we can handle
     *       when we fail to register an AceToken with the Ingest Server
     *
     * @return The TokenResponse passed in
     */
    @Override
    public TokenResponse get() {
        String filename = getFilename();
        String bag = entry.getBag().getName();

        AceTokenModel model = new AceTokenModel()
                .setAlgorithm(response.getDigestService())
                .setFilename(filename)
                .setProof(IMSUtil.formatProof(response))
                .setImsService(response.getTokenClassName())
                .setRound(response.getRoundId())
                .setCreateDate(response.getTimestamp().toGregorianCalendar().toZonedDateTime());

        Call<AceTokenModel> call = tokens.createToken(entry.getBag().getId(), model);
        try {
            Response<AceTokenModel> execute = call.execute();
            if (execute.isSuccessful()) {
                log.debug("[Tokenizer (Bag {})] Successfully registered token for {}", bag, filename);
            } else {
                log.warn("[Tokenizer (Bag {})] Unable to register token with Ingest Server (response code {})",
                        bag, execute.code());
            }
        } catch (IOException e) {
            log.error("[Tokenizer (Bag {})] Error communicating with Ingest Server", bag, e);
        }

        return response;
    }

    /**
     * Retrieve the path to a file from the "name" field of a TokenResponse
     *
     * In our case we know it is of the form "(depositor,bag)::path", so we make an attempt
     * to extract it
     *
     * Failure still tbd
     *
     * @return the filename associated with the TokenResponse
     */
    @VisibleForTesting
    public String getFilename() {
        Matcher matcher = pattern.matcher(response.getName());
        return matcher.group(matcher.groupCount());
    }
}
