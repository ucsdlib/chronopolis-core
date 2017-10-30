package org.chronopolis.tokenize;

import com.google.common.annotations.VisibleForTesting;
import edu.umiacs.ace.ims.api.IMSUtil;
import edu.umiacs.ace.ims.ws.TokenResponse;
import org.chronopolis.rest.api.TokenService;
import org.chronopolis.rest.models.AceTokenModel;
import org.chronopolis.rest.models.Bag;
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
     * - depositor/bag/path names can have all sorts of symbols, maybe wildcards are the best?
     */
    private static Pattern pattern = Pattern.compile("\\(.*?,.*?\\)::(.*)");

    private final String imsHost;
    private final TokenService tokens;
    private final ManifestEntry entry;
    private final TokenResponse response;

    public TokenRegistrar(TokenService tokens, ManifestEntry entry, TokenResponse response, String imsHost) {
        this.tokens = tokens;
        this.entry = entry;
        this.response = response;
        this.imsHost = imsHost;
    }

    /**
     * Register an AceToken with the Chronopolis Ingest Server and forward the TokenResponse
     * for the next action. Makes three attempts to register with the Ingest Server before aborting.
     * This can still be improved if we want to delay when we retry, fail fast if the response is a 409, etc.
     *
     * todo: assert fields are not null?
     * todo: We might want to return something other than the TokenResponse so that we can handle
     *       when we fail to register an AceToken with the Ingest Server
     *
     * @return The TokenResponse passed in
     */
    @Override
    public TokenResponse get() {
        String filename = getFilename();

        AceTokenModel model = new AceTokenModel()
                .setAlgorithm(response.getDigestService())
                .setFilename(filename)
                .setProof(IMSUtil.formatProof(response))
                .setImsHost(imsHost)
                .setImsService(response.getTokenClassName())
                .setRound(response.getRoundId())
                .setCreateDate(response.getTimestamp().toGregorianCalendar().toZonedDateTime());

        int attempt = 0;
        boolean success = false;
        while (attempt < 3 && !success) {
            attempt += 1;
            success = call(entry.getBag(), model);
        }

        return response;
    }

    /**
     * Make an attempt to register an AceToken with the Ingest Server
     *
     * @param bag the bag which the AceToken belongs to
     * @param model the AceToken to register
     * @return the result of the operation
     */
    private boolean call(Bag bag, AceTokenModel model) {
        boolean result = true;
        String filename = model.getFilename();
        String identifier = bag.getDepositor() + "::" + bag.getName() + "/" + filename;
        Call<AceTokenModel> call = tokens.createToken(entry.getBag().getId(), model);
        try {
            Response<AceTokenModel> execute = call.execute();
            if (execute.isSuccessful()) {
                log.debug("[Tokenizer ({})] Successfully registered token", identifier);
            } else {
                log.warn("[Tokenizer ({})] Unable to register token with Ingest Server (response code {})",
                        identifier, execute.code());
                result = false;
            }
        } catch (IOException e) {
            log.error("[Tokenizer ({})] Error communicating with Ingest Server", identifier, e);
            result = false;
        }

        return result;
    }

    /**
     * Retrieve the path to a file from the "name" field of a TokenResponse
     * <p>
     * In our case we know it is of the form "(depositor,bag)::path", so we make an attempt
     * to extract it
     * <p>
     * Failure still tbd
     *
     * @return the filename associated with the TokenResponse
     */
    @VisibleForTesting
    public String getFilename() {
        Matcher matcher = pattern.matcher(response.getName());
        matcher.matches();
        return matcher.group(matcher.groupCount());
    }
}
