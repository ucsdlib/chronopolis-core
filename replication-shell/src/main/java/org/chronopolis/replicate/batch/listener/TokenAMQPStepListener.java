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
public class TokenAMQPStepListener implements StepExecutionListener {
    private final Logger log = LoggerFactory.getLogger(TokenAMQPStepListener.class);


    private ReplicationNotifier notifier;
    private ReplicationSettings settings;
    private MailUtil mailUtil;
    private String digest;

    public TokenAMQPStepListener(ReplicationNotifier notifier,
                                 ReplicationSettings settings,
                                 MailUtil mailUtil,
                                 String digest) {
        this.notifier = notifier;
        this.settings = settings;
        this.mailUtil = mailUtil;
        this.digest = digest;
    }

    @Override
    public void beforeStep(final StepExecution stepExecution) {

    }

    @Override
    public ExitStatus afterStep(final StepExecution stepExecution) {
        ExitStatus status;
        String calculatedDigest = notifier.getCalculatedTokenDigest();

        if (!calculatedDigest.equalsIgnoreCase(digest)) {
            // Fail
            log.error("Downloaded token store does not match expected digest!" +
                            "\nFound {}\nExpected {}",
                    calculatedDigest,
                    digest);

            notifier.setSuccess(false);
            notifier.setTokenStep("Downloaded token store does not match expected digest");

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
            }


            mailUtil.send(mailUtil.createMessage(
                    settings.getNode(),
                    "Replication Failed for collection "
                            + notifier.getMessage().getCollection(),
                    stringWriter.toString()
            ));

            status = ExitStatus.FAILED;
            // throw new FixityException("Could not validate the fixity of the token store");
        } else {
            log.info("Successfully validated token store");
            status = ExitStatus.COMPLETED;
        }


        return status;
    }
}
