package org.chronopolis.replicate.batch.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chronopolis.common.mail.MailUtil;
import org.chronopolis.replicate.ReplicationNotifier;
import org.chronopolis.replicate.config.ReplicationSettings;
import org.chronopolis.rest.api.IngestAPI;
import org.chronopolis.rest.models.Replication;
import org.chronopolis.rest.models.ReplicationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Formatter;
import java.util.List;

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
            Replication updated = null;

            // If there are any exceptions, fail and stop replication
            try {
                updated = ingestAPI.updateReplication(replication.getID(), replication);
            } catch (Exception e) {
                log.error("Error communicating with the ingest-server", e);
                stepExecution.getFailureExceptions().add(e);
                failure = true;
            }

            if (failure || updated.getStatus() == ReplicationStatus.FAILURE_TAG_MANIFEST) {
                log.error("Error validating tagmanifest");
                stepExecution.upgradeStatus(BatchStatus.STOPPED);
                updated.setBag(replication.getBag());
                updated.setNodeUser(settings.getNode());
                sendMail(updated, stepExecution.getFailureExceptions());
                return ExitStatus.FAILED;
            }
        } else {
            // general failure
            replication.setStatus(ReplicationStatus.FAILURE);
            ingestAPI.updateReplication(replication.getID(), replication);
        }

        return ExitStatus.COMPLETED;
    }

    private void sendMail(Replication replication, List<Throwable> throwables) {
        Formatter titleFormat = new Formatter();
        String title = "Replication failed for collection %s";
        titleFormat.format(title, replication.getBag().getName());

        ObjectMapper mapper = new ObjectMapper();
        StringWriter stringWriter = new StringWriter();
        PrintWriter textBody = new PrintWriter(stringWriter, true);
        try {
            textBody.println(mapper.writeValueAsString(replication));
        } catch (JsonProcessingException e) {
            log.info("Error writing replication as bag", e);
            // ignore
        }
        textBody.println();
        textBody.println();
        textBody.println("Exceptions: \n");
        for (Throwable t : throwables) {
            textBody.println(t.getMessage());
            for (StackTraceElement element : t.getStackTrace()) {
                textBody.println(element);
            }
            textBody.println();
        }

        mail.send(mail.createMessage(
                settings.getNode(),
                titleFormat.toString(),
                stringWriter.toString()
        ));
    }
}
