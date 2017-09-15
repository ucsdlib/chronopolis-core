package org.chronopolis.rest.api;

import org.chronopolis.rest.models.FixityUpdate;
import org.chronopolis.rest.models.RStatusUpdate;
import org.chronopolis.rest.models.Replication;
import org.chronopolis.rest.models.ReplicationRequest;
import org.springframework.data.domain.PageImpl;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.QueryMap;

import java.util.Map;

import static org.chronopolis.rest.api.Paths.REPLICATION_ROOT;

/**
 * Service to interact with the Replication API in the ingest server
 *
 * @author shake
 */
public interface ReplicationService {


    /**
     * Get a replication by its id
     *
     * @param id the id of the replication
     * @return the replication
     */
    @GET(REPLICATION_ROOT + "/{id}")
    Call<Replication> get(@Path("id") Long id);

    /**
     * Get all replications, optionally filtered with the query parameters
     *
     * available query parameters:
     * - createdAfter
     * - createdBefore
     * - updatedAfter
     * - updatedBefore
     * - nodeUsername
     * - status
     *
     * @param params the map of parameters to filter on
     * @return all replications which match the query
     */
    @GET(REPLICATION_ROOT)
    Call<PageImpl<Replication>> get(@QueryMap Map<String, Object> params);

    /**
     * Create a Replication request
     *
     * @param body information for which bag to create the replication for and
     *             where the replication should go
     * @return the newly created replication
     */
    @POST(REPLICATION_ROOT)
    Call<Replication> create(@Body ReplicationRequest body);

    /**
     * Update the fixity value for a received token store
     *
     * @param id the id of the replication
     * @param update the new fixity value
     * @return the updated replication
     */
    @PUT(REPLICATION_ROOT + "/{id}/tokenstore")
    Call<Replication> updateTokenStoreFixity(@Path("id") Long id, @Body FixityUpdate update);

    /**
     * Update the fixity value for a received tag manifest
     *
     * @param id the id of the replication
     * @param update the new fixity value
     * @return the updated replication
     */
    @PUT(REPLICATION_ROOT + "/{id}/tagmanifest")
    Call<Replication> updateTagManifestFixity(@Path("id") Long id, @Body FixityUpdate update);

    /**
     * Update the status of a replication
     *
     * @param id the id of the replication
     * @param update the status to set
     * @return the updated replication
     */
    @PUT(REPLICATION_ROOT + "/{id}/status")
    Call<Replication> updateStatus(@Path("id") Long id, @Body RStatusUpdate update);

    /**
     * Fail a replication
     *
     * @param id the id of the replication
     * @return the updated replication
     */
    @PUT(REPLICATION_ROOT + "/{id}/failure")
    Call<Replication> fail(@Path("id") Long id);

}
