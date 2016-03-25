package org.chronopolis.replicate.batch.ace;

import org.chronopolis.common.ace.AceService;
import org.chronopolis.replicate.ReplicationNotifier;
import org.chronopolis.replicate.batch.callback.UpdateCallback;
import org.chronopolis.replicate.config.ReplicationSettings;
import org.chronopolis.rest.api.IngestAPI;
import org.chronopolis.rest.models.RStatusUpdate;
import org.chronopolis.rest.entities.Replication;
import org.chronopolis.rest.models.ReplicationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.io.IOException;

/**
 *
 * Created by shake on 3/8/16.
 */
public class AceAuditTasklet implements Runnable {
    private final Logger log = LoggerFactory.getLogger(AceAuditTasklet.class);

    private IngestAPI ingest;
    private AceService aceService;
    private Replication replication;
    private ReplicationSettings settings;
    private ReplicationNotifier notifier;
    private Long id;

    public AceAuditTasklet(IngestAPI ingest, AceService aceService, Replication replication, ReplicationSettings settings, ReplicationNotifier notifier, Long id) {
        this.ingest = ingest;
        this.aceService = aceService;
        this.replication = replication;
        this.settings = settings;
        this.notifier = notifier;
        this.id = id;
    }

    @Override
    public void run() {
        Call<Void> auditCall = aceService.startAudit(id);

        auditCall.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Response<Void> response) {
                if (response.isSuccess()) {
                    Call<Replication> update = ingest.updateReplicationStatus(replication.getId(), new RStatusUpdate(ReplicationStatus.ACE_AUDITING));
                    update.enqueue(new UpdateCallback());
                } else {
                    log.error("Error starting audit for collection: {} - {}", response.code(), response.message());
                    String message = "Error starting audit:\n";
                    try {
                        message += response.errorBody().string();
                        log.debug("{}", message);
                    } catch (IOException ignored) {
                    }

                    notifier.setAceStep(message);
                    notifier.setSuccess(false);
                }
            }

            @Override
            public void onFailure(Throwable throwable) {
                log.error("Error communicating with ACE Server", throwable);
                notifier.setSuccess(false);
                notifier.setAceStep(throwable.getMessage());
            }
        });
    }

}