package org.chronopolis.replicate.batch.callback;

import org.chronopolis.rest.models.Replication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Callback;
import retrofit2.Response;

import java.io.IOException;

/**
 *
 * Created by shake on 3/8/16.
 */
public class UpdateCallback implements Callback<Replication> {
    private final Logger log = LoggerFactory.getLogger(UpdateCallback.class);

    @Override
    public void onResponse(Response<Replication> response) {
        if (response.isSuccess()) {
            log.info("Successfully updated replication {}", response.body().getId());
        } else {
            log.warn("Error updating replication: {} - {}", response.code(), response.message());
            try {
                log.warn("{}", response.errorBody().string());
            } catch (IOException ignored) {
            }
        }
    }

    @Override
    public void onFailure(Throwable throwable) {
        log.error("Error communicating with Ingest Server", throwable);
    }
}
