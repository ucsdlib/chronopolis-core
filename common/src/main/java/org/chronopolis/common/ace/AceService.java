package org.chronopolis.common.ace;

import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

import java.util.Map;

/**
 * Interface to interact with ACE
 *
 * Created by shake on 5/16/14.
 */
public interface AceService {

    @POST("rest/collection/audit/{id}")
    Call<Void> startAudit(@Path("id") long id, @Query("corrupt") Boolean corrupt);

    @POST("rest/collection/")
    Call<Map<String, Long>> addCollection(@Body GsonCollection collection);

    @GET("rest/collection/settings/by-id/{id}")
    Call<GsonCollection> getCollectionById(@Path("id") long id);

    @GET("rest/collection/settings/by-name/{name}")
    Call<GsonCollection> getCollectionByName(@Path("name") String name);

    @GET("rest/collection/settings/by-name/{name}/{group}")
    Call<GsonCollection> getCollectionByName(@Path("name") String name,
                                             @Path("group") String group);

    @Multipart
    @POST("rest/tokenstore/{id}")
    Call<Void> loadTokenStore(@Path("id") long id, @Part("file") RequestBody tokenStore);

    @POST("rest/compare/{id}")
    Call<CompareResponse> compareToCollection(@Path("id") long id, @Body CompareRequest request);

}
