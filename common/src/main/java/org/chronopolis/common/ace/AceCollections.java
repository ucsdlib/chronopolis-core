package org.chronopolis.common.ace;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

import java.util.List;

/**
 * ACE collections API
 *
 * Created by shake on 4/17/17.
 */
public interface AceCollections {

    @GET("rest/collections")
    Call<List<GsonCollection>> getCollections(@Query("group") String group, @Query("corrupt") Boolean corrupt, @Query("active") Boolean active);

    @GET("rest/collections/{id}/items")
    Call<List<MonitoredItem>> getItems(@Path("id") Long id, @Query("state") String state);

}
