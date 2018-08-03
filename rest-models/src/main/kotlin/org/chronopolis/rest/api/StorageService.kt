package org.chronopolis.rest.api

import org.chronopolis.rest.api.Paths.STORAGE_ROOT
import org.chronopolis.rest.models.StorageRegion
import org.chronopolis.rest.models.create.RegionCreate
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.QueryMap

/**
 * Service to interact with the Storage API of the ingest server
 *
 * @author shake
 */
interface StorageService {

    /**
     * Get a StorageRegion by its id
     *
     * @param id the id of the StorageRegion
     * @return the StorageRegion
     */
    @GET("$STORAGE_ROOT/{id}")
    fun get(@Path("id") id: Long): Call<StorageRegion>

    /**
     * Get all StorageRegions optionally filtered on query parameters
     *
     * available parameters:
     * - type
     * - name
     * - capacityLess
     * - capacityGreater
     *
     * @param parameters the query parameters
     * @return all storage regions
     */
    @GET(STORAGE_ROOT)
    fun get(@QueryMap parameters: Map<String, String>): Call<Iterable<StorageRegion>>

    /**
     * Create a StorageRegion for monitoring in the Ingest API
     *
     * @param create information about the StorageRegion to create
     * @return the new StorageRegion
     */
    @POST(STORAGE_ROOT)
    fun create(@Body create: RegionCreate): Call<StorageRegion>


}

