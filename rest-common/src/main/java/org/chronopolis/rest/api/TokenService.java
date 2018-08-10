package org.chronopolis.rest.api;

import org.chronopolis.rest.models.AceTokenModel;
import org.springframework.data.domain.PageImpl;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.QueryMap;

import java.util.Map;

import static org.chronopolis.rest.api.Paths.BAG_ROOT;
import static org.chronopolis.rest.api.Paths.TOKEN_ROOT;

/**
 * Ingest API operations on ACE Tokens
 *
 * @author shake
 */
public interface TokenService {

    @GET(TOKEN_ROOT)
    Call<PageImpl<AceTokenModel>> getTokens(@QueryMap Map<String, String> params);

    @GET(TOKEN_ROOT + "/{id}")
    Call<AceTokenModel> getToken(@Path("id") Long id);

    @GET(BAG_ROOT + "/{bagId}/tokens")
    Call<PageImpl<AceTokenModel>> getBagTokens(@Path("bagId") Long bagId, @QueryMap Map<String, String> params);

    @POST(BAG_ROOT + "/{bagId}/tokens")
    Call<AceTokenModel> createToken(@Path("bagId") Long bagId, @Body AceTokenModel model);

}
