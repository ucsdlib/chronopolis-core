package org.chronopolis.replicate.batch;

import org.chronopolis.common.mail.MailUtil;
import org.chronopolis.replicate.config.ReplicationSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Created by shake on 9/2/14.
 */
public class ReplicationStepListener implements StepExecutionListener {
    private final Logger log = LoggerFactory.getLogger(ReplicationStepListener.class);
    ReplicationSettings replicationSettings;
    MailUtil mailUtil;

    public ReplicationStepListener(final ReplicationSettings replicationSettings,
                                   final MailUtil mailUtil) {
        this.replicationSettings = replicationSettings;
        this.mailUtil = mailUtil;
    }

    @Override
    public void beforeStep(final StepExecution stepExecution) {
        log.trace("Before {}", stepExecution.getStepName());
    }

    @Override
    public ExitStatus afterStep(final StepExecution stepExecution) {
        ExitStatus status;
        log.trace("After {}", stepExecution.getStepName());
        log.debug("Step execution status {}", stepExecution.getStatus().toString());
        log.debug("Job execution status {}", stepExecution.getJobExecution().getStatus().toString());
        if (stepExecution.getStatus().isUnsuccessful()) {
            log.error("Step was unsuccessful");
            int i = 0;
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
                    replicationSettings.getNode(),
                    "Replication Failed",
                    stringWriter.toString()
            ));

            status = ExitStatus.FAILED;
        } else {
            status = ExitStatus.COMPLETED;
        }
        return status;
    }
}
