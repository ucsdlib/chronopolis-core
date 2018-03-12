package org.chronopolis.rest.api;

import org.chronopolis.rest.entities.DepositorContact;
import org.chronopolis.rest.models.Bag;
import org.chronopolis.rest.models.DepositorContactCreate;
import org.chronopolis.rest.models.DepositorContactRemove;
import org.chronopolis.rest.models.DepositorCreate;
import org.chronopolis.rest.models.DepositorModel;
import org.springframework.data.domain.PageImpl;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
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
    Call<DepositorModel> createDepositor(@Body DepositorCreate depositor);

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

    /**
     * Add a DepositorContact to a Depositor
     *
     * @param namespace the namespace of the Depositor
     * @param create    the DepositorContact to add
     * @return the created DepositorContact
     */
    @POST(DEPOSITOR_ROOT + "/{namespace}/contacts")
    Call<DepositorContact> createContact(@Path("namespace") String namespace,
                                         @Body DepositorContactCreate create);

    /**
     * Remove a DepositorContact from a Depositor
     *
     * @param namespace the namespace of the Depositor
     * @param remove    the DepositorContact to remove, identified by their email
     * @return the updated DepositorModel
     */
    @DELETE(DEPOSITOR_ROOT + "/{namespace}/contacts/{contactEmail}")
    Call<DepositorModel> deleteContact(@Path("namespace") String namespace,
                                       @Body DepositorContactRemove remove);

    /**
     * Add a Node as a ReplicatingNode for a Depositor
     *
     * @param namespace the namespace of the Depositor
     * @param nodeName  the name of the Node
     * @return the updated DepositorModel
     */
    @POST(DEPOSITOR_ROOT + "/{namespace}/nodes/{nodeName}")
    Call<DepositorModel> addNode(@Path("namespace") String namespace,
                                 @Path("nodeName") String nodeName);

    /**
     * Remove a Node from the ReplicatingNodes for a Depositor
     *
     * @param namespace the namespace of the Depositor
     * @param nodeName  the name of the Node
     * @return the updated DepositorModel
     */
    @DELETE(DEPOSITOR_ROOT + "/{namespace}/nodes/{nodeName}")
    Call<DepositorModel> removeNode(@Path("namespace") String namespace,
                                    @Path("nodeName") String nodeName);
}
