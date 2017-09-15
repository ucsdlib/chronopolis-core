package org.chronopolis.rest.api;

import org.chronopolis.rest.models.repair.AuditStatus;
import org.chronopolis.rest.models.repair.FulfillmentStrategy;
import org.chronopolis.rest.models.repair.Repair;
import org.chronopolis.rest.models.repair.RepairRequest;
import org.chronopolis.rest.models.repair.RepairStatus;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.QueryMap;

import java.util.Map;

import static org.chronopolis.rest.api.Paths.REPAIR_ROOT;

/**
 * @author shake
 */
public interface RepairService {

    /**
     * Get a repair by its id
     *
     * @param id the id of the repair
     * @return the repair
     */
    @GET(REPAIR_ROOT + "/{id}")
    Call<Repair> get(@Path("id") Long id);

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
     * @return
     */
    @GET(REPAIR_ROOT)
    Call<Repair> get(@QueryMap Map<String, String> parameters);

    /**
     * Create a Repair request in the Ingest API
     *
     * @param body the information about the repair to request
     * @return the newly created repair
     */
    @POST(REPAIR_ROOT + "/{id}")
    Call<Repair> create(RepairRequest body);

    /**
     * Offer to fulfill a Repair request
     *
     * @param id the id of the repair to fulfill
     * @return the updated repair
     */
    Call<Repair> fulfill(Long id);

    /**
     * Update a repair with fulfillment information in order for
     * a requesting node to complete the repair
     *
     * @param id the id of the repair
     * @param body the fulfillment information
     * @return the updated repair
     */
    Call<Repair> ready(Long id, FulfillmentStrategy body);

    /**
     * Complete a repair
     *
     * @param id the id of the repair
     * @return the updated repair
     */
    Call<Repair> complete(Long id);

    /**
     * Update a repair with the status of an ongoing ACE Audit
     *
     * @param id the id of the repair
     * @param body the status to update with
     * @return the updated repair
     */
    Call<Repair> auditing(Long id, AuditStatus body);

    /**
     * Mark a repair showing that the pull fulfillment data has been
     * cleaned from the repairing node's point of view
     *
     * @param id the id of the repair
     * @return the updated repair
     */
    Call<Repair> cleaned(Long id);

    /**
     * Mark that a repair has successfully replaced corrupt data
     * with the pulled fulfillment data
     *
     * @param id
     * @param body
     * @return
     */
    Call<Repair> replaced(Long id, AuditStatus body);

    /**
     * Update the fulfillment status of a repair
     *
     * @param id the id of the repair
     * @param body the status to update to
     * @return the updated repair
     */
    Call<Repair> fulfillmentUpdate(Long id, RepairStatus body);

    /**
     * Mark that the pulled fulfillment data has successfully been
     * validated against known fixity information
     *
     * @param id the id of the repair
     * @return the updated repair
     */
    Call<Repair> validate(Long id);

}
