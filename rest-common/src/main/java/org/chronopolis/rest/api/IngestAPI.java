package org.chronopolis.rest.api;

import org.chronopolis.rest.models.Bag;
import org.chronopolis.rest.models.FixityUpdate;
import org.chronopolis.rest.models.IngestRequest;
import org.chronopolis.rest.models.RStatusUpdate;
import org.chronopolis.rest.entities.Replication;
import org.chronopolis.rest.models.ReplicationRequest;
import org.chronopolis.rest.entities.Restoration;
import org.springframework.data.domain.PageImpl;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.QueryMap;

import java.util.List;
import java.util.Map;


/**
 * Interface laying out the RESTful functions of the ingest-server
 *
 * Created by shake on 11/25/14.
 */
public interface IngestAPI {

    // Bag methods

    @GET("api/bags")
    Call<PageImpl<Bag>> getBags(@QueryMap Map<String, Object> params);

    @POST("api/bags")
    Call<Bag> stageBag(@Body IngestRequest body);

    @GET("api/bags/{bag-id}")
    Call<Bag> getBag(@Path("bag-id") Long bagId);

    // Replication methods

    @GET("api/replications")
    Call<PageImpl<Replication>> getReplications(@QueryMap Map<String, Object> params);

    @POST("api/replications")
    Call<Replication> createReplication(@Body ReplicationRequest body);

    @GET("api/replications/{id}")
    Call<Replication> getReplication(@Path("id") Long id);

    @Deprecated
    @PUT("api/replications/{id}")
    Call<Replication> updateReplication(@Path("id") Long id, @Body Replication body);

    @PUT("api/replications/{id}/tokenstore")
    Call<Replication> updateTokenStore(@Path("id") Long id, @Body FixityUpdate update);

    @PUT("api/replications/{id}/tagmanifest")
    Call<Replication> updateTagManifest(@Path("id") Long id, @Body FixityUpdate update);

    @PUT("api/replications/{id}/status")
    Call<Replication> updateReplicationStatus(@Path("id") Long id, @Body RStatusUpdate update);

    @PUT("api/replications/{id}/failure")
    Call<Replication> failReplication(@Path("id") Long id);

    // Restore methods

    @GET("api/restorations")
    Call<List<Restoration>> getRestorations();

    @PUT("api/restorations")
    Call<Restoration> putRestoration(IngestRequest request);

    @GET("api/restorations/{id}")
    Call<Restoration> getRestoration(@Path("id") Long id);

    @PUT("api/restorations/{id}")
    Call<Restoration> acceptRestoration(@Path("id") Long id);

    @POST("api/restorations/{id}")
    Call<Restoration> updateRestoration(@Path("id") Long id, @Body Restoration restoration);

}
