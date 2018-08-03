package org.chronopolis.rest.api

import org.chronopolis.rest.api.Paths.REPAIR_ROOT
import org.chronopolis.rest.models.FulfillmentStrategy
import org.chronopolis.rest.models.Repair
import org.chronopolis.rest.models.create.RepairCreate
import org.chronopolis.rest.models.enums.AuditStatus
import org.chronopolis.rest.models.enums.RepairStatus
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.QueryMap

/**
 * Request and fulfill Repairs in Chronopolis
 *
 * @author shake
 */
interface RepairService {

    /**
     * Get a repair by its id
     *
     * @param id the id of the repair
     * @return the repair
     */
    @GET("$REPAIR_ROOT/{id}")
    fun get(@Path("id") id: Long): Call<Repair>

    /**
     * Get all repairs, optionally filtered on query parameters
     *
     * available parameters:
     * - to
     * - status
     * - cleaned
     * - replaced
     * - validated
     * - requester
     *
     * @return all repairs
     */
    @GET(REPAIR_ROOT)
    fun get(@QueryMap parameters: Map<String, String>): Call<Iterable<Repair>>

    /**
     * Create a Repair request in the Ingest API
     *
     * @param body the information about the repair to request
     * @return the newly created repair
     */
    @POST("$REPAIR_ROOT/{id}")
    fun create(body: RepairCreate): Call<Repair>

    /**
     * Offer to fulfill a Repair request
     *
     * @param id the id of the repair to fulfill
     * @return the updated repair
     */
    @POST("$REPAIR_ROOT/{id}/fulfill")
    fun fulfill(id: Long): Call<Repair>

    /**
     * Update a repair with fulfillment information in order for
     * a requesting node to complete the repair
     *
     * @param id the id of the repair
     * @param body the fulfillment information
     * @return the updated repair
     */
    @PUT("$REPAIR_ROOT/{id}/ready")
    fun ready(id: Long, body: FulfillmentStrategy): Call<Repair>

    /**
     * Complete a repair
     *
     * @param id the id of the repair
     * @return the updated repair
     */
    @PUT("$REPAIR_ROOT/{id}/complete")
    fun complete(id: Long): Call<Repair>

    /**
     * Update a repair with the status of an ongoing ACE Audit
     *
     * @param id the id of the repair
     * @param body the status to update with
     * @return the updated repair
     */
    @PUT("$REPAIR_ROOT/{id}/audit")
    fun auditing(id: Long, body: AuditStatus): Call<Repair>

    /**
     * Mark a repair showing that the pull fulfillment data has been
     * cleaned from the repairing node's point of view
     *
     * @param id the id of the repair
     * @return the updated repair
     */
    @PUT("$REPAIR_ROOT/{id}/cleaned")
    fun cleaned(id: Long): Call<Repair>

    /**
     * Mark that a repair has successfully replaced corrupt data
     * with the pulled fulfillment data
     *
     * @param id the id of the replications
     * @param body the status of the audit
     * @return the updated repair
     */
    @PUT("$REPAIR_ROOT/{id}/replaced")
    fun replaced(id: Long, body: AuditStatus): Call<Repair>

    /**
     * Update the fulfillment status of a repair
     *
     * @param id the id of the repair
     * @param body the status to update to
     * @return the updated repair
     */
    @PUT("$REPAIR_ROOT/{id}/status")
    fun fulfillmentUpdate(id: Long, body: RepairStatus): Call<Repair>

    /**
     * Mark that the pulled fulfillment data has successfully been
     * validated against known fixity information
     *
     * @param id the id of the repair
     * @return the updated repair
     */
    @PUT("$REPAIR_ROOT/{id}/validated")
    fun validate(id: Long): Call<Repair>

}