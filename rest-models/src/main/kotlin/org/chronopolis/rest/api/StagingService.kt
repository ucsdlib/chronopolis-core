package org.chronopolis.rest.api

import org.chronopolis.rest.api.Paths.STAGING_ROOT
import org.chronopolis.rest.models.Fixity
import org.chronopolis.rest.models.StagingStorage
import org.chronopolis.rest.models.create.FixityCreate
import org.chronopolis.rest.models.update.ActiveToggle
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path

/**
 * Access StagingStorage information for a Bag
 *
 * @author shake
 */
interface StagingService {

    /**
     * Retrieve a StagingStorage area for a bag by its type
     *
     * @param bag  the id of the bag
     * @param type the type of storage
     * @return the StagingStorage entity
     */
    @GET("$STAGING_ROOT/storage/{type}")
    fun getStorageForBag(@Path("id") bag: Long, @Path("type") type: String): Call<StagingStorage>

    /**
     * Toggle a StagingStorage to be active/inactive for a bag
     *
     * @param bag  the id of the bag
     * @param type the type of storage
     * @return the StagingStorage entity
     */
    @PUT("$STAGING_ROOT/storage/{type}")
    fun toggleStorage(@Path("id") bag: Long, @Path("type") type: String,
                      @Body toggle: ActiveToggle): Call<StagingStorage>

    /**
     * Get all fixity information associated with a StagingStorage entity
     * of a bag
     *
     * @param bag  the id of the bag
     * @param type the type of storage
     * @return the fixities associated with the Staging entity
     */
    @GET("$STAGING_ROOT/storage/{type}/fixity")
    fun getFixityForBag(@Path("id") bag: Long, @Path("type") type: String): Call<Set<Fixity>>

    /**
     * Get the fixity information for an algorithm associated with the
     * StagingStorage entity of a bag. Allows for querying with a response
     * which is only a single value.
     *
     * @param bag       the id of the bag
     * @param type      the type of storage
     * @param algorithm the algorithm
     * @return the fixity entity
     */
    @GET("$STAGING_ROOT/storage/{type}/fixity/{alg}")
    fun getFixityForBag(@Path("id") bag: Long,
                        @Path("type") type: String,
                        @Path("alg") algorithm: String): Call<Fixity>

    /**
     * Associate fixity information with the StagingStorage entity of a bag
     *
     * @param bag  the id of the bag
     * @param type the type of storage
     * @param body the fixity information to associate
     * @return the newly created fixity entity
     */
    @PUT("$STAGING_ROOT/storage/{type}/fixity")
    fun createFixityForBag(@Path("id") bag: Long,
                           @Path("type") type: String,
                           @Body body: FixityCreate): Call<Fixity>


}