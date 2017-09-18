package org.chronopolis.rest.api;

import org.chronopolis.rest.models.RegionCreate;
import org.chronopolis.rest.models.storage.StorageRegion;
import org.springframework.data.domain.PageImpl;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.QueryMap;

import java.util.Map;

import static org.chronopolis.rest.api.Paths.STORAGE_ROOT;

/**
 * Service to interact with the Storage API of the ingest server
 *
 * @author shake
 */
public interface StorageService {

    /**
     * Get a StorageRegion by its id
     *
     * @param id the id of the StorageRegion
     * @return the StorageRegion
     */
    @GET(STORAGE_ROOT + "/{id}")
    Call<StorageRegion> get(@Path("id") Long id);

    /**
     * Get all StorageRegions optionally filtered on query parameters
     *
     * available parameters:
     * - type
     * - name
     * - capacityLess
     * - capacityGreater
     *
     * @param parameters
     * @return
     */
    @GET(STORAGE_ROOT)
    Call<PageImpl<StorageRegion>> get(@QueryMap Map<String, String> parameters);

    /**
     * Create a StorageRegion for monitoring in the Ingest API
     *
     * @param create information about the StorageRegion to create
     * @return the new StorageRegion
     */
    @POST(STORAGE_ROOT)
    Call<StorageRegion> create(@Body RegionCreate create);
}
