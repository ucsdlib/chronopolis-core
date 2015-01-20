package org.chronopolis.replicate.batch.listener;

import org.chronopolis.common.mail.MailUtil;
import org.chronopolis.replicate.ReplicationNotifier;
import org.chronopolis.replicate.config.ReplicationSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Created by shake on 12/4/14.
 */
public class BagAMQPStepListener implements StepExecutionListener {
    private final Logger log = LoggerFactory.getLogger(BagAMQPStepListener.class);

    private ReplicationSettings settings;
    private ReplicationNotifier notifier;
    private MailUtil mailUtil;
    private String tagDigest;

    public BagAMQPStepListener(ReplicationSettings settings,
                               ReplicationNotifier notifier,
                               MailUtil mailUtil,
                               String tagDigest) {
        this.settings = settings;
        this.notifier = notifier;
        this.mailUtil = mailUtil;
        this.tagDigest = tagDigest;
    }

    @Override
    public void beforeStep(final StepExecution stepExecution) {
    }

    @Override
    public ExitStatus afterStep(final StepExecution stepExecution) {
        ExitStatus status;
        String calculatedDigest = notifier.getCalculatedTagDigest();
        String statusMessage = "success";

        if (!calculatedDigest.equalsIgnoreCase(tagDigest)) {
            log.error("Downloaded tagmanifest does not match expected digest!" +
                            "\nFound {}\nExpected {}",
                    calculatedDigest,
                    tagDigest);
            statusMessage = "Downloaded tag manifest does not match expected digest";


            StringWriter stringWriter = new StringWriter();
            PrintWriter textBody = new PrintWriter(stringWriter, true);
            textBody.println(stepExecution.getJobParameters().toString());
            textBody.println();
            textBody.println();
            textBody.println("Exceptions: \n");
            for (Throwable t : stepExecution.getFailureExceptions()) {
                textBody.println(t.getMessage());
                for (StackTraceElement element : t.getStackTrace()) {
                    textBody.println(element);
                }
            };


            mailUtil.send(mailUtil.createMessage(
                    settings.getNode(),
                    "Replication Failed for collection "
                            + notifier.getMessage().getCollection(),
                    stringWriter.toString()
            ));

            status = ExitStatus.FAILED;
        } else {
            log.info("Successfully validated tagmanifest");
            status = ExitStatus.COMPLETED;
        }

        notifier.setBagStep(statusMessage);

        return status;
    }
}
