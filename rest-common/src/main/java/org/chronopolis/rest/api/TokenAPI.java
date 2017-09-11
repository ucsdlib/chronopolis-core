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

/**
 * Ingest API operations on ACE Tokens
 *
 * @author shake
 */
public interface TokenAPI {

    @GET("/api/tokens")
    Call<PageImpl<AceTokenModel>> getTokens();

    @GET("/api/tokens/{id}")
    Call<AceTokenModel> getToken(@Path("id") Long id);

    @GET("/api/bags/{bagId}/tokens")
    Call<PageImpl<AceTokenModel>> getBagTokens(@Path("bagId") Long bagId, @QueryMap Map<String, String> params);

    @POST("/api/bags/{bagId}/tokens")
    Call<AceTokenModel> createToken(@Path("bagId") Long bagId, @Body AceTokenModel model);

}
