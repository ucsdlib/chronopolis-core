package org.chronopolis.common.dpn;

import retrofit.http.Body;
import retrofit.http.POST;

/**
 * Created by shake on 9/30/14.
 */
public interface DPNService {

    @POST("/api-v1/bag/")
    DPNBag createBag(@Body DPNBag bag);

}
