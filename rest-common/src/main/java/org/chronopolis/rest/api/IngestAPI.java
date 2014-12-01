package org.chronopolis.rest.api;

import org.chronopolis.rest.models.Bag;
import org.chronopolis.rest.models.IngestRequest;
import org.chronopolis.rest.models.Replication;
import org.chronopolis.rest.models.ReplicationRequest;
import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.PUT;
import retrofit.http.Path;

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
    List<Replication> getReplications();

    @PUT("/api/staging/replications")
    Replication putReplication(@Body ReplicationRequest body);

    @GET("/api/staging/replications/{id}")
    Replication getReplication(@Path("id") Long id);

    @POST("/api/staging/replications/{id}")
    Replication updateReplication(@Path("id") Long id, @Body Replication body);

}
