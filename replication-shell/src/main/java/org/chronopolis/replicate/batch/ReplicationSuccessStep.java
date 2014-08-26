package org.chronopolis.replicate.batch;

import org.chronopolis.amqp.ChronProducer;
import org.chronopolis.common.mail.MailUtil;
import org.chronopolis.messaging.base.ChronMessage;
import org.chronopolis.messaging.collection.CollectionInitMessage;
import org.chronopolis.messaging.factory.MessageFactory;
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
    private CollectionInitMessage message;

    public ReplicationSuccessStep(ChronProducer producer,
                                  CollectionInitMessage message,
                                  MessageFactory messageFactory,
                                  MailUtil mailUtil,
                                  ReplicationSettings replicationSettings) {
        this.producer = producer;
        this.message = message;
        this.messageFactory = messageFactory;
        this.mailUtil = mailUtil;
        settings = replicationSettings;
    }


    @Override
    public RepeatStatus execute(final StepContribution stepContribution, final ChunkContext chunkContext) throws Exception {
        String correlationId = message.getCorrelationId();
        String returnKey = message.getReturnKey();
        String nodeName = settings.getNode();
        String subject = "Successful replication of " + message.getCollection();

        ChronMessage response = messageFactory.collectionInitCompleteMessage(correlationId);
        producer.send(response, returnKey);

        String text = MailFunctions.createText(message, new HashMap<String, String>(), null);
        SimpleMailMessage mailMessage = mailUtil.createMessage(nodeName, subject, text);
        mailUtil.send(mailMessage);

        return RepeatStatus.FINISHED;
    }
}
