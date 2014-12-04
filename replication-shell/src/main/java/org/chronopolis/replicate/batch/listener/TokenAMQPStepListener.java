package org.chronopolis.replicate.batch.listener;

import org.chronopolis.common.mail.MailUtil;
import org.chronopolis.replicate.ReplicationNotifier;
import org.chronopolis.replicate.config.ReplicationSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;

/**
 * Created by shake on 12/4/14.
 */
public class TokenAMQPStepListener implements StepExecutionListener {
    private final Logger log = LoggerFactory.getLogger(TokenAMQPStepListener.class);


    private ReplicationNotifier notifier;
    private ReplicationSettings settings;
    private MailUtil mailUtil;
    private String digest;

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
            status = ExitStatus.FAILED;
            // throw new FixityException("Could not validate the fixity of the token store");
        } else {
            log.info("Successfully validated token store");
            status = ExitStatus.COMPLETED;
        }


        return status;
    }
}
