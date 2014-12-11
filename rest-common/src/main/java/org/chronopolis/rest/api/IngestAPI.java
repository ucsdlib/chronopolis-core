package org.chronopolis.rest.api;

import org.chronopolis.rest.models.Bag;
import org.chronopolis.rest.models.IngestRequest;
import org.chronopolis.rest.models.Replication;
import org.chronopolis.rest.models.ReplicationRequest;
import org.chronopolis.rest.models.ReplicationStatus;
import org.chronopolis.rest.models.Restoration;
import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.PUT;
import retrofit.http.Path;
import retrofit.http.Query;

import java.util.List;


/**
 * Created by shake on 11/25/14.
 */
public interface IngestAPI {

    // Bag methods

    @GET("/api/bags")
    List<Bag> getBags();

    @PUT("/api/bags")
    Bag putBag(@Body IngestRequest body);

    @GET("/api/bags/{bag-id}")
    Bag getBag(@Path("bag-id") Long bagId);

    // Replication methods

    @GET("/api/staging/replications")
    List<Replication> getReplications(@Query("status") ReplicationStatus status);

    @PUT("/api/staging/replications")
    Replication putReplication(@Body ReplicationRequest body);

    @GET("/api/staging/replications/{id}")
    Replication getReplication(@Path("id") Long id);

    @POST("/api/staging/replications/{id}")
    Replication updateReplication(@Path("id") Long id, @Body Replication body);

    // Restore methods

    @GET("/api/restorations")
    List<Restoration> getRestorations();

    @PUT("/api/restorations")
    Restoration putRestoration(IngestRequest request);

    @GET("/api/restorations/{id}")
    Restoration getRestoration(@Path("id") Long id);

    @PUT("/api/restorations/{id}")
    Restoration acceptRestoration(@Path("id") Long id);

    @POST("/api/restorations/{id}")
    Restoration updateRestoration(@Path("id") Long id, @Body Restoration restoration);

}
