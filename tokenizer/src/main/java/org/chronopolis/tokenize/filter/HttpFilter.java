package org.chronopolis.tokenize.filter;

import com.google.common.collect.ImmutableMap;
import org.chronopolis.common.util.Filter;
import org.chronopolis.rest.api.TokenAPI;
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
    private TokenAPI api;

    public HttpFilter(Long bagId, TokenAPI api) {
        this.bagId = bagId;
        this.api = api;
    }

    @Override
    public boolean add(String path) {
        return contains(path);
    }

    @Override
    public boolean contains(String path) {
        boolean contains;
        // todo: depending on what this returns we'll want to throw an exception (or some datatype indicating existence && !communicationsFailure)
        Call<PageImpl<AceTokenModel>> tokens = api.getBagTokens(bagId, ImmutableMap.of("filename", path));
        try {
            Response<PageImpl<AceTokenModel>> response = tokens.execute();
            contains = response.isSuccessful() && response.body().getTotalElements() > 0;
            log.trace("{} token exists? {}", path, contains);
        } catch (Exception e) {
            contains = false;
        }

        return contains;
    }
}
