package org.chronopolis.common.dpn;

import retrofit.http.Body;
import retrofit.http.PUT;

/**
 * Created by shake on 9/30/14.
 */
public interface DPNService {

    @PUT("/api/registry/create")
    void putRegistryItem(@Body RegistryItemModel model);

}
