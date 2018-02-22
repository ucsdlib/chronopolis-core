package org.chronopolis.rest.api;

import org.chronopolis.rest.models.Bag;
import org.chronopolis.rest.models.DepositorModel;
import org.springframework.data.domain.PageImpl;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

import static org.chronopolis.rest.api.Paths.DEPOSITOR_ROOT;

/**
 * Chronopolis API Methods for interacting with a Depositor and their resources
 *
 * @author shake
 */
public interface DepositorAPI {

    /**
     * Get all depositors
     *
     * @return all depositors
     */
    @GET(DEPOSITOR_ROOT)
    Call<PageImpl<DepositorModel>> getDepositors();

    /**
     * Create a depositor
     *
     * @param depositor the depositor to create
     * @return the created depositor
     */
    @POST(DEPOSITOR_ROOT)
    Call<DepositorModel> createDepositor(@Body DepositorModel depositor);

    /**
     * Get a depositor identified by their namespace
     *
     * @param namespace the namespace of the depositor
     * @return the depositor
     */
    @GET(DEPOSITOR_ROOT + "/{namespace}")
    Call<DepositorModel> getDepositor(@Path("namespace") String namespace);

    /**
     * Get all bags for a depositor
     *
     * @param namespace the namespace of the depositor
     * @return the depositor's bags
     */
    @GET(DEPOSITOR_ROOT + "/{namespace}/bags")
    Call<PageImpl<Bag>> getDepositorBags(@Path("namespace") String namespace);

    /**
     * Get a bag for a depositor identified by its name
     *
     * @param namespace the namespace of the depositor
     * @param bagName   the name of the bag
     * @return the bag identified by the depositor and bag name
     */
    @GET(DEPOSITOR_ROOT + "/{namespace}/bags/{bagName}")
    Call<Bag> getDepositorBag(@Path("namespace") String namespace,
                              @Path("bagName") String bagName);


}
