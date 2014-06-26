package org.chronopolis.replicate.jobs;

import org.chronopolis.amqp.ChronProducer;
import org.chronopolis.common.mail.MailUtil;
import org.chronopolis.messaging.Indicator;
import org.chronopolis.messaging.collection.CollectionInitMessage;
import org.chronopolis.messaging.collection.CollectionInitReplyMessage;
import org.chronopolis.messaging.factory.MessageFactory;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * TODO There's shared code between this and the TokenStoreJobListener.
 * Can probably make a function they can both use.
 *
 * Created by shake on 6/13/14.
 */
public class BagDownloadJobListener extends JobListenerSupport {
    private final Logger log = LoggerFactory.getLogger(BagDownloadJobListener.class);

    private final String name;
    private final Scheduler scheduler;
    private final ReplicationProperties properties;
    private final MailUtil mailUtil;
    private final MessageFactory messageFactory;
    private final ChronProducer producer;

    public BagDownloadJobListener(final String name,
                                  final Scheduler scheduler,
                                  final ReplicationProperties properties,
                                  final MailUtil mailUtil,
                                  final MessageFactory messageFactory,
                                  final ChronProducer producer) {
        this.name = name;
        this.scheduler = scheduler;
        this.properties = properties;
        this.mailUtil = mailUtil;
        this.messageFactory = messageFactory;
        this.producer = producer;
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
         CollectionInitMessage message =
                (CollectionInitMessage) jobDetail.getJobDataMap()
                                                 .get(BagDownloadJob.MESSAGE);



        if (e == null) {
            JobKey key = jobExecutionContext.getJobDetail().getKey();
            CollectionInitReplyMessage reply;

            Boolean success = (Boolean) jobExecutionContext.getResult();

            String replyKey = message.getReturnKey();
            String correlationId = message.getCorrelationId();
            String depositor = message.getDepositor();
            String collection = message.getCollection();
            List<String> failedItems = new ArrayList<String>();
            Indicator ind;

            if (success) {
                // Spawn the next job and prepare an ACK
                try {
                    scheduler.triggerJob(new JobKey(key.getName(), "AceRegister"));
                } catch (SchedulerException e1) {
                    log.error("Scheduler Exception! ", e1);
                }
                ind = Indicator.ACK;
            } else {
                ind = Indicator.NAK;
                failedItems.add("tagmanifest-sha256.txt");
            }

            reply = messageFactory.collectionInitReplyMessage(correlationId,
                    ind,
                    depositor,
                    collection,
                    failedItems);
            producer.send(reply, replyKey);
        } else {
            CollectionInitMessage msg =
                    (CollectionInitMessage) jobDetail.getJobDataMap()
                                                     .get(BagDownloadJob.MESSAGE);

            String nodeName = properties.getNodeName();
            String subject = "Failure in CollectionInit - Bag Download Job";
            String text = MailFunctions.createText(msg, completionMap, e);

            SimpleMailMessage mailMessage = mailUtil.createMessage(nodeName, subject, text);
            mailUtil.send(mailMessage);
        }
    }

}
