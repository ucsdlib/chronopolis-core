package org.chronopolis.rest.api;

import org.chronopolis.rest.models.Bag;
import org.chronopolis.rest.models.IngestRequest;
import org.chronopolis.rest.models.Replication;
import org.chronopolis.rest.models.ReplicationRequest;
import org.chronopolis.rest.models.ReplicationStatus;
import org.chronopolis.rest.models.Restoration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.PUT;
import retrofit.http.Path;
import retrofit.http.Query;
import retrofit.http.QueryMap;

import java.util.List;
import java.util.Map;


/**
 * Interface laying out the RESTful functions of the ingest-server
 *
 * Created by shake on 11/25/14.
 */
public interface IngestAPI {

    // Bag methods

    @GET("/api/bags")
    PageImpl<Bag> getBags(@QueryMap Map<String, Object> params);

    @POST("/api/bags")
    Bag stageBag(@Body IngestRequest body);

    @GET("/api/bags/{bag-id}")
    Bag getBag(@Path("bag-id") Long bagId);

    // Replication methods

    @GET("/api/replications")
    PageImpl<Replication> getReplications(@QueryMap Map<String, Object> params);

    @POST("/api/replications")
    Replication createReplication(@Body ReplicationRequest body);

    @GET("/api/replications/{id}")
    Replication getReplication(@Path("id") Long id);

    @PUT("/api/replications/{id}")
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
