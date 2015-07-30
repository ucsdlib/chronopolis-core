package org.chronopolis.common.dpn;

import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.Path;

import java.util.List;

/**
 * TODO: Get this from EarthScraper?
 *
 * Created by shake on 9/30/14.
 */
public interface DPNService {

    @POST("/api-v1/bag/")
    DPNBag createBag(@Body DPNBag bag);

    @GET("/api-v1/node/")
    List<DPNNode> getNodes();

    @POST("/api-v1/replicate/{id}")
    DPNReplication createReplication(@Path("id") String id, @Body DPNReplication replication);

}
