package org.chronopolis.replicate.batch;

import org.chronopolis.amqp.ChronProducer;
import org.chronopolis.common.mail.MailUtil;
import org.chronopolis.messaging.base.ChronMessage;
import org.chronopolis.messaging.collection.CollectionInitMessage;
import org.chronopolis.messaging.factory.MessageFactory;
import org.chronopolis.replicate.ReplicationNotifier;
import org.chronopolis.replicate.config.ReplicationSettings;
import org.chronopolis.replicate.util.MailFunctions;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.mail.SimpleMailMessage;

import java.util.HashMap;

/**
 * Created by shake on 8/26/14.
 */
public class ReplicationSuccessStep implements Tasklet {

    private MailUtil mailUtil;
    private ChronProducer producer;
    private MessageFactory messageFactory;
    private ReplicationSettings settings;
    private ReplicationNotifier notifier;

    public ReplicationSuccessStep(ChronProducer producer,
                                  MessageFactory messageFactory,
                                  MailUtil mailUtil,
                                  ReplicationSettings replicationSettings,
                                  ReplicationNotifier notifier) {
        this.producer = producer;
        this.messageFactory = messageFactory;
        this.mailUtil = mailUtil;
        this.notifier = notifier;
        settings = replicationSettings;
    }


    @Override
    public RepeatStatus execute(final StepContribution stepContribution, final ChunkContext chunkContext) throws Exception {
        CollectionInitMessage message = notifier.getMessage();
        String correlationId = message.getCorrelationId();
        String returnKey = message.getReturnKey();
        String nodeName = settings.getNode();

        String subject = notifier.isSuccess()
                ? "Successful replication of " + message.getCollection()
                : "Failure in replication of " + message.getCollection();


        ChronMessage response = messageFactory.collectionInitCompleteMessage(correlationId);
        producer.send(response, returnKey);

        SimpleMailMessage mailMessage = mailUtil.createMessage(nodeName,
                subject,
                notifier.getNotificationBody());
        mailUtil.send(mailMessage);

        return RepeatStatus.FINISHED;
    }
}
