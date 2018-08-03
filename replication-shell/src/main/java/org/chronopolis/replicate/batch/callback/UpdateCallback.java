package org.chronopolis.replicate.batch.callback;

import org.chronopolis.rest.kot.models.Replication;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.io.IOException;

/**
 * Reusable callback for status updates to Replications
 *
 * Created by shake on 3/8/16.
 */
public class UpdateCallback implements Callback<Replication> {
    private final Logger log = LoggerFactory.getLogger(UpdateCallback.class);

    @Override
    public void onResponse(@NotNull Call<Replication> call,
                           @NotNull Response<Replication> response) {
        if (response.isSuccessful()) {
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
    public void onFailure(@NotNull Call<Replication> call, @NotNull Throwable throwable) {
        log.error("Error communicating with Ingest Server", throwable);
    }
}
