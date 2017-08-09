package org.chronopolis.tokenize.filter;

import com.google.common.collect.ImmutableMap;
import org.chronopolis.common.util.Filter;
import org.chronopolis.rest.api.TokenAPI;
import org.chronopolis.rest.models.AceTokenModel;
import org.springframework.data.domain.Page;
import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Basic TokenFilter to see if a token exists based on its path
 *
 * @author shake
 */
public class HttpFilter implements Filter<Path> {

    private Long bagId;
    private TokenAPI api;

    public HttpFilter(Long bagId, TokenAPI api) {
        this.bagId = bagId;
        this.api = api;
    }

    @Override
    public boolean add(Path path) {
        return false;
    }

    @Override
    public boolean contains(Path path) {
        boolean contains;
        Call<Page<AceTokenModel>> tokens = api.getBagTokens(bagId, ImmutableMap.of("filename", path.toString()));
        try {
            Response<Page<AceTokenModel>> response = tokens.execute();
            contains = response.isSuccessful() && response.body().getTotalElements() > 0;
        } catch (IOException e) {
            contains = false;
        }

        return contains;
    }
}
