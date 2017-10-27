package org.chronopolis.tokenize.filter;

import com.google.common.collect.ImmutableMap;
import org.chronopolis.common.util.Filter;
import org.chronopolis.rest.api.TokenService;
import org.chronopolis.rest.models.AceTokenModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageImpl;
import retrofit2.Call;
import retrofit2.Response;

/**
 * Basic TokenFilter to see if a token exists based on its path
 *
 * @author shake
 */
public class HttpFilter implements Filter<String> {

    private final Logger log = LoggerFactory.getLogger(HttpFilter.class);

    private Long bagId;
    private TokenService api;

    public HttpFilter(Long bagId, TokenService api) {
        this.bagId = bagId;
        this.api = api;
    }

    @Override
    public boolean add(String path) {
        return contains(path);
    }

    /**
     * Check against a Chronopolis Token API to see if a Bag contains a given token for a path
     *
     * Returns true if the token exists, or if there are any issues communicating with the server
     * and creation needs to be skipped
     * Returns false if the token does not exist
     *
     * Note: We might want to update this interface to reflect the multiple causes of filtering,
     *       either by changing the return value to something more ?monadic?. Like an Either
     *       interface... but something to convey information about network errors/400/409/etc
     *
     * @param path the path to query on
     * @return existence of the token
     */
    @Override
    public boolean contains(String path) {
        boolean contains;
        try {
            Call<PageImpl<AceTokenModel>> tokens = api.getBagTokens(bagId, ImmutableMap.of("filename", path.trim()));
            Response<PageImpl<AceTokenModel>> response = tokens.execute();
            contains = response.isSuccessful() && response.body().getTotalElements() > 0;
            log.trace("{} token exists? {}", path, contains);
        } catch (Exception e) {
            String identifier = String.valueOf(bagId) + "/" + path;
            log.error("[{}] error communicating with ingest server, returning contains=true to avoid tokenization", identifier, e);
            contains = true;
        }

        return contains;
    }
}
