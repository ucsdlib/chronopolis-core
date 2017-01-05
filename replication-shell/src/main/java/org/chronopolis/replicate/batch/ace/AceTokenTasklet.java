package org.chronopolis.replicate.batch.ace;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import org.chronopolis.common.ace.AceService;
import org.chronopolis.replicate.ReplicationNotifier;
import org.chronopolis.replicate.batch.callback.UpdateCallback;
import org.chronopolis.replicate.config.ReplicationSettings;
import org.chronopolis.rest.api.IngestAPI;
import org.chronopolis.rest.models.Bag;
import org.chronopolis.rest.models.RStatusUpdate;
import org.chronopolis.rest.models.Replication;
import org.chronopolis.rest.models.ReplicationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * moo
 *
 * Created by shake on 3/8/16.
 */
public class AceTokenTasklet implements Runnable {
    private final Logger log = LoggerFactory.getLogger(AceTokenTasklet.class);

    private IngestAPI ingest;
    private AceService aceService;
    private Replication replication;
    private ReplicationSettings settings;
    private ReplicationNotifier notifier;
    private Long id;

    public AceTokenTasklet(IngestAPI ingest, AceService aceService, Replication replication, ReplicationSettings settings, ReplicationNotifier notifier, Long id) {
        this.ingest = ingest;
        this.aceService = aceService;
        this.replication = replication;
        this.settings = settings;
        this.notifier = notifier;
        this.id = id;
    }

    @Override
    public void run() {
        // Short circuit this mahfk
        if (replication.getStatus() == ReplicationStatus.ACE_TOKEN_LOADED
                || replication.getStatus() == ReplicationStatus.ACE_AUDITING) {
            return;
        }

        Bag bag = replication.getBag();
        log.info("Loading token store for {}", bag.getName());
        final AtomicBoolean complete = new AtomicBoolean(false);

        Path manifest = Paths.get(settings.getPreservation(), bag.getTokenLocation());

        log.info("Params for loadTokenStore: {} {}", id, manifest);
        Call<Void> call = aceService.loadTokenStore(id, RequestBody.create(MediaType.parse("ASCII Text"), manifest.toFile()));
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Response<Void> response) {
                if (response.isSuccess()) {
                    log.info("Successfully loaded token store");
                    Call<Replication> update = ingest.updateReplicationStatus(replication.getId(), new RStatusUpdate(ReplicationStatus.ACE_TOKEN_LOADED));
                    update.enqueue(new UpdateCallback());
                } else {
                    log.error("Error loading token store for collection: {} - {}", response.code(), response.message());
                    try {
                        log.debug("{}", response.errorBody().string());
                    } catch (IOException ignored) {
                    }
                    notifier.setSuccess(false);
                    throw new RuntimeException("Error loading token store");
                }

                complete.getAndSet(true);
            }

            @Override
            public void onFailure(Throwable throwable) {
                complete.getAndSet(true);
                notifier.setSuccess(false);
                log.error("", throwable);
                throw new RuntimeException(throwable);
            }
        });

        // Since the callback is asynchronous, we need to wait for it to complete before moving on
        // TODO: Should use something like the SimpleCallback to wait for it to complete
        //       or we could wrap it in a try/catch
        log.trace("Waiting for token register to complete");
        waitForCallback(complete);
    }


    private void waitForCallback(AtomicBoolean complete) {
        while (!complete.get()) {
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                log.error("Sleep interrupted", e);
            }
        }
    }
}
