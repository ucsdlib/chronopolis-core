package org.chronopolis.replicate.batch;

import org.chronopolis.common.mail.MailUtil;
import org.chronopolis.replicate.ReplicationNotifier;
import org.chronopolis.replicate.config.ReplicationSettings;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.mail.SimpleMailMessage;

/**
 * @deprecated will be removed in 1.4.0
 * Final step of the replication job. Determine whether or not the replication
 * was successful and send an email to chron-support regarding its status.
 *
 * Created by shake on 8/26/14.
 */
@Deprecated
public class ReplicationSuccessStep implements Tasklet {

    private MailUtil mailUtil;
    private ReplicationSettings settings;
    private ReplicationNotifier notifier;

    public ReplicationSuccessStep(MailUtil mailUtil,
                                  ReplicationSettings replicationSettings,
                                  ReplicationNotifier notifier) {
        this.mailUtil = mailUtil;
        this.notifier = notifier;
        settings = replicationSettings;
    }


    @Override
    public RepeatStatus execute(final StepContribution stepContribution, final ChunkContext chunkContext) throws Exception {
        String nodeName = settings.getNode();

        String subject = notifier.isSuccess()
                ? "Successful replication of " + notifier.getCollection()
                : "Failure in replication of " + notifier.getCollection();

        // Send on failure or success if we want it
        if (settings.sendOnSuccess() || !notifier.isSuccess()) {
            SimpleMailMessage mailMessage = mailUtil.createMessage(nodeName,
                    subject,
                    notifier.getNotificationBody());
            mailUtil.send(mailMessage);
        }

        return RepeatStatus.FINISHED;
    }
}
