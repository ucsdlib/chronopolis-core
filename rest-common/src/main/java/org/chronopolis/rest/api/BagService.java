package org.chronopolis.rest.api;

import org.chronopolis.rest.models.Bag;
import org.chronopolis.rest.models.IngestRequest;
import org.springframework.data.domain.PageImpl;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.QueryMap;

import java.util.Map;

import static org.chronopolis.rest.api.Paths.BAG_ROOT;

/**
 * Service to interact with the Ingest server Bag API
 *
 * @author shake
 */
public interface BagService {

    /**
     * Get a bag by its id
     *
     * @param bag the id of the bag
     * @return the bag
     */
    @GET(BAG_ROOT + "/{id}")
    Call<Bag> get(@Path("id") Long bag);

    /**
     * Get all bags from the ingest server, optionally using
     * query parameters to filter the results
     *
     * available query parameters:
     * - createdAfter
     * - createdBefore
     * - updatedAfter
     * - updatedBefore
     * - depositor
     * - name
     * - status
     * @param params the query parameters to filter on
     * @return all bags matching the query
     */
    @GET(BAG_ROOT)
    Call<PageImpl<Bag>> get(@QueryMap Map<String, Object> params);

    /**
     * Deposit a bag to the ingest server
     *
     * @param body the request containing information about the bag
     * @return the newly created Bag
     */
    @POST(BAG_ROOT)
    Call<Bag> deposit(@Body IngestRequest body);

}
