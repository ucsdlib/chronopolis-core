package org.chronopolis.replicate.jobs;

import org.chronopolis.amqp.ChronProducer;
import org.chronopolis.messaging.base.ChronMessage;
import org.chronopolis.messaging.collection.CollectionInitMessage;
import org.chronopolis.messaging.factory.MessageFactory;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.listeners.JobListenerSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by shake on 6/13/14.
 */
public class AceRegisterJobListener extends JobListenerSupport {
    private final Logger log = LoggerFactory.getLogger(AceRegisterJobListener.class);

    private final String name;
    private final Scheduler scheduler;
    private final ChronProducer producer;
    private final MessageFactory messageFactory;

    public AceRegisterJobListener(final String name,
                                  final Scheduler scheduler,
                                  final ChronProducer producer,
                                  final MessageFactory messageFactory) {
        this.name = name;
        this.scheduler = scheduler;
        this.producer = producer;
        this.messageFactory = messageFactory;
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
        String returnKey = message.getReturnKey();
        String correlationId = jobExecutionContext.getJobDetail().getKey().getName();

        // Send collection init complete
        if (e == null) {
            ChronMessage response = messageFactory.collectionInitCompleteMessage(correlationId);
            producer.send(response, returnKey);
        } else {

        }
    }

}
