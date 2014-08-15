package org.chronopolis.ingest.processor;

import org.chronopolis.amqp.ChronProducer;
import org.chronopolis.db.DatabaseManager;
import org.chronopolis.db.model.ReplicationFlow;
import org.chronopolis.db.model.ReplicationState;
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

    private final ChronProducer producer;
    private final MessageFactory messageFactory;
    private final DatabaseManager manager;

    public CollectionInitReplyProcessor(final ChronProducer producer,
                                        final MessageFactory messageFactory,
                                        final DatabaseManager manager) {
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
        String node = msg.getOrigin();

        ReplicationFlow flow = manager.getReplicationFlowTable()
                                      .findByDepositorAndCollectionAndNode(depositor,
                                              collection,
                                              node);

        if (msg.getMessageAtt().equals("ack")) {
            flow.setCurrentState(ReplicationState.REPLICATING);
        } else {
            flow.setCurrentState(ReplicationState.FAILED);
        }
        manager.getReplicationFlowTable().save(flow);

    }
}
