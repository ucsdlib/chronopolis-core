package org.chronopolis.replicate.batch.ace;

import org.chronopolis.common.ace.AceService;
import org.chronopolis.replicate.ReplicationNotifier;
import org.chronopolis.replicate.batch.callback.UpdateCallback;
import org.chronopolis.rest.api.ReplicationService;
import org.chronopolis.rest.api.ServiceGenerator;
import org.chronopolis.rest.models.Replication;
import org.chronopolis.rest.models.enums.ReplicationStatus;
import org.chronopolis.rest.models.update.ReplicationStatusUpdate;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.io.IOException;

/**
 * Start an audit in an ACE AM instance for a given collection id
 * <p>
 * Created by shake on 3/8/16.
 */
public class AceAuditTasklet implements Runnable {
    private final Logger log = LoggerFactory.getLogger("ace-log");

    private final Long id;
    private final AceService aceService;
    private final Replication replication;
    private final ReplicationNotifier notifier;
    private final ReplicationService replications;

    /**
     * Constructor for AceAuditTasklet
     *
     * @param generator   the generator to create Chronopolis Ingest API connections
     * @param aceService  the service to connect to an ACE AM
     * @param replication the replication to audit/update
     * @param notifier    the notifier to hold status information regarding this process
     * @param id          the id of the collection in ACE
     */
    public AceAuditTasklet(ServiceGenerator generator,
                           AceService aceService,
                           Replication replication,
                           ReplicationNotifier notifier,
                           Long id) {
        this.id = id;
        this.notifier = notifier;
        this.aceService = aceService;
        this.replication = replication;
        this.replications = generator.replications();
    }

    @Override
    public void run() {
        String name = replication.getBag().getName();
        Call<Void> auditCall = aceService.startAudit(id, false);

        auditCall.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NotNull Call<Void> call,
                                   @NotNull Response<Void> response) {
                if (response.isSuccessful()) {
                    Call<Replication> update = replications.updateStatus(replication.getId(),
                            new ReplicationStatusUpdate(ReplicationStatus.ACE_AUDITING));
                    update.enqueue(new UpdateCallback());
                } else {
                    log.error("{} Error starting audit for collection: {} - {}", name, response.code(), response.message());
                    String message = "Error starting audit:\n";
                    try {
                        message += response.errorBody().string();
                        log.debug("{} {}", name, message);
                    } catch (IOException ignored) {
                    }

                    notifier.setAceStep(message);
                    notifier.setSuccess(false);
                }
            }

            @Override
            public void onFailure(@NotNull Call<Void> call, @NotNull Throwable throwable) {
                log.error("{} Error communicating with ACE Server", name, throwable);
                notifier.setSuccess(false);
                notifier.setAceStep(throwable.getMessage());
            }
        });
    }

}
