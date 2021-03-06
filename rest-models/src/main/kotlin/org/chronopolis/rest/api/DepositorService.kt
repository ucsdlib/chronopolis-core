package org.chronopolis.rest.api

import org.chronopolis.rest.models.Bag
import org.chronopolis.rest.models.Depositor
import org.chronopolis.rest.models.DepositorContact
import org.chronopolis.rest.models.create.DepositorContactCreate
import org.chronopolis.rest.models.create.DepositorCreate
import org.chronopolis.rest.models.delete.DepositorContactDelete
import org.chronopolis.rest.models.page.SpringPage
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.HEAD
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.QueryMap

/**
 * Chronopolis API Methods for interacting with a Depositor and their resources
 *
 * @author shake
 */
interface DepositorService {

    /**
     * Get all depositors
     *
     * @return all depositors
     */
    @GET(Paths.DEPOSITOR_ROOT)
    fun getDepositors(@QueryMap params: Map<String, String>): Call<SpringPage<Depositor>>

    /**
     * Create a depositor
     *
     * @param depositor the depositor to create
     * @return the created depositor
     */
    @POST(Paths.DEPOSITOR_ROOT)
    fun createDepositor(@Body depositor: DepositorCreate): Call<Depositor>

    /**
     * Get a depositor identified by their namespace
     *
     * @param namespace the namespace of the depositor
     * @return the depositor
     */
    @GET("${Paths.DEPOSITOR_ROOT}/{namespace}")
    fun getDepositor(@Path("namespace") namespace: String): Call<Depositor>

    /**
     * Return the headers for the call which identifies a depositor by their namespace
     *
     * @param namespace the namespace of the depositor
     * @return the ResponseHeaders with an empty Response Body
     */
    @HEAD("${Paths.DEPOSITOR_ROOT}/{namespace}")
    fun getDepositorHeaders(@Path("namespace") namespace: String): Call<Void>

    /**
     * Get all bags for a depositor
     *
     * @param namespace the namespace of the depositor
     * @return the depositor's bags
     */
    @GET("${Paths.DEPOSITOR_ROOT}/{namespace}/bags")
    fun getDepositorBags(@Path("namespace") namespace: String): Call<SpringPage<Bag>>

    /**
     * Get a bag for a depositor identified by its name
     *
     * @param namespace the namespace of the depositor
     * @param bagName   the name of the bag
     * @return the bag identified by the depositor and bag name
     */
    @GET("${Paths.DEPOSITOR_ROOT}/{namespace}/bags/{bagName}")
    fun getDepositorBag(@Path("namespace") namespace: String,
                        @Path("bagName") bagName: String): Call<Bag>

    /**
     * Add a DepositorContact to a Depositor
     *
     * @param namespace the namespace of the Depositor
     * @param create    the DepositorContact to add
     * @return the created DepositorContact
     */
    @POST("${Paths.DEPOSITOR_ROOT}/{namespace}/contacts")
    fun createContact(@Path("namespace") namespace: String,
                      @Body create: DepositorContactCreate): Call<DepositorContact>

    /**
     * Remove a DepositorContact from a Depositor
     *
     * @param namespace the namespace of the Depositor
     * @param remove    the DepositorContact to remove, identified by their email
     * @return the updated Depositor
     */
    @DELETE("${Paths.DEPOSITOR_ROOT}/{namespace}/contacts")
    fun deleteContact(@Path("namespace") namespace: String,
                      @Body remove: DepositorContactDelete): Call<Depositor>

    /**
     * Add a Node as a ReplicatingNode for a Depositor
     *
     * @param namespace the namespace of the Depositor
     * @param nodeName  the name of the Node
     * @return the updated Depositor
     */
    @POST("${Paths.DEPOSITOR_ROOT}/{namespace}/nodes/{nodeName}")
    fun addNode(@Path("namespace") namespace: String,
                @Path("nodeName") nodeName: String): Call<Depositor>

    /**
     * Remove a Node from the ReplicatingNodes for a Depositor
     *
     * @param namespace the namespace of the Depositor
     * @param nodeName  the name of the Node
     * @return the updated Depositor
     */
    @DELETE("${Paths.DEPOSITOR_ROOT}/{namespace}/nodes/{nodeName}")
    fun removeNode(@Path("namespace") namespace: String,
                   @Path("nodeName") nodeName: String): Call<Depositor>


}