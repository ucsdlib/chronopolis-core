package org.chronopolis.replicate.jobs;

import org.chronopolis.amqp.ChronProducer;
import org.chronopolis.common.mail.MailUtil;
import org.chronopolis.messaging.base.ChronMessage;
import org.chronopolis.messaging.collection.CollectionInitMessage;
import org.chronopolis.messaging.factory.MessageFactory;
import org.chronopolis.replicate.config.ReplicationSettings;
import org.chronopolis.replicate.util.MailFunctions;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.listeners.JobListenerSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;

import java.util.Map;

/**
 * Created by shake on 6/13/14.
 */
public class AceRegisterJobListener extends JobListenerSupport {
    private final Logger log = LoggerFactory.getLogger(AceRegisterJobListener.class);

    private final String name;
    private final Scheduler scheduler;
    private final ChronProducer producer;
    private final MessageFactory messageFactory;
    private final ReplicationSettings settings;
    private final MailUtil mailUtil;

    public AceRegisterJobListener(final String name,
                                  final Scheduler scheduler,
                                  final ChronProducer producer,
                                  final MessageFactory messageFactory,
                                  final ReplicationSettings settings,
                                  final MailUtil mailUtil) {
        this.name = name;
        this.scheduler = scheduler;
        this.producer = producer;
        this.settings = settings;
        this.messageFactory = messageFactory;
        this.mailUtil = mailUtil;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void jobWasExecuted(final JobExecutionContext jobExecutionContext,
                               final JobExecutionException e) {
        JobDataMap jobData = jobExecutionContext.getJobDetail().getJobDataMap();
        CollectionInitMessage message =
                (CollectionInitMessage) jobData.get(AceRegisterJob.MESSAGE);
        Map<String, String> completionMap =
                 (Map<String, String>) jobData.get(BagDownloadJob.COMPLETED);

        String returnKey = message.getReturnKey();
        String correlationId = jobExecutionContext.getJobDetail().getKey().getName();
        String nodeName = settings.getNode();
        String subject;

        // Send collection init complete
        if (e == null) {
            ChronMessage response = messageFactory.collectionInitCompleteMessage(correlationId);
            producer.send(response, returnKey);
            subject = "Successful replication of " + message.getCollection();
        } else {
            subject = "Failure in CollectionInit - Ace Register Job";
        }

        String text = MailFunctions.createText(message, completionMap, e);
        SimpleMailMessage mailMessage = mailUtil.createMessage(nodeName, subject, text);
        mailUtil.send(mailMessage);
    }

}
