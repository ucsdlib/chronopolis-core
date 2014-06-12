package org.chronopolis.ingest.processor;

import org.chronopolis.amqp.ChronProducer;
import org.chronopolis.db.DatabaseManager;
import org.chronopolis.db.model.ReplicationFlow;
import org.chronopolis.db.model.ReplicationState;
import org.chronopolis.ingest.IngestProperties;
import org.chronopolis.messaging.base.ChronMessage;
import org.chronopolis.messaging.base.ChronProcessor;
import org.chronopolis.messaging.collection.CollectionInitReplyMessage;
import org.chronopolis.messaging.factory.MessageFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by shake on 6/12/14.
 */
public class CollectionInitReplyProcessor implements ChronProcessor {
    private final Logger log = LoggerFactory.getLogger(CollectionInitReplyProcessor.class);

    private final IngestProperties properties;
    private final ChronProducer producer;
    private final MessageFactory messageFactory;
    private final DatabaseManager manager;

    public CollectionInitReplyProcessor(final IngestProperties properties,
                                        final ChronProducer producer,
                                        final MessageFactory messageFactory,
                                        final DatabaseManager manager) {
        this.properties = properties;
        this.producer = producer;
        this.messageFactory = messageFactory;
        this.manager = manager;
    }

    @Override
    public void process(final ChronMessage chronMessage) {
        if (!(chronMessage instanceof CollectionInitReplyMessage)) {
            log.error("error");
            return;
        }

        CollectionInitReplyMessage msg = (CollectionInitReplyMessage) chronMessage;
        String depositor = msg.getDepositor();
        String collection = msg.getCollection();

        ReplicationFlow flow = manager.getReplicationFlowTable().findByDepositorAndCollection(depositor, collection);
        flow.setCurrentState(ReplicationState.REPLICATING);
        manager.getReplicationFlowTable().save(flow);

    }
}
