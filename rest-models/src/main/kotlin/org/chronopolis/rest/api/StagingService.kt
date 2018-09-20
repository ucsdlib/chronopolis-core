package org.chronopolis.rest.api

import org.chronopolis.rest.api.Paths.STAGING_ROOT
import org.chronopolis.rest.models.Bag
import org.chronopolis.rest.models.StagingStorage
import org.chronopolis.rest.models.create.StagingCreate
import org.chronopolis.rest.models.update.ActiveToggle
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path

/**
 * Access StagingStorage resources for a Bag
 *
 * @author shake
 */
interface StagingService {

    /**
     * Retrieve a StagingStorage resource for a bag by its type
     *
     * @param bag  the id of the bag
     * @param type the type of storage
     * @return the StagingStorage resource
     */
    @GET("$STAGING_ROOT/storage/{type}")
    fun getStorageForBag(@Path("id") bag: Long, @Path("type") type: String): Call<StagingStorage>

    /**
     * Create a [StagingStorage] resource for a [Bag]
     *
     * @param bag the id of the [Bag]
     * @param type the type of data the [StagingStorage] contains (todo: move to create?)
     * @param create the form information for creating the [StagingStorage]
     * @return the created [StagingStorage] resource
     */
    @PUT("$STAGING_ROOT/storage/{type}")
    fun createStorageForBag(@Path("id") bag: Long,
                            @Path("type") type: String,
                            @Body create: StagingCreate): Call<StagingStorage>

    /**
     * Toggle a StagingStorage to be active/inactive for a bag
     *
     * @param bag  the id of the bag
     * @param type the type of storage
     * @return the StagingStorage resource
     */
    @PUT("$STAGING_ROOT/storage/{type}/active")
    fun toggleStorage(@Path("id") bag: Long,
                      @Path("type") type: String,
                      @Body toggle: ActiveToggle): Call<StagingStorage>

}