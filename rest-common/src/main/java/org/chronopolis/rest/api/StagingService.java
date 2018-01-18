package org.chronopolis.rest.api;

import org.chronopolis.rest.models.storage.ActiveToggle;
import org.chronopolis.rest.models.storage.Fixity;
import org.chronopolis.rest.models.storage.FixityCreate;
import org.chronopolis.rest.models.storage.StagingStorageModel;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PUT;
import retrofit2.http.Path;

import java.util.Set;

import static org.chronopolis.rest.api.Paths.STAGING_ROOT;

/**
 * Access for StagingStorage for a Bag
 *
 * @author shake
 */
public interface StagingService {

    /**
     * Retrieve a StagingStorage area for a bag by its type
     *
     * @param bag  the id of the bag
     * @param type the type of storage
     * @return the StagingStorage entity
     */
    @GET(STAGING_ROOT + "/storage/{type}")
    Call<StagingStorageModel> getStorageForBag(@Path("id") Long bag, @Path("type") String type);

    /**
     * Toggle a StagingStorage to be active/inactive for a bag
     *
     * @param bag  the id of the bag
     * @param type the type of storage
     * @return the StagingStorage entity
     */
    @PUT(STAGING_ROOT + "/storage/{type}")
    Call<StagingStorageModel> toggleStorage(@Path("id") Long bag, @Path("type") String type, @Body ActiveToggle toggle);

    /**
     * Get all fixity information associated with a StagingStorage entity
     * of a bag
     *
     * @param bag  the id of the bag
     * @param type the type of storage
     * @return the fixities associated with the Staging entity
     */
    @GET(STAGING_ROOT + "/storage/{type}/fixity")
    Call<Set<Fixity>> getFixityForBag(@Path("id") Long bag, @Path("type") String type);

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
    @GET(STAGING_ROOT + "/storage/{type}/fixity/{alg}")
    Call<Fixity> getFixityForBag(@Path("id") Long bag, @Path("type") String type, @Path("alg") String algorithm);

    /**
     * Associate fixity information with the StagingStorage entity of a bag
     *
     * @param bag  the id of the bag
     * @param type the type of storage
     * @param body the fixity information to associate
     * @return the newly created fixity entity
     */
    @PUT(STAGING_ROOT + "/storage/{type}/fixity")
    Call<Fixity> createFixityForBag(@Path("id") Long bag, @Path("type") String type, @Body FixityCreate body);

}
