package org.chronopolis.tokenize.filter;

import com.google.common.collect.ImmutableMap;
import org.chronopolis.rest.api.TokenService;
import org.chronopolis.rest.models.AceToken;
import org.chronopolis.tokenize.ManifestEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Call;
import retrofit2.Response;

import java.util.function.Predicate;

/**
 * Basic TokenFilter to see if a token exists based on its path
 *
 * @author shake
 */
public class HttpFilter implements Predicate<ManifestEntry> {

    private final Logger log = LoggerFactory.getLogger(HttpFilter.class);

    private Long bagId;
    private TokenService api;

    public HttpFilter(Long bagId, TokenService api) {
        this.bagId = bagId;
        this.api = api;
    }

    /**
     * Check against a Chronopolis Token API to see if a Bag contains a given Token for a
     * ManifestEntry. This will determine if a ManifestEntry should be processed or not.
     * <p>
     * Since the test is to see if the ManifestEntry should be processed, the return values are as
     * follows:
     * true -> ResponseCode is 200 (Ok) and ResponseBody size is 0
     * false -> ResponseCode is not 200 or an Exception is thrown communicating with the API
     *
     * @param manifestEntry the ManifestEntry containing the path to check
     * @return the result of the test
     */
    @Override
    public boolean test(ManifestEntry manifestEntry) {
        boolean processEntry;
        String path = manifestEntry.getPath();
        try {
            Call<Iterable<AceToken>> tokens = api.getBagTokens(
                    bagId,
                    ImmutableMap.of("filename", path.trim()));
            Response<Iterable<AceToken>> response = tokens.execute();
            // errr uhh figure this out yea
            processEntry = response.code() == 200 && !response.body().iterator().hasNext();
            log.trace("{} token exists? {}", path, !processEntry);
        } catch (Exception e) {
            String identifier = String.valueOf(bagId) + "/" + path;
            log.error("[{}] error communicating with ingest server, avoiding tokenization",
                    identifier, e);
            processEntry = false;
        }

        return processEntry;
    }
}
