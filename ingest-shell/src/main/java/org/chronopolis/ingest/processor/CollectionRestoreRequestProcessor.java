package org.chronopolis.ingest.processor;

import org.chronopolis.amqp.ChronProducer;
import org.chronopolis.amqp.RoutingKey;
import org.chronopolis.common.mail.MailUtil;
import org.chronopolis.common.restore.CollectionRestore;
import org.chronopolis.ingest.IngestProperties;
import org.chronopolis.messaging.Indicator;
import org.chronopolis.messaging.base.ChronMessage;
import org.chronopolis.messaging.base.ChronProcessor;
import org.chronopolis.messaging.collection.CollectionRestoreRequestMessage;
import org.chronopolis.messaging.factory.MessageFactory;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by shake on 7/23/14.
 */
public class CollectionRestoreRequestProcessor implements ChronProcessor {

    private final ChronProducer producer;
    private final IngestProperties properties;
    private final MessageFactory messageFactory;
    private final CollectionRestore restore;
    private final MailUtil mailUtil;


    public CollectionRestoreRequestProcessor(final ChronProducer producer,
                                             final IngestProperties properties,
                                             final MessageFactory messageFactory,
                                             final CollectionRestore restore,
                                             final MailUtil mailUtil) {
        this.producer = producer;
        this.properties = properties;
        this.messageFactory = messageFactory;
        this.restore = restore;
        this.mailUtil = mailUtil;
    }

    @Override
    public void process(final ChronMessage chronMessage) {
        if (!(chronMessage instanceof CollectionRestoreRequestMessage)) {
            throw new RuntimeException("Invalid message!");
        }

        CollectionRestoreRequestMessage msg = (CollectionRestoreRequestMessage) chronMessage;
        String depositor = msg.getDepositor();
        String collection = msg.getCollection();

        ChronMessage next = null;
        String route;
        if (Paths.get(properties.getPreservation()).toFile().exists()) {
            Path restored = restore.restore(depositor, collection);
            next = messageFactory.collectionRestoreCompleteMessage(Indicator.ACK,
                restored.toString(),
                msg.getCorrelationId());
            route = msg.getReturnKey();
        } else {
            next = messageFactory.collectionRestoreRequestMessage(collection, depositor);
            route = RoutingKey.REPLICATE_BROADCAST.asRoute();
        }

        producer.send(next, route);
    }
}
