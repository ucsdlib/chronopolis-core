package org.chronopolis.rest.kot.api

import org.chronopolis.rest.kot.api.Paths.REPLICATION_ROOT
import org.chronopolis.rest.kot.models.Replication
import org.chronopolis.rest.kot.models.create.ReplicationCreate
import org.chronopolis.rest.kot.models.update.FixityUpdate
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.QueryMap

/**
 * Service to interact with the Replication API in the ingest server
 *
 * @author shake
 */
interface ReplicationService {

    /**
     * Get a replication by its id
     *
     * @param id the id of the replication
     * @return the replication
     */
    @GET("$REPLICATION_ROOT/{id}")
    fun get(@Path("id") id: Long): Call<Replication>

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
    fun get(@QueryMap params: Map<String, String>): Call<Iterable<Replication>>

    /**
     * Create a Replication request
     *
     * @param body information for which bag to create the replication for and
     *             where the replication should go
     * @return the newly created replication
     */
    @POST(REPLICATION_ROOT)
    fun create(@Body body: ReplicationCreate): Call<Replication>

    /**
     * Update the fixity value for a received token store
     *
     * @param id the id of the replication
     * @param update the new fixity value
     * @return the updated replication
     */
    @PUT("$REPLICATION_ROOT/{id}/tokenstore")
    fun updateTokenStoreFixity(@Path("id") id: Long, @Body update: FixityUpdate): Call<Replication>

    /**
     * Update the fixity value for a received tag manifest
     *
     * @param id the id of the replication
     * @param update the new fixity value
     * @return the updated replication
     */
    @PUT("$REPLICATION_ROOT/{id}/tagmanifest")
    fun updateTagManifestFixity(@Path("id") id: Long, @Body update: FixityUpdate): Call<Replication>

}