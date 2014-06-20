package org.chronopolis.replicate.jobs;

import org.chronopolis.common.mail.MailUtil;
import org.chronopolis.messaging.collection.CollectionInitMessage;
import org.chronopolis.replicate.ReplicationProperties;
import org.chronopolis.replicate.processor.CollectionInitProcessor;
import org.chronopolis.replicate.util.MailFunctions;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.listeners.JobListenerSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;

import java.util.Map;

/**
 * Created by shake on 6/13/14.
 */
public class TokenStoreDownloadJobListener extends JobListenerSupport {
    private final Logger log = LoggerFactory.getLogger(TokenStoreDownloadJobListener.class);

    private final String name;
    private final Scheduler scheduler;
    private final ReplicationProperties properties;
    private final MailUtil mailUtil;

    public TokenStoreDownloadJobListener(String name,
                                         Scheduler scheduler,
                                         ReplicationProperties properties,
                                         MailUtil mailUtil) {
        this.name = name;
        this.scheduler = scheduler;
        this.properties = properties;
        this.mailUtil = mailUtil;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void jobWasExecuted(final JobExecutionContext jobExecutionContext,
                               final JobExecutionException e) {
        JobDetail jobDetail = jobExecutionContext.getJobDetail();
        Map<String, String> completionMap =
                (Map<String, String>) jobDetail.getJobDataMap()
                                               .get(BagDownloadJob.COMPLETED);

        // If there was no exception, schedule our next job
        if (e == null) {
            JobKey myKey = jobExecutionContext.getJobDetail().getKey();

            try {
                scheduler.triggerJob(new JobKey(myKey.getName(), "BagDownload"));
            } catch (SchedulerException e1) {
                log.error("Scheduler exception!!", e1);
            }

        } else { // requeue our job..?
            CollectionInitMessage msg =
                    (CollectionInitMessage) jobDetail.getJobDataMap()
                            .get(BagDownloadJob.MESSAGE);

            String nodeName = properties.getNodeName();
            String subject = "Failure in CollectionInit - Token Store Job";
            String text = MailFunctions.createText(msg, completionMap, e);

            SimpleMailMessage message = mailUtil.createMessage(nodeName, subject, text);
            mailUtil.send(message);
        }
    }
}
