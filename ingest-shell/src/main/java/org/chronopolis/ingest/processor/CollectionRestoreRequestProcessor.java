package org.chronopolis.ingest.processor;

import org.chronopolis.amqp.ChronProducer;
import org.chronopolis.amqp.RoutingKey;
import org.chronopolis.common.restore.CollectionRestore;
import org.chronopolis.db.common.RestoreRepository;
import org.chronopolis.db.common.model.RestoreRequest;
import org.chronopolis.ingest.config.IngestSettings;
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
    private final IngestSettings settings;
    private final MessageFactory messageFactory;
    private final CollectionRestore restore;
    private final RestoreRepository restoreRepository;

    public CollectionRestoreRequestProcessor(final ChronProducer producer,
                                             final IngestSettings settings,
                                             final MessageFactory messageFactory,
                                             final CollectionRestore restore,
                                             final RestoreRepository restoreRepository) {
        this.producer = producer;
        this.settings = settings;
        this.messageFactory = messageFactory;
        this.restore = restore;
        this.restoreRepository = restoreRepository;
    }

    @Override
    public void process(final ChronMessage chronMessage) {
        if (!(chronMessage instanceof CollectionRestoreRequestMessage)) {
            throw new RuntimeException("Invalid message!");
        }

        CollectionRestoreRequestMessage msg = (CollectionRestoreRequestMessage) chronMessage;
        String depositor = msg.getDepositor();
        String collection = msg.getCollection();

        ChronMessage next;
        String route;

        if (Paths.get(settings.getPreservation()).toFile().exists()) {
            Path restored = restore.restore(depositor, collection);
            next = messageFactory.collectionRestoreCompleteMessage(Indicator.ACK,
                restored.toString(),
                msg.getCorrelationId());
            route = msg.getReturnKey();
        } else {
            // reuse the correlation id for consistency
            next = messageFactory.collectionRestoreRequestMessage(collection,
                    depositor,
                    msg.getCorrelationId());
            route = RoutingKey.REPLICATE_BROADCAST.asRoute();

            // Add a RestoreRequest to keep track of the request through our flow
            RestoreRequest restoreRequest = new RestoreRequest(next.getCorrelationId());
            restoreRequest.setDepositor(depositor);
            restoreRequest.setCollectionName(collection);
            restoreRequest.setReturnKey(msg.getReturnKey());
            restoreRequest.setDirectory("directory");
            restoreRepository.save(restoreRequest);
        }

        producer.send(next, route);
    }
}
