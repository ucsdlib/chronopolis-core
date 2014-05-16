package org.chronopolis.common.ace;

import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.Path;

/**
 * Created by shake on 5/16/14.
 */
public interface AceService {

    @POST("/api/collection/{id}")
    void startAudit(@Path("id") long id);

    @POST("/api/collection/modify/{id}")
    void modifyCollection(@Path("id") long id, @Body GsonCollection collection);

    @POST("/api/collection/")
    void addCollection(@Body GsonCollection collection);

    @GET("/api/collection/settings/by-id/{id}")
    GsonCollection getCollectionById(@Path("id") long id);

    @GET("/api/collection/settings/by-name/{name}")
    GsonCollection getCollectionByName(@Path("name") String name);

    @GET("/api/collection/settings/by-name/{name}/{group}")
    GsonCollection getCollectionByName(@Path("name") String name, @Path("group") String group);
}
