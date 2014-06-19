package org.chronopolis.common.ace;

import retrofit.Callback;
import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.Multipart;
import retrofit.http.POST;
import retrofit.http.Part;
import retrofit.http.Path;
import retrofit.mime.TypedFile;

import java.util.Map;

/**
 * Created by shake on 5/16/14.
 */
public interface AceService {

    @POST("/rest/collection/audit/{id}")
    void startAudit(@Path("id") long id, Callback<Void> callback);

    @POST("/rest/collection/modify/{id}")
    void modifyCollection(@Path("id") long id, @Body GsonCollection collection, Callback<Void> callback);

    @POST("/rest/collection/")
    Map<String, Integer> addCollection(@Body GsonCollection collection);

    @GET("/rest/collection/settings/by-id/{id}")
    GsonCollection getCollectionById(@Path("id") long id);

    @GET("/rest/collection/settings/by-name/{name}")
    GsonCollection getCollectionByName(@Path("name") String name);

    @GET("/rest/collection/settings/by-name/{name}/{group}")
    GsonCollection getCollectionByName(@Path("name") String name, @Path("group") String group);

    @Multipart
    @POST("/rest/tokenstore/{id}")
    void loadTokenStore(@Path("id") long id, @Part("file") TypedFile tokenStore, Callback<Void> callback);

}
