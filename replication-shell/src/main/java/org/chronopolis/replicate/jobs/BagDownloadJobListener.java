package org.chronopolis.replicate.jobs;

import org.chronopolis.common.mail.MailUtil;
import org.chronopolis.messaging.collection.CollectionInitMessage;
import org.chronopolis.replicate.ReplicationProperties;
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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;

/**
 * Created by shake on 6/13/14.
 */
public class BagDownloadJobListener extends JobListenerSupport {
    private final Logger log = LoggerFactory.getLogger(BagDownloadJobListener.class);

    private final String name;
    private final Scheduler scheduler;
    private final ReplicationProperties properties;
    private final MailUtil mailUtil;

    public BagDownloadJobListener(final String name,
                                  final Scheduler scheduler,
                                  final ReplicationProperties properties,
                                  final MailUtil mailUtil) {
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

        if (e == null) {
            JobKey key = jobExecutionContext.getJobDetail().getKey();

            try {
                scheduler.triggerJob(new JobKey(key.getName(), "AceRegister"));
            } catch (SchedulerException e1) {
                log.error("Scheduler Exception! ", e1);
            }

        } else {
            CollectionInitMessage msg =
                    (CollectionInitMessage) jobDetail.getJobDataMap()
                                                     .get(BagDownloadJob.MESSAGE);
            Map<String, String> completionMap =
                    (Map<String, String>) jobDetail.getJobDataMap()
                                                   .get(BagDownloadJob.COMPLETED);

            String nodeName = properties.getNodeName();
            String subject = "Failure in CollectionInit - Bag Download Job";
            String text = MailFunctions.createText(msg, completionMap, e);

            SimpleMailMessage message = mailUtil.createMessage(nodeName, subject, text);
            mailUtil.send(message);
        }
    }

}
