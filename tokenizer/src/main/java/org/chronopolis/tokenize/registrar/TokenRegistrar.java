package org.chronopolis.tokenize.registrar;

import com.google.common.annotations.VisibleForTesting;
import edu.umiacs.ace.ims.ws.TokenResponse;
import org.chronopolis.tokenize.ManifestEntry;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Interface representing the actions needed to register an AceToken with the
 * Chronopolis Ingest Service
 *
 * @author shake
 */
public interface TokenRegistrar {

    void register(Map<ManifestEntry, TokenResponse> tokenResponseMap);


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
    default String getFilename(TokenResponse response) {
        /*
         * A regex Pattern to match the TokenResponse's name on
         * Some notes
         * - we should look into the regex used to see if we can refine it at all
         * - depositor/bag/path names can have all sorts of symbols, maybe wildcards are the best?
         */
        Pattern pattern = Pattern.compile("\\(.*?,.*?\\)::(.*)");

        Matcher matcher = pattern.matcher(response.getName());
        boolean matches = matcher.matches();
        if (matches) {
            return matcher.group(matcher.groupCount());
        } else {
            throw new RuntimeException("TokenName does not match regex");
        }
    }
}
