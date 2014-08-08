package org.chronopolis.ingest.processor;

import org.chronopolis.amqp.ChronProducer;
import org.chronopolis.db.common.RestoreRepository;
import org.chronopolis.db.common.model.RestoreRequest;
import org.chronopolis.messaging.Indicator;
import org.chronopolis.messaging.base.ChronMessage;
import org.chronopolis.messaging.base.ChronProcessor;
import org.chronopolis.messaging.collection.CollectionRestoreCompleteMessage;
import org.chronopolis.messaging.factory.MessageFactory;

/**
 * Created by shake on 8/8/14.
 */
public class CollectionRestoreCompleteProcessor implements ChronProcessor {

    private ChronProducer producer;
    private MessageFactory messageFactory;
    private RestoreRepository restoreRepository;

    @Override
    public void process(final ChronMessage chronMessage) {
        if (!(chronMessage instanceof CollectionRestoreCompleteMessage)) {
            throw new RuntimeException("Invalid message for "
                    + this.getClass().getName()
                    + ": "
                    + chronMessage.getClass().getName());
        }

        CollectionRestoreCompleteMessage msg =
                (CollectionRestoreCompleteMessage) chronMessage;

        if (msg.getMessageAtt().equalsIgnoreCase(Indicator.ACK.toString())) {
            // notify intake of completion
            RestoreRequest request = restoreRepository.findByCorrelationId(msg.getCorrelationId());
            String location = request.getDirectory();
            String returnKey = request.getReturnKey();

            ChronMessage reply = messageFactory.collectionRestoreCompleteMessage(Indicator.ACK,
                    location,
                    msg.getCorrelationId());

            producer.send(reply, returnKey);

        } else {
            // Send mail notifying failure
        }
    }
}
