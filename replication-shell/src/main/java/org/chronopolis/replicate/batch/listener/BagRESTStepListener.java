package org.chronopolis.replicate.batch.listener;

import org.chronopolis.common.mail.MailUtil;
import org.chronopolis.replicate.ReplicationNotifier;
import org.chronopolis.replicate.config.ReplicationSettings;
import org.chronopolis.rest.api.IngestAPI;
import org.chronopolis.rest.models.FixityUpdate;
import org.chronopolis.rest.models.Replication;
import org.chronopolis.rest.models.ReplicationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static org.chronopolis.replicate.batch.listener.Util.sendFailure;

/**
 * Step listener for after the bag-download step when triggered from the RESTful
 * interface. If there was an error validating the tag manifest, we want to stop
 * the job so that we do not attempt to register bad files with ACE.
 * <p/>
 * Created by shake on 12/4/14.
 */
public class BagRESTStepListener implements StepExecutionListener {
    private final Logger log = LoggerFactory.getLogger(BagRESTStepListener.class);

    private MailUtil mail;
    private IngestAPI ingestAPI;
    private Replication replication;
    private ReplicationSettings settings;
    private ReplicationNotifier notifier;

    public BagRESTStepListener(MailUtil mail,
                               IngestAPI ingestAPI,
                               Replication replication,
                               ReplicationSettings settings,
                               ReplicationNotifier notifier) {
        this.mail = mail;
        this.ingestAPI = ingestAPI;
        this.replication = replication;
        this.settings = settings;
        this.notifier = notifier;
    }

    @Override
    public void beforeStep(final StepExecution stepExecution) {
    }

    @Override
    public ExitStatus afterStep(final StepExecution stepExecution) {
        // Check if we were able to download, if not let the ingest-server know
        if (notifier.isSuccess()) {
            // A boolean in case we can't communicate with the ingest-server
            boolean failure = false;
            String digest = notifier.getCalculatedTagDigest();
            replication.setReceivedTagFixity(digest);
            Call<Replication> call;
            Replication updated = null;

            // If there are any exceptions, fail and stop replication
            try {
                // call = ingestAPI.updateReplication(replication.getId(), replication);
                call = ingestAPI.updateTagManifest(replication.getId(), new FixityUpdate(digest));
                Response<Replication> response = call.execute();
                updated = response.body();
            } catch (Exception e) {
                log.error("Error communicating with the ingest-server", e);
                stepExecution.getFailureExceptions().add(e);
                failure = true;
            }

            if (failure || updated.getStatus() == ReplicationStatus.FAILURE_TAG_MANIFEST) {
                log.error("Error validating tagmanifest");
                stepExecution.upgradeStatus(BatchStatus.STOPPED);
                if (updated != null) {
                    updated.setBag(replication.getBag());
                    updated.setNodeUser(settings.getNode());
                }
                sendFailure(mail, settings, updated, stepExecution.getFailureExceptions());
                return ExitStatus.FAILED;
            }
        } else {
            // general failure
            Call<Replication> call = ingestAPI.failReplication(replication.getId());
            call.enqueue(new Callback<Replication>() {
                @Override
                public void onResponse(Response<Replication> response) {
                    log.debug("Update to replication {}: {} - {}", new Object[]{replication.getId(),
                            response.code(),
                            response.message()});
                }

                @Override
                public void onFailure(Throwable throwable) {
                    log.error("Error communicating with ingest server", throwable);
                }
            });
            sendFailure(mail, settings, replication, stepExecution.getFailureExceptions());
            return ExitStatus.FAILED;
        }

        return ExitStatus.COMPLETED;
    }

}
